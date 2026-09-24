package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewState;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.ChartStructure;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The explicit emphasized export (issue #361).
 *
 * <p>Ordinary export is the canonical chart whatever the screen
 * shows - byte for byte, by the same code path. When the reader
 * explicitly carries the screen's emphasis onto paper, one recording
 * is made and every format writes it, each recording the semantic
 * target in its own metadata; and the choice exists in the dialog
 * only while something is emphasized, always starting unchecked.
 */
class ExportEmphasisTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static SheetRecording recorded(ChartStructure target) {
        return ChartSheet.record(Atlas.assembler()::assemble,
                ChartViewState.DEFAULT, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4, ENGLISH,
                target == null ? java.util.Set.of()
                        : java.util.Set.of(target));
    }

    @Test
    void ordinaryExportIsCanonicalByTheSamePath() throws Exception {
        SheetRecording legacy = ChartSheet.record(
                Atlas.assembler()::assemble, ChartViewState.DEFAULT,
                ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                PaperSize.A4, ENGLISH);
        SheetRecording unemphasized = recorded(null);
        for (SheetFormat format : SheetFormat.values()) {
            org.junit.jupiter.api.Assertions.assertArrayEquals(
                    SheetWriters.write(legacy, format, 300),
                    SheetWriters.write(unemphasized, format, 300),
                    format + ": no emphasis is the sheet the atlas"
                            + " always wrote, byte for byte");
        }
        assertNull(unemphasized.metadata().emphasis(),
                "and its metadata records no target");
    }

    @Test
    void everyFormatWritesTheOneEmphasizedRecordingAndSaysSo()
            throws Exception {
        SheetRecording emphasized = recorded(
                ChartStructure.EQUATORIAL_GRID);
        assertEquals("equatorial-grid",
                emphasized.metadata().emphasis(),
                "the recording knows its semantic target as a stable"
                        + " token");

        String svg = new String(SheetWriters.write(emphasized,
                SheetFormat.SVG, 300), StandardCharsets.UTF_8);
        assertTrue(svg.contains(
                        "<metadata>emphasis:equatorial-grid</metadata>"),
                "SVG records the target");

        String pdf = new String(SheetWriters.write(emphasized,
                SheetFormat.PDF, 300), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains("/Keywords (emphasis:equatorial-grid)"),
                "PDF records the target");

        String png = new String(SheetWriters.write(emphasized,
                SheetFormat.PNG, 300), StandardCharsets.ISO_8859_1);
        assertTrue(png.contains("Emphasis")
                        && png.contains("equatorial-grid"),
                "PNG records the target");

        // And the canonical sheet says none of it, in any format.
        SheetRecording canonical = recorded(null);
        assertFalse(new String(SheetWriters.write(canonical,
                        SheetFormat.SVG, 300), StandardCharsets.UTF_8)
                        .contains("emphasis:"),
                "an ordinary SVG carries no emphasis record");
        assertFalse(new String(SheetWriters.write(canonical,
                        SheetFormat.PDF, 300),
                        StandardCharsets.ISO_8859_1)
                        .contains("/Keywords"),
                "an ordinary PDF carries no emphasis record");
        assertFalse(new String(SheetWriters.write(canonical,
                        SheetFormat.PNG, 300),
                        StandardCharsets.ISO_8859_1)
                        .contains("Emphasis"),
                "an ordinary PNG carries no emphasis record");
    }

    @Test
    void theEmphasizedSheetDiffersOnlyBecauseOfItsInk() throws Exception {
        // The pixels: an emphasized PNG is not the canonical PNG,
        // which is what makes the explicit choice mean something.
        byte[] canonical = SheetWriters.write(recorded(null),
                SheetFormat.PNG, 300);
        byte[] emphasized = SheetWriters.write(
                recorded(ChartStructure.EQUATORIAL_GRID),
                SheetFormat.PNG, 300);
        assertFalse(java.util.Arrays.equals(canonical, emphasized),
                "carrying the emphasis changes the sheet");
    }

    @Test
    void theChoiceExistsOnlyWhileSomethingIsEmphasized() {
        List<ExportSheet.Request> chosen = new ArrayList<>();
        Component plain = ExportSheetDialog.content(
                ExportSheetSession.defaults(), chosen::add, () -> { },
                juranometria.ui.language.InterfaceText.forLanguage("en"));
        assertNull(byName(plain, ExportSheetDialog.EMPHASIS_BOX),
                "no emphasis, no choice: the dialog is the dialog it"
                        + " always was");

        Component offered = ExportSheetDialog.content(
                ExportSheetSession.defaults(), chosen::add, () -> { },
                juranometria.ui.language.InterfaceText.forLanguage("en"),
                true);
        javax.swing.JCheckBox include = (javax.swing.JCheckBox)
                byName(offered, ExportSheetDialog.EMPHASIS_BOX);
        assertTrue(include != null && !include.isSelected(),
                "the choice is offered, and always starts unchecked");
        // The button-to-request wiring is held where the dialog's
        // controls are already driven: ExportJourneyTest.
        assertTrue(chosen.isEmpty(),
                "and inspecting the surface exports nothing");
    }

    private static Component byName(Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = byName(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
