package juranometria.tool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import juranometria.solar.SolarSystemPack;
import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;

/**
 * Builds the bundled Solar System ephemeris pack from its pinned raw
 * inputs (Sprint 35, issue #399; the Moon added in Sprint 36, issue
 * #407): a JUranometria-modified excerpt of JPL's DE440 for the Sun,
 * the Earth–Moon barycentre, the Earth and the Moon over 1900–2100,
 * and the IERS leap-second file unmodified, with the manifest, notice
 * and provenance the atlas keeps beside bundled data.
 *
 * <p>Deterministic by construction: no timestamps other than the
 * literal audit date, fixed {@code Locale.ROOT} formatting, and an
 * excerpt whose bytes follow from the source bytes, the segments and
 * the interval alone. Running it twice writes the same pack, so the
 * committed pack is proved by rebuilding it.
 *
 * <p>The kernel is a <em>modified</em> kernel in NAIF's sense and is
 * treated as one (issue #398, R-a): a name of its own, JUranometria
 * named as the modifier in the comment area, the source's identity and
 * acknowledgement retained as source attribution, and every fact of
 * the modification in {@code PROVENANCE.md}. The coefficients are the
 * source's coefficients, and this builder proves it before it writes:
 * the excerpt is evaluated against the source at thousands of epochs
 * and must agree exactly.
 *
 * <p>Run via {@code make import-solar-system}, after
 * {@code scripts/download-solar-system-sources.sh}.
 */
public final class SolarSystemPackMain {

    static final String PACK_NAME = "solar-system";
    static final int PACK_VERSION = 2;
    static final String AUDIT_DATE = "2026-09-29";
    static final String RETRIEVED = "2026-09-28";

    static final String KERNEL_NAME =
            "juranometria-de440-sun-emb-earth-moon-1900-2100.bsp";
    static final String LEAP_NAME = "Leap_Second.dat";
    static final String NOTICE_NAME = "NOTICE-solar-system.md";
    static final String PROVENANCE_NAME = "PROVENANCE.md";

    static final String DE440S_URL = "https://naif.jpl.nasa.gov/pub/naif/"
            + "generic_kernels/spk/planets/de440s.bsp";
    static final String LEAP_URL =
            "https://hpiers.obspm.fr/iers/bul/bulc/Leap_Second.dat";

    /** The raw inputs, named as upstream names them, and their digests. */
    static final Map<String, String> PINNED = Map.of(
            "de440s.bsp",
            "c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2",
            "Leap_Second.dat",
            "6cb6f5d4b819f2e568e25db4b0b26d89dedf031fdffb18bc94d40f4e94e268d7");

    /** Segments kept: centre → target, in this order. */
    static final List<SpkExcerpt.Pick> PICKS = List.of(
            new SpkExcerpt.Pick(0, 3),    // Earth–Moon barycentre from the SSB
            new SpkExcerpt.Pick(0, 10),   // Sun from the SSB
            new SpkExcerpt.Pick(3, 399),  // Earth from the Earth–Moon barycentre
            new SpkExcerpt.Pick(3, 301)); // Moon from the Earth–Moon barycentre

    /**
     * The civil interval, as TDB Julian dates, and the margin. The
     * margin is required: a civil instant of 1900-01-01 00:00 lies
     * seconds before 1900-01-01 in TDB once ΔT is applied, and the
     * kernel must cover it (measured in #398).
     */
    static final double COVERAGE_START_JD = 2415020.5; // 1900-01-01 00:00 TDB
    static final double COVERAGE_END_JD = 2488434.5;   // 2101-01-01 00:00 TDB
    static final double MARGIN_DAYS = 31.0;

    private SolarSystemPackMain() {
    }

