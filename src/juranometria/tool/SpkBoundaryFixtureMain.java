package juranometria.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import juranometria.solar.spk.SpkExcerpt;
import juranometria.solar.spk.SpkKernel;

/**
 * Cuts the committed test fixture for epoch-aware SPK selection (issue
 * #473): a small modified excerpt of JUP365 over twenty days each side
 * of its segment split at JD 2450464.5 TDB (1997-01-16; measured in
 * #472), for Io (5 → 501), Jupiter (5 → 599) and the Jupiter
 * barycentre from the solar-system barycentre (0 → 5, one segment,
 * DE440's). The first two pairs therefore carry <em>two</em> segments
 * each, meeting at the split; the third carries one.
 *
 * <p>Deterministic, and proved before writing: every record of the
 * excerpt is evaluated against the source at the record's start, its
 * midpoint and a point near its end and must agree exactly. The
 * independent proof is {@code spk-boundary-reference.txt} beside it,
 * which jplephem produced from the <em>unmodified</em> source
 * ({@code scripts/spk-boundary-reference.py}); the tests hold the
 * reader to both.
 *
 * <p>A modified kernel in NAIF's sense, treated as one: its own name,
 * JUranometria named as the modifier in the comment area, the source's
 * identity, digest and whole comment area retained as attribution.
 * Study evidence only: it is not a resource of the application.
 *
 * <p>Run via {@code make spk-boundary-fixture} with
 * {@code imports/raw/jovian-system/jup365.bsp} in place
 * ({@code scripts/download-jovian-sources.sh}).
 */
public final class SpkBoundaryFixtureMain {

    static final String SOURCE_NAME = "jup365.bsp";
    static final String SOURCE_SHA256 =
            "dbf016c01ba4d022154838000cf3f06962cf958ddc503a366f7fe8f81495c5cb";
    static final String SOURCE_URL = "https://naif.jpl.nasa.gov/pub/naif/"
            + "generic_kernels/spk/satellites/jup365.bsp";
    static final String FIXTURE_NAME = "jup365-boundary-1997.bsp";
    static final String REFERENCE_NAME = "spk-boundary-reference.txt";

    /** The split, JD TDB, measured in #472 (docs/decisions/jovian-system.md). */
    static final double SPLIT_JD = 2450464.5;
    static final double HALF_WINDOW_DAYS = 20.0;
    static final double J2000_JD = 2451545.0;

    static final List<SpkExcerpt.Pick> PICKS = List.of(
            new SpkExcerpt.Pick(5, 501), // Io from the Jupiter barycentre: two segments
            new SpkExcerpt.Pick(5, 599), // Jupiter from its barycentre: two segments
            new SpkExcerpt.Pick(0, 5));  // Jupiter barycentre from the SSB: one (DE440)

    private SpkBoundaryFixtureMain() {
    }

