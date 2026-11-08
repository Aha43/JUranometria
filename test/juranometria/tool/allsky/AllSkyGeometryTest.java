package juranometria.tool.allsky;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.sky.Ecliptic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #686 study's projection mathematics held by execution rather
 * than by citation (the USGS source could not be fetched; the
 * formulas came from MathWorld's transcription of Snyder 1987).
 */
class AllSkyGeometryTest {

    private static final double SQRT2 = Math.sqrt(2.0);

    @Test
    void everyFormRoundTripsTheWholeSphere() {
        for (AllSkyForm form : AllSkyForm.values()) {
            double worst = 0.0;
            for (int lat = -89; lat <= 89; lat++) {
                for (int lon = -179; lon <= 179; lon++) {
                    double l = Math.toRadians(lon + 0.37);
                    double p = Math.toRadians(lat + 0.21);
                    double[] xy = form.forward(l, p);
                    assertTrue(form.inside(xy[0], xy[1]), form + " " + lon + "," + lat);
                    double[] back = form.inverse(xy[0], xy[1]);
                    assertNotNull(back, form + " " + lon + "," + lat);
                    worst = Math.max(worst, Math.max(Math.abs(back[0] - l),
                            Math.abs(back[1] - p)));
                }
            }
            assertTrue(worst < 1e-9, form + " round trip " + worst);
        }
    }

    @Test
    void mollweideAuxiliaryAngleSatisfiesItsEquation() {
        double worst = 0.0;
        for (int i = -9000; i <= 9000; i++) {
            double phi = Math.toRadians(i / 100.0);
            double theta = AllSkyForm.mollweideTheta(phi);
            double residual = 2 * theta + Math.sin(2 * theta) - Math.PI * Math.sin(phi);
            worst = Math.max(worst, Math.abs(residual));
        }
        assertTrue(worst < 1e-12, "residual " + worst);
        // The root at 45 degrees that rejected MathWorld's eq (6) first guess.
        double root = 2 * AllSkyForm.mollweideTheta(Math.PI / 4);
        double eq6 = 2 * Math.asin(2 * (Math.PI / 4) / Math.PI);
        assertEquals(1.2672, root, 1e-4);
        assertEquals(1.0472, eq6, 1e-4);
        assertTrue(root - eq6 > 0.2, "eq (6) is not a start for this equation");
        assertEquals(Math.PI / 2, AllSkyForm.mollweideTheta(Math.PI / 2), 0.0);
    }

    @Test
    void outlinesAreTheTwoToOneEllipseAndItsRectangle() {
        for (AllSkyForm form : new AllSkyForm[] {AllSkyForm.HAMMER, AllSkyForm.MOLLWEIDE}) {
            for (int lat = -90; lat <= 90; lat += 5) {
                double[] edge = form.forward(Math.PI, Math.toRadians(lat));
                assertEquals(1.0, edge[0] * edge[0] / 8 + edge[1] * edge[1] / 2, 1e-9,
                        form + " at " + lat);
            }
            assertEquals(2 * SQRT2, form.halfWidth(), 0.0);
            assertEquals(SQRT2, form.forward(0, Math.PI / 2)[1], 1e-12);
            assertNull(form.inverse(2 * SQRT2, 0.5));
        }
        AllSkyForm eq = AllSkyForm.EQUIRECTANGULAR;
        assertEquals(SQRT2, eq.forward(0, Math.PI / 2)[1], 1e-12);
        assertEquals(2 * SQRT2, eq.halfWidth(), 1e-12);
    }

    /** Area element by the Jacobian of the forward map, per unit cos(phi) dlambda dphi. */
    private static double areaScale(AllSkyForm form, double l, double p) {
        double h = 1e-6;
        double[] a = form.forward(l - h, p);
        double[] b = form.forward(l + h, p);
        double[] c = form.forward(l, p - h);
        double[] d = form.forward(l, p + h);
        double xl = (b[0] - a[0]) / (2 * h);
        double yl = (b[1] - a[1]) / (2 * h);
        double xp = (d[0] - c[0]) / (2 * h);
        double yp = (d[1] - c[1]) / (2 * h);
        return Math.abs(xl * yp - xp * yl) / Math.cos(p);
    }

    @Test
    void theTwoEllipsesAreEqualAreaAndTheControlIsNot() {
        for (AllSkyForm form : AllSkyForm.values()) {
            double min = Double.MAX_VALUE;
            double max = 0.0;
            for (int lat = -80; lat <= 80; lat += 10) {
                for (int lon = -170; lon <= 170; lon += 20) {
                    double s = areaScale(form, Math.toRadians(lon), Math.toRadians(lat));
                    min = Math.min(min, s);
                    max = Math.max(max, s);
                }
            }
            if (form.equalArea()) {
                // Whole sphere 4 pi onto an ellipse of area pi * 2 sqrt2 * sqrt2 = 4 pi.
                assertEquals(1.0, min, 1e-6, form.toString());
                assertEquals(1.0, max, 1e-6, form.toString());
            } else {
                assertTrue(max / min > 5.0, "equirectangular area ratio " + max / min);
            }
        }
    }

    @Test
    void eastIsPositiveAndTheSeamIsOppositeTheCentralMeridian() {
        AllSkyMap map = new AllSkyMap(AllSkyForm.HAMMER, AllSkyOrientation.EQUATORIAL, 90.0);
        assertTrue(map.project(new SkyPosition(100.0, 0.0))[0] > 0.0,
                "increasing right ascension is celestial east, x positive");
        assertEquals(0.0, map.project(new SkyPosition(90.0, 0.0))[0], 1e-12);
        assertEquals(0.0, map.seamDistanceDegrees(new SkyPosition(270.0, 10.0)), 1e-9);
        assertTrue(map.crossesSeam(new SkyPosition(268.0, 0.0), new SkyPosition(272.0, 0.0)));
        assertFalse(map.crossesSeam(new SkyPosition(88.0, 0.0), new SkyPosition(92.0, 0.0)));
    }

    @Test
    void theEclipticOrientationPutsTheEclipticOnTheMiddleLine() {
        AllSkyMap map = new AllSkyMap(AllSkyForm.MOLLWEIDE, AllSkyOrientation.ECLIPTIC, 0.0);
        for (int lon = 0; lon < 360; lon += 15) {
            SkyPosition on = Ecliptic.toEquatorial(lon, 0.0);
            assertEquals(0.0, map.project(on)[1], 1e-9, "ecliptic longitude " + lon);
        }
        SkyPosition back = map.unproject(0.3, 0.4);
        double[] xy = map.project(back);
        assertEquals(0.3, xy[0], 1e-9);
        assertEquals(0.4, xy[1], 1e-9);
    }
}
