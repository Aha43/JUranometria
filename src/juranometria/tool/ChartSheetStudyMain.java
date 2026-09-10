package juranometria.tool;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PdfSheetWriter;
import juranometria.sheet.PngSheetWriter;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SvgSheetWriter;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

/**
 * The sheets themselves, so a reader can open one (Sprint 29,
 * issue #285).
 *
 * <p>Every claim the tests make about the SVG is made against a
 * document they wrote a moment earlier and threw away. These are the
 * same documents, kept: a reviewer can open one in a browser or an
 * editor and see whether the thing is actually a chart.
 *
 * <p>They are written by the production writer through the
 * production boundary. Nothing here draws anything.
 */
public final class ChartSheetStudyMain {

    private ChartSheetStudyMain() {
    }

    private static final File DIR = new File("docs/studies/chart-sheet");

    /** The reader's own case: Orion at the sheet field. */
    private static final ChartViewState ORION = new ChartViewState(
            new SkyPosition(83.0, 0.0), 42.0, 6.0);

    /** The March equinox, where the ecliptic's landmarks are. */
    private static final ChartViewState EQUINOX = new ChartViewState(
            new SkyPosition(0.0, 0.0), 42.0, 6.0);

    public static void main(String[] args) throws Exception {
        // Emptied first: the contract runs a study twice in one
        // JVM to see whether it reproduces here.
        sizes.setLength(0);
        DIR.mkdirs();
        StringBuilder report = new StringBuilder();

        report.append("# Chart sheets, written\n\n");
        report.append("The documents this sprint's tests assert"
                + " against, kept so that a\nreader can open one."
                + " Written by `juranometria.sheet.SvgSheetWriter`"
                + " through\n`juranometria.sheet.ChartSheet` - the"
                + " production boundary and the production\nwriter,"
                + " with nothing in this file drawing anything.\n\n");
        report.append("Regenerate with `make chart-sheet-study`.\n\n");

        report.append("## The paper\n\n");
        report.append("| | A4 | US Letter |\n|---|---:|---:|\n");
        report.append(String.format(Locale.ROOT,
                "| sheet | %.1f x %.1f mm | %.1f x %.1f mm |%n",
                PaperSize.A4.wideMm(), PaperSize.A4.highMm(),
                PaperSize.LETTER.wideMm(), PaperSize.LETTER.highMm()));
        report.append(String.format(Locale.ROOT,
                "| chart rectangle | %.1f x %.1f mm | %.1f x %.1f mm |%n",
                PaperSize.A4.chartWideMm(), PaperSize.A4.chartHighMm(),
                PaperSize.LETTER.chartWideMm(),
                PaperSize.LETTER.chartHighMm()));
        report.append(String.format(Locale.ROOT,
                "| margins | %.1f mm | %.1f mm |%n",
                PaperSize.A4.marginMm(), PaperSize.LETTER.marginMm()));

        report.append("\n## The sheets\n\n");
        // No byte column. A file's size is what a font's outlines
        // encode to, and holding one machine's number against
        // another's says nothing about the sheet: what a reader needs
        // to know is that the file is a valid one, that the vector
        // structure is there, and how many shapes and labels it
        // carries (#315). The sizes are recorded beside this.
        report.append("| file | paper | shapes | labels |\n");
        report.append("|---|---|---:|---:|\n");

        svg(report, "sheet-a4.svg", ORION, PaperSize.A4, false,
                SvgSheetWriter.Text.EDITABLE);
        svg(report, "sheet-letter.svg", ORION, PaperSize.LETTER, false,
                SvgSheetWriter.Text.EDITABLE);
        svg(report, "sheet-a4-outlines.svg", ORION, PaperSize.A4, false,
                SvgSheetWriter.Text.OUTLINES);
        svg(report, "sheet-a4-modules.svg", EQUINOX, PaperSize.A4, true,
                SvgSheetWriter.Text.EDITABLE);
        pdf(report, "sheet-a4.pdf", ORION, PaperSize.A4, false);
        pdf(report, "sheet-a4-modules.pdf", EQUINOX, PaperSize.A4, true);
        png(report, "sheet-a4-300dpi.png", ORION, PaperSize.A4,
                PngSheetWriter.DEFAULT_RESOLUTION);

        report.append("\n`sheet-a4-outlines.svg` is the same chart with"
                + " every label converted to\nits outline, for a"
                + " machine whose fonts are unknown. It is larger and"
                + " it\ncannot be edited as words, which is why it is"
                + " the variant and not the\nmaster.\n\n");
        report.append("`sheet-a4-modules.svg` and its PDF carry the"
                + " meridian, the horizon, the zenith and"
                + "\nthe ecliptic - the March equinox page, where the"
                + " ecliptic's landmarks are.\n\n");
        report.append("The PDF draws its labels as outlines, because the"
                + " base-14 fonts every\nreader has cannot spell the"
                + " chart's own notation; the PNG is the whole"
                + " sheet\nat "
                + PngSheetWriter.DEFAULT_RESOLUTION + " dpi with a"
                + " `pHYs` chunk stating that, so a printer sizes it"
                + " rather\nthan fitting it. All three come from one"
                + " recording of one render.\n\n");

        report.append("## What to measure on paper\n\n");
        report.append("Print `sheet-a4.pdf` at **actual size** - not"
                + " fit-to-page, which\nshrinks it by a few per cent"
                + " - and measure these with a ruler under\nordinary"
                + " light. Every figure is taken from the sheet"
                + " itself, not from\nthe gate's candidate"
                + " numbers.\n\n");
        report.append("They are **provisional targets, not"
                + " findings**. Nothing here has been\nprinted."
                + " Issue #293 owns the paper check, and paper"
                + " evidence outranks\nthis table: if a printed sheet"
                + " disagrees, the sheet is what"
                + " changes.\n\n");
        report.append(printChecks());
        report.append("\nA sheet that measures right and cannot be"
                + " read is still a failure.\nThe last two rows are"
                + " the ones that decide whether this works at an"
                + " observing\ntable: a faint star has to be a mark"
                + " rather than a speck, and a label has to\nbe a"
                + " word rather than a smudge, by torchlight, at"
                + " arm's length.\n\n");

        report.append("## What is not settled here\n\n");
        report.append("**Nothing on this page has been printed.** The"
                + " sizes above are arithmetic\nand the tests are"
                + " arithmetic; legibility on paper is neither."
                + " Issue #293\nowns a printed sheet measured with a"
                + " ruler, and that measurement can\nrevise these"
                + " numbers.\n\n");
        report.append("**Label positions are this machine's.** The"
                + " renderer places a label with\nfont metrics, so"
                + " another machine's sans-serif moves it slightly and"
                + " may\nfit one where this one did not. The sheets"
                + " reproduce byte for byte on a\ngiven machine, which"
                + " is the same classification the renderer studies"
                + " carry.\n\n");

        Files.writeString(new File(DIR, "measurements.md").toPath(),
                report.toString(), StandardCharsets.UTF_8);
        System.out.print(report);

        StringBuilder observed = new StringBuilder();
        PlatformEvidence.preface(observed,
                "Chart sheets, weighed on one machine",
                "Sprint 18, issue #141; classified in Sprint 31,"
                        + " issue #315.");
        observed.append("What a sheet weighs is what its fonts encode"
                + " to: an SVG with its text as\noutlines carries the"
                + " glyph paths of whatever font drew it, and a PNG"
                + " carries\nwhatever those glyphs rasterised to. The"
                + " report beside this one carries the\nfile's"
                + " structure - how many shapes, how many labels,"
                + " which paper - which is\nthe sheet's own answer"
                + " and the same everywhere.\n\n");
        observed.append("| file | bytes |\n|---|---:|\n");
        observed.append(sizes);
        PlatformEvidence.write(observed,
                "docs/studies/chart-sheet/platform.md");
    }

