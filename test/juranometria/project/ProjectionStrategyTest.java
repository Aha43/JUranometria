package juranometria.project;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two real projections behind one boundary (issue #297).
 *
 * <p>An interface demonstrated by one implementation is not
 * demonstrated, so both of the atlas's projections are held to the
 * same promises here: they place a position and take it back, they
 * say where they stop and stop there, they say what a great circle
 * becomes, and they agree with the viewport about how a page is
 * scaled.
 *
 * <p>Nothing in this file asks which projection it is holding except
 * where the answers differ <em>because</em> they are different
 * projections - the domain, and the shape a great circle takes. That
 * is the point of the seam.
 */
class ProjectionStrategyTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);

    private static List<Projection> both() {
        return List.of(new GnomonicProjection(ORION),
                new StereographicProjection(ORION));
    }

    /** A position a given angle from a centre, due north of it. */
    private static SkyPosition along(SkyPosition centre, double degrees) {
        double dec = centre.decDegrees() + degrees;
        return dec <= 90.0 ? new SkyPosition(centre.raDegrees(), dec)
                : new SkyPosition((centre.raDegrees() + 180.0) % 360.0,
                        180.0 - dec);
    }

    private static double separation(SkyPosition a, SkyPosition b) {
        Direction direction = CentreFrame.about(a).directionTo(b);
        return Math.toDegrees(Math.atan2(direction.transverse(),
                direction.along()));
    }

    @Test
    void bothTakeAPositionToThePlaneAndBackAgain() {
        for (Projection projection : both()) {
            double worst = 0.0;
            int reached = 0;
            for (double dec = -85.0; dec <= 85.0; dec += 5.0) {
                for (double ra = 0.0; ra < 360.0; ra += 15.0) {
                    SkyPosition position = new SkyPosition(ra, dec);
                    Optional<PlanePoint> plane = projection.project(position);
                    if (plane.isEmpty()) {
                        continue;
                    }
                    reached++;
                    SkyPosition back = projection.unproject(plane.get())
                            .orElseThrow(() -> new AssertionError(
                                    projection.name() + " placed "
                                            + position + " and cannot"
                                            + " take it back"));
                    worst = Math.max(worst, separation(position, back));
                }
            }
            assertTrue(reached > 300, projection.name()
                    + " reaches a useful part of the sky: " + reached);
            assertTrue(worst < 1.0e-11, projection.name()
                    + " loses " + worst + " degrees on the round trip");
        }
    }

    @Test
    void eachStopsWhereItSaysItStops() {
        // The domain is the projection's own statement about itself,
        // and project() is the authority for any one position. The
        // two differ, which is most of why there are two.
        Projection gnomonic = new GnomonicProjection(ORION);
        assertEquals(90.0, gnomonic.limitDegrees());
        assertTrue(gnomonic.project(along(ORION, 89.9)).isPresent(),
                "the tangent plane reaches nearly a right angle");
        assertTrue(gnomonic.project(along(ORION, 90.0)).isEmpty(),
                "and not the right angle itself, where it is"
                        + " infinitely far away");

        Projection stereographic = new StereographicProjection(ORION);
        assertEquals(180.0, stereographic.limitDegrees());
        assertTrue(stereographic.project(along(ORION, 90.0)).isPresent(),
                "the overview crosses a right angle without noticing");
        assertTrue(stereographic.project(along(ORION, 179.999)).isPresent(),
                "and reaches a thousandth of a degree from the"
                        + " antipode");
        assertTrue(stereographic
                        .project(new SkyPosition(263.0, 0.0)).isEmpty(),
                "stopping at the one point opposite the centre");
    }

    @Test
    void aQuarterOfATurnIsAnsweredExactlyAndSoIsHalfOfOne() {
        // Where a double's trigonometry says 6.1e-17 or 1.2e-16 and
        // the answer is zero. Sprint 30's gate spent four rounds on
        // this family; the atlas's oldest projection test found the
        // quarter turn the moment the domain rule became exact.
        Projection gnomonic =
                new GnomonicProjection(new SkyPosition(0.0, 0.0));
        assertTrue(gnomonic.project(new SkyPosition(90.0, 0.0)).isEmpty(),
                "a quarter turn east is on the horizon, not sixteen"
                        + " quadrillion units out");
        assertTrue(gnomonic.project(new SkyPosition(270.0, 0.0)).isEmpty(),
                "and so is a quarter turn west");
        assertTrue(gnomonic.project(new SkyPosition(0.0, 90.0)).isEmpty(),
                "and the pole");
        assertTrue(gnomonic.project(new SkyPosition(180.0, 0.0)).isEmpty(),
                "and the antipode, behind the plane");
    }

    @Test
    void aPoleIsTheSamePoleHoweverItsRightAscensionIsWritten() {
        // At a pole the right ascension means nothing, so every way
        // of writing it is that point.
        Projection projection =
                new StereographicProjection(new SkyPosition(0.0, 90.0));
        for (double ra : new double[] {0.0, 37.0, 90.0, 180.0, 271.5}) {
            PlanePoint itself = projection
                    .project(new SkyPosition(ra, 90.0)).orElseThrow();
            assertEquals(0.0, Math.hypot(itself.xiEast(),
                            itself.etaNorth()),
                    "the centre written at right ascension " + ra);
            assertTrue(projection.project(new SkyPosition(ra, -90.0))
                            .isEmpty(),
                    "and the opposite pole at right ascension " + ra);
        }
    }

    @Test
    void eachSaysWhatAGreatCircleBecomesAndTheyDoNotAgree() {
        // The pole of the celestial equator, seen from a centre on
        // it. Under a tangent plane every great circle is straight;
        // under the overview only those through the page centre are,
        // and this one is - so the interesting case is a circle that
        // is not.
        SkyPosition eclipticPole = new SkyPosition(270.0, 66.5607);

        PlaneConic straight = new GnomonicProjection(ORION)
                .greatCircle(eclipticPole).orElseThrow();
        assertEquals(0.0, straight.a(), "a gnomonic great circle has no");
        assertEquals(0.0, straight.b(), "quadratic part at all:");
        assertEquals(0.0, straight.c(), "it is a line");

        PlaneConic curved = new StereographicProjection(ORION)
                .greatCircle(eclipticPole).orElseThrow();
        assertFalse(curved.a() == 0.0 && curved.b() == 0.0
                        && curved.c() == 0.0,
                "the same circle under the overview is not a line");
        assertEquals(curved.a(), curved.c(), 1.0e-15,
                "it is a circle: equal squares,");
        assertEquals(0.0, curved.b(), "and no cross term");
    }

    @Test
    void aGreatCircleIsWhereTheProjectionSaysItIs() {
        // The conic against positions the projection genuinely
        // placed, which is what makes it a statement about this sky
        // rather than about an equation.
        for (Projection projection : both()) {
            SkyPosition pole = new SkyPosition(270.0, 66.5607);
            double worst = 0.0;
            int on = 0;
            PlaneConic conic = projection.greatCircle(pole).orElseThrow();
            for (SkyPosition position : around(pole)) {
                Optional<PlanePoint> plane = projection.project(position);
                if (plane.isEmpty()) {
                    continue;
                }
                double x = plane.get().xiEast();
                double y = plane.get().etaNorth();
                double[] slope = conic.gradientAt(x, y);
                double steepness = Math.hypot(slope[0], slope[1]);
                if (steepness == 0.0) {
                    continue;
                }
                on++;
                worst = Math.max(worst,
                        Math.abs(conic.at(x, y)) / steepness);
            }
            assertTrue(on > 100, projection.name()
                    + " placed a circle's worth of points: " + on);
            assertTrue(worst < 1.0e-9, projection.name()
                    + " says the ecliptic is where it puts it, to "
                    + worst + " plane units");
        }
    }

    @Test
    void aCurvedCircleIsDrawnAsACurveAndNotRefused() {
        // Issue #297 left this refusing, loudly, and named #298 as
        // what would draw it. This is #298: the seam carries the
        // curve now, so the refusal is gone and its absence is the
        // thing to hold. A page whose ecliptic curves must come back
        // with a curve on it - not with nothing, which would be the
        // page silently short of a line it promised.
        ChartViewport viewport = new ChartViewport(ORION, 42.0, 900, 700,
                ChartProjection.STEREOGRAPHIC);
        SkyPosition eclipticPole = new SkyPosition(270.0, 66.5607);
        PageRegion paper = PageRegion.paper(0, 0, 900, 700);
        ViewportMapping mapping = new ViewportMapping(viewport, juranometria.project.Projections.of(viewport.projection(), viewport.centre()));
        Projection projection = Projections.forViewport(viewport);

        PlaneCurve curve = mapping.onPage(
                projection.greatCircle(eclipticPole).orElseThrow(), paper);
        assertEquals("circular", curve.form(),
                "the overview curves this circle");

        // A circle this page really does carry, drawn straight by
        // both projections because it runs through the page centre -
        // so the form above is about the shape a circle takes and not
        // about the projection's name.
        SkyPosition equatorPole = new SkyPosition(0.0, 90.0);
        for (ChartProjection kind : ChartProjection.values()) {
            ChartViewport carrying = new ChartViewport(ORION, 42.0,
                    900, 700, kind);
            List<CurveRun> runs = GreatCirclePage.clip(
                    Projections.forViewport(carrying),
                    new ViewportMapping(carrying, juranometria.project.Projections.of(carrying.projection(), carrying.centre())), paper, equatorPole);
            assertFalse(runs.isEmpty(),
                    kind + " draws the celestial equator across a"
                            + " chart centred on it");
            assertTrue(runs.get(0) instanceof CurveRun.Segment,
                    kind + " draws it straight, because it runs through"
                            + " the page centre");
        }
    }

    @Test
    void aCircleGenuinelyOffThePageIsStillSilence() {
        // The distinction the curve above must not blur. Off the page
        // is silence: the chart draws nothing rather than promising a
        // line the sky has not made.
        ChartViewport viewport = new ChartViewport(ORION, 1.0, 900, 700);
        assertTrue(GreatCirclePage.clip(Projections.forViewport(viewport),
                        new ViewportMapping(viewport, juranometria.project.Projections.of(viewport.projection(), viewport.centre())),
                        PageRegion.paper(0, 0, 900, 700),
                        new SkyPosition(83.0, 89.0))
                        .isEmpty(),
                "a circle that misses a one-degree page is simply not"
                        + " drawn");
    }

    @Test
    void theViewportIsScaledByItsOwnProjectionAndNotByATangent() {
        // Half the page holds half the field, whichever projection
        // is drawing. Written as a tangent this was one projection's
        // answer given for all of them - and the reason a viewport
        // used to refuse a field of 180 degrees, which was never
        // about pages.
        for (ChartProjection kind : ChartProjection.values()) {
            ChartViewport viewport = new ChartViewport(ORION, 60.0,
                    900, 700, kind);
            Projection projection = Projections.forViewport(viewport);
            ViewportMapping mapping = new ViewportMapping(viewport, juranometria.project.Projections.of(viewport.projection(), viewport.centre()));
            // Half the field east of the centre, which is half the
            // page across - and east is left, so it lands on the
            // left edge whichever projection scaled it.
            PlanePoint halfway = projection
                    .project(new SkyPosition(ORION.raDegrees() + 30.0, 0.0))
                    .orElseThrow();
            PixelPoint edge = mapping.toPixel(halfway);
            assertEquals(0.0, edge.x(), 1.0e-9,
                    kind + ": half the field lands half the page across");
        }
    }

    @Test
    void theOverviewReachesFieldsTheTangentPlaneCannotHold() {
        // The whole reason for a second projection. A 120-degree
        // page has a corner two thirds of a right angle out, and a
        // tangent plane cannot scale one at all past 180.
        ChartViewport wide = new ChartViewport(ORION, 170.0, 900, 700,
                ChartProjection.STEREOGRAPHIC);
        ViewportMapping mapping = new ViewportMapping(wide, juranometria.project.Projections.of(wide.projection(), wide.centre()));
        assertTrue(mapping.pixelsPerPlaneUnit() > 0.0,
                "the overview scales a 170-degree page");

        // Two, to the precision the arithmetic has: tan of a
        // quarter turn comes back from a double as
        // 0.9999999999999999, so a hemisphere is 1.9999999999999998
        // plane units. Nothing divides by it, so nothing here needs
        // the exact answer - unlike the degeneracies above, where
        // something did.
        assertEquals(2.0, new StereographicProjection(ORION)
                        .planeRadius(90.0), 1.0e-15,
                "a whole hemisphere inside a plane radius of two");
        assertTrue(Double.isInfinite(new GnomonicProjection(ORION)
                        .visiblePlaneRadius()),
                "and a chart plane has no edge to the sky");
    }

    /** Positions evenly around the great circle with this pole. */
    private static List<SkyPosition> around(SkyPosition pole) {
        double[] axis = {
                Math.cos(Math.toRadians(pole.decDegrees()))
                        * Math.cos(Math.toRadians(pole.raDegrees())),
                Math.cos(Math.toRadians(pole.decDegrees()))
                        * Math.sin(Math.toRadians(pole.raDegrees())),
                Math.sin(Math.toRadians(pole.decDegrees()))};
        double[] any = Math.abs(axis[2]) < 0.9
                ? new double[] {0, 0, 1} : new double[] {1, 0, 0};
        double[] u = normalise(cross(any, axis));
        double[] v = cross(axis, u);
        List<SkyPosition> positions = new java.util.ArrayList<>();
        for (int i = 0; i < 360; i++) {
            double t = 2.0 * Math.PI * i / 360.0;
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
}
