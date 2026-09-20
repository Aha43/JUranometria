package juranometria.ui.language;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the controls can speak, and where that answer comes from
 * (Sprint 33, issue #348).
 *
 * <p>English is offered because a descriptor ships, not because no
 * other language does. The distinction is the whole subject of this
 * file: an implicit English fallback would give the right answer
 * today from a mechanism that had never run, and the first person to
 * add a second interface language would discover that only when it
 * failed to appear.
 *
 * <p>The other half is separation. A Norwegian chart pack must not
 * make a Norwegian interface appear - the reader was promised the two
 * settings are independent. That is held here structurally rather
 * than by intention: different directories, different schema markers,
 * and indexes with different column counts, so neither discovery can
 * read the other's and answer plausibly.
 */
class InterfaceLanguagesTest {

    /** What this build actually offers. */
    @Test
    void theBuildOffersTheInterfacesItShipsDescriptorsFor() {
        InterfaceLanguages offered = InterfaceLanguages.discover();

        assertEquals(List.of("en", "nb-NO"), offered.tags(),
                "two interface descriptors ship, so two interface"
                        + " languages are offered - sorted by tag, not"
                        + " by the order a directory enumerates");
        assertEquals("English", offered.displayName("en"),
                "under the name it gives itself - descriptor data,"
                        + " not a decision this code makes");
        assertEquals("Norsk bokm\u00e5l", offered.displayName("nb-NO"),
                "and so does the one #350 added");
    }

    /**
     * English comes from the descriptor, not from an empty registry.
     *
     * <p>The mutation this file exists for. If discovery answered
     * "en" when it found nothing, this test would pass with the
     * descriptor deleted - and so would the application, until a
     * second language was added and silently ignored.
     */
    @Test
    void anAbsentIndexIsReportedRatherThanAnsweredWithEnglish() {
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> InterfaceLanguages.discover(path -> null),
                "a build with no interface index has not run its"
                        + " indexer, and saying so beats guessing");
        assertTrue(refused.getMessage().contains("no")
                        && refused.getMessage().contains("interface"),
                refused.getMessage());
        assertFalse(refused.getMessage().isBlank());
    }

    /** An index with no language in it is refused too. */
    @Test
    void anEmptyIndexIsRefusedRatherThanTreatedAsEnglish() {
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> InterfaceLanguages.discover(
                        from("tag\tdisplay-name\n")),
                "an index listing nothing would leave the controls"
                        + " speaking no language at all");
        assertTrue(refused.getMessage().contains("no language"),
                refused.getMessage());
    }

    /**
     * A language nobody wrote Java for is offered.
     *
     * <p>The same contract the chart packs keep: it works because it
     * was registered, not because a constant mentions it.
     */
    @Test
    void anInterfaceNobodyWroteJavaForIsOffered() throws Exception {
        Path shipped = Files.createTempDirectory("interfaces");
        try {
            // No strings of its own: a language may register and read
            // entirely in English, which is how a translation starts
            // (#350). Declaring a strings file that does not ship is
            // what the indexer refuses.
            write(shipped.resolve("xx-invented.manifest"), """
                    interface-schema=1
                    tag=xx-invented
                    display-name=An invented tongue
                    status=draft
                    """);
            index(shipped);

            InterfaceLanguages offered =
                    InterfaceLanguages.discover(from(shipped));
            assertEquals(List.of("xx-invented"), offered.tags(),
                    "registered, therefore offered");
            assertEquals("An invented tongue",
                    offered.displayName("xx-invented"),
                    "without one line of Java written for it");
        } finally {
            delete(shipped);
        }
    }

    /** Offered in a stable order, whatever the disk says. */
    @Test
    void languagesAreOfferedInAStableOrder() throws Exception {
        Path shipped = Files.createTempDirectory("interfaces");
        try {
            for (String tag : List.of("zz-last", "aa-first", "mm-mid")) {
                write(shipped.resolve(tag + ".manifest"),
                        "interface-schema=1\ntag=" + tag
                                + "\ndisplay-name=" + tag + "\n");
            }
            index(shipped);

            assertEquals(List.of("aa-first", "mm-mid", "zz-last"),
                    InterfaceLanguages.discover(from(shipped)).tags(),
                    "a selector listing its choices in one order here"
                            + " and another there is a difference a"
                            + " reader can see and nothing explains");
        } finally {
            delete(shipped);
        }
    }

    // ---- the separation, held structurally -------------------------

    /**
     * A chart pack cannot register an interface.
     *
     * <p>The defect the whole separation exists to prevent, attempted
     * directly: a pack manifest, complete and valid on its own side,
     * dropped into the interface directory.
     */
    @Test
    void aChartPackCannotRegisterAnInterfaceLanguage() throws Exception {
        Path shipped = Files.createTempDirectory("interfaces");
        try {
            write(shipped.resolve("nb-NO.manifest"), """
                    schema=1
                    tag=nb-NO
                    display-name=Norsk bokmal
                    names=nb-NO.tsv
                    status=verified
                    """);
            write(shipped.resolve("nb-NO.tsv"), "IAU\tName\nSgr\tSkytten\n");

            IllegalStateException refused = assertThrows(
                    IllegalStateException.class, () -> index(shipped),
                    "a Norwegian sky must not imply Norwegian menus:"
                            + " the reader chooses those separately");
            assertTrue(refused.getMessage().contains("interface-schema"),
                    refused.getMessage());
        } finally {
            delete(shipped);
        }
    }

    /**
     * Neither discovery can read the other's index.
     *
     * <p>Not "does not today" - cannot. The chart index has three
     * columns and this one has two, so each rejects the other's rows
     * as wrong-shaped rather than half-reading them into a plausible
     * answer built from a filename.
     */
    @Test
    void theTwoIndexesCannotBeReadForEachOther() {
        String chartIndex = "tag\tnames\tdisplay-name\n"
                + "nb-NO\tnb-NO.tsv\tNorsk bokmal\n";
        String interfaceIndex = "tag\tdisplay-name\nen\tEnglish\n";

        assertThrows(IllegalStateException.class,
                () -> InterfaceLanguages.discover(from(chartIndex)),
                "the chart index offers this nothing it can use, and"
                        + " an unusable index is reported rather than"
                        + " read as an empty one");

        juranometria.geo.SkyNames names =
                juranometria.geo.SkyNames.discover(
                        chartPath -> chartPath.endsWith("index.tsv")
                                ? new ByteArrayInputStream(interfaceIndex
                                        .getBytes(StandardCharsets.UTF_8))
                                : null);
        assertEquals(List.of(), names.chartLanguages(),
                "and an interface index registers no chart language,"
                        + " so en cannot become a sky the atlas claims"
                        + " it can name");
    }

    /** Two descriptors for one language are refused at indexing. */
    @Test
    void duplicateTagsAreRefusedBeforeAnythingCouldSelectOne()
            throws Exception {
        Path shipped = Files.createTempDirectory("interfaces");
        try {
            for (String file : List.of("aa", "bb")) {
                write(shipped.resolve(file + ".manifest"), """
                        interface-schema=1
                        tag=zz-contested
                        display-name=Contested
                        """);
            }
            IllegalStateException refused = assertThrows(
                    IllegalStateException.class, () -> index(shipped));
            assertTrue(refused.getMessage().contains("zz-contested"),
                    refused.getMessage());
        } finally {
            delete(shipped);
        }
    }

    /** An empty directory fails the build, with a message. */
    @Test
    void aBuildWithNoDescriptorFailsWhereSomebodyBuiltIt()
            throws Exception {
        Path empty = Files.createTempDirectory("interfaces");
        try {
            IllegalStateException refused = assertThrows(
                    IllegalStateException.class, () -> index(empty),
                    "no chart pack is a legitimate build; no"
                            + " interface is not, and the difference"
                            + " belongs where somebody can act on it");
            assertTrue(refused.getMessage().contains("interface"),
                    refused.getMessage());
        } finally {
            delete(empty);
        }
    }

    /** A fixture that reaches the build output is refused. */
    @Test
    void aFixtureInTheBuildOutputIsRefused() throws Exception {
        Path shipped = Files.createTempDirectory("interfaces");
        try {
            write(shipped.resolve("x-test.manifest"), """
                    interface-schema=1
                    tag=x-juranometria-test
                    display-name=Fixture
                    fixture=true
                    """);
            assertTrue(assertThrows(IllegalStateException.class,
                            () -> index(shipped)).getMessage()
                            .contains("test fixture"),
                    "a fixture must not ship, and the build says so"
                            + " rather than a reader discovering it");
        } finally {
            delete(shipped);
        }
    }

    /** The shipped descriptor states why it exists before #350. */
    @Test
    void theShippedDescriptorSaysWhatItIsAndIsNotYet() throws Exception {
        String descriptor = Files.readString(Path.of(
                "src/resources/interface-language/en.manifest"));

        assertTrue(descriptor.contains("interface-schema=1")
                        && descriptor.contains("tag=en")
                        && descriptor.contains("display-name=English"),
                "the fields discovery reads");
        assertTrue(descriptor.contains("strings=en.properties"),
                "and says where its words live. #348 shipped this"
                        + " saying strings=in-code, which was true"
                        + " then; #350 replaced a stated fact rather"
                        + " than discovering one, which is what"
                        + " registering English before it had strings"
                        + " was for");
    }

    // ---- helpers ---------------------------------------------------

    /** Runs the build's own indexer over a directory of descriptors. */
    private static void index(Path descriptors) throws Exception {
        Path root = descriptors.getParent().resolve(
                descriptors.getFileName() + "-root");
        Path into = root.resolve("resources/interface-language");
        Files.createDirectories(into);
        try (Stream<Path> files = Files.list(descriptors)) {
            for (Path file : files.toList()) {
                Files.copy(file, into.resolve(file.getFileName()),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        try {
            juranometria.tool.InterfaceLanguageIndexMain.main(
                    new String[] {root.toString()});
            Files.copy(into.resolve("index.tsv"),
                    descriptors.resolve("index.tsv"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            delete(root);
        }
    }

    /** Reads descriptors from a directory, as the classpath would. */
    private static InterfaceLanguages.Resources from(Path descriptors) {
        return path -> {
            Path file = descriptors.resolve(
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

    /** Serves one index body, whatever is asked for. */
    private static InterfaceLanguages.Resources from(String index) {
        return path -> new ByteArrayInputStream(
                index.getBytes(StandardCharsets.UTF_8));
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
