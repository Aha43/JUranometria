package juranometria.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The launch failure surface of the 1.0 audit (issue #145). Two
 * things are proved here: that the remedy offered fits the failure
 * that actually happened (audit review, P1 - re-downloading cannot
 * repair a preferences store or an application defect), and that a
 * real process really does exit rather than lingering with no
 * window, which is the defect the surface exists to end.
 */
class StartupFailureTest {

    @Test
    void aDamagedDownloadIsExplainedInTheLoadersOwnWordsWithItsRemedy() {
        // The observed failure shape: the catalogue's verification
        // throws inside a static initializer, so the exception that
        // reaches the launch handler carries no message of its own
        // and the useful sentence is one cause down.
        IllegalStateException verification = new IllegalStateException(
                "star tile r10-d1/stars.csv does not match its"
                        + " manifest checksum");
        verification.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("juranometria.catalog.TiledCatalogue",
                        "load", "TiledCatalogue.java", 120)});
        Throwable observed = new ExceptionInInitializerError(verification);

        assertEquals(StartupFailure.Kind.BUNDLED_DATA,
                StartupFailure.classify(observed));
        String message = StartupFailure.message(observed);

        assertTrue(message.contains("r10-d1/stars.csv"),
                "the reader is told which file failed: " + message);
        assertTrue(message.contains("SHA-256"),
                "with the remedy that actually fixes it: " + message);
        assertTrue(message.contains("Download the release again"),
                "stated as an instruction, not a diagnosis: " + message);
        assertFalse(message.contains("ExceptionInInitializerError"),
                "and never as a class name: " + message);
    }

    @Test
    void anUnreadableSettingsStoreIsNeverBlamedOnTheDownload()
            throws Exception {
        // The real shape: the JDK's preferences store throws from its
        // own frames. Re-downloading the atlas would not touch it.
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        node.put("chart.deepSkyObjects", "false");
        ChartOptionsStore store = ChartOptionsStore.forNode(node);
        node.removeNode();
        Throwable thrown = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, store::load);

        assertEquals(StartupFailure.Kind.SETTINGS,
                StartupFailure.classify(thrown),
                "a preferences failure is recognised by where it came"
                        + " from, since its type is shared");
        String message = StartupFailure.message(thrown);

        assertFalse(message.contains("Download the release again"),
                "re-downloading cannot repair a settings store: "
                        + message);
        assertFalse(message.contains("SHA-256"));
        assertTrue(message.contains("saved settings"),
                "the reader is told what the store is: " + message);
        assertTrue(message.contains("defaults"),
                "and that removing it costs only the defaults: "
                        + message);
        assertTrue(message.contains("com.apple.java.util.prefs.plist")
                        && message.contains(".java/.userPrefs")
                        && message.contains("JavaSoft"),
                "with the location on each supported platform: "
                        + message);
    }

    @Test
    void aBackingStoreFailureIsASettingsFailureWhereverItComesFrom() {
        assertEquals(StartupFailure.Kind.SETTINGS,
                StartupFailure.classify(new RuntimeException(
                        new BackingStoreException("prefs unavailable"))),
                "the JDK's own preferences exception is decisive even"
                        + " when it arrives wrapped");
    }

    @Test
    void anUnrecognisedFailureAdmitsItRatherThanGuessingARemedy() {
        // An application defect: nothing to do with the download or
        // the settings, and the message must not pretend otherwise.
        String message = StartupFailure.message(
                new NullPointerException(
                        "Cannot invoke \"String.length()\" because"
                                + " \"title\" is null"));

        assertEquals(StartupFailure.Kind.UNRECOGNISED,
                StartupFailure.classify(new NullPointerException()));
        assertFalse(message.contains("Download the release again"),
                "no invented remedy: " + message);
        assertFalse(message.contains("saved settings"));
        assertTrue(message.contains("not a failure JUranometria"
                        + " recognises"),
                "it says so plainly: " + message);
        assertTrue(message.contains("more likely a defect"),
                "and points at the likelier culprit: " + message);
        assertTrue(message.contains(AppInfo.REPO_URL + "/issues"),
                "with somewhere to report it: " + message);
        assertTrue(message.contains("\"title\" is null"),
                "keeping the detail that makes a report useful");
    }

    @Test
    void aFailureWithNothingToSayStillNamesItself() {
        String message = StartupFailure.message(new NullPointerException());

        assertTrue(message.contains("NullPointerException"),
                "a causeless, messageless failure still identifies its"
                        + " kind rather than showing a blank line: "
                        + message);
    }

    @Test
    void aRealLaunchWithADamagedTileExitsNonZeroInsteadOfLingering()
            throws Exception {
        // The defect this whole surface exists to end, proved on a
        // real process: before the fix, this JVM stayed alive with no
        // window. The corrupted tile goes FIRST on the class path, so
        // the application loads it instead of the good one and
        // nothing in the repository is touched.
        Path original = Path.of("build/classes/resources/catalog/bright-sky"
                + "/tiles/r10-d1/stars.csv");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(original),
                "needs the compiled resources");
        Path shadow = Files.createTempDirectory("juranometria-damaged");
        Path damaged = shadow.resolve("resources/catalog/bright-sky"
                + "/tiles/r10-d1/stars.csv");
        Files.createDirectories(damaged.getParent());
        Files.writeString(damaged, Files.readString(original)
                + "this row is not a star\n");

        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java")
                        .toString(),
                "-Djava.awt.headless=true",
                "-cp", shadow + java.io.File.pathSeparator
                        + System.getProperty("java.class.path"),
                "juranometria.app.JUranometriaMain")
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        boolean finished = process.waitFor(120, java.util.concurrent.TimeUnit
                .SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }

        assertTrue(finished,
                "the process must end itself, never linger invisibly");
        assertEquals(1, process.exitValue(),
                "and end non-zero: " + output);
        assertTrue(output.contains("JUranometria could not start."),
                "having said so: " + output);
        assertTrue(output.contains("manifest checksum"),
                "naming the verification that failed: " + output);
        assertTrue(output.contains("Download the release again"),
                "with the remedy that fits a damaged download: "
                        + output);
        cleanUp(shadow);
    }

    @Test
    void aRealLaunchThatFailsForAnUnknownReasonOffersNoFalseRemedy()
            throws Exception {
        // Headless is not a damaged download: the same real process,
        // failing a different way, must not be told to re-download.
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java")
                        .toString(),
                "-Djava.awt.headless=true",
                "-cp", System.getProperty("java.class.path"),
                "juranometria.app.JUranometriaMain")
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS),
                "the process must end itself");

        assertEquals(1, process.exitValue(), output);
        assertTrue(output.contains("HeadlessException"),
                "the real failure is reported: " + output);
        assertFalse(output.contains("Download the release again"),
                "and no remedy is invented for it: " + output);
    }

    private static void cleanUp(Path directory) throws IOException {
        try (var tree = Files.walk(directory)) {
            for (Path path : tree.sorted(java.util.Comparator.reverseOrder())
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void theRemediesStayDistinct() {
        // Guarding the guard: three kinds, three genuinely different
        // instructions, so a future edit cannot quietly collapse them.
        List<String> remedies = List.of(
                StartupFailure.message(bundledDataFailure()),
                StartupFailure.message(settingsFailure()),
                StartupFailure.message(new IllegalArgumentException("odd")));
        assertEquals(3, remedies.stream().distinct().count(),
                "each kind of failure says something different");
    }

    private static Throwable bundledDataFailure() {
        IllegalStateException failure = new IllegalStateException("bad tile");
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("juranometria.geo.ConstellationGeography",
                        "load", "ConstellationGeography.java", 40)});
        return failure;
    }

    private static Throwable settingsFailure() {
        IllegalStateException failure =
                new IllegalStateException("Node has been removed.");
        failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("java.util.prefs.AbstractPreferences",
                        "get", "AbstractPreferences.java", 252)});
        return failure;
    }
}
