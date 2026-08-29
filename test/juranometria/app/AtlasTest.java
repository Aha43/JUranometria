package juranometria.app;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;
import juranometria.search.SearchResult;
import juranometria.ui.SceneAssembler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end guards over the wired application data: all-sky search and
 * navigation, and deterministic representative charts outside M31 — the
 * M31 view itself remains guarded by the byte-identical reference image.
 */
class AtlasTest {

    static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT);

    private static int[] render(SkyPosition centre) {
        SceneAssembler assembler = Atlas.assembler();
        ChartScene scene = assembler.assemble(
                ChartViewState.DEFAULT.recenteredAt(centre), 900, 700);
        BufferedImage image = RENDERER.renderToImage(scene);
        return image.getRGB(0, 0, 900, 700, null, 0, 900);
    }

    @Test
    void offlineSearchFindsTheWholeSky() {
        List<SearchResult> m42 = Atlas.search().search("M42");
        assertEquals("NGC 1976", m42.get(0).identity());
        assertEquals("M 42", m42.get(0).label());
        assertTrue(Atlas.assembler().fits(m42.get(0).position(), 8.0),
                "all-sky coverage accepts the Orion field at every step");

        assertEquals("NGC 104",
                Atlas.search().search("47 Tuc Cluster").get(0).identity());
        assertEquals("NGC 5457", Atlas.search().search("m101").get(0).identity());
        assertTrue(Atlas.search().search("TYC 4628-237-1").get(0).label()
                .equals("TYC 4628-237-1"), "Polaris by TYC id");
    }

    @Test
    void representativeChartsOutsideM31AreDeterministic() {
        // Orion, an RA-wrap field, and a far-southern field render twice
        // identically and are visibly populated.
        for (SkyPosition centre : new SkyPosition[] {
                new SkyPosition(83.818667, -5.389667),
                new SkyPosition(0.3, 45.0),
                new SkyPosition(6.0, -72.0)}) {
            int[] first = render(centre);
            assertArrayEquals(first, render(centre),
                    "chart at " + centre + " must render deterministically");
            int ink = 0;
            for (int pixel : first) {
                if (pixel != 0xFFFFFFFF) {
                    ink++;
                }
            }
            assertTrue(ink > 2000, "chart at " + centre + " is visibly populated");
        }
    }

    @Test
    void navigationIsUnrestrictedAcrossTheDeclaredCoverage() {
        SceneAssembler assembler = Atlas.assembler();
        assertTrue(assembler.fits(new SkyPosition(0.1, -89.5), 8.0),
                "near the south pole");
        assertTrue(assembler.fits(new SkyPosition(359.9, 0.0), 1.0), "at the RA wrap");
        assertEquals(36.0, assembler.widestFittingFieldDegrees(
                new SkyPosition(180.0, -45.0)).orElseThrow(),
                "the widest regional step fits anywhere under all-sky coverage");
        assertTrue(assembler.maxPageHeightPx(new SkyPosition(83.8, -5.4), 8.0, 900) > 10000,
                "the projection-sanity cap sits far beyond real windows");
    }

    @Test
    void theAssemblerMarginComesFromTheManifestNotAConstant() {
        assertEquals(5.39, Atlas.assembler().objectExtentMarginDegrees(),
                "the pack's declared LMC semi-extent drives the query margin");
    }

    @Test
    void largeObjectsReachingIntoTheFrameAreNeverOmitted() {
        // Codex review, Sprint 5 release finding 1: with the old 1.5-degree
        // margin, a centre 8 degrees north of the LMC omitted it even
        // though its 5.38-degree semi-extent reaches into the frame.
        ChartScene lmcEdge = Atlas.assembler().assemble(ChartViewState.DEFAULT
                .recenteredAt(new SkyPosition(80.893750, -61.756111)), 900, 700);
        assertTrue(lmcEdge.deepSkyObjects().stream()
                        .anyMatch(dso -> dso.id().equals("ESO056-115")),
                "the LMC must be in the scene when its ellipse reaches the frame");

        // The Hyades (329', semi-extent 2.74 degrees), centre 7 degrees away:
        // outside the old reach of 6.56, inside the manifest-driven 10.45.
        ChartScene hyadesEdge = Atlas.assembler().assemble(ChartViewState.DEFAULT
                .recenteredAt(new SkyPosition(66.725, 22.866667)), 900, 700);
        assertTrue(hyadesEdge.deepSkyObjects().stream()
                        .anyMatch(dso -> dso.id().equals("C041")),
                "the Hyades must be in the scene from seven degrees away");
    }

    @Test
    void theM42JourneyGainsOrionExactlyAtThePolicyThresholds() {
        // Sprint 7 acceptance: search M42, zoom out - Orion's figure and
        // name establish the neighbourhood while M42 stays centred and
        // titled; each layer appears exactly at its decided threshold.
        var m42 = Atlas.search().search("m 42").get(0);
        // The journey runs through the real controller with the real
        // coverage predicate, so the reset at the end is the actual
        // transition the toolbar performs (Sprint 7 Codex review).
        var controller = new juranometria.ui.ChartViewController(
                Atlas.assembler()::fits);
        controller.recenter(m42.position(), m42.regionTitle(), m42.identity());

        var at8 = Atlas.assembler().assemble(controller.state(), 900, 700);
        assertEquals(0, at8.geography().figureSegments().size(),
                "the released 8-degree page carries no geography");
        assertEquals(0, at8.geography().boundarySegments().size());

        controller.zoomOut(); // 12 degrees
        var at12 = Atlas.assembler().assemble(controller.state(), 900, 700);
        assertTrue(!at12.geography().figureSegments().isEmpty(),
                "figures arrive exactly at 12 degrees");
        assertTrue(at12.geography().latinNames().containsValue("Orion"),
                "Orion is named on its visible figure");
        assertEquals(0, at12.geography().boundarySegments().size(),
                "boundaries wait for 18 degrees");

        controller.zoomOut(); // 18 degrees
        var at18 = Atlas.assembler().assemble(controller.state(), 900, 700);
        assertTrue(!at18.geography().boundarySegments().isEmpty(),
                "boundaries arrive exactly at 18 degrees");

        while (controller.state().fieldWidthDegrees() < 36.0) {
            controller.zoomOut();
        }
        var at36 = Atlas.assembler().assemble(controller.state(), 900, 700);
        assertEquals("M 42 · Great Orion Nebula region", at36.title(),
                "M 42 still titles its page at the widest field");
        assertEquals(m42.position(), at36.viewport().centre());
        assertTrue(at36.geography().figureSegments().stream().anyMatch(
                        segment -> segment.constellationId().equals("Ori")),
                "Orion's figure is on the widest page");
        assertTrue(at36.geography().latinNames().size() >= 4,
                "the neighbourhood is named around Orion");
        // Every name belongs to a constellation with figure ink present.
        var present = at36.geography().figureSegments().stream()
                .map(juranometria.geo.GeoSegment::constellationId)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(present.containsAll(at36.geography().latinNames().keySet()),
                "names attach only to constellations whose figures are present");

        // Sprint 8: a real pan from the wide searched view departs the
        // target honestly - label and identity clear together and the
        // assembled page titles by its coordinates - while field and
        // magnitude carry on around the panned centre.
        assertTrue(controller.pan(controller.state().centre(),
                        new juranometria.project.PlanePoint(0.08, -0.05)),
                "the pan from the searched wide view is accepted");
        assertEquals(null, controller.state().targetLabel());
        assertEquals(null, controller.state().targetIdentity());
        assertEquals(36.0, controller.state().fieldWidthDegrees());
        var panned = Atlas.assembler().assemble(controller.state(), 900, 700);
        assertTrue(panned.title().contains("h ")
                        && panned.title().contains("°"),
                "the panned page titles by coordinates: " + panned.title());

        // The actual reset transition from the searched, zoomed, and
        // panned view: it restores the exact released default state, and
        // the decided geography default at 8 degrees is none.
        controller.reset();
        assertEquals(juranometria.chart.ChartViewState.DEFAULT,
                controller.state(),
                "reset from the wide searched view restores the default");
        var reset = Atlas.assembler().assemble(controller.state(), 900, 700);
        assertEquals("M31 · Andromeda Galaxy region", reset.title());
        assertEquals(juranometria.chart.SceneGeography.EMPTY, reset.geography(),
                "the default 8-degree page carries no geography");
    }

    @Test
    void resetStillMeansM31() {
        assertEquals(new SkyPosition(10.684708, 41.268750),
                ChartViewState.DEFAULT.centre());
        assertEquals(8.0, ChartViewState.DEFAULT.fieldWidthDegrees());
        assertEquals(8.0, ChartViewState.DEFAULT.limitingMagnitude());
    }
}
