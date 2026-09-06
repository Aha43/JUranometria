package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.sky.Ecliptic;
import juranometria.sky.GreatCircle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the printable-chart gate promised (Sprint 29, issue #283).
 *
 * <p>A design gate changes nothing a reader can reach, and its
 * central measurements should not be able to rot quietly between now
 * and #284. Both are held here.
 */
class PrintableChartGateTest {

    @Test
    void theWiderFieldIsNotReachableFromTheApplicationYet() {
        // The gate decided 42 degrees; #284 implements it. Until then
        // the released step sequence is exactly what it was, and a
        // reader cannot reach a field the atlas has not yet decided
        // how to draw honestly.
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewState(new SkyPosition(83.0, 0.0),
                        42.0, 6.0),
                "42 degrees is not a released step during the gate");
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewState(new SkyPosition(83.0, 0.0),
                        48.0, 6.0),
                "and neither is the field the gate rejected");

        ChartViewState widest = new ChartViewState(
                new SkyPosition(83.0, 0.0), 36.0, 6.0);
        assertEquals(36.0, widest.fieldWidthDegrees(),
                "36 degrees is still the atlas's widest");
    }

    @Test
    void gnomonicKeepsGreatCirclesStraightAtEveryCandidateField() {
        // The reason the gate stayed gnomonic. If this ever stops
        // holding, the analytic clipping three modules' ink depends
        // on has stopped holding with it.
        for (double field : new double[] {36.0, 42.0, 48.0}) {
            assertEquals(0.0, sagitta(field), 1.0e-6,
                    "a great circle is exactly straight at " + field
                            + " degrees, which is what lets the chart"
                            + " clip it analytically");
        }
    }

    @Test
    void theStereographicCandidateBowsAndTheMeasurementCanTell() {
        // The control: the same reading of the rejected candidate
        // finds the bow the decision was made on, so the zeros above
        // are an answer rather than a silence.
        double bow = stereographicSagitta(42.0);
        assertTrue(bow > 5.0, "the rejected candidate bows measurably: "
                + bow + " px");
    }

    @Test
    void theCartographyDrawsNothingAChartSheetCannotCarry()
            throws Exception {
        // The gate's architectural claim, executed: the recorder
        // throws on everything outside the seventeen vector methods,
        // so a render that completes through it is a render a vector
        // sheet can hold entirely.
        ChartSheetRecorder recorder = new ChartSheetRecorder(400, 300);
        Graphics2D g = (Graphics2D) recorder.create();
        g.setColor(Color.BLACK);
        g.setStroke(new java.awt.BasicStroke(1.0f));
        g.draw(new Ellipse2D.Double(10, 10, 20, 20));
        g.fill(new Ellipse2D.Double(40, 40, 6, 6));
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF,
                java.awt.Font.PLAIN, 10));
        g.drawString("α", 50, 50);
        g.dispose();

        assertEquals(2, recorder.drawn().size(),
                "shapes are recorded as shapes");
        assertEquals(1, recorder.text().size(),
                "and text as text, with its own characters");
        assertEquals("α", recorder.text().get(0).text(),
                "including the notation a base-14 PDF font cannot"
                        + " name");

        // And it refuses rather than rasterising.
        assertThrows(UnsupportedOperationException.class,
                () -> recorder.drawImage(
                        new java.awt.image.BufferedImage(1, 1,
                                java.awt.image.BufferedImage
                                        .TYPE_INT_RGB), 0, 0, null),
                "an image would be a raster on a vector sheet, and is"
                        + " refused rather than quietly accepted");
    }

    @Test
    void theStudyReportsSayWhatTheDecisionQuotes() throws IOException {
        // The decision document quotes the study. If a number moves,
        // one of the two is wrong, and this says which.
        String measurements = Files.readString(Path.of(
                "docs/studies/printable-chart/measurements.md"));
        String formats = Files.readString(Path.of(
                "docs/studies/printable-chart/formats.md"));
        String decision = Files.readString(Path.of(
                "docs/decisions/printable-chart.md"));

        for (String quoted : List.of("+11.2%", "+8.1%", "+14.8%")) {
            assertTrue(measurements.contains(quoted),
                    "the study measured " + quoted);
            assertTrue(decision.contains(quoted),
                    "and the decision quotes it: " + quoted);
        }
        assertTrue(formats.contains("3508 × 2480"),
                "the format report states the sheet actually written");
        assertTrue(decision.contains("3508 × 2480"),
                "and the decision states the same one");
        assertFalse(formats.contains("3208"),
                "and neither still specifies the rejected PNG");
        assertFalse(decision.contains("3208"),
                "in either place");
        assertTrue(formats.contains("2268 shapes"),
                "the format study recorded the production render");
        assertTrue(decision.contains("2268 shapes"),
                "and the decision quotes that too");
        assertTrue(measurements.contains("Fanafjellet"),
                "and the reader who asked is in the study, not only"
                        + " in the decision");
    }

    @Test
    void theSheetPrototypesAreWhatTheyClaim() throws IOException {
        Path pdf = Path.of("docs/studies/printable-chart/sheet-a4.pdf");
        String content = Files.readString(pdf,
                java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse(content.contains("/Image"),
                "the PDF carries no image: a raster wrapped in a PDF"
                        + " does not satisfy a vector claim");
        assertFalse(content.contains("/DCTDecode"),
                "nor a compressed photograph");
        assertTrue(content.contains(" m\n") || content.contains(" m\r\n"),
                "and it does carry path geometry");

        String svg = Files.readString(Path.of(
                "docs/studies/printable-chart/sheet-a4.svg"));
        assertFalse(svg.contains("<image"),
                "the SVG carries no image either");
        assertFalse(svg.contains("xlink:href"),
                "and references nothing outside itself");
        assertTrue(svg.contains("α"),
                "and keeps the chart's own notation as text");
    }

    @Test
    void aQuadraticReachesThePdfAsTheSameCurve() {
        // The review's own check: a deliberately ASYMMETRIC quadratic,
        // compared as geometry rather than by opening the file. An
        // earlier version emitted the quadratic's control point twice
        // as both cubic controls, which is a different curve and
        // changed the page silently.
        java.awt.geom.Path2D.Double quadratic =
                new java.awt.geom.Path2D.Double();
        quadratic.moveTo(10.0, 20.0);
        quadratic.quadTo(90.0, 30.0, 40.0, 110.0);

        String pdf = ChartSheetExportStudyMain.pdfPath(quadratic);
        java.util.List<Double> numbers = new java.util.ArrayList<>();
        for (String token : pdf.replace("m", " ").replace("c", " ")
                .trim().split("\\s+")) {
            numbers.add(Double.parseDouble(token));
        }
        assertEquals(8, numbers.size(),
                "a move and one cubic: " + pdf);

        double p0x = numbers.get(0);
        double p0y = numbers.get(1);
        double c1x = numbers.get(2);
        double c1y = numbers.get(3);
        double c2x = numbers.get(4);
        double c2y = numbers.get(5);
        double p2x = numbers.get(6);
        double p2y = numbers.get(7);

        // Sampled along the curve, the emitted cubic must BE the
        // quadratic - not merely start and end where it does.
        for (double t = 0.0; t <= 1.0; t += 0.05) {
            double u = 1.0 - t;
            double qx = u * u * 10.0 + 2 * u * t * 90.0 + t * t * 40.0;
            double qy = u * u * 20.0 + 2 * u * t * 30.0 + t * t * 110.0;
            double cx = u * u * u * p0x + 3 * u * u * t * c1x
                    + 3 * u * t * t * c2x + t * t * t * p2x;
            double cy = u * u * u * p0y + 3 * u * u * t * c1y
                    + 3 * u * t * t * c2y + t * t * t * p2y;
            assertEquals(qx, cx, 0.02,
                    "the PDF curve is the source curve at t=" + t);
            assertEquals(qy, cy, 0.02,
                    "in both directions, at t=" + t);
        }

        // And the control points are genuinely different from each
        // other, so the check above could have failed.
        assertTrue(Math.hypot(c1x - c2x, c1y - c2y) > 1.0,
                "an asymmetric quadratic has two distinct cubic"
                        + " controls: " + c1x + "," + c1y + " and "
                        + c2x + "," + c2y);
    }

    /** The chart rectangle a sheet's ink must stay inside, in points. */
    private static final double A4_CHART_WIDE = 841.89 - 72.0;
    private static final double A4_CHART_HIGH = 595.28 - 72.0;
    private static final double LETTER_CHART_WIDE = 792.0 - 72.0;
    private static final double LETTER_CHART_HIGH = 612.0 - 72.0;

    @Test
    void everyVectorFormatClipsToProductionsOwnRectangleAndItCutsInk()
            throws IOException {
        // Counting clip attributes proves clipping syntax. Replacing
        // every clip with a wrong one keeps the count and restores
        // the margin bleed, and a narrower clip than production's
        // would crop the chart instead (PR #288 rounds 2 and 3). So
        // hold the clip's own rectangle, in both vector formats, and
        // prove it does work.
        for (String[] sheet : new String[][] {
                {"sheet-a4.svg", "770", "523", "text"},
                {"sheet-a4-text-as-paths.svg", "770", "523", "paths"},
                {"sheet-letter.svg", "720", "540", "text"},
                {"sheet-a4.pdf", "770", "523", "paths"}}) {
            String name = sheet[0];
            double chartWide = Double.parseDouble(sheet[1]);
            double chartHigh = Double.parseDouble(sheet[2]);
            boolean editableText = sheet[3].equals("text");
            byte[] bytes = Files.readAllBytes(
                    Path.of("docs/studies/printable-chart", name));
            ClipEvidence evidence = name.endsWith(".pdf")
                    ? pdfEvidence(bytes, chartWide, chartHigh)
                    : svgEvidence(new String(bytes,
                            java.nio.charset.StandardCharsets.UTF_8),
                            chartWide, chartHigh);

            // Production sets the chart clip to the paper inset by a
            // pixel on each side. Every clip the file carries is that
            // rectangle: not a wider one, which would bleed into the
            // margin, and not a narrower one, which would crop.
            assertFalse(evidence.clips().isEmpty(),
                    name + " carries clips at all");
            for (double[] clip : evidence.clips()) {
                assertEquals(1.0, clip[0], 0.6,
                        name + " clips from production's own left"
                                + " edge: " + java.util.Arrays
                                .toString(clip));
                assertEquals(1.0, clip[1], 0.6,
                        name + " and its own top edge");
                assertEquals(chartWide - 1.0, clip[2], 0.6,
                        name + " to its own right edge");
                assertEquals(chartHigh - 1.0, clip[3], 0.6,
                        name + " and its own bottom edge");
            }

            // And the clip is doing work: ink whose own geometry
            // crosses the chart boundary exists, and all of it is
            // drawn inside a clip rather than left to reach paper.
            assertTrue(evidence.escapingClipped()
                            + evidence.escapingUnclipped() > 0,
                    name + " has ink crossing the chart boundary, so"
                            + " the check below could have failed");
            assertEquals(0, evidence.escapingUnclipped(),
                    name + " draws none of that ink unclipped, which"
                            + " is what would bleed into the"
                            + " half-inch margin");

            // Labels are ink too, and on the editable sheets they are
            // text operations rather than paths - which the oracle
            // did not look at, while two committed constellation
            // labels are anchored outside the chart (PR #288 round
            // 4). Their clips are the only thing keeping them off the
            // margin.
            if (editableText) {
                assertTrue(evidence.textEscapingClipped()
                                + evidence.textEscapingUnclipped() > 0,
                        name + " has labels anchored outside the chart"
                                + " rectangle, so the check below"
                                + " could have failed");
                assertEquals(0, evidence.textEscapingUnclipped(),
                        name + " clips every one of them - an"
                                + " unclipped label is a name printed"
                                + " out in the half-inch margin");
            } else {
                assertEquals(0, evidence.textEscapingClipped()
                                + evidence.textEscapingUnclipped(),
                        name + " keeps no text as text, so the paths"
                                + " above are all of its ink");
            }
        }

        // Which is the whole of the PDF's ink: it carries no text
        // operator at all, so nothing in it escapes the path oracle.
        String pdf = Files.readString(
                Path.of("docs/studies/printable-chart/sheet-a4.pdf"),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        assertFalse(pdf.contains("BT "),
                "the shipped PDF draws its text as outlines, so the"
                        + " path reading above covers every mark on"
                        + " it - the base-14 prototype that does use"
                        + " text operators is kept only as the record"
                        + " of why");
    }

    /**
     * What a vector sheet says about clipping: the distinct clip
     * rectangles it carries, and how much of the ink that crosses the
     * chart boundary is drawn inside a clip and how much is not.
     */
    private record ClipEvidence(java.util.List<double[]> clips,
                                int escapingClipped,
                                int escapingUnclipped,
                                int textEscapingClipped,
                                int textEscapingUnclipped) {
    }

    private static ClipEvidence svgEvidence(String svg, double chartWide,
                                            double chartHigh) {
        java.util.Map<String, double[]> defined = new java.util.HashMap<>();
        java.util.regex.Matcher declares = java.util.regex.Pattern.compile(
                        "<clipPath id=\"(clip\\d+)\"><path d=\"([^\"]+)\"")
                .matcher(svg);
        while (declares.find()) {
            defined.put(declares.group(1), boundsOf(declares.group(2)));
        }

        java.util.List<double[]> used = new java.util.ArrayList<>();
        int clipped = 0;
        int unclipped = 0;
        java.util.regex.Matcher paths = java.util.regex.Pattern.compile(
                "<path d=\"([^\"]+)\"([^/]*)/>").matcher(svg);
        while (paths.find()) {
            if (svg.lastIndexOf("<clipPath", paths.start())
                    > svg.lastIndexOf("</clipPath>", paths.start())) {
                continue;  // a clip definition, not ink
            }
            java.util.regex.Matcher carried = java.util.regex.Pattern
                    .compile("url\\(#(clip\\d+)\\)")
                    .matcher(paths.group(2));
            double[] clip = carried.find()
                    ? defined.get(carried.group(1)) : null;
            if (clip != null && !used.contains(clip)) {
                used.add(clip);
            }
            if (escapes(boundsOf(paths.group(1)), chartWide, chartHigh)) {
                if (clip == null) {
                    unclipped++;
                } else {
                    clipped++;
                }
            }
        }

        // The same reading of the label operations. A text run's full
        // extent needs the font to measure, but its anchor does not,
        // and an anchor outside the chart rectangle is already proof
        // the run leaves it.
        int textClipped = 0;
        int textUnclipped = 0;
        java.util.regex.Matcher labels = java.util.regex.Pattern.compile(
                        "<text x=\"(-?[\\d.]+)\" y=\"(-?[\\d.]+)\"([^>]*)>")
                .matcher(svg);
        while (labels.find()) {
            double x = Double.parseDouble(labels.group(1));
            double y = Double.parseDouble(labels.group(2));
            java.util.regex.Matcher carried = java.util.regex.Pattern
                    .compile("url\\(#(clip\\d+)\\)")
                    .matcher(labels.group(3));
            double[] clip = carried.find()
                    ? defined.get(carried.group(1)) : null;
            if (clip != null && !used.contains(clip)) {
                used.add(clip);
            }
            if (escapes(new double[] {x, y, x, y}, chartWide, chartHigh)) {
                if (clip == null) {
                    textUnclipped++;
                } else {
                    textClipped++;
                }
            }
        }
        return new ClipEvidence(used, clipped, unclipped, textClipped,
                textUnclipped);
    }

    /**
     * The same reading of a PDF, taken from its content stream. The
     * stream is written uncompressed, one operator to a line, in the
     * chart's own top-left space, so a clip reads as the {@code q}
     * … {@code W n} … {@code Q} block it is.
     */
    private static ClipEvidence pdfEvidence(byte[] pdf, double chartWide,
                                            double chartHigh) {
        String file = new String(pdf,
                java.nio.charset.StandardCharsets.ISO_8859_1);
        String stream = file.substring(file.indexOf("stream\n") + 7,
                file.indexOf("endstream"));

        java.util.List<double[]> used = new java.util.ArrayList<>();
        java.util.List<Double> numbers = new java.util.ArrayList<>();
        double[] active = null;
        int clipped = 0;
        int unclipped = 0;
        for (String line : stream.split("\n")) {
            if (line.equals("q")) {
                numbers.clear();
            } else if (line.equals("W n")) {
                double[] clip = boundsOfNumbers(numbers);
                active = clip;
                if (!used.contains(clip)) {
                    used.add(clip);
                }
                numbers.clear();
            } else if (line.equals("Q")) {
                active = null;
                numbers.clear();
            } else if (line.endsWith(" m") || line.endsWith(" l")
                    || line.endsWith(" c")) {
                java.util.regex.Matcher each = java.util.regex.Pattern
                        .compile("-?\\d+(?:\\.\\d+)?").matcher(line);
                while (each.find()) {
                    numbers.add(Double.parseDouble(each.group()));
                }
            } else if (line.equals("f") || line.equals("S")) {
                if (!numbers.isEmpty() && escapes(
                        boundsOfNumbers(numbers), chartWide, chartHigh)) {
                    if (active == null) {
                        unclipped++;
                    } else {
                        clipped++;
                    }
                }
                numbers.clear();
            }
        }
        return new ClipEvidence(used, clipped, unclipped, 0, 0);
    }

    /** Whether a bounding box reaches outside the chart rectangle. */
    private static boolean escapes(double[] box, double chartWide,
                                   double chartHigh) {
        return box[0] < -0.5 || box[1] < -0.5
                || box[2] > chartWide + 0.5 || box[3] > chartHigh + 0.5;
    }

    @Test
    void theLetterChartIsLaidOutInsideLettersOwnRectangle()
            throws IOException {
        // Differing root widths and file lengths were already true of
        // the defect this replaces: the original wrote both page
        // sizes while reusing A4's recording (PR #288 round 2). What
        // distinguishes them is the rectangle the ink is laid out in,
        // read after clipping, which is what reaches the paper.
        double[] a4 = inkBounds(Files.readString(Path.of(
                "docs/studies/printable-chart/sheet-a4.svg")));
        double[] letter = inkBounds(Files.readString(Path.of(
                "docs/studies/printable-chart/sheet-letter.svg")));

        // Letter's ink occupies Letter's own chart rectangle: half
        // inch margins all round, 720 x 540 pt of chart.
        assertEquals(0.0, letter[0], 2.0, "Letter's ink starts at the"
                + " left margin");
        assertEquals(0.0, letter[1], 2.0, "and at the top one");
        assertEquals(LETTER_CHART_WIDE, letter[2], 2.0,
                "and reaches Letter's own chart width");
        assertEquals(LETTER_CHART_HIGH, letter[3], 2.0,
                "and its own chart height - an A4 layout dropped in"
                        + " here would stop " + String.format(
                        java.util.Locale.ROOT, "%.0f",
                        LETTER_CHART_HIGH - (A4_CHART_HIGH))
                        + " pt short of the bottom");

        // Which is a different rectangle from A4's, so the assertions
        // above could have failed: A4's chart is wider and shorter.
        assertEquals(A4_CHART_WIDE, a4[2], 2.0,
                "A4's ink fills A4's own width");
        assertEquals(A4_CHART_HIGH, a4[3], 2.0, "and its own height");
        assertTrue(a4[2] - letter[2] > 40.0,
                "the two rectangles differ in width: " + a4[2]
                        + " against " + letter[2]);
        assertTrue(letter[3] - a4[3] > 10.0,
                "and in height, the direction that catches a reused"
                        + " A4 recording: " + letter[3] + " against "
                        + a4[3]);
    }

    @Test
    void thePngIsAPhysicalSheetAndNotALargeCanvasOfScreenInk()
            throws IOException {
        // The original defect was not the canvas size or the missing
        // metadata; it was that every label and stroke shrank
        // fourfold against the paper. Deleting the dpi/72 transform
        // keeps the dimensions and the pHYs chunk (PR #288 round 2),
        // so this measures the ink.
        int dpi = 300;
        double scale = dpi / 72.0;
        java.awt.image.BufferedImage sheet = javax.imageio.ImageIO.read(
                Path.of("docs/studies/printable-chart",
                        "sheet-a4-300dpi.png").toFile());

        assertEquals(3508, sheet.getWidth(),
                "the whole A4 sheet at 300 dpi");
        assertEquals(2480, sheet.getHeight(), "in both directions");

        // It also says so to whatever opens it: without the pHYs
        // chunk a printer has only pixels and will fit them to the
        // page, which is the whole reason the file is this size
        // (PR #288 round 3).
        int[] physical = pngPhysicalResolution(Files.readAllBytes(
                Path.of("docs/studies/printable-chart",
                        "sheet-a4-300dpi.png")));
        assertEquals(1, physical[2],
                "the pHYs chunk states its unit as the metre, which is"
                        + " the only unit that fixes a physical size");
        int perMetre = (int) Math.round(dpi / 0.0254);
        assertEquals(perMetre, physical[0],
                "and " + perMetre + " px/m across, which is " + dpi
                        + " dpi");
        assertEquals(perMetre, physical[1], "and the same down");

        // Where the ink is: the half-inch margins must be clear and
        // the chart must fill the rectangle inside them. Under the
        // old defect the chart occupied only the top-left 770x523 px.
        int marginPx = (int) Math.round(36.0 * scale);
        int[] bounds = inkBoundsOf(sheet);
        assertEquals(marginPx, bounds[0], 3,
                "ink starts at the half-inch margin, " + marginPx
                        + " px in");
        assertEquals(marginPx, bounds[1], 3, "at the top too");
        assertEquals(sheet.getWidth() - marginPx, bounds[2], 3,
                "and stops at the far margin, which the old"
                        + " pixel-sized render never reached");
        assertEquals(sheet.getHeight() - marginPx, bounds[3], 3,
                "and at the bottom");

        // And the ink itself is physically sized: the chart frame is
        // a one-point stroke, which at 300 dpi is about 4.2 px and
        // under the old pixel-sized render was one. Read down the
        // frame's whole length and take the commonest run, so a band
        // of chart ink crossing a single row cannot set the answer.
        java.util.Map<Integer, Integer> runs = new java.util.HashMap<>();
        for (int y = bounds[1] + 20; y < bounds[3] - 20; y += 7) {
            int run = 0;
            for (int x = bounds[0]; x < bounds[0] + 40; x++) {
                if (isInk(sheet, x, y)) {
                    run++;
                } else if (run > 0) {
                    break;
                }
            }
            if (run > 0) {
                runs.merge(run, 1, Integer::sum);
            }
        }
        int frameThickness = runs.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .orElseThrow().getKey();
        assertEquals(1.0 * scale, frameThickness, 1.5,
                "a one-point frame is about " + String.format(
                        java.util.Locale.ROOT, "%.1f", scale)
                        + " px at 300 dpi, not one pixel: measured "
                        + frameThickness + " from " + runs);
    }

    /**
     * A PNG's declared physical resolution: pixels per unit across,
     * pixels per unit down, and the unit specifier (1 is the metre).
     * Absent the chunk, the file is pixels and nothing else.
     */
    private static int[] pngPhysicalResolution(byte[] png) {
        for (int i = 8; i + 12 < png.length; ) {
            int length = ((png[i] & 0xff) << 24) | ((png[i + 1] & 0xff) << 16)
                    | ((png[i + 2] & 0xff) << 8) | (png[i + 3] & 0xff);
            String type = new String(png, i + 4, 4,
                    java.nio.charset.StandardCharsets.US_ASCII);
            if (type.equals("pHYs")) {
                int at = i + 8;
                return new int[] {readInt(png, at), readInt(png, at + 4),
                        png[at + 8] & 0xff};
            }
            i += 12 + length;
        }
        throw new AssertionError("the sheet carries no pHYs chunk, so it"
                + " states no physical size at all");
    }

    private static int readInt(byte[] bytes, int at) {
        return ((bytes[at] & 0xff) << 24) | ((bytes[at + 1] & 0xff) << 16)
                | ((bytes[at + 2] & 0xff) << 8) | (bytes[at + 3] & 0xff);
    }

    /** The bounding box of every non-white pixel: minX minY maxX maxY. */
    private static int[] inkBoundsOf(java.awt.image.BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = 0;
        int maxY = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isInk(image, x, y)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return new int[] {minX, minY, maxX + 1, maxY + 1};
    }

    private static boolean isInk(java.awt.image.BufferedImage image,
                                 int x, int y) {
        int rgb = image.getRGB(x, y) & 0xffffff;
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return r + g + b < 3 * 250;
    }

    /** The bounding box of every coordinate in an SVG path. */
    private static double[] boundsOf(String d) {
        java.util.List<Double> numbers = new java.util.ArrayList<>();
        java.util.regex.Matcher each = java.util.regex.Pattern.compile(
                "-?\\d+(?:\\.\\d+)?").matcher(d);
        while (each.find()) {
            numbers.add(Double.parseDouble(each.group()));
        }
        return boundsOfNumbers(numbers);
    }

    /** The bounding box of a flat list of x y x y coordinates. */
    private static double[] boundsOfNumbers(java.util.List<Double> numbers) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i + 1 < numbers.size(); i += 2) {
            minX = Math.min(minX, numbers.get(i));
            maxX = Math.max(maxX, numbers.get(i));
            minY = Math.min(minY, numbers.get(i + 1));
            maxY = Math.max(maxY, numbers.get(i + 1));
        }
        return new double[] {minX, minY, maxX, maxY};
    }

    /**
     * Where an SVG's ink actually reaches on paper: every drawn
     * path's own bounds cut down by the clip it carries, which is
     * what a reader sees rather than what the recorder captured.
     */
    private static double[] inkBounds(String svg) {
        java.util.Map<String, double[]> clips = new java.util.HashMap<>();
        java.util.regex.Matcher defined = java.util.regex.Pattern.compile(
                        "<clipPath id=\"(clip\\d+)\"><path d=\"([^\"]+)\"")
                .matcher(svg);
        while (defined.find()) {
            clips.put(defined.group(1), boundsOf(defined.group(2)));
        }

        double[] ink = {Double.MAX_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE, -Double.MAX_VALUE};
        java.util.regex.Matcher paths = java.util.regex.Pattern.compile(
                "<path d=\"([^\"]+)\"([^/]*)/>").matcher(svg);
        while (paths.find()) {
            if (svg.lastIndexOf("<clipPath", paths.start())
                    > svg.lastIndexOf("</clipPath>", paths.start())) {
                continue;  // a clip definition, not ink
            }
            double[] box = boundsOf(paths.group(1));
            java.util.regex.Matcher carried = java.util.regex.Pattern
                    .compile("url\\(#(clip\\d+)\\)")
                    .matcher(paths.group(2));
            if (carried.find()) {
                double[] clip = clips.get(carried.group(1));
                box = new double[] {Math.max(box[0], clip[0]),
                        Math.max(box[1], clip[1]),
                        Math.min(box[2], clip[2]),
                        Math.min(box[3], clip[3])};
            }
            ink[0] = Math.min(ink[0], box[0]);
            ink[1] = Math.min(ink[1], box[1]);
            ink[2] = Math.max(ink[2], box[2]);
            ink[3] = Math.max(ink[3], box[3]);
        }
        return ink;
    }

    // ---- the same readings the study takes ---------------------------

    private static double sagitta(double fieldDegrees) {
        return departure(fieldDegrees, true);
    }

    private static double stereographicSagitta(double fieldDegrees) {
        return departure(fieldDegrees, false);
    }

    private static double departure(double fieldDegrees,
                                    boolean gnomonic) {
        SkyPosition centre = new SkyPosition(0.0, 12.0);
        GreatCircle circle = new GreatCircle(new SkyPosition(0.0, 90.0));
        List<double[]> onPage = new java.util.ArrayList<>();
        for (SkyPosition each : circle.around(2048)) {
            double[] pixel = pixels(centre, each, gnomonic, fieldDegrees);
            if (pixel == null) {
                continue;
            }
            if (Math.abs(pixel[0]) <= 450 && Math.abs(pixel[1]) <= 350) {
                onPage.add(pixel);
            }
        }
        assertTrue(onPage.size() > 100,
                "the circle really crosses this page: " + onPage.size()
                        + " points");
        double[] from = onPage.get(0);
        double[] to = onPage.get(onPage.size() - 1);
        double length = Math.hypot(to[0] - from[0], to[1] - from[1]);
        double worst = 0.0;
        for (double[] point : onPage) {
            worst = Math.max(worst, Math.abs(
                    (to[0] - from[0]) * (from[1] - point[1])
                            - (from[0] - point[0]) * (to[1] - from[1]))
                    / length);
        }
        return worst;
    }

    private static double[] pixels(SkyPosition centre, SkyPosition at,
                                   boolean gnomonic, double field) {
        if (gnomonic) {
            return new GnomonicProjection(centre).project(at)
                    .map(plane -> {
                        double scale = 900.0 / (2.0 * Math.tan(
                                Math.toRadians(field) / 2.0));
                        return new double[] {plane.xiEast() * scale,
                                plane.etaNorth() * scale};
                    }).orElse(null);
        }
        return new StereographicCandidate(centre).project(at)
                .map(plane -> {
                    double scale = StereographicCandidate
                            .pixelsPerPlaneUnit(900, field);
                    return new double[] {plane.xiEast() * scale,
                            plane.etaNorth() * scale};
                }).orElse(null);
    }

    @Test
    void theEclipticStillClipsExactlyAtTheDecidedField() {
        // The wider field must not disturb the ink the last sprint
        // built: at 42 degrees the ecliptic is still a straight line
        // the chart can clip without sampling.
        GreatCircle ecliptic = Ecliptic.circle();
        assertEquals(90.0, ecliptic.pole().separationDegrees(
                        Ecliptic.landmark("march-equinox").orElseThrow()
                                .at()), 1.0e-9,
                "the ecliptic is unchanged by any of this");
        assertEquals(0.0, sagitta(42.0), 1.0e-6,
                "and stays straight at the decided field");
    }
}
