package juranometria.render;

import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.Star;
import juranometria.project.PixelPoint;
import juranometria.project.DrawnPage;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;

/**
 * The measuring boundary: what a page's text is, in geometry (Sprint
 * 31, issue #313).
 *
 * <p>{@link LabelPlacement} decides where geometry may go and never
 * measures a string. This is the other half of that line - the one
 * place a run of text is measured, so the screen, a study and the
 * sheet writers cannot each measure it separately and disagree by a
 * pixel on a machine where the font is a little different.
 *
 * <p>The candidate vocabulary is the gate's
 * (docs/decisions/label-placement.md): eight positions around the mark
 * a label names, east first because that is where every label sits
 * today, so an uncrowded page keeps the placement it has; and for a
 * constellation name, its anchor and two rings of eight, because a
 * name has no mark to sit beside and a whole figure to sit in.
 */
public final class LabelGeometry {

    /** The gap between a mark and the text beside it, in pixels. */
    private static final double GAP_PX = 3.0;

    /** The rings a constellation name may step out to. */
    private static final double[] NAME_RINGS = {20.0, 40.0};

    /**
     * The eight directions, east first. A label that fits where it
     * has always sat does not move, so a released page does not
     * change for nothing.
     */
    private static final double[][] AROUND = {
            {1, 0}, {-1, 0}, {1, -1}, {-1, -1}, {1, 1}, {-1, 1},
            {0, -1}, {0, 1}};

    private LabelGeometry() {
    }

