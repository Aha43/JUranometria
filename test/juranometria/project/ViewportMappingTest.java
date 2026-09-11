package juranometria.project;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewportMappingTest {

    /** Absolute tolerance for pixel coordinates. */
    static final double PIXEL_TOLERANCE = 1e-9;

    static final SkyPosition M31 = new SkyPosition(10.684708, 41.268750);
    static final ChartViewport VIEWPORT = new ChartViewport(M31, 8.0, 800, 600);

    @Test
    void chartCentreMapsToPixelCentre() {
        PixelPoint pixel = new ViewportMapping(VIEWPORT, juranometria.project.Projections.of(VIEWPORT.projection(), VIEWPORT.centre())).toPixel(new PlanePoint(0.0, 0.0));
        assertEquals(400.0, pixel.x(), PIXEL_TOLERANCE);
        assertEquals(300.0, pixel.y(), PIXEL_TOLERANCE);
    }

    @Test
    void halfFieldEastMapsToLeftEdge() {
        double halfFieldPlane = Math.tan(Math.toRadians(4.0));
        PixelPoint pixel = new ViewportMapping(VIEWPORT, juranometria.project.Projections.of(VIEWPORT.projection(), VIEWPORT.centre())).toPixel(new PlanePoint(halfFieldPlane, 0.0));
        assertEquals(0.0, pixel.x(), PIXEL_TOLERANCE);
        assertEquals(300.0, pixel.y(), PIXEL_TOLERANCE);
    }

    @Test
    void eastIsLeftAndNorthIsUp() {
        // Regression guard for the atlas orientation: a sky position east and
        // north of centre must land left of and above the pixel centre.
        ViewportMapping mapping = new ViewportMapping(VIEWPORT, juranometria.project.Projections.of(VIEWPORT.projection(), VIEWPORT.centre()));
        GnomonicProjection projection = new GnomonicProjection(M31);
        PixelPoint pixel = mapping.toPixel(projection.project(
                new SkyPosition(M31.raDegrees() + 1.0, M31.decDegrees() + 1.0)).orElseThrow());
        assertTrue(pixel.x() < 400.0, "east must be left of the pixel centre");
        assertTrue(pixel.y() < 300.0, "north must be above the pixel centre");
    }

    @Test
    void verticalScaleMatchesHorizontalScale() {
        ViewportMapping mapping = new ViewportMapping(VIEWPORT, juranometria.project.Projections.of(VIEWPORT.projection(), VIEWPORT.centre()));
        PixelPoint east = mapping.toPixel(new PlanePoint(0.01, 0.0));
        PixelPoint north = mapping.toPixel(new PlanePoint(0.0, 0.01));
        assertEquals(400.0 - east.x(), 300.0 - north.y(), PIXEL_TOLERANCE);
    }

    @Test
    void rejectsFieldsTooWideForGnomonicCharts() {
        // Named, because a viewport that says nothing now takes its
        // field's own projection and 180 degrees is the overview's -
        // which draws it, and is refused for a different reason.
        assertThrows(IllegalArgumentException.class,
                () -> new ViewportMapping(new ChartViewport(M31, 180.0,
                        800, 600, juranometria.chart.ChartProjection.GNOMONIC),
                        Projections.of(
                                juranometria.chart.ChartProjection.GNOMONIC,
                                M31)));
    }

    // ---- the globe frame reaches only a page that IS the globe ----

    /** A bounded projection: a limb at ninety degrees, radius one. */
    private static Projection bounded(double limitDegrees) {
        return new Projection() {
            @Override
            public String name() {
                return "bounded";
            }

            @Override
            public SkyPosition centre() {
                return M31;
            }

            @Override
            public java.util.Optional<PlanePoint> project(
                    SkyPosition position) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<SkyPosition> unproject(
                    PlanePoint point) {
                return java.util.Optional.empty();
            }

            @Override
            public double planeRadius(double angleDegrees) {
                return Math.sin(Math.toRadians(angleDegrees));
            }

            @Override
            public double angleAtPlaneRadius(double planeRadius) {
                return Math.toDegrees(Math.asin(planeRadius));
            }

            @Override
            public double limitDegrees() {
                return limitDegrees;
            }

            @Override
            public double usefulCornerDegrees() {
                return limitDegrees;
            }

            @Override
            public double visiblePlaneRadius() {
                return 1.0;
            }

            @Override
            public java.util.Optional<juranometria.project.PlaneConic>
                    greatCircle(SkyPosition pole) {
                return java.util.Optional.empty();
            }
        };
    }

    @Test
    void aWholeHemisphereIsADiscPlacedOnThePaper() {
        // The page IS the bounded object, so the frame rule applies:
        // the disc fills a stated fraction of the short side and the
        // rest of the page is paper.
        var whole = new ViewportMapping(
                new ChartViewport(M31, 180.0, 1200, 800,
                        juranometria.chart.ChartProjection.STEREOGRAPHIC),
                bounded(90.0));
        double discRadiusPx = whole.pixelsPerPlaneUnit() * 1.0;
        assertEquals(0.90 * 800 / 2.0, discRadiusPx, 0.5,
                "the disc is ninety per cent of the short side, so it"
                        + " leaves a border on every edge instead of"
                        + " being flattened against two of them");
    }

    @Test
    void aNarrowerBoundedPageIsStillAnOrdinaryPage() {
        // Review of #335. "Bounded" alone made the field stop
        // affecting the scale, so a page saying 120 degrees was drawn
        // at the scale of the full limb and put its own 60-degree
        // edge nowhere near the frame it claimed. A projection with
        // an edge is not the same thing as a page that reaches it.
        var narrow = new ViewportMapping(
                new ChartViewport(M31, 120.0, 1200, 800,
                        juranometria.chart.ChartProjection.STEREOGRAPHIC),
                bounded(90.0));
        double halfFieldPx =
                narrow.pixelsPerPlaneUnit() * Math.sin(Math.toRadians(60.0));
        assertEquals(1200 / 2.0, halfFieldPx, 0.5,
                "half its field is half its width, which is the rule"
                        + " every other page in the atlas is sized by");

        var whole = new ViewportMapping(
                new ChartViewport(M31, 180.0, 1200, 800,
                        juranometria.chart.ChartProjection.STEREOGRAPHIC),
                bounded(90.0));
        assertTrue(narrow.pixelsPerPlaneUnit()
                        > whole.pixelsPerPlaneUnit(),
                "and a narrower page is drawn larger, not identically:"
                        + " the field has to reach the scale at all");
    }
}
