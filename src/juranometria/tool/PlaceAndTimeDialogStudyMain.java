package juranometria.tool;

import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.UIManager;

import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;
import juranometria.ui.placeandtime.PlaceAndTimeDialog;
import juranometria.ui.placeandtime.PlaceStore;

/**
 * The production place-and-time dialog, photographed (Sprint 25,
 * issue #228).
 *
 * <p>The <em>packed dialog itself</em>, not its controls rearranged
 * in a stand-in panel: an artificial arrangement can hide a clipped
 * control that the dialog's real packed geometry would show
 * (review). What is painted is the dialog's content pane at the size
 * the constructor decided, under the real look and feel, at the
 * ordinary size, at enlarged text and in the dark theme.
 *
 * <p>Needs a display, because a dialog does; run where the atlas
 * runs. Nothing here is a session - the store is a throwaway node,
 * the clock answers the frozen instant, and the dialog is never
 * shown.
 */
public final class PlaceAndTimeDialogStudyMain {

    private PlaceAndTimeDialogStudyMain() {
    }

    private static final File DIR = new File("docs/studies/place-and-time");

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("dialog photographs need a display:"
                    + " a packed dialog cannot exist without one, and"
                    + " a stand-in panel is what the review rejected");
            System.exit(1);
        }
        DIR.mkdirs();
        write("dialog-real", 12, false);
        write("dialog-real-enlarged", 18, false);
        write("dialog-real-dark", 12, true);
        System.out.println("dialog photographs written to "
                + DIR.getPath());
    }

    private static void write(String name, int points, boolean dark)
            throws Exception {
        if (dark) {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } else {
            com.formdev.flatlaf.FlatLightLaf.setup();
        }
        UIManager.put("defaultFont",
                new Font(Font.SANS_SERIF, Font.PLAIN, points));

        MeridianModule module = new MeridianModule(new Observer(
                59.913, 10.752, Instant.parse("2026-03-20T21:33:00Z")));
        java.util.prefs.Preferences throwaway =
                java.util.prefs.Preferences.userRoot()
                        .node("juranometria-study-" + name);
        // Built and painted on the event thread, like every other
        // living Swing surface: the first version worked off it, and
        // FlatLaf's layout state made the packed width depend on
        // which theme had run first in the JVM - a black stripe
        // where the fill and the paint disagreed, which the new
        // structural checks caught in a committed photograph (#242).
        Frame[] owner = new Frame[1];
        PlaceAndTimeDialog[] dialog = new PlaceAndTimeDialog[1];
        BufferedImage[] image = new BufferedImage[1];
        int[] size = new int[2];
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                owner[0] = new JFrame("study");
                dialog[0] = PlaceAndTimeDialog.packedForStudy(owner[0],
                        module, PlaceStore.forNode(throwaway));
                java.awt.Container content = dialog[0].getContentPane();
                size[0] = content.getWidth();
                size[1] = content.getHeight();
                image[0] = new BufferedImage(size[0], size[1],
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image[0].createGraphics();
                try {
                    g.setColor(content.getBackground());
                    g.fillRect(0, 0, size[0], size[1]);
                    content.paint(g);
                } finally {
                    g.dispose();
                }
            });
            ImageIO.write(image[0], "png", new File(DIR, name + ".png"));
            System.out.printf(Locale.ROOT, "  %s (%dx%d px, %d pt%s)%n",
                    name, size[0], size[1], points,
                    dark ? ", dark" : "");
        } finally {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                if (dialog[0] != null) {
                    dialog[0].dispose();
                }
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
            try {
                // A photograph is not a session, and must not leave
                // one behind in the developer's real preferences.
                throwaway.removeNode();
            } catch (java.util.prefs.BackingStoreException e) {
                // Leaving an empty node is a blemish, not a failure.
            }
        }
    }
}
