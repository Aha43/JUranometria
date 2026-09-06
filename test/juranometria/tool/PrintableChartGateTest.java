package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.sheet.SheetRecorder;
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
    void theApplicationGainedTheFieldTheGateChoseAndNoOther() {
        // While the gate was open this said the wider field was not
        // reachable at all. #284 made the chosen one reachable, which
        // is the only way this assertion should ever have changed -
        // so it now holds the same line from the other side.
        ChartViewState sheet = new ChartViewState(
                new SkyPosition(83.0, 0.0), 42.0, 6.0);
        assertEquals(42.0, sheet.fieldWidthDegrees(),
                "42 degrees is the step the gate chose, and the atlas"
                        + " has it");

        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewState(new SkyPosition(83.0, 0.0),
                        48.0, 6.0),
                "and 48 - the tempting one, at 14.8% anisotropy - is"
                        + " still not a page this atlas will draw");
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewState(new SkyPosition(83.0, 0.0),
                        45.0, 6.0),
                "nor 45, at 12.9%, which is what the 12% budget"
                        + " excludes");
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
        SheetRecorder recorder = new SheetRecorder(400, 300);
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
        String decision = Files.readString(Path.of(
                "docs/decisions/printable-chart.md"));

        for (String quoted : List.of("+11.2%", "+8.1%", "+14.8%")) {
            assertTrue(measurements.contains(quoted),
                    "the study measured " + quoted);
            assertTrue(decision.contains(quoted),
                    "and the decision quotes it: " + quoted);
        }
        assertFalse(decision.contains("3208"),
                "the decision does not specify the rejected PNG");
        // The gate's own prototype sheets were retired in #286, when
        // production gained writers of its own. What the decision
        // still owns is the projection and the budget it was chosen
        // on; the formats are held against production's sheets in
        // juranometria.sheet.SheetFormatsTest.
        assertTrue(decision.contains("2268 shapes"),
                "the decision keeps what the gate measured, as the"
                        + " record of what it decided on");
        assertTrue(measurements.contains("Fanafjellet"),
                "and the reader who asked is in the study, not only"
                        + " in the decision");
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
