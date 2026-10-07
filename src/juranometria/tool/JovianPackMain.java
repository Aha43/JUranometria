package juranometria.tool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import juranometria.solar.JovianPack;
import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;

/**
 * Builds the bundled Jovian ephemeris pack from its pinned raw inputs
 * (Sprint 44, issue #473; the layout ruled on #472): two
 * JUranometria-modified excerpts of NAIF's {@code jup365.bsp} - the
 * JUP365 Galilean-satellite ephemeris merged with DE440 - and the
 * planetary constants the contract names, copied from NAIF's
 * {@code pck00011.tpc} into the manifest, with the notice and
 * provenance the atlas keeps beside bundled data.
 *
 * <p>Layout A, as ruled: the released Solar System pack is untouched -
 * this builder proves its kernel still carries its released digest -
 * and a second pack is added beside it. Jupiter (5 → 599) and its
 * barycentre (0 → 5) over 1900–2100 in one kernel; Io, Europa,
 * Ganymede and Callisto (5 → 501 … 504) over 2000–2100 in another,
 * each with the released 31-day margin. JUP365 keeps two segments per
 * body meeting at 1997-01-16, so Jupiter's kernel carries both of his
 * (the epoch-aware writer of #473's first change) and the moons' the
 * later one.
 *
 * <p>The barycentre is DE440's: NAIF merged DE440's 0 → 5 into
 * {@code jup365.bsp}, and this builder proves that segment identical,
 * state for state, to the released source {@code de440s.bsp} over the
 * interval before cutting it - so the pack's barycentre is the one
 * the Sun's and the Moon's computations already stand on.
 *
 * <p>Deterministic by construction, as the released pack's builder:
 * no timestamps other than the literal audit date, {@code Locale.ROOT}
 * formatting, and excerpts whose bytes follow from the source bytes,
 * the picks and the intervals alone. Running it twice writes the same
 * pack; the committed pack is proved by rebuilding it. Every excerpt
 * is evaluated against the source before writing and must agree
 * exactly.
 *
 * <p>Run via {@code make import-jovian-system}, after
 * {@code scripts/download-jovian-sources.sh} and
 * {@code scripts/download-solar-system-sources.sh}.
 */
public final class JovianPackMain {

    static final String PACK_NAME = "jovian-system";
    static final int PACK_VERSION = 1;
    static final String AUDIT_DATE = "2026-10-08";
    static final String RETRIEVED = "2026-10-07";

    static final String JUPITER_KERNEL_NAME =
            "juranometria-jup365-jupiter-1900-2100.bsp";
    static final String MOONS_KERNEL_NAME =
            "juranometria-jup365-galilean-2000-2100.bsp";
    static final String NOTICE_NAME = "NOTICE-jovian-system.md";
    static final String PROVENANCE_NAME = "PROVENANCE.md";

    static final String JUP365_URL = "https://naif.jpl.nasa.gov/pub/naif/"
            + "generic_kernels/spk/satellites/jup365.bsp";
    static final String PCK_URL = "https://naif.jpl.nasa.gov/pub/naif/"
            + "generic_kernels/pck/pck00011.tpc";
    static final String DE440S_URL = SolarSystemPackMain.DE440S_URL;

    /** The raw inputs, named as upstream names them, and their digests. */
    static final Map<String, String> PINNED = Map.of(
            "jup365.bsp",
            "dbf016c01ba4d022154838000cf3f06962cf958ddc503a366f7fe8f81495c5cb",
            "pck00011.tpc",
            "3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1");

    /** The released Solar System kernel's digest, which must not move. */
    static final String RELEASED_SOLAR_KERNEL_SHA256 =
            "63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580";

    static final int SSB = 0;
    static final int JUPITER_BARYCENTRE = 5;
    static final int JUPITER = 599;
    static final List<Integer> MOONS = List.of(501, 502, 503, 504);

