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

        boolean replacing = file.exists();
        try {
            place(bytes, file.toPath(), parent.toPath(), sink);
        } catch (OriginalLostException lost) {
            // Never "unchanged" here. This is the one path where the
            // reader's own file did not survive, and saying anything
            // reassuring about it would be a lie (PR #291 round 3).
            return new Outcome.Refused(file.getName()
                    + " could not be written, and " + lost.getMessage()
                    + ".");
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

    /**
     * Thrown when a write failed <em>and</em> what was there could
     * not be put back.
     *
     * <p>The one outcome an export must never report vaguely. It
     * carries where the original's bytes were set aside, so the
     * reader is told something they can act on rather than only that
     * something went wrong (PR #291 round 3).
     */
    static final class OriginalLostException extends IOException {

        private static final long serialVersionUID = 1L;

        private final transient Path rescue;

        OriginalLostException(String message, Path rescue,
                              Throwable cause) {
            super(message, cause);
            this.rescue = rescue;
        }

        Path rescue() {
            return rescue;
        }
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
     * <p>Written beside the destination and moved onto it where the
     * folder allows that, so a failure cannot truncate what is
     * already there.
     *
     * <p>Where the folder does not allow it - a read-only directory
     * holding a writable file, which is a legitimate thing to
     * replace - there is nowhere beside the destination to write, so
     * the destination is written directly. That can be interrupted,
     * so what was in it is <strong>held first and put back</strong>
     * if the write fails. A chart sheet is a few hundred kilobytes;
     * holding one for the length of a write is cheaper than losing
     * the reader's own file (PR #291 round 2).
     *
     * <p>What is never done, in any path, is <strong>deleting the
     * destination</strong>. An earlier version removed it when the
     * write failed, on the reasoning that a partial file should not
     * be left looking finished; the file it removed was the reader's
     * own chart.
     */
    static void place(byte[] bytes, Path file, Path parent, ByteSink sink)
            throws IOException {
        if (!java.nio.file.Files.isWritable(parent)) {
            if (!Files.exists(file)) {
                // Nothing to lose: a folder that cannot be written
                // to has no new file to give, and the sink will say
                // so.
                sink.write(file, bytes);
                return;
            }
            // A copy of the reader's own bytes, kept somewhere the
            // folder's permissions cannot reach, so that a failed
            // restore is recoverable rather than final.
            Path rescue = Files.createTempFile("juranometria-original-",
                    "-" + file.getFileName());
            Files.copy(file, rescue,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            boolean rescueIsTheOnlyCopy = false;
            try {
                sink.write(file, bytes);
            } catch (IOException failure) {
                try {
                    sink.write(file, Files.readAllBytes(rescue));
                } catch (IOException lost) {
                    rescueIsTheOnlyCopy = true;
                    throw new OriginalLostException(
                            "the export failed and " + file.getFileName()
                                    + " could not be put back as it"
                                    + " was; a copy of what was in it"
                                    + " is at " + rescue,
                            rescue, failure);
                }
                throw failure;
            } finally {
                // Never a return in here: one swallowed the very
                // exception this path exists to report, in the first
                // version of it. The copy goes unless it is the only
                // one left of the reader's bytes.
                if (!rescueIsTheOnlyCopy) {
                    Files.deleteIfExists(rescue);
                }
            }
            return;
        }
        Path partial = Files.createTempFile(parent,
                file.getFileName() + ".", ".part");
        try {
            sink.write(partial, bytes);
            try {
                Files.move(partial, file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException
                    unsupported) {
                Files.move(partial, file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(partial);
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
