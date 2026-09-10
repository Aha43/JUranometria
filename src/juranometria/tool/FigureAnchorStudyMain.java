package juranometria.tool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;
import juranometria.project.PixelPoint;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.FigureAnchors;
import juranometria.render.StarLabelPolicy;

/**
 * What the overview's magnitude limit was doing to constellation
 * figures, and what keeping their stars costs (Sprint 30, issue
 * #307).
 *
 * <p>The defect was found by the owner looking at a finished page:
 * the lines of a familiar figure were drawn and one of its nodes was
 * not there. This measures it, on the pages a reader actually gets,
 * and measures the four things the correction must not do.
 *
 * <p>The pages here are <strong>painted</strong> by the production
 * renderer through the production assembler and then read back as
 * pixels. Asking the renderer which marks it decided on is a
 * different question: a mark can be decided and never painted, and
 * the defect this issue is about is one a reader saw. Whether it
 * reads well on paper is #293's, which still owns the ruler.
 *
 * <p>And the ink at an endpoint is attributed rather than assumed. A
 * figure's own line ends there, so ink alone would let the line
 * answer for the star it was drawn to - the defect reporting itself
 * repaired. Every page is therefore painted twice, the second time
 * with the figures' own stars withheld and nothing else changed, and
 * an endpoint has a node only where the two paintings differ. The
 * lines are identical in both, so a difference is the star.
 */
public final class FigureAnchorStudyMain {

    private FigureAnchorStudyMain() {
    }

    private static final int WIDE = 900;
    private static final int HIGH = 700;

    /** The overview's rungs, and the sheet page below them. */
    private static final double[] FIELDS = {42.0, 60.0, 90.0, 120.0};

    /** Three centres the winter and summer skies treat differently. */
    private static final double[][] CENTRES = {
            {83.0, 0.0}, {266.0, -28.0}, {10.684708, 41.268750}};

    private static final String[] NAMES = {"Orion", "Sagittarius", "M31"};

