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
 * optional - and the searched star's guaranteed label survives the
 * toggle exactly as the deep-sky target's does. The equatorial grid
 * (ICRS/J2000) likewise has no dependency: pure view geometry with
 * no target of its own.
 */
public record ChartOptions(boolean deepSkyObjects, boolean deepSkyLabels,
                           boolean constellationFigures,
                           boolean constellationBoundaries,
                           boolean constellationNames,
                           boolean starLabels,
                           boolean equatorialGrid) {

    /** The released chart: everything on. */
    public static final ChartOptions DEFAULTS =
            new ChartOptions(true, true, true, true, true, true, true);

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

    /** Labels depend on symbols: {@code labelled} stays inside {@code drawn}. */
    public boolean effectiveDeepSkyLabels() {
        return deepSkyObjects && deepSkyLabels;
    }

    /** Names depend on figures: names anchor on visible figure ink. */
    public boolean effectiveConstellationNames() {
        return constellationFigures && constellationNames;
    }
}
