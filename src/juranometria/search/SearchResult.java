package juranometria.search;

import juranometria.chart.SkyPosition;

/**
 * One resolved search result: a human-readable label, a stable catalogue
 * identity, the kind of thing found, and its sky position.
 */
public record SearchResult(String label, String identity, Kind kind, SkyPosition position) {

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
    }
}
