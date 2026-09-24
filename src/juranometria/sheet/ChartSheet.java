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
                                        PaperSize paper,
                                        juranometria.project.PageWords words) {
        return record(pages, state, options, reference,
                ChartRenderer.ReferenceLayer.NONE, paper, words);
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
                                        PaperSize paper,
                                        juranometria.project.PageWords words) {
        return record(pages, state, options, reference, overChart, paper,
                words, null);
    }

    /**
     * The same, carrying the screen's emphasized structure onto the
     * paper - only when the reader explicitly asked (#361). One
     * recording is made, and every format writes it, so SVG, PDF
     * and PNG cannot disagree about what was emphasized; the
     * metadata records the semantic target as a stable token.
     * {@code null} is the canonical sheet, by the same code path.
     */
    public static SheetRecording record(Pages pages,
                                        ChartViewState state,
                                        ChartOptions options,
                                        ChartRenderer.ReferenceLayer reference,
                                        ChartRenderer.ReferenceLayer overChart,
                                        PaperSize paper,
                                        juranometria.project.PageWords words,
                                        java.util.Set<juranometria.render
                                                .ChartStructure>
                                                emphasized) {
        if (pages == null || state == null || options == null
                || paper == null || words == null) {
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
        // The page's own options, then the paper's ground - the same
        // two steps, in the same order, that the screen takes
        // (Sprint 32, issue #331, step five).
        //
        // Only the palette was applied here, so a sheet was written
        // from the reader's raw switches while the screen drew the
        // page's. On a globe those differ: a hemisphere turns
        // constellation boundaries off, because the gate measured
        // them costing more ink than any other layer at a limb
        // carrying half the sky. An exported globe therefore carried
        // 89,570 px of boundary the reader had never been shown, and
        // the file disagreed with the page it was a copy of.
        //
        // On every page whose sky has no edge onPage is the identity,
        // so every released sheet is unchanged - which the evidence
        // contract holds to the byte.
        ChartOptions onPaper = options
                .onPage(juranometria.project.DrawnPage.of(scene))
                .withPalette(ChartPalette.WHITE_PAPER);

        SheetRecorder recorder = new SheetRecorder(paper.chartWideUnits(),
                paper.chartHighUnits());
        Graphics2D g = (Graphics2D) recorder.create();
        try {
            // One words instance, two consumers. The renderer draws
            // the title block with it and the metadata is written
            // from it, so the pixels and the file's own description
            // cannot end up in different languages (#350).
            new ChartRenderer(StarSizePolicy.DEFAULT, words)
                    .render(g, scene, onPaper, reference, null,
                            emphasized);
            // After the chart, in the order the screen paints it.
            // Nothing is reserved against it: the reader's own marks
            // belong OVER the finished chart, which is the whole of
            // their contract.
            overChart.paint(g, scene, java.util.List.of());
        } finally {
            g.dispose();
        }
        SheetMetadata about =
                SheetMetadata.of(scene, state, onPaper, paper, words);
        return new SheetRecording(recorder, paper, scene, onPaper,
                juranometria.render.ChartStructure
                        .joinedTokens(emphasized)
                        .map(about::withEmphasis).orElse(about));
    }
}
