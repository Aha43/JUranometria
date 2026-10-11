package juranometria.tool;

import java.io.IOException;
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
 * The #500 pack study: what Venus costs in bytes, and what the released
 * Sun, Earth, Moon and Jovian states have to do with it.
 *
 * <p>Measured, not estimated. Venus is already in the released pack's
 * own source, {@code de440s.bsp}: segment 0 → 2 (the Venus barycentre
 * about the solar-system barycentre) and segment 2 → 299 (Venus about
 * its barycentre, which carries no moon and so is identically zero).
 * Both are cut over the released interval with the released excerpt
 * writer and proved bit-identical to the source; the 2 → 299 segment
 * is also proved zero at every sampled epoch. The two candidate layouts
 * are then built as files, not sized from directories: <b>A</b>, a
 * separate planetary kernel beside the released packs, and <b>B</b>, a
 * regenerated Solar System kernel of six segments. In B the four
 * released segments are proved to give the released kernel's own
 * states bit for bit, but the file is new; in A the released files are
 * untouched, which their digests show. Both layouts leave the Jovian
 * pack's files as released.
 *
 * <p>Layout C, asked for by the owner's ruling on #500: one logical
 * {@code planets} pack over the whole road - Venus, then Mercury, Mars,
 * Saturn, Uranus, Neptune and Pluto - measured both as a kernel file
 * per planet and as one kernel regenerated at each release, with the
 * per-planet packs (D) beside them: sizes, bit identity on
 * regeneration, file stability and the bytes each release changes.
 *
 * <p>Inputs: the released pack's pinned {@code de440s.bsp} (gitignored
 * download) and the committed packs. Output: the study excerpts under
 * {@code build/venus-pack-study/} and a report on stdout. Study
 * tooling only: nothing in the application reads this.
 */
public final class VenusPackStudyMain {

    private static final Path DE440S = Path.of("imports/raw/solar-system/de440s.bsp");
    private static final Path RELEASED = Path.of(
            "src/resources/solar-system/juranometria-de440-sun-emb-earth-moon-1900-2100.bsp");
    private static final Path JOVIAN = Path.of("src/resources/jovian-system");
    private static final Path OUT = Path.of("build/venus-pack-study");

    /** The released pack's interval and margin (SolarSystemPackMain). */
    private static final double COVERAGE_START_JD = 2415020.5; // 1900-01-01 00:00 TDB
    private static final double COVERAGE_END_JD = 2488434.5;   // 2101-01-01 00:00 TDB
    private static final double MARGIN_DAYS = 31.0;
    private static final double J2000_JD = 2451545.0;

    private static final List<SpkExcerpt.Pick> RELEASED_PICKS = List.of(
            new SpkExcerpt.Pick(0, 3), new SpkExcerpt.Pick(0, 10),
            new SpkExcerpt.Pick(3, 399), new SpkExcerpt.Pick(3, 301));
    private static final SpkExcerpt.Pick VENUS_BARYCENTRE = new SpkExcerpt.Pick(0, 2);
    private static final SpkExcerpt.Pick VENUS = new SpkExcerpt.Pick(2, 299);

    private VenusPackStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        double startEt = (COVERAGE_START_JD - MARGIN_DAYS - J2000_JD) * 86400.0;
        double endEt = (COVERAGE_END_JD + MARGIN_DAYS - J2000_JD) * 86400.0;
        StringBuilder report = new StringBuilder();
        report.append("# Venus pack study (#500): the excerpt, measured\n\n");

