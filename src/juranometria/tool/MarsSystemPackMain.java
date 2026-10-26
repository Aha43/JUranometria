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

import juranometria.solar.MarsSystemPack;
import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;

/**
 * Builds the bundled Mars System pack - Phobos and Deimos - from its
 * pinned raw inputs (issue #602; the contract ruled on #601, M-7a with
 * the exclusive boundary): one JUranometria-modified excerpt of NAIF's
 * {@code mar099s.bsp}, the moons' radii copied from NAIF's
 * {@code pck00011.tpc} into the manifest, and the notice and provenance
 * the atlas keeps beside bundled data.
 *
 * <p>Mars itself is not here: it is the planets pack's third planet,
 * DE440's Mars barycentre (#601, M-1). This pack holds the moons about
 * that barycentre, 4 → 401 (Phobos) and 4 → 402 (Deimos), from 2000 with
 * the released 31-day margin up to mar099s's own end, 2050-01-01 TDB.
 * That end is the moons' exclusive boundary: the service never evaluates
 * it, and this builder's proof stops short of it (the #601 measurement:
 * at that one instant source and excerpt may select neighbouring
 * records). The barycentre the moons stand on is DE440's; the builder
 * proves mar099s's own 0 → 4 identical to the released {@code de440s.bsp}
 * over the moons' interval before cutting.
 *
 * <p>The released Solar System, Jovian, Saturn System and planets kernels
 * are untouched: the builder proves their digests before writing.
 * Deterministic by construction, as the released builders.
 *
 * <p>Run via {@code make import-mars-system}, after
 * {@code scripts/download-mars-sources.sh},
 * {@code scripts/download-jovian-sources.sh} (the PCK) and
 * {@code scripts/download-solar-system-sources.sh}.
 */
public final class MarsSystemPackMain {

    static final String PACK_NAME = "mars-system";
    static final int PACK_VERSION = 1;
    static final String AUDIT_DATE = "2026-10-25";
    static final String RETRIEVED = "2026-10-25";

    static final String MOONS_KERNEL_NAME = "juranometria-mar099-moons-2000-2050.bsp";
    static final String NOTICE_NAME = "NOTICE-mars-system.md";
    static final String PROVENANCE_NAME = "PROVENANCE.md";

