package juranometria.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyFormat;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;

/**
 * Deterministic local search over the bundled catalogue's identifiers,
 * aliases, and coordinate input. Built once from immutable domain lists —
 * never from files — and independent of Swing, rendering, and the
 * network. Every outcome is an immutable result list; empty, malformed,
 * out-of-range, and unmatched queries return an empty list rather than
 * throwing.
 *
 * Normalization is forgiving within reason: case-insensitive, whitespace
 * ignored, and "Messier N" equivalent to "M N". Exact normalized matches
 * rank before prefix matches, prefix before partial, each bucket ordered
 * by label then identity, bounded to {@link #MAX_RESULTS}.
 *
 * Supported coordinate forms (RA then Dec, separated by whitespace or a
 * comma): decimal degrees ("10.68 41.27") and colon sexagesimal
 * ("0:42:44.3 +41:16:09", RA in hours).
 */
public final class LocalSearch {

    public static final int MAX_RESULTS = 8;

    private record Entry(SearchResult result, List<String> keys) {
    }

    private final List<Entry> entries;

    public LocalSearch(List<Star> stars, List<DeepSkyObject> deepSkyObjects) {
        List<Entry> built = new ArrayList<>();
        for (DeepSkyObject dso : deepSkyObjects) {
            List<String> keys = new ArrayList<>();
            keys.add(normalize(dso.id()));
            for (String alias : dso.aliases()) {
                keys.add(normalize(alias));
            }
            built.add(new Entry(new SearchResult(displayLabel(dso), dso.id(),
                    SearchResult.Kind.DEEP_SKY_OBJECT, dso.position(),
                    regionTitle(dso)), keys));
        }
        for (Star star : stars) {
            built.add(new Entry(new SearchResult(star.id(), star.id(),
                    SearchResult.Kind.STAR, star.position(),
                    star.id() + " region"),
                    List.of(normalize(star.id()))));
        }
        this.entries = List.copyOf(built);
    }

    /** Resolves a query to a bounded, deterministic, immutable result list. */
    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Optional<SkyPosition> coordinates = parseCoordinates(query.trim());
        if (coordinates.isPresent()) {
            SkyPosition position = coordinates.get();
            String display = SkyFormat.formatRa(position.raDegrees())
                    + ", " + SkyFormat.formatDec(position.decDegrees());
            return List.of(new SearchResult(display, display,
                    SearchResult.Kind.COORDINATES, position, display));
        }

        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<SearchResult> exact = matches(entry ->
                entry.keys().stream().anyMatch(normalized::equals));
        List<SearchResult> prefix = matches(entry ->
                entry.keys().stream().anyMatch(key -> key.startsWith(normalized)));
        List<SearchResult> partial = matches(entry ->
                entry.keys().stream().anyMatch(key -> key.contains(normalized)));

        Set<String> seen = new LinkedHashSet<>();
        List<SearchResult> ranked = new ArrayList<>();
        for (List<SearchResult> bucket : List.of(exact, prefix, partial)) {
            for (SearchResult result : bucket) {
                if (seen.add(result.identity()) && ranked.size() < MAX_RESULTS) {
                    ranked.add(result);
                }
            }
        }
        return List.copyOf(ranked);
    }

    private List<SearchResult> matches(java.util.function.Predicate<Entry> predicate) {
        return entries.stream()
                .filter(predicate)
                .map(Entry::result)
                .sorted(Comparator.comparing(SearchResult::label)
                        .thenComparing(SearchResult::identity))
                .toList();
    }

    /** The chart title for a result: label, common name, and "region". */
    private static String regionTitle(DeepSkyObject dso) {
        String label = displayLabel(dso);
        return dso.aliases().stream()
                .filter(alias -> !alias.startsWith("M ") && !alias.equals(label))
                .findFirst()
                .map(common -> label + " \u00b7 " + common + " region")
                .orElse(label + " region");
    }

    /** The atlas names Messier objects by their Messier name. */
    private static String displayLabel(DeepSkyObject dso) {
        return dso.aliases().stream()
                .filter(alias -> alias.startsWith("M "))
                .findFirst()
                .orElse(dso.id());
    }

    /** Lowercase, whitespace removed, "messier" folded to its "m" form. */
    static String normalize(String raw) {
        String key = raw.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", "");
        if (key.startsWith("messier")) {
            key = "m" + key.substring("messier".length());
        }
        return key;
    }

    /** Parses the documented coordinate forms; empty when not coordinates. */
    static Optional<SkyPosition> parseCoordinates(String raw) {
        String[] tokens = raw.split("\\s*,\\s*|\\s+");
        if (tokens.length != 2) {
            return Optional.empty();
        }
        try {
            double raDegrees = tokens[0].contains(":")
                    ? sexagesimal(tokens[0]) * 15.0
                    : Double.parseDouble(tokens[0]);
            double decDegrees = tokens[1].contains(":")
                    ? sexagesimal(tokens[1])
                    : Double.parseDouble(tokens[1]);
            if (!(raDegrees >= 0.0 && raDegrees < 360.0)
                    || !(decDegrees >= -90.0 && decDegrees <= 90.0)) {
                return Optional.empty();
            }
            return Optional.of(new SkyPosition(raDegrees, decDegrees));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static double sexagesimal(String token) {
        boolean negative = token.startsWith("-");
        String[] parts = token.replaceFirst("^[+-]", "").split(":");
        if (parts.length != 3) {
            throw new NumberFormatException("expected three colon-separated parts: " + token);
        }
        // Only the whole token may carry a sign, and minutes and seconds
        // must lie in [0, 60) — otherwise 1:-30:00 or 0:99:00 would be
        // silently normalized into a different valid position.
        for (String part : parts) {
            if (part.startsWith("+") || part.startsWith("-")) {
                throw new NumberFormatException("signed component in: " + token);
            }
        }
        int whole = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        if (minutes >= 60 || seconds >= 60.0) {
            throw new NumberFormatException("minutes and seconds must be below 60: " + token);
        }
        double value = whole + minutes / 60.0 + seconds / 3600.0;
        return negative ? -value : value;
    }
}
