package dev.huskuraft.gradle.plugins.fuse.merger;

import java.io.InputStream;
import java.io.OutputStream;

public record MergerContext(
    InputStream input,
    OutputStream output
) {

}
