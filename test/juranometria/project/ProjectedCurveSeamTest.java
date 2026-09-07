package juranometria.project;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The curve seam, held to the sky it came from (issue #298).
 *
 * <p>A great circle is contributed by meaning - a pole and what kind
 * of line it is - and nothing about the projection that will draw it.
 * What the seam owes is that the ink lands where the sky says, in
 * either projection, and that a curve is drawn as a curve.
 *
 * <p>Nothing here names a meridian, a horizon or an ecliptic. They
 * are poles, and the atlas's modules contribute them the same way in
 * both projections; what differs is only what the page makes of
 * them.
 */
class ProjectedCurveSeamTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);
    private static final SkyPosition POLE = new SkyPosition(0.0, 90.0);
    private static final SkyPosition EQUINOX = new SkyPosition(0.0, 0.0);

    /** Poles of the circles the atlas's modules contribute. */
    private static final List<SkyPosition> POLES = List.of(
            new SkyPosition(0.0, 90.0),        // the celestial equator's
            new SkyPosition(270.0, 66.5607),   // a permanent circle's
            new SkyPosition(150.0, 0.0),       // a line across the sky
            new SkyPosition(60.0, 25.0));      // a boundary of what is seen

    private record Page(ChartProjection kind, SkyPosition centre,
                        double field) {

        ChartViewport viewport() {
            return new ChartViewport(centre, field, 900, 700, kind);
        }

        Projection projection() {
            return Projections.forViewport(viewport());
        }

        ViewportMapping mapping() {
            return new ViewportMapping(viewport());
        }

        PageRegion region() {
            return mapping().regionFor(viewport(), projection());
        }

        @Override
        public String toString() {
            return kind + " over " + centre + " at " + field + " degrees";
        }
    }

    /**
     * The fields this seam has to serve, which are wider than the
     * ones a reader can ask for.
     *
     * <p>Measured: at every field on the ladder today the widest an
     * overview arc stands off its own chord is <strong>0.08
     * pixels</strong>, and at 60, 90 and 120 degrees it is 14, 37 and
     * 71. That is why the released atlas came through this issue byte
     * for byte identical, and why a test of curves that only used the
     * offered fields would be testing nothing: a chord would pass.
     *
     * <p>The wider pages are constructed here rather than asked of a
     * view state, because they are not on the ladder and this issue
     * does not put them there. Issue #299 does.
     */
    private static List<Page> pages() {
        List<Page> pages = new ArrayList<>();
        for (ChartProjection kind : ChartProjection.values()) {
            for (SkyPosition centre : List.of(ORION, POLE, EQUINOX)) {
                for (double field : new double[] {12.0, 24.0, 42.0,
                        60.0, 90.0, 120.0}) {
                    if (field / 2.0 >= Projections.of(kind, centre)
                            .limitDegrees()) {
                        continue;
                    }
                    pages.add(new Page(kind, centre, field));
                }
            }
        }
        return pages;
    }

    /** Positions evenly around the great circle with this pole. */
    private static List<SkyPosition> around(SkyPosition pole, int many) {
        double dec = Math.toRadians(pole.decDegrees());
        double ra = Math.toRadians(pole.raDegrees());
        double[] axis = {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
        double[] any = Math.abs(axis[2]) < 0.9
                ? new double[] {0, 0, 1} : new double[] {1, 0, 0};
        double[] u = normalise(cross(any, axis));
        double[] v = cross(axis, u);
        List<SkyPosition> positions = new ArrayList<>(many);
        for (int i = 0; i < many; i++) {
            double t = 2.0 * Math.PI * i / many;
            double x = Math.cos(t) * u[0] + Math.sin(t) * v[0];
            double y = Math.cos(t) * u[1] + Math.sin(t) * v[1];
            double z = Math.cos(t) * u[2] + Math.sin(t) * v[2];
            positions.add(new SkyPosition(
                    (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0,
                    Math.toDegrees(Math.asin(Math.clamp(z, -1.0, 1.0)))));
        }
        return positions;
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalise(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / length, v[1] / length, v[2] / length};
    }

    /** How far a page point lies off a curve, in page units. */
    private static double missOf(PlaneCurve curve, PixelPoint at) {
        if (curve instanceof PlaneCurve.Straight line) {
            return line.distanceFrom(at.x(), at.y());
        }
        PlaneCurve.Circular circle = (PlaneCurve.Circular) curve;
        return Math.abs(Math.hypot(at.x() - circle.centreX(),
                at.y() - circle.centreY()) - circle.radius());
    }

    @Test
    void aDrawnCurveLandsWhereTheSkySaysOnEitherProjection() {
        // The contributed pole is the same in both projections. What
        // differs is what the page makes of it, and the test of that
        // is not what shape came back but whether the shape passes
        // through positions the projection genuinely placed.
        double worst = 0.0;
        int checked = 0;
        for (Page page : pages()) {
            for (SkyPosition pole : POLES) {
                var conic = page.projection().greatCircle(pole);
                if (conic.isEmpty()) {
                    continue;
                }
                PlaneCurve curve =
                        page.mapping().onPage(conic.get(), page.region());
                for (SkyPosition on : around(pole, 720)) {
                    var plane = page.projection().project(on);
                    if (plane.isEmpty()) {
                        continue;
                    }
                    PixelPoint at = page.mapping().toPixel(plane.get());
                    if (!page.region().contains(at.x(), at.y())) {
                        continue;
                    }
                    checked++;
                    worst = Math.max(worst, missOf(curve, at));
                }
            }
        }
        assertTrue(checked > 5000,
                "a real quantity of sky was checked: " + checked);
        assertTrue(worst < 1.0e-6, "the drawn curve is where the sky is,"
                + " worst " + worst + " page units");
    }

    @Test
    void noStraightChordStandsInForAVisibleArc() {
        // The failure this issue names, and the one that would pass
        // unnoticed: a chord between a run's two ends looks like the
        // curve at a glance and is not it. So the test is not that
        // the form says "circular" - it is that what is drawn departs
        // from that chord by more than a chord could, and by more
        // than the page's own ink is wide.
        int curved = 0;
        for (Page page : pages()) {
            if (page.kind() != ChartProjection.STEREOGRAPHIC) {
                continue;
            }
            for (SkyPosition pole : POLES) {
                var conic = page.projection().greatCircle(pole);
                if (conic.isEmpty()) {
                    continue;
                }
                PlaneCurve curve =
                        page.mapping().onPage(conic.get(), page.region());
                if (!(curve instanceof PlaneCurve.Circular circle)) {
                    continue;
                }
                for (CurveRun run : curve.clipTo(page.region())) {
                    if (run.closed()) {
                        continue;
                    }
                    CurveRun.Arc arc = (CurveRun.Arc) run;
                    PixelPoint from = arc.from().orElseThrow();
                    PixelPoint to = arc.to().orElseThrow();
                    // The sagitta: how far the arc's middle stands
                    // off the chord between its ends.
                    PixelPoint middle = circle.at(
                            arc.startRadians() + arc.spanRadians() / 2.0);
                    double sagitta = java.awt.geom.Line2D.ptLineDist(
                            from.x(), from.y(), to.x(), to.y(),
                            middle.x(), middle.y());
                    if (sagitta <= 5.0) {
                        continue;  // too flat here to tell either way
                    }
                    curved++;
                    // Five page units is far above the 0.08 an arc
                    // reaches at any offered field and far below the
                    // 14 it reaches at the narrowest overview rung,
                    // so nothing sits near it.
                    assertTrue(sagitta > 5.0, "an arc a reader could see"
                            + " the curve of: " + sagitta + " page units"
                            + " off its own chord, on " + page);
                }
            }
        }
        assertTrue(curved > 0, "at least one page carries an arc whose"
                + " curvature a chord would visibly lose");
    }

    @Test
    void aCurveCanCrossOnePageMoreThanOnce() {
        // What the old single-arc answer could not say. A circle and
        // a rectangle meet in up to eight points, so a great circle
        // can leave the paper and come back, and an answer that could
        // only say "once" would draw one piece and silently drop the
        // rest.
        int several = 0;
        for (Page page : pages()) {
            for (SkyPosition pole : POLES) {
                List<CurveRun> runs = GreatCirclePage.clip(
                        page.projection(), page.mapping(), page.region(),
                        pole);
                assertTrue(runs.size() <= 4, page + ": a circle and a"
                        + " rectangle make at most four runs, not "
                        + runs.size());
                if (runs.size() > 1) {
                    several++;
                }
            }
        }
        assertTrue(several > 0, "real pages do cut a circle into more"
                + " than one run");
    }

    @Test
    void aCurveWhollyOnThePageHasNoEndToNameItAt() {
        // The other thing two endpoints could not say. A great circle
        // inside the paper closes, and the rule that names a line
        // where it leaves the page has nothing to hold - so the run
        // says it has no ends rather than inventing a pair.
        PlaneCurve.Circular inside =
                new PlaneCurve.Circular(450.0, 350.0, 120.0);
        List<CurveRun> runs =
                inside.clipTo(PageRegion.paper(0, 0, 900, 700));
        assertEquals(1, runs.size(), "one run, closed on itself");
        assertTrue(runs.get(0).closed(), "with no ends at all");
        assertTrue(runs.get(0).from().isEmpty(), "neither of them");
        assertTrue(runs.get(0).to().isEmpty(), "nor the other");
    }

    @Test
    void inkStopsWhereTheSkyStopsAndNotOnlyWhereThePaperDoes() {
        // A projection showing a hemisphere has an edge, and a curve
        // drawn from its own equation would run past it. Held on the
        // vocabulary directly, because the atlas's two projections
        // have no limb - the seam must be able to say it before the
        // projection that needs it arrives.
        PageRegion globe = PageRegion.within(0, 0, 900, 700,
                450.0, 350.0, 200.0);
        // A line across the middle: it leaves the paper at x = 0 and
        // 900, and leaves the sky at 250 and 650.
        List<CurveRun> runs = PlaneCurve.Straight.of(-350.0, 0.0, 1.0)
                .clipTo(globe);
        assertEquals(1, runs.size(), "one run");
        CurveRun.Segment cut = (CurveRun.Segment) runs.get(0);
        assertEquals(250.0, Math.min(cut.start().x(), cut.end().x()),
                1.0e-9, "cut at the limb, not at the paper's edge");
        assertEquals(650.0, Math.max(cut.start().x(), cut.end().x()),
                1.0e-9, "at both ends");

        assertTrue(PlaneCurve.Straight.of(-600.0, 0.0, 1.0)
                        .clipTo(globe).isEmpty(),
                "and a line that misses the globe is not drawn at all");
    }

    @Test
    void theVocabularyCarriesTwoWordsAndRefusesToInventAThird() {
        // The smallest vocabulary the atlas's two projections
        // demonstrate. The gate measured a third - an ellipse, which
        // is what a projection showing a hemisphere makes of a great
        // circle - and it is deliberately not here: no page the atlas
        // can draw crosses one, and a word nothing can exercise is a
        // word no test can defend.
        java.util.Set<String> forms = new java.util.TreeSet<>();
        for (Page page : pages()) {
            for (SkyPosition pole : POLES) {
                page.projection().greatCircle(pole).ifPresent(conic ->
                        forms.add(page.mapping()
                                .onPage(conic, page.region()).form()));
            }
        }
        assertEquals(java.util.Set.of("circular", "straight"), forms,
                "both words are used, and no third is invented");

        // And meeting one says so rather than drawing the wrong
        // curve. An ellipse of unequal radii is what #301 brings.
        ViewportMapping mapping =
                new ViewportMapping(new ChartViewport(ORION, 42.0,
                        900, 700));
        PlaneConic ellipse = new PlaneConic(4.0, 0.0, 1.0, 0.0, 0.0, -1.0);
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> mapping.onPage(ellipse,
                        PageRegion.paper(0, 0, 900, 700)));
        assertTrue(refused.getMessage().contains("#301"),
                "and names what brings it: " + refused.getMessage());
    }

    @Test
    void aCurveReachesASheetAsACurveAndNotAsAChord() {
        // The acceptance's last geometry clause: screen, SVG, PDF and
        // PNG carry the same curve. They do because a sheet replays
        // the production render - the recorder keeps the Shape the
        // chart drew and the writers walk its own path - so what has
        // to be shown is that the shape being kept is curved, and
        // that the writers put a curve in the file rather than a line
        // between the same two ends.
        ChartViewport viewport = new ChartViewport(ORION, 90.0, 900, 700,
                ChartProjection.STEREOGRAPHIC);
        ViewportMapping mapping = new ViewportMapping(viewport);
        PageRegion region = mapping.regionFor(viewport,
                Projections.forViewport(viewport));
        List<CurveRun> runs = GreatCirclePage.clip(
                Projections.forViewport(viewport), mapping, region,
                new SkyPosition(270.0, 66.5607));
        assertFalse(runs.isEmpty(), "this page carries the circle");

        java.awt.Shape drawn =
                juranometria.ui.ReferenceInk.shapeOf(runs.get(0));
        int curves = 0;
        int lines = 0;
        double[] point = new double[6];
        for (var each = drawn.getPathIterator(null); !each.isDone();
                each.next()) {
            switch (each.currentSegment(point)) {
                case java.awt.geom.PathIterator.SEG_CUBICTO,
                     java.awt.geom.PathIterator.SEG_QUADTO -> curves++;
                case java.awt.geom.PathIterator.SEG_LINETO -> lines++;
                default -> { }
            }
        }
        assertTrue(curves > 0, "the shape the chart draws is curved:"
                + " " + curves + " curved segments against " + lines
                + " straight ones");

        // And what a sheet keeps of it. Every writer walks the path
        // of the shape the recorder kept, so the question that
        // decides whether SVG, PDF and PNG carry the curve is whether
        // the recorder keeps a curve - a recorder that flattened to
        // segments would hand all three of them a chord.
        juranometria.sheet.SheetRecorder recorder =
                new juranometria.sheet.SheetRecorder(900, 700);
        java.awt.Graphics2D g = (java.awt.Graphics2D) recorder.create();
        try {
            g.draw(drawn);
        } finally {
            g.dispose();
        }
        int kept = 0;
        for (var each = recorder.drawn().get(0).shape()
                .getPathIterator(null); !each.isDone(); each.next()) {
            int segment = each.currentSegment(point);
            if (segment == java.awt.geom.PathIterator.SEG_CUBICTO
                    || segment == java.awt.geom.PathIterator.SEG_QUADTO) {
                kept++;
            }
        }
        assertEquals(curves, kept, "the sheet keeps every curved segment"
                + " the chart drew, which is what puts the same curve in"
                + " SVG, PDF and PNG");

        // A whole sheet of a curved page is not reachable yet: a view
        // state will not take a field the ladder does not carry, and
        // this issue does not widen the ladder. Issue #299 does, and
        // owes the end-to-end sheet.
    }

    @Test
    void aModuleSaysWhereAndNeverWhatItLooksLike() {
        // The boundary this issue must preserve. The same pole, the
        // same contribution, two projections - and the module is
        // told nothing about either. What changes is what the page
        // makes of it, which is the whole of the rule Sprint 30
        // settled.
        SkyPosition pole = new SkyPosition(270.0, 66.5607);
        Page tangent = new Page(ChartProjection.GNOMONIC, ORION, 42.0);
        Page overview = new Page(ChartProjection.STEREOGRAPHIC, ORION, 42.0);

        String straight = tangent.mapping().onPage(
                tangent.projection().greatCircle(pole).orElseThrow(),
                tangent.region()).form();
        String curved = overview.mapping().onPage(
                overview.projection().greatCircle(pole).orElseThrow(),
                overview.region()).form();
        assertEquals("straight", straight, "a tangent plane draws it flat");
        assertEquals("circular", curved, "and the overview curves it");
        assertFalse(straight.equals(curved),
                "from one contribution, which named neither");
    }
}
