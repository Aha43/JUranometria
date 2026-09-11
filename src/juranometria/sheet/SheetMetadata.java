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
                            ChartOptions options, PaperSize paper) {
        return of(juranometria.project.DrawnPage.of(scene),
                state.fieldWidthDegrees(), state.limitingMagnitude(),
                options, paper);
    }

    /**
     * The same, for a page that carries the projection that drew it
     * (Sprint 32, issue #301; #329 owns the removal).
     *
     * <p>A sheet says what it is, and the projection is part of what
     * it is - which is why this takes the page rather than the scene:
     * the scene's viewport names a <em>kind</em>, and for the length
     * of the celestial-globe gate a study page's kind is not what
     * drew it.
     */
    static SheetMetadata of(juranometria.project.DrawnPage page,
                            double fieldWidthDegrees,
                            double limitingMagnitude,
                            ChartOptions options, PaperSize paper) {
        ChartScene scene = page.scene();
        String subject = scene.title();
        return new SheetMetadata(
                AppInfo.NAME + " chart sheet: " + subject,
                String.format(Locale.ROOT,
                        "%s. Centre RA %.4f, Dec %+.4f (ICRS/J2000)."
                                + " Field %.0f degrees wide, %s."
                                + " Stars to V %.1f. %s. Ground: %s.",
                        subject,
                        scene.viewport().centre().raDegrees(),
                        scene.viewport().centre().decDegrees(),
                        fieldWidthDegrees,
                        // Asked, not asserted. This said "gnomonic"
                        // in every sheet the atlas had ever written,
                        // which was true of every page it could draw
                        // until #299 put three rungs on the ladder
                        // that another projection draws - and then it
                        // was a sheet claiming to be something it was
                        // not, which is worse than a sheet that says
                        // nothing (Sprint 30, issue #300).
                        page.projectionName(),
                        limitingMagnitude,
                        paper.describe(),
                        options.palette().storedAs()),
                AppInfo.NAME + " " + AppInfo.version() + ", "
                        + AppInfo.REPO_URL
                        + " - drawn by the application's own chart"
                        + " renderer; no external resources.");
    }
}
