package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.SymbolFamily;
import juranometria.ui.ReaderInput;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every switch, both routes, both ways (Sprint 31, issue #312).
 *
 * <p>The issue's acceptance is a matrix rather than a sample: each of
 * the chart's seventeen layers reached through <strong>the reader's
 * own checkbox and through the palette letter</strong>, in both
 * directions, from the same start, compared on three things that can
 * disagree independently -
 *
 * <ul>
 *   <li>the chart the reader is looking at now;</li>
 *   <li>what is <strong>written down</strong>, read back out of the
 *       store rather than out of the controller that wrote it;</li>
 *   <li>the <strong>page itself</strong>, drawn from each route's
 *       result, because a switch that changes an option and does not
 *       change the picture has not done what its name says.</li>
 * </ul>
 *
 * <p>A sample cannot hold this. One registry entry calling the wrong
 * transition, or persisting differently from its own control, is
 * exactly the sort of mistake that hides in the sixteen switches
 * nobody wrote a case for (review, #312).
 *
 * <p>The GUI route presses <strong>OK</strong>. The dialog's checkbox
 * previews and its OK commits; comparing an uncommitted preview with
 * the keyboard's committed action compares two different things and
 * would pass with a broken OK.
 */
class ChartKeyboardMatrixTest {

    /**
     * How the dialog names each switch, where that differs from the
     * registry's own label. The registry names things as a reader
     * reads them on the palette; the dialog is older and spells the
     * grid out. Pinned rather than guessed, and every chart switch
     * has to be found on the dialog or this test fails before it
     * compares anything.
     */
    private static final Map<String, String> AS_THE_DIALOG_SAYS =
            Map.of("chart.equatorialGrid", "Equatorial coordinate grid");

    private static final int WIDE = 480;
    private static final int HIGH = 360;

    /**
     * Pages to look for a switch's effect on. A layer draws nothing
     * where there is nothing of its kind: the default page has a
     * galaxy on it and no globular cluster, and asking one page to
     * show every family would only measure the page.
     */
    private static final List<ChartViewState> PAGES = List.of(
            ChartViewState.DEFAULT,
            new ChartViewState(new SkyPosition(83.0, 0.0), 12.0, 7.0),
            new ChartViewState(new SkyPosition(83.0, 0.0), 36.0, 6.0),
            new ChartViewState(new SkyPosition(250.4, 36.5), 12.0, 8.0),
            new ChartViewState(new SkyPosition(56.75, 24.1), 8.0, 8.0),
            new ChartViewState(new SkyPosition(283.4, 33.0), 8.0, 8.0),
            new ChartViewState(new SkyPosition(0.0, -16.0), 8.0, 8.0));

    @Test
    void everyChartSwitchAgreesThroughBothRoutesBothWays()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the reader's own controls are pressed in a window");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            Chart chart = new Chart();
            List<String> covered = new ArrayList<>();
            try {
                chart.open();
                for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
                    if (!toggle.id().startsWith("chart.")) {
                        continue;
                    }
                    covered.add(toggle.id());
                    for (boolean from : new boolean[] {false, true}) {
                        oneSwitch(chart, toggle, from);
                    }
                }
            } finally {
                chart.close();
            }
            assertEquals(17, covered.size(),
                    "every chart switch on the palette was walked, not"
                            + " a sample of them: " + covered);
        });
    }

    /** One switch, from one start, by both routes. */
    private void oneSwitch(Chart chart, ChartKeys.Toggle toggle,
                           boolean from) throws Exception {
        String what = toggle.label() + " from " + (from ? "on" : "off");
        ChartOptions start = starting(toggle, from);

        chart.store(start);
        ChartOptions byTheDialog = chart.throughTheDialog(toggle);
        chart.store(start);
        ChartOptions byTheKeyboard = chart.throughTheKeyboard(toggle);

        assertEquals(!from, on(byTheDialog, toggle.id()),
                what + ": the reader's own checkbox threw it");
        assertEquals(!from, on(byTheKeyboard, toggle.id()),
                what + ": and so did the letter");
        assertEquals(byTheDialog, byTheKeyboard,
                what + ": the two routes leave the same chart, in"
                        + " every one of its seventeen components -"
                        + " one that changed something else as well"
                        + " would differ here");
        assertTrue(toggle.persistent(),
                what + ": the chart's layers are the kind that is"
                        + " written down");

        // And the page a reader is looking at. Compared between the
        // routes, and required to differ from the start's - a switch
        // that agrees with itself and draws nothing has not done what
        // its name says.
        int page = pageShowing(start, byTheKeyboard, what);
        assertArrayEquals(drawn(page, byTheDialog), drawn(page, byTheKeyboard),
                what + ": and the same page, pixel for pixel, on the"
                        + " page this switch is visible on");
    }

    /**
     * The first of the pages where throwing this switch changes what
     * is drawn, or a failure naming every page that was tried.
     */
    private int pageShowing(ChartOptions before, ChartOptions after,
                            String what) throws Exception {
        for (int page = 0; page < PAGES.size(); page++) {
            if (!java.util.Arrays.equals(drawn(page, before),
                    drawn(page, after))) {
                return page;
            }
        }
        throw new AssertionError(what + ": no page in the atlas changed"
                + " when this switch was thrown - " + PAGES.size()
                + " tried, centred at "
                + PAGES.stream().map(state -> state.centre()
                        + " at " + state.fieldWidthDegrees() + " deg")
                        .toList());
    }

    // ---- the modules, through their own controls -------------------

    @Test
    void theModulesAgreeThroughTheirOwnControlsAndKeepTheirOwnPromise()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the reader's own controls are pressed in a window");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            Modules modules = new Modules();
            try {
                modules.open();
                for (boolean from : new boolean[] {false, true}) {
                    modules.ecliptic(from);
                    modules.observerLine("module.meridian", from);
                    modules.observerLine("module.horizon", from);
                }
            } finally {
                modules.close();
            }
        });
    }

    // ---- one window, the way the application builds it -------------

    private final class Chart {

        private JFrame frame;
        private javax.swing.JPanel page;
        private ChartOptionsController options;
        private ChartKeyboard palette;
        private java.util.prefs.Preferences node;

        void open() throws Exception {
            node = java.util.prefs.Preferences.userRoot().node(
                    "juranometria/test/chart-keyboard-matrix-"
                            + System.nanoTime());
            options = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("chart keyboard matrix");
                frame.setLayout(new BorderLayout());
                page = new javax.swing.JPanel();
                page.setFocusable(true);
                frame.add(page, BorderLayout.CENTER);
                frame.setSize(900, 600);
                ChartKeyboard.install(frame.getRootPane(),
                        ChartSwitches.of(options,
                                new NoEcliptic(), new NoLines()),
                        keyboard -> {
                            palette = keyboard;
                            keyboard.showIn(frame.getRootPane());
                        });
                frame.setVisible(true);
            });
            flush();
        }

        /** Both routes start from the same written-down chart. */
        void store(ChartOptions start) throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                options.apply(start);
                options.confirm();
            });
            flush();
            assertEquals(start, reloaded(),
                    "the start is what the store holds");
        }

        /** What the store holds now, read by somebody who did not write it. */
        ChartOptions reloaded() {
            return new ChartOptionsController(
                    ChartOptionsStore.forNode(node)).options();
        }

        /** The chart after a reader presses the checkbox and then OK. */
        ChartOptions throughTheDialog(ChartKeys.Toggle toggle)
                throws Exception {
            SwingUtilities.invokeAndWait(() ->
                    ChartOptionsDialog.open(frame, options));
            flush();
            JDialog dialog = dialogTitled("Chart Options");
            assertNotNull(dialog, "the reader's own dialog opens");
            try {
                String text = AS_THE_DIALOG_SAYS.getOrDefault(
                        toggle.id(), toggle.label());
                JTabbedPane tabs = onEdt(() -> tabs(dialog));
                for (String tab : List.of("Deep sky", "Stars",
                        "Constellations", "Chart")) {
                    JCheckBox found = onEdt(() -> box(dialog, text));
                    if (found != null && onEdt(found::isShowing)) {
                        break;
                    }
                    ReaderInput.chooseTab(tabs, tab);
                }
                JCheckBox box = onEdt(() -> box(dialog, text));
                assertNotNull(box, text + " is a control on the dialog");
                assertTrue(onEdt(box::isShowing) && onEdt(box::isEnabled),
                        text + " is a control a reader can press");
                ReaderInput.click(box, () -> new java.awt.Point(
                        box.getWidth() / 2, box.getHeight() / 2), 0);
                flush();

                // And the gesture the dialog asks for. Without this
                // the comparison is between an uncommitted preview
                // and the keyboard's committed action, and a broken
                // OK route passes (review, #312).
                JButton ok = onEdt(() -> button(dialog, "OK"));
                assertNotNull(ok, "the dialog's own OK");
                ReaderInput.click(ok);
                flush();
            } finally {
                SwingUtilities.invokeAndWait(dialog::dispose);
                flush();
            }
            return reloaded();
        }

        /** The chart after a reader opens the keyboard and presses a letter. */
        ChartOptions throughTheKeyboard(ChartKeys.Toggle toggle)
                throws Exception {
            palette = null;
            SwingUtilities.invokeAndWait(page::requestFocusInWindow);
            flush();
            ReaderInput.shortcut(page, KeyEvent.VK_K,
                    AppMenuBar.menuShortcutMask());
            flush();
            assertNotNull(palette, "the chart keyboard opens on "
                    + ChartKeys.prefixText());
            ChartKeyboard open = palette;
            boolean was = ChartSwitches.of(options, new NoEcliptic(),
                    new NoLines()).on(toggle.id());
            letter(open, toggle.key());
            assertEquals(!was, ChartSwitches.of(options, new NoEcliptic(),
                            new NoLines()).on(toggle.id()),
                    toggle.label() + ": the letter " + toggle.key()
                            + " reached its switch through the"
                            + " palette's own binding");
            SwingUtilities.invokeAndWait(open::close);
            flush();
            return reloaded();
        }

        void close() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                for (Window open : Window.getWindows()) {
                    if (open instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
                if (frame != null) {
                    frame.dispose();
                }
            });
            if (node != null) {
                node.removeNode();
            }
        }
    }

    // ---- the modules' own windows -----------------------------------

    private final class Modules {

        private JFrame frame;
        private javax.swing.JPanel page;
        private javax.swing.JMenuBar bar;
        private juranometria.meridian.MeridianModule observer;
        private juranometria.ecliptic.EclipticModule ecliptic;
        private final List<Boolean> eclipticSaved = new ArrayList<>();
        private final List<String> placeSaved = new ArrayList<>();
        private ChartKeyboard palette;
        private java.util.prefs.Preferences node;

        void open() throws Exception {
            node = java.util.prefs.Preferences.userRoot().node(
                    "juranometria/test/chart-keyboard-modules-"
                            + System.nanoTime());
            observer = new juranometria.meridian.MeridianModule(
                    new juranometria.sky.Observer(59.9, 10.7,
                            java.time.Instant.parse(
                                    "2026-01-01T21:00:00Z")));
            ecliptic = new juranometria.ecliptic.EclipticModule();
            Runnable theMenusOwnSwitch =
                    juranometria.ui.ecliptic.EclipticSession.toggle(
                            ecliptic, eclipticStore());
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("chart keyboard modules");
                frame.setLayout(new BorderLayout());
                page = new javax.swing.JPanel();
                page.setFocusable(true);
                frame.add(page, BorderLayout.CENTER);
                frame.setSize(900, 600);
                bar = AppMenuBar.create(null, null, () -> { }, () -> { },
                        () -> { }, () -> { }, theMenusOwnSwitch);
                frame.setJMenuBar(bar);
                ChartKeyboard.install(frame.getRootPane(),
                        ChartSwitches.of(
                                new ChartOptionsController(
                                        ChartOptionsStore.forNode(node)),
                                new ChartSwitches.Ecliptic() {
                                    @Override
                                    public boolean showing() {
                                        return ecliptic.showing();
                                    }

                                    @Override
                                    public void toggle() {
                                        theMenusOwnSwitch.run();
                                    }
                                },
                                new ChartSwitches.ObserverLines() {
                                    @Override
                                    public boolean meridianShowing() {
                                        return observer.meridianShowing();
                                    }

                                    @Override
                                    public boolean horizonShowing() {
                                        return observer.horizonShowing();
                                    }

                                    @Override
                                    public void showing(boolean line,
                                                        boolean horizon) {
                                        observer.showing(line, horizon,
                                                observer.zenithShowing());
                                    }
                                }),
                        keyboard -> {
                            palette = keyboard;
                            keyboard.showIn(frame.getRootPane());
                        });
                frame.setVisible(true);
            });
            flush();
        }

        /**
         * The ecliptic: the View menu's own item against the letter,
         * from the same start, both ways.
         *
         * <p>A menu item's action is its whole surface, which is the
         * convention the test-evidence decision records; the popup it
         * lives in is shown and measured in its own test.
         */
        void ecliptic(boolean from) throws Exception {
            String what = "The ecliptic from " + (from ? "on" : "off");
            javax.swing.JCheckBoxMenuItem item =
                    AppMenuBar.eclipticItem(bar);
            assertNotNull(item, "the View menu carries the ecliptic");

            set(() -> ecliptic.showing(from));
            int savedBefore = eclipticSaved.size();
            int drawnBefore = ecliptic.contributedGeometry().size();
            // A menu item's action is its whole surface, which is
            // the convention docs/decisions/test-evidence.md records
            // and the scanner counts. Written so it is counted.
            SwingUtilities.invokeAndWait(() -> item.doClick());
            flush();
            boolean byTheMenu = ecliptic.showing();
            int drawnAfter = ecliptic.contributedGeometry().size();
            assertEquals(!from, byTheMenu,
                    what + ": the menu item threw it");
            assertEquals(savedBefore + 1, eclipticSaved.size(),
                    what + ": and wrote the choice down, which is what"
                            + " the ecliptic's own session has done"
                            + " since it was built");
            assertTrue(drawnAfter != drawnBefore,
                    what + ": and the module offers the page different"
                            + " geometry - " + drawnBefore
                            + " contributions against " + drawnAfter);

            set(() -> ecliptic.showing(from));
            savedBefore = eclipticSaved.size();
            press('I');
            assertEquals(byTheMenu, ecliptic.showing(),
                    what + ": the letter leaves the ecliptic where the"
                            + " menu item left it");
            assertEquals(drawnAfter, ecliptic.contributedGeometry().size(),
                    what + ": with the same geometry on the page");
            assertEquals(savedBefore + 1, eclipticSaved.size(),
                    what + ": through the same one path to the store -"
                            + " the keyboard adds no second one");
            assertTrue(ChartKeys.toggle("module.ecliptic").persistent(),
                    what + ": which is what the registry says of it");
        }

        /**
         * A line of the observer's: the Place and Time checkbox
         * against the letter, both ways, with the promise the atlas
         * makes about it - that nothing is written down.
         */
        void observerLine(String id, boolean from) throws Exception {
            ChartKeys.Toggle toggle = ChartKeys.toggle(id);
            String what = toggle.label() + " from " + (from ? "on" : "off");
            String control = "module.meridian".equals(id)
                    ? "Meridian" : "Mathematical horizon";

            set(() -> lines(id, from));
            int drawnBefore = observer.contributedGeometry().size();
            byTheDialog(control);
            boolean byTheControl = on(id);
            int drawnAfter = observer.contributedGeometry().size();
            assertEquals(!from, byTheControl,
                    what + ": the reader's own checkbox threw it");
            assertTrue(drawnAfter != drawnBefore,
                    what + ": and the module offers the page different"
                            + " geometry - " + drawnBefore
                            + " contributions against " + drawnAfter);
            // What Place and Time writes down is a place. Pressing
            // any control commits whichever field the caret was in,
            // and that gesture saves a latitude and a longitude - so
            // this counts the traffic rather than forbidding it, and
            // reads what was written.
            int placesSaved = placeSaved.size();
            for (String written : placeSaved) {
                assertEquals(place(), written,
                        what + ": what the store was given is the"
                                + " reader's place and nothing about"
                                + " what is drawn");
            }

            set(() -> lines(id, from));
            press(toggle.key());
            assertEquals(byTheControl, on(id),
                    what + ": the letter leaves the same line drawn");
            assertEquals(drawnAfter, observer.contributedGeometry().size(),
                    what + ": with the same geometry on the page");
            assertEquals(placesSaved, placeSaved.size(),
                    what + ": and the keyboard wrote nothing down at"
                            + " all - the lines last as long as the"
                            + " window and no longer");
            assertFalse(toggle.persistent(),
                    what + ": which is what the registry says of it");
        }

        private boolean on(String id) {
            return "module.meridian".equals(id)
                    ? observer.meridianShowing()
                    : observer.horizonShowing();
        }

        /** The reader's place, as anything storing it would write it. */
        private String place() {
            return observer.observer().latitudeDegrees() + ","
                    + observer.observer().eastLongitudeDegrees();
        }

        private void lines(String id, boolean to) {
            boolean line = "module.meridian".equals(id)
                    ? to : observer.meridianShowing();
            boolean horizon = "module.horizon".equals(id)
                    ? to : observer.horizonShowing();
            observer.showing(line, horizon, observer.zenithShowing());
        }

        /** The reader's own Place and Time control, pressed. */
        private void byTheDialog(String control) throws Exception {
            SwingUtilities.invokeAndWait(() ->
                    juranometria.ui.placeandtime.PlaceAndTimeDialog.open(
                            frame, observer, placeStore(),
                            () -> observer.observer().instant()));
            flush();
            JDialog dialog = dialogTitled("Place and Time");
            assertNotNull(dialog, "the reader's own dialog opens");
            try {
                JCheckBox box = onEdt(() -> box(dialog, control));
                assertNotNull(box, control + " is a control on it");
                assertTrue(onEdt(box::isShowing),
                        control + " is a control a reader can press");
                ReaderInput.click(box, () -> new java.awt.Point(
                        box.getWidth() / 2, box.getHeight() / 2), 0);
                flush();
            } finally {
                SwingUtilities.invokeAndWait(dialog::dispose);
                flush();
            }
        }

        /** A store that says what it was asked to remember. */
        private juranometria.ui.ecliptic.EclipticStore eclipticStore() {
            return new juranometria.ui.ecliptic.EclipticStore() {
                @Override
                public java.util.Optional<Boolean> shown() {
                    return eclipticSaved.isEmpty()
                            ? java.util.Optional.empty()
                            : java.util.Optional.of(eclipticSaved.get(
                                    eclipticSaved.size() - 1));
                }

                @Override
                public void save(boolean shown) {
                    eclipticSaved.add(shown);
                }

                @Override
                public void flush() {
                }
            };
        }

        private juranometria.ui.placeandtime.PlaceStore placeStore() {
            return new juranometria.ui.placeandtime.PlaceStore() {
                @Override
                public juranometria.sky.Observer load(
                        java.time.Instant instant) {
                    return observer.observer().at(instant);
                }

                @Override
                public void save(double latitude, double eastLongitude) {
                    placeSaved.add(latitude + "," + eastLongitude);
                }

                @Override
                public boolean remembered() {
                    return true;
                }

                @Override
                public void flush() {
                }
            };
        }

        private void press(char letter) throws Exception {
            palette = null;
            SwingUtilities.invokeAndWait(page::requestFocusInWindow);
            flush();
            ReaderInput.shortcut(page, KeyEvent.VK_K,
                    AppMenuBar.menuShortcutMask());
            flush();
            assertNotNull(palette, "the chart keyboard opens on "
                    + ChartKeys.prefixText());
            letter(palette, letter);
            SwingUtilities.invokeAndWait(palette::close);
            flush();
        }

        private void set(Runnable what) throws Exception {
            SwingUtilities.invokeAndWait(what);
            flush();
        }

        void close() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                for (Window open : Window.getWindows()) {
                    if (open instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
                if (frame != null) {
                    frame.dispose();
                }
            });
            if (node != null) {
                node.removeNode();
            }
        }
    }

    // ---- the chart as each route leaves it --------------------------

    private static final Map<String, byte[]> PAINTED = new HashMap<>();
    private static final List<ChartScene> SCENES = new ArrayList<>();

    /** The page drawn with these options, as this machine draws it. */
    private static byte[] drawn(int page, ChartOptions options)
            throws Exception {
        String key = page + " " + options;
        byte[] known = PAINTED.get(key);
        if (known != null) {
            return known;
        }
        while (SCENES.size() <= page) {
            SCENES.add(Atlas.assembler().assemble(
                    PAGES.get(SCENES.size()), WIDE, HIGH));
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new ChartRenderer(StarSizePolicy.DEFAULT)
                .renderToImage(SCENES.get(page), options), "png", bytes);
        byte[] painted = bytes.toByteArray();
        PAINTED.put(key, painted);
        return painted;
    }

    /** The chart every case starts from, with this switch set. */
    private static ChartOptions starting(ChartKeys.Toggle toggle,
                                         boolean on) {
        ChartOptions start = ChartOptions.DEFAULTS;
        if (toggle.dependsOn() != null) {
            start = with(start, toggle.dependsOn(), true);
        }
        return with(start, toggle.id(), on);
    }

    /**
     * The same chart with one switch set, built by the production
     * transition rather than by a second copy of it. Setting up a
     * start state with its own switch statement would be one more
     * place for the seventeen components to be dropped.
     */
    private static ChartOptions with(ChartOptions from, String id,
                                     boolean to) {
        if (on(from, id) == to) {
            return from;
        }
        ChartOptions[] held = {from};
        ChartOptionsController controller = new ChartOptionsController(
                new ChartOptionsStore() {
                    @Override
                    public ChartOptions load() {
                        return held[0];
                    }

                    @Override
                    public void save(ChartOptions next) {
                        held[0] = next;
                    }
                });
        ChartSwitches.of(controller, new NoEcliptic(), new NoLines())
                .toggle(id);
        return controller.options();
    }

    private static boolean on(ChartOptions options, String id) {
        for (SymbolFamily family : SymbolFamily.values()) {
            if (id.equals("chart." + name(family))) {
                return options.family(family);
            }
        }
        return switch (id) {
            case ChartKeys.DEEP_SKY -> options.deepSkyObjects();
            case "chart.deepSkyLabels" -> options.deepSkyLabels();
            case "chart.starNames" -> options.starNames();
            case "chart.bayerLetters" -> options.bayerLetters();
            case "chart.flamsteedNumbers" -> options.flamsteedNumbers();
            case ChartKeys.FIGURES -> options.constellationFigures();
            case "chart.constellationBoundaries" ->
                    options.constellationBoundaries();
            case "chart.constellationNames" ->
                    options.constellationNames();
            case "chart.equatorialGrid" -> options.equatorialGrid();
            case "chart.titleBlock" -> options.titleBlock();
            case "chart.magnitudeKey" -> options.magnitudeKey();
            case "chart.blackSky" -> options.palette()
                    == juranometria.render.ChartPalette.BLACK_SKY;
            default -> throw new IllegalArgumentException(
                    "no such switch: " + id);
        };
    }

    private static String name(SymbolFamily family) {
        return switch (family) {
            case GALAXIES -> "galaxies";
            case OPEN_CLUSTERS -> "openClusters";
            case GLOBULAR_CLUSTERS -> "globularClusters";
            case NEBULAE -> "nebulae";
            case PLANETARY_NEBULAE -> "planetaryNebulae";
        };
    }

    // ---- the plumbing a journey needs -------------------------------

    private static final class NoEcliptic
            implements ChartSwitches.Ecliptic {

        private boolean showing;

        @Override
        public boolean showing() {
            return showing;
        }

        @Override
        public void toggle() {
            showing = !showing;
        }
    }

    private static final class NoLines
            implements ChartSwitches.ObserverLines {

        private boolean meridian;
        private boolean horizon;

        @Override
        public boolean meridianShowing() {
            return meridian;
        }

        @Override
        public boolean horizonShowing() {
            return horizon;
        }

        @Override
        public void showing(boolean line, boolean sky) {
            meridian = line;
            horizon = sky;
        }
    }

    private static JDialog dialogTitled(String title) throws Exception {
        JDialog[] found = new JDialog[1];
        SwingUtilities.invokeAndWait(() -> {
            for (Window open : Window.getWindows()) {
                if (open instanceof JDialog dialog && dialog.isVisible()
                        && title.equals(dialog.getTitle())) {
                    found[0] = dialog;
                }
            }
        });
        return found[0];
    }

    private static JTabbedPane tabs(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTabbedPane strip) {
                return strip;
            }
            if (child instanceof Container inside) {
                JTabbedPane found = tabs(inside);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JCheckBox box(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JCheckBox check
                    && text.equals(check.getText())) {
                return check;
            }
            if (child instanceof Container inside) {
                JCheckBox found = box(inside, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton button(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton pressed
                    && text.equals(pressed.getText())) {
                return pressed;
            }
            if (child instanceof Container inside) {
                JButton found = button(inside, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * A letter, pressed on the shown palette by a reader.
     *
     * <p>Not {@code press(char)}. The palette binds each letter in
     * its own input map, and a route test that calls the method the
     * binding calls proves the arithmetic and nothing about the
     * keyboard: an absent binding, a letter bound to the wrong
     * action, or a palette that never took the focus would all pass
     * (review, #312). The shared helper insists this palette is the
     * focus owner and then dispatches the key.
     */
    private static void letter(ChartKeyboard palette, char key)
            throws Exception {
        ReaderInput.shortcutOn(palette,
                Character.toUpperCase(key), 0);
        flush();
    }

    private static <T> T onEdt(Callable<T> ask) throws Exception {
        Object[] answer = new Object[1];
        Exception[] trouble = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                answer[0] = ask.call();
            } catch (Exception failure) {
                trouble[0] = failure;
            }
        });
        if (trouble[0] != null) {
            throw trouble[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) answer[0];
        return typed;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
