/*
 * This file is part of ModFusioner, licensed under the GNU Lesser General Public License v2.1.
 *
 * This project is based on, and contains code from https://github.com/PacifistMC/Forgix, licensed under the same license.
 * See their license here: https://github.com/PacifistMC/Forgix/blob/main/LICENSE
 *
 * Copyright HypherionSA and Contributors
 * Forgix Code Copyright by their contributors and Ran-Mewo
 */
package com.hypherionmc.modfusioner.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import com.hypherionmc.modfusioner.Constants;
import com.hypherionmc.modfusioner.task.FuseJar;

/**
 * @author HypherionSA
 * Main Gradle Plugin Class
 */
public class ModFusionerPlugin implements Plugin<Project> {

//    public static Project rootProject;
    public static Logger logger;
//    public static FusionerExtension modFusionerExtension;

	public static final String FUSE_JAR_TASK_NAME = "fuseJar";

    @Override
    public void apply(Project project) {
        // We only want to apply the project to the Root project
//        if (project != project.getRootProject())
//            return;

//        rootProject = project.getRootProject();
        logger = project.getLogger();

        // Register the extension
//        modFusionerExtension = project.getExtensions().create(Constants.EXTENSION_NAME, FusionerExtension.class);



        // Register the task
		TaskProvider<FuseJar> task = project.getTasks().register(FUSE_JAR_TASK_NAME, FuseJar.class, fuse -> {
			fuse.setGroup(Constants.TASK_GROUP);
			fuse.setDescription("Merge multiple jars into a single jar, for multi mod loader projects");
			fuse.getArchiveClassifier().set("all");
		});
//		project.getArtifacts().add(Constants.TASK_GROUP, project.getTasks().named(FUSE_JAR_TASK_NAME));



		// Check for task dependencies and register them on the main task
		project.allprojects(cc -> cc.afterEvaluate(ccc -> {
//            if (modFusionerExtension.getForgeConfiguration() != null
//                    && modFusionerExtension.getForgeConfiguration().inputTaskName != null
//                    && !modFusionerExtension.getForgeConfiguration().inputTaskName.isEmpty()) {
//                if (ccc.getName().equals(modFusionerExtension.getForgeConfiguration().getProjectName()))
//                    resolveInputTasks(
//                            ccc,
//                            modFusionerExtension.getForgeConfiguration().getInputTaskName(),
//                            modFusionerExtension.getForgeConfiguration().getProjectName(),
//                            task
//                    );
//            }
//
//            if (modFusionerExtension.getFabricConfiguration() != null
//                    && modFusionerExtension.getFabricConfiguration().inputTaskName != null
//                    && !modFusionerExtension.getFabricConfiguration().inputTaskName.isEmpty()) {
//                if (ccc.getName().equals(modFusionerExtension.getFabricConfiguration().getProjectName()))
//                    resolveInputTasks(
//                            ccc,
//                            modFusionerExtension.getFabricConfiguration().getInputTaskName(),
//                            modFusionerExtension.getFabricConfiguration().getProjectName(),
//                            task
//                    );
//            }
//
//            if (modFusionerExtension.getQuiltConfiguration() != null
//                    && modFusionerExtension.getQuiltConfiguration().inputTaskName != null
//                    && !modFusionerExtension.getQuiltConfiguration().inputTaskName.isEmpty()) {
//                if (ccc.getName().equals(modFusionerExtension.getQuiltConfiguration().getProjectName()))
//                    resolveInputTasks(
//                            ccc,
//                            modFusionerExtension.getQuiltConfiguration().getInputTaskName(),
//                            modFusionerExtension.getQuiltConfiguration().getProjectName(),
//                            task
//                    );
//            }

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
