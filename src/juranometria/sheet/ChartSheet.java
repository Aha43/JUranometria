package juranometria.sheet;

import java.awt.Graphics2D;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

/**
 * The chart as a sheet of paper (Sprint 29, issue #285).
 *
 * <p>This is the one boundary where a chart stops being a thing on a
 * screen. Everything on the far side of it - SVG here, PDF and PNG
 * in #286 - writes what this records, and nothing on the far side
 * knows anything about the sky.
 *
 * <p><strong>What it is not</strong> matters as much. It is not a
 * screenshot of the chart component: it does not read a single
 * pixel. It is not a second description of a chart assembled from a
 * parallel set of astronomy rules: it holds no rule of its own. It
 * plays <em>the production render</em> - the same
 * {@link ChartRenderer}, over a scene from the same assembler, at the
 * same view state, with the same options and the same module ink -
 * into a {@link SheetRecorder}, and keeps what the renderer did.
 *
 * <p>Two things legitimately differ from the screen, and only two.
 *
 * <p>The first is the <strong>extent</strong>. A page is assembled
 * for the pixels it will occupy, and a sheet's chart rectangle is not
 * the window's shape, so the scene is assembled for the paper. That
 * is the same assembler answering a different question, not a
 * different answer to the same one.
 *
 * <p>The second is the <strong>ground</strong>. The gate rejected
 * printing the black sky: it is a screen decision about a
 * dark-adapted eye, and on paper it asks a reader to lay down a sheet
 * of toner and read white marks out of it. A sheet is always white
 * paper, whatever the screen is showing, and this is where that is
 * enforced rather than left to a caller to remember.
 */
public final class ChartSheet {

    private ChartSheet() {
    }

    /**
     * Where a page comes from: the production scene assembler, named
     * as what the sheet needs of it rather than as itself.
     *
     * <p>The sheet asks for a page of a stated size and is given
     * one. It does not know what a catalogue is, what coverage means,
     * or that the thing answering lives in the user-interface
     * package - and so nothing about paper leaks back the other way.
     */
    @FunctionalInterface
    public interface Pages {

        ChartScene assemble(ChartViewState state, int wide, int high);
    }

    /**
     * Assembles the chart for this paper and records what the
     * renderer draws.
     *
     * @param pages the production scene assembler - the same one the
     *     screen uses, asked for the paper's extent
     * @param state the chart state on screen: centre, field width,
     *     limiting magnitude and target, unchanged
     * @param options the reader's chart options, with the ground
     *     replaced by white paper
     * @param reference the module ink the screen is carrying, as the
     *     production reference layer. {@link
     *     ChartRenderer.ReferenceLayer#NONE} for a chart with no
     *     module showing - the caller passes what the chart has, and
     *     the sheet does not go looking. Never null: a caller that
     *     has not decided is a defect, not a chart without modules
     * @param paper which sheet, and therefore which rectangle
     */
    public static SheetRecording record(Pages pages,
                                        ChartViewState state,
                                        ChartOptions options,
                                        ChartRenderer.ReferenceLayer reference,
                                        PaperSize paper) {
        return record(pages, state, options, reference,
                ChartRenderer.ReferenceLayer.NONE, paper);
    }

    /**
     * The same, with ink that belongs <em>over</em> the chart.
     *
     * <p>Two layers, because the chart has two. A line of reference
     * goes above the grid and below every mark, which is inside the
     * render and the only moment it can be laid down. A reader's own
     * marks - the rings and crosses of a working selection - go
     * after the whole chart, because they are an interaction overlay
     * and not catalogue symbols. Folding the second into the first
     * put a reader's rings underneath the stars they were marking
     * (PR #292 review).
     */
    public static SheetRecording record(Pages pages,
                                        ChartViewState state,
                                        ChartOptions options,
                                        ChartRenderer.ReferenceLayer reference,
                                        ChartRenderer.ReferenceLayer overChart,
                                        PaperSize paper) {
        if (pages == null || state == null || options == null
                || paper == null) {
            throw new IllegalArgumentException(
                    "pages, state, options and paper are required");
        }
        // A chart with no module showing is a real thing and says so
        // by passing NONE. Null is a caller that has not decided, and
        // treating it as "no modules" would turn a miswired export -
        // one that meant to carry the ecliptic and lost it - into a
        // sheet that looks perfectly correct (PR #290 review).
        if (reference == null || overChart == null) {
            throw new IllegalArgumentException(
                    "both layers are required; pass"
                            + " ChartRenderer.ReferenceLayer.NONE for a"
                            + " chart carrying no module ink and none"
                            + " of the reader's own marks");
        }

        ChartScene scene = pages.assemble(state,
                paper.chartWideUnits(), paper.chartHighUnits());
        ChartOptions onPaper = options.withPalette(ChartPalette.WHITE_PAPER);

        SheetRecorder recorder = new SheetRecorder(paper.chartWideUnits(),
                paper.chartHighUnits());
        Graphics2D g = (Graphics2D) recorder.create();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, scene, onPaper, reference);
            // After the chart, in the order the screen paints it.
            overChart.paint(g, scene);
        } finally {
            g.dispose();
        }
        return new SheetRecording(recorder, paper, scene, onPaper,
                SheetMetadata.of(scene, state, onPaper, paper));
    }

    /**
     * A sheet of a page whose projection its viewport does not name
     * (Sprint 32, issue #301).
     *
     * <p><strong>Study only, and temporary: #331 owns its
     * removal</strong>, when a globe is a page a reader can reach and
     * a {@link ChartViewState} can describe one. Until then the sheet
     * path cannot be asked about a hemisphere at all, because a state
     * refuses a 180-degree field - so the gate cannot see what a globe
     * exports without this.
     *
     * <p>Everything else is the production path: the same recorder,
     * the same renderer, the same white-paper palette, the same
     * module layers in the same order, and metadata read from the page
     * that drew it.
     */
    public static SheetRecording recordForStudy(
            juranometria.project.DrawnPage page,
            double fieldWidthDegrees,
            ChartOptions options,
            ChartRenderer.ReferenceLayer reference,
            ChartRenderer.ReferenceLayer overChart,
            PaperSize paper) {
        if (page == null || options == null || paper == null) {
            throw new IllegalArgumentException(
                    "page, options and paper are required");
        }
        if (reference == null || overChart == null) {
            throw new IllegalArgumentException(
                    "both layers are required; pass"
                            + " ChartRenderer.ReferenceLayer.NONE");
        }
        ChartScene scene = page.scene();
        ChartOptions onPaper =
                options.withPalette(ChartPalette.WHITE_PAPER);

        SheetRecorder recorder = new SheetRecorder(
                paper.chartWideUnits(), paper.chartHighUnits());
        Graphics2D g = (Graphics2D) recorder.create();
        try {
            // The plain renderer: a page and its viewport now name
            // the same projection at every rung, including the
            // globe's, so there is nothing left for a renderer built
            // around one page to be told (#329 removed that door).
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, scene, onPaper, reference);
            overChart.paint(g, scene);
        } finally {
            g.dispose();
        }
        return new SheetRecording(recorder, paper, scene, onPaper,
                SheetMetadata.of(page, fieldWidthDegrees,
                        scene.limitingMagnitude(), onPaper, paper));
    }
}
