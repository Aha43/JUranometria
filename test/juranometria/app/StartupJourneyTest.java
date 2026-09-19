package juranometria.app;

import java.awt.Window;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.ui.ecliptic.EclipticStore;
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
