package dev.huskuraft.gradle.plugins.fuse.tasks;

import dev.huskuraft.gradle.plugins.fuse.config.FuseSource;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;

public interface FuseSpec extends CopySpec {
    FuseSpec includeJar(Action<FuseSource> closure);

    default FuseSpec includeJar(Project project, String task) {
        return includeJar(fuseSourceSpec -> {
            fuseSourceSpec.source(project);
            fuseSourceSpec.task(task);
        });
    }
}
