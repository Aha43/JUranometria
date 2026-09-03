package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

/**
 * What the observer's lines should look like on the atlas's own
 * paper (Sprint 25, issue #225).
 *
 * <p>Drawn over real pages the assembler builds, in both themes and
 * at the fields that matter, so the vocabulary is chosen by looking
 * rather than by describing. The candidates differ only in ink -
 * the geometry beneath them is the same model the measurements use.
 *
 * <p>Study only. The chart itself learns to ink
 * {@code REFERENCE_LINE} geometry in #227; nothing here touches
 * production rendering.
 */
public final class PlaceAndTimeInkStudyMain {

    private PlaceAndTimeInkStudyMain() {
    }

    private static final File DIR = new File("docs/studies/place-and-time");
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    /** An observer, and the instant the study is drawn for. */
    private static final SkyOrientation.Observer OSLO =
            new SkyOrientation.Observer(59.913, 10.752);
    private static final Instant WHEN = ZonedDateTime
            .of(2026, 3, 20, 21, 33, 0, 0, ZoneOffset.UTC).toInstant();

    /**
     * Two candidates, so the choice is between things rather than
     * against nothing.
     *
     * <p><strong>Quiet</strong> keeps both lines in one weight and
     * tells them apart by dash alone. <strong>Stated</strong> gives
     * the horizon more presence, on the argument that it is the one
     * line that divides the sky a reader can see from the sky they
     * cannot.
     */
    private enum Candidate { QUIET, STATED }

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        SkyPosition zenith = SkyOrientation.zenith(OSLO, WHEN,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);

        for (Candidate candidate : Candidate.values()) {
            String name = candidate.name().toLowerCase(Locale.ROOT);
            write("ink-" + name + "-wide", page(zenith, 36.0), candidate,
                    false);
            write("ink-" + name + "-wide-dark", page(zenith, 36.0),
                    candidate, true);
        }
        // The chosen candidate, on the pages that decide whether it
        // survives contact with the atlas.
        Candidate chosen = Candidate.QUIET;
        write("page-zenith-8", page(zenith, 8.0), chosen, false);
        write("page-dense", page(new SkyPosition(186.6, 12.7), 36.0),
                chosen, false);
        write("page-pole", page(new SkyPosition(0.0, 88.0), 36.0),
                chosen, false);
        write("page-ra-zero", page(new SkyPosition(0.2, 20.0), 36.0),
                chosen, false);
        write("page-horizon-only", page(horizonEdge(zenith), 24.0),
                chosen, false);
        write("page-nothing", page(ChartViewState.DEFAULT.centre(), 8.0),
                chosen, false);
        // Close in on the pole, where the meridian turns at the pole
        // of date rather than at the pole the chart is drawn around.
        write("page-pole-close", page(new SkyPosition(0.0, 89.6), 4.0),
                chosen, false);

