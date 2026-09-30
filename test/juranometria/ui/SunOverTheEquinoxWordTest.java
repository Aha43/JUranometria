package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.CardinalLandmark;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.tool.SeasonalEventsFixture;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The owner's #415 checkpoint finding, on its own page: the IMCCE's
 * 2026 September equinox, a 6° page centred near RA 12h, Dec 0°, the
 * ecliptic drawn. Switching the Sun on hid part of the September
 * equinox landmark's word under the opaque disc; switching it off
 * restored the word.
 *
 * <p>The repair holds the disc as an obstacle for annotation, never
 * as permission to paint over it: the Sun keeps its exact position
 * and true size, the landmark's word takes a legal alternate box
 * clear of the disc's ink, and the Sun's own name tries its
 * alternatives after that and yields first.
 */
class SunOverTheEquinoxWordTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final ChartRenderer RENDERER = new ChartRenderer(
            StarSizePolicy.DEFAULT, ENGLISH);
    private static final SolarSystemService SERVICE = SolarSystemService.load();
    private static final String WORD = "September equinox";

    private static Instant equinox() throws Exception {
        return SeasonalEventsFixture.read().get("september-equinox").instant();
    }

    /** The owner's page: 6°, centred at RA 12h, Dec 0°. */
    private static ChartScene page() {
        return Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(180.0, 0.0), 6.0, 8.0, null, null), 900, 700);
    }

    private static OverlayRegistry registry(Observer observer, boolean sun) {
        OverlayRegistry registry = new OverlayRegistry();
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);
        SolarSystemModule solar = new SolarSystemModule(() -> observer,
                () -> SERVICE, () -> false);
        solar.sunShowing(sun);
        registry.offer(SolarSystemModule.ID, solar::contributedGeometry);
        return registry;
    }

    /** One rendered page, and the reserved ink the renderer handed its layers. */
    private record Rendered(BufferedImage image, List<Shape> reserved) {
    }

    private static Rendered render(ChartScene scene, OverlayRegistry registry,
                                   boolean paintBodies) {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        List<Shape> reserved = new ArrayList<>();
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, ChartOptions.DEFAULTS,
                    (layerG, painted, given) -> {
                        reserved.addAll(given);
                        ReferenceInk.paint(layerG, DrawnPage.of(painted),
                                registry.collect(), ChartPalette.WHITE_PAPER,
                                ENGLISH, given);
                    },
                    paintBodies
                            ? (layerG, painted, given) -> ReferenceInk.paintBodies(
                                    layerG, DrawnPage.of(painted), registry.collect(),
                                    ChartPalette.WHITE_PAPER, ENGLISH, given,
                                    ReferenceInk.referenceBoxes(DrawnPage.of(painted),
                                            registry.collect(), ENGLISH, given,
                                            structure -> false))
                            : ChartRenderer.ReferenceLayer.NONE,
                    null, java.util.Set.of());
        } finally {
            g.dispose();
        }
        return new Rendered(image, List.copyOf(reserved));
    }

    private static Rectangle2D wordBox(ChartScene scene, OverlayRegistry registry,
                                       List<Shape> reserved) {
        for (ReferenceInk.NamePlacement placed : ReferenceInk.pointWordPlacements(
                DrawnPage.of(scene), registry.collect(), ENGLISH, reserved)) {
            if (placed.name().equals(WORD)) {
                return placed.box();
            }
        }
        return null;
    }

    /** The disc's ink: its limb, half the ring's stroke and a pixel of antialiasing. */
    private static Shape inkOf(ReferenceInk.BodyPlacement body) {
        Rectangle2D disc = body.disc().getBounds2D();
        double r = disc.getWidth() / 2.0;
        double grown = r + Math.max(1.0, r / 12.0) / 2.0 + 1.0;
        return new Ellipse2D.Double(disc.getCenterX() - grown,
                disc.getCenterY() - grown, 2.0 * grown, 2.0 * grown);
    }

    @Test
    void theEquinoxWordStepsClearOfTheSunAndTheSunStaysExactly() throws Exception {
        Observer oslo = new Observer(59.913, 10.752, equinox());
        ChartScene scene = page();
        DrawnPage page = DrawnPage.of(scene);

        // The finding's page, as released: the word where it always was.
        OverlayRegistry without = registry(oslo, false);
        Rendered bare = render(scene, without, true);
        Rectangle2D released = wordBox(scene, without, bare.reserved());
        assertNotNull(released, "the landmark's word is written without the Sun");

        OverlayRegistry with = registry(oslo, true);
        Rendered lit = render(scene, with, true);
        List<ReferenceInk.BodyPlacement> bodies = ReferenceInk.bodyPlacements(page,
                with.collect(), ChartPalette.WHITE_PAPER, ENGLISH, lit.reserved());
        assertEquals(1, bodies.size());
        ReferenceInk.BodyPlacement sun = bodies.get(0);
        Shape ink = inkOf(sun);
        assertTrue(ink.intersects(released),
                "this is the owner's page: the released word lies under the disc");

        // The Sun is exactly where it was and exactly as large.
        SunObservation observed = (SunObservation) SERVICE.observe(Body.SUN, oslo);
        ViewportMapping mapping = new ViewportMapping(page);
        PixelPoint centre = mapping.toPixel(
                page.projection().project(observed.astrometricJ2000()).orElseThrow());
        assertEquals(centre.x(), sun.centre().x(), 1e-9);
        assertEquals(centre.y(), sun.centre().y(), 1e-9);
        SkyPosition limb = new SkyPosition(observed.astrometricJ2000().raDegrees(),
                observed.astrometricJ2000().decDegrees()
                        + observed.angularDiameterArcseconds() / 3600.0 / 2.0);
        PixelPoint edge = mapping.toPixel(page.projection().project(limb).orElseThrow());
        double radius = Math.hypot(edge.x() - centre.x(), edge.y() - centre.y());
        assertEquals(2.0 * radius, sun.disc().getBounds2D().getWidth(), 1e-9,
                "true scale, unchanged by the repair");

        // The landmark's word is kept, beside its own mark, clear of the disc.
        Rectangle2D moved = wordBox(scene, with, lit.reserved());
        assertNotNull(moved, "the equinox word is preserved, not omitted");
        assertFalse(ink.intersects(moved), "the word is clear of the disc's ink: " + moved);
        PixelPoint mark = mapping.toPixel(page.projection().project(
                new SkyPosition(180.0, 0.0)).orElseThrow());
        double reach = 2.0 * CardinalLandmark.GAP + moved.getWidth() + moved.getHeight();
        assertTrue(Math.hypot(moved.getCenterX() - mark.x(),
                        moved.getCenterY() - mark.y()) <= reach,
                "a legal alternate beside its own diamond at " + mark + ": " + moved);

        // The Sun's name yields: wherever it went, it is clear of the word,
        // of every reference box and of its own disc - or it is not written.
        if (sun.box() != null) {
            assertFalse(sun.box().intersects(moved), "the Sun's name is not on the word");
            assertFalse(ink.intersects(sun.box()), "the name is beside the disc");
            for (Rectangle2D box : ReferenceInk.referenceBoxes(page, with.collect(),
                    ENGLISH, lit.reserved(), structure -> false)) {
                assertFalse(sun.box().intersects(box), "no fused label: " + box);
            }
        }

        // Nothing the bodies layer paints lands on the word: the same page
        // without that layer differs from it only outside the word's box.
        Rendered under = render(scene, with, false);
        for (int y = (int) Math.floor(moved.getMinY()); y < Math.ceil(moved.getMaxY()); y++) {
            for (int x = (int) Math.floor(moved.getMinX()); x < Math.ceil(moved.getMaxX()); x++) {
                assertEquals(under.image().getRGB(x, y), lit.image().getRGB(x, y),
                        "the bodies layer painted over the word at (" + x + ", " + y + ")");
            }
        }

        // Switched off again, the word is back in its released box.
        assertEquals(released, wordBox(scene, registry(oslo, false), bare.reserved()));
    }
}
