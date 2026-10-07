package juranometria.solar.spk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SPK reader selects a segment by centre, target and epoch
 * (issue #473). JUP365 keeps two segments per body meeting at JD
 * 2450464.5 TDB (1997-01-16; measured in #472), and the committed
 * fixture {@code docs/studies/jovian-system/spk/jup365-boundary-1997.bsp}
 * carries both of Io's and Jupiter's, cut twenty days each side of the
 * split, beside one single-segment pair. The independent reference
 * {@code spk-boundary-reference.txt} (jplephem on the unmodified
 * source) says what every covering segment evaluates to, so the
 * reader's choice and its arithmetic are held separately: on each side
 * of the split the covering segment, at the split the later one -
 * NAIF's precedence - and a gap or an epoch beyond coverage refused by
 * name. Kernels with a gap and with overlapping, disagreeing segments
 * are assembled from the fixture by {@link SpkAssembly}.
 */
class SpkSegmentSelectionTest {

    static final Path STUDY = Path.of("docs/studies/jovian-system/spk");
    static final Path FIXTURE = STUDY.resolve("jup365-boundary-1997.bsp");
    static final Path REFERENCE = STUDY.resolve("spk-boundary-reference.txt");
    static final String FIXTURE_SHA256 =
            "7f2c5decfbb8dea9d882ddd53df88000bf8c78557e15903e68c541a4912794ec";
    static final String REFERENCE_SHA256 =
            "edb35099986cd5fe20af7fd2a0a045ad329115655c043fca6956504b723b0c15";

    /** The split, JD 2450464.5 TDB, as seconds past J2000. */
    static final double SPLIT_ET = (2450464.5 - 2451545.0) * 86400.0;
    static final double DAY = 86400.0;
    static final int JUPITER_BARYCENTRE = 5;
    static final int IO = 501;
    static final int JUPITER = 599;

    static byte[] fixtureBytes;
    static SpkKernel fixture;

    @BeforeAll
    static void readTheFixture() throws IOException {
        fixtureBytes = Files.readAllBytes(FIXTURE);
        fixture = SpkKernel.read(fixtureBytes);
    }

    record Reference(int center, int target, int index, double jd,
                     Vector3 position, Vector3 velocity) {
        double et() {
            return (jd - 2451545.0) * 86400.0;
        }
    }

    static List<Reference> reference() throws IOException {
        List<Reference> rows = new ArrayList<>();
        for (String line : Files.readAllLines(REFERENCE, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.trim().split("\\s+");
            String[] pair = f[0].split("->");
            rows.add(new Reference(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]),
                    Integer.parseInt(f[1]), Double.parseDouble(f[2]),
                    new Vector3(Double.parseDouble(f[3]), Double.parseDouble(f[4]),
                            Double.parseDouble(f[5])),
                    new Vector3(Double.parseDouble(f[6]), Double.parseDouble(f[7]),
                            Double.parseDouble(f[8]))));
        }
        return rows;
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void theFixtureIsThePinnedBytesWithTwoSegmentsForIoAndJupiterMeetingAtTheSplit()
            throws IOException {
        assertEquals(FIXTURE_SHA256, sha256(fixtureBytes), "the fixture's bytes");
        assertEquals(REFERENCE_SHA256, sha256(Files.readAllBytes(REFERENCE)),
                "the reference's bytes");
        assertEquals(5, fixture.segments().size());
        for (int body : new int[] {IO, JUPITER}) {
            List<SpkKernel.Segment> of = fixture.segments(JUPITER_BARYCENTRE, body);
            assertEquals(2, of.size(), "two segments for " + body);
            assertEquals(SPLIT_ET, of.get(0).endEt(), "the first ends at the split");
            assertEquals(SPLIT_ET, of.get(1).startEt(), "the second begins there");
            assertTrue(of.get(0).startEt() < SPLIT_ET - 19 * DAY
                    && of.get(1).endEt() > SPLIT_ET + 19 * DAY, "twenty days each side");
        }
        assertEquals(1, fixture.segments(0, JUPITER_BARYCENTRE).size(),
                "the barycentre is DE440's one segment");
        assertTrue(fixture.segments(JUPITER_BARYCENTRE, 502).isEmpty(),
                "Europa is not in the fixture");
        assertTrue(fixture.comments().contains("MODIFIED by JUranometria"),
                "the modifier is named in the comment area");
        assertTrue(fixture.comments().contains("jup365.bsp"),
                "and the source");
    }

    @Test
    void theSegmentCoveringTheEpochIsSelectedOnEachSideOfTheSplit() {
        List<SpkKernel.Segment> io = fixture.segments(JUPITER_BARYCENTRE, IO);
        assertSame(io.get(0), fixture.segment(JUPITER_BARYCENTRE, IO, SPLIT_ET - 1.0));
        assertSame(io.get(0), fixture.segment(JUPITER_BARYCENTRE, IO, SPLIT_ET - 15 * DAY));
        assertSame(io.get(1), fixture.segment(JUPITER_BARYCENTRE, IO, SPLIT_ET + 1.0));
        assertSame(io.get(1), fixture.segment(JUPITER_BARYCENTRE, IO, SPLIT_ET + 15 * DAY));
        IllegalArgumentException several = assertThrows(IllegalArgumentException.class,
                () -> fixture.segment(JUPITER_BARYCENTRE, IO));
        assertTrue(several.getMessage().contains("2 segments 5 -> 501")
                && several.getMessage().contains("select one by epoch"),
                "a pair with two segments has no 'the one segment': "
                        + several.getMessage());
        assertSame(fixture.segments(0, JUPITER_BARYCENTRE).get(0),
                fixture.segment(0, JUPITER_BARYCENTRE),
                "a one-segment pair still has its one segment");
    }

    @Test
    void atTheSplitTheLaterSegmentIsSelectedAsNaifReads() {
        for (int body : new int[] {IO, JUPITER}) {
            List<SpkKernel.Segment> of = fixture.segments(JUPITER_BARYCENTRE, body);
            assertTrue(of.get(0).covers(SPLIT_ET) && of.get(1).covers(SPLIT_ET),
                    "both cover the split");
            assertSame(of.get(1), fixture.segment(JUPITER_BARYCENTRE, body, SPLIT_ET),
                    "the segment later in the file takes precedence");
        }
    }

    @Test
    void everyReferenceStateIsReproducedFromTheSegmentTheReaderSelects()
            throws IOException {
        List<Reference> rows = reference();
        assertTrue(rows.size() >= 200, "the reference has its rows: " + rows.size());
        int[] seen = new int[2];
        int before = 0;
        int after = 0;
        int atSplit = 0;
        double worstPosition = 0.0;
        double worstVelocity = 0.0;
        double discontinuity = 0.0;
        for (Reference r : rows) {
            double et = r.et();
            SpkKernel.Segment chosen = fixture.segment(r.center, r.target, et);
            List<SpkKernel.Segment> of = fixture.segments(r.center, r.target);
            if (et == SPLIT_ET && of.size() == 2) {
                // Both segments' rows exist here; the reader's answer is
                // the later segment's, and the two rows measure the fit's
                // own discontinuity across the split.
                atSplit++;
                SpkKernel.State s = fixture.state(r.center, r.target, et);
                if (r.index == 1) {
                    worstPosition = Math.max(worstPosition,
                            s.position().minus(r.position).length());
                    worstVelocity = Math.max(worstVelocity,
                            s.velocity().minus(r.velocity).length());
                } else {
                    discontinuity = Math.max(discontinuity,
                            s.position().minus(r.position).length());
                }
                continue;
            }
            assertSame(of.get(r.index), chosen, "the reference's segment is the"
                    + " reader's at " + r);
            seen[r.index] += of.size() == 2 ? 1 : 0;
            if (of.size() == 2) {
                if (et < SPLIT_ET) {
                    before++;
                } else {
                    after++;
                }
            }
            SpkKernel.State s = fixture.state(r.center, r.target, et);
            worstPosition = Math.max(worstPosition, s.position().minus(r.position).length());
            worstVelocity = Math.max(worstVelocity, s.velocity().minus(r.velocity).length());
        }
        assertTrue(before >= 40 && after >= 40, "rows on both sides of the split for"
                + " the two-segment pairs: " + before + " before, " + after + " after");
        assertEquals(4, atSplit, "Io's and Jupiter's two rows each at the split");
        assertTrue(seen[0] > 0 && seen[1] > 0, "both segments of a pair are exercised");
        // The worst rows lie a fraction of a second from a record
        // boundary, where jplephem measures the epoch from the source
        // segment's start four centuries away and loses about a
        // microsecond of offset (9 mm at Io's 17 km/s); the reader
        // measures from the record's own midpoint. On the fixture
        // itself jplephem agrees with the reader to a nanometre there.
        assertTrue(worstPosition < 1e-3,
                "positions agree to a metre; worst " + worstPosition + " km");
        assertTrue(worstVelocity < 1e-9,
                "velocities agree to a micrometre per second; worst "
                        + worstVelocity + " km/s");
        assertTrue(discontinuity < 0.1, "the two fits meet at the split within"
                + " 100 m; measured " + discontinuity + " km");
        System.out.printf("spk split: %d rows, worst position %.3e km, worst velocity"
                + " %.3e km/s, discontinuity of the fits at the split %.3e km%n",
                rows.size(), worstPosition, worstVelocity, discontinuity);
    }

    @Test
    void anEpochBeyondBothSegmentsIsRefusedNamingWhatTheyCover() {
        List<SpkKernel.Segment> io = fixture.segments(JUPITER_BARYCENTRE, IO);
        for (double et : new double[] {io.get(0).startEt() - 1.0, io.get(1).endEt() + 1.0}) {
            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                    () -> fixture.state(JUPITER_BARYCENTRE, IO, et));
            assertTrue(refused.getMessage().contains("outside the 2 segments 5 -> 501")
                    && refused.getMessage().contains("which cover ["),
                    "the refusal names the pair and its coverage: " + refused.getMessage());
        }
        fixture.state(JUPITER_BARYCENTRE, IO, io.get(0).startEt());
        fixture.state(JUPITER_BARYCENTRE, IO, io.get(1).endEt());
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> fixture.state(JUPITER_BARYCENTRE, 502, SPLIT_ET));
        assertTrue(missing.getMessage().contains("no segment 5 -> 502"),
                "a pair the kernel lacks is refused by name: " + missing.getMessage());
    }

    /** Io's first segment cut to end ten days before the split, and its second cut to begin five days after. */
    static byte[] gapKernel() throws IOException {
        List<SpkKernel.Segment> io = fixture.segments(JUPITER_BARYCENTRE, IO);
        List<SpkExcerpt.Pick> pick = List.of(new SpkExcerpt.Pick(JUPITER_BARYCENTRE, IO));
        byte[] early = SpkExcerpt.of(fixtureBytes, pick, io.get(0).startEt(),
                SPLIT_ET - 10 * DAY, "TEST EARLY", "test");
        byte[] late = SpkExcerpt.of(fixtureBytes, pick, SPLIT_ET + 5 * DAY,
                io.get(1).endEt(), "TEST LATE", "test");
        return SpkAssembly.of(List.of(
                new SpkAssembly.Part(early, 0, JUPITER_BARYCENTRE, IO),
                new SpkAssembly.Part(late, 0, JUPITER_BARYCENTRE, IO)));
    }

    /** Io's first segment, then Jupiter's first segment relabelled as Io's: two that overlap and disagree. */
    static byte[] overlapKernel() throws IOException {
        return SpkAssembly.of(List.of(
                new SpkAssembly.Part(fixtureBytes, 0, JUPITER_BARYCENTRE, IO),
                new SpkAssembly.Part(fixtureBytes, 2, JUPITER_BARYCENTRE, IO)));
    }

    @Test
    void anEpochInAGapBetweenAPairsSegmentsIsRefusedAndEitherSideIsRead()
            throws IOException {
        SpkKernel gap = SpkKernel.read(gapKernel());
        List<SpkKernel.Segment> io = gap.segments(JUPITER_BARYCENTRE, IO);
        assertEquals(2, io.size());
        assertTrue(io.get(0).endEt() < io.get(1).startEt(), "a gap: "
                + io.get(0).endEt() + " to " + io.get(1).startEt());
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> gap.state(JUPITER_BARYCENTRE, IO, SPLIT_ET));
        assertTrue(refused.getMessage().contains("outside the 2 segments 5 -> 501"),
                refused.getMessage());
        for (double et : new double[] {SPLIT_ET - 15 * DAY, SPLIT_ET + 10 * DAY}) {
            assertEquals(fixture.state(JUPITER_BARYCENTRE, IO, et),
                    gap.state(JUPITER_BARYCENTRE, IO, et),
                    "either side of the gap reads as the fixture does");
        }
    }

    @Test
    void whereSegmentsOverlapTheLaterInTheFileWins() throws IOException {
        SpkKernel overlap = SpkKernel.read(overlapKernel());
        assertEquals(2, overlap.segments(JUPITER_BARYCENTRE, IO).size());
        double et = SPLIT_ET - 5 * DAY;
        assertEquals(fixture.state(JUPITER_BARYCENTRE, JUPITER, et),
                overlap.state(JUPITER_BARYCENTRE, IO, et),
                "the later segment's words - Jupiter's, relabelled - are read");
        assertTrue(!fixture.state(JUPITER_BARYCENTRE, IO, et).equals(
                overlap.state(JUPITER_BARYCENTRE, IO, et)),
                "and not the earlier segment's");
    }
}
