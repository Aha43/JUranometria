package juranometria.tool.overview;

import java.awt.geom.Point2D;
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

    StudyMapping(StudyProjection projection, double fieldWidthDegrees,
                 double widthPx, double heightPx) {
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

    /** The other way, for identifying what a reader points at. */
    Optional<SkyPosition> skyAt(Point2D page) {
        return projection.unproject(new PlanePoint(
                (centreX - page.getX()) / pageUnitsPerPlaneUnit,
                (centreY - page.getY()) / pageUnitsPerPlaneUnit));
    }

    StudyProjection projection() {
        return projection;
    }
}
