package juranometria.solar;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.tool.MarsComparison;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each step the contract names, done the wrong way, must fail its ruled
 * target on the committed Horizons rows of the exact interval (issue
 * #602): so the targets are known to bite. Unlike Mercury's, Mars's
 * deflection bites: without it the apparent place misses by more than
 * the 0.2″ any proposed amendment would allow.
 */
class MarsMutationTest {

    private static final MarsService SERVICE = MarsService.load();

    private interface Quantity {
        double of(MarsService.MarsObservation o, MarsComparison.Table t, String[] f);
    }

    /** The worst of a quantity over the exact interval's Oslo rows, for a rival. */
    private static double worst(MarsService.Rival rival, Quantity q) throws IOException {
        double worst = 0.0;
        for (String stem : List.of("named-mars-oslo", "matrix-7d-mars-oslo")) {
            MarsComparison.Table t = MarsComparison.table(stem);
            for (String[] f : t.rows()) {
                Instant when = MarsComparison.instant(f[0]);
                int year = when.atOffset(java.time.ZoneOffset.UTC).getYear();
                if (year < 1972 || !when.isBefore(Instant.parse("2027-06-28T00:00:00Z"))) {
                    continue;
                }
                MarsService.MarsObservation o = SERVICE.observeRival(MarsComparison.observer("oslo", when), rival);
                if (o.elongationDegrees() > 1.0) {
                    worst = Math.max(worst, Math.abs(q.of(o, t, f)));
                }
            }
        }
        return worst;
    }

    private static final Quantity APPARENT = (o, t, f) -> MarsComparison.separation(o.apparentOfDate(),
            MarsComparison.d(f, t, "R.A.__(a-app)"), MarsComparison.d(f, t, "DEC___(a-app)"));
    private static final Quantity PHASE = (o, t, f) ->
            (o.phaseAngleDegrees() - MarsComparison.d(f, t, "phi")) * 3600.0;
    private static final Quantity POLE = (o, t, f) ->
            MarsComparison.wrap(o.poleAngleDegrees() - MarsComparison.d(f, t, "NP.ang")) * 3600.0;

    @Test
    void theRightComputationMeetsItsTargetsOnTheseRows() throws IOException {
        assertTrue(worst(MarsService.Rival.NONE, PHASE) <= 0.5);
        assertTrue(worst(MarsService.Rival.NONE, POLE) <= 0.5);
    }

    @Test
    void leavingOutAberrationFailsTheApparentPlace() throws IOException {
        assertTrue(worst(MarsService.Rival.NO_ABERRATION, APPARENT) > 10.0);
    }

    @Test
    void theSecularPoleFailsThePolesPositionAngle() throws IOException {
        assertTrue(worst(MarsService.Rival.SECULAR_POLE, POLE) > 100.0, "M-3: the periodic terms are required");
    }

    @Test
    void theAberratedDownLegFailsThePhaseAgainstPhi() throws IOException {
        assertTrue(worst(MarsService.Rival.ABERRATED_DOWN_LEG_PHASE, PHASE) > 0.5, "M-4: phi, not S-T-O");
    }

    @Test
    void anUnaberratedSunFailsThePhase() throws IOException {
        assertTrue(worst(MarsService.Rival.UNABERRATED_SUN, PHASE) > 0.5);
    }

    @Test
    void thePlanetodeticTiltIsNotThePlanetocentric() throws IOException {
        double worst = worst(MarsService.Rival.PLANETODETIC_TILT, (o, t, f) -> {
            double centric = SERVICE.observe(MarsComparison.observer("oslo", o.instant())).tiltDegrees();
            return (o.tiltDegrees() - centric) * 3600.0;
        });
        assertTrue(worst > 600.0, "M-3: Horizons' planetodetic value is never the tilt: " + worst);
    }

    @Test
    void leavingOutTheSunsDeflectionFailsTheApparentPlace() throws IOException {
        double with = worst(MarsService.Rival.NONE, APPARENT);
        double without = worst(MarsService.Rival.NO_DEFLECTION, APPARENT);
        assertTrue(without > 0.3 && without > 2.0 * with, String.format(java.util.Locale.ROOT,
                "M-5: with the deflection %.4f\", without it %.4f\"", with, without));
    }

    @Test
    void j2000OffsetsFailTheMoons() throws IOException {
        double worst = 0.0;
        MarsComparison.Table t = MarsComparison.table("matrix-7d-deimos-oslo");
        for (String[] f : t.rows()) {
            Instant when = MarsComparison.instant(f[0]);
            if (!SERVICE.moonsAnswerAt(when) || !when.isBefore(Instant.parse("2027-06-28T00:00:00Z"))) {
                continue;
            }
            MarsService.Configuration c = SERVICE.configurationRival(MarsComparison.observer("oslo", when),
                    MarsService.Rival.J2000_OFFSETS);
            if (c.mars().elongationDegrees() > 1.0) {
                worst = Math.max(worst, Math.abs(c.moon(MarsService.Moon.DEIMOS).xArcseconds()
                        - MarsComparison.d(f, t, "X_(sat-prim)")));
            }
        }
        assertTrue(worst > 0.05, "the moons' offsets are of date: " + worst);
    }
}
