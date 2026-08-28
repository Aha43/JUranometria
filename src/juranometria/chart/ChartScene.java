package juranometria.chart;

import java.util.List;

/**
 * A complete, immutable description of one chart to draw: the viewport and
 * the catalogue objects inside it. Scenes are assembled outside painting;
 * the renderer only consumes them.
 */
public record ChartScene(ChartViewport viewport, List<Star> stars) {

    public ChartScene {
        if (viewport == null) {
            throw new IllegalArgumentException("scene viewport must not be null");
        }
        stars = List.copyOf(stars);
    }
}
