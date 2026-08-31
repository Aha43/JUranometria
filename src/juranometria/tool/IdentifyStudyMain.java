package juranometria.tool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

/**
 * The point-and-identify gate's measurements (Sprint 19, issue
 * #168): what a reader can actually hit on a real page, how often
 * pointing is ambiguous, and how large a tolerance the chart's own
 * ink demands.
 *
 * <p>Everything is measured through {@link ChartRenderer#drawnMarks}
 * - the placements the renderer paints from - so these numbers
 * describe the atlas as drawn, not a model of it.
 */
public final class IdentifyStudyMain {

    private IdentifyStudyMain() {
    }

    /** A page worth measuring, and why it is in the study. */
    private record Page(String name, SkyPosition centre, double field,
                        String why) {
    }

    private static final List<Page> PAGES = List.of(
            new Page("m31-08", new SkyPosition(10.68, 41.27), 8.0,
                    "the released default page"),
            new Page("orion-36", new SkyPosition(83.8, 0.0), 36.0,
                    "the widest field, a constellation at a glance"),
            new Page("sagittarius-08", new SkyPosition(271.0, -24.0), 8.0,
                    "the densest sky the pack carries"),
            new Page("sagittarius-01", new SkyPosition(271.0, -24.0), 1.0,
                    "the narrowest field, where marks are far apart"),
            new Page("crux-18", new SkyPosition(186.6, -60.0), 18.0,
                    "far southern, with overlapping cluster symbols"),
            new Page("polaris-18", new SkyPosition(37.9, 89.26), 18.0,
                    "a polar page, where projection distorts most"),
            new Page("wrap-18", new SkyPosition(0.0, 5.0), 18.0,
                    "across RA 0"),
            new Page("virgo-08", new SkyPosition(187.7, 12.4), 8.0,
                    "a galaxy cluster: many symbols, few stars"),
            new Page("empty-08", new SkyPosition(40.0, -35.0), 8.0,
                    "quiet sky, where clicks often hit nothing"));

    /** Screen tolerances to sweep, in page pixels. */
    private static final double[] TOLERANCES = {0.0, 2.0, 3.0, 4.0, 6.0, 8.0, 12.0};

    private static final int PAGE_WIDTH = 900;
    private static final int PAGE_HEIGHT = 700;

    public static void main(String[] args) {
        System.out.println("# Point-and-identify study (issue #168)");
        System.out.println();
        System.out.println("Measured through ChartRenderer.drawnMarks on "
                + PAGES.size() + " pages at " + PAGE_WIDTH + "x"
                + PAGE_HEIGHT + ".");
        System.out.println();
        markInventory();
        starDotSizes();
        blindClicks();
        aimedClicks();
        ambiguity();
    }

    private static ChartScene scene(Page page) {
        return Atlas.assembler().assemble(
                new ChartViewState(page.centre(), page.field(), 8.0,
                        null, null),
                PAGE_WIDTH, PAGE_HEIGHT);
    }

    private static List<ChartRenderer.DrawnMark> marks(Page page) {
        return new ChartRenderer(StarSizePolicy.DEFAULT)
                .drawnMarks(scene(page), ChartOptions.DEFAULTS);
    }

    /** What is actually on each page. */
    private static void markInventory() {
        System.out.println("## What each page draws");
        System.out.println();
        System.out.println("| page | field | stars | symbols | why |");
        System.out.println("|---|---:|---:|---:|---|");
        for (Page page : PAGES) {
            List<ChartRenderer.DrawnMark> marks = marks(page);
            long stars = marks.stream().filter(m -> m.star() != null).count();
            long symbols = marks.size() - stars;
            System.out.printf(Locale.ROOT,
                    "| %s | %.0f° | %d | %d | %s |%n",
                    page.name(), page.field(), stars, symbols, page.why());
        }
        System.out.println();
    }

