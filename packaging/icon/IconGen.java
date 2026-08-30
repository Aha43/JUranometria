import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Renders the JUranometria application icon: the Tabler north-star
 *  glyph (MIT, pinned v3.46.0 - its four strokes drawn verbatim from
 *  the SVG path coordinates) in chart ink on the atlas's paper,
 *  inside a quiet rounded square with the chart frame's grey. */
public class IconGen {
    public static void main(String[] args) throws Exception {
        int[] sizes = {16, 32, 64, 128, 256, 512, 1024};
        for (int size : sizes) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            double inset = size * 0.04;
            double arc = size * 0.22;
            var square = new RoundRectangle2D.Double(inset, inset,
                    size - 2 * inset, size - 2 * inset, arc, arc);
            g.setColor(Color.WHITE);
            g.fill(square);
            g.setColor(new Color(51, 51, 51));
            g.setStroke(new BasicStroke(Math.max(1f, size / 128f)));
            g.draw(square);
            double pad = size * 0.20;
            double s = (size - 2 * pad) / 24.0;
            g.translate(pad, pad);
            g.setColor(new Color(17, 17, 17));
            g.setStroke(new BasicStroke((float) (2.0 * s),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Tabler north-star, verbatim path coordinates:
            g.draw(new Line2D.Double(3 * s, 12 * s, 21 * s, 12 * s));
            g.draw(new Line2D.Double(12 * s, 21 * s, 12 * s, 3 * s));
            g.draw(new Line2D.Double(7.5 * s, 7.5 * s, 16.5 * s, 16.5 * s));
            g.draw(new Line2D.Double(7.5 * s, 16.5 * s, 16.5 * s, 7.5 * s));
            g.dispose();
            ImageIO.write(img, "png", new File(args[0], "JUranometria-" + size + ".png"));
        }
        System.out.println("icon PNGs written");
    }
}
