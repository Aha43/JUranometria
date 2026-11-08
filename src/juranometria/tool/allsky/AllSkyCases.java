package juranometria.tool.allsky;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import juranometria.chart.SkyPosition;
import juranometria.jovianchart.JovianModule;
import juranometria.module.OverlayContribution;
import juranometria.saturnchart.SaturnModule;
import juranometria.sky.Ecliptic;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solarchart.SolarSystemModule;

/**
 * The study's chart times, found by the application rather than typed
 * (#686, study only; the "cite every number" rule).
 *
 * <p>Every position here is a production module's own contribution -
 * the same {@code OverlayContribution} a page draws - so a case is
 * chosen by exactly what the chart would show. Three searches, each
 * stated with its range and criterion so it can be rerun:
 * <ol>
 * <li><b>evening</b>: Oslo, each night at 21:00 UTC over a year from the
 * study's start; among nights with the Sun more than 12 degrees below
 * the horizon, the one with most of the eight bodies other than the
 * Sun above it (earliest wins a tie).</li>
 * <li><b>gathering</b>: 00:00 UTC daily over five years; the instant whose
 * seven planets fit in the shortest arc of ecliptic longitude.</li>
 * <li><b>spread</b>: the same days; the instant whose nine bodies (Sun,
 * Moon, seven planets) leave the smallest largest empty gap in
 * ecliptic longitude - so that no central meridian can put the seam
 * far from every body.</li>
 * </ol>
 */
public final class AllSkyCases {

    public static final double OSLO_LAT = 59.91;
    public static final double OSLO_LON = 10.75;
    public static final Instant START = Instant.parse("2026-11-08T00:00:00Z");

    static final List<String> PLANETS = List.of("mercury", "venus", "mars",
            "jovian.jupiter", "saturn", "uranus", "neptune");

    /** One body as a module contributed it. */
    public record Seen(String identity, SkyPosition at, boolean belowHorizon) {
    }

    /** A found case and how it was found. */
    public record Case(String id, Instant instant, String criterion, String measure) {
    }

    private final Observer[] now = new Observer[1];
    private final SolarSystemModule solar;
    private final JovianModule jupiter;
    private final SaturnModule saturn;

    public AllSkyCases(AllSkyComposition.Services services) {
        solar = new SolarSystemModule(() -> now[0], () -> services.solar, () -> true);
        solar.withMars(() -> services.mars);
        solar.withUranus(() -> services.uranus);
        solar.withNeptune(() -> services.neptune);
        solar.sunShowing(true);
        solar.moonShowing(true);
        solar.mercuryShowing(true);
        solar.venusShowing(true);
        solar.marsShowing(true);
        solar.uranusShowing(true);
        solar.neptuneShowing(true);
        jupiter = new JovianModule(() -> now[0], () -> services.jovian, () -> true);
        jupiter.showing(true);
        saturn = new SaturnModule(() -> now[0], () -> services.saturn, () -> true);
        saturn.showing(true);
    }

    /** The Sun, Moon and seven planets at this instant, as the modules offer them. */
    public Map<String, Seen> bodies(Observer observer) {
        now[0] = observer;
        Map<String, Seen> out = new LinkedHashMap<>();
        List<OverlayContribution> all = new ArrayList<>();
        all.addAll(solar.contributedGeometry());
        all.addAll(jupiter.contributedGeometry());
        all.addAll(saturn.contributedGeometry());
        for (OverlayContribution c : all) {
            if (c instanceof OverlayContribution.Body b) {
                out.put(b.identity(), new Seen(b.identity(), b.at(), b.belowHorizon()));
            } else if (c instanceof OverlayContribution.OblateBody b) {
                out.put(b.identity(), new Seen(b.identity(), b.at(), b.belowHorizon()));
            } else if (c instanceof OverlayContribution.RingedBody b) {
                out.put(b.identity(), new Seen(b.identity(), b.at(), b.belowHorizon()));
            }
        }
        return out;
    }

    public static Observer oslo(Instant instant) {
        return new Observer(OSLO_LAT, OSLO_LON, instant);
    }

    private static double longitude(SkyPosition p) {
        return Ecliptic.toEcliptic(p).longitudeDegrees();
    }

    /** Shortest arc of longitude holding every value, and the largest empty gap. */
    static double[] arcAndGap(List<Double> longitudes) {
        List<Double> sorted = new ArrayList<>(longitudes);
        sorted.sort(Double::compare);
        double gap = 0.0;
        for (int i = 0; i < sorted.size(); i++) {
            double next = i + 1 < sorted.size() ? sorted.get(i + 1) : sorted.get(0) + 360.0;
            gap = Math.max(gap, next - sorted.get(i));
        }
        return new double[] {360.0 - gap, gap};
    }

    public List<Case> find() {
        List<Case> cases = new ArrayList<>();
        Instant bestEvening = null;
        int bestUp = -1;
        for (int d = 0; d < 365; d++) {
            Instant t = START.plus(Duration.ofDays(d)).plus(Duration.ofHours(21));
            Observer o = oslo(t);
            Map<String, Seen> seen = bodies(o);
            double sunAlt = new LocalSky(o).altitudeDegrees(seen.get("sun").at());
            if (sunAlt > -12.0) {
                continue;
            }
            int up = 0;
            for (Seen s : seen.values()) {
                if (!s.identity().equals("sun") && !s.belowHorizon()) {
                    up++;
                }
            }
            if (up > bestUp) {
                bestUp = up;
                bestEvening = t;
            }
        }
        cases.add(new Case("evening", bestEvening,
                "Oslo 21:00 UTC nightly for 365 nights from " + START
                        + "; Sun below -12 degrees; most of Moon and seven planets above the horizon",
                bestUp + " of 8 above the horizon"));
        Instant gather = null;
        double gatherArc = Double.MAX_VALUE;
        Instant spread = null;
        double spreadGap = Double.MAX_VALUE;
        for (int d = 0; d < 5 * 365; d++) {
            Instant t = START.plus(Duration.ofDays(d));
            Map<String, Seen> seen = bodies(oslo(t));
            List<Double> planets = new ArrayList<>();
            List<Double> nine = new ArrayList<>();
            for (Seen s : seen.values()) {
                double lon = longitude(s.at());
                nine.add(lon);
                if (PLANETS.contains(s.identity())) {
                    planets.add(lon);
                }
            }
            double arc = arcAndGap(planets)[0];
            if (arc < gatherArc) {
                gatherArc = arc;
                gather = t;
            }
            double gap = arcAndGap(nine)[1];
            if (gap < spreadGap) {
                spreadGap = gap;
                spread = t;
            }
        }
        cases.add(new Case("gathering", gather,
                "00:00 UTC daily for 1825 days from " + START
                        + "; shortest ecliptic-longitude arc holding the seven planets",
                String.format(java.util.Locale.ROOT, "%.1f degrees", gatherArc)));
        cases.add(new Case("spread", spread,
                "00:00 UTC daily for 1825 days from " + START
                        + "; smallest largest empty gap among the nine bodies",
                String.format(java.util.Locale.ROOT, "largest gap %.1f degrees", spreadGap)));
        return cases;
    }
}
