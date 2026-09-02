package juranometria.tool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/**
 * What the <strong>On this page</strong> sidebar would look like
 * (Sprint 24, issue #214).
 *
 * <p>Mock-ups, not production: the panel here is a plain table built
 * from the study's own measured inventories, so the gate can be
 * judged on how it reads at real densities rather than on a
 * description of how it would read. The rows are real - the same
 * pages, the same objects, the same visibility states the study
 * measured - because a mock-up filled with invented rows tests
 * nothing.
 *
 * <p>Drawn into images rather than shown in a window, so this runs
 * anywhere and reproduces byte for byte.
 */
public final class OnThisPageMockupMain {

    private OnThisPageMockupMain() {
    }

    private static final File DIR = new File("docs/studies/on-this-page");

    private record Row(String identity, String kind, String magnitude,
                       String from, String visibility) {
    }

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        com.formdev.flatlaf.FlatLightLaf.setup();

        write("released", releasedPage(), 320, 12, false);
        write("released-enlarged", releasedPage(), 320, 18, false);
        write("released-narrow", releasedPage(), 240, 12, false);
        write("released-dark", releasedPage(), 320, 12, true);
        write("dense", densePage(), 320, 12, false);
        write("dense-enlarged", densePage(), 320, 18, false);
        write("hidden-family", hiddenFamilyPage(), 320, 12, false);
        write("empty", List.of(), 320, 12, false);

