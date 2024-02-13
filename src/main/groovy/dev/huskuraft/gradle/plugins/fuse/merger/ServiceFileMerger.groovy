package dev.huskuraft.gradle.plugins.fuse.merger

import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet

class ServiceFileMerger implements Merger, PatternFilterable {

    private static final String SERVICES_PATTERN = "**/META-INF/services/**"

    private static final String GROOVY_EXTENSION_MODULE_DESCRIPTOR_PATTERN =
            "META-INF/services/org.codehaus.groovy.runtime.ExtensionModule"

//    private Map<String, ServiceStream> serviceEntries = [:].withDefault { new ServiceStream() }

    private final PatternSet patternSet =
            new PatternSet().include(SERVICES_PATTERN).exclude(GROOVY_EXTENSION_MODULE_DESCRIPTOR_PATTERN)

    void setPath(String path) {
        patternSet.setIncludes(["${path}/**"])
    }

    @Override
    boolean canMerge(FileTreeElement element) {
        return patternSet.asSpec.isSatisfiedBy(element)
    }

    @Override
    void merge(MergerContext context) {

        def reader = new BufferedReader(context.input().newReader())
        def writer = new BufferedWriter(context.output().newWriter())

        def line
        while ((line = reader.readLine()) != null) {
            writer.write(line)
            writer.newLine()
        }

        writer.flush();
        writer.close();
        reader.close();

    }

//    @Override
//    boolean hasTransformedResource() {
//        return serviceEntries.size() > 0
//    }

//    @Override
//    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
//        serviceEntries.each { String path, ServiceStream stream ->
//            ZipEntry entry = new ZipEntry(path)
//            entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
//            os.putNextEntry(entry)
//            IOUtil.copy(stream.toInputStream(), os)
//            os.closeEntry()
//        }
//    }

//    static class ServiceStream extends ByteArrayOutputStream {
//
//        ServiceStream(){
//            super( 1024 )
//        }
//
//        void append(InputStream is ) throws IOException {
//            if ( super.count > 0 && super.buf[super.count - 1] != '\n' && super.buf[super.count - 1] != '\r' ) {
//                byte[] newline = '\n'.bytes
//                write(newline, 0, newline.length)
//            }
//            IOUtil.copy(is, this)
//        }
//
//        InputStream toInputStream() {
//            return new ByteArrayInputStream( super.buf, 0, super.count )
//        }
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger include(String... includes) {
        patternSet.include(includes)
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger include(Iterable<String> includes) {
        patternSet.include(includes)
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger include(Spec<FileTreeElement> includeSpec) {
        patternSet.include(includeSpec)
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger include(Closure includeSpec) {
        patternSet.include(includeSpec)
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger exclude(String... excludes) {
        patternSet.exclude(excludes)
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger exclude(Iterable<String> excludes) {
        patternSet.exclude(excludes)
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger exclude(Spec<FileTreeElement> excludeSpec) {
        patternSet.exclude(excludeSpec)
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger exclude(Closure excludeSpec) {
        patternSet.exclude(excludeSpec)
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Input
    Set<String> getIncludes() {
        return patternSet.includes
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger setIncludes(Iterable<String> includes) {
        patternSet.includes = includes
        return this
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Input
    Set<String> getExcludes() {
        return patternSet.excludes
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ServiceFileMerger setExcludes(Iterable<String> excludes) {
        patternSet.excludes = excludes
        return this
    }
}
