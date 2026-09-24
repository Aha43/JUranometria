package juranometria.render;

import java.util.Optional;

/**
 * The semantic chart structures a reader may temporarily emphasize
 * (issue #361).
 *
 * <p>Chart-owned. The core painters name these constants directly;
 * modules never see this type - they go on contributing typed
 * geometry under their own string identities, and the chart maps the
 * identities it knows here, centrally. An identity the chart does
 * not know keeps canonical ink, so a future module's contribution is
 * drawn correctly before it is ever emphasizable.
 *
 * <p>Emphasis alters ink only. It must never reuse visibility
 * toggles or any path that changes anchors, names, geometry or
 * membership - the discovery mock proved a layer toggle reflows the
 * page (#307 anchored stars, name anchoring), which is exactly what
 * a reading aid must not do.
 */
public enum ChartStructure {

    MERIDIAN,
    ECLIPTIC,
    EQUATORIAL_GRID,
    HORIZON,
    CONSTELLATION_BOUNDARIES,
    CONSTELLATION_FIGURES;

    /**
     * This structure as a stable lowercase token - the form metadata
     * and resource keys carry, e.g. {@code equatorial-grid}.
     */
    public String token() {
        return name().toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-');
    }

    /**
     * The active set as one deterministic string: tokens in this
     * enum's own order, joined with {@code +} - and a single target
     * is the bare token, so one-target metadata is byte-identical
     * to the released #361 form (multiple-emphasis ruling).
     * Empty means none, and callers write nothing.
     */
    public static java.util.Optional<String> joinedTokens(
            java.util.Set<ChartStructure> active) {
        if (active == null || active.isEmpty()) {
            return java.util.Optional.empty();
        }
        StringBuilder joined = new StringBuilder();
        for (ChartStructure structure : values()) {
            if (active.contains(structure)) {
                if (joined.length() > 0) {
                    joined.append('+');
                }
                joined.append(structure.token());
            }
        }
        return java.util.Optional.of(joined.toString());
    }

    /**
     * The structure a module's contributed identity belongs to, or
     * empty for an identity the chart does not know.
     *
     * <p>The mapping is the chart's, not the modules': the meridian
     * line and the zenith read as one structure, the horizon and its
     * cardinal landmarks as another, the ecliptic and its four
     * seasonal landmarks as a third. Cardinal identities are mapped
     * although this tree does not yet contribute them (#359 does),
     * so the two features compose without either learning the other.
     */
    public static Optional<ChartStructure> ofIdentity(String identity) {
        return switch (identity) {
            case "meridian", "zenith" -> Optional.of(MERIDIAN);
            case "horizon", "cardinal-north", "cardinal-east",
                    "cardinal-south", "cardinal-west"
                    -> Optional.of(HORIZON);
            case "ecliptic", "march-equinox", "september-equinox",
                    "june-solstice", "december-solstice"
                    -> Optional.of(ECLIPTIC);
            default -> Optional.empty();
        };
    }
}
