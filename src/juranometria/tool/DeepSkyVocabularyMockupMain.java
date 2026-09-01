package juranometria.tool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.app.UiTheme;
import juranometria.chart.DsoType;
import juranometria.render.ChartRenderer;

/**
 * The deep-sky vocabulary gate's dialog mock-ups (Sprint 21, issue
 * #184): the proposed tabbed Chart Options built from real Swing
 * controls under the real application themes, so the review judges
 * what the dialog would actually look like rather than a drawing of
 * it.
 *
 * <p>Every symbol is painted by {@link ChartRenderer#drawLegendSymbol}
 * - the chart's own geometry. Nothing here draws a legend shape of
 * its own, so the legend cannot drift from the page.
 *
 * <p>This is a mock-up. Production options and the production dialog
 * are untouched by this issue; #185 implements what the review
 * approves.
 */
public final class DeepSkyVocabularyMockupMain {

    private DeepSkyVocabularyMockupMain() {
    }

    static final File DIR = new File("docs/studies/deep-sky-vocabulary");

    /** The chart's paper, so a symbol chip shows the page's own ground. */
    private static final Color PAPER = Color.WHITE;

    /** Ordinary width: what the tabbed dialog packs to. */
    static final int ORDINARY_WIDTH = 420;

    /**
     * The narrowest width worth supporting. The atlas sets no minimum
     * window size (docs/decisions/point-and-identify.md), so this is
     * the dialog's own floor: narrow enough to sit beside a chart in
     * a 640 px window, which Sprint 19 established as the width where
     * the Inspector yields.
     */
    static final int MINIMUM_WIDTH = 320;

    /** One rendered mock-up, and the question it exists to answer. */
    record Shot(String name, boolean dark, int width, double textScale,
                boolean masterOn, int focusRow, String why) {
    }

    static final List<Shot> SHOTS = List.of(
            new Shot("deep-sky-tab", false, ORDINARY_WIDTH, 1.0, true, -1,
                    "the tab as a reader first meets it"),
            new Shot("deep-sky-tab-dark", true, ORDINARY_WIDTH, 1.0, true, -1,
                    "the same tab in the dark theme"),
            new Shot("deep-sky-tab-narrow", false, MINIMUM_WIDTH, 1.0, true,
                    -1, "at the dialog's narrowest useful width"),
            new Shot("deep-sky-tab-narrow-dark", true, MINIMUM_WIDTH, 1.0,
                    true, -1, "narrow, dark"),
            new Shot("deep-sky-tab-large-text", false, ORDINARY_WIDTH, 1.5,
                    true, -1, "with text enlarged by half"),
            new Shot("deep-sky-tab-large-text-dark", true, ORDINARY_WIDTH,
                    1.5, true, -1, "enlarged text, dark"),
            new Shot("deep-sky-tab-master-off", false, ORDINARY_WIDTH, 1.0,
                    false, -1,
                    "master off: the families are remembered, not erased"),
            new Shot("deep-sky-tab-master-off-dark", true, ORDINARY_WIDTH,
                    1.0, false, -1, "master off, dark"),
            new Shot("deep-sky-tab-focus", false, ORDINARY_WIDTH, 1.0, true,
                    2, "keyboard focus on the third family"),
            new Shot("deep-sky-tab-focus-dark", true, ORDINARY_WIDTH, 1.0,
                    true, 2, "keyboard focus, dark"));

    /** The other three tabs, rendered once each in the light theme. */
    static final List<String> OTHER_TABS =
            List.of("Stars", "Constellations", "Chart");

    /**
     * Renders every mock-up and returns the report rows the study
     * document prints: what was drawn, at what size, and whether any
     * control was clipped or lost its focus ring.
     */
    static List<String> write() throws IOException {
        DIR.mkdirs();
        List<String> report = new ArrayList<>();
        if (GraphicsEnvironment.isHeadless()) {
            report.add("**No display: the dialog mock-ups were NOT"
                    + " rendered.** They are evidence about a real"
                    + " window, so this run produced none rather than"
                    + " a headless imitation.");
            return report;
        }
        report.add("| mock-up | theme | width | text | dialog |"
                + " controls on the tab | cut off across | needs"
                + " scrolling | focus ring |");
        report.add("|---|---|---:|---:|---|---:|---:|---|---|");
        for (Shot shot : SHOTS) {
            report.add(render(shot));
        }
        for (String tab : OTHER_TABS) {
            report.add(renderTab(tab));
        }
        return report;
    }

