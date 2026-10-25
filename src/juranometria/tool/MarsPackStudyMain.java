package juranometria.tool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;
import juranometria.solar.spk.Vector3;

/**
 * The #601 pack study: what Mars, Phobos and Deimos would cost as bounded
 * excerpts, before the owner rules the authority, the interval or whether
 * any moon ships.
 *
 * <p>Measured, not estimated. The sources are NAIF's {@code mar099s.bsp}
 * (Mars, Phobos and Deimos about the Mars barycentre, 1995-2050) and the
 * released {@code de440s.bsp} (the Mars barycentre, 1849-2150). The study
 * answers three questions from the files:
 * <ul>
 *   <li>how far Mars's centre (499) ever lies from the Mars barycentre (4)
 *       over the kernel's span, which decides whether the planet needs the
 *       satellite kernel at all;</li>
 *   <li>whether mar099s's own barycentre is the released DE440's, to the
 *       bit;</li>
 *   <li>what each candidate kernel costs, cut with the released writer and
 *       proved against its source.</li>
 * </ul>
 *
 * <p>Inputs (gitignored downloads, pinned by
 * {@code scripts/download-mars-sources.sh} and
 * {@code scripts/download-solar-system-sources.sh}). Output: the study
 * excerpts under {@code build/mars-pack-study/} and the report on stdout.
 * Study tooling only: nothing in the application reads this.
 */
public final class MarsPackStudyMain {

    private static final Path MAR099S = Path.of("imports/raw/mars-system/mar099s.bsp");
    private static final String MAR099S_SHA256 =
            "997dc93ba640e476da7a494d2237dcdeb145e528db37be8ccee588c615e4e1ff";
    private static final Path DE440S = Path.of("imports/raw/solar-system/de440s.bsp");
    private static final String DE440S_SHA256 =
            "c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2";
    private static final Path OUT = Path.of("build/mars-pack-study");

    /** The released packs' interval and margin (SolarSystemPackMain). */
    private static final double COVERAGE_START_JD = 2415020.5; // 1900-01-01 00:00 TDB
    private static final double COVERAGE_END_JD = 2488434.5;   // 2101-01-01 00:00 TDB
    private static final double MARGIN_DAYS = 31.0;
    private static final double J2000_JD = 2451545.0;

    private static final int MARS_BARYCENTRE = 4;
    private static final int MARS = 499;
    private static final int PHOBOS = 401;
    private static final int DEIMOS = 402;

    private MarsPackStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        StringBuilder report = new StringBuilder();
        report.append("# Mars pack study (#601): the excerpts, measured\n\n");

        byte[] mar099s = pinned(MAR099S, MAR099S_SHA256);
        byte[] de440s = pinned(DE440S, DE440S_SHA256);
        report.append(String.format(Locale.ROOT,
                "Sources: `mar099s.bsp`, %,d bytes, sha256 `%s`; `de440s.bsp`, %,d bytes, sha256 `%s`"
                        + " (both pinned).%n%n", mar099s.length, MAR099S_SHA256, de440s.length,
                DE440S_SHA256));
        SpkKernel mars = SpkKernel.read(mar099s);
        SpkKernel planets = SpkKernel.read(de440s);
        ByteBuffer words = ByteBuffer.wrap(mar099s).order(ByteOrder.LITTLE_ENDIAN);

        report.append("## The source segments of `mar099s.bsp`\n\n")
                .append("| body | centre | target | type | frame | from (JD TDB) | to (JD TDB) |"
                        + " interval length | record words | records | name |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|\n");
        for (SpkKernel.Segment s : mars.segments()) {
            int last = s.lastAddress();
            report.append(String.format(Locale.ROOT,
                    "| %s | %d | %d | %d | %d | %.1f | %.1f | %.4f d | %d | %,d | %s |%n",
                    nameOf(s.target()), s.center(), s.target(), s.type(), s.frame(),
                    J2000_JD + s.startEt() / 86400.0, J2000_JD + s.endEt() / 86400.0,
                    word(words, last - 2) / 86400.0, (int) word(words, last - 1),
                    (int) word(words, last), s.name().strip()));
        }

