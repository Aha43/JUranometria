package juranometria.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import juranometria.project.PixelPoint;

/**
 * How a pointer drag may move the chart (issue #691, ruled C-1 to C-6
 * on #690, docs/decisions/guide-and-explain.md §4 and §11): freely, along
 * the screen's rows only, along its columns only, or not at all.
 *
 * <p>The constraint is a statement about the screen, never about the
 * sky. The production drag ({@link PanInteraction}) asks the exact grab
 * solver to put the sky grabbed at the press under a page pixel; a mode
 * changes only which pixel - the pointer's column on the press row, the
 * pointer's row on the press column, or none. The solver, its pole rules
 * and every projection stay as they are, so a horizontal-only drag keeps
 * a screen row, not a parallel of declination (measured in the study).
 *
 * <p>Session state only (C-4): every launch starts {@link Mode#FREE};
 * nothing is stored. Lock zoom is a different thing and is untouched.
 */
public final class PointerDrag {

    /** The four ruled modes (C-1). */
    public enum Mode {
        FREE, HORIZONTAL, VERTICAL, LOCKED;

        /** How the view report writes a mode: free, horizontal, vertical, locked. */
        public String token() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The axis a Shift-held free drag keeps to (C-2). */
    public enum Axis { HORIZONTAL, VERTICAL }

    private Mode mode = Mode.FREE;
    private final List<Consumer<Mode>> listeners = new ArrayList<>();

    /** The mode a drag pressed now would use. */
    public Mode mode() {
        return mode;
    }

    /** Chooses a mode; listeners hear a change, never a repeat. */
    public void choose(Mode chosen) {
        if (chosen == null) {
            throw new IllegalArgumentException("a pointer drag has a mode");
        }
        if (chosen == mode) {
            return;
        }
        mode = chosen;
        for (Consumer<Mode> listener : List.copyOf(listeners)) {
            listener.accept(chosen);
        }
    }

    /**
     * Calls {@code listener} with the current mode now and with every
     * change; the returned action unsubscribes.
     */
    public Runnable onChange(Consumer<Mode> listener) {
        listeners.add(listener);
        listener.accept(mode);
        return () -> listeners.remove(listener);
    }

    /**
     * The page pixel the grabbed sky should stand under while the pointer
     * is at {@code pointer}, or empty when the mode lets no drag pan.
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
     * The axis a Shift-held free drag keeps to, decided once from the
     * first pointer position past the click threshold: its larger
     * component, a tie going across.
     */
    public static Axis dominant(PixelPoint press, PixelPoint firstBeyondThreshold) {
        double dx = Math.abs(firstBeyondThreshold.x() - press.x());
        double dy = Math.abs(firstBeyondThreshold.y() - press.y());
        return dx >= dy ? Axis.HORIZONTAL : Axis.VERTICAL;
    }

    /** The mode an axis stands for while it is held. */
    public static Mode modeFor(Axis axis) {
        return axis == Axis.HORIZONTAL ? Mode.HORIZONTAL : Mode.VERTICAL;
    }
}
