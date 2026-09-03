package juranometria.tool;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;
import juranometria.ui.placeandtime.PlaceAndTimeDialog;
import juranometria.ui.placeandtime.PlaceStore;

/**
 * The real place-and-time dialog, photographed (Sprint 25, issue
 * #228).
 *
 * <p>The gate's mock-ups proposed this surface; these pictures are
 * the production content itself, at the ordinary size, at enlarged
 * text, and in the dark theme, so the review judges what a reader
 * gets rather than what a drawing promised. Nothing here is a
 * session: the store is a throwaway node and the clock answers the
 * frozen instant.
 */
public final class PlaceAndTimeDialogStudyMain {

    private PlaceAndTimeDialogStudyMain() {
    }

    private static final File DIR = new File("docs/studies/place-and-time");

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        write("dialog-real", 420, 12, false);
        write("dialog-real-enlarged", 460, 18, false);
        write("dialog-real-dark", 420, 12, true);
        System.out.println("dialog photographs written to "
                + DIR.getPath());
    }

    private static void write(String name, int width, int points,
                              boolean dark) throws IOException {
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
        JComponent content;
        try {
            content = PlaceAndTimeDialog.contentForStudy(module,
                    PlaceStore.forNode(throwaway));
        } finally {
            try {
                // A photograph is not a session, and must not leave
                // one behind in the developer's real preferences.
                throwaway.removeNode();
            } catch (java.util.prefs.BackingStoreException e) {
                // Leaving an empty node is a blemish, not a failure.
            }
        }

        JPanel frame = new JPanel(new BorderLayout());
        frame.add(content, BorderLayout.CENTER);
        int height = Math.max(content.getPreferredSize().height + 20,
                points > 14 ? 400 : 330);
        frame.setSize(width, height);
        layOut(frame);

        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(frame.getBackground());
            g.fillRect(0, 0, width, height);
            frame.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        System.out.printf(Locale.ROOT, "  %s (%d px, %d pt%s)%n", name,
                width, points, dark ? ", dark" : "");
    }

    private static void layOut(java.awt.Component component) {
        component.doLayout();
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }
}
