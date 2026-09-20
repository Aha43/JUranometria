package juranometria.app;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.KeyStroke;

/**
 * The one place that knows which key reaches what a chart shows
 * (Sprint 31, issue #312, from the gate in
 * docs/decisions/chart-toggle-shortcuts.md).
 *
 * <p>A keystroke has three parts that drift apart if they are kept
 * apart: the action it performs, the key itself, and the words a
 * reader is shown for it. Here they are one record. The palette reads
 * this, the tooltips read this, the tests read this, and nothing
 * types a key name into a sentence.
 *
 * <p><strong>The letters are assigned here rather than inherited.</strong>
 * The chart-options dialog's mnemonics only have to be unique on
 * their own tab, and across the dialog two of them are not: `F` is
 * both Flamsteed numbers and Constellation figures, `B` is both
 * Constellation boundaries and Black sky. Three letters therefore
 * differ from the dialog's, and each says so where a reader can see
 * it rather than being quietly different.
 */
public final class ChartKeys {

    private ChartKeys() {
    }

    /**
     * One switch a reader can reach: what it does, the letter that
     * does it, and what the atlas promises about remembering it.
     *
     * <p><strong>No words.</strong> This carried a {@code label} and a
     * {@code note} until #350, and both were English sentences living
     * in an application registry. They are gone rather than
     * deprecated: a member that still exists is a member a new
     * consumer will read, and the whole point of the boundary is that
     * the English cannot be recovered from this type. The words live
     * in {@link juranometria.ui.language.ChartKeyboardText}, keyed by
     * the ids below.
     *
     * <p>{@code sequence()} went the same way. It joined the prefix
     * and the letter with the English word "then", which rode into
     * every translation looking like part of the shortcut. The glyphs
     * and the letter are notation and stay here; where they sit in a
     * sentence, and what goes between them, belong to
     * {@code ShortcutText}.
     *
     * @param id the semantic action, the same string the switches
     *     seam answers to
     * @param key the letter, pressed after the prefix
     * @param dependsOn the switch that must be on before this one
     *     shows anything, or null
     * @param persistent whether the atlas remembers this switch
     *     between sessions - the chart's own layers and the ecliptic
     *     do, the observer's lines do not, and the palette says which
     *     rather than letting a reader assume
     */
    public record Toggle(String id, char key, String dependsOn,
                         boolean persistent) {

        public Toggle {
            if (id == null) {
                throw new IllegalArgumentException(
                        "a switch needs an action");
            }
        }

        /**
         * The second keystroke on its own, as a reader sees it.
         *
         * <p>Notation, and the only reader-facing thing this record
         * still produces: a letter is the same letter in every
         * language.
         */
        public String keyLetter() {
            return String.valueOf(key).toUpperCase(Locale.ROOT);
        }
    }

    /**
     * Something the chart shows that the keyboard deliberately does
     * not switch (#312).
     *
     * <p>Its id is <strong>not</strong> a switch id, and that is the
     * point. {@code chart.zenith} would name a switch
     * {@code ChartSwitches} does not answer to and would throw if
     * anything asked; {@code module.zenith} would suggest the
     * meridian module offers one. This is keyboard policy - a line the
     * palette prints so that "everything the chart shows" stays a
     * claim somebody can check - so it has an identity of its own,
     * and neither the component name nor the resource lookup touches
     * the English word.
     *
     * @param id canonical, lower case, no namespace shared with a
     *     switch
     */
    public record Refusal(String id) {

        public Refusal {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "a refusal is still something with a name");
            }
        }
    }

    /** Deep-sky objects, the master of the families and their labels. */
    public static final String DEEP_SKY = "chart.deepSkyObjects";
    /** Constellation figures, the master of the names. */
    public static final String FIGURES = "chart.constellationFigures";

    private static final List<Toggle> TOGGLES = List.of(
            new Toggle(DEEP_SKY, 'D', null, true),
            new Toggle("chart.galaxies", 'G', DEEP_SKY, true),
            new Toggle("chart.openClusters", 'O', DEEP_SKY, true),
            new Toggle("chart.globularClusters", 'C', DEEP_SKY, true),
            new Toggle("chart.nebulae", 'U', DEEP_SKY, true),
            new Toggle("chart.planetaryNebulae", 'P', DEEP_SKY, true),
            new Toggle("chart.deepSkyLabels", 'L', DEEP_SKY, true),
            new Toggle("chart.starNames", 'S', null, true),
            new Toggle("chart.bayerLetters", 'Y', null, true),
            new Toggle("chart.flamsteedNumbers", 'M', null, true),
            new Toggle(FIGURES, 'F', null, true),
            new Toggle("chart.constellationBoundaries", 'B', null, true),
            new Toggle("chart.constellationNames", 'N', FIGURES, true),
            new Toggle("chart.equatorialGrid", 'E', null, true),
            new Toggle("chart.titleBlock", 'T', null, true),
            new Toggle("chart.magnitudeKey", 'J', null, true),
            new Toggle("chart.blackSky", 'K', null, true),
            new Toggle("module.ecliptic", 'I', null, true),
            // The observer's lines are not written down: Place and
            // Time keeps a place, not a picture.
            new Toggle("module.meridian", 'R', null, false),
            new Toggle("module.horizon", 'H', null, false));

    /** The one thing the palette lists and will not switch. */
    private static final List<Refusal> REFUSED =
            List.of(new Refusal("zenith"));

    /**
     * Every switch the keyboard reaches, in the order the palette
     * lists them - which is the order the chart draws them in, not
     * the order the letters happen to fall.
     */
    public static List<Toggle> toggles() {
        return TOGGLES;
    }

    /** One switch by its action, or null. */
    public static Toggle toggle(String id) {
        for (Toggle toggle : TOGGLES) {
            if (toggle.id().equals(id)) {
                return toggle;
            }
        }
        return null;
    }

    /** One switch by the letter a reader pressed, or null. */
    public static Toggle forKey(char key) {
        char wanted = Character.toUpperCase(key);
        for (Toggle toggle : TOGGLES) {
            if (Character.toUpperCase(toggle.key()) == wanted) {
                return toggle;
            }
        }
        return null;
    }

    /**
     * What the chart's keyboard is opened with: the platform's own
     * menu key and K.
     */
    public static KeyStroke prefix() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_K,
                AppMenuBar.menuShortcutMask());
    }

    /** The prefix in the words a reader is shown. */
    public static String prefixText() {
        // Spelled by the one source that spells every other keystroke
        // the atlas answers (#311), so a tooltip and a menu can never
        // disagree about what this platform calls a modifier.
        return juranometria.ui.Shortcuts.text(prefix());
    }

    /**
     * What the atlas deliberately does not reach from the keyboard -
     * so that "everything the chart shows" stays a claim somebody can
     * check.
     *
     * <p>Identities only. This was a {@code Map<String, String>} whose
     * key and value were both English, and the key doubled as a Swing
     * component name, so translating the map would have renamed a
     * component (#350).
     */
    public static List<Refusal> refused() {
        return REFUSED;
    }

    /**
     * Every letter this map claims, for the audit that proves no two
     * actions claim one.
     */
    public static List<Character> letters() {
        List<Character> letters = new ArrayList<>();
        for (Toggle toggle : TOGGLES) {
            letters.add(Character.toUpperCase(toggle.key()));
        }
        return letters;
    }
}
