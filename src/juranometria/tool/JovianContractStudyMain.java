package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/**
 * The Jupiter and Galilean-moons table, proposed before it is built
 * (Sprint 44, issue #472).
 *
 * <p>Every number a mock-up shows is read from the kept JPL Horizons
 * responses under {@code docs/studies/jovian-system/horizons/} for the
 * named instants of 11 December 2026 at Oslo - the sprint's named
 * fixture - never typed here; the mock-ups are arrangements and words,
 * and the words are the study's proposal in both languages, marked as
 * such until the reader's table (#474) carries them in the language
 * files. The diagrams draw the frames, the light paths and the
 * visibility states the contract settles.
 *
 * <p>Painted off screen with the application's theme, as the
 * contract's headless process requires. Standard output is the
 * portable report; what this desktop draws is {@code platform.md}
 * beside it; the images carry the {@code controls-} prefix of
 * widget-rendered inspection imagery.
 */
public final class JovianContractStudyMain {

    private static final Path DIR = Path.of("docs/studies/jovian-system");
    private static final Path HORIZONS = DIR.resolve("horizons");
    private static final String[] LANGUAGES = {"en", "nb-NO"};
    private static final int WIDTH = 360;
    private static final String INSIDE = "2026-Dec-11 22:45:00";
    private static final String BEFORE = "2026-Dec-11 22:30:00";
    /** IMCCE's centre has left Jupiter, the disc still overlaps: the definition boundary (ruled on #472). */
    private static final String BOUNDARY = "2026-Dec-11 22:55:00";
    /** Callisto clear of Jupiter under every definition: the post-Callisto case (ruled on #472). */
    private static final String AFTER = "2026-Dec-11 23:00:00";
    private static final String[] MOONS = {"io", "europa", "ganymede", "callisto"};

    private JovianContractStudyMain() {
    }

    /** One Horizons row of a moon file: the columns the contract reads. */
    private record MoonRow(double x, double y, double positionAngle, double separation,
                           String visibility, double diameter, double deltaAu) {
    }

    /** One Horizons row of the Jupiter file. */
    private record JupiterRow(double raDeg, double decDeg, double azimuth, double elevation,
                              double illuminated, double diameter, double poleAngle,
                              double poleDistance, double deltaAu, double phaseAngle) {
    }

