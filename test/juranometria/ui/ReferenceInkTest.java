package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.meridian.MeridianModule;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.GnomonicProjection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the chart inks a module's reference geometry (Sprint 25,
 * issue #227).
 *
 * <p>Every claim is asked of the rendered page rather than of the
 * code that renders it: whether the ordinary chart is untouched,
 * where the line sits in the stack, whether it is drawn at all, and
 * whether two modules attached in either order produce one page.
 */
class ReferenceInkTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);
    private static final ChartScene SCENE = Atlas.assembler()
            .assemble(ChartViewState.DEFAULT, 900, 700);

    private static BufferedImage page(List<OverlayRegistry.Owned> ink) {
        BufferedImage image = new BufferedImage(
                SCENE.viewport().widthPx(), SCENE.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, SCENE, ChartOptions.DEFAULTS,
                    (layerG, scene) ->
                            ReferenceInk.paint(layerG, scene, ink));
        } finally {
            g.dispose();
        }
        return image;
    }

    private static BufferedImage ordinaryPage() {
        BufferedImage image = new BufferedImage(
                SCENE.viewport().widthPx(), SCENE.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, SCENE, ChartOptions.DEFAULTS);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static List<OverlayRegistry.Owned> offered(String moduleId,
            List<OverlayContribution> geometry) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(moduleId, () -> geometry);
        return registry.collect();
    }

    /**
     * The pole of a great circle through a position.
     *
     * <p>Ninety degrees away along the equator, which is
     * perpendicular to the position and so puts the circle through
     * it. Any perpendicular would do; this one is arithmetic a
     * reader can check by eye.
     */
    private static SkyPosition poleThrough(SkyPosition position) {
        double ra = position.raDegrees() + 90.0;
        return new SkyPosition(ra >= 360.0 ? ra - 360.0 : ra, 0.0);
    }

    private static double[] pixelOf(SkyPosition at) {
        return new GnomonicProjection(SCENE.viewport().centre()).project(at)
                .map(new ViewportMapping(SCENE.viewport())::toPixel)
                .map(pixel -> new double[] {pixel.x(), pixel.y()})
                .orElseThrow();
    }

    // ---- the chart without a module ---------------------------------

    @Test
    void withNothingContributedThePageIsTheOrdinaryChart() {
        assertEquals(0, differences(ordinaryPage(), page(List.of())),
                "an atlas whose modules are absent draws the released"
                        + " page, byte for byte");
    }

    @Test
    void aModuleShowingNothingLeavesNoTraceEither() {
        MeridianModule module = new MeridianModule(new Observer(59.913,
                10.752, Instant.parse("2026-03-20T21:33:00Z")));
        module.showing(false, false, false);

        assertEquals(0, differences(ordinaryPage(),
                        page(offered(MeridianModule.ID,
                                module.contributedGeometry()))),
                "removable has to mean removable, and a module that is"
                        + " merely quiet leaves the page alone too");
    }

    @Test
    void aCircleThatMissesThePaperIsDrawnAsNothing() {
        // The pole at the page's own centre: its circle is ninety
        // degrees away in every direction. On most pages none of the
        // three crosses the paper, and the honest page is the
        // ordinary one.
        assertEquals(0, differences(ordinaryPage(),
                        page(offered("far", List.of(
                                new OverlayContribution.GreatCircle(
                                        "nowhere", "Nowhere",
                                        SCENE.viewport().centre(),
                                        OverlayContribution.Reference.LINE,
                                        InkRole.REFERENCE_LINE))))),
                "off the page is silence, not a line drawn along an"
                        + " edge");
    }

    @Test
    void geometryInAnotherRoleIsNotReferenceInk() {
        // On clear paper, proved clear first. The first version put
        // the point at the page centre, where M31's own ellipse is
        // painted over the reference layer - so a painter that drew
        // every role would still have shown a clean page, and the
        // mutation that dropped the role filter failed nothing.
        BufferedImage before = ordinaryPage();
        int[] clear = aClearPixel(before);
        assertTrue(clear != null, "the page has clear paper on it");
        SkyPosition on = ChartHitTest.skyAt(SCENE, clear[0] + 0.5,
                clear[1] + 0.5);

        assertEquals(0, differences(before,
                        page(offered("other", List.of(
                                new OverlayContribution.Point("mark", "Mark",
                                        on, InkRole.INTERACTION))))),
                "an interaction mark is painted elsewhere, after the"
                        + " chart; this layer draws reference ink and"
                        + " nothing else");

        // And the premise, so this cannot rot the same way again: the
        // same point in the reference role really would have shown.
        assertTrue(differences(before,
                        page(offered("other", List.of(
                                new OverlayContribution.Point("mark", "Mark",
                                        on, InkRole.REFERENCE_LINE))))) > 0,
                "the spot is genuinely clear enough for ink to show");
    }

    /** A white pixel with white around it, well inside the paper. */
    private static int[] aClearPixel(BufferedImage page) {
        int paper = java.awt.Color.WHITE.getRGB();
        for (int y = 100; y < 600; y++) {
            pixels:
            for (int x = 100; x < 800; x++) {
                for (int dy = -12; dy <= 12; dy++) {
                    for (int dx = -12; dx <= 40; dx++) {
                        if (page.getRGB(x + dx, y + dy) != paper) {
                            continue pixels;
                        }
                    }
                }
                return new int[] {x, y};
            }
        }
        return null;
    }

    // ---- where it sits in the stack ---------------------------------

    @Test
    void theLineIsDrawnUnderEveryMark() {
        // A circle put through a star's own position, so the line
        // and the star want the same pixel. The star must win: a
        // reference line exists to be read across the chart and must
        // never hide an object.
        ChartRenderer.DrawnMark star = RENDERER
                .drawnMarks(SCENE, ChartOptions.DEFAULTS).stream()
                .filter(mark -> mark.kind()
                        == ChartRenderer.DrawnMark.Kind.STAR)
                .filter(mark -> mark.centre().x() > 100
                        && mark.centre().x() < 800
                        && mark.centre().y() > 100
                        && mark.centre().y() < 600)
                .findFirst().orElseThrow();
        SkyPosition through = ChartHitTest.skyAt(SCENE,
                star.centre().x(), star.centre().y());

        BufferedImage before = ordinaryPage();
        BufferedImage after = page(offered("through", List.of(
                new OverlayContribution.GreatCircle("line", "Line",
                        poleThrough(through),
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE))));

        int x = (int) Math.round(star.centre().x());
        int y = (int) Math.round(star.centre().y());
        assertTrue(differences(before, after) > 0,
                "the premise: this circle is drawn on this page");
        assertEquals(before.getRGB(x, y), after.getRGB(x, y),
                "and the star is drawn over the line, not under it");
    }

    @Test
    void theLineIsDrawnOverTheGrid() {
        // The other half of the layering. The circle is built
        // through a pixel the graticule has already claimed, so the
        // two want the same ink, and the question is which of them
        // is showing afterwards.
        //
        // Counted over the page rather than checked at that one
        // pixel: the first version of this test asked about the
        // chosen pixel and failed at 370,209, where M31's ellipse
        // covers the line - which is the layering working, not
        // breaking. A line hidden by a galaxy is still a line drawn
        // over the grid everywhere else.
        BufferedImage before = ordinaryPage();
        int[] grid = anyGridPixel(before);
        assertTrue(grid != null, "the page draws a graticule");

        // The pixel's centre, not its corner: a line through the
        // corner is half covered and the test would be measuring
        // antialiasing rather than layering.
        SkyPosition on = ChartHitTest.skyAt(SCENE, grid[0] + 0.5,
                grid[1] + 0.5);
        BufferedImage after = page(offered("over", List.of(
                new OverlayContribution.GreatCircle("line", "Line",
                        poleThrough(on),
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE))));

        assertTrue(gridPixelsCovered(before, after) > 0,
                "the reference line covers the grid where it crosses"
                        + " it; drawn first, the graticule would be"
                        + " painted back over every one of them");
    }

    /** Pixels the graticule had and the reference line took. */
    private static int gridPixelsCovered(BufferedImage before,
                                         BufferedImage after) {
        int gridInk = juranometria.render.EquatorialGrid.GRID_INK.getRGB();
        int taken = 0;
        for (int y = 0; y < before.getHeight(); y++) {
            for (int x = 0; x < before.getWidth(); x++) {
                if (before.getRGB(x, y) == gridInk
                        && after.getRGB(x, y) != gridInk) {
                    taken++;
                }
            }
        }
        return taken;
    }

    /** A pixel the graticule drew, well inside the paper. */
    private static int[] anyGridPixel(BufferedImage page) {
        int gridInk = juranometria.render.EquatorialGrid.GRID_INK.getRGB();
        for (int y = 200; y < 500; y++) {
            for (int x = 200; x < 700; x++) {
                if (page.getRGB(x, y) == gridInk) {
                    return new int[] {x, y};
                }
            }
        }
        return null;
    }

    @Test
    void theLinesLandOnTheModelsPositionsOnEveryAwkwardPage() {
        // The acceptance criterion, on the pages that break things:
        // the seam at right ascension zero, both celestial poles, and
        // fields from the widest to the narrowest, where a projection
        // refuses half the sky. Every end of every drawn line is
        // turned back into a direction through the chart's own
        // inverse and must be ninety degrees from the circle's pole.
        //
        // Nothing is bridged. The clip answers from the visible half
        // alone, so a circle whose far side the projection refuses is
        // drawn across the paper it does cross and nowhere else -
        // which is the whole reason the geometry is a pole and not a
        // polyline.
        MeridianModule module = new MeridianModule(new Observer(59.913,
                10.752, Instant.parse("2026-03-20T21:33:00Z")));
        int drawn = 0;
        for (SkyPosition centre : List.of(
                new SkyPosition(0.0, 0.0),      // the seam
                new SkyPosition(359.98, 0.02),  // and just the other side
                new SkyPosition(0.0, 89.7),     // hard against the pole
                new SkyPosition(180.0, -89.7),  // and the other one
                new SkyPosition(83.8, -5.4))) {
            for (double field : new double[] {36.0, 8.0, 1.0}) {
                ChartScene scene = Atlas.assembler().assemble(
                        new ChartViewState(centre, field, 8.0), 900, 700);
                for (OverlayContribution offered
                        : module.contributedGeometry()) {
                    if (!(offered
                            instanceof OverlayContribution.GreatCircle circle)) {
                        continue;
                    }
                    var paper = ChartRenderer.paperOf(scene);
                    var arc = juranometria.project.GreatCirclePage.clip(
                            new GnomonicProjection(scene.viewport().centre()),
                            new ViewportMapping(scene.viewport()),
                            new juranometria.project.GreatCirclePage.Page(
                                    paper.getMinX(), paper.getMinY(),
                                    paper.getMaxX(), paper.getMaxY()),
                            circle.pole());
                    if (arc.isEmpty()) {
                        continue;
                    }
                    drawn++;
                    for (var end : List.of(arc.get().from(), arc.get().to())) {
                        SkyPosition sky = ChartHitTest.skyAt(scene, end.x(),
                                end.y());
                        assertTrue(sky != null,
                                "an end of the drawn line is on the sky");
                        assertEquals(90.0,
                                circle.pole().separationDegrees(sky), 1e-6,
                                circle.identity() + " at " + centre + ", "
                                        + field + "°: the drawn end is on"
                                        + " the circle it belongs to");
                    }
                }
            }
        }
        assertTrue(drawn > 4,
                "the premise: these pages really do show lines - "
                        + drawn + " of them, so the loop above is not"
                        + " quietly asserting nothing");
    }

    // ---- the chart decides, not the modules -------------------------

    @Test
    void twoModulesAttachedEitherWayRoundDrawTheSamePage() {
        List<OverlayContribution> first = List.of(
                new OverlayContribution.GreatCircle("a", "A",
                        new SkyPosition(100.0, 0.0),
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE));
        List<OverlayContribution> second = List.of(
                new OverlayContribution.GreatCircle("b", "B",
                        new SkyPosition(101.0, 2.0),
                        OverlayContribution.Reference.BOUNDARY,
                        InkRole.REFERENCE_LINE));

        OverlayRegistry oneWay = new OverlayRegistry();
        oneWay.offer("first", () -> first);
        oneWay.offer("second", () -> second);
        OverlayRegistry theOther = new OverlayRegistry();
        theOther.offer("second", () -> second);
        theOther.offer("first", () -> first);

        assertEquals(0, differences(page(oneWay.collect()),
                        page(theOther.collect())),
                "ordering is the chart's decision, and a module that"
                        + " attached first has no claim on the page");
    }

    @Test
    void aBoundaryIsDrawnDifferentlyFromALineAcrossTheSky() {
        SkyPosition pole = new SkyPosition(100.0, 0.0);
        BufferedImage line = page(offered("m", List.of(
                new OverlayContribution.GreatCircle("x", "X", pole,
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE))));
        BufferedImage boundary = page(offered("m", List.of(
                new OverlayContribution.GreatCircle("x", "X", pole,
                        OverlayContribution.Reference.BOUNDARY,
                        InkRole.REFERENCE_LINE))));

        assertTrue(differences(line, boundary) > 0,
                "the same circle drawn as a boundary of visibility is"
                        + " not the same ink as a line across the sky");
        assertTrue(differences(ordinaryPage(), boundary)
                        < differences(ordinaryPage(), line),
                "and it is the dashed one, so it puts down less ink: "
                        + differences(ordinaryPage(), boundary) + " px"
                        + " against " + differences(ordinaryPage(), line));
    }

    @Test
    void theNameOnThePageIsTheNameTheModuleGaveIt() {
        SkyPosition pole = new SkyPosition(100.0, 0.0);
        BufferedImage named = page(offered("m", List.of(
                new OverlayContribution.GreatCircle("x", "Meridian", pole,
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE))));
        BufferedImage renamed = page(offered("m", List.of(
                new OverlayContribution.GreatCircle("x", "Horizon", pole,
                        OverlayContribution.Reference.LINE,
                        InkRole.REFERENCE_LINE))));

        assertTrue(differences(named, renamed) > 0,
                "a reader who cannot see the line is told the same word"
                        + " it is drawn with, so changing that word"
                        + " changes the page");
    }

    // ---- and it is not a catalogue object ---------------------------

    @Test
    void referenceGeometryIsNeverMistakenForAStarOrADeepSkyObject() {
        SkyPosition zenith = new SkyPosition(10.7, 41.3);
        List<OverlayRegistry.Owned> ink = offered("m", List.of(
                new OverlayContribution.Point("zenith", "Zenith", zenith,
                        InkRole.REFERENCE_LINE)));
        page(ink);

        double[] at = pixelOf(zenith);
        for (ChartRenderer.DrawnMark mark : ChartHitTest.orderedHits(
                RENDERER.drawnMarks(SCENE, ChartOptions.DEFAULTS),
                at[0], at[1], ChartHitTest.TOLERANCE_PX)) {
            String id = mark.star() != null ? mark.star().id()
                    : mark.deepSky().id();
            assertNotEquals("zenith", id,
                    "reference ink is furniture: a reader pointing at"
                            + " it is told about the sky underneath,"
                            + " never handed a line as if it were an"
                            + " object");
        }
        assertTrue(RENDERER.drawnMarks(SCENE, ChartOptions.DEFAULTS).stream()
                        .noneMatch(mark -> mark.star() == null
                                && mark.deepSky() == null),
                "and nothing contributed has become a drawn mark");
    }

    @Test
    void paintingReadsNoCatalogue() throws IOException {
        // Repainting must perform no catalogue query. Asked of the
        // compiled painter, because that is where a stray lookup
        // would show up whether or not any test happened to notice
        // the cost of it.
        String pool = new String(Files.readAllBytes(Path.of(
                        "build/classes/juranometria/ui/ReferenceInk.class")),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        for (String forbidden : List.of("juranometria/catalog",
                "juranometria/search", "juranometria/app/Atlas")) {
            assertTrue(!pool.contains(forbidden),
                    "the reference painter is given a scene and inks"
                            + " it; it must not reach for " + forbidden);
        }
    }

    // ----------------------------------------------------------------

    private static int differences(BufferedImage a, BufferedImage b) {
        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(a.getHeight(), b.getHeight());
        int differing = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    differing++;
                }
            }
        }
        return differing;
    }
}
