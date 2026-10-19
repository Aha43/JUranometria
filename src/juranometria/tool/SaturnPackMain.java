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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import juranometria.solar.SaturnPack;
import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;

/**
 * Builds the bundled Saturn ephemeris pack from its pinned raw inputs
 * (Sprint 52, issue #575; the layout ruled on #574): two
 * JUranometria-modified excerpts of NAIF's {@code sat441.bsp} - the
 * SAT441 Saturnian-satellite ephemeris merged with DE440 - the planetary
 * constants the contract names, copied from NAIF's {@code pck00011.tpc}
 * into the manifest, and the ring radii the ruling names, copied from
 * the PDS Ring-Moon Systems Node's table as the study retained it, with
 * the notice and provenance the atlas keeps beside bundled data.
 *
 * <p>Jupiter's layout, as ruled: the released Solar System, Jovian and
 * planets packs are untouched - this builder proves their kernels still
 * carry their released digests - and a fourth pack is added beside them.
 * Saturn (6 → 699) and its barycentre (0 → 6) over 1900–2100 in one
 * kernel; Mimas, Enceladus, Tethys, Dione, Rhea, Titan and Iapetus
 * (6 → 601 … 606, 608) over 2000–2100 in another, each with the released
 * 31-day margin. SAT441 keeps two segments per body meeting at
 * JD 2456928.5 (2014-09-28), so both kernels carry both, written by the
 * epoch-aware writer of #473.
 *
 * <p>The barycentre is DE440's: NAIF merged DE440's 0 → 6 into
 * {@code sat441.bsp}, and this builder proves that segment identical,
 * state for state, to the released source {@code de440s.bsp} over the
 * interval before cutting it.
 *
 * <p>Deterministic by construction: no timestamps other than the literal
 * audit date, {@code Locale.ROOT} formatting, and excerpts whose bytes
 * follow from the source bytes, the picks and the intervals alone. Every
 * excerpt is evaluated against the source before writing and must agree
 * exactly.
 *
 * <p>Run via {@code make import-saturn-system}, after
 * {@code scripts/download-saturn-sources.sh},
 * {@code scripts/download-jovian-sources.sh} (the PCK) and
 * {@code scripts/download-solar-system-sources.sh}.
 */
public final class SaturnPackMain {

    static final String PACK_NAME = "saturn-system";
    static final int PACK_VERSION = 1;
    static final String AUDIT_DATE = "2026-10-19";
    static final String RETRIEVED = "2026-10-19";

    static final String SATURN_KERNEL_NAME = "juranometria-sat441-saturn-1900-2100.bsp";
    static final String MOONS_KERNEL_NAME = "juranometria-sat441-moons-2000-2100.bsp";
    static final String NOTICE_NAME = "NOTICE-saturn-system.md";
    static final String PROVENANCE_NAME = "PROVENANCE.md";

    static final String SAT441_URL = "https://naif.jpl.nasa.gov/pub/naif/"
            + "generic_kernels/spk/satellites/sat441.bsp";
    static final String PCK_URL = JovianPackMain.PCK_URL;
    static final String DE440S_URL = SolarSystemPackMain.DE440S_URL;
    static final String RINGS_URL = "https://pds-rings.seti.org/saturn/saturn_tables.html";

    /** The raw satellite ephemeris, named as upstream names it, and its digest. */
    static final String SAT441_SHA256 =
            "d7e444a9ba7a52b8f448ff0747030789524d2f0c1212e4e2bd7f5fc3c96444d5";

    /** The retained PDS page and the table the study extracted from it. */
    static final Path RINGS_PAGE = Path.of("docs/studies/saturn-system/rings/saturn_tables.html");
    static final String RINGS_PAGE_SHA256 =
            "2dea8fcda149c94324353ccfb47e3df27f5dfef00ba8d62ab738a917378fc64c";
    static final Path RINGS_TABLE = Path.of("docs/studies/saturn-system/rings/pds-rings-table.tsv");

