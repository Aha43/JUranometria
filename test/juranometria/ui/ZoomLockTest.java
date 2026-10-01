package juranometria.ui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.app.SwingSession;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The zoom lock (Sprint 38, issue #428): while locked, the wheel and
 * the trackpad leave the field and the centre exactly where they are
 * and bank nothing for later; everything deliberate - the toolbar's
 * zoom buttons, the controller's zoom, a chosen field - still works;
 * panning is untouched; the toolbar's toggle and the lock are one
 * state, named in both languages; and the choice is a preference that
 * begins unlocked.
 */
class ZoomLockTest {

    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(InterfaceText.forLanguage("en"));

    /** The real chart with wheel zoom and panning, and a lock. */
    private static final class Fixture {
        final ChartViewController controller;
        final ChartComponent chart;
        final ZoomLock lock = new ZoomLock();

        Fixture() throws Exception {
            SceneAssembler assembler = SceneAssembler.allSky(
                    new SceneAssemblerTest.CountingCatalogue(), 1.5);
            controller = new ChartViewController(assembler::fits);
            ChartComponent[] holder = new ChartComponent[1];
            SwingUtilities.invokeAndWait(() -> {
                holder[0] = new ChartComponent(assembler, ENGLISH);
                ZoomInteraction.install(holder[0], controller, lock);
                PanInteraction.install(holder[0], controller);
                controller.onChange(holder[0]::setViewState);
                holder[0].setSize(900, 700);
            });
            flush();
            chart = holder[0];
        }

        MouseWheelEvent wheel(int x, int y, double rotation) throws Exception {
            MouseWheelEvent event = new MouseWheelEvent(chart,
                    MouseEvent.MOUSE_WHEEL, System.nanoTime() / 1_000_000,
                    0, x, y, x, y, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1,
                    (int) rotation, rotation);
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(event));
            flush();
            return event;
        }

        void mouse(int id, int x, int y) throws Exception {
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                    new MouseEvent(chart, id, System.nanoTime() / 1_000_000,
                            id == MouseEvent.MOUSE_RELEASED ? 0
                                    : MouseEvent.BUTTON1_DOWN_MASK,
                            x, y, 1, false, MouseEvent.BUTTON1)));
            flush();
        }

        void lock(boolean locked) throws Exception {
            SwingUtilities.invokeAndWait(() -> lock.lock(locked));
        }

        static void flush() throws Exception {
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    @Test
    void lockedTheWheelAndTheTrackpadLeaveTheFieldAndTheCentre() throws Exception {
        Fixture fixture = new Fixture();
        fixture.lock(true);
        ChartViewState before = fixture.controller.state();
        // A wheel notch each way, and a trackpad's fine rotations.
        for (double rotation : new double[] {-1.0, 1.0, -3.0, -0.4, -0.4, -0.4,
                0.25, 0.25, 0.25, 0.25, 0.25}) {
            MouseWheelEvent event = fixture.wheel(300, 200, rotation);
            assertTrue(event.isConsumed(),
                    "over the paper the chart still owns its wheel");
        }
        assertEquals(before, fixture.controller.state(),
                "neither the field nor the centre moved");

        // Unlocking releases nothing the reader did while locked.
        fixture.lock(false);
        fixture.wheel(300, 200, -0.4);
        assertEquals(before, fixture.controller.state(),
                "no remainder was banked while locked");
        fixture.wheel(300, 200, -1.0);
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees(),
                "unlocked, one notch is one step again");
    }

    @Test
    void unlockedTheWheelBehavesAsReleased() throws Exception {
        Fixture fixture = new Fixture();
        assertFalse(fixture.lock.locked(), "a fresh lock is unlocked");
        fixture.wheel(300, 200, -1.0);
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees());
        fixture.wheel(300, 200, 1.0);
        assertEquals(8.0, fixture.controller.state().fieldWidthDegrees());
    }

    @Test
    void lockedEverythingDeliberateStillZooms() throws Exception {
        Fixture fixture = new Fixture();
        fixture.lock(true);
        AtlasToolbar[] bar = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() -> bar[0] = new AtlasToolbar(
                fixture.controller, new SearchField(Atlas.search(),
                        Atlas.assembler(), fixture.controller,
                        InterfaceText.forLanguage("en")),
                null, null, null, null, fixture.lock,
                InterfaceText.forLanguage("en")));
        javax.swing.JButton zoomIn = (javax.swing.JButton) bar[0].getComponent(0);
        SwingUtilities.invokeAndWait(zoomIn::doClick);
        Fixture.flush();
        assertEquals(6.0, fixture.controller.state().fieldWidthDegrees(),
                "the toolbar's Zoom In still zooms");
        SwingUtilities.invokeAndWait(fixture.controller::zoomOut);
        assertEquals(8.0, fixture.controller.state().fieldWidthDegrees(),
                "View's Zoom Out and its key reach the same controller");
        SkyPosition orion = new SkyPosition(83.8, -5.4);
        SwingUtilities.invokeAndWait(() -> fixture.controller.recenter(orion, 18.0));
        assertEquals(18.0, fixture.controller.state().fieldWidthDegrees(),
                "a chosen field is still chosen");
        assertTrue(fixture.lock.locked(), "and none of it touched the lock");
    }

    @Test
    void lockedPanningStillPans() throws Exception {
        Fixture fixture = new Fixture();
        fixture.lock(true);
        ChartViewState before = fixture.controller.state();
        fixture.mouse(MouseEvent.MOUSE_PRESSED, 450, 350);
        fixture.mouse(MouseEvent.MOUSE_DRAGGED, 520, 380);
        fixture.mouse(MouseEvent.MOUSE_RELEASED, 520, 380);
        assertTrue(before.centre().separationDegrees(
                        fixture.controller.state().centre()) > 0.1,
                "a drag still moves the chart");
        assertEquals(before.fieldWidthDegrees(),
                fixture.controller.state().fieldWidthDegrees(),
                "at the same field");
    }

    @Test
    void theToggleAndTheLockAreOneStateInBothLanguages() throws Exception {
        for (String tag : new String[] {"en", "nb-NO"}) {
            InterfaceText said = InterfaceText.forLanguage(tag);
            ZoomLock lock = new ZoomLock();
            ChartViewController controller = new ChartViewController();
            AtlasToolbar[] bar = new AtlasToolbar[1];
            SwingUtilities.invokeAndWait(() -> bar[0] = new AtlasToolbar(controller,
                    new SearchField(Atlas.search(), Atlas.assembler(), controller,
                            said),
                    null, null, null, null, lock, said));
            javax.swing.JToggleButton toggle = bar[0].zoomLockButton();
            assertNotNull(toggle, tag + ": the bar carries the lock");
            assertEquals(AtlasToolbar.ZOOM_LOCK, toggle.getName());
            assertEquals(said.say("toolbar.zoomLock.label"), toggle.getText());
            assertEquals(said.say("toolbar.zoomLock.a11y"),
                    toggle.getAccessibleContext().getAccessibleName());
            assertTrue(toggle.isFocusable(), tag + ": reachable by keyboard");
            assertEquals(bar[0].getComponentIndex(toggle), 2,
                    tag + ": beside the two zoom buttons");
            assertFalse(toggle.isSelected(), tag + ": unlocked to begin");

            SwingUtilities.invokeAndWait(toggle::doClick);
            assertTrue(lock.locked() && toggle.isSelected(),
                    tag + ": pressing it locks, and it shows so");
            SwingUtilities.invokeAndWait(() -> lock.lock(false));
            assertFalse(toggle.isSelected(),
                    tag + ": a change made elsewhere shows on the bar");
        }
        assertEquals("Lock zoom", InterfaceText.forLanguage("en")
                .say("toolbar.zoomLock.label"));
        assertEquals("Lås zoom", InterfaceText.forLanguage("nb-NO")
                .say("toolbar.zoomLock.label"));
    }

    @Test
    void everyEarlierBarHasNoLock() throws Exception {
        ChartViewController controller = new ChartViewController();
        AtlasToolbar[] bar = new AtlasToolbar[1];
        SwingUtilities.invokeAndWait(() -> bar[0] = new AtlasToolbar(controller,
                new SearchField(Atlas.search(), Atlas.assembler(), controller,
                        InterfaceText.forLanguage("en")),
                InterfaceText.forLanguage("en")));
        assertNull(bar[0].zoomLockButton());
    }

    @Test
    void theChoiceIsAPreferenceThatBeginsUnlocked() throws Exception {
        SwingSession.scratchPreferences("zoom-lock", this::rememberedIn);
    }

    private void rememberedIn(Preferences node) {
        ZoomLockStore store = ZoomLockStore.forNode(node);
        assertEquals(Optional.empty(), store.locked(), "never chosen");
        assertFalse(store.lockedOrDefault(), "a fresh profile is unlocked");
        store.save(true);
        assertEquals(Optional.of(Boolean.TRUE),
                ZoomLockStore.forNode(node).locked(), "and kept");
        node.put("zoomLocked", "perhaps");
        assertEquals(Optional.empty(), ZoomLockStore.forNode(node).locked(),
                "anything unreadable is never chosen");
    }
}
