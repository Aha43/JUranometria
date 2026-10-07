package juranometria.tool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;
import juranometria.solar.spk.Vector3;

/**
 * The #472 pack study: what a bounded Jovian excerpt of NAIF's
 * {@code jup365.bsp} costs, and what the released Sun, Earth and Moon
 * states have to do with it.
 *
 * <p>Measured, not estimated. JUP365 keeps <em>two</em> segments per
 * body, split at JD 2450464.5 TDB (1997-01-16): the released reader
 * and excerpt writer take the first segment of a body, so the second
 * half of 1900–2100 cannot be cut with them as they stand - a finding
 * for #473, recorded in the report. The study therefore cuts the first
 * half (the released pack's 31-day margin before 1900-01-01 up to the
 * split) with {@link SpkExcerpt} and proves every state bit-identical
 * to the source, and sizes the second half (the split up to 2100-12-31
 * plus the margin) exactly from the second segments' own directories
 * - records kept × record size - laid out as the writer lays a file
 * out. The Jupiter barycentre about the solar-system barycentre is cut
 * from the released {@code de440s.bsp} (segment 0 → 5) and proved the
 * same way. The released kernel is read back and its digest reported:
 * neither layout rewrites its four segments.
 *
 * <p>Inputs (gitignored downloads, pinned by
 * {@code scripts/download-jovian-sources.sh} and the released
 * {@code scripts/download-solar-system-sources.sh}). Output: the
 * excerpts under {@code build/jovian-pack-study/} and a report on
 * stdout. Study tooling only: nothing in the application reads this.
 */
public final class JovianPackStudyMain {

    private static final Path JUP365 = Path.of("imports/raw/jovian-system/jup365.bsp");
    private static final Path DE440S = Path.of("imports/raw/solar-system/de440s.bsp");
    private static final Path RELEASED = Path.of(
            "src/resources/solar-system/juranometria-de440-sun-emb-earth-moon-1900-2100.bsp");
    private static final Path OUT = Path.of("build/jovian-pack-study");

    /** The released pack's interval and margin (SolarSystemPackMain). */
    private static final double COVERAGE_START_JD = 2415020.5; // 1900-01-01 00:00 TDB
    private static final double COVERAGE_END_JD = 2488434.5;   // 2101-01-01 00:00 TDB
    private static final double MARGIN_DAYS = 31.0;
    private static final double J2000_JD = 2451545.0;
    private static final int RECORD_BYTES = 1024;

    private static final int JUPITER_BARYCENTRE = 5;
    private static final int[] BODIES = {501, 502, 503, 504, 599};

    private JovianPackStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        double startEt = (COVERAGE_START_JD - MARGIN_DAYS - J2000_JD) * 86400.0;
        double endEt = (COVERAGE_END_JD + MARGIN_DAYS - J2000_JD) * 86400.0;
        StringBuilder report = new StringBuilder();
        report.append("# Jovian pack study (#472): the excerpt, measured\n\n");

