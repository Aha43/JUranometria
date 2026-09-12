package juranometria.project;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where an extended object really lands, and what the projection does
 * to its mark (Sprint 32, issue #331).
 *
 * <p>The atlas turns an object's angular axes into page units at the
 * <em>page centre's</em> rate. On every page it could draw before the
 * globe that was near enough; on a hemisphere it is wrong by a factor
 * of seven at the limb, which is how the Large Magellanic Cloud came
 * to be drawn as a grey ellipse half on the sky and half on the
 * paper.
 *
 * <p>This is the geometry that answers instead. It is the gate's own
 * measuring instrument, promoted to production so that the page draws
 * what the study measured rather than the two agreeing by luck.
 */
class SkyFootprintTest {

    private static final SkyPosition PAGE =
            new SkyPosition(266.0, -28.0);

    private static final SkyPosition LMC =
            new SkyPosition(80.894, -69.756);

    private static final double LMC_MAJOR_ARCMIN = 645.0;

    private static final double LMC_MINOR_ARCMIN = 550.0;

    private static final double LMC_POSITION_ANGLE = 170.0;

    private static final int WIDE_PX = 1200;

    private static final int HIGH_PX = 800;

    /**
     * How near the identity the centre's transform comes.
     *
     * <p>Not machine epsilon, and the reason is stated rather than
     * absorbed: the axes are fitted to 360 discrete samples of an
     * outline that is an ellipse on the sphere and only nearly one on
     * the plane. The residual measures 1.0e-5, which on the largest
     * mark the atlas draws is under a thousandth of a pixel.
     */
    private static final double NUMERICALLY_THE_IDENTITY = 1.0e-4;

    @Test
    void atThePageCentreTheProjectionDoesNothingToAMark() {
        var transform = SkyFootprint.foreshortening(globe(), mapping(),
                PAGE, 60.0, 40.0, 30.0);
        assertNotNull(transform);
        assertEquals(0.0, transform.offTheIdentity(),
                NUMERICALLY_THE_IDENTITY,
                "a mark at the centre of a globe is the mark every"
                        + " other page draws");
        assertEquals(1.0, transform.areaFactor(),
                NUMERICALLY_THE_IDENTITY, "and covers the same area");
    }

    @Test
    void aMarkIsForeshortenedByHowFarTheSphereHasTurnedAway() {
        // The area a mark covers falls as the cosine of its angle
        // from the centre, which is what looking at a sphere from
        // outside does. Nothing says so in the code: it comes out of
        // the two measured footprints.
        for (double out : new double[] {15.0, 30.0, 45.0, 60.0, 75.0}) {
            var transform = SkyFootprint.foreshortening(globe(),
                    mapping(), awayFromCentre(out), 30.0, 20.0, 30.0);
            assertNotNull(transform, out + " degrees out is on the page");
            assertEquals(Math.cos(Math.toRadians(out)),
                    transform.areaFactor(), 0.02,
                    "a mark " + out + " degrees out covers cos(" + out
                            + ") of what it would at the centre");
        }
    }

    @Test
    void theCloudIsSquashedAcrossTheRadiusAndNotAlongIt() {
        // The fixture the whole rule exists for. At 82 degrees out
        // the cloud's own axes at the page centre's rate make it very
        // nearly round - 67.5 by 57.6 px - and it spills over the
        // limb onto paper, although the cloud itself ends at 87.5
        // degrees with two and a half to spare.
        SkyFootprint.Extent extent = SkyFootprint.extentOn(globe(),
                mapping(), LMC, LMC_MAJOR_ARCMIN, LMC_MINOR_ARCMIN,
                LMC_POSITION_ANGLE);
        assertNotNull(extent);
        assertTrue(extent.whole(),
                "every point of this cloud's outline is on the visible"
                        + " hemisphere, so what follows is about"
                        + " foreshortening and not about clipping");

        double ordinaryMajor = arcminToPx(LMC_MAJOR_ARCMIN);
        double ordinaryMinor = arcminToPx(LMC_MINOR_ARCMIN);
        assertEquals(0.85, ordinaryMinor / ordinaryMajor, 0.02,
                "sized at the page centre's rate the cloud is nearly"
                        + " round");
        assertTrue(extent.minorPx() / extent.majorPx() < 0.25,
                "and its projected footprint is not: " + extent.majorPx()
                        + " by " + extent.minorPx() + " px");

        // Squashed across the radius by the cosine, and left alone
        // along it - which is the difference between a sphere seen
        // edge-on and a circle drawn smaller.
        double squash = ordinaryMinor / extent.minorPx();
        assertTrue(squash > 5.0 && squash < 8.0,
                "the radial squash is about 1/cos(82) = 7.4: " + squash);
        assertEquals(ordinaryMajor, extent.majorPx(),
                0.2 * ordinaryMajor,
                "while the span across the radius is nearly what it"
                        + " always was");
    }

    @Test
    void aStraddlerKeepsItsVisiblePartAndNothingBehindTheLimb() {
        // An object whose outline really does cross the boundary. Its
        // visible part is real sky and is drawn; dropping the object
        // would be throwing that away, and drawing the whole of it
        // would be inventing sky behind the globe.
        SkyPosition justInside = awayFromCentre(88.0);
        double majorArcmin = 8.0 * 60.0;

        List<PlanePoint> projected = SkyFootprint.projected(globe(),
                justInside, majorArcmin, majorArcmin, 0.0);
        assertTrue(projected.size() > 0 && projected.size() < SkyFootprint.SAMPLES,
                "the fixture straddles rather than clearing the limb"
                        + " or hiding behind it: " + projected.size()
                        + " of " + SkyFootprint.SAMPLES
                        + " outline points are on the page");

        SkyFootprint.Extent extent = SkyFootprint.extentOn(globe(),
                mapping(), justInside, majorArcmin, majorArcmin, 0.0);
        assertNotNull(extent);
        assertTrue(!extent.whole(),
                "and it says so, rather than reporting a whole"
                        + " footprint measured from half of one");

        List<PixelPoint> outline = SkyFootprint.visibleOn(globe(),
                mapping(), justInside, majorArcmin, majorArcmin, 0.0);
        assertTrue(outline.size() > projected.size(),
                "its visible part carries the crossings as well as the"
                        + " samples: " + outline.size() + " points"
                        + " against " + projected.size());
        double limb = mapping().pixelsPerPlaneUnit()
                * globe().visiblePlaneRadius();
        for (PixelPoint point : outline) {
            double from = Math.hypot(point.x() - WIDE_PX / 2.0,
                    point.y() - HIGH_PX / 2.0);
            assertTrue(from <= limb + 1.0e-6,
                    "and every point of it is inside the limb, where"
                            + " the visible sky is: " + from
                            + " against " + limb);
        }
    }

    @Test
    void anObjectBehindTheGlobeHasNoFootprintAtAll() {
        SkyPosition farSide = awayFromCentre(140.0);
        assertTrue(SkyFootprint.projected(globe(), farSide, 30.0, 20.0,
                0.0).isEmpty(),
                "nothing of it projects");
        assertNull(SkyFootprint.extentOn(globe(), mapping(), farSide,
                        30.0, 20.0, 0.0),
                "so it has no extent - which is not the same answer as"
                        + " a very small one");
        assertNull(SkyFootprint.foreshortening(globe(), mapping(),
                        farSide, 30.0, 20.0, 0.0),
                "and no transform, because there is no mark to carry");
    }

    @Test
    void anObjectWithNoRecordedExtentHasNoTransform() {
        assertNull(SkyFootprint.foreshortening(globe(), mapping(), PAGE,
                        0.0, 0.0, 0.0),
                "an object whose catalogue records no axes has no"
                        + " ordinary footprint to carry anywhere");
    }

    private static double arcminToPx(double arcmin) {
        return Math.toRadians(arcmin / 60.0)
                * mapping().pixelsPerPlaneUnit();
    }

    /** A position a stated angle from the page centre, due south. */
    private static SkyPosition awayFromCentre(double degrees) {
        double dec = PAGE.decDegrees() - degrees;
        if (dec < -90.0) {
            return new SkyPosition((PAGE.raDegrees() + 180.0) % 360.0,
                    -180.0 - dec);
        }
        return new SkyPosition(PAGE.raDegrees(), dec);
    }

    private static Projection globe() {
        return Projections.of(ChartProjection.ORTHOGRAPHIC, PAGE);
    }

    private static ViewportMapping mapping() {
        return new ViewportMapping(new ChartViewport(PAGE, 180.0,
                WIDE_PX, HIGH_PX, ChartProjection.ORTHOGRAPHIC),
                globe());
    }
}
