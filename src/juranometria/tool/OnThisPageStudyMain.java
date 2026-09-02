package juranometria.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.RegionalDetailPolicy;

/**
 * What is actually on a page (Sprint 24, issue #214).
 *
 * <p>The gate's evidence. The chart has always known more than it
 * draws - a page carries objects whose family is switched off, whose
 * magnitude is past the limit, whose symbol the atlas does not have,
 * and until now the only way to ask was to point at ink that was not
 * there. Before deciding what a table should say, this measures what
 * a table would have to hold.
 *
 * <p>Everything comes from production: the same projection and
 * viewport mapping the renderer places marks with, the same
 * {@code permitted} rule the family switches obey, the same detail
 * policy, and the scene the application's own assembler builds.
 */
public final class OnThisPageStudyMain {

    private OnThisPageStudyMain() {
    }

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    /** The pages the gate asks for. */
    private record Page(String name, double ra, double dec, String why) {
    }

    private static final List<Page> PAGES = List.of(
            new Page("m31", 10.684708, 41.268750, "the released default"),
            new Page("orion", 83.8, 0.0, "bright, familiar, equatorial"),
            new Page("virgo", 187.706, 12.391, "the densest galaxies"),
            new Page("lmc", 80.894, -69.756, "the Large Magellanic Cloud"),
            new Page("ra-zero", 0.0, 20.0, "the seam"),
            new Page("polar", 0.0, 85.0, "near the pole"));

    private static final double[] FIELDS = {1.0, 8.0, 18.0, 36.0};

    /**
     * Why an object is or is not visible, decided by production
     * rather than guessed. The order is the order a reader meets
     * them: what they can see, then the three reasons they cannot.
     */
    private enum Visibility {
        DRAWN("drawn"),
        FAMILY_HIDDEN("hidden by a chart option"),
        BELOW_LIMIT("fainter than the magnitude limit"),
        NO_SYMBOL("no chart symbol for its type"),
        TOO_SMALL("below the detail policy at this field");

        private final String prose;

        Visibility(String prose) {
            this.prose = prose;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("# What is on this page");
        System.out.println();
        System.out.println("Measured by `make on-this-page-study`."
                + " Every position, projection and visibility answer"
                + " comes from production: the projection and viewport"
                + " mapping the renderer places marks with, the"
                + " `permitted` rule the family switches obey, the"
                + " detail policy, and the scene the application's own"
                + " assembler builds.");
        System.out.println();

        definition();
        sizes();
        visibilityBreakdown();
        theStarQuestion();
        ordering();
        cost();
    }

    // ------------------------------------------------------------------

    private static void definition() {
        System.out.println("## What \"on this page\" means");
        System.out.println();
        System.out.println("**An object is on the page when its"
                + " recorded position projects onto the paper.**"
                + " Nothing else: not whether its symbol is drawn, its"
                + " family switched on, its magnitude inside the"
                + " limit, or the atlas has a symbol for its type at"
                + " all.");
        System.out.println();
        System.out.println("The paper is the viewport the renderer"
                + " clips to - `1, 1, width-2, height-2` - which is"
                + " the same rectangle `drawnMarks` tests against, so"
                + " a table and the drawing cannot come to disagree"
                + " about where the page ends. Letterbox chrome is"
                + " not paper: it is not in the viewport at all.");
        System.out.println();
        System.out.println("An object behind the projection's horizon"
                + " has no place on the page and is not on it. A"
                + " gnomonic page shows less than a hemisphere, and"
                + " the projection says so by refusing the point"
                + " rather than by returning a distant one.");
        System.out.println();
    }

    /** Production's own answer to where a position lands. */
    private static PixelPoint onPage(GnomonicProjection projection,
                                     ViewportMapping mapping,
                                     SkyPosition position) {
        return projection.project(position)
                .map(mapping::toPixel)
                .filter(OnThisPageStudyMain::insidePaper)
                .orElse(null);
    }

    private static boolean insidePaper(PixelPoint pixel) {
        return pixel.x() >= 1 && pixel.y() >= 1
                && pixel.x() <= WIDTH - 1 && pixel.y() <= HEIGHT - 1;
    }

    private record Inventory(List<DeepSkyObject> deepSky, List<Star> stars) {
    }

