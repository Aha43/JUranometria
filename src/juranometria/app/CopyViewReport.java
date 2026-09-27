package juranometria.app;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Help &gt; Copy View Report (#372): the current view, as plain text,
 * on the clipboard - and nothing else.
 *
 * <p>The report is formatted from a snapshot the application reads
 * from its live owners at the moment the reader asks. It is not sent,
 * saved or remembered, and nothing about the view changes. Where the
 * clipboard refuses, the reader is told; success claims nothing it
 * did not do.
 */
public final class CopyViewReport {

    /** Where a report is copied: the system clipboard, or a test's. */
    public interface Clipboard {

        /**
         * Places the text on the clipboard.
         *
         * @throws IllegalStateException when the clipboard cannot take
         *     it right now
         */
        void copy(String text);
    }

    /** The reader's own clipboard. */
    public static final Clipboard SYSTEM = text -> {
        java.awt.datatransfer.Clipboard system;
        try {
            system = java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard();
        } catch (java.awt.HeadlessException | SecurityException refused) {
            throw new IllegalStateException(
                    "no clipboard is available here", refused);
        }
        system.setContents(
                new java.awt.datatransfer.StringSelection(text), null);
    };

    private CopyViewReport() {
    }

    /**
     * The action: read the state, format it, copy it; tell
     * {@code refused} the reason if the clipboard would not take it.
     */
    public static Runnable action(Supplier<ViewReport.Snapshot> state,
                                  Clipboard clipboard,
                                  Consumer<String> refused) {
        if (state == null || clipboard == null || refused == null) {
            throw new IllegalArgumentException(
                    "a report needs its state, a clipboard, and a way to"
                            + " say it could not be copied");
        }
        return () -> {
            String report = ViewReport.format(state.get());
            try {
                clipboard.copy(report);
            } catch (IllegalStateException unavailable) {
                refused.accept(unavailable.getMessage() == null
                        ? unavailable.toString()
                        : unavailable.getMessage());
            }
        };
    }
}