    public static void main(String[] args) throws IOException {
        Path rawDir = Path.of(args.length > 0 ? args[0]
                : "imports/raw/solar-system");
        Path outDir = Path.of(args.length > 1 ? args[1]
                : "src/resources/solar-system");

        for (Map.Entry<String, String> pin : new TreeMap<>(PINNED).entrySet()) {
            Path file = rawDir.resolve(pin.getKey());
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("missing raw input " + file
                        + "; run scripts/download-solar-system-sources.sh");
            }
            String actual = PinnedInputs.sha256Hex(Files.readAllBytes(file));
            if (!actual.equals(pin.getValue())) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "raw input %s fails its pinned checksum: expected %s,"
                                + " found %s", file, pin.getValue(), actual));
            }
        }

        byte[] source = Files.readAllBytes(rawDir.resolve("de440s.bsp"));
        byte[] leap = Files.readAllBytes(rawDir.resolve("Leap_Second.dat"));
        SpkKernel full = SpkKernel.read(source);
        double startEt = (COVERAGE_START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;
        double endEt = (COVERAGE_END_JD + MARGIN_DAYS - 2451545.0) * 86400.0;
        byte[] kernel = SpkExcerpt.of(source, PICKS, startEt, endEt,
                "JURANOMETRIA DE440 SUN EMB EARTH MOON 1900-2100 V" + PACK_VERSION,
                comment(full.comments()));

        // Proof before writing: the excerpt says what the source says.
        SpkKernel excerpt = SpkKernel.read(kernel);
        int states = 0;
        double worst = 0.0;
        for (double jd = COVERAGE_START_JD; jd <= COVERAGE_END_JD; jd += 1.7) {
            double et = (jd - 2451545.0) * 86400.0;
            for (SpkExcerpt.Pick pick : PICKS) {
                SpkKernel.State a = excerpt.state(pick.center(), pick.target(), et);
                SpkKernel.State b = full.state(pick.center(), pick.target(), et);
                worst = Math.max(worst, a.position().minus(b.position()).length());
                worst = Math.max(worst, a.velocity().minus(b.velocity()).length());
                states++;
            }
        }
        if (worst != 0.0) {
            throw new IllegalStateException("the excerpt disagrees with its"
                    + " source by " + worst + "; nothing was written");
        }
        String validation = String.format(Locale.ROOT, "%d states over the"
                + " coverage at 1.7-day spacing, all four segments: position"
                + " and velocity identical to the source, worst difference"
                + " %.1f", states, worst);

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);
        Map<String, String> checksums = new LinkedHashMap<>();
        write(outDir.resolve(KERNEL_NAME), kernel, checksums);
        write(outDir.resolve(LEAP_NAME), leap, checksums);
        write(outDir.resolve(NOTICE_NAME),
                notice().getBytes(StandardCharsets.UTF_8), checksums);
        SpkKernel.Segment sample = excerpt.segments().get(0);
        write(outDir.resolve(PROVENANCE_NAME), provenance(
                PinnedInputs.sha256Hex(source), PinnedInputs.sha256Hex(leap),
                checksums.get(KERNEL_NAME), kernel.length, excerpt, validation)
                .getBytes(StandardCharsets.UTF_8), checksums);
        Files.writeString(outDir.resolve("manifest.properties"),
                manifest(checksums, excerpt), StandardCharsets.UTF_8);

        SolarSystemPack loaded = SolarSystemPack.load(name -> {
            try {
                return Files.newInputStream(outDir.resolve(name));
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        System.out.println("solar-system pack written to " + outDir);
        System.out.println("  " + KERNEL_NAME + ": " + kernel.length
                + " bytes, sha256 " + checksums.get(KERNEL_NAME));
        System.out.println("  " + LEAP_NAME + ": " + leap.length
                + " bytes, exact until " + loaded.leapSeconds().expires());
        System.out.println("  coverage " + sample.startEt() + " .. "
                + sample.endEt() + " s past J2000 (TDB)");
        System.out.println("  " + validation);
        System.out.println("  reloaded and verified: " + loaded.manifest()
                .get("pack.name") + " v" + loaded.manifest().get("pack.version"));
    }

    private static String comment(String sourceComments) {
        StringBuilder text = new StringBuilder();
        text.append("JUranometria Solar System pack, version ").append(PACK_VERSION)
                .append(": a subset of the JPL planetary ephemeris DE440,\n")
                .append("MODIFIED by JUranometria (https://github.com/Aha43/JUranometria).\n")
                .append("\n")
                .append("This is not an original JPL/NAIF file. It was produced by\n")
                .append("juranometria.tool.SolarSystemPackMain from the official kernel\n")
                .append("de440s.bsp, obtained from NASA/JPL NAIF at\n")
                .append("  ").append(DE440S_URL).append("\n")
                .append("(SHA-256 ").append(PINNED.get("de440s.bsp")).append(")\n")
                .append("by keeping only these SPK Type 2 segments\n")
                .append("  0 -> 3    Earth-Moon barycentre from the solar-system barycentre\n")
                .append("  0 -> 10   Sun from the solar-system barycentre\n")
                .append("  3 -> 399  Earth from the Earth-Moon barycentre\n")
                .append("  3 -> 301  Moon from the Earth-Moon barycentre\n")
                .append("over the interval 1900-01-01 to 2101-01-01 (TDB) with a margin\n")
                .append("of 31 days at each end. Every Chebyshev coefficient kept is the\n")
                .append("source's coefficient, unchanged; only whole records outside the\n")
                .append("interval were removed. Full provenance, digests and validation\n")
                .append("are in PROVENANCE.md beside this file.\n")
                .append("\n")
                .append("The ephemeris itself is DE440 by Park, Folkner, Williams and\n")
                .append("Boggs (2021), NASA/JPL. The source kernel's own comment area\n")
                .append("follows, retained as source attribution:\n")
                .append("\n")
                .append("---- de440s.bsp comment area, as distributed by NAIF ----\n");
        for (String line : sourceComments.split("\n", -1)) {
            text.append(printable(line)).append('\n');
        }
        text.append("---- end of the source kernel's comment area ----");
        return text.toString();
    }

    /** The source comments are ASCII; anything else is replaced. */
    private static String printable(String line) {
        StringBuilder out = new StringBuilder();
        for (char c : line.toCharArray()) {
            out.append(c >= 0x20 && c <= 0x7e ? c : '?');
        }
        return out.toString();
    }

    static String notice() {
        return """
            # Notice: Solar System ephemeris pack

            This pack carries a **JUranometria-modified excerpt of the JPL
            planetary ephemeris DE440** and the **IERS leap-second file**,
            so the atlas can compute where the Sun and the Moon are without
            any network access at build, test or run time.

            ## The ephemeris excerpt

            `juranometria-de440-sun-emb-earth-moon-1900-2100.bsp` was produced
            by JUranometria from the official kernel `de440s.bsp` distributed
            by NASA/JPL's Navigation and Ancillary Information Facility (NAIF).
            It keeps four of that kernel's segments - the Sun and the
            Earth-Moon barycentre relative to the solar-system barycentre,
            and the Earth and the Moon relative to the Earth-Moon barycentre -
            over the years 1900 to 2100, with every coefficient unchanged. It is a
            **modified kernel** under NAIF's rules and is named, annotated
            and attributed as such; it is not an original JPL file and JPL
            did not produce it.

            NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
            by NAIF is permitted as long as they have not been modified." "If
            a kernel distributed by NAIF has been modified in any way, any
            embedded or otherwise allied attribution of the original kernel
            producer must be replaced with the name and institution of
            whomever has made the last modification." "The file name must
            also be changed to help avoid confusion." SPICE data carry no
            fees or licensing ("Technology and Software Publicly Available").

            The ephemeris is the work of R. S. Park, W. M. Folkner,
            J. G. Williams and D. H. Boggs, *The JPL Planetary and Lunar
            Ephemerides DE440 and DE441*, The Astronomical Journal 161:105
            (2021), doi:10.3847/1538-3881/abd414. JUranometria acknowledges
            NASA/JPL NAIF and the DE440 authors as the source of these data.

            ## The leap-second file

            `Leap_Second.dat` is the IERS Earth Orientation Centre's
            (Observatoire de Paris) published record of TAI - UTC, bundled
            **unmodified**, including the validity date it states for itself.
            Its expiry is the boundary of the atlas's exact time reading, not
            a prediction about leap seconds; a later file moves that boundary
            through this pack's provenance.

            ## Provenance

            `PROVENANCE.md` beside this notice records the source URLs,
            retrieval date, upstream digests, the exact extraction and its
            validation, the output digest and the coverage. The manifest
            pins every file's SHA-256, verified before use.
            """;
    }

    private static String provenance(String sourceSha, String leapSha,
                                     String kernelSha, int kernelBytes,
                                     SpkKernel excerpt, String validation) {
        StringBuilder md = new StringBuilder();
        md.append("# Solar System pack provenance\n\n")
                .append("Generated resource - do not edit; regenerate with:\n\n")
                .append("```sh\nscripts/download-solar-system-sources.sh\n")
                .append("make import-solar-system\n```\n\n")
                .append("Pack `").append(PACK_NAME).append("` version ")
                .append(PACK_VERSION).append(", audited ").append(AUDIT_DATE)
                .append(".\n\n")
                .append("## Sources\n\n")
                .append("| input | URL | retrieved | SHA-256 |\n|---|---|---|---|\n")
                .append("| `de440s.bsp` (NASA/JPL NAIF, DE440 short kernel) | ")
                .append(DE440S_URL).append(" | ").append(RETRIEVED).append(" | `")
                .append(sourceSha).append("` |\n")
                .append("| `Leap_Second.dat` (IERS Earth Orientation Centre) | ")
                .append(LEAP_URL).append(" | ").append(RETRIEVED).append(" | `")
                .append(leapSha).append("` |\n\n")
                .append("## Extraction\n\n")
                .append("Tool: `juranometria.tool.SolarSystemPackMain` (this repository),")
                .append(" using `juranometria.solar.spk.SpkExcerpt`. Segments kept,")
                .append(" in order: 0 -> 3 (Earth-Moon barycentre), 0 -> 10 (Sun),")
                .append(" 3 -> 399 (Earth), 3 -> 301 (Moon); all SPK Type 2.")
                .append(" Interval: 1900-01-01 to")
                .append(" 2101-01-01 TDB, with a 31-day margin at each end; whole")
                .append(" records are kept from the first covering the start to the")
                .append(" last covering the end, coefficients unchanged.\n\n")
                .append("Coverage of the written segments (seconds past J2000, TDB):\n\n");
        for (SpkKernel.Segment s : excerpt.segments()) {
            md.append(String.format(Locale.ROOT, "- %d -> %d: %.1f to %.1f\n",
                    s.center(), s.target(), s.startEt(), s.endEt()));
        }
        md.append("\n## Output\n\n")
                .append("| file | bytes | SHA-256 |\n|---|---|---|\n")
                .append("| `").append(KERNEL_NAME).append("` | ").append(kernelBytes)
                .append(" | `").append(kernelSha).append("` |\n\n")
                .append("## Validation\n\n").append(validation).append(".\n\n")
                .append("## Applicable rules\n\n")
                .append("NAIF: unmodified kernels may be redistributed; a modified")
                .append(" kernel must be renamed and its attribution replaced with the")
                .append(" modifier's, which this pack does in the kernel's comment area")
                .append(" and in `").append(NOTICE_NAME).append("`. SPICE data are")
                .append(" \"Technology and Software Publicly Available\"; no fees or")
                .append(" licensing are required. The IERS file is public bulletin")
                .append(" data, bundled unmodified.\n");
        return md.toString();
    }

    private static String manifest(Map<String, String> checksums,
                                   SpkKernel excerpt) {
        Map<String, String> m = new TreeMap<>();
        m.put("format.version", "1");
        m.put("pack.name", PACK_NAME);
        m.put("pack.version", Integer.toString(PACK_VERSION));
        m.put("audit.date", AUDIT_DATE);
        m.put("ephemeris", "DE440");
        m.put("ephemeris.bodies", "3,10,301,399");
        m.put("ephemeris.kernel", KERNEL_NAME);
        m.put("ephemeris.coverage.start.jd", String.format(Locale.ROOT, "%.1f",
                COVERAGE_START_JD));
        m.put("ephemeris.coverage.end.jd", String.format(Locale.ROOT, "%.1f",
                COVERAGE_END_JD));
        m.put("ephemeris.segments", Integer.toString(excerpt.segments().size()));
        m.put("leapseconds.file", LEAP_NAME);
        m.put("source.de440s.url", DE440S_URL);
        m.put("source.de440s.sha256", PINNED.get("de440s.bsp"));
        m.put("source.leapseconds.url", LEAP_URL);
        m.put("source.leapseconds.sha256", PINNED.get("Leap_Second.dat"));
        m.put("terms", "NAIF rules for modified kernels; IERS public data");
        for (Map.Entry<String, String> c : checksums.entrySet()) {
            m.put("checksum." + c.getKey(), c.getValue());
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            text.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        return text.toString();
    }

    private static void write(Path path, byte[] bytes,
                              Map<String, String> checksums) throws IOException {
        Files.write(path, bytes);
        checksums.put(path.getFileName().toString(),
                PinnedInputs.sha256Hex(bytes));
    }

    /** A non-empty output directory must already be this pack. */
    static void ensureSafeToClean(Path outDir) throws IOException {
        if (!Files.exists(outDir)) {
            return;
        }
        Path manifest = outDir.resolve("manifest.properties");
        if (!Files.isRegularFile(manifest)) {
            try (var listing = Files.list(outDir)) {
                if (listing.findAny().isPresent()) {
                    throw new IllegalStateException(outDir + " is not empty and"
                            + " carries no manifest; refusing to clean it");
                }
            }
            return;
        }
        String name = null;
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            if (line.startsWith("pack.name=")) {
                name = line.substring("pack.name=".length()).strip();
            }
        }
        if (!PACK_NAME.equals(name)) {
            throw new IllegalStateException(outDir + " holds pack \"" + name
                    + "\", not " + PACK_NAME + "; refusing to clean it");
        }
        try (var tree = Files.walk(outDir)) {
            for (Path p : tree.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    static InputStream resource(String name) {
        return SolarSystemPackMain.class.getResourceAsStream(
                "/resources/solar-system/" + name);
    }
}
