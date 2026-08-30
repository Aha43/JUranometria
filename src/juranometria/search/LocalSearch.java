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
import juranometria.chart.StarIdentity;
import juranometria.geo.Constellation;

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
 *
 * Star identities follow the reviewed grammar
 * (docs/decisions/star-identity.md): traditional names by prefix;
 * Bayer as the Greek letter or its spelled-out name plus the
 * constellation (genitive and IAU abbreviation both accepted, with
 * and without a component digit); Flamsteed as the number plus the
 * constellation. A bare letter or bare number never silently
 * resolves - it lists its matches like any prefix search, and every
 * star display line carries the full identity so the reader picks
 * knowingly.
 */
public final class LocalSearch {

    public static final int MAX_RESULTS = 8;

    private record Entry(SearchResult result, List<String> keys) {
    }

    private final List<Entry> entries;

    public LocalSearch(List<Star> stars, List<DeepSkyObject> deepSkyObjects) {
        this(stars, deepSkyObjects, List.of());
    }

    public LocalSearch(List<Star> stars, List<DeepSkyObject> deepSkyObjects,
                       List<Constellation> constellations) {
        java.util.Map<String, Constellation> byAbbreviation =
                new java.util.HashMap<>();
        for (Constellation constellation : constellations) {
            byAbbreviation.put(constellation.id(), constellation);
        }
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
            List<String> keys = new ArrayList<>();
            keys.add(normalize(star.id()));
            identityKeys(star.identity(), byAbbreviation, keys);
            built.add(new Entry(new SearchResult(starLabel(star), star.id(),
                    SearchResult.Kind.STAR, star.position(),
                    starRegionTitle(star)), List.copyOf(keys)));
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

    /** The Greek letters, spelled out for typed Bayer queries. */
    private static final java.util.Map<Character, String> GREEK_NAMES =
            java.util.Map.ofEntries(
                    java.util.Map.entry('\u03b1', "alpha"),
                    java.util.Map.entry('\u03b2', "beta"),
                    java.util.Map.entry('\u03b3', "gamma"),
                    java.util.Map.entry('\u03b4', "delta"),
                    java.util.Map.entry('\u03b5', "epsilon"),
                    java.util.Map.entry('\u03b6', "zeta"),
                    java.util.Map.entry('\u03b7', "eta"),
                    java.util.Map.entry('\u03b8', "theta"),
                    java.util.Map.entry('\u03b9', "iota"),
                    java.util.Map.entry('\u03ba', "kappa"),
                    java.util.Map.entry('\u03bb', "lambda"),
                    java.util.Map.entry('\u03bc', "mu"),
                    java.util.Map.entry('\u03bd', "nu"),
                    java.util.Map.entry('\u03be', "xi"),
                    java.util.Map.entry('\u03bf', "omicron"),
                    java.util.Map.entry('\u03c0', "pi"),
                    java.util.Map.entry('\u03c1', "rho"),
                    java.util.Map.entry('\u03c3', "sigma"),
                    java.util.Map.entry('\u03c4', "tau"),
                    java.util.Map.entry('\u03c5', "upsilon"),
                    java.util.Map.entry('\u03c6', "phi"),
                    java.util.Map.entry('\u03c7', "chi"),
                    java.util.Map.entry('\u03c8', "psi"),
                    java.util.Map.entry('\u03c9', "omega"));

    /**
     * The reviewed identity grammar as normalized keys. Every Bayer
     * and Flamsteed key carries a constellation form, so a bare
     * letter or number can only ever prefix-list, never resolve
     * exactly; componentless variants make "alpha cru" list both
     * components of a split designation rather than hiding them.
     */
    private static void identityKeys(StarIdentity identity,
                                     java.util.Map<String, Constellation> byAbbreviation,
                                     List<String> keys) {
        if (identity == null) {
            return;
        }
        if (identity.name() != null) {
            keys.add(normalize(identity.name()));
        }
        if (identity.constellation() == null) {
            return;
        }
        List<String> constellationForms = new ArrayList<>();
        constellationForms.add(normalize(identity.constellation()));
        Constellation constellation =
                byAbbreviation.get(identity.constellation());
        if (constellation != null) {
            constellationForms.add(normalize(constellation.genitive()));
        }
        if (identity.bayer() != null) {
            for (String letter : letterForms(identity.bayer())) {
                for (String form : constellationForms) {
                    keys.add(letter + form);
                }
            }
        }
        if (identity.flamsteed() != null) {
            for (String form : constellationForms) {
                keys.add(normalize(identity.flamsteed()) + form);
            }
        }
    }

    /** Verbatim, spelled-out, and componentless forms of a Bayer letter. */
    private static List<String> letterForms(String bayer) {
        String verbatim = normalize(bayer);
        LinkedHashSet<String> forms = new LinkedHashSet<>();
        forms.add(verbatim);
        forms.add(spellGreek(verbatim));
        String componentless = verbatim.replaceAll("\\d+$", "");
        if (!componentless.isEmpty() && !componentless.equals(verbatim)) {
            forms.add(componentless);
            forms.add(spellGreek(componentless));
        }
        return List.copyOf(forms);
    }

    private static String spellGreek(String letter) {
        String name = letter.isEmpty() ? null
                : GREEK_NAMES.get(letter.charAt(0));
        return name == null ? letter : name + letter.substring(1);
    }

    /**
     * The display line for a star, per the decision: the full
     * identity ("Betelgeuse \u00b7 \u03b1 Ori \u00b7 V 0.6") so ambiguous
     * queries resolve by an informed choice; duplicate proper names
     * disambiguate by designation. An identityless star stays its
     * catalogue identifier.
     */
    private static String starLabel(Star star) {
        String designation = designation(star.identity());
        if (star.identity() == null
                || (star.identity().name() == null && designation == null)) {
            return star.id();
        }
        List<String> parts = new ArrayList<>();
        if (star.identity().name() != null) {
            parts.add(star.identity().name());
        }
        if (designation != null) {
            parts.add(designation);
        }
        parts.add(String.format(java.util.Locale.ROOT, "V %.1f",
                star.magnitude()));
        return String.join(" \u00b7 ", parts);
    }

    /** The chart title for a star: its best human identity, honestly. */
    private static String starRegionTitle(Star star) {
        String designation = designation(star.identity());
        if (star.identity() == null
                || (star.identity().name() == null && designation == null)) {
            return star.id() + " region";
        }
        if (star.identity().name() == null) {
            return designation + " region";
        }
        return designation == null
                ? star.identity().name() + " region"
                : star.identity().name() + " \u00b7 " + designation + " region";
    }

    /** The star's designation, Bayer before Flamsteed, or null. */
    private static String designation(StarIdentity identity) {
        if (identity == null) {
            return null;
        }
        if (identity.bayer() != null) {
            return identity.bayer() + " " + identity.constellation();
        }
        if (identity.flamsteed() != null) {
            return identity.flamsteed() + " " + identity.constellation();
        }
        return null;
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
