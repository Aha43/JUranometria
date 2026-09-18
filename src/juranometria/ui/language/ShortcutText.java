package juranometria.ui.language;

import juranometria.ui.Shortcuts;

/**
 * A control's words joined to the keystroke that does the same thing
 * (Sprint 33, issue #350).
 *
 * <p>{@code Shortcuts.saying} built this by hand:
 *
 * <pre>
 *   return what + " (" + text(id) + ")";
 * </pre>
 *
 * <p>The parenthesis is not punctuation the atlas owns. Where a
 * keystroke goes beside a description, whether it is bracketed, and
 * whether it comes first or last are a language's decisions, and a
 * helper that decides them centrally decides them in English for
 * every language at once. The keystroke itself is the opposite: it is
 * what this desktop calls those keys, and no translation may touch it.
 *
 * <p>So the two halves are separated. {@link Shortcuts} keeps binding
 * identity and platform spelling - it alone knows that this machine
 * says {@code ⌘K} where another says {@code ⌃K}. This class asks the
 * reader's language where the keystroke belongs in the sentence, and
 * hands it over as an argument.
 */
public final class ShortcutText {

    private final InterfaceText said;

    private ShortcutText(InterfaceText said) {
        this.said = said;
    }

    /** The joining pattern for a language a caller states. */
    public static ShortcutText in(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the words have to be in some language");
        }
        return new ShortcutText(said);
    }

    /**
     * A description, and the keystroke that does the same thing.
     *
     * <p>The keystroke is looked up by binding identity, so a hover
     * cannot promise a stroke the menu does not answer, and is
     * spelled by the platform rather than written down.
     */
    public String withKeystroke(String description, String shortcutId) {
        return said.say("shortcut.hovered", description,
                Shortcuts.text(shortcutId));
    }
}
