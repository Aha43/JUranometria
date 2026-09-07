package juranometria.sheet;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A projected curve, carried to paper (Sprint 30, issue #298).
 *
 * <p>The issue's last geometry clause is that screen, SVG, PDF and
 * PNG carry <strong>the same curve geometry and paint order</strong>.
 * That is a claim about the whole production path, so this is the
 * whole production path: the atlas's own scene assembler, a real view
 * state, {@link ChartSheet}, the production renderer, and the three
 * writers - reading <em>one</em> recording, because a claim that four
 * things agree is not tested by making four of them.
 *
 * <p>The page is one a reader can ask for, and its centre is chosen
 * for two reasons that both matter. A great circle <em>through</em>
 * the page centre is straight under every projection, so a page
 * centred on the ecliptic's own crossing would have proved nothing
 * about a curve; and the permanent circle has to meet the title
 * panel somewhere, because paint order in a raster is only visible
 * where two things overlap.
 *
 * <p>What this cannot show is a curve a reader would <em>see</em>
 * bending. At 42 degrees the arc stands about a twentieth of a page
 * unit off its own chord - which is why the released atlas is byte
 * for byte identical through this issue - so what is asked here is
 * whether the curve is carried, exactly, and unchanged from one
 * format to the next. Whether it is visibly a curve is measured on
 * the wider pages, in {@code ProjectedCurveSeamTest}, where the seam
 * has to serve them.
 */
class ProjectedCurveOnASheetTest {

    /**
     * A page the permanent circle crosses off-centre, and where it
     * runs under the title panel.
     */
    private static final SkyPosition ABOVE_THE_CROSSING =
            new SkyPosition(0.0, 16.0);

    /** The permanent circle's own stroke: long dash, short dot. */
    private static final float[] PERMANENT = {12.0f, 4.0f, 2.0f, 4.0f};

    private static final int DPI = 300;

    /**
     * The meridian and the ecliptic, as the screen carries them,
     * with the permanent circle showing or not.
     *
     * <p>The switch is the reader's own, and it is the whole of the
     * control: both modules are attached either way and the
     * observer's lines are contributed identically, so a page drawn
     * with {@code false} differs from one drawn with {@code true} by
     * the permanent circle and by nothing else. A control that
     * removed <em>every</em> module - which is what this asked for
     * first - could not attribute the extra ink to the circle rather
     * than to the meridian or the horizon running through the same
     * band.
     */
    private static ChartRenderer.ReferenceLayer modules(
            boolean permanentCircleShowing) {
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(new Observer(59.9, 10.7,
                java.time.Instant.parse("2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(permanentCircleShowing);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);
        return (g, painted) -> ReferenceInk.paint(g, painted,
                registry.collect(), ChartPalette.WHITE_PAPER);
    }

    /** One whole production sheet of this page, on A4. */
    private static SheetRecording sheet(ChartProjection kind,
                                        ChartRenderer.ReferenceLayer ink) {
        return ChartSheet.record(Atlas.assembler()::assemble,
                new ChartViewState(ABOVE_THE_CROSSING, 42.0, 6.0, null,
                        null, kind),
                ChartOptions.DEFAULTS, ink, PaperSize.A4);
    }

    private static SheetRecording sheet(ChartProjection kind) {
        return sheet(kind, modules(true));
    }

    /** Where in the render something happened, and what it was. */
    private record At(int index, Shape shape) {
    }

    /**
     * The permanent circle's own ink, found by its own stroke.
     *
     * <p>By the dash pattern rather than by shape or position, so
     * that what is read is the very ink the module's contribution
     * became and not some other curve of the page.
     */
    private static At permanentInk(SheetRecording recorded) {
        for (int at = 0; at < recorded.recorder().operations().size(); at++) {
            if (recorded.recorder().operations().get(at)
                    instanceof SheetRecorder.Drawn drawn
                    && !drawn.filled() && drawn.stroke() != null
                    && Arrays.equals(drawn.stroke().dash(), PERMANENT)) {
                return new At(at, drawn.shape());
            }
        }
        throw new AssertionError("this sheet carries the permanent circle");
    }

    /** The opaque panel the title block lays down over the chart. */
    private static At panel(SheetRecording recorded) {
        At found = null;
        for (int at = 0; at < recorded.recorder().operations().size(); at++) {
            if (recorded.recorder().operations().get(at)
                    instanceof SheetRecorder.Drawn drawn
                    && drawn.filled()) {
                Rectangle2D box = drawn.shape().getBounds2D();
                if (box.getWidth() > 200 && box.getHeight() > 30
                        && box.getHeight() < 80) {
                    found = new At(at, drawn.shape());
                }
            }
        }
        assertTrue(found != null, "the sheet lays down a title panel");
        return found;
    }

    /** How many curved segments a shape is drawn from. */
    private static int curvedSegmentsOf(Shape shape) {
        int curves = 0;
        double[] point = new double[6];
        for (PathIterator each = shape.getPathIterator(null); !each.isDone();
                each.next()) {
            int segment = each.currentSegment(point);
            if (segment == PathIterator.SEG_CUBICTO
                    || segment == PathIterator.SEG_QUADTO) {
                curves++;
            }
        }
        return curves;
    }

    /** Samples inside a region of the page, and how many carry ink. */
    private static int[] inkIn(BufferedImage png, Area area) {
        double scale = DPI / 72.0;
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

    /** The band of paper the circle's own line runs through. */
    private static Area bandOf(Shape circle) {
        return new Area(new java.awt.BasicStroke(2.0f)
                .createStrokedShape(circle));
    }

    @Test
    void theRenderKeepsTheCurveAndTheTangentPlanesLine() {
        // One contribution, two projections, and the difference is
        // the projection rather than the drawing: the same module ink
        // on the same page reaches the sheet as a curve under one and
        // as a straight line under the other.
        Shape curved = permanentInk(sheet(ChartProjection.STEREOGRAPHIC))
                .shape();
        Shape flat = permanentInk(sheet(ChartProjection.GNOMONIC)).shape();
        assertTrue(curvedSegmentsOf(curved) > 0,
                "the sheet's own ink for the circle is curved: "
                        + curvedSegmentsOf(curved) + " curved segments");
        assertEquals(0, curvedSegmentsOf(flat),
                "and the tangent plane's is a line");
    }

    @Test
    void bothVectorFormatsCarryThatVeryCurveFromOneRecording()
            throws Exception {
        // One recording, three writers. The writers do not re-derive
        // geometry - they walk the path of the shape the recorder
        // kept - and the way to hold them to it is to look for that
        // shape's own path text in each file rather than for any
        // curve at all.
        SheetRecording sheet = sheet(ChartProjection.STEREOGRAPHIC);
        Shape circle = permanentInk(sheet).shape();
        assertTrue(curvedSegmentsOf(circle) > 0,
                "the recording kept a curve to carry");

        String svg = SvgSheetWriter.write(sheet,
                SvgSheetWriter.Text.EDITABLE);
        assertTrue(svg.contains(SvgSheetWriter.path(circle)),
                "the SVG draws the circle the recording kept");

        String pdf = new String(PdfSheetWriter.write(sheet),
                StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains(PdfSheetWriter.path(circle)),
                "and so does the PDF");
    }

    @Test
    void thePngShowsTheCurveWhereTheRecordingPutIt() throws Exception {
        // A raster cannot be asked what shape it holds, so it is
        // asked where its ink is: along the band the recorded curve
        // runs through, against the same band of the same page drawn
        // with the permanent circle switched off and everything else
        // - the grid, the stars, the constellation lines, and the
        // observer's own meridian and horizon - drawn exactly as
        // before. The two pages differ by this one curve.
        SheetRecording sheet = sheet(ChartProjection.STEREOGRAPHIC);
        Area band = bandOf(permanentInk(sheet).shape());

        BufferedImage drawn = ImageIO.read(new ByteArrayInputStream(
                PngSheetWriter.write(sheet, DPI)));
        BufferedImage without = ImageIO.read(new ByteArrayInputStream(
                PngSheetWriter.write(sheet(ChartProjection.STEREOGRAPHIC,
                        modules(false)), DPI)));

        int[] withCircle = inkIn(drawn, band);
        int[] withoutCircle = inkIn(without, band);
        assertTrue(withCircle[0] > 2000, "there is a band to look in: "
                + withCircle[0] + " samples");
        assertTrue(withCircle[1] * 4 > withCircle[0],
                "the PNG carries the circle's ink along it: "
                        + withCircle[1] + " of " + withCircle[0]
                        + " samples are dark");
        assertTrue(withCircle[1] > 3 * withoutCircle[1],
                "and that ink is the circle's own, not the page's: "
                        + withCircle[1] + " dark with the module"
                        + " showing against " + withoutCircle[1]
                        + " in the same band without it");
    }

    @Test
    void everyFormatPaintsTheCircleBeforeThePanelThatCoversIt()
            throws Exception {
        // Paint order, in all four. A reference line goes under the
        // title block, so the panel hides the part of the circle it
        // covers - and a writer that emitted all the strokes and then
        // all the fills would put the circle back on top, which is
        // the fault PR #292's review found with a constellation name.
        SheetRecording sheet = sheet(ChartProjection.STEREOGRAPHIC);
        At circle = permanentInk(sheet);
        At panel = panel(sheet);
        assertTrue(circle.index() < panel.index(),
                "the render draws the circle before the panel: "
                        + circle.index() + " then " + panel.index());

        String svg = SvgSheetWriter.write(sheet,
                SvgSheetWriter.Text.EDITABLE);
        assertTrue(svg.indexOf(SvgSheetWriter.path(circle.shape()))
                        < svg.indexOf(SvgSheetWriter.path(panel.shape())),
                "the SVG writes it in that order, so a viewer paints"
                        + " the panel over it");

        String pdf = new String(PdfSheetWriter.write(sheet),
                StandardCharsets.ISO_8859_1);
        assertTrue(pdf.indexOf(PdfSheetWriter.path(circle.shape()))
                        < pdf.indexOf(PdfSheetWriter.path(panel.shape())),
                "and the PDF too");

        // And the raster, which is the only one that shows the
        // outcome rather than the order.
        BufferedImage png = ImageIO.read(new ByteArrayInputStream(
                PngSheetWriter.write(sheet, DPI)));
        Area band = bandOf(circle.shape());
        Area hidden = new Area(band);
        hidden.intersect(new Area(panel.shape()));
        // The title block's own words are written over the panel and
        // run through the same place, and they are legitimately dark.
        for (SheetRecorder.Text label : sheet.recorder().text()) {
            Shape letters = label.font()
                    .createGlyphVector(new java.awt.font.FontRenderContext(
                            null, true, true), label.text())
                    .getOutline((float) label.x(), (float) label.y());
            hidden.subtract(new Area(new java.awt.BasicStroke(6.0f)
                    .createStrokedShape(letters)));
            hidden.subtract(new Area(letters));
        }
        Area showing = new Area(band);
        showing.subtract(new Area(panel.shape()));

        int[] under = inkIn(png, hidden);
        int[] beside = inkIn(png, showing);
        assertTrue(under[0] > 200, "the circle genuinely runs under the"
                + " panel, so there is something to hide: " + under[0]
                + " samples");
        assertTrue(under[1] * 20 < under[0],
                "and the panel hides it: " + under[1] + " of "
                        + under[0] + " samples are dark");
        assertTrue(beside[1] * 4 > beside[0],
                "while the rest of the circle is drawn: " + beside[1]
                        + " of " + beside[0] + " samples are dark");
    }
}
