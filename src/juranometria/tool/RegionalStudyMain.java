package juranometria.tool;

import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.catalog.SkyTiling;
import juranometria.catalog.TiledCatalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartRenderer;

/**
 * The Sprint 6 regional-zoom study (issue #54): renders representative
 * charts at candidate horizontal fields beyond the released 8-degree
 * bound and prints the measurements the decision needs. Everything runs
 * through the real catalogue, viewport, projection, and renderer; only
 * the query radius mirrors SceneAssembler#queryRadiusDegrees (three
 * lines, kept identical by inspection) because the released view state
 * deliberately refuses unreleased field widths.
 *
 * Run via "make regional-study"; charts land in build/regional-study/.
 */
public final class RegionalStudyMain {

    private static final double[] FIELDS = {8.0, 12.0, 18.0, 24.0, 36.0};
    private static final double LIMIT_V = 8.0;
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    private record Target(String name, SkyPosition centre) {
    }

    private static final List<Target> TARGETS = List.of(
            new Target("m31", new SkyPosition(10.684708, 41.268750)),
            new Target("m42", new SkyPosition(83.818667, -5.389667)),
            new Target("m45", new SkyPosition(56.869167, 24.105278)),
            new Target("m13", new SkyPosition(250.421250, 36.461667)),
            new Target("polar", new SkyPosition(37.946619, 89.264135)));

    private RegionalStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        File outDir = new File(args.length > 0 ? args[0] : "build/regional-study");
        outDir.mkdirs();
        TiledCatalogue catalogue = TiledCatalogue.load();
        double margin = catalogue.manifest().maxObjectSemiExtentDegrees();
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);

        System.out.printf(Locale.ROOT,
                "%-6s %5s | %6s %5s | %6s %5s %6s | %5s %5s | %4s %5s | %5s %5s %5s | %5s %5s%n",
                "target", "field", "stars", "drawn", "dsos", "drawn", "labels",
                "pDrw", "pLbl", "tile", "coll", "qry", "asm", "rnd", "edge", "corn");

        for (Target target : TARGETS) {
            for (double field : FIELDS) {
                study(catalogue, renderer, margin, target, field, outDir);
            }
            System.out.println();
        }
        System.out.println("Distortion columns: radial linear scale (sec^2 theta)"
                + " at the horizontal edge and the corner, centre = 1.000.");
        System.out.println("Charts written to " + outDir);
    }

    private static void study(TiledCatalogue catalogue, ChartRenderer renderer,
                              double margin, Target target, double field, File outDir)
            throws Exception {
        // Mirrors SceneAssembler#queryRadiusDegrees.
        double halfW = Math.tan(Math.toRadians(field) / 2.0);
        double halfH = halfW * HEIGHT / (double) WIDTH;
        double corner = Math.toDegrees(Math.atan(Math.hypot(halfW, halfH)));
        double radius = Math.min(180.0, corner + margin);
        SkyRegion query = new SkyRegion(target.centre(), radius);

        long t0 = System.nanoTime();
        List<Star> stars = catalogue.starsIn(query);
        List<DeepSkyObject> dsos = catalogue.deepSkyObjectsIn(query);
        long t1 = System.nanoTime();
        ChartViewport viewport = new ChartViewport(target.centre(), field, WIDTH, HEIGHT);
        ChartScene scene = new ChartScene(viewport, stars, dsos,
                target.name() + " study " + field, LIMIT_V);
        long t2 = System.nanoTime();
        var image = renderer.renderToImage(scene);
        long t3 = System.nanoTime();

        // Visibly drawn = projected inside the frame (and V-limited for stars).
        var projection = new juranometria.project.GnomonicProjection(target.centre());
        var mapping = new juranometria.project.ViewportMapping(viewport);
        int drawnStars = 0;
        for (Star star : stars) {
            if (star.magnitude() <= LIMIT_V && inFrame(projection, mapping, star.position())) {
                drawnStars++;
            }
        }
        int drawnDsos = 0;
        int labels = 0;
        List<double[]> labelAnchors = new java.util.ArrayList<>();
        for (DeepSkyObject dso : dsos) {
            if (!ChartRenderer.hasSymbol(dso) || !inFrame(projection, mapping, dso.position())) {
                continue;
            }
            drawnDsos++;
            if (dso.labelPriority() <= 1) {
                labels++;
                var pixel = mapping.toPixel(projection.project(dso.position()).orElseThrow());
                labelAnchors.add(new double[] {pixel.x(), pixel.y()});
            }
        }
        // Candidate regional policy: draw at true size only (no minimum
        // clamp inflation), keep priority-1 objects, label only true-size.
        // True size uses the renderer's exact viewport scale, not the
        // linear field/pixel ratio (PR #58 review: 3.3% apart at 36 deg).
        int policyDrawn = 0;
        int policyLabels = 0;
        for (DeepSkyObject dso : dsos) {
            if (!ChartRenderer.hasSymbol(dso) || !inFrame(projection, mapping, dso.position())) {
                continue;
            }
            boolean trueSize = Math.toRadians(dso.majorAxisArcmin() / 60.0)
                    * mapping.pixelsPerPlaneUnit() >= 6.0;
            if (trueSize || dso.labelPriority() <= 1) {
                policyDrawn++;
            }
            if (trueSize && dso.labelPriority() <= 1) {
                policyLabels++;
            }
        }
        int collisions = 0;
        for (int i = 0; i < labelAnchors.size(); i++) {
            for (int j = i + 1; j < labelAnchors.size(); j++) {
                if (Math.abs(labelAnchors.get(i)[0] - labelAnchors.get(j)[0]) < 60
                        && Math.abs(labelAnchors.get(i)[1] - labelAnchors.get(j)[1]) < 14) {
                    collisions++;
                }
            }
        }

        double edgeTheta = Math.toRadians(field) / 2.0;
        double edgeScale = 1.0 / Math.pow(Math.cos(Math.atan(Math.tan(edgeTheta))), 2);
        double cornerScale = 1.0 / Math.pow(Math.cos(Math.toRadians(corner)), 2);

        int tiles = SkyTiling.tilesIntersecting(query).size();
        System.out.printf(Locale.ROOT,
                "%-6s %4.0f° | %6d %5d | %6d %5d %6d | %5d %5d | %4d %5d"
                        + " | %4.0fms %4.1fms %4.0fms | %5.3f %5.3f%n",
                target.name(), field, stars.size(), drawnStars, dsos.size(), drawnDsos,
                labels, policyDrawn, policyLabels, tiles, collisions,
                (t1 - t0) / 1e6, (t2 - t1) / 1e6, (t3 - t2) / 1e6, edgeScale, cornerScale);

        ImageIO.write(image, "png", new File(outDir,
                String.format(Locale.ROOT, "%s-%02.0fdeg.png", target.name(), field)));
    }

    private static boolean inFrame(juranometria.project.GnomonicProjection projection,
                                   juranometria.project.ViewportMapping mapping,
                                   SkyPosition position) {
        var plane = projection.project(position);
        if (plane.isEmpty()) {
            return false;
        }
        var pixel = mapping.toPixel(plane.get());
        return pixel.x() >= 0 && pixel.x() < WIDTH && pixel.y() >= 0 && pixel.y() < HEIGHT;
    }
}
