package juranometria.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import juranometria.chart.ChartViewState;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;

/**
 * Writing a chart sheet where a reader asked for it (Sprint 29,
 * issue #286).
 *
 * <p>The part of exporting that is not drawing: which file, whether
 * it can be written, what it is called, and what to say when it
 * cannot. The rule the issue sets is the one worth stating - a
 * failure must never leave something that looks like a finished
 * export - so the bytes are made first and the destination is written
 * in one go, and a destination that cannot be written is refused
 * before anything is claimed.
 */
public final class ExportSheet {

    private ExportSheet() {
    }

    /** What the reader chose, and what the chart was showing. */
    public record Request(SheetFormat format, PaperSize paper, int dpi,
                          boolean workingSelection) {

        public Request {
            if (format == null || paper == null) {
                throw new IllegalArgumentException(
                        "a format and a paper are required");
            }
            if (dpi <= 0) {
                throw new IllegalArgumentException(
                        "a resolution is a positive number of dots per"
                                + " inch: " + dpi);
            }
        }
    }

    /** What happened, in words a dialog can show a reader. */
    public sealed interface Outcome {

        /** The file is on disk and complete. */
        record Written(Path file, long bytes, SheetFormat format)
                implements Outcome {

            public String message() {
                return String.format(Locale.ROOT,
                        "%s written to %s (%,d bytes)",
                        format.readableName(), file.getFileName(), bytes);
            }
        }

        /** Nothing was written, and the reason says what to do. */
        record Refused(String reason) implements Outcome {
        }
    }

    /**
     * Records the sheet and writes it.
     *
     * @param pages the production assembler
     * @param state the chart as the reader has it
     * @param options the reader's chart options
     * @param ink the ink the chart is carrying, already assembled
     * @param request what the reader chose
     * @param destination where they chose to put it
     */
    public static Outcome write(ChartSheet.Pages pages,
                                ChartViewState state, ChartOptions options,
                                ChartRenderer.ReferenceLayer ink,
                                Request request, File destination) {
        if (destination == null) {
            return new Outcome.Refused("No file was chosen.");
        }
        File file = withExtension(destination, request.format());

        // Refuse before promising. A directory that does not exist, a
        // path that is a directory, a file that cannot be replaced -
        // each is answerable now, and each would otherwise be a
        // half-written file with the reader told it worked.
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent == null || !parent.isDirectory()) {
            return new Outcome.Refused("There is no folder at "
                    + (parent == null ? "that path" : parent.getPath())
                    + " to write into.");
        }
        if (file.isDirectory()) {
            return new Outcome.Refused(file.getName()
                    + " is a folder, not a file.");
        }
        if (file.exists() && !file.canWrite()) {
            return new Outcome.Refused(file.getName()
                    + " cannot be replaced: it is not writable.");
        }
        if (!file.exists() && !parent.canWrite()) {
            return new Outcome.Refused("That folder cannot be written"
                    + " to: " + parent.getPath());
        }

        byte[] bytes;
        try {
            SheetRecording sheet = ChartSheet.record(pages, state, options,
                    ink, request.paper());
            bytes = SheetWriters.write(sheet, request.format(),
                    request.dpi());
        } catch (IOException | RuntimeException failure) {
            // Nothing has been written yet, so there is nothing to
            // clean up and nothing that looks finished.
            return new Outcome.Refused("The sheet could not be made: "
                    + failure.getMessage());
        }

        try {
            Files.write(file.toPath(), bytes);
        } catch (IOException failure) {
            // A write that failed part way leaves a file that looks
            // like an export and is not one. It goes.
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException ignored) {
                return new Outcome.Refused(file.getName()
                        + " could not be written, and the partial file"
                        + " could not be removed: " + failure.getMessage());
            }
            return new Outcome.Refused(file.getName()
                    + " could not be written: " + failure.getMessage());
        }
        return new Outcome.Written(file.toPath(), bytes.length,
                request.format());
    }

    /**
     * The destination with the chosen format's extension.
     *
     * <p>A reader who types "orion" gets orion.svg, and one who types
     * "orion.pdf" and then chooses PNG gets orion.pdf.png rather than
     * a PNG pretending to be a PDF - the extension a file claims and
     * the bytes inside it have to agree.
     */
    static File withExtension(File chosen, SheetFormat format) {
        String name = chosen.getName();
        if (name.toLowerCase(Locale.ROOT)
                .endsWith("." + format.extension())) {
            return chosen;
        }
        return new File(chosen.getParentFile(),
                name + "." + format.extension());
    }
}
