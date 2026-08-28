package juranometria.project;

/**
 * A point on the tangent chart plane in standard coordinates (radian-scaled,
 * dimensionless): {@code xiEast} grows toward east, {@code etaNorth} toward
 * north. The plane origin is the projection centre.
 */
public record PlanePoint(double xiEast, double etaNorth) {
}
