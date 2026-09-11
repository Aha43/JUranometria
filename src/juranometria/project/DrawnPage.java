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
 * <p><strong>Issue #329 owns the end of this.</strong> When
 * orthographic becomes a production projection paired with a real
 * field, {@link #of(ChartScene)} becomes the only way to build one
 * and the pair can never disagree again.
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
    }

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
