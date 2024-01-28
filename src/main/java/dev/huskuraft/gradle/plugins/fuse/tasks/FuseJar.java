package dev.huskuraft.gradle.plugins.fuse.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.WorkResults;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.huskuraft.gradle.plugins.fuse.FusePlugin;
import dev.huskuraft.gradle.plugins.fuse.config.FuseConfiguration;
import dev.huskuraft.gradle.plugins.fuse.utils.FileChecks;
import dev.huskuraft.gradle.plugins.fuse.utils.FileTools;
import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;

public class FuseJar extends Jar implements FuseSpec {

    // Fixed values
    private final File mergedJar;
    private static final AtomicBoolean hasRun = new AtomicBoolean(false);


	// Group, or package names that will be used for the final jar
	@Getter
	@Setter  @Input
	private String groupName;

	// The name of the final jar
	@Getter @Setter @Input
	private String mergedJarName;

	// The name of the final jar
	@Getter @Setter  @Input
	private String jarVersion;

	// Duplicate packages that will be de-duplicated upon merge
	@Getter @Nested
	private List<String> duplicateRelocations;

	// The output directory for the merged jar
	@Getter @Setter @Input
	private String outputDirectory;


	// Custom Project Configurations
	@Getter @Nested
	private final List<FuseConfiguration> fuseConfigurations = new ArrayList<>();


	public FuseJar() {
		fusionerExtension();
        // Set task default values from extension
        getArchiveBaseName().set(getMergedJarName());
        getArchiveVersion().set(getJarVersion());
        getDestinationDirectory().set(getProject().file(getOutputDirectory()));

        // We don't allow custom input files, when the user defines their own task
        getInputs().files();

        // Only allow the task to run once per cycle
        getOutputs().upToDateWhen(spec -> hasRun.get());

        // Set output file
        mergedJar = new File(getDestinationDirectory().get().getAsFile(), getArchiveFileName().get());
        getOutputs().file(mergedJar);
    }


	void fusionerExtension() {
		if (groupName == null || groupName.isEmpty()) {
			// TODO: 28/1/24
//			if (ModFusionerPlugin.rootProject.hasProperty("group") && ModFusionerPlugin.rootProject.property("group") != null) {
//				group = ModFusionerPlugin.rootProject.property("group").toString();
//			} else {
//				ModFusionerPlugin.logger.error("\"group\" is not defined and cannot be set automatically");
//			}
		}

		if (mergedJarName == null || mergedJarName.isEmpty()) {
			mergedJarName = "MergedJar";
		}

		if (jarVersion != null && jarVersion.isEmpty()) {
			// TODO: 28/1/24
//			if (ModFusionerPlugin.rootProject.hasProperty("version") && ModFusionerPlugin.rootProject.property("version") != null) {
//				jarVersion = ModFusionerPlugin.rootProject.property("version").toString();
//			} else {
//				jarVersion = "1.0";
//			}
		}

		if (outputDirectory == null || outputDirectory.isEmpty())
			outputDirectory = "artifacts/fused";

		duplicateRelocations = new ArrayList<>();
	}

    /**
     * Main task logic
     * @throws IOException - Thrown when an IO error occurs
     */
    void fuseJars() throws IOException {
        long time = System.currentTimeMillis();

        FusePlugin.logger.lifecycle("Start Fusing Jars");

        // Get settings from extension
        List<FuseConfiguration> fuseConfigurations = getFuseConfigurations();

        // Try to resolve the projects specific in the extension config
        Map<Project, FuseConfiguration> customProjects = new HashMap<>();
        List<Boolean> validation = new ArrayList<>();

        if (fuseConfigurations != null) {
            for (FuseConfiguration customSettings : fuseConfigurations) {
                try {
                    customProjects.put(getProject().getAllprojects().stream().filter(p -> p.getPath().equals(getProject().project(customSettings.getSource()).getPath())).findFirst().get(), customSettings);
                    validation.add(true);
                } catch (NoSuchElementException ignored) { }
            }
        }

        // Check that at least 2 projects are defined
        if (validation.size() < 2) {
            if (validation.size() == 1) FusePlugin.logger.error("Only one project was found. Skipping fusejars task.");
            if (validation.size() == 0) FusePlugin.logger.error("No projects were found. Skipping fusejars task.");
            return;
        }
        validation.clear();

        // Try to automatically determine the input jar from the projects
        File forgeJar = null;
        File fabricJar = null;
        File quiltJar = null;
        Map<FuseConfiguration, File> customJars = new HashMap<>();

        for (Map.Entry<Project, FuseConfiguration> entry : customProjects.entrySet()) {
            File f = getInputFile(entry.getValue().getInputFile(), entry.getValue().getInputTaskName(), entry.getKey());
            if (f != null)
                customJars.put(entry.getValue(), f);
        }

        // Set up the final output jar
        if (mergedJar.exists()) FileUtils.forceDelete(mergedJar);
        if (!mergedJar.getParentFile().exists()) mergedJar.getParentFile().mkdirs();

        // Set up the jar merge action
        MergeJarAction mergeAction = MergeJarAction.of(
                customJars,
                getDuplicateRelocations(),
                getGroupName(),
                new File(getTemporaryDir(), "fuseJar"),
                getArchiveFileName().get()
        );

        // Merge them jars
        Path tempMergedJarPath = mergeAction.mergeJars(false).toPath();

        // Move the merged jar to the specified output directory
        Files.move(tempMergedJarPath, mergedJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Cleanup
        mergeAction.clean();

        FusePlugin.logger.lifecycle("Fused jar created in " + (System.currentTimeMillis() - time) / 1000.0 + " seconds.");
        hasRun.set(true);
    }

    /**
     * Run the main task logic and copy the files to the correct locations
     * @return - Just returns true to say the task executed
     */
    @Override
    protected @NotNull CopyAction createCopyAction() {
        return copyActionProcessingStream -> {
            copyActionProcessingStream.process(fileCopyDetailsInternal -> {
                if (!hasRun.get())
                    try {
                        fuseJars();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
            });

            return WorkResults.didWork(true);
        };
    }

    /**
     * Try to determine the input jar of a project
     * @param jarLocation - The user defined jar location
     * @param inProject - The project the file should be for or from
     * @return - The jar file or null
     */
    @Nullable
    private File getInputFile(@Nullable String jarLocation, String inputTaskName, Project inProject) {
        if (jarLocation != null && !jarLocation.isEmpty()) {
            return new File(inProject.getProjectDir(), jarLocation);
        } else if (inputTaskName != null && !inputTaskName.isEmpty()) {
          return FileTools.resolveFile(inProject, inputTaskName);
        } else {
            int i = 0;
            for (File file : new File(inProject.getBuildDir(), "libs").listFiles()) {
                if (file.isDirectory()) continue;
                if (FileChecks.isZipFile(file)) {
                    if (file.getName().length() < i || i == 0) {
                        i = file.getName().length();
                        return file;
                    }
                }
            }
        }

        return null;
    }


	/**
	 * Set up custom project configurations
	 */
	@Override
	public FuseConfiguration fuse(Closure<FuseConfiguration> closure) {
		FuseConfiguration fuseConfiguration = new FuseConfiguration();
		getProject().configure(fuseConfiguration, closure);

		if (fuseConfiguration.getProjectName() == null || fuseConfiguration.getProjectName().isEmpty()) {
			throw new IllegalStateException("Custom project configurations need to specify a \"projectName\"");
		}
		fuseConfigurations.add(fuseConfiguration);
		return fuseConfiguration;
	}
}
