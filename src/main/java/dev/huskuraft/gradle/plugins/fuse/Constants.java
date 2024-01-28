package dev.huskuraft.gradle.plugins.fuse;

import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class Constants {

    public static final String TASK_GROUP = "fuse";
    public static final String TASK_NAME = "fuseJar";
    public static final String EXTENSION_NAME = "fuseJar";
    public static final String MANIFEST_KEY = "FuseJar-Version";

    public static Set<PosixFilePermission> filePerms = new HashSet<>();

    static {
        filePerms.add(PosixFilePermission.OTHERS_EXECUTE);
        filePerms.add(PosixFilePermission.OTHERS_WRITE);
        filePerms.add(PosixFilePermission.OTHERS_READ);
        filePerms.add(PosixFilePermission.OWNER_EXECUTE);
        filePerms.add(PosixFilePermission.OWNER_WRITE);
        filePerms.add(PosixFilePermission.OWNER_READ);
        filePerms.add(PosixFilePermission.GROUP_EXECUTE);
        filePerms.add(PosixFilePermission.GROUP_WRITE);
        filePerms.add(PosixFilePermission.GROUP_READ);
    }

}
