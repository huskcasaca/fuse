package dev.huskuraft.gradle.plugins.fuse.config;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

import java.util.HashMap;
import java.util.Map;

public class FuseConfiguration {

    // The name of the gradle module that contains the custom code
    @Getter
    @Setter
    @Input
    String projectName;

    // The name of the gradle module that contains the custom code
    @Getter
    @Setter
    @Input
    String source;

    // The name of the task to run to get the input file
    @Getter
    @Setter
    @Input
    String inputTaskName;

    // Packages that should be relocated, instead of duplicated
    @Getter
    @Nested
    Map<String, String> relocations = new HashMap<>();

    /**
     * Add a package to relocate, instead of duplicating
     *
     * @param from - The original name of the package. For example: com.google.gson
     * @param to   - The new name of the package. For example: forge.com.google.gson
     */
    public void relocate(String from, String to) {
        this.relocations.put(from, to);
    }
}
