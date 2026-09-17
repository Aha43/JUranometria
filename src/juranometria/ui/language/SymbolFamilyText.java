package juranometria.ui.language;

import juranometria.render.SymbolFamily;

/**
 * What a deep-sky family is called, in the reader's interface
 * language (Sprint 33, issue #350).
 *
 * <p>Owner ruling, 2026-09-17. {@code SymbolFamily} owns the family's
 * <em>identity</em>, its symbol geometry and rendering policy, and its
 * catalogue examples as canonical identifiers. It does not own English
 * labels, reader descriptions, sentence construction, or keys tied to
 * one surface. Those are presentation, and this is where they live.
 *
 * <p>One adapter, not one per consumer. Chart Options, the Inspector,
 * the legends, accessibility and exported chart furniture all ask the
 * same question - <em>what is this family called, and what is it?</em>
 * - and a translation written twice is a translation that drifts. A
 * separate key is right only where a sentence needs different grammar
 * in a different position, not merely because a different class is
 * asking.
 *
 * <p>Which language governs which words was settled by the gate:
 * constellation names follow the <strong>chart</strong> language;
 * "Galaxies", "Open clusters" and their explanatory prose follow the
 * <strong>interface</strong> language; and {@code M 31},
 * {@code NGC 3628} and the family's canonical identity are data that
 * no language changes.
 *
 * <p>The renderer knows none of this. It is handed a family and draws
 * its symbol; it never learns which language is selected and never
 * assembles a sentence.
 */
public final class SymbolFamilyText {

    private final InterfaceText said;

    private SymbolFamilyText(InterfaceText said) {
        this.said = said;
    }

    /** The family words for a language a caller states. */
    public static SymbolFamilyText in(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the words have to be in some language");
        }
        return new SymbolFamilyText(said);
    }

    /** What this family is called, on a control or in a legend. */
    public String label(SymbolFamily family) {
        return said.say(key(family) + ".label");
    }

    /** The name assistive technology speaks for it. */
    public String accessibleName(SymbolFamily family) {
        return said.say(key(family) + ".a11y");
    }

    /**
     * What this family is, as a whole sentence.
     *
     * <p>The catalogue examples go in as data. They are identifiers -
     * {@code M 31}, {@code NGC 3628} - and a translation places them
     * where its own grammar wants them rather than receiving a
     * sentence already assembled in English order.
     */
    public String description(SymbolFamily family) {
        return said.say(key(family) + ".explain", family.examples());
    }

    /**
     * The interface-language key stem for a family.
     *
     * <p>Derived from the canonical identity rather than from a
     * label, so renaming what a family is CALLED never silently
     * changes which resource answers for it.
     */
    private static String key(SymbolFamily family) {
        return switch (family) {
            case GALAXIES -> "chartoptions.galaxies";
            case OPEN_CLUSTERS -> "chartoptions.openClusters";
            case GLOBULAR_CLUSTERS -> "chartoptions.globularClusters";
            case NEBULAE -> "chartoptions.nebulae";
            case PLANETARY_NEBULAE -> "chartoptions.planetaryNebulae";
        };
    }
}
