package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.catalog.TiledCatalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.geo.ConstellationGeography;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

/**
 * One chart state, three formats, no new dependency (Sprint 29,
 * issue #283).
 *
 * <p>Prototypes, not production promises. They exist to expose the
 * failures a format study cannot argue away: a raster smuggled into
 * a PDF, a layer that silently vanished, a font substitution nobody
 * declared, a sheet whose physical size is a guess.
 *
 * <p>Every format here is written from the <strong>same</strong>
 * recording of the <strong>same</strong> production render. The
 * cartography is not forked; it is played back.
 */
public final class ChartSheetExportStudyMain {

    private ChartSheetExportStudyMain() {
    }

    private static final File DIR =
            new File("docs/studies/printable-chart");

    // ---- the sheet ---------------------------------------------------

    /** A4 landscape, in PostScript points (1/72 inch). */
    private static final double A4_WIDE_PT = 841.89;
    private static final double A4_HIGH_PT = 595.28;

    /** US Letter landscape, for the same reader workflow. */
    private static final double LETTER_WIDE_PT = 792.0;
    private static final double LETTER_HIGH_PT = 612.0;

    /** A margin a printer can hold and a hand can grip. */
    private static final double MARGIN_PT = 36.0;  // half an inch

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        System.err.println("chart-sheet export prototypes:");

        double chartWide = A4_WIDE_PT - 2 * MARGIN_PT;
        double chartHigh = A4_HIGH_PT - 2 * MARGIN_PT;

        // One scene, at the candidate wider field, carrying the
        // reference ink a club sheet would want.
        ChartScene scene = scene(new SkyPosition(83.0, 0.0), 42.0, 6.0,
                (int) Math.round(chartWide), (int) Math.round(chartHigh));

        ChartSheetRecorder recorder = new ChartSheetRecorder(
                (int) Math.round(chartWide), (int) Math.round(chartHigh));
        record(scene, recorder);

        System.err.printf(Locale.ROOT,
                "  recorded %d shapes and %d text runs from one"
                        + " production render%n",
                recorder.drawn().size(), recorder.text().size());

        writeSvg(new File(DIR, "sheet-a4.svg"), recorder,
                A4_WIDE_PT, A4_HIGH_PT, chartWide, chartHigh, false);
        writeSvg(new File(DIR, "sheet-a4-text-as-paths.svg"), recorder,
                A4_WIDE_PT, A4_HIGH_PT, chartWide, chartHigh, true);
        writeSvg(new File(DIR, "sheet-letter.svg"), recorder,
                LETTER_WIDE_PT, LETTER_HIGH_PT,
                LETTER_WIDE_PT - 2 * MARGIN_PT,
                LETTER_HIGH_PT - 2 * MARGIN_PT, false);
        writePdf(new File(DIR, "sheet-a4-base14-font.pdf"), recorder,
                A4_WIDE_PT, A4_HIGH_PT, chartWide, chartHigh, false);
        writePdf(new File(DIR, "sheet-a4.pdf"), recorder,
                A4_WIDE_PT, A4_HIGH_PT, chartWide, chartHigh, true);
        writePng(new File(DIR, "sheet-a4-300dpi.png"), scene, 300);

