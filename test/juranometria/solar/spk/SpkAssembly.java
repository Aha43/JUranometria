package juranometria.solar.spk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * A test-only DAF assembler: a kernel made of chosen segments copied
 * whole from other kernels, each optionally relabelled with another
 * centre and target, in the order given. It exists to make kernels
 * the writer refuses to make - a pair whose segments leave a gap, or
 * two that overlap and disagree - so that the reader's selection can
 * be proved on them (issue #473).
 */
final class SpkAssembly {

    private static final int RECORD_BYTES = 1024;
    private static final int WORDS_PER_RECORD = RECORD_BYTES / 8;
    private static final int SUMMARY_WORDS = 5;
    private static final byte[] FTPSTR =
            "FTPSTR:\r:\n:\r\n:\r\u0000:\u0081:\u0010\u00ce:ENDFTP"
                    .getBytes(StandardCharsets.ISO_8859_1);

    /** A segment of {@code kernel} at {@code index}, labelled as given. */
    record Part(byte[] kernel, int index, int center, int target) {
    }

    private SpkAssembly() {
    }

    static byte[] of(List<Part> parts) throws IOException {
        int firstDataWord = 3 * WORDS_PER_RECORD + 1; // file, summary, names
        int address = firstDataWord;
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        ByteBuffer summary = record();
        summary.putDouble(0.0).putDouble(0.0).putDouble(parts.size());
        ByteBuffer names = record();
        for (Part part : parts) {
            SpkKernel.Segment s = SpkKernel.read(part.kernel).segments().get(part.index);
            ByteBuffer in = ByteBuffer.wrap(part.kernel).order(ByteOrder.LITTLE_ENDIAN);
            int words = s.lastAddress() - s.firstAddress() + 1;
            byte[] copy = new byte[words * 8];
            in.get((s.firstAddress() - 1) * 8, copy);
            body.write(copy);
            summary.putDouble(s.startEt()).putDouble(s.endEt());
            summary.putInt(part.target).putInt(part.center).putInt(s.frame())
                    .putInt(s.type()).putInt(address).putInt(address + words - 1);
            names.put(padded(s.name(), SUMMARY_WORDS * 8));
            address += words;
        }
        ByteBuffer fileRecord = record();
        fileRecord.put("DAF/SPK ".getBytes(StandardCharsets.US_ASCII));
        fileRecord.putInt(2).putInt(6);
        fileRecord.put(padded("TEST ASSEMBLY", 60));
        fileRecord.putInt(2).putInt(2).putInt(address);
        fileRecord.put("LTL-IEEE".getBytes(StandardCharsets.US_ASCII));
        fileRecord.position(fileRecord.position() + 603);
        fileRecord.put(FTPSTR);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(fileRecord.array());
        out.write(summary.array());
        out.write(names.array());
        out.write(body.toByteArray());
        int tail = (RECORD_BYTES - out.size() % RECORD_BYTES) % RECORD_BYTES;
        out.write(new byte[tail]);
        return out.toByteArray();
    }

    private static ByteBuffer record() {
        return ByteBuffer.allocate(RECORD_BYTES).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static byte[] padded(String text, int length) {
        byte[] out = new byte[length];
        Arrays.fill(out, (byte) ' ');
        byte[] ascii = text.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(ascii, 0, out, 0, ascii.length);
        return out;
    }
}
