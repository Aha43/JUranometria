package juranometria.app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFileName;
import juranometria.sheet.SheetFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Writing the file (Sprint 29, issue #286).
 *
 * <p>The drawing is held elsewhere. What is held here is everything
 * around it, which is where an export disappoints a reader: a name
 * they can find again, an extension that matches the bytes, a
 * refusal that says what to do, and - the one that matters most -
 * nothing left on disk that looks like a finished sheet when it is
 * not.
 */
class ExportSheetTest {

    private static final ChartViewState ORION = new ChartViewState(
            new SkyPosition(83.0, 0.0), 42.0, 6.0, "Orion region",
            "TYC 129-1873-1");

    private static ExportSheet.Outcome export(SheetFormat format,
                                              File destination) {
        return ExportSheet.write(Atlas.assembler()::assemble, ORION,
                ChartOptions.DEFAULTS, ChartRenderer.ReferenceLayer.NONE,
                new ExportSheet.Request(format, PaperSize.A4, 150, false),
                destination);
    }

    @Test
    void allThreeFormatsReachTheDiskAsThemselves(@TempDir Path folder)
            throws Exception {
        for (SheetFormat format : SheetFormat.values()) {
            File file = folder.resolve("orion." + format.extension())
                    .toFile();
            var outcome = assertInstanceOf(
                    ExportSheet.Outcome.Written.class,
                    export(format, file),
                    format + " is written");
            assertTrue(Files.size(file.toPath()) > 5000,
                    format + " has a chart in it: "
                            + Files.size(file.toPath()) + " bytes");
            assertEquals(Files.size(file.toPath()), outcome.bytes(),
                    "and the reader is told the size that is there");

            byte[] head = new byte[8];
            try (var in = Files.newInputStream(file.toPath())) {
                assertEquals(8, in.read(head), "and it has a beginning");
            }
            String start = new String(head,
                    java.nio.charset.StandardCharsets.ISO_8859_1);
            switch (format) {
                case SVG -> assertTrue(start.startsWith("<svg"),
                        "an SVG begins as one");
                case PDF -> assertTrue(start.startsWith("%PDF-"),
                        "a PDF begins as one");
                case PNG -> assertEquals((byte) 0x89, head[0],
                        "a PNG begins as one");
            }
        }
    }

    @Test
    void theExtensionAndTheBytesAlwaysAgree(@TempDir Path folder) {
        // A reader who types a bare name gets the right suffix, and
        // one who types the wrong suffix does not get a PNG called
        // .pdf - the name a file claims and what is inside it are the
        // same promise.
        assertEquals("orion.svg", ExportSheet.withExtension(
                folder.resolve("orion").toFile(), SheetFormat.SVG)
                .getName());
        assertEquals("orion.pdf.png", ExportSheet.withExtension(
                folder.resolve("orion.pdf").toFile(), SheetFormat.PNG)
                .getName(),
                "a PNG chosen after a .pdf name is still a PNG, and"
                        + " says so last");
        assertEquals("orion.PNG", ExportSheet.withExtension(
                folder.resolve("orion.PNG").toFile(), SheetFormat.PNG)
                .getName(),
                "and a reader who shouts is not corrected");
    }

    @Test
    void aRefusalSaysWhatToDoAndLeavesNothingBehind(@TempDir Path folder)
            throws Exception {
        // A folder standing where a file was asked for. It has to
        // carry the format's own extension to get this far, which a
        // folder can: appending one to a bare name would have made a
        // sibling file instead, and that is a legitimate export.
        Path directory = Files.createDirectory(folder.resolve("sheets.svg"));
        var asFolder = assertInstanceOf(ExportSheet.Outcome.Refused.class,
                export(SheetFormat.SVG, directory.toFile()),
                "a folder is not a file to write");
        assertTrue(asFolder.reason().contains("folder"),
                "and the reason says so: " + asFolder.reason());

        // A folder that does not exist.
        var missing = assertInstanceOf(ExportSheet.Outcome.Refused.class,
                export(SheetFormat.SVG,
                        folder.resolve("nowhere").resolve("orion.svg")
                                .toFile()),
                "a path through a folder that is not there is refused");
        assertTrue(missing.reason().contains("no folder"),
                "and named: " + missing.reason());

        // A file that cannot be replaced.
        Path locked = Files.writeString(folder.resolve("locked.svg"),
                "not a chart");
        assertTrue(locked.toFile().setWritable(false),
                "the test can make a file unwritable");
        try {
            var refused = assertInstanceOf(
                    ExportSheet.Outcome.Refused.class,
                    export(SheetFormat.SVG, locked.toFile()),
                    "an unwritable file is refused rather than"
                            + " half-written");
            assertTrue(refused.reason().contains("locked.svg"),
                    "and named: " + refused.reason());
            assertTrue(Files.exists(locked),
                    "with the file that was already there still"
                            + " there - a refused export destroys"
                            + " nothing");
            assertEquals("not a chart", Files.readString(locked),
                    "and its contents exactly as they were");
        } finally {
            locked.toFile().setWritable(true);
        }

        // And nothing was created by any of those attempts.
        try (var listing = Files.list(folder)) {
            assertEquals(List.of("locked.svg", "sheets.svg"),
                    listing.map(each -> each.getFileName().toString())
                            .sorted().toList(),
                    "a refused export leaves the folder as it found it");
        }
    }

    @Test
    void theSuggestedNameIsOneAReaderCanFindAgain() {
        assertEquals("juranometria-orion-region-42deg.svg",
                SheetFileName.suggest(ORION,
                        Atlas.assembler().assemble(ORION, 770, 523),
                        SheetFormat.SVG),
                "the chart's own subject and field, not chart.svg");

        // A page with no named target still says where it is, from
        // the title the chart gives itself.
        ChartViewState anonymous = new ChartViewState(
                new SkyPosition(83.0, 0.0), 42.0, 6.0);
        String name = SheetFileName.suggest(anonymous,
                Atlas.assembler().assemble(anonymous, 770, 523),
                SheetFormat.PDF);
        assertTrue(name.startsWith("juranometria-")
                        && name.endsWith("-42deg.pdf"),
                "an unnamed page is still named for what it is: " + name);
        assertTrue(name.matches("[a-z0-9.\\-]+"),
                "with nothing in it a file system will argue about: "
                        + name);
    }

    @Test
    void theDefaultsMakeAUsefulSheetWithoutAnyKnowledge() {
        // A reader who knows nothing about pixels, dots per inch or
        // projections presses Export and gets something worth having.
        ExportSheet.Request defaults = ExportSheetSession.defaults();
        assertEquals(SheetFormat.SVG, defaults.format(),
                "the editable master, which is also the smallest");
        assertEquals(PaperSize.A4, defaults.paper(), "and A4");
        assertEquals(300, defaults.dpi(),
                "with a resolution that prints, for when they choose"
                        + " the format that uses it");
        assertFalse(defaults.workingSelection(),
                "and no transient marks: a sheet outlives the session"
                        + " that made it");
    }
}
