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
        ExportSheetDialog.open(owner, defaults(), request ->
                choosePlace(owner, navigation, chart, options, working,
                        request));
    }

    private static void choosePlace(Frame owner,
                                    ChartViewController navigation,
                                    ChartComponent chart,
                                    ChartOptionsController options,
                                    WorkingSelection working,
                                    ExportSheet.Request request) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export chart sheet");
        chooser.setSelectedFile(new File(SheetFileName.suggest(
                navigation.state(), chart.currentScene(),
                request.format())));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;  // cancelled: nothing made, nothing written
        }

        ExportSheet.Outcome outcome = ExportSheet.write(
                juranometria.app.Atlas.assembler()::assemble,
                navigation.state(), options.options(),
                SheetInk.of(chart, working.lead(),
                        request.workingSelection()),
                request, chooser.getSelectedFile());
        report(owner, outcome);
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
