package juranometria.chart;

import org.junit.jupiter.api.Test;

import juranometria.project.DrawnPage;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the 180-degree rung means (Sprint 32, issue #329).
 *
 * <p>This replaces {@code CelestialGlobeDoorTest}, which held that no
 * reader could reach a globe while #301 was deciding what one should
 * be. That test said of itself that when the orthographic projection
 * became production and 180 degrees became a rung it should be
 * <em>replaced by the contract that decides what the rung means, not
 * deleted quietly because it started failing</em>. This is that
 * contract.
 */
class CelestialGlobeRungTest {

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    @Test
    void aReaderCanAskForAHundredAndEightyDegrees() {
        ChartViewState globe = new ChartViewState(SAGITTARIUS, 180.0, 5.0);
        assertEquals(ChartProjection.ORTHOGRAPHIC, globe.projection(),
                "the widest rung is the globe's, and the projection"
                        + " comes with the rung rather than from a menu");
        assertEquals(180.0, ChartViewState.fieldWidthSteps().get(0),
                "and it is the top of the ladder");
        assertFalse(globe.canZoomOut(),
                "with nothing above it: a hemisphere is all the sky"
                        + " one page can hold");
        assertTrue(globe.canZoomIn(),
                "and the way back is the way a reader came");
    }

    @Test
    void thereIsStillNoProjectionToChoose() {
        // The rung carries the projection; nothing offers a choice of
        // one, now or later. A reader who had to be told which
        // projection was drawing would be a reader being told about a
        // problem they do not have.
        assertEquals(ChartProjection.ORTHOGRAPHIC,
                ChartProjection.forField(180.0));
        assertEquals(ChartProjection.STEREOGRAPHIC,
                ChartProjection.forField(120.0),
                "and the rung below is unchanged");
        assertEquals(ChartProjection.GNOMONIC,
                ChartProjection.forField(42.0),
                "as is the atlas's own");

        IllegalArgumentException refused = org.junit.jupiter.api.Assertions
                .assertThrows(IllegalArgumentException.class,
                        () -> new ChartViewState(SAGITTARIUS, 180.0, 5.0,
                                null, null, ChartProjection.STEREOGRAPHIC));
        assertTrue(refused.getMessage().contains("not a choice"),
                "a state that disagrees with its own field is refused:"
                        + " " + refused.getMessage());
    }

    @Test
    void theAtlasShipsExactlyThreeProjections() {
        assertEquals(
                java.util.List.of(ChartProjection.GNOMONIC,
                        ChartProjection.STEREOGRAPHIC,
                        ChartProjection.ORTHOGRAPHIC),
                java.util.List.of(ChartProjection.values()),
                "three production projections, in the order the ladder"
                        + " reaches them from the eyepiece outward");
        for (ChartProjection kind : ChartProjection.values()) {
            var projection = Projections.of(kind, SAGITTARIUS);
            assertEquals(kind.displayName(), projection.name(),
                    "every one names itself the same way through the"
                            + " registry as on the page");
        }
        // And every rung is drawn by one of them, with no field left
        // over for a fourth.
        for (double field : ChartViewState.fieldWidthSteps()) {
            assertTrue(java.util.List.of(ChartProjection.values())
                            .contains(ChartProjection.forField(field)),
                    field + " degrees is drawn by a shipped projection");
        }
    }

    @Test
    void theStudyDoorIsGoneRatherThanUnused() throws Exception {
        // Not "no test looks at it any more": the method is not there
        // to be called. #301 assembled globes through
        // SceneAssembler.assembleForStudy because no rung could make
        // one; #329 removed the door with the need for it.
        for (var method : juranometria.ui.SceneAssembler.class
                .getDeclaredMethods()) {
            assertFalse(method.getName().contains("ForStudy"),
                    "the study door is still there: "
                            + method.getName());
        }
        for (var method : juranometria.render.ChartRenderer.class
                .getDeclaredMethods()) {
            assertFalse("drawing".equals(method.getName()),
                    "the renderer's page door is still there");
        }
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName(
                        "juranometria.tool.globe.GlobeProjection"),
                "and the study's own copy of the projection is gone,"
                        + " because production has one");
        assertNull(System.getProperty("juranometria.globeFrame"),
                "nor is the frame something a property can move");
    }

    @Test
    void theDiscIsNinetyPerCentOfTheShortSide() {
        for (int[] shape : new int[][] {{1200, 800}, {800, 1200},
                {1123, 794}, {900, 900}}) {
            ChartViewport viewport = new ChartViewport(SAGITTARIUS,
                    180.0, shape[0], shape[1],
                    ChartProjection.ORTHOGRAPHIC);
            var projection = Projections.forViewport(viewport);
            double discPx = 2.0 * new ViewportMapping(viewport, projection)
                    .pixelsPerPlaneUnit() * projection.visiblePlaneRadius();
            assertEquals(0.90 * Math.min(shape[0], shape[1]), discPx,
                    1.0,
                    shape[0] + "x" + shape[1] + ": the disc fills nine"
                            + " tenths of the short side, and the rest"
                            + " is the border the furniture lives in");
        }
    }

    @Test
    void theRungArrivesAtVFive() {
        // Its own default, measured by the gate: at V 8.0 a
        // hemisphere's limb band is 80 per cent inked, and at V 4.0
        // three hundred stars are too few for the figures to hold
        // together (docs/decisions/celestial-globe.md).
        assertEquals(5.0, ChartViewState.defaultMagnitudeFor(180.0),
                "the globe's own limit");
        assertEquals(5.0,
                new ChartViewState(SAGITTARIUS, 180.0,
                        ChartViewState.defaultMagnitudeFor(180.0))
                        .limitingMagnitude(),
                "so a globe made from defaults is at V 5.0");
    }

    @Test
    void arrivingFromTheRungBelowKeepsTheReadersOwnLimit() {
        // The ladder's older promise, which this rung does not get to
        // break: zooming out never adds stars. A reader who chose
        // V 4.0 and zoomed out to the globe sees V 4.0, and the same
        // globe must not depend on how it was reached.
        ChartViewState overview = new ChartViewState(SAGITTARIUS, 120.0, 4.0);
        ChartViewState globe = overview.zoomOut();
        assertEquals(180.0, globe.fieldWidthDegrees());
        assertEquals(4.0, globe.limitingMagnitude(),
                "V 4.0 carried across: zooming out never adds stars,"
                        + " and the rung's own default is a default"
                        + " rather than a value imposed on arrival");

        ChartViewState fainter = new ChartViewState(SAGITTARIUS, 120.0, 8.0);
        assertEquals(5.0, fainter.zoomOut().limitingMagnitude(),
                "and arriving from a fainter limit takes the rung's"
                        + " own, which is the existing wide-field rule");
    }

    @Test
    void goingBackDownLeavesTheRungBelowAsItWas() {
        ChartViewState globe = new ChartViewState(SAGITTARIUS, 180.0, 5.0);
        ChartViewState back = globe.zoomIn();
        assertEquals(120.0, back.fieldWidthDegrees());
        assertEquals(ChartProjection.STEREOGRAPHIC, back.projection(),
                "the rung below is drawn by the projection it always"
                        + " was");
        assertEquals(SAGITTARIUS, back.centre(),
                "and centred where the reader was");
    }

    @Test
    void aGlobePageDrawsWithoutBoundariesAndChangesNothingElse() {
        DrawnPage page = DrawnPage.of(new ChartScene(
                new ChartViewport(SAGITTARIUS, 180.0, 900, 900,
                        ChartProjection.ORTHOGRAPHIC),
                java.util.List.of(), java.util.List.of(),
                "Sagittarius", 5.0));
        ChartOptions drawn = ChartOptions.DEFAULTS.onPage(page);
        assertFalse(drawn.constellationBoundaries(),
                "boundaries are off on a globe: the gate measured them"
                        + " costing more ink than any other layer at a"
                        + " limb carrying half the sky");
        assertTrue(drawn.constellationFigures(), "figures stay");
        assertTrue(drawn.equatorialGrid(), "the grid stays");
        assertTrue(drawn.constellationNames(), "names stay");
        assertTrue(drawn.starNames() && drawn.bayerLetters(),
                "star names and letters stay");
        assertTrue(drawn.galaxies() && drawn.openClusters()
                        && drawn.globularClusters() && drawn.nebulae()
                        && drawn.planetaryNebulae(),
                "and all five deep-sky families stay");
        assertEquals(ChartOptions.DEFAULTS.withPalette(drawn.palette())
                        .constellationBoundaries(), true,
                "the reader's own options are untouched by asking");
    }

    @Test
    void thePageSaysWhichProjectionDrewItEverywhereItSpeaks() {
        DrawnPage page = DrawnPage.of(new ChartScene(
                new ChartViewport(SAGITTARIUS, 180.0, 900, 900,
                        ChartProjection.ORTHOGRAPHIC),
                java.util.List.of(), java.util.List.of(),
                "Sagittarius", 5.0));
        assertEquals("orthographic", page.projectionName());
        assertTrue(page.describe().contains("orthographic projection"),
                "the accessible description names it: " + page.describe());
        assertNotNull(page.projection());
        assertTrue(page.bounded(),
                "and the page knows its sky ends inside the paper");
    }
}
