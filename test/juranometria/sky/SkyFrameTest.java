package juranometria.sky;

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
class SkyFrameTest {

    private static Instant utc(int year, int month, int day,
                               int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0,
                ZoneOffset.UTC).toInstant();
    }

    private static final double ARCSECOND = 1.0 / 3600.0;

    /** Observers carry an instant; these are re-pointed with at(). */
    private static final Instant EPOCH = utc(2000, 1, 1, 12, 0, 0);

    // ---- published values ------------------------------------------

    @Test
    void siderealTimeAgreesWithThePublishedWorkedExamples() {
        // Meeus, example 12.a: 1987 April 10 at 0h UT, mean
        // sidereal time at Greenwich is 13h10m46.3668s.
        double atMidnight = SkyFrame.gmstDegrees(
                SkyFrame.julianDate(utc(1987, 4, 10, 0, 0, 0)));
        assertEquals(hours(13, 10, 46.3668), atMidnight, 1e-5,
                "the printed value, which this code did not produce");

        // Example 12.b: the same day at 19h21m00s UT gives
        // 8h34m57.0896s.
        double thatEvening = SkyFrame.gmstDegrees(
                SkyFrame.julianDate(utc(1987, 4, 10, 19, 21, 0)));
        assertEquals(hours(8, 34, 57.0896), thatEvening, 1e-5,
                "and the second example, on the same day");
    }

    @Test
    void theJulianDateAgreesWithThePublishedEpochs() {
        assertEquals(2451545.0, SkyFrame.julianDate(
                        utc(2000, 1, 1, 12, 0, 0)), 1e-9,
                "J2000.0 is 2000 January 1 at 12h UT");
        assertEquals(2446470.5, SkyFrame.julianDate(
                        utc(1986, 2, 9, 0, 0, 0)), 1e-9,
                "Meeus example 7.b");
    }

    @Test
    void nutationAgreesWithThePublishedWorkedExample() {
        // Meeus, example 22.a: 1987 April 10 at 0h TD.
        double t = SkyFrame.centuries(
                SkyFrame.julianDate(utc(1987, 4, 10, 0, 0, 0)));
        double[] nutation = SkyFrame.nutationDegrees(t);

        assertEquals(-3.788 * ARCSECOND, nutation[0], 0.02 * ARCSECOND,
                "nutation in longitude, against the printed -3.788\"");
        assertEquals(9.443 * ARCSECOND, nutation[1], 0.02 * ARCSECOND,
                "and in obliquity, against 9.443\"");
        assertEquals(23.44094, SkyFrame.meanObliquityDegrees(t), 1e-4,
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
        return SkyFrame.normalise(era + precession);
    }

    @Test
    void twoIndependentDerivationsOfSiderealTimeAgree() {
        double worst = 0;
        String worstCase = "none";
        for (int year : new int[] {1900, 1950, 2000, 2026, 2050, 2100}) {
            for (int month : new int[] {1, 4, 7, 10}) {
                double jd = SkyFrame.julianDate(
                        utc(year, month, 15, 6, 30, 0));
                double mine = SkyFrame.gmstDegrees(jd);
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

    // ---- the rotation itself, end to end ---------------------------

    /**
     * The pole of the ecliptic, which precession turns the celestial
     * pole around. Its J2000 position follows from the obliquity
     * alone and owes nothing to the rotations under test.
     */
    private static final SkyPosition ECLIPTIC_POLE =
            new SkyPosition(270.0, 90.0 - 23.4392911);

    @Test
    void theCelestialPoleStaysOnTheCircleItIsSupposedToTurnOn() {
        // The strongest end-to-end check available offline. Every
        // component of this sprint can be individually right and the
        // combined rotation still wrong - a swapped order, a
        // transpose, a sign - and this catches all three: precession
        // moves the celestial pole around the *ecliptic* pole at a
        // fixed angular radius, the obliquity. A rotation assembled
        // wrongly takes the pole off that circle immediately.
        for (Instant instant : whenever()) {
            SkyPosition poleOfDate = SkyFrame.toJ2000(
                    new SkyPosition(0, 90),
                    SkyFrame.julianDate(instant));
            // The *true* pole, because production offers no
            // precession-only route to the mean one - so the circle
            // is met to within nutation rather than exactly. Ten
            // arcseconds is nutation's whole range; a wrong
            // composition, transpose or sign is degrees out, and
            // this still catches every one of them.
            assertEquals(23.4392911,
                    ECLIPTIC_POLE.separationDegrees(poleOfDate), 10 * ARCSECOND,
                    "the pole of " + instant + " is one obliquity from"
                            + " the ecliptic pole, to within nutation,"
                            + " as it must be for every date");
        }
    }

    @Test
    void thePoleTurnsAtThePublishedRateAndInThePublishedDirection() {
        // 50.29 arcseconds a year of general precession in longitude,
        // and the celestial pole therefore moves 50.29 sin(obliquity)
        // = 20.0 arcseconds a year. Textbook quantities, not this
        // code's.
        double years = 100.0;
        Instant start = utc(2000, 1, 1, 12, 0, 0);
        Instant later = start.plusSeconds((long) (years * 365.25 * 86400));

        SkyPosition atStart = SkyFrame.toJ2000(new SkyPosition(0, 90),
                SkyFrame.julianDate(start));
        SkyPosition atEnd = SkyFrame.toJ2000(new SkyPosition(0, 90),
                SkyFrame.julianDate(later));

        double moved = atStart.separationDegrees(atEnd) * 3600.0 / years;
        assertEquals(50.29 * Math.sin(Math.toRadians(23.4392911)), moved,
                0.15, "the pole moves at the published rate: " + moved
                        + " arcseconds a year");

        // And the right way round the ecliptic pole: the longitude
        // about it advances rather than retreats.
        // How far it went round, and that it kept going the same
        // way. The *sign* is not asserted: it belongs to the basis
        // this test builds for itself, not to the sky, and claiming
        // it would be dressing an arbitrary choice as physics.
        double firstCentury = difference(longitudeAboutEclipticPole(atEnd),
                longitudeAboutEclipticPole(atStart));
        SkyPosition twoCenturiesOn = SkyFrame.toJ2000(
                new SkyPosition(0, 90),
                SkyFrame.julianDate(start.plusSeconds(
                        (long) (2 * years * 365.25 * 86400))));
        double secondCentury = difference(
                longitudeAboutEclipticPole(twoCenturiesOn),
                longitudeAboutEclipticPole(atEnd));

        assertEquals(50.29 * years / 3600.0, Math.abs(firstCentury), 0.01,
                "it goes round by the published amount of general"
                        + " precession in longitude");
        assertTrue(Math.signum(firstCentury) == Math.signum(secondCentury),
                "and keeps going the same way round: " + firstCentury
                        + "° then " + secondCentury + "°");
    }

    @Test
    void nutationIsAppliedAndIsTheSizeItShouldBe() {
        // Measured as the gap between the true pole and the circle
        // the *mean* pole travels on, which is exactly what nutation
        // in obliquity is - and needs no precession-only route to
        // ask for. A nutation omitted leaves the gap at zero; one
        // applied twice, or in the wrong frame, leaves it far too
        // large.
        double biggest = 0;
        for (Instant instant : whenever()) {
            SkyPosition poleOfDate = SkyFrame.toJ2000(
                    new SkyPosition(0, 90),
                    SkyFrame.julianDate(instant));
            double gap = Math.abs(23.4392911
                    - ECLIPTIC_POLE.separationDegrees(poleOfDate)) * 3600.0;
            assertTrue(gap < 10.0,
                    "nutation is under ten arcseconds: " + gap
                            + "\" at " + instant);
            biggest = Math.max(biggest, gap);
        }
        assertTrue(biggest > 0.5,
                "and it is actually applied - the largest gap over"
                        + " these dates is " + biggest + "\"");
    }

    @Test
    void theEquationOfTheEquinoxesIsTheNutationItClaimsToBe() {
        // GAST - GMST must equal the nutation in longitude times the
        // cosine of the true obliquity. Two quantities the code
        // computes by different routes, which must agree.
        for (Instant instant : whenever()) {
            double jd = SkyFrame.julianDate(instant);
            double t = SkyFrame.centuries(jd);
            double[] nutation = SkyFrame.nutationDegrees(t);
            double expected = nutation[0] * Math.cos(Math.toRadians(
                    SkyFrame.meanObliquityDegrees(t) + nutation[1]));

            assertEquals(expected, difference(SkyFrame.gastDegrees(jd),
                            SkyFrame.gmstDegrees(jd)), 1e-9,
                    "the equation of the equinoxes at " + instant);
        }
    }

    @Test
    void theTransformationIsARotationAndNotSomethingElse() {
        // A rotation preserves every angle. Nothing that is not a
        // rotation does - a scaling, a projection, a matrix composed
        // in the wrong order with a transpose in it - so this is the
        // structural property to ask for, and it needs no inverse to
        // ask it with.
        List<SkyPosition> directions = List.of(
                new SkyPosition(0.0, 0.0),
                new SkyPosition(359.9, -0.1),
                new SkyPosition(83.8, -5.4),
                new SkyPosition(0.0, 89.9),
                new SkyPosition(180.0, -89.9),
                new SkyPosition(266.4, -28.9));

        for (Instant instant : whenever()) {
            double jd = SkyFrame.julianDate(instant);
            for (int i = 0; i < directions.size(); i++) {
                for (int j = i + 1; j < directions.size(); j++) {
                    SkyPosition first = directions.get(i);
                    SkyPosition second = directions.get(j);
                    assertEquals(first.separationDegrees(second),
                            SkyFrame.toJ2000(first, jd)
                                    .separationDegrees(
                                            SkyFrame.toJ2000(second, jd)),
                            1e-9,
                            "the angle between " + first + " and "
                                    + second + " survives the change of"
                                    + " frame at " + instant);
                }
            }
        }
    }

    /** Longitude about the ecliptic pole, for direction of travel. */
    private static double longitudeAboutEclipticPole(SkyPosition point) {
        double[] pole = SkyFrame.toVector(ECLIPTIC_POLE);
        double[] reference = SkyFrame.toVector(new SkyPosition(0, 0));
        double[] first = normalise(cross(cross(pole, reference), pole));
        double[] second = cross(pole, first);
        double[] v = SkyFrame.toVector(point);
        return Math.toDegrees(Math.atan2(dot(v, second), dot(v, first)));
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalise(double[] v) {
        double n = Math.sqrt(dot(v, v));
        return new double[] {v[0] / n, v[1] / n, v[2] / n};
    }

    // ---- invariants ------------------------------------------------

    private static final Observer OSLO =
            new Observer(59.913, 10.752, EPOCH);
    private static final Observer SOUTH =
            new Observer(-33.87, 151.21, EPOCH);
    private static final Observer EQUATOR =
            new Observer(0.0, 0.0, EPOCH);
    private static final Observer NORTH_POLE =
            new Observer(90.0, 0.0, EPOCH);
    private static final Observer SOUTH_POLE =
            new Observer(-90.0, 137.0, EPOCH);

    private static List<Observer> everywhere() {
        return List.of(OSLO, SOUTH, EQUATOR, NORTH_POLE, SOUTH_POLE,
                new Observer(51.48, -0.0015, EPOCH),   // west
                new Observer(-89.9, 0.0, EPOCH));      // near a pole
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
        for (Observer observer : everywhere()) {
            for (Instant instant : whenever()) {
                SkyPosition zenith = new LocalSky(observer.at(instant)).zenith();
                for (SkyPosition point : new LocalSky(observer.at(instant)).horizon().around(72)) {
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
        for (Observer observer : everywhere()) {
            for (Instant instant : whenever()) {
                SkyPosition zenith = new LocalSky(observer.at(instant)).zenith();
                List<SkyPosition> meridian = new LocalSky(observer.at(instant)).meridian().around(360);

                assertTrue(nearest(meridian, zenith) < 1.0,
                        "the meridian runs through the zenith: "
                                + observer + " at " + instant);
                // The poles of date, carried into J2000 the same way.
                SkyPosition northOfDate = SkyFrame.toJ2000(
                        new SkyPosition(0, 90),
                        SkyFrame.julianDate(instant));
                assertTrue(nearest(meridian, northOfDate) < 1.0,
                        "and through the celestial pole");
            }
        }
    }

    @Test
    void anObserverAtThePoleHasThePoleOverhead() {
        for (Instant instant : whenever()) {
            SkyPosition zenith = new LocalSky(NORTH_POLE.at(instant)).zenith();
            SkyPosition poleOfDate = SkyFrame.toJ2000(
                    new SkyPosition(0, 90), SkyFrame.julianDate(instant));
            assertEquals(0.0, zenith.separationDegrees(poleOfDate), 1e-6,
                    "standing at the north pole, the celestial pole of"
                            + " that date is overhead");
        }
    }

    @Test
    void anObserverOnTheEquatorHasBothPolesOnTheHorizon() {
        for (Instant instant : whenever()) {
            List<SkyPosition> horizon = new LocalSky(EQUATOR.at(instant)).horizon().around(720);
            for (double dec : new double[] {90, -90}) {
                SkyPosition pole = SkyFrame.toJ2000(
                        new SkyPosition(0, dec),
                        SkyFrame.julianDate(instant));
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
        // Asked of local sidereal time, which is where the
        // definition lives. There is no of-date route through the
        // model any more: production carries the full rotation and
        // offers nothing lesser (#226).
        double before = new LocalSky(OSLO.at(first))
                .localSiderealTimeDegrees();
        double after = new LocalSky(OSLO.at(first.plusSeconds(3600)))
                .localSiderealTimeDegrees();

        assertEquals(15.0410, difference(after, before), 1e-3,
                "one sidereal hour of right ascension");

        // And in the chart's own frame it is still a turn about the
        // pole of date: the zenith stays the same distance from it.
        SkyPosition poleOfDate = SkyFrame.toJ2000(
                new SkyPosition(0, 90), SkyFrame.julianDate(first));
        SkyPosition beforeJ2000 = new LocalSky(OSLO.at(first)).zenith();
        SkyPosition afterJ2000 =
                new LocalSky(OSLO.at(first.plusSeconds(3600))).zenith();
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
        SkyPosition before = new LocalSky(OSLO.at(first)).zenith();
        SkyPosition after = new LocalSky(OSLO.at(later)).zenith();

        assertTrue(before.separationDegrees(after) < 1.0 * ARCSECOND * 60,
                "a sidereal day later the same sky is overhead, to"
                        + " within a minute of arc: "
                        + before.separationDegrees(after) * 3600 + "\"");
    }

    @Test
    void longitudeMovesTheZenithEastAndLatitudeMovesItNorth() {
        Instant instant = utc(2026, 5, 5, 21, 0, 0);
        // In the frame the definitions live in: local sidereal time
        // for longitude, latitude for declination. Right ascension
        // differences are not preserved by the rotation into J2000,
        // which is exactly why the module must carry whole curves
        // through it rather than shifting a right ascension.
        LocalSky here = new LocalSky(new Observer(59.913, 10.752, instant));
        LocalSky eastwards =
                new LocalSky(new Observer(59.913, 40.752, instant));
        LocalSky northwards =
                new LocalSky(new Observer(69.913, 10.752, instant));

        assertEquals(30.0, difference(
                        eastwards.localSiderealTimeDegrees(),
                        here.localSiderealTimeDegrees()), 1e-9,
                "thirty degrees east is thirty degrees of sidereal"
                        + " time: longitude is east-positive");
        assertEquals(10.0,
                northwards.observer().latitudeDegrees()
                        - here.observer().latitudeDegrees(), 1e-9,
                "and ten degrees north is ten of latitude, which the"
                        + " zenith carries as its declination of"
                        + " date");
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
