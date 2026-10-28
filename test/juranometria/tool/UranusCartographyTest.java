package juranometria.tool;

import java.time.Instant;
import java.util.List;

import juranometria.chart.ChartViewState;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.solar.UranusService;
import juranometria.solar.UranusService.Configuration;
import juranometria.solar.UranusService.DiscRelation;
import juranometria.solar.UranusService.MoonPlace;
import juranometria.solar.UranusService.UranusObservation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The frame and the rules the Uranus cartography study proposes (issue
 * #637): every body at its own astrometric J2000 place through the page's
 * ordinary projection, the moons' J2000 offsets within a fraction of a
 * pixel of the table's of-date ones even at 0.25°; the study's decision
 * rule - behind is never drawn, a moon on Uranus's mark is not drawn
 * without Jupiter's floor; the thresholds it states - Uranus never reaches
 * the mark, the Epsilon ring leaves it only at 0.25°; and the ladder still
 * refusing every closer field.
 */
class UranusCartographyTest {

    private static UranusService service;

    @BeforeAll
    static void load() {
        service = UranusService.load();
        UranusCartographyStudyMain.service = service;
    }

    @Test
    void everyBodyIsDrawnAtItsOwnJ2000PlaceThroughThePagesProjection() {
        Configuration c = service.observeMoonsGeocentric(UranusCartographyStudyMain.OPPOSITION_2026);
        DrawnPage page = DrawnPage.of(SaturnCartographyStudyMain.scene(c.uranus().astrometricJ2000(), 0.25,
                UranusCartographyStudyMain.PAGE_W, UranusCartographyStudyMain.PAGE_H));
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        PixelPoint uranus = mapping.toPixel(projection.project(c.uranus().astrometricJ2000()).orElseThrow());
        double per = SaturnCartographyStudyMain.pxPerArcsecAt(projection, mapping, c.uranus().astrometricJ2000());
        for (MoonPlace m : c.moons()) {
            PixelPoint at = mapping.toPixel(projection.project(m.astrometricJ2000()).orElseThrow());
            double[] o = UranusCartographyStudyMain.j2000Offset(c.uranus(), m);
            // East is left on the chart; north is up.
            assertEquals(-o[0] * per, at.x() - uranus.x(), 0.05, m.moon() + " east-west on the page");
            assertEquals(-o[1] * per, at.y() - uranus.y(), 0.05, m.moon() + " north-south on the page");
            assertTrue(Math.hypot(o[0] - m.xArcseconds(), o[1] - m.yArcseconds()) * per < 0.5,
                    "the table's of-date offsets stay within half a pixel of the J2000 ones at 0.25°");
        }
    }

    @Test
    void aMoonBehindUranusIsNeverDrawnAndOneOnTheMarkIsNotDrawnWithoutTheFloor() {
        // Horizons codes Miranda U (behind, in the shadow) from Oslo on 2005-03-19.
        Configuration behind = service.observeMoons(UranusCartographyStudyMain.OSLO.at(
                UranusCartographyStudyMain.MIRANDA_BEHIND_IN_SHADOW));
        assertEquals(DiscRelation.BEHIND, behind.moon(UranusService.Moon.MIRANDA).discRelation());
        double per1 = JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700);
        for (boolean floor : new boolean[] {true, false}) {
            assertEquals(UranusCartographyStudyMain.Decision.BEHIND,
                    UranusCartographyStudyMain.decide(floor, behind, per1).get(0));
        }
        // At 8° every moon stands on Uranus's 6 px mark at the 2026 opposition.
        Configuration close = service.observeMoonsGeocentric(UranusCartographyStudyMain.OPPOSITION_2026);
        double per8 = JovianCartographyStudyMain.pxPerArcsec(8.0, 900, 700);
        for (UranusCartographyStudyMain.Decision d : UranusCartographyStudyMain.decide(false, close, per8)) {
            assertEquals(UranusCartographyStudyMain.Decision.AT_URANUS, d);
        }
        // With Jupiter's floor at 1°, every moon not behind is drawn, on the mark or not.
        List<UranusCartographyStudyMain.Decision> floored = UranusCartographyStudyMain.decide(true, close, per1);
        for (int i = 0; i < floored.size(); i++) {
            assertEquals(close.moons().get(i).discRelation() == DiscRelation.BEHIND
                            ? UranusCartographyStudyMain.Decision.BEHIND : UranusCartographyStudyMain.Decision.DRAWN,
                    floored.get(i));
        }
    }

    @Test
    void uranusNeverReachesTheMarkAndTheEpsilonRingLeavesItOnlyAtTheNarrowestField() {
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700);
        // Uranus's largest disc in the weekly Oslo matrix (measurements.md, 4.07631″, 2051-03-13).
        UranusObservation largest = service.observeGeocentric(Instant.parse("2051-03-13T00:00:00Z"));
        assertTrue(largest.equatorialDiameterArcseconds() * at1 / 0.25 < UranusCartographyStudyMain.MARK_PX,
                "under the mark even at 0.25°");
        double ring = UranusCartographyStudyMain.ringDiameterArcseconds(largest,
                UranusCartographyStudyMain.EPSILON_RING_KM);
        assertTrue(ring * at1 / 0.5 < UranusCartographyStudyMain.MARK_PX, "inside the mark at 0.5°");
        assertTrue(ring * at1 / 0.25 > UranusCartographyStudyMain.MARK_PX, "and beyond it at 0.25°");
        assertEquals(UranusCartographyStudyMain.EPSILON_RING_KM / 25559.0,
                ring / largest.equatorialDiameterArcseconds(), 1e-3,
                "the ring's diameter against the globe's is the radii's ratio");
        for (double field : UranusCartographyStudyMain.CLOSER_FIELDS) {
            assertFalse(ChartViewState.fieldWidthSteps().contains(field), "the ladder refuses " + field + "°");
            assertTrue(field < ChartViewState.normalMinimumFieldDegrees());
        }
    }
}
