package juranometria.chart;

import java.util.List;
import java.util.Map;

import juranometria.geo.GeoSegment;

/**
 * The constellation geography one scene carries: the figure and
 * boundary segments intersecting the page's query region, and the
 * Latin names of the constellations whose figures are present. Empty
 * lists mean the scale policy keeps that layer off this page - the
 * scene states exactly what may be drawn, assembled outside painting.
 */
public record SceneGeography(List<GeoSegment> figureSegments,
                             List<GeoSegment> boundarySegments,
                             Map<String, String> latinNames) {

    public static final SceneGeography EMPTY =
            new SceneGeography(List.of(), List.of(), Map.of());

    public SceneGeography {
        figureSegments = List.copyOf(figureSegments);
        boundarySegments = List.copyOf(boundarySegments);
        latinNames = Map.copyOf(latinNames);
    }
}
