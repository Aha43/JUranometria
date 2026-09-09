package juranometria.tool.labels;

import java.awt.geom.Path2D;
import java.util.LinkedHashMap;
import java.util.Map;

import juranometria.project.PixelPoint;

/**
 * The part of the page a constellation owns (Sprint 31, issue #310).
 *
 * <p>A constellation name may move, but not so far that it is naming
 * a different piece of sky. What it may not leave is its own figure,
 * and "its own figure" has to be a shape rather than a rectangle: the
 * bounding box of Eridanus, which wanders half the sky, contains most
 * of Orion, and a name allowed anywhere in that box could be written
 * over the wrong constellation entirely and still pass.
 *
 * <p>So the region is the <strong>convex hull of the figure's visible
 * ink</strong>, and "visible ink" is the renderer's own definition:
 * the drawn pieces its half-degree subdivision cuts each segment into,
 * counted where the piece crosses the paper. The hull is taken over
 * those pieces' ends, because it has to contain the ink and the ink
 * runs from end to end; the <em>centroid</em> is averaged over their
 * midpoints, because that is what the renderer averages, so the anchor
 * here is the anchor there. Convex rather than the ink
 * itself, because a name has to sit <em>among</em> a figure's lines
 * rather than on one: the ink is a few hairlines and nothing but the
 * hairlines would be inside it.
 *
 * <p>Geometry is used here for speed, and checked against ink: the
 * gate test asserts that every pixel the renderer actually inks for a
 * constellation's figure falls inside that constellation's hull.
 */
public final class FigureRegion {

    private final Map<String, Path2D.Double> hulls;
    private final Map<String, double[]> centroids;

    private FigureRegion(Map<String, Path2D.Double> hulls,
                         Map<String, double[]> centroids) {
        this.hulls = hulls;
        this.centroids = centroids;
    }

    /**
     * The regions this page's constellations own, from the ink the
     * renderer publishes for each figure (issue #313).
     *
     * <p>This class used to re-walk the renderer's subdivision to find
     * that ink, and agreed with it to within a few pixels rather than
     * exactly - which is why the gate asked for the geometry to be
     * published and this asks for it now.
     */
    public static FigureRegion of(Page page) {
        Map<String, Path2D.Double> hulls = new LinkedHashMap<>();
        Map<String, double[]> centroids = new LinkedHashMap<>();
        for (var entry : Page.renderer()
                .figureInk(page.scene(), page.options()).entrySet()) {
            java.awt.Shape hull = juranometria.render.LabelGeometry.hullOf(
                    entry.getValue().ink());
            if (hull != null) {
                Path2D.Double path = new Path2D.Double();
                path.append(hull, false);
                hulls.put(entry.getKey(), path);
            }
            PixelPoint anchor = entry.getValue().nameAnchor();
            if (anchor != null) {
                centroids.put(entry.getKey(),
                        new double[] {anchor.x(), anchor.y()});
            }
        }
        return new FigureRegion(hulls, centroids);
    }

    /**
     * Whether a name written in this box would be written across the
     * figure it names.
     *
     * <p>Overlap rather than "its centre is inside", because a figure
     * can be smaller than its own name: Crater on a 90-degree page
     * leaves six pixels by five of visible ink and CRATER is fifty
     * pixels wide, so no placement of it could put its centre inside
     * its own figure and the strict rule would have refused every
     * candidate and taught nothing. What the rule is for is stopping a
     * name drifting onto somebody else's constellation, and overlap
     * says that.
     */
    public boolean ownsBox(String constellationId,
                           java.awt.geom.Rectangle2D box) {
        Path2D.Double hull = hulls.get(constellationId);
        return hull != null && hull.intersects(box);
    }

    /** Whether this constellation owns this point of the page. */
    public boolean owns(String constellationId, double x, double y) {
        Path2D.Double hull = hulls.get(constellationId);
        // A constellation with no visible figure owns nothing, and its
        // name is not drawn either - the renderer anchors names on
        // visible ink and so does this.
        return hull != null && hull.contains(x, y);
    }

    /**
     * Where a constellation's name is anchored: the centroid of its
     * visible figure ink, which is the renderer's own rule.
     *
     * <p>Over the sampled points that land on the paper, not over the
     * segments' endpoints. A constellation whose figure crosses the
     * page while both ends of every segment lie off it - Pyxis on the
     * 90-degree Orion page - has visible ink and no visible endpoint,
     * and an endpoint-based centroid does not name it at all. The
     * released page does.
     */
    public double[] centroidOf(String constellationId) {
        return centroids.get(constellationId);
    }

    /** Whether this page knows a region for this constellation. */
    public boolean knows(String constellationId) {
        return hulls.containsKey(constellationId);
    }

    /** The region itself, for a study to draw or a test to check. */
    public java.awt.Shape regionOf(String constellationId) {
        return hulls.get(constellationId);
    }

    public java.util.Set<String> constellations() {
        return java.util.Set.copyOf(hulls.keySet());
    }

}
