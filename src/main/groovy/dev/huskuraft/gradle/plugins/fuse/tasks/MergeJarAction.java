package dev.huskuraft.gradle.plugins.fuse.tasks;

import com.hypherionmc.jarmanager.JarManager;
import com.hypherionmc.jarrelocator.Relocation;
import dev.huskuraft.gradle.plugins.fuse.FuseJavaPlugin;
import dev.huskuraft.gradle.plugins.fuse.config.FuseConfiguration;
import dev.huskuraft.gradle.plugins.fuse.utils.FileTools;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.WorkResults;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

@RequiredArgsConstructor(staticName = "of")
class MergeJarAction implements CopyAction {

    private final Map<FuseConfiguration, File> customInputs;
    // Relocations
    private final List<String> ignoredPackages;
    private final Map<String, String> ignoredDuplicateRelocations = new HashMap<>();
    private final Map<String, String> removeDuplicateRelocationResources = new HashMap<>();
    private final List<Relocation> relocations = new ArrayList<>();
    private final JarManager jarManager = JarManager.getInstance();
    // Settings
    private final File tempDir;
    private final File jarFile;
    // Custom
    private Map<FuseConfiguration, Map<File, File>> customTemps;

    /**
     * Start the merge process
     *
     * @return - The fully merged jar file
     */

    @Override
    public WorkResult execute(CopyActionProcessingStream stream) {
        try {
            merge();
            clean();
        } catch (IOException e) {
            return WorkResults.didWork(false);
        }
        return WorkResults.didWork(true);
    }

