package juranometria.ui;

import java.util.OptionalDouble;

import juranometria.catalog.Catalogue;
import juranometria.chart.ChartScene;
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
     * the frame but whose symbol reaches in is still queried. M31 itself
     * is the largest catalogued object at a 88.9 arcminute semi-major
     * axis, comfortably under this margin.
     */
    static final double OBJECT_EXTENT_MARGIN_DEGREES = 1.5;

    private final Catalogue catalogue;
    private final SkyPosition dataCentre;
    private final String title;
    private final double coverageRadiusDegrees;

    /**
     * @param dataCentre the fixed centre of the bundled data's coverage
     *     cone (distinct from the mutable chart centre)
     * @param coverageRadiusDegrees the bundled data's cone radius around
     *     the data centre
     */
    public SceneAssembler(Catalogue catalogue, SkyPosition dataCentre, String title,
                          double coverageRadiusDegrees) {
        if (catalogue == null || dataCentre == null || title == null) {
            throw new IllegalArgumentException("catalogue, centre, and title are required");
        }
        if (!(coverageRadiusDegrees > OBJECT_EXTENT_MARGIN_DEGREES)) {
            throw new IllegalArgumentException(
                    "coverage must exceed the object margin: " + coverageRadiusDegrees);
        }
        this.catalogue = catalogue;
        this.dataCentre = dataCentre;
        this.title = title;
        this.coverageRadiusDegrees = coverageRadiusDegrees;
    }

    /**
     * Queries the catalogue around the state's centre and builds the scene
     * for one page. The page dimensions must respect
     * {@link #maxPageHeightPx}; a view that would reach beyond the bundled
     * coverage is an error, never a silently sparse chart.
     */
    public ChartScene assemble(ChartViewState state, int widthPx, int heightPx) {
        double offset = state.centre().separationDegrees(dataCentre);
        double radius = queryRadiusDegrees(state.fieldWidthDegrees(), widthPx, heightPx);
        if (offset + radius > coverageRadiusDegrees) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    "page %dx%d at a %.1f-degree field offset %.2f degrees needs data"
                            + " to %.2f degrees but coverage ends at %.1f",
                    widthPx, heightPx, state.fieldWidthDegrees(), offset,
                    offset + radius, coverageRadiusDegrees));
        }
        ChartViewport viewport = new ChartViewport(
                state.centre(), state.fieldWidthDegrees(), widthPx, heightPx);
        SkyRegion query = new SkyRegion(state.centre(), radius);
        return new ChartScene(viewport,
                catalogue.starsIn(query),
                catalogue.deepSkyObjectsIn(query),
                title, state.limitingMagnitude());
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
        return centre.separationDegrees(dataCentre)
                + fieldWidthDegrees / 2.0
                + OBJECT_EXTENT_MARGIN_DEGREES
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
        double allowedCornerDegrees = coverageRadiusDegrees
                - OBJECT_EXTENT_MARGIN_DEGREES
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
    static double queryRadiusDegrees(double fieldWidthDegrees, int widthPx, int heightPx) {
        double halfWidthPlane = Math.tan(Math.toRadians(fieldWidthDegrees) / 2.0);
        double halfHeightPlane = halfWidthPlane * heightPx / (double) widthPx;
        double cornerDegrees = Math.toDegrees(
                Math.atan(Math.hypot(halfWidthPlane, halfHeightPlane)));
        return cornerDegrees + OBJECT_EXTENT_MARGIN_DEGREES;
    }
}
