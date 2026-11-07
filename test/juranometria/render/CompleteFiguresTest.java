package juranometria.render;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Complete constellation figures under an absolute stellar limit (#571,
 * ruled D-1 to D-4, docs/decisions/complete-figures.md, superseding #307's
 * figure anchors): no star beyond the limit has a mark on any rung, a hidden
 * endpoint is nothing to click, raising the limit admits it, and the figures
 * switched on or off change only the lines.
 */
class CompleteFiguresTest {

    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));
    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);
    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);

    private static ChartScene page(double field, double limit) {
        return Atlas.assembler().assemble(new ChartViewState(ORION, field, limit), 900, 700);
    }

    private static Set<String> starsDrawn(ChartScene scene, ChartOptions options) {
        Set<String> ids = new HashSet<>();
        for (var mark : RENDERER.drawnMarks(scene, options)) {
            if (mark.star() != null) {
                ids.add(mark.star().id());
            }
        }
        return ids;
    }

    private static ChartOptions figuresOff() {
        ChartOptions o = ChartOptions.DEFAULTS;
        return new ChartOptions(o.deepSkyObjects(), o.deepSkyLabels(), false,
                o.constellationBoundaries(), o.constellationNames(), o.starNames(),
                o.bayerLetters(), o.flamsteedNumbers(), o.equatorialGrid(), o.titleBlock(),
                o.magnitudeKey(), o.galaxies(), o.openClusters(), o.globularClusters(),
                o.nebulae(), o.planetaryNebulae(), o.palette());
    }

    private static int belowLimit(ChartScene scene) {
        return FigureEndpoints.belowLimit(scene, ChartOptions.DEFAULTS,
                RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS));
    }

    @Test
    void noStarBeyondTheLimitHasAMarkOnAnyRung() {
        for (double field : new double[] {8.0, 12.0, 18.0, 24.0, 36.0, 42.0, 60.0, 90.0, 120.0, 180.0}) {
            for (double limit : new double[] {4.0, 5.0, 6.0}) {
                ChartScene scene = page(field, limit);
                for (var mark : RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)) {
                    if (mark.star() != null) {
                        assertTrue(mark.star().magnitude() <= limit,
                                mark.star().id() + " at " + field + "° and V " + limit);
                    }
                }
            }
        }
    }

    @Test
    void theFiguresChangeOnlyTheLines() {
        for (double field : new double[] {18.0, 60.0, 90.0, 120.0, 180.0}) {
            ChartScene scene = page(field, 4.0);
            assertEquals(starsDrawn(scene, figuresOff()),
                    starsDrawn(scene, ChartOptions.DEFAULTS), "the same stars at " + field + "°");
        }
    }

    @Test
    void aHiddenEndpointIsCountedAndRaisingTheLimitAdmitsIt() {
        assertTrue(belowLimit(page(120.0, 4.0)) > 0, "lines reach excluded stars at V 4");
        assertTrue(belowLimit(page(120.0, 5.0)) < belowLimit(page(120.0, 4.0)), "+ admits most");
        assertEquals(0, belowLimit(page(120.0, 7.0)), "and by V 7 every one");
        assertEquals(0, FigureEndpoints.belowLimit(page(120.0, 4.0), figuresOff(),
                RENDERER.drawnMarks(page(120.0, 4.0), figuresOff())), "no figures, no count");
        assertEquals(0, belowLimit(page(8.0, 4.0)), "a page drawing no figures has none");
    }

    @Test
    void aHiddenEndpointIsNothingToSelect() {
        ChartScene scene = page(120.0, 4.0);
        var marks = RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS);
        var page = juranometria.project.DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        int hiddenSeen = 0;
        for (GeoSegment segment : scene.geography().figureSegments()) {
            for (SkyPosition end : List.of(segment.from(), segment.to())) {
                var excluded = scene.stars().stream().filter(s ->
                        s.position().separationDegrees(end) <= FigureEndpoints.SAME_STAR_DEGREES)
                        .toList();
                var plane = page.projection().project(end);
                if (excluded.isEmpty() || plane.isEmpty()
                        || excluded.stream().anyMatch(s -> s.magnitude() <= 4.0)) {
                    continue;
                }
                PixelPoint at = mapping.toPixel(plane.get());
                hiddenSeen++;
                for (var hit : ChartHitTest.orderedHits(marks, at.x(), at.y(),
                        ChartHitTest.TOLERANCE_PX)) {
                    if (hit.star() != null) {
                        assertFalse(excluded.stream().anyMatch(
                                s -> s.id().equals(hit.star().id())),
                                "a click cannot reach the excluded star itself");
                    }
                }
            }
        }
        assertTrue(hiddenSeen > 0, "the page has hidden endpoints to try");
    }
}
