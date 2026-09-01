package juranometria.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.UIManager;

import juranometria.render.ChartRenderer;
import juranometria.render.SymbolFamily;

/**
 * A scrap of the chart's own paper carrying one production symbol
 * (Sprint 21, issue #185).
 *
 * <p>The symbol is painted by
 * {@link ChartRenderer#drawLegendSymbol}, at the family's own
 * exemplar proportions, so what a reader is taught in the dialog is
 * what the page will draw. Nothing here draws a shape of its own: a
 * hand-made icon that merely resembled the chart's mark would be a
 * second vocabulary, free to drift from the first.
 *
 * <p><strong>Why the paper.</strong> Chart ink is mid-grey on white
 * and deliberately never follows the application theme. Painted
 * straight onto the dark theme's panel the outline grey scores
 * 1.85:1, which is invisible; on the chart's own white it scores
 * 5.74:1. The chip is not decoration - it is the only honest way a
 * dark dialog can show what the page draws.
 *
 * <p><strong>Why it never fades.</strong> A family switched off keeps
 * its symbol fully drawn, and only its checkbox and text take the
 * platform's disabled styling. Fading the chip to 45% would take it
 * to 1.97:1, below the floor, in precisely the state where a reader
 * consults the legend to decide what to switch back on. The symbol is
 * information, not a control (docs/decisions/deep-sky-vocabulary.md).
 */
public final class SymbolChip extends JComponent {

    /** The chip's side and the symbol's larger axis, at ordinary text. */
    public static final int CHIP_PX = 22;
    public static final double SYMBOL_PX = 11.0;

    /** The dialog font the chip's proportions were measured against. */
    public static final float BASE_FONT_PX = 13.0f;

    private final SymbolFamily family;

    public SymbolChip(SymbolFamily family) {
        if (family == null) {
            throw new IllegalArgumentException("a family is required");
        }
        this.family = family;
        // The chip keeps pace with the dialog's text: a reader who
        // enlarged the type did not ask for a smaller symbol.
        int side = Math.round(CHIP_PX * textScale());
        setPreferredSize(new Dimension(side, side));
        setMinimumSize(new Dimension(side, side));
        setMaximumSize(new Dimension(side, side));
        getAccessibleContext().setAccessibleName(
                "The symbol the chart draws for " + family.label());
        getAccessibleContext().setAccessibleDescription(family.prose());
    }

    /** How far this session's text has been enlarged past the ordinary. */
    public static float textScale() {
        Font font = UIManager.getFont("Label.font");
        return font == null ? 1.0f : font.getSize2D() / BASE_FONT_PX;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.ICON;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            ChartRenderer.drawLegendSymbol(g, exampleType(family),
                    getWidth() / 2.0, getHeight() / 2.0,
                    SYMBOL_PX * textScale());
            // A hairline edge: white paper on a light dialog is
            // otherwise not visibly a chip at all.
            Color edge = UIManager.getColor("Component.borderColor");
            g.setColor(edge == null ? Color.GRAY : edge);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        } finally {
            g.dispose();
        }
    }

    /** Any catalogue type of this family; they all draw the same mark. */
    public static juranometria.chart.DsoType exampleType(
            SymbolFamily family) {
        for (juranometria.chart.DsoType type
                : juranometria.chart.DsoType.values()) {
            if (SymbolFamily.of(type) == family) {
                return type;
            }
        }
        throw new IllegalStateException("no type draws " + family);
    }
}
