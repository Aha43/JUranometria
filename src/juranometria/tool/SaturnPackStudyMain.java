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
 * The #574 pack study: what a bounded excerpt of NAIF's {@code sat441.bsp}
 * would cost, body by body and interval by interval, before the owner rules
 * the moon set, the interval or whether any excerpt ships.
 *
 * <p>Measured, not estimated. Saturn (699) and each candidate moon -
 * Mimas, Enceladus, Tethys, Dione, Rhea, Titan, Hyperion, Iapetus
 * (601-608), and Phoebe (609) for comparison - about the Saturn
 * barycentre, from {@code sat441.bsp}; the barycentre about the
 * solar-system barycentre from the released {@code de440s.bsp}. The full
 * candidate excerpt over the released interval (1900-2100, the released
 * packs' 31-day margin each side) is cut with the released writer, read
 * back and proved bit-identical to its source on a 1.7-day grid; every
 * candidate interval is then sized exactly from the segment directories,
 * records kept by record size, laid out as the writer lays a file out.
 *
 * <p>Inputs (gitignored downloads, pinned by
 * {@code scripts/download-saturn-sources.sh} and
 * {@code scripts/download-solar-system-sources.sh}). Output: the study
 * excerpt under {@code build/saturn-pack-study/} and the report on stdout
 * and in {@code build/saturn-pack-study/report.md}. Study tooling only:
 * nothing in the application reads this, and the excerpt is not a
 * resource.
 */
public final class SaturnPackStudyMain {

    private static final Path SAT441 = Path.of("imports/raw/saturn-system/sat441.bsp");
    private static final String SAT441_SHA256 =
            "d7e444a9ba7a52b8f448ff0747030789524d2f0c1212e4e2bd7f5fc3c96444d5";
    private static final Path DE440S = Path.of("imports/raw/solar-system/de440s.bsp");
    private static final String DE440S_SHA256 =
            "c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2";
    private static final Path OUT = Path.of("build/saturn-pack-study");

    /** The released packs' interval and margin (SolarSystemPackMain). */
    private static final double COVERAGE_START_JD = 2415020.5; // 1900-01-01 00:00 TDB
    private static final double COVERAGE_END_JD = 2488434.5;   // 2101-01-01 00:00 TDB
    private static final double MARGIN_DAYS = 31.0;
    /** The ruled Jovian moons' start, 2000-01-01 00:00 TDB (JovianPackMain). */
    private static final double MOONS_START_JD = 2451544.5;
    private static final double J2000_JD = 2451545.0;
    private static final int RECORD_BYTES = 1024;

    private static final int SATURN_BARYCENTRE = 6;
    private static final int[] BODIES = {699, 601, 602, 603, 604, 605, 606, 607, 608, 609};
    private static final String[] NAMES = {"Saturn", "Mimas", "Enceladus", "Tethys", "Dione",
            "Rhea", "Titan", "Hyperion", "Iapetus", "Phoebe"};

    private SaturnPackStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        double startEt = (COVERAGE_START_JD - MARGIN_DAYS - J2000_JD) * 86400.0;
        double endEt = (COVERAGE_END_JD + MARGIN_DAYS - J2000_JD) * 86400.0;
        StringBuilder report = new StringBuilder();
        report.append("# Saturn pack study (#574): the excerpt, measured\n\n");

        byte[] sat441 = Files.readAllBytes(SAT441);
        String sourceDigest = sha256(sat441);
        if (!sourceDigest.equals(SAT441_SHA256)) {
            throw new IllegalStateException("sat441.bsp does not match its pin: " + sourceDigest);
        }
        report.append(String.format(Locale.ROOT,
                "source `sat441.bsp`: %,d bytes, sha256 `%s` (pinned)%n%n", sat441.length,
                sourceDigest));
        SpkKernel full = SpkKernel.read(sat441);
        ByteBuffer words = ByteBuffer.wrap(sat441).order(ByteOrder.LITTLE_ENDIAN);

        report.append("## The source segments\n\n")
                .append("| body | centre | target | type | from (JD TDB) | to (JD TDB) |"
                        + " interval length | record words | records |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (SpkKernel.Segment s : full.segments()) {
            int last = s.lastAddress();
            double intlen = word(words, last - 2);
            int rsize = (int) word(words, last - 1);
            int n = (int) word(words, last);
            report.append(String.format(Locale.ROOT,
                    "| %s | %d | %d | %d | %.1f | %.1f | %.2f d | %d | %,d |%n",
                    nameOf(s.target()), s.center(), s.target(), s.type(),
                    J2000_JD + s.startEt() / 86400.0, J2000_JD + s.endEt() / 86400.0,
                    intlen / 86400.0, rsize, n));
        }

        // The full candidate excerpt over the released interval, cut and proved.
        List<SpkExcerpt.Pick> picks = new ArrayList<>();
        for (int body : BODIES) {
            picks.add(new SpkExcerpt.Pick(SATURN_BARYCENTRE, body));
        }
        byte[] excerpt = SpkExcerpt.of(sat441, picks, startEt, endEt,
                "JURANOMETRIA SAT441 SATURN MOONS 1900-2100 STUDY",
                "Study excerpt (#574), not released.\n");
        Path excerptFile = OUT.resolve("juranometria-sat441-saturn-moons-1900-2100-study.bsp");
        Files.write(excerptFile, excerpt);
        SpkKernel excerptKernel = SpkKernel.read(excerpt);
        report.append(String.format(Locale.ROOT,
                "%n## Saturn and every candidate moon, 1900-2100%n%n"
                        + "Cut with the released `SpkExcerpt` over 1899-12-01 to 2101-02-01 TDB (the"
                        + " released packs' interval and 31-day margin): `%s`, %,d bytes, sha256 `%s`,"
                        + " %d segments.%n", excerptFile.getFileName(), excerpt.length,
                sha256(excerpt), excerptKernel.segments().size()));
        report.append(prove(full, excerptKernel, picks, startEt, endEt));

        // Every candidate interval, per body, sized from the directories.
        double gzipRatio = gzipSize(excerpt) / (double) excerpt.length;
        report.append(String.format(Locale.ROOT,
                "%n## Candidate intervals, per body, sized from the directories%n%n"
                        + "Bytes of each body's kept records (with the released 31-day margin), and one"
                        + " kernel holding Saturn and the eight major moons laid out as the writer lays it"
                        + " out. gzip -9 is estimated at the full excerpt's measured ratio, %.3f.%n%n",
                gzipRatio));
        report.append("| interval | ");
        for (String name : NAMES) {
            report.append(name).append(" | ");
        }
        report.append("Saturn + 601-608, one kernel | gzip, estimated |\n|---|");
        for (int i = 0; i < NAMES.length + 2; i++) {
            report.append("---:|");
        }
        report.append('\n');
        double[][] candidates = {
                {COVERAGE_START_JD, COVERAGE_END_JD},  // 1900-2100
                {2433282.5, COVERAGE_END_JD},          // 1950-2100
                {J2000_JD - 0.5, COVERAGE_END_JD},     // 2000-2100
                {J2000_JD - 0.5, 2469807.5},           // 2000-2050
                {2458849.5, 2480764.5},                // 2020-2080
        };
        String[] intervals = {"1900-2100 (the released interval)", "1950-2100", "2000-2100",
                "2000-2050", "2020-2080"};
        for (int c = 0; c < candidates.length; c++) {
            double from = (candidates[c][0] - MARGIN_DAYS - J2000_JD) * 86400.0;
            double to = (candidates[c][1] + MARGIN_DAYS - J2000_JD) * 86400.0;
            long[] perBody = new long[BODIES.length];
            for (SpkKernel.Segment s : full.segments()) {
                if (s.center() != SATURN_BARYCENTRE) {
                    continue;
                }
                int b = indexOf(s.target());
                if (b < 0) {
                    continue;
                }
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
                perBody[b] += ((long) (stop - first + 1) * rsize + 4) * 8;
            }
            long major = 0;
            for (int b = 0; b < BODIES.length; b++) {
                if (BODIES[b] != 609) {
                    major += perBody[b];
                }
            }
            long file = layout(major / 8, 1);
            report.append("| ").append(intervals[c]).append(" | ");
            for (long bytes : perBody) {
                report.append(String.format(Locale.ROOT, "%,d | ", bytes));
            }
            report.append(String.format(Locale.ROOT, "%,d | %,d |%n", file,
                    Math.round(file * gzipRatio)));
        }

        // The Saturn barycentre from the released source.
        byte[] de440s = Files.readAllBytes(DE440S);
        if (!sha256(de440s).equals(DE440S_SHA256)) {
            throw new IllegalStateException("de440s.bsp does not match its pin");
        }
        SpkKernel planets = SpkKernel.read(de440s);
        List<SpkExcerpt.Pick> barycentrePick = List.of(new SpkExcerpt.Pick(0, SATURN_BARYCENTRE));
        byte[] barycentre = SpkExcerpt.of(de440s, barycentrePick, startEt, endEt,
                "JURANOMETRIA DE440 SATURN BARYCENTRE 1900-2100 STUDY",
                "Study excerpt (#574), not released.\n");
        Path barycentreFile = OUT.resolve("juranometria-de440-saturn-barycentre-1900-2100-study.bsp");
        Files.write(barycentreFile, barycentre);
        report.append(String.format(Locale.ROOT,
                "%n## The Saturn barycentre%n%nFrom the released `de440s.bsp` (0 -> 6), 1899-12-01 to"
                        + " 2101-02-01, cut with `SpkExcerpt`: `%s`, %,d bytes, sha256 `%s`.%n",
                barycentreFile.getFileName(), barycentre.length, sha256(barycentre)));
        report.append(prove(planets, SpkKernel.read(barycentre), barycentrePick, startEt, endEt));

        // The barycentre in sat441 against de440s: the same DE440 coefficients?
        report.append(compareBarycentres(full, planets, startEt, endEt));

        report.append(String.format(Locale.ROOT,
                "%ngzip -9: full excerpt %,d bytes, barycentre %,d bytes.%n",
                gzipSize(excerpt), gzipSize(barycentre)));

        // The production layout, cut (the Jovian layout ruled on #472): Saturn and
        // its barycentre over 1900-2100 in one kernel, the moons over 2000-2100 in
        // another - for the five proposed moons and for the seven with Mimas and
        // Enceladus - each written by the released writer and proved.
        double moonsStartEt = (MOONS_START_JD - MARGIN_DAYS - J2000_JD) * 86400.0;
        List<SpkExcerpt.Pick> planetPicks = List.of(new SpkExcerpt.Pick(0, SATURN_BARYCENTRE),
                new SpkExcerpt.Pick(SATURN_BARYCENTRE, 699));
        byte[] planet = SpkExcerpt.of(sat441, planetPicks, startEt, endEt,
                "JURANOMETRIA SAT441 SATURN 1900-2100 STUDY", "Study excerpt (#574), not released.\n");
        report.append(String.format(Locale.ROOT,
                "%n## The production layout, cut%n%n"
                        + "As Jupiter's packs are laid out (#472): Saturn (6 -> 699) and its barycentre"
                        + " (0 -> 6, sat441's own, bit-identical to de440s's as shown above) over 1899-12-01"
                        + " to 2101-02-01 in one kernel; the moons (6 -> 6xx) over 1999-12-01 to 2101-02-01"
                        + " in another. Each cut with the released `SpkExcerpt` and proved against"
                        + " `sat441.bsp`.%n%n"
                        + "| kernel | bodies | bytes | gzip -9 bytes | sha256 |%n|---|---|---:|---:|---|%n"));
        report.append(String.format(Locale.ROOT, "| Saturn, 1900-2100 | 6, 699 | %,d | %,d | `%s` |%n",
                planet.length, gzipSize(planet), sha256(planet)));
        String proofs = prove(full, SpkKernel.read(planet), planetPicks, startEt, endEt);
        long[] totals = new long[2];
        long[] gzipTotals = new long[2];
        int[][] sets = {{603, 604, 605, 606, 608}, {601, 602, 603, 604, 605, 606, 608}};
        String[] setNames = {"five moons", "seven moons"};
        for (int k = 0; k < sets.length; k++) {
            List<SpkExcerpt.Pick> moonPicks = new ArrayList<>();
            StringBuilder ids = new StringBuilder();
            for (int body : sets[k]) {
                moonPicks.add(new SpkExcerpt.Pick(SATURN_BARYCENTRE, body));
                ids.append(ids.length() == 0 ? "" : ", ").append(body);
            }
            byte[] moons = SpkExcerpt.of(sat441, moonPicks, moonsStartEt, endEt,
                    "JURANOMETRIA SAT441 SATURN MOONS 2000-2100 STUDY", "Study excerpt (#574), not released.\n");
            report.append(String.format(Locale.ROOT, "| %s, 2000-2100 | %s | %,d | %,d | `%s` |%n",
                    setNames[k], ids, moons.length, gzipSize(moons), sha256(moons)));
            proofs += prove(full, SpkKernel.read(moons), moonPicks, moonsStartEt, endEt);
            totals[k] = planet.length + moons.length;
            gzipTotals[k] = gzipSize(planet) + gzipSize(moons);
        }
        report.append(String.format(Locale.ROOT,
                "%nThe Saturn pack, both kernels: **%,d bytes with the five moons** (%,d gzipped);"
                        + " **%,d bytes with the seven** (%,d gzipped).%n%nProofs, in the order above:%n%s",
                totals[0], gzipTotals[0], totals[1], gzipTotals[1], proofs));
        Files.writeString(OUT.resolve("report.md"), report, StandardCharsets.UTF_8);
        System.out.print(report);
    }

    /** Whether sat441's own 0 -> 6 is the released de440s's, to the bit, over the interval. */
    private static String compareBarycentres(SpkKernel sat441, SpkKernel de440s,
                                             double startEt, double endEt) {
        double worst = 0.0;
        int states = 0;
        for (double et = startEt; et <= endEt; et += 1.7 * 86400.0) {
            Vector3 a = sat441.state(0, SATURN_BARYCENTRE, et).position();
            Vector3 b = de440s.state(0, SATURN_BARYCENTRE, et).position();
            worst = Math.max(worst, a.minus(b).length());
            states++;
        }
        return String.format(Locale.ROOT,
                "%nsat441's own Saturn barycentre (0 -> 6) against de440s's, %,d states on a 1.7-day"
                        + " grid: worst position difference %s.%n", states,
                worst == 0.0 ? "0 (bit-identical)" : String.format(Locale.ROOT, "%.6f km", worst));
    }

    private static int indexOf(int body) {
        for (int b = 0; b < BODIES.length; b++) {
            if (BODIES[b] == body) {
                return b;
            }
        }
        return -1;
    }

    private static String nameOf(int target) {
        int b = indexOf(target);
        return b >= 0 ? NAMES[b] : switch (target) {
            case 6 -> "Saturn barycentre";
            case 10 -> "Sun";
            case 3 -> "Earth-Moon barycentre";
            case 399 -> "Earth";
            case 612 -> "Helene";
            case 613 -> "Telesto";
            case 614 -> "Calypso";
            case 632 -> "Methone";
            case 634 -> "Polydeuces";
            default -> Integer.toString(target);
        };
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

    /** Every kept segment evaluated on a 1.7-day grid plus both ends, in excerpt and source. */
    private static String prove(SpkKernel source, SpkKernel excerpt,
                                List<SpkExcerpt.Pick> picks, double startEt, double endEt) {
        int states = 0;
        double worst = 0.0;
        double step = 1.7 * 86400.0;
        for (SpkExcerpt.Pick pick : picks) {
            for (double et = startEt; et <= endEt; et += step) {
                worst = Math.max(worst, differ(source, excerpt, pick, et));
                states++;
            }
            worst = Math.max(worst, differ(source, excerpt, pick, endEt));
            states++;
        }
        return String.format(Locale.ROOT,
                "%nProof: %,d states on a 1.7-day grid, excerpt against source, worst difference %s.%n",
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
