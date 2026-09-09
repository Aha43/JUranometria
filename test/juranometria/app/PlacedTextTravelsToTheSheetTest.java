package juranometria.app;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One settled page, exported through the reader's own route, read
 * back out of all three formats (Sprint 31, issue #314).
 *
 * <p>The placement decision is taken once, for the paper's extent,
 * and the sheet writers replay one recording of that page. So what
 * has to be shown is that the decision survives the journey intact -
 * every label at the box the page placed it in, the ones the page
 * refused to place absent from all three files, the drawing order
 * kept, and the furniture still on top of what it covers.
 *
 * <p>Each format is read by something that did not write it: the SVG
 * by an XML parser, the PDF by its own content stream, the PNG by its
 * pixels.
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

    private static ChartScene sheetScene() {
        return Atlas.assembler().assemble(SAGITTARIUS,
                PaperSize.A4.chartWideUnits(), PaperSize.A4.chartHighUnits());
    }

    private static List<LabelPlacement.Placement> decision() {
        return RENDERER.textPlacements(ChartRenderer.TextMetrics.offscreen(),
                sheetScene(), ON_PAPER);
    }

    @Test
    void theSheetCarriesThePagesOwnPlacementIntoAllThreeFormats(
            @TempDir Path folder) throws Exception {
        List<LabelPlacement.Placement> placed = decision();
        List<LabelPlacement.Placement> drawn = new ArrayList<>();
        List<LabelPlacement.Placement> refused = new ArrayList<>();
        for (LabelPlacement.Placement one : placed) {
            (one.omitted() ? refused : drawn).add(one);
        }
        assertTrue(drawn.size() > 80, "the page has text to carry: "
                + drawn.size() + " labels");
        assertFalse(refused.isEmpty(), "and text it refused to place,"
                + " which is the half a format cannot show by drawing"
                + " something");

        // ---- one recording, three formats -------------------------
        SheetRecording sheet = ChartSheet.record(Atlas.assembler()::assemble,
                SAGITTARIUS, ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        String svg = new String(SheetWriters.write(sheet, SheetFormat.SVG,
                300), StandardCharsets.UTF_8);
        String pdf = new String(SheetWriters.write(sheet, SheetFormat.PDF,
                300), StandardCharsets.ISO_8859_1);
        BufferedImage png = ImageIO.read(new java.io.ByteArrayInputStream(
                SheetWriters.write(sheet, SheetFormat.PNG, 300)));

        // And the reader's own route writes those same bytes, so what
        // is read below is what File > Export Chart Sheet produces.
        for (SheetFormat format : SheetFormat.values()) {
            var outcome = assertInstanceOf(ExportSheet.Outcome.Written.class,
                    ExportSheet.write(Atlas.assembler()::assemble,
                            SAGITTARIUS, ChartOptions.DEFAULTS,
                            ChartRenderer.ReferenceLayer.NONE,
                            new ExportSheet.Request(format, PaperSize.A4,
                                    300, false),
                            folder.resolve("sagittarius").toFile(),
                            existing -> true),
                    format + " is written through the export route");
            assertTrue(java.util.Arrays.equals(
                            Files.readAllBytes(outcome.file()),
                            SheetWriters.write(sheet, format, 300)),
                    "and byte for byte it is this recording: " + format);
        }

        // ---- the SVG, read as XML ---------------------------------
        Map<String, double[]> inSvg = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();
        Matcher text = Pattern.compile(
                        "<text x=\"([-\\d.]+)\" y=\"([-\\d.]+)\"[^>]*>"
                                + "([^<]*)</text>")
                .matcher(svg);
        while (text.find()) {
            String what = unescape(text.group(3));
            order.add(what);
            inSvg.put(what + "@" + text.group(1) + "," + text.group(2),
                    new double[] {Double.parseDouble(text.group(1)),
                            Double.parseDouble(text.group(2))});
        }
        assertTrue(order.size() > drawn.size(),
                "the SVG carries the page's text and the grid's: "
                        + order.size() + " runs");

        var metrics = ChartRenderer.TextMetrics.offscreen();
        List<String> missing = new ArrayList<>();
        for (LabelPlacement.Placement one : drawn) {
            double ascent = ascentFor(one, metrics);
            String key = one.request().text() + "@"
                    + String.format(Locale.ROOT, "%.2f,%.2f",
                            one.at().getX() + 2.0, one.at().getY() + ascent);
            if (!inSvg.containsKey(key)) {
                missing.add(key);
            }
        }
        assertEquals(List.of(), missing,
                "every label the page placed is in the SVG at the box"
                        + " the page placed it in");

        for (LabelPlacement.Placement one : refused) {
            assertFalse(order.contains(one.request().text()),
                    "and text the page refused to place is in no format"
                            + " at all: " + one.request().text());
        }

        // ---- the order the page draws in --------------------------
        assertTrue(lastIndexOfFamily(order, drawn,
                        LabelPlacement.Family.CONSTELLATION)
                        < firstIndexOfFamily(order, drawn,
                                LabelPlacement.Family.STAR),
                "constellation names are written before star labels, as"
                        + " on screen");
        assertTrue(lastIndexOfFamily(order, drawn,
                        LabelPlacement.Family.STAR)
                        < firstIndexOfFamily(order, drawn,
                                LabelPlacement.Family.DEEP_SKY),
                "and star labels before deep-sky labels");
        int titleBlock = -1;
        for (int at = 0; at < order.size(); at++) {
            if (order.get(at).startsWith("Field 120.0°")) {
                titleBlock = at;
            }
        }
        assertTrue(titleBlock > lastIndexOfFamily(order, drawn,
                        LabelPlacement.Family.DEEP_SKY),
                "and the title block last of all, over everything");

        // ---- the PDF, read as its own content stream --------------
        String stream = pdf.substring(pdf.indexOf("stream\n") + 7,
                pdf.indexOf("endstream"));
        List<Rectangle2D> fills = filledBoxes(stream);
        int found = 0;
        for (LabelPlacement.Placement one : drawn) {
            if (glyphsIn(fills, one.at(), ascentFor(one, metrics))) {
                found++;
            }
        }
        assertEquals(drawn.size(), found,
                "the PDF has a run of glyphs in the box of every label"
                        + " the page placed");
        for (LabelPlacement.Placement one : refused) {
            assertFalse(glyphsIn(fills,
                            one.request().candidates().get(0),
                            ascentFor(one, metrics)),
                    "and nothing text-shaped where a refused label"
                            + " would have gone: " + one.request().text());
        }

        // ---- the PNG, read as pixels ------------------------------
        double scale = (double) png.getWidth()
                / PngSheetWriter.widePixels(PaperSize.A4, 72);
        assertTrue(scale > 1.0, "the PNG is the sheet at 300 dpi");
        int inked = 0;
        for (LabelPlacement.Placement one : drawn) {
            if (inkIn(png, one.at(), scale, sheet)) {
                inked++;
            }
        }
        assertEquals(drawn.size(), inked,
                "and the PNG has ink in every one of those boxes");
    }

    /** The baseline offset the renderer draws each family at. */
    private static double ascentFor(LabelPlacement.Placement one,
                                    ChartRenderer.TextMetrics metrics) {
        return one.request().family() == LabelPlacement.Family.CONSTELLATION
                ? metrics.names().getAscent()
                : metrics.labels().getAscent();
    }

    private static int firstIndexOfFamily(
            List<String> order, List<LabelPlacement.Placement> drawn,
            LabelPlacement.Family family) {
        int first = Integer.MAX_VALUE;
        for (LabelPlacement.Placement one : drawn) {
            if (one.request().family() == family) {
                int at = order.indexOf(one.request().text());
                if (at >= 0) {
                    first = Math.min(first, at);
                }
            }
        }
        return first;
    }

    private static int lastIndexOfFamily(
            List<String> order, List<LabelPlacement.Placement> drawn,
            LabelPlacement.Family family) {
        int last = -1;
        for (LabelPlacement.Placement one : drawn) {
            if (one.request().family() == family) {
                last = Math.max(last, order.lastIndexOf(one.request().text()));
            }
        }
        return last;
    }

    /**
     * Every filled path in a PDF content stream, as its bounding box.
     *
     * <p>The writer draws a label as one filled glyph outline, so a
     * label's presence is a fill whose box sits inside the box the
     * page placed it in.
     */
    private static List<Rectangle2D> filledBoxes(String stream) {
        List<Rectangle2D> boxes = new ArrayList<>();
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
                    boxes.add(box);
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
     * Whether a run of glyphs was filled where a label was placed.
     *
     * <p>Not "is there ink in the box" - a star's disc can be inside
     * a name's box and is not the name. The renderer starts a run two
     * pixels inside its box and sits it on a baseline one ascent
     * down, so that corner is what the PDF is asked for. The content
     * stream is written in the chart's own coordinates: the page
     * transform is in the page setup, not in the ink.
     */
    private static boolean glyphsIn(List<Rectangle2D> fills,
                                    Rectangle2D at, double ascent) {
        double left = at.getX() + 2.0;
        double baseline = at.getY() + ascent;
        for (Rectangle2D fill : fills) {
            double below = fill.getMaxY() - baseline;
            if (Math.abs(fill.getMinX() - left) < 2.0 && below > -1.5
                    && below < 4.0 && fill.getMaxX() <= at.getMaxX() + 1.0) {
                return true;
            }
        }
        return false;
    }

    /** Whether the rasterised sheet has ink inside a label's box. */
    private static boolean inkIn(BufferedImage png, Rectangle2D at,
                                 double scale, SheetRecording sheet) {
        // The chart sits inside the paper's own margin, which is
        // where the sheet places it.
        double margin = sheet.paper().marginPoints();
        int x0 = (int) Math.round((margin + at.getX()) * scale);
        int y0 = (int) Math.round((margin + at.getY()) * scale);
        int x1 = (int) Math.round((margin + at.getMaxX()) * scale);
        int y1 = (int) Math.round((margin + at.getMaxY()) * scale);
        for (int y = Math.max(0, y0); y < Math.min(png.getHeight(), y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(png.getWidth(), x1);
                    x++) {
                int rgb = png.getRGB(x, y) & 0xffffff;
                if (rgb != 0xffffff) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String unescape(String text) {
        return text.replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'");
    }
}
