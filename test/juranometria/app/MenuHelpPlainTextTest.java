package juranometria.app;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.ui.ChartViewController;
import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Menu help is plain text (issue #508).
 *
 * <p>A menu item's tooltip is drawn by the platform: under the macOS
 * screen menu bar the system shows it as plain text, so the HTML that
 * wraps a long Swing tooltip appeared there as source - on Sun...,
 * Venus..., Moon..., Jupiter... and Help ▸ Copy View Report, in English,
 * and on Jupiter... and Copy View Report in Norwegian, where the other
 * three happen to fall under the wrapping length. Every menu item of the
 * bar as the application builds it - the items it adds afterwards
 * included - carries its help as the language file's words, character
 * for character, and no menu tooltip carries markup; the spoken
 * descriptions are unchanged; and a tooltip Swing draws itself keeps
 * its wrapping.
 */
class MenuHelpPlainTextTest {

    /** The five items whose help was shown as source, by key stem. */
    private static final List<String> HELPED = List.of(
            "menu.sun", "menu.venus", "menu.moon", "menu.jupiter", "menu.viewReport");

    @Test
    void theFiveItemsCarryTheirWordsExactlyInBothLanguages() throws Exception {
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            JMenuBar bar = bar(said);
            for (String stem : HELPED) {
                JMenuItem item = item(bar, said.say(stem + ".label"));
                assertNotNull(item, "the premise: " + stem + " is on the bar in " + language);
                assertEquals(said.say(stem + ".hover"), item.getToolTipText(),
                        stem + " in " + language + ": the hover, as the language file says it");
                assertEquals(said.say(stem + ".explain"),
                        item.getAccessibleContext().getAccessibleDescription(),
                        stem + " in " + language + ": the spoken description, unchanged");
            }
        }
    }

    @Test
    void noMenuTooltipCarriesMarkup() throws Exception {
        for (String language : List.of("en", "nb-NO")) {
            List<String> marked = new ArrayList<>();
            int helped = 0;
            for (JMenuItem item : items(bar(InterfaceText.forLanguage(language)))) {
                String tip = item.getToolTipText();
                if (tip == null) {
                    continue;
                }
                helped++;
                if (tip.contains("<") || tip.contains(">") || tip.contains("&lt;")
                        || tip.contains("&amp;") || tip.contains("&gt;")) {
                    marked.add(item.getText() + ": " + tip);
                }
            }
            assertEquals(List.of(), marked, "menu help is plain text in " + language);
            assertEquals(HELPED.size(), helped, "the premise: every hovered menu item was seen in "
                    + language);
        }
    }

    @Test
    void aTooltipSwingDrawsItselfKeepsItsWrapping() {
        String longHelp = InterfaceText.forLanguage("en").say("menu.jupiter.hover");
        JButton button = Explain.control(new JButton("x"), longHelp, "a spoken description");
        assertTrue(button.getToolTipText().startsWith("<html>"),
                "a button's long tooltip is still wrapped by Swing: " + button.getToolTipText());
        JMenuItem item = Explain.control(new JMenuItem("x"), longHelp, "a spoken description");
        assertEquals(longHelp, item.getToolTipText(), "a menu item's is the words alone");
    }

    /** The bar the application builds, with the items it adds afterwards. */
    private static JMenuBar bar(InterfaceText said) throws Exception {
        JMenuBar[] bar = new JMenuBar[1];
        SwingUtilities.invokeAndWait(() -> {
            bar[0] = AppMenuBar.create(new ChartViewController(), () -> { }, () -> { },
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, said);
            AppMenuBar.addVenusTable(bar[0], said, () -> { });
            AppMenuBar.addJupiterTable(bar[0], said, () -> { });
            AppMenuBar.addJupiterOnChart(bar[0], said, () -> { });
            AppMenuBar.addVenusOnChart(bar[0], said, () -> { });
        });
        return bar[0];
    }

    private static List<JMenuItem> items(JMenuBar bar) {
        List<JMenuItem> all = new ArrayList<>();
        for (int m = 0; m < bar.getMenuCount(); m++) {
            if (bar.getMenu(m) != null) {
                items(bar.getMenu(m), all);
            }
        }
        return all;
    }

    private static void items(JMenu menu, List<JMenuItem> into) {
        into.add(menu);
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item instanceof JMenu sub) {
                items(sub, into);
            } else if (item != null) {
                into.add(item);
            }
        }
    }

    private static JMenuItem item(JMenuBar bar, String label) {
        for (JMenuItem item : items(bar)) {
            if (label.equals(item.getText())) {
                return item;
            }
        }
        return null;
    }
}
