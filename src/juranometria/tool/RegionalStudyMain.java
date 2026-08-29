package juranometria.tool;

import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.catalog.SkyTiling;
import juranometria.catalog.TiledCatalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
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
            new Target("polar", new SkyPosition(37.946619, 89.264135)),
            // Sprint finish (#57): a far-southern giant object and an
            // RA-wrap field, so the representative pages reproduce here.
            new Target("lmc", new SkyPosition(80.893750, -69.756111)),
            new Target("rawrap", new SkyPosition(359.457625, -32.591028)));

    private RegionalStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        File outDir = new File(args.length > 0 ? args[0] : "build/regional-study");
        outDir.mkdirs();
        TiledCatalogue catalogue = TiledCatalogue.load();
        double margin = catalogue.manifest().maxObjectSemiExtentDegrees();
        juranometria.ui.SceneAssembler assembler =
                juranometria.ui.SceneAssembler.allSky(catalogue, margin,
                        juranometria.geo.ConstellationGeography.load());
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);

        System.out.printf(Locale.ROOT,
                "%-6s %5s | %6s %5s | %6s %5s %6s | %5s %5s | %4s %5s"
                        + " | %5s %5s %5s %5s | %5s %5s%n",
                "target", "field", "stars", "drawn", "dsos", "drawn", "labels",
                "pDrw", "pLbl", "tile", "coll", "qry", "scn", "rnd", "e2e",
                "edge", "corn");

        for (Target target : TARGETS) {
            for (double field : FIELDS) {
                study(catalogue, assembler, renderer, margin, target, field, outDir);
            }
            System.out.println();
        }
        System.out.println("Timing columns, one warm run per row: qry = catalogue"
                + " query, scn = scene construction (study scene); e2e = the real"
                + " SceneAssembler.assemble end to end (query + construction);"
                + " rnd = render of the real assembled page, geography included.");
        System.out.println("Distortion columns: radial linear scale (sec^2 theta)"
                + " at the horizontal edge and the corner, centre = 1.000.");
        System.out.println("Charts written to " + outDir);
    }

    private static void study(TiledCatalogue catalogue,
                              juranometria.ui.SceneAssembler assembler,
                              ChartRenderer renderer, double margin,
                              Target target, double field, File outDir)
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

        // The real production path, end to end: every study field is a
        // released zoom step since #55, so the view state accepts it and
        // SceneAssembler.assemble (catalogue query + scene construction)
        // can be measured as the application runs it (Codex review,
        // Sprint 6 finding 1). Warmed by the queries above.
        ChartViewState state = new ChartViewState(
                target.centre(), field, LIMIT_V, null, null);
        long e0 = System.nanoTime();
        ChartScene assembled = assembler.assemble(state, WIDTH, HEIGHT);
        long e1 = System.nanoTime();
        // rnd times the real assembled page - constellation geography
        // included since #65 (PR #69 review) - the image that is written.
        long r0 = System.nanoTime();
        var image = renderer.renderToImage(assembled);
        long r1 = System.nanoTime();

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
        // Since #56 the regional detail policy is the implementation, not
        // a prediction: measure through the renderer's own policy object.
        // (These study scenes carry no searched target, exactly like the
        // decision's numbers; the target exemption is renderer-tested.)
        var policy = new juranometria.render.RegionalDetailPolicy(
                scene, mapping.pixelsPerPlaneUnit());
        int policyDrawn = 0;
        int policyLabels = 0;
        for (DeepSkyObject dso : dsos) {
            if (!inFrame(projection, mapping, dso.position())) {
                continue;
            }
            if (policy.drawn(dso)) {
                policyDrawn++;
            }
            if (policy.labelled(dso)) {
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
                        + " | %4.1f %5.2f %4.0f %5.1f | %5.3f %5.3f%n",
                target.name(), field, stars.size(), drawnStars, dsos.size(), drawnDsos,
                labels, policyDrawn, policyLabels, tiles, collisions,
                (t1 - t0) / 1e6, (t2 - t1) / 1e6, (r1 - r0) / 1e6,
                (e1 - e0) / 1e6, edgeScale, cornerScale);

        // The committed page is the product truth: the assembled scene
        // through the real seam, constellation geography included (#65).
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
