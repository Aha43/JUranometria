package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;
import juranometria.render.RegionalDetailPolicy;

/**
 * The Sprint 17 Bayer-Flamsteed notation study (issue #153):
 * measures the released identity layer's real inventory - Greek and
 * Latin Bayer letters, component digits, Flamsteed numbers, stars
 * carrying both a name and a letter, and the ambiguity a bare letter
 * would carry - then renders candidate notation and density policies
 * over real pages.
 *
 * The candidate pass composes exactly as production does: the base
 * page renders with the released star labels suppressed (the
 * existing chart option), the candidate labels are placed against a
 * collision set seeded with the production deep-sky label boxes and
 * title block, and every box comes from the renderer's own shared
 * geometry ({@code ChartRenderer.starLabelBounds},
 * {@code labelBounds}, {@code titleBlockBounds}) rather than a
 * mirrored approximation. Run via "make bayer-study"; pages land in
 * build/bayer-study/.
 */
public final class BayerStudyMain {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    /** Which identity form a candidate policy shows for a star. */
    enum Form { NAME, LETTER, LATIN, NAME_AND_LETTER, FLAMSTEED, NONE }

    /**
     * A candidate policy: per field band, the magnitude limits for
     * proper names, Bayer letters, and Flamsteed numbers, whether
     * letters carry their constellation, and whether a star with
     * both a name and a letter shows both.
     */
    record Policy(String label, double nameWide, double nameMid,
                  double nameRegional, double bayerWide, double bayerMid,
                  double bayerRegional, double flamsteedRegional,
                  boolean qualified, boolean bothWhenAvailable,
                  boolean greekOnlyOutsideRegional) {

        double nameLimit(double field) {
            return field >= 24.0 ? nameWide : field >= 12.0 ? nameMid
                    : nameRegional;
        }

        double bayerLimit(double field) {
            return field >= 24.0 ? bayerWide : field >= 12.0 ? bayerMid
                    : bayerRegional;
        }

        double flamsteedLimit(double field) {
            return field >= 12.0 ? Double.NEGATIVE_INFINITY
                    : flamsteedRegional;
        }
    }

    /** Today's released behaviour, for paired comparison. */
    static final Policy RELEASED = new Policy("released",
            2.5, 3.0, 4.5,
            Double.NEGATIVE_INFINITY, 3.0, 4.5, 5.0, false, false, false);
    /** The proposal: letters reach the constellation pages. */
    static final Policy LETTERED = new Policy("lettered",
            2.5, 3.0, 4.5, 3.5, 4.5, 5.0, 5.5, false, false, false);
    /**
     * THE PROPOSAL: letters reach the constellation pages, names and
     * letters travel together, post-omega Latin letters wait for the
     * regional fields, and Flamsteed numbers keep their released
     * limit (the evidence below shows bare numbers are the weakest
     * notation - "32" beside "M 32" reads as a Messier number).
     */
    static final Policy PROPOSED = new Policy("proposed",
            2.5, 3.0, 4.5, 3.5, 4.5, 5.0, 5.0, false, true, true);
    /** The proposal with names and letters together where both exist. */
    static final Policy LETTERED_BOTH = new Policy("lettered-both",
            2.5, 3.0, 4.5, 3.5, 4.5, 5.0, 5.5, false, true, false);
    /** Letters qualified by their constellation abbreviation. */
    static final Policy QUALIFIED = new Policy("qualified",
            2.5, 3.0, 4.5, 3.5, 4.5, 5.0, 5.5, true, false, false);
    /** The bad alternative: every identifier the pack knows. */
    static final Policy EVERYTHING = new Policy("everything",
            9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, false, true, false);

    private static final char[] SUPERSCRIPTS = {
            '⁰', '¹', '²', '³', '⁴',
            '⁵', '⁶', '⁷', '⁸', '⁹'};

    private BayerStudyMain() {
    }

    /**
     * Conventional Bayer notation from the STRUCTURED identity: the
     * letter verbatim (Greek or post-omega Latin, as the pack
     * carries it) with its component digits raised, never inferred
     * from a display string.
     */
    static String bayerNotation(StarIdentity identity) {
        String bayer = identity.bayer();
        if (bayer == null) {
            return null;
        }
        int split = bayer.length();
        while (split > 0 && Character.isDigit(bayer.charAt(split - 1))) {
            split--;
        }
        StringBuilder out = new StringBuilder(bayer.substring(0, split));
        for (int i = split; i < bayer.length(); i++) {
            out.append(SUPERSCRIPTS[bayer.charAt(i) - '0']);
        }
        return out.toString();
    }

