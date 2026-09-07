package juranometria.project;

/**
 * What a projection says a great circle is: {@code a x^2 + b xy +
 * c y^2 + d x + e y + f = 0}, in the projection's own plane.
 *
 * <p>One representation for every answer. A line has no quadratic
 * part, a circle has {@code a = c} with no {@code xy}, and an ellipse
 * is the general case - and the passage from one to another is a
 * coefficient going through zero, which needs no special case at all.
 *
 * <p>That is the whole reason it is not a centre and a radius. A
 * stereographic great circle whose pole is nearly square to the page
 * centre has a radius of {@code 2/|a|}, which for the ecliptic seen
 * from the vernal equinox is 2.8e16 - because {@code cos(270
 * degrees)} is not zero in a double, it is -1.8e-16. A form that has
 * to be rescued by a tolerance near its degenerate case, and cannot
 * be rescued by an equality either, is the wrong form. These
 * coefficients stay finite and well conditioned straight through.
 */
public record PlaneConic(double a, double b, double c, double d,
                         double e, double f) {

    /** A line, which is a conic with no quadratic part. */
    public static PlaneConic line(double along, double east, double north) {
        return new PlaneConic(0.0, 0.0, 0.0, east, north, along);
    }

    public double at(double x, double y) {
        return a * x * x + b * x * y + c * y * y + d * x + e * y + f;
    }

    /** How fast the conic changes, which is how far a miss is. */
    public double[] gradientAt(double x, double y) {
        return new double[] {2.0 * a * x + b * y + d,
                b * x + 2.0 * c * y + e};
    }

    /** How much the quadratic part can contribute over a distance. */
    public double curvatureOver(double reach) {
        return (Math.abs(a) + Math.abs(b) + Math.abs(c)) * reach * reach;
    }
}
