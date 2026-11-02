package juranometria.bookmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The whole library: the permanent root's children in the user's order,
 * and the version that last wrote it. The root itself is never stored as
 * text; the interface names it *Bookmarks* / *Bokmerker*.
 */
public record Library(String writtenBy, List<Node> root) {

    /** A library with nothing in it, as a new profile starts. */
    public static Library empty(String writtenBy) {
        return new Library(writtenBy, List.of());
    }

    public Library {
        writtenBy = writtenBy == null ? "" : writtenBy;
        root = List.copyOf(root);
    }

    /** Every bookmark, depth first in the user's order. */
    public List<Node.Bookmark> bookmarks() {
        return collect(root, Node.Bookmark.class);
    }

    /** Every folder, depth first. */
    public List<Node.Folder> folders() {
        return collect(root, Node.Folder.class);
    }

    /** Every record needing attention, depth first. */
    public List<Node.Kept> kept() {
        return collect(root, Node.Kept.class);
    }

    /** The node with this id, wherever it is. */
    public Optional<Node> find(String id) {
        return find(root, id);
    }

    private static Optional<Node> find(List<Node> nodes, String id) {
        for (Node n : nodes) {
            if (n.id().equals(id)) {
                return Optional.of(n);
            }
            if (n instanceof Node.Folder f) {
                Optional<Node> inner = find(f.children(), id);
                if (inner.isPresent()) {
                    return inner;
                }
            }
        }
        return Optional.empty();
    }

    private static <T extends Node> List<T> collect(List<Node> nodes, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Node n : nodes) {
            if (type.isInstance(n)) {
                out.add(type.cast(n));
            }
            if (n instanceof Node.Folder f) {
                out.addAll(collect(f.children(), type));
            }
        }
        return out;
    }
}
