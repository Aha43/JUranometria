package juranometria.meteor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

import juranometria.chart.SkyPosition;
import juranometria.sky.SkyFrame;

/**
 * What the atlas can say about a meteor shower at an instant (#716):
 * whether it is active, where its radiant is, and when its nominal
 * maximum falls - every answer asked of the Sun's J2000 ecliptic
 * longitude at that instant, never of a calendar date (R-5).
 *
 * <p>Pure: the pack and a function giving the Sun's longitude. No
 * observer, no Swing, no clock.
 */
public final class MeteorShowers {

    /** The Sun's mean motion in longitude, degrees a day; a first step only. */
    private static final double MEAN_MOTION = 0.985647;

    /** How an evaluated radiant was reached, so no value hides its footing (R-9). */
    public enum Footing {
        /** Between two printed rows of the radiant table. */
        BETWEEN_ROWS,
        /** Beyond the first or last printed row, carried on along the nearest pair. */
        BEYOND_ROWS,
        /** Fewer than two rows: held at the printed position. */
        FIXED
    }

    /** A radiant evaluated at a solar longitude. */
    public record Radiant(SkyPosition at, Footing footing) {
    }

    private final MeteorShowerPack pack;
    private final ToDoubleFunction<Instant> solarLongitude;

    public MeteorShowers(MeteorShowerPack pack, ToDoubleFunction<Instant> solarLongitude) {
        if (pack == null || solarLongitude == null) {
            throw new IllegalArgumentException("a pack and the Sun's longitude");
        }
        this.pack = pack;
        this.solarLongitude = solarLongitude;
    }

    /** The pack the answers are from. */
    public MeteorShowerPack pack() {
        return pack;
    }

    /** The showers, in the order of the year. */
    public List<MeteorShower> showers() {
        return pack.showers();
    }

    /** The shower with an IAU code, if the pack carries it. */
    public Optional<MeteorShower> shower(String code) {
        return pack.showers().stream().filter(s -> s.code().equals(code)).findFirst();
    }

    /** The Sun's geocentric J2000 ecliptic longitude at an instant, degrees. */
    public double solarLongitude(Instant instant) {
        return solarLongitude.applyAsDouble(instant);
    }

    /** Whether a solar longitude lies in a shower's activity interval (R-6). */
    public static boolean active(MeteorShower shower, double solarLongitude) {
        double span = SkyFrame.normalise(shower.endLongitude() - shower.startLongitude());
        return SkyFrame.normalise(solarLongitude - shower.startLongitude()) < span;
    }

    /** Whether a shower is active at an instant. */
    public boolean active(MeteorShower shower, Instant instant) {
        return active(shower, solarLongitude(instant));
    }

    /** The showers active at an instant, in the order of the year. */
    public List<MeteorShower> activeAt(Instant instant) {
        double sol = solarLongitude(instant);
        List<MeteorShower> found = new ArrayList<>();
        for (MeteorShower shower : pack.showers()) {
            if (active(shower, sol)) {
                found.add(shower);
            }
        }
        return List.copyOf(found);
    }

    /** The radiant at an instant, whether or not the shower is active then. */
    public Radiant radiantAt(MeteorShower shower, Instant instant) {
        return radiantAt(shower, solarLongitude(instant));
    }

    /**
     * The ruled evaluation (R-9): the radiant table read against solar
     * longitude, along the great circle between two printed rows, the
     * nearest pair carried on beyond them, and a shower with fewer than
     * two rows held at its printed position.
     */
    public static Radiant radiantAt(MeteorShower shower, double solarLongitude) {
        List<MeteorShower.Row> rows = shower.rows();
        if (rows.size() < 2) {
            return new Radiant(rows.isEmpty() ? shower.radiantAtMaximum() : rows.get(0).at(),
                    Footing.FIXED);
        }
        double anchor = shower.maximumLongitude();
        double x = signed(solarLongitude - anchor);
        int i = 0;
        while (i < rows.size() - 2 && signed(rows.get(i + 1).solarLongitude() - anchor) < x) {
            i++;
        }
        MeteorShower.Row a = rows.get(i);
        MeteorShower.Row b = rows.get(i + 1);
        double xa = signed(a.solarLongitude() - anchor);
        double xb = signed(b.solarLongitude() - anchor);
        double f = (x - xa) / (xb - xa);
        return new Radiant(along(a.at(), b.at(), f),
                f < 0.0 || f > 1.0 ? Footing.BEYOND_ROWS : Footing.BETWEEN_ROWS);
    }

    /**
     * The first instant at or after another at which the Sun reaches the
     * shower's listed solar longitude - its nominal maximum (R-7, R-17) -
     * to the second, or empty when that falls outside the years the Sun
     * is computed for.
     */
    public Optional<Instant> nominalMaximumAtOrAfter(MeteorShower shower, Instant from) {
        try {
            double ahead = SkyFrame.normalise(shower.maximumLongitude() - solarLongitude(from));
            // A maximum within a second behind is this one, not next year's.
            if (ahead > 360.0 - 1e-5) {
                ahead -= 360.0;
            }
            Instant found = solve(shower, from.getEpochSecond()
                    + Math.round(ahead / MEAN_MOTION * 86400.0));
            if (found.isBefore(from)) {
                found = solve(shower, found.getEpochSecond() + Math.round(365.2564 * 86400.0));
            }
            return Optional.of(found);
        } catch (IllegalArgumentException outsideTheYears) {
            return Optional.empty();
        }
    }

    /** The instant near a guess at which the Sun reaches the shower's listed longitude. */
    private Instant solve(MeteorShower shower, long guess) {
        long t = guess;
        for (int i = 0; i < 20; i++) {
            double miss = signed(shower.maximumLongitude()
                    - solarLongitude(Instant.ofEpochSecond(t)));
            long step = Math.round(miss / MEAN_MOTION * 86400.0);
            t += step;
            if (step == 0) {
                break;
            }
        }
        return Instant.ofEpochSecond(t);
    }

    /** The point a fraction of the way along the great circle from a to b, or beyond it. */
    static SkyPosition along(SkyPosition a, SkyPosition b, double f) {
        double omega = Math.toRadians(a.separationDegrees(b));
        if (omega < 1e-12) {
            return a;
        }
        double[] va = SkyFrame.toVector(a);
        double[] vb = SkyFrame.toVector(b);
        double sa = Math.sin((1.0 - f) * omega) / Math.sin(omega);
        double sb = Math.sin(f * omega) / Math.sin(omega);
        return SkyFrame.toPosition(new double[] {sa * va[0] + sb * vb[0],
                sa * va[1] + sb * vb[1], sa * va[2] + sb * vb[2]});
    }

    static double signed(double degrees) {
        double d = degrees % 360.0;
        return d > 180.0 ? d - 360.0 : d <= -180.0 ? d + 360.0 : d;
    }
}
