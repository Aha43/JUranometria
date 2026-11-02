package juranometria.tool;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.tool.BookmarkDocument.Bookmark;
import juranometria.tool.BookmarkDocument.ChartTime;
import juranometria.tool.BookmarkDocument.Folder;
import juranometria.tool.BookmarkDocument.Kept;
import juranometria.tool.BookmarkDocument.Library;
import juranometria.tool.BookmarkDocument.Node;
import juranometria.tool.BookmarkDocument.Place;
import juranometria.tool.BookmarkDocument.Read;
import juranometria.tool.BookmarkDocument.Subject;
import juranometria.tool.BookmarkDocument.SubjectKind;
import juranometria.tool.BookmarkDocument.TimePurpose;
import juranometria.tool.BookmarkDocument.Usable;
import juranometria.tool.BookmarkDocument.View;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #497 study's proposed version-1 bookmark document: a canonical
 * round trip, a strict reader, and records it cannot use kept and
 * written back rather than dropped. Study code; no application class
 * reads a bookmark yet.
 */
class BookmarkDocumentTest {

    private static Library library() {
        Subject moon = new Subject(SubjectKind.BODY, "moon");
        Subject star = new Subject(SubjectKind.STAR, "TYC 1800-2202-1");
        Bookmark timed = new Bookmark("b-1", "Månen ved Alcyone", new View(56.893713, 24.029342, 2),
                List.of(star, moon), Optional.of(moon),
                Optional.of(new ChartTime(Instant.parse("2026-11-24T11:54:00Z"), TimePurpose.EVENT)),
                Optional.of(new Place(59.913, 10.752)), Optional.of("Klart \"vær\".\nLinje to."));
        Bookmark plain = new Bookmark("b-2", "Orion", new View(83.8, -5.4, 12), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        List<Node> root = List.of(plain, new Folder("f-1", "Hendelser", List.of(timed)));
        return new Library("6.2.0-test", root);
    }

    private static String text(Library library) {
        return BookmarkDocument.write(BookmarkDocument.json(library));
    }

    private static Read read(String text) throws BookmarkDocument.Malformed {
        return BookmarkDocument.read(BookmarkDocument.parse(text));
    }

    @Test
    void aLibraryRoundTripsByteForByteAndKeepsSubjectOrderApartFromThePrimary() throws Exception {
        String text = text(library());
        Usable read = assertInstanceOf(Usable.class, read(text));
        assertEquals(library(), read.library(), "the domain read back is the domain written");
        assertEquals(text, text(read.library()), "written again, byte for byte");
        Bookmark timed = BookmarkDocument.bookmarks(read.library().root()).get(1);
        assertEquals(List.of("TYC 1800-2202-1", "moon"),
                timed.subjects().stream().map(Subject::id).toList(),
                "the user's order, the primary second");
        assertEquals("moon", timed.primary().orElseThrow().id(), "the primary held by identity");
        assertTrue(text.contains("Månen ved Alcyone"), "non-ASCII written as itself");
        assertTrue(text.endsWith("}\n") && !text.contains("\r"), "LF line ends, one trailing LF");
    }

    @Test
    void theReaderIsStrictAboutWhatIsNotJson() {
        for (String bad : List.of("{\"a\": 1,}", "// no\n{}", "{\"a\": 1} {}", "{\"a\": 1, \"a\": 2}",
                "﻿{}", "{\"a\": \"line\nbreak\"}", "{\"a\": 01}", "[1, 2")) {
            assertThrows(BookmarkDocument.Malformed.class, () -> BookmarkDocument.parse(bad),
                    "refused: " + bad.replace("\n", "\\n"));
        }
    }

    @Test
    void aNewerSchemaIsNeverReinterpretedAndAForeignDocumentIsNotOurs() throws Exception {
        String text = text(library());
        BookmarkDocument.Newer newer = assertInstanceOf(BookmarkDocument.Newer.class,
                read(text.replace("\"version\": 1", "\"version\": 2")));
        assertEquals(2, newer.version());
        assertInstanceOf(BookmarkDocument.NotOurs.class, read("{\"bookmarks\": []}"));
        assertInstanceOf(BookmarkDocument.NotOurs.class,
                read(text.replace("\"version\": 1", "\"version\": 1.0")));
    }

    @Test
    void aRecordItCannotUseIsKeptExactlyAndWrittenBack() throws Exception {
        String text = text(library()).replace("\"root\": [",
                "\"root\": [\n    {\"type\": \"separator\", \"id\": \"s-1\", \"weight\": 1.50},");
        Usable read = assertInstanceOf(Usable.class, read(text));
        List<Kept> kept = BookmarkDocument.kept(read.library().root());
        assertEquals(1, kept.size());
        assertEquals(List.of("an unknown type \"separator\""), kept.get(0).reasons());
        assertEquals(2, BookmarkDocument.bookmarks(read.library().root()).size(),
                "every other record stays usable");
        String again = text(read.library());
        assertTrue(again.contains("\"weight\": 1.50"), "the number keeps its exact text");
        assertEquals(again, text(((Usable) read(again)).library()), "and stays put after that");
    }

    @Test
    void aBookmarkThatBreaksTheRulesIsKeptWithItsReasons() throws Exception {
        Library lib = library();
        Bookmark first = BookmarkDocument.bookmarks(lib.root()).get(0);
        Bookmark strayPrimary = new Bookmark("b-3", "Stray", first.view(), List.of(),
                Optional.of(new Subject(SubjectKind.BODY, "mars")), Optional.empty(),
                Optional.empty(), Optional.empty());
        String text = text(new Library("t", List.of(strayPrimary)))
                .replace("\"field\": 12", "\"field\": 7");
        Kept kept = BookmarkDocument.kept(((Usable) read(text)).library().root()).get(0);
        assertEquals(List.of("a view whose field is not one of the atlas's steps",
                "a primary that is not one of the subjects"), kept.reasons());
        String local = text(lib).replace("11:54:00Z\"", "11:54:00\"");
        assertEquals(List.of("a time whose instant is not stated in UTC"),
                BookmarkDocument.kept(((Usable) read(local)).library().root()).get(0).reasons());
    }
}
