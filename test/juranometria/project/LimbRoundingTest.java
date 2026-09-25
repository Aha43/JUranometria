package juranometria.project;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.chart.Cardinal;
import juranometria.chart.ChartProjection;
import juranometria.chart.SkyPosition;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

/**
 * The orthographic limb admits exactly what rounding can put there
 * (#359 completion, owner ruling 3).
 *
 * <p>A position ninety degrees from the centre is on the limb; the
 * double arithmetic that decides so can land a hair either side of
 * zero. The projection admits a value within the error bound of its
 * own vector and dot-product arithmetic, derived for that position,
 * and refuses anything genuinely behind the limb. Nothing here
 * mentions latitude, poles or cardinals: the fixtures below are
 * positions, and the rule is the arithmetic's.
 */
class LimbRoundingTest {

    /** The limb point this far behind it, towards the far side. */
    private static SkyPosition behind(SkyPosition centre,
                                      SkyPosition onLimb,
                                      double radians) {
        double[] c = unit(centre);
        double[] p = unit(onLimb);
        double[] v = new double[3];
        for (int i = 0; i < 3; i++) {
            v[i] = p[i] * Math.cos(radians) - c[i] * Math.sin(radians);
        }
        return position(v);
    }

    @Test
    void anObserversHorizonPointsAllLieOnTheLimbOfTheirZenithGlobe() {
        // Every exact horizon point of a zenith-centred globe is on
        // its limb, whatever the place and instant - including an
        // observer on the equator, whose north point sits beside the
        // celestial pole, where the old test refused it every time.
        double[][] places = {{59.913, 10.752}, {69.65, 18.96},
                {0.0, -78.5}, {0.0, 0.0}, {1e-6, 12.0},
                {-33.87, 151.21}, {-55.98, -67.27}};
        String[] instants = {"2026-03-20T21:33:00Z",
                "2026-06-21T12:00:00Z", "2026-09-23T03:00:00Z",
                "2026-12-21T18:00:00Z"};
        for (double[] place : places) {
            for (String when : instants) {
                LocalSky sky = new LocalSky(new Observer(place[0],
                        place[1], Instant.parse(when)));
                Projection globe = Projections.of(
                        ChartProjection.ORTHOGRAPHIC, sky.zenith());
                for (Cardinal direction : Cardinal.values()) {
                    SkyPosition point = sky.cardinal(direction);
                    assertTrue(globe.project(point).isPresent(),
                            direction + " at " + place[0] + ", "
                                    + place[1] + " on " + when
                                    + " lies on the limb: " + point);
                }
            }
        }
    }

    @Test
    void aPointGenuinelyBehindTheLimbIsRefused() {
        SkyPosition centre = new SkyPosition(123.4, 56.7);
        Projection globe = Projections.of(ChartProjection.ORTHOGRAPHIC,
                centre);
        SkyPosition onLimb = position(perpendicular(unit(centre)));
        assertTrue(globe.project(onLimb).isPresent(),
                "the exact limb point is admitted");
        for (double radians : new double[] {1e-13, 1e-11, 1e-9, 1e-6,
                1e-3}) {
            SkyPosition far = behind(centre, onLimb, radians);
            assertTrue(globe.project(far).isEmpty(),
                    radians + " rad behind the limb is far side, not"
                            + " rounding: " + far);
        }
    }

    // ---- vectors -----------------------------------------------------

    private static double[] perpendicular(double[] c) {
        double[] a = Math.abs(c[2]) < 0.9 ? new double[] {0, 0, 1}
                : new double[] {1, 0, 0};
        double[] v = {c[1] * a[2] - c[2] * a[1],
                c[2] * a[0] - c[0] * a[2], c[0] * a[1] - c[1] * a[0]};
        double n = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / n, v[1] / n, v[2] / n};
    }

    private static double[] unit(SkyPosition p) {
        double ra = Math.toRadians(p.raDegrees());
        double dec = Math.toRadians(p.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static SkyPosition position(double[] v) {
        double ra = Math.toDegrees(Math.atan2(v[1], v[0]));
        return new SkyPosition(ra < 0 ? ra + 360.0 : ra,
                Math.toDegrees(Math.atan2(v[2], Math.hypot(v[0], v[1]))));
    }
}
