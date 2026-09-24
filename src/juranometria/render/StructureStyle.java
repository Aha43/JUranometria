package juranometria.render;

import java.awt.BasicStroke;
import java.awt.Color;

/**
 * The chart's one style resolver for semantic structures (issue
 * #361).
 *
 * <p>Every painter that inks an emphasizable structure asks here,
 * handing over the palette, the structure it is drawing, whether
 * that structure is the reader's selected target, and the canonical
 * ink and stroke it would otherwise use. The answer is colour and
 * stroke together. Painters do not invent their own accents.
 *
 * <p>An unselected structure gets back exactly what it handed in,
 * so a page with no emphasis - or any layer that is not the target -
 * is canonical by construction, not by luck. An emphasized style
 * modestly raises stroke weight while keeping the dash pattern, so
 * each structure's solid/dashed/dash-dot identity survives, and the
 * distinction survives monochrome conversion through stroke and
 * luminance rather than hue alone.
 *
 * <p>The accent values are the palette study's provisional
 * candidates, one per structure per ground, designed as one family:
 * restrained hues at deliberate luminance distance from their
 * canonical greys. They are frozen only by that study's ruling.
 */
public final class StructureStyle {

    /** Colour and stroke, decided together. */
    public record Style(Color color, BasicStroke stroke) { }

    /** How much an emphasized stroke gains, in pixels. */
    static final float STROKE_GAIN = 0.6f;

    private StructureStyle() {
    }

    /**
     * The style this structure is drawn with.
     *
     * @param canonicalInk    the ink the painter would use anyway
     * @param canonicalStroke the stroke it would use anyway
     */
    public static Style resolve(ChartPalette palette,
                                ChartStructure structure,
                                boolean selected,
                                Color canonicalInk,
                                BasicStroke canonicalStroke) {
        if (!selected) {
            return new Style(canonicalInk, canonicalStroke);
        }
        return new Style(accent(palette, structure),
                emphasized(canonicalStroke));
    }

    /**
     * The canonical stroke, modestly heavier, dash pattern kept: a
     * dashed boundary stays dashed, a dash-dot permanent circle
     * stays dash-dot.
     */
    static BasicStroke emphasized(BasicStroke canonical) {
        return new BasicStroke(canonical.getLineWidth() + STROKE_GAIN,
                canonical.getEndCap(), canonical.getLineJoin(),
                canonical.getMiterLimit(), canonical.getDashArray(),
                canonical.getDashPhase());
    }

    /** The one accent this structure carries on this ground. */
    static Color accent(ChartPalette palette, ChartStructure structure) {
        boolean dark = palette == ChartPalette.BLACK_SKY;
        return switch (structure) {
            case MERIDIAN -> dark
                    // Lighter and warmer than the paper red: under
                    // protanopia the darker candidate collapsed to
                    // 5.5 from its canonical grey, and luminance
                    // must carry what hue cannot (the #361 study).
                    ? new Color(232, 144, 126)
                    : new Color(164, 46, 46);
            case ECLIPTIC -> dark
                    ? new Color(207, 168, 78)    // muted gold
                    // Darker than the first candidate: its luminance
                    // nearly equalled the canonical grey's, leaving
                    // monochrome output the stroke gain alone (1.6
                    // separation, the #361 study).
                    : new Color(130, 97, 15);
            case EQUATORIAL_GRID -> dark
                    ? new Color(122, 152, 199)   // cool blue-grey
                    : new Color(86, 110, 150);
            case HORIZON -> dark
                    ? new Color(139, 176, 146)   // grounded green-grey
                    : new Color(74, 108, 82);
            case CONSTELLATION_BOUNDARIES -> dark
                    ? new Color(163, 132, 199)   // subdued violet
                    : new Color(109, 76, 141);
            case CONSTELLATION_FIGURES -> dark
                    ? new Color(204, 116, 71)    // rust
                    : new Color(148, 79, 48);
        };
    }
}
