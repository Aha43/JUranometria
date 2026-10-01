package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Every registered generator of committed evidence, and which side of
 * the interaction route it falls on (issue #428).
 *
 * <p>Derived, never listed. The generators are the evidence
 * contract's registries and the interface photographers' registry;
 * where each writes is read from those registries and from the
 * {@code docs/studies/...} paths its own class names; and whether it
 * is a <strong>chart producer</strong> is read from what is committed
 * there. A generator whose output directories hold any committed
 * renderer-drawn picture - outside the photographers' own directory,
 * whose photographs are interface evidence by registry - is a chart
 * producer: the interaction route never runs it, and a change that
 * reaches it is wide. Every other evidence generator commits only
 * reports, platform records, fixtures or inspection imagery, and is
 * <strong>reproduced</strong> on every interaction run, byte for
 * byte, so nothing it owns is taken on trust.
 *
 * <p>Fails closed: a generator whose outputs cannot be resolved is a
 * chart producer.
 */
public final class EvidenceGenerators {

    /** Which side of the interaction route a generator falls on. */
    public enum Kind {
        /** Owns a committed chart picture, or cannot be resolved. */
        CHART,
        /** Owns no chart picture; reproduced on interaction runs. */
        REPRODUCED,
        /** An interface photographer, by its registry. */
        PHOTOGRAPHER
    }

    /** One generator: its class, its kind, and where it writes. */
    public record Generator(String type, Kind kind, Set<String> outputs) {
    }

    private final Map<String, Generator> generators;

    private EvidenceGenerators(Map<String, Generator> generators) {
        this.generators = generators;
    }

    /** Every generator, by class name in slashed form. */
    public Map<String, Generator> all() {
        return Collections.unmodifiableMap(generators);
    }

    /** The generators of one kind, in slashed form. */
    public List<String> of(Kind kind) {
        List<String> found = new ArrayList<>();
        for (Generator generator : generators.values()) {
            if (generator.kind() == kind) {
                found.add(generator.type());
            }
        }
        return found;
    }

    /** The directories the generators of one kind write. */
    public Set<String> outputsOf(Kind kind) {
        Set<String> found = new TreeSet<>();
        for (Generator generator : generators.values()) {
            if (generator.kind() == kind) {
                found.addAll(generator.outputs());
            }
        }
        return found;
    }

    /**
     * Reads the registries against the compiled classes and the
     * committed studies tree.
     */
    public static EvidenceGenerators of(RenderingClosure classes, Path studies)
            throws IOException {
        Map<String, Set<String>> outputs = new LinkedHashMap<>();
        for (Map.Entry<String, String> report
                : EvidenceContractMain.reportMains().entrySet()) {
            directoryOf(outputs, report.getKey(), report.getValue());
        }
        for (Map.Entry<String, String> record
                : EvidenceContractMain.platformReports().entrySet()) {
            directoryOf(outputs, record.getKey(), record.getValue());
        }
        for (Map.Entry<String, String> writer
                : EvidenceContractMain.buildWriters().entrySet()) {
            Set<String> into = outputs.computeIfAbsent(
                    slashed(writer.getKey()), k -> new TreeSet<>());
            for (Map.Entry<String, String> promoted
                    : EvidenceContractMain.promotedDirectories().entrySet()) {
                if (promoted.getValue().equals(writer.getValue())) {
                    into.add(promoted.getKey());
                }
            }
        }
        for (String image : EvidenceContractMain.imageMains()) {
            outputs.computeIfAbsent(slashed(image), k -> new TreeSet<>());
        }
        // And whatever studies directories each generator's own class
        // names - which is how the image writers say where they write.
        for (Map.Entry<String, Set<String>> generator : outputs.entrySet()) {
            for (String text : classes.stringsOf(generator.getKey())) {
                if (text.startsWith("docs/studies/") && text.length()
                        > "docs/studies/".length()) {
                    Path named = Path.of(text);
                    if (Files.isDirectory(named)) {
                        generator.getValue().add(withSlash(text));
                    } else if (named.getParent() != null) {
                        generator.getValue().add(
                                withSlash(named.getParent().toString()));
                    }
                }
            }
        }

        List<String> pictures = chartPictures(studies);
        Map<String, Generator> generators = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> generator : outputs.entrySet()) {
            Set<String> where = generator.getValue();
            boolean ownsPicture = pictures.stream().anyMatch(
                    picture -> where.stream().anyMatch(picture::startsWith));
            Kind kind = where.isEmpty() || ownsPicture
                    ? Kind.CHART : Kind.REPRODUCED;
            generators.put(generator.getKey(), new Generator(
                    generator.getKey(), kind, Set.copyOf(where)));
        }
        for (String photographer : InterfacePhotographers.ALL.keySet()) {
            String type = "juranometria/tool/" + photographer;
            generators.put(type, new Generator(type, Kind.PHOTOGRAPHER,
                    Set.of(InterfacePhotographers.DIRECTORY)));
        }
        return new EvidenceGenerators(generators);
    }

    /**
     * Every committed renderer-drawn file outside the photographers'
     * directory: the chart pictures.
     */
    static List<String> chartPictures(Path studies) throws IOException {
        List<String> pictures = new ArrayList<>();
        if (!Files.isDirectory(studies)) {
            return pictures;
        }
        try (Stream<Path> tree = Files.walk(studies)) {
            for (Path file : (Iterable<Path>) tree.filter(Files::isRegularFile)
                    .sorted()::iterator) {
                // Relative to the repository, whatever the tree was
                // read from: ownership is decided by comparing these
                // with the registries' repository-relative paths.
                String path = ("docs/studies/" + studies.relativize(file))
                        .replace('\\', '/');
                if (path.startsWith(InterfacePhotographers.DIRECTORY)) {
                    continue;
                }
                if ("renderer-drawn".equals(TestEvidenceScan.artifactClass(
                        file.getFileName().toString()))) {
                    pictures.add(path);
                }
            }
        }
        return pictures;
    }

    private static void directoryOf(Map<String, Set<String>> outputs,
                                    String generator, String file) {
        Path parent = Path.of(file).getParent();
        Set<String> into = outputs.computeIfAbsent(slashed(generator),
                k -> new TreeSet<>());
        if (parent != null) {
            into.add(withSlash(parent.toString()));
        }
    }

    static String slashed(String dotted) {
        return dotted.replace('.', '/');
    }

    private static String withSlash(String directory) {
        String forward = directory.replace('\\', '/');
        return forward.endsWith("/") ? forward : forward + "/";
    }
}
