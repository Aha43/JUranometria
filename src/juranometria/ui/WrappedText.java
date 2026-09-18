package juranometria.ui;

import java.awt.FontMetrics;

/**
 * Prose broken into lines that fit (Sprint 33, issue #350).
 *
 * <p>Extracted so two surfaces share one algorithm rather than two
 * copies of it. Chart Options has wrapped its family descriptions
 * this way since #311; the Inspector needed the same when a
 * translated sentence ran past the panel's edge and lost its ending,
 * and a second implementation would have been free to drift from the
 * first.
 *
 * <p><strong>Measured, not declared.</strong> A CSS width on an HTML
 * body does not bound a label's preferred width - measured at 425 px
 * for a body declared 310 px wide - and Swing then paints the text
 * past the label's own edge, where no bounds check finds it. Breaking
 * the lines against real font metrics is the only way the words are
 * known to fit.
 */
public final class WrappedText {

    private WrappedText() {
    }

    /**
     * The prose as HTML, broken so no line exceeds the width.
     *
     * <p>Breaks between words only. A single word wider than the
     * space is left long rather than cut: a name split mid-word is
     * worse than one that overhangs, and the caller who chose the
     * width is better placed to widen it.
     */
    public static String html(String prose, int widthPx,
                              FontMetrics metrics) {
        StringBuilder html = new StringBuilder("<html>");
        StringBuilder line = new StringBuilder();
        for (String word : prose.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty()
                    && metrics.stringWidth(candidate) > widthPx) {
                html.append(escaped(line.toString())).append("<br>");
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        return html.append(escaped(line.toString())).append("</html>")
                .toString();
    }

    private static String escaped(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
