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

    /** One drawn shape or one label, as the thing it puts on paper. */
    private static String markOf(SheetRecorder.Operation operation) {
        if (operation instanceof SheetRecorder.Drawn drawn) {
            return "shape " + SvgSheetWriter.path(drawn.shape());
        }
        SheetRecorder.Text text = (SheetRecorder.Text) operation;
        return "text " + text.text() + " at " + text.x() + "," + text.y();
    }

    /** A label's letters, as the ink they actually lay down. */
    private static Shape lettersOf(SheetRecorder.Text text) {
        return text.font()
                .createGlyphVector(new java.awt.font.FontRenderContext(
                        null, true, true), text.text())
                .getOutline((float) text.x(), (float) text.y());
    }

    /**
     * Everything the module puts on the page except the circle.
     *
     * <p>Derived rather than guessed: whatever the two renders
     * differ by, with the circle itself set aside, is the rest of
     * what the module contributed - its name here, its landmarks on
     * a page that carries them. Taking that from both sides of the
     * comparison leaves a region the two pages differ in by one
     * curve. Naming the parts instead would make this test true of
     * the module as it stands rather than of the module.
     *
     * <p>The difference is taken both ways. Ink that appeared only
     * when the circle was hidden would be as much a second variable
     * as ink that appeared only when it was shown.
     */
    private static Area everythingElseTheModuleDraws(
            SheetRecording shown, SheetRecording hidden, Shape circle) {
        java.util.Set<String> common = new java.util.HashSet<>();
        for (SheetRecorder.Operation operation
                : hidden.recorder().operations()) {
            common.add(markOf(operation));
        }
        java.util.Set<String> onlyShown = new java.util.HashSet<>();
        for (SheetRecorder.Operation operation
                : shown.recorder().operations()) {
            onlyShown.add(markOf(operation));
        }
        java.util.Set<String> different = new java.util.HashSet<>();
        different.addAll(onlyShown);
        different.removeAll(common);
        java.util.Set<String> onlyHidden = new java.util.HashSet<>(common);
        onlyHidden.removeAll(onlyShown);

        String itself = "shape " + SvgSheetWriter.path(circle);
        assertTrue(different.contains(itself),
                "the control page is the one without this circle, which"
                        + " is the whole of what makes it a control");
        Area others = new Area();
        for (SheetRecording each : java.util.List.of(shown, hidden)) {
            for (SheetRecorder.Operation operation
                    : each.recorder().operations()) {
                String mark = markOf(operation);
                if (mark.equals(itself)
                        || !(different.contains(mark)
                                || onlyHidden.contains(mark))) {
                    continue;
                }
                Shape ink = operation instanceof SheetRecorder.Drawn drawn
                        ? drawn.shape()
                        : lettersOf((SheetRecorder.Text) operation);
                // Generously: a mark's ink is wider than its
                // geometry, and a landmark half excluded would leave
                // the half that mattered.
                others.add(new Area(new java.awt.BasicStroke(8.0f)
                        .createStrokedShape(ink)));
                others.add(new Area(ink));
            }
        }
        return others;
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
        // runs through, against the same band of the same page with
        // the permanent circle switched off. The grid, the stars,
        // the constellation lines and the observer's own meridian
        // and horizon are drawn identically on both.
        //
        // One thing that switch does not leave alone: a module
        // contributes the circle *and* everything else it draws, so
        // whatever that is comes away with it. Rather than name
        // those things, the region is the band with whatever the two
        // renders differ by taken out of it, the circle itself set
        // aside. What is left differs by one curve.
        //
        // On this page that difference is two operations: the circle
        // and the word "Ecliptic", which lies on the band and was
        // real second-variable ink. The module's four landmarks are
        // off this paper - the page is centred sixteen degrees north
        // and the nearest of them falls below its bottom edge - so
        // no landmark is in the measurement. The exclusion is
        // derived rather than written down, so it holds a page that
        // does carry one.
        SheetRecording sheet = sheet(ChartProjection.STEREOGRAPHIC);
        SheetRecording bare = sheet(ChartProjection.STEREOGRAPHIC,
                modules(false));
        Shape circle = permanentInk(sheet).shape();
        Area band = bandOf(circle);
        band.subtract(everythingElseTheModuleDraws(sheet, bare, circle));

        BufferedImage drawn = ImageIO.read(new ByteArrayInputStream(
                PngSheetWriter.write(sheet, DPI)));
        BufferedImage without = ImageIO.read(new ByteArrayInputStream(
                PngSheetWriter.write(bare, DPI)));

        int[] withCircle = inkIn(drawn, band);
        int[] withoutCircle = inkIn(without, band);
        assertTrue(withCircle[0] > 2000, "there is a band to look in: "
                + withCircle[0] + " samples");
        assertTrue(withCircle[1] * 4 > withCircle[0],
                "the PNG carries the circle's ink along it: "
                        + withCircle[1] + " of " + withCircle[0]
                        + " samples are dark");
        assertTrue(withCircle[1] > 5 * withoutCircle[1] / 2,
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
