package juranometria.project;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A great circle does not blink (Sprint 32, issue #331, step four).
 *
 * <p>Owner testing found the ecliptic and the meridian disappearing as
 * a globe was turned. The cause was not in either module and not in
 * the clipping: the curve was <em>generated every time</em> and then
 * thrown away here.
 *
 * <p>A curve that crosses the page's boundary nowhere is wholly on the
 * page or wholly off it, so one point of it settles the question - and
 * the point asked was at angle zero. On a hemisphere that is the worst
 * one available. A great circle projects to an ellipse inscribed in the
 * limb, touching it exactly at the two ends of its major axis, and
 * angle zero is one of them. The sample sat 405.000000000 page units
 * from the middle, the sky stopped at 405.000000000, and the sign of
 * the difference between them - a tenth of a picometre - decided
 * whether the line was drawn.
 *
 * <p>The measured cost, swept over 132 orientations before the repair:
 * the ecliptic vanished from 58 of them, the meridian from 43, the
 * mathematical horizon from 48.
 *
 * <p>The law these hold is stronger than "usually drawn". Two great
 * circles on a sphere always meet, and the edge of a hemisphere is
 * itself a great circle, so <strong>every great circle crosses every
 * visible hemisphere</strong>. On a globe there is no orientation in
 * which one of them is legitimately absent, and that is what makes a
 * blank a defect rather than a view.
 */
class GreatCircleContinuityTest {

    private static final int SIDE_PX = 900;

    /** The ecliptic's pole, and two more with no relation to it. */
    private static final SkyPosition ECLIPTIC =
            new SkyPosition(270.0, 66.561);

    private static final SkyPosition MERIDIAN = new SkyPosition(90.0, 0.0);

    private static final SkyPosition HORIZON = new SkyPosition(120.0, 35.0);

    @Test
    void everyGreatCircleIsDrawnAtEveryOrientationOfTheGlobe() {
        int drawn = 0;
        int asked = 0;
        StringBuilder lost = new StringBuilder();
        for (SkyPosition pole : List.of(ECLIPTIC, MERIDIAN, HORIZON)) {
            for (double dec = -75.0; dec <= 75.0; dec += 15.0) {
                for (double ra = 0.0; ra < 360.0; ra += 30.0) {
                    SkyPosition centre = new SkyPosition(ra, dec);
                    asked++;
                    if (runsOn(centre, pole).isEmpty()) {
                        lost.append(' ').append(ra).append('/').append(dec);
                    } else {
                        drawn++;
                    }
                }
            }
        }
        assertEquals(asked, drawn,
                "a great circle meets every hemisphere, so none of"
                        + " these orientations may draw nothing. Lost:"
                        + lost);
        assertTrue(asked >= 396,
                "and the sweep really covered the sphere: " + asked
                        + " orientations");
    }

    @Test
    void theCurveWasAlwaysGeneratedEvenWhenItWasNotDrawn() {
        // Where the defect was, stated so that a later repair cannot
        // be aimed at the wrong stage. Clipping a curve that had
        // never been generated would have made the page look clean
        // while leaving the disappearance untouched, so this pins
        // that generation was never the problem.
        for (double dec = -75.0; dec <= 75.0; dec += 15.0) {
            for (double ra = 0.0; ra < 360.0; ra += 30.0) {
                SkyPosition centre = new SkyPosition(ra, dec);
                assertTrue(projectionAt(centre).greatCircle(ECLIPTIC)
                                .isPresent(),
                        "the conic exists at " + ra + "/" + dec);
            }
        }
    }

    @Test
    void anEdgeOnCircleIsADiameterAndNeverAnEmptyCurve() {
        // The pole exactly on the limb. The ellipse has collapsed to
        // a repeated line, and the honest image of a great circle
        // seen edge-on is the diameter it really is - not silence,
        // which would say the sky has no ecliptic here.
        SkyPosition centre = ninetyDegreesFrom(ECLIPTIC);
        PageRegion region = regionAt(centre);

        assertEquals("straight",
                mappingAt(centre).onPage(projectionAt(centre)
                        .greatCircle(ECLIPTIC).orElseThrow(), region).form(),
                "seen edge-on a great circle is a straight line");

        List<CurveRun> runs = runsOn(centre, ECLIPTIC);
        assertEquals(1, runs.size(),
                "and it is drawn, in one piece");
        CurveRun run = runs.get(0);
        double span = Math.hypot(
                run.to().orElseThrow().x() - run.from().orElseThrow().x(),
                run.to().orElseThrow().y() - run.from().orElseThrow().y());
        assertEquals(2.0 * region.limbRadius(), span, 1.0e-6,
                "and it spans the disc: " + span + " px against a"
                        + " diameter of " + 2.0 * region.limbRadius());
    }

    @Test
    void aCircleThatIsItselfTheLimbIsStillDrawn() {
        // The horizon of a page centred on the zenith: a great
        // circle that IS the limb, so no point of it is strictly
        // inside. Measured rather than assumed, this case was never
        // at risk - the circle's radius comes out exactly the limb's
        // and its samples land on it or a hair within, never outside
        // - so this is a guard against that ceasing to be true, and
        // not evidence for the sampling above. The sampling is held
        // by the sweep, where removing it puts 149 orientations back
        // in the dark.
        SkyPosition centre = new SkyPosition(120.0, 35.0);
        List<CurveRun> runs = runsOn(centre, centre);
        assertTrue(!runs.isEmpty(),
                "a great circle whose pole is the page centre is the"
                        + " limb itself, and the limb is on the page");
    }

    @Test
    void anOrdinaryPageStillKeepsItsSilence() {
        // The rule reaches the globe and changes nothing else. On a
        // tangent plane the circle ninety degrees from the centre has
        // no image at all, and silence there is truthful - a line
        // drawn anyway would be a promise the sky has not made.
        SkyPosition centre = new SkyPosition(266.0, -28.0);
        for (double field : new double[] {42.0, 120.0}) {
            ChartViewport viewport = new ChartViewport(centre, field,
                    SIDE_PX, SIDE_PX, ChartProjection.forField(field));
            Projection projection = Projections.of(
                    ChartProjection.forField(field), centre);
            ViewportMapping mapping = new ViewportMapping(viewport,
                    projection);
            assertTrue(GreatCirclePage.clip(projection, mapping,
                            mapping.regionFor(viewport, projection),
                            centre).isEmpty(),
                    field + " degrees: a circle whose pole is the page"
                            + " centre passes nowhere near this page");
        }
    }

    private static List<CurveRun> runsOn(SkyPosition centre,
                                         SkyPosition pole) {
        return GreatCirclePage.clip(projectionAt(centre), mappingAt(centre),
                regionAt(centre), pole);
    }

    private static Projection projectionAt(SkyPosition centre) {
        return Projections.of(ChartProjection.ORTHOGRAPHIC, centre);
    }

    private static ViewportMapping mappingAt(SkyPosition centre) {
        return new ViewportMapping(viewportAt(centre),
                projectionAt(centre));
    }

    private static PageRegion regionAt(SkyPosition centre) {
        return mappingAt(centre).regionFor(viewportAt(centre),
                projectionAt(centre));
    }

    private static ChartViewport viewportAt(SkyPosition centre) {
        return new ChartViewport(centre, 180.0, SIDE_PX, SIDE_PX,
                ChartProjection.ORTHOGRAPHIC);
    }

    /** A page centre exactly ninety degrees from a pole. */
    private static SkyPosition ninetyDegreesFrom(SkyPosition pole) {
        double dec = pole.decDegrees() - 90.0;
        return dec < -90.0
                ? new SkyPosition((pole.raDegrees() + 180.0) % 360.0,
                        -180.0 - dec)
                : new SkyPosition(pole.raDegrees(), dec);
    }
}
