package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;

import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.SymbolFamily;
import juranometria.ui.SymbolChip;

/**
 * The Chart Options dialog (issue #105, retabbed in Sprint 21 for
 * issue #185): pure wiring onto the production
 * {@link ChartOptionsController}, exactly the interaction model of
 * docs/decisions/chart-options.md. Every change previews live on the
 * chart; OK confirms and persists; Cancel, the window close button,
 * and Escape revert to the options captured when the dialog opened
 * and persist nothing; Restore Defaults is an ordinary previewed
 * transition back to the released chart.
 *
 * <p><strong>Four tabs</strong>, by subject: Deep sky, Stars,
 * Constellations, Chart. Eleven checkboxes in one column was already
 * long; the five deep-sky families would have made it sixteen, which
 * is a list rather than a dialog
 * (docs/decisions/deep-sky-vocabulary.md).
 *
 * <p>The Deep sky tab is <strong>legend and control at once</strong>:
 * each family carries the symbol the chart actually draws, its name,
 * and a sentence saying what it is. The dependencies appear as
 * enablement - deep-sky labels and the five families are effective
 * only while deep-sky objects are on, each remembering its state
 * while disabled - and constellation names only while figures are on.
 *
 * <p>Modeless, owned and centred on the atlas window so the chart
 * stays visible while choosing, and single-instance: opening again
 * brings the existing dialog forward instead of multiplying stale
 * copies.
 */
public final class ChartOptionsDialog extends JDialog {

    /** The one live instance; guarded on the EDT. */
    private static ChartOptionsDialog current;

    /** What the dialog packs to, and its floor at a narrow screen. */
    public static final int ORDINARY_WIDTH = 420;
    public static final int MINIMUM_WIDTH = 320;

    /**
     * Room left for the title bar and a margin, and the shortest the
     * dialog is ever made. See {@link #ceilingForUsableHeight}.
     */
    public static final int WINDOW_CHROME = 60;
    public static final int MINIMUM_CEILING = 320;