    /**
     * Every piece of ink on this page that text must not cover.
     *
     * <p>A mark's own ink, not its silhouette: an open cluster is a
     * dotted ring around nothing, and a label inside it covers
     * nothing. The renderer publishes both, and this asks for the
     * one that is ink.
     */
    public static List<LabelPlacement.Obstacle> obstaclesOn(
            ChartRenderer renderer, FontMetrics metrics, ChartScene scene,
            ChartOptions options) {
        List<LabelPlacement.Obstacle> ink = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark
                : renderer.drawnMarks(scene, options)) {
            String id = mark.star() != null ? mark.star().id()
                    : mark.deepSky().id();
            ink.add(new LabelPlacement.Obstacle(LabelPlacement.Refusal.MARK,
                    id, mark.ink()));
        }
        if (options.titleBlock()) {
            java.awt.Rectangle block =
                    ChartRenderer.titleBlockBounds(metrics, scene);
            if (block != null) {
                ink.add(new LabelPlacement.Obstacle(
                        LabelPlacement.Refusal.FURNITURE, "title block",
                        block));
            }
        }
        if (options.magnitudeKey()) {
            Rectangle2D key = renderer.magnitudeKeyBounds(metrics, scene);
            if (key != null) {
                ink.add(new LabelPlacement.Obstacle(
                        LabelPlacement.Refusal.FURNITURE, "magnitude key",
                        key));
            }
        }
        return List.copyOf(ink);
    }

    /**
     * What this page's star labels ask for.
     *
     * <p>Which stars qualify is {@link StarLabelPolicy}'s question and
     * stays there; this asks it and turns the answer into geometry.
     */
    public static List<LabelPlacement.Request> starLabels(
            ChartRenderer renderer, FontMetrics metrics, ChartScene scene,
            ChartOptions options) {
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        Projection projection = page.projection();
        StarLabelPolicy policy =
                new StarLabelPolicy(scene.viewport().fieldWidthDegrees());
        List<Star> stars = new ArrayList<>(scene.stars());
        stars.sort(Comparator.comparingDouble(Star::magnitude)
                .thenComparing(Star::id));
        List<LabelPlacement.Request> asked = new ArrayList<>();
        for (Star star : stars) {
            if (star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            boolean target = scene.targetIdentity() != null
                    && scene.targetIdentity().equals(star.id());
            String text = target
                    ? StarLabelPolicy.guaranteedLabelFor(star)
                    : policy.qualifying(star).text(options.starNames(),
                            options.bayerLetters(),
                            options.flamsteedNumbers());
            if (text == null) {
                continue;
            }
            var plane = projection.project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint at = mapping.toPixel(plane.get());
            if (at.x() < 0 || at.x() >= scene.viewport().widthPx()
                    || at.y() < 0 || at.y() >= scene.viewport().heightPx()) {
                // A star off the paper is not labelled, which is the
                // star-label pass's own rule. Asking for one and then
                // reporting it as omitted would count this study's
                // mistake as the decision's cost.
                continue;
            }
            double reach = StarSizePolicyOf(renderer, star);
            asked.add(new LabelPlacement.Request(
                    target ? LabelPlacement.Family.TARGET
                            : LabelPlacement.Family.STAR,
                    star.id(), text, at.x(), at.y(),
                    around(metrics, text, at, reach), star.id(), null,
                    target, star.magnitude()));
        }
        return List.copyOf(asked);
    }

    private static double StarSizePolicyOf(ChartRenderer renderer,
                                           Star star) {
        return renderer.starSize().radiusFor(star.magnitude());
    }

    /**
     * What this page's deep-sky labels ask for - the objects the page
     * labels today, at the boxes the renderer publishes for them.
     */
    public static List<LabelPlacement.Request> deepSkyLabels(
            ChartRenderer renderer, FontMetrics metrics, ChartScene scene,
            ChartOptions options) {
        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        Projection projection = page.projection();
        List<LabelPlacement.Request> asked = new ArrayList<>();
        // The objects the page labels, from the renderer's own rule
        // and from nothing beside it. An earlier version asked for a
        // label for every drawn symbol, which is not what the atlas
        // draws: it asked for hundreds the page never names, most of
        // them off its edge, and then reported them as omissions - a
        // migration cost that was mostly this mistake. A second
        // version answered the option itself and so lost the searched
        // object's label, which the atlas keeps when the reader has
        // switched deep-sky labels off. Both rules live in the list
        // below; neither is worth a second copy here.
        for (DeepSkyObject dso : renderer.labelledDeepSky(scene, options)) {
            var plane = projection.project(dso.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint at = mapping.toPixel(plane.get());
            Rectangle2D home = ChartRenderer.labelBounds(metrics, dso, at,
                    mapping.pixelsPerPlaneUnit());
            boolean target = scene.targetIdentity() != null
                    && scene.targetIdentity().equals(dso.id());
            asked.add(new LabelPlacement.Request(
                    target ? LabelPlacement.Family.TARGET
                            : LabelPlacement.Family.DEEP_SKY,
                    dso.id(), ChartRenderer.labelTextFor(dso), at.x(),
                    at.y(), around(metrics,
                            ChartRenderer.labelTextFor(dso), at,
                            home.getX() - at.x() - GAP_PX),
                    dso.id(), null, target, dso.labelPriority()));
        }
        return List.copyOf(asked);
    }

    /**
     * What this page's constellation names ask for, and the region
     * each may not leave.
     *
     * <p>Both come from the ink the renderer published: the anchor is
     * the renderer's own, and the region is the convex hull of the
     * pieces it drew. A name outside its own figure is naming a
     * different part of the sky, and the hull rather than a bounding
     * box because the box of Eridanus, which wanders half the sky,
     * contains most of Orion.
     */
    public static List<LabelPlacement.Request> constellationNames(
            ChartRenderer renderer, FontMetrics metrics, ChartScene scene,
            ChartOptions options) {
        if (!options.effectiveConstellationNames()
                || !new GeographyDetailPolicy(
                        scene.viewport().fieldWidthDegrees())
                        .namesDrawn()) {
            return List.of();
        }
        Map<String, ChartRenderer.FigureInk> ink =
                renderer.figureInk(scene, options);
        List<LabelPlacement.Request> asked = new ArrayList<>();
        for (Map.Entry<String, String> name
                : scene.geography().latinNames().entrySet()) {
            ChartRenderer.FigureInk figure = ink.get(name.getKey());
            if (figure == null || figure.nameAnchor() == null) {
                continue;
            }
            String text = name.getValue().toUpperCase(java.util.Locale.ROOT);
            asked.add(new LabelPlacement.Request(
                    LabelPlacement.Family.CONSTELLATION, name.getKey(),
                    text, figure.nameAnchor().x(), figure.nameAnchor().y(),
                    ringsAround(metrics, text, figure.nameAnchor()),
                    null, hullOf(figure.ink()), false, 0.0));
        }
        return List.copyOf(asked);
    }

    /** The box a run of text occupies when drawn at a given corner. */
    public static Rectangle2D boxFor(FontMetrics metrics, String text,
                                     double x, double baseline) {
        return new Rectangle2D.Double(x - 2.0,
                baseline - metrics.getAscent(),
                metrics.stringWidth(text) + 4.0, metrics.getHeight());
    }

    /** The eight boxes around a mark, east first. */
    private static List<Rectangle2D> around(FontMetrics metrics, String text,
                                            PixelPoint at, double reach) {
        double width = metrics.stringWidth(text);
        double height = metrics.getHeight();
        List<Rectangle2D> candidates = new ArrayList<>();
        for (double[] step : AROUND) {
            double x = step[0] > 0 ? at.x() + reach + GAP_PX
                    : step[0] < 0 ? at.x() - reach - GAP_PX - width
                            : at.x() - width / 2.0;
            double baseline = at.y() + step[1] * (reach + GAP_PX
                    + height / 2.0) + metrics.getAscent() / 2.0 - 1.0;
            candidates.add(boxFor(metrics, text, x, baseline));
        }
        return candidates;
    }

    /** A name's own place, then two rings of eight around it. */
    private static List<Rectangle2D> ringsAround(FontMetrics metrics,
                                                 String text,
                                                 PixelPoint at) {
        double width = metrics.stringWidth(text);
        List<Rectangle2D> candidates = new ArrayList<>();
        candidates.add(boxFor(metrics, text, at.x() - width / 2.0, at.y()));
        for (double ring : NAME_RINGS) {
            for (double[] step : AROUND) {
                candidates.add(boxFor(metrics, text,
                        at.x() - width / 2.0 + step[0] * ring,
                        at.y() + step[1] * ring));
            }
        }
        return candidates;
    }

    /**
     * The convex hull of a figure's drawn ink, by monotone chain -
     * sorted input, cross products, no tolerance and no iteration, so
     * the same ink gives the same region everywhere.
     */
    public static Shape hullOf(Shape ink) {
        List<double[]> points = new ArrayList<>();
        double[] point = new double[6];
        for (java.awt.geom.PathIterator along =
                ink.getPathIterator(null, 0.5); !along.isDone();
                along.next()) {
            int kind = along.currentSegment(point);
            if (kind == java.awt.geom.PathIterator.SEG_MOVETO
                    || kind == java.awt.geom.PathIterator.SEG_LINETO) {
                points.add(new double[] {point[0], point[1]});
            }
        }
        if (points.size() < 3) {
            // A figure drawn in one piece - Pyxis crosses the corner
            // of the 90-degree Orion page as a single line - has no
            // hull to speak of. Its own ink, given a little width, is
            // the region it owns: small, but its own.
            return points.size() < 2 ? null
                    : new java.awt.BasicStroke(8.0f)
                            .createStrokedShape(ink);
        }
        points.sort((one, other) -> one[0] != other[0]
                ? Double.compare(one[0], other[0])
                : Double.compare(one[1], other[1]));
        List<double[]> lower = chain(points);
        List<double[]> upper = chain(points.reversed());
        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        if (lower.size() < 3) {
            return new java.awt.BasicStroke(8.0f).createStrokedShape(ink);
        }
        java.awt.geom.Path2D.Double hull = new java.awt.geom.Path2D.Double();
        hull.moveTo(lower.get(0)[0], lower.get(0)[1]);
        for (int at = 1; at < lower.size(); at++) {
            hull.lineTo(lower.get(at)[0], lower.get(at)[1]);
        }
        hull.closePath();
        return hull;
    }

    private static List<double[]> chain(List<double[]> sorted) {
        List<double[]> half = new ArrayList<>();
        for (double[] point : sorted) {
            while (half.size() >= 2
                    && cross(half.get(half.size() - 2),
                            half.get(half.size() - 1), point) <= 0) {
                half.remove(half.size() - 1);
            }
            half.add(point);
        }
        return half;
    }

    private static double cross(double[] origin, double[] one,
                                double[] other) {
        return (one[0] - origin[0]) * (other[1] - origin[1])
                - (one[1] - origin[1]) * (other[0] - origin[0]);
    }
}
