package juranometria.bookmark;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The bookmark library kept as one local document (#497 ruling 7;
 * docs/decisions/bookmarks.md, sections 7-9): {@code bookmarks.json} in
 * the folder {@link BookmarkFolder} names, with {@code bookmarks.json.bak}
 * as the last library that read as usable.
 *
 * <p><b>The write.</b> The whole document goes to {@code bookmarks.json.part}
 * beside it and is forced to the disk; the current document, if it reads
 * as usable, is copied over the backup; then the temporary file is moved
 * onto the document atomically - refused, never copied, where the file
 * system cannot move atomically. An interruption at any point leaves the
 * previous library as the document or the backup.
 *
 * <p><b>The start.</b> A stray temporary file is deleted unread. A missing
 * document is an empty library; nothing is written until the reader adds
 * something. A document that cannot be read is kept unchanged and never
 * overwritten: the library is {@link LibraryState.Unreadable}, offering the
 * backup if it reads as usable. A document from a later version is
 * {@link LibraryState.Newer}: never reinterpreted, never written.
 */
public final class DocumentBookmarkLibrary extends TreeBookmarkLibrary {

    static final String DOCUMENT = "bookmarks.json";
    static final String BACKUP = "bookmarks.json.bak";
    static final String PART = "bookmarks.json.part";

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    /** A move the file system performs atomically or refuses (a seam for tests). */
    interface Mover {
        void move(Path from, Path to) throws IOException;
    }

    private final Path folder;
    private final String writtenBy;
    private final Clock clock;
    private final Mover mover;

    private DocumentBookmarkLibrary(Path folder, String writtenBy, Supplier<String> ids,
                                    Clock clock, Mover mover, LibraryState initial) {
        super(ids, initial);
        this.folder = folder;
        this.writtenBy = writtenBy;
        this.clock = clock;
        this.mover = mover;
    }

    /**
     * Opens the library in a folder, as the application's start does.
     * Never throws: a folder or document that cannot be read is an
     * {@link LibraryState.Unreadable} library, so bookmarks can never stop
     * the atlas from starting.
     */
    public static DocumentBookmarkLibrary open(Path folder, String writtenBy) {
        return open(folder, writtenBy, () -> UUID.randomUUID().toString(), Clock.systemUTC(),
                DocumentBookmarkLibrary::atomicMove);
    }

    static DocumentBookmarkLibrary open(Path folder, String writtenBy, Supplier<String> ids,
                                        Clock clock, Mover mover) {
        LibraryState initial;
        try {
            Files.deleteIfExists(folder.resolve(PART));
            initial = read(folder, writtenBy);
        } catch (IOException | RuntimeException unreadable) {
            initial = new LibraryState.Unreadable("the bookmark folder cannot be read: "
                    + unreadable.getMessage(), Optional.empty());
        }
        return new DocumentBookmarkLibrary(folder, writtenBy, ids, clock, mover, initial);
    }

    private static LibraryState read(Path folder, String writtenBy) throws IOException {
        Path document = folder.resolve(DOCUMENT);
        if (!Files.exists(document)) {
            return new LibraryState.Available(Library.empty(writtenBy));
        }
        String text;
        try {
            text = Files.readString(document, StandardCharsets.UTF_8);
        } catch (java.nio.charset.CharacterCodingException notUtf8) {
            return unreadable(folder, "the document is not UTF-8 text");
        }
        return switch (BookmarkDocument.read(text)) {
            case BookmarkDocument.Usable u -> new LibraryState.Available(u.library());
            case BookmarkDocument.Newer n -> new LibraryState.Newer(n.version(), n.writtenBy());
            case BookmarkDocument.NotOurs n -> unreadable(folder, n.reason());
        };
    }

    private static LibraryState unreadable(Path folder, String reason) throws IOException {
        Path backup = folder.resolve(BACKUP);
        Optional<LibraryState.BackupOffer> offer = Optional.empty();
        if (Files.exists(backup) && BookmarkDocument.read(
                Files.readString(backup, StandardCharsets.UTF_8)) instanceof BookmarkDocument.Usable u) {
            offer = Optional.of(new LibraryState.BackupOffer(u.library().bookmarks().size(),
                    u.library().folders().size(), Files.getLastModifiedTime(backup).toInstant()));
        }
        return new LibraryState.Unreadable(reason, offer);
    }

    /** The folder this library keeps its document in. */
    public Path folder() {
        return folder;
    }

    @Override
    protected void keep(Library next) throws IOException {
        Library stamped = new Library(writtenBy, next.root());
        write(BookmarkDocument.text(stamped));
    }

    @Override
    protected void adopt(Library next) throws IOException {
        super.adopt(new Library(writtenBy, next.root()));
    }

