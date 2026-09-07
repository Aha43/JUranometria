package juranometria.tool.overview;

/**
 * What a projection says a great circle is (issue #296).
 *
 * <p>{@code a x^2 + b xy + c y^2 + d x + e y + f = 0}, in the
 * projection's own plane. One representation for all three answers:
 * a line has no quadratic part, a circle has {@code a = c} with no
 * {@code xy}, and an ellipse is the general case. It is what the
 * projection returns, and it is deliberately <em>not</em> a drawable
 * form.
 *
 * <p>The reason is the whole of two review findings. Written as a
 * centre and a radius, a stereographic great circle whose pole is
 * nearly square to the page centre has a radius of
 * {@code 2/|a|} - which for the ecliptic seen from the vernal
 * equinox is 2.8e16, because {@code cos(270 degrees)} is not zero in
 * a double, it is -1.8e-16. A form that has to be rescued by a
 * tolerance near its degenerate case, and cannot be rescued by an
 * equality either, is the wrong form. These coefficients stay finite
 * and well conditioned right through the degeneracy: the circle
 * becomes a line by its {@code x^2} coefficient passing through
 * zero, which needs no special case at all.
 *
 * <p>Turning this into something drawable is
 * {@link StudyMapping#onPage}'s work, because it is a question about
 * a page - the same great circle is plainly curved across a
 * hemisphere and plainly straight across a telescope field, and only
 * the page knows which is being drawn.
 */
record PlaneConic(double a, double b, double c, double d, double e,
                  double f) {

    /** A line, which is a conic with no quadratic part. */
    static PlaneConic line(double along, double east, double north) {
        return new PlaneConic(0.0, 0.0, 0.0, east, north, along);
    }

    double at(double x, double y) {
        return a * x * x + b * x * y + c * y * y + d * x + e * y + f;
    }

    /** How fast the conic changes, which is how far a miss is. */
    double[] gradientAt(double x, double y) {
        return new double[] {2.0 * a * x + b * y + d,
                b * x + 2.0 * c * y + e};
    }

    /** How much the quadratic part can contribute over a distance. */
    double curvatureOver(double reach) {
        return (Math.abs(a) + Math.abs(b) + Math.abs(c)) * reach * reach;
    }

    /**
     * The same conic in page coordinates, where
     * {@code x = intoX - scale * xi} and
     * {@code y = intoY - scale * eta}.
     */
    PlaneConic mapped(double scale, double intoX, double intoY) {
        return new PlaneConic(a, b, c,
                -2.0 * a * intoX - b * intoY - scale * d,
                -b * intoX - 2.0 * c * intoY - scale * e,
                a * intoX * intoX + b * intoX * intoY + c * intoY * intoY
                        + scale * d * intoX + scale * e * intoY
                        + scale * scale * f);
    }
}
