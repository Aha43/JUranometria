package juranometria.ui.bookmark;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.app.AppMenuBar;
import juranometria.bookmark.BookmarkLibrary;
import juranometria.bookmark.ChartTime;
import juranometria.bookmark.InMemoryBookmarkLibrary;
import juranometria.bookmark.Library;
import juranometria.bookmark.LibraryState;
import juranometria.bookmark.Node;
import juranometria.bookmark.Subject;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.ui.Shortcuts;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Add Bookmark... (#681 stage 2): the dialog's content on an in-memory
 * library - it shows every proposed value, saves only on Add, keeps the
 * reader's order and choice of primary, refuses a blank title and an
 * unavailable library with words - and the Bookmarks menu with its key.
 */
class AddBookmarkDialogTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-11-24T11:54:00Z"));
    private static final ChartViewState VIEW = new ChartViewState(
            new SkyPosition(56.893713, 24.029342), 2.0, 8.0, null, null);

    private static AddBookmarkDialog.Content content(BookmarkLibrary library, List<String> members,
                                                     String lead) throws Exception {
        AddBookmarkDialog.Content[] made = new AddBookmarkDialog.Content[1];
        SwingUtilities.invokeAndWait(() -> made[0] = new AddBookmarkDialog.Content(EN, library,
                BookmarkCapture.propose(VIEW, members, lead, OSLO), Subject::id, "Suggested"));
        return made[0];
    }

    private static Node.Bookmark only(BookmarkLibrary library) {
        List<Node.Bookmark> all = ((LibraryState.Available) library.state()).library().bookmarks();
        assertEquals(1, all.size());
        return all.get(0);
    }

    @Test
    void addSavesTheProposalWithTheExplicitChoicesAsShown() throws Exception {
        InMemoryBookmarkLibrary library = new InMemoryBookmarkLibrary(Library.empty("t"));
        AddBookmarkDialog.Content c = content(library, List.of("TYC 1800-2202-1", "moon"), "moon");
        AtomicBoolean closed = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            c.whenDone(() -> closed.set(true));
            assertEquals("Suggested", c.title.getText());
            assertTrue(c.time.isSelected(), "C-4: the Moon pre-ticks the time");
            assertTrue(c.time.getText().contains("2026-11-24 11:54 UTC"), "the instant written out");
            assertFalse(c.place.isSelected(), "the place is never pre-ticked");
            assertTrue(c.place.getText().contains("59.913°"));
            c.list.setSelectedIndex(1);
            c.list.clearSelection();
            c.add();
        });
        assertTrue(closed.get(), "Add closes the window");
        Node.Bookmark saved = only(library);
        assertEquals(List.of("TYC 1800-2202-1", "moon"), saved.subjects().stream().map(Subject::id).toList());
        assertEquals("moon", saved.primary().orElseThrow().id());
        assertEquals(ChartTime.Purpose.EVENT, saved.time().orElseThrow().purpose());
        assertTrue(saved.place().isEmpty());
    }

    @Test
    void theReadersOrderUntickingAndNoneAreKept() throws Exception {
        InMemoryBookmarkLibrary library = new InMemoryBookmarkLibrary(Library.empty("t"));
        AddBookmarkDialog.Content c = content(library, List.of("moon", "venus"), "moon");
        SwingUtilities.invokeAndWait(() -> {
            c.list.setSelectedIndex(1);
            c.shift(-1);
            c.primary.setSelectedIndex(0);
            c.time.setSelected(false);
            assertFalse(c.event.isEnabled(), "the purpose follows the box");
            c.add();
        });
        Node.Bookmark saved = only(library);
        assertEquals(List.of("venus", "moon"), saved.subjects().stream().map(Subject::id).toList());
        assertTrue(saved.primary().isEmpty(), "None: no primary");
        assertTrue(saved.time().isEmpty(), "unticked: no time");
    }

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path folder;

    @Test
    void aBlankTitleAndAnUnavailableLibraryAreRefusedWithWords() throws Exception {
        InMemoryBookmarkLibrary library = new InMemoryBookmarkLibrary(Library.empty("t"));
        AddBookmarkDialog.Content c = content(library, List.of(), null);
        SwingUtilities.invokeAndWait(() -> {
            c.title.setText("  ");
            c.add();
            assertEquals(EN.say("bookmark.refused.title"), c.status.getText());
        });
        assertTrue(((LibraryState.Available) library.state()).library().bookmarks().isEmpty());

        java.nio.file.Files.writeString(folder.resolve("bookmarks.json"),
                "{\"format\": \"juranometria-bookmarks\", \"version\": 2, \"writtenBy\": \"9.0.0\", \"root\": []}");
        BookmarkLibrary newer = juranometria.bookmark.DocumentBookmarkLibrary.open(folder, "t");
        AddBookmarkDialog.Content refused = content(newer, List.of(), null);
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(refused.add.isEnabled());
            assertEquals(EN.say("bookmark.refused.newer"), refused.status.getText());
        });
    }

    @Test
    void theFolderChoiceListsTheRootAndEveryFolderByItsPath() throws Exception {
        InMemoryBookmarkLibrary library = new InMemoryBookmarkLibrary(Library.empty("t"));
        String events = library.addFolder(BookmarkLibrary.ROOT, 0, "Sky events");
        library.addFolder(events, 0, "2026");
        List<String> shown = AddBookmarkDialog.folders(library, EN).stream()
                .map(AddBookmarkDialog.FolderChoice::shown).toList();
        assertEquals(List.of("Bookmarks", "Bookmarks ▸ Sky events", "Bookmarks ▸ Sky events ▸ 2026"), shown);
    }

    @Test
    void theBookmarksMenuStandsBeforeHelpWithCommandD() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JMenuBar bar = new JMenuBar();
            bar.add(new JMenu(EN.say("menu.view.label")));
            bar.add(new JMenu(EN.say("menu.help.label")));
            JMenu menu = AppMenuBar.addBookmarks(bar, EN, () -> { });
            assertNotNull(menu);
            assertEquals(1, bar.getComponentIndex(menu), "between the rest and Help");
            assertEquals('B', menu.getMnemonic());
            JMenuItem add = menu.getItem(0);
            assertEquals(AppMenuBar.ADD_BOOKMARK_ITEM, add.getName());
            assertEquals(Shortcuts.of(Shortcuts.ADD_BOOKMARK).stroke(), add.getAccelerator());
            assertEquals(1, add.getActionListeners().length, "the item opens the dialog");
        });
        assertEquals(java.awt.event.KeyEvent.VK_D, Shortcuts.of(Shortcuts.ADD_BOOKMARK).stroke().getKeyCode());
    }
}
