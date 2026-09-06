package juranometria.app;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.GnomonicProjection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.chart.StarSizePolicy;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sky.Observer;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReferenceInk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A reader exporting, end to end (Sprint 29, issue #286).
 *
 * <p>The route a club member takes: the File menu, the dialog, one
 * choice each of the three formats, and three files that are what
 * they claim. Each is opened again afterwards by something that did
 * not write it - an XML parser, a PDF content-stream reader, an image
 * decoder - and asked what it holds.
 *
 * <p>The chart used is the sprint's own case: the new 42-degree
 * field, with the meridian and the ecliptic showing, because a sheet
 * that only works on the released default is a sheet that has not
 * been tried.
 */
class ExportJourneyTest {

    private static final ChartViewState EQUINOX = new ChartViewState(
            new SkyPosition(0.0, 0.0), 42.0, 6.0);

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

    @SuppressWarnings("unchecked")
    private static <T extends JComponent> T named(JComponent root,
                                                  String name) {
        if (name.equals(root.getName())) {
            return (T) root;
        }
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JComponent component) {
                T found = named(component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void theReaderExportsAllThreeFormatsFromTheFileMenu(
            @TempDir Path folder) throws Exception {
        // ---- 1. the route exists where a reader looks for it -------
        List<String> asked = new ArrayList<>();
        ChartViewController navigation = new ChartViewController();
        navigation.recenter(EQUINOX.centre(),
                EQUINOX.fieldWidthDegrees());
        JMenuBar bar = AppMenuBar.create(navigation, () -> { }, () -> { },
                () -> { }, () -> { }, () -> { }, () -> { },
                () -> asked.add("export"));

        JMenuItem export = AppMenuBar.exportItem(bar);
        assertTrue(export != null, "1. File carries an export item");
        assertEquals("File", bar.getMenu(0).getText(),
                "in the File menu, where every application keeps"
                        + " 'make me a file'");
        assertEquals("Export Chart Sheet...", export.getText(),
                "named for what it does");
        assertTrue(export.getAccelerator() != null,
                "with the platform's own shortcut");
        export.doClick();
        assertEquals(List.of("export"), asked,
                "and the item runs the export, not something else");

        // ---- 2. the dialog, once, for three formats ----------------
        List<ExportSheet.Request> chosen = new ArrayList<>();
        for (SheetFormat format : SheetFormat.values()) {
            JComponent dialog = ExportSheetDialog.content(
                    ExportSheetSession.defaults(), chosen::add, () -> { });
            JComboBox<SheetFormat> box =
                    named(dialog, ExportSheetDialog.FORMAT_BOX);
            box.setSelectedItem(format);
            ((JButton) named(dialog, ExportSheetDialog.EXPORT_BUTTON))
                    .doClick();
        }
        assertEquals(List.of(SheetFormat.SVG, SheetFormat.PDF,
                        SheetFormat.PNG),
                chosen.stream().map(ExportSheet.Request::format).toList(),
                "2. the reader chooses each format through the real"
                        + " dialog");

        // ---- 3. three files, written -------------------------------
        List<Path> written = new ArrayList<>();
        for (ExportSheet.Request request : chosen) {
            File destination = folder.resolve("equinox").toFile();
            var outcome = assertInstanceOf(
                    ExportSheet.Outcome.Written.class,
                    ExportSheet.write(Atlas.assembler()::assemble, EQUINOX,
                            ChartOptions.DEFAULTS, modules(), request,
                            destination),
                    "3. " + request.format() + " is written");
            written.add(outcome.file());
        }
        assertEquals(3, written.stream().distinct().count(),
                "three formats are three files, not one overwritten");

        // ---- 4. each opened by something that did not write it -----
        String svg = Files.readString(written.get(0), StandardCharsets.UTF_8);
        var document = javax.xml.parsers.DocumentBuilderFactory
                .newInstance().newDocumentBuilder()
                .parse(written.get(0).toFile());
        assertEquals("svg", document.getDocumentElement().getNodeName(),
                "4. an XML parser reads the SVG as an SVG");
        assertTrue(svg.contains("width=\"841.89pt\""),
                "at A4's own size");

        String pdf = Files.readString(written.get(1),
                StandardCharsets.ISO_8859_1);
        assertTrue(pdf.startsWith("%PDF-1.4"), "the PDF is a PDF");
        assertTrue(pdf.contains("/MediaBox [0 0 841.89 595.28]"),
                "stating A4 as its page");
        assertFalse(pdf.contains("/Image") || pdf.contains("/DCTDecode"),
                "with no image in it at all");
        String stream = pdf.substring(pdf.indexOf("stream\n") + 7,
                pdf.indexOf("endstream"));
        assertTrue(count(stream, " m\n") > 500,
                "and real path geometry: " + count(stream, " m\n")
                        + " subpaths");

        BufferedImage png = ImageIO.read(written.get(2).toFile());
        assertEquals(PngSheetWriter.widePixels(PaperSize.A4, 300),
                png.getWidth(), "the PNG is the whole sheet at 300 dpi");
        assertEquals(PngSheetWriter.highPixels(PaperSize.A4, 300),
                png.getHeight(), "in both directions");

        // ---- 5. and they are the same chart ------------------------
        sameChart(written.get(0), png);
    }

    /**
     * The formats against each other, through the sky: the marks
     * production drew for this sheet are in the SVG at their own
     * place, and the PNG has ink at the same places scaled by its own
     * resolution.
     */
    private void sameChart(Path svgFile, BufferedImage png)
            throws Exception {
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble, EQUINOX, ChartOptions.DEFAULTS,
                modules(), PaperSize.A4);
        String svg = Files.readString(svgFile, StandardCharsets.UTF_8);
        List<double[]> inSvg = pathCentres(svg);

        GnomonicProjection projection = new GnomonicProjection(
                sheet.scene().viewport().centre());
        ViewportMapping mapping = new ViewportMapping(
                sheet.scene().viewport());
        double scale = 300 / 72.0;
        double margin = PaperSize.A4.marginPoints();

        int checked = 0;
        List<String> missingFromSvg = new ArrayList<>();
        List<String> blankInPng = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark
                : new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(sheet.scene(), sheet.options())) {
            var at = projection.project(mark.star() != null
                            ? mark.star().position()
                            : mark.deepSky().position())
                    .map(mapping::toPixel);
            if (at.isEmpty() || at.get().x() < 20 || at.get().y() < 20
                    || at.get().x() > PaperSize.A4.chartWideUnits() - 20
                    || at.get().y() > PaperSize.A4.chartHighUnits() - 20) {
                continue;
            }
            checked++;
            double x = at.get().x();
            double y = at.get().y();
            if (inSvg.stream().noneMatch(centre ->
                    Math.hypot(centre[0] - x, centre[1] - y) < 1.0)) {
                missingFromSvg.add("mark at " + x + "," + y);
            }
            // The same place on the raster, which is the sheet
            // coordinate plus the margin, times the resolution.
            int px = (int) Math.round((x + margin) * scale);
            int py = (int) Math.round((y + margin) * scale);
            // Unless production itself painted over it. The title
            // block is an opaque panel in the lower left, and a mark
            // under it is hidden on any raster - in the vector
            // formats the path is still there, beneath the panel,
            // which is the same layering a viewer will apply. So the
            // recording is asked whether anything filled over this
            // mark after it was drawn.
            if (coveredLater(sheet, x, y)) {
                continue;
            }
            // Within the mark's own extent, not a fixed box: a
            // deep-sky symbol is an open outline and its centre is
            // blank paper, so looking only at the middle of one asks
            // the wrong question.
            int reach = (int) Math.ceil(
                    Math.max(mark.reach(), 1.5) * scale) + 4;
            if (!inkNear(png, px, py, reach)) {
                blankInPng.add("blank at " + px + "," + py);
            }
        }
        assertTrue(checked > 50,
                "5. there are marks to compare across the formats: "
                        + checked);
        assertEquals(List.of(), missingFromSvg.stream().limit(3).toList(),
                missingFromSvg.size() + " of " + checked
                        + " marks are absent from the SVG");
        assertEquals(List.of(), blankInPng.stream().limit(3).toList(),
                blankInPng.size() + " of " + checked + " marks have no"
                        + " ink at the matching place in the PNG");
    }

    /**
     * Whether production drew an opaque fill over this point after
     * the mark itself - the title block, which is a panel and not a
     * transparency.
     */
    private static boolean coveredLater(SheetRecording sheet, double x,
                                        double y) {
        boolean seenMark = false;
        for (juranometria.sheet.SheetRecorder.Drawn drawn
                : sheet.recorder().drawn()) {
            java.awt.geom.Rectangle2D box = drawn.shape().getBounds2D();
            boolean isTheMark = Math.hypot(box.getCenterX() - x,
                    box.getCenterY() - y) < 1.0 && box.getWidth() < 20;
            if (isTheMark) {
                seenMark = true;
                continue;
            }
            if (seenMark && drawn.filled() && box.getWidth() > 20
                    && drawn.shape().contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private static boolean inkNear(BufferedImage image, int x, int y,
                                   int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < 0 || py < 0 || px >= image.getWidth()
                        || py >= image.getHeight()) {
                    continue;
                }
                int rgb = image.getRGB(px, py) & 0xffffff;
                int sum = ((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff)
                        + (rgb & 0xff);
                if (sum < 3 * 250) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<double[]> pathCentres(String svg) {
        List<double[]> centres = new ArrayList<>();
        Matcher each = Pattern.compile("<path d=\"([^\"]+)\"")
                .matcher(svg.substring(svg.indexOf("<g id=\"ink\"")));
        while (each.find()) {
            List<Double> numbers = new ArrayList<>();
            Matcher number = Pattern.compile("-?\\d+(?:\\.\\d+)?")
                    .matcher(each.group(1));
            while (number.find()) {
                numbers.add(Double.parseDouble(number.group()));
            }
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
            centres.add(new double[] {(minX + maxX) / 2.0,
                    (minY + maxY) / 2.0});
        }
        return centres;
    }

    private static int count(String text, String needle) {
        int found = 0;
        for (int at = text.indexOf(needle); at >= 0;
                at = text.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }
}
