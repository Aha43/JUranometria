package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.sky.Ecliptic;

/**
 * The Hammer projection: the whole sky at one chart time (#687, ruled
 * on #686; docs/decisions/all-sky-projection.md).
 *
 * <p>The one projection in the atlas that is not azimuthal, and the one
 * page that holds every Solar System body at once: the best-placed
 * overview holds five or six of the nine, the best globe hemisphere
 * seven, this nine of nine every time (the study's M§6). Equal-area,
 * with no angular deformation at the centre and the least of the
 * whole-sky forms along the middle line (R-2).
 *
 * <p><strong>Ecliptic horizontal</strong> (R-3): the map's middle line
 * is the ecliptic, so the planets lie along one straight line, and
 * "up" is ecliptic north. The frame is {@link Ecliptic}'s - the same
 * J2000 obliquity the drawn ecliptic and its landmarks use - so the
 * map's middle line and the ecliptic drawn on it cannot disagree. This
 * is the one projection that knows the ecliptic; the three azimuthal
 * ones are frame-free.
 *
 * <p><strong>Observer-aligned</strong> (R-4): the central meridian is
 * the ecliptic longitude of the observer's zenith. That puts the zenith
 * on the central meridian, not at the map's centre - its ecliptic
 * latitude is kept, so from a northern observer it stands well above
 * the middle line. The centre handed in here is a position on the
 * ecliptic; its longitude is the central meridian's.
 *
 * <p><strong>The seam</strong> (R-5) is the half-meridian opposite the
 * central one. A position on it belongs to both edges and is drawn on
 * the east edge; {@link #mayJoin} is how a joining pass learns that two
 * neighbours straddle it. Nothing is hidden to get a break.
 *
 * <p>Formulas: Hammer-Aitoff as transcribed from Wolfram MathWorld
 * ("Hammer-Aitoff Equal-Area Projection", eqs (1)-(5), read
 * 2026-11-08), which attributes them to J. P. Snyder, <em>Map
 * Projections - A Working Manual</em>, USGS Professional Paper 1395
 * (1987), pp. 182 and 185; held by {@code HammerProjectionTest}
 * (round trip, equal area by the Jacobian, the outline) rather than by
 * the citation alone, because the USGS copy refused an automated fetch
 * during the study. On the unit sphere, with {@code lambda} the
 * longitude from the central meridian and {@code phi} the latitude:
 * {@code d = sqrt(1 + cos phi cos(lambda/2))},
 * {@code x = 2 sqrt 2 cos phi sin(lambda/2) / d},
 * {@code y = sqrt 2 sin phi / d}; the outline is
 * {@code x^2/8 + y^2/2 = 1}.
 */
public final class HammerProjection implements Projection {

    /** What the title block, exports and the view report call it. */
    public static final String NAME = "hammer";

    /** The orientation token the whole-sky statement carries. */
    public static final String ORIENTATION = "ecliptic";

    private static final double SQRT2 = Math.sqrt(2.0);

    /** Half the outline's width on the plane: {@code 2 sqrt 2}. */
    public static final double HALF_WIDTH = 2.0 * SQRT2;

    /** Half the outline's height on the plane: {@code sqrt 2}. */
    public static final double HALF_HEIGHT = SQRT2;

    private final double centralLongitudeDegrees;
    private final SkyPosition centre;

    /**
     * The whole sky about this centre, which lies on the ecliptic:
     * the central meridian is its ecliptic longitude.
     */
    public HammerProjection(SkyPosition centre) {
        if (centre == null) {
            throw new IllegalArgumentException("centre must not be null");
        }
        this.centralLongitudeDegrees = normalised(
                Ecliptic.toEcliptic(centre).longitudeDegrees());
        this.centre = centreFor(centralLongitudeDegrees);
    }

    /**
     * The position on the ecliptic at this longitude: the centre a
     * whole-sky state is built about.
     */
    public static SkyPosition centreFor(double eclipticLongitudeDegrees) {
        return Ecliptic.toEquatorial(normalised(eclipticLongitudeDegrees),
                0.0);
    }

