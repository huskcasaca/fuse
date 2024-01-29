package dev.huskuraft.gradle.plugins.fuse

import dev.huskuraft.gradle.plugins.fuse.tasks.FuseJar
import org.gradle.api.Plugin
import org.gradle.api.Project

class FuseJavaPlugin implements Plugin<Project> {

    public static final String FUSE_JAR_TASK_NAME = "fuseJar"
    public static final String FUSE_JAR_TASK_GROUP = "fuse"

    @Override
    void apply(Project project) {
        project.getTasks().register(FUSE_JAR_TASK_NAME, FuseJar.class, task -> {
            task.setGroup(FUSE_JAR_TASK_GROUP)
        })
    }

}
