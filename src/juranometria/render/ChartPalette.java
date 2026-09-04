package juranometria.render;

import java.awt.Color;

/**
 * The chart's two intentional palettes (Sprint 26, issue #246):
 * white paper with black and grey ink - the released chart - and
 * <strong>Black sky</strong>, white stars and restrained light ink
 * on black. A chart choice, not an application theme: a reader may
 * want either chart inside either application chrome, and the
 * application theme never reaches chart ink
 * (docs/application-appearance.md).
 *
 * <p>Black sky is not an inversion. Every ink keeps the <em>WCAG
 * contrast ratio against its ground</em> that it earned on white
 * paper - the derivation the black-sky study executes and this enum
 * pins (docs/decisions/black-sky.md). Because sRGB luminance is
 * non-linear, equal contrast lands the mid-greys well away from
 * their channel mirrors (figure ink 115 where 255-120 would say
 * 135), and systematically dimmer: the restrained light ink the
 * issue asks for falls out of the rule rather than being tuned by
 * hand. The hierarchy order - which ink is more prominent than
 * which - is preserved exactly, because the mapping is monotone.
 *
 * <p>Modules never see this type: they contribute typed geometry
 * and {@link juranometria.module.InkRole}s, and the chart's own
 * ink painters translate role to colour here.
 *
 * <p>The parked Milky Way imagery has <em>no</em> colour here by
 * decision, not by omission: its dark-ground palette is deferred
 * until that source is licensed (issue #246, out of scope).
 */
public enum ChartPalette {

    /** The released chart: black and grey ink on white paper. */
    WHITE_PAPER(
            Color.WHITE,                 // ground
            Color.BLACK,                 // star discs
            new Color(51, 51, 51),       // frame and furniture outlines
            new Color(34, 34, 34),       // label and furniture text
            new Color(120, 120, 120),    // constellation figures
            new Color(190, 190, 190),    // constellation boundaries
            new Color(120, 120, 120),    // constellation names
            new Color(232, 232, 232),    // galaxy ellipse fill
            new Color(102, 102, 102),    // deep-sky outlines
            new Color(132, 132, 132),    // nebula boxes (3.74:1 floor)
            new Color(216, 216, 216),    // equatorial grid
            new Color(150, 150, 150),    // grid notation
            new Color(0x88, 0x88, 0x88), // selection ring
            Color.BLACK),                // interaction crosses

    /**
     * White stars and restrained light ink on black, each value the
     * equal-contrast partner of its white-paper ink, rounded to the
     * nearest sRGB grey. The two values nearest their readability
     * thresholds carry their measured ratios: the nebula box at
     * 3.71:1 stays clear of the 3:1 floor the deep-sky vocabulary
     * decision set, and the galaxy fill at 1.22:1 matches the
     * deliberate whisper of its 1.23:1 on paper.
     */
    BLACK_SKY(
            Color.BLACK,                 // ground
            Color.WHITE,                 // star discs
            new Color(201, 201, 201),    // frame (12.68:1 vs 12.63:1)
            new Color(224, 224, 224),    // text (15.91:1)
            new Color(115, 115, 115),    // figures (4.43:1 vs 4.42:1)
            new Color(58, 58, 58),       // boundaries (1.85:1 vs 1.86:1)
            new Color(115, 115, 115),    // constellation names
            new Color(27, 27, 27),       // galaxy fill (1.22:1)
            new Color(134, 134, 134),    // deep-sky outlines (5.77:1)
            new Color(103, 103, 103),    // nebula boxes (3.71:1)
            new Color(40, 40, 40),       // grid (1.42:1 vs 1.43:1)
            new Color(88, 88, 88),       // grid notation (2.95:1)
            new Color(100, 100, 100),    // selection ring (3.55:1)
            Color.WHITE);                // interaction crosses

    private final Color ground;
    private final Color starInk;
    private final Color frameInk;
    private final Color textInk;
    private final Color figureInk;
    private final Color boundaryInk;
    private final Color constellationNameInk;
    private final Color galaxyFill;
    private final Color deepSkyOutline;
    private final Color nebulaOutline;
    private final Color gridInk;
    private final Color gridLabelInk;
    private final Color selectionInk;
    private final Color interactionInk;

    ChartPalette(Color ground, Color starInk, Color frameInk,
                 Color textInk, Color figureInk, Color boundaryInk,
                 Color constellationNameInk, Color galaxyFill,
                 Color deepSkyOutline, Color nebulaOutline,
                 Color gridInk, Color gridLabelInk, Color selectionInk,
                 Color interactionInk) {
        this.ground = ground;
        this.starInk = starInk;
        this.frameInk = frameInk;
        this.textInk = textInk;
        this.figureInk = figureInk;
        this.boundaryInk = boundaryInk;
        this.constellationNameInk = constellationNameInk;
        this.galaxyFill = galaxyFill;
        this.deepSkyOutline = deepSkyOutline;
        this.nebulaOutline = nebulaOutline;
        this.gridInk = gridInk;
        this.gridLabelInk = gridLabelInk;
        this.selectionInk = selectionInk;
        this.interactionInk = interactionInk;
    }

    /** The sky the page is filled with, and furniture interiors. */
    public Color ground() {
        return ground;
    }

    /** Star discs and the magnitude key's sample circles. */
    public Color starInk() {
        return starInk;
    }

    /** The page border and furniture outlines. */
    public Color frameInk() {
        return frameInk;
    }

    /** Star and deep-sky labels, and furniture text. */
    public Color textInk() {
        return textInk;
    }

    /** Constellation figures, and reference lines with them. */
    public Color figureInk() {
        return figureInk;
    }

    /** Constellation boundaries: the quietest structural whisper. */
    public Color boundaryInk() {
        return boundaryInk;
    }

    /** Constellation names, anchored on figure ink. */
    public Color constellationNameInk() {
        return constellationNameInk;
    }

    /** The pale wash inside a galaxy ellipse. */
    public Color galaxyFill() {
        return galaxyFill;
    }

    /** Ellipse, dotted, crossed and planetary outlines. */
    public Color deepSkyOutline() {
        return deepSkyOutline;
    }

    /** The nebula box, held to its 3:1 floor on either ground. */
    public Color nebulaOutline() {
        return nebulaOutline;
    }

    /** The equatorial graticule. */
    public Color gridInk() {
        return gridInk;
    }

    /** Grid notation, and reference-line labels with it. */
    public Color gridLabelInk() {
        return gridLabelInk;
    }

    /** The selection ring: the reader's, not the sky's. */
    public Color selectionInk() {
        return selectionInk;
    }

    /** Working crosses: as prominent as a star, like on paper. */
    public Color interactionInk() {
        return interactionInk;
    }

    /** The token this choice persists as. */
    public String storedAs() {
        return this == BLACK_SKY ? "black-sky" : "white-paper";
    }

    /**
     * The palette a stored token names. Unknown or corrupt values
     * mean the released white paper, never a launch failure - the
     * chart-options store's standing rule.
     */
    public static ChartPalette stored(String token) {
        return "black-sky".equals(token) ? BLACK_SKY : WHITE_PAPER;
    }
}
