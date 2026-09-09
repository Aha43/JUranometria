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

    /** Sees every repaint request in the JVM while installed. */
    private static final class RecordingRepaints extends RepaintManager {

        final List<JComponent> asked = new ArrayList<>();

        @Override
        public synchronized void addDirtyRegion(JComponent component,
                                                int x, int y,
                                                int width, int height) {
            asked.add(component);
            super.addDirtyRegion(component, x, y, width, height);
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
            ChartScene scene = onEdt(atlas.chart::currentScene);
            PageContents inventory = onEdt(atlas.modules::inventory);
            repaints.asked.clear();

            atlas.letter('E');

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
                ChartScene scene = onEdt(atlas.chart::currentScene);
                PageContents inventory = onEdt(atlas.modules::inventory);
                repaints.asked.clear();

                atlas.letter(letter);

                assertTrue(atlas.showing(letter),
                        what + ": the letter reached the module");
                assertTrue(repaints.asked.contains(atlas.chart)
                                || repaints.asked.contains(
                                        atlas.frame.getRootPane()),
                        what + ": and something was asked to repaint,"
                                + " so the lines arrive on the page"
                                + " rather than at the next thing that"
                                + " happens to move");
                assertSame(scene, onEdt(atlas.chart::currentScene),
                        what + ": on the same scene object - a module"
                                + " showing its own geometry"
                                + " reassembles nothing");
                assertSame(inventory, onEdt(atlas.modules::inventory),
                        what + ": and rebuilds no inventory");
                assertFalse(java.util.Arrays.equals(before,
                                atlas.painted()),
                        what + ": and the page is different, which is"
                                + " the whole point of the letter");
            }
        });
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
                ChartKeyboardSession.install(frame.getRootPane(), options,
                        ecliptic,
                        juranometria.ui.ecliptic.EclipticSession.toggle(
                                ecliptic, eclipticStore()),
                        observer, frame);
                frame.setVisible(true);
                chart.setViewState(ChartViewState.DEFAULT);
            });
            flush();
        }

        /** The page a reader navigated to. */
        void goTo(juranometria.chart.SkyPosition centre) throws Exception {
            SwingUtilities.invokeAndWait(() -> chart.setViewState(
                    new ChartViewState(centre, 36.0,
                            ChartViewState.defaultMagnitudeFor(36.0))));
            flush();
        }

        /** The prefix, then the letter, both as a reader presses them. */
        void letter(char key) throws Exception {
            palette = null;
            SwingUtilities.invokeAndWait(chart::requestFocusInWindow);
            flush();
            ReaderInput.shortcut(chart, KeyEvent.VK_K,
                    AppMenuBar.menuShortcutMask());
            flush();
            palette = shownPalette();
            assertNotNull(palette, "the chart keyboard opens on "
                    + ChartKeys.prefixText());
            ReaderInput.shortcutOn(palette,
                    Character.toUpperCase(key), 0);
            flush();
            ChartKeyboard open = palette;
            SwingUtilities.invokeAndWait(open::close);
            flush();
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
