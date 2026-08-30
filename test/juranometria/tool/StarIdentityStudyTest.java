package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarIdentityStudyTest {

    @Test
    void aTamperedBrightSkyTileIsRejectedBeforeComponentSelection()
            throws Exception {
        // P1 (Codex review, PR #118): the magnitudes that pick a
        // multi-component system's winning component come from the
        // bright-sky tiles, which are pack output, not pinned raw
        // input. A tampered magnitude silently re-attaches an identity
        // to the wrong TYC while every locked count still passes - so
        // the join must refuse a tile that fails the pack manifest's
        // checksum, and a tile the manifest does not know at all.
        Path pack = Files.createTempDirectory("bright-sky-tampered");
        try {
            Path real = Path.of("src/resources/catalog/bright-sky");
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    Files.isDirectory(real),
                    "bright-sky pack sources not present in this checkout");
            Files.copy(real.resolve("manifest.properties"),
                    pack.resolve("manifest.properties"));
            Path tile = Files.createDirectories(
                    pack.resolve("tiles/r00-d0")).resolve("stars.csv");
            String tampered = Files.readString(
                    real.resolve("tiles/r00-d0/stars.csv"))
                    + "TYC 9999-9999-9,0.0,0.0,0.01\n";
            Files.writeString(tile, tampered);
            IllegalStateException failure = assertThrows(
                    IllegalStateException.class,
                    () -> StarIdentityStudyMain.packMagnitudes(pack));
            assertTrue(failure.getMessage().contains("checksum"),
                    "the diagnostic names the check: " + failure.getMessage());
            assertTrue(failure.getMessage().contains("r00-d0"),
                    "the diagnostic names the tile: " + failure.getMessage());

            Path unknown = Files.createDirectories(
                    pack.resolve("tiles/r99-d9")).resolve("stars.csv");
            Files.copy(real.resolve("tiles/r00-d0/stars.csv"), unknown);
            Files.copy(real.resolve("tiles/r00-d0/stars.csv"), tile,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            IllegalStateException stray = assertThrows(
                    IllegalStateException.class,
                    () -> StarIdentityStudyMain.packMagnitudes(pack));
            assertTrue(stray.getMessage().contains("no checksum"),
                    "an unmanifested tile is refused: " + stray.getMessage());
        } finally {
            try (var walk = Files.walk(pack)) {
                for (Path path : walk.sorted(
                        java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

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
}
