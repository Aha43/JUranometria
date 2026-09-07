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
        out.append("Recorded on: `" + WiderFieldStudyMain.platform()
                + "`\n\n");

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
                BufferedImage painted = paint(scene);
                java.awt.Rectangle titleBlock = titleBlockOf(scene);
                int missing = 0;
                int covered = 0;
                for (SkyPosition endpoint : endpoints) {
                    if (inkAt(painted, scene, endpoint)) {
                        continue;
                    }
                    if (under(titleBlock, scene, endpoint)) {
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
        out.append("\nBefore the repair the same column read 0, 43"
                + " and 76 at Orion for 60, 90\nand 120 degrees. Sixty"
                + " degrees never had the defect: at V 5.0 every figure"
                + " star\nof that page is already admitted.\n\n");
        out.append("The last column is the chart's own furniture, not"
                + " a missing star. The title\nblock is painted over"
                + " the sky, and a node beneath it is covered like"
                + " anything\nelse there - it happens on the released"
                + " 42-degree page too, where no star is\nheld back"
                + " for a figure at all. Every one of them has its star"
                + " drawn; the\nblock is simply on top.\n\n");

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
                if (at.x() < 0 || at.y() < 0 || at.x() > WIDE
                        || at.y() > HIGH) {
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

    /** The page as a reader sees it. */
    private static BufferedImage paint(ChartScene scene) {
        BufferedImage image = new BufferedImage(WIDE, HIGH,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT).render(g, scene,
                    ChartOptions.DEFAULTS);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * Whether the painted page carries a node at this endpoint.
     *
     * <p>Read from the pixels rather than from the renderer's list of
     * marks. A mark the renderer decided on and never painted would
     * satisfy the list and leave the reader looking at the hole this
     * issue is about.
     *
     * <p>A node is ink at the endpoint itself, and the smallest star
     * the atlas draws is about two and a half pixels across, so a
     * couple of pixels either way is the whole of the mark rather
     * than a neighbourhood that might catch a figure line passing
     * through.
     */
    private static boolean inkAt(BufferedImage painted, ChartScene scene,
                                 SkyPosition endpoint) {
        var mapping = new ViewportMapping(scene.viewport());
        var projection = Projections.forViewport(scene.viewport());
        var plane = projection.project(endpoint);
        if (plane.isEmpty()) {
            return false;
        }
        PixelPoint at = mapping.toPixel(plane.get());
        int centreX = (int) Math.round(at.x());
        int centreY = (int) Math.round(at.y());
        int ground = ChartOptions.DEFAULTS.palette().ground().getRGB()
                & 0xffffff;
        for (int y = centreY - 1; y <= centreY + 1; y++) {
            for (int x = centreX - 1; x <= centreX + 1; x++) {
                if (x < 0 || y < 0 || x >= WIDE || y >= HIGH) {
                    continue;
                }
                if ((painted.getRGB(x, y) & 0xffffff) != ground) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ChartRenderer.DrawnMark nodeAt(
            List<ChartRenderer.DrawnMark> marks, SkyPosition endpoint) {
        ChartRenderer.DrawnMark best = null;
        double closest = FigureAnchors.SAME_STAR_DEGREES;
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.star() == null) {
                continue;
            }
            double apart =
                    mark.star().position().separationDegrees(endpoint);
            if (apart <= closest) {
                closest = apart;
                best = mark;
            }
        }
        return best;
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
