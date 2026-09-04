package juranometria.tool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

/**
 * The working-selection surface mock-ups (Sprint 27, issue #258):
 * the Inspector's working-set section and the visible Accumulate
 * control, drawn as widgets for the gate's eye - the
 * widget-rendered-inspection class, reviewed pictures of a decision
 * rather than production output.
 *
 * <p>The members shown are real identities from the released
 * Andromeda page plus one from wide Orion, because a mock-up filled
 * with invented rows tests nothing (the on-this-page rule) - and
 * one of them is off the current page, which is the surface's whole
 * reason to exist.
 */
public final class WorkingSelectionMockupMain {

    private WorkingSelectionMockupMain() {
    }

    private static final File DIR =
            new File("docs/studies/working-selection");

    /** A member row: identity, whether lead, whether off-page. */
    private record Member(String name, boolean lead, boolean offPage) {
    }

    private static final List<Member> MEMBERS = List.of(
            new Member("M 31", false, false),
            new Member("M 32", false, false),
            new Member("NGC 206", true, false),
            new Member("M 42", false, true),
            new Member("v And", false, false));

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        write("set", 320, 12, false);
        write("set-dark", 320, 12, true);
        write("set-enlarged", 320, 18, false);
        write("set-narrow", 240, 12, false);
        accumulate("accumulate", 12);
        System.out.println("mock-ups written to " + DIR.getPath());
    }

    /** The Inspector's working-set section. */
    private static void write(String name, int width, int textSize,
                              boolean dark) throws IOException {
        UIManager.put("defaultFont",
                new Font(Font.SANS_SERIF, Font.PLAIN, textSize));
        Font plain = new Font(Font.SANS_SERIF, Font.PLAIN, textSize);
        Color fg = dark ? new Color(220, 220, 220) : new Color(30, 30, 30);
        Color quiet = dark ? new Color(150, 150, 150)
                : new Color(110, 110, 110);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel heading = new JLabel("Working set · "
                + MEMBERS.size() + " objects");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD,
                textSize + 2));
        heading.setForeground(fg);
        heading.setAlignmentX(0.0f);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(6));

        for (Member member : MEMBERS) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setOpaque(false);
            row.setAlignmentX(0.0f);
            JLabel leadDot = new JLabel(member.lead() ? "◉ " : "   ");
            leadDot.setFont(plain);
            leadDot.setForeground(fg);
            row.add(leadDot);
            JLabel label = new JLabel(member.name());
            label.setFont(member.lead()
                    ? plain.deriveFont(Font.BOLD) : plain);
            label.setForeground(fg);
            row.add(label);
            if (member.offPage()) {
                JLabel off = new JLabel("  off this page");
                off.setFont(plain.deriveFont(Font.ITALIC,
                        (float) textSize - 1));
                off.setForeground(quiet);
                row.add(off);
            }
            row.add(Box.createHorizontalGlue());
            JLabel remove = new JLabel("✕ ");
            remove.setFont(plain);
            remove.setForeground(quiet);
            row.add(remove);
            row.setMaximumSize(new Dimension(width - 24,
                    textSize + 10));
            panel.add(row);
        }
        panel.add(Box.createVerticalStrut(8));
        JButton clear = new JButton("Clear selection");
        clear.setFont(plain);
        clear.setAlignmentX(0.0f);
        panel.add(clear);

        int height = (MEMBERS.size() + 4) * (textSize + 12) + 20;
        paint(panel, name, width, height, textSize, dark);
    }

    /** The Accumulate toggle, off and on, side by side. */
    private static void accumulate(String name, int textSize)
            throws IOException {
        UIManager.put("defaultFont",
                new Font(Font.SANS_SERIF, Font.PLAIN, textSize));
        JPanel strip = new JPanel();
        strip.setLayout(new BoxLayout(strip, BoxLayout.X_AXIS));
        strip.setOpaque(false);
        strip.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JToggleButton off = new JToggleButton("Accumulate");
        JToggleButton on = new JToggleButton("Accumulate");
        on.setSelected(true);
        JLabel legend = new JLabel("  off · on - gestures add and"
                + " remove without discarding the rest");
        legend.setFont(new Font(Font.SANS_SERIF, Font.ITALIC,
                textSize - 1));
        legend.setForeground(new Color(110, 110, 110));
        strip.add(off);
        strip.add(Box.createHorizontalStrut(8));
        strip.add(on);
        strip.add(legend);
        paint(strip, name, 660, 54, textSize, false);
    }

    private static void paint(JPanel panel, String name, int width,
                              int height, int textSize, boolean dark)
            throws IOException {
        panel.setSize(width, height);
        layOut(panel);
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(
                    java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(dark ? new Color(30, 30, 32) : Color.WHITE);
            g.fillRect(0, 0, width, height);
            panel.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png",
                new File(DIR, "selection-" + name + ".png"));
        System.out.printf(Locale.ROOT, "  selection-%s (%d px, %d pt%s)%n",
                name, width, textSize, dark ? ", dark" : "");
    }

    /** Lays a component tree out without a window to do it for us. */
    private static void layOut(java.awt.Component component) {
        component.doLayout();
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }
}
