package juranometria.tool.overview;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.project.PlanePoint;

/**
 * Plane to page, for any candidate (issue #296).
 *
 * <p>Production's {@code ViewportMapping} works out its scale as
 * {@code width / (2 tan(field/2))}. The tangent is the gnomonic
 * projection's own radius function written into the mapping, which is
 * why the class refuses a field of 180 degrees or more: not because a
 * page cannot hold one, but because {@code tan} cannot. The general
 * rule is the same sentence with the projection's own radius in it -
 * <em>half the page holds half the field</em> - and it agrees with
 * production exactly wherever production works.
 */
final class StudyMapping {

    private final StudyProjection projection;
    private final double centreX;
    private final double centreY;
    private final double pageUnitsPerPlaneUnit;
    private final double allowedPageUnits;

    StudyMapping(StudyProjection projection, double fieldWidthDegrees,
                 double widthPx, double heightPx) {
        this(projection, fieldWidthDegrees, widthPx, heightPx,
                DEFAULT_ALLOWED_PAGE_UNITS);
    }

    StudyMapping(StudyProjection projection, double fieldWidthDegrees,
                 double widthPx, double heightPx,
                 double allowedPageUnits) {
        this.allowedPageUnits = allowedPageUnits;
        this.projection = projection;
        this.centreX = widthPx / 2.0;
        this.centreY = heightPx / 2.0;
        this.pageUnitsPerPlaneUnit = widthPx
                / (2.0 * projection.planeRadius(fieldWidthDegrees / 2.0));
    }

    double pageUnitsPerPlaneUnit() {
        return pageUnitsPerPlaneUnit;
    }

    /** East to the left and north up, as the atlas has always drawn. */
    Point2D toPage(PlanePoint plane) {
        return new Point2D.Double(
                centreX - plane.xiEast() * pageUnitsPerPlaneUnit,
                centreY - plane.etaNorth() * pageUnitsPerPlaneUnit);
    }

    Optional<Point2D> pageOf(SkyPosition position) {
        return projection.project(position).map(this::toPage);
    }

    /**
     * A curve from the projection's plane onto the page.
     *
     * <p>The mapping is a similarity - scale, half a turn, move - so
     * each of the three forms stays the form it was, and this is why
     * one vocabulary serves in both places. The half turn is the
     * atlas's own convention arriving: east to the left and north
     * up, on a page whose y runs down.
     */
    /**
     * The simplest drawable form of a projection's conic that is
     * right on this page.
     *
     * <p>This is the decision a projection cannot make, and the
     * reason it cannot is not squeamishness about layers. Whether a
     * circle is a line is a question about a page: the same great
     * circle is plainly curved across a hemisphere and plainly
     * straight across a telescope field. Only the page knows the
     * scale, and only the page knows how much of the curve is
     * showing.
     *
     * <p>So the test is not how large a radius is or how small a
     * coefficient is. It is how far the simpler curve would lie from
     * the true one <strong>over this paper</strong>, in page units -
     * a page unit being the width of the thinnest line the atlas
     * draws.
     */
    PlaneCurve onPage(PlaneConic conic) {
        PlaneConic here = conic.mapped(pageUnitsPerPlaneUnit, centreX,
                centreY);
        double reach = Math.hypot(centreX, centreY);
        double[] slope = here.gradientAt(centreX, centreY);
        double steepness = Math.hypot(slope[0], slope[1]);

        // Dropping the quadratic part moves the curve by at most the
        // quadratic part's own size, divided by how fast the conic
        // changes - which converts a value into a distance.
        if (steepness > 0.0
                && here.curvatureOver(reach) / steepness
                        < allowedPageUnits) {
            return tangentAtTheCentre(here, slope);
        }

        // A real conic on this page. Its centre, tilt and radii are
        // safe to work out now, in a way they were not before: the
        // test above is exactly the condition that the quadratic
        // part is not vanishing, so nothing here divides by a number
        // on its way to zero.
        double determinant = 4.0 * here.a() * here.c() - here.b() * here.b();
        if (determinant == 0.0) {
            return tangentAtTheCentre(here, slope);
        }
        double middleX = (here.b() * here.e() - 2.0 * here.c() * here.d())
                / determinant;
        double middleY = (here.b() * here.d() - 2.0 * here.a() * here.e())
                / determinant;
        double atMiddle = here.at(middleX, middleY);
        double tilt = 0.5 * Math.atan2(here.b(), here.a() - here.c());
        double cos = Math.cos(tilt);
        double sin = Math.sin(tilt);
        double along = here.a() * cos * cos + here.b() * cos * sin
                + here.c() * sin * sin;
        double across = here.a() * sin * sin - here.b() * sin * cos
                + here.c() * cos * cos;
        if (-atMiddle / along <= 0.0 || -atMiddle / across <= 0.0) {
            return tangentAtTheCentre(here, slope);
        }
        double radiusAlong = Math.sqrt(-atMiddle / along);
        double radiusAcross = Math.sqrt(-atMiddle / across);

        // An ellipse thinner than the page can see is the line it
        // has collapsed towards. Left as an ellipse it is a shape
        // with no interior, and the affine frame that clips it
        // cannot be inverted.
        if (Math.min(radiusAlong, radiusAcross) < allowedPageUnits) {
            double majorTilt = radiusAlong > radiusAcross
                    ? tilt : tilt + Math.PI / 2.0;
            double normalX = -Math.sin(majorTilt);
            double normalY = Math.cos(majorTilt);
            return PlaneCurve.Straight.of(
                    -(normalX * middleX + normalY * middleY),
                    normalX, normalY);
        }

        // Two radii the page cannot tell apart are a circle, which
        // is the simpler of two true names for the same curve.
        if (Math.abs(radiusAlong - radiusAcross) < allowedPageUnits) {
            return new PlaneCurve.Circular(middleX, middleY,
                    (radiusAlong + radiusAcross) / 2.0);
        }
        return new PlaneCurve.Elliptical(middleX, middleY, radiusAlong,
                radiusAcross, tilt);
    }

