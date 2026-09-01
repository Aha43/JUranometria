package juranometria.render;

/**
 * The reader's chart options, per docs/decisions/chart-options.md: an
 * immutable presentation value consumed at the renderer's pass
 * structure - never part of {@code ChartViewState} (it is not
 * navigation), never part of the scene (it is not sky data), and never
 * visible to scene assembly, so every toggle is repaint-only.
 *
 * Enabled means permission to draw where the unchanged policies allow;
 * disabled means never draw - except the searched target, which stays
 * drawn and labelled across every toggle. Two options are dependent:
 * deep-sky labels are effective only while deep-sky objects are on,
 * and constellation names only while figures are on (labels and names
 * attach to drawn ink; detached text is not a chart). Star labels
 * have no dependency: they attach to star dots, which are never
 * optional - and the searched star's guaranteed label survives every
 * identifier toggle exactly as the deep-sky target's does. The three
 * identifier layers (names, Bayer letters, Flamsteed numbers) are
 * independent by the Sprint 17 decision: letters and numbers have
 * measurably different value density. The equatorial grid
 * (ICRS/J2000) likewise has no dependency: pure view geometry with
 * no target of its own.
 */
public record ChartOptions(boolean deepSkyObjects, boolean deepSkyLabels,
                           boolean constellationFigures,
                           boolean constellationBoundaries,
                           boolean constellationNames,
                           boolean starNames, boolean bayerLetters,
                           boolean flamsteedNumbers,
                           boolean equatorialGrid,
                           boolean titleBlock, boolean magnitudeKey,
                           boolean galaxies, boolean openClusters,
                           boolean globularClusters, boolean nebulae,
                           boolean planetaryNebulae) {

    /**
     * The released chart: every layer on, the title block on, and
     * the stellar-magnitude key OFF - the Sprint 20 decision, which
     * measured the key covering up to 436 px of star and symbol ink
     * on a wide page and left it for the reader to ask for.
     */
    public static final ChartOptions DEFAULTS = new ChartOptions(
            true, true, true, true, true, true, true, true, true,
            true, false, true, true, true, true, true);

    /**
     * The chart before the deep-sky families became the reader's
     * (through 1.2.0): every family drew whenever deep-sky objects
     * did. An upgrading store therefore gains all five switched on,
     * and the chart a 1.2.0 reader left behind is the chart they
     * come back to (docs/decisions/deep-sky-vocabulary.md).
     */
    public ChartOptions(boolean deepSkyObjects, boolean deepSkyLabels,
                        boolean constellationFigures,
                        boolean constellationBoundaries,
                        boolean constellationNames,
                        boolean starNames, boolean bayerLetters,
                        boolean flamsteedNumbers,
                        boolean equatorialGrid,
                        boolean titleBlock, boolean magnitudeKey) {
        this(deepSkyObjects, deepSkyLabels, constellationFigures,
                constellationBoundaries, constellationNames, starNames,
                bayerLetters, flamsteedNumbers, equatorialGrid,
                titleBlock, magnitudeKey, true, true, true, true, true);
    }

    /**
     * The chart before the furniture became optional (through
     * 1.1.0): the title block drew always, and there was no key.
     */
    public ChartOptions(boolean deepSkyObjects, boolean deepSkyLabels,
                        boolean constellationFigures,
                        boolean constellationBoundaries,
                        boolean constellationNames,
                        boolean starNames, boolean bayerLetters,
                        boolean flamsteedNumbers,
                        boolean equatorialGrid) {
        this(deepSkyObjects, deepSkyLabels, constellationFigures,
                constellationBoundaries, constellationNames, starNames,
                bayerLetters, flamsteedNumbers, equatorialGrid,
                true, false);
    }

    /**
     * The chart when one control governed every star label (through
     * 0.16.0): that single choice becomes all three identifier
     * layers, the migration the Sprint 17 decision records.
     */
    public ChartOptions(boolean deepSkyObjects, boolean deepSkyLabels,
                        boolean constellationFigures,
                        boolean constellationBoundaries,
                        boolean constellationNames,
                        boolean starLabels,
                        boolean equatorialGrid) {
        this(deepSkyObjects, deepSkyLabels, constellationFigures,
                constellationBoundaries, constellationNames, starLabels,
                starLabels, starLabels, equatorialGrid);
    }

    /** The chart before the equatorial grid existed (through 0.14.0). */
    public ChartOptions(boolean deepSkyObjects, boolean deepSkyLabels,
                        boolean constellationFigures,
                        boolean constellationBoundaries,
                        boolean constellationNames,
                        boolean starLabels) {
        this(deepSkyObjects, deepSkyLabels, constellationFigures,
                constellationBoundaries, constellationNames, starLabels,
                true);
    }

    /** The chart before star labels existed (through 0.12.0). */
    public ChartOptions(boolean deepSkyObjects, boolean deepSkyLabels,
                        boolean constellationFigures,
                        boolean constellationBoundaries,
                        boolean constellationNames) {
        this(deepSkyObjects, deepSkyLabels, constellationFigures,
                constellationBoundaries, constellationNames, true, true);
    }

    /** Whether any star-identifier layer may draw at all. */
    public boolean anyStarLabels() {
        return starNames || bayerLetters || flamsteedNumbers;
    }

    /** Labels depend on symbols: {@code labelled} stays inside {@code drawn}. */
    public boolean effectiveDeepSkyLabels() {
        return deepSkyObjects && deepSkyLabels;
    }

    /**
     * Whether the reader has asked for this family, ignoring the
     * master (Sprint 21, issue #185).
     *
     * <p>A family is one drawn symbol, which is what makes the
     * question answerable at all: the five flags are the renderer's
     * five marks, and every catalogue type reaches its flag through
     * {@link SymbolFamily}. A type the atlas draws nothing for has no
     * family and so no flag - not because it is switched off, but
     * because there is nothing to switch.
     */
    public boolean family(SymbolFamily family) {
        return switch (family) {
            case GALAXIES -> galaxies;
            case OPEN_CLUSTERS -> openClusters;
            case GLOBULAR_CLUSTERS -> globularClusters;
            case NEBULAE -> nebulae;
            case PLANETARY_NEBULAE -> planetaryNebulae;
        };
    }

    /**
     * Whether this family draws: the master and the family together.
     * A family switched off while the master is off stays off when
     * the master comes back - the flags remember, and the master
     * governs (issue #185).
     */
    public boolean effectiveFamily(ChartRenderer.Symbol symbol) {
        SymbolFamily family = SymbolFamily.of(symbol);
        return deepSkyObjects && family != null && family(family);
    }

    /** This chart with one family's flag replaced. */
    public ChartOptions withFamily(SymbolFamily family, boolean enabled) {
        return new ChartOptions(deepSkyObjects, deepSkyLabels,
                constellationFigures, constellationBoundaries,
                constellationNames, starNames, bayerLetters,
                flamsteedNumbers, equatorialGrid, titleBlock, magnitudeKey,
                family == SymbolFamily.GALAXIES ? enabled : galaxies,
                family == SymbolFamily.OPEN_CLUSTERS ? enabled
                        : openClusters,
                family == SymbolFamily.GLOBULAR_CLUSTERS ? enabled
                        : globularClusters,
                family == SymbolFamily.NEBULAE ? enabled : nebulae,
                family == SymbolFamily.PLANETARY_NEBULAE ? enabled
                        : planetaryNebulae);
    }

    /** Names depend on figures: names anchor on visible figure ink. */
    public boolean effectiveConstellationNames() {
        return constellationFigures && constellationNames;
    }
}
