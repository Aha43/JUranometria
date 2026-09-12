package juranometria.project;

import java.util.Locale;

import juranometria.chart.ChartScene;

/**
 * A page and the projection that draws it, together.
 *
 * <p>Everything that states what a page <em>is</em> - the title block,
 * the accessible description, the exported sheet's metadata - and the
 * geometry that draws it now read the projection from here, so there
 * is one answer rather than four that happen to agree.
 *
 * <p>They agreed until Sprint 32. A viewport carries a projection
 * <em>kind</em>, the kind is a function of the field, and the field
 * is a rung on the ladder, so asking the viewport was asking the
 * projection. The celestial-globe gate (issue #301) needs production
 * pages drawn by a projection the enum has no value for, before
 * production has one: for the length of that gate, and only through
 * {@code SceneAssembler.assembleForStudy}, a page exists whose
 * viewport kind is not what drew it.
 *
 * <p><strong>That mismatch is the point of the temporary door, and
 * this is where it is contained.</strong> Not four optional overrides
 * that could drift apart - one value, carried together, so a page
 * cannot be drawn as one thing and named as another. The atlas has
 * made that mistake once already: until #300 every exported sheet
 * said "gnomonic", which was true of every page it could draw until
 * it was not, and then it was a sheet claiming to be something it
 * was not.
 *
 * <p>It lives beside the projections rather than beside the renderer
 * because everything that draws or describes a page needs it, and the
 * mapping from plane to pixels needs it first: a page's scale is its
 * projection's answer at half its field, and a mapping built from a
 * different projection draws the page at the wrong size. Sprint 32
 * found exactly that - a hemisphere's stars placed orthographically
 * on a plane scaled stereographically, the disc landing at half the
 * page it should have filled.
 *
 * <p><strong>This value is not temporary.</strong> The mismatch it
 * carries is: when #329 makes orthographic a production projection
 * paired with a real field, {@link #of(ChartScene)} becomes the only
 * way to build one and the pair can never disagree again. The value
 * itself stays, because production had thirteen implicit copies of
 * this boundary before a globe existed to make them disagree, and
 * {@code OneProjectionPerPageTest} exists to keep it at one.
 */
public record DrawnPage(ChartScene scene, Projection projection) {

    public DrawnPage {
        if (scene == null) {
            throw new IllegalArgumentException("scene must not be null");
        }
        if (projection == null) {
            throw new IllegalArgumentException(
                    "a page is drawn by a projection; none is not an"
                            + " answer");
        }
        // And drawn about the place it says it is drawn about
        // (review of #335). Refusing nulls was not enough: a scene
        // centred on Orion carrying a projection centred on M31 was
        // accepted, and every mark would then have been placed around
        // M31 while the title block, the accessible description, the
        // catalogue query and the exported sheet all said Orion. That
        // is the same page carrying two geometric identities, which
        // is the one thing this value exists to make impossible.
        //
        // Compared as the sky compares positions rather than field by
        // field: a centre is a place, and two ways of writing the
        // same place are the same centre.
        double apart = scene.viewport().centre()
                .separationDegrees(projection.centre());
        if (!(apart <= CENTRE_TOLERANCE_DEGREES)) {
            throw new IllegalArgumentException(String.format(
                    java.util.Locale.ROOT,
                    "a page drawn about %s cannot be a page centred"
                            + " on %s: they are %.6f degrees apart,"
                            + " and every mark on it would be placed"
                            + " around one while the page said the"
                            + " other",
                    projection.centre(), scene.viewport().centre(),
                    apart));
        }
    }

    /**
     * How far apart two spellings of the same centre may be.
     *
     * <p>Not zero, because a centre travels through solvers and
     * round trips: the projections round-trip to 3.3e-13 degrees, and
     * a page rebuilt about its own unprojected centre can differ in
     * the last bits without being a different place. Far below
     * anything a reader could see - a thousandth of an arcsecond is
     * a hundred-thousandth of a pixel on the widest page the atlas
     * draws.
     */
    private static final double CENTRE_TOLERANCE_DEGREES = 1e-9;

    /** The ordinary case: the page is drawn by what its viewport says. */
    public static DrawnPage of(ChartScene scene) {
        return new DrawnPage(scene,
                Projections.forViewport(scene.viewport()));
    }

    /** What this page calls its projection, wherever it says so. */
    public String projectionName() {
        return projection.name();
    }

    /**
     * What a reader who cannot see the page is told about it.
     *
     * <p>Lives here rather than beside the component that shows it,
     * because a description is a statement about the page and has to
     * name the projection that drew it - the same requirement the
     * title block and the exported sheet have, from the same source.
     */
    public String describe() {
        return String.format(Locale.ROOT,
                "%s. Centre RA %.4f, Dec %+.4f (ICRS J2000)."
                        + " Field %.1f degrees wide, %s projection."
                        + " Stars to V %.1f. North up, east left.",
                scene.title(),
                scene.viewport().centre().raDegrees(),
                scene.viewport().centre().decDegrees(),
                scene.viewport().fieldWidthDegrees(),
                projectionName(),
                scene.limitingMagnitude());
    }
}