    /** The ruler check, in millimetres, from the sheet's own ink. */
    private static String printChecks() {
        SheetRecording sheet = record(ORION, PaperSize.A4, false);
        double thinnest = Double.MAX_VALUE;
        double smallestMark = Double.MAX_VALUE;
        for (var drawn : sheet.recorder().drawn()) {
            if (!drawn.filled()) {
                thinnest = Math.min(thinnest, drawn.stroke().width());
            } else if (drawn.shape().getBounds2D().getWidth() > 0.5
                    && drawn.shape().getBounds2D().getWidth() < 20) {
                smallestMark = Math.min(smallestMark,
                        drawn.shape().getBounds2D().getWidth());
            }
        }
        int smallestLabel = Integer.MAX_VALUE;
        for (var text : sheet.recorder().text()) {
            smallestLabel = Math.min(smallestLabel,
                    text.font().getSize());
        }

        StringBuilder checks = new StringBuilder();
        checks.append("| measure | provisional target |\n|---|---:|\n");
        checks.append(String.format(Locale.ROOT,
                "| the sheet, edge to edge | %.1f x %.1f mm |%n",
                PaperSize.A4.wideMm(), PaperSize.A4.highMm()));
        checks.append(String.format(Locale.ROOT,
                "| the margin, paper edge to chart frame | %.1f mm |%n",
                PaperSize.A4.marginMm()));
        checks.append(String.format(Locale.ROOT,
                "| the chart frame, inside edge to inside edge |"
                        + " %.1f x %.1f mm |%n",
                PaperSize.A4.chartWideMm(), PaperSize.A4.chartHighMm()));
        checks.append(String.format(Locale.ROOT,
                "| the thinnest line on the sheet | %.3f mm"
                        + " (%.2f pt) |%n",
                PaperSize.mmOf(thinnest), thinnest));
        checks.append(String.format(Locale.ROOT,
                "| the faintest star's disc, across | %.2f mm"
                        + " (%.2f pt) |%n",
                PaperSize.mmOf(smallestMark), smallestMark));
        checks.append(String.format(Locale.ROOT,
                "| the smallest label's capital height | about"
                        + " %.2f mm (%d pt type) |%n",
                PaperSize.mmOf(smallestLabel * 0.7), smallestLabel));
        return checks.toString();
    }

