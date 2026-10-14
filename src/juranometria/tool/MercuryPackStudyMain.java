package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;

/**
 * The #528 pack study: what Mercury costs in bytes, written exactly as
 * the released planets-pack builder would write it, and what the
 * released packs have to do with it - nothing, proved by digest.
 *
 * <p>Mercury is already in the released pack's own source,
 * {@code de440s.bsp}: segment 0 → 1 (the Mercury barycentre about the
 * solar-system barycentre) and 1 → 199 (Mercury about its barycentre,
 * identically zero: Mercury has no moon). The study cuts the kernel
 * the builder would add to the {@code planets} pack at its next
 * version - the builder's own interval, margins, internal name and
 * comment, through its own helpers - into the study directory, never
 * into the pack, proves every state bit-identical to the source on the
 * builder's grid, and reads Mercury's radii from the pinned PCK as the
 * builder would. Nothing under {@code src/resources} is written.
 *
 * <p>Inputs: the pinned {@code de440s.bsp} and {@code pck00011.tpc}
 * (gitignored downloads) and the committed packs on the classpath.
 * Output: {@code build/mercury-pack-study/} and the report on standard
 * output ({@code make mercury-pack-study}).
 */
public final class MercuryPackStudyMain {

    private static final Path DE440S = Path.of("imports/raw/solar-system/de440s.bsp");
    private static final Path PCK = Path.of("imports/raw/jovian-system/pck00011.tpc");
    private static final Path PLANETS = Path.of("src/resources/planets");
    private static final Path OUT = Path.of("build/mercury-pack-study");

    /**
     * Mercury as the builder's next entry would state it - since #529,
     * the builder's own entry: the study measured the file the builder
     * then added, byte for byte.
     */
    static final PlanetsPackMain.Planet MERCURY = PlanetsPackMain.PLANETS.stream()
            .filter(p -> p.key().equals("mercury")).findFirst().orElseThrow();

    private MercuryPackStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        StringBuilder r = new StringBuilder();
        r.append("# Mercury pack study (#528): the excerpt, measured\n\n");
        byte[] source = Files.readAllBytes(DE440S);
        SpkKernel full = SpkKernel.read(source);
        r.append(String.format(Locale.ROOT, "source de440s.bsp: %,d bytes, sha256 %s%n%n",
                source.length, sha256(source)));
        r.append("Mercury in the source (type, coverage in JD TDB):\n\n"
                + "| segment | centre | target | type | from | to |\n|---|---|---|---|---|---|\n");
        for (SpkKernel.Segment s : full.segments()) {
            if (s.target() == 1 || s.target() == 199) {
                r.append(String.format(Locale.ROOT, "| %s | %d | %d | %d | %.1f | %.1f |%n",
                        s.name(), s.center(), s.target(), s.type(),
                        2451545.0 + s.startEt() / 86400.0, 2451545.0 + s.endEt() / 86400.0));
            }
        }
        double startEt = (PlanetsPackMain.START_JD - PlanetsPackMain.MARGIN_DAYS - 2451545.0) * 86400.0;
        double endEt = (PlanetsPackMain.END_JD + PlanetsPackMain.MARGIN_DAYS - 2451545.0) * 86400.0;

        r.append("\n## The Mercury segments, cut alone over the released interval\n\n");
        r.append("| segment | bytes | gzip -9 | sha256 |\n|---|---:|---:|---|\n");
        for (SpkExcerpt.Pick pick : MERCURY.picks()) {
            byte[] alone = SpkExcerpt.of(source, List.of(pick), startEt, endEt,
                    "JURANOMETRIA DE440 " + pick.center() + " " + pick.target() + " STUDY",
                    "Study excerpt (#528), not released.\n");
            Files.write(OUT.resolve("mercury-" + pick.center() + "-" + pick.target() + "-study.bsp"), alone);
            r.append(String.format(Locale.ROOT, "| %d -> %d | %,d | %,d | `%s` |%n",
                    pick.center(), pick.target(), alone.length, gzipSize(alone), sha256(alone)));
        }
        r.append("\n- 1 -> 199 is identically zero: ")
                .append(PlanetsPackMain.zero(full, 1, 199)).append('\n');

