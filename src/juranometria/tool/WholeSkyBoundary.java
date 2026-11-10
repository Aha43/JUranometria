package juranometria.tool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.CurveRun;
import juranometria.project.DrawnPage;
import juranometria.project.GreatCirclePage;
import juranometria.project.PageRegion;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.render.BelowHorizon;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartRenderer;
import juranometria.sky.Ecliptic;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;

/**
 * The whole-sky page where its central-meridian rule runs out (#709):
 * the zenith at or near an ecliptic pole.
 *
 * <p>The central meridian is the ecliptic longitude of the zenith (R-4).
 * At an ecliptic pole that longitude is undefined, and close to one it
 * turns fast; an observer near a polar circle has the zenith pass close
 * to a pole once every sidereal day. Here is what the as-built study
 * and {@code WholeSkyPageTest} both measure there, in one place, so the
 * report and the test cannot disagree about what was asked: where to
 * stand, and what a page composed there must still hold.
 *
 * <p>Nothing here decides anything. It measures the production page.
 */
public final class WholeSkyBoundary {

    private WholeSkyBoundary() {
    }

    /** The north ecliptic pole of J2000, and the south. */
    public static final SkyPosition NORTH_POLE = Ecliptic.POLE;
    public static final SkyPosition SOUTH_POLE = new SkyPosition(
            SkyFrame.normalise(Ecliptic.POLE.raDegrees() + 180.0),
            -Ecliptic.POLE.decDegrees());

    /**
     * The observer whose zenith is this J2000 direction at this
     * instant, through the atlas's own frame: the direction of date is
     * the latitude and the local sidereal time, and a few corrections
     * on the zenith itself take the remainder down to rounding.
     */
    public static Observer observerUnder(SkyPosition j2000, Instant when) {
        double jd = SkyFrame.julianDate(when);
        SkyPosition ofDate = SkyFrame.toOfDate(j2000, jd);
        Observer observer = new Observer(ofDate.decDegrees(),
                SkyFrame.normalise(ofDate.raDegrees() - SkyFrame.gastDegrees(jd)), when);
        for (int i = 0; i < 4; i++) {
            SkyPosition zenith = new LocalSky(observer).zenith();
            double east = ((j2000.raDegrees() - zenith.raDegrees() + 540.0) % 360.0) - 180.0;
            double north = j2000.decDegrees() - zenith.decDegrees();
            observer = new Observer(
                    Math.max(-90.0, Math.min(90.0, observer.latitudeDegrees() + north)),
                    observer.eastLongitudeDegrees() + east, when);
        }
        return observer;
    }

    /** One reference great circle as the page draws it. */
    public record Circle(int runs, boolean finite, double longestChordPx) {
    }

    /**
     * What a composed whole-sky page holds.
     *
     * @param centralLongitudeDegrees the central meridian the page states
     * @param zenithOffCentralMeridianPx how far the zenith is drawn from
     *     the central meridian, in pixels (R-4 says none)
     * @param bodies the Solar System bodies drawn
     * @param circles the ecliptic, the celestial equator, the horizon
     *     and the observer's meridian, by those names
     * @param meridianPointsAstray points of the observer's meridian that
     *     are neither on the central meridian's line nor on the outline -
     *     meaningful when the meridian passes through the map's poles,
     *     where half of it is the central meridian and half the seam
     * @param subdueDisagreement the share of sample pixels where the
     *     region below the drawn horizon is not the true side of it,
     *     on a grid that avoids the middle line (where the horizon
     *     itself lies when the zenith is a pole)
     * @param title the title block's lines
     */
    public record Held(double centralLongitudeDegrees, double zenithOffCentralMeridianPx,
                       int bodies, Map<String, Circle> circles, int meridianPointsAstray,
                       double subdueDisagreement, List<String> title) {
    }