    static final String MAR099S_URL = "https://naif.jpl.nasa.gov/pub/naif/"
            + "generic_kernels/spk/satellites/mar099s.bsp";
    static final String MAR099S_SHA256 = PlanetsPackMain.MAR099S_SHA256;
    static final String PCK_URL = JovianPackMain.PCK_URL;
    static final String DE440S_URL = SolarSystemPackMain.DE440S_URL;

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
            "78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d",
            "/resources/saturn-system/juranometria-sat441-saturn-1900-2100.bsp",
            "28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d",
            "/resources/saturn-system/juranometria-sat441-moons-2000-2100.bsp",
            "8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b");

    static final int SSB = 0;
    static final int MARS_BARYCENTRE = 4;
    /** The ruled moons (#601, M-7a), outward from Mars. */
    static final List<Integer> MOONS = List.of(401, 402);
    static final List<SpkExcerpt.Pick> MOON_PICKS = MOONS.stream()
            .map(id -> new SpkExcerpt.Pick(MARS_BARYCENTRE, id)).toList();

    /** The moons' start, as Jupiter's and Saturn's: 2000-01-01 00:00 TDB, with the margin. */
    static final double MOONS_START_JD = JovianPackMain.MOONS_START_JD;
    static final double MARGIN_DAYS = SolarSystemPackMain.MARGIN_DAYS;
    static final String MOONS_FIRST_DAY = "2000-01-01";

    /** The PCK keywords copied into the manifest, in manifest order. */
    static final List<String> PCK_KEYWORDS = List.of("BODY401_RADII", "BODY402_RADII");

    private MarsSystemPackMain() {
    }

    public static void main(String[] args) throws IOException {
        Path rawDir = Path.of(args.length > 0 ? args[0] : "imports/raw/mars-system");
        Path jovianRawDir = Path.of(args.length > 1 ? args[1] : "imports/raw/jovian-system");
        Path solarRawDir = Path.of(args.length > 2 ? args[2] : "imports/raw/solar-system");
        Path outDir = Path.of(args.length > 3 ? args[3] : "src/resources/mars-system");

        verifyPinned(rawDir.resolve("mar099s.bsp"), MAR099S_SHA256,
                "scripts/download-mars-sources.sh");
        verifyPinned(jovianRawDir.resolve("pck00011.tpc"), JovianPackMain.PINNED.get("pck00011.tpc"),
                "scripts/download-jovian-sources.sh");
        Path de440s = solarRawDir.resolve("de440s.bsp");
        verifyPinned(de440s, SolarSystemPackMain.PINNED.get("de440s.bsp"),
                "scripts/download-solar-system-sources.sh");

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

        byte[] source = Files.readAllBytes(rawDir.resolve("mar099s.bsp"));
        SpkKernel full = SpkKernel.read(source);
        String pck = Files.readString(jovianRawDir.resolve("pck00011.tpc"),
                StandardCharsets.US_ASCII);
        Map<String, String> constants = PlanetsPackMain.PinnedPck.values(pck, PCK_KEYWORDS);

        double moonsStart = (MOONS_START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;
        double moonsEnd = endOfMoons(full);

        // The barycentre is DE440's: identical to the released source.
        SpkKernel released = SpkKernel.read(Files.readAllBytes(de440s));
        int barycentreStates = 0;
        for (double et = moonsStart; et < moonsEnd; et += 1.7 * 86400.0) {
            if (!full.state(SSB, MARS_BARYCENTRE, et).equals(
                    released.state(SSB, MARS_BARYCENTRE, et))) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "mar099s's Mars barycentre differs from de440s's at %.1f s; nothing was"
                                + " written", et));
            }
            barycentreStates++;
        }

        byte[] moonsKernel = SpkExcerpt.of(source, MOON_PICKS, moonsStart, moonsEnd,
                "JURANOMETRIA MAR099 MOONS 2000-2050 V" + PACK_VERSION,
                comment(full.comments()));
        String validation = prove(SpkKernel.read(moonsKernel), full, moonsStart, moonsEnd);

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);
        Map<String, String> checksums = new LinkedHashMap<>();
        write(outDir.resolve(MOONS_KERNEL_NAME), moonsKernel, checksums);
        write(outDir.resolve(NOTICE_NAME), notice().getBytes(StandardCharsets.UTF_8), checksums);
        write(outDir.resolve(PROVENANCE_NAME), provenance(
                PinnedInputs.sha256Hex(pck.getBytes(StandardCharsets.US_ASCII)), checksums,
                moonsKernel.length, SpkKernel.read(moonsKernel), validation, barycentreStates,
                moonsEnd, constants).getBytes(StandardCharsets.UTF_8), checksums);
        Files.writeString(outDir.resolve("manifest.properties"),
                manifest(checksums, constants, SpkKernel.read(moonsKernel), moonsEnd),
                StandardCharsets.UTF_8);

        MarsSystemPack loaded = MarsSystemPack.load(name -> {
            try {
                return Files.newInputStream(outDir.resolve(name));
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        System.out.println("mars-system pack written to " + outDir);
        System.out.println("  " + MOONS_KERNEL_NAME + ": " + moonsKernel.length
                + " bytes, sha256 " + checksums.get(MOONS_KERNEL_NAME));
        System.out.println("  barycentre: " + barycentreStates
                + " states identical to the released de440s.bsp");
        System.out.println("  " + validation);
        System.out.println("  released Solar System, Jovian, Saturn System and planets kernels untouched");
        System.out.println("  reloaded and verified: " + loaded.manifest().get("pack.name")
                + " v" + loaded.manifest().get("pack.version") + ", moons to (exclusive) "
                + loaded.moonsEndJdTdb());
    }

    /** mar099s's own end for the moons: the same for both, read from the kernel. */
    static double endOfMoons(SpkKernel full) {
        Double end = null;
        for (int moon : MOONS) {
            double e = full.segment(MARS_BARYCENTRE, moon).endEt();
            if (end != null && end != e) {
                throw new IllegalStateException("mar099s's moons end at different instants");
            }
            end = e;
        }
        return end;
    }

    private static String resourceSha(String resource) throws IOException {
        try (InputStream in = MarsSystemPackMain.class.getResourceAsStream(resource)) {
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

    /**
     * Both moons against the source at 1.7-day spacing, strictly before the
     * exclusive end: the instant the ruling says production never evaluates
     * is not evaluated here either.
     */
    private static String prove(SpkKernel excerpt, SpkKernel source, double startEt, double endEt) {
        int states = 0;
        double worst = 0.0;
        for (double et = startEt; et < endEt; et += 1.7 * 86400.0) {
            for (SpkExcerpt.Pick pick : MOON_PICKS) {
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
        return String.format(Locale.ROOT, "%d states at 1.7-day spacing from the start up to,"
                + " not including, the exclusive end, both moons: position and velocity identical"
                + " to the source, worst difference %.1f", states, worst);
    }

    private static String comment(String sourceComments) {
        StringBuilder text = new StringBuilder();
        text.append(MOONS_KERNEL_NAME).append(":\n")
                .append("JUranometria Mars System pack, version ").append(PACK_VERSION)
                .append(": a subset of the JPL satellite ephemeris MAR099,\n")
                .append("MODIFIED by JUranometria (https://github.com/Aha43/JUranometria).\n")
                .append("\n")
                .append("This is not an original JPL/NAIF file. It was produced by\n")
                .append("juranometria.tool.MarsSystemPackMain from the official kernel\n")
                .append("mar099s.bsp, obtained from NASA/JPL NAIF at\n")
                .append("  ").append(MAR099S_URL).append("\n")
                .append("(SHA-256 ").append(MAR099S_SHA256).append(")\n")
                .append("by keeping, for Phobos (401) and Deimos (402), only these SPK\n")
                .append("Type 2 segments\n")
                .append("  4 -> 401  Phobos from the Mars barycentre\n")
                .append("  4 -> 402  Deimos from the Mars barycentre\n")
                .append("over the interval 2000-01-01 (TDB), with a margin of 31 days\n")
                .append("before it, to the source's own end, 2050-01-01 (TDB), which is\n")
                .append("the moons' exclusive boundary. Every Chebyshev coefficient kept\n")
                .append("is the source's coefficient, unchanged; only whole records\n")
                .append("outside the interval were removed. Full provenance, digests and\n")
                .append("validation are in PROVENANCE.md beside this file.\n")
                .append("\n")
                .append("The satellite ephemeris is MAR099 (JPL Solar System Dynamics);\n")
                .append("the Mars barycentre it is stated about is DE440's (Park, Folkner,\n")
                .append("Williams and Boggs, 2021). The source kernel's own comment area\n")
                .append("follows, retained as source attribution:\n")
                .append("\n")
                .append("---- mar099s.bsp comment area, as distributed by NAIF ----\n");
        for (String line : sourceComments.split("\n", -1)) {
            StringBuilder printable = new StringBuilder();
            for (char c : line.toCharArray()) {
                printable.append(c >= 0x20 && c <= 0x7e ? c : '?');
            }
            text.append(printable).append('\n');
        }
        text.append("---- end of the source kernel's comment area ----");
        return text.toString();
    }

    static String notice() {
        return """
            # Notice: Mars System ephemeris pack

            This pack carries one **JUranometria-modified excerpt of the JPL
            satellite ephemeris MAR099**, so the atlas can compute where
            Phobos and Deimos are without any network access at build, test
            or run time. It stands beside the Solar System, Jovian, Saturn
            System and planets packs, which it does not change. Mars itself
            is in the planets pack.

            ## The moons' years

            Phobos and Deimos answer from 2000 up to, but not including, the
            first instant of 2050 (TDB), the end of NAIF's short Mars kernel,
            as ruled on #601: the atlas speaks of them as 2000 to 2049. Mars
            answers over 1900 to 2100 from the planets pack; outside the
            moons' years Mars still answers and the moons refuse, naming
            their years.

            ## The ephemeris excerpt

            `juranometria-mar099-moons-2000-2050.bsp` was produced by
            JUranometria from the official kernel `mar099s.bsp` distributed
            by NASA/JPL's Navigation and Ancillary Information Facility
            (NAIF): MAR099 for Phobos and Deimos about the Mars barycentre,
            merged by NAIF with DE440. It keeps the two moons over the years
            2000 to 2050, every coefficient unchanged. It is a **modified
            kernel** under NAIF's rules and is named, annotated and attributed
            as such; it is not an original JPL file and JPL did not produce
            it.

            NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
            by NAIF is permitted as long as they have not been modified." "If a
            kernel distributed by NAIF has been modified in any way, any
            embedded or otherwise allied attribution of the original kernel
            producer must be replaced with the name and institution of whomever
            has made the last modification." "The file name must also be
            changed to help avoid confusion." "No fees or licensing are
            required."

            The satellite ephemeris is MAR099, from JPL Solar System Dynamics,
            distributed by NAIF as `mar099s.bsp`. The barycentre it is stated
            about is DE440's, the work of R. S. Park, W. M. Folkner,
            J. G. Williams and D. H. Boggs, *The JPL Planetary and Lunar
            Ephemerides DE440 and DE441*, The Astronomical Journal 161:105
            (2021), doi:10.3847/1538-3881/abd414. JUranometria acknowledges
            NASA/JPL NAIF, JPL Solar System Dynamics and the ephemeris authors
            as the source of these data.

            ## The planetary constants

            The manifest carries, copied verbatim, the radii of Phobos and
            Deimos from NAIF's `pck00011.tpc` (the IAU Working Group on
            Cartographic Coordinates and Rotational Elements, 2015 report, as
            NAIF states it). The PCK file itself is not redistributed; its
            digest is recorded.

            ## Provenance

            `PROVENANCE.md` beside this notice records the source URLs,
            retrieval date, upstream digests, the exact extraction and its
            validation, the output digest and the coverage. The manifest pins
            every file's SHA-256, verified before use.
            """;
    }

    private static String provenance(String pckSha, Map<String, String> checksums, int moonsBytes,
                                     SpkKernel moons, String validation, int barycentreStates,
                                     double moonsEnd, Map<String, String> constants) {
        StringBuilder md = new StringBuilder();
        md.append("# Mars System pack provenance\n\n")
                .append("Generated resource - do not edit; regenerate with:\n\n")
                .append("```sh\nscripts/download-mars-sources.sh\n")
                .append("scripts/download-jovian-sources.sh\n")
                .append("scripts/download-solar-system-sources.sh\n")
                .append("make import-mars-system\n```\n\n")
                .append("Pack `").append(PACK_NAME).append("` version ").append(PACK_VERSION)
                .append(", audited ").append(AUDIT_DATE).append(".\n\n")
                .append("## Sources\n\n")
                .append("| input | URL | retrieved | SHA-256 |\n|---|---|---|---|\n")
                .append("| `mar099s.bsp` (NASA/JPL NAIF, MAR099 with DE440) | ").append(MAR099S_URL)
                .append(" | ").append(RETRIEVED).append(" | `").append(MAR099S_SHA256).append("` |\n")
                .append("| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | ")
                .append(PCK_URL).append(" | ").append(JovianPackMain.RETRIEVED).append(" | `")
                .append(pckSha).append("` |\n")
                .append("| `de440s.bsp` (the released Solar System pack's source; the barycentre identity proof) | ")
                .append(DE440S_URL).append(" | ").append(SolarSystemPackMain.RETRIEVED).append(" | `")
                .append(SolarSystemPackMain.PINNED.get("de440s.bsp")).append("` |\n\n")
                .append("## Extraction\n\n")
                .append("Tool: `juranometria.tool.MarsSystemPackMain` (this repository), using")
                .append(" `juranometria.solar.spk.SpkExcerpt`. `").append(MOONS_KERNEL_NAME)
                .append("`: 4 -> 401 (Phobos), 4 -> 402 (Deimos), SPK Type 2, from 2000-01-01 TDB")
                .append(" with a 31-day margin before it, to the source's own end, ")
                .append(String.format(Locale.ROOT, "%.1f", moonsEnd))
                .append(" s past J2000 TDB (2050-01-01 00:00 TDB), the moons' exclusive boundary")
                .append(" (#601, M-7a with the amendment). Whole records kept, coefficients")
                .append(" unchanged.\n\n")
                .append("Coverage of the written segments (seconds past J2000, TDB):\n\n");
        for (SpkKernel.Segment s : moons.segments()) {
            md.append(String.format(Locale.ROOT, "- %d -> %d: %.1f to %.1f\n",
                    s.center(), s.target(), s.startEt(), s.endEt()));
        }
        md.append("\n## Output\n\n")
                .append("| file | bytes | SHA-256 |\n|---|---|---|\n")
                .append("| `").append(MOONS_KERNEL_NAME).append("` | ").append(moonsBytes)
                .append(" | `").append(checksums.get(MOONS_KERNEL_NAME)).append("` |\n\n")
                .append("## Validation\n\n")
                .append("- The moons' kernel: ").append(validation).append(".\n")
                .append("- The barycentre: ").append(barycentreStates)
                .append(" states of mar099s.bsp's 0 -> 4 over the moons' interval, at 1.7-day")
                .append(" spacing, identical to the released de440s.bsp's; the moons stand on the")
                .append(" barycentre the planets pack carries.\n")
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
        md.append("\n## Applicable rules\n\n")
                .append("NAIF: unmodified kernels may be redistributed; a modified kernel must be")
                .append(" renamed and its attribution replaced with the modifier's, which this pack")
                .append(" does in the kernel's comment area and in `").append(NOTICE_NAME)
                .append("`. No fees or licensing are required.\n");
        return md.toString();
    }

    private static String manifest(Map<String, String> checksums, Map<String, String> constants,
                                   SpkKernel moons, double moonsEnd) {
        Map<String, String> m = new TreeMap<>();
        m.put("format.version", "1");
        m.put("pack.name", PACK_NAME);
        m.put("pack.version", Integer.toString(PACK_VERSION));
        m.put("audit.date", AUDIT_DATE);
        m.put("ephemeris", "MAR099 with DE440");
        m.put("ephemeris.moons.kernel", MOONS_KERNEL_NAME);
        m.put("ephemeris.moons.bodies", String.join(",", MOONS.stream().map(String::valueOf).toList()));
        m.put("ephemeris.moons.segments", Integer.toString(moons.segments().size()));
        m.put("ephemeris.moons.coverage.start.jd", String.format(Locale.ROOT, "%.1f", MOONS_START_JD));
        m.put("ephemeris.moons.coverage.end.jd.exclusive",
                String.format(Locale.ROOT, "%.1f", 2451545.0 + moonsEnd / 86400.0));
        m.put("ephemeris.moons.first.day", MOONS_FIRST_DAY);
        m.put("ephemeris.moons.last.year", "2049");
        m.put("source.mar099s.url", MAR099S_URL);
        m.put("source.mar099s.sha256", MAR099S_SHA256);
        m.put("source.pck.url", PCK_URL);
        m.put("source.pck.sha256", JovianPackMain.PINNED.get("pck00011.tpc"));
        m.put("source.de440s.sha256", SolarSystemPackMain.PINNED.get("de440s.bsp"));
        m.put("solar-system.kernel.sha256", RELEASED_SOLAR_KERNEL_SHA256);
        for (Map.Entry<String, String> c : constants.entrySet()) {
            m.put("pck." + c.getKey(), c.getValue());
        }
        m.put("terms", "NAIF rules for modified kernels; PCK values copied, file not redistributed");
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