        byte[] source = Files.readAllBytes(DE440S);
        SpkKernel full = SpkKernel.read(source);
        report.append(String.format(Locale.ROOT,
                "source de440s.bsp: %,d bytes, sha256 %s%n%n", source.length, sha256(source)));
        report.append("Venus in the source (type, coverage in JD TDB):\n\n"
                + "| segment | centre | target | type | from | to |\n|---|---|---|---|---|---|\n");
        for (SpkKernel.Segment s : full.segments()) {
            if (s.target() == 2 || s.target() == 299) {
                report.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %.1f | %.1f |%n",
                        s.name(), s.center(), s.target(), s.type(),
                        J2000_JD + s.startEt() / 86400.0, J2000_JD + s.endEt() / 86400.0));
            }
        }

        // Each Venus segment alone, so the cost of each is its own.
        report.append("\n## The Venus segments, cut alone over the released interval\n\n");
        report.append("| segment | bytes | gzip -9 | sha256 |\n|---|---:|---:|---|\n");
        List<String> proofs = new ArrayList<>();
        for (SpkExcerpt.Pick pick : List.of(VENUS_BARYCENTRE, VENUS)) {
            byte[] alone = SpkExcerpt.of(source, List.of(pick), startEt, endEt,
                    "JURANOMETRIA DE440 " + pick.center() + " " + pick.target() + " STUDY",
                    "Study excerpt (#500), not released.\n");
            Files.write(OUT.resolve("venus-" + pick.center() + "-" + pick.target() + "-study.bsp"), alone);
            report.append(String.format(Locale.ROOT, "| %d -> %d | %,d | %,d | `%s` |%n",
                    pick.center(), pick.target(), alone.length, gzipSize(alone), sha256(alone)));
            proofs.add(pick.center() + " -> " + pick.target() + ": "
                    + prove(full, SpkKernel.read(alone), List.of(pick), startEt, endEt));
        }
        report.append('\n');
        for (String p : proofs) {
            report.append("- ").append(p).append('\n');
        }
        report.append("- 2 -> 299 is identically zero: ").append(zero(full, startEt, endEt)).append('\n');

        // Layout A: a planetary kernel of the two Venus segments, beside the released packs.
        byte[] planetary = SpkExcerpt.of(source, List.of(VENUS_BARYCENTRE, VENUS), startEt, endEt,
                "JURANOMETRIA DE440 VENUS 1900-2100 STUDY",
                "Study excerpt (#500), layout A, not released.\n");
        Files.write(OUT.resolve("juranometria-de440-venus-1900-2100-study.bsp"), planetary);
        SpkKernel planetaryKernel = SpkKernel.read(planetary);

        // Layout B: the Solar System kernel regenerated with six segments.
        List<SpkExcerpt.Pick> six = new ArrayList<>(RELEASED_PICKS);
        six.add(VENUS_BARYCENTRE);
        six.add(VENUS);
        byte[] v3 = SpkExcerpt.of(source, six, startEt, endEt,
                "JURANOMETRIA DE440 SUN EMB EARTH MOON VENUS 1900-2100 STUDY",
                "Study excerpt (#500), layout B, not released.\n");
        Files.write(OUT.resolve("juranometria-de440-sun-emb-earth-moon-venus-1900-2100-study.bsp"), v3);
        SpkKernel v3Kernel = SpkKernel.read(v3);

        byte[] released = Files.readAllBytes(RELEASED);
        SpkKernel releasedKernel = SpkKernel.read(released);

        report.append("\n## The layouts, built\n\n");
        report.append("| layout | file | bytes | gzip -9 | sha256 |\n|---|---|---:|---:|---|\n");
        report.append(String.format(Locale.ROOT, "| released | `%s` | %,d | %,d | `%s` |%n",
                RELEASED.getFileName(), released.length, gzipSize(released), sha256(released)));
        report.append(String.format(Locale.ROOT, "| A, added beside it | `juranometria-de440-venus-1900-2100.bsp` | %,d | %,d | `%s` |%n",
                planetary.length, gzipSize(planetary), sha256(planetary)));
        report.append(String.format(Locale.ROOT, "| B, replacing it | `juranometria-de440-sun-emb-earth-moon-venus-1900-2100.bsp` | %,d | %,d | `%s` |%n",
                v3.length, gzipSize(v3), sha256(v3)));
        report.append(String.format(Locale.ROOT,
                "%nAdded to a download: A %,d bytes (%,d compressed); B %,d bytes (%,d compressed).%n",
                planetary.length, gzipSize(planetary), v3.length - released.length,
                gzipSize(v3) - gzipSize(released)));

        report.append("\n## Invariance\n\n");
        report.append("- A, Venus against de440s: ").append(prove(full, planetaryKernel,
                List.of(VENUS_BARYCENTRE, VENUS), startEt, endEt)).append('\n');
        report.append("- B, all six segments against de440s: ").append(prove(full, v3Kernel, six, startEt, endEt)).append('\n');
        report.append("- B, the four released segments against the released kernel: ")
                .append(prove(releasedKernel, v3Kernel, RELEASED_PICKS, startEt, endEt)).append('\n');
        report.append("- B, its file against the released file: ")
                .append(java.util.Arrays.equals(v3, released) ? "identical" : "different bytes (new name, comment and directory)")
                .append('\n');
        report.append(String.format(Locale.ROOT,
                "- A, the released Solar System kernel is not rewritten: sha256 %s, as committed.%n", sha256(released)));
        report.append("- Both layouts, the Jovian pack, as committed:\n");
        try (var files = Files.list(JOVIAN)) {
            for (Path p : files.sorted().toList()) {
                byte[] b = Files.readAllBytes(p);
                report.append(String.format(Locale.ROOT, "  - `%s`: %,d bytes, sha256 %s%n",
                        p.getFileName(), b.length, sha256(b)));
            }
        }
        report.append("\nThe Sun, Earth and Moon (and so every Sun, Moon and Jovian answer, whose"
                + " Earth and Sun come from the released kernel) are the released bytes in A; in B they"
                + " are the same coefficients in a new file, held bit for bit above.\n");

        layoutC(report, source, full, startEt, endEt, released);

        Files.writeString(OUT.resolve("report.md"), report, StandardCharsets.UTF_8);
        System.out.print(report);
    }

    /** Every kept segment on a 1.7-day grid plus its end, required identical to the bit. */
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
        return String.format(Locale.ROOT, "%,d states on a 1.7-day grid, worst difference %s",
                states, worst == 0.0 ? "0 (bit-identical)" : worst + " km - NOT IDENTICAL");
    }

    private static String zero(SpkKernel kernel, double startEt, double endEt) {
        return zero(kernel, VENUS, startEt, endEt);
    }

    private static String zero(SpkKernel kernel, SpkExcerpt.Pick pick, double startEt, double endEt) {
        int states = 0;
        double worst = 0.0;
        for (double et = startEt; et <= endEt; et += 1.7 * 86400.0) {
            SpkKernel.State s = kernel.state(pick.center(), pick.target(), et);
            worst = Math.max(worst, Math.max(s.position().length(), s.velocity().length()));
            states++;
        }
        return String.format(Locale.ROOT, "%,d states, largest position or velocity %s", states,
                worst == 0.0 ? "0" : Double.toString(worst));
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

    // ---- Layout C: one logical planets pack, measured over the whole road -------

    /** A planet of the road, its de440s segments, and where its centre is. */
    private record Planet(String name, List<SpkExcerpt.Pick> picks, String centre) {
    }

    /**
     * The road's planets in the order the owner named them (#500, ruling 2):
     * Venus now, then Mercury, Mars, Saturn, Uranus, Neptune and Pluto across
     * later 5.x releases; Jupiter is the Jovian pack's. de440s carries every
     * barycentre, and a planet's own centre only for Mercury and Venus
     * (1 -> 199 and 2 -> 299, identically zero). The other centres are in
     * NAIF's satellite kernels, as NAIF's {@code aa_summaries.txt} lists
     * them; they are named here and not measured.
     */
    private static final List<Planet> ROAD = List.of(
            new Planet("venus", List.of(new SpkExcerpt.Pick(0, 2), new SpkExcerpt.Pick(2, 299)),
                    "in de440s (2 -> 299, zero)"),
            new Planet("mercury", List.of(new SpkExcerpt.Pick(0, 1), new SpkExcerpt.Pick(1, 199)),
                    "in de440s (1 -> 199, zero)"),
            new Planet("mars", List.of(new SpkExcerpt.Pick(0, 4)), "499 about 4: a Mars satellite kernel"),
            new Planet("saturn", List.of(new SpkExcerpt.Pick(0, 6)), "699 about 6: a Saturn satellite kernel"),
            new Planet("uranus", List.of(new SpkExcerpt.Pick(0, 7)), "799 about 7: a Uranus satellite kernel"),
            new Planet("neptune", List.of(new SpkExcerpt.Pick(0, 8)), "899 about 8: a Neptune satellite kernel"),
            new Planet("pluto", List.of(new SpkExcerpt.Pick(0, 9)), "999 about 9: a Pluto satellite kernel"));

    /** The released builder's comment shape: its own header, then de440s's comment area. */
    private static String comment(String header, String sourceComments) {
        StringBuilder text = new StringBuilder(header);
        text.append("\n---- de440s.bsp comment area, as distributed by NAIF ----\n");
        for (String line : sourceComments.split("\n", -1)) {
            StringBuilder printable = new StringBuilder();
            for (char c : line.toCharArray()) {
                printable.append(c >= 0x20 && c <= 0x7e ? c : '?');
            }
            text.append(printable).append('\n');
        }
        return text.append("---- end of the source kernel's comment area ----").toString();
    }

    private static byte[] kernel(byte[] source, SpkKernel full, List<SpkExcerpt.Pick> picks, String label,
                                 double startEt, double endEt) throws IOException {
        return SpkExcerpt.of(source, picks, startEt, endEt,
                "JURANOMETRIA DE440 " + label.toUpperCase(Locale.ROOT) + " 1900-2100 STUDY",
                comment("JUranometria planets pack, study excerpt (#500): " + label
                        + ".\nMODIFIED by JUranometria; not an original JPL/NAIF file.\n", full.comments()));
    }

    /** The text files a pack carries beside its kernels, measured on the released Jovian pack. */
    private static long packText() throws IOException {
        long bytes = 0;
        for (String name : List.of("NOTICE-jovian-system.md", "PROVENANCE.md", "manifest.properties")) {
            bytes += Files.size(JOVIAN.resolve(name));
        }
        return bytes;
    }

    private static void layoutC(StringBuilder report, byte[] source, SpkKernel full, double startEt,
                                double endEt, byte[] released) throws Exception {
        report.append("\n## Layout C: one logical `planets` pack over the whole road (#500, ruling 2)\n\n");
        report.append("The road's planets in the order the owner named them - Venus now, then Mercury, Mars,"
                + " Saturn, Uranus, Neptune\nand Pluto across later 5.x releases; Jupiter stays the Jovian"
                + " pack's. Every kernel carries the released builder's\ncomment area (its own header and"
                + " de440s's comments), so a size here is a size it would ship at.\n\n");

        // C1: one logical pack, one kernel file per planet.
        report.append("### Each planet's kernel, cut alone over 1900-2100\n\n");
        report.append("| planet | segments | bytes | gzip -9 | sha256 | cut again | centre |\n"
                + "|---|---|---:|---:|---|---|---|\n");
        List<byte[]> members = new ArrayList<>();
        List<String> proofs = new ArrayList<>();
        for (Planet p : ROAD) {
            byte[] once = kernel(source, full, p.picks(), p.name(), startEt, endEt);
            byte[] again = kernel(source, full, p.picks(), p.name(), startEt, endEt);
            members.add(once);
            Files.write(OUT.resolve("juranometria-de440-" + p.name() + "-1900-2100-study.bsp"), once);
            StringBuilder segs = new StringBuilder();
            for (SpkExcerpt.Pick pick : p.picks()) {
                segs.append(segs.length() == 0 ? "" : ", ").append(pick.center()).append(" -> ").append(pick.target());
            }
            report.append(String.format(Locale.ROOT, "| %s | %s | %,d | %,d | `%s` | %s | %s |%n",
                    p.name(), segs, once.length, gzipSize(once), sha256(once),
                    java.util.Arrays.equals(once, again) ? "byte-identical" : "DIFFERENT", p.centre()));
            proofs.add(p.name() + " against de440s: " + prove(full, SpkKernel.read(once), p.picks(), startEt, endEt));
        }
        report.append('\n');
        for (String proof : proofs) {
            report.append("- ").append(proof).append('\n');
        }
        report.append("- 1 -> 199 is identically zero, as 2 -> 299 is: ")
                .append(zero(full, new SpkExcerpt.Pick(1, 199), startEt, endEt)).append('\n');

        // C2: one kernel regenerated at every release.
        report.append("\n### One growing kernel, regenerated at each release\n\n");
        report.append("| release adds | planets | kernel bytes | gzip -9 | sha256 | earlier planets against the previous release's kernel |\n"
                + "|---|---:|---:|---:|---|---|\n");
        List<SpkExcerpt.Pick> grown = new ArrayList<>();
        SpkKernel previous = null;
        List<SpkExcerpt.Pick> previousPicks = new ArrayList<>();
        long growingChurn = 0;
        byte[] last = null;
        for (int i = 0; i < ROAD.size(); i++) {
            grown.addAll(ROAD.get(i).picks());
            byte[] k = kernel(source, full, grown, "planets " + (i + 1), startEt, endEt);
            SpkKernel kk = SpkKernel.read(k);
            growingChurn += k.length;
            String held = previous == null ? "-" : prove(previous, kk, previousPicks, startEt, endEt);
            report.append(String.format(Locale.ROOT, "| %s | %d | %,d | %,d | `%s` | %s |%n",
                    ROAD.get(i).name(), i + 1, k.length, gzipSize(k), sha256(k), held));
            previous = kk;
            previousPicks = new ArrayList<>(grown);
            last = k;
        }
        Files.write(OUT.resolve("juranometria-de440-planets-1900-2100-study.bsp"), last);

        // The comparison.
        long text = packText();
        long memberTotal = 0;
        long memberGzip = 0;
        long memberChurn = 0;
        long memberFiles = 0;
        for (byte[] m : members) {
            memberTotal += m.length;
            memberGzip += gzipSize(m);
        }
        report.append(String.format(Locale.ROOT, "%n### The three layouts over the seven releases%n%n"
                + "A pack's notice, provenance and manifest are measured on the released Jovian pack: %,d bytes"
                + " together.%n%n", text));
        report.append("| layout | files when complete | bytes when complete (gzip -9) | a planet's kernel when a later one is added |"
                + " bytes changed per release (kernel + text) | kernel bytes committed over the road |\n"
                + "|---|---|---|---|---|---|\n");
        StringBuilder perRelease = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            memberChurn += members.get(i).length;
            perRelease.append(i == 0 ? "" : ", ").append(String.format(Locale.ROOT, "%,d", members.get(i).length));
        }
        memberFiles = members.size();
        report.append(String.format(Locale.ROOT,
                "| **C, one logical pack, a kernel file per planet** | %d kernels + 3 text | %,d (%,d) |"
                        + " the same file, same name, same digest | one new kernel (%s) + the 3 text files rewritten |"
                        + " %,d |%n",
                memberFiles, memberTotal + text, memberGzip, perRelease, memberChurn));
        report.append(String.format(Locale.ROOT,
                "| C', one logical pack, one growing kernel | 1 kernel + 3 text | %,d (%,d) |"
                        + " the same states (proved above), a new file: new bytes, new digest |"
                        + " the whole kernel again + the 3 text files | %,d |%n",
                last.length + text, gzipSize(last), growingChurn));
        report.append(String.format(Locale.ROOT,
                "| D, a pack per planet | %d kernels + %d text | %,d (%,d) |"
                        + " the same file, untouched | one new kernel + 3 new text files; no earlier pack touched |"
                        + " %,d |%n",
                memberFiles, 3 * memberFiles, memberTotal + 3 * memberFiles * text, memberGzip, memberChurn));
        report.append(String.format(Locale.ROOT,
                "%nWhat is not measured: the planets' own centres for Mars, Saturn, Uranus, Neptune and Pluto"
                        + " (499, 699, 799, 899, 999),%nwhich are in NAIF's satellite kernels, not in de440s;"
                        + " each layout would add them as further segments\n(C, D: in that planet's own file)."
                        + " Measuring them needs those kernels, which are not downloaded.%n%n"
                        + "The released Solar System kernel is untouched by every layout here: sha256 %s.%n",
                sha256(released)));
    }

}
