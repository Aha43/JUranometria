package juranometria.sheet;

import java.awt.Color;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A recorded sheet as PDF (Sprint 29, issue #286).
 *
 * <p>The PDF is the printable handoff: a reader with no vector editor
 * and no interest in one opens it and presses print, and the platform's
 * own dialog does the rest. So it has to be two things at once - a
 * genuine vector document, and a document that states its physical
 * page size - and it is written by hand against the same recording SVG
 * is written from. There is no second render and no new dependency.
 *
 * <p><strong>Text is drawn as outlines</strong>, and that is a
 * decision with a measurement behind it. The gate's own prototype used
 * the base-14 Helvetica every PDF reader has, and the chart's notation
 * came out as question marks: the sheet needs {@code ° ³ · α β γ δ ε ι
 * κ ξ π ′ −}, and WinAnsi encoding has none of the Greek. Embedding a
 * font was rejected for this sprint, so every label is emitted as the
 * curves of its glyphs. A label in this file cannot be retyped - which
 * is what the SVG is for.
 */
public final class PdfSheetWriter {

    private PdfSheetWriter() {
    }

    /** The sheet as PDF bytes. */
    public static byte[] write(SheetRecording sheet) throws IOException {
        if (sheet == null) {
            throw new IllegalArgumentException("a sheet is required");
        }
        PaperSize paper = sheet.paper();
        StringBuilder content = new StringBuilder();

        // PDF's origin is bottom-left and the chart's is top-left, so
        // the whole page is flipped once, here, rather than every
        // coordinate being flipped as it is written.
        content.append(String.format(Locale.ROOT, "1 0 0 -1 %.2f %.2f cm%n",
                paper.marginPoints(),
                paper.highPoints() - paper.marginPoints()));


        // The paper, so a viewer that composites onto black still
        // shows a white sheet.
        Color ground = sheet.options().palette().ground();
        content.append(String.format(Locale.ROOT, "%.3f %.3f %.3f rg%n",
                ground.getRed() / 255.0, ground.getGreen() / 255.0,
                ground.getBlue() / 255.0));
        content.append(String.format(Locale.ROOT,
                "%.2f %.2f m %.2f %.2f l %.2f %.2f l %.2f %.2f l h f%n",
                -paper.marginPoints(), -paper.marginPoints(),
                paper.widePoints() - paper.marginPoints(),
                -paper.marginPoints(),
                paper.widePoints() - paper.marginPoints(),
                paper.highPoints() - paper.marginPoints(),
                -paper.marginPoints(),
                paper.highPoints() - paper.marginPoints()));

        // In the order the renderer drew, not shapes then text: the
        // second is a reordering, and it printed "CANIS MAJOR"
        // through the title box (PR #292, found on paper).
        for (SheetRecorder.Operation operation
                : sheet.recorder().operations()) {
            content.append(open(operation.clip()));
            if (operation instanceof SheetRecorder.Drawn drawn) {
                Color colour = drawn.colour();
                content.append(String.format(Locale.ROOT,
                        "%.3f %.3f %.3f %s%n",
                        colour.getRed() / 255.0, colour.getGreen() / 255.0,
                        colour.getBlue() / 255.0,
                        drawn.filled() ? "rg" : "RG"));
                if (!drawn.filled()) {
                    SheetRecorder.BasicStrokeSpec stroke = drawn.stroke();
                    content.append(String.format(Locale.ROOT,
                            "%.2f w %d J %d j %.2f M%n", stroke.width(),
                            pdfCap(stroke.cap()), pdfJoin(stroke.join()),
                            stroke.miterLimit()));
                    content.append(dash(stroke.dash(),
                            stroke.dashPhase()));
                }
                content.append(path(drawn.shape()));
                content.append(drawn.filled() ? "f\n" : "S\n");
            } else {
                SheetRecorder.Text label = (SheetRecorder.Text) operation;
                Color colour = label.colour();
                content.append(String.format(Locale.ROOT,
                        "%.3f %.3f %.3f rg%n",
                        colour.getRed() / 255.0, colour.getGreen() / 255.0,
                        colour.getBlue() / 255.0));
                GlyphVector glyphs = label.font().createGlyphVector(
                        new FontRenderContext(null, true, true),
                        label.text());
                content.append(path(glyphs.getOutline((float) label.x(),
                        (float) label.y())));
                content.append("f\n");
            }
            content.append(close(operation.clip()));
        }

        return document(content.toString(), sheet);
    }

    /** The objects, the cross-reference table and the trailer. */
    private static byte[] document(String content, SheetRecording sheet)
            throws IOException {
        PaperSize paper = sheet.paper();
        byte[] stream = content.getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objects = new ArrayList<>();
        objects.add(latin("<< /Type /Catalog /Pages 2 0 R >>"));
        objects.add(latin("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"));
        // The MediaBox is the physical page: this is where a PDF
        // states how big it is, and a printer believes it.
        objects.add(latin(String.format(Locale.ROOT,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f]"
                        + " /Resources << >> /Contents 4 0 R >>",
                paper.widePoints(), paper.highPoints())));
        objects.add(latin(String.format(Locale.ROOT,
                "<< /Length %d >>\nstream\n", stream.length)
                + content + "endstream"));
        objects.add(latin("<< /Type /Info /Producer "
                + pdfString(sheet.metadata().producedBy()) + " /Title "
                + pdfString(sheet.metadata().title()) + " /Subject "
                + pdfString(sheet.metadata().description()) + " >>"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(latin("%PDF-1.4\n"));
        int[] offsets = new int[objects.size() + 1];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = out.size();
            out.write(latin((i + 1) + " 0 obj\n"));
            out.write(objects.get(i));
            out.write(latin("\nendobj\n"));
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
                .append(" /Root 1 0 R /Info ").append(objects.size())
                .append(" 0 R >>\nstartxref\n").append(xref)
                .append("\n%%EOF\n");
        out.write(latin(tail.toString()));
        return out.toByteArray();
    }

    /** Push the graphics state and clip, when there is a clip. */
    private static String open(Shape clip) {
        return clip == null ? "" : "q\n" + path(clip) + "W n\n";
    }

    private static String close(Shape clip) {
        return clip == null ? "" : "Q\n";
    }

    private static String dash(float[] pattern, float phase) {
        if (pattern == null) {
            return "[] 0 d\n";
        }
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < pattern.length; i++) {
            text.append(i == 0 ? "" : " ")
                    .append(String.format(Locale.ROOT, "%.2f", pattern[i]));
        }
        return text.append(String.format(Locale.ROOT, "] %.2f d%n", phase))
                .toString();
    }

    /** PDF's number for a Java end cap. */
    private static int pdfCap(int cap) {
        return switch (cap) {
            case java.awt.BasicStroke.CAP_BUTT -> 0;
            case java.awt.BasicStroke.CAP_ROUND -> 1;
            default -> 2;
        };
    }

    /** PDF's number for a Java line join. */
    private static int pdfJoin(int join) {
        return switch (join) {
            case java.awt.BasicStroke.JOIN_MITER -> 0;
            case java.awt.BasicStroke.JOIN_ROUND -> 1;
            default -> 2;
        };
    }

    /**
     * A shape as PDF path operators.
     *
     * <p>PDF has no quadratic operator, so a quadratic is
     * <em>converted</em> rather than approximated, and the equivalent
     * cubic's controls depend on the current point: {@code P0 +
     * 2/3(Q - P0)} and {@code P2 + 2/3(Q - P2)}. An early version
     * emitted the quadratic's own control twice, which is a different
     * curve, and changed the geometry silently (PR #288 review).
     */
    static String path(Shape shape) {
        StringBuilder d = new StringBuilder();
        double[] c = new double[6];
        double currentX = 0;
        double currentY = 0;
        double startX = 0;
        double startY = 0;
        for (PathIterator it = shape.getPathIterator(null); !it.isDone();
                it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> {
                    d.append(String.format(Locale.ROOT, "%.2f %.2f m%n",
                            c[0], c[1]));
                    currentX = c[0];
                    currentY = c[1];
                    startX = c[0];
                    startY = c[1];
                }
                case PathIterator.SEG_LINETO -> {
                    d.append(String.format(Locale.ROOT, "%.2f %.2f l%n",
                            c[0], c[1]));
                    currentX = c[0];
                    currentY = c[1];
                }
                case PathIterator.SEG_QUADTO -> {
                    double c1x = currentX + 2.0 / 3.0 * (c[0] - currentX);
                    double c1y = currentY + 2.0 / 3.0 * (c[1] - currentY);
                    double c2x = c[2] + 2.0 / 3.0 * (c[0] - c[2]);
                    double c2y = c[3] + 2.0 / 3.0 * (c[1] - c[3]);
                    d.append(String.format(Locale.ROOT,
                            "%.2f %.2f %.2f %.2f %.2f %.2f c%n",
                            c1x, c1y, c2x, c2y, c[2], c[3]));
                    currentX = c[2];
                    currentY = c[3];
                }
                case PathIterator.SEG_CUBICTO -> {
                    d.append(String.format(Locale.ROOT,
                            "%.2f %.2f %.2f %.2f %.2f %.2f c%n",
                            c[0], c[1], c[2], c[3], c[4], c[5]));
                    currentX = c[4];
                    currentY = c[5];
                }
                case PathIterator.SEG_CLOSE -> {
                    d.append("h\n");
                    currentX = startX;
                    currentY = startY;
                }
                default -> throw new IllegalStateException(
                        "a path segment a page cannot hold");
            }
        }
        return d.toString();
    }

    private static String pdfString(String text) {
        return "(" + text.replace("\\", "\\\\").replace("(", "\\(")
                .replace(")", "\\)") + ")";
    }

    private static byte[] latin(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }
}
