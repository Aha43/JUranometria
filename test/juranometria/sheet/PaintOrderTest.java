package juranometria.sheet;

import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The order the renderer drew in, kept (issue #287, found on paper).
 *
 * <p>The chart draws a constellation name, then fills the title
 * panel over whatever is beneath it, then writes the title on top.
 * Every writer used to emit all the shapes and then all the text,
 * which is a different picture: it put the constellation name back
 * above the panel, and <strong>CANIS MAJOR read straight through the
 * title box</strong> of every exported Orion sheet.
 *
 * <p>No automated check caught it. It was found by opening an
 * exported sheet and looking at it - the SVG directly, and the PDF
 * through an independent renderer - which nothing in this repository
 * had done. The printed inspection this issue owes is still owed.
 *
 * <p>So this holds the one overlap it was found in, in all three
 * formats: the part of the name under the panel is hidden, the part
 * outside it is not, and the title's own words stay above the fill.
 */
class PaintOrderTest {

    /** Orion at the sheet field, where the name meets the panel. */
    private static final ChartViewState ORION = new ChartViewState(
            new SkyPosition(83.0, 0.0), 42.0, 6.0);

    private static final String NAME = "CANIS MAJOR";

    private static SheetRecording sheet() {
        return ChartSheet.record(Atlas.assembler()::assemble, ORION,
                ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
    }

    /** The opaque panel the title block lays down. */
    private static Rectangle2D panel(SheetRecording sheet) {
        Rectangle2D found = null;
        for (SheetRecorder.Operation operation
                : sheet.recorder().operations()) {
            if (operation instanceof SheetRecorder.Drawn drawn
                    && drawn.filled()) {
                Rectangle2D box = drawn.shape().getBounds2D();
                if (box.getWidth() > 200 && box.getHeight() > 30
                        && box.getHeight() < 80) {
                    found = box;
                }
            }
        }
        assertTrue(found != null,
                "the sheet lays down a title panel to cover things");
        return found;
    }

    private static SheetRecorder.Text label(SheetRecording sheet,
                                            String text) {
        return sheet.recorder().text().stream()
                .filter(each -> each.text().equals(text))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "this page carries the label " + text));
    }

    @Test
    void theRecordingItselfKeepsTheOrderTheRendererDrewIn() {
        SheetRecording sheet = sheet();
        int name = -1;
        int fill = -1;
        int title = -1;
        for (int at = 0; at < sheet.recorder().operations().size(); at++) {
            SheetRecorder.Operation operation =
                    sheet.recorder().operations().get(at);
            if (operation instanceof SheetRecorder.Text text) {
                if (text.text().equals(NAME)) {
                    name = at;
                } else if (text.text().startsWith("Centre ")) {
                    title = at;
                }
            } else if (operation instanceof SheetRecorder.Drawn drawn
                    && drawn.filled()
                    && drawn.shape().getBounds2D().getWidth() > 200
                    && drawn.shape().getBounds2D().getHeight() < 80) {
                fill = at;
            }
        }
        assertTrue(name >= 0 && fill >= 0 && title >= 0,
                "the page has the name, the panel and the title on it:"
                        + " " + name + ", " + fill + ", " + title);
        assertTrue(name < fill,
                "the constellation name is drawn before the panel that"
                        + " covers it: " + name + " then " + fill);
        assertTrue(fill < title,
                "and the title is written after the panel it sits on: "
                        + fill + " then " + title);
    }

    @Test
    void theSvgWritesThemInThatOrder() {
        SheetRecording sheet = sheet();
        String svg = SvgSheetWriter.write(sheet,
                SvgSheetWriter.Text.EDITABLE);
        String body = svg.substring(svg.indexOf("<g id=\"chart\""));

        int name = body.indexOf(">" + NAME + "</text>");
        int fill = body.indexOf(pathOf(panel(sheet)));
        int title = body.indexOf(">Centre ");
        assertTrue(name >= 0, "the SVG carries the name");
        assertTrue(fill >= 0, "and the panel");
        assertTrue(title >= 0, "and the title");
        assertTrue(name < fill,
                "with the name before the panel, so a viewer paints"
                        + " the panel over it: " + name + " then "
                        + fill);
        assertTrue(fill < title,
                "and the title after it: " + fill + " then " + title);
    }

    @Test
    void thePdfWritesThemInThatOrder() throws Exception {
        SheetRecording sheet = sheet();
        String pdf = new String(PdfSheetWriter.write(sheet),
                StandardCharsets.ISO_8859_1);
        String stream = pdf.substring(pdf.indexOf("stream\n") + 7,
                pdf.indexOf("endstream"));

        // The PDF draws its text as outlines, so the name is found by
        // the geometry of its own glyphs rather than by its letters.
        int name = stream.indexOf(pathOfPdf(glyphs(label(sheet, NAME))));
        int fill = stream.indexOf(pathOfPdf(panel(sheet)));
        int title = stream.indexOf(pathOfPdf(glyphs(
                sheet.recorder().text().stream()
                        .filter(each -> each.text().startsWith("Centre "))
                        .findFirst().orElseThrow())));
        assertTrue(name >= 0 && fill >= 0 && title >= 0,
                "the PDF carries the name, the panel and the title: "
                        + name + ", " + fill + ", " + title);
        assertTrue(name < fill,
                "with the name before the panel: " + name + " then "
                        + fill);
        assertTrue(fill < title,
                "and the title after it: " + fill + " then " + title);
    }

    @Test
    void thePngShowsThePanelOverTheNameAndTheNameBesideIt()
            throws Exception {
        SheetRecording sheet = sheet();
        BufferedImage png = ImageIO.read(new ByteArrayInputStream(
                PngSheetWriter.write(sheet, 300)));

        SheetRecorder.Text name = label(sheet, NAME);
        Shape letters = glyphs(name);

        // The name's own ink where the panel covers it, with every
        // other label's ink taken out - the title block's lines run
        // through the same place and are legitimately dark.
        Area hidden = new Area(letters);
        hidden.intersect(new Area(panel(sheet)));
        for (SheetRecorder.Text other : sheet.recorder().text()) {
            if (other == name) {
                continue;
            }
            Shape near = glyphs(other);
            hidden.subtract(new Area(new java.awt.BasicStroke(6.0f)
                    .createStrokedShape(near)));
            hidden.subtract(new Area(near));
        }

        int[] under = inkIn(png, hidden);
        assertTrue(under[0] > 200,
                "the name genuinely runs under the panel, so there is"
                        + " something to hide: " + under[0]
                        + " samples");
        assertTrue(under[1] * 20 < under[0],
                "and the panel hides it: " + under[1] + " of "
                        + under[0] + " samples are dark, where a"
                        + " text-last replay leaves about a quarter"
                        + " of them dark");

        // And the part of the name outside the panel is still there:
        // hiding what the panel covers must not delete the word.
        // Only the part on the page counts - this name begins left
        // of the chart and production clips what runs off it, so a
        // region that included the clipped half would be asking for
        // ink that was never owed.
        Area beside = new Area(letters);
        beside.subtract(new Area(panel(sheet)));
        beside.intersect(new Area(new Rectangle2D.Double(1, 1,
                PaperSize.A4.chartWideUnits() - 2,
                PaperSize.A4.chartHighUnits() - 2)));
        int[] outside = inkIn(png, beside);
        assertTrue(outside[0] > 200,
                "part of the name lies outside the panel: "
                        + outside[0] + " samples");
        assertTrue(outside[1] * 4 > outside[0],
                "and it is drawn there: " + outside[1] + " of "
                        + outside[0] + " samples are dark");
    }

    /** Samples inside a shape, and how many of them carry ink. */
    private static int[] inkIn(BufferedImage png, Area area) {
        double scale = 300 / 72.0;
        double margin = PaperSize.A4.marginPoints();
        Rectangle2D box = area.getBounds2D();
        int samples = 0;
        int dark = 0;
        for (double y = box.getMinY(); y <= box.getMaxY(); y += 0.25) {
            for (double x = box.getMinX(); x <= box.getMaxX(); x += 0.25) {
                if (!area.contains(x, y)) {
                    continue;
                }
                int px = (int) Math.round((x + margin) * scale);
                int py = (int) Math.round((y + margin) * scale);
                if (px < 0 || py < 0 || px >= png.getWidth()
                        || py >= png.getHeight()) {
                    continue;
                }
                samples++;
                int rgb = png.getRGB(px, py) & 0xffffff;
                if (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff)
                        + (rgb & 0xff) < 700) {
                    dark++;
                }
            }
        }
        return new int[] {samples, dark};
    }

    private static Shape glyphs(SheetRecorder.Text text) {
        return text.font().createGlyphVector(
                        new FontRenderContext(null, true, true),
                        text.text())
                .getOutline((float) text.x(), (float) text.y());
    }

    private static String pathOf(Shape shape) {
        return SvgSheetWriter.path(shape);
    }

    private static String pathOfPdf(Shape shape) {
        return PdfSheetWriter.path(shape);
    }
}
