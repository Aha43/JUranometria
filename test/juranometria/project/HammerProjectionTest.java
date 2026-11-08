package juranometria.project;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.SkyPosition;
import juranometria.sky.Ecliptic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Hammer projection as ruled on #686 and built by #687: its
 * formulas held by computation rather than by the citation alone, its
 * frame the ecliptic, its central meridian a longitude, its seam a join
 * rule - and the three azimuthal projections unchanged by what the
 * interface gained.
 */
class HammerProjectionTest {

    private static final double LONGITUDE = 65.0;
    private static final SkyPosition CENTRE = HammerProjection.centreFor(LONGITUDE);
    private static final HammerProjection HAMMER = new HammerProjection(CENTRE);

    private static SkyPosition at(double offsetDegrees, double latitudeDegrees) {
        return Ecliptic.toEquatorial(LONGITUDE + offsetDegrees, latitudeDegrees);
    }

    @Test
    void theCentreLiesOnTheEclipticAtTheStatedLongitude() {
        Ecliptic.Coordinates c = Ecliptic.toEcliptic(HAMMER.centre());
        assertEquals(0.0, c.latitudeDegrees(), 1e-9, "on the ecliptic");
        assertEquals(LONGITUDE, c.longitudeDegrees(), 1e-9, "at the longitude");
        assertEquals(LONGITUDE, HAMMER.centralLongitudeDegrees(), 1e-9);
        // The registry's own, about the same centre.
        Projection registered = Projections.of(ChartProjection.HAMMER, CENTRE);
        assertTrue(registered instanceof HammerProjection);
        assertTrue(registered.centre().separationDegrees(CENTRE) < 1e-9,
                "the page is centred where it says it is");
        assertEquals("hammer", registered.name());
    }

    @Test
    void everyPositionHasAPointAndRoundTrips() {
        int checked = 0;
        for (double ra = 0.0; ra < 360.0; ra += 10.0) {
            for (double dec = -85.0; dec <= 85.0; dec += 10.0) {
                SkyPosition sky = new SkyPosition(ra, dec);
                Optional<PlanePoint> plane = HAMMER.project(sky);
                assertTrue(plane.isPresent(), "the whole sphere is on the plane: " + sky);
                assertTrue(HAMMER.outline().contains(plane.get().xiEast(),
                        plane.get().etaNorth()), "inside the outline: " + sky);
                SkyPosition back = HAMMER.unproject(plane.get()).orElseThrow();
                assertTrue(back.separationDegrees(sky) < 1e-7,
                        sky + " round-trips: " + back);
                checked++;
            }
        }
        assertTrue(checked > 600);
    }

    @Test
    void theOutlineIsTheHammerEllipseAndNothingOutsideItIsSky() {
        // MathWorld's outline, x^2/8 + y^2/2 = 1: reached at the seam,
        // in every latitude.
        for (double phi = -89.0; phi <= 89.0; phi += 11.0) {
            PlanePoint east = HAMMER.project(at(180.0 - 1e-9, phi)).orElseThrow();
            PlanePoint west = HAMMER.project(at(-180.0 + 1e-9, phi)).orElseThrow();
            for (PlanePoint edge : new PlanePoint[] {east, west}) {
                double on = edge.xiEast() * edge.xiEast() / 8.0
                        + edge.etaNorth() * edge.etaNorth() / 2.0;
                assertEquals(1.0, on, 1e-6, "on the outline at latitude " + phi);
            }
            assertTrue(east.xiEast() > 0.0 && west.xiEast() < 0.0,
                    "the two edges are the two sides of the seam");
        }
        assertEquals(new PlaneOutline.Ellipse(2.0 * Math.sqrt(2.0), Math.sqrt(2.0)),
                HAMMER.outline());
        assertTrue(HAMMER.unproject(new PlanePoint(2.9, 0.0)).isEmpty(), "paper");
        assertTrue(HAMMER.unproject(new PlanePoint(0.0, 1.5)).isEmpty(), "paper");
        assertTrue(HAMMER.unproject(new PlanePoint(2.5, 1.0)).isEmpty(),
                "inside the box, outside the ellipse: paper");
    }

    @Test
    void itIsEqualArea() {
        // The Jacobian of (lambda, phi) -> (x, y) equals cos phi, the
        // sphere's own area element, so equal areas on the sky are
        // equal areas on the plane (R-2's reason).
        double h = 1e-4;
        for (double lambda = -170.0; lambda <= 170.0; lambda += 20.0) {
            for (double phi = -80.0; phi <= 80.0; phi += 20.0) {
                PlanePoint dl1 = HAMMER.project(at(lambda + h, phi)).orElseThrow();
                PlanePoint dl0 = HAMMER.project(at(lambda - h, phi)).orElseThrow();
                PlanePoint dp1 = HAMMER.project(at(lambda, phi + h)).orElseThrow();
                PlanePoint dp0 = HAMMER.project(at(lambda, phi - h)).orElseThrow();
                double step = Math.toRadians(2.0 * h);
                double xl = (dl1.xiEast() - dl0.xiEast()) / step;
                double yl = (dl1.etaNorth() - dl0.etaNorth()) / step;
                double xp = (dp1.xiEast() - dp0.xiEast()) / step;
                double yp = (dp1.etaNorth() - dp0.etaNorth()) / step;
                double jacobian = Math.abs(xl * yp - xp * yl);
                assertEquals(Math.cos(Math.toRadians(phi)), jacobian, 1e-5,
                        "equal area at " + lambda + ", " + phi);
            }
        }
    }

