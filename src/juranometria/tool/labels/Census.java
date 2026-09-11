package juranometria.tool.labels;

import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.Star;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;
import juranometria.render.RegionalDetailPolicy;
import juranometria.chart.StarSizePolicy;

/**
 * Every collision one page has, with both participants named (Sprint
 * 31, issue #310).
 *
 * <p>Two steps, and the difference between them is the point. The
 * first <em>nominates</em>: published boxes and outlines say which
 * pairs could possibly meet, cheaply and generously. The second
 * <em>attributes</em>: {@link Attribution} paints the page with each
 * of the pair withheld and reports the pixels where one is drawn over
 * the other. A nomination is allowed to be wrong. An attribution is
 * not, and nothing reaches the report that has not survived it.
 *
 * <p>The counts this produces are therefore smaller than a rectangle
 * census would give, and they are the ones a reader would agree with.
 */
public final class Census {

    /** How far apart two pieces of ink may be and still be nominated. */
    private static final double NOMINATION_SLACK_PX = 2.0;

    private final Page page;
    private final Attribution attribution;
    private final List<Participant> text = new ArrayList<>();
    private final Map<Participant, Ink> textInk = new LinkedHashMap<>();
    private final List<Participant> obstacles = new ArrayList<>();
    private final Map<Participant, java.awt.Shape> obstacleShape =
            new LinkedHashMap<>();
    private final List<Attribution.Meeting> collisions = new ArrayList<>();
    private int nominated;
    private final List<String> omissions = new ArrayList<>();

    public Census(Page page) {
        this.page = page;
        this.attribution = new Attribution(page);
        gatherText();
        gatherObstacles();
        attribute();
        // The census is complete, and the painting caches were only
        // ever scratch for taking it (#323). The study keeps every
        // page's census for the whole report, so anything left here
        // is multiplied by the corpus.
        attribution.forgetPaintings();
    }

    public Page page() {
        return page;
    }

    public List<Attribution.Meeting> collisions() {
        return List.copyOf(collisions);
    }

    /** How many pairs the cheap geometry put forward for painting. */
    public int nominated() {
        return nominated;
    }

    public List<Participant> text() {
        return List.copyOf(text);
    }

    public Ink inkOf(Participant participant) {
        return textInk.containsKey(participant) ? textInk.get(participant)
                : attribution.inkOf(participant);
    }

    public int renders() {
        return attribution.renders();
    }

    public java.awt.image.BufferedImage base() {
        return attribution.base();
    }

    /**
     * The paint order, checked rather than trusted.
     *
     * <p>The collision measure is symmetric and does not depend on who
     * is on top. Who <em>is</em> on top is a second measurement: of the
     * shared pixels, how many each participant is still holding on the
     * finished page, where "holding" means its removal changes them.
     * The one drawn later should hold at least as many as the one
     * drawn earlier.
     *
     * <p>Per pair that comparison ties constantly, and rightly: where
     * an antialiased glyph crosses an antialiased line, both inks are
     * genuinely in the pixel and removing either changes it. Summed
     * over a family pair the ties cancel and the claim becomes
     * testable. This returns the worst ratio of earlier-held to
     * later-held over the page's family pairs; above one would mean the
     * order this study reports collisions in is not the order the atlas
     * draws in.
     */
    public double worstOrderRatio() {
        Map<String, long[]> byPair = new LinkedHashMap<>();
        for (Attribution.Meeting meeting : collisions) {
            long[] sums = byPair.computeIfAbsent(
                    meeting.over().family() + ">" + meeting.under().family(),
                    key -> new long[2]);
            sums[0] += meeting.laterOnTop();
            sums[1] += meeting.earlierOnTop();
        }
        double worst = 0.0;
        for (long[] sums : byPair.values()) {
            if (sums[0] > 0) {
                worst = Math.max(worst, (double) sums[1] / sums[0]);
            } else if (sums[1] > 0) {
                worst = Math.max(worst, Double.POSITIVE_INFINITY);
            }
        }
        return worst;
    }

