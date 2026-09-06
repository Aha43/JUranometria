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
        String subject = scene.title();
        return new SheetMetadata(
                AppInfo.NAME + " chart sheet: " + subject,
                String.format(Locale.ROOT,
                        "%s. Centre RA %.4f, Dec %+.4f (ICRS/J2000)."
                                + " Field %.0f degrees wide, gnomonic."
                                + " Stars to V %.1f. %s. Ground: %s.",
                        subject,
                        scene.viewport().centre().raDegrees(),
                        scene.viewport().centre().decDegrees(),
                        state.fieldWidthDegrees(),
                        state.limitingMagnitude(),
                        paper.describe(),
                        options.palette().storedAs()),
                AppInfo.NAME + " " + AppInfo.version() + ", "
                        + AppInfo.REPO_URL
                        + " - drawn by the application's own chart"
                        + " renderer; no external resources.");
    }
}
