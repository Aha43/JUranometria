package juranometria.solar.spk;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static juranometria.solar.spk.SpkSegmentSelectionTest.DAY;
import static juranometria.solar.spk.SpkSegmentSelectionTest.IO;
import static juranometria.solar.spk.SpkSegmentSelectionTest.JUPITER;
import static juranometria.solar.spk.SpkSegmentSelectionTest.JUPITER_BARYCENTRE;
import static juranometria.solar.spk.SpkSegmentSelectionTest.SPLIT_ET;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The excerpt writer keeps every segment of a pick that has records in
 * the interval, cut to it, in the source's file order (issue #473):
 * across JUP365's split both halves, inside one half only that half, a
 * gap or an interval past the segments refused by name, and overlapping
 * segments kept as they lie so a reader of the excerpt chooses as in
 * the source. The released one-segment pack is unchanged: the bundled
 * kernel, excerpted over its own coverage with its own name and
 * comment, is reproduced byte for byte.
 */
class SpkExcerptTest {

    private static final String BUNDLED = "/resources/solar-system/"
            + "juranometria-de440-sun-emb-earth-moon-1900-2100.bsp";
    private static final List<SpkExcerpt.Pick> IO_PICK =
            List.of(new SpkExcerpt.Pick(JUPITER_BARYCENTRE, IO));

    private static byte[] fixtureBytes;
    private static SpkKernel fixture;

    @BeforeAll
    static void readTheFixture() throws IOException {
        SpkSegmentSelectionTest.readTheFixture();
        fixtureBytes = SpkSegmentSelectionTest.fixtureBytes;
        fixture = SpkSegmentSelectionTest.fixture;
    }

    private static void assertSameStates(SpkKernel a, SpkKernel b, int center, int target,
                                         double from, double to) {
        int states = 0;
        for (double et = from; et <= to; et += 0.37 * DAY) {
            assertEquals(a.state(center, target, et), b.state(center, target, et),
                    "state at " + et);
            states++;
        }
        assertTrue(states > 10, "states compared: " + states);
    }

    @Test
    void anExcerptAcrossTheSplitKeepsBothSegmentsInFileOrderWithTheSourcesWords()
            throws IOException {
        byte[] bytes = SpkExcerpt.of(fixtureBytes,
                List.of(new SpkExcerpt.Pick(JUPITER_BARYCENTRE, IO),
                        new SpkExcerpt.Pick(0, JUPITER_BARYCENTRE)),
                SPLIT_ET - 5 * DAY, SPLIT_ET + 5 * DAY, "TEST ACROSS", "test");
        SpkKernel excerpt = SpkKernel.read(bytes);
        assertEquals(List.of("5->501", "5->501", "0->5"), excerpt.segments().stream()
                .map(s -> s.center() + "->" + s.target()).toList(),
                "both of Io's segments, then the barycentre's one");
        List<SpkKernel.Segment> io = excerpt.segments(JUPITER_BARYCENTRE, IO);
        assertEquals(SPLIT_ET, io.get(0).endEt(), "the first still ends at the split");
        assertEquals(SPLIT_ET, io.get(1).startEt(), "the second still begins there");
        assertTrue(io.get(0).startEt() <= SPLIT_ET - 5 * DAY
                && io.get(0).startEt() > SPLIT_ET - 7 * DAY, "cut to whole records");
        assertTrue(io.get(1).endEt() >= SPLIT_ET + 5 * DAY
                && io.get(1).endEt() < SPLIT_ET + 7 * DAY);
        assertSameStates(fixture, excerpt, JUPITER_BARYCENTRE, IO,
                SPLIT_ET - 5 * DAY, SPLIT_ET + 5 * DAY);
        assertSameStates(fixture, excerpt, 0, JUPITER_BARYCENTRE,
                SPLIT_ET - 5 * DAY, SPLIT_ET + 5 * DAY);
        assertEquals(fixture.state(JUPITER_BARYCENTRE, IO, SPLIT_ET),
                excerpt.state(JUPITER_BARYCENTRE, IO, SPLIT_ET),
                "at the split the same later segment is read");
    }

    @Test
    void anIntervalInsideOneHalfKeepsOnlyThatHalf() throws IOException {
        List<SpkKernel.Segment> io = fixture.segments(JUPITER_BARYCENTRE, IO);
        SpkKernel early = SpkKernel.read(SpkExcerpt.of(fixtureBytes, IO_PICK,
                SPLIT_ET - 15 * DAY, SPLIT_ET - 5 * DAY, "TEST EARLY", "test"));
        assertEquals(1, early.segments().size());
        assertTrue(early.segments().get(0).endEt() <= SPLIT_ET, "from the first segment");
        SpkKernel upToTheSplit = SpkKernel.read(SpkExcerpt.of(fixtureBytes, IO_PICK,
                SPLIT_ET - 5 * DAY, SPLIT_ET, "TEST TO SPLIT", "test"));
        assertEquals(1, upToTheSplit.segments().size(),
                "an interval ending at the split has no record of the second segment");
        assertEquals(SPLIT_ET, upToTheSplit.segments().get(0).endEt());
        SpkKernel fromTheSplit = SpkKernel.read(SpkExcerpt.of(fixtureBytes, IO_PICK,
                SPLIT_ET, SPLIT_ET + 5 * DAY, "TEST FROM SPLIT", "test"));
        assertEquals(1, fromTheSplit.segments().size(),
                "an interval beginning at the split has no record of the first");
        assertEquals(SPLIT_ET, fromTheSplit.segments().get(0).startEt());
        assertEquals(io.get(1).name(), fromTheSplit.segments().get(0).name());
        assertSameStates(fixture, fromTheSplit, JUPITER_BARYCENTRE, IO,
                SPLIT_ET, SPLIT_ET + 5 * DAY);
    }

