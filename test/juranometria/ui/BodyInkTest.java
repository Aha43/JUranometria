package juranometria.ui;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

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
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws a Solar System body (Sprint 37, issue #415, under
 * the #414 ruling): at true scale where the projection puts it, opaque,
 * a ring with its centre dot for the Sun, dimmed below the horizon with
 * the language's status, named by an adjacent box or not at all, and
 * absent when off the page.
 */
class BodyInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final ChartRenderer RENDERER = new ChartRenderer(
            StarSizePolicy.DEFAULT, ENGLISH);
    /** The March-equinox page: 24°, 900 × 700, the Sun's disc about 20 px. */
    private static final ChartScene EQUINOX = Atlas.assembler().assemble(
            new ChartViewState(new SkyPosition(0.0, 0.0), 24.0, 8.0, null, null),
            900, 700);
    private static final double SUN_DIAMETER = 32.0 * 60.0; // 32′, nominal
    /**
     * Placeholder distances, not measurements: only their order is read,
     * to paint the farther body first.
     */
    private static final double FAR = 2.0;
    private static final double NEAR = 1.0;

    private static OverlayRegistry offering(OverlayContribution... bodies) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("solar-system", () -> List.of(bodies));
        return registry;
    }

    private static OverlayContribution.Body sun(SkyPosition at, boolean below) {
        return new OverlayContribution.Body("sun", "Sun", at, SUN_DIAMETER, null,
                below, FAR, InkRole.BODY);
    }

    private static List<ReferenceInk.BodyPlacement> placed(ChartScene scene,
                                                           OverlayRegistry registry,
                                                           PageText words) {
        return ReferenceInk.bodyPlacements(DrawnPage.of(scene), registry.collect(),
                ChartPalette.WHITE_PAPER, words, List.of());
    }

    @Test
    void theDiscSitsWhereTheProjectionPutsItAtTrueScale() {
        SkyPosition at = new SkyPosition(2.0, 1.0);
        List<ReferenceInk.BodyPlacement> bodies = placed(EQUINOX, offering(sun(at, false)),
                ENGLISH);
        assertEquals(1, bodies.size());
        ReferenceInk.BodyPlacement sun = bodies.get(0);
        DrawnPage page = DrawnPage.of(EQUINOX);
        PixelPoint expected = new ViewportMapping(page).toPixel(
                page.projection().project(at).orElseThrow());
        assertEquals(expected.x(), sun.centre().x(), 1e-9);
        assertEquals(expected.y(), sun.centre().y(), 1e-9);
        double diameterPx = sun.disc().getBounds2D().getWidth();
        // 32′ on a 24° field 900 px wide is 20 px, and the gnomonic
        // page stretches it by a per cent or so this close to the centre.
        assertTrue(diameterPx > 19.5 && diameterPx < 21.0, "true scale: " + diameterPx);
        assertEquals("Sun", sun.name());
        assertNotNull(sun.box(), "the name found an adjacent box");
        assertFalse(sun.box().intersects(sun.disc().getBounds2D()),
                "beside the disc, not over it");
        assertTrue(sun.covers(sun.centre().x(), sun.centre().y()));
        assertFalse(sun.covers(sun.centre().x() + 30, sun.centre().y()));
    }

    @Test
    void theDiscIsOpaqueAndTheSunIsARingWithItsDot() {
        SkyPosition at = new SkyPosition(2.0, 1.0);
        OverlayRegistry registry = offering(sun(at, false));
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, EQUINOX, ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                    (layerG, scene, reserved) -> ReferenceInk.paintBodies(layerG,
                            DrawnPage.of(scene), registry.collect(), ChartPalette.WHITE_PAPER,
                            ENGLISH, reserved, List.of()),
                    null, java.util.Set.of());
        } finally {
            g.dispose();
        }
        ReferenceInk.BodyPlacement sun = placed(EQUINOX, registry, ENGLISH).get(0);
        int cx = (int) Math.round(sun.centre().x());
        int cy = (int) Math.round(sun.centre().y());
        double r = sun.disc().getBounds2D().getWidth() / 2.0;
        assertEquals(Color.BLACK.getRGB(), image.getRGB(cx, cy), "the centre dot");
        assertEquals(Color.WHITE.getRGB(), image.getRGB(cx, cy - (int) Math.round(r * 0.6)),
                "inside the ring the disc is the ground: opaque");
        Color limb = new Color(image.getRGB(cx, cy - (int) Math.round(r)));
        assertTrue(limb.getRed() < 128, "the ring on the limb, antialiased but dark: " + limb);
    }

    @Test
    void belowTheHorizonItIsDimmedAndSaysSoInTheLanguage() {
        SkyPosition at = new SkyPosition(2.0, 1.0);
        ReferenceInk.BodyPlacement en = placed(EQUINOX, offering(sun(at, true)), ENGLISH).get(0);
        assertTrue(en.belowHorizon());
        assertEquals("Sun (below the horizon)", en.name());
        ReferenceInk.BodyPlacement nb = placed(EQUINOX, offering(sun(at, true)), NORSK).get(0);
        assertEquals("Solen (under horisonten)", nb.name());
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        OverlayRegistry registry = offering(sun(at, true));
        java.awt.Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, EQUINOX, ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                    (layerG, scene, reserved) -> ReferenceInk.paintBodies(layerG,
                            DrawnPage.of(scene), registry.collect(), ChartPalette.WHITE_PAPER,
                            ENGLISH, reserved, List.of()),
                    null, java.util.Set.of());
        } finally {
            g.dispose();
        }
        int rgb = image.getRGB((int) Math.round(en.centre().x()),
                (int) Math.round(en.centre().y()));
        Color dot = new Color(rgb);
        assertTrue(dot.getRed() > 150 && dot.getRed() < 220,
                "the dot is dimmed towards the ground, not black: " + dot);
    }

    @Test
    void offThePageNothingIsDrawnAndNoHintIsLeft() {
        SkyPosition off = new SkyPosition(40.0, 1.0); // 40° east of a 24° page
        assertEquals(List.of(), placed(EQUINOX, offering(sun(off, false)), ENGLISH));
    }

    @Test
    void aNameWithNoCleanBoxIsRefusedAndTheMarkStays() {
        // Two bodies on top of each other: the second's every adjacent
        // box is taken by the first's name or disc; its mark is drawn,
        // its name is not.
        SkyPosition at = new SkyPosition(2.0, 1.0);
        OverlayContribution.Body other = new OverlayContribution.Body("moon", "Moon",
                at, SUN_DIAMETER, null, false, NEAR, InkRole.BODY);
        List<ReferenceInk.BodyPlacement> bodies = placed(EQUINOX, offering(other,
                sun(at, false)), ENGLISH);
        assertEquals(2, bodies.size());
        assertEquals("sun", bodies.get(0).identity(), "painted farthest first");
        ReferenceInk.BodyPlacement nearer = bodies.get(1);
        assertEquals("moon", nearer.identity());
        assertNotNull(nearer.box(), "the nearer body is named first");
        ReferenceInk.BodyPlacement farther = bodies.get(0);
        assertEquals(nearer.centre(), farther.centre(), "the mark never moves");
        if (farther.box() != null) {
            assertFalse(farther.box().intersects(nearer.box()),
                    "a placed name never overwrites another");
        }
    }

    @Test
    void theNameNeverOverwritesTheEquinoxLandmarksWord() {
        // The Sun at the March equinox sits on the ecliptic's own
        // landmark. The landmark's word is the reference layer's; the
        // Sun's name must find another box or none (#414 ruling C4).
        juranometria.ecliptic.EclipticModule ecliptic = new juranometria.ecliptic.EclipticModule();
        ecliptic.showing(true);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("ecliptic", ecliptic::contributedGeometry);
        SkyPosition equinox = new SkyPosition(0.0, 0.0);
        registry.offer("solar-system", () -> List.of(sun(equinox, false)));
        DrawnPage page = DrawnPage.of(EQUINOX);
        List<java.awt.geom.Rectangle2D> referenceBoxes = ReferenceInk.referenceBoxes(page,
                registry.collect(), ENGLISH, List.of(), structure -> false);
        assertFalse(referenceBoxes.isEmpty(), "the landmark's word took a box");
        ReferenceInk.BodyPlacement sun = placed(EQUINOX, registry, ENGLISH).get(0);
        if (sun.box() != null) {
            for (java.awt.geom.Rectangle2D box : referenceBoxes) {
                assertFalse(sun.box().intersects(box),
                        "the Sun's name stays clear of every reference word");
            }
        }
        assertEquals(equinox, sun(equinox, false).at(), "and the mark stays put");
    }

    @Test
    void theContributionRefusesWhatItCannotDraw() {
        SkyPosition at = new SkyPosition(2.0, 1.0);
        assertThrows(IllegalArgumentException.class, () -> new OverlayContribution.Body(
                "sun", "Sun", at, 0.0, null, false, FAR, InkRole.BODY));
        assertThrows(IllegalArgumentException.class, () -> new OverlayContribution.Body(
                "sun", "Sun", at, SUN_DIAMETER, null, false, FAR, InkRole.REFERENCE_LINE));
        assertThrows(IllegalArgumentException.class, () -> new OverlayContribution.Body(
                "sun", "Sun", at, SUN_DIAMETER, null, false, 0.0, InkRole.BODY));
        assertThrows(IllegalArgumentException.class, () -> new OverlayContribution.Lit(
                0.5, 90.0, 190.0));
        assertThrows(IllegalArgumentException.class, () -> new OverlayContribution.Lit(
                1.5, 0.0, 0.0));
        assertNull(sun(at, false).lit());
    }
}
