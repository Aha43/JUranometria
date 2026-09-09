package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.chart.WorkingSelection;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecorder;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One settled page, exported by a reader through the File menu and
 * read back out of all three formats (Sprint 31, issue #314).
 *
 * <p>The placement decision is taken once, for the paper's extent, and
 * the sheet writers replay one recording of that page. What has to be
 * shown is that the decision survives the whole journey: every label
 * at the box the page placed it in, the ones the page refused absent
 * from all three files, the families in the order the chart draws
 * them, and the furniture still over what it covers.
 *
 * <p>A reader's own route to these files - the shown window, the real
 * File menu item, the real export dialog and its controls - is walked
 * in {@code ExportJourneyTest}, which reads the same three formats
 * back with the same readers. This file is where each reader is put
 * to the question, because a reader that cannot fail proves nothing.
 *
 * <p>Each format is read by something that did not write it: the SVG
 * by its text elements, the PDF by the glyph outlines in its content
 * stream, the PNG by differencing renders rather than by asking
 * whether a box holds a dark pixel - a disc, a grid line or another
 * label would answer that. And each format is asked to fail: the
 * second test breaks the recording in four ways a reader would notice
 * and requires all three to show every break, so that agreement here
 * is the writers preserving the decision rather than three readers
 * agreeing about one recording.
 */
class PlacedTextTravelsToTheSheetTest {

    /** A crowded sky, where placement has work to do. */
    private static final ChartViewState SAGITTARIUS = new ChartViewState(
            new SkyPosition(281.0, -26.0), 120.0,
            ChartViewState.defaultMagnitudeFor(120.0));

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    /** The chart options a sheet is drawn with: the reader's, on paper. */
    private static final ChartOptions ON_PAPER =
            ChartOptions.DEFAULTS.withPalette(ChartPalette.WHITE_PAPER);

    /** The grey a constellation name is written in. */
    private static final java.awt.Color NAME_INK =
            new java.awt.Color(0x78, 0x78, 0x78);

    private static ChartScene sheetScene() {
        return Atlas.assembler().assemble(SAGITTARIUS,
                PaperSize.A4.chartWideUnits(), PaperSize.A4.chartHighUnits());
    }

    private static List<LabelPlacement.Placement> decision() {
        return RENDERER.textPlacements(ChartRenderer.TextMetrics.offscreen(),
                sheetScene(), ON_PAPER);
    }

    // ---- 2. every format is asked to fail ---------------------------

    @Test
    void everyFormatShowsTheDecisionBeingBroken() throws Exception {
        SheetRecording sheet = ChartSheet.record(Atlas.assembler()::assemble,
                SAGITTARIUS, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        List<LabelPlacement.Placement> placed = decision();
        LabelPlacement.Placement refused = placed.stream()
                .filter(LabelPlacement.Placement::omitted)
                .filter(one -> onTheSheet(
                        one.request().candidates().get(0), sheet))
                .findFirst().orElseThrow();
        // A name with a mark over it, because that is what putting
        // the families in the wrong order changes: a name is drawn
        // before every mark on the page, so one written last would
        // appear on top of the disc that covers it. A name nothing
        // covers would rasterise the same either way, and the control
        // would be measuring nothing.
        LabelPlacement.Placement name = placed.stream()
                .filter(one -> !one.omitted() && one.request().family()
                        == LabelPlacement.Family.CONSTELLATION)
                .filter(one -> onTheSheet(one.at(), sheet)
                        && drawnOnce(sheet, one.request().text())
                        && coveredByAMark(one.at(), sheet))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "this page has a constellation name with a mark"
                                + " drawn over it"));
        Rectangle2D would = refused.request().candidates().get(0);
        Rectangle2D block = ChartRenderer.titleBlockBounds(
                ChartRenderer.TextMetrics.offscreen().labels(),
                sheet.scene());

        record Break(String what, SheetRecording sheet) { }
        List<Break> breaks = List.of(
                // A label the page refused, drawn anyway.
                new Break("a label the page refused",
                        sheetWith(sheet, operations -> operations.add(
                                textAt(refused.request().text(), would)))),
                // A label drawn somewhere other than where it was placed.
                new Break("a label moved out of its box",
                        sheetWith(sheet, operations ->
                                moveText(operations, name.request().text(),
                                        60.0, 40.0))),
                // The families out of order: a name written last, over
                // the marks it belongs under.
                new Break("the families out of order",
                        sheetWith(sheet, operations ->
                                lastText(operations,
                                        name.request().text()))),
                // Text replayed over the furniture, which is opaque and
                // drawn last precisely so that it is not.
                new Break("text over the furniture",
                        sheetWith(sheet, operations -> operations.add(
                                textAt("OVER THE BLOCK",
                                        new Rectangle2D.Double(
                                                block.getCenterX() - 40.0,
                                                block.getCenterY() - 12.0,
                                                80.0, 15.0))))));
        for (Break broken : breaks) {
            for (SheetFormat format : SheetFormat.values()) {
                assertFalse(java.util.Arrays.equals(
                                SheetWriters.write(sheet, format, 300),
                                SheetWriters.write(broken.sheet(), format,
                                        300)),
                        format + " would carry " + broken.what());
            }
        }

        // And it is the readers above that notice, not merely the bytes.
        double ascent = ChartRenderer.TextMetrics.offscreen().names()
                .getAscent();
        SheetRecording drawsAnOmission = breaks.get(0).sheet();
        assertTrue(runAt(textRuns(new String(SheetWriters.write(
                                drawsAnOmission, SheetFormat.SVG, 300),
                        StandardCharsets.UTF_8)),
                        refused.request().text(), would, ascent),
                "the SVG reader sees the refused label drawn");
        assertTrue(glyphsAt(filledBoxes(streamOf(new String(
                        SheetWriters.write(drawsAnOmission, SheetFormat.PDF,
                                300), StandardCharsets.ISO_8859_1))),
                        would, ascent),
                "and the PDF reader sees glyphs in a box the page"
                        + " refused");
        BufferedImage plain = pngOf(sheet);
        assertTrue(differsInside(plain, pngOf(drawsAnOmission), would, sheet)
                        > 10,
                "and the PNG differs inside that box, so the absence in"
                        + " the exported sheet is a fact about the page"
                        + " rather than about invisible ink");

        SheetRecording overTheFurniture = breaks.get(3).sheet();
        assertTrue(differsInside(plain, pngOf(overTheFurniture), block,
                        sheet) > 10,
                "and text laid over the title block changes the block's"
                        + " own pixels, which is how the PNG knows the"
                        + " furniture is on top of what it covers");

        SheetRecording movedText = breaks.get(1).sheet();
        assertTrue(differsInside(plain, pngOf(movedText), name.at(), sheet)
                        > 10,
                "and a label moved out of its box empties the box the"
                        + " page placed it in");
    }

    // ---- the three readers ------------------------------------------

    /** The SVG: every run of text, where it is, and in what order. */
    void readTheSvg(String svg,
                            List<LabelPlacement.Placement> drawn,
                            List<LabelPlacement.Placement> refused) {
        List<Run> runs = textRuns(svg);
        assertTrue(runs.size() > drawn.size(),
                "the SVG carries the page's text and the grid's: "
                        + runs.size() + " runs");
        var metrics = ChartRenderer.TextMetrics.offscreen();
        List<String> missing = new ArrayList<>();
        for (LabelPlacement.Placement one : drawn) {
            if (runAt(runs, one, ascentFor(one, metrics)) < 0) {
                missing.add(one.request().text() + " at " + one.at());
            }
        }
        assertEquals(List.of(), missing,
                "every label the page placed is in the SVG at the box"
                        + " the page placed it in");
        for (LabelPlacement.Placement one : refused) {
            for (Rectangle2D candidate : one.request().candidates()) {
                assertFalse(runAt(runs, one.request().text(), candidate,
                                ascentFor(one, metrics)),
                        "and the text the page refused is drawn at none"
                                + " of its positions: "
                                + one.request().text());
            }
        }
        assertOrder("the SVG", spans(runs, drawn, metrics,
                        PlacedTextTravelsToTheSheetTest::runAt),
                lastTitleBlockRun(runs));
    }

    /** The PDF: the glyph outlines its content stream fills. */
    void readThePdf(String pdf,
                            List<LabelPlacement.Placement> drawn,
                            List<LabelPlacement.Placement> refused) {
        List<Run> fills = filledBoxes(streamOf(pdf));
        var metrics = ChartRenderer.TextMetrics.offscreen();
        int found = 0;
        for (LabelPlacement.Placement one : drawn) {
            if (glyphRunAt(fills, one, ascentFor(one, metrics)) >= 0) {
                found++;
            }
        }
        assertEquals(drawn.size(), found,
                "the PDF fills a run of glyphs in the box of every label"
                        + " the page placed");
        for (LabelPlacement.Placement one : refused) {
            for (Rectangle2D candidate : one.request().candidates()) {
                assertFalse(glyphsAt(fills, candidate,
                                ascentFor(one, metrics)),
                        "and nothing text-shaped at any position it"
                                + " refused: " + one.request().text());
            }
        }
        assertOrder("the PDF", spans(fills, drawn, metrics,
                        PlacedTextTravelsToTheSheetTest::glyphRunAt),
                lastTitleBlockFill(fills));
    }

    /**
     * The PNG: what changes when one thing is taken away.
     *
     * <p>Asking whether a box holds a dark pixel proves nothing - a
     * star's disc, a grid line or another label would answer yes. So a
     * label is measured by writing the sheet again without it and
     * differencing: the pixels that change are that label's, and they
     * are inside the box the page placed it in and nowhere else.
     */
    void readThePng(BufferedImage png, SheetRecording sheet,
                            List<LabelPlacement.Placement> drawn,
                            List<LabelPlacement.Placement> refused)
            throws Exception {
        assertEquals(PngSheetWriter.widePixels(PaperSize.A4, 300),
                png.getWidth(), "the PNG is the whole sheet at 300 dpi");
        // One label of each family, because each difference costs a
        // whole sheet rasterised again.
        for (LabelPlacement.Family family : List.of(
                LabelPlacement.Family.STAR, LabelPlacement.Family.DEEP_SKY,
                LabelPlacement.Family.CONSTELLATION)) {
            LabelPlacement.Placement one = drawn.stream()
                    .filter(each -> each.request().family() == family
                            && onTheSheet(each.at(), sheet)
                            && drawnOnce(sheet, each.request().text()))
                    .findFirst().orElseThrow();
            BufferedImage without = pngOf(sheetWithout(sheet,
                    one.request().text()));
            int inside = differsInside(png, without, one.at(), sheet);
            int outside = differsOutside(png, without, one.at(), sheet);
            assertTrue(inside > 10, family + ": the PNG carries "
                    + one.request().text() + " - " + inside
                    + " px change when it is taken away");
            assertEquals(0, outside, family + ": and every pixel it"
                    + " changes is inside the box the page placed it in");
        }
        for (LabelPlacement.Placement one : refused) {
            for (Rectangle2D candidate : one.request().candidates()) {
                if (!onTheSheet(candidate, sheet)) {
                    continue;
                }
                BufferedImage anyway = pngOf(sheetWith(sheet,
                        operations -> operations.add(
                                textAt(one.request().text(), candidate))));
                assertTrue(differsInside(png, anyway, candidate, sheet) > 10,
                        "the exported sheet does not carry "
                                + one.request().text() + ", and the PNG"
                                + " would show it there if it did");
            }
        }
    }

    // ---- what the readers are asked ---------------------------------

    /** One run of text, or of glyph outlines, in drawing order. */
    private record Run(String text, Rectangle2D box, int at) {
    }

    /**
     * How a format is asked where a label is. The two differ because
     * the SVG says where it starts the string and the PDF says where
     * the glyphs it filled ended up.
     */
    @FunctionalInterface
    private interface Locator {
        int at(List<Run> runs, LabelPlacement.Placement one, double ascent);
    }

    private static List<Run> textRuns(String svg) {
        List<Run> runs = new ArrayList<>();
        Matcher text = Pattern.compile(
                        "<text x=\"([-\\d.]+)\" y=\"([-\\d.]+)\"[^>]*>"
                                + "([^<]*)</text>")
                .matcher(svg);
        while (text.find()) {
            runs.add(new Run(unescape(text.group(3)),
                    new Rectangle2D.Double(
                            Double.parseDouble(text.group(1)),
                            Double.parseDouble(text.group(2)), 0, 0),
                    runs.size()));
        }
        return runs;
    }

    /**
     * Where a label sits in a format, or -1.
     *
     * <p>The corner, not the neighbourhood: the renderer starts a run
     * two pixels inside its box on a baseline one ascent down, so that
     * point is what each format is asked for. Ink that merely happens
     * to be in the box cannot answer.
     */
    private static int runAt(List<Run> runs, LabelPlacement.Placement one,
                             double ascent) {
        for (Run run : runs) {
            if (run.text() != null
                    && !run.text().equals(one.request().text())) {
                continue;
            }
            if (Math.abs(run.box().getX() - (one.at().getX() + 2.0)) < 1.0
                    && Math.abs(run.box().getY()
                            - (one.at().getY() + ascent)) < 1.0) {
                return run.at();
            }
        }
        return -1;
    }

    /**
     * Whether a named run of text is drawn at a box's own corner.
     *
     * <p>Both halves matter. The text alone is not enough - a page
     * draws a dozen labels reading the same Greek letter - and the
     * corner alone is not either, because another label's origin can
     * fall inside a constellation name's candidate, which is the size
     * of a word.
     */
    private static boolean runAt(List<Run> runs, String text,
                                 Rectangle2D box, double ascent) {
        for (Run run : runs) {
            if (!text.equals(run.text())) {
                continue;
            }
            if (Math.abs(run.box().getX() - (box.getX() + 2.0)) < 1.0
                    && Math.abs(run.box().getY()
                            - (box.getY() + ascent)) < 1.0) {
                return true;
            }
        }
        return false;
    }

    /** Where each family's text sits in a format's own order. */
    private static Map<LabelPlacement.Family, int[]> spans(
            List<Run> runs, List<LabelPlacement.Placement> drawn,
            ChartRenderer.TextMetrics metrics, Locator locator) {
        Map<LabelPlacement.Family, int[]> span = new LinkedHashMap<>();
        for (LabelPlacement.Placement one : drawn) {
            int at = locator.at(runs, one, ascentFor(one, metrics));
            if (at < 0) {
                continue;
            }
            int[] range = span.computeIfAbsent(one.request().family(),
                    key -> new int[] {Integer.MAX_VALUE, -1});
            range[0] = Math.min(range[0], at);
            range[1] = Math.max(range[1], at);
        }
        return span;
    }

    private static void assertOrder(String what,
                                    Map<LabelPlacement.Family, int[]> span,
                                    int furniture) {
        int[] names = span.get(LabelPlacement.Family.CONSTELLATION);
        int[] stars = span.get(LabelPlacement.Family.STAR);
        int[] deepSky = span.get(LabelPlacement.Family.DEEP_SKY);
        assertTrue(names != null && stars != null && deepSky != null,
                what + " carries all three families");
        assertTrue(names[1] < stars[0], what + ": constellation names"
                + " are written before star labels, as on screen");
        assertTrue(stars[1] < deepSky[0],
                what + ": and star labels before deep-sky labels");
        assertTrue(furniture > deepSky[1], what + ": and the furniture"
                + " after all of them, over what it covers");
    }

    /** Where the title block's own text ends in the SVG. */
    private static int lastTitleBlockRun(List<Run> runs) {
        int last = -1;
        for (Run run : runs) {
            if (run.text() != null && run.text().startsWith("Field ")) {
                last = Math.max(last, run.at());
            }
        }
        return last;
    }

    /** Where the title block is filled in the PDF. */
    private static int lastTitleBlockFill(List<Run> fills) {
        int last = -1;
        for (Run fill : fills) {
            if (fill.box().getWidth() > 200.0
                    && fill.box().getHeight() > 30.0) {
                last = Math.max(last, fill.at());
            }
        }
        return last;
    }

    private static String streamOf(String pdf) {
        return pdf.substring(pdf.indexOf("stream\n") + 7,
                pdf.indexOf("endstream"));
    }

    /** Every filled path in a PDF content stream, in order. */
    private static List<Run> filledBoxes(String stream) {
        List<Run> boxes = new ArrayList<>();
        Rectangle2D.Double box = null;
        Pattern point = Pattern.compile("^([-\\d. ]+) (m|l|c)$");
        for (String line : stream.split("\n")) {
            Matcher along = point.matcher(line);
            if (along.matches()) {
                // A glyph is drawn in curves, so the numbers that
                // matter are all of them, not the first pair.
                String[] numbers = along.group(1).trim().split(" ");
                for (int at = 0; at + 1 < numbers.length; at += 2) {
                    double x = Double.parseDouble(numbers[at]);
                    double y = Double.parseDouble(numbers[at + 1]);
                    if (box == null) {
                        box = new Rectangle2D.Double(x, y, 0, 0);
                    } else {
                        box.add(x, y);
                    }
                }
            } else if (line.equals("f")) {
                if (box != null) {
                    boxes.add(new Run(null, box, boxes.size()));
                }
                box = null;
            } else if (!line.equals("h")) {
                // Anything else ends the path without filling it - a
                // stroke, or the clip rectangle, which is not ink.
                box = null;
            }
        }
        return boxes;
    }

    /**
     * Where a run of glyphs was filled at a box's own corner, or -1.
     *
     * <p>A filled path answers only if it starts where the renderer
     * starts a string - two pixels inside the box - and sits on the
     * baseline one ascent down, ending inside the box's own width. A
     * disc or a symbol in the same box is neither.
     */
    private static int glyphRunAt(List<Run> fills,
                                  LabelPlacement.Placement one,
                                  double ascent) {
        return glyphRunAt(fills, one.at(), ascent);
    }

    private static int glyphRunAt(List<Run> fills, Rectangle2D at,
                                  double ascent) {
        double left = at.getX() + 2.0;
        double baseline = at.getY() + ascent;
        for (Run fill : fills) {
            double below = fill.box().getMaxY() - baseline;
            if (Math.abs(fill.box().getMinX() - left) < 2.0 && below > -1.5
                    && below < 4.0
                    && fill.box().getMaxX() <= at.getMaxX() + 1.0) {
                return fill.at();
            }
        }
        return -1;
    }

    private static boolean glyphsAt(List<Run> fills, Rectangle2D at,
                                    double ascent) {
        return glyphRunAt(fills, at, ascent) >= 0;
    }

    // ---- the sheet, broken on purpose -------------------------------

    /** The same recording with its operations changed. */
    private static SheetRecording sheetWith(SheetRecording sheet,
            java.util.function.Consumer<List<SheetRecorder.Operation>> change) {
        SheetRecorder mutant = new SheetRecorder(
                sheet.paper().chartWideUnits(),
                sheet.paper().chartHighUnits());
        List<SheetRecorder.Operation> operations =
                new ArrayList<>(sheet.recorder().operations());
        change.accept(operations);
        mutant.operations().addAll(operations);
        return new SheetRecording(mutant, sheet.paper(), sheet.scene(),
                sheet.options(), sheet.metadata());
    }

    /** The same recording with one run of text left out. */
    private static SheetRecording sheetWithout(SheetRecording sheet,
                                               String text) {
        return sheetWith(sheet, operations -> operations.removeIf(
                each -> each instanceof SheetRecorder.Text run
                        && run.text().equals(text)));
    }

    /** Whether a mark is drawn over this box, after the text under it. */
    private static boolean coveredByAMark(Rectangle2D box,
                                          SheetRecording sheet) {
        for (ChartRenderer.DrawnMark mark : RENDERER.drawnMarks(
                sheet.scene(), sheet.options().withPalette(
                        ChartPalette.WHITE_PAPER))) {
            if (mark.ink() != null && mark.ink().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /** Whether this page draws a run of text exactly once. */
    private static boolean drawnOnce(SheetRecording sheet, String text) {
        int found = 0;
        for (SheetRecorder.Text run : sheet.recorder().text()) {
            if (run.text().equals(text)) {
                found++;
            }
        }
        return found == 1;
    }

    private static SheetRecorder.Text textAt(String text, Rectangle2D box) {
        return new SheetRecorder.Text(text, box.getX() + 2.0,
                box.getY() + ChartRenderer.TextMetrics.offscreen().names()
                        .getAscent(),
                ChartRenderer.constellationNameFont(), NAME_INK, null);
    }

    private static void moveText(List<SheetRecorder.Operation> operations,
                                 String text, double byX, double byY) {
        for (int at = 0; at < operations.size(); at++) {
            if (operations.get(at) instanceof SheetRecorder.Text run
                    && run.text().equals(text)) {
                operations.set(at, new SheetRecorder.Text(run.text(),
                        run.x() + byX, run.y() + byY, run.font(),
                        run.colour(), run.clip()));
                return;
            }
        }
        throw new AssertionError("the page draws " + text);
    }

    private static void lastText(List<SheetRecorder.Operation> operations,
                                 String text) {
        for (int at = 0; at < operations.size(); at++) {
            if (operations.get(at) instanceof SheetRecorder.Text run
                    && run.text().equals(text)) {
                operations.add(operations.remove(at));
                return;
            }
        }
        throw new AssertionError("the page draws " + text);
    }

    // ---- pixels ------------------------------------------------------

    private static BufferedImage pngOf(SheetRecording sheet)
            throws Exception {
        return ImageIO.read(new ByteArrayInputStream(
                SheetWriters.write(sheet, SheetFormat.PNG, 300)));
    }

    private static boolean onTheSheet(Rectangle2D box, SheetRecording sheet) {
        return box.getMinX() > 0 && box.getMinY() > 0
                && box.getMaxX() < sheet.paper().chartWideUnits()
                && box.getMaxY() < sheet.paper().chartHighUnits();
    }

    private static int differsInside(BufferedImage one, BufferedImage other,
                                     Rectangle2D box, SheetRecording sheet) {
        return differing(one, other, box, sheet, true);
    }

    private static int differsOutside(BufferedImage one, BufferedImage other,
                                      Rectangle2D box, SheetRecording sheet) {
        return differing(one, other, box, sheet, false);
    }

    private static int differing(BufferedImage one, BufferedImage other,
                                 Rectangle2D box, SheetRecording sheet,
                                 boolean inside) {
        double scale = one.getWidth() / sheet.paper().widePoints();
        double margin = sheet.paper().marginPoints();
        Rectangle2D on = new Rectangle2D.Double(
                (margin + box.getX()) * scale - 2.0,
                (margin + box.getY()) * scale - 2.0,
                box.getWidth() * scale + 4.0,
                box.getHeight() * scale + 4.0);
        int count = 0;
        for (int y = 0; y < one.getHeight(); y++) {
            for (int x = 0; x < one.getWidth(); x++) {
                if (one.getRGB(x, y) == other.getRGB(x, y)) {
                    continue;
                }
                if (on.contains(x, y) == inside) {
                    count++;
                }
            }
        }
        return count;
    }

    private static double ascentFor(LabelPlacement.Placement one,
                                    ChartRenderer.TextMetrics metrics) {
        return one.request().family() == LabelPlacement.Family.CONSTELLATION
                ? metrics.names().getAscent()
                : metrics.labels().getAscent();
    }

    private static String unescape(String text) {
        return text.replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'");
    }
}
