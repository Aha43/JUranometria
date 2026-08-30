package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarIdentityStudyTest {

    @Test
    void aCorruptStarNamesInputIsRejectedBeforeAnyMeasurement() throws Exception {
        // The gate's rule: no measurement from unverified inputs. A
        // corrupt or truncated starnames.json fails its pinned SHA-256
        // with an actionable diagnostic, never a silent wrong report.
        Path corrupt = Files.createTempFile("starnames-corrupt", ".json");
        try {
            Files.writeString(corrupt, "{\"27989\": {\"name\": \"Betelgeuse\"}");
            IllegalStateException failure = assertThrows(
                    IllegalStateException.class,
                    () -> StarIdentityStudyMain.verifyStarnames(corrupt));
            assertTrue(failure.getMessage().contains("SHA-256"),
                    "the diagnostic names the check: " + failure.getMessage());
            assertTrue(failure.getMessage().contains("download"),
                    "the diagnostic names the recovery command");
        } finally {
            Files.deleteIfExists(corrupt);
        }
    }

    @Test
    void brightSkyVerificationIsCompleteInBothDirections() throws Exception {
        // Codex review, PR #118 (P1 and follow-up P2): the magnitudes
        // that pick a multi-component system's winning component come
        // from the bright-sky tiles - pack output, not pinned raw
        // input. The join must refuse, each with its own named
        // diagnostic: a tampered declared tile, a declared tile that
        // is missing, a stray tile the manifest does not declare, and
        // a manifest that is not the bright-sky pack at all. None may
        // silently re-attach an identity while every locked count
        // still passes.
        Path real = Path.of("src/resources/catalog/bright-sky");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(real),
                "bright-sky pack sources not present in this checkout");
        String tileText = Files.readString(
                real.resolve("tiles/r00-d0/stars.csv"));

        Path pack = tempPack(real, "tiles/r00-d0/stars.csv");
        try {
            // A valid one-tile pack passes and yields magnitudes.
            Map<String, Double> magnitudes =
                    StarIdentityStudyMain.packMagnitudes(pack);
            assertTrue(!magnitudes.isEmpty(), "the verified tile is read");

            // Tampered declared tile: checksum refusal names the tile.
            Files.writeString(pack.resolve("tiles/r00-d0/stars.csv"),
                    tileText + "TYC 9999-9999-9,0.0,0.0,0.01\n");
            IllegalStateException tampered = assertThrows(
                    IllegalStateException.class,
                    () -> StarIdentityStudyMain.packMagnitudes(pack));
            assertTrue(tampered.getMessage().contains("checksum")
                            && tampered.getMessage().contains("r00-d0"),
                    "tampering is named: " + tampered.getMessage());
            Files.writeString(pack.resolve("tiles/r00-d0/stars.csv"),
                    tileText);

            // Stray undeclared tile: refused even though it verifies
            // nowhere - unmanifested content is never read.
            Path stray = Files.createDirectories(
                    pack.resolve("tiles/r99-d9")).resolve("stars.csv");
            Files.writeString(stray, tileText);
            IllegalStateException undeclared = assertThrows(
                    IllegalStateException.class,
                    () -> StarIdentityStudyMain.packMagnitudes(pack));
            assertTrue(undeclared.getMessage().contains("no checksum"),
                    "a stray tile is refused: " + undeclared.getMessage());
            Files.delete(stray);

            // Missing declared tile: the reverse direction.
            Files.delete(pack.resolve("tiles/r00-d0/stars.csv"));
            IllegalStateException missing = assertThrows(
                    IllegalStateException.class,
                    () -> StarIdentityStudyMain.packMagnitudes(pack));
            assertTrue(missing.getMessage().contains("missing"),
                    "a missing declared tile is refused: "
                            + missing.getMessage());
        } finally {
            deleteTree(pack);
        }

        // A manifest that is not the bright-sky pack: identity is
        // checked before any tile is trusted (format version and
        // tiling scheme are enforced by the production PackManifest
        // contract this verification parses through).
        Path foreignPack = tempPack(real, "tiles/r00-d0/stars.csv");
        try {
            Path manifest = foreignPack.resolve("manifest.properties");
            Files.writeString(manifest, Files.readString(manifest)
                    .replace("pack.name=bright-sky",
                            "pack.name=somebody-else"));
            IllegalStateException foreign = assertThrows(
                    IllegalStateException.class,
                    () -> StarIdentityStudyMain.packMagnitudes(foreignPack));
            assertTrue(foreign.getMessage().contains("somebody-else"),
                    "a foreign pack is refused by name: "
                            + foreign.getMessage());
        } finally {
            deleteTree(foreignPack);
        }
    }

    @Test
    void duplicateUnmatchedNamesStayDistinguishableByHipKey() {
        // Codex review, PR #118 (P2): the source holds duplicate
        // proper names (two stars are both named Hunahpú); the honest
        // report must keep such rows distinct, so every unmatched
        // entry carries its Hipparcos key.
        String first = StarIdentityStudyMain.unmatchedLabel("55147",
                Map.of("name", "Hunahpú"));
        String second = StarIdentityStudyMain.unmatchedLabel("55174",
                Map.of("name", "Hunahpú"));
        assertNotEquals(first, second,
                "duplicate names must stay distinguishable");
        assertEquals("Hunahpú (HIP 55147)", first);
        assertEquals("Hunahpú (HIP 55174)", second);
        assertEquals("β Ori (HIP 24436)", StarIdentityStudyMain
                        .unmatchedLabel("24436", Map.of("bayer", "β", "c", "Ori")),
                "designation-only entries carry the key too");
        assertEquals("HIP 12345", StarIdentityStudyMain
                        .unmatchedLabel("12345", Map.of()),
                "an entry with nothing else is its key alone");
    }

    /**
     * A temporary bright-sky pack holding only the given tiles, with
     * the real manifest filtered to declare exactly those star tiles
     * (all non-tile keys kept, so the production manifest contract
     * still parses).
     */
    private static Path tempPack(Path real, String... tiles)
            throws IOException {
        Path pack = Files.createTempDirectory("bright-sky-check");
        java.util.Set<String> keep = java.util.Set.of(tiles);
        String manifest = Files.readString(
                real.resolve("manifest.properties")).lines()
                .filter(line -> !line.startsWith("checksum.tiles/")
                        || keep.contains(line.substring("checksum.".length(),
                                line.indexOf('='))))
                .collect(Collectors.joining("\n", "", "\n"));
        Files.writeString(pack.resolve("manifest.properties"), manifest);
        for (String tile : tiles) {
            Path target = pack.resolve(tile);
            Files.createDirectories(target.getParent());
            Files.copy(real.resolve(tile), target);
        }
        return pack;
    }

    private static void deleteTree(Path root) throws IOException {
        try (var walk = Files.walk(root)) {
            for (Path path : walk.sorted(
                    java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
