package juranometria.render;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;

/**
 * The regional detail policy of docs/decisions/regional-zoom.md, in one
 * testable place. At the classic fields (18 degrees and narrower) every
 * symbol-bearing DSO draws, clamped up to the practical minimum when tiny,
 * and Messier-priority objects carry labels — the released behaviour,
 * untouched. At wider regional fields the practical-minimum clamp would
 * inflate hundreds of sub-pixel objects into speckle, so there:
 *
 * <ul>
 * <li>a symbol draws only at its true projected size (measured with the
 *     renderer's exact viewport scale), never clamp-inflated;</li>
 * <li>priority-1 (Messier) objects always draw, clamped when necessary;</li>
 * <li>the searched target — matched by the scene's stable catalogue
 *     identity — always draws and is always labelled, clamped when
 *     necessary, whatever its priority or size. The guarantee reaches
 *     only types with an established chart symbol; symbol-less types are
 *     never forced into existence;</li>
 * <li>labels attach only to Messier objects drawn at true size, plus the
 *     target — which dissolves the measured label collisions.</li>
 * </ul>
 *
 * Styling only: no decision here reads or alters stored dimensions beyond
 * projecting them to the page.
 */
public final class RegionalDetailPolicy {

    /** The widest field at which the released classic behaviour holds. */
    public static final double WIDEST_CLASSIC_FIELD_DEGREES = 18.0;

    /** Practical minimum drawn major axis; the true axis ratio is kept. */
    public static final double PRACTICAL_MINIMUM_MAJOR_PX = 6.0;

    /** Messier priority: always drawn regionally, labelled classically. */
    private static final int ALWAYS_DRAWN_PRIORITY = 1;

    private final boolean regional;
    private final double pixelsPerPlaneUnit;
    private final String targetIdentity;

    public RegionalDetailPolicy(ChartScene scene, double pixelsPerPlaneUnit) {
        this.regional =
                scene.viewport().fieldWidthDegrees() > WIDEST_CLASSIC_FIELD_DEGREES;
        this.pixelsPerPlaneUnit = pixelsPerPlaneUnit;
        this.targetIdentity = scene.targetIdentity();
    }

    /** Whether the true projected major axis reaches the practical minimum. */
    public boolean drawsAtTrueSize(DeepSkyObject dso) {
        return Math.toRadians(dso.majorAxisArcmin() / 60.0) * pixelsPerPlaneUnit
                >= PRACTICAL_MINIMUM_MAJOR_PX;
    }

    /** Whether this object's symbol appears on the page at all. */
    public boolean drawn(DeepSkyObject dso) {
        if (!ChartRenderer.hasSymbol(dso)) {
            return false;
        }
        return !regional || drawsAtTrueSize(dso)
                || dso.labelPriority() <= ALWAYS_DRAWN_PRIORITY || isTarget(dso);
    }

    /** Whether a sub-minimum symbol may be inflated to the minimum. */
    public boolean clampAllowed(DeepSkyObject dso) {
        return !regional
                || dso.labelPriority() <= ALWAYS_DRAWN_PRIORITY || isTarget(dso);
    }

    /** Whether this object's label appears on the page. */
    public boolean labelled(DeepSkyObject dso) {
        if (!ChartRenderer.hasSymbol(dso)) {
            return false;
        }
        if (!regional) {
            // The released classic rule, untouched: Messier labels only.
            return dso.labelPriority() <= ALWAYS_DRAWN_PRIORITY;
        }
        return isTarget(dso) || (dso.labelPriority() <= ALWAYS_DRAWN_PRIORITY
                && drawsAtTrueSize(dso));
    }

    private boolean isTarget(DeepSkyObject dso) {
        return targetIdentity != null && targetIdentity.equals(dso.id());
    }
}
