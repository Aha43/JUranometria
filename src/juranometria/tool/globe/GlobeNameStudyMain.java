package juranometria.tool.globe;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.app.Atlas;
import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;

/**
 * Whether a hemisphere's names still name anything (Sprint 32, issue
 * #301).
 *
 * <p>Counting successful placements would say almost nothing. The
 * shared policy finds a free rectangle for most labels most of the
 * time; the question a reader cares about is whether the rectangle it
 * found is near enough to the thing it names, and near enough that no
 * neighbour claims it instead.
 *
 * <p><strong>The shared placement policy is used unchanged.</strong>
 * Nothing here places anything: it asks the production renderer for
 * its own placements and then asks what became of them. If the policy
 * cannot cope near the limb, that is a finding to bring back rather
 * than grounds for a private globe rule (#301).
 *
 * <p>Three outcomes, by family and by band:
 *
 * <ul>
 *   <li><strong>identified</strong> - its own object is strictly
 *       nearer the label's box than every same-family rival;</li>
 *   <li><strong>doubtful</strong> - another object of the same family
 *       is equally near or nearer, so a reader cannot attribute the
 *       name whatever the distance;</li>
 *   <li><strong>omitted</strong> - the shared policy found no
 *       truthful placement at all.</li>
 * </ul>
 *
 * <p>Distances are measured from the label's actual box to the ink
 * the object owns - the mark or symbol the reader sees - rather than
 * from a text origin, and the candidate rank is reported beside them,
 * so a label that is still attributable but has been pushed to a late
 * position near the limb can be told from one that sits where it
 * first asked.
 */
public final class GlobeNameStudyMain {

    private GlobeNameStudyMain() {
    }

    static final File DIR = new File("build/globe-study/names");

    private static final int SIDE_PX = 900;
    private static final double FRAME = 0.90;

    /** The settled page: V 5.0, boundaries off (#301). */
    private static final double LIMIT = 5.0;

    /** Where a label's box sits in relation to the globe's edge. */
    private enum Against {
        /** Wholly on the sphere. */
        INSIDE,
        /** Half on the sphere and half on the paper beyond it. */
        CROSSING,
        /** Entirely on the paper outside the sphere. */
        OUTSIDE
    }

    /** What became of one name. */
    private record Named(LabelPlacement.Family family, String id,
                         double radiusOnDisc, int candidate,
                         double toOwn, double toRival,
                         boolean omitted, Against against,
                         List<LabelPlacement.Refusal> refusedBy) {

        boolean identified() {
            return !omitted && toOwn < toRival;
        }

        boolean doubtful() {
            return !omitted && !identified();
        }
    }

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        System.out.println("# Whether a hemisphere's names name"
                + " anything");
        System.out.println();
        System.out.println("The production placement policy, asked"
                + " what became of its own placements. Identified");
        System.out.println("means the label's own object is strictly"
                + " nearer its box than every same-family rival;");
        System.out.println("doubtful means one is as near or nearer;"
                + " omitted means no truthful placement exists.");

