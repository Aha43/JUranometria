package juranometria.app;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.chart.ChartViewState;
import juranometria.ui.ChartViewController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 11 acceptance journey with real windows: the About and
 * Settings dialogs opened through the real menu actions, owned and
 * centred on the atlas frame, closable by Escape, with the accepted
 * appearance persisting across a session boundary and never touching
 * chart state. Requires a display; skipped (not silently passed) on
 * headless runners, where the headless-safe content and boundary
 * tests still run.
 */
class PublicFaceJourneyTest {

    @Test
    void aboutAndSettingsJourneyThroughRealMenusAndWindows() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "dialog ownership needs a display; content is tested headless");
        juranometria.app.SwingSession.Held previousSession =
                juranometria.app.SwingSession.capture();
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        // Held here so the journey can ask, after pressing the real
        // button, whether the press reached the language seam (#348).
        juranometria.ui.language.SkyLanguageSession language;
        JFrame[] frame = new JFrame[1];
        try {
            AppearanceStore store = AppearanceStore.forNode(node);
            juranometria.ui.language.SkyLanguageStore languageStore =
                    juranometria.ui.language.SkyLanguageStore
                            .forNode(node);
            language = juranometria.ui.language.SkyLanguageSession.begin(
                    languageStore, Atlas.languages());
            AppearanceSession session = new AppearanceSession(store, false);
            ChartViewController controller = new ChartViewController();
            ChartViewState chartBefore = controller.state();

            assertFalse(session.startupDark(),
                    "a clean preference state starts light");

            SwingUtilities.invokeAndWait(() -> {
                com.formdev.flatlaf.FlatLightLaf.setup();
                frame[0] = new JFrame("test-atlas");
                JMenuBar bar = AppMenuBar.create(
                        () -> SettingsDialog.open(frame[0], session,
                                effectiveDark -> {
                                    UiTheme.apply(effectiveDark);
                                    com.formdev.flatlaf.FlatLaf.updateUI();
                                },
                                // The journey's own scratch node, which
                                // it already removes in its finally -
                                // a second node here would outlive the
                                // run (#348).
                                language,
                                Atlas.names(),
                                juranometria.ui.language.InterfaceLanguages
                                        .discover()),
                        null,
                        () -> AboutDialog.open(frame[0]));
                frame[0].setJMenuBar(bar);
                frame[0].pack();
                // About, through the real Help menu item.
                bar.getMenu(1).getItem(0).doClick();
            });
            flush();
            JDialog about = visibleDialog("About " + AppInfo.NAME);
            assertNotNull(about, "the About dialog opened");
            assertSame(frame[0], about.getOwner(), "owned by the atlas window");
            assertTrue(about.getAccessibleContext().getAccessibleName()
                    .contains("About"));
            pressEscape(about);
            flush();
            assertFalse(about.isDisplayable(), "Escape closes About");

            // Settings, through the real menu item: choose Dark, OK.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog settings = visibleDialog("Settings");
            assertNotNull(settings, "the Settings dialog opened");
            assertSame(frame[0], settings.getOwner());
            juranometria.ui.ReaderInput.click(
                    findRadio(settings, "Dark appearance"));
            // Change the sky language too, so that pressing OK has
            // something to carry. Asserting the store afterwards only
            // means anything if the dialog was showing something
            // OTHER than what was already stored - otherwise an OK
            // wired to the unchanged current choice would look
            // exactly like an OK that read the controls (#348).
            SwingUtilities.invokeAndWait(() ->
                    chartLanguage(settings).setSelectedIndex(2));
            juranometria.ui.ReaderInput.click(
                    AboutDialogTest.button(settings.getContentPane(),
                            "OK"));
            assertFalse(settings.isDisplayable(), "OK closes Settings");
            assertTrue(UIManager.getLookAndFeel().getName()
                    .toLowerCase(java.util.Locale.ROOT).contains("dark"),
                    "the accepted theme applied immediately");
            assertEquals(Optional.of("dark"), store.load(),
                    "the accepted choice persisted");
            // And the same press reached the language seam. The
            // headless tests say what each selector MEANS; this says
            // the button a reader presses actually gets there, which
            // no synthetic click could establish (#348).
            assertEquals(java.util.Map.of(
                            juranometria.ui.language.SkyLanguageChoice
                                    .INTERFACE_KEY, "en",
                            juranometria.ui.language.SkyLanguageChoice
                                    .CHART_KEY, "nb-NO"),
                    languageStore.stated(),
                    "the real press reached the language seam and"
                            + " carried what the selector was showing."
                            + " An OK wired only to appearance leaves"
                            + " this empty; an OK wired to the choice"
                            + " the dialog opened with stores"
                            + " follow-interface, not the language the"
                            + " reader just picked");

            // The session boundary: a fresh session over the same node
            // decides the next launch; the override stays a non-writing
            // per-session pin in either direction.
            AppearanceStore nextStore = AppearanceStore.forNode(node);
            assertTrue(new AppearanceSession(nextStore, false).startupDark(),
                    "the persisted appearance returns after restart");
            assertTrue(new AppearanceSession(nextStore, true).startupDark());
            assertEquals(Optional.of("dark"), nextStore.load());

            // Chart and navigation state never entered the exchange.
            assertSame(chartBefore, controller.state(),
                    "appearance is application state, not chart state");

            // Return to Light through the same real path, cancelling
            // first to prove Cancel changes nothing.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog reopened = visibleDialog("Settings");
            juranometria.ui.ReaderInput.click(
                    findRadio(reopened, "Light appearance"));
            juranometria.ui.ReaderInput.click(
                    AboutDialogTest.button(reopened.getContentPane(),
                            "Cancel"));
            assertEquals(Optional.of("dark"), store.load(),
                    "Cancel persisted nothing");
            assertTrue(UIManager.getLookAndFeel().getName()
                    .toLowerCase(java.util.Locale.ROOT).contains("dark"),
                    "Cancel changed no appearance");

            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog again = visibleDialog("Settings");
            juranometria.ui.ReaderInput.click(
                    findRadio(again, "Light appearance"));
            juranometria.ui.ReaderInput.click(
                    AboutDialogTest.button(again.getContentPane(), "OK"));
            assertEquals(Optional.of("light"), store.load());
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame[0] != null) {
                    frame[0].dispose();
                }
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
            });
            previousSession.restore();
            node.removeNode();
        }
    }

    private static JDialog visibleDialog(String title) {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isDisplayable()
                    && title.equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private static void pressEscape(JDialog dialog) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var listener = dialog.getRootPane()
                    .getActionForKeyStroke(KeyStroke.getKeyStroke(
                            KeyEvent.VK_ESCAPE, 0));
            assertNotNull(listener, "an Escape binding exists");
            listener.actionPerformed(new java.awt.event.ActionEvent(
                    dialog.getRootPane(), 0, "escape"));
        });
    }

    private static javax.swing.JRadioButton findRadio(JDialog dialog,
                                                      String accessibleName) {
        return find(dialog.getContentPane(), accessibleName);
    }

    private static javax.swing.JRadioButton find(java.awt.Component component,
                                                 String accessibleName) {
        if (component instanceof javax.swing.JRadioButton radio
                && accessibleName.equals(radio.getAccessibleContext()
                        .getAccessibleName())) {
            return radio;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = find(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    /** The Names on chart selector inside a real Settings dialog. */
    @SuppressWarnings("unchecked")
    private static javax.swing.JComboBox<Object> chartLanguage(
            JDialog settings) {
        java.util.List<javax.swing.JComboBox<Object>> boxes =
                new java.util.ArrayList<>();
        collectBoxes(settings.getContentPane(), boxes);
        return boxes.get(1);
    }

    @SuppressWarnings("unchecked")
    private static void collectBoxes(java.awt.Container from,
            java.util.List<javax.swing.JComboBox<Object>> into) {
        for (java.awt.Component child : from.getComponents()) {
            if (child instanceof javax.swing.JComboBox<?> box) {
                into.add((javax.swing.JComboBox<Object>) box);
            }
            if (child instanceof java.awt.Container container) {
                collectBoxes(container, into);
            }
        }
    }
}
