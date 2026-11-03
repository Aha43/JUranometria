package juranometria.bookmark;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Pure edits of the library's tree, shared by every implementation: each
 * returns a new library and leaves the old one untouched, so a change that
 * cannot be kept is simply not adopted.
 */
final class LibraryEdits {

    private LibraryEdits() {
    }

    static Library insert(Library library, String parentId, int position, Node node) {
        if (library.find(node.id()).isPresent()) {
            throw new IllegalArgumentException("the id " + node.id() + " is already in the library");
        }
        return new Library(library.writtenBy(), childrenOf(library.root(), parentId,
                children -> {
                    List<Node> next = new ArrayList<>(children);
                    next.add(clamp(position, next.size()), node);
                    return next;
                }));
    }

    static Library replace(Library library, String id, Function<Node, Node> change) {
        Node old = library.find(id).orElseThrow(() -> unknown(id));
        Node replacement = change.apply(old);
        return new Library(library.writtenBy(), replaced(library.root(), id, replacement));
    }

    static Library remove(Library library, String id) {
        library.find(id).orElseThrow(() -> unknown(id));
        return new Library(library.writtenBy(), without(library.root(), id));
    }

    static Library removeFolder(Library library, String folderId, BookmarkLibrary.Contents contents) {
        Node node = library.find(folderId).orElseThrow(() -> unknown(folderId));
        if (!(node instanceof Node.Folder folder)) {
            throw new IllegalArgumentException(folderId + " is not a folder");
        }
        if (contents == BookmarkLibrary.Contents.DELETE) {
            return remove(library, folderId);
        }
        return new Library(library.writtenBy(), spliced(library.root(), folder));
    }

    static Library move(Library library, String id, String newParentId, int position) {
        Node node = library.find(id).orElseThrow(() -> unknown(id));
        if (node instanceof Node.Folder f && (f.id().equals(newParentId)
                || new Library("", f.children()).find(newParentId).isPresent())) {
            throw new IllegalArgumentException("a folder cannot move into itself");
        }
        if (!newParentId.equals(BookmarkLibrary.ROOT)
                && !(library.find(newParentId).orElse(null) instanceof Node.Folder)) {
            throw new IllegalArgumentException(newParentId + " is not a folder");
        }
        Library removed = new Library(library.writtenBy(), without(library.root(), id));
        return new Library(library.writtenBy(), childrenOf(removed.root(), newParentId,
                children -> {
                    List<Node> next = new ArrayList<>(children);
                    next.add(clamp(position, next.size()), node);
                    return next;
                }));
    }

    private static int clamp(int position, int size) {
        return Math.max(0, Math.min(position, size));
    }

    private static IllegalArgumentException unknown(String id) {
        return new IllegalArgumentException("no record " + id + " in the library");
    }

    private static List<Node> childrenOf(List<Node> nodes, String parentId,
                                         Function<List<Node>, List<Node>> change) {
        if (parentId.equals(BookmarkLibrary.ROOT)) {
            return change.apply(nodes);
        }
        boolean[] found = {false};
        List<Node> out = edit(nodes, parentId, change, found);
        if (!found[0]) {
            throw new IllegalArgumentException(parentId + " is not a folder");
        }
        return out;
    }

    private static List<Node> edit(List<Node> nodes, String parentId,
                                   Function<List<Node>, List<Node>> change, boolean[] found) {
        List<Node> out = new ArrayList<>();
        for (Node n : nodes) {
            if (n instanceof Node.Folder f) {
                if (f.id().equals(parentId)) {
                    found[0] = true;
                    out.add(new Node.Folder(f.id(), f.title(), change.apply(f.children())));
                } else {
                    out.add(new Node.Folder(f.id(), f.title(), edit(f.children(), parentId, change, found)));
                }
            } else {
                out.add(n);
            }
        }
        return out;
    }

    private static List<Node> replaced(List<Node> nodes, String id, Node replacement) {
        List<Node> out = new ArrayList<>();
        for (Node n : nodes) {
            if (n.id().equals(id)) {
                out.add(replacement);
            } else if (n instanceof Node.Folder f) {
                out.add(new Node.Folder(f.id(), f.title(), replaced(f.children(), id, replacement)));
            } else {
                out.add(n);
            }
        }
        return out;
    }

    private static List<Node> without(List<Node> nodes, String id) {
        List<Node> out = new ArrayList<>();
        for (Node n : nodes) {
            if (n.id().equals(id)) {
                continue;
            }
            if (n instanceof Node.Folder f) {
                out.add(new Node.Folder(f.id(), f.title(), without(f.children(), id)));
            } else {
                out.add(n);
            }
        }
        return out;
    }

    private static List<Node> spliced(List<Node> nodes, Node.Folder folder) {
        List<Node> out = new ArrayList<>();
        for (Node n : nodes) {
            if (n.id().equals(folder.id())) {
                out.addAll(folder.children());
            } else if (n instanceof Node.Folder f) {
                out.add(new Node.Folder(f.id(), f.title(), spliced(f.children(), folder)));
            } else {
                out.add(n);
            }
        }
        return out;
    }
}
