package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.project.DrawnPage;
import juranometria.project.PageBasis;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;
import juranometria.solar.MarsService;
import juranometria.solar.MarsService.LimbConditioning;
import juranometria.solar.MarsService.MarsObservation;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Mars on the chart, as the production composition draws it (issue #606,
 * part 1, under the rulings on #605): the Solar System module attached to
 * the real component with Mars switched on, production ink only, no study
 * overlay. Espenak's cited rows in
 * {@code docs/studies/mars-system/mars-events.txt} - the 2003 closest
 * opposition and the 2026 conjunction - at the ordinary fields and the
 * closer fields the controller reaches (R-6), both palettes and both
 * languages; Mars behind the Sun at the hour of 1912 the service finds it
 * deepest; and the day of the 2018 perihelic apparition on which Mars's
 * dark side is widest on the sky, its drawn lit share and facing measured
 * from the rendered pixels against the service's own k and χ. Written to
 * {@code docs/studies/mars-on-the-chart/} by
 * {@code make mars-on-the-chart-study}: the report to stdout, the pages
 * beside it.
 */
public final class MarsOnTheChartStudyMain {

    private static final File DIR = new File("docs/studies/mars-on-the-chart");
    private static final Path EVENTS = Path.of("docs/studies/mars-system/mars-events.txt");
    /** The contract's observer (#500): Oslo at sea level. */
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static SolarSystemService solar;
    private static MarsService mars;

    private MarsOnTheChartStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        try {
            solar = SolarSystemService.load();
            mars = MarsService.load();
            write();
        } finally {
            // The portable contract runs every generator in one JVM: the
            // services are released, as #605's study releases its own.
            solar = null;
            mars = null;
        }
        System.out.print(REPORT);
        System.out.flush();
    }

    private static void write() throws Exception {
        Map<String, Instant> events = events();
        p("# Mars on the chart");
        p("");
        p("Issue #606, part 1. The production composition - `ChartComponent` over the"
                + " bundled catalogue, with the Solar System module attached and Mars switched"
                + " on - under the rulings on #605: rule M phased (R-1), the larger of the true"
                + " equatorial disc and a 6 px mark, the lit region turned through the page's"
                + " north and east at Mars; no colour (R-2); the planet selectable and pointed"
                + " at as an occurrence (R-5); eligible for the closer fields (R-6); on screen"
                + " and on paper as the other bodies (R-7). The moons are not drawn: they are"
                + " part 2 (R-8). The instants are Espenak's rows in"
                + " `docs/studies/mars-system/mars-events.txt` and hours the service itself"
                + " finds; positions and phases are the service's for Oslo at sea level."
                + " Regenerate with `make mars-on-the-chart-study`.");
        p("");
        Instant opposition = events.get("2003 Aug 28 Mars at Opposition");
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, opposition);
        MarsObservation m = mars.observe(oslo);
        p("## The 2003 closest opposition, " + opposition + ", Oslo");
        p("");
        p(says(m));
        p("");
        page("opposition-2003-8-nb", "8°, the page's words in Norwegian", oslo, m, 8.0, 0,
                NORSK, ChartPalette.WHITE_PAPER, false,
                "The true disc is under a pixel at 8°: the 6 px mark stands in, and the page"
                        + " calls it a symbol.");
        page("opposition-2003-3-dark", "3° on the black sky", oslo, m, 3.0, 0, ENGLISH,
                ChartPalette.BLACK_SKY, false,
                "The mark in the black sky's star ink; no colour (R-2).");
        page("opposition-2003-1", "The minimum field, 1°", oslo, m,
                ChartViewState.normalMinimumFieldDegrees(), 0, ENGLISH, ChartPalette.WHITE_PAPER,
                false, "25″ is a little over 6 px at 1°: the true disc, near full, and no"
                        + " longer a symbol.");
        page("opposition-2003-0.5-nb", "The 0.5° closer field, in Norwegian", oslo, m,
                ChartViewState.normalMinimumFieldDegrees(), 1, NORSK, ChartPalette.WHITE_PAPER,
                false, "Reached by zooming in from 1°, as the application offers it with Mars"
                        + " on the page (R-6).");
        page("opposition-2003-0.25-dark", "The 0.25° closer field on the black sky", oslo, m,
                ChartViewState.normalMinimumFieldDegrees(), 2, ENGLISH, ChartPalette.BLACK_SKY,
                false, "The true disc some 25 px across.");
        p("## The conjunctions, Oslo, with the Sun");
        p("");
        Instant conjunction = events.get("2026 Jan 09 Mars in Conjunction with Sun");
        Observer atConjunction = new Observer(OSLO_LAT, OSLO_LON, conjunction);
        MarsObservation c = mars.observe(atConjunction);
        p("### The 2026 conjunction, " + conjunction);
        p("");
        p(says(c));
        p("");
        page("conjunction-2026-3", "At 3°", atConjunction, c, 3.0, 0, ENGLISH,
                ChartPalette.WHITE_PAPER, true,
                "Mars passes north of the Sun's disc at this conjunction: the mark beside the"
                        + " Sun, beneath the stars, near full.");
        Instant behind = deepestBehindTheSun();
        Observer atBehind = new Observer(OSLO_LAT, OSLO_LON, behind);
        MarsObservation b = mars.observe(atBehind);
        p("### Behind the Sun, " + behind);
        p("");
        p("The hour of 1912 the service puts Mars geocentrically deepest behind the Sun's"
                + " disc (no almanac row is quoted for it). " + says(b));
        p("");
        page("behind-the-sun-1912-1", "At 1°", atBehind, b,
                ChartViewState.normalMinimumFieldDegrees(), 0, ENGLISH,
                ChartPalette.WHITE_PAPER, true,
                "The mark laid beneath the stars and covered by the Sun's ground-filled disc;"
                        + " the page says *behind the Sun*.");
        page("behind-the-sun-1912-1-nb", "At 1°, in Norwegian on the black sky", atBehind, b,
                ChartViewState.normalMinimumFieldDegrees(), 0, NORSK, ChartPalette.BLACK_SKY,
                true, "The same, spoken in Norwegian.");
        gibbous(Instant.parse("2018-01-01T00:00:00Z"), Instant.parse("2019-01-01T00:00:00Z"));
        p("## Outside the years");
        p("");
        Observer before = new Observer(OSLO_LAT, OSLO_LON, Instant.parse("1899-12-31T12:00:00Z"));
        ChartComponent early = compose(new ChartViewState(m.astrometricJ2000(), 8.0, 8.0, null,
                null), ENGLISH, ChartPalette.WHITE_PAPER, before, false);
        p(String.format(Locale.ROOT, "At %s - before the planets pack's first day, %s - the"
                + " module draws nothing for Mars: %d bodies drawn, and the page says nothing"
                + " of it.", before.instant(), MarsService.MARS_FIRST_DAY,
                early.renderedBodies().size()));
        p("");
    }

    /** What the Mars table says of this instant, in the page's terms. */
    private static String says(MarsObservation v) {
        return String.format(Locale.ROOT, "The service: %.1f″ across, %.1f %% lit, phase %s,"
                        + " χ %.1f° (%s), %.1f° from the Sun.",
                v.equatorialDiameterArcseconds(), 100.0 * v.illuminatedFraction(),
                v.phase().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                v.brightLimbAngleDegrees(),
                v.brightLimbConditioning() == LimbConditioning.WELL_DEFINED
                        ? "well defined" : "not well defined",
                v.elongationDegrees());
    }

    /** The description's last body sentence: Mars's, in either language. */
    private static String spokenTail(String said) {
        int at = said.lastIndexOf(". Mars");
        return (at < 0 ? said : said.substring(at + 1)).strip();
    }

    /** Espenak's rows, keyed by date and event: "2003 Aug 28 Mars at Opposition". */
    private static Map<String, Instant> events() throws IOException {
        Map<String, Instant> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.strip().split(" ", 5);
            String hour = f[3].contains(":") ? f[3] : f[3] + ":00";
            Instant when = java.time.LocalDateTime.parse(f[0] + " " + f[1] + " " + f[2] + " "
                    + hour, java.time.format.DateTimeFormatter.ofPattern("yyyy MMM dd H:mm",
                    Locale.ENGLISH)).toInstant(java.time.ZoneOffset.UTC);
            out.put(f[0] + " " + f[1] + " " + f[2] + " " + f[4].strip(), when);
        }
        return out;
    }

    /** The hour of 1912 the service puts Mars deepest behind the Sun, geocentrically. */
    private static Instant deepestBehindTheSun() {
        Instant best = null;
        double least = 360.0;
        for (Instant day = Instant.parse("1912-01-01T00:00:00Z");
                day.isBefore(Instant.parse("1913-01-01T00:00:00Z"));
                day = day.plus(Duration.ofDays(1))) {
            double e = mars.observeGeocentric(day).elongationDegrees();
            if (e < least) {
                least = e;
                best = day;
            }
        }
        Instant around = best;
        for (int h = -24; h <= 24; h++) {
            Instant t = around.plus(Duration.ofHours(h));
            double e = mars.observeGeocentric(t).elongationDegrees();
            if (e < least) {
                least = e;
                best = t;
            }
        }
        return best;
    }

    private static ChartComponent compose(ChartViewState state, PageText words,
                                          ChartPalette palette, Observer observer,
                                          boolean withSun) throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            holder[0].setViewState(state);
            holder[0].overlays().offer(SolarSystemModule.ID, module(observer, withSun)
                    ::contributedGeometry);
        });
        SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
    }

    private static SolarSystemModule module(Observer observer, boolean withSun) {
        SolarSystemService s = solar;
        MarsService m = mars;
        SolarSystemModule module = new SolarSystemModule(() -> observer, () -> s, () -> false);
        module.withMars(() -> m);
        module.marsShowing(true);
        module.sunShowing(withSun);
        return module;
    }

    private static BufferedImage paint(ChartComponent chart) throws Exception {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image;
    }

    /**
     * A page of the application's, through its component, module and
     * controller: centred on Mars at the field given, then zoomed in
     * {@code closerSteps} times into the closer fields the controller
     * offers with Mars on the page.
     */
    private static ChartComponent page(String name, String title, Observer observer,
                                       MarsObservation v, double field, int closerSteps,
                                       PageText words, ChartPalette palette, boolean withSun,
                                       String caption) throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        ChartViewController[] controller = new ChartViewController[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            controller[0] = new ChartViewController();
            controller[0].onChange(holder[0]::setViewState);
            holder[0].setCloserFieldBodies(java.util.Set.of(SolarSystemModule.MERCURY,
                    SolarSystemModule.VENUS, SolarSystemModule.MARS));
            controller[0].offerCloserFields(holder[0]::offersCloserField);
            holder[0].onPageContentsChange(controller[0]::closerOfferChanged);
            controller[0].recenter(v.astrometricJ2000(), field);
            holder[0].overlays().offer(SolarSystemModule.ID, module(observer, withSun)
                    ::contributedGeometry);
        });
        for (int i = 0; i < closerSteps; i++) {
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(controller[0]::zoomIn);
        }
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        BufferedImage image = paint(chart);
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        StringBuilder note = new StringBuilder();
        for (ReferenceInk.BodyPlacement body : chart.renderedBodies()) {
            note.append(String.format(Locale.ROOT, "%s at (%.1f, %.1f), %s %.1f px%s, name %s; ",
                    body.name(), body.centre().x(), body.centre().y(),
                    body.symbol() ? "symbol" : "disc", body.disc().getBounds2D().getWidth(),
                    body.state() == null ? "" : ", state " + body.state(),
                    body.box() == null ? "refused" : "placed"));
        }
        p("### " + title + String.format(Locale.ROOT, " (the page's field %s°)",
                trim(chart.currentScene().viewport().fieldWidthDegrees())));
        p("");
        p("![](" + name + ".png)");
        p("");
        p(caption + " Drawn: " + (note.length() == 0 ? "nothing" : note.toString().strip())
                + " Spoken: " + spokenTail(chart.getAccessibleContext()
                        .getAccessibleDescription()));
        p("");
        return chart;
    }

    /**
     * The day in the years given on which Mars's dark side is widest on
     * the sky - the defect of illumination, (1 - k) times the disc - at the
     * 0.25° closer field: the lit share and the facing of the lit side
     * measured from the rendered pixels inside the disc's 1 px outline,
     * set beside the service's k and χ. Mars is never less than some 84 %
     * lit, so the defect is a few pixels at most even here.
     */
    private static void gibbous(Instant from, Instant to) throws Exception {
        Instant best = from;
        double widest = 0.0;
        for (Instant day = from; day.isBefore(to); day = day.plus(Duration.ofDays(1))) {
            MarsObservation g = mars.observeGeocentric(day);
            double defect = (1.0 - g.illuminatedFraction()) * g.equatorialDiameterArcseconds();
            if (defect > widest) {
                widest = defect;
                best = day;
            }
        }
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, best);
        MarsObservation v = mars.observe(oslo);
        p("## The widest dark side of the 2018 apparition, " + best);
        p("");
        p(String.format(Locale.ROOT, "The day of 2018 on which the service finds Mars's"
                + " defect of illumination widest on the sky, geocentrically, %.1f″; drawn for"
                + " Oslo. ", widest) + says(v));
        p("");
        ChartComponent chart = page("gibbous-2018-0.25", "The 0.25° closer field", oslo, v,
                ChartViewState.normalMinimumFieldDegrees(), 2, ENGLISH, ChartPalette.WHITE_PAPER,
                false, "The dark side a narrow band on the limb away from the Sun.");
        BufferedImage image = paint(chart);
        ReferenceInk.BodyPlacement drawn = chart.renderedBodies().get(0);
        double cx = drawn.centre().x() + chart.pageOffsetX();
        double cy = drawn.centre().y() + chart.pageOffsetY();
        double r = drawn.disc().getBounds2D().getWidth() / 2.0 - 1.0;
        double lit = 0.0;
        double all = 0.0;
        double[] darkSum = {0.0, 0.0};
        int darkTone = (int) Math.round(255 * ReferenceInk.darkSide());
        for (int y = (int) Math.floor(cy - r); y <= Math.ceil(cy + r); y++) {
            for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                if (Math.hypot(dx, dy) > r) {
                    continue;
                }
                int red = new Color(image.getRGB(x, y)).getRed();
                all++;
                if (red > (darkTone + 255) / 2) {
                    lit++;
                } else {
                    darkSum[0] += dx;
                    darkSum[1] += dy;
                }
            }
        }
        double share = all == 0 ? 0.0 : lit / all;
        PageBasis basis = PageBasis.at(DrawnPage.of(chart.currentScene()),
                v.astrometricJ2000()).orElseThrow();
        double facing = basis.positionAngleDegrees(-darkSum[0], -darkSum[1]);
        double off = Math.abs(((facing - v.brightLimbAngleDegrees()) % 360.0 + 540.0)
                % 360.0 - 180.0);
        p(String.format(Locale.ROOT, "Measured inside the outline: %.0f of %.0f pixels lit,"
                        + " **%.0f %%** against k = %.1f %%; the lit side faces **%.0f°**"
                        + " (read from the dark side's pixels through the page's north and"
                        + " east), %.0f° from the service's χ = %.1f°. The outline's ring is"
                        + " left out of the count and takes the dark side's outermost pixels"
                        + " with it, so the share reads a few points high; what is held is that"
                        + " the disc is drawn short of full and the dark side lies opposite χ,"
                        + " within 45°.",
                lit, all, 100.0 * share, 100.0 * v.illuminatedFraction(), facing, off,
                v.brightLimbAngleDegrees()));
        p("");
        if (off > 45.0 || share >= 1.0 || share < 0.5) {
            System.out.print(REPORT);
            System.out.flush();
            throw new IllegalStateException("the drawn phase disagrees with the service: lit "
                    + share + ", facing " + off + "° from χ");
        }
    }

    private static String trim(double field) {
        return field == Math.rint(field) ? String.valueOf((long) field) : String.valueOf(field);
    }
}
