package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Which way celestial north and east lie on a drawn page at one sky
 * position (Sprint 37, issue #416, the rotation contract of the #414
 * ruling, C3).
 *
 * <p>A position angle - the Moon's bright limb χ, measured from
 * celestial north through east - means nothing on a page until it is
 * turned into the page's own directions there. North is page-up only
 * at the centre of an equatorial page; elsewhere it turns, and on a
 * tangent page away from its centre the projected north and east are
 * not even perpendicular (88.3° at a 36° page's corner, measured in
 * {@code docs/studies/solar-cartography/}). So both vectors are kept:
 * {@code north} is the page direction of increasing declination and
 * {@code east} of increasing right ascension, each a unit vector in
 * pixels (y down), found by projecting the position and a point one
 * arcsecond towards each.
 *
 * <p>The contract is a round trip. {@link #direction} turns a position
 * angle into a page direction, cos χ · n̂ + sin χ · ê; {@link
 * #positionAngleDegrees} takes any page direction back to the angle,
 * by the exact inverse of that combination - the direction's
 * components in the (n̂, ê) basis - so the angle comes back whole
 * even where the basis is skewed. An angle is never recomputed from
 * J2000 positions.
 *
 * <p>Numbers only: this package says where ink goes, never draws it.
 */
public record PageBasis(PixelPoint at, double northX, double northY,
                        double eastX, double eastY) {

    /** One arcsecond, in degrees: the step the basis is measured over. */
    private static final double STEP = 1.0 / 3600.0;

    public PageBasis {
        if (at == null) {
            throw new IllegalArgumentException("a basis is somewhere on the page");
        }
        double det = northX * eastY - northY * eastX;
        if (!Double.isFinite(det) || Math.abs(det) < 1e-9) {
            throw new IllegalArgumentException(
                    "north and east are independent directions on the page");
        }
    }

    /**
     * The basis at a sky position, or empty where this page's
     * projection cannot place the position or a step from it.
     */
    public static Optional<PageBasis> at(DrawnPage page, SkyPosition position) {
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        Optional<PixelPoint> centre = projection.project(position)
                .map(mapping::toPixel);
        // North: a step towards increasing declination, or - at the pole
        // itself, where there is none - the reverse of a step away.
        double dec = position.decDegrees();
        boolean pole = dec + STEP > 90.0;
        Optional<PixelPoint> north = projection.project(new SkyPosition(
                        position.raDegrees(), pole ? dec - STEP : dec + STEP))
                .map(mapping::toPixel);
        double cosDec = Math.cos(Math.toRadians(dec));
        if (centre.isEmpty() || north.isEmpty() || cosDec < 1e-9) {
            return Optional.empty();
        }
        double ra = (position.raDegrees() + STEP / cosDec) % 360.0;
        Optional<PixelPoint> east = projection.project(new SkyPosition(
                        ra < 0.0 ? ra + 360.0 : ra, dec))
                .map(mapping::toPixel);
        if (east.isEmpty()) {
            return Optional.empty();
        }
        PixelPoint c = centre.get();
        double nx = north.get().x() - c.x();
        double ny = north.get().y() - c.y();
        if (pole) {
            nx = -nx;
            ny = -ny;
        }
        double ex = east.get().x() - c.x();
        double ey = east.get().y() - c.y();
        double nl = Math.hypot(nx, ny);
        double el = Math.hypot(ex, ey);
        if (!(nl > 0.0) || !(el > 0.0)) {
            return Optional.empty();
        }
        return Optional.of(new PageBasis(c, nx / nl, ny / nl, ex / el, ey / el));
    }

    /**
     * The page direction of a position angle: cos χ · n̂ + sin χ · ê,
     * scaled to unit length, as {x, y} in pixels (y down).
     */
    public double[] direction(double positionAngleDegrees) {
        double chi = Math.toRadians(positionAngleDegrees);
        double x = Math.cos(chi) * northX + Math.sin(chi) * eastX;
        double y = Math.cos(chi) * northY + Math.sin(chi) * eastY;
        double length = Math.hypot(x, y);
        return new double[] {x / length, y / length};
    }

    /**
     * The position angle a page direction states, in [0°, 360°): the
     * direction's components (a, b) with d = a · n̂ + b · ê, and
     * atan2(b, a) - the exact inverse of {@link #direction}.
     */
    public double positionAngleDegrees(double dx, double dy) {
        double det = northX * eastY - northY * eastX;
        double a = (dx * eastY - dy * eastX) / det;
        double b = (northX * dy - northY * dx) / det;
        double angle = Math.toDegrees(Math.atan2(b, a));
        return angle < 0.0 ? angle + 360.0 : angle;
    }

    /** The angle between projected north and east, in degrees. */
    public double skewDegrees() {
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0,
                northX * eastX + northY * eastY))));
    }
}
