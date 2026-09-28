package juranometria.solar.spk;

/**
 * A rectangular vector, in whatever unit the caller keeps: kilometres
 * for positions, kilometres per second for velocities, or none for a
 * direction.
 *
 * <p>Immutable and exact about what it does; the arithmetic is the
 * arithmetic, and any meaning - which frame, which origin - is the
 * caller's to state in its own types.
 */
public record Vector3(double x, double y, double z) {

    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    public Vector3 plus(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 minus(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 times(double factor) {
        return new Vector3(x * factor, y * factor, z * factor);
    }

    public double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3 cross(Vector3 other) {
        return new Vector3(y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    public double length() {
        return Math.sqrt(dot(this));
    }

    /** This vector scaled to unit length; refuses the zero vector. */
    public Vector3 unit() {
        double length = length();
        if (length == 0.0) {
            throw new ArithmeticException("the zero vector has no direction");
        }
        return times(1.0 / length);
    }

    /** The angle between this vector and another, in degrees. */
    public double angleDegrees(Vector3 other) {
        // atan2 of |a×b| and a·b is exact for tiny and near-180° angles
        // where acos of a cosine loses digits.
        return Math.toDegrees(Math.atan2(cross(other).length(), dot(other)));
    }
}