    public static void main(String[] args) throws IOException {
        for (String line : write()) {
            System.out.println(line);
        }
        for (String line : mnemonics()) {
            System.out.println(line);
        }
    }

    /**
     * Every mnemonic the proposed dialog carries, tab by tab, and
     * whether any two collide. The five new families need five free
     * letters, and eleven controls already hold one each.
     */
    static List<String> mnemonics() {
        List<String> report = new ArrayList<>();
        JTabbedPane tabs = tabbedOptions(true);
        java.util.Map<Character, String> everywhere =
                new java.util.LinkedHashMap<>();
        List<String> withinTab = new ArrayList<>();
        List<String> acrossTabs = new ArrayList<>();
        report.add("| tab | control | mnemonic |");
        report.add("|---|---|---|");
        for (int i = 0; i < tabs.getTabCount(); i++) {
            JComponent view = (JComponent)
                    ((javax.swing.JScrollPane) tabs.getComponentAt(i))
                            .getViewport().getView();
            java.util.Map<Character, String> here =
                    new java.util.LinkedHashMap<>();
            for (Component control : controls(view)) {
                if (!(control instanceof AbstractButton button)
                        || button.getMnemonic() == 0) {
                    continue;
                }
                char key = (char) button.getMnemonic();
                report.add("| " + tabs.getTitleAt(i) + " | "
                        + button.getText() + " | `" + key + "` |");
                String sameTab = here.put(key, button.getText());
                if (sameTab != null) {
                    withinTab.add(key + ": " + sameTab + " and "
                            + button.getText());
                }
                String elsewhere = everywhere.put(key, button.getText());
                if (elsewhere != null && sameTab == null) {
                    acrossTabs.add(key + ": " + elsewhere + " and "
                            + button.getText());
                }
            }
        }
        report.add("| (buttons) | Restore Defaults | `R` |");
        report.add("");
        report.add(withinTab.isEmpty()
                ? "**No two controls on one tab share a letter**, which"
                        + " is the collision that would matter: a"
                        + " mnemonic only reaches the tab in front."
                : "**Collision on one tab**: "
                        + String.join("; ", withinTab));
        if (!acrossTabs.isEmpty()) {
            report.add("");
            report.add("Shared across tabs, harmlessly: "
                    + String.join("; ", acrossTabs)
                    + ". This pair collides **today**, in the single"
                    + " panel of the 1.2.0 dialog, where both controls"
                    + " are visible at once. Separating them onto"
                    + " different tabs is what makes each letter"
                    + " unambiguous - an inherited defect the tabs"
                    + " happen to fix, recorded here rather than left"
                    + " to be discovered.");
        }
        return report;
    }

    /** One mock-up, shown in a real window so focus is real focus. */
    private static String render(Shot shot) throws IOException {
        return paint(shot.name(), shot.dark(), shot.width(),
                shot.textScale(), shot.masterOn(), shot.focusRow(),
                "Deep sky");
    }

    private static String renderTab(String tab) throws IOException {
        return paint("tab-" + tab.toLowerCase(Locale.ROOT), false,
                ORDINARY_WIDTH, 1.0, true, -1, tab);
    }

