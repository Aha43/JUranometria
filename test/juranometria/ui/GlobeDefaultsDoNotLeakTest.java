package juranometria.ui;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A globe's defaults are the page's, never the reader's (Sprint 32,
 * issue #329).
 *
 * <p>The obvious way to give a globe its own furniture is to write
 * that furniture into the options the reader keeps, and it would pass
 * any test that only asked what the globe draws. What it would do is
 * change the reader's charts behind them: they turn boundaries on,
 * visit a globe once, and come back to a sheet page that has quietly
 * lost them.
 *
 * <p>So the round trip starts from a <strong>deliberately
 * non-default</strong> page. A test that began at the released
 * defaults could not tell preservation from a restore, because both
 * end at the same place.
 */
class GlobeDefaultsDoNotLeakTest {

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    /**
     * Nothing a reader would be handed: boundaries deliberately on
     * and several other switches moved off their released values, so
     * that "restored the defaults" and "kept what I had" cannot be
     * mistaken for each other.
     */
    private static final ChartOptions THE_READERS_OWN = new ChartOptions(
            true, false,
            false, true, true,
            true, false, false,
            false, true, true,
            true, false, true, false, true,
            ChartPalette.BLACK_SKY);

    /** The reader's own control, found the way a reader finds it. */
    private static javax.swing.JButton buttonNamed(
            java.awt.Component component, String accessibleName) {
        if (component instanceof javax.swing.JButton button
                && accessibleName.equals(button.getAccessibleContext()
                        .getAccessibleName())) {
            return button;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                javax.swing.JButton found =
                        buttonNamed(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static DrawnPage pageAt(double field) {
        return DrawnPage.of(new ChartScene(
                new ChartViewport(SAGITTARIUS, field, 900, 900,
                        ChartProjection.forField(field)),
                java.util.List.of(), java.util.List.of(),
                "Sagittarius", 5.0));
    }

    @Test
    void theGlobeHidesBoundariesAndTheReadersChoiceIsUntouched() {
        assertTrue(THE_READERS_OWN.constellationBoundaries(),
                "the fixture has them deliberately on");

        ChartOptions onTheGlobe = THE_READERS_OWN.onPage(pageAt(180.0));
        assertFalse(onTheGlobe.constellationBoundaries(),
                "the globe does not draw them");
        assertTrue(THE_READERS_OWN.constellationBoundaries(),
                "and the reader's own options still say on - asking"
                        + " what a page draws may not change what the"
                        + " reader chose");
    }

    @Test
    void everySwitchButBoundariesSurvivesTheGlobeUnchanged() {
        ChartOptions onTheGlobe = THE_READERS_OWN.onPage(pageAt(180.0));
        assertEquals(THE_READERS_OWN.deepSkyObjects(),
                onTheGlobe.deepSkyObjects());
        assertEquals(THE_READERS_OWN.deepSkyLabels(),
                onTheGlobe.deepSkyLabels());
        assertEquals(THE_READERS_OWN.constellationFigures(),
                onTheGlobe.constellationFigures());
        assertEquals(THE_READERS_OWN.constellationNames(),
                onTheGlobe.constellationNames());
        assertEquals(THE_READERS_OWN.starNames(), onTheGlobe.starNames());
        assertEquals(THE_READERS_OWN.bayerLetters(),
                onTheGlobe.bayerLetters());
        assertEquals(THE_READERS_OWN.flamsteedNumbers(),
                onTheGlobe.flamsteedNumbers());
        assertEquals(THE_READERS_OWN.equatorialGrid(),
                onTheGlobe.equatorialGrid());
        assertEquals(THE_READERS_OWN.titleBlock(), onTheGlobe.titleBlock());
        assertEquals(THE_READERS_OWN.magnitudeKey(),
                onTheGlobe.magnitudeKey());
        assertEquals(THE_READERS_OWN.galaxies(), onTheGlobe.galaxies());
        assertEquals(THE_READERS_OWN.openClusters(),
                onTheGlobe.openClusters());
        assertEquals(THE_READERS_OWN.globularClusters(),
                onTheGlobe.globularClusters());
        assertEquals(THE_READERS_OWN.nebulae(), onTheGlobe.nebulae());
        assertEquals(THE_READERS_OWN.planetaryNebulae(),
                onTheGlobe.planetaryNebulae());
        assertEquals(THE_READERS_OWN.palette(), onTheGlobe.palette(),
                "a globe is not a black-sky decision either");
    }

    @Test
    void theyComeBackOnTheRungsEitherSideOfTheGlobe() {
        for (double field : new double[] {120.0, 90.0, 60.0, 42.0, 8.0}) {
            ChartOptions ordinary = THE_READERS_OWN.onPage(pageAt(field));
            assertTrue(ordinary.constellationBoundaries(),
                    field + " degrees is an ordinary page and draws the"
                            + " boundaries the reader asked for");
            assertSame(THE_READERS_OWN, ordinary,
                    field + " degrees does not even make a new value:"
                            + " an unbounded page draws what it was"
                            + " given");
        }
    }

    @Test
    void theWholeRoundTripLeavesTheReaderWhereTheyStarted() {
        // Out to the globe and back, asking each page what it draws.
        ChartViewState start = new ChartViewState(SAGITTARIUS, 42.0, 4.0);
        ChartOptions chosen = THE_READERS_OWN;

        ChartOptions onSheet = chosen.onPage(pageAt(42.0));
        assertTrue(onSheet.constellationBoundaries());

        ChartViewState wide = start.withFieldWidth(120.0);
        assertTrue(chosen.onPage(pageAt(120.0)).constellationBoundaries());

        ChartViewState globe = wide.zoomOut();
        assertEquals(180.0, globe.fieldWidthDegrees());
        assertFalse(chosen.onPage(pageAt(180.0)).constellationBoundaries(),
                "hidden while the globe is drawn");
        assertEquals(4.0, globe.limitingMagnitude(),
                "and the reader's brighter limit carried across rather"
                        + " than being replaced by the rung's V 5.0");

        ChartViewState home = globe.zoomIn().withFieldWidth(42.0);
        assertEquals(42.0, home.fieldWidthDegrees());
        assertEquals(ChartProjection.GNOMONIC, home.projection());
        assertTrue(chosen.onPage(pageAt(42.0)).constellationBoundaries(),
                "and on again the moment the page is an ordinary one");
        assertEquals(THE_READERS_OWN, chosen,
                "with the reader's own options the object they always"
                        + " were - nothing on this journey wrote to"
                        + " them");
    }

    @Test
    void restoreDefaultsOnAGlobeResetsLayersAndMovesNothingElse()
            throws Exception {
        // Through the button a reader actually presses, because the
        // question is what that route does rather than what today's
        // model happens to hold. A future change could reset the
        // magnitude down another controller path without ever putting
        // one in ChartOptions, and the structural test below would go
        // on passing.
        java.util.prefs.Preferences node =
                java.util.prefs.Preferences.userRoot()
                        .node("juranometria-test-" + System.nanoTime());
        try {
            juranometria.app.ChartOptionsStore store =
                    juranometria.app.ChartOptionsStore.forNode(node);
            store.save(THE_READERS_OWN);
            juranometria.app.ChartOptionsController options =
                    new juranometria.app.ChartOptionsController(store);

            // On a globe, at a magnitude that is not the rung's own
            // default, centred somewhere deliberate.
            ChartViewController navigation = new ChartViewController();
            navigation.recenter(SAGITTARIUS, 180.0);
            ChartViewState before = navigation.state();
            assertEquals(180.0, before.fieldWidthDegrees());

            javax.swing.JComponent content = juranometria.app
                    .ChartOptionsDialog.contentForStudy(options);
            javax.swing.JButton restore = buttonNamed(content,
                    "Restore Defaults");
            org.junit.jupiter.api.Assertions.assertNotNull(restore,
                    "the reader's own route to this");
            restore.doClick();

            assertEquals(ChartOptions.DEFAULTS, options.options(),
                    "the layers are the released chart's again");
            assertFalse(options.options().onPage(pageAt(180.0))
                            .constellationBoundaries(),
                    "and the globe still draws them as a globe does");

            assertEquals(before.limitingMagnitude(),
                    navigation.state().limitingMagnitude(),
                    "the magnitude is page state and this button is"
                            + " not a navigation event");
            assertEquals(before.centre(), navigation.state().centre(),
                    "nor did the page move");
            assertEquals(before.fieldWidthDegrees(),
                    navigation.state().fieldWidthDegrees(),
                    "nor the rung");

            assertEquals(THE_READERS_OWN, store.load(),
                    "and nothing is stored by a preview - the reader's"
                            + " own choices are still on disk");
            options.confirm();
            assertEquals(ChartOptions.DEFAULTS, store.load(),
                    "OK stores the released layers");
            assertTrue(store.load().constellationBoundaries(),
                    "with boundaries ON: what is kept is the reader's"
                        + " chart, never the globe's view of it");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void restoringDefaultsCannotMoveTheMagnitude() {
        // Restore Defaults means the chart's layers and has meant
        // that since it existed. The magnitude is page state, not a
        // layer - so the button cannot move it, and the globe does
        // not get a version of it that can. Structural rather than
        // behavioural on purpose: what is held is that there is no
        // magnitude in the value the button restores, which is a
        // stronger statement than watching one button not change one
        // number (#329).
        for (var component : ChartOptions.class.getRecordComponents()) {
            assertFalse(component.getName().toLowerCase()
                            .contains("magnitude")
                            && !component.getName().equals("magnitudeKey"),
                    "the options carry a magnitude: " + component);
        }
        assertEquals(ChartOptions.DEFAULTS,
                ChartOptions.DEFAULTS.onPage(pageAt(42.0)),
                "and the released layers on an ordinary page are the"
                        + " released layers");

        // On a globe those same restored layers are drawn as a globe
        // draws them, with the stored choice still saying otherwise.
        assertTrue(ChartOptions.DEFAULTS.constellationBoundaries(),
                "the released chart draws boundaries");
        assertFalse(ChartOptions.DEFAULTS.onPage(pageAt(180.0))
                        .constellationBoundaries(),
                "a globe drawn from them still hides boundaries");
    }

    @Test
    void theChartComponentAsksThePageRatherThanTheStore() throws Exception {
        // The seam as the application uses it: what the reader chose
        // is one question, what this page draws is another, and the
        // inventory and the ink both have to ask the second.
        ChartComponent[] holder = new ChartComponent[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ChartComponent(juranometria.app.Atlas.assembler());
            holder[0].setSize(900, 700);
            holder[0].setChartOptions(THE_READERS_OWN);
            holder[0].setViewState(
                    new ChartViewState(SAGITTARIUS, 180.0, 5.0));
        });
        ChartComponent chart = holder[0];

        assertTrue(chart.chartOptions().constellationBoundaries(),
                "what the reader chose, unchanged by being on a globe");
        assertFalse(chart.drawnOptions().constellationBoundaries(),
                "what this page draws");

        javax.swing.SwingUtilities.invokeAndWait(() -> holder[0]
                .setViewState(new ChartViewState(SAGITTARIUS, 42.0, 5.0)));
        assertTrue(chart.chartOptions().constellationBoundaries());
        assertTrue(chart.drawnOptions().constellationBoundaries(),
                "and on an ordinary page the two are the same answer");
    }
}