    /** The text a policy gives a star, and which form it is. */
    static String[] labelFor(Star star, Policy policy, double field) {
        StarIdentity identity = star.identity();
        if (identity == null) {
            return null;
        }
        double magnitude = star.magnitude();
        boolean name = identity.name() != null
                && magnitude <= policy.nameLimit(field);
        boolean greekLetter = identity.bayer() != null
                && identity.bayer().charAt(0) >= 'α'
                && identity.bayer().charAt(0) <= 'ω';
        boolean letter = identity.bayer() != null
                && magnitude <= policy.bayerLimit(field)
                && (greekLetter || !policy.greekOnlyOutsideRegional()
                        || field < 12.0);
        String notation = letter ? bayerNotation(identity) : null;
        if (policy.qualified() && notation != null) {
            notation = notation + " " + identity.constellation();
        }
        if (name && letter && policy.bothWhenAvailable()) {
            return new String[] {identity.name() + " " + notation,
                    Form.NAME_AND_LETTER.name()};
        }
        if (name) {
            return new String[] {identity.name(), Form.NAME.name()};
        }
        if (letter) {
            return new String[] {notation,
                    greekLetter ? Form.LETTER.name() : Form.LATIN.name()};
        }
        if (identity.flamsteed() != null
                && magnitude <= policy.flamsteedLimit(field)) {
            return new String[] {identity.flamsteed(), Form.FLAMSTEED.name()};
        }
        return null;
    }

    record PageResult(int names, int letters, int both, int flamsteed,
                      int rejected, int ambiguousLetters, double renderMs) {
    }

    public static void main(String[] args) throws Exception {
        File outDir = new File("build/bayer-study");
        outDir.mkdirs();
        inventory();

        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        record Page(String name, double ra, double dec, double field) {
        }
        List<Page> pages = List.of(
                new Page("orion-36", 83.818667, 0.0, 36.0),
                new Page("orion-18", 83.818667, -1.0, 18.0),
                new Page("orion-08", 83.818667, -5.389667, 8.0),
                new Page("andromeda-36", 15.0, 38.0, 36.0),
                new Page("ursa-major-36", 165.0, 56.0, 36.0),
                new Page("pleiades-08", 56.869167, 24.105278, 8.0),
                new Page("boundary-crossing-18", 120.0, 20.0, 18.0),
                new Page("polaris-36", 37.946619, 89.264135, 36.0),
                new Page("crux-18", 186.649563, -63.099093, 18.0),
                new Page("m31-08", 10.684708, 41.268750, 8.0));

        System.out.printf(Locale.ROOT, "%n%-24s %6s | %5s %5s %5s %5s |"
                        + " %8s %9s %8s%n",
                "page (policy)", "field", "names", "lettr", "both", "flams",
                "rejected", "ambiguous", "render");
        for (Page page : pages) {
            study(renderer, page.name(), new SkyPosition(page.ra(),
                    page.dec()), page.field(), PROPOSED, outDir);
        }
        // Paired comparisons on one page: what changes, and the two
        // alternatives the decision must weigh.
        for (Policy policy : List.of(RELEASED, LETTERED, QUALIFIED,
                EVERYTHING)) {
            study(renderer, "orion-36-" + policy.label(),
                    new SkyPosition(83.818667, 0.0), 36.0, policy, outDir);
        }
        study(renderer, "orion-08-" + RELEASED.label(),
                new SkyPosition(83.818667, -5.389667), 8.0, RELEASED, outDir);
        study(renderer, "m31-08-" + RELEASED.label(),
                new SkyPosition(10.684708, 41.268750), 8.0, RELEASED, outDir);
        study(renderer, "m31-08-" + PROPOSED.label(),
                new SkyPosition(10.684708, 41.268750), 8.0, PROPOSED,
                outDir);
        study(renderer, "crux-18-" + PROPOSED.label(),
                new SkyPosition(186.649563, -63.099093), 18.0, PROPOSED,
                outDir);
        System.out.println("Pages written to " + outDir);
    }

    /**
     * The released identity layer counted by rendering category -
     * returned, not merely printed, so the gate's tests execute the
     * same census the decision quotes (PR #157 review).
     */
    record Inventory(int identities, int greek, int latin, int components,
                     int flamsteed, int names, int nameAndLetter,
                     int letterOnly, int distinctLetters,
                     int sharedLetters, String widestLetter,
                     int widestSpread) {
    }