    /**
     * How far a substituted curve may lie from the true one, in page
     * units.
     *
     * <p>Not a free choice, and measured rather than asserted.
     * Substituting too eagerly leaves a visible gap; substituting
     * too late keeps a conic whose centre and radius are so large
     * that working them out loses every digit that matters.
     * {@link SubstitutionReport} measures both ends and this is the
     * value that sweep settled on.
     *
     * <p>It is a mapping's own, and the report varies it by building
     * mappings rather than by assigning to anything: a study that
     * reached across and changed a shared setting for the duration
     * of a measurement would be a study that could be read wrongly
     * by whatever else happened to be running.
     */
    static final double DEFAULT_ALLOWED_PAGE_UNITS = 1.0e-3;

    /** The conic's own tangent where the page is. */
    private PlaneCurve tangentAtTheCentre(PlaneConic here, double[] slope) {
        return PlaneCurve.Straight.of(
                here.at(centreX, centreY) - slope[0] * centreX
                        - slope[1] * centreY,
                slope[0], slope[1]);
    }

    /**
     * Where this page may carry ink: the paper, and the limb if the
     * projection has one.
     */
    PageRegion region() {
        Rectangle2D paper = new Rectangle2D.Double(0, 0,
                2.0 * centreX, 2.0 * centreY);
        double visible = projection.visiblePlaneRadius();
        return Double.isFinite(visible)
                ? PageRegion.of(paper, centreX, centreY,
                        visible * pageUnitsPerPlaneUnit)
                : PageRegion.of(paper);
    }

    /** The other way, for identifying what a reader points at. */    /** The other way, for identifying what a reader points at. */
    Optional<SkyPosition> skyAt(Point2D page) {
        return projection.unproject(new PlanePoint(
                (centreX - page.getX()) / pageUnitsPerPlaneUnit,
                (centreY - page.getY()) / pageUnitsPerPlaneUnit));
    }

    double pageCentreX() {
        return centreX;
    }

    double pageCentreY() {
        return centreY;
    }

    StudyProjection projection() {
        return projection;
    }
}
