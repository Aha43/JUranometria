package juranometria.bookmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The local bookmark document (#681 stage 1; docs/decisions/bookmarks.md,
 * sections 7-9): the atomic write with its last-known-good backup, every
 * interruption leaving a usable library, a damaged or newer document never
 * overwritten, the backup recovered only when asked, and an export that a
 * restore reproduces byte for byte. Each test works in its own temporary
 * folder; the reader's real folder is never touched.
 */
class DocumentBookmarkLibraryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T20:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path folder;

    /** One id sequence across every library this test opens on the folder. */
    private final AtomicInteger ids = new AtomicInteger();

    private static BookmarkLibrary.Draft draft(String title) {
        return new BookmarkLibrary.Draft(title, new SavedView(10, 20, 8), List.of(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    private DocumentBookmarkLibrary open() throws IOException {
        return open(DocumentBookmarkLibraryTest::move);
    }

    private DocumentBookmarkLibrary open(DocumentBookmarkLibrary.Mover mover) throws IOException {
        return DocumentBookmarkLibrary.open(folder, "6.2.0-test", () -> "id-" + ids.incrementAndGet(),
                CLOCK, mover);
    }

    private static void move(Path from, Path to) throws IOException {
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private String document() throws IOException {
        return Files.readString(folder.resolve("bookmarks.json"), StandardCharsets.UTF_8);
    }

    private static int bookmarks(BookmarkLibrary lib) {
        return ((LibraryState.Available) lib.state()).library().bookmarks().size();
    }

    @Test
    void aNewProfileIsAnEmptyLibraryAndNothingIsWrittenUntilSomethingIsAdded() throws Exception {
        DocumentBookmarkLibrary lib = open();
        assertEquals(0, bookmarks(lib));
        assertFalse(Files.exists(folder.resolve("bookmarks.json")));
        lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        assertTrue(document().startsWith("{\n  \"format\": \"juranometria-bookmarks\",\n  \"version\": 1,\n"
                + "  \"writtenBy\": \"6.2.0-test\","));
        assertFalse(Files.exists(folder.resolve("bookmarks.json.bak")), "no usable document before");
        assertEquals(1, bookmarks(open()), "the next start reads it");
    }

    @Test
    void eachWriteKeepsThePreviousLibraryAsTheBackup() throws Exception {
        DocumentBookmarkLibrary lib = open();
        lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        String one = document();
        lib.add(BookmarkLibrary.ROOT, 1, draft("B"));
        assertEquals(one, Files.readString(folder.resolve("bookmarks.json.bak")));
        assertFalse(Files.exists(folder.resolve("bookmarks.json.part")));
    }

    @Test
    void anInterruptedWriteLeavesThePreviousLibraryAndChangesNothing() throws Exception {
        open().add(BookmarkLibrary.ROOT, 0, draft("A"));
        String before = document();
        DocumentBookmarkLibrary lib = open((from, to) -> {
            throw new IOException("the move was refused");
        });
        assertThrows(IOException.class, () -> lib.add(BookmarkLibrary.ROOT, 1, draft("B")));
        assertEquals(1, bookmarks(lib), "the change was not adopted");
        assertEquals(before, document(), "the document is the previous library");
        assertFalse(Files.exists(folder.resolve("bookmarks.json.part")));
        Files.writeString(folder.resolve("bookmarks.json.part"), "{ half a docu");
        assertEquals(1, bookmarks(open()), "a stray temporary file is ignored");
        assertFalse(Files.exists(folder.resolve("bookmarks.json.part")), "and deleted unread");
    }

    @Test
    void anEditWithAMoveIsOneChangeKeptWholeOrNotAtAll() throws Exception {
        DocumentBookmarkLibrary lib = open();
        String a = lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        String folderId = lib.addFolder(BookmarkLibrary.ROOT, 1, "Deep sky");
        String inner = lib.addFolder(folderId, 0, "Nebulae");
        String before = document();
        DocumentBookmarkLibrary failing = open((from, to) -> {
            throw new IOException("the move was refused");
        });
        assertThrows(IOException.class, () -> failing.updateAndMove(a, draft("A, renamed"), folderId));
        assertThrows(IOException.class, () -> failing.renameAndMove(inner, "Galaxies", BookmarkLibrary.ROOT));
        assertEquals(before, document(), "nothing of either change was written");
        Library unchanged = ((LibraryState.Available) failing.state()).library();
        assertEquals("A", ((Node.Bookmark) unchanged.find(a).orElseThrow()).title(), "nor adopted");
        assertEquals("Nebulae", ((Node.Folder) unchanged.find(inner).orElseThrow()).title());

        lib.updateAndMove(a, draft("A, renamed"), folderId);
        lib.renameAndMove(inner, "Galaxies", BookmarkLibrary.ROOT);
        Library after = ((LibraryState.Available) open().state()).library();
        Node.Folder deep = (Node.Folder) after.find(folderId).orElseThrow();
        assertEquals(List.of("A, renamed"), deep.children().stream().filter(n -> n instanceof Node.Bookmark)
                .map(n -> ((Node.Bookmark) n).title()).toList(), "edited and moved, kept together");
        assertEquals("Galaxies", ((Node.Folder) after.find(inner).orElseThrow()).title());
        assertTrue(after.root().stream().anyMatch(n -> n.id().equals(inner)), "renamed and moved to the root");
    }

    @Test
    void anUnreadableDocumentIsKeptAndItsBackupOfferedOnlyOnRequest() throws Exception {
        DocumentBookmarkLibrary first = open();
        first.add(BookmarkLibrary.ROOT, 0, draft("A"));
        first.add(BookmarkLibrary.ROOT, 1, draft("B"));
        String damaged = document().substring(0, 120);
        Files.writeString(folder.resolve("bookmarks.json"), damaged);
        DocumentBookmarkLibrary lib = open();
        LibraryState.Unreadable state = assertInstanceOf(LibraryState.Unreadable.class, lib.state());
        assertEquals(1, state.backup().orElseThrow().bookmarks(), "the last library that read as usable");
        assertThrows(IllegalStateException.class, () -> lib.add(BookmarkLibrary.ROOT, 0, draft("C")));
        assertEquals(damaged, document(), "never overwritten by the start that found it");
        lib.recoverFromBackup();
        assertEquals(1, bookmarks(lib));
        assertEquals(damaged, Files.readString(folder.resolve("bookmarks-unreadable-20261102T200000Z.json")),
                "the damaged document is kept");
        assertEquals(1, bookmarks(open()));
    }

    @Test
    void aRestoreFromAFileRecoversAnUnreadableDocumentAndKeepsIt(@TempDir Path elsewhere) throws Exception {
        DocumentBookmarkLibrary lib = open();
        lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        Path exported = elsewhere.resolve("export.json");
        lib.exportTo(exported);
        Files.writeString(folder.resolve("bookmarks.json"), "not json");
        DocumentBookmarkLibrary damaged = open();
        assertInstanceOf(LibraryState.Unreadable.class, damaged.state());
        Path kept = damaged.restoreFrom(exported);
        assertEquals("not json", Files.readString(kept), "the damaged document kept");
        assertEquals(1, bookmarks(damaged));
        Files.writeString(folder.resolve("bookmarks.json"),
                document().replace("\"version\": 1", "\"version\": 2"));
        DocumentBookmarkLibrary newer = open();
        assertThrows(IllegalStateException.class, () -> newer.restoreFrom(exported), "a newer document is never written");
    }

    @Test
    void anUnreadableDocumentWithoutABackupOffersNothing() throws Exception {
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("bookmarks.json"), "not json");
        LibraryState.Unreadable state = assertInstanceOf(LibraryState.Unreadable.class, open().state());
        assertTrue(state.backup().isEmpty());
    }

    @Test
    void aNewerDocumentIsNeverWritten() throws Exception {
        open().add(BookmarkLibrary.ROOT, 0, draft("A"));
        String newer = document().replace("\"version\": 1", "\"version\": 3");
        Files.writeString(folder.resolve("bookmarks.json"), newer);
        DocumentBookmarkLibrary lib = open();
        assertEquals(3, assertInstanceOf(LibraryState.Newer.class, lib.state()).version());
        assertThrows(IllegalStateException.class, () -> lib.add(BookmarkLibrary.ROOT, 0, draft("B")));
        assertEquals(newer, document());
    }

    @Test
    void exportThenRestoreIsExactAndTheRestoreCanBeUndone(@TempDir Path elsewhere) throws Exception {
        DocumentBookmarkLibrary lib = open();
        lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        lib.addFolder(BookmarkLibrary.ROOT, 1, "Events");
        Path exported = elsewhere.resolve("my-bookmarks.json");
        lib.exportTo(exported);
        assertEquals(document(), Files.readString(exported), "the export is the library's canonical text");
        lib.add(BookmarkLibrary.ROOT, 0, draft("B"));
        String before = document();
        assertEquals(new BookmarkLibrary.RestoreSummary.Restorable(1, 1, 0, "6.2.0-test"), lib.inspect(exported));
        Path kept = lib.restoreFrom(exported);
        assertEquals(Files.readString(exported), document(), "byte for byte");
        assertEquals(before, Files.readString(kept), "the library before the restore is kept");
        lib.restoreFrom(kept);
        assertEquals(before, document(), "and restoring it undoes the restore");
    }

    @Test
    void aSourceThatCannotBeRestoredIsRefusedBeforeAnythingIsTouched(@TempDir Path elsewhere) throws Exception {
        DocumentBookmarkLibrary lib = open();
        lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        String before = document();
        Path foreign = elsewhere.resolve("other.json");
        Files.writeString(foreign, "{\"bookmarks\": []}");
        assertInstanceOf(BookmarkLibrary.RestoreSummary.Refused.class, lib.inspect(foreign));
        assertThrows(IOException.class, () -> lib.restoreFrom(foreign));
        Path newer = elsewhere.resolve("newer.json");
        Files.writeString(newer, before.replace("\"version\": 1", "\"version\": 2"));
        assertTrue(((BookmarkLibrary.RestoreSummary.Refused) lib.inspect(newer)).reason().contains("newer"));
        assertEquals(before, document());
    }

    @Test
    void theFolderIsTheSystemsOwnUnlessThePropertyNamesAnother() {
        assertEquals(Path.of("/Users/r/Library/Application Support/JUranometria"),
                BookmarkFolder.resolve("Mac OS X", "/Users/r", Map.of(), null));
        assertEquals(Path.of("C:/Users/r/AppData/Roaming", "JUranometria"),
                BookmarkFolder.resolve("Windows 11", "C:/Users/r", Map.of("APPDATA", "C:/Users/r/AppData/Roaming"), null));
        assertEquals(Path.of("/home/r/.local/share/juranometria"),
                BookmarkFolder.resolve("Linux", "/home/r", Map.of(), null));
        assertEquals(Path.of("/data/xdg/juranometria"),
                BookmarkFolder.resolve("Linux", "/home/r", Map.of("XDG_DATA_HOME", "/data/xdg"), null));
        assertEquals(Path.of("/tmp/elsewhere"),
                BookmarkFolder.resolve("Mac OS X", "/Users/r", Map.of(), "/tmp/elsewhere"));
    }
}
