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
        if (curve instanceof PlaneCurve.Circular circle) {
            return Math.abs(Math.hypot(at.x() - circle.centreX(),
                    at.y() - circle.centreY()) - circle.radius());
        }
        PlaneCurve.Elliptical ellipse = (PlaneCurve.Elliptical) curve;
        // How far off the unit circle the point is in the ellipse's
        // own frame, brought back out by the larger radius - which
        // cannot understate the distance on the page.
        double cos = Math.cos(ellipse.tiltRadians());
        double sin = Math.sin(ellipse.tiltRadians());
        double dx = at.x() - ellipse.centreX();
        double dy = at.y() - ellipse.centreY();
        double u = (dx * cos + dy * sin) / ellipse.radiusAlong();
        double v = (-dx * sin + dy * cos) / ellipse.radiusAcross();
        return Math.abs(Math.hypot(u, v) - 1.0)
                * Math.max(ellipse.radiusAlong(), ellipse.radiusAcross());
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
    void theTwoShippedProjectionsUseTheFirstTwoWordsAndInventNoOther() {
        java.util.Set<String> forms = new java.util.TreeSet<>();
        for (Page page : pages()) {
            for (SkyPosition pole : POLES) {
                page.projection().greatCircle(pole).ifPresent(conic ->
                        forms.add(page.mapping()
                                .onPage(conic, page.region()).form()));
            }
        }
        assertEquals(java.util.Set.of("circular", "straight"), forms,
                "both words are used, and neither page invents another");
    }

    /**
     * A page of the third word, built from the projection's own
     * closed form rather than from a shipped projection.
     *
     * <p>A hemisphere seen orthographically: a direction an angle
     * {@code t} from the centre lands at {@code sin t}, so the sky
     * stops at one plane unit and a great circle with pole
     * {@code (a, b, c)} in the centre's frame satisfies
     * {@code (a² + b²)ξ² + 2bc·ξη + (a² + c²)η² = a²} - the pole
     * being square to every point of its own circle, with the
     * remaining component of a unit direction put back in.
     *
     * <p>The frame, the conic and the sample points are all worked
     * out here, from vectors, so that what production is asked is
     * only the two things this issue built: which word this is, and
     * where it crosses. A disc is taller than a landscape page at
     * the scale that makes it wide enough, which the gate measured
     * and which is why this one is cut at top and bottom.
     */
    private record Hemisphere(SkyPosition centre, ViewportMapping mapping) {

        static Hemisphere over(SkyPosition centre) {
            return new Hemisphere(centre, new ViewportMapping(
                    new ChartViewport(centre, 90.0, 900, 700,
                            ChartProjection.STEREOGRAPHIC)));
        }

        /** One plane unit in page units: the limb's own radius. */
        double limbRadius() {
            return mapping.pixelsPerPlaneUnit();
        }

        PageRegion region() {
            PixelPoint middle = mapping.toPixel(new PlanePoint(0.0, 0.0));
            double r = limbRadius();
            return PageRegion.within(middle.x() - 1.2 * r,
                    middle.y() - 0.95 * r, middle.x() + 1.2 * r,
                    middle.y() + 0.95 * r, middle.x(), middle.y(), r);
        }

        /** The centre's frame: towards it, east of it, north of it. */
        double[][] frame() {
            double[] along = towards(centre);
            double[] north = normalise(new double[] {
                    -along[2] * along[0], -along[2] * along[1],
                    1.0 - along[2] * along[2]});
            return new double[][] {along, cross(north, along), north};
        }

        /** Where a direction lands, at {@code sin t} from the centre. */
        PlanePoint project(SkyPosition position) {
            double[][] frame = frame();
            double[] to = towards(position);
            return new PlanePoint(dot(to, frame[1]), dot(to, frame[2]));
        }

        boolean visible(SkyPosition position) {
            return dot(towards(position), frame()[0]) > 0.0;
        }

        PlaneConic greatCircle(SkyPosition pole) {
            double[][] frame = frame();
            double[] to = towards(pole);
            double a = dot(to, frame[0]);
            double b = dot(to, frame[1]);
            double c = dot(to, frame[2]);
            return new PlaneConic(a * a + b * b, 2.0 * b * c,
                    a * a + c * c, 0.0, 0.0, -a * a);
        }
    }

    private static double[] towards(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    @Test
    void theThirdWordIsCarriedAndDrawsAHemispheresGreatCircleExactly() {
        // The gate measured this form over seventeen real pages and
        // gave all three words to this issue, so that the globe would
        // be an addition rather than a redesign of the seam. It is
        // exercised on the page it actually crosses - a hemisphere's,
        // from the projection's own closed form - because no page the
        // atlas ships reaches one yet.
        Hemisphere globe = Hemisphere.over(ORION);
        PageRegion region = globe.region();
        double worst = 0.0;
        int elliptical = 0;
        int straight = 0;
        for (SkyPosition pole : POLES) {
            PlaneCurve curve = globe.mapping()
                    .onPage(globe.greatCircle(pole), region);
            if (curve instanceof PlaneCurve.Elliptical ellipse) {
                elliptical++;
                assertTrue(ellipse.radiusAlong() != ellipse.radiusAcross(),
                        "a real ellipse, not a circle by another name");
            } else {
                // The gate's own third row: a great circle through
                // the page centre is straight under every one of the
                // three, because the centre is on it and a projection
                // about that centre cannot bend a curve through it.
                // Orion sits on the celestial equator, so the
                // equator's is that circle here.
                straight++;
                assertEquals("straight", curve.form(),
                        "a circle through the centre is a line, and"
                                + " nothing else is: " + pole);
            }

            // Where the sky says the circle is, worked out here.
            for (SkyPosition on : around(pole, 720)) {
                if (!globe.visible(on)) {
                    continue;
                }
                PixelPoint at =
                        globe.mapping().toPixel(globe.project(on));
                if (!region.contains(at.x(), at.y())) {
                    continue;
                }
                worst = Math.max(worst, missOf(curve, at));
            }
        }
        assertEquals(3, elliptical,
                "three of these circles miss the page centre, and a"
                        + " hemisphere makes an ellipse of every one");
        assertEquals(1, straight, "and the fourth runs through it");
        assertTrue(worst < 1.0e-9, "the drawn curve passes through the"
                + " positions the sky projects to, worst miss " + worst
                + " page units");
    }

    @Test
    void aHemispheresCurveIsCutByThePaperAndStopsAtTheSky() {
        Hemisphere globe = Hemisphere.over(ORION);
        PageRegion region = globe.region();
        // A disc wider than the page is tall: the equator's own
        // circle leaves the paper at the top and the bottom, so this
        // page cuts the ellipse into runs rather than keeping it
        // whole.
        List<CurveRun> runs = globe.mapping()
                .onPage(globe.greatCircle(new SkyPosition(150.0, 0.0)),
                        region)
                .clipTo(region);
        assertFalse(runs.isEmpty(), "this page carries the circle");
        for (CurveRun run : runs) {
            CurveRun.Arc arc = (CurveRun.Arc) run;
            assertTrue(arc.radiusAlong() != arc.radiusAcross(),
                    "each run keeps the ellipse's two radii");
            assertTrue(onTheBoundary(region, arc.from().orElseThrow()),
                    "and begins where the page cut it, not inside it");
            assertTrue(onTheBoundary(region, arc.to().orElseThrow()),
                    "and ends there too");
            for (int step = 1; step < 40; step++) {
                PixelPoint on = pointOn(arc,
                        arc.startRadians()
                                + arc.spanRadians() * step / 40.0);
                assertTrue(region.contains(on.x(), on.y()),
                        "and no part of a run leaves the page: " + on);
            }
        }

        // And the claim that lets an ellipse ignore the limb is
        // checked rather than trusted. One that would leave the
        // visible region says so, and names the quartic that cutting
        // it there would need.
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> new PlaneCurve.Elliptical(region.limbX(),
                        region.limbY(), 2.0 * region.limbRadius(),
                        0.5 * region.limbRadius(), 0.0).clipTo(region));
        assertTrue(refused.getMessage().contains("#301"),
                "and says whose it is: " + refused.getMessage());
    }

    /** Whether a point sits on an edge of the page, or on the limb. */
    private static boolean onTheBoundary(PageRegion region, PixelPoint at) {
        double nearest = Math.min(
                Math.min(Math.abs(at.x() - region.minX()),
                        Math.abs(region.maxX() - at.x())),
                Math.min(Math.abs(at.y() - region.minY()),
                        Math.abs(region.maxY() - at.y())));
        if (region.bounded()) {
            nearest = Math.min(nearest, Math.abs(
                    Math.hypot(at.x() - region.limbX(),
                            at.y() - region.limbY())
                            - region.limbRadius()));
        }
        return nearest < 1.0e-6;
    }

    /** A point of an arc, at an angle in the arc's own frame. */
    private static PixelPoint pointOn(CurveRun.Arc arc, double angle) {
        double cos = Math.cos(arc.tiltRadians());
        double sin = Math.sin(arc.tiltRadians());
        double along = arc.radiusAlong() * Math.cos(angle);
        double across = arc.radiusAcross() * Math.sin(angle);
        return new PixelPoint(arc.centreX() + along * cos - across * sin,
                arc.centreY() + along * sin + across * cos);
    }

    /**
     * The whole sheet of a curved page - the atlas's own
     * assembler, the production renderer and all three writers -
     * is held in {@code juranometria.sheet.ProjectedCurveOnASheetTest},
     * where a test can name the very path each writer emitted.
     */
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

    @Test
    void aPageRefusesALimbThatWouldQuietlyStopClipping() {
        // The limb is the only thing that stops ink at the sky's
        // edge, and both ways of losing it are silent. A centre that
        // is not a place makes every distance from it infinite, so
        // the comparison against the radius answers the same way
        // everywhere and no test of a curve notices. And a "within"
        // whose radius is infinite is a bounded page that is not
        // bounded - which is the fault the gate found by looking at
        // a committed page, ink running off the globe onto the
        // corners of the paper.
        assertThrows(IllegalArgumentException.class,
                () -> PageRegion.within(0, 0, 900, 700,
                        Double.POSITIVE_INFINITY, 350, 300),
                "a limb centred nowhere is not a limb");
        assertThrows(IllegalArgumentException.class,
                () -> PageRegion.within(0, 0, 900, 700, 450, 350,
                        Double.NaN),
                "nor is one with no radius");
        IllegalArgumentException endless = assertThrows(
                IllegalArgumentException.class,
                () -> PageRegion.within(0, 0, 900, 700, 450, 350,
                        Double.POSITIVE_INFINITY),
                "nor one that reaches for ever");
        assertTrue(endless.getMessage().contains("paper()"),
                "and it says how a page with no limb is written: "
                        + endless.getMessage());

        // Which is a page that says so by name, and clips to its
        // paper alone.
        PageRegion unbounded = PageRegion.paper(0, 0, 900, 700);
        assertFalse(unbounded.bounded(), "paper has no edge to the sky");
        assertTrue(unbounded.contains(899.0, 699.0),
                "and carries ink to its own corner");
    }

    @Test
    void aRunRefusesToBeMalformedWhereARendererWouldNotNotice() {
        PixelPoint here = new PixelPoint(100.0, 100.0);
        // A span of nothing is not a short run, it is a mark drawn
        // where a reference line should be - and the clipping never
        // produces one.
        assertThrows(IllegalArgumentException.class,
                () -> new CurveRun.Arc(0, 0, 10, 10, 0, 0, 0.0,
                        here, here),
                "an arc that spans nothing is not an arc");
        assertThrows(IllegalArgumentException.class,
                () -> new CurveRun.Arc(0, 0, 10, 10, 0, 0,
                        3.0 * Math.PI, here, here),
                "nor is one drawn twice round");
        assertThrows(IllegalArgumentException.class,
                () -> new CurveRun.Arc(0, 0, 0.0, 10, 0, 0, 1.0,
                        here, here),
                "nor one with no radius");
        // The ends are what a label hangs on, and the rule that hangs
        // it reads one of them: half a pair is a run no rule can
        // name, and a closed run with ends is one named at a join it
        // does not have.
        assertThrows(IllegalArgumentException.class,
                () -> new CurveRun.Arc(0, 0, 10, 10, 0, 0, 1.0,
                        here, null),
                "an arc has both ends or neither");
        assertThrows(IllegalArgumentException.class,
                () -> new CurveRun.Arc(0, 0, 10, 10, 0, 0, 1.0,
                        null, null),
                "and only a whole turn closes");
        assertThrows(IllegalArgumentException.class,
                () -> new CurveRun.Arc(0, 0, 10, 10, 0, 0,
                        CurveRun.Arc.WHOLE_TURN, here, here),
                "and a whole turn is closed, so it has nowhere to"
                        + " hang a name");
        assertThrows(IllegalArgumentException.class,
                () -> new CurveRun.Arc(0, 0, 10, 10, 0, 0, 1.0, here,
                        new PixelPoint(Double.NaN, 0.0)),
                "an arc ends at pixels a renderer can draw");
        assertThrows(IllegalArgumentException.class,
                () -> new CurveRun.Segment(here,
                        new PixelPoint(Double.POSITIVE_INFINITY, 0.0)),
                "a run ends at pixels a renderer can draw");

        // And a line whose direction is not a unit vector measures
        // distances in units of nothing, which is the whole meaning
        // of the record.
        assertThrows(IllegalArgumentException.class,
                () -> new PlaneCurve.Straight(1.0, 3.0, 4.0),
                "a line's direction is normalised");
        assertEquals(1.0,
                Math.hypot(PlaneCurve.Straight.of(1.0, 3.0, 4.0).b(),
                        PlaneCurve.Straight.of(1.0, 3.0, 4.0).c()),
                1.0e-12, "which is what of() is for");
        assertThrows(IllegalArgumentException.class,
                () -> new PlaneCurve.Circular(0.0, 0.0, 0.0),
                "and a circle has a radius");
    }
}
