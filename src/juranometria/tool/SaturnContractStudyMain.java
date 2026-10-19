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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import juranometria.chart.ChartViewState;

/**
 * The Saturn table, its moons and the ring geometry, proposed before any
 * of them is built (Sprint 52, issue #574).
 *
 * <p>Every number a mock-up, diagram or table shows is read from the
 * kept JPL Horizons responses under
 * {@code docs/studies/saturn-system/horizons/} and the retained PDS ring
 * table under {@code docs/studies/saturn-system/rings/} - never typed
 * here. Saturn's radii are read from the Horizons response's own
 * physical-data header, the ring radii from the PDS table as extracted.
 * The study computes only what Horizons does not print: the ring
 * plane's planetocentric opening from Horizons' planetodetic
 * sub-observer and sub-solar latitudes (the authority comparison shows
 * that identity to 0.003″), the apparent polar diameter at that opening,
 * and the rings' apparent ellipses. The words are the study's proposal
 * in both languages, marked as such until the reader's table carries
 * them in the language files; the moon states reuse Jupiter's released
 * vocabulary with Saturn's name.
 *
 * <p>Painted off screen with the application's theme, as the contract's
 * headless process requires. Standard output is the portable report;
 * what this desktop draws is {@code platform.md} beside it; the images
 * carry the {@code controls-} prefix of widget-rendered inspection
 * imagery.
 */
public final class SaturnContractStudyMain {

    private static final Path DIR = Path.of("docs/studies/saturn-system");
    private static final Path HORIZONS = DIR.resolve("horizons");
    private static final Path RINGS = DIR.resolve("rings/pds-rings-table.tsv");
    private static final String[] LANGUAGES = {"en", "nb-NO"};
    private static final int WIDTH = 360;
    private static final int PAGE_W = 900;
    private static final int PAGE_H = 700;
    private static final double AU_KM = 149597870.7;
    /** The proposed set, outward from Saturn: the reasons are in docs/decisions/saturn-system.md. */
    static final List<String> PROPOSED = List.of("tethys", "dione", "rhea", "titan", "iapetus");
    static final List<String> INVENTORY = List.of("mimas", "enceladus", "tethys", "dione", "rhea",
            "titan", "hyperion", "iapetus");
    /** The ring edges the diagrams draw, as the PDS table names the features. */
    private static final String[][] EDGES = {
            {"C Ring", "Inner"}, {"B Ring", "Inner"}, {"B Ring", "Outer"},
            {"Cassini Division", "Outer"}, {"A Ring", "Outer"}};

    private SaturnContractStudyMain() {
    }

    // ---- the fixtures ------------------------------------------------

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss", Locale.ROOT);

    /** One Horizons Saturn row: the columns the contract reads. */
    record SaturnRow(LocalDateTime when, double ra, double dec, double azimuth, double elevation,
                     double illuminated, double diameter, double obsSubLat, double sunSubLat,
                     double poleAngle, double deltaAu, double phaseAngle) {
    }

    /** One Horizons moon row. */
    record MoonRow(String moon, LocalDateTime when, double x, double y, double separation,
                   String code, double diameter) {
    }

    /** Saturn's radii as the Horizons response states them, km. */
    record Radii(double equatorial, double polar) {
    }

    static LocalDateTime parse(String stamp) {
        String s = stamp.strip();
        int dot = s.indexOf('.');
        return LocalDateTime.parse(dot < 0 ? s : s.substring(0, dot), STAMP);
    }

    /** The header names and data rows of a kept response. */
    static List<List<String>> table(String file) throws java.io.IOException {
        String text = Files.readString(HORIZONS.resolve(file), StandardCharsets.UTF_8);
        int soe = text.indexOf("$$SOE");
        String[] head = text.substring(0, soe).strip().split("\n");
        List<List<String>> rows = new ArrayList<>();
        rows.add(cells(head[head.length - 2]));
        for (String line : text.substring(soe + 5, text.indexOf("$$EOE")).strip().split("\n")) {
            if (!line.isBlank()) {
                rows.add(cells(line));
            }
        }
        return rows;
    }

    private static List<String> cells(String line) {
        List<String> out = new ArrayList<>();
        for (String c : line.split(",", -1)) {
            out.add(c.strip());
        }
        return out;
    }