    /** The released packs' kernels, whose digests must not move. */
    static final String RELEASED_SOLAR_KERNEL_SHA256 =
            JovianPackMain.RELEASED_SOLAR_KERNEL_SHA256;
    static final Map<String, String> RELEASED_KERNELS = Map.of(
            "/resources/jovian-system/juranometria-jup365-jupiter-1900-2100.bsp",
            "efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92",
            "/resources/jovian-system/juranometria-jup365-galilean-2000-2100.bsp",
            "6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6",
            "/resources/planets/juranometria-de440-venus-1900-2100.bsp",
            "8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12",
            "/resources/planets/juranometria-de440-mercury-1900-2100.bsp",
            "78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d");

    static final int SSB = 0;
    static final int SATURN_BARYCENTRE = 6;
    static final int SATURN = 699;
    /** The ruled moons (#574, ruling 2), outward from Saturn; Hyperion (607) excluded. */
    static final List<Integer> MOONS = List.of(601, 602, 603, 604, 605, 606, 608);

    static final List<SpkExcerpt.Pick> SATURN_PICKS = List.of(
            new SpkExcerpt.Pick(SSB, SATURN_BARYCENTRE),
            new SpkExcerpt.Pick(SATURN_BARYCENTRE, SATURN));
    static final List<SpkExcerpt.Pick> MOON_PICKS = MOONS.stream()
            .map(id -> new SpkExcerpt.Pick(SATURN_BARYCENTRE, id)).toList();

    /** The civil intervals as TDB Julian dates, with the released margin, as Jupiter's. */
    static final double SATURN_START_JD = SolarSystemPackMain.COVERAGE_START_JD;
    static final double SATURN_END_JD = SolarSystemPackMain.COVERAGE_END_JD;
    static final double MOONS_START_JD = JovianPackMain.MOONS_START_JD;
    static final double MOONS_END_JD = SolarSystemPackMain.COVERAGE_END_JD;
    static final double MARGIN_DAYS = SolarSystemPackMain.MARGIN_DAYS;
    static final String SATURN_FIRST_DAY = "1900-01-01";
    static final String SATURN_LAST_DAY = "2100-12-31";
    static final String MOONS_FIRST_DAY = "2000-01-01";
    static final String MOONS_LAST_DAY = "2100-12-31";

    /** The PCK keywords copied into the manifest, in manifest order. */
    static final List<String> PCK_KEYWORDS = List.of(
            "BODY699_RADII", "BODY699_POLE_RA", "BODY699_POLE_DEC", "BODY10_RADII",
            "BODY601_RADII", "BODY602_RADII", "BODY603_RADII", "BODY604_RADII",
            "BODY605_RADII", "BODY606_RADII", "BODY608_RADII");

    /**
     * The ruled ring model's edges (#574, ruling 4): the manifest key, the
     * PDS feature and the boundary, as the table names them.
     */
    static final List<String[]> RING_EDGES = List.of(
            new String[] {"rings.c-ring.inner.km", "C Ring", "Inner"},
            new String[] {"rings.b-ring.outer.km", "B Ring", "Outer"},
            new String[] {"rings.cassini-division.outer.km", "Cassini Division", "Outer"},
            new String[] {"rings.a-ring.outer.km", "A Ring", "Outer"});

    private SaturnPackMain() {
    }

