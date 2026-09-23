package juranometria.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.ui.ReferenceInk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Temporary cartographic emphasis alters ink only (issue #361).
 *
 * <p>The boundary the discovery mock proved: emphasis must never
 * reuse visibility toggles or any path that changes anchors, names,
 * geometry or membership. What is asserted here is that boundary -
 * the canonical page is byte-identical with the seam present and
 * idle, an emphasized page differs only where the selected
 * structure's own ink is, and every identity the chart does not
 * know keeps canonical ink under every selection.
 */
class StructureEmphasisTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));
    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);

    private static ChartScene page(double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(83.0, 0.0), field,
                        ChartViewState.defaultMagnitudeFor(field)),
                900, 700);
    }

    // ---- the chart's central identity map ---------------------------

    @Test
    void theChartMapsTheIdentitiesItKnows() {
        assertEquals(Optional.of(ChartStructure.MERIDIAN),
                ChartStructure.ofIdentity("meridian"));
        assertEquals(Optional.of(ChartStructure.MERIDIAN),
                ChartStructure.ofIdentity("zenith"),
                "the zenith reads with the meridian");
        for (String identity : List.of("horizon", "cardinal-north",
                "cardinal-east", "cardinal-south", "cardinal-west")) {
            assertEquals(Optional.of(ChartStructure.HORIZON),
                    ChartStructure.ofIdentity(identity),
                    "the horizon and its landmarks are one structure: "
                            + identity);
        }
        for (String identity : List.of("ecliptic", "march-equinox",
                "september-equinox", "june-solstice",
                "december-solstice")) {
            assertEquals(Optional.of(ChartStructure.ECLIPTIC),
                    ChartStructure.ofIdentity(identity),
                    "the ecliptic and its landmarks are one structure: "
                            + identity);
        }
        assertEquals(Optional.empty(),
                ChartStructure.ofIdentity("aurora-oval"),
                "an identity the chart does not know is not guessed at");
    }

    // ---- the one style resolver ------------------------------------

    @Test
    void anUnselectedStructureIsExactlyCanonical() {
        BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f,
                new float[] {4.0f, 3.0f}, 0.0f);
        for (ChartPalette palette : ChartPalette.values()) {
            for (ChartStructure structure : ChartStructure.values()) {
                StructureStyle.Style style = StructureStyle.resolve(
                        palette, structure, false,
                        palette.figureInk(), dashed);
                assertEquals(palette.figureInk(), style.color(),
                        structure + " unselected keeps its ink on "
                                + palette);
                assertEquals(dashed, style.stroke(),
                        structure + " unselected keeps its stroke");
            }
        }
    }

    @Test
    void emphasisRaisesStrokeAndKeepsItsPattern() {
        BasicStroke dashDot = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f,
                new float[] {6.0f, 3.0f, 1.0f, 3.0f}, 0.0f);
        for (ChartPalette palette : ChartPalette.values()) {
            java.util.Set<Color> accents = new java.util.HashSet<>();
            for (ChartStructure structure : ChartStructure.values()) {
                StructureStyle.Style style = StructureStyle.resolve(
                        palette, structure, true,
                        palette.figureInk(), dashDot);
                assertNotEquals(palette.figureInk(), style.color(),
                        structure + " emphasized takes an accent");
                accents.add(style.color());
                assertEquals(dashDot.getLineWidth()
                                + StructureStyle.STROKE_GAIN,
                        style.stroke().getLineWidth(), 1e-6,
                        "a modest stroke gain, stated once");
                org.junit.jupiter.api.Assertions.assertArrayEquals(
                        dashDot.getDashArray(),
                        style.stroke().getDashArray(),
                        "the dash-dot identity survives emphasis");
                assertEquals(dashDot.getEndCap(),
                        style.stroke().getEndCap());
            }
            assertEquals(ChartStructure.values().length, accents.size(),
                    "six structures, six distinguishable accents on "
                            + palette);
        }
    }

    // ---- the page ---------------------------------------------------

    @Test
    void theCanonicalPageIsUntouchedByTheSeam() {
        ChartScene scene = page(42.0);
        for (ChartPalette palette : ChartPalette.values()) {
            ChartOptions options = ChartOptions.DEFAULTS
                    .withPalette(palette);
            assertTrue(identical(RENDERER.renderToImage(scene, options),
                            RENDERER.renderToImage(scene, options, null)),
                    "no emphasis is the canonical page, byte for byte,"
                            + " on " + palette);
        }
    }

    @Test
    void gridEmphasisStaysInsideTheGridsOwnInk() {
        ChartScene scene = page(42.0);
        ChartOptions options = ChartOptions.DEFAULTS;
        BufferedImage canonical = RENDERER.renderToImage(scene, options);
        BufferedImage emphasized = RENDERER.renderToImage(scene, options,
                ChartStructure.EQUATORIAL_GRID);
        // Where the grid is answerable: the on/off difference, grown
        // for antialiased edges and the stroke gain. The toggle is
        // measurement here, never the emphasis mechanism.
        ChartOptions off = new ChartOptions(options.deepSkyObjects(),
                options.deepSkyLabels(), options.constellationFigures(),
                options.constellationBoundaries(),
                options.constellationNames(), options.starNames(),
                options.bayerLetters(), options.flamsteedNumbers(),
                false, options.titleBlock(), options.magnitudeKey(),
                options.galaxies(), options.openClusters(),
                options.globularClusters(), options.nebulae(),
                options.planetaryNebulae(), options.palette());
        boolean[] mask = grown(differenceMask(canonical,
                RENDERER.renderToImage(scene, off)), 900, 700, 2);
        long changed = 0;
        for (int y = 0; y < 700; y++) {
            for (int x = 0; x < 900; x++) {
                if (canonical.getRGB(x, y) == emphasized.getRGB(x, y)) {
                    continue;
                }
                changed++;
                assertTrue(mask[y * 900 + x],
                        "a changed pixel at " + x + "," + y
                                + " is inside the grid's own ink");
            }
        }
        assertTrue(changed > 500,
                "and the emphasis is visible: " + changed + " pixels");
    }

    @Test
    void figureEmphasisLeavesEverythingButTheStrokes() {
        ChartScene scene = page(42.0);
        ChartOptions options = ChartOptions.DEFAULTS;
        BufferedImage canonical = RENDERER.renderToImage(scene, options);
        BufferedImage emphasized = RENDERER.renderToImage(scene, options,
                ChartStructure.CONSTELLATION_FIGURES);
        // The renderer publishes the figure strokes it drew. Every
        // changed pixel must lie on them - anchored stars and
        // constellation names keep canonical ink, so a change
        // anywhere else is the reflow this feature must never cause.
        BufferedImage strokes = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = strokes.createGraphics();
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        for (ChartRenderer.FigureInk figure
                : RENDERER.figureInk(scene, options).values()) {
            g.draw(figure.ink());
        }
        g.dispose();
        long changed = 0;
        for (int y = 0; y < 700; y++) {
            for (int x = 0; x < 900; x++) {
                if (canonical.getRGB(x, y) == emphasized.getRGB(x, y)) {
                    continue;
                }
                changed++;
                assertTrue((strokes.getRGB(x, y) & 0xffffff) != 0,
                        "a changed pixel at " + x + "," + y
                                + " lies on a figure stroke, so stars"
                                + " and names kept their ink");
            }
        }
        assertTrue(changed > 500,
                "and the emphasis is visible: " + changed + " pixels");
    }

    // ---- module structures and the unknown identity -----------------

    @Test
    void anUnknownIdentityKeepsCanonicalInkUnderEverySelection() {
        // The reference layer alone, so what is compared is the
        // unknown line's own ink and not the page around it - under
        // a grid selection the page's grid rightly changes.
        ChartScene scene = page(42.0);
        OverlayContribution unknown = new OverlayContribution.GreatCircle(
                "aurora-oval", "Aurora oval",
                new SkyPosition(353.0, 45.0),
                OverlayContribution.Reference.LINE,
                InkRole.REFERENCE_LINE);
        BufferedImage canonical = layerAlone(scene, unknown, null);
        for (ChartStructure selected : ChartStructure.values()) {
            assertTrue(identical(canonical,
                            layerAlone(scene, unknown, selected)),
                    "an unknown identity is canonical under " + selected);
        }
    }

    private static BufferedImage layerAlone(ChartScene scene,
                                            OverlayContribution geometry,
                                            ChartStructure emphasized) {
        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            ReferenceInk.paint(g, scene,
                    List.of(new OverlayRegistry.Owned("test-module",
                            geometry)),
                    ChartPalette.WHITE_PAPER, ENGLISH, List.of(),
                    emphasized);
        } finally {
            g.dispose();
        }
        return image;
    }

    @Test
    void aModuleStructureTakesItsAccentAndOnlyItOwnInkMoves() {
        ChartScene scene = page(42.0);
        OverlayContribution meridian = new OverlayContribution.GreatCircle(
                "meridian", "Meridian", new SkyPosition(173.0, 0.0),
                OverlayContribution.Reference.LINE,
                InkRole.REFERENCE_LINE);
        OverlayContribution unknown = new OverlayContribution.GreatCircle(
                "aurora-oval", "Aurora oval",
                new SkyPosition(353.0, 45.0),
                OverlayContribution.Reference.LINE,
                InkRole.REFERENCE_LINE);
        List<OverlayContribution> both = List.of(meridian, unknown);
        BufferedImage canonical = painted(scene, both, null);
        BufferedImage emphasized = painted(scene, both,
                ChartStructure.MERIDIAN);
        assertTrue(!identical(canonical, emphasized),
                "emphasizing the meridian changes its ink");
        // And nothing but the meridian's: with the meridian withheld
        // the two selections paint the same page, so every changed
        // pixel above belonged to the meridian.
        assertTrue(identical(painted(scene, List.of(unknown), null),
                        painted(scene, List.of(unknown),
                                ChartStructure.MERIDIAN)),
                "the unknown line is untouched by the selection");
    }

    // ----------------------------------------------------------------

    private static BufferedImage painted(ChartScene scene,
                                         List<OverlayContribution>
                                                 contributions,
                                         ChartStructure emphasized) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer("test-module", () -> contributions);
        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, ChartOptions.DEFAULTS,
                    (layerG, painted, reserved) -> ReferenceInk.paint(
                            layerG, painted, registry.collect(),
                            ChartOptions.DEFAULTS.palette(), ENGLISH,
                            reserved, emphasized),
                    null, emphasized);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static boolean[] differenceMask(BufferedImage one,
                                            BufferedImage other) {
        boolean[] mask = new boolean[900 * 700];
        for (int y = 0; y < 700; y++) {
            for (int x = 0; x < 900; x++) {
                mask[y * 900 + x] = one.getRGB(x, y) != other.getRGB(x, y);
            }
        }
        return mask;
    }

    private static boolean[] grown(boolean[] mask, int wide, int high,
                                   int by) {
        boolean[] out = new boolean[mask.length];
        for (int y = 0; y < high; y++) {
            for (int x = 0; x < wide; x++) {
                if (!mask[y * wide + x]) {
                    continue;
                }
                for (int dy = -by; dy <= by; dy++) {
                    for (int dx = -by; dx <= by; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < wide && ny < high) {
                            out[ny * wide + nx] = true;
                        }
                    }
                }
            }
        }
        return out;
    }

    private static boolean identical(BufferedImage one,
                                     BufferedImage other) {
        for (int y = 0; y < one.getHeight(); y++) {
            for (int x = 0; x < one.getWidth(); x++) {
                if (one.getRGB(x, y) != other.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
