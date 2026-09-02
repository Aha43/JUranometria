package juranometria.module;

import juranometria.chart.SkyPosition;

/**
 * A module asking the chart to move (Sprint 24, issue #215).
 *
 * <p>Deliberate and explicit, because the alternative is a module
 * that moves the chart as a side effect of a reader looking at
 * something - which is how a panel starts stealing the page. The
 * chart decides whether to honour it; a request is not a command.
 */
public record NavigationRequest(SkyPosition centre, Double fieldWidthDegrees,
                                String because) {

    public NavigationRequest {
        if (centre == null) {
            throw new IllegalArgumentException(
                    "a navigation request always names where to go");
        }
        if (because == null || because.isBlank()) {
            throw new IllegalArgumentException(
                    "a request says why, so a reader can be told what"
                            + " moved their page");
        }
        if (fieldWidthDegrees != null && !(fieldWidthDegrees > 0)) {
            throw new IllegalArgumentException(
                    "a field width is positive or absent: "
                            + fieldWidthDegrees);
        }
    }

    /** Go here, keeping the field the reader chose. */
    public static NavigationRequest centreOn(SkyPosition where,
                                             String because) {
        return new NavigationRequest(where, null, because);
    }
}
