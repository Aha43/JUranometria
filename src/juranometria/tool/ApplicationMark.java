package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The candidate JUranometria application marks (Sprint 23, issue
 * #200): one geometry, drawn at any size, from which every export is
 * made.
 *
 * <p>The application ships as four native images and should not
 * arrive in a task switcher wearing Java's default cup. What it wears
 * instead ought to remember where the atlas began - a small,
 * deliberately cropped piece of Andromeda cartography, drawn with the
 * chart's own discipline: white paper, dark ink, an ellipse tilted
 * the way a galaxy actually lies, and nothing else. No gradient, no
 * glow, no telescope, no swoosh.
 *
 * <p>The geometry is written in fractions of the icon's side, so
 * every size is the same drawing rather than a resampling of one
 * size, and 16 px is composed rather than shrunk. Each candidate
 * names its own stars so a study can leave one out and ask whether
 * the reader would have seen it.
 *
 * <p>Nothing here is packaging. The gate ends with a choice; #202
 * carries the chosen geometry into the platforms.
 */
public final class ApplicationMark {

    private ApplicationMark() {
    }

    /** Paper, and the chart's own ink. */
    public static final Color PAPER = Color.WHITE;
    public static final Color FRAME = new Color(51, 51, 51);
    public static final Color INK = new Color(17, 17, 17);
    /**
     * The galaxy's outline, darker than the chart's own grey 132.
     * The chart draws on paper a reader holds; an icon is 16 px on a
     * dock, and grey 132 at that size is a smudge.
     */
    public static final Color OUTLINE = new Color(34, 34, 34);
    public static final Color FILL = new Color(232, 232, 232);

    /** A star in the mark: centre and radius, both as fractions. */
    public record Dot(double x, double y, double radius) {
    }

    /**
     * The four compositions the gate compares. They differ in what
     * the mark is <em>about</em> - not in offsets of one drawing.
     */
    public enum Candidate {
        /**
         * A bold galaxy entering from the lower left and leaving the
         * frame, with three stars in the open sky above it. The
         * cropped ellipse is the whole idea: a piece of a chart, not
         * a portrait of a galaxy.
         */
        RIFT("Rift",
                "A galaxy crossing the corner and leaving the frame,"
                        + " three stars in the sky above it."),
        /**
         * The same crop with M 31's companion beside it - the
         * cartography this atlas opens on, and the pair #201 spent a
         * sprint making visible.
         */
        COMPANION("Companion",
                "The galaxy and one companion, as the default page"
                        + " draws them, with two stars."),
        /**
         * A complete small galaxy among four stars: the composition
         * the issue suspects will look like any astronomy
         * application. Included to be measured, not to be flattered.
         */
        FIELD("Field",
                "A complete galaxy centred among four stars - the"
                        + " generic arrangement, included as the"
                        + " control."),
        /**
         * The galaxy entering from the top, nearly filling the
         * width: the boldest silhouette, and the test of whether
         * boldness or cropping is what carries at 16 px.
         */
        CROWN("Crown",
                "A broad galaxy entering from the top edge, three"
                        + " stars beneath it.");

        private final String label;
        private final String prose;

        Candidate(String label, String prose) {
            this.label = label;
            this.prose = prose;
        }

        public String label() {
            return label;
        }

        public String prose() {
            return prose;
        }
    }

    /** The stars a candidate draws, in fractions of the side. */
    public static List<Dot> dots(Candidate candidate) {
        List<Dot> dots = new ArrayList<>();
        switch (candidate) {
            case RIFT -> {
                dots.add(new Dot(0.70, 0.24, 0.055));
                dots.add(new Dot(0.45, 0.30, 0.036));
                dots.add(new Dot(0.80, 0.45, 0.026));
            }
            case COMPANION -> {
                dots.add(new Dot(0.74, 0.26, 0.052));
                dots.add(new Dot(0.50, 0.22, 0.030));
            }
            case FIELD -> {
                dots.add(new Dot(0.24, 0.26, 0.050));
                dots.add(new Dot(0.76, 0.30, 0.034));
                dots.add(new Dot(0.30, 0.76, 0.030));
                dots.add(new Dot(0.78, 0.74, 0.024));
            }
            case CROWN -> {
                dots.add(new Dot(0.26, 0.74, 0.052));
                dots.add(new Dot(0.54, 0.80, 0.034));
                dots.add(new Dot(0.78, 0.68, 0.026));
            }
        }
        return List.copyOf(dots);
    }

    /** The galaxy shapes a candidate draws, largest first. */
    public static List<Shape> galaxies(Candidate candidate, double side) {
        List<Shape> shapes = new ArrayList<>();
        switch (candidate) {
            case RIFT -> shapes.add(ellipse(side, 0.30, 0.72, 1.15, 0.34, -38));
            case COMPANION -> {
                shapes.add(ellipse(side, 0.34, 0.70, 1.05, 0.32, -35));
                shapes.add(ellipse(side, 0.72, 0.52, 0.22, 0.15, -35));
            }
            case FIELD -> shapes.add(ellipse(side, 0.50, 0.50, 0.56, 0.26, -30));
            case CROWN -> shapes.add(ellipse(side, 0.52, 0.26, 1.20, 0.36, -12));
        }
        return List.copyOf(shapes);
    }

    private static Shape ellipse(double side, double cx, double cy,
                                 double major, double minor,
                                 double degrees) {
        Ellipse2D local = new Ellipse2D.Double(-major * side / 2.0,
                -minor * side / 2.0, major * side, minor * side);
        AffineTransform place = AffineTransform.getTranslateInstance(
                cx * side, cy * side);
        place.rotate(Math.toRadians(degrees));
        return place.createTransformedShape(local);
    }

    /** The paper the mark is drawn on: a quiet rounded square. */
    public static Shape card(double side) {
        double inset = side * 0.04;
        double arc = side * 0.22;
        return new RoundRectangle2D.Double(inset, inset,
                side - 2 * inset, side - 2 * inset, arc, arc);
    }

    /**
     * Draws one candidate at one size.
     *
     * @param omitDot the star to leave out, or -1 for all of them -
     *                which is how a study asks whether a reader would
     *                have seen a given dot at all
     */
    public static void paint(Graphics2D g, Candidate candidate,
                             double side, int omitDot) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        Shape card = card(side);
        g.setColor(PAPER);
        g.fill(card);

        // The galaxies are clipped to the paper, so a cropped ellipse
        // is cropped by the card's own edge rather than by a
        // rectangle that ignores its corners.
        Graphics2D inside = (Graphics2D) g.create();
        try {
            inside.clip(card);
            float stroke = (float) Math.max(1.0, side / 32.0);
            for (Shape galaxy : galaxies(candidate, side)) {
                inside.setColor(FILL);
                inside.fill(galaxy);
                inside.setColor(OUTLINE);
                inside.setStroke(new BasicStroke(stroke));
                inside.draw(galaxy);
            }
            List<Dot> dots = dots(candidate);
            inside.setColor(INK);
            for (int i = 0; i < dots.size(); i++) {
                if (i == omitDot) {
                    continue;
                }
                Dot dot = dots.get(i);
                double r = dot.radius() * side;
                inside.fill(new Ellipse2D.Double(dot.x() * side - r,
                        dot.y() * side - r, 2 * r, 2 * r));
            }
        } finally {
            inside.dispose();
        }

        g.setColor(FRAME);
        g.setStroke(new BasicStroke((float) Math.max(1.0, side / 128.0)));
        g.draw(card);
    }

    /** The ink of a candidate at a size, as an area; for measuring. */
    public static Area inkArea(Candidate candidate, double side) {
        Area area = new Area();
        for (Shape galaxy : galaxies(candidate, side)) {
            area.add(new Area(galaxy));
        }
        for (Dot dot : dots(candidate)) {
            double r = dot.radius() * side;
            area.add(new Area(new Ellipse2D.Double(dot.x() * side - r,
                    dot.y() * side - r, 2 * r, 2 * r)));
        }
        area.intersect(new Area(card(side)));
        return area;
    }
}
