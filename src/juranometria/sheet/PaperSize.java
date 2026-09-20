package juranometria.sheet;

import java.util.Locale;

/**
 * A sheet of paper, in the units paper is sold in (Sprint 29,
 * issue #285, from docs/decisions/printable-chart.md).
 *
 * <p>Everything here is physical. A chart on screen is measured in
 * pixels, which mean whatever the display says they mean; a chart on
 * paper is 271.6 millimetres wide or it is not the chart that was
 * asked for. The sheet owns the page, its orientation, its margins
 * and the rectangle the chart is drawn into, and it states all four
 * in millimetres as well as in the points a vector file speaks.
 *
 * <p><strong>Both papers, always.</strong> The gate declined to guess
 * one from a locale: the reader who asked for this is in Norway and
 * the project is public, so A4 and US Letter are both offered and
 * neither is a default derived from where a machine happens to be.
 *
 * <p>Landscape, because a chart page is wider than it is tall - the
 * atlas's own pages are, and a field width is a horizontal quantity.
 */
public enum PaperSize {

    /** 297 x 210 mm, landscape. */
    A4("A4", 841.89, 595.28),

    /** 11 x 8.5 inches, landscape. */
    LETTER("US Letter", 792.0, 612.0);

    /** Half an inch, the margin the gate chose for both papers. */
    public static final double MARGIN_POINTS = 36.0;

    private static final double POINTS_PER_MM = 72.0 / 25.4;

    private final String identity;
    private final double widePoints;
    private final double highPoints;

    PaperSize(String identity, double widePoints, double highPoints) {
        this.identity = identity;
        this.widePoints = widePoints;
        this.highPoints = highPoints;
    }

    /** What a reader calls this paper. */
    /**
     * What this paper is CALLED, in every language (#350).
     *
     * <p>Owner ruling: {@code A4} and {@code US Letter} are paper
     * identities in this atlas, like {@code SVG} and {@code PDF}. A
     * Norwegian print shop knows both by these names, and a
     * translated or reversed form would name nothing anyone could
     * order. They are handed to sentences as arguments, so a
     * translator is never invited to invent another paper name.
     */
    public String identity() {
        return identity;
    }

    public double widePoints() {
        return widePoints;
    }

    public double highPoints() {
        return highPoints;
    }

    public double wideMm() {
        return widePoints / POINTS_PER_MM;
    }

    public double highMm() {
        return highPoints / POINTS_PER_MM;
    }

    /** The margin on every side, in points. */
    public double marginPoints() {
        return MARGIN_POINTS;
    }

    public double marginMm() {
        return MARGIN_POINTS / POINTS_PER_MM;
    }

    /** The chart's own rectangle inside the margins, in points. */
    public double chartWidePoints() {
        return widePoints - 2 * MARGIN_POINTS;
    }

    public double chartHighPoints() {
        return highPoints - 2 * MARGIN_POINTS;
    }

    public double chartWideMm() {
        return chartWidePoints() / POINTS_PER_MM;
    }

    public double chartHighMm() {
        return chartHighPoints() / POINTS_PER_MM;
    }

    /**
     * The chart rectangle as whole units for a renderer, which thinks
     * in integers. A point is the unit the vector file uses, so the
     * chart is drawn at one unit per point and the file needs no
     * scale factor of its own.
     */
    public int chartWideUnits() {
        return (int) Math.round(chartWidePoints());
    }

    public int chartHighUnits() {
        return (int) Math.round(chartHighPoints());
    }

    /** Points to millimetres, for anything that has to say both. */
    public static double mmOf(double points) {
        return points / POINTS_PER_MM;
    }

    // describe() moved to the language pack (#350). A paper is its
    // identity and its measurements; "landscape", "margins" and
    // "chart" are prose, and this type had no way to say them in
    // anything but English. The numbers are still its own and are
    // handed to the sentence as Locale.ROOT text.

}
