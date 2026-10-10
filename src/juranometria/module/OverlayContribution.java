package juranometria.module;

import java.util.List;

import juranometria.chart.Cardinal;
import juranometria.chart.SkyPosition;

/**
 * Geometry a module offers the chart to ink (Sprint 24, issue #215).
 *
 * <p><strong>A module never receives a {@code Graphics2D}.</strong>
 * Handing one out makes the chart a generic canvas, lets a module
 * invent cartography the atlas has not decided, and puts painting
 * policy in two places. So a module contributes typed geometry with
 * an {@link InkRole}, an identity for hit testing and - where the
 * geometry carries one - a reader-facing name; a variant whose words
 * belong to the page's language, such as a cardinal direction,
 * carries no name at all and is resolved by type where language
 * lives. The chart owns how each role is inked, in what order,
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
         * A point in a role that does not consult its kind.
         *
         * <p>For the working-mark role: a mark a reader put on the
         * chart is inked by its role, and asking every caller to
         * classify it would be ceremony for a question the chart
         * never asks.
         *
         * <p><strong>Refused for {@link InkRole#REFERENCE_LINE}.</strong>
         * That role is exactly where the kind decides what is drawn,
         * and a convenience that quietly supplied {@code PLACE} there
         * would preserve the failure this vocabulary was added to
         * end: a module contributing an equinox, saying nothing, and
         * silently receiving the zenith's ring and upward tick
         * (PR #278 review). A reference point states what it is.
         */
        public Point(String identity, String accessibleName,
                     SkyPosition at, InkRole role) {
            this(identity, accessibleName, at, requirePlaceable(role,
                    identity), role);
        }

        private static Mark requirePlaceable(InkRole role,
                                             String identity) {
            if (role == InkRole.REFERENCE_LINE) {
                throw new IllegalArgumentException(
                        "a reference point says whether it is a place"
                                + " or a landmark; the chart inks the"
                                + " two differently: " + identity);
            }
            return Mark.PLACE;
        }
    }

    /**
     * The observer's cardinal direction on the mathematical horizon
     * (issue #359).
     *
     * <p>It carries the direction's identity and its exact sky
     * position, and <strong>no words - not even an accessible
     * name</strong>. This variant deliberately does not promise one:
     * the drawn letter and the spoken name are the page's language -
     * {@code PageWords.directionLetter} and {@code directionSpoken} -
     * and the rendering and accessibility layer resolves the mark by
     * type ({@code ReferenceInk.directionPlacements}), so every
     * rendered direction carries both localized texts. A module that
     * shipped an N would be choosing a language, which is not its to
     * choose; a contract method that threw would make valid domain
     * data unsafe to inspect (review).
     *
     * <p>The mark stays at this exact horizon point. Its letter may
     * take one of a few adjacent boxes, but the mark itself is the
     * direction and never moves to the frame or to a friendlier spot
     * on the horizon; where no adjacent box is clean the whole
     * landmark is omitted rather than left as an unexplained diamond.
     * That rule is the chart's ({@code ReferenceInk}); what is stated
     * here is only what the mark is.
     */
    record DirectionMark(Cardinal direction, SkyPosition at)
            implements OverlayContribution {

        public DirectionMark {
            if (direction == null) {
                throw new IllegalArgumentException(
                        "a cardinal mark says which direction it is");
            }
            if (at == null) {
                throw new IllegalArgumentException(
                        "a cardinal mark is somewhere: "
                                + direction.identity());
            }
        }

        @Override
        public InkRole role() {
            return InkRole.REFERENCE_LINE;
        }

        @Override
        public String identity() {
            return direction.identity();
        }

    }

    /** An open run of sky positions. */
    /**
     * A Solar System body drawn at its true angular size (Sprint 37,
     * issue #415, under the #414 ruling): the centre, the disc's
     * apparent diameter, whether it is below the observer's drawn
     * horizon, and - for the Moon - how it is lit. Nothing about how
     * to draw it: the page draws the Sun as a ring with its centre
     * dot and the Moon as a phased disc, in its own ink, and never
     * enlarges either.
     *
     * @param angularDiameterArcseconds the disc's apparent diameter
     * @param lit how the disc is lit, or null for a body lit entirely
     *            (the Sun)
     * @param belowHorizon true when the body stands below the
     *                     observer's mathematical horizon and that
     *                     horizon is drawn: the page dims it and may
     *                     say so
     * @param distanceKm how far the body is from the observer: the
     *                   page paints the farthest first, so the nearer
     *                   covers the farther as it does in the sky - the
     *                   Moon over the Sun at a new Moon (issue #416)
     */
    record Body(String identity, String accessibleName, SkyPosition at,
                double angularDiameterArcseconds, Lit lit,
                boolean belowHorizon, double distanceKm, InkRole role)
            implements OverlayContribution {
        public Body {
            requireIdentified(identity, accessibleName, role);
            if (at == null) {
                throw new IllegalArgumentException(
                        "a body is somewhere: " + identity);
            }
            if (!(angularDiameterArcseconds > 0.0)
                    || !Double.isFinite(angularDiameterArcseconds)) {
                throw new IllegalArgumentException(
                        "a body has a positive apparent diameter: "
                                + identity);
            }
            if (!(distanceKm > 0.0) || !Double.isFinite(distanceKm)) {
                throw new IllegalArgumentException(
                        "a body is at a positive distance: " + identity);
            }
            if (role != InkRole.BODY) {
                throw new IllegalArgumentException(
                        "a body is drawn in the body role: " + identity);
            }
        }
    }

    /**
     * An oblate body drawn with a cartographic minimum (issue #484, the
     * owner's rulings on #482): Jupiter.
     *
     * <p>Unlike a {@link Body}, true scale alone would lose it: at most
     * of the atlas's fields its disc is smaller than the smallest star
     * mark. So the page draws it at its true size or at
     * {@code minimumMarkPx}, whichever is larger. While the true disc is
     * below the minimum, the mark is <em>a cartographic symbol, not the
     * body's apparent diameter</em>: it is painted beneath the stars,
     * never erasing one, and a click through it reaches what is under
     * it. Once the true disc reaches the minimum it is the body itself:
     * opaque, in the bodies layer, hiding what it covers, and a click on
     * it reaches no hidden star. Its outline turns continuously from a
     * circle at the minimum to the true axis ratio as the true disc grows
     * to twice the minimum, the minor axis along the pole.
     *
     * @param poleAngleDegrees the north pole's position angle at the
     *                         body's J2000 place, from J2000 north through
     *                         east - the chart's own frame, never an
     *                         apparent of-date angle rotated by an assumed
     *                         correction
     * @param minimumMarkPx the smallest mark the page draws, in pixels
     */
    record OblateBody(String identity, String accessibleName, SkyPosition at,
                      double equatorialDiameterArcseconds,
                      double polarDiameterArcseconds, double poleAngleDegrees,
                      double minimumMarkPx, boolean belowHorizon,
                      double distanceKm, InkRole role)
            implements OverlayContribution {
        public OblateBody {
            requireIdentified(identity, accessibleName, role);
            if (at == null) {
                throw new IllegalArgumentException("a body is somewhere: " + identity);
            }
            if (!(equatorialDiameterArcseconds > 0.0)
                    || !Double.isFinite(equatorialDiameterArcseconds)
                    || !(polarDiameterArcseconds > 0.0)
                    || !(polarDiameterArcseconds <= equatorialDiameterArcseconds)) {
                throw new IllegalArgumentException("an oblate body has a positive"
                        + " equatorial diameter no smaller than its polar one: " + identity);
            }
            if (!Double.isFinite(poleAngleDegrees)) {
                throw new IllegalArgumentException("a pole has an angle: " + identity);
            }
            if (!(minimumMarkPx > 0.0) || !Double.isFinite(minimumMarkPx)) {
                throw new IllegalArgumentException("a minimum mark is a positive size: "
                        + identity);
            }
            if (!(distanceKm > 0.0) || !Double.isFinite(distanceKm)) {
                throw new IllegalArgumentException("a body is at a positive distance: "
                        + identity);
            }
            if (role != InkRole.BODY) {
                throw new IllegalArgumentException("a body is drawn in the body role: "
                        + identity);
            }
        }
    }

    /**
     * A satellite of an {@link OblateBody} (issue #485, the owner's
     * rulings 4 and 5 on #482): a Galilean moon. Its true disc is below
     * a pixel on every page, so the page draws a symbol of
     * {@code markPx}, never its apparent diameter, at its own J2000
     * place.
     *
     * <p>Whether a page draws it is decided by the page, moon by moon,
     * because it depends on the page's scale: behind its primary it is
     * omitted; at the chart's normal minimum field every other moon is
     * drawn at its exact place, overlap allowed; above it, a moon is drawn
     * only where its own mark is distinguishable, and a collision
     * suppresses only the lower-precedence mark - never the whole system.
     *
     * @param primary the identity of the body it belongs to
     * @param relation where it stands against its primary's disc
     * @param shadowed whether it stands wholly or partly in the primary's
     *                 shadow
     * @param precedence the order collisions are settled in among moons
     *                   of the same state, lowest first
     */
    record Satellite(String identity, String accessibleName, SkyPosition at,
                     String primary, Relation relation, boolean shadowed,
                     int precedence, double markPx, boolean belowHorizon,
                     InkRole role)
            implements OverlayContribution {

        /** Where a satellite stands against its primary's disc. */
        public enum Relation {
            CLEAR, IN_FRONT, BEHIND
        }

        public Satellite {
            requireIdentified(identity, accessibleName, role);
            if (at == null || primary == null || relation == null) {
                throw new IllegalArgumentException("a satellite is somewhere, of"
                        + " something, in some relation to it: " + identity);
            }
            if (!(markPx > 0.0) || !Double.isFinite(markPx)) {
                throw new IllegalArgumentException("a satellite's mark is a positive"
                        + " size: " + identity);
            }
            if (role != InkRole.BODY) {
                throw new IllegalArgumentException("a body is drawn in the body role: "
                        + identity);
            }
        }
    }

    /**
     * How a disc is lit (issue #416): the illuminated fraction, the
     * bright limb's position angle from celestial north through east,
     * and the phase angle whose cosine is the terminator's axis ratio.
     */
    record Lit(double illuminatedFraction, double brightLimbAngleDegrees,
               double phaseAngleDegrees) {
        public Lit {
            if (!(illuminatedFraction >= 0.0 && illuminatedFraction <= 1.0)) {
                throw new IllegalArgumentException(
                        "an illuminated fraction is in [0, 1]: "
                                + illuminatedFraction);
            }
            if (!Double.isFinite(brightLimbAngleDegrees)) {
                throw new IllegalArgumentException(
                        "a bright limb has an angle: " + brightLimbAngleDegrees);
            }
            if (!(phaseAngleDegrees >= 0.0 && phaseAngleDegrees <= 180.0)) {
                throw new IllegalArgumentException(
                        "a phase angle is in [0°, 180°]: " + phaseAngleDegrees);
            }
        }
    }

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
