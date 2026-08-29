package juranometria.ui;

import java.util.OptionalDouble;

import juranometria.catalog.Catalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.SceneGeography;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;

/**
 * Assembles complete immutable chart scenes by querying the local
 * catalogue from the current view state and pixel viewport. This is the
 * seam between interaction and data: it runs on state or size changes at
 * the UI boundary, never inside painting.
 *
 * The assembler distinguishes the mutable chart centre (part of the view
 * state) from the fixed data centre of the bundled coverage cone. The
 * declared coverage rule for any view is: the chart centre's offset from
 * the data centre, plus the visible corner radius, plus the object-extent
 * margin, must stay inside the coverage cone. {@link #fits} and
 * {@link #widestFittingFieldDegrees} answer coverage questions without
 * querying or painting.
 */
public final class SceneAssembler {

    /**
     * Added to the corner distance so an object whose centre lies outside
     * the frame but whose symbol reaches in is still queried. The value
     * is the installed pack's declared maximum object semi-extent from
     * its manifest (5.39 degrees for the bright-sky pack, the Large
     * Magellanic Cloud) - never a constant sized for one region (Sprint 5
     * Codex review).
     */
    private final double objectExtentMarginDegrees;

    private final Catalogue catalogue;
    private final SkyPosition dataCentre;
    private final double coverageRadiusDegrees;
    private final boolean allSky;
    /** Bundled constellation geography; null keeps every page bare. */
    private final juranometria.geo.ConstellationGeography geography;

    /**
     * @param dataCentre the fixed centre of the bundled data's coverage
     *     cone (distinct from the mutable chart centre)
     * @param coverageRadiusDegrees the bundled data's cone radius around
     *     the data centre
     */
    public SceneAssembler(Catalogue catalogue, SkyPosition dataCentre,
                          double coverageRadiusDegrees, double objectExtentMarginDegrees) {
        this(catalogue, dataCentre, coverageRadiusDegrees,
                objectExtentMarginDegrees, null);
    }

    public SceneAssembler(Catalogue catalogue, SkyPosition dataCentre,
                          double coverageRadiusDegrees,
                          double objectExtentMarginDegrees,
                          juranometria.geo.ConstellationGeography geography) {
        if (catalogue == null || dataCentre == null) {
            throw new IllegalArgumentException("catalogue and centre are required");
        }
        requireValidMargin(objectExtentMarginDegrees);
        if (!(coverageRadiusDegrees > objectExtentMarginDegrees)) {
            throw new IllegalArgumentException(
                    "coverage must exceed the object margin: " + coverageRadiusDegrees);
        }
        this.catalogue = catalogue;
        this.dataCentre = dataCentre;
        this.coverageRadiusDegrees = coverageRadiusDegrees;
        this.objectExtentMarginDegrees = objectExtentMarginDegrees;
        this.allSky = false;
        this.geography = geography;
    }

    private SceneAssembler(Catalogue catalogue, double objectExtentMarginDegrees,
                           juranometria.geo.ConstellationGeography geography) {
        this.catalogue = catalogue;
        this.dataCentre = null;
        this.coverageRadiusDegrees = Double.NaN;
        this.objectExtentMarginDegrees = objectExtentMarginDegrees;
        this.allSky = true;
        this.geography = geography;
    }

    /**
     * An assembler over complete all-sky coverage: every centre fits at
     * every supported field, there is no data centre to be offset from,
     * and the page height is bounded only by projection sanity (chart
     * corners stay within {@link #PROJECTION_CORNER_LIMIT_DEGREES} of the
     * centre, far beyond any realistic window).
     *
     * @param objectExtentMarginDegrees the pack's declared maximum object
     *     semi-extent, from its manifest
     */
    public static SceneAssembler allSky(Catalogue catalogue,
                                        double objectExtentMarginDegrees) {
        return allSky(catalogue, objectExtentMarginDegrees, null);
    }

