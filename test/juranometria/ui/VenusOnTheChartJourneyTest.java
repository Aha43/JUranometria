package juranometria.ui;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.VenusService.VenusObservation;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.solar.SunChartSession;
import juranometria.ui.solar.VenusChartSession;
import juranometria.ui.solar.VenusChartStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Venus on the chart, through the component (Sprint 48, issue #514,
 * under the rulings on #512 and #513): switched on, it is drawn where
 * the service puts it - the true disc at the minimum field, the phased
 * 6 px mark at 8°, spoken as a symbol there - named; a click on it
 * selects Venus's occurrence at the instant and place drawn; the switch
 * is its own, remembered and restored into its menu item in both
 * languages; and View's Solar System submenu keeps the ruled order.
 * The instant is the 2026 greatest eastern elongation Espenak's row
 * cites ({@code docs/studies/venus/venus-events.txt}).
 */
class VenusOnTheChartJourneyTest {

    private static final PageText ENGLISH = PageText.in(InterfaceText.forLanguage("en"));
    private static final SolarSystemService SERVICE = SolarSystemService.load();

    private ChartComponent chart;
    private SelectionModel selection;
    private ChartModuleHost host;
    private SolarSystemModule module;
    private Preferences node;

    private static Instant elongation() throws Exception {
        for (String line : Files.readAllLines(Path.of("docs/studies/venus/venus-events.txt"),
                StandardCharsets.UTF_8)) {
            if (line.startsWith("2026-greatest-elongation-east\t")) {
                String iso = line.split("\t")[1];
                return Instant.parse(iso.length() == 17 ? iso.substring(0, 16) + ":00Z" : iso);
            }
        }
        throw new IllegalStateException("the cited elongation is in the fixture");
    }

    private static VenusObservation venus(Observer at) {
        return (VenusObservation) SERVICE.observe(Body.VENUS, at);
    }

    private void build(Observer observer, double field) throws Exception {
        VenusObservation v = venus(observer);
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler(), ENGLISH);
            chart.setSize(900, 700);
            chart.setViewState(new ChartViewState(v.astrometricJ2000(), field, 8.0, null, null));
            selection = new SelectionModel();
            host = new ChartModuleHost(chart, selection, request -> { });
            SelectInteraction.install(chart, selection, host.workingSelection(),
                    new juranometria.chart.SelectionMode());
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
        flush();
    }

    private void click(double pageX, double pageY) throws Exception {
        int cx = (int) Math.round(pageX) + chart.pageOffsetX();
        int cy = (int) Math.round(pageY) + chart.pageOffsetY();
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED,
                MouseEvent.MOUSE_CLICKED}) {
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(new MouseEvent(chart, id,
                    System.nanoTime() / 1_000_000, 0, cx, cy, 1, false, MouseEvent.BUTTON1)));
        }
        flush();
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
        build(new Observer(59.913, 10.752, elongation()), 8.0);
        paint();
        assertEquals(List.of(), chart.renderedBodies());
        assertFalse(chart.getAccessibleContext().getAccessibleDescription().contains("Venus"));
    }

    @Test
    void switchedOnVenusIsDrawnWhereTheServicePutsItAsTheRuleSaysAndSpoken()
            throws Exception {
        Observer oslo = new Observer(59.913, 10.752, elongation());
        VenusObservation v = venus(oslo);
        // At 8°: the mark stands in, a symbol, spoken as one.
        build(oslo, 8.0);
        SwingUtilities.invokeAndWait(() -> module.venusShowing(true));
        paint();
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        assertEquals(1, drawn.size());
        ReferenceInk.BodyPlacement symbol = drawn.get(0);
        assertEquals(SolarSystemModule.VENUS, symbol.identity());
        DrawnPage page = DrawnPage.of(chart.currentScene());
        PixelPoint expected = new ViewportMapping(page).toPixel(
                page.projection().project(v.astrometricJ2000()).orElseThrow());
        assertEquals(expected.x(), symbol.centre().x(), 1e-6);
        assertEquals(expected.y(), symbol.centre().y(), 1e-6);
        assertTrue(symbol.symbol(), "the 6 px mark stands in at 8°");
        assertEquals(SolarSystemModule.VENUS_MINIMUM_MARK_PX,
                symbol.disc().getBounds2D().getWidth(), 0.05);
        assertEquals("Venus", symbol.name());
        String said = chart.getAccessibleContext().getAccessibleDescription();
        assertTrue(said.contains(" Venus (a cartographic symbol, not Venus's apparent"
                + " diameter)."), said);
        assertTrue(chart.bodyAt(symbol.centre().x(), symbol.centre().y()).isEmpty(),
                "a symbol hides nothing");
        assertTrue(chart.selectableBodyAt(symbol.centre().x(), symbol.centre().y()).isPresent(),
                "but is pointed at as Venus");
        SwingUtilities.invokeAndWait(host::detachAll);
        host = null;

        // At the minimum field: the true disc, itself, opaque.
        build(oslo, ChartViewState.normalMinimumFieldDegrees());
        SwingUtilities.invokeAndWait(() -> module.venusShowing(true));
        paint();
        ReferenceInk.BodyPlacement disc = chart.renderedBodies().get(0);
        assertFalse(disc.symbol(), "about 6.1 px at 1°: the true disc");
        assertTrue(chart.bodyAt(disc.centre().x(), disc.centre().y()).isPresent(),
                "opaque: it consumes what it covers");
        said = chart.getAccessibleContext().getAccessibleDescription();
        assertTrue(said.contains(" Venus.") || said.endsWith(" Venus."), said);
        assertFalse(said.contains("symbol"), said);
    }

    /** The transit through the component at 6°: Venus's symbol over the Sun, the Sun opaque beneath. */
    @Test
    void inTransitTheSymbolIsDrawnOverTheSunAndIsPointedAtAsVenus() throws Exception {
        Instant transit = null;
        for (String line : Files.readAllLines(Path.of("docs/studies/venus/venus-events.txt"),
                StandardCharsets.UTF_8)) {
            if (line.startsWith("2012-inferior-conjunction\t")) {
                String iso = line.split("\t")[1];
                transit = Instant.parse(iso.length() == 17 ? iso.substring(0, 16) + ":00Z" : iso);
            }
        }
        Observer oslo = new Observer(59.913, 10.752, transit);
        build(oslo, 6.0);
        SwingUtilities.invokeAndWait(() -> {
            module.sunShowing(true);
            module.venusShowing(true);
        });
        paint();
        List<ReferenceInk.BodyPlacement> drawn = chart.renderedBodies();
        assertEquals(2, drawn.size(), "the Sun, then Venus in front");
        assertEquals(SolarSystemModule.SUN, drawn.get(0).identity());
        ReferenceInk.BodyPlacement venus = drawn.get(1);
        assertTrue(venus.symbol());
        assertEquals(SolarSystemModule.SUN, chart.bodyAt(venus.centre().x(), venus.centre().y())
                .orElseThrow().identity(), "the opaque disc under the pointer is the Sun's");
        assertEquals(SolarSystemModule.VENUS, chart.selectableBodyAt(venus.centre().x(),
                venus.centre().y()).orElseThrow().identity(), "and Venus is what is pointed at");
        String said = chart.getAccessibleContext().getAccessibleDescription();
        assertTrue(said.contains("Venus, in front of the Sun"), said);
    }

    @Test
    void aClickOnVenusSelectsItsOccurrence() throws Exception {
        Observer oslo = new Observer(59.913, 10.752, elongation());
        build(oslo, ChartViewState.normalMinimumFieldDegrees());
        SwingUtilities.invokeAndWait(() -> module.venusShowing(true));
        paint();
        ReferenceInk.BodyPlacement drawn = chart.renderedBodies().get(0);
        click(drawn.centre().x(), drawn.centre().y());
        Selection.Occurrence chosen = assertInstanceOf(Selection.Occurrence.class,
                selection.selection(), "Venus is a selectable occurrence (#513, ruling 2): "
                        + selection.selection());
        assertEquals(SolarSystemModule.VENUS, chosen.identity());
        assertEquals(oslo.instant(), chosen.instant());
        assertEquals(List.of(), selection.candidates());
        assertEquals(List.of(SolarSystemModule.VENUS), host.workingSelection().members());
        assertTrue(host.inventory().holds(SolarSystemModule.VENUS), "in the page's inventory");
    }

    @Test
    void theSwitchIsItsOwnRememberedAndRestoredWithItsMenuItemInBothLanguages()
            throws Exception {
        Observer oslo = new Observer(59.913, 10.752, elongation());
        node = Preferences.userRoot().node("juranometria-514-venus-" + System.nanoTime());
        VenusChartStore store = VenusChartStore.forNode(node);
        assertFalse(store.shownOrDefault(), "hidden until asked");
        build(oslo, 8.0);
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            javax.swing.JMenuBar[] bar = new javax.swing.JMenuBar[1];
            javax.swing.JCheckBoxMenuItem[] item = new javax.swing.JCheckBoxMenuItem[1];
            SwingUtilities.invokeAndWait(() -> {
                bar[0] = AppMenuBar.create(new ChartViewController(), () -> { }, () -> { },
                        () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                        () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, said);
                item[0] = AppMenuBar.addVenusOnChart(bar[0], said,
                        VenusChartSession.toggle(module, store));
                VenusChartSession.restore(module, store, item[0]);
            });
            flush();
            assertEquals(said.say("menu.venuschart.label"), item[0].getText());
            assertFalse(item[0].isSelected(), language + ": restored hidden");
            assertFalse(module.venusShowing());
            // The item sits directly after the Sun's, before the Moon's (the ruled order).
            javax.swing.JMenu solar = AppMenuBar.solarSystemMenu(bar[0]);
            int sun = -1;
            int venus = -1;
            int moon = -1;
            for (int i = 0; i < solar.getMenuComponentCount(); i++) {
                String name = solar.getMenuComponent(i).getName();
                if (AppMenuBar.SUN_CHART_ITEM.equals(name)) {
                    sun = i;
                } else if (AppMenuBar.VENUS_CHART_ITEM.equals(name)) {
                    venus = i;
                } else if (AppMenuBar.MOON_CHART_ITEM.equals(name)) {
                    moon = i;
                }
            }
            assertTrue(sun >= 0 && venus == sun + 1 && moon == venus + 1,
                    language + ": Sun, Venus, Moon on the chart in that order: "
                            + sun + ", " + venus + ", " + moon);
            SwingUtilities.invokeAndWait(item[0]::doClick);
            flush();
            assertTrue(module.venusShowing(), language + ": the item switched Venus on");
            assertTrue(store.shownOrDefault(), "and remembered it");
            assertTrue(item[0].isSelected());
            paint();
            assertEquals(1, chart.renderedBodies().size());
            assertFalse(module.sunShowing() || module.moonShowing(),
                    "its own switch: the Sun's and the Moon's untouched");
            SwingUtilities.invokeAndWait(item[0]::doClick);
            flush();
            assertFalse(module.venusShowing());
            assertFalse(store.shownOrDefault());
        }
    }
}