    /**
     * Builds the mock dialog, shows it, paints it into a file, and
     * measures it. The window is real because the two questions that
     * matter - does anything clip, and does the focus ring show -
     * cannot be answered by an offscreen image.
     */
    private static String paint(String name, boolean dark, int width,
                                double textScale, boolean masterOn,
                                int focusRow, String tab)
            throws IOException {
        Object[] measured = new Object[6];
        BufferedImage[] image = new BufferedImage[1];
        JFrame[] window = new JFrame[1];
        JTabbedPane[] pane = new JTabbedPane[1];
        JCheckBox[] wanted = new JCheckBox[1];
        int[] size = new int[2];
        try {
            // Laid out at its real width first, then re-wrapped and
            // re-measured around the taller text, and only then
            // shown - so the focus request is the last thing the
            // window is asked for, and the toolkit can deliver the
            // activation and the focus change before anything is
            // measured. Sleeping on the event thread would prevent
            // exactly those events from arriving.
            SwingUtilities.invokeAndWait(() -> {
                theme(dark, textScale);
                JTabbedPane tabs = tabbedOptions(masterOn);
                JComponent content = frameContent(tabs);
                JFrame frame = new JFrame("Chart Options");
                frame.setContentPane(content);
                frame.pack();
                frame.setSize(width, frame.getHeight());
                frame.validate();
                rewrap(content);
                int height = tallestTab(frame, tabs, width);
                select(tabs, tab);
                frame.setSize(width, height);
                frame.validate();
                // A second pass, because the first one can bring a
                // scroll bar into being and take its width away from
                // the very text that summoned it.
                rewrap(content);
                frame.setSize(width, tallestTab(frame, tabs, width));
                select(tabs, tab);
                height = frame.getHeight();
                frame.validate();
                frame.setLocation(60, 60);
                frame.setVisible(true);
                frame.toFront();
                frame.requestFocus();
                if (focusRow >= 0) {
                    wanted[0] = familyBoxes(tabs).get(focusRow);
                    wanted[0].requestFocusInWindow();
                }
                window[0] = frame;
                pane[0] = tabs;
                size[0] = width;
                size[1] = height;
            });
            settle(window[0], wanted[0]);
            SwingUtilities.invokeAndWait(() -> {
                JFrame frame = window[0];
                JComponent content = (JComponent) frame.getContentPane();
                frame.validate();
                BufferedImage shot = new BufferedImage(size[0], size[1],
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = shot.createGraphics();
                try {
                    content.paint(g);
                } finally {
                    g.dispose();
                }
                // Only the tab a reader can see: the hidden tabs are
                // laid out too, and counting them would say nothing
                // about what is on screen.
                javax.swing.JViewport port = viewport(pane[0]);
                JComponent visible = (JComponent) port.getView();
                List<Component> across = beyond(port, visible, false);
                List<Component> below = beyond(port, visible, true);
                measured[0] = size[0] + "x" + size[1] + " px";
                measured[1] = controls(visible).size();
                measured[2] = across.size();
                measured[3] = wanted[0] == null ? "-"
                        : wanted[0].isFocusOwner()
                                ? "yes, on " + wanted[0].getText()
                                : "**NO**";
                measured[4] = across.isEmpty() ? "" : names(across);
                measured[5] = below.isEmpty() ? "no"
                        : below.size() + " row"
                                + (below.size() == 1 ? "" : "s");
                image[0] = shot;
                frame.dispose();
            });
        } catch (Exception e) {
            throw new IllegalStateException("mock-up " + name, e);
        }
        ImageIO.write(image[0], "png", new File(DIR, name + ".png"));
        return String.format(Locale.ROOT,
                "| [%s](%s.png) | %s | %d px | %.1fx | %s | %d | %d%s |"
                        + " %s | %s |",
                name, name, dark ? "dark" : "light", width, textScale,
                measured[0], (Integer) measured[1], (Integer) measured[2],
                measured[4].toString().isEmpty() ? ""
                        : " (" + measured[4] + ")",
                measured[5], measured[3]);
    }

    /**
     * Waits, off the event thread, for the window to become active
     * and the wanted control to take focus - asking again on each
     * turn, since a request made before the window can accept focus
     * is simply refused rather than queued.
     */
    private static void settle(JFrame frame, JCheckBox wanted) {
        long deadline = System.nanoTime()
                + java.util.concurrent.TimeUnit.SECONDS.toNanos(4);
        while (System.nanoTime() < deadline) {
            if (frame.isActive()
                    && (wanted == null || wanted.isFocusOwner())) {
                return;
            }
            try {
                SwingUtilities.invokeAndWait(() -> {
                    if (!frame.isActive()) {
                        frame.toFront();
                        frame.requestFocus();
                    }
                    if (wanted != null && !wanted.isFocusOwner()) {
                        wanted.requestFocusInWindow();
                    }
                });
                Thread.sleep(100);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Applies the real application theme, optionally with larger
     * text. The scale is cleared first: {@code defaultFont} is a user
     * default that outlives a look-and-feel change, so leaving it set
     * would silently enlarge every later mock-up.
     */
    static void theme(boolean dark, double scale) {
        UIManager.put("defaultFont", null);
        UiTheme.apply(dark);
        if (scale != 1.0) {
            Font base = UIManager.getFont("defaultFont");
            if (base == null) {
                base = UIManager.getFont("Label.font");
            }
            UIManager.put("defaultFont", base.deriveFont(
                    (float) (base.getSize2D() * scale)));
            UiTheme.apply(dark);
        }
    }

    /** The tabbed dialog, buttons and all, as #185 would build it. */
    static JComponent frameContent(JTabbedPane tabs) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        panel.add(tabs, java.awt.BorderLayout.CENTER);

        JPanel buttons = new JPanel(new java.awt.BorderLayout());
        buttons.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JButton restore = new JButton("Restore Defaults");
        restore.setMnemonic('R');
        buttons.add(restore, java.awt.BorderLayout.WEST);
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(new JButton("Cancel"));
        right.add(Box.createHorizontalStrut(8));
        right.add(new JButton("OK"));
        buttons.add(right, java.awt.BorderLayout.EAST);
        panel.add(buttons, java.awt.BorderLayout.SOUTH);
        return panel;
    }

    /** The four proposed tabs. */
    static JTabbedPane tabbedOptions(boolean masterOn) {
        JTabbedPane tabs = new JTabbedPane();
        // One row of tabs, always. The default wrapping layout
        // rearranges the rows so the selected tab's row sits nearest
        // the content - at 320 px it moved Deep sky below
        // Constellations and Chart, which is a dialog that
        // reorganises itself under the reader.
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.getAccessibleContext().setAccessibleName("Chart options");
        tabs.addTab("Deep sky", scrolling(deepSkyTab(masterOn)));
        tabs.addTab("Stars", plainTab(new String[][] {
                {"Star names", "S",
                        "Traditional proper names such as Betelgeuse"},
                {"Bayer letters", "y",
                        "Greek designations such as alpha Orionis"},
                {"Flamsteed numbers", "F",
                        "Flamsteed catalogue numbers on regional charts"}}));
        tabs.addTab("Constellations", plainTab(new String[][] {
                {"Constellation figures", "f",
                        "The joined stick figures"},
                {"Constellation boundaries", "b",
                        "The IAU boundaries, precessed from B1875"},
                {"Constellation names", "n",
                        "The figure's name, drawn where the figure is"}}));
        tabs.addTab("Chart", plainTab(new String[][] {
                {"Equatorial coordinate grid", "E",
                        "ICRS/J2000 grid lines with coordinate labels"},
                {"Title block", "T",
                        "The lower-left panel stating target, centre,"
                                + " frame, field, limit and orientation"},
                {"Stellar-magnitude key", "k",
                        "The upper-right key showing the circle size"
                                + " drawn for three visual magnitudes"}}));
        return tabs;
    }

    private static void select(JTabbedPane tabs, String title) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getTitleAt(i).equals(title)) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
    }

    /** The Deep sky tab: master, five families as legend and control. */
    private static JComponent deepSkyTab(boolean masterOn) {
        JPanel panel = column();
        JCheckBox master = box("Deep-sky objects", 'D', masterOn,
                "Draw deep-sky objects on the chart at all");
        panel.add(master);
        panel.add(Box.createVerticalStrut(8));
        for (DeepSkyVocabularyStudyMain.Family family
                : DeepSkyVocabularyStudyMain.FAMILIES) {
            panel.add(familyRow(family, masterOn));
        }
        panel.add(Box.createVerticalStrut(8));
        JCheckBox labels = box("Deep-sky labels", 'l', true,
                "Name the deep-sky objects the chart draws");
        labels.setEnabled(masterOn);
        panel.add(labels);
        return panel;
    }

    /**
     * One family: its checkbox, the chart's own symbol, its name and
     * a concise explanation with examples. The explanation is visible
     * text and an accessible description both - no meaning here
     * depends on hovering.
     */
    private static JComponent familyRow(
            DeepSkyVocabularyStudyMain.Family family, boolean enabled) {
        JPanel row = column();
        row.setBorder(BorderFactory.createEmptyBorder(2, 16, 6, 0));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.X_AXIS));
        head.setAlignmentX(0.0f);
        JCheckBox check = box(family.name(), family.mnemonic(), true,
                DeepSkyVocabularyStudyMain.prose(family));
        check.setEnabled(enabled);
        head.add(check);
        head.add(Box.createHorizontalStrut(6));
        head.add(new SymbolChip(family));
        head.add(Box.createHorizontalGlue());
        row.add(head);

