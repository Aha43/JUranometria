package juranometria.chart;

/**
 * The interactive view state of the chart: the celestial centre, the
 * current field width, and the limiting magnitude.
 *
 * Field width and limiting magnitude move along small explicit step
 * sequences rather than a continuous range — an honest reflection of the
 * bundled data, which is a product promise: the chart may show a smaller
 * field or hide faint stars, but it may never claim deeper or wider
 * coverage than the data holds. The bounds live here and nowhere else:
 * field width 8° (default) down to 1°, limiting magnitude V 8.0 (default)
 * down to V 4.0.
 *
 * The centre is a free sky position; whether a centre/field combination
 * fits inside the bundled data's coverage is the scene assembler's rule,
 * not the state's. The default centre is M31 — the same position as the
 * data cone's centre, though the two are distinct concepts.
 *
 * States are immutable; transitions return a new state, or {@code this}
 * when already at a bound. The {@code can*} queries let a UI disable an
 * unavailable transition instead of offering a dead control.
 */
public record ChartViewState(SkyPosition centre, double fieldWidthDegrees,
                             double limitingMagnitude) {

    /** Zoom sequence, widest first; zooming in walks toward 1 degree. */
    private static final double[] FIELD_WIDTH_STEPS = {8.0, 6.0, 4.0, 3.0, 2.0, 1.0};

    /** Magnitude-limit sequence, brightest first; fainter walks toward 8. */
    private static final double[] MAGNITUDE_LIMIT_STEPS = {4.0, 5.0, 6.0, 7.0, 8.0};

    /** The Sprint 1 chart: M31, 8-degree field, stars to V 8.0. */
    public static final ChartViewState DEFAULT =
            new ChartViewState(new SkyPosition(10.684708, 41.268750), 8.0, 8.0);

    public ChartViewState {
        if (centre == null) {
            throw new IllegalArgumentException("centre must not be null");
        }
        if (indexOf(FIELD_WIDTH_STEPS, fieldWidthDegrees) < 0) {
            throw new IllegalArgumentException(
                    "field width is not a supported step: " + fieldWidthDegrees);
        }
        if (indexOf(MAGNITUDE_LIMIT_STEPS, limitingMagnitude) < 0) {
            throw new IllegalArgumentException(
                    "limiting magnitude is not a supported step: " + limitingMagnitude);
        }
    }

    /** The supported field widths, widest first, for coverage decisions. */
    public static java.util.List<Double> fieldWidthSteps() {
        return java.util.stream.DoubleStream.of(FIELD_WIDTH_STEPS).boxed().toList();
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
                ? new ChartViewState(centre,
                        FIELD_WIDTH_STEPS[fieldWidthIndex() + 1], limitingMagnitude)
                : this;
    }

    /** The next wider field, or this state at the 8-degree bound. */
    public ChartViewState zoomOut() {
        return canZoomOut()
                ? new ChartViewState(centre,
                        FIELD_WIDTH_STEPS[fieldWidthIndex() - 1], limitingMagnitude)
                : this;
    }

    /** A brighter limit (fewer stars), or this state at V 4.0. */
    public ChartViewState decreaseMagnitudeLimit() {
        return canDecreaseMagnitudeLimit()
                ? new ChartViewState(centre, fieldWidthDegrees,
                        MAGNITUDE_LIMIT_STEPS[magnitudeIndex() - 1])
                : this;
    }

    /** A fainter limit (more stars), or this state at V 8.0. */
    public ChartViewState increaseMagnitudeLimit() {
        return canIncreaseMagnitudeLimit()
                ? new ChartViewState(centre, fieldWidthDegrees,
                        MAGNITUDE_LIMIT_STEPS[magnitudeIndex() + 1])
                : this;
    }

    /** The same field and limit centred on a new position. */
    public ChartViewState recenteredAt(SkyPosition newCentre) {
        if (newCentre == null) {
            throw new IllegalArgumentException("centre must not be null");
        }
        return new ChartViewState(newCentre, fieldWidthDegrees, limitingMagnitude);
    }

    /** This centre and limit at another supported field width. */
    public ChartViewState withFieldWidth(double newFieldWidthDegrees) {
        return new ChartViewState(centre, newFieldWidthDegrees, limitingMagnitude);
    }

    /** The complete default state: M31, 8-degree field, stars to V 8.0. */
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
