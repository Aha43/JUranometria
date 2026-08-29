package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import juranometria.catalog.PackManifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards over the generated bright-sky pack as bundled on the classpath:
 * the manifest, per-file checksums, representative sky records, and
 * whole-pack identifier uniqueness.
 */
class BrightSkyPackTest {

    static final String PACK = "/resources/catalog/bright-sky/";

    private static PackManifest manifest() {
        return PackManifest.parse(new InputStreamReader(
                resource(PACK + "manifest.properties"), StandardCharsets.UTF_8),
                "bright-sky manifest");
    }

    @Test
    void theManifestDeclaresThePackTheDecisionChose() {
        PackManifest manifest = manifest();
        assertEquals("bright-sky", manifest.packName());
        assertEquals("all-sky", manifest.coverage());
        assertEquals(8.0, manifest.starLimitVmag());
        assertEquals("45630", manifest.entries().get("rows.stars.total"));
        assertEquals("13371", manifest.entries().get("rows.dsos.total"));
    }

    @Test
    void representativeRecordsLandInTheirPredictedTiles() {
        assertTrue(tileContains("r02-d2/dsos.csv",
                        "NGC 1976,M 42|Great Orion Nebula|Orion Nebula,Cl+N,83.818667,-5.389667,90.00,60.00,,4.00,4.00,1"),
                "M42 with its aliases in the Orion tile");
        assertTrue(tileContains("r01-d3/dsos.csv", "Mel022,M 45|Pleiades,OCl,"),
                "the Pleiades in their tile");
        assertTrue(tileContains("r01-d5/stars.csv", "TYC 4628-237-1,37.946619,89.264135,1.98"),
                "Polaris in the polar tile via the fallback position");
        assertTrue(tileContains("r00-d0/dsos.csv", "NGC 104,47 Tuc Cluster,GCl,6.022333,-72.081444,31.80,,,4.09,5.78,2"),
                "47 Tucanae with its honestly empty minor axis and angle");
        assertTrue(tileContains("r00-d4/dsos.csv", "NGC 7801,,OCl,0.089333,50.745000,4.20,,,,,2"),
                "a positioned object without any photometry ships with empty fields");
        assertTrue(tileContains("r00-d4/dsos.csv", "NGC 224,M 31|Andromeda Galaxy,G,"),
                "M31 unchanged in its home tile");
        assertTrue(tileContains("r05-d2/dsos.csv", "NGC 3110,NGC 3122|NGC 3518,G,"),
                "comma-separated cross references become separate aliases");
        assertTrue(lines("tiles/r11-d4/stars.csv").size() > 100,
                "the RA-wrap column is populated");
    }

    @Test
    void everyTileFileMatchesItsManifestChecksumAndIdsAreUnique() throws IOException {
        PackManifest manifest = manifest();
        Set<String> starIds = new HashSet<>();
        Set<String> dsoIds = new HashSet<>();
        int checksummed = 0;
        for (var entry : manifest.entries().entrySet()) {
            if (!entry.getKey().startsWith("checksum.tiles/")) {
                continue;
            }
            String tileFile = entry.getKey().substring("checksum.".length());
            byte[] bytes;
            try (InputStream stream = resource(PACK + tileFile)) {
                bytes = stream.readAllBytes();
            }
            assertEquals(entry.getValue(), PinnedInputs.sha256Hex(bytes),
                    tileFile + " must match its manifest checksum");
            checksummed++;
            boolean stars = tileFile.endsWith("stars.csv");
            Set<String> ids = stars ? starIds : dsoIds;
            int expectedFields = stars ? 4 : 11;
            for (String line : new String(bytes, StandardCharsets.UTF_8).split("\n")) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                assertEquals(expectedFields, fields.length,
                        "malformed row in " + tileFile + ": " + line);
                assertTrue(ids.add(fields[0]),
                        "duplicate identifier in " + tileFile + ": " + line);
            }
        }
        assertTrue(checksummed >= 72, "every tile file carries a checksum");
        assertEquals(45630, starIds.size(), "star total matches the manifest");
        assertEquals(13371, dsoIds.size(), "DSO total matches the manifest");
    }

    @Test
    void everyTileFileOnDiskHasAManifestEntry() throws Exception {
        // The reverse direction of the checksum guard (Codex review, PR #45):
        // a stale file from an earlier generation would ship unchecksummed.
        java.net.URL manifestUrl = BrightSkyPackTest.class
                .getResource(PACK + "manifest.properties");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                "file".equals(manifestUrl.getProtocol()),
                "directory-classpath run required to walk the pack");
        java.nio.file.Path packDir = java.nio.file.Path.of(manifestUrl.toURI()).getParent();
        Set<String> manifestFiles = new HashSet<>();
        for (String key : manifest().entries().keySet()) {
            if (key.startsWith("checksum.")) {
                manifestFiles.add(key.substring("checksum.".length()));
            }
        }
        try (var walk = java.nio.file.Files.walk(packDir.resolve("tiles"))) {
            for (java.nio.file.Path file : walk.filter(java.nio.file.Files::isRegularFile).toList()) {
                String relative = packDir.relativize(file).toString().replace('\\', '/');
                assertTrue(manifestFiles.contains(relative),
                        "unchecksummed stale file in the pack: " + relative);
            }
        }
    }

    @Test
    void noticesAndProvenanceShipInsideThePack() {
        assertTrue(text(PACK + "NOTICE-tycho2.md").contains("may not be used commercially"));
        assertTrue(text(PACK + "NOTICE-openngc.md").contains("CC-BY-SA-4.0"));
        assertTrue(text(PACK + "LICENSE-CC-BY-SA-4.0.txt")
                .contains("Attribution-ShareAlike 4.0"));
        assertTrue(text(PACK + "PROVENANCE.md").contains("main-catalogue-wins"));
    }

    private static boolean tileContains(String tileFile, String prefix) {
        return lines("tiles/" + tileFile).stream().anyMatch(line -> line.startsWith(prefix));
    }

    private static List<String> lines(String packRelative) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource(PACK + packRelative), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return lines;
    }

    private static String text(String resource) {
        try (InputStream stream = resource(resource)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream resource(String name) {
        InputStream stream = BrightSkyPackTest.class.getResourceAsStream(name);
        assertNotNull(stream, name + " must ship on the classpath");
        return stream;
    }
}
