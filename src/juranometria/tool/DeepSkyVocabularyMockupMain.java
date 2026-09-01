package juranometria.tool;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.app.UiTheme;
import juranometria.render.ChartOptions;
import juranometria.render.SymbolFamily;

/**
 * The deep-sky vocabulary study's dialog pictures (Sprint 21, issues
 * #184 and #185).
 *
 * <p>These were mock-ups at the gate. Now that #185 has built the
 * dialog, they are <strong>the production dialog itself</strong>,
 * shown in a real window under the real application themes: this
 * class builds nothing of its own beyond the frame around
 * {@code ChartOptionsDialog.content}. A study that kept its own copy
 * of the surface would be a second implementation free to drift from
 * the first - the mistake this sprint spent its gate refusing.
 *
 * <p>What it still owns is the measuring: what is cut off, what needs
 * scrolling, whether the action buttons stay reachable on a screen
 * this machine does not have, and whether keyboard focus lands where
 * it should.
 */
public final class DeepSkyVocabularyMockupMain {

    private DeepSkyVocabularyMockupMain() {
    }

    static final File DIR = new File("docs/studies/deep-sky-vocabulary");

    /**
     * A 768 px display with a 40 px taskbar - the shortest screen
     * still in ordinary use, and the one a fixed cap would fail.
     */
    static final int SHORT_SCREEN = 728;

    /**
     * An ordinary desktop's usable height, stated rather than
     * measured.
     *
     * <p>Every picture names the screen it assumes. Reading this
     * machine's own would put a number in the report that changes
     * when a dock hides itself, and a study whose output moves
     * between two runs cannot be reproduced by a reviewer. The real
     * screen's path is production's own
     * ({@code ChartOptionsDialog.sizeToScreen}), exercised whenever
     * the dialog opens and pinned by
     * {@code ChartOptionsDialogHeightTest}.
     */
    static final int ORDINARY_SCREEN = 900;

    /**
     * One rendered picture, and the question it exists to answer.
     *
     * <p>{@code usableHeight} is the screen area the dialog believes
     * it has: 0 means this machine's own, and any other value stands
     * in for a screen this machine does not have - which is the only
     * way a short display can be reviewed from a tall one.
     */
    record Shot(String name, boolean dark, int width, double textScale,
                boolean masterOn, int focusRow, int usableHeight,
                String why) {
    }

    static final List<Shot> SHOTS = List.of(
            new Shot("deep-sky-tab", false, ChartOptionsDialog.ORDINARY_WIDTH,
                    1.0, true, -1, ORDINARY_SCREEN,
                    "the tab as a reader first meets it"),
            new Shot("deep-sky-tab-dark", true,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.0, true, -1,
                    ORDINARY_SCREEN, "the same tab in the dark theme"),
            new Shot("deep-sky-tab-narrow", false,
                    ChartOptionsDialog.MINIMUM_WIDTH, 1.0, true, -1,
                    ORDINARY_SCREEN,
                    "at the dialog's narrowest useful width"),
            new Shot("deep-sky-tab-narrow-dark", true,
                    ChartOptionsDialog.MINIMUM_WIDTH, 1.0, true, -1,
                    ORDINARY_SCREEN, "narrow, dark"),
            new Shot("deep-sky-tab-large-text", false,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.5, true, -1,
                    ORDINARY_SCREEN, "with text enlarged by half"),
            new Shot("deep-sky-tab-large-text-dark", true,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.5, true, -1,
                    ORDINARY_SCREEN, "enlarged text, dark"),
            new Shot("deep-sky-tab-master-off", false,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.0, false, -1,
                    ORDINARY_SCREEN,
                    "master off: the families are remembered, not erased"),
            new Shot("deep-sky-tab-master-off-dark", true,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.0, false, -1,
                    ORDINARY_SCREEN, "master off, dark"),
            new Shot("deep-sky-tab-focus", false,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.0, true, 2,
                    ORDINARY_SCREEN,
                    "keyboard focus on the third family"),
            new Shot("deep-sky-tab-focus-dark", true,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.0, true, 2,
                    ORDINARY_SCREEN, "keyboard focus, dark"),
            new Shot("deep-sky-tab-short-screen", false,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.0, true, -1,
                    SHORT_SCREEN,
                    "on a 768 px display: does the reader still reach OK?"),
            new Shot("deep-sky-tab-short-screen-large-text", false,
                    ChartOptionsDialog.ORDINARY_WIDTH, 1.5, true, -1,
                    SHORT_SCREEN,
                    "the worst case: a short screen and enlarged text"));