        SpkKernel.Segment centre = mars.segment(MARS_BARYCENTRE, MARS);
        double spanStart = centre.startEt();
        double spanEnd = centre.endEt();

        // 1. Mars's centre about its barycentre, hourly over the whole span.
        double worst = 0.0;
        double worstEt = spanStart;
        double[] moonMin = {Double.MAX_VALUE, Double.MAX_VALUE};
        double[] moonMax = {0.0, 0.0};
        int hours = 0;
        for (double et = spanStart; et <= spanEnd; et += 3600.0) {
            double r = mars.state(MARS_BARYCENTRE, MARS, et).position().length();
            if (r > worst) {
                worst = r;
                worstEt = et;
            }
            int[] moons = {PHOBOS, DEIMOS};
            for (int k = 0; k < 2; k++) {
                double dd = mars.state(MARS_BARYCENTRE, moons[k], et).position()
                        .minus(mars.state(MARS_BARYCENTRE, MARS, et).position()).length();
                moonMin[k] = Math.min(moonMin[k], dd);
                moonMax[k] = Math.max(moonMax[k], dd);
            }
            hours++;
        }
        report.append(String.format(Locale.ROOT,
                "%n## Mars's centre and the Mars barycentre%n%n"
                        + "4 -> 499 evaluated hourly over the whole span (%,d epochs): the centre never lies"
                        + " more than **%.3f m** from the barycentre (largest at JD %.4f TDB).%n%n"
                        + "Moon distances from Mars's centre over the same epochs: Phobos %,.1f to %,.1f km,"
                        + " Deimos %,.1f to %,.1f km.%n", hours, worst * 1000.0,
                J2000_JD + worstEt / 86400.0, moonMin[0], moonMax[0], moonMin[1], moonMax[1]));

        // 2. mar099s's barycentre against the released DE440.
        double bary = 0.0;
        int baryStates = 0;
        for (double et = spanStart; et <= spanEnd; et += 1.7 * 86400.0) {
            Vector3 a = mars.state(0, MARS_BARYCENTRE, et).position();
            Vector3 b = planets.state(0, MARS_BARYCENTRE, et).position();
            bary = Math.max(bary, a.minus(b).length());
            baryStates++;
        }
        report.append(String.format(Locale.ROOT,
                "%nmar099s's own Mars barycentre (0 -> 4) against de440s's, %,d states on a 1.7-day"
                        + " grid: worst position difference %s.%n", baryStates,
                bary == 0.0 ? "0 (bit-identical)" : String.format(Locale.ROOT, "%.9f km", bary)));

