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

import juranometria.solar.NeptuneSystemPack;
import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;

/**
 * Builds the bundled Neptune System pack from its pinned raw inputs (issue
 * #655; the contract ruled on #654): two JUranometria-modified excerpts of
 * NAIF's {@code nep098_part-1.bsp}, the radii, the pole and its periodic
 * term copied from NAIF's {@code pck00011.tpc} into the manifest, and the
 * notice and provenance the atlas keeps beside bundled data - Uranus's
 * layout, a planet kernel and a moons kernel from one satellite kernel.
 *
 * <ul>
 *   <li><b>Neptune</b> (N-1a, N-2): the Neptune barycentre about the
 *   solar-system barycentre, 0 → 8, from the kernel's own DE442 segment,
 *   1900-2100 with the released 31-day margins. The barycentre stands for
 *   the planet: this builder measures Neptune's centre (8 → 899) against it
 *   again, hourly over 1900-2100, and refuses above
 *   {@link #CENTRE_OFFSET_LIMIT_KM} - the premise of N-2, re-measured,
 *   never assumed.</li>
 *   <li><b>The moons</b> (N-6a): Triton and Nereid about the Neptune
 *   barycentre, 8 → 801 and 802, from NEP098, 2000-2100 with the
 *   margins.</li>
 * </ul>
 *
 * <p>The pole is the IAU 2015 model with its periodic term over N (N-4):
 * the builder refuses a PCK whose periodic coefficients or angle differ from
 * those #654 measured. The released Solar System, Jovian, planets, Saturn
 * System, Mars System and Uranus System kernels are untouched: their digests
 * are proved before writing. Deterministic by
 * construction, as the released builders.
 *
 * <p>Run via {@code make import-neptune-system}, after
 * {@code scripts/download-neptune-sources.sh} and
 * {@code scripts/download-jovian-sources.sh} (the PCK).
 */
public final class NeptuneSystemPackMain {

    static final String PACK_NAME = "neptune-system";
    static final int PACK_VERSION = 1;
    static final String AUDIT_DATE = "2026-10-29";
    static final String RETRIEVED = "2026-10-29";

    static final String NEPTUNE_KERNEL_NAME = "juranometria-de442-neptune-1900-2100.bsp";
    static final String MOONS_KERNEL_NAME = "juranometria-nep098-moons-2000-2100.bsp";
    static final String NOTICE_NAME = "NOTICE-neptune-system.md";
    static final String PROVENANCE_NAME = "PROVENANCE.md";

