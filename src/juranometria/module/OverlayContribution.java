package juranometria.module;

import java.util.List;

import juranometria.chart.SkyPosition;

/**
 * Geometry a module offers the chart to ink (Sprint 24, issue #215).
 *
 * <p><strong>A module never receives a {@code Graphics2D}.</strong>
 * Handing one out makes the chart a generic canvas, lets a module
 * invent cartography the atlas has not decided, and puts painting
 * policy in two places. So a module contributes typed geometry with
 * an {@link InkRole}, an identity for hit testing and an accessible
 * name - and the chart owns how each role is inked, in what order,
 * and whether it appears in ordinary and reference rendering at all.
 *
 * <p>Positions are given in <strong>sky</strong> coordinates, not
 * pixels. A module that computed pixels would be reimplementing the
 * projection, and would be wrong the moment the page moved.
 */
public sealed interface OverlayContribution {

    /** What this ink is for. */
    InkRole role();

    /** Stable identity, so a reader can point at it. */
    String identity();

    /** What a reader is told this is, in words. */
    String accessibleName();

    /** A single place on the sky. */
    record Point(String identity, String accessibleName, SkyPosition at,
                 InkRole role) implements OverlayContribution {

        public Point {
            requireIdentified(identity, accessibleName, role);
            if (at == null) {
                throw new IllegalArgumentException(
                        "a point is somewhere: " + identity);
            }
        }
    }

    /** An open run of sky positions. */
    record Path(String identity, String accessibleName,
                List<SkyPosition> along, InkRole role)
            implements OverlayContribution {

        public Path {
            requireIdentified(identity, accessibleName, role);
            along = List.copyOf(along);
            if (along.size() < 2) {
                throw new IllegalArgumentException(
                        "a path joins at least two positions: "
                                + identity);
            }
        }
    }

    /** A closed area of sky. */
    record Region(String identity, String accessibleName,
                  List<SkyPosition> boundary, InkRole role)
            implements OverlayContribution {

        public Region {
            requireIdentified(identity, accessibleName, role);
            boundary = List.copyOf(boundary);
            if (boundary.size() < 3) {
                throw new IllegalArgumentException(
                        "a region is bounded by at least three"
                                + " positions: " + identity);
            }
        }
    }

    private static void requireIdentified(String identity,
                                          String accessibleName,
                                          InkRole role) {
        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException(
                    "contributed geometry carries an identity, so a"
                            + " reader can point at it");
        }
        if (accessibleName == null || accessibleName.isBlank()) {
            throw new IllegalArgumentException(
                    "contributed geometry carries an accessible name,"
                            + " so a reader who cannot see it is told"
                            + " what it is: " + identity);
        }
        if (role == null) {
            throw new IllegalArgumentException(
                    "contributed geometry states its ink role: "
                            + identity);
        }
    }
}
