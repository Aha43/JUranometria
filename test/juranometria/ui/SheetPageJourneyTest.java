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
            assertEquals(ChartViewState.DEFAULT, navigation.state(),
                    "1. the reader starts where the atlas opens");
            BufferedImage home = paint(chart);
            int homeStars = chart.currentScene().stars().size();

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
                        navigation.state().fieldWidthDegrees(),
                        "and the press lands on " + expected);
            }
            assertFalse(out.isEnabled(),
                    "2. the sheet page is the end of the sequence, and"
                            + " the control says so rather than sitting"
                            + " there dead");

            ChartScene sheet = chart.currentScene();
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
            SkyPosition beforePan = navigation.state().centre();
            ReaderInput.drag(chart, 500, 380, 380, 300);
            SwingUtilities.invokeAndWait(() -> { });
            SkyPosition afterPan = navigation.state().centre();
            assertTrue(afterPan.separationDegrees(beforePan) > 1.0,
                    "3. grab-to-pan moves the sheet page: "
                            + afterPan.separationDegrees(beforePan)
                            + " degrees");
            assertEquals(42.0, navigation.state().fieldWidthDegrees(),
                    "and panning does not change the field");

            // ---- 4. identify, out here, on the widened page ----------
            ChartRenderer.DrawnMark star = markOn(chart);
            SwingUtilities.invokeAndWait(() -> {
                int x = (int) Math.round(star.centre().x());
                int y = (int) Math.round(star.centre().y())
                        + chart.pageOffsetY();
                for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                        MouseEvent.MOUSE_RELEASED}) {
                    chart.dispatchEvent(new MouseEvent(chart, id,
                            System.nanoTime() / 1_000_000,
                            MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                            MouseEvent.BUTTON1));
                }
            });
            SwingUtilities.invokeAndWait(() -> { });
            Selection.Object identified = assertInstanceOf(
                    Selection.Object.class, selection.selection(),
                    "4. a star on the sheet page identifies like a star"
                            + " on any other page");
            assertEquals(star.star().id(), identified.catalogueId(),
                    "and it is the star that was under the pointer");
            assertFalse(heard.isEmpty(),
                    "and the seam told whoever was listening");

            // ---- 5. the working selection, out here -----------------
            SwingUtilities.invokeAndWait(() ->
                    working.add(identified.catalogueId()));
            assertTrue(working.isMember(identified.catalogueId()),
                    "5. and it can be kept, on this page as on any"
                            + " other");

            // ---- 6. Home, pressed, exact ----------------------------
            ReaderInput.click(button(toolbar, "Reset view"));
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(ChartViewState.DEFAULT, navigation.state(),
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

    /** A star well inside the current page, to click on. */
    private static ChartRenderer.DrawnMark markOn(ChartComponent chart)
            throws Exception {
        ChartRenderer.DrawnMark[] chosen = new ChartRenderer.DrawnMark[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartScene scene = chart.currentScene();
            chosen[0] = RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)
                    .stream()
                    .filter(mark -> mark.star() != null)
                    .filter(mark -> mark.centre().x() > 150
                            && mark.centre().x() < scene.viewport().widthPx() - 150
                            && mark.centre().y() > 120
                            && mark.centre().y() < scene.viewport().heightPx() - 120)
                    .findFirst().orElseThrow();
        });
        return chosen[0];
    }
}
