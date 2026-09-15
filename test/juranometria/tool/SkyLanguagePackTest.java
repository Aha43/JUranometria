package juranometria.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import juranometria.geo.Constellation;
import juranometria.geo.ConstellationGeography;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A language arrives as data, or it does not arrive (issue #347).
 *
 * <p>The gate has to show that a contributor can add a sky language
 * without private material and without touching code. The fixture
 * pack is how that is demonstrated: adding a data file and a
 * manifest is enough to be resolved, and every way of getting it
 * wrong is refused at the data boundary rather than surfacing later
 * as a blank label or a mistranslated identity.
 *
 * <p>The fixture is honest about being a fixture. Its manifest says
 * {@code fixture=true} and leaves the source fields empty rather
 * than inventing a publication, and production loading refuses it
 * for exactly that reason - which is what stops the schema being
 * satisfiable by a plausible-looking lie.
 */
class SkyLanguagePackTest {

    private static final Path DATA =
            Path.of("test/resources/sky-language/x-juranometria-test.tsv");

    private static final Path MANIFEST =
            Path.of("test/resources/sky-language/x-juranometria-test.manifest");

    /** The canonical identities, from the identity layer alone. */
    private static Set<String> canonical() {
        Set<String> ids = new TreeSet<>();
        for (Constellation each
                : ConstellationGeography.load().constellations()) {
            ids.add(each.id());
        }
        return ids;
    }

    /** Adding data is enough to be resolved. */
    @Test
    void aPackIsReadFromDataAloneAndNamesEveryConstellation()
            throws Exception {
        SkyLanguagePack.Pack pack = SkyLanguagePack.read(DATA, MANIFEST,
                canonical(), false);

        assertEquals("x-juranometria-test", pack.tag());
        assertEquals(88, pack.names().size(),
                "all 88 canonical constellations are named exactly"
                        + " once");
        assertEquals(canonical(), new TreeSet<>(pack.names().keySet()),
                "and they are the canonical identities, not a"
                        + " parallel list of the contributor's own");
        assertTrue(pack.names().values().stream()
                        .anyMatch(name -> name.chars()
                                .anyMatch(c -> c > 127)),
                "the fixture carries non-ASCII letters, so glyph"
                        + " coverage is exercised rather than assumed");
        assertTrue(pack.names().values().stream()
                        .allMatch(name -> name.startsWith("zz")),
                "and its names are unmistakably synthetic: no reader"
                        + " could take one for a constellation");
    }

    /** The deterministic fallback, and what never falls back. */
    @Test
    void anAbsentNameFallsBackToLatinAndAnIdentityNeverDoes()
            throws Exception {
        SkyLanguagePack.Pack pack = SkyLanguagePack.read(DATA, MANIFEST,
                canonical(), false);

        assertEquals("zzæ-and-testnavn", pack.nameOf("And", "Andromeda"),
                "a named constellation uses the pack");
        assertEquals("Andromeda", pack.nameOf("Xyz", "Andromeda"),
                "an unnamed one falls back to the official Latin name,"
                        + " so a page never shows a blank where a name"
                        + " belongs");
        // The abbreviation is canonical data, not a translation.
        // There is nothing for it to fall back to, which is why no
        // pack may supply one.
        assertTrue(pack.names().keySet().stream()
                        .allMatch(id -> canonical().contains(id)),
                "a pack names constellations; it does not get to say"
                        + " what they are called canonically");
    }

    /** Every way of being nearly right is refused. */
    @Test
    void anIncompleteOrInventedPackIsRefusedAtTheDataBoundary()
            throws Exception {
        List<String> rows = Files.readAllLines(DATA);

        SkyLanguagePack.Refused missing = assertThrows(
                SkyLanguagePack.Refused.class,
                () -> read(rows.subList(0, rows.size() - 1)),
                "eighty-seven names is not a language pack");
        assertTrue(missing.getMessage().contains("unnamed"),
                missing.getMessage());

        SkyLanguagePack.Refused invented = assertThrows(
                SkyLanguagePack.Refused.class,
                () -> read(Stream.concat(rows.stream(),
                        Stream.of("Xyz\tzzq-xyz-testnavn")).toList()),
                "an eighty-ninth constellation does not exist");
        assertTrue(invented.getMessage().contains("do not exist"),
                invented.getMessage());

        SkyLanguagePack.Refused twice = assertThrows(
                SkyLanguagePack.Refused.class,
                () -> read(Stream.concat(rows.stream(),
                        Stream.of("And\tzzq-and-again")).toList()),
                "a duplicate means one constellation was named twice"
                        + " and another not at all");
        assertTrue(twice.getMessage().contains("named twice"),
                twice.getMessage());
    }

    /** Production refuses a pack that admits it is a fixture. */
    @Test
    void theFixtureCannotBeLoadedAsALanguage() {
        SkyLanguagePack.Refused refused = assertThrows(
                SkyLanguagePack.Refused.class,
                () -> SkyLanguagePack.read(DATA, MANIFEST, canonical(),
                        true),
                "a pack declaring itself a fixture is not a language a"
                        + " reader may choose");
        assertTrue(refused.getMessage().contains("test fixture"),
                refused.getMessage());
    }

    /** A real contribution must say where its names came from. */
    @Test
    void aNonFixturePackWithoutProvenanceIsRefused() throws Exception {
        Path manifest = Files.createTempFile("pack", ".manifest");
        Files.writeString(manifest, """
                schema=1
                tag=xx-invented
                display-name=A pack with no account of itself
                rows=88
                source=
                licence=
                retrieved=
                transformations=
                """);
        try {
            SkyLanguagePack.Refused refused = assertThrows(
                    SkyLanguagePack.Refused.class,
                    () -> SkyLanguagePack.read(DATA, manifest,
                            canonical(), false));
            for (String required : List.of("source", "licence",
                    "retrieved", "transformations")) {
                assertTrue(refused.getMessage().contains(required),
                        "the refusal names " + required + ": "
                                + refused.getMessage());
            }
        } finally {
            Files.deleteIfExists(manifest);
        }
    }

    /** The fixture never reaches a reader's machine. */
    @Test
    void theFixtureIsAbsentFromEverythingThatShips() throws Exception {
        assertFalse(Files.exists(Path.of(
                        "src/resources/sky-language/x-juranometria-test.tsv")),
                "the fixture is not among the packaged resources");
        Path packaged = Path.of("build/classes/resources");
        if (Files.isDirectory(packaged)) {
            try (Stream<Path> walk = Files.walk(packaged)) {
                assertTrue(walk.noneMatch(p -> p.getFileName().toString()
                                .startsWith("x-juranometria-test")),
                        "nor does it reach the build output, which is"
                                + " what the application image is made"
                                + " from");
            }
        }
    }

    private static SkyLanguagePack.Pack read(List<String> rows)
            throws Exception {
        Path data = Files.createTempFile("pack", ".tsv");
        try {
            Files.write(data, rows);
            return SkyLanguagePack.read(data, MANIFEST, canonical(), false);
        } finally {
            Files.deleteIfExists(data);
        }
    }
}
