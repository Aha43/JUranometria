package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.AppInfo;
import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.module.NavigationRequest;
import juranometria.page.PageContents;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartRenderer;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.placeandtime.PlaceAndTimeDialog;
import juranometria.ui.placeandtime.PlaceStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 25 journey: a reader and their own sky (issue #229).
 *
 * <p>One production-path walk through everything the sprint built,
 * using the application's own wiring - the real menu, the real
 * dialog, the real module host - because Sprint 24's closing journey
 * found defects in code that had already passed review as
 * components, and only a journey can find that kind.
 *
 * <p>Every claim is asked of the page or the model, never of the
 * test's own arithmetic: drawn ends are pushed back through the
 * chart's inverse and must sit on their circle, the zenith ring is
 * looked for where the model says overhead is, and "unchanged" is a
 * pixel count of zero.
 */
class SprintTwentyFiveJourneyTest {

    private static final Instant EQUINOX =
            Instant.parse("2026-03-20T21:33:00Z");

    private JFrame window;
    private ChartComponent chart;
    private ChartViewController navigation;
    private ChartModuleHost modules;
    private MeridianModule meridian;
    private PlaceStore placeStore;
    private final java.util.prefs.Preferences node =
            java.util.prefs.Preferences.userRoot().node(
                    "juranometria-sprint25-journey-" + System.nanoTime());
    private final List<NavigationRequest> requests = new ArrayList<>();

    @AfterEach
    void closeTheWindow() throws Exception {
        if (modules != null) {
            modules.detachAll();
            modules = null;
        }
        SwingUtilities.invokeAndWait(() -> {
            for (java.awt.Window open : java.awt.Window.getWindows()) {
                if (open.isDisplayable()) {
                    open.dispose();
                }
            }
        });
        window = null;
        node.removeNode();
    }

    /** The application's own wiring, in a window a reader could use. */
    private void openTheAtlas() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a reader's menus and dialogs need a display");
        placeStore = PlaceStore.forNode(node);

        SwingUtilities.invokeAndWait(() -> {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            chart.setViewState(ChartViewState.DEFAULT);
            SelectionModel selection = new SelectionModel();
            SelectInteraction.install(chart, selection);

            modules = new ChartModuleHost(chart, selection, request -> {
                requests.add(request);
                navigation.recenter(request.centre());
            });
            // The application's startup: the remembered place, the
            // clock read once, every switch off - the session begins
            // with the ordinary chart.
            meridian = modules.attach(new MeridianModule(
                    placeStore.load(EQUINOX)));
            meridian.showing(false, false, false);

            window = new JFrame(AppInfo.NAME + " " + AppInfo.version());
            window.setLayout(new BorderLayout());
            window.add(chart, BorderLayout.CENTER);
            window.setJMenuBar(AppMenuBar.create(navigation,
                    () -> { }, () -> { }, () -> { }, null,
                    () -> PlaceAndTimeDialog.open(window, meridian,
                            placeStore, () -> EQUINOX)));
            window.setSize(1100, 820);
            window.setVisible(true);
        });
        flush();
    }

    @Test
    void aReaderFindsTheirOwnSkyOnTheFixedChart() throws Exception {
        openTheAtlas();

        // The reviewed case: a place chosen so the meridian crosses
        // the released page and the zenith sits on clear paper.
        // Stated, not anyone's home.
        double latitude = 42.5;
        double longitude = -130.994;

        // 1. Open the module's controls from the View menu without
        // disturbing the chart: the page before and after is the
        // same page, to the pixel.
        BufferedImage released = paintChart();
        SwingUtilities.invokeAndWait(() ->
                menuItem("Place and Time...").doClick());
        flush();
        assertEquals(0, differing(released, paintChart()),
                "opening the controls draws nothing on the chart");
        PlaceAndTimeDialog dialog = theDialog();

        // 2. A reviewed place and instant through the real fields,
        // and the three geometries identified from the page itself.
        commit(dialog, "latitudeField", Double.toString(latitude));
        commit(dialog, "longitudeField", Double.toString(longitude));
        commit(dialog, "instantField", "2026-03-20 21:33:00");
        clickShow(dialog, "showMeridian", true);
        clickShow(dialog, "showMathematicalhorizon", true);
        clickShow(dialog, "showZenith", true);
        flush();

        LocalSky sky = new LocalSky(meridian.observer());
        BufferedImage inked = paintChart();
        assertTrue(differing(released, inked) > 100,
                "the reader's sky reached the page");
        // The zenith: ink where the model says overhead is.
        double[] zenithAt = pagePixel(sky.zenith());
        assertTrue(inkNear(inked, released, zenithAt, 10),
                "the zenith ring is drawn at the model's zenith");
        // The meridian: a changed pixel far from the zenith, pushed
        // back through the chart's own inverse, sits on the great
        // circle whose pole the model states.
        assertOnCircle(inked, released, sky.meridian().pole());
        // The horizon is ninety degrees from a zenith that is on
        // this page: honestly absent here, and found on its own page
        // in step 4.

        // 3. Longitude, latitude and time independently, each
        // changing only what astronomy says it changes - and none of
        // them touching the catalogue scene or moving the page.
        ChartScene scene = chart.currentScene();
        ChartViewState where = navigation.state();

        double lstBefore = new LocalSky(meridian.observer())
                .localSiderealTimeDegrees();
        commit(dialog, "longitudeField", Double.toString(longitude + 10));
        assertEquals(10.0, forward(lstBefore,
                        new LocalSky(meridian.observer())
                                .localSiderealTimeDegrees()), 1e-9,
                "ten degrees east is ten degrees later in the sidereal"
                        + " day, and nothing else moved");
        commit(dialog, "longitudeField", Double.toString(longitude));

        double decBefore = new LocalSky(meridian.observer())
                .zenith().decDegrees();
        commit(dialog, "latitudeField", Double.toString(latitude - 20));
        assertTrue(new LocalSky(meridian.observer()).zenith().decDegrees()
                        < decBefore,
                "twenty degrees south took the zenith south");
        commit(dialog, "latitudeField", Double.toString(latitude));

        lstBefore = new LocalSky(meridian.observer())
                .localSiderealTimeDegrees();
        commit(dialog, "instantField", "2026-03-20 22:33:00");
        assertEquals(15.041, forward(lstBefore,
                        new LocalSky(meridian.observer())
                                .localSiderealTimeDegrees()), 0.01,
                "an hour turned the sky a sidereal hour");
        commit(dialog, "instantField", "2026-03-20 21:33:00");

        assertSame(scene, chart.currentScene(),
                "none of it reassembled the catalogue scene");
        assertEquals(where, navigation.state(),
                "and none of it moved the page");

        // 4. Across the RA seam, to the pole, and to a southern
        // page: the lines land on their circles with no discontinuity
        // and no invented chord, and the horizon is found where it
        // really is.
        for (SkyPosition centre : List.of(
                new SkyPosition(359.9, 0.1),   // the seam
                new SkyPosition(0.0, 89.6),    // hard against the pole
                new SkyPosition(190.0, -41.0))) { // the southern sky
            SwingUtilities.invokeAndWait(() ->
                    navigation.recenter(centre));
            flush();
            checkEveryDrawnLine();
        }
        // The horizon itself: a page centred on it must show the
        // dashed boundary, and its ink must sit on the circle whose
        // pole is the zenith.
        SkyPosition onHorizon = new LocalSky(meridian.observer())
                .horizon().around(360).get(0);
        SwingUtilities.invokeAndWait(() -> navigation.recenter(onHorizon));
        flush();
        BufferedImage horizonPage = paintChart();
        BufferedImage bareHorizonPage = withModuleQuiet();
        assertTrue(differing(bareHorizonPage, horizonPage) > 50,
                "the mathematical horizon is drawn on its own page");
        assertOnCircle(horizonPage, bareHorizonPage,
                new LocalSky(meridian.observer()).horizon().pole());

        // 5. What the rest of the atlas holds stays coherent while
        // the module changes: the inventory is the page's, and
        // module state does not leak into it.
        PageContents inventory = modules.inventory();
        commit(dialog, "latitudeField", "10");
        commit(dialog, "latitudeField", Double.toString(latitude));
        assertSame(inventory, modules.inventory(),
                "changing the observer rebuilt no inventory: the"
                        + " page did not change");

        // 6. Centre on zenith, once, through its real control - and
        // after everything above, it is the first navigation any
        // module action has requested.
        assertEquals(0, requests.size(),
                "no module action so far has asked the chart to move");
        SwingUtilities.invokeAndWait(() ->
                ((javax.swing.AbstractButton) named(dialog,
                        "centreButton")).doClick());
        flush();
        assertEquals(1, requests.size(),
                "Center on zenith asked once");
        assertEquals(new LocalSky(meridian.observer()).zenith(),
                requests.get(0).centre(), "for the point overhead");
        assertTrue(navigation.state().centre().separationDegrees(
                        new LocalSky(meridian.observer()).zenith())
                        < 1e-9,
                "and the chart went there - through the controller,"
                        + " which was free to refuse");

        // 7. Home, disable, and the ordinary chart to the byte.
        // Home through the controller's own reset - a bare recenter
        // to the same coordinates is a different page, because the
        // released default carries its target's identity in the
        // title block, which this journey's first red run showed as
        // 1,168 differing pixels of title text.
        SwingUtilities.invokeAndWait(navigation::reset);
        flush();
        clickShow(dialog, "showMeridian", false);
        clickShow(dialog, "showMathematicalhorizon", false);
        clickShow(dialog, "showZenith", false);
        flush();
        assertEquals(0, differing(released, paintChart()),
                "switched off, the page is the released page exactly");

        // Detached entirely: still that page, and nothing left
        // behind.
        SwingUtilities.invokeAndWait(() -> modules.detachAll());
        modules = null;
        flush();
        assertEquals(0, differing(released, paintChart()),
                "detached, the page is still the released page");

        // 8. A restart: only the approved settings return. The place
        // was committed above and comes back; the instant is the new
        // session's own; the switches are off.
        placeStore.flush();
        Instant nextSession = Instant.parse("2026-09-05T19:00:00Z");
        Observer reborn = PlaceStore.forNode(node).load(nextSession);
        assertEquals(latitude, reborn.latitudeDegrees(),
                "the reader's latitude came back");
        assertEquals(longitude, reborn.eastLongitudeDegrees(),
                "and their longitude");
        assertEquals(nextSession, reborn.instant(),
                "the instant did not: it is the new session's own");
        MeridianModule fresh = new MeridianModule(reborn);
        fresh.showing(false, false, false);
        assertEquals(List.of(), fresh.contributedGeometry(),
                "and the session begins with the ordinary chart");
    }

    // ---- asking the page itself --------------------------------------

    /** Where the production seam clips this circle on this page. */
    private java.util.Optional<juranometria.project.GreatCirclePage.Arc>
            predictedArc(SkyPosition pole) {
        ChartScene scene = chart.currentScene();
        var paper = ChartRenderer.paperOf(scene);
        return juranometria.project.GreatCirclePage.clip(
                new juranometria.project.GnomonicProjection(
                        scene.viewport().centre()),
                new juranometria.project.ViewportMapping(scene.viewport()),
                new juranometria.project.GreatCirclePage.Page(
                        paper.getMinX(), paper.getMinY(),
                        paper.getMaxX(), paper.getMaxY()),
                pole);
    }

    private double degreesPerPixel() {
        return chart.currentScene().viewport().fieldWidthDegrees()
                / chart.currentScene().viewport().widthPx();
    }

    /**
     * The drawn line is the model's circle, asked two ways: ink runs
     * along the seam's predicted arc, and pixels on that arc, pushed
     * back through the chart's own inverse, sit ninety degrees from
     * the pole. The first would catch a line drawn elsewhere; the
     * second would catch a prediction and a drawing wrong together.
     */
    private void assertOnCircle(BufferedImage inked, BufferedImage bare,
                                SkyPosition pole) throws Exception {
        var arc = predictedArc(pole).orElseThrow(() -> new AssertionError(
                "the circle crosses this page"));
        ChartScene scene = chart.currentScene();
        int along = 0, checked = 0;
        for (int y = 0; y < inked.getHeight(); y++) {
            for (int x = 0; x < inked.getWidth(); x++) {
                if (inked.getRGB(x, y) == bare.getRGB(x, y)) {
                    continue;
                }
                double toArc = java.awt.geom.Line2D.ptSegDist(
                        arc.from().x(), arc.from().y(),
                        arc.to().x(), arc.to().y(),
                        x, y - pageOffset());
                if (toArc > 1.5) {
                    continue;
                }
                along++;
                if (toArc < 0.5 && checked < 20) {
                    SkyPosition under = ChartHitTest.skyAt(scene, x,
                            y - pageOffset());
                    if (under != null) {
                        double off = Math.abs(90.0
                                - pole.separationDegrees(under));
                        assertTrue(off < 3 * degreesPerPixel(),
                                "ink at " + x + "," + y + " is on the"
                                        + " circle under the chart's"
                                        + " own inverse: off by " + off
                                        + "°");
                        checked++;
                    }
                }
            }
        }
        assertTrue(along > 50,
                "the line is drawn along the model's own arc: "
                        + along + " pixels of it");
        assertTrue(checked > 5,
                "and its pixels were pushed back through the inverse: "
                        + checked);
    }

    /**
     * No invented chord: every changed pixel on this page belongs to
     * one of the three geometries or to a name beside one of them. A
     * chord bridging refused sky, or a line landing somewhere merely
     * plausible, produces ink far from all of them.
     */
    private void checkEveryDrawnLine() throws Exception {
        BufferedImage inked = paintChart();
        BufferedImage bare = withModuleQuiet();
        if (differing(bare, inked) == 0) {
            // Nothing crosses this page; silence is the decided
            // answer, not a failure.
            return;
        }
        LocalSky sky = new LocalSky(meridian.observer());
        var meridianArc = predictedArc(sky.meridian().pole());
        var horizonArc = predictedArc(sky.horizon().pole());
        double[] zenithAt = zenithPixelOrNull(sky);
        int inspected = 0;
        for (int y = 0; y < inked.getHeight(); y++) {
            for (int x = 0; x < inked.getWidth(); x++) {
                if (inked.getRGB(x, y) == bare.getRGB(x, y)) {
                    continue;
                }
                double px = x, py = y - pageOffset();
                double nearest = Double.MAX_VALUE;
                for (var arc : List.of(meridianArc, horizonArc)) {
                    if (arc.isPresent()) {
                        nearest = Math.min(nearest,
                                java.awt.geom.Line2D.ptSegDist(
                                        arc.get().from().x(),
                                        arc.get().from().y(),
                                        arc.get().to().x(),
                                        arc.get().to().y(), px, py));
                        // A name is drawn once where the line leaves
                        // the paper; ink near either end is its.
                        for (var end : List.of(arc.get().from(),
                                arc.get().to())) {
                            nearest = Math.min(nearest, Math.hypot(
                                    end.x() - px, end.y() - py));
                        }
                    }
                }
                if (zenithAt != null) {
                    nearest = Math.min(nearest, Math.hypot(
                            zenithAt[0] - px,
                            zenithAt[1] - pageOffset() - py));
                }
                assertTrue(nearest < 90.0,
                        "changed ink at " + x + "," + y + " is " + nearest
                                + " px from every geometry and every"
                                + " label anchor: an invented chord"
                                + " would land exactly here");
                inspected++;
            }
        }
        assertTrue(inspected > 0, "the changed page was inspected");
    }

    private double[] zenithPixelOrNull(LocalSky sky) {
        ChartScene scene = chart.currentScene();
        return new juranometria.project.GnomonicProjection(
                scene.viewport().centre()).project(sky.zenith())
                .map(plane -> {
                    var pixel = new juranometria.project.ViewportMapping(
                            scene.viewport()).toPixel(plane);
                    return new double[] {pixel.x(),
                            pixel.y() + pageOffset()};
                }).orElse(null);
    }

    // ---- the machinery ----------------------------------------------

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private BufferedImage paintChart() throws Exception {
        BufferedImage[] image = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            image[0] = new BufferedImage(chart.getWidth(),
                    chart.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = image[0].createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image[0];
    }

    /** The same page with the module contributing nothing. */
    private BufferedImage withModuleQuiet() throws Exception {
        boolean m = meridian.meridianShowing();
        boolean h = meridian.horizonShowing();
        boolean z = meridian.zenithShowing();
        SwingUtilities.invokeAndWait(() ->
                meridian.showing(false, false, false));
        BufferedImage bare = paintChart();
        SwingUtilities.invokeAndWait(() -> meridian.showing(m, h, z));
        return bare;
    }

    private static int differing(BufferedImage a, BufferedImage b) {
        assertEquals(a.getWidth(), b.getWidth());
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

    private static boolean inkNear(BufferedImage inked, BufferedImage bare,
                                   double[] at, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = (int) Math.round(at[0]) + dx;
                int y = (int) Math.round(at[1]) + dy;
                if (x >= 0 && y >= 0 && x < inked.getWidth()
                        && y < inked.getHeight()
                        && inked.getRGB(x, y) != bare.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private double[] pagePixel(SkyPosition position) {
        ChartScene scene = chart.currentScene();
        var plane = new juranometria.project.GnomonicProjection(
                scene.viewport().centre()).project(position).orElseThrow();
        var pixel = new juranometria.project.ViewportMapping(
                scene.viewport()).toPixel(plane);
        return new double[] {pixel.x(), pixel.y() + pageOffset()};
    }

    private int pageOffset() {
        return chart.pageOffsetY();
    }

    private static double forward(double from, double to) {
        double turned = (to - from) % 360.0;
        return turned < 0 ? turned + 360.0 : turned;
    }

    private PlaceAndTimeDialog theDialog() {
        for (java.awt.Window open : java.awt.Window.getWindows()) {
            if (open instanceof PlaceAndTimeDialog found
                    && found.isDisplayable()) {
                return found;
            }
        }
        throw new AssertionError("the dialog is open");
    }

    private javax.swing.JMenuItem menuItem(String text) {
        javax.swing.JMenuBar bar = window.getJMenuBar();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            var menu = bar.getMenu(i);
            for (int j = 0; j < menu.getItemCount(); j++) {
                var item = menu.getItem(j);
                if (item != null && text.equals(item.getText())) {
                    return item;
                }
            }
        }
        throw new AssertionError("the menu carries " + text);
    }

    private static java.awt.Component named(java.awt.Container root,
                                            String name) {
        for (java.awt.Component child : root.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof java.awt.Container inner) {
                java.awt.Component found = named(inner, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void commit(PlaceAndTimeDialog dialog, String field,
                        String text) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField entry = (JTextField) named(dialog, field);
            entry.setText(text);
            entry.postActionEvent();
        });
        flush();
    }

    private void clickShow(PlaceAndTimeDialog dialog, String name,
                           boolean wanted) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            javax.swing.JCheckBox box =
                    (javax.swing.JCheckBox) named(dialog, name);
            if (box.isSelected() != wanted) {
                box.doClick();
            }
        });
        flush();
    }
}
