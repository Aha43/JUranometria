package juranometria.tool.labels;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import juranometria.chart.DeepSkyObject;
import juranometria.render.ChartRenderer;

/**
 * The shapes a deep-sky symbol actually draws (Sprint 31, issue #310).
 *
 * <p>A symbol's published outline is its <em>silhouette</em> - what a
 * reader aims at, from #168 - and it is not what the renderer inks. An
 * open cluster is a dotted ring around nothing, a nebula an empty box,
 * a planetary a small circle with four spokes inside a square that is
 * mostly air. A placement policy that treated the silhouette as ink
 * would reserve blank paper and refuse candidates that cover nothing.
 *
 * <p>The obvious repair is to read the ink off the page, and this
 * study did that for one round. It is wrong for a different reason:
 * rasterised pixels are a fact about a machine's font and antialiasing
 * and the placement policy's contract is that the same page comes out
 * the same way everywhere. Pixels are the right instrument for
 * <em>judging</em> geometry and the wrong input for deciding placement.
 *
 * <p>So the geometry is reconstructed here, deterministically, from
 * what production publishes: which symbol an object gets ({@link
 * ChartRenderer#symbolFor}), the silhouette, its centre, its reach and
 * its position angle. That reconstruction is the one thing in this
 * study that restates production rather than asking it, and it is
 * checked against the render: the gate test requires the rendered ink
 * to lie inside these shapes and requires them to leave an open
 * cluster's middle alone.
 *
 * <p>It is also the reason `#313` is asked to publish the drawn
 * geometry the way the star-label pass has published its decisions
 * since #154. With that in hand nobody has to reconstruct anything.
 */
public final class SymbolInk {

    /**
     * The renderer's own strokes, copied value for value.
     *
     * <p>An earlier draft used 1.5 pixels "and a little for the
     * edges", which is not what the atlas draws, and drew the open
     * cluster's ring solid where the atlas dots it one pixel on and
     * three off. Both made a symbol claim ink it does not lay down,
     * and a claim like that refuses candidates and skews the
     * fallback's ranking.
     */
    private static final java.awt.Stroke OUTLINE =
            new BasicStroke(1.0f);
    private static final java.awt.Stroke DOTTED = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {1.0f, 3.0f}, 0.0f);

    private SymbolInk() {
    }

    /** The ink this mark lays down, as shapes rather than pixels. */
    public static Area of(ChartRenderer.DrawnMark mark) {
        DeepSkyObject dso = mark.deepSky();
        if (dso == null || mark.outline() == null) {
            return new Area();
        }
        Shape outline = mark.outline();
        double centreX = mark.centre().x();
        double centreY = mark.centre().y();
        double reach = mark.reach();
        double turn = -Math.toRadians(dso.positionAngleDegrees());
        return switch (ChartRenderer.symbolFor(dso)) {
            // Filled, and the only one that is: a galaxy's ellipse
            // carries the pale fill the palette calls galaxyFill.
            case ELLIPSE -> new Area(outline);
            // Drawn along their own boundary and hollow inside - the
            // open cluster dotted, the nebula's box solid.
            case DOTTED_CIRCLE -> new Area(
                    DOTTED.createStrokedShape(outline));
            case BOX -> stroked(outline);
            // A ring with two diameters across it, at the object's
            // own position angle.
            case CROSSED_CIRCLE -> {
                Area ink = stroked(outline);
                ink.add(stroked(arm(centreX, centreY, reach, turn)));
                ink.add(stroked(arm(centreX, centreY, reach,
                        turn + Math.PI / 2.0)));
                yield ink;
            }
            // A small circle with four spokes reaching past it. The
            // renderer takes a radius r, draws the circle at r/1.7 and
            // the spokes out to 1.7r; the silhouette is the square the
            // spokes reach, so reach is 1.7r and the circle's radius
            // is reach/1.7 SQUARED. An earlier draft dropped one of
            // the two and drew the circle 1.7 times too wide.
            case PLANETARY -> {
                double radius = reach / (1.7 * 1.7);
                Area ink = stroked(new Ellipse2D.Double(centreX - radius,
                        centreY - radius, 2.0 * radius, 2.0 * radius));
                ink.add(stroked(arm(centreX, centreY, reach, turn)));
                ink.add(stroked(arm(centreX, centreY, reach,
                        turn + Math.PI / 2.0)));
                yield ink;
            }
            case NONE -> new Area();
        };
    }

    private static Shape arm(double centreX, double centreY, double reach,
                             double turn) {
        double dx = reach * Math.cos(turn);
        double dy = reach * Math.sin(turn);
        return new Line2D.Double(centreX - dx, centreY - dy,
                centreX + dx, centreY + dy);
    }

    private static Area stroked(Shape shape) {
        return new Area(OUTLINE.createStrokedShape(shape));
    }

    /**
     * The band a symbol's ink lies in: its shapes stroked solid,
     * whatever the dash pattern, for judging where ink is rather than
     * how much of it there is.
     *
     * <p>The dash <em>phase</em> is the one thing this reconstruction
     * cannot match. The renderer strokes in the symbol's own rotated
     * frame and this strokes the already-placed silhouette, so the
     * dots of an open cluster's ring sit at the same size and the same
     * spacing in a different place around it. It is the sharpest
     * single reason #313 should publish the drawn geometry rather than
     * leave it to be rebuilt.
     */
    public static Area bandOf(ChartRenderer.DrawnMark mark) {
        DeepSkyObject dso = mark.deepSky();
        if (dso == null || mark.outline() == null) {
            return new Area();
        }
        if (ChartRenderer.symbolFor(dso)
                == ChartRenderer.Symbol.DOTTED_CIRCLE) {
            return stroked(mark.outline());
        }
        return of(mark);
    }
}
