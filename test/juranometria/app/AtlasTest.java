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
    void resetStillMeansM31() {
        assertEquals(new SkyPosition(10.684708, 41.268750),
                ChartViewState.DEFAULT.centre());
        assertEquals(8.0, ChartViewState.DEFAULT.fieldWidthDegrees());
        assertEquals(8.0, ChartViewState.DEFAULT.limitingMagnitude());
    }
}
