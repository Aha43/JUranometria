package juranometria.tool;

import java.time.Instant;
import java.util.List;

import juranometria.chart.ChartViewState;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.solar.MarsService;
import juranometria.solar.MarsService.Configuration;
import juranometria.solar.MarsService.DiscRelation;
import juranometria.solar.MarsService.MoonPlace;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The frame and the rules the Mars cartography study proposes (issue
 * #605): every body at its own astrometric J2000 place through the page's
 * ordinary projection, the moons' J2000 offsets within a fraction of a
 * pixel of the table's of-date ones even at 0.25°; the study's decision
 * rule - behind is never drawn, a moon on Mars's mark is not drawn without
 * Jupiter's floor; the thresholds it states; and the ladder still refusing
 * every closer field.
 */
class MarsCartographyTest {

    private static MarsService service;

    @BeforeAll
    static void load() {
        service = MarsService.load();
        MarsCartographyStudyMain.service = service;
    }

    @Test
    void everyBodyIsDrawnAtItsOwnJ2000PlaceThroughThePagesProjection() {
        Configuration c = service.observeMoonsGeocentric(MarsCartographyStudyMain.OPPOSITION_2003);
        DrawnPage page = DrawnPage.of(SaturnCartographyStudyMain.scene(c.mars().astrometricJ2000(), 0.25,
                MarsCartographyStudyMain.PAGE_W, MarsCartographyStudyMain.PAGE_H));
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        PixelPoint mars = mapping.toPixel(projection.project(c.mars().astrometricJ2000()).orElseThrow());
        double per = SaturnCartographyStudyMain.pxPerArcsecAt(projection, mapping, c.mars().astrometricJ2000());
        for (MoonPlace m : c.moons()) {
            PixelPoint at = mapping.toPixel(projection.project(m.astrometricJ2000()).orElseThrow());
            double[] o = MarsCartographyStudyMain.j2000Offset(c.mars(), m);
            // East is left on the chart; north is up.
            assertEquals(-o[0] * per, at.x() - mars.x(), 0.05, m.moon() + " east-west on the page");
            assertEquals(-o[1] * per, at.y() - mars.y(), 0.05, m.moon() + " north-south on the page");
            assertTrue(Math.hypot(o[0] - m.xArcseconds(), o[1] - m.yArcseconds()) * per < 0.5,
                    "the table's of-date offsets stay within half a pixel of the J2000 ones at 0.25°");
        }
    }

    @Test
    void aMoonBehindMarsIsNeverDrawnAndOneOnTheMarkIsNotDrawnWithoutTheFloor() {
        // Horizons codes Phobos U (behind, in the shadow) from Oslo on 2005-02-27.
        Configuration behind = service.observeMoons(MarsCartographyStudyMain.OSLO.at(
                MarsCartographyStudyMain.PHOBOS_BEHIND_IN_SHADOW));
        assertEquals(DiscRelation.BEHIND, behind.moon(MarsService.Moon.PHOBOS).discRelation());
        double per1 = JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700);
        for (boolean floor : new boolean[] {true, false}) {
            assertEquals(MarsCartographyStudyMain.Decision.BEHIND,
                    MarsCartographyStudyMain.decide(floor, behind, per1).get(0));
        }
        // At 8° both moons stand on Mars's 6 px mark at the 2003 opposition.
        Configuration close = service.observeMoonsGeocentric(MarsCartographyStudyMain.OPPOSITION_2003);
        double per8 = JovianCartographyStudyMain.pxPerArcsec(8.0, 900, 700);
        List<MarsCartographyStudyMain.Decision> d = MarsCartographyStudyMain.decide(false, close, per8);
        assertEquals(List.of(MarsCartographyStudyMain.Decision.AT_MARS, MarsCartographyStudyMain.Decision.AT_MARS), d);
        assertEquals(MarsCartographyStudyMain.decide(8.0, close, per8), d,
                "8° is above the floor: the geometry decides");
    }

    @Test
    void theClosestMarsExceedsTheMarkOnlyAtOneDegreeAndTheLadderStillRefusesTheCloserFields() {
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700);
        double largest = service.observeGeocentric(Instant.parse("2003-08-27T00:00:00Z")).equatorialDiameterArcseconds();
        assertTrue(largest * at1 > MarsCartographyStudyMain.MARK_PX, "above the mark at 1° near the 2003 opposition");
        assertTrue(largest * at1 / 2.0 < MarsCartographyStudyMain.MARK_PX, "and below it at 2°");
        for (double field : MarsCartographyStudyMain.CLOSER_FIELDS) {
            assertFalse(ChartViewState.fieldWidthSteps().contains(field), "the ladder refuses " + field + "°");
            assertTrue(field < ChartViewState.normalMinimumFieldDegrees());
        }
    }
}
