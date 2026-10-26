package juranometria.ui;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;
import juranometria.solar.MarsService;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phobos and Deimos on the page (issue #606 part 2, R-3 to R-5 on #605):
 * each a 3 px symbol at its own J2000 place, decided on its own with no
 * floor - drawn only where its mark stands clear of Mars's drawn mark and
 * of the other moon, on every field; in front only over a drawn disc of at
 * least 12 px; in the shadow a hollow ring; behind, or too close to tell
 * apart, not drawn but spoken; never selectable; outside 2000-2049 Mars
 * alone. The instants are found by the service itself, for Oslo, around
 * the 2018 perihelic opposition and, for a transit, in June 2001.
 */
class MarsMoonsInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final SolarSystemService SOLAR = SolarSystemService.load();
    private static final MarsService MARS = MarsService.load();
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;
    private static final String PHOBOS = SolarSystemModule.identityOf(MarsService.Moon.PHOBOS);
    private static final String DEIMOS = SolarSystemModule.identityOf(MarsService.Moon.DEIMOS);

    private static Observer oslo(Instant when) {
        return new Observer(OSLO_LAT, OSLO_LON, when);
    }

    private static OverlayRegistry offering(Instant when) {
        SolarSystemModule module = new SolarSystemModule(() -> oslo(when), () -> SOLAR,
                () -> false);
        module.withMars(() -> MARS);
        module.marsShowing(true);
        module.sunShowing(false);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        return registry;
    }

    private static ChartScene page(Instant when, double field) {
        return Atlas.assembler().assemble(new ChartViewState(
                MARS.observe(oslo(when)).astrometricJ2000(), field, 8.0, null, null), 900, 700);
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

    /** The first ten-minute step from {@code from} over {@code days} where the moon is as asked. */
    private static Instant first(Instant from, int days, MarsService.Moon moon,
                                 Predicate<MarsService.MoonPlace> asked) {
        for (Instant t = from; t.isBefore(from.plus(Duration.ofDays(days)));
                t = t.plus(Duration.ofMinutes(10))) {
            if (asked.test(MARS.observeMoons(oslo(t)).moon(moon))) {
                return t;
            }
        }
        throw new AssertionError("no such instant in the window");
    }

    private static final Instant OPPOSITION_2018 = Instant.parse("2018-07-27T06:00:00Z");

    @Test
    void deimosClearAtTheClosestFieldIsADotAndTooCloseAtEightDegreesIsSpoken() {
        // Deimos near its greatest elongation: 70″ and more from Mars's centre.
        Instant wide = first(OPPOSITION_2018, 3, MarsService.Moon.DEIMOS,
                m -> m.separationArcseconds() > 70.0
                        && m.discRelation() == MarsService.DiscRelation.CLEAR);
        OverlayRegistry r = offering(wide);
        ChartScene close = page(wide, 0.25);
        ReferenceInk.BodyPlacement deimos = find(placed(close, r, ENGLISH), DEIMOS);
        assertNotNull(deimos, "at 0.25° Deimos stands clear of Mars and is drawn");
        assertEquals(SolarSystemModule.MARS_MOON_MARK_PX,
                deimos.disc().getBounds2D().getWidth(), 0.05, "the 3 px symbol");
        assertTrue(deimos.symbol(), "a cartographic symbol");
        assertNull(deimos.state(), "a clear moon needs no word");
        assertFalse(deimos.selectable(), "not selectable (R-5)");
        assertTrue(said(close, r, ENGLISH).contains(
                " Deimos (a cartographic symbol, not Deimos's apparent diameter)."));
        ChartScene wideField = page(wide, 8.0);
        assertNull(find(placed(wideField, r, ENGLISH), DEIMOS),
                "at 8° Deimos stands on Mars's mark: not drawn - no floor (R-3)");
        assertTrue(said(wideField, r, ENGLISH).contains(
                " Deimos, too close to tell apart at this field, not drawn."),
                said(wideField, r, ENGLISH));
        assertTrue(said(wideField, r, NORSK).contains(
                " Deimos, for nær til å skjelnes i dette feltet, ikke tegnet."),
                said(wideField, r, NORSK));
    }

    @Test
    void phobosInFrontOfTheDrawnDiscIsRingedInTheGroundAndSaysSo() {
        // Mars's tilt keeps Phobos off the disc around the 2018 opposition;
        // in June 2001 it crosses a 20″ disc.
        Instant transit = first(Instant.parse("2001-06-30T12:00:00Z"), 1,
                MarsService.Moon.PHOBOS,
                m -> m.discRelation() == MarsService.DiscRelation.IN_FRONT
                        && m.shadowRelation() == MarsService.ShadowRelation.SUNLIT
                        && m.separationMarsRadii() < 0.8);
        OverlayRegistry r = offering(transit);
        ChartScene close = page(transit, 0.25);
        ReferenceInk.BodyPlacement mars = find(placed(close, r, ENGLISH), SolarSystemModule.MARS);
        assertTrue(mars.disc().getBounds2D().getWidth() >= 12.0,
                "Mars's true disc can hold a mark at 0.25° in 2001");
        ReferenceInk.BodyPlacement phobos = find(placed(close, r, ENGLISH), PHOBOS);
        assertNotNull(phobos, "in front of a drawn disc of at least 12 px it is drawn (R-4)");
        assertEquals("mars.inFront", phobos.state());
        assertTrue(said(close, r, ENGLISH).contains(" Phobos, in front of Mars"),
                said(close, r, ENGLISH));
        assertTrue(said(close, r, NORSK).contains(" Phobos, foran Mars"));
        ChartScene minimum = page(transit, ChartViewState.normalMinimumFieldDegrees());
        assertNull(find(placed(minimum, r, ENGLISH), PHOBOS),
                "at 1° Mars is a 6 px mark that cannot hold it: not drawn, no floor");
        assertTrue(said(minimum, r, ENGLISH).contains(
                " Phobos, too close to tell apart at this field, not drawn."));
    }

    @Test
    void phobosBehindMarsIsNotDrawnButSpoken() {
        Instant behind = first(OPPOSITION_2018, 2, MarsService.Moon.PHOBOS,
                m -> m.discRelation() == MarsService.DiscRelation.BEHIND);
        OverlayRegistry r = offering(behind);
        ChartScene close = page(behind, 0.25);
        assertNull(find(placed(close, r, ENGLISH), PHOBOS), "behind Mars: never drawn");
        assertTrue(said(close, r, ENGLISH).contains(" Phobos, behind Mars, not drawn."));
        assertTrue(said(close, r, NORSK).contains(" Phobos, bak Mars, ikke tegnet."));
    }

    @Test
    void aMoonInMarsShadowIsAHollowRingWithItsState() {
        // Before the opposition the shadow falls beside the disc: the first
        // step at which Phobos is wholly in it and clear of the disc by its
        // own diameter at 0.25° (1″ to the pixel there).
        Instant from = Instant.parse("2018-05-01T00:00:00Z");
        Instant shadowed = null;
        for (Instant t = from; t.isBefore(from.plus(Duration.ofDays(20)));
                t = t.plus(Duration.ofMinutes(10))) {
            MarsService.MoonPlace m = MARS.observeMoons(oslo(t)).moon(MarsService.Moon.PHOBOS);
            double radius = MARS.observe(oslo(t)).equatorialDiameterArcseconds() / 2.0;
            if (m.shadowRelation() == MarsService.ShadowRelation.IN_SHADOW
                    && m.discRelation() == MarsService.DiscRelation.CLEAR
                    && m.separationArcseconds() - radius > 6.0) {
                shadowed = t;
                break;
            }
        }
        assertNotNull(shadowed, "a shadowed Phobos clear of the disc in May 2018");
        OverlayRegistry r = offering(shadowed);
        ReferenceInk.BodyPlacement phobos = find(placed(page(shadowed, 0.25), r, ENGLISH), PHOBOS);
        assertNotNull(phobos, "clear of the disc at 0.25°, it is drawn");
        assertEquals("mars.shadowed", phobos.state());
        assertTrue(said(page(shadowed, 0.25), r, ENGLISH).contains(" Phobos, in Mars's shadow"));
    }

    @Test
    void outsideTheMoonsYearsMarsIsDrawnAlone() {
        Instant later = Instant.parse("2051-03-01T00:00:00Z");
        OverlayRegistry r = offering(later);
        assertEquals(1, r.collect().size(), "Mars alone, as the table shows it");
        assertTrue(r.collect().get(0).geometry() instanceof OverlayContribution.Body);
        assertTrue(ReferenceInk.unseenMoons(DrawnPage.of(page(later, 1.0)), r.collect()).isEmpty(),
                "and nothing said of moons with no numbers");
    }
}