    @Test
    void theSeamIsAJoinRuleNotAHiddenSliver() {
        SkyPosition east = at(179.0, 0.0);
        SkyPosition west = at(-179.0, 0.0);
        assertTrue(HAMMER.project(east).isPresent() && HAMMER.project(west).isPresent(),
                "both sides of the seam are drawn");
        assertFalse(HAMMER.mayJoin(east, west), "and not joined across it");
        assertTrue(HAMMER.mayJoin(east, at(170.0, 0.0)), "joined along the same side");
        assertTrue(HAMMER.mayJoin(at(90.0, 20.0), at(-90.0, 20.0)),
                "half a turn apart through the centre is joinable");
        // A position exactly on the seam is drawn, on the east edge.
        PlanePoint onSeam = HAMMER.project(at(180.0, 30.0)).orElseThrow();
        assertTrue(onSeam.xiEast() > 0.0);
    }

    @Test
    void theRadialLawIsTheMiddleLine() {
        assertEquals(0.0, HAMMER.planeRadius(0.0), 1e-12);
        assertEquals(HammerProjection.HALF_WIDTH, HAMMER.planeRadius(180.0), 1e-12);
        for (double a = 10.0; a < 180.0; a += 20.0) {
            double r = HAMMER.planeRadius(a);
            assertEquals(a, HAMMER.angleAtPlaneRadius(r), 1e-9, "inverse at " + a);
            PlanePoint along = HAMMER.project(at(a, 0.0)).orElseThrow();
            assertEquals(r, along.xiEast(), 1e-12,
                    "the law is where the ecliptic lands at " + a);
            assertEquals(0.0, along.etaNorth(), 1e-12, "the ecliptic is the middle line");
        }
        assertTrue(Double.isNaN(HAMMER.planeRadius(181.0)));
        assertTrue(Double.isNaN(HAMMER.angleAtPlaneRadius(3.0)));
    }

    @Test
    void whatItStatesAboutItself() {
        assertEquals(180.0, HAMMER.limitDegrees());
        assertEquals(180.0, HAMMER.usefulCornerDegrees());
        assertEquals(HammerProjection.HALF_WIDTH, HAMMER.visiblePlaneRadius());
        assertFalse(HAMMER.greatCirclesAreConics(), "no conic: the sampled route");
        assertTrue(HAMMER.greatCircle(Ecliptic.POLE).isEmpty());
        assertTrue(HAMMER.greatCircleVisibleHalf(Ecliptic.POLE).isEmpty());
        Projection.WholeSphere whole = HAMMER.wholeSphere().orElseThrow();
        assertEquals("ecliptic", whole.orientation());
        assertEquals(LONGITUDE, whole.centralLongitudeDegrees(), 1e-9);
    }

    @Test
    void eclipticNorthIsUpAndEclipticLongitudeIncreasesLeftAtCentre() {
        // The plane's east is positive xi, which the viewport mapping
        // draws leftwards; its north is positive eta, drawn upwards.
        PlanePoint east = HAMMER.project(at(10.0, 0.0)).orElseThrow();
        PlanePoint north = HAMMER.project(at(0.0, 10.0)).orElseThrow();
        assertTrue(east.xiEast() > 0.0 && Math.abs(east.etaNorth()) < 1e-12,
                "increasing ecliptic longitude is plane east");
        assertTrue(north.etaNorth() > 0.0 && Math.abs(north.xiEast()) < 1e-12,
                "ecliptic north is plane north");
        juranometria.chart.ChartViewport viewport = new juranometria.chart.ChartViewport(
                CENTRE, ChartProjection.WHOLE_SKY_DEGREES, 2000, 1000,
                ChartProjection.HAMMER);
        ViewportMapping mapping = new ViewportMapping(viewport, HAMMER);
        PixelPoint middle = mapping.toPixel(new PlanePoint(0.0, 0.0));
        assertTrue(mapping.toPixel(east).x() < middle.x(), "left on the page");
        assertTrue(mapping.toPixel(north).y() < middle.y(), "up on the page");
    }

    @Test
    void theAzimuthalProjectionsAreUnchangedByTheWidening() {
        SkyPosition orion = new SkyPosition(83.0, 0.0);
        for (ChartProjection kind : new ChartProjection[] {ChartProjection.GNOMONIC,
                ChartProjection.STEREOGRAPHIC, ChartProjection.ORTHOGRAPHIC}) {
            Projection projection = Projections.of(kind, orion);
            assertTrue(projection.greatCirclesAreConics(), kind + " keeps its conics");
            assertTrue(projection.wholeSphere().isEmpty(), kind + " shows part of the sky");
            assertTrue(projection.mayJoin(new SkyPosition(80.0, 10.0),
                    new SkyPosition(260.0, -10.0)), kind + " joins any two points");
            PlaneOutline outline = projection.outline();
            assertEquals(Double.isFinite(projection.visiblePlaneRadius()), outline.bounded(),
                    kind + ": the outline says what the radius said");
            if (outline instanceof PlaneOutline.Disc disc) {
                assertEquals(projection.visiblePlaneRadius(), disc.radius());
            } else {
                assertTrue(outline instanceof PlaneOutline.Unbounded);
            }
        }
    }
}
