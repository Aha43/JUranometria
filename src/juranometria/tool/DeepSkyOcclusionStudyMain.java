package juranometria.tool;

import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

/**
 * Which deep-sky symbols hide which, and what the stacking rule of
 * issue #201 changes (Sprint 23).
 *
 * <p>The atlas's founding page names three galaxies and showed two.
 * M31's disc is opaque and 178 arcminutes long; the bundled rows
 * reach the page as NGC 205, NGC 221, NGC 224, so it was painted
 * last and swallowed M 32 whole. The label still drew - labels are a
 * later pass - leaving a name with no mark to attach it to.
 *
 * <p>Everything here is measured through production seams:
 * {@link ChartRenderer#drawnMarks} publishes the placements and
 * the order they are painted in, the application's own assembler
 * builds the page, and each object's surviving ink is measured by rendering
 * the same page again without it. The study owns no geometry of its
 * own, so it cannot come to measure a chart the atlas stopped
 * drawing.
 */
public final class DeepSkyOcclusionStudyMain {

    private DeepSkyOcclusionStudyMain() {
    }

    private static final File DIR = new File("docs/studies/deep-sky-occlusion");

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        List<DeepSkyObject> pack = DeepSkyVocabularyStudyMain.wholePack();

        System.out.println("# Overlapping deep-sky symbols");
        System.out.println();
        System.out.println("Measured by `make deep-sky-occlusion-study`"
                + " over the bundled all-sky pack and the released"
                + " default page. Every placement, order and pixel"
                + " comes from production: `ChartRenderer.drawnMarks`"
                + " publishes what is painted and in what order, and"
                + " an object's surviving ink is measured by"
                + " rendering the same page again without it.");
        System.out.println();

