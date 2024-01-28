package dev.huskuraft.gradle.plugins.fuse

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class FusePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.with {
            plugins.apply(FuseBasePlugin)
            plugins.withType(JavaPlugin) {
                plugins.apply(FuseJavaPlugin)
            }
        }

    }

}