        // The explanation wraps to the width it is actually given
        // (see rewrap), rather than to a fixed column that would
        // overflow a narrow dialog and waste a wide one.
        String prose = DeepSkyVocabularyStudyMain.prose(family);
        JLabel explains = new JLabel(prose);
        explains.putClientProperty(WRAPPED_TEXT, prose);
        explains.setAlignmentX(0.0f);
        explains.setBorder(BorderFactory.createEmptyBorder(1, 22, 0, 0));
        explains.putClientProperty("FlatLaf.styleClass", "small");
        explains.setEnabled(enabled);
        // The row is one thing to a screen reader: the checkbox
        // carries the whole meaning, so the visible explanation is
        // not read twice.
        explains.getAccessibleContext().setAccessibleName("");
        row.add(explains);
        return row;
    }

    /**
     * A scrap of the chart's own paper carrying the production symbol.
     *
     * <p>The chart's ink is mid-grey on white and never follows the
     * application theme (docs/chart-conventions.md). Painted straight
     * onto a dark dialog it would be all but invisible, so the legend
     * shows the symbol on the ground the page actually gives it. The
     * study measures both contrasts rather than asserting this.
     */
    static final class SymbolChip extends JComponent {

        private final DeepSkyVocabularyStudyMain.Family family;

        SymbolChip(DeepSkyVocabularyStudyMain.Family family) {
            this.family = family;
            // The chip keeps pace with the dialog's text: a reader
            // who enlarged the type did not ask for a smaller symbol.
            int side = Math.round(CHIP_PX * textScale());
            setPreferredSize(new Dimension(side, side));
            setMaximumSize(new Dimension(side, side));
            setMinimumSize(new Dimension(side, side));
            getAccessibleContext().setAccessibleName(
                    "The symbol the chart draws for " + family.name());
        }

        @Override
        public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJComponent() {
                    @Override
                    public javax.accessibility.AccessibleRole
                            getAccessibleRole() {
                        return javax.accessibility.AccessibleRole.ICON;
                    }
                };
            }
            return accessibleContext;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PAPER);
                g.fillRect(0, 0, getWidth(), getHeight());
                ChartRenderer.drawLegendSymbol(g,
                        DeepSkyVocabularyStudyMain.typeFor(family),
                        getWidth() / 2.0, getHeight() / 2.0,
                        SYMBOL_PX * textScale());
                // A hairline edge: white paper on a light dialog is
                // otherwise not visibly a chip at all.
                Color edge = UIManager.getColor("Component.borderColor");
                g.setColor(edge == null ? Color.GRAY : edge);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            } finally {
                g.dispose();
            }
        }
    }

    /** The chip's side, and the symbol's larger axis inside it. */
    static final int CHIP_PX = 22;
    static final double SYMBOL_PX = 11.0;

    /** The size the chip is measured at: the ordinary dialog font. */
    static final float BASE_FONT_PX = 13.0f;

    /** How far the reader has enlarged the dialog's text. */
    static float textScale() {
        Font font = UIManager.getFont("Label.font");
        return font == null ? 1.0f : font.getSize2D() / BASE_FONT_PX;
    }

    /**
     * The fade a switched-off family's chip was going to take, kept
     * only so the study can price the alternative the gate rejected:
     * fading the symbol halves its contrast, and the symbol is
     * information rather than a control.
     */
    static final float REJECTED_DISABLED_ALPHA = 0.45f;

    private static JComponent plainTab(String[][] rows) {
        JPanel panel = column();
        for (String[] row : rows) {
            panel.add(box(row[0], row[1].charAt(0), true, row[2]));
        }
        return scrolling(panel);
    }

    /**
     * Every tab scrolls rather than clips. Enlarged text and a narrow
     * dialog both make a tab taller than the space it has, and a
     * control below the fold must still be reachable - by keyboard as
     * well as by pointer, which scrolling gives and clipping does not.
     */
    private static javax.swing.JScrollPane scrolling(JComponent view) {
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(view,
                javax.swing.ScrollPaneConstants
                        .VERTICAL_SCROLLBAR_AS_NEEDED,
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    /**
     * A column that never grows wider than the space it is given.
     *
     * <p>A plain panel inside a scroll pane keeps its preferred
     * width, so a long line pushes the column past the viewport and,
     * with no horizontal scroll bar, quietly cuts the right-hand edge
     * off every row. Tracking the viewport's width is what makes
     * "nothing is cut off across the dialog" true rather than hoped
     * for; the measurement in the study catches it if it stops being
     * true.
     */
    private static final class ScrollableColumn extends JPanel
            implements javax.swing.Scrollable {

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

    private static JPanel column() {
        JPanel panel = new ScrollableColumn();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.setAlignmentX(0.0f);
        return panel;
    }

    static JCheckBox box(String text, char mnemonic, boolean selected,
                         String description) {
        JCheckBox check = new JCheckBox(text, selected);
        check.setMnemonic(mnemonic);
        check.setOpaque(false);
        check.setAlignmentX(0.0f);
        check.getAccessibleContext().setAccessibleName(text);
        check.getAccessibleContext().setAccessibleDescription(description);
        check.setToolTipText(description);
        return check;
    }

    /** The five family checkboxes of the Deep sky tab, in order. */
    static List<JCheckBox> familyBoxes(JTabbedPane tabs) {
        List<JCheckBox> found = new ArrayList<>();
        for (Component control : controls((JComponent) tabs.getComponentAt(0))) {
            if (control instanceof JCheckBox check) {
                found.add(check);
            }
        }
        // The master leads and the labels checkbox trails; the
        // families are what lies between.
        return found.subList(1, found.size() - 1);
    }

    /** Every control a reader can operate or read, in tree order. */
    static List<Component> controls(JComponent root) {
        List<Component> found = new ArrayList<>();
        collect(root, found);
        return found;
    }

    private static void collect(Container container, List<Component> into) {
        for (Component child : container.getComponents()) {
            if (child instanceof AbstractButton || child instanceof JLabel) {
                into.add(child);
            }
            if (child instanceof Container inner) {
                collect(inner, into);
            }
        }
    }

    /**
     * Controls reaching past the visible tab, in one direction.
     *
     * <p>The two directions are not the same failure. A control cut
     * off <em>across</em> the dialog is unreadable and unreachable,
     * and must never happen. A control <em>below</em> the fold is
     * reached by scrolling, which is why every tab sits in a scroll
     * pane - it is worth reporting, but it is not a defect.
     */
    static List<Component> beyond(javax.swing.JViewport port,
                                  JComponent within, boolean vertically) {
        List<Component> out = new ArrayList<>();
        Rectangle visible = new Rectangle(0, 0, port.getWidth(),
                port.getHeight());
        for (Component control : controls(within)) {
            if (control.getWidth() == 0 || control.getHeight() == 0) {
                continue;
            }
            Rectangle bounds = SwingUtilities.convertRectangle(
                    control.getParent(), control.getBounds(), within);
            boolean out0 = vertically
                    ? bounds.y + bounds.height > visible.height
                    : bounds.x + bounds.width > visible.width
                            || bounds.x < 0;
            if (out0) {
                out.add(control);
            }
        }
        return out;
    }

    /** Where a wrapped label keeps the words it is wrapping. */
    private static final String WRAPPED_TEXT = "juranometria.wrappedText";

    /**
     * Breaks prose into lines that fit a given width, measured with
     * the font that will draw them.
     *
     * <p>A CSS width on the body does not bound a label's preferred
     * width - measured here at 425 px for a body declared 310 px
     * wide - and Swing then paints the text past the label's own
     * edge, where no bounds check finds it. Breaking the lines
     * against real font metrics is the only way the words are known
     * to fit.
     */
    static String wrapped(String prose, int widthPx,
                          java.awt.FontMetrics metrics) {
        StringBuilder html = new StringBuilder("<html>");
        StringBuilder line = new StringBuilder();
        for (String word : prose.split(" ")) {
            String candidate = line.isEmpty() ? word
                    : line + " " + word;
            if (!line.isEmpty()
                    && metrics.stringWidth(candidate) > widthPx) {
                html.append(escaped(line.toString())).append("<br>");
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        return html.append(escaped(line.toString()))
                .append("</html>").toString();
    }

    private static String escaped(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Re-wraps every wrapped label to the width the laid-out dialog
     * actually gave it, and to the font it ended up with.
     */
    static void rewrap(JComponent root) {
        for (Component control : controls(root)) {
            if (!(control instanceof JLabel label)) {
                continue;
            }
            Object prose = label.getClientProperty(WRAPPED_TEXT);
            if (prose == null) {
                continue;
            }
            java.awt.Insets insets = label.getInsets();
            int available = label.getWidth() - insets.left - insets.right;
            if (available > 40) {
                label.setText(wrapped((String) prose, available,
                        label.getFontMetrics(label.getFont())));
            }
        }
        root.revalidate();
    }

    /** The viewport of the selected tab's scroll pane. */
    static javax.swing.JViewport viewport(JTabbedPane tabs) {
        return ((javax.swing.JScrollPane) tabs.getSelectedComponent())
                .getViewport();
    }

    /**
     * The height that holds the tallest of the four tabs, so moving
     * between tabs never resizes the dialog under the reader. Capped,
     * because a tall dialog on a short screen is worse than a scroll
     * bar.
     */
    static int tallestTab(JFrame frame, JTabbedPane tabs, int width) {
        int tallest = 0;
        int selected = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setSelectedIndex(i);
            frame.pack();
            frame.setSize(width, frame.getHeight());
            frame.validate();
            javax.swing.JViewport port = viewport(tabs);
            int overflow = Math.max(0,
                    port.getView().getPreferredSize().height
                            - port.getHeight());
            tallest = Math.max(tallest, frame.getHeight() + overflow);
        }
        tabs.setSelectedIndex(selected);
        return Math.min(tallest, MAXIMUM_HEIGHT);
    }

    /** As tall as the dialog may grow before it starts scrolling. */
    static final int MAXIMUM_HEIGHT = 780;

    private static String names(List<Component> components) {
        List<String> names = new ArrayList<>();
        for (Component component : components) {
            String text = component instanceof AbstractButton button
                    ? button.getText()
                    : component instanceof JLabel label
                            ? label.getText() : component.getName();
            names.add(shorten(text));
        }
        return String.join("; ", names);
    }

    /** A control's name in a few words: the report is a table. */
    private static String shorten(String text) {
        if (text == null) {
            return "unnamed";
        }
        String plain = text.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ").trim();
        return plain.length() <= 40 ? plain
                : plain.substring(0, 37) + "...";
    }

}
