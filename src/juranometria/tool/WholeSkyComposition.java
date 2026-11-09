package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.jovianchart.JovianModule;
import juranometria.meridian.MeridianModule;
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
import juranometria.ui.ChartViewController;
import juranometria.ui.WholeSkySession;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.onthispage.OnThisPageModule;

/**
 * The whole-sky page as the application composes it (#687), for the
 * study that measures it and the tests that hold it: a production
 * {@link ChartComponent} with the production modules attached as
 * {@code JUranometriaMain} attaches them - the meridian module with
 * its horizon, zenith and Subdue below horizon, the ecliptic, every
 * Solar System module with every body on, On this page - driven by a
 * real {@link ChartViewController} through the real
 * {@link WholeSkySession}, so the route a reader takes is the route
 * measured. Nothing here is a study door: the whole sky is a
 * production kind.
 */
public final class WholeSkyComposition {

    /** The Solar System services, loaded once for a run. */
    public static final class Services {
        private SolarSystemService solar;
        private JovianSystemService jovian;
        private SaturnSystemService saturn;
        private MarsService mars;
        private UranusService uranus;
        private NeptuneService neptune;

        public synchronized SolarSystemService solar() {
            if (solar == null) {
                solar = SolarSystemService.load();
            }
            return solar;
        }

        public synchronized JovianSystemService jovian() {
            if (jovian == null) {
                jovian = new JovianSystemService(SolarSystemPack.load(), JovianPack.load());
            }
            return jovian;
        }

        public synchronized SaturnSystemService saturn() {
            if (saturn == null) {
                saturn = new SaturnSystemService(SolarSystemPack.load(), SaturnPack.load());
            }
            return saturn;
        }

        public synchronized MarsService mars() {
            if (mars == null) {
                mars = new MarsService(SolarSystemPack.load(), PlanetsPack.load(),
                        MarsSystemPack.load());
            }
            return mars;
        }

        public synchronized UranusService uranus() {
            if (uranus == null) {
                uranus = UranusService.load();
            }
            return uranus;
        }

        public synchronized NeptuneService neptune() {
            if (neptune == null) {
                neptune = NeptuneService.load();
            }
            return neptune;
        }
    }

    /**
     * One page to compose.
     *
     * @param observer   the chart's place and time
     * @param palette    the ground
     * @param language   the interface language tag, {@code en} or {@code nb-NO}
     * @param limit      the reader's limiting magnitude before entering
     * @param horizon    whether the meridian module draws the horizon
     * @param subdue     Subdue below horizon
     * @param ordinaryField the ordinary field the reader leaves for the whole sky
     */
    public record Ask(Observer observer, ChartPalette palette, String language,
                      double limit, boolean horizon, boolean subdue,
                      double ordinaryField, int widthPx, int heightPx) {
    }

    /** What was composed, after one paint. */
    public record Page(ChartComponent chart, ChartViewController controller,
                       WholeSkySession session, ChartModuleHost host,
                       MeridianModule meridian, OnThisPageModule onThisPage,
                       PageText words, InterfaceText said, BufferedImage image) {
    }

    private WholeSkyComposition() {
    }

    /** Composes the page and paints it once, on the event thread. */
    public static Page paint(Ask ask, Services services) throws Exception {
        Page[] out = new Page[1];
        SwingUtilities.invokeAndWait(() -> out[0] = compose(ask, services));
        return out[0];
    }

    private static Page compose(Ask ask, Services services) {
        InterfaceText said = InterfaceText.forLanguage(ask.language());
        PageText words = PageText.in(said);
        ChartComponent chart = new ChartComponent(Atlas.assembler(), words);
        chart.setSize(ask.widthPx(), ask.heightPx());
        chart.setChartOptions(ChartOptions.DEFAULTS.withPalette(ask.palette()));
        ChartViewController controller = new ChartViewController();
        controller.onChange(chart::setViewState);
        ChartModuleHost host = new ChartModuleHost(chart, new SelectionModel(),
                request -> controller.recenter(request.centre()));
        OnThisPageModule onThisPage = host.attach(new OnThisPageModule(said));
        MeridianModule meridian = host.attach(new MeridianModule(ask.observer()));
        meridian.showing(true, ask.horizon(), true);
        meridian.subduing(ask.subdue());
        chart.setContext(meridian.context());
        chart.setSubdueBelowHorizon(() -> meridian.attached() && meridian.subduing()
                ? new LocalSky(meridian.observer()).zenith() : null);
        EclipticModule ecliptic = host.attach(new EclipticModule());
        ecliptic.showing(true);
        Supplier<Observer> observer = () -> meridian.attached() ? meridian.observer() : null;
        SolarSystemModule solar = host.attach(new SolarSystemModule(observer,
                services::solar, meridian::subduing));
        solar.withMars(services::mars);
        solar.withUranus(services::uranus);
        solar.withNeptune(services::neptune);
        solar.sunShowing(true);
        solar.moonShowing(true);
        solar.mercuryShowing(true);
        solar.venusShowing(true);
        solar.marsShowing(true);
        solar.uranusShowing(true);
        solar.neptuneShowing(true);
        host.attach(new JovianModule(observer, services::jovian, meridian::subduing))
                .showing(true);
        host.attach(new SaturnModule(observer, services::saturn, meridian::subduing))
                .showing(true);
        // The reader's ordinary page first, then the whole sky through
        // the one switch - the route the application takes.
        controller.recenter(juranometria.chart.ChartViewState.DEFAULT.centre(),
                ask.ordinaryField());
        while (controller.state().limitingMagnitude() < ask.limit()
                && controller.canIncreaseMagnitudeLimit()) {
            controller.increaseMagnitudeLimit();
        }
        while (controller.state().limitingMagnitude() > ask.limit()
                && controller.canDecreaseMagnitudeLimit()) {
            controller.decreaseMagnitudeLimit();
        }
        WholeSkySession session = new WholeSkySession(controller, observer);
        meridian.onChange(session::observerChanged);
        session.enter();
        BufferedImage image = paintNow(chart, ask.widthPx(), ask.heightPx());
        return new Page(chart, controller, session, host, meridian, onThisPage, words,
                said, image);
    }

    /** Paints the chart as it stands; on the event thread already. */
    public static BufferedImage paintNow(ChartComponent chart, int widthPx, int heightPx) {
        BufferedImage image = new BufferedImage(widthPx, heightPx,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            chart.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** Paints the page again, on the event thread, and returns the image. */
    public static BufferedImage repaint(Page page, int widthPx, int heightPx) throws Exception {
        BufferedImage[] out = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> out[0] = paintNow(page.chart(), widthPx, heightPx));
        return out[0];
    }

    /**
     * Open here (R-8): the ordinary chart centred on a position at the
     * field the reader left, through the controller's own recentre.
     */
    public static BufferedImage openHere(Page page, SkyPosition position, int widthPx,
                                         int heightPx) throws Exception {
        BufferedImage[] out = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            page.controller().recenter(position);
            out[0] = paintNow(page.chart(), widthPx, heightPx);
        });
        return out[0];
    }
}
