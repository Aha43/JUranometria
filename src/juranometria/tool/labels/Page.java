package juranometria.tool.labels;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import juranometria.chart.ChartScene;
import juranometria.chart.StarSizePolicy;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.ui.ReferenceInk;

/**
 * One page as the application paints it, or as a candidate policy
 * would (Sprint 31, issue #310).
 *
 * <p>The production renderer, the production assembler's scene, the
 * reader's own options and the modules' own contributions, through
 * the same reference layer the chart component hands the renderer.
 * Nothing here invents a mark, a line or a curve: this study measures
 * the atlas, and a study that painted its own chart would be
 * measuring itself.
 *
 * <p>A <em>candidate</em> page is the same production page with the
 * text families the candidate is responsible for switched off through
 * the reader's own options, and that text written back where the
 * candidate would put it, in the renderer's own fonts and inks. The
 * chart underneath is production's to the pixel, so a candidate is
 * answerable to the same oracle as the released page rather than to
 * its own arithmetic.
 */
public record Page(String slug, ChartScene scene, ChartOptions options,
                   List<OverlayRegistry.Owned> overlays,
                   List<PlacedText> placed, List<String> selected,
                   List<juranometria.render.LabelPlacement.Placement> text) {

    public Page(String slug, ChartScene scene, ChartOptions options,
                List<OverlayRegistry.Owned> overlays,
                List<PlacedText> placed, List<String> selected) {
        this(slug, scene, options, overlays, placed, selected, List.of());
    }

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    public Page(String slug, ChartScene scene, ChartOptions options,
                List<OverlayRegistry.Owned> overlays) {
        this(slug, scene, options, overlays, List.of(), List.of());
    }

    public int wide() {
        return scene.viewport().widthPx();
    }

    public int high() {
        return scene.viewport().heightPx();
    }

    /** Whether a candidate policy is drawing this page's text. */
    public boolean isCandidate() {
        return !placed.isEmpty();
    }

    public Page withScene(ChartScene other) {
        return new Page(slug, other, options, overlays, placed, selected,
                text);
    }

    public Page withOptions(ChartOptions other) {
        return new Page(slug, scene, other, overlays, placed, selected,
                text);
    }

    public Page withoutOverlays() {
        return new Page(slug, scene, options, List.of(), placed, selected,
                text);
    }

    public Page withPlaced(List<PlacedText> written) {
        return new Page(slug, scene, options, overlays,
                List.copyOf(written), selected, text);
    }

    /**
     * This page with its text placed once and held there.
     *
     * <p>Since #314 placement is a decision about the whole page, so
     * taking one star away moves every label after it. A study
     * measuring what that star inked must hold the rest of the page
     * still, or it measures the placement rather than the ink.
     */
    public Page withTextHeld() {
        if (!text.isEmpty()) {
            return this;
        }
        return new Page(slug, scene, options, overlays, placed, selected,
                RENDERER.textPlacements(
                        juranometria.render.ChartRenderer.TextMetrics
                                .offscreen(), scene, options));
    }

    /** This page with one held piece of production text left out. */
    public Page withoutText(
            java.util.function.Predicate<
                    juranometria.render.LabelPlacement.Request> which) {
        List<juranometria.render.LabelPlacement.Placement> kept =
                new ArrayList<>();
        for (var placement : text) {
            if (!which.test(placement.request())) {
                kept.add(placement);
            }
        }
        return new Page(slug, scene, options, overlays, placed, selected,
                List.copyOf(kept));
    }

    /** The reader's working selection, as the chart component rings it. */
    public Page withSelection(List<String> members) {
        return new Page(slug, scene, options, overlays, placed,
                List.copyOf(members), text);
    }

    /** This page with one selected member's ring left off. */
    public Page withoutSelected(String member) {
        List<String> kept = new ArrayList<>(selected);
        kept.remove(member);
        return new Page(slug, scene, options, overlays, placed,
                List.copyOf(kept), text);
    }

    /** This page with one candidate-placed piece of text left out. */
    public Page withoutPlaced(Participant.Family family, String id) {
        List<PlacedText> kept = new ArrayList<>();
        for (PlacedText written : placed) {
            if (written.family() != family || !written.id().equals(id)) {
                kept.add(written);
            }
        }
        return new Page(slug, scene, options, overlays, List.copyOf(kept),
                selected, text);
    }

    /** The page, painted. */
    public BufferedImage paint() {
        return paint(null);
    }

    /**
     * The page, or one window of it, painted.
     *
     * <p>A window is the same page seen through a hole: the renderer
     * is handed a translated context onto a small image and draws
     * exactly the pixels it would have drawn there on the whole page.
     * It is how this study affords to repaint for every pair of
     * participants rather than for every participant.
     */
    public BufferedImage paint(java.awt.Rectangle window) {
        BufferedImage image = new BufferedImage(
                window == null ? wide() : window.width,
                window == null ? high() : window.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        if (window != null) {
            g.translate(-window.x, -window.y);
        }
        try {
            RENDERER.render(g, scene, options, (layerG, layerScene) ->
                    ReferenceInk.paint(layerG, layerScene, overlays,
                            options.palette()),
                    text.isEmpty() ? null : text);
            // One ring per selected member, after the chart and before
            // the study's own text - which is where the chart
            // component draws it (issue #261).
            for (String member : selected) {
                RENDERER.drawSelectionHighlight(g, scene, options, member);
            }
            if (!placed.isEmpty()) {
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(
                        java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                // The chart's own clip, so a candidate cannot write
                // where the released page would not have let it.
                g.setClip(1, 1, wide() - 2, high() - 2);
                for (PlacedText text : placed) {
                    g.setFont(text.font());
                    g.setColor(text.ink());
                    g.drawString(text.text(), (float) text.x(),
                            (float) text.baseline());
                }
                g.setClip(null);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /** The renderer this study reads decisions from and paints with. */
    public static ChartRenderer renderer() {
        return RENDERER;
    }
}
