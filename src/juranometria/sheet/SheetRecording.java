package juranometria.sheet;

import juranometria.chart.ChartScene;
import juranometria.render.ChartOptions;

/**
 * One recorded chart sheet: what the renderer drew, on which paper,
 * for which chart (Sprint 29, issue #285).
 *
 * <p>A writer takes this and nothing else. It carries the scene and
 * the options as well as the recording, so a writer can state what
 * the sheet is without asking the sky anything.
 */
public record SheetRecording(SheetRecorder recorder, PaperSize paper,
                             ChartScene scene, ChartOptions options,
                             SheetMetadata metadata) {

    public SheetRecording {
        if (recorder == null || paper == null || scene == null
                || options == null || metadata == null) {
            throw new IllegalArgumentException(
                    "a recording needs all of its parts");
        }
    }

    /** How many shapes the render put down. */
    public int shapeCount() {
        return recorder.drawn().size();
    }

    /** How many text runs it put down. */
    public int textCount() {
        return recorder.text().size();
    }
}