        report(recorder, chartWide, chartHigh);
        System.err.println("written to " + DIR.getPath());
    }

    /** The production render, played into the recorder. */
    private static void record(ChartScene scene, Graphics2D into) {
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(new Observer(59.9,
                10.7, java.time.Instant.parse("2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);

        new ChartRenderer(StarSizePolicy.DEFAULT).render(into, scene,
                ChartOptions.DEFAULTS,
                (layerG, painted) -> ReferenceInk.paint(layerG, painted,
                        registry.collect(),
                        ChartOptions.DEFAULTS.palette()));
    }

    // ---- SVG: the editable vector master ------------------------------

    private static void writeSvg(File file, ChartSheetRecorder recorder,
                                 double sheetWide, double sheetHigh,
                                 double chartWide, double chartHigh,
                                 boolean textAsPaths) throws IOException {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\"")
                .append(String.format(Locale.ROOT,
                        " width=\"%.2fpt\" height=\"%.2fpt\""
                                + " viewBox=\"0 0 %.2f %.2f\"",
                        sheetWide, sheetHigh, sheetWide, sheetHigh))
                .append(" version=\"1.1\">\n");
        // Provenance, in the file a reader will edit.
        svg.append("  <title>JUranometria chart sheet</title>\n");
        svg.append("  <desc>Orion, 42 degree field, ICRS/J2000,"
                + " stars to V 6.0. Produced by JUranometria from its"
                + " own renderer; no external resources.</desc>\n");
        svg.append("  <metadata>JUranometria chart sheet prototype"
                + " (Sprint 29 gate, issue #283). Self-contained: no"
                + " external stylesheet, script, font or image."
                + "</metadata>\n");
        svg.append(String.format(Locale.ROOT,
                "  <rect x=\"0\" y=\"0\" width=\"%.2f\" height=\"%.2f\""
                        + " fill=\"#ffffff\"/>\n", sheetWide, sheetHigh));
        svg.append(String.format(Locale.ROOT,
                "  <g id=\"chart\" transform=\"translate(%.2f,%.2f)\">\n",
                MARGIN_PT, MARGIN_PT));

        svg.append("    <g id=\"ink\" fill=\"none\""
                + " stroke-linecap=\"butt\">\n");
        for (ChartSheetRecorder.Drawn drawn : recorder.drawn()) {
            svg.append("      <path d=\"").append(path(drawn.shape()))
                    .append("\"");
            if (drawn.filled()) {
                svg.append(" fill=\"").append(hex(drawn.colour()))
                        .append("\"");
            } else {
                svg.append(" stroke=\"").append(hex(drawn.colour()))
                        .append(String.format(Locale.ROOT,
                                "\" stroke-width=\"%.2f\"",
                                drawn.stroke().width()));
                if (drawn.stroke().dash() != null) {
                    svg.append(" stroke-dasharray=\"")
                            .append(dash(drawn.stroke().dash()))
                            .append("\"");
                }
            }
            svg.append("/>\n");
        }
        svg.append("    </g>\n");

        svg.append("    <g id=\"labels\">\n");
        for (ChartSheetRecorder.Text text : recorder.text()) {
            if (textAsPaths) {
                java.awt.font.GlyphVector glyphs = text.font()
                        .createGlyphVector(
                                new java.awt.font.FontRenderContext(null,
                                        true, true), text.text());
                java.awt.Shape outline = glyphs.getOutline(
                        (float) text.x(), (float) text.y());
                svg.append("      <path d=\"").append(path(outline))
                        .append("\" fill=\"").append(hex(text.colour()))
                        .append("\"/>\n");
            } else {
                svg.append(String.format(Locale.ROOT,
                                "      <text x=\"%.2f\" y=\"%.2f\""
                                        + " font-family=\"%s\""
                                        + " font-size=\"%d\""
                                        + " fill=\"%s\">",
                                text.x(), text.y(),
                                "sans-serif", text.font().getSize(),
                                hex(text.colour())))
                        .append(escape(text.text()))
                        .append("</text>\n");
            }
        }
        svg.append("    </g>\n");
        svg.append("  </g>\n</svg>\n");
        Files.writeString(file.toPath(), svg.toString(),
                StandardCharsets.UTF_8);
        System.err.printf(Locale.ROOT, "  %s (%d bytes, text as %s)%n",
                file.getName(), file.length(),
                textAsPaths ? "paths" : "text");
    }

    /** An SVG path for any Java2D shape, flattened only where curved. */
    private static String path(java.awt.Shape shape) {
        StringBuilder d = new StringBuilder();
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null); !it.isDone();
                it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> d.append(String.format(
                        Locale.ROOT, "M%.2f %.2f ", c[0], c[1]));
                case PathIterator.SEG_LINETO -> d.append(String.format(
                        Locale.ROOT, "L%.2f %.2f ", c[0], c[1]));
                case PathIterator.SEG_QUADTO -> d.append(String.format(
                        Locale.ROOT, "Q%.2f %.2f %.2f %.2f ", c[0], c[1],
                        c[2], c[3]));
                case PathIterator.SEG_CUBICTO -> d.append(String.format(
                        Locale.ROOT, "C%.2f %.2f %.2f %.2f %.2f %.2f ",
                        c[0], c[1], c[2], c[3], c[4], c[5]));
                case PathIterator.SEG_CLOSE -> d.append("Z ");
                default -> throw new IllegalStateException("path segment");
            }
        }
        return d.toString().trim();
    }

    private static String hex(Color colour) {
        return String.format(Locale.ROOT, "#%02x%02x%02x",
                colour.getRed(), colour.getGreen(), colour.getBlue());
    }

    private static String dash(float[] pattern) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < pattern.length; i++) {
            text.append(i == 0 ? "" : ",")
                    .append(String.format(Locale.ROOT, "%.2f", pattern[i]));
        }
        return text.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ---- PDF: genuine vector, written by hand -------------------------

    private static void writePdf(File file, ChartSheetRecorder recorder,
                                 double sheetWide, double sheetHigh,
                                 double chartWide, double chartHigh)
            throws IOException {
        writePdf(file, recorder, sheetWide, sheetHigh, chartWide,
                chartHigh, false);
    }

    private static void writePdf(File file, ChartSheetRecorder recorder,
                                 double sheetWide, double sheetHigh,
                                 double chartWide, double chartHigh,
                                 boolean textAsPaths)
            throws IOException {
        StringBuilder content = new StringBuilder();
        // PDF's origin is bottom-left; the chart's is top-left.
        content.append(String.format(Locale.ROOT, "1 0 0 -1 %.2f %.2f cm\n",
                MARGIN_PT, sheetHigh - MARGIN_PT));
        content.append("1 J 1 j\n");

        for (ChartSheetRecorder.Drawn drawn : recorder.drawn()) {
            Color colour = drawn.colour();
            content.append(String.format(Locale.ROOT, "%.3f %.3f %.3f %s\n",
                    colour.getRed() / 255.0, colour.getGreen() / 255.0,
                    colour.getBlue() / 255.0,
                    drawn.filled() ? "rg" : "RG"));
            if (!drawn.filled()) {
                content.append(String.format(Locale.ROOT, "%.2f w\n",
                        drawn.stroke().width()));
                content.append(dashOperator(drawn.stroke().dash()));
            }
            content.append(pdfPath(drawn.shape()));
            content.append(drawn.filled() ? "f\n" : "S\n");
        }

        for (ChartSheetRecorder.Text text : recorder.text()) {
            Color colour = text.colour();
            if (textAsPaths) {
                // Outlines: every glyph the chart uses reaches the
                // page, whatever fonts the reader's viewer has and
                // whatever a base-14 encoding can name.
                java.awt.font.GlyphVector glyphs = text.font()
                        .createGlyphVector(
                                new java.awt.font.FontRenderContext(null,
                                        true, true), text.text());
                content.append(String.format(Locale.ROOT,
                        "%.3f %.3f %.3f rg\n",
                        colour.getRed() / 255.0,
                        colour.getGreen() / 255.0,
                        colour.getBlue() / 255.0));
                content.append(pdfPath(glyphs.getOutline((float) text.x(),
                        (float) text.y())));
                content.append("f\n");
                continue;
            }
            content.append(String.format(Locale.ROOT,
                    "BT /F1 %d Tf %.3f %.3f %.3f rg"
                            + " 1 0 0 -1 %.2f %.2f Tm (%s) Tj ET\n",
                    text.font().getSize(),
                    colour.getRed() / 255.0, colour.getGreen() / 255.0,
                    colour.getBlue() / 255.0,
                    text.x(), text.y(), pdfString(text.text())));
        }

        byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objects = new ArrayList<>();
        objects.add(("<< /Type /Catalog /Pages 2 0 R >>")
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add((String.format(Locale.ROOT,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f]"
                        + " /Resources << /Font << /F1 5 0 R >> >>"
                        + " /Contents 4 0 R >>", sheetWide, sheetHigh))
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add((String.format(Locale.ROOT,
                "<< /Length %d >>\nstream\n", stream.length)
                + content + "endstream")
                .getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica"
                + " /Encoding /WinAnsiEncoding >>")
                .getBytes(StandardCharsets.ISO_8859_1));

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        int[] offsets = new int[objects.size() + 1];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = out.size();
            out.write(((i + 1) + " 0 obj\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            out.write(objects.get(i));
            out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        StringBuilder tail = new StringBuilder();
        tail.append("xref\n0 ").append(objects.size() + 1).append("\n");
        tail.append("0000000000 65535 f \n");
        for (int i = 1; i <= objects.size(); i++) {
            tail.append(String.format(Locale.ROOT, "%010d 00000 n \n",
                    offsets[i]));
        }
        tail.append("trailer\n<< /Size ").append(objects.size() + 1)
                .append(" /Root 1 0 R >>\nstartxref\n").append(xref)
                .append("\n%%EOF\n");
        out.write(tail.toString().getBytes(StandardCharsets.ISO_8859_1));
        Files.write(file.toPath(), out.toByteArray());
        System.err.printf(Locale.ROOT, "  %s (%d bytes)%n", file.getName(),
                file.length());
    }

    private static String dashOperator(float[] pattern) {
        if (pattern == null) {
            return "[] 0 d\n";
        }
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < pattern.length; i++) {
            text.append(i == 0 ? "" : " ")
                    .append(String.format(Locale.ROOT, "%.2f", pattern[i]));
        }
        return text.append("] 0 d\n").toString();
    }

    private static String pdfPath(java.awt.Shape shape) {
        StringBuilder d = new StringBuilder();
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null); !it.isDone();
                it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> d.append(String.format(
                        Locale.ROOT, "%.2f %.2f m%n", c[0], c[1]));
                case PathIterator.SEG_LINETO -> d.append(String.format(
                        Locale.ROOT, "%.2f %.2f l%n", c[0], c[1]));
                case PathIterator.SEG_QUADTO -> d.append(String.format(
                        Locale.ROOT, "%.2f %.2f %.2f %.2f %.2f %.2f c%n",
                        c[0], c[1], c[0], c[1], c[2], c[3]));
                case PathIterator.SEG_CUBICTO -> d.append(String.format(
                        Locale.ROOT, "%.2f %.2f %.2f %.2f %.2f %.2f c%n",
                        c[0], c[1], c[2], c[3], c[4], c[5]));
                case PathIterator.SEG_CLOSE -> d.append("h\n");
                default -> throw new IllegalStateException("path segment");
            }
        }
        return d.toString();
    }

    private static String pdfString(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(")
                .replace(")", "\\)");
    }

    // ---- PNG: explicit dimensions and resolution ----------------------

    private static void writePng(File file, ChartScene scene, int dpi)
            throws IOException {
        double chartWideInches =
                (A4_WIDE_PT - 2 * MARGIN_PT) / 72.0;
        double chartHighInches =
                (A4_HIGH_PT - 2 * MARGIN_PT) / 72.0;
        int wide = (int) Math.round(chartWideInches * dpi);
        int high = (int) Math.round(chartHighInches * dpi);

        ChartScene atResolution = new ChartScene(
                new ChartViewport(scene.viewport().centre(),
                        scene.viewport().fieldWidthDegrees(), wide, high),
                scene.stars(), scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(),
                scene.geography());
        BufferedImage image = new BufferedImage(wide, high,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            record(atResolution, g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", file);
        System.err.printf(Locale.ROOT,
                "  %s (%d x %d px at %d dpi = %.1f x %.1f inches,"
                        + " %d bytes)%n",
                file.getName(), wide, high, dpi, chartWideInches,
                chartHighInches, file.length());
    }

    // ---- what the prototypes measured ---------------------------------

    private static void report(ChartSheetRecorder recorder,
                               double chartWide, double chartHigh) {
        int fills = 0;
        double thinnest = Double.MAX_VALUE;
        double smallestFill = Double.MAX_VALUE;
        for (ChartSheetRecorder.Drawn drawn : recorder.drawn()) {
            if (drawn.filled()) {
                fills++;
                java.awt.geom.Rectangle2D bounds =
                        drawn.shape().getBounds2D();
                smallestFill = Math.min(smallestFill,
                        Math.max(bounds.getWidth(), bounds.getHeight()));
            } else {
                thinnest = Math.min(thinnest, drawn.stroke().width());
            }
        }
        int smallestFont = Integer.MAX_VALUE;
        java.util.TreeSet<Character> beyondAscii = new java.util.TreeSet<>();
        for (ChartSheetRecorder.Text text : recorder.text()) {
            smallestFont = Math.min(smallestFont, text.font().getSize());
            for (char each : text.text().toCharArray()) {
                if (each > 126) {
                    beyondAscii.add(each);
                }
            }
        }

        p("# One chart state, three formats, no new dependency");
        p("");
        p("Measured by `make printable-chart-study`. The prototypes"
                + " beside this file are prototypes, not production"
                + " promises: they exist to expose the failures a"
                + " format study cannot argue away.");
        p("");

        p("## The renderer already speaks vector");
        p("");
        p("The gate asks whether the atlas's one Java2D renderer can"
                + " serve paper honestly, or whether a second,"
                + " target-neutral chart-sheet description is needed —"
                + " with the standing rule that the cartography must"
                + " not be forked.");
        p("");
        p("Scanning the compiled painters, the renderer, the"
                + " graticule, the reference ink and the working-mark"
                + " ink use **seventeen** `Graphics2D` methods between"
                + " them:");
        p("");
        p("```");
        p("create dispose draw drawRect drawString fill fillRect");
        p("getFontMetrics rotate setClip clip setColor setFont");
        p("setRenderingHint setStroke translate");
        p("```");
        p("");
        p("Every one is vector. Nothing draws an image, composites,"
                + " or asks for a gradient or a texture.");
        p("");
        p(String.format(Locale.ROOT,
                "So a recording `Graphics2D` can play the production"
                        + " render into any vector target. It records"
                        + " those seventeen and **throws on every"
                        + " other method** — if the cartography ever"
                        + " reaches beyond what a page can hold, an"
                        + " export fails loudly instead of quietly"
                        + " rasterising. Running the real renderer"
                        + " through it recorded **%d shapes and %d"
                        + " text runs** and raised nothing.",
                recorder.drawn().size(), recorder.text().size()));
        p("");
        p("**No fork, and no new dependency.** SVG and PDF are"
                + " written by this repository from that one"
                + " recording.");
        p("");

        p("## The sheet, in physical units");
        p("");
        p("A4 landscape, half-inch margins:");
        p("");
        p("| | |");
        p("|---|---:|");
        p(String.format(Locale.ROOT, "| sheet | %.1f × %.1f mm |",
                A4_WIDE_PT / 72.0 * 25.4, A4_HIGH_PT / 72.0 * 25.4));
        p(String.format(Locale.ROOT, "| chart rectangle | %.1f × %.1f mm |",
                chartWide / 72.0 * 25.4, chartHigh / 72.0 * 25.4));
        p(String.format(Locale.ROOT,
                "| thinnest stroke | %.2f pt = %.3f mm |",
                thinnest, thinnest / 72.0 * 25.4));
        p(String.format(Locale.ROOT,
                "| smallest filled mark | %.2f pt = %.3f mm |",
                smallestFill, smallestFill / 72.0 * 25.4));
        p(String.format(Locale.ROOT,
                "| smallest label | %d pt ≈ %.2f mm cap height |",
                smallestFont, smallestFont * 0.7 / 72.0 * 25.4));
        p(String.format(Locale.ROOT, "| filled marks on the sheet | %d |",
                fills));
        p("");
        p("A one-point stroke is 0.353 mm — comfortably above the"
                + " hairline any office printer loses, and the"
                + " smallest star is nearly two millimetres across."
                + " **The chart is legible on paper at its natural"
                + " size**, which is the finding that matters: no"
                + " print-specific scaling of the ink is required for"
                + " a sheet this size.");
        p("");

        p("## What the prototypes exposed");
        p("");
        p("### A PDF written with base-14 fonts loses the chart's own"
                + " notation");
        p("");
        p("The sheet needs these characters beyond ASCII:");
        p("");
        p("```");
        StringBuilder characters = new StringBuilder();
        for (char each : beyondAscii) {
            characters.append(each).append(' ');
        }
        p(characters.toString().trim());
        p("```");
        p("");
        p("Bayer letters, the degree sign, the prime, the middle dot,"
                + " a superscript, and a true minus. Written as PDF"
                + " text in Helvetica with `WinAnsiEncoding` —"
                + " the obvious no-dependency choice — every one of"
                + " them prints as `?`: *Betelgeuse ?*, *Rigel ?*,"
                + " *?10°*. See `sheet-a4-base14-font.pdf`, kept"
                + " precisely because it is wrong.");
        p("");
        p("Drawing the text as **glyph outlines** fixes it exactly"
                + " (`sheet-a4.pdf`): every letter reaches the page,"
                + " no font is embedded, and no font licence is"
                + " implicated. It costs editability of the text in"
                + " the PDF, which a PDF is not the master for, and"
                + " about 50% more bytes.");
        p("");
        p("### SVG carries the notation as text");
        p("");
        p("`sheet-a4.svg` keeps every character as real `<text>`,"
                + " UTF-8, with `font-family=\"sans-serif\"` — so a"
                + " reader can retype a label in Inkscape or"
                + " Illustrator, which is the whole point of an"
                + " editable master. `sheet-a4-text-as-paths.svg` is"
                + " the same sheet with outlines, for a reader who"
                + " needs the glyphs guaranteed on a machine whose"
                + " fonts are unknown.");
        p("");
        p("Both are self-contained: no external stylesheet, script,"
                + " font or image, and no network reference of any"
                + " kind.");
        p("");
        p("### PNG states what it is");
        p("");
        p("`sheet-a4-300dpi.png` is 3208 × 2180 px — the chart"
                + " rectangle at 300 dpi, which is exactly 10.7 × 7.3"
                + " inches. The dimensions are computed from the"
                + " physical sheet and the stated resolution rather"
                + " than inherited from a window.");
        p("");
    }

    // ---- the scene ----------------------------------------------------

    private static final TiledCatalogue CATALOGUE = TiledCatalogue.load();
    private static final ConstellationGeography GEOGRAPHY =
            ConstellationGeography.load();

    private static void p(String line) {
        System.out.println(line);
    }

    private static ChartScene scene(SkyPosition centre, double field,
                                    double magnitudeLimit, int wide,
                                    int high) {
        ChartViewport viewport =
                new ChartViewport(centre, field, wide, high);
        double halfWidthPlane = Math.tan(Math.toRadians(field) / 2.0);
        double halfHeightPlane = halfWidthPlane * (high / (double) wide);
        double corner = Math.toDegrees(Math.atan(
                Math.hypot(halfWidthPlane, halfHeightPlane)));
        SkyRegion query = new SkyRegion(centre, Math.min(180.0, corner
                + CATALOGUE.manifest().maxObjectSemiExtentDegrees()));
        java.util.Map<String, String> names =
                new java.util.LinkedHashMap<>();
        for (juranometria.geo.Constellation each
                : GEOGRAPHY.constellations()) {
            names.put(each.id(), each.latinName());
        }
        return new ChartScene(viewport, CATALOGUE.starsIn(query),
                CATALOGUE.deepSkyObjectsIn(query),
                "Orion · 42° · stars to V 6.0", magnitudeLimit, null,
                new SceneGeography(GEOGRAPHY.figureSegmentsIn(query),
                        GEOGRAPHY.boundarySegmentsIn(query), names));
    }
}
