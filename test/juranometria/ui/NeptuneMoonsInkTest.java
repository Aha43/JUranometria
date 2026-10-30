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
import juranometria.solar.NeptuneService;
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
 * Triton and Nereid on the page (issue #659 part 2, R-5, R-6a, R-7 and R-9
 * on #658): each a 3 px symbol at its own J2000 place, offered only at the
 * chart's normal minimum field and the closer fields, and there decided on
 * its own by Mars's clearance rule - drawn only where its mark stands clear
 * of Neptune's drawn mark and of the other moon; on a wider field neither
 * drawn nor spoken, so Nereid is never a lone dot among far brighter stars;
 * a transit never drawn, since Neptune's mark cannot hold a moon; behind,
 * or too close to tell apart, not drawn but spoken; never selectable;
 * before 2000 Neptune alone. The instants are Espenak's 2026 opposition
 * hour and Horizons' coded Oslo rows (docs/studies/neptune-system/).
 */
class NeptuneMoonsInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final SolarSystemService SOLAR = SolarSystemService.load();
    private static final NeptuneService NEPTUNE = NeptuneService.load();
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;
    /** Espenak's 2026 opposition hour (neptune-events.txt). */
    private static final Instant OPPOSITION_2026 = Instant.parse("2026-09-26T00:00:00Z");

    private static String id(NeptuneService.Moon moon) {
        return SolarSystemModule.identityOf(moon);
    }

    private static Observer oslo(Instant when) {
        return new Observer(OSLO_LAT, OSLO_LON, when);
    }

    private static OverlayRegistry offering(Instant when) {
        SolarSystemModule module = new SolarSystemModule(() -> oslo(when), () -> SOLAR,
                () -> false);
        module.withNeptune(() -> NEPTUNE);
        module.neptuneShowing(true);
        module.sunShowing(false);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        return registry;
    }

    private static ChartScene page(Instant when, double field) {
        return Atlas.assembler().assemble(new ChartViewState(
                NEPTUNE.observe(oslo(when)).astrometricJ2000(), field, 8.0, null, null), 900, 700);
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
    void atOneDegreeNereidStandsClearAndTritonOnTheMarkIsSpoken() {
        OverlayRegistry r = offering(OPPOSITION_2026);
        ChartScene minimum = page(OPPOSITION_2026, ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> drawn = placed(minimum, r, ENGLISH);
        ReferenceInk.BodyPlacement nereid = find(drawn, id(NeptuneService.Moon.NEREID));
        assertNotNull(nereid, "Nereid stands clear of Neptune's mark at 1° on the 2026 opposition");
        assertEquals(SolarSystemModule.NEPTUNE_MOON_MARK_PX, nereid.disc().getBounds2D().getWidth(), 0.05,
                "the 3 px symbol");
        assertTrue(nereid.symbol(), "a cartographic symbol");
        assertNull(nereid.state(), "a clear moon needs no word");
        assertFalse(nereid.selectable(), "not selectable (R-9)");
        assertNull(find(drawn, id(NeptuneService.Moon.TRITON)),
                "Triton never clears the mark at 1° (#658, section B): not drawn");
        String english = said(minimum, r, ENGLISH);
        assertTrue(english.contains(" Nereid (a cartographic symbol, not Nereid's apparent diameter)."), english);
        assertTrue(english.contains(" Triton, too close to tell apart at this field, not drawn."), english);
        assertTrue(said(minimum, r, NORSK).contains(" Triton, for nær til å skjelnes i dette feltet, ikke tegnet."));
    }

    @Test
    void onAWiderFieldTheMoonsAreNeitherDrawnNorSpoken() {
        // R-6a: offered only at 1° and the closer fields. At 8° Nereid would stand
        // clear by the geometry alone, 14 px out; it is not offered.
        OverlayRegistry r = offering(OPPOSITION_2026);
        for (double field : new double[] {2.0, 8.0}) {
            ChartScene wide = page(OPPOSITION_2026, field);
            List<ReferenceInk.BodyPlacement> drawn = placed(wide, r, ENGLISH);
            for (NeptuneService.Moon moon : NeptuneService.Moon.values()) {
                assertNull(find(drawn, id(moon)), "at " + field + "° no moon is drawn: " + moon);
            }
            assertTrue(ReferenceInk.unseenMoons(DrawnPage.of(wide), r.collect()).isEmpty(),
                    "and none is spoken at " + field + "°");
            String english = said(wide, r, ENGLISH);
            assertFalse(english.contains("Nereid") || english.contains("Triton"), english);
            String norsk = said(wide, r, NORSK);
            assertFalse(norsk.contains("Nereid") || norsk.contains("Triton"), norsk);
        }
        // The module still offers them: the page's field decides, never the module.
        assertEquals(3, r.collect().size(), "Neptune and both moons offered to every page");
    }

    @Test
    void atTheClosestFieldBothMoonsAreDrawnWhereTheyStandClear() {
        OverlayRegistry r = offering(OPPOSITION_2026);
        ChartScene closest = page(OPPOSITION_2026, 0.25);
        List<ReferenceInk.BodyPlacement> drawn = placed(closest, r, ENGLISH);
        for (NeptuneService.Moon moon : NeptuneService.Moon.values()) {
            ReferenceInk.BodyPlacement m = find(drawn, id(moon));
            assertNotNull(m, moon + " stands clear at 0.25° on the 2026 opposition");
            assertFalse(m.selectable());
        }
    }

    @Test
    void aTransitIsNeverDrawnNeptunesMarkCannotHoldAMoon() {
        // Horizons codes Triton t (in front of Neptune) from Oslo on 2043-09-07.
        Instant transit = Instant.parse("2043-09-07T00:00:00Z");
        assertEquals(NeptuneService.DiscRelation.IN_FRONT,
                NEPTUNE.observeMoons(oslo(transit)).moon(NeptuneService.Moon.TRITON).discRelation());
        OverlayRegistry r = offering(transit);
        ChartScene closest = page(transit, 0.25);
        assertNull(find(placed(closest, r, ENGLISH), id(NeptuneService.Moon.TRITON)),
                "even at 0.25° the 6 px mark cannot hold it (R-7)");
        assertTrue(said(closest, r, ENGLISH).contains(
                " Triton, too close to tell apart at this field, not drawn."), said(closest, r, ENGLISH));
    }

    @Test
    void aMoonBehindNeptuneIsNotDrawnButSpoken() {
        // Horizons codes Triton U (behind, in the shadow) from Oslo on 2044-09-26.
        Instant behind = Instant.parse("2044-09-26T00:00:00Z");
        OverlayRegistry r = offering(behind);
        ChartScene closest = page(behind, 0.25);
        assertNull(find(placed(closest, r, ENGLISH), id(NeptuneService.Moon.TRITON)), "behind: never drawn");
        assertTrue(said(closest, r, ENGLISH).contains(" Triton, behind Neptune, not drawn."));
        assertTrue(said(closest, r, NORSK).contains(" Triton, bak Neptun, ikke tegnet."));
    }

    @Test
    void beforeTheMoonsYearsNeptuneIsDrawnAlone() {
        Instant earlier = Instant.parse("1999-12-31T12:00:00Z");
        OverlayRegistry r = offering(earlier);
        assertEquals(1, r.collect().size(), "Neptune alone, as the table shows it");
        assertTrue(r.collect().get(0).geometry() instanceof OverlayContribution.Body);
        assertTrue(ReferenceInk.unseenMoons(DrawnPage.of(page(earlier, 1.0)), r.collect()).isEmpty(),
                "and nothing said of moons with no numbers");
    }
}
