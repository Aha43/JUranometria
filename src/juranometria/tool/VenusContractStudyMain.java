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
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;

/**
 * The Venus table and the cartography handoff, proposed before either
 * is built (Sprint 47, issue #500).
 *
 * <p>Every number a mock-up, diagram or table shows is read from the
 * kept JPL Horizons responses under {@code docs/studies/venus/horizons/}
 * - the named instants at Oslo with the Sun beside them, the hourly
 * window around the 2026 inferior conjunction, and the weekly Oslo
 * matrix over 1900-2100 - never typed here. The study computes only
 * what Horizons does not print: the bright limb's position angle
 * (Meeus 48.5) from Horizons' own Sun and Venus, in the J2000 frame the
 * chart draws in and of date, and the Sun's apparent radius from its
 * distance and the PCK's radius. The words are the study's proposal in
 * both languages, marked as such until the reader's table carries them
 * in the language files.
 *
 * <p>The cartography handoff measures the diameter on the atlas's own
 * field ladder (read from {@link ChartViewState}) through the
 * production projection, as the Jovian cartography study did, and on
 * the A4 and Letter sheets. It draws no Venus on a chart page.
 *
 * <p>Painted off screen with the application's theme, as the
 * contract's headless process requires. Standard output is the
 * portable report; what this desktop draws is {@code platform.md}
 * beside it; the images carry the {@code controls-} prefix of
 * widget-rendered inspection imagery.
 */
public final class VenusContractStudyMain {

    private static final Path DIR = Path.of("docs/studies/venus");
    private static final Path HORIZONS = DIR.resolve("horizons");
    private static final String[] LANGUAGES = {"en", "nb-NO"};
    private static final int WIDTH = 360;
    private static final int PAGE_W = 900;
    private static final int PAGE_H = 700;
    /** The Sun's radius as NAIF's pck00011.tpc states it (BODY10_RADII), km. */
    private static final double SUN_RADIUS_KM = 695700.0;
    /** Venus's radius as pck00011.tpc and Horizons state it (BODY299_RADII), km. */
    private static final double VENUS_RADIUS_KM = 6051.8;
    /** The mean cloud-top altitude, VIRTIS (Ignatiev et al. 2009), km. */
    private static final double CLOUD_TOP_KM = 74.0;
    private static final double AU_KM = 149597870.7;

    /** The card's instant: the 2026 greatest eastern elongation (Espenak's row). */
    private static final String CARD = "2026-Aug-15 06:00:00";
    /** The range: every 14 days through the 2026-27 apparition, at 21:00 UTC. */
    private static final String RANGE_FILE = "apparition-2026-oslo";
    /** The report's close-up: one row a day at 03:00 through the 2026 inferior conjunction. */
    private static final String CONJUNCTION_FILE = "dense-2026-inferior-conjunction-oslo";
    private static final String CONJUNCTION_HOUR = "03:00:00";

    /**
     * Proposed phase bands on the illuminated fraction k, in percent: the
     * Moon's ruled bands (moon-computation.md, M5), half-open, so the two
     * bodies' words change at the same fractions and χ is "not well-defined"
     * by the same two thresholds.
     */
    static final double NEAR_NEW = 2.0;
    static final double HALF_LOW = 48.0;
    static final double HALF_HIGH = 52.0;
    static final double NEAR_FULL = 98.0;

    /** Proposed readability: a disc at least this wide, its lit and dark parts each at least a pixel. */
    static final double READABLE_DISC_PX = 6.0;
    static final double READABLE_PART_PX = 1.0;

    private VenusContractStudyMain() {
    }

    /** One Horizons Venus row, with the Sun beside it where both were fetched. */
    record VenusRow(LocalDateTime when, double ra, double dec, double raApp, double decApp,
                    double azimuth, double elevation, double magnitude, double illuminated,
                    double diameter, double deltaAu, double elongation, boolean trailing,
                    double phaseAngle, Double chiJ2000, Double chiOfDate, Double sunRadiusArcsec) {

        String phase(Map<String, String> w) {
            if (sunRadiusArcsec != null && elongation * 3600.0 < sunRadiusArcsec + diameter / 2.0) {
                return w.get(phaseAngle > 90.0 ? "transit" : "behindSun");
            }
            if (illuminated < NEAR_NEW) {
                return w.get("nearInferior");
            }
            if (illuminated < HALF_LOW) {
                return w.get("crescent");
            }
            if (illuminated < HALF_HIGH) {
                return w.get("half");
            }
            if (illuminated < NEAR_FULL) {
                return w.get("gibbous");
            }
            return w.get("nearFull");
        }

        String sky(Map<String, String> w) {
            return trailing ? w.get("evening") : w.get("morning");
        }

        /** The Moon's lit-side cell: χ of date with the nearest of sixteen points, or why not. */
        String litSide(Map<String, String> w, String language) {
            if (illuminated < NEAR_NEW) {
                return w.get("litUndefinedNew");
            }
            if (illuminated >= NEAR_FULL) {
                return w.get("litUndefinedFull");
            }
            String[] points = w.get("compass").split(",");
            int index = (int) Math.round(chiOfDate / 22.5) % 16;
            return num(language, chiOfDate, 0) + "° (" + points[index] + ")";
        }
    }