        containmentCensus(pack);
        acrossPages();
        defaultPage();
        System.out.println();
        System.out.println("Images in"
                + " [`docs/studies/deep-sky-occlusion/`](.).");
    }

    // ------------------------------------------------------------------
    // What the pack contains, and why the sky's own geometry is only
    // an upper bound on it.

    private static void containmentCensus(List<DeepSkyObject> pack) {
        int pairs = 0;
        int larger = 0;
        int arcminute = 0;
        java.util.Map<String, int[]> holders = new java.util.LinkedHashMap<>();
        List<DeepSkyObject> sized = new ArrayList<>(pack);
        sized.sort(Comparator.comparing(DeepSkyObject::id));
        for (DeepSkyObject big : sized) {
            if (!fillsOpaquely(big) || big.majorAxisArcmin() <= 0.0) {
                continue;
            }
            for (DeepSkyObject small : sized) {
                if (small.id().equals(big.id()) || !inside(big, small)) {
                    continue;
                }
                pairs++;
                if (big.majorAxisArcmin() <= small.majorAxisArcmin()) {
                    continue;
                }
                larger++;
                if (small.majorAxisArcmin() < 1.0
                        && big.majorAxisArcmin() < 2.0) {
                    arcminute++;
                } else if (big.majorAxisArcmin() >= 10.0) {
                    int[] tally = holders.computeIfAbsent(big.id(),
                            k -> new int[] {0, (int) Math.round(
                                    big.majorAxisArcmin() * 100.0)});
                    tally[0]++;
                }
            }
        }

        System.out.println("## What the pack contains");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "Asking the sky rather than any one page, **%d** of the"
                        + " %d bundled rows' centres fall inside a"
                        + " galaxy's disc, and in **%d** of those the"
                        + " disc is the larger of the two. Only the"
                        + " galaxy ellipse paints an opaque interior -"
                        + " every other symbol in the vocabulary is an"
                        + " outline and crosses nothing out - so a"
                        + " galaxy is the only mark that can bury"
                        + " another.%n%n",
                pairs, pack.size(), larger);

        System.out.printf(Locale.ROOT,
                "**That figure is an upper bound, not the answer.**"
                        + " %d of the %d are pairs of sub-arcminute"
                        + " galaxies inside a disc under two"
                        + " arcminutes - NED components of one"
                        + " catalogued system, a few tenths of an"
                        + " arcminute apart. At any field width a"
                        + " reader actually uses, the"
                        + " practical-minimum clamp enlarges both to"
                        + " the same drawn size, so neither contains"
                        + " the other on the page however the sky is"
                        + " arranged. Sky geometry cannot answer this;"
                        + " only drawn geometry can, which is what the"
                        + " next section measures.%n%n",
                arcminute, larger);

        System.out.println("What survives that reasoning is a"
                + " smaller and much more interesting population: a"
                + " genuinely large galaxy holding catalogued objects"
                + " of its own. Grouped by the disc doing the"
                + " covering:");
        System.out.println();
        System.out.println("| the disc | its size | objects inside it |");
        System.out.println("|---|---:|---:|");
        holders.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, int[]>comparingByValue(
                        Comparator.comparingInt(v -> -v[0])))
                .limit(12)
                .forEach(e -> System.out.printf(Locale.ROOT,
                        "| %s | %.0f' | %d |%n", e.getKey(),
                        e.getValue()[1] / 100.0, e.getValue()[0]));
        System.out.println();
        System.out.printf(Locale.ROOT,
                "**%d** discs, holding **%d** objects between them."
                        + " The Magellanic Clouds dominate the list"
                        + " and explain themselves: the atlas draws"
                        + " the Large Cloud as one filled ellipse"
                        + " nearly eleven degrees across, and the"
                        + " hundreds of clusters and nebulae the"
                        + " catalogue records *within* it all fall"
                        + " inside that disc. Under storage order"
                        + " every one of them is at the mercy of"
                        + " where its row happens to sit.%n%n",
                holders.size(), holders.values().stream()
                        .mapToInt(v -> v[0]).sum());
        System.out.println("M 31 with M 32 is the same defect on a"
                + " page every reader opens.");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // What the two orders do on real pages, at drawn sizes.

    private record Page(String name, double ra, double dec, String why) {
    }

    private static final List<Page> PAGES = List.of(
            new Page("m31-default", 10.684708, 41.268750,
                    "the page the application opens on"),
            new Page("lmc", 80.894, -69.756,
                    "the Large Magellanic Cloud, one 11-degree disc"),
            new Page("smc", 13.187, -72.829,
                    "the Small Cloud"),
            new Page("m33", 23.462, 30.660,
                    "M 33 and the HII regions catalogued inside it"),
            new Page("virgo", 187.706, 12.391,
                    "the Virgo cluster's crowd of galaxies"),
            new Page("m51", 202.470, 47.195,
                    "an interacting pair catalogued as two"));

    private static final double[] FIELDS = {2.0, 8.0, 36.0};

    private static void acrossPages() {
        ChartRenderer renderer =
                new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT);
        ChartOptions options = ChartOptions.DEFAULTS;

        System.out.println("## What each order does, at drawn sizes");
        System.out.println();
        System.out.println("A mark is **fully covered** when some"
                + " filled disc painted after it contains the whole"
                + " of its outline: exact geometry over the outlines"
                + " `drawnMarks` publishes, at the sizes the page"
                + " really draws, clamp included. Storage order is"
                + " the order the scene's own list arrives in - what"
                + " the renderer used before this issue.");
        System.out.println();
        System.out.println("| page | field | symbols | fully covered,"
                + " storage order | fully covered, stacking rule |");
        System.out.println("|---|---:|---:|---:|---:|");
        int wasCovered = 0;
        int nowCovered = 0;
        for (Page page : PAGES) {
            for (double field : FIELDS) {
                ChartScene scene = juranometria.app.Atlas.assembler()
                        .assemble(new ChartViewState(
                                new juranometria.chart.SkyPosition(
                                        page.ra(), page.dec()),
                                field, 8.0), WIDTH, HEIGHT);
                List<ChartRenderer.DrawnMark> stacked =
                        deepSkyOf(renderer.drawnMarks(scene, options));
                List<ChartRenderer.DrawnMark> stored =
                        inStorageOrder(stacked, scene);
                int before = fullyCovered(stored);
                int after = fullyCovered(stacked);
                wasCovered += before;
                nowCovered += after;
                System.out.printf(Locale.ROOT,
                        "| %s | %.0f° | %d | %d | %d |%n",
                        page.name(), field, stacked.size(), before, after);
            }
        }
        System.out.println();
        System.out.printf(Locale.ROOT,
                "Across these %d pages, storage order fully buried"
                        + " **%d** symbols that the reader was"
                        + " nevertheless told about. The stacking rule"
                        + " leaves **%d**.%n%n",
                PAGES.size() * FIELDS.length, wasCovered, nowCovered);
        if (nowCovered > 0) {
            System.out.println("The remainder are the honest cases:"
                    + " objects whose outlines coincide so closely"
                    + " that no order can show both. They are"
                    + " classified rather than fixed - see the"
                    + " decision.");
            System.out.println();
        }
    }

    private static List<ChartRenderer.DrawnMark> deepSkyOf(
            List<ChartRenderer.DrawnMark> marks) {
        List<ChartRenderer.DrawnMark> out = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.kind() == ChartRenderer.DrawnMark.Kind.DEEP_SKY) {
                out.add(mark);
            }
        }
        return out;
    }

    /** The same marks, back in the order the scene's list carries. */
    private static List<ChartRenderer.DrawnMark> inStorageOrder(
            List<ChartRenderer.DrawnMark> marks, ChartScene scene) {
        List<String> order = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            order.add(dso.id());
        }
        List<ChartRenderer.DrawnMark> out = new ArrayList<>(marks);
        out.sort(Comparator.comparingInt(
                mark -> order.indexOf(mark.deepSky().id())));
        return out;
    }

    /** How many marks a later filled disc swallows whole. */
    private static int fullyCovered(List<ChartRenderer.DrawnMark> ordered) {
        int covered = 0;
        for (int i = 0; i < ordered.size(); i++) {
            for (int j = i + 1; j < ordered.size(); j++) {
                if (fillsOpaquely(ordered.get(j).deepSky())
                        && contains(ordered.get(j), ordered.get(i))) {
                    covered++;
                    break;
                }
            }
        }
        return covered;
    }

    /** Only the galaxy ellipse paints an interior (production's rule). */
    private static boolean fillsOpaquely(DeepSkyObject dso) {
        return ChartRenderer.symbolForType(dso.type())
                == ChartRenderer.Symbol.ELLIPSE;
    }

    /**
     * Is one object's centre inside another's sky ellipse? Worked in
     * arcminutes of true angular separation, with the position angle
     * measured east of north as the catalogue records it.
     */
    private static boolean inside(DeepSkyObject big, DeepSkyObject small) {
        double a = big.majorAxisArcmin() / 2.0;
        double b = big.minorAxisArcmin() / 2.0;
        if (a <= 0.0 || b <= 0.0) {
            return false;
        }
        double decRad = Math.toRadians(big.position().decDegrees());
        double east = (small.position().raDegrees()
                - big.position().raDegrees()) * 60.0 * Math.cos(decRad);
        double north = (small.position().decDegrees()
                - big.position().decDegrees()) * 60.0;
        if (Math.abs(east) > 3.0 * a || Math.abs(north) > 3.0 * a) {
            return false;
        }
        double pa = Math.toRadians(big.positionAngleDegrees());
        // Along the major axis, and across it.
        double along = north * Math.cos(pa) + east * Math.sin(pa);
        double across = -north * Math.sin(pa) + east * Math.cos(pa);
        return (along * along) / (a * a) + (across * across) / (b * b) <= 1.0;
    }

    // ------------------------------------------------------------------
    // The released default page, which is where a reader meets the
    // defect.

    private static void defaultPage() throws IOException {
        // The application's own assembler and its own default state,
        // so this is the page a reader actually opens on.
        ChartScene scene = juranometria.app.Atlas.assembler()
                .assemble(ChartViewState.DEFAULT, WIDTH, HEIGHT);
        ChartRenderer renderer =
                new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT);
        ChartOptions options = ChartOptions.DEFAULTS;

        System.out.println("## The released default page");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "M 31 at a %.0f-degree field, stars to V %.1f - the"
                        + " page the application opens on.%n%n",
                ChartViewState.DEFAULT.fieldWidthDegrees(),
                ChartViewState.DEFAULT.limitingMagnitude());

        List<ChartRenderer.DrawnMark> marks = renderer.drawnMarks(scene, options);
        List<ChartRenderer.DrawnMark> dsos = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.kind() == ChartRenderer.DrawnMark.Kind.DEEP_SKY) {
                dsos.add(mark);
            }
        }

        System.out.println("### The order the renderer paints in");
        System.out.println();
        System.out.println("Published by `drawnMarks`, which is the"
                + " list the renderer paints from - so this is the"
                + " drawing order itself, not a description of it.");
        System.out.println();
        System.out.println("| painted | object | drawn axes (px) |"
                + " symbol ink (px) |");
        System.out.println("|---:|---|---:|---:|");
        int position = 0;
        for (ChartRenderer.DrawnMark mark : dsos) {
            java.awt.geom.Rectangle2D box = mark.outline().getBounds2D();
            System.out.printf(Locale.ROOT, "| %d | %s | %.0f x %.0f | %d |%n",
                    ++position, mark.deepSky().id(),
                    Math.max(box.getWidth(), box.getHeight()),
                    Math.min(box.getWidth(), box.getHeight()),
                    symbolInk(renderer, scene, mark.deepSky().id()));
        }
        System.out.println();
        System.out.println("**Symbol ink** is measured, not assumed:"
                + " the page is rendered again with that one object"
                + " removed, and the pixels that change inside its own"
                + " outline are the ones its symbol contributes."
                + " Labels are switched off and the searched target"
                + " cleared first, because under the defect M 32's"
                + " label went on drawing while its ellipse was gone"
                + " entirely - counting the label would have reported"
                + " ink for a mark no reader could see. Zero means"
                + " exactly that: a label with nothing under it.");
        System.out.println();

        andromedaTrio(renderer, scene, options, dsos);

        ImageIO.write(renderer.renderToImage(scene), "png",
                new File(DIR, "m31-default.png"));
    }

    /**
     * The three galaxies the page names, and the geometry that
     * decides whether a reader can see all three.
     */
    private static void andromedaTrio(ChartRenderer renderer,
                                      ChartScene scene,
                                      ChartOptions options,
                                      List<ChartRenderer.DrawnMark> dsos)
            throws IOException {
        ChartRenderer.DrawnMark m31 = named(dsos, "NGC 224");
        ChartRenderer.DrawnMark m32 = named(dsos, "NGC 221");
        ChartRenderer.DrawnMark m110 = named(dsos, "NGC 205");
        if (m31 == null || m32 == null || m110 == null) {
            System.out.println("### Andromeda's three galaxies");
            System.out.println();
            System.out.println("Not all three are on this page.");
            return;
        }

        System.out.println("### Andromeda's three galaxies");
        System.out.println();
        System.out.println("| galaxy | painted | inside M 31's disc |"
                + " symbol ink (px) |");
        System.out.println("|---|---:|---|---:|");
        for (ChartRenderer.DrawnMark mark
                : List.of(m31, m32, m110)) {
            System.out.printf(Locale.ROOT, "| %s | %d | %s | %d |%n",
                    mark.deepSky().id(), dsos.indexOf(mark) + 1,
                    mark == m31 ? "-"
                            : (contains(m31, mark) ? "**entirely**"
                                    : "partly"),
                    symbolInk(renderer, scene, mark.deepSky().id()));
        }
        System.out.println();
        System.out.printf(Locale.ROOT,
                "M 31's disc **entirely contains** M 32's ellipse:"
                        + " every point of the smaller mark lies"
                        + " within the larger one, and the larger one"
                        + " is filled. Painting M 31 second - which"
                        + " storage order did - therefore leaves M 32"
                        + " no ink at all, whatever the page. The"
                        + " stacking rule paints it %s instead, and"
                        + " M 32 keeps its outline.%n%n",
                dsos.indexOf(m31) < dsos.indexOf(m32) ? "first" : "second");
    }

    private static boolean contains(ChartRenderer.DrawnMark outer,
                                    ChartRenderer.DrawnMark inner) {
        Area area = new Area(outer.outline());
        Area other = new Area(inner.outline());
        other.subtract(area);
        return other.isEmpty();
    }

    private static ChartRenderer.DrawnMark named(
            List<ChartRenderer.DrawnMark> marks, String id) {
        for (ChartRenderer.DrawnMark mark : marks) {
            if (id.equals(mark.deepSky().id())) {
                return mark;
            }
        }
        return null;
    }

    /**
     * The pixels this object's <strong>symbol</strong> leaves on the
     * page: render it, render the page without it, and count what
     * changed inside the mark's own outline.
     *
     * <p>Labels are switched off and the searched target cleared
     * first (#201 review). Under the defect M 32's label went on
     * drawing while its ellipse was gone entirely, so a measurement
     * that counted the label would have reported ink for a mark no
     * reader could see - which is the exact shape of the bug this
     * study exists to measure. Confining the count to the mark's own
     * outline keeps a neighbour from answering for it.
     */
    static int symbolInk(ChartRenderer renderer, ChartScene page,
                         String id) {
        ChartScene scene = anonymous(page);
        ChartOptions options = symbolsOnly();
        ChartRenderer.DrawnMark mark = null;
        for (ChartRenderer.DrawnMark m
                : renderer.drawnMarks(scene, options)) {
            if (m.kind() == ChartRenderer.DrawnMark.Kind.DEEP_SKY
                    && id.equals(m.deepSky().id())) {
                mark = m;
            }
        }
        if (mark == null) {
            return 0;
        }
        BufferedImage with = renderer.renderToImage(scene, options);
        BufferedImage none = renderer.renderToImage(
                without(scene, id), options);
        java.awt.Shape outline = mark.outline();
        java.awt.geom.Rectangle2D box = outline.getBounds2D();
        int x0 = Math.max(0, (int) Math.floor(box.getMinX()));
        int x1 = Math.min(with.getWidth() - 1, (int) Math.ceil(box.getMaxX()));
        int y0 = Math.max(0, (int) Math.floor(box.getMinY()));
        int y1 = Math.min(with.getHeight() - 1, (int) Math.ceil(box.getMaxY()));
        int changed = 0;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (outline.contains(x + 0.5, y + 0.5)
                        && with.getRGB(x, y) != none.getRGB(x, y)) {
                    changed++;
                }
            }
        }
        return changed;
    }

    /** The chart with no deep-sky labels: the symbol pass alone. */
    private static ChartOptions symbolsOnly() {
        ChartOptions d = ChartOptions.DEFAULTS;
        return new ChartOptions(d.deepSkyObjects(), false,
                d.constellationFigures(), d.constellationBoundaries(),
                d.constellationNames(), d.starNames(), d.bayerLetters(),
                d.flamsteedNumbers(), d.equatorialGrid(), d.titleBlock(),
                d.magnitudeKey(), d.galaxies(), d.openClusters(),
                d.globularClusters(), d.nebulae(), d.planetaryNebulae());
    }

    /** The same page with no searched target, so nothing is exempt. */
    private static ChartScene anonymous(ChartScene scene) {
        return new ChartScene(scene.viewport(), scene.stars(),
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), null, scene.geography());
    }

    /** The same page, minus one object. */
    static ChartScene without(ChartScene scene, String id) {
        List<DeepSkyObject> kept = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!id.equals(dso.id())) {
                kept.add(dso);
            }
        }
        return new ChartScene(scene.viewport(), scene.stars(), kept,
                scene.title(), scene.limitingMagnitude(),
                scene.targetIdentity(), scene.geography());
    }
}
