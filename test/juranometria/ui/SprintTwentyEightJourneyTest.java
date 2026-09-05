package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import java.awt.GraphicsEnvironment;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    /** How many pixels changed in a box about a point. */
    private static int inkCount(BufferedImage without, BufferedImage with,
                                int x, int y, int radius) {
        int count = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px >= 0 && py >= 0 && px < with.getWidth()
                        && py < with.getHeight()
                        && with.getRGB(px, py) != without.getRGB(px, py)) {
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
        // The walk ends by PRESSING the toolbar's Reset view, which
        // means the toolbar has to be on screen: a click on a button
        // in no window proves nothing about whether a reader could
        // reach it (PR #280 round 4). The geometry, the module, the
        // ink and the store are all covered headless elsewhere.
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "pressing a real control needs a display");
        SwingSession.scratchPreferences("sprint-28-journey", node -> {
            EclipticStore store = EclipticStore.forNode(node);
            List<NavigationRequest> asked = new ArrayList<>();

            // ---- 1. the released default, undisturbed ------------
            // Through the production controller, which is what the
            // toolbar's Reset view drives - so Home at step 7 is the
            // reader's own control rather than an assignment
            // (PR #280 re-review).
            ChartViewController controller =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartComponent chart = chart(ChartViewState.DEFAULT);
            AtlasToolbar[] toolbarHolder = new AtlasToolbar[1];
            JFrame[] window = new JFrame[1];
            SwingUtilities.invokeAndWait(() -> {
                controller.onChange(chart::setViewState);
                // The real toolbar, because Home is a control a
                // reader presses - calling the controller's method
                // would skip the button that does it and whatever
                // else that button does (PR #280 round 3).
                toolbarHolder[0] = new AtlasToolbar(controller,
                        new SearchField(new juranometria.search
                                .LocalSearch(List.of(), List.of()),
                                Atlas.assembler(), controller));
                // On screen, in a window a reader can see: showing,
                // size and point-reachability are what make a press
                // evidence of anything.
                window[0] = new JFrame("sprint-28-journey");
                window[0].setLayout(new java.awt.BorderLayout());
                window[0].add(toolbarHolder[0], java.awt.BorderLayout.NORTH);
                // The chart keeps the 900x700 the rest of the suite
                // compares at, so packing around it gives a real
                // reader's window without resizing the page under
                // the pixel comparisons.
                chart.setPreferredSize(new java.awt.Dimension(900, 700));
                window[0].add(chart, java.awt.BorderLayout.CENTER);
                window[0].pack();
                window[0].setVisible(true);
            });
            SwingUtilities.invokeAndWait(() -> { });
            AtlasToolbar toolbar = toolbarHolder[0];
            assertEquals(900, chart.getWidth(),
                    "the page is the size the suite compares at");
            assertEquals(700, chart.getHeight(),
                    "in both directions");
            BufferedImage home = paint(chart);
            SwingUtilities.invokeAndWait(() -> controller.recenter(
                    EQUINOX_PAGE.centre(),
                    EQUINOX_PAGE.fieldWidthDegrees()));
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(EQUINOX_PAGE.centre(),
                    chart.viewState().centre(),
                    "1. the reader navigates to the equinox page"
                            + " through the controller the toolbar"
                            + " drives");
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

            // Its geometry, taken from what the MODULE contributed
            // rather than from the model it was built from: asking
            // the static model would prove the model and say nothing
            // about what this page is carrying (PR #280 review).
            java.util.Map<String, SkyPosition> contributed =
                    new java.util.LinkedHashMap<>();
            SkyPosition circlePole = null;
            for (OverlayRegistry.Owned owned : chart.overlays().collect()) {
                if (owned.geometry()
                        instanceof juranometria.module.OverlayContribution
                                .Point point) {
                    assertEquals(juranometria.module.OverlayContribution
                                    .Mark.LANDMARK, point.mark(),
                            point.accessibleName() + " is offered as a"
                                    + " landmark, not a place");
                    contributed.put(point.identity(), point.at());
                } else if (owned.geometry()
                        instanceof juranometria.module.OverlayContribution
                                .GreatCircle circle) {
                    circlePole = circle.pole();
                }
            }
            assertEquals(0.0,
                    contributed.get("march-equinox").raDegrees(), 1.0e-9,
                    "the offered March equinox sits at the RA origin,"
                            + " which is what it means on a J2000"
                            + " chart");
            assertEquals(Ecliptic.OBLIQUITY_DEGREES,
                    contributed.get("june-solstice").decDegrees(), 1.0e-9,
                    "the offered June solstice is an obliquity north");
            assertEquals(-Ecliptic.OBLIQUITY_DEGREES,
                    contributed.get("december-solstice").decDegrees(),
                    1.0e-9, "and the December solstice as far south");
            for (java.util.Map.Entry<String, SkyPosition> each
                    : contributed.entrySet()) {
                assertEquals(90.0,
                        circlePole.separationDegrees(each.getValue()),
                        1.0e-9, each.getKey() + " lies on the circle"
                                + " this module offered, not on some"
                                + " other one");
            }

            // And the one this page carries is actually drawn, as a
            // mark rather than as the line running through it: the
            // circle passes through every landmark, so ink near one
            // proves nothing on its own.
            double[] equinoxAt = host.projection()
                    .toPage(contributed.get("march-equinox"))
                    .orElseThrow();
            int atTheMark = inkCount(released, shown,
                    (int) Math.round(equinoxAt[0]),
                    (int) Math.round(equinoxAt[1]), 6);
            // Five degrees of longitude along: far enough from the
            // mark that no part of it reaches, and still well inside
            // the paper - a probe that lands off the page counts
            // zero and makes the ratio below pass for free, which is
            // how the first version of this check failed to bite.
            double[] bareLine = host.projection()
                    .toPage(Ecliptic.toEquatorial(5.0, 0.0))
                    .orElseThrow();
            int bareX = (int) Math.round(bareLine[0]);
            int bareY = (int) Math.round(bareLine[1]);
            assertTrue(bareX > 20 && bareX < 880
                            && bareY > 20 && bareY < 680,
                    "the bare-line probe is on the paper, at "
                            + bareX + "," + bareY);
            assertTrue(Math.hypot(bareX - equinoxAt[0],
                            bareY - equinoxAt[1]) > 60.0,
                    "and clear of the mark");
            int onBareLine = inkCount(released, shown, bareX, bareY, 6);
            assertTrue(onBareLine > 0,
                    "and the line really is drawn there: " + onBareLine
                            + " px");
            assertTrue(atTheMark > onBareLine * 2, "the March equinox"
                    + " is painted as a mark where the model puts it: "
                    + atTheMark + " px against " + onBareLine
                    + " on bare line");

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
            // An observer whose meridian actually crosses this page,
            // derived rather than hoped for: local sidereal time is
            // Greenwich apparent sidereal time plus east longitude,
            // so a longitude of minus GAST puts the observer's
            // meridian through right ascension 0h - the middle of
            // this page. Counting contributions without showing the
            // ink was the gap (PR #280 review).
            java.time.Instant equinoxEvening =
                    java.time.Instant.parse("2026-03-20T21:33:00Z");
            double gast = juranometria.sky.SkyFrame.gastDegrees(
                    juranometria.sky.SkyFrame.julianDate(equinoxEvening));
            double eastLongitude = -gast;
            while (eastLongitude < -180.0) {
                eastLongitude += 360.0;
            }
            MeridianModule meridian = host.attach(new MeridianModule(
                    new Observer(59.9, eastLongitude, equinoxEvening)));
            meridian.showing(true, true, true);
            BufferedImage withBoth = paint(chart);
            assertTrue(differences(shown, withBoth) > 100,
                    "4. the observer's own lines are drawn beside the"
                            + " ecliptic, not merely contributed: "
                            + differences(shown, withBoth) + " px more"
                            + " ink than the ecliptic alone");
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

            // The chart's own working selection, with something
            // really in it. A fresh disconnected one, already empty,
            // was the gap (PR #280 review).
            WorkingSelection working = host.workingSelection();
            String member = chart.currentScene().deepSkyObjects()
                    .get(0).id();
            working.add(member);
            assertEquals(List.of(member), working.members(),
                    "the reader marks something");
            assertTrue(ecliptic.showing(),
                    "the working selection is not the ecliptic's"
                            + " business");
            assertTrue(item.isSelected(), "and its tick is untouched");
            working.clear();
            assertEquals(List.of(), working.members(),
                    "and clearing it is not the ecliptic's business"
                            + " either");
            assertTrue(ecliptic.showing(),
                    "with the ecliptic still shown throughout");
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
            // Home is PRESSED, not assigned: controller.reset() is
            // exactly what the toolbar's Reset view runs, and an
            // assignment bypasses the reader's control and the
            // controller's own reset path (PR #280 re-review).
            assertNotEquals(ChartViewState.DEFAULT.centre(),
                    chart.viewState().centre(),
                    "7. the reader is away from Home before pressing"
                            + " it, so the press has somewhere to go");
            javax.swing.JButton resetView = null;
            for (java.awt.Component each : toolbar.getComponents()) {
                if (each instanceof javax.swing.JButton button
                        && "Reset view".equals(button
                                .getAccessibleContext()
                                .getAccessibleName())) {
                    resetView = button;
                }
            }
            assertTrue(resetView != null,
                    "the toolbar carries the Reset view control");
            assertTrue(resetView.isEnabled(),
                    "and it is reachable rather than greyed");
            // A real pointer press, through the shared route helper,
            // which proves the button is showing, has a size, and
            // that the point pressed is one a reader could reach
            // before it dispatches anything.
            ReaderInput.click(resetView);
            assertEquals(ChartViewState.DEFAULT, chart.viewState(),
                    "and pressing Reset view - the reader's own Home"
                            + " control - returns the released view"
                            + " state");

            ChartComponent neverHadAModule =
                    chart(ChartViewState.DEFAULT);
            assertEquals(0, differences(paint(chart),
                            paint(neverHadAModule)),
                    "and the chart this reader has been using all"
                            + " along is the page an atlas that never"
                            + " had the module draws");
            assertEquals(0, differences(home, paint(chart)),
                    "the same page it opened on, byte for byte");

            SwingUtilities.invokeAndWait(window[0]::dispose);
        });
    }
}
