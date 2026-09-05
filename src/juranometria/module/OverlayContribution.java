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

    /**
     * What a reference line <em>is</em>, so the chart can decide
     * what it looks like.
     *
     * <p>Not an appearance. A module that said "dashed" would be
     * inking, which is the chart's decision and not its own; what it
     * says instead is whether the line is drawn through the sky or
     * bounds what can be seen of it, and the chart draws the second
     * one dashed because a boundary of visibility is not a thing in
     * the sky.
     *
     * <p>The gate said the seam needed exactly one new thing, the
     * great circle. It needs this too, and it is worth saying why
     * rather than quietly adding it: the ink was decided as solid
     * for the meridian and dashed for the horizon, and the only
     * other way for the chart to tell those apart is to know what a
     * horizon is - which is the one thing the seam exists to
     * prevent. So the distinction is carried, in the module's
     * vocabulary rather than the chart's.
     */
    enum Reference {

        /** A line drawn across the sky. */
        LINE,

        /** The boundary of what can be seen of it. */
        BOUNDARY,

        /**
         * A permanent circle of the celestial sphere: true for every
         * observer and every date.
         *
         * <p>The Sprint 28 gate's one addition to this vocabulary
         * (docs/decisions/ecliptic.md). A meridian belongs to a place
         * and a moment; a horizon bounds what one observer can see;
         * this belongs to the frame itself and to nobody. Drawing it
         * in the meridian's stroke made the two indistinguishable on
         * a page carrying both, which the gate's candidate pages
         * showed.
         *
         * <p>Like the other two, it says what the geometry <em>is</em>
         * and never what it looks like. It is deliberately not named
         * for the ecliptic: a module drawing the galactic equator
         * would say exactly this.
         */
        PERMANENT
    }

    /**
     * What a contributed point <em>is</em>, so the chart can decide
     * what it looks like.
     *
     * <p>The Sprint 28 gate's second addition, and it exists for the
     * same reason as {@link Reference}: the chart drew every
     * reference point as the zenith's ring and upward tick, and the
     * tick means <em>overhead</em>. An equinox is not overhead. A
     * shared Java type did not make that cartographic meaning
     * generic, and only the module knows which kind it is offering.
     *
     * <p>Consulted only for {@link InkRole#REFERENCE_LINE}. A working
     * mark is inked by its role, and its kind says nothing.
     */
    enum Mark {

        /**
         * A place an observer stands under or faces: it has an up.
         * The zenith is one; nothing else so far is.
         */
        PLACE,

        /**
         * A distinguished position <em>on</em> a reference line, with
         * no orientation of its own - an equinox, a solstice, or the
         * galactic centre on a galactic equator.
         */
        LANDMARK
    }

    /**
     * A great circle, given by its pole.
     *
     * <p>The one new geometry the gate named. A pole and nothing
     * else: the chart clips it to the page analytically, because a
     * gnomonic projection maps every great circle to a straight
     * line, and a polyline could not answer a page that lies between
     * its own vertices. It knows nothing of meridians, horizons,
     * observers or time, and a module drawing a galactic equator
     * would use this same type.
     */
    record GreatCircle(String identity, String accessibleName,
                       SkyPosition pole, Reference reference, InkRole role)
            implements OverlayContribution {

        public GreatCircle {
            requireIdentified(identity, accessibleName, role);
            if (pole == null) {
                throw new IllegalArgumentException(
                        "a great circle is given by its pole: "
                                + identity);
            }
            if (reference == null) {
                throw new IllegalArgumentException(
                        "a reference line says whether it crosses the"
                                + " sky or bounds it: " + identity);
            }
        }
    }

    /** A single place on the sky. */
    record Point(String identity, String accessibleName, SkyPosition at,
                 Mark mark, InkRole role) implements OverlayContribution {

        public Point {
            requireIdentified(identity, accessibleName, role);
            if (at == null) {
                throw new IllegalArgumentException(
                        "a point is somewhere: " + identity);
            }
            if (mark == null) {
                throw new IllegalArgumentException(
                        "a point says what kind of place it is: "
                                + identity);
            }
        }

        /**
         * A point whose kind is a {@link Mark#PLACE}.
         *
         * <p>For the working-mark role, where the kind is not
         * consulted at all: a mark a reader put on the chart is inked
         * by its role, and asking every caller to classify it would
         * be ceremony for a question the chart does not ask. The
         * reference role states its kind explicitly.
         */
        public Point(String identity, String accessibleName,
                     SkyPosition at, InkRole role) {
            this(identity, accessibleName, at, Mark.PLACE, role);
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
