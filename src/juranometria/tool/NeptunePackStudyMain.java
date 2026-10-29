package juranometria.tool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
 * The #654 pack study: what Neptune, Triton and Nereid would cost as bounded
 * excerpts, before the owner rules the authority, the interval or which
 * moons ship - the Uranus study (#633) for Neptune.
 *
 * <p>Measured, not estimated. The sources are NAIF's
 * {@code nep098_part-1.bsp} (Neptune, Triton, Nereid and Naiad about the
 * Neptune barycentre, NEP098, with DE442's barycentre, 1800-2199) and the
 * released {@code de440s.bsp} (the Neptune barycentre). The study answers
 * from the files:
 * <ul>
 *   <li>how far Neptune's centre (899) lies from the Neptune barycentre (8)
 *       over the atlas's years - in kilometres as well as the angle it
 *       makes from the Earth, the Uranus road's lesson;</li>
 *   <li>how far nep098's own barycentre (DE442) lies from the released
 *       DE440's;</li>
 *   <li>what each candidate excerpt costs, cut with the released writer
 *       and proved against its source.</li>
 * </ul>
 *
 * <p>Inputs: gitignored downloads, approved on #654 and pinned by digest.
 * Output: the study excerpts under {@code build/neptune-pack-study/} and the
 * report on stdout. Study tooling only: nothing in the application reads
 * this.
 */
public final class NeptunePackStudyMain {

    private static final Path NEP098 = Path.of("imports/raw/neptune-system/nep098_part-1.bsp");
    private static final String NEP098_SHA256 =
            "5327f47abba438d993237ff676ab03b728778a965bb238f86de883c0ad7790b8";
    private static final Path DE440S = Path.of("imports/raw/solar-system/de440s.bsp");
    private static final String DE440S_SHA256 =
            "c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2";
    private static final Path OUT = Path.of("build/neptune-pack-study");

    /** The released packs' interval and margin (SolarSystemPackMain). */
    private static final double COVERAGE_START_JD = 2415020.5; // 1900-01-01 00:00 TDB
    private static final double COVERAGE_END_JD = 2488434.5;   // 2101-01-01 00:00 TDB
    private static final double MOONS_2000_JD = 2451544.5;     // 2000-01-01 00:00 TDB
    private static final double MARGIN_DAYS = 31.0;
    private static final double J2000_JD = 2451545.0;

    private static final int BARYCENTRE = 8;
    private static final int NEPTUNE = 899;
    /** The candidates in the kernel: Triton, Nereid, and Naiad, measured for the record. */
    private static final int[] MOONS = {801, 802, 803};
    /** The weekly Oslo matrix the least distance is read from, never typed. */
    private static final Path MATRIX = Path.of("docs/studies/neptune-system/horizons/matrix-7d-neptune-oslo.txt");
    private static final double AU_KM = 149_597_870.7;

    private NeptunePackStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        StringBuilder report = new StringBuilder();
        report.append("# Neptune pack study (#654): the excerpts, measured\n\n");

        byte[] ura = pinned(NEP098, NEP098_SHA256);
        byte[] de440s = pinned(DE440S, DE440S_SHA256);
        report.append(String.format(Locale.ROOT,
                "Sources: `nep098_part-1.bsp`, %,d bytes, sha256 `%s`; `de440s.bsp`, %,d bytes,"
                        + " sha256 `%s` (both pinned).%n%n", ura.length, NEP098_SHA256,
                de440s.length, DE440S_SHA256));
        SpkKernel neptune = SpkKernel.read(ura);
        SpkKernel planets = SpkKernel.read(de440s);
        ByteBuffer words = ByteBuffer.wrap(ura).order(ByteOrder.LITTLE_ENDIAN);

        report.append("## The source segments of `nep098_part-1.bsp`\n\n")
                .append("| body | centre | target | type | frame | from (JD TDB) | to (JD TDB) |"
                        + " interval length | record words | records | name |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|\n");
        for (SpkKernel.Segment s : neptune.segments()) {
            if (s.target() != NEPTUNE && s.target() != BARYCENTRE && !isMoon(s.target())) {
                continue;
            }
            int last = s.lastAddress();
            report.append(String.format(Locale.ROOT,
                    "| %s | %d | %d | %d | %d | %.1f | %.1f | %.4f d | %d | %,d | %s |%n",
                    nameOf(s.target()), s.center(), s.target(), s.type(), s.frame(),
                    J2000_JD + s.startEt() / 86400.0, J2000_JD + s.endEt() / 86400.0,
                    word(words, last - 2) / 86400.0, (int) word(words, last - 1),
                    (int) word(words, last), s.name().strip()));
        }

        double startEt = (COVERAGE_START_JD - J2000_JD) * 86400.0;
        double endEt = (COVERAGE_END_JD - J2000_JD) * 86400.0;

        // 1. Neptune's centre about its barycentre, hourly over 1900-2100.
        double[] least = leastDelta();
        double worst = 0.0;
        double worstEt = startEt;
        double[] moonMin = new double[MOONS.length];
        double[] moonMax = new double[MOONS.length];
        java.util.Arrays.fill(moonMin, Double.MAX_VALUE);
        long hours = 0;
        for (double et = startEt; et <= endEt; et += 3600.0) {
            Vector3 c = neptune.state(BARYCENTRE, NEPTUNE, et).position();
            double r = c.length();
            if (r > worst) {
                worst = r;
                worstEt = et;
            }
            for (int k = 0; k < MOONS.length; k++) {
                double dd = neptune.state(BARYCENTRE, MOONS[k], et).position().minus(c).length();
                moonMin[k] = Math.min(moonMin[k], dd);
                moonMax[k] = Math.max(moonMax[k], dd);
            }
            hours++;
        }
        report.append(String.format(Locale.ROOT,
                "%n## Neptune's centre and the Neptune barycentre%n%n"
                        + "8 -> 899 evaluated hourly over 1900-2100 TDB (%,d epochs): the centre never"
                        + " lies more than **%.3f km** from the barycentre (largest at JD %.4f TDB) -"
                        + " **%.4f″** seen from %.3f AU, Neptune's least distance from Oslo in the weekly"
                        + " Horizons matrix (`horizons/matrix-7d-neptune-oslo.txt`, %s).%n%n"
                        + "Moon distances from Neptune's centre over the same epochs:%n%n"
                        + "| moon | least (km) | greatest (km) |%n|---|---:|---:|%n", hours, worst,
                J2000_JD + worstEt / 86400.0, Math.toDegrees(worst / (least[0] * AU_KM)) * 3600.0,
                least[0], LEAST_AT[0]));
        for (int k = 0; k < MOONS.length; k++) {
            report.append(String.format(Locale.ROOT, "| %s | %,.1f | %,.1f |%n", nameOf(MOONS[k]),
                    moonMin[k], moonMax[k]));
        }

        // 2. nep098's barycentre (DE442) against the released DE440's.
        double bary = 0.0;
        double baryEt = startEt;
        int baryStates = 0;
        boolean hasBarycentre = !neptune.segments(0, BARYCENTRE).isEmpty();
        if (hasBarycentre) {
            for (double et = startEt; et <= endEt; et += 1.7 * 86400.0) {
                Vector3 a = neptune.state(0, BARYCENTRE, et).position();
                Vector3 b = planets.state(0, BARYCENTRE, et).position();
                double d = a.minus(b).length();
                if (d > bary) {
                    bary = d;
                    baryEt = et;
                }
                baryStates++;
            }
            report.append(String.format(Locale.ROOT,
                    "%nnep098's own Neptune barycentre (0 -> 8, from DE442) against de440s's, %,d"
                            + " states on a 1.7-day grid over 1900-2100: worst position difference"
                            + " **%.3f km** (JD %.1f TDB).%n", baryStates, bary,
                    J2000_JD + baryEt / 86400.0));
        } else {
            report.append("\nnep098_part-1 carries no 0 -> 8 segment readable here.\n");
        }

        // 3. The candidate kernels, cut and proved.
        double cutStart = (COVERAGE_START_JD - MARGIN_DAYS - J2000_JD) * 86400.0;
        double cutEnd = (COVERAGE_END_JD + MARGIN_DAYS - J2000_JD) * 86400.0;
        double moons2000 = (MOONS_2000_JD - MARGIN_DAYS - J2000_JD) * 86400.0;
        report.append("\n## The candidate kernels, cut\n\n"
                + "Each is cut with the released `SpkExcerpt` and proved against its source on a"
                + " 1.7-day grid and at both ends, with the released packs' 31-day margin at each"
                + " end.\n\n"
                + "| kernel | source | bodies | from (JD) | to (JD) | bytes | gzip -9 bytes | proof |\n"
                + "|---|---|---|---:|---:|---:|---:|---|\n");
        cut(report, "Neptune barycentre, 1900-2100", "de440s", de440s, planets,
                List.of(new SpkExcerpt.Pick(0, BARYCENTRE)), cutStart, cutEnd);
        cut(report, "Neptune barycentre (DE442), 1900-2100", "nep098_part-1", ura, neptune,
                List.of(new SpkExcerpt.Pick(0, BARYCENTRE)), cutStart, cutEnd);
        cut(report, "Neptune centre, 1900-2100", "nep098_part-1", ura, neptune,
                List.of(new SpkExcerpt.Pick(BARYCENTRE, NEPTUNE)), cutStart, cutEnd);
        List<SpkExcerpt.Pick> two = List.of(new SpkExcerpt.Pick(BARYCENTRE, 801),
                new SpkExcerpt.Pick(BARYCENTRE, 802));
        cut(report, "Triton and Nereid, 1900-2100", "nep098_part-1", ura, neptune, two, cutStart, cutEnd);
        cut(report, "Triton and Nereid, 2000-2100", "nep098_part-1", ura, neptune, two, moons2000, cutEnd);
        for (int m : MOONS) {
            cut(report, nameOf(m) + ", 1900-2100", "nep098_part-1", ura, neptune,
                    List.of(new SpkExcerpt.Pick(BARYCENTRE, m)), cutStart, cutEnd);
            cut(report, nameOf(m) + ", 2000-2100", "nep098_part-1", ura, neptune,
                    List.of(new SpkExcerpt.Pick(BARYCENTRE, m)), moons2000, cutEnd);
        }
        System.out.print(report);
    }

    private static final String[] LEAST_AT = {""};

    /** Neptune's least delta (AU) in the committed weekly Oslo matrix, and its row's date. */
    private static double[] leastDelta() throws IOException {
        List<String> lines = Files.readAllLines(MATRIX);
        int column = -1;
        boolean table = false;
        double least = Double.MAX_VALUE;
        for (String line : lines) {
            if (line.strip().startsWith("Date__(UT)")) {
                String[] h = line.split(",");
                for (int i = 0; i < h.length; i++) {
                    if (h[i].strip().equals("delta")) {
                        column = i;
                    }
                }
            } else if (line.startsWith("$$SOE")) {
                table = true;
            } else if (line.startsWith("$$EOE")) {
                table = false;
            } else if (table && column >= 0) {
                String[] f = line.split(",");
                double d = Double.parseDouble(f[column].strip());
                if (d < least) {
                    least = d;
                    LEAST_AT[0] = f[0].strip().substring(0, 11);
                }
            }
        }
        if (least == Double.MAX_VALUE) {
            throw new IllegalStateException(MATRIX + ": no delta column read");
        }
        return new double[] {least};
    }

    private static boolean isMoon(int target) {
        for (int m : MOONS) {
            if (m == target) {
                return true;
            }
        }
        return false;
    }

    private static void cut(StringBuilder report, String label, String sourceName, byte[] source,
                            SpkKernel sourceKernel, List<SpkExcerpt.Pick> picks,
                            double startEt, double endEt) throws Exception {
        byte[] excerpt = SpkExcerpt.of(source, picks, startEt, endEt,
                "JURANOMETRIA NEPTUNE STUDY", "Study excerpt (#654), not released.\n");
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
            case 8 -> "Neptune barycentre";
            case 899 -> "Neptune";
            case 801 -> "Triton";
            case 802 -> "Nereid";
            case 803 -> "Naiad";
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