    /** The other three tabs, rendered once each in the light theme. */
    static final List<String> OTHER_TABS =
            List.of("Stars", "Constellations", "Chart");

    /**
     * Renders every picture and returns the report rows the study
     * document prints.
     */
    static List<String> write() throws IOException {
        DIR.mkdirs();
        List<String> report = new ArrayList<>();
        if (GraphicsEnvironment.isHeadless()) {
            report.add("**No display: the dialog pictures were NOT"
                    + " rendered.** They are evidence about a real"
                    + " window, so this run produced none rather than"
                    + " a headless imitation.");
            return report;
        }
        report.add("| picture | theme | width | text | usable screen |"
                + " dialog | controls on the tab | cut off across |"
                + " needs scrolling | OK, Cancel, Restore |"
                + " focus ring |");
        report.add("|---|---|---:|---:|---|---|---:|---:|---|---|---|");
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
     * Every mnemonic the dialog carries, tab by tab, and whether any
     * two collide. Read from the production dialog, so this is what
     * the reader's keyboard actually reaches.
     */
    static List<String> mnemonics() {
        List<String> report = new ArrayList<>();
        JComponent content = dialogContent(true);
        JTabbedPane tabs = ChartOptionsDialog.tabsOf(content);
        java.util.Map<Character, String> everywhere =
                new java.util.LinkedHashMap<>();
        List<String> withinTab = new ArrayList<>();
        List<String> acrossTabs = new ArrayList<>();
        report.add("| tab | control | mnemonic |");
        report.add("|---|---|---|");
        for (int i = 0; i < tabs.getTabCount(); i++) {
            java.util.Map<Character, String> here =
                    new java.util.LinkedHashMap<>();
            for (Component control : controls(viewOf(tabs, i))) {
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
                    + ". This pair collided in the single panel of the"
                    + " 1.2.0 dialog, where both controls were visible"
                    + " at once and one of the two was unreachable by"
                    + " its own letter. Separating them onto different"
                    + " tabs is what makes each letter unambiguous.");
        }
        return report;
    }

    private static String render(Shot shot) throws IOException {
        return paint(shot.name(), shot.dark(), shot.width(),
                shot.textScale(), shot.masterOn(), shot.focusRow(),
                "Deep sky", shot.usableHeight());
    }

    private static String renderTab(String tab) throws IOException {
        return paint("tab-" + tab.toLowerCase(Locale.ROOT), false,
                ChartOptionsDialog.ORDINARY_WIDTH, 1.0, true, -1, tab,
                ORDINARY_SCREEN);
    }

    /**
     * Builds the production dialog's content, shows it in a real
     * window, paints it into a file, and measures it. The window is
     * real because the two questions that matter - does anything
     * clip, and does the focus ring show - cannot be answered by an
     * offscreen image.
     */
    private static String paint(String name, boolean dark, int width,
                                double textScale, boolean masterOn,
                                int focusRow, String tab, int usableHeight)
            throws IOException {
        Object[] measured = new Object[8];
        BufferedImage[] image = new BufferedImage[1];
        JFrame[] window = new JFrame[1];
        JTabbedPane[] pane = new JTabbedPane[1];
        JCheckBox[] wanted = new JCheckBox[1];
        int[] size = new int[2];
        try {
            SwingUtilities.invokeAndWait(() -> {
                theme(dark, textScale);
                JComponent content = dialogContent(masterOn);
                JFrame frame = new JFrame("Chart Options");
                frame.setContentPane(content);
                JTabbedPane tabs = ChartOptionsDialog.tabsOf(content);
                select(tabs, tab);
                sizeTo(frame, content, width, usableHeight);
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
                size[1] = frame.getHeight();
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
                JViewport port = viewport(pane[0]);
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
                measured[6] = actionButtons(frame);
                measured[7] = usableHeight + " px";
                image[0] = shot;
                frame.dispose();
            });
        } catch (Exception e) {
            throw new IllegalStateException("picture " + name, e);
        }
        ImageIO.write(image[0], "png", new File(DIR, name + ".png"));
        return String.format(Locale.ROOT,
                "| [%s](%s.png) | %s | %d px | %.1fx | %s | %s | %d |"
                        + " %d%s | %s | %s | %s |",
                name, name, dark ? "dark" : "light", width, textScale,
                measured[7], measured[0], (Integer) measured[1],
                (Integer) measured[2],
                measured[4].toString().isEmpty() ? ""
                        : " (" + measured[4] + ")",
                measured[5], measured[6], measured[3]);
    }

    /**
     * The production dialog's content, over a scratch preferences
     * node so the reader's own choices are never touched.
     */
    static JComponent dialogContent(boolean masterOn) {
        Preferences scratch = Preferences.userRoot()
                .node("juranometria-study-" + System.nanoTime());
        ChartOptionsStore store = ChartOptionsStore.forNode(scratch);
        ChartOptions options = ChartOptions.DEFAULTS;
        if (!masterOn) {
            // Master off, families untouched: the state that shows
            // they are remembered rather than erased.
            options = new ChartOptions(false, options.deepSkyLabels(),
                    options.constellationFigures(),
                    options.constellationBoundaries(),
                    options.constellationNames(), options.starNames(),
                    options.bayerLetters(), options.flamsteedNumbers(),
                    options.equatorialGrid(), options.titleBlock(),
                    options.magnitudeKey(), options.galaxies(),
                    options.openClusters(), options.globularClusters(),
                    options.nebulae(), options.planetaryNebulae());
        }
        store.save(options);
        try {
            return ChartOptionsDialog.contentForStudy(
                    new ChartOptionsController(store));
        } finally {
            try {
                scratch.removeNode();
            } catch (Exception e) {
                throw new IllegalStateException("scratch preferences", e);
            }
        }
    }

    /** Sizes the frame exactly the way the dialog sizes itself. */
    private static void sizeTo(JFrame frame, JComponent content, int width,
                               int usableHeight) {
        ChartOptionsDialog.sizeToScreen(frame, width,
                usableHeight > 0 ? usableHeight
                        : ChartOptionsDialog.usableHeight(
                                frame.getGraphicsConfiguration()));
    }

    /**
     * Applies the real application theme, optionally with larger
     * text. The scale is cleared first: {@code defaultFont} is an
     * override that outlives a look-and-feel change, so leaving it
     * set would silently enlarge every later picture.
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

    /**
     * Waits, off the event thread, for the window to become active
     * and the wanted control to take focus - asking again on each
     * turn, since a request made before the window can accept focus
     * is refused rather than queued.
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

    private static void select(JTabbedPane tabs, String title) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getTitleAt(i).equals(title)) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Whether the dialog's three action buttons are still where a
     * reader can use them: inside the window, inside the screen's
     * usable area, and reachable by Tab. This is the check a fixed
     * height ceiling would have failed - a scroll bar answers a tab
     * that is too tall; nothing answers an OK button under a taskbar.
     */
    static String actionButtons(JFrame frame) {
        Rectangle usable = frame.getGraphicsConfiguration().getBounds();
        java.awt.Insets screen = java.awt.Toolkit.getDefaultToolkit()
                .getScreenInsets(frame.getGraphicsConfiguration());
        usable = new Rectangle(usable.x + screen.left, usable.y + screen.top,
                usable.width - screen.left - screen.right,
                usable.height - screen.top - screen.bottom);
        List<String> lost = new ArrayList<>();
        List<String> unreachable = new ArrayList<>();
        for (String name : List.of("OK", "Cancel", "Restore Defaults")) {
            AbstractButton button = button(frame.getContentPane(), name);
            if (button == null || !button.isShowing()) {
                lost.add(name);
                continue;
            }
            Rectangle onScreen = new Rectangle(button.getLocationOnScreen(),
                    button.getSize());
            if (!frame.getBounds().contains(onScreen)
                    || !usable.contains(onScreen)) {
                lost.add(name);
            }
            if (!reachableByTab(frame, button)) {
                unreachable.add(name);
            }
        }
        if (lost.isEmpty() && unreachable.isEmpty()) {
            return "on screen, tab-reachable";
        }
        return "**"
                + (lost.isEmpty() ? "" : "off screen: "
                        + String.join(", ", lost))
                + (lost.isEmpty() || unreachable.isEmpty() ? "" : "; ")
                + (unreachable.isEmpty() ? "" : "no keyboard route: "
                        + String.join(", ", unreachable))
                + "**";
    }

    /** Whether Tab from the first control ever arrives at a button. */
    private static boolean reachableByTab(JFrame frame, Component target) {
        java.awt.FocusTraversalPolicy policy =
                frame.getFocusTraversalPolicy();
        if (policy == null) {
            return false;
        }
        Component at = policy.getFirstComponent(frame);
        for (int step = 0; at != null && step < 200; step++) {
            if (at == target) {
                return true;
            }
            at = policy.getComponentAfter(frame, at);
        }
        return false;
    }

    private static AbstractButton button(Container container, String text) {
        for (Component child : container.getComponents()) {
            if (child instanceof AbstractButton button
                    && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof Container inner) {
                AbstractButton found = button(inner, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** The five family checkboxes of the Deep sky tab, in order. */
    static List<JCheckBox> familyBoxes(JTabbedPane tabs) {
        List<JCheckBox> found = new ArrayList<>();
        for (Component control : controls(viewOf(tabs, 0))) {
            if (control instanceof JCheckBox check) {
                found.add(check);
            }
        }
        // The master leads and the labels checkbox trails; the
        // families are what lies between.
        return found.subList(1, found.size() - 1);
    }

    private static JComponent viewOf(JTabbedPane tabs, int index) {
        return (JComponent) ((JScrollPane) tabs.getComponentAt(index))
                .getViewport().getView();
    }

    /** The viewport of the selected tab's scroll pane. */
    static JViewport viewport(JTabbedPane tabs) {
        return ((JScrollPane) tabs.getSelectedComponent()).getViewport();
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
     * pane - worth reporting, but not a defect.
     */
    static List<Component> beyond(JViewport port, JComponent within,
                                  boolean vertically) {
        List<Component> out = new ArrayList<>();
        Rectangle visible = new Rectangle(0, 0, port.getWidth(),
                port.getHeight());
        for (Component control : controls(within)) {
            if (control.getWidth() == 0 || control.getHeight() == 0) {
                continue;
            }
            Rectangle bounds = SwingUtilities.convertRectangle(
                    control.getParent(), control.getBounds(), within);
            boolean outside = vertically
                    ? bounds.y + bounds.height > visible.height
                    : bounds.x + bounds.width > visible.width
                            || bounds.x < 0;
            if (outside) {
                out.add(control);
            }
        }
        return out;
    }

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
        return plain.length() <= 40 ? plain : plain.substring(0, 37) + "...";
    }

    /** The family a checkbox governs, by position on the tab. */
    static SymbolFamily familyAt(int row) {
        return SymbolFamily.values()[row];
    }
}
