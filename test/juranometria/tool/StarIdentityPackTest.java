package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled star-identity pack (issue #113) against the reviewed
 * decision (docs/decisions/star-identity.md): the packaged data
 * reproduces the decision report's counts, every checksum covers
 * every file in both directions, representative records carry their
 * exact designations, and an unsupported or foreign manifest fails
 * loudly.
 */
class StarIdentityPackTest {

    private static final String PACK = "/resources/catalog/star-identities/";

    @Test
    void theManifestDeclaresThePackTheDecisionChose() {
        Properties manifest = packagedManifest();
        StarIdentityPackMain.validateManifest(manifest);
        assertEquals("star-identities", manifest.getProperty("pack.name"));
        assertEquals("BSD-3-Clause", manifest.getProperty("license"));
        assertEquals("7e720a3de062059d4c5400a379146a601d9010e0",
                manifest.getProperty("source.commit"));
        assertTrue(manifest.getProperty("join.contract")
                        .contains("brightest-packed-component"),
                "the multi-component exception policy is declared");
        assertTrue(manifest.getProperty("names.character")
                        .contains("not-per-name-IAU-certified"),
                "traditional names never claim per-name IAU certification");
    }

    @Test
    void theCountsAgreeWithTheReviewedDecisionReport() {
        // The decision's measured join, reproduced by the generator
        // and locked here: 4,805 of 4,869 source entries join, 64
        // honestly unmatched, 201 multi-component systems attach to
        // their brightest packed component only, and 539 of the 602
        // proper names reach the pack.
        Properties manifest = packagedManifest();
        assertEquals("4869", manifest.getProperty("source.entries"));
        assertEquals("4805", manifest.getProperty("join.matched"));
        assertEquals("64", manifest.getProperty("join.unmatched"));
        assertEquals("201", manifest.getProperty("join.multi.component"));
        assertEquals("4805", manifest.getProperty("rows"));
        assertEquals("539", manifest.getProperty("rows.with.name"));
        assertEquals("1967", manifest.getProperty("rows.with.bayer"));
        assertEquals("2649", manifest.getProperty("rows.with.flamsteed"));
        assertEquals("9", manifest.getProperty(
                        "rows.constellation.invalid.in.source.carried.unknown"),
                "the source's NSV constellation fragments are counted,"
                        + " carried as unknown, never invented");
        assertEquals(4805L, rows().size(), "the CSV carries every joined row");
    }

    @Test
    void everyPackagedFileMatchesItsManifestChecksumBothDirections()
            throws Exception {
        Properties manifest = packagedManifest();
        Map<String, String> checksums = new TreeMap<>();
        for (String key : manifest.stringPropertyNames()) {
            if (key.startsWith("checksum.")) {
                checksums.put(key.substring("checksum.".length()),
                        manifest.getProperty(key));
            }
        }
        // Direction one: every checksummed file exists and matches.
        for (Map.Entry<String, String> entry : checksums.entrySet()) {
            assertEquals(entry.getValue(),
                    PinnedInputs.sha256Hex(packagedBytes(entry.getKey())),
                    entry.getKey() + " must match its manifest checksum");
        }
        // Direction two: every generated file beside the manifest is
        // checksummed - a file the manifest does not know is a loud
        // failure, not silently unverified content.
        Path packDir = Path.of("src/resources/catalog/star-identities");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(packDir),
                "generated pack sources not present in this checkout");
        try (var listing = Files.list(packDir)) {
            Set<String> files = listing
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.equals("manifest.properties"))
                    .collect(Collectors.toSet());
            assertEquals(checksums.keySet(), files,
                    "manifest checksums and pack files must agree exactly");
        }
    }

    @Test
    void representativeRecordsCarryTheirExactDesignations() {
        // The issue's named checks plus the southern sky: exact rows,
        // designations verbatim from the source (Acrux keeps its
        // component digit), the multi-component policy visible in a
        // single row per name.
        Map<String, String[]> byName = rows().stream()
                .filter(row -> !row[1].isEmpty())
                .collect(Collectors.toMap(row -> row[1], row -> row,
                        (a, b) -> a));
        assertRow(byName, "Betelgeuse", "TYC 129-1873-1", "α", "58", "Ori");
        assertRow(byName, "Rigel", "TYC 5331-1752-1", "β", "19", "Ori");
        assertRow(byName, "Polaris", "TYC 4628-237-1", "α", "1", "UMi");
        assertRow(byName, "Vega", "TYC 3105-2070-1", "α", "3", "Lyr");
        assertRow(byName, "Sirius", "TYC 5949-2777-1", "α", "9", "CMa");
        assertRow(byName, "Acrux", "TYC 8979-3464-1", "α1", "", "Cru");
        long acruxRows = rows().stream()
                .filter(row -> row[1].equals("Acrux")).count();
        assertEquals(1L, acruxRows, "one name, one star - never duplicated"
                + " across a multi-component system");
    }

    @Test
    void everyRowIsHonestNoInventedFieldsNoOrphanDesignations() {
        Set<String> tycs = new java.util.HashSet<>();
        for (String[] row : rows()) {
            assertEquals(5, row.length, "every row has the five fields");
            assertTrue(row[0].startsWith("TYC "), "keyed by TYC identifier");
            assertTrue(tycs.add(row[0]), "TYC ids are unique: " + row[0]);
            if (!row[2].isEmpty() || !row[3].isEmpty()) {
                assertTrue(!row[4].isEmpty(), "a Bayer or Flamsteed"
                        + " designation never floats without its"
                        + " constellation: " + String.join(",", row));
            }
        }
    }

    @Test
    void unsupportedOrForeignManifestsFailLoudly() {
        Properties wrongVersion = packagedManifest();
        wrongVersion.setProperty("format.version", "2");
        IllegalStateException version = assertThrows(
                IllegalStateException.class,
                () -> StarIdentityPackMain.validateManifest(wrongVersion));
        assertTrue(version.getMessage().contains("format.version"),
                "the diagnostic names the incompatibility: "
                        + version.getMessage());

        Properties foreign = packagedManifest();
        foreign.setProperty("pack.name", "bright-sky");
        assertThrows(IllegalStateException.class,
                () -> StarIdentityPackMain.validateManifest(foreign));

        Properties missing = packagedManifest();
        missing.remove("join.contract");
        assertThrows(IllegalStateException.class,
                () -> StarIdentityPackMain.validateManifest(missing));
    }

    private static void assertRow(Map<String, String[]> byName, String name,
                                  String tyc, String bayer, String flamsteed,
                                  String constellation) {
        String[] row = byName.get(name);
        assertTrue(row != null, name + " must be in the pack");
        assertEquals(tyc, row[0], name + " TYC identifier");
        assertEquals(bayer, row[2], name + " Bayer designation");
        assertEquals(flamsteed, row[3], name + " Flamsteed number");
        assertEquals(constellation, row[4], name + " constellation");
    }

    private static List<String[]> rows() {
        String csv = new String(packagedBytes("star-identities.csv"),
                StandardCharsets.UTF_8);
        List<String> lines = csv.lines().toList();
        assertEquals("tyc,name,bayer,flamsteed,constellation", lines.get(0));
        List<String[]> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            rows.add(line.split(",", -1));
        }
        return rows;
    }

    private static Properties packagedManifest() {
        Properties properties = new Properties();
        try (InputStream stream = StarIdentityPackTest.class
                .getResourceAsStream(PACK + "manifest.properties")) {
            assertTrue(stream != null, "manifest must ship on the classpath");
            properties.load(new InputStreamReader(stream,
                    StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }

    private static byte[] packagedBytes(String name) {
        try (InputStream stream = StarIdentityPackTest.class
                .getResourceAsStream(PACK + name)) {
            assertTrue(stream != null, name + " must ship on the classpath");
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
