package juranometria.ui;

import java.awt.Shape;
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
import juranometria.project.PageBasis;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.ChartRenderer.DrawnMark;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws Jupiter (issue #484, the owner's rulings on #482):
 * the larger of its true disc and a 6 px minimum; while the minimum
 * stands in for it, a cartographic symbol painted beneath the stars,
 * never erasing one, that a click passes through and that the page
 * calls a symbol; once the true disc reaches the minimum, the body
 * itself, opaque over what it covers; its outline turning continuously
 * from a circle to the true axis ratio, the minor axis along the pole
 * through the page's own north and east; dimmed below a drawn horizon;
 * absent off the page.
 */
class JupiterInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
    private static final SkyPosition REGULUS = new SkyPosition(152.093, 11.967);
    private static final double MIN = 6.0;

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    private static OverlayContribution.OblateBody jupiter(SkyPosition at, double equatorial,
                                                          double polar, double pole,
                                                          boolean below) {
        return new OverlayContribution.OblateBody("jovian.jupiter", "Jupiter", at, equatorial, polar,
                pole, MIN, below, 6.0e8, InkRole.BODY);
    }

    private static OverlayRegistry offering(OverlayContribution... bodies) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("jovian.system", () -> List.of(bodies));
        return registry;
    }

    private static List<ReferenceInk.BodyPlacement> placed(ChartScene scene, OverlayRegistry r,
                                                           PageText words) {
        return ReferenceInk.bodyPlacements(DrawnPage.of(scene), r.collect(),
                ChartPalette.WHITE_PAPER, words, List.of());
    }

    /** The page with both of the module's layers: the reference layer beneath the marks, the bodies above. */
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

    private static DrawnMark star(ChartScene scene, SkyPosition near) {
        DrawnPage page = DrawnPage.of(scene);
        PixelPoint at = new ViewportMapping(page).toPixel(page.projection().project(near).orElseThrow());
        DrawnMark best = null;
        double closest = Double.MAX_VALUE;
        for (DrawnMark mark : RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.kind() != DrawnMark.Kind.STAR) {
                continue;
            }
            java.awt.geom.Rectangle2D b = mark.outline().getBounds2D();
            double d = Math.hypot(b.getCenterX() - at.x(), b.getCenterY() - at.y());
            if (d < closest) {
                closest = d;
                best = mark;
            }
        }
        assertTrue(closest < 2.0, "a star mark at the place: " + closest);
        return best;
    }

    @Test
    void atOrdinaryFieldsTheMinimumIsASymbolBeneathTheStarsThatAClickPassesThrough() {
        ChartScene scene = page(REGULUS, 8.0);
        // Jupiter's true disc is 1-2 px here; the mark is the 6 px minimum.
        OverlayRegistry registry = offering(jupiter(REGULUS, 40.0, 37.4, 0.0, false));
        ReferenceInk.BodyPlacement placed = placed(scene, registry, ENGLISH).get(0);
        assertTrue(placed.symbol(), "below the minimum the mark is a symbol");
        double width = placed.disc().getBounds2D().getWidth();
        assertEquals(MIN, width, 0.05, "the 6 px minimum, a circle");
        assertFalse(placed.covers(placed.centre().x(), placed.centre().y()),
                "a symbol hides nothing, so a click through it reaches the star");
        // Ruling 7: the symbolic mark never erases a real star.
        DrawnMark regulus = star(scene, REGULUS);
        BufferedImage without = painted(scene, offering(), ChartPalette.WHITE_PAPER);
        BufferedImage with = painted(scene, registry, ChartPalette.WHITE_PAPER);
        java.awt.Rectangle b = regulus.outline().getBounds();
        int checked = 0;
        for (int x = b.x; x < b.x + b.width; x++) {
            for (int y = b.y; y < b.y + b.height; y++) {
                if (regulus.outline().contains(x + 0.5, y + 0.5)) {
                    assertEquals(without.getRGB(x, y), with.getRGB(x, y),
                            "Regulus's own ink is unchanged at " + x + "," + y);
                    checked++;
                }
            }
        }
        assertTrue(checked > 4, "the star's ink was compared: " + checked);
    }

    @Test
    void onceTheTrueDiscReachesTheMinimumItIsTheBodyOpaqueOverWhatItCovers() {
        ChartScene scene = page(REGULUS, 1.0);
        // A disc of 60 px at 1° - larger than Jupiter, to cover the star wholly.
        OverlayRegistry registry = offering(jupiter(REGULUS, 240.0, 224.4, 0.0, false));
        ReferenceInk.BodyPlacement placed = placed(scene, registry, ENGLISH).get(0);
        assertFalse(placed.symbol());
        assertTrue(placed.covers(placed.centre().x(), placed.centre().y()),
                "a click on the disc reaches no hidden star");
        BufferedImage without = painted(scene, offering(), ChartPalette.WHITE_PAPER);
        BufferedImage with = painted(scene, registry, ChartPalette.WHITE_PAPER);
        int cx = (int) Math.round(placed.centre().x());
        int cy = (int) Math.round(placed.centre().y());
        assertNotEquals(without.getRGB(cx, cy), with.getRGB(cx, cy), "the star beneath is covered");
        assertEquals(java.awt.Color.WHITE.getRGB(), with.getRGB(cx, cy),
                "on white paper the lit face is the ground: opaque");
    }

    @Test
    void theOutlineTurnsFromACircleToTheTrueRatioAcrossTwiceTheMinimum() {
        ChartScene scene = page(new SkyPosition(150.0, 0.0), 1.0);
        double perArcsec = 900.0 / 3600.0; // a 1° field 900 px wide, near the centre
        double ratio = 0.5; // exaggerated so the figure is measurable
        double[] trueWidths = {MIN, 1.5 * MIN, 2 * MIN, 4 * MIN};
        double[] expectedRatio = {1.0, 0.75, 0.5, 0.5};
        for (int i = 0; i < trueWidths.length; i++) {
            double eq = trueWidths[i] / perArcsec;
            ReferenceInk.OblateMark mark = ReferenceInk.oblateOn(DrawnPage.of(scene),
                    jupiter(new SkyPosition(150.0, 0.0), eq, eq * ratio, 0.0, false));
            java.awt.geom.Rectangle2D b = mark.outline().getBounds2D();
            assertEquals(expectedRatio[i], b.getHeight() / b.getWidth(), 0.02,
                    "pole north: the minor axis is vertical, ratio at " + trueWidths[i] + " px");
        }
    }

    @Test
    void theMinorAxisLiesAlongThePoleThroughThePagesOwnNorthAndEast() {
        SkyPosition at = new SkyPosition(150.0, 40.0);
        ChartScene scene = page(new SkyPosition(149.4, 39.6), 3.0);
        DrawnPage page = DrawnPage.of(scene);
        double eq = 300.0; // 25 px on a 3° page: past twice the minimum, the true ratio
        for (double pole : new double[] {0.0, 30.0, 90.0, 147.0}) {
            ReferenceInk.OblateMark mark = ReferenceInk.oblateOn(page,
                    jupiter(at, eq, eq * 0.5, pole, false));
            double[] up = PageBasis.at(page, at).orElseThrow().direction(pole);
            double along = reach(mark.outline(), mark.centre(), up);
            double across = reach(mark.outline(), mark.centre(), new double[] {-up[1], up[0]});
            assertEquals(0.5, along / across, 0.05, "the pole at " + pole + "° is the short axis");
        }
    }

    /** How far the outline reaches from its centre along a page direction. */
    private static double reach(Shape outline, PixelPoint c, double[] d) {
        double r = 0.0;
        while (outline.contains(c.x() + d[0] * (r + 0.1), c.y() + d[1] * (r + 0.1))) {
            r += 0.1;
        }
        return r;
    }

    @Test
    void belowTheDrawnHorizonItIsDimmedAndSaysSoAndOffThePageNothingIsDrawn() {
        ChartScene scene = page(REGULUS, 8.0);
        ReferenceInk.BodyPlacement en = placed(scene,
                offering(jupiter(REGULUS, 40.0, 37.4, 0.0, true)), ENGLISH).get(0);
        assertTrue(en.belowHorizon());
        assertEquals("Jupiter (below the horizon)", en.name());
        assertEquals("Jupiter (under horisonten)", placed(scene,
                offering(jupiter(REGULUS, 40.0, 37.4, 0.0, true)), NORSK).get(0).name());
        assertEquals(List.of(), placed(scene,
                offering(jupiter(new SkyPosition(REGULUS.raDegrees() + 30.0, 11.967), 40.0, 37.4,
                        0.0, false)), ENGLISH), "off the page: nothing, no edge hint");
    }

    @Test
    void thePageCallsTheMinimumASymbolNotJupitersApparentDiameter() {
        ChartScene scene = page(REGULUS, 8.0);
        List<ReferenceInk.BodyPlacement> symbol = placed(scene,
                offering(jupiter(REGULUS, 40.0, 37.4, 0.0, false)), ENGLISH);
        assertEquals("Chart. Jupiter (a cartographic symbol, not Jupiter's apparent diameter).",
                ChartComponent.withBodies("Chart.", symbol, ENGLISH));
        assertEquals("Diagram. Jupiter (et kartografisk symbol, ikke Jupiters tilsynelatende diameter).",
                ChartComponent.withBodies("Diagram.", placed(scene,
                        offering(jupiter(REGULUS, 40.0, 37.4, 0.0, false)), NORSK), NORSK));
        List<ReferenceInk.BodyPlacement> disc = placed(page(REGULUS, 1.0),
                offering(jupiter(REGULUS, 240.0, 224.4, 0.0, false)), ENGLISH);
        assertEquals("Chart. Jupiter.", ChartComponent.withBodies("Chart.", disc, ENGLISH),
                "a true disc is the body, not a symbol");
    }
}
