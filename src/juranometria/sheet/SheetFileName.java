package juranometria.sheet;

import java.util.Locale;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;

/**
 * What a sheet is called before a reader renames it (Sprint 29,
 * issue #286).
 *
 * <p>A reader printing for a club evening is making files for other
 * people, and "chart.svg" in a shared folder is a file nobody can
 * find again. So the name says what the chart is and how wide it is:
 * {@code juranometria-orion-42deg.svg}.
 */
public final class SheetFileName {

    private SheetFileName() {
    }

    /** The suggested name, extension included. */
    public static String suggest(ChartViewState state, ChartScene scene,
                                 SheetFormat format) {
        if (state == null || scene == null || format == null) {
            throw new IllegalArgumentException(
                    "a chart and a format are required");
        }
        String subject = state.targetLabel() != null
                ? state.targetLabel() : scene.title();
        String slug = slug(subject);
        return String.format(Locale.ROOT, "juranometria-%s%.0fdeg.%s",
                slug.isEmpty() ? "" : slug + "-",
                state.fieldWidthDegrees(), format.extension());
    }

    /**
     * A subject as a filename fragment: lower case, ASCII letters and
     * digits, single hyphens, and short enough to read. Accented
     * letters keep their base form rather than vanishing, so
     * "Andrómeda" does not become "andrmeda".
     */
    static String slug(String subject) {
        if (subject == null) {
            return "";
        }
        String folded = java.text.Normalizer.normalize(subject,
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        StringBuilder slug = new StringBuilder();
        for (char each : folded.toLowerCase(Locale.ROOT).toCharArray()) {
            if (each >= 'a' && each <= 'z' || each >= '0' && each <= '9') {
                slug.append(each);
            } else if (slug.length() > 0
                    && slug.charAt(slug.length() - 1) != '-') {
                slug.append('-');
            }
        }
        while (slug.length() > 0 && slug.charAt(slug.length() - 1) == '-') {
            slug.setLength(slug.length() - 1);
        }
        return slug.length() > 40 ? trim(slug.substring(0, 40))
                : slug.toString();
    }

    private static String trim(String slug) {
        int lastHyphen = slug.lastIndexOf('-');
        return lastHyphen > 0 ? slug.substring(0, lastHyphen) : slug;
    }
}
