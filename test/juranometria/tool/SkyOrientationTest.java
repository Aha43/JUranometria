package juranometria.tool;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sky-orientation model, held to something other than itself
 * (Sprint 25, issue #225).
 *
 * <p>Three kinds of check, because none of them alone is enough.
 *
 * <ul>
 *   <li><strong>Published values.</strong> Sidereal time is a
 *       quantity astronomy has been printing for a century; the
 *       worked examples in Meeus, <em>Astronomical Algorithms</em>
 *       (2nd ed., ch. 12) are numbers this code did not produce.</li>
 *   <li><strong>An independent derivation.</strong> The Earth
 *       rotation angle of IAU 2000 plus the precession-in-right-
 *       ascension polynomial reaches sidereal time by a different
 *       road than the classical polynomial. Two roads arriving
 *       together is evidence; one road driven twice is not.</li>
 *   <li><strong>Invariants.</strong> A horizon is exactly ninety
 *       degrees from its zenith, a meridian passes through the poles
 *       and the zenith, an observer at the pole has the pole
 *       overhead. These hold whatever the epoch, and a rotation that
 *       quietly stopped being a rotation would break them.</li>
 * </ul>
 */
class SkyOrientationTest {

    private static Instant utc(int year, int month, int day,
                               int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0,
                ZoneOffset.UTC).toInstant();
    }

    private static final double ARCSECOND = 1.0 / 3600.0;

    // ---- published values ------------------------------------------

    @Test
    void siderealTimeAgreesWithThePublishedWorkedExamples() {
        // Meeus, example 12.a: 1987 April 10 at 0h UT, mean
        // sidereal time at Greenwich is 13h10m46.3668s.
        double atMidnight = SkyOrientation.gmstDegrees(
                SkyOrientation.julianDate(utc(1987, 4, 10, 0, 0, 0)));
        assertEquals(hours(13, 10, 46.3668), atMidnight, 1e-5,
                "the printed value, which this code did not produce");

        // Example 12.b: the same day at 19h21m00s UT gives
        // 8h34m57.0896s.
        double thatEvening = SkyOrientation.gmstDegrees(
                SkyOrientation.julianDate(utc(1987, 4, 10, 19, 21, 0)));
        assertEquals(hours(8, 34, 57.0896), thatEvening, 1e-5,
                "and the second example, on the same day");
    }

    @Test
    void theJulianDateAgreesWithThePublishedEpochs() {
        assertEquals(2451545.0, SkyOrientation.julianDate(
                        utc(2000, 1, 1, 12, 0, 0)), 1e-9,
                "J2000.0 is 2000 January 1 at 12h UT");
        assertEquals(2446470.5, SkyOrientation.julianDate(
                        utc(1986, 2, 9, 0, 0, 0)), 1e-9,
                "Meeus example 7.b");
    }

    @Test
    void nutationAgreesWithThePublishedWorkedExample() {
        // Meeus, example 22.a: 1987 April 10 at 0h TD.
        double t = SkyOrientation.centuries(
                SkyOrientation.julianDate(utc(1987, 4, 10, 0, 0, 0)));
        double[] nutation = SkyOrientation.nutationDegrees(t);

        assertEquals(-3.788 * ARCSECOND, nutation[0], 0.02 * ARCSECOND,
                "nutation in longitude, against the printed -3.788\"");
        assertEquals(9.443 * ARCSECOND, nutation[1], 0.02 * ARCSECOND,
                "and in obliquity, against 9.443\"");
        assertEquals(23.44094, SkyOrientation.meanObliquityDegrees(t), 1e-4,
                "with the mean obliquity of 23°26'27.407\"");
    }

    // ---- an independent derivation ---------------------------------

    /**
     * Sidereal time by the other road: the IAU 2000 Earth rotation
     * angle, plus the precession-in-right-ascension polynomial of
     * IAU 2006. Different constants, different structure, same
     * quantity.
     */
    private static double gmstByEarthRotationAngle(double julianDate) {
        double days = julianDate - 2451545.0;
        double era = 360.0 * (0.7790572732640 + 1.00273781191135448 * days);
        double t = days / 36525.0;
        double precession = (0.014506 + 4612.156534 * t
                + 1.3915817 * t * t - 0.00000044 * t * t * t
                - 0.000029956 * t * t * t * t) / 3600.0;
        return SkyOrientation.normalise(era + precession);
    }

    @Test
    void twoIndependentDerivationsOfSiderealTimeAgree() {
        double worst = 0;
        String worstCase = "none";
        for (int year : new int[] {1900, 1950, 2000, 2026, 2050, 2100}) {
            for (int month : new int[] {1, 4, 7, 10}) {
                double jd = SkyOrientation.julianDate(
                        utc(year, month, 15, 6, 30, 0));
                double mine = SkyOrientation.gmstDegrees(jd);
                double theirs = gmstByEarthRotationAngle(jd);
                double apart = Math.abs(difference(mine, theirs));
                if (apart > worst) {
                    worst = apart;
                    worstCase = year + "-" + month;
                }
            }
        }
        // Over two centuries the two differ by a fraction of an
        // arcsecond. They do not differ by *nothing*, and should not:
        // the classical polynomial and the IAU 2006 form rest on
        // different precession models, so the residual is real
        // astronomy rather than a defect. What matters to a chart is
        // its size - a tenth of a pixel at the narrowest field the
        // atlas offers.
        assertTrue(worst < 0.5 * ARCSECOND, String.format(
                "the two derivations differ by %.4f arcseconds at %s",
                worst * 3600.0, worstCase));
        assertTrue(worst > 0, "and they are genuinely different code");
    }

    // ---- invariants ------------------------------------------------

    private static final SkyOrientation.Observer OSLO =
            new SkyOrientation.Observer(59.913, 10.752);
    private static final SkyOrientation.Observer SOUTH =
            new SkyOrientation.Observer(-33.87, 151.21);
    private static final SkyOrientation.Observer EQUATOR =
            new SkyOrientation.Observer(0.0, 0.0);
    private static final SkyOrientation.Observer NORTH_POLE =
            new SkyOrientation.Observer(90.0, 0.0);
    private static final SkyOrientation.Observer SOUTH_POLE =
            new SkyOrientation.Observer(-90.0, 137.0);

    private static List<SkyOrientation.Observer> everywhere() {
        return List.of(OSLO, SOUTH, EQUATOR, NORTH_POLE, SOUTH_POLE,
                new SkyOrientation.Observer(51.48, -0.0015),   // west
                new SkyOrientation.Observer(-89.9, 0.0));      // near a pole
    }

    /** Instants chosen to catch an epoch shortcut and the seasons. */
    private static List<Instant> whenever() {
        return List.of(
                utc(2000, 1, 1, 12, 0, 0),      // J2000 itself
                utc(2026, 3, 20, 14, 33, 0),    // equinox
                utc(2026, 6, 21, 8, 24, 0),     // solstice
                utc(2026, 9, 23, 0, 5, 0),      // equinox
                utc(2026, 12, 21, 20, 3, 0),    // solstice
                utc(2050, 7, 4, 3, 0, 0),       // far from J2000
                utc(1975, 11, 30, 23, 59, 59)); // and far the other way
    }

    @Test
    void everyHorizonPointIsNinetyDegreesFromItsZenith() {
        for (SkyOrientation.Observer observer : everywhere()) {
            for (Instant instant : whenever()) {
                SkyPosition zenith = SkyOrientation.zenith(observer, instant,
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                for (SkyPosition point : SkyOrientation.horizon(observer,
                        instant,
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION, 72)) {
                    assertEquals(90.0, zenith.separationDegrees(point), 1e-6,
                            "the horizon is the circle ninety degrees from"
                                    + " overhead: " + observer + " at "
                                    + instant);
                }
            }
        }
    }

    @Test
    void theMeridianPassesThroughTheZenithAndBothPoles() {
        for (SkyOrientation.Observer observer : everywhere()) {
            for (Instant instant : whenever()) {
                SkyPosition zenith = SkyOrientation.zenith(observer, instant,
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                List<SkyPosition> meridian = SkyOrientation.meridian(observer,
                        instant,
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION, 360);

                assertTrue(nearest(meridian, zenith) < 1.0,
                        "the meridian runs through the zenith: "
                                + observer + " at " + instant);
                // The poles of date, carried into J2000 the same way.
                SkyPosition northOfDate = SkyOrientation.toJ2000(
                        new SkyPosition(0, 90),
                        SkyOrientation.julianDate(instant),
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                assertTrue(nearest(meridian, northOfDate) < 1.0,
                        "and through the celestial pole");
            }
        }
    }

    @Test
    void anObserverAtThePoleHasThePoleOverhead() {
        for (Instant instant : whenever()) {
            SkyPosition zenith = SkyOrientation.zenith(NORTH_POLE, instant,
                    SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
            SkyPosition poleOfDate = SkyOrientation.toJ2000(
                    new SkyPosition(0, 90), SkyOrientation.julianDate(instant),
                    SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
            assertEquals(0.0, zenith.separationDegrees(poleOfDate), 1e-6,
                    "standing at the north pole, the celestial pole of"
                            + " that date is overhead");
        }
    }

    @Test
    void anObserverOnTheEquatorHasBothPolesOnTheHorizon() {
        for (Instant instant : whenever()) {
            List<SkyPosition> horizon = SkyOrientation.horizon(EQUATOR,
                    instant, SkyOrientation.Fidelity.PRECESSION_AND_NUTATION,
                    720);
            for (double dec : new double[] {90, -90}) {
                SkyPosition pole = SkyOrientation.toJ2000(
                        new SkyPosition(0, dec),
                        SkyOrientation.julianDate(instant),
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
                assertTrue(nearest(horizon, pole) < 0.5,
                        "from the equator both celestial poles sit on the"
                                + " horizon: " + instant);
            }
        }
    }

    @Test
    void theZenithTurnsWithTheEarthAndNotWithTheChart() {
        // Fifteen degrees of right ascension per hour - asked of the
        // frame the definition lives in.
        //
        // Asked of J2000 instead, the answer came out 15.105, and
        // that is not an error: a frame rotation does not preserve
        // differences of right ascension, and at declination 60° a
        // third of a degree of precession moves an RA difference by
        // four arcminutes. It is also the reason the module must
        // carry every point of a curve through the rotation rather
        // than shifting a right ascension and calling it the
        // meridian.
        Instant first = utc(2026, 5, 5, 21, 0, 0);
        SkyPosition before = SkyOrientation.zenith(OSLO, first,
                SkyOrientation.Fidelity.EPOCH_SHORTCUT);
        SkyPosition after = SkyOrientation.zenith(OSLO,
                first.plusSeconds(3600),
                SkyOrientation.Fidelity.EPOCH_SHORTCUT);

        assertEquals(15.0410, difference(after.raDegrees(),
                        before.raDegrees()), 1e-3,
                "one sidereal hour of right ascension");
        assertEquals(before.decDegrees(), after.decDegrees(), 1e-9,
                "and the same declination: latitude has not changed");

        // And in the chart's own frame it is still a turn about the
        // pole of date: the zenith stays the same distance from it.
        SkyPosition poleOfDate = SkyOrientation.toJ2000(
                new SkyPosition(0, 90), SkyOrientation.julianDate(first),
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
        SkyPosition beforeJ2000 = SkyOrientation.zenith(OSLO, first,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
        SkyPosition afterJ2000 = SkyOrientation.zenith(OSLO,
                first.plusSeconds(3600),
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
        assertEquals(poleOfDate.separationDegrees(beforeJ2000),
                poleOfDate.separationDegrees(afterJ2000), 1e-6,
                "the same angle from the pole an hour later: the sky"
                        + " turned, the observer did not move");
    }

    @Test
    void aWholeSiderealDayBringsTheZenithBack() {
        Instant first = utc(2026, 5, 5, 21, 0, 0);
        // 23h56m04.0905s: the sidereal day.
        Instant later = first.plusMillis((long) (86164.0905 * 1000));
        SkyPosition before = SkyOrientation.zenith(OSLO, first,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
        SkyPosition after = SkyOrientation.zenith(OSLO, later,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);

        assertTrue(before.separationDegrees(after) < 1.0 * ARCSECOND * 60,
                "a sidereal day later the same sky is overhead, to"
                        + " within a minute of arc: "
                        + before.separationDegrees(after) * 3600 + "\"");
    }

    @Test
    void longitudeMovesTheZenithEastAndLatitudeMovesItNorth() {
        Instant instant = utc(2026, 5, 5, 21, 0, 0);
        // In the frame the definition lives in, for the reason
        // above: right ascension differences are not preserved by
        // the rotation into J2000.
        SkyPosition here = SkyOrientation.zenith(
                new SkyOrientation.Observer(59.913, 10.752), instant,
                SkyOrientation.Fidelity.EPOCH_SHORTCUT);
        SkyPosition eastwards = SkyOrientation.zenith(
                new SkyOrientation.Observer(59.913, 40.752), instant,
                SkyOrientation.Fidelity.EPOCH_SHORTCUT);
        SkyPosition northwards = SkyOrientation.zenith(
                new SkyOrientation.Observer(69.913, 10.752), instant,
                SkyOrientation.Fidelity.EPOCH_SHORTCUT);

        assertEquals(30.0, difference(eastwards.raDegrees(),
                        here.raDegrees()), 1e-6,
                "thirty degrees east is thirty degrees of right"
                        + " ascension: longitude is east-positive");
        assertEquals(10.0, northwards.decDegrees() - here.decDegrees(),
                1e-9, "and ten degrees north is ten of declination");
    }

    // ----------------------------------------------------------------

    private static double hours(int h, int m, double s) {
        return (h + m / 60.0 + s / 3600.0) * 15.0;
    }

    /** Signed difference in degrees, wrapped into (-180, 180]. */
    private static double difference(double a, double b) {
        double d = (a - b) % 360.0;
        if (d > 180) {
            d -= 360;
        }
        if (d <= -180) {
            d += 360;
        }
        return d;
    }

    private static double nearest(List<SkyPosition> curve, SkyPosition to) {
        double best = 180;
        for (SkyPosition point : curve) {
            best = Math.min(best, point.separationDegrees(to));
        }
        return best;
    }
}
