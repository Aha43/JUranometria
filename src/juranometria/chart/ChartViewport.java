package juranometria.chart;

/**
 * An immutable chart viewport: the sky position at the chart centre, the
 * horizontal angular extent of the chart in degrees, and the pixel
 * dimensions of the drawing surface.
 *
 * The viewport carries no projection knowledge; upper bounds on the field
 * width are left to projection work.
 */
public record ChartViewport(SkyPosition centre, double fieldWidthDegrees,
                            int widthPx, int heightPx) {

    public ChartViewport {
        if (centre == null) {
            throw new IllegalArgumentException("centre must not be null");
        }
        if (!(fieldWidthDegrees > 0.0) || !Double.isFinite(fieldWidthDegrees)) {
            throw new IllegalArgumentException(
                    "field width must be positive and finite degrees: " + fieldWidthDegrees);
        }
        if (widthPx <= 0 || heightPx <= 0) {
            throw new IllegalArgumentException(
                    "pixel dimensions must be positive: " + widthPx + "x" + heightPx);
        }
    }
}
