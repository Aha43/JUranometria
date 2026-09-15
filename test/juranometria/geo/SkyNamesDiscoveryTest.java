package juranometria.geo;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A language arrives by being installed (Sprint 33, issue #348).
 *
 * <p>The contribution contract promises that <em>adding a pack is
 * enough</em>. Nothing lists the languages: each pack under
 * {@code resources/sky-language} carries its own names and its own
 * provenance, the build writes a sorted index of what it found, and
 * discovery reads that index. These hold that promise from both
 * ends - a pack nobody wrote Java for is offered, and a pack that is
 * malformed, duplicated or a test fixture is refused before a reader
 * could select it.
 *
 * <p>Rejection happens at the INDEXING boundary, not at runtime. A
 * duplicate tag discovered while resolving would mean one pack had
 * already silently won; refusing to write the index at all is what
 * makes the failure visible to whoever built it.
 */
class SkyNamesDiscoveryTest {

    /** Where the build puts what it ships. */
    private static final Path BUILT =
            Path.of("build/classes/resources/sky-language");

    /** Discovery finds the shipped pack, and only validated ones. */
    @Test
    void discoveryOffersTheInstalledPacksAndNothingElse() {
        SkyNames names = SkyNames.discover();

        assertTrue(names.chartLanguages().contains("nb-NO"),
                "the Norwegian pack ships and is offered: "
                        + names.chartLanguages());
        assertEquals("Norsk bokmal", names.displayName("nb-NO"),
                "under the name it gives itself");
        assertFalse(names.chartLanguages().contains("x-juranometria-test"),
                "and the test fixture is not a language a reader can"
                        + " choose: " + names.chartLanguages());
        List<String> sorted = new ArrayList<>(names.chartLanguages());
        java.util.Collections.sort(sorted);
        assertEquals(sorted, names.chartLanguages(),
                "offered in a stable order, not the order a jar"
                        + " happens to store its entries");
    }

    /**
     * A pack this code has never heard of is resolved.
     *
     * <p>Production-shaped: its own names, its own manifest, its own
     * tag. No constant, no enum, no switch anywhere in Java mentions
     * it - it works because it was installed, which is the whole
     * contract.
     */
    @Test
    void aPackNobodyWroteJavaForIsResolved() throws Exception {
        Path shipped = Files.createTempDirectory("packs");
        try {
            write(shipped.resolve("xx-invented.manifest"), """
                    schema=1
                    tag=xx-invented
                    display-name=An invented sky
                    names=xx-invented.tsv
                    status=verified
                    source=a test
                    licence=none
                    retrieved=2026-09-15
                    transformations=none
                    """);
            write(shipped.resolve("xx-invented.tsv"),
                    "IAU\tName\nSgr\tThe Archer's Own Name\n");
            index(shipped);

            SkyNames names = SkyNames.discover(from(shipped));
            assertEquals(List.of("xx-invented"), names.chartLanguages(),
                    "installed, therefore offered");
            assertEquals("The Archer's Own Name",
                    names.namesFor("xx-invented",
                            Map.of("Sgr", "Sagittarius")).get("Sgr"),
                    "and it names the sky without one line of Java"
                            + " being written for it");
        } finally {
            delete(shipped);
        }
    }

    /** Two packs claiming one language are refused at indexing. */
    @Test
    void duplicateTagsAreRefusedBeforeAnythingCouldSelectOne()
            throws Exception {
        Path packs = Files.createTempDirectory("packs");
        try {
            for (String file : List.of("aa", "bb")) {
                write(packs.resolve(file + ".manifest"), """
                        schema=1
                        tag=zz-contested
                        display-name=Contested
                        status=verified
                        source=a test
                        licence=none
                        retrieved=2026-09-15
                        transformations=none
                        """);
                write(packs.resolve(file + ".tsv"), "IAU\tName\nSgr\tOne\n");
            }
            IllegalStateException refused = assertThrows(
                    IllegalStateException.class, () -> index(packs),
                    "two packs cannot claim one language: whichever"
                            + " resolved first would silently win");
            assertTrue(refused.getMessage().contains("zz-contested"),
                    refused.getMessage());
        } finally {
            delete(packs);
        }
    }

    /** A manifest with no tag, or no names beside it, is refused. */
    @Test
    void anIncompletePackIsRefusedAtIndexing() throws Exception {
        Path noTag = Files.createTempDirectory("packs");
        try {
            write(noTag.resolve("quiet.manifest"), "schema=1\n");
            assertTrue(assertThrows(IllegalStateException.class,
                            () -> index(noTag)).getMessage()
                            .contains("no language tag"),
                    "a pack that does not say which language it is"
                            + " could never be selected");
        } finally {
            delete(noTag);
        }

        Path noData = Files.createTempDirectory("packs");
        try {
            write(noData.resolve("empty.manifest"), """
                    schema=1
                    tag=zz-nameless
                    """);
            assertTrue(assertThrows(IllegalStateException.class,
                            () -> index(noData)).getMessage()
                            .contains("not beside it"),
                    "an indexed language the atlas cannot draw is"
                            + " worse than one it never offered");
        } finally {
            delete(noData);
        }
    }

    /** A fixture that reaches the build output is refused. */
    @Test
    void aFixtureInTheBuildOutputIsRefused() throws Exception {
        Path packs = Files.createTempDirectory("packs");
        try {
            write(packs.resolve("x-test.manifest"), """
                    schema=1
                    tag=x-juranometria-test
                    display-name=Fixture
                    fixture=true
                    """);
            write(packs.resolve("x-test.tsv"), "IAU\tName\nSgr\tzz\n");
            assertTrue(assertThrows(IllegalStateException.class,
                            () -> index(packs)).getMessage()
                            .contains("test fixture"),
                    "a fixture must not ship, and the build says so"
                            + " rather than a reader discovering it");
        } finally {
            delete(packs);
        }
    }

    /**
     * An uninstalled language resolves; it does not become selectable.
     *
     * <p>Discovery governs availability, fallback governs resolution.
     * Conflating them would let any tag a caller invented appear in a
     * selector as though the atlas could draw it.
     */
    @Test
    void anUninstalledLanguageFallsBackWithoutBecomingAChoice() {
        SkyNames names = SkyNames.discover();
        Map<String, String> latin = latinNames();

        Map<String, String> resolved = names.namesFor("sv-SE", latin);
        assertEquals(latin.size(), resolved.size(),
                "a complete map: every constellation asked about is"
                        + " named, so no page can show a blank");
        assertEquals(latin, resolved,
                "all of them the official Latin names");
        assertFalse(names.chartLanguages().contains("sv-SE"),
                "and Swedish is not offered, because no pack is"
                        + " installed: " + names.chartLanguages());
    }

    /** The map the scene receives is keyed and ordered by identity. */
    @Test
    void namesAreKeyedAndOrderedByCanonicalIdentity() {
        SkyNames names = SkyNames.discover();
        Map<String, String> latin = latinNames();

        List<String> inLatin =
                new ArrayList<>(names.namesFor(SkyNames.LATIN, latin)
                        .keySet());
        List<String> inNorwegian =
                new ArrayList<>(names.namesFor("nb-NO", latin).keySet());

        assertEquals(inLatin, inNorwegian,
                "chart language changes values, never keys or their"
                        + " order - iteration order reaches the"
                        + " renderer, and overlapping names must stack"
                        + " identically whatever language is chosen");
        assertEquals(new ArrayList<>(new TreeMap<>(latin).keySet()),
                inNorwegian, "and the order is the identities' own");
    }

    /** The fixture is absent from everything that ships. */
    @Test
    void theFixtureIsAbsentFromTheBuildOutput() throws Exception {
        assertFalse(Files.exists(Path.of(
                        "src/resources/sky-language/x-juranometria-test.tsv")),
                "not among the packaged resources");
        if (Files.isDirectory(BUILT)) {
            try (Stream<Path> walk = Files.walk(BUILT)) {
                assertTrue(walk.noneMatch(p -> p.getFileName().toString()
                                .startsWith("x-juranometria-test")),
                        "nor in the build output the image is made"
                                + " from");
            }
            Path index = BUILT.resolve("index.tsv");
            if (Files.isRegularFile(index)) {
                assertFalse(Files.readString(index)
                                .contains("x-juranometria-test"),
                        "nor in the generated index");
            }
        }
    }

    /** The shipped pack keeps its licence and attribution beside it. */
    @Test
    void theShippedPackCarriesItsOwnProvenance() throws Exception {
        String manifest = Files.readString(
                Path.of("src/resources/sky-language/nb-NO.manifest"));

        for (String required : List.of("source=", "licence=", "retrieved=",
                "transformations=", "CC BY-SA")) {
            assertTrue(manifest.contains(required),
                    "the shipped names carry their own account: "
                            + required + " is missing, and provenance"
                            + " that lives only in git history is"
                            + " provenance a reader of the resource"
                            + " cannot see");
        }
    }

    private static Map<String, String> latinNames() {
        Map<String, String> latin = new TreeMap<>();
        for (Constellation each
                : ConstellationGeography.load().constellations()) {
            latin.put(each.id(), each.latinName());
        }
        return latin;
    }

    /** Runs the build's own indexer over a directory of packs. */
    private static void index(Path packs) throws Exception {
        Path root = packs.getParent().resolve(
                packs.getFileName() + "-root");
        Path into = root.resolve("resources/sky-language");
        Files.createDirectories(into);
        try (Stream<Path> files = Files.list(packs)) {
            for (Path file : files.toList()) {
                Files.copy(file, into.resolve(file.getFileName()),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        try {
            juranometria.tool.SkyLanguageIndexMain.main(
                    new String[] {root.toString()});
            Files.copy(into.resolve("index.tsv"),
                    packs.resolve("index.tsv"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            delete(root);
        }
    }

    /** Reads packs from a directory, as the classpath would. */
    private static SkyNames.Resources from(Path packs) {
        return path -> {
            Path file = packs.resolve(
                    path.substring(path.lastIndexOf('/') + 1));
            try {
                return Files.isRegularFile(file)
                        ? new ByteArrayInputStream(Files.readAllBytes(file))
                        : null;
            } catch (java.io.IOException cannotRead) {
                throw new IllegalStateException(cannotRead);
            }
        };
    }

    private static void write(Path file, String text) throws Exception {
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static void delete(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
        } catch (java.io.IOException ignored) {
            // A temporary directory that outlives the test is not a
            // failure of what the test is about.
        }
    }
}
