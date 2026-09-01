package juranometria.render;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;

/**
 * The five deep-sky families a reader sees, decided by the Sprint 21
 * gate (docs/decisions/deep-sky-vocabulary.md, issue #184).
 *
 * <p>A family is <strong>exactly one drawn symbol</strong>. That is
 * what makes the grouping checkable rather than a matter of taste: a
 * family is the set of catalogue types the chart already draws
 * identically, so a reader who learns the mark has learned the
 * family. Fourteen of the pack's nineteen source types reach a family
 * this way, and the other five draw nothing at all.
 *
 * <p>The words live here, beside the symbols, because the dialog that
 * offers them, the study that reviews them and the chart that draws
 * them must not each keep their own copy. No raw OpenNGC token
 * appears anywhere a reader can see; the tokens stay in the pack, the
 * decision and the study.
 */
public enum SymbolFamily {

    GALAXIES(ChartRenderer.Symbol.ELLIPSE, "Galaxies", 'G',
            "Galaxies, drawn at their catalogued size and orientation,"
                    + " including close pairs, triplets and groups.",
            "M 31, M 51, NGC 3628"),

    OPEN_CLUSTERS(ChartRenderer.Symbol.DOTTED_CIRCLE, "Open clusters", 'O',
            "Loose clusters of young stars in the plane of the Milky"
                    + " Way.",
            "M 45, M 44, NGC 869"),

    GLOBULAR_CLUSTERS(ChartRenderer.Symbol.CROSSED_CIRCLE,
            "Globular clusters", 'C',
            "Dense, ancient balls of stars in the galactic halo.",
            "M 13, M 22, NGC 5139"),

    NEBULAE(ChartRenderer.Symbol.BOX, "Nebulae", 'U',
            "Clouds of gas and dust: emission, reflection and dark"
                    + " nebulae, H II regions, supernova remnants, and"
                    + " clusters still wrapped in nebulosity.",
            "M 42, M 1, NGC 7000"),

    PLANETARY_NEBULAE(ChartRenderer.Symbol.PLANETARY, "Planetary nebulae",
            'P',
            "Shells thrown off by dying stars, drawn small and crossed"
                    + " so they read apart from the other nebulae.",
            "M 57, M 27, NGC 7009");

    private final ChartRenderer.Symbol symbol;
    private final String label;
    private final char mnemonic;
    private final String description;
    private final String examples;

    SymbolFamily(ChartRenderer.Symbol symbol, String label, char mnemonic,
                 String description, String examples) {
        this.symbol = symbol;
        this.label = label;
        this.mnemonic = mnemonic;
        this.description = description;
        this.examples = examples;
    }

    /** The one chart symbol this family is. */
    public ChartRenderer.Symbol symbol() {
        return symbol;
    }

    /** The name a reader sees, on the control and in the legend. */
    public String label() {
        return label;
    }

    /**
     * The letter this family answers to. The five were chosen so that
     * every control existing in 1.2.0 keeps the mnemonic it had, and
     * so that no two controls on one tab share a letter - which is
     * the collision that matters, since a mnemonic only reaches the
     * tab in front.
     */
    public char mnemonic() {
        return mnemonic;
    }

    /** What the family is, in one sentence. */
    public String description() {
        return description;
    }

    /** Objects a reader may already know. */
    public String examples() {
        return examples;
    }

    /**
     * The whole explanation: the sentence and its examples. This is
     * both the visible text of a legend row and the control's
     * accessible description, so the two cannot come to differ and no
     * meaning depends on hovering.
     */
    public String prose() {
        return description + " For example: " + examples + ".";
    }

    /** The family that draws this symbol, or null for one that draws none. */
    public static SymbolFamily of(ChartRenderer.Symbol symbol) {
        for (SymbolFamily family : values()) {
            if (family.symbol == symbol) {
                return family;
            }
        }
        return null;
    }

    /** The family a catalogue type belongs to, or null when undrawn. */
    public static SymbolFamily of(DsoType type) {
        return of(ChartRenderer.symbolForType(type));
    }

    /** The family an object belongs to, or null when it draws nothing. */
    public static SymbolFamily of(DeepSkyObject dso) {
        return of(ChartRenderer.symbolFor(dso));
    }
}
