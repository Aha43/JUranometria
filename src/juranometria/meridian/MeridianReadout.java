package juranometria.meridian;

import java.util.List;
import java.util.Optional;

/**
 * The contextual altitude readout's rule (#563, studied and ruled in
 * #675 stage 5): geometry only, no ink. When the meridian crosses the
 * page with none of its ruled ticks on it, one readout stands on the
 * visible line at the point nearest the page centre and states that
 * point's altitude rounded to the whole degree (ruling R-2, R-3: "A to
 * the degree"). The whole-degree candidate the study also measured was
 * removed with the ruling.
 */
public final class MeridianReadout {

    private MeridianReadout() {
    }

    /** A point of the meridian on the page: its altitude and its pixel. */
    public record Sample(double altitudeDegrees, double x, double y) {
    }

    /** Whether any of the ruled ticks' altitudes lies among the visible samples' altitudes. */
    public static boolean aRuledTickIsVisible(List<Sample> visible, MeridianModule.Ticks ticks) {
        if (visible.isEmpty()) {
            return false;
        }
        double low = visible.stream().mapToDouble(Sample::altitudeDegrees).min().orElseThrow();
        double high = visible.stream().mapToDouble(Sample::altitudeDegrees).max().orElseThrow();
        for (double altitude : ticks.altitudes()) {
            if (altitude >= low - 1e-9 && altitude <= high + 1e-9) {
                return true;
            }
        }
        return false;
    }

    /** The visible sample nearest the page centre, if any is visible and above the horizon. */
    public static Optional<Sample> nearestTheCentre(List<Sample> visible, double cx, double cy) {
        Sample best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Sample sample : visible) {
            if (sample.altitudeDegrees() <= 0.0) {
                continue;
            }
            double d = Math.hypot(sample.x() - cx, sample.y() - cy);
            if (d < bestDistance) {
                bestDistance = d;
                best = sample;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * An altitude rounded to the nearest whole degree, as the readout
     * states it; a half rounds up, as a reader rounds (not to even, as
     * {@code Math.rint} would).
     */
    public static double roundedToTheDegree(double altitudeDegrees) {
        return Math.floor(altitudeDegrees + 0.5);
    }

    /**
     * Whether a rounded altitude may be stated (R-8): above the horizon
     * and below the zenith - 0° is the horizon's and 90° the zenith's,
     * and neither is ever a readout, as neither is ever a tick.
     */
    public static boolean statable(double roundedDegrees) {
        return roundedDegrees > 0.0 && roundedDegrees < 90.0;
    }
}
