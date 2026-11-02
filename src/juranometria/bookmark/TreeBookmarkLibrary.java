package juranometria.bookmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The library operations over an immutable tree, shared by every
 * implementation: each change builds the next library, asks the
 * implementation to keep it, and adopts it only once it is kept.
 */
abstract class TreeBookmarkLibrary implements BookmarkLibrary {

    private final Supplier<String> ids;
    private final List<Runnable> listeners = new ArrayList<>();
    protected LibraryState state;

    TreeBookmarkLibrary(Supplier<String> ids, LibraryState initial) {
        this.ids = ids;
        this.state = initial;
    }

    /** Keeps the next library; throws, leaving everything as it was, if it cannot. */
    protected abstract void keep(Library next) throws IOException;

    @Override
    public LibraryState state() {
        return state;
    }

    protected Library available() {
        if (state instanceof LibraryState.Available a) {
            return a.library();
        }
        throw new IllegalStateException("the bookmark library cannot be changed now: " + state);
    }

    protected void adopt(Library next) throws IOException {
        keep(next);
        state = new LibraryState.Available(next);
        changed();
    }

    protected void changed() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }

    @Override
    public String add(String parentId, int position, Draft draft) throws IOException {
        String id = ids.get();
        adopt(LibraryEdits.insert(available(), parentId, position, bookmark(id, draft)));
        return id;
    }

    @Override
    public void update(String bookmarkId, Draft draft) throws IOException {
        adopt(LibraryEdits.replace(available(), bookmarkId, old -> {
            if (!(old instanceof Node.Bookmark)) {
                throw new IllegalArgumentException(bookmarkId + " is not a bookmark");
            }
            return bookmark(bookmarkId, draft);
        }));
    }

    @Override
    public void updateAndMove(String bookmarkId, Draft draft, String parentId) throws IOException {
        Library library = available();
        Library next = LibraryEdits.replace(library, bookmarkId, old -> {
            if (!(old instanceof Node.Bookmark)) {
                throw new IllegalArgumentException(bookmarkId + " is not a bookmark");
            }
            return bookmark(bookmarkId, draft);
        });
        if (!parentId.equals(parentOf(library, bookmarkId))) {
            next = LibraryEdits.move(next, bookmarkId, parentId, Integer.MAX_VALUE);
        }
        adopt(next);
    }

    @Override
    public void renameAndMove(String folderId, String title, String parentId) throws IOException {
        Library library = available();
        Library next = LibraryEdits.replace(library, folderId, old -> {
            if (!(old instanceof Node.Folder f)) {
                throw new IllegalArgumentException(folderId + " is not a folder");
            }
            return new Node.Folder(f.id(), title, f.children());
        });
        if (!parentId.equals(parentOf(library, folderId))) {
            next = LibraryEdits.move(next, folderId, parentId, Integer.MAX_VALUE);
        }
        adopt(next);
    }

    /** The id of the folder holding an entry; the root's for a top-level one. */
    static String parentOf(Library library, String id) {
        return parentIn(library.root(), ROOT, id).orElseThrow(
                () -> new IllegalArgumentException("no record " + id + " in the library"));
    }

    private static java.util.Optional<String> parentIn(List<Node> nodes, String parent, String id) {
        for (Node n : nodes) {
            if (n.id().equals(id)) {
                return java.util.Optional.of(parent);
            }
            if (n instanceof Node.Folder f) {
                java.util.Optional<String> inner = parentIn(f.children(), f.id(), id);
                if (inner.isPresent()) {
                    return inner;
                }
            }
        }
        return java.util.Optional.empty();
    }

    @Override
    public String addFolder(String parentId, int position, String title) throws IOException {
        String id = ids.get();
        adopt(LibraryEdits.insert(available(), parentId, position,
                new Node.Folder(id, title, List.of())));
        return id;
    }

    @Override
    public void rename(String folderId, String title) throws IOException {
        adopt(LibraryEdits.replace(available(), folderId, old -> {
            if (!(old instanceof Node.Folder f)) {
                throw new IllegalArgumentException(folderId + " is not a folder");
            }
            return new Node.Folder(f.id(), title, f.children());
        }));
    }

    @Override
    public void move(String id, String newParentId, int position) throws IOException {
        adopt(LibraryEdits.move(available(), id, newParentId, position));
    }

    @Override
    public void remove(String id) throws IOException {
        Library library = available();
        if (library.find(id).orElse(null) instanceof Node.Folder) {
            throw new IllegalArgumentException("a folder is removed with removeFolder and a choice");
        }
        adopt(LibraryEdits.remove(library, id));
    }

    @Override
    public void removeFolder(String folderId, Contents contents) throws IOException {
        adopt(LibraryEdits.removeFolder(available(), folderId, contents));
    }

    @Override
    public Runnable onChange(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private static Node.Bookmark bookmark(String id, Draft d) {
        return new Node.Bookmark(id, d.title(), d.view(), d.subjects(), d.primary(), d.time(),
                d.place(), d.note());
    }
}
