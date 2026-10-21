package juranometria.tool;

import java.time.Instant;
import java.util.List;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.Configuration;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.SaturnObservation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The frame contract the Saturn cartography study proposes (issue #578):
 * every body at its own astrometric J2000 place through the page's
 * ordinary projection, on the 1° page and the two closer fields it
 * paints; Saturn's pole derived in the chart's frame, never the table's
 * of-date angle; the ring ellipse's axes on the page the contract's
 * semi-axes at the page's scale, its major axis across the pole; the
 * near half on the ruled side of the pole; the thresholds the study
 * states; and the ladder still refusing every closer field.
 */
class SaturnCartographyTest {

    private static SaturnSystemService service;

    @BeforeAll
    static void load() {
        service = SaturnSystemService.load();
        SaturnCartographyStudyMain.service = service;
    }

    @Test
    void thePoleDerivedInTheChartsFrameAgreesWithTheTablesAngleCarriedBackToJ2000() {
        double worst = 0.0;
        double turn = 0.0;
        for (int year = 1900; year <= 2100; year += 4) {
            Instant t = Instant.parse(year + "-03-01T00:00:00Z");
            SaturnObservation s = service.observeSaturnGeocentric(t);
            double jd = service.timeScales().tt(t).jdTt();
            double derived = service.poleAngleJ2000Degrees(s);
            double carried = JovianChartGeometry.ofDateAngleCarriedToJ2000(s.apparentOfDate(),
                    s.poleAngleDegrees(), jd);
            worst = Math.max(worst, Math.abs(SaturnCartographyStudyMain.wrap(derived - carried)));
            turn = Math.max(turn, Math.abs(SaturnCartographyStudyMain.wrap(derived - s.poleAngleDegrees())));
        }
        assertTrue(worst < 0.01, "derived in J2000 and carried back agree to 0.01°: " + worst);
        assertTrue(turn > 0.1, "while the two frames' angles differ - the reason the"
                + " table's is never drawn as it stands: " + turn);
    }

    @Test
    void everyBodyProjectsOntoEveryPageAndBack() {
        Configuration c = service.observeMoons(SaturnCartographyStudyMain.OSLO);
        double[] fields = {ChartViewState.normalMinimumFieldDegrees(),
                SaturnCartographyStudyMain.CLOSER_FIELDS[0], SaturnCartographyStudyMain.CLOSER_FIELDS[1]};
        for (double field : fields) {
            DrawnPage page = DrawnPage.of(SaturnCartographyStudyMain.scene(
                    c.saturn().astrometricJ2000(), field, SaturnCartographyStudyMain.PAGE_W,
                    SaturnCartographyStudyMain.PAGE_H));
            Projection projection = page.projection();
            ViewportMapping mapping = new ViewportMapping(page);
            PixelPoint centre = mapping.toPixel(projection.project(c.saturn().astrometricJ2000()).orElseThrow());
            assertEquals(SaturnCartographyStudyMain.PAGE_W / 2.0, centre.x(), 1e-6, "Saturn centred at " + field);
            assertEquals(SaturnCartographyStudyMain.PAGE_H / 2.0, centre.y(), 1e-6);
            for (MoonPlace m : c.moons()) {
                SkyPosition at = m.astrometricJ2000();
                PlanePoint plane = projection.project(at).orElseThrow();
                SkyPosition back = projection.unproject(plane).orElseThrow();
                assertTrue(at.separationDegrees(back) * 3600.0 < 1e-6,
                        "projected and unprojected at " + field + "°, a place returns to itself: " + at);
            }
        }
    }

    @Test
    void theRingEllipseOnThePageIsTheContractsSemiAxesAcrossThePole() {
        SaturnObservation s = service.observeSaturn(SaturnCartographyStudyMain.OSLO);
        DrawnPage page = DrawnPage.of(SaturnCartographyStudyMain.scene(s.astrometricJ2000(), 0.25,
                SaturnCartographyStudyMain.PAGE_W, SaturnCartographyStudyMain.PAGE_H));
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        SaturnCartographyStudyMain.Figure fig = SaturnCartographyStudyMain.figureOn(s, projection, mapping, 1.0);
        double per = SaturnCartographyStudyMain.pxPerArcsecAt(projection, mapping, s.astrometricJ2000());
        double aOuter = SaturnCartographyStudyMain.aOuterKm();
        assertTrue(fig.rings(), "at 0.25° the rings are drawn true");
        assertEquals(s.ringSemiMajorArcseconds(aOuter) * per, fig.a(), 1e-9, "the semi-major axis at the page's scale");
        assertEquals(s.ringSemiMinorArcseconds(aOuter) * per, fig.b(), 1e-9, "the semi-minor axis, |sin B| of it");
        assertEquals(s.equatorialDiameterArcseconds() * per, fig.globeEq(), 1e-9);
        assertEquals(s.polarDiameterArcseconds() * per, fig.globePol(), 1e-9, "the polar diameter at B, the contract's");
        double dot = fig.majorPx()[0] * fig.polePx()[0] + fig.majorPx()[1] * fig.polePx()[1];
        assertEquals(0.0, dot, 1e-9, "the major axis is across the pole");
        double[] up = JovianChartGeometry.pageDirection(projection, mapping, s.astrometricJ2000(),
                service.poleAngleJ2000Degrees(s));
        assertEquals(up[0], fig.polePx()[0], 1e-9, "the pole is the derived J2000 angle turned by the page");
        assertEquals(up[1], fig.polePx()[1], 1e-9);
    }

    @Test
    void theNearHalfLiesAwayFromTheProjectedPoleWhenTheOpeningIsNorth() {
        double[] pole = {0.0, -1.0};   // page-up
        java.awt.Shape northOpen = SaturnCartographyStudyMain.nearHalf(100, pole, +10.0);
        java.awt.Shape southOpen = SaturnCartographyStudyMain.nearHalf(100, pole, -10.0);
        assertTrue(northOpen.contains(0, 5), "B > 0: the near half is the page-down side, away from the pole");
        assertFalse(northOpen.contains(0, -5));
        assertTrue(southOpen.contains(0, -5), "B < 0: the near half is toward the pole");
        assertFalse(southOpen.contains(0, 5));
    }

    @Test
    void theTablesOffsetsAreNotThePagesOneTheJ2000PlacesAre() {
        double worst = 0.0;
        for (int year = 2000; year <= 2100; year += 10) {
            Configuration c = service.observeMoonsGeocentric(Instant.parse(year + "-06-01T00:00:00Z"));
            for (MoonPlace m : c.moons()) {
                double[] off = SaturnCartographyStudyMain.j2000Offset(c.saturn(), m);
                worst = Math.max(worst, Math.hypot(off[0] - m.xArcseconds(), off[1] - m.yArcseconds()));
            }
        }
        assertTrue(worst > 0.5, "the of-date X/Y differ from the J2000 offsets by whole arcseconds"
                + " over the century, so a page never draws Saturn's place plus X/Y: " + worst);
    }

    @Test
    void theLadderStillOffersNoCloserField() {
        List<Double> steps = ChartViewState.fieldWidthSteps();
        for (double closer : SaturnCartographyStudyMain.CLOSER_FIELDS) {
            assertFalse(steps.contains(closer), "the ladder does not hold " + closer);
            assertTrue(closer < ChartViewState.normalMinimumFieldDegrees());
        }
    }

    @Test
    void theStudysThresholdsAreTheOnesItStates() {
        assertEquals(6.0, SaturnCartographyStudyMain.SATURN_MARK_PX, "Jupiter's ruled minimum mark");
        assertEquals(3.0, SaturnCartographyStudyMain.MOON_SYMBOL_PX, "Jupiter's ruled moon symbol");
        assertEquals(6.0, SaturnCartographyStudyMain.SEPARABLE_PX);
        assertEquals(2.0, SaturnCartographyStudyMain.EDGE_ON_PX, "rings thinner than this are a line");
        assertEquals(1.0, SaturnCartographyStudyMain.LINE_PX);
        assertEquals(6.0, SaturnCartographyStudyMain.OUTLINE_MINOR_PX);
        SaturnObservation s = service.observeSaturn(SaturnCartographyStudyMain.OSLO);
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, SaturnCartographyStudyMain.PAGE_W,
                SaturnCartographyStudyMain.PAGE_H);
        assertTrue(SaturnCartographyStudyMain.figure(s, at1, 0.0).rings(),
                "at 1° the card instant's rings are wider than the mark: drawn true");
        assertFalse(SaturnCartographyStudyMain.figure(s, at1 / 2.0, 0.0).rings(),
                "at 2° they are under it: the symbol stands in");
    }
}
