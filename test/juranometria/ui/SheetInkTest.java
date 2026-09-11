package juranometria.ui;

import java.awt.Graphics2D;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetRecorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ink a chart hands a sheet (issue #287).
 *
 * <p>The working selection is two marks, not one. A reader who has
 * marked objects sees a <strong>ring</strong> around each one the
 * page actually draws, and a <strong>cross</strong> for each one it
 * does not - never both treatments for the same object, and never
 * neither. A sheet that carried only the crosses would give a reader
 * who marked ten visible stars a chart with nothing on it, and would
 * look correct in every test that only counted ink (PR #292 review).
 */
class SheetInkTest {

    private static final ChartViewState ORION = new ChartViewState(
            new SkyPosition(83.0, 0.0), 42.0, 6.0);

    private static ChartComponent chart() throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler());
            holder[0].setSize(PaperSize.A4.chartWideUnits(),
                    PaperSize.A4.chartHighUnits());
            holder[0].setViewState(ORION);
        });
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
    }

    @Test
    void everyMarkedObjectThePageDrawsGetsItsRing() throws Exception {
        ChartScene scene = Atlas.assembler().assemble(ORION,
                PaperSize.A4.chartWideUnits(),
                PaperSize.A4.chartHighUnits());
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        List<ChartRenderer.DrawnMark> marks =
                renderer.drawnMarks(scene, ChartOptions.DEFAULTS);

        // Three the page draws, spread across it.
        List<ChartRenderer.DrawnMark> chosen = marks.stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 100
                        && mark.centre().x() < 600)
                .limit(3).toList();
        assertEquals(3, chosen.size(), "the page has marks to ring");
        List<String> members = chosen.stream()
                .map(mark -> mark.star().id()).toList();

        SheetRecorder recorder = new SheetRecorder(
                PaperSize.A4.chartWideUnits(),
                PaperSize.A4.chartHighUnits());
        Graphics2D g = (Graphics2D) recorder.create();
        SheetInk.working(chart(), members,
                        members.get(members.size() - 1),
                        ChartOptions.DEFAULTS)
                .paint(g, scene);
        g.dispose();

        assertTrue(recorder.drawn().size() >= 3,
                "a ring for each marked object the page draws: "
                        + recorder.drawn().size() + " shapes");

        // Each ring is where its object is, and is the selection's
        // own ink rather than any colour that happened to be set.
        for (ChartRenderer.DrawnMark mark : chosen) {
            boolean ringed = recorder.drawn().stream()
                    .filter(drawn -> !drawn.filled())
                    .filter(drawn -> drawn.colour().equals(
                            ChartPalette.WHITE_PAPER.selectionInk()))
                    .anyMatch(drawn -> {
                        var box = drawn.shape().getBounds2D();
                        return Math.hypot(
                                box.getCenterX() - mark.centre().x(),
                                box.getCenterY() - mark.centre().y())
                                < 0.51
                                && box.getWidth() > 2 * mark.reach();
                    });
            assertTrue(ringed, mark.star().id()
                    + " is marked, is on the page, and has its ring at "
                    + mark.centre());
        }
    }

    @Test
    void everyMarkedObjectThePageDoesNotDrawGetsItsCross()
            throws Exception {
        // The other half. A marked object the page holds but does not
        // draw - too faint for the limit, or hidden under a symbol -
        // is given a cross instead of a ring, so that a reader never
        // sees both treatments for one object and never neither. The
        // ring test alone left this uncovered (PR #292 re-review).
        ChartScene scene = Atlas.assembler().assemble(ORION,
                PaperSize.A4.chartWideUnits(),
                PaperSize.A4.chartHighUnits());
        ChartComponent chart = chart();

        // A chart carrying an interaction mark, which is what a
        // module contributes for an undrawn member.
        SkyPosition undrawn = new SkyPosition(
                scene.viewport().centre().raDegrees() + 3.0,
                scene.viewport().centre().decDegrees() + 2.0);
        chart.overlays().offer("on-this-page", () -> List.of(
                new juranometria.module.OverlayContribution.Point(
                        "TYC undrawn", "working mark on TYC undrawn,"
                                + " on this page but not drawn",
                        undrawn,
                        juranometria.module.InkRole.INTERACTION)));

        SheetRecorder recorder = new SheetRecorder(
                PaperSize.A4.chartWideUnits(),
                PaperSize.A4.chartHighUnits());
        Graphics2D g = (Graphics2D) recorder.create();
        SheetInk.working(chart, List.of(), null, ChartOptions.DEFAULTS)
                .paint(g, scene);
        g.dispose();

        var at = new juranometria.project.GnomonicProjection(
                        scene.viewport().centre()).project(undrawn)
                .map(new juranometria.project.ViewportMapping(scene.viewport(),
                juranometria.project.Projections.of(scene.viewport().projection(), scene.viewport().centre()))::toPixel).orElseThrow();
        // The cross is four ticks with a gap in the middle, so the
        // object itself stays visible through its own mark. None of
        // them is centred on it; the four together are.
        List<SheetRecorder.Drawn> ticks = recorder.drawn().stream()
                .filter(drawn -> drawn.colour().equals(
                        ChartPalette.WHITE_PAPER.interactionInk()))
                .toList();
        assertEquals(4, ticks.size(),
                "the undrawn member's cross is on the sheet, as its"
                        + " four arms: " + recorder.drawn().size()
                        + " shapes drawn in all");

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (SheetRecorder.Drawn tick : ticks) {
            var box = tick.shape().getBounds2D();
            minX = Math.min(minX, box.getMinX());
            maxX = Math.max(maxX, box.getMaxX());
            minY = Math.min(minY, box.getMinY());
            maxY = Math.max(maxY, box.getMaxY());
        }
        assertEquals(at.x(), (minX + maxX) / 2.0, 0.51,
                "centred on the object it marks");
        assertEquals(at.y(), (minY + maxY) / 2.0, 0.51,
                "in both directions");
        assertTrue(maxX - minX > 8.0 && maxY - minY > 8.0,
                "and big enough to see: " + (maxX - minX) + " x "
                        + (maxY - minY));
    }

    @Test
    void aChartWithNothingMarkedPutsNothingOnTheSheet() throws Exception {
        ChartScene scene = Atlas.assembler().assemble(ORION,
                PaperSize.A4.chartWideUnits(),
                PaperSize.A4.chartHighUnits());
        SheetRecorder recorder = new SheetRecorder(
                PaperSize.A4.chartWideUnits(),
                PaperSize.A4.chartHighUnits());
        Graphics2D g = (Graphics2D) recorder.create();
        SheetInk.working(chart(), List.of(), null, ChartOptions.DEFAULTS)
                .paint(g, scene);
        g.dispose();

        assertEquals(0, recorder.drawn().size(),
                "a reader who marked nothing gets a sheet with no"
                        + " marks on it");
        assertEquals(0, recorder.text().size(), "and no labels for them");
    }

    @Test
    void theInkIsPaperInkWhateverTheScreenWasShowing() throws Exception {
        ChartScene scene = Atlas.assembler().assemble(ORION,
                PaperSize.A4.chartWideUnits(),
                PaperSize.A4.chartHighUnits());
        String marked = new ChartRenderer(StarSizePolicy.DEFAULT)
                .drawnMarks(scene, ChartOptions.DEFAULTS).stream()
                .filter(mark -> mark.star() != null)
                .findFirst().orElseThrow().star().id();

        SheetRecorder recorder = new SheetRecorder(
                PaperSize.A4.chartWideUnits(),
                PaperSize.A4.chartHighUnits());
        Graphics2D g = (Graphics2D) recorder.create();
        // Asked with the black-sky options a reader might have had.
        SheetInk.working(chart(), List.of(marked), marked,
                        ChartOptions.DEFAULTS.withPalette(
                                ChartPalette.BLACK_SKY))
                .paint(g, scene);
        g.dispose();

        assertTrue(recorder.drawn().stream().allMatch(drawn ->
                        !drawn.colour().equals(
                                ChartPalette.BLACK_SKY.selectionInk())),
                "a sheet is paper, so the reader's marks are drawn in"
                        + " paper ink even when the screen was dark");
    }
}
