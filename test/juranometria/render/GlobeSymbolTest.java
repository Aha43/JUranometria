package juranometria.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.project.Projections;
import juranometria.project.SkyFootprint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a bounded page makes of a mark (Sprint 32, issue #331).
 *
 * <p>The atlas draws every deep-sky object as its family's glyph, and
 * that vocabulary survives the globe: a globular stays a circle with
 * a cross, an open cluster a dotted ring, a nebula an open box. What
 * changes near a limb is not what the mark is but what the projection
 * does to it - the sphere turns away, and a mark drawn on it is
 * foreshortened along the direction of that turn.
 *
 * <p>So the glyph is built exactly as every other page builds it and
 * then the composed shape is carried into the projected footprint.
 * These hold the five things that makes true, and the first is the
 * one that keeps it true later: <strong>every dimensioned family
 * inherits the transform</strong>, because it is applied to the mark
 * rather than written into each family's definition.
 */
class GlobeSymbolTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final int WIDE_PX = 1200;

    private static final int HIGH_PX = 800;

    private static final SkyPosition PAGE =
            new SkyPosition(266.0, -28.0);

    private static final ChartPalette PAPER = ChartPalette.WHITE_PAPER;

    /** As documented on the transform: 360 samples of a sphere's ellipse. */
    private static final double NUMERICAL_ERROR = 0.02;

    @Test
    void everyDimensionedFamilyInheritsTheForeshortening() {
        // Family by family, from the vocabulary itself rather than
        // from a list written here: a family added to the enum later
        // joins this test by existing. What is asserted is that the
        // same object drawn near the limb covers about the cosine of
        // what it covers at the centre - which is what the transform
        // does, and which no family can opt out of because the
        // transform is applied to the composed mark.
        int checked = 0;
        for (DsoType type : DsoType.values()) {
            if (ChartRenderer.symbolForType(type)
                    == ChartRenderer.Symbol.NONE) {
                continue;
            }
            int atCentre = inkOf(type, 0.0);
            int nearLimb = inkOf(type, 70.0);
            if (atCentre == 0) {
                continue;
            }
            checked++;
            double kept = nearLimb / (double) atCentre;
            assertTrue(kept < 0.75,
                    type + " is not foreshortened near the limb: it"
                            + " keeps " + kept + " of the ink it lays"
                            + " down at the centre, where the sphere"
                            + " has turned away by cos(70) = 0.34");
            assertTrue(nearLimb > 0,
                    type + " vanished entirely, which is a different"
                            + " fault from not being foreshortened");
        }
        assertTrue(checked >= 5,
                "every drawn family was measured, not a sample: "
                        + checked);
    }

    @Test
    void aMarkAtThePageCentreIsTheMarkEveryOtherPageDraws() {
        // At the centre the two footprints are the same ellipse, so
        // the transform is the identity to within the sampling error
        // its own documentation states. The mark a globe draws there
        // is therefore the mark a 42-degree page draws, and this
        // compares the ink rather than the matrix.
        // Compared as the mark's own size and shape rather than as a
        // count of inked pixels. The released vocabulary is published
        // as an Area - the union of its stroked pieces - while the
        // renderer strokes each piece in turn, so a box's four
        // corners are covered once by the first and twice by the
        // second. That difference is about how two ways of asking
        // count an overlap, and it reported an 8 per cent
        // disagreement about a nebula that is the same nebula.
        for (DsoType type : new DsoType[] {DsoType.GALAXY,
                DsoType.OPEN_CLUSTER, DsoType.GLOBULAR_CLUSTER,
                DsoType.NEBULA, DsoType.PLANETARY_NEBULA}) {
            int[] onTheGlobe = inkBoxOf(object(type,
                    awayFromCentre(0.0), FIXTURE_ARCMIN, FIXTURE_ARCMIN,
                    0.0));
            int[] released = plainInkBoxOf(type);
            assertTrue(released[0] > 0 && released[1] > 0,
                    type + " draws something");
            assertEquals(released[0], onTheGlobe[0], 1,
                    type + " at a globe's centre is " + onTheGlobe[0]
                            + " px across where the released glyph is "
                            + released[0]);
            assertEquals(released[1], onTheGlobe[1], 1,
                    type + " at a globe's centre is " + onTheGlobe[1]
                            + " px tall where the released glyph is "
                            + released[1]);
        }
    }

    @Test
    void theCloudIsDrawnAtItsProjectedProportions() {
        // The fixture the rule exists for: sized at the page centre's
        // rate the cloud is nearly round and hangs over the limb;
        // projected, it is a long thin ellipse lying along the limb.
        DeepSkyObject cloud = object(DsoType.GALAXY,
                new SkyPosition(80.894, -69.756), 645.0, 550.0, 170.0);
        int[] box = inkBoxOf(cloud);
        assertTrue(box[0] > 0 && box[1] > 0, "the cloud is drawn");

        double longer = Math.max(box[0], box[1]);
        double shorter = Math.min(box[0], box[1]);
        assertTrue(longer / shorter > 3.0,
                "the cloud is drawn long and thin, as a sphere seen"
                        + " edge-on makes it: " + box[0] + " by "
                        + box[1] + " px");

        // And it agrees with what the geometry measured, rather than
        // with a shape that merely looks squashed.
        SkyFootprint.Extent extent = SkyFootprint.extentOn(
                Projections.of(ChartProjection.ORTHOGRAPHIC, PAGE),
                mapping(), cloud.position(), cloud.majorAxisArcmin(),
                cloud.minorAxisArcmin(), cloud.positionAngleDegrees());
        assertEquals(extent.majorPx() / extent.minorPx(),
                longer / shorter, 0.35 * extent.majorPx() / extent.minorPx(),
                "the ink's proportions are the footprint's: measured "
                        + extent.majorPx() + " by " + extent.minorPx());
    }

    @Test
    void aStraddlerDrawsItsVisiblePartAndNoMore() {
        // An object whose outline really crosses the limb. Projection
        // decides its shape; the page's clip decides where the page
        // ends. Both are checked: it draws, and nothing it draws is
        // past the stroke edge.
        DeepSkyObject straddler = object(DsoType.NEBULA,
                awayFromCentre(88.0), 8.0 * 60.0, 8.0 * 60.0, 0.0);
        List<juranometria.project.PlanePoint> outline =
                SkyFootprint.projected(
                        Projections.of(ChartProjection.ORTHOGRAPHIC, PAGE),
                        straddler.position(), straddler.majorAxisArcmin(),
                        straddler.minorAxisArcmin(),
                        straddler.positionAngleDegrees());
        assertTrue(outline.size() > 0
                        && outline.size() < SkyFootprint.SAMPLES,
                "the fixture straddles: " + outline.size() + " of "
                        + SkyFootprint.SAMPLES + " outline points are"
                        + " on the page");

        BufferedImage page = draw(sceneWith(straddler));
        int inside = 0;
        double deepest = 0.0;
        double disc = 0.90 * HIGH_PX / 2.0;
        for (int y = 0; y < HIGH_PX; y++) {
            for (int x = 0; x < WIDE_PX; x++) {
                if ((page.getRGB(x, y) & 0xffffff)
                        == (PAPER.ground().getRGB() & 0xffffff)) {
                    continue;
                }
                if (x < 3 || y < 3 || x >= WIDE_PX - 3
                        || y >= HIGH_PX - 3) {
                    continue;
                }
                double out = Math.hypot(x + 0.5 - WIDE_PX / 2.0,
                        y + 0.5 - HIGH_PX / 2.0) - disc;
                if (out <= 0.0) {
                    inside++;
                } else {
                    deepest = Math.max(deepest, out);
                }
            }
        }
        assertTrue(inside > 0,
                "its visible part is drawn rather than dropped");
        assertTrue(deepest <= 1.5,
                "and its hidden part is not: ink reaches " + deepest
                        + " px beyond the limb, past the painted edge"
                        + " a clipped stroke can account for");
    }

    @Test
    void aMarkTooSmallFallsBackToItsFamilysMinimumGlyph() {
        // The major span against the practical minimum: the reader
        // cannot see this object's own shape, so they are shown the
        // family's mark instead.
        // Well inside the disc, so what is measured is the fallback
        // and not the page's clip: a mark at 89 degrees out is half
        // over the limb and cut, which is a different rule.
        DeepSkyObject small = object(DsoType.OPEN_CLUSTER,
                awayFromCentre(30.0), 8.0, 8.0, 0.0);
        SkyFootprint.Extent extent = extentOf(small);
        assertTrue(extent.majorPx()
                        < RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                "the fixture fails the major test: " + extent.majorPx()
                        + " px");
        assertItIsTheMinimumGlyph(small, "too small to resolve");
    }

    @Test
    void aMarkTooThinFallsBackToItsFamilysMinimumGlyph() {
        // The other route, and the one a single threshold would have
        // missed: long enough to resolve, and so foreshortened that
        // its two sides would merge into one bar carrying no family
        // at all.
        // Elongated rather than near the limb, for the same reason:
        // an object 81 by 25 arcmin at 45 degrees out projects to
        // about 8.5 by 1.8 px - long enough to resolve, too thin to
        // carry a family - and sits nowhere near the edge.
        DeepSkyObject flattened = object(DsoType.OPEN_CLUSTER,
                awayFromCentre(45.0), 81.0, 25.0, 90.0);
        SkyFootprint.Extent extent = extentOf(flattened);
        assertTrue(extent.majorPx()
                        >= RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                "the fixture passes the major test: " + extent.majorPx()
                        + " px");
        assertTrue(extent.minorPx() < 2.0,
                "and fails the minor one: " + extent.minorPx() + " px");
        assertItIsTheMinimumGlyph(flattened, "too thin to read");
    }

    /**
     * That a fallen-back mark is the family's own minimum glyph,
     * decided by comparing it with all five of them.
     *
     * <p>"Still drawn" was the first version of this and is satisfied
     * by any mark at all. Bounds are satisfied by any mark of the
     * right size. Comparing pixel for pixel against one reference
     * turned out to measure the two drawing routes instead - a dashed
     * ring stroked by the renderer and the same ring built as a
     * stroked shape put their dashes in different places, and a
     * nebula's box counted its corners once or twice depending which
     * way it was asked.
     *
     * <p>So the question is put as the reader's: of the five marks
     * the atlas could have drawn here, which one is this? The answer
     * has to be the object's own family, and by a margin - if a
     * globular's cross were drawn where an open cluster's ring
     * belongs, the nearest match would be the wrong family.
     */
    private static void assertItIsTheMinimumGlyph(DeepSkyObject dso,
                                                  String why) {
        int[][] drawn = maskOf(draw(sceneWith(dso)), draw(emptyScene()));
        assertTrue(drawn.length > 0, why + ": something is drawn");

        int[][] minimum = maskOf(minimumGlyphAt(dso, dso.type()));
        assertEquals(minimum.length, drawn.length, 1,
                why + ": the mark is the minimum glyph's own height -"
                        + " not the footprint's, which is what a"
                        + " fallback that forgot to fall back would"
                        + " draw");
        assertEquals(minimum[0].length, drawn[0].length, 1,
                why + ": and its width");
        assertTrue(shapeApart(drawn, minimum) < 0.35,
                why + ": and its shape - " + shapeApart(drawn, minimum)
                        + " of the two marks' pixels differ, over two"
                        + " drawing routes that place a dash"
                        + " differently");

        // And it is not the footprint-sized mark, which is the thing
        // the fallback exists to refuse.
        SkyFootprint.Extent extent = extentOf(dso);
        assertTrue(Math.abs(drawn[0].length - extent.majorPx()) > 1.0
                        || Math.abs(drawn.length - extent.majorPx()) > 1.0
                        || extent.majorPx()
                                < RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                why + ": the mark drawn is not the footprint, which"
                        + " measures " + extent.majorPx() + " by "
                        + extent.minorPx() + " px");
    }

    /**
     * How far two marks are from being the same shape: the share of
     * pixels that differ, over the larger of the two boxes, with each
     * mark taken from its own bounding box so a sub-pixel difference
     * in where they sit is not counted as a difference in what they
     * are.
     */
    private static double shapeApart(int[][] one, int[][] other) {
        int high = Math.max(one.length, other.length);
        int wide = Math.max(one.length == 0 ? 0 : one[0].length,
                other.length == 0 ? 0 : other[0].length);
        if (high == 0 || wide == 0) {
            return 1.0;
        }
        int differing = 0;
        int inked = 0;
        for (int y = 0; y < high; y++) {
            for (int x = 0; x < wide; x++) {
                int a = y < one.length && x < one[y].length
                        ? one[y][x] : 0;
                int b = y < other.length && x < other[y].length
                        ? other[y][x] : 0;
                if (a == 1 || b == 1) {
                    inked++;
                }
                if (a != b) {
                    differing++;
                }
            }
        }
        return inked == 0 ? 1.0 : differing / (double) inked;
    }

    /** The released minimum glyph for a family, where this object is. */
    private static BufferedImage minimumGlyphAt(DeepSkyObject dso,
                                                DsoType family) {
        SkyFootprint.Extent extent = extentOf(dso);
        BufferedImage tile = new BufferedImage(WIDE_PX, HIGH_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tile.createGraphics();
        try {
            g.setColor(PAPER.ground());
            g.fillRect(0, 0, WIDE_PX, HIGH_PX);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            // The atlas's own published answer to "what does this
            // glyph ink", which #313 exposed for exactly this kind of
            // question rather than having callers rebuild it.
            double minimum =
                    RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX;
            g.setColor(java.awt.Color.BLACK);
            g.fill(ChartRenderer.symbolInk(
                    ChartRenderer.symbolForType(family),
                    extent.centreX(), extent.centreY(), minimum,
                    minimum, dso.positionAngleDegrees()));
        } finally {
            g.dispose();
        }
        return tile;
    }

    /** One mark's ink, cut from its own bounding box. */
    private static int[][] maskOf(BufferedImage page) {
        return maskOf(page, null);
    }

    /** Ink that belongs to the object rather than to the page. */
    private static boolean marked(BufferedImage page,
                                  BufferedImage without, int x, int y) {
        return without == null ? isInk(page, x, y)
                : (page.getRGB(x, y) & 0xffffff)
                        != (without.getRGB(x, y) & 0xffffff);
    }

    /**
     * The same, against the page as it would be without the object -
     * so that the page's own furniture, its border and its limb, is
     * not read as part of the mark.
     */
    private static int[][] maskOf(BufferedImage page,
                                  BufferedImage without) {
        int minX = WIDE_PX;
        int minY = HIGH_PX;
        int maxX = -1;
        int maxY = -1;
        for (int y = 3; y < HIGH_PX - 3; y++) {
            for (int x = 3; x < WIDE_PX - 3; x++) {
                if (!marked(page, without, x, y)) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < 0) {
            return new int[0][0];
        }
        int[][] mask = new int[maxY - minY + 1][maxX - minX + 1];
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                mask[y - minY][x - minX] =
                        marked(page, without, x, y) ? 1 : 0;
            }
        }
        return mask;
    }

    private static boolean isInk(BufferedImage page, int x, int y) {
        return (page.getRGB(x, y) & 0xffffff)
                != (PAPER.ground().getRGB() & 0xffffff);
    }

    private static SkyFootprint.Extent extentOf(DeepSkyObject dso) {
        return SkyFootprint.extentOn(
                Projections.of(ChartProjection.ORTHOGRAPHIC, PAGE),
                mapping(), dso.position(), dso.majorAxisArcmin(),
                dso.minorAxisArcmin(), dso.positionAngleDegrees());
    }

    @Test
    void aRoundMarkAtTheCentreIsNotTurnedByAnArbitraryFit() {
        // A round footprint has no orientation, and the second-moment
        // fit says so by answering arbitrarily - xx and yy equal, xy
        // nothing, and the tilt half the angle of a vector that is
        // numerically zero. Composed with the mark's own frame that
        // turned the mark: a nebula at a globe's centre came out 24 px
        // across where every other page draws it 20.
        //
        // The elongated fixture in SkyFootprintTest cannot see this,
        // because its tilt is well determined. A square glyph on a
        // circular object can, and so can the transform itself.
        var transform = SkyFootprint.foreshortening(
                Projections.of(ChartProjection.ORTHOGRAPHIC, PAGE),
                mapping(), PAGE, FIXTURE_ARCMIN, FIXTURE_ARCMIN, 0.0);
        assertTrue(transform.offTheIdentity() < 1.0e-3,
                "a round mark at the page centre is left alone: "
                        + transform);

        int[] box = inkBoxOf(object(DsoType.NEBULA, PAGE,
                FIXTURE_ARCMIN, FIXTURE_ARCMIN, 0.0));
        int[] released = plainInkBoxOf(DsoType.NEBULA);
        assertEquals(released[0], box[0], 1,
                "and its box is square-on rather than turned: "
                        + box[0] + " by " + box[1] + " px against "
                        + released[0] + " by " + released[1]);
        assertEquals(released[1], box[1], 1);
        assertEquals(box[0], box[1], 1,
                "a square glyph on a round object is drawn square");
    }

    /**
     * How large the fixtures are, in arcminutes.
     *
     * <p>Three degrees, and the size is load-bearing. A whole
     * hemisphere across 720 px makes an arcminute about a third of a
     * pixel, so a 40-arcmin object is 4.2 px on a globe - under the
     * practical minimum, drawn as its family's minimum glyph at the
     * centre and at the limb alike. A first draft of this test used
     * one and reported that galaxies keep 1.0 of their ink near the
     * limb: perfectly true, and about the fallback rather than about
     * foreshortening. These fixtures resolve at both ends so the
     * transform is what is being measured.
     */
    private static final double FIXTURE_ARCMIN = 180.0;

    /** Ink laid down by one object of this family, a stated angle out. */
    private static int inkOf(DsoType type, double degreesOut) {
        DeepSkyObject dso = object(type, awayFromCentre(degreesOut),
                FIXTURE_ARCMIN, FIXTURE_ARCMIN, 0.0);
        BufferedImage page = draw(sceneWith(dso));
        BufferedImage without = draw(emptyScene());
        int inked = 0;
        for (int y = 0; y < HIGH_PX; y++) {
            for (int x = 0; x < WIDE_PX; x++) {
                if ((page.getRGB(x, y) & 0xffffff)
                        != (without.getRGB(x, y) & 0xffffff)) {
                    inked++;
                }
            }
        }
        return inked;
    }

    /**
     * The same page without the object on it: its border and its own
     * limb, and nothing else.
     *
     * <p>The mark is measured as the difference between the two,
     * which is the only way to ask what one object inks on a page
     * that draws furniture of its own. Excluding the furniture by
     * where it sits would not do here - the limb runs through the
     * band these fixtures are placed in, and a mark near it would be
     * measured short.
     */
    private static ChartScene emptyScene() {
        return new ChartScene(new ChartViewport(PAGE, 180.0, WIDE_PX,
                HIGH_PX, ChartProjection.ORTHOGRAPHIC), List.of(),
                List.of(), "no object", 8.0);
    }

    /** The released vocabulary's own glyph, as a bounding box. */
    private static int[] plainInkBoxOf(DsoType type) {
        double axis = Math.toRadians(FIXTURE_ARCMIN / 60.0)
                * mapping().pixelsPerPlaneUnit();
        BufferedImage tile = new BufferedImage(WIDE_PX, HIGH_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tile.createGraphics();
        try {
            g.setColor(PAPER.ground());
            g.fillRect(0, 0, WIDE_PX, HIGH_PX);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(java.awt.Color.BLACK);
            g.fill(ChartRenderer.symbolInk(
                    ChartRenderer.symbolForType(type), WIDE_PX / 2.0,
                    HIGH_PX / 2.0, axis, axis, 0.0));
        } finally {
            g.dispose();
        }
        return boxOf(tile);
    }

    /** The same glyph as the released vocabulary draws it. */
    private static int plainInkOf(DsoType type) {
        double axis = Math.toRadians(FIXTURE_ARCMIN / 60.0)
                * mapping().pixelsPerPlaneUnit();
        BufferedImage tile = new BufferedImage(WIDE_PX, HIGH_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tile.createGraphics();
        try {
            g.setColor(PAPER.ground());
            g.fillRect(0, 0, WIDE_PX, HIGH_PX);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(java.awt.Color.BLACK);
            g.fill(ChartRenderer.symbolInk(
                    ChartRenderer.symbolForType(type), WIDE_PX / 2.0,
                    HIGH_PX / 2.0, axis, axis, 0.0));
        } finally {
            g.dispose();
        }
        int ground = PAPER.ground().getRGB() & 0xffffff;
        int inked = 0;
        for (int y = 0; y < HIGH_PX; y++) {
            for (int x = 0; x < WIDE_PX; x++) {
                if ((tile.getRGB(x, y) & 0xffffff) != ground) {
                    inked++;
                }
            }
        }
        return inked;
    }

    /** The bounding box of one object's ink, in page pixels. */
    private static int[] inkBoxOf(DeepSkyObject dso) {
        return boxOf(draw(sceneWith(dso)), draw(emptyScene()));
    }

    private static int[] boxOf(BufferedImage page) {
        return boxOf(page, null);
    }

    private static int[] boxOf(BufferedImage page,
                               BufferedImage without) {
        int ground = PAPER.ground().getRGB() & 0xffffff;
        int minX = WIDE_PX;
        int minY = HIGH_PX;
        int maxX = -1;
        int maxY = -1;
        for (int y = 3; y < HIGH_PX - 3; y++) {
            for (int x = 3; x < WIDE_PX - 3; x++) {
                int here = page.getRGB(x, y) & 0xffffff;
                if (without == null ? here == ground
                        : here == (without.getRGB(x, y) & 0xffffff)) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX < 0 ? new int[] {0, 0}
                : new int[] {maxX - minX + 1, maxY - minY + 1};
    }

    private static BufferedImage draw(ChartScene scene) {
        BufferedImage canvas = new BufferedImage(WIDE_PX, HIGH_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                    .render(g, scene, symbolsOnly());
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private static ChartScene sceneWith(DeepSkyObject dso) {
        return new ChartScene(new ChartViewport(PAGE, 180.0, WIDE_PX,
                HIGH_PX, ChartProjection.ORTHOGRAPHIC), List.of(),
                List.of(dso), "one object", 8.0);
    }

    private static DeepSkyObject object(DsoType type, SkyPosition where,
                                        double majorArcmin,
                                        double minorArcmin,
                                        double positionAngle) {
        return new DeepSkyObject("fixture", new ArrayList<>(), type,
                where, majorArcmin, minorArcmin, positionAngle, 5.0, 1,
                new DeepSkyObject.Recorded(majorArcmin, minorArcmin,
                        positionAngle,
                        DeepSkyObject.Recorded.Band.VISUAL));
    }

    /** A position a stated angle from the page centre. */
    private static SkyPosition awayFromCentre(double degrees) {
        double dec = PAGE.decDegrees() - degrees;
        if (dec < -90.0) {
            return new SkyPosition((PAGE.raDegrees() + 180.0) % 360.0,
                    -180.0 - dec);
        }
        return new SkyPosition(PAGE.raDegrees(), dec);
    }

    private static ViewportMapping mapping() {
        ChartViewport viewport = new ChartViewport(PAGE, 180.0, WIDE_PX,
                HIGH_PX, ChartProjection.ORTHOGRAPHIC);
        return new ViewportMapping(viewport,
                Projections.of(ChartProjection.ORTHOGRAPHIC, PAGE));
    }

    /** Deep-sky symbols alone: no grid, no geography, no words. */
    private static ChartOptions symbolsOnly() {
        return new ChartOptions(true, false, false, false, false, false,
                false, false, false, false, false, true, true, true,
                true, true, PAPER);
    }
}
