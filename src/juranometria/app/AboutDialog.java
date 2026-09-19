package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyEvent;

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

    public static final String SUMMARY_RESOURCE = "/resources/about/licensing-summary.txt";

    /**
     * One bundled document: what it is, and where it ships.
     *
     * <p><strong>No heading.</strong> This was a
     * {@code String[][]} pairing an English title with a resource
     * path - half prose, half identity, in one array - and the
     * heading is gone rather than kept beside the path, so nothing
     * can read an English title off the registry. The words live in
     * {@link juranometria.ui.language.AboutText}, keyed by the id
     * below; tests and diagnostics use the id.
     *
     * @param id canonical, stable, never shown to a reader
     * @param resourcePath the packaged document, shown byte for byte
     *     in every language
     */
    public record Notice(String id, String resourcePath) {

        public Notice {
            if (id == null || id.isBlank()
                    || resourcePath == null || resourcePath.isBlank()) {
                throw new IllegalArgumentException(
                        "a bundled document has a name of its own and"
                                + " somewhere it ships from, and blank"
                                + " is neither");
            }
        }
    }

    /** The bundled notices shown by the fuller view, in display order. */
    public static final java.util.List<Notice> NOTICES = java.util.List.of(
            new Notice("tycho2",
                    "/resources/catalog/bright-sky/NOTICE-tycho2.md"),
            new Notice("openngc",
                    "/resources/catalog/bright-sky/NOTICE-openngc.md"),
            new Notice("ccbysa",
                    "/resources/catalog/bright-sky/LICENSE-CC-BY-SA-4.0.txt"),
            new Notice("constellations",
                    "/resources/geo/constellations/NOTICE-constellations.md"),
            new Notice("staridentities",
                    "/resources/catalog/star-identities/NOTICE-star-identities.md"),
            new Notice("bsd3",
                    "/resources/geo/constellations/LICENSE-BSD-3-Clause.txt"),
            new Notice("tabler", "/resources/icons/LICENSE"));

    private final juranometria.ui.language.AboutText said;

    AboutDialog(Frame owner,
                juranometria.ui.language.InterfaceText words) {
        super(owner, juranometria.ui.language.AboutText.in(words)
                .title(AppInfo.NAME), false);
        this.said = juranometria.ui.language.AboutText.in(words);
        getAccessibleContext().setAccessibleName(said.title(AppInfo.NAME));
        getAccessibleContext().setAccessibleDescription(
                said.windowExplain());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(compactContent(this::showNotices, words));
        installEscapeToClose(this);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog owned by and centred on the atlas window. */
    public static void open(Frame owner,
                            juranometria.ui.language.InterfaceText words) {
        new AboutDialog(owner, words).setVisible(true);
    }

    private void showNotices() {
        setContentPane(noticesContent(said.words()));
        revalidate();
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** The compact first view; headless-constructible for tests. */
    /**
     * The two views' content, for the audit that reads what every
     * control says (#311).
     */
    public static JComponent compactContentForStudy(
            juranometria.ui.language.InterfaceText words) {
        return compactContent(() -> { }, words);
    }

    /** The notices view, for the same audit. */
    public static JComponent noticesContentForStudy(
            juranometria.ui.language.InterfaceText words) {
        return noticesContent(words);
    }

    static JComponent compactContent(Runnable showNotices,
            juranometria.ui.language.InterfaceText words) {
        juranometria.ui.language.AboutText said =
                juranometria.ui.language.AboutText.in(words);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // The mark beside the name, not in place of anything (issue
        // #202): the version, the licensing summary and the way to
        // the notices all keep their room. It is branding, so it is
        // decorative to assistive technology - a screen reader is
        // told the application's name, not the position of three
        // stars.
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.X_AXIS));
        heading.setAlignmentX(0.0f);
        JLabel mark = new JLabel(new javax.swing.ImageIcon(
                ApplicationIcon.at(48)));
        // Deliberately unnamed and undescribed. The application's
        // name is beside it, in words; a screen reader gains nothing
        // from being told where three stars sit.
        mark.setFocusable(false);
        mark.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        heading.add(mark);

        JLabel title = new JLabel(said.heading(AppInfo.NAME,
                AppInfo.version()));
        title.putClientProperty("FlatLaf.styleClass", "h2");
        title.setAlignmentX(0.0f);
        heading.add(title);
        heading.add(Box.createHorizontalGlue());
        panel.add(heading);
        panel.add(Box.createVerticalStrut(6));

        JLabel description = new JLabel(said.description());
        description.setAlignmentX(0.0f);
        panel.add(description);
        panel.add(Box.createVerticalStrut(12));

        JTextArea summary = readOnlyText(said.summary(), 14, 46);
        summary.getAccessibleContext().setAccessibleName(
                said.summaryName());
        // Read-only prose. Its own words are the whole of it, and a
        // tooltip over a page of text is a box in the way of reading.
        juranometria.ui.Explain.selfExplanatory(summary,
                said.summaryExplain());
        JScrollPane summaryScroll = new JScrollPane(summary);
        summaryScroll.setAlignmentX(0.0f);
        panel.add(summaryScroll);
        panel.add(Box.createVerticalStrut(12));

        JButton notices = new JButton(said.noticesButton());
        notices.getAccessibleContext().setAccessibleName(
                said.noticesButtonName());
        juranometria.ui.Explain.selfExplanatory(notices,
                said.noticesButtonExplain());
        notices.addActionListener(event -> showNotices.run());
        JButton close = closeButton(said);
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
    static JComponent noticesContent(
            juranometria.ui.language.InterfaceText words) {
        juranometria.ui.language.AboutText said =
                juranometria.ui.language.AboutText.in(words);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextArea text = readOnlyText(noticesText(words), 24, 66);
        text.getAccessibleContext().setAccessibleName(
                said.noticesName());
        juranometria.ui.Explain.selfExplanatory(text,
                said.noticesExplain());
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(560, 420));
        panel.add(scroll, BorderLayout.CENTER);
        JButton close = closeButton(said);
        JPanel south = new JPanel(new BorderLayout());
        south.add(close, BorderLayout.EAST);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * The canonical English compact summary, verbatim.
     *
     * <p>The document the licensing-map contract is written on, and
     * what any language falls back to whole.
     */
    public static String summaryText() {
        return juranometria.ui.language.AboutText.canonicalSummary();
    }

    /**
     * Every bundled notice and licence, ready to show.
     *
     * <p>Translated heading, immutable body, and a rule between them.
     * The rules and the blank lines are <strong>rendering</strong> -
     * they are the same characters in every language - and the bodies
     * are passed through untouched: nothing here reflows, trims or
     * re-encodes 25 235 bytes of somebody else's licence text.
     */
    public static String noticesText(
            juranometria.ui.language.InterfaceText words) {
        juranometria.ui.language.AboutText said =
                juranometria.ui.language.AboutText.in(words);
        StringBuilder out = new StringBuilder();
        for (Notice notice : NOTICES) {
            out.append(RULE)
                    .append(said.heading(notice)).append('\n')
                    .append(RULE).append('\n')
                    .append(juranometria.ui.language.AboutText
                            .noticesBody(notice))
                    .append("\n\n");
        }
        return out.toString();
    }

    /** Rendering, not language: the same characters everywhere. */
    private static final String RULE =
            "================================================\n";

    /** The way out, identical on both views and built once. */
    private static JButton closeButton(
            juranometria.ui.language.AboutText said) {
        JButton close = new JButton(said.closeButton());
        close.getAccessibleContext().setAccessibleName(
                said.closeButton());
        juranometria.ui.Explain.selfExplanatory(close,
                said.closeExplain());
        close.addActionListener(event -> {
            java.awt.Window window =
                    javax.swing.SwingUtilities.getWindowAncestor(close);
            if (window != null) {
                window.dispose();
            }
        });
        return close;
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
