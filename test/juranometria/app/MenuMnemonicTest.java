package juranometria.app;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.ui.ChartViewController;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.MnemonicText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A menu's access letters belong to the language that wrote its words
 * (Sprint 33, issue #350).
 *
 * <p>An accelerator and a mnemonic look alike and are not. Command-E
 * is platform notation. A mnemonic marks a letter <em>inside the
 * item's own displayed word</em>, and that word is translated: `C` is
 * in "Chart Options" and nowhere in "Kartvalg"; `P` is in "Place and
 * Time" and nowhere in "Sted og tid".
 *
 * <p>Five were hard-coded in English, and the runtime inventory did
 * not see them - it read labels, hovers, spoken names and spoken
 * descriptions, and a mnemonic is none of those. A whole channel was
 * untranslated behind a surface that otherwise looked finished.
 */
class MenuMnemonicTest {

    private static final List<String> ITEMS = List.of(
            "menu.export", "menu.chartoptions", "menu.placeandtime",
            "menu.inspector", "menu.ecliptic");

    /** Each item carries the letter its own language declares. */
    @Test
    void everyItemUsesTheLetterItsLanguageDeclares() throws Exception {
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            List<String> wrong = new ArrayList<>();
            inMenu(said, bar -> {
                for (String stem : ITEMS) {
                    JMenuItem item = itemLabelled(bar,
                            said.say(stem + ".label"));
                    assertTrue(item != null,
                            "the premise: " + stem + " is on the bar in "
                                    + language);
                    char declared = said.say(stem + ".mnemonic").charAt(0);
                    if (item.getMnemonic()
                            != Character.toUpperCase(declared)) {
                        wrong.add(stem + " shows "
                                + (char) item.getMnemonic()
                                + ", the pack says " + declared);
                    }
                }
            });
            assertEquals(List.of(), wrong,
                    "every access letter in " + language
                            + " comes from that language's pack");
        }
    }

    /** And it is a letter a reader can actually see. */
    @Test
    void everyLetterIsPresentInTheLabelItMarks() throws Exception {
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            for (String stem : ITEMS) {
                String label = said.say(stem + ".label");
                char letter = said.say(stem + ".mnemonic").charAt(0);
                assertTrue(label.toUpperCase(java.util.Locale.ROOT)
                                .indexOf(Character.toUpperCase(letter)) >= 0,
                        stem + " in " + language + ": '" + letter
                                + "' must be visible in \"" + label + "\"");
            }
        }
    }

    /**
     * The English letter cannot be forced onto a Norwegian label.
     *
     * <p>The mutation the ruling names: Norwegian *Kartvalg* with the
     * English `C`. It is refused because the character is not in the
     * label, rather than quietly accepted as a key that does nothing.
     */
    @Test
    void anEnglishLetterOnANorwegianLabelIsRefused() {
        InterfaceText english = InterfaceText.forLanguage("en");
        InterfaceText norsk = InterfaceText.forLanguage("nb-NO");
        String norwegianLabel = norsk.say("menu.chartoptions.label");

        // The premises, so this cannot pass for an unrelated reason.
        // An earlier version of this test asked the NORWEGIAN pack
        // for Export's letter and applied it to Kartvalg: it proved
        // that an absent letter is refused, but not the C-to-K case
        // its own name promised.
        assertEquals("C", english.say("menu.chartoptions.mnemonic"),
                "English declares C for Chart Options");
        assertEquals("K", norsk.say("menu.chartoptions.mnemonic"),
                "and Norwegian declares K for Kartvalg");
        assertTrue(norwegianLabel.toUpperCase(java.util.Locale.ROOT)
                        .indexOf('C') < 0,
                "the premise: the Norwegian label has no C - "
                        + norwegianLabel);

        // The English declaration, put on the Norwegian label.
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> MnemonicText.in(english).letterFor(
                        "menu.chartoptions.mnemonic", norwegianLabel));
        assertTrue(refused.getMessage().contains("not in the label"),
                "and says why: " + refused.getMessage());
        assertTrue(refused.getMessage().contains(norwegianLabel),
                "naming the label a reader would be staring at: "
                        + refused.getMessage());
    }

    /** A malformed value is a mistake, not a gap to paper over. */
    @Test
    void aMnemonicThatIsNotOneCharacterIsRefused() {
        MnemonicText letters =
                MnemonicText.in(InterfaceText.forLanguage("en"));
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> letters.letterFor("menu.file.label", "File"));
        assertTrue(refused.getMessage().contains("exactly one character"),
                "a whole word is not an access letter: "
                        + refused.getMessage());
    }

    /** Letters do not collide inside one menu. */
    @Test
    void twoItemsInOneMenuDoNotClaimTheSameLetter() throws Exception {
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            inMenu(said, bar -> {
                for (int m = 0; m < bar.getMenuCount(); m++) {
                    JMenu menu = bar.getMenu(m);
                    List<Integer> claimed = new ArrayList<>();
                    for (int i = 0; i < menu.getItemCount(); i++) {
                        JMenuItem item = menu.getItem(i);
                        if (item == null || item.getMnemonic() == 0) {
                            continue;
                        }
                        assertTrue(!claimed.contains(item.getMnemonic()),
                                "two items on " + menu.getText() + " in "
                                        + language + " claim '"
                                        + (char) item.getMnemonic() + "'");
                        claimed.add(item.getMnemonic());
                    }
                }
            });
        }
    }

    // ---- building the real bar ---------------------------------------

    private interface Check {
        void on(JMenuBar bar);
    }

    private static void inMenu(InterfaceText said, Check check)
            throws Exception {
        JMenuBar[] bar = new JMenuBar[1];
        SwingUtilities.invokeAndWait(() -> bar[0] = AppMenuBar.create(
                new ChartViewController(), () -> { }, () -> { }, () -> { },
                () -> { }, () -> { }, () -> { }, () -> { }, said));
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> check.on(bar[0]));
    }

    private static JMenuItem itemLabelled(JMenuBar bar, String label) {
        for (int m = 0; m < bar.getMenuCount(); m++) {
            JMenu menu = bar.getMenu(m);
            if (menu == null) {
                continue;
            }
            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem item = menu.getItem(i);
                if (item != null && label.equals(item.getText())) {
                    return item;
                }
            }
        }
        return null;
    }
}
