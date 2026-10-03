package juranometria.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartOptions;
import juranometria.sky.Observer;
import juranometria.ui.companion.CompanionSection;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.placeandtime.PlaceAndTimePanel;
import juranometria.ui.placeandtime.PlaceStore;

/**
 * Chart Options in the Controls companion, measured before it moves
 * (Sprint 40, issue #442).
 *
 * <p>Every mock-up holds the <em>production</em> Chart Options
 * controls - {@link ChartOptionsDialog#contentForStudy}, in the stated
 * language - and the production section heading,
 * {@link CompanionSection}. The layouts compared are the four tabs as
 * they are, the tabs' four columns as collapsible subject groups (all
 * open, or one open at a time), both companion sections together on a
 * short screen, and the groups with a master switch off. The action
 * rows each semantic would need are drawn with the production words
 * where they exist and study words where they do not (since #443 the
 * dialog has Close, and Cancel and OK are study words for the footer it
 * used to have).
 *
 * <p>Painted off screen, as the contract's headless process requires;
 * the implementation's photographs will be of real windows. Standard output is the portable report; how tall and
 * wide the words are drawn is this machine's, in {@code platform.md}.
 * The images carry the {@code controls-} prefix: widget inspection
 * imagery, held to substance and not to bytes.
 */
public final class ChartOptionsCompanionStudyMain {

    static final Path DIR = Path.of("docs/studies/chart-options-companion");

    /** The companion's default width (CompanionWindow.DEFAULT_WIDTH). */
    static final int WIDTH = juranometria.ui.companion.CompanionWindow.DEFAULT_WIDTH;

    /** A short screen's room for the companion, for the scrolling mock-up. */
    static final int SHORT = 640;

    /** The screens whose usable height is compared, logical pixels. */
    static final int[] SCREEN_HEIGHTS = {800, 900, 1080, 1440};

    /** What a screen and a window keep for themselves: a menu bar and a title bar. */
    static final int CHROME = 25 + 28;

    static final String[] LANGUAGES = {"en", "nb-NO"};

    /** Words the semantics need that production does not have yet. */
    private static String study(String language, String what) {
        boolean en = language.equals("en");
        return switch (what) {
            case "close" -> en ? "Close" : "Lukk";
            case "cancel" -> en ? "Cancel" : "Avbryt";
            case "ok" -> "OK";
            case "apply" -> en ? "Apply" : "Bruk";
            case "revert" -> en ? "Revert" : "Tilbakestill";
            case "pending" -> en
                    ? "Not applied yet: the chart shows the last applied choices."
                    : "Ikke tatt i bruk ennå: kartet viser de sist brukte valgene.";
            default -> throw new IllegalArgumentException(what);
        };
    }