    public static SceneAssembler allSky(Catalogue catalogue,
                                        double objectExtentMarginDegrees,
                                        juranometria.geo.ConstellationGeography geography) {
        if (catalogue == null) {
            throw new IllegalArgumentException("catalogue is required");
        }
        requireValidMargin(objectExtentMarginDegrees);
        return new SceneAssembler(catalogue, objectExtentMarginDegrees, geography);
    }

    private static void requireValidMargin(double objectExtentMarginDegrees) {
        if (!(objectExtentMarginDegrees > 0.0)
                || !Double.isFinite(objectExtentMarginDegrees)) {
            throw new IllegalArgumentException(
                    "object extent margin must be positive and finite: "
                            + objectExtentMarginDegrees);
        }
    }

    /** The margin this assembler queries with; for tests and diagnostics. */
    public double objectExtentMarginDegrees() {
        return objectExtentMarginDegrees;
    }

    /** Gnomonic charts degrade far from the centre; cap the page there. */
    static final double PROJECTION_CORNER_LIMIT_DEGREES = 60.0;

    /**
     * Queries the catalogue around the state's centre and builds the scene
     * for one page. The page dimensions must respect
     * {@link #maxPageHeightPx}; a view that would reach beyond the bundled
     * coverage is an error, never a silently sparse chart.
     */
    public ChartScene assemble(ChartViewState state, int widthPx, int heightPx) {
        double radius = queryRadiusDegrees(state.fieldWidthDegrees(), widthPx, heightPx);
        if (allSky) {
            return assembleScene(state, widthPx, heightPx, radius);
        }
        double offset = state.centre().separationDegrees(dataCentre);
        if (offset + radius > coverageRadiusDegrees) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    "page %dx%d at a %.1f-degree field offset %.2f degrees needs data"
                            + " to %.2f degrees but coverage ends at %.1f",
                    widthPx, heightPx, state.fieldWidthDegrees(), offset,
                    offset + radius, coverageRadiusDegrees));
        }
        return assembleScene(state, widthPx, heightPx, radius);
    }

    private ChartScene assembleScene(ChartViewState state, int widthPx, int heightPx,
                                     double radius) {
        ChartViewport viewport = new ChartViewport(
                state.centre(), state.fieldWidthDegrees(), widthPx, heightPx);
        SkyRegion query = new SkyRegion(state.centre(), Math.min(radius, 180.0));
        return new ChartScene(viewport,
                catalogue.starsIn(query),
                catalogue.deepSkyObjectsIn(query),
                titleFor(state), state.limitingMagnitude(), state.targetIdentity(),
                geographyFor(state, query));
    }

    /**
     * Queries the geography layers the scale policy admits at this field
     * - and only those, so narrow pages perform no geography work at all.
     */
    private SceneGeography geographyFor(ChartViewState state, SkyRegion query) {
        if (geography == null) {
            return SceneGeography.EMPTY;
        }
        var policy = new juranometria.render.GeographyDetailPolicy(
                state.fieldWidthDegrees());
        java.util.List<juranometria.geo.GeoSegment> figures =
                policy.figuresDrawn() ? geography.figureSegmentsIn(query)
                        : java.util.List.of();
        java.util.List<juranometria.geo.GeoSegment> boundaries =
                policy.boundariesDrawn() ? geography.boundarySegmentsIn(query)
                        : java.util.List.of();
        java.util.Map<String, String> names = new java.util.LinkedHashMap<>();
        if (policy.namesDrawn()) {
            java.util.Set<String> present = new java.util.LinkedHashSet<>();
            for (juranometria.geo.GeoSegment segment : figures) {
                present.add(segment.constellationId());
            }
            for (juranometria.geo.Constellation constellation
                    : geography.constellations()) {
                if (present.contains(constellation.id())) {
                    names.put(constellation.id(), constellation.latinName());
                }
            }
        }
        return new SceneGeography(figures, boundaries, names);
    }

    /**
     * The title policy: a named target titles the chart; an anonymous
     * view titles itself by its coordinates in the chart's notation.
     */
    static String titleFor(ChartViewState state) {
        if (state.targetLabel() != null) {
            return state.targetLabel();
        }
        return juranometria.chart.SkyFormat.formatRa(state.centre().raDegrees())
                + ", " + juranometria.chart.SkyFormat.formatDec(state.centre().decDegrees());
    }

    /**
     * At exact horizontal equality the letterboxed page height is zero, so
     * fitting demands this much slack beyond the half field — enough for a
     * small but genuinely drawable page (Codex review, Sprint 4).
     */
    static final double MINIMUM_PAGE_ALLOWANCE_DEGREES = 0.05;

    /**
     * Whether a centre/field combination can be drawn completely as a
     * positive page: the centre's offset plus the field's half width plus
     * the object margin stay strictly inside the coverage cone with the
     * minimum page allowance. Geometry-independent — the page height
     * letterboxes separately via {@link #maxPageHeightPx}. This is the one
     * validity predicate shared by search, navigation transitions, and
     * scene assembly.
     */
    public boolean fits(SkyPosition centre, double fieldWidthDegrees) {
        if (allSky) {
            return true;
        }
        return centre.separationDegrees(dataCentre)
                + fieldWidthDegrees / 2.0
                + objectExtentMarginDegrees
                + MINIMUM_PAGE_ALLOWANCE_DEGREES <= coverageRadiusDegrees;
    }

    /** The shared predicate over a complete view state. */
    public boolean fits(juranometria.chart.ChartViewState state) {
        return fits(state.centre(), state.fieldWidthDegrees());
    }

    /**
     * The widest supported field step that can be drawn completely around
     * the centre, or empty when not even the narrowest step fits there.
     */
    public OptionalDouble widestFittingFieldDegrees(SkyPosition centre) {
        for (double step : ChartViewState.fieldWidthSteps()) {
            if (fits(centre, step)) {
                return OptionalDouble.of(step);
            }
        }
        return OptionalDouble.empty();
    }

    /**
     * The tallest page (in pixels) whose corners, plus the object-extent
     * margin, stay inside the bundled coverage for this centre and field
     * width. A taller window letterboxes the page rather than promising
     * sky the data does not hold; an offset centre allows less height.
     */
    public int maxPageHeightPx(SkyPosition centre, double fieldWidthDegrees, int widthPx) {
        double halfWidthPlane = Math.tan(Math.toRadians(fieldWidthDegrees) / 2.0);
        if (allSky) {
            double limitPlane = Math.tan(Math.toRadians(PROJECTION_CORNER_LIMIT_DEGREES));
            double halfHeightPlane = Math.sqrt(
                    limitPlane * limitPlane - halfWidthPlane * halfWidthPlane);
            return (int) Math.floor(widthPx * halfHeightPlane / halfWidthPlane);
        }
        double allowedCornerDegrees = coverageRadiusDegrees
                - objectExtentMarginDegrees
                - centre.separationDegrees(dataCentre);
        if (allowedCornerDegrees <= 0) {
            return 0;
        }
        double maxCornerPlane = Math.tan(Math.toRadians(allowedCornerDegrees));
        if (maxCornerPlane <= halfWidthPlane) {
            return 0;
        }
        double halfHeightPlane = Math.sqrt(
                maxCornerPlane * maxCornerPlane - halfWidthPlane * halfWidthPlane);
        return (int) Math.floor(widthPx * halfHeightPlane / halfWidthPlane);
    }

    /**
     * The exact angular distance from the centre to the viewport corners
     * on the gnomonic plane, at the current aspect ratio, plus the object
     * extent margin — so no eligible object is silently clipped even in
     * tall or wide windows.
     */
    double queryRadiusDegrees(double fieldWidthDegrees, int widthPx, int heightPx) {
        double halfWidthPlane = Math.tan(Math.toRadians(fieldWidthDegrees) / 2.0);
        double halfHeightPlane = halfWidthPlane * heightPx / (double) widthPx;
        double cornerDegrees = Math.toDegrees(
                Math.atan(Math.hypot(halfWidthPlane, halfHeightPlane)));
        return cornerDegrees + objectExtentMarginDegrees;
    }
}
