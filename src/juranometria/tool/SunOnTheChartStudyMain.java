package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.ecliptic.EclipticModule;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ReferenceInk;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * The Sun on the chart, as the production composition draws it
 * (Sprint 37, issue #415): the module attached to the real component,
 * on the pages the #414 ruling was made over - production ink only,
 * no study overlay. Written to {@code docs/studies/sun-on-the-chart/}
 * by {@code make sun-on-the-chart-study}: the report to stdout, the
 * pages beside it.
 */
public final class SunOnTheChartStudyMain {

    private static final File DIR = new File("docs/studies/sun-on-the-chart");

    /** The gallery's own observer and instant, restated. */
    private static final Instant GALLERY_WHEN = Instant.parse("2026-03-20T21:33:00Z");
    private static final Observer OSLO = new Observer(59.913, 10.752, GALLERY_WHEN);

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final PageText NORSK = PageText.in(InterfaceText.forLanguage("nb-NO"));

    private static final StringBuilder REPORT = new StringBuilder();

    private SunOnTheChartStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        DIR.mkdirs();
        SolarSystemService service = SolarSystemService.load();
        p("# The Sun on the chart");
        p("");
        p("Sprint 37, issue #415. The production composition - `ChartComponent`"
                + " over the bundled catalogue, with the Solar System module"
                + " attached and the Sun switched on - on the pages the #414"
                + " ruling was made over. Production ink only: a true-scale ring"
                + " with its centre dot in the page's own ink, its name in an"
                + " adjacent box or not at all, dimmed below a drawn horizon."
                + " Positions are the ephemeris's for the instant and observer"
                + " stated. Regenerate with `make sun-on-the-chart-study`.");
        p("");
        Instant equinox = SeasonalEventsFixture.read().get("march-equinox").instant();
        Observer atEquinox = OSLO.at(equinox);
        SunObservation sunE = (SunObservation) service.observe(Body.SUN, atEquinox);
        page("equinox-24", "The March equinox page at the IMCCE's equinox instant, Oslo",
                new ChartViewState(new SkyPosition(0.0, 0.0), 24.0, 8.0, null, null),
                ENGLISH, ChartPalette.WHITE_PAPER, atEquinox, service, false, true,
                "The Sun on the ecliptic at RA 0h, "
                        + String.format(Locale.ROOT, "%.1f", sunE.horizontal().altitudeDegrees())
                        + "° above Oslo's horizon; the ecliptic's own equinox"
                        + " landmark sits under it, and the Sun's name yields to it"
                        + " or takes a clear box, never both meanings in one label.");
        page("equinox-24-dark", "The same page on the black sky",
                new ChartViewState(new SkyPosition(0.0, 0.0), 24.0, 8.0, null, null),
                ENGLISH, ChartPalette.BLACK_SKY, atEquinox, service, false, true,
                "The same ring and dot in the black sky's star ink.");
        page("equinox-3-nb", "The Sun at 3°, the page's words in Norwegian",
                new ChartViewState(sunE.astrometricJ2000(), 3.0, 8.0, null, null),
                NORSK, ChartPalette.WHITE_PAPER, atEquinox, service, false, false,
                "The disc at true scale, 160 px across on a 3° page, named"
                        + " *Solen*.");
        // The horizon page nearest the Sun in the evening twilight of
        // the equinox day at Oslo: a chosen instant, the Sun a few
        // degrees below the ground, so the page can show it there.
        Observer dusk = OSLO.at(Instant.parse("2026-03-20T18:30:00Z"));
        SunObservation sunNight = (SunObservation) service.observe(Body.SUN, dusk);
        LocalSky sky = new LocalSky(dusk);
        SkyPosition nearest = null;
        double least = Double.MAX_VALUE;
        for (SkyPosition on : sky.horizon().around(720)) {
            double sep = on.separationDegrees(sunNight.astrometricJ2000());
            if (sep < least) {
                least = sep;
                nearest = on;
            }
        }
        page("horizon-below-36", "The horizon page nearest the Sun, 2026-03-20 18:30 UTC at Oslo, 36° field",
                new ChartViewState(nearest, 36.0, 8.0, null, null),
                ENGLISH, ChartPalette.WHITE_PAPER, dusk, service, true, false,
                "The Sun "
                        + String.format(Locale.ROOT, "%.1f", sunNight.horizontal().altitudeDegrees())
                        + "° below the drawn mathematical horizon: dimmed towards"
                        + " the ground, its name carrying the status.");
        page("horizon-hidden-36", "The same page with the horizon hidden",
                new ChartViewState(nearest, 36.0, 8.0, null, null),
                ENGLISH, ChartPalette.WHITE_PAPER, dusk, service, false, false,
                "With no horizon drawn, the celestial chart draws the Sun"
                        + " normally, as ruled (C4).");
        System.out.print(REPORT);
        System.out.flush();
    }

    private static void page(String name, String title, ChartViewState state,
                             PageText words, ChartPalette palette, Observer observer,
                             SolarSystemService service, boolean withHorizon,
                             boolean withEcliptic, String caption) throws Exception {
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
            solar.sunShowing(true);
            holder[0].overlays().offer(SolarSystemModule.ID,
                    solar::contributedGeometry);
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
        ChartComponent chart = holder[0];
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        ImageIO.write(image, "png", new File(DIR, name + ".png"));
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        StringBuilder note = new StringBuilder();
        for (ReferenceInk.BodyPlacement body : drawn) {
            note.append(String.format(Locale.ROOT, "%s at (%.1f, %.1f), disc %.1f px,"
                    + " name %s; ", body.name(), body.centre().x(), body.centre().y(),
                    body.disc().getBounds2D().getWidth(),
                    body.box() == null ? "refused" : "placed"));
        }
        p("## " + title);
        p("");
        p("![](" + name + ".png)");
        p("");
        p(caption + " Drawn: " + (drawn.isEmpty() ? "nothing" : note.toString().strip()));
        p("");
    }
}
