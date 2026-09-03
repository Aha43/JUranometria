package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the maintenance gate is a gate (Sprint 26, issue #241).
 *
 * <p>Three duties. The report and the decision document must agree
 * with the scanner and with each other, so neither can drift from
 * the evidence. Production must be untouched, because a measurement
 * gate that changed the page would be a change wearing a gate's
 * name. And every guard the decision proposes must already catch
 * the failure it names - proven here against deliberately broken
 * fixtures - with the corpus's remaining debt pinned at its exact
 * size, so it can shrink under its named owner but never grow
 * unnoticed.
 */
class TestEvidenceGateTest {

    private static final Path REPORT =
            Path.of("docs/studies/test-evidence/measurements.md");
    private static final Path DECISION =
            Path.of("docs/decisions/test-evidence.md");

    // ---- the gate changes nothing -----------------------------------

    @Test
    void theGateChangesNothingTheChartDraws() {
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        var scene = Atlas.assembler()
                .assemble(ChartViewState.DEFAULT, 900, 700);
        BufferedImage before = renderer.renderToImage(scene,
                ChartOptions.DEFAULTS);
        // Everything the gate can do, done: the whole tree scanned
        // and classified.
        try {
            TestEvidenceScan.scan(Path.of("test"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        BufferedImage after = renderer.renderToImage(scene,
                ChartOptions.DEFAULTS);
        assertTrue(identical(before, after),
                "a measurement gate measures, and changes nothing");
    }

    // ---- the report cannot drift ------------------------------------

    @Test
    void theReportAndTheDecisionAgreeWithTheScanner()
            throws IOException {
        List<TestEvidenceScan.File> files =
                TestEvidenceScan.scan(Path.of("test"));
        String report = Files.readString(REPORT);
        String decision = Files.readString(DECISION);

        long touching = files.stream()
                .filter(f -> !f.globalState().isEmpty()).count();
        long shared = count(files, "protected-shared");
        long local = count(files, "protected-locally");
        long flagged = count(files, "UNPROTECTED");
        long display = files.stream()
                .filter(TestEvidenceScan.File::displayDependent).count();

        for (String claim : List.of(
                "**" + touching + " files** touch process-wide state",
                shared + " protected by the shared SwingSession",
                local + " restoring locally",
                flagged + " unprotected",
                "**" + display + " display-dependent files.**")) {
            assertTrue(report.contains(claim),
                    "the report states what the scanner found: "
                            + claim);
        }
        for (String claim : List.of(
                "**" + touching + " files** touch process-wide state",
                "**" + shared + "** use the shared",
                "**" + local + "** restore locally",
                "**" + flagged + " flagged unprotected**",
                "**" + display + " files** depend on a display")) {
            assertTrue(decision.contains(claim),
                    "and the decision quotes it rather than a memory"
                            + " of it: " + claim);
        }
        int reads = files.stream()
                .mapToInt(TestEvidenceScan.File::liveReads).sum();
        int handOffs = files.stream()
                .mapToInt(TestEvidenceScan.File::edtHandOffs).sum();
        assertTrue(report.contains("**" + reads + "**")
                        && report.contains("**" + handOffs + "**"),
                "the traffic counts are the scanner's");
        assertTrue(decision.contains("**" + reads + "\nreads")
                        || decision.contains("**" + reads + " reads")
                        || decision.contains(reads + "\nreads of live"),
                "and the decision carries the same read count: "
                        + reads);
    }

    // ---- guard G1: global state is protected ------------------------

    @Test
    void aTouchWithNoRestoreIsCaughtAndTheDebtIsPinned()
            throws IOException {
        // The mechanism, proven on fixtures that do the wrong
        // thing on purpose. The needles are split in this source,
        // so the gate test does not scan as its own fixture.
        TestEvidenceScan.File bare = TestEvidenceScan.classify(
                "Fixture.java",
                "class Fixture { void t() throws Exception {"
                        + " javax.swing.UIManager.setLook" + "AndFeel("
                        + "new javax.swing.plaf.metal.MetalLookAndFeel());"
                        + " } }");
        assertEquals("UNPROTECTED", bare.stateClass(),
                "a theme set and never restored is the leak the guard"
                        + " names");

        // The review's exact case: an unrelated finally in the same
        // file must not vouch for a state nobody captured.
        TestEvidenceScan.File unrelatedFinally =
                TestEvidenceScan.classify("Fixture.java",
                        "class Fixture { void t() throws Exception {"
                                + " javax.swing.UIManager.setLook"
                                + "AndFeel(new javax.swing.plaf.metal"
                                + ".MetalLookAndFeel());"
                                + " java.io.InputStream in = open();"
                                + " try { in.read(); } finally {"
                                + " in.close(); } } }");
        assertEquals(List.of("look-and-feel"),
                unrelatedFinally.unprotectedState(),
                "closing a stream is not restoring a theme");

        // And a theme set through a door with no setter in sight -
        // the AppSmokeTest shape, which the first rule missed.
        TestEvidenceScan.File throughTheDoor =
                TestEvidenceScan.classify("Fixture.java",
                        "class Fixture { void t() { UiTheme.app"
                                + "ly(); } }");
        assertEquals("UNPROTECTED", throughTheDoor.stateClass(),
                "UiTheme.app" + "ly installs a look and feel just as"
                        + " surely as the setter does");

        // The corpus, pinned exactly: the five flagged files are
        // read and owned in the decision document - two real leaks,
        // two crash-path gaps, one fixture by design - and a sixth
        // would be a new leak nobody has read.
        List<TestEvidenceScan.File> files =
                TestEvidenceScan.scan(Path.of("test"));
        assertEquals(List.of(
                        "juranometria/app/AppSmokeTest.java",
                        "juranometria/app/ExitProbeMain.java",
                        "juranometria/app/PackagedAcceptanceRestoresTest.java",
                        "juranometria/app/StartupFailureTest.java",
                        "juranometria/app/ToolbarVersionAndExitTest.java"),
                files.stream().filter(f -> f.stateClass()
                                .equals("UNPROTECTED"))
                        .map(TestEvidenceScan.File::path).toList(),
                "the flagged set must shrink under #224, never grow");
    }

    @Test
    void theEvidenceExecutablesAreScannedAndTheirDebtIsPinned()
            throws IOException {
        // The review's first finding: the study mains and the
        // packaged acceptance modify preferences, fonts and themes
        // and the first scan never looked at them.
        List<TestEvidenceScan.File> executables =
                TestEvidenceScan.scanEvidenceExecutables();
        assertTrue(executables.stream().anyMatch(f -> f.path()
                        .endsWith("PackagedAcceptanceMain.java")),
                "the packaged acceptance is in the inventory");
        assertTrue(executables.stream()
                        .filter(f -> f.path()
                                .endsWith("PackagedAcceptanceMain.java"))
                        .allMatch(f -> f.unprotectedState().isEmpty()),
                "and its preference use is paired: restored under"
                        + " guard, restart node removed");
        List<String> unpaired = executables.stream()
                .filter(f -> !f.unprotectedState().isEmpty())
                .map(TestEvidenceScan.File::path).sorted().toList();
        assertEquals(List.of(
                        "src/juranometria/tool/DeepSkyVocabularyMockupMain.java",
                        "src/juranometria/tool/OnThisPageMockupMain.java",
                        "src/juranometria/tool/PlaceAndTimeControlsMockupMain.java",
                        "src/juranometria/tool/PlaceAndTimeDialogStudyMain.java"),
                unpaired,
                "the four widget photographers, whose theme dies with"
                        + " the JVM - benign by construction, and"
                        + " pinned so a fifth arrives by decision");
    }

    // ---- guard G2: nobody opens the reader's real store -------------

    @Test
    void aTestOpeningTheRealPreferenceNodeIsCaughtAndThereAreNone()
            throws IOException {
        assertTrue(TestEvidenceScan.opensRealPreferences(
                        "class Fixture { void t() {"
                                + " java.util.prefs.Preferences.user"
                                + "Root().node(\"juranometr"
                                + "ia\").put(\"k\", \"v\"); } }"),
                "the bare node is the reader's own settings, and the"
                        + " scanner sees it");
        int offenders = 0;
        try (var tree = Files.walk(Path.of("test"))) {
            for (Path source : tree
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList()) {
                if (TestEvidenceScan.opensRealPreferences(
                        Files.readString(source))) {
                    offenders++;
                }
            }
        }
        assertEquals(0, offenders,
                "no test edits the reader's settings - the standing"
                        + " state, held from this gate on");
    }

    // ---- guard G3: display premises are stated ----------------------

    @Test
    void pointerEventsWithoutAPremiseAreCaughtAndAdoptionIsPinned()
            throws IOException {
        TestEvidenceScan.File broken = TestEvidenceScan.classify(
                "Fixture.java",
                "class Fixture { void t() {"
                        + " org.junit.jupiter.api.Assumptions"
                        + ".assumeFalse(GraphicsEnvironment.isHead"
                        + "less());"
                        + " c.dispatchEvent(new java.awt.event.MouseEvent("
                        + "c, java.awt.event.MouseEvent.MOUSE_"
                        + "PRESSED, 0, 0, 5, 5, 1, false)); } }");
        assertTrue(broken.displayDependent()
                        && broken.routes().contains("pointer-events")
                        && broken.premises().isEmpty(),
                "a display test clicking with no stated premise is"
                        + " exactly what the guard names");

        List<TestEvidenceScan.File> display = TestEvidenceScan
                .scan(Path.of("test")).stream()
                .filter(TestEvidenceScan.File::displayDependent)
                .toList();
        long focusPremise = display.stream().filter(f ->
                f.premises().contains("focused-window")
                        || f.premises().contains("focus-owner")).count();
        long reachPremise = display.stream().filter(f ->
                f.premises().contains("point-reachable")).count();
        assertEquals(20, display.size(),
                "the display corpus is the twenty the decision names");
        assertTrue(focusPremise >= 7,
                "focus premises may only spread under #243: "
                        + focusPremise + " of " + display.size());
        assertTrue(reachPremise >= 2,
                "and so may reachability premises: " + reachPremise);
    }

    // ---- guard G4/G5 ratchets ---------------------------------------

    @Test
    void theBackDoorCountsMayShrinkButNeverGrow() throws IOException {
        List<TestEvidenceScan.File> files =
                TestEvidenceScan.scan(Path.of("test"));
        long doClick = files.stream().filter(f ->
                f.routes().contains("back-door-click")).count();
        long postAction = files.stream().filter(f ->
                f.routes().contains("back-door-commit")).count();
        assertTrue(doClick <= 25,
                "doClick files are the measured twenty-five, shrinking"
                        + " under #243: " + doClick);
        assertTrue(postAction <= 12,
                "postActionEvent files are the measured twelve,"
                        + " shrinking under #243: " + postAction);
    }

    @Test
    void theFiveEvidenceClassesEachClaimTheRightArtifacts()
            throws IOException {
        // Guard G5, proven on one name from each class. The first
        // draft filed every PNG as inspection imagery - weakening
        // the renderer studies' byte contract - and the SOFA oracle
        // as artwork (review).
        assertEquals("deterministic-report",
                TestEvidenceScan.artifactClass("measurements.md"));
        assertEquals("byte-exact-fixture",
                TestEvidenceScan.artifactClass("reference-vectors.txt"),
                "the SOFA oracle is committed data with provenance,"
                        + " not artwork");
        assertEquals("byte-exact-fixture",
                TestEvidenceScan.artifactClass("reference-vectors.c"));
        assertEquals("renderer-drawn",
                TestEvidenceScan.artifactClass("m31-08.png"),
                "a chart study keeps its byte-reproducibility"
                        + " contract");
        assertEquals("widget-rendered-inspection",
                TestEvidenceScan.artifactClass("controls-dialog.png"));
        assertEquals("widget-rendered-inspection",
                TestEvidenceScan.artifactClass("sidebar-dense.png"));
        assertEquals("session-photograph",
                TestEvidenceScan.artifactClass("dialog-real-dark.png"));

        String report = Files.readString(REPORT);
        for (String named : List.of("deterministic-report",
                "byte-exact-fixture", "renderer-drawn",
                "widget-rendered-inspection", "session-photograph",
                "reference-vectors.txt", "dialog-real.png")) {
            assertTrue(report.contains(named),
                    "the report names it: " + named);
        }
    }

    // ----------------------------------------------------------------

    private static long count(List<TestEvidenceScan.File> files,
                              String stateClass) {
        return files.stream()
                .filter(f -> f.stateClass().equals(stateClass)).count();
    }

    private static boolean identical(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth()
                || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
