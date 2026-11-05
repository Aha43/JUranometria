package juranometria.tool;

import java.util.Optional;

import juranometria.project.PixelPoint;

/**
 * The study's prototype of constrained pointer dragging (issue #690,
 * road #689): which page pixel the grabbed sky is asked to follow, under
 * each proposed mode, while the pointer is at a given pixel.
 *
 * <p>Nothing here is production. The production drag
 * ({@code juranometria.ui.PanInteraction}) asks the exact grab solver to
 * put the sky grabbed at the press under the pointer. A constraint, as
 * proposed, changes only <em>which pixel</em> it is put under: the
 * pointer's column on the press row (horizontal only), the pointer's row
 * on the press column (vertical only), or nowhere (locked). The solver,
 * its pole rules and every projection stay as they are, so the
 * constraint is a statement about the screen and never about a celestial
 * coordinate or curve.
 *
 * <p>The temporary modifier constrains one free drag to its dominant
 * axis, decided once - at the first movement past the click threshold,
 * from that movement's larger component, a tie going to horizontal - and
 * held for the rest of that drag only.
 */
public final class PointerDragSketch {

    /** The four modes the road starts the study with. */
    public enum Mode { FREE, HORIZONTAL, VERTICAL, LOCKED }

    /** The axis a modifier-held drag keeps to. */
    public enum Axis { HORIZONTAL, VERTICAL }

    private PointerDragSketch() {
    }

    /**
     * The pixel the grabbed sky should stand under, or empty when the
     * mode lets no drag pan.
     */
    public static Optional<PixelPoint> target(Mode mode, PixelPoint press,
                                              PixelPoint pointer) {
        return switch (mode) {
            case FREE -> Optional.of(pointer);
            case HORIZONTAL -> Optional.of(new PixelPoint(pointer.x(), press.y()));
            case VERTICAL -> Optional.of(new PixelPoint(press.x(), pointer.y()));
            case LOCKED -> Optional.empty();
        };
    }

    /**
     * The axis a modifier-held drag keeps to, decided from the first
     * pointer position that leaves the click threshold.
     */
    public static Axis dominant(PixelPoint press, PixelPoint firstBeyondThreshold) {
        double dx = Math.abs(firstBeyondThreshold.x() - press.x());
        double dy = Math.abs(firstBeyondThreshold.y() - press.y());
        return dx >= dy ? Axis.HORIZONTAL : Axis.VERTICAL;
    }

    /** The mode one axis stands for while a modifier is held. */
    public static Mode modeFor(Axis axis) {
        return axis == Axis.HORIZONTAL ? Mode.HORIZONTAL : Mode.VERTICAL;
    }

    /**
     * Whether a pointer has left the click threshold, as production
     * decides it ({@code PanInteraction.mouseDragged}): at least
     * {@code thresholdPx} from the press.
     */
    public static boolean beyond(PixelPoint press, PixelPoint pointer, double thresholdPx) {
        return Math.hypot(pointer.x() - press.x(), pointer.y() - press.y()) >= thresholdPx;
    }
}