    public static void main(String[] args) throws IOException {
        Path rawDir = Path.of(args.length > 0 ? args[0] : "imports/raw/saturn-system");
        Path jovianRawDir = Path.of(args.length > 1 ? args[1] : "imports/raw/jovian-system");
        Path solarRawDir = Path.of(args.length > 2 ? args[2] : "imports/raw/solar-system");
        Path outDir = Path.of(args.length > 3 ? args[3] : "src/resources/saturn-system");

        verifyPinned(rawDir.resolve("sat441.bsp"), SAT441_SHA256,
                "scripts/download-saturn-sources.sh");
        verifyPinned(jovianRawDir.resolve("pck00011.tpc"), JovianPackMain.PINNED.get("pck00011.tpc"),
                "scripts/download-jovian-sources.sh");
        Path de440s = solarRawDir.resolve("de440s.bsp");
        verifyPinned(de440s, SolarSystemPackMain.PINNED.get("de440s.bsp"),
                "scripts/download-solar-system-sources.sh");
        verifyPinned(RINGS_PAGE, RINGS_PAGE_SHA256, "the study's retained page (docs/studies/saturn-system/rings)");

        // The released packs, untouched.
        String solarSha = resourceSha("/resources/solar-system/"
                + "juranometria-de440-sun-emb-earth-moon-1900-2100.bsp");
        if (!RELEASED_SOLAR_KERNEL_SHA256.equals(solarSha)) {
            throw new IllegalStateException("the released Solar System kernel has moved: "
                    + solarSha + "; nothing was written");
        }
        for (Map.Entry<String, String> k : new TreeMap<>(RELEASED_KERNELS).entrySet()) {
            String sha = resourceSha(k.getKey());
            if (!k.getValue().equals(sha)) {
                throw new IllegalStateException("the released kernel " + k.getKey()
                        + " has moved: " + sha + "; nothing was written");
            }
        }

        byte[] source = Files.readAllBytes(rawDir.resolve("sat441.bsp"));
        SpkKernel full = SpkKernel.read(source);
        String pck = Files.readString(jovianRawDir.resolve("pck00011.tpc"),
                StandardCharsets.US_ASCII);
        Map<String, String> constants = constants(pck);
        Map<String, String> rings = rings();

        // The barycentre is DE440's: identical to the released source.
        SpkKernel released = SpkKernel.read(Files.readAllBytes(de440s));
        int barycentreStates = 0;
        for (double jd = SATURN_START_JD - MARGIN_DAYS; jd <= SATURN_END_JD + MARGIN_DAYS; jd += 1.7) {
            double et = (jd - 2451545.0) * 86400.0;
            if (!full.state(SSB, SATURN_BARYCENTRE, et).equals(
                    released.state(SSB, SATURN_BARYCENTRE, et))) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "sat441's barycentre differs from de440s's at JD %.3f; nothing was written", jd));
            }
            barycentreStates++;
        }

        double saturnStart = (SATURN_START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;
        double saturnEnd = (SATURN_END_JD + MARGIN_DAYS - 2451545.0) * 86400.0;
        double moonsStart = (MOONS_START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;
        double moonsEnd = (MOONS_END_JD + MARGIN_DAYS - 2451545.0) * 86400.0;
        byte[] saturnKernel = SpkExcerpt.of(source, SATURN_PICKS, saturnStart, saturnEnd,
                "JURANOMETRIA SAT441 SATURN 1900-2100 V" + PACK_VERSION,
                comment(SATURN_KERNEL_NAME, "Saturn (699) and the Saturn barycentre (6)",
                        "  0 -> 6    Saturn barycentre from the solar-system barycentre (DE440's, as merged)\n"
                                + "  6 -> 699  Saturn from the Saturn barycentre (both SAT441 segments,"
                                + " meeting at 2014-09-28)\n",
                        "1900-01-01 to 2101-01-01", full.comments()));
        byte[] moonsKernel = SpkExcerpt.of(source, MOON_PICKS, moonsStart, moonsEnd,
                "JURANOMETRIA SAT441 MOONS 2000-2100 V" + PACK_VERSION,
                comment(MOONS_KERNEL_NAME, "Mimas (601), Enceladus (602), Tethys (603), Dione (604),"
                                + " Rhea (605), Titan (606) and Iapetus (608)",
                        "  6 -> 601  Mimas from the Saturn barycentre\n  6 -> 602  Enceladus\n"
                                + "  6 -> 603  Tethys\n  6 -> 604  Dione\n  6 -> 605  Rhea\n"
                                + "  6 -> 606  Titan\n  6 -> 608  Iapetus\n"
                                + "  (each both SAT441 segments, meeting at 2014-09-28)\n",
                        "2000-01-01 to 2101-01-01", full.comments()));

        // Proof before writing: each excerpt says what the source says.
        String saturnValidation = prove(SpkKernel.read(saturnKernel), full, SATURN_PICKS,
                SATURN_START_JD, SATURN_END_JD, "two");
        String moonsValidation = prove(SpkKernel.read(moonsKernel), full, MOON_PICKS,
                MOONS_START_JD, MOONS_END_JD, "seven");

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);
        Map<String, String> checksums = new LinkedHashMap<>();
        write(outDir.resolve(SATURN_KERNEL_NAME), saturnKernel, checksums);
        write(outDir.resolve(MOONS_KERNEL_NAME), moonsKernel, checksums);
        write(outDir.resolve(NOTICE_NAME), notice().getBytes(StandardCharsets.UTF_8), checksums);
        write(outDir.resolve(PROVENANCE_NAME), provenance(
                PinnedInputs.sha256Hex(pck.getBytes(StandardCharsets.US_ASCII)), checksums,
                saturnKernel.length, moonsKernel.length, SpkKernel.read(saturnKernel),
                SpkKernel.read(moonsKernel), saturnValidation, moonsValidation, barycentreStates,
                constants, rings).getBytes(StandardCharsets.UTF_8), checksums);
        Files.writeString(outDir.resolve("manifest.properties"),
                manifest(checksums, constants, rings, SpkKernel.read(saturnKernel),
                        SpkKernel.read(moonsKernel)), StandardCharsets.UTF_8);

        SaturnPack loaded = SaturnPack.load(name -> {
            try {
                return Files.newInputStream(outDir.resolve(name));
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        System.out.println("saturn-system pack written to " + outDir);
        System.out.println("  " + SATURN_KERNEL_NAME + ": " + saturnKernel.length
                + " bytes, sha256 " + checksums.get(SATURN_KERNEL_NAME));
        System.out.println("  " + MOONS_KERNEL_NAME + ": " + moonsKernel.length
                + " bytes, sha256 " + checksums.get(MOONS_KERNEL_NAME));
        System.out.println("  barycentre: " + barycentreStates
                + " states identical to the released de440s.bsp");
        System.out.println("  " + saturnValidation);
        System.out.println("  " + moonsValidation);
        System.out.println("  released Solar System, Jovian and planets kernels untouched");
        System.out.println("  reloaded and verified: " + loaded.manifest().get("pack.name")
                + " v" + loaded.manifest().get("pack.version") + ", constants " + loaded.constants());
    }

    private static String resourceSha(String resource) throws IOException {
        try (InputStream in = SaturnPackMain.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " is not on the classpath; run make classes");
            }
            return PinnedInputs.sha256Hex(in.readAllBytes());
        }
    }

    private static void verifyPinned(Path file, String expected, String origin) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("missing input " + file + "; see " + origin);
        }
        String actual = PinnedInputs.sha256Hex(Files.readAllBytes(file));
        if (!actual.equals(expected)) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "input %s fails its pinned checksum: expected %s, found %s", file, expected, actual));
        }
    }

    /** Every pick of the excerpt against the source at 1.7-day spacing. */
    private static String prove(SpkKernel excerpt, SpkKernel source, List<SpkExcerpt.Pick> picks,
                                double startJd, double endJd, String bodies) {
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
            throw new IllegalStateException("the excerpt disagrees with its source by " + worst
                    + "; nothing was written");
        }
        return String.format(Locale.ROOT, "%d states over the coverage at 1.7-day spacing, all %s"
                + " bodies: position and velocity identical to the source, worst difference %.1f",
                states, bodies, worst);
    }

    /**
     * The PCK's values for the keywords the contract names, verbatim (the
     * last assignment of each), and the proof that the PCK gives Saturn's
     * pole no nutation terms the service would leave out.
     */
    static Map<String, String> constants(String pck) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String keyword : PCK_KEYWORDS) {
            Matcher m = Pattern.compile("^\\s*" + keyword + "\\s*=\\s*\\(([^)]*)\\)",
                    Pattern.MULTILINE).matcher(pck);
            String last = null;
            while (m.find()) {
                last = String.join(",", m.group(1).trim().split("\\s+"));
            }
            if (last == null) {
                throw new IllegalStateException("pck00011.tpc does not assign " + keyword);
            }
            values.put(keyword, last);
        }
        if (Pattern.compile("^\\s*BODY699_NUT_PREC_(RA|DEC)\\s*=", Pattern.MULTILINE)
                .matcher(pck).find()) {
            throw new IllegalStateException("pck00011.tpc gives Saturn's pole nutation terms"
                    + " the contract does not model");
        }
        return values;
    }

    /**
     * The ruled ring edges, read from the table the study extracted - which
     * names the retained page by the digest verified above - as the table
     * states them (thousands separators dropped).
     */
    static Map<String, String> rings() throws IOException {
        List<String> lines = Files.readAllLines(RINGS_TABLE, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).contains(RINGS_PAGE_SHA256)) {
            throw new IllegalStateException(RINGS_TABLE + " does not name the retained page"
                    + " by its digest");
        }
        List<String> rows = lines.stream().filter(l -> !l.startsWith("#")).toList();
        List<String> head = List.of(rows.get(0).split("\t", -1));
        Map<String, String> edges = new LinkedHashMap<>();
        for (String[] edge : RING_EDGES) {
            int feature = head.indexOf("Feature");
            int column = head.indexOf(edge[2] + " Boundary (km)");
            String value = null;
            for (String row : rows.subList(1, rows.size())) {
                String[] cells = row.split("\t", -1);
                if (cells[feature].equals(edge[1])) {
                    value = cells[column].replace(",", "");
                }
            }
            if (value == null || !value.matches("\\d+")) {
                throw new IllegalStateException("the PDS table gives no " + edge[2] + " boundary"
                        + " for " + edge[1]);
            }
            edges.put(edge[0], value);
        }
        return edges;
    }

    private static String comment(String fileName, String bodies, String segments, String interval,
                                  String sourceComments) {
        StringBuilder text = new StringBuilder();
        text.append(fileName).append(":\n")
                .append("JUranometria Saturn System pack, version ").append(PACK_VERSION)
                .append(": a subset of the JPL satellite ephemeris SAT441,\n")
                .append("MODIFIED by JUranometria (https://github.com/Aha43/JUranometria).\n")
                .append("\n")
                .append("This is not an original JPL/NAIF file. It was produced by\n")
                .append("juranometria.tool.SaturnPackMain from the official kernel\n")
                .append("sat441.bsp, obtained from NASA/JPL NAIF at\n")
                .append("  ").append(SAT441_URL).append("\n")
                .append("(SHA-256 ").append(SAT441_SHA256).append(")\n")
                .append("by keeping, for ").append(bodies).append(",\n")
                .append("only these SPK Type 2 segments\n")
                .append(segments)
                .append("over the interval ").append(interval).append(" (TDB) with a margin\n")
                .append("of 31 days at each end. Every Chebyshev coefficient kept is the\n")
                .append("source's coefficient, unchanged; only whole records outside the\n")
                .append("interval were removed. Full provenance, digests and validation\n")
                .append("are in PROVENANCE.md beside this file.\n")
                .append("\n")
                .append("The satellite ephemeris is SAT441 (R. A. Jacobson, JPL Solar\n")
                .append("System Dynamics); the Saturn barycentre is DE440's (Park,\n")
                .append("Folkner, Williams and Boggs, 2021), as NAIF merged it. The source\n")
                .append("kernel's own comment area follows, retained as source attribution:\n")
                .append("\n")
                .append("---- sat441.bsp comment area, as distributed by NAIF ----\n");
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
            # Notice: Saturn System ephemeris pack

            This pack carries two **JUranometria-modified excerpts of the JPL
            satellite ephemeris SAT441**, so the atlas can compute where Saturn,
            its rings and seven of its moons are without any network access at
            build, test or run time. It stands beside the Solar System, Jovian
            and planets packs, which it does not change.

            ## One pack, two kernel files

            The two kernels are one logical, versioned pack: one manifest, one
            validation boundary, one notice and one provenance. They are two
            files because their intervals differ, as ruled on #574: Saturn
            answers over 1900 to 2100, the years the Sun and the Moon answer
            for, while Mimas, Enceladus, Tethys, Dione, Rhea, Titan and Iapetus
            answer over 2000 to 2100. Outside the moons' years Saturn still
            answers and the moon configuration refuses, naming its interval.

            ## The ephemeris excerpts

            `juranometria-sat441-saturn-1900-2100.bsp` and
            `juranometria-sat441-moons-2000-2100.bsp` were produced by
            JUranometria from the official kernel `sat441.bsp` distributed by
            NASA/JPL's Navigation and Ancillary Information Facility (NAIF):
            SAT441 for Saturn and its satellites about the Saturn barycentre,
            merged by NAIF with DE440's Saturn barycentre about the
            solar-system barycentre. The first keeps Saturn and the barycentre
            over the years 1900 to 2100, the second the seven moons over 2000
            to 2100, every coefficient unchanged. They are **modified kernels**
            under NAIF's rules and are named, annotated and attributed as such;
            they are not original JPL files and JPL did not produce them.

            NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
            by NAIF is permitted as long as they have not been modified." "If a
            kernel distributed by NAIF has been modified in any way, any
            embedded or otherwise allied attribution of the original kernel
            producer must be replaced with the name and institution of whomever
            has made the last modification." "The file name must also be
            changed to help avoid confusion." "No fees or licensing are
            required."

            The satellite ephemeris is SAT441 by R. A. Jacobson (JPL Solar
            System Dynamics), distributed by NAIF as `sat441.bsp`. The
            barycentre is DE440's, the work of R. S. Park, W. M. Folkner,
            J. G. Williams and D. H. Boggs, *The JPL Planetary and Lunar
            Ephemerides DE440 and DE441*, The Astronomical Journal 161:105
            (2021), doi:10.3847/1538-3881/abd414. JUranometria acknowledges
            NASA/JPL NAIF, JPL Solar System Dynamics and the ephemeris authors
            as the source of these data.

            ## The planetary constants and the rings

            The manifest carries, copied verbatim, the radii of Saturn, the Sun
            and the seven moons and Saturn's pole from NAIF's `pck00011.tpc`
            (the IAU Working Group on Cartographic Coordinates and Rotational
            Elements, 2015 report, as NAIF states it). The PCK file itself is
            not redistributed; its digest is recorded.

            The four ring radii the atlas draws - the C Ring's inner boundary,
            the B Ring's outer boundary, the Cassini Division's outer boundary
            and the A Ring's outer boundary - are quoted from the table "Rings
            of Saturn" of the Planetary Data System's Ring-Moon Systems Node,
            Vital Statistics for Saturn's Rings and Inner Satellites
            (https://pds-rings.seti.org/saturn/saturn_tables.html), with the
            footnoted sources that table cites. The page itself is not
            redistributed with the application.

            ## Not included

            Nothing of the IMCCE's Saturnian-phenomena predictions, software or
            files is in this pack or anywhere in the application; the atlas has
            no dependency on IMCCE, JPL Horizons or the PDS at build, test or
            run time. The study that chose this pack quoted a few of IMCCE's
            published event rows as test evidence, with attribution, in the
            repository's study record only.

            ## Provenance

            `PROVENANCE.md` beside this notice records the source URLs,
            retrieval date, upstream digests, the exact extraction and its
            validation, the output digests and the coverage. The manifest pins
            every file's SHA-256, verified before use.
            """;
    }

    private static String provenance(String pckSha, Map<String, String> checksums, int saturnBytes,
                                     int moonsBytes, SpkKernel saturn, SpkKernel moons,
                                     String saturnValidation, String moonsValidation,
                                     int barycentreStates, Map<String, String> constants,
                                     Map<String, String> rings) {
        StringBuilder md = new StringBuilder();
        md.append("# Saturn System pack provenance\n\n")
                .append("Generated resource - do not edit; regenerate with:\n\n")
                .append("```sh\nscripts/download-saturn-sources.sh\n")
                .append("scripts/download-jovian-sources.sh\n")
                .append("scripts/download-solar-system-sources.sh\n")
                .append("make import-saturn-system\n```\n\n")
                .append("Pack `").append(PACK_NAME).append("` version ").append(PACK_VERSION)
                .append(", audited ").append(AUDIT_DATE).append(".\n\n")
                .append("## Sources\n\n")
                .append("| input | URL | retrieved | SHA-256 |\n|---|---|---|---|\n")
                .append("| `sat441.bsp` (NASA/JPL NAIF, SAT441 with DE440) | ").append(SAT441_URL)
                .append(" | ").append(RETRIEVED).append(" | `").append(SAT441_SHA256).append("` |\n")
                .append("| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | ")
                .append(PCK_URL).append(" | ").append(JovianPackMain.RETRIEVED).append(" | `")
                .append(pckSha).append("` |\n")
                .append("| `saturn_tables.html` (PDS Ring-Moon Systems Node, \"Rings of Saturn\"; values quoted, page not redistributed) | ")
                .append(RINGS_URL).append(" | ").append(RETRIEVED).append(" | `")
                .append(RINGS_PAGE_SHA256).append("` |\n")
                .append("| `de440s.bsp` (the released Solar System pack's source; the barycentre identity proof) | ")
                .append(DE440S_URL).append(" | ").append(SolarSystemPackMain.RETRIEVED).append(" | `")
                .append(SolarSystemPackMain.PINNED.get("de440s.bsp")).append("` |\n\n")
                .append("## Extraction\n\n")
                .append("Tool: `juranometria.tool.SaturnPackMain` (this repository), using")
                .append(" `juranometria.solar.spk.SpkExcerpt`, which keeps every segment of a pick")
                .append(" with records in the interval (SAT441 keeps two per body, meeting at JD")
                .append(" 2456928.5 TDB, 2014-09-28). `").append(SATURN_KERNEL_NAME)
                .append("`: 0 -> 6 (Saturn barycentre), 6 -> 699 (Saturn), 1900-01-01 to 2101-01-01")
                .append(" TDB. `").append(MOONS_KERNEL_NAME).append("`: 6 -> 601 (Mimas), 602")
                .append(" (Enceladus), 603 (Tethys), 604 (Dione), 605 (Rhea), 606 (Titan), 608")
                .append(" (Iapetus), 2000-01-01 to 2101-01-01 TDB; Hyperion (607) excluded as ruled.")
                .append(" All SPK Type 2; a 31-day margin at each end; whole records kept from the")
                .append(" first covering the start to the last covering the end, coefficients")
                .append(" unchanged.\n\n")
                .append("Coverage of the written segments (seconds past J2000, TDB):\n\n");
        for (SpkKernel.Segment s : saturn.segments()) {
            md.append(String.format(Locale.ROOT, "- %s: %d -> %d: %.1f to %.1f\n",
                    SATURN_KERNEL_NAME, s.center(), s.target(), s.startEt(), s.endEt()));
        }
        for (SpkKernel.Segment s : moons.segments()) {
            md.append(String.format(Locale.ROOT, "- %s: %d -> %d: %.1f to %.1f\n",
                    MOONS_KERNEL_NAME, s.center(), s.target(), s.startEt(), s.endEt()));
        }
        md.append("\n## Output\n\n")
                .append("| file | bytes | SHA-256 |\n|---|---|---|\n")
                .append("| `").append(SATURN_KERNEL_NAME).append("` | ").append(saturnBytes)
                .append(" | `").append(checksums.get(SATURN_KERNEL_NAME)).append("` |\n")
                .append("| `").append(MOONS_KERNEL_NAME).append("` | ").append(moonsBytes)
                .append(" | `").append(checksums.get(MOONS_KERNEL_NAME)).append("` |\n\n")
                .append("## Validation\n\n")
                .append("- Saturn's kernel: ").append(saturnValidation).append(".\n")
                .append("- The moons' kernel: ").append(moonsValidation).append(".\n")
                .append("- The barycentre: ").append(barycentreStates)
                .append(" states of sat441.bsp's 0 -> 6 over 1900-2100 with the margin, at")
                .append(" 1.7-day spacing, identical to the released de440s.bsp's; the pack's")
                .append(" barycentre is DE440's, the one the Sun and the Moon stand on.\n")
                .append("- The released Solar System kernel is untouched: sha256 `")
                .append(RELEASED_SOLAR_KERNEL_SHA256).append("`, verified before writing.\n");
        for (Map.Entry<String, String> k : new TreeMap<>(RELEASED_KERNELS).entrySet()) {
            md.append("- `").append(k.getKey()).append("` is untouched: sha256 `")
                    .append(k.getValue()).append("`, verified before writing.\n");
        }
        md.append("\n## Planetary constants, copied from `pck00011.tpc`\n\n")
                .append("| keyword | value, verbatim |\n|---|---|\n");
        for (Map.Entry<String, String> c : constants.entrySet()) {
            md.append("| `").append(c.getKey()).append("` | `").append(c.getValue()).append("` |\n");
        }
        md.append("\nThe PCK assigns no BODY699_NUT_PREC_RA or BODY699_NUT_PREC_DEC: Saturn's pole")
                .append(" is the linear model above, verified by the builder.\n\n")
                .append("## Ring radii, quoted from the PDS table\n\n")
                .append("| manifest key | PDS feature and boundary | km |\n|---|---|---:|\n");
        for (String[] edge : RING_EDGES) {
            md.append("| `").append(edge[0]).append("` | ").append(edge[1]).append(", ")
                    .append(edge[2].toLowerCase(Locale.ROOT)).append(" | ")
                    .append(rings.get(edge[0])).append(" |\n");
        }
        md.append("\nRead from `docs/studies/saturn-system/rings/pds-rings-table.tsv`, the study's")
                .append(" row-by-row extraction of the retained page, which names the page by the")
                .append(" digest above; the B Ring's outer boundary is the B Ring row's, as ruled on")
                .append(" #574.\n\n")
                .append("## Applicable rules\n\n")
                .append("NAIF: unmodified kernels may be redistributed; a modified kernel must be")
                .append(" renamed and its attribution replaced with the modifier's, which this pack")
                .append(" does in each kernel's comment area and in `").append(NOTICE_NAME)
                .append("`. No fees or licensing are required. The PDS page states no terms; its")
                .append(" values are quoted with their source, the page not redistributed. Nothing")
                .append(" of IMCCE's is included.\n");
        return md.toString();
    }

    private static String manifest(Map<String, String> checksums, Map<String, String> constants,
                                   Map<String, String> rings, SpkKernel saturn, SpkKernel moons) {
        Map<String, String> m = new TreeMap<>();
        m.put("format.version", "1");
        m.put("pack.name", PACK_NAME);
        m.put("pack.version", Integer.toString(PACK_VERSION));
        m.put("audit.date", AUDIT_DATE);
        m.put("ephemeris", "SAT441 with DE440");
        m.put("ephemeris.saturn.kernel", SATURN_KERNEL_NAME);
        m.put("ephemeris.saturn.bodies", "6,699");
        m.put("ephemeris.saturn.segments", Integer.toString(saturn.segments().size()));
        m.put("ephemeris.saturn.coverage.start.jd", String.format(Locale.ROOT, "%.1f", SATURN_START_JD));
        m.put("ephemeris.saturn.coverage.end.jd", String.format(Locale.ROOT, "%.1f", SATURN_END_JD));
        m.put("ephemeris.saturn.first.day", SATURN_FIRST_DAY);
        m.put("ephemeris.saturn.last.day", SATURN_LAST_DAY);
        m.put("ephemeris.moons.kernel", MOONS_KERNEL_NAME);
        m.put("ephemeris.moons.bodies", String.join(",", MOONS.stream().map(String::valueOf).toList()));
        m.put("ephemeris.moons.segments", Integer.toString(moons.segments().size()));
        m.put("ephemeris.moons.coverage.start.jd", String.format(Locale.ROOT, "%.1f", MOONS_START_JD));
        m.put("ephemeris.moons.coverage.end.jd", String.format(Locale.ROOT, "%.1f", MOONS_END_JD));
        m.put("ephemeris.moons.first.day", MOONS_FIRST_DAY);
        m.put("ephemeris.moons.last.day", MOONS_LAST_DAY);
        m.put("source.sat441.url", SAT441_URL);
        m.put("source.sat441.sha256", SAT441_SHA256);
        m.put("source.pck.url", PCK_URL);
        m.put("source.pck.sha256", JovianPackMain.PINNED.get("pck00011.tpc"));
        m.put("source.rings.url", RINGS_URL);
        m.put("source.rings.sha256", RINGS_PAGE_SHA256);
        m.put("source.de440s.sha256", SolarSystemPackMain.PINNED.get("de440s.bsp"));
        m.put("solar-system.kernel.sha256", RELEASED_SOLAR_KERNEL_SHA256);
        for (Map.Entry<String, String> c : constants.entrySet()) {
            m.put("pck." + c.getKey(), c.getValue());
        }
        m.putAll(rings);
        m.put("terms", "NAIF rules for modified kernels; PCK values copied, file not redistributed;"
                + " PDS ring radii quoted, page not redistributed; nothing of IMCCE's");
        for (Map.Entry<String, String> c : checksums.entrySet()) {
            m.put("checksum." + c.getKey(), c.getValue());
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            text.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        return text.toString();
    }

    private static void write(Path path, byte[] bytes, Map<String, String> checksums) throws IOException {
        Files.write(path, bytes);
        checksums.put(path.getFileName().toString(), PinnedInputs.sha256Hex(bytes));
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
                    throw new IllegalStateException(outDir + " is not empty and carries no manifest;"
                            + " refusing to clean it");
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
            throw new IllegalStateException(outDir + " holds pack \"" + name + "\", not " + PACK_NAME
                    + "; refusing to clean it");
        }
        try (var tree = Files.walk(outDir)) {
            for (Path p : tree.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
