package juranometria.project;

/**
 * Where a page may carry ink (Sprint 30, issue #298).
 *
 * <p>For a chart this is the paper and nothing else, and every atlas
 * page until now has been exactly that. For a projection that shows
 * a hemisphere it is the paper <strong>and the limb</strong>: beyond
 * that circle there is no sky to draw - not empty sky, no sky.
 *
 * <p>The gate found this by looking at a page rather than by
 * reasoning about one. Marks stopped at the limb by themselves,
 * because a mark is a projected point and a point off the hemisphere
 * has no projection; curves did not, because a curve is drawn from
 * its own equation and the only thing clipping it was the rectangle
 * (docs/decisions/overview-projection.md).
 */
public record PageRegion(double minX, double minY, double maxX,
                         double maxY, double limbX, double limbY,
                         double limbRadius) {

    public PageRegion {
        // Every edge a real pixel. Width and height alone let an
        // infinite edge through - it is greater than its opposite, so
        // the rectangle looks well formed - and the clipping then
        // divides by it and hands back a run whose ends are infinity
        // or not-a-number: a line no renderer can draw and no test of
        // the run would recognise as wrong. The type this replaced
        // was held to that, and dropping the check while replacing it
        // would have been a quiet loss.
        if (!Double.isFinite(minX) || !Double.isFinite(minY)
                || !Double.isFinite(maxX) || !Double.isFinite(maxY)) {
            throw new IllegalArgumentException(
                    "a page's edges are pixels: " + minX + "," + minY
                            + " to " + maxX + "," + maxY);
        }
        if (!(maxX > minX) || !(maxY > minY)) {
            throw new IllegalArgumentException(
                    "a page needs a width and a height: " + minX + ","
                            + minY + " to " + maxX + "," + maxY);
        }
        if (Double.isNaN(limbX) || Double.isNaN(limbY)
                || Double.isNaN(limbRadius) || limbRadius <= 0.0) {
            throw new IllegalArgumentException(
                    "a limb is a circle with a radius: " + limbX + ","
                            + limbY + " r " + limbRadius);
        }
    }

    /** A page with no edge to the sky: every chart projection. */
    public static PageRegion paper(double minX, double minY,
                                   double maxX, double maxY) {
        return new PageRegion(minX, minY, maxX, maxY, 0.0, 0.0,
                Double.POSITIVE_INFINITY);
    }

    /** A page showing a hemisphere, and where that hemisphere ends. */
    public static PageRegion within(double minX, double minY,
                                    double maxX, double maxY,
                                    double limbX, double limbY,
                                    double limbRadius) {
        return new PageRegion(minX, minY, maxX, maxY, limbX, limbY,
                limbRadius);
    }

    /** Whether the sky itself stops somewhere on this page. */
    public boolean bounded() {
        return Double.isFinite(limbRadius);
    }

    public boolean contains(double x, double y) {
        if (x < minX || x > maxX || y < minY || y > maxY) {
            return false;
        }
        return !bounded()
                || Math.hypot(x - limbX, y - limbY) <= limbRadius;
    }

    /** Half the diagonal: how far this page reaches from its middle. */
    public double reach() {
        return 0.5 * Math.hypot(maxX - minX, maxY - minY);
    }

    public double centreX() {
        return (minX + maxX) / 2.0;
    }

    public double centreY() {
        return (minY + maxY) / 2.0;
    }
}
