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
        return encode(image, dpi, sheet.metadata());
    }

    /**
     * PNG bytes carrying a pHYs chunk that states the resolution, and
     * tEXt chunks that state what the sheet is (Sprint 30, #300).
     *
     * <p>A PNG said only how large it was. The other two formats have
     * carried the sheet's own account of itself since #285 - SVG in
     * its title, description and metadata elements, PDF in its
     * information dictionary - and a raster sheet travels further
     * than either, because it is the one a reader drops into a
     * message. It could not say what part of the sky it showed, let
     * alone which projection drew it.
     *
     * <p><strong>iTXt</strong> rather than tEXt, and that is the
     * whole of a review finding. A tEXt chunk is Latin-1 by the
     * format's definition, and this atlas writes Greek: a sheet
     * titled for α Orionis came out titled for "? Orionis". iTXt is
     * PNG's own answer for exactly that - UTF-8 text, uncompressed
     * here so a reader with any tool can see it - and the keywords
     * are still the format's registered ones.
     */
    private static byte[] encode(BufferedImage image, int dpi,
                                 SheetMetadata about)
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
        IIOMetadataNode text = new IIOMetadataNode("iTXt");
        for (String[] said : new String[][] {
                {"Title", about.title()},
                {"Description", about.description()},
                {"Software", about.producedBy()}}) {
            IIOMetadataNode entry = new IIOMetadataNode("iTXtEntry");
            entry.setAttribute("keyword", said[0]);
            entry.setAttribute("compressionFlag", "FALSE");
            entry.setAttribute("compressionMethod", "0");
            entry.setAttribute("languageTag", "");
            entry.setAttribute("translatedKeyword", said[0]);
            entry.setAttribute("text", said[1]);
            text.appendChild(entry);
        }
        root.appendChild(text);

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
