package juranometria.app;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The application's mark, at whatever size something asks for
 * (Sprint 23, issue #202).
 *
 * <p>Drawn from {@link ApplicationMark}, the geometry the gate chose,
 * rather than loaded from a file. The window icon a desktop shows and
 * the containers a platform installs are therefore the same drawing
 * by construction - there is no committed PNG for the running
 * application to fall out of step with, and no size that is a
 * resampling of another.
 *
 * <p>Before this, the application set no window icon at all, so a
 * task switcher showed Java's default cup.
 */
public final class ApplicationIcon {

    private ApplicationIcon() {
    }

    /** The chosen mark (issue #200): a cropped Andromeda. */
    public static final ApplicationMark.Candidate MARK =
            ApplicationMark.Candidate.RIFT;

    /**
     * The sizes a window manager chooses between. Java hands the
     * whole set to the platform, which picks what its title bar,
     * task switcher and dock each want.
     */
    public static final int[] WINDOW_SIZES = {16, 24, 32, 48, 64, 128, 256};

    /** The mark at one size, composed at that size. */
    public static BufferedImage at(int size) {
        BufferedImage image = new BufferedImage(size, size,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            ApplicationMark.paint(g, MARK, size, -1);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** The set to hand a window, smallest first. */
    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : WINDOW_SIZES) {
            icons.add(at(size));
        }
        return List.copyOf(icons);
    }
}