    @Test
    void anIntervalReachingPastTheSegmentsIsRefusedByName() {
        List<SpkKernel.Segment> io = fixture.segments(JUPITER_BARYCENTRE, IO);
        IllegalArgumentException before = assertThrows(IllegalArgumentException.class,
                () -> SpkExcerpt.of(fixtureBytes, IO_PICK, io.get(0).startEt() - 10 * DAY,
                        SPLIT_ET, "TEST", "test"));
        assertTrue(before.getMessage().contains("5 -> 501")
                && before.getMessage().contains("do not cover the whole interval"),
                before.getMessage());
        IllegalArgumentException after = assertThrows(IllegalArgumentException.class,
                () -> SpkExcerpt.of(fixtureBytes, IO_PICK, SPLIT_ET,
                        io.get(1).endEt() + 10 * DAY, "TEST", "test"));
        assertTrue(after.getMessage().contains("do not cover the whole interval"),
                after.getMessage());
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> SpkExcerpt.of(fixtureBytes, List.of(new SpkExcerpt.Pick(5, 502)),
                        SPLIT_ET - DAY, SPLIT_ET + DAY, "TEST", "test"));
        assertTrue(missing.getMessage().contains("no segment 5 -> 502"), missing.getMessage());
    }

    @Test
    void aGapInsideTheIntervalIsRefusedByName() throws IOException {
        byte[] gap = SpkSegmentSelectionTest.gapKernel();
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> SpkExcerpt.of(gap, IO_PICK, SPLIT_ET - 12 * DAY, SPLIT_ET + 8 * DAY,
                        "TEST", "test"));
        assertTrue(refused.getMessage().contains("5 -> 501 do not cover the whole interval")
                && refused.getMessage().contains("is uncovered"), refused.getMessage());
        List<SpkKernel.Segment> io = SpkKernel.read(gap).segments(JUPITER_BARYCENTRE, IO);
        assertTrue(refused.getMessage().contains(String.format("%.3f to %.3f is uncovered",
                io.get(0).endEt(), io.get(1).startEt())), "the gap is named: "
                + refused.getMessage());
        SpkKernel either = SpkKernel.read(SpkExcerpt.of(gap, IO_PICK,
                SPLIT_ET - 15 * DAY, SPLIT_ET - 12 * DAY, "TEST", "test"));
        assertEquals(1, either.segments().size(), "an interval on one side is cut");
    }

    @Test
    void overlappingSegmentsAreKeptAsTheyLieSoTheExcerptReadsAsTheSource()
            throws IOException {
        byte[] overlap = SpkSegmentSelectionTest.overlapKernel();
        SpkKernel source = SpkKernel.read(overlap);
        SpkKernel excerpt = SpkKernel.read(SpkExcerpt.of(overlap, IO_PICK,
                SPLIT_ET - 10 * DAY, SPLIT_ET - 2 * DAY, "TEST OVERLAP", "test"));
        assertEquals(2, excerpt.segments(JUPITER_BARYCENTRE, IO).size(),
                "both overlapping segments are kept");
        double et = SPLIT_ET - 5 * DAY;
        assertEquals(source.state(JUPITER_BARYCENTRE, IO, et),
                excerpt.state(JUPITER_BARYCENTRE, IO, et));
        assertEquals(fixture.state(JUPITER_BARYCENTRE, JUPITER, et),
                excerpt.state(JUPITER_BARYCENTRE, IO, et),
                "which is the later segment's: Jupiter's words relabelled");
    }

    @Test
    void theReleasedOneSegmentPackIsReproducedByteForByte() throws IOException {
        byte[] released;
        try (InputStream in = SpkExcerptTest.class.getResourceAsStream(BUNDLED)) {
            released = in.readAllBytes();
        }
        SpkKernel kernel = SpkKernel.read(released);
        List<SpkExcerpt.Pick> picks = kernel.segments().stream()
                .map(s -> new SpkExcerpt.Pick(s.center(), s.target())).toList();
        assertEquals(4, picks.size());
        // The Moon's and Earth's 4-day records begin and end inside the
        // Sun's 32-day ones; an interval every segment covers keeps every
        // record of each, as the pack's builder did.
        double start = kernel.segments().stream().mapToDouble(SpkKernel.Segment::startEt)
                .max().orElseThrow();
        double end = kernel.segments().stream().mapToDouble(SpkKernel.Segment::endEt)
                .min().orElseThrow();
        String name = new String(released, 16, 60, StandardCharsets.US_ASCII).strip();
        String comment = kernel.comments();
        comment = comment.endsWith("\n") ? comment.substring(0, comment.length() - 1)
                : comment;
        byte[] again = SpkExcerpt.of(released, picks, start, end, name, comment);
        assertEquals(released.length, again.length, "the same length");
        assertArrayEquals(released, again, "the released kernel, byte for byte");
        assertTrue(Arrays.equals(released, again));
    }
}