        System.out.println("mock-ups written to " + DIR.getPath());
    }

    /**
     * The released M 31 page, as the study measured it: eight
     * deep-sky rows in the decided order, four named stars, and the
     * counted line for the rest.
     */
    private static List<Row> releasedPage() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("M 31", "galaxy", "3.4 V", "0.00°", "drawn"));
        rows.add(new Row("M 32", "galaxy", "8.1 V", "0.40°", "drawn"));
        rows.add(new Row("M 110", "galaxy", "8.2 V", "0.61°", "drawn"));
        rows.add(new Row("NGC 317A", "galaxy", "13.6 B", "3.74°", "drawn"));
        rows.add(new Row("NGC 317B", "galaxy", "13.9 B", "3.73°", "drawn"));
        rows.add(new Row("IC 1550", "galaxy", "15.0 B", "4.67°", "drawn"));
        rows.add(new Row("NGC 206", "star cloud", "not recorded",
                "0.67°", "no chart symbol"));
        rows.add(new Row("NGC 317", "galaxy", "not recorded",
                "3.73°", "drawn"));
        rows.add(new Row("ν And", "star", "4.5 V", "1.42°", "drawn"));
        rows.add(new Row("μ And", "star", "3.9 V", "2.71°", "drawn"));
        rows.add(new Row("32 And", "star", "5.3 V", "1.42°", "drawn"));
        rows.add(new Row("π And", "star", "5.0 V", "5.77°", "drawn"));
        rows.add(new Row("and 44 further stars", "star", "", "",
                "none named"));
        return rows;
    }

    /** Virgo at 36°, where the detail policy refuses most of it. */
    private static List<Row> densePage() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("M 49", "galaxy", "8.4 V", "2.11°", "drawn"));
        rows.add(new Row("M 58", "galaxy", "9.7 V", "1.16°", "drawn"));
        rows.add(new Row("M 59", "galaxy", "9.6 V", "1.72°", "drawn"));
        rows.add(new Row("M 60", "galaxy", "8.8 V", "2.02°", "drawn"));
        rows.add(new Row("M 61", "galaxy", "9.7 V", "5.34°", "drawn"));
        rows.add(new Row("NGC 4438", "galaxy", "10.0 B", "1.09°", "drawn"));
        rows.add(new Row("IC 3583", "galaxy", "13.3 B", "1.14°",
                "too small at this field"));
        rows.add(new Row("IC 3591", "galaxy", "14.9 B", "1.21°",
                "too small at this field"));
        rows.add(new Row("VCC 1030", "galaxy", "not recorded", "1.32°",
                "too small at this field"));
        rows.add(new Row("and 1,825 further deep-sky objects",
                "", "", "", "1,524 too small here"));
        rows.add(new Row("and 558 further stars", "star", "", "",
                "none named"));
        return rows;
    }

    /** The released page with Galaxies switched off. */
    private static List<Row> hiddenFamilyPage() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("M 31", "galaxy", "3.4 V", "0.00°",
                "hidden by a chart option"));
        rows.add(new Row("M 32", "galaxy", "8.1 V", "0.40°",
                "hidden by a chart option"));
        rows.add(new Row("M 110", "galaxy", "8.2 V", "0.61°",
                "hidden by a chart option"));
        rows.add(new Row("NGC 206", "star cloud", "not recorded",
                "0.67°", "no chart symbol"));
        rows.add(new Row("ν And", "star", "4.5 V", "1.42°", "drawn"));
        rows.add(new Row("and 44 further stars", "star", "", "",
                "none named"));
        return rows;
    }

    /**
     * The family, as one character rather than a word. The chart
     * already teaches these shapes in Chart Options; a sidebar has
     * no room to spell them and no need to.
     */
    private static String glyphFor(String kind) {
        return switch (kind) {
            case "galaxy" -> "\u25cf";
            case "star" -> "\u2217";
            case "star cloud" -> "\u25cb";
            default -> "\u00b7";
        };
    }

    private static void write(String name, List<Row> rows, int width,
                              int textSize, boolean dark)
            throws IOException {
        if (dark) {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } else {
            com.formdev.flatlaf.FlatLightLaf.setup();
        }
        UIManager.put("defaultFont",
                new Font(Font.SANS_SERIF, Font.PLAIN, textSize));

        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.setBorder(javax.swing.BorderFactory
                .createEmptyBorder(10, 12, 10, 12));

        JLabel heading = new JLabel(rows.isEmpty()
                ? "On this page" : "On this page · " + rows.size() + " rows");
        heading.putClientProperty("FlatLaf.styleClass", "h3");
        heading.setFont(heading.getFont().deriveFont(
                Font.BOLD, textSize + 2f));
        panel.add(heading, java.awt.BorderLayout.NORTH);

        if (rows.isEmpty()) {
            JLabel empty = new JLabel("<html>Nothing catalogued is on"
                    + " this page.<br>The sky here is empty of"
                    + " anything the atlas holds.</html>");
            empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, textSize));
            panel.add(empty, java.awt.BorderLayout.CENTER);
        } else {
            DefaultTableModel model = new DefaultTableModel(
                    new Object[] {"Object", "Mag", "From", "On the chart"},
                    0);
            for (Row row : rows) {
                // Kind travels with the name rather than in a column
                // of its own: five text columns truncate to "gal..."
                // and "no char..." at a 320 px sidebar, which the
                // first mock-up showed plainly.
                model.addRow(new Object[] {
                        glyphFor(row.kind()) + " " + row.identity(),
                        row.magnitude(), row.from(), row.visibility()});
            }
            JTable table = new JTable(model);
            table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, textSize));
            table.setRowHeight(textSize + 10);
            table.getTableHeader().setFont(
                    new Font(Font.SANS_SERIF, Font.BOLD, textSize));
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            table.getColumnModel().getColumn(0)
                    .setPreferredWidth(textSize * 9);
            table.getColumnModel().getColumn(1)
                    .setPreferredWidth(textSize * 4);
            table.getColumnModel().getColumn(2)
                    .setPreferredWidth(textSize * 3);
            table.setSelectionMode(javax.swing.ListSelectionModel
                    .MULTIPLE_INTERVAL_SELECTION);
            // Two marked, one of them the lead: what a reader sees
            // after asking about a pair of undrawn objects.
            if (rows.size() > 8) {
                table.setRowSelectionInterval(6, 7);
            }
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(width - 24,
                    Math.min(360, (rows.size() + 2) * (textSize + 10))));
            panel.add(scroll, java.awt.BorderLayout.CENTER);
        }

        int height = rows.isEmpty() ? 140
                : Math.min(420, (rows.size() + 4) * (textSize + 10) + 40);
        panel.setSize(width, height);
        panel.doLayout();
        layOut(panel);

        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(dark ? new Color(30, 30, 32) : Color.WHITE);
            g.fillRect(0, 0, width, height);
            panel.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, "sidebar-" + name + ".png"));
        System.out.printf(Locale.ROOT, "  %s (%d px, %d pt%s)%n",
                name, width, textSize, dark ? ", dark" : "");
    }

    /** Lays a component tree out without a window to do it for us. */
    private static void layOut(java.awt.Component component) {
        component.doLayout();
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                child.setSize(child.getPreferredSize().width == 0
                        ? container.getWidth() : child.getWidth(),
                        child.getHeight());
                layOut(child);
            }
        }
    }
}