        for (var look : GlobeDensityStudyMain.corpus().subList(0, 2)) {
            DrawnPage page = Atlas.assembler().assembleForStudy(
                    look.centre(), 180.0, LIMIT, look.title(),
                    new GlobeProjection(look.centre()), SIDE_PX,
                    SIDE_PX);
            List<Named> names = examine(page);
            System.out.println();
            System.out.println(look.slug() + ":");
            report("centre", names, 0.0, 0.5);
            report("limb", names, 0.9, 1.0);
            report("whole disc", names, 0.0, 1.0);
        }
    }

    private static void report(String band, List<Named> all,
                               double from, double to) {
        List<Named> inBand = new ArrayList<>();
        for (Named one : all) {
            if (one.radiusOnDisc() >= from && one.radiusOnDisc() <= to) {
                inBand.add(one);
            }
        }
        System.out.printf(Locale.ROOT, "  %s:%n", band);
        System.out.printf(Locale.ROOT,
                "    %-16s %6s %9s %8s %7s %6s %8s %7s %7s%n",
                "family", "asked", "identified", "doubtful", "omitted",
                "rank", "in disc", "crosses", "outside");

        Map<LabelPlacement.Family, List<Named>> byFamily =
                new LinkedHashMap<>();
        for (Named one : inBand) {
            byFamily.computeIfAbsent(one.family(),
                    key -> new ArrayList<>()).add(one);
        }
        for (var entry : byFamily.entrySet()) {
            List<Named> family = entry.getValue();
            long identified = family.stream()
                    .filter(Named::identified).count();
            long doubtful = family.stream()
                    .filter(Named::doubtful).count();
            long omitted = family.stream()
                    .filter(Named::omitted).count();
            int[] ranks = family.stream().filter(one -> !one.omitted())
                    .mapToInt(Named::candidate).sorted().toArray();
            System.out.printf(Locale.ROOT,
                    "    %-16s %6d %9d %8d %7d %6s %8d %7d %7d%n",
                    entry.getKey(), family.size(), identified,
                    doubtful, omitted,
                    ranks.length == 0 ? "-"
                            : String.valueOf(ranks[ranks.length / 2]),
                    count(family, Against.INSIDE),
                    count(family, Against.CROSSING),
                    count(family, Against.OUTSIDE));
        }

        // Omissions by cause, always. On a globe the paper's edge and
        // the limb are different boundaries, so which one refused a
        // label is part of the decision rather than diagnostic detail.
        Map<LabelPlacement.Refusal, Integer> causes =
                new LinkedHashMap<>();
        for (Named one : inBand) {
            if (!one.omitted()) {
                continue;
            }
            for (LabelPlacement.Refusal kind : one.refusedBy()) {
                causes.merge(kind, 1, Integer::sum);
            }
        }
        if (!causes.isEmpty()) {
            StringBuilder said = new StringBuilder();
            for (var cause : causes.entrySet()) {
                said.append(said.isEmpty() ? "" : ", ")
                        .append(cause.getKey()).append(" ")
                        .append(cause.getValue());
            }
            System.out.printf(Locale.ROOT,
                    "    omitted labels were refused by: %s%n", said);
        }
    }

    private static long count(List<Named> of, Against where) {
        return of.stream().filter(one -> one.against() == where)
                .count();
    }

    /** Every placement the production policy made, examined. */
    private static List<Named> examine(DrawnPage page) {
        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        List<LabelPlacement.Placement> placements;
        try {
            ChartRenderer renderer = ChartRenderer.drawing(page,
                    StarSizePolicy.DEFAULT);
            placements = renderer.textPlacements(
                    ChartRenderer.TextMetrics.of(g), page.scene(),
                    settled());
        } finally {
            g.dispose();
        }

        double centre = SIDE_PX / 2.0;
        double discRadius = FRAME * SIDE_PX / 2.0;
        List<Named> examined = new ArrayList<>();

        for (LabelPlacement.Placement placement : placements) {
            LabelPlacement.Request request = placement.request();
            Shape owns = request.owns();
            Rectangle2D mine = owns == null ? null : owns.getBounds2D();
            double radius = mine == null
                    ? Math.hypot(request.anchorX() - centre,
                            request.anchorY() - centre) / discRadius
                    : Math.hypot(mine.getCenterX() - centre,
                            mine.getCenterY() - centre) / discRadius;

            List<LabelPlacement.Refusal> kinds = new ArrayList<>();
            for (LabelPlacement.Refused refused
                    : placement.refusals()) {
                if (!kinds.contains(refused.kind())) {
                    kinds.add(refused.kind());
                }
            }

            if (placement.omitted()) {
                examined.add(new Named(request.family(), request.id(),
                        radius, -1, 0.0, 0.0, true, null, kinds));
                continue;
            }

            double toOwn = mine == null ? 0.0
                    : between(placement.at(), mine);
            double toRival = Double.MAX_VALUE;
            for (LabelPlacement.Placement other : placements) {
                if (other == placement
                        || other.request().family() != request.family()
                        || other.request().owns() == null) {
                    continue;
                }
                toRival = Math.min(toRival, between(placement.at(),
                        other.request().owns().getBounds2D()));
            }
            examined.add(new Named(request.family(), request.id(),
                    radius, placement.candidate(), toOwn, toRival,
                    false, against(placement.at(), centre, discRadius),
                    kinds));
        }
        return examined;
    }

    /**
     * Where this box sits in relation to the limb.
     *
     * <p>The shared policy knows the paper's rectangular edge, and on
     * a globe that is not the boundary that matters: a visible object
     * near the limb can be given a perfectly attributable label out in
     * the white margin, which passes every test above and breaks the
     * settled rule that nothing floats outside the disc.
     *
     * <p>Exact for a rectangle against a circle: it is wholly inside
     * when its furthest corner is within the limb, wholly outside when
     * its nearest point is beyond it, and crossing otherwise.
     */
    private static Against against(Rectangle2D box, double centre,
                                   double discRadius) {
        double furthest = 0.0;
        for (double[] corner : new double[][] {
                {box.getMinX(), box.getMinY()},
                {box.getMaxX(), box.getMinY()},
                {box.getMinX(), box.getMaxY()},
                {box.getMaxX(), box.getMaxY()}}) {
            furthest = Math.max(furthest, Math.hypot(
                    corner[0] - centre, corner[1] - centre));
        }
        double nearestX = Math.max(box.getMinX(),
                Math.min(centre, box.getMaxX()));
        double nearestY = Math.max(box.getMinY(),
                Math.min(centre, box.getMaxY()));
        double nearest = Math.hypot(nearestX - centre,
                nearestY - centre);
        if (furthest <= discRadius) {
            return Against.INSIDE;
        }
        return nearest > discRadius ? Against.OUTSIDE
                : Against.CROSSING;
    }

    /**
     * The gap between a label's box and an object's ink, in pixels,
     * and zero where they overlap.
     */
    private static double between(Rectangle2D box, Rectangle2D ink) {
        double dx = Math.max(0.0, Math.max(ink.getMinX() - box.getMaxX(),
                box.getMinX() - ink.getMaxX()));
        double dy = Math.max(0.0, Math.max(ink.getMinY() - box.getMaxY(),
                box.getMinY() - ink.getMaxY()));
        return Math.hypot(dx, dy);
    }

    /** The page as #301 has settled it so far. */
    private static ChartOptions settled() {
        return new ChartOptions(
                true, true, true, false, true,
                true, true, true, true, true, false,
                true, true, true, true, true,
                ChartPalette.WHITE_PAPER);
    }
}
