package juranometria.ui;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.saturnchart.SaturnModule;
import juranometria.sky.Observer;
import juranometria.solar.SaturnSystemService;
import juranometria.solar.SaturnSystemService.SaturnObservation;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.SaturnChartSession;
import juranometria.ui.solar.SaturnChartStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Saturn on the production chart (issue #579), through the real
 * component, module host and selection: hidden until the switch is on;
 * then drawn where the service puts it and spoken - as a cartographic
 * symbol at ordinary fields, as Saturn itself at the normal minimum
 * field; a click through the symbol is not consumed, a click on the
 * figure is; the switch remembered and restored with its View item,
 * directly after Jupiter's, in both languages.
 */
class SaturnOnTheChartJourneyTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    /** The contract's card instant (#574) at Oslo. */
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-10-05T00:00:00Z"));
    private static SaturnSystemService service;

    private ChartComponent chart;
    private ChartModuleHost host;
    private SaturnModule module;
    private Preferences node;

    @BeforeAll
    static void load() {
        service = SaturnSystemService.load();
    }

    private void build(double field) throws Exception {
        SaturnObservation saturn = service.observeSaturn(OSLO);
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart.setSize(900, 700);
            chart.setViewState(new ChartViewState(saturn.astrometricJ2000(), field, 8.0,
                    null, null));
            SelectionModel selection = new SelectionModel();
            SelectInteraction.install(chart, selection, new juranometria.chart.WorkingSelection(),
                    new juranometria.chart.SelectionMode());
            host = new ChartModuleHost(chart, selection, request -> { });
            MeridianModule meridian = host.attach(new MeridianModule(OSLO));
            meridian.showing(false, false, false);
            module = host.attach(new SaturnModule(
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
    void atAnOrdinaryFieldSaturnIsASpokenSymbolWhereTheServicePutsIt() throws Exception {
        build(8.0);
        paint();
        assertEquals(List.of(), chart.renderedBodies(), "hidden until switched on");
        SwingUtilities.invokeAndWait(() -> module.showing(true));
        paint();
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        assertEquals(1, drawn.size(), "Saturn alone; its moons follow in the second pull request");
        ReferenceInk.BodyPlacement placed = drawn.get(0);
        assertEquals(SaturnModule.SATURN, placed.identity());
        SaturnObservation saturn = service.observeSaturn(OSLO);
        DrawnPage page = DrawnPage.of(chart.currentScene());
        PixelPoint expected = new ViewportMapping(page).toPixel(
                page.projection().project(saturn.astrometricJ2000()).orElseThrow());
        assertEquals(expected.x(), placed.centre().x(), 1e-6);
        assertEquals(expected.y(), placed.centre().y(), 1e-6);
        assertTrue(placed.symbol(), "the rings about 1.4 px at 8°: the 6 px minimum stands in");
        assertEquals("Saturn", placed.name());
        assertTrue(chart.getAccessibleContext().getAccessibleDescription().contains(
                " Saturn (a cartographic symbol, not Saturn's apparent diameter)."),
                chart.getAccessibleContext().getAccessibleDescription());
        assertTrue(chart.bodyAt(placed.centre().x(), placed.centre().y()).isEmpty(),
                "a click through the symbol is not consumed: it hides nothing");
        assertTrue(chart.selectableBodyAt(placed.centre().x(), placed.centre().y())
                .isPresent(), "but the symbol is pointed at as Saturn (#513)");
    }

    @Test
    void atTheNormalMinimumFieldSaturnIsItselfAndConsumesAClick() throws Exception {
        build(ChartViewState.normalMinimumFieldDegrees());
        SwingUtilities.invokeAndWait(() -> module.showing(true));
        paint();
        ReferenceInk.BodyPlacement placed = chart.renderedBodies().get(0);
        assertFalse(placed.symbol(), "the rings about 11 px at 1°: the true figure");
        String said = chart.getAccessibleContext().getAccessibleDescription();
        assertTrue(said.contains(" Saturn. ") || said.endsWith(" Saturn."), said);
        assertTrue(chart.bodyAt(placed.centre().x(), placed.centre().y()).isPresent(),
                "the opaque globe consumes a click, so no hidden star is offered");
        assertEquals(SaturnModule.SATURN, chart.selectableBodyAt(placed.centre().x(),
                placed.centre().y()).orElseThrow().identity(),
                "and the figure is pointed at as Saturn's occurrence (#513)");
    }

    @Test
    void theSwitchIsRememberedAndRestoredWithItsViewItemInBothLanguages() throws Exception {
        build(8.0);
        node = Preferences.userRoot().node("juranometria-saturn-chart-" + System.nanoTime());
        SaturnChartStore store = SaturnChartStore.forNode(node);
        assertFalse(store.shownOrDefault(), "hidden until chosen");
        Runnable toggle = SaturnChartSession.toggle(module, store);
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
            AppMenuBar.addJupiterOnChart(bar[0], InterfaceText.forLanguage("en"), () -> { });
            item[0] = AppMenuBar.addSaturnOnChart(bar[0], InterfaceText.forLanguage("en"), toggle);
        });
        assertNotNull(item[0], "View > Solar System carries Saturn's switch");
        assertEquals("Saturn on the chart", item[0].getText());
        javax.swing.JMenu solar = AppMenuBar.solarSystemMenu(bar[0]);
        int jupiter = -1;
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            if ("jupiterOnChartItem".equals(solar.getMenuComponent(i).getName())) {
                jupiter = i;
            }
        }
        assertTrue(jupiter >= 0, "Jupiter's item is there");
        assertEquals("saturnOnChartItem", solar.getMenuComponent(jupiter + 1).getName(),
                "directly after Jupiter and moons on the chart");
        SwingUtilities.invokeAndWait(() -> SaturnChartSession.restore(module, store, item[0]));
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
            nb[0] = AppMenuBar.addSaturnOnChart(norsk[0], InterfaceText.forLanguage("nb-NO"),
                    toggle);
        });
        assertEquals("Saturn på kartet", nb[0].getText());
    }
}
