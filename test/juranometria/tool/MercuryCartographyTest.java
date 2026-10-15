package juranometria.tool;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.project.PageBasis;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartPalette;
import juranometria.solar.MercuryService;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #539 study's frame and round trips (Sprint 50), on the ordinary
 * 1° page and on the two candidate closer fields it paints: Mercury's
 * J2000 place projects onto the page and back to within a
 * microarcsecond; the production bodies layer centres Mercury on that
 * very pixel; under rule M the mark stands in exactly while the true
 * disc is smaller; the lit region is k of the disc; and the ladder
 * itself still refuses every candidate field - the study paints them
 * through viewports and offers none of them.
 */
class MercuryCartographyTest {

    private static final Instant ELONGATION = Instant.parse("2026-02-19T18:00:00Z");
    private static final Instant CRESCENT = Instant.parse("2026-02-23T00:00:00Z");

    @BeforeAll
    static void load() throws Exception {
        java.lang.reflect.Field m = MercuryCartographyStudyMain.class.getDeclaredField("mercury");
        m.setAccessible(true);
        m.set(null, MercuryService.load());
        java.lang.reflect.Field s = MercuryCartographyStudyMain.class.getDeclaredField("solar");
        s.setAccessible(true);
        s.set(null, juranometria.solar.SolarSystemService.load());
    }

    private static MercuryService.MercuryObservation at(Instant when) {
        return MercuryService.load().observe(MercuryCartographyStudyMain.OSLO.at(when));
    }

    @Test
    void theLadderStillRefusesEveryCandidateField() {
        for (double field : MercuryCartographyStudyMain.CLOSER_FIELDS) {
            assertThrows(IllegalArgumentException.class, () -> new ChartViewState(
                    new SkyPosition(0.0, 0.0), field, 8.0), field + "° is not offered");
        }
        assertEquals(1.0, ChartViewState.normalMinimumFieldDegrees(), "the floor is unchanged");
    }

    @Test
    void mercuryProjectsOntoEveryCandidatePageAndBack() {
        SkyPosition place = at(ELONGATION).astrometricJ2000();
        for (double field : new double[] {1.0, 0.5, 0.25}) {
            DrawnPage page = DrawnPage.of(MercuryCartographyStudyMain.scene(place, field));
            PlanePoint plane = page.projection().project(place).orElseThrow();
            SkyPosition back = page.projection().unproject(plane).orElseThrow();
            assertTrue(back.separationDegrees(place) * 3600.0e6 < 1.0,
                    field + "°: back to within a microarcsecond");
            PixelPoint px = new ViewportMapping(page).toPixel(plane);
            assertEquals(MercuryCartographyStudyMain.PAGE_W / 2.0, px.x(), 1e-6, "centred");
            assertEquals(MercuryCartographyStudyMain.PAGE_H / 2.0, px.y(), 1e-6);
        }
    }

    @Test
    void theBodiesLayerCentresMercuryOnItsProjectedPixelAndKeepsRuleM() {
        MercuryService.MercuryObservation m = at(CRESCENT);
        PageText en = PageText.in(InterfaceText.forLanguage("en"));
        for (double field : new double[] {1.0, 0.5, 0.25}) {
            ChartScene scene = MercuryCartographyStudyMain.scene(m.astrometricJ2000(), field);
            DrawnPage page = DrawnPage.of(scene);
            PixelPoint expected = new ViewportMapping(page).toPixel(
                    page.projection().project(m.astrometricJ2000()).orElseThrow());
            MercuryCartographyStudyMain.Drawn d = MercuryCartographyStudyMain.paint(
                    new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB), scene,
                    MercuryCartographyStudyMain.offering(MercuryCartographyStudyMain.OSLO.at(CRESCENT),
                            false, MercuryCartographyStudyMain.MARK_PX, false, false),
                    ChartPalette.WHITE_PAPER, MercuryCartographyStudyMain.words(en, false), Set.of());
            assertNotNull(d, field + "°: Mercury drawn");
            assertEquals(expected.x(), d.centre().x(), 0.01, field + "°: on its projected pixel");
            assertEquals(expected.y(), d.centre().y(), 0.01);
            double truePx = m.solidBodyDiameterArcseconds() * 900.0 / (field * 3600.0);
            if (truePx < MercuryCartographyStudyMain.MARK_PX) {
                assertTrue(d.symbol(), field + "°: the mark stands in for a smaller disc");
                assertEquals(MercuryCartographyStudyMain.MARK_PX, d.discPx(), 0.01);
            } else {
                assertFalse(d.symbol(), field + "°: the true disc once it reaches the mark");
                assertEquals(truePx, d.discPx(), 0.05);
            }
        }
    }

    @Test
    void theLitRegionIsKOfTheDiscOnTheClosestPage() {
        MercuryService.MercuryObservation m = at(CRESCENT);
        DrawnPage page = DrawnPage.of(MercuryCartographyStudyMain.scene(m.astrometricJ2000(), 0.25));
        PageBasis basis = PageBasis.at(page, m.astrometricJ2000()).orElseThrow();
        double share = MercuryCartographyStudyMain.litShare(new PixelPoint(450.0, 350.0), 40.0,
                basis, new juranometria.module.OverlayContribution.Lit(m.illuminatedFraction(),
                        m.brightLimbAngleDegrees(), m.phaseAngleDegrees()));
        assertEquals(m.illuminatedFraction(), share, 0.005,
                "the lit area is k of the disc's (C3), measured on a 40 px disc");
    }
}
