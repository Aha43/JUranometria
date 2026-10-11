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

import juranometria.solar.PlanetsPack;
import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;

/**
 * Builds the bundled planets ephemeris pack from its pinned raw inputs
 * (Sprint 47, issue #501; layout C as ruled on #500): one logical pack
 * with one JUranometria-modified excerpt of DE440 per planet, the
 * radii the contract names copied from NAIF's {@code pck00011.tpc}
 * into the manifest, and the notice and provenance the atlas keeps
 * beside bundled data.
 *
 * <p>Layout C, as ruled: the released Solar System and Jovian packs
 * are untouched - this builder proves their kernels still carry their
 * released digests - and every planet has a file of its own, so a
 * later release adds a planet without rewriting a released one. Venus
 * is the first: segments 0 → 2 (the Venus barycentre) and 2 → 299
 * (Venus about it, identically zero; proved) from the released pack's
 * own pinned source, over 1900–2100 with the released 31-day margin.
 * A planet whose centre is not in de440s (Mars and the outer planets)
 * will carry that relationship in its own file from its own authority,
 * ruled in its own study; a barycentre is never drawn as the planet.
 *
 * <p>Deterministic by construction, as the released packs' builders:
 * no timestamps other than the literal audit date, {@code Locale.ROOT}
 * formatting, and excerpts whose bytes follow from the source bytes,
 * the picks and the interval alone. Running it twice writes the same
 * pack, and a released planet's file is required to come out byte for
 * byte as it went in. Every excerpt is evaluated against the source
 * before writing and must agree exactly.
 *
 * <p>Run via {@code make import-planets}, after
 * {@code scripts/download-solar-system-sources.sh} and
 * {@code scripts/download-jovian-sources.sh} (the PCK).
 */
public final class PlanetsPackMain {

    static final String PACK_NAME = "planets";
    static final int PACK_VERSION = 1;
    static final String AUDIT_DATE = "2026-10-11";

    static final String NOTICE_NAME = "NOTICE-planets.md";
    static final String PROVENANCE_NAME = "PROVENANCE.md";

    static final String DE440S_URL = SolarSystemPackMain.DE440S_URL;
    static final String PCK_URL = JovianPackMain.PCK_URL;

    /** The released packs' kernels, whose digests must not move. */
    static final String RELEASED_SOLAR_KERNEL_SHA256 =
            JovianPackMain.RELEASED_SOLAR_KERNEL_SHA256;
    static final Map<String, String> RELEASED_JOVIAN_KERNELS = Map.of(
            "juranometria-jup365-jupiter-1900-2100.bsp",
            "efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92",
            "juranometria-jup365-galilean-2000-2100.bsp",
            "6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6");

    /** A planet of the pack: its file, its segments, its centre's radius keyword. */
    record Planet(String key, String label, String kernelName, List<SpkExcerpt.Pick> picks,
                  String radiusKeyword) {
    }

    /** The planets, in the order they were added; a release appends. */
    static final List<Planet> PLANETS = List.of(
            new Planet("venus", "Venus", "juranometria-de440-venus-1900-2100.bsp",
                    List.of(new SpkExcerpt.Pick(0, 2), new SpkExcerpt.Pick(2, 299)),
                    "BODY299_RADII"));

    /** The released interval and margin (SolarSystemPackMain), and its civil days. */
    static final double START_JD = SolarSystemPackMain.COVERAGE_START_JD;
    static final double END_JD = SolarSystemPackMain.COVERAGE_END_JD;
    static final double MARGIN_DAYS = SolarSystemPackMain.MARGIN_DAYS;
    static final String FIRST_DAY = "1900-01-01";
    static final String LAST_DAY = "2100-12-31";

    private PlanetsPackMain() {
    }

