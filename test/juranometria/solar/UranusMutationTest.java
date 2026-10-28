package juranometria.solar;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.tool.UranusComparison;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each step the contract names, done the wrong way, must fail its ruled
 * target on the committed Horizons rows of the exact interval (issue
 * #634), so the targets are known to bite - Mars's (#602) for Uranus.
 */
class UranusMutationTest {

    private static final UranusService SERVICE = UranusService.load();

    private interface Quantity {
        double of(UranusService.UranusObservation o, UranusComparison.Table t, String[] f);
    }

    /** The worst of a quantity over the exact interval's Oslo rows, beyond 1° of the Sun. */
    private static double worst(UranusService.Rival rival, Quantity q) throws IOException {
        double worst = 0.0;
        for (String stem : List.of("named-uranus-oslo", "matrix-7d-uranus-oslo")) {
            UranusComparison.Table t = UranusComparison.table(stem);
            for (String[] f : t.rows()) {
                Instant when = UranusComparison.instant(f[0]);
                int year = when.atOffset(java.time.ZoneOffset.UTC).getYear();
                if (year < 1972 || !when.isBefore(Instant.parse("2027-06-28T00:00:00Z"))) {
                    continue;
                }
                UranusService.UranusObservation o =
                        SERVICE.observeRival(UranusComparison.observer("oslo", when), rival);
                if (o.elongationDegrees() > 1.0) {
                    worst = Math.max(worst, Math.abs(q.of(o, t, f)));
                }
            }
        }
        return worst;
    }

    private static final Quantity APPARENT = (o, t, f) -> UranusComparison.separation(o.apparentOfDate(),
            UranusComparison.d(f, t, "R.A.__(a-app)"), UranusComparison.d(f, t, "DEC___(a-app)"));
    private static final Quantity PHASE = (o, t, f) ->
            (o.phaseAngleDegrees() - UranusComparison.d(f, t, "phi")) * 3600.0;

    @Test
    void theRightComputationMeetsItsTargetsOnTheseRows() throws IOException {
        assertTrue(worst(UranusService.Rival.NONE, PHASE) <= 0.5);
        assertTrue(worst(UranusService.Rival.NONE, APPARENT) <= 0.2);
    }

    @Test
    void leavingOutAberrationFailsTheApparentPlace() throws IOException {
        assertTrue(worst(UranusService.Rival.NO_ABERRATION, APPARENT) > 10.0);
    }

    @Test
    void theAberratedDownLegFailsThePhaseAgainstPhi() throws IOException {
        assertTrue(worst(UranusService.Rival.ABERRATED_DOWN_LEG_PHASE, PHASE) > 0.5, "U-5: phi, not S-T-O");
    }

    @Test
    void anUnaberratedSunFailsThePhase() throws IOException {
        assertTrue(worst(UranusService.Rival.UNABERRATED_SUN, PHASE) > 0.5);
    }

    @Test
    void thePlanetodeticTiltIsNotThePlanetocentric() throws IOException {
        double worst = worst(UranusService.Rival.PLANETODETIC_TILT, (o, t, f) -> {
            double centric = SERVICE.observe(UranusComparison.observer("oslo", o.instant())).tiltDegrees();
            return (o.tiltDegrees() - centric) * 3600.0;
        });
        assertTrue(worst > 600.0, "U-4: Horizons' planetodetic value is never the tilt: " + worst);
    }

    @Test
    void leavingOutTheSunsDeflectionFailsTheApparentPlace() throws IOException {
        double with = worst(UranusService.Rival.NONE, APPARENT);
        double without = worst(UranusService.Rival.NO_DEFLECTION, APPARENT);
        assertTrue(without > 0.2 && without > 2.0 * with, String.format(java.util.Locale.ROOT,
                "with the deflection %.4f\", without it %.4f\"", with, without));
    }

    @Test
    void j2000OffsetsFailTheMoons() throws IOException {
        double worst = 0.0;
        UranusComparison.Table t = UranusComparison.table("matrix-7d-oberon-oslo");
        for (String[] f : t.rows()) {
            Instant when = UranusComparison.instant(f[0]);
            if (!SERVICE.moonsAnswerAt(when) || !when.isBefore(Instant.parse("2027-06-28T00:00:00Z"))) {
                continue;
            }
            UranusService.Configuration c = SERVICE.configurationRival(
                    UranusComparison.observer("oslo", when), UranusService.Rival.J2000_OFFSETS);
            if (c.uranus().elongationDegrees() > 1.0) {
                worst = Math.max(worst, Math.abs(c.moon(UranusService.Moon.OBERON).xArcseconds()
                        - UranusComparison.d(f, t, "X_(sat-prim)")));
            }
        }
        assertTrue(worst > 0.05, "the moons' offsets are of date: " + worst);
    }
}
