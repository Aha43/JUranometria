package juranometria.ui;

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
 * the UI boundary, never inside painting, and a future recentring or
 * search feature changes what is asked for here without touching the
 * renderer.
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
    private final SkyPosition centre;
    private final String title;
    private final double coverageRadiusDegrees;

    /**
     * @param coverageRadiusDegrees the bundled data's cone radius around
     *     the centre; the declared coverage rule is that the visible
     *     corners plus the object-extent margin never exceed it.
     */
    public SceneAssembler(Catalogue catalogue, SkyPosition centre, String title,
                          double coverageRadiusDegrees) {
        if (catalogue == null || centre == null || title == null) {
            throw new IllegalArgumentException("catalogue, centre, and title are required");
        }
        if (!(coverageRadiusDegrees > OBJECT_EXTENT_MARGIN_DEGREES)) {
            throw new IllegalArgumentException(
                    "coverage must exceed the object margin: " + coverageRadiusDegrees);
        }
        this.catalogue = catalogue;
        this.centre = centre;
        this.title = title;
        this.coverageRadiusDegrees = coverageRadiusDegrees;
    }

    /**
     * Queries the catalogue and builds the scene for one page. The page
     * dimensions must respect {@link #maxPageHeightPx}; a query that would
     * reach beyond the bundled coverage is an error, never a silently
     * sparse chart.
     */
    public ChartScene assemble(ChartViewState state, int widthPx, int heightPx) {
        double radius = queryRadiusDegrees(state.fieldWidthDegrees(), widthPx, heightPx);
        if (radius > coverageRadiusDegrees) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    "page %dx%d at a %.1f-degree field needs %.2f degrees of data"
                            + " but coverage ends at %.1f",
                    widthPx, heightPx, state.fieldWidthDegrees(), radius,
                    coverageRadiusDegrees));
        }
        ChartViewport viewport =
                new ChartViewport(centre, state.fieldWidthDegrees(), widthPx, heightPx);
        SkyRegion query = new SkyRegion(centre, radius);
        return new ChartScene(viewport,
                catalogue.starsIn(query),
                catalogue.deepSkyObjectsIn(query),
                title, state.limitingMagnitude());
    }

    /**
     * The tallest page (in pixels) whose corners, plus the object-extent
     * margin, stay inside the bundled coverage at this field width. A
     * taller window letterboxes the page rather than promising sky the
     * data does not hold.
     */
    public int maxPageHeightPx(double fieldWidthDegrees, int widthPx) {
        double halfWidthPlane = Math.tan(Math.toRadians(fieldWidthDegrees) / 2.0);
        double maxCornerPlane = Math.tan(Math.toRadians(
                coverageRadiusDegrees - OBJECT_EXTENT_MARGIN_DEGREES));
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
