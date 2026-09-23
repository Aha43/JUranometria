package juranometria.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.PixelPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coordinate-grid contract of docs/decisions/coordinate-grid.md,
 * now locked against the production seam itself (issue #133): east-left RA
 * ordering, RA-wrap continuity, signed declination and 0h notation,
 * polar convergence without runaway density, honest clipping, the
 * adaptive spacing rule across every released field, deterministic
 * output, and the stated 0.05 px approximation tolerance.
 */
class EquatorialGridTest {

    private static final double TOLERANCE_PX = 0.05;

    private static ChartViewport page(double ra, double dec, double field) {
        return new ChartViewport(new SkyPosition(ra, dec), field, 900, 700);
    }

    @Test
    void raRunsEastLeftAndZeroHoursWraps() {
        ChartViewport viewport = page(0.3, 45.0, 24.0);
        var grid = EquatorialGrid.gridFor(pageOf(viewport), null);
        // Bottom-edge RA labels: for any two, the greater RA (mod the
        // wrap around this page's centre) sits further LEFT.
        var raLabels = grid.labels().stream()
                .filter(label -> label.text().contains("h")).toList();
        assertTrue(raLabels.size() >= 3, "the wrap page labels its RA");
        assertTrue(raLabels.stream().anyMatch(
                        label -> label.text().equals("0h")),
                "the wrap meridian is labelled 0h");
        for (var a : raLabels) {
            for (var b : raLabels) {
                double raA = raOf(a.text());
                double raB = raOf(b.text());
                double delta = ((raB - raA) % 360.0 + 540.0) % 360.0 - 180.0;
                if (delta > 0 && Math.abs(delta) < 90) {
                    assertTrue(b.x() < a.x(),
                            "greater RA lands further left: " + a.text()
                                    + " vs " + b.text());
                }
            }
        }
    }

    private static double raOf(String label) {
        String[] parts = label.replace("h", "").replace("m", "").split(" ");
        double hours = Double.parseDouble(parts[0]);
        if (parts.length > 1) {
            hours += Double.parseDouble(parts[1]) / 60.0;
        }
        return hours * 15.0;
    }

    @Test
    void parallelsCrossTheWrapWithoutSeams() {
        // On the RA-0h page every visible parallel is one continuous
        // polyline: the sampling walks the sky, so RA 0 is nothing
        // special and no seam splits the curve there.
        var grid = EquatorialGrid.gridFor(pageOf(page(0.3, 45.0, 24.0)), null);
        for (List<PixelPoint> piece : grid.parallels()) {
            for (int i = 1; i < piece.size(); i++) {
                double jump = Math.hypot(
                        piece.get(i).x() - piece.get(i - 1).x(),
                        piece.get(i).y() - piece.get(i - 1).y());
                assertTrue(jump < 40.0,
                        "consecutive samples stay contiguous: " + jump);
            }
        }
        assertTrue(grid.parallels().size() >= 3, "parallels are present");
    }

    @Test
    void notationIsSignedAndCompact() {
        assertEquals("0h", EquatorialGrid.raLabel(0.0));
        assertEquals("6h", EquatorialGrid.raLabel(90.0));
        assertEquals("5h 40m", EquatorialGrid.raLabel(85.0));
        assertEquals("0h 30m", EquatorialGrid.raLabel(7.5));
        assertEquals("+41°", EquatorialGrid.decLabel(41.0, 1.0));
        assertEquals("−5°", EquatorialGrid.decLabel(-5.0, 5.0));
        assertEquals("+41° 30′", EquatorialGrid.decLabel(41.5, 0.5));
        assertEquals("−0° 30′", EquatorialGrid.decLabel(-0.5, 0.5));
    }

    @Test
    void thePoleConvergesWithoutSeamsOrRunawayDensity() {
        var grid = EquatorialGrid.gridFor(pageOf(page(37.946619, 89.9, 36.0)), null);
        assertEquals(90.0, grid.spec().raStepDegrees(),
                "the RA step reaches its 6h cap at the pole");
        assertTrue(grid.meridians().size() <= 8,
                "four radiating meridians, in clipped pieces - never a"
                        + " runaway fan: " + grid.meridians().size());
        assertTrue(grid.parallels().size() >= 5,
                "the parallels ring the pole");
        assertTrue(grid.maxChordErrorPx() < TOLERANCE_PX,
                "polar curves stay within the stated tolerance");
    }

    @Test
    void clippingIsHonestAndOutputDeterministic() {
        ChartViewport viewport = page(83.818667, -5.389667, 12.0);
        var grid = EquatorialGrid.gridFor(pageOf(viewport), null);
        for (var family : List.of(grid.meridians(), grid.parallels())) {
            for (List<PixelPoint> piece : family) {
                assertTrue(piece.size() >= 2);
                boolean touches = false;
                for (int i = 1; i < piece.size(); i++) {
                    if (new java.awt.geom.Line2D.Double(
                            piece.get(i - 1).x(), piece.get(i - 1).y(),
                            piece.get(i).x(), piece.get(i).y())
                            .intersects(0, 0, viewport.widthPx(),
                                    viewport.heightPx())) {
                        touches = true;
                    }
                }
                assertTrue(touches, "every kept piece touches the page");
            }
        }
        var again = EquatorialGrid.gridFor(pageOf(viewport), null);
        assertEquals(grid.meridians().size(), again.meridians().size());
        assertEquals(grid.parallels().size(), again.parallels().size());
        assertEquals(grid.labels(), again.labels(),
                "the grid is a pure deterministic function of the viewport");
    }

    @Test
    void everyReleasedFieldStaysWithinToleranceAndPleasantDensity() {
        for (double field : new double[] {1, 2, 3, 4, 6, 8, 12, 18, 24, 36}) {
            ChartViewport viewport = page(83.818667, -5.389667, field);
            var grid = EquatorialGrid.gridFor(pageOf(viewport), null);
            assertTrue(grid.maxChordErrorPx() < TOLERANCE_PX,
                    "tolerance holds at " + field + " degrees: "
                            + grid.maxChordErrorPx());
            double raSpacing = grid.spec().raStepDegrees()
                    * Math.cos(Math.toRadians(-5.389667))
                    * viewport.widthPx() / field;
            double decSpacing = grid.spec().decStepDegrees()
                    * viewport.widthPx() / field;
            assertTrue(raSpacing >= EquatorialGrid.MINIMUM_SPACING_PX,
                    "RA spacing respects the floor at " + field);
            assertTrue(decSpacing >= EquatorialGrid.MINIMUM_SPACING_PX,
                    "Dec spacing respects the floor at " + field);
            assertTrue(raSpacing < 2.6 * EquatorialGrid.MINIMUM_SPACING_PX,
                    "RA spacing stays pleasant at " + field);
            assertTrue(decSpacing < 2.6 * EquatorialGrid.MINIMUM_SPACING_PX,
                    "Dec spacing stays pleasant at " + field);
        }
    }

    @Test
    void oneExactBoundsCalculationGovernsPlacementContainmentAndSuppression() {
        // PR #137 review (P1): label geometry is the real font's, not
        // a guessed width, and a label that would clip the paper edge
        // after ordinary panning is suppressed by the same shared
        // calculation that governs title collisions and drawing.
        var metrics = EquatorialGrid.labelMetrics();
        boolean sawEdgeSuppression = false;
        for (int shift = 0; shift < 24; shift++) {
            ChartViewport viewport = new ChartViewport(
                    new SkyPosition(83.0 + shift * 0.11, -5.389667),
                    12.0, 900, 700);
            var grid = EquatorialGrid.gridFor(pageOf(viewport), null);
            for (var label : grid.labels()) {
                assertTrue(EquatorialGrid.fitsPaper(label, metrics, viewport),
                        "every emitted label's exact box lies on the paper:"
                                + " " + label);
            }
            // A meridian whose bottom crossing sits within one label
            // width of the right paper edge cannot carry a contained
            // label - the sweep must hit at least one such pan.
            long bottomRaLabels = grid.labels().stream()
                    .filter(label -> label.text().contains("h")).count();
            if (bottomRaLabels < grid.meridians().size()
                    && grid.suppressedLabels() > 0) {
                sawEdgeSuppression = true;
            }
        }
        assertTrue(sawEdgeSuppression,
                "the panning sweep exercised the paper-containment"
                        + " suppression, not just the happy path");

        // The shared calculation is the drawing geometry too: the box
        // of a known label is exactly the metrics' string bounds.
        var label = new EquatorialGrid.Label("5h 40m", 100.0, 690.0);
        var box = EquatorialGrid.labelBounds(label, metrics);
        assertEquals(metrics.stringWidth("5h 40m") + 2.0, box.getWidth());
        assertEquals(metrics.getHeight(), box.getHeight());
    }

    @Test
    void gridLabelsYieldToTheTitleBlockOnly() {
        ChartViewport viewport = page(10.684708, 41.268750, 8.0);
        var open = EquatorialGrid.gridFor(pageOf(viewport), null);
        var titled = EquatorialGrid.gridFor(pageOf(viewport),
                new java.awt.Rectangle(12, 560, 320, 128));
        assertTrue(titled.suppressedLabels() > 0,
                "labels under the title block are suppressed");
        assertEquals(open.labels().size(),
                titled.labels().size() + titled.suppressedLabels(),
                "suppression is exactly the title collisions, nothing else");
    }

    /**
     * A viewport as the page it would draw: these tests are about the
     * grid's geometry, and a grid is drawn on a page (#301).
     */
    /**
     * A curve that never reaches its preferred edge names itself
     * elsewhere (issue #360).
     *
     * <p>The reading problem this sprint exists for: on a wide page a
     * coordinate line can be drawn the whole way across and never
     * touch the edge its figure is allowed to live on. It was
     * anonymous, and a reader had to find a labelled neighbour and
     * count.
     */
    @Test
    void aCurveThatMissesItsPreferredEdgeIsNamedElsewhere() {
        ChartViewport viewport = page(0.3, 20.0, 120.0);
        var grid = EquatorialGrid.gridFor(pageOf(viewport), null);

        var offPreferred = grid.labels().stream()
                .filter(label -> !onPreferredEdge(label, viewport))
                .toList();
        // Both kinds, separately. #360 says fixing only declination
        // would leave the same failure rotated ninety degrees, and a
        // test that accepted either would not notice.
        assertTrue(offPreferred.stream()
                        .anyMatch(label -> label.text().contains("h")),
                "a right-ascension meridian that misses the bottom"
                        + " names itself elsewhere: " + grid.labels());
        assertTrue(offPreferred.stream()
                        .anyMatch(label -> !label.text().contains("h")),
                "and so does a declination parallel that misses the"
                        + " left: " + grid.labels());
        for (var label : offPreferred) {
            assertTrue(EquatorialGrid.fitsPaper(label,
                            EquatorialGrid.labelMetrics(), viewport),
                    "and it is on the paper: " + label);
        }
    }

    /**
     * The established convention is not disturbed where it works.
     *
     * <p><strong>Equatorial</strong> narrow pages, and the word is
     * load-bearing. Whether a curve reaches its preferred edge turns
     * out to depend on declination rather than on field width: at
     * Dec +60 an eighteen-degree page already has meridians that
     * curve away and never meet the bottom, and they are rescued -
     * see {@link #aNarrowPageAtHighDeclinationAlsoHasMissedEdges()}.
     *
     * <p>So this holds the case the convention was designed for and
     * says so, rather than claiming every narrow page is untouched.
     */
    @Test
    void equatorialNarrowPagesKeepTheEstablishedPreferredEdges() {
        for (double field : new double[] {8.0, 12.0, 18.0}) {
            ChartViewport viewport = page(83.818667, -5.389667, field);
            var grid = EquatorialGrid.gridFor(pageOf(viewport), null);
            for (var label : grid.labels()) {
                assertTrue(onPreferredEdge(label, viewport),
                        "at " + field + "° every figure keeps the"
                                + " established edge: " + label);
            }
        }
    }

    /**
     * The failure is declination-driven, not width-driven.
     *
     * <p>Discovered by measuring committed pages rather than by
     * reasoning: `dec60-18` is an eighteen-degree page, and two of
     * its meridians curve away from the bottom edge entirely. The
     * sprint's discovery matrix was centred on the equator and
     * missed this, which is why the inventory was re-run against the
     * committed study pages before anything was promoted.
     *
     * <p>Recorded as a contract so the next reader does not repeat
     * "wide pages only".
     */
    @Test
    void aNarrowPageAtHighDeclinationAlsoHasMissedEdges() {
        ChartViewport viewport = page(37.946619, 60.0, 18.0);
        var grid = EquatorialGrid.gridFor(pageOf(viewport), null);
        assertTrue(grid.labels().stream()
                        .anyMatch(label -> !onPreferredEdge(label, viewport)),
                "at Dec +60 an 18-degree page already needs the"
                        + " fallback: " + grid.labels());
    }

    /**
     * A suppressed preferred figure is NOT rescued elsewhere.
     *
     * <p>The scope boundary, and the one most easily lost. A curve
     * that reaches its preferred edge and has its figure refused -
     * by the title block or by paper containment - stays unlabelled.
     * That refusal is a decision about furniture, and #360 does not
     * reopen it; rescuing it would move committed narrow pages that
     * nobody reviewed.
     *
     * <p>Structural rather than remembered: the fallback lives in
     * the {@code else} of "a preferred crossing exists". Moving it
     * out of that branch fails here.
     */
    @Test
    void aSuppressedPreferredFigureIsNotRescuedElsewhere() {
        ChartViewport viewport = page(83.818667, -5.389667, 8.0);
        var metrics = EquatorialGrid.labelMetrics();
        // Reserve the whole bottom edge: every RA figure that WOULD
        // have been placed there is now suppressed, and none of them
        // may reappear on another edge.
        var wholeBottom = new java.awt.Rectangle(
                0, (int) viewport.heightPx() - 40,
                (int) viewport.widthPx(), 40);
        var grid = EquatorialGrid.gridFor(pageOf(viewport), wholeBottom);

        for (var label : grid.labels()) {
            assertTrue(!label.text().contains("h"),
                    "an RA figure whose bottom placement was"
                            + " suppressed does not reappear on"
                            + " another edge: " + label);
        }
        assertTrue(grid.suppressedLabels() > 0,
                "the premise: reserving the bottom really did"
                        + " suppress RA figures");
    }

    /**
     * Both crossings of a re-entering curve are found, and both
     * refusals are named.
     *
     * <p>Orion's minus-45 parallel at 120°: it reaches neither
     * vertical edge, dips below the frame and returns, so it crosses
     * the bottom twice. A search that stopped at the first crossing
     * would describe half of it.
     *
     * <p>It is <strong>not</strong> rescued, and that is the point.
     * One crossing collides with an existing figure and the other
     * with the title block, so it stays honestly anonymous rather
     * than being given room by moving something else.
     */
    @Test
    void aReenteringCurveOffersBothCrossingsAndMayStillBeRefused() {
        ChartViewport viewport = page(83.818667, -5.389667, 120.0);
        var metrics = EquatorialGrid.labelMetrics();
        var page = pageOf(viewport);
        var grid = EquatorialGrid.gridFor(page, null);

        var minus45 = grid.labels().stream()
                .filter(label -> label.text().contains("45")
                        && label.text().contains("\u2212"))
                .toList();
        assertEquals(java.util.List.of(), minus45,
                "the minus-45 parallel stays anonymous: both of its"
                        + " crossings are refused, and nothing is"
                        + " moved to make room");
    }

    /** One figure per curve, and never two figures in one place. */
    @Test
    void everyCurveIsNamedAtMostOnceAndFiguresDoNotOverlap() {
        var metrics = EquatorialGrid.labelMetrics();
        for (double field : new double[] {42.0, 60.0, 90.0, 120.0}) {
            ChartViewport viewport = page(0.3, 20.0, field);
            var grid = EquatorialGrid.gridFor(pageOf(viewport), null);

            var seen = new java.util.ArrayList<String>();
            for (var label : grid.labels()) {
                assertTrue(!seen.contains(label.text()),
                        "at " + field + "° no curve is named twice: "
                                + label.text());
                seen.add(label.text());
            }
            for (int i = 0; i < grid.labels().size(); i++) {
                for (int j = i + 1; j < grid.labels().size(); j++) {
                    var one = EquatorialGrid.labelBounds(
                            grid.labels().get(i), metrics);
                    var other = EquatorialGrid.labelBounds(
                            grid.labels().get(j), metrics);
                    assertTrue(!one.intersects(other),
                            "at " + field + "° two figures overlap: "
                                    + grid.labels().get(i) + " and "
                                    + grid.labels().get(j));
                }
            }
        }
    }

    /** A crossing too shallow or too near a corner is refused. */
    @Test
    void shallowAndCornerCrossingsAreRefused() {
        var metrics = EquatorialGrid.labelMetrics();
        for (double field : new double[] {42.0, 60.0, 90.0, 120.0}) {
            ChartViewport viewport = page(0.3, 20.0, field);
            var page = pageOf(viewport);
            var grid = EquatorialGrid.gridFor(page, null);
            for (var label : grid.labels()) {
                if (onPreferredEdge(label, viewport)) {
                    continue;
                }
                // Every fallback figure came from a candidate that
                // passed both gates; none sits in a corner.
                double x = label.x();
                double y = label.y();
                boolean nearLeft = x < EquatorialGrid.CORNER_CLEARANCE_PX
                        && y < EquatorialGrid.CORNER_CLEARANCE_PX;
                boolean nearRight =
                        x > viewport.widthPx()
                                        - EquatorialGrid.CORNER_CLEARANCE_PX
                                && y < EquatorialGrid.CORNER_CLEARANCE_PX;
                assertTrue(!nearLeft && !nearRight,
                        "no fallback figure straddles a corner: "
                                + label);
            }
        }
    }

    /** The convention: RA along the bottom, Dec down the left. */
    /**
     * Every centre this file uses, both poles, and all four widths.
     *
     * <p>The corpus is the point. The all-pairs check already existed
     * and was correct, but it ran at one centre - {@code (0.3, 20)} -
     * and never at a pole, so it could not see a rescued figure land
     * on a parallel's own notation. The defect it missed was real:
     * {@code 6h} drawn through {@code -30} on the released south-pole
     * page (issue #360).
     *
     * <p>What is asserted here is the hierarchy, not mere tidiness: a
     * rescued figure is a repair of last resort and yields to every
     * ordinary preferred-edge figure and to every rescue accepted
     * before it. Ordinary figures are not asserted against each other
     * here - see {@link #onlyOneOverlapPredatesThisSprint}.
     */
    @Test
    void aRescuedFigureYieldsToEveryOtherFigure() {
        var metrics = EquatorialGrid.labelMetrics();
        for (double[] centre : CENTRES) {
            for (double field : FIELDS) {
                ChartViewport viewport =
                        page(centre[0], centre[1], field);
                var labels = EquatorialGrid.gridFor(
                        pageOf(viewport), null).labels();
                for (int i = 0; i < labels.size(); i++) {
                    for (int j = i + 1; j < labels.size(); j++) {
                        var one = labels.get(i);
                        var two = labels.get(j);
                        if (onPreferredEdge(one, viewport)
                                && onPreferredEdge(two, viewport)) {
                            continue;
                        }
                        assertTrue(!EquatorialGrid.labelBounds(one, metrics)
                                        .intersects(EquatorialGrid
                                                .labelBounds(two, metrics)),
                                "at centre " + centre[0] + "," + centre[1]
                                        + " field " + field
                                        + " a rescued figure overlaps: "
                                        + one + " and " + two);
                    }
                }
            }
        }
    }

    /**
     * The named fixture for the defect two-pass ordering repairs.
     *
     * <p>Kept as its own test, at the exact page that failed, so the
     * regression has a name rather than being one iteration of a
     * sweep. On this page no figure may overlap any other.
     *
     * <p>Placing rescues inside the RA loop - before any parallel has
     * an ordinary figure - makes it fail: {@code 18h} is rescued to a
     * spot that looks clear because no parallel has spoken yet, and
     * the parallel's {@code -30} is then added on top of it, an
     * ordinary figure never being overlap-checked itself.
     *
     * <p>With both passes the hour is not named on this page at all.
     * That is the hierarchy, not a loss: the rescue can now see the
     * parallel's notation, finds nowhere that clears it, and yields.
     * A curve unnamed is the honest outcome of a repair with no room;
     * a curve named illegibly on top of another is not.
     */
    @Test
    void theSouthPoleWidePageDrawsNoFigureThroughAnother() {
        var metrics = EquatorialGrid.labelMetrics();
        ChartViewport viewport = page(180.0, -89.9, 120.0);
        var labels = EquatorialGrid.gridFor(pageOf(viewport), null).labels();
        assertTrue(labels.size() >= 2,
                "the page carries figures to hold apart");
        for (int i = 0; i < labels.size(); i++) {
            for (int j = i + 1; j < labels.size(); j++) {
                assertTrue(!EquatorialGrid
                                .labelBounds(labels.get(i), metrics)
                                .intersects(EquatorialGrid
                                        .labelBounds(labels.get(j), metrics)),
                        "south-pole 120 degrees drew "
                                + labels.get(i).text() + " through "
                                + labels.get(j).text());
            }
        }
    }

    /**
     * One overlap survives, and it is not this sprint's.
     *
     * <p>Two ordinary preferred-edge figures meet near the bottom-left
     * corner of one page. Measured against the code without #360, it
     * is there too, so the repair neither caused it nor is chartered
     * to fix it. Pinning the set is what stops a new ordinary-figure
     * collision hiding behind a known one: another arrival fails here
     * as a visible change rather than passing unnoticed.
     */
    @Test
    void onlyOneOverlapPredatesThisSprint() {
        var metrics = EquatorialGrid.labelMetrics();
        var found = new java.util.TreeSet<String>();
        for (double[] centre : CENTRES) {
            for (double field : FIELDS) {
                ChartViewport viewport =
                        page(centre[0], centre[1], field);
                var labels = EquatorialGrid.gridFor(
                        pageOf(viewport), null).labels();
                for (int i = 0; i < labels.size(); i++) {
                    for (int j = i + 1; j < labels.size(); j++) {
                        var one = labels.get(i);
                        var two = labels.get(j);
                        if (!onPreferredEdge(one, viewport)
                                || !onPreferredEdge(two, viewport)) {
                            continue;
                        }
                        if (EquatorialGrid.labelBounds(one, metrics)
                                .intersects(EquatorialGrid
                                        .labelBounds(two, metrics))) {
                            found.add(one.text() + "/" + two.text()
                                    + " at " + centre[0] + ","
                                    + centre[1] + " " + field);
                        }
                    }
                }
            }
        }
        assertEquals(java.util.Set.of(
                        "7h/\u221220\u00b0 at 83.818667,-5.389667 42.0"),
                found,
                "the pre-existing ordinary-figure overlaps are pinned;"
                        + " a new one is a visible change, not a silence");
    }

    /** Every centre this file exercises, plus both poles. */
    private static final double[][] CENTRES = {
            {0.3, 20.0}, {0.3, 45.0},
            {10.684708, 41.268750}, {37.946619, 60.0},
            {37.946619, 89.9}, {83.818667, -5.389667},
            {0.3, -89.9}, {180.0, -89.9}, {180.0, 89.9}, {0.3, 89.9}};

    /** The widths the fallback policy was settled at. */
    private static final double[] FIELDS = {42.0, 60.0, 90.0, 120.0};

    private static boolean onPreferredEdge(EquatorialGrid.Label label,
                                           ChartViewport viewport) {
        if (label.text().contains("h")) {
            return Math.abs(label.y() - (viewport.heightPx() - 5.0)) < 1.0;
        }
        return Math.abs(label.x() - 3.0) < 1.0;
    }

    private static juranometria.project.DrawnPage pageOf(
            ChartViewport viewport) {
        return juranometria.project.DrawnPage.of(
                new juranometria.chart.ChartScene(viewport,
                        java.util.List.of(), java.util.List.of(),
                        "grid", 6.0, null));
    }
}
