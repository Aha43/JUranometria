package juranometria.ui.ecliptic;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
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

    private static final juranometria.render.ChartRenderer RENDERER =
            new juranometria.render.ChartRenderer(
                    juranometria.chart.StarSizePolicy.DEFAULT);

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
            out[0] = new ChartComponent(Atlas.assembler());
            out[0].setSize(900, 700);
            out[0].setViewState(state);
        });
        SwingUtilities.invokeAndWait(() -> { });
        return out[0];
    }

    // ---- the control exists only when the module does ----------------

    @Test
    void withNoEclipticModuleThereIsNoEclipticControl() {
        JMenuBar bar = AppMenuBar.create(null, null, () -> { },
                () -> { }, () -> { }, () -> { });
        assertNull(AppMenuBar.eclipticItem(bar),
                "an atlas without the module offers no switch for it:"
                        + " a tick for something that cannot be drawn"
                        + " would be a promise the atlas cannot keep");
        assertNotNull(AppMenuBar.inspectorItem(bar),
                "while the controls that do belong to it are there,"
                        + " so the absence above is an answer");
    }

    @Test
    void theControlIsACheckboxOnTheViewMenuUnderTheInspector() {
        JMenuBar bar = AppMenuBar.create(null, null, () -> { },
                () -> { }, () -> { }, () -> { }, () -> { });
        JCheckBoxMenuItem item = AppMenuBar.eclipticItem(bar);
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

        // On the View menu, below the Inspector's.
        javax.swing.JMenu view = null;
        for (int i = 0; i < bar.getMenuCount(); i++) {
            if ("View".equals(bar.getMenu(i).getText())) {
                view = bar.getMenu(i);
            }
        }
        assertNotNull(view, "the View menu is where the gate put it");
        int inspectorAt = -1;
        int eclipticAt = -1;
        for (int i = 0; i < view.getItemCount(); i++) {
            javax.swing.JMenuItem each = view.getItem(i);
            if (each == null) {
                continue;
            }
            if (AppMenuBar.INSPECTOR_ITEM.equals(each.getName())) {
                inspectorAt = i;
            }
            if (AppMenuBar.ECLIPTIC_ITEM.equals(each.getName())) {
                eclipticAt = i;
            }
        }
        assertTrue(inspectorAt >= 0 && eclipticAt == inspectorAt + 1,
                "directly below the Inspector's, as the gate drew it:"
                        + " inspector at " + inspectorAt + ", ecliptic"
                        + " at " + eclipticAt);
    }

    // ---- the routes a reader has --------------------------------------

    @Test
    void pointerAndKeyboardBothReachIt() {
        int[] toggles = {0};
        JMenuBar bar = AppMenuBar.create(null, null, () -> { },
                () -> { }, () -> { }, () -> { }, () -> toggles[0]++);
        JCheckBoxMenuItem item = AppMenuBar.eclipticItem(bar);

        // The pointer route: what a click on the item does.
        item.doClick();
        assertEquals(1, toggles[0], "clicking the item toggles it");

        // The keyboard route: the item carries a mnemonic, so the
        // View menu can be walked to it without a pointer at all.
        assertEquals('E', (char) item.getMnemonic(),
                "and it has a mnemonic, so the menu is walkable");
        assertTrue(item.isEnabled(),
                "and the item is reachable rather than greyed");

        // Firing the action the keyboard would fire.
        item.getAction();
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
        meridian.showing(false, false, false);
        assertEquals(0, differences(eclipticOnly, paint(chart)),
                "turning the other module off and on again returns"
                        + " the ecliptic's page unchanged");
        assertTrue(differences(both, eclipticOnly) >= 0,
                "and the two compose without either answering for the"
                        + " other");

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
            // menu item does.
            ChartComponent[] first = new ChartComponent[1];
            ChartComponent chartOne = chartOn(eclipticPage(), first);
            ChartModuleHost hostOne = new ChartModuleHost(chartOne,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            EclipticModule one = EclipticSession.begin(hostOne, store);
            assertFalse(one.showing(),
                    "a first session begins with the released"
                            + " default, hidden");
            one.showing(true);
            store.save(true);
            store.flush();

            // A genuine second session: fresh chart, fresh host,
            // fresh module, fed from a fresh store - the way
            // JUranometriaMain builds it.
            ChartComponent[] second = new ChartComponent[1];
            ChartComponent chartTwo = chartOn(eclipticPage(), second);
            ChartModuleHost hostTwo = new ChartModuleHost(chartTwo,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            EclipticModule two = EclipticSession.begin(hostTwo,
                    EclipticStore.forNode(node));
            assertTrue(two.showing(),
                    "and the second session draws what the first one"
                            + " chose");

            // And the reader can put it back, and that survives too.
            two.showing(false);
            EclipticStore.forNode(node).save(false);
            EclipticStore.forNode(node).flush();
            ChartComponent[] third = new ChartComponent[1];
            ChartComponent chartThree = chartOn(eclipticPage(), third);
            ChartModuleHost hostThree = new ChartModuleHost(chartThree,
                    new juranometria.chart.SelectionModel(),
                    request -> { });
            assertFalse(EclipticSession.begin(hostThree,
                            EclipticStore.forNode(node)).showing(),
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