    private static void pdf(StringBuilder report, String name,
                            ChartViewState state, PaperSize paper,
                            boolean modules) throws Exception {
        SheetRecording sheet = record(state, paper, modules);
        byte[] pdf = PdfSheetWriter.write(sheet);
        Files.write(new File(DIR, name).toPath(), pdf);
        report.append(String.format(Locale.ROOT,
                "| `%s` | %s | %d | %d as outlines |%n", name,
                paper.readableName(), sheet.shapeCount(),
                sheet.textCount()));
        sized(name, pdf.length);
    }

    private static void png(StringBuilder report, String name,
                            ChartViewState state, PaperSize paper,
                            int dpi) throws Exception {
        SheetRecording sheet = record(state, paper, false);
        byte[] png = PngSheetWriter.write(sheet, dpi);
        Files.write(new File(DIR, name).toPath(), png);
        report.append(String.format(Locale.ROOT,
                "| `%s` | %s at %d dpi, %d x %d px | %d | %d |%n",
                name, paper.readableName(), dpi,
                PngSheetWriter.widePixels(paper, dpi),
                PngSheetWriter.highPixels(paper, dpi),
                sheet.shapeCount(), sheet.textCount()));
        sized(name, png.length);
    }

    private static SheetRecording record(ChartViewState state,
                                         PaperSize paper, boolean modules) {
        return ChartSheet.record(Atlas.assembler()::assemble, state,
                ChartOptions.DEFAULTS,
                modules ? modules() : ChartRenderer.ReferenceLayer.NONE,
                paper);
    }

    private static void svg(StringBuilder report, String name,
                              ChartViewState state, PaperSize paper,
                              boolean modules, SvgSheetWriter.Text text)
            throws Exception {
        SheetRecording sheet = record(state, paper, modules);
        String svg = SvgSheetWriter.write(sheet, text);
        File file = new File(DIR, name);
        Files.writeString(file.toPath(), svg, StandardCharsets.UTF_8);
        report.append(String.format(Locale.ROOT,
                "| `%s` | %s | %d | %d |%n", name,
                paper.readableName(), sheet.shapeCount(),
                text == SvgSheetWriter.Text.OUTLINES ? 0
                        : sheet.textCount()));
        sized(name, file.length());
    }

    /** One file's encoded size, for the record beside the report. */
    private static void sized(String name, long bytes) {
        sizes.append(String.format(Locale.ROOT, "| `%s` | %d |%n",
                name, bytes));
    }

    /** The sizes, which are the fonts' answer and not the sheet's. */
    private static final StringBuilder sizes = new StringBuilder();

    /** The observer's lines and the ecliptic, as the screen has them. */
    private static ChartRenderer.ReferenceLayer modules() {
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(new Observer(59.9, 10.7,
                java.time.Instant.parse("2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);
        return (g, painted) -> ReferenceInk.paint(g, painted,
                registry.collect(), ChartPalette.WHITE_PAPER);
    }
}
