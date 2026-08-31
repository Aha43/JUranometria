package juranometria.tool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
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
        catalogueHonesty();
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


    /**
     * What the pack records, and what the runtime model currently
     * keeps of it (gate review, P1). The inspector may only promise
     * facts the application can still tell apart from silence.
     */
    private static void catalogueHonesty() {
        int rows = 0;
        int noMajor = 0;
        int noMinor = 0;
        int noPa = 0;
        int noV = 0;
        int noVButB = 0;
        int noPhotometry = 0;
        for (int ra = 0; ra < 12; ra++) {
            for (int dec = 0; dec < 6; dec++) {
                String tile = String.format(Locale.ROOT,
                        "/resources/catalog/bright-sky/tiles/r%02d-d%d/dsos.csv",
                        ra, dec);
                java.io.InputStream in =
                        IdentifyStudyMain.class.getResourceAsStream(tile);
                if (in == null) {
                    continue;
                }
                try (java.io.BufferedReader reader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(in,
                                        java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("#") || line.isBlank()) {
                            continue;
                        }
                        String[] f = line.split(",", -1);
                        if (f.length < 11) {
                            continue;
                        }
                        rows++;
                        if (f[5].isEmpty()) {
                            noMajor++;
                        }
                        if (f[6].isEmpty()) {
                            noMinor++;
                        }
                        if (f[7].isEmpty()) {
                            noPa++;
                        }
                        if (f[8].isEmpty()) {
                            noV++;
                            if (!f[9].isEmpty()) {
                                noVButB++;
                            } else {
                                noPhotometry++;
                            }
                        }
                    }
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(tile, e);
                }
            }
        }
        System.out.println("## What the pack knows, and what survives loading");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "Of **%,d** deep-sky rows in the bundled pack:%n%n", rows);
        System.out.println("| fact | rows where the source records nothing | share |");
        System.out.println("|---|---:|---:|");
        System.out.printf(Locale.ROOT,
                "| major axis | %,d | %.1f%% |%n", noMajor, pct(noMajor, rows));
        System.out.printf(Locale.ROOT,
                "| minor axis | %,d | %.1f%% |%n", noMinor, pct(noMinor, rows));
        System.out.printf(Locale.ROOT,
                "| position angle | %,d | %.1f%% |%n", noPa, pct(noPa, rows));
        System.out.printf(Locale.ROOT,
                "| V magnitude | %,d | %.1f%% |%n", noV, pct(noV, rows));
        System.out.printf(Locale.ROOT,
                "| ...of which a B magnitude exists | %,d | %.1f%% |%n",
                noVButB, pct(noVButB, rows));
        System.out.printf(Locale.ROOT,
                "| ...no photometry at all | %,d | %.1f%% |%n",
                noPhotometry, pct(noPhotometry, rows));
        System.out.println();
        System.out.println("The runtime `DeepSkyObject` keeps none of these"
                + " distinctions: the loader substitutes a nominal extent"
                + " for an absent size, `minor = major` for an absent"
                + " minor axis, `0.0` for an absent position angle, and"
                + " stores V-or-B in one unlabelled `magnitude` field."
                + " An inspector built on today's model would state a"
                + " size no one measured, a position angle of exactly"
                + " zero, and a B magnitude labelled V.");
        System.out.println();
    }

    private static double pct(int part, int whole) {
        return whole == 0 ? 0.0 : 100.0 * part / whole;
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
            // The renderer's own hit rule: the mark's footprint
            // expanded by the tolerance, never a circle around its
            // centre (gate review).
            if (mark.hitBy(x, y, tolerance)) {
                hits.add(mark);
            }
        }
        // The reviewed order (gate review, P2). An earlier version
        // ranked by "prominence", comparing star magnitudes against
        // negative symbol radii - two different quantities in one
        // comparator, which is not an order anyone could reason
        // about. These three keys are all kind-independent:
        //   1. ink beats nearness: a click INSIDE a mark outranks one
        //      merely within tolerance of a closer centre, so
        //      clicking a galaxy's disc never yields the star beside
        //      it;
        //   2. then distance, rounded to 0.1 px so sub-pixel noise
        //      cannot reorder equals;
        //   3. then the SMALLER reach, because the tighter mark is
        //      the more specific answer - a dot on top of a wide
        //      nebula means the dot;
        //   4. then catalogue identity, unique and stable.
        hits.sort(Comparator
                .comparing((ChartRenderer.DrawnMark mark) ->
                        mark.outline().contains(x, y) ? 0 : 1)
                .thenComparingDouble(mark ->
                        Math.round(mark.distanceFrom(x, y) * 10.0) / 10.0)
                .thenComparingDouble(ChartRenderer.DrawnMark::reach)
                .thenComparing(IdentifyStudyMain::identityOf));
        return hits;
    }

    /** A stable key, so ties never depend on iteration order. */
    private static String identityOf(ChartRenderer.DrawnMark mark) {
        return mark.star() != null ? mark.star().id() : mark.deepSky().id();
    }
}