    /** The participants this page's text families actually drew. */
    private void gatherText() {
        FontMetrics metrics = Metrics.forFont(ChartRenderer.labelFont());
        var mapping = new ViewportMapping(page.scene().viewport());
        Projection projection =
                Projections.forViewport(page.scene().viewport());

        if (page.isCandidate()) {
            for (PlacedText placed : page.placed()) {
                add(new Participant(placed.family(), placed.id(),
                        placed.text()));
            }
        } else {
            for (ChartRenderer.StarLabelPlacement placement
                    : Page.renderer().starLabelPlacements(
                            ChartRenderer.TextMetrics.offscreen(),
                            page.scene(), page.options())) {
                add(new Participant(Participant.Family.STAR_LABEL,
                        placement.star().id(), placement.text()));
            }
            for (Map.Entry<String, String> name
                    : page.scene().geography().latinNames().entrySet()) {
                add(new Participant(Participant.Family.CONSTELLATION_NAME,
                        name.getKey(),
                        name.getValue().toUpperCase(java.util.Locale.ROOT)));
            }
        }
        // Deep-sky labels are switched as a family, so each is
        // localised by the box the renderer publishes for it. A box
        // with no ink in it is a label the page did not draw.
        for (DeepSkyObject dso : page.scene().deepSkyObjects()) {
            var plane = projection.project(dso.position());
            if (plane.isEmpty()) {
                continue;
            }
            Rectangle2D box = ChartRenderer.labelBounds(metrics, dso,
                    mapping.toPixel(plane.get()),
                    mapping.pixelsPerPlaneUnit());
            add(new Participant(Participant.Family.DEEP_SKY_LABEL, dso.id(),
                    ChartRenderer.labelTextFor(dso)).within(box));
        }
    }

    private void add(Participant participant) {
        Ink ink = attribution.inkOf(participant);
        if (ink.any()) {
            text.add(participant);
            textInk.put(participant, ink);
        } else {
            omissions.add(participant.toString());
        }
    }

    /**
     * The ink a label must not cover, and where each piece of it is.
     *
     * <p>A mark whose own object is labelled on this page carries that
     * label as the thing its withholding also takes: removing a star
     * from the scene removes its name with it, and a disc's ink has to
     * be that disc's. See {@link Participant#alsoTaking}.
     */
    private void gatherObstacles() {
        FontMetrics metrics = Metrics.forFont(ChartRenderer.labelFont());
        Map<String, Participant> labelled = new LinkedHashMap<>();
        for (Participant drawn : text) {
            labelled.put(drawn.family() + ":" + drawn.id(), drawn);
        }
        for (ChartRenderer.DrawnMark mark
                : Page.renderer().drawnMarks(page.scene(), page.options())) {
            if (mark.star() != null) {
                Star star = mark.star();
                Participant disc = new Participant(
                        Participant.Family.STAR_DISC, star.id(),
                        String.format(java.util.Locale.ROOT, "V %.2f",
                                star.magnitude()));
                Participant ownLabel = labelled.get(
                        Participant.Family.STAR_LABEL + ":" + star.id());
                if (ownLabel != null) {
                    disc = disc.alsoTaking(ownLabel);
                }
                obstacles.add(disc);
                obstacleShape.put(disc, mark.outline());
            } else if (mark.deepSky() != null) {
                Participant symbol = new Participant(
                        Participant.Family.DEEP_SKY_SYMBOL,
                        mark.deepSky().id(), mark.deepSky().type().name());
                Participant ownLabel = labelled.get(
                        Participant.Family.DEEP_SKY_LABEL + ":"
                                + mark.deepSky().id());
                if (ownLabel != null) {
                    symbol = symbol.alsoTaking(ownLabel);
                }
                obstacles.add(symbol);
                obstacleShape.put(symbol, mark.outline());
            }
        }
        for (String constellation
                : page.scene().geography().latinNames().keySet()) {
            obstacles.add(new Participant(Participant.Family.FIGURE_LINE,
                    constellation, "figure lines"));
        }
        obstacles.add(new Participant(Participant.Family.BOUNDARY_LINE,
                "all", "boundaries"));
        obstacles.add(new Participant(Participant.Family.GRID_INK, "all",
                "grid"));
        if (!page.overlays().isEmpty()) {
            obstacles.add(new Participant(Participant.Family.REFERENCE_INK,
                    "all", "reference layer"));
        }
        if (page.options().titleBlock()) {
            Rectangle2D block = Metrics.titleBlockOf(page);
            if (block != null) {
                Participant furniture = new Participant(
                        Participant.Family.TITLE_BLOCK, "title", "title block");
                obstacles.add(furniture);
                obstacleShape.put(furniture, block);
            }
        }
        if (page.options().magnitudeKey()) {
            Rectangle2D key = Page.renderer().magnitudeKeyBounds(metrics,
                    page.scene());
            if (key != null) {
                Participant furniture = new Participant(
                        Participant.Family.MAGNITUDE_KEY, "key",
                        "magnitude key");
                obstacles.add(furniture);
                obstacleShape.put(furniture, key);
            }
        }
    }

