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
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.VenusService.LimbConditioning;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.SolarTable;
import juranometria.ui.solar.SolarTableWords;

/**
 * Venus on the chart, as the production composition draws it (Sprint
 * 48, issue #514, under the rulings on #512): the module attached to
 * the real component, production ink only, no study overlay. The cited
 * instants - Espenak's rows in {@code docs/studies/venus/venus-events.txt}
 * - at the fields the cartography study measured, both palettes and
 * both languages; the 2012 transit over the Sun's ring; and the 2026-27
 * apparition drawn as a row of phases, each disc cut from its own
 * production page and measured from the rendered pixels against the
 * Venus table's own words. Written to
 * {@code docs/studies/venus-on-the-chart/} by
 * {@code make venus-on-the-chart-study}: the report to stdout, the
 * pages beside it.
 */
public final class VenusOnTheChartStudyMain {

    private static final File DIR = new File("docs/studies/venus-on-the-chart");
    private static final Path EVENTS = Path.of("docs/studies/venus/venus-events.txt");
    /** The contract's observer (#500): Oslo at sea level. */
    private static final double OSLO_LAT = 59.91;
    private static final double OSLO_LON = 10.75;

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));
    private static final StringBuilder REPORT = new StringBuilder();

    private VenusOnTheChartStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        SolarSystemService service = SolarSystemService.load();
        Map<String, Instant> events = events();
        p("# Venus on the chart");
        p("");
        p("Sprint 48, issue #514. The production composition - `ChartComponent` over"
                + " the bundled catalogue, with the Solar System module attached and"
                + " Venus switched on - under the rulings on #512: rule M, the larger of"
                + " the true solid-body disc and a 6 px mark, phased either way by the"
                + " page's own lit region (χ of date turned through the page's north and"
                + " east at Venus); while the mark stands in, a cartographic symbol"
                + " beneath the stars, spoken as one; no sliver invented near"
                + " conjunction; the transit and the occultation by painting order; the"
                + " geometric state spoken when Venus meets the Sun's disc. The instants"
                + " are Espenak's rows in `docs/studies/venus/venus-events.txt`;"
                + " positions and phases are the service's for the instant and observer"
                + " stated (Oslo, sea level). Regenerate with `make"
                + " venus-on-the-chart-study`.");
        p("");
        Instant elongation = events.get("2026-greatest-elongation-east");
        Observer oslo = new Observer(OSLO_LAT, OSLO_LON, elongation);
        VenusObservation v = venus(service, oslo);
        p("## The 2026 greatest eastern elongation, Oslo");
        p("");
        page("elongation-8-nb", "Største østlige elongasjon, 8°, the page's words in Norwegian",
                centredOn(v, 8.0), NORSK, ChartPalette.WHITE_PAPER, oslo, service, false,
                tableSays(v, "nb-NO") + " At 8° the true disc is under a pixel: the 6 px"
                        + " mark stands in, phased, and the page calls it a symbol.");
        page("elongation-3", "The elongation at 3°", centredOn(v, 3.0), ENGLISH,
                ChartPalette.WHITE_PAPER, oslo, service, false,
                "The true disc is about 2 px at 3°: the mark still stands in.");
        page("elongation-3-dark", "The elongation at 3° on the black sky", centredOn(v, 3.0),
                ENGLISH, ChartPalette.BLACK_SKY, oslo, service, false,
                "The lit side in the black sky's star ink, the dark side a little above the"
                        + " sky's ground.");
        page("elongation-1", "The elongation at the minimum field, 1°", centredOn(v, 1.0),
                ENGLISH, ChartPalette.WHITE_PAPER, oslo, service, false,
                tableSays(v, "en") + " About 6.1 px at 1°: the true disc, just over the"
                        + " mark, drawn as itself and opaque.");
        p("## The conjunctions, Oslo, 1°, with the Sun");
        p("");
        Instant transit = events.get("2012-inferior-conjunction");
        Observer atTransit = new Observer(OSLO_LAT, OSLO_LON, transit);
        VenusObservation inFront = venus(service, atTransit);
        page("transit-2012-1", "The 2012 transit, " + transit, centredOn(inFront, 1.0),
                ENGLISH, ChartPalette.WHITE_PAPER, atTransit, service, true,
                "In front of the Sun: the nearer body painted last, Venus's dark disc"
                        + " inside the Sun's ring, and the page says so beside the name.");
        page("transit-2012-6", "The 2012 transit at 6°", centredOn(inFront, 6.0), ENGLISH,
                ChartPalette.WHITE_PAPER, atTransit, service, true,
                "The true disc is under 6 px here, so the phased mark stands in - painted in"
                        + " the bodies layer over the Sun's ring, because a symbol in front of"
                        + " an opaque disc is that layer's to paint (the #514 checkpoint); its"
                        + " name takes the first clean box beside the mark, clear of the Sun's"
                        + " ink, or is refused where none is (C4).");
        page("transit-2012-8", "The 2012 transit at 8°, in Norwegian", centredOn(inFront, 8.0),
                NORSK, ChartPalette.BLACK_SKY, atTransit, service, true,
                "The mark over the ring, and the name beside it where a box is clean.");
        Instant inferior = events.get("2026-inferior-conjunction");
        Observer atInferior = new Observer(OSLO_LAT, OSLO_LON, inferior);
        VenusObservation thin = venus(service, atInferior);
        page("inferior-conjunction-2026-1", "The 2026 inferior conjunction, " + inferior,
                centredOn(thin, 1.0), NORSK, ChartPalette.WHITE_PAPER, atInferior, service,
                false, tableSays(thin, "nb-NO") + " The largest disc of the apparition,"
                        + " its lit sliver under a tenth of a pixel: drawn dark with its"
                        + " limb, as the geometry gives.");
        Instant superior = events.get("2026-superior-conjunction");
        Observer atSuperior = new Observer(OSLO_LAT, OSLO_LON, superior);
        VenusObservation full = venus(service, atSuperior);
        page("superior-conjunction-2026-3", "The 2026 superior conjunction at 3°, with the Sun",
                centredOn(full, 3.0), ENGLISH, ChartPalette.WHITE_PAPER, atSuperior, service,
                true, String.format(Locale.ROOT, "Venus %.2f° from the Sun at the published"
                        + " hour, near full: the mark beside the Sun's ring.",
                        full.elongationDegrees()));
        apparition(service, superior, inferior);
        System.out.print(REPORT);
        System.out.flush();
    }

    private static Map<String, Instant> events() throws IOException {
        Map<String, Instant> out = new LinkedHashMap<>();
        for (String line : Files.readAllLines(EVENTS, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t");
            String iso = f[1].length() == 17 ? f[1].substring(0, 16) + ":00Z" : f[1];
            out.put(f[0], Instant.parse(iso));
        }
        return out;
    }

    private static VenusObservation venus(SolarSystemService service, Observer at) {
        return (VenusObservation) service.observe(Body.VENUS, at);
    }

    private static ChartViewState centredOn(VenusObservation v, double field) {
        return new ChartViewState(v.astrometricJ2000(), field, 8.0, null, null);
    }

    /** What the Venus table says of this instant, in the language stated. */
    private static String tableSays(VenusObservation v, String language) {
        SolarTableWords words = new SolarTableWords(InterfaceText.forLanguage(language),
                SolarTable.venus().stem());
        return String.format(Locale.ROOT, "The table: %.1f %% lit, %s, χ %.1f° (%s).",
                100.0 * v.illuminatedFraction(), phaseWord(v, words),
                v.brightLimbAngleDegrees(),
                v.brightLimbConditioning() == LimbConditioning.WELL_DEFINED
                        ? words.say("litside.pattern", v.brightLimbCompassPoint().name(),
                                String.format(Locale.ROOT, "%.0f°", v.brightLimbAngleDegrees()))
                        : "not well-defined");
    }

    private static String phaseWord(VenusObservation v, SolarTableWords words) {
        return words.say("phase." + switch (v.phase()) {
            case IN_FRONT_OF_SUN -> "inFrontOfSun";
            case BEHIND_SUN -> "behindSun";
            case NEAR_INFERIOR_CONJUNCTION -> "nearInferiorConjunction";
            case CRESCENT -> "crescent";
            case NEAR_HALF_LIT -> "nearHalfLit";
            case GIBBOUS -> "gibbous";
            case NEAR_FULL -> "nearFull";
        });
    }

    private static ChartComponent compose(ChartViewState state, PageText words,
                                          ChartPalette palette, Observer observer,
                                          SolarSystemService service, boolean withSun)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            holder[0].setViewState(state);
            SolarSystemModule module = new SolarSystemModule(() -> observer, () -> service,
                    () -> false);
            module.venusShowing(true);
            module.sunShowing(withSun);
            holder[0].overlays().offer(SolarSystemModule.ID, module::contributedGeometry);
        });
        SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
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

    private static void page(String name, String title, ChartViewState state, PageText words,
                             ChartPalette palette, Observer observer,
                             SolarSystemService service, boolean withSun, String caption)
            throws Exception {
        ChartComponent chart = compose(state, words, palette, observer, service, withSun);
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
        p("### " + title);
        p("");
        p("![](" + name + ".png)");
        p("");
        p(caption + " Drawn: " + (note.length() == 0 ? "nothing" : note.toString().strip())
                + " Spoken: " + chart.getAccessibleContext().getAccessibleDescription()
                .substring(chart.getAccessibleContext().getAccessibleDescription()
                        .lastIndexOf(" Venus")).strip());
        p("");
    }

    /**
     * The 2026-27 apparition as a row of phases: Venus every 14 days from
     * the superior conjunction through the inferior one, each drawn on
     * its own 1° page by the production component, the mark or disc cut
     * out of the rendered page; then each disc's lit share and facing
     * measured from those pixels and set beside the table's own words.
     * The mark is small at 1° for most of the apparition, so the
     * measurement is of a few dozen pixels; the agreement asked is the
     * phase's half and its facing, not a percentage.
     */
    private static void apparition(SolarSystemService service, Instant start, Instant inferior)
            throws Exception {
        int days = 14;
        int tile = 48;
        int perRow = 12;
        int count = (int) (Duration.between(start, inferior.plus(Duration.ofDays(120)))
                .toDays() / days) + 1;
        BufferedImage strip = new BufferedImage(perRow * tile,
                ((count + perRow - 1) / perRow) * tile, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = strip.createGraphics();
        sg.setColor(ChartPalette.WHITE_PAPER.ground());
        sg.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        SolarTableWords words = new SolarTableWords(InterfaceText.forLanguage("en"),
                SolarTable.venus().stem());
        p("## The 2026-27 apparition as a row of phases");
        p("");
        p("From the 2026 superior conjunction, " + start + ", every " + days + " days through"
                + " the inferior conjunction and 120 days beyond, Oslo: each mark or disc cut"
                + " from its own 1° page drawn by the production component (left to right, "
                + perRow + " to a row). Beside the Venus table's own cells, what the rendered"
                + " pixels show - every pixel inside the mark classed as lit, dark side or"
                + " the limb's own ink, which is left out: the share drawn lit, and the"
                + " direction the lit side faces read back through the page's north and"
                + " east at Venus. A mark of 6 px holds some two dozen pixels, so the share"
                + " is coarse; what is held is that a disc the table calls crescent is drawn"
                + " less than half lit and a gibbous one more where the table's k is at least"
                + " five points from a half, and that the drawn lit side faces within 45° of"
                + " the table's χ wherever the table says χ is well defined.");
        p("");
        p("![](apparition-2026-27.png)");
        p("");
        p("| step | instant (UTC) | table: lit | table: phase | table: χ | drawn | drawn: lit"
                + " | drawn: facing | agrees |");
        p("|---:|---|---:|---|---:|---|---:|---|---|");
        int agreeing = 0;
        int judged = 0;
        for (int step = 0; step < count; step++) {
            Instant when = start.plus(Duration.ofDays((long) step * days));
            Observer oslo = new Observer(OSLO_LAT, OSLO_LON, when);
            VenusObservation v = venus(service, oslo);
            ChartComponent chart = compose(centredOn(v, 1.0), ENGLISH, ChartPalette.WHITE_PAPER,
                    oslo, service, false);
            BufferedImage image = paint(chart);
            ReferenceInk.BodyPlacement drawn = chart.renderedBodies().get(0);
            int x0 = (int) Math.round(drawn.centre().x() + chart.pageOffsetX()) - tile / 2;
            int y0 = (int) Math.round(drawn.centre().y() + chart.pageOffsetY()) - tile / 2;
            sg.drawImage(image.getSubimage(Math.max(0, x0), Math.max(0, y0), tile, tile),
                    (step % perRow) * tile, (step / perRow) * tile, null);
            double cx = drawn.centre().x() + chart.pageOffsetX();
            double cy = drawn.centre().y() + chart.pageOffsetY();
            double r = drawn.disc().getBounds2D().getWidth() / 2.0;
            double lit = 0.0;
            double all = 0.0;
            double[] litSum = {0.0, 0.0};
            double[] darkSum = {0.0, 0.0};
            int darkTone = (int) Math.round(255 * ReferenceInk.darkSide());
            for (int y = (int) Math.floor(cy - r); y <= Math.ceil(cy + r); y++) {
                for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
                    double dx = x + 0.5 - cx;
                    double dy = y + 0.5 - cy;
                    if (Math.hypot(dx, dy) > r - 0.75 || x < 0 || y < 0
                            || x >= image.getWidth() || y >= image.getHeight()) {
                        continue;
                    }
                    int red = new Color(image.getRGB(x, y)).getRed();
                    if (red < darkTone / 2) {
                        continue;
                    }
                    all++;
                    if (red > (darkTone + 255) / 2) {
                        lit++;
                        litSum[0] += dx;
                        litSum[1] += dy;
                    } else {
                        darkSum[0] += dx;
                        darkSum[1] += dy;
                    }
                }
            }
            double share = all == 0 ? 0.0 : lit / all;
            String facing = "-";
            String agrees = "-";
            // A mark of a few dozen pixels cannot tell 47 % from 50 %: the
            // half is held only where the table's k is at least five points
            // from a half; nearer, the facing alone is held.
            boolean halfAgrees = Math.abs(v.illuminatedFraction() - 0.5) < 0.05
                    || (v.illuminatedFraction() < 0.5 ? share < 0.5 : share > 0.5);
            if (v.brightLimbConditioning() == LimbConditioning.WELL_DEFINED && all > 0
                    && (lit > 0 && lit < all)) {
                PageBasis basis = PageBasis.at(DrawnPage.of(chart.currentScene()),
                        v.astrometricJ2000()).orElseThrow();
                double angle = share < 0.5
                        ? basis.positionAngleDegrees(litSum[0], litSum[1])
                        : basis.positionAngleDegrees(-darkSum[0], -darkSum[1]);
                double off = Math.abs(((angle - v.brightLimbAngleDegrees()) % 360.0 + 540.0)
                        % 360.0 - 180.0);
                facing = String.format(Locale.ROOT, "%.0f°", angle);
                judged++;
                boolean ok = halfAgrees && off <= 45.0;
                agreeing += ok ? 1 : 0;
                agrees = ok ? "yes" : "no";
            }
            p(String.format(Locale.ROOT, "| %d | %s | %.1f %% | %s | %.1f° | %s %.1f px | %.0f %% | %s | %s |",
                    step, when.toString().replace("T", " ").replace(":00Z", ""),
                    100.0 * v.illuminatedFraction(), phaseWord(v, words),
                    v.brightLimbAngleDegrees(), drawn.symbol() ? "symbol" : "disc",
                    drawn.disc().getBounds2D().getWidth(), 100.0 * share, facing, agrees));
        }
        sg.dispose();
        ImageIO.write(strip, "png", new File(DIR, "apparition-2026-27.png"));
        p("");
        p(String.format(Locale.ROOT, "**%d of the %d steps where the table states χ agree**:"
                + " the drawn half is the table's and the lit side faces within 45° of χ.",
                agreeing, judged));
        p("");
        if (agreeing != judged) {
            // The report is written whole before the study refuses, so
            // the disagreeing rows can be read.
            System.out.print(REPORT);
            System.out.flush();
            throw new IllegalStateException("the drawn phase disagrees with the table on "
                    + (judged - agreeing) + " steps");
        }
    }
}
