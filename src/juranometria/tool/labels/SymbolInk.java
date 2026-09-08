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

    /** The renderer's outline stroke, and a little for its edges. */
    private static final float STROKE_PX = 1.5f;

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
            // Drawn along their own boundary and hollow inside.
            case DOTTED_CIRCLE, BOX -> stroked(outline);
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
            // silhouette is the square the spokes reach to, so the
            // circle's radius is the spoke's own 1/1.7 of it.
            case PLANETARY -> {
                double radius = reach / 1.7;
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
        return new Area(new BasicStroke(STROKE_PX).createStrokedShape(shape));
    }
}
