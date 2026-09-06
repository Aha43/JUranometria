package juranometria.sheet;

import java.awt.Color;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A recorded sheet as SVG (Sprint 29, issue #285).
 *
 * <p>SVG is the editable master, and the gate chose it for a reason
 * that shapes every decision here: the reader who asked for this had
 * already written his own charts in Python because the atlas could
 * not give him one, and what he wanted back was a file he could
 * work with - not a picture of a chart, a chart.
 *
 * <p>So: <strong>text stays text</strong>, in a generic family, so a
 * label can be retyped in Inkscape rather than nudged as a set of
 * outlines. <strong>Layers are grouped</strong>, so ink and labels
 * can be selected apart. <strong>Nothing is fetched</strong> - no
 * stylesheet, no script, no font, no image, no network reference of
 * any kind - so the file works on a machine that has never heard of
 * this project, which is the 1.0 offline promise applied to what the
 * atlas hands out rather than only to what it reads.
 *
 * <p>Written by hand against the seventeen-method surface the
 * cartography actually uses. The gate weighed Batik and rejected it:
 * a large tree for one writer, against four native images whose size
 * is a stated contract.
 */
public final class SvgSheetWriter {

    private SvgSheetWriter() {
    }

    /** Whether a label is written as characters or as its outline. */
    public enum Text {

        /**
         * Characters, which is the master: editable, searchable, and
         * a tenth the size. Rendered with whatever sans-serif the
         * viewer has.
         */
        EDITABLE,

        /**
         * Outlines, for a machine whose fonts are unknown. Every
         * label lands exactly where this machine put it, and none of
         * them can be edited as words again.
         */
        OUTLINES
    }

    /** The sheet as an SVG document. */
    public static String write(SheetRecording sheet, Text text) {
        if (sheet == null || text == null) {
            throw new IllegalArgumentException(
                    "a sheet and a text policy are required");
        }
        PaperSize paper = sheet.paper();
        StringBuilder svg = new StringBuilder();

        // Physical size first, in points, with a viewBox in the same
        // units: a viewer that honours the width and height prints it
        // at the size the paper actually is, and one that only knows
        // the viewBox still gets the right proportions.
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\"")
                .append(String.format(Locale.ROOT,
                        " width=\"%.2fpt\" height=\"%.2fpt\""
                                + " viewBox=\"0 0 %.2f %.2f\"",
                        paper.widePoints(), paper.highPoints(),
                        paper.widePoints(), paper.highPoints()))
                .append(" version=\"1.1\">\n");

        SheetMetadata about = sheet.metadata();
        svg.append("  <title>").append(escape(about.title()))
                .append("</title>\n");
        svg.append("  <desc>").append(escape(about.description()))
                .append("</desc>\n");
        svg.append("  <metadata>").append(escape(about.producedBy()))
                .append("</metadata>\n");

        // Production's own clips, as clip paths. The renderer cuts
        // its ink at the paper's edge; dropping that would let a line
        // that production had clipped bleed into the margin.
        List<Shape> clips = distinctClips(sheet.recorder());
        svg.append("  <defs>\n");
        for (int i = 0; i < clips.size(); i++) {
            svg.append(String.format(Locale.ROOT,
                    "    <clipPath id=\"clip%d\"><path d=\"%s\"/>"
                            + "</clipPath>%n", i, path(clips.get(i))));
        }
        svg.append("  </defs>\n");

        // The paper itself. Named, so a reader who wants a dark sheet
        // changes one fill rather than every mark.
        svg.append(String.format(Locale.ROOT,
                "  <rect id=\"paper\" x=\"0\" y=\"0\" width=\"%.2f\""
                        + " height=\"%.2f\" fill=\"%s\"/>%n",
                paper.widePoints(), paper.highPoints(),
                hex(sheet.options().palette().ground())));

        svg.append(String.format(Locale.ROOT,
                "  <g id=\"chart\" transform=\"translate(%.2f,%.2f)\">%n",
                paper.marginPoints(), paper.marginPoints()));

        svg.append("    <g id=\"ink\" fill=\"none\""
                + " stroke-linecap=\"butt\">\n");
        for (SheetRecorder.Drawn drawn : sheet.recorder().drawn()) {
            svg.append("      <path d=\"").append(path(drawn.shape()))
                    .append('"').append(clipAttribute(clips, drawn.clip()));
            if (drawn.filled()) {
                svg.append(" fill=\"").append(hex(drawn.colour()))
                        .append('"');
            } else {
                svg.append(" stroke=\"").append(hex(drawn.colour()))
                        .append(String.format(Locale.ROOT,
                                "\" stroke-width=\"%.2f\"",
                                drawn.stroke().width()));
                if (drawn.stroke().dash() != null) {
                    svg.append(" stroke-dasharray=\"")
                            .append(dash(drawn.stroke().dash()))
                            .append('"');
                }
            }
            svg.append("/>\n");
        }
        svg.append("    </g>\n");

        svg.append("    <g id=\"labels\">\n");
        for (SheetRecorder.Text label : sheet.recorder().text()) {
            String clip = clipAttribute(clips, label.clip());
            if (text == Text.OUTLINES) {
                GlyphVector glyphs = label.font().createGlyphVector(
                        new FontRenderContext(null, true, true),
                        label.text());
                svg.append("      <path d=\"")
                        .append(path(glyphs.getOutline(
                                (float) label.x(), (float) label.y())))
                        .append('"').append(clip)
                        .append(" fill=\"").append(hex(label.colour()))
                        .append("\"/>\n");
            } else {
                // The weight and the slant are the label's meaning,
                // not its decoration: the title block is bold
                // because it is the title, and a viewer given only a
                // family and a size would draw it as body text
                // (PR #290 review).
                svg.append(String.format(Locale.ROOT,
                                "      <text x=\"%.2f\" y=\"%.2f\"%s"
                                        + " font-family=\"sans-serif\""
                                        + " font-size=\"%d\"%s%s"
                                        + " fill=\"%s\">",
                                label.x(), label.y(), clip,
                                label.font().getSize(),
                                label.font().isBold()
                                        ? " font-weight=\"bold\"" : "",
                                label.font().isItalic()
                                        ? " font-style=\"italic\"" : "",
                                hex(label.colour())))
                        .append(escape(label.text()))
                        .append("</text>\n");
            }
        }
        svg.append("    </g>\n");

        svg.append("  </g>\n</svg>\n");
        return svg.toString();
    }

    /**
     * The distinct clips the render had in force, in the order they
     * were first used, so a path can name the one it was drawn under.
     */
    static List<Shape> distinctClips(SheetRecorder recorder) {
        List<Shape> clips = new ArrayList<>();
        for (SheetRecorder.Drawn drawn : recorder.drawn()) {
            remember(clips, drawn.clip());
        }
        for (SheetRecorder.Text text : recorder.text()) {
            remember(clips, text.clip());
        }
        return clips;
    }

    private static void remember(List<Shape> clips, Shape clip) {
        if (clip == null) {
            return;
        }
        for (Shape known : clips) {
            if (path(known).equals(path(clip))) {
                return;
            }
        }
        clips.add(clip);
    }

    private static String clipAttribute(List<Shape> clips, Shape clip) {
        if (clip == null) {
            return "";
        }
        String wanted = path(clip);
        for (int i = 0; i < clips.size(); i++) {
            if (path(clips.get(i)).equals(wanted)) {
                return " clip-path=\"url(#clip" + i + ")\"";
            }
        }
        return "";
    }

    /** A shape as SVG path data. */
    static String path(Shape shape) {
        StringBuilder path = new StringBuilder();
        double[] segment = new double[6];
        for (PathIterator each = shape.getPathIterator(null);
                !each.isDone(); each.next()) {
            switch (each.currentSegment(segment)) {
                case PathIterator.SEG_MOVETO -> path.append(
                        String.format(Locale.ROOT, "M%.2f %.2f ",
                                segment[0], segment[1]));
                case PathIterator.SEG_LINETO -> path.append(
                        String.format(Locale.ROOT, "L%.2f %.2f ",
                                segment[0], segment[1]));
                case PathIterator.SEG_QUADTO -> path.append(
                        String.format(Locale.ROOT, "Q%.2f %.2f %.2f %.2f ",
                                segment[0], segment[1], segment[2],
                                segment[3]));
                case PathIterator.SEG_CUBICTO -> path.append(
                        String.format(Locale.ROOT,
                                "C%.2f %.2f %.2f %.2f %.2f %.2f ",
                                segment[0], segment[1], segment[2],
                                segment[3], segment[4], segment[5]));
                default -> path.append("Z ");
            }
        }
        return path.toString().trim();
    }

    private static String dash(float[] pattern) {
        StringBuilder dashes = new StringBuilder();
        for (float each : pattern) {
            if (dashes.length() > 0) {
                dashes.append(',');
            }
            dashes.append(String.format(Locale.ROOT, "%.2f", each));
        }
        return dashes.toString();
    }

    private static String hex(Color colour) {
        return String.format(Locale.ROOT, "#%02x%02x%02x",
                colour.getRed(), colour.getGreen(), colour.getBlue());
    }

    /** XML's five, and nothing else - the text itself stays UTF-8. */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
