package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;

/**
 * The Sprint 15 coordinate-grid study (issue #132): the candidate
 * ICRS/J2000 equatorial grid - constant-RA meridians and constant-Dec
 * parallels projected through the REAL gnomonic viewport and mapping,
 * subdivided in the sky like the geography pass, clipped per piece,
 * with page-edge labels that yield to the production title block
 * (shared {@code ChartRenderer.titleBlockBounds}). Renders
 * representative pages at every released field width, reports chosen
 * intervals, curve and label counts, suppressed collisions,
 * subdivision counts, the measured maximum curve-approximation error,
 * and warm cost. Run via "make grid-study"; pages land in
 * build/grid-study/.
 *
 * Composition note, stated honestly: the grid belongs BENEATH every
 * other ink. The study emulates that exactly by rendering the grid on
 * white and compositing the finished chart over it, replacing the
 * chart's pure-paper pixels with grid pixels - identical to true
 * under-drawing except at antialiased ink fringes that cross a grid
 * line, where production (#133) blends against the grid instead of
 * the paper. Production parity is verified when #133 regenerates
 * these pages through the real pass, the Sprint 13 pattern.
 */
public final class GridStudyMain {

    // The geometry lives in production (render.EquatorialGrid, issue
    // #133); the study is a consumer - measurements and pages come
    // from the same implementation the renderer draws, never a
    // second predictive copy.

    public static void main(String[] args) throws Exception {
        File outDir = new File("build/grid-study");
        outDir.mkdirs();
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);

        record Page(String name, double ra, double dec, double field,
                    int width, int height) {
        }
        List<Page> pages = List.of(
                new Page("m31-08", 10.684708, 41.268750, 8.0, 900, 700),
                new Page("orion-36", 83.818667, 0.0, 36.0, 900, 700),
                new Page("orion-12", 83.818667, -5.389667, 12.0, 900, 700),
                new Page("orion-03", 83.818667, -5.389667, 3.0, 900, 700),
                new Page("m42-01", 83.818667, -5.389667, 1.0, 900, 700),
                new Page("ra-wrap-24", 0.3, 45.0, 24.0, 900, 700),
                new Page("polar-36", 37.946619, 89.9, 36.0, 900, 700),
                new Page("dec60-18", 37.946619, 60.0, 18.0, 900, 700),
                new Page("crux-18", 186.649563, -63.099093, 18.0, 900, 700),
                new Page("pleiades-08", 56.869167, 24.105278, 8.0, 900, 700),
                new Page("minwin-08", 10.684708, 41.268750, 8.0, 500, 400),
                new Page("letterbox-36", 37.946619, 85.0, 36.0, 900, 4712));

        // The pages ARE production output (issue #133): the renderer
        // draws the grid beneath everything natively, so the gate's
        // paper-pixel compositing is gone. The gate's
        // absent/lines-only/labelled comparison pages remain committed
        // unchanged as the decision's historical evidence.
        System.out.printf(Locale.ROOT,
                "%-14s %5s | %9s %8s | %5s %5s | %6s %5s | %7s %9s%n",
                "page", "field", "RA-step", "Dec-step", "merid", "paral",
                "labels", "supp", "samples", "worst-err");
        long warmNanos = 0;
        int warmed = 0;
        for (Page page : pages) {
            ChartViewState state = new ChartViewState(
                    new SkyPosition(page.ra(), page.dec()), page.field(),
                    8.0, null, null);
            ChartScene scene = Atlas.assembler().assemble(
                    state, page.width(), page.height());
            BufferedImage chart = renderer.renderToImage(scene);
            ImageIO.write(chart, "png",
                    new File(outDir, page.name() + ".png"));

            Graphics2D probe = chart.createGraphics();
            java.awt.Rectangle titleBlock =
                    ChartRenderer.titleBlockBounds(probe, scene);
            probe.dispose();
            long t0 = System.nanoTime();
            EquatorialGrid.Grid grid = EquatorialGrid.gridFor(
                    scene.viewport(), titleBlock);
            long t1 = System.nanoTime();
            warmNanos += t1 - t0;
            warmed++;
            System.out.printf(Locale.ROOT,
                    "%-14s %4.0f\u00b0 | %8.2f\u00b0 %7.1f\u00b0 | %5d %5d"
                            + " | %6d %5d | %7d %9.3f%n",
                    page.name(), page.field(), grid.spec().raStepDegrees(),
                    grid.spec().decStepDegrees(), grid.meridians().size(),
                    grid.parallels().size(), grid.labels().size(),
                    grid.suppressedLabels(), grid.subdivisionSamples(),
                    grid.maxChordErrorPx());
        }
        System.out.printf(Locale.ROOT,
                "mean grid computation: %.2f ms per page (drawing is"
                        + " part of the ordinary paint)%n",
                warmNanos / 1e6 / warmed);
        System.out.println("Pages written to " + outDir);
    }
}