    public void merge() throws IOException {

        jarManager.setCompressionLevel(Deflater.BEST_COMPRESSION);
        File outJar = new File(tempDir, jarFile.getName());

        FuseJavaPlugin.logger.lifecycle("Cleaning output Directory");
        FileTools.createOrReCreate(tempDir);

        // Check if the required input files exists
        if (customInputs.isEmpty()) {
            throw new IllegalArgumentException("No input jars were provided.");
        }
        customInputs.forEach((key, value) -> {
            if (!FileTools.exists(value)) {
                FuseJavaPlugin.logger.warn(key.getProjectName() + " jar does not exist! You can ignore this if you are not using custom configurations");
            }
        });

        // Remap the jar files to match their platform name
        remapJars();

        customTemps = new HashMap<>();
        customInputs.forEach((key, value) -> {
            Map<File, File> temp = new HashMap<>();

            temp.put(value, new File(tempDir, key.getProjectName() + "-temp"));
            FileTools.getOrCreate(new File(tempDir, key.getProjectName() + "-temp"));
            customTemps.put(key, temp);
        });

        // Extract the input jars to their processing directories
        FuseJavaPlugin.logger.lifecycle("Unpacking input jars");

        customTemps.forEach((key, value) -> value.forEach((k, v) -> {
            if (FileTools.exists(k)) {
                try {
                    jarManager.unpackJar(k, v);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));

        File mergedTemp = FileTools.getOrCreate(new File(tempDir, "merged-temp"));
        processManifests(mergedTemp);

        for (Map.Entry<FuseConfiguration, Map<File, File>> entry : customTemps.entrySet()) {
            for (Map.Entry<File, File> entry2 : entry.getValue().entrySet()) {
                FileTools.moveDirectory(entry2.getValue(), mergedTemp);
            }
        }

        // Process duplicate packages and resources
        FuseJavaPlugin.logger.lifecycle("Processing duplicate packages and resources");
        processDuplicatePackages();
        removeDuplicatePackages(mergedTemp);
        removeDuplicateResources(mergedTemp);

        // Clean the output jar if it exists
        FileUtils.deleteQuietly(outJar);

        // Repack the fully processed jars into a single jar
        FuseJavaPlugin.logger.lifecycle("Fusing jars into single jar");
        jarManager.remapAndPack(mergedTemp, outJar, relocations);

        Files.move(outJar.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

    }

    /**
     * Clean the output directory before the task exists
     *
     * @throws IOException - Thrown if an IO error occurs
     */
    public void clean() throws IOException {
        FuseJavaPlugin.logger.lifecycle("Finishing up");
        FileUtils.deleteQuietly(tempDir);
    }

    /**
     * ================================================================================================================
     * =                                            Jar Remapping                                                     =
     * ================================================================================================================
     */

    /**
     * Process input jars to relocate them internally to their final package names
     *
     * @throws IOException - Thrown if an IO error occurs
     */
    public void remapJars() throws IOException {
        FuseJavaPlugin.logger.lifecycle("Start processing input jars");

        for (Map.Entry<FuseConfiguration, File> entry : customInputs.entrySet()) {
            if (FileTools.exists(entry.getValue())) {
                remapCustomJar(entry.getKey(), entry.getValue());
            }
        }
    }


    /**
     * Remap a Custom Jar
     *
     * @param configuration - The configuration of the custom package
     * @param jarFile       - The input jar of the custom project to be processed
     * @throws IOException - Thrown if an io exception occurs
     */
    private void remapCustomJar(FuseConfiguration configuration, File jarFile) throws IOException {
        String name = configuration.getProjectName();
        File remappedJar = FileTools.createOrReCreateF(new File(tempDir, "tempCustomInMerging_" + name + ".jar"));

        List<Relocation> customRelocations = new ArrayList<>();
//        customRelocations.add(new Relocation(group, name + "." + group));
        if (configuration.getRelocations() != null)
            customRelocations.addAll(configuration.getRelocations().entrySet().stream().map(entry -> new Relocation(entry.getKey(), entry.getValue())).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));


        jarManager.remapJar(jarFile, remappedJar, customRelocations);
        customInputs.replace(configuration, jarFile, remappedJar);
    }


    /**
     * Process the manifest files from all the input jars and combine them into one
     *
     * @param mergedTemp - The processing directory
     * @throws IOException - Thrown if an IO error occurs
     */
    public void processManifests(File mergedTemp) throws IOException {
        Manifest mergedManifest = new Manifest();
        List<Manifest> customManifests = new ArrayList<>();

        FileInputStream fileInputStream = null;
        for (Map.Entry<FuseConfiguration, Map<File, File>> entry : customTemps.entrySet()) {
            for (Map.Entry<File, File> entry2 : entry.getValue().entrySet()) {
                Manifest manifest = new Manifest();
                if (FileTools.exists(entry2.getKey())) {
                    manifest.read(fileInputStream = new FileInputStream(new File(entry2.getValue(), "META-INF/MANIFEST.MF")));
                    customManifests.add(manifest);
                }
                if (fileInputStream != null) fileInputStream.close();
            }
        }

        for (Manifest manifest : customManifests) {
            manifest.getMainAttributes().forEach((key, value) -> mergedManifest.getMainAttributes().putValue(key.toString(), value.toString()));
        }

        if (mergedManifest.getMainAttributes().getValue("MixinConfigs") != null) {
            String value = mergedManifest.getMainAttributes().getValue("MixinConfigs");
            String[] mixins;
            List<String> remappedMixin = new ArrayList<>();

            if (value.contains(",")) {
                mixins = value.split(",");
            } else {
                mixins = new String[]{value};
            }

            for (String mixin : mixins) {
                remappedMixin.add("forge-" + mixin);
            }

            mergedManifest.getMainAttributes().putValue("MixinConfigs", String.join(",", remappedMixin));
        }
        // TODO Manifest Version
        //mergedManifest.getMainAttributes().putValue(manifestVersionKey, version);

        for (Map.Entry<FuseConfiguration, Map<File, File>> entry : customTemps.entrySet()) {
            for (Map.Entry<File, File> entry2 : entry.getValue().entrySet()) {
                if (FileTools.exists(entry2.getKey())) new File(entry2.getValue(), "META-INF/MANIFEST.MF").delete();
            }
        }

        new File(FileTools.getOrCreate(new File(mergedTemp, "META-INF")), "MANIFEST.MF").createNewFile();
        FileOutputStream outputStream = new FileOutputStream(new File(mergedTemp, "META-INF/MANIFEST.MF"));
        mergedManifest.write(outputStream);
        outputStream.close();
    }

    /**
     * ================================================================================================================
     * =                                    Duplicate Package Processing                                              =
     * ================================================================================================================
     */

    /**
     * Build a list of duplicate packages that need to be removed from the final jar
     */
    private void processDuplicatePackages() {
        if (ignoredPackages != null) {
            for (String duplicate : ignoredPackages) {
                String duplicatePath = duplicate.replace(".", "/");

                for (Map.Entry<FuseConfiguration, Map<File, File>> entry : customTemps.entrySet()) {
                    for (Map.Entry<File, File> entry2 : entry.getValue().entrySet()) {
                        if (FileTools.exists(entry2.getKey())) {
                            String name = entry.getKey().getProjectName();
                            ignoredDuplicateRelocations.put(name + "." + duplicate, duplicate);
                            removeDuplicateRelocationResources.put(name + "/" + duplicatePath, duplicatePath);
                        }
                    }
                }
            }

            removeDuplicateRelocationResources.putAll(ignoredDuplicateRelocations);
        }
    }

    /**
     * Relocate duplicate packages from their original location, to a single location
     *
     * @param mergedTemps - The processing directory
     * @throws IOException - Thrown if an IO exception occurs
     */
    private void removeDuplicatePackages(File mergedTemps) throws IOException {
        for (Map.Entry<String, String> entry : ignoredDuplicateRelocations.entrySet()) {
            File baseFile = new File(mergedTemps, entry.getKey().replace(".", "/") + "/");
            String name = entry.getValue().replace(".", "/") + "/";
            File outFile = new File(mergedTemps, name);

            if (outFile.isDirectory())
                outFile.mkdirs();

            FileTools.moveDirectory(baseFile, outFile);
            relocations.add(new Relocation(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Remove duplicate resources files from extracted jars
     *
     * @param mergedTemps - The processing directory
     * @throws IOException - Thrown if an IO error occurs
     */
    public void removeDuplicateResources(File mergedTemps) throws IOException {
        if (ignoredPackages != null) {
            for (File file : FileTools.getTextFiles(mergedTemps)) {
                List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder();

                for (String line : lines) {
                    for (HashMap.Entry<String, String> entry : removeDuplicateRelocationResources.entrySet()) {
                        line = line.replace(entry.getKey(), entry.getValue());
                    }
                    sb.append(line).append("\n");
                }
                FileUtils.write(file, sb.toString().trim() + "\n", StandardCharsets.UTF_8);
            }
        }
    }
}
