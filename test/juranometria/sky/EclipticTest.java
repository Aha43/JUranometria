package juranometria.sky;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ecliptic geometry, held to itself (Sprint 28, issue #272).
 *
 * <p>These are <strong>invariants</strong>: they constrain the
 * rotation from several sides and say <em>which</em> part of it went
 * wrong when one fails. They deliberately do not stand in for the
 * absolute oracle - a coherent error in orientation satisfies every
 * one of them, which is what {@link EclipticReferenceVectorTest} is
 * for, and what {@link EclipticMutationTest} demonstrates.
 */
class EclipticTest {

    /** Machine precision is generous here; nothing is measured. */
    private static final double EXACT = 1.0e-9;

    @Test
    void theEquinoxesAreAtTheRightAscensionOriginAndItsOpposite() {
        // Right ascension is measured from the March equinox, so on a
        // J2000 chart the ecliptic crosses the equator at exactly
        // (0h, 0) and (12h, 0). If this fails the longitude origin is
        // wrong.
        SkyPosition march = Ecliptic.toEquatorial(0.0, 0.0);
        assertEquals(0.0, march.raDegrees(), EXACT,
                "the March equinox is at right ascension zero");
        assertEquals(0.0, march.decDegrees(), EXACT,
                "and on the equator");

        SkyPosition september = Ecliptic.toEquatorial(180.0, 0.0);
        assertEquals(180.0, september.raDegrees(), EXACT,
                "the September equinox is opposite it");
        assertEquals(0.0, september.decDegrees(), EXACT,
                "and also on the equator");
    }

    @Test
    void theSolsticesAreTheExtremaAndCarryTheObliquity() {
        // If the declinations here are right but the extrema are
        // elsewhere, the rotation axis is wrong rather than its
        // angle.
        SkyPosition june = Ecliptic.toEquatorial(90.0, 0.0);
        assertEquals(90.0, june.raDegrees(), EXACT,
                "the June solstice is a quarter turn along");
        assertEquals(Ecliptic.OBLIQUITY_DEGREES, june.decDegrees(), EXACT,
                "and as far north as the obliquity");

        SkyPosition december = Ecliptic.toEquatorial(270.0, 0.0);
        assertEquals(270.0, december.raDegrees(), EXACT,
                "the December solstice is three quarters along");
        assertEquals(-Ecliptic.OBLIQUITY_DEGREES, december.decDegrees(),
                EXACT, "and as far south");

        // And they really are the extrema: nothing on the circle
        // reaches further, checked all the way round.
        double north = -91.0;
        double south = 91.0;
        double northAt = -1;
        for (int tenth = 0; tenth < 3600; tenth++) {
            double dec = Ecliptic.toEquatorial(tenth / 10.0, 0.0)
                    .decDegrees();
            if (dec > north) {
                north = dec;
                northAt = tenth / 10.0;
            }
            south = Math.min(south, dec);
        }
        assertEquals(Ecliptic.OBLIQUITY_DEGREES, north, 1.0e-6,
                "the northern extreme is the obliquity");
        assertEquals(-Ecliptic.OBLIQUITY_DEGREES, south, 1.0e-6,
                "and the southern extreme its negative");
        assertEquals(90.0, northAt, 0.05,
                "and the northern extreme is at the June solstice,"
                        + " not somewhere else with the right value");
    }

    @Test
    void theHandednessIsEastwardFromTheMarchEquinox() {
        // Ecliptic longitude increases eastward, so a little past the
        // March equinox is a little north of the equator, not south.
        // A sign error in longitude - or in the obliquity - shows
        // here and nowhere in the extrema.
        assertTrue(Ecliptic.toEquatorial(1.0, 0.0).decDegrees() > 0,
                "just past the March equinox the ecliptic runs north");
        assertTrue(Ecliptic.toEquatorial(181.0, 0.0).decDegrees() < 0,
                "and just past the September equinox, south");
        assertTrue(Ecliptic.toEquatorial(1.0, 0.0).raDegrees() > 0
                        && Ecliptic.toEquatorial(1.0, 0.0).raDegrees() < 5,
                "and right ascension increases with longitude");
    }

    @Test
    void thePoleIsTheObliquityFromTheCelestialPole() {
        // The whole geometry reduces to this: if it holds, the circle
        // is the ecliptic and no other circle.
        assertEquals(Ecliptic.OBLIQUITY_DEGREES,
                Ecliptic.POLE.separationDegrees(new SkyPosition(0.0, 90.0)),
                EXACT,
                "the ecliptic pole is the obliquity from the"
                        + " celestial pole");
        assertEquals(270.0, Ecliptic.POLE.raDegrees(), EXACT,
                "on the 18h meridian, so the June solstice is north");
    }

    @Test
    void everyLandmarkLiesOnTheCircle() {
        GreatCircle circle = Ecliptic.circle();
        for (Ecliptic.Landmark landmark : Ecliptic.landmarks()) {
            assertTrue(circle.contains(landmark.at(), 1.0e-9),
                    landmark.name() + " lies on the ecliptic");
            assertEquals(0.0, Ecliptic.latitudeDegrees(landmark.at()),
                    1.0e-9,
                    landmark.name() + " is at zero ecliptic latitude");
        }
    }

    @Test
    void theLandmarksAreTheFourTheGateNamed() {
        List<Ecliptic.Landmark> landmarks = Ecliptic.landmarks();
        assertEquals(List.of("march-equinox", "june-solstice",
                        "september-equinox", "december-solstice"),
                landmarks.stream().map(Ecliptic.Landmark::identity)
                        .toList(),
                "four cardinal landmarks, in order of longitude");
        assertEquals(List.of("March equinox", "June solstice",
                        "September equinox", "December solstice"),
                landmarks.stream().map(Ecliptic.Landmark::name).toList(),
                "named by month, because the geometry has no observer"
                        + " to have a season");
        assertEquals(List.of(0.0, 90.0, 180.0, 270.0),
                landmarks.stream()
                        .map(Ecliptic.Landmark::longitudeDegrees).toList(),
                "at the cardinal longitudes");
        assertTrue(Ecliptic.landmark("june-solstice").isPresent(),
                "and each is reachable by its identity");
        assertTrue(Ecliptic.landmark("midsummer").isEmpty(),
                "and nothing else is");
    }

    @Test
    void longitudeWrapsContinuouslyThroughTheOrigin() {
        // The seam is where an unwrapped angle would either throw or
        // jump. Neither may happen.
        SkyPosition before = Ecliptic.toEquatorial(359.999, 0.0);
        SkyPosition after = Ecliptic.toEquatorial(0.001, 0.0);
        assertTrue(before.separationDegrees(after) < 0.01,
                "the two sides of the origin are next to each other,"
                        + " not a degree apart");
        assertTrue(before.raDegrees() > 359.0,
                "and the one before it stays in [0, 360)");

        assertEquals(0.0, Ecliptic.toEquatorial(360.0, 0.0)
                        .separationDegrees(Ecliptic.toEquatorial(0.0, 0.0)),
                EXACT, "a full turn is where it started");
        assertEquals(0.0, Ecliptic.toEquatorial(-90.0, 0.0)
                        .separationDegrees(
                                Ecliptic.toEquatorial(270.0, 0.0)),
                EXACT, "and a negative longitude is the same place"
                        + " as its positive twin");
    }

    @Test
    void theRoundTripReturnsTheAngleItWasGiven() {
        // Constrains the rotation from the other side, and says which
        // of the two angles moved when one does.
        for (double lambda = 0.0; lambda < 360.0; lambda += 7.0) {
            for (double beta : new double[] {-80, -23.5, 0, 12.25, 66}) {
                Ecliptic.Coordinates back = Ecliptic.toEcliptic(
                        Ecliptic.toEquatorial(lambda, beta));
                assertEquals(lambda, back.longitudeDegrees(), 1.0e-9,
                        "longitude survives the round trip at beta "
                                + beta);
                assertEquals(beta, back.latitudeDegrees(), 1.0e-9,
                        "and so does latitude at longitude " + lambda);
            }
        }
    }

    @Test
    void latitudeIsTheDistanceFromThePlane() {
        // Two independent routes to the same number: the pole
        // separation and the inverse rotation. If they disagree the
        // pole and the rotation have drifted apart.
        for (double lambda = 0.0; lambda < 360.0; lambda += 11.0) {
            for (double beta : new double[] {-45, -3, 0, 3, 45}) {
                SkyPosition at = Ecliptic.toEquatorial(lambda, beta);
                assertEquals(beta, Ecliptic.latitudeDegrees(at), 1.0e-9,
                        "the pole says the same as the rotation at "
                                + lambda + ", " + beta);
            }
        }
    }

    @Test
    void thePolesOfTheEclipticAreAtNinetyDegreesLatitude() {
        assertEquals(90.0,
                Ecliptic.toEcliptic(Ecliptic.POLE).latitudeDegrees(),
                1.0e-9, "the ecliptic pole is at ecliptic latitude 90");
        // The celestial pole sits at ecliptic longitude 90 and
        // latitude 90 - obliquity: it is the obliquity away from the
        // ecliptic pole, on the solstice side. That the rotation puts
        // it exactly on the equatorial pole is a check on latitude
        // that the plane alone cannot give.
        assertEquals(90.0,
                Ecliptic.toEquatorial(90.0, 90.0 - Ecliptic
                        .OBLIQUITY_DEGREES).decDegrees(), 1.0e-9,
                "and the celestial pole is at ecliptic (90, 90-eps),"
                        + " so the transformation carries latitude too");
    }

    @Test
    void badAnglesAreRefusedRatherThanQuietlyWrapped() {
        assertThrows(IllegalArgumentException.class,
                () -> Ecliptic.toEquatorial(0.0, 90.001),
                "a latitude off the sphere is a mistake, not a wrap");
        assertThrows(IllegalArgumentException.class,
                () -> Ecliptic.toEquatorial(Double.NaN, 0.0),
                "and neither angle may be NaN");
        assertThrows(IllegalArgumentException.class,
                () -> Ecliptic.toEcliptic(null),
                "and a direction is somewhere");
    }

    @Test
    void nothingHereCanReadALocaleAZoneOrAClock()
            throws java.io.IOException {
        // The model states it is deterministic on every host. An
        // earlier version of this test proved it by swapping the
        // default locale and time zone, which meant a pure-geometry
        // test mutating process-wide state - and the evidence gate
        // rightly refused it, since a global touched outside the
        // shared guard is a global that escapes one afternoon.
        //
        // The constant pool answers the same question better: a class
        // that cannot refer to a locale, a calendar, a formatter or a
        // clock cannot vary with one. It is also the check the
        // removable-model boundary already uses, for the same reason.
        String pool = new String(java.nio.file.Files.readAllBytes(
                        java.nio.file.Path.of("build/classes",
                                "juranometria/sky/Ecliptic.class")),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        for (String forbidden : List.of("java/time", "Locale",
                "TimeZone", "Calendar", "java/text",
                "currentTimeMillis", "nanoTime", "Random")) {
            assertTrue(!pool.contains(forbidden),
                    "the ecliptic refers to no " + forbidden
                            + ", so it cannot vary with one");
        }
        assertTrue(pool.contains("juranometria/chart/SkyPosition"),
                "and the scan is looking at the right class, which is"
                        + " what makes the absences above an answer");
    }

    @Test
    void thereIsNoWayToAskForTheEclipticOfDate() {
        // The gate's central refusal, executed rather than promised:
        // no method here takes an epoch, an instant or a date, so the
        // rejected shortcut is not reachable from this type.
        List<String> dated = java.util.Arrays.stream(
                        Ecliptic.class.getDeclaredMethods())
                .filter(method -> java.lang.reflect.Modifier
                        .isPublic(method.getModifiers()))
                .filter(method -> java.util.Arrays
                        .stream(method.getParameterTypes())
                        .anyMatch(type ->
                                type.getName().contains("time")
                                        || type.getName().contains("Date")
                                        || type.getName()
                                                .contains("Instant")))
                .map(java.lang.reflect.Method::getName).sorted().toList();
        assertEquals(List.of(), dated,
                "no public method takes a date, an instant or an"
                        + " epoch: the ecliptic of date is not"
                        + " reachable from here");
        assertNotEquals(0, Ecliptic.landmarks().size(),
                "and the type is not empty, so that is an answer");
    }
}
