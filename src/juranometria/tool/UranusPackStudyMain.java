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
 * The #633 pack study: what Uranus and its five major moons would cost as
 * bounded excerpts, before the owner rules the authority, the interval or
 * which moons ship - Mars's study (#601) for Uranus.
 *
 * <p>Measured, not estimated. The sources are NAIF's
 * {@code ura184_part-3.bsp} (Uranus and Miranda, Ariel, Umbriel, Titania
 * and Oberon about the Uranus barycentre, URA182.21, 1600-2399) and the
 * released {@code de440s.bsp} (the Uranus barycentre). The study answers
 * from the files:
 * <ul>
 *   <li>how far Uranus's centre (799) lies from the Uranus barycentre (7)
 *       over the atlas's years, which decides whether the planet needs the
 *       satellite kernel at all;</li>
 *   <li>how far ura184's own barycentre (DE442) lies from the released
 *       DE440's;</li>
 *   <li>what each candidate excerpt costs, cut with the released writer
 *       and proved against its source.</li>
 * </ul>
 *
 * <p>Inputs: gitignored downloads, approved on #633 and pinned by digest.
 * Output: the study excerpts under {@code build/uranus-pack-study/} and the
 * report on stdout. Study tooling only: nothing in the application reads
 * this.
 */
public final class UranusPackStudyMain {

    private static final Path URA184 = Path.of("imports/raw/uranus-system/ura184_part-3.bsp");
    private static final String URA184_SHA256 =
            "273cd4ccc470d1098562cd4e94fa48fc2b4c6b1c2896308e473c949b87937c53";
    private static final Path DE440S = Path.of("imports/raw/solar-system/de440s.bsp");
    private static final String DE440S_SHA256 =
            "c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2";
    private static final Path OUT = Path.of("build/uranus-pack-study");

    /** The released packs' interval and margin (SolarSystemPackMain). */
    private static final double COVERAGE_START_JD = 2415020.5; // 1900-01-01 00:00 TDB
    private static final double COVERAGE_END_JD = 2488434.5;   // 2101-01-01 00:00 TDB
    private static final double MOONS_2000_JD = 2451544.5;     // 2000-01-01 00:00 TDB
    private static final double MARGIN_DAYS = 31.0;
    private static final double J2000_JD = 2451545.0;

    private static final int BARYCENTRE = 7;
    private static final int URANUS = 799;
    /** Outward from Uranus: Miranda, Ariel, Umbriel, Titania, Oberon. */
    private static final int[] MOONS = {705, 701, 702, 703, 704};

    private UranusPackStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        StringBuilder report = new StringBuilder();
        report.append("# Uranus pack study (#633): the excerpts, measured\n\n");

        byte[] ura = pinned(URA184, URA184_SHA256);
        byte[] de440s = pinned(DE440S, DE440S_SHA256);
        report.append(String.format(Locale.ROOT,
                "Sources: `ura184_part-3.bsp`, %,d bytes, sha256 `%s`; `de440s.bsp`, %,d bytes,"
                        + " sha256 `%s` (both pinned).%n%n", ura.length, URA184_SHA256,
                de440s.length, DE440S_SHA256));
        SpkKernel uranus = SpkKernel.read(ura);
        SpkKernel planets = SpkKernel.read(de440s);
        ByteBuffer words = ByteBuffer.wrap(ura).order(ByteOrder.LITTLE_ENDIAN);

