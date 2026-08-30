package juranometria.render;

import juranometria.chart.Star;
import juranometria.chart.StarIdentity;

/**
 * The reviewed star-label scale policy (docs/decisions/
 * star-identity.md), option-free like every policy class: which
 * identity form labels a star at the page's field width. Priority per
 * star is proper name, then Bayer, then Flamsteed; Greek letters and
 * component digits render as themselves; magnitude limits per band:
 *
 *   24-36 degrees: names V <= 2.5 only;
 *   12-18 degrees: names and Bayer V <= 3.0;
 *   <= 8 degrees:  names and Bayer V <= 4.5, Flamsteed V <= 5.0.
 */
public final class StarLabelPolicy {

    private final double fieldWidthDegrees;

    public StarLabelPolicy(double fieldWidthDegrees) {
        this.fieldWidthDegrees = fieldWidthDegrees;
    }

    /** The text the policy labels this star with, or null: none. */
    public String labelFor(Star star) {
        StarIdentity identity = star.identity();
        if (identity == null) {
            return null;
        }
        if (identity.name() != null && star.magnitude() <= nameLimit()) {
            return identity.name();
        }
        if (identity.bayer() != null && star.magnitude() <= bayerLimit()) {
            return identity.bayer();
        }
        if (identity.flamsteed() != null
                && star.magnitude() <= flamsteedLimit()) {
            return identity.flamsteed();
        }
        return null;
    }

    /**
     * The searched star's guaranteed label: its best identity form,
     * exempt from every magnitude threshold (the same shape as the
     * deep-sky target exemption). Null when the star has no
     * designation to show.
     */
    public static String guaranteedLabelFor(Star star) {
        StarIdentity identity = star.identity();
        if (identity == null) {
            return null;
        }
        if (identity.name() != null) {
            return identity.name();
        }
        if (identity.bayer() != null) {
            return identity.bayer();
        }
        return identity.flamsteed();
    }

    double nameLimit() {
        // 2.5, not 2.0: the reviewed adjustment (PR #120) - the
        // 2.0-2.5 magnitude band holds the sky's asterism anchors
        // (Alpheratz, Mirach, Dubhe, Mizar...), and a figure whose
        // principal stars stay anonymous is less useful cartography.
        return fieldWidthDegrees >= 24.0 ? 2.5
                : fieldWidthDegrees >= 12.0 ? 3.0 : 4.5;
    }

    double bayerLimit() {
        return fieldWidthDegrees >= 24.0 ? Double.NEGATIVE_INFINITY
                : fieldWidthDegrees >= 12.0 ? 3.0 : 4.5;
    }

    double flamsteedLimit() {
        return fieldWidthDegrees >= 12.0 ? Double.NEGATIVE_INFINITY : 5.0;
    }
}
