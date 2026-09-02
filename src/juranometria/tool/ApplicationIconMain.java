package juranometria.tool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import juranometria.app.ApplicationIcon;

/**
 * Writes every committed application-icon container from the one
 * geometry the gate chose (Sprint 23, issue #202).
 *
 * <p>The containers are written <strong>here, in Java</strong>,
 * rather than by each platform's own tool. ICO and ICNS are both
 * simple envelopes around PNG payloads, and writing them directly
 * buys three things a toolchain cannot: the same bytes on any
 * machine, no dependence on macOS to produce a macOS icon, and a
 * regeneration a reviewer can run and compare. Nothing here emits a
 * timestamp, a path, or a random identifier.
 *
 * <p>Run by {@code make icons}. The outputs are committed, because a
 * clean release must not need a design tool - or a network - to
 * build.
 */
public final class ApplicationIconMain {

    private ApplicationIconMain() {
    }

    /** The PNGs committed beside the containers, and their sizes. */
    private static final int[] PNG_SIZES =
            {16, 24, 32, 48, 64, 128, 256, 512, 1024};

    /**
     * The sizes Windows actually selects between, smallest first.
     * 256 is the one Explorer's large views use and the one an
     * incomplete icon is usually missing.
     */
    private static final int[] ICO_SIZES = {16, 24, 32, 48, 64, 128, 256};

    /**
     * The ICNS entries macOS reads, as four-character types. The
     * modern PNG-payload types only: ic07 upward take a PNG whole,
     * which is what keeps this writer honest and small.
     */
    private static final String[][] ICNS_ENTRIES = {
            {"ic11", "32"},    // 16pt @2x
            {"ic12", "64"},    // 32pt @2x
            {"ic07", "128"},
            {"ic08", "256"},
            {"ic09", "512"},
            {"ic10", "1024"},  // 512pt @2x
    };

    public static void main(String[] args) throws IOException {
        File dir = new File(args.length > 0 ? args[0] : "packaging/icon");
        dir.mkdirs();

        for (int size : PNG_SIZES) {
            Files.write(new File(dir, "JUranometria-" + size + ".png").toPath(),
                    png(size));
        }
        Files.write(new File(dir, "JUranometria.ico").toPath(), ico());
        Files.write(new File(dir, "JUranometria.icns").toPath(), icns());

        System.out.println("application mark written to " + dir.getPath()
                + ": " + PNG_SIZES.length + " PNGs, one ICO of "
                + ICO_SIZES.length + " sizes, one ICNS of "
                + ICNS_ENTRIES.length + " entries");
    }

    /** One size of the mark, PNG-encoded. */
    static byte[] png(int size) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(ApplicationIcon.at(size), "png", bytes);
        return bytes.toByteArray();
    }

    /**
     * A Windows ICO: a six-byte header, one sixteen-byte directory
     * entry per size, then the PNG payloads. A 256-pixel entry
     * records its width and height as zero, which is how the format
     * says "256" in a byte.
     */
    static byte[] ico() throws IOException {
        List<byte[]> payloads = new ArrayList<>();
        for (int size : ICO_SIZES) {
            payloads.add(png(size));
        }
        int offset = 6 + 16 * ICO_SIZES.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(le16(0));   // reserved
        out.write(le16(1));   // an icon, not a cursor
        out.write(le16(ICO_SIZES.length));
        for (int i = 0; i < ICO_SIZES.length; i++) {
            int size = ICO_SIZES[i];
            byte[] payload = payloads.get(i);
            out.write(size >= 256 ? 0 : size);
            out.write(size >= 256 ? 0 : size);
            out.write(0);                 // colours in palette: none
            out.write(0);                 // reserved
            out.write(le16(1));           // colour planes
            out.write(le16(32));          // bits per pixel
            out.write(le32(payload.length));
            out.write(le32(offset));
            offset += payload.length;
        }
        for (byte[] payload : payloads) {
            out.write(payload);
        }
        return out.toByteArray();
    }

    /**
     * A macOS ICNS: the magic, the total length, then one
     * type-and-length-prefixed PNG per entry. Big-endian, unlike the
     * ICO - the two formats agree on almost nothing.
     */
    static byte[] icns() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (String[] entry : ICNS_ENTRIES) {
            byte[] payload = png(Integer.parseInt(entry[1]));
            body.write(entry[0].getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            body.write(be32(8 + payload.length));
            body.write(payload);
        }
        byte[] entries = body.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("icns".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(be32(8 + entries.length));
        out.write(entries);
        return out.toByteArray();
    }

    private static byte[] le16(int value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) value).array();
    }

    private static byte[] le32(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value).array();
    }

    private static byte[] be32(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                .putInt(value).array();
    }
}