    private static Inventory inventoryOf(ChartScene scene) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        List<DeepSkyObject> deepSky = new ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (onPage(projection, mapping, dso.position()) != null) {
                deepSky.add(dso);
            }
        }
        List<Star> stars = new ArrayList<>();
        for (Star star : scene.stars()) {
            if (onPage(projection, mapping, star.position()) != null) {
                stars.add(star);
            }
        }
        return new Inventory(deepSky, stars);
    }

    private static ChartScene pageOf(Page page, double field) {
        return Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(page.ra(), page.dec()), field, 8.0),
                WIDTH, HEIGHT);
    }

    // ------------------------------------------------------------------

    private static void sizes() {
        System.out.println("## How much is on a page");
        System.out.println();
        System.out.println("| page | why | field | deep-sky | stars |"
                + " total |");
        System.out.println("|---|---|---:|---:|---:|---:|");
        int worstStars = 0;
        int worstDeepSky = 0;
        for (Page page : PAGES) {
            for (double field : FIELDS) {
                Inventory inventory = inventoryOf(pageOf(page, field));
                worstStars = Math.max(worstStars, inventory.stars().size());
                worstDeepSky = Math.max(worstDeepSky,
                        inventory.deepSky().size());
                System.out.printf(Locale.ROOT,
                        "| %s | %s | %.0f° | %d | %d | %d |%n",
                        page.name(), page.why(), field,
                        inventory.deepSky().size(),
                        inventory.stars().size(),
                        inventory.deepSky().size()
                                + inventory.stars().size());
            }
        }
        System.out.println();
        System.out.printf(Locale.ROOT,
                "The worst page here carries **%d stars** and **%d"
                        + " deep-sky objects**. That is the number a"
                        + " table has to survive, and it decides"
                        + " whether one undifferentiated list is"
                        + " honest.%n%n",
                worstStars, worstDeepSky);
    }

    // ------------------------------------------------------------------

    private static void visibilityBreakdown() {
        ChartRenderer renderer =
                new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT);
        ChartOptions defaults = ChartOptions.DEFAULTS;
        ChartOptions galaxiesOff = defaults.withFamily(
                juranometria.render.SymbolFamily.GALAXIES, false);

        System.out.println("## Present, and why it cannot be seen");
        System.out.println();
        System.out.println("Every state is production's own answer."
                + " `permitted` is the rule the family switches obey,"
                + " the detail policy is the one the renderer asks,"
                + " and the magnitude limit is the scene's. Nothing"
                + " here infers a state from a missing pixel.");
        System.out.println();
        System.out.println("| page | field | drawn | hidden by an"
                + " option | fainter than the limit | no symbol |"
                + " too small at this field |");
        System.out.println("|---|---:|---:|---:|---:|---:|---:|");
        for (Page page : PAGES) {
            for (double field : new double[] {8.0, 36.0}) {
                ChartScene scene = pageOf(page, field);
                Map<Visibility, Integer> tally =
                        classify(scene, defaults, renderer);
                System.out.printf(Locale.ROOT,
                        "| %s | %.0f° | %d | %d | %d | %d | %d |%n",
                        page.name(), field,
                        tally.getOrDefault(Visibility.DRAWN, 0),
                        tally.getOrDefault(Visibility.FAMILY_HIDDEN, 0),
                        tally.getOrDefault(Visibility.BELOW_LIMIT, 0),
                        tally.getOrDefault(Visibility.NO_SYMBOL, 0),
                        tally.getOrDefault(Visibility.TOO_SMALL, 0));
            }
        }
        System.out.println();

        ChartScene m31 = pageOf(PAGES.get(0), 8.0);
        Map<Visibility, Integer> withGalaxies =
                classify(m31, defaults, renderer);
        Map<Visibility, Integer> without =
                classify(m31, galaxiesOff, renderer);
        System.out.printf(Locale.ROOT,
                "Switching **Galaxies** off on the released page moves"
                        + " **%d** objects from *drawn* to *hidden by a"
                        + " chart option* - %d to %d - without changing"
                        + " what the page contains. That is the"
                        + " distinction the table exists to make:"
                        + " presence is a fact about the sky, and"
                        + " visibility is a fact about the reader's"
                        + " choices.%n%n",
                withGalaxies.getOrDefault(Visibility.DRAWN, 0)
                        - without.getOrDefault(Visibility.DRAWN, 0),
                withGalaxies.getOrDefault(Visibility.DRAWN, 0),
                without.getOrDefault(Visibility.DRAWN, 0));
    }

    private static Map<Visibility, Integer> classify(
            ChartScene scene, ChartOptions options,
            ChartRenderer renderer) {
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        RegionalDetailPolicy policy =
                new RegionalDetailPolicy(scene, mapping.pixelsPerPlaneUnit());
        Inventory inventory = inventoryOf(scene);
        Map<Visibility, Integer> tally = new LinkedHashMap<>();
        for (DeepSkyObject dso : inventory.deepSky()) {
            tally.merge(stateOf(scene, dso, options, policy), 1, Integer::sum);
        }
        for (Star star : inventory.stars()) {
            tally.merge(star.magnitude() > scene.limitingMagnitude()
                            ? Visibility.BELOW_LIMIT : Visibility.DRAWN,
                    1, Integer::sum);
        }
        return tally;
    }

    private static Visibility stateOf(ChartScene scene, DeepSkyObject dso,
                                      ChartOptions options,
                                      RegionalDetailPolicy policy) {
        if (ChartRenderer.symbolForType(dso.type())
                == ChartRenderer.Symbol.NONE) {
            return Visibility.NO_SYMBOL;
        }
        if (!ChartRenderer.permitted(scene, dso, options)) {
            return Visibility.FAMILY_HIDDEN;
        }
        if (!policy.drawn(dso)) {
            return Visibility.TOO_SMALL;
        }
        return Visibility.DRAWN;
    }

    // ------------------------------------------------------------------

    /**
     * The star question. A page can carry more than a thousand
     * stars, and almost all of them are a catalogue number and a
     * brightness - a row saying "TYC 2801-2090-1, 4.51" answers
     * nothing a reader asked. This measures how many carry a name,
     * a Bayer letter or a Flamsteed number: something a reader could
     * have come looking for.
     */
    private static void theStarQuestion() {
        System.out.println("## The stars, and whether they belong in it");
        System.out.println();
        System.out.println("A page carries far more stars than"
                + " deep-sky objects, and nearly all of them are a"
                + " catalogue number and a brightness. A row reading"
                + " *TYC 2801-2090-1, 4.5* answers no question a"
                + " reader had. So the question is not whether stars"
                + " are on the page - they are, and the table must"
                + " not pretend otherwise - but which of them a"
                + " reader could have come looking for.");
        System.out.println();
        System.out.println("| page | field | stars | with a name,"
                + " Bayer letter or Flamsteed number | anonymous |");
        System.out.println("|---|---:|---:|---:|---:|");
        for (Page page : PAGES) {
            for (double field : new double[] {8.0, 36.0}) {
                ChartScene scene = pageOf(page, field);
                List<Star> stars = inventoryOf(scene).stars();
                int named = 0;
                for (Star star : stars) {
                    if (isNamed(star)) {
                        named++;
                    }
                }
                System.out.printf(Locale.ROOT,
                        "| %s | %.0f° | %d | %d | %d |%n",
                        page.name(), field, stars.size(), named,
                        stars.size() - named);
            }
        }
        System.out.println();

        // And what the magnitude limit does, which the bundled pack
        // hides at the default: it holds bright sky only, so nothing
        // is past V 8 until a reader asks for less.
        ChartScene bright = Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(83.8, 0.0), 18.0, 4.0), WIDTH, HEIGHT);
        List<Star> all = inventoryOf(bright).stars();
        int past = 0;
        for (Star star : all) {
            if (star.magnitude() > bright.limitingMagnitude()) {
                past++;
            }
        }
        System.out.printf(Locale.ROOT,
                "**The magnitude limit only bites when a reader asks"
                        + " it to.** The bundled pack is bright sky,"
                        + " so at the released limit of V 8 nothing on"
                        + " any page above is past it. Set Orion's"
                        + " 18° page to V 4 and **%d of its %d stars**"
                        + " become present-but-unplotted - which is"
                        + " precisely the state a reader cannot"
                        + " discover by looking at the paper.%n%n",
                past, all.size());
    }

    /** Whether a star carries something a reader could search for. */
    private static boolean isNamed(Star star) {
        juranometria.chart.StarIdentity identity = star.identity();
        return identity != null && (identity.name() != null
                || identity.bayer() != null
                || identity.flamsteed() != null);
    }

    private static void ordering() {
        System.out.println("## The order a reader meets them");
        System.out.println();
        System.out.println("Proposed default: **a Messier number"
                + " first, then recorded brightness, then angular"
                + " distance from the centre, then catalogue"
                + " identity.** Identity last makes it total, so the"
                + " same page always lists in the same order however"
                + " the catalogue arrives.");
        System.out.println();
        ChartScene scene = pageOf(PAGES.get(0), 8.0);
        List<DeepSkyObject> deepSky =
                new ArrayList<>(inventoryOf(scene).deepSky());
        deepSky.sort(defaultOrder(scene.viewport().centre()));
        System.out.println("The released page, in that order:");
        System.out.println();
        System.out.println("| # | object | Messier | magnitude |"
                + " from centre |");
        System.out.println("|---:|---|---|---:|---:|");
        int row = 0;
        for (DeepSkyObject dso : deepSky) {
            if (++row > 8) {
                break;
            }
            System.out.printf(Locale.ROOT, "| %d | %s | %s | %s | %.2f° |%n",
                    row, dso.id(),
                    messierOf(dso) == null ? "—" : "M " + messierOf(dso),
                    Double.isNaN(dso.magnitude()) ? "not recorded"
                            : String.format(Locale.ROOT, "%.1f",
                                    dso.magnitude()),
                    scene.viewport().centre()
                            .separationDegrees(dso.position()));
        }
        System.out.printf(Locale.ROOT, "%n**%d** deep-sky rows in"
                + " all on that page.%n%n", deepSky.size());
    }

    /** A Messier number from the object's aliases, or null. */
    private static Integer messierOf(DeepSkyObject dso) {
        for (String alias : dso.aliases()) {
            String trimmed = alias.trim();
            if (trimmed.startsWith("M ") || trimmed.startsWith("M")) {
                String digits = trimmed.replaceAll("[^0-9]", "");
                if (!digits.isEmpty() && trimmed.replaceAll("[0-9 ]", "")
                        .equalsIgnoreCase("M")) {
                    return Integer.valueOf(digits);
                }
            }
        }
        return null;
    }

    private static Comparator<DeepSkyObject> defaultOrder(SkyPosition centre) {
        return Comparator
                .comparingInt((DeepSkyObject dso) ->
                        messierOf(dso) == null ? 1 : 0)
                .thenComparingInt(dso -> messierOf(dso) == null
                        ? Integer.MAX_VALUE : messierOf(dso))
                .thenComparingDouble(dso -> Double.isNaN(dso.magnitude())
                        ? Double.MAX_VALUE : dso.magnitude())
                .thenComparingDouble(dso ->
                        centre.separationDegrees(dso.position()))
                .thenComparing(DeepSkyObject::id);
    }

    // ------------------------------------------------------------------

    private static void cost() {
        System.out.println("## What it costs to know");
        System.out.println();
        System.out.println("The scene the chart already assembled"
                + " holds **every** star and deep-sky object in the"
                + " queried region, unfiltered - the renderer applies"
                + " the magnitude limit as it draws. So an inventory"
                + " needs no catalogue query of its own: it is a"
                + " projection sweep over a list the page is already"
                + " holding.");
        System.out.println();
        long objects = 0;
        for (Page page : PAGES) {
            ChartScene scene = pageOf(page, 8.0);
            objects += scene.stars().size() + scene.deepSkyObjects().size();
        }
        System.out.printf(Locale.ROOT,
                "Across the six pages at 8°, that is **%d objects**"
                        + " to project - arithmetic, no catalogue, no"
                        + " allocation beyond the answer.%n%n", objects);
        System.out.println("Timings are deliberately absent from this"
                + " report. They differ between machines and between"
                + " two runs on one machine, and a study whose output"
                + " moves cannot be reproduced - the same rule the"
                + " Milky Way gate settled. #215 sets the interaction"
                + " budget and enforces it on the supported CI path.");
        System.out.println();
    }
}
