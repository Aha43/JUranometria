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
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.tool.MoonEventsFixture;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.MoonChartSession;
import juranometria.ui.solar.MoonChartStore;
import juranometria.ui.solar.SunChartSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Moon on the chart, through the component (Sprint 37, issue
 * #416): switched on, it is drawn at true scale where the service puts
 * it, named and spoken; a click on its disc is the empty sky there and
 * never the star it hides; switched off, the page is the released page;
 * its switch is its own, remembered and restored into its menu item.
 */
class MoonOnTheChartJourneyTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final SolarSystemService SERVICE = SolarSystemService.load();

    private ChartComponent chart;
    private SelectionModel selection;
    private ChartModuleHost host;
    private SolarSystemModule module;
    private Preferences node;

    private static Observer atFullMoon() throws Exception {
        Instant full = MoonEventsFixture.read().get("full-moon-june").instant();
        return new Observer(59.913, 10.752, full);
    }

    private void build(Observer observer, double field) throws Exception {
        MoonObservation moon = (MoonObservation) SERVICE.observe(Body.MOON, observer);
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart.setSize(900, 700);
            chart.setViewState(new ChartViewState(moon.astrometricJ2000(), field,
                    8.0, null, null));
            selection = new SelectionModel();
            SelectInteraction.install(chart, selection,
                    new juranometria.chart.WorkingSelection(),
                    new juranometria.chart.SelectionMode());
            host = new ChartModuleHost(chart, selection, request -> { });
            MeridianModule meridian = host.attach(new MeridianModule(observer));
            meridian.showing(false, false, false);
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
    void switchedOnTheMoonIsDrawnWhereTheServicePutsItAndSpoken() throws Exception {
        Observer oslo = atFullMoon();
        build(oslo, 3.0);
        paint();
        assertEquals(List.of(), chart.renderedBodies(), "hidden until switched on");
        SwingUtilities.invokeAndWait(() -> module.moonShowing(true));
        paint();
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        assertEquals(1, drawn.size(), "the Moon's switch draws the Moon alone");
        MoonObservation moon = (MoonObservation) SERVICE.observe(Body.MOON, oslo);
        DrawnPage page = DrawnPage.of(chart.currentScene());
        PixelPoint expected = new ViewportMapping(page).toPixel(
                page.projection().project(moon.astrometricJ2000()).orElseThrow());
        assertEquals(expected.x(), drawn.get(0).centre().x(), 1e-6);
        assertEquals(expected.y(), drawn.get(0).centre().y(), 1e-6);
        assertEquals("Moon", drawn.get(0).name());
        assertTrue(chart.getAccessibleContext().getAccessibleDescription().endsWith(" Moon."),
                chart.getAccessibleContext().getAccessibleDescription());
    }

    @Test
    void aClickOnTheDiscNeverSelectsTheStarItHides() throws Exception {
        // The first of the fixture's June phases whose disc, on a 3°
        // page, covers a star the bare page would have answered.
        int[] hidden = null;
        for (String event : List.of("new-moon-june", "first-quarter-june",
                "full-moon-june", "last-quarter-july")) {
            if (host != null) {
                SwingUtilities.invokeAndWait(host::detachAll);
            }
            Instant when = MoonEventsFixture.read().get(event).instant();
            build(new Observer(59.913, 10.752, when), 3.0);
            SwingUtilities.invokeAndWait(() -> module.moonShowing(true));
            paint();
            hidden = starUnder(chart.renderedBodies().get(0));
            if (hidden != null) {
                break;
            }
        }
        assertNotNull(hidden, "a June 2026 Moon covers a catalogue star on a 3° page");
        click(hidden[0], hidden[1] + chart.pageOffsetY());
        assertInstanceOf(Selection.EmptySky.class, selection.selection(),
                "the disc consumed the click: " + selection.selection());
        assertEquals(List.of(), selection.candidates(), "the hidden star is not offered");
    }

    /** A pixel on the disc where the bare page would have answered a star, or null. */
    private int[] starUnder(ReferenceInk.BodyPlacement moon) {
        ChartHitTest bare = new ChartHitTest(new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH));
        int[] hidden = null;
        double r = moon.disc().getBounds2D().getWidth() / 2.0;
        search:
        for (int dy = (int) -r + 3; dy < r - 3; dy += 2) {
            for (int dx = (int) -r + 3; dx < r - 3; dx += 2) {
                int x = (int) Math.round(moon.centre().x()) + dx;
                int y = (int) Math.round(moon.centre().y()) + dy;
                if (!moon.covers(x, y)) {
                    continue;
                }
                ChartHitTest.Hit under = bare.at(chart.currentScene(), chart.chartOptions(), x, y);
                if (under != null && under.selection() instanceof Selection.Object object
                        && object.kind() == Selection.Object.Kind.STAR) {
                    hidden = new int[] {x, y};
                    break search;
                }
            }
        }
        return hidden;
    }

    @Test
    void theMoonsSwitchIsItsOwnRememberedAndRestoredWithItsMenuItem() throws Exception {
        build(atFullMoon(), 3.0);
        node = Preferences.userRoot().node("juranometria-moon-chart-" + System.nanoTime());
        MoonChartStore store = MoonChartStore.forNode(node);
        assertFalse(store.shownOrDefault(), "hidden until chosen");
        Runnable toggle = MoonChartSession.toggle(module, store);
        SwingUtilities.invokeAndWait(toggle);
        assertTrue(module.moonShowing());
        assertFalse(module.sunShowing(), "the Moon's switch leaves the Sun alone");
        assertEquals(java.util.Optional.of(Boolean.TRUE), store.shown());
        SwingUtilities.invokeAndWait(() -> module.moonShowing(false));
        JMenuBar[] bar = new JMenuBar[1];
        SwingUtilities.invokeAndWait(() -> bar[0] = AppMenuBar.create(null, null,
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, () -> { }, () -> { }, () -> { }, toggle,
                InterfaceText.forLanguage("en")));
        javax.swing.JCheckBoxMenuItem item = AppMenuBar.moonChartItem(bar[0]);
        assertNotNull(item, "View carries the Moon's switch");
        SwingUtilities.invokeAndWait(() -> MoonChartSession.restore(module, store, item));
        assertTrue(module.moonShowing() && item.isSelected(),
                "the module and its tick agree with the store");
        assertEquals("Moon on the chart", item.getText());
        JMenuBar[] norsk = new JMenuBar[1];
        SwingUtilities.invokeAndWait(() -> norsk[0] = AppMenuBar.create(null, null,
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, () -> { }, () -> { }, () -> { }, toggle,
                InterfaceText.forLanguage("nb-NO")));
        assertEquals("Månen på kartet", AppMenuBar.moonChartItem(norsk[0]).getText());
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
