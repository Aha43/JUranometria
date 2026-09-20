package juranometria.ui.language;

import juranometria.app.ChartKeys;

/**
 * Every word the chart's keyboard says (Sprint 33, issue #350).
 *
 * <p>{@code ChartKeys} keeps what a switch <em>is</em> - its action,
 * its letter, what it waits for, whether the atlas remembers it. This
 * keeps what a reader is <em>told</em>. The registry carried an
 * English label and an English note per switch until now, which is the
 * fourth and fifth instance of a defect already repaired in
 * {@code SymbolFamily}, {@code PageVisibility}, {@code SheetFormat} and
 * {@code PaperSize}: a type that owns identity had grown prose.
 *
 * <p><strong>Sentences are whole.</strong> The palette assembled
 * eleven of them out of Java string addition - a label, a dash, a
 * state, a note in brackets; a label, a comma, "unavailable until", a
 * master, "is on", a shortcut. Every one is now a single pattern with
 * numbered arguments, so a language decides its own word order. The
 * one that proves it is the dependent row: four ordered pieces, two
 * joins and a keystroke, which no language can be expected to keep in
 * English order.
 *
 * <p><strong>A master's name is not its label.</strong>
 * {@code ChartKeyboard} lowercased a label with {@code Locale.ROOT} to
 * drop it into a sentence. Whether a noun is lower case inside a
 * sentence is a language's rule and not the atlas's - German would be
 * wrong outright - so each master declares the forms it needs instead:
 * {@link #masterToEnable} for "enable X first", and
 * {@link #masterSatisfied} for the clause that says it is on, which is
 * where number agreement lives ("Deep-sky objects <em>are</em> on").
 *
 * <p><strong>It refuses rather than falls back.</strong> A registered
 * switch with no English text is a mistake in the resources, not a
 * switch without a name, and English is what every other language
 * falls back to - so a gap there is a gap everywhere. Asking for a
 * missing label throws rather than returning the id: an id that
 * reached a reader would be a defect wearing the shape of a word.
 */
public final class ChartKeyboardText {

    /**
     * What a language writes where it has no note to give.
     *
     * <p>Notes explain why a letter differs from the one on the
     * switch's own control - "F names the figures". That is a fact
     * about <em>this language's</em> mnemonics: another language's
     * labels may collide differently, or not at all, so which switches
     * carry a note is a language's decision and not the registry's.
     *
     * <p>Which means silence has to be said out loud. A missing key
     * and a deliberate "no note here" look identical if absence is
     * expressed by absence, and the first is a mistake the pack should
     * be told about. Every switch declares a note or declares this.
     * It is not a word in any language, so nothing will translate it
     * by accident.
     */
    public static final String NO_NOTE = "!none";

    private static final String STEM = "chartkeyboard.";

    private final InterfaceText said;
    private final ShortcutText shortcuts;

    private ChartKeyboardText(InterfaceText said) {
        this.said = said;
        this.shortcuts = ShortcutText.in(said);
    }

