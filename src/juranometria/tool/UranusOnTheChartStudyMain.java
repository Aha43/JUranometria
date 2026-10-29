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
import juranometria.solar.UranusService;
import juranometria.solar.UranusService.UranusObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * Uranus on the chart, as the production composition draws it (issue
 * #638, parts 1 and 2, under the rulings on #637): the Solar System module
 * attached to the real component with Uranus switched on, production ink
 * only, no study overlay. Espenak's cited rows in
 * {@code docs/studies/uranus-system/uranus-events.txt} - the 2026
 * opposition and conjunction - at the ordinary fields and the closer
 * fields the controller reaches (R-7), both palettes and both languages;
 * the mark measured from the rendered pixels as drawn full (R-2); and,
 * since part 2, the five moons by Mars's clearance rule (R-5): at their
 * widest, and in the states Horizons codes from Oslo. Written
 * to {@code docs/studies/uranus-on-the-chart/} by
 * {@code make uranus-on-the-chart-study}: the report to stdout, the pages
 * beside it.
 */
public final class UranusOnTheChartStudyMain {

    private static final File DIR = new File("docs/studies/uranus-on-the-chart");
    private static final Path EVENTS = Path.of("docs/studies/uranus-system/uranus-events.txt");
    /** The contract's observer (#500): Oslo at sea level. */
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();
    private static SolarSystemService solar;
    private static UranusService uranus;

    private UranusOnTheChartStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        try {
            solar = SolarSystemService.load();
            uranus = UranusService.load();
            write();
        } finally {
            // The portable contract runs every generator in one JVM: the
            // services are released, as #637's study releases its own.
            solar = null;
            uranus = null;
        }
        System.out.print(REPORT);
        System.out.flush();
    }

    private static void write() throws Exception {
        Map<String, Instant> events = events();
        p("# Uranus on the chart");
        p("");
        p("Issue #638, parts 1 and 2. The production composition - `ChartComponent` over the"
                + " bundled catalogue, with the Solar System module attached and Uranus"
                + " switched on - under the rulings on #637: rule M with the 6 px mark always"
                + " standing in (R-1), drawn full and round with no colour (R-2), the pole and"
                + " the rings not drawn (R-3, R-4), the planet selectable and pointed at as an"
                + " occurrence (R-8), eligible for the closer fields (R-7), on screen and on"
                + " paper as the other bodies (R-9). Since part 2 the five moons are drawn as 3 px"
                + " symbols at their own J2000 places, each only where it stands clear of Uranus's"
                + " mark and of the moons already drawn, on every field (R-5); a moon behind, in"
                + " transit or too close is not drawn and the page says so (R-6); none is"
                + " selectable (R-8). The instants are Espenak's rows in"
                + " `docs/studies/uranus-system/uranus-events.txt` and the Horizons rows in"
                + " `docs/studies/uranus-system/horizons/`; positions are the service's"
                + " for Oslo at sea level. Regenerate with `make uranus-on-the-chart-study`.");
        p("");
        Instant opposition = events.get("2026 Nov 25 Uranus at Opposition");
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, opposition);
        UranusObservation u = uranus.observe(oslo);
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
                false, "Reached by zooming in from 1°, as the application offers it with Uranus"
                        + " on the page (R-7).");
        ChartComponent closest = page("opposition-2026-0.25-dark", "The 0.25° closer field on the"
                        + " black sky", oslo, u, ChartViewState.normalMinimumFieldDegrees(), 2,
                ENGLISH, ChartPalette.BLACK_SKY, false,
                "Under 4 px even here: the mark still stands in (R-1).");
        full(closest, u);
        p("## The 2026 conjunction, Oslo, with the Sun");
        p("");
        Instant conjunction = events.get("2026 May 22 Uranus in Conjunction with Sun");
        Observer atConjunction = new Observer(OSLO_LAT, OSLO_LON, conjunction);
        UranusObservation c = uranus.observe(atConjunction);
        p(says(c) + (c.behindSun() ? " Its disc meets the Sun's: behind the Sun." : ""));
        p("");
        page("conjunction-2026-1", "At 1°", atConjunction, c,
                ChartViewState.normalMinimumFieldDegrees(), 0, ENGLISH,
                ChartPalette.WHITE_PAPER, true,
                "The mark laid beneath the stars and covered by the Sun's ground-filled disc;"
                        + " the page says *behind the Sun* (#514).");
        page("conjunction-2026-1-nb", "At 1°, in Norwegian on the black sky", atConjunction, c,
                ChartViewState.normalMinimumFieldDegrees(), 0, NORSK, ChartPalette.BLACK_SKY,
                true, "The same, spoken in Norwegian.");
        moons();
        p("## Outside the years");
        p("");
        Observer before = new Observer(OSLO_LAT, OSLO_LON, Instant.parse("1899-12-31T12:00:00Z"));
        ChartComponent early = compose(new ChartViewState(u.astrometricJ2000(), 8.0, 8.0, null,
                null), ENGLISH, ChartPalette.WHITE_PAPER, before, false);
        p(String.format(Locale.ROOT, "At %s - before the Uranus System pack's first day, %s -"
                + " the module draws nothing for Uranus: %d bodies drawn, and the page says"
                + " nothing of it.", before.instant(), UranusService.URANUS_FIRST_DAY,
                early.renderedBodies().size()));
        p("");
    }

    /**
     * The moons (#638 part 2): Titania and Oberon at their widest in the
     * weekly Oslo matrix (measurements.md, 2052-03-23), and the states
     * Horizons codes from Oslo - Miranda behind in the shadow (U, 2005-03-19),
     * in front (t, 2005-05-28), Ariel in the shadow (u, 2006-02-04) - with
     * Uranus alone before the moons' years.
     */
    private static void moons() throws Exception {
        p("## The moons");
        p("");
        Instant widest = Instant.parse("2052-03-23T00:00:00Z");
        Observer atWidest = new Observer(OSLO_LAT, OSLO_LON, widest);
        UranusObservation w = uranus.observe(atWidest);
        p("### Titania and Oberon at their widest, " + widest + ", Oslo");
        p("");
        page("moons-widest-2052-1", "The minimum field, 1°", atWidest, w,
                ChartViewState.normalMinimumFieldDegrees(), 0, ENGLISH, ChartPalette.WHITE_PAPER,
                false, "The outer moons stand clear of the mark; the inner ones are spoken.");
        page("moons-widest-2052-0.25-nb", "The 0.25° closer field, in Norwegian", atWidest, w,
                ChartViewState.normalMinimumFieldDegrees(), 2, NORSK, ChartPalette.WHITE_PAPER,
                false, "Where most of the system stands apart.");
        page("moons-widest-2052-8", "8°", atWidest, w, 8.0, 0, ENGLISH, ChartPalette.WHITE_PAPER,
                false, "No moon clears the mark: none drawn, each spoken (R-5, no floor).");
        state("miranda-behind-2005-0.25", "Miranda behind Uranus in its shadow (Horizons U, 2005-03-19)",
                Instant.parse("2005-03-19T00:00:00Z"), ENGLISH, ChartPalette.WHITE_PAPER,
                "Behind: not drawn, and spoken.");
        state("miranda-in-front-2005-0.25-dark", "Miranda in front of Uranus (Horizons t, 2005-05-28)",
                Instant.parse("2005-05-28T00:00:00Z"), ENGLISH, ChartPalette.BLACK_SKY,
                "The mark cannot hold a transit: not drawn, and spoken as too close (R-6).");
        state("ariel-shadow-2006-0.25-nb", "Ariel in Uranus's shadow (Horizons u, 2006-02-04)",
                Instant.parse("2006-02-04T00:00:00Z"), NORSK, ChartPalette.WHITE_PAPER,
                "An eclipsed moon stands beside the limb, not clear of the mark: not drawn, and spoken.");
        Instant before = Instant.parse("1999-12-31T12:00:00Z");
        Observer atBefore = new Observer(OSLO_LAT, OSLO_LON, before);
        p("### Before the moons' years, " + before);
        p("");
        page("before-the-moons-1999-0.25", "The 0.25° closer field", atBefore,
                uranus.observe(atBefore), ChartViewState.normalMinimumFieldDegrees(), 2, ENGLISH,
                ChartPalette.WHITE_PAPER, false, "Uranus alone, as the table shows it.");
    }

    private static void state(String name, String title, Instant when, PageText words,
                              ChartPalette palette, String caption) throws Exception {
        Observer at = new Observer(OSLO_LAT, OSLO_LON, when);
        p("### " + title);
        p("");
        page(name, "The 0.25° closer field", at, uranus.observe(at),
                ChartViewState.normalMinimumFieldDegrees(), 2, words, palette, false, caption);
    }

    /** What the Uranus table says of this instant, in the page's terms. */
    private static String says(UranusObservation v) {
        return String.format(Locale.ROOT, "The service: %.2f″ across, %.3f %% lit (phase angle"
                        + " %.2f°), tilt %.1f°, %.1f° from the Sun.",
                v.equatorialDiameterArcseconds(), 100.0 * v.illuminatedFraction(),
                v.phaseAngleDegrees(), v.tiltDegrees(), v.elongationDegrees());
    }

    /** The description's last body sentence: Uranus's, in either language. */
    private static String spokenTail(String said) {
        int at = said.lastIndexOf(". Uranus");
        return (at < 0 ? said : said.substring(at + 1)).strip();
    }

    /** Espenak's rows, keyed by date and event: "2026 Nov 25 Uranus at Opposition". */
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
        UranusService u = uranus;
        SolarSystemModule module = new SolarSystemModule(() -> observer, () -> s, () -> false);
        module.withUranus(() -> u);
        module.uranusShowing(true);
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
     * controller: centred on Uranus at the field given, then zoomed in
     * {@code closerSteps} times into the closer fields the controller
     * offers with Uranus on the page.
     */
    private static ChartComponent page(String name, String title, Observer observer,
                                       UranusObservation v, double field, int closerSteps,
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
                    SolarSystemModule.VENUS, SolarSystemModule.MARS, SolarSystemModule.URANUS));
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
     * k = 99.92 % would darken none of them either.
     */
    private static void full(ChartComponent chart, UranusObservation v) throws Exception {
        BufferedImage image = paint(chart);
        ReferenceInk.BodyPlacement drawn = chart.renderedBodies().stream()
                .filter(b -> SolarSystemModule.URANUS.equals(b.identity())).findFirst().orElseThrow();
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