    private ChartOptionsDialog(Frame owner, ChartOptionsController controller) {
        super(owner, "Chart Options", false);
        getAccessibleContext().setAccessibleName("Chart Options");
        getAccessibleContext().setAccessibleDescription(
                "Choose which chart content and labels draw");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        ChartOptions snapshot = controller.options();
        Runnable cancel = () -> {
            controller.revertTo(snapshot);
            dispose();
        };
        setContentPane(content(controller, cancel, () -> {
            controller.confirm();
            dispose();
        }));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancel.run();
            }
        });
        getRootPane().registerKeyboardAction(event -> cancel.run(),
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        sizeToScreen(this, ORDINARY_WIDTH);
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog, or brings the existing one forward. */
    public static void open(Frame owner, ChartOptionsController controller) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new ChartOptionsDialog(owner, controller);
        current.setVisible(true);
    }

    /**
     * The dialog's content, for the study that photographs it
     * (docs/studies/deep-sky-vocabulary/). The study reviews the
     * surface a reader gets, so it builds the surface a reader gets;
     * its buttons do nothing, because a picture is not a session.
     */
    public static JComponent contentForStudy(
            ChartOptionsController controller) {
        return content(controller, () -> { }, () -> { });
    }

    /**
     * The dialog content; headless-constructible for tests and for
     * the study that reviews it. Controls reflect the controller's
     * current options, every change previews live through
     * {@code controller.apply}, and the dependency enablement follows
     * the decided rules.
     */
    static JComponent content(ChartOptionsController controller,
                              Runnable cancel, Runnable confirm) {
        ChartOptions initial = controller.options();

        JCheckBox dsos = checkBox("Deep-sky objects", 'D',
                initial.deepSkyObjects(), "Deep-sky objects",
                "Draw deep-sky objects on the chart at all",
                ChartKeys.DEEP_SKY);
        JCheckBox labels = checkBox("Deep-sky labels", 'l',
                initial.deepSkyLabels(), "Deep-sky labels",
                "Name the deep-sky objects the chart draws",
                "chart.deepSkyLabels");
        List<JCheckBox> families = new ArrayList<>();
        for (SymbolFamily family : SymbolFamily.values()) {
            families.add(checkBox(family.label(), family.mnemonic(),
                    initial.family(family), family.label(),
                    family.prose(), familyKey(family)));
        }

        JCheckBox figures = checkBox("Constellation figures", 'f',
                initial.constellationFigures(), "Constellation figures",
                "The joined stick figures of the constellations",
                ChartKeys.FIGURES);
        JCheckBox boundaries = checkBox("Constellation boundaries", 'b',
                initial.constellationBoundaries(),
                "Constellation boundaries",
                "The IAU boundaries, precessed from B1875",
                "chart.constellationBoundaries");
        JCheckBox names = checkBox("Constellation names", 'n',
                initial.constellationNames(), "Constellation names",
                "The figure's name, drawn where the figure is",
                "chart.constellationNames");
        JCheckBox starNames = checkBox("Star names", 'S',
                initial.starNames(), "Star names",
                "Traditional proper names such as Betelgeuse",
                "chart.starNames");
        JCheckBox bayerLetters = checkBox("Bayer letters", 'y',
                initial.bayerLetters(), "Bayer letters",
                "Greek and Latin Bayer designations such as alpha Orionis",
                "chart.bayerLetters");
        JCheckBox flamsteedNumbers = checkBox("Flamsteed numbers", 'F',
                initial.flamsteedNumbers(), "Flamsteed numbers",
                "Flamsteed catalogue numbers on the regional charts",
                "chart.flamsteedNumbers");
        JCheckBox grid = checkBox("Equatorial coordinate grid", 'E',
                initial.equatorialGrid(), "Equatorial coordinate grid",
                "ICRS/J2000 right-ascension and declination grid lines"
                        + " with coordinate labels",
                "chart.equatorialGrid");
        JCheckBox titleBlock = checkBox("Title block", 'T',
                initial.titleBlock(), "Title block",
                "The panel in the lower left stating the target, centre,"
                        + " frame, field width, limiting magnitude and"
                        + " orientation",
                "chart.titleBlock");
        JCheckBox magnitudeKey = checkBox("Stellar-magnitude key", 'k',
                initial.magnitudeKey(), "Stellar-magnitude key",
                "A key in the upper right showing the circle size the"
                        + " chart draws for three visual magnitudes,"
                        + " including this page's limit",
                "chart.magnitudeKey");
        JCheckBox blackSky = checkBox("Black sky", 'B',
                initial.palette() == ChartPalette.BLACK_SKY, "Black sky",
                "White stars and restrained light ink on a black"
                        + " ground, instead of the white-paper chart;"
                        + " a chart choice, independent of the"
                        + " application's light or dark appearance",
                "chart.blackSky");

        Runnable sync = () -> {
            // The two decided dependencies, and the five families,
            // which the master governs while they remember.
            labels.setEnabled(dsos.isSelected());
            names.setEnabled(figures.isSelected());
            for (JCheckBox family : families) {
                family.setEnabled(dsos.isSelected());
            }
            ChartOptions next = new ChartOptions(dsos.isSelected(),
                    labels.isSelected(), figures.isSelected(),
                    boundaries.isSelected(), names.isSelected(),
                    starNames.isSelected(), bayerLetters.isSelected(),
                    flamsteedNumbers.isSelected(), grid.isSelected(),
                    titleBlock.isSelected(), magnitudeKey.isSelected(),
                    families.get(0).isSelected(),
                    families.get(1).isSelected(),
                    families.get(2).isSelected(),
                    families.get(3).isSelected(),
                    families.get(4).isSelected(),
                    blackSky.isSelected() ? ChartPalette.BLACK_SKY
                            : ChartPalette.WHITE_PAPER);
            controller.apply(next);
        };
        labels.setEnabled(initial.deepSkyObjects());
        names.setEnabled(initial.constellationFigures());
        for (JCheckBox family : families) {
            family.setEnabled(initial.deepSkyObjects());
        }
        List<JCheckBox> all = new ArrayList<>(List.of(dsos, labels, figures,
                boundaries, names, starNames, bayerLetters,
                flamsteedNumbers, grid, titleBlock, magnitudeKey,
                blackSky));
        all.addAll(families);
        for (JCheckBox box : all) {
            box.addActionListener(event -> sync.run());
        }

        JTabbedPane tabs = new JTabbedPane();
        // One row of tabs, always. The default wrapping layout moves
        // the selected tab's row next to the content, which at the
        // narrow width really did put Deep sky below Constellations -
        // a dialog that rearranges itself under the reader.
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.getAccessibleContext().setAccessibleName("Chart options");
        // The four titles are the whole meaning; a tooltip repeating
        // a tab's own word is a box in the way of reading it.
        juranometria.ui.Explain.selfExplanatory(tabs,
                "Four groups of switches: deep sky, stars,"
                        + " constellations, and the chart's own"
                        + " furniture");
        tabs.addTab("Deep sky", scrolling(deepSkyTab(dsos, families, labels)));
        tabs.addTab("Stars", scrolling(column(starNames, bayerLetters,
                flamsteedNumbers)));
        tabs.addTab("Constellations", scrolling(column(figures, boundaries,
                names)));
        tabs.addTab("Chart", scrolling(column(grid, titleBlock,
                magnitudeKey, blackSky)));
        // After the tabs, not before: the strip builds its overflow
        // controls when it has tabs to overflow, so naming them first
        // named nothing at all and left the one control on this
        // window a reader cannot guess unexplained (#311 audit).
        nameTabStripControls(tabs);

        JButton restore = new JButton("Restore Defaults");
        restore.setMnemonic('R');
        restore.getAccessibleContext().setAccessibleName("Restore Defaults");
        juranometria.ui.Explain.control(restore,
                "Preview the released chart: every layer and every"
                        + " deep-sky family on, the title block on,"
                        + " the magnitude key off, on white paper",
                "Puts every switch in this window back to what the"
                        + " atlas ships with. It is a preview like any"
                        + " other here: OK keeps it, Cancel leaves"
                        + " your own choices alone.");
        restore.addActionListener(event -> {
            controller.restoreDefaults();
            ChartOptions defaults = controller.options();
            dsos.setSelected(defaults.deepSkyObjects());
            labels.setSelected(defaults.deepSkyLabels());
            figures.setSelected(defaults.constellationFigures());
            boundaries.setSelected(defaults.constellationBoundaries());
            titleBlock.setSelected(defaults.titleBlock());
            magnitudeKey.setSelected(defaults.magnitudeKey());
            names.setSelected(defaults.constellationNames());
            starNames.setSelected(defaults.starNames());
            bayerLetters.setSelected(defaults.bayerLetters());
            flamsteedNumbers.setSelected(defaults.flamsteedNumbers());
            grid.setSelected(defaults.equatorialGrid());
            blackSky.setSelected(
                    defaults.palette() == ChartPalette.BLACK_SKY);
            for (int i = 0; i < families.size(); i++) {
                families.get(i).setSelected(
                        defaults.family(SymbolFamily.values()[i]));
                families.get(i).setEnabled(true);
            }
            labels.setEnabled(true);
            names.setEnabled(true);
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.getAccessibleContext().setAccessibleName("Cancel");
        juranometria.ui.Explain.selfExplanatory(cancelButton,
                "Puts the chart back the way it was and closes this"
                        + " window");
        cancelButton.addActionListener(event -> cancel.run());
        JButton ok = new JButton("OK");
        ok.getAccessibleContext().setAccessibleName("OK");
        juranometria.ui.Explain.selfExplanatory(ok,
                "Keeps what the chart is showing now, and remembers it"
                        + " for next time");
        ok.addActionListener(event -> confirm.run());

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        buttons.add(restore, BorderLayout.WEST);
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(cancelButton);
        right.add(Box.createHorizontalStrut(8));
        right.add(ok);
        buttons.add(right, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        panel.add(tabs, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Names the controls the scrolling tab layout adds for itself.
     *
     * <p>When the four titles do not fit - a narrow dialog, a larger
     * font, a platform whose text is wider - the layout adds arrow
     * buttons and a hidden-tabs button of its own, and they arrive
     * with no accessible name at all. They are controls a reader can
     * operate, so they must say what they do; and they come and go as
     * the strip is resized, so the naming follows them rather than
     * happening once.
     *
     * <p>Found by the accessibility surface test on Linux, where the
     * titles need scrolling at a width that fits them on macOS.
     */
    private static void nameTabStripControls(JTabbedPane tabs) {
        for (Component child : tabs.getComponents()) {
            nameTabStripControl(child);
        }
        tabs.addContainerListener(new java.awt.event.ContainerAdapter() {
            @Override
            public void componentAdded(java.awt.event.ContainerEvent event) {
                nameTabStripControl(event.getChild());
            }
        });
    }

    private static void nameTabStripControl(Component child) {
        if (!(child instanceof javax.swing.AbstractButton button)) {
            return;
        }
        String name;
        if (child instanceof javax.swing.plaf.basic.BasicArrowButton arrow) {
            int direction = arrow.getDirection();
            name = direction == javax.swing.SwingConstants.WEST
                    || direction == javax.swing.SwingConstants.NORTH
                    ? "Show earlier tabs" : "Show later tabs";
        } else {
            name = "Show hidden tabs";
        }
        button.getAccessibleContext().setAccessibleName(name);
        // Whatever the look and feel put there. Left alone it set a
        // tooltip and no description, and Swing then reads the
        // tooltip back as the description - the same words twice, to
        // the one reader who cannot see the first of them (#311).
        juranometria.ui.Explain.control(button, name,
                "The tab titles do not all fit in the window; this"
                        + " brings the rest into view");
    }

    /** The Deep sky tab: master, five families as legend and control. */
    private static JComponent deepSkyTab(JCheckBox master,
                                         List<JCheckBox> families,
                                         JCheckBox labels) {
        JPanel panel = column();
        panel.add(master);
        panel.add(Box.createVerticalStrut(8));
        for (int i = 0; i < families.size(); i++) {
            panel.add(familyRow(SymbolFamily.values()[i], families.get(i)));
        }
        panel.add(Box.createVerticalStrut(8));
        panel.add(labels);
        return panel;
    }

    /**
     * One family: its checkbox, the chart's own symbol, and a
     * sentence saying what the family is. The sentence is visible
     * text and the checkbox's accessible description both, so a
     * reader who never hovers loses nothing.
     */
    private static JComponent familyRow(SymbolFamily family,
                                        JCheckBox check) {
        JPanel row = column();
        row.setBorder(BorderFactory.createEmptyBorder(2, 16, 6, 0));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.X_AXIS));
        head.setAlignmentX(0.0f);
        head.add(check);
        head.add(Box.createHorizontalStrut(6));
        head.add(new SymbolChip(family));
        head.add(Box.createHorizontalGlue());
        row.add(head);

        JLabel explains = new JLabel(family.prose());
        explains.putClientProperty(WRAPPED_TEXT, family.prose());
        explains.setAlignmentX(0.0f);
        explains.setBorder(BorderFactory.createEmptyBorder(1, 22, 0, 0));
        explains.putClientProperty("FlatLaf.styleClass", "small");
        // One thing to a screen reader: the checkbox carries the whole
        // meaning, so the visible sentence is not read out twice.
        explains.getAccessibleContext().setAccessibleName("");
        row.add(explains);
        return row;
    }

    /**
     * One switch, explained twice over.
     *
     * <p>The hover form is what the switch draws, with the keys that
     * reach it from the chart itself - quoted from the registry that
     * binds them (#312), never typed here, so the dialog and the
     * chart keyboard cannot drift apart.
     *
     * <p>The spoken form says the same thing and then the two things
     * a reader who cannot see the dialog has no other way of
     * learning: that there is a second route to this switch, and
     * which master it waits for. The greying that tells a sighted
     * reader is not a sentence.
     */
    private static JCheckBox checkBox(String text, char mnemonic,
                                      boolean selected,
                                      String accessibleName,
                                      String description, String key) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setMnemonic(mnemonic);
        box.setOpaque(false);
        box.setAlignmentX(0.0f);
        box.getAccessibleContext().setAccessibleName(accessibleName);
        ChartKeys.Toggle toggle = ChartKeys.toggle(key);
        if (toggle == null) {
            throw new IllegalArgumentException(
                    "no switch on the chart keyboard is called " + key
                            + ", so a tooltip must not promise one");
        }
        StringBuilder spoken = new StringBuilder(description)
                .append(". Switched here, or from the chart by"
                        + " pressing ")
                .append(toggle.sequence()).append('.');
        if (toggle.dependsOn() != null) {
            spoken.append(" Needs ")
                    .append(ChartKeys.toggle(toggle.dependsOn()).label()
                            .toLowerCase(java.util.Locale.ROOT))
                    .append(" on.");
        }
        return juranometria.ui.Explain.control(box,
                description + " (" + toggle.sequence() + ")",
                spoken.toString());
    }

    /** The registry's name for a symbol family's switch. */
    private static String familyKey(SymbolFamily family) {
        return switch (family) {
            case GALAXIES -> "chart.galaxies";
            case OPEN_CLUSTERS -> "chart.openClusters";
            case GLOBULAR_CLUSTERS -> "chart.globularClusters";
            case NEBULAE -> "chart.nebulae";
            case PLANETARY_NEBULAE -> "chart.planetaryNebulae";
        };
    }

    private static JPanel column(JComponent... rows) {
        JPanel panel = new ScrollableColumn();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.setAlignmentX(0.0f);
        for (JComponent row : rows) {
            panel.add(row);
        }
        return panel;
    }

    /**
     * Every tab scrolls rather than clips. Enlarged text and a narrow
     * dialog both make a tab taller than the space it has, and a
     * control below the fold must still be reachable - by keyboard as
     * much as by pointer, which scrolling gives and clipping does not.
     */
    private static JScrollPane scrolling(JComponent view) {
        JScrollPane scroll = new JScrollPane(view,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    /**
     * A column that never grows wider than the space it is given. A
     * plain panel inside a scroll pane keeps its preferred width, so
     * a long line pushes the column past the viewport and, with no
     * horizontal scroll bar, quietly cuts the right-hand edge off
     * every row.
     */
    private static final class ScrollableColumn extends JPanel
            implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible,
                                              int orientation,
                                              int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible,
                                               int orientation,
                                               int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ---- sizing ----------------------------------------------------
    //
    // The dialog is as tall as its tallest tab, so moving between tabs
    // never resizes the window under the reader - and never taller
    // than the reader's own screen allows, beyond which the tab
    // scrolls inside it.

    /** Where a wrapped label keeps the words it is wrapping. */
    private static final String WRAPPED_TEXT = "juranometria.wrappedText";

    /**
     * Sizes a window around this content, for the screen it is on.
     *
     * <p>The window is asked for exactly one size, and that size is
     * measured first, by laying the content out. Packing a window to
     * measure it asks the desktop for a size, and at enlarged text
     * that size is taller than the screen: the refusal comes back as
     * a resize delivered later, after the cap has been applied.
     */
    public static void sizeToScreen(Window window, int width) {
        sizeToScreen(window, width,
                usableHeight(window.getGraphicsConfiguration()));
    }

    /**
     * The same, against a stated usable height - which is how a short
     * screen is reviewed and regression-tested from a tall one.
     */
    public static void sizeToScreen(Window window, int width,
                                    int usableHeight) {
        window.addNotify();
        Insets chrome = window.getInsets();
        JComponent content = (JComponent)
                ((javax.swing.RootPaneContainer) window).getContentPane();
        int inner = width - chrome.left - chrome.right;
        int ceiling = ceilingForUsableHeight(usableHeight);
        window.setSize(width, Math.min(ceiling,
                tallestTab(content, inner, ceiling)
                        + chrome.top + chrome.bottom));
        window.validate();
    }

    /**
     * The content height that holds the tallest tab, laid out at a
     * given inner width. Wrapped explanations are re-wrapped twice:
     * the first pass can bring a scroll bar into being and take its
     * width away from the very text that summoned it.
     */
    public static int tallestTab(JComponent content, int innerWidth,
                                 int ceiling) {
        JTabbedPane tabs = tabsOf(content);
        layOut(content, innerWidth, ceiling);
        rewrap(content);
        layOut(content, innerWidth, ceiling);
        rewrap(content);
        int tallest = 0;
        int selected = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setSelectedIndex(i);
            layOut(content, innerWidth, ceiling);
            tallest = Math.max(tallest, content.getPreferredSize().height);
        }
        tabs.setSelectedIndex(Math.max(0, selected));
        layOut(content, innerWidth, ceiling);
        return tallest;
    }

    private static void layOut(JComponent content, int width, int height) {
        content.setSize(width, height);
        content.doLayout();
        content.validate();
    }

    /**
     * How tall this dialog may be on the screen it opens on.
     *
     * <p>Not a constant. A dialog capped at a number chosen on a tall
     * display puts its own OK button under the taskbar of a short
     * one, and a reader cannot resize what they cannot reach.
     */
    public static int ceilingForUsableHeight(int usableHeight) {
        return Math.max(MINIMUM_CEILING, usableHeight - WINDOW_CHROME);
    }

    /** The screen area left over once the desktop has taken its share. */
    public static int usableHeight(GraphicsConfiguration screen) {
        if (screen == null) {
            return MINIMUM_CEILING + WINDOW_CHROME;
        }
        Rectangle bounds = screen.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        return bounds.height - insets.top - insets.bottom;
    }

    /** The tabbed pane inside a content pane this class built. */
    public static JTabbedPane tabsOf(Container content) {
        for (Component child : content.getComponents()) {
            if (child instanceof JTabbedPane tabs) {
                return tabs;
            }
        }
        throw new IllegalStateException("no tabbed pane in " + content);
    }

    /**
     * Re-wraps every wrapped label to the width the laid-out dialog
     * actually gave it, against the font that will draw it.
     *
     * <p>A CSS width on an HTML body does not bound a label's
     * preferred width - measured at 425 px for a body declared 310 px
     * wide - and Swing then paints the text past the label's own
     * edge, where no bounds check finds it. Breaking the lines
     * against real font metrics is the only way the words are known
     * to fit.
     */
    public static void rewrap(JComponent root) {
        for (Component control : wrappedLabels(root)) {
            JLabel label = (JLabel) control;
            Object prose = label.getClientProperty(WRAPPED_TEXT);
            Insets insets = label.getInsets();
            int available = label.getWidth() - insets.left - insets.right;
            if (available > 40) {
                label.setText(wrapped((String) prose, available,
                        label.getFontMetrics(label.getFont())));
            }
        }
        root.revalidate();
    }

    private static List<Component> wrappedLabels(Container container) {
        List<Component> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label
                    && label.getClientProperty(WRAPPED_TEXT) != null) {
                found.add(child);
            }
            if (child instanceof Container inner) {
                found.addAll(wrappedLabels(inner));
            }
        }
        return found;
    }

    private static String wrapped(String prose, int widthPx,
                                  java.awt.FontMetrics metrics) {
        StringBuilder html = new StringBuilder("<html>");
        StringBuilder line = new StringBuilder();
        for (String word : prose.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty()
                    && metrics.stringWidth(candidate) > widthPx) {
                html.append(escaped(line.toString())).append("<br>");
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        return html.append(escaped(line.toString())).append("</html>")
                .toString();
    }

    private static String escaped(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
