package juranometria.ui;

import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

/**
 * The ink a chart is carrying, as a sheet takes it (Sprint 29,
 * issue #286).
 *
 * <p>The sheet is handed its ink and never goes looking, so the
 * assembling happens here, where both painters live: reference ink
 * for the modules, and - only when the reader asks - the working
 * selection's own crosses.
 *
 * <p><strong>The working selection is off unless asked for.</strong>
 * Rings and crosses mark a reader's transient working set, and a
 * sheet outlives the session that made it: a chart handed to someone
 * else carrying yesterday's half-finished list of objects is telling
 * them something that is not about the sky. The gate decided this and
 * required one explicit control, which #286 provides.
 */
public final class SheetInk {

    private SheetInk() {
    }

    /**
     * The chart's own ink for a sheet.
     *
     * @param chart the chart being exported, asked for what it holds
     * @param leadIdentity the working selection's lead, or null
     * @param workingSelection whether the reader asked for the
     *     transient marks to be included
     */
    public static ChartRenderer.ReferenceLayer of(ChartComponent chart,
                                                  String leadIdentity,
                                                  boolean workingSelection) {
        if (chart == null) {
            throw new IllegalArgumentException("a chart is required");
        }
        return (g, scene) -> {
            // Paper, always: the sheet has already replaced the
            // ground, and the ink has to match the ground it is on.
            ReferenceInk.paint(g, scene, chart.overlays().collect(),
                    ChartPalette.WHITE_PAPER);
            if (workingSelection) {
                WorkingCrossInk.paint(g, scene, chart.overlays().collect(),
                        leadIdentity, ChartPalette.WHITE_PAPER);
            }
        };
    }
}
