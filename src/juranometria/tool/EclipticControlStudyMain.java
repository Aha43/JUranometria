package juranometria.tool;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;
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
 * at its 240 px floor. That is a structural property of popups, and
 * it is the reason the menu was chosen.
 *
 * <p><strong>These are arrangement mock-ups, and their pixel sizes
 * are not the View popup's.</strong> The items are the production
 * classes, with the neighbours' real accelerators, but they are laid
 * out in a {@code JPanel} with a {@code BoxLayout} rather than by the
 * menu UI, which owns its own check-icon and accelerator columns,
 * insets, spacing and separator metrics. A popup paints nothing until
 * it is shown, so a real one cannot be photographed here. The numbers
 * printed below describe this arrangement and are quoted nowhere as
 * the menu's; #274 owes a real-menu test - both themes, enlarged
 * text, nothing clipped (PR #276 round 2).
 *
 * <p>The first page shows the <em>released default</em>: a reader who
 * has never asked for the ecliptic does not get it.
 */
public final class EclipticControlStudyMain {

    private EclipticControlStudyMain() {
    }

    private static final File DIR = new File("docs/studies/ecliptic");

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        System.out.println("ecliptic control surface:");
        // The released default first: a fresh reader has never asked
        // for the ecliptic, so it is not ticked and not drawn.
        write("view-menu-default", 12, false, false);
        write("view-menu", 12, false, true);
        write("view-menu-enlarged", 18, false, true);
        write("view-menu-dark", 12, true, true);
        write("view-menu-dark-enlarged", 18, true, true);
        System.out.println("written to " + DIR.getPath());
    }

    private static void write(String name, int points, boolean dark,
                              boolean eclipticShown) throws Exception {
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

                JPanel view = viewMenu(eclipticShown);
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
                        "  controls-%s (%s, %d pt, ecliptic %s):"
                                + " arrangement %d x %d px",
                        name, dark ? "dark" : "light", points,
                        eclipticShown ? "shown" : "the released default",
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
    private static JPanel viewMenu(boolean eclipticShown) {
        JPanel view = new JPanel();
        view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
        view.setBackground(UIManager.getColor("PopupMenu.background"));
        view.setBorder(BorderFactory.createLineBorder(
                UIManager.getColor("PopupMenu.borderColor") != null
                        ? UIManager.getColor("PopupMenu.borderColor")
                        : UIManager.getColor("Separator.foreground")));
        view.add(new JMenuItem("Chart Options..."));
        view.add(new JMenuItem("Place and Time..."));
        // The production neighbours carry accelerators, which the real
        // menu lays out in a column of their own; they are set here so
        // the arrangement is not quietly narrower than the true one.
        JCheckBoxMenuItem inspector = new JCheckBoxMenuItem("Inspector");
        inspector.setSelected(true);
        inspector.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
                java.awt.Toolkit.getDefaultToolkit()
                        .getMenuShortcutKeyMaskEx()));
        view.add(inspector);
        // The one new control the ecliptic module adds.
        JCheckBoxMenuItem ecliptic = new JCheckBoxMenuItem("Ecliptic");
        ecliptic.setSelected(eclipticShown);
        view.add(ecliptic);
        view.add(new JSeparator());
        JMenuItem zoomIn = new JMenuItem("Zoom In");
        zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                java.awt.Toolkit.getDefaultToolkit()
                        .getMenuShortcutKeyMaskEx()));
        view.add(zoomIn);
        JMenuItem zoomOut = new JMenuItem("Zoom Out");
        zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                java.awt.Toolkit.getDefaultToolkit()
                        .getMenuShortcutKeyMaskEx()));
        view.add(zoomOut);
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
