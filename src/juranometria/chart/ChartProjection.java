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
    STEREOGRAPHIC("stereographic");

    private final String displayName;

    ChartProjection(String displayName) {
        this.displayName = displayName;
    }

    /** What a title block, an export and a decision document call it. */
    public String displayName() {
        return displayName;
    }
}
