package juranometria.meridian;

import java.util.List;
import java.util.Optional;

/**
 * The contextual altitude readout's rule (#563, studied in #675 stage 5):
 * geometry only, no ink. When the meridian crosses the page with none of
 * its ruled ticks on it, one readout may stand on the visible line; this
 * says where, and what altitude it states, from samples of the visible
 * line the study - and later the chart - hands it.
 *
 * <p>Two candidates are offered to the ruling, and the one not chosen is
 * removed with it:
 * <ul>
 *   <li>{@link #nearestTheCentre}: the visible point nearest the page
 *       centre, stating its own altitude rounded;</li>
 *   <li>{@link #wholeDegreeNearestTheCentre}: the whole degree of
 *       altitude on the visible line nearest the page centre - an honest
 *       tick at an exact degree, outside the 10° sequence.</li>
 * </ul>
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
     * The whole degree of altitude on the visible line nearest the page
     * centre, interpolated between the samples that bracket it; empty when
     * no whole degree above the horizon and below the zenith is visible.
     */
    public static Optional<Sample> wholeDegreeNearestTheCentre(List<Sample> visible,
                                                               double cx, double cy) {
        Sample best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 1; i < visible.size(); i++) {
            Sample a = visible.get(i - 1);
            Sample b = visible.get(i);
            double low = Math.min(a.altitudeDegrees(), b.altitudeDegrees());
            double high = Math.max(a.altitudeDegrees(), b.altitudeDegrees());
            for (int degree = (int) Math.ceil(low - 1e-9); degree <= Math.floor(high + 1e-9); degree++) {
                if (degree <= 0 || degree >= 90) {
                    continue;
                }
                double t = high == low ? 0.0 : (degree - a.altitudeDegrees())
                        / (b.altitudeDegrees() - a.altitudeDegrees());
                Sample at = new Sample(degree, a.x() + t * (b.x() - a.x()),
                        a.y() + t * (b.y() - a.y()));
                double d = Math.hypot(at.x() - cx, at.y() - cy);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = at;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** An altitude rounded to the nearest whole degree, as a readout would state it. */
    public static double roundedToTheDegree(double altitudeDegrees) {
        return Math.rint(altitudeDegrees);
    }

    /** An altitude rounded to the nearest half degree. */
    public static double roundedToTheHalfDegree(double altitudeDegrees) {
        return Math.rint(altitudeDegrees * 2.0) / 2.0;
    }
}