        // 3. The candidate kernels, cut and proved.
        double startEt = (COVERAGE_START_JD - MARGIN_DAYS - J2000_JD) * 86400.0;
        double endEt = (COVERAGE_END_JD + MARGIN_DAYS - J2000_JD) * 86400.0;
        report.append("\n## The candidate kernels, cut\n\n"
                + "Each is cut with the released `SpkExcerpt` and proved against its source on a"
                + " 1.7-day grid and at both ends. The moons' interval ends where `mar099s` ends,"
                + " 2050-01-01 TDB, so it carries no margin after its end.\n\n"
                + "| kernel | source | bodies | from (JD) | to (JD) | bytes | gzip -9 bytes | proof |\n"
                + "|---|---|---|---:|---:|---:|---:|---|\n");
        cut(report, "Mars barycentre, 1900-2100", "de440s", de440s, planets,
                List.of(new SpkExcerpt.Pick(0, MARS_BARYCENTRE)), startEt, endEt);
        double moons2000 = Math.max(spanStart, (2451544.5 - MARGIN_DAYS - J2000_JD) * 86400.0);
        double moons2020 = (2458849.5 - MARGIN_DAYS - J2000_JD) * 86400.0;
        double moons2030 = (2462502.5 - MARGIN_DAYS - J2000_JD) * 86400.0;
        List<SpkExcerpt.Pick> both = List.of(new SpkExcerpt.Pick(MARS_BARYCENTRE, PHOBOS),
                new SpkExcerpt.Pick(MARS_BARYCENTRE, DEIMOS));
        List<SpkExcerpt.Pick> withCentre = List.of(new SpkExcerpt.Pick(MARS_BARYCENTRE, MARS),
                new SpkExcerpt.Pick(MARS_BARYCENTRE, PHOBOS),
                new SpkExcerpt.Pick(MARS_BARYCENTRE, DEIMOS));
        cut(report, "Phobos and Deimos, 2000-2050", "mar099s", mar099s, mars, both, moons2000, spanEnd);
        cut(report, "Mars centre, Phobos and Deimos, 2000-2050", "mar099s", mar099s, mars, withCentre,
                moons2000, spanEnd);
        cut(report, "Phobos, 2000-2050", "mar099s", mar099s, mars,
                List.of(new SpkExcerpt.Pick(MARS_BARYCENTRE, PHOBOS)), moons2000, spanEnd);
        cut(report, "Deimos, 2000-2050", "mar099s", mar099s, mars,
                List.of(new SpkExcerpt.Pick(MARS_BARYCENTRE, DEIMOS)), moons2000, spanEnd);
        cut(report, "Phobos and Deimos, 2020-2050", "mar099s", mar099s, mars, both, moons2020, spanEnd);
        cut(report, "Phobos and Deimos, 2030-2050", "mar099s", mar099s, mars, both, moons2030, spanEnd);
        System.out.print(report);
    }

    private static void cut(StringBuilder report, String label, String sourceName, byte[] source,
                            SpkKernel sourceKernel, List<SpkExcerpt.Pick> picks,
                            double startEt, double endEt) throws Exception {
        byte[] excerpt = SpkExcerpt.of(source, picks, startEt, endEt,
                "JURANOMETRIA MARS STUDY", "Study excerpt (#601), not released.\n");
        Files.write(OUT.resolve(label.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase(Locale.ROOT)
                + ".bsp"), excerpt);
        StringBuilder bodies = new StringBuilder();
        for (SpkExcerpt.Pick p : picks) {
            bodies.append(bodies.length() == 0 ? "" : ", ").append(p.center()).append(" -> ")
                    .append(p.target());
        }
        report.append(String.format(Locale.ROOT, "| %s | %s | %s | %.1f | %.1f | %,d | %,d | %s |%n",
                label, sourceName, bodies, J2000_JD + startEt / 86400.0, J2000_JD + endEt / 86400.0,
                excerpt.length, gzipSize(excerpt),
                prove(sourceKernel, SpkKernel.read(excerpt), picks, startEt, endEt)));
    }

    private static byte[] pinned(Path path, String expected) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        String digest = sha256(bytes);
        if (!digest.equals(expected)) {
            throw new IllegalStateException(path + " does not match its pin: " + digest);
        }
        return bytes;
    }

    private static String nameOf(int target) {
        return switch (target) {
            case 4 -> "Mars barycentre";
            case 499 -> "Mars";
            case 401 -> "Phobos";
            case 402 -> "Deimos";
            case 10 -> "Sun";
            case 3 -> "Earth-Moon barycentre";
            case 399 -> "Earth";
            default -> Integer.toString(target);
        };
    }

    private static double word(ByteBuffer words, int address) {
        return words.getDouble((address - 1) * 8);
    }

    /**
     * Every kept segment evaluated on a 1.7-day grid in excerpt and source,
     * and separately at the interval's last instant: where the interval ends at
     * the source's own last instant, the two may select neighbouring records
     * there, so that instant is reported on its own, never folded into the grid.
     */
    private static String prove(SpkKernel source, SpkKernel excerpt,
                                List<SpkExcerpt.Pick> picks, double startEt, double endEt) {
        int states = 0;
        double grid = 0.0;
        double end = 0.0;
        for (SpkExcerpt.Pick pick : picks) {
            for (double et = startEt; et < endEt; et += 1.7 * 86400.0) {
                grid = Math.max(grid, differ(source, excerpt, pick, et));
                states++;
            }
            end = Math.max(end, differ(source, excerpt, pick, endEt));
        }
        return String.format(Locale.ROOT, "grid: %,d states, worst %s; last instant: %s", states,
                grid == 0.0 ? "0 (bit-identical)"
                        : String.format(Locale.ROOT, "%.3g km - NOT IDENTICAL", grid),
                end == 0.0 ? "0 (bit-identical)" : String.format(Locale.ROOT, "%.3g km", end));
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
