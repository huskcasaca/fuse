package dev.huskuraft.gradle.plugins.fuse.merger

import org.gradle.api.Named
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.Internal

trait Merger implements Named {

    abstract boolean canMerge(FileTreeElement element)

    abstract void merge(MergerContext context)

    @Internal
    String getName() {
        return getClass().simpleName
    }
}
