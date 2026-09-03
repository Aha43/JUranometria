package juranometria.ui.placeandtime;

import java.time.Instant;
import java.util.List;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.meridian.MeridianModule;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The session-start policy, asked of the one seam that owns it
 * (Sprint 25, issue #229).
 *
 * <p>The application's main, the journey and the packaged
 * acceptance all begin the meridian module through
 * {@link PlaceAndTimeSession#begin}, so what a reader's next evening
 * looks like is decided here and nowhere else. These tests hold the
 * policy itself; the journeys hold that real sessions actually go
 * through it.
 */
class PlaceAndTimeSessionTest {

    private static final Instant MOMENT =
            Instant.parse("2026-09-05T10:28:31Z");

    private final Preferences node = Preferences.userRoot().node(
            "juranometria-test-session-" + System.nanoTime());

    @AfterEach
    void dropTestNode() throws Exception {
        node.removeNode();
    }

    private ChartComponent chart;

    private ChartModuleHost host() {
        chart = new ChartComponent(Atlas.assembler());
        chart.setSize(900, 700);
        chart.setViewState(ChartViewState.DEFAULT);
        return new ChartModuleHost(chart, new SelectionModel(),
                request -> { });
    }

    @Test
    void aSessionBeginsOnTheRememberedPlaceAtTheStatedMoment() {
        PlaceStore store = PlaceStore.forNode(node);
        store.save(42.5, -130.994);

        MeridianModule module = PlaceAndTimeSession.begin(host(), store,
                MOMENT);

        assertEquals(42.5, module.observer().latitudeDegrees(),
                "the remembered latitude");
        assertEquals(-130.994, module.observer().eastLongitudeDegrees(),
                "and longitude");
        assertEquals(MOMENT, module.observer().instant(),
                "at the moment the caller stated - the seam has no"
                        + " clock of its own");
    }

    @Test
    void everySwitchBeginsOffAndTheSessionContributesNothing() {
        MeridianModule module = PlaceAndTimeSession.begin(host(),
                PlaceStore.forNode(node), MOMENT);

        assertTrue(!module.meridianShowing() && !module.horizonShowing()
                        && !module.zenithShowing(),
                "the switches are not persisted, so each session"
                        + " begins with all three off");
        assertEquals(List.of(), module.contributedGeometry(),
                "and the session begins on the ordinary chart");
        assertEquals(0, module.timesTheSkyWasComputed(),
                "having computed no sky to get there");
    }

    @Test
    void theModuleIsAttachedAndNotMerelyConstructed() {
        // begin() must leave the module living on the chart: a seam
        // that constructed without attaching would hand back a
        // module whose dialog worked and whose lines never drew.
        // Asked of the chart's own registry - the geometry the
        // painter would collect at the next paint.
        ChartModuleHost host = host();
        MeridianModule module = PlaceAndTimeSession.begin(host,
                PlaceStore.forNode(node), MOMENT);

        assertEquals(0, chart.overlays().collect().size(),
                "quiet at first, and registered: nothing to ink yet");
        module.showing(true, true, true);
        assertEquals(3, chart.overlays().collect().size(),
                "showing reaches the chart, so the module was really"
                        + " attached");
        module.detach();
        assertEquals(0, chart.overlays().collect().size(),
                "and detach withdraws it");
    }
}
