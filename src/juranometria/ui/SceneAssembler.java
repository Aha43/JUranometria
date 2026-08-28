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

    public SceneAssembler(Catalogue catalogue, SkyPosition centre, String title) {
        if (catalogue == null || centre == null || title == null) {
            throw new IllegalArgumentException("catalogue, centre, and title are required");
        }
        this.catalogue = catalogue;
        this.centre = centre;
        this.title = title;
    }

    /** Queries the catalogue and builds the scene for one view. */
    public ChartScene assemble(ChartViewState state, int widthPx, int heightPx) {
        ChartViewport viewport =
                new ChartViewport(centre, state.fieldWidthDegrees(), widthPx, heightPx);
        SkyRegion query = new SkyRegion(centre, queryRadiusDegrees(
                state.fieldWidthDegrees(), widthPx, heightPx));
        return new ChartScene(viewport,
                catalogue.starsIn(query),
                catalogue.deepSkyObjectsIn(query),
                title, state.limitingMagnitude());
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
