package juranometria.project;

import juranometria.chart.ChartViewport;

/**
 * Maps tangent-plane coordinates to pixel coordinates for a viewport,
 * preserving the atlas convention: north is up and east is left.
 *
 * The viewport's field width is the horizontal angular extent of the
 * chart: <strong>half the page holds half the field</strong>. The
 * tangent that used to be written here was one projection's answer to
 * how far out that is, inlined - which is also why this class used to
 * refuse a field of 180 degrees or more. That was never about pages;
 * it was about {@code tan}. The projection is asked now, and says.
 */
public final class ViewportMapping {

    private final double centreX;
    private final double centreY;
    private final double pixelsPerPlaneUnit;

    public ViewportMapping(ChartViewport viewport) {
        this(viewport, Projections.forViewport(viewport));
    }

    /**
     * Private, and it matters that it is.
     *
     * <p>A mapping and the projection it maps by must be the same
     * projection about the same centre, and there is no way to say
     * that with two arguments except by trusting the caller - a
     * review pointed out that this overload let a caller hand in a
     * projection disagreeing with the viewport in either. There is
     * one viewport, it names one projection, and this asks it.
     */
    private ViewportMapping(ChartViewport viewport,
                            Projection projection) {
        double half = viewport.fieldWidthDegrees() / 2.0;
        if (half >= projection.limitDegrees()) {
            throw new IllegalArgumentException(
                    "field width reaches past what the "
                            + projection.name() + " projection shows: "
                            + viewport.fieldWidthDegrees());
        }
        this.centreX = viewport.widthPx() / 2.0;
        this.centreY = viewport.heightPx() / 2.0;
        this.pixelsPerPlaneUnit =
                viewport.widthPx() / (2.0 * projection.planeRadius(half));
    }

    /** Pixels per tangent-plane unit; multiply an angle in radians to get
     *  its small-angle pixel extent at the chart centre. */
    public double pixelsPerPlaneUnit() {
        return pixelsPerPlaneUnit;
    }

    /** Converts a tangent-plane point to pixels; east left, north up. */
    public PixelPoint toPixel(PlanePoint point) {
        return new PixelPoint(
                centreX - point.xiEast() * pixelsPerPlaneUnit,
                centreY - point.etaNorth() * pixelsPerPlaneUnit);
    }
}