    static final String NEP098_SOURCE_URL = "https://naif.jpl.nasa.gov/pub/naif/"
            + "generic_kernels/spk/satellites/nep098_part-1.bsp";
    static final String NEP098_SOURCE_SHA256 =
            "5327f47abba438d993237ff676ab03b728778a965bb238f86de883c0ad7790b8";
    static final String PCK_URL = JovianPackMain.PCK_URL;

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
            "/resources/planets/juranometria-de440-mars-1900-2100.bsp",
            "c59b91b0188dc540906916c067238c8d311fd718f65b909825720e54e22e7ffb",
            "/resources/saturn-system/juranometria-sat441-saturn-1900-2100.bsp",
            "28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d",
            "/resources/saturn-system/juranometria-sat441-moons-2000-2100.bsp",
            "8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b",
            "/resources/mars-system/juranometria-mar099-moons-2000-2050.bsp",
            "cd84a9bfd574ebaeec63893d30735129de030e7e361af6c0f465f634629bc208",
            "/resources/uranus-system/juranometria-de442-uranus-1900-2100.bsp",
            "21964f569b7fe33b2a5648090215942e86542af8a5718f93fa5987259af25c9e",
            "/resources/uranus-system/juranometria-ura182-moons-2000-2100.bsp",
            "6f8ff87802472a0c30f4c1779d6b1dec004922ce5573795200b518a6ebc19180");

    static final int SSB = 0;
    static final int NEPTUNE_BARYCENTRE = 8;
    static final int NEPTUNE = 899;
    /** The ruled moons (#654, N-6a), outward from Neptune. */
    static final List<Integer> MOONS = List.of(801, 802);
    static final List<SpkExcerpt.Pick> MOON_PICKS = MOONS.stream()
            .map(id -> new SpkExcerpt.Pick(NEPTUNE_BARYCENTRE, id)).toList();
    static final List<SpkExcerpt.Pick> NEPTUNE_PICKS =
            List.of(new SpkExcerpt.Pick(SSB, NEPTUNE_BARYCENTRE));

    /** The planet's and the moons' intervals, the released packs' and Saturn's moons'. */
    static final double START_JD = SolarSystemPackMain.COVERAGE_START_JD;
    static final double END_JD = SolarSystemPackMain.COVERAGE_END_JD;
    static final double MOONS_START_JD = JovianPackMain.MOONS_START_JD;
    static final double MARGIN_DAYS = SolarSystemPackMain.MARGIN_DAYS;
    static final String FIRST_DAY = "1900-01-01";
    static final String LAST_DAY = "2100-12-31";
    static final String MOONS_FIRST_DAY = "2000-01-01";

    /**
     * The builder refuses when the kernel puts Neptune's centre farther than
     * this from its barycentre: the bound N-2a ruled, above the 74.190 km
     * #654 measured (0.0036″ seen from the Earth).
     */
    static final double CENTRE_OFFSET_LIMIT_KM = 80.0;

    /** The PCK keywords copied into the manifest, in manifest order. */
    static final List<String> PCK_KEYWORDS = List.of("BODY899_RADII", "BODY899_POLE_RA",
            "BODY899_POLE_DEC", "BODY899_NUT_PREC_RA", "BODY899_NUT_PREC_DEC",
            "BODY8_NUT_PREC_ANGLES", "BODY801_RADII", "BODY802_RADII", "BODY10_RADII");
    /**
     * The periodic pole term #654 measured and N-4 ruled: 0.70 sin N in RA and
     * -0.51 cos N in Dec, N = 357.85° + 52.316° T, every other coefficient
     * zero. The builder refuses a PCK that says otherwise.
     */
    static final double NUT_PREC_RA = 0.70;
    static final double NUT_PREC_DEC = -0.51;
    static final double[] ANGLE_N = {357.85, 52.316};

    private NeptuneSystemPackMain() {
    }

    public static void main(String[] args) throws IOException {
        Path rawDir = Path.of(args.length > 0 ? args[0] : "imports/raw/neptune-system");
        Path jovianRawDir = Path.of(args.length > 1 ? args[1] : "imports/raw/jovian-system");
        Path outDir = Path.of(args.length > 2 ? args[2] : "src/resources/neptune-system");

        verifyPinned(rawDir.resolve("nep098_part-1.bsp"), NEP098_SOURCE_SHA256,
                "scripts/download-neptune-sources.sh");
        verifyPinned(jovianRawDir.resolve("pck00011.tpc"), JovianPackMain.PINNED.get("pck00011.tpc"),
                "scripts/download-jovian-sources.sh");

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

        byte[] source = Files.readAllBytes(rawDir.resolve("nep098_part-1.bsp"));
        SpkKernel full = SpkKernel.read(source);
        String pck = Files.readString(jovianRawDir.resolve("pck00011.tpc"),
                StandardCharsets.US_ASCII);
        Map<String, String> constants = PlanetsPackMain.PinnedPck.values(pck, PCK_KEYWORDS);
        String periodic = periodicTerm(constants);

        double start = (START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;
        double end = (END_JD + MARGIN_DAYS - 2451545.0) * 86400.0;
        double moonsStart = (MOONS_START_JD - MARGIN_DAYS - 2451545.0) * 86400.0;

        String barycentreSource = barycentreSource(full);
        String centre = centreOffset(full);

        byte[] neptuneKernel = SpkExcerpt.of(source, NEPTUNE_PICKS, start, end,
                "JURANOMETRIA DE442 NEPTUNE 1900-2100 V" + PACK_VERSION,
                comment(NEPTUNE_KERNEL_NAME, full.comments(), false));
        byte[] moonsKernel = SpkExcerpt.of(source, MOON_PICKS, moonsStart, end,
                "JURANOMETRIA NEP098 MOONS 2000-2100 V" + PACK_VERSION,
                comment(MOONS_KERNEL_NAME, full.comments(), true));
        String neptuneProof = prove(SpkKernel.read(neptuneKernel), full, NEPTUNE_PICKS, start, end);
        String moonsProof = prove(SpkKernel.read(moonsKernel), full, MOON_PICKS, moonsStart, end);

        ensureSafeToClean(outDir);
        Files.createDirectories(outDir);
        Map<String, String> checksums = new LinkedHashMap<>();
        write(outDir.resolve(NEPTUNE_KERNEL_NAME), neptuneKernel, checksums);
        write(outDir.resolve(MOONS_KERNEL_NAME), moonsKernel, checksums);
        write(outDir.resolve(NOTICE_NAME), notice().getBytes(StandardCharsets.UTF_8), checksums);
        write(outDir.resolve(PROVENANCE_NAME), provenance(
                PinnedInputs.sha256Hex(pck.getBytes(StandardCharsets.US_ASCII)), checksums,
                neptuneKernel.length, moonsKernel.length, SpkKernel.read(neptuneKernel),
                SpkKernel.read(moonsKernel), neptuneProof, moonsProof, barycentreSource, centre,
                constants).getBytes(StandardCharsets.UTF_8), checksums);
        Files.writeString(outDir.resolve("manifest.properties"),
                manifest(checksums, constants, SpkKernel.read(neptuneKernel),
                        SpkKernel.read(moonsKernel)), StandardCharsets.UTF_8);

        NeptuneSystemPack loaded = NeptuneSystemPack.load(name -> {
            try {
                return Files.newInputStream(outDir.resolve(name));
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        System.out.println("neptune-system pack written to " + outDir);
        System.out.println("  " + NEPTUNE_KERNEL_NAME + ": " + neptuneKernel.length
                + " bytes, sha256 " + checksums.get(NEPTUNE_KERNEL_NAME));
        System.out.println("  " + MOONS_KERNEL_NAME + ": " + moonsKernel.length
                + " bytes, sha256 " + checksums.get(MOONS_KERNEL_NAME));
        System.out.println("  the barycentre: " + barycentreSource);
        System.out.println("  Neptune about its barycentre: " + centre);
        System.out.println("  " + neptuneProof);
        System.out.println("  " + moonsProof);
        System.out.println("  the pole: " + periodic);
        System.out.println("  released Solar System, Jovian, planets, Saturn System, Mars System"
                + " and Uranus System kernels untouched");
        System.out.println("  reloaded and verified: " + loaded.manifest().get("pack.name")
                + " v" + loaded.manifest().get("pack.version") + ", moons " + loaded.moons());
    }

    /**
     * Neptune's periodic pole term is the one #654 measured (N-4): one
     * coefficient in RA and one in Dec, over the first BODY8 angle, N.
     */
    static String periodicTerm(Map<String, String> constants) {
        double[] ra = numbers(constants.get("BODY899_NUT_PREC_RA"));
        double[] dec = numbers(constants.get("BODY899_NUT_PREC_DEC"));
        double[] angles = numbers(constants.get("BODY8_NUT_PREC_ANGLES"));
        boolean held = ra.length == dec.length && angles.length >= 2 * ra.length
                && ra[0] == NUT_PREC_RA && dec[0] == NUT_PREC_DEC
                && angles[0] == ANGLE_N[0] && angles[1] == ANGLE_N[1];
        for (int k = 1; held && k < ra.length; k++) {
            held = ra[k] == 0.0 && dec[k] == 0.0;
        }
        if (!held) {
            throw new IllegalStateException("pck00011.tpc's periodic pole terms for Neptune are not"
                    + " the ruled 0.70 sin N and -0.51 cos N over N = 357.85 + 52.316 T (N-4);"
                    + " nothing was written");
        }
        return String.format(Locale.ROOT, "RA %+.2f sin N and Dec %+.2f cos N over"
                + " N = %.2f° + %.3f° T, every other coefficient zero, as ruled (N-4)",
                NUT_PREC_RA, NUT_PREC_DEC, ANGLE_N[0], ANGLE_N[1]);
    }

    private static double[] numbers(String commaSeparated) {
        String[] fields = commaSeparated.split(",");
        double[] out = new double[fields.length];
        for (int i = 0; i < fields.length; i++) {
            out[i] = Double.parseDouble(fields[i].strip().replace('D', 'E'));
        }
        return out;
    }

    /** The kernel's 0 -> 8 is DE442's, as N-1a rules: the segment says so by name. */
    static String barycentreSource(SpkKernel full) {
        List<SpkKernel.Segment> segments = full.segments(SSB, NEPTUNE_BARYCENTRE);
        if (segments.isEmpty()) {
            throw new IllegalStateException("nep098_part-1 carries no 0 -> 8 segment");
        }
        for (SpkKernel.Segment s : segments) {
            if (!s.name().strip().startsWith("DE-0442")) {
                throw new IllegalStateException("nep098_part-1's 0 -> 8 segment is "
                        + s.name().strip() + ", not DE442's, as N-1a rules; nothing was written");
            }
        }
        return segments.size() + " segment(s) of 0 -> 8, named "
                + segments.get(0).name().strip() + ": DE442's, as ruled (N-1a)";
    }

    /**
     * Neptune's centre about its barycentre (8 -> 899), hourly over 1900-2100:
     * #654's measurement, repeated, held to {@link #CENTRE_OFFSET_LIMIT_KM}.
     */
    static String centreOffset(SpkKernel full) {
        double start = (START_JD - 2451545.0) * 86400.0;
        double end = (END_JD - 2451545.0) * 86400.0;
        double worst = 0.0;
        long states = 0;
        for (double et = start; et <= end; et += 3600.0) {
            worst = Math.max(worst, full.state(NEPTUNE_BARYCENTRE, NEPTUNE, et).position().length());
            states++;
        }
        if (!(worst <= CENTRE_OFFSET_LIMIT_KM)) {
            throw new IllegalStateException(String.format(Locale.ROOT, "nep098 puts Neptune's"
                    + " centre %.3f km from its barycentre, beyond the %.1f km the ruling N-2"
                    + " rests on; nothing was written", worst, CENTRE_OFFSET_LIMIT_KM));
        }
        return String.format(Locale.ROOT, "%d hourly states over 1900-2100, the centre at most"
                + " %.3f km from the barycentre (limit %.1f km): the planet is drawn from its"
                + " barycentre, as ruled (N-2)", states, worst, CENTRE_OFFSET_LIMIT_KM);
    }

    private static String resourceSha(String resource) throws IOException {
        try (InputStream in = NeptuneSystemPackMain.class.getResourceAsStream(resource)) {
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

    /** Every kept segment against the source at 1.7-day spacing and at both ends. */
    private static String prove(SpkKernel excerpt, SpkKernel source, List<SpkExcerpt.Pick> picks,
                                double startEt, double endEt) {
        int states = 0;
        double worst = 0.0;
        List<Double> instants = new ArrayList<>();
        for (double et = startEt; et < endEt; et += 1.7 * 86400.0) {
            instants.add(et);
        }
        instants.add(endEt);
        for (double et : instants) {
            for (SpkExcerpt.Pick pick : picks) {
                SpkKernel.State a = excerpt.state(pick.center(), pick.target(), et);
                SpkKernel.State b = source.state(pick.center(), pick.target(), et);
                worst = Math.max(worst, a.position().minus(b.position()).length());
                worst = Math.max(worst, a.velocity().minus(b.velocity()).length());
                states++;
            }
        }
        if (worst > 1e-9) {
            throw new IllegalStateException("the excerpt disagrees with its source by " + worst
                    + " km; nothing was written");
        }
        StringBuilder bodies = new StringBuilder();
        for (SpkExcerpt.Pick p : picks) {
            bodies.append(bodies.length() == 0 ? "" : ", ").append(p.center()).append(" -> ")
                    .append(p.target());
        }
        return String.format(Locale.ROOT, "%s: %d states at 1.7-day spacing and the last"
                + " instant, position and velocity as the source's, worst difference %.3g km",
                bodies, states, worst);
    }

    private static String comment(String kernelName, String sourceComments, boolean moons) {
        StringBuilder text = new StringBuilder();
        text.append(kernelName).append(":\n")
                .append("JUranometria Neptune System pack, version ").append(PACK_VERSION)
                .append(moons ? ": a subset of the JPL satellite ephemeris NEP098,\n"
                        : ": the Neptune barycentre from DE442, as merged by NAIF\n")
                .append("MODIFIED by JUranometria (https://github.com/Aha43/JUranometria).\n")
                .append("\n")
                .append("This is not an original JPL/NAIF file. It was produced by\n")
                .append("juranometria.tool.NeptuneSystemPackMain from the official kernel\n")
                .append("nep098_part-1.bsp, obtained from NASA/JPL NAIF at\n")
                .append("  ").append(NEP098_SOURCE_URL).append("\n")
                .append("(SHA-256 ").append(NEP098_SOURCE_SHA256).append(")\n");
        if (moons) {
            text.append("by keeping only these SPK Type 2 segments\n")
                    .append("  8 -> 801  Triton from the Neptune barycentre\n")
                    .append("  8 -> 802  Nereid from the Neptune barycentre\n")
                    .append("over the interval 2000-01-01 to 2101-01-01 (TDB), with a margin\n");
        } else {
            text.append("by keeping only the SPK Type 2 segment\n")
                    .append("  0 -> 8    the Neptune barycentre from the solar-system barycentre\n")
                    .append("over the interval 1900-01-01 to 2101-01-01 (TDB), with a margin\n");
        }
        text.append("of 31 days at each end. Every Chebyshev coefficient kept is the\n")
                .append("source's coefficient, unchanged; only whole records outside the\n")
                .append("interval were removed. Full provenance, digests and validation\n")
                .append("are in PROVENANCE.md beside this file.\n")
                .append("\n")
                .append("The satellite ephemeris is NEP098 (JPL Solar System Dynamics);\n")
                .append("the Neptune barycentre is DE442's (JPL). The source kernel's own\n")
                .append("comment area follows, retained as source attribution:\n")
                .append("\n")
                .append("---- nep098_part-1.bsp comment area, as distributed by NAIF ----\n");
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
            # Notice: Neptune System ephemeris pack

            This pack carries two **JUranometria-modified excerpts of NAIF's
            Neptune satellite kernel `nep098_part-1.bsp`**, so the atlas can
            compute where Neptune, Triton and Nereid are without any network
            access at build, test or run time. It stands beside the Solar
            System, Jovian, planets, Saturn System, Mars System and Uranus
            System packs, which it does not change.

            ## The years

            Neptune answers from 1900 to 2100. Triton and Nereid answer from
            2000 to 2100, as ruled on #654; before 2000 Neptune still answers
            and the moons refuse, naming their years.

            ## The ephemeris excerpts

            `juranometria-de442-neptune-1900-2100.bsp` keeps the Neptune
            barycentre about the solar-system barycentre from DE442, as NAIF
            merged it into `nep098_part-1.bsp`; the atlas takes that
            barycentre as the planet, Neptune's centre lying within 80 km of
            it. `juranometria-nep098-moons-2000-2100.bsp` keeps Triton and
            Nereid about the Neptune barycentre from the satellite solution
            NEP098. Both were produced by JUranometria from the official
            kernel distributed by NASA/JPL's Navigation and Ancillary
            Information Facility (NAIF), every coefficient unchanged. They are
            **modified kernels** under NAIF's rules and are named, annotated
            and attributed as such; they are not original JPL files and JPL
            did not produce them.

            NAIF's rules, quoted: "Redistribution of SPICE kernels distributed
            by NAIF is permitted as long as they have not been modified." "If a
            kernel distributed by NAIF has been modified in any way, any
            embedded or otherwise allied attribution of the original kernel
            producer must be replaced with the name and institution of whomever
            has made the last modification." "The file name must also be
            changed to help avoid confusion." "No fees or licensing are
            required."

            The satellite ephemeris is NEP098, from JPL Solar System
            Dynamics, and the planetary ephemeris is DE442, from JPL,
            distributed together by NAIF as `nep098_part-1.bsp`. JUranometria
            acknowledges NASA/JPL NAIF, JPL Solar System Dynamics and the
            ephemeris authors as the source of these data.

            ## The planetary constants

            The manifest carries, copied verbatim, Neptune's radii, its pole
            with the periodic term and its angles, and Triton's and Nereid's
            radii from NAIF's `pck00011.tpc` (the IAU Working
            Group on Cartographic Coordinates and Rotational Elements, 2015
            report, as NAIF states it). The PCK file itself is not
            redistributed; its digest is recorded.

            ## Provenance

            `PROVENANCE.md` beside this notice records the source URLs,
            retrieval date, upstream digests, the exact extraction and its
            validation, the output digests and the coverage. The manifest pins
            every file's SHA-256, verified before use.
            """;
    }

    private static String provenance(String pckSha, Map<String, String> checksums, int neptuneBytes,
                                     int moonsBytes, SpkKernel neptune, SpkKernel moons,
                                     String neptuneProof, String moonsProof,
                                     String barycentreSource, String centre,
                                     Map<String, String> constants) {
        StringBuilder md = new StringBuilder();
        md.append("# Neptune System pack provenance\n\n")
                .append("Generated resource - do not edit; regenerate with:\n\n")
                .append("```sh\nscripts/download-neptune-sources.sh\n")
                .append("scripts/download-jovian-sources.sh\n")
                .append("make import-neptune-system\n```\n\n")
                .append("Pack `").append(PACK_NAME).append("` version ").append(PACK_VERSION)
                .append(", audited ").append(AUDIT_DATE).append(".\n\n")
                .append("## Sources\n\n")
                .append("| input | URL | retrieved | SHA-256 |\n|---|---|---|---|\n")
                .append("| `nep098_part-1.bsp` (NASA/JPL NAIF, NEP098 with DE442) | ")
                .append(NEP098_SOURCE_URL).append(" | ").append(RETRIEVED).append(" | `")
                .append(NEP098_SOURCE_SHA256).append("` |\n")
                .append("| `pck00011.tpc` (NASA/JPL NAIF, IAU 2015 constants; values copied, file not redistributed) | ")
                .append(PCK_URL).append(" | ").append(JovianPackMain.RETRIEVED).append(" | `")
                .append(pckSha).append("` |\n\n")
                .append("## Extraction\n\n")
                .append("Tool: `juranometria.tool.NeptuneSystemPackMain` (this repository), using")
                .append(" `juranometria.solar.spk.SpkExcerpt`.\n\n")
                .append("- `").append(NEPTUNE_KERNEL_NAME).append("`: 0 -> 8, SPK Type 2, from")
                .append(" 1900-01-01 to 2101-01-01 TDB with 31-day margins (#654, N-1a, N-2).\n")
                .append("- `").append(MOONS_KERNEL_NAME).append("`: 8 -> 801 (Triton), 802")
                .append(" (Nereid), SPK Type 2, from")
                .append(" 2000-01-01 to 2101-01-01 TDB with 31-day margins (#654, N-6a).\n\n")
                .append("Whole records kept, coefficients unchanged. Coverage of the written")
                .append(" segments (seconds past J2000, TDB):\n\n");
        for (SpkKernel k : List.of(neptune, moons)) {
            for (SpkKernel.Segment s : k.segments()) {
                md.append(String.format(Locale.ROOT, "- %d -> %d: %.1f to %.1f (%s)\n",
                        s.center(), s.target(), s.startEt(), s.endEt(), s.name().strip()));
            }
        }
        md.append("\n## Output\n\n")
                .append("| file | bytes | SHA-256 |\n|---|---|---|\n")
                .append("| `").append(NEPTUNE_KERNEL_NAME).append("` | ").append(neptuneBytes)
                .append(" | `").append(checksums.get(NEPTUNE_KERNEL_NAME)).append("` |\n")
                .append("| `").append(MOONS_KERNEL_NAME).append("` | ").append(moonsBytes)
                .append(" | `").append(checksums.get(MOONS_KERNEL_NAME)).append("` |\n\n")
                .append("## Validation\n\n")
                .append("- Neptune's kernel: ").append(neptuneProof).append(".\n")
                .append("- The moons' kernel: ").append(moonsProof).append(".\n")
                .append("- The barycentre's source: ").append(barycentreSource).append(".\n")
                .append("- Neptune about its barycentre: ").append(centre).append(".\n")
                .append("- The pole's periodic term: ").append(periodicTerm(constants)).append(".\n")
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
                .append(" does in each kernel's comment area and in `").append(NOTICE_NAME)
                .append("`. No fees or licensing are required.\n");
        return md.toString();
    }

    private static String manifest(Map<String, String> checksums, Map<String, String> constants,
                                   SpkKernel neptune, SpkKernel moons) {
        Map<String, String> m = new TreeMap<>();
        m.put("format.version", "1");
        m.put("pack.name", PACK_NAME);
        m.put("pack.version", Integer.toString(PACK_VERSION));
        m.put("audit.date", AUDIT_DATE);
        m.put("ephemeris", "NEP098 with DE442");
        m.put("ephemeris.neptune.kernel", NEPTUNE_KERNEL_NAME);
        m.put("ephemeris.neptune.segments", Integer.toString(neptune.segments().size()));
        m.put("ephemeris.neptune.coverage.start.jd", String.format(Locale.ROOT, "%.1f", START_JD));
        m.put("ephemeris.neptune.coverage.end.jd", String.format(Locale.ROOT, "%.1f", END_JD));
        m.put("ephemeris.neptune.first.day", FIRST_DAY);
        m.put("ephemeris.neptune.last.day", LAST_DAY);
        m.put("ephemeris.moons.kernel", MOONS_KERNEL_NAME);
        m.put("ephemeris.moons.bodies", String.join(",", MOONS.stream().map(String::valueOf).toList()));
        m.put("ephemeris.moons.segments", Integer.toString(moons.segments().size()));
        m.put("ephemeris.moons.coverage.start.jd", String.format(Locale.ROOT, "%.1f", MOONS_START_JD));
        m.put("ephemeris.moons.coverage.end.jd", String.format(Locale.ROOT, "%.1f", END_JD));
        m.put("ephemeris.moons.first.day", MOONS_FIRST_DAY);
        m.put("ephemeris.moons.last.day", LAST_DAY);
        m.put("source.nep098.url", NEP098_SOURCE_URL);
        m.put("source.nep098.sha256", NEP098_SOURCE_SHA256);
        m.put("source.pck.url", PCK_URL);
        m.put("source.pck.sha256", JovianPackMain.PINNED.get("pck00011.tpc"));
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
