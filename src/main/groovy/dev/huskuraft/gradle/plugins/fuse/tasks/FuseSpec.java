package dev.huskuraft.gradle.plugins.fuse.tasks;

import dev.huskuraft.gradle.plugins.fuse.config.FuseConfiguration;
import groovy.lang.Closure;

public interface FuseSpec {
    FuseConfiguration fuse(Closure<FuseConfiguration> closure);
}
