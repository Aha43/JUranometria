package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.app.SwingSession;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionMode;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.chart.WorkingSelection;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader's walk out to the sheet page and back (issue #284).
 *
 * <p>The gate bought one new step. A step is not a number in an
 * array: it is a page a reader reaches with the control they already
 * use, works on the way they already work, and leaves the way they
 * already leave. So this walks out to it through the real toolbar,
 * pans it, identifies a star on it, puts that star in the working
 * selection, and presses Reset view - and requires the page that
 * comes back to be the released Home page to the pixel.
 */
class SheetPageJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static BufferedImage paint(ChartComponent chart)
            throws Exception {
        BufferedImage image = new BufferedImage(chart.getWidth(),
                chart.getHeight(), BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image;
    }

    private static int differences(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Read live state on the event thread, which is where it lives.
     * Every reading in this journey - the view state, the assembled
     * page, what the selection holds - goes through here, so none of
     * them races the thread that produces them (#284 review).
     */
    private static <T> T onEdt(java.util.concurrent.Callable<T> read)
            throws Exception {
        Object[] held = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                held[0] = read.call();
            } catch (Exception thrown) {
                failed[0] = thrown;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T value = (T) held[0];
        return value;
    }

    private static JButton button(AtlasToolbar toolbar, String name) {
        for (java.awt.Component each : toolbar.getComponents()) {
            if (each instanceof JButton press
                    && name.equals(press.getAccessibleContext()
                            .getAccessibleName())) {
                return press;
            }
        }
        throw new AssertionError("no control named " + name);
    }

    @Test
    void theReaderZoomsOutToTheSheetPageWorksOnItAndComesHome()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "pressing a real control needs a display");

        JFrame[] window = new JFrame[1];
        SwingSession.guarded(() -> {
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartComponent[] chartHolder = new ChartComponent[1];
            AtlasToolbar[] toolbarHolder = new AtlasToolbar[1];
            SelectionModel selection = new SelectionModel();
            WorkingSelection working = new WorkingSelection();
            List<SelectionModel.Change> heard = new ArrayList<>();

            SwingUtilities.invokeAndWait(() -> {
                ChartComponent chart = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart::setViewState);
                PanInteraction.install(chart, navigation);
                ZoomInteraction.install(chart, navigation);
                SelectInteraction.install(chart, selection, working,
                        new SelectionMode());
                selection.onChange(heard::add);
                chart.setViewState(ChartViewState.DEFAULT);
                chart.setPreferredSize(new java.awt.Dimension(900, 700));
                toolbarHolder[0] = new AtlasToolbar(navigation,
                        new SearchField(Atlas.search(), Atlas.assembler(),
                                navigation));
                window[0] = new JFrame("sheet-page-journey");
                window[0].setLayout(new BorderLayout());
                window[0].add(toolbarHolder[0], BorderLayout.NORTH);
                window[0].add(chart, BorderLayout.CENTER);
                window[0].pack();
                window[0].setVisible(true);
                chartHolder[0] = chart;
            });
            SwingUtilities.invokeAndWait(() -> { });
            ChartComponent chart = chartHolder[0];
            AtlasToolbar toolbar = toolbarHolder[0];

            // ---- 1. the released Home page ---------------------------
            assertEquals(ChartViewState.DEFAULT,
                    onEdt(navigation::state),
                    "1. the reader starts where the atlas opens");
            BufferedImage home = paint(chart);
            int homeStars = onEdt(() -> chart.currentScene().stars().size());

            // ---- 2. out to the sheet page, one control ---------------
            // Zoom out is the control a reader already has. Five
            // presses is the released journey plus the one new step.
            JButton out = button(toolbar, "Zoom out");
            for (double expected : new double[] {12.0, 18.0, 24.0, 36.0,
                    42.0}) {
                assertTrue(out.isEnabled(),
                        "2. zoom out is live on the way to " + expected);
                ReaderInput.click(out);
                assertEquals(expected,
                        onEdt(() -> navigation.state()
                                .fieldWidthDegrees()),
                        "and the press lands on " + expected);
            }
            // The sheet page was the end of the sequence until #299
            // put the overview's three rungs above it. The reader
            // stops here because this is the page they came for, not
            // because the control ran out.
            assertTrue(out.isEnabled(),
                    "2. there is more sky beyond the sheet page now,"
                            + " and the control says so");

            ChartScene sheet = onEdt(chart::currentScene);
            assertEquals(42.0, sheet.viewport().fieldWidthDegrees(),
                    "2. the page the reader is on is the sheet page");
            assertTrue(sheet.stars().size() > homeStars,
                    "and it holds more sky than Home: "
                            + sheet.stars().size() + " against "
                            + homeStars);
            BufferedImage sheetPage = paint(chart);
            assertTrue(differences(home, sheetPage) > 100_000,
                    "which is a different page, drawn");

            // ---- 3. panning still works out here ---------------------
            SkyPosition beforePan = onEdt(() -> navigation.state().centre());
            ReaderInput.drag(chart, 500, 380, 380, 300);
            SkyPosition afterPan = onEdt(() -> navigation.state().centre());
            assertTrue(afterPan.separationDegrees(beforePan) > 1.0,
                    "3. grab-to-pan moves the sheet page: "
                            + afterPan.separationDegrees(beforePan)
                            + " degrees");
            assertEquals(42.0,
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "and panning does not change the field");

            // ---- 4. identify, out here, on the widened page ----------
            // The mark is chosen and clicked inside ONE event-thread
            // turn. Deriving it in one turn and clicking in the next
            // resolves the click against whatever page the chart has
            // by then, which is the stale-scene race #220 was about
            // (PR #289 review).
            ChartRenderer.DrawnMark star = clickOn(chart, 0,
                    SheetPageJourneyTest::firstStar);
            Selection.Object identified = assertInstanceOf(
                    Selection.Object.class, onEdt(selection::selection),
                    "4. a star on the sheet page identifies like a star"
                            + " on any other page");
            assertEquals(star.star().id(), identified.catalogueId(),
                    "and it is the star that was under the pointer");
            assertFalse(heard.isEmpty(),
                    "and the seam told whoever was listening");

            // ---- 5. the working selection, out here -----------------
            // Through the chart, not by writing to the model. An
            // ordinary click replaces the working selection and the
            // platform's additive modifier toggles the next one in -
            // the semantics of docs/decisions/working-selection.md,
            // reached the way a reader reaches them. Writing to
            // WorkingSelection here would have passed with the
            // chart-to-selection wiring severed (PR #289 review).
            assertEquals(List.of(identified.catalogueId()),
                    onEdt(working::members),
                    "5. the same click that identified the star put it"
                            + " in the working selection");

            ChartRenderer.DrawnMark second = clickOn(chart,
                    SelectInteraction.toggleModifierMask(),
                    scene -> otherStar(scene, star));
            assertEquals(List.of(identified.catalogueId(),
                            second.star().id()),
                    onEdt(working::members),
                    "and the additive modifier toggles a second one in"
                            + " beside it, on this page as on any"
                            + " other");
            assertEquals(second.star().id(), onEdt(working::lead),
                    "with the newcomer leading");

            // ---- 6. Home, pressed, exact ----------------------------
            ReaderInput.click(button(toolbar, "Reset view"));
            assertEquals(ChartViewState.DEFAULT, onEdt(navigation::state),
                    "6. Reset view comes all the way home from the new"
                            + " step, as it does from every old one");
            assertEquals(0, differences(home, paint(chart)),
                    "and the page that comes back is the released Home"
                            + " page to the pixel");
            assertNotEquals(0, differences(home, sheetPage),
                    "which is a claim about this journey rather than"
                            + " about two identical images");
        }, () -> SwingUtilities.invokeAndWait(() -> {
            if (window[0] != null) {
                window[0].dispose();
            }
        }));
    }

    /**
     * Choose a mark from the page and click it <em>in the same
     * event-thread turn</em>, so nothing can reassemble the scene
     * between the pixel being derived and the press landing on it.
     */
    private static ChartRenderer.DrawnMark clickOn(ChartComponent chart,
            int modifiers,
            java.util.function.Function<ChartScene,
                    ChartRenderer.DrawnMark> choose) throws Exception {
        ChartRenderer.DrawnMark[] chosen = new ChartRenderer.DrawnMark[1];
        ReaderInput.click(chart, () -> {
            chosen[0] = choose.apply(chart.currentScene());
            return new java.awt.Point(
                    (int) Math.round(chosen[0].centre().x()),
                    (int) Math.round(chosen[0].centre().y())
                            + chart.pageOffsetY());
        }, modifiers);
        return chosen[0];
    }

    /** A star well inside the page, clear of its neighbours. */
    private static ChartRenderer.DrawnMark firstStar(ChartScene scene) {
        return wellInside(scene).findFirst().orElseThrow();
    }

    /** Another one, far enough away that the click cannot land on both. */
    private static ChartRenderer.DrawnMark otherStar(ChartScene scene,
            ChartRenderer.DrawnMark first) {
        return wellInside(scene)
                .filter(mark -> mark.centre().x() - first.centre().x() > 60
                        || first.centre().x() - mark.centre().x() > 60)
                .findFirst().orElseThrow();
    }

    private static java.util.stream.Stream<ChartRenderer.DrawnMark>
            wellInside(ChartScene scene) {
        return RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS).stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 150
                        && mark.centre().x() < scene.viewport().widthPx() - 150
                        && mark.centre().y() > 120
                        && mark.centre().y() < scene.viewport().heightPx() - 120)
                .filter(mark -> alone(scene, mark));
    }

    /** No other mark close enough to be a candidate for the same click. */
    private static boolean alone(ChartScene scene,
                                 ChartRenderer.DrawnMark mark) {
        return RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS).stream()
                .filter(other -> !other.equals(mark))
                .noneMatch(other -> Math.hypot(
                        other.centre().x() - mark.centre().x(),
                        other.centre().y() - mark.centre().y()) < 12.0);
    }
}