        System.out.println("ink studies written to " + DIR.getPath());
    }

    /** A page centred where the horizon crosses, and the zenith does not. */
    private static SkyPosition horizonEdge(SkyPosition zenith) {
        return SkyOrientation.horizon(OSLO, WHEN,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION, 72).get(9);
    }

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(centre, field, 8.0), WIDTH, HEIGHT);
    }

    // ---------------------------------------------------------------

    private static void write(String name, ChartScene scene,
                              Candidate candidate, boolean dark)
            throws IOException {
        BufferedImage page = RENDERER.renderToImage(scene,
                ChartOptions.DEFAULTS);
        if (dark) {
            page = inverted(page);
        }
        Graphics2D g = page.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            paintReferenceLines(g, scene, candidate, dark);
        } finally {
            g.dispose();
        }
        ImageIO.write(page, "png", new File(DIR, name + ".png"));
        System.out.printf(Locale.ROOT, "  %s%n", name);
    }

    /**
     * The observer's frame, inked as reference lines.
     *
     * <p>Everything here is a candidate for #227's decision, and
     * every colour is a grey the chart already uses: the reference
     * lines must belong to the same instrument as the grid and the
     * constellation boundaries, not arrive from another atlas.
     */
    private static void paintReferenceLines(Graphics2D g, ChartScene scene,
                                            Candidate candidate,
                                            boolean dark) {
        Color ink = dark ? new Color(150, 150, 150) : new Color(120, 120, 120);
        Color labelInk = dark ? new Color(170, 170, 170)
                : new Color(110, 110, 110);

        List<List<PixelPoint>> meridian = runsOnPaper(scene,
                SkyOrientation.meridian(OSLO, WHEN,
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION,
                        200000));
        List<List<PixelPoint>> horizon = runsOnPaper(scene,
                SkyOrientation.horizon(OSLO, WHEN,
                        SkyOrientation.Fidelity.PRECESSION_AND_NUTATION,
                        200000));

        g.setColor(ink);
        g.setStroke(new BasicStroke(1.0f));
        draw(g, meridian);
        label(g, meridian, "meridian", labelInk);

        g.setStroke(candidate == Candidate.QUIET
                ? new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10.0f,
                        new float[] {6.0f, 4.0f}, 0.0f)
                : new BasicStroke(1.6f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10.0f,
                        new float[] {8.0f, 3.0f}, 0.0f));
        draw(g, horizon);
        label(g, horizon, "horizon", labelInk);

        // The zenith: an open ring with a short upward tick, so it
        // reads as a place rather than as an object, and cannot be
        // mistaken for a working cross.
        PixelPoint zenith = pixel(scene, SkyOrientation.zenith(OSLO, WHEN,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION));
        if (zenith != null && ChartRenderer.paperOf(scene)
                .contains(zenith.x(), zenith.y())) {
            g.setStroke(new BasicStroke(1.0f));
            g.setColor(ink);
            g.draw(new Ellipse2D.Double(zenith.x() - 5, zenith.y() - 5, 10, 10));
            g.draw(new Line2D.Double(zenith.x(), zenith.y() - 9,
                    zenith.x(), zenith.y() - 6));
            g.setColor(labelInk);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.drawString("zenith", (float) zenith.x() + 9,
                    (float) zenith.y() - 6);
        }
    }

    private static void draw(Graphics2D g, List<List<PixelPoint>> runs) {
        for (List<PixelPoint> run : runs) {
            if (run.size() < 2) {
                continue;
            }
            Path2D.Double path = new Path2D.Double();
            path.moveTo(run.get(0).x(), run.get(0).y());
            for (int i = 1; i < run.size(); i++) {
                path.lineTo(run.get(i).x(), run.get(i).y());
            }
            g.draw(path);
        }
    }

    /** A name where the line leaves the paper, if it leaves it. */
    private static void label(Graphics2D g, List<List<PixelPoint>> runs,
                              String what, Color ink) {
        if (runs.isEmpty() || runs.get(0).isEmpty()) {
            return;
        }
        List<PixelPoint> longest = runs.get(0);
        for (List<PixelPoint> run : runs) {
            if (run.size() > longest.size()) {
                longest = run;
            }
        }
        PixelPoint at = longest.get(longest.size() - 1);
        Color was = g.getColor();
        g.setColor(ink);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString(what, (float) Math.min(at.x() + 6, WIDTH - 60),
                (float) Math.max(14, Math.min(at.y() - 6, HEIGHT - 8)));
        g.setColor(was);
    }

    /**
     * The runs of the curve that are on the paper, kept in the order
     * they were sampled.
     *
     * <p>Grouped by <em>parameter</em>, not by how far apart two
     * projected points landed. The first version of this broke a
     * segment whenever consecutive points were more than forty
     * pixels apart, which at a four-degree field is every segment -
     * so the meridian vanished from the page that mattered most and
     * left only its label behind. #227 inherits the lesson: a great
     * circle is a straight line on this projection, and the place to
     * decide what is on the paper is the geometry, not a threshold
     * in pixels.
     */
    private static List<List<PixelPoint>> runsOnPaper(ChartScene scene,
                                                      List<SkyPosition> curve) {
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        List<List<PixelPoint>> runs = new ArrayList<>();
        List<PixelPoint> current = new ArrayList<>();
        for (SkyPosition point : curve) {
            PixelPoint at = pixel(scene, point);
            if (at != null && paper.contains(at.x(), at.y())) {
                current.add(at);
            } else if (!current.isEmpty()) {
                runs.add(List.copyOf(current));
                current.clear();
            }
        }
        if (!current.isEmpty()) {
            runs.add(List.copyOf(current));
        }
        return runs;
    }

    private static PixelPoint pixel(ChartScene scene, SkyPosition position) {
        return new GnomonicProjection(scene.viewport().centre())
                .project(position)
                .map(new ViewportMapping(scene.viewport())::toPixel)
                .orElse(null);
    }

    /** A quick stand-in for the dark theme's paper. */
    private static BufferedImage inverted(BufferedImage page) {
        BufferedImage dark = new BufferedImage(page.getWidth(),
                page.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                int rgb = page.getRGB(x, y);
                dark.setRGB(x, y, ~rgb & 0xffffff);
            }
        }
        return dark;
    }
}
