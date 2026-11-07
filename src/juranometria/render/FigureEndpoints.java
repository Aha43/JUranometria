package juranometria.render;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.geo.GeoSegment;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

/**
 * Which figure endpoints a page leaves in open sky (#571, ruled D-1 to D-4,
 * docs/decisions/complete-figures.md): complete figure lines under an
 * absolute stellar limit may end at the true position of a star the limit
 * excludes. Such an endpoint has no mark, identity or selection target. This
 * only counts them, for the view report; the chart and speech never do.
 *
 * <p>The figures are stored as coordinates and carry no catalogue identity,
 * so a star is matched to an endpoint by position, at the tolerance #307
 * measured.
 */
public final class FigureEndpoints {

    /**
     * How near a figure's endpoint a star must be to <em>be</em> that
     * endpoint, in degrees: half of the bundled endpoints lie within
     * 0.00005°, nine tenths within 0.0007°, and the next nearest star is
     * 0.89° away (#307, docs/studies/figure-anchors/measurements.md).
     */
    public static final double SAME_STAR_DEGREES = 0.01;

    private FigureEndpoints() {
    }

    /**
     * How many figure endpoints on this page's paper have a star the limit
     * excludes and no drawn star. Zero when the page draws no figures.
     */
    public static int belowLimit(ChartScene scene, ChartOptions options,
                                 List<ChartRenderer.DrawnMark> marks) {
        if (scene == null || options == null || marks == null
                || !options.constellationFigures()
                || !new GeographyDetailPolicy(
                        scene.viewport().fieldWidthDegrees()).figuresDrawn()) {
            return 0;
        }
        Set<String> drawn = new HashSet<>();
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.star() != null) {
                drawn.add(mark.star().id());
            }
        }
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        Set<String> seen = new HashSet<>();
        int hidden = 0;
        for (GeoSegment segment : scene.geography().figureSegments()) {
            for (SkyPosition end : List.of(segment.from(), segment.to())) {
                var plane = page.projection().project(end);
                if (plane.isEmpty()) {
                    continue;
                }
                PixelPoint at = mapping.toPixel(plane.get());
                // The area the chart draws in; the border is under its frame.
                if (at.x() < 1.0 || at.y() < 1.0
                        || at.x() > scene.viewport().widthPx() - 2.0
                        || at.y() > scene.viewport().heightPx() - 2.0
                        || !seen.add(end.raDegrees() + "," + end.decDegrees())) {
                    continue;
                }
                boolean node = false;
                boolean excluded = false;
                for (Star star : scene.stars()) {
                    if (star.position().separationDegrees(end) <= SAME_STAR_DEGREES) {
                        node |= drawn.contains(star.id());
                        excluded |= star.magnitude() > scene.limitingMagnitude();
                    }
                }
                if (excluded && !node) {
                    hidden++;
                }
            }
        }
        return hidden;
    }
}
