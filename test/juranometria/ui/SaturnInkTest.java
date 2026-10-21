package juranometria.ui;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the page draws Saturn (issue #579, the owner's rulings on #578):
 * the larger of its true figure - globe and rings - and a 6 px minimum,
 * measured by the rings' major axis, as a discrete switch; while the
 * minimum stands in for it, a cartographic symbol beneath the stars that
 * a click passes through; once the rings reach the minimum, the figure,
 * opaque; the rings' major axis across the pole through the page's own
 * north and east; the near half over the globe, away from the projected
 * pole when the north face is seen; edge-on, a line; the unlit face
 * dimmed; the outline and the Cassini gap only where the rule gives
 * them; dimmed below a drawn horizon; absent off the page.
 */
class SaturnInkTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
    private static final SkyPosition REGULUS = new SkyPosition(152.093, 11.967);
    private static final double MIN = 6.0;
    /** The ruled ring edges (#574, ruling 4; the manifest's PDS values), km. */
    private static final List<Double> RINGS = List.of(74491.0, 117570.0, 122050.0, 136770.0);
    /** The PCK radii (#574): equatorial and polar, km. */
    private static final double RE = 60268.0;
    private static final double RP = 54364.0;
    /** About Saturn's distance at the card instant: 8.43 AU in km (IAU 2012 AU). */
    private static final double FAR_KM = 8.434258 * 149_597_870.7;

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    /** Saturn at a distance, with its true figure from the radii, at a ring opening. */
    private static OverlayContribution.RingedBody saturn(SkyPosition at, double distanceKm,
                                                         double openingB, boolean unlit,
                                                         double pole, boolean below) {
        double eq = 2.0 * Math.toDegrees(Math.asin(RE / distanceKm)) * 3600.0;
        double b = Math.toRadians(openingB);
        double semiPolar = Math.sqrt(RP * RP * Math.cos(b) * Math.cos(b)
                + RE * RE * Math.sin(b) * Math.sin(b));
        double pol = 2.0 * Math.toDegrees(Math.asin(semiPolar / distanceKm)) * 3600.0;
        return new OverlayContribution.RingedBody("saturn", "Saturn", at, eq, pol, pole,
                openingB, unlit, RINGS, MIN, below, distanceKm, InkRole.BODY, null, false);
    }

    private static OverlayRegistry offering(OverlayContribution... bodies) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("saturn.system", () -> List.of(bodies));
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

    private static DrawnMark star(ChartScene scene, SkyPosition near) {
        DrawnPage page = DrawnPage.of(scene);
        PixelPoint at = new ViewportMapping(page).toPixel(page.projection().project(near).orElseThrow());
        DrawnMark best = null;
        double closest = Double.MAX_VALUE;
        for (DrawnMark mark : RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.kind() != DrawnMark.Kind.STAR) {
                continue;
            }
            Rectangle2D b = mark.outline().getBounds2D();
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
        // The rings' true major axis is about 1.4 px here; the mark is the 6 px minimum.
        OverlayRegistry registry = offering(saturn(REGULUS, FAR_KM, -7.4, false, 0.0, false));
        ReferenceInk.BodyPlacement placed = placed(scene, registry, ENGLISH).get(0);
        assertTrue(placed.symbol(), "under the minimum the mark is a symbol");
        assertEquals(MIN, placed.disc().getBounds2D().getWidth(), 0.05, "the 6 px minimum, a circle");
        assertFalse(placed.covers(placed.centre().x(), placed.centre().y()),
                "a symbol hides nothing, so a click through it reaches the star");
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
    void onceTheRingsReachTheMinimumTheFigureIsOpaqueOverWhatItCovers() {
        ChartScene scene = page(REGULUS, 1.0);
        // Ten times nearer: the rings about 110 px across at 1°, the globe about 48.
        OverlayRegistry registry = offering(saturn(REGULUS, FAR_KM / 10.0, -20.0, false, 0.0, false));
        ReferenceInk.BodyPlacement placed = placed(scene, registry, ENGLISH).get(0);
        assertFalse(placed.symbol(), "past the minimum, the true figure");
        assertTrue(placed.covers(placed.centre().x(), placed.centre().y()),
                "a click on the globe reaches no hidden star");
        BufferedImage without = painted(scene, offering(), ChartPalette.WHITE_PAPER);
        BufferedImage with = painted(scene, registry, ChartPalette.WHITE_PAPER);
        int cx = (int) Math.round(placed.centre().x());
        int cy = (int) Math.round(placed.centre().y());
        assertNotEquals(without.getRGB(cx, cy), with.getRGB(cx, cy), "the star beneath is covered");
        assertEquals(java.awt.Color.WHITE.getRGB(), with.getRGB(cx, cy),
                "on white paper the globe's face is the ground: opaque");
    }

    @Test
    void theRingsMajorAxisLiesAcrossThePoleThroughThePagesOwnNorthAndEast() {
        SkyPosition at = new SkyPosition(150.0, 40.0);
        ChartScene scene = page(new SkyPosition(149.4, 39.6), 3.0);
        DrawnPage page = DrawnPage.of(scene);
        // Opened 60°, so the rings' minor axis (0.87 of the major) reaches
        // well past the globe along the pole.
        for (double pole : new double[] {0.0, 30.0, 90.0, 147.0}) {
            ReferenceInk.RingedMark mark = ReferenceInk.ringedOn(page,
                    saturn(at, FAR_KM / 30.0, 60.0, false, pole, false));
            assertFalse(mark.symbol());
            double[] up = PageBasis.at(page, at).orElseThrow().direction(pole);
            double along = extent(mark.outline(), mark.centre(), up);
            double across = extent(mark.outline(), mark.centre(), new double[] {-up[1], up[0]});
            assertEquals(Math.sin(Math.toRadians(60.0)), along / across, 0.03,
                    "the pole at " + pole + "° is the rings' short axis, |sin B| of the long");
        }
    }

    /** How far a shape's ink extends from a centre along a page direction: its outermost point. */
    private static double extent(Shape outline, PixelPoint c, double[] d) {
        for (double r = 400.0; r > 0.0; r -= 0.1) {
            if (outline.contains(c.x() + d[0] * r, c.y() + d[1] * r)) {
                return r;
            }
        }
        return 0.0;
    }

    @Test
    void theNearHalfLiesAwayFromTheProjectedPoleWhenTheNorthFaceIsSeen() {
        SkyPosition at = new SkyPosition(150.0, 0.0);
        DrawnPage page = DrawnPage.of(page(at, 1.0));
        double[] up = PageBasis.at(page, at).orElseThrow().direction(0.0);
        ReferenceInk.RingedMark north = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM / 10.0, 20.0, false, 0.0, false));
        ReferenceInk.RingedMark south = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM / 10.0, -20.0, false, 0.0, false));
        assertNotNull(north.nearRings());
        assertNotNull(north.farRings());
        double northSide = side(north.nearRings().getBounds2D(), north.centre(), up);
        double southSide = side(south.nearRings().getBounds2D(), south.centre(), up);
        assertTrue(northSide < 0, "B > 0: the near half lies away from the pole: " + northSide);
        assertTrue(southSide > 0, "B < 0: the near half lies toward the pole: " + southSide);
        assertTrue(side(north.farRings().getBounds2D(), north.centre(), up) > 0,
                "and the far half on the other side");
    }

    /** Where a box's centre lies from a centre along a direction: positive toward it. */
    private static double side(Rectangle2D box, PixelPoint c, double[] d) {
        return (box.getCenterX() - c.x()) * d[0] + (box.getCenterY() - c.y()) * d[1];
    }

    @Test
    void seenEdgeOnTheRingsAreALineAcrossTheGlobe() {
        SkyPosition at = new SkyPosition(150.0, 0.0);
        DrawnPage page = DrawnPage.of(page(at, 1.0));
        ReferenceInk.RingedMark edgeOn = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM / 10.0, 0.4, false, 0.0, false));
        assertFalse(edgeOn.symbol());
        assertNotNull(edgeOn.edgeLine(), "a minor axis under 2 px: a line");
        assertNull(edgeOn.nearRings());
        assertNull(edgeOn.farRings());
        Rectangle2D line = edgeOn.edgeLine().getBounds2D();
        assertTrue(line.getWidth() > 100.0 && line.getHeight() < 2.5,
                "the line spans the rings' major axis, 1 px thick: " + line);
        assertTrue(edgeOn.outline().contains(edgeOn.centre().x() + line.getWidth() / 2.0 - 2.0,
                edgeOn.centre().y()), "the line is opaque ink beyond the globe");
        ReferenceInk.RingedMark open = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM / 10.0, 3.0, false, 0.0, false));
        assertNull(open.edgeLine(), "at 3° the minor axis passes 2 px: annuli again");
        assertNotNull(open.nearRings());
    }

    @Test
    void theUnlitFaceDimsTheRings() {
        SkyPosition at = new SkyPosition(150.0, 0.0);
        ChartScene scene = page(at, 1.0);
        DrawnPage page = DrawnPage.of(scene);
        ReferenceInk.RingedMark lit = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM / 10.0, -20.0, false, 0.0, false));
        // A point on the near half along the minor axis: the first pixel from
        // the centre's column that the near half's ink contains.
        int px = (int) Math.round(lit.centre().x());
        int py = -1;
        for (int y = (int) Math.round(lit.centre().y()) - 80; y < lit.centre().y() + 80; y++) {
            if (lit.nearRings().contains(px + 0.5, y + 0.5)) {
                py = y;
                break;
            }
        }
        assertTrue(py >= 0, "a ring point in the near half");
        int litRgb = painted(scene, offering(saturn(at, FAR_KM / 10.0, -20.0, false, 0.0, false)),
                ChartPalette.WHITE_PAPER).getRGB(px, py);
        int unlitRgb = painted(scene, offering(saturn(at, FAR_KM / 10.0, -20.0, true, 0.0, false)),
                ChartPalette.WHITE_PAPER).getRGB(px, py);
        assertNotEquals(litRgb, unlitRgb, "the unlit face takes a different ink");
        assertTrue(brightness(unlitRgb) > brightness(litRgb),
                "quieter on white paper: " + brightness(unlitRgb) + " > " + brightness(litRgb));
    }

    private static int brightness(int rgb) {
        return ((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff);
    }

    @Test
    void theOutlineAndTheCassiniGapAppearOnlyWhereTheRuleGivesThem() {
        SkyPosition at = new SkyPosition(150.0, 0.0);
        DrawnPage page = DrawnPage.of(page(at, 1.0));
        // Saturn's own distance at 1°: the figure about 11 px wide, the gap 0.2 px.
        ReferenceInk.RingedMark small = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM, -20.0, false, 0.0, false));
        assertFalse(small.symbol(), "11 px across: the true figure");
        assertNull(small.ringOutline(), "no outline under 20 px");
        assertNotNull(small.nearRings());
        // Ten times nearer, open 30°: 111 px wide, minor 56 px, the gap 1.9 px.
        ReferenceInk.RingedMark large = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM / 10.0, -30.0, false, 0.0, false));
        assertNotNull(large.ringOutline(), "wide and thick: the A Ring's outer edge outlined");
        // The gap is 1.9 px across at the ansae and 0.95 px across on the
        // minor axis (foreshortened by sin 30°): ruling 2's "only where it
        // is a pixel" opens it along the major axis and closes it along the
        // minor - the owner's checkpoint finding on #579, two dots at twelve
        // o'clock where a sub-pixel sliver was painted. With the pole up and
        // B < 0 the far half is below the centre.
        double perArcsec = large.reach() / (Math.toDegrees(Math.asin(RINGS.get(3) / (FAR_KM / 10.0))) * 3600.0);
        double sinB = Math.sin(Math.toRadians(30.0));
        double gapArcsec = (Math.toDegrees(Math.asin(RINGS.get(2) / (FAR_KM / 10.0)))
                + Math.toDegrees(Math.asin(RINGS.get(1) / (FAR_KM / 10.0)))) / 2.0 * 3600.0;
        double gapPx = gapArcsec * perArcsec * sinB;
        double y = large.centre().y() + gapPx;
        assertTrue(inRings(large, large.centre().x(), y),
                "0.95 px across on the minor axis: one band at " + gapPx + " px down");
        double x = large.centre().x() + gapArcsec * perArcsec;
        assertFalse(inRings(large, x, large.centre().y()),
                "1.9 px across at the ansa: the Cassini gap is a gap " + (x - large.centre().x())
                        + " px along the major axis");
        // Twenty times nearer: 3.8 px at the ansae, 1.9 px on the minor axis - open all round.
        ReferenceInk.RingedMark nearer = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM / 20.0, -30.0, false, 0.0, false));
        double perNearer = nearer.reach() / (Math.toDegrees(Math.asin(RINGS.get(3) / (FAR_KM / 20.0))) * 3600.0);
        double gapNearerArcsec = (Math.toDegrees(Math.asin(RINGS.get(2) / (FAR_KM / 20.0)))
                + Math.toDegrees(Math.asin(RINGS.get(1) / (FAR_KM / 20.0)))) / 2.0 * 3600.0;
        assertFalse(inRings(nearer, nearer.centre().x(),
                nearer.centre().y() + gapNearerArcsec * perNearer * sinB),
                "a pixel across on the minor axis too: the gap runs all round");
        ReferenceInk.RingedMark openSmall = ReferenceInk.ringedOn(page,
                saturn(at, FAR_KM, -30.0, false, 0.0, false));
        double perSmall = openSmall.reach() / (Math.toDegrees(Math.asin(RINGS.get(3) / FAR_KM)) * 3600.0);
        double gapSmallPx = (Math.toDegrees(Math.asin(RINGS.get(2) / FAR_KM))
                + Math.toDegrees(Math.asin(RINGS.get(1) / FAR_KM))) / 2.0 * 3600.0 * perSmall * sinB;
        double ySmall = openSmall.centre().y() + gapSmallPx;
        assertTrue(openSmall.farRings().contains(openSmall.centre().x(), ySmall)
                        || openSmall.nearRings().contains(openSmall.centre().x(), ySmall),
                "under a pixel the gap closes: one band at " + gapSmallPx + " px");
    }

    private static boolean inRings(ReferenceInk.RingedMark mark, double x, double y) {
        return mark.farRings().contains(x, y) || mark.nearRings().contains(x, y);
    }

    @Test
    void belowTheDrawnHorizonItIsDimmedAndSaysSoAndOffThePageNothingIsDrawn() {
        ChartScene scene = page(REGULUS, 8.0);
        ReferenceInk.BodyPlacement en = placed(scene,
                offering(saturn(REGULUS, FAR_KM, -7.4, false, 0.0, true)), ENGLISH).get(0);
        assertTrue(en.belowHorizon());
        assertEquals("Saturn (below the horizon)", en.name());
        assertEquals("Saturn (under horisonten)", placed(scene,
                offering(saturn(REGULUS, FAR_KM, -7.4, false, 0.0, true)), NORSK).get(0).name());
        assertEquals(List.of(), placed(scene,
                offering(saturn(new SkyPosition(REGULUS.raDegrees() + 30.0, 11.967), FAR_KM,
                        -7.4, false, 0.0, false)), ENGLISH), "off the page: nothing, no edge hint");
    }

    @Test
    void thePageCallsTheMinimumASymbolNotSaturnsApparentDiameter() {
        ChartScene scene = page(REGULUS, 8.0);
        List<ReferenceInk.BodyPlacement> symbol = placed(scene,
                offering(saturn(REGULUS, FAR_KM, -7.4, false, 0.0, false)), ENGLISH);
        assertEquals("Chart. Saturn (a cartographic symbol, not Saturn's apparent diameter).",
                ChartComponent.withBodies("Chart.", symbol, ENGLISH));
        assertEquals("Diagram. Saturn (et kartografisk symbol, ikke Saturns tilsynelatende diameter).",
                ChartComponent.withBodies("Diagram.", placed(scene,
                        offering(saturn(REGULUS, FAR_KM, -7.4, false, 0.0, false)), NORSK), NORSK));
        List<ReferenceInk.BodyPlacement> figure = placed(page(REGULUS, 1.0),
                offering(saturn(REGULUS, FAR_KM, -7.4, false, 0.0, false)), ENGLISH);
        assertFalse(figure.get(0).symbol(), "at 1° Saturn's own rings pass the minimum");
        assertEquals("Chart. Saturn.", ChartComponent.withBodies("Chart.", figure, ENGLISH),
                "the true figure is the body, not a symbol");
    }
}
