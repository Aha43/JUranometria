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
     * Whether the reader agrees to replace a file that is already
     * there.
     *
     * <p>A separate decision from choosing the destination, because
     * the destination a reader chooses and the file that gets written
     * are not always the same name: a chooser that approved "orion"
     * has approved nothing about the "orion.svg" already sitting
     * beside it (PR #291 review).
     */
    @FunctionalInterface
    public interface ReplaceDecision {

        /** Never replaces: the answer for a caller with no reader. */
        ReplaceDecision REFUSE = existing -> false;

        boolean mayReplace(File existing);
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
                                Request request, File destination,
                                ReplaceDecision replace) {
        return write(pages, state, options, ink, request, destination,
                replace, SINK);
    }

    /** The same, writing however it is told to - a seam for tests. */
    static Outcome write(ChartSheet.Pages pages,
                         ChartViewState state, ChartOptions options,
                         ChartRenderer.ReferenceLayer ink,
                         Request request, File destination,
                         ReplaceDecision replace, ByteSink sink) {
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
        // Asked here rather than left to the file chooser, because
        // the name the chooser approved is not always the name that
        // gets written: choosing "orion" with SVG selected writes
        // "orion.svg", and the chooser never saw that one.
        if (file.exists() && !replace.mayReplace(file)) {
            return new Outcome.Refused(file.getName()
                    + " was left as it was.");
        }
        // A sheet is written completely or not at all, and that
        // needs somewhere beside the destination to write it. A
        // folder that cannot hold a working file cannot hold a safe
        // export, so it is refused rather than written to directly:
        // a direct write is interruptible, and a rescue copy only
        // helps a process that lives long enough to use it. A crash
        // does not grant that (PR #291 round 4).
        if (!parent.canWrite()) {
            return new Outcome.Refused(parent.getPath()
                    + " cannot be written to, so the sheet cannot be"
                    + " written there safely. Choose another folder,"
                    + " or make that one writable.");
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

        boolean replacing = file.exists();
        try {
            place(bytes, file.toPath(), parent.toPath(), sink);
        } catch (IOException failure) {
            return new Outcome.Refused(file.getName()
                    + " could not be written: " + failure.getMessage()
                    + (replacing
                            ? " What was already there is unchanged."
                            : ""));
        }
        return new Outcome.Written(file.toPath(), bytes.length,
                request.format());
    }

    /** How bytes reach a path; a seam so a failure can be tried. */
    @FunctionalInterface
    interface ByteSink {

        void write(Path file, byte[] bytes) throws IOException;
    }

    /** The real one. */
    static final ByteSink SINK = Files::write;

    /**
     * Puts the finished bytes at the destination without ever
     * putting unfinished ones there.
     *
     * <p>Written beside the destination and moved onto it in one
     * step. There is no other path: a direct write can be
     * interrupted, and everything that could be done about that
     * afterwards - putting the old bytes back, keeping a rescue copy
     * - needs a process that survives to do it. A crash does not
     * grant that, so the case where a working file cannot be made is
     * refused before anything is written (PR #291 round 4).
     *
     * <p>What is never done is <strong>deleting the
     * destination</strong>. An earlier version removed it when the
     * write failed, on the reasoning that a partial file should not
     * be left looking finished; the file it removed was the reader's
     * own chart.
     */
    static void place(byte[] bytes, Path file, Path parent, ByteSink sink)
            throws IOException {
        place(bytes, file, parent, sink, ExportSheet::moveOnto);
    }

    /** The same, moving however it is told to - a seam for tests. */
    static void place(byte[] bytes, Path file, Path parent, ByteSink sink,
                      Mover mover) throws IOException {
        Path partial = Files.createTempFile(parent,
                file.getFileName() + ".", ".part");
        try {
            sink.write(partial, bytes);
            mover.move(partial, file);
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    /** How a finished working file becomes the destination. */
    @FunctionalInterface
    interface Mover {

        void move(Path from, Path to) throws IOException;
    }

    /**
     * One step, or none.
     *
     * <p>A move that is not atomic is a copy and a delete, and a
     * copy can be interrupted - which would put half a sheet at the
     * destination under the name of a whole one. Where the file
     * system will not promise atomicity, the export says so and
     * writes nothing, because "written completely or not at all" is
     * either true or it is not worth saying.
     */
    static void moveOnto(Path from, Path to) throws IOException {
        try {
            Files.move(from, to,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException cannot) {
            throw new IOException(to.getFileName()
                    + " could not be replaced in one step on this file"
                    + " system, and a sheet is written completely or"
                    + " not at all", cannot);
        }
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
