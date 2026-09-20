package juranometria.sheet;

import java.util.Locale;

import juranometria.app.AppInfo;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.render.ChartOptions;

/**
 * What a sheet says about itself (Sprint 29, issue #285).
 *
 * <p>A sheet printed for a club evening outlives the session that
 * made it and travels to people who were not there. It has to be able
 * to answer, on its own: what part of the sky is this, in what frame,
 * how deep, how wide, on what paper, and what made it.
 *
 * <p>It answers nothing else. There is no filename, no user name and
 * no directory here: a reader sending a chart to a mailing list is
 * not offering to send their home directory with it.
 *
 * <p>"What part of the sky, how wide" includes <strong>which
 * projection drew it</strong> (Sprint 30, issue #300). Two sheets of
 * the same centre and field drawn differently are different
 * documents, and a sheet that could not say which one it was could
 * not be checked.
 */
public record SheetMetadata(String title, String description,
                            String producedBy) {

    public SheetMetadata {
        if (title == null || title.isBlank()
                || description == null || description.isBlank()
                || producedBy == null || producedBy.isBlank()) {
            throw new IllegalArgumentException(
                    "a sheet states what it is; blank is not a"
                            + " statement");
        }
    }

    static SheetMetadata of(ChartScene scene, ChartViewState state,
                            ChartOptions options, PaperSize paper,
                            juranometria.project.PageWords words) {
        return of(juranometria.project.DrawnPage.of(scene),
                state.fieldWidthDegrees(), state.limitingMagnitude(),
                options, paper, words);
    }

    /**
     * What an exported file says about itself, in a stated language
     * (Sprint 33, issue #350).
     *
     * <p>This assembled four English sentences out of literals, a
     * paper's own {@code describe()} and a palette's stored token -
     * and every SVG, PDF and PNG the atlas has ever written carried
     * them. A Norwegian reader exporting a Norwegian chart got an
     * English description inside the file, which #349 forbids: an
     * export says what the screen says.
     *
     * <p>It assembles nothing now. Three complete patterns, and the
     * words come from the <em>same</em> {@link PageWords} instance
     * the renderer drew the page with - passed down from
     * {@code ExportSheet.write}, not resolved again here, so the
     * pixels and the metadata cannot disagree about the language.
     *
     * <p>Numbers keep {@code Locale.ROOT}. The palette's stored token
     * stays the stored token and is mapped to a reader's phrase;
     * printing it raw inside a sentence is what this replaces.
     */
    static SheetMetadata of(juranometria.project.DrawnPage page,
                            double fieldWidthDegrees,
                            double limitingMagnitude,
                            ChartOptions options, PaperSize paper,
                            juranometria.project.PageWords words) {
        if (words == null) {
            throw new IllegalArgumentException("an exported file says"
                    + " what it is in some language (#350)");
        }
        ChartScene scene = page.scene();
        String subject = scene.title();
        return new SheetMetadata(
                words.sheetTitle(AppInfo.NAME, subject),
                words.sheetDescription(
                        subject,
                        String.format(Locale.ROOT, "%.4f",
                                scene.viewport().centre().raDegrees()),
                        String.format(Locale.ROOT, "%+.4f",
                                scene.viewport().centre().decDegrees()),
                        String.format(Locale.ROOT, "%.0f",
                                fieldWidthDegrees),
                        // Asked, not asserted. This said "gnomonic"
                        // in every sheet the atlas had ever written,
                        // which was true of every page it could draw
                        // until #299 put three rungs on the ladder
                        // that another projection draws - and then it
                        // was a sheet claiming to be something it was
                        // not, which is worse than a sheet that says
                        // nothing (Sprint 30, issue #300).
                        words.projection(page.projectionName()),
                        String.format(Locale.ROOT, "%.1f",
                                limitingMagnitude),
                        words.paper(paper.identity(),
                                String.format(Locale.ROOT, "%.1f",
                                        paper.wideMm()),
                                String.format(Locale.ROOT, "%.1f",
                                        paper.highMm()),
                                String.format(Locale.ROOT, "%.1f",
                                        paper.marginMm()),
                                String.format(Locale.ROOT, "%.1f",
                                        paper.chartWideMm()),
                                String.format(Locale.ROOT, "%.1f",
                                        paper.chartHighMm())),
                        words.ground(options.palette().storedAs())),
                words.producedBy(AppInfo.NAME + " " + AppInfo.version(),
                        AppInfo.REPO_URL));
    }
}
