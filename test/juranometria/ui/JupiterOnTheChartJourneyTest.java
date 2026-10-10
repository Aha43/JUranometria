package juranometria.ui;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.jovianchart.JovianModule;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService;
import juranometria.solar.JovianSystemService.JupiterObservation;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.JovianChartSession;
import juranometria.ui.solar.JovianChartStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jupiter on the production chart (issue #484), through the real
 * component, module host and selection: hidden until the switch is on;
 * then drawn where the service puts it and spoken - as a cartographic
 * symbol at ordinary fields, as Jupiter itself at the normal minimum
 * field; a click through the symbol is not consumed, a click on the true
 * disc is; the switch remembered and restored with its View item, in both
 * languages.
 */
class JupiterOnTheChartJourneyTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-12-11T22:45:00Z"));
    private static JovianSystemService service;

    private ChartComponent chart;
    private ChartModuleHost host;
    private JovianModule module;
    private Preferences node;

    @BeforeAll
    static void load() {
        service = JovianSystemService.load();
    }

    private void build(double field) throws Exception {
        JupiterObservation jupiter = service.observeJupiter(OSLO);
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart.setSize(900, 700);
            chart.setViewState(new ChartViewState(jupiter.astrometricJ2000(), field, 8.0,
                    null, null));
            SelectionModel selection = new SelectionModel();
            SelectInteraction.install(chart, selection, new juranometria.chart.WorkingSelection(),
                    new juranometria.chart.SelectionMode());
            host = new ChartModuleHost(chart, selection, request -> { });
            MeridianModule meridian = host.attach(new MeridianModule(OSLO));
            meridian.showing(false, false, false);
            module = host.attach(new JovianModule(
                    () -> meridian.attached() ? meridian.observer() : null,
                    () -> service, meridian::horizonShowing));
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
    void atAnOrdinaryFieldJupiterIsASpokenSymbolWhereTheServicePutsIt() throws Exception {
        build(8.0);
        paint();
        assertEquals(List.of(), chart.renderedBodies(), "hidden until switched on");
        SwingUtilities.invokeAndWait(() -> module.showing(true));
        paint();
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        assertEquals(JovianModule.JUPITER, drawn.get(0).identity(),
                "Jupiter first; its moons (#485) after it, each decided on its own");
        JupiterObservation jupiter = service.observeJupiter(OSLO);
        DrawnPage page = DrawnPage.of(chart.currentScene());
        PixelPoint expected = new ViewportMapping(page).toPixel(
                page.projection().project(jupiter.astrometricJ2000()).orElseThrow());
        ReferenceInk.BodyPlacement placed = drawn.get(0);
        assertEquals(expected.x(), placed.centre().x(), 1e-6);
        assertEquals(expected.y(), placed.centre().y(), 1e-6);
        assertTrue(placed.symbol(), "1-2 px true at 8°: the 6 px minimum stands in for it");
        assertEquals("Jupiter", placed.name());
        assertTrue(chart.getAccessibleContext().getAccessibleDescription().contains(
                " Jupiter (a cartographic symbol, not Jupiter's apparent diameter)."),
                chart.getAccessibleContext().getAccessibleDescription());
        assertTrue(chart.bodyAt(placed.centre().x(), placed.centre().y()).isEmpty(),
                "a click through the symbol is not consumed: it hides nothing");
    }

    @Test
    void atTheNormalMinimumFieldJupiterIsItselfAndConsumesAClick() throws Exception {
        build(ChartViewState.normalMinimumFieldDegrees());
        SwingUtilities.invokeAndWait(() -> module.showing(true));
        paint();
        ReferenceInk.BodyPlacement placed = chart.renderedBodies().get(0);
        assertFalse(placed.symbol(), "about 10 px at 1°: the true disc");
        assertTrue(chart.getAccessibleContext().getAccessibleDescription().contains(" Jupiter. "),
                chart.getAccessibleContext().getAccessibleDescription());
        assertTrue(chart.bodyAt(placed.centre().x(), placed.centre().y()).isPresent(),
                "the opaque disc consumes a click, so no hidden star is offered");
    }

    @Test
    void theSwitchIsRememberedAndRestoredWithItsViewItemInBothLanguages() throws Exception {
        build(8.0);
        node = Preferences.userRoot().node("juranometria-jupiter-chart-" + System.nanoTime());
        JovianChartStore store = JovianChartStore.forNode(node);
        assertFalse(store.shownOrDefault(), "hidden until chosen");
        Runnable toggle = JovianChartSession.toggle(module, store);
        SwingUtilities.invokeAndWait(toggle);
        assertTrue(module.showing());
        assertEquals(java.util.Optional.of(Boolean.TRUE), store.shown());
        SwingUtilities.invokeAndWait(() -> module.showing(false));
        JMenuBar[] bar = new JMenuBar[1];
        javax.swing.JCheckBoxMenuItem[] item = new javax.swing.JCheckBoxMenuItem[1];
        SwingUtilities.invokeAndWait(() -> {
            bar[0] = AppMenuBar.create(null, null,
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                    InterfaceText.forLanguage("en"));
            item[0] = AppMenuBar.addJupiterOnChart(bar[0], InterfaceText.forLanguage("en"), toggle);
        });
        assertNotNull(item[0], "View > Solar System carries Jupiter's switch");
        assertEquals("Jupiter and moons on the chart", item[0].getText());
        javax.swing.JMenu solar = AppMenuBar.solarSystemMenu(bar[0]);
        assertEquals("jupiterOnChartItem", solar.getMenuComponent(
                solar.getPopupMenu().getComponentIndex(AppMenuBar.moonChartItem(bar[0])) + 1)
                .getName(), "directly after Moon on the chart");
        SwingUtilities.invokeAndWait(() -> JovianChartSession.restore(module, store, item[0]));
        assertTrue(module.showing() && item[0].isSelected(),
                "the module and its tick agree with the store");
        SwingUtilities.invokeAndWait(() -> module.showing(false));
        assertFalse(item[0].isSelected(), "the tick follows the module");
        JMenuBar[] norsk = new JMenuBar[1];
        javax.swing.JCheckBoxMenuItem[] nb = new javax.swing.JCheckBoxMenuItem[1];
        SwingUtilities.invokeAndWait(() -> {
            norsk[0] = AppMenuBar.create(null, null,
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                    InterfaceText.forLanguage("nb-NO"));
            nb[0] = AppMenuBar.addJupiterOnChart(norsk[0], InterfaceText.forLanguage("nb-NO"),
                    toggle);
        });
        assertEquals("Jupiter og månene på kartet", nb[0].getText());
    }
}
