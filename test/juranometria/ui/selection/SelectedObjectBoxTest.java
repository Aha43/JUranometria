package juranometria.ui.selection;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.module.Furniture;
import juranometria.render.ChartFurniture;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solarchart.SolarSystemModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The selected-object box on a production chart under a production module
 * host (#692, ruled C-7 to C-14 on #690): it describes the working
 * selection's lead exactly while the lead is on the page, the switch is on
 * and the chart is not clean; it prefers the upper left; and a selected
 * body is ringed.
 */
class SelectedObjectBoxTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));

    private static final class Bench {
        ChartComponent chart;
        ChartModuleHost host;
        MeridianModule meridian;
        SelectedObjectSwitch shown = new SelectedObjectSwitch(SelectedObjectStore.inMemory());
        SelectedObjectBox box;

        void paint() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                BufferedImage image = new BufferedImage(chart.getWidth(), chart.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                try {
                    chart.paint(g);
                } finally {
                    g.dispose();
                }
            });
        }

        BufferedImage image() throws Exception {
            BufferedImage[] out = new BufferedImage[1];
            SwingUtilities.invokeAndWait(() -> {
                out[0] = new BufferedImage(chart.getWidth(), chart.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = out[0].createGraphics();
                try {
                    chart.paint(g);
                } finally {
                    g.dispose();
                }
            });
            return out[0];
        }

        void view(SkyPosition centre, double field) throws Exception {
            SwingUtilities.invokeAndWait(() -> chart.setViewState(
                    new ChartViewState(centre, field, 8.0)));
            paint();
        }

        void select(String identity) throws Exception {
            SwingUtilities.invokeAndWait(() ->
                    host.workingSelection().replaceWith(List.of(identity), identity));
            paint();
        }

        Optional<ChartFurniture.Placed> drawnBox() {
            return chart.renderedFurniture().stream().map(d -> d.placed())
                    .filter(p -> p.furniture().key().endsWith("/" + SelectedObjectFacts.IDENTITY))
                    .findFirst();
        }
    }

    private static Bench bench(int width, int height) throws Exception {
        Bench b = new Bench();
        SwingUtilities.invokeAndWait(() -> {
            b.chart = new ChartComponent(Atlas.assembler(), PageText.in(EN));
            b.chart.setSize(width, height);
            b.host = new ChartModuleHost(b.chart, new SelectionModel(), r -> { });
            b.meridian = b.host.attach(new MeridianModule(OSLO));
            b.box = b.host.attach(new SelectedObjectBox(b.shown, id -> false, EN));
        });
        return b;
    }

    private static SkyPosition at(String query) {
        return Atlas.search().search(query).get(0).position();
    }

    private static String id(String query) {
        return Atlas.search().search(query).get(0).identity();
    }

    @Test
    void anUntouchedChartIsTheReleasedPage() throws Exception {
        Bench b = bench(900, 700);
        b.view(at("Betelgeuse"), 8.0);
        assertTrue(b.drawnBox().isEmpty(), "no selection, no box");
        assertTrue(b.box.offered().isEmpty());
    }

    @Test
    void theLeadIsDescribedInTheUpperLeftWithTheRulesFacts() throws Exception {
        Bench b = bench(900, 700);
        b.view(at("Betelgeuse"), 8.0);
        b.select(id("Betelgeuse"));
        ChartFurniture.Placed placed = b.drawnBox().orElseThrow();
        assertEquals(ChartFurniture.Corner.UPPER_LEFT, placed.corner(), "upper left first (C-11)");
        assertEquals("Betelgeuse", placed.furniture().heading());
        assertEquals(List.of("α Ori · TYC 129-1873-1", "V 0.58", "5h 55.2m   +7° 24′"),
                placed.furniture().lines());
        assertTrue(b.chart.getAccessibleContext().getAccessibleDescription()
                .contains("Selected: Betelgeuse, α Ori"), "spoken with the page");
    }

    @Test
    void theBoxLeavesWithTheSubjectAndReturnsWithIt() throws Exception {
        Bench b = bench(900, 700);
        b.view(at("M 42"), 8.0);
        b.select(id("M 42"));
        assertTrue(b.drawnBox().isPresent());
        b.view(new SkyPosition(120.0, -5.0), 8.0);
        assertTrue(b.drawnBox().isEmpty(), "off the page, no box");
        b.view(at("M 42"), 8.0);
        assertEquals("M 42", b.drawnBox().orElseThrow().furniture().heading(), "back with it");
    }

    @Test
    void theSwitchAndCleanChartHideItAndTouchNothingElse() throws Exception {
        Bench b = bench(900, 700);
        b.view(at("M 42"), 8.0);
        b.select(id("M 42"));
        SwingUtilities.invokeAndWait(() -> b.shown.show(false));
        b.paint();
        assertTrue(b.drawnBox().isEmpty(), "switched off");
        SwingUtilities.invokeAndWait(() -> b.shown.show(true));
        b.paint();
        assertTrue(b.drawnBox().isPresent(), "switched on");
        SwingUtilities.invokeAndWait(() -> b.box.chromeShown(false));
        b.paint();
        assertTrue(b.drawnBox().isEmpty(), "a clean chart hides it (C-12)");
        assertEquals(id("M 42"), b.host.workingSelection().lead(), "the selection is kept");
        assertTrue(b.shown.showing(), "and the setting");
        SwingUtilities.invokeAndWait(() -> b.box.chromeShown(true));
        b.paint();
        assertTrue(b.drawnBox().isPresent(), "restored with the toolbar");
    }

    @Test
    void theRuledBoxesKeepTheirCornersAndThisOneTakesTheUpperLeft() throws Exception {
        Bench b = bench(900, 700);
        b.view(at("M 42"), 8.0);
        SwingUtilities.invokeAndWait(() -> b.meridian.showing(true, true, false));
        juranometria.ui.placeandtime.PlaceAndTimeFurniture.offer(b.meridian, () -> false,
                new juranometria.ui.placeandtime.UpdateToNow(b.meridian, () -> OSLO.instant()),
                EN);
        b.select(id("M 42"));
        List<ChartFurniture.Placed> drawn = b.chart.renderedFurniture().stream()
                .map(d -> d.placed()).toList();
        assertEquals(2, drawn.size());
        assertEquals(ChartFurniture.Corner.LOWER_RIGHT, drawn.stream()
                .filter(p -> !p.furniture().upperLeftFirst()).findFirst().orElseThrow().corner(),
                "Place and Time keeps the ruled order");
        assertEquals(ChartFurniture.Corner.UPPER_LEFT, b.drawnBox().orElseThrow().corner());
    }

    @Test
    void aSelectedBodyIsRingedAndDescribedForTheChartTime() throws Exception {
        Bench b = bench(900, 700);
        SolarSystemService service = SolarSystemService.load();
        SolarSystemModule[] solar = new SolarSystemModule[1];
        SwingUtilities.invokeAndWait(() -> {
            solar[0] = b.host.attach(new SolarSystemModule(() -> b.meridian.observer(),
                    () -> service, () -> false));
            solar[0].moonShowing(true);
        });
        SkyPosition moon = null;
        for (SkyPosition look : List.of(new SkyPosition(0, 0), new SkyPosition(180, 0),
                new SkyPosition(0, 89), new SkyPosition(0, -89))) {
            b.view(look, 180.0);
            moon = b.host.inventory().bodies().stream().filter(x -> x.identity().equals("moon"))
                    .map(x -> x.position()).findFirst().orElse(null);
            if (moon != null) {
                break;
            }
        }
        b.view(moon, 3.0);
        BufferedImage before = b.image();
        b.select("moon");
        BufferedImage after = b.image();
        ChartFurniture.Placed placed = b.drawnBox().orElseThrow();
        assertEquals("Moon", placed.furniture().heading());
        assertEquals("for 2026-03-20 21:33 UTC", placed.furniture().lines().get(0));
        var disc = b.chart.renderedBodies().stream().filter(x -> x.identity().equals("moon"))
                .findFirst().orElseThrow();
        double r = Math.max(disc.disc().getBounds2D().getWidth(),
                disc.disc().getBounds2D().getHeight()) / 2.0 + 5.0;
        int x = (int) Math.round(disc.centre().x() + r);
        int y = (int) Math.round(disc.centre().y());
        boolean ringed = false;
        for (int dx = -2; dx <= 2 && !ringed; dx++) {
            ringed = before.getRGB(x + dx, y) != after.getRGB(x + dx, y);
        }
        assertTrue(ringed, "the ring stands just clear of the disc (C-10)");
    }

    @Test
    void theBoxCarriesNoControlAndTravelsToPaper() throws Exception {
        Bench b = bench(900, 700);
        b.view(at("M 42"), 8.0);
        b.select(id("M 42"));
        Furniture offered = b.box.offered().orElseThrow();
        assertEquals(null, offered.action(), "nothing to press, so nothing to focus");
        assertTrue(offered.upperLeftFirst());
        assertTrue(b.chart.paperFurniture().stream().anyMatch(f ->
                f.box().heading().equals("M 42") && f.box().upperLeftFirst()),
                "the sheet carries the same box, upper left first");
        assertFalse(b.chart.paperFurniture().isEmpty());
    }
}
