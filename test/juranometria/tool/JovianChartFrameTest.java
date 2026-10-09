package juranometria.tool;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.Configuration;
import juranometria.solar.JovianSystemService.JupiterObservation;
import juranometria.solar.JovianSystemService.MoonPlace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The frame contract the Jovian cartography study proposes (issue #482):
 * every body at its own astrometric J2000 place through the page's
 * ordinary projection, and Jupiter's pole derived in the chart's frame -
 * never the table's apparent, of-date offsets or angle rotated by an
 * assumed correction.
 */
class JovianChartFrameTest {

    private static JovianSystemService service;
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-12-11T22:45:00Z"));

    @BeforeAll
    static void load() {
        service = JovianSystemService.load();
    }

    @Test
    void thePoleDerivedInTheChartsFrameAgreesWithTheTablesAngleCarriedBackToJ2000() {
        double worst = 0.0;
        double turn = 0.0;
        for (int year = 1900; year <= 2100; year += 4) {
            Instant t = Instant.parse(year + "-03-01T00:00:00Z");
            JupiterObservation j = service.observeJupiterGeocentric(t);
            double jd = service.timeScales().tt(t).jdTt();
            double derived = JovianChartGeometry.positionAngleJ2000(j.astrometricJ2000(),
                    JovianChartGeometry.pole(service.pack().constants(), jd));
            double carried = JovianChartGeometry.ofDateAngleCarriedToJ2000(j.apparentOfDate(),
                    j.poleAngleDegrees(), jd);
            worst = Math.max(worst, Math.abs(wrap(derived - carried)));
            turn = Math.max(turn, Math.abs(wrap(derived - j.poleAngleDegrees())));
        }
        assertTrue(worst < 0.01, "derived in J2000 and carried back agree to 0.01°: " + worst);
        assertTrue(turn > 0.1, "while the two frames' angles differ - the reason the"
                + " table's is never drawn as it stands: " + turn);
    }

    @Test
    void everyBodysPageCentreIsItsOwnJ2000PlaceAndRoundTrips() {
        Configuration c = service.observeMoons(OSLO);
        DrawnPage page = JovianCartographyStudyMain.emptyPage(c.jupiter().astrometricJ2000(),
                1.0, 900, 700);
        Projection projection = page.projection();
        List<SkyPosition> places = new java.util.ArrayList<>();
        places.add(c.jupiter().astrometricJ2000());
        for (MoonPlace m : c.moons()) {
            places.add(m.astrometricJ2000());
        }
        for (SkyPosition at : places) {
            PlanePoint plane = projection.project(at).orElseThrow();
            SkyPosition back = projection.unproject(plane).orElseThrow();
            assertTrue(at.separationDegrees(back) * 3600.0 < 1e-6,
                    "projected and unprojected, a place returns to itself: " + at);
        }
    }

    @Test
    void aMoonsPageOffsetIsItsJ2000OffsetTurnedByThePagesTangentsAcrossTheSky() {
        Configuration c = service.observeMoons(OSLO);
        JupiterObservation j = c.jupiter();
        // Jupiter's own arrangement, carried to other parts of the sky by
        // keeping each moon's J2000 offset - the poles, the equator, RA 0/24h.
        for (SkyPosition centre : List.of(new SkyPosition(0.001, 0.0),
                new SkyPosition(359.999, 0.0), new SkyPosition(150.0, 15.0),
                new SkyPosition(40.0, 89.5), new SkyPosition(220.0, -89.5))) {
            DrawnPage page = JovianCartographyStudyMain.emptyPage(centre, 1.0, 900, 700);
            Projection projection = page.projection();
            ViewportMapping mapping = new ViewportMapping(page);
            for (MoonPlace m : c.moons()) {
                double[] off = JovianCartographyStudyMain.j2000Offset(j, m);
                double distance = Math.hypot(off[0], off[1]) / 3600.0;
                double angle = Math.toDegrees(Math.atan2(off[0], off[1]));
                SkyPosition moonHere = JovianChartGeometry.offset(centre, angle, distance);
                PixelPoint a = mapping.toPixel(projection.project(centre).orElseThrow());
                PixelPoint b = mapping.toPixel(projection.project(moonHere).orElseThrow());
                double[] dir = JovianChartGeometry.pageDirection(projection, mapping, centre, angle);
                double perArcsec = JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700);
                double expectedX = a.x() + dir[0] * distance * 3600.0 * perArcsec;
                double expectedY = a.y() + dir[1] * distance * 3600.0 * perArcsec;
                assertTrue(Math.hypot(b.x() - expectedX, b.y() - expectedY) < 0.05,
                        m.moon() + " at " + centre + ": the page offset is the J2000 offset"
                                + " turned by the page's tangents");
            }
        }
    }

    @Test
    void jupitersPagePositionIsContinuousThroughItsJup365SegmentBoundary() {
        Instant split = Instant.parse("1997-01-16T00:00:00Z").minusSeconds(65); // TDB 2450464.5 is ~63 s after 00:00 UTC
        double worst = 0.0;
        JupiterObservation before = null;
        for (int s = 0; s <= 240; s++) {
            JupiterObservation now = service.observeJupiterGeocentric(split.plusSeconds(s));
            if (before != null) {
                worst = Math.max(worst, now.astrometricJ2000()
                        .separationDegrees(before.astrometricJ2000()) * 3600.0);
            }
            before = now;
        }
        DrawnPage page = JovianCartographyStudyMain.emptyPage(before.astrometricJ2000(), 1.0, 900, 700);
        double perArcsec = JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700);
        assertTrue(worst * perArcsec < 0.01, "one second's step is under a hundredth of a"
                + " pixel at 1° across the boundary: " + worst * perArcsec);
        assertTrue(page.projection().project(before.astrometricJ2000()).isPresent());
    }

    @Test
    void theTablesOffsetsAreNotThePagesOneTheJ2000PlacesAre() {
        double worst = 0.0;
        for (Instant t = Instant.parse("2001-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2100-12-01T00:00:00Z")); t = t.plusSeconds(86400L * 97)) {
            Configuration c = service.observeMoonsGeocentric(t);
            for (MoonPlace m : c.moons()) {
                double[] off = JovianCartographyStudyMain.j2000Offset(c.jupiter(), m);
                worst = Math.max(worst, Math.hypot(off[0] - m.xArcseconds(), off[1] - m.yArcseconds()));
            }
        }
        assertTrue(worst > 1.0 && worst < 8.0, "J2000 offsets differ from the table's apparent"
                + " X/Y by arcseconds (measured 6.0″ over the century): " + worst);
    }

    @Test
    void theStudysThresholdsAreTheOnesItStates() {
        assertEquals(1.0, JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700) * 3600.0 / 900.0,
                1e-3, "900 px across a 1° page");
        assertEquals(6.0, JovianCartographyStudyMain.JUPITER_MARK_PX);
        assertEquals(3.0, JovianCartographyStudyMain.MOON_SYMBOL_PX);
    }

    private static double wrap(double degrees) {
        double d = SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
    }
}
