package dev.huskuraft.gradle.plugins.fuse

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

class FuseBasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (GradleVersion.current() < GradleVersion.version("8.0")) {
            throw new GradleException("This version of Shadow supports Gradle 8.0+ only. Please upgrade.")
        }

    }

}
