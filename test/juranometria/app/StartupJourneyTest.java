package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.ui.ecliptic.EclipticStore;
import juranometria.ui.ReaderInput;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The application starts, and starting it composes a working menu
 * (Sprint 33, issue #350).
 *
 * <p>Nothing ran {@code JUranometriaMain.start}. Every other test
 * builds the pieces it needs and wires them itself, which is right
 * for a component contract and leaves the composition - the order the
 * lines actually run in - held by nobody. So a checkpoint shipped
 * that could not launch: the menu bar moved down the method to be
 * built in the reader's language, and the two lines that read it back
 * stayed above it. {@code frame.getJMenuBar()} was null,
 * {@code AppMenuBar.checkBoxItem} does not guard, and a packaged
 * application would have shown the failure reporter instead of a
 * window. 1445 tests passed over it.
 *
 * <p>This is the permanent contract for that. It runs the real
 * {@code start} - not a reconstruction of it - and asks the questions
 * only a composed application can answer.
 *
 * <p><strong>Its own five stores.</strong> Startup reads appearance,
 * chart options, language, place and ecliptic. All five are injected,
 * from one scratch node, because the real application reads all five
 * from one node; supplying some and letting the rest reach the
 * reader's own preferences would report on a session this had not
 * determined, and would write to a real reader's settings doing it.
 * The look and feel follows from the injected appearance store rather
 * than from whatever a peer left installed.
 *
 * <p>Ordering is checked by <strong>consequence</strong>, not by
 * reading the source. The ecliptic's remembered choice has to arrive
 * at its menu item, which cannot happen unless the bar is on the
 * frame before the restore runs - and {@code EclipticSession.restore}
 * refuses a null item outright, so a null guard added in the wrong
 * place fails here too rather than quietly skipping the restore.
 */
class StartupJourneyTest {

    /** What a caller needs from a started application. */
    record Running(JFrame frame, JMenuBar menu, InterfaceText words) {
    }

    /** Everything a channel said, in the order it was found. */
    private record Channels(String title, String windowDescription,
                            Set<String> shown, Set<String> spoken,
                            List<String> letters) {
    }

    @Test
    void theApplicationStartsAndComposesItsMenu() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "starting the application means making a window");
        running("en", true, app -> {
            assertNotNull(app.frame(), "startup produced a window");
            assertTrue(app.frame().isShowing(),
                    "and showed it, which is what a reader sees");
            assertNotNull(app.menu(), "the frame carries a menu bar");
            assertTrue(app.menu().getMenuCount() >= 3,
                    "the premise: the bar has menus on it - "
                            + app.menu().getMenuCount());

            // Composed in the session's language, which is what moved
            // the bar down the method in the first place.
            assertNotNull(menuNamed(app.menu(),
                            app.words().say("menu.view.label")),
                    "the bar was built from the session's words: no \""
                            + app.words().say("menu.view.label")
                            + "\" menu on it");

            // The consequence that fixes the ordering in place. The
            // stored choice was "shown"; it can only have reached the
            // item if the bar was set before the restore ran.
            JCheckBoxMenuItem ecliptic =
                    AppMenuBar.eclipticItem(app.menu());
            assertNotNull(ecliptic,
                    "the ecliptic's item is on the composed bar");
            assertTrue(ecliptic.isSelected(),
                    "and carries the choice the store remembered,"
                            + " which is only possible if the bar was"
                            + " on the frame when startup read it"
                            + " back");

            assertNotNull(AppMenuBar.inspectorItem(app.menu()),
                    "and the inspector's item is there to be wired to"
                            + " the toggle");
        });
    }

    /**
     * Starts the real application with scratch stores and hands the
     * result to a caller.
     *
     * @param interfaceTag the language a previous session left stored
     * @param eclipticShown what that session remembered about the
     *     ecliptic, so the restore has something to carry
     */
    static void running(String interfaceTag, boolean eclipticShown,
                        Body body) throws Exception {
        SwingSession.restoring(() ->
                SwingSession.scratchPreferences("juranometria-350-startup",
                        node -> {
                    SkyLanguageStore language = SkyLanguageStore
                            .forNode(node);
                    language.save(language.choice(Atlas.languages())
                            .withInterface(interfaceTag));
                    EclipticStore ecliptic = EclipticStore.forNode(node);
                    ecliptic.save(eclipticShown);
                    StartupStores stores = new StartupStores(
                            AppearanceStore.forNode(node),
                            ChartOptionsStore.forNode(node), language,
                            juranometria.ui.placeandtime.PlaceStore
                                    .forNode(node),
                            ecliptic);

                    JFrame[] frame = new JFrame[1];
                    try {
                        SwingUtilities.invokeAndWait(() ->
                                frame[0] = JUranometriaMain.start(false,
                                        stores));
                        SwingUtilities.invokeAndWait(() -> { });
                        body.run(new Running(frame[0],
                                frame[0].getJMenuBar(),
                                InterfaceText.forLanguage(interfaceTag)));
                    } finally {
                        closeEverything(frame[0]);
                    }
                }));
    }

    /** What a caller does with a started application. */
    interface Body {
        void run(Running app) throws Exception;
    }


    // ---- the surface that made the seam necessary --------------

    @Test
    void theApplicationOpensPlaceAndTimeInTheStoredLanguage()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a window has to exist for its channels to");
        InterfaceText norsk = InterfaceText.forLanguage("nb-NO");
        InterfaceText english = InterfaceText.forLanguage("en");
        Channels[] found = new Channels[1];
        running("nb-NO", true, app -> {
            press(app, norsk);
            found[0] = read(app.frame());
        });
        Channels said = found[0];

        // The premises. Each channel is asserted non-empty before it
        // is asserted correct: "no English found" is worth nothing
        // from a channel that was never read, which is how three
        // surfaces passed with whole channels untranslated (#350).
        assertTrue(said.shown().size() >= 8,
                "the premise: the dialog showed words - "
                        + said.shown().size());
        assertTrue(said.spoken().size() >= 3,
                "the premise: it spoke some too - "
                        + said.spoken().size());
        assertEquals(8, said.letters().size(),
                "the premise: all eight access letters were set - "
                        + said.letters());

        // The window itself, which is a channel an inventory that
        // walks the content pane never reads (#350).
        assertEquals(norsk.say("placeandtime.title"), said.title(),
                "the window is titled in the language the reader"
                        + " stored, not the one the call site names");
        assertEquals(norsk.say("placeandtime.explain"),
                said.windowDescription(),
                "and describes itself in it");

        // Shown, hovered and spoken.
        for (String key : List.of("placeandtime.latitude.label",
                "placeandtime.longitude.label",
                "placeandtime.centre.label")) {
            assertTrue(said.shown().contains(norsk.say(key)),
                    key + " reads \"" + norsk.say(key) + "\"");
            assertTrue(!said.shown().contains(english.say(key)),
                    key + " does not read \"" + english.say(key) + "\"");
        }
        assertTrue(said.spoken().contains(norsk.say(
                        "placeandtime.meridian.a11y")),
                "the meridian switch answers a screen reader in"
                        + " Norwegian: " + said.spoken());

        // Access letters belong to the translated word, so they are
        // the Norwegian ones and they are in the words shown.
        assertTrue(said.letters().contains(
                        norsk.say("placeandtime.latitude.mnemonic")),
                "the latitude letter is the one Norwegian declares, "
                        + norsk.say("placeandtime.latitude.mnemonic")
                        + " for " + norsk.say("placeandtime.latitude.label")
                        + ", and not English's "
                        + english.say("placeandtime.latitude.mnemonic")
                        + " - " + said.letters());

        // Notation is not language and does not move.
        assertTrue(said.shown().stream()
                        .anyMatch(word -> word.contains("⌘")),
                "the keystrokes are spelled by this desktop: "
                        + said.shown());
        assertTrue(said.shown().stream()
                        .noneMatch(word -> word.contains(" then ")),
                "and nothing joins two of them with an English word");
        assertTrue(said.shown().stream().anyMatch(word ->
                        word.matches(".*\\d{4}-\\d{2}-\\d{2}.*")),
                "the instant is written as an instant: " + said.shown());
    }

    /**
     * Opens the View menu and clicks the item, as a reader does.
     *
     * <p>Not {@code doClick()}. The convention allows it on a menu
     * item, and the gate's back-door counts are being driven down
     * rather than up, so this journey pays the price of a real press:
     * the popup is shown, the item is proven on screen and sized, and
     * the click goes through the shared helper that states those
     * premises before it dispatches anything.
     */
    private static void press(Running app, InterfaceText norsk)
            throws Exception {
        JMenuBar bar = app.menu();
        assertTrue(bar != null, "the premise: the application has a menu");
        JMenu view = menuNamed(bar, norsk.say("menu.view.label"));
        assertTrue(view != null, "the premise: a Norwegian session's bar"
                + " carries \"" + norsk.say("menu.view.label") + "\"");
        JMenuItem item = itemNamed(view,
                norsk.say("menu.placeandtime.label"));
        assertTrue(item != null, "the premise: that menu offers \""
                + norsk.say("menu.placeandtime.label") + "\"");
        try {
            SwingUtilities.invokeAndWait(() ->
                    view.setPopupMenuVisible(true));
            SwingUtilities.invokeAndWait(() -> { });
            assertTrue(view.getPopupMenu().isShowing(),
                    "the premise: the menu is open, because a popup"
                            + " paints nothing until it is shown");
            ReaderInput.click(item);
        } finally {
            SwingUtilities.invokeAndWait(() ->
                    view.setPopupMenuVisible(false));
        }
        SwingUtilities.invokeAndWait(() -> { });
    }

    /** Every channel of the dialog the press opened. */
    private static Channels read(JFrame frame) throws Exception {
        Channels[] channels = new Channels[1];
        SwingUtilities.invokeAndWait(() -> {
            JDialog dialog = dialogOf(frame);
            assertTrue(dialog != null,
                    "the premise: pressing the item opened a window");
            Set<String> shown = new LinkedHashSet<>();
            Set<String> spoken = new LinkedHashSet<>();
            List<String> letters = new ArrayList<>();
            walk(dialog.getContentPane(), shown, spoken, letters);
            channels[0] = new Channels(dialog.getTitle(),
                    dialog.getAccessibleContext()
                            .getAccessibleDescription(),
                    shown, spoken, letters);
        });
        return channels[0];
    }

    private static void walk(Container from, Set<String> shown,
                             Set<String> spoken, List<String> letters) {
        for (Component child : from.getComponents()) {
            if (child instanceof JComponent widget) {
                if (widget instanceof AbstractButton button) {
                    add(shown, button.getText());
                    letter(letters, button.getMnemonic());
                }
                if (widget instanceof JLabel label) {
                    add(shown, label.getText());
                    letter(letters, label.getDisplayedMnemonic());
                }
                if (widget instanceof javax.swing.JTextField field) {
                    add(shown, field.getText());
                }
                // A hover is a shown channel: a reader reads it.
                add(shown, widget.getToolTipText());
                if (widget.getAccessibleContext() != null) {
                    add(spoken, widget.getAccessibleContext()
                            .getAccessibleName());
                    add(spoken, widget.getAccessibleContext()
                            .getAccessibleDescription());
                }
            }
            if (child instanceof Container nested) {
                walk(nested, shown, spoken, letters);
            }
        }
    }

    private static void letter(List<String> letters, int keyCode) {
        if (keyCode != 0) {
            letters.add(String.valueOf((char) keyCode));
        }
    }

    private static void add(Set<String> said, String text) {
        if (text != null && !text.isBlank()) {
            // A wrap becomes a space and is never deleted: Chart
            // Options once published "angitt ikatalogen" that way.
            said.add(text.replaceAll("<[^>]*>", " ")
                    .replaceAll("\\s+", " ").trim());
        }
    }

    private static JDialog dialogOf(JFrame frame) {
        for (Window window : frame.getOwnedWindows()) {
            if (window instanceof JDialog dialog && dialog.isShowing()) {
                return dialog;
            }
        }
        return null;
    }

    private static JMenuItem itemNamed(JMenu menu, String label) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && label.equals(item.getText())) {
                return item;
            }
        }
        return null;
    }

    static JMenu menuNamed(JMenuBar bar, String label) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu != null && label.equals(menu.getText())) {
                return menu;
            }
        }
        return null;
    }

    /**
     * Every window the application opened, and the frame.
     *
     * <p>Dialogs the atlas keeps one live instance of would
     * otherwise be handed to the next test that asked - a window from
     * a session with different preferences, brought forward rather
     * than built.
     */
    static void closeEverything(JFrame frame) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (frame != null) {
                for (Window owned : frame.getOwnedWindows()) {
                    owned.dispose();
                }
                frame.dispose();
            }
        });
        SwingUtilities.invokeAndWait(() -> { });
    }

    /** A started application is one thing; asserting is another. */
    static void assertLanguage(String tag, String key, String actual) {
        assertEquals(InterfaceText.forLanguage(tag).say(key), actual,
                key + " is said in " + tag);
    }
}
