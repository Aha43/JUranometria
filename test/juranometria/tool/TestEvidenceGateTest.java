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

        // The corpus, settled by #224 and pinned there: nothing is
        // unprotected, and every JVM-global state flows through the
        // shared guard - the only file restoring one locally is the
        // guard itself, whose body IS the restore. The preference
        // locals are the JUnit-fixture removal shape, kept on
        // purpose because a node's life spans BeforeEach to
        // AfterEach and a body-wrapper cannot hold it.
        List<TestEvidenceScan.File> files =
                TestEvidenceScan.scan(Path.of("test"));
        assertEquals(List.of(),
                files.stream().filter(f -> f.stateClass()
                                .equals("UNPROTECTED"))
                        .map(TestEvidenceScan.File::path).toList(),
                "the gate found five and #224 settled them; the sixth"
                        + " is caught here, at the source, before a"
                        + " stranger's flaky run finds it");
        assertEquals(List.of(),
                files.stream()
                        .filter(f -> f.stateClass()
                                .equals("protected-locally"))
                        .filter(f -> !f.globalState().equals(
                                List.of("preferences")))
                        .map(TestEvidenceScan.File::path).toList(),
                "every non-preference global flows through the shared"
                        + " guard");
        assertEquals(List.of("juranometria/app/SwingSession.java"),
                files.stream()
                        .filter(f -> f.stateClass()
                                .equals("the-shared-guard"))
                        .map(TestEvidenceScan.File::path).toList(),
                "and exactly one file is the guard itself - a second"
                        + " claimant would be a copy wearing its name");
    }

    @Test
    void aDirectWriteToTheRootIsStillCaughtAndTheWitnessIsPinned() {
        // The review's case: exempting by no-.node( shape also
        // exempted a write straight to the root. Shape exemptions
        // are gone; the one read-only witness is exempt by exact
        // name, and a root write classifies as the touch it is.
        TestEvidenceScan.File rootWriter = TestEvidenceScan.classify(
                "Fixture.java",
                "class Fixture { void t() {"
                        + " java.util.prefs.Preferences.user"
                        + "Root().put(\"k\", \"v\"); } }");
        assertTrue(rootWriter.globalState().contains("preferences")
                        && rootWriter.stateClass().equals("UNPROTECTED"),
                "a write to the root itself is a preference touch"
                        + " with no cleanup");
        TestEvidenceScan.File readOnly = TestEvidenceScan.classify(
                "juranometria/app/PrefsExistsProbe.java", "test",
                "class PrefsExistsProbe { void t() throws Exception {"
                        + " java.util.prefs.Preferences.user"
                        + "Root().sync(); } }");
        assertTrue(!readOnly.globalState().contains("preferences"),
                "the pinned witness is exempt by its exact name");
        assertEquals(List.of("juranometria/app/PrefsExistsProbe.java"),
                TestEvidenceScan.READ_ONLY_WITNESSES,
                "and the exemption list is exactly that one name");
    }

    @Test
    void theInstrumentsAreExcludedByExactlyTheirTwoNames() {
        // The ruler is not a thing being measured: the scanner's
        // marker definitions are string literals that read exactly
        // like the behaviour they detect (review). Pinned, so the
        // exemption cannot quietly grow into a dump.
        assertEquals(List.of(
                        "src/juranometria/tool/TestEvidenceScan.java",
                        "src/juranometria/tool/TestEvidenceStudyMain.java"),
                TestEvidenceScan.INSTRUMENTS);
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
                        "src/juranometria/tool/EclipticCandidateStudyMain.java",
                        "src/juranometria/tool/EclipticControlStudyMain.java",
                        "src/juranometria/tool/OnThisPageMockupMain.java",
                        "src/juranometria/tool/PlaceAndTimeControlsMockupMain.java",
                        "src/juranometria/tool/PlaceAndTimeDialogStudyMain.java",
                        "src/juranometria/tool/WorkingSelectionMockupMain.java"),
                unpaired,
                "the seven widget photographers, whose font setting"
                        + " dies with the JVM - benign by"
                        + " construction, and pinned so the next one"
                        + " arrives by decision; the fifth arrived by"
                        + " exactly that route (#258's gate), and the"
                        + " sixth and seventh by #271's - the ecliptic"
                        + " control mock-up, and the candidate study,"
                        + " which sets the font only to show that"
                        + " enlarged text does not reach the chart");
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
        // The review's case: a production factory opens the same
        // store on the caller's behalf, and the literal-node needle
        // walked straight past it.
        assertTrue(TestEvidenceScan.opensRealPreferences(
                        "class Fixture { void t() {"
                                + " var store = ChartOptionsStore.us"
                                + "er(); store.load(); } }"),
                "a store factory is a door to the same settings");
        assertTrue(TestEvidenceScan.opensRealPreferences(
                        "class Fixture { void t() {"
                                + " AppShutdown.re" + "al().run();"
                                + " } }"),
                "and so is the shutdown route the remembered list"
                        + " missed - which is why the set is derived"
                        + " now");

        // The derivation itself, proven on a synthetic production
        // source: a public factory over the literal derives, its
        // private helper does not, and a public method reaching the
        // literal through that helper derives too.
        assertEquals(List.of("Fixture.begin(", "Fixture.open("),
                TestEvidenceScan.doorsIn("Fixture",
                        "class Fixture {"
                                + " public static Fixture open() {"
                                + " return of(Preferences.user"
                                + "Root().node(\"juranometr"
                                + "ia\")); }"
                                + " private static Preferences held() {"
                                + " return Preferences.user" + "Root()"
                                + ".node(\"juranometr" + "ia\"); }"
                                + " public static void begin() {"
                                + " held().put(\"k\", \"v\"); } }")
                        .stream().sorted().toList(),
                "doors are every non-private static route to the"
                        + " literal, direct or through a same-file"
                        + " helper");

        // And the derived set over the real production tree, pinned:
        // a new door arrives as a visible pin change, never a silent
        // gap in a remembered list (review).
        assertEquals(List.of("AppShutdown.re" + "al(",
                        "AppearanceStore.us" + "er(",
                        "ChartOptionsStore.us" + "er(",
                        "PackagedAcceptanceMain.ma" + "in(",
                        "PlaceStore.us" + "er("),
                TestEvidenceScan.realPreferenceDoors(),
                "the five production entry points to the reader's"
                        + " store");
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
        assertEquals(23, display.size(),
                "the display corpus is the twenty the decision names"
                        + " plus the black-sky journey (#246) and the"
                        + " #261 pair - the surfaces journey and the"
                        + " table gestures, whose presses go through"
                        + " Swing's own toolkit-asking UI - each with"
                        + " its premises stated");
        assertTrue(focusPremise >= 14,
                "focus premises spread under #243 and may not"
                        + " retreat: " + focusPremise + " of "
                        + display.size());
        assertTrue(reachPremise >= 13,
                "nor may reachability premises: " + reachPremise);
    }

    @Test
    void theOnlyRawKeyDispatcherIsTheSharedHelperItself()
            throws IOException {
        // The exact pin the review asked for, replacing a rule an
        // unrelated helper call in the same file could still mask:
        // one file in the whole test tree may build and dispatch a
        // raw KeyEvent, and it is the shared helper whose routes
        // carry the premises. A second dispatcher arrives by
        // decision, through this pin, or not at all.
        List<String> dispatchers = new java.util.ArrayList<>();
        try (var tree = Files.walk(Path.of("test"))) {
            for (Path source : tree
                    .filter(f -> f.toString().endsWith(".java"))
                    .sorted().toList()) {
                if (TestEvidenceScan.dispatchesRawKeys(
                        Files.readString(source))) {
                    dispatchers.add(source.toString());
                }
            }
        }
        assertEquals(List.of("test/juranometria/ui/ReaderInput.java"),
                dispatchers,
                "every journey presses through the shared routes -"
                        + " typeAndEnter, shortcut, or shortcutOn");
        // And the raw dispatcher is private (review): a public
        // premise-free press was a bypass wearing the helper's name.
        String helper = Files.readString(
                Path.of("test/juranometria/ui/ReaderInput.java"));
        assertTrue(helper.contains("private static void press("),
                "press is the helper's own business");
        assertTrue(!helper.contains("public static void press("),
                "and no route without a premise is public");
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
        assertTrue(doClick <= 26,
                "doClick files shrank under #243 to menu convention"
                        + " and mechanism tests, and may not grow"
                        + " beyond them - the black-sky journey"
                        + " (#246) added one file whose only doClick"
                        + " is the recorded View-menu convention, the"
                        + " reader surfaces (#261) added two"
                        + " control-mechanism files (the Accumulate"
                        + " toggle, the working-set rows) whose"
                        + " journeys drive the same controls with"
                        + " real pointer events, and the #262 closing"
                        + " journey reaches Settings and Chart"
                        + " Options through the same recorded"
                        + " menu-item convention: " + doClick);
        assertTrue(postAction <= 3,
                "postActionEvent survives only in the named mechanism"
                        + " tests: " + postAction);
    }

    @Test
    void theSixEvidenceClassesEachClaimTheRightArtifacts()
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
        assertEquals("captured-evidence",
                TestEvidenceScan.artifactClass(
                        "screenshot-dock-app.png"),
                "an operating-system screenshot is captured"
                        + " evidence, digest-pinned - never filed as"
                        + " artwork a command could regenerate");

        String report = Files.readString(REPORT);
        for (String named : List.of("deterministic-report",
                "byte-exact-fixture", "renderer-drawn",
                "widget-rendered-inspection", "session-photograph",
                "captured-evidence",
                "reference-vectors.txt",
                "scripts/reference-vectors.c",
                "dialog-real.png",
                "screenshot-dock-app.png")) {
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
