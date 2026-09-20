package juranometria.app;

import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicFileChooserUI;

/**
 * What a reader's new folder is called, in a JVM of its own
 * (Sprint 33, issue #350).
 *
 * <p>One of the forty-one keys cannot be read off a drawn dialog.
 * {@code FileChooser.other.newFolder} names the directory the chooser
 * creates when a reader presses its new-folder button, so nothing
 * shows it until the action runs and something is created on disk.
 *
 * <p>It also behaves unlike the other forty. The JDK copies it into a
 * {@code static final} field when {@code FileSystemView} initialises,
 * which happens the first time anything in the JVM asks for one -
 * building a {@code JFileChooser} is enough. Whatever the defaults
 * table said at that instant is what every folder in that JVM is
 * named, and installing the atlas's words afterwards changes nothing.
 * So this is not a question about a value; it is a question about
 * <em>order</em>, and order can only be asked in a fresh process.
 *
 * <p>Hence a probe with a {@code main}. It runs the real
 * {@code JUranometriaMain.start} through the startup journey's own
 * harness - the same composition a reader launches - and only then
 * builds a chooser and invokes its action, which is exactly what the
 * export does. It prints one line for the caller to read.
 *
 * <p>Two modes, because one of them has to be able to fail. In
 * {@code latched} a chooser is built <em>before</em> startup, freezing
 * the JDK's own name, and the probe is expected to report something
 * other than the atlas's word. That is the mutation: if both modes
 * agreed, this would be proving nothing about order.
 */
public final class NewFolderProbeMain {

    private NewFolderProbeMain() {
    }

    /**
     * @param args the mode - {@code application} or {@code latched} -
     *     and an existing empty directory to create the folder in
     */
    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path into = Path.of(args[1]);

        if (mode.equals("latched")) {
            // The hazard, deliberately: this initialises
            // FileSystemView and freezes the JDK's own name before
            // the application has said a word.
            new JFileChooser();
        }

        String[] made = new String[1];
        StartupJourneyTest.running("nb-NO", true, app ->
                SwingUtilities.invokeAndWait(() -> {
                    JFileChooser chooser =
                            new JFileChooser(into.toFile());
                    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                    javax.swing.JDialog holder = new javax.swing.JDialog();
                    try {
                        holder.setContentPane(chooser);
                        holder.pack();
                        if (!(chooser.getUI()
                                instanceof BasicFileChooserUI ui)) {
                            throw new IllegalStateException("the premise:"
                                    + " this look and feel offers a"
                                    + " new-folder action");
                        }
                        ui.getNewFolderAction().actionPerformed(
                                new java.awt.event.ActionEvent(chooser, 0,
                                        "New Folder"));
                    } finally {
                        holder.dispose();
                    }
                }));

        try (var files = java.nio.file.Files.list(into)) {
            made[0] = files.map(one -> one.getFileName().toString())
                    .findFirst().orElse("<nothing was created>");
        }
        System.out.println("NEW-FOLDER-NAME=" + made[0]);
        System.exit(0);
    }
}
