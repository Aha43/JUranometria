package juranometria.ui;

import juranometria.render.ChartOptions;
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
     * The modules' ink: a reference layer, which belongs inside the
     * render, above the grid and below every mark.
     */
    public static ChartRenderer.ReferenceLayer reference(
            ChartComponent chart) {
        if (chart == null) {
            throw new IllegalArgumentException("a chart is required");
        }
        // Paper, always: the sheet has already replaced the ground,
        // and the ink has to match the ground it is on.
        return (g, scene) -> ReferenceInk.paint(g, scene,
                chart.overlays().collect(), ChartPalette.WHITE_PAPER);
    }

    /**
     * The reader's own marks, which belong over the finished chart.
     *
     * <p>Both halves of them, in the order the screen paints them: a
     * ring around every marked object the page actually draws, and a
     * cross for every one it does not. An earlier version drew only
     * the crosses, and drew them inside the reference layer - so a
     * reader who asked for their marks got half of them, underneath
     * the stars they were marking (PR #292 review).
     *
     * @param members the working selection, in membership order
     * @param leadIdentity the member that leads, or null
     */
    public static ChartRenderer.ReferenceLayer working(
            ChartComponent chart, java.util.List<String> members,
            String leadIdentity, ChartOptions options) {
        if (chart == null || members == null || options == null) {
            throw new IllegalArgumentException(
                    "a chart, its members and its options are required");
        }
        ChartOptions onPaper =
                options.withPalette(ChartPalette.WHITE_PAPER);
        ChartRenderer renderer = new ChartRenderer(
                juranometria.chart.StarSizePolicy.DEFAULT);
        return (g, scene) -> {
            for (String member : members) {
                renderer.drawSelectionHighlight(g, scene, onPaper, member);
            }
            WorkingCrossInk.paint(g, scene, chart.overlays().collect(),
                    leadIdentity, ChartPalette.WHITE_PAPER);
        };
    }
}
