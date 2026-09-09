package juranometria.ui;

import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.app.ChartKeyboard;
import juranometria.app.ChartKeyboardSession;
import juranometria.app.ChartKeys;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsStore;
import juranometria.app.SwingSession;
import juranometria.app.TargetRetirement;
import juranometria.app.UiTheme;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.page.PageContents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a letter does to the atlas that is running (Sprint 31, issue
 * #312).
 *
 * <p>The matrix proves that every switch reaches the same transition
 * by both routes, and it does that against the chart's options rather
 * than against a chart. That is one claim short of the issue's, and
 * the missing one is the one a reader would notice first: <em>the
 * page in front of them changes, and changes cheaply</em>.
 *
 * <p>A controller listener that was never connected, a module adapter
 * that forgot to ask for a repaint, or a route that quietly
 * reassembles the page would pass every test in the matrix and leave
 * a reader pressing a letter at a chart that does not move (review,
 * #312). So this builds the atlas the way {@code JUranometriaMain}
 * builds it - the real chart component, the real module host, the
 * real modules through their own session seams, the same
 * {@link ChartKeyboardSession} the application installs - and watches
 * both sides of the seam at once:
 *
 * <ul>
 *   <li>a recording {@link RepaintManager} sees the repaint request
 *       itself, so "nothing less than a paint" is evidence rather
 *       than hope;</li>
 *   <li>the scene and the inventory are compared <strong>by
 *       identity</strong>, because both are cached and a rebuild
 *       hands back a new object - so "nothing more than a paint" is
 *       evidence too;</li>
 *   <li>and the component is painted before and after, so the claim
 *       is about the picture and not about a field.</li>
 * </ul>
 *
 * <p>The control is in {@link #aRealPageChangeDoesRebuildEverything}:
 * the same instruments, a real page change, and every one of them
 * moves. Without it this class would be an elaborate way of asserting
 * nothing.
 */
class ChartKeyboardWiringTest {

    /**
     * Sees every repaint request in the JVM while installed -
     * <strong>including the ones aimed at a window</strong>.
     *
     * <p>The second overload is not decoration. {@code frame.repaint()}
     * never reaches the component overload, so a recorder that watched
     * only components would have counted the two routes as equal while
     * one of them painted the whole window again (review, #312).
     */
    private static final class RecordingRepaints extends RepaintManager {

        final List<JComponent> asked = new ArrayList<>();
        final List<java.awt.Window> windows = new ArrayList<>();

        @Override
        public synchronized void addDirtyRegion(JComponent component,
                                                int x, int y,
                                                int width, int height) {
            asked.add(component);
            super.addDirtyRegion(component, x, y, width, height);
        }

        @Override
        public void addDirtyRegion(java.awt.Window window, int x, int y,
                                   int width, int height) {
            windows.add(window);
            super.addDirtyRegion(window, x, y, width, height);
        }

        void clear() {
            asked.clear();
            windows.clear();
        }
    }

    @Test
    void aChartLayerLetterRepaintsTheShownChartAndRebuildsNothing()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the prefix and its letters are pressed in a window");
        withTheAtlasRunning((atlas, repaints) -> {
            assertTrue(atlas.options.options().equatorialGrid(),
                    "the grid is on to begin with");
            byte[] before = atlas.painted();
            atlas.openPalette();
            ChartScene scene = onEdt(atlas.chart::currentScene);
            PageContents inventory = onEdt(atlas.modules::inventory);
            repaints.clear();

            atlas.dispatch('E');

            assertFalse(atlas.options.options().equatorialGrid(),
                    "the letter reached the chart's own options");
            assertTrue(repaints.asked.contains(atlas.chart),
                    "and the chart was asked to repaint - a route that"
                            + " changed the option and asked for"
                            + " nothing would leave the grid on screen"
                            + " until something else happened to move");
            assertSame(scene, onEdt(atlas.chart::currentScene),
                    "the very same scene object: choosing what the"
                            + " chart draws is a repaint, and nothing"
                            + " was reassembled or asked of the"
                            + " catalogue to do it");
            assertNotSame(inventory, onEdt(atlas.modules::inventory),
                    "and the inventory is rebuilt, which is the host's"
                            + " own rule for a chart option - what is"
                            + " on this page reports what is drawn, and"
                            + " one built from a stale options set"
                            + " would call a hidden object drawn. So"
                            + " this is evidence the letter reached the"
                            + " host as well as the renderer");
            atlas.closePalette();
            assertFalse(java.util.Arrays.equals(before, atlas.painted()),
                    "and the page a reader is looking at is different");
        });
    }

    @Test
    void everyModuleLetterRepaintsTheShownChartAndRebuildsNothing()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the prefix and its letters are pressed in a window");
        withTheAtlasRunning((atlas, repaints) -> {
            // Each of the three, from its own start, through the
            // adapters the application installs. These are the ones
            // a matrix against the options cannot reach at all: they
            // do not live in the chart's options and never did.
            for (char letter : new char[] {'I', 'R', 'H'}) {
                String what = ChartKeys.forKey(letter).label();
                // On a page the module's own geometry crosses. A line
                // drawn somewhere else is still not drawn here, and
                // the default page is the study's own "page-nothing".
                juranometria.sky.LocalSky sky =
                        new juranometria.sky.LocalSky(
                                atlas.observer.observer());
                atlas.goTo(switch (letter) {
                    // The vernal equinox is on the ecliptic by
                    // definition; the zenith is on the meridian by
                    // definition; and the horizon is the one line a
                    // page at the zenith cannot reach, being ninety
                    // degrees away from it - the study's own
                    // "horizon edge".
                    case 'I' -> new juranometria.chart.SkyPosition(0.0, 0.0);
                    case 'R' -> sky.zenith();
                    default -> sky.horizon().around(72).get(9);
                });
                byte[] before = atlas.painted();
                atlas.openPalette();
                ChartScene scene = onEdt(atlas.chart::currentScene);
                PageContents inventory = onEdt(atlas.modules::inventory);
                repaints.clear();

                atlas.dispatch(letter);

                assertTrue(atlas.showing(letter),
                        what + ": the letter reached the module");
                assertTrue(repaints.asked.contains(atlas.chart),
                        what + ": and the chart itself was asked to"
                                + " repaint, through the module's own"
                                + " redraw seam - the palette's own ink"
                                + " is not an answer, which is why it"
                                + " is opened before the recorder is"
                                + " cleared: " + repaints.asked);
                assertSame(scene, onEdt(atlas.chart::currentScene),
                        what + ": on the same scene object - a module"
                                + " showing its own geometry"
                                + " reassembles nothing");
                assertSame(inventory, onEdt(atlas.modules::inventory),
                        what + ": and rebuilds no inventory");
                atlas.closePalette();
                assertFalse(java.util.Arrays.equals(before,
                                atlas.painted()),
                        what + ": and the page is different, which is"
                                + " the whole point of the letter");
            }
        });
    }

    @Test
    void theReadersOwnControlAndTheLetterCostTheSame() throws Exception {
        // The issue asks for identical rendered effect *and* identical
        // repaint cost. Two routes that both end up showing the line
        // can still differ in what they ask the toolkit to do, and one
        // of them did: the ecliptic's adapter painted the whole frame
        // again on top of the module's own redraw, so the letter cost
        // more than the menu item for the same change (review, #312).
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the reader's own controls are pressed in a window");
        withTheAtlasRunning((atlas, repaints) -> {
            for (char letter : new char[] {'I', 'R', 'H'}) {
                String what = ChartKeys.forKey(letter).label();
                atlas.goTo(pageFor(atlas, letter));

                atlas.hide(letter);
                if (letter != 'I') {
                    atlas.openPlaceAndTime(letter);
                }
                Cost byTheControl = atlas.measure(repaints,
                        () -> atlas.readersControl(letter));
                assertTrue(atlas.showing(letter),
                        what + ": the reader's own control showed it");
                if (letter != 'I') {
                    atlas.closePlaceAndTime();
                }

                atlas.hide(letter);
                atlas.openPalette();
                Cost byTheLetter = atlas.measure(repaints,
                        () -> atlas.dispatch(letter));
                assertTrue(atlas.showing(letter),
                        what + ": and so did the letter");
                atlas.closePalette();

                assertEquals(byTheControl.chartRepaints,
                        byTheLetter.chartRepaints,
                        what + ": and asked the chart to repaint the"
                                + " same number of times - " + byTheControl
                                + " against " + byTheLetter);
                assertEquals(0, byTheControl.aboveTheChart,
                        what + ": the reader's own control repaints"
                                + " nothing above the chart - "
                                + byTheControl);
                assertEquals(0, byTheLetter.aboveTheChart,
                        what + ": and neither does the letter, which"
                                + " once painted the whole window again"
                                + " on top of the module's own redraw - "
                                + byTheLetter);
                assertEquals(byTheControl.windowRepaints,
                        byTheLetter.windowRepaints,
                        what + ": and painted the whole window again"
                                + " the same number of times - which"
                                + " for both of them is none, the"
                                + " module's own redraw being the"
                                + " route");
                assertEquals(byTheControl.reassembled,
                        byTheLetter.reassembled,
                        what + ": reassembling the page exactly as"
                                + " often, which is not at all");
                assertEquals(byTheControl.rebuilt, byTheLetter.rebuilt,
                        what + ": and rebuilding the inventory as"
                                + " often");
            }
        });
    }

    /**
     * What one gesture asked of the atlas.
     *
     * <p>Two counts, because the bug this holds asked nothing of the
     * chart at all: it painted the whole window again on top of the
     * module's own redraw. So everything <em>above</em> the chart is
     * counted too - the content pane, the layered pane, the root
     * pane, the frame - and neither route may touch any of them.
     *
     * <p>Each route's own control surface is left out of both counts,
     * because it is not shared: the palette redraws its own line and
     * the menu redraws its own tick, and neither is a cost the other
     * could have. Nothing above the chart is either route's control
     * surface, which is what makes that count comparable.
     */
    private record Cost(int chartRepaints, int aboveTheChart,
                        int windowRepaints, boolean reassembled,
                        boolean rebuilt) {
    }

    /** The page each module's own geometry crosses. */
    private static juranometria.chart.SkyPosition pageFor(Atlas atlas,
                                                          char letter) {
        juranometria.sky.LocalSky sky = new juranometria.sky.LocalSky(
                atlas.observer.observer());
        // The vernal equinox is on the ecliptic by definition; the
        // zenith is on the meridian by definition; and the horizon is
        // the one line a page at the zenith cannot reach, being ninety
        // degrees away from it - the study's own "horizon edge".
        return switch (letter) {
            case 'I' -> new juranometria.chart.SkyPosition(0.0, 0.0);
            case 'R' -> sky.zenith();
            default -> sky.horizon().around(72).get(9);
        };
    }

    @Test
    void aRealPageChangeDoesRebuildEverything() throws Exception {
        // The control. If the identities survived a rebuild too, the
        // two tests above would pass whatever the routes did.
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "built the way the application builds it");
        withTheAtlasRunning((atlas, repaints) -> {
            ChartScene scene = onEdt(atlas.chart::currentScene);
            PageContents inventory = onEdt(atlas.modules::inventory);

            SwingUtilities.invokeAndWait(() -> atlas.chart.setViewState(
                    new ChartViewState(
                            new juranometria.chart.SkyPosition(83.822,
                                    -5.391), 8.0, 8.0)));
            flush();

            assertFalse(scene == onEdt(atlas.chart::currentScene),
                    "a real page change reassembles the scene, so the"
                            + " sameness above is evidence and not"
                            + " deafness");
            assertFalse(inventory == onEdt(atlas.modules::inventory),
                    "and rebuilds the inventory");
        });
    }

    // ---- the atlas, built the way the application builds it ---------

    private interface Body {
        void run(Atlas rig, RecordingRepaints repaints) throws Exception;
    }

    private void withTheAtlasRunning(Body body) throws Exception {
        RecordingRepaints repaints = new RecordingRepaints();
        SwingSession.restoring(() -> SwingSession.restoringRepaintManager(
                () -> {
                    UiTheme.apply(false);
                    RepaintManager.setCurrentManager(repaints);
                    Atlas rig = new Atlas();
                    try {
                        rig.open();
                        body.run(rig, repaints);
                    } finally {
                        rig.close();
                    }
                }));
    }

    /**
     * The application's own wiring: the chart the reader looks at,
     * the modules attached through their own session seams, and the
     * chart keyboard installed by the same call {@code main} makes.
     */
    private static final class Atlas {

        JFrame frame;
        ChartComponent chart;
        ChartModuleHost modules;
        ChartOptionsController options;
        juranometria.meridian.MeridianModule observer;
        juranometria.ecliptic.EclipticModule ecliptic;
        ChartViewController navigation;
        Runnable eclipticToggle;
        final List<Boolean> eclipticSaved = new ArrayList<>();
        ChartKeyboard palette;
        java.util.prefs.Preferences node;

        void open() throws Exception {
            node = java.util.prefs.Preferences.userRoot().node(
                    "juranometria/test/chart-keyboard-wiring-"
                            + System.nanoTime());
            options = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            navigation = new ChartViewController(
                    juranometria.app.Atlas.assembler()::fits);
            SwingUtilities.invokeAndWait(() -> {
                chart = new ChartComponent(
                        juranometria.app.Atlas.assembler());
                navigation.onChange(chart::setViewState);
                // The same one call that connects the options to the
                // chart in the application.
                TargetRetirement.connect(options, chart, navigation);
                modules = new ChartModuleHost(chart, new SelectionModel(),
                        request -> { });
                observer = juranometria.ui.placeandtime
                        .PlaceAndTimeSession.begin(modules,
                                placeStore(), java.time.Instant.parse(
                                        "2026-03-20T21:33:00Z"));
                ecliptic = juranometria.ui.ecliptic.EclipticSession
                        .begin(modules);
                frame = new JFrame("chart keyboard wiring");
                frame.setLayout(new java.awt.BorderLayout());
                frame.add(chart, java.awt.BorderLayout.CENTER);
                frame.setSize(900, 700);
                // One switch, handed to both routes, exactly as the
                // application hands the View menu and the palette the
                // ecliptic's own session switch.
                eclipticToggle = juranometria.ui.ecliptic.EclipticSession
                        .toggle(ecliptic, eclipticStore());
                frame.setJMenuBar(AppMenuBar.create(null, null, () -> { },
                        () -> { }, () -> { }, () -> { }, eclipticToggle));
                ChartKeyboardSession.install(frame.getRootPane(), options,
                        ecliptic, eclipticToggle, observer);
                frame.setVisible(true);
                chart.setViewState(ChartViewState.DEFAULT);
            });
            flush();
        }

        /** What one gesture cost, and nothing else's. */
        Cost measure(RecordingRepaints repaints, Gesture gesture)
                throws Exception {
            ChartScene scene = onEdt(chart::currentScene);
            PageContents inventory = onEdt(modules::inventory);
            repaints.clear();
            gesture.make();
            int chartRepaints = (int) repaints.asked.stream()
                    .filter(asked -> asked == chart).count();
            int aboveTheChart = (int) repaints.asked.stream()
                    .filter(asked -> asked != chart)
                    .filter(asked -> SwingUtilities.isDescendingFrom(
                            chart, asked))
                    .count();
            // The chart's own window, and not every window: a
            // dialog painting itself is its own control surface, as
            // the palette's labels are the letter's, and one desktop
            // repaints it where another does not.
            int windowRepaints = (int) repaints.windows.stream()
                    .filter(asked -> asked == frame).count();
            return new Cost(chartRepaints, aboveTheChart,
                    windowRepaints,
                    scene != onEdt(chart::currentScene),
                    inventory != onEdt(modules::inventory));
        }

        /** Puts a module's line away again, without measuring it. */
        void hide(char letter) throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                switch (letter) {
                    case 'I' -> ecliptic.showing(false);
                    case 'R' -> observer.showing(false,
                            observer.horizonShowing(),
                            observer.zenithShowing());
                    default -> observer.showing(
                            observer.meridianShowing(), false,
                            observer.zenithShowing());
                }
            });
            flush();
        }

        /**
         * The reader's own control for this module: the View menu's
         * Ecliptic item, or the Place and Time checkbox.
         */
        void readersControl(char letter) throws Exception {
            if (letter == 'I') {
                javax.swing.JCheckBoxMenuItem item =
                        AppMenuBar.eclipticItem(frame.getJMenuBar());
                assertNotNull(item, "the View menu carries it");
                // A menu item's action is its whole surface, which is
                // the convention docs/decisions/test-evidence.md
                // records and the scanner counts.
                SwingUtilities.invokeAndWait(() -> item.doClick());
                flush();
                return;
            }
            javax.swing.JCheckBox box = onEdt(() -> checkBox(
                    placeAndTime, letter == 'R'
                            ? "Meridian" : "Mathematical horizon"));
            assertNotNull(box, "Place and Time carries it");
            ReaderInput.click(box, () -> new java.awt.Point(
                    box.getWidth() / 2, box.getHeight() / 2), 0);
            flush();
        }

        /**
         * The reader's own Place and Time window, opened before the
         * measurement starts - for the same reason the palette is.
         */
        void openPlaceAndTime(char letter) throws Exception {
            SwingUtilities.invokeAndWait(() ->
                    juranometria.ui.placeandtime.PlaceAndTimeDialog.open(
                            frame, observer, placeStore(),
                            () -> observer.observer().instant()));
            flush();
            placeAndTime = onEdt(() -> {
                for (java.awt.Window open : java.awt.Window.getWindows()) {
                    if (open instanceof javax.swing.JDialog dialog
                            && dialog.isVisible()
                            && "Place and Time".equals(
                                    dialog.getTitle())) {
                        return dialog;
                    }
                }
                return null;
            });
            assertNotNull(placeAndTime, "the reader's own dialog opens");
            // And the caret out of the fields before anything is
            // counted. A field commits when it is left, and
            // committing a place asks the chart to redraw - a
            // second gesture of the reader's, held in the matrix,
            // and not part of what pressing a checkbox costs.
            javax.swing.JCheckBox box = onEdt(() -> checkBox(
                    placeAndTime, letter == 'R'
                            ? "Meridian" : "Mathematical horizon"));
            assertNotNull(box, "Place and Time carries it");
            SwingUtilities.invokeAndWait(box::requestFocusInWindow);
            flush();
        }

        void closePlaceAndTime() throws Exception {
            javax.swing.JDialog open = placeAndTime;
            SwingUtilities.invokeAndWait(open::dispose);
            flush();
            placeAndTime = null;
        }

        private javax.swing.JDialog placeAndTime;

        /** The page a reader navigated to. */
        void goTo(juranometria.chart.SkyPosition centre) throws Exception {
            SwingUtilities.invokeAndWait(() -> chart.setViewState(
                    new ChartViewState(centre, 36.0,
                            ChartViewState.defaultMagnitudeFor(36.0))));
            flush();
        }

        /**
         * The prefix, as a reader presses it.
         *
         * <p>Separate from the letter on purpose. Opening and closing
         * the palette dirties the layered pane on its own, so a
         * measurement that spanned all three could be answered by
         * palette ink and a deleted module redraw would go unnoticed
         * (review, #312). What is measured is the letter.
         */
        void openPalette() throws Exception {
            palette = null;
            SwingUtilities.invokeAndWait(chart::requestFocusInWindow);
            flush();
            ReaderInput.shortcut(chart, KeyEvent.VK_K,
                    AppMenuBar.menuShortcutMask());
            flush();
            palette = shownPalette();
            assertNotNull(palette, "the chart keyboard opens on "
                    + ChartKeys.prefixText());
        }

        /** One letter on the open palette, and nothing else. */
        void dispatch(char key) throws Exception {
            ReaderInput.shortcutOn(palette,
                    Character.toUpperCase(key), 0);
            flush();
        }

        void closePalette() throws Exception {
            ChartKeyboard open = palette;
            SwingUtilities.invokeAndWait(open::close);
            flush();
            palette = null;
        }

        /** The palette the application's own opener put on the window. */
        private ChartKeyboard shownPalette() throws Exception {
            return onEdt(() -> {
                for (java.awt.Component child
                        : frame.getRootPane().getLayeredPane()
                                .getComponents()) {
                    if (child instanceof ChartKeyboard found) {
                        return found;
                    }
                }
                return null;
            });
        }

        boolean showing(char letter) {
            return switch (letter) {
                case 'I' -> ecliptic.showing();
                case 'R' -> observer.meridianShowing();
                case 'H' -> observer.horizonShowing();
                default -> throw new IllegalArgumentException(
                        "not a module letter: " + letter);
            };
        }

        /** The chart as it is on the screen now. */
        byte[] painted() throws Exception {
            BufferedImage image = new BufferedImage(
                    Math.max(1, chart.getWidth()),
                    Math.max(1, chart.getHeight()),
                    BufferedImage.TYPE_INT_ARGB);
            SwingUtilities.invokeAndWait(() -> {
                java.awt.Graphics2D g = image.createGraphics();
                chart.paint(g);
                g.dispose();
            });
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            return bytes.toByteArray();
        }

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
                    return new juranometria.sky.Observer(59.913, 10.752,
                            instant);
                }

                @Override
                public void save(double latitude, double eastLongitude) {
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

        void close() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
            if (node != null) {
                node.removeNode();
            }
        }
    }

    /** One thing a reader does, whose cost is being measured. */
    private interface Gesture {
        void make() throws Exception;
    }

    private static javax.swing.JCheckBox checkBox(java.awt.Container root,
                                                  String text) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof javax.swing.JCheckBox box
                    && text.equals(box.getText())) {
                return box;
            }
            if (child instanceof java.awt.Container inside) {
                javax.swing.JCheckBox found = checkBox(inside, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
