package juranometria.tool;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelGeometry;
import juranometria.render.LabelPlacement;
import juranometria.render.StarLabelPolicy;
import juranometria.search.SearchResult;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

/**
 * An optional "all star identities where they fit" density, studied before
 * any production change (Sprint 51, issue #557, ahead of #558).
 *
 * <p>Every page is a production scene from the bundled catalogue, labelled by
 * the production {@code LabelPlacement} against the production obstacles. The
 * released policy's requests are the renderer's own ({@code textRequests});
 * the study adds, for every star on the page that carries an identity in the
 * star-identity pack and is not already asked for, a request with the same
 * composition the released policy uses (name with Bayer letter, else the
 * letter, else the Flamsteed number) and none of its field or magnitude
 * thresholds, at the production candidate boxes. Two orderings are measured:
 *
 * <ul>
 *   <li><b>A - as stars.</b> Every identity competes as a star label, brightest
 *       first, ahead of deep-sky labels and constellation names - what the
 *       released ordering does if its thresholds are simply removed. Labels
 *       refused everywhere are placed at their least bad place, as today.</li>
 *   <li><b>B - after the page.</b> The released page is placed exactly as it is;
 *       the extra identities, brightest first, then take only room that is
 *       free: an extra identity no candidate of which is clean is refused, never
 *       placed under duress.</li>
 * </ul>
 *
 * <p>The report records what is the same on every desktop: the pages and how
 * many identities each offers. How many are placed depends on this desktop's
 * fonts and stands in {@code platform.md}; the time each placement takes is
 * said on standard error and committed nowhere, because it does not reproduce.
 * The study reads two private production helpers by reflection - the candidate
 * boxes and the clean-candidate test - so it measures production's geometry
 * rather than a copy of it; it changes nothing in production.
 */
public final class StarIdentityDensityStudyMain {

    private static final File DIR = new File("docs/studies/star-identity-density");
    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final double[] FIELDS = {2.0, 4.0, 8.0, 12.0, 24.0, 42.0, 60.0};
    /** The reader's default limit, which zooming keeps. */
    private static final double LIMIT = ChartViewState.DEFAULT.limitingMagnitude();

    private static final StringBuilder REPORT = new StringBuilder();
    private static final StringBuilder PLATFORM = new StringBuilder();

    private record Region(String name, String star, String caption) {
    }

    record Measured(int released, int offered, int placedA, int duressA,
                            int deepSkyA, int namesA, int placedB, int refusedB,
                            int deepSky, int names, int richer, double msA, double msB,
                            int releasedPlaced, int placedC, int refusedC, int keptC,
                            int deepSkyC, int namesC,
                            List<LabelPlacement.Placement> releasedPlacements,
                            List<LabelPlacement.Placement> a,
                            List<LabelPlacement.Placement> b,
                            List<LabelPlacement.Placement> c) {
    }

    private StarIdentityDensityStudyMain() {
    }

    private static void p(String line) {
        REPORT.append(line).append('\n');
    }

    private static void q(String line) {
        PLATFORM.append(line).append('\n');
    }

