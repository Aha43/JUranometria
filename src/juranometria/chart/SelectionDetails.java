package juranometria.chart;

import java.util.Optional;

/**
 * The catalogue record behind a selection (Sprint 19, issue #170).
 *
 * <p>A {@link Selection} deliberately carries only identity and
 * position, so the facts to show are looked up when they are shown -
 * and looked up <strong>in the page the reader is already looking
 * at</strong>. The scene holds the objects it drew; the selected one
 * is by construction among them, so answering "what is this?" costs
 * no catalogue query at all. Selecting must never make the atlas go
 * to disk, and this is why it does not have to.
 *
 * <p>Returns empty when the selection names something this page does
 * not draw - after the reader pans away, for instance. A caller shows
 * what it has and says nothing it cannot support.
 */
public final class SelectionDetails {

    private SelectionDetails() {
    }

    /** The star a selection names, if this page drew it. */
    public static Optional<Star> star(ChartScene scene, Selection selection) {
        if (!(selection instanceof Selection.Object object)
                || object.kind() != Selection.Object.Kind.STAR
                || scene == null) {
            return Optional.empty();
        }
        return scene.stars().stream()
                .filter(star -> star.id().equals(object.catalogueId()))
                .findFirst();
    }

    /** The deep-sky object a selection names, if this page drew it. */
    public static Optional<DeepSkyObject> deepSky(ChartScene scene,
                                                  Selection selection) {
        if (!(selection instanceof Selection.Object object)
                || object.kind() != Selection.Object.Kind.DEEP_SKY
                || scene == null) {
            return Optional.empty();
        }
        return scene.deepSkyObjects().stream()
                .filter(dso -> dso.id().equals(object.catalogueId()))
                .findFirst();
    }
}