    private ChartOptionsCompanionStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIR);
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        Preferences node = Preferences.userRoot().node(
                "juranometria-study-co-companion-" + System.nanoTime());
        StringBuilder report = new StringBuilder();
        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "Chart Options in the companion, as this desktop draws it",
                "Sprint 40, issue #442.");
        List<String> images = new ArrayList<>();
        try {
            SwingUtilities.invokeAndWait(() -> UIManager.put("defaultFont", null));
            preface(report);
            platform.append("## Heights and widths\n\n")
                    .append("Heights at the companion's default width, ")
                    .append(WIDTH).append(" px. The narrowest companion is"
                            + " the one in which no\ncheckbox, label, button or"
                            + " field is drawn narrower than it asks.\n\n")
                    .append("| language | appearance | Deep sky | Stars |"
                            + " Constellations | Chart | groups, all open |"
                            + " tabs | Place and Time | both, groups open |"
                            + " narrowest, groups | narrowest, tabs |\n")
                    .append("|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
            List<String> fits = new ArrayList<>();
            for (String language : LANGUAGES) {
                InterfaceText said = InterfaceText.forLanguage(language);
                for (boolean dark : new boolean[] {false, true}) {
                    String appearance = dark ? "dark" : "light";
                    int[] row = onEdt(() -> {
                        juranometria.app.UiTheme.apply(dark);
                        return measure(node, said);
                    });
                    platform.append("| `").append(language).append("` | ")
                            .append(appearance);
                    for (int value : row) {
                        platform.append(" | ").append(value);
                    }
                    platform.append(" |\n");
                    if (!dark) {
                        StringBuilder line = new StringBuilder("| `" + language
                                + "` | " + row[7] + " px");
                        for (int screen : SCREEN_HEIGHTS) {
                            line.append(" | ").append(row[7] <= screen - CHROME
                                    ? "fits" : "scrolls");
                        }
                        fits.add(line.append(" |").toString());
                    }
                }
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    return drawLight(node, said, language);
                }));
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(true);
                    String name = "controls-co-companion-" + language
                            + "-3-groups-dark.png";
                    write(shell(List.of(chartOptionsGroups(node, said, false, -1)),
                            Integer.MAX_VALUE), name);
                    return List.of(name);
                }));
                inventory(report, node, said);
            }
            platform.append("\n## Both sections, open, on four screens\n\n")
                    .append("Usable height is the screen less a menu bar and a"
                            + " title bar (").append(CHROME).append(" px).\n\n")
                    .append("| language | both open |");
            for (int screen : SCREEN_HEIGHTS) {
                platform.append(" ").append(screen).append(" |");
            }
            platform.append("\n|---|---:|");
            for (int ignored : SCREEN_HEIGHTS) {
                platform.append("---|");
            }
            platform.append('\n');
            for (String line : fits) {
                platform.append(line).append('\n');
            }
            focus(report, node);
            report.append("## The images\n\n");
            for (String image : images) {
                report.append("- `").append(image).append("`\n");
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                UIManager.put("defaultFont", font);
                try {
                    UIManager.setLookAndFeel(before);
                } catch (javax.swing.UnsupportedLookAndFeelException gone) {
                    throw new IllegalStateException(gone);
                }
            });
            try {
                node.removeNode();
            } catch (java.util.prefs.BackingStoreException leaving) {
                // An empty node is a blemish, not a failure.
            }
        }
        System.out.print(PlatformEvidence.portable(report.toString()));
        PlatformEvidence.write(platform, DIR.resolve("platform.md").toString());
    }

    private static void preface(StringBuilder out) {
        out.append("# Chart Options in the companion, measured before it moves\n\n")
                .append("Sprint 40, issue #442. Generated by\n"
                        + "`juranometria.tool.ChartOptionsCompanionStudyMain`"
                        + " from the compiled application.\n\n")
                .append("Every mock-up holds the production Chart Options"
                        + " controls and the production\ncompanion section"
                        + " heading. The groups are the dialog's four tab"
                        + " columns,\nmoved under headings; the action rows"
                        + " are the three semantics' proposals.\nHow tall and"
                        + " wide the words are drawn is one machine's answer,"
                        + " in\n`platform.md` beside this.\n\n");
    }

    // ---- the production pieces ---------------------------------------

    /** A controller over a scratch node, at the released defaults or with deep sky off. */
    private static ChartOptionsController controller(Preferences node,
                                                     boolean deepSkyOff) {
        try {
            node.clear();
        } catch (java.util.prefs.BackingStoreException e) {
            throw new IllegalStateException(e);
        }
        ChartOptionsController controller = new ChartOptionsController(
                ChartOptionsStore.forNode(node));
        if (deepSkyOff) {
            ChartOptions d = ChartOptions.DEFAULTS;
            controller.apply(new ChartOptions(false, d.deepSkyLabels(),
                    d.constellationFigures(), d.constellationBoundaries(),
                    d.constellationNames(), d.starNames(), d.bayerLetters(),
                    d.flamsteedNumbers(), d.equatorialGrid(), d.titleBlock(),
                    d.magnitudeKey(), d.galaxies(), d.openClusters(),
                    d.globularClusters(), d.nebulae(), d.planetaryNebulae(),
                    d.palette()));
        }
        return controller;
    }

    /** The dialog's content: its tabs and its button row. */
    private static JComponent dialogContent(Preferences node, InterfaceText said,
                                            boolean deepSkyOff) {
        return ChartOptionsDialog.contentForStudy(controller(node, deepSkyOff),
                said.language());
    }

    /** One subject: its tab's title and its column of controls. */
    private record Subject(String title, JComponent column) {
    }

    /** The four tab columns, lifted out of the tabs they sit in. */
    private static List<Subject> subjects(JComponent content) {
        JTabbedPane tabs = ChartOptionsDialog.tabsOf(content);
        List<Subject> subjects = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            JScrollPane scroll = (JScrollPane) tabs.getComponentAt(i);
            JComponent column = (JComponent) scroll.getViewport().getView();
            scroll.getViewport().remove(column);
            subjects.add(new Subject(tabs.getTitleAt(i), column));
        }
        return subjects;
    }

    /**
     * The Chart Options section as collapsible subject groups.
     *
     * @param open which subject is open, or -1 for all of them
     */
    private static JComponent chartOptionsGroups(Preferences node,
                                                 InterfaceText said,
                                                 boolean deepSkyOff, int open) {
        List<Subject> subjects = subjects(dialogContent(node, said, deepSkyOff));
        JPanel groups = new JPanel();
        groups.setLayout(new BoxLayout(groups, BoxLayout.Y_AXIS));
        for (int i = 0; i < subjects.size(); i++) {
            CompanionSection group = CompanionSection.forStudy(
                    "chartoptions." + i, subjects.get(i).title(),
                    subjects.get(i).column(), said);
            if (open >= 0 && open != i) {
                group.heading().doClick();
            }
            groups.add(group);
        }
        groups.add(actions(said, List.of(said.say("chartoptions.defaults.label")),
                List.of(), null));
        return CompanionSection.forStudy("chartoptions",
                said.say("chartoptions.title"), groups, said);
    }

    /** The Chart Options section holding the four tabs as they are. */
    private static JComponent chartOptionsTabs(Preferences node,
                                               InterfaceText said) {
        JComponent content = dialogContent(node, said, false);
        replaceButtons(content, actions(said,
                List.of(said.say("chartoptions.defaults.label")), List.of(), null));
        return CompanionSection.forStudy("chartoptions",
                said.say("chartoptions.title"), content, said);
    }

    private static JComponent placeAndTime(InterfaceText said) {
        PlaceAndTimeSheetMain.State oslo = PlaceAndTimeSheetMain.STATES.stream()
                .filter(state -> state.name().equals("oslo"))
                .findFirst().orElseThrow();
        MeridianModule module = new MeridianModule(new Observer(oslo.latitude(),
                oslo.eastLongitude(), PlaceAndTimeSheetMain.WHEN));
        module.showing(oslo.meridian(), oslo.horizon(), oslo.zenith());
        Preferences place = Preferences.userRoot().node(
                "juranometria-study-co-place-" + System.nanoTime());
        try {
            return CompanionSection.forStudy("placeandtime",
                    said.say("placeandtime.title"),
                    new PlaceAndTimePanel(module, PlaceStore.forNode(place),
                            () -> PlaceAndTimeSheetMain.WHEN, said), said);
        } finally {
            try {
                place.removeNode();
            } catch (java.util.prefs.BackingStoreException leaving) {
                // An empty node is a blemish, not a failure.
            }
        }
    }

    /** An action row: buttons on the left, buttons on the right, a note above. */
    private static JComponent actions(InterfaceText said, List<String> left,
                                      List<String> right, String note) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        JPanel west = new JPanel();
        west.setLayout(new BoxLayout(west, BoxLayout.X_AXIS));
        for (String text : left) {
            west.add(new JButton(text));
        }
        JPanel east = new JPanel();
        east.setLayout(new BoxLayout(east, BoxLayout.X_AXIS));
        for (String text : right) {
            if (east.getComponentCount() > 0) {
                east.add(Box.createHorizontalStrut(8));
            }
            east.add(new JButton(text));
        }
        row.add(west, BorderLayout.WEST);
        row.add(east, BorderLayout.EAST);
        if (note != null) {
            JLabel pending = new JLabel("<html>" + note + "</html>");
            pending.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            pending.putClientProperty("FlatLaf.style",
                    "foreground: $Label.disabledForeground");
            row.add(pending, BorderLayout.NORTH);
        }
        return row;
    }

    /** Swaps the dialog's own button row for a proposed one. */
    private static void replaceButtons(JComponent content, JComponent row) {
        BorderLayout layout = (BorderLayout) content.getLayout();
        Component buttons = layout.getLayoutComponent(BorderLayout.SOUTH);
        if (buttons != null) {
            content.remove(buttons);
        }
        content.add(row, BorderLayout.SOUTH);
    }

    // ---- the shell, laid out and drawn off screen ----------------------

    /** The companion's sections, at its default width, at most so tall. */
    private static JPanel shell(List<JComponent> sections, int tallest) {
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        for (JComponent section : sections) {
            stack.add(section);
        }
        JScrollPane scroll = new JScrollPane(new WidthTracking(stack),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel shell = new JPanel(new BorderLayout());
        shell.add(scroll, BorderLayout.CENTER);
        return sized(shell, WIDTH, tallest);
    }

    /**
     * Lays a mock-up out off screen, as the contract needs: it runs this
     * generator in a headless process. Every pass invalidates the whole
     * tree first - a box layout keeps its children's sizes until it is
     * told they changed - and re-wraps the dialog's descriptions until
     * the widths settle, as the dialog's own sizing does. Without the
     * invalidation every family's description was laid out at the height
     * of one line and drawn clipped; the heights this produces were
     * checked against the same trees validated in real windows.
     */
    private static JPanel sized(JPanel shell, int width, int tallest) {
        shell.setSize(width, 2000);
        for (int pass = 0; pass < 3; pass++) {
            layOut(shell);
            ChartOptionsDialog.rewrap(shell);
        }
        layOut(shell);
        int height = Math.min(tallest, shell.getPreferredSize().height);
        shell.setSize(width, height);
        layOut(shell);
        return shell;
    }

    private static void layOut(Component at) {
        invalidateAll(at);
        layOutValid(at);
    }

    private static void invalidateAll(Component at) {
        at.invalidate();
        if (at instanceof Container container) {
            for (Component child : container.getComponents()) {
                invalidateAll(child);
            }
        }
    }

    private static void layOutValid(Component at) {
        if (at instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layOutValid(child);
            }
        }
    }

    /** The narrowest shell in which no control is drawn narrower than it asks. */
    private static int narrowest(java.util.function.Supplier<List<JComponent>> sections) {
        for (int width = 240; width <= 800; width += 2) {
            JPanel shell = new JPanel(new BorderLayout());
            JPanel stack = new JPanel();
            stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
            for (JComponent section : sections.get()) {
                stack.add(section);
            }
            shell.add(new JScrollPane(new WidthTracking(stack),
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
            sized(shell, width, Integer.MAX_VALUE);
            if (truncated(shell).isEmpty()) {
                return width;
            }
        }
        return -1;
    }

    static List<String> truncated(Container root) {
        List<Component> all = new ArrayList<>();
        walk(root, all);
        List<String> narrow = new ArrayList<>();
        for (Component component : all) {
            if ((component instanceof JCheckBox || component instanceof AbstractButton
                    || component instanceof JTextField
                    || (component instanceof JLabel label
                            && !String.valueOf(label.getText()).startsWith("<html>")))
                    && visibleUpTo(component, root)
                    && component.getWidth() < component.getPreferredSize().width) {
                narrow.add(component.getClass().getSimpleName());
            }
        }
        return narrow;
    }

    private static boolean visibleUpTo(Component component, Container root) {
        for (Component at = component; at != null && at != root; at = at.getParent()) {
            if (!at.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static void walk(Component at, List<Component> into) {
        into.add(at);
        if (at instanceof Container container) {
            for (Component child : container.getComponents()) {
                walk(child, into);
            }
        }
    }

    private static void write(JPanel shell, String name) throws java.io.IOException {
        BufferedImage image = new BufferedImage(shell.getWidth(),
                Math.max(1, shell.getHeight()), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            shell.paint(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
    }

    // ---- the measurements and the mock-ups -----------------------------

    /**
     * Deep sky, Stars, Constellations, Chart, all four open, the tabs,
     * Place and Time, both sections open, and the narrowest companion
     * for groups and for tabs.
     */
    private static int[] measure(Preferences node, InterfaceText said) {
        int[] heights = new int[10];
        for (int i = 0; i < 4; i++) {
            int open = i;
            heights[i] = shell(List.of(chartOptionsGroups(node, said, false, open)),
                    Integer.MAX_VALUE).getHeight()
                    - shell(List.of(chartOptionsGroups(node, said, false, 99)),
                            Integer.MAX_VALUE).getHeight();
        }
        heights[4] = shell(List.of(chartOptionsGroups(node, said, false, -1)),
                Integer.MAX_VALUE).getHeight();
        heights[5] = shell(List.of(chartOptionsTabs(node, said)),
                Integer.MAX_VALUE).getHeight();
        heights[6] = shell(List.of(placeAndTime(said)), Integer.MAX_VALUE).getHeight();
        heights[7] = shell(List.of(placeAndTime(said),
                chartOptionsGroups(node, said, false, -1)), Integer.MAX_VALUE).getHeight();
        heights[8] = narrowest(() -> List.of(placeAndTime(said),
                chartOptionsGroups(node, said, false, -1)));
        heights[9] = narrowest(() -> List.of(placeAndTime(said),
                chartOptionsTabs(node, said)));
        return heights;
    }

    private static List<String> drawLight(Preferences node, InterfaceText said,
                                          String language) throws Exception {
        String stem = "controls-co-companion-" + language + "-";
        List<String> names = new ArrayList<>();
        write(shell(List.of(chartOptionsTabs(node, said)), Integer.MAX_VALUE),
                stem + "1-tabs.png");
        names.add(stem + "1-tabs.png");
        write(shell(List.of(chartOptionsGroups(node, said, false, -1)),
                Integer.MAX_VALUE), stem + "2-groups.png");
        names.add(stem + "2-groups.png");
        write(shell(List.of(chartOptionsGroups(node, said, false, 0)),
                Integer.MAX_VALUE), stem + "4-one-open.png");
        names.add(stem + "4-one-open.png");
        write(shell(List.of(placeAndTime(said),
                chartOptionsGroups(node, said, false, -1)), SHORT),
                stem + "5-both-short-screen.png");
        names.add(stem + "5-both-short-screen.png");
        write(shell(List.of(chartOptionsGroups(node, said, true, -1)),
                Integer.MAX_VALUE), stem + "6-deep-sky-off.png");
        names.add(stem + "6-deep-sky-off.png");

        // The old dialog under each semantic: its tabs, with the action
        // row that semantic gives it.
        JComponent shared = dialogContent(node, said, false);
        replaceButtons(shared, actions(said,
                List.of(said.say("chartoptions.defaults.label")),
                List.of(study(language, "close")), null));
        write(sized(wrap(shared), 420, Integer.MAX_VALUE),
                stem + "7-dialog-shared-immediate.png");
        names.add(stem + "7-dialog-shared-immediate.png");
        // The transactional footer the dialog had until #443, drawn by
        // the study since production no longer has it.
        JComponent transactional = dialogContent(node, said, false);
        replaceButtons(transactional, actions(said,
                List.of(said.say("chartoptions.defaults.label")),
                List.of(study(language, "cancel"), study(language, "ok")), null));
        write(sized(wrap(transactional), 420, Integer.MAX_VALUE),
                stem + "8-dialog-transactional.png");
        names.add(stem + "8-dialog-transactional.png");

        // The companion holding a draft: Apply and Revert, and a line
        // saying the chart does not show what the boxes show.
        List<Subject> subjects = subjects(dialogContent(node, said, false));
        JPanel groups = new JPanel();
        groups.setLayout(new BoxLayout(groups, BoxLayout.Y_AXIS));
        for (int i = 0; i < subjects.size(); i++) {
            CompanionSection group = CompanionSection.forStudy(
                    "chartoptions." + i, subjects.get(i).title(),
                    subjects.get(i).column(), said);
            if (i != 0) {
                group.heading().doClick();
            }
            groups.add(group);
        }
        groups.add(actions(said, List.of(said.say("chartoptions.defaults.label")),
                List.of(study(language, "revert"), study(language, "apply")),
                study(language, "pending")));
        write(shell(List.of(CompanionSection.forStudy("chartoptions",
                        said.say("chartoptions.title"), groups, said)),
                Integer.MAX_VALUE), stem + "9-companion-draft.png");
        names.add(stem + "9-companion-draft.png");
        return names;
    }

    private static JPanel wrap(JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    /** What each subject holds, in both languages: the same controls either way. */
    private static void inventory(StringBuilder out, Preferences node,
                                  InterfaceText said) throws Exception {
        List<Subject> subjects = onEdt(() -> subjects(dialogContent(node, said, false)));
        out.append("## The controls in `").append(said.language()).append("`\n\n")
                .append("| subject | control | accessible name |\n|---|---|---|\n");
        int count = 0;
        for (Subject subject : subjects) {
            List<Component> all = new ArrayList<>();
            walk(subject.column(), all);
            for (Component component : all) {
                if (component instanceof JCheckBox box) {
                    count++;
                    out.append("| ").append(subject.title()).append(" | ")
                            .append(box.getText()).append(" | ")
                            .append(box.getAccessibleContext().getAccessibleName())
                            .append(" |\n");
                }
            }
        }
        out.append("\n**").append(count).append(" checkboxes in ")
                .append(subjects.size()).append(" subjects.** Any layout below"
                        + " holds exactly these.\n\n");
    }

    /** How many stops a keyboard makes through each layout. */
    private static void focus(StringBuilder out, Preferences node) throws Exception {
        InterfaceText said = InterfaceText.forLanguage("en");
        int[] stops = onEdt(() -> {
            List<Subject> subjects = subjects(dialogContent(node, said, false));
            int all = 0;
            int first = 0;
            for (int i = 0; i < subjects.size(); i++) {
                List<Component> walked = new ArrayList<>();
                walk(subjects.get(i).column(), walked);
                int boxes = (int) walked.stream()
                        .filter(c -> c instanceof JCheckBox).count();
                all += boxes;
                if (i == 0) {
                    first = boxes;
                }
            }
            return new int[] {subjects.size(), all, first};
        });
        out.append("## Keyboard stops through Chart Options\n\n")
                .append("Counted from the production controls; Restore Defaults"
                        + " is one more stop in\nevery layout.\n\n")
                .append("| layout | stops before the last control |\n|---|---:|\n")
                .append("| tabs: the strip, then the open tab's boxes | ")
                .append(1 + stops[2]).append(" to reach Deep sky's last; a"
                        + " different tab is a separate gesture |\n")
                .append("| groups, all open: four headings and every box | ")
                .append(stops[0] + stops[1]).append(" |\n")
                .append("| groups, one open: four headings and the open"
                        + " subject's boxes | ")
                .append(stops[0] + stops[2]).append(" with Deep sky open |\n\n");
    }

    /** Content held to the viewport's width, as the companion holds it. */
    private static final class WidthTracking extends JPanel implements Scrollable {

        WidthTracking(JComponent content) {
            super(new BorderLayout());
            add(content, BorderLayout.NORTH);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation,
                                              int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation,
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

    private interface EdtWork<T> {
        T run() throws Exception;
    }

    private static <T> T onEdt(EdtWork<T> work) throws Exception {
        Object[] result = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                result[0] = work.run();
            } catch (Exception e) {
                failed[0] = e;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }
}
