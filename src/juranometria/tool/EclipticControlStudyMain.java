package juranometria.tool;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Where a reader turns the ecliptic on (Sprint 28, issue #271).
 *
 * <p>The first draft of this gate rejected Chart Options and a
 * separate View surface and then said only "its own module control",
 * which names an owner rather than a place a reader can reach. A
 * review refused that as unimplementable (PR #276 review), so the
 * home is chosen and drawn here.
 *
 * <p><strong>A checkbox item on the View menu</strong>, beside the
 * Inspector's - which is already exactly this: a
 * {@code JCheckBoxMenuItem} whose tick tells the reader whether the
 * thing is showing. The ecliptic has no settings at all - no
 * observer, no instant, nothing to type - so a dialog would be a
 * window built around one checkbox.
 *
 * <p>The menu is also the one surface a narrow window cannot truncate:
 * a popup is laid out by its own content, not by the window it hangs
 * from, which is what disqualified the Inspector for place-and-time
 * at its 240 px floor. These pages show the item at ordinary and
 * enlarged text in both application themes, and print the width each
 * needs.
 */
public final class EclipticControlStudyMain {

    private EclipticControlStudyMain() {
    }

    private static final File DIR = new File("docs/studies/ecliptic");

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        System.out.println("ecliptic control surface:");
        write("view-menu", 12, false);
        write("view-menu-enlarged", 18, false);
        write("view-menu-dark", 12, true);
        write("view-menu-dark-enlarged", 18, true);
        System.out.println("written to " + DIR.getPath());
    }

    private static void write(String name, int points, boolean dark)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (dark) {
                    com.formdev.flatlaf.FlatDarkLaf.setup();
                } else {
                    com.formdev.flatlaf.FlatLightLaf.setup();
                }
                Font base = UIManager.getFont("defaultFont");
                if (base == null) {
                    base = new Font(Font.SANS_SERIF, Font.PLAIN, points);
                }
                UIManager.put("defaultFont",
                        base.deriveFont((float) points));

                JPanel view = viewMenu();
                Dimension size = view.getPreferredSize();
                view.setSize(size);
                view.doLayout();

                JPanel host = new JPanel(null);
                host.setSize(size.width + 24, size.height + 24);
                host.setBackground(UIManager.getColor("Panel.background"));
                view.setLocation(12, 12);
                host.add(view);
                host.doLayout();
                layOut(view);

                BufferedImage image = new BufferedImage(host.getWidth(),
                        host.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                try {
                    host.printAll(g);
                } finally {
                    g.dispose();
                }
                ImageIO.write(image, "png",
                        new File(DIR, "controls-" + name + ".png"));
                System.out.println(String.format(Locale.ROOT,
                        "  controls-%s (%s, %d pt) needs %d x %d px",
                        name, dark ? "dark" : "light", points,
                        size.width, size.height));
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
        });
        UIManager.put("defaultFont", null);
    }

    /**
     * The View menu as it would be with the ecliptic module loaded:
     * the atlas's existing items, and one more checkbox beside the
     * Inspector's.
     *
     * <p>The real items, in a panel rather than a {@code JPopupMenu},
     * because a popup paints nothing until it is showing. A mock-up
     * of the arrangement, as the place-and-time control study is; the
     * item classes, their ticks and their text are production's.
     */
    private static JPanel viewMenu() {
        JPanel view = new JPanel();
        view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
        view.setBackground(UIManager.getColor("PopupMenu.background"));
        view.setBorder(BorderFactory.createLineBorder(
                UIManager.getColor("PopupMenu.borderColor") != null
                        ? UIManager.getColor("PopupMenu.borderColor")
                        : UIManager.getColor("Separator.foreground")));
        view.add(new JMenuItem("Chart Options..."));
        view.add(new JMenuItem("Place and Time..."));
        JCheckBoxMenuItem inspector = new JCheckBoxMenuItem("Inspector");
        inspector.setSelected(true);
        view.add(inspector);
        // The one new control the ecliptic module adds. Checked,
        // because that is the state a reader needs to be able to see.
        JCheckBoxMenuItem ecliptic = new JCheckBoxMenuItem("Ecliptic");
        ecliptic.setSelected(true);
        view.add(ecliptic);
        view.add(new JSeparator());
        view.add(new JMenuItem("Zoom In"));
        view.add(new JMenuItem("Zoom Out"));
        return view;
    }

    /** Sizes every item, since nothing here is in a real window. */
    private static void layOut(java.awt.Container container) {
        container.doLayout();
        for (java.awt.Component child : container.getComponents()) {
            child.setSize(child.getPreferredSize());
            if (child instanceof java.awt.Container nested) {
                layOut(nested);
            }
        }
        container.doLayout();
    }
}