    static Inventory inventory() throws Exception {
        var identities = juranometria.catalog.StarIdentities.load();
        Map<String, java.util.Set<String>> constellationsPerLetter =
                new TreeMap<>();
        int greek = 0;
        int latin = 0;
        int components = 0;
        int flamsteed = 0;
        int names = 0;
        int nameAndLetter = 0;
        int letterOnly = 0;
        var wholeSky = new juranometria.chart.SkyRegion(
                Atlas.DEFAULT_CENTRE, 180.0);
        for (Star star : juranometria.catalog.TiledCatalogue.load()
                .starsIn(wholeSky)) {
            StarIdentity identity = star.identity();
            if (identity == null) {
                continue;
            }
            if (identity.name() != null) {
                names++;
            }
            if (identity.bayer() != null) {
                char first = identity.bayer().charAt(0);
                if (first >= 'α' && first <= 'ω') {
                    greek++;
                } else {
                    latin++;
                }
                String bare = identity.bayer().replaceAll("\\d+$", "");
                constellationsPerLetter
                        .computeIfAbsent(bare, key -> new java.util.TreeSet<>())
                        .add(identity.constellation());
                if (!bare.equals(identity.bayer())) {
                    components++;
                }
                if (identity.name() != null) {
                    nameAndLetter++;
                } else {
                    letterOnly++;
                }
            }
            if (identity.flamsteed() != null) {
                flamsteed++;
            }
        }
        // Shared means used in more than one CONSTELLATION - the
        // ambiguity that matters for an unqualified label - not
        // merely carried by more than one star (PR #157 review).
        int sharedLetters = 0;
        int worstSpread = 0;
        String worstLetter = "";
        for (var entry : constellationsPerLetter.entrySet()) {
            int spread = entry.getValue().size();
            if (spread > 1) {
                sharedLetters++;
            }
            if (spread > worstSpread) {
                worstSpread = spread;
                worstLetter = entry.getKey();
            }
        }
        System.out.printf(Locale.ROOT,
                "identity inventory (the released pack, %d identities):%n"
                        + "  Bayer %d = Greek %d + post-omega Latin %d;"
                        + " %d carry component digits%n"
                        + "  Flamsteed %d; proper names %d"
                        + " (%d also lettered, %d lettered without a name)%n"
                        + "  %d distinct bare letters, %d of them used in"
                        + " more than one constellation (worst: '%s' in %d"
                        + " constellations) - a bare letter is unique only"
                        + " in context%n",
                identities.size(), greek + latin, greek, latin, components,
                flamsteed, names, nameAndLetter, letterOnly,
                constellationsPerLetter.size(), sharedLetters, worstLetter,
                worstSpread);
        System.out.println("  conventional notation from structured"
                + " identity: pi + '1' -> "
                + bayerNotation(new StarIdentity(null, "π1", null, "Ori"))
                + ", alpha + '2' -> "
                + bayerNotation(new StarIdentity(null, "α2", null, "Cru")));
        return new Inventory(identities.size(), greek, latin, components,
                flamsteed, names, nameAndLetter, letterOnly,
                constellationsPerLetter.size(), sharedLetters, worstLetter,
                worstSpread);
    }

    /** Per-page counts the candidate pass produced. */
    record Counts(int names, int letters, int latin, int both, int flams,
                  int rejected, int ambiguous) {
    }

