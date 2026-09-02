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

        private ChartServices services;
        private Runnable unsubscribe;
        private final List<WorkingMarksModel.Change> heard = new ArrayList<>();

        @Override
        public String name() {
            return "reference";
        }

        @Override
        public void attach(ChartServices services) {
            this.services = services;
            this.unsubscribe = services.workingMarks().onChange(heard::add);
        }

        @Override
        public void detach() {
            if (unsubscribe != null) {
                unsubscribe.run();
            }
            unsubscribe = null;
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
        final List<OverlayContribution> contributed = new ArrayList<>();

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

        @Override public void contribute(List<OverlayContribution> geometry) {
            contributed.clear();
            contributed.addAll(geometry);
        }
    }

    @Test
    void aModuleReadsThePageMarksItAndOffersGeometryForTheChartToInk() {
        Services services = new Services();
        MarkingModule module = new MarkingModule();
        module.attach(services);

        String first = services.inventory().deepSky().get(0).identity();
        services.workingMarks().mark(first);
        services.contribute(module.overlay());

        assertEquals(1, services.contributed.size(),
                "one cross for the one marked object");
        OverlayContribution ink = services.contributed.get(0);
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
        MarkingModule module = new MarkingModule();
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
    void aDetachedModuleHearsNothingFurtherAndHoldsNothing() {
        Services services = new Services();
        MarkingModule module = new MarkingModule();
        module.attach(services);
        services.workingMarks().mark("NGC 224");
        int heardWhileAttached = module.heard.size();

        module.detach();
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
