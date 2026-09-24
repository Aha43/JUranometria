package juranometria.project;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The visible half of a great circle, held to the owner's ruling on
 * the pan regression: a bounded projection's curve carries its own
 * visibility, the invisible hemisphere is excluded before drawing,
 * the degeneracy is resolved upstream by the projection's closed
 * form, the limb guard is unrelaxed, and pages whose answer was
 * already right are untouched to the digit.
 */
class VisibleGreatCircleTest {

    private static final Observer OSLO = new Observer(59.9, 10.7,
            java.time.Instant.parse("2026-03-20T21:33:00Z"));
    private static final LocalSky SKY = new LocalSky(OSLO);

    private record Setup(Projection projection, ViewportMapping mapping,
                         PageRegion region, ChartViewport viewport) {

        static Setup globe(SkyPosition centre) {
            ChartViewport viewport = new ChartViewport(centre, 180.0,
                    920, 920);
            Projection projection = Projections.forViewport(viewport);
            ViewportMapping mapping =
                    new ViewportMapping(viewport, projection);
            return new Setup(projection, mapping,
                    mapping.regionFor(viewport, projection), viewport);
        }
    }

    /** Sky positions of every sampled point of the drawn runs. */
    private static java.util.List<SkyPosition> drawnSky(Setup on,
            SkyPosition pole) {
        List<CurveRun> runs = GreatCirclePage.clip(on.projection(),
                on.mapping(), on.region(), pole);
        java.util.List<SkyPosition> out = new java.util.ArrayList<>();
        double scale = on.mapping().pixelsPerPlaneUnit();
        PixelPoint centre = on.mapping().toPixel(new PlanePoint(0, 0));
        for (CurveRun run : runs) {
            for (int s = 0; s <= 400; s++) {
                PixelPoint at = run.at(s / 400.0);
                on.projection().unproject(new PlanePoint(
                                (centre.x() - at.x()) / scale,
                                (centre.y() - at.y()) / scale))
                        .ifPresent(out::add);
            }
        }
        return out;
    }

    @Test
    void everyDrawnPointIsOnTheCircleAndOnTheNearSide() {
        // The two halves of correctness at once, for the horizon,
        // the meridian's own circle and the ecliptic, at obliquities
        // spread around the sphere: each drawn point unprojects onto
        // the circle (the geometry is right) and onto the visible
        // hemisphere (the fold is gone).
        SkyPosition[] poles = {SKY.zenith(),
                SKY.cardinal(juranometria.chart.Cardinal.EAST),
                new SkyPosition(270.0, 66.5607)};
        for (SkyPosition pole : poles) {
            for (double theta : new double[] {15, 45, 80, 89.9, 90.1,
                    120, 165}) {
                SkyPosition centre = away(pole, theta);
                Setup on = Setup.globe(centre);
                java.util.List<SkyPosition> drawn = drawnSky(on, pole);
                assertTrue(!drawn.isEmpty(),
                        "the circle crosses a hemisphere at theta "
                                + theta);
                for (SkyPosition s : drawn) {
                    double offCircle = Math.abs(90.0
                            - pole.separationDegrees(s));
                    assertTrue(offCircle < 0.02,
                            "a drawn point lies on its circle"
                                    + " (theta " + theta + "): off by "
                                    + offCircle + " degrees");
                    double fromCentre = centre.separationDegrees(s);
                    assertTrue(fromCentre <= 90.0 + 0.02,
                            "and on the visible hemisphere (theta "
                                    + theta + "): " + fromCentre
                                    + " degrees from centre");
                }
            }
        }
    }

    @Test
    void knownNearPointsAreDrawnAndKnownFarPointsAreNot() {
        // Not only nothing wrong drawn: the right things still are.
        // Sample the true circle; every visible point must be close
        // to some drawn point, and every invisible point far from
        // all of them.
        SkyPosition pole = SKY.zenith();
        SkyPosition centre = away(pole, 60.0);
        Setup on = Setup.globe(centre);
        java.util.List<SkyPosition> drawn = drawnSky(on, pole);

        double[] p = unit(pole);
        double[] c = unit(centre);
        double a3 = dot(p, c);
        double[] uNear = norm(sub(c, scale(p, a3)));
        double[] v = cross(p, uNear);
        int nearChecked = 0;
        int farChecked = 0;
        for (int i = 0; i < 72; i++) {
            double t = Math.toRadians(i * 5.0);
            double[] s = add(scale(uNear, Math.cos(t)),
                    scale(v, Math.sin(t)));
            SkyPosition sky = position(s);
            double visible = centre.separationDegrees(sky);
            double nearest = 1e9;
            for (SkyPosition d : drawn) {
                nearest = Math.min(nearest, sky.separationDegrees(d));
            }
            if (visible < 88.0) {
                nearChecked++;
                assertTrue(nearest < 0.5,
                        "a visible circle point is drawn: nearest ink "
                                + nearest + " degrees away at t=" + t);
            } else if (visible > 92.0) {
                farChecked++;
                assertTrue(nearest > 1.0,
                        "an invisible circle point is absent: ink "
                                + nearest + " degrees away at t=" + t);
            }
        }
        assertTrue(nearChecked > 20 && farChecked > 20,
                "both hemispheres were really sampled: " + nearChecked
                        + " near, " + farChecked + " far");
    }