    public static void main(String[] args) throws IOException {
        Path solarRawDir = Path.of(args.length > 0 ? args[0] : "imports/raw/solar-system");
        Path jovianRawDir = Path.of(args.length > 1 ? args[1] : "imports/raw/jovian-system");
        Path outDir = Path.of(args.length > 2 ? args[2] : "src/resources/planets");

        Path de440s = solarRawDir.resolve("de440s.bsp");
        Path pckFile = jovianRawDir.resolve("pck00011.tpc");
        verifyPinned(de440s, SolarSystemPackMain.PINNED.get("de440s.bsp"),
                "scripts/download-solar-system-sources.sh");
        verifyPinned(pckFile, JovianPackMain.PINNED.get("pck00011.tpc"),
                "scripts/download-jovian-sources.sh");

        // The released packs, untouched.
        String solarSha = resourceSha("/resources/solar-system/"
                + "juranometria-de440-sun-emb-earth-moon-1900-2100.bsp");
        if (!RELEASED_SOLAR_KERNEL_SHA256.equals(solarSha)) {
            throw new IllegalStateException("the released Solar System kernel has"
                    + " moved: " + solarSha + "; nothing was written");
        }
        for (Map.Entry<String, String> k : new TreeMap<>(RELEASED_JOVIAN_KERNELS).entrySet()) {
            String sha = resourceSha("/resources/jovian-system/" + k.getKey());
            if (!k.getValue().equals(sha)) {
                throw new IllegalStateException("the released Jovian kernel "
                        + k.getKey() + " has moved: " + sha + "; nothing was written");
            }
        }

        byte[] source = Files.readAllBytes(de440s);
        SpkKernel full = SpkKernel.read(source);
        String pck = Files.readString(pckFile, StandardCharsets.US_ASCII);
        Map<String, String> constants = new LinkedHashMap<>();
        for (Planet p : PLANETS) {
            constants.putAll(PinnedPck.values(pck, List.of(p.radiusKeyword)));
        }
        constants.putAll(PinnedPck.values(pck, List.of("BODY10_RADII")));

        double startEt = (START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;
        double endEt = (END_JD + MARGIN_DAYS - 2451545.0) * 86400.0;
        Map<String, byte[]> kernels = new LinkedHashMap<>();
        Map<String, String> validations = new LinkedHashMap<>();
        for (Planet p : PLANETS) {
            byte[] kernel = SpkExcerpt.of(source, p.picks, startEt, endEt,
                    "JURANOMETRIA DE440 " + p.label.toUpperCase(Locale.ROOT)
                            + " 1900-2100 V" + PACK_VERSION,
                    comment(p, full.comments()));
            validations.put(p.key, prove(SpkKernel.read(kernel), full, p));
            kernels.put(p.key, kernel);
        }
        String venusZero = zero(full, 2, 299);

        // A released planet's file never moves: if the pack is already
        // here, each planet's file must come out as it went in.
        for (Planet p : PLANETS) {
            Path existing = outDir.resolve(p.kernelName);
            if (Files.isRegularFile(existing)
                    && !java.util.Arrays.equals(Files.readAllBytes(existing), kernels.get(p.key))) {
                throw new IllegalStateException(p.kernelName + " would change; a released"
                        + " planet's file is never rewritten. Nothing was written.");
            }
        }

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);
        Map<String, String> checksums = new LinkedHashMap<>();
        for (Planet p : PLANETS) {
            write(outDir.resolve(p.kernelName), kernels.get(p.key), checksums);
        }
        write(outDir.resolve(NOTICE_NAME), notice().getBytes(StandardCharsets.UTF_8), checksums);
        write(outDir.resolve(PROVENANCE_NAME), provenance(
                PinnedInputs.sha256Hex(source),
                PinnedInputs.sha256Hex(pck.getBytes(StandardCharsets.US_ASCII)),
                checksums, kernels, validations, venusZero, constants)
                .getBytes(StandardCharsets.UTF_8), checksums);
        Files.writeString(outDir.resolve("manifest.properties"),
                manifest(checksums, constants, kernels), StandardCharsets.UTF_8);

        PlanetsPack loaded = PlanetsPack.load(name -> {
            try {
                return Files.newInputStream(outDir.resolve(name));
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        System.out.println("planets pack written to " + outDir);
        for (Planet p : PLANETS) {
            System.out.println("  " + p.kernelName + ": " + kernels.get(p.key).length
                    + " bytes, sha256 " + checksums.get(p.kernelName));
            System.out.println("  " + validations.get(p.key));
        }
        System.out.println("  2 -> 299: " + venusZero);
        System.out.println("  released Solar System and Jovian kernels untouched");
        System.out.println("  reloaded and verified: " + loaded.manifest().get("pack.name")
                + " v" + loaded.manifest().get("pack.version") + ", planets "
                + loaded.planets());
    }

    private static String resourceSha(String resource) throws IOException {
        try (InputStream in = PlanetsPackMain.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " is not on the classpath;"
                        + " run make classes");
            }
            return PinnedInputs.sha256Hex(in.readAllBytes());
        }
    }

    private static void verifyPinned(Path file, String expected, String script)
            throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("missing raw input " + file + "; run " + script);
        }
        String actual = PinnedInputs.sha256Hex(Files.readAllBytes(file));
        if (!actual.equals(expected)) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "raw input %s fails its pinned checksum: expected %s, found %s",
                    file, expected, actual));
        }
    }

    /** Every pick of a planet's excerpt against the source at 1.7-day spacing. */
    private static String prove(SpkKernel excerpt, SpkKernel source, Planet p) {
        int states = 0;
        double worst = 0.0;
        for (double jd = START_JD; jd <= END_JD; jd += 1.7) {
            double et = (jd - 2451545.0) * 86400.0;
            for (SpkExcerpt.Pick pick : p.picks) {
                SpkKernel.State a = excerpt.state(pick.center(), pick.target(), et);
                SpkKernel.State b = source.state(pick.center(), pick.target(), et);
                worst = Math.max(worst, a.position().minus(b.position()).length());
                worst = Math.max(worst, a.velocity().minus(b.velocity()).length());
                states++;
            }
        }
        if (worst != 0.0) {
            throw new IllegalStateException("the " + p.label + " excerpt disagrees with"
                    + " its source by " + worst + "; nothing was written");
        }
        return String.format(Locale.ROOT, "%s: %d states over the coverage at 1.7-day"
                + " spacing, every segment: position and velocity identical to the"
                + " source, worst difference %.1f", p.kernelName, states, worst);
    }

    /** A planet about its barycentre, where the planet has no moon: zero, proved. */
    private static String zero(SpkKernel source, int center, int target) {
        int states = 0;
        for (double jd = START_JD; jd <= END_JD; jd += 1.7) {
            SpkKernel.State s = source.state(center, target, (jd - 2451545.0) * 86400.0);
            if (s.position().length() != 0.0 || s.velocity().length() != 0.0) {
                throw new IllegalStateException(center + " -> " + target
                        + " is not identically zero; nothing was written");
            }
            states++;
        }
        return String.format(Locale.ROOT, "%d states, position and velocity identically"
                + " zero: the planet is its barycentre", states);
    }

    private static String comment(Planet p, String sourceComments) {
        StringBuilder text = new StringBuilder();
        text.append(p.kernelName).append(":\n")
                .append("JUranometria planets pack, version ").append(PACK_VERSION)
                .append(": ").append(p.label).append(", a subset of the JPL planetary\n")
                .append("ephemeris DE440, MODIFIED by JUranometria\n")
                .append("(https://github.com/Aha43/JUranometria).\n")
                .append("\n")
                .append("This is not an original JPL/NAIF file. It was produced by\n")
                .append("juranometria.tool.PlanetsPackMain from the official kernel\n")
                .append("de440s.bsp, obtained from NASA/JPL NAIF at\n")
                .append("  ").append(DE440S_URL).append("\n")
                .append("(SHA-256 ").append(SolarSystemPackMain.PINNED.get("de440s.bsp")).append(")\n")
                .append("by keeping only these SPK Type 2 segments\n");
        for (SpkExcerpt.Pick pick : p.picks) {
            text.append(String.format(Locale.ROOT, "  %d -> %d%n", pick.center(), pick.target()));
        }
        text.append("over the interval 1900-01-01 to 2101-01-01 (TDB) with a margin\n")
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
            # Notice: planets ephemeris pack

            This pack carries **JUranometria-modified excerpts of the JPL
            planetary ephemeris DE440**, one file per planet, so the atlas can
            compute where the planets are, how large they look and how much
            of each is lit, without any network access at build, test or run
            time. It stands beside the Solar System pack and the Jovian System
            pack, which it does not change: the Sun, the Earth and the Moon
            are computed from the Solar System pack alone, as released, and
            each planet's place is measured from them.

            ## One pack, one file per planet

            The planets are one logical, versioned pack: one manifest, one
            validation boundary, one notice and one provenance. Each planet
            has a kernel file of its own, as ruled on #500: a later release
            adds a planet's file and its lines in the manifest, and a planet's
            file, once released, is never rewritten - its name and digest
            stand.

            ## The ephemeris excerpts

            `juranometria-de440-venus-1900-2100.bsp` was produced by
            JUranometria from the official kernel `de440s.bsp` distributed by
            NASA/JPL's Navigation and Ancillary Information Facility (NAIF).
            It keeps two of that kernel's segments - the Venus barycentre
            relative to the solar-system barycentre, and Venus relative to
            its barycentre, which is identically zero because Venus has no
            moon - over the years 1900 to 2100, with every coefficient
            unchanged. It is a **modified kernel** under NAIF's rules and is
            named, annotated and attributed as such; it is not an original JPL
            file and JPL did not produce it.

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

            ## The planetary constants

            The manifest carries, copied verbatim from NAIF's `pck00011.tpc`
            (the IAU Working Group on Cartographic Coordinates and Rotational
            Elements, 2015 report, as NAIF states it), each planet's radii and
            the Sun's. Venus's radius, 6051.8 km, is its solid body; the
            visible cloud deck lies higher, and the atlas states, never draws,
            that difference. The PCK file itself is not redistributed; its
            digest is recorded.

            ## Provenance

            `PROVENANCE.md` beside this notice records, for each planet's
            file, the source URL, retrieval date, upstream digest, the exact
            extraction and its validation, the output digest and the coverage.
            The manifest pins every file's SHA-256, verified before use.
            """;
    }

    private static String provenance(String sourceSha, String pckSha,
                                     Map<String, String> checksums,
                                     Map<String, byte[]> kernels,
                                     Map<String, String> validations, String venusZero,
                                     Map<String, String> constants) throws IOException {
        StringBuilder md = new StringBuilder();
        md.append("# Planets pack provenance\n\n")
                .append("Generated resource - do not edit; regenerate with:\n\n")
                .append("```sh\nscripts/download-solar-system-sources.sh\n")
                .append("scripts/download-jovian-sources.sh\n")
                .append("make import-planets\n```\n\n")
                .append("Pack `").append(PACK_NAME).append("` version ")
                .append(PACK_VERSION).append(", audited ").append(AUDIT_DATE)
                .append(".\n\n")
                .append("## Sources\n\n")
                .append("| input | URL | retrieved | SHA-256 |\n|---|---|---|---|\n")
                .append("| `de440s.bsp` (NASA/JPL NAIF, DE440 short kernel; the released Solar System pack's source) | ")
                .append(DE440S_URL).append(" | ").append(SolarSystemPackMain.RETRIEVED)
                .append(" | `").append(sourceSha).append("` |\n")
                .append("| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | ")
                .append(PCK_URL).append(" | ").append(JovianPackMain.RETRIEVED).append(" | `")
                .append(pckSha).append("` |\n\n")
                .append("## Extraction\n\n")
                .append("Tool: `juranometria.tool.PlanetsPackMain` (this repository),")
                .append(" using `juranometria.solar.spk.SpkExcerpt`. One file per planet;")
                .append(" each keeps the planet's de440s segments, SPK Type 2, over")
                .append(" 1900-01-01 to 2101-01-01 TDB with a 31-day margin at each end;")
                .append(" whole records kept from the first covering the start to the last")
                .append(" covering the end, coefficients unchanged.\n\n");
        for (Planet p : PLANETS) {
            SpkKernel k = SpkKernel.read(kernels.get(p.key));
            md.append("`").append(p.kernelName).append("` (").append(p.label)
                    .append("), coverage of the written segments (seconds past J2000, TDB):\n\n");
            for (SpkKernel.Segment s : k.segments()) {
                md.append(String.format(Locale.ROOT, "- %d -> %d: %.1f to %.1f\n",
                        s.center(), s.target(), s.startEt(), s.endEt()));
            }
            md.append('\n');
        }
        md.append("## Output\n\n| file | bytes | SHA-256 |\n|---|---|---|\n");
        for (Planet p : PLANETS) {
            md.append("| `").append(p.kernelName).append("` | ").append(kernels.get(p.key).length)
                    .append(" | `").append(checksums.get(p.kernelName)).append("` |\n");
        }
        md.append("\n## Validation\n\n");
        for (Planet p : PLANETS) {
            md.append("- ").append(validations.get(p.key)).append(".\n");
        }
        md.append("- Venus about its barycentre (2 -> 299): ").append(venusZero).append(".\n")
                .append("- The released Solar System kernel is untouched: sha256 `")
                .append(RELEASED_SOLAR_KERNEL_SHA256).append("`, verified before writing.\n");
        for (Map.Entry<String, String> k : new TreeMap<>(RELEASED_JOVIAN_KERNELS).entrySet()) {
            md.append("- The released Jovian kernel `").append(k.getKey())
                    .append("` is untouched: sha256 `").append(k.getValue())
                    .append("`, verified before writing.\n");
        }
        md.append("- A released planet's file is never rewritten: the builder refuses")
                .append(" to write a pack in which one would change.\n\n")
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
                .append(" licensing are required.\n");
        return md.toString();
    }

    private static String manifest(Map<String, String> checksums,
                                   Map<String, String> constants,
                                   Map<String, byte[]> kernels) throws IOException {
        Map<String, String> m = new TreeMap<>();
        m.put("format.version", "1");
        m.put("pack.name", PACK_NAME);
        m.put("pack.version", Integer.toString(PACK_VERSION));
        m.put("audit.date", AUDIT_DATE);
        m.put("ephemeris", "DE440");
        StringBuilder planets = new StringBuilder();
        for (Planet p : PLANETS) {
            planets.append(planets.length() == 0 ? "" : ",").append(p.key);
            String prefix = "ephemeris." + p.key + ".";
            StringBuilder bodies = new StringBuilder();
            for (SpkExcerpt.Pick pick : p.picks) {
                bodies.append(bodies.length() == 0 ? "" : ",").append(pick.target());
            }
            m.put(prefix + "kernel", p.kernelName);
            m.put(prefix + "bodies", bodies.toString());
            m.put(prefix + "segments", Integer.toString(
                    SpkKernel.read(kernels.get(p.key)).segments().size()));
            m.put(prefix + "coverage.start.jd", String.format(Locale.ROOT, "%.1f", START_JD));
            m.put(prefix + "coverage.end.jd", String.format(Locale.ROOT, "%.1f", END_JD));
            m.put(prefix + "first.day", FIRST_DAY);
            m.put(prefix + "last.day", LAST_DAY);
        }
        m.put("pack.planets", planets.toString());
        m.put("source.de440s.url", DE440S_URL);
        m.put("source.de440s.sha256", SolarSystemPackMain.PINNED.get("de440s.bsp"));
        m.put("source.pck.url", PCK_URL);
        m.put("source.pck.sha256", JovianPackMain.PINNED.get("pck00011.tpc"));
        m.put("solar-system.kernel.sha256", RELEASED_SOLAR_KERNEL_SHA256);
        for (Map.Entry<String, String> c : constants.entrySet()) {
            m.put("pck." + c.getKey(), c.getValue());
        }
        m.put("terms", "NAIF rules for modified kernels; PCK values copied,"
                + " file not redistributed");
        for (Map.Entry<String, String> c : checksums.entrySet()) {
            m.put("checksum." + c.getKey(), c.getValue());
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            text.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        return text.toString();
    }

    private static void write(Path path, byte[] bytes, Map<String, String> checksums)
            throws IOException {
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

    /** The PCK's values for named keywords, verbatim, as the Jovian builder reads them. */
    static final class PinnedPck {
        private PinnedPck() {
        }

        static Map<String, String> values(String pck, List<String> keywords) {
            Map<String, String> out = new LinkedHashMap<>();
            for (String keyword : keywords) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s*" + keyword
                        + "\\s*=\\s*\\(([^)]*)\\)", java.util.regex.Pattern.MULTILINE).matcher(pck);
                String last = null;
                while (m.find()) {
                    last = String.join(",", m.group(1).trim().split("\\s+"));
                }
                if (last == null) {
                    throw new IllegalStateException("pck00011.tpc does not assign " + keyword);
                }
                out.put(keyword, last);
            }
            return out;
        }
    }
}
