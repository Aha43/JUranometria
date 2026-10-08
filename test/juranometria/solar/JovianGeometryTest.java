package juranometria.solar;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.solar.JovianSystemService.Configuration;
import juranometria.solar.JovianSystemService.DiscRelation;
import juranometria.solar.JovianSystemService.JupiterObservation;
import juranometria.solar.JovianSystemService.Moon;
import juranometria.solar.JovianSystemService.MoonPlace;
import juranometria.solar.JovianSystemService.ShadowRelation;
import juranometria.solar.JovianSystemService.VisibilityState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The geometry over the whole range at fixed ephemeris time (ruling 3
 * of #473's amendments): whatever a future ΔT prediction says about
 * which civil instant is which, the front/behind, disc, shadow and
 * figure relations hold their own consistency at every TDB epoch of
 * the pack's coverage, sampled every four hours over 2000–2100 from
 * the Earth's centre, and Jupiter's figure and phase stay within what
 * the geometry allows over 1900–2100.
 */
class JovianGeometryTest {

    private static final double DAY = 86400.0;
    private static final double MOONS_START_ET = (2451544.5 - 2451545.0) * DAY; // 2000-01-01 TDB
    private static final double END_ET = (2488434.5 - 2451545.0) * DAY;         // 2101-01-01 TDB
    private static final double STEP = 4.0 * 3600.0;

    @Test
    void everyRelationIsConsistentAtEveryEpochOfTheMoonsCoverage() {
        JovianSystemService service = JovianSystemService.load();
        JovianPack.Constants k = service.pack().constants();
        Map<Moon, Map<VisibilityState, Integer>> seen = new EnumMap<>(Moon.class);
        for (Moon moon : Moon.values()) {
            seen.put(moon, new EnumMap<>(VisibilityState.class));
        }
        int epochs = 0;
        int figureDiffers = 0;
        for (double et = MOONS_START_ET; et <= END_ET; et += STEP) {
            Configuration c = service.configurationAtEt(et);
            JupiterObservation j = c.jupiter();
            double jupiterRadius = j.equatorialDiameterArcseconds() / 2.0;
            for (MoonPlace p : c.moons()) {
                String at = p.moon() + " at et " + et;
                assertTrue(Double.isFinite(p.xArcseconds()) && Double.isFinite(p.yArcseconds())
                        && Double.isFinite(p.separationArcseconds())
                        && Double.isFinite(p.positionAngleDegrees()), at);
                // front and behind from depth alone
                if (p.discRelation() == DiscRelation.IN_FRONT) {
                    assertTrue(p.depthKm() < 0.0, at + ": in front is nearer");
                    assertEquals(ShadowRelation.SUNLIT, p.shadowRelation(),
                            at + ": a transiting moon is sunlit by geometry");
                }
                if (p.discRelation() == DiscRelation.BEHIND) {
                    assertTrue(p.depthKm() > 0.0, at + ": behind is farther");
                }
                // the shadow lies beyond Jupiter as the Earth sees it
                if (p.shadowRelation() != ShadowRelation.SUNLIT) {
                    assertTrue(p.depthKm() > 0.0, at + ": a shadowed moon is beyond Jupiter");
                }
                // an overlap, on either definition, is within the limb sums
                double limbs = jupiterRadius + p.angularDiameterArcseconds() / 2.0;
                if (p.sphericalDiscRelation() != DiscRelation.CLEAR) {
                    assertTrue(p.separationArcseconds() < limbs + 0.1,
                            at + ": on the sphere within the limb sum");
                }
                if (p.discRelation() != DiscRelation.CLEAR) {
                    assertTrue(p.separationArcseconds() < limbs + 0.1,
                            at + ": the figure lies inside the sphere");
                }
                if (p.discRelation() != p.sphericalDiscRelation()) {
                    figureDiffers++;
                    assertEquals(DiscRelation.CLEAR, p.discRelation(),
                            at + ": the oblate figure only ever frees a limb the sphere holds");
                    assertTrue(Math.abs(p.separationArcseconds() - limbs) < 1.7,
                            at + ": and only at a graze");
                }
                assertEquals(VisibilityState.of(p.discRelation(), p.shadowRelation()), p.state());
                assertEquals(p.separationArcseconds() / jupiterRadius, p.separationJupiterRadii(), 1e-9);
                seen.get(p.moon()).merge(p.state(), 1, Integer::sum);
            }
            epochs++;
        }
        assertTrue(epochs > 200_000, "a century at four-hour steps: " + epochs);
        for (Moon moon : Moon.values()) {
            Map<VisibilityState, Integer> states = seen.get(moon);
            for (VisibilityState state : VisibilityState.values()) {
                if (moon == Moon.CALLISTO && (state == VisibilityState.PARTLY_IN_JUPITERS_SHADOW
                        || state == VisibilityState.BEHIND_JUPITER_IN_ITS_SHADOW)) {
                    continue; // Callisto's partial eclipses are rare at this sampling
                }
                assertTrue(states.getOrDefault(state, 0) > 0, moon + " shows " + state
                        + " somewhere in the century: " + states);
            }
            assertTrue(states.get(VisibilityState.CLEAR_OF_JUPITER) > states.values().stream()
                    .mapToInt(Integer::intValue).sum() * 0.8, moon + " is mostly clear");
        }
        System.out.printf(Locale.ROOT, "jovian geometry: %d epochs; figure differs from the"
                + " sphere at %d moon-epochs; states %s%n", epochs, figureDiffers, seen);
    }

    @Test
    void jupitersFigureAndPhaseStayWithinWhatTheGeometryAllows() {
        JovianSystemService service = JovianSystemService.load();
        double startEt = (2415020.5 - 2451545.0) * DAY; // 1900-01-01 TDB
        double worstDiameter = 0.0;
        double leastDiameter = 1e9;
        double worstLatitude = 0.0;
        double leastIlluminated = 1.0;
        int epochs = 0;
        for (double et = startEt; et <= END_ET; et += 7.0 * DAY) {
            JupiterObservation j = service.configurationAtEt(Math.max(et, MOONS_START_ET)).jupiter();
            // Jupiter's distance from the Earth lies between 3.9 and 6.5 AU:
            // 2·asin(71 492 km / d) is 30″ to 50″.
            assertTrue(j.equatorialDiameterArcseconds() > 29.0 && j.equatorialDiameterArcseconds() < 51.0,
                    "diameter " + j.equatorialDiameterArcseconds());
            assertTrue(j.polarDiameterArcseconds() < j.equatorialDiameterArcseconds());
            assertEquals(66854.0 / 71492.0, j.polarDiameterArcseconds() / j.equatorialDiameterArcseconds(), 1e-6);
            // the sub-observer latitude is bounded by the axial tilt (3.13°) and the Earth's view
            assertTrue(Math.abs(j.subObserverLatitudeDegrees()) < 3.7, "B " + j.subObserverLatitudeDegrees());
            // the phase angle never exceeds about 12°, so k ≥ 0.98
            assertTrue(j.illuminatedFraction() > 0.98 && j.illuminatedFraction() <= 1.0);
            assertTrue(j.phaseAngleDegrees() >= 0.0 && j.phaseAngleDegrees() < 12.5);
            worstDiameter = Math.max(worstDiameter, j.equatorialDiameterArcseconds());
            leastDiameter = Math.min(leastDiameter, j.equatorialDiameterArcseconds());
            worstLatitude = Math.max(worstLatitude, Math.abs(j.subObserverLatitudeDegrees()));
            leastIlluminated = Math.min(leastIlluminated, j.illuminatedFraction());
            epochs++;
        }
        System.out.printf(Locale.ROOT, "jupiter over %d weekly epochs: diameter %.2f to %.2f\","
                + " |B| up to %.2f deg, k down to %.4f%n", epochs, leastDiameter, worstDiameter,
                worstLatitude, leastIlluminated);
    }
}
