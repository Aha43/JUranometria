package juranometria.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PageBasis;
import juranometria.project.PixelPoint;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.CompassPoint;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.tool.MoonEventsFixture;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws the Moon (Sprint 37, issue #416, under the #414
 * ruling): a true-scale phased disc whose lit part has the area the
 * table's k states and faces the bright limb the table's χ states,
 * turned through the page's own north and east at the Moon; opaque,
 * its dark side inked; in front of the Sun; dimmed below a drawn
 * horizon; named in the page's language.
 */
class MoonInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final ChartRenderer RENDERER = new ChartRenderer(
            StarSizePolicy.DEFAULT, ENGLISH);
    private static final SolarSystemService SERVICE = SolarSystemService.load();
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;
    /** Placeholder distances: only their order is read. */
    private static final double FAR = 2.0;
    private static final double NEAR = 1.0;

    private static DrawnPage page(SkyPosition centre, double field) {
        return DrawnPage.of(Atlas.assembler().assemble(
                new ChartViewState(centre, field, 8.0, null, null), 900, 700));
    }

    /** The area of a shape in pixels, and its centroid, by rasterising it. */
    private static double[] areaAndCentroid(Shape shape, int size) {
        BufferedImage mask = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = mask.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fill(shape);
        } finally {
            g.dispose();
        }
        double n = 0.0;
        double sx = 0.0;
        double sy = 0.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if ((mask.getRGB(x, y) & 0xFFFFFF) != 0) {
                    n++;
                    sx += x + 0.5;
                    sy += y + 0.5;
                }
            }
        }
        return new double[] {n, sx / n, sy / n};
    }

    /**
     * The position angle the drawn lit side faces: the lit part's
     * centroid for a crescent, and for a gibbous disc - whose lit
     * centroid barely leaves the centre - the opposite of the small
     * dark part's.
     */
    private static double facing(PageBasis basis, PixelPoint centre, double r,
                                 Area lit, double[] litMeasured) {
        double[] at = litMeasured;
        double sign = 1.0;
        Area dark = new Area(new java.awt.geom.Ellipse2D.Double(
                centre.x() - r, centre.y() - r, 2 * r, 2 * r));
        dark.subtract(lit);
        double[] darkMeasured = areaAndCentroid(dark, 1000);
        if (darkMeasured[0] < litMeasured[0]) {
            at = darkMeasured;
            sign = -1.0;
        }
        return basis.positionAngleDegrees(sign * (at[1] - centre.x()),
                sign * (at[2] - centre.y()));
    }

    @Test
    void theLitAreaIsTheIlluminatedFractionAndFacesTheBrightLimb() {
        // A large disc on a skewed corner of a 36° page at +45°, so the
        // page's north and east are neither up nor perpendicular.
        DrawnPage page = page(new SkyPosition(90.0, 45.0), 36.0);
        SkyPosition at = page.projection().unproject(
                new juranometria.project.PlanePoint(0.2, 0.15)).orElseThrow();
        PageBasis basis = PageBasis.at(page, at).orElseThrow();
        PixelPoint centre = new PixelPoint(500.0, 500.0);
        PageBasis there = new PageBasis(centre, basis.northX(), basis.northY(),
                basis.eastX(), basis.eastY());
        double r = 400.0;
        double disc = areaAndCentroid(new java.awt.geom.Ellipse2D.Double(
                centre.x() - r, centre.y() - r, 2 * r, 2 * r), 1000)[0];
        for (double i = 10.0; i <= 170.0; i += 20.0) {
            double k = (1.0 + Math.cos(Math.toRadians(i))) / 2.0;
            for (double chi = 0.0; chi < 360.0; chi += 45.0) {
                Area lit = ReferenceInk.litRegion(centre, r, there,
                        new OverlayContribution.Lit(k, chi, i));
                double[] measured = areaAndCentroid(lit, 1000);
                assertEquals(k, measured[0] / disc, 0.004,
                        "the lit area is k at i = " + i + "°");
                double facing = facing(there, centre, r, lit, measured);
                double miss = Math.abs(((facing - chi) % 360.0 + 540.0) % 360.0 - 180.0);
                assertTrue(miss < 0.5, "the lit side faces χ = " + chi + "° at i = " + i
                        + "°, drawn towards " + facing + "°");
            }
        }
    }

    /**
     * The four phases of the June 2026 lunation, as named by the
     * fixture: the drawn Moon's lit side points where the Moon table's
     * compass point says, and is as large as its k.
     */
    @Test
    void theJuneLunationIsDrawnAsTheTableStatesIt() throws Exception {
        Map<String, MoonEventsFixture.Event> events = MoonEventsFixture.read();
        for (String name : List.of("new-moon-june", "first-quarter-june",
                "full-moon-june", "last-quarter-july")) {
            Observer oslo = new Observer(OSLO_LAT, OSLO_LON, events.get(name).instant());
            MoonObservation moon = (MoonObservation) SERVICE.observe(Body.MOON, oslo);
            OverlayRegistry registry = registry(oslo, false, true);
            DrawnPage page = page(moon.astrometricJ2000(), 3.0);
            ReferenceInk.BodyPlacement drawn = ReferenceInk.bodyPlacements(page,
                    registry.collect(), ChartPalette.WHITE_PAPER, ENGLISH, List.of()).get(0);
            double r = drawn.disc().getBounds2D().getWidth() / 2.0;
            PageBasis basis = PageBasis.at(page, moon.astrometricJ2000()).orElseThrow();
            Area lit = ReferenceInk.litRegion(drawn.centre(), r, basis,
                    new OverlayContribution.Lit(moon.illuminatedFraction(),
                            moon.brightLimbAngleDegrees(), moon.phaseAngleDegrees()));
            double[] measured = areaAndCentroid(lit, 1000);
            double disc = Math.PI * r * r;
            assertEquals(moon.illuminatedFraction(), measured[0] / disc, 0.01,
                    name + ": the lit area is the table's k");
            if (moon.brightLimbConditioning()
                    == SolarSystemService.LimbConditioning.WELL_DEFINED) {
                double facing = facing(basis, drawn.centre(), r, lit, measured);
                assertEquals(moon.brightLimbCompassPoint(), CompassPoint.of(facing),
                        name + ": the drawn bright limb faces the table's compass point");
            }
        }
    }

    @Test
    void theMoonIsOpaqueItsDarkSideInkedAndInFrontOfTheSun() {
        SkyPosition at = new SkyPosition(2.0, 1.0);
        ChartScene scene = Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(0.0, 0.0), 24.0, 8.0, null, null), 900, 700);
        // A new Moon exactly over the Sun: the nearer body covers the farther.
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("solar-system", () -> List.of(
                new OverlayContribution.Body("sun", "Sun", at, 32.0 * 60.0, null,
                        false, FAR, InkRole.BODY),
                new OverlayContribution.Body("moon", "Moon", at, 33.0 * 60.0,
                        new OverlayContribution.Lit(0.0, 0.0, 180.0), false, NEAR,
                        InkRole.BODY)));
        BufferedImage image = paint(scene, registry, ChartPalette.WHITE_PAPER);
        List<ReferenceInk.BodyPlacement> drawn = ReferenceInk.bodyPlacements(
                DrawnPage.of(scene), registry.collect(), ChartPalette.WHITE_PAPER,
                ENGLISH, List.of());
        ReferenceInk.BodyPlacement moon = drawn.get(1);
        assertEquals("moon", moon.identity(), "painted last, as the nearer");
        int cx = (int) Math.round(moon.centre().x());
        int cy = (int) Math.round(moon.centre().y());
        Color centre = new Color(image.getRGB(cx, cy));
        int darkSide = (int) Math.round(255 * ReferenceInk.DARK_SIDE);
        assertEquals(darkSide, centre.getRed(), 2,
                "the Sun's centre dot is hidden under the new Moon's inked dark side: "
                        + centre);
    }

    @Test
    void belowADrawnHorizonItIsDimmedAndNamedInTheLanguage() {
        SkyPosition at = new SkyPosition(2.0, 1.0);
        ChartScene scene = Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(0.0, 0.0), 24.0, 8.0, null, null), 900, 700);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("solar-system", () -> List.of(
                new OverlayContribution.Body("moon", "Moon", at, 33.0 * 60.0,
                        new OverlayContribution.Lit(0.0, 0.0, 180.0), true, NEAR,
                        InkRole.BODY)));
        ReferenceInk.BodyPlacement en = ReferenceInk.bodyPlacements(DrawnPage.of(scene),
                registry.collect(), ChartPalette.WHITE_PAPER, ENGLISH, List.of()).get(0);
        assertEquals("Moon (below the horizon)", en.name());
        ReferenceInk.BodyPlacement nb = ReferenceInk.bodyPlacements(DrawnPage.of(scene),
                registry.collect(), ChartPalette.WHITE_PAPER, NORSK, List.of()).get(0);
        assertEquals("Månen (under horisonten)", nb.name());
        BufferedImage image = paint(scene, registry, ChartPalette.WHITE_PAPER);
        Color centre = new Color(image.getRGB((int) Math.round(en.centre().x()),
                (int) Math.round(en.centre().y())));
        int darkSide = (int) Math.round(255 * ReferenceInk.DARK_SIDE);
        assertTrue(centre.getRed() > darkSide + 20,
                "the dark side is dimmed towards the ground: " + centre);
    }

    @Test
    void onTheBlackSkyTheLitSideIsTheStarInk() throws Exception {
        MoonEventsFixture.Event full = MoonEventsFixture.read().get("full-moon-june");
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, full.instant());
        MoonObservation moon = (MoonObservation) SERVICE.observe(Body.MOON, oslo);
        OverlayRegistry registry = registry(oslo, false, true);
        ChartScene scene = Atlas.assembler().assemble(new ChartViewState(
                moon.astrometricJ2000(), 3.0, 8.0, null, null), 900, 700);
        BufferedImage image = paint(scene, registry, ChartPalette.BLACK_SKY);
        ReferenceInk.BodyPlacement drawn = ReferenceInk.bodyPlacements(
                DrawnPage.of(scene), registry.collect(), ChartPalette.BLACK_SKY,
                ENGLISH, List.of()).get(0);
        Color centre = new Color(image.getRGB((int) Math.round(drawn.centre().x()),
                (int) Math.round(drawn.centre().y())));
        assertEquals(ChartPalette.BLACK_SKY.starInk().getRGB(), centre.getRGB(),
                "a full Moon on the black sky is lit in the star ink");
    }

    private static OverlayRegistry registry(Observer observer, boolean sun, boolean moon) {
        SolarSystemModule module = new SolarSystemModule(() -> observer, () -> SERVICE,
                () -> false);
        module.sunShowing(sun);
        module.moonShowing(moon);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(SolarSystemModule.ID, module::contributedGeometry);
        return registry;
    }

    private static BufferedImage paint(ChartScene scene, OverlayRegistry registry,
                                       ChartPalette palette) {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, ChartOptions.DEFAULTS.withPalette(palette),
                    ChartRenderer.ReferenceLayer.NONE,
                    (layerG, painted, reserved) -> ReferenceInk.paintBodies(layerG,
                            DrawnPage.of(painted), registry.collect(), palette,
                            ENGLISH, reserved, List.of()),
                    null, java.util.Set.of());
        } finally {
            g.dispose();
        }
        return image;
    }
}
