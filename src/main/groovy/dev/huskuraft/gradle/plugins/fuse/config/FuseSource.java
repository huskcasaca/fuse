package dev.huskuraft.gradle.plugins.fuse.config;

import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;

import java.util.LinkedHashMap;
import java.util.Map;

public class FuseSource implements FuseSourceSpec {

    String task;

    Map<String, String> relocations = new LinkedHashMap<>();

    private Project project;

    @Override
    public void source(Project project) {
        this.project = project;
    }

    @Override
    public void task(String task) {
        this.task = task;
    }

    public void relocate(String from, String to) {
        this.relocations.put(from, to);
    }

    @Input
    public String getTask() {
        return task;
    }

    @Nested
    public Map<String, String> getRelocations() {
        return relocations;
    }

    @Internal
    public Project getProject() {
        return project;
    }
}