    /** How big the marks themselves are: the reach a click gets free. */
    private static void starDotSizes() {
        System.out.println("## The ink a reader aims at");
        System.out.println();
        System.out.println("Star dot radii in page pixels, by magnitude"
                + " (StarSizePolicy.DEFAULT):");
        System.out.println();
        System.out.println("| V | radius px | diameter px |");
        System.out.println("|---:|---:|---:|");
        for (double magnitude : new double[] {0.0, 2.0, 4.0, 6.0, 8.0}) {
            double radius = StarSizePolicy.DEFAULT.radiusFor(magnitude);
            System.out.printf(Locale.ROOT, "| %.1f | %.2f | %.2f |%n",
                    magnitude, radius, 2.0 * radius);
        }
        System.out.println();
        System.out.println("A V 8 star is the smallest thing on the page,"
                + " and it is what a reader most often wants to name.");
        System.out.println();
    }

    /**
     * Clicks that are NOT aimed: a grid over the whole page. This
     * measures how much of the paper answers at all, and how often
     * an unaimed click is ambiguous.
     */
    private static void blindClicks() {
        System.out.println("## Unaimed clicks: what a grid over the page hits");
        System.out.println();
        System.out.print("| page |");
        for (double tolerance : TOLERANCES) {
            System.out.printf(Locale.ROOT, " hit@%.0f | amb@%.0f |",
                    tolerance, tolerance);
        }
        System.out.println();
        System.out.print("|---|");
        for (double ignored : TOLERANCES) {
            System.out.print("---:|---:|");
        }
        System.out.println();
        for (Page page : PAGES) {
            List<ChartRenderer.DrawnMark> marks = marks(page);
            System.out.printf("| %s |", page.name());
            for (double tolerance : TOLERANCES) {
                int sampled = 0;
                int hit = 0;
                int ambiguous = 0;
                for (int y = 10; y < PAGE_HEIGHT - 10; y += 7) {
                    for (int x = 10; x < PAGE_WIDTH - 10; x += 7) {
                        sampled++;
                        int candidates = candidatesAt(marks, x, y, tolerance)
                                .size();
                        if (candidates >= 1) {
                            hit++;
                        }
                        if (candidates >= 2) {
                            ambiguous++;
                        }
                    }
                }
                System.out.printf(Locale.ROOT, " %.1f%% | %.1f%% |",
                        100.0 * hit / sampled,
                        100.0 * ambiguous / sampled);
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Clicks that ARE aimed: at each drawn mark's centre, and jittered
     * by a few pixels as a hand does. This is the number that decides
     * the tolerance: can a reader name the thing they meant?
     */
    private static void aimedClicks() {
        System.out.println("## Aimed clicks: does the reader get the mark"
                + " they pointed at?");
        System.out.println();
        System.out.println("Every drawn mark on every page is clicked at"
                + " its centre and on rings of 1.5, 3.5 and 5.5 px in"
                + " eight directions (25 clicks each). The ring radii"
                + " deliberately match NO swept tolerance: an earlier"
                + " version jittered by exactly ±3 px, which made"
                + " 'listed@3 = 100%' true by construction rather than"
                + " by measurement. 'first' counts the intended mark"
                + " ranked first; 'listed' counts it present at all.");
        System.out.println();
        System.out.print("| page |");
        for (double tolerance : TOLERANCES) {
            System.out.printf(Locale.ROOT, " first@%.0f | listed@%.0f |",
                    tolerance, tolerance);
        }
        System.out.println();
        System.out.print("|---|");
        for (double ignored : TOLERANCES) {
            System.out.print("---:|---:|");
        }
        System.out.println();
        double[][] offsets = clickRings();
        for (Page page : PAGES) {
            List<ChartRenderer.DrawnMark> marks = marks(page);
            System.out.printf("| %s |", page.name());
            for (double tolerance : TOLERANCES) {
                int clicks = 0;
                int first = 0;
                int listed = 0;
                for (ChartRenderer.DrawnMark intended : marks) {
                    for (double[] offset : offsets) {
                        double x = intended.centre().x() + offset[0];
                        double y = intended.centre().y() + offset[1];
                        if (x < 0 || y < 0 || x >= PAGE_WIDTH
                                || y >= PAGE_HEIGHT) {
                            continue;
                        }
                        clicks++;
                        List<ChartRenderer.DrawnMark> candidates =
                                candidatesAt(marks, x, y, tolerance);
                        if (!candidates.isEmpty()
                                && candidates.get(0) == intended) {
                            first++;
                        }
                        if (candidates.contains(intended)) {
                            listed++;
                        }
                    }
                }
                System.out.printf(Locale.ROOT, " %.1f%% | %.1f%% |",
                        100.0 * first / clicks, 100.0 * listed / clicks);
            }
            System.out.println();
        }
        System.out.println();
    }


    /**
     * A hand's aim: the exact centre, and rings at radii chosen to
     * fall between the swept tolerances so no measurement can be
     * satisfied by construction.
     */
    private static double[][] clickRings() {
        double[] radii = {0.0, 1.5, 3.5, 5.5};
        List<double[]> offsets = new ArrayList<>();
        for (double radius : radii) {
            if (radius == 0.0) {
                offsets.add(new double[] {0.0, 0.0});
                continue;
            }
            for (int step = 0; step < 8; step++) {
                double angle = Math.PI * step / 4.0;
                offsets.add(new double[] {radius * Math.cos(angle),
                        radius * Math.sin(angle)});
            }
        }
        return offsets.toArray(new double[0][]);
    }

    /** How deep an ambiguous answer gets, and how often. */
    private static void ambiguity() {
        System.out.println("## How ambiguous is ambiguous");
        System.out.println();
        System.out.println("Aimed clicks only, at the tolerance under"
                + " consideration. 'worst' is the most candidates any"
                + " single click produced.");
        System.out.println();
        System.out.println("| page | tol | 1 candidate | 2 | 3 | 4+ | worst |");
        System.out.println("|---|---:|---:|---:|---:|---:|---:|");
        for (Page page : PAGES) {
            List<ChartRenderer.DrawnMark> marks = marks(page);
            for (double tolerance : new double[] {3.0, 4.0, 6.0}) {
                int[] buckets = new int[4];
                int worst = 0;
                for (ChartRenderer.DrawnMark intended : marks) {
                    List<ChartRenderer.DrawnMark> candidates = candidatesAt(
                            marks, intended.centre().x(),
                            intended.centre().y(), tolerance);
                    int n = candidates.size();
                    worst = Math.max(worst, n);
                    if (n >= 1) {
                        buckets[Math.min(n, 4) - 1]++;
                    }
                }
                int total = buckets[0] + buckets[1] + buckets[2] + buckets[3];
                if (total == 0) {
                    continue;
                }
                System.out.printf(Locale.ROOT,
                        "| %s | %.0f | %.1f%% | %.1f%% | %.1f%% | %.1f%% | %d |%n",
                        page.name(), tolerance,
                        100.0 * buckets[0] / total, 100.0 * buckets[1] / total,
                        100.0 * buckets[2] / total, 100.0 * buckets[3] / total,
                        worst);
            }
        }
        System.out.println();
    }

    /**
     * The candidates a click produces, in the proposed order: nearest
     * first, then the more prominent mark (brighter star, larger
     * symbol), then a stable catalogue identity so the order can
     * never depend on iteration or locale.
     */
    public static List<ChartRenderer.DrawnMark> candidatesAt(
            List<ChartRenderer.DrawnMark> marks, double x, double y,
            double tolerance) {
        List<ChartRenderer.DrawnMark> hits = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.outline().contains(x, y)
                    || mark.distanceFrom(x, y) <= mark.reach() + tolerance) {
                hits.add(mark);
            }
        }
        hits.sort(Comparator
                .comparingDouble((ChartRenderer.DrawnMark mark) ->
                        Math.round(mark.distanceFrom(x, y) * 10.0) / 10.0)
                .thenComparingDouble(IdentifyStudyMain::prominence)
                .thenComparing(IdentifyStudyMain::identityOf));
        return hits;
    }

    /** Lower sorts first: a brighter star, a larger symbol. */
    private static double prominence(ChartRenderer.DrawnMark mark) {
        Star star = mark.star();
        return star != null ? star.magnitude() : -mark.reach();
    }

    /** A stable key, so ties never depend on iteration order. */
    private static String identityOf(ChartRenderer.DrawnMark mark) {
        Star star = mark.star();
        return star != null ? star.id() : mark.deepSky().id();
    }
}