    public static void main(String[] args) throws IOException {
        Path raw = Path.of(args.length > 0 ? args[0]
                : "imports/raw/jovian-system").resolve(SOURCE_NAME);
        Path outDir = Path.of(args.length > 1 ? args[1]
                : "docs/studies/jovian-system/spk");
        if (!Files.isRegularFile(raw)) {
            throw new IllegalStateException("missing raw input " + raw
                    + "; run scripts/download-jovian-sources.sh");
        }
        byte[] source = Files.readAllBytes(raw);
        String actual = PinnedInputs.sha256Hex(source);
        if (!actual.equals(SOURCE_SHA256)) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "raw input %s fails its pinned checksum: expected %s,"
                            + " found %s", raw, SOURCE_SHA256, actual));
        }
        SpkKernel full = SpkKernel.read(source);
        double startEt = (SPLIT_JD - HALF_WINDOW_DAYS - J2000_JD) * 86400.0;
        double endEt = (SPLIT_JD + HALF_WINDOW_DAYS - J2000_JD) * 86400.0;
        byte[] fixture = SpkExcerpt.of(source, PICKS, startEt, endEt,
                "JURANOMETRIA JUP365 BOUNDARY FIXTURE 1997 (#473)",
                comment(full.comments()));

        // Proof before writing: every kept record says what the source says.
        SpkKernel excerpt = SpkKernel.read(fixture);
        int states = 0;
        for (SpkKernel.Segment s : excerpt.segments()) {
            double[] directory = directory(fixture, s);
            double init = directory[0];
            double intlen = directory[1];
            int n = (int) directory[3];
            for (int record = 0; record < n; record++) {
                for (double at : new double[] {0.0, 0.5, 0.999}) {
                    double et = init + (record + at) * intlen;
                    SpkKernel.State a = excerpt.state(s.center(), s.target(), et);
                    SpkKernel.State b = full.state(s.center(), s.target(), et);
                    if (!a.equals(b)) {
                        throw new IllegalStateException(String.format(Locale.ROOT,
                                "excerpt differs from source at %d -> %d, et %.3f",
                                s.center(), s.target(), et));
                    }
                    states++;
                }
            }
        }

        Files.createDirectories(outDir);
        Files.write(outDir.resolve(FIXTURE_NAME), fixture);
        Path reference = outDir.resolve(REFERENCE_NAME);
        String referenceDigest = Files.isRegularFile(reference)
                ? PinnedInputs.sha256Hex(Files.readAllBytes(reference))
                : "(not present; run scripts/spk-boundary-reference.py)";
        Files.writeString(outDir.resolve("README.md"),
                readme(excerpt, fixture, states, referenceDigest),
                StandardCharsets.UTF_8);
        System.out.printf(Locale.ROOT, "%s: %,d bytes, sha256 %s, %d segments;"
                        + " %d states identical to the source%n",
                FIXTURE_NAME, fixture.length, PinnedInputs.sha256Hex(fixture),
                excerpt.segments().size(), states);
    }

    /** INIT, INTLEN, RSIZE, N from the segment's own directory. */
    private static double[] directory(byte[] kernel, SpkKernel.Segment s) {
        java.nio.ByteBuffer words = java.nio.ByteBuffer.wrap(kernel)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        double[] d = new double[4];
        for (int i = 0; i < 4; i++) {
            d[i] = words.getDouble((s.lastAddress() - 4 + i) * 8);
        }
        return d;
    }

    private static String comment(String sourceComments) {
        StringBuilder c = new StringBuilder();
        c.append(FIXTURE_NAME).append(": a test fixture cut from the JPL satellite\n")
                .append("ephemeris JUP365, MODIFIED by JUranometria\n")
                .append("(https://github.com/Aha43/JUranometria).\n")
                .append("\n")
                .append("This is not an original JPL/NAIF file. It was produced by\n")
                .append("juranometria.tool.SpkBoundaryFixtureMain from the official kernel\n")
                .append("jup365.bsp, obtained from NASA/JPL NAIF at\n")
                .append("  ").append(SOURCE_URL).append("\n")
                .append("(SHA-256 ").append(SOURCE_SHA256).append(")\n")
                .append("by keeping, of these SPK Type 2 segment pairs,\n")
                .append("  5 -> 501  Io from the Jupiter barycentre (two segments)\n")
                .append("  5 -> 599  Jupiter from the Jupiter barycentre (two segments)\n")
                .append("  0 -> 5    Jupiter barycentre from the solar-system barycentre\n")
                .append("only the whole records within twenty days of JD 2450464.5 TDB\n")
                .append("(1997-01-16), where JUP365's two segments per body meet. Every\n")
                .append("Chebyshev coefficient kept is the source's coefficient, unchanged.\n")
                .append("The fixture exists so that JUranometria's SPK reader and excerpt\n")
                .append("writer are tested across a segment boundary (issue #473); it is\n")
                .append("not an ephemeris for use.\n")
                .append("\n")
                .append("The ephemeris itself is JUP365 by Jacobson (2021), NASA/JPL, with\n")
                .append("DE440 (Park, Folkner, Williams and Boggs, 2021). The source\n")
                .append("kernel's own comment area follows, retained as source attribution:\n")
                .append("\n")
                .append("---- jup365.bsp comment area, as distributed by NAIF ----\n");
        for (String line : sourceComments.split("\n", -1)) {
            c.append(printable(line)).append('\n');
        }
        c.append("---- end of the source kernel's comment area ----");
        return c.toString();
    }

    /** The source comments are ASCII; anything else is replaced. */
    private static String printable(String line) {
        StringBuilder out = new StringBuilder();
        for (char ch : line.toCharArray()) {
            out.append(ch >= 0x20 && ch <= 0x7e ? ch : '?');
        }
        return out.toString();
    }

    private static String readme(SpkKernel excerpt, byte[] fixture, int states,
                                 String referenceDigest) {
        StringBuilder md = new StringBuilder();
        md.append("# The JUP365 segment-split fixture (#473)\n\n")
                .append("`").append(FIXTURE_NAME).append("` is a modified excerpt of ")
                .append("NAIF's `jup365.bsp` (sha256 `").append(SOURCE_SHA256)
                .append("`, from `").append(SOURCE_URL).append("`), cut by ")
                .append("`juranometria.tool.SpkBoundaryFixtureMain` (`make spk-boundary-fixture`) ")
                .append("over twenty days each side of JD ")
                .append(String.format(Locale.ROOT, "%.1f", SPLIT_JD))
                .append(" TDB, where JUP365's two segments per body meet (measured in #472). ")
                .append("Io and Jupiter therefore carry two segments each, the Jupiter barycentre one. ")
                .append("Every coefficient is the source's; the builder proved ")
                .append(String.format(Locale.ROOT, "%,d", states))
                .append(" states identical to the source before writing. ")
                .append("JUranometria is named as the modifier in the comment area and the source's ")
                .append("comment area is retained there as the file carries it - up to its end-of-text ")
                .append("marker, as NAIF's DAF layout defines the area; the bytes after that marker are ")
                .append("not comment, and the longer `jup365.cmt` published beside the kernel is a ")
                .append("separate document - as the released Solar System pack does.\n\n")
                .append("`").append(REFERENCE_NAME).append("` holds states that jplephem, an independent ")
                .append("reader, evaluated from the *unmodified* source at epochs on both sides of the split ")
                .append("and at the split itself, from every covering segment (`scripts/spk-boundary-reference.py`). ")
                .append("`SpkSegmentSelectionTest` holds the reader to both files; `SpkExcerptTest` cuts the fixture ")
                .append("further to prove the writer across the split and on gaps, overlaps and missing epochs. ")
                .append("The reader agrees with the reference to the test's metre bound; its worst position ")
                .append("difference is at the rows a fraction of a second from a record boundary, where jplephem, ")
                .append("measuring the epoch from the source segment's start four centuries away, loses about a ")
                .append("microsecond of offset (9 mm at Io's 17 km/s), while the reader measures from the record's ")
                .append("own midpoint - on this fixture, whose segments begin twenty days from the split, jplephem ")
                .append("and the reader agree to a nanometre at the same rows.\n\n")
                .append("Study evidence only: nothing at build, test or run time depends on these files beyond ")
                .append("the tests reading them as fixtures; the fixture is not an ephemeris for use and not a ")
                .append("resource of the application.\n\n")
                .append("| file | bytes | sha256 |\n|---|---:|---|\n")
                .append(String.format(Locale.ROOT, "| `%s` | %d | `%s` |\n", FIXTURE_NAME,
                        fixture.length, PinnedInputs.sha256Hex(fixture)))
                .append(String.format(Locale.ROOT, "| `%s` | | `%s` |\n\n", REFERENCE_NAME,
                        referenceDigest))
                .append("Segments written (seconds past J2000, TDB; JD TDB):\n\n")
                .append("| segment | centre | target | from | to | from (JD) | to (JD) |\n")
                .append("|---|---|---|---|---|---|---|\n");
        for (SpkKernel.Segment s : excerpt.segments()) {
            md.append(String.format(Locale.ROOT,
                    "| %s | %d | %d | %.1f | %.1f | %.6f | %.6f |\n", s.name(),
                    s.center(), s.target(), s.startEt(), s.endEt(),
                    J2000_JD + s.startEt() / 86400.0, J2000_JD + s.endEt() / 86400.0));
        }
        return md.toString();
    }
}
