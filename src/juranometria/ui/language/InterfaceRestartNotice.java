package juranometria.ui.language;

/**
 * What a reader is told when they change the interface language
 * (Sprint 33, issue #350).
 *
 * <p>The choice is <strong>restart-bound</strong>, and until this
 * existed nothing said so at the moment it mattered. The owner found
 * it the honest way: chose Norsk bokmål, and the chart's title block
 * went on saying <em>Centre</em> and <em>Field</em>. It was not a
 * missing translation - the words were there and a restart produced
 * them - it was a session that had changed in some places and not
 * others, with nothing to explain the difference.
 *
 * <p>It really is mixed, and that is worth being exact about. A
 * dialog opened <em>after</em> the change resolves its words when it
 * opens, so Chart Options, About, Place and Time and the export all
 * switch at once. The menu bar, the toolbar and the drawn page were
 * built at startup from the language of that startup, and they keep
 * it. So the reader sees the new language spread unevenly, which
 * looks like a defect until someone says otherwise.
 *
 * <p>Hence "throughout the application": not <em>nothing has
 * happened</em>, which would be false, but <em>the rest of it
 * happens when you start again</em>. The title states the boundary -
 * a restart is required - and the body repeats what was saved, so
 * neither half depends on the reader having read the other first.
 *
 * <p><strong>This decides; it does not show.</strong> Returning the
 * words rather than opening a dialog is what lets the rule - exactly
 * once, for a genuinely changed interface language, and never for
 * anything else - be asserted without a window.
 */
public final class InterfaceRestartNotice {

    private InterfaceRestartNotice() {
    }

    /** A title and a message, in one language. */
    public record Said(String title, String message) {
    }

    private static final String TITLE =
            "settings.language.interface.restart.title";

    private static final String MESSAGE =
            "settings.language.interface.restart.message";

    /**
     * What to tell the reader, or {@code null} to tell them nothing.
     *
     * <p>Nothing is the answer for every case except one: the
     * interface language the reader settled on differs from the one
     * they had. Confirming unchanged values says nothing. Changing
     * only the chart language says nothing - that one applies at
     * once and needs no restart. Opening Settings and reading it
     * says nothing, and neither does Cancel, because neither reaches
     * here.
     *
     * @param was the interface language before the reader confirmed
     * @param now the interface language they confirmed
     */
    public static Said forChoice(String was, String now) {
        if (now == null || now.equals(was)) {
            return null;
        }
        Said said = inLanguage(now);
        // The new language first, because a reader who has just
        // asked for Norwegian should be answered in Norwegian. If
        // that language cannot produce BOTH halves, the one they
        // have been reading does - and if neither can, they are told
        // nothing rather than half a sentence.
        return said != null ? said : inLanguage(was);
    }

    /**
     * Both halves from one language, or neither.
     *
     * <p>Atomic on purpose. A title in one language over a message
     * in another is worse than either alone: it reads as a defect in
     * the thing it is explaining.
     */
    private static Said inLanguage(String tag) {
        if (tag == null) {
            return null;
        }
        try {
            InterfaceText said = InterfaceText.forLanguage(tag);
            String title = said.sayIfDefined(TITLE);
            String message = said.sayIfDefined(MESSAGE);
            return title == null || message == null
                    ? null : new Said(title, message);
        } catch (RuntimeException | Error refused) {
            // A language that will not load must not take the
            // reader's settings down with it. They asked to save a
            // choice, and the choice is already saved by the time
            // anyone asks what to say about it.
            return null;
        }
    }
}
