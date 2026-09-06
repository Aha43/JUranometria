package juranometria.sheet;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * A recorded sheet as PNG (Sprint 29, issue #286).
 *
 * <p>The sharing format: a picture of the sheet, for a message or a
 * club page, where a vector file would be a nuisance and a PDF would
 * be a download.
 *
 * <p>Two things make the difference between a useful one and a
 * misleading one, and both were learned the hard way in the gate.
 *
 * <p>First, it is the <strong>whole sheet</strong> - margins
 * included - drawn at the stated resolution, not the chart re-rendered
 * into a bigger pixel grid. The gate's first version did the latter:
 * the canvas was the right size, the metadata was right, and every
 * label had shrunk fourfold against the paper while looking entirely
 * plausible on screen. So the sheet's own recorded ink is
 * <em>replayed</em> through one scale, and a point stays a point.
 *
 * <p>Second, it <strong>says what it is</strong>. Without a
 * {@code pHYs} chunk a PNG is pixels and nothing else, and a printer
 * fits it to whatever page it has. With one, 3508 x 2480 px is an A4
 * sheet at 300 dpi and prints as one.
 */
public final class PngSheetWriter {

    private PngSheetWriter() {
    }

    /** The resolutions the reader is offered, in dots per inch. */
    public static final int[] RESOLUTIONS = {150, 300, 600};

    /** The default: good on paper and on a screen, without being huge. */
    public static final int DEFAULT_RESOLUTION = 300;

    /** How many pixels wide this sheet is at this resolution. */
    public static int widePixels(PaperSize paper, int dpi) {
        return (int) Math.round(paper.widePoints() * dpi / 72.0);
    }

    public static int highPixels(PaperSize paper, int dpi) {
        return (int) Math.round(paper.highPoints() * dpi / 72.0);
    }

    /** The sheet as PNG bytes at the stated resolution. */
    public static byte[] write(SheetRecording sheet, int dpi)
            throws IOException {
        if (sheet == null) {
            throw new IllegalArgumentException("a sheet is required");
        }
        if (dpi <= 0) {
            throw new IllegalArgumentException(
                    "a resolution is a positive number of dots per"
                            + " inch: " + dpi);
        }
        PaperSize paper = sheet.paper();
        double scale = dpi / 72.0;
        int wide = widePixels(paper, dpi);
        int high = highPixels(paper, dpi);

        BufferedImage image = new BufferedImage(wide, high,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            g.setColor(sheet.options().palette().ground());
            g.fillRect(0, 0, wide, high);
            // One scale for the whole sheet, then the margin: the ink
            // is point-sized geometry passing through it, so a
            // one-point stroke lands as dpi/72 pixels.
            g.scale(scale, scale);
            g.translate(paper.marginPoints(), paper.marginPoints());
            SheetReplay.into(g, sheet.recorder());
        } finally {
            g.dispose();
        }
        return encode(image, dpi);
    }

    /** PNG bytes carrying a pHYs chunk that states the resolution. */
    private static byte[] encode(BufferedImage image, int dpi)
            throws IOException {
        ImageWriter writer =
                ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        IIOMetadata metadata = writer.getDefaultImageMetadata(
                new ImageTypeSpecifier(image), params);

        String format = "javax_imageio_png_1.0";
        IIOMetadataNode physical = new IIOMetadataNode("pHYs");
        long perMetre = Math.round(dpi / 0.0254);
        physical.setAttribute("pixelsPerUnitXAxis", Long.toString(perMetre));
        physical.setAttribute("pixelsPerUnitYAxis", Long.toString(perMetre));
        physical.setAttribute("unitSpecifier", "meter");
        IIOMetadataNode root = new IIOMetadataNode(format);
        root.appendChild(physical);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            metadata.mergeTree(format, root);
            try (ImageOutputStream out =
                         ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(image, null, metadata),
                        params);
            }
        } catch (IIOInvalidTreeException failure) {
            throw new IOException("the PNG could not state its"
                    + " resolution, and a PNG that cannot say how big"
                    + " it is prints at whatever size a printer"
                    + " chooses", failure);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