    public static void main(String[] args) throws Exception {
        REPORT.setLength(0);
        PLATFORM.setLength(0);
        DIR.mkdirs();
        PlatformEvidence.preface(PLATFORM,
                "All star identities where they fit: what this desktop's fonts place",
                "Sprint 51, issue #557.");
        List<Region> regions = List.of(
                new Region("orion", "Alnilam", "Orion, centred on Alnilam - the"
                        + " owner's crowded named field."),
                new Region("aries", "Mesarthim", "Aries, centred on Mesarthim - the"
                        + " issue's case: galaxies in Aries, keeping Mesarthim."),
                new Region("cygnus", "Sadr", "Cygnus, centred on Sadr - the Milky"
                        + " Way, crowded with faint identities."),
                new Region("eridanus", "Acamar", "Eridanus, centred on Acamar - a"
                        + " sparse southern field."));

        p("# All star identities where they fit: measurements");
        p("");
        p("Sprint 51, issue #557, a study ahead of #558. Every page is a production scene"
                + " (" + WIDTH + " × " + HEIGHT + " px, limit V " + fmt(LIMIT) + ", the"
                + " released default options) labelled by the production `LabelPlacement`"
                + " against the production obstacles. **A - as stars**: every identity competes"
                + " as a star label, brightest first, ahead of deep-sky labels and"
                + " constellation names, least-bad placement kept. **B - after the page**: the"
                + " released page placed exactly as it is, then every extra identity, brightest"
                + " first, in clean room only - never under duress. **C - the page, fuller**:"
                + " the released requests in their released order, each star's text upgraded"
                + " to its full identity, then the extra identities as in B. Regions are found by"
                + " searching the bundled catalogue for the named star. Counts of what is placed"
                + " depend on this desktop's fonts and are in `platform.md`. Regenerate with `make star-identity-density-study`.");
        p("");
        p("## Identities on each page");
        p("");
        p("A star is *offered* by the density mode when it is on the page, within the"
                + " page's magnitude limit, carries a name, a Bayer designation or a Flamsteed"
                + " number in the star-identity pack, and is not already asked for by the"
                + " released policy. *Released* counts the released policy's star requests.");
        p("");
        p("| page | field | stars with an identity | released requests | offered in addition |");
        p("|---|---:|---:|---:|---:|");
        q("## What each ordering places");
        q("");
        q("C: the released requests in their released order, each star's text upgraded to"
                + " its full identity (C kept: released star labels placed; compare *released"
                + " placed*), then extra identities in clean room only (C placed, C refused).");
        q("");
        q("A: placed (of which under duress), deep-sky labels and constellation names kept."
                + " B: extra identities placed clean, refused; the released page's own labels"
                + " are untouched by construction. *Richer* counts released labels whose star"
                + " carries more identity than the released text shows (B leaves them as"
                + " released).");
        q("");
        q("| page | field | released | offered | A placed | A duress | A deep-sky | A names"
                + " | B placed | B refused | deep-sky | names | richer | released placed"
                + " | C kept | C placed | C refused | C deep-sky | C names |");
        q("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|");

        ChartRenderer renderer = new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT,
                PageText.in(EN));
        List<String> images = new ArrayList<>();
        List<String> mesarthim = new ArrayList<>();
        for (Region region : regions) {
            SkyPosition centre = find(region.star());
            for (double field : FIELDS) {
                ChartScene scene = Atlas.assembler().assemble(
                        new ChartViewState(centre, field, LIMIT), WIDTH, HEIGHT);
                Measured m = measure(renderer, scene);
                String page = region.name() + "-" + fieldToken(field);
                if (region.name().equals("aries")) {
                    for (Star star : scene.stars()) {
                        if (star.identity() != null
                                && "Mesarthim".equals(star.identity().name())) {
                            String shown = new StarLabelPolicy(field).qualifying(star)
                                    .text(true, true, true);
                            mesarthim.add("| " + fmt(field) + "° | V " + String.format(
                                    Locale.ROOT, "%.2f", star.magnitude()) + " | "
                                    + (shown == null ? "(none)" : shown) + " | "
                                    + everything(star) + " |");
                        }
                    }
                }
                p("| " + page + " | " + fmt(field) + "° | " + identities(scene) + " | "
                        + m.released() + " | " + m.offered() + " |");
                q("| " + page + " | " + fmt(field) + "° | " + m.released() + " | "
                        + m.offered() + " | " + m.placedA() + " | " + m.duressA() + " | "
                        + m.deepSkyA() + " | " + m.namesA() + " | " + m.placedB() + " | "
                        + m.refusedB() + " | " + m.deepSky() + " | " + m.names() + " | "
                        + m.richer() + " | " + m.releasedPlaced() + " | " + m.keptC() + " | "
                        + m.placedC() + " | " + m.refusedC() + " | " + m.deepSkyC() + " | "
                        + m.namesC() + " |");
                // Time is not evidence that reproduces: said, never committed.
                System.err.println(String.format(Locale.ROOT, "%s: A %.1f ms, B %.1f ms",
                        page, m.msA(), m.msB()));
                if (field == 8.0 || ((field == 24.0 || field == 60.0) && region.name().equals("orion"))) {
                    for (String variant : List.of("released", "a", "c")) {
                        String file = page + "-" + variant + ".png";
                        draw(renderer, scene, variant.equals("released")
                                ? m.releasedPlacements()
                                : variant.equals("a") ? m.a() : m.c(), file);
                        images.add(file + "|" + region.caption() + "|" + fmt(field)
                                + "|" + variant);
                    }
                }
            }
        }
        p("");
        q("");
        p("## The issue's case: Mesarthim");
        p("");
        p("What the released policy writes beside Mesarthim at each field, and its full"
                + " identity. The released policy already labels it, by its letter: it is a"
                + " *richer* case, not an extra one - ordering B, which leaves every released"
                + " label as it is, would never show the name; ordering C does.");
        p("");
        p("| field | magnitude | released text | full identity |");
        p("|---:|---|---|---|");
        for (String line : mesarthim) {
            p(line);
        }
        p("");
        p("## The pages");
        p("");
        p("Each comparison is the same production page three times: the released labels,"
                + " ordering A and ordering B. Placement depends on this desktop's fonts, so the"
                + " images are this machine's answer; what they show - whether the released"
                + " page survives, and how much the crowded fields carry - is the decision's"
                + " evidence.");
        p("");
        for (String image : images) {
            String[] f = image.split("\\|");
            String what = switch (f[3]) {
                case "released" -> "the released labels";
                case "a" -> "A - every identity as a star label, least-bad placement kept";
                default -> "C - the released labels in their released order with each"
                        + " star's full identity, then extra identities in clean room";
            };
            p("### " + f[0].replace(".png", ""));
            p("");
            p(f[1] + " " + f[2] + "°: " + what + ".");
            p("");
            p("![](" + f[0] + ")");
            p("");
        }
        System.out.print(PlatformEvidence.portable(REPORT.toString()));
        System.out.flush();
        PlatformEvidence.write(PLATFORM, new File(DIR, "platform.md").toString());
    }

    static Measured measure(ChartRenderer renderer, ChartScene scene)
            throws Exception {
        ChartOptions options = ChartOptions.DEFAULTS;
        ChartRenderer.TextMetrics metrics = ChartRenderer.TextMetrics.offscreen();
        FontMetrics labels = metrics.labels();
        DrawnPage page = DrawnPage.of(scene);
        List<LabelPlacement.Request> released = renderer.textRequests(metrics, scene, options);
        List<LabelPlacement.Obstacle> obstacles = renderer.textObstacles(metrics, scene,
                options);
        Set<String> asked = new HashSet<>();
        int releasedStars = 0;
        for (LabelPlacement.Request request : released) {
            if (request.family() == LabelPlacement.Family.STAR
                    || request.family() == LabelPlacement.Family.TARGET) {
                asked.add(request.id());
                releasedStars++;
            }
        }
        ViewportMapping mapping = new ViewportMapping(page);
        List<LabelPlacement.Request> extras = new ArrayList<>();
        List<LabelPlacement.Request> everyIdentity = new ArrayList<>();
        int richer = 0;
        java.util.Map<String, LabelPlacement.Request> upgraded = new java.util.HashMap<>();
        List<Star> stars = new ArrayList<>(scene.stars());
        stars.sort(Comparator.comparingDouble(Star::magnitude).thenComparing(Star::id));
        Method around = LabelGeometry.class.getDeclaredMethod("around", FontMetrics.class,
                String.class, PixelPoint.class, double.class);
        around.setAccessible(true);
        for (Star star : stars) {
            String text = everything(star);
            if (text == null || star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            var plane = page.projection().project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint at = mapping.toPixel(plane.get());
            if (at.x() < 0 || at.x() >= WIDTH || at.y() < 0
                    || at.y() >= scene.viewport().heightPx()) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Rectangle2D> boxes = (List<Rectangle2D>) around.invoke(null, labels, text,
                    at, renderer.starSize().radiusFor(star.magnitude()));
            LabelPlacement.Request request = new LabelPlacement.Request(
                    LabelPlacement.Family.STAR, star.id(), text, at.x(), at.y(), boxes,
                    star.id(), null, false, star.magnitude());
            everyIdentity.add(request);
            if (asked.contains(star.id())) {
                for (LabelPlacement.Request mine : released) {
                    if (mine.id().equals(star.id()) && !mine.text().equals(text)
                            && mine.family() == LabelPlacement.Family.STAR) {
                        richer++;
                        upgraded.put(star.id(), request);
                    }
                }
            } else {
                extras.add(request);
            }
        }

        // A: the released requests with every star's composition unthresholded.
        List<LabelPlacement.Request> asStars = new ArrayList<>();
        for (LabelPlacement.Request request : released) {
            if (request.family() != LabelPlacement.Family.STAR) {
                asStars.add(request);
            }
        }
        asStars.addAll(everyIdentity);
        long start = System.nanoTime();
        List<LabelPlacement.Placement> a = new LabelPlacement(WIDTH,
                scene.viewport().heightPx(), obstacles).placeAll(asStars);
        double msA = (System.nanoTime() - start) / 1e6;

        // B: the released page, then extra identities in clean room only.
        start = System.nanoTime();
        LabelPlacement placement = new LabelPlacement(WIDTH, scene.viewport().heightPx(),
                obstacles);
        List<LabelPlacement.Placement> releasedPlacements = placement.placeAll(released);
        Method refuse = LabelPlacement.class.getDeclaredMethod("refuse",
                LabelPlacement.Request.class, int.class);
        refuse.setAccessible(true);
        List<LabelPlacement.Placement> b = new ArrayList<>(releasedPlacements);
        int placedB = 0;
        int refusedB = 0;
        for (LabelPlacement.Request extra : extras) {
            boolean clean = false;
            for (int at = 0; at < extra.candidates().size() && !clean; at++) {
                clean = refuse.invoke(placement, extra, at) == null;
            }
            if (clean) {
                b.add(placement.place(extra));
                placedB++;
            } else {
                refusedB++;
            }
        }
        double msB = (System.nanoTime() - start) / 1e6;

        // C: the released requests in their released order, each star's text
        // upgraded to its full identity; then the extra identities, clean only.
        List<LabelPlacement.Request> fuller = new ArrayList<>();
        for (LabelPlacement.Request request : released) {
            fuller.add(upgraded.getOrDefault(request.id(), request));
        }
        LabelPlacement placementC = new LabelPlacement(WIDTH, scene.viewport().heightPx(),
                obstacles);
        List<LabelPlacement.Placement> c = new ArrayList<>(placementC.placeAll(fuller));
        int placedC = 0;
        int refusedC = 0;
        for (LabelPlacement.Request extra : extras) {
            boolean clean = false;
            for (int at = 0; at < extra.candidates().size() && !clean; at++) {
                clean = refuse.invoke(placementC, extra, at) == null;
            }
            if (clean) {
                c.add(placementC.place(extra));
                placedC++;
            } else {
                refusedC++;
            }
        }

        int placedA = 0;
        int duressA = 0;
        int deepSkyA = 0;
        int namesA = 0;
        for (LabelPlacement.Placement one : a) {
            if (one.at() == null) {
                continue;
            }
            switch (one.request().family()) {
                case STAR, TARGET -> {
                    placedA++;
                    duressA += one.underDuress() ? 1 : 0;
                }
                case DEEP_SKY -> deepSkyA++;
                case CONSTELLATION -> namesA++;
            }
        }
        int deepSky = 0;
        int names = 0;
        for (LabelPlacement.Placement one : releasedPlacements) {
            if (one.at() == null) {
                continue;
            }
            if (one.request().family() == LabelPlacement.Family.DEEP_SKY) {
                deepSky++;
            } else if (one.request().family() == LabelPlacement.Family.CONSTELLATION) {
                names++;
            }
        }
        int releasedPlaced = 0;
        for (LabelPlacement.Placement one : releasedPlacements) {
            if (one.at() != null && (one.request().family() == LabelPlacement.Family.STAR
                    || one.request().family() == LabelPlacement.Family.TARGET)) {
                releasedPlaced++;
            }
        }
        int keptC = 0;
        int deepSkyC = 0;
        int namesC = 0;
        for (int i = 0; i < fuller.size(); i++) {
            LabelPlacement.Placement one = c.get(i);
            if (one.at() == null) {
                continue;
            }
            switch (one.request().family()) {
                case STAR, TARGET -> keptC++;
                case DEEP_SKY -> deepSkyC++;
                case CONSTELLATION -> namesC++;
            }
        }
        return new Measured(releasedStars, extras.size(), placedA, duressA, deepSkyA, namesA,
                placedB, refusedB, deepSky, names, richer, msA, msB, releasedPlaced, placedC,
                refusedC, keptC, deepSkyC, namesC, releasedPlacements, a, List.copyOf(b),
                List.copyOf(c));
    }

    /** The released composition with none of its thresholds: name and letter, the letter, the number. */
    private static String everything(Star star) {
        StarIdentity identity = star.identity();
        if (identity == null) {
            return null;
        }
        return new StarLabelPolicy.Forms(identity.name(),
                StarLabelPolicy.bayerNotation(identity), identity.flamsteed())
                .text(true, true, true);
    }

    private static int identities(ChartScene scene) {
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        int count = 0;
        for (Star star : scene.stars()) {
            if (everything(star) == null || star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            var plane = page.projection().project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint at = mapping.toPixel(plane.get());
            if (at.x() >= 0 && at.x() < WIDTH && at.y() >= 0
                    && at.y() < scene.viewport().heightPx()) {
                count++;
            }
        }
        return count;
    }

    private static void draw(ChartRenderer renderer, ChartScene scene,
                             List<LabelPlacement.Placement> placements, String file)
            throws Exception {
        BufferedImage image = new BufferedImage(WIDTH, scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            renderer.render(g, scene, ChartOptions.DEFAULTS,
                    ChartRenderer.ReferenceLayer.NONE, placements);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, file));
    }

    static SkyPosition find(String name) {
        for (SearchResult result : Atlas.search().search(name)) {
            if (result.kind() == SearchResult.Kind.STAR
                    && result.label().toLowerCase(Locale.ROOT).contains(
                            name.toLowerCase(Locale.ROOT))) {
                return result.position();
            }
        }
        throw new IllegalStateException("the bundled catalogue has no star named " + name);
    }

    private static String fieldToken(double field) {
        return String.format(Locale.ROOT, "%.0f", field);
    }

    private static String fmt(double value) {
        return value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
    }
}
