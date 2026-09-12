package juranometria.tool.globe;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.project.PlanePoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An extended object describes a sky footprint, not a scaled symbol
 * (Sprint 32, issue #301).
 *
 * <p>The atlas turns an object's angular axes into page units at the
 * page centre's rate. On every page it could draw until Sprint 32
 * that was near enough. On a globe the radial scale is
 * {@code cos(theta)} - 0.135 at the Large Magellanic Cloud's 82
 * degrees - so the cloud is drawn about seven times too wide across
 * the radius and spills over the limb onto paper, although the cloud
 * itself ends at 87.5 degrees with two and a half degrees to spare.
 *
 * <p>The owner found it by looking at a picture: a grey ellipse half
 * on the sky and half on the paper.
 *
 * <p>Two fixtures, and each guards against the opposite mistake.
 * <strong>The LMC</strong> holds the rule that the footprint is
 * projected rather than scaled - restore centre-scale sizing and its
 * ink crosses the limb, which this fails on. <strong>A genuine
 * straddler</strong> holds the other half: an object that really does
 * cross the boundary keeps its visible part, so "clip what is
 * outside" cannot quietly become "drop the object".
 *
 * <p>Drawing this belongs to #331; any seam production needs, to
 * #329.
 */
class GlobeFootprintTest {

    /** The page these are measured on: the crowded hemisphere. */
    private static final SkyPosition PAGE =
            new SkyPosition(266.0, -28.0);

    /** The limb, in plane units. */
    private static final double LIMB = 1.0;

    private static final SkyPosition LMC =
            new SkyPosition(80.894, -69.756);
    private static final double LMC_MAJOR_ARCMIN = 645.0;
    private static final double LMC_MINOR_ARCMIN = 550.0;
    private static final double LMC_POSITION_ANGLE = 170.0;

    @Test
    void theCloudsFootprintStaysOnTheSkyWhereTheCloudIs() {
        juranometria.project.Projection globe = juranometria.project.Projections.of(
                juranometria.chart.ChartProjection.ORTHOGRAPHIC, PAGE);
        double centreSeparation = PAGE.separationDegrees(LMC);
        assertTrue(centreSeparation > 80.0 && centreSeparation < 85.0,
                "the fixture is the cloud near the limb, and it has"
                        + " moved: " + centreSeparation);

        List<PlanePoint> footprint = Footprint.projected(globe, LMC,
                LMC_MAJOR_ARCMIN, LMC_MINOR_ARCMIN,
                LMC_POSITION_ANGLE);
        assertEquals(Footprint.SAMPLES, footprint.size(),
                "every point of this cloud's outline is on the visible"
                        + " hemisphere - it reaches 87.5 degrees and"
                        + " the limb is at 90");
        assertTrue(Footprint.furthestPlaneRadius(footprint) < LIMB,
                "so its footprint is inside the limb, where the cloud"
                        + " is: "
                        + Footprint.furthestPlaneRadius(footprint));
    }

    @Test
    void scalingTheCloudsAxesAtTheCentreDrawsItOffTheSky() {
        // The mutation, asserted rather than described: this is what
        // the atlas does today, and on this page it is visibly wrong.
        // If a change ever makes centre-scale sizing agree with the
        // projected footprint here, the reason will be worth knowing
        // and this will say so by failing.
        juranometria.project.Projection globe = juranometria.project.Projections.of(
                juranometria.chart.ChartProjection.ORTHOGRAPHIC, PAGE);
        List<PlanePoint> scaled = Footprint.atCentreScale(globe, LMC,
                LMC_MAJOR_ARCMIN, LMC_MINOR_ARCMIN,
                LMC_POSITION_ANGLE);
        double furthest = Footprint.furthestPlaneRadius(scaled);

        assertTrue(furthest > LIMB,
                "scaled about its projected centre the cloud reaches"
                        + " past the limb and is drawn on paper, which"
                        + " is why the globe may not size an extent"
                        + " that way: " + furthest);
        assertTrue(furthest > 1.05,
                "and not marginally: it overruns the edge of the world"
                        + " by several per cent of the whole disc: "
                        + furthest);
    }

    @Test
    void anObjectAcrossTheLimbKeepsThePartThatIsSky() {
        // Four degrees across, centred eighty-nine degrees out: this
        // one really does straddle, and the rule is that the limb
        // clips rather than erases.
        juranometria.project.Projection globe = juranometria.project.Projections.of(
                juranometria.chart.ChartProjection.ORTHOGRAPHIC, PAGE);
        SkyPosition straddling = Footprint.outlineIn(PAGE,
                2.0 * 89.0 * 60.0, 2.0 * 89.0 * 60.0, 0.0).get(0);
        assertTrue(PAGE.separationDegrees(straddling) > 88.0,
                "the fixture sits just inside the limb");

        List<PlanePoint> footprint = Footprint.projected(globe,
                straddling, 240.0, 240.0, 0.0);

        assertTrue(!footprint.isEmpty(),
                "the visible part is real sky and is drawn: an object"
                        + " crossing the limb is clipped, never"
                        + " dropped");
        assertTrue(footprint.size() < Footprint.SAMPLES,
                "and the hidden part is not: some of this outline is"
                        + " on the far side of the globe, where there"
                        + " is nothing to draw");
        assertTrue(Footprint.furthestPlaneRadius(footprint)
                        <= LIMB + 1e-9,
                "nothing it draws reaches past the limb");
    }
}
