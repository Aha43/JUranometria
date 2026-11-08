package juranometria.bookmark;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whole-sky bookmarks (#687, ruled R-11 on #686, option B-2): the
 * version-1 schema with one optional member, written only by a
 * whole-sky bookmark; its chart time and place required; every
 * ordinary bookmark written exactly as before.
 */
class WholeSkyBookmarkTest {

    private static final ChartTime TIME = new ChartTime(
            Instant.parse("2026-12-15T21:00:00Z"), ChartTime.Purpose.OBSERVATION);
    private static final Place OSLO = new Place(59.91, 10.75);

    private static Node.Bookmark wholeSky(Optional<ChartTime> time, Optional<Place> place) {
        return new Node.Bookmark("b1", "Tonight's whole sky", SavedView.wholeSkyView(),
                List.of(new Subject(Subject.Kind.BODY, "saturn")), Optional.empty(),
                time, place, Optional.empty());
    }

    @Test
    void theViewIsWrittenAsOneModeMemberInTheVersionOneSchema() {
        Library library = new Library("6.5.0-test",
                List.of(wholeSky(Optional.of(TIME), Optional.of(OSLO))));
        String text = BookmarkDocument.text(library);
        assertTrue(text.contains("\"version\": 1"), text);
        assertTrue(text.contains("\"view\": {\n        \"mode\": \"whole-sky\"\n      }"), text);
        assertFalse(text.contains("\"ra\"") || text.contains("\"field\""),
                "no centre and no field are stored: " + text);
        assertTrue(text.contains("\"time\"") && text.contains("\"place\""), text);
        BookmarkDocument.Read read = BookmarkDocument.read(text);
        assertTrue(read instanceof BookmarkDocument.Usable, read.toString());
        Library back = ((BookmarkDocument.Usable) read).library();
        assertEquals(library, back, "round trip");
        assertEquals(text, BookmarkDocument.text(back), "byte for byte");
        assertTrue(((Node.Bookmark) back.root().get(0)).view().wholeSky());
    }

    @Test
    void aWholeSkyBookmarkWithoutItsTimeOrPlaceNeedsAttention() {
        for (Node.Bookmark incomplete : List.of(
                wholeSky(Optional.empty(), Optional.of(OSLO)),
                wholeSky(Optional.of(TIME), Optional.empty()),
                wholeSky(Optional.empty(), Optional.empty()))) {
            String text = BookmarkDocument.text(new Library("x", List.of(incomplete)));
            Library back = ((BookmarkDocument.Usable) BookmarkDocument.read(text)).library();
            Node node = back.root().get(0);
            assertTrue(node instanceof Node.Kept, "kept, needing attention: " + node);
            assertTrue(((Node.Kept) node).reasons().stream()
                    .anyMatch(r -> r.contains("whole-sky bookmark without its chart time and place")),
                    ((Node.Kept) node).reasons().toString());
        }
    }

    @Test
    void anUnknownModeOrAModeWithACentreNeedsAttention() {
        String odd = """
                {
                  "format": "juranometria-bookmarks",
                  "version": 1,
                  "writtenBy": "x",
                  "root": [
                    {"type": "bookmark", "id": "a", "title": "A",
                     "view": {"mode": "fish-eye"},
                     "time": {"instant": "2026-12-15T21:00:00Z", "purpose": "observation"},
                     "place": {"latitude": 59.91, "eastLongitude": 10.75}},
                    {"type": "bookmark", "id": "b", "title": "B",
                     "view": {"mode": "whole-sky", "ra": 10.0, "dec": 41.0, "field": 8.0},
                     "time": {"instant": "2026-12-15T21:00:00Z", "purpose": "observation"},
                     "place": {"latitude": 59.91, "eastLongitude": 10.75}}
                  ]
                }
                """;
        Library back = ((BookmarkDocument.Usable) BookmarkDocument.read(odd)).library();
        assertEquals(2, back.root().size());
        for (Node node : back.root()) {
            assertTrue(node instanceof Node.Kept, node.toString());
        }
        assertTrue(((Node.Kept) back.root().get(0)).reasons().toString().contains("mode"));
        assertTrue(((Node.Kept) back.root().get(1)).reasons().toString()
                .contains("a whole-sky view with a centre or a field"));
    }

    @Test
    void anOrdinaryBookmarkIsWrittenExactlyAsBefore() {
        Node.Bookmark ordinary = new Node.Bookmark("o", "Orion", new SavedView(83.0, 0.0, 24.0),
                List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        String text = BookmarkDocument.text(new Library("x", List.of(ordinary)));
        assertTrue(text.contains("\"view\": {\n        \"ra\": 83,\n        \"dec\": 0,\n"
                + "        \"field\": 24\n      }"), text);
        assertFalse(text.contains("mode"), "no member an older atlas would not know");
        assertFalse(new SavedView(83.0, 0.0, 24.0).wholeSky());
    }

    @Test
    void theWholeSkyViewHasNoCentreOrFieldOfItsOwn() {
        assertTrue(SavedView.wholeSkyView().wholeSky());
        assertEquals(SavedView.wholeSkyView(), SavedView.wholeSkyView(), "one spelling");
        assertThrows(IllegalArgumentException.class,
                () -> new SavedView(10.0, 20.0, 8.0, true), "a centre on the whole sky");
        assertThrows(IllegalArgumentException.class,
                () -> new SavedView(0.0, 0.0, 360.0), "360 is not a field a bookmark stores");
    }
}
