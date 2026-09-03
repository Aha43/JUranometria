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
        // The mechanism, proven on a fixture that does the wrong
        // thing on purpose: sets a look and feel and walks away.
        TestEvidenceScan.File broken = TestEvidenceScan.classify(
                "Fixture.java",
                // The needles are split in this source, so the gate
                // test does not scan as its own fixture.
                "class Fixture { void t() throws Exception {"
                        + " javax.swing.UIManager.setLook" + "AndFeel("
                        + "new javax.swing.plaf.metal.MetalLookAndFeel());"
                        + " } }");
        assertEquals("UNPROTECTED", broken.stateClass(),
                "a theme set and never restored is the leak the guard"
                        + " names");

        // And the corpus, pinned exactly: the three flagged files
        // are read and owned in the decision document, and a fourth
        // would be a new leak nobody has read.
        List<TestEvidenceScan.File> files =
                TestEvidenceScan.scan(Path.of("test"));
        assertEquals(3, count(files, "UNPROTECTED"),
                "the flagged set is the decision document's three -"
                        + " one real leak, one crash-path gap, one"
                        + " fixture by design - and must shrink under"
                        + " #224, never grow: "
                        + files.stream().filter(f -> f.stateClass()
                                .equals("UNPROTECTED"))
                                .map(TestEvidenceScan.File::path)
                                .toList());
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
    void theEvidenceClassesAreDisjointAndCompletelyNamed()
            throws IOException {
        // Guard G5's premise: the report classifies every study
        // artifact into exactly one of the three classes, and the
        // photographs are the named set - a new session-dependent
        // artifact must arrive with a decision, not a habit.
        String report = Files.readString(REPORT);
        assertTrue(report.contains("deterministic reports")
                        && report.contains(
                                "platform-rendered inspection")
                        && report.contains(
                                "session-dependent photographs"),
                "three classes, each named");
        for (String photograph : List.of("dialog-real.png",
                "dialog-real-enlarged.png", "dialog-real-dark.png")) {
            assertTrue(report.contains(photograph),
                    "the photographs are enumerated by name: "
                            + photograph);
        }
        assertTrue(!report.contains(
                        "docs/studies/place-and-time/measurements.md"
                                + " | photograph"),
                "and no report is filed as a photograph");
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
