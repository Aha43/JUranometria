package juranometria.solar;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.tool.NeptuneComparison;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each step the contract names, done the wrong way, must fail its ruled
 * target on the committed Horizons rows of the exact interval (issue
 * #655), so the targets are known to bite - Uranus's (#634) for Neptune.
 */
class NeptuneMutationTest {

    private static final NeptuneService SERVICE = NeptuneService.load();

    private interface Quantity {
        double of(NeptuneService.NeptuneObservation o, NeptuneComparison.Table t, String[] f);
    }

    /** The worst of a quantity over the exact interval's Oslo rows, beyond 1° of the Sun. */
    private static double worst(NeptuneService.Rival rival, Quantity q) throws IOException {
        double worst = 0.0;
        for (String stem : List.of("named-neptune-oslo", "matrix-7d-neptune-oslo")) {
            NeptuneComparison.Table t = NeptuneComparison.table(stem);
            for (String[] f : t.rows()) {
                Instant when = NeptuneComparison.instant(f[0]);
                int year = when.atOffset(java.time.ZoneOffset.UTC).getYear();
                if (year < 1972 || !when.isBefore(Instant.parse("2027-06-28T00:00:00Z"))) {
                    continue;
                }
                NeptuneService.NeptuneObservation o =
                        SERVICE.observeRival(NeptuneComparison.observer("oslo", when), rival);
                if (o.elongationDegrees() > 1.0) {
                    worst = Math.max(worst, Math.abs(q.of(o, t, f)));
                }
            }
        }
        return worst;
    }

    private static final Quantity APPARENT = (o, t, f) -> NeptuneComparison.separation(o.apparentOfDate(),
            NeptuneComparison.d(f, t, "R.A.__(a-app)"), NeptuneComparison.d(f, t, "DEC___(a-app)"));
    private static final Quantity PHASE = (o, t, f) ->
            (o.phaseAngleDegrees() - NeptuneComparison.d(f, t, "phi")) * 3600.0;

    @Test
    void theRightComputationMeetsItsTargetsOnTheseRows() throws IOException {
        assertTrue(worst(NeptuneService.Rival.NONE, PHASE) <= 0.5);
        assertTrue(worst(NeptuneService.Rival.NONE, APPARENT) <= 0.2);
    }

    @Test
    void leavingOutAberrationFailsTheApparentPlace() throws IOException {
        assertTrue(worst(NeptuneService.Rival.NO_ABERRATION, APPARENT) > 10.0);
    }

    @Test
    void theAberratedDownLegFailsThePhaseAgainstPhi() throws IOException {
        assertTrue(worst(NeptuneService.Rival.ABERRATED_DOWN_LEG_PHASE, PHASE) > 0.5, "N-5: phi, not S-T-O");
    }

    @Test
    void anUnaberratedSunFailsThePhase() throws IOException {
        assertTrue(worst(NeptuneService.Rival.UNABERRATED_SUN, PHASE) > 0.5);
    }

    @Test
    void thePlanetodeticTiltIsNotThePlanetocentric() throws IOException {
        double worst = worst(NeptuneService.Rival.PLANETODETIC_TILT, (o, t, f) -> {
            double centric = SERVICE.observe(NeptuneComparison.observer("oslo", o.instant())).tiltDegrees();
            return (o.tiltDegrees() - centric) * 3600.0;
        });
        assertTrue(worst > 600.0, "N-4: Horizons' planetodetic value is never the tilt: " + worst);
    }

    @Test
    void leavingOutThePolesPeriodicTermFailsThePoleAngle() throws IOException {
        Quantity poleAngle = (o, t, f) ->
                NeptuneComparison.wrap(o.poleAngleDegrees() - NeptuneComparison.d(f, t, "NP.ang")) * 3600.0;
        assertTrue(worst(NeptuneService.Rival.NONE, poleAngle) <= 0.5, "the ruled pole meets 0.5″");
        double without = worst(NeptuneService.Rival.NO_PERIODIC_POLE, poleAngle);
        assertTrue(without > 0.5, "N-4: the periodic term is required: " + without);
    }

    @Test
    void leavingOutTheSunsDeflectionFailsTheApparentPlace() throws IOException {
        double with = worst(NeptuneService.Rival.NONE, APPARENT);
        double without = worst(NeptuneService.Rival.NO_DEFLECTION, APPARENT);
        assertTrue(without > 0.2 && without > 2.0 * with, String.format(java.util.Locale.ROOT,
                "with the deflection %.4f\", without it %.4f\"", with, without));
    }

    @Test
    void j2000OffsetsFailTheMoons() throws IOException {
        double worst = 0.0;
        NeptuneComparison.Table t = NeptuneComparison.table("matrix-7d-nereid-oslo");
        for (String[] f : t.rows()) {
            Instant when = NeptuneComparison.instant(f[0]);
            if (!SERVICE.moonsAnswerAt(when) || !when.isBefore(Instant.parse("2027-06-28T00:00:00Z"))) {
                continue;
            }
            NeptuneService.Configuration c = SERVICE.configurationRival(
                    NeptuneComparison.observer("oslo", when), NeptuneService.Rival.J2000_OFFSETS);
            if (c.neptune().elongationDegrees() > 1.0) {
                worst = Math.max(worst, Math.abs(c.moon(NeptuneService.Moon.NEREID).xArcseconds()
                        - NeptuneComparison.d(f, t, "X_(sat-prim)")));
            }
        }
        assertTrue(worst > 0.05, "the moons' offsets are of date: " + worst);
    }
}
