package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

/**
 * The About surface (issue #98): identifies the application and its
 * packaged version, describes it in the established language, and
 * states the licensing of the code and every bundled resource family
 * plainly - including the non-commercial consequence of the
 * Tycho-2-derived data - entirely offline.
 *
 * Source-of-truth rule: the compact summary is the packaged
 * {@code /resources/about/licensing-summary.txt} (tested to agree
 * with LICENSING.md), and the fuller view concatenates the notice and
 * licence files already shipped in the jar - no legal prose lives in
 * Java strings, and the version comes from {@link AppInfo}, never a
 * second hard-coded copy.
 *
 * The dialog is modeless, owned and centred on the atlas window,
 * closable by its button and by Escape, and touches no chart state.
 */
public final class AboutDialog extends JDialog {

    static final String SUMMARY_RESOURCE = "/resources/about/licensing-summary.txt";

    /** The bundled notices shown by the fuller view, in display order. */
    static final String[][] NOTICE_RESOURCES = {
            {"Tycho-2 star data", "/resources/catalog/bright-sky/NOTICE-tycho2.md"},
            {"OpenNGC deep-sky data", "/resources/catalog/bright-sky/NOTICE-openngc.md"},
            {"CC BY-SA 4.0 licence text", "/resources/catalog/bright-sky/LICENSE-CC-BY-SA-4.0.txt"},
            {"Constellation geography", "/resources/geo/constellations/NOTICE-constellations.md"},
            {"Star identities", "/resources/catalog/star-identities/NOTICE-star-identities.md"},
            {"BSD-3-Clause licence text", "/resources/geo/constellations/LICENSE-BSD-3-Clause.txt"},
            {"Tabler icons licence", "/resources/icons/LICENSE"},
    };

    static final String DESCRIPTION =
            "A quiet, interactive atlas for learning the geography of the sky.";

    AboutDialog(Frame owner) {
        super(owner, "About " + AppInfo.NAME, false);
        getAccessibleContext().setAccessibleName("About " + AppInfo.NAME);
        getAccessibleContext().setAccessibleDescription(
                "Application identity, version, and licensing information");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(compactContent(this::showNotices));
        installEscapeToClose(this);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog owned by and centred on the atlas window. */
    public static void open(Frame owner) {
        new AboutDialog(owner).setVisible(true);
    }

    private void showNotices() {
        setContentPane(noticesContent());
        revalidate();
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** The compact first view; headless-constructible for tests. */
    static JComponent compactContent(Runnable showNotices) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel(AppInfo.NAME + " " + AppInfo.version());
        title.putClientProperty("FlatLaf.styleClass", "h2");
        title.setAlignmentX(0.0f);
        panel.add(title);
        panel.add(Box.createVerticalStrut(6));

        JLabel description = new JLabel(DESCRIPTION);
        description.setAlignmentX(0.0f);
        panel.add(description);
        panel.add(Box.createVerticalStrut(12));

        JTextArea summary = readOnlyText(summaryText(), 14, 46);
        summary.getAccessibleContext().setAccessibleName("Licensing summary");
        JScrollPane summaryScroll = new JScrollPane(summary);
        summaryScroll.setAlignmentX(0.0f);
        panel.add(summaryScroll);
        panel.add(Box.createVerticalStrut(12));

        JButton notices = new JButton("Full notices and licences...");
        notices.getAccessibleContext().setAccessibleName(
                "Full notices and licences");
        notices.addActionListener(event -> showNotices.run());
        JButton close = new JButton("Close");
        close.getAccessibleContext().setAccessibleName("Close");
        close.addActionListener(event -> {
            java.awt.Window window =
                    javax.swing.SwingUtilities.getWindowAncestor(close);
            if (window != null) {
                window.dispose();
            }
        });
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setAlignmentX(0.0f);
        buttons.add(notices);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(close);
        panel.add(buttons);
        return panel;
    }

    /** The fuller notices view; headless-constructible for tests. */
    static JComponent noticesContent() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextArea text = readOnlyText(noticesText(), 24, 66);
        text.getAccessibleContext().setAccessibleName(
                "Bundled notices and licence texts");
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(560, 420));
        panel.add(scroll, BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.getAccessibleContext().setAccessibleName("Close");
        close.addActionListener(event -> {
            java.awt.Window window =
                    javax.swing.SwingUtilities.getWindowAncestor(close);
            if (window != null) {
                window.dispose();
            }
        });
        JPanel south = new JPanel(new BorderLayout());
        south.add(close, BorderLayout.EAST);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    /** The packaged compact licensing summary, verbatim. */
    static String summaryText() {
        return resourceText(SUMMARY_RESOURCE);
    }

    /** Every bundled notice and licence text, concatenated for display. */
    static String noticesText() {
        StringBuilder out = new StringBuilder();
        for (String[] notice : NOTICE_RESOURCES) {
            out.append("================================================\n")
                    .append(notice[0]).append('\n')
                    .append("================================================\n\n")
                    .append(resourceText(notice[1])).append("\n\n");
        }
        return out.toString();
    }

    private static String resourceText(String resource) {
        try (InputStream stream = AboutDialog.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "missing packaged notice resource: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JTextArea readOnlyText(String text, int rows, int columns) {
        JTextArea area = new JTextArea(text, rows, columns);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        return area;
    }

    /** Escape disposes the dialog, the conventional close gesture. */
    static void installEscapeToClose(JDialog dialog) {
        dialog.getRootPane().registerKeyboardAction(
                event -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
