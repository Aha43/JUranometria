package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The overview page a reader can reach (Sprint 30, issue #299).
 *
 * <p>The gate settled that the overview is <strong>another rung of
 * the field ladder, not a mode</strong>: zoom, pan, recentre and
 * every module keep working because they are the same operations,
 * and which projection draws a page is a property of its field
 * rather than a setting (docs/decisions/overview-projection.md).
 * That is a claim about the whole surface, so what is held here is
 * the reader's own route through it.
 */
class OverviewPageTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);
    private static final int WIDE = 900;
    private static final int HIGH = 700;

    private static ChartScene sceneAt(double field, double magnitude) {
        return Atlas.assembler().assemble(
                new ChartViewState(ORION, field, magnitude), WIDE, HIGH);
    }

    @Test
    void zoomingOutOfTheDetailedAtlasArrivesOnTheOverviewAndComesBack() {
        // The whole route, in the reader's own transitions. Nothing
        // announces a mode and nothing is switched: the rung changes
        // and the projection comes with it.
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 42.0);
        assertEquals(ChartProjection.GNOMONIC,
                controller.state().projection(),
                "the sheet page is the atlas's own");
        assertFalse(controller.state().overview());

        controller.zoomOut();
        assertEquals(60.0, controller.state().fieldWidthDegrees());
        assertTrue(controller.state().overview(),
                "one step out of the sheet page is the overview");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                controller.state().projection());
        assertEquals(ORION, controller.state().centre(),
                "and the reader is still looking where they were");

        controller.zoomOut();
        controller.zoomOut();
        assertEquals(120.0, controller.state().fieldWidthDegrees(),
                "three rungs, and the widest the sky is offered at");
        assertFalse(controller.state().canZoomOut(),
                "180 degrees is drawable and not readable, so it is"
                        + " not offered");

        controller.zoomIn();
        controller.zoomIn();
        controller.zoomIn();
        assertEquals(42.0, controller.state().fieldWidthDegrees(),
                "and the way back is the same ladder");
        assertEquals(ChartProjection.GNOMONIC,
                controller.state().projection());
        assertEquals(ORION, controller.state().centre(),
                "with the centre the reader kept throughout");
    }

    @Test
    void theControlThatEntersAndLeavesTheOverviewSaysSo() throws Exception {
        // The issue asks for one explicit, accessible control that
        // enters and leaves the overview; the gate ruled out a mode
        // and a projection menu. Both are satisfied by the control
        // that already makes the step saying where it goes - and
        // only at the step where the kind of chart changes, because a
        // button that renamed itself at every rung would be a readout
        // pretending to be a control.
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 42.0);
        java.util.concurrent.atomic.AtomicReference<AtlasToolbar> made =
                new java.util.concurrent.atomic.AtomicReference<>();
        javax.swing.SwingUtilities.invokeAndWait(() -> made.set(
                new AtlasToolbar(controller, new SearchField(
                        new juranometria.search.LocalSearch(List.of(),
                                List.of()),
                        Atlas.assembler(), controller))));
        AtlasToolbar toolbar = made.get();

        javax.swing.JButton out = button(toolbar, "Zoom out");
        javax.swing.JButton in = button(toolbar, "Zoom in");
        assertTrue(out.getToolTipText().contains("overview"),
                "at the sheet page, zoom out says where it leads: "
                        + out.getToolTipText());
        assertEquals(out.getToolTipText(),
                out.getAccessibleContext().getAccessibleDescription(),
                "and says it to assistive technology in the same words");
        assertFalse(in.getToolTipText().contains("overview"),
                "the other direction is an ordinary step here");

        javax.swing.SwingUtilities.invokeAndWait(controller::zoomOut);
        assertTrue(button(toolbar, "Zoom in").getToolTipText()
                        .contains("detailed atlas"),
                "and on the overview, zoom in says how to get back: "
                        + button(toolbar, "Zoom in").getToolTipText());
        assertFalse(button(toolbar, "Zoom out").getToolTipText()
                        .contains("overview"),
                "while a wider rung is an ordinary step again");
    }

    private static javax.swing.JButton button(java.awt.Container root,
                                              String name) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof javax.swing.JButton candidate
                    && name.equals(candidate.getAccessibleContext()
                            .getAccessibleName())) {
                return candidate;
            }
            if (child instanceof java.awt.Container inner) {
                javax.swing.JButton found = button(inner, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void pointingAtAnOverviewPageReadsTheSkyBackThroughItsOwnProjection() {
        // "If I click a star here, can I enter a detailed page centred
        // on it?" - which needs the inverse of the projection that
        // drew the page, not of the one that draws the page it leads
        // to.
        ChartScene overview = sceneAt(120.0,
                ChartViewState.defaultMagnitudeFor(120.0));
        ChartScene sheet = sceneAt(42.0, 6.0);
        double worst = 0.0;
        int checked = 0;
        for (double x : new double[] {60, 250, 450, 700, 860}) {
            for (double y : new double[] {40, 200, 350, 520, 660}) {
                SkyPosition read = ChartHitTest.skyAt(overview, x, y);
                PixelPoint back = new ViewportMapping(overview.viewport())
                        .toPixel(Projections.forViewport(overview.viewport())
                                .project(read).orElseThrow());
                worst = Math.max(worst,
                        Math.hypot(back.x() - x, back.y() - y));
                checked++;

                // And it is genuinely the overview's answer: the
                // tangent plane reads a different sky at the same
                // pixel, which is what makes reading it back through
                // the wrong one a silent fault rather than a visible
                // one.
                if (x != 450 || y != 350) {
                    assertNotEquals(read, ChartHitTest.skyAt(sheet, x, y),
                            "the two pages do not agree about " + x
                                    + "," + y);
                }
            }
        }
        assertEquals(25, checked);
        assertTrue(worst < 1.0e-6, "every pointing round-trips through"
                + " the page's own projection: worst " + worst + " px");
    }

    @Test
    void clickingAnOverviewAndEnteringDetailCentresWhereTheReaderPointed() {
        // The transition the issue asks for by name. The centre comes
        // from the overview's inverse; the page it opens is the
        // detailed atlas's, drawn by the projection that field
        // belongs to.
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 120.0);
        ChartScene overview = Atlas.assembler().assemble(
                controller.state(), WIDE, HIGH);
        SkyPosition clicked = ChartHitTest.skyAt(overview, 700.0, 210.0);

        controller.recenter(clicked);
        while (controller.state().fieldWidthDegrees() > 8.0) {
            controller.zoomIn();
        }
        assertEquals(8.0, controller.state().fieldWidthDegrees());
        assertEquals(ChartProjection.GNOMONIC,
                controller.state().projection(),
                "the detailed page is the atlas's own again");
        assertEquals(clicked, controller.state().centre(),
                "centred exactly where the reader pointed on the"
                        + " overview, with no reprojection in between");
    }

    @Test
    void panningAnOverviewPageWorksAndTheSolverIsNotTheTangentPlanes() {
        // The pan centre solver refused every projection but one
        // until this issue. The grab invariant is the same one: the
        // sky under the hand stays under the hand.
        ChartScene overview = sceneAt(120.0, 4.0);
        PlanePoint grabbedAt = PanSolver.planeFromPixel(
                overview.viewport(), new PixelPoint(620.0, 260.0));
        SkyPosition grabbed = PanSolver.skyFromPlane(
                overview.viewport(), grabbedAt);
        PlanePoint droppedAt = PanSolver.planeFromPixel(
                overview.viewport(), new PixelPoint(560.0, 300.0));

        var solved = PanSolver.solveCentre(ChartProjection.STEREOGRAPHIC,
                grabbed, droppedAt, ORION);
        assertTrue(solved.centre().isPresent(), "the drag has a centre");
        assertFalse(solved.constrained(), "and is not against a bound");

        PixelPoint landed = new ViewportMapping(
                new juranometria.chart.ChartViewport(solved.centre().get(),
                        120.0, WIDE, HIGH))
                .toPixel(Projections.of(ChartProjection.STEREOGRAPHIC,
                                solved.centre().get())
                        .project(grabbed).orElseThrow());
        assertTrue(Math.hypot(landed.x() - 560.0, landed.y() - 300.0)
                        < 1.0e-2,
                "the sky that was under the hand is under it still: "
                        + landed);
        assertNotEquals(ORION, solved.centre().get(), "the page moved");
    }

    @Test
    void everyLayerOfInkComposesOnAnOverviewPage() {
        // Catalogue marks, the chart's own furniture and three
        // modules' reference geometry, on the widest page there is.
        // The claim is not that it looks a particular way - that is
        // the study's - but that nothing along the way refuses or
        // draws somewhere else.
        ChartScene scene = sceneAt(120.0, 4.0);
        assertTrue(scene.stars().size() > 100,
                "the widest page holds a sky: " + scene.stars().size());

        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(new Observer(59.9, 10.7,
                java.time.Instant.parse("2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);

        BufferedImage image = new BufferedImage(WIDE, HIGH,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT).render(g, scene,
                    ChartOptions.DEFAULTS,
                    (into, painted) -> ReferenceInk.paint(into, painted,
                            registry.collect(), ChartPalette.WHITE_PAPER));
        } finally {
            g.dispose();
        }

        // Every mark the renderer decided on is on the page it was
        // drawn for, which is the composition claim in one line.
        List<ChartRenderer.DrawnMark> marks =
                new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(scene, ChartOptions.DEFAULTS);
        assertFalse(marks.isEmpty(), "the renderer drew marks");
        int onThePaper = 0;
        for (ChartRenderer.DrawnMark mark : marks) {
            assertTrue(Double.isFinite(mark.centre().x())
                            && Double.isFinite(mark.centre().y()),
                    "a mark at a place rather than at infinity: " + mark);
            if (mark.centre().x() >= 0 && mark.centre().x() <= WIDE
                    && mark.centre().y() >= 0
                    && mark.centre().y() <= HIGH) {
                onThePaper++;
            }
        }
        assertTrue(onThePaper > 50,
                "and most of them on the paper: " + onThePaper
                        + " of " + marks.size());
    }

    @Test
    void aConstellationFitsOnAnOverviewPageWithItsShapeIntact() {
        // The acceptance asks that a recognisable constellation fits,
        // with the distortion the gate measured. Both halves are
        // checked, and the second is the gate's whole argument for
        // this projection: a reader matches a *shape* against the
        // sky, and the overview's is conformal - the Plough drawn 60
        // per cent out of round in a corner is not a wider view of
        // the Plough, it is a different asterism.
        ChartScene page = sceneAt(60.0, 5.0);
        var figures = page.geography().figureSegments();
        assertFalse(figures.isEmpty(), "the page carries figures");

        var mapping = new ViewportMapping(page.viewport());
        var projection = Projections.forViewport(page.viewport());
        String orion = "Ori";
        int drawn = 0;
        double worstAngle = 0.0;
        for (var segment : figures) {
            if (!orion.equals(segment.constellationId())) {
                continue;
            }
            PixelPoint from = mapping.toPixel(
                    projection.project(segment.from()).orElseThrow());
            PixelPoint to = mapping.toPixel(
                    projection.project(segment.to()).orElseThrow());
            assertTrue(from.x() >= 0 && from.x() <= WIDE
                            && from.y() >= 0 && from.y() <= HIGH
                            && to.x() >= 0 && to.x() <= WIDE
                            && to.y() >= 0 && to.y() <= HIGH,
                    "the whole figure is on the paper: " + from
                            + " to " + to);
            drawn++;

            // Conformality, measured where a reader would notice it:
            // the angle a figure line makes on the page against the
            // bearing it makes in the sky. A projection that changed
            // shape would turn one into the other.
            worstAngle = Math.max(worstAngle,
                    Math.abs(pageAngle(from, to)
                            - skyBearing(segment.from(), segment.to())));
        }
        assertEquals(24, drawn, "the whole of Orion's figure, not a"
                + " corner of it - the same 24 lines the sheet page"
                + " draws");
        assertTrue(worstAngle < 2.0,
                "and drawn in the shape the sky has, to " + worstAngle
                        + " degrees over " + drawn + " lines");
    }

    /** The direction of a page line, in degrees, north through east. */
    private static double pageAngle(PixelPoint from, PixelPoint to) {
        return Math.toDegrees(Math.atan2(from.x() - to.x(),
                from.y() - to.y()));
    }

    /** The bearing from one sky position to another, the same way. */
    private static double skyBearing(SkyPosition from, SkyPosition to) {
        double dec = Math.toRadians(from.decDegrees());
        double dec2 = Math.toRadians(to.decDegrees());
        double dra = Math.toRadians(to.raDegrees() - from.raDegrees());
        return Math.toDegrees(Math.atan2(
                Math.cos(dec2) * Math.sin(dra),
                Math.cos(dec) * Math.sin(dec2)
                        - Math.sin(dec) * Math.cos(dec2) * Math.cos(dra)));
    }

    @Test
    void anOverviewRefusesRatherThanInventingSkyItCannotDraw() {
        // "Unsupported pan/zoom operations refuse or explain
        // themselves rather than producing invalid sky." On an
        // overview page the refusals are the same ones as anywhere -
        // the sequence's own ends, and a pointer whose anchor the
        // geometry cannot pin.
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 120.0);
        ChartViewState before = controller.state();
        assertEquals(ChartViewController.PointerZoomOutcome.AT_BOUND,
                controller.zoomAt(new PlanePoint(0.2, 0.1), false),
                "the widest rung is the end of the sequence");
        assertEquals(before, controller.state(), "and nothing moved");

        // A near-polar overview page, where the north-up geometry
        // cannot pin a corner pointer: refused, not approximated.
        ChartViewController polar = new ChartViewController();
        polar.recenter(new SkyPosition(0.0, 89.9), 60.0);
        ChartViewState polarBefore = polar.state();
        PlanePoint corner = PanSolver.planeFromPixel(
                new juranometria.chart.ChartViewport(
                        polarBefore.centre(), 60.0, WIDE, HIGH),
                new PixelPoint(899.0, 1.0));
        assertEquals(ChartViewController.PointerZoomOutcome
                        .INFEASIBLE_POINTER,
                polar.zoomAt(corner, true),
                "a pointer the geometry cannot pin is refused");
        assertEquals(polarBefore, polar.state(),
                "and the chart is exactly as it was");
    }

    @Test
    void anOverviewExportsAsTheProductionPageItIs() throws Exception {
        // The issue asks whether export means anything here. It means
        // what it means everywhere else: the sheet plays the
        // production render, and the writers record what it played.
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble,
                new ChartViewState(ORION, 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        assertEquals(120.0, sheet.scene().viewport().fieldWidthDegrees(),
                "the sheet is the page the reader was on");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                sheet.scene().viewport().projection(),
                "drawn by the projection that field belongs to");
        for (SheetFormat format : SheetFormat.values()) {
            assertTrue(SheetWriters.write(sheet, format, 150).length > 1000,
                    format + " writes the overview");
        }
    }

    @Test
    void theAtlasOpensWhereItAlwaysDidAndRemembersNothing() {
        // Fresh install and persistence, decided out loud rather than
        // inherited from whatever zoom storage happens to exist:
        // there is none. The atlas keeps no view state between runs,
        // so every launch - first or thousandth - opens on Home, and
        // an overview is somewhere a reader goes rather than
        // somewhere they can be left.
        assertEquals(8.0, ChartViewState.DEFAULT.fieldWidthDegrees(),
                "Home is the 8-degree page it has always been");
        assertEquals(ChartProjection.GNOMONIC,
                ChartViewState.DEFAULT.projection());
        assertFalse(ChartViewState.DEFAULT.overview());

        ChartViewController controller = new ChartViewController();
        assertEquals(ChartViewState.DEFAULT, controller.state(),
                "a new controller is Home, whatever the last one saw");
        controller.recenter(ORION, 120.0);
        controller.reset();
        assertEquals(ChartViewState.DEFAULT, controller.state(),
                "and Home from the overview is the complete default -"
                        + " centre, field, projection and limit");
    }
}