    /**
     * Every nominated pair, tested for real.
     *
     * <p>Text against text and text against everything else. Ink that
     * is not text colliding with ink that is not text is the chart
     * drawing the sky, and no placement policy is going to move a
     * boundary off a grid line.
     */
    private void attribute() {
        List<Participant> all = new ArrayList<>(text);
        all.addAll(obstacles);
        for (int i = 0; i < text.size(); i++) {
            Participant one = text.get(i);
            Ink ink = textInk.get(one);
            Rectangle2D reach = grown(ink.bounds());
            for (int j = 0; j < all.size(); j++) {
                Participant other = all.get(j);
                if (other.equals(one)) {
                    continue;
                }
                if (j < text.size() && j <= i) {
                    continue;
                }
                if (isOwnMark(one, other) || !nominated(other, reach, ink)) {
                    continue;
                }
                nominated++;
                Attribution.Meeting meeting =
                        attribution.meeting(one, other);
                if (meeting.collides()) {
                    collisions.add(meeting);
                }
            }
        }
    }

    /**
     * Whether this obstacle is the very thing the label names.
     *
     * <p>A star's name is anchored beside its own disc by decision;
     * the gate's second observed defect is a name across an
     * <em>unrelated</em> star, and counting the anchor's own mark
     * would bury that in noise. A deep-sky label sits beside its own
     * symbol on the same terms.
     */
    private static boolean isOwnMark(Participant text, Participant other) {
        return (text.family() == Participant.Family.STAR_LABEL
                        && other.family() == Participant.Family.STAR_DISC
                        || text.family() == Participant.Family.DEEP_SKY_LABEL
                                && other.family()
                                        == Participant.Family.DEEP_SKY_SYMBOL)
                && text.id().equals(other.id());
    }

    private boolean nominated(Participant other, Rectangle2D reach, Ink ink) {
        if (reach == null) {
            return false;
        }
        java.awt.Shape shape = obstacleShape.get(other);
        if (shape != null) {
            return shape.intersects(reach);
        }
        if (textInk.containsKey(other)) {
            Rectangle2D box = textInk.get(other).bounds();
            return box != null && box.intersects(reach);
        }
        // A line family: no cheap geometry, so nominate on whether any
        // of its visible ink comes near this text at all. Generous on
        // purpose - the attribution below is what decides.
        return attribution.inkOf(other).within(reach).any();
    }

    private static Rectangle2D grown(Rectangle2D box) {
        return box == null ? null : new Rectangle2D.Double(
                box.getX() - NOMINATION_SLACK_PX,
                box.getY() - NOMINATION_SLACK_PX,
                box.getWidth() + 2 * NOMINATION_SLACK_PX,
                box.getHeight() + 2 * NOMINATION_SLACK_PX);
    }

    /** Shared font metrics and furniture bounds, off one scratch page. */
    static final class Metrics {

        private static final java.awt.image.BufferedImage SCRATCH =
                new java.awt.image.BufferedImage(1, 1,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);

        private Metrics() {
        }

        static FontMetrics forFont(java.awt.Font font) {
            java.awt.Graphics2D g = SCRATCH.createGraphics();
            try {
                return g.getFontMetrics(font);
            } finally {
                g.dispose();
            }
        }

        static Rectangle2D titleBlockOf(Page page) {
            java.awt.Graphics2D g = SCRATCH.createGraphics();
            try {
                return ChartRenderer.titleBlockBounds(g, page.scene());
            } finally {
                g.dispose();
            }
        }

        static double radiusFor(double magnitude) {
            return StarSizePolicy.DEFAULT.radiusFor(magnitude);
        }

        static PixelPoint pixelOf(Page page, juranometria.chart.SkyPosition at) {
            var mapping = new ViewportMapping(page.scene().viewport());
            var plane = Projections.forViewport(page.scene().viewport())
                    .project(at);
            return plane.map(mapping::toPixel).orElse(null);
        }

        static EquatorialGrid.Grid gridOf(Page page) {
            java.awt.Graphics2D g = SCRATCH.createGraphics();
            try {
                return EquatorialGrid.gridFor(page.scene().viewport(),
                        page.options().titleBlock()
                                ? ChartRenderer.titleBlockBounds(g,
                                        page.scene()) : null,
                        page.options().magnitudeKey()
                                ? ChartRenderer.magnitudeKeyBounds(
                                        g.getFontMetrics(
                                                EquatorialGrid.GRID_LABEL_FONT),
                                        page.scene(), StarSizePolicy.DEFAULT)
                                : null);
            } finally {
                g.dispose();
            }
        }
    }
}
