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
        Throwable observed = new ExceptionInInitializerError(
                new juranometria.catalog.PackIntegrityException(
                        "star tile r10-d1/stars.csv does not match its"
                                + " manifest checksum"));

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
    void aDefectInsideTheLoadersIsNotCalledADamagedDownload() {
        // The signal must be closed (audit review, P1). A programming
        // defect thrown from the very packages that verify the data
        // used to be reported as damaged data, sending the reader to
        // re-download a file that was never at fault.
        NullPointerException defect = new NullPointerException(
                "Cannot read field \"tiles\" because \"pack\" is null");
        defect.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("juranometria.catalog.TiledCatalogue",
                        "starsIn", "TiledCatalogue.java", 140),
                new StackTraceElement("juranometria.geo.ConstellationGeography",
                        "load", "ConstellationGeography.java", 40)});

        assertEquals(StartupFailure.Kind.UNRECOGNISED,
                StartupFailure.classify(defect),
                "frames inside the catalogue packages prove nothing;"
                        + " only the verification type does");
        String message = StartupFailure.message(defect);
        assertFalse(message.contains("Download the release again"),
                "a defect of ours must never be blamed on the"
                        + " reader's download: " + message);
        assertTrue(message.contains("more likely a defect"));
    }

    @Test
    void aJavaRuntimeWithoutSha256IsNotADamagedDownloadEither() {
        // The digest helpers' policy, seen from the reader's end
        // (audit review, P2): one pack's copy used to report an
        // absent SHA-256 as a pack integrity failure. This is the
        // exact exception the single shared helper now throws.
        Throwable brokenRuntime = new IllegalStateException(
                "SHA-256 is unavailable in this Java runtime",
                new java.security.NoSuchAlgorithmException("SHA-256"));

        assertEquals(StartupFailure.Kind.UNRECOGNISED,
                StartupFailure.classify(brokenRuntime),
                "a runtime missing a guaranteed algorithm says nothing"
                        + " about the data");
        assertFalse(StartupFailure.message(brokenRuntime)
                        .contains("Download the release again"),
                "so the reader is not sent to re-download files that"
                        + " are perfectly good");
    }

    @Test
    void aWrappedIntegrityFailureKeepsTheDetailThatIdentifiesTheFile() {
        // describe() used to stop at the first wrapper carrying a
        // message, which hid the filename and checksum underneath it
        // (audit review, P1).
        Throwable wrapped = new juranometria.catalog.PackIntegrityException(
                "failed to load the bright-sky pack",
                new juranometria.catalog.PackIntegrityException(
                        "catalogue tile tiles/r10-d1/stars.csv does not"
                                + " match its manifest checksum\n"
                                + "  expected 9e503ae2\n"
                                + "  actual   a22c6ed8"));

        String message = StartupFailure.message(wrapped);

        assertTrue(message.contains("failed to load the bright-sky pack"),
                "the outer sentence is kept: " + message);
        assertTrue(message.contains("tiles/r10-d1/stars.csv")
                        && message.contains("9e503ae2")
                        && message.contains("a22c6ed8"),
                "and never at the cost of the detail that identifies"
                        + " the file: " + message);
        assertEquals(StartupFailure.Kind.BUNDLED_DATA,
                StartupFailure.classify(wrapped));
    }

    @Test
    void theSettingsRemedyTouchesOnlyThisApplicationsOwnSettings() {
        // On macOS every Java application shares ONE preferences
        // file; the first version of this message told the reader to
        // delete it, which would have destroyed other applications'
        // settings while promising nothing else was lost (audit
        // review, P1).
        String message = StartupFailure.message(settingsFailure());

        assertTrue(message.contains("do not delete it"),
                "the shared file is protected explicitly: " + message);
        assertTrue(message.contains("PlistBuddy")
                        && message.contains("Delete \":/:juranometria/\""),
                "with a command that removes only this application's"
                        + " own entry: " + message);
        assertTrue(message.contains(".java/.userPrefs/juranometria")
                        && message.contains("Prefs\\juranometria"),
                "and application-specific paths elsewhere: " + message);
        assertFalse(message.matches("(?s).*delete[^\\n]*"
                        + "com\\.apple\\.java\\.util\\.prefs\\.plist.*"),
                "never an instruction to delete the shared file: "
                        + message);
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
        return new juranometria.catalog.PackIntegrityException("bad tile");
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
