package juranometria.bookmark;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The version-1 bookmark document (#681, schema in docs/decisions/bookmarks.md):
 * the study's eight required examples round-trip byte for byte through the
 * application's own reader and writer; the reader is strict about JSON and
 * never throws on content; what it cannot use is kept and written back.
 */
class BookmarkDocumentFormatTest {

    private static final Path EXAMPLES = Path.of("docs/studies/bookmarks/examples/library.json");

    @Test
    void theStudysExampleLibraryRoundTripsByteForByte() throws Exception {
        String text = Files.readString(EXAMPLES, StandardCharsets.UTF_8);
        BookmarkDocument.Usable read = assertInstanceOf(BookmarkDocument.Usable.class,
                BookmarkDocument.read(text));
        assertEquals(text, BookmarkDocument.text(read.library()), "written again, byte for byte");
        Library library = read.library();
        assertEquals(8, library.bookmarks().size(), "the eight examples #497 requires");
        assertEquals(2, library.folders().size());
        assertEquals(0, library.kept().size());
        Node.Bookmark moonAndStar = library.bookmarks().get(4);
        assertEquals(List.of("moon", "TYC 1800-2202-1"),
                moonAndStar.subjects().stream().map(Subject::id).toList());
        assertEquals("moon", moonAndStar.primary().orElseThrow().id());
        assertEquals(ChartTime.Purpose.EVENT, moonAndStar.time().orElseThrow().purpose());
        assertTrue(library.bookmarks().get(0).subjects().isEmpty(), "a position with nothing else");
        assertTrue(library.bookmarks().get(2).time().isEmpty(), "an observing field, time absent");
        assertEquals(ChartTime.Purpose.OBSERVATION,
                library.bookmarks().get(7).time().orElseThrow().purpose());
    }

    @Test
    void theJsonIsStrict() {
        for (String bad : List.of("{\"a\": 1,}", "// no\n{}", "{\"a\": 1} {}", "{\"a\": 1, \"a\": 2}",
                "﻿{}", "{\"a\": \"line\nbreak\"}", "{\"a\": 01}", "[1, 2", "{\"a\": .5}")) {
            assertThrows(Json.Malformed.class, () -> Json.parse(bad), bad.replace("\n", "\\n"));
        }
    }

    @Test
    void numbersAndKeyOrderSurviveAsRead() throws Exception {
        String text = "{\n  \"b\": 1.50,\n  \"a\": [\n    -0.0,\n    2e3\n  ]\n}\n";
        assertEquals(text, Json.write(Json.parse(text)));
        assertEquals("12", Json.Num.of(12.0).lexeme());
        assertEquals("0.25", Json.Num.of(0.25).lexeme());
        assertEquals("56.893713", Json.Num.of(56.893713).lexeme());
    }

    @Test
    void foreignNewerAndDamagedDocumentsAreSaidToBeSo() throws Exception {
        String good = Files.readString(EXAMPLES, StandardCharsets.UTF_8);
        assertInstanceOf(BookmarkDocument.NotOurs.class, BookmarkDocument.read("{\"bookmarks\": []}"));
        assertInstanceOf(BookmarkDocument.NotOurs.class, BookmarkDocument.read(good.substring(0, 900)));
        assertInstanceOf(BookmarkDocument.NotOurs.class, BookmarkDocument.read("﻿" + good));
        BookmarkDocument.Newer newer = assertInstanceOf(BookmarkDocument.Newer.class,
                BookmarkDocument.read(good.replace("\"version\": 1", "\"version\": 2")));
        assertEquals(2, newer.version());
    }

    @Test
    void recordsItCannotUseAreKeptWithTheirReasonsAndWrittenBack() throws Exception {
        String good = Files.readString(EXAMPLES, StandardCharsets.UTF_8);
        String text = good.replace("\"root\": [", "\"root\": [\n    {\"type\": \"separator\", \"id\": \"s-1\", \"w\": 1.50},")
                .replaceFirst("\"field\": 12", "\"field\": 7")
                .replaceFirst("(\"instant\": \"[0-9T:-]+)Z\"", "$1\"");
        Library library = ((BookmarkDocument.Usable) BookmarkDocument.read(text)).library();
        List<List<String>> reasons = library.kept().stream().map(Node.Kept::reasons).toList();
        assertEquals(List.of(List.of("an unknown type \"separator\""),
                List.of("a view whose field is not one of the atlas's steps"),
                List.of("a time whose instant is not stated in UTC")), reasons);
        assertEquals(6, library.bookmarks().size(), "every other bookmark stays usable");
        String again = BookmarkDocument.text(library);
        assertTrue(again.contains("\"w\": 1.50"), "kept exactly");
        assertEquals(again, BookmarkDocument.text(((BookmarkDocument.Usable) BookmarkDocument.read(again)).library()));
    }

    @Test
    void aPrimaryOutsideTheSubjectsAndARepeatedIdAreKept() {
        Node.Bookmark one = new Node.Bookmark("b-1", "One", new SavedView(10, 20, 8), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        String text = BookmarkDocument.text(new Library("t", List.of(one)))
                .replaceFirst("\"title\": \"One\",", "\"title\": \"One\",\n      \"primary\": {\"kind\": \"body\", \"id\": \"mars\"},");
        Library library = ((BookmarkDocument.Usable) BookmarkDocument.read(text)).library();
        assertEquals(List.of(List.of("a primary that is not one of the subjects")),
                library.kept().stream().map(Node.Kept::reasons).toList());
        Library twice = ((BookmarkDocument.Usable) BookmarkDocument.read(
                BookmarkDocument.text(new Library("t", List.of(one, one))))).library();
        assertEquals(1, twice.bookmarks().size(), "the first keeps its id");
        assertEquals(List.of(List.of("the id b-1 twice")),
                twice.kept().stream().map(Node.Kept::reasons).toList());
    }

    @Test
    void anInstantIsStoredInUtcWithItsPurpose() {
        Node.Bookmark timed = new Node.Bookmark("b-1", "Timed", new SavedView(10, 20, 8),
                List.of(new Subject(Subject.Kind.BODY, "moon")), Optional.empty(),
                Optional.of(new ChartTime(Instant.parse("2026-11-24T11:54:00Z"), ChartTime.Purpose.EVENT)),
                Optional.empty(), Optional.empty());
        String text = BookmarkDocument.text(new Library("t", List.of(timed)));
        assertTrue(text.contains("\"instant\": \"2026-11-24T11:54:00Z\""));
        assertTrue(text.contains("\"purpose\": \"event\""));
    }
}
