package dev.huskuraft.gradle.plugins.fuse.tasks;

import com.hypherionmc.jarmanager.JarManager;
import com.hypherionmc.jarrelocator.Relocation;
import dev.huskuraft.gradle.plugins.fuse.utils.FileTools;
import org.apache.commons.io.FileUtils;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.WorkResults;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

class MergeJarAction implements CopyAction {

    public static final String FUSE_MERGE_DIR = "merged";

    public static final String FUSE_UNPACKED_SUFFIX = "unpacked";

    private final Map<String, String> ignoredDuplicateRelocations = new HashMap<>();
    private final Map<String, String> removeDuplicateRelocationResources = new HashMap<>();
    private final List<Relocation> relocations = new ArrayList<>();

    private final File jarFile;
    private final File tempDir;
    private final JarManager jarManager;
    private final List<Fuse> fuses;
    private final List<String> ignoredPackages;

    MergeJarAction(File jarFile, File tempDir, JarManager jarManager, List<Fuse> fuses, List<String> ignoredPackages) {
        this.jarFile = jarFile;
        this.tempDir = tempDir;
        this.jarManager = jarManager;
        this.fuses = fuses;
        this.ignoredPackages = ignoredPackages;
    }

    @Override
    public WorkResult execute(CopyActionProcessingStream stream) {
        try {
            FileTools.createOrReCreate(tempDir);
            mergeFuse();
            FileUtils.deleteQuietly(tempDir);
        } catch (IOException e) {
            e.printStackTrace();
            return WorkResults.didWork(false);
        }
        return WorkResults.didWork(true);
    }

    public void mergeFuse() throws IOException {
        var unpackedDirs = getUnpackedDirs();
        var mergedDir = getMergeDir();
        var mergedManifest = mergeManifestsInMetaInf(unpackedDirs);

        for (var dir : unpackedDirs) {
            FileTools.moveDirectory(dir, mergedDir);
        }

        writeManifestInMetaInf(mergedDir, mergedManifest);

        jarManager.setCompressionLevel(Deflater.BEST_COMPRESSION);
        jarManager.remapAndPack(mergedDir, jarFile, relocations);

    }

    private List<File> getUnpackedDirs() throws IOException {
        if (fuses.isEmpty()) {
            throw new IllegalArgumentException("No input jars were provided.");
        }
        var fuseDirs = new ArrayList<File>();
        for (var fuse : fuses) {
            if (!FileTools.exists(fuse.file())) {
                throw new FileNotFoundException("Fuse artifact " + fuse.file().getName() + " does not exist!");
            }
            fuseDirs.add(unpackJar(remapJar(fuse.file(), fuse.relocations())));
        }
        return fuseDirs;
    }

    private File getManifestFile(File file, boolean recreate) throws IOException {
        var metaInfDir = FileTools.getOrCreate(new File(file, "META-INF"));
        var manifestFile = new File(metaInfDir, "MANIFEST.MF");
        if (recreate) {
            return FileTools.createOrReCreateF(manifestFile);
        }
        return manifestFile;
    }

    private File getMergeDir() throws IOException {
        var mergedDir = new File(tempDir, FUSE_MERGE_DIR);
        return FileTools.getOrCreate(mergedDir);
    }

    private File getExtractTempDir(File file) {
        return FileTools.getOrCreate(new File(tempDir, file.getName() + "-" + FUSE_UNPACKED_SUFFIX));
    }

    private File getRemappedJarFile(File file) {
        return new File(tempDir, file.getName());
    }

    private File unpackJar(File file) throws IOException {
        var unpacked = getExtractTempDir(file);
        jarManager.unpackJar(file, unpacked);
        return unpacked;
    }

    private File remapJar(File file, List<Relocation> relocations) throws IOException {
        var remapped = FileTools.createOrReCreateF(getRemappedJarFile(file));
        jarManager.remapJar(file, remapped, relocations);
        return remapped;
    }

    private Manifest readManifestInMetaInf(File file) throws IOException {
        var manifest = new Manifest();
        var manifestFile = getManifestFile(file, false);
        if (manifestFile.exists()) {
            manifest.read(new FileInputStream(manifestFile));
        }
        return manifest;
    }

    private void writeManifestInMetaInf(File file, Manifest manifest) throws IOException {
        var outputStream = new FileOutputStream(getManifestFile(file, true));
        manifest.write(outputStream);
        outputStream.close();
    }

    private Manifest mergeManifestsInMetaInf(List<File> dirs) throws IOException {
        var mergedManifest = new Manifest();
        for (var dir : dirs) {
            readManifestInMetaInf(dir).getMainAttributes().forEach((key, value) -> mergedManifest.getMainAttributes().putValue(key.toString(), value.toString()));
        }

        return mergedManifest;
    }

}
