package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.ui.companion.CompanionStore;
import juranometria.ui.companion.CompanionWindow;
import juranometria.ui.language.SkyLanguageStore;
import juranometria.ui.placeandtime.PlaceAndTimeDialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The companion in the running application (#434, PR 3; ruled on
 * #433): the real {@code JUranometriaMain.start}, three times, on one
 * scratch preference node. A fresh profile starts with the companion
 * closed; View, Controls opens it and the tick follows it however it
 * is closed; the instant Now sets in it is what the Place and Time
 * dialog shows; it starts closed at every launch (ruled on #455)
 * where it was and without asking for the keyboard; hidden at the end,
 * it stays hidden.
 */
class CompanionStartupJourneyTest {

    @Test
    void theCompanionIsReachedFromViewAndRestoredAsItWasLeft() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the real application opens a window");
        SwingSession.restoring(() -> SwingSession.scratchPreferences(
                "juranometria-434-companion", node -> {
                    Rectangle[] left = new Rectangle[1];
                    // First session: fresh; open it from View, use it,
                    // close it by its box, open it again, and quit.
                    run(node, frame -> {
                        JCheckBoxMenuItem item = AppMenuBar.companionItem(
                                frame.getJMenuBar());
                        assertNotNull(item, "View carries the Controls switch");
                        assertEquals("Controls", item.getText());
                        JMenu view = viewOf(frame, item);
                        int at = Arrays.asList(view.getMenuComponents()).indexOf(item);
                        assertEquals("Place and Time...",
                                ((JMenuItem) view.getMenuComponent(at - 1)).getText(),
                                "directly after the section it holds");
                        assertFalse(item.isSelected(), "a fresh profile: closed");
                        assertEquals(null, companion(), "and not on screen");

                        SwingUtilities.invokeAndWait(item::doClick);
                        flush();
                        CompanionWindow companion = companion();
                        assertNotNull(companion, "View, Controls opened it");
                        assertTrue(item.isSelected(), "and the tick says so");
                        assertEquals(frame, companion.getOwner(),
                                "owned by the chart window");

                        // Chart Options, the third section since the
                        // chart's controls came first (#450; #443): its
                        // groups as first introduced, and a box pressed in
                        // it is what the Chart Options dialog then shows.
                        assertEquals(4, companion.sections().size(),
                                "Chart controls, Place and Time, Solar System, then"
                                        + " Chart Options (ruled on #457)");
                        assertEquals("Chart controls, expanded",
                                companion.sections().get(0).heading()
                                        .getAccessibleContext().getAccessibleName());
                        assertEquals("Solar System, collapsed",
                                companion.sections().get(2).heading()
                                        .getAccessibleContext().getAccessibleName(),
                                "introduced collapsed");
                        assertEquals("Chart Options, expanded",
                                companion.sections().get(3).heading()
                                        .getAccessibleContext().getAccessibleName());
                        assertFalse(((javax.swing.AbstractButton) named(
                                companion.getContentPane(),
                                "heading.chartoptions.deepsky")).isSelected(),
                                "Deep sky is introduced collapsed");
                        assertTrue(((javax.swing.AbstractButton) named(
                                companion.getContentPane(),
                                "heading.chartoptions.stars")).isSelected(),
                                "Stars open");
                        javax.swing.JCheckBox grid = ChartOptionsDialogTest.box(
                                companion.getContentPane(),
                                "Equatorial coordinate grid");
                        javax.swing.SwingUtilities.invokeAndWait(() ->
                                grid.scrollRectToVisible(new java.awt.Rectangle(
                                        grid.getSize())));
                        flush();
                        juranometria.ui.ReaderInput.click(grid);
                        flush();
                        assertFalse(grid.isSelected(), "the grid switched off");
                        SwingUtilities.invokeAndWait(() -> menuItem(frame,
                                "Chart Options...").doClick());
                        flush();
                        javax.swing.JDialog options = (javax.swing.JDialog) Arrays
                                .stream(Window.getWindows())
                                .filter(w -> w instanceof javax.swing.JDialog d
                                        && w.isDisplayable()
                                        && "Chart Options".equals(d.getTitle()))
                                .findFirst().orElseThrow();
                        assertFalse(ChartOptionsDialogTest.box(
                                        options.getContentPane(),
                                        "Equatorial coordinate grid").isSelected(),
                                "pressed in the companion, shown by the dialog");
                        juranometria.ui.ReaderInput.click(ChartOptionsDialogTest.button(
                                options.getContentPane(), "Close"));
                        flush();

                        // With the dialog already open, Now pressed in the
                        // companion with the pointer is the instant the
                        // open dialog then shows - it follows, it is not
                        // rebuilt.
                        SwingUtilities.invokeAndWait(() -> menuItem(frame,
                                "Place and Time...").doClick());
                        flush();
                        PlaceAndTimeDialog dialog = (PlaceAndTimeDialog) Arrays
                                .stream(Window.getWindows())
                                .filter(w -> w instanceof PlaceAndTimeDialog
                                        && w.isDisplayable())
                                .findFirst().orElseThrow();
                        JTextField shown = (JTextField) named(
                                dialog.getContentPane(), "instantField");
                        String before = shown.getText();
                        // The instant is shown to the second; a second
                        // passes so that Now is a different one.
                        Thread.sleep(1100);
                        juranometria.ui.ReaderInput.click((javax.swing.JComponent)
                                named(companion.getContentPane(), "nowButton"));
                        flush();
                        JTextField instant = (JTextField) named(
                                companion.getContentPane(), "instantField");
                        assertFalse(before.equals(shown.getText()),
                                "the open dialog moved with the companion's Now");
                        assertEquals(instant.getText(), shown.getText(),
                                "to the very instant the companion shows");
                        SwingUtilities.invokeAndWait(dialog::dispose);

                        SwingUtilities.invokeAndWait(() -> companion.dispatchEvent(
                                new WindowEvent(companion,
                                        WindowEvent.WINDOW_CLOSING)));
                        flush();
                        assertFalse(companion.isShowing(), "the close box hid it");
                        assertFalse(item.isSelected(), "and the tick followed");

                        SwingUtilities.invokeAndWait(item::doClick);
                        flush();
                        assertTrue(companion.isShowing() && item.isSelected(),
                                "opened again: the same window");
                        left[0] = companion.getBounds();
                    });

                    // Second session: open when the last one ended, and
                    // still closed at the start (ruled on #455); View opens
                    // it where it was left.
                    run(node, frame -> {
                        assertEquals(null, companion(),
                                "open at the end, closed at the start");
                        JCheckBoxMenuItem item = AppMenuBar.companionItem(
                                frame.getJMenuBar());
                        assertFalse(item.isSelected(), "and View says so");
                        SwingUtilities.invokeAndWait(item::doClick);
                        flush();
                        CompanionWindow companion = companion();
                        assertNotNull(companion, "View opens it");
                        assertEquals(left[0], companion.getBounds(),
                                "where it was left");
                        assertTrue(item.isSelected());
                        SwingUtilities.invokeAndWait(item::doClick);
                        flush();
                        assertFalse(companion.isShowing(), "View closes it too");
                    });

                    // Third session: hidden when the last one ended; the same.
                    run(node, frame -> {
                        assertEquals(null, companion(),
                                "hidden at the end, closed at the start");
                        assertFalse(AppMenuBar.companionItem(frame.getJMenuBar())
                                .isSelected());
                    });
                }));
    }

    private interface Session {
        void run(JFrame frame) throws Exception;
    }

    private static void run(java.util.prefs.Preferences node, Session session)
            throws Exception {
        SkyLanguageStore language = SkyLanguageStore.forNode(node);
        language.save(language.choice(Atlas.languages()).withInterface("en"));
        StartupStores stores = new StartupStores(AppearanceStore.forNode(node),
                ChartOptionsStore.forNode(node), language,
                juranometria.ui.placeandtime.PlaceStore.forNode(node),
                juranometria.ui.ecliptic.EclipticStore.forNode(node),
                juranometria.ui.solar.SunChartStore.forNode(node),
                juranometria.ui.solar.MoonChartStore.forNode(node),
                juranometria.ui.solar.JovianChartStore.forNode(node),
                juranometria.ui.ZoomLockStore.forNode(node),
                CompanionStore.forNode(node),
                juranometria.ui.ChartChromeStore.forNode(node),
                juranometria.ui.ChartWindowStore.forNode(node));
        JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(() ->
                    frame[0] = JUranometriaMain.start(false, stores));
            flush();
            session.run(frame[0]);
        } finally {
            StartupJourneyTest.closeEverything(frame[0]);
        }
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    /** The companion on screen, or null. */
    private static CompanionWindow companion() throws Exception {
        CompanionWindow[] found = new CompanionWindow[1];
        SwingUtilities.invokeAndWait(() -> found[0] = Arrays
                .stream(Window.getWindows())
                .filter(w -> w instanceof CompanionWindow && w.isShowing())
                .map(CompanionWindow.class::cast)
                .findFirst().orElse(null));
        return found[0];
    }

    private static JMenu viewOf(JFrame frame, JMenuItem item) {
        for (int i = 0; i < frame.getJMenuBar().getMenuCount(); i++) {
            JMenu menu = frame.getJMenuBar().getMenu(i);
            if (Arrays.asList(menu.getMenuComponents()).contains(item)) {
                return menu;
            }
        }
        throw new AssertionError("the item is in no menu");
    }

    private static JMenuItem menuItem(JFrame frame, String text) {
        for (int i = 0; i < frame.getJMenuBar().getMenuCount(); i++) {
            for (Component child : frame.getJMenuBar().getMenu(i)
                    .getMenuComponents()) {
                if (child instanceof JMenuItem item && text.equals(item.getText())) {
                    return item;
                }
            }
        }
        throw new AssertionError("no item " + text);
    }

    private static Component named(Container from, String name) {
        for (Component child : from.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container inner) {
                Component deeper = named(inner, name);
                if (deeper != null) {
                    return deeper;
                }
            }
        }
        return null;
    }
}
