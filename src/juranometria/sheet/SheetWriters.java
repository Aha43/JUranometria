package juranometria.sheet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The one place a format becomes bytes (Sprint 29, issue #286).
 *
 * <p>Three writers, one recording, and nothing that chooses between
 * them anywhere else. A reader surface asks for a format and gets a
 * file; it never learns that PDF outlines its text or that PNG has a
 * resolution to state.
 */
public final class SheetWriters {

    private SheetWriters() {
    }

    /**
     * The sheet, written.
     *
     * @param dpi the resolution, used only by the format that has one
     */
    public static byte[] write(SheetRecording sheet, SheetFormat format,
                               int dpi) throws IOException {
        if (sheet == null || format == null) {
            throw new IllegalArgumentException(
                    "a sheet and a format are required");
        }
        return switch (format) {
            // The master keeps its labels as words. The gate chose
            // this over outlines because the reader who asked for the
            // sprint wanted a file he could work with.
            case SVG -> SvgSheetWriter.write(sheet,
                    SvgSheetWriter.Text.EDITABLE)
                    .getBytes(StandardCharsets.UTF_8);
            case PDF -> PdfSheetWriter.write(sheet);
            case PNG -> PngSheetWriter.write(sheet, dpi);
        };
    }
}
