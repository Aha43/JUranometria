package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.NeptuneService;
import juranometria.solar.NeptuneService.NeptuneObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Neptune on the chart, as the production composition draws it (issue
 * #659, parts 1 and 2, under the rulings on #658): the Solar System module
 * attached to the real component with Neptune switched on, production ink
 * only, no study overlay. Espenak's cited rows in
 * {@code docs/studies/neptune-system/neptune-events.txt} - the 2026
 * opposition, the 2026 conjunction beside the Sun and the 2003 conjunction
 * behind it - at the ordinary fields and the closer fields the controller
 * reaches (R-8), both palettes and both languages; the mark measured from
 * the rendered pixels as drawn full (R-2); and, since part 2, Triton and
 * Nereid at the floor and the closer fields only, by Mars's clearance rule
 * there (R-5, R-6a): at their extremes, and in the states Horizons codes
 * from Oslo. Written
 * to {@code docs/studies/neptune-on-the-chart/} by
 * {@code make neptune-on-the-chart-study}: the report to stdout, the pages
 * beside it.
 */
public final class NeptuneOnTheChartStudyMain {

    private static final File DIR = new File("docs/studies/neptune-on-the-chart");
    private static final Path EVENTS = Path.of("docs/studies/neptune-system/neptune-events.txt");
    /** The contract's observer (#500): Oslo at sea level. */
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static SolarSystemService solar;
    private static NeptuneService neptune;

    private NeptuneOnTheChartStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        try {
            solar = SolarSystemService.load();
            neptune = NeptuneService.load();
            write();
        } finally {
            // The portable contract runs every generator in one JVM: the
            // services are released, as #658's study releases its own.
            solar = null;
            neptune = null;
        }
        System.out.print(REPORT);
        System.out.flush();
    }

    private static void write() throws Exception {
        Map<String, Instant> events = events();
        p("# Neptune on the chart");
        p("");
        p("Issue #659, parts 1 and 2. The production composition - `ChartComponent` over the"
                + " bundled catalogue, with the Solar System module attached and Neptune"
                + " switched on - under the rulings on #658: rule M with the 6 px mark always"
                + " standing in (R-1), drawn full and round with no colour (R-2), the pole not"
                + " drawn and no rings (R-3, R-4), the planet selectable and pointed at as an"
                + " occurrence (R-9), eligible for the closer fields (R-8), on screen and on"
                + " paper as the other bodies (R-10). Since part 2 Triton and Nereid are drawn as"
                + " 3 px symbols at their own J2000 places, offered only at the 1° field and the"
                + " closer fields, each there only where it stands clear of Neptune's mark and of"
                + " the other moon (R-5, R-6a); on wider fields no moon is drawn or spoken; a moon"
                + " behind, in transit or too close is not drawn and the page says so; none is"
                + " selectable (R-9). The instants are Espenak's rows in"
                + " `docs/studies/neptune-system/neptune-events.txt`; positions are the service's"
                + " for Oslo at sea level. Regenerate with `make neptune-on-the-chart-study`.");
        p("");
        Instant opposition = events.get("2026 Sep 26 Neptune at Opposition");
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, opposition);
        NeptuneObservation u = neptune.observe(oslo);
        p("## The 2026 opposition, " + opposition + ", Oslo");
        p("");
        p(says(u));
        p("");
        page("opposition-2026-8-nb", "8°, the page's words in Norwegian", oslo, u, 8.0, 0,
                NORSK, ChartPalette.WHITE_PAPER, false,
                "The true disc is a tenth of a pixel at 8°: the 6 px mark stands in, and the page"
                        + " calls it a symbol.");
        page("opposition-2026-3-dark", "3° on the black sky", oslo, u, 3.0, 0, ENGLISH,
                ChartPalette.BLACK_SKY, false,
                "The mark in the black sky's star ink; no colour (R-2).");
        page("opposition-2026-1", "The minimum field, 1°", oslo, u,
                ChartViewState.normalMinimumFieldDegrees(), 0, ENGLISH, ChartPalette.WHITE_PAPER,
                false, "Under a pixel even at the minimum field: still the mark.");
        page("opposition-2026-0.5-nb", "The 0.5° closer field, in Norwegian", oslo, u,
                ChartViewState.normalMinimumFieldDegrees(), 1, NORSK, ChartPalette.WHITE_PAPER,
                false, "Reached by zooming in from 1°, as the application offers it with Neptune"
                        + " on the page (R-8).");
        ChartComponent closest = page("opposition-2026-0.25-dark", "The 0.25° closer field on the"
                        + " black sky", oslo, u, ChartViewState.normalMinimumFieldDegrees(), 2,
                ENGLISH, ChartPalette.BLACK_SKY, false,
                "Under 4 px even here: the mark still stands in (R-1).");
        full(closest, u);
        p("## The 2026 conjunction, Oslo, with the Sun: beside it");
        p("");
        Instant conjunction = events.get("2026 Mar 22 Neptune in Conjunction with Sun");
        Observer atConjunction = new Observer(OSLO_LAT, OSLO_LON, conjunction);
        NeptuneObservation c = neptune.observe(atConjunction);
        p(says(c) + (c.behindSun() ? " Its disc meets the Sun's: behind the Sun."
                : " The conjunction is in ecliptic longitude: the mark stands beside the Sun's disc,"
                        + " not behind it."));
        p("");
        page("conjunction-2026-3", "At 3°", atConjunction, c, 3.0, 0, ENGLISH,
                ChartPalette.WHITE_PAPER, true,
                "The mark beside the production Sun, a degree and more from its centre; nothing"
                        + " covers it and the page states no Sun relation.");
        p("## The 2003 conjunction, Oslo, with the Sun: behind it");
        p("");
        Instant behind = events.get("2003 Jan 30 Neptune in Conjunction with Sun");
        Observer atBehind = new Observer(OSLO_LAT, OSLO_LON, behind);
        NeptuneObservation b = neptune.observe(atBehind);
        p(says(b) + (b.behindSun() ? " Its disc meets the Sun's: behind the Sun." : ""));
        p("");
        page("conjunction-2003-1", "At 1°", atBehind, b,
                ChartViewState.normalMinimumFieldDegrees(), 0, ENGLISH,
                ChartPalette.WHITE_PAPER, true,
                "The mark laid beneath the stars and covered by the Sun's ground-filled disc;"
                        + " the page says *behind the Sun* (#514).");
        page("conjunction-2003-1-nb", "At 1°, in Norwegian on the black sky", atBehind, b,
                ChartViewState.normalMinimumFieldDegrees(), 0, NORSK, ChartPalette.BLACK_SKY,
                true, "The same, spoken in Norwegian.");
        moons();
        p("## Outside the years");
        p("");
        Observer before = new Observer(OSLO_LAT, OSLO_LON, Instant.parse("1899-12-31T12:00:00Z"));
        ChartComponent early = compose(new ChartViewState(u.astrometricJ2000(), 8.0, 8.0, null,
                null), ENGLISH, ChartPalette.WHITE_PAPER, before, false);
        p(String.format(Locale.ROOT, "At %s - before the Neptune System pack's first day, %s -"
                + " the module draws nothing for Neptune: %d bodies drawn, and the page says"
                + " nothing of it.", before.instant(), NeptuneService.NEPTUNE_FIRST_DAY,
                early.renderedBodies().size()));
        p("");
    }

    /**
     * The moons (#659 part 2): Triton at its widest and Nereid at its widest
     * and its nearest in the weekly Oslo matrix (the cartography study's
     * section A: 2045-11-04, 2018-08-25 and 2008-03-22), the states Horizons
     * codes from Oslo - Triton behind Neptune in its shadow (U, 2044-09-26)
     * and in front (t, 2043-09-07) - and Neptune alone before the moons' years.
     */
    private static void moons() throws Exception {
        p("## The moons");
        p("");
        Instant tritonWidest = Instant.parse("2045-11-04T00:00:00Z");
        Observer atTriton = new Observer(OSLO_LAT, OSLO_LON, tritonWidest);
        NeptuneObservation t = neptune.observe(atTriton);
        p("### Triton at its widest, " + tritonWidest + ", Oslo");
        p("");
        page("triton-widest-2045-1", "The minimum field, 1°", atTriton, t,
                ChartViewState.normalMinimumFieldDegrees(), 0, ENGLISH, ChartPalette.WHITE_PAPER,
                false, "Even at its widest Triton stands on Neptune's mark at 1°: spoken, not drawn;"
                        + " Nereid where it stands.");
        page("triton-widest-2045-0.25-nb", "The 0.25° closer field, in Norwegian", atTriton, t,
                ChartViewState.normalMinimumFieldDegrees(), 2, NORSK, ChartPalette.WHITE_PAPER,
                false, "Triton clear of the mark at 0.25°.");
        page("triton-widest-2045-8", "8°", atTriton, t, 8.0, 0, ENGLISH, ChartPalette.WHITE_PAPER,
                false, "A wider field: the moons are not offered (R-6a), so none is drawn and none"
                        + " is spoken.");
        Instant nereidWidest = Instant.parse("2018-08-25T00:00:00Z");
        Observer atNereid = new Observer(OSLO_LAT, OSLO_LON, nereidWidest);
        p("### Nereid at its widest, " + nereidWidest + ", Oslo");
        p("");
        page("nereid-widest-2018-1", "The minimum field, 1°", atNereid, neptune.observe(atNereid),
                ChartViewState.normalMinimumFieldDegrees(), 0, ENGLISH, ChartPalette.BLACK_SKY,
                false, "Nereid far out on the 1° page, among stars at least ten magnitudes"
                        + " brighter; this is the widest field it is ever drawn on.");
        page("nereid-widest-2018-2", "2°", atNereid, neptune.observe(atNereid), 2.0, 0, ENGLISH,
                ChartPalette.WHITE_PAPER, false,
                "At 2° Nereid would stand clear, and is not offered: Neptune alone (R-6a).");
        Instant nereidNearest = Instant.parse("2008-03-22T00:00:00Z");
        Observer atNearest = new Observer(OSLO_LAT, OSLO_LON, nereidNearest);
        p("### Nereid at its nearest, " + nereidNearest + ", Oslo");
        p("");
        page("nereid-nearest-2008-0.25", "The 0.25° closer field", atNearest,
                neptune.observe(atNearest), ChartViewState.normalMinimumFieldDegrees(), 2, ENGLISH,
                ChartPalette.WHITE_PAPER, false,
                "At its least separation Nereid stands on the mark even at 0.25°: spoken, not drawn.");
        state("triton-behind-2044-0.25", "Triton behind Neptune in its shadow (Horizons U, 2044-09-26)",
                Instant.parse("2044-09-26T00:00:00Z"), ENGLISH, ChartPalette.WHITE_PAPER,
                "Behind: not drawn, and spoken.");
        state("triton-in-front-2043-0.25-dark", "Triton in front of Neptune (Horizons t, 2043-09-07)",
                Instant.parse("2043-09-07T00:00:00Z"), NORSK, ChartPalette.BLACK_SKY,
                "The mark cannot hold a transit: not drawn, and spoken as too close (R-7).");
        Instant before = Instant.parse("1999-12-31T12:00:00Z");
        Observer atBefore = new Observer(OSLO_LAT, OSLO_LON, before);
        p("### Before the moons' years, " + before);
        p("");
        page("before-the-moons-1999-0.25", "The 0.25° closer field", atBefore,
                neptune.observe(atBefore), ChartViewState.normalMinimumFieldDegrees(), 2, ENGLISH,
                ChartPalette.WHITE_PAPER, false, "Neptune alone, as the table shows it.");
    }

    private static void state(String name, String title, Instant when, PageText words,
                              ChartPalette palette, String caption) throws Exception {
        Observer at = new Observer(OSLO_LAT, OSLO_LON, when);
        p("### " + title);
        p("");
        page(name, "The 0.25° closer field", at, neptune.observe(at),
                ChartViewState.normalMinimumFieldDegrees(), 2, words, palette, false, caption);
    }

    /** What the Neptune table says of this instant, in the page's terms. */
    private static String says(NeptuneObservation v) {
        return String.format(Locale.ROOT, "The service: %.2f″ across, %.3f %% lit (phase angle"
                        + " %.2f°), tilt %.1f°, %.1f° from the Sun.",
                v.equatorialDiameterArcseconds(), 100.0 * v.illuminatedFraction(),
                v.phaseAngleDegrees(), v.tiltDegrees(), v.elongationDegrees());
    }

    /** The description's last body sentence: Neptune's, in either language. */
    private static String spokenTail(String said) {
        int at = said.lastIndexOf(". Neptune");
        return (at < 0 ? said : said.substring(at + 1)).strip();
    }

    /** Espenak's rows, keyed by date and event: "2026 Sep 26 Neptune at Opposition". */
    private static Map<String, Instant> events() throws IOException {
        Map<String, Instant> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#") || !Character.isDigit(line.charAt(0))) {
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
        NeptuneService u = neptune;
        SolarSystemModule module = new SolarSystemModule(() -> observer, () -> s, () -> false);
        module.withNeptune(() -> u);
        module.neptuneShowing(true);
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
     * controller: centred on Neptune at the field given, then zoomed in
     * {@code closerSteps} times into the closer fields the controller
     * offers with Neptune on the page.
     */
    private static ChartComponent page(String name, String title, Observer observer,
                                       NeptuneObservation v, double field, int closerSteps,
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
                    SolarSystemModule.VENUS, SolarSystemModule.MARS, SolarSystemModule.URANUS,
                    SolarSystemModule.NEPTUNE));
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
     * The mark as painted at the closest field, measured from the rendered
     * pixels inside its 1 px outline: every one lit (R-2), where a phase of
     * k = 99.97 % would darken none of them either.
     */
    private static void full(ChartComponent chart, NeptuneObservation v) throws Exception {
        BufferedImage image = paint(chart);
        ReferenceInk.BodyPlacement drawn = chart.renderedBodies().stream()
                .filter(b -> SolarSystemModule.NEPTUNE.equals(b.identity())).findFirst().orElseThrow();
        double cx = drawn.centre().x() + chart.pageOffsetX();
        double cy = drawn.centre().y() + chart.pageOffsetY();
        double r = drawn.disc().getBounds2D().getWidth() / 2.0 - 1.0;
        int darkTone = (int) Math.round(255 * ReferenceInk.darkSide());
        double lit = 0.0;
        double all = 0.0;
        for (int y = (int) Math.floor(cy - r); y <= Math.ceil(cy + r); y++) {
            for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
                if (Math.hypot(x + 0.5 - cx, y + 0.5 - cy) > r) {
                    continue;
                }
                int red = new Color(image.getRGB(x, y)).getRed();
                all++;
                if (red > (darkTone + 255) / 2) {
                    lit++;
                }
            }
        }
        p(String.format(Locale.ROOT, "Measured inside the mark's outline at 0.25°: %.0f of %.0f"
                + " pixels in the lit tone, against k = %.3f %%. The mark is drawn full (R-2).",
                lit, all, 100.0 * v.illuminatedFraction()));
        p("");
        if (all == 0 || lit < all) {
            System.out.print(REPORT);
            System.out.flush();
            throw new IllegalStateException("the mark is not drawn full: " + lit + " of " + all);
        }
    }

    private static String trim(double field) {
        return field == Math.rint(field) ? String.valueOf((long) field) : String.valueOf(field);
    }
}
