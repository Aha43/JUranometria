package juranometria.app;

import java.awt.Frame;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import juranometria.chart.WorkingSelection;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sheet.SheetFileName;
import juranometria.sheet.SheetFormat;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.SheetInk;

/**
 * What happens when a reader picks File, Export Chart Sheet
 * (Sprint 29, issue #286).
 *
 * <p>Two steps, in the order a reader expects: decide what kind of
 * sheet, then decide where to put it. The second step is the
 * platform's own file chooser, because saving a file is not a
 * problem this application should have opinions about, and it is
 * where overwrite confirmation and folder navigation already live.
 *
 * <p>The chart is asked what it is showing at the moment the reader
 * asks - its state, its options, its module ink and, if they said so,
 * its working selection. Nothing is remembered between exports: the
 * gate did not approve persisting a reader's output paths, and a
 * private directory is not something an atlas should keep.
 */
public final class ExportSheetSession {

    private ExportSheetSession() {
    }

    /** The defaults a reader who chooses nothing gets. */
    static ExportSheet.Request defaults() {
        return new ExportSheet.Request(SheetFormat.SVG, PaperSize.A4,
                PngSheetWriter.DEFAULT_RESOLUTION, false);
    }

    /** Opens the dialog, and on Export, the file chooser. */
    public static void open(Frame owner, ChartViewController navigation,
                            ChartComponent chart,
                            ChartOptionsController options,
                            WorkingSelection working) {
        ExportSheetDialog.open(owner, defaults(), request -> {
            File chosen = choosePlace(owner, navigation, chart, request);
            if (chosen == null) {
                return;  // cancelled: nothing made, nothing written
            }
            report(owner, exportTo(chosen, request, navigation, chart,
                    options, working, replaceDecision(owner)));
        });
    }

    /** Where the reader wants it, or null if they changed their mind. */
    private static File choosePlace(Frame owner,
                                    ChartViewController navigation,
                                    ChartComponent chart,
                                    ExportSheet.Request request) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export chart sheet");
        chooser.setSelectedFile(new File(SheetFileName.suggest(
                navigation.state(), chart.currentScene(),
                request.format())));
        return chooser.showSaveDialog(owner) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile() : null;
    }

    /**
     * The export itself, with the chart asked what it is showing.
     *
     * <p>Separated from the two dialogs so that the wiring between
     * them is a thing a test can drive: which chart is exported,
     * which ink goes on it, and - the one that would otherwise be
     * invisible - that the reader is asked before anything of theirs
     * is replaced (PR #291 round 2).
     */
    static ExportSheet.Outcome exportTo(File destination,
                                        ExportSheet.Request request,
                                        ChartViewController navigation,
                                        ChartComponent chart,
                                        ChartOptionsController options,
                                        WorkingSelection working,
                                        ExportSheet.ReplaceDecision replace) {
        return ExportSheet.write(
                juranometria.app.Atlas.assembler()::assemble,
                navigation.state(), options.options(),
                SheetInk.of(chart, working.lead(),
                        request.workingSelection()),
                request, destination, replace);
    }

    /**
     * How a yes-or-no question reaches the reader.
     *
     * <p>A seam of exactly one method, so that what the application
     * asks - and whether it asks at all - is something a test can
     * watch without a modal dialog appearing on someone's screen
     * (PR #291 round 2).
     */
    @FunctionalInterface
    interface Confirmer {

        /** The reader's answer: a {@code JOptionPane} option value. */
        int ask(Frame owner, String question, String title);
    }

    /** The real one: a dialog, owned by and centred on the atlas. */
    static int confirmOnScreen(Frame owner, String question,
                               String title) {
        return JOptionPane.showConfirmDialog(owner, question, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * How the reader is asked, in the running application: a dialog.
     *
     * <p>Never a constant. A decision that always said yes would
     * replace a reader's file without a word and would look exactly
     * like this from the outside, so it is a named thing that asks.
     */
    static ExportSheet.ReplaceDecision replaceDecision(Frame owner) {
        return replaceDecision(owner, ExportSheetSession::confirmOnScreen);
    }

    /** The same decision, asking however it is told to ask. */
    static ExportSheet.ReplaceDecision replaceDecision(Frame owner,
                                                       Confirmer confirmer) {
        return existing -> confirmer.ask(owner,
                existing.getName() + " already exists in "
                        + existing.getAbsoluteFile().getParent()
                        + ".\nReplace it?",
                "Replace the existing file?")
                == JOptionPane.YES_OPTION;
    }

    /** Says what happened, in the reader's own terms. */
    static void report(Frame owner, ExportSheet.Outcome outcome) {
        if (outcome instanceof ExportSheet.Outcome.Written written) {
            JOptionPane.showMessageDialog(owner, written.message(),
                    "Chart sheet exported",
                    JOptionPane.INFORMATION_MESSAGE);
        } else if (outcome instanceof ExportSheet.Outcome.Refused refused) {
            // A refusal is not a crash and not a silence: the reader
            // is told what did not happen and why, and no file is
            // left behind looking finished.
            JOptionPane.showMessageDialog(owner, refused.reason(),
                    "The chart sheet was not written",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}
