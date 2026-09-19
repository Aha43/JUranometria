package juranometria.ui.language;

import javax.swing.AbstractButton;

/**
 * The letter a reader presses to reach a menu item
 * (Sprint 33, issue #350).
 *
 * <p>An accelerator and a mnemonic look alike and are not alike. An
 * accelerator - Command-E - is platform notation, spelled by the
 * desktop, and no language touches it. A <strong>mnemonic</strong>
 * identifies a letter <em>inside the item's own displayed word</em>,
 * and the displayed word is translated. {@code C} is in "Chart
 * Options" and is nowhere in "Kartvalg"; {@code P} is in "Place and
 * Time" and nowhere in "Sted og tid".
 *
 * <p>Frozen in English, they give a translated menu invisible or
 * misleading keyboard navigation while every visible and spoken
 * sentence reads correctly - a whole channel untranslated behind a
 * surface that looks finished. The runtime inventory missed them
 * because it read labels, hovers, spoken names and descriptions, and
 * a mnemonic is none of those.
 *
 * <p><strong>It refuses rather than guesses.</strong> A value that is
 * not exactly one character, or that is not in the label it claims to
 * mark, is a mistake in the resource and is reported as one. Nothing
 * here falls back to English and nothing infers a letter from the
 * text: a silently chosen letter would be a keyboard route nobody
 * decided, differing per language for reasons no one could see.
 */
public final class MnemonicText {

    private final InterfaceText said;

    private MnemonicText(InterfaceText said) {
        this.said = said;
    }

    /** The mnemonics for a language a caller states. */
    public static MnemonicText in(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the letters have to belong to some language");
        }
        return new MnemonicText(said);
    }

    /**
     * Gives an item the access letter its language declares.
     *
     * <p>The label must already be set: the letter is checked against
     * what the item actually displays, which is the only thing a
     * reader can look for.
     */
    public void apply(AbstractButton item, String key) {
        item.setMnemonic(letterFor(key, item.getText()));
    }

    /**
     * Gives a field's label the access letter its language declares.
     *
     * <p>The same policy, not a second one. A {@code JLabel} carries
     * its letter through {@code setDisplayedMnemonic} and is not an
     * {@code AbstractButton}, which is why an inventory that walked
     * only buttons found five of Place and Time's eight letters and
     * missed the three on its field labels (#350).
     *
     * <p>The label must already know which control it names: an
     * access letter on a label that targets nothing moves focus
     * nowhere, which is a keyboard route that looks present and is
     * not.
     */
    public void apply(javax.swing.JLabel label, String key) {
        if (label.getLabelFor() == null) {
            throw new IllegalStateException("the label for \"" + key
                    + "\" names no control, so its access letter would"
                    + " move focus nowhere");
        }
        label.setDisplayedMnemonic(letterFor(key, label.getText()));
    }

    /**
     * The declared letter, checked against the rendered label.
     *
     * @throws IllegalStateException if the resource declares more or
     *     less than one character, or a character the label does not
     *     contain - either is a mistake in the pack rather than a gap
     *     in it, and a reader would be told to press a key that does
     *     nothing visible
     */
    public char letterFor(String key, String label) {
        String declared = said.say(key);
        if (declared.length() != 1) {
            throw new IllegalStateException("the mnemonic for \"" + key
                    + "\" must be exactly one character, and is \""
                    + declared + "\"");
        }
        char letter = declared.charAt(0);
        if (label == null || label.toUpperCase(java.util.Locale.ROOT)
                .indexOf(Character.toUpperCase(letter)) < 0) {
            throw new IllegalStateException("the mnemonic for \"" + key
                    + "\" is '" + letter + "', which is not in the label"
                    + " it marks: \"" + label + "\". A reader cannot"
                    + " press a letter that is not shown");
        }
        return letter;
    }
}