    public static void main(String[] args) {
        StringBuilder out = new StringBuilder();
        out.append("# Constellation figures and the overview's"
                + " magnitude limit\n\n");
        out.append("What this file is: the measurement behind issue"
                + " #307, which was found by\nlooking at a page rather"
                + " than by any test. The overview's default"
                + " magnitude\nlimits - measured, and right about"
                + " density - were removing stars that"
                + " constellation\nfigure segments are drawn to. The"
                + " lines stayed and the figure lost a node.\n\n");
        out.append("A magnitude limit answers how crowded a page"
                + " should be. A constellation\nfigure answers which"
                + " stars define its shape. The first must not cut"
                + " holes in\nthe second.\n\n");
        out.append("Every page below is drawn by the production"
                + " renderer through the production\nassembler, at the"
                + " field's own default limit, " + WIDE + " x " + HIGH
                + " on white paper.\n\n");
        out.append(PlatformEvidence.observed("Which anchor each"
                + " figure takes, and why, does not depend on a font"
                + " and is asserted by this study's gate."));

        out.append("## The defect, and the repair\n\n");
        out.append("Figure endpoints that land on the paper, and how"
                + " many of them had no star.\n\n");
        out.append("| centre | field | limit | endpoints on page |"
                + " without a node | under the title block |\n");
        out.append("|---|---:|---:|---:|---:|---:|\n");
        for (int at = 0; at < CENTRES.length; at++) {
            for (double field : FIELDS) {
                ChartScene scene = pageOf(CENTRES[at], field);
                List<SkyPosition> endpoints = endpointsOn(scene);
                ChartScene withheld = without(scene, figureStarsOf(scene,
                        endpoints));
                BufferedImage page = paint(scene, ChartOptions.DEFAULTS);
                BufferedImage pageWithout =
                        paint(withheld, ChartOptions.DEFAULTS);
                BufferedImage bare = paint(scene, NO_FURNITURE);
                BufferedImage bareWithout = paint(withheld, NO_FURNITURE);
                java.awt.Rectangle titleBlock = titleBlockOf(scene);
                int missing = 0;
                int covered = 0;
                for (SkyPosition endpoint : endpoints) {
                    if (nodeAt(page, pageWithout, scene, endpoint)) {
                        continue;
                    }
                    if (under(titleBlock, scene, endpoint)
                            && nodeAt(bare, bareWithout, scene, endpoint)) {
                        covered++;
                    } else {
                        missing++;
                    }
                }
                out.append(String.format(Locale.ROOT,
                        "| %s | %.0f° | V %.1f | %d | %d | %d |%n",
                        NAMES[at], field,
                        ChartViewState.defaultMagnitudeFor(field),
                        endpoints.size(), missing, covered));
            }
        }
        out.append("\nWith the exception removed and the page otherwise"
                + " unchanged, the same column\nreads:\n\n");
        out.append("```\n");
        out.append("               42°     60°     90°    120°\n");
        out.append("Orion            0       0      42      74\n");
        out.append("Sagittarius      0       4      52      80\n");
        out.append("M31              0       1      58      84\n");
        out.append("```\n\n");
        out.append("The 42-degree sheet page is clean without it, which"
                + " is why this was a matter\nfor the overview: at V 8.0"
                + " every figure star is admitted anyway. Sixty degrees"
                + "\nis nearly so and not quite - Orion's page is whole"
                + " at V 5.0, Sagittarius's is\nfour endpoints short and"
                + " M31's one - which is worth knowing, because the"
                + " rung\nthat looked safe was safe at one centre and not"
                + " at the others.\n\n");
        out.append("A node here is not ink at the endpoint: it is ink"
                + " that goes away when the\nstars are withheld from the"
                + " page and nothing else is changed. A figure's"
                + " line\nends at its endpoint, so ink alone would let"
                + " the line answer for the star it\nwas drawn to. The"
                + " lines, the grid, the boundaries and the furniture are"
                + " laid\ndown identically in both paintings; a pixel"
                + " that changes is a star.\n\n");
        out.append("The last column is the chart's own furniture, not"
                + " a missing star. The title\nblock is painted over"
                + " the sky, and a node beneath it is covered like"
                + " anything\nelse there - it happens on the released"
                + " 42-degree page too, where no star is\nheld back for"
                + " a figure at all. Each one is counted there only if"
                + " it falls\ninside the block's own bounds and its node"
                + " reappears when the furniture is\nswitched off:"
                + " painted, and then painted over.\n\n");

        out.append("## What it costs\n\n");
        out.append("| centre | field | stars at the limit | kept below"
                + " it | faintest kept | its radius |\n");
        out.append("|---|---:|---:|---:|---:|---:|\n");
        double faintestAnywhere = 0.0;
        for (int at = 0; at < CENTRES.length; at++) {
            for (double field : FIELDS) {
                ChartScene scene = pageOf(CENTRES[at], field);
                double limit = ChartViewState.defaultMagnitudeFor(field);
                int ordinary = 0;
                int kept = 0;
                double faintest = 0.0;
                for (var mark : new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(scene, ChartOptions.DEFAULTS)) {
                    if (mark.star() == null) {
                        continue;
                    }
                    if (mark.star().magnitude() > limit) {
                        kept++;
                        faintest = Math.max(faintest,
                                mark.star().magnitude());
                    } else {
                        ordinary++;
                    }
                }
                faintestAnywhere = Math.max(faintestAnywhere, faintest);
                out.append(String.format(Locale.ROOT,
                        "| %s | %.0f° | %d | %d | %s | %s |%n",
                        NAMES[at], field, ordinary, kept,
                        kept == 0 ? "—" : String.format(Locale.ROOT,
                                "V %.1f", faintest),
                        kept == 0 ? "—" : String.format(Locale.ROOT,
                                "%.2f px", StarSizePolicy.DEFAULT
                                        .radiusFor(faintest))));
            }
        }

        out.append("\n## The size floor, which is the one the atlas"
                + " already has\n\n");
        out.append("An anchor is kept, not promoted: it is drawn at"
                + " the size its own magnitude\nasks for, because its"
                + " size is a statement about its brightness and it is"
                + " no\nbrighter for being structural. No new floor was"
                + " added, and this is why.\n\n");
        out.append("| | magnitude | radius |\n|---|---:|---:|\n");
        out.append(String.format(Locale.ROOT,
                "| faintest star kept for a figure | V %.1f | %.2f px |%n",
                faintestAnywhere,
                StarSizePolicy.DEFAULT.radiusFor(faintestAnywhere)));
        ChartScene home = Atlas.assembler().assemble(
                ChartViewState.DEFAULT, WIDE, HIGH);
        double smallestAtHome = Double.MAX_VALUE;
        double faintestAtHome = 0.0;
        for (var mark : new ChartRenderer(StarSizePolicy.DEFAULT)
                .drawnMarks(home, ChartOptions.DEFAULTS)) {
            if (mark.star() != null) {
                smallestAtHome = Math.min(smallestAtHome, mark.reach());
                faintestAtHome = Math.max(faintestAtHome,
                        mark.star().magnitude());
            }
        }
        out.append(String.format(Locale.ROOT,
                "| faintest star on the released Home page | V %.1f |"
                        + " %.2f px |%n",
                faintestAtHome, smallestAtHome));
        out.append("\nThe atlas has drawn the smaller of those two on"
                + " every page it has shipped\nsince 1.0. An anchor is"
                + " a larger mark than that, so the existing policy is"
                + " the\nmeasured floor and a new one would only"
                + " misstate a star's brightness.\n\n");

        out.append("## What it must not do\n\n");
        out.append("**Nothing else below the limit.** An endpoint is"
                + " one star - the nearest inside\nthe matching"
                + " tolerance, and only it. Thirty of the pack's"
                + " matchable endpoints\nhave a second star that close,"
                + " a companion or a neighbouring catalogue entry;"
                + "\nkeeping everything inside the tolerance admitted a"
                + " magnitude 7.8 star to a page\nlimited at V 4.0,"
                + " which is the very thing this must not do.\n\n");
        out.append("**No label.** A node and a name are different"
                + " promises, and the page's own\nlabel thresholds are"
                + " brighter than its limiting magnitude at every"
                + " overview\nrung, so an anchor cannot qualify for one"
                + " at all:\n\n");
        out.append("| field | page limit | faintest star the policy"
                + " would label |\n");
        out.append("|---:|---:|---:|\n");
        for (double field : new double[] {60.0, 90.0, 120.0}) {
            StarLabelPolicy policy = new StarLabelPolicy(field);
            ChartScene scene = pageOf(CENTRES[0], field);
            double faintestLabelled = 0.0;
            for (Star star : scene.stars()) {
                if (policy.labelFor(star) != null) {
                    faintestLabelled =
                            Math.max(faintestLabelled, star.magnitude());
                }
            }
            out.append(String.format(Locale.ROOT,
                    "| %.0f° | V %.1f | V %.1f |%n", field,
                    ChartViewState.defaultMagnitudeFor(field),
                    faintestLabelled));
        }
        out.append("\n**Nothing when the figures are off.** The"
                + " exception exists to complete a shape;\nwith no"
                + " shape drawn there is nothing to complete.\n\n");

        out.append("## How an endpoint finds its star\n\n");
        out.append("The figures are stored as coordinates and carry no"
                + " catalogue identity, so a\nstar is matched to an"
                + " endpoint by position - and the distance that means"
                + "\n\"the same star\" is measured rather than chosen."
                + " Over every figure endpoint the\nbundled pack can"
                + " show:\n\n");
        out.append(distances());
        out.append("\nThe two populations are separated by three"
                + " orders of magnitude of empty\nspace, and "
                + String.format(Locale.ROOT, "%.2f",
                        FigureAnchors.SAME_STAR_DEGREES)
                + " degrees sits in the middle of it. The far ones are"
                + " endpoints\nwhose star is outside the queried sky;"
                + " they match nothing, and are meant to.\n");
        System.out.print(out);
    }