    private static Map<String, String> words(String language) {
        boolean en = language.equals("en");
        Map<String, String> w = new LinkedHashMap<>();
        w.put("jupiter", "Jupiter");
        w.put("io", "Io");
        w.put("europa", "Europa");
        w.put("ganymede", en ? "Ganymede" : "Ganymedes");
        w.put("callisto", "Callisto");
        w.put("instant", en ? "Instant (UTC)" : "Tidspunkt (UTC)");
        w.put("ra", en ? "Right ascension (J2000)" : "Rektascensjon (J2000)");
        w.put("dec", en ? "Declination (J2000)" : "Deklinasjon (J2000)");
        w.put("altitude", en ? "Altitude" : "Høyde");
        w.put("azimuth", en ? "Azimuth" : "Asimut");
        w.put("distance", en ? "Distance" : "Avstand");
        w.put("diameter", en ? "Apparent diameter, equator / poles" : "Tilsynelatende diameter, ekvator / poler");
        w.put("illuminated", en ? "Illuminated" : "Belyst");
        w.put("pole", en ? "North pole, position angle" : "Nordpol, posisjonsvinkel");
        w.put("moon", en ? "Moon" : "Måne");
        w.put("side", en ? "Side" : "Side");
        w.put("east", en ? "east" : "øst");
        w.put("west", en ? "west" : "vest");
        w.put("north", en ? "north" : "nord");
        w.put("south", en ? "south" : "sør");
        w.put("eastwest", en ? "East–west" : "Øst–vest");
        w.put("northsouth", en ? "North–south" : "Nord–sør");
        w.put("separation", en ? "Separation" : "Avstand fra Jupiter");
        w.put("radii", en ? "Jupiter radii" : "Jupiter-radier");
        w.put("state", en ? "State" : "Tilstand");
        w.put("clear", en ? "clear of Jupiter" : "klar av Jupiter");
        w.put("transit", en ? "in front of Jupiter" : "foran Jupiter");
        w.put("occulted", en ? "behind Jupiter" : "bak Jupiter");
        w.put("eclipsed", en ? "in Jupiter's shadow" : "i Jupiters skygge");
        w.put("partly", en ? "partly in Jupiter's shadow" : "delvis i Jupiters skygge");
        w.put("occultedEclipsed", en ? "behind Jupiter, in its shadow" : "bak Jupiter, i skyggen");
        w.put("configuration", en ? "The four moons" : "De fire månene");
        w.put("noObserver", en ? "No place is set in Place and Time, so there is nothing to compute for."
                : "Ingen sted er satt i Sted og tid, så det er ingenting å regne ut for.");
        w.put("outside", en ? "Jupiter is computed for civil dates from 1900 to 2100; 2101-01-01 is outside that."
                : "Jupiter regnes ut for sivile datoer fra 1900 til 2100; 2101-01-01 er utenfor.");
        w.put("tooMany", en ? "A range of 1 000 rows at most: every 6 hours over 400 days asks 1 601."
                : "En rekke på høyst 1 000 rader: hver 6. time over 400 dager ber om 1 601.");
        w.put("update", en ? "Update from Place and Time" : "Oppdater fra Sted og tid");
        return w;
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIR);
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        StringBuilder report = new StringBuilder();
        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "The Jupiter and Galilean-moons table, as this desktop draws the proposal",
                "Sprint 44, issue #472.");
        List<String> images = new ArrayList<>();
        Map<String, JupiterRow> jupiter = new LinkedHashMap<>();
        Map<String, Map<String, MoonRow>> moons = new LinkedHashMap<>();
        for (String instant : new String[] {BEFORE, INSIDE, BOUNDARY, AFTER}) {
            jupiter.put(instant, jupiterRow(instant));
            Map<String, MoonRow> at = new LinkedHashMap<>();
            for (String moon : MOONS) {
                at.put(moon, moonRow(moon, instant));
            }
            moons.put(instant, at);
        }
        preface(report, jupiter, moons);
        // The range mock-ups sample the three unambiguous instants; the
        // boundary case stays in the report's tables.
        Map<String, JupiterRow> sampled = new LinkedHashMap<>(jupiter);
        sampled.remove(BOUNDARY);
        Map<String, Map<String, MoonRow>> sampledMoons = new LinkedHashMap<>(moons);
        sampledMoons.remove(BOUNDARY);
        try {
            SwingUtilities.invokeAndWait(() -> UIManager.put("defaultFont", null));
            platform.append("## Widths and heights\n\n")
                    .append("Every mock-up at the Controller's default width, ")
                    .append(WIDTH).append(" px, light appearance unless named dark.\n\n")
                    .append("| image | width | height |\n|---|---:|---:|\n");
            for (String language : LANGUAGES) {
                Map<String, String> w = words(language);
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    List<String> names = new ArrayList<>();
                    names.add(write(card(w, jupiter.get(INSIDE), INSIDE), language, "1-jupiter-card", platform));
                    names.add(write(moonTable(w, jupiter.get(INSIDE), moons.get(INSIDE), true), language,
                            "2-moons-table", platform));
                    names.add(write(grouped(w, sampled, sampledMoons), language, "3-range-grouped", platform));
                    names.add(write(flat(w, sampled, sampledMoons), language, "4-range-flat", platform));
                    names.add(write(refused(w), language, "5-refused", platform));
                    names.add(write(group(w, jupiter.get(INSIDE), moons.get(INSIDE)), language,
                            "6-controller-group", platform));
                    return names;
                }));
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(true);
                    return List.of(write(moonTable(w, jupiter.get(INSIDE), moons.get(INSIDE), true),
                            language, "2-moons-table-dark", platform));
                }));
            }
            onEdt(() -> {
                juranometria.app.UiTheme.apply(false);
                return null;
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                UIManager.put("defaultFont", font);
                try {
                    UIManager.setLookAndFeel(before);
                } catch (javax.swing.UnsupportedLookAndFeelException gone) {
                    throw new IllegalStateException(gone);
                }
            });
        }
        images.add(diagramFrames());
        images.add(diagramStates());
        images.add(diagramFigure(jupiter.get(INSIDE), moons.get(INSIDE)));
        vocabulary(report);
        report.append("## The images\n\n");
        for (String image : images) {
            report.append("- `").append(image).append("`\n");
        }
        System.out.print(PlatformEvidence.portable(report.toString()));
        PlatformEvidence.write(platform, DIR.resolve("platform.md").toString());
    }

    // ---- the fixtures ------------------------------------------------

    private static List<String> fields(String file, String instant) throws java.io.IOException {
        Path path = HORIZONS.resolve(file);
        String text = Files.readString(path, StandardCharsets.UTF_8);
        int from = text.indexOf("$$SOE");
        int to = text.indexOf("$$EOE");
        if (from < 0 || to < 0) {
            throw new IllegalStateException(path + " is not a complete Horizons response");
        }
        for (String line : text.substring(from + 5, to).strip().split("\n")) {
            if (line.contains(instant)) {
                List<String> f = new ArrayList<>();
                for (String cell : line.split(",")) {
                    f.add(cell.strip());
                }
                return f;
            }
        }
        throw new IllegalStateException(path + " holds no row at " + instant);
    }

    private static JupiterRow jupiterRow(String instant) throws java.io.IOException {
        List<String> f = fields("named-jupiter-oslo.txt", instant);
        return new JupiterRow(d(f.get(3)), d(f.get(4)), d(f.get(7)), d(f.get(8)), d(f.get(9)),
                d(f.get(10)), d(f.get(11)), d(f.get(12)), d(f.get(13)), d(f.get(15)));
    }

    private static MoonRow moonRow(String moon, String instant) throws java.io.IOException {
        List<String> f = fields("named-" + moon + "-oslo.txt", instant);
        return new MoonRow(d(f.get(9)), d(f.get(10)), d(f.get(11)), d(f.get(12)),
                f.get(13).replace("/", "").strip(), d(f.get(14)), d(f.get(15)));
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }

    // ---- the words a value is shown with -----------------------------

    private static String state(Map<String, String> w, String code) {
        return switch (code) {
            case "t" -> w.get("transit");
            case "O" -> w.get("occulted");
            case "u", "p" -> code.equals("u") ? w.get("eclipsed") : w.get("partly");
            case "U", "P" -> w.get("occultedEclipsed");
            default -> w.get("clear");
        };
    }

    private static String hms(double raDeg) {
        double hours = raDeg / 15.0;
        int h = (int) hours;
        double m = (hours - h) * 60.0;
        int mi = (int) m;
        double s = (m - mi) * 60.0;
        return String.format(Locale.ROOT, "%02dh %02dm %04.1fs", h, mi, s);
    }

    private static String dms(double deg) {
        String sign = deg < 0 ? "−" : "+";
        double a = Math.abs(deg);
        int d = (int) a;
        double m = (a - d) * 60.0;
        int mi = (int) m;
        double s = (m - mi) * 60.0;
        return String.format(Locale.ROOT, "%s%02d° %02d′ %02.0f″", sign, d, mi, s);
    }

    private static String num(String language, double value, int decimals) {
        String s = String.format(Locale.ROOT, "%,." + decimals + "f", value);
        return language.equals("en") ? s : s.replace(',', ' ').replace('.', ',');
    }

    private static String arcsec(String language, double value, int decimals) {
        return num(language, value, decimals) + "″";
    }

    // ---- the arrangements ---------------------------------------------

    private static JComponent card(Map<String, String> w, JupiterRow j, String instant) {
        String language = w.get("ganymede").equals("Ganymede") ? "en" : "nb-NO";
        JPanel card = column();
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        card.add(heading(w.get("jupiter") + " · " + instant.replace("2026-Dec-11", "2026-12-11") + " UTC"));
        double polar = j.diameter * 66854.0 / 71492.0;
        String[][] rows = {
                {w.get("ra"), hms(j.raDeg)},
                {w.get("dec"), dms(j.decDeg)},
                {w.get("altitude"), num(language, j.elevation, 1) + "°"},
                {w.get("azimuth"), num(language, j.azimuth, 1) + "°"},
                {w.get("distance"), num(language, j.deltaAu, 3) + " AU"},
                {w.get("diameter"), arcsec(language, j.diameter, 1) + " / " + arcsec(language, polar, 1)},
                {w.get("illuminated"), num(language, j.illuminated, 1) + " %"},
                {w.get("pole"), num(language, j.poleAngle, 1) + "°"},
        };
        for (String[] row : rows) {
            card.add(line(row[0], row[1]));
        }
        return card;
    }

    private static JComponent moonTable(Map<String, String> w, JupiterRow j,
                                        Map<String, MoonRow> rows, boolean heading) {
        String language = w.get("ganymede").equals("Ganymede") ? "en" : "nb-NO";
        String[] columns = {w.get("moon"), w.get("side"), w.get("eastwest"), w.get("northsouth"),
                w.get("separation"), w.get("radii"), w.get("state")};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        double jupiterRadius = j.diameter / 2.0;
        for (String moon : MOONS) {
            MoonRow m = rows.get(moon);
            model.addRow(new Object[] {w.get(moon),
                    m.x >= 0 ? w.get("east") : w.get("west"),
                    arcsec(language, Math.abs(m.x), 1) + " " + (m.x >= 0 ? w.get("east") : w.get("west")).charAt(0),
                    arcsec(language, Math.abs(m.y), 1) + " " + (m.y >= 0 ? w.get("north") : w.get("south")).charAt(0),
                    arcsec(language, m.separation, 1),
                    num(language, m.separation / jupiterRadius, 2),
                    state(w, m.visibility)});
        }
        JTable table = new JTable(model);
        table.setName("jupiterMoons");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {76, 44, 78, 78, 84, 84, 160};
        for (int c = 0; c < widths.length; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }
        JPanel holder = column();
        if (heading) {
            holder.add(heading(w.get("configuration")));
        }
        JPanel tableBox = new JPanel(new BorderLayout());
        tableBox.add(table.getTableHeader(), BorderLayout.NORTH);
        tableBox.add(table, BorderLayout.CENTER);
        tableBox.setAlignmentX(0f);
        holder.add(tableBox);
        return holder;
    }

    private static JComponent grouped(Map<String, String> w, Map<String, JupiterRow> jupiter,
                                      Map<String, Map<String, MoonRow>> moons) {
        JPanel holder = column();
        holder.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        for (String instant : jupiter.keySet()) {
            holder.add(heading(instant.replace("2026-Dec-11", "2026-12-11") + " UTC"));
            holder.add(moonTable(w, jupiter.get(instant), moons.get(instant), false));
            holder.add(Box.createVerticalStrut(8));
        }
        return holder;
    }

    private static JComponent flat(Map<String, String> w, Map<String, JupiterRow> jupiter,
                                   Map<String, Map<String, MoonRow>> moons) {
        String language = w.get("ganymede").equals("Ganymede") ? "en" : "nb-NO";
        String[] columns = {w.get("instant"), w.get("moon"), w.get("eastwest"), w.get("northsouth"),
                w.get("separation"), w.get("radii"), w.get("state")};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        for (String instant : jupiter.keySet()) {
            double jupiterRadius = jupiter.get(instant).diameter / 2.0;
            for (String moon : MOONS) {
                MoonRow m = moons.get(instant).get(moon);
                model.addRow(new Object[] {instant.replace("2026-Dec-11", "2026-12-11"), w.get(moon),
                        arcsec(language, Math.abs(m.x), 1) + " " + (m.x >= 0 ? w.get("east") : w.get("west")).charAt(0),
                        arcsec(language, Math.abs(m.y), 1) + " " + (m.y >= 0 ? w.get("north") : w.get("south")).charAt(0),
                        arcsec(language, m.separation, 1), num(language, m.separation / jupiterRadius, 2),
                        state(w, m.visibility)});
            }
        }
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {124, 76, 78, 78, 84, 84, 160};
        for (int c = 0; c < widths.length; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }
        JPanel tableBox = new JPanel(new BorderLayout());
        tableBox.add(table.getTableHeader(), BorderLayout.NORTH);
        tableBox.add(table, BorderLayout.CENTER);
        return tableBox;
    }

    private static JComponent refused(Map<String, String> w) {
        JPanel holder = column();
        holder.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        for (String key : new String[] {"noObserver", "outside", "tooMany"}) {
            JLabel label = new JLabel("<html><body style='width: 300px'>" + w.get(key) + "</body></html>");
            label.setAlignmentX(0f);
            holder.add(label);
            holder.add(Box.createVerticalStrut(10));
        }
        return holder;
    }

    private static JComponent group(Map<String, String> w, JupiterRow j, Map<String, MoonRow> rows) {
        JPanel holder = column();
        holder.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        javax.swing.JToggleButton head = new javax.swing.JToggleButton("▾ " + w.get("jupiter"), true);
        head.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        head.setAlignmentX(0f);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, head.getPreferredSize().height));
        holder.add(head);
        holder.add(card(w, j, INSIDE));
        holder.add(moonTable(w, j, rows, true));
        javax.swing.JButton update = new javax.swing.JButton(w.get("update"));
        update.setAlignmentX(0f);
        holder.add(Box.createVerticalStrut(6));
        holder.add(update);
        return holder;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(0f);
        return p;
    }

    private static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setAlignmentX(0f);
        label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        return label;
    }

    private static JComponent line(String name, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setAlignmentX(0f);
        JLabel key = new JLabel(name);
        key.setForeground(UIManager.getColor("Label.disabledForeground"));
        row.add(key, BorderLayout.WEST);
        JLabel v = new JLabel(value);
        v.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        row.add(v, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 2));
        return row;
    }

    // ---- painting --------------------------------------------------------

    private static String write(JComponent content, String language, String stem,
                                StringBuilder platform) throws java.io.IOException {
        String name = "controls-jovian-" + language + "-" + stem + ".png";
        JPanel shell = new JPanel(new BorderLayout());
        shell.add(content, BorderLayout.NORTH);
        int width = Math.max(WIDTH, Math.min(content.getPreferredSize().width, 900));
        shell.setSize(width, 10);
        layOut(shell);
        shell.setSize(width, shell.getPreferredSize().height);
        layOut(shell);
        BufferedImage image = new BufferedImage(shell.getWidth(), Math.max(1, shell.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            shell.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        platform.append("| `").append(name).append("` | ").append(image.getWidth())
                .append(" | ").append(image.getHeight()).append(" |\n");
        return name;
    }

    private static void layOut(Component at) {
        if (at instanceof Container container) {
            container.invalidate();
            container.doLayout();
            for (Component child : container.getComponents()) {
                layOut(child);
            }
        }
    }

    // ---- the diagrams ---------------------------------------------------

    private static Graphics2D canvas(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(Color.BLACK);
        return g;
    }

    private static void arrow(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        double a = Math.atan2(y2 - y1, x2 - x1);
        for (int s = -1; s <= 1; s += 2) {
            g.draw(new Line2D.Double(x2, y2, x2 - 9 * Math.cos(a + s * 0.4), y2 - 9 * Math.sin(a + s * 0.4)));
        }
    }

    /** Frames and the two light paths: observer, Jupiter, a moon, the Sun. */
    private static String diagramFrames() throws java.io.IOException {
        BufferedImage image = new BufferedImage(720, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(image);
        g.drawString("Frames and light time: what each quantity is measured from", 16, 22);
        // Sun, Jupiter, moon, observer
        g.setColor(new Color(240, 170, 20));
        g.fill(new Ellipse2D.Double(50, 160, 40, 40));
        g.setColor(Color.BLACK);
        g.drawString("Sun, at the instant light left it for the moon", 20, 225);
        g.setColor(new Color(210, 160, 110));
        g.fill(new Ellipse2D.Double(470, 150, 60, 56));
        g.setColor(Color.BLACK);
        g.drawString("Jupiter, at its own emission instant", 420, 232);
        g.setColor(new Color(120, 120, 120));
        g.fill(new Ellipse2D.Double(560, 120, 12, 12));
        g.setColor(Color.BLACK);
        g.drawString("moon, at its emission instant", 540, 112);
        g.setColor(new Color(60, 100, 200));
        g.fill(new Ellipse2D.Double(650, 290, 14, 14));
        g.setColor(Color.BLACK);
        g.drawString("observer, on the rotating Earth (UT1 = UTC), at the civil instant", 330, 318);
        g.setColor(new Color(60, 100, 200));
        arrow(g, 657, 290, 566, 134);
        arrow(g, 657, 290, 505, 203);
        g.setColor(new Color(240, 170, 20));
        arrow(g, 90, 180, 468, 178);
        arrow(g, 90, 180, 560, 128);
        g.setColor(Color.BLACK);
        g.drawString("down-leg: each body where it was one light-time earlier;", 300, 262);
        g.drawString("aberrated by the observer's velocity for the apparent place", 300, 278);
        g.drawString("up-leg: the Sun as Jupiter and the moon see it; the shadow is", 100, 140);
        g.drawString("cast from where the Sun was one Sun–Jupiter light-time earlier", 100, 156);
        g.drawString("Astrometric J2000: the down-leg direction without aberration.  Apparent of date: aberrated,", 16, 340);
        g.drawString("precessed and nutated to the true equator and equinox of date.  Horizontal: airless, at sea level.", 16, 354);
        g.dispose();
        String name = "controls-jovian-7-frames-light-time.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name;
    }

    /** The visibility states: plane-of-sky offset against observer-line depth, and the shadow. */
    private static String diagramStates() throws java.io.IOException {
        BufferedImage image = new BufferedImage(720, 420, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(image);
        g.drawString("Visibility states: the plane of the sky is not enough", 16, 22);
        // Left: seen from above the orbit plane. Observer at the bottom, Sun to the upper left.
        int cx = 220;
        int cy = 220;
        g.setColor(new Color(210, 160, 110));
        g.fill(new Ellipse2D.Double(cx - 40, cy - 37, 80, 74));
        g.setColor(Color.BLACK);
        g.drawString("Jupiter", cx - 22, cy + 4);
        // observer line of sight (vertical), Sun direction (tilted by the phase angle)
        g.setColor(new Color(60, 100, 200));
        g.draw(new Line2D.Double(cx, cy + 150, cx, cy - 170));
        g.drawString("line of sight, from the observer", cx + 8, cy - 160);
        g.setColor(new Color(240, 170, 20));
        double phase = Math.toRadians(11.0);
        double sx = cx - 160 * Math.sin(phase);
        double sy = cy - 160 * Math.cos(phase);
        g.draw(new Line2D.Double(cx, cy, sx, sy));
        g.drawString("to the Sun (phase angle up to 12°)", (int) sx - 215, (int) sy + 14);
        // shadow cone on the far side of the Sun
        g.setColor(new Color(90, 90, 90, 70));
        double ax = Math.sin(phase);
        double ay = Math.cos(phase);
        int[] px = {(int) (cx - 40 * ay), (int) (cx + 40 * ay), (int) (cx + 40 * ay + 190 * ax * 0.9),
                (int) (cx - 40 * ay + 190 * ax * 0.9)};
        int[] py = {(int) (cy + 40 * ax), (int) (cy - 40 * ax), (int) (cy - 40 * ax + 190 * ay * 0.9),
                (int) (cy + 40 * ax + 190 * ay * 0.9)};
        g.fillPolygon(px, py, 4);
        g.setColor(Color.BLACK);
        g.drawString("umbra", cx + 20, cy + 120);
        // moons in each state
        drawMoon(g, cx, cy + 80, "in front: nearer than Jupiter, on the disc");
        drawMoon(g, cx, cy - 95, "behind: farther than Jupiter, on the disc");
        drawMoon(g, cx + 95, cy + 20, "clear: off the disc, sunlit");
        drawMoon(g, (int) (cx + 150 * ax * 0.9), (int) (cy + 150 * ay * 0.9), "in Jupiter's shadow: off the disc, dark");
        g.setColor(Color.BLACK);
        g.drawString("Depth along the line of sight decides in front from behind; the shadow cone decides eclipsed;", 16, 392);
        g.drawString("the disc test uses Jupiter's apparent figure. A moon can be behind and in shadow at once.", 16, 408);
        // Right: the plane of the sky, with the X/Y basis
        int rx = 560;
        int ry = 200;
        g.setColor(new Color(210, 160, 110));
        g.fill(new Ellipse2D.Double(rx - 40, ry - 37, 80, 74));
        g.setColor(Color.BLACK);
        g.drawString("the plane of the sky", rx - 60, ry - 110);
        arrow(g, rx, ry, rx - 120, ry);
        g.drawString("east (+X: RA increasing)", rx - 150, ry - 8);
        arrow(g, rx, ry, rx, ry - 90);
        g.drawString("north (+Y)", rx + 6, ry - 80);
        g.drawString("X = Δα·cos δ, Y = Δδ, arcseconds", rx - 100, ry + 70);
        g.drawString("separation: the angle between the centres;", rx - 100, ry + 90);
        g.drawString("in Jupiter radii: separation / apparent equatorial radius", rx - 100, ry + 106);
        g.dispose();
        String name = "controls-jovian-8-visibility-states.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name;
    }

    private static void drawMoon(Graphics2D g, int x, int y, String caption) {
        g.setColor(new Color(120, 120, 120));
        g.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
        g.setColor(Color.BLACK);
        g.drawString(caption, x + 12, y + 4);
    }

    /** Jupiter's apparent figure and the four moons at the named instant, to scale. */
    private static String diagramFigure(JupiterRow j, Map<String, MoonRow> moons) throws java.io.IOException {
        BufferedImage image = new BufferedImage(720, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(image);
        g.drawString("The apparent figure and the four moons, 2026-12-11 22:45 UTC at Oslo, to scale"
                + " (east left, north up)", 16, 22);
        double scale = 2.0; // pixels per arcsecond
        int cx = 360;
        int cy = 200;
        double a = j.diameter / 2.0 * scale;
        double b = a * 66854.0 / 71492.0;
        AffineTransform saved = g.getTransform();
        g.translate(cx, cy);
        g.rotate(Math.toRadians(-j.poleAngle));
        g.setColor(new Color(210, 160, 110));
        g.fill(new Ellipse2D.Double(-a, -b, 2 * a, 2 * b));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Line2D.Double(0, -b, 0, -b - 14));
        g.setTransform(saved);
        g.drawString(String.format(Locale.ROOT, "equatorial %.1f″, polar %.1f″, pole %.1f° from north through east",
                j.diameter, j.diameter * 66854.0 / 71492.0, j.poleAngle), 16, 340);
        int index = 0;
        for (String moon : MOONS) {
            MoonRow m = moons.get(moon);
            double x = cx - m.x * scale; // east to the left
            double y = cy - m.y * scale;
            double r = Math.max(2.0, m.diameter / 2.0 * scale);
            g.setColor("t".equals(m.visibility) ? new Color(50, 50, 50) : new Color(120, 120, 120));
            g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
            g.setColor(Color.BLACK);
            String word = switch (m.visibility) {
                case "t" -> "in front";
                case "O" -> "behind";
                case "u", "p", "U", "P" -> "in shadow";
                default -> "clear";
            };
            // a leader to a label below the figure, two to a row, so labels never cover the disc
            int lx = 40 + (index % 2) * 340;
            int ly = 290 + (index / 2) * 22;
            g.setColor(new Color(150, 150, 150));
            g.draw(new Line2D.Double(x, y, lx + 40, ly - 14));
            g.setColor(Color.BLACK);
            g.drawString(Character.toUpperCase(moon.charAt(0)) + moon.substring(1) + " · " + word
                    + String.format(Locale.ROOT, " (%.1f″ %s, %.1f″ %s)", Math.abs(m.x),
                            m.x >= 0 ? "E" : "W", Math.abs(m.y), m.y >= 0 ? "N" : "S"), lx, ly);
            index++;
        }
        g.dispose();
        String name = "controls-jovian-9-apparent-figure.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name;
    }

    // ---- the report -------------------------------------------------------

    private static void preface(StringBuilder out, Map<String, JupiterRow> jupiter,
                                Map<String, Map<String, MoonRow>> moons) {
        out.append("# The Jupiter and Galilean-moons table, proposed before it is built\n\n")
                .append("Sprint 44, issue #472. Generated by `juranometria.tool.JovianContractStudyMain`"
                        + " from the kept JPL Horizons\nresponses under `horizons/` for the named"
                        + " instants of 11 December 2026 at Oslo (59.91° N, 10.75° E, sea level):\n"
                        + "every number below and in the mock-ups is read from those files, never"
                        + " typed here. The words are the\nstudy's proposal in both languages, marked"
                        + " as such until the reader's table carries them. How tall and wide\nthe"
                        + " arrangements are drawn is one machine's answer, in `platform.md`"
                        + " beside this.\n\n");
        out.append("## The named instants, as Horizons states them\n\n");
        out.append("| instant (UTC) | Jupiter RA (ICRF) | Dec | altitude | azimuth | distance (AU) |"
                + " equatorial diameter | pole angle | illuminated |\n|---|---|---|---|---|---|---|---|---|\n");
        for (Map.Entry<String, JupiterRow> e : jupiter.entrySet()) {
            JupiterRow j = e.getValue();
            out.append(String.format(Locale.ROOT, "| %s | %.6f° | %.6f° | %.3f° | %.3f° | %.6f | %.3f″ | %.2f° | %.3f %% |%n",
                    e.getKey(), j.raDeg, j.decDeg, j.elevation, j.azimuth, j.deltaAu, j.diameter,
                    j.poleAngle, j.illuminated));
        }
        out.append("\n| instant (UTC) | moon | X (″, +east) | Y (″, +north) | position angle | separation |"
                + " Jupiter radii | Horizons code |\n|---|---|---|---|---|---|---|---|\n");
        for (Map.Entry<String, Map<String, MoonRow>> e : moons.entrySet()) {
            double radius = jupiter.get(e.getKey()).diameter / 2.0;
            for (String moon : MOONS) {
                MoonRow m = e.getValue().get(moon);
                out.append(String.format(Locale.ROOT, "| %s | %s | %+.3f | %+.3f | %.3f° | %.3f″ | %.3f | %s |%n",
                        e.getKey(), moon, m.x, m.y, m.positionAngle, m.separation,
                        m.separation / radius, m.visibility.isEmpty() ? "*" : m.visibility));
            }
        }
        out.append("\nA Jupiter radius here is Horizons' equatorial angular radius (half of `Ang-diam`);"
                + " the Horizons code is\nlimb-to-limb with equatorial radii (`t` in front, `O` behind,"
                + " `u`/`p` total/partial umbral eclipse, `U`/`P` both,\n`*` clear), as its legend states.\n\n");
    }

    private static void vocabulary(StringBuilder out) {
        out.append("## The words, proposed\n\n| key | English | Norsk bokmål |\n|---|---|---|\n");
        Map<String, String> en = words("en");
        Map<String, String> nb = words("nb-NO");
        for (String key : en.keySet()) {
            out.append("| `").append(key).append("` | ").append(en.get(key)).append(" | ")
                    .append(nb.get(key)).append(" |\n");
        }
        out.append('\n');
    }

    private interface EdtWork<T> {
        T run() throws Exception;
    }

    private static <T> T onEdt(EdtWork<T> work) throws Exception {
        Object[] result = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                result[0] = work.run();
            } catch (Exception e) {
                failed[0] = e;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }
}
