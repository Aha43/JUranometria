package juranometria.solar.spk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Writes a new SPK kernel holding chosen segments of a source kernel
 * over a chosen interval - a <em>modified</em> kernel in NAIF's sense,
 * with its own name, comment area and attribution (issue #398, R-a).
 *
 * <p>The coefficients are copied, never recomputed: for every record
 * kept, the words are the source's words, so a reader evaluating the
 * excerpt gets the source's answer to the last bit. What changes is
 * what is kept - whole records, from the first that covers the start
 * of the interval to the last that covers its end - and the four-word
 * directory that says how many there are and where they begin.
 *
 * <p>The output is deterministic: the same source bytes, segments,
 * interval, internal name and comment give the same output bytes, so
 * a committed pack can be proved by rebuilding it. Little-endian IEEE,
 * as the source kernels are; a big-endian source is refused rather
 * than converted.
 */
public final class SpkExcerpt {

    private static final int RECORD_BYTES = 1024;
    private static final int WORDS_PER_RECORD = RECORD_BYTES / 8;
    private static final int SUMMARY_WORDS = 5; // ND + (NI + 1) / 2
    private static final byte[] FTPSTR =
            "FTPSTR:\r:\n:\r\n:\r\u0000:\u0081:\u0010\u00ce:ENDFTP"
                    .getBytes(StandardCharsets.ISO_8859_1);

    private SpkExcerpt() {
    }

    /** A segment to keep, by its centre and target. */
    public record Pick(int center, int target) {
    }

    /**
     * The excerpt.
     *
     * @param source       the source kernel's bytes
     * @param picks        which segments to keep, in output order
     * @param startEt      start of the interval, TDB seconds past J2000
     * @param endEt        end of the interval
     * @param internalName up to 60 ASCII characters for the file record
     * @param comment      the comment area, plain text with newlines
     */
    public static byte[] of(byte[] source, List<Pick> picks,
                            double startEt, double endEt,
                            String internalName, String comment)
            throws IOException {
        if (!(endEt > startEt)) {
            throw new IllegalArgumentException("the interval must run forward");
        }
        SpkKernel kernel = SpkKernel.read(source);
        String format = new String(source, 88, 8, StandardCharsets.US_ASCII)
                .strip();
        if (!"LTL-IEEE".equals(format)) {
            throw new IOException("only a little-endian source is excerpted;"
                    + " this one is " + format);
        }
        ByteBuffer in = ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN);

        // Gather the kept records of each picked segment.
        List<double[]> data = new ArrayList<>();
        List<double[]> bounds = new ArrayList<>();
        List<SpkKernel.Segment> kept = new ArrayList<>();
        for (Pick pick : picks) {
            SpkKernel.Segment s = kernel.segment(pick.center, pick.target);
            if (!s.covers(startEt) || !s.covers(endEt)) {
                throw new IllegalArgumentException("segment " + s.name()
                        + " " + pick + " does not cover the whole interval");
            }
            int last = s.lastAddress();
            double init = word(in, last - 3);
            double intlen = word(in, last - 2);
            int rsize = (int) word(in, last - 1);
            int n = (int) word(in, last);
            int first = (int) Math.floor((startEt - init) / intlen);
            int stop = (int) Math.ceil((endEt - init) / intlen) - 1;
            first = Math.max(0, first);
            stop = Math.min(n - 1, Math.max(stop, first));
            int records = stop - first + 1;
            double[] words = new double[records * rsize + 4];
            for (int i = 0; i < records * rsize; i++) {
                words[i] = word(in, s.firstAddress() + first * rsize + i);
            }
            double newInit = init + first * intlen;
            words[records * rsize] = newInit;
            words[records * rsize + 1] = intlen;
            words[records * rsize + 2] = rsize;
            words[records * rsize + 3] = records;
            data.add(words);
            bounds.add(new double[] {newInit, newInit + records * intlen});
            kept.add(s);
        }

        // Lay the file out: file record, comment records, one summary
        // record, one name record, then the data.
        byte[] commentBytes = commentArea(comment);
        int commentRecords = (commentBytes.length + RECORD_BYTES - 1) / RECORD_BYTES;
        int forward = 2 + commentRecords;
        int firstDataWord = (forward + 1) * WORDS_PER_RECORD + 1;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int address = firstDataWord;
        int[] firstAddress = new int[kept.size()];
        int[] lastAddress = new int[kept.size()];
        for (int i = 0; i < kept.size(); i++) {
            firstAddress[i] = address;
            address += data.get(i).length;
            lastAddress[i] = address - 1;
        }
        int free = address;

        ByteBuffer fileRecord = record();
        fileRecord.put("DAF/SPK ".getBytes(StandardCharsets.US_ASCII));
        fileRecord.putInt(2).putInt(6);
        fileRecord.put(padded(internalName, 60));
        fileRecord.putInt(forward).putInt(forward).putInt(free);
        fileRecord.put("LTL-IEEE".getBytes(StandardCharsets.US_ASCII));
        fileRecord.position(fileRecord.position() + 603);
        fileRecord.put(FTPSTR);
        out.write(fileRecord.array());

        byte[] comments = Arrays.copyOf(commentBytes, commentRecords * RECORD_BYTES);
        out.write(comments);

        ByteBuffer summary = record();
        summary.putDouble(0.0).putDouble(0.0).putDouble(kept.size());
        for (int i = 0; i < kept.size(); i++) {
            SpkKernel.Segment s = kept.get(i);
            summary.putDouble(bounds.get(i)[0]).putDouble(bounds.get(i)[1]);
            summary.putInt(s.target()).putInt(s.center()).putInt(s.frame())
                    .putInt(s.type()).putInt(firstAddress[i])
                    .putInt(lastAddress[i]);
        }
        out.write(summary.array());

        ByteBuffer names = record();
        for (SpkKernel.Segment s : kept) {
            names.put(padded(s.name(), SUMMARY_WORDS * 8));
        }
        out.write(names.array());

        ByteBuffer body = ByteBuffer.allocate((free - firstDataWord) * 8)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (double[] words : data) {
            for (double w : words) {
                body.putDouble(w);
            }
        }
        out.write(body.array());
        // DAF files are whole records long.
        int tail = (RECORD_BYTES - out.size() % RECORD_BYTES) % RECORD_BYTES;
        out.write(new byte[tail]);
        return out.toByteArray();
    }

    private static double word(ByteBuffer in, int address) {
        return in.getDouble((address - 1) * 8);
    }

    private static ByteBuffer record() {
        return ByteBuffer.allocate(RECORD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static byte[] padded(String text, int length) {
        byte[] ascii = text.getBytes(StandardCharsets.US_ASCII);
        if (ascii.length > length) {
            throw new IllegalArgumentException("\"" + text + "\" exceeds "
                    + length + " characters");
        }
        byte[] out = new byte[length];
        Arrays.fill(out, (byte) ' ');
        System.arraycopy(ascii, 0, out, 0, ascii.length);
        return out;
    }

    /** NAIF's comment encoding: lines end in 0x00, the area in 0x04. */
    private static byte[] commentArea(String comment) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (String line : comment.split("\n", -1)) {
            byte[] ascii = line.getBytes(StandardCharsets.US_ASCII);
            for (byte b : ascii) {
                if (b < 0x20 || b > 0x7e) {
                    throw new IllegalArgumentException("comment lines are"
                            + " printable ASCII; found byte " + b);
                }
            }
            bytes.writeBytes(ascii);
            bytes.write(0x00);
        }
        bytes.write(0x04);
        return bytes.toByteArray();
    }
}
