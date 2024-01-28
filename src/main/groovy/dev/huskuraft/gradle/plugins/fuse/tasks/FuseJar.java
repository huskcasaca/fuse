package dev.huskuraft.gradle.plugins.fuse.tasks;

import dev.huskuraft.gradle.plugins.fuse.FuseJavaPlugin;
import dev.huskuraft.gradle.plugins.fuse.config.FuseConfiguration;
import dev.huskuraft.gradle.plugins.fuse.utils.FileTools;
import groovy.lang.Closure;
import lombok.Getter;
import org.gradle.api.Project;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.tasks.Nested;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class FuseJar extends Jar implements FuseSpec {

    @Getter
    @Nested
    private final List<FuseConfiguration> fuseConfigurations = new ArrayList<>();

    @Getter
    @Nested
    private final List<String> duplicateRelocations = new ArrayList<>();

    @Override
    protected @NotNull CopyAction createCopyAction() {
        var customProjects = new HashMap<Project, FuseConfiguration>();
        var validation = new ArrayList<Boolean>();

        for (FuseConfiguration customSettings : getFuseConfigurations()) {
            try {
                customProjects.put(getProject().getAllprojects().stream().filter(p -> p.getPath().equals(getProject().project(customSettings.getSource()).getPath())).findFirst().get(), customSettings);
                validation.add(true);
            } catch (NoSuchElementException ignored) {
            }
        }

        if (validation.isEmpty()) FuseJavaPlugin.logger.warn("Only one project was found.");
        if (validation.size() == 1) FuseJavaPlugin.logger.warn("No projects were found.");

        var fuses = new HashMap<FuseConfiguration, File>();

        for (var entry : customProjects.entrySet()) {
            var inputFile = getInputFile(entry.getValue().getInputTaskName(), entry.getKey());
            if (inputFile != null) {
                fuses.put(entry.getValue(), inputFile);
            }
        }

        return MergeJarAction.of(
            getArchiveFile().get().getAsFile(),
            new File(getTemporaryDir(), "fuseJar"),
            fuses,
            getDuplicateRelocations()
        );
    }

    @Nullable
    private File getInputFile(String inputTaskName, Project inProject) {
        assert inputTaskName != null;
        assert !inputTaskName.isEmpty();
        return FileTools.resolveFile(inProject, inputTaskName);
    }

    @Override
    public FuseConfiguration fuse(Closure<FuseConfiguration> closure) {
        var fuseConfiguration = new FuseConfiguration();
        getProject().configure(fuseConfiguration, closure);

        if (fuseConfiguration.getProjectName() == null || fuseConfiguration.getProjectName().isEmpty()) {
            throw new IllegalStateException("Custom project configurations need to specify a \"projectName\"");
        }
        fuseConfigurations.add(fuseConfiguration);
        return fuseConfiguration;
    }
}
