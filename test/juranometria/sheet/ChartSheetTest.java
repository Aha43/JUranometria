package juranometria.sheet;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.chart.StarSizePolicy;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart-sheet boundary (Sprint 29, issue #285).
 *
 * <p>The whole point of the boundary is that it exports the atlas's
 * own chart rather than something that resembles it. That is not a
 * claim a comment can carry, so every assertion here compares the
 * sheet against production asked directly - the same assembler, the
 * same renderer, the same module contributions - rather than against
 * a number this test decided was right.
 */
class ChartSheetTest {

    static final ChartViewState ORION = new ChartViewState(
            new SkyPosition(83.0, 0.0), 42.0, 6.0);

    /**
     * Whether a shape is the landmark's own open diamond about a
     * point: four vertices, each on an axis through the centre,
     * equidistant from it, and the path closed.
     *
     * <p>Counting four corners is not enough - a box has four
     * corners too, and an unclosed path has four points and no
     * fourth side (PR #290 round 2). What distinguishes a diamond is
     * where the vertices sit: on the axes, not at the corners of the
     * box that bounds them.
     */
    private static boolean isDiamondAbout(java.awt.Shape shape,
                                          double x, double y) {
        List<double[]> vertices = new java.util.ArrayList<>();
        boolean closed = false;
        double[] segment = new double[6];
        for (var each = shape.getPathIterator(null); !each.isDone();
                each.next()) {
            int kind = each.currentSegment(segment);
            if (kind == java.awt.geom.PathIterator.SEG_CLOSE) {
                closed = true;
            } else if (kind == java.awt.geom.PathIterator.SEG_MOVETO
                    || kind == java.awt.geom.PathIterator.SEG_LINETO) {
                vertices.add(new double[] {segment[0], segment[1]});
            } else {
                return false;  // a curve is not this diamond
            }
        }
        if (!closed || vertices.size() != 4) {
            return false;
        }

        double reach = -1.0;
        boolean up = false;
        boolean down = false;
        boolean left = false;
        boolean right = false;
        for (double[] vertex : vertices) {
            double dx = vertex[0] - x;
            double dy = vertex[1] - y;
            // Exactly one offset is zero: the vertex is on an axis
            // through the centre, which a box's corner never is.
            boolean onAxis = Math.abs(dx) < 0.01 ^ Math.abs(dy) < 0.01;
            if (!onAxis) {
                return false;
            }
            double distance = Math.hypot(dx, dy);
            if (reach < 0) {
                reach = distance;
            } else if (Math.abs(distance - reach) > 0.01) {
                return false;  // not equidistant, so not this diamond
            }
            up |= dy < -0.01;
            down |= dy > 0.01;
            left |= dx < -0.01;
            right |= dx > 0.01;
        }
        return up && down && left && right && Math.abs(reach - 6.0) < 0.5;
    }

    /** A sheet with nothing switched on, on A4. */
    static SheetRecording bare(ChartViewState state) {
        return ChartSheet.record(Atlas.assembler()::assemble, state,
                ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                PaperSize.A4);
    }

    /** The meridian and the ecliptic, as the screen carries them. */
    static ChartRenderer.ReferenceLayer modules() {
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(new Observer(59.9, 10.7,
                java.time.Instant.parse("2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);
        return (g, painted) -> ReferenceInk.paint(g, painted,
                registry.collect(), ChartPalette.WHITE_PAPER);
    }

    @Test
    void theSheetIsTheAtlassOwnPageAssembledForThePaper() {
        SheetRecording sheet = bare(ORION);

        // Not a screenshot, and not a second set of astronomy: the
        // scene on the sheet is the scene the production assembler
        // gives for this state, and the only thing asked differently
        // is the extent - which is the paper's, in points.
        assertEquals(PaperSize.A4.chartWideUnits(),
                sheet.scene().viewport().widthPx(),
                "the chart is assembled for the paper's own rectangle");
        assertEquals(PaperSize.A4.chartHighUnits(),
                sheet.scene().viewport().heightPx(), "in both directions");
        assertEquals(ORION.centre(), sheet.scene().viewport().centre(),
                "at the chart's own centre, untouched");
        assertEquals(42.0, sheet.scene().viewport().fieldWidthDegrees(),
                "and its own field");

        var onPaper = Atlas.assembler().assemble(ORION,
                PaperSize.A4.chartWideUnits(), PaperSize.A4.chartHighUnits());
        assertEquals(onPaper.stars().size(), sheet.scene().stars().size(),
                "with the catalogue composition the assembler decided,"
                        + " star for star");
        assertEquals(onPaper.deepSkyObjects().size(),
                sheet.scene().deepSkyObjects().size(),
                "and deep-sky object for deep-sky object");

        assertTrue(sheet.shapeCount() > 1000,
                "and the renderer drew it: " + sheet.shapeCount()
                        + " shapes");
        assertTrue(sheet.textCount() > 5,
                "labels included: " + sheet.textCount() + " runs");
    }

    @Test
    void everyMarkTheRendererDecidedOnIsOnTheSheet() {
        // Layer presence by production identity rather than by
        // counting ink: production is asked what marks this page has,
        // and each one has to be found in the recording at its own
        // place.
        SheetRecording sheet = bare(ORION);
        List<ChartRenderer.DrawnMark> marks =
                new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(sheet.scene(), sheet.options());
        assertTrue(marks.size() > 100,
                "the page has marks to look for: " + marks.size());

        List<String> missing = new java.util.ArrayList<>();
        for (ChartRenderer.DrawnMark mark : marks) {
            boolean found = sheet.recorder().drawn().stream()
                    .anyMatch(drawn -> {
                        var box = drawn.shape().getBounds2D();
                        return Math.hypot(
                                box.getCenterX() - mark.centre().x(),
                                box.getCenterY() - mark.centre().y()) < 0.51;
                    });
            if (!found) {
                missing.add(mark.kind() + " at " + mark.centre());
            }
        }
        assertEquals(List.of(), missing.stream().limit(5).toList(),
                missing.size() + " of " + marks.size() + " marks the"
                        + " renderer decided on are absent from the"
                        + " sheet");
    }

    @Test
    void moduleInkTravelsToPaperAndTheSheetNeverGoesLookingForIt() {
        // The March equinox page. Orion is where the rest of this
        // class looks, but no ecliptic landmark reaches it - the
        // solstices sit 23 degrees off a page half that tall - and a
        // check for ink that was never owed passes for free (the
        // lesson of PR #278).
        ChartViewState equinox = new ChartViewState(
                new SkyPosition(0.0, 0.0), 42.0, 6.0);
        SheetRecording without = bare(equinox);
        SheetRecording with = ChartSheet.record(
                Atlas.assembler()::assemble, equinox, ChartOptions.DEFAULTS,
                modules(), PaperSize.A4);

        assertTrue(with.shapeCount() > without.shapeCount(),
                "a chart carrying the meridian and the ecliptic puts"
                        + " more on paper than one carrying neither: "
                        + with.shapeCount() + " against "
                        + without.shapeCount());

        // And it is that ink, identified by what the module itself
        // contributed rather than by counting shapes: the ecliptic's
        // four landmarks, projected the way production projects them,
        // each have a mark on the paper.
        var projection = new juranometria.project.GnomonicProjection(
                with.scene().viewport().centre());
        var mapping = new juranometria.project.ViewportMapping(
                with.scene().viewport());

        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        List<String> absent = new java.util.ArrayList<>();
        int onPaper = 0;
        for (var contribution : ecliptic.contributedGeometry()) {
            if (!(contribution
                    instanceof juranometria.module.OverlayContribution.Point
                            landmark)) {
                continue;
            }
            var at = projection.project(landmark.at())
                    .map(mapping::toPixel);
            if (at.isEmpty()
                    || at.get().x() < 0 || at.get().y() < 0
                    || at.get().x() > with.scene().viewport().widthPx()
                    || at.get().y() > with.scene().viewport().heightPx()) {
                continue;  // not on this page, so nothing is owed
            }
            onPaper++;
            // The ecliptic's own line passes exactly through its
            // landmarks, so "some shape centred here" would be
            // satisfied by a segment of the line and the diamond
            // could go missing unnoticed (PR #290 review). What is
            // owed is the open diamond: a closed four-sided path
            // about twelve units across, centred on the landmark.
            boolean found = with.recorder().drawn().stream()
                    .filter(drawn -> !drawn.filled())
                    .anyMatch(drawn -> isDiamondAbout(drawn.shape(),
                            at.get().x(), at.get().y()));
            if (!found) {
                absent.add(landmark.accessibleName() + " at " + at.get());
            }
        }
        assertTrue(onPaper > 0,
                "the ecliptic reaches this page, so there is landmark"
                        + " ink to look for: " + onPaper);
        assertEquals(List.of(), absent,
                "and every landmark the module contributed is on the"
                        + " paper as its own open diamond, not merely"
                        + " somewhere the ecliptic's line happens to"
                        + " pass");

        // The sheet is handed the ink; it never goes looking. Given
        // no reference layer it draws the chart an atlas with every
        // module switched off draws, and the whole difference between
        // the two sheets is what the modules put on one of them.
        assertTrue(without.recorder().drawn().size()
                        < with.recorder().drawn().size(),
                "a sheet given no reference layer carries no module"
                        + " ink at all");
    }

    @Test
    void aSheetWillNotGuessWhetherAChartHasModulesOnIt() {
        // NONE is a chart with nothing switched on. Null is a caller
        // that has not decided, and a sheet that quietly read it as
        // NONE would turn a miswired export - one that meant to
        // carry the ecliptic and lost it - into a sheet that looks
        // entirely correct (PR #290 review).
        assertThrows(IllegalArgumentException.class,
                () -> ChartSheet.record(Atlas.assembler()::assemble,
                        ORION, ChartOptions.DEFAULTS, null,
                        PaperSize.A4),
                "an undecided reference layer is refused rather than"
                        + " defaulted");
    }

    @Test
    void aSheetIsWhitePaperEvenWhenTheScreenIsNot() {
        // The gate's rule, enforced here rather than trusted to
        // whoever calls: printing the black sky asks a reader to lay
        // down a sheet of toner and read white marks out of it.
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, ORION,
                ChartOptions.DEFAULTS.withPalette(ChartPalette.BLACK_SKY),
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);

        assertEquals(ChartPalette.WHITE_PAPER, sheet.options().palette(),
                "a sheet made from a black-sky chart is still paper");
        assertTrue(sheet.metadata().description().contains("white-paper"),
                "and says so on the sheet itself");

        // The ground is the ground, not any black mark: on white
        // paper the stars are black discs, so the thing to look at is
        // the fill that covers the page.
        var ground = sheet.recorder().drawn().stream()
                .filter(drawn -> drawn.filled())
                .filter(drawn -> drawn.shape().getBounds2D().getWidth()
                        >= sheet.paper().chartWideUnits() - 1)
                .findFirst().orElseThrow(() -> new AssertionError(
                        "the sheet lays down a ground to draw on"));
        assertEquals(ChartPalette.WHITE_PAPER.ground(), ground.colour(),
                "which is white paper, whatever the screen was set to");
    }

    @Test
    void aSheetRefusesWhatPaperCannotHold() {
        // The recorder is the boundary's guard, and it is production
        // now rather than a study prop: if the cartography ever
        // reaches past what a page can hold, the export fails loudly
        // instead of quietly rasterising.
        SheetRecorder recorder = new SheetRecorder(100, 100);
        assertThrows(UnsupportedOperationException.class,
                () -> recorder.drawImage(new java.awt.image.BufferedImage(
                        1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB),
                        0, 0, null),
                "an image on a vector sheet is refused, not accepted"
                        + " quietly");
    }

    @Test
    void bothPapersAreTheirOwnRectangleInMillimetres() {
        // The gate's own table, in the units paper is sold in.
        assertEquals(297.0, PaperSize.A4.wideMm(), 0.05, "A4 is 297 mm");
        assertEquals(210.0, PaperSize.A4.highMm(), 0.05, "by 210");
        assertEquals(271.6, PaperSize.A4.chartWideMm(), 0.05,
                "and its chart rectangle is the gate's 271.6 mm");
        assertEquals(184.6, PaperSize.A4.chartHighMm(), 0.05, "by 184.6");

        assertEquals(279.4, PaperSize.LETTER.wideMm(), 0.05,
                "US Letter is 11 inches");
        assertEquals(215.9, PaperSize.LETTER.highMm(), 0.05, "by 8.5");
        assertEquals(12.7, PaperSize.A4.marginMm(), 0.01,
                "half an inch of margin on both");
        assertEquals(12.7, PaperSize.LETTER.marginMm(), 0.01, "the same");

        // And they are genuinely different sheets, laid out
        // separately - not one cut down to the other.
        SheetRecording a4 = bare(ORION);
        SheetRecording letter = ChartSheet.record(
                Atlas.assembler()::assemble, ORION, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.LETTER);
        assertEquals(PaperSize.LETTER.chartWideUnits(),
                letter.scene().viewport().widthPx(),
                "Letter's chart is assembled at Letter's own width");
        assertTrue(a4.scene().viewport().widthPx()
                        > letter.scene().viewport().widthPx(),
                "which is narrower than A4's");
        assertTrue(a4.scene().viewport().heightPx()
                        < letter.scene().viewport().heightPx(),
                "and taller");
    }
}
