package juranometria.tool.overview;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Where a page may carry ink (issue #296).
 *
 * <p>For a chart this is the paper and nothing else, and every atlas
 * page until now has been exactly that. For a globe it is the paper
 * <strong>and the limb</strong>: an orthographic projection shows one
 * hemisphere, its edge is a circle, and beyond that circle there is
 * no sky to draw - not empty sky, no sky.
 *
 * <p>A review found this by looking at a committed page. The stars
 * stopped at the limb, because a star is a projected point and a
 * point beyond the hemisphere has no projection; but the celestial
 * equator ran straight on across the corners of the paper, because a
 * curve is drawn from its own equation and the only thing it was
 * being clipped to was the rectangle. The ink left the globe.
 *
 * <p>So the visible region is part of the seam rather than something
 * issue #301 would have to add later - which is what the milestone
 * meant by studying the globe from the beginning so that the seam
 * could not accidentally fit stereographic alone.
 */
record PageRegion(Rectangle2D paper, double limbX, double limbY,
                  double limbRadius) {

    /** A page with no edge to the sky: every chart projection. */
    static PageRegion of(Rectangle2D paper) {
        return new PageRegion(paper, 0.0, 0.0,
                Double.POSITIVE_INFINITY);
    }

    /** A page showing a hemisphere, and where that hemisphere ends. */
    static PageRegion of(Rectangle2D paper, double limbX, double limbY,
                         double limbRadius) {
        return new PageRegion(paper, limbX, limbY, limbRadius);
    }

    boolean bounded() {
        return Double.isFinite(limbRadius);
    }

    boolean contains(Point2D point) {
        if (!paper.contains(point)) {
            return false;
        }
        return !bounded() || Math.hypot(point.getX() - limbX,
                point.getY() - limbY) <= limbRadius;
    }
}