    /** The keyboard's words in a language a caller states. */
    public static ChartKeyboardText in(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the words have to be in some language");
        }
        return new ChartKeyboardText(said);
    }

    /** The language these words are in, for callers that pass it on. */
    public InterfaceText words() {
        return said;
    }

    // ---- the palette itself ----------------------------------------

    /** What a screen reader calls the palette. */
    public String title() {
        return said.say(STEM + "title");
    }

    /**
     * What the palette says it is for.
     *
     * <p>It no longer claims to list "every layer the chart can show":
     * the zenith is listed and deliberately not switchable, so that
     * claim was contradicted by a row of the same panel.
     */
    public String explain() {
        return said.say(STEM + "explain", ChartKeys.prefixText());
    }

    /** The heading over the rows. */
    public String heading() {
        return said.say(STEM + "heading");
    }

    /**
     * The line telling a reader what to press.
     *
     * <p>"Escape or {@code ⌘K} closes without changing anything" was
     * misleading - it read as though the letters did not change
     * anything either, when a letter is the whole gesture and there is
     * nothing to take back.
     */
    public String instruction() {
        return said.say(STEM + "instruction", ChartKeys.prefixText());
    }

    // ---- one switch ------------------------------------------------

    /** What a switch is called. */
    public String label(ChartKeys.Toggle toggle) {
        return required(key(toggle, "label"));
    }

    /**
     * Why this letter differs from the one on the switch's own
     * control, or null when this language has nothing to explain.
     *
     * @throws IllegalStateException if the pack declares neither a
     *     note nor {@link #NO_NOTE} - see that constant
     */
    public String note(ChartKeys.Toggle toggle) {
        String declared = required(key(toggle, "note"));
        return NO_NOTE.equals(declared) ? null : declared;
    }

    /** The name of a master, as it reads after "enable". */
    public String masterToEnable(ChartKeys.Toggle master) {
        return required(key(master, "master.enable"));
    }

    /** The clause saying a master is on, agreement included. */
    public String masterSatisfied(ChartKeys.Toggle master) {
        return required(key(master, "master.on"));
    }

    /** On, or off, as this language says it. */
    public String state(boolean on) {
        return said.say(STEM + (on ? "state.on" : "state.off"));
    }

    /** What a row not yet reachable says instead of on or off. */
    public String stateUnavailable(ChartKeys.Toggle master) {
        return said.say(STEM + "state.unavailable",
                masterToEnable(master));
    }

    /**
     * One row of the palette, whole.
     *
     * <p>Letter, name, state and - where there is one - the note, in
     * whatever order and with whatever punctuation the language
     * chooses. The brackets around a note were Java's.
     */
    public String row(ChartKeys.Toggle toggle, String state) {
        String note = note(toggle);
        return note == null
                ? said.say(STEM + "row", toggle.keyLetter(),
                        label(toggle), state)
                : said.say(STEM + "row.noted", toggle.keyLetter(),
                        label(toggle), state, note);
    }

    /** What a screen reader is told about a reachable row. */
    public String spoken(ChartKeys.Toggle toggle, boolean on) {
        return said.say(STEM + "spoken", label(toggle), state(on),
                sequence(toggle));
    }

    /**
     * What a screen reader is told about a row that is waiting.
     *
     * <p>The stress case for the whole surface: a name, a condition
     * naming another switch, and a two-keystroke shortcut. Assembled
     * in Java it could only ever come out in English order.
     */
    public String spokenUnavailable(ChartKeys.Toggle toggle,
                                    ChartKeys.Toggle master) {
        return said.say(STEM + "spoken.unavailable", label(toggle),
                masterSatisfied(master), sequence(toggle));
    }

    /** What the palette says out loud when a switch is thrown. */
    public String announce(ChartKeys.Toggle toggle, boolean on) {
        return said.say(STEM + (toggle.persistent()
                        ? "announce.saved" : "announce.session"),
                label(toggle), state(on));
    }

    /** What it says when the letter reaches a switch that is waiting. */
    public String announceUnavailable(ChartKeys.Toggle toggle,
                                      ChartKeys.Toggle master) {
        return said.say(STEM + "announce.unavailable", label(toggle),
                masterToEnable(master));
    }

    /**
     * The keystrokes, joined as this language joins them.
     *
     * <p>Through {@link ShortcutText}, which owns the word between two
     * keystrokes. {@code ChartKeys.Toggle.sequence()} froze it as the
     * English "then"; this was its last caller in the application.
     */
    public String sequence(ChartKeys.Toggle toggle) {
        return shortcuts.sequence(ChartKeys.prefixText(),
                toggle.keyLetter());
    }

    // ---- what the keyboard will not switch -------------------------

    /** The refused row, as it is shown. */
    public String refusedRow(ChartKeys.Refusal refusal) {
        return required(STEM + "refused." + refusal.id() + ".row");
    }

    /** The refused row, as it is spoken. */
    public String refusedSpoken(ChartKeys.Refusal refusal) {
        return required(STEM + "refused." + refusal.id() + ".spoken");
    }

    private static String key(ChartKeys.Toggle toggle, String role) {
        return STEM + toggle.id() + "." + role;
    }

    /**
     * A value the pack must carry.
     *
     * <p>{@code InterfaceText} answers a missing key with the key
     * itself, which is right for a surface that would rather show
     * something than nothing. It is wrong here: these are a fixed,
     * enumerable set that a test can walk, and a switch showing
     * {@code chartkeyboard.chart.nebulae.label} is a defect that looks
     * like a word. A new switch added to the registry with no text
     * fails here, loudly, rather than appearing blank.
     */
    private String required(String key) {
        String value = said.say(key);
        if (value == null || value.isBlank() || value.equals(key)) {
            throw new IllegalStateException("the chart keyboard has no"
                    + " words for \"" + key + "\". Every registered"
                    + " switch needs them in English, because English"
                    + " is what every other language falls back to -"
                    + " a gap here is a gap in every language at"
                    + " once.");
        }
        return value;
    }
}