    static final List<SpkExcerpt.Pick> JUPITER_PICKS = List.of(
            new SpkExcerpt.Pick(SSB, JUPITER_BARYCENTRE),
            new SpkExcerpt.Pick(JUPITER_BARYCENTRE, JUPITER));
    static final List<SpkExcerpt.Pick> MOON_PICKS = List.of(
            new SpkExcerpt.Pick(JUPITER_BARYCENTRE, 501),
            new SpkExcerpt.Pick(JUPITER_BARYCENTRE, 502),
            new SpkExcerpt.Pick(JUPITER_BARYCENTRE, 503),
            new SpkExcerpt.Pick(JUPITER_BARYCENTRE, 504));

    /**
     * The civil intervals as TDB Julian dates, with the released margin
     * (31 days, measured in #398: a civil instant of the first day lies
     * seconds before it in TDB). Jupiter: the released 1900-01-01 to
     * 2101-01-01; the moons: 2000-01-01 (JD 2451544.5, the J2000 epoch
     * less half a day) to 2101-01-01, as ruled on #472.
     */
    static final double JUPITER_START_JD = SolarSystemPackMain.COVERAGE_START_JD;
    static final double JUPITER_END_JD = SolarSystemPackMain.COVERAGE_END_JD;
    static final double MOONS_START_JD = 2451544.5;
    static final double MOONS_END_JD = SolarSystemPackMain.COVERAGE_END_JD;
    static final double MARGIN_DAYS = SolarSystemPackMain.MARGIN_DAYS;
    static final String JUPITER_FIRST_DAY = "1900-01-01";
    static final String JUPITER_LAST_DAY = "2100-12-31";
    static final String MOONS_FIRST_DAY = "2000-01-01";
    static final String MOONS_LAST_DAY = "2100-12-31";

    /** The PCK keywords copied into the manifest, in manifest order. */
    static final List<String> PCK_KEYWORDS = List.of(
            "BODY599_RADII", "BODY599_POLE_RA", "BODY599_POLE_DEC",
            "BODY10_RADII", "BODY501_RADII", "BODY502_RADII",
            "BODY503_RADII", "BODY504_RADII");

    private JovianPackMain() {
    }

