package juranometria.project;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exact hemisphere (Sprint 32, issue #329).
 *
 * <p>Each test here is also a mutation control: it names the wrong
 * implementation it would catch, because a projection test that only
 * confirms the arithmetic it was written from confirms nothing. The
 * wrong implementations are the ones the issue listed - a tangent
 * inverse, an open limb, a far side accepted, a circle where an
 * ellipse belongs, and pole or seam arithmetic that loses digits.
 */
class OrthographicProjectionTest {

    private static final SkyPosition CENTRE =
            new SkyPosition(266.0, -28.0);

    private static final Projection GLOBE =
            new OrthographicProjection(CENTRE);

    /** Plane units are exact here, so the tolerance is arithmetic. */
    private static final double EXACT = 1.0e-12;

    @Test
    void theCentreIsTheOrigin() {
        assertEquals(new PlanePoint(0.0, 0.0),
                GLOBE.project(CENTRE).orElseThrow(),
                "the centre of the page is the centre of the disc");
        assertEquals(0.0, GLOBE.planeRadius(0.0), EXACT);
    }

    @Test
    void howFarOutIsTheSineAndNothingElse() {
        // Mutation control: a tangent or a half-angle here is the
        // other two projections' rule, and both are larger than this
        // everywhere except zero.
        for (double degrees : new double[] {0.0, 1.0, 30.0, 45.0, 60.0,
                89.0, 89.999, 90.0}) {
            assertEquals(Math.sin(Math.toRadians(degrees)),
                    GLOBE.planeRadius(degrees), EXACT,
                    degrees + " degrees out lands at sin of it");
        }
        assertTrue(Double.isNaN(GLOBE.planeRadius(90.001)),
                "past the limb there is no answer, and sin continued"
                        + " would give one that reads as an answer:"
                        + " 120 degrees would land where 60 does");
        assertTrue(Double.isNaN(GLOBE.planeRadius(120.0)));
    }

    @Test
    void theLimbIsAttainedAndBelongsToThePage() {
        // Mutation control: an open limb - a projection that refuses
        // exactly 90 degrees, or answers a radius just under one -
        // passes every other test here and leaves a page whose edge
        // is a hair of unreachable sky.
        assertEquals(1.0, GLOBE.planeRadius(90.0), 0.0,
                "ninety degrees is exactly the limb");
        assertEquals(90.0, GLOBE.limitDegrees(),
                "and the limit is ninety");
        assertEquals(1.0, GLOBE.visiblePlaneRadius(), 0.0,
                "which is where the sky ends on the plane");
        assertEquals(90.0, GLOBE.angleAtPlaneRadius(1.0), EXACT,
                "asin(1) is exactly a right angle");

        SkyPosition onTheLimb = ninetyDegreesFrom(CENTRE);
        assertEquals(90.0, CENTRE.separationDegrees(onTheLimb), 1.0e-9,
                "the fixture really is on the limb");
        PlanePoint landed = GLOBE.project(onTheLimb).orElseThrow(
                () -> new AssertionError("the limb is on the page"));
        assertEquals(1.0, Math.hypot(landed.xiEast(), landed.etaNorth()),
                1.0e-9, "and lands at plane radius one");
    }

    @Test
    void theFarSideHasNoAnswerAndTheNearSideAlwaysDoes() {
        // Mutation control: accepting the far hemisphere. Every
        // position there projects onto the near side's disc, so a
        // globe that took them would draw the whole sky twice and
        // look almost right.
        SkyPosition antipode = new SkyPosition(
                (CENTRE.raDegrees() + 180.0) % 360.0,
                -CENTRE.decDegrees());
        assertTrue(GLOBE.project(antipode).isEmpty(),
                "the point opposite the centre is not on this page");
        assertTrue(GLOBE.unproject(new PlanePoint(1.0001, 0.0)).isEmpty(),
                "and neither is a plane point outside the limb");
        assertTrue(GLOBE.unproject(new PlanePoint(0.8, 0.8)).isEmpty(),
                "including one inside the paper's corner but outside"
                        + " the disc");

        for (double along = 0.0; along <= 90.0; along += 7.5) {
            SkyPosition near = towards(CENTRE, along, 33.0);
            assertTrue(GLOBE.project(near).isPresent(),
                    along + " degrees out is on the visible"
                            + " hemisphere and must project");
        }
    }

    @Test
    void projectingAndBackIsTheSamePositionEverywhere() {
        // Including both poles and the seam, which is where an
        // inverse built on a cosine loses its digits: the study that
        // did it lost a disc of sky three milliarcseconds across.
        SkyPosition[] cases = {
                CENTRE,
                new SkyPosition(0.0, 90.0),
                new SkyPosition(0.0, -90.0),
                new SkyPosition(0.0, 0.0),
                new SkyPosition(359.9999, -0.0001),
                new SkyPosition(180.0, 62.0),
                ninetyDegreesFrom(CENTRE)};
        for (SkyPosition where : cases) {
            Projection about = new OrthographicProjection(
                    new SkyPosition(0.0, 0.0));
            Optional<PlanePoint> plane = about.project(where);
            if (plane.isEmpty()) {
                continue;
            }
            SkyPosition back = about.unproject(plane.get()).orElseThrow();
            assertEquals(0.0, where.separationDegrees(back), 1.0e-9,
                    where + " survives the round trip");
        }
    }

    @Test
    void aGreatCircleThroughTheCentreIsStraightAndTheRestAreEllipses() {
        ChartViewportFixture page = new ChartViewportFixture();

        // A pole square to the centre: the circle passes through the
        // page centre, and its conic is a perfect square whose
        // gradient is zero along it. Mutation control: the general
        // branch answers NaN here, and the tangent-at-the-centre
        // fallback answers NaN too.
        PlaneCurve straight = page.curveFor(ninetyDegreesFrom(CENTRE));
        assertEquals("straight", straight.form(),
                "the great circle through the page centre is a line");

        // A pole at the centre: the circle IS the limb.
        PlaneCurve limb = page.curveFor(CENTRE);
        assertNotEquals("straight", limb.form(),
                "the circle whose pole is the centre is the limb, not"
                        + " a line");

        // And in between, an ellipse - which is the form the atlas's
        // other two projections never produce. Mutation control: a
        // circle here is what a projection that forgot the
        // foreshortening would draw.
        int elliptical = 0;
        for (double away = 15.0; away <= 75.0; away += 15.0) {
            PlaneCurve curve = page.curveFor(towards(CENTRE, away, 0.0));
            if ("elliptical".equals(curve.form())) {
                elliptical++;
            }
        }
        assertTrue(elliptical >= 4,
                "a hemisphere draws great circles as ellipses: "
                        + elliptical + " of 5");
    }

    @Test
    void everyGreatCircleStaysInsideItsOwnLimb() {
        // The Sprint 30 result, held rather than assumed: an
        // orthographic great circle's visible image is contained by
        // the limb. Mutation control: clipping to the paper alone
        // lets a curve run out across the margin, which is what
        // #331 has to prevent for every other kind of ink.
        ChartViewportFixture page = new ChartViewportFixture();
        for (double away = 0.0; away <= 90.0; away += 10.0) {
            for (double about = 0.0; about < 360.0; about += 45.0) {
                PlaneConic conic = GLOBE
                        .greatCircle(towards(CENTRE, away, about))
                        .orElseThrow();
                for (double t = 0.0; t < 2.0 * Math.PI; t += 0.05) {
                    double[] point = pointOn(conic, t);
                    if (point == null) {
                        continue;
                    }
                    assertTrue(Math.hypot(point[0], point[1])
                                    <= 1.0 + 1.0e-9,
                            "a great circle's image leaves the limb at "
                                    + point[0] + "," + point[1]);
                }
            }
        }
    }

    @Test
    void theUsefulReachIsTheLimbBecauseTheCornersArePaper() {
        assertEquals(90.0, GLOBE.usefulCornerDegrees(),
                "a globe page is a bounded disc, so its useful reach"
                        + " is its own edge (#301)");
        assertEquals(GLOBE.limitDegrees(), GLOBE.usefulCornerDegrees(),
                "and the two are the same question here, unlike a"
                        + " chart projection, which stops being worth"
                        + " reading long before it stops working");
    }

    @Test
    void nothingOutsideTheProjectionPackageNamesTheImplementation() throws Exception {
        // The registry is the one place a name becomes a projection,
        // and a globe is reached through it like the other two.
        Projection through = Projections.of(
                juranometria.chart.ChartProjection.ORTHOGRAPHIC, CENTRE);
        assertEquals("orthographic", through.name());
        assertEquals(OrthographicProjection.class, through.getClass());

        java.nio.file.Path src = java.nio.file.Path.of("src");
        try (var walk = java.nio.file.Files.walk(src)) {
            var offenders = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString()
                            .contains("juranometria/project/"))
                    .filter(p -> {
                        try {
                            return java.nio.file.Files.readString(p)
                                    .contains("new OrthographicProjection(");
                        } catch (java.io.IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    })
                    .toList();
            assertTrue(offenders.isEmpty(),
                    "the implementation is constructed outside its own"
                            + " package: " + offenders);
        }
    }

    /** A position exactly ninety degrees from another. */
    private static SkyPosition ninetyDegreesFrom(SkyPosition centre) {
        return towards(centre, 90.0, 0.0);
    }

    /** A position a stated angle from a centre, on a stated bearing. */
    private static SkyPosition towards(SkyPosition centre,
                                       double awayDegrees,
                                       double bearingDegrees) {
        double dec = Math.toRadians(centre.decDegrees());
        double ra = Math.toRadians(centre.raDegrees());
        double away = Math.toRadians(awayDegrees);
        double bearing = Math.toRadians(bearingDegrees);
        double newDec = Math.asin(Math.sin(dec) * Math.cos(away)
                + Math.cos(dec) * Math.sin(away) * Math.cos(bearing));
        double newRa = ra + Math.atan2(
                Math.sin(bearing) * Math.sin(away) * Math.cos(dec),
                Math.cos(away) - Math.sin(dec) * Math.sin(newDec));
        return new SkyPosition(
                (Math.toDegrees(newRa) % 360.0 + 360.0) % 360.0,
                Math.toDegrees(newDec));
    }

    /**
     * A point on the conic at a parameter, by walking the unit circle
     * and solving outward - enough to say whether the curve leaves
     * the limb.
     */
    private static double[] pointOn(PlaneConic conic, double angle) {
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        double a = conic.a() * dx * dx + conic.b() * dx * dy
                + conic.c() * dy * dy;
        double b = conic.d() * dx + conic.e() * dy;
        double c = conic.f();
        if (a == 0.0) {
            return b == 0.0 ? null : new double[] {-c / b * dx, -c / b * dy};
        }
        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0.0) {
            return null;
        }
        double root = (-b + Math.sqrt(discriminant)) / (2.0 * a);
        return new double[] {root * dx, root * dy};
    }

    /** A page at the globe rung, for the curve seam's own answers. */
    private static final class ChartViewportFixture {

        private final ViewportMapping mapping;

        private final PageRegion region;

        ChartViewportFixture() {
            juranometria.chart.ChartViewport viewport =
                    new juranometria.chart.ChartViewport(CENTRE, 180.0,
                            900, 700,
                            juranometria.chart.ChartProjection.ORTHOGRAPHIC);
            this.mapping = new ViewportMapping(viewport, GLOBE);
            this.region = mapping.regionFor(viewport, GLOBE);
        }

        PlaneCurve curveFor(SkyPosition pole) {
            return mapping.onPage(GLOBE.greatCircle(pole).orElseThrow(),
                    region);
        }
    }
}
