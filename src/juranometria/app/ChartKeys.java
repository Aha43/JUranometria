package juranometria.app;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * does it, and - where the letter is not the one on its own
     * control - why.
     *
     * @param id the semantic action, the same string the switches
     *     seam answers to
     * @param label the switch's own name, as its control says it
     * @param key the letter, pressed after the prefix
     * @param note why this letter differs from the control's own
     *     mnemonic, or null when it does not
     * @param dependsOn the switch that must be on before this one
     *     shows anything, or null
     * @param persistent whether the atlas remembers this switch
     *     between sessions - the chart's own layers and the ecliptic
     *     do, the observer's lines do not, and the palette says which
     *     rather than letting a reader assume
     */
    public record Toggle(String id, String label, char key, String note,
                         String dependsOn, boolean persistent) {

        public Toggle {
            if (id == null || label == null) {
                throw new IllegalArgumentException(
                        "a switch needs an action and a name");
            }
        }

        /** What a reader presses, in this platform's own words. */
        public String sequence() {
            return prefixText() + " then "
                    + String.valueOf(key).toUpperCase(Locale.ROOT);
        }
    }

    /** Deep-sky objects, the master of the families and their labels. */
    public static final String DEEP_SKY = "chart.deepSkyObjects";
    /** Constellation figures, the master of the names. */
    public static final String FIGURES = "chart.constellationFigures";

    private static final List<Toggle> TOGGLES = List.of(
            new Toggle(DEEP_SKY, "Deep-sky objects", 'D', null, null,
                    true),
            new Toggle("chart.galaxies", "Galaxies", 'G', null, DEEP_SKY,
                    true),
            new Toggle("chart.openClusters", "Open clusters", 'O', null,
                    DEEP_SKY, true),
            new Toggle("chart.globularClusters", "Globular clusters", 'C',
                    null, DEEP_SKY, true),
            new Toggle("chart.nebulae", "Nebulae", 'U', null, DEEP_SKY, true),
            new Toggle("chart.planetaryNebulae", "Planetary nebulae", 'P',
                    null, DEEP_SKY, true),
            new Toggle("chart.deepSkyLabels", "Deep-sky labels", 'L', null,
                    DEEP_SKY, true),
            new Toggle("chart.starNames", "Star names", 'S', null, null, true),
            new Toggle("chart.bayerLetters", "Bayer letters", 'Y', null,
                    null, true),
            new Toggle("chart.flamsteedNumbers", "Flamsteed numbers", 'M',
                    "F names the figures", null, true),
            new Toggle(FIGURES, "Constellation figures", 'F', null, null, true),
            new Toggle("chart.constellationBoundaries",
                    "Constellation boundaries", 'B', null, null, true),
            new Toggle("chart.constellationNames", "Constellation names",
                    'N', null, FIGURES, true),
            new Toggle("chart.equatorialGrid", "Equatorial grid", 'E', null,
                    null, true),
            new Toggle("chart.titleBlock", "Title block", 'T', null, null, true),
            new Toggle("chart.magnitudeKey", "Stellar-magnitude key",
                    'J', "K darkens the sky", null, true),
            new Toggle("chart.blackSky", "Black sky", 'K',
                    "B bounds the constellations", null, true),
            new Toggle("module.ecliptic", "The ecliptic", 'I', null, null,
                    true),
            // The observer's lines are not written down: Place and
            // Time keeps a place, not a picture.
            new Toggle("module.meridian", "Your meridian", 'R', null, null,
                    false),
            new Toggle("module.horizon", "Your horizon", 'H', null, null,
                    false));

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
        String modifiers = java.awt.event.InputEvent.getModifiersExText(
                prefix().getModifiers());
        String key = KeyEvent.getKeyText(prefix().getKeyCode());
        return modifiers.isEmpty() ? key : modifiers + key;
    }

    /**
     * What the atlas deliberately does not reach from the keyboard,
     * and why - so that "everything the chart shows" stays a claim
     * somebody can check.
     */
    public static Map<String, String> refused() {
        Map<String, String> refused = new LinkedHashMap<>();
        refused.put("Zenith", "controlled in Place and Time — no"
                + " independent shortcut");
        return refused;
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