        r.append("\n## The kernel the builder would add (planets pack version "
                + MERCURY.version() + ")\n\n");
        byte[] kernel = SpkExcerpt.of(source, MERCURY.picks(), startEt, endEt,
                "JURANOMETRIA DE440 MERCURY 1900-2100 V" + MERCURY.version(),
                PlanetsPackMain.comment(MERCURY, full.comments()));
        Files.write(OUT.resolve(MERCURY.kernelName()), kernel);
        r.append(String.format(Locale.ROOT, "- `%s`: %,d bytes (%,d gzip -9), sha256 `%s`, written to `%s`%n",
                MERCURY.kernelName(), kernel.length, gzipSize(kernel), sha256(kernel), OUT));
        r.append("- ").append(PlanetsPackMain.prove(SpkKernel.read(kernel), full, MERCURY)).append('\n');
        r.append("- the builder's own comment, interval, margins and grid; the internal name and the"
                + " comment's version line say V" + MERCURY.version() + ".\n");

        r.append("\n## The radii, read from the pinned PCK as the builder reads them\n\n");
        String pck = Files.readString(PCK, StandardCharsets.US_ASCII);
        byte[] pckBytes = pck.getBytes(StandardCharsets.US_ASCII);
        Map<String, String> radii = PlanetsPackMain.PinnedPck.values(pck, List.of("BODY199_RADII", "BODY299_RADII", "BODY10_RADII"));
        r.append(String.format(Locale.ROOT, "pck00011.tpc: %,d bytes, sha256 %s%n%n", pckBytes.length, sha256(pckBytes)));
        for (Map.Entry<String, String> e : radii.entrySet()) {
            r.append("- `").append(e.getKey()).append("` = ").append(e.getValue()).append('\n');
        }
        r.append("\nMercury's figure is triaxial in the PCK (equatorial 2440.53, 2440.53, polar 2438.26 km);"
                + " Venus's and the Sun's are spheres. Which radius the apparent diameter is computed from is"
                + " a question for the ruling; the authority comparison measures each against Horizons.\n");

        r.append("\n## The released packs, untouched\n\n");
        r.append("The study writes nothing under `src/resources`. The planets pack as committed:\n\n");
        r.append("| file | bytes | sha256 |\n|---|---:|---|\n");
        long packBytes = 0;
        try (var files = Files.list(PLANETS)) {
            for (Path p : files.sorted().toList()) {
                byte[] b = Files.readAllBytes(p);
                packBytes += b.length;
                r.append(String.format(Locale.ROOT, "| `%s` | %,d | `%s` |%n", p.getFileName(), b.length, sha256(b)));
            }
        }
        r.append(String.format(Locale.ROOT, "%nThe pack is %,d bytes; with Mercury's kernel it would be %,d"
                + " (its notice, provenance and manifest growing by their Mercury lines). The released Solar"
                + " System kernel's digest is `%s` and the Jovian kernels' are held by the builder"
                + " (`PlanetsPackMain.RELEASED_*`), which refuses to write if any has moved; the Venus"
                + " file above is the one the next version keeps byte for byte.%n",
                packBytes, packBytes + kernel.length, PlanetsPackMain.RELEASED_SOLAR_KERNEL_SHA256));
        for (Map.Entry<String, String> k : PlanetsPackMain.RELEASED_JOVIAN_KERNELS.entrySet()) {
            r.append("- `").append(k.getKey()).append("`: `").append(k.getValue()).append("`\n");
        }
        Files.writeString(OUT.resolve("report.md"), r, StandardCharsets.UTF_8);
        System.out.print(r);
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
