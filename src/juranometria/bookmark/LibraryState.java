package juranometria.bookmark;

import java.util.Optional;

/**
 * What the bookmark library can offer the reader right now (#497 ruling 8).
 * Only an available library is ever written.
 */
public sealed interface LibraryState {

    /** The library, usable; it may hold records needing attention. */
    record Available(Library library) implements LibraryState {
    }

    /**
     * The document was written by a later JUranometria: it is never
     * reinterpreted and never written; adding and editing are refused.
     */
    record Newer(int version, String writtenBy) implements LibraryState {
    }

    /**
     * The document cannot be read. It is kept unchanged, never
     * overwritten; a usable last-known-good backup, if there is one, is
     * offered and restored only on the reader's explicit yes.
     */
    record Unreadable(String reason, Optional<BackupOffer> backup) implements LibraryState {
    }

    /** A last-known-good library that can replace an unreadable document. */
    record BackupOffer(int bookmarks, int folders, java.time.Instant written) {
    }
}
