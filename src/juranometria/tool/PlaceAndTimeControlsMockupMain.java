package juranometria.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * Where the reader sets a place and an instant (Sprint 25, issue
 * #225).
 *
 * <p>Two candidate homes, drawn at the sizes that decide between
 * them. The Inspector already carries two modes and stacks its
 * chooser at the narrow end; a third would cost a third row of
 * chrome before a reader has read anything. A settings dialog is
 * what the atlas already uses for choices that are not readings -
 * Chart Options - and it has room for a frozen instant, two
 * coordinates and two deliberate actions.
 *
 * <p>Mock-ups, not production: plain components arranged as the
 * decision proposes, so the gate can be judged on how it reads.
 */
public final class PlaceAndTimeControlsMockupMain {

    private PlaceAndTimeControlsMockupMain() {
    }

    private static final File DIR = new File("docs/studies/place-and-time");

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        write("controls-dialog", 420, 12, false);
        write("controls-dialog-enlarged", 460, 18, false);
        write("controls-dialog-dark", 420, 12, true);
        write("controls-sidebar-240", 240, 12, false);
        System.out.println("control mock-ups written to " + DIR.getPath());
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

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel heading = new JLabel("Place and time");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, points + 3));
        heading.setAlignmentX(0.0f);
        panel.add(heading);
        panel.add(strut(10));

        panel.add(field("Latitude", "59.913° N", points, width));
        panel.add(strut(6));
        panel.add(field("Longitude", "10.752° E", points, width));
        panel.add(strut(6));
        panel.add(field("Instant (UTC)", "2026-03-20 21:33", points, width));
        panel.add(strut(10));

        JLabel frozen = new JLabel("<html>The chart is drawn for that"
                + " instant and stays there. Nothing ticks.</html>");
        frozen.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, points - 1));
        frozen.setForeground(dark ? new Color(170, 170, 170)
                : new Color(110, 110, 110));
        frozen.setAlignmentX(0.0f);
        panel.add(frozen);
        panel.add(strut(12));

        JPanel shows = new JPanel();
        shows.setLayout(new BoxLayout(shows, BoxLayout.Y_AXIS));
        shows.setAlignmentX(0.0f);
        for (String what : new String[] {"Meridian", "Horizon", "Zenith"}) {
            JCheckBox box = new JCheckBox(what, true);
            box.setAlignmentX(0.0f);
            box.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, points));
            shows.add(box);
        }
        panel.add(shows);
        panel.add(strut(12));

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setAlignmentX(0.0f);
        JButton now = new JButton("Now");
        JButton centre = new JButton("Center on zenith");
        for (JButton button : new JButton[] {now, centre}) {
            button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, points));
        }
        actions.add(now);
        actions.add(centre);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                centre.getPreferredSize().height));
        panel.add(actions);

        int height = points > 14 ? 380 : 320;
        JPanel frame = new JPanel(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(width, height);
        frame.doLayout();
        layOut(frame);

        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(dark ? new Color(30, 30, 32) : Color.WHITE);
            g.fillRect(0, 0, width, height);
            frame.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        System.out.printf(Locale.ROOT, "  %s (%d px, %d pt%s)%n", name,
                width, points, dark ? ", dark" : "");
    }

    private static JPanel field(String label, String value, int points,
                                int width) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(0.0f);
        JLabel name = new JLabel(label);
        name.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, points));
        name.setPreferredSize(new Dimension(points * 9, points + 12));
        JTextField entry = new JTextField(value);
        entry.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, points));
        row.add(name);
        row.add(Box.createHorizontalStrut(8));
        row.add(entry);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                entry.getPreferredSize().height));
        return row;
    }

    private static java.awt.Component strut(int height) {
        java.awt.Component strut = Box.createVerticalStrut(height);
        ((javax.swing.JComponent) strut).setAlignmentX(0.0f);
        return strut;
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
