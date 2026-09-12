package juranometria.chart;

/**
 * Which projection a chart is drawn by (Sprint 30, issue #297).
 *
 * <p>Part of the chart's own immutable state, not a setting hidden in
 * a surface: two charts of the same centre and field drawn by
 * different projections are different pages, and a page that could
 * not say which one it was would be a page nobody could check.
 *
 * <p>A name only. What each one does with the sky lives in
 * {@code juranometria.project}, and nothing outside that package
 * switches on which of these it holds - a chart asks its projection
 * for an answer rather than asking which projection it is.
 */
public enum ChartProjection {

    /**
     * The atlas's own, and the right one for the fields it serves:
     * every great circle straight, every telescope field where the
     * telescope will find it.
     */
    GNOMONIC("gnomonic"),

    /**
     * The overview, chosen by Sprint 30's gate because shape survives
     * it and nothing else does
     * (docs/decisions/overview-projection.md).
     */
    STEREOGRAPHIC("stereographic"),

    /**
     * The celestial globe, and the only one of the three with a
     * horizon: one hemisphere, ending at a real limb
     * (docs/decisions/celestial-globe.md). It draws the widest rung
     * and nothing else.
     */
    ORTHOGRAPHIC("orthographic");

    private final String displayName;

    ChartProjection(String displayName) {
        this.displayName = displayName;
    }

    /** What a title block, an export and a decision document call it. */
    public String displayName() {
        return displayName;
    }

    /**
     * The widest field the atlas's own projection draws.
     *
     * <p>The 42-degree sheet page, which is where the tangent plane's
     * corner distortion stops being a price worth paying
     * (docs/decisions/printable-chart.md). Everything wider is the
     * overview's.
     */
    public static final double WIDEST_TANGENT_FIELD_DEGREES = 42.0;

    /**
     * Which projection draws a page of this width.
     *
     * <p>A property of the field, not a setting: there is no
     * projection menu, now or later, because a reader who wanted "the
     * stereographic view" would be a reader who had been told about a
     * problem they do not have
     * (docs/decisions/overview-projection.md). The overview is the
     * field ladder continued, and the projection changes with the
     * rung because past 42 degrees the tangent plane has nothing left
     * to offer - at a 90-degree corner it is 61 per cent out of
     * shape, and it cannot reach 90 degrees from its centre at any
     * price.
     *
     * <p>Sprint 30, issue #299. Issue #297 built the two projections
     * and deliberately left them uncoupled, because the rungs that
     * need the second one did not exist yet.
     */
    public static ChartProjection forField(double fieldWidthDegrees) {
        if (fieldWidthDegrees >= WHOLE_HEMISPHERE_DEGREES) {
            return ORTHOGRAPHIC;
        }
        return fieldWidthDegrees > WIDEST_TANGENT_FIELD_DEGREES
                ? STEREOGRAPHIC : GNOMONIC;
    }

    /**
     * The field that is a whole hemisphere, and the only one the
     * globe draws.
     *
     * <p>Not "wide enough for the globe" but exactly a hemisphere: a
     * hundred and seventy-nine degrees is a stereographic page that
     * happens to be very wide, and a hundred and eighty is a sphere
     * seen from outside. The projection changes because the page
     * becomes a different kind of thing, not because a threshold was
     * crossed.
     */
    public static final double WHOLE_HEMISPHERE_DEGREES = 180.0;
}