    /** Measures a composed page; on the event thread. */
    public static Held measure(WholeSkyComposition.Page page, int widthPx, int heightPx) {
        ChartScene scene = page.chart().currentScene();
        DrawnPage drawn = DrawnPage.of(scene);
        Projection projection = drawn.projection();
        Projection.WholeSphere whole = projection.wholeSphere().orElseThrow();
        ViewportMapping mapping = new ViewportMapping(drawn);
        PageRegion region = mapping.regionFor(scene.viewport(), projection);
        LocalSky sky = new LocalSky(page.meridian().observer());
        SkyPosition zenith = sky.zenith();

        Optional<PlanePoint> zenithOnPlane = projection.project(zenith);
        double zenithOff = zenithOnPlane.isEmpty() ? Double.NaN
                : mapping.toPixel(zenithOnPlane.get()).x() - region.limbX();

        Map<String, SkyPosition> poles = new LinkedHashMap<>();
        poles.put("ecliptic", Ecliptic.POLE);
        poles.put("celestial equator", new SkyPosition(0.0, 90.0));
        poles.put("horizon", sky.horizon().pole());
        poles.put("meridian", sky.meridian().pole());
        Map<String, Circle> circles = new LinkedHashMap<>();
        int astray = 0;
        for (Map.Entry<String, SkyPosition> each : poles.entrySet()) {
            List<CurveRun> runs = GreatCirclePage.clip(projection, mapping, region,
                    each.getValue());
            boolean finite = true;
            double longest = 0.0;
            for (CurveRun run : runs) {
                List<PixelPoint> points = ((CurveRun.Polyline) run).points();
                for (int i = 0; i < points.size(); i++) {
                    PixelPoint at = points.get(i);
                    finite &= Double.isFinite(at.x()) && Double.isFinite(at.y());
                    if (i > 0) {
                        longest = Math.max(longest, Math.hypot(at.x() - points.get(i - 1).x(),
                                at.y() - points.get(i - 1).y()));
                    }
                    if (each.getKey().equals("meridian")) {
                        double x = (at.x() - region.limbX()) / region.limbRadius();
                        double y = (at.y() - region.limbY()) / region.limbRadiusAcross();
                        boolean onTheLine = Math.abs(at.x() - region.limbX()) < 1.0;
                        boolean onTheOutline = Math.abs(x * x + y * y - 1.0) < 1e-3;
                        if (!onTheLine && !onTheOutline) {
                            astray++;
                        }
                    }
                }
            }
            circles.put(each.getKey(), new Circle(runs.size(), finite, longest));
        }

        java.awt.Shape below = BelowHorizon.of(zenith, scene).belowRegion();
        int sampled = 0;
        int wrong = 0;
        for (int py = 1; py < heightPx; py += 8) {
            for (int px = 1; px < widthPx; px += 8) {
                SkyPosition at = ChartHitTest.skyAt(drawn, px, py);
                if (at == null) {
                    continue;
                }
                sampled++;
                if (below.contains(px, py) != (sky.altitudeDegrees(at) < 0.0)) {
                    wrong++;
                }
            }
        }
        List<String> title = new ArrayList<>(
                new ChartRenderer(StarSizePolicy.DEFAULT, page.words()).titleBlockLines(scene));
        return new Held(whole.centralLongitudeDegrees(), zenithOff,
                page.chart().renderedBodies().size(), circles, astray,
                sampled == 0 ? Double.NaN : (double) wrong / sampled, title);
    }

    /**
     * How the central meridian moves through one sidereal day at a
     * latitude: the nearest the zenith comes to its ecliptic pole,
     * whether the central longitude goes all the way round or swings
     * and by how much, its fastest turn, and the shortest time in which
     * it turns a quarter.
     *
     * @param swingDegrees the width of the swing, or 360 when the
     *     central meridian goes all the way round
     * @param quarterTurnSeconds the shortest time for a 90-degree
     *     turn, or -1 when it never turns that far
     */
    public record Day(double nearestPoleDegrees, double swingDegrees, double swingAboutDegrees,
                      double fastestDegreesPerMinute, int quarterTurnSeconds) {

        public boolean goesRound() {
            return swingDegrees >= 359.0;
        }
    }

    /** One sidereal day, sampled every second of chart time. */
    public static Day day(double latitudeDegrees, double eastLongitudeDegrees, Instant from) {
        int seconds = 86164;
        SkyPosition pole = latitudeDegrees >= 0.0 ? NORTH_POLE : SOUTH_POLE;
        double[] unwrapped = new double[seconds + 1];
        double nearest = Double.MAX_VALUE;
        double fastest = 0.0;
        double low = 0.0;
        double high = 0.0;
        double previous = 0.0;
        for (int s = 0; s <= seconds; s++) {
            SkyPosition zenith = new LocalSky(new Observer(latitudeDegrees,
                    eastLongitudeDegrees, from.plusSeconds(s))).zenith();
            double longitude = Ecliptic.toEcliptic(zenith).longitudeDegrees();
            nearest = Math.min(nearest, zenith.separationDegrees(pole));
            if (s == 0) {
                unwrapped[0] = longitude;
                low = longitude;
                high = longitude;
            } else {
                double step = ((longitude - previous + 540.0) % 360.0) - 180.0;
                unwrapped[s] = unwrapped[s - 1] + step;
                fastest = Math.max(fastest, Math.abs(step) * 60.0);
                low = Math.min(low, unwrapped[s]);
                high = Math.max(high, unwrapped[s]);
            }
            previous = longitude;
        }
        int quarter = -1;
        if (high - low >= 90.0) {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i <= seconds; i++) {
                for (int j = i + 1; j <= seconds && j - i < best; j++) {
                    if (Math.abs(unwrapped[j] - unwrapped[i]) >= 90.0) {
                        best = j - i;
                        break;
                    }
                }
            }
            quarter = best == Integer.MAX_VALUE ? -1 : best;
        }
        double swing = Math.min(360.0, high - low);
        return new Day(nearest, swing, SkyFrame.normalise((high + low) / 2.0), fastest, quarter);
    }
}
