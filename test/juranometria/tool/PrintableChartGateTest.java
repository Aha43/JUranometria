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

    @Test
    void bothVectorSheetsCarryTheProductionClip() throws IOException {
        // The recorder captured production's clip and a first version
        // of both writers discarded it, so ink production cut at the
        // paper bled into the sheet margin.
        String svg = Files.readString(Path.of(
                "docs/studies/printable-chart/sheet-a4.svg"));
        assertTrue(svg.contains("<clipPath id=\"clip0\""),
                "the SVG defines the clip production had in force");
        assertTrue(svg.split("clip-path=\"url", -1).length - 1 > 1000,
                "and applies it to the ink rather than defining it and"
                        + " forgetting it");

        String pdf = Files.readString(Path.of(
                        "docs/studies/printable-chart/sheet-a4.pdf"),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(pdf.split("W n", -1).length - 1 > 1000,
                "and the PDF clips too, with the same operations"
                        + " bracketed by q/Q");
        assertTrue(pdf.contains("q\n"), "pushing the graphics state");
        assertTrue(pdf.contains("Q\n"), "and popping it");
    }

    @Test
    void thePngIsTheWholeSheetAtItsStatedResolution() throws IOException {
        // A first version re-rendered the chart into a bigger pixel
        // grid, which shrank every label relative to the paper, held
        // only the chart rectangle, and stated its resolution
        // nowhere.
        javax.imageio.stream.ImageInputStream in =
                javax.imageio.ImageIO.createImageInputStream(
                        Path.of("docs/studies/printable-chart",
                                "sheet-a4-300dpi.png").toFile());
        javax.imageio.ImageReader reader =
                javax.imageio.ImageIO.getImageReaders(in).next();
        try {
            reader.setInput(in);
            // A4 at 300 dpi, the WHOLE sheet.
            assertEquals(3508, reader.getWidth(0),
                    "the PNG is A4 wide at 300 dpi, margins included");
            assertEquals(2480, reader.getHeight(0),
                    "and A4 high");

            javax.imageio.metadata.IIOMetadata metadata =
                    reader.getImageMetadata(0);
            org.w3c.dom.Node root = metadata.getAsTree(
                    "javax_imageio_png_1.0");
            assertTrue(physicalResolution(root),
                    "and states its own resolution, so a reader's"
                            + " software can place it on paper");
        } finally {
            reader.dispose();
            in.close();
        }
    }

    /** Whether a PNG metadata tree carries a metre-based pHYs. */
    private static boolean physicalResolution(org.w3c.dom.Node node) {
        if ("pHYs".equals(node.getNodeName())) {
            org.w3c.dom.NamedNodeMap attributes = node.getAttributes();
            org.w3c.dom.Node unit =
                    attributes.getNamedItem("unitSpecifier");
            org.w3c.dom.Node perAxis =
                    attributes.getNamedItem("pixelsPerUnitXAxis");
            // 300 dpi is 11811 pixels per metre.
            return unit != null && "meter".equals(unit.getNodeValue())
                    && perAxis != null
                    && Math.abs(Long.parseLong(perAxis.getNodeValue())
                            - 11811L) <= 1;
        }
        for (org.w3c.dom.Node child = node.getFirstChild();
                child != null; child = child.getNextSibling()) {
            if (physicalResolution(child)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void theLetterSheetIsItsOwnChartAndNotAnA4OneCutDown()
            throws IOException {
        // Reusing A4's recording would run the chart past Letter's
        // right margin and let the viewport cut it.
        String letter = Files.readString(Path.of(
                "docs/studies/printable-chart/sheet-letter.svg"));
        String a4 = Files.readString(Path.of(
                "docs/studies/printable-chart/sheet-a4.svg"));

        assertTrue(letter.contains("width=\"792.00pt\""),
                "the Letter sheet is Letter wide");
        assertTrue(a4.contains("width=\"841.89pt\""),
                "and the A4 sheet is A4 wide");
        assertNotEquals(a4.length(), letter.length(),
                "and they are different renders, not one file wearing"
                        + " two page sizes");

        String formats = Files.readString(Path.of(
                "docs/studies/printable-chart/formats.md"));
        assertTrue(formats.contains("US Letter"),
                "and the report says both were recorded at their own"
                        + " chart rectangle");
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