    /** The ecliptic longitude on the central meridian, in [0, 360). */
    public double centralLongitudeDegrees() {
        return centralLongitudeDegrees;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public SkyPosition centre() {
        return centre;
    }

    /** Longitude east of the central meridian, in (-180, 180]. */
    double offsetDegrees(SkyPosition position) {
        double lon = Ecliptic.toEcliptic(position).longitudeDegrees();
        double d = normalised(lon - centralLongitudeDegrees);
        return d > 180.0 ? d - 360.0 : d;
    }

    /**
     * Every position has a point: the whole sphere is on the plane.
     * A position exactly on the seam lands on the east edge.
     */
    @Override
    public Optional<PlanePoint> project(SkyPosition position) {
        Ecliptic.Coordinates c = Ecliptic.toEcliptic(position);
        double d = normalised(c.longitudeDegrees() - centralLongitudeDegrees);
        double lambda = Math.toRadians(d > 180.0 ? d - 360.0 : d);
        double phi = Math.toRadians(c.latitudeDegrees());
        double cosPhi = Math.cos(phi);
        double root = Math.sqrt(1.0 + cosPhi * Math.cos(lambda / 2.0));
        return Optional.of(new PlanePoint(
                2.0 * SQRT2 * cosPhi * Math.sin(lambda / 2.0) / root,
                SQRT2 * Math.sin(phi) / root));
    }

    /** The sky under a plane point, or empty outside the outline. */
    @Override
    public Optional<SkyPosition> unproject(PlanePoint point) {
        double x = point.xiEast();
        double y = point.etaNorth();
        if (!outline().contains(x, y)) {
            return Optional.empty();
        }
        // MathWorld eqs (3)-(5); the arctangent takes both signs.
        double z = Math.sqrt(Math.max(0.0,
                1.0 - (x / 4.0) * (x / 4.0) - (y / 2.0) * (y / 2.0)));
        double lambda = 2.0 * Math.atan2(z * x, 2.0 * (2.0 * z * z - 1.0));
        double phi = Math.asin(Math.max(-1.0, Math.min(1.0, y * z)));
        return Optional.of(Ecliptic.toEquatorial(
                normalised(centralLongitudeDegrees + Math.toDegrees(lambda)),
                Math.toDegrees(phi)));
    }

    /**
     * Two positions may be joined unless the short way between them
     * crosses the seam: their offsets from the central meridian then
     * lie on opposite sides of 180 degrees.
     */
    @Override
    public boolean mayJoin(SkyPosition from, SkyPosition to) {
        return Math.abs(offsetDegrees(from) - offsetDegrees(to)) <= 180.0;
    }

    /**
     * How far out along the middle line an angle lands: the map is not
     * radially symmetric, and this is the one radial law it can state
     * honestly - the ecliptic's own, where the planets are. With
     * {@code phi = 0}: {@code x = 2 sqrt 2 sin(a/2) / sqrt(1 + cos(a/2))},
     * which is {@code 2 sqrt 2} at 180 degrees, the half-width.
     */
    @Override
    public double planeRadius(double angleDegrees) {
        double a = Math.abs(angleDegrees);
        if (a > 180.0) {
            return Double.NaN;
        }
        double half = Math.toRadians(a) / 2.0;
        return 2.0 * SQRT2 * Math.sin(half) / Math.sqrt(1.0 + Math.cos(half));
    }

    /**
     * The inverse of {@link #planeRadius} along the middle line:
     * {@code x^2 = 8 (1 - cos(a/2))}, so {@code a = 2 acos(1 - x^2/8)};
     * NaN beyond the half-width.
     */
    @Override
    public double angleAtPlaneRadius(double planeRadius) {
        if (planeRadius > HALF_WIDTH) {
            return Double.NaN;
        }
        return Math.toDegrees(2.0 * Math.acos(
                Math.max(-1.0, 1.0 - planeRadius * planeRadius / 8.0)));
    }

    /** The whole sphere, attained. */
    @Override
    public double limitDegrees() {
        return 180.0;
    }

    /** No corner to cap: the page corners are paper, not sky. */
    @Override
    public double usefulCornerDegrees() {
        return 180.0;
    }

    /** How far ink reaches: the half-width. The shape is {@link #outline}. */
    @Override
    public double visiblePlaneRadius() {
        return HALF_WIDTH;
    }

    @Override
    public PlaneOutline outline() {
        return new PlaneOutline.Ellipse(HALF_WIDTH, HALF_HEIGHT);
    }

    /** No conic: the sampled route draws every great circle here. */
    @Override
    public boolean greatCirclesAreConics() {
        return false;
    }

    /**
     * Never asked, because {@link #greatCirclesAreConics} says no; and
     * empty if asked, because no conic is the truth.
     */
    @Override
    public Optional<PlaneConic> greatCircle(SkyPosition pole) {
        return Optional.empty();
    }

    @Override
    public Optional<WholeSphere> wholeSphere() {
        return Optional.of(new WholeSphere(ORIENTATION,
                centralLongitudeDegrees));
    }

    private static double normalised(double degrees) {
        double d = degrees % 360.0;
        if (d < 0.0) {
            d += 360.0;
        }
        return d == 360.0 ? 0.0 : d;
    }
}