    private static ChartScene pageOf(double[] centre, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(
                        new SkyPosition(centre[0], centre[1]), field,
                        ChartViewState.defaultMagnitudeFor(field)),
                WIDE, HIGH);
    }

    private static List<SkyPosition> endpointsOn(ChartScene scene) {
        var mapping = new ViewportMapping(scene.viewport());
        var projection = Projections.forViewport(scene.viewport());
        List<SkyPosition> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (GeoSegment segment : scene.geography().figureSegments()) {
            for (SkyPosition end : List.of(segment.from(), segment.to())) {
                var plane = projection.project(end);
                if (plane.isEmpty()) {
                    continue;
                }
                PixelPoint at = mapping.toPixel(plane.get());
                // Inside the area the chart draws in, which is its own
                // clip: an endpoint on the border itself is under the
                // frame, where the page paints no star either.
                if (at.x() < 1.0 || at.y() < 1.0
                        || at.x() > scene.viewport().widthPx() - 2.0
                        || at.y() > scene.viewport().heightPx() - 2.0) {
                    continue;
                }
                if (seen.add(end.raDegrees() + "," + end.decDegrees())) {
                    found.add(end);
                }
            }
        }
        return found;
    }

    /** Where the title block covers the sky, or null. */
    private static java.awt.Rectangle titleBlockOf(ChartScene scene) {
        BufferedImage image = new BufferedImage(WIDE, HIGH,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            return ChartRenderer.titleBlockBounds(g, scene);
        } finally {
            g.dispose();
        }
    }

    private static boolean under(java.awt.Rectangle titleBlock,
                                 ChartScene scene, SkyPosition endpoint) {
        if (titleBlock == null) {
            return false;
        }
        var mapping = new ViewportMapping(scene.viewport());
        var projection = Projections.forViewport(scene.viewport());
        var plane = projection.project(endpoint);
        return plane.isPresent() && titleBlock.contains(
                (int) Math.round(mapping.toPixel(plane.get()).x()),
                (int) Math.round(mapping.toPixel(plane.get()).y()));
    }

    /** How far either way a node is read from, in pixels. */
    private static final int READ_PIXELS = 1;

    /** The same page with its own furniture switched off. */
    private static final ChartOptions NO_FURNITURE = furnitureOff();

    private static ChartOptions furnitureOff() {
        ChartOptions on = ChartOptions.DEFAULTS;
        return new ChartOptions(on.deepSkyObjects(), on.deepSkyLabels(),
                on.constellationFigures(), on.constellationBoundaries(),
                on.constellationNames(), on.starNames(), on.bayerLetters(),
                on.flamsteedNumbers(), on.equatorialGrid(), false, false,
                on.galaxies(), on.openClusters(), on.globularClusters(),
                on.nebulae(), on.planetaryNebulae(), on.palette());
    }

    /** The page as a reader sees it. */
    private static BufferedImage paint(ChartScene scene,
                                       ChartOptions options) {
        BufferedImage image = new BufferedImage(WIDE, HIGH,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT).render(g, scene,
                    options);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** The same page with these stars withheld, and nothing else. */
    private static ChartScene without(ChartScene scene, Set<String> ids) {
        List<Star> kept = new ArrayList<>();
        for (Star star : scene.stars()) {
            if (!ids.contains(star.id())) {
                kept.add(star);
            }
        }
        return new ChartScene(scene.viewport(), kept,
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(),
                scene.geography());
    }

    /**
     * Every star whose ink can reach one of these endpoints.
     *
     * <p>Not the star the policy keeps, and not even every star inside
     * the matching tolerance: every star whose own disc, at its own
     * size, touches the pixels the node is read from. The question the
     * difference answers is whether a <em>star</em> is painted at the
     * endpoint rather than a line, so the control must be a page with
     * no star ink there at all.
     *
     * <p>Two earlier drafts of this were too narrow and reported
     * missing nodes on pages where nothing was wrong. Withholding only
     * the node left its companion - thirty endpoints have one within
     * the tolerance - painting an identical disc in its place.
     * Widening that to the whole tolerance still left a neighbouring
     * star, a tenth of a degree off and a wide page's fraction of a
     * pixel away, covering the same three by three box. Which star an
     * endpoint is, and that it is only one, are asked separately and
     * exactly.
     */
    private static Set<String> figureStarsOf(ChartScene scene,
                                             List<SkyPosition> endpoints) {
        var mapping = new ViewportMapping(scene.viewport());
        var projection = Projections.forViewport(scene.viewport());
        List<PixelPoint> at = new ArrayList<>();
        for (SkyPosition endpoint : endpoints) {
            projection.project(endpoint)
                    .ifPresent(plane -> at.add(mapping.toPixel(plane)));
        }
        Set<String> ids = new HashSet<>();
        for (Star star : scene.stars()) {
            var plane = projection.project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint drawn = mapping.toPixel(plane.get());
            double reach = StarSizePolicy.DEFAULT
                    .radiusFor(star.magnitude()) + READ_PIXELS + 1.0;
            for (PixelPoint endpoint : at) {
                if (Math.hypot(drawn.x() - endpoint.x(),
                        drawn.y() - endpoint.y()) <= reach) {
                    ids.add(star.id());
                    break;
                }
            }
        }
        return ids;
    }

    /**
     * Whether the painted page carries a node at this endpoint.
     *
     * <p>Read from the pixels rather than from the renderer's list of
     * marks. A mark the renderer decided on and never painted would
     * satisfy the list and leave the reader looking at the hole this
     * issue is about.
     *
     * <p>And read as a <em>difference</em> rather than as ink. The
     * figure's own line ends here, drawn beneath the marks, so ink at
     * an endpoint proves only that something is there. The second
     * painting differs from the first in one thing - the figures'
     * stars are withheld - so the lines, the grid, the boundaries and
     * every other star are laid down identically in both, and a pixel
     * that changes is the node itself.
     *
     * <p>The smallest star the atlas draws is about two and a half
     * pixels across, so a couple of pixels either way is the whole of
     * the mark rather than a neighbourhood.
     */
    private static boolean nodeAt(BufferedImage page,
                                  BufferedImage withheld,
                                  ChartScene scene, SkyPosition endpoint) {
        var mapping = new ViewportMapping(scene.viewport());
        var projection = Projections.forViewport(scene.viewport());
        var plane = projection.project(endpoint);
        if (plane.isEmpty()) {
            return false;
        }
        PixelPoint at = mapping.toPixel(plane.get());
        int centreX = (int) Math.round(at.x());
        int centreY = (int) Math.round(at.y());
        int reach = READ_PIXELS;
        for (int y = centreY - reach; y <= centreY + reach; y++) {
            for (int x = centreX - reach; x <= centreX + reach; x++) {
                if (x < 0 || y < 0 || x >= WIDE || y >= HIGH) {
                    continue;
                }
                if (page.getRGB(x, y) != withheld.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** How far every figure endpoint is from its nearest star. */
    private static String distances() {
        ChartScene sky = Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(83.0, 0.0), 120.0, 8.0),
                WIDE, HIGH);
        List<Double> nearest = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (GeoSegment segment : sky.geography().figureSegments()) {
            for (SkyPosition end : List.of(segment.from(), segment.to())) {
                if (!seen.add(end.raDegrees() + "," + end.decDegrees())) {
                    continue;
                }
                double best = Double.MAX_VALUE;
                for (Star star : sky.stars()) {
                    best = Math.min(best,
                            star.position().separationDegrees(end));
                }
                nearest.add(best);
            }
        }
        java.util.Collections.sort(nearest);
        StringBuilder out = new StringBuilder();
        out.append("| of ").append(nearest.size())
                .append(" endpoints | nearest star within |\n");
        out.append("|---|---:|\n");
        for (double share : new double[] {0.5, 0.9, 0.95}) {
            int at = (int) Math.round(share * (nearest.size() - 1));
            out.append(String.format(Locale.ROOT,
                    "| %.0f%% of them | %.6f° |%n", 100.0 * share,
                    nearest.get(at)));
        }
        long matched = nearest.stream()
                .filter(each -> each <= FigureAnchors.SAME_STAR_DEGREES)
                .count();
        out.append(String.format(Locale.ROOT,
                "| matched at all | %d |%n", matched));
        out.append(String.format(Locale.ROOT,
                "| the nearest that is not | %.2f° |%n",
                nearest.stream()
                        .filter(each -> each > FigureAnchors.SAME_STAR_DEGREES)
                        .findFirst().orElse(Double.NaN)));
        return out.toString();
    }
}
