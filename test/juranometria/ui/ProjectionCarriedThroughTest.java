package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.ui.SceneAssembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart draws by the projection its state chose (issue #297).
 *
 * <p>An interface two implementations satisfy in isolation proves
 * nothing about a chart: what matters is whether the choice
 * <em>travels</em> - from the state a reader holds, into the scene,
 * through the assembler that fetches the sky and the renderer that
 * draws it, without anything along the way assuming which one it is.
 *
 * <p>No reader can choose the overview yet, and this does not give
 * them one: that is issue #299, and this issue stops before reader
 * controls. What it establishes is that the road exists and carries
 * traffic, so #299 has only to open it.
 */
class ProjectionCarriedThroughTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);

    /** The narrowest rung the overview draws, and the widest the
     *  atlas's own does: the two sides of the ladder's one seam. */
    private static final double OVERVIEW = 60.0;
    private static final double SHEET = 42.0;

    private static ChartScene sceneDrawnBy(ChartProjection kind,
                                           double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(ORION, field, 6.0, null, null, kind),
                900, 700);
    }

    @Test
    void theStateCarriesItsProjectionIntoTheScene() {
        assertEquals(ChartProjection.STEREOGRAPHIC,
                sceneDrawnBy(ChartProjection.STEREOGRAPHIC, OVERVIEW)
                        .viewport().projection(),
                "the scene is drawn by the projection the state chose");
        assertEquals(ChartProjection.GNOMONIC,
                sceneDrawnBy(ChartProjection.GNOMONIC, 42.0)
                        .viewport().projection(),
                "and a released state is drawn by the atlas's own");
    }

    @Test
    void aStateThatSaysNothingIsDrawnByTheAtlasesOwnProjection() {
        // Every released state is one of these. The default is not a
        // convenience: it is the promise that adding a second
        // projection changed no page that existed.
        assertEquals(ChartProjection.GNOMONIC,
                ChartViewState.DEFAULT.projection(),
                "the chart the atlas opens with");
        assertEquals(ChartProjection.GNOMONIC,
                new ChartViewState(ORION, 8.0, 6.0).projection(),
                "and any state written without one");
        assertEquals(ChartProjection.GNOMONIC,
                new ChartViewport(ORION, 8.0, 900, 700).projection(),
                "and any viewport written without one");
    }

    @Test
    void everyOrdinaryTransitionKeepsTheProjectionTheReaderIsLookingAt() {
        // A review found this, and it is the sharp edge of the
        // defaulting constructor: a transition written against the
        // five-argument form says "gnomonic" without being asked,
        // and none of these asked. A chart that reverted on a zoom
        // would still draw a page, still be centred where the reader
        // left it, and still be wrong.
        //
        // The overview is a page a reader can reach now (#299), so
        // this is no longer a mechanism held up for inspection. What
        // it holds is the rule read twice: a transition that keeps
        // the field keeps the projection, and a transition that
        // changes the field takes the new field's.
        ChartViewState overview = new ChartViewState(ORION, 90.0, 6.0);
        assertEquals(ChartProjection.STEREOGRAPHIC, overview.projection(),
                "a 90-degree page is the overview's");

        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.increaseMagnitudeLimit().projection(),
                "reaching fainter");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.decreaseMagnitudeLimit().projection(),
                "and brighter");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.recenteredAt(new SkyPosition(90.0, 10.0))
                        .projection(),
                "recentring on a position");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.recenteredAt(new SkyPosition(90.0, 10.0),
                        "a target", "NGC 1").projection(),
                "recentring on a target");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.zoomOut().projection(),
                "and a wider rung is still the overview's");

        // And the seam of the ladder, which is the one step where it
        // changes: 60 degrees is the overview's narrowest and 42 is
        // the atlas's own widest.
        assertEquals(ChartProjection.GNOMONIC,
                overview.zoomIn().zoomIn().projection(),
                "zooming in through 60 reaches the sheet page");
        assertEquals(SHEET,
                overview.zoomIn().zoomIn().fieldWidthDegrees());
        assertEquals(ChartProjection.GNOMONIC,
                overview.withFieldWidth(24.0).projection(),
                "and changing field outright takes the new field's");

        // Home is the one transition that is meant to change it,
        // because Home is not a transition from this chart at all -
        // it is the chart the atlas opens with.
        assertEquals(ChartProjection.GNOMONIC, overview.reset().projection(),
                "Home returns the atlas's own chart, projection and"
                        + " all");
    }

    @Test
    void anOverviewPageIsNotCappedByTheTangentPlanesOwnCorner() {
        // A review found a 120-degree overview page calculating to
        // zero pixels tall. The corner cap was a constant written for
        // the tangent plane and applied through whichever projection
        // was drawing, and at 120 degrees the overview's half-field
        // and that cap are the same plane distance - so the height
        // came out as the square root of nothing.
        //
        // It was tighter everywhere, not only broken at 120: every
        // overview page came out shorter than a tangent-plane page
        // of the same field, which is backwards, since the whole
        // reason for this projection is that it holds up further out.
        SceneAssembler assembler = Atlas.assembler();
        SkyPosition centre = ORION;
        for (double field : new double[] {42.0, 60.0, 90.0, 120.0}) {
            int overview = assembler.maxPageHeightPx(
                    ChartProjection.STEREOGRAPHIC, centre, field, 900);
            assertTrue(overview >= 700,
                    "an overview page at " + field + " degrees is at"
                            + " least a window tall: " + overview + " px");
            int tangent = assembler.maxPageHeightPx(
                    ChartProjection.GNOMONIC, centre, field, 900);
            assertTrue(overview > tangent,
                    "and taller than the tangent plane's at the same"
                            + " field, which is the point of it: "
                            + overview + " against " + tangent);
        }

        // The tangent plane keeps its own answer, unchanged: 120
        // degrees is past where it stops being worth reading, and it
        // still says so.
        assertEquals(0, assembler.maxPageHeightPx(
                        ChartProjection.GNOMONIC, centre, 120.0, 900),
                "a 120-degree tangent-plane page is still refused");
    }

    @Test
    void eachProjectionSaysHowFarItIsWorthReading() {
        // The two caps allow the same distortion of distance - a
        // corner degree four times a centre degree - and the
        // conformal one throws in exact shape. That is the
        // derivation, not a preference.
        assertEquals(60.0, Projections.of(ChartProjection.GNOMONIC, ORION)
                        .usefulCornerDegrees(),
                "the tangent plane's corner, unchanged since the atlas"
                        + " began");
        assertEquals(120.0, Projections
                        .of(ChartProjection.STEREOGRAPHIC, ORION)
                        .usefulCornerDegrees(),
                "and the overview's, at the same scale budget");

        // The globe's cap is not a distortion budget and cannot be
        // one. Its radial scale goes to zero at the limb rather than
        // growing, so there is no angle out at which a corner degree
        // is four times a centre degree - the ratio at its cap is a
        // quarter of nothing. What #301 settled instead is that the
        // whole hemisphere is the useful reach, because a globe page
        // is a bounded disc and its corners are paper rather than
        // sky (docs/decisions/celestial-globe.md).
        assertEquals(90.0, Projections.of(ChartProjection.ORTHOGRAPHIC, ORION)
                        .usefulCornerDegrees(),
                "the globe's useful reach is its own limb");

        for (ChartProjection kind : ChartProjection.values()) {
            Projection projection = Projections.of(kind, ORION);
            double corner = projection.usefulCornerDegrees();
            assertTrue(corner <= projection.limitDegrees(),
                    kind + ": and the cap is inside what it can show");
            if (Double.isFinite(projection.visiblePlaneRadius())) {
                assertEquals(projection.limitDegrees(), corner,
                        kind + ": a bounded page is useful to its own"
                                + " edge, where the sky stops");
                // Measured just inside the limb, because at the limb
                // itself the outward difference has no sky to land
                // on: the scale does not grow toward a bound here, it
                // collapses to nothing at it, which is what the edge
                // of a sphere looks like.
                assertTrue(radialScaleAt(projection, corner - 0.01)
                                / radialScaleAt(projection, 1.0e-4)
                                < 0.02,
                        kind + ": its scale collapses at the limb"
                                + " rather than growing");
                continue;
            }
            double centreScale = radialScaleAt(projection, 1.0e-4);
            double cornerScale = radialScaleAt(projection, corner);
            assertEquals(4.0, cornerScale / centreScale, 0.01,
                    kind + ": a corner degree is four times a centre"
                            + " degree at the cap");
        }
    }

    /** How fast the plane stretches, radially, at an angle out. */
    private static double radialScaleAt(Projection projection,
                                        double degrees) {
        double step = 1.0e-6;
        return (projection.planeRadius(degrees + step)
                - projection.planeRadius(degrees - step))
                / (2.0 * Math.toRadians(step));
    }

    @Test
    void everyRungIsPairedWithTheProjectionThatDrawsIt() {
        // The pairing #297 deliberately did not make, because the
        // rungs that need a second projection did not exist. Every
        // released field is still the atlas's own, and the three
        // rungs above the sheet page are the overview's.
        for (double field : ChartViewState.fieldWidthSteps()) {
            ChartProjection expected;
            if (field >= ChartProjection.WHOLE_HEMISPHERE_DEGREES) {
                expected = ChartProjection.ORTHOGRAPHIC;
            } else if (field > SHEET) {
                expected = ChartProjection.STEREOGRAPHIC;
            } else {
                expected = ChartProjection.GNOMONIC;
            }
            assertEquals(expected,
                    new ChartViewState(ORION, field, 5.0).projection(),
                    field + " degrees");
        }
        // Named at the three boundaries rather than left to the loop:
        // 42 is the atlas's own widest, 120 is still the overview's,
        // and 180 is the globe's (#329).
        assertEquals(ChartProjection.GNOMONIC,
                ChartProjection.forField(42.0));
        assertEquals(ChartProjection.STEREOGRAPHIC,
                ChartProjection.forField(120.0));
        assertEquals(ChartProjection.ORTHOGRAPHIC,
                ChartProjection.forField(180.0));
        // And it is not a preference a caller can override.
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewState(ORION, 8.0, 6.0, null, null,
                        ChartProjection.STEREOGRAPHIC),
                "there is no projection menu, now or later");
    }

    @Test
    void aTransitionOfAGnomonicChartStaysGnomonic() {
        // The other half, and the one every released page depends
        // on: nothing above turned a released chart into an overview
        // by accident either.
        ChartViewState released = ChartViewState.DEFAULT;
        for (ChartViewState after : List.of(released.zoomIn(),
                released.zoomOut(), released.increaseMagnitudeLimit(),
                released.decreaseMagnitudeLimit(),
                released.withFieldWidth(24.0),
                released.recenteredAt(ORION))) {
            assertEquals(ChartProjection.GNOMONIC, after.projection(),
                    "a released chart stays the atlas's own");
        }
    }

    @Test
    void theRendererDrawsADifferentPageForADifferentProjection() {
        // The point of the whole issue, put as a reader would see it:
        // two charts of the same centre, field and sky, drawn by
        // different projections, are different pages. If they came
        // out the same, the choice would be travelling nowhere.
        ChartOptions options = ChartOptions.DEFAULTS;
        BufferedImage tangent = draw(sceneDrawnBy(
                ChartProjection.GNOMONIC, 42.0), options);
        BufferedImage overview = draw(sceneDrawnBy(
                ChartProjection.STEREOGRAPHIC, OVERVIEW), options);

        int differing = 0;
        for (int y = 0; y < 700; y += 3) {
            for (int x = 0; x < 900; x += 3) {
                if (tangent.getRGB(x, y) != overview.getRGB(x, y)) {
                    differing++;
                }
            }
        }
        assertTrue(differing > 500, "the two pages differ where the"
                + " projections differ: " + differing + " sampled"
                + " pixels");
    }

    @Test
    void everyMarkOnAnOverviewPageIsWhereItsProjectionPutsIt() {
        // Not merely different - right. Every mark the renderer drew
        // is checked against the projection the scene names, so a
        // page drawn by one projection and placed by another would
        // fail here rather than merely look odd.
        ChartScene scene = sceneDrawnBy(ChartProjection.STEREOGRAPHIC, OVERVIEW);
        Projection projection = Projections.forViewport(scene.viewport());
        var mapping = new juranometria.project.ViewportMapping(juranometria.project.DrawnPage.of(scene));
        List<ChartRenderer.DrawnMark> marks =
                new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(scene, ChartOptions.DEFAULTS);

        int checked = 0;
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.star() == null) {
                continue;
            }
            PlanePoint plane = projection.project(mark.star().position())
                    .orElseThrow();
            var expected = mapping.toPixel(plane);
            assertEquals(expected.x(), mark.centre().x(), 1.0e-9,
                    mark.star().id() + " is where the overview puts it");
            assertEquals(expected.y(), mark.centre().y(), 1.0e-9,
                    "in both directions");
            checked++;
        }
        assertTrue(checked > 100,
                "a page's worth of marks: " + checked);
    }

    /**
     * A field each projection actually draws.
     *
     * <p>Not one field for all three: a projection is paired with the
     * rungs it draws, and asking the globe about a 60-degree page
     * asks it about a page no reader can be on.
     */
    private static double fieldFor(ChartProjection kind) {
        return switch (kind) {
            case GNOMONIC -> SHEET;
            case STEREOGRAPHIC -> OVERVIEW;
            case ORTHOGRAPHIC -> 180.0;
        };
    }

    @Test
    void pointingAtThePageReadsItBackThroughTheSameProjection() {
        // A review found the pointer scaled by one projection and
        // inverted by another: the plane point came from the
        // viewport's own scale, and the sky position came back
        // through a tangent plane whatever page the reader was
        // looking at. Hit testing, pan press and pointer zoom all
        // read the wrong sky, and none of them looked wrong.
        //
        // Held as a round trip through the chart: what the projection
        // puts at a pixel is what pointing at that pixel returns.
        for (ChartProjection kind : ChartProjection.values()) {
            ChartScene scene = sceneDrawnBy(kind, fieldFor(kind));
            Projection projection =
                    Projections.forViewport(scene.viewport());
            var mapping = new juranometria.project.ViewportMapping(juranometria.project.DrawnPage.of(scene));
            int checked = 0;
            for (int x = 150; x <= 750; x += 150) {
                for (int y = 100; y <= 600; y += 125) {
                    SkyPosition pointed =
                            juranometria.render.ChartHitTest.skyAt(scene,
                                    x, y);
                    if (pointed == null) {
                        // A bounded page has paper outside its limb,
                        // and pointing there is pointing past the
                        // edge of the world. The positive half of
                        // that rule is checked below: it happens on
                        // a globe and on no other page.
                        assertTrue(juranometria.project.DrawnPage
                                        .of(scene).bounded(),
                                kind + " lost the sky at " + x + ","
                                        + y + " and its sky has no"
                                        + " edge to lose it at");
                        continue;
                    }
                    var back = mapping.toPixel(projection
                            .project(pointed).orElseThrow());
                    assertEquals(x, back.x(), 1.0e-6,
                            kind + ": pointing at " + x + "," + y
                                    + " reads back the sky this page"
                                    + " draws there");
                    assertEquals(y, back.y(), 1.0e-6, "and its row");
                    checked++;
                }
            }
            assertTrue(checked >= (kind == ChartProjection.ORTHOGRAPHIC
                            ? 12 : 25),
                    kind + ": a grid of pointings: " + checked);
            if (kind == ChartProjection.ORTHOGRAPHIC) {
                assertTrue(checked < 25,
                        "and some of that grid is paper rather than"
                                + " sky, which is what a bounded page"
                                + " means: " + checked);
            }
        }
    }

    @Test
    void pointerZoomKeepsTheStarUnderThePointerOnEitherProjection() {
        // The pointer-zoom scale was a ratio of tangents, which is
        // the tangent plane's answer given for every projection - in
        // the one place a reader notices most, because that ratio is
        // exactly what keeps the star under the pointer while the
        // field changes. Held by the promise itself rather than by
        // the formula.
        for (ChartProjection kind : ChartProjection.values()) {
            Projection projection = Projections.of(kind, ORION);
            double from = 24.0;
            double to = 12.0;
            // The controller's own number, checked the other way
            // round: the ratio of the scales two viewports actually
            // draw at. A wrong formula agrees with itself; it does
            // not agree with the pages.
            double scale = ChartViewController.zoomScale(projection,
                    from, to, 900, 700);

            // A star a third of the way out on the wider page, and
            // where it must sit on the narrower one for the pointer
            // to have held it.
            var wide = new ChartViewport(ORION, from, 900, 700, kind);
            var narrow = new ChartViewport(ORION, to, 900, 700, kind);
            var wideMapping =
                    new juranometria.project.ViewportMapping(wide,
                    Projections.of(wide.projection(), wide.centre()));
            var narrowMapping =
                    new juranometria.project.ViewportMapping(narrow,
                    Projections.of(narrow.projection(), narrow.centre()));
            assertEquals(wideMapping.pixelsPerPlaneUnit()
                            / narrowMapping.pixelsPerPlaneUnit(),
                    scale, 1.0e-12,
                    kind + ": the zoom scale is the ratio of the scales"
                            + " the two pages are drawn at");
            PlanePoint pointer = new PlanePoint(
                    projection.planeRadius(from / 2.0) / 3.0, 0.0);
            assertEquals(
                    wideMapping.toPixel(pointer).x(),
                    narrowMapping.toPixel(new PlanePoint(
                            pointer.xiEast() * scale, 0.0)).x(),
                    1.0e-6,
                    kind + ": the scale between two fields is the one"
                            + " that leaves a point where it was");

            // And the controller's own solver uses it, where it can
            // be asked. Asserting the formula on its own would only
            // say the formula is right, not that the thing which
            // zooms employs it - the same tautology as checking a
            // renderer against the factory it used.
            SkyPosition under = juranometria.project.PanSolver
                    .skyFromPlane(kind, ORION, pointer);
            PlanePoint after = new PlanePoint(
                    pointer.xiEast() * scale, pointer.etaNorth() * scale);
            // The pan centre solver answers for both projections
            // now (#299). It solved the tangent plane's own
            // equations and refused everything else, naming this
            // issue; what generalised it was noticing that the two
            // quantities written in the tangent plane's units - the
            // cosine of the angle from the centre, and that offset's
            // eastward part - are things every azimuthal projection
            // can be asked for.
            SkyPosition moved = ChartViewController
                    .solveExactReversible(kind, kind, ORION, from, to,
                            pointer, 900, 700)
                    .orElseThrow(() -> new AssertionError(
                            kind + ": a reversible pointer zoom"));
            // Measured in pixels, which is the unit the atlas
            // makes this promise in and the reason it can keep
            // it: the pan solver accepts a centre whose
            // reprojection lands within 1e-6 plane units, so a
            // residual of about a millionth of a degree is the
            // solver's own tolerance rather than noise. Asserted
            // in degrees at 1e-9 this passed on one platform and
            // failed on another - a stricter promise than the
            // atlas makes, which is not a better test but a
            // flakier one. PointerZoomControllerTest holds the
            // released path to a hundredth of a pixel; so does
            // this.
            double driftDegrees = under.separationDegrees(
                    juranometria.project.PanSolver
                            .skyFromPlane(kind, moved, after));
            double driftPixels = narrowMapping.pixelsPerPlaneUnit()
                    * Math.toRadians(driftDegrees);
            assertTrue(driftPixels < 1.0e-2,
                    kind + ": the star under the pointer is still"
                            + " under it after the zoom: " + driftPixels
                            + " px (" + driftDegrees + " degrees)");
        }

        // And across the one step where the projection changes,
        // which no single-projection check can see. Each page is
        // read by its own: the pointer is a plane point on the page
        // being left and the target is the same pixel on the page
        // being entered, and those are different plane points.
        var sheet = new ChartViewport(ORION, SHEET, 900, 700);
        var overview = new ChartViewport(ORION, OVERVIEW, 900, 700);
        var sheetMapping = new juranometria.project.ViewportMapping(sheet,
                    Projections.of(sheet.projection(), sheet.centre()));
        var overviewMapping =
                new juranometria.project.ViewportMapping(overview,
                    Projections.of(overview.projection(), overview.centre()));
        juranometria.project.PixelPoint corner = new juranometria.project.PixelPoint(780.0, 620.0);
        PlanePoint onSheet = juranometria.project.PanSolver
                .planeFromPixel(sheet, corner);
        SkyPosition star = juranometria.project.PanSolver
                .skyFromPlane(sheet, onSheet);
        SkyPosition after = ChartViewController.solveExactReversible(
                        ChartProjection.GNOMONIC,
                        ChartProjection.STEREOGRAPHIC, ORION, SHEET,
                        OVERVIEW, onSheet, 900, 700)
                .orElseThrow(() -> new AssertionError(
                        "zooming out of the sheet page onto the"
                                + " overview is a reversible step"));
        juranometria.project.PixelPoint landed = new juranometria.project.ViewportMapping(
                new ChartViewport(after, OVERVIEW, 900, 700),
                Projections.of(ChartProjection.STEREOGRAPHIC, after))
                .toPixel(Projections.of(ChartProjection.STEREOGRAPHIC,
                        after).project(star).orElseThrow());
        assertTrue(Math.hypot(landed.x() - corner.x(),
                        landed.y() - corner.y()) < 1.0e-2,
                "the star stays under the pointer across the rung"
                        + " where the projection changes: " + landed
                        + " against " + corner);
        assertTrue(overviewMapping.pixelsPerPlaneUnit()
                        != sheetMapping.pixelsPerPlaneUnit(),
                "and the two pages really are drawn at different"
                        + " scales, so the step is not a no-op");
    }

    @Test
    void thePageIsCentredOnTheChartsOwnCentre() {
        // Checked against the chart rather than against the factory.
        // The test above compares the renderer's marks with the same
        // factory the renderer used, so it agrees with itself even
        // when the factory is wrong - a projection built about a
        // centre a hundredth of a degree off passes it happily. This
        // is the property that does not depend on asking twice: the
        // position at the middle of the chart is at the middle of
        // the page, exactly, whichever projection drew it.
        for (ChartProjection kind : ChartProjection.values()) {
            ChartScene scene = sceneDrawnBy(kind, fieldFor(kind));
            Projection projection =
                    Projections.forViewport(scene.viewport());
            var mapping = new juranometria.project.ViewportMapping(juranometria.project.DrawnPage.of(scene));
            var middle = mapping.toPixel(projection
                    .project(scene.viewport().centre()).orElseThrow());
            assertEquals(450.0, middle.x(), 1.0e-9,
                    kind + ": the chart's centre is the page's centre");
            assertEquals(350.0, middle.y(), 1.0e-9,
                    "in both directions");
        }
    }

    @Test
    void theOverviewAndTheTangentPlaneDisagreeAboutWhereAStarGoes() {
        // The mutation this guards: a renderer that fetched the
        // wrong projection would still draw a page, and every mark
        // on it would be in the wrong place by an amount no eye
        // catches at the centre. They agree there, and must not
        // agree at the edge.
        ChartScene scene = sceneDrawnBy(ChartProjection.STEREOGRAPHIC, OVERVIEW);
        SkyPosition nearTheEdge = new SkyPosition(
                ORION.raDegrees() + 20.0, 12.0);
        PlanePoint overview = Projections
                .of(ChartProjection.STEREOGRAPHIC, ORION)
                .project(nearTheEdge).orElseThrow();
        PlanePoint tangent = Projections
                .of(ChartProjection.GNOMONIC, ORION)
                .project(nearTheEdge).orElseThrow();
        assertNotEquals(tangent.xiEast(), overview.xiEast(),
                "the two projections put an edge star in different"
                        + " places, so drawing by the wrong one is a"
                        + " visible mistake");
        assertTrue(Math.abs(tangent.xiEast() - overview.xiEast()) > 1.0e-3,
                "and by a margin, not a rounding: "
                        + Math.abs(tangent.xiEast() - overview.xiEast()));
        assertEquals(ChartProjection.STEREOGRAPHIC,
                scene.viewport().projection(),
                "while the scene still names the one it chose");
    }

    private static BufferedImage draw(ChartScene scene,
                                      ChartOptions options) {
        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, scene, options);
        } finally {
            g.dispose();
        }
        return image;
    }
}
