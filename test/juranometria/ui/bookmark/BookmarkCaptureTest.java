package juranometria.ui.bookmark;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.bookmark.BookmarkLibrary;
import juranometria.bookmark.ChartTime;
import juranometria.bookmark.Subject;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What Add Bookmark... proposes and what it saves (#681 stage 2; rulings of
 * 2026-11-02 on #497): the view always; the working selection in its order
 * with its lead as the primary; the chart time pre-ticked as event time
 * exactly when a Solar System body is among the subjects (C-4); the place
 * never pre-ticked; and only what the reader leaves ticked is saved.
 */
class BookmarkCaptureTest {

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-11-24T11:54:00Z"));
    private static final ChartViewState VIEW = new ChartViewState(
            new SkyPosition(56.893713, 24.029342), 2.0, 8.0, null, null);

    @Test
    void identitiesAreGivenTheirKinds() {
        assertEquals(Subject.Kind.STAR, BookmarkCapture.subject("TYC 1800-2202-1").kind());
        assertEquals(Subject.Kind.DEEP_SKY, BookmarkCapture.subject("NGC 598").kind());
        assertEquals(Subject.Kind.DEEP_SKY, BookmarkCapture.subject("IC 434").kind());
        assertEquals(Subject.Kind.BODY, BookmarkCapture.subject("moon").kind());
        assertEquals(Subject.Kind.BODY, BookmarkCapture.subject("jovian.jupiter").kind());
    }

    @Test
    void aSolarSystemSubjectPreTicksTheTimeAsAnEventAndTheViewIsAlwaysThere() {
        BookmarkCapture.Proposal proposal = BookmarkCapture.propose(VIEW,
                List.of("TYC 1800-2202-1", "moon"), "moon", OSLO);
        assertEquals(56.893713, proposal.view().raDegrees());
        assertEquals(2.0, proposal.view().fieldWidthDegrees());
        assertEquals(List.of("TYC 1800-2202-1", "moon"),
                proposal.subjects().stream().map(Subject::id).toList(), "the selection's order");
        assertEquals("moon", proposal.primary().orElseThrow().id(), "its lead");
        assertTrue(proposal.timeTicked(), "C-4: ticked because the Moon is a subject");
        assertEquals(Instant.parse("2026-11-24T11:54:00Z"), proposal.instant());
    }

    @Test
    void withoutABodyTheTimeStartsUntickedAndWithoutASelectionThereIsNoPrimary() {
        assertFalse(BookmarkCapture.propose(VIEW, List.of("NGC 598"), "NGC 598", OSLO).timeTicked());
        BookmarkCapture.Proposal bare = BookmarkCapture.propose(VIEW, List.of(), null, OSLO);
        assertTrue(bare.subjects().isEmpty());
        assertTrue(bare.primary().isEmpty());
        assertFalse(bare.timeTicked());
    }

    @Test
    void onlyWhatTheReaderLeavesTickedIsSaved() {
        BookmarkCapture.Proposal proposal = BookmarkCapture.propose(VIEW,
                List.of("moon", "TYC 1800-2202-1"), "moon", OSLO);
        BookmarkLibrary.Draft timeless = BookmarkCapture.draft(proposal, " The Moon at Alcyone ",
                proposal.subjects(), proposal.primary(), false, ChartTime.Purpose.EVENT, false, "");
        assertEquals("The Moon at Alcyone", timeless.title());
        assertTrue(timeless.time().isEmpty(), "unticked: no time");
        assertTrue(timeless.place().isEmpty(), "unticked: no place");
        assertTrue(timeless.note().isEmpty());
        BookmarkLibrary.Draft observed = BookmarkCapture.draft(proposal, "Seen", proposal.subjects(),
                Optional.empty(), true, ChartTime.Purpose.OBSERVATION, true, "Klart");
        assertEquals(ChartTime.Purpose.OBSERVATION, observed.time().orElseThrow().purpose());
        assertEquals(59.913, observed.place().orElseThrow().latitudeDegrees());
        assertTrue(observed.primary().isEmpty(), "None chosen: no primary");
        assertThrows(IllegalArgumentException.class, () -> BookmarkCapture.draft(proposal, " ",
                proposal.subjects(), proposal.primary(), false, ChartTime.Purpose.EVENT, false, ""));
    }

    @Test
    void aPrimaryRemovedFromTheSubjectsIsNotKept() {
        BookmarkCapture.Proposal proposal = BookmarkCapture.propose(VIEW,
                List.of("moon", "venus"), "moon", OSLO);
        BookmarkLibrary.Draft draft = BookmarkCapture.draft(proposal, "Venus", List.of(
                new Subject(Subject.Kind.BODY, "venus")), proposal.primary(), true,
                ChartTime.Purpose.EVENT, false, null);
        assertTrue(draft.primary().isEmpty());
    }
}
