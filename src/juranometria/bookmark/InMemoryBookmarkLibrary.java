package juranometria.bookmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A bookmark library held in memory for one process: the tests' and the
 * photographers' library. It keeps nothing on disk except what export
 * writes and reads nothing except what restore reads.
 */
public final class InMemoryBookmarkLibrary extends TreeBookmarkLibrary {

    public InMemoryBookmarkLibrary(Library initial) {
        this(initial, () -> UUID.randomUUID().toString());
    }

    public InMemoryBookmarkLibrary(Library initial, Supplier<String> ids) {
        super(ids, new LibraryState.Available(initial));
    }

    @Override
    protected void keep(Library next) {
        // Held in memory; nothing to write.
    }

    @Override
    public void exportTo(Path destination) throws IOException {
        Files.writeString(destination, BookmarkDocument.text(available()), StandardCharsets.UTF_8);
    }

    @Override
    public RestoreSummary inspect(Path source) throws IOException {
        return DocumentBookmarkLibrary.summary(source);
    }

    @Override
    public Path restoreFrom(Path source) throws IOException {
        Library current = available();
        if (!(BookmarkDocument.read(Files.readString(source, StandardCharsets.UTF_8))
                instanceof BookmarkDocument.Usable u)) {
            throw new IOException("not a restorable bookmark library: " + source);
        }
        Path kept = Files.createTempFile("bookmarks-before-restore-", ".json");
        Files.writeString(kept, BookmarkDocument.text(current), StandardCharsets.UTF_8);
        adopt(u.library());
        return kept;
    }

    @Override
    public void recoverFromBackup() {
        throw new IllegalStateException("an in-memory library is never unreadable");
    }
}
