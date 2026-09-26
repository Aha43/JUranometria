package juranometria.tool;

import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A generator that always refuses, for proving what a refusal keeps.
 *
 * <p>It writes one file, then photographs a packed dialog whose
 * window is resized after the pack established it - the #376
 * transition, forced. The capture coordinator refuses to prove that
 * geometry, and the exception ends the process with a non-zero exit,
 * exactly as a real generator's refusal does. Test code only: it is
 * registered nowhere and writes into whatever directory it is given.
 */
public final class RefusingCaptureProbeMain {

    /** What it writes before refusing. */
    static final String WROTE = "written-before-refusing.md";

    private RefusingCaptureProbeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path into = Path.of(args[0]);
        Files.writeString(into.resolve(WROTE),
                "written before the capture refused\n",
                StandardCharsets.UTF_8);
        JDialog[] dialog = new JDialog[1];
        JPanel[] content = new JPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            dialog[0] = new JDialog(new JFrame("owner"));
            content[0] = new JPanel();
            content[0].setPreferredSize(new Dimension(333, 223));
            dialog[0].setContentPane(content[0]);
        });
        SheetCapture.tracing("refusing-probe");
        SheetCapture.Sizing packedThenResized = (window, held) -> {
            SheetCapture.Established packed =
                    SheetCapture.packed().establish(window, held);
            SwingUtilities.invokeLater(() -> window.setSize(
                    window.getWidth() - 7, window.getHeight() - 17));
            return packed;
        };
        try {
            SheetCapture.take(dialog[0], content[0], packedThenResized,
                    SheetCapture.Premise.none(), () -> {
                        throw new IllegalStateException("the probe was"
                                + " meant to be refused before"
                                + " drawing");
                    });
        } catch (Exception refused) {
            // Printed and ended explicitly: an exception alone does
            // not end a process whose event thread is still alive.
            refused.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
