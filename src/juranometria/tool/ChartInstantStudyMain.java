package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * The frozen chart instant, studied before any furniture is built
 * (Sprint 51, issue #556, ahead of #259).
 *
 * <p>Every page is the production {@code ChartComponent} over the bundled
 * catalogue with the Place and Time module attached as the application
 * attaches it. The proposed <em>Place and Time box</em> is drawn by this
 * study over the finished page, in the title block's own geometry and
 * type (its margin, padding and face read from the renderer), and is
 * marked STUDY: no production furniture exists. The report records what
 * is the same on every desktop - the cases, the rule that decides when a
 * page states its instant, and the proposals' wording; the box's size and
 * what each candidate corner would displace are this desktop's fonts'
 * answer and stand in {@code platform.md}. The panel and two-screen
 * proposals are schematic drawings, not photographs of Swing.
 *
 * <p>The sample instant is the cited 2026 greatest eastern elongation of
 * Venus, {@code docs/studies/venus/venus-events.txt}; the observer is
 * Oslo at sea level.
 */
public final class ChartInstantStudyMain {

    private static final File DIR = new File("docs/studies/chart-instant");
    private static final Path VENUS_EVENTS = Path.of("docs/studies/venus/venus-events.txt");
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    private static final PageText ENGLISH = PageText.in(EN);
    private static final PageText NORSK = PageText.in(NB);

    /** The letterbox band's colour, stated (the look and feel's light panel grey). */
    private static final Color LETTERBOX_BAND = new Color(0xEE, 0xEE, 0xEE);

    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();

    /** The three corners the title block (lower left) leaves. */
    enum Corner { LOWER_RIGHT, UPPER_RIGHT, UPPER_LEFT }

    private record Case(String name, String caption, ChartViewState state, int width, int height,
                        ChartPalette palette, boolean norwegian, boolean lines, boolean venus) {
    }

    private ChartInstantStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    private static void q(String line) {
        PLATFORM.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        PLATFORM.setLength(0);
        DIR.mkdirs();
        Instant instant = elongation();
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, instant);
        SolarSystemService service = SolarSystemService.load();
        PlatformEvidence.preface(PLATFORM,
                "The proposed Place and Time box on production pages, as this desktop's fonts size it",
                "Sprint 51, issue #556.");

        p("# The frozen chart instant: measurements and mock-ups");
        p("");
        p("Sprint 51, issue #556, a study ahead of #259. Every page below is the production"
                + " `ChartComponent` over the bundled catalogue with the Place and Time module attached"
                + " as the application attaches it; the proposed **Place and Time box** is drawn by"
                + " this study over the finished page in the title block's own margin, padding and"
                + " face, and marked STUDY - no production furniture exists. The sample instant is"
                + " Venus's cited 2026 greatest eastern elongation (" + utc(instant) + ", from"
                + " `docs/studies/venus/venus-events.txt`), the observer Oslo at sea level. The box's"
                + " size and what each corner would displace are in `platform.md`. Regenerate with"
                + " `make chart-instant-study`.");
        p("");

        rule();
        List<Case> cases = cases();
        pages(cases, oslo, service);
        sheet(oslo);
        panels(instant);
        checkpoint(instant);

        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    // ---- the rule --------------------------------------------------------------------

    private static void rule() {
        p("## 1. When a page states its instant");
        p("");
        p("The catalogue's stars, the grid, the figures and the boundaries are J2000 and do not"
                + " depend on the instant. A page depends on it only through what the Place and Time"
                + " module or a Solar System body contributes: the meridian, the mathematical"
                + " horizon, the zenith, their cardinal letters and altitude ticks, or a body. The"
                + " proposal tested here: **the box is drawn whenever a time-dependent layer is"
                + " switched on** - an observer line or a body on the chart - whether or not its ink"
                + " falls on the page in view, so that panning never makes the box come and go; and"
                + " never otherwise, so every page with no such layer on stays byte for byte as it"
                + " is. (The alternative - only while such ink is on the page - is measured by the"
                + " same cases: the dense page below has the lines on but off the page.)");
        p("");
    }

    // ---- the cases -------------------------------------------------------------------

    private static List<Case> cases() {
        List<Case> out = new ArrayList<>();
        SkyPosition m31 = ChartViewState.DEFAULT.centre();
        out.add(new Case("default-8", "The released opening page, nothing time-dependent drawn:"
                + " the rule draws no box.", ChartViewState.DEFAULT, 900, 700,
                ChartPalette.WHITE_PAPER, false, false, false));
        SkyPosition southMeridian = new SkyPosition(64.0, -28.0);
        out.add(new Case("sparse-lines-8", "Sparse: Eridanus on Oslo's meridian at the sample"
                + " instant, the meridian and the mathematical horizon crossing the page.",
                new ChartViewState(southMeridian, 8.0, 8.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, true, false));
        out.add(new Case("sparse-lines-8-dark", "The same on the black sky.",
                new ChartViewState(southMeridian, 8.0, 8.0), 900, 700, ChartPalette.BLACK_SKY,
                false, true, false));
        out.add(new Case("sparse-lines-8-nb", "The same in Norwegian.",
                new ChartViewState(southMeridian, 8.0, 8.0), 900, 700, ChartPalette.WHITE_PAPER,
                true, true, false));
        out.add(new Case("dense-lines-8", "Dense: the Milky Way in Sagittarius with the lines"
                + " switched on - off this page at the sample instant, so the rule's choice shows.",
                new ChartViewState(new SkyPosition(270.0, -25.0), 8.0, 8.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, true, false));
        out.add(new Case("venus-3", "A Solar System body: Venus at the sample instant, 3°, the"
                + " lines off - the body alone makes the page time-dependent.", null, 900, 700,
                ChartPalette.WHITE_PAPER, false, false, true));
        out.add(new Case("wide-lines-60", "Wide: a 60° overview page with the lines on.",
                new ChartViewState(new SkyPosition(30.0, 40.0), 60.0, 5.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, true, false));
        out.add(new Case("narrow-lines-8", "Narrow: a 520 px wide window.",
                new ChartViewState(m31, 8.0, 8.0), 520, 700, ChartPalette.WHITE_PAPER, false, true,
                false));
        out.add(new Case("polar-lines-8", "Polar: centred two degrees from the celestial pole.",
                new ChartViewState(new SkyPosition(0.0, 88.0), 8.0, 8.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, true, false));
        out.add(new Case("globe-lines-180", "The celestial globe with the lines on.",
                new ChartViewState(new SkyPosition(30.0, 40.0), 180.0, 5.0), 900, 700,
                ChartPalette.WHITE_PAPER, false, true, false));
        out.add(new Case("letterbox-lines-120", "Letterboxed: a 120° overview page in a tall"
                + " 600 × 1900 window, the page shorter than the component.", new ChartViewState(m31, 120.0, 4.0), 600,
                1900, ChartPalette.WHITE_PAPER, false, true, false));
        return out;
    }

    // ---- the pages -------------------------------------------------------------------

    private static void pages(List<Case> cases, Observer oslo, SolarSystemService service)
            throws Exception {
        p("## 2. The box on production pages");
        p("");
        p("Proposed content (English / Norwegian): a heading, the observer, the frozen UTC"
                + " instant and the local sidereal time, then one on-screen action. No civil time,"
                + " no zone, nothing that ticks.");
        p("");
        p("| line | English | Norwegian |");
        p("|---|---|---|");
        List<String> en = boxLines(oslo, false, true);
        List<String> nb = boxLines(oslo, true, true);
        for (int i = 0; i < en.size(); i++) {
            p("| " + (i + 1) + " | " + en.get(i) + " | " + nb.get(i) + " |");
        }
        p("");
        p("Each page below draws the box in the corner the proposed allocation gives it - the"
                + " lower right first, the upper right while the magnitude key is off, then the"
                + " upper left. The box is drawn over the finished page, so a grid figure or a name"
                + " under it is covered here; in production it would yield to the box exactly as it"
                + " yields to the title block today. `platform.md` gives, for every page, whether"
                + " the box fits each of the three corners the title block leaves and how many grid"
                + " figures and stars it would displace there.");
        p("");
        q("## What each corner would displace");
        q("");
        q("| page | box (px) | corner | fits | grid figures under it | stars under it |");
        q("|---|---|---|---|---:|---:|");
        for (Case c : cases) {
            Case actual = c;
            if (c.venus()) {
                SkyPosition venus = ((juranometria.solar.VenusService.VenusObservation)
                        service.observe(SolarSystemService.Body.VENUS, oslo)).astrometricJ2000();
                actual = new Case(c.name(), c.caption(), new ChartViewState(venus, 3.0, 8.0),
                        c.width(), c.height(), c.palette(), c.norwegian(), c.lines(), c.venus());
            }
            page(actual, oslo, service);
        }
        q("");
    }

    private static void page(Case c, Observer oslo, SolarSystemService service) throws Exception {
        PageText words = c.norwegian() ? NORSK : ENGLISH;
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            // The letterbox band is the component's background, which the
            // look and feel supplies; stated here so the page does not depend
            // on whichever generator ran before it in the same JVM.
            holder[0].setBackground(LETTERBOX_BAND);
            holder[0].setSize(c.width(), c.height());
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(c.palette()));
            holder[0].setViewState(c.state());
            MeridianModule meridian = new MeridianModule(oslo);
            meridian.showing(c.lines(), c.lines(), c.lines());
            holder[0].overlays().offer(MeridianModule.ID, meridian::contributedGeometry);
            if (c.venus()) {
                SolarSystemModule solar = new SolarSystemModule(() -> oslo, () -> service,
                        () -> false);
                solar.venusShowing(true);
                holder[0].overlays().offer(SolarSystemModule.ID, solar::contributedGeometry);
            }
        });
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        BufferedImage image = new BufferedImage(c.width(), c.height(), BufferedImage.TYPE_INT_RGB);
        boolean timeDependent = c.lines() || c.venus();
        Rectangle[] drawnBox = new Rectangle[1];
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
                if (timeDependent) {
                    Graphics2D onPage = (Graphics2D) g.create();
                    onPage.translate(chart.pageOffsetX(), chart.pageOffsetY());
                    drawnBox[0] = drawBox(onPage, chart.currentScene(), c.palette(),
                            boxLines(oslo, c.norwegian(), true),
                            allocate(chart, c.norwegian(), oslo), true);
                    onPage.dispose();
                }
            } finally {
                g.dispose();
            }
        });
        ImageIO.write(image, "png", new File(DIR, c.name() + ".png"));
        ChartScene scene = chart.currentScene();
        p("### " + c.name());
        p("");
        Corner placed = timeDependent ? allocate(chart, c.norwegian(), oslo) : null;
        p(c.caption() + (timeDependent ? " The box stands in the "
                + (placed == null ? "(none: refused)" : placed.name().toLowerCase(Locale.ROOT)
                        .replace('_', ' ')) + "." : " (No box: nothing on the page depends on the"
                + " instant.)") + (chart.pageOffsetY() > 0 ? " The page is letterboxed: "
                + scene.viewport().heightPx() + " px tall in a " + c.height() + " px window." : ""));
        p("");
        p("![](" + c.name() + ".png)");
        p("");
        if (!timeDependent) {
            return;
        }
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, words);
        FontMetrics label = metrics(false);
        Rectangle title = chart.chartOptions().titleBlock()
                ? renderer.titleBlockBounds(label, scene) : null;
        List<String> lines = boxLines(oslo, c.norwegian(), true);
        for (Corner corner : Corner.values()) {
            Rectangle box = boxBounds(scene, lines, corner);
            String fits;
            int figures = 0;
            int stars = 0;
            if (box == null) {
                fits = "no - too small";
            } else if (title != null && box.intersects(title)) {
                fits = "no - meets the title block";
            } else {
                fits = corner == Corner.UPPER_RIGHT ? "yes, unless the magnitude key is on" : "yes";
                DrawnPage drawn = DrawnPage.of(scene);
                EquatorialGrid.Grid grid = title == null ? EquatorialGrid.gridFor(drawn)
                        : EquatorialGrid.gridFor(drawn, title);
                FontMetrics gm = EquatorialGrid.labelMetrics();
                for (EquatorialGrid.Label l : grid.labels()) {
                    if (EquatorialGrid.labelBounds(l, gm).intersects(box)) {
                        figures++;
                    }
                }
                ViewportMapping mapping = new ViewportMapping(drawn);
                for (Star s : scene.stars()) {
                    var plane = drawn.projection().project(s.position());
                    if (plane.isPresent()) {
                        PixelPoint px = mapping.toPixel(plane.get());
                        if (box.contains(px.x(), px.y())) {
                            stars++;
                        }
                    }
                }
            }
            q(String.format(Locale.ROOT, "| %s | %s | %s | %s | %d | %d |", c.name(),
                    box == null ? "-" : box.width + "×" + box.height,
                    corner.name().toLowerCase(Locale.ROOT).replace('_', ' '), fits, figures, stars));
        }
    }

    // ---- the exported sheet -------------------------------------------------------------

    private static void sheet(Observer oslo) throws Exception {
        p("## 3. The exported sheet");
        p("");
        p("On paper the action cannot be pressed, so it is not drawn: the box keeps the observer,"
                + " the instant and the sidereal time, and says the page is frozen there. The"
                + " sheet's description would carry the same sentence (section 5 of the decision).");
        p("");
        List<String> en = boxLines(oslo, false, false);
        p("| line | on paper (English) |");
        p("|---|---|");
        for (int i = 0; i < en.size(); i++) {
            p("| " + (i + 1) + " | " + en.get(i) + " |");
        }
        p("");
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
            holder[0].setSize(900, 700);
            holder[0].setViewState(new ChartViewState(new SkyPosition(64.0, -28.0), 8.0, 8.0));
            MeridianModule meridian = new MeridianModule(oslo);
            meridian.showing(true, true, true);
            holder[0].overlays().offer(MeridianModule.ID, meridian::contributedGeometry);
        });
        SwingUtilities.invokeAndWait(() -> { });
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                holder[0].paint(g);
                drawBox(g, holder[0].currentScene(), ChartPalette.WHITE_PAPER, en,
                        Corner.LOWER_RIGHT, false);
            } finally {
                g.dispose();
            }
        });
        ImageIO.write(image, "png", new File(DIR, "sheet-static.png"));
        p("![](sheet-static.png)");
        p("");
    }

    // ---- the panels (schematic) ---------------------------------------------------------

    private static void panels(Instant chartInstant) throws Exception {
        p("## 4. The panels, schematic");
        p("");
        p("Not photographs: schematic drawings of the proposal for a Solar System group in the"
                + " Controller, in the order the rows would stand. Today a group's observer note"
                + " shows the observer read at its last Compute, with *Update from Place and Time*"
                + " last in the group; nothing says when that differs from the chart. Proposed: an"
                + " agreement line at the top of every group, read from Place and Time whenever it"
                + " changes - never recomputing on its own - with the update action beside it.");
        p("");
        Instant stale = chartInstant.minusSeconds(9 * 3600);
        for (boolean norwegian : new boolean[] {false, true}) {
            for (boolean agrees : new boolean[] {true, false}) {
                String name = "panel-" + (agrees ? "agrees" : "differs") + "-"
                        + (norwegian ? "nb" : "en") + ".png";
                drawPanel(name, norwegian, chartInstant, agrees ? chartInstant : stale);
                p("![](" + name + ")");
                p("");
            }
        }
    }

    private static void drawPanel(String name, boolean norwegian, Instant chart, Instant table)
            throws IOException {
        boolean agrees = chart.equals(table);
        int w = 420;
        int h = 210;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = schematic(image);
        heading(g, norwegian ? "Venus" : "Venus", 16, 26);
        int y = 44;
        Color strip = agrees ? new Color(0xEE, 0xF4, 0xEE) : new Color(0xFB, 0xF1, 0xDE);
        g.setColor(strip);
        g.fillRect(12, y, w - 24, agrees ? 30 : 62);
        g.setColor(new Color(0x88, 0x88, 0x88));
        g.drawRect(12, y, w - 25, agrees ? 30 : 62);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        if (agrees) {
            g.drawString(norwegian ? "Samme tidspunkt som kartet: " + utc(chart)
                    : "Same instant as the chart: " + utc(chart), 20, y + 19);
        } else {
            g.drawString((norwegian ? "Kartet: " : "Chart: ") + utc(chart), 20, y + 17);
            g.drawString((norwegian ? "Tabellen: " : "This table: ") + utc(table), 20, y + 33);
            button(g, norwegian ? "Oppdater fra Sted og tid" : "Update from Place and Time",
                    20, y + 40);
        }
        y += agrees ? 46 : 78;
        g.setColor(new Color(0x55, 0x55, 0x55));
        g.drawString(norwegian ? "( ) Dette tidspunktet   ( ) Over et tidsrom"
                : "( ) This instant   ( ) Over a range", 20, y + 4);
        g.drawString(norwegian ? "… resultatkort, Vis på kartet, Sentrer på kartet …"
                : "… result card, Show on chart, Centre on chart …", 20, y + 26);
        stamp(g, w, h);
        g.dispose();
        ImageIO.write(image, "png", new File(DIR, name));
    }

    // ---- the two-screen checkpoint (schematic) -----------------------------------------

    private static void checkpoint(Instant chartInstant) throws Exception {
        p("## 5. The laptop Controller and the external chart, schematic");
        p("");
        p("The arrangement the owner works in: the Controller on the laptop's screen, the chart"
                + " on an external monitor. Proposed: the chart's box carries the instant where the"
                + " page is read, and the Controller's Solar System tab opens with one line naming"
                + " the chart's instant and offering *Update to now*, so a reader at the laptop never"
                + " has to visit the Chart tab to learn what the chart is frozen at.");
        p("");
        int w = 980;
        int h = 420;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = schematic(image);
        // The laptop.
        g.setColor(new Color(0x33, 0x33, 0x33));
        g.drawRect(20, 60, 300, 330);
        heading(g, "Laptop - Controller", 20, 50);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.drawString("[ Chart ]  [ Solar System ]", 32, 84);
        g.setColor(new Color(0xEE, 0xF4, 0xEE));
        g.fillRect(30, 96, 280, 50);
        g.setColor(Color.BLACK);
        g.drawString("Chart: " + utc(chartInstant), 38, 114);
        button(g, "Update to now", 38, 122);
        g.drawString("▸ Sun   ▸ Mercury   ▾ Venus", 32, 170);
        g.setColor(new Color(0xEE, 0xF4, 0xEE));
        g.fillRect(40, 180, 260, 26);
        g.setColor(Color.BLACK);
        g.drawString("Same instant as the chart", 48, 198);
        g.setColor(new Color(0x55, 0x55, 0x55));
        g.drawString("… the Venus table …", 48, 228);
        // The external monitor.
        g.setColor(new Color(0x33, 0x33, 0x33));
        g.drawRect(360, 40, 600, 350);
        heading(g, "External monitor - chart", 360, 30);
        g.setColor(new Color(0xF6, 0xF6, 0xF6));
        g.fillRect(361, 41, 599, 349);
        g.setColor(Color.BLACK);
        g.drawRect(372, 330, 170, 48);
        g.drawString("Title block", 382, 350);
        g.drawRect(760, 296, 188, 88);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString("Place and Time", 768, 312);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString("59.91° N · 10.75° E", 768, 326);
        g.drawString(utc(chartInstant), 768, 340);
        g.drawString("LST 04h 17m", 768, 354);
        button(g, "Update to now", 768, 358);
        stamp(g, w, h);
        g.dispose();
        ImageIO.write(image, "png", new File(DIR, "checkpoint-two-screens.png"));
        p("![](checkpoint-two-screens.png)");
        p("");
    }

    // ---- the box ------------------------------------------------------------------------

    /** The box's lines: heading, observer, instant, sidereal time, and the action on screen. */
    static List<String> boxLines(Observer at, boolean norwegian, boolean screen) {
        List<String> lines = new ArrayList<>();
        lines.add(norwegian ? "Sted og tid" : "Place and Time");
        lines.add(String.format(Locale.ROOT, "%.2f° %s · %.2f° %s", Math.abs(at.latitudeDegrees()),
                at.latitudeDegrees() >= 0 ? "N" : "S", Math.abs(at.eastLongitudeDegrees()),
                at.eastLongitudeDegrees() >= 0 ? (norwegian ? "Ø" : "E") : (norwegian ? "V" : "W")));
        lines.add(utc(at.instant()));
        double lst = new LocalSky(at).localSiderealTimeDegrees() / 15.0;
        int minutes = (int) Math.floor(lst * 60.0) % (24 * 60);
        lines.add(String.format(Locale.ROOT, "%s %02dh %02dm", norwegian ? "Stjernetid" : "LST",
                minutes / 60, minutes % 60));
        lines.add(screen ? (norwegian ? "[ Oppdater til nå ]" : "[ Update to now ]")
                : (norwegian ? "Kartet er frosset ved dette tidspunktet."
                        : "The chart is frozen at this instant."));
        return lines;
    }

    /**
     * The proposal's allocation: the first corner, in the chart's order -
     * lower right, upper right while the magnitude key is off, upper left -
     * where the box fits inside the margins and meets no furniture that
     * will draw; none, and the box is refused, as the title block is.
     */
    static Corner allocate(ChartComponent chart, boolean norwegian, Observer at) {
        ChartScene scene = chart.currentScene();
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT,
                norwegian ? NORSK : ENGLISH);
        Rectangle title = chart.chartOptions().titleBlock()
                ? renderer.titleBlockBounds(metrics(false), scene) : null;
        Rectangle key = chart.chartOptions().magnitudeKey()
                ? renderer.magnitudeKeyBounds(metrics(false), scene) : null;
        List<String> lines = boxLines(at, norwegian, true);
        for (Corner corner : Corner.values()) {
            Rectangle box = boxBounds(scene, lines, corner);
            if (box != null && (title == null || !box.intersects(title))
                    && (key == null || !box.intersects(key))) {
                return corner;
            }
        }
        return null;
    }

    private static Rectangle boxBounds(ChartScene scene, List<String> lines, Corner corner) {
        FontMetrics plain = metrics(false);
        FontMetrics bold = metrics(true);
        int margin = rendererInt("TITLE_MARGIN_PX");
        int padding = rendererInt("TITLE_PADDING_PX");
        int width = bold.stringWidth(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            width = Math.max(width, plain.stringWidth(lines.get(i)));
        }
        width += 2 * padding;
        int height = lines.size() * plain.getHeight() + 2 * padding;
        int pageW = scene.viewport().widthPx();
        int pageH = scene.viewport().heightPx();
        if (width + 2 * margin > pageW || height + 2 * margin > pageH) {
            return null;
        }
        int x = corner == Corner.UPPER_LEFT ? margin : pageW - margin - width;
        int y = corner == Corner.LOWER_RIGHT ? pageH - margin - height : margin;
        return new Rectangle(x, y, width + 1, height + 1);
    }

    private static Rectangle drawBox(Graphics2D g, ChartScene scene, ChartPalette palette,
                                     List<String> lines, Corner corner, boolean screen) {
        if (corner == null) {
            return null;
        }
        Rectangle box = boxBounds(scene, lines, corner);
        if (box == null) {
            return null;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int padding = rendererInt("TITLE_PADDING_PX");
        g.setColor(palette.ground());
        g.fillRect(box.x, box.y, box.width - 1, box.height - 1);
        g.setColor(palette.frameInk());
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(box.x, box.y, box.width - 1, box.height - 1);
        FontMetrics plain = g.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        int y = box.y + padding + plain.getAscent();
        for (int i = 0; i < lines.size(); i++) {
            boolean action = screen && i == lines.size() - 1;
            g.setFont(new Font(Font.SANS_SERIF, i == 0 ? Font.BOLD : Font.PLAIN, 11));
            g.setColor(palette.textInk());
            String text = action ? lines.get(i).replace("[ ", "").replace(" ]", "") : lines.get(i);
            g.drawString(text, box.x + padding + (action ? 4 : 0), y);
            if (action) {
                // The one control: a framed action, in the box's own ink.
                int tw = plain.stringWidth(text);
                g.drawRoundRect(box.x + padding, y - plain.getAscent(), tw + 8,
                        plain.getHeight(), 5, 5);
            }
            y += plain.getHeight();
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        g.setColor(new Color(0xB0, 0x30, 0x30));
        g.drawString("STUDY", box.x + box.width - 34, box.y + 10);
        return box;
    }

    // ---- helpers ------------------------------------------------------------------------

    private static Graphics2D schematic(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        return g;
    }

    private static void heading(Graphics2D g, String text, int x, int y) {
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.drawString(text, x, y);
    }

    private static void button(Graphics2D g, String text, int x, int y) {
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        FontMetrics m = g.getFontMetrics(font);
        int w = m.stringWidth(text) + 16;
        g.setColor(new Color(0xFF, 0xFF, 0xFF));
        g.fillRoundRect(x, y, w, 20, 6, 6);
        g.setColor(new Color(0x55, 0x55, 0x55));
        g.drawRoundRect(x, y, w, 20, 6, 6);
        g.setColor(Color.BLACK);
        g.setFont(font);
        g.drawString(text, x + 8, y + 14);
    }

    private static void stamp(Graphics2D g, int w, int h) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g.setColor(new Color(0xB0, 0x30, 0x30));
        g.drawString("STUDY - schematic, not a photograph", w - 210, h - 8);
    }

    private static FontMetrics metrics(boolean bold) {
        Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        FontMetrics m = g.getFontMetrics(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, 11));
        g.dispose();
        return m;
    }

    /** The renderer's own furniture constant, read rather than restated. */
    private static int rendererInt(String field) {
        try {
            java.lang.reflect.Field f = ChartRenderer.class.getDeclaredField(field);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("the renderer's " + field, e);
        }
    }

    private static String utc(Instant instant) {
        return DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
                .withZone(ZoneOffset.UTC).format(instant) + " UTC";
    }

    private static Instant elongation() throws IOException {
        Map<String, Instant> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(VENUS_EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            String iso = f[1].length() == 17 ? f[1].substring(0, 16) + ":00Z" : f[1];
            out.put(f[0], Instant.parse(iso));
        }
        return out.get("2026-greatest-elongation-east");
    }
}
