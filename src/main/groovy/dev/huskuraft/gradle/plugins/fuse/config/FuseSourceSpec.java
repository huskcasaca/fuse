package dev.huskuraft.gradle.plugins.fuse.config;

import org.gradle.api.Project;

public interface FuseSourceSpec {

    void source(Project project);

    void task(String task);

    /**
     * Add a package to relocate, instead of duplicating
     *
     * @param from - The original name of the package. For example: com.google.gson
     * @param to   - The new name of the package. For example: forge.com.google.gson
     */
    void relocate(String from, String to);
}
