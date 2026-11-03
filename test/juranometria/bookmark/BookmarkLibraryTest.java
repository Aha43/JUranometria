package juranometria.bookmark;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bookmark library's domain operations (#681 stage 1), on the
 * in-memory implementation every consumer's test uses: folders and user
 * order, moving without changing content, and a folder's removal that
 * never silently deletes what it holds (#497 ruling 5).
 */
class BookmarkLibraryTest {

    private static BookmarkLibrary.Draft draft(String title) {
        return new BookmarkLibrary.Draft(title, new SavedView(10, 20, 8),
                List.of(new Subject(Subject.Kind.BODY, "moon"), new Subject(Subject.Kind.DEEP_SKY, "NGC 598")),
                Optional.of(new Subject(Subject.Kind.DEEP_SKY, "NGC 598")), Optional.empty(),
                Optional.empty(), Optional.of("note"));
    }

    private static InMemoryBookmarkLibrary library() {
        AtomicInteger next = new AtomicInteger();
        return new InMemoryBookmarkLibrary(Library.empty("t"), () -> "id-" + next.incrementAndGet());
    }

    private static Library tree(BookmarkLibrary library) {
        return ((LibraryState.Available) library.state()).library();
    }

    private static List<String> titles(List<Node> nodes) {
        return nodes.stream().map(n -> switch (n) {
            case Node.Folder f -> f.title() + "/";
            case Node.Bookmark b -> b.title();
            case Node.Kept k -> "kept";
        }).toList();
    }

    @Test
    void bookmarksAndFoldersStandWhereTheUserPutsThem() throws Exception {
        BookmarkLibrary lib = library();
        String a = lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        String events = lib.addFolder(BookmarkLibrary.ROOT, 1, "Events");
        String b = lib.add(events, 0, draft("B"));
        lib.add(BookmarkLibrary.ROOT, 0, draft("C"));
        assertEquals(List.of("C", "A", "Events/"), titles(tree(lib).root()));
        Node.Bookmark stored = (Node.Bookmark) tree(lib).find(b).orElseThrow();
        assertEquals(List.of("moon", "NGC 598"), stored.subjects().stream().map(Subject::id).toList(),
                "the subjects in the user's order, the primary second");
        assertEquals("NGC 598", stored.primary().orElseThrow().id(), "the primary by identity");
        lib.move(a, events, 0);
        assertEquals(List.of("C", "Events/"), titles(tree(lib).root()));
        Node.Folder folder = (Node.Folder) tree(lib).find(events).orElseThrow();
        assertEquals(List.of("A", "B"), titles(folder.children()));
        assertEquals(draft("A").subjects(), ((Node.Bookmark) tree(lib).find(a).orElseThrow()).subjects(),
                "moving changes no content");
    }

    @Test
    void editingAndRenamingChangeOnlyWhatTheyName() throws Exception {
        BookmarkLibrary lib = library();
        String folder = lib.addFolder(BookmarkLibrary.ROOT, 0, "Deep sky");
        String b = lib.add(folder, 0, draft("M33"));
        lib.update(b, draft("M33, wider"));
        lib.rename(folder, "Dyphimmel");
        assertEquals(List.of("Dyphimmel/"), titles(tree(lib).root()));
        assertEquals("M33, wider", ((Node.Bookmark) tree(lib).find(b).orElseThrow()).title());
        assertThrows(IllegalArgumentException.class, () -> lib.rename(b, "x"), "a bookmark is not renamed as a folder");
        assertThrows(IllegalArgumentException.class, () -> lib.remove(folder), "a folder needs the choice");
    }

    @Test
    void removingAFolderKeepsItsContentsUnlessDeletionIsChosen() throws Exception {
        BookmarkLibrary lib = library();
        lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        String folder = lib.addFolder(BookmarkLibrary.ROOT, 1, "Events");
        lib.add(folder, 0, draft("B"));
        lib.add(folder, 1, draft("C"));
        lib.add(BookmarkLibrary.ROOT, 2, draft("D"));
        lib.removeFolder(folder, BookmarkLibrary.Contents.KEEP);
        assertEquals(List.of("A", "B", "C", "D"), titles(tree(lib).root()),
                "the contents move to the parent, in order, where the folder stood");
        String other = lib.addFolder(BookmarkLibrary.ROOT, 0, "Other");
        lib.add(other, 0, draft("E"));
        lib.removeFolder(other, BookmarkLibrary.Contents.DELETE);
        assertEquals(List.of("A", "B", "C", "D"), titles(tree(lib).root()), "deleted only when chosen");
    }

    @Test
    void aFolderCannotMoveIntoItselfAndUnknownTargetsAreRefused() throws Exception {
        BookmarkLibrary lib = library();
        String outer = lib.addFolder(BookmarkLibrary.ROOT, 0, "Outer");
        String inner = lib.addFolder(outer, 0, "Inner");
        assertThrows(IllegalArgumentException.class, () -> lib.move(outer, inner, 0));
        assertThrows(IllegalArgumentException.class, () -> lib.move(outer, outer, 0));
        assertThrows(IllegalArgumentException.class, () -> lib.add("nowhere", 0, draft("X")));
        assertEquals(List.of("Outer/"), titles(tree(lib).root()), "nothing changed");
    }

    @Test
    void everyChangeIsAnnounced() throws Exception {
        BookmarkLibrary lib = library();
        AtomicInteger heard = new AtomicInteger();
        Runnable stop = lib.onChange(heard::incrementAndGet);
        lib.add(BookmarkLibrary.ROOT, 0, draft("A"));
        lib.addFolder(BookmarkLibrary.ROOT, 0, "F");
        stop.run();
        lib.add(BookmarkLibrary.ROOT, 0, draft("B"));
        assertEquals(2, heard.get());
        assertTrue(lib.state() instanceof LibraryState.Available);
    }
}
