package juranometria.tool;

import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A generator that always refuses, for proving what a refusal keeps.
 *
 * <p>It writes one file, then photographs a fixed canvas that cannot
 * be held at the size the study established: the component's own
 * validation lays it out at another size, every time. The capture
 * coordinator refuses to prove a geometry that was not established,
 * and the refusal ends the process with a non-zero exit, exactly as a
 * real generator's refusal does.
 *
 * <p>Pure Java, and deterministic by construction (#390). The first
 * version forced its refusal with a stale resize of a native window,
 * and the macOS peer sometimes put the packed size back - so the
 * capture was proved, the probe was not refused, and the contract
 * about failure evidence failed on whether the operating system kept
 * a resize. There is no window here, no queue to race and nothing to
 * wait for. Test code only: it is registered nowhere and writes into
 * whatever directory it is given.
 */
public final class RefusingCaptureProbeMain {

    /** What it writes before refusing. */
    static final String WROTE = "written-before-refusing.md";

    /** The size the study establishes. */
    static final Dimension ESTABLISHED = new Dimension(333, 223);

    /** The size the component's own validation insists on. */
    static final Dimension INSISTED = new Dimension(326, 206);

    /**
     * A canvas that lays itself out at {@link #INSISTED} whenever it is
     * validated, whatever size it was given.
     */
    private static final class Insists extends JPanel {

        @Override
        public void validate() {
            super.validate();
            setSize(INSISTED);
        }
    }

    private RefusingCaptureProbeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path into = Path.of(args[0]);
        Files.writeString(into.resolve(WROTE),
                "written before the capture refused\n",
                StandardCharsets.UTF_8);
        Insists[] canvas = new Insists[1];
        SwingUtilities.invokeAndWait(() -> canvas[0] = new Insists());
        SheetCapture.tracing("refusing-probe");
        try {
            SheetCapture.take(null, canvas[0],
                    SheetCapture.fixedCanvas(() ->
                            canvas[0].setSize(ESTABLISHED)),
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
