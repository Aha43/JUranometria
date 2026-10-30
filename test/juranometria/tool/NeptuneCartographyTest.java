package juranometria.tool;

import java.util.List;

import juranometria.chart.ChartViewState;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.solar.NeptuneService;
import juranometria.solar.NeptuneService.Configuration;
import juranometria.solar.NeptuneService.DiscRelation;
import juranometria.solar.NeptuneService.MoonPlace;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The frame and the rules the Neptune cartography study proposes (issue
 * #658): every body at its own astrometric J2000 place through the page's
 * ordinary projection, the moons' J2000 offsets within half a pixel of
 * the table's of-date ones at 0.25° at the 2026 opposition (the report
 * measures the century's worst, Nereid's 2.3″); the study's decision
 * rule - behind is never drawn, a moon on Neptune's mark is not drawn
 * without Jupiter's floor; the thresholds it states - Neptune never reaches
 * the mark, Triton never clears it at 1°; the moons' magnitudes read from
 * Horizons' committed responses; and the ladder still refusing every
 * closer field.
 */
class NeptuneCartographyTest {

    private static NeptuneService service;

    @BeforeAll
    static void load() {
        service = NeptuneService.load();
        NeptuneCartographyStudyMain.service = service;
    }

    @Test
    void everyBodyIsDrawnAtItsOwnJ2000PlaceThroughThePagesProjection() {
        Configuration c = service.observeMoonsGeocentric(NeptuneCartographyStudyMain.OPPOSITION_2026);
        DrawnPage page = DrawnPage.of(SaturnCartographyStudyMain.scene(c.neptune().astrometricJ2000(), 0.25,
                NeptuneCartographyStudyMain.PAGE_W, NeptuneCartographyStudyMain.PAGE_H));
        Projection projection = page.projection();
        ViewportMapping mapping = new ViewportMapping(page);
        PixelPoint neptune = mapping.toPixel(projection.project(c.neptune().astrometricJ2000()).orElseThrow());
        double per = SaturnCartographyStudyMain.pxPerArcsecAt(projection, mapping, c.neptune().astrometricJ2000());
        for (MoonPlace m : c.moons()) {
            PixelPoint at = mapping.toPixel(projection.project(m.astrometricJ2000()).orElseThrow());
            double[] o = NeptuneCartographyStudyMain.j2000Offset(c.neptune(), m);
            // East is left on the chart; north is up.
            assertEquals(-o[0] * per, at.x() - neptune.x(), 0.05, m.moon() + " east-west on the page");
            assertEquals(-o[1] * per, at.y() - neptune.y(), 0.05, m.moon() + " north-south on the page");
            assertTrue(Math.hypot(o[0] - m.xArcseconds(), o[1] - m.yArcseconds()) * per < 0.5,
                    "at the 2026 opposition the table's of-date offsets stay within half a pixel of the J2000 ones at 0.25°");
        }
    }

    @Test
    void aMoonBehindNeptuneIsNeverDrawnAndOneOnTheMarkIsNotDrawnWithoutTheFloor() {
        // Horizons codes Triton U (behind, in the shadow) from Oslo on 2044-09-26.
        Configuration behind = service.observeMoons(NeptuneCartographyStudyMain.OSLO.at(
                NeptuneCartographyStudyMain.TRITON_BEHIND_IN_SHADOW));
        assertEquals(DiscRelation.BEHIND, behind.moon(NeptuneService.Moon.TRITON).discRelation());
        double per1 = JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700);
        for (boolean floor : new boolean[] {true, false}) {
            assertEquals(NeptuneCartographyStudyMain.Decision.BEHIND,
                    NeptuneCartographyStudyMain.decide(floor, behind, per1).get(0));
        }
        // At 8° Triton stands on Neptune's 6 px mark at the 2026 opposition; Nereid, far out, does not.
        Configuration close = service.observeMoonsGeocentric(NeptuneCartographyStudyMain.OPPOSITION_2026);
        double per8 = JovianCartographyStudyMain.pxPerArcsec(8.0, 900, 700);
        List<NeptuneCartographyStudyMain.Decision> at8 = NeptuneCartographyStudyMain.decide(false, close, per8);
        assertEquals(NeptuneCartographyStudyMain.Decision.AT_NEPTUNE, at8.get(NeptuneService.Moon.TRITON.ordinal()));
        assertEquals(NeptuneCartographyStudyMain.Decision.DRAWN, at8.get(NeptuneService.Moon.NEREID.ordinal()));
        // With Jupiter's floor at 1°, every moon not behind is drawn, on the mark or not.
        List<NeptuneCartographyStudyMain.Decision> floored = NeptuneCartographyStudyMain.decide(true, close, per1);
        for (int i = 0; i < floored.size(); i++) {
            assertEquals(close.moons().get(i).discRelation() == DiscRelation.BEHIND
                            ? NeptuneCartographyStudyMain.Decision.BEHIND : NeptuneCartographyStudyMain.Decision.DRAWN,
                    floored.get(i));
        }
    }

    @Test
    void neptuneNeverReachesTheMarkAndTritonNeverClearsItAtOneDegree() throws Exception {
        double at1 = JovianCartographyStudyMain.pxPerArcsec(1.0, 900, 700);
        NeptuneCartographyStudyMain.Ranges r = NeptuneCartographyStudyMain.ranges();
        assertTrue(r.eqMax() * at1 / 0.25 < NeptuneCartographyStudyMain.MARK_PX, "under the mark even at 0.25°");
        int triton = NeptuneService.Moon.TRITON.ordinal();
        // Triton's greatest separation at 1° is inside the clearance a 3 px symbol needs from the mark.
        assertTrue(r.sepMax()[triton] * at1 < NeptuneCartographyStudyMain.MARK_PX / 2.0
                + JovianCartographyStudyMain.SEPARABLE_PX / 2.0 + NeptuneCartographyStudyMain.MOON_SYMBOL_PX / 2.0,
                "Triton never clears Neptune's mark at 1°");
        double[][] mags = NeptuneCartographyStudyMain.moonMagnitudes();
        assertTrue(mags[triton][0] > 13.0 && mags[triton][1] < 14.0, "Triton about magnitude 13.5");
        assertTrue(mags[NeptuneService.Moon.NEREID.ordinal()][0] > 18.0, "Nereid fainter than 18");
        for (double field : NeptuneCartographyStudyMain.CLOSER_FIELDS) {
            assertFalse(ChartViewState.fieldWidthSteps().contains(field), "the ladder refuses " + field + "°");
            assertTrue(field < ChartViewState.normalMinimumFieldDegrees());
        }
    }
}
