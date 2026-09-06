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
        report.append("| file | paper | shapes | labels | bytes |\n");
        report.append("|---|---|---:|---:|---:|\n");

        write(report, "sheet-a4.svg", ORION, PaperSize.A4, false,
                SvgSheetWriter.Text.EDITABLE);
        write(report, "sheet-letter.svg", ORION, PaperSize.LETTER, false,
                SvgSheetWriter.Text.EDITABLE);
        write(report, "sheet-a4-outlines.svg", ORION, PaperSize.A4, false,
                SvgSheetWriter.Text.OUTLINES);
        write(report, "sheet-a4-modules.svg", EQUINOX, PaperSize.A4, true,
                SvgSheetWriter.Text.EDITABLE);

        report.append("\n`sheet-a4-outlines.svg` is the same chart with"
                + " every label converted to\nits outline, for a"
                + " machine whose fonts are unknown. It is larger and"
                + " it\ncannot be edited as words, which is why it is"
                + " the variant and not the\nmaster.\n\n");
        report.append("`sheet-a4-modules.svg` carries the meridian, the"
                + " horizon, the zenith and\nthe ecliptic - the March"
                + " equinox page, where the ecliptic's landmarks"
                + " are.\n\n");

        report.append("## What is not settled here\n\n");
        report.append("**Nothing on this page has been printed.** The"
                + " sizes above are arithmetic\nand the tests are"
                + " arithmetic; legibility on paper is neither."
                + " Issue #287\nowes a printed sheet measured with a"
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
    }

    private static void write(StringBuilder report, String name,
                              ChartViewState state, PaperSize paper,
                              boolean modules, SvgSheetWriter.Text text)
            throws Exception {
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, state, ChartOptions.DEFAULTS,
                modules ? modules() : ChartRenderer.ReferenceLayer.NONE,
                paper);
        String svg = SvgSheetWriter.write(sheet, text);
        File file = new File(DIR, name);
        Files.writeString(file.toPath(), svg, StandardCharsets.UTF_8);
        report.append(String.format(Locale.ROOT,
                "| `%s` | %s | %d | %d | %d |%n", name,
                paper.readableName(), sheet.shapeCount(),
                text == SvgSheetWriter.Text.OUTLINES ? 0
                        : sheet.textCount(),
                file.length()));
    }

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
