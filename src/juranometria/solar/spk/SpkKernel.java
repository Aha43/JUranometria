package juranometria.solar.spk;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A reader for an SPK ephemeris kernel - the NAIF binary that carries
 * a JPL Development Ephemeris as Chebyshev polynomials - for the one
 * segment type such a kernel uses (Type 2, position-only Chebyshev
 * records, velocity by differentiation).
 *
 * <p>The format is NAIF's DAF: a file record naming the layout, a
 * comment area, a linked list of summary records describing segments,
 * and the segments' data addressed as 1-based double-word indexes.
 * Each Type 2 segment is a run of equal-length records, one per
 * interval of {@code INTLEN} seconds, each holding the interval's
 * midpoint and radius and then the coefficients for x, y and z; the
 * segment ends with a four-word directory {@code INIT, INTLEN, RSIZE,
 * N}. Everything here is read from those words, never assumed: the
 * record grid, the polynomial degree and the byte order all come from
 * the file.
 *
 * <p>This class knows nothing of bodies, time scales or frames beyond
 * what the kernel states about itself. A state is what the segment's
 * coefficients evaluate to at a TDB epoch in seconds past J2000, in
 * the kernel's own units - kilometres and kilometres per second - and
 * an epoch outside a segment's coverage is refused, never
 * extrapolated.
 *
 * <p>A kernel may carry several segments for one centre and target,
 * each over its own interval - JUP365 keeps two per body, split at
 * 1997-01-16 (issue #472, measured) - so a segment is selected by the
 * epoch as well as the pair (issue #473). Where more than one segment
 * covers an epoch, the one later in the file is used, which is the
 * precedence NAIF's own readers give (SPK Required Reading, the search
 * order of segments); a gap between a pair's segments is refused like
 * any other uncovered epoch, naming what the kernel does cover.
 */
public final class SpkKernel {

    private static final int RECORD_BYTES = 1024;

    /** One segment as its summary describes it. */
    public record Segment(int target, int center, int frame, int type,
                          double startEt, double endEt,
                          int firstAddress, int lastAddress, String name) {

        /** Whether {@code et} lies inside this segment's coverage. */
        public boolean covers(double et) {
            return et >= startEt && et <= endEt;
        }
    }

    /** Position in kilometres and velocity in kilometres per second. */
    public record State(Vector3 position, Vector3 velocity) {
    }

    private final ByteBuffer words;
    private final List<Segment> segments;
    private final String comments;

    private SpkKernel(ByteBuffer words, List<Segment> segments,
                      String comments) {
        this.words = words;
        this.segments = segments;
        this.comments = comments;
    }

    /** Reads a whole kernel from a stream and closes nothing. */
    public static SpkKernel read(InputStream in) throws IOException {
        return read(in.readAllBytes());
    }

    /** Reads a whole kernel from its bytes. */
    public static SpkKernel read(byte[] bytes) throws IOException {
        if (bytes.length < RECORD_BYTES) {
            throw new IOException("not a DAF: " + bytes.length + " bytes");
        }
        String locidw = ascii(bytes, 0, 8);
        if (!locidw.startsWith("DAF/SPK")) {
            throw new IOException("not an SPK kernel: LOCIDW is \""
                    + locidw + "\"");
        }
        String format = ascii(bytes, 88, 8).strip();
        ByteOrder order = switch (format) {
            case "LTL-IEEE" -> ByteOrder.LITTLE_ENDIAN;
            case "BIG-IEEE" -> ByteOrder.BIG_ENDIAN;
            default -> throw new IOException("unsupported DAF binary"
                    + " format \"" + format + "\"");
        };
        ByteBuffer file = ByteBuffer.wrap(bytes).order(order);
        int nd = file.getInt(8);
        int ni = file.getInt(12);
        if (nd != 2 || ni != 6) {
            throw new IOException("not an SPK summary layout: ND=" + nd
                    + " NI=" + ni);
        }
        int forward = file.getInt(76);
        int summarySize = nd + (ni + 1) / 2; // 5 doubles
        List<Segment> segments = new ArrayList<>();
        int record = forward;
        while (record > 0) {
            int base = (record - 1) * RECORD_BYTES;
            int next = (int) file.getDouble(base);
            int count = (int) file.getDouble(base + 16);
            int names = record * RECORD_BYTES; // the following record
            for (int s = 0; s < count; s++) {
                int at = base + 24 + s * summarySize * 8;
                double startEt = file.getDouble(at);
                double endEt = file.getDouble(at + 8);
                int target = file.getInt(at + 16);
                int center = file.getInt(at + 20);
                int frame = file.getInt(at + 24);
                int type = file.getInt(at + 28);
                int first = file.getInt(at + 32);
                int last = file.getInt(at + 36);
                String name = ascii(bytes, names + s * summarySize * 8,
                        summarySize * 8).strip();
                if (type != 2) {
                    throw new IOException("segment " + name + " is SPK type "
                            + type + "; only Type 2 is read");
                }
                segments.add(new Segment(target, center, frame, type,
                        startEt, endEt, first, last, name));
            }
            record = next;
        }
        String comments = comments(bytes, forward);
        return new SpkKernel(file, Collections.unmodifiableList(segments),
                comments);
    }

    /** The comment area, as the file's producers and modifiers left it. */
    public String comments() {
        return comments;
    }

    /** The segments, in file order. */
    public List<Segment> segments() {
        return segments;
    }

    /**
     * Every segment from {@code center} to {@code target}, in file
     * order; empty if the kernel carries none.
     */
    public List<Segment> segments(int center, int target) {
        List<Segment> of = new ArrayList<>();
        for (Segment s : segments) {
            if (s.center == center && s.target == target) {
                of.add(s);
            }
        }
        return Collections.unmodifiableList(of);
    }

    /**
     * The one segment from {@code center} to {@code target}, for a
     * kernel that carries exactly one; a pair with several segments
     * must be selected by epoch with {@link #segment(int, int, double)}.
     *
     * @throws IllegalArgumentException if the kernel has no such
     *         segment, or more than one
     */
    public Segment segment(int center, int target) {
        List<Segment> of = segments(center, target);
        if (of.isEmpty()) {
            throw noSegment(center, target);
        }
        if (of.size() > 1) {
            throw new IllegalArgumentException("the kernel has "
                    + of.size() + " segments " + center + " -> " + target
                    + ", covering " + coverage(of)
                    + "; select one by epoch");
        }
        return of.get(0);
    }

    /**
     * The segment from {@code center} to {@code target} that covers a
     * TDB epoch in seconds past J2000. Where several cover it, the one
     * later in the file - NAIF's precedence.
     *
     * @throws IllegalArgumentException if the kernel has no such
     *         segment, or none that covers the epoch
     */
    public Segment segment(int center, int target, double et) {
        List<Segment> of = segments(center, target);
        if (of.isEmpty()) {
            throw noSegment(center, target);
        }
        for (int i = of.size() - 1; i >= 0; i--) {
            if (of.get(i).covers(et)) {
                return of.get(i);
            }
        }
        throw new IllegalArgumentException(String.format(
                "epoch %.3f s past J2000 (TDB) is outside the %d segment%s"
                        + " %d -> %d, which cover%s %s",
                et, of.size(), of.size() == 1 ? "" : "s", center, target,
                of.size() == 1 ? "s" : "", coverage(of)));
    }

    private static IllegalArgumentException noSegment(int center, int target) {
        return new IllegalArgumentException("the kernel has no segment "
                + center + " -> " + target);
    }

    /** "[a to b], [c to d]" in seconds past J2000, in file order. */
    private static String coverage(List<Segment> of) {
        StringBuilder text = new StringBuilder();
        for (Segment s : of) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(String.format("[%.3f to %.3f]", s.startEt, s.endEt));
        }
        return text.toString();
    }

    /**
     * The state of {@code target} relative to {@code center} at a TDB
     * epoch in seconds past J2000, from the coefficients of the
     * segment that covers it.
     *
     * @throws IllegalArgumentException if no segment covers the epoch
     */
    public State state(int center, int target, double et) {
        Segment s = segment(center, target, et);
        int last = s.lastAddress;
        double init = word(last - 3);
        double intlen = word(last - 2);
        int rsize = (int) word(last - 1);
        int n = (int) word(last);
        int coefficients = (rsize - 2) / 3;
        int index = (int) Math.floor((et - init) / intlen);
        if (index < 0) {
            index = 0;
        } else if (index >= n) {
            index = n - 1;
        }
        int recordAt = s.firstAddress + index * rsize;
        double mid = word(recordAt);
        double radius = word(recordAt + 1);
        double tau = (et - mid) / radius;
        if (tau < -1.0000001 || tau > 1.0000001) {
            throw new IllegalStateException(String.format(
                    "epoch %.3f falls in no record of %s: record %d covers"
                            + " %.3f ± %.3f", et, s.name, index, mid, radius));
        }
        double[] t = new double[coefficients];
        double[] dt = new double[coefficients];
        t[0] = 1.0;
        dt[0] = 0.0;
        if (coefficients > 1) {
            t[1] = tau;
            dt[1] = 1.0;
        }
        for (int k = 2; k < coefficients; k++) {
            t[k] = 2.0 * tau * t[k - 1] - t[k - 2];
            dt[k] = 2.0 * tau * dt[k - 1] + 2.0 * t[k - 1] - dt[k - 2];
        }
        double[] position = new double[3];
        double[] velocity = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            int base = recordAt + 2 + axis * coefficients;
            double p = 0.0;
            double v = 0.0;
            for (int k = coefficients - 1; k >= 0; k--) {
                double c = word(base + k);
                p += c * t[k];
                v += c * dt[k];
            }
            position[axis] = p;
            velocity[axis] = v / radius;
        }
        return new State(new Vector3(position[0], position[1], position[2]),
                new Vector3(velocity[0], velocity[1], velocity[2]));
    }

    /** A double by its 1-based DAF address. */
    private double word(int address) {
        return words.getDouble((address - 1) * 8);
    }

    private static String ascii(byte[] bytes, int at, int length) {
        return new String(bytes, at, length, StandardCharsets.US_ASCII);
    }

    /**
     * The comment area lies between the file record and the first
     * summary record; NAIF stores it as lines terminated by EOL (0x00)
     * and ends it with 0x04.
     */
    private static String comments(byte[] bytes, int forward) {
        StringBuilder text = new StringBuilder();
        int end = (forward - 1) * RECORD_BYTES;
        for (int at = RECORD_BYTES; at < end; at++) {
            byte b = bytes[at];
            if (b == 0x04) {
                break;
            }
            text.append(b == 0x00 ? '\n' : (char) (b & 0xff));
        }
        return text.toString();
    }
}
