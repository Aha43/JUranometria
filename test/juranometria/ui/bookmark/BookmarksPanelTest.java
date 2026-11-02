package juranometria.ui.bookmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.bookmark.BookmarkLibrary;
import juranometria.bookmark.ChartTime;
import juranometria.bookmark.DocumentBookmarkLibrary;
import juranometria.bookmark.InMemoryBookmarkLibrary;
import juranometria.bookmark.Library;
import juranometria.bookmark.LibraryState;
import juranometria.bookmark.Node;
import juranometria.bookmark.SavedView;
import juranometria.bookmark.Subject;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Bookmarks tab (#681 stage 3): the library as a tree, details without
 * opening, explicit opening, reordering, and a folder's deletion that keeps
 * its contents unless deleting them is chosen (#497 ruling 5); a library
 * that cannot be shown says so.
 */
class BookmarksPanelTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");

    @TempDir
    Path folder;

    private static BookmarkLibrary.Draft draft(String title, boolean timed) {
        return new BookmarkLibrary.Draft(title, new SavedView(303.027292, 38.354944, 8),
                List.of(new Subject(Subject.Kind.DEEP_SKY, "NGC 6888")),
                Optional.of(new Subject(Subject.Kind.DEEP_SKY, "NGC 6888")),
                timed ? Optional.of(new ChartTime(Instant.parse("2026-11-02T20:56:48Z"),
                        ChartTime.Purpose.OBSERVATION)) : Optional.empty(),
                Optional.empty(), Optional.of("First time with the Seestar"));
    }

    private static final class Host implements BookmarksPanel.Host {
        final List<String> opened = new ArrayList<>();

        @Override
        public BookmarkOpener.Report open(Node.Bookmark bookmark) {
            opened.add(bookmark.id());
            return new BookmarkOpener.Report(List.of(new BookmarkOpener.SubjectReport(
                    bookmark.subjects().get(0), BookmarkOpener.Outcome.HIDDEN_BY_OPTIONS)),
                    false, false, List.of());
        }

        @Override
        public BookmarkCapture.Proposal now() {
            throw new UnsupportedOperationException();
        }
    }

    private static BookmarksPanel panel(BookmarkLibrary library, Host host) throws Exception {
        BookmarksPanel[] made = new BookmarksPanel[1];
        SwingUtilities.invokeAndWait(() -> made[0] = new BookmarksPanel(EN, library, host, Subject::id));
        return made[0];
    }

    private static List<String> rows(BookmarksPanel p) {
        List<String> out = new ArrayList<>();
        for (int row = 0; row < p.tree.getRowCount(); row++) {
            Object node = ((javax.swing.tree.DefaultMutableTreeNode)
                    p.tree.getPathForRow(row).getLastPathComponent()).getUserObject();
            out.add(node instanceof Node.Folder f ? f.title() + "/"
                    : node instanceof Node.Bookmark b ? b.title() : String.valueOf(node));
        }
        return out;
    }

    @Test
    void theLibraryIsShownAsATreeAndChoosingShowsDetailsWithoutOpening() throws Exception {
        InMemoryBookmarkLibrary library = new InMemoryBookmarkLibrary(Library.empty("t"));
        String events = library.addFolder(BookmarkLibrary.ROOT, 0, "Sky events");
        String crescent = library.add(events, 0, draft("Crescent", true));
        library.add(BookmarkLibrary.ROOT, 1, draft("Plain", false));
        Host host = new Host();
        BookmarksPanel p = panel(library, host);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(List.of("Bookmarks", "Sky events/", "Crescent", "Plain"), rows(p));
            p.select(crescent);
            assertTrue(p.details.getText().contains("Observation time: 2026-11-02 20:56:48 UTC"));
            assertTrue(p.details.getText().contains("★ NGC 6888"), "the primary marked");
            assertTrue(p.details.getText().contains("First time with the Seestar"));
            assertTrue(p.open.isEnabled());
        });
        assertTrue(host.opened.isEmpty(), "choosing never opens");
        SwingUtilities.invokeAndWait(p::openChosen);
        assertEquals(List.of(crescent), host.opened);
        SwingUtilities.invokeAndWait(() -> assertTrue(p.message.getAccessibleContext().getAccessibleName()
                .contains("NGC 6888 is hidden by your current chart options."), "the report is said"));
    }

    @Test
    void aFoldersDeletionKeepsItsContentsUnlessDeletingThemIsChosen() throws Exception {
        InMemoryBookmarkLibrary library = new InMemoryBookmarkLibrary(Library.empty("t"));
        String events = library.addFolder(BookmarkLibrary.ROOT, 0, "Sky events");
        library.add(events, 0, draft("A", false));
        library.add(events, 1, draft("B", false));
        BookmarksPanel p = panel(library, new Host());
        SwingUtilities.invokeAndWait(() -> {
            p.asker = (parent, question, title, options) -> 0;
            p.select(events);
            p.deleteChosen();
        });
        assertEquals(List.of("A", "B"), ((LibraryState.Available) library.state()).library().bookmarks()
                .stream().map(Node.Bookmark::title).toList());
        assertTrue(((LibraryState.Available) library.state()).library().folders().isEmpty());

        String other = library.addFolder(BookmarkLibrary.ROOT, 0, "Other");
        library.add(other, 0, draft("C", false));
        SwingUtilities.invokeAndWait(() -> {
            p.rebuild();
            p.asker = (parent, question, title, options) -> 2;
            p.select(other);
            p.deleteChosen();
        });
        assertEquals(3, ((LibraryState.Available) library.state()).library().bookmarks().size(), "Cancel deletes nothing");
        SwingUtilities.invokeAndWait(() -> {
            p.asker = (parent, question, title, options) -> 1;
            p.select(other);
            p.deleteChosen();
        });
        assertEquals(List.of("A", "B"), ((LibraryState.Available) library.state()).library().bookmarks()
                .stream().map(Node.Bookmark::title).toList(), "deleted with its contents only when chosen");
    }

    @Test
    void theDeletionQuestionCountsEverythingInsideTheFolder() throws Exception {
        InMemoryBookmarkLibrary library = new InMemoryBookmarkLibrary(Library.empty("t"));
        String outer = library.addFolder(BookmarkLibrary.ROOT, 0, "Sky events");
        library.add(outer, 0, draft("A", false));
        String year = library.addFolder(outer, 1, "2026");
        library.add(year, 0, draft("B", false));
        String month = library.addFolder(year, 1, "November");
        library.add(month, 0, draft("C", false));
        BookmarksPanel p = panel(library, new Host());
        List<String> asked = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            p.asker = (parent, question, title, options) -> {
                asked.add(question);
                return 2;
            };
            p.select(outer);
            p.deleteChosen();
        });
        assertEquals(List.of("Delete the folder Sky events? Counting every folder inside it, it holds"
                + " 3 bookmarks, 2 folders and 0 records needing attention."), asked);
    }

    @Test
    void entriesMoveUpAndDownWithinTheirFolder() throws Exception {
        InMemoryBookmarkLibrary library = new InMemoryBookmarkLibrary(Library.empty("t"));
        library.add(BookmarkLibrary.ROOT, 0, draft("A", false));
        String b = library.add(BookmarkLibrary.ROOT, 1, draft("B", false));
        BookmarksPanel p = panel(library, new Host());
        SwingUtilities.invokeAndWait(() -> {
            p.select(b);
            p.shift(-1);
        });
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals(List.of("B", "A"), ((LibraryState.Available) library.state()).library().bookmarks()
                .stream().map(Node.Bookmark::title).toList());
    }

    @Test
    void aNewerLibraryIsSaidToBeSoAndNothingCanBeDone() throws Exception {
        Files.writeString(folder.resolve("bookmarks.json"),
                "{\"format\": \"juranometria-bookmarks\", \"version\": 2, \"writtenBy\": \"9.0.0\", \"root\": []}");
        BookmarksPanel p = panel(DocumentBookmarkLibrary.open(folder, "t"), new Host());
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(p.newFolder.isEnabled());
            assertFalse(p.open.isEnabled());
        });
    }
}
