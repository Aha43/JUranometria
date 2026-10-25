package juranometria.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.Cardinal;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.placeandtime.PlaceStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Subdue below horizon, held on production pages (Sprint 55, issue
 * #615; the rulings and mutation cases in
 * {@code docs/decisions/below-horizon.md}). The pages are the
 * production composition - the component with the meridian and Solar
 * System modules attached - at the gallery instant, Oslo 2026-03-20
 * 21:33 UTC, the horizon across the south at 24°, painted with the
 * switch on and off and compared pixel by pixel.
 */
class BelowHorizonTest {

    private static final Observer OSLO =
            new Observer(59.913, 10.752, Instant.parse("2026-03-20T21:33:00Z"));
    private static final LocalSky SKY = new LocalSky(OSLO);
    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    /** A painted page, and the component that painted it. */
    private record Page(BufferedImage image, ChartComponent chart, MeridianModule meridian) {
    }

    private static Page paint(ChartPalette palette, boolean subdue, boolean line,
                              ChartViewState state) throws Exception {
        Page[] out = new Page[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartComponent chart = new ChartComponent(Atlas.assembler(),
                    PageText.in(InterfaceText.forLanguage("en")));
            chart.setSize(900, 700);
            chart.setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            chart.setViewState(state);
            MeridianModule meridian = new MeridianModule(OSLO);
            meridian.showing(true, line, false);
            meridian.subduing(subdue);
            chart.overlays().offer(MeridianModule.ID, meridian::contributedGeometry);
            SolarSystemModule solar = new SolarSystemModule(() -> OSLO, () -> service,
                    meridian::subduing);
            solar.sunShowing(true);
            chart.overlays().offer(SolarSystemModule.ID, solar::contributedGeometry);
            chart.setSubdueBelowHorizon(() -> meridian.subduing() ? SKY.zenith() : null);
            out[0] = new Page(null, chart, meridian);
        });
        SwingUtilities.invokeAndWait(() -> { });
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                out[0].chart().paint(g);
            } finally {
                g.dispose();
            }
        });
        return new Page(image, out[0].chart(), out[0].meridian());
    }

    private static ChartViewState south(double field) {
        return new ChartViewState(SKY.cardinal(Cardinal.SOUTH), field, 8.0, null, null);
    }

    @Test
    void theRuleIsTheSkysAltitude() {
        BelowHorizon rule = BelowHorizon.of(SKY.zenith(), null);
        assertFalse(rule.active(), "without a page there is nothing to subdue");
        Random random = new Random(615);
        ChartScene scene = Atlas.assembler().assemble(south(24.0), 900, 700);
        BelowHorizon onPage = BelowHorizon.of(SKY.zenith(), scene);
        assertTrue(onPage.active());
        int checked = 0;
        for (int i = 0; i < 2000; i++) {
            SkyPosition p = new SkyPosition(random.nextDouble() * 360.0,
                    Math.toDegrees(Math.asin(2.0 * random.nextDouble() - 1.0)));
            double altitude = SKY.altitudeDegrees(p);
            if (Math.abs(altitude) < 1e-6) {
                continue;
            }
            assertEquals(altitude < 0.0, onPage.below(p), "at " + p + ", altitude " + altitude);
            checked++;
        }
        assertTrue(checked > 1990);
        assertFalse(BelowHorizon.NONE.below(new SkyPosition(0, -89)), "NONE: nothing is below");
    }

    @Test
    void theRuledStrengthKeepsFortyPerCentOfTheInk() {
        assertEquals(0.40, BelowHorizon.KEPT);
        Color subdued = BelowHorizon.subdued(Color.WHITE, Color.BLACK);
        assertEquals(new Color(102, 102, 102), subdued, "255 x 0.40 = 102");
        assertEquals(new Color(153, 153, 153), BelowHorizon.subdued(Color.BLACK, Color.WHITE));
    }

    @Test
    void theRegionIsTheSideOfTheDrawnHorizon() throws Exception {
        Page on = paint(ChartPalette.BLACK_SKY, true, true, south(24.0));
        ChartScene scene = on.chart().currentScene();
        BelowHorizon rule = on.chart().belowHorizon(scene);
        java.awt.Shape below = rule.belowRegion();
        DrawnPage page = DrawnPage.of(scene);
        int sampled = 0;
        for (int y = 5; y < 700; y += 7) {
            for (int x = 5; x < 900; x += 11) {
                SkyPosition at = ChartHitTest.skyAt(page, x + 0.5, y + 0.5);
                if (at == null || Math.abs(SKY.altitudeDegrees(at)) < 0.05) {
                    continue;
                }
                assertEquals(SKY.altitudeDegrees(at) < 0.0, below.contains(x + 0.5, y + 0.5),
                        "pixel " + x + "," + y);
                sampled++;
            }
        }
        assertTrue(sampled > 5000, "sampled " + sampled);
    }

    @Test
    void starsBelowAreSubduedWholeAndStarsAboveAreUntouched() throws Exception {
        Page on = paint(ChartPalette.BLACK_SKY, true, true, south(24.0));
        Page off = paint(ChartPalette.BLACK_SKY, false, true, south(24.0));
        ChartScene scene = on.chart().currentScene();
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        int dx = on.chart().pageOffsetX();
        int dy = on.chart().pageOffsetY();
        Color ground = ChartPalette.BLACK_SKY.ground();
        int below = 0;
        int above = 0;
        for (Star star : scene.stars()) {
            PixelPoint p = page.projection().project(star.position())
                    .map(mapping::toPixel).orElse(null);
            if (p == null) {
                continue;
            }
            int x = (int) Math.round(p.x()) + dx;
            int y = (int) Math.round(p.y()) + dy;
            if (x < 3 || y < 3 || x > 896 || y > 696) {
                continue;
            }
            double altitude = SKY.altitudeDegrees(star.position());
            if (Math.abs(altitude) < 0.5) {
                continue;
            }
            Color was = new Color(off.image().getRGB(x, y));
            Color now = new Color(on.image().getRGB(x, y));
            if (!was.equals(ChartPalette.BLACK_SKY.starInk())) {
                continue; // a centre pixel that is not the star's own ink: furniture, a label
            }
            if (altitude < 0.0) {
                assertEquals(ground.getRed() + (was.getRed() - ground.getRed()) * 0.40,
                        now.getRed(), 1.0, star.id() + " at " + x + "," + y
                                + " below keeps 40 % of its ink, opaque: nothing compounds");
                below++;
            } else {
                assertEquals(was, now, star.id() + " above is untouched");
                above++;
            }
        }
        assertTrue(below > 50 && above > 50, "below " + below + ", above " + above);
    }

    @Test
    void theTitleBlockTheHorizonsDirectionsAndThePlacementsAreUntouched() throws Exception {
        Page on = paint(ChartPalette.BLACK_SKY, true, true, south(24.0));
        Page off = paint(ChartPalette.BLACK_SKY, false, true, south(24.0));
        // The box's interior: the box is opaque over the sky it stands on.
        Rectangle block = new Rectangle(16, 634, 408, 52);
        for (int y = block.y; y < block.y + block.height; y++) {
            for (int x = block.x; x < block.x + block.width; x++) {
                assertEquals(off.image().getRGB(x, y), on.image().getRGB(x, y),
                        "the title block at " + x + "," + y);
            }
        }
        List<ReferenceInk.BodyPlacement> bodiesOn = on.chart().renderedBodies();
        List<ReferenceInk.BodyPlacement> bodiesOff = off.chart().renderedBodies();
        assertEquals(bodiesOff.size(), bodiesOn.size());
        for (int i = 0; i < bodiesOn.size(); i++) {
            assertEquals(bodiesOff.get(i).box(), bodiesOn.get(i).box(),
                    "a body's name is placed where it was");
            assertEquals(bodiesOff.get(i).centre(), bodiesOn.get(i).centre());
        }
    }

    @Test
    void theSunIsSubduedOnceByTheCommonRuleWithOrWithoutTheLine() throws Exception {
        SolarSystemService.SunObservation sun = (SolarSystemService.SunObservation)
                service.observe(SolarSystemService.Body.SUN, OSLO);
        assertTrue(sun.horizontal().altitudeDegrees() < 0.0, "the premise: the Sun is down");
        ChartViewState onTheSun = new ChartViewState(sun.astrometricJ2000(), 3.0, 8.0,
                null, null);
        Page subduedNoLine = paint(ChartPalette.BLACK_SKY, true, false, onTheSun);
        Page full = paint(ChartPalette.BLACK_SKY, false, true, onTheSun);
        ReferenceInk.BodyPlacement placedSun = subduedNoLine.chart().renderedBodies().get(0);
        assertTrue(placedSun.belowHorizon(), "subdued with the line off (P-I)");
        assertFalse(full.chart().renderedBodies().get(0).belowHorizon(),
                "and not with the switch off, line or no line");
        // The centre dot: the star ink, at the ruled strength, once.
        int x = (int) Math.round(placedSun.centre().x()) + subduedNoLine.chart().pageOffsetX();
        int y = (int) Math.round(placedSun.centre().y()) + subduedNoLine.chart().pageOffsetY();
        Color expected = BelowHorizon.subdued(ChartPalette.BLACK_SKY.starInk(),
                ChartPalette.BLACK_SKY.ground());
        Color dot = new Color(subduedNoLine.image().getRGB(x, y));
        assertEquals(expected.getRed(), dot.getRed(), 3, "the dot at 40 %, not 40 % of 40 %");
    }

    @Test
    void theSwitchIsRememberedAndOnForAFreshProfile() throws Exception {
        Preferences node = Preferences.userRoot().node("juranometria-test-subdue-" + System.nanoTime());
        try {
            PlaceStore store = PlaceStore.forNode(node);
            assertTrue(store.subdueBelowHorizon().isEmpty(), "a fresh profile has not chosen");
            store.saveSubdueBelowHorizon(false);
            assertEquals(java.util.Optional.of(false), PlaceStore.forNode(node).subdueBelowHorizon());
            MeridianModule module = new MeridianModule(OSLO);
            assertTrue(module.subduing(), "on by default (P-D)");
        } finally {
            node.removeNode();
        }
    }
}
