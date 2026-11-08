package juranometria.tool.allsky;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.ecliptic.EclipticModule;
import juranometria.jovianchart.JovianModule;
import juranometria.meridian.MeridianModule;
import juranometria.project.Projections;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.saturnchart.SaturnModule;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.solar.JovianPack;
import juranometria.solar.JovianSystemService;
import juranometria.solar.MarsService;
import juranometria.solar.MarsSystemPack;
import juranometria.solar.NeptuneService;
import juranometria.solar.PlanetsPack;
import juranometria.solar.SaturnPack;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SolarSystemPack;
import juranometria.solar.SolarSystemService;
import juranometria.solar.UranusService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.language.PageText;

/**
 * The production chart composition, as {@code JUranometriaMain} wires
 * it, drawn through the #686 study door (study only).
 *
 * <p>Every astronomical mark on a study page comes from a production
 * module through the production host: the meridian module (horizon,
 * meridian, zenith, subdue), the ecliptic module, the Solar System
 * module with Mars, Uranus and Neptune on their own services, the
 * Jovian module and the Saturn module. The services are loaded once
 * per study run and shared, as the application's lazy suppliers are.
 */
public final class AllSkyComposition {

    /** The services, loaded once per study run. */
    public static final class Services {
        final SolarSystemService solar = SolarSystemService.load();
        final JovianSystemService jovian = new JovianSystemService(
                SolarSystemPack.load(), JovianPack.load());
        final SaturnSystemService saturn = new SaturnSystemService(
                SolarSystemPack.load(), SaturnPack.load());
        final MarsService mars = new MarsService(SolarSystemPack.load(),
                PlanetsPack.load(), MarsSystemPack.load());
        final UranusService uranus = UranusService.load();
        final NeptuneService neptune = NeptuneService.load();

        public SolarSystemService solar() {
            return solar;
        }
    }

    /** What one page is asked to show. */
    public record Ask(Observer observer, ChartPalette palette, PageText words,
                      double limitingMagnitude, boolean horizon, boolean subdue,
                      int widthPx, int heightPx) {
    }

    /** A composed page, its image, and the live component for questions. */
    public record Page(ChartComponent chart, ChartModuleHost host,
                       MeridianModule meridian, BufferedImage image) {
    }

    private AllSkyComposition() {
    }

    /**
     * Composes and paints one page through the study door. Must be
     * called on the event dispatch thread; every question asked of
     * the returned component that derives a projection must also be
     * asked inside {@link Projections#drawnForStudy}.
     */
    public static Page paint(AllSkyProjection projection, Ask ask, Services services)
            throws Exception {
        return Projections.drawnForStudy(projection, () -> compose(projection, ask, services));
    }

    private static Page compose(AllSkyProjection projection, Ask ask, Services services) {
        ChartComponent chart = new ChartComponent(Atlas.assembler(), ask.words());
        chart.setSize(ask.widthPx(), ask.heightPx());
        chart.setChartOptions(ChartOptions.DEFAULTS.withPalette(ask.palette()));
        ChartModuleHost host = new ChartModuleHost(chart, new SelectionModel(), request -> { });
        MeridianModule meridian = host.attach(new MeridianModule(ask.observer()));
        meridian.showing(false, ask.horizon(), false);
        meridian.subduing(ask.subdue());
        chart.setContext(meridian.context());
        chart.setSubdueBelowHorizon(() -> meridian.attached() && meridian.subduing()
                ? new LocalSky(meridian.observer()).zenith() : null);
        EclipticModule ecliptic = host.attach(new EclipticModule());
        ecliptic.showing(true);
        Supplier<Observer> observer = () -> meridian.attached() ? meridian.observer() : null;
        SolarSystemModule solar = host.attach(new SolarSystemModule(observer,
                () -> services.solar, meridian::subduing));
        solar.withMars(() -> services.mars);
        solar.withUranus(() -> services.uranus);
        solar.withNeptune(() -> services.neptune);
        solar.sunShowing(true);
        solar.moonShowing(true);
        solar.mercuryShowing(true);
        solar.venusShowing(true);
        solar.marsShowing(true);
        solar.uranusShowing(true);
        solar.neptuneShowing(true);
        JovianModule jupiter = host.attach(new JovianModule(observer,
                () -> services.jovian, meridian::subduing));
        jupiter.showing(true);
        SaturnModule saturn = host.attach(new SaturnModule(observer,
                () -> services.saturn, meridian::subduing));
        saturn.showing(true);
        chart.setViewState(new ChartViewState(projection.centre(), 180.0,
                ask.limitingMagnitude()));
        BufferedImage image = new BufferedImage(ask.widthPx(), ask.heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            chart.paint(g);
        } finally {
            g.dispose();
        }
        return new Page(chart, host, meridian, image);
    }
}
