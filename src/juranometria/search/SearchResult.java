package juranometria.search;

import juranometria.chart.SkyPosition;

/**
 * One resolved search result: a human-readable label, a stable catalogue
 * identity, the kind of thing found, its sky position, and a ready-made
 * region title for a chart recentred on it (for example
 * "M 42 \u00b7 Great Orion Nebula region").
 */
public record SearchResult(String label, String identity, Kind kind, SkyPosition position,
                           String regionTitle) {

    public enum Kind {
        DEEP_SKY_OBJECT,
        STAR,
        COORDINATES
    }

    public SearchResult {
        if (label == null || label.isBlank() || identity == null || identity.isBlank()) {
            throw new IllegalArgumentException("label and identity must not be blank");
        }
        if (kind == null || position == null) {
            throw new IllegalArgumentException("kind and position are required");
        }
        if (regionTitle == null || regionTitle.isBlank()) {
            throw new IllegalArgumentException("region title must not be blank");
        }
    }
}
