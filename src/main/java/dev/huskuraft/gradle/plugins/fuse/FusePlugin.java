package dev.huskuraft.gradle.plugins.fuse;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import dev.huskuraft.gradle.plugins.fuse.tasks.FuseJar;

public class FusePlugin implements Plugin<Project> {

    public static Logger logger;

	public static final String FUSE_JAR_TASK_NAME = "fuseJar";
	public static final String FUSE_JAR_TASK_GROUP = "fuse";

    @Override
    public void apply(Project project) {
        logger = project.getLogger();

        // Register the task
		TaskProvider<FuseJar> task = project.getTasks().register(FUSE_JAR_TASK_NAME, FuseJar.class, fuse -> {
			fuse.setGroup(FUSE_JAR_TASK_GROUP);
			fuse.setDescription("Merge multiple jars into a single jar, for multi mod loader projects");
			fuse.getArchiveClassifier().set("fuse");
		});
//		project.getArtifacts().add(Constants.TASK_GROUP, project.getTasks().named(FUSE_JAR_TASK_NAME));

		// Check for task dependencies and register them on the main task
		project.allprojects(cc -> cc.afterEvaluate(ccc -> {

			if (task.get().getFuseConfigurations() != null && !task.get().getFuseConfigurations().isEmpty()) {
				task.get().getFuseConfigurations().forEach(c -> {
					if (ccc.getPath().equals(project.project(c.getSource()).getPath()) && c.getInputTaskName() != null && !c.getInputTaskName().isEmpty())
						resolveInputTasks(ccc, c.getInputTaskName(), c.getProjectName(), task);
				});
			}
		}));

    }

    /**
	 * Try to locate the correct task to run on the subproject
	 *
	 * @param targetProject - Sub project being processed
	 * @param inTask        - The name of the task that will be run
	 * @param inProject     - The name of the project the task is on
	 * @param mainTask      - The FuseJars task
	 */
    private void resolveInputTasks(Project targetProject, Object inTask, String inProject, TaskProvider<FuseJar> mainTask) {
        if (inTask == null)
            return;

        Task task = null;

        if (inProject == null || inProject.isEmpty())
            return;

        if (targetProject == null)
            return;

        if (inTask instanceof String) {
            task = targetProject.getTasks().getByName((String) inTask);
        }

        if (!(task instanceof AbstractArchiveTask))
            return;

        ;
        mainTask.get().dependsOn(targetProject.task("prepareFuseTask" + targetProject.getPath().replace(":", "-")).dependsOn(task.getPath()));
    }
}
