package juranometria.project;

import juranometria.chart.ChartViewport;

/**
 * Maps tangent-plane coordinates to pixel coordinates for a viewport,
 * preserving the atlas convention: north is up and east is left.
 *
 * The viewport's field width is the horizontal angular extent of the chart,
 * so the plane interval [-tan(fieldWidth/2), +tan(fieldWidth/2)] spans the
 * pixel width. The same scale applies vertically, keeping the chart square
 * on the sky.
 */
public final class ViewportMapping {

    private final double centreX;
    private final double centreY;
    private final double pixelsPerPlaneUnit;

    public ViewportMapping(ChartViewport viewport) {
        double halfFieldRadians = Math.toRadians(viewport.fieldWidthDegrees()) / 2.0;
        if (halfFieldRadians >= Math.PI / 2.0) {
            throw new IllegalArgumentException(
                    "field width must be below 180 degrees for a gnomonic chart: "
                            + viewport.fieldWidthDegrees());
        }
        this.centreX = viewport.widthPx() / 2.0;
        this.centreY = viewport.heightPx() / 2.0;
        this.pixelsPerPlaneUnit = viewport.widthPx() / (2.0 * Math.tan(halfFieldRadians));
    }

    /** Converts a tangent-plane point to pixels; east left, north up. */
    public PixelPoint toPixel(PlanePoint point) {
        return new PixelPoint(
                centreX - point.xiEast() * pixelsPerPlaneUnit,
                centreY - point.etaNorth() * pixelsPerPlaneUnit);
    }
}