        byte[] jup365 = Files.readAllBytes(JUP365);
        report.append(String.format(Locale.ROOT,
                "source jup365.bsp: %,d bytes, sha256 %s%n%n", jup365.length, sha256(jup365)));
        SpkKernel full = SpkKernel.read(jup365);
        report.append("source segments (type, coverage in JD TDB):\n\n| segment | centre | target | type | from | to |\n|---|---|---|---|---|---|\n");
        for (SpkKernel.Segment s : full.segments()) {
            report.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %.1f | %.1f |%n",
                    s.name(), s.center(), s.target(), s.type(),
                    J2000_JD + s.startEt() / 86400.0, J2000_JD + s.endEt() / 86400.0));
        }

        // The split: the first segment of each body ends where the second begins.
        List<SpkKernel.Segment> firsts = new ArrayList<>();
        List<SpkKernel.Segment> seconds = new ArrayList<>();
        for (int body : BODIES) {
            List<SpkKernel.Segment> of = new ArrayList<>();
            for (SpkKernel.Segment s : full.segments()) {
                if (s.center() == JUPITER_BARYCENTRE && s.target() == body) {
                    of.add(s);
                }
            }
            if (of.size() != 2) {
                throw new IllegalStateException("expected two segments for " + body + ", found " + of.size());
            }
            firsts.add(of.get(0));
            seconds.add(of.get(1));
        }
        double splitEt = firsts.get(0).endEt();
        report.append(String.format(Locale.ROOT,
                "%nEvery Galilean body and Jupiter has two segments, split at JD %.1f TDB (1997-01-16)."
                        + " The reader (`SpkKernel.segment`) and writer (`SpkExcerpt.of`) released at 5.0.0 took the first"
                        + " segment of a body, so an excerpt over 1900–2100 needed epoch-aware segment selection"
                        + " in both - repaired by #473's first change. Here, as in the study, the first half is cut"
                        + " and proved and the second half is sized exactly from its directories.%n%n",
                J2000_JD + splitEt / 86400.0));

        // Part B: the first half, with the released writer, proved bit-identical.
        List<SpkExcerpt.Pick> picks = new ArrayList<>();
        for (int body : BODIES) {
            picks.add(new SpkExcerpt.Pick(JUPITER_BARYCENTRE, body));
        }
        byte[] partB = SpkExcerpt.of(jup365, picks, startEt, splitEt,
                "JURANOMETRIA JUP365 GALILEAN JUPITER 1900-1997 STUDY",
                "Study excerpt (#472), the first half, not released.\n");
        Path partBFile = OUT.resolve("juranometria-jup365-galilean-jupiter-1900-1997-study.bsp");
        Files.write(partBFile, partB);
        SpkKernel partBKernel = SpkKernel.read(partB);
        report.append(String.format(Locale.ROOT,
                "## The Jovian excerpt%n%nfirst half, 1899-12-01 to 1997-01-16, cut with `SpkExcerpt`: `%s`, %,d bytes, sha256 %s, %d segments%n",
                partBFile.getFileName(), partB.length, sha256(partB), partBKernel.segments().size()));
        report.append(prove(full, partBKernel, picks, startEt, splitEt));

        // Part F: the second half, sized from the directories.
        ByteBuffer words = ByteBuffer.wrap(jup365).order(ByteOrder.LITTLE_ENDIAN);
        long dataWords = 0;
        StringBuilder detail = new StringBuilder();
        for (SpkKernel.Segment s : seconds) {
            int last = s.lastAddress();
            double init = word(words, last - 3);
            double intlen = word(words, last - 2);
            int rsize = (int) word(words, last - 1);
            int n = (int) word(words, last);
            int first = Math.max(0, (int) Math.floor((splitEt - init) / intlen));
            int stop = Math.min(n - 1, Math.max((int) Math.ceil((endEt - init) / intlen) - 1, first));
            int records = stop - first + 1;
            dataWords += (long) records * rsize + 4;
            detail.append(String.format(Locale.ROOT,
                    "| %d | %.0f s (%.2f d) | %d | %d of %d | %d |%n", s.target(), intlen, intlen / 86400.0,
                    rsize, records, n, (records * rsize + 4) * 8));
        }
        long partFBytes = layout(dataWords, 1);
        report.append(String.format(Locale.ROOT,
                "%nsecond half, 1997-01-16 to 2101-02-01, sized from the segment directories (the writer's layout:"
                        + " file record, one comment record, summary and name records, then the data): %,d bytes%n%n",
                partFBytes));
        report.append("| body | interval length | record words | records kept | bytes |\n|---|---|---|---|---|\n")
                .append(detail);
        long jovianBytes = partB.length + partFBytes - 4L * RECORD_BYTES;
        report.append(String.format(Locale.ROOT,
                "%nOne Jovian kernel holding both halves (ten segments, one header): about %,d bytes.%n",
                jovianBytes));

        // What a narrower interval would cost: every candidate sized from
        // the same directories, per body, both halves, one header.
        double gzipRatio = gzipSize(partB) / (double) partB.length;
        report.append(String.format(Locale.ROOT,
                "%n### Candidate intervals, sized from the directories (gzip -9 estimated at the first half's"
                        + " measured ratio, %.3f)%n%n", gzipRatio));
        report.append("| interval (civil, with the 31-day margin) | Io | Europa | Ganymede | Callisto | Jupiter | one kernel | gzip, estimated |\n"
                + "|---|---:|---:|---:|---:|---:|---:|---:|\n");
        double[][] candidates = {
                {COVERAGE_START_JD, COVERAGE_END_JD},            // 1900-2100
                {2433282.5, COVERAGE_END_JD},                    // 1950-01-01 .. 2100
                {J2000_JD - 0.5, COVERAGE_END_JD},               // 2000-01-01 .. 2100
                {J2000_JD - 0.5, 2469807.5},                     // 2000-01-01 .. 2050-01-01
                {2458849.5, 2480764.5},                          // 2020-01-01 .. 2080-01-01
        };
        String[] names = {"1900–2100 (the released interval)", "1950–2100", "2000–2100", "2000–2050", "2020–2080"};
        List<SpkKernel.Segment> both = new ArrayList<>(firsts);
        both.addAll(seconds);
        for (int c = 0; c < candidates.length; c++) {
            double from = (candidates[c][0] - MARGIN_DAYS - J2000_JD) * 86400.0;
            double to = (candidates[c][1] + MARGIN_DAYS - J2000_JD) * 86400.0;
            long[] perBody = new long[BODIES.length];
            long total = 0;
            for (SpkKernel.Segment s : both) {
                double lo = Math.max(from, s.startEt());
                double hi = Math.min(to, s.endEt());
                if (hi <= lo) {
                    continue;
                }
                int last = s.lastAddress();
                double init = word(words, last - 3);
                double intlen = word(words, last - 2);
                int rsize = (int) word(words, last - 1);
                int n = (int) word(words, last);
                int first = Math.max(0, (int) Math.floor((lo - init) / intlen));
                int stop = Math.min(n - 1, Math.max((int) Math.ceil((hi - init) / intlen) - 1, first));
                long bytes = ((long) (stop - first + 1) * rsize + 4) * 8;
                for (int b = 0; b < BODIES.length; b++) {
                    if (BODIES[b] == s.target()) {
                        perBody[b] += bytes;
                    }
                }
                total += bytes;
            }
            long file = layout(total / 8, 1);
            report.append(String.format(Locale.ROOT, "| %s | %,d | %,d | %,d | %,d | %,d | %,d | %,d |%n",
                    names[c], perBody[0], perBody[1], perBody[2], perBody[3], perBody[4], file,
                    Math.round(file * gzipRatio)));
        }

        // The Jupiter barycentre from the released source (de440s).
        byte[] de440s = Files.readAllBytes(DE440S);
        SpkKernel planets = SpkKernel.read(de440s);
        List<SpkExcerpt.Pick> barycentrePick = List.of(new SpkExcerpt.Pick(0, JUPITER_BARYCENTRE));
        byte[] barycentre = SpkExcerpt.of(de440s, barycentrePick, startEt, endEt,
                "JURANOMETRIA DE440 JUPITER BARYCENTRE 1900-2100 STUDY",
                "Study excerpt (#472), not released.\n");
        Path barycentreFile = OUT.resolve("juranometria-de440-jupiter-barycentre-1900-2100-study.bsp");
        Files.write(barycentreFile, barycentre);
        report.append(String.format(Locale.ROOT,
                "%n## The Jupiter barycentre%n%nfrom de440s (0 -> 5), 1899-12-01 to 2101-02-01, cut with `SpkExcerpt`: `%s`, %,d bytes, sha256 %s%n",
                barycentreFile.getFileName(), barycentre.length, sha256(barycentre)));
        report.append(prove(planets, SpkKernel.read(barycentre), barycentrePick, startEt, endEt));

        // The released pack, read back.
        byte[] released = Files.readAllBytes(RELEASED);
        SpkKernel releasedKernel = SpkKernel.read(released);
        report.append(String.format(Locale.ROOT,
                "%n## The released pack%n%n`%s`: %,d bytes, sha256 %s, %d segments (0 -> 3, 0 -> 10, 3 -> 399, 3 -> 301).%n",
                RELEASED.getFileName(), released.length, sha256(released), releasedKernel.segments().size()));
        List<SpkExcerpt.Pick> releasedPicks = new ArrayList<>();
        for (SpkKernel.Segment s : releasedKernel.segments()) {
            releasedPicks.add(new SpkExcerpt.Pick(s.center(), s.target()));
        }
        report.append(prove(planets, releasedKernel, releasedPicks, startEt, endEt)
                .replace("excerpt against source", "released kernel against de440s"));

        // The layouts.
        long layoutA = released.length + barycentre.length + jovianBytes;
        long layoutB = layoutA - 3L * RECORD_BYTES * 2;
        report.append("\n## The layouts\n\n");
        report.append(String.format(Locale.ROOT,
                "- **A, a second pack beside the released one:** the released kernel untouched (same %,d bytes,"
                        + " same digest), plus a Jovian pack of two kernels - the barycentre (%,d bytes) and the"
                        + " Galilean excerpt (about %,d bytes) - or one Jovian kernel of eleven segments: about"
                        + " %,d bytes in all.%n",
                released.length, barycentre.length, jovianBytes, layoutA));
        report.append(String.format(Locale.ROOT,
                "- **B, one regenerated Solar System pack (v3)** of fifteen segments: about %,d bytes; the four"
                        + " released segments would be rewritten from the same de440s coefficients (the builder"
                        + " proves them identical, as it did for v2), but the released file's bytes change.%n",
                layoutB));
        report.append(String.format(Locale.ROOT,
                "%ngzip -9: released %,d, barycentre %,d, first-half Galilean excerpt %,d.%n",
                gzipSize(released), gzipSize(barycentre), gzipSize(partB)));
        report.append("\nThe Sun, Earth and Moon states are the released kernel's own bytes in layout A; in"
                + " layout B they are the same coefficients rewritten, held bit for bit by the builder's proof"
                + " and `SunInvarianceTest`.\n");

        Files.writeString(OUT.resolve("report.md"), report, StandardCharsets.UTF_8);
        System.out.print(report);
    }

    /** The writer's layout: file record, comment records, summary record, name record, data. */
    private static long layout(long dataWords, int commentRecords) {
        long dataBytes = dataWords * 8;
        long dataRecords = (dataBytes + RECORD_BYTES - 1) / RECORD_BYTES;
        return (2L + commentRecords + 2L + dataRecords) * RECORD_BYTES;
    }

    private static double word(ByteBuffer words, int address) {
        return words.getDouble((address - 1) * 8);
    }

    /**
     * Every kept segment evaluated on a 1.7-day grid plus both ends,
     * in excerpt and source, and required identical to the bit.
     */
    private static String prove(SpkKernel source, SpkKernel excerpt,
                                List<SpkExcerpt.Pick> picks, double startEt, double endEt) {
        int states = 0;
        double worst = 0.0;
        double step = 1.7 * 86400.0;
        for (SpkExcerpt.Pick pick : picks) {
            SpkKernel.Segment segment = excerpt.segment(pick.center(), pick.target());
            double from = Math.max(startEt, segment.startEt());
            double to = Math.min(endEt, segment.endEt());
            for (double et = from; et <= to; et += step) {
                worst = Math.max(worst, differ(source, excerpt, pick, et));
                states++;
            }
            worst = Math.max(worst, differ(source, excerpt, pick, to));
            states++;
        }
        return String.format(Locale.ROOT,
                "  proof: %,d states on a 1.7-day grid, excerpt against source, worst difference %s%n",
                states, worst == 0.0 ? "0 (bit-identical)" : worst + " km - NOT IDENTICAL");
    }

    private static double differ(SpkKernel a, SpkKernel b, SpkExcerpt.Pick pick, double et) {
        SpkKernel.State x = a.state(pick.center(), pick.target(), et);
        SpkKernel.State y = b.state(pick.center(), pick.target(), et);
        Vector3 dp = x.position().minus(y.position());
        Vector3 dv = x.velocity().minus(y.velocity());
        return Math.max(dp.length(), dv.length());
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static long gzipSize(byte[] bytes) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(out) {
            { def.setLevel(9); }
        }) {
            gz.write(bytes);
        }
        return out.size();
    }
}
