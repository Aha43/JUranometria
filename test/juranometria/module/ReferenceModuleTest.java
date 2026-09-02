package juranometria.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageInventory;
import juranometria.page.WorkingMarksModel;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A module built on the seam, to prove the seam can carry one
 * (Sprint 24, issue #215).
 *
 * <p>#216 builds the real <strong>On this page</strong> module - the
 * table and the crosses. This one is the smallest thing that
 * exercises every capability the gate reviewed: it reads the
 * inventory, subscribes to the marked set, contributes typed
 * geometry in an ink role, asks for navigation, and lets go of all
 * of it when detached.
 *
 * <p>It lives in the tests on purpose. A module that had to exist
 * for the chart to work would not be a module.
 */
class ReferenceModuleTest {

    /** Marks whatever is on the page, and offers a cross for each. */
    private static final class MarkingModule implements ChartModule {

        private final String id;
        private ChartServices services;
        private Runnable unsubscribe;
        private Runnable withdraw;
        private final List<WorkingMarksModel.Change> heard = new ArrayList<>();

        MarkingModule(String id) {
            this.id = id;
        }

        @Override
        public String name() {
            return id;
        }

        @Override
        public void attach(ChartServices services) {
            this.services = services;
            this.unsubscribe = services.workingMarks().onChange(heard::add);
            this.withdraw = services.contribute(id, this::overlay);
        }

        @Override
        public void detach() {
            if (unsubscribe != null) {
                unsubscribe.run();
            }
            if (withdraw != null) {
                withdraw.run();
            }
            unsubscribe = null;
            withdraw = null;
            services = null;
        }

        /** Its contribution: a cross per marked object, as points. */
        List<OverlayContribution> overlay() {
            List<OverlayContribution> geometry = new ArrayList<>();
            for (String identity : services.workingMarks().marks()) {
                PageEntry entry = services.inventory().find(identity)
                        .orElse(null);
                if (entry == null) {
                    continue;
                }
                geometry.add(new OverlayContribution.Point(
                        "working-mark:" + identity,
                        "working mark on " + identity,
                        entry.position(), InkRole.INTERACTION));
            }
            return geometry;
        }
    }

    /** The chart, as a module is allowed to see it. */
    private static final class Services implements ChartServices {

        private final ChartViewState state = ChartViewState.DEFAULT;
        private final ChartScene scene =
                Atlas.assembler().assemble(ChartViewState.DEFAULT, 900, 700);
        private final ChartOptions options = ChartOptions.DEFAULTS;
        private final PageContents inventory =
                PageInventory.of(
                        Atlas.assembler().assemble(ChartViewState.DEFAULT,
                                900, 700), ChartOptions.DEFAULTS);
        private final SelectionModel selection = new SelectionModel();
        private final WorkingMarksModel marks = new WorkingMarksModel();
        final List<NavigationRequest> requested = new ArrayList<>();
        final OverlayRegistry overlays = new OverlayRegistry();

        @Override public ChartViewState viewState() {
            return state;
        }

        @Override public ChartScene scene() {
            return scene;
        }

        @Override public ChartOptions options() {
            return options;
        }

        @Override public PageContents inventory() {
            return inventory;
        }

        @Override public Runnable onPageChange(Consumer<PageContents> l) {
            l.accept(inventory);
            return () -> { };
        }

        @Override public Projection projection() {
            return new Projection() {
                @Override public Optional<double[]> toPage(SkyPosition at) {
                    return new juranometria.project.GnomonicProjection(
                            scene.viewport().centre()).project(at)
                            .map(plane -> {
                                var pixel = new juranometria.project
                                        .ViewportMapping(scene.viewport())
                                        .toPixel(plane);
                                return new double[] {pixel.x(), pixel.y()};
                            });
                }

                @Override public SkyPosition toSky(double x, double y) {
                    return juranometria.render.ChartHitTest.skyAt(scene, x, y);
                }
            };
        }

        @Override public SelectionModel selection() {
            return selection;
        }

        @Override public WorkingMarksModel workingMarks() {
            return marks;
        }

        @Override public void request(NavigationRequest request) {
            requested.add(request);
        }

        @Override public Runnable contribute(String moduleId,
                java.util.function.Supplier<List<OverlayContribution>> geometry) {
            return overlays.offer(moduleId, geometry);
        }
    }

    @Test
    void aModuleReadsThePageMarksItAndOffersGeometryForTheChartToInk() {
        Services services = new Services();
        MarkingModule module = new MarkingModule("reference");
        module.attach(services);

        String first = services.inventory().deepSky().get(0).identity();
        services.workingMarks().mark(first);

        List<OverlayRegistry.Owned> collected = services.overlays.collect();
        assertEquals(1, collected.size(),
                "one cross for the one marked object");
        assertEquals("reference", collected.get(0).moduleId(),
                "and the chart knows whose it is");
        OverlayContribution ink = collected.get(0).geometry();
        assertEquals(InkRole.INTERACTION, ink.role(),
                "interaction ink, not cartographic vocabulary");
        assertTrue(ink.accessibleName().contains(first),
                "and a reader who cannot see it is told what it is: "
                        + ink.accessibleName());
        assertTrue(ink instanceof OverlayContribution.Point,
                "typed geometry, never a drawing callback");
    }

    @Test
    void aModuleAsksTheChartToMoveRatherThanMovingIt() {
        Services services = new Services();
        MarkingModule module = new MarkingModule("reference");
        module.attach(services);

        services.request(NavigationRequest.centreOn(
                new SkyPosition(83.8, -5.4), "the reader chose M42"));

        assertEquals(1, services.requested.size());
        assertEquals("the reader chose M42",
                services.requested.get(0).because(),
                "a request says why, so a reader can be told what moved"
                        + " their page");
        assertEquals(ChartViewState.DEFAULT.centre(),
                services.viewState().centre(),
                "and asking is not moving: the chart decides");
    }

    @Test
    void aRequestWithoutAReasonIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> NavigationRequest.centreOn(
                        new SkyPosition(83.8, -5.4), " "),
                "a page that moves for no stated reason is a page that"
                        + " moved by itself, as far as a reader knows");
    }

    @Test
    void twoModulesContributeSideBySideAndNeitherOverwritesTheOther() {
        // The earlier seam took a bare list, so whichever module
        // contributed last replaced the other's ink and neither
        // could take back only its own (review).
        Services services = new Services();
        MarkingModule first = new MarkingModule("first");
        MarkingModule second = new MarkingModule("second");
        first.attach(services);
        second.attach(services);

        String marked = services.inventory().deepSky().get(0).identity();
        services.workingMarks().mark(marked);

        List<OverlayRegistry.Owned> both = services.overlays.collect();
        assertEquals(List.of("first", "second"),
                both.stream().map(OverlayRegistry.Owned::moduleId).toList(),
                "both modules are inking, in the order they attached");
        assertEquals(2, both.stream().map(OverlayRegistry.Owned::key)
                        .distinct().count(),
                "and their keys are distinct, though both call the"
                        + " geometry the same thing: " + both.get(0).key()
                        + " and " + both.get(1).key());

        second.detach();
        assertEquals(List.of("first"),
                services.overlays.collect().stream()
                        .map(OverlayRegistry.Owned::moduleId).toList(),
                "detaching one withdraws its ink and leaves the"
                        + " other's alone");
    }

    @Test
    void aModuleCannotContributeTwiceUnderOneName() {
        Services services = new Services();
        MarkingModule module = new MarkingModule("reference");
        module.attach(services);

        assertThrows(IllegalStateException.class,
                () -> services.contribute("reference", List::of),
                "two registrations under one name is a module that has"
                        + " lost track of its own lifecycle, and"
                        + " silently replacing the first is how ink"
                        + " went missing");
    }

    @Test
    void aModuleThatRepeatsAnIdentityIsRefusedRatherThanDrawnTwice() {
        // Per-module ownership makes keys unique across modules; it
        // does nothing about one module repeating itself (review),
        // and two pieces of ink under one supposedly stable key
        // leave a hit test unable to say which was pointed at.
        Services services = new Services();
        SkyPosition somewhere = new SkyPosition(10.7, 41.3);
        services.contribute("careless", () -> List.of(
                new OverlayContribution.Point("same", "the first",
                        somewhere, InkRole.INTERACTION),
                new OverlayContribution.Point("same", "the second",
                        somewhere, InkRole.INTERACTION)));

        IllegalStateException refused = assertThrows(
                IllegalStateException.class, services.overlays::collect);
        assertTrue(refused.getMessage().contains("careless")
                        && refused.getMessage().contains("same"),
                refused.getMessage());
    }

    @Test
    void twoModulesMayRepeatEachOthersIdentitiesWithoutColliding() {
        Services services = new Services();
        SkyPosition somewhere = new SkyPosition(10.7, 41.3);
        services.contribute("first", () -> List.of(
                new OverlayContribution.Point("m31", "one module's m31",
                        somewhere, InkRole.INTERACTION)));
        services.contribute("second", () -> List.of(
                new OverlayContribution.Point("m31", "another's m31",
                        somewhere, InkRole.INTERACTION)));

        List<String> keys = services.overlays.collect().stream()
                .map(OverlayRegistry.Owned::key).toList();
        assertEquals(List.of("first/m31", "second/m31"), keys,
                "an identity is unique within its module, and neither"
                        + " module has to know the other exists");
    }

    @Test
    void aDetachedModuleHearsNothingFurtherAndHoldsNothing() {
        Services services = new Services();
        MarkingModule module = new MarkingModule("reference");
        module.attach(services);
        services.workingMarks().mark("NGC 224");
        int heardWhileAttached = module.heard.size();

        module.detach();
        assertTrue(!services.overlays.holds("reference"),
                "its ink went with it");
        services.workingMarks().mark("NGC 205");

        assertEquals(heardWhileAttached, module.heard.size(),
                "removing a module removes its feature: it is"
                        + " unsubscribed, not merely ignored");
        assertEquals(List.of("NGC 224", "NGC 205"),
                services.workingMarks().marks(),
                "and the chart's own state is untouched by its"
                        + " departure - no module-specific state left"
                        + " behind, and nothing missing either");
    }
}
