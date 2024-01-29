package dev.huskuraft.gradle.plugins.fuse.tasks;

import com.hypherionmc.jarrelocator.Relocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public interface Fuse {

    File file();

    String name();

    List<Relocation> relocations();

    record Impl(
        @NotNull File file,
        @NotNull String name,
        @NotNull List<Relocation> relocations
    ) implements Fuse {
    }

}
