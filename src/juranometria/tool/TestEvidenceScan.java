package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * What the test suite touches, and how it protects it (Sprint 26,
 * issue #241).
 *
 * <p>A mechanical scan of the test sources: which files disturb
 * process-wide Swing state, which depend on a display, which
 * premises and routes they use, and which protections are present.
 * The rules are textual and deliberately simple - a marker is a
 * string the source contains - so the same rule applied twice gives
 * the same answer, and the gate can aim the classifier at a
 * deliberately broken fixture to prove a guard catches what it
 * names.
 *
 * <p>Protection is paired, not merely present. An earlier rule
 * counted any {@code finally} or {@code AfterEach} in the file as
 * protection, so an unrelated cleanup could vouch for a leak it
 * never touched (review). A state now counts as protected only when
 * the file carries the whole restoring shape for that same state:
 * the <em>capture</em> (a {@code getLookAndFeel}, a
 * {@code Locale.getDefault}), the <em>restoring write</em> (the
 * matching setter; for a preference node its removal or clearing),
 * and a place to run it ({@code finally} or {@code AfterEach}).
 * The capture matters because a correct restore often holds exactly
 * one setter - the put-back - with the disturbance arriving through
 * another door such as {@code UiTheme.apply} or a FlatLaf setup.
 *
 * <p>What a textual rule cannot decide is stated rather than
 * papered over: whether a particular read happens on the event
 * thread is a property of control flow, not of text, and the
 * decision document assigns that discipline to named helpers and to
 * the race tests that already hold it (#220), not to this scan.
 */
public final class TestEvidenceScan {

    private TestEvidenceScan() {
    }

    /** One scanned source, classified. */
    public record File(String path,
                       String kind,
                       List<String> globalState,
                       List<String> unprotectedState,
                       String stateClass,
                       boolean displayDependent,
                       List<String> premises,
                       List<String> routes,
                       List<String> platformAssumptions,
                       int edtHandOffs,
                       int liveReads) {
    }

    // ---- markers, named once ----------------------------------------

    private record Marker(String name, String... needles) {
        boolean in(String source) {
            for (String needle : needles) {
                if (source.contains(needle)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A process-wide state: the writes that touch it, the reads that
     * capture what was there, and the writes that put it back.
     * Look-and-feel and the default font may instead be covered by
     * the shared {@code SwingSession} guard, which captures and
     * restores both. A preference node has no capture - its
     * protection is removal or clearing of what the file created.
     */
    private record SharedState(String name, String[] touches,
                               String[] captures, String[] restores,
                               String[] sharedGuards) {
    }

    private static final List<SharedState> GLOBAL_STATE = List.of(
            new SharedState("look-and-feel",
                    new String[] {"UIManager.setLookAndFeel",
                            "FlatLightLaf.setup", "FlatDarkLaf.setup",
                            "UiTheme.apply"},
                    new String[] {"UIManager.getLookAndFeel"},
                    new String[] {"UIManager.setLookAndFeel"},
                    new String[] {"SwingSession.restoring(",
                            "SwingSession.capture()"}),
            new SharedState("default-font",
                    new String[] {"UIManager.put(\"defaultFont\""},
                    new String[] {"fontOverride()",
                            "get(\"defaultFont\")",
                            "containsKey(\"defaultFont\")"},
                    new String[] {"UIManager.put(\"defaultFont\""},
                    new String[] {"SwingSession.restoring(",
                            "SwingSession.capture()"}),
            new SharedState("locale",
                    new String[] {"Locale.setDefault"},
                    new String[] {"Locale.getDefault"},
                    new String[] {"Locale.setDefault"},
                    new String[] {"SwingSession.restoringLocale("}),
            new SharedState("time-zone",
                    new String[] {"TimeZone.setDefault"},
                    new String[] {"TimeZone.getDefault"},
                    new String[] {"TimeZone.setDefault"},
                    new String[] {"SwingSession.restoringTimeZone("}),
            new SharedState("repaint-manager",
                    new String[] {"RepaintManager.setCurrentManager"},
                    new String[] {"RepaintManager.currentManager"},
                    new String[] {"RepaintManager.setCurrentManager"},
                    new String[] {"SwingSession.restoringRepaintManager("}),
            new SharedState("preferences",
                    new String[] {"Preferences.userRoot"},
                    new String[] {},
                    new String[] {"removeNode()", ".clear()"},
                    new String[] {"SwingSession.scratchPreferences("}));

    private static final List<Marker> DISPLAY = List.of(
            new Marker("display",
                    "assumeFalse(GraphicsEnvironment.isHeadless",
                    "assumeFalse(java.awt.GraphicsEnvironment.isHeadless"));

    private static final List<Marker> PREMISES = List.of(
            new Marker("focused-window", "insistOnFocus"),
            new Marker("focus-owner", "isFocusOwner()",
                    "ReaderInput.typeAndEnter("),
            new Marker("point-reachable", "getVisibleRect().contains",
                    "ReaderInput.click("),
            new Marker("control-showing", "isShowing()",
                    "ReaderInput.click("),
            new Marker("focus-settles", "settlesOn("));

    /**
     * Raw keyboard dispatch outside the shared helper: the shape
     * that let two journeys press shortcuts no reader could have
     * pressed, masked in per-file premise counts by an unrelated
     * honest gesture (review). Counted so the gate can pin the
     * dispatchers to the files that establish focus for that very
     * gesture - the shared helper itself, and journeys that insist
     * before they press.
     */
    public static boolean dispatchesRawKeys(String rawSource) {
        String source = withoutComments(rawSource);
        return source.contains("KeyEvent.KEY_PRESSED")
                && source.contains("dispatchEvent");
    }

    private static final List<Marker> ROUTES = List.of(
            new Marker("pointer-events", "MouseEvent.MOUSE_PRESSED"),
            new Marker("keyboard-events", "KeyEvent.KEY_PRESSED",
                    "KeyEvent.KEY_TYPED"),
            new Marker("back-door-click", ".doClick("),
            new Marker("back-door-commit", ".postActionEvent("));

    private static final List<Marker> PLATFORM = List.of(
            new Marker("named-look-and-feel", "\"Metal\"", "Mac OS X"),
            new Marker("menu-shortcut-mask", "menuShortcut"),
            new Marker("os-name", "os.name"));

    /** Reads of live chart or Swing state a thread can get stale. */
    private static final List<String> LIVE_READS = List.of(
            ".currentScene()", ".pageOffsetY()", "navigation.state()");

    // ---- classification ----------------------------------------------

    /** Classifies one source, comments stripped first. */
    public static File classify(String path, String rawSource) {
        return classify(path, "test", rawSource);
    }

    /** Classifies one source of the given kind (test or executable). */
    public static File classify(String path, String kind,
                                String rawSource) {
        String source = withoutComments(rawSource);
        // The shared guard itself: its bodies ARE the captures and
        // restores every other file is measured against, and after
        // the suppression rework (#224 review) they run through
        // guarded() rather than a literal finally - so the rule
        // that serves the corpus would flag the instrument. Named
        // for what it is instead, and the gate pins that exactly
        // one file may claim the name.
        if (source.contains("class SwingSession")
                && source.contains("static void guarded(")) {
            return new File(path, kind, List.of("all-of-them"),
                    List.of(), "the-shared-guard", false, List.of(),
                    List.of(), List.of(), count(source, "invokeAndWait"),
                    0);
        }
        // A place a cleanup actually runs: a finally, a JUnit
        // AfterEach, or a JVM shutdown hook - the last for probes
        // whose success path is System.exit, where nothing written
        // after the click can ever run.
        boolean restorePlace = source.contains("finally")
                || source.contains("AfterEach")
                || source.contains("addShutdownHook");
        List<String> touched = new ArrayList<>();
        List<String> unprotected = new ArrayList<>();
        boolean anyShared = false;
        for (SharedState state : GLOBAL_STATE) {
            boolean touches = false;
            for (String touch : state.touches()) {
                touches |= source.contains(touch);
            }
            // The read-only witnesses, exempted by pinned name
            // rather than by shape: an earlier rule exempted every
            // source with no .node( call, which also exempted a
            // direct write to the root itself (review). The list is
            // pinned by the gate, and everything else that reaches
            // userRoot - roots, nodes, reads that turn out to write -
            // stays detectable.
            if (state.name().equals("preferences")
                    && READ_ONLY_WITNESSES.contains(path)) {
                touches = false;
            }
            if (!touches) {
                continue;
            }
            touched.add(state.name());
            // Each state has its own shared guards, matched with an
            // opening parenthesis so restoringLocale( cannot vouch
            // for a look and feel that restoring( would have.
            boolean guarded = false;
            for (String guard : state.sharedGuards()) {
                guarded |= source.contains(guard);
            }
            if (guarded) {
                anyShared = true;
                continue;
            }
            // Paired: THIS state's capture, THIS state's restoring
            // write, and a place to run it. An unrelated finally
            // cannot vouch for a state it never captured (review).
            boolean captured = state.captures().length == 0;
            for (String capture : state.captures()) {
                captured |= source.contains(capture);
            }
            boolean restores = false;
            for (String restore : state.restores()) {
                restores |= source.contains(restore);
            }
            if (!(captured && restores && restorePlace)) {
                unprotected.add(state.name());
            }
        }
        String stateClass;
        if (touched.isEmpty()) {
            stateClass = "touches-nothing";
        } else if (!unprotected.isEmpty()) {
            stateClass = "UNPROTECTED";
        } else if (anyShared) {
            stateClass = "protected-shared";
        } else {
            stateClass = "protected-locally";
        }
        int handOffs = count(source, "invokeAndWait");
        int reads = 0;
        for (String read : LIVE_READS) {
            reads += count(source, read);
        }
        return new File(path, kind, touched, unprotected, stateClass,
                !names(DISPLAY, source).isEmpty(),
                names(PREMISES, source), names(ROUTES, source),
                names(PLATFORM, source), handOffs, reads);
    }

    /**
     * The evidence executables under src: the study and packaging
     * mains, and the packaged acceptance - each a single-JVM run
     * whose look-and-feel dies with its process but whose preference
     * writes outlive it (review: the first scan looked only under
     * test/ and missed them).
     */
    /**
     * The measuring instruments themselves, excluded by name: their
     * marker definitions are string literals that read exactly like
     * the behaviour they detect, so scanning them reports the ruler
     * as a thing being measured (review). The list is pinned by the
     * gate so it cannot quietly grow into an exemption dump.
     */
    /**
     * Sources that only look at the preference store - a sync and a
     * nodeExists, nothing created, nothing written. Exempt by exact
     * name and pinned by the gate: a second witness arrives by
     * decision, and nothing is exempted by shape.
     */
    public static final List<String> READ_ONLY_WITNESSES = List.of(
            "juranometria/app/PrefsExistsProbe.java");

    public static final List<String> INSTRUMENTS = List.of(
            "src/juranometria/tool/TestEvidenceScan.java",
            "src/juranometria/tool/TestEvidenceStudyMain.java");

    public static List<File> scanEvidenceExecutables()
            throws IOException {
        List<File> files = new ArrayList<>();
        try (Stream<Path> tree =
                Files.walk(Path.of("src/juranometria/tool"))) {
            for (Path source : tree
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted().toList()) {
                String path = source.toString()
                        .replace(java.io.File.separatorChar, '/');
                if (INSTRUMENTS.contains(path)) {
                    continue;
                }
                files.add(classify(path, "executable",
                        Files.readString(source)));
            }
        }
        Path acceptance =
                Path.of("src/juranometria/app/PackagedAcceptanceMain.java");
        files.add(classify(acceptance.toString()
                        .replace(java.io.File.separatorChar, '/'),
                "executable", Files.readString(acceptance)));
        return files;
    }

    /** Every test source under the root, classified, sorted by path. */
    public static List<File> scan(Path root) throws IOException {
        List<File> files = new ArrayList<>();
        try (Stream<Path> tree = Files.walk(root)) {
            for (Path source : tree
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted().toList()) {
                files.add(classify(
                        root.relativize(source).toString()
                                .replace(java.io.File.separatorChar, '/'),
                        "test", Files.readString(source)));
            }
        }
        return files;
    }

    // ---- generated evidence ------------------------------------------

    /**
     * Which contract a study artifact lives under.
     *
     * <p>Renderer-drawn images are the default for a PNG, and their
     * contract is byte-reproducibility per machine - an earlier
     * classification lumped them in with widget photography and
     * quietly weakened a contract sprints of studies had honoured
     * (review). Widget-rendered artifacts are the named prefixes
     * below, produced by the mains that set a look and feel to
     * photograph Swing; a new one arrives by adding its name here,
     * which is a reviewed decision, not a habit. Text fixtures -
     * the SOFA oracle and the script that regenerates it - are
     * byte-exact committed data, not artwork. Captured evidence -
     * the {@code screenshot-} prefix - is an operating-system
     * screenshot no command of ours can regenerate: what the desktop
     * showed a reader at a stated moment (issue #245's Finder, Dock
     * and switcher inspections). Its contract is a pinned digest,
     * so a re-capture is a provenance event, never a regeneration.
     */
    public static String artifactClass(String fileName) {
        if (fileName.endsWith(".md")) {
            return "deterministic-report";
        }
        if (fileName.endsWith(".txt") || fileName.endsWith(".c")) {
            return "byte-exact-fixture";
        }
        for (String prefix : new String[] {"screenshot-"}) {
            if (fileName.startsWith(prefix)) {
                return "captured-evidence";
            }
        }
        for (String prefix : new String[] {"dialog-real"}) {
            if (fileName.startsWith(prefix)) {
                return "session-photograph";
            }
        }
        for (String prefix : new String[] {"controls-", "sidebar-",
                "deep-sky-tab", "tab-", "symbols-", "selection-"}) {
            if (fileName.startsWith(prefix)) {
                return "widget-rendered-inspection";
            }
        }
        return "renderer-drawn";
    }

    /**
     * Whether a test source reaches the application's real
     * preference store - by opening the bare node, or through any
     * production entry point that opens it on the caller's behalf.
     *
     * <p>The doors are <em>derived</em>, not remembered: an earlier
     * needle list named the three store factories and missed
     * {@code AppShutdown.real()}, and a remembered subset misses
     * whatever is added next (review). Every non-private static
     * method in production whose body carries the bare-node literal
     * is a door, found by {@link #realPreferenceDoors}, and the gate
     * pins the derived set so a new door arrives as a visible pin
     * change rather than a silent gap.
     */
    public static boolean opensRealPreferences(String rawSource)
            throws IOException {
        String source = withoutComments(rawSource);
        if (source.contains(".node(\"juranometria\")")) {
            return true;
        }
        for (String door : realPreferenceDoors()) {
            if (source.contains(door)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> doors;

    /**
     * Every production entry point to the real preference node, as
     * {@code Class.method(} needles, derived from the sources under
     * src/juranometria and sorted.
     */
    public static synchronized List<String> realPreferenceDoors()
            throws IOException {
        if (doors != null) {
            return doors;
        }
        List<String> found = new ArrayList<>();
        try (Stream<Path> tree = Files.walk(Path.of("src/juranometria"))) {
            for (Path source : tree
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted().toList()) {
                String name = source.getFileName().toString();
                found.addAll(doorsIn(
                        name.substring(0, name.length() - 5),
                        Files.readString(source)));
            }
        }
        doors = List.copyOf(found.stream().sorted().distinct().toList());
        return doors;
    }

    /**
     * The doors one production source declares: each non-private
     * static method whose body chunk - the text from its header to
     * the next static header - contains the bare-node literal. A
     * private method is not an entry point a test could call, so
     * the door it serves is the public one that wraps it.
     */
    static List<String> doorsIn(String className, String rawSource) {
        String source = withoutComments(rawSource);
        if (!source.contains(".node(\"juranometria\")")) {
            return List.of();
        }
        java.util.regex.Matcher headers = java.util.regex.Pattern
                .compile("(private\\s+)?static\\s+[\\w<>\\[\\].]+"
                        + "\\s+(\\w+)\\s*\\(")
                .matcher(source);
        List<int[]> starts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Boolean> visible = new ArrayList<>();
        while (headers.find()) {
            starts.add(new int[] {headers.start()});
            names.add(headers.group(2));
            visible.add(headers.group(1) == null);
        }
        // First the chunks that hold the literal themselves, any
        // visibility - a private holder is not a door, but a public
        // method that calls it in the same file is, which is how the
        // packaged acceptance's main reaches the store through its
        // private journey helpers (review: the set must be complete,
        // not remembered).
        List<String> holders = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            if (chunk(source, starts, i)
                    .contains(".node(\"juranometria\")")) {
                holders.add(names.get(i));
            }
        }
        List<String> found = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            if (!visible.get(i)) {
                continue;
            }
            String body = chunk(source, starts, i);
            boolean door = body.contains(".node(\"juranometria\")");
            for (String holder : holders) {
                door |= !names.get(i).equals(holder)
                        && body.contains(holder + "(");
            }
            if (door) {
                found.add(className + "." + names.get(i) + "(");
            }
        }
        return found;
    }

    private static String chunk(String source, List<int[]> starts,
                                int index) {
        int from = starts.get(index)[0];
        int to = index + 1 < starts.size() ? starts.get(index + 1)[0]
                : source.length();
        return source.substring(from, to);
    }

    // ----------------------------------------------------------------

    private static List<String> names(List<Marker> markers,
                                      String source) {
        List<String> found = new ArrayList<>();
        for (Marker marker : markers) {
            if (marker.in(source)) {
                found.add(marker.name());
            }
        }
        return found;
    }

    private static int count(String source, String needle) {
        int found = 0;
        for (int at = source.indexOf(needle); at >= 0;
                at = source.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }

    private static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");
    }
}
