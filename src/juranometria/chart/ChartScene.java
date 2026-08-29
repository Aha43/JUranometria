package juranometria.chart;

import java.util.List;

/**
 * A complete, immutable description of one chart to draw: the viewport, the
 * catalogue objects inside it, and the facts the title block states. Scenes
 * are assembled outside painting; the renderer only consumes them.
 */
public record ChartScene(ChartViewport viewport, List<Star> stars,
                         List<DeepSkyObject> deepSkyObjects,
                         String title, double limitingMagnitude,
                         String targetIdentity) {

    /** A scene without a named target. */
    public ChartScene(ChartViewport viewport, List<Star> stars,
                      List<DeepSkyObject> deepSkyObjects,
                      String title, double limitingMagnitude) {
        this(viewport, stars, deepSkyObjects, title, limitingMagnitude, null);
    }

    public ChartScene {
        if (viewport == null) {
            throw new IllegalArgumentException("scene viewport must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("scene title must not be blank");
        }
        if (!Double.isFinite(limitingMagnitude)) {
            throw new IllegalArgumentException(
                    "limiting magnitude must be finite: " + limitingMagnitude);
        }
        stars = List.copyOf(stars);
        deepSkyObjects = List.copyOf(deepSkyObjects);
    }
}
