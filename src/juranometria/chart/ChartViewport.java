package juranometria.chart;

/**
 * An immutable chart viewport: the sky position at the chart centre, the
 * horizontal angular extent of the chart in degrees, and the pixel
 * dimensions of the drawing surface.
 *
 * The viewport says which projection draws it (Sprint 30, issue #297).
 * It says nothing about what that projection does: it carries a name,
 * and {@code juranometria.project} turns the name into an answer. Upper
 * bounds on the field width belong to the projection, which knows how
 * far it reaches.
 */
public record ChartViewport(SkyPosition centre, double fieldWidthDegrees,
                            int widthPx, int heightPx,
                            ChartProjection projection) {

    /** A viewport drawn by the atlas's own projection. */
    public ChartViewport(SkyPosition centre, double fieldWidthDegrees,
                         int widthPx, int heightPx) {
        this(centre, fieldWidthDegrees, widthPx, heightPx,
                ChartProjection.GNOMONIC);
    }

    public ChartViewport {
        if (projection == null) {
            throw new IllegalArgumentException(
                    "projection must not be null");
        }
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