    private static Map<String, String> words(String language) {
        boolean en = language.equals("en");
        Map<String, String> w = new LinkedHashMap<>();
        w.put("venus", "Venus");
        w.put("instant", en ? "Instant (UTC)" : "Tidspunkt (UTC)");
        w.put("ra", en ? "Right ascension (J2000)" : "Rektascensjon (J2000)");
        w.put("dec", en ? "Declination (J2000)" : "Deklinasjon (J2000)");
        w.put("altitude", en ? "Altitude" : "Høyde");
        w.put("azimuth", en ? "Azimuth" : "Asimut");
        w.put("distance", en ? "Distance" : "Avstand");
        w.put("diameter", en ? "Apparent diameter" : "Tilsynelatende diameter");
        w.put("elongation", en ? "Elongation from the Sun" : "Elongasjon fra Solen");
        w.put("phaseAngle", en ? "Phase angle" : "Fasevinkel");
        w.put("phaseAngleNote", en ? "Sun–Venus–observer" : "Sol–Venus–observatør");
        w.put("illuminated", en ? "Illuminated" : "Belyst");
        w.put("phase", en ? "Phase" : "Fase");
        w.put("litSide", en ? "Lit side" : "Belyst side");
        w.put("east", en ? "E" : "Ø");
        w.put("west", en ? "W" : "V");
        w.put("evening", en ? "evening sky" : "kveldshimmelen");
        w.put("morning", en ? "morning sky" : "morgenhimmelen");
        w.put("nearInferior", en ? "near inferior conjunction" : "nær nedre konjunksjon");
        w.put("crescent", en ? "crescent" : "sigd");
        w.put("half", en ? "near half lit" : "nær halvt belyst");
        w.put("gibbous", en ? "gibbous" : "mer enn halvt belyst");
        w.put("nearFull", en ? "near full" : "nær full");
        w.put("transit", en ? "in front of the Sun" : "foran Solen");
        w.put("behindSun", en ? "behind the Sun" : "bak Solen");
        w.put("litUndefinedNew", en ? "not well-defined (near inferior conjunction)"
                : "ikke veldefinert (nær nedre konjunksjon)");
        w.put("litUndefinedFull", en ? "not well-defined (near full)" : "ikke veldefinert (nær full)");
        w.put("compass", en
                ? "north,north-northeast,northeast,east-northeast,east,east-southeast,southeast,south-southeast,"
                        + "south,south-southwest,southwest,west-southwest,west,west-northwest,northwest,north-northwest"
                : "nord,nord-nordøst,nordøst,øst-nordøst,øst,øst-sørøst,sørøst,sør-sørøst,"
                        + "sør,sør-sørvest,sørvest,vest-sørvest,vest,vest-nordvest,nordvest,nord-nordvest");
        w.put("noObserver", en ? "No place is set in Place and Time, so there is nothing to compute for."
                : "Ingen sted er satt i Sted og tid, så det er ingenting å regne ut for.");
        w.put("outside", en ? "Venus is computed for civil dates from 1900-01-01 to 2100-12-31; 2101-01-01 is outside that."
                : "Venus beregnes for sivile datoer fra 1900-01-01 til 2100-12-31; 2101-01-01 er utenfor det.");
        // The released Sun and Moon refusal, read from the language file, with
        // the service's own cap and a range one row longer than it.
        int cap = juranometria.solar.TimeRange.MAX_ROWS;
        w.put("tooMany", java.text.MessageFormat.format(released(language, "solartable.refused.rows"),
                num(language, cap + 1, 0), num(language, cap, 0)));
        w.put("update", en ? "Update from Place and Time" : "Oppdater fra Sted og tid");
        w.put("sun", en ? "Sun" : "Solen");
        w.put("moon", en ? "Moon" : "Månen");
        w.put("jupiter", "Jupiter");
        return w;
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

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIR);
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        StringBuilder report = new StringBuilder();
        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "The Venus table, as this desktop draws the proposal",
                "Sprint 47, issue #500.");
        List<VenusRow> named = new ArrayList<>(rows("named-oslo", null));
        named.addAll(rows("appearances-oslo", null));
        named.sort(java.util.Comparator.comparing(VenusRow::when));
        List<VenusRow> range = rows(RANGE_FILE, null);
        List<VenusRow> conjunction = rows(CONJUNCTION_FILE, CONJUNCTION_HOUR);
        List<VenusRow> weekly = rows("matrix-7d-oslo", null);
        VenusRow card = named.stream()
                .filter(r -> r.when().equals(parse(CARD))).findFirst().orElseThrow();
        preface(report, named);
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
                    names.add(write(card(w, language, card), language, "1-venus-card", platform));
                    names.add(write(rangeTable(w, language, range), language, "2-range", platform));
                    names.add(write(refused(w), language, "3-refused", platform));
                    names.add(write(group(w, language, card), language, "4-controller-group", platform));
                    return names;
                }));
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(true);
                    return List.of(write(rangeTable(w, language, range), language, "2-range-dark", platform));
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
        images.add(diagramGeometry());
        images.add(diagramAppearances(named));
        images.add(diagramApparition(weekly));
        phases(report, named, range, conjunction);
        cartography(report, weekly, named);
        vocabulary(report);
        report.append("## The images\n\n");
        for (String image : images) {
            report.append("- `").append(image).append("`\n");
        }
        System.out.print(PlatformEvidence.portable(report.toString()));
        PlatformEvidence.write(platform, DIR.resolve("platform.md").toString());
    }

    // ---- the fixtures ------------------------------------------------

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss", Locale.ROOT);

    private static LocalDateTime parse(String stamp) {
        String s = stamp.strip();
        int dot = s.indexOf('.');
        return LocalDateTime.parse(dot < 0 ? s : s.substring(0, dot), STAMP);
    }

    private static List<List<String>> table(String file) throws java.io.IOException {
        Path path = HORIZONS.resolve(file);
        String text = Files.readString(path, StandardCharsets.UTF_8);
        int from = text.indexOf("$$SOE");
        int to = text.indexOf("$$EOE");
        if (from < 0 || to < 0) {
            throw new IllegalStateException(path + " is not a complete Horizons response");
        }
        List<List<String>> out = new ArrayList<>();
        for (String line : text.substring(from + 5, to).strip().split("\n")) {
            List<String> f = new ArrayList<>();
            for (String cell : line.split(",")) {
                f.add(cell.strip());
            }
            out.add(f);
        }
        return out;
    }

    /**
     * The Venus rows of a stem, with the Sun's where its file exists;
     * {@code hour} keeps only the rows at that time of day.
     */
    private static List<VenusRow> rows(String stem, String hour) throws java.io.IOException {
        String venusFile = Files.exists(HORIZONS.resolve(stem + "-299.txt")) ? stem + "-299.txt" : stem + ".txt";
        Map<LocalDateTime, List<String>> sun = new LinkedHashMap<>();
        if (Files.exists(HORIZONS.resolve(stem + "-10.txt"))) {
            for (List<String> f : table(stem + "-10.txt")) {
                sun.put(parse(f.get(0)), f);
            }
        }
        List<VenusRow> out = new ArrayList<>();
        for (List<String> f : table(venusFile)) {
            if (hour != null && !f.get(0).contains(hour)) {
                continue;
            }
            LocalDateTime when = parse(f.get(0));
            List<String> s = sun.get(when);
            Double chiJ2000 = null;
            Double chiOfDate = null;
            Double sunRadius = null;
            if (s != null) {
                chiJ2000 = brightLimb(d(s.get(3)), d(s.get(4)), d(f.get(3)), d(f.get(4)));
                chiOfDate = brightLimb(d(s.get(5)), d(s.get(6)), d(f.get(5)), d(f.get(6)));
                sunRadius = Math.toDegrees(Math.asin(SUN_RADIUS_KM / (d(s.get(9)) * AU_KM))) * 3600.0;
            }
            out.add(new VenusRow(when, d(f.get(3)), d(f.get(4)), d(f.get(5)), d(f.get(6)),
                    d(f.get(7)), d(f.get(8)), d(f.get(9)), d(f.get(11)), d(f.get(12)), d(f.get(15)),
                    d(f.get(17)), f.get(18).contains("T"), d(f.get(20)), chiJ2000, chiOfDate, sunRadius));
        }
        return out;
    }

    /**
     * Meeus 48.5: the position angle of the bright limb's midpoint, from
     * north through east, from the Sun (a0, d0) and the body (a, d) in one
     * frame, degrees.
     */
    static double brightLimb(double a0deg, double d0deg, double adeg, double ddeg) {
        double a0 = Math.toRadians(a0deg);
        double d0 = Math.toRadians(d0deg);
        double a = Math.toRadians(adeg);
        double d = Math.toRadians(ddeg);
        double y = Math.cos(d0) * Math.sin(a0 - a);
        double x = Math.sin(d0) * Math.cos(d) - Math.cos(d0) * Math.sin(d) * Math.cos(a0 - a);
        double chi = Math.toDegrees(Math.atan2(y, x));
        return chi < 0 ? chi + 360.0 : chi;
    }

    /** A Horizons number; "n.a." (its magnitude beside the Sun) is not a number. */
    private static double d(String s) {
        return s.equals("n.a.") ? Double.NaN : Double.parseDouble(s);
    }

    // ---- the words a value is shown with -----------------------------

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

    private static String day(LocalDateTime when) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d %02d:%02d", when.getYear(), when.getMonthValue(),
                when.getDayOfMonth(), when.getHour(), when.getMinute());
    }

    private static String elongation(Map<String, String> w, String language, VenusRow r) {
        return num(language, r.elongation(), 1) + "° " + (r.trailing() ? w.get("east") : w.get("west"));
    }

    // ---- the arrangements ---------------------------------------------

    private static JComponent card(Map<String, String> w, String language, VenusRow r) {
        JPanel card = column();
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        card.add(heading(w.get("venus") + " · " + day(r.when()) + " UTC"));
        String[][] rows = {
                {w.get("ra"), hms(r.ra())},
                {w.get("dec"), dms(r.dec())},
                {w.get("altitude"), num(language, r.elevation(), 1) + "°"},
                {w.get("azimuth"), num(language, r.azimuth(), 1) + "°"},
                {w.get("distance"), num(language, r.deltaAu(), 3) + " AU · "
                        + num(language, r.deltaAu() * AU_KM / 1e6, 1) + (language.equals("en") ? " million km" : " mill. km")},
                {w.get("diameter"), num(language, r.diameter(), 1) + "″"},
                {w.get("elongation"), elongation(w, language, r) + " (" + r.sky(w) + ")"},
                {w.get("phaseAngle"), num(language, r.phaseAngle(), 1) + "°"},
                {w.get("illuminated"), num(language, r.illuminated(), 1) + " %"},
                {w.get("phase"), r.phase(w)},
                {w.get("litSide"), r.litSide(w, language)},
        };
        for (String[] row : rows) {
            card.add(line(row[0], row[1]));
        }
        return card;
    }

    private static JComponent rangeTable(Map<String, String> w, String language, List<VenusRow> rows) {
        String[] columns = {w.get("instant"), w.get("elongation"), w.get("illuminated"), w.get("phase"),
                w.get("diameter"), w.get("distance"), w.get("altitude"), w.get("litSide")};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (VenusRow r : rows) {
            model.addRow(new Object[] {day(r.when()), elongation(w, language, r),
                    num(language, r.illuminated(), 2) + " %", r.phase(w),
                    num(language, r.diameter(), 1) + "″", num(language, r.deltaAu(), 4) + " AU",
                    num(language, r.elevation(), 1) + "°", r.litSide(w, language)});
        }
        JTable table = new JTable(model);
        table.setName("venusRange");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {136, 160, 90, 180, 150, 96, 70, 250};
        for (int c = 0; c < widths.length; c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }
        JPanel tableBox = new JPanel(new BorderLayout());
        tableBox.add(table.getTableHeader(), BorderLayout.NORTH);
        tableBox.add(table, BorderLayout.CENTER);
        tableBox.setAlignmentX(0f);
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

    /** The Controller's Solar System sections, ordered outward from the Sun, Venus open. */
    private static JComponent group(Map<String, String> w, String language, VenusRow r) {
        JPanel holder = column();
        holder.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        holder.add(section("▸ " + w.get("sun"), false));
        holder.add(section("▾ " + w.get("venus"), true));
        holder.add(card(w, language, r));
        javax.swing.JButton update = new javax.swing.JButton(w.get("update"));
        update.setAlignmentX(0f);
        holder.add(Box.createVerticalStrut(6));
        holder.add(update);
        holder.add(Box.createVerticalStrut(6));
        holder.add(section("▸ " + w.get("moon"), false));
        holder.add(section("▸ " + w.get("jupiter"), false));
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
        String name = "controls-venus-" + language + "-" + stem + ".png";
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
     * The lit part of a disc of radius r centred at the origin, the
     * bright limb's midpoint at position angle chi (north through east),
     * drawn east left and north up: the bright half-disc, with the
     * terminator's half-ellipse (semi-axis |1 − 2k|·r) added on the dark
     * side when more than half is lit and taken from the bright half
     * when less is. A flat shape: no surface, cloud or texture.
     */
    static Area lit(double r, double k, double chiDeg) {
        Area half = new Area(new Rectangle2D.Double(0, -r, r, 2 * r));
        half.intersect(new Area(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r)));
        double a = Math.abs(1.0 - 2.0 * k) * r;
        Area terminator = new Area(new Ellipse2D.Double(-a, -r, 2 * a, 2 * r));
        if (k >= 0.5) {
            half.add(terminator);
        } else {
            half.subtract(terminator);
        }
        // +x is the bright side; turn it to the bright limb on a north-up, east-left page,
        // where position angle chi points along (−sin chi, −cos chi) in page coordinates.
        double chi = Math.toRadians(chiDeg);
        return half.createTransformedArea(AffineTransform.getRotateInstance(
                Math.atan2(-Math.cos(chi), -Math.sin(chi))));
    }

    private static void disc(Graphics2D g, double x, double y, double r, double k, double chi) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        g.setColor(new Color(60, 60, 70));
        g.fill(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));
        g.setColor(new Color(250, 236, 190));
        g.fill(lit(r, k, chi));
        g.setColor(new Color(150, 150, 150));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));
        g.setTransform(saved);
    }

    private static void arrow(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        double a = Math.atan2(y2 - y1, x2 - x1);
        for (int s = -1; s <= 1; s += 2) {
            g.draw(new Line2D.Double(x2, y2, x2 - 9 * Math.cos(a + s * 0.4), y2 - 9 * Math.sin(a + s * 0.4)));
        }
    }

    /** The geometry: elongation at the Earth, phase angle at Venus, the four turning points. */
    private static String diagramGeometry() throws java.io.IOException {
        BufferedImage image = new BufferedImage(760, 470, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(image);
        g.drawString("Why the phase and the size change together: seen from above the orbits (not to scale)", 16, 22);
        double sx = 380;
        double sy = 210;
        double rv = 110;
        double re = 152;
        g.setColor(new Color(200, 200, 200));
        g.draw(new Ellipse2D.Double(sx - rv, sy - rv, 2 * rv, 2 * rv));
        g.draw(new Ellipse2D.Double(sx - re, sy - re, 2 * re, 2 * re));
        g.setColor(new Color(240, 170, 20));
        g.fill(new Ellipse2D.Double(sx - 14, sy - 14, 28, 28));
        g.setColor(Color.BLACK);
        g.drawString("Sun", (int) sx - 10, (int) sy + 30);
        double ex = sx;
        double ey = sy + re;
        g.setColor(new Color(60, 100, 200));
        g.fill(new Ellipse2D.Double(ex - 7, ey - 7, 14, 14));
        g.setColor(Color.BLACK);
        g.drawString("Earth (the observer)", (int) ex + 12, (int) ey + 4);
        // inferior conjunction, the two greatest elongations (where the line of sight
        // touches Venus's orbit), superior conjunction
        double tangent = Math.acos(rv / re);
        double[][] at = {
                {sx, sy + rv, 0},
                {sx + rv * Math.sin(tangent), sy + rv * Math.cos(tangent), 1},
                {sx, sy - rv, 2},
                {sx - rv * Math.sin(tangent), sy + rv * Math.cos(tangent), 3}};
        String[] names = {"inferior conjunction: nearest, largest, almost unlit",
                "greatest elongation: half lit", "superior conjunction: farthest, smallest, full",
                "greatest elongation: half lit"};
        for (double[] p : at) {
            g.setColor(new Color(150, 150, 150));
            g.draw(new Line2D.Double(ex, ey, p[0], p[1]));
            // the lit hemisphere faces the Sun
            double toSun = Math.atan2(sy - p[1], sx - p[0]);
            AffineTransform saved = g.getTransform();
            g.translate(p[0], p[1]);
            g.rotate(toSun);
            g.setColor(new Color(60, 60, 70));
            g.fill(new Ellipse2D.Double(-8, -8, 16, 16));
            g.setColor(new Color(250, 236, 190));
            g.fill(new Area(new Rectangle2D.Double(0, -8, 8, 16)) {
                {
                    intersect(new Area(new Ellipse2D.Double(-8, -8, 16, 16)));
                }
            });
            g.setTransform(saved);
            g.setColor(Color.BLACK);
            int index = (int) p[2];
            int width = g.getFontMetrics().stringWidth(names[index]);
            int lx = index == 3 ? (int) p[0] - 14 - width : (int) p[0] + 14;
            int ly = index == 0 ? (int) p[1] + 22 : index == 2 ? (int) p[1] - 12 : (int) p[1] - 6;
            g.drawString(names[index], lx, ly);
        }
        g.setColor(Color.BLACK);
        g.drawString("Elongation: the Sun–Earth–Venus angle, seen at the Earth (at most about 47°).", 16, 418);
        g.drawString("Phase angle: the Sun–Venus–Earth angle, at Venus; illuminated fraction k = (1 + cos phase angle) / 2.", 16, 434);
        g.drawString("Near inferior conjunction Venus is nearest and largest but shows the Earth its unlit side.", 16, 450);
        g.dispose();
        String name = "controls-venus-5-phase-geometry.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name;
    }

    /** Every named appearance at Oslo, to one scale, bright limb at its J2000 position angle. */
    private static String diagramAppearances(List<VenusRow> named) throws java.io.IOException {
        List<VenusRow> shown = new ArrayList<>();
        for (VenusRow r : named) {
            if (r.chiJ2000() != null && r.when().getYear() >= 2012 && r.when().getYear() <= 2027) {
                shown.add(r);
            }
        }
        int rowsOfDiscs = (shown.size() + 3) / 4;
        BufferedImage image = new BufferedImage(780, 110 + rowsOfDiscs * 160, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(image);
        g.drawString("The named appearances at Oslo, to one scale (1.6 px per arcsecond), east left, north up;"
                + " the bright limb at its J2000 position angle", 16, 22);
        double scale = 1.6;
        int columns = 4;
        int index = 0;
        for (VenusRow r : shown) {
            int cx = 95 + (index % columns) * 190;
            int cy = 100 + (index / columns) * 160;
            disc(g, cx, cy, r.diameter() / 2.0 * scale, r.illuminated() / 100.0, r.chiJ2000());
            g.setColor(Color.BLACK);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.drawString(day(r.when()).substring(0, 10), cx - 80, cy + 58);
            g.drawString(r.phase(words("en")), cx - 80, cy + 71);
            g.drawString(String.format(Locale.ROOT, "%.1f″, k %.1f %%, χ %.0f°", r.diameter(), r.illuminated(),
                    r.chiJ2000()), cx - 80, cy + 84);
            index++;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString("A flat lit shape on a dark disc: the illuminated fraction and the bright limb's direction,"
                + " nothing of a surface.", 16, image.getHeight() - 12);
        g.dispose();
        String name = "controls-venus-6-appearances.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name;
    }

    /** One apparition from the weekly Oslo matrix: diameter, illuminated fraction and elongation together. */
    private static String diagramApparition(List<VenusRow> weekly) throws java.io.IOException {
        List<VenusRow> span = new ArrayList<>();
        for (VenusRow r : weekly) {
            LocalDateTime w = r.when();
            if (!w.isBefore(LocalDateTime.of(2026, 1, 1, 0, 0)) && w.isBefore(LocalDateTime.of(2027, 7, 1, 0, 0))) {
                span.add(r);
            }
        }
        BufferedImage image = new BufferedImage(760, 380, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(image);
        g.drawString("One apparition, weekly at Oslo, 2026-01 to 2027-06: as the disc grows the lit part shrinks", 16, 22);
        int left = 60;
        int right = 700;
        int top = 50;
        int bottom = 290;
        g.setColor(new Color(220, 220, 220));
        g.drawRect(left, top, right - left, bottom - top);
        double n = span.size() - 1;
        Path2D diam = new Path2D.Double();
        Path2D lit = new Path2D.Double();
        Path2D elong = new Path2D.Double();
        for (int i = 0; i < span.size(); i++) {
            VenusRow r = span.get(i);
            double x = left + (right - left) * i / n;
            double yd = bottom - (bottom - top) * r.diameter() / 70.0;
            double yk = bottom - (bottom - top) * r.illuminated() / 100.0;
            double ye = bottom - (bottom - top) * r.elongation() / 50.0;
            if (i == 0) {
                diam.moveTo(x, yd);
                lit.moveTo(x, yk);
                elong.moveTo(x, ye);
            } else {
                diam.lineTo(x, yd);
                lit.lineTo(x, yk);
                elong.lineTo(x, ye);
            }
        }
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(60, 100, 200));
        g.draw(diam);
        g.setColor(new Color(220, 150, 20));
        g.draw(lit);
        g.setColor(new Color(120, 120, 120));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {5f, 4f}, 0f));
        g.draw(elong);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.BLACK);
        g.drawString("apparent diameter (blue, 0–70″)", left, 330);
        g.drawString("illuminated (orange, 0–100 %)", left + 230, 330);
        g.drawString("elongation from the Sun (grey, 0–50°)", left + 450, 330);
        for (int i = 0; i < span.size(); i += 13) {
            double x = left + (right - left) * i / n;
            g.drawString(day(span.get(i).when()).substring(0, 7), (int) x - 20, bottom + 14);
        }
        g.drawString("Read from Horizons' weekly Oslo rows: superior conjunction near the start, inferior conjunction"
                + " where the lit fraction is least.", 16, 362);
        g.dispose();
        String name = "controls-venus-7-apparition.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        return name;
    }

    // ---- the report -------------------------------------------------------

    private static void preface(StringBuilder out, List<VenusRow> named) {
        out.append("# The Venus table and the cartography handoff, proposed before either is built\n\n")
                .append("Sprint 47, issue #500. Generated by `juranometria.tool.VenusContractStudyMain`"
                        + " from the kept JPL Horizons\nresponses under `horizons/` at Oslo (59.91° N,"
                        + " 10.75° E, sea level): every number below and in the\nimages is read from"
                        + " those files, or computed from them as stated, never typed here. The words"
                        + " are\nthe study's proposal in both languages, marked as such until the reader's"
                        + " table carries them.\nHow tall and wide the arrangements are drawn is one"
                        + " machine's answer, in `platform.md` beside this.\n\n");
        out.append("## The named instants, as Horizons states them, with the bright limb computed\n\n");
        out.append("| instant (UTC) | RA (ICRF) | Dec | altitude | distance (AU) | diameter | elongation |"
                + " phase angle (phi) | illuminated | χ J2000 | χ of date | J2000 − of date |\n"
                + "|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (VenusRow r : named) {
            out.append(String.format(Locale.ROOT,
                    "| %s | %.6f° | %.6f° | %.3f° | %.6f | %.3f″ | %.4f° %s | %.4f° | %.4f %% | %.2f° | %.2f° | %+.3f° |%n",
                    day(r.when()), r.ra(), r.dec(), r.elevation(), r.deltaAu(), r.diameter(), r.elongation(),
                    r.trailing() ? "E" : "W", r.phaseAngle(), r.illuminated(), r.chiJ2000(), r.chiOfDate(),
                    wrap(r.chiJ2000() - r.chiOfDate())));
        }
        out.append("\nχ is Meeus 48.5 from Horizons' own Sun and Venus: in J2000 from the astrometric ICRF"
                + " places (the chart's frame),\nof date from the apparent places. E/W is Horizons' /T or /L"
                + " flag: Venus trailing the Sun (east of it, the evening sky)\nor leading it (west, the morning"
                + " sky).\n\n");
    }

    private static double wrap(double deg) {
        double x = (deg + 540.0) % 360.0 - 180.0;
        return x;
    }

    private static void phases(StringBuilder out, List<VenusRow> named, List<VenusRow> range,
                               List<VenusRow> conjunction) {
        out.append("## The phase words, proposed\n\n");
        out.append(String.format(Locale.ROOT,
                "Decided by the illuminated fraction k on the Moon's ruled half-open bands, with two geometric"
                        + " states first:%n%n| word | when |%n|---|---|%n"
                        + "| in front of the Sun | Venus's centre within the Sun's apparent radius plus its own,"
                        + " phase angle above 90° (a transit) |%n"
                        + "| behind the Sun | the same, phase angle below 90° (Venus hidden) |%n"
                        + "| near inferior conjunction | k < %.0f %% |%n| crescent | %.0f %% ≤ k < %.0f %% |%n"
                        + "| near half lit | %.0f %% ≤ k < %.0f %% |%n| gibbous | %.0f %% ≤ k < %.0f %% |%n"
                        + "| near full | k ≥ %.0f %% |%n%n"
                        + "The lit side is χ of date, as the Moon's, with the nearest of sixteen points, and"
                        + " *not well-defined* by the same two\nthresholds that name the phase near inferior"
                        + " conjunction and near full.%n%n",
                NEAR_NEW, NEAR_NEW, HALF_LOW, HALF_LOW, HALF_HIGH, HALF_HIGH, NEAR_FULL, NEAR_FULL));
        out.append("Beside it, as the Moon's table has it: the elongation with E or W, east or west of the Sun in"
                + " apparent right\nascension of date (the evening or the morning sky).\n\n");
        Map<String, String> en = words("en");
        Map<String, String> nb = words("nb-NO");
        out.append("| instant (UTC) | k | elongation | English | Norsk |\n|---|---|---|---|---|\n");
        for (VenusRow r : named) {
            out.append(String.format(Locale.ROOT, "| %s | %.4f %% | %.2f° %s | %s, %s | %s, %s |%n",
                    day(r.when()), r.illuminated(), r.elongation(), r.trailing() ? "E" : "W",
                    r.phase(en), r.sky(en), r.phase(nb), r.sky(nb)));
        }
        out.append("\nThe range mock-up's rows, every 14 days through the 2026-27 apparition at Oslo:\n\n");
        out.append("| instant (UTC) | k | diameter | elongation | phase angle | χ of date | χ J2000 | phase |\n"
                + "|---|---|---|---|---|---|---|---|\n");
        Map<String, String> words = words("en");
        for (VenusRow r : range) {
            out.append(String.format(Locale.ROOT, "| %s | %.4f %% | %.3f″ | %.3f° %s | %.3f° | %.2f° | %.2f° | %s |%n",
                    day(r.when()), r.illuminated(), r.diameter(), r.elongation(), r.trailing() ? "E" : "W",
                    r.phaseAngle(), r.chiOfDate(), r.chiJ2000(), r.phase(words)));
        }
        String flip = "";
        for (int i = 1; i < conjunction.size(); i++) {
            if (conjunction.get(i).trailing() != conjunction.get(i - 1).trailing()) {
                flip = day(conjunction.get(i - 1).when()) + " and " + day(conjunction.get(i).when());
            }
        }
        out.append("\nAnd one row a day at 03:00 through the 2026 inferior conjunction. Horizons' side of the Sun"
                + " (right ascension)\nchanges between " + flip + " UTC, before the conjunction in ecliptic"
                + " longitude that Espenak names\n(`venus-events.txt`); `authority-comparison.md` gives both"
                + " instants:\n\n");
        out.append("| instant (UTC) | k | diameter | elongation | phase angle | χ J2000 |\n|---|---|---|---|---|---|\n");
        for (VenusRow r : conjunction) {
            out.append(String.format(Locale.ROOT, "| %s | %.4f %% | %.3f″ | %.3f° %s | %.3f° | %.2f° |%n",
                    day(r.when()), r.illuminated(), r.diameter(), r.elongation(), r.trailing() ? "E" : "W",
                    r.phaseAngle(), r.chiJ2000()));
        }
        out.append('\n');
    }

    private static void cartography(StringBuilder out, List<VenusRow> weekly, List<VenusRow> named) {
        double dMin = Double.MAX_VALUE;
        double dMax = 0.0;
        VenusRow near = null;
        VenusRow far = null;
        for (VenusRow r : weekly) {
            if (r.diameter() < dMin) {
                dMin = r.diameter();
                far = r;
            }
            if (r.diameter() > dMax) {
                dMax = r.diameter();
                near = r;
            }
        }
        out.append("## The cartography handoff\n\n### A. The diameter on every page\n\n");
        out.append(String.format(Locale.ROOT,
                "Over 1900-2100 (%d weekly rows at Oslo, topocentric) Venus's apparent diameter is %.2f″"
                        + " (%s, farthest) to %.2f″ (%s, nearest),\nfrom Horizons' `Ang-diam` on the %.1f km"
                        + " solid-body radius. The cloud tops, %.0f km higher, would draw it %.2f %% wider:"
                        + " %.3f″ at its largest.%n%n",
                weekly.size(), dMin, day(far.when()).substring(0, 10), dMax, day(near.when()).substring(0, 10),
                VENUS_RADIUS_KM, CLOUD_TOP_KM, CLOUD_TOP_KM / VENUS_RADIUS_KM * 100.0,
                dMax * CLOUD_TOP_KM / VENUS_RADIUS_KM));
        double a4Mm = PaperSize.A4.chartWideMm();
        double letterMm = PaperSize.LETTER.chartWideMm();
        int a4W = (int) Math.round(PaperSize.A4.chartWidePoints() * PngSheetWriter.DEFAULT_RESOLUTION / 72.0);
        out.append(String.format(Locale.ROOT,
                "Pixels on the %d × %d page at its centre (each field's own projection, as the Jovian"
                        + " cartography study measures it);%nmillimetres on the A4 sheet's chart area (%.0f mm"
                        + " wide, %d px at %d dpi) and the Letter sheet's (%.0f mm).%n%n",
                PAGE_W, PAGE_H, a4Mm, a4W, PngSheetWriter.DEFAULT_RESOLUTION, letterMm));
        out.append("| field | diameter px (min-max) | cloud tops add, px (max) | A4 mm (min-max) | Letter mm (min-max) |"
                + " A4 at 300 dpi, px (max) | weeks the phase is readable on the page | on A4 at 300 dpi |\n"
                + "|---:|---|---:|---|---|---:|---:|---:|\n");
        for (double field : ChartViewState.fieldWidthSteps()) {
            double perArcsec = JovianCartographyStudyMain.pxPerArcsec(field, PAGE_W, PAGE_H);
            double a4PerArcsec = a4Mm / (field * 3600.0);
            double letterPerArcsec = letterMm / (field * 3600.0);
            double a4PxPerArcsec = a4W / (field * 3600.0);
            out.append(String.format(Locale.ROOT, "| %.0f° | %.2f-%.2f | %.3f | %.3f-%.3f | %.3f-%.3f | %.1f | %s | %s |%n",
                    field, dMin * perArcsec, dMax * perArcsec, dMax * CLOUD_TOP_KM / VENUS_RADIUS_KM * perArcsec,
                    dMin * a4PerArcsec, dMax * a4PerArcsec, dMin * letterPerArcsec, dMax * letterPerArcsec,
                    dMax * a4PxPerArcsec, readable(weekly, perArcsec), readable(weekly, a4PxPerArcsec)));
        }
        out.append(String.format(Locale.ROOT,
                "%nA week counts as readable when the disc is at least %.0f px across and its lit and dark"
                        + " parts, along the bright limb's axis,%nare each at least %.0f px (k·D and (1 − k)·D):"
                        + " the study's proposed criterion, for the ruling.%n%n",
                READABLE_DISC_PX, READABLE_PART_PX));
        double at1 = JovianCartographyStudyMain.pxPerArcsec(ChartViewState.normalMinimumFieldDegrees(), PAGE_W, PAGE_H);
        out.append(String.format(Locale.ROOT, "### B. At the atlas's minimum field (%.0f°), the named appearances%n%n",
                ChartViewState.normalMinimumFieldDegrees()));
        out.append("| instant (UTC) | diameter px | lit px (k·D) | dark px | readable | χ J2000 |\n|---|---:|---:|---:|---|---:|\n");
        for (VenusRow r : named) {
            double dpx = r.diameter() * at1;
            double k = r.illuminated() / 100.0;
            out.append(String.format(Locale.ROOT, "| %s | %.2f | %.2f | %.2f | %s | %.1f° |%n",
                    day(r.when()), dpx, k * dpx, (1 - k) * dpx, readable(r, at1) ? "yes" : "no", r.chiJ2000()));
        }
        // The field below which the largest and the half-lit disc would read.
        double kHalfDiameter = 0.0;
        for (VenusRow r : weekly) {
            if (Math.abs(r.illuminated() - 50.0) < 5.0) {
                kHalfDiameter = Math.max(kHalfDiameter, r.diameter());
            }
        }
        out.append(String.format(Locale.ROOT,
                "%nThe largest half-lit disc in the weekly rows is %.1f″; at %.0f° it is %.1f px across, each half"
                        + " %.1f px.%n%n",
                kHalfDiameter, ChartViewState.normalMinimumFieldDegrees(), kHalfDiameter * at1,
                kHalfDiameter * at1 / 2.0));
        out.append("### C. The bright limb on the J2000 chart\n\n"
                + "χ is a position angle from north through east, in the frame its two directions are"
                + " given in. The table states\nχ of date, as the Moon's does; the named table above shows"
                + " the J2000 χ, from the astrometric ICRF Sun and Venus,\ndiffering from it by up to the"
                + " precession of the equator since 2000. A J2000 page must not draw χ of date as if it\n"
                + "were J2000: the Moon's ruling has the later drawing rotate χ into the chart's north basis,"
                + " not recompute it.\nOn a page, north is moreover the projection's local north at"
                + " Venus's place, not the page's top: the renderer\nturns the J2000 direction into the page"
                + " by the projected direction of increasing declination there, as Jupiter's\npole was"
                + " derived in the chart's frame (#484).\n\n");
    }

    private static boolean readable(VenusRow r, double perArcsec) {
        double dpx = r.diameter() * perArcsec;
        double k = r.illuminated() / 100.0;
        return dpx >= READABLE_DISC_PX && k * dpx >= READABLE_PART_PX && (1 - k) * dpx >= READABLE_PART_PX;
    }

    private static String readable(List<VenusRow> weekly, double perArcsec) {
        int n = 0;
        for (VenusRow r : weekly) {
            if (readable(r, perArcsec)) {
                n++;
            }
        }
        return String.format(Locale.ROOT, "%.1f %%", 100.0 * n / weekly.size());
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
