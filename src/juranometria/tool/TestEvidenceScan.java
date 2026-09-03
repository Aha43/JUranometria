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
 * <p>What a textual rule cannot decide is stated rather than
 * papered over: whether a particular read happens on the event
 * thread is a property of control flow, not of text, and the
 * decision document assigns that discipline to named helpers and to
 * the race tests that already hold it (#220), not to this scan.
 */
public final class TestEvidenceScan {

    private TestEvidenceScan() {
    }

    /** One test source, classified. */
    public record File(String path,
                       List<String> globalState,
                       List<String> protections,
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

    private static final List<Marker> GLOBAL_STATE = List.of(
            new Marker("look-and-feel", "UIManager.setLookAndFeel"),
            new Marker("default-font", "UIManager.put(\"defaultFont\""),
            new Marker("locale", "Locale.setDefault"),
            new Marker("time-zone", "TimeZone.setDefault"),
            new Marker("repaint-manager",
                    "RepaintManager.setCurrentManager"),
            new Marker("preferences", "Preferences.userRoot"));

    private static final List<Marker> PROTECTIONS = List.of(
            new Marker("SwingSession", "SwingSession.restoring"),
            new Marker("finally-restore", "finally"),
            new Marker("after-each", "AfterEach"),
            new Marker("node-removed", "removeNode()"));

    private static final List<Marker> DISPLAY = List.of(
            new Marker("display",
                    "assumeFalse(GraphicsEnvironment.isHeadless",
                    "assumeFalse(java.awt.GraphicsEnvironment.isHeadless"));

    private static final List<Marker> PREMISES = List.of(
            new Marker("focused-window", "insistOnFocus"),
            new Marker("focus-owner", "isFocusOwner()"),
            new Marker("point-reachable", "getVisibleRect().contains"),
            new Marker("control-showing", "isShowing()"),
            new Marker("focus-settles", "settlesOn("));

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
        String source = withoutComments(rawSource);
        List<String> state = names(GLOBAL_STATE, source);
        List<String> shields = names(PROTECTIONS, source);
        String stateClass;
        if (state.isEmpty()) {
            stateClass = "touches-nothing";
        } else if (shields.contains("SwingSession")) {
            stateClass = "protected-shared";
        } else if (shields.contains("finally-restore")
                || shields.contains("after-each")) {
            stateClass = "protected-locally";
        } else {
            stateClass = "UNPROTECTED";
        }
        int handOffs = count(source, "invokeAndWait");
        int reads = 0;
        for (String read : LIVE_READS) {
            reads += count(source, read);
        }
        return new File(path, state, shields, stateClass,
                !names(DISPLAY, source).isEmpty(),
                names(PREMISES, source), names(ROUTES, source),
                names(PLATFORM, source), handOffs, reads);
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
                        Files.readString(source)));
            }
        }
        return files;
    }

    /**
     * Whether a test source opens the application's real preference
     * node. Zero is the standing state and the gate holds it there:
     * a test that wrote to the reader's own store would be a test
     * editing somebody's settings.
     */
    public static boolean opensRealPreferences(String rawSource) {
        return withoutComments(rawSource)
                .contains(".node(\"juranometria\")");
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
