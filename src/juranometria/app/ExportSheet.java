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

            /**
             * What was written, in the reader's language (#350).
             *
             * <p>The format identity, the file name and the byte
             * count go in as arguments: none of them is a word, and
             * the sentence around them is not this record's to build.
             */
            public String message(
                    juranometria.ui.language.InterfaceText said) {
                // The count goes in as a NUMBER. Formatted here in
                // Locale.ROOT it arrived already grouped the English
                // way - "194,460" - which is not language-neutral
                // data but one language's punctuation, handed to
                // every other. MessageFormat groups it in the
                // language being read (#350).
                return said.say("export.written.message",
                        format.identity(), String.valueOf(file.getFileName()),
                        bytes);
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
                                ReplaceDecision replace,
                                juranometria.ui.language.InterfaceText said) {
        return write(pages, state, options, ink,
                ChartRenderer.ReferenceLayer.NONE, request, destination,
                replace, SINK, said);
    }

    /** The same, with the reader's own marks over the chart. */
    public static Outcome write(ChartSheet.Pages pages,
                                ChartViewState state, ChartOptions options,
                                ChartRenderer.ReferenceLayer ink,
                                ChartRenderer.ReferenceLayer overChart,
                                Request request, File destination,
                                ReplaceDecision replace,
                                juranometria.ui.language.InterfaceText said) {
        return write(pages, state, options, ink, overChart, request,
                destination, replace, SINK, said);
    }

    /** The same, writing however it is told to - a seam for tests. */
    static Outcome write(ChartSheet.Pages pages,
                         ChartViewState state, ChartOptions options,
                         ChartRenderer.ReferenceLayer ink,
                         ChartRenderer.ReferenceLayer overChart,
                         Request request, File destination,
                         ReplaceDecision replace, ByteSink sink,
                         juranometria.ui.language.InterfaceText said) {
        if (destination == null) {
            return new Outcome.Refused(said.say("export.refused.nofile"));
        }
        File file = withExtension(destination, request.format());

        // Refuse before promising. A directory that does not exist, a
        // path that is a directory, a file that cannot be replaced -
        // each is answerable now, and each would otherwise be a
        // half-written file with the reader told it worked.
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent == null || !parent.isDirectory()) {
            // Two whole sentences rather than one with a fragment
            // substituted into it: "that path" is not a folder name,
            // and a language may not want it in the same position
            // (#350).
            return new Outcome.Refused(parent == null
                    ? said.say("export.refused.nofolder.unknown")
                    : said.say("export.refused.nofolder", parent.getPath()));
        }
        if (file.isDirectory()) {
            return new Outcome.Refused(
                    said.say("export.refused.isfolder", file.getName()));
        }
        if (file.exists() && !file.canWrite()) {
            return new Outcome.Refused(
                    said.say("export.refused.unwritable", file.getName()));
        }
        // Asked here rather than left to the file chooser, because
        // the name the chooser approved is not always the name that
        // gets written: choosing "orion" with SVG selected writes
        // "orion.svg", and the chooser never saw that one.
        if (file.exists() && !replace.mayReplace(file)) {
            return new Outcome.Refused(
                    said.say("export.refused.kept", file.getName()));
        }
        // A sheet is written completely or not at all, and that
        // needs somewhere beside the destination to write it. A
        // folder that cannot hold a working file cannot hold a safe
        // export, so it is refused rather than written to directly:
        // a direct write is interruptible, and a rescue copy only
        // helps a process that lives long enough to use it. A crash
        // does not grant that (PR #291 round 4).
        if (!parent.canWrite()) {
            return new Outcome.Refused(said.say(
                    "export.refused.foldernotwritable", parent.getPath()));
        }

        byte[] bytes;
        try {
            SheetRecording sheet = ChartSheet.record(pages, state, options,
                    ink, overChart, request.paper());
            bytes = SheetWriters.write(sheet, request.format(),
                    request.dpi());
        } catch (IOException | RuntimeException failure) {
            // Nothing has been written yet, so there is nothing to
            // clean up and nothing that looks finished.
            // The exception's own message is platform detail,
             // written by whoever threw it and in whatever language
             // that library uses. It is not spliced into a reader's
             // sentence (#350); what a reader needs is that nothing
             // was written, which is what this says.
            return new Outcome.Refused(said.say("export.refused.notmade"));
        }

        boolean replacing = file.exists();
        try {
            place(bytes, file.toPath(), parent.toPath(), sink);
        } catch (IOException failure) {
            // Two whole patterns rather than one with an optional
            // clause glued on: whether a language puts the
            // reassurance second, or in the same sentence at all, is
            // its own decision (#350).
            return new Outcome.Refused(replacing
                    ? said.say("export.refused.notreplaced", file.getName())
                    : said.say("export.refused.notwritten", file.getName()));
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
