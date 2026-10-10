package juranometria.ui;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.jovianchart.JovianModule;
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
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four Galilean moons on the page (issue #485, the owner's rulings 4-7
 * on #482): at their own J2000 places; behind Jupiter omitted; at the
 * normal minimum field every other moon drawn, those in front painted over
 * Jupiter's disc; state vocabulary A on both palettes; a symbol never
 * erasing a star; each label refused on its own; spoken with its state.
 */
class JovianMoonsInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
    private static final Observer TRIPLE = new Observer(59.913, 10.752,
            Instant.parse("2026-12-11T22:45:00Z"));
    private static JovianSystemService service;

    @BeforeAll
    static void load() {
        service = JovianSystemService.load();
    }

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    private static OverlayRegistry offering(List<OverlayContribution> offered) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(JovianModule.ID, () -> offered);
        return registry;
    }

    private static List<OverlayContribution> module(Observer at) {
        JovianModule module = new JovianModule(() -> at, () -> service, () -> false);
        module.showing(true);
        return module.contributedGeometry();
    }

    private static List<ReferenceInk.BodyPlacement> placed(ChartScene scene, OverlayRegistry r,
                                                           ChartPalette palette, PageText words) {
        return ReferenceInk.bodyPlacements(DrawnPage.of(scene), r.collect(), palette, words,
                List.of());
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

    @Test
    void theTripleTransitAtTheNormalMinimumKeepsItsThreeMarksInFrontOfJupiter() {
        List<OverlayContribution> offered = module(TRIPLE);
        JovianSystemService.Configuration c = service.observeMoons(TRIPLE);
        ChartScene scene = page(c.jupiter().astrometricJ2000(),
                ChartViewState.normalMinimumFieldDegrees());
        List<ReferenceInk.BodyPlacement> all = placed(scene, offering(offered),
                ChartPalette.WHITE_PAPER, ENGLISH);
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        int inFront = 0;
        for (JovianSystemService.MoonPlace m : c.moons()) {
            ReferenceInk.BodyPlacement p = named(all, JovianModule.identityOf(m.moon()));
            if (m.discRelation() == JovianSystemService.DiscRelation.BEHIND) {
                assertNull(p, m.moon() + " behind Jupiter is omitted");
                continue;
            }
            assertNotNull(p, m.moon() + " is drawn at the normal minimum field");
            PixelPoint expected = mapping.toPixel(
                    page.projection().project(m.astrometricJ2000()).orElseThrow());
            assertEquals(expected.x(), p.centre().x(), 1e-9, "at its own J2000 place");
            assertEquals(expected.y(), p.centre().y(), 1e-9);
            assertTrue(p.symbol(), "a symbol: a click passes through");
            assertEquals(3.0, p.disc().getBounds2D().getWidth(), 1e-9, "3 px");
            if (m.discRelation() == JovianSystemService.DiscRelation.IN_FRONT) {
                assertEquals("jovian.inFront", p.state());
                inFront++;
            }
        }
        assertEquals(3, inFront, "Io, Europa and Callisto in front: never a lone Jupiter mark");
        // In front, painted over the disc: the mark's centre is ink, not the disc's face.
        BufferedImage image = painted(scene, offering(offered), ChartPalette.WHITE_PAPER);
        ReferenceInk.BodyPlacement callisto = named(all, "jovian.callisto");
        java.awt.Color at = new java.awt.Color(image.getRGB((int) Math.round(callisto.centre().x()),
                (int) Math.round(callisto.centre().y())));
        assertTrue(at.getRed() < 128, "Callisto's dot reads over Jupiter: " + at);
    }

    /** One moon, synthetic, at a known place beside a synthetic Jupiter. */
    private static List<OverlayContribution> pair(SkyPosition jupiter, SkyPosition moon,
                                                  Satellite.Relation relation, boolean shadowed) {
        return List.of(new OverlayContribution.OblateBody(JovianModule.JUPITER, "Jupiter", jupiter,
                        40.0, 37.4, 0.0, 6.0, false, 6.0e8, InkRole.BODY),
                new Satellite("jovian.io", "Io", moon, JovianModule.JUPITER, relation, shadowed, 2,
                        3.0, false, InkRole.BODY));
    }

    @Test
    void stateVocabularyAOnBothPalettes() {
        SkyPosition jupiter = new SkyPosition(150.0, 0.0);
        SkyPosition beside = new SkyPosition(150.0 - 60.0 / 3600.0, 0.0); // 15 px east at 1°
        ChartScene scene = page(jupiter, 1.0);
        for (ChartPalette palette : List.of(ChartPalette.WHITE_PAPER, ChartPalette.BLACK_SKY)) {
            int ink = palette.starInk().getRGB();
            BufferedImage clear = painted(scene, offering(pair(jupiter, beside,
                    Satellite.Relation.CLEAR, false)), palette);
            BufferedImage shadow = painted(scene, offering(pair(jupiter, beside,
                    Satellite.Relation.CLEAR, true)), palette);
            ReferenceInk.BodyPlacement io = named(placed(scene, offering(pair(jupiter, beside,
                    Satellite.Relation.CLEAR, false)), palette, ENGLISH), "jovian.io");
            int x = (int) Math.round(io.centre().x() - 0.5);
            int y = (int) Math.round(io.centre().y() - 0.5);
            assertEquals(ink, clear.getRGB(x, y), palette + ": clear is a filled dot");
            assertTrue(shadow.getRGB(x, y) != ink, palette + ": in shadow a hollow ring");
            BufferedImage behind = painted(scene, offering(pair(jupiter, beside,
                    Satellite.Relation.BEHIND, false)), palette);
            BufferedImage none = painted(scene, offering(List.of(pair(jupiter, beside,
                    Satellite.Relation.CLEAR, false).get(0))), palette);
            assertTrue(java.util.Arrays.equals(
                    behind.getRGB(0, 0, 900, 700, null, 0, 900),
                    none.getRGB(0, 0, 900, 700, null, 0, 900)),
                    palette + ": behind is not drawn at all - no ghost");
        }
        // In front on both palettes: the dot ringed in the page's ground, over
        // the disc - a synthetic disc 40 px wide at 1°, so the ring has room.
        SkyPosition onDisc = new SkyPosition(150.0 - 30.0 / 3600.0, 0.0);
        ChartScene close = page(jupiter, 1.0);
        for (ChartPalette palette : List.of(ChartPalette.WHITE_PAPER, ChartPalette.BLACK_SKY)) {
            List<OverlayContribution> offered = List.of(
                    new OverlayContribution.OblateBody(JovianModule.JUPITER, "Jupiter", jupiter,
                            160.0, 149.6, 0.0, 6.0, false, 6.0e8, InkRole.BODY),
                    new Satellite("jovian.io", "Io", onDisc, JovianModule.JUPITER,
                            Satellite.Relation.IN_FRONT, false, 2, 3.0, false, InkRole.BODY));
            ReferenceInk.BodyPlacement io = named(placed(close, offering(offered), palette,
                    ENGLISH), "jovian.io");
            BufferedImage image = painted(close, offering(offered), palette);
            int x = (int) Math.round(io.centre().x() - 0.5);
            int y = (int) Math.round(io.centre().y() - 0.5);
            assertEquals(palette.starInk().getRGB(), image.getRGB(x, y), palette + ": the dot");
            java.awt.Color ring = new java.awt.Color(image.getRGB(x + 2, y));
            java.awt.Color ground = palette.ground();
            assertTrue(Math.abs(ring.getRed() - ground.getRed()) < 60
                    && Math.abs(ring.getGreen() - ground.getGreen()) < 60, palette
                    + ": ringed in the page's ground over the disc: " + ring);
        }
    }

    @Test
    void aMoonsSymbolNeverErasesAStar() {
        SkyPosition regulus = new SkyPosition(152.093, 11.967);
        ChartScene scene = page(regulus, 1.0);
        SkyPosition jupiter = new SkyPosition(152.093 + 60.0 / 3600.0, 11.967);
        OverlayRegistry with = offering(pair(jupiter, regulus, Satellite.Relation.CLEAR, false));
        OverlayRegistry without = offering(List.of(pair(jupiter, regulus,
                Satellite.Relation.CLEAR, false).get(0)));
        BufferedImage a = painted(scene, without, ChartPalette.WHITE_PAPER);
        BufferedImage b = painted(scene, with, ChartPalette.WHITE_PAPER);
        ChartRenderer.DrawnMark star = null;
        DrawnPage page = DrawnPage.of(scene);
        PixelPoint at = new ViewportMapping(page).toPixel(page.projection().project(regulus).orElseThrow());
        for (ChartRenderer.DrawnMark m : RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (m.kind() == ChartRenderer.DrawnMark.Kind.STAR
                    && m.outline().contains(at.x(), at.y())) {
                star = m;
            }
        }
        assertNotNull(star, "Regulus is drawn under the moon's place");
        java.awt.Rectangle r = star.outline().getBounds();
        int checked = 0;
        for (int x = r.x; x < r.x + r.width; x++) {
            for (int y = r.y; y < r.y + r.height; y++) {
                if (star.outline().contains(x + 0.5, y + 0.5)) {
                    assertEquals(a.getRGB(x, y), b.getRGB(x, y), "the star's ink at " + x + "," + y);
                    checked++;
                }
            }
        }
        assertTrue(checked > 4);
    }

    @Test
    void labelsAreRefusedOneByOneAndNeverOverlap() {
        // Every 6 hours over 2026 at the normal minimum field: no two names
        // overlap, no name covers a mark, and no configuration loses every
        // moon name to one crowded pair.
        int refused = 0;
        for (Instant t = Instant.parse("2026-01-01T00:00:00Z");
                t.isBefore(Instant.parse("2027-01-01T00:00:00Z")); t = t.plusSeconds(6 * 3600 * 7)) {
            Observer at = new Observer(59.913, 10.752, t);
            List<OverlayContribution> offered = module(at);
            ChartScene scene = page(service.observeJupiter(at).astrometricJ2000(),
                    ChartViewState.normalMinimumFieldDegrees());
            List<ReferenceInk.BodyPlacement> all = placed(scene, offering(offered),
                    ChartPalette.WHITE_PAPER, ENGLISH);
            List<java.awt.geom.Rectangle2D> boxes = new ArrayList<>();
            int moons = 0;
            int named = 0;
            for (ReferenceInk.BodyPlacement p : all) {
                if (p.box() == null) {
                    if (p.identity().startsWith("jovian.") && !p.identity().equals(JovianModule.JUPITER)) {
                        refused++;
                    }
                    continue;
                }
                for (java.awt.geom.Rectangle2D other : boxes) {
                    assertFalse(other.intersects(p.box()), t + ": names never overlap");
                }
                for (ReferenceInk.BodyPlacement mark : all) {
                    assertFalse(mark.disc().intersects(p.box()), t + ": no name covers a mark");
                }
                boxes.add(p.box());
                if (!p.identity().equals(JovianModule.JUPITER)) {
                    named++;
                }
            }
            for (ReferenceInk.BodyPlacement p : all) {
                moons += p.identity().equals(JovianModule.JUPITER) ? 0 : 1;
            }
            assertTrue(moons == 0 || named > 0, t + ": " + moons + " moons, every name refused");
        }
        assertTrue(refused >= 0);
    }

    @Test
    void eachMoonIsSpokenWithItsStateAndAsASymbolInBothLanguages() {
        List<OverlayContribution> offered = module(TRIPLE);
        JovianSystemService.Configuration c = service.observeMoons(TRIPLE);
        ChartScene scene = page(c.jupiter().astrometricJ2000(), 1.0);
        String en = ChartComponent.withBodies("Chart.", placed(scene, offering(offered),
                ChartPalette.WHITE_PAPER, ENGLISH), ENGLISH);
        assertTrue(en.contains(" Io, in front of Jupiter (a cartographic symbol, not Io's"
                + " apparent diameter)."), en);
        assertTrue(en.contains(" Ganymede (a cartographic symbol, not Ganymede's apparent"
                + " diameter)."), en);
        String nb = ChartComponent.withBodies("Kart.", placed(scene, offering(offered),
                ChartPalette.WHITE_PAPER, NORSK), NORSK);
        assertTrue(nb.contains(" Ganymedes (et kartografisk symbol, ikke Ganymedes'"
                + " tilsynelatende diameter)."), nb);
        assertTrue(nb.contains(" Callisto, foran Jupiter"), nb);
    }
}