    private static int column(List<String> header, String prefix) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).startsWith(prefix)) {
                return i;
            }
        }
        throw new IllegalStateException("no column " + prefix);
    }

    static Radii radii() throws java.io.IOException {
        String text = Files.readString(HORIZONS.resolve("dense-2026-saturn-oslo.txt"), StandardCharsets.UTF_8);
        Matcher eq = Pattern.compile("Equat\\. radius \\(1 bar\\) = (\\d+)").matcher(text);
        Matcher pol = Pattern.compile("Polar radius \\(km\\)\\s+=\\s+(\\d+)").matcher(text);
        if (!eq.find() || !pol.find()) {
            throw new IllegalStateException("the Saturn response states no radii");
        }
        return new Radii(Double.parseDouble(eq.group(1)), Double.parseDouble(pol.group(1)));
    }

    static List<SaturnRow> saturn(String file) throws java.io.IOException {
        List<List<String>> t = table(file);
        List<String> h = t.get(0);
        int[] c = {column(h, "R.A.___"), column(h, "DEC____"), column(h, "Azimuth"), column(h, "Elevation"),
                column(h, "Illu%"), column(h, "Ang-diam"), column(h, "ObsSub-LAT"), column(h, "SunSub-LAT"),
                column(h, "NP.ang"), column(h, "delta"), column(h, "S-T-O")};
        List<SaturnRow> rows = new ArrayList<>();
        for (List<String> f : t.subList(1, t.size())) {
            rows.add(new SaturnRow(parse(f.get(0)), d(f.get(c[0])), d(f.get(c[1])), d(f.get(c[2])),
                    d(f.get(c[3])), d(f.get(c[4])), d(f.get(c[5])), d(f.get(c[6])), d(f.get(c[7])),
                    d(f.get(c[8])), d(f.get(c[9])), d(f.get(c[10]))));
        }
        return rows;
    }

    static List<MoonRow> moon(String moon, String file) throws java.io.IOException {
        List<List<String>> t = table(file);
        List<String> h = t.get(0);
        int x = column(h, "X_(sat-prim)");
        int y = column(h, "Y_(sat-prim)");
        int sep = column(h, "ang-sep");
        int vis = column(h, "vis.");
        int diam = column(h, "Ang-diam");
        List<MoonRow> rows = new ArrayList<>();
        for (List<String> f : t.subList(1, t.size())) {
            String code = f.get(vis).replace("/", "").strip();
            rows.add(new MoonRow(moon, parse(f.get(0)), d(f.get(x)), d(f.get(y)), d(f.get(sep)),
                    code.isEmpty() ? "*" : code, d(f.get(diam))));
        }
        return rows;
    }

    /** The PDS ring table's boundary of a feature, km, as it stands. */
    static double ring(String feature, String edge) throws java.io.IOException {
        List<String> lines = Files.readAllLines(RINGS, StandardCharsets.UTF_8).stream()
                .filter(l -> !l.startsWith("#")).toList();
        List<String> head = List.of(lines.get(0).split("\t", -1));
        int f = head.indexOf("Feature");
        int e = head.indexOf(edge + " Boundary (km)");
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            if (cells[f].equals(feature)) {
                return Double.parseDouble(cells[e].replace(",", ""));
            }
        }
        throw new IllegalStateException("the PDS table has no " + feature);
    }

    private static double d(String s) {
        return Double.parseDouble(s.strip());
    }

    // ---- the geometry ------------------------------------------------

    /** The planetocentric latitude of a planetodetic one on Saturn's spheroid, degrees. */
    static double centric(double detic, Radii r) {
        double k = (r.polar() / r.equatorial()) * (r.polar() / r.equatorial());
        return Math.toDegrees(Math.atan(Math.tan(Math.toRadians(detic)) * k));
    }

    /** The apparent polar diameter at a planetocentric opening B, arcsec, from the equatorial one. */
    static double polarDiameter(double equatorial, double bCentric, Radii r) {
        double b = Math.toRadians(bCentric);
        double projected = Math.sqrt(r.polar() * r.polar() * Math.cos(b) * Math.cos(b)
                + r.equatorial() * r.equatorial() * Math.sin(b) * Math.sin(b));
        return equatorial * projected / r.equatorial();
    }

    /** A ring edge's apparent major axis, arcsec, at a distance in AU. */
    static double ringMajor(double radiusKm, double deltaAu) {
        return 2.0 * Math.toDegrees(Math.asin(radiusKm / (deltaAu * AU_KM))) * 3600.0;
    }

    // ---- the words ---------------------------------------------------

    private static Map<String, String> words(String language) {
        boolean en = language.equals("en");
        Map<String, String> w = new LinkedHashMap<>();
        w.put("saturn", en ? "Saturn" : "Saturn");
        w.put("ra", released(language, "jupitertable.card.ra"));
        w.put("dec", released(language, "jupitertable.card.dec"));
        w.put("altitude", released(language, "jupitertable.card.altitude"));
        w.put("azimuth", released(language, "jupitertable.card.azimuth"));
        w.put("distance", released(language, "jupitertable.card.distance"));
        w.put("diameter", en ? "Apparent diameter, equator / poles" : "Tilsynelatende diameter, ekvator / poler");
        w.put("illuminated", released(language, "jupitertable.card.illuminated"));
        w.put("pole", released(language, "jupitertable.card.pole"));
        w.put("ringOpening", en ? "Rings, opening to the observer" : "Ringene, åpning mot observatøren");
        w.put("ringSun", en ? "Rings, opening to the Sun" : "Ringene, åpning mot Solen");
        w.put("ringExtent", en ? "Rings, apparent size" : "Ringene, tilsynelatende størrelse");
        w.put("faceNorth", en ? "north face" : "nordsiden");
        w.put("faceSouth", en ? "south face" : "sørsiden");
        w.put("unlitFace", en ? "the unlit face is seen" : "den ubelyste siden sees");
        w.put("edgeOn", en ? "edge-on" : "på kant");
        w.put("moonsHeading", en ? "The five moons" : "De fem månene");
        w.put("moon", released(language, "jupitertable.column.moon"));
        w.put("side", released(language, "jupitertable.column.side"));
        w.put("eastwest", released(language, "jupitertable.column.eastwest"));
        w.put("northsouth", released(language, "jupitertable.column.northsouth"));
        w.put("separation", en ? "Separation" : "Avstand fra Saturn");
        w.put("radii", en ? "Saturn radii" : "Saturnradier");
        w.put("state", released(language, "jupitertable.column.state"));
        w.put("east", released(language, "jupitertable.side.east"));
        w.put("west", released(language, "jupitertable.side.west"));
        w.put("clear", en ? "clear of Saturn" : "klar av Saturn");
        w.put("inFront", en ? "in front of Saturn" : "foran Saturn");
        w.put("behind", en ? "behind Saturn" : "bak Saturn");
        w.put("shadow", en ? "in Saturn's shadow" : "i Saturns skygge");
        w.put("partlyShadow", en ? "partly in Saturn's shadow" : "delvis i Saturns skygge");
        w.put("behindShadow", en ? "behind Saturn, in its shadow" : "bak Saturn, i skyggen");
        w.put("mimas", "Mimas");
        w.put("enceladus", "Enceladus");
        w.put("tethys", "Tethys");
        w.put("dione", "Dione");
        w.put("rhea", "Rhea");
        w.put("titan", "Titan");
        w.put("hyperion", "Hyperion");
        w.put("iapetus", "Iapetus");
        w.put("sun", en ? "Sun" : "Solen");
        w.put("mercury", en ? "Mercury" : "Merkur");
        w.put("venus", "Venus");
        w.put("moonBody", en ? "Moon" : "Månen");
        w.put("jupiter", "Jupiter");
        w.put("update", en ? "Update from Place and Time" : "Oppdater fra Sted og tid");
        return w;
    }

    /** Horizons' visibility code, in the proposed words: behind first, then in front, then the shadow. */
    static String state(Map<String, String> w, String code) {
        return switch (code) {
            case "O" -> w.get("behind");
            case "P", "U" -> w.get("behindShadow");
            case "t" -> w.get("inFront");
            case "u" -> w.get("shadow");
            case "p" -> w.get("partlyShadow");
            case "*" -> w.get("clear");
            default -> throw new IllegalStateException("Horizons code " + code);
        };
    }

    /** A released interface string, as the language file states it. */
    private static String released(String language, String key) {
        java.util.Properties p = new java.util.Properties();
        try (var in = Files.newBufferedReader(Path.of("src/resources/interface-language/" + language
                + ".properties"), StandardCharsets.UTF_8)) {
            p.load(in);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        String value = p.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(language + " has no " + key);
        }
        return value;
    }

    // ---- the run -----------------------------------------------------

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIR);
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        StringBuilder report = new StringBuilder();
        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "The Saturn table, as this desktop draws the proposal",
                "Sprint 52, issue #574.");
        Radii radii = radii();
        List<SaturnRow> year = saturn("dense-2026-saturn-oslo.txt");
        List<SaturnRow> weekly = saturn("matrix-7d-saturn-oslo.txt");
        SaturnRow card = year.get(0);
        for (SaturnRow r : year) {
            if (r.deltaAu() < card.deltaAu()) {
                card = r;
            }
        }
        final SaturnRow at = card;
        List<MoonRow> moons = new ArrayList<>();
        for (String m : INVENTORY) {
            for (MoonRow r : moon(m, "dense-2026-" + m + "-oslo.txt")) {
                if (r.when().equals(at.when())) {
                    moons.add(r);
                }
            }
        }
        preface(report, radii, at);
        List<String> images = new ArrayList<>();
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
                    names.add(write(card(w, language, at, radii), language, "1-saturn-card", platform));
                    names.add(write(moonsTable(w, language, moons, at), language, "2-moons-table", platform));
                    names.add(write(group(w, language, at, radii), language, "3-controller-group", platform));
                    return names;
                }));
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(true);
                    return List.of(write(moonsTable(w, language, moons, at), language, "2-moons-table-dark",
                            platform));
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
        images.add(diagramRings(at, radii, moons));
        images.add(diagramOpening(weekly, radii));
        rings(report, at, radii, weekly);
        moonSet(report);
        cartography(report, weekly, radii);
        vocabulary(report);
        report.append("## The images\n\n");
        for (String image : images) {
            report.append("- `").append(image).append("`\n");
        }
        System.out.print(PlatformEvidence.portable(report.toString()));
        PlatformEvidence.write(platform, DIR.resolve("platform.md").toString());
    }

    // ---- formatting --------------------------------------------------

    private static String hms(double raDeg) {
        double h = raDeg / 15.0;
        int hh = (int) Math.floor(h);
        double m = (h - hh) * 60.0;
        int mm = (int) Math.floor(m);
        double s = (m - mm) * 60.0;
        return String.format(Locale.ROOT, "%02dh %02dm %04.1fs", hh, mm, s);
    }

    private static String dms(double deg) {
        String sign = deg < 0 ? "−" : "+";
        double a = Math.abs(deg);
        int d = (int) Math.floor(a);
        double m = (a - d) * 60.0;
        int mi = (int) Math.floor(m);
        double s = (m - mi) * 60.0;
        return String.format(Locale.ROOT, "%s%02d° %02d′ %02.0f″", sign, d, mi, s);
    }

    private static String num(String language, double value, int decimals) {
        String s = String.format(Locale.ROOT, "%,." + decimals + "f", value);
        return language.equals("en") ? s : s.replace(',', ' ').replace('.', ',');
    }

    private static String day(LocalDateTime when) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d %02d:%02d", when.getYear(), when.getMonthValue(),
                when.getDayOfMonth(), when.getHour(), when.getMinute());
    }

    private static String opening(Map<String, String> w, String language, double b) {
        if (Math.abs(b) < 0.05) {
            return num(language, b, 2) + "° (" + w.get("edgeOn") + ")";
        }
        return num(language, Math.abs(b), 2) + "° (" + (b > 0 ? w.get("faceNorth") : w.get("faceSouth")) + ")";
    }

    // ---- the arrangements ---------------------------------------------

    private static JComponent card(Map<String, String> w, String language, SaturnRow r, Radii radii)
            throws java.io.IOException {
        JPanel card = column();
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        card.add(heading(w.get("saturn") + " · " + day(r.when()) + " UTC"));
        double b = centric(r.obsSubLat(), radii);
        double bSun = centric(r.sunSubLat(), radii);
        double outer = ringMajor(ring("A Ring", "Outer"), r.deltaAu());
        String sunFace = opening(w, language, bSun);
        if (Math.signum(b) != Math.signum(bSun)) {
            sunFace += " · " + w.get("unlitFace");
        }
        String[][] rows = {
                {w.get("ra"), hms(r.ra())},
                {w.get("dec"), dms(r.dec())},
                {w.get("altitude"), num(language, r.elevation(), 1) + "°"},
                {w.get("azimuth"), num(language, r.azimuth(), 1) + "°"},
                {w.get("distance"), num(language, r.deltaAu(), 3) + " AU"},
                {w.get("diameter"), num(language, r.diameter(), 1) + "″ / "
                        + num(language, polarDiameter(r.diameter(), b, radii), 1) + "″"},
                {w.get("illuminated"), num(language, r.illuminated(), 1) + " %"},
                {w.get("pole"), num(language, r.poleAngle(), 1) + "°"},
                {w.get("ringOpening"), opening(w, language, b)},
                {w.get("ringSun"), sunFace},
                {w.get("ringExtent"), num(language, outer, 1) + "″ × "
                        + num(language, outer * Math.abs(Math.sin(Math.toRadians(b))), 1) + "″"},
        };
        for (String[] row : rows) {
            card.add(line(row[0], row[1]));
        }
        return card;
    }

    private static JComponent moonsTable(Map<String, String> w, String language, List<MoonRow> moons,
                                         SaturnRow at) {
        String[] columns = {w.get("moon"), w.get("side"), w.get("eastwest"), w.get("northsouth"),
                w.get("separation"), w.get("radii"), w.get("state")};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        String e = released(language, "jupitertable.letter.east");
        String wl = released(language, "jupitertable.letter.west");
        String n = released(language, "jupitertable.letter.north");
        String s = released(language, "jupitertable.letter.south");
        for (MoonRow m : moons) {
            if (!PROPOSED.contains(m.moon())) {
                continue;
            }
            model.addRow(new Object[] {w.get(m.moon()), m.x() >= 0 ? w.get("east") : w.get("west"),
                    num(language, Math.abs(m.x()), 1) + "″ " + (m.x() >= 0 ? e : wl),
                    num(language, Math.abs(m.y()), 1) + "″ " + (m.y() >= 0 ? n : s),
                    num(language, m.separation(), 1) + "″",
                    num(language, m.separation() / (at.diameter() / 2.0), 2),
                    state(w, m.code())});
        }
        JTable table = new JTable(model);
        table.setName("saturnMoons");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {90, 50, 90, 96, 130, 96, 190};
        for (int c = 0; c < widths.length; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }
        JPanel box = column();
        box.add(heading(w.get("moonsHeading")));
        JPanel tableBox = new JPanel(new BorderLayout());
        tableBox.add(table.getTableHeader(), BorderLayout.NORTH);
        tableBox.add(table, BorderLayout.CENTER);
        tableBox.setAlignmentX(0f);
        box.add(tableBox);
        return box;
    }

    /** The Controller's Solar System sections, in the released order with Saturn last and open. */
    private static JComponent group(Map<String, String> w, String language, SaturnRow r, Radii radii)
            throws java.io.IOException {
        JPanel holder = column();
        holder.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        holder.add(section("▸ " + w.get("sun"), false));
        holder.add(section("▸ " + w.get("mercury"), false));
        holder.add(section("▸ " + w.get("venus"), false));
        holder.add(section("▸ " + w.get("moonBody"), false));
        holder.add(section("▸ " + w.get("jupiter"), false));
        holder.add(section("▾ " + w.get("saturn"), true));
        holder.add(card(w, language, r, radii));
        javax.swing.JButton update = new javax.swing.JButton(w.get("update"));
        update.setAlignmentX(0f);
        holder.add(Box.createVerticalStrut(6));
        holder.add(update);
        return holder;
    }

    private static JComponent section(String text, boolean open) {
        javax.swing.JToggleButton head = new javax.swing.JToggleButton(text, open);
        head.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        head.setAlignmentX(0f);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, head.getPreferredSize().height));
        return head;
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
        String name = "controls-saturn-" + language + "-" + stem + ".png";
        JPanel shell = new JPanel(new BorderLayout());
        shell.add(content, BorderLayout.NORTH);
        int width = Math.max(WIDTH, Math.min(content.getPreferredSize().width, 1200));
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

    /**
     * An ellipse of semi-axes a (along the ring's major axis) and b, centred
     * at the origin, turned so that the minor axis lies along the projected
     * pole at position angle pa (north through east) on a north-up,
     * east-left page.
     */
    private static Shape ellipse(double a, double b, double pa) {
        Shape e = new Ellipse2D.Double(-a, -b, 2 * a, 2 * b);
        // the minor axis (page y) turned to the pole: position angle pa points along
        // (-sin pa, -cos pa) on the page
        double turn = Math.atan2(-Math.cos(Math.toRadians(pa)), -Math.sin(Math.toRadians(pa))) + Math.PI / 2;
        return AffineTransform.getRotateInstance(turn).createTransformedShape(e);
    }

    /**
     * The half of the page on the near side of the rings: with the observer
     * north of the ring plane (B > 0) the near half lies away from the
     * projected north pole, and the far half towards it (geometry: a ring
     * point toward the observer projects at -sin B cos B along the pole).
     */
    private static Shape nearHalf(double size, double pa, double b) {
        Shape half = new Rectangle2D.Double(-size, 0, 2 * size, size);   // page +y: below the major axis
        double turn = Math.atan2(-Math.cos(Math.toRadians(pa)), -Math.sin(Math.toRadians(pa))) + Math.PI / 2;
        // page +y before turning is the minor axis's -pole side
        AffineTransform t = AffineTransform.getRotateInstance(turn);
        if (b < 0) {
            t.rotate(Math.PI);
        }
        return t.createTransformedShape(half);
    }

    /** The rings, the globe and the moons at the card's instant, to scale, north up, east left. */
    private static String diagramRings(SaturnRow at, Radii radii, List<MoonRow> moons) throws java.io.IOException {
        BufferedImage image = new BufferedImage(900, 520, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(image);
        double b = centric(at.obsSubLat(), radii);
        double scale = 6.0;   // px per arcsec
        double cx = 450;
        double cy = 270;
        g.drawString(String.format(Locale.ROOT, "Saturn at %s UTC, Oslo (Horizons): north up, east left, %.0f px"
                + " per arcsecond", day(at.when()), scale), 16, 22);
        g.drawString(String.format(Locale.ROOT, "ring opening B = %+.2f° planetocentric (Horizons' planetodetic"
                + " ObsSub-LAT %+.2f°); pole %.1f° from north through east", b, at.obsSubLat(), at.poleAngle()),
                16, 40);
        AffineTransform saved = g.getTransform();
        g.translate(cx, cy);
        double sinB = Math.abs(Math.sin(Math.toRadians(b)));
        Area rings = new Area();
        double[][] bands = {{ring("C Ring", "Inner"), ring("B Ring", "Outer")},
                {ring("Cassini Division", "Outer"), ring("A Ring", "Outer")}};
        for (double[] band : bands) {
            double ao = ringMajor(band[1], at.deltaAu()) / 2.0 * scale;
            double ai = ringMajor(band[0], at.deltaAu()) / 2.0 * scale;
            Area a = new Area(ellipse(ao, ao * sinB, at.poleAngle()));
            a.subtract(new Area(ellipse(ai, ai * sinB, at.poleAngle())));
            rings.add(a);
        }
        Area near = new Area(rings);
        near.intersect(new Area(nearHalf(1000, at.poleAngle(), b)));
        Area far = new Area(rings);
        far.subtract(near);
        double re = at.diameter() / 2.0 * scale;
        double rp = polarDiameter(at.diameter(), b, radii) / 2.0 * scale;
        Color ringInk = new Color(196, 176, 130);
        g.setColor(ringInk);
        g.fill(far);
        g.setColor(new Color(230, 210, 160));
        g.fill(ellipse(re, rp, at.poleAngle()));
        g.setColor(new Color(120, 100, 60));
        g.setStroke(new BasicStroke(1f));
        g.draw(ellipse(re, rp, at.poleAngle()));
        g.setColor(ringInk.darker());
        g.fill(near);
        // the pole
        double pa = Math.toRadians(at.poleAngle());
        g.setColor(Color.BLACK);
        g.draw(new Line2D.Double(0, 0, -Math.sin(pa) * (rp + 30), -Math.cos(pa) * (rp + 30)));
        g.drawString("N pole", (float) (-Math.sin(pa) * (rp + 44)) - 18, (float) (-Math.cos(pa) * (rp + 44)));
        for (MoonRow m : moons) {
            if (!PROPOSED.contains(m.moon())) {
                continue;
            }
            double x = -m.x() * scale;
            double y = -m.y() * scale;
            if (Math.abs(x) > 440 || Math.abs(y) > 240) {
                continue;
            }
            g.setColor(m.code().equals("*") ? Color.BLACK : Color.GRAY);
            g.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
            g.drawString(m.moon(), (float) x + 5, (float) y - 4);
        }
        g.setTransform(saved);
        g.setColor(Color.DARK_GRAY);
        g.drawString("Darker ring ink: the near half (in front of the globe); lighter: the far half (behind it)."
                + " Ring radii from the PDS table: C Ring inner to B Ring outer,", 16, 492);
        g.drawString("and the Cassini Division's outer edge to the A Ring's outer edge. Moons beyond the frame are"
                + " not drawn; grey marks a moon not clear of Saturn.", 16, 508);
        g.dispose();
        String name = "controls-saturn-4-ring-geometry.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name;
    }

    /** The ring opening over 1900-2100: planetocentric against Horizons' planetodetic. */
    private static String diagramOpening(List<SaturnRow> weekly, Radii radii) throws java.io.IOException {
        BufferedImage image = new BufferedImage(900, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(image);
        double x0 = 60;
        double x1 = 870;
        double y0 = 190;
        double perDeg = 4.2;
        double first = weekly.get(0).when().getYear();
        double span = weekly.get(weekly.size() - 1).when().getYear() + 1 - first;
        g.drawString("The ring opening to the Earth at Oslo, weekly 1900-2100 (Horizons): planetocentric B"
                + " (solid) and Horizons' planetodetic ObsSub-LAT (dashed)", 16, 22);
        g.setColor(Color.LIGHT_GRAY);
        g.draw(new Line2D.Double(x0, y0, x1, y0));
        for (int deg = -30; deg <= 30; deg += 10) {
            g.drawString(String.format(Locale.ROOT, "%+d°", deg), 16, (float) (y0 - deg * perDeg + 4));
        }
        for (int year = 1900; year <= 2100; year += 25) {
            double x = x0 + (year - first) / span * (x1 - x0);
            g.drawString(Integer.toString(year), (float) x - 14, 352);
        }
        Path2D centric = new Path2D.Double();
        Path2D detic = new Path2D.Double();
        int crossings = 0;
        double previous = Double.NaN;
        for (SaturnRow r : weekly) {
            double t = r.when().getYear() + (r.when().getDayOfYear() - 1) / 365.25;
            double x = x0 + (t - first) / span * (x1 - x0);
            double b = centric(r.obsSubLat(), radii);
            if (centric.getCurrentPoint() == null) {
                centric.moveTo(x, y0 - b * perDeg);
                detic.moveTo(x, y0 - r.obsSubLat() * perDeg);
            } else {
                centric.lineTo(x, y0 - b * perDeg);
                detic.lineTo(x, y0 - r.obsSubLat() * perDeg);
            }
            if (!Double.isNaN(previous) && previous * b < 0) {
                crossings++;
                g.setColor(new Color(200, 60, 60));
                g.fill(new Ellipse2D.Double(x - 2.5, y0 - 2.5, 5, 5));
            }
            previous = b;
        }
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(centric);
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 3f}, 0f));
        g.draw(detic);
        g.setColor(new Color(200, 60, 60));
        g.drawString(String.format(Locale.ROOT, "%d weekly sign changes (red): the Earth's ring-plane crossings,"
                + " single or triple", crossings), (float) x0, 336);
        g.dispose();
        String name = "controls-saturn-5-ring-opening.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name;
    }

    // ---- the report ------------------------------------------------------

    private static void preface(StringBuilder out, Radii radii, SaturnRow at) {
        out.append("# Saturn table and ring geometry, proposed (#574)\n\n")
                .append("Every number below is read from the kept Horizons responses under"
                        + " `docs/studies/saturn-system/horizons/`\nand the retained PDS ring table"
                        + " (`rings/pds-rings-table.tsv`); none is typed in the study.\n\n")
                .append(String.format(Locale.ROOT,
                        "Saturn's radii, as the Horizons response's physical data states them: %.0f km at the"
                                + " equator (1 bar), %.0f km polar.%n", radii.equatorial(), radii.polar()))
                .append(String.format(Locale.ROOT,
                        "The card's instant is the 2026 daily Oslo row nearest the Earth: %s UTC, %.6f AU.%n%n",
                        day(at.when()), at.deltaAu()));
    }

    private static void rings(StringBuilder out, SaturnRow at, Radii radii, List<SaturnRow> weekly)
            throws java.io.IOException {
        out.append("## The ring geometry at the card's instant\n\n");
        double b = centric(at.obsSubLat(), radii);
        double bSun = centric(at.sunSubLat(), radii);
        out.append(String.format(Locale.ROOT,
                "- opening to the observer: Horizons' planetodetic ObsSub-LAT %+.6f° is %+.4f° planetocentric%n"
                        + "- opening to the Sun: Horizons' SunSub-LAT %+.6f° is %+.4f° planetocentric%n"
                        + "- north pole's position angle (Horizons' NP.ang, of date): %.4f°; the rings' major axis"
                        + " lies at %.4f°%n"
                        + "- the globe: %.3f″ at the equator, %.3f″ pole to pole at this opening%n%n",
                at.obsSubLat(), b, at.sunSubLat(), bSun, at.poleAngle(), (at.poleAngle() + 90.0) % 360.0,
                at.diameter(), polarDiameter(at.diameter(), b, radii)));
        out.append("| PDS feature | edge | radius km | major axis ″ | minor axis ″ |\n|---|---|---:|---:|---:|\n");
        for (String[] edge : EDGES) {
            double km = ring(edge[0], edge[1]);
            double major = ringMajor(km, at.deltaAu());
            out.append(String.format(Locale.ROOT, "| %s | %s | %,.0f | %.3f | %.3f |%n", edge[0],
                    edge[1].toLowerCase(Locale.ROOT), km, major, major * Math.abs(Math.sin(Math.toRadians(b)))));
        }
        out.append("\nThe near half of the rings - the half in front of the globe - lies on the side away"
                + " from the projected north pole\nwhen the observer is north of the ring plane (B > 0), and"
                + " towards it when south: a ring point in the direction of\nthe observer projects at"
                + " −sin B cos B along the projected pole.\n\n");
        double widest = 0;
        SaturnRow wide = null;
        int unlit = 0;
        for (SaturnRow r : weekly) {
            double c = centric(r.obsSubLat(), radii);
            if (Math.abs(c) > Math.abs(widest)) {
                widest = c;
                wide = r;
            }
            if (Math.signum(c) != Math.signum(centric(r.sunSubLat(), radii))) {
                unlit++;
            }
        }
        out.append(String.format(Locale.ROOT,
                "Over 1900-2100 (%d weekly Oslo rows) the widest planetocentric opening is %+.3f° (%s), where"
                        + " Horizons' planetodetic\nObsSub-LAT reads %+.3f°. In %d rows the Earth and the Sun are"
                        + " on opposite sides of the ring plane: the unlit face is seen.%n%n",
                weekly.size(), widest, day(wide.when()).substring(0, 10), wide.obsSubLat(), unlit));
    }

    private static void moonSet(StringBuilder out) throws java.io.IOException {
        out.append("## The moon set, from the fixtures\n\n")
                .append("Separations and Horizons' visibility codes from the weekly Oslo matrix 1900-2100;"
                        + " brightness from Horizons' geocentric\n30-day magnitudes (airless, no extinction)."
                        + " Proposed: the bodies marked yes; the reasons for each are in the decision record.\n\n")
                .append("| moon | proposed | separation ″ (min-max) | diameter ″ (max) | APmag (brightest-faintest)"
                        + " | rows not clear of Saturn |\n|---|---|---|---:|---|---:|\n");
        for (String m : INVENTORY) {
            List<MoonRow> rows = moon(m, "matrix-7d-" + m + "-oslo.txt");
            double min = Double.MAX_VALUE;
            double max = 0;
            double diam = 0;
            int notClear = 0;
            for (MoonRow r : rows) {
                min = Math.min(min, r.separation());
                max = Math.max(max, r.separation());
                diam = Math.max(diam, r.diameter());
                if (!r.code().equals("*")) {
                    notClear++;
                }
            }
            List<List<String>> mags = table("magnitudes-30d-" + m + "-geocentric.txt");
            int col = column(mags.get(0), "APmag");
            double bright = Double.MAX_VALUE;
            double faint = -Double.MAX_VALUE;
            for (List<String> f : mags.subList(1, mags.size())) {
                double v = d(f.get(col));
                bright = Math.min(bright, v);
                faint = Math.max(faint, v);
            }
            out.append(String.format(Locale.ROOT, "| %s | %s | %.1f-%.1f | %.4f | %.2f-%.2f | %d of %d |%n", m,
                    PROPOSED.contains(m) ? "yes" : "no", min, max, diam, bright, faint, notClear, rows.size()));
        }
        out.append('\n');
    }

    private static void cartography(StringBuilder out, List<SaturnRow> weekly, Radii radii)
            throws java.io.IOException {
        double far = 0;
        double near = Double.MAX_VALUE;
        for (SaturnRow r : weekly) {
            far = Math.max(far, r.deltaAu());
            near = Math.min(near, r.deltaAu());
        }
        double aOuter = ring("A Ring", "Outer");
        double globeNear = 0;
        double globeFar = Double.MAX_VALUE;
        for (SaturnRow r : weekly) {
            globeNear = Math.max(globeNear, r.diameter());
            globeFar = Math.min(globeFar, r.diameter());
        }
        out.append("## The cartography handoff\n\n")
                .append(String.format(Locale.ROOT,
                        "Over 1900-2100 the A Ring's outer edge spans %.2f″ (farthest, %.4f AU) to %.2f″ (nearest,"
                                + " %.4f AU) across;%nthe globe's equator %.2f″ to %.2f″. Pixels on the %d × %d page at"
                                + " its centre, each field's own projection,%nthe ladder's fields and then the closer fields:%n%n",
                        ringMajor(aOuter, far), far, ringMajor(aOuter, near), near, globeFar, globeNear,
                        PAGE_W, PAGE_H))
                .append("| field | rings' major axis px (min-max) | globe px (min-max) |\n|---:|---|---|\n");
        List<Double> fields = new ArrayList<>(ChartViewState.fieldWidthSteps());
        for (double closer : ChartViewState.closerFieldSteps()) {
            if (!fields.contains(closer)) {
                fields.add(closer);
            }
        }
        for (double field : fields) {
            double per = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            out.append(String.format(Locale.ROOT, "| %s | %.2f-%.2f | %.2f-%.2f |%n",
                    field >= 1 ? String.format(Locale.ROOT, "%.0f°", field)
                            : String.format(Locale.ROOT, "%.0f′", field * 60.0),
                    ringMajor(aOuter, far) * per, ringMajor(aOuter, near) * per, globeFar * per, globeNear * per));
        }
        out.append('\n');
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