    public static void main(String[] args) throws IOException {
        Path rawDir = Path.of(args.length > 0 ? args[0]
                : "imports/raw/jovian-system");
        Path solarRawDir = Path.of(args.length > 1 ? args[1]
                : "imports/raw/solar-system");
        Path outDir = Path.of(args.length > 2 ? args[2]
                : "src/resources/jovian-system");

        for (Map.Entry<String, String> pin : new TreeMap<>(PINNED).entrySet()) {
            verifyPinned(rawDir.resolve(pin.getKey()), pin.getValue(),
                    "scripts/download-jovian-sources.sh");
        }
        Path de440s = solarRawDir.resolve("de440s.bsp");
        verifyPinned(de440s, SolarSystemPackMain.PINNED.get("de440s.bsp"),
                "scripts/download-solar-system-sources.sh");

        // The released pack, untouched: its kernel's digest is the one
        // 5.0.0 shipped, read from the resources this build carries.
        String releasedSha;
        try (InputStream in = SolarSystemPackMain.resource(
                "juranometria-de440-sun-emb-earth-moon-1900-2100.bsp")) {
            if (in == null) {
                throw new IllegalStateException("the released Solar System"
                        + " kernel is not on the classpath; run make classes");
            }
            releasedSha = PinnedInputs.sha256Hex(in.readAllBytes());
        }
        if (!RELEASED_SOLAR_KERNEL_SHA256.equals(releasedSha)) {
            throw new IllegalStateException("the released Solar System kernel"
                    + " has moved: expected " + RELEASED_SOLAR_KERNEL_SHA256
                    + ", found " + releasedSha + "; nothing was written");
        }

        byte[] source = Files.readAllBytes(rawDir.resolve("jup365.bsp"));
        SpkKernel full = SpkKernel.read(source);
        String pck = Files.readString(rawDir.resolve("pck00011.tpc"),
                StandardCharsets.US_ASCII);
        Map<String, String> constants = constants(pck);

        // The barycentre is DE440's: identical to the released source.
        SpkKernel released = SpkKernel.read(Files.readAllBytes(de440s));
        int barycentreStates = 0;
        for (double jd = JUPITER_START_JD - MARGIN_DAYS;
                jd <= JUPITER_END_JD + MARGIN_DAYS; jd += 1.7) {
            double et = (jd - 2451545.0) * 86400.0;
            SpkKernel.State a = full.state(SSB, JUPITER_BARYCENTRE, et);
            SpkKernel.State b = released.state(SSB, JUPITER_BARYCENTRE, et);
            if (!a.equals(b)) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "jup365's barycentre differs from de440s's at JD %.3f;"
                                + " nothing was written", jd));
            }
            barycentreStates++;
        }

        double jupiterStart = (JUPITER_START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;
        double jupiterEnd = (JUPITER_END_JD + MARGIN_DAYS - 2451545.0) * 86400.0;
        double moonsStart = (MOONS_START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;
        double moonsEnd = (MOONS_END_JD + MARGIN_DAYS - 2451545.0) * 86400.0;
        byte[] jupiterKernel = SpkExcerpt.of(source, JUPITER_PICKS, jupiterStart,
                jupiterEnd, "JURANOMETRIA JUP365 JUPITER 1900-2100 V" + PACK_VERSION,
                comment(JUPITER_KERNEL_NAME, "Jupiter (599) and the Jupiter"
                        + " barycentre (5)", "  0 -> 5    Jupiter barycentre from"
                        + " the solar-system barycentre (DE440's, as merged)\n"
                        + "  5 -> 599  Jupiter from the Jupiter barycentre (both"
                        + " JUP365 segments, meeting at 1997-01-16)\n",
                        "1900-01-01 to 2101-01-01", full.comments()));
        byte[] moonsKernel = SpkExcerpt.of(source, MOON_PICKS, moonsStart, moonsEnd,
                "JURANOMETRIA JUP365 GALILEAN 2000-2100 V" + PACK_VERSION,
                comment(MOONS_KERNEL_NAME, "Io (501), Europa (502), Ganymede (503)"
                        + " and Callisto (504)", "  5 -> 501  Io from the Jupiter"
                        + " barycentre\n  5 -> 502  Europa\n  5 -> 503  Ganymede\n"
                        + "  5 -> 504  Callisto\n", "2000-01-01 to 2101-01-01",
                        full.comments()));

        // Proof before writing: each excerpt says what the source says.
        String jupiterValidation = prove(SpkKernel.read(jupiterKernel), full,
                JUPITER_PICKS, JUPITER_START_JD, JUPITER_END_JD, "two");
        String moonsValidation = prove(SpkKernel.read(moonsKernel), full,
                MOON_PICKS, MOONS_START_JD, MOONS_END_JD, "four");

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);
        Map<String, String> checksums = new LinkedHashMap<>();
        write(outDir.resolve(JUPITER_KERNEL_NAME), jupiterKernel, checksums);
        write(outDir.resolve(MOONS_KERNEL_NAME), moonsKernel, checksums);
        write(outDir.resolve(NOTICE_NAME),
                notice().getBytes(StandardCharsets.UTF_8), checksums);
        write(outDir.resolve(PROVENANCE_NAME), provenance(
                PinnedInputs.sha256Hex(source), PinnedInputs.sha256Hex(
                        pck.getBytes(StandardCharsets.US_ASCII)),
                checksums, jupiterKernel.length, moonsKernel.length,
                SpkKernel.read(jupiterKernel), SpkKernel.read(moonsKernel),
                jupiterValidation, moonsValidation, barycentreStates,
                constants).getBytes(StandardCharsets.UTF_8), checksums);
        Files.writeString(outDir.resolve("manifest.properties"),
                manifest(checksums, constants, SpkKernel.read(jupiterKernel),
                        SpkKernel.read(moonsKernel)), StandardCharsets.UTF_8);

        JovianPack loaded = JovianPack.load(name -> {
            try {
                return Files.newInputStream(outDir.resolve(name));
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        System.out.println("jovian-system pack written to " + outDir);
        System.out.println("  " + JUPITER_KERNEL_NAME + ": " + jupiterKernel.length
                + " bytes, sha256 " + checksums.get(JUPITER_KERNEL_NAME));
        System.out.println("  " + MOONS_KERNEL_NAME + ": " + moonsKernel.length
                + " bytes, sha256 " + checksums.get(MOONS_KERNEL_NAME));
        System.out.println("  barycentre: " + barycentreStates
                + " states identical to the released de440s.bsp");
        System.out.println("  " + jupiterValidation);
        System.out.println("  " + moonsValidation);
        System.out.println("  released Solar System kernel untouched: sha256 "
                + releasedSha);
        System.out.println("  reloaded and verified: " + loaded.manifest()
                .get("pack.name") + " v" + loaded.manifest().get("pack.version")
                + ", constants " + loaded.constants());
    }

    private static void verifyPinned(Path file, String expected, String script)
            throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("missing raw input " + file
                    + "; run " + script);
        }
        String actual = PinnedInputs.sha256Hex(Files.readAllBytes(file));
        if (!actual.equals(expected)) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "raw input %s fails its pinned checksum: expected %s,"
                            + " found %s", file, expected, actual));
        }
    }

    /** Every pick of the excerpt against the source at 1.7-day spacing. */
    private static String prove(SpkKernel excerpt, SpkKernel source,
                                List<SpkExcerpt.Pick> picks, double startJd,
                                double endJd, String bodies) {
        int states = 0;
        double worst = 0.0;
        for (double jd = startJd; jd <= endJd; jd += 1.7) {
            double et = (jd - 2451545.0) * 86400.0;
            for (SpkExcerpt.Pick pick : picks) {
                SpkKernel.State a = excerpt.state(pick.center(), pick.target(), et);
                SpkKernel.State b = source.state(pick.center(), pick.target(), et);
                worst = Math.max(worst, a.position().minus(b.position()).length());
                worst = Math.max(worst, a.velocity().minus(b.velocity()).length());
                states++;
            }
        }
        if (worst != 0.0) {
            throw new IllegalStateException("the excerpt disagrees with its"
                    + " source by " + worst + "; nothing was written");
        }
        return String.format(Locale.ROOT, "%d states over the coverage at"
                + " 1.7-day spacing, all %s bodies: position and velocity"
                + " identical to the source, worst difference %.1f", states,
                bodies, worst);
    }

    /**
     * The PCK's values for the keywords the contract names, verbatim:
     * the last assignment of each in the file (the text assigns them
     * once in its explanation and once in its data).
     */
    static Map<String, String> constants(String pck) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String keyword : PCK_KEYWORDS) {
            Matcher m = Pattern.compile("^\\s*" + keyword
                    + "\\s*=\\s*\\(([^)]*)\\)", Pattern.MULTILINE).matcher(pck);
            String last = null;
            while (m.find()) {
                last = String.join(",", m.group(1).trim().split("\\s+"));
            }
            if (last == null) {
                throw new IllegalStateException("pck00011.tpc does not assign "
                        + keyword);
            }
            values.put(keyword, last);
        }
        return values;
    }

    private static String comment(String fileName, String bodies, String segments,
                                  String interval, String sourceComments) {
        StringBuilder text = new StringBuilder();
        text.append(fileName).append(":\n")
                .append("JUranometria Jovian System pack, version ").append(PACK_VERSION)
                .append(": a subset of the JPL satellite ephemeris JUP365,\n")
                .append("MODIFIED by JUranometria (https://github.com/Aha43/JUranometria).\n")
                .append("\n")
                .append("This is not an original JPL/NAIF file. It was produced by\n")
                .append("juranometria.tool.JovianPackMain from the official kernel\n")
                .append("jup365.bsp, obtained from NASA/JPL NAIF at\n")
                .append("  ").append(JUP365_URL).append("\n")
                .append("(SHA-256 ").append(PINNED.get("jup365.bsp")).append(")\n")
                .append("by keeping, for ").append(bodies).append(",\n")
                .append("only these SPK Type 2 segments\n")
                .append(segments)
                .append("over the interval ").append(interval).append(" (TDB) with a margin\n")
                .append("of 31 days at each end. Every Chebyshev coefficient kept is the\n")
                .append("source's coefficient, unchanged; only whole records outside the\n")
                .append("interval were removed. Full provenance, digests and validation\n")
                .append("are in PROVENANCE.md beside this file.\n")
                .append("\n")
                .append("The satellite ephemeris is JUP365 (R. A. Jacobson, JPL Solar\n")
                .append("System Dynamics, 2021); the Jupiter barycentre is DE440's (Park,\n")
                .append("Folkner, Williams and Boggs, 2021), as NAIF merged it. The source\n")
                .append("kernel's own comment area follows, retained as source attribution:\n")
                .append("\n")
                .append("---- jup365.bsp comment area, as distributed by NAIF ----\n");
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
            # Notice: Jovian System ephemeris pack

            This pack carries two **JUranometria-modified excerpts of the JPL
            satellite ephemeris JUP365**, so the atlas can compute where
            Jupiter and its four Galilean moons are without any network
            access at build, test or run time. It stands beside the Solar
            System pack, which it does not change: the Sun and the Moon are
            computed from that pack alone, as released.

            ## The ephemeris excerpts

            `juranometria-jup365-jupiter-1900-2100.bsp` and
            `juranometria-jup365-galilean-2000-2100.bsp` were produced by
            JUranometria from the official kernel `jup365.bsp` distributed by
            NASA/JPL's Navigation and Ancillary Information Facility (NAIF):
            JUP365 for Io, Europa, Ganymede, Callisto and Jupiter about the
            Jupiter barycentre, merged by NAIF with DE440's Jupiter barycentre
            about the solar-system barycentre. The first keeps Jupiter and
            the barycentre over the years 1900 to 2100, the second the four
            moons over 2000 to 2100, every coefficient unchanged. They are
            **modified kernels** under NAIF's rules and are named, annotated
            and attributed as such; they are not original JPL files and JPL
            did not produce them.

            NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
            by NAIF is permitted as long as they have not been modified." "If
            a kernel distributed by NAIF has been modified in any way, any
            embedded or otherwise allied attribution of the original kernel
            producer must be replaced with the name and institution of
            whomever has made the last modification." "The file name must
            also be changed to help avoid confusion." SPICE data carry no
            fees or licensing ("Technology and Software Publicly Available").

            The satellite ephemeris is JUP365 by R. A. Jacobson (JPL Solar
            System Dynamics, 2021), distributed by NAIF as `jup365.bsp`
            ("JUP365 Satellite Ephemeris with Jupiter barycenter (5), Sun
            (10), Earth Moon barycenter (3), and Earth (399) from DE440",
            NAIF, 2021-03-14). The barycentre is DE440's, the work of
            R. S. Park, W. M. Folkner, J. G. Williams and D. H. Boggs, *The
            JPL Planetary and Lunar Ephemerides DE440 and DE441*, The
            Astronomical Journal 161:105 (2021), doi:10.3847/1538-3881/abd414.
            JUranometria acknowledges NASA/JPL NAIF, JPL Solar System Dynamics
            and the ephemeris authors as the source of these data.

            ## The planetary constants

            The manifest carries, copied verbatim, the radii of Jupiter, the
            Sun and the four moons and Jupiter's pole from NAIF's
            `pck00011.tpc` (the IAU Working Group on Cartographic Coordinates
            and Rotational Elements, 2015 report, as NAIF states it). The PCK
            file itself is not redistributed; its digest is recorded.

            ## Not included

            Nothing of the IMCCE's Galilean-phenomena catalogue, software or
            files is in this pack or anywhere in the application; the atlas
            has no dependency on IMCCE at build, test or run time. The study
            that chose this pack quoted a few of IMCCE's published event rows
            as test evidence, with attribution, in the repository's study
            record only.

            ## Provenance

            `PROVENANCE.md` beside this notice records the source URLs,
            retrieval date, upstream digests, the exact extraction and its
            validation, the output digests and the coverage. The manifest
            pins every file's SHA-256, verified before use.
            """;
    }

    private static String provenance(String sourceSha, String pckSha,
                                     Map<String, String> checksums,
                                     int jupiterBytes, int moonsBytes,
                                     SpkKernel jupiter, SpkKernel moons,
                                     String jupiterValidation,
                                     String moonsValidation,
                                     int barycentreStates,
                                     Map<String, String> constants) {
        StringBuilder md = new StringBuilder();
        md.append("# Jovian System pack provenance\n\n")
                .append("Generated resource - do not edit; regenerate with:\n\n")
                .append("```sh\nscripts/download-jovian-sources.sh\n")
                .append("scripts/download-solar-system-sources.sh\n")
                .append("make import-jovian-system\n```\n\n")
                .append("Pack `").append(PACK_NAME).append("` version ")
                .append(PACK_VERSION).append(", audited ").append(AUDIT_DATE)
                .append(".\n\n")
                .append("## Sources\n\n")
                .append("| input | URL | retrieved | SHA-256 |\n|---|---|---|---|\n")
                .append("| `jup365.bsp` (NASA/JPL NAIF, JUP365 with DE440) | ")
                .append(JUP365_URL).append(" | ").append(RETRIEVED).append(" | `")
                .append(sourceSha).append("` |\n")
                .append("| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | ")
                .append(PCK_URL).append(" | ").append(RETRIEVED).append(" | `")
                .append(pckSha).append("` |\n")
                .append("| `de440s.bsp` (the released Solar System pack's source; the barycentre identity proof) | ")
                .append(DE440S_URL).append(" | ").append(SolarSystemPackMain.RETRIEVED)
                .append(" | `").append(SolarSystemPackMain.PINNED.get("de440s.bsp"))
                .append("` |\n\n")
                .append("## Extraction\n\n")
                .append("Tool: `juranometria.tool.JovianPackMain` (this repository),")
                .append(" using `juranometria.solar.spk.SpkExcerpt`, which keeps every")
                .append(" segment of a pick with records in the interval (JUP365 keeps")
                .append(" two per body, meeting at JD 2450464.5 TDB, 1997-01-16).")
                .append(" `").append(JUPITER_KERNEL_NAME).append("`: 0 -> 5 (Jupiter")
                .append(" barycentre), 5 -> 599 (Jupiter), 1900-01-01 to 2101-01-01 TDB.")
                .append(" `").append(MOONS_KERNEL_NAME).append("`: 5 -> 501 (Io),")
                .append(" 5 -> 502 (Europa), 5 -> 503 (Ganymede), 5 -> 504 (Callisto),")
                .append(" 2000-01-01 to 2101-01-01 TDB. All SPK Type 2; a 31-day margin")
                .append(" at each end; whole records kept from the first covering the")
                .append(" start to the last covering the end, coefficients unchanged.\n\n")
                .append("Coverage of the written segments (seconds past J2000, TDB):\n\n");
        for (SpkKernel.Segment s : jupiter.segments()) {
            md.append(String.format(Locale.ROOT, "- %s: %d -> %d: %.1f to %.1f\n",
                    JUPITER_KERNEL_NAME, s.center(), s.target(), s.startEt(), s.endEt()));
        }
        for (SpkKernel.Segment s : moons.segments()) {
            md.append(String.format(Locale.ROOT, "- %s: %d -> %d: %.1f to %.1f\n",
                    MOONS_KERNEL_NAME, s.center(), s.target(), s.startEt(), s.endEt()));
        }
        md.append("\n## Output\n\n")
                .append("| file | bytes | SHA-256 |\n|---|---|---|\n")
                .append("| `").append(JUPITER_KERNEL_NAME).append("` | ").append(jupiterBytes)
                .append(" | `").append(checksums.get(JUPITER_KERNEL_NAME)).append("` |\n")
                .append("| `").append(MOONS_KERNEL_NAME).append("` | ").append(moonsBytes)
                .append(" | `").append(checksums.get(MOONS_KERNEL_NAME)).append("` |\n\n")
                .append("## Validation\n\n")
                .append("- Jupiter's kernel: ").append(jupiterValidation).append(".\n")
                .append("- The moons' kernel: ").append(moonsValidation).append(".\n")
                .append("- The barycentre: ").append(barycentreStates)
                .append(" states of jup365.bsp's 0 -> 5 over 1900-2100 with the margin,")
                .append(" at 1.7-day spacing, identical to the released de440s.bsp's;")
                .append(" the pack's barycentre is DE440's, the one the Sun and the Moon")
                .append(" stand on.\n")
                .append("- The released Solar System kernel is untouched: sha256 `")
                .append(RELEASED_SOLAR_KERNEL_SHA256).append("`, verified before writing.\n\n")
                .append("## Planetary constants, copied from `pck00011.tpc`\n\n")
                .append("| keyword | value, verbatim |\n|---|---|\n");
        for (Map.Entry<String, String> c : constants.entrySet()) {
            md.append("| `").append(c.getKey()).append("` | `").append(c.getValue())
                    .append("` |\n");
        }
        md.append("\n## Applicable rules\n\n")
                .append("NAIF: unmodified kernels may be redistributed; a modified")
                .append(" kernel must be renamed and its attribution replaced with the")
                .append(" modifier's, which this pack does in each kernel's comment area")
                .append(" and in `").append(NOTICE_NAME).append("`. SPICE data are")
                .append(" \"Technology and Software Publicly Available\"; no fees or")
                .append(" licensing are required. Nothing of IMCCE's is included.\n");
        return md.toString();
    }

    private static String manifest(Map<String, String> checksums,
                                   Map<String, String> constants,
                                   SpkKernel jupiter, SpkKernel moons) {
        Map<String, String> m = new TreeMap<>();
        m.put("format.version", "1");
        m.put("pack.name", PACK_NAME);
        m.put("pack.version", Integer.toString(PACK_VERSION));
        m.put("audit.date", AUDIT_DATE);
        m.put("ephemeris", "JUP365 with DE440");
        m.put("ephemeris.jupiter.kernel", JUPITER_KERNEL_NAME);
        m.put("ephemeris.jupiter.bodies", "5,599");
        m.put("ephemeris.jupiter.segments", Integer.toString(jupiter.segments().size()));
        m.put("ephemeris.jupiter.coverage.start.jd",
                String.format(Locale.ROOT, "%.1f", JUPITER_START_JD));
        m.put("ephemeris.jupiter.coverage.end.jd",
                String.format(Locale.ROOT, "%.1f", JUPITER_END_JD));
        m.put("ephemeris.jupiter.first.day", JUPITER_FIRST_DAY);
        m.put("ephemeris.jupiter.last.day", JUPITER_LAST_DAY);
        m.put("ephemeris.moons.kernel", MOONS_KERNEL_NAME);
        m.put("ephemeris.moons.bodies", "501,502,503,504");
        m.put("ephemeris.moons.segments", Integer.toString(moons.segments().size()));
        m.put("ephemeris.moons.coverage.start.jd",
                String.format(Locale.ROOT, "%.1f", MOONS_START_JD));
        m.put("ephemeris.moons.coverage.end.jd",
                String.format(Locale.ROOT, "%.1f", MOONS_END_JD));
        m.put("ephemeris.moons.first.day", MOONS_FIRST_DAY);
        m.put("ephemeris.moons.last.day", MOONS_LAST_DAY);
        m.put("source.jup365.url", JUP365_URL);
        m.put("source.jup365.sha256", PINNED.get("jup365.bsp"));
        m.put("source.pck.url", PCK_URL);
        m.put("source.pck.sha256", PINNED.get("pck00011.tpc"));
        m.put("source.de440s.sha256", SolarSystemPackMain.PINNED.get("de440s.bsp"));
        m.put("solar-system.kernel.sha256", RELEASED_SOLAR_KERNEL_SHA256);
        for (Map.Entry<String, String> c : constants.entrySet()) {
            m.put("pck." + c.getKey(), c.getValue());
        }
        m.put("terms", "NAIF rules for modified kernels; PCK values copied,"
                + " file not redistributed; nothing of IMCCE's");
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
}