    @Test
    void aDenseSweepThroughTheDegeneracyIsCleanAndContinuous() {
        // The band that threw: pole-to-centre through ninety, finely.
        // No exception, no limb overshoot past the unrelaxed graze,
        // and no discontinuous jump - successive drawn arcs stay
        // within a step-sized distance of each other.
        SkyPosition pole = SKY.zenith();
        PixelPoint previousMid = null;
        double previousTheta = 0;
        for (double theta = 89.0; theta <= 91.0 + 1e-9; theta += 0.002) {
            Setup on = Setup.globe(away(pole, theta));
            List<CurveRun> runs = GreatCirclePage.clip(on.projection(),
                    on.mapping(), on.region(), pole);
            assertEquals(1, runs.size(),
                    "one visible run at theta " + theta);
            CurveRun run = runs.get(0);
            if (run instanceof CurveRun.Arc arc) {
                double apart = Math.hypot(
                        arc.centreX() - on.region().limbX(),
                        arc.centreY() - on.region().limbY());
                double overshoot = apart + Math.max(arc.radiusAlong(),
                        arc.radiusAcross()) - on.region().limbRadius();
                assertTrue(overshoot <= on.region().limbRadius()
                                * 1.0e-7,
                        "no limb overshoot past the unrelaxed graze"
                                + " at theta " + theta + ": "
                                + overshoot);
            }
            PixelPoint mid = run.at(0.5);
            if (previousMid != null) {
                double moved = Math.hypot(mid.x() - previousMid.x(),
                        mid.y() - previousMid.y());
                // A 0.002-degree step moves the curve by about
                // R * step-in-radians ~ 0.015 px; ten times that is
                // continuity, a jump is orders more.
                assertTrue(moved < 0.5,
                        "the drawn curve moves continuously across"
                                + " theta " + previousTheta + " -> "
                                + theta + ": " + moved + " px");
            }
            previousMid = mid;
            previousTheta = theta;
        }
    }

    @Test
    void tangentPlanePagesAnswerToTheDigitWhatTheyAlwaysDid() {
        // The projections whose whole conic was already right carry
        // no visible-half statement, and their runs are pinned to
        // the exact values measured before the repair.
        ChartViewport gnomonic = new ChartViewport(
                new SkyPosition(83.0, 20.0), 8.0, 900, 700);
        ChartViewport stereographic = new ChartViewport(
                new SkyPosition(151.9, 30.0), 120.0, 900, 700);
        SkyPosition horizonPole = SKY.zenith();
        SkyPosition eclipticPole = new SkyPosition(270.0, 66.5607);

        assertTrue(Projections.forViewport(gnomonic)
                        .greatCircleVisibleHalf(horizonPole).isEmpty()
                        && Projections.forViewport(stereographic)
                        .greatCircleVisibleHalf(eclipticPole).isEmpty(),
                "unbounded projections state no visible half and keep"
                        + " their conic path");

        assertEquals(
                "844.332907786,0.000000000 858.249680839,0.676546023"
                        + " 872.166453893,1.353092046"
                        + " 886.083226946,2.029638068"
                        + " 900.000000000,2.706184091",
                fingerprint(gnomonic, eclipticPole),
                "the gnomonic page's ecliptic, to the digit");
        assertEquals(
                "900.000000000,257.105353067 687.866687036,373.876983364"
                        + " 465.888753782,470.634170483"
                        + " 235.949556937,546.555984641"
                        + " -0.000000000,600.998272363",
                fingerprint(stereographic, eclipticPole),
                "the stereographic page's ecliptic, to the digit");
        assertEquals(
                "900.000000000,679.122611287 890.527920267,684.515338345"
                        + " 880.991245044,689.792991735"
                        + " 871.391372264,694.954797834"
                        + " 861.729709119,700.000000000"
                        + " 37.986013960,700.000000000"
                        + " 28.396857375,694.993216034"
                        + " 18.868553848,689.871571610"
                        + " 9.402479134,684.635806222"
                        + " -0.000000000,679.286675841",
                fingerprint(stereographic, horizonPole),
                "the stereographic page's horizon, to the digit");
    }

    private static String fingerprint(ChartViewport viewport,
                                      SkyPosition pole) {
        Projection projection = Projections.forViewport(viewport);
        ViewportMapping mapping =
                new ViewportMapping(viewport, projection);
        StringBuilder said = new StringBuilder();
        for (CurveRun run : GreatCirclePage.clip(projection, mapping,
                mapping.regionFor(viewport, projection), pole)) {
            for (double f : new double[] {0.0, 0.25, 0.5, 0.75, 1.0}) {
                PixelPoint at = run.at(f);
                if (said.length() > 0) {
                    said.append(' ');
                }
                said.append(String.format(java.util.Locale.ROOT,
                        "%.9f,%.9f", at.x(), at.y()));
            }
        }
        return said.toString();
    }

    // ---- vector helpers --------------------------------------------

    private static SkyPosition away(SkyPosition pole, double degrees) {
        double[] p = unit(pole);
        double[] axis = Math.abs(p[2]) < 0.9
                ? new double[] {0, 0, 1} : new double[] {1, 0, 0};
        double[] u = norm(cross(axis, p));
        double t = Math.toRadians(degrees);
        return position(add(scale(p, Math.cos(t)),
                scale(u, Math.sin(t))));
    }

    private static double[] unit(SkyPosition sp) {
        double ra = Math.toRadians(sp.raDegrees());
        double dec = Math.toRadians(sp.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static SkyPosition position(double[] v) {
        double[] n = norm(v);
        double ra = Math.toDegrees(Math.atan2(n[1], n[0]));
        if (ra < 0) {
            ra += 360.0;
        }
        return new SkyPosition(ra, Math.toDegrees(Math.asin(
                Math.max(-1.0, Math.min(1.0, n[2])))));
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] scale(double[] a, double s) {
        return new double[] {a[0] * s, a[1] * s, a[2] * s};
    }

    private static double[] norm(double[] a) {
        double n = Math.sqrt(dot(a, a));
        return new double[] {a[0] / n, a[1] / n, a[2] / n};
    }
}