        report.append("## The source segments of `ura184_part-3.bsp` for the six bodies\n\n")
                .append("| body | centre | target | type | frame | from (JD TDB) | to (JD TDB) |"
                        + " interval length | record words | records | name |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|\n");
        for (SpkKernel.Segment s : uranus.segments()) {
            if (s.target() != URANUS && s.target() != BARYCENTRE && !isMoon(s.target())) {
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

        // 1. Uranus's centre about its barycentre, hourly over 1900-2100.
        double worst = 0.0;
        double worstEt = startEt;
        double[] moonMin = new double[MOONS.length];
        double[] moonMax = new double[MOONS.length];
        java.util.Arrays.fill(moonMin, Double.MAX_VALUE);
        long hours = 0;
        for (double et = startEt; et <= endEt; et += 3600.0) {
            Vector3 c = uranus.state(BARYCENTRE, URANUS, et).position();
            double r = c.length();
            if (r > worst) {
                worst = r;
                worstEt = et;
            }
            for (int k = 0; k < MOONS.length; k++) {
                double dd = uranus.state(BARYCENTRE, MOONS[k], et).position().minus(c).length();
                moonMin[k] = Math.min(moonMin[k], dd);
                moonMax[k] = Math.max(moonMax[k], dd);
            }
            hours++;
        }
        report.append(String.format(Locale.ROOT,
                "%n## Uranus's centre and the Uranus barycentre%n%n"
                        + "7 -> 799 evaluated hourly over 1900-2100 TDB (%,d epochs): the centre never"
                        + " lies more than **%.3f km** from the barycentre (largest at JD %.4f TDB).%n%n"
                        + "Moon distances from Uranus's centre over the same epochs:%n%n"
                        + "| moon | least (km) | greatest (km) |%n|---|---:|---:|%n", hours, worst,
                J2000_JD + worstEt / 86400.0));
        for (int k = 0; k < MOONS.length; k++) {
            report.append(String.format(Locale.ROOT, "| %s | %,.1f | %,.1f |%n", nameOf(MOONS[k]),
                    moonMin[k], moonMax[k]));
        }

        // 2. ura184's barycentre (DE442) against the released DE440's.
        double bary = 0.0;
        double baryEt = startEt;
        int baryStates = 0;
        boolean hasBarycentre = !uranus.segments(0, BARYCENTRE).isEmpty();
        if (hasBarycentre) {
            for (double et = startEt; et <= endEt; et += 1.7 * 86400.0) {
                Vector3 a = uranus.state(0, BARYCENTRE, et).position();
                Vector3 b = planets.state(0, BARYCENTRE, et).position();
                double d = a.minus(b).length();
                if (d > bary) {
                    bary = d;
                    baryEt = et;
                }
                baryStates++;
            }
            report.append(String.format(Locale.ROOT,
                    "%nura184's own Uranus barycentre (0 -> 7, from DE442) against de440s's, %,d"
                            + " states on a 1.7-day grid over 1900-2100: worst position difference"
                            + " **%.3f km** (JD %.1f TDB).%n", baryStates, bary,
                    J2000_JD + baryEt / 86400.0));
        } else {
            report.append("\nura184_part-3 carries no 0 -> 7 segment readable here.\n");
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
        cut(report, "Uranus barycentre, 1900-2100", "de440s", de440s, planets,
                List.of(new SpkExcerpt.Pick(0, BARYCENTRE)), cutStart, cutEnd);
        cut(report, "Uranus barycentre (DE442), 1900-2100", "ura184_part-3", ura, uranus,
                List.of(new SpkExcerpt.Pick(0, BARYCENTRE)), cutStart, cutEnd);
        cut(report, "Uranus centre, 1900-2100", "ura184_part-3", ura, uranus,
                List.of(new SpkExcerpt.Pick(BARYCENTRE, URANUS)), cutStart, cutEnd);
        List<SpkExcerpt.Pick> five = new ArrayList<>();
        for (int m : MOONS) {
            five.add(new SpkExcerpt.Pick(BARYCENTRE, m));
        }
        cut(report, "The five moons, 1900-2100", "ura184_part-3", ura, uranus, five, cutStart, cutEnd);
        cut(report, "The five moons, 2000-2100", "ura184_part-3", ura, uranus, five, moons2000, cutEnd);
        for (int m : MOONS) {
            cut(report, nameOf(m) + ", 1900-2100", "ura184_part-3", ura, uranus,
                    List.of(new SpkExcerpt.Pick(BARYCENTRE, m)), cutStart, cutEnd);
        }
        List<SpkExcerpt.Pick> four = new ArrayList<>();
        for (int m : MOONS) {
            if (m != 705) {
                four.add(new SpkExcerpt.Pick(BARYCENTRE, m));
            }
        }
        cut(report, "The four without Miranda, 1900-2100", "ura184_part-3", ura, uranus, four,
                cutStart, cutEnd);
        cut(report, "The four without Miranda, 2000-2100", "ura184_part-3", ura, uranus, four,
                moons2000, cutEnd);
        System.out.print(report);
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
                "JURANOMETRIA URANUS STUDY", "Study excerpt (#633), not released.\n");
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
            case 7 -> "Uranus barycentre";
            case 799 -> "Uranus";
            case 705 -> "Miranda";
            case 701 -> "Ariel";
            case 702 -> "Umbriel";
            case 703 -> "Titania";
            case 704 -> "Oberon";
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
