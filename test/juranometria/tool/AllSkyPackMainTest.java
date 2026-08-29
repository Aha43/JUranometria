package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllSkyPackMainTest {

    @Test
    void refusesToCleanADirectoryItDoesNotOwn() throws Exception {
        // Codex review, PR #45 follow-up: a mistyped output argument must
        // never delete arbitrary files.
        Path stranger = Files.createTempDirectory("juranometria-not-a-pack");
        try {
            Files.writeString(stranger.resolve("precious.txt"), "not catalogue data");
            IllegalStateException refusal = assertThrows(IllegalStateException.class,
                    () -> AllSkyPackMain.ensureSafeToClean(stranger));
            assertTrue(refusal.getMessage().contains("refusing to clean"));
            assertTrue(Files.exists(stranger.resolve("precious.txt")),
                    "nothing may be deleted on refusal");
        } finally {
            Files.deleteIfExists(stranger.resolve("precious.txt"));
            Files.deleteIfExists(stranger);
        }
    }

    @Test
    void missingEmptyAndOwnedDirectoriesAreSafeToClean() throws Exception {
        assertDoesNotThrow(() -> AllSkyPackMain.ensureSafeToClean(
                Path.of("build/does-not-exist-anywhere")));

        Path empty = Files.createTempDirectory("juranometria-empty");
        try {
            assertDoesNotThrow(() -> AllSkyPackMain.ensureSafeToClean(empty));
        } finally {
            Files.deleteIfExists(empty);
        }

        Path owned = Files.createTempDirectory("juranometria-owned");
        try {
            Files.writeString(owned.resolve("manifest.properties"),
                    "format.version=1\npack.name=bright-sky\n");
            assertDoesNotThrow(() -> AllSkyPackMain.ensureSafeToClean(owned));
        } finally {
            Files.deleteIfExists(owned.resolve("manifest.properties"));
            Files.deleteIfExists(owned);
        }
    }
}
