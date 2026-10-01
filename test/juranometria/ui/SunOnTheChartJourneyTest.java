package juranometria.ui;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.SunChartSession;
import juranometria.ui.solar.SunChartStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sun on the chart, through the component (Sprint 37, issue #415):
 * switched on, it is drawn at true scale where the service puts it,
 * named, spoken, dimmed below a drawn horizon; a click on its disc is
 * the empty sky there and never a star behind it; switched off, the
 * page is the released page; the switch is remembered and restored
 * into the menu item together with the module.
 */
class SunOnTheChartJourneyTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final Instant EQUINOX = Instant.parse("2026-03-20T14:45:53Z");
    private static final Observer OSLO = new Observer(59.913, 10.752, EQUINOX);
    private static final SolarSystemService SERVICE = SolarSystemService.load();

    private ChartComponent chart;
    private SelectionModel selection;
    private ChartModuleHost host;
    private MeridianModule meridian;
    private SolarSystemModule module;
    private Preferences node;

    private void build(Observer observer, boolean horizonDrawn) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart.setSize(900, 700);
            chart.setViewState(new ChartViewState(new SkyPosition(0.0, 0.0), 24.0,
                    8.0, null, null));
            selection = new SelectionModel();
            SelectInteraction.install(chart, selection,
                    new juranometria.chart.WorkingSelection(),
                    new juranometria.chart.SelectionMode());
            host = new ChartModuleHost(chart, selection, request -> { });
            meridian = host.attach(new MeridianModule(observer));
            meridian.showing(horizonDrawn, horizonDrawn, false);
            module = SunChartSession.begin(host,
                    () -> meridian.attached() ? meridian.observer() : null,
                    () -> SERVICE, meridian::horizonShowing);
        });
        flush();
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private void paint() throws Exception {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (host != null) {
            SwingUtilities.invokeAndWait(host::detachAll);
        }
        if (node != null) {
            node.removeNode();
        }
    }

    @Test
    void offByDefaultThePageIsTheReleasedPage() throws Exception {
        build(OSLO, false);
        paint();
        assertEquals(List.of(), chart.renderedBodies());
        assertFalse(chart.getAccessibleContext().getAccessibleDescription().contains("Sun"));
    }

    @Test
    void switchedOnTheSunIsDrawnWhereTheServicePutsItAndSpoken() throws Exception {
        build(OSLO, false);
        SwingUtilities.invokeAndWait(() -> module.sunShowing(true));
        paint();
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        assertEquals(1, drawn.size());
        SunObservation sun = (SunObservation) SERVICE.observe(Body.SUN, OSLO);
        DrawnPage page = DrawnPage.of(chart.currentScene());
        PixelPoint expected = new ViewportMapping(page).toPixel(
                page.projection().project(sun.astrometricJ2000()).orElseThrow());
        assertEquals(expected.x(), drawn.get(0).centre().x(), 1e-6);
        assertEquals(expected.y(), drawn.get(0).centre().y(), 1e-6);
        assertEquals("Sun", drawn.get(0).name());
        assertFalse(drawn.get(0).belowHorizon(), "the horizon is not drawn");
        assertTrue(chart.getAccessibleContext().getAccessibleDescription().endsWith(" Sun."),
                chart.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void aClickOnTheDiscIsTheEmptySkyThereAndHidesWhatIsBehindIt() throws Exception {
        build(OSLO, false);
        SwingUtilities.invokeAndWait(() -> module.sunShowing(true));
        paint();
        ReferenceInk.BodyPlacement sun = chart.renderedBodies().get(0);
        int cx = (int) Math.round(sun.centre().x());
        int cy = (int) Math.round(sun.centre().y());
        // What the catalogue would have answered there, without the disc.
        ChartHitTest bare = new ChartHitTest(new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH));
        ChartHitTest.Hit under = bare.at(chart.currentScene(), chart.chartOptions(), cx, cy);
        click(cx, cy + chart.pageOffsetY());
        assertInstanceOf(Selection.EmptySky.class, selection.selection(),
                "the disc consumed the click: " + selection.selection()
                        + " (the bare page would have said " + under + ")");
        assertEquals(List.of(), selection.candidates(), "nothing behind it is offered");
        SkyPosition said = ((Selection.EmptySky) selection.selection()).position();
        assertTrue(said.separationDegrees(sun(OSLO).astrometricJ2000()) < 0.05,
                "the place the reader pointed at is the Sun's");
        // Beside the disc, the page answers as before.
        SwingUtilities.invokeAndWait(selection::clear);
        click(cx + 60, cy + chart.pageOffsetY());
        assertFalse(selection.selection() instanceof Selection.None);
    }

    @Test
    void belowADrawnHorizonItIsDimmedAndSaysSo() throws Exception {
        Observer night = OSLO.at(Instant.parse("2026-03-20T21:33:00Z"));
        build(night, true);
        SwingUtilities.invokeAndWait(() -> {
            module.sunShowing(true);
            chart.setViewState(new ChartViewState(sun(night).astrometricJ2000(), 24.0,
                    8.0, null, null));
        });
        flush();
        paint();
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        assertEquals(1, drawn.size());
        assertTrue(drawn.get(0).belowHorizon());
        assertEquals("Sun (below the horizon)", drawn.get(0).name());
        SwingUtilities.invokeAndWait(() -> meridian.showing(false, false, false));
        flush();
        paint();
        assertFalse(chart.renderedBodies().get(0).belowHorizon(),
                "with the horizon hidden, drawn normally");
        assertEquals("Sun", chart.renderedBodies().get(0).name());
    }

    @Test
    void theSwitchIsRememberedAndRestoredWithItsMenuItem() throws Exception {
        build(OSLO, false);
        node = Preferences.userRoot().node("juranometria-sun-chart-" + System.nanoTime());
        SunChartStore store = SunChartStore.forNode(node);
        assertFalse(store.shownOrDefault(), "hidden until chosen");
        Runnable toggle = SunChartSession.toggle(module, store);
        SwingUtilities.invokeAndWait(toggle);
        assertTrue(module.sunShowing());
        assertEquals(java.util.Optional.of(Boolean.TRUE), store.shown());
        SwingUtilities.invokeAndWait(() -> module.sunShowing(false));
        JMenuBar[] bar = new JMenuBar[1];
        SwingUtilities.invokeAndWait(() -> bar[0] = AppMenuBar.create(null, null,
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, () -> { }, () -> { }, toggle,
                InterfaceText.forLanguage("en")));
        javax.swing.JCheckBoxMenuItem item = AppMenuBar.sunChartItem(bar[0]);
        assertTrue(item != null, "View carries the switch");
        SwingUtilities.invokeAndWait(() -> SunChartSession.restore(module, store, item));
        assertTrue(module.sunShowing() && item.isSelected(),
                "the module and its tick agree with the store");
        assertEquals("Sun on the chart", item.getText());
    }

    private static SunObservation sun(Observer at) {
        return (SunObservation) SERVICE.observe(Body.SUN, at);
    }

    private void click(int x, int y) throws Exception {
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED,
                MouseEvent.MOUSE_CLICKED}) {
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(new MouseEvent(chart, id,
                    System.nanoTime() / 1_000_000, 0, x, y, 1, false, MouseEvent.BUTTON1)));
        }
        flush();
    }
}
