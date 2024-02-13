package dev.huskuraft.gradle.plugins.fuse.tasks;

import com.hypherionmc.jarrelocator.Relocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public interface Fuse {

    File root();

    String name();

    List<Relocation> relocations();

    record Impl(
        @NotNull File root,
        @NotNull String name,
        @NotNull List<Relocation> relocations
    ) implements Fuse {
    }

}
