package juranometria.tool.allsky;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import juranometria.chart.SkyPosition;
import juranometria.render.ChartPalette;
import juranometria.sky.Ecliptic;
import juranometria.sky.GreatCircle;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

/**
 * What production cannot yet draw on a seamed map, drawn by the study
 * and captioned as study ink on every page that carries it (#686).
 *
 * <p>The outline, and the three great circles whose production route
 * is the conic-only curve seam (#298): the ecliptic, the celestial
 * equator and the mathematical horizon. The strokes copy
 * {@code ReferenceInk}'s exactly - the ecliptic's long-dash-dot, the
 * horizon's even dash - in the palette's figure ink, so a page reads as
 * production would if the seam were extended; the equator, which on an
 * ordinary page is the grid's zero parallel, is drawn solid in grid
 * ink. Curves are sampled every quarter degree along the circle and
 * broken wherever consecutive samples fall on opposite sides of the
 * seam; that break is the seam rule the study proposes and measures.
 */
public final class AllSkyStudyInk {

    static final BasicStroke SOLID = new BasicStroke(1.0f);
    static final BasicStroke DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] {6.0f, 4.0f}, 0.0f);
    static final BasicStroke DASH_DOT = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[] {12.0f, 4.0f, 2.0f, 4.0f}, 0.0f);

    /** Samples per full circle: every quarter degree. */
    static final int SAMPLES = 1440;

    private AllSkyStudyInk() {
    }

    /** Plane to pixel for a page whose outline exactly fills a w x h paper, east left. */
    public record Frame(AllSkyMap map, int widthPx, int heightPx) {
        double scale() {
            return widthPx / (2.0 * map.form().halfWidth());
        }

        public double[] pixel(double x, double y) {
            return new double[] {widthPx / 2.0 - x * scale(), heightPx / 2.0 - y * scale()};
        }

        public double[] pixel(SkyPosition p) {
            double[] xy = map.project(p);
            return pixel(xy[0], xy[1]);
        }

        /** The sky under a pixel, or null off the outline. */
        public SkyPosition sky(double px, double py) {
            return map.unproject((widthPx / 2.0 - px) / scale(), (heightPx / 2.0 - py) / scale());
        }
    }

    /** A great circle as seam-broken runs of pixel points. */
    /** The circle walked once and closed: the first point repeated at the end. */
    static List<SkyPosition> closed(GreatCircle circle) {
        List<SkyPosition> walk = new ArrayList<>(circle.around(SAMPLES));
        walk.add(walk.get(0));
        return walk;
    }

    public static List<List<double[]>> runs(Frame frame, GreatCircle circle) {
        List<List<double[]>> out = new ArrayList<>();
        List<double[]> run = new ArrayList<>();
        SkyPosition previous = null;
        for (SkyPosition p : closed(circle)) {
            if (previous != null && frame.map().crossesSeam(previous, p)) {
                if (run.size() > 1) {
                    out.add(run);
                }
                run = new ArrayList<>();
            }
            run.add(frame.pixel(p));
            previous = p;
        }
        if (run.size() > 1) {
            out.add(run);
        }
        // A closed circle that never met the seam began and ended at one point.
        return out;
    }

    /** How many times a great circle crosses the seam. */
    public static int seamCrossings(AllSkyMap map, GreatCircle circle) {
        int n = 0;
        SkyPosition previous = null;
        for (SkyPosition p : closed(circle)) {
            if (previous != null && map.crossesSeam(previous, p)) {
                n++;
            }
            previous = p;
        }
        return n;
    }

    public static Path2D outline(Frame frame) {
        Path2D path = new Path2D.Double();
        AllSkyMap m = frame.map();
        AllSkyForm f = m.form();
        int n = 720;
        for (int i = 0; i <= n; i++) {
            double phi = Math.PI * (i / (double) n - 0.5);
            double[] xy = frame.pixel(f.forward(Math.PI, phi)[0], f.forward(Math.PI, phi)[1]);
            if (i == 0) {
                path.moveTo(xy[0], xy[1]);
            } else {
                path.lineTo(xy[0], xy[1]);
            }
        }
        for (int i = n; i >= 0; i--) {
            double phi = Math.PI * (i / (double) n - 0.5);
            double[] xy = frame.pixel(f.forward(-Math.PI, phi)[0], f.forward(-Math.PI, phi)[1]);
            path.lineTo(xy[0], xy[1]);
        }
        path.closePath();
        return path;
    }

    private static void stroke(Graphics2D g, List<List<double[]>> runs) {
        for (List<double[]> run : runs) {
            Path2D path = new Path2D.Double();
            path.moveTo(run.get(0)[0], run.get(0)[1]);
            for (int i = 1; i < run.size(); i++) {
                path.lineTo(run.get(i)[0], run.get(i)[1]);
            }
            g.draw(path);
        }
    }

    /** Paints the study's ink over a production page. */
    public static void paint(Graphics2D g, Frame frame, ChartPalette palette,
                             Observer observer, boolean horizon) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(palette.frameInk());
        g.setStroke(SOLID);
        g.draw(outline(frame));
        g.setColor(palette.gridLabelInk());
        g.setStroke(SOLID);
        stroke(g, runs(frame, new GreatCircle(new SkyPosition(0.0, 90.0))));
        g.setColor(palette.figureInk());
        g.setStroke(DASH_DOT);
        stroke(g, runs(frame, Ecliptic.circle()));
        if (horizon && observer != null) {
            g.setStroke(DASHED);
            stroke(g, runs(frame, new LocalSky(observer).horizon()));
        }
    }

    /** A thin caption strip naming the study ink, painted in a corner of the page. */
    public static void caption(Graphics2D g, int widthPx, ChartPalette palette, String text) {
        g.setFont(g.getFont().deriveFont(11f));
        int w = g.getFontMetrics().stringWidth(text) + 12;
        g.setColor(palette.ground());
        g.fillRect(widthPx - w - 6, 6, w, 18);
        g.setColor(new Color(palette.textInk().getRGB()));
        g.drawRect(widthPx - w - 6, 6, w, 18);
        g.drawString(text, widthPx - w, 19);
    }
}
