package dev.huskuraft.gradle.plugins.fuse.tasks;

import dev.huskuraft.gradle.plugins.fuse.FuseJavaPlugin;
import dev.huskuraft.gradle.plugins.fuse.config.FuseConfiguration;
import dev.huskuraft.gradle.plugins.fuse.utils.FileChecks;
import dev.huskuraft.gradle.plugins.fuse.utils.FileTools;
import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;
import org.gradle.api.Project;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class FuseJar extends Jar implements FuseSpec {

    // Custom Project Configurations
    @Getter
    @Nested
    private final List<FuseConfiguration> fuseConfigurations = new ArrayList<>();
    // Duplicate packages that will be de-duplicated upon merge
    @Getter
    @Nested
    private final List<String> duplicateRelocations = new ArrayList<>();
    // The name of the final jar
    @Getter
    @Setter
    @Input
    private String jarVersion;


    public FuseJar() {
    }

    /**
     * Run the main task logic and copy the files to the correct locations
     *
     * @return - Just returns true to say the task executed
     */
    @Override
    protected @NotNull CopyAction createCopyAction() {
        // Get settings from extension
        List<FuseConfiguration> fuseConfigurations = getFuseConfigurations();

        // Try to resolve the projects specific in the extension config
        Map<Project, FuseConfiguration> customProjects = new HashMap<>();
        List<Boolean> validation = new ArrayList<>();

        for (FuseConfiguration customSettings : fuseConfigurations) {
            try {
                customProjects.put(getProject().getAllprojects().stream().filter(p -> p.getPath().equals(getProject().project(customSettings.getSource()).getPath())).findFirst().get(), customSettings);
                validation.add(true);
            } catch (NoSuchElementException ignored) {
            }
        }

        // Check that at least 2 projects are defined
        if (validation.isEmpty()) FuseJavaPlugin.logger.warn("Only one project was found.");
        if (validation.size() == 1) FuseJavaPlugin.logger.warn("No projects were found.");

        validation.clear();

        Map<FuseConfiguration, File> customJars = new HashMap<>();

        for (Map.Entry<Project, FuseConfiguration> entry : customProjects.entrySet()) {
            File f = getInputFile(entry.getValue().getInputTaskName(), entry.getKey());
            if (f != null)
                customJars.put(entry.getValue(), f);
        }

        return MergeJarAction.of(
                customJars,
                getDuplicateRelocations(),
                new File(getTemporaryDir(), "fuseJar"),
                getArchiveFile().get().getAsFile()
        );
    }

    /**
     * Try to determine the input jar of a project
     *
     * @param inProject   - The project the file should be for or from
     * @return - The jar file or null
     */
    @Nullable
    private File getInputFile(String inputTaskName, Project inProject) {
        if (inputTaskName != null && !inputTaskName.isEmpty()) {
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
