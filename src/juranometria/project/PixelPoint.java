package juranometria.project;

/**
 * A chart position in pixel coordinates. The origin is the top-left corner
 * of the drawing surface; x grows rightward and y grows downward. Values may
 * lie outside the viewport bounds; clipping is the renderer's concern.
 */
public record PixelPoint(double x, double y) {
}
