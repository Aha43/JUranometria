package juranometria.tool;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chooser photograph is of a chooser that has finished listing
 * (#393).
 *
 * <p>A {@code JFileChooser} lists its folder on a loader thread of its
 * own. PR #388's second CI run photographed the Norwegian chooser twice
 * and got two pictures, because nothing waited for that listing. These
 * contracts control when the listing completes - a folder view whose
 * listing is held until the test lets it go - so the outcome depends on
 * the readiness boundary alone, never on how fast a loader happens to
 * be.
 */
class ChooserListingReadinessTest {

    private static final List<String> ENTRIES = List.of("Kart", "Notater",
            "andromeda.pdf", "orion.svg", "perseus.png");

    /** A folder view that lists nothing until it is let go. */
    private static final class HeldListing extends FileSystemView {

        private final FileSystemView real =
                FileSystemView.getFileSystemView();
        final CountDownLatch release = new CountDownLatch(1);

        @Override
        public File[] getFiles(File dir, boolean useFileHiding) {
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return new File[0];
            }
            return real.getFiles(dir, useFileHiding);
        }

        @Override
        public File createNewFolder(File containingDir)
                throws java.io.IOException {
            return real.createNewFolder(containingDir);
        }
    }

    private static Path folder() throws Exception {
        Path folder = Files.createTempDirectory("chooser-listing");
        Files.createDirectory(folder.resolve("Kart"));
        Files.createDirectory(folder.resolve("Notater"));
        for (String file : List.of("andromeda.pdf", "orion.svg",
                "perseus.png")) {
            Files.writeString(folder.resolve(file), "");
        }
        return folder;
    }

    private static JFileChooser chooser(Path folder, FileSystemView view)
            throws Exception {
        JFileChooser[] made = new JFileChooser[1];
        SwingUtilities.invokeAndWait(() -> {
            // The application's own look and feel, as the photographer
            // installs it: the platform's default on macOS keeps no
            // directory model a photograph could wait for.
            juranometria.app.UiTheme.apply(false);
            made[0] = new JFileChooser(folder.toFile(), view);
        });
        return made[0];
    }

    private static void remove(Path folder) throws Exception {
        try (var walk = Files.walk(folder)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(one -> one.toFile().delete());
        }
    }

    @Test
    void aListingThatNeverCompletesIsRefusedWithinItsBound()
            throws Exception {
        juranometria.app.SwingSession.restoring(() -> {
            Path folder = folder();
            HeldListing held = new HeldListing();
            try {
                JFileChooser chooser = chooser(folder, held);
                IllegalStateException refused = assertThrows(
                        IllegalStateException.class,
                        () -> SwingChromeSheetMain.awaitListing(chooser,
                                ENTRIES, Duration.ofMillis(300)),
                        "a chooser that has not listed its folder is not"
                                + " photographed");
                assertTrue(refused.getMessage().contains("did not finish")
                                && refused.getMessage().contains("orion.svg"),
                        "and the refusal says what it was to show: "
                                + refused.getMessage());
            } finally {
                held.release.countDown();
                remove(folder);
            }
        });
    }

    @Test
    void aListingThatCompletesAfterTheChooserOpensIsWaitedFor()
            throws Exception {
        juranometria.app.SwingSession.restoring(() -> {
            Path folder = folder();
            HeldListing held = new HeldListing();
            try {
                JFileChooser chooser = chooser(folder, held);
                // The chooser is open and has listed nothing; only now may
                // its listing complete.
                held.release.countDown();
                SwingChromeSheetMain.awaitListing(chooser, ENTRIES,
                        Duration.ofSeconds(30));
                String[] listed = new String[1];
                SwingUtilities.invokeAndWait(() -> {
                    var model = ((javax.swing.plaf.basic.BasicFileChooserUI)
                            chooser.getUI()).getModel();
                    java.util.Set<String> names = new java.util.TreeSet<>();
                    for (int i = 0; i < model.getSize(); i++) {
                        names.add(((File) model.getElementAt(i)).getName());
                    }
                    listed[0] = names.toString();
                });
                assertEquals(new java.util.TreeSet<>(ENTRIES).toString(),
                        listed[0],
                        "when the wait ends, the chooser lists everything"
                                + " the photograph is of");
            } finally {
                held.release.countDown();
                remove(folder);
            }
        });
    }

    @Test
    void aListingWithoutAnExpectedEntryIsRefused() throws Exception {
        juranometria.app.SwingSession.restoring(() -> {
            Path folder = folder();
            try {
                JFileChooser chooser = chooser(folder,
                        FileSystemView.getFileSystemView());
                List<String> more = new java.util.ArrayList<>(ENTRIES);
                more.add("sagittarius.png");
                IllegalStateException refused = assertThrows(
                        IllegalStateException.class,
                        () -> SwingChromeSheetMain.awaitListing(chooser, more,
                                Duration.ofMillis(500)),
                        "a listing that is not exactly what the photograph"
                                + " is of does not count as complete");
                assertTrue(refused.getMessage().contains("sagittarius.png"),
                        "and the refusal names what is missing: "
                                + refused.getMessage());
            } finally {
                remove(folder);
            }
        });
    }
}
