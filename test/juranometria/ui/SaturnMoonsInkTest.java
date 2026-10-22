package juranometria.ui;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayContribution.Satellite;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.saturnchart.SaturnModule;
import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.Configuration;
import juranometria.solar.SaturnSystemService.DiscRelation;
import juranometria.solar.SaturnSystemService.MoonPlace;
import juranometria.solar.SaturnSystemService.ShadowRelation;
import juranometria.solar.SaturnSystemService.VisibilityState;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seven Saturnian moons on the page (issue #579, ruling 3 on #578):
 * each a 3 px symbol at its own J2000 place, decided on its own against
 * Saturn's drawn figure - the rings' ellipse, not a disc - with the
 * table's states spoken; a moon in front painted over the figure;
 * labels attempted for every drawn moon and refused one by one.
 */
class SaturnMoonsInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
    /** The contract's card instant (#574) at Oslo. */
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-10-05T00:00:00Z"));
    private static final List<Double> RINGS = List.of(74491.0, 117570.0, 122050.0, 136770.0);
    private static final double RE = 60268.0;
    private static final double RP = 54364.0;
    /** The card's distance (#574): 8.434258 AU. */
    private static final double FAR_KM = 8.434258 * 149_597_870.7;
    private static SaturnSystemService service;

    @BeforeAll
    static void load() {
        service = SaturnSystemService.load();
    }

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    private static List<OverlayContribution> module(Observer at) {
        SaturnModule module = new SaturnModule(() -> at, () -> service, () -> false);
        module.showing(true);
        return module.contributedGeometry();
    }

    private static OverlayRegistry offering(List<OverlayContribution> offered) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SaturnModule.ID, () -> offered);
        return registry;
    }

    private static List<ReferenceInk.BodyPlacement> placed(ChartScene scene, OverlayRegistry r,
                                                           PageText words) {
        return ReferenceInk.bodyPlacements(DrawnPage.of(scene), r.collect(),
                ChartPalette.WHITE_PAPER, words, List.of());
    }

    private static BufferedImage painted(ChartScene scene, OverlayRegistry registry,
                                         ChartPalette palette) {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, ChartOptions.DEFAULTS.withPalette(palette),
                    (layerG, s, reserved) -> ReferenceInk.paint(layerG, s, registry.collect(),
                            palette, ENGLISH, reserved),
                    (layerG, s, reserved) -> ReferenceInk.paintBodies(layerG,
                            DrawnPage.of(s), registry.collect(), palette, ENGLISH, reserved,
                            List.of()),
                    null, java.util.Set.of());
        } finally {
            g.dispose();
        }
        return image;
    }

    private static ReferenceInk.BodyPlacement named(List<ReferenceInk.BodyPlacement> all,
                                                    String identity) {
        return all.stream().filter(p -> p.identity().equals(identity)).findFirst().orElse(null);
    }

    /** The first hour at Oslo from July 2026 with a moon in the state, found rather than typed. */
    private static Observer firstWith(VisibilityState state) {
        for (Instant t = Instant.parse("2026-07-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plusSeconds(3600)) {
            Configuration c = service.observeMoons(OSLO.at(t));
            for (MoonPlace m : c.moons()) {
                if (m.state() == state) {
                    return OSLO.at(t);
                }
            }
        }
        throw new IllegalStateException("no 2026 hour at Oslo with a moon " + state);
    }

    private static String stateOf(MoonPlace m) {
        return m.discRelation() == DiscRelation.IN_FRONT ? "saturn.inFront"
                : m.shadowRelation() == ShadowRelation.PARTLY_IN_SHADOW ? "saturn.partlyShadowed"
                : m.shadowRelation() == ShadowRelation.IN_SHADOW ? "saturn.shadowed" : null;
    }

    @Test
    void atTheNormalMinimumEveryMoonNotBehindIsASymbolAtItsOwnPlaceWithItsState() {
        List<OverlayContribution> offered = module(OSLO);
        Configuration c = service.observeMoons(OSLO);
        ChartScene scene = page(c.saturn().astrometricJ2000(),
                ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> all = placed(scene, offering(offered), ENGLISH);
        assertEquals(SaturnModule.SATURN, all.get(0).identity(), "Saturn first");
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        int drawn = 0;
        for (MoonPlace m : c.moons()) {
            ReferenceInk.BodyPlacement p = named(all, SaturnModule.identityOf(m.moon()));
            if (m.discRelation() == DiscRelation.BEHIND) {
                assertNull(p, m.moon() + " behind Saturn is omitted");
                continue;
            }
            assertNotNull(p, m.moon() + " is drawn at the normal minimum field");
            PixelPoint expected = mapping.toPixel(
                    page.projection().project(m.astrometricJ2000()).orElseThrow());
            assertEquals(expected.x(), p.centre().x(), 1e-9, "at its own J2000 place");
            assertEquals(expected.y(), p.centre().y(), 1e-9);
            assertTrue(p.symbol(), "a symbol: a click passes through");
            assertEquals(3.0, p.disc().getBounds2D().getWidth(), 1e-9, "3 px");
            assertEquals(stateOf(m), p.state(), "the table's state");
            assertFalse(p.selectable(), "not selectable (ruling 5)");
            drawn++;
        }
        assertTrue(drawn >= 5, "most of the seven are not behind Saturn at once: " + drawn);
    }

    @Test
    void aMoonInFrontIsPaintedOverTheFigure() {
        Observer at = firstWith(VisibilityState.IN_FRONT_OF_SATURN);
        List<OverlayContribution> offered = module(at);
        Configuration c = service.observeMoons(at);
        ChartScene scene = page(c.saturn().astrometricJ2000(),
                ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> all = placed(scene, offering(offered), ENGLISH);
        BufferedImage image = painted(scene, offering(offered), ChartPalette.WHITE_PAPER);
        int inFront = 0;
        for (MoonPlace m : c.moons()) {
            if (m.discRelation() != DiscRelation.IN_FRONT) {
                continue;
            }
            ReferenceInk.BodyPlacement p = named(all, SaturnModule.identityOf(m.moon()));
            assertNotNull(p, m.moon() + " in front is drawn at the floor");
            assertEquals("saturn.inFront", p.state());
            java.awt.Color pixel = new java.awt.Color(image.getRGB(
                    (int) Math.round(p.centre().x()), (int) Math.round(p.centre().y())));
            assertTrue(pixel.getRed() < 128, m.moon() + "'s dot reads over the figure: " + pixel);
            inFront++;
        }
        assertTrue(inFront >= 1, "the found hour has a moon in front");
    }

    /** Saturn at a distance with its true figure from the radii, at a ring opening; the pole page-up. */
    private static OverlayContribution.RingedBody saturn(SkyPosition at, double distanceKm,
                                                         double openingB) {
        double eq = 2.0 * Math.toDegrees(Math.asin(RE / distanceKm)) * 3600.0;
        double b = Math.toRadians(openingB);
        double semiPolar = Math.sqrt(RP * RP * Math.cos(b) * Math.cos(b)
                + RE * RE * Math.sin(b) * Math.sin(b));
        double pol = 2.0 * Math.toDegrees(Math.asin(semiPolar / distanceKm)) * 3600.0;
        return new OverlayContribution.RingedBody("saturn", "Saturn", at, eq, pol, 0.0,
                openingB, false, RINGS, 6.0, false, distanceKm, InkRole.BODY, null, false);
    }

    private static Satellite moon(String id, SkyPosition at, Satellite.Relation relation) {
        return new Satellite("saturn." + id, id, at, "saturn", relation, false, 0, 3.0, false,
                InkRole.BODY);
    }

    @Test
    void aboveTheFloorAMoonIsMeasuredAgainstTheRingsEllipseNotADisc() {
        // Ten times nearer than the card, open 25°, on a 2° page: the rings
        // about 56 px across (a = 28) and 24 px high (b = 11.8). Two
        // synthetic moons 0.0667° (30 px) from the centre - one along the
        // major axis, 2 px from the rings' edge and so within the 6 px
        // clearance, one along the minor axis, 18 px clear of it. A disc
        // 56 px wide would have refused both.
        SkyPosition at = new SkyPosition(150.0, 0.0);
        ChartScene scene = page(at, 2.0);
        OverlayContribution.RingedBody figure = saturn(at, FAR_KM / 10.0, -25.0);
        double off = 30.0 * 2.0 / 900.0;
        List<OverlayContribution> offered = List.of(figure,
                moon("titan", new SkyPosition(150.0 + off, 0.0), Satellite.Relation.CLEAR),
                moon("rhea", new SkyPosition(150.0, off), Satellite.Relation.CLEAR));
        OverlayRegistry registry = offering(offered);
        DrawnPage page = DrawnPage.of(scene);
        ReferenceInk.RingedMark mark = ReferenceInk.ringedOn(page, figure);
        assertFalse(mark.symbol());
        assertTrue(mark.outline().getBounds2D().getWidth() > 50.0, "the figure is wide here");
        Map<String, String> decided = ReferenceInk.satelliteDecisions(page, registry.collect());
        assertEquals("AT_PRIMARY", decided.get("saturn.titan"),
                "along the major axis the mark is at the rings: " + decided);
        assertEquals("DRAWN", decided.get("saturn.rhea"),
                "along the minor axis the mark stands clear of the figure: " + decided);
        List<ReferenceInk.BodyPlacement> all = placed(scene, registry, ENGLISH);
        assertNull(named(all, "saturn.titan"));
        assertNotNull(named(all, "saturn.rhea"));
        // In front above the floor needs the globe to hold the mark: the
        // globe is about 23 px here, so a transit resolves; at the card's
        // distance (2 px) it does not.
        Satellite transit = moon("dione", at, Satellite.Relation.IN_FRONT);
        assertEquals("DRAWN", ReferenceInk.satelliteDecisions(page,
                offering(List.of(figure, transit)).collect()).get("saturn.dione"));
        assertEquals("IN_FRONT_UNRESOLVED", ReferenceInk.satelliteDecisions(page,
                offering(List.of(saturn(at, FAR_KM, -25.0), transit)).collect())
                .get("saturn.dione"));
    }

    @Test
    void labelsAreAttemptedForEveryDrawnMoonAndRefusedOneByOne() {
        // Weekly over 2026 at the normal minimum field: no two names
        // overlap, no name covers a mark, and no configuration loses every
        // moon name - a refused label never blocks another (ruling 3).
        int refused = 0;
        int named = 0;
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plusSeconds(7 * 86400)) {
            Observer at = OSLO.at(t);
            List<OverlayContribution> offered = module(at);
            ChartScene scene = page(service.observeSaturn(at).astrometricJ2000(),
                    ChartViewState.normalMinimumFieldDegrees());
            List<ReferenceInk.BodyPlacement> all = placed(scene, offering(offered), ENGLISH);
            List<java.awt.geom.Rectangle2D> boxes = new ArrayList<>();
            int moons = 0;
            int namedHere = 0;
            for (ReferenceInk.BodyPlacement p : all) {
                boolean moon = !p.identity().equals(SaturnModule.SATURN);
                moons += moon ? 1 : 0;
                if (p.box() == null) {
                    refused += moon ? 1 : 0;
                    continue;
                }
                for (java.awt.geom.Rectangle2D other : boxes) {
                    assertFalse(other.intersects(p.box()), t + ": names never overlap");
                }
                for (ReferenceInk.BodyPlacement mark : all) {
                    assertFalse(mark.disc().intersects(p.box()), t + ": no name covers a mark");
                }
                boxes.add(p.box());
                namedHere += moon ? 1 : 0;
            }
            assertTrue(moons == 0 || namedHere > 0, t + ": " + moons + " moons, every name refused");
            named += namedHere;
        }
        assertTrue(named > 0 && refused > 0,
                "seven names crowd a figure a dozen pixels wide: some placed, some refused - "
                        + named + " placed, " + refused + " refused");
    }

    @Test
    void eachMoonIsSpokenWithTheTablesStateWordsInBothLanguages() {
        Observer at = firstWith(VisibilityState.IN_FRONT_OF_SATURN);
        List<OverlayContribution> offered = module(at);
        Configuration c = service.observeMoons(at);
        ChartScene scene = page(c.saturn().astrometricJ2000(), 1.0);
        String en = ChartComponent.withBodies("Chart.", placed(scene, offering(offered), ENGLISH),
                ENGLISH);
        String nb = ChartComponent.withBodies("Kart.", placed(scene, offering(offered), NORSK),
                NORSK);
        MoonPlace front = c.moons().stream()
                .filter(m -> m.discRelation() == DiscRelation.IN_FRONT).findFirst().orElseThrow();
        String name = SaturnModule.nameOf(front.moon());
        assertTrue(en.contains(" " + name + ", in front of Saturn (a cartographic symbol, not "
                + name + "'s apparent diameter)."), en);
        assertTrue(nb.contains(" " + name + ", foran Saturn (et kartografisk symbol, ikke "
                + name), nb);
        MoonPlace clear = c.moons().stream()
                .filter(m -> m.state() == VisibilityState.CLEAR_OF_SATURN).findFirst().orElseThrow();
        String clearName = SaturnModule.nameOf(clear.moon());
        assertTrue(en.contains(" " + clearName + " (a cartographic symbol, not " + clearName
                + "'s apparent diameter)."), "a clear moon needs no state word: " + en);
        // Partly in shadow keeps the table's distinction (ruling 4).
        Observer partly = firstWith(VisibilityState.PARTLY_IN_SATURNS_SHADOW);
        Configuration cp = service.observeMoons(partly);
        String enPartly = ChartComponent.withBodies("Chart.", placed(
                page(cp.saturn().astrometricJ2000(), 1.0), offering(module(partly)), ENGLISH),
                ENGLISH);
        MoonPlace shaded = cp.moons().stream()
                .filter(m -> m.state() == VisibilityState.PARTLY_IN_SATURNS_SHADOW).findFirst()
                .orElseThrow();
        assertTrue(enPartly.contains(" " + SaturnModule.nameOf(shaded.moon())
                + ", partly in Saturn's shadow ("), enPartly);
    }
}
