package juranometria.meridian;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.InkRole;
import juranometria.module.NavigationRequest;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.module.TestChartServices;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The removable module (Sprint 25, issue #227).
 *
 * <p>What is asked here is ownership and restraint rather than
 * astronomy: the geometry is the model's, checked against it rather
 * than recomputed; the chart does not move unless a reader asks; a
 * module showing nothing costs nothing; and detaching takes away
 * this module's ink and no one else's.
 */
class MeridianModuleTest {

    private static final Instant WHEN =
            Instant.parse("2026-03-20T21:33:00Z");
    private static final Observer OSLO =
            new Observer(59.913, 10.752, WHEN);

    private static MeridianModule attached(TestChartServices services) {
        MeridianModule module = new MeridianModule(OSLO);
        module.attach(services);
        return module;
    }

    // ---- the geometry is the model's --------------------------------

    @Test
    void theThreeGeometriesAreTheModelsOwnAndNotARecomputationOfThem() {
        MeridianModule module = new MeridianModule(OSLO);
        LocalSky sky = new LocalSky(OSLO);

        assertEquals(sky.meridian().pole(), circle(module, "meridian").pole(),
                "the meridian is the model's meridian");
        assertEquals(sky.horizon().pole(), circle(module, "horizon").pole(),
                "the horizon is the model's horizon");
        assertEquals(sky.zenith(), point(module, "zenith").at(),
                "and the zenith is the model's zenith");
    }

    @Test
    void aLineAcrossTheSkyAndABoundaryOfWhatCanBeSeenAreDistinguished() {
        MeridianModule module = new MeridianModule(OSLO);

        assertEquals(OverlayContribution.Reference.LINE,
                circle(module, "meridian").reference(),
                "the meridian is a line drawn across the sky");
        assertEquals(OverlayContribution.Reference.BOUNDARY,
                circle(module, "horizon").reference(),
                "and the horizon bounds what can be seen of it - which"
                        + " is what lets the chart draw one dashed"
                        + " without being told what a horizon is");
        for (OverlayContribution offered : module.contributedGeometry()) {
            assertEquals(InkRole.REFERENCE_LINE, offered.role(),
                    "all three are reference ink: " + offered.identity());
            assertTrue(!offered.accessibleName().isBlank(),
                    "and each says what it is, in words: "
                            + offered.identity());
        }
    }

    @Test
    void everyLineCanBeShownOrHiddenOnItsOwn() {
        MeridianModule module = new MeridianModule(OSLO);

        module.showing(true, false, false);
        assertEquals(List.of("meridian"), identities(module));
        module.showing(false, true, false);
        assertEquals(List.of("horizon"), identities(module));
        module.showing(false, false, true);
        assertEquals(List.of("zenith"), identities(module));
        module.showing(true, true, true);
        assertEquals(List.of("meridian", "horizon", "zenith"),
                identities(module));
    }

    @Test
    void aModuleShowingNothingOffersNothingAndWorksOutNoSky() {
        // "With the module disabled the chart performs no observer or
        // time work" is the acceptance criterion, so it is asked of
        // the module rather than described: the count does not move,
        // which means the answer was reached before any of the
        // observer's sky was computed.
        MeridianModule module = new MeridianModule(OSLO);
        module.showing(false, false, false);
        long before = module.timesTheSkyWasComputed();

        assertEquals(List.of(), module.contributedGeometry(),
                "a module with nothing to show contributes nothing");
        assertEquals(before, module.timesTheSkyWasComputed(),
                "and works out no sidereal time, no precession and no"
                        + " nutation to say so");

        module.showing(true, false, false);
        module.contributedGeometry();
        assertTrue(module.timesTheSkyWasComputed() > before,
                "while a module with something to show does the work -"
                        + " so the count above is a measurement and not"
                        + " a constant");
    }

    // ---- the chart stays where the reader put it ---------------------

    @Test
    void changingPlaceOrTimeRedrawsTheLinesAndLeavesThePageWhereItIs() {
        TestChartServices services = new TestChartServices();
        MeridianModule module = attached(services);
        int redrawsBefore = services.redraws;
        int inventoryBefore = services.inventoryReads;

        module.observer(OSLO.from(-33.87, 151.21));
        module.observer(module.observer().at(WHEN.plusSeconds(7200)));
        module.showing(true, false, true);

        assertEquals(List.of(), services.requested,
                "not one of those asked the chart to move: a page that"
                        + " moved while a reader was setting a clock"
                        + " could not be read");
        assertEquals(redrawsBefore + 3, services.redraws,
                "each asked for the lines to be drawn again");
        assertEquals(inventoryBefore, services.inventoryReads,
                "and none of them read the catalogue - changing place"
                        + " or time reassembles nothing");
    }

    @Test
    void onlyAReaderAskingMovesTheChartAndThenOnlyAsARequest() {
        TestChartServices services = new TestChartServices();
        MeridianModule module = attached(services);

        module.centreOnZenith();

        assertEquals(1, services.requested.size(),
                "once, because the reader asked once");
        NavigationRequest asked = services.requested.get(0);
        assertEquals(new LocalSky(OSLO).zenith(), asked.centre(),
                "on the point overhead");
        assertEquals(null, asked.fieldWidthDegrees(),
                "keeping the field the reader chose");
        assertTrue(!asked.because().isBlank(),
                "and saying why, so a reader can be told what moved"
                        + " their page: " + asked.because());
    }

    @Test
    void anUnattachedModuleHasNoChartToAsk() {
        assertThrows(IllegalStateException.class,
                () -> new MeridianModule(OSLO).centreOnZenith(),
                "a module with no chart cannot ask one to move");
    }

    // ---- lifecycle ---------------------------------------------------

    @Test
    void detachingWithdrawsThisModulesInkAndNobodyElses() {
        TestChartServices services = new TestChartServices();
        MeridianModule module = attached(services);
        ChartModule other = new ChartModule() {
            @Override public String name() {
                return "other";
            }

            @Override public void attach(ChartServices given) {
                given.contribute("other", () -> List.of(
                        new OverlayContribution.Point("mark", "Mark",
                                new SkyPosition(10.0, 41.0),
                                InkRole.INTERACTION)));
            }

            @Override public void detach() {
            }
        };
        other.attach(services);
        assertEquals(4, services.overlays.collect().size(),
                "three lines and the other module's mark");

        module.detach();

        List<String> left = services.overlays.collect().stream()
                .map(OverlayRegistry.Owned::key).toList();
        assertEquals(List.of("other/mark"), left,
                "detaching took away this module's ink and left the"
                        + " other module's exactly where it was");
    }

    @Test
    void detachingTwiceIsNotAWayToWithdrawSomebodyElse() {
        TestChartServices services = new TestChartServices();
        MeridianModule module = attached(services);
        module.detach();
        module.detach();
        assertEquals(List.of(), services.overlays.collect());
    }

    @Test
    void attachingTwiceIsRefused() {
        TestChartServices services = new TestChartServices();
        MeridianModule module = attached(services);
        assertThrows(IllegalStateException.class,
                () -> module.attach(new TestChartServices()),
                "a module attached twice has lost track of its own"
                        + " lifecycle, and the first contribution has"
                        + " nothing left to withdraw it");
    }

    @Test
    void aModuleIsAlwaysSomewhereAtSomeMoment() {
        assertThrows(IllegalArgumentException.class,
                () -> new MeridianModule(null));
        assertThrows(IllegalArgumentException.class,
                () -> new MeridianModule(OSLO).observer(null));
    }

    // ----------------------------------------------------------------

    private static List<String> identities(MeridianModule module) {
        return module.contributedGeometry().stream()
                .map(OverlayContribution::identity).toList();
    }

    private static OverlayContribution.GreatCircle circle(
            MeridianModule module, String identity) {
        return assertInstanceOf(OverlayContribution.GreatCircle.class,
                offered(module, identity));
    }

    private static OverlayContribution.Point point(MeridianModule module,
                                                   String identity) {
        return assertInstanceOf(OverlayContribution.Point.class,
                offered(module, identity));
    }

    private static OverlayContribution offered(MeridianModule module,
                                               String identity) {
        return module.contributedGeometry().stream()
                .filter(geometry -> geometry.identity().equals(identity))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "the module offers " + identity));
    }
}
