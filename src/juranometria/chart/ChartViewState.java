package juranometria.chart;

/**
 * The interactive view state of the Sprint 2 chart: the current field width
 * and limiting magnitude, always centred on M31.
 *
 * Both values move along small explicit step sequences rather than a
 * continuous range — an honest reflection of the bundled fixture, which is
 * a product promise: the chart may show a smaller field or hide faint
 * stars, but it may never claim deeper or wider coverage than the fixture
 * holds. The Sprint 2 bounds live here and nowhere else: field width 8°
 * (default) down to 1°, limiting magnitude V 8.0 (default) down to V 4.0.
 *
 * States are immutable; transitions return a new state, or {@code this}
 * when already at a bound. The {@code can*} queries let a UI disable an
 * unavailable transition instead of offering a dead control.
 */
public record ChartViewState(double fieldWidthDegrees, double limitingMagnitude) {

    /** Zoom sequence, widest first; zooming in walks toward 1 degree. */
    private static final double[] FIELD_WIDTH_STEPS = {8.0, 6.0, 4.0, 3.0, 2.0, 1.0};

    /** Magnitude-limit sequence, brightest first; fainter walks toward 8. */
    private static final double[] MAGNITUDE_LIMIT_STEPS = {4.0, 5.0, 6.0, 7.0, 8.0};

    /** The Sprint 1 chart: 8-degree field, stars to V 8.0. */
    public static final ChartViewState DEFAULT = new ChartViewState(8.0, 8.0);

    public ChartViewState {
        if (indexOf(FIELD_WIDTH_STEPS, fieldWidthDegrees) < 0) {
            throw new IllegalArgumentException(
                    "field width is not a supported step: " + fieldWidthDegrees);
        }
        if (indexOf(MAGNITUDE_LIMIT_STEPS, limitingMagnitude) < 0) {
            throw new IllegalArgumentException(
                    "limiting magnitude is not a supported step: " + limitingMagnitude);
        }
    }

    public boolean canZoomIn() {
        return fieldWidthIndex() < FIELD_WIDTH_STEPS.length - 1;
    }

    public boolean canZoomOut() {
        return fieldWidthIndex() > 0;
    }

    /** True when the limit can move brighter (show fewer stars). */
    public boolean canDecreaseMagnitudeLimit() {
        return magnitudeIndex() > 0;
    }

    /** True when the limit can move fainter (show more stars). */
    public boolean canIncreaseMagnitudeLimit() {
        return magnitudeIndex() < MAGNITUDE_LIMIT_STEPS.length - 1;
    }

    /** The next narrower field, or this state at the 1-degree bound. */
    public ChartViewState zoomIn() {
        return canZoomIn()
                ? new ChartViewState(FIELD_WIDTH_STEPS[fieldWidthIndex() + 1], limitingMagnitude)
                : this;
    }

    /** The next wider field, or this state at the 8-degree bound. */
    public ChartViewState zoomOut() {
        return canZoomOut()
                ? new ChartViewState(FIELD_WIDTH_STEPS[fieldWidthIndex() - 1], limitingMagnitude)
                : this;
    }

    /** A brighter limit (fewer stars), or this state at V 4.0. */
    public ChartViewState decreaseMagnitudeLimit() {
        return canDecreaseMagnitudeLimit()
                ? new ChartViewState(fieldWidthDegrees, MAGNITUDE_LIMIT_STEPS[magnitudeIndex() - 1])
                : this;
    }

    /** A fainter limit (more stars), or this state at V 8.0. */
    public ChartViewState increaseMagnitudeLimit() {
        return canIncreaseMagnitudeLimit()
                ? new ChartViewState(fieldWidthDegrees, MAGNITUDE_LIMIT_STEPS[magnitudeIndex() + 1])
                : this;
    }

    /** The complete default state: 8-degree field, stars to V 8.0. */
    public ChartViewState reset() {
        return DEFAULT;
    }

    private int fieldWidthIndex() {
        return indexOf(FIELD_WIDTH_STEPS, fieldWidthDegrees);
    }

    private int magnitudeIndex() {
        return indexOf(MAGNITUDE_LIMIT_STEPS, limitingMagnitude);
    }

    private static int indexOf(double[] steps, double value) {
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
