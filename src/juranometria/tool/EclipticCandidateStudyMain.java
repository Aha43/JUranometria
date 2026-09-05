package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.UIManager;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.GnomonicProjection;
import juranometria.project.GreatCirclePage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;
import juranometria.sky.SkyFrame;
import juranometria.ui.ReferenceInk;

/**
 * Choosing the ecliptic's ink, beside the meridian's (Sprint 28,
 * issue #271).
 *
 * <p>The first draft of this gate proved the existing
 * {@code Reference.LINE} treatment is wrong for the ecliptic - it is
 * the meridian's own stroke - and then left the replacement to
 * #273. A review rightly refused that: the gate is where the line
 * vocabulary is decided, and #273 should implement a reviewed
 * contract rather than reopen the question while changing production
 * (PR #276 review).
 *
 * <p>So this study draws the candidates <strong>on the page where
 * they would be confused</strong>: a dense field carrying both the
 * meridian and the ecliptic at once, in both chart grounds.
 *
 * <h2>What is production here and what is not</h2>
 *
 * <p>The chart, the catalogue ink, the projection, the analytic
 * clipping and the <em>meridian</em> are all production: the meridian
 * is contributed as the great circle through both celestial poles -
 * exactly what {@code MeridianModule} offers - and inked by the real
 * {@code ReferenceInk}. Only the ecliptic's own stroke and its
 * landmark symbols are drawn by this study, because the treatments
 * being compared do not exist yet. They use production's own
 * clipping, palette and label placement, so nothing but the stroke
 * and the symbol differs.
 *
 * <p><strong>And they are drawn in production's own reference
 * layer.</strong> An earlier draft painted the candidates
 * <em>after</em> a completed chart, which put them above every star,
 * symbol and label - so the pages answered the opposite of the
 * question they were used to settle: whether the ecliptic hides an
 * object, and whether a diamond survives beside deep-sky ink. In
 * production a mark covers the reference line, not the other way
 * round (PR #276 round 2). The study now drives
 * {@link ChartRenderer#render} directly and fills its
 * {@code ReferenceLayer}, the same boundary {@code ChartComponent}
 * hands to {@code ReferenceInk}: above the grid and the geography,
 * below every catalogued mark and label.
 */
public final class EclipticCandidateStudyMain {

    private EclipticCandidateStudyMain() {
    }

    private static final File DIR = new File("docs/studies/ecliptic");

    private static final double EPS0 = SkyFrame.meanObliquityDegrees(0.0);
    private static final SkyPosition ECLIPTIC_POLE =
            new SkyPosition(270.0, 90.0 - EPS0);

    /**
     * A meridian: the great circle through both celestial poles. Its
     * own pole therefore lies on the equator. This one runs through
     * RA 270, so it crosses the ecliptic near the December solstice -
     * the crossing that makes the two lines confusable.
     */
    private static final SkyPosition MERIDIAN_POLE =
            new SkyPosition(0.0, 0.0);

    /** The page where the two lines meet in crowded sky. */
    private static final SkyPosition DECISIVE =
            new SkyPosition(270.0, -EPS0);

    /** A candidate stroke for the ecliptic. */
    private record Candidate(String key, String note, BasicStroke stroke) {
    }

    private static final List<Candidate> LINES = List.of(
            new Candidate("solid",
                    "the existing LINE - the meridian's own stroke",
                    new BasicStroke(1.0f)),
            new Candidate("dashed",
                    "the existing BOUNDARY - 6 on, 4 off",
                    dash(1.0f, 6.0f, 4.0f)),
            new Candidate("longdash",
                    "long dash - 12 on, 6 off",
                    dash(1.0f, 12.0f, 6.0f)),
            new Candidate("dashdot",
                    "dash-dot - 12, 4, 2, 4: the cartographic datum line",
                    dash(1.0f, 12.0f, 4.0f, 2.0f, 4.0f)),
            new Candidate("dotted",
                    "fine dotted - 2 on, 4 off",
                    dash(1.0f, 2.0f, 4.0f)));

    /** A candidate symbol for a landmark on the line. */
    private enum Landmark {

        /** The existing reference point: the zenith's ring and tick. */
        RING_TICK,

        /** A ring with no tick. */
        RING,

        /** An open diamond. */
        DIAMOND
    }

