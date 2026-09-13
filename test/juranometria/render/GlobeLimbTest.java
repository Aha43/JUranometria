package juranometria.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.project.PageRegion;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The globe draws its own edge (Sprint 32, issue #331).
 *
 * <p>Owner testing found the disc's outline breaking up as the globe
 * was turned. It was not a clipping fault and not a layering fault:
 * <strong>no limb was being drawn at all</strong>. What a reader took
 * for the outline was assembled from whatever sky ink happened to end
 * at the clip - 27 per cent of the circumference with stars alone, 76
 * with every layer on - so rotating the sphere moved the gaps about
 * instead of filling them.
 *
 * <p>An outline three-quarters present is not an outline. It also
 * says nothing dependable about where the visible hemisphere stops,
 * which is the only thing it is there to say.
 *
 * <p>So the page draws one circle, after all the sky ink, from the
 * clip's own shape, at a quarter of the grid's strength - quiet
 * enough not to rebuild the reinforced rim that fading the grid was
 * done to remove, and firm enough that no figure ending on it can
 * break it or impersonate it.
 */
class GlobeLimbTest {

    private static final int SIDE_PX = 900;

    /** A crowded hemisphere and a sparse one. */
    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    private static final SkyPosition SPARSE = new SkyPosition(30.0, 20.0);

    /** How many angles around the limb are asked. */
    private static final int AROUND = 720;

    @Test
    void theLimbIsUnbrokenOnACrowdedPageAndOnASparseOne() {
        for (SkyPosition centre : List.of(SAGITTARIUS, SPARSE)) {
            assertEquals(AROUND, covered(globe(centre), everything()),
                    "the limb is continuous all the way round the page"
                            + " centred at " + centre.raDegrees() + "/"
                            + centre.decDegrees());
        }
    }

    @Test
    void theLimbIsTheSameWithEverySkyLayerSwitchedOff() {
        // The decisive one. With the sky's own ink gone there is
        // nothing left to assemble an outline out of, so a page that
        // still shows a whole circle is showing its own.
        // Genuinely empty, not merely switched off: a page's stars
        // are drawn whatever the reader's switches say, so a globe
        // assembled from the catalogue still has ink on its limb. The
        // scene is built with no sky in it at all, which is the only
        // way to look at the limb by itself.
        ChartScene bare = emptyGlobe(SAGITTARIUS);
        assertEquals(AROUND, covered(bare, nothing()),
                "a globe with nothing on it still has an edge");

        // And it is drawn at the settled weight. With the sky off,
        // every pixel on the limb belongs to the limb, so this is the
        // circle's own ink and nothing else's.
        //
        // Asked as a bound rather than an equality because the
        // circle is antialiased: a pixel the stroke only partly
        // covers is blended towards the ground, which can lighten it
        // and can never darken it. So no pixel of the limb may be
        // darker than the ink it is drawn in, and some must reach it.
        int settled = quarterOfTheGrid();
        int darkest = 255;
        for (int level : levelsOnTheLimb(bare, nothing())) {
            assertTrue(level >= settled,
                    "a pixel on the limb is darker than the quarter of"
                            + " the grid's ink the limb is drawn in: "
                            + level + " against " + settled);
            darkest = Math.min(darkest, level);
        }
        assertEquals(settled, darkest,
                "and the limb does reach that weight somewhere, rather"
                        + " than being lighter everywhere");
    }

    /**
     * The grey the limb is drawn in: the grid's own ink, a quarter of
     * the way from the paper towards it. Computed here from the
     * palette rather than written down, so the test states the rule
     * and not a number that could drift from it.
     */
    private static int quarterOfTheGrid() {
        int ground = ChartPalette.WHITE_PAPER.ground().getRed();
        int grid = ChartPalette.WHITE_PAPER.gridInk().getRed();
        return (int) Math.round(ground + (grid - ground) * 0.25);
    }

    /** The darkest level found at each angle around the limb. */
    private static List<Integer> levelsOnTheLimb(ChartScene scene,
                                                 ChartOptions options) {
        BufferedImage drawn = render(scene, options);
        PageRegion region = regionOf(scene);
        List<Integer> levels = new ArrayList<>();
        for (int at = 0; at < AROUND; at++) {
            double angle = at * 2.0 * Math.PI / AROUND;
            int darkest = 255;
            for (double off = -1.5; off <= 1.5; off += 0.5) {
                int x = (int) Math.round(region.limbX()
                        + (region.limbRadius() + off) * Math.cos(angle));
                int y = (int) Math.round(region.limbY()
                        + (region.limbRadius() + off) * Math.sin(angle));
                if (x < 0 || y < 0 || x >= SIDE_PX || y >= SIDE_PX) {
                    continue;
                }
                darkest = Math.min(darkest,
                        drawn.getRGB(x, y) & 0xff);
            }
            levels.add(darkest);
        }
        return levels;
    }

    @Test
    void theLimbDoesNotChangeAsTheGlobeTurns() {
        // What the reader was actually doing when they found this.
        // The limb is furniture: it is the same circle at every
        // orientation, and only the sky inside it moves.
        int settled = quarterOfTheGrid();
        int turns = 0;
        for (double ra = 0.0; ra < 360.0; ra += 45.0) {
            ChartScene scene = globe(new SkyPosition(ra, -28.0));
            assertEquals(AROUND, covered(scene, everything()),
                    "the limb is whole at right ascension " + ra
                            + " - this is the check the old page failed"
                            + " at three-quarters, with the gaps moving"
                            + " as it turned");
            // And it is the page's own circle that is doing it, at
            // the same weight at every orientation - measured on the
            // empty sky, where nothing else can lie on the limb.
            ChartScene bare = emptyGlobe(new SkyPosition(ra, -28.0));
            int darkest = 255;
            for (int level : levelsOnTheLimb(bare, nothing())) {
                assertTrue(level >= settled,
                        "at right ascension " + ra + " a pixel on the"
                                + " limb is darker than the limb's own"
                                + " ink: " + level + " against "
                                + settled);
                darkest = Math.min(darkest, level);
            }
            assertEquals(settled, darkest,
                    "and the limb is drawn at its settled weight at"
                            + " right ascension " + ra);
            turns++;
        }
        assertTrue(turns >= 8, "the globe was really turned: " + turns);
    }

    @Test
    void anOrdinaryPageDrawsNoLimbAtAll() {
        // The rule reaches the globe and nothing else. A page whose
        // sky has no edge must not acquire a circle - there is no
        // hemisphere boundary to draw, and the released pages are
        // held to the byte.
        for (double field : new double[] {42.0, 120.0}) {
            ChartScene scene = Atlas.assembler().assemble(
                    new ChartViewState(SAGITTARIUS, field, 5.0),
                    SIDE_PX, SIDE_PX);
            ViewportMapping mapping = new ViewportMapping(
                    DrawnPage.of(scene));
            assertTrue(!mapping.regionFor(scene.viewport(),
                            DrawnPage.of(scene).projection()).bounded(),
                    field + " degrees has no limb to draw");
        }
    }

    /**
     * How much of the limb carries ink, by angle - the reader's own
     * question: standing at the edge and turning all the way round,
     * is there always a line?
     */
    private static int covered(ChartScene scene, ChartOptions options) {
        BufferedImage drawn = render(scene, options);
        PageRegion region = regionOf(scene);
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        int covered = 0;
        for (int at = 0; at < AROUND; at++) {
            double angle = at * 2.0 * Math.PI / AROUND;
            for (double off = -1.5; off <= 1.5; off += 0.5) {
                int x = (int) Math.round(region.limbX()
                        + (region.limbRadius() + off) * Math.cos(angle));
                int y = (int) Math.round(region.limbY()
                        + (region.limbRadius() + off) * Math.sin(angle));
                if (x < 0 || y < 0 || x >= SIDE_PX || y >= SIDE_PX) {
                    continue;
                }
                if ((drawn.getRGB(x, y) & 0xffffff) != ground) {
                    covered++;
                    break;
                }
            }
        }
        return covered;
    }

    private static PageRegion regionOf(ChartScene scene) {
        DrawnPage page = DrawnPage.of(scene);
        return new ViewportMapping(page).regionFor(scene.viewport(),
                page.projection());
    }

    private static BufferedImage render(ChartScene scene,
                                        ChartOptions options) {
        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, scene, options);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /** A globe with no sky on it at all: the limb, and nothing else. */
    private static ChartScene emptyGlobe(SkyPosition centre) {
        return new ChartScene(new juranometria.chart.ChartViewport(centre,
                180.0, SIDE_PX, SIDE_PX,
                juranometria.chart.ChartProjection.ORTHOGRAPHIC),
                List.of(), List.of(), "an empty globe", 5.0);
    }

    private static ChartScene globe(SkyPosition centre) {
        return Atlas.assembler().assemble(
                new ChartViewState(centre, 180.0, 5.0), SIDE_PX, SIDE_PX);
    }

    /** Every sky layer on, boundaries off, as the globe settles them. */
    private static ChartOptions everything() {
        return new ChartOptions(true, true, true, true, true, true, true,
                true, true, false, false, true, true, true, true, true,
                ChartPalette.WHITE_PAPER);
    }

    /** Every reader switch off, so nothing but furniture is left. */
    private static ChartOptions nothing() {
        return new ChartOptions(false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, ChartPalette.WHITE_PAPER);
    }
}
