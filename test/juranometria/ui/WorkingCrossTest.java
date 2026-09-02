package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.module.NavigationRequest;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageVisibility;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.chart.StarSizePolicy;
import juranometria.ui.onthispage.OnThisPageModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The working crosses (Sprint 24, issue #216).
 *
 * <p>A cross is <strong>interaction ink</strong>: it answers a
 * reader's gesture, not the sky. So the questions here are about
 * where it lands, when it appears at all, and - most of all - that
 * the page a reader has not marked anything on is the page the atlas
 * has always drawn.
 */
class WorkingCrossTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(centre, field, 8.0), 900, 700);
    }

    /** The pixel production itself would place this position at. */
    private static PixelPoint productionPixel(ChartScene scene,
                                              SkyPosition position) {
        return new GnomonicProjection(scene.viewport().centre())
                .project(position)
                .map(new ViewportMapping(scene.viewport())::toPixel)
                .orElseThrow();
    }

    private static BufferedImage inked(ChartScene scene,
                                       List<juranometria.module.OverlayRegistry
                                               .Owned> contributions,
                                       String lead) {
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            WorkingCrossInk.paint(g, scene, contributions, lead);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static juranometria.module.OverlayRegistry.Owned crossAt(
            String identity, SkyPosition where) {
        return new juranometria.module.OverlayRegistry.Owned("on-this-page",
                new juranometria.module.OverlayContribution.Point(identity,
                        "working mark on " + identity, where,
                        juranometria.module.InkRole.INTERACTION));
    }

    @Test
    void aCrossLandsOnTheProjectionsOwnPixelAtTheSeamAndAtAPole() {
        // The two places a projection is easiest to get wrong, and
        // the reason the cross is given a sky position rather than a
        // pixel: a module that computed pixels would be
        // reimplementing the projection and would be wrong here.
        for (SkyPosition centre : List.of(
                new SkyPosition(0.0, 0.0),        // the RA seam
                new SkyPosition(359.9, -0.2),     // just across it
                new SkyPosition(0.0, 89.5))) {    // a pole
            ChartScene scene = page(centre, 8.0);
            SkyPosition object = juranometria.page.PageExtent.offsetOf(
                    centre, 1.5, 40.0);
            PixelPoint expected = productionPixel(scene, object);

            BufferedImage image = inked(scene,
                    List.of(crossAt("TEST", object)), null);

            // The ink's own centre against the projection's pixel,
            // rather than four probes: an arm drawn at a fractional
            // row lands on one pixel row or the next, and a test
            // that pinned the row would fail for the rounding rather
            // than for the geometry - which is how the first version
            // of this failed.
            double[] box = inkBounds(image);
            // Within the resolution of a one-pixel stroke: the ink
            // box's centre is a whole-pixel quantity, and a stroke
            // drawn at a fractional coordinate covers one pixel
            // strongly and its neighbour faintly, so the measured
            // centre sits up to a pixel below the true one.
            assertTrue(Math.hypot(box[0] - expected.x(),
                            box[1] - expected.y()) <= 1.5,
                    String.format("the cross is centred on the pixel"
                            + " the projection gives - %.2f,%.2f"
                            + " against %.2f,%.2f - at %s",
                            box[0], box[1], expected.x(), expected.y(),
                            centre));
            assertTrue(box[2] >= 6.0 && box[3] <= 16.0,
                    String.format("and it is a cross rather than a"
                            + " blob or a speck: %.0f x %.0f px",
                            box[2], box[3]));
            assertFalse(isInk(image, (int) Math.round(expected.x()),
                            (int) Math.round(expected.y())),
                    "with the position itself left clear, so it stays"
                            + " readable through the mark, at " + centre);
        }
    }

    @Test
    void theLeadWearsTheChartsOwnSelectionTreatment() {
        ChartScene scene = page(new SkyPosition(10.684, 41.269), 8.0);
        SkyPosition object = juranometria.page.PageExtent.offsetOf(
                scene.viewport().centre(), 1.0, 90.0);
        PixelPoint at = productionPixel(scene, object);

        BufferedImage plain = inked(scene,
                List.of(crossAt("TEST", object)), null);
        BufferedImage leading = inked(scene,
                List.of(crossAt("TEST", object)), "TEST");

        assertTrue(inkCount(leading) > inkCount(plain),
                "the lead carries more than a bare cross, so a reader"
                        + " can see which row their facts belong to");
        assertTrue(isInk(leading, (int) Math.round(at.x()) + 9,
                        (int) Math.round(at.y())),
                "the ring the chart already uses for a selection");
    }

    @Test
    void geometryTheChartHasNotDecidedHowToInkIsLeftAlone() {
        // Roles for modules this sprint does not build. Drawing them
        // as something would be the chart inventing cartography on a
        // module's behalf.
        ChartScene scene = page(new SkyPosition(10.684, 41.269), 8.0);
        SkyPosition object = scene.viewport().centre();
        var reference = new juranometria.module.OverlayRegistry.Owned(
                "meridian", new juranometria.module.OverlayContribution.Path(
                        "line", "a meridian",
                        List.of(object, juranometria.page.PageExtent.offsetOf(
                                object, 1.0, 0.0)),
                        juranometria.module.InkRole.REFERENCE_LINE));

        assertEquals(0, inkCount(inked(scene, List.of(reference), null)),
                "silence is the honest answer to geometry the chart"
                        + " has no ink for yet");
    }

    @Test
    void aPageWithNothingMarkedIsTheChartTheAtlasHasAlwaysDrawn()
            throws Exception {
        // The promise the decision makes and #216 requires: with no
        // working marks, ordinary rendering is byte-identical - not
        // nearly, and not "no visible difference".
        BufferedImage withoutModule = paintedComponent(false);
        BufferedImage withModule = paintedComponent(true);

        assertTrue(identical(withoutModule, withModule),
                "attaching the module changes no pixel of an unmarked"
                        + " page");
    }

    @Test
    void onlyTheMarkedObjectsThePageDoesNotDrawGetACross() throws Exception {
        Host host = new Host();
        try {
            PageContents page = host.services.inventory();

            String drawn = firstWith(page, PageVisibility.DRAWN);
            String undrawn = firstUndrawn(page);
            host.services.workingMarks().replaceWith(
                    List.of(drawn, undrawn), undrawn);

            // Read through the chart's own registry rather than
            // from the module: what the chart collects is what a
            // reader sees.
            List<String> crossed = crossedIdentities(host);
            assertEquals(List.of(undrawn), crossed,
                    "the visible object keeps its own symbol, and"
                            + " marking it adds no second mark for the"
                            + " same thing");
            assertTrue(host.services.workingMarks().isMarked(drawn),
                    "though it is certainly marked");
        } finally {
            host.dispose();
        }
    }

    @Test
    void severalInvisibleObjectsProduceExactlyTheirOwnCrosses()
            throws Exception {
        Host host = new Host();
        try {
            PageContents page = host.services.inventory();
            List<String> undrawn = new ArrayList<>();
            for (PageEntry entry : page.entries()) {
                if (entry.visibility() != PageVisibility.DRAWN
                        && undrawn.size() < 4) {
                    undrawn.add(entry.identity());
                }
            }
            host.services.workingMarks().replaceWith(undrawn,
                    undrawn.get(undrawn.size() - 1));

            List<String> crossed = crossedIdentities(host);
            assertEquals(undrawn, crossed,
                    "exactly their crosses, in the order they were"
                            + " marked - no more and no fewer");
        } finally {
            host.dispose();
        }
    }

    @Test
    void paintingAndMarkingBuildNoInventoryOfTheirOwn() throws Exception {
        // "No catalogue query during paint or a selection-only
        // repaint" is a property of when the inventory is built, so
        // it is measured by counting rebuilds rather than asserted in
        // prose. The host tells its listeners whenever it builds one.
        Host host = new Host();
        try {
            int[] rebuilds = {0};
            host.services.onPageChange(page -> rebuilds[0]++);
            rebuilds[0] = 0;

            String undrawn = firstUndrawn(host.services.inventory());
            host.services.workingMarks().mark(undrawn);
            for (int i = 0; i < 5; i++) {
                paint(host.chart);
            }

            assertEquals(0, rebuilds[0],
                    "marking and repainting build nothing: the page"
                            + " has not changed, so the inventory has"
                            + " not either");
        } finally {
            host.dispose();
        }
    }

    // ----------------------------------------------------------------

    /** A real chart component with the module attached. */
    private static final class Host {
        final ChartComponent chart;
        final ChartModuleHost services;
        final OnThisPageModule module;
        final List<NavigationRequest> requests = new ArrayList<>();

        Host() throws Exception {
            ChartComponent[] made = new ChartComponent[1];
            SwingUtilities.invokeAndWait(() -> {
                made[0] = new ChartComponent(Atlas.assembler());
                made[0].setSize(900, 700);
                made[0].setViewState(ChartViewState.DEFAULT);
            });
            chart = made[0];
            services = new ChartModuleHost(chart, new SelectionModel(),
                    requests::add);
            module = services.attach(new OnThisPageModule());
        }

        void dispose() {
            services.detachAll();
        }
    }

    /** What the chart would ink, in the order it collected it. */
    private static List<String> crossedIdentities(Host host) {
        List<String> crossed = new ArrayList<>();
        for (var owned : host.chart.overlays().collect()) {
            assertEquals(OnThisPageModule.ID, owned.moduleId());
            crossed.add(owned.geometry().identity());
        }
        return crossed;
    }

    private static String firstWith(PageContents page, PageVisibility state) {
        for (PageEntry entry : page.entries()) {
            if (entry.visibility() == state) {
                return entry.identity();
            }
        }
        throw new AssertionError("no entry is " + state + " on this page");
    }

    private static String firstUndrawn(PageContents page) {
        for (PageEntry entry : page.entries()) {
            if (entry.visibility() != PageVisibility.DRAWN) {
                return entry.identity();
            }
        }
        throw new AssertionError("this page draws everything on it");
    }

    private static BufferedImage paintedComponent(boolean withModule)
            throws Exception {
        ChartComponent[] made = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            made[0] = new ChartComponent(Atlas.assembler());
            made[0].setSize(900, 700);
            made[0].setViewState(ChartViewState.DEFAULT);
        });
        ChartModuleHost host = null;
        if (withModule) {
            host = new ChartModuleHost(made[0], new SelectionModel(),
                    request -> { });
            host.attach(new OnThisPageModule());
        }
        try {
            return paint(made[0]);
        } finally {
            if (host != null) {
                host.detachAll();
            }
        }
    }

    private static BufferedImage paint(ChartComponent chart)
            throws Exception {
        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
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

    /** The centre and size of everything inked: {x, y, width, height}. */
    private static double[] inkBounds(BufferedImage image) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xffffff) != 0xffffff) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            throw new AssertionError("nothing was inked at all");
        }
        return new double[] {(minX + maxX) / 2.0, (minY + maxY) / 2.0,
                maxX - minX + 1, maxY - minY + 1};
    }

    private static boolean isInk(BufferedImage image, int x, int y) {
        if (x < 0 || y < 0 || x >= image.getWidth()
                || y >= image.getHeight()) {
            return false;
        }
        return (image.getRGB(x, y) & 0xffffff) != 0xffffff;
    }

    private static int inkCount(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xffffff) != 0xffffff) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean identical(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
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
