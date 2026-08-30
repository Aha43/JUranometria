package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
}
