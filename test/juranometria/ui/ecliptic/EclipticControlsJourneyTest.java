package juranometria.ui.ecliptic;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.app.ChartOptionsControls;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsStore;
import juranometria.app.Atlas;
import juranometria.app.SwingSession;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.sky.Ecliptic;
import juranometria.sky.Observer;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader's route to the ecliptic (Sprint 28, issue #274).
 *
 * <p>Through the real menu bar the application builds, the real
 * module, the real overlay registry and the real painter. Every claim
 * is asked of what the reader can reach and of what lands on the
 * page.
 */
class EclipticControlsJourneyTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final juranometria.render.ChartRenderer RENDERER =
            new juranometria.render.ChartRenderer(
                    juranometria.chart.StarSizePolicy.DEFAULT, ENGLISH);

    /** The March equinox page: the ecliptic crosses the equator here. */
    private static ChartViewState eclipticPage() {
        return new ChartViewState(new SkyPosition(0.0, 0.0), 24.0, 8.0);
    }

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

    /** A chart with a host, sized and on the ecliptic page. */
    private static ChartComponent chartOn(ChartViewState state,
                                          ChartComponent[] out)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            out[0] = new ChartComponent(Atlas.assembler(), ENGLISH);
            out[0].setSize(900, 700);
            out[0].setViewState(state);
        });
        SwingUtilities.invokeAndWait(() -> { });
        return out[0];
    }

    // ---- the control exists only when the module does ----------------

    /** Chart Options' controls over a scratch options store, with or without the ecliptic's switch. */
    private static ChartOptionsControls controls(EclipticSession.Switch ecliptic) {
        // An in-memory options store: no preference node of the reader's.
        return new ChartOptionsControls(new ChartOptionsController(new ChartOptionsStore() {
            private juranometria.render.ChartOptions kept = juranometria.render.ChartOptions.DEFAULTS;
            @Override
            public juranometria.render.ChartOptions load() {
                return kept;
            }
            @Override
            public void save(juranometria.render.ChartOptions options) {
                kept = options;
            }
        }), () -> false, ecliptic,
                juranometria.ui.language.InterfaceText.forLanguage("en"));
    }

    @Test
    void withNoEclipticModuleThereIsNoEclipticControl() {
        assertNull(controls(null).eclipticBox(),
                "an atlas without the module offers no switch for it:"
                        + " a tick for something that cannot be drawn"
                        + " would be a promise the atlas cannot keep");
        JMenuBar bar = AppMenuBar.create(null, null, () -> { },
                () -> { }, () -> { }, () -> { }, juranometria.ui.language.InterfaceText.forLanguage("en"));
        assertNotNull(AppMenuBar.inspectorItem(bar),
                "while the controls that do belong to it are there,"
                        + " so the absence above is an answer");
    }

    @Test
    void theControlIsABoxInChartOptionsChartChecklistAfterTheGrid() {
        ChartOptionsControls controls = controls(EclipticSession.of(
                new EclipticModule(), EclipticStore.inMemory()));
        JCheckBox item = controls.eclipticBox();
        assertNotNull(item, "the ecliptic has a switch");
        assertEquals("Ecliptic", item.getText(),
                "named in full, so nothing depends on a glyph, a"
                        + " zodiac sign or a colour to say what it is");
        assertEquals("Ecliptic",
                item.getAccessibleContext().getAccessibleName(),
                "and a reader who cannot see it is told the same word");
        assertTrue(item.getAccessibleContext()
                        .getAccessibleDescription().contains("equinox"),
                "with a description that names what it draws: "
                        + item.getAccessibleContext()
                                .getAccessibleDescription());

        // In the Chart checklist (#675, ruled on #674), directly after
        // the equatorial grid: the two reference lines together.
        ChartOptionsControls.Subject chart = controls.subjects().stream()
                .filter(subject -> subject.id().equals("chart")).findFirst()
                .orElseThrow();
        java.util.List<String> boxes = new java.util.ArrayList<>();
        for (java.awt.Component each : chart.column().getComponents()) {
            if (each instanceof JCheckBox box) {
                boxes.add(box.getText());
            }
        }
        assertEquals(java.util.List.of("Equatorial coordinate grid", "Ecliptic",
                        "Title block", "Stellar-magnitude key", "Black sky"), boxes,
                "the Chart checklist, with the ecliptic after the grid");
    }

    // ---- the routes a reader has --------------------------------------

    @Test
    void pointerAndKeyboardBothReachIt() {
        int[] toggles = {0};
        EclipticModule module = new EclipticModule();
        EclipticStore counting = new EclipticStore() {
            private java.util.Optional<Boolean> kept = java.util.Optional.empty();
            @Override
            public java.util.Optional<Boolean> shown() {
                return kept;
            }
            @Override
            public void save(boolean shown) {
                kept = java.util.Optional.of(shown);
                toggles[0]++;
            }
            @Override
            public void flush() {
            }
        };
        JCheckBox item = controls(EclipticSession.of(module, counting)).eclipticBox();

        // The pointer route: what a click on the box does.
        item.doClick();
        assertEquals(1, toggles[0], "clicking the box toggles it");
        assertTrue(module.showing(), "and the module shows");

        // The keyboard route: the box carries a mnemonic, so the
        // checklist can be walked to it without a pointer at all.
        assertEquals('I', (char) item.getMnemonic(),
                "and it has a mnemonic, so the checklist is walkable");
        assertTrue(item.isEnabled(),
                "and the box is reachable rather than greyed");

        // Firing the action the keyboard would fire.
        for (java.awt.event.ActionListener listener
                : item.getActionListeners()) {
            listener.actionPerformed(new java.awt.event.ActionEvent(
                    item, java.awt.event.ActionEvent.ACTION_PERFORMED,
                    "Ecliptic"));
        }
        assertEquals(2, toggles[0],
                "and the keyboard reaches the same one action, not a"
                        + " second path with its own behaviour");
    }

    // ---- the wiring a reader actually sets off ------------------------

    @Test
    void theBoxDrivesTheModuleAndRemembersTheChoice()
            throws Exception {
        // The chain, end to end, through the production controls and
        // the production switch. Rehearsing the box, the module and
        // the store separately proves each of them and none of the
        // wiring between them (PR #279 review): a box bound to
        // nothing, or a switch that forgot to save, would pass all
        // three.
        SwingSession.scratchPreferences("ecliptic-wiring", node -> {
            EclipticStore store = EclipticStore.forNode(node);
            ChartComponent[] holder = new ChartComponent[1];
            ChartComponent chart = chartOn(eclipticPage(), holder);
            ChartModuleHost host = new ChartModuleHost(chart,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            EclipticModule module = EclipticSession.begin(host);
            BufferedImage hidden = paint(chart);

            EclipticSession.Switch eclipticSwitch = EclipticSession.of(module, store);
            JCheckBox item = controls(eclipticSwitch).eclipticBox();
            // Production's own restore, not a rehearsal of it: one
            // call sets both the chart and the tick from one read of
            // the store.
            eclipticSwitch.restore();
            assertFalse(item.isSelected(),
                    "the box starts out showing the released"
                            + " default, which is hidden");
            assertFalse(module.showing(),
                    "and so does the chart");

            item.doClick();
            assertTrue(module.showing(),
                    "the box drives the module");
            assertEquals(java.util.Optional.of(Boolean.TRUE),
                    store.shown(),
                    "and the same click remembers the choice");
            assertTrue(item.isSelected(),
                    "and the tick shows what is on the chart");
            assertTrue(differences(hidden, paint(chart)) > 100,
                    "and the ecliptic is on the page");

            item.doClick();
            assertFalse(module.showing(), "clicking again hides it");
            assertEquals(java.util.Optional.of(Boolean.FALSE),
                    store.shown(),
                    "and remembers that too - hiding it is a choice,"
                            + " not a return to never having chosen");
            assertFalse(item.isSelected(), "and the tick clears");
            assertEquals(0, differences(hidden, paint(chart)),
                    "and the page is the page it started as");
        });
    }

    @Test
    void theSwitchMovesNothingButItsOwnLayer() throws Exception {
        SwingSession.scratchPreferences("ecliptic-quiet", node -> {
            ChartComponent[] holder = new ChartComponent[1];
            ChartComponent chart = chartOn(eclipticPage(), holder);
            List<juranometria.module.NavigationRequest> asked =
                    new java.util.ArrayList<>();
            ChartModuleHost host = new ChartModuleHost(chart,
                    new juranometria.chart.SelectionModel(), asked::add);
            EclipticStore store = EclipticStore.forNode(node);
            EclipticModule module = EclipticSession.begin(host);
            EclipticSession.Switch eclipticSwitch = EclipticSession.of(module, store);
            controls(eclipticSwitch);
            eclipticSwitch.restore();

            ChartViewState where = chart.viewState();
            ChartOptions options = chart.chartOptions();
            eclipticSwitch.toggle();

            assertEquals(where, chart.viewState(),
                    "showing the ecliptic leaves the page where the"
                            + " reader put it");
            assertEquals(options, chart.chartOptions(),
                    "and changes no chart option");
            assertEquals(List.of(), asked,
                    "and asks the chart to move nowhere");
        });
    }

    @Test
    void aLoadedModuleWithoutAControlIsRefused() throws Exception {
        // The pairing that must not exist: a module that can draw,
        // a remembered choice that says draw it, and nothing a
        // reader could use to stop it (PR #279 round 3; the rule
        // carried from the menu item to the switch in #675).
        SwingSession.scratchPreferences("ecliptic-no-control", node -> {
            EclipticStore store = EclipticStore.forNode(node);
            store.save(true);
            ChartComponent[] holder = new ChartComponent[1];
            ChartComponent chart = chartOn(eclipticPage(), holder);
            ChartModuleHost host = new ChartModuleHost(chart,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            EclipticModule module = EclipticSession.begin(host);
            EclipticSession.Switch nobodyFollows = EclipticSession.of(module, store);

            assertThrows(IllegalStateException.class, nobodyFollows::restore,
                    "restoring a loaded module without a control is a"
                            + " miswiring, not a configuration");
            assertFalse(module.showing(),
                    "and it is refused before anything is drawn, so"
                            + " the refusal leaves no half-restored"
                            + " chart behind");

            // Controls built without the switch carry no box, and give
            // the switch nothing to follow: refused the same way rather
            // than silently drawing.
            assertNull(controls(null).eclipticBox(),
                    "controls built without the switch carry no box");
            assertEquals(0, nobodyFollows.followers(),
                    "and the switch still has nothing following it");
            assertThrows(IllegalStateException.class, nobodyFollows::restore,
                    "and restoring is still refused");

            // With the box following, the same restore draws the choice.
            JCheckBox item = controls(nobodyFollows).eclipticBox();
            nobodyFollows.restore();
            assertTrue(module.showing() && item.isSelected(),
                    "with a control, the remembered choice is drawn and shown");
        });
    }

    // ---- what the switch does -----------------------------------------

    @Test
    void theSwitchDrawsAndUndrawsTheEclipticAndNothingElse()
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        ChartComponent chart = chartOn(eclipticPage(), holder);
        ChartModuleHost host = new ChartModuleHost(chart,
                new juranometria.chart.SelectionModel(), request -> { });
        BufferedImage before = paint(chart);

        EclipticModule module = host.attach(new EclipticModule());
        assertFalse(module.showing(), "hidden to begin with");
        assertEquals(0, differences(before, paint(chart)),
                "so attaching the module leaves the page it found");

        module.showing(true);
        BufferedImage shown = paint(chart);
        assertTrue(differences(before, shown) > 100,
                "the switch draws the ecliptic on a page it crosses");

        module.showing(false);
        assertEquals(0, differences(before, paint(chart)),
                "and turning it off gives back the same page, byte"
                        + " for byte");

        module.showing(true);
        module.detach();
        assertEquals(0, differences(before, paint(chart)),
                "and detaching the module recovers the module-free"
                        + " chart, byte for byte, with it still on");
    }

    @Test
    void itIsDrawnOnEveryPageTheGateNamedAndSilentWhereItShouldBe()
            throws Exception {
        record Page(String what, ChartViewState state, boolean crosses) {
        }
        List<Page> pages = List.of(
                new Page("the March equinox",
                        new ChartViewState(new SkyPosition(0.0, 0.0),
                                24.0, 8.0), true),
                new Page("the September equinox",
                        new ChartViewState(new SkyPosition(180.0, 0.0),
                                24.0, 8.0), true),
                new Page("the June solstice",
                        new ChartViewState(new SkyPosition(90.0,
                                Ecliptic.OBLIQUITY_DEGREES), 24.0, 8.0),
                        true),
                new Page("the December solstice",
                        new ChartViewState(new SkyPosition(270.0,
                                -Ecliptic.OBLIQUITY_DEGREES), 24.0, 8.0),
                        true),
                new Page("the right-ascension wrap",
                        new ChartViewState(new SkyPosition(359.0, 0.0),
                                18.0, 8.0), true),
                new Page("a dense Milky Way field",
                        new ChartViewState(new SkyPosition(270.0,
                                -Ecliptic.OBLIQUITY_DEGREES), 8.0, 8.0),
                        true),
                new Page("the celestial pole",
                        new ChartViewState(new SkyPosition(0.0, 88.0),
                                8.0, 8.0), false));

        for (Page page : pages) {
            ChartComponent[] holder = new ChartComponent[1];
            ChartComponent chart = chartOn(page.state(), holder);
            ChartModuleHost host = new ChartModuleHost(chart,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            BufferedImage before = paint(chart);
            EclipticModule module = host.attach(new EclipticModule());
            module.showing(true);
            int ink = differences(before, paint(chart));

            if (page.crosses()) {
                assertTrue(ink > 50, "the ecliptic is drawn on "
                        + page.what() + ": " + ink + " pixels");
            } else {
                assertEquals(0, ink, "and on " + page.what()
                        + " it is drawn as nothing, because it does"
                        + " not cross that sky - off the page is"
                        + " silence, not a line along an edge");
            }
        }
    }

    // ---- beside the other modules --------------------------------------

    @Test
    void changingOneModuleChangesOnlyItsOwnPresentation()
            throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        ChartComponent chart = chartOn(eclipticPage(), holder);
        ChartModuleHost host = new ChartModuleHost(chart,
                new juranometria.chart.SelectionModel(), request -> { });

        MeridianModule meridian = host.attach(new MeridianModule(
                new Observer(59.9, 10.7,
                        java.time.Instant.parse("2026-03-20T21:33:00Z"))));
        meridian.showing(false, false, false);
        EclipticModule ecliptic = host.attach(new EclipticModule());

        BufferedImage neither = paint(chart);
        ecliptic.showing(true);
        BufferedImage eclipticOnly = paint(chart);
        assertTrue(differences(neither, eclipticOnly) > 0,
                "the ecliptic draws");

        // The meridian's switches move the meridian's ink and leave
        // the ecliptic's where it is.
        meridian.showing(true, true, true);
        BufferedImage both = paint(chart);
        ecliptic.showing(false);
        BufferedImage meridianOnly = paint(chart);
        ecliptic.showing(true);

        // An earlier version asserted differences(...) >= 0 here,
        // which cannot fail (PR #279 review). What is worth
        // asserting is that neither answers for the other: with both
        // on, removing the ecliptic changes the page, and removing
        // the meridian gives back exactly the ecliptic's own.
        assertTrue(differences(both, meridianOnly) > 0,
                "with both showing, the ecliptic is contributing ink"
                        + " of its own");
        meridian.showing(false, false, false);
        assertEquals(0, differences(eclipticOnly, paint(chart)),
                "and turning the other module off and on again"
                        + " returns the ecliptic's page unchanged,"
                        + " byte for byte");

        // The registry keeps them apart by name.
        List<String> owners = new java.util.ArrayList<>();
        for (OverlayRegistry.Owned owned : chart.overlays().collect()) {
            owners.add(owned.moduleId());
        }
        assertTrue(owners.contains(EclipticModule.ID),
                "the ecliptic's ink is its own");
        ecliptic.detach();
        for (OverlayRegistry.Owned owned : chart.overlays().collect()) {
            assertFalse(EclipticModule.ID.equals(owned.moduleId()),
                    "and detaching it removes only its own");
        }
    }

    @Test
    void thePaletteAndTheChartOptionsAreNotItsBusiness() throws Exception {
        ChartComponent[] holder = new ChartComponent[1];
        ChartComponent chart = chartOn(eclipticPage(), holder);
        ChartModuleHost host = new ChartModuleHost(chart,
                new juranometria.chart.SelectionModel(), request -> { });
        EclipticModule module = host.attach(new EclipticModule());
        module.showing(true);
        BufferedImage onPaper = paint(chart);

        SwingUtilities.invokeAndWait(() -> chart.setChartOptions(
                ChartOptions.DEFAULTS.withPalette(ChartPalette.BLACK_SKY)));
        BufferedImage onBlack = paint(chart);
        assertTrue(differences(onPaper, onBlack) > 0,
                "the palette changes the page");
        assertTrue(module.showing(),
                "and changes nothing about the module's own state:"
                        + " the ecliptic is still shown");

        SwingUtilities.invokeAndWait(() -> chart.setChartOptions(
                ChartOptions.DEFAULTS));
        assertEquals(0, differences(onPaper, paint(chart)),
                "and changing it back gives the same page");
    }

    // ---- across a restart ----------------------------------------------

    @Test
    void aSecondSessionDrawsWhatTheFirstOneChose() throws Exception {
        SwingSession.scratchPreferences("ecliptic-restart", node -> {
            EclipticStore store = EclipticStore.forNode(node);

            // First session: the reader turns it on, the way the
            // Chart Options box does (#675).
            ChartComponent[] first = new ChartComponent[1];
            ChartComponent chartOne = chartOn(eclipticPage(), first);
            ChartModuleHost hostOne = new ChartModuleHost(chartOne,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            EclipticModule one = EclipticSession.begin(hostOne);
            EclipticSession.Switch switchOne = EclipticSession.of(one, store);
            JCheckBox itemOne = controls(switchOne).eclipticBox();
            switchOne.restore();
            assertFalse(one.showing(),
                    "a first session begins with the released"
                            + " default, hidden");
            assertFalse(itemOne.isSelected(),
                    "and its tick says so");
            itemOne.doClick();
            store.flush();

            // A genuine second session: fresh chart, fresh host,
            // fresh module, fed from a fresh store - the way
            // JUranometriaMain builds it.
            ChartComponent[] second = new ChartComponent[1];
            ChartComponent chartTwo = chartOn(eclipticPage(), second);
            ChartModuleHost hostTwo = new ChartModuleHost(chartTwo,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            EclipticModule two = EclipticSession.begin(hostTwo);
            EclipticSession.Switch switchTwo = EclipticSession.of(two,
                    EclipticStore.forNode(node));
            JCheckBox itemTwo = controls(switchTwo).eclipticBox();
            switchTwo.restore();
            assertTrue(two.showing(),
                    "and the second session draws what the first one"
                            + " chose");
            assertTrue(itemTwo.isSelected(),
                    "with the tick showing it, from the same one read"
                            + " of the store");

            // And the reader can put it back, and that survives too.
            itemTwo.doClick();
            EclipticStore.forNode(node).flush();
            ChartComponent[] third = new ChartComponent[1];
            ChartComponent chartThree = chartOn(eclipticPage(), third);
            ChartModuleHost hostThree = new ChartModuleHost(chartThree,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            EclipticModule three = EclipticSession.begin(hostThree);
            EclipticSession.Switch switchThree = EclipticSession.of(three,
                    EclipticStore.forNode(node));
            controls(switchThree);
            switchThree.restore();
            assertFalse(three.showing(),
                    "hiding it again is a choice that survives too,"
                            + " and is not read back as never having"
                            + " chosen");
        });
    }

    @Test
    void anAtlasWithoutTheModuleIsUnaffectedByARememberedChoice()
            throws Exception {
        // The gate's requirement: module absence leaves no
        // preference-dependent chart change.
        SwingSession.scratchPreferences("ecliptic-no-module", node -> {
            EclipticStore.forNode(node).save(true);
            EclipticStore.forNode(node).flush();

            ChartComponent[] holder = new ChartComponent[1];
            ChartComponent chart = chartOn(eclipticPage(), holder);
            BufferedImage withNoModule = paint(chart);

            ChartComponent[] plain = new ChartComponent[1];
            ChartComponent bare = chartOn(eclipticPage(), plain);
            assertEquals(0, differences(withNoModule, paint(bare)),
                    "with no module attached the remembered choice"
                            + " reaches no page: the preference is a"
                            + " module's to act on, and an absent one"
                            + " acts on nothing");
        });
    }
}
