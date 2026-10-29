package juranometria.ui;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.UranusService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Miranda, Ariel, Umbriel, Titania and Oberon on the page (issue #638 part
 * 2, R-5, R-6 and R-8 on #637): each a 3 px symbol at its own J2000 place,
 * decided on its own by Mars's clearance rule - drawn only where its mark
 * stands clear of Uranus's drawn mark and of the moons already drawn, on
 * every field, with no floor; a transit never drawn, since Uranus's mark
 * cannot hold a moon; behind, or too close to tell apart, not drawn but
 * spoken; never selectable; before 2000 Uranus alone. The instants are
 * Espenak's 2026 opposition hour and Horizons' coded Oslo rows
 * (docs/studies/uranus-system/).
 */
class UranusMoonsInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final SolarSystemService SOLAR = SolarSystemService.load();
    private static final UranusService URANUS = UranusService.load();
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;
    /** Espenak's 2026 opposition hour (uranus-events.txt). */
    private static final Instant OPPOSITION_2026 = Instant.parse("2026-11-25T23:00:00Z");

    private static String id(UranusService.Moon moon) {
        return SolarSystemModule.identityOf(moon);
    }

    private static Observer oslo(Instant when) {
        return new Observer(OSLO_LAT, OSLO_LON, when);
    }

    private static OverlayRegistry offering(Instant when) {
        SolarSystemModule module = new SolarSystemModule(() -> oslo(when), () -> SOLAR,
                () -> false);
        module.withUranus(() -> URANUS);
        module.uranusShowing(true);
        module.sunShowing(false);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        return registry;
    }

    private static ChartScene page(Instant when, double field) {
        return Atlas.assembler().assemble(new ChartViewState(
                URANUS.observe(oslo(when)).astrometricJ2000(), field, 8.0, null, null), 900, 700);
    }

    private static List<ReferenceInk.BodyPlacement> placed(ChartScene scene, OverlayRegistry r,
                                                           PageText words) {
        return ReferenceInk.bodyPlacements(DrawnPage.of(scene), r.collect(),
                ChartPalette.WHITE_PAPER, words, List.of());
    }

    private static String said(ChartScene scene, OverlayRegistry r, PageText words) {
        return ChartComponent.withUnseenMoons(ChartComponent.withBodies("The page.",
                placed(scene, r, words), words),
                ReferenceInk.unseenMoons(DrawnPage.of(scene), r.collect()), words);
    }

    private static ReferenceInk.BodyPlacement find(List<ReferenceInk.BodyPlacement> all,
                                                   String identity) {
        return all.stream().filter(b -> identity.equals(b.identity())).findFirst().orElse(null);
    }

    @Test
    void atOneDegreeTheOuterMoonsStandClearAndAreDotsAndAtEightTheyAreSpoken() {
        OverlayRegistry r = offering(OPPOSITION_2026);
        ChartScene minimum = page(OPPOSITION_2026, ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> drawn = placed(minimum, r, ENGLISH);
        for (UranusService.Moon moon : List.of(UranusService.Moon.TITANIA, UranusService.Moon.OBERON)) {
            ReferenceInk.BodyPlacement m = find(drawn, id(moon));
            assertNotNull(m, moon + " stands clear of Uranus's mark at 1° on the 2026 opposition");
            assertEquals(SolarSystemModule.URANUS_MOON_MARK_PX, m.disc().getBounds2D().getWidth(), 0.05,
                    "the 3 px symbol");
            assertTrue(m.symbol(), "a cartographic symbol");
            assertNull(m.state(), "a clear moon needs no word");
            assertFalse(m.selectable(), "not selectable (R-8)");
        }
        for (UranusService.Moon moon : List.of(UranusService.Moon.MIRANDA, UranusService.Moon.ARIEL)) {
            assertNull(find(drawn, id(moon)), moon + " stands on the mark at 1°: not drawn, no floor (R-5)");
        }
        assertTrue(said(minimum, r, ENGLISH).contains(
                " Oberon (a cartographic symbol, not Oberon's apparent diameter)."), said(minimum, r, ENGLISH));
        assertTrue(said(minimum, r, ENGLISH).contains(
                " Miranda, too close to tell apart at this field, not drawn."), said(minimum, r, ENGLISH));
        ChartScene wide = page(OPPOSITION_2026, 8.0);
        for (UranusService.Moon moon : UranusService.Moon.values()) {
            assertNull(find(placed(wide, r, ENGLISH), id(moon)), "at 8° no moon is drawn: " + moon);
        }
        assertTrue(said(wide, r, NORSK).contains(
                " Oberon, for nær til å skjelnes i dette feltet, ikke tegnet."), said(wide, r, NORSK));
    }

    @Test
    void aTransitIsNeverDrawnUranussMarkCannotHoldAMoon() {
        // Horizons codes Miranda t (in front of Uranus) from Oslo on 2005-05-28.
        Instant transit = Instant.parse("2005-05-28T00:00:00Z");
        assertEquals(UranusService.DiscRelation.IN_FRONT,
                URANUS.observeMoons(oslo(transit)).moon(UranusService.Moon.MIRANDA).discRelation());
        OverlayRegistry r = offering(transit);
        ChartScene closest = page(transit, 0.25);
        assertNull(find(placed(closest, r, ENGLISH), id(UranusService.Moon.MIRANDA)),
                "even at 0.25° the 6 px mark cannot hold it (R-6)");
        assertTrue(said(closest, r, ENGLISH).contains(
                " Miranda, too close to tell apart at this field, not drawn."), said(closest, r, ENGLISH));
    }

    @Test
    void aMoonBehindUranusIsNotDrawnButSpoken() {
        // Horizons codes Miranda U (behind, in the shadow) from Oslo on 2005-03-19.
        Instant behind = Instant.parse("2005-03-19T00:00:00Z");
        OverlayRegistry r = offering(behind);
        ChartScene closest = page(behind, 0.25);
        assertNull(find(placed(closest, r, ENGLISH), id(UranusService.Moon.MIRANDA)), "behind: never drawn");
        assertTrue(said(closest, r, ENGLISH).contains(" Miranda, behind Uranus, not drawn."));
        assertTrue(said(closest, r, NORSK).contains(" Miranda, bak Uranus, ikke tegnet."));
    }

    @Test
    void anEclipsedMoonStandsAtTheLimbAndIsNotDrawn() {
        // Horizons codes Ariel u (in the shadow) from Oslo on 2006-02-04.
        Instant eclipsed = Instant.parse("2006-02-04T00:00:00Z");
        assertEquals(UranusService.ShadowRelation.IN_SHADOW,
                URANUS.observeMoons(oslo(eclipsed)).moon(UranusService.Moon.ARIEL).shadowRelation());
        OverlayRegistry r = offering(eclipsed);
        ChartScene closest = page(eclipsed, 0.25);
        assertNull(find(placed(closest, r, ENGLISH), id(UranusService.Moon.ARIEL)),
                "beside the limb, not clear of the mark even at 0.25° (#637, section B)");
        assertTrue(said(closest, r, ENGLISH).contains(
                " Ariel, too close to tell apart at this field, not drawn."), said(closest, r, ENGLISH));
    }

    @Test
    void beforeTheMoonsYearsUranusIsDrawnAlone() {
        Instant earlier = Instant.parse("1999-12-31T12:00:00Z");
        OverlayRegistry r = offering(earlier);
        assertEquals(1, r.collect().size(), "Uranus alone, as the table shows it");
        assertTrue(r.collect().get(0).geometry() instanceof OverlayContribution.Body);
        assertTrue(ReferenceInk.unseenMoons(DrawnPage.of(page(earlier, 1.0)), r.collect()).isEmpty(),
                "and nothing said of moons with no numbers");
    }
}
