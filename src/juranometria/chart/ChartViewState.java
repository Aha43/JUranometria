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
 * field width 120° down to 1°, 8° the default, limiting magnitude V 8.0
 * (default) down to V 4.0.
 *
 * Which projection draws a rung is part of the same rule and lives here
 * too: the three widest are the overview's, everything from 42° down is
 * the atlas's own, and a state may not disagree with its own field
 * (Sprint 30, issue #299).
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
                             double limitingMagnitude, String targetLabel,
                             String targetIdentity,
                             ChartProjection projection) {

    /** Zoom sequence, widest first; zooming in walks toward 1 degree.
     *  The regional steps above 8 come from docs/decisions/regional-zoom.md;
     *  the 42-degree sheet step comes from
     *  docs/decisions/printable-chart.md, which measured the distortion
     *  budget that admits it and excludes anything wider *for the
     *  tangent plane*; the three overview rungs above it come from
     *  docs/decisions/overview-projection.md, which measured that a
     *  different projection carries them and that 180 degrees is
     *  drawable but not readable. */
    private static final double[] FIELD_WIDTH_STEPS =
            {120.0, 90.0, 60.0,
             42.0, 36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0};

    /** Magnitude-limit sequence, brightest first; fainter walks toward 8. */
    private static final double[] MAGNITUDE_LIMIT_STEPS = {4.0, 5.0, 6.0, 7.0, 8.0};

    /** The Sprint 1 chart: M31, 8-degree field, stars to V 8.0. */
    public static final ChartViewState DEFAULT = new ChartViewState(
            new SkyPosition(10.684708, 41.268750), 8.0, 8.0,
            "M31 \u00b7 Andromeda Galaxy region", "NGC 224");

    /** A view without a named target; its title is its coordinates. */
    public ChartViewState(SkyPosition centre, double fieldWidthDegrees,
                          double limitingMagnitude) {
        this(centre, fieldWidthDegrees, limitingMagnitude, null, null);
    }

    /**
     * A view drawn by its own field's projection.
     *
     * <p>Every state is one of these, and says so by omission rather
     * than by repeating in a hundred places a word that the field
     * already determines. It named the gnomonic projection until
     * #299 put rungs on the ladder that another one draws; nothing
     * that called it had made a choice, and now nothing can.
     */
    public ChartViewState(SkyPosition centre, double fieldWidthDegrees,
                          double limitingMagnitude, String targetLabel,
                          String targetIdentity) {
        this(centre, fieldWidthDegrees, limitingMagnitude, targetLabel,
                targetIdentity, ChartProjection.forField(fieldWidthDegrees));
    }

    public ChartViewState {
        if (centre == null) {
            throw new IllegalArgumentException("centre must not be null");
        }
        if (projection == null) {
            throw new IllegalArgumentException(
                    "projection must not be null");
        }
        if (indexOf(FIELD_WIDTH_STEPS, fieldWidthDegrees) < 0) {
            throw new IllegalArgumentException(
                    "field width is not a supported step: " + fieldWidthDegrees);
        }
        // Which projection draws which rung is a property of the
        // field and not a setting, so a state that disagrees with its
        // own field is not a state the atlas can be in. Holding it
        // here rather than in the surfaces means there is nowhere to
        // forget it: a page drawn by the wrong projection is centred
        // where the reader left it, carries the field they chose, and
        // is silently the wrong sky (docs/decisions/overview-projection.md).
        if (projection != ChartProjection.forField(fieldWidthDegrees)) {
            throw new IllegalArgumentException(String.format(
                    java.util.Locale.ROOT,
                    "a %.0f-degree page is drawn by the %s projection and"
                            + " this one says %s: which projection draws a"
                            + " field is not a choice",
                    fieldWidthDegrees,
                    ChartProjection.forField(fieldWidthDegrees).displayName(),
                    projection.displayName()));
        }
        if (indexOf(MAGNITUDE_LIMIT_STEPS, limitingMagnitude) < 0) {
            throw new IllegalArgumentException(
                    "limiting magnitude is not a supported step: " + limitingMagnitude);
        }
        if (targetLabel != null && targetLabel.isBlank()) {
            throw new IllegalArgumentException(
                    "target label must be null (no target) or non-blank");
        }
        if (targetIdentity != null && targetIdentity.isBlank()) {
            throw new IllegalArgumentException(
                    "target identity must be null (no target) or non-blank");
        }
        // A catalogue target is atomic: label and identity together, or
        // neither (PR #59 review) - a chart may never name a target whose
        // identity the rendering policy cannot preserve.
        if ((targetLabel == null) != (targetIdentity == null)) {
            throw new IllegalArgumentException(
                    "target label and identity must both be present or both absent");
        }
    }

    /**
     * The limiting magnitude a page of this width arrives at.
     *
     * <p>Provisional numbers from the projection gate, confirmed or
     * revised by {@code juranometria.tool.OverviewInkStudyMain} -
     * the gate could not settle them, because its pages carried no
     * star labels and used production's stroke policy nowhere, and
     * it said so and required this issue to measure them
     * (docs/decisions/overview-projection.md).
     *
     * <p>This decides where a page <em>starts</em>. The reader's own
     * magnitude control is unchanged and still wins.
     */
    public static double defaultMagnitudeFor(double fieldWidthDegrees) {
        if (fieldWidthDegrees >= 90.0) {
            return 4.0;
        }
        if (fieldWidthDegrees >= 60.0) {
            return 5.0;
        }
        return DEFAULT.limitingMagnitude();
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

    // Every transition that keeps the field carries the projection
    // through, and every transition that changes the field takes the
    // new field's. Both are the same rule read twice: the projection
    // belongs to the field. Issue #297 wrote these to carry it
    // because there was nothing yet to derive it from, and a review
    // found what happens when they do not - a page still centred
    // where the reader left it, still at the field they chose, and
    // silently the wrong sky.

    /** The next narrower field, or this state at the 1-degree bound. */
    public ChartViewState zoomIn() {
        return canZoomIn()
                ? withFieldWidth(FIELD_WIDTH_STEPS[fieldWidthIndex() + 1])
                : this;
    }

    /** The next wider field, or this state at the 42-degree bound. */
    public ChartViewState zoomOut() {
        return canZoomOut()
                ? withFieldWidth(FIELD_WIDTH_STEPS[fieldWidthIndex() - 1])
                : this;
    }

    /** A brighter limit (fewer stars), or this state at V 4.0. */
    public ChartViewState decreaseMagnitudeLimit() {
        return canDecreaseMagnitudeLimit()
                ? new ChartViewState(centre, fieldWidthDegrees,
                        MAGNITUDE_LIMIT_STEPS[magnitudeIndex() - 1], targetLabel,
                        targetIdentity, projection)
                : this;
    }

    /** A fainter limit (more stars), or this state at V 8.0. */
    public ChartViewState increaseMagnitudeLimit() {
        return canIncreaseMagnitudeLimit()
                ? new ChartViewState(centre, fieldWidthDegrees,
                        MAGNITUDE_LIMIT_STEPS[magnitudeIndex() + 1], targetLabel,
                        targetIdentity, projection)
                : this;
    }

    /**
     * The same field and limit centred on a new position without a named
     * target; the chart titles itself by its coordinates.
     */
    public ChartViewState recenteredAt(SkyPosition newCentre) {
        return recenteredAt(newCentre, null, null);
    }

    /**
     * Recentres on a named target: the label titles the chart, and the
     * stable catalogue identity keeps the target drawn and labelled at
     * regional fields (docs/decisions/regional-zoom.md).
     */
    public ChartViewState recenteredAt(SkyPosition newCentre, String newTargetLabel,
                                       String newTargetIdentity) {
        if (newCentre == null) {
            throw new IllegalArgumentException("centre must not be null");
        }
        return new ChartViewState(newCentre, fieldWidthDegrees, limitingMagnitude,
                newTargetLabel, newTargetIdentity, projection);
    }

    /**
     * This centre, target and limit at another supported field width,
     * drawn by whichever projection that width belongs to.
     *
     * <p>The projection changes with the rung and says nothing about
     * it, because nothing a reader chose is lost: the centre and the
     * field carry across, and the page they asked for is the page
     * they get. A reader who had to be told which projection was
     * drawing would be a reader being told about a problem they do
     * not have.
     *
     * <p>The limit comes with the rung too, and only ever brighter.
     * A 120-degree page at the atlas's own V 8.0 is <strong>47.9 per
     * cent ink</strong> - half the paper marked, with no shapes left
     * to read - against 13.7 per cent at the limit its field arrives
     * with, which is the released sheet page's own 13.1 per cent
     * (docs/studies/overview-ink/measurements.md). So a wider rung
     * takes its field's limit when that is brighter, and a narrower
     * one keeps what the reader has: this can hide a faint star, and
     * can never bring back one they chose to hide.
     *
     * <p>It does not remember. Zooming out to the overview and back
     * leaves the brighter limit, and the reader's own control - or
     * Home - restores it. Nothing here keeps a second copy of a
     * choice the reader can see and change.
     */
    public ChartViewState withFieldWidth(double newFieldWidthDegrees) {
        return new ChartViewState(centre, newFieldWidthDegrees,
                Math.min(limitingMagnitude,
                        defaultMagnitudeFor(newFieldWidthDegrees)),
                targetLabel, targetIdentity,
                ChartProjection.forField(newFieldWidthDegrees));
    }

    /** Whether this page is one of the overview's wide rungs. */
    public boolean overview() {
        return projection != ChartProjection.GNOMONIC;
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
