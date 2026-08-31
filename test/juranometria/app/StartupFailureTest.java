package juranometria.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The launch failure surface of the 1.0 audit (issue #145). The
 * message is asserted rather than the dialog: showing it needs a
 * screen, but what it says is the part a reader depends on.
 */
class StartupFailureTest {

    @Test
    void aDamagedDownloadIsExplainedInTheLoadersOwnWordsWithItsRemedy() {
        // The observed failure shape: the catalogue's verification
        // throws inside a static initializer, so the exception that
        // reaches the launch handler carries no message of its own
        // and the useful sentence is one cause down.
        Throwable observed = new ExceptionInInitializerError(
                new IllegalStateException(
                        "star tile r10-d1/stars.csv does not match its"
                                + " manifest checksum"));

        String message = StartupFailure.message(observed);

        assertTrue(message.contains("r10-d1/stars.csv"),
                "the reader is told which file failed: " + message);
        assertTrue(message.contains("manifest checksum"),
                "in the loader's own words: " + message);
        assertTrue(message.contains("SHA-256"),
                "with the remedy that actually fixes it: " + message);
        assertTrue(message.contains("Download the release again"),
                "stated as an instruction, not a diagnosis: " + message);
        assertFalse(message.contains("ExceptionInInitializerError"),
                "and never as a class name: " + message);
    }

    @Test
    void aFailureWithNothingToSayStillNamesItself() {
        String message = StartupFailure.message(new NullPointerException());

        assertTrue(message.contains("NullPointerException"),
                "a causeless, messageless failure still identifies its"
                        + " kind rather than showing a blank line: "
                        + message);
        assertTrue(message.contains("Download the release again"),
                "and still carries the remedy");
    }
}