    /**
     * The candidate label pass, composed exactly as production
     * composes its own: boxes from the renderer's shared geometry,
     * the collision set seeded with the deep-sky labels and title
     * block production seeds (and NOT the grid labels production
     * ignores), brightest star first with the stable TYC tie-break.
     * Draws when {@code g} is present; measures either way.
     */
    static Counts candidatePass(java.awt.image.BufferedImage image,
                                ChartScene scene, double field,
                                Policy policy, Graphics2D drawInto) {
        Graphics2D g = drawInto != null ? drawInto : image.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(ChartRenderer.labelFont());
        g.setColor(new java.awt.Color(34, 34, 34));
        var metrics = g.getFontMetrics();
        var centre = scene.viewport().centre();
        var projection = new GnomonicProjection(centre);
        var mapping = new ViewportMapping(scene.viewport());
        StarSizePolicy sizes = StarSizePolicy.DEFAULT;

        List<Rectangle2D> occupied = new ArrayList<>();
        var title = ChartRenderer.titleBlockBounds(g, scene);
        if (title != null) {
            occupied.add(title);
        }
        var detail = new RegionalDetailPolicy(scene,
                mapping.pixelsPerPlaneUnit());
        for (var dso : scene.deepSkyObjects()) {
            if (!detail.labelled(dso)) {
                continue;
            }
            var plane = projection.project(dso.position());
            if (plane.isPresent()) {
                occupied.add(ChartRenderer.labelBounds(metrics, dso,
                        mapping.toPixel(plane.get()),
                        mapping.pixelsPerPlaneUnit()));
            }
        }

        List<Star> stars = new ArrayList<>(scene.stars());
        stars.sort(java.util.Comparator.comparingDouble(Star::magnitude)
                .thenComparing(Star::id));
        int names = 0;
        int letters = 0;
        int latin = 0;
        int both = 0;
        int flams = 0;
        int rejected = 0;
        int ambiguous = 0;
        Map<String, String> letterOwners = new HashMap<>();
        for (Star star : stars) {
            if (star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            String[] label = labelFor(star, policy, field);
            if (label == null) {
                continue;
            }
            var plane = projection.project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint pixel = mapping.toPixel(plane.get());
            if (pixel.x() < 0 || pixel.x() >= scene.viewport().widthPx()
                    || pixel.y() < 0
                    || pixel.y() >= scene.viewport().heightPx()) {
                continue;
            }
            Rectangle2D box = ChartRenderer.starLabelBounds(metrics,
                    label[0], pixel, sizes.radiusFor(star.magnitude()));
            boolean collides = false;
            for (Rectangle2D other : occupied) {
                if (other.intersects(box)) {
                    collides = true;
                    break;
                }
            }
            if (collides) {
                rejected++;
                continue;
            }
            occupied.add(box);
            g.drawString(label[0], (float) (box.getX() + 2.0),
                    (float) (box.getY() + metrics.getAscent()));
            switch (Form.valueOf(label[1])) {
                case NAME -> names++;
                case LETTER -> letters++;
                case LATIN -> latin++;
                case NAME_AND_LETTER -> both++;
                default -> flams++;
            }
            if (star.identity().bayer() != null) {
                String bare = star.identity().bayer().replaceAll("\\d+$", "");
                String owner = letterOwners.putIfAbsent(bare,
                        star.identity().constellation());
                if (owner != null
                        && !owner.equals(star.identity().constellation())) {
                    ambiguous++;
                }
            }
        }
        if (drawInto == null) {
            g.dispose();
        }
        return new Counts(names, letters, latin, both, flams, rejected,
                ambiguous);
    }

    private static void study(ChartRenderer renderer, String name,
                              SkyPosition centre, double field,
                              Policy policy, File outDir) throws Exception {
        ChartViewState state = new ChartViewState(centre, field, 8.0, null, null);
        ChartScene scene = Atlas.assembler().assemble(state, WIDTH, HEIGHT);
        ChartOptions base = new ChartOptions(
                true, true, true, true, true, false, true);
        // Warm timing covering BOTH the base page and the candidate
        // label pass - the whole cost a reader would pay (PR #157
        // review): three warm-up rounds, then the best of five.
        for (int warm = 0; warm < 3; warm++) {
            candidatePass(renderer.renderToImage(scene, base), scene,
                    field, policy, null);
        }
        long best = Long.MAX_VALUE;
        for (int round = 0; round < 5; round++) {
            long t0 = System.nanoTime();
            candidatePass(renderer.renderToImage(scene, base), scene,
                    field, policy, null);
            best = Math.min(best, System.nanoTime() - t0);
        }

        var image = renderer.renderToImage(scene, base);
        Graphics2D g = image.createGraphics();
        Counts counts = candidatePass(image, scene, field, policy, g);
        g.dispose();
        ImageIO.write(image, "png", new File(outDir, name + ".png"));
        System.out.printf(Locale.ROOT, "%-24s %5.0f\u00b0 | %5d %5d %5d %5d"
                        + " | %8d %9d %6.1fms%n",
                name + " (" + policy.label() + ")", field, counts.names(),
                counts.letters() + counts.latin(), counts.both(),
                counts.flams(), counts.rejected(), counts.ambiguous(),
                best / 1e6);
        if (counts.latin() > 0) {
            System.out.printf(Locale.ROOT,
                    "    (of the %d letters, %d are post-omega Latin)%n",
                    counts.letters() + counts.latin(), counts.latin());
        }
    }
}
