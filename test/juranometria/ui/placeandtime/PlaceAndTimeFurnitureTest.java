package juranometria.ui.placeandtime;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.meridian.MeridianModule;
import juranometria.module.Furniture;
import juranometria.module.OverlayContribution;
import juranometria.render.ChartFurniture;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Place and Time's box on the chart, held to the rulings on #556 (Sprint
 * 51, issue #259; docs/decisions/chart-instant.md): when it is drawn,
 * what it says, where the chart puts it, what it displaces, what one
 * activation of its action reads, and that the page with no box is the
 * page as released. Headless: a production {@link ChartComponent} under
 * a production {@link ChartModuleHost}, painted into an image.
 *
 * <p>The instant and the place are a stated observer; the sidereal time
 * the box must show is asked of {@link LocalSky}, the atlas's own, and
 * never typed here.
 */
class PlaceAndTimeFurnitureTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    private static final Instant AUGUST = Instant.parse("2026-08-15T06:00:00Z");
    private static final Instant LATER = Instant.parse("2026-08-15T21:30:00Z");

    private static Observer oslo() {
        return new Observer(59.91, 10.75, AUGUST);
    }

    /** A clock that answers each read with the next instant, and counts. */
    private static final class CountingClock implements Supplier<Instant> {
        final AtomicInteger reads = new AtomicInteger();
        private final Instant answer;

        CountingClock(Instant answer) {
            this.answer = answer;
        }

        @Override
        public Instant get() {
            reads.incrementAndGet();
            return answer;
        }
    }

    /** The chart, its host, Place and Time, and its box - as the atlas composes them. */
    private static final class Rig {
        final ChartComponent chart;
        final ChartModuleHost host;
        final MeridianModule module;
        final CountingClock clock = new CountingClock(LATER);
        final boolean[] bodies = new boolean[1];

        Rig(int width, int height, ChartOptions options, InterfaceText said) {
            chart = new ChartComponent(Atlas.assembler(), PageText.in(said));
            chart.setSize(width, height);
            chart.setChartOptions(options);
            chart.setViewState(new ChartViewState(new SkyPosition(64.0, -28.0), 8.0, 8.0));
            host = new ChartModuleHost(chart, new SelectionModel(), request -> { });
            module = host.attach(new MeridianModule(oslo()));
            module.showing(false, false, false);
            PlaceAndTimeFurniture.offer(module, () -> bodies[0],
                    new UpdateToNow(module, clock), said);
        }

        Rig() {
            this(900, 700, ChartOptions.DEFAULTS, EN);
        }

        int[] paint() {
            BufferedImage image = new BufferedImage(chart.getWidth(), chart.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
            return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0,
                    image.getWidth());
        }

        ChartComponent.DrawnFurniture box() {
            paint();
            assertEquals(1, chart.renderedFurniture().size(),
                    "the page draws the one box: " + chart.renderedFurniture());
            return chart.renderedFurniture().get(0);
        }
    }

    /** The released options with the stellar-magnitude key on or off. */
    private static ChartOptions withKey(boolean key) {
        ChartOptions o = ChartOptions.DEFAULTS;
        return new ChartOptions(o.deepSkyObjects(), o.deepSkyLabels(),
                o.constellationFigures(), o.constellationBoundaries(),
                o.constellationNames(), o.starNames(), o.bayerLetters(),
                o.flamsteedNumbers(), o.equatorialGrid(), o.titleBlock(), key,
                o.galaxies(), o.openClusters(), o.globularClusters(), o.nebulae(),
                o.planetaryNebulae(), o.palette());
    }

    /** The same page with no module at all - the released page. */
    private static int[] releasedPage(int width, int height, ChartOptions options) {
        ChartComponent chart = new ChartComponent(Atlas.assembler(), PageText.in(EN));
        chart.setSize(width, height);
        chart.setChartOptions(options);
        chart.setViewState(new ChartViewState(new SkyPosition(64.0, -28.0), 8.0, 8.0));
        new ChartModuleHost(chart, new SelectionModel(), request -> { });
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            chart.paint(g);
        } finally {
            g.dispose();
        }
        return image.getRGB(0, 0, width, height, null, 0, width);
    }

    @Test
    void noLayerDrawnForTheInstantIsTheReleasedPageByteForByte() {
        Rig rig = new Rig();
        assertArrayEquals(releasedPage(900, 700, ChartOptions.DEFAULTS), rig.paint(),
                "with every observer line off and no body on, the box is not drawn and"
                        + " the page is the released page (ruling 1)");
        assertEquals(List.of(), rig.chart.renderedFurniture());
        assertFalse(rig.chart.getAccessibleContext().getAccessibleDescription()
                .contains("Place and Time"), "and nothing of it is spoken");
        assertNull(rig.chart.getAccessibleContext().getAccessibleAction(),
                "and the chart offers no action");
    }

    @Test
    void anyLayerDrawnForTheInstantDrawsTheBoxWhereverItsInkFalls() {
        Rig rig = new Rig();
        rig.module.showing(true, false, false);
        assertEquals(PlaceAndTimeFurniture.IDENTITY,
                rig.box().owned().furniture().identity(), "the meridian on: the box");
        rig.module.showing(false, false, false);
        rig.module.ticksShowing(true);
        rig.paint();
        assertEquals(List.of(), rig.chart.renderedFurniture(),
                "ticks ride the meridian: with the line off they draw nothing, nor the box");
        rig.module.ticksShowing(false);
        for (boolean[] lines : new boolean[][] {{false, true, false}, {false, false, true}}) {
            rig.module.showing(lines[0], lines[1], lines[2]);
            rig.box();
        }
        rig.module.showing(false, false, false);
        rig.bodies[0] = true;
        rig.chart.repaint();
        rig.box();
        // A body switched on is a layer drawn for the instant whether or not
        // it stands on this page: the box does not come and go with panning.
        rig.chart.setViewState(new ChartViewState(new SkyPosition(200.0, 40.0), 8.0, 8.0));
        rig.box();
    }

    @Test
    void theBoxSaysThePlaceTheInstantAndTheSiderealTimeAndNoCivilTime() {
        Rig rig = new Rig();
        rig.module.showing(true, true, true);
        Furniture box = rig.box().owned().furniture();
        double lst = new LocalSky(oslo()).localSiderealTimeDegrees() / 15.0;
        int minutes = (int) Math.floor(lst * 60.0) % (24 * 60);
        String sidereal = String.format(java.util.Locale.ROOT, "LST %02dh %02dm",
                minutes / 60, minutes % 60);
        assertEquals("Place and Time", box.heading());
        assertEquals(List.of("59.91° N · 10.75° E", "2026-08-15 06:00:00 UTC", sidereal),
                box.lines(), "ruling 2: the observer to two decimals with hemisphere"
                        + " letters, UTC to the second, the sidereal time to the minute");
        assertEquals("Update to now", box.action().label());
        assertEquals("The chart is frozen at this instant.", box.action().paperLine());
        assertEquals("Place and Time: 59.91° N, 10.75° E, 2026-08-15 06:00:00 UTC, local"
                + " sidereal time " + sidereal.substring(4) + ".", box.spoken());
        assertTrue(rig.chart.getAccessibleContext().getAccessibleDescription()
                .endsWith(box.spoken()), "the box is spoken with the page");

        Furniture norsk = PlaceAndTimeFurniture.box(oslo(), NB);
        assertEquals("Sted og tid", norsk.heading());
        assertEquals("59.91° N · 10.75° Ø", norsk.lines().get(0));
        assertTrue(norsk.lines().get(2).startsWith("Stjernetid "));
        assertEquals("Oppdater til nå", norsk.action().label());

        Furniture west = PlaceAndTimeFurniture.box(
                new Observer(-33.87, 360.0 - 70.65, AUGUST), EN);
        assertEquals("33.87° S · 70.65° W", west.lines().get(0),
                "south and west, the longitude folded");

        for (Furniture said : List.of(box, norsk, west)) {
            List<String> all = new ArrayList<>(said.lines());
            all.add(said.spoken());
            all.add(said.described());
            all.add(said.action().explanation());
            for (String text : all) {
                String lower = text.toLowerCase(java.util.Locale.ROOT);
                for (String civil : List.of("zone", "sone", "cet", "cest", "gmt", "utc+",
                        "utc-", "offset", "summer", "sommer", "europe/", "civil",
                        "local time", "lokal tid")) {
                    assertFalse(lower.contains(civil), "no civil time, zone or offset"
                            + " from longitude (ruling 2): \"" + text + "\"");
                }
            }
        }
    }

    @Test
    void theChartAllocatesTheCornerAndRefusesWhereNoneFits() {
        Rig wide = new Rig();
        wide.module.showing(true, false, false);
        assertEquals(ChartFurniture.Corner.LOWER_RIGHT, wide.box().placed().corner(),
                "the lower right first");

        Rig narrow = new Rig(520, 700, ChartOptions.DEFAULTS, EN);
        narrow.module.showing(true, false, false);
        assertEquals(ChartFurniture.Corner.UPPER_RIGHT, narrow.box().placed().corner(),
                "a narrow page's lower right meets the title block: the upper right");

        Rig keyed = new Rig(520, 700, withKey(true), EN);
        keyed.module.showing(true, false, false);
        assertEquals(ChartFurniture.Corner.UPPER_LEFT, keyed.box().placed().corner(),
                "and with the magnitude key holding the upper right: the upper left");

        Rig tiny = new Rig(160, 140, ChartOptions.DEFAULTS, EN);
        tiny.module.showing(true, false, false);
        tiny.paint();
        assertEquals(List.of(), tiny.chart.renderedFurniture(),
                "a page no corner can hold refuses the box, as it refuses the title block");
        assertFalse(tiny.chart.getAccessibleContext().getAccessibleDescription()
                .contains("Place and Time"), "and a refused box is not spoken");
    }

    @Test
    void theBoxMeetsNoFurnitureAndEveryFigureAndNameYieldsToIt() {
        for (int[] size : new int[][] {{900, 700}, {520, 700}, {600, 1900}}) {
            for (boolean key : new boolean[] {false, true}) {
                ChartOptions options = withKey(key);
                Rig rig = new Rig(size[0], size[1], options, EN);
                rig.module.showing(true, true, true);
                rig.paint();
                if (rig.chart.renderedFurniture().isEmpty()) {
                    continue;
                }
                ChartFurniture.Placed placed = rig.chart.renderedFurniture().get(0).placed();
                ChartScene scene = rig.chart.currentScene();
                ChartRenderer renderer = new ChartRenderer(
                        juranometria.chart.StarSizePolicy.DEFAULT, PageText.in(EN));
                renderer.setFurniture(() -> List.of(placed.furniture()));
                java.awt.FontMetrics metrics = ChartRenderer.TextMetrics.offscreen().labels();
                ChartOptions drawn = rig.chart.drawnOptions();
                String page = size[0] + "x" + size[1] + (key ? " with the key" : "");
                Rectangle title = renderer.titleBlockBounds(metrics, scene);
                assertFalse(title != null && title.intersects(placed.covers()),
                        page + ": the box meets no title block");
                Rectangle magnitude = key ? renderer.magnitudeKeyBounds(metrics, scene) : null;
                assertFalse(magnitude != null && new Rectangle(magnitude.x, magnitude.y,
                                magnitude.width + 1, magnitude.height + 1)
                                .intersects(placed.covers()),
                        page + ": and no magnitude key");
                for (ChartRenderer.GridLabelPlacement grid
                        : renderer.gridLabelPlacements(metrics, scene, drawn)) {
                    assertFalse(grid.box().intersects(placed.covers()),
                            page + ": the grid figure " + grid.text() + " yields to the box");
                }
                for (var text : renderer.textPlacements(
                        ChartRenderer.TextMetrics.offscreen(), scene, drawn)) {
                    if (text.at() == null || !text.at().intersects(placed.covers())) {
                        continue;
                    }
                    // Only a name every candidate of which was refused may
                    // stand at its least bad place under the box - as under
                    // the title block - and the box, opaque and drawn last,
                    // covers it; the box must be among what refused it.
                    assertTrue(text.underDuress() && text.refusals().stream()
                                    .anyMatch(r -> r.kind()
                                            == juranometria.render.LabelPlacement.Refusal.FURNITURE
                                            && r.by().equals(placed.furniture().key())),
                            page + ": the name " + text.request().text() + " yields to it");
                }
            }
        }
    }

    @Test
    void oneActivationReadsTheClockOnceAndRedrawsOnlyThePlaceAndTimeLines()
            throws Exception {
        Rig rig = new Rig();
        rig.module.showing(true, true, true);
        ChartComponent.DrawnFurniture box = rig.box();
        ChartScene scene = rig.chart.currentScene();
        ChartViewState view = rig.chart.viewState();
        ChartOptions options = rig.chart.chartOptions();
        var inventory = rig.host.inventory();
        javax.accessibility.AccessibleAction action =
                rig.chart.getAccessibleContext().getAccessibleAction();
        assertNotNull(action, "the box's action is an accessible action of the chart");
        assertEquals(1, action.getAccessibleActionCount());
        assertEquals("Update to now", action.getAccessibleActionDescription(0));

        assertTrue(action.doAccessibleAction(0));

        assertEquals(1, rig.clock.reads.get(), "one activation, one clock read");
        assertEquals(LATER, rig.module.observer().instant(), "the instant it read");
        assertEquals(oslo().latitudeDegrees(), rig.module.observer().latitudeDegrees());
        assertEquals(oslo().eastLongitudeDegrees(),
                rig.module.observer().eastLongitudeDegrees(), "the place is kept");
        SkyPosition expected = new LocalSky(rig.module.observer()).meridian().pole();
        SkyPosition drawn = rig.chart.overlays().collect().stream()
                .map(owned -> owned.geometry())
                .filter(g -> g instanceof OverlayContribution.GreatCircle circle
                        && circle.identity().equals("meridian"))
                .map(g -> ((OverlayContribution.GreatCircle) g).pole())
                .findFirst().orElseThrow();
        assertEquals(expected, drawn, "the meridian moved to the circle of the new instant");
        assertSame(scene, rig.chart.currentScene(),
                "repaint only: the same scene, so no catalogue was queried");
        assertSame(inventory, rig.host.inventory(), "and the same inventory");
        assertEquals(view, rig.chart.viewState(), "no navigation");
        assertEquals(options, rig.chart.chartOptions(), "no option touched");
        assertEquals("2026-08-15 21:30:00 UTC", rig.box().owned().furniture().lines().get(1),
                "and the box states the instant now drawn");
        assertSame(box.owned().actions(), rig.box().owned().actions());
    }

    @Test
    void holdingTheClockAndWaitingChangesNothingAndAsksForNoRepaint() throws Exception {
        List<JComponent> asked = new ArrayList<>();
        RepaintManager recording = new RepaintManager() {
            @Override
            public synchronized void addDirtyRegion(JComponent component, int x, int y,
                                                    int width, int height) {
                asked.add(component);
                super.addDirtyRegion(component, x, y, width, height);
            }
        };
        juranometria.app.SwingSession.restoringRepaintManager(() -> {
            RepaintManager.setCurrentManager(recording);
            Rig rig = new Rig();
            rig.module.showing(true, false, false);
            rig.box();
            AtomicReference<Observer> before = new AtomicReference<>(rig.module.observer());
            for (int i = 0; i < 3; i++) {
                SwingUtilities.invokeAndWait(() -> { });
            }
            asked.clear();
            Thread.sleep(1500);
            for (int i = 0; i < 3; i++) {
                SwingUtilities.invokeAndWait(() -> { });
            }
            assertEquals(0, rig.clock.reads.get(), "no read without a reader asking");
            assertEquals(before.get(), rig.module.observer(), "the instant stayed frozen");
            assertFalse(asked.contains(rig.chart),
                    "and nothing asked the chart to repaint: no timer, no polling");
        });
    }

    @Test
    void detachingTheModuleRemovesTheBoxAndRecoversThePage() {
        Rig rig = new Rig();
        rig.module.showing(true, true, true);
        rig.box();
        rig.host.detachAll();
        assertEquals(List.of(), rig.chart.furniture().collect(),
                "detached, the module offers nothing");
        assertArrayEquals(releasedPage(900, 700, ChartOptions.DEFAULTS), rig.paint(),
                "and the page is the released page, byte for byte");
        assertNull(rig.chart.getAccessibleContext().getAccessibleAction());
    }

    @Test
    void aModulesFurnitureCarriesTextAndAnActionNameAndNothingThatDraws() {
        for (Class<?> type : List.of(Furniture.class, Furniture.Action.class)) {
            for (var component : type.getRecordComponents()) {
                Class<?> carried = component.getType();
                assertTrue(carried == String.class || carried == List.class
                                || carried == Furniture.Action.class,
                        type.getSimpleName() + "." + component.getName()
                                + " carries " + carried.getName()
                                + ": furniture is meaning and text, never a graphics"
                                + " context, a component, a colour or a rectangle");
            }
        }
    }

    @Test
    void paperCarriesNoControlAndSaysTheChartIsFrozenAndTheSheetSaysWhen() {
        Rig rig = new Rig();
        rig.module.showing(true, false, false);
        List<juranometria.sheet.ChartSheet.Furnished> paper = rig.chart.paperFurniture();
        assertEquals(1, paper.size());
        ChartFurniture onPaper = paper.get(0).box();
        assertNull(onPaper.action(), "no control on paper: it cannot be pressed");
        assertEquals("The chart is frozen at this instant.",
                onPaper.lines().get(onPaper.lines().size() - 1));
        assertEquals("The chart is drawn for 59.91° N, 10.75° E at 2026-08-15 06:00:00"
                + " UTC, and is frozen at that instant.", paper.get(0).described());

        juranometria.sheet.SheetRecording sheet = juranometria.sheet.ChartSheet.record(
                Atlas.assembler()::assemble, rig.chart.viewState(), ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, juranometria.sheet.PaperSize.A4,
                PageText.in(EN), java.util.Set.of(), paper);
        assertTrue(sheet.metadata().description().endsWith(paper.get(0).described()),
                "the sheet's own description gains the sentence: "
                        + sheet.metadata().description());
        juranometria.sheet.SheetRecording plain = juranometria.sheet.ChartSheet.record(
                Atlas.assembler()::assemble, rig.chart.viewState(), ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, juranometria.sheet.PaperSize.A4,
                PageText.in(EN), java.util.Set.of());
        assertEquals(plain.metadata().description() + " " + paper.get(0).described(),
                sheet.metadata().description(), "and nothing else changes in it");
    }

    @Test
    void theStartUpInstantIsTheSessionsOwnAndThePlaceComesBack() throws Exception {
        java.util.prefs.Preferences node = java.util.prefs.Preferences.userRoot()
                .node("juranometria-259-restart-" + System.nanoTime());
        try {
            PlaceStore store = PlaceStore.forNode(node);
            store.save(59.91, 10.75);
            ChartComponent chart = new ChartComponent(Atlas.assembler(), PageText.in(EN));
            chart.setSize(900, 700);
            chart.setViewState(ChartViewState.DEFAULT);
            ChartModuleHost first = new ChartModuleHost(chart, new SelectionModel(), r -> { });
            MeridianModule session = PlaceAndTimeSession.begin(first, store, AUGUST);
            new UpdateToNow(session, () -> LATER).run();
            assertEquals(LATER, session.observer().instant());
            first.detachAll();

            Instant restart = Instant.parse("2026-08-16T08:00:00Z");
            ChartModuleHost second = new ChartModuleHost(chart, new SelectionModel(), r -> { });
            MeridianModule next = PlaceAndTimeSession.begin(second, store, restart);
            assertEquals(restart, next.observer().instant(),
                    "a restart's instant is its own start-up read, never a stored one");
            assertEquals(59.91, next.observer().latitudeDegrees());
            assertEquals(10.75, next.observer().eastLongitudeDegrees(), "the place returns");
        } finally {
            node.removeNode();
        }
    }
}
