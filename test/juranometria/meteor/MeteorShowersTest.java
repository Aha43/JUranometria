package juranometria.meteor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.catalog.PackIntegrityException;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shower service over an invented pack and an invented Sun (#716):
 * activity as an interval of solar longitude (R-6), the radiant by the
 * ruled evaluation with its footing (R-9), the nominal maximum as the
 * instant the Sun reaches the listed longitude (R-7, R-17), and the
 * pack's own refusals. No real shower's value appears here.
 */
class MeteorShowersTest {

    private static final MeteorShowers SERVICE = SyntheticShowers.service();
    private static final MeteorShower AAA = SERVICE.shower("AAA").orElseThrow();
    private static final MeteorShower BBB = SERVICE.shower("BBB").orElseThrow();
    private static final MeteorShower CCC = SERVICE.shower("CCC").orElseThrow();

    @Test
    void activityIsAnIntervalOfSolarLongitudeClosedAtItsStartAndOpenAtItsEnd() {
        assertFalse(MeteorShowers.active(AAA, 89.999));
        assertTrue(MeteorShowers.active(AAA, 90.0), "the first date's 00:00 is inside");
        assertTrue(MeteorShowers.active(AAA, 109.999));
        assertFalse(MeteorShowers.active(AAA, 110.0), "the last date's 24:00 is outside");
    }

    @Test
    void aShowerThatSpansLongitudeZeroNeedsNoSpecialCase() {
        assertTrue(MeteorShowers.active(BBB, 355.0));
        assertTrue(MeteorShowers.active(BBB, 0.0));
        assertTrue(MeteorShowers.active(BBB, 9.9));
        assertFalse(MeteorShowers.active(BBB, 10.0));
        assertFalse(MeteorShowers.active(BBB, 349.9));
        assertFalse(MeteorShowers.active(BBB, 180.0));
    }

    @Test
    void theShowersActiveAtAnInstantAreAskedOfTheSunNotOfADate() {
        assertEquals(List.of("AAA"), SERVICE.activeAt(SyntheticShowers.at(100.0)).stream()
                .map(MeteorShower::code).toList());
        // The same solar longitude one turn later, whatever the calendar says then.
        assertEquals(List.of("AAA"), SERVICE.activeAt(SyntheticShowers.at(460.0)).stream()
                .map(MeteorShower::code).toList());
        assertEquals(List.of(), SERVICE.activeAt(SyntheticShowers.at(150.0)));
    }

    @Test
    void betweenTwoRowsTheRadiantLiesOnTheirGreatCircleInProportion() {
        MeteorShowers.Radiant half = MeteorShowers.radiantAt(AAA, 97.5);
        assertEquals(MeteorShowers.Footing.BETWEEN_ROWS, half.footing());
        SkyPosition a = AAA.rows().get(0).at();
        SkyPosition b = AAA.rows().get(1).at();
        assertEquals(a.separationDegrees(b) / 2.0, half.at().separationDegrees(a), 1e-9);
        assertEquals(a.separationDegrees(b) / 2.0, half.at().separationDegrees(b), 1e-9,
                "midway, and on the arc: the two halves sum to the whole");
        assertEquals(0.0, MeteorShowers.radiantAt(AAA, 100.0).at().separationDegrees(b), 1e-9,
                "on a printed row it is the printed row");
    }

    @Test
    void beyondThePrintedRowsTheNearestPairIsCarriedOnAndSaysSo() {
        MeteorShowers.Radiant before = MeteorShowers.radiantAt(AAA, 92.5);
        assertEquals(MeteorShowers.Footing.BEYOND_ROWS, before.footing());
        SkyPosition first = AAA.rows().get(0).at();
        SkyPosition second = AAA.rows().get(1).at();
        assertEquals(first.separationDegrees(second) / 2.0, before.at().separationDegrees(first),
                1e-9, "half a row's step before the first row");
        assertEquals(first.separationDegrees(second) * 1.5, before.at().separationDegrees(second),
                1e-9, "and on the far side of it from the second");
        assertEquals(MeteorShowers.Footing.BEYOND_ROWS,
                MeteorShowers.radiantAt(AAA, 108.0).footing());
    }

    @Test
    void aShowerWithFewerThanTwoRowsIsHeldWhereItIsPrinted() {
        MeteorShowers.Radiant one = MeteorShowers.radiantAt(CCC, 189.0);
        assertEquals(MeteorShowers.Footing.FIXED, one.footing());
        assertEquals(CCC.rows().get(0).at(), one.at(), "its one row");
        MeteorShowers.Radiant none = MeteorShowers.radiantAt(BBB, 5.0);
        assertEquals(MeteorShowers.Footing.FIXED, none.footing());
        assertEquals(BBB.radiantAtMaximum(), none.at(), "no row: the working list's radiant");
    }

    @Test
    void theNominalMaximumIsTheInstantTheSunReachesTheListedLongitude() {
        Instant expected = SyntheticShowers.at(100.5);
        assertEquals(expected, SERVICE.nominalMaximumAtOrAfter(AAA, SyntheticShowers.at(20.0))
                .orElseThrow());
        assertEquals(expected, SERVICE.nominalMaximumAtOrAfter(AAA, expected).orElseThrow(),
                "at the maximum, the next maximum is this one");
        assertEquals(expected.plusSeconds(360L * 86400L),
                SERVICE.nominalMaximumAtOrAfter(AAA, expected.plusSeconds(1)).orElseThrow(),
                "a second later it is the next turn's");
        assertEquals(SyntheticShowers.at(360.25),
                SERVICE.nominalMaximumAtOrAfter(BBB, SyntheticShowers.at(355.0)).orElseThrow(),
                "across longitude zero");
    }

    @Test
    void aSunThatRefusesTheInstantLeavesNoMaximumRatherThanAnInventedOne() {
        MeteorShowers refusing = new MeteorShowers(SyntheticShowers.pack(), instant -> {
            throw new IllegalArgumentException("outside the years");
        });
        assertTrue(refusing.nominalMaximumAtOrAfter(AAA, SyntheticShowers.EPOCH).isEmpty());
    }

    @Test
    void theMaximumIsKnownNoFinerThanItsListingPrintsIt() {
        assertEquals(MeteorShower.MaximumPrecision.HOURS, AAA.maximumPrecision());
        assertEquals(MeteorShower.MaximumPrecision.TEN_MINUTES, BBB.maximumPrecision());
        assertEquals(MeteorShower.MaximumPrecision.DATE, CCC.maximumPrecision());
        assertEquals("100.5", AAA.maximumLongitudeText(), "kept as printed");
        assertEquals("20+", BBB.zhr(), "the rate keeps its plus");
        assertEquals("Var", CCC.zhr());
    }

    @Test
    void anIdentityIsTheStemAndTheCode() {
        assertEquals("meteor-shower.AAA", AAA.identity());
        assertEquals("AAA", MeteorShower.codeOf("meteor-shower.AAA"));
        assertEquals(null, MeteorShower.codeOf("moon"));
    }

    @Test
    void thePackNamesItsEditionAndRefusesWhatDoesNotMatchItsManifest() {
        MeteorShowerPack pack = SyntheticShowers.pack();
        assertEquals("Invented Calendar 2030", pack.edition());
        assertEquals("never", pack.correctAsOf());
        assertEquals("Source: nobody", pack.credit());
        assertEquals(List.of("AAA", "BBB", "CCC"), pack.showers().stream()
                .map(MeteorShower::code).toList(), "the pack's own order");

        Map<String, String> altered = SyntheticShowers.files();
        altered.put(MeteorShowerPack.SHOWERS, SyntheticShowers.SHOWERS.replace("100.5", "100.6"));
        PackIntegrityException changed = assertThrows(PackIntegrityException.class,
                () -> MeteorShowerPack.load(SyntheticShowers.opener(altered)));
        assertTrue(changed.getMessage().contains("fails its checksum"), changed.getMessage());

        Map<String, String> missing = SyntheticShowers.files();
        missing.remove(MeteorShowerPack.RADIANTS);
        assertThrows(PackIntegrityException.class,
                () -> MeteorShowerPack.load(SyntheticShowers.opener(missing)));

        Map<String, String> unnamed = SyntheticShowers.files();
        unnamed.put("manifest.properties", unnamed.get("manifest.properties")
                .replace("edition=Invented Calendar 2030\n", ""));
        PackIntegrityException noEdition = assertThrows(PackIntegrityException.class,
                () -> MeteorShowerPack.load(SyntheticShowers.opener(unnamed)));
        assertTrue(noEdition.getMessage().contains("edition"), "a pack names its edition (R-4)");

        Map<String, String> miscounted = SyntheticShowers.files();
        miscounted.put("manifest.properties", miscounted.get("manifest.properties")
                .replace("showers=3", "showers=4"));
        assertThrows(PackIntegrityException.class,
                () -> MeteorShowerPack.load(SyntheticShowers.opener(miscounted)));
    }
}