    private void write(String text) throws IOException {
        Files.createDirectories(folder);
        Path document = folder.resolve(DOCUMENT);
        Path part = folder.resolve(PART);
        try {
            try (FileChannel channel = FileChannel.open(part, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer bytes = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) {
                    channel.write(bytes);
                }
                channel.force(true);
            }
            if (Files.exists(document) && BookmarkDocument.read(
                    Files.readString(document, StandardCharsets.UTF_8)) instanceof BookmarkDocument.Usable) {
                Files.copy(document, folder.resolve(BACKUP), StandardCopyOption.REPLACE_EXISTING);
            }
            mover.move(part, document);
        } finally {
            Files.deleteIfExists(part);
        }
    }

    private static void atomicMove(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            throw new IOException("this folder cannot replace a file atomically, so nothing was"
                    + " replaced: " + to.getParent(), notAtomic);
        }
    }

    @Override
    public void exportTo(Path destination) throws IOException {
        String text = BookmarkDocument.text(available());
        Path parent = destination.toAbsolutePath().getParent();
        Path part = Files.createTempFile(parent, destination.getFileName() + ".", ".part");
        try {
            Files.writeString(part, text, StandardCharsets.UTF_8);
            mover.move(part, destination);
        } finally {
            Files.deleteIfExists(part);
        }
    }

    @Override
    public RestoreSummary inspect(Path source) throws IOException {
        return summary(source);
    }

    static RestoreSummary summary(Path source) throws IOException {
        String text;
        try {
            text = Files.readString(source, StandardCharsets.UTF_8);
        } catch (java.nio.charset.CharacterCodingException notUtf8) {
            return new RestoreSummary.Refused("the file is not UTF-8 text");
        }
        return switch (BookmarkDocument.read(text)) {
            case BookmarkDocument.Usable u -> new RestoreSummary.Restorable(
                    u.library().bookmarks().size(), u.library().folders().size(),
                    u.library().kept().size(), u.library().writtenBy());
            case BookmarkDocument.Newer n -> new RestoreSummary.Refused(
                    "the file was written by a newer JUranometria (bookmark format version "
                            + n.version() + ")");
            case BookmarkDocument.NotOurs n -> new RestoreSummary.Refused(n.reason());
        };
    }

    /**
     * Replaces the library with a validated source. Allowed on an available
     * library and on an unreadable one - a restore from an export is a way
     * back from a damaged document, which is kept, as the current document
     * always is, beside the library. Refused for a newer document, which is
     * never written.
     */
    @Override
    public Path restoreFrom(Path source) throws IOException {
        if (!(state instanceof LibraryState.Available) && !(state instanceof LibraryState.Unreadable)) {
            throw new IllegalStateException("the bookmark library cannot be changed now: " + state);
        }
        if (!(BookmarkDocument.read(Files.readString(source, StandardCharsets.UTF_8))
                instanceof BookmarkDocument.Usable u)) {
            throw new IOException("not a restorable bookmark library: " + source);
        }
        Files.createDirectories(folder);
        Path kept = folder.resolve("bookmarks-before-restore-" + STAMP.format(clock.instant()) + ".json");
        Path document = folder.resolve(DOCUMENT);
        if (Files.exists(document)) {
            Files.copy(document, kept, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.writeString(kept, BookmarkDocument.text(Library.empty(writtenBy)), StandardCharsets.UTF_8);
        }
        Library restored = new Library(writtenBy, u.library().root());
        write(BookmarkDocument.text(restored));
        state = new LibraryState.Available(restored);
        changed();
        return kept;
    }

    @Override
    public void recoverFromBackup() throws IOException {
        if (!(state instanceof LibraryState.Unreadable unreadable) || unreadable.backup().isEmpty()) {
            throw new IllegalStateException("there is no unreadable document with a backup to recover");
        }
        Path backup = folder.resolve(BACKUP);
        String text = Files.readString(backup, StandardCharsets.UTF_8);
        if (!(BookmarkDocument.read(text) instanceof BookmarkDocument.Usable u)) {
            throw new IOException("the backup no longer reads as a usable library");
        }
        Path document = folder.resolve(DOCUMENT);
        Path damaged = folder.resolve("bookmarks-unreadable-" + STAMP.format(clock.instant()) + ".json");
        Files.copy(document, damaged, StandardCopyOption.REPLACE_EXISTING);
        Path part = folder.resolve(PART);
        try {
            Files.writeString(part, text, StandardCharsets.UTF_8);
            mover.move(part, document);
        } finally {
            Files.deleteIfExists(part);
        }
        state = new LibraryState.Available(u.library());
        changed();
    }
}
