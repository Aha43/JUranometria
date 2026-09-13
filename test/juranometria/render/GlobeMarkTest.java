package juranometria.render;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One answer about a mark (Sprint 32, issue #331, review P1s).
 *
 * <p>The first correction of an extended object's shape was made in
 * the painter, after {@link ChartRenderer#drawnMarks} had already
 * published what exists and what shape it has. Everything downstream
 * reads that publication - hit testing, label obstacles, the page
 * inventory, selection highlighting, stacking - so a globe could
 * paint a thin sliver near the limb while still accepting clicks
 * over, and reserving label space for, the large ellipse the mark
 * used to be. The correction also promoted every collapsed object to
 * a minimum glyph, which the settled regional policy allows only for
 * a Messier landmark or the reader's searched target.
 *
 * <p>These hold the one rule that closes both: <strong>what exists,
 * what is painted, what can be clicked, what blocks a label and what
 * enters the inventory are the same answer</strong>, decided once,
 * before the mark is published.
 */
class GlobeMarkTest {

    private static final int WIDE_PX = 1200;

    private static final int HIGH_PX = 800;

    private static final SkyPosition PAGE = new SkyPosition(266.0, -28.0);

    private static final ChartPalette PAPER = ChartPalette.WHITE_PAPER;

    /**
     * An object big enough that the page centre's rate calls it
     * resolved - it is 180 arcminutes, which is 15.7 px - and far
     * enough out that its projected footprint is not. At 87 degrees
     * the radial span is cut by cos(87) = 1/19.
     */
    private static final double FIXTURE_ARCMIN = 180.0;

    private static final double COLLAPSING_DEGREES = 87.0;

    /** Ordinary: neither a Messier landmark nor anyone's target. */
    private static final int ORDINARY_PRIORITY = 3;

    /** Messier priority, which the regional policy always draws. */
    private static final int LANDMARK_PRIORITY = 1;

    @Test
    void anOrdinaryObjectWhoseProjectedFootprintCollapsesIsWithdrawn() {
        DeepSkyObject collapsed = object(DsoType.NEBULA,
                awayFromCentre(COLLAPSING_DEGREES), ORDINARY_PRIORITY);

        // The premise, stated rather than assumed: the old
        // centre-scale question admits this object, so it is on the
        // page today for a reason that the projection has since
        // disproved. Without this the test could pass by the object
        // never having been eligible at all.
        ChartScene scene = sceneWith(collapsed);
        RegionalDetailPolicy policy = new RegionalDetailPolicy(scene,
                mapping().pixelsPerPlaneUnit());
        assertTrue(policy.drawn(collapsed),
                "the fixture is admitted by the centre-scale rule, so"
                        + " withdrawing it is a decision and not an"
                        + " object that was never there");
        assertFalse(policy.clampAllowed(collapsed),
                "and it is not one of the two the policy promotes");

        assertTrue(marksOf(scene).isEmpty(),
                "an object admitted only because centre-scale said it"
                        + " resolved, whose real footprint does not,"
                        + " has no remaining reason to be on the page");
        assertEquals(0, inkOf(scene),
                "and nothing of it is painted either - the page model"
                        + " and the page agree that it is not there");
    }

    @Test
    void aMessierLandmarkKeepsItsMinimumGlyph() {
        keepsItsMinimumGlyph(sceneWith(object(DsoType.NEBULA,
                        awayFromCentre(COLLAPSING_DEGREES),
                        LANDMARK_PRIORITY)),
                "a Messier landmark");
    }

    @Test
    void theSearchedTargetKeepsItsMinimumGlyph() {
        DeepSkyObject searched = object(DsoType.NEBULA,
                awayFromCentre(COLLAPSING_DEGREES), ORDINARY_PRIORITY);
        keepsItsMinimumGlyph(new ChartScene(viewport(), List.of(),
                        List.of(searched), "one object", 8.0,
                        searched.id()),
                "the object the reader searched for");
    }

    /**
     * The two routes the settled policy does promote. Their reason
     * for being drawn was never their size, so a footprint that
     * collapses takes their shape away and not their place.
     */
    private static void keepsItsMinimumGlyph(ChartScene scene, String who) {
        List<ChartRenderer.DrawnMark> marks = marksOf(scene);
        assertEquals(1, marks.size(),
                who + " stays on the page when its footprint"
                        + " collapses: " + marks.size() + " marks");
        ChartRenderer.DrawnMark mark = marks.get(0);
        assertNotNull(mark.painted());
        assertEquals(RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                mark.painted().majorPx(), 1.0e-9,
                who + " is drawn at the family's minimum glyph");
        assertEquals(mark.painted().majorPx(), mark.painted().minorPx(),
                1.0e-9,
                "in both axes, rather than at the centre-scale size"
                        + " this step exists to stop trusting");
        assertTrue(mark.painted().carried() == null,
                "and painted as built, because a minimum glyph is not"
                        + " a footprint to be carried anywhere");
        assertTrue(inkOf(scene) > 0, who + " is painted");
    }

    @Test
    void whatIsPaintedIsWhatCanBeClickedAndWhatBlocksALabel() {
        // The fault this closes, measured: the published mark used to
        // be the uncorrected ellipse while the paint was the
        // foreshortened one. An object well inside the limb, drawn
        // large enough to keep its own shape, so the comparison is
        // about the correction and not about the fallback.
        ChartScene scene = sceneWith(object(DsoType.GALAXY,
                awayFromCentre(70.0), LANDMARK_PRIORITY));
        List<ChartRenderer.DrawnMark> marks = marksOf(scene);
        assertEquals(1, marks.size());
        ChartRenderer.DrawnMark mark = marks.get(0);
        assertNotNull(mark.painted().carried(),
                "the fixture is carried rather than falling back, so"
                        + " the published and painted shapes could"
                        + " differ if they came from two derivations");

        Rectangle2D published = mark.ink().getBounds2D();
        int[] painted = paintedBox(scene);
        assertTrue(painted[0] > 0, "the fixture is painted");
        // Bounds against bounds, to a pixel of stroke and rounding.
        assertEquals(painted[0], published.getWidth(), 2.0,
                "what a label must avoid is " + published.getWidth()
                        + " px wide where the page paints " + painted[0]);
        assertEquals(painted[1], published.getHeight(), 2.0,
                "and " + published.getHeight() + " px tall where the"
                        + " page paints " + painted[1]);

        // The same object's uncorrected footprint, which is what the
        // publication used to be. If it were still that, the check
        // above could not have passed - this states the size of the
        // disagreement rather than leaving it implied.
        double[] uncorrected = ChartRenderer.symbolAxesPx(
                scene.deepSkyObjects().get(0),
                new RegionalDetailPolicy(scene,
                        mapping().pixelsPerPlaneUnit()),
                mapping().pixelsPerPlaneUnit());
        assertTrue(uncorrected[0] > 1.5 * published.getHeight(),
                "the uncorrected mark was " + uncorrected[0] + " px"
                        + " across against the " + published.getHeight()
                        + " px now published - a disagreement a reader"
                        + " would have felt as a click landing on"
                        + " nothing");
    }

    @Test
    void aSelectionRingIsDrawnAroundTheMarkAsPainted() {
        // An object lying ALONG the radius, so the axis the
        // projection shortens is its major one - the case that
        // separates a reach taken from the measured footprint from a
        // reach scaled by the map. A map that only squashes leaves
        // one direction alone, so its largest stretch is 1, and a
        // reach carried that way would not move at all.
        DeepSkyObject lengthwise = new DeepSkyObject("lengthwise",
                new ArrayList<>(), DsoType.GALAXY, awayFromCentre(70.0),
                300.0, 90.0, 0.0, 5.0, LANDMARK_PRIORITY,
                new DeepSkyObject.Recorded(300.0, 90.0, 0.0,
                        DeepSkyObject.Recorded.Band.VISUAL));
        ChartScene scene = sceneWith(lengthwise);
        List<ChartRenderer.DrawnMark> marks = marksOf(scene);
        assertEquals(1, marks.size());
        ChartRenderer.DrawnMark mark = marks.get(0);
        assertNotNull(mark.painted().carried(), "the fixture is carried");

        int[] painted = paintedBox(scene);
        double across = Math.max(painted[0], painted[1]);
        assertEquals(across / 2.0, mark.reach(), 2.0,
                "a selection ring is drawn at reach " + mark.reach()
                        + " around a mark the page paints " + across
                        + " px across");

        double[] uncorrected = ChartRenderer.symbolAxesPx(lengthwise,
                new RegionalDetailPolicy(scene,
                        mapping().pixelsPerPlaneUnit()),
                mapping().pixelsPerPlaneUnit());
        assertTrue(uncorrected[0] / 2.0 > 2.0 * mark.reach(),
                "and the fixture really does separate the two"
                        + " answers: the uncorrected reach is "
                        + uncorrected[0] / 2.0 + " against "
                        + mark.reach());
    }

    @Test
    void stackingFollowsThePaintedFootprintRatherThanTheCatalogueSize() {
        // Two objects the reader sees at once: a large one strongly
        // foreshortened near the limb, and a smaller one that keeps
        // its shape. At the centre's rate the first is the larger and
        // would be painted first, behind; once the projection has had
        // its way it is the smaller of the two and must come forward,
        // or it would be buried by a companion it is now smaller than.
        DeepSkyObject flattened = at("flattened", DsoType.GALAXY,
                awayFromCentre(80.0), 240.0);
        DeepSkyObject upright = at("upright", DsoType.GALAXY,
                awayFromCentre(5.0), 120.0);
        ChartScene scene = new ChartScene(viewport(), List.of(),
                List.of(flattened, upright), "two objects", 8.0);

        List<ChartRenderer.DrawnMark> marks = marksOf(scene);
        assertEquals(2, marks.size());
        assertEquals("upright", marks.get(0).deepSky().id(),
                "the larger painted footprint goes behind, and near"
                        + " the limb that is no longer the larger"
                        + " object");
        assertTrue(area(marks.get(0)) > area(marks.get(1)),
                "which is what the order means: "
                        + area(marks.get(0)) + " px behind "
                        + area(marks.get(1)));
    }

    /** The painted area of a carried mark, for the stacking claim. */
    private static double area(ChartRenderer.DrawnMark mark) {
        ChartRenderer.DrawnMark.Painted painted = mark.painted();
        double built = Math.PI * painted.majorPx() * painted.minorPx()
                / 4.0;
        return painted.carried() == null ? built
                : built * painted.carried().areaFactor();
    }

    private static List<ChartRenderer.DrawnMark> marksOf(ChartScene scene) {
        return new ChartRenderer(StarSizePolicy.DEFAULT)
                .drawnMarks(scene, symbolsOnly());
    }

    /**
     * How many pixels of <em>sky</em> the page inks. The page's own
     * circular frame is furniture - it is drawn whatever is on the
     * globe, and counting it would make "nothing is painted" read as
     * 3,996 px of something (#331, step one).
     */
    private static int inkOf(ChartScene scene) {
        BufferedImage canvas = draw(scene);
        int inked = 0;
        for (int y = 0; y < HIGH_PX; y++) {
            for (int x = 0; x < WIDE_PX; x++) {
                if (insideTheLimb(x, y)
                        && (canvas.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    inked++;
                }
            }
        }
        return inked;
    }

    /** The width and height of what the page inks on the sky. */
    private static int[] paintedBox(ChartScene scene) {
        BufferedImage canvas = draw(scene);
        int minX = WIDE_PX;
        int minY = HIGH_PX;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < HIGH_PX; y++) {
            for (int x = 0; x < WIDE_PX; x++) {
                if (insideTheLimb(x, y)
                        && (canvas.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? new int[] {0, 0}
                : new int[] {maxX - minX + 1, maxY - minY + 1};
    }

    /**
     * Sky rather than furniture: inside the limb by more than the
     * frame's own stroke, so the circle the page always draws is not
     * mistaken for something on the globe.
     */
    private static boolean insideTheLimb(int x, int y) {
        double limb = mapping().pixelsPerPlaneUnit()
                * juranometria.project.Projections.of(
                        ChartProjection.ORTHOGRAPHIC, PAGE)
                .visiblePlaneRadius();
        return Math.hypot(x + 0.5 - WIDE_PX / 2.0,
                y + 0.5 - HIGH_PX / 2.0) < limb - 2.0;
    }

    private static BufferedImage draw(ChartScene scene) {
        BufferedImage canvas = new BufferedImage(WIDE_PX, HIGH_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, scene, symbolsOnly());
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private static ChartViewport viewport() {
        return new ChartViewport(PAGE, 180.0, WIDE_PX, HIGH_PX,
                ChartProjection.ORTHOGRAPHIC);
    }

    private static juranometria.project.ViewportMapping mapping() {
        return new juranometria.project.ViewportMapping(viewport(),
                juranometria.project.Projections.of(
                        ChartProjection.ORTHOGRAPHIC, PAGE));
    }

    private static ChartScene sceneWith(DeepSkyObject dso) {
        return new ChartScene(viewport(), List.of(), List.of(dso),
                "one object", 8.0);
    }

    private static DeepSkyObject object(DsoType type, SkyPosition where,
                                        int priority) {
        return new DeepSkyObject("fixture", new ArrayList<>(), type,
                where, FIXTURE_ARCMIN, FIXTURE_ARCMIN, 0.0, 5.0,
                priority, recorded(FIXTURE_ARCMIN));
    }

    private static DeepSkyObject at(String id, DsoType type,
                                    SkyPosition where, double arcmin) {
        return new DeepSkyObject(id, new ArrayList<>(), type, where,
                arcmin, arcmin, 0.0, 5.0, LANDMARK_PRIORITY,
                recorded(arcmin));
    }

    private static DeepSkyObject.Recorded recorded(double arcmin) {
        return new DeepSkyObject.Recorded(arcmin, arcmin, 0.0,
                DeepSkyObject.Recorded.Band.VISUAL);
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

    /** Deep-sky symbols alone: no grid, no geography, no words. */
    private static ChartOptions symbolsOnly() {
        return new ChartOptions(true, false, false, false, false, false,
                false, false, false, false, false, true, true, true,
                true, true, PAPER);
    }
}