    private static BasicStroke dash(float width, float... pattern) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, pattern, 0.0f);
    }

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        System.out.println("ecliptic ink candidates:");

        // Every line candidate, on the page where the ecliptic and
        // the meridian cross, in both grounds.
        for (Candidate candidate : LINES) {
            for (boolean black : new boolean[] {false, true}) {
                render("line-" + candidate.key() + (black ? "-black" : ""),
                        DECISIVE, 18.0, black, candidate,
                        Landmark.RING_TICK, false);
            }
        }

        // Every landmark candidate, at the December solstice where a
        // mark sits on the line, beside the meridian.
        for (Landmark landmark : Landmark.values()) {
            render("mark-" + landmark.name().toLowerCase(Locale.ROOT),
                    DECISIVE, 18.0, false, chosenLine(), landmark, false);
        }

        // The chosen treatment, at the fields that stress it.
        render("chosen-narrow", DECISIVE, 8.0, false, chosenLine(),
                chosenLandmark(), false);
        render("chosen-wide", DECISIVE, 36.0, false, chosenLine(),
                chosenLandmark(), false);
        render("chosen-black", DECISIVE, 18.0, true, chosenLine(),
                chosenLandmark(), false);

        // Enlarged application text, to show it does not reach the
        // page: the chart's faces are the renderer's own.
        File ordinary = render("chosen-ordinary", DECISIVE, 18.0, false,
                chosenLine(), chosenLandmark(), false);
        File enlarged = render("chosen-enlarged", DECISIVE, 18.0, false,
                chosenLine(), chosenLandmark(), true);
        System.out.println("  enlarged application text reaches the page: "
                + (digest(ordinary).equals(digest(enlarged))
                        ? "no - the two pages are byte-identical"
                        : "YES - the pages differ"));

        System.out.println("written to " + DIR.getPath());
    }

    /**
     * The four cardinal landmarks, named by month rather than by
     * season: the geometry has no observer, and a southern reader's
     * summer is a northern reader's winter (PR #276 review).
     */
    public static final List<Mark> LANDMARKS = List.of(
            new Mark("March equinox", new SkyPosition(0.0, 0.0)),
            new Mark("June solstice", new SkyPosition(90.0, EPS0)),
            new Mark("September equinox", new SkyPosition(180.0, 0.0)),
            new Mark("December solstice", new SkyPosition(270.0, -EPS0)));

    /** A named landmark on the circle. */
    public record Mark(String name, SkyPosition at) {
    }

    /**
     * The ecliptic in the treatment this gate chose, over a painted
     * chart: dash-dot line, open-diamond landmarks.
     *
     * <p>Shared with {@link EclipticInkStudyMain}, so the pages that
     * show the decision and the pages that argued it cannot drift
     * apart. Production's clipping, projection, palette and label
     * placement; only the stroke and the symbol are the study's,
     * because the treatments do not exist in the chart yet.
     */
    public static void drawChosen(Graphics2D g, ChartScene scene,
                                  ChartPalette palette) {
        draw(g, scene, palette, chosenLine(), chosenLandmark(), LANDMARKS);
    }

    /** The stroke the gate chose; see docs/decisions/ecliptic.md. */
    private static Candidate chosenLine() {
        return LINES.stream().filter(c -> c.key().equals("dashdot"))
                .findFirst().orElseThrow();
    }

    /** The landmark symbol the gate chose. */
    private static Landmark chosenLandmark() {
        return Landmark.DIAMOND;
    }

    private static File render(String name, SkyPosition centre,
                               double field, boolean black,
                               Candidate line, Landmark landmark,
                               boolean enlargedText) throws Exception {
        if (enlargedText) {
            Font base = UIManager.getFont("defaultFont") != null
                    ? UIManager.getFont("defaultFont")
                    : new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            UIManager.put("defaultFont", base.deriveFont(18.0f));
        }
        ChartScene scene = Atlas.assembler().assemble(
                new ChartViewState(centre, field, 8.0), 900, 700);
        ChartOptions options = black
                ? ChartOptions.DEFAULTS.withPalette(ChartPalette.BLACK_SKY)
                : ChartOptions.DEFAULTS;
        ChartPalette palette = options.palette();

        // The meridian: production geometry, offered exactly as
        // MeridianModule offers it, and inked by the real
        // ReferenceInk inside the renderer's own reference layer.
        List<OverlayRegistry.Owned> meridian = List.of(
                new OverlayRegistry.Owned("meridian",
                        new OverlayContribution.GreatCircle("meridian",
                                "Meridian", MERIDIAN_POLE,
                                OverlayContribution.Reference.LINE,
                                InkRole.REFERENCE_LINE)));

        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT).render(g, scene,
                    options, (layerG, layerScene) -> {
                        ReferenceInk.paint(layerG, layerScene, meridian,
                                palette);
                        drawCandidate(layerG, layerScene, palette, line,
                                landmark);
                    });
        } finally {
            g.dispose();
        }
        File file = new File(DIR, "candidate-" + name + ".png");
        ImageIO.write(image, "png", file);
        if (enlargedText) {
            UIManager.put("defaultFont", null);
        }
        System.out.println("  candidate-" + name + " ("
                + (black ? "black sky" : "white paper") + ", "
                + String.format(Locale.ROOT, "%.0f°", field) + ") "
                + line.note());
        return file;
    }

    /**
     * The ecliptic in a candidate treatment, over the painted chart.
     *
     * <p>Production's clipping, palette and label placement; only the
     * stroke and the landmark symbol are this study's.
     */
    private static void drawCandidate(Graphics2D g, ChartScene scene,
                                      ChartPalette palette, Candidate line,
                                      Landmark landmark) {
        // The candidate pages show one landmark - the one that sits
        // on this page, where the two lines cross.
        draw(g, scene, palette, line, landmark, List.of(
                new Mark("December solstice",
                        new SkyPosition(270.0, -EPS0))));
    }

    private static void draw(Graphics2D g, ChartScene scene,
                             ChartPalette palette, Candidate line,
                             Landmark landmark, List<Mark> landmarks) {
        ChartViewport viewport = scene.viewport();
        GnomonicProjection projection =
                new GnomonicProjection(viewport.centre());
        ViewportMapping mapping = new ViewportMapping(viewport);
        Rectangle2D paper = new Rectangle2D.Double(1, 1,
                viewport.widthPx() - 2, viewport.heightPx() - 2);
        GreatCirclePage.Page page = new GreatCirclePage.Page(
                paper.getMinX(), paper.getMinY(),
                paper.getMaxX(), paper.getMaxY());

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(paper);
            Optional<GreatCirclePage.Arc> arc = GreatCirclePage.clip(
                    projection, mapping, page, ECLIPTIC_POLE);
            if (arc.isPresent()) {
                g2.setColor(palette.figureInk());
                g2.setStroke(line.stroke());
                g2.draw(new Line2D.Double(arc.get().from().x(),
                        arc.get().from().y(), arc.get().to().x(),
                        arc.get().to().y()));
                g2.setColor(palette.gridLabelInk());
                g2.setFont(EquatorialGrid.GRID_LABEL_FONT);
                Rectangle2D box = ReferenceInk.labelBox(paper, arc.get(),
                        "Ecliptic", g2.getFontMetrics());
                g2.drawString("Ecliptic", (float) box.getMinX(),
                        (float) (box.getMaxY()
                                - g2.getFontMetrics().getDescent()));
            }
            for (Mark each : landmarks) {
                mark(g2, projection, mapping, paper, palette, landmark,
                        each.at(), each.name());
            }
        } finally {
            g2.dispose();
        }
    }

    private static void mark(Graphics2D g, GnomonicProjection projection,
                             ViewportMapping mapping, Rectangle2D paper,
                             ChartPalette palette, Landmark landmark,
                             SkyPosition at, String name) {
        PixelPoint p = projection.project(at).map(mapping::toPixel)
                .orElse(null);
        if (p == null || !paper.contains(p.x(), p.y())) {
            return;
        }
        double r = 5.0;
        g.setColor(palette.figureInk());
        g.setStroke(new BasicStroke(1.0f));
        switch (landmark) {
            case RING_TICK -> {
                g.draw(new Ellipse2D.Double(p.x() - r, p.y() - r,
                        2 * r, 2 * r));
                g.draw(new Line2D.Double(p.x(), p.y() - r,
                        p.x(), p.y() - r - 4.0));
            }
            case RING -> g.draw(new Ellipse2D.Double(p.x() - r, p.y() - r,
                    2 * r, 2 * r));
            case DIAMOND -> {
                Path2D diamond = new Path2D.Double();
                double d = r + 1.0;
                diamond.moveTo(p.x(), p.y() - d);
                diamond.lineTo(p.x() + d, p.y());
                diamond.lineTo(p.x(), p.y() + d);
                diamond.lineTo(p.x() - d, p.y());
                diamond.closePath();
                g.draw(diamond);
            }
            default -> throw new IllegalStateException(landmark.name());
        }
        g.setColor(palette.gridLabelInk());
        g.setFont(EquatorialGrid.GRID_LABEL_FONT);
        Rectangle2D box = ReferenceInk.labelBox(paper,
                new GreatCirclePage.Arc(p, p), name, g.getFontMetrics());
        g.drawString(name, (float) box.getMinX(),
                (float) (box.getMaxY() - g.getFontMetrics().getDescent()));
    }

    private static String digest(File file) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(java.nio.file.Files.readAllBytes(
                file.toPath()));
        StringBuilder text = new StringBuilder();
        for (byte b : hash) {
            text.append(String.format(Locale.ROOT, "%02x", b));
        }
        return text.toString();
    }
}
