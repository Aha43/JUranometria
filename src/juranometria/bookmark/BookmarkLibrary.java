package juranometria.bookmark;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The bookmark library, as every consumer sees it (#497 ruling 7): the
 * add dialog, the Bookmarks tab, the opener, export and restore all use
 * these bookmark-domain operations and nothing else. Where and how the
 * library is kept - the local document today - is hidden behind this
 * interface; nothing outside it sees a path inside the library's folder,
 * a JSON value or a file.
 *
 * <p>Every change is whole: it is kept before the method returns, or it
 * fails with an {@link IOException} and the library is as it was. A change
 * asked of a library that is not {@link LibraryState.Available} is
 * refused with an {@link IllegalStateException}.
 */
public interface BookmarkLibrary {

    /** The id of the permanent root folder, *Bookmarks* / *Bokmerker*. */
    String ROOT = "root";

    /** What the library can offer now. */
    LibraryState state();

    /** Adds a bookmark into a folder (or the root) at a position; returns its id. */
    String add(String parentId, int position, Draft draft) throws IOException;

    /** Replaces a bookmark's content; its place in the tree is unchanged. */
    void update(String bookmarkId, Draft draft) throws IOException;

    /**
     * Replaces a bookmark's content and, if the folder differs, moves it to
     * the end of that folder - as one change: both are kept, or neither is.
     */
    void updateAndMove(String bookmarkId, Draft draft, String parentId) throws IOException;

    /**
     * Renames a folder and, if the parent differs, moves it to the end of
     * that folder - as one change: both are kept, or neither is.
     */
    void renameAndMove(String folderId, String title, String parentId) throws IOException;

    /** Adds a folder; returns its id. */
    String addFolder(String parentId, int position, String title) throws IOException;

    /** Renames a folder. */
    void rename(String folderId, String title) throws IOException;

    /**
     * Moves a bookmark, folder or kept record to a new parent and position.
     * Only membership and order change; content never does.
     */
    void move(String id, String newParentId, int position) throws IOException;

    /** Removes a bookmark or a kept record. */
    void remove(String id) throws IOException;

    /** How a folder's removal treats what it holds (#497 ruling 5). */
    enum Contents {
        /** Its contents move to its parent, in their order, where it stood. */
        KEEP,
        /** Its contents are deleted with it - only on the reader's explicit choice. */
        DELETE
    }

    /** Removes a folder, keeping or deleting its contents as chosen. */
    void removeFolder(String folderId, Contents contents) throws IOException;

    /** Writes the whole library, records needing attention included, to a chosen file. */
    void exportTo(Path destination) throws IOException;

    /** Reads a candidate restore without touching the library. */
    RestoreSummary inspect(Path source) throws IOException;

    /**
     * Replaces the library with a validated source, keeping the current
     * library first so the restore can be undone; returns where it was kept.
     */
    Path restoreFrom(Path source) throws IOException;

    /** Replaces an unreadable document with its offered backup, keeping the damaged one. */
    void recoverFromBackup() throws IOException;

    /** Listens for every change of state. */
    Runnable onChange(Runnable listener);

    /** A bookmark's content, as the add and edit surfaces produce it. */
    record Draft(String title, SavedView view, java.util.List<Subject> subjects,
                 Optional<Subject> primary, Optional<ChartTime> time, Optional<Place> place,
                 Optional<String> note) {
        public Draft {
            subjects = java.util.List.copyOf(subjects);
        }
    }

    /** What a restore source holds, or why it cannot be restored. */
    sealed interface RestoreSummary {
        /** A usable version-1 library. */
        record Restorable(int bookmarks, int folders, int needingAttention, String writtenBy)
                implements RestoreSummary {
        }

        /** Not restorable; the library is untouched. */
        record Refused(String reason) implements RestoreSummary {
        }
    }
}
