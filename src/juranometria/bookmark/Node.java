package juranometria.bookmark;

import java.util.List;
import java.util.Optional;

/**
 * One entry of the library's tree: a folder, a bookmark, or a record
 * this version could not use and keeps exactly as it was read.
 */
public sealed interface Node {

    /** The record's stable identity; empty only for a kept record that had none. */
    String id();

    /** A user folder, its children in the user's order. */
    record Folder(String id, String title, List<Node> children) implements Node {
        public Folder {
            if (id == null || id.isBlank() || title == null || title.isBlank()) {
                throw new IllegalArgumentException("a folder has an id and a title");
            }
            children = List.copyOf(children);
        }
    }

    /**
     * A bookmark: a saved view, optionally enriched (#497). The subjects
     * are in the user's order; the primary is held by identity, never by
     * position, and empty means opening changes no selection.
     */
    record Bookmark(String id, String title, SavedView view, List<Subject> subjects,
                    Optional<Subject> primary, Optional<ChartTime> time,
                    Optional<Place> place, Optional<String> note) implements Node {
        public Bookmark {
            if (id == null || id.isBlank() || title == null || title.isBlank() || view == null) {
                throw new IllegalArgumentException("a bookmark has an id, a title and a view");
            }
            subjects = List.copyOf(subjects);
            if (subjects.size() != new java.util.HashSet<>(subjects).size()) {
                throw new IllegalArgumentException("a subject appears once");
            }
            if (primary.isPresent() && !subjects.contains(primary.get())) {
                throw new IllegalArgumentException("the primary is one of the subjects");
            }
        }
    }

    /**
     * A record this version cannot use: kept as read, written back
     * unchanged, reported with its reasons, and never opened.
     */
    record Kept(String id, Json.Value raw, List<String> reasons) implements Node {
        public Kept {
            reasons = List.copyOf(reasons);
        }
    }
}
