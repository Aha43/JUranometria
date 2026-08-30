package juranometria.tool;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SceneGeography;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;
import juranometria.search.SearchResult;

/**
 * The Sprint 12 chart-options study (issue #103): renders the candidate
 * option combinations by scene surgery - removing a layer's data from
 * an assembled scene before rendering - so the visual consequences can
 * be judged before any production option pipeline exists. Layers whose
 * suppression the renderer alone controls (deep-sky labels without
 * their symbols) cannot be produced this way; the decision document
 * covers them from the existing Sprint 6 label-policy studies.
 *
 * Also times a repaint-only toggle: rendering the same assembled scene
 * with a layer emptied, without any reassembly or catalogue query.
 * Run via "make chart-options-study"; charts land in
 * build/chart-options-study/.
 */
public final class ChartOptionsStudyMain {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    public static void main(String[] args) throws Exception {
        File outDir = new File("build/chart-options-study");
        outDir.mkdirs();
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);

        // M31 at the released 8-degree default: the byte-identity anchor.
        ChartScene m31 = Atlas.assembler().assemble(
                ChartViewState.DEFAULT, WIDTH, HEIGHT);
        write(renderer, m31, outDir, "m31-08-defaults");

        // M42 at 36 degrees: every layer present, then each candidate
        // suppression.
        SearchResult m42 = Atlas.search().search("m 42").get(0);
        ChartViewState wide = ChartViewState.DEFAULT.recenteredAt(
                m42.position(), m42.regionTitle(), m42.identity());
        while (wide.fieldWidthDegrees() < 36.0) {
            wide = wide.zoomOut();
        }
        ChartScene full = Atlas.assembler().assemble(wide, WIDTH, HEIGHT);
        write(renderer, full, outDir, "m42-36-defaults");
        write(renderer, withGeography(full, SceneGeography.EMPTY),
                outDir, "m42-36-geography-off");
        // The decided semantics of the deep-sky toggle: the searched
        // target keeps its symbol and label while the crowd hides.
        write(renderer, withDsos(full, targetOnly(full)),
                outDir, "m42-36-dsos-off");
        write(renderer, withGeography(full, new SceneGeography(
                        full.geography().figureSegments(),
                        full.geography().boundarySegments(), Map.of())),
                outDir, "m42-36-names-off");
        write(renderer, withGeography(full, new SceneGeography(
                        full.geography().figureSegments(), List.of(),
                        full.geography().latinNames())),
                outDir, "m42-36-boundaries-off");
        write(renderer, withGeography(full, new SceneGeography(
                        List.of(), full.geography().boundarySegments(),
                        full.geography().latinNames())),
                outDir, "m42-36-figures-off-names-on");

        // A searched small target at 36 degrees with the deep-sky layer
        // suppressed: the decided target exemption keeps the searched
        // object drawn and labelled while the crowd is hidden.
        SearchResult m57 = Atlas.search().search("m 57").get(0);
        ChartViewState ring = ChartViewState.DEFAULT.recenteredAt(
                m57.position(), m57.regionTitle(), m57.identity());
        while (ring.fieldWidthDegrees() < 36.0) {
            ring = ring.zoomOut();
        }
        ChartScene ringScene = Atlas.assembler().assemble(ring, WIDTH, HEIGHT);
        write(renderer, ringScene, outDir, "m57-36-defaults");
        write(renderer, withDsos(ringScene, targetOnly(ringScene)),
                outDir, "m57-36-dsos-off-target-kept");

        // Repaint-only timing: the same assembled scene, one layer
        // emptied, re-rendered - no reassembly, no catalogue query.
        ChartScene toggled = withGeography(full, SceneGeography.EMPTY);
        renderer.renderToImage(toggled);
        long t0 = System.nanoTime();
        renderer.renderToImage(toggled);
        long t1 = System.nanoTime();
        renderer.renderToImage(full);
        long t2 = System.nanoTime();
        System.out.printf(Locale.ROOT,
                "repaint-only toggle at 36 deg (no reassembly, no query):"
                        + " %d ms without geography, %d ms with%n",
                (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000);
        System.out.println("Charts written to " + outDir);
    }

    /** The decided deep-sky-off content: only the searched target. */
    private static List<DeepSkyObject> targetOnly(ChartScene scene) {
        return scene.deepSkyObjects().stream()
                .filter(dso -> dso.id().equals(scene.targetIdentity()))
                .toList();
    }

    private static ChartScene withGeography(ChartScene scene,
                                            SceneGeography geography) {
        return new ChartScene(scene.viewport(), scene.stars(),
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(), geography);
    }

    private static ChartScene withDsos(ChartScene scene,
                                       List<DeepSkyObject> dsos) {
        return new ChartScene(scene.viewport(), scene.stars(), dsos,
                scene.title(), scene.limitingMagnitude(),
                scene.targetIdentity(), scene.geography());
    }

    private static void write(ChartRenderer renderer, ChartScene scene,
                              File outDir, String name) throws Exception {
        ImageIO.write(renderer.renderToImage(scene), "png",
                new File(outDir, name + ".png"));
        System.out.println("  " + name + ".png");
    }
}
