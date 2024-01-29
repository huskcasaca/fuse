package dev.huskuraft.gradle.plugins.fuse.tasks;

import com.hypherionmc.jarmanager.JarManager;
import com.hypherionmc.jarrelocator.Relocation;
import dev.huskuraft.gradle.plugins.fuse.config.FuseSource;
import dev.huskuraft.gradle.plugins.fuse.config.FuseSourceSpec;
import dev.huskuraft.gradle.plugins.fuse.utils.FileTools;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FuseJar extends Jar implements FuseSpec {

    public static final String FUSE_JAR_DESCRIPTION = "Merge multiple jars into a single jar, for multi mod loader projects";
    public static final String FUSE_JAR_CLASSIFIER = "fuse";

    private final List<FuseSource> fuseSources = new ArrayList<>();

    private final List<String> duplicateRelocations = new ArrayList<>();

    public FuseJar() {
        setDescription(FUSE_JAR_DESCRIPTION);
        getArchiveClassifier().set(FUSE_JAR_CLASSIFIER);

        getProject().afterEvaluate(project -> {
            for (var fuseConfiguration : getFuseConfigurations()) {
                resolveInputTasks(fuseConfiguration.getProject(), fuseConfiguration.getTask(), FuseJar.this);
            }
        });



    }

    @Nested
    public List<FuseSource> getFuseConfigurations() {
        return fuseSources;
    }

    @Nested
    public List<String> getDuplicateRelocations() {
        return duplicateRelocations;
    }

    /**
     * Try to locate the correct task to run on the subproject
     *
     * @param project - Sub project being processed
     * @param taskLike        - The name of the task that will be run
     * @param fuseTask      - The FuseJars task
     */
    private void resolveInputTasks(Project project, Object taskLike, FuseJar fuseTask) {
        if (taskLike == null) throw new NullPointerException("task name cannot be null");
        if (project == null) throw new NullPointerException("source project cannot be null");

        Task task = null;

        if (taskLike instanceof String string) {
            task = project.getTasks().getByName(string);
        }

        if (!(task instanceof AbstractArchiveTask archiveTask)) throw new IllegalArgumentException("task must be an AbstractArchiveTask");

        var prepareTask = project.task("prepareFuseTask" + project.getPath().replace(":", "-"));
        fuseTask.dependsOn(prepareTask.dependsOn(archiveTask));
    }

    @Override
    protected @NotNull CopyAction createCopyAction() {

        return new MergeJarAction(
            getArchiveFile().get().getAsFile(),
            getTemporaryDir(),
            JarManager.getInstance(),
            getFuses(),
            getDuplicateRelocations()
        );
    }

    private List<Fuse> getFuses() {

        if (getFuseConfigurations().isEmpty()) getLogger().warn("Only one project was found.");
        if (getFuseConfigurations().size() == 1) getLogger().warn("No projects were found.");

        var fuses = new ArrayList<Fuse>();

        for (var entry : getFuseConfigurations()) {
            var inputFile = FileTools.resolveFile(entry.getProject(), entry.getTask());
            if (inputFile != null) {
                var relocations = entry.getRelocations().entrySet().stream().map(e -> new Relocation(e.getKey(), e.getValue())).toList();
                fuses.add(new Fuse.Impl(inputFile, entry.getProject().getName(), relocations));
            }
        }

        return fuses;

    }

    @Override
    public FuseSpec includeJar(Action<FuseSourceSpec> closure) {
        var fuseConfiguration = new FuseSource();
        getProject().configure(List.of(fuseConfiguration), closure);

        if (fuseConfiguration.getProject() == null) {
            throw new IllegalStateException("includeJar {} requires a \"source\"");
        }
        fuseSources.add(fuseConfiguration);
        return this;
    }

}
