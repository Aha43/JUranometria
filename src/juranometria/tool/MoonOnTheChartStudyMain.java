package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PageBasis;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.CompassPoint;
import juranometria.solar.SolarSystemService.LimbConditioning;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.SolarTable;
import juranometria.ui.solar.SolarTableWords;

/**
 * The Moon on the chart, as the production composition draws it
 * (Sprint 37, issue #416): the module attached to the real component,
 * production ink only, no study overlay. The June 2026 lunation's four
 * phases, the year's nearest perigee and farthest apogee, Oslo and
 * Cape Town, both palettes and both languages, the Moon below a drawn
 * horizon, the new Moon beside the Sun - and the lunation drawn as a
 * row of phases, each disc measured from the rendered pixels and set
 * against the Moon table's own words. Written to
 * {@code docs/studies/moon-on-the-chart/} by
 * {@code make moon-on-the-chart-study}: the report to stdout, the pages
 * beside it.
 */
public final class MoonOnTheChartStudyMain {

    private static final File DIR = new File("docs/studies/moon-on-the-chart");

    /** The two places the Moon table's study states (MoonTableStudyMain). */
    private static final double OSLO_LAT = 59.913;
    private static final double OSLO_LON = 10.752;
    private static final double CAPE_TOWN_LAT = -33.93;
    private static final double CAPE_TOWN_LON = 18.42;

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));

    private static final StringBuilder REPORT = new StringBuilder();

    private MoonOnTheChartStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        SolarSystemService service = SolarSystemService.load();
        Map<String, MoonEventsFixture.Event> events = MoonEventsFixture.read();
        p("# The Moon on the chart");
        p("");
        p("Sprint 37, issue #416. The production composition - `ChartComponent`"
                + " over the bundled catalogue, with the Solar System module"
                + " attached and the Moon switched on - under the #414 ruling:"
                + " a true-scale phased disc in the page's own ink, its dark"
                + " side inked, its lit side turned to the bright limb χ"
                + " through the page's own north and east at the Moon, the"
                + " terminator a half-ellipse of axis ratio |cos i|; its name"
                + " in an adjacent box or not at all; dimmed below a drawn"
                + " horizon. The instants are the named events of"
                + " `docs/studies/solar-system/moon-events-2026.txt`; positions"
                + " and phases are the ephemeris's for the instant and observer"
                + " stated. Regenerate with `make moon-on-the-chart-study`.");
        p("");
        p("## The June 2026 lunation, Oslo, 3° pages");
        p("");
        for (String[] phase : new String[][] {
                {"new-moon-june", "june-new-3", "The new Moon"},
                {"first-quarter-june", "june-first-quarter-3", "First quarter"},
                {"full-moon-june", "june-full-3", "The full Moon"},
                {"last-quarter-july", "june-last-quarter-3", "Last quarter"}}) {
            Observer oslo = new Observer(OSLO_LAT, OSLO_LON,
                    events.get(phase[0]).instant());
            MoonObservation moon = moon(service, oslo);
            page(phase[1], phase[2] + ", " + events.get(phase[0]).instant(),
                    centredOn(moon, 3.0), ENGLISH, ChartPalette.WHITE_PAPER, oslo,
                    service, false, false, false, tableSays(moon, "en"));
        }
        MoonEventsFixture.Event quarter = events.get("first-quarter-june");
        Observer capeTown = new Observer(CAPE_TOWN_LAT, CAPE_TOWN_LON, quarter.instant());
        MoonObservation fromCape = moon(service, capeTown);
        page("june-first-quarter-cape-town-3", "First quarter from Cape Town (33.93° S)",
                centredOn(fromCape, 3.0), ENGLISH, ChartPalette.WHITE_PAPER, capeTown,
                service, false, false, false,
                "The chart is the sky's, north up and east left wherever the"
                        + " reader stands: the lit side is where χ puts it, and a"
                        + " southern reader who turns to face the Moon sees the"
                        + " same disc turned over. " + tableSays(fromCape, "en"));
        page("june-first-quarter-3-dark", "First quarter on the black sky",
                centredOn(moon(service, new Observer(OSLO_LAT, OSLO_LON,
                        quarter.instant())), 3.0),
                ENGLISH, ChartPalette.BLACK_SKY,
                new Observer(OSLO_LAT, OSLO_LON, quarter.instant()), service,
                false, false, false,
                "The lit side in the black sky's star ink, the dark side a"
                        + " little above the sky's ground.");
        page("june-first-quarter-8-nb", "Første kvarter, 8°, the page's words in Norwegian",
                centredOn(moon(service, new Observer(OSLO_LAT, OSLO_LON,
                        quarter.instant())), 8.0),
                NORSK, ChartPalette.WHITE_PAPER,
                new Observer(OSLO_LAT, OSLO_LON, quarter.instant()), service,
                false, true, false, tableSays(moon(service, new Observer(OSLO_LAT,
                        OSLO_LON, quarter.instant())), "nb-NO"));
        p("## The year's nearest perigee and farthest apogee, Oslo, 3° pages");
        p("");
        for (String[] extreme : new String[][] {
                {"perigee-nearest", "perigee-nearest-3", "The year's nearest perigee"},
                {"apogee-farthest", "apogee-farthest-3", "The year's farthest apogee"}}) {
            MoonEventsFixture.Event event = events.get(extreme[0]);
            Observer oslo = new Observer(OSLO_LAT, OSLO_LON, event.instant());
            MoonObservation moon = moon(service, oslo);
            page(extreme[1], extreme[2] + ", " + event.instant(), centredOn(moon, 3.0),
                    ENGLISH, ChartPalette.WHITE_PAPER, oslo, service, false, false,
                    false, String.format(Locale.ROOT, "Seen from Oslo the Moon is"
                            + " %.0f km away and %.1f″ across; the disc is drawn at"
                            + " that size.", moon.distanceKm(),
                            moon.angularDiameterArcseconds()));
        }
        p("## The new Moon beside the Sun, and the Moon below the horizon");
        p("");
        MoonEventsFixture.Event newMoon = events.get("new-moon-june");
        Observer atNew = new Observer(OSLO_LAT, OSLO_LON, newMoon.instant());
        MoonObservation dark = moon(service, atNew);
        page("june-new-with-sun-12", "The new Moon of June 2026 with the Sun, 12°",
                centredOn(dark, 12.0), ENGLISH, ChartPalette.WHITE_PAPER, atNew, service,
                false, true, true,
                String.format(Locale.ROOT, "The Moon %.1f° from the Sun, %.1f %% lit:"
                        + " a disc of dark side beside the Sun's ring. Where the two"
                        + " overlap, the nearer covers the farther.",
                        dark.elongationDegrees(), 100.0 * dark.illuminatedFraction()));
        MoonEventsFixture.Event last = events.get("last-quarter-july");
        Observer evening = new Observer(OSLO_LAT, OSLO_LON, last.instant());
        MoonObservation low = moon(service, evening);
        SkyPosition nearest = nearestHorizonPoint(evening, low.astrometricJ2000());
        page("july-last-quarter-horizon-36", "Last quarter at Oslo, the horizon drawn, 36°",
                new ChartViewState(halfway(nearest, low.astrometricJ2000()), 36.0, 8.0,
                        null, null), ENGLISH,
                ChartPalette.WHITE_PAPER, evening, service, true, false, false,
                String.format(Locale.ROOT, "The Moon at %.1f° of altitude, the page"
                        + " centred halfway between it and the drawn horizon nearest it:"
                        + " %s", low.horizontal().altitudeDegrees(),
                        low.horizontal().altitudeDegrees() < 0.0
                                ? "dimmed towards the ground, its name carrying the status."
                                : "above the ground, drawn normally."));
        lunation(service, newMoon.instant());
        System.out.print(REPORT);
        System.out.flush();
    }

    private static MoonObservation moon(SolarSystemService service, Observer at) {
        return (MoonObservation) service.observe(Body.MOON, at);
    }

    private static ChartViewState centredOn(MoonObservation moon, double field) {
        return new ChartViewState(moon.astrometricJ2000(), field, 8.0, null, null);
    }

    /** What the Moon table says about this Moon, in its own words. */
    private static String tableSays(MoonObservation moon, String language) {
        SolarTableWords words = new SolarTableWords(InterfaceText.forLanguage(language),
                SolarTable.moon().stem());
        SolarTable table = SolarTable.moon();
        return "The Moon table: " + table.cell(moon, 7, words) + " lit, "
                + table.cell(moon, 8, words) + "; lit side " + table.cell(moon, 10, words)
                + "; diameter " + table.cell(moon, 6, words) + ".";
    }

    /** The midpoint of the great-circle arc between two positions. */
    private static SkyPosition halfway(SkyPosition a, SkyPosition b) {
        double[] u = unit(a);
        double[] v = unit(b);
        double x = u[0] + v[0];
        double y = u[1] + v[1];
        double z = u[2] + v[2];
        double ra = Math.toDegrees(Math.atan2(y, x));
        return new SkyPosition(ra < 0.0 ? ra + 360.0 : ra,
                Math.toDegrees(Math.atan2(z, Math.hypot(x, y))));
    }

    private static double[] unit(SkyPosition p) {
        double ra = Math.toRadians(p.raDegrees());
        double dec = Math.toRadians(p.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra),
                Math.sin(dec)};
    }

    private static SkyPosition nearestHorizonPoint(Observer observer, SkyPosition to) {
        SkyPosition nearest = null;
        double least = Double.MAX_VALUE;
        for (SkyPosition on : new LocalSky(observer).horizon().around(720)) {
            double sep = on.separationDegrees(to);
            if (sep < least) {
                least = sep;
                nearest = on;
            }
        }
        return nearest;
    }

    /** A page drawn by the production component with the module attached. */
    private static ChartComponent compose(ChartViewState state, PageText words,
                                          ChartPalette palette, Observer observer,
                                          SolarSystemService service, boolean withHorizon,
                                          boolean withEcliptic, boolean withSun)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler(), words);
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(ChartOptions.DEFAULTS.withPalette(palette));
            holder[0].setViewState(state);
            MeridianModule meridian = new MeridianModule(observer);
            meridian.showing(withHorizon, withHorizon, false);
            if (withHorizon) {
                holder[0].overlays().offer(MeridianModule.ID,
                        meridian::contributedGeometry);
            }
            if (withEcliptic) {
                EclipticModule ecliptic = new EclipticModule();
                ecliptic.showing(true);
                holder[0].overlays().offer(EclipticModule.ID,
                        ecliptic::contributedGeometry);
            }
            SolarSystemModule solar = new SolarSystemModule(() -> observer,
                    () -> service, meridian::horizonShowing);
            solar.moonShowing(true);
            solar.sunShowing(withSun);
            holder[0].overlays().offer(SolarSystemModule.ID, solar::contributedGeometry);
        });
        SwingUtilities.invokeAndWait(() -> { });
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

    private static void page(String name, String title, ChartViewState state,
                             PageText words, ChartPalette palette, Observer observer,
                             SolarSystemService service, boolean withHorizon,
                             boolean withEcliptic, boolean withSun, String caption)
            throws Exception {
        ChartComponent chart = compose(state, words, palette, observer, service,
                withHorizon, withEcliptic, withSun);
        BufferedImage image = paint(chart);
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        StringBuilder note = new StringBuilder();
        for (ReferenceInk.BodyPlacement body : chart.renderedBodies()) {
            note.append(String.format(Locale.ROOT, "%s at (%.1f, %.1f), disc %.1f px,"
                    + " name %s; ", body.name(), body.centre().x(), body.centre().y(),
                    body.disc().getBounds2D().getWidth(),
                    body.box() == null ? "refused" : "placed"));
        }
        p("### " + title);
        p("");
        p("![](" + name + ".png)");
        p("");
        p(caption + " Drawn: " + (note.length() == 0 ? "nothing" : note.toString().strip()));
        p("");
    }

    /**
     * The lunation as a row of phases: the Moon at the new Moon's
     * instant and every day after it, each drawn on its own 6° page by
     * the production component, the disc cut out of the rendered page;
     * then each disc's lit share and the direction it faces measured
     * from those pixels and set beside the table's own words.
     */
    private static void lunation(SolarSystemService service, Instant start) throws Exception {
        int days = 30;
        int tile = 96;
        int perRow = 10;
        BufferedImage strip = new BufferedImage(perRow * tile,
                ((days + perRow - 1) / perRow) * tile, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = strip.createGraphics();
        sg.setColor(ChartPalette.WHITE_PAPER.ground());
        sg.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        SolarTableWords words = new SolarTableWords(InterfaceText.forLanguage("en"),
                SolarTable.moon().stem());
        SolarTable table = SolarTable.moon();
        p("## The June 2026 lunation as a row of phases");
        p("");
        p("From the fixture's new Moon, " + start + ", every 24 hours, Oslo: each disc"
                + " cut from its own 6° page drawn by the production component (left"
                + " to right, ten to a row). Beside the Moon table's own cells, what"
                + " the rendered pixels of the same Moon on its own 1° page show - the"
                + " disc some 480 px across, every pixel classed as lit, dark side or"
                + " the limb's own ink, which is left out: the share of the disc drawn lit, and the"
                + " direction the lit side faces - the lit pixels' centroid for a"
                + " crescent, the opposite of the dark pixels' for a gibbous disc -"
                + " read back through the page's north and east at the Moon.");
        p("");
        p("![](lunation-june-2026.png)");
        p("");
        p("| day | instant (UTC) | table: lit | table: phase | table: lit side |"
                + " drawn: lit | drawn: facing | agrees |");
        p("|---:|---|---:|---|---|---:|---|---|");
        int agreeing = 0;
        int stated = 0;
        for (int day = 0; day < days; day++) {
            Instant when = start.plus(Duration.ofDays(day));
            Observer oslo = new Observer(OSLO_LAT, OSLO_LON, when);
            MoonObservation moon = moon(service, oslo);
            ChartComponent tileChart = compose(centredOn(moon, 6.0), ENGLISH,
                    ChartPalette.WHITE_PAPER, oslo, service, false, false, false);
            BufferedImage tileImage = paint(tileChart);
            ReferenceInk.BodyPlacement tiled = tileChart.renderedBodies().get(0);
            int x0 = (int) Math.round(tiled.centre().x() + tileChart.pageOffsetX()) - tile / 2;
            int y0 = (int) Math.round(tiled.centre().y() + tileChart.pageOffsetY()) - tile / 2;
            sg.drawImage(tileImage.getSubimage(x0, y0, tile, tile),
                    (day % perRow) * tile, (day / perRow) * tile, null);
            // Measured on its own 1° page, where the disc is some 480 px
            // across: every pixel inside the limb's outer edge, classed by
            // its ink - lit (the paper), dark side, or the limb's own star
            // ink, which says nothing about the phase and is left out.
            ChartComponent chart = compose(centredOn(moon, 1.0), ENGLISH,
                    ChartPalette.WHITE_PAPER, oslo, service, false, false, false);
            BufferedImage image = paint(chart);
            ReferenceInk.BodyPlacement drawn = chart.renderedBodies().get(0);
            double cx = drawn.centre().x() + chart.pageOffsetX();
            double cy = drawn.centre().y() + chart.pageOffsetY();
            double r = drawn.disc().getBounds2D().getWidth() / 2.0;
            double lit = 0.0;
            double all = 0.0;
            double[] litSum = {0.0, 0.0};
            double[] darkSum = {0.0, 0.0};
            int darkTone = (int) Math.round(255 * 0.3);
            for (int y = (int) Math.floor(cy - r); y <= Math.ceil(cy + r); y++) {
                for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
                    double dx = x + 0.5 - cx;
                    double dy = y + 0.5 - cy;
                    if (Math.hypot(dx, dy) > r || x < 0 || y < 0
                            || x >= image.getWidth() || y >= image.getHeight()) {
                        continue;
                    }
                    int red = new Color(image.getRGB(x, y)).getRed();
                    if (red < darkTone / 2) {
                        continue; // the limb's star ink
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
            double share = lit / all;
            String facing = "-";
            String agrees = "-";
            if (moon.brightLimbConditioning() == LimbConditioning.WELL_DEFINED) {
                PageBasis basis = PageBasis.at(DrawnPage.of(chart.currentScene()),
                        moon.astrometricJ2000()).orElseThrow();
                double angle = share < 0.5
                        ? basis.positionAngleDegrees(litSum[0], litSum[1])
                        : basis.positionAngleDegrees(-darkSum[0], -darkSum[1]);
                CompassPoint point = CompassPoint.of(angle);
                facing = String.format(Locale.ROOT, "%.0f° %s", angle,
                        SolarTable.Moon.compassWord(point, words));
                boolean same = point == moon.brightLimbCompassPoint()
                        && Math.abs(share - moon.illuminatedFraction()) < 0.02;
                agrees = same ? "yes" : "NO";
                stated++;
                if (same) {
                    agreeing++;
                }
            } else {
                boolean same = Math.abs(share - moon.illuminatedFraction()) < 0.02;
                agrees = same ? "yes (share)" : "NO";
            }
            p(String.format(Locale.ROOT, "| %d | %s | %s | %s | %s | %.1f %% | %s | %s |",
                    day, when, table.cell(moon, 7, words), table.cell(moon, 8, words),
                    table.cell(moon, 10, words), 100.0 * share, facing, agrees));
        }
        sg.dispose();
        ImageIO.write(strip, "png", new File(DIR, "lunation-june-2026.png"));
        p("");
        p(String.format(Locale.ROOT, "Where the table states a lit side, the drawn"
                + " disc faces the table's compass point and shows its lit share"
                + " within 2 percentage points on %d of %d days.", agreeing, stated));
        p("");
    }
}
