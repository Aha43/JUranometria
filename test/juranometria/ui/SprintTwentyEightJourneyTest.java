package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.app.SwingSession;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.NavigationRequest;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.Ecliptic;
import juranometria.sky.Observer;
import juranometria.ui.ecliptic.EclipticSession;
import juranometria.ui.ecliptic.EclipticStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader's walk through Sprint 28 (issue #275).
 *
 * <p>One journey along the production path: the released default, the
 * ecliptic switched on from the real View menu, its geometry read off
 * the page, the awkward pages crossed, the other modules alongside
 * it, the chart's own settings changed underneath it, and the whole
 * thing put back until the page is the page the atlas draws without
 * any of it.
 *
 * <p>Nothing here reconstructs an expected line. Every claim is asked
 * of the module's own contribution and of the pixels the production
 * painter put down.
 */
class SprintTwentyEightJourneyTest {

    private static BufferedImage paint(ChartComponent chart)
            throws Exception {
        BufferedImage image = new BufferedImage(chart.getWidth(),
                chart.getHeight(), BufferedImage.TYPE_INT_RGB);
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

    private static int differences(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static ChartComponent chart(ChartViewState state)
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(Atlas.assembler());
            holder[0].setSize(900, 700);
            holder[0].setViewState(state);
        });
        SwingUtilities.invokeAndWait(() -> { });
        return holder[0];
    }

    /** The March equinox at 24 degrees: the crossing, on the page. */
    private static final ChartViewState EQUINOX_PAGE =
            new ChartViewState(new SkyPosition(0.0, 0.0), 24.0, 8.0);

    @Test
    void theReaderWalksTheEclipticAndPutsTheChartBackAsTheyFoundIt()
            throws Exception {
        SwingSession.scratchPreferences("sprint-28-journey", node -> {
            EclipticStore store = EclipticStore.forNode(node);
            List<NavigationRequest> asked = new ArrayList<>();

            // ---- 1. the released default, undisturbed ------------
            ChartComponent chart = chart(EQUINOX_PAGE);
            SelectionModel selection = new SelectionModel();
            ChartModuleHost host = new ChartModuleHost(chart, selection,
                    asked::add);
            BufferedImage released = paint(chart);
            ChartViewState wherever = chart.viewState();

            EclipticModule ecliptic = EclipticSession.begin(host);
            JMenuBar bar = AppMenuBar.create(null, null, () -> { },
                    () -> { }, () -> { }, () -> { },
                    EclipticSession.toggle(ecliptic, store));
            JCheckBoxMenuItem item = AppMenuBar.eclipticItem(bar);
            EclipticSession.restore(ecliptic, store, item);

            assertFalse(item.isSelected(),
                    "1. a reader who has never asked opens with the"
                            + " ecliptic hidden");
            assertEquals(0, differences(released, paint(chart)),
                    "and loading the module disturbed no pixel");
            assertEquals(wherever, chart.viewState(),
                    "no page moved");
            assertEquals(List.of(), selection.candidates(),
                    "and nothing became selected");

            // ---- 2. switched on, and read off the page -----------
            item.doClick();
            BufferedImage shown = paint(chart);
            assertTrue(item.isSelected(), "2. the reader asks for it");
            assertTrue(differences(released, shown) > 100,
                    "and the ecliptic is drawn");

            List<String> offered = new ArrayList<>();
            for (OverlayRegistry.Owned owned : chart.overlays().collect()) {
                offered.add(owned.geometry().identity());
            }
            assertEquals(List.of("ecliptic", "march-equinox",
                            "june-solstice", "september-equinox",
                            "december-solstice"), offered,
                    "the circle and its four named landmarks, and"
                            + " nothing else");

            // Its geometry, in the chart's own fixed frame: the March
            // equinox at the right-ascension origin, the solstices an
            // obliquity north and south, every landmark on the
            // circle.
            assertEquals(0.0, Ecliptic.landmark("march-equinox")
                            .orElseThrow().at().raDegrees(), 1.0e-9,
                    "the March equinox sits at the RA origin, which is"
                            + " what it means on a J2000 chart");
            assertEquals(Ecliptic.OBLIQUITY_DEGREES,
                    Ecliptic.landmark("june-solstice").orElseThrow()
                            .at().decDegrees(), 1.0e-9,
                    "the June solstice is an obliquity north");
            assertEquals(-Ecliptic.OBLIQUITY_DEGREES,
                    Ecliptic.landmark("december-solstice").orElseThrow()
                            .at().decDegrees(), 1.0e-9,
                    "and the December solstice as far south");
            for (Ecliptic.Landmark landmark : Ecliptic.landmarks()) {
                assertEquals(0.0,
                        Ecliptic.latitudeDegrees(landmark.at()), 1.0e-9,
                        landmark.name() + " lies on the circle");
            }

            // ---- 3. the awkward pages ---------------------------
            record Page(String what, ChartViewState state,
                        boolean crosses) {
            }
            for (Page awkward : List.of(
                    new Page("the right-ascension wrap",
                            new ChartViewState(new SkyPosition(359.0, 0.0),
                                    18.0, 8.0), true),
                    new Page("a narrow field at the crossing",
                            new ChartViewState(new SkyPosition(0.0, 0.0),
                                    4.0, 8.0), true),
                    new Page("the widest field",
                            new ChartViewState(new SkyPosition(0.0, 0.0),
                                    36.0, 8.0), true),
                    new Page("a dense Milky Way field",
                            new ChartViewState(new SkyPosition(270.0,
                                    -Ecliptic.OBLIQUITY_DEGREES), 8.0,
                                    8.0), true),
                    new Page("sparse southern sky",
                            new ChartViewState(new SkyPosition(45.0,
                                    -60.0), 24.0, 8.0), false),
                    new Page("the celestial pole",
                            new ChartViewState(new SkyPosition(0.0, 88.0),
                                    8.0, 8.0), false))) {
                ChartComponent onPage = chart(awkward.state());
                ChartModuleHost pageHost = new ChartModuleHost(onPage,
                        new SelectionModel(), asked::add);
                BufferedImage bare = paint(onPage);
                EclipticModule on = pageHost.attach(new EclipticModule());
                on.showing(true);
                int ink = differences(bare, paint(onPage));
                if (awkward.crosses()) {
                    assertTrue(ink > 20, "3. drawn on " + awkward.what()
                            + ": " + ink + " px");
                } else {
                    assertEquals(0, ink, "3. and silent on "
                            + awkward.what() + ", where it does not"
                            + " reach - no invented chord");
                }
            }

            // ---- 4. beside the observer's own lines --------------
            // An equinox-inspired frozen instant. The ecliptic has no
            // observer and no clock; the meridian has both. They are
            // drawn on one page without either borrowing the other's
            // frame.
            MeridianModule meridian = host.attach(new MeridianModule(
                    new Observer(59.9, 10.7, java.time.Instant.parse(
                            "2026-03-20T21:33:00Z"))));
            meridian.showing(true, true, true);
            List<String> together = new ArrayList<>();
            for (OverlayRegistry.Owned owned : chart.overlays().collect()) {
                together.add(owned.geometry().identity());
            }
            assertEquals(List.of("december-solstice", "ecliptic",
                            "horizon", "june-solstice", "march-equinox",
                            "meridian", "september-equinox", "zenith"),
                    together.stream().sorted().toList(),
                    "4. every contributed line is accounted for: five"
                            + " of the fixed sky and three of the"
                            + " observer's, and nothing else");
            assertTrue(ecliptic.showing() && meridian.meridianShowing(),
                    "both modules are showing, neither having touched"
                            + " the other's state");

            meridian.detach();
            assertEquals(0, differences(shown, paint(chart)),
                    "and removing the observer's lines gives back the"
                            + " ecliptic's own page, byte for byte -"
                            + " the frames never mixed");

            // ---- 5. the chart's own settings, changed underneath -
            SwingUtilities.invokeAndWait(() -> chart.setChartOptions(
                    ChartOptions.DEFAULTS.withPalette(
                            ChartPalette.BLACK_SKY)));
            assertTrue(ecliptic.showing(),
                    "5. changing the ground leaves the ecliptic shown");
            assertTrue(item.isSelected(), "and its tick set");
            SwingUtilities.invokeAndWait(() -> chart.setChartOptions(
                    ChartOptions.DEFAULTS));
            assertEquals(0, differences(shown, paint(chart)),
                    "and changing it back gives the same page");

            WorkingSelection working = new WorkingSelection();
            working.clear();
            assertTrue(ecliptic.showing(),
                    "the working selection is not its business either");
            assertEquals(List.of(), asked,
                    "and nothing in any of this asked the chart to"
                            + " move");

            // ---- 6. put away, and remembered ---------------------
            item.doClick();
            assertFalse(item.isSelected(), "6. the reader puts it away");
            assertEquals(0, differences(released, paint(chart)),
                    "and the ordinary chart is back, byte for byte");
            assertEquals(java.util.Optional.of(Boolean.FALSE),
                    store.shown(),
                    "with the choice remembered as a choice");

            ecliptic.detach();
            assertEquals(0, differences(released, paint(chart)),
                    "detaching leaves the same page");
            assertEquals(List.of(), chart.overlays().collect(),
                    "and no contribution behind");
            assertEquals(1, node.keys().length,
                    "and exactly one preference is kept - only the"
                            + " approved one returns: "
                            + java.util.Arrays.toString(node.keys()));

            // ---- 7. home, module-free ---------------------------
            ChartComponent home = chart(ChartViewState.DEFAULT);
            ChartComponent neverHadAModule =
                    chart(ChartViewState.DEFAULT);
            assertEquals(0, differences(paint(home),
                            paint(neverHadAModule)),
                    "7. and Home is the released page, drawn by an"
                            + " atlas that never had the module at"
                            + " all");
        });
    }
}
