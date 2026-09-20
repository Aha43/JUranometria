package juranometria.app;

import java.awt.Frame;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import juranometria.chart.WorkingSelection;
import juranometria.render.ChartRenderer;
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

    /**
     * Everything this session asks a person, in one place.
     *
     * <p>Named so that the whole route - item, dialog, destination,
     * replace, report - can be driven end to end rather than
     * reassembled from its parts by a test that then proves only
     * that the parts exist (PR #292 review). Production supplies
     * dialogs; a journey supplies the same dialog with its buttons
     * pressed.
     */
    public interface Surfaces {

        /** What kind of sheet, or empty if the reader changes their mind. */
        java.util.Optional<ExportSheet.Request> chooseWhat(Frame owner,
                ExportSheet.Request initial);

        /** Where to put it, or empty if they change their mind. */
        java.util.Optional<File> chooseWhere(Frame owner,
                String suggestedName);

        /** Whether to replace something already there. */
        ExportSheet.ReplaceDecision replace(Frame owner);

        /** What happened. */
        void report(Frame owner, ExportSheet.Outcome outcome);
    }

    /** The surfaces the running application uses: real windows. */
    public static Surfaces onScreen(juranometria.ui.language.InterfaceText said) {
        return new Surfaces() {

            @Override
            public java.util.Optional<ExportSheet.Request> chooseWhat(
                    Frame owner, ExportSheet.Request initial) {
                java.util.List<ExportSheet.Request> chosen =
                        new java.util.ArrayList<>();
                ExportSheetDialog.open(owner, initial, chosen::add, said);
                return chosen.stream().findFirst();
            }

            @Override
            public java.util.Optional<File> chooseWhere(Frame owner,
                    String suggestedName) {
                JFileChooser chooser = new JFileChooser();
                // Ours - and since #350 so are the chooser's own
                // forty-one words: its buttons, labels, hovers and
                // spoken names are installed from the reader's
                // interface language at startup, where Swing used to
                // take them from the operating system. What remains
                // the platform's is filesystem DATA: volume and
                // folder names, and the list of places.
                chooser.setDialogTitle(said.say("export.chooser.title"));
                chooser.setSelectedFile(new File(suggestedName));
                return chooser.showSaveDialog(owner)
                        == JFileChooser.APPROVE_OPTION
                        ? java.util.Optional.of(chooser.getSelectedFile())
                        : java.util.Optional.empty();
            }

            @Override
            public ExportSheet.ReplaceDecision replace(Frame owner) {
                return replaceDecision(owner, said);
            }

            @Override
            public void report(Frame owner, ExportSheet.Outcome outcome) {
                ExportSheetSession.report(owner, outcome, said);
            }
        };
    }

    /** Opens the dialog, and on Export, the file chooser. */
    public static void open(Frame owner, ChartViewController navigation,
                            ChartComponent chart,
                            ChartOptionsController options,
                            WorkingSelection working,
                            juranometria.ui.language.InterfaceText said) {
        open(owner, navigation, chart, options, working, onScreen(said),
                said);
    }

    /** The same route, asking through whatever surfaces it is given. */
    public static void open(Frame owner, ChartViewController navigation,
                            ChartComponent chart,
                            ChartOptionsController options,
                            WorkingSelection working, Surfaces surfaces,
                            juranometria.ui.language.InterfaceText said) {
        surfaces.chooseWhat(owner, defaults()).ifPresent(request ->
                surfaces.chooseWhere(owner, SheetFileName.suggest(
                                navigation.state(), chart.currentScene(),
                                request.format()))
                        .ifPresent(destination -> surfaces.report(owner,
                                exportTo(destination, request, navigation,
                                        chart, options, working,
                                        surfaces.replace(owner), said))));
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
                                        ExportSheet.ReplaceDecision replace,
                                        juranometria.ui.language.InterfaceText said) {
        return ExportSheet.write(
                // The chart's own assembler, not the application's.
                // They were the same object until a reader could
                // choose a sky language; asking the chart is what
                // keeps screen and paper in one language by
                // construction rather than by two call sites
                // remembering to agree (#348).
                chart.assembler()::assemble,
                navigation.state(), options.options(),
                SheetInk.reference(chart),
                request.workingSelection()
                        ? SheetInk.working(chart, working.members(),
                                working.lead(), options.options())
                        : ChartRenderer.ReferenceLayer.NONE,
                request, destination, replace, said);
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
    static ExportSheet.ReplaceDecision replaceDecision(Frame owner,
                                                       juranometria.ui.language.InterfaceText said) {
        return replaceDecision(owner, ExportSheetSession::confirmOnScreen,
                said);
    }

    /** The same decision, asking however it is told to ask. */
    static ExportSheet.ReplaceDecision replaceDecision(Frame owner,
                                                       Confirmer confirmer,
                                                       juranometria.ui.language.InterfaceText said) {
        juranometria.ui.language.ExportText words =
                juranometria.ui.language.ExportText.in(said);
        return existing -> confirmer.ask(owner,
                words.replaceQuestion(existing.getName(),
                        existing.getAbsoluteFile().getParent()),
                words.replaceTitle())
                == JOptionPane.YES_OPTION;
    }

    /** Says what happened, in the reader's own terms. */
    static void report(Frame owner, ExportSheet.Outcome outcome,
                       juranometria.ui.language.InterfaceText said) {
        if (outcome instanceof ExportSheet.Outcome.Written written) {
            JOptionPane.showMessageDialog(owner, written.message(said),
                    said.say("export.written.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else if (outcome instanceof ExportSheet.Outcome.Refused refused) {
            // A refusal is not a crash and not a silence: the reader
            // is told what did not happen and why, and no file is
            // left behind looking finished.
            JOptionPane.showMessageDialog(owner, refused.reason(),
                    said.say("export.refused.title"),
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}
