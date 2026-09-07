package juranometria.tool.overview;

import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.project.PlanePoint;

/**
 * A candidate projection, as the gate needs to measure it (Sprint 30,
 * issue #296).
 *
 * <p><strong>Study only.</strong> Nothing production draws goes
 * through this. It exists so three candidates can be drawn and
 * measured side by side, and so the gate can propose an interface
 * shaped by what the measuring actually needed rather than by what
 * looked tidy beforehand.
 *
 * <p>The shape here is the gate's proposal for #297. Every method is
 * one the study had to call:
 *
 * <ul>
 *   <li>{@link #project} and {@link #unproject} - the chart needs
 *       both, for drawing and for identifying what a reader clicked;
 *   <li>{@link #planeRadius} - the scale rule, which is
 *       {@code tan(f/2)} for one candidate and something else for
 *       every other, so a viewport cannot keep assuming it;
 *   <li>{@link #limitDegrees} - what the projection can show at all,
 *       which is a hemisphere for two of the three and everything but
 *       one point for the other;
 *   <li>{@link #greatCircle} - what a great circle <em>becomes</em>,
 *       stated by the projection in closed form.
 * </ul>
 *
 * <p>That last one was missing from the gate's first proposal, and a
 * review was right that its absence was the whole problem. Without
 * it a caller can only find out what a curve became by projecting a
 * few hundred points and fitting a form to them - which is sampling,
 * which the issue refuses; then asking which form it got, which is a
 * type check; and then discovering that a projection nobody had
 * studied needed a form nobody had written, which is widening the
 * vocabulary later. A projection knows the answer analytically. It
 * should be asked for it.
 */
public interface StudyProjection {

    /** What a decision document and a title block would call it. */
    String name();

    /** The position the page is centred on. */
    SkyPosition centre();

    /**
     * The plane image of a sky position, or empty when the projection
     * has none - beyond the limb, or at the point it cannot show.
     */
    Optional<PlanePoint> project(SkyPosition position);

    /** The sky position a plane point stands for, or empty. */
    Optional<SkyPosition> unproject(PlanePoint point);

    /**
     * The plane distance from the centre at an angular distance.
     *
     * <p>This is the projection's whole scale rule in one function,
     * and it is the thing {@code ViewportMapping} has been assuming:
     * it divides the page by {@code tan(field/2)}, which is this
     * function for exactly one of the three candidates.
     */
    double planeRadius(double angleDegrees);

    /**
     * The furthest a position can be from the centre and still have
     * an image: 90 for a hemisphere, less than 180 for a projection
     * that only loses the antipode.
     */
    double limitDegrees();

    /**
     * What the great circle with this pole becomes on the plane.
     *
     * <p>In closed form, from the projection's own geometry - not
     * fitted to projected points. Empty when the circle does not
     * appear on this projection at all, which happens under the
     * gnomonic projection when the pole is the centre: that circle
     * lies entirely at ninety degrees, where the tangent plane is
     * infinitely far away.
     */
    Optional<PlaneConic> greatCircle(SkyPosition pole);
}
