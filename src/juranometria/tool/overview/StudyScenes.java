package juranometria.tool.overview;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import juranometria.catalog.TiledCatalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.geo.Constellation;
import juranometria.geo.ConstellationGeography;
import juranometria.geo.GeoSegment;
import juranometria.render.GeographyDetailPolicy;

/**
 * Real pages of the real sky, for any candidate (issue #296).
 *
 * <p>The catalogue, the geography, the detail policy and the scene
 * itself are production's. One thing is not, and it is the finding
 * this class exists to make visible: <strong>production decides how
 * much sky to fetch with a gnomonic formula.</strong>
 *
 * <p>{@code SceneAssembler.queryRadiusDegrees} works out the page's
 * corner as {@code atan(hypot(tan(field/2), ...))}. That is the
 * gnomonic corner and no other projection's. Ask it for a
 * 120-degree stereographic page and it fetches a cone of 65.5
 * degrees for a page that reaches 72.4 - and the stars in the ring
 * between are not drawn faintly or badly, they are absent, in the
 * corners, where nothing looks obviously wrong.
 *
 * <p>So the study asks each projection where its own page corner is
 * and fetches that, and {@link #shortfall} counts what production's
 * rule would have left out.
 */
final class StudyScenes {

    private final TiledCatalogue catalogue;
    private final ConstellationGeography geography;
    private final double margin;

    StudyScenes() {
        this.catalogue = TiledCatalogue.load();
        this.geography = ConstellationGeography.load();
        this.margin = catalogue.manifest().maxObjectSemiExtentDegrees();
    }

    /** The page corner for this projection, plus the object margin. */
    double queryRadiusFor(StudyProjection projection, double fieldDegrees) {
        double corner = DistortionReport.cornerDegrees(fieldDegrees,
                projection);
        return Math.min(180.0,
                (Double.isNaN(corner) ? projection.limitDegrees() : corner)
                        + margin);
    }

    ChartScene of(StudyProjection projection, double fieldDegrees,
                  int widthPx, int heightPx, String title) {
        return of(projection, fieldDegrees, widthPx, heightPx, title,
                DEFAULT_LIMIT);
    }

    ChartScene of(StudyProjection projection, double fieldDegrees,
                  int widthPx, int heightPx, String title,
                  double limitingMagnitude) {
        SkyRegion query = new SkyRegion(projection.centre(),
                queryRadiusFor(projection, fieldDegrees));
        return new ChartScene(
                new ChartViewport(projection.centre(), fieldDegrees,
                        widthPx, heightPx),
                catalogue.starsIn(query),
                catalogue.deepSkyObjectsIn(query),
                title, limitingMagnitude, null,
                geographyOf(fieldDegrees, query));
    }

    /**
     * How many catalogue objects production's gnomonic query radius
     * would have missed on this page.
     */
    int shortfall(StudyProjection projection, double fieldDegrees) {
        double honest = queryRadiusFor(projection, fieldDegrees);
        StudyProjection gnomonic = Candidates.gnomonic(projection.centre());
        double asProduction = queryRadiusFor(gnomonic, fieldDegrees);
        if (asProduction >= honest) {
            return 0;
        }
        SkyPosition centre = projection.centre();
        int missed = 0;
        for (var star : catalogue.starsIn(new SkyRegion(centre, honest))) {
            double away = separation(centre, star.position());
            if (away > asProduction) {
                missed++;
            }
        }
        return missed;
    }

    /**
     * The magnitude the pages are drawn at.
     *
     * <p>The chart's existing default, held constant so that the
     * pages differ by projection and by nothing else.
     */
    static final double DEFAULT_LIMIT = 6.0;

    private SceneGeography geographyOf(double fieldDegrees, SkyRegion query) {
        GeographyDetailPolicy policy =
                new GeographyDetailPolicy(fieldDegrees);
        List<GeoSegment> figures = policy.figuresDrawn()
                ? geography.figureSegmentsIn(query) : List.of();
        List<GeoSegment> boundaries = policy.boundariesDrawn()
                ? geography.boundarySegmentsIn(query) : List.of();
        Map<String, String> names = new LinkedHashMap<>();
        if (policy.namesDrawn()) {
            Set<String> present = new LinkedHashSet<>();
            for (GeoSegment segment : figures) {
                present.add(segment.constellationId());
            }
            for (Constellation constellation : geography.constellations()) {
                if (present.contains(constellation.id())) {
                    names.put(constellation.id(), constellation.latinName());
                }
            }
        }
        return new SceneGeography(figures, boundaries, names);
    }

    /**
     * The angle between two positions, in degrees.
     *
     * <p>From the chord rather than from the cosine. The cosine
     * formula is the obvious one and it cannot measure a small
     * angle: near zero separation its argument is within rounding of
     * 1, and `acos` there has a floor of about
     * <strong>8.5e-07 degrees</strong> whatever it is given. The
     * study's round-trip measurement reported exactly that figure
     * for all three candidates - the same number three times, which
     * is what an instrument reads when it is measuring itself. From
     * the chord the same round trip reads 1e-13, and the three
     * candidates differ from each other again.
     */
    static double separation(SkyPosition a, SkyPosition b) {
        double[] first = unit(a);
        double[] second = unit(b);
        double chord = Math.sqrt(
                (first[0] - second[0]) * (first[0] - second[0])
                        + (first[1] - second[1]) * (first[1] - second[1])
                        + (first[2] - second[2]) * (first[2] - second[2]));
        return Math.toDegrees(
                2.0 * Math.asin(Math.clamp(chord / 2.0, -1.0, 1.0)));
    }

    private static double[] unit(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }
}
