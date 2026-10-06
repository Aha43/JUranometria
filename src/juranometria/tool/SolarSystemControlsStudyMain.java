package juranometria.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsControls;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.chart.SelectionMode;
import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.ui.ChartActions;
import juranometria.ui.ChartControls;
import juranometria.ui.ChartViewController;
import juranometria.ui.InspectorToggle;
import juranometria.ui.SearchField;
import juranometria.ui.ZoomLock;
import juranometria.ui.companion.CompanionSection;
import juranometria.ui.companion.CompanionStore;
import juranometria.ui.companion.CompanionWindow;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.placeandtime.PlaceAndTimePanel;
import juranometria.ui.placeandtime.PlaceStore;
import juranometria.ui.solar.SolarTable;
import juranometria.ui.solar.SolarTableDialog;

/**
 * The Sun and Moon tables in the JUranometria Controller, measured
 * before they move (Sprint 42, issue #457).
 *
 * <p>Every mock-up holds the <em>production</em> table content -
 * {@link SolarTableDialog#content}, in the stated language, over the
 * Oslo observer {@code PlaceAndTimeSheetMain} states at its frozen
 * instant - with its controls lifted out of the dialog's own layout
 * into the arrangements compared, and the production companion
 * section heading. The section and group titles, the proposed
 * "Show on chart" box and the menu rows are drawn with production
 * words where they exist ({@code menu.sun.label} and so on) and
 * study words where they do not, marked as such.
 *
 * <p>Painted off screen, as the contract's headless process requires;
 * the implementation's photographs will be of real windows. Standard
 * output is the portable report: what the tables hold, what is state
 * and whose, the menu as it is and as proposed, the access letters
 * that would collide in one window, and what a stale table shows. How
 * tall and wide the words are drawn is this machine's, in
 * {@code platform.md}. The images carry the {@code controls-} prefix:
 * widget inspection imagery, held to substance and not to bytes.
 */
public final class SolarSystemControlsStudyMain {

    static final Path DIR = Path.of("docs/studies/solar-system-controls");

    /** The companion's default width (CompanionWindow.DEFAULT_WIDTH). */
    static final int WIDTH = CompanionWindow.DEFAULT_WIDTH;

    /** The tallest the companion opens (its own cap). */
    static final int TALLEST = 900;

    static final String[] LANGUAGES = {"en", "nb-NO"};

    /** Rows a range gives at the steps a reader is offered, for the heights. */
    static final int[] ROWS = {1, 4, 8, 25, 169};

    private static SolarSystemService service;

    private SolarSystemControlsStudyMain() {
    }

    /** Words the arrangements need that production does not have. */
    private static String study(String language, String what) {
        boolean en = language.equals("en");
        return switch (what) {
            case "solarsystem" -> en ? "Solar System" : "Solsystem";
            case "sun" -> en ? "Sun" : "Solen";
            case "moon" -> en ? "Moon" : "Månen";
            case "result" -> en ? "Result" : "Resultat";
            default -> throw new IllegalArgumentException(what);
        };
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIR);
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        service = SolarSystemService.load();
        StringBuilder report = new StringBuilder();
        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "The Sun and Moon tables in the Controller, as this desktop draws them",
                "Sprint 42, issue #457.");
        List<String> images = new ArrayList<>();
        try {
            SwingUtilities.invokeAndWait(() -> UIManager.put("defaultFont", null));
            preface(report);
            onEdt(() -> {
                juranometria.app.UiTheme.apply(false);
                inventory(report);
                stale(report);
                menus(report);
                letters(report);
                return null;
            });
            platform.append("## Widths and heights\n\n")
                    .append("The dialogs' content at its own packed width, and"
                            + " the arrangements at the\ncompanion's default"
                            + " width, ").append(WIDTH).append(" px, light"
                            + " appearance. \"Card\" shows the one row of the\n"
                            + "instant view as heading-and-value lines; \"table\""
                            + " shows it as the table, scrolling\nsideways."
                            + " \"Both\" is Sun and Moon open; \"Controller\" is"
                            + " all four sections with Solar\nSystem as"
                            + " stated, and whether that fits a 900 px"
                            + " companion.\n\n")
                    .append("| language | Sun dialog | Moon dialog | Sun group,"
                            + " card | Sun group, table | Moon group, card |"
                            + " Moon group, table | both, card | Controller,"
                            + " Solar System collapsed | Controller, Sun open |"
                            + " Controller, both open |\n")
                    .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
            StringBuilder rowHeights = new StringBuilder();
            for (String language : LANGUAGES) {
                InterfaceText said = InterfaceText.forLanguage(language);
                onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    int[] row = measure(said);
                    platform.append("| `").append(language).append("`");
                    for (int value : row) {
                        platform.append(" | ").append(value);
                    }
                    platform.append(" |\n");
                    if (language.equals("en")) {
                        rowHeights.append(tableHeights(said));
                    }
                    return null;
                });
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    return drawLight(said, language);
                }));
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(true);
                    String name = "controls-solar-" + language + "-6-both-open-dark.png";
                    write(shell(List.of(solarSystem(said, true, true, true)),
                            Integer.MAX_VALUE), name);
                    return List.of(name);
                }));
                onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    stops(report, said);
                    return null;
                });
            }
            platform.append('\n').append(rowHeights);
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
        }
        System.out.print(PlatformEvidence.portable(report.toString()));
        PlatformEvidence.write(platform, DIR.resolve("platform.md").toString());
    }

    private static void preface(StringBuilder out) {
        out.append("# The Sun and Moon tables in the Controller, measured before they move\n\n")
                .append("Sprint 42, issue #457. Generated by\n"
                        + "`juranometria.tool.SolarSystemControlsStudyMain`"
                        + " from the compiled application.\n\n")
                .append("Every mock-up holds the production table content -"
                        + " the same `SolarTableDialog.content`\nthe dialogs"
                        + " hold, over `PlaceAndTimeSheetMain`'s Oslo observer"
                        + " at its frozen instant - with\nits controls lifted"
                        + " into the arrangements compared, and the production"
                        + " companion\nsection heading. Section and group"
                        + " titles, the proposed *Show on chart* box and the"
                        + " menu\nrows use production words where they exist"
                        + " and study words where they do not. How tall\nand"
                        + " wide the words are drawn is one machine's answer,"
                        + " in `platform.md` beside this.\n\n");
    }

    // ---- the production pieces ---------------------------------------

    /** The observer every mock-up reads: Oslo, at the frozen instant the sheets use. */
    private static Observer oslo() {
        PlaceAndTimeSheetMain.State oslo = PlaceAndTimeSheetMain.STATES.stream()
                .filter(state -> state.name().equals("oslo"))
                .findFirst().orElseThrow();
        return new Observer(oslo.latitude(), oslo.eastLongitude(),
                PlaceAndTimeSheetMain.WHEN);
    }

    /** The production content for a body, computed once as the dialog computes. */
    private static SolarTableDialog.Content content(SolarTable table,
                                                    InterfaceText said) {
        Observer observer = oslo();
        SolarTableDialog.Content content = SolarTableDialog.content(
                () -> observer, service, said, table);
        // The dialog's own scroll pane learns of the header on
        // addNotify, which never comes off screen; told here.
        for (Component c : all(content)) {
            if (c instanceof JScrollPane scroll
                    && scroll.getViewport().getView() == content.table) {
                scroll.setColumnHeaderView(content.table.getTableHeader());
            }
        }
        return content;
    }

    /** A content put into its range view with the sheets' four-row range. */
    private static SolarTableDialog.Content ranged(SolarTable table,
                                                   InterfaceText said) {
        SolarTableDialog.Content content = content(table, said);
        content.rangeView.setSelected(true);
        content.end.setText(content.start.getText().substring(0, 10)
                .replaceAll("-(\\d\\d)$", "-" + String.format("%02d",
                        Integer.parseInt(content.start.getText().substring(8, 10)) + 3))
                + " 07:30");
        content.update();
        return content;
    }

    /**
     * A body's group as the Controller would hold it, narrow: the
     * observer note wrapped, the two views on one row, the range
     * fields on rows of their own, the step and Compute, the status
     * line, the result, the proposed *Show on chart* box, and Update
     * from Place and Time. No Close: the group is not a window.
     *
     * @param card   the instant view's one row as heading-and-value
     *               lines, instead of the table
     */
    private static JComponent group(SolarTable table, InterfaceText said,
                                    boolean card, boolean range) {
        SolarTableDialog.Content content = range ? ranged(table, said)
                : content(table, said);
        String language = said.language();
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));

        // Wrapped to the room a 360 px companion gives it, as the
        // Chart Options descriptions are.
        JLabel note = new JLabel("<html><body style='width: 300px'>"
                + content.observerNote.getText() + "</body></html>");
        note.putClientProperty("FlatLaf.styleClass", "small");
        note.setAlignmentX(0f);
        column.add(note);
        column.add(Box.createVerticalStrut(6));

        JPanel views = new JPanel();
        views.setLayout(new BoxLayout(views, BoxLayout.X_AXIS));
        views.add(content.instantView);
        views.add(Box.createHorizontalStrut(10));
        views.add(content.rangeView);
        column.add(leading(views));

        column.add(labelled(said, "solartable.range.start.label", content.start));
        column.add(labelled(said, "solartable.range.end.label", content.end));
        JPanel stepRow = new JPanel();
        stepRow.setLayout(new BoxLayout(stepRow, BoxLayout.X_AXIS));
        stepRow.add(new JLabel(said.say("solartable.range.step.label")));
        stepRow.add(Box.createHorizontalStrut(6));
        stepRow.add(content.step);
        stepRow.add(Box.createHorizontalStrut(10));
        stepRow.add(content.compute);
        column.add(leading(stepRow));
        column.add(Box.createVerticalStrut(4));
        column.add(leading(content.status));
        column.add(Box.createVerticalStrut(4));

        if (card && !range) {
            column.add(cardOf(content));
        } else {
            column.add(tableOf(content, range ? 4 : 1));
        }
        column.add(Box.createVerticalStrut(6));

        // The proposed switch, with the production words of the View
        // item it would share its state with (study placement).
        JCheckBox onChart = new JCheckBox(said.say(table.body().name().equals("SUN")
                ? "menu.sunchart.label" : "menu.moonchart.label"));
        onChart.getAccessibleContext().setAccessibleName(onChart.getText());
        column.add(leading(onChart));
        column.add(Box.createVerticalStrut(4));
        column.add(leading(content.update));
        return CompanionSection.forStudy(table.prefix(),
                study(language, table.prefix()), column, said);
    }

    /** A labelled field on a row of its own, the field as wide as the row. */
    private static JComponent labelled(InterfaceText said, String key, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        JLabel label = new JLabel(said.say(key));
        label.setLabelFor(field);
        row.add(label, BorderLayout.WEST);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    /** The production table in a pane that scrolls sideways, as tall as its rows. */
    private static JComponent tableOf(SolarTableDialog.Content content, int rows) {
        JTable table = content.table;
        Container old = table.getParent();
        if (old != null) {
            old.remove(table);
        }
        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        // Off screen nothing calls addNotify, which is where a table
        // tells its scroll pane about its header; said here instead.
        scroll.setColumnHeaderView(table.getTableHeader());
        int height = table.getTableHeader().getPreferredSize().height
                + rows * table.getRowHeight()
                + scroll.getHorizontalScrollBar().getPreferredSize().height + 4;
        scroll.setPreferredSize(new Dimension(WIDTH - 40, height));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        scroll.setAlignmentX(0f);
        return scroll;
    }

    /** The instant view's one row as heading-and-value lines. */
    private static JComponent cardOf(SolarTableDialog.Content content) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        card.setAlignmentX(0f);
        for (int c = 0; c < content.model.getColumnCount(); c++) {
            JPanel line = new JPanel(new BorderLayout(8, 0));
            JLabel heading = new JLabel(content.model.getColumnName(c));
            heading.putClientProperty("FlatLaf.style",
                    "foreground: $Label.disabledForeground");
            String value = content.model.getRowCount() == 0 ? "—"
                    : String.valueOf(content.model.getValueAt(0, c));
            JLabel shown = new JLabel(value);
            line.add(heading, BorderLayout.WEST);
            line.add(shown, BorderLayout.EAST);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    line.getPreferredSize().height));
            card.add(line);
        }
        return card;
    }

    /** The Solar System section: Sun and Moon groups, each open or not. */
    private static JComponent solarSystem(InterfaceText said, boolean sunOpen,
                                          boolean moonOpen, boolean card) {
        JPanel groups = new JPanel();
        groups.setLayout(new BoxLayout(groups, BoxLayout.Y_AXIS));
        CompanionSection sun = (CompanionSection) group(SolarTable.sun(), said, card, false);
        CompanionSection moon = (CompanionSection) group(SolarTable.moon(), said, card, false);
        if (!sunOpen) {
            sun.heading().doClick();
        }
        if (!moonOpen) {
            moon.heading().doClick();
        }
        groups.add(sun);
        groups.add(moon);
        return CompanionSection.forStudy("solarsystem",
                study(said.language(), "solarsystem"), groups, said);
    }

    /** The Controller's three sections as it holds them today. */
    private static List<JComponent> existing(InterfaceText said) {
        return List.of(chartControls(said), placeAndTime(said), chartOptions(said));
    }

    private static JComponent chartControls(InterfaceText said) {
        ChartViewController controller = new ChartViewController(Atlas.assembler()::fits);
        InspectorToggle inspector = new InspectorToggle();
        inspector.bind(() -> { }, () -> true);
        ChartActions actions = new ChartActions(controller);
        ChartControls controls = new ChartControls(controller,
                new SearchField(Atlas.search(), Atlas.assembler(), controller, said),
                inspector, new SelectionMode(), new ZoomLock(), actions, said);
        controls.attachEmphasis(new juranometria.ui.ChartComponent(Atlas.assembler(),
                juranometria.ui.language.PageText.in(said)));
        return CompanionSection.forStudy("chartcontrols",
                said.say("chartcontrols.title"), controls.inCompanion(), said);
    }

    private static JComponent placeAndTime(InterfaceText said) {
        PlaceAndTimeSheetMain.State oslo = PlaceAndTimeSheetMain.STATES.stream()
                .filter(state -> state.name().equals("oslo"))
                .findFirst().orElseThrow();
        MeridianModule module = new MeridianModule(new Observer(oslo.latitude(),
                oslo.eastLongitude(), PlaceAndTimeSheetMain.WHEN));
        module.showing(oslo.meridian(), oslo.horizon(), oslo.zenith());
        Preferences place = Preferences.userRoot().node(
                "juranometria-study-ss-place-" + System.nanoTime());
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

    private static JComponent chartOptions(InterfaceText said) {
        Preferences node = Preferences.userRoot().node(
                "juranometria-study-ss-options-" + System.nanoTime());
        try {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            return CompanionSection.forStudy("chartoptions",
                    said.say("chartoptions.title"),
                    new ChartOptionsControls(controller, () -> false, said)
                            .inCompanion(CompanionStore.forNode(node), said), said);
        } finally {
            try {
                node.removeNode();
            } catch (java.util.prefs.BackingStoreException leaving) {
                // An empty node is a blemish, not a failure.
            }
        }
    }

    // ---- the portable report ------------------------------------------

    /** What each dialog's content holds, read from the production content. */
    private static void inventory(StringBuilder out) {
        InterfaceText said = InterfaceText.forLanguage("en");
        out.append("## What the two dialogs hold\n\n")
                .append("Read from `SolarTableDialog.content` for each body, in"
                        + " `en`: the controls in the\norder a keyboard meets"
                        + " them, then the columns. The \"state\" column is the"
                        + " study's:\n**observer** is Place and Time's, read on"
                        + " demand; a **draft** is the reader's typed\nquery;"
                        + " a **result** is what the service answered; a"
                        + " **mode** is which view is shown.\n\n")
                .append("| body | control | kind | state | shared today |\n"
                        + "|---|---|---|---|---|\n");
        for (SolarTable table : List.of(SolarTable.sun(), SolarTable.moon())) {
            SolarTableDialog.Content content = content(table, said);
            String b = table.prefix();
            out.append("| ").append(b).append(" | ").append(content.observerNote.getName())
                    .append(" | JLabel | observer | read from Place and Time when the"
                            + " dialog opens, comes to the front, or on Update |\n");
            out.append("| ").append(b).append(" | ").append(content.instantView.getText())
                    .append(" / ").append(content.rangeView.getText())
                    .append(" | JRadioButton | mode | no: each dialog's own |\n");
            for (JTextField field : List.of(content.start, content.end)) {
                out.append("| ").append(b).append(" | ").append(field.getName())
                        .append(" | JTextField | draft | no: seeded from the observer"
                                + " once, then the dialog's own |\n");
            }
            out.append("| ").append(b).append(" | ").append(content.step.getName())
                    .append(" | JComboBox | draft | no |\n");
            out.append("| ").append(b).append(" | ").append(content.compute.getText())
                    .append(" | JButton | action: compute the draft | no |\n");
            out.append("| ").append(b).append(" | ").append(content.update.getText())
                    .append(" | JButton | action: re-read the observer, recompute"
                            + " | no |\n");
            out.append("| ").append(b).append(" | ").append(content.status.getName())
                    .append(" | JLabel | result: rows or a refusal | no |\n");
            out.append("| ").append(b).append(" | ").append(content.table.getName())
                    .append(" | JTable, ").append(content.model.getColumnCount())
                    .append(" columns | result | no: the dialog's own model |\n");
            out.append("| ").append(b).append(" | ").append(content.close.getText())
                    .append(" | JButton | furniture | the dialog's |\n");
        }
        out.append("\nNeither dialog persists anything (ruled on #400), and"
                + " neither is told when Place and\nTime changes: the observer is"
                + " pulled, never pushed. The chart-visibility switches\n(View ▸"
                + " Sun on the chart, Moon on the chart) are `SolarSystemModule`"
                + " flags with\n**no notification seam**: the menu tick is the"
                + " item's own, and nothing follows the\nmodule. Toggling opens"
                + " no table; opening a table toggles nothing.\n\n");
        out.append("### The columns\n\n| body | columns | packed viewport |\n|---|---|---:|\n");
        for (SolarTable table : List.of(SolarTable.sun(), SolarTable.moon())) {
            out.append("| ").append(table.prefix()).append(" | ")
                    .append(String.join(", ", table.columns())).append(" | ")
                    .append(table.preferredWidth()).append(" px |\n");
        }
        out.append("\nA 360 px companion is narrower than either viewport, so a"
                + " table there scrolls\nsideways or the one row of the instant view"
                + " is shown as lines.\n\n");
    }

    /** A dialog's table goes stale when Place and Time moves, until it is asked. */
    private static void stale(StringBuilder out) {
        InterfaceText said = InterfaceText.forLanguage("en");
        Observer[] observer = {oslo()};
        SolarTableDialog.Content content = SolarTableDialog.content(
                () -> observer[0], service, said, SolarTable.sun());
        String before = String.valueOf(content.model.getValueAt(0, 0));
        observer[0] = new Observer(observer[0].latitudeDegrees(),
                observer[0].eastLongitudeDegrees(),
                observer[0].instant().plus(java.time.Duration.ofDays(1)));
        String stillShown = String.valueOf(content.model.getValueAt(0, 0));
        content.update();
        String after = String.valueOf(content.model.getValueAt(0, 0));
        out.append("## Reproduced: a table does not follow Place and Time until asked\n\n")
                .append("The production Sun content over an observer supplier,"
                        + " headless. Place and Time moved\na day; the table"
                        + " still showed `").append(stillShown)
                .append("` (as before: `").append(before)
                .append("`) until `update()` - the dialog's\nUpdate, or its"
                        + " coming to the front - after which it showed `")
                .append(after).append("`.\n\nThe dialog reads when it opens,"
                        + " comes to the front, or on Update (ruled on #400). A"
                        + " group\nin the Controller never comes to the front"
                        + " on its own, so without a rule of its own it\nwould"
                        + " show the last result for as long as the Controller"
                        + " stays open.\n\n");
    }

    /** The View menu as it is, and with a Solar System submenu. */
    private static void menus(StringBuilder out) {
        out.append("## View, as it is and as proposed\n\n")
                .append("| today (flat) | proposed |\n|---|---|\n");
        String[] today = {"menu.chartoptions", "menu.placeandtime", "menu.companion",
                "menu.toolbar", "menu.sun", "menu.moon", "menu.inspector",
                "menu.ecliptic", "menu.sunchart", "menu.moonchart", "—",
                "menu.zoomIn", "menu.zoomOut"};
        String[] proposed = {"menu.chartoptions", "menu.placeandtime", "menu.companion",
                "menu.toolbar", "menu.inspector", "menu.ecliptic",
                "Solar System ▸ (menu.sunchart, menu.moonchart, —, menu.sun, menu.moon)",
                "—", "menu.zoomIn", "menu.zoomOut"};
        InterfaceText said = InterfaceText.forLanguage("en");
        for (int i = 0; i < Math.max(today.length, proposed.length); i++) {
            out.append("| ").append(i < today.length ? label(said, today[i]) : "")
                    .append(" | ").append(i < proposed.length ? label(said, proposed[i]) : "")
                    .append(" |\n");
        }
        out.append("\nThe submenu's order is the issue's: Sun on the chart, Moon"
                + " on the chart, a separator,\nSun…, Moon…. *Solar System /"
                + " Solsystem* is a study word. Today's bar has no submenu;\n"
                + "`MenuMnemonicTest` checks each top-level menu's direct items"
                + " and would need to\nrecurse.\n\n");
    }

    private static String label(InterfaceText said, String key) {
        if (key.startsWith("menu.")) {
            return said.say(key + ".label");
        }
        if (key.startsWith("Solar System")) {
            return "Solar System ▸ " + said.say("menu.sunchart.label") + ", "
                    + said.say("menu.moonchart.label") + ", —, "
                    + said.say("menu.sun.label") + ", " + said.say("menu.moon.label");
        }
        return key;
    }

    /** Access letters that would collide inside one Controller window. */
    private static void letters(StringBuilder out) {
        out.append("## Access letters in one window\n\n")
                .append("Each dialog is its own window, so its letters are its own."
                        + " In the Controller every\ngroup shares one window with"
                        + " every other section. The letters the two table"
                        + " contents\ndeclare, per language, and the Controller"
                        + " controls already using them:\n\n")
                .append("| language | letter | Sun and Moon both use it for |"
                        + " already used in the Controller by |\n|---|---|---|---|\n");
        for (String language : LANGUAGES) {
            InterfaceText said = InterfaceText.forLanguage(language);
            Map<Character, String> table = new LinkedHashMap<>();
            SolarTableDialog.Content content = content(SolarTable.sun(), said);
            for (AbstractButton button : List.of(content.instantView, content.rangeView,
                    content.compute, content.update, content.close)) {
                if (button.getMnemonic() != 0) {
                    table.put(Character.toUpperCase((char) button.getMnemonic()),
                            button.getText());
                }
            }
            for (String key : List.of("solartable.range.start.mnemonic",
                    "solartable.range.end.mnemonic", "solartable.range.step.mnemonic")) {
                String letter = said.say(key);
                if (!letter.isBlank()) {
                    table.put(Character.toUpperCase(letter.charAt(0)),
                            said.say(key.replace(".mnemonic", ".label")));
                }
            }
            Map<Character, List<String>> existing = new LinkedHashMap<>();
            for (JComponent section : existing(said)) {
                for (Component c : all(section)) {
                    if (c instanceof AbstractButton b && b.getMnemonic() != 0) {
                        existing.computeIfAbsent(Character.toUpperCase(
                                (char) b.getMnemonic()), k -> new ArrayList<>())
                                .add(b.getText());
                    }
                }
            }
            for (Map.Entry<Character, String> each : table.entrySet()) {
                List<String> clash = existing.getOrDefault(each.getKey(), List.of());
                out.append("| `").append(language).append("` | ").append(each.getKey())
                        .append(" | ").append(each.getValue()).append(" | ")
                        .append(clash.isEmpty() ? "—" : String.join(", ", clash))
                        .append(" |\n");
            }
        }
        out.append("\nTwo groups with the same letters in one window is itself a"
                + " collision: Swing picks\nwhichever registered first. The"
                + " Controller's groups therefore carry no access letters\n"
                + "of their own (the dialogs keep theirs), or distinct ones.\n\n");
    }

    /** How many stops a keyboard makes through each arrangement. */
    private static void stops(StringBuilder out, InterfaceText said) {
        out.append("## Keyboard stops, `").append(said.language()).append("`\n\n")
                .append("| arrangement | headings | controls | stops |\n|---|---:|---:|---:|\n");
        Map<String, JComponent> arrangements = new LinkedHashMap<>();
        arrangements.put("the Sun dialog's content", content(SolarTable.sun(), said));
        arrangements.put("the Sun group, card", group(SolarTable.sun(), said, true, false));
        arrangements.put("Solar System, both open, card", solarSystem(said, true, true, true));
        arrangements.put("Solar System, collapsed", solarSystem(said, false, false, true));
        for (Map.Entry<String, JComponent> each : arrangements.entrySet()) {
            int headings = 0;
            int controls = 0;
            for (Component c : all(each.getValue())) {
                if (!c.isFocusable() || !visibleUpTo(c, each.getValue())) {
                    continue;
                }
                if (c instanceof AbstractButton b && String.valueOf(b.getName())
                        .startsWith("heading.")) {
                    headings++;
                } else if (c instanceof AbstractButton || c instanceof JTextField
                        || c instanceof javax.swing.JComboBox || c instanceof JTable) {
                    controls++;
                }
            }
            out.append("| ").append(each.getKey()).append(" | ").append(headings)
                    .append(" | ").append(controls).append(" | ").append(headings + controls)
                    .append(" |\n");
        }
        out.append('\n');
    }

    // ---- the measurements and the mock-ups -----------------------------

    private static int[] measure(InterfaceText said) {
        int[] v = new int[10];
        v[0] = sized(wrap(content(SolarTable.sun(), said)),
                SolarTable.sun().preferredWidth() + 24, Integer.MAX_VALUE).getHeight();
        v[1] = sized(wrap(content(SolarTable.moon(), said)),
                SolarTable.moon().preferredWidth() + 24, Integer.MAX_VALUE).getHeight();
        v[2] = shell(List.of(group(SolarTable.sun(), said, true, false)),
                Integer.MAX_VALUE).getHeight();
        v[3] = shell(List.of(group(SolarTable.sun(), said, false, false)),
                Integer.MAX_VALUE).getHeight();
        v[4] = shell(List.of(group(SolarTable.moon(), said, true, false)),
                Integer.MAX_VALUE).getHeight();
        v[5] = shell(List.of(group(SolarTable.moon(), said, false, false)),
                Integer.MAX_VALUE).getHeight();
        v[6] = shell(List.of(solarSystem(said, true, true, true)),
                Integer.MAX_VALUE).getHeight();
        List<JComponent> collapsed = new ArrayList<>(existing(said));
        collapsed.add(solarSystem(said, false, false, true));
        ((CompanionSection) collapsed.get(3)).heading().doClick();
        v[7] = shell(collapsed, Integer.MAX_VALUE).getHeight();
        List<JComponent> sunOpen = new ArrayList<>(existing(said));
        sunOpen.add(solarSystem(said, true, false, true));
        v[8] = shell(sunOpen, Integer.MAX_VALUE).getHeight();
        List<JComponent> both = new ArrayList<>(existing(said));
        both.add(solarSystem(said, true, true, true));
        v[9] = shell(both, Integer.MAX_VALUE).getHeight();
        return v;
    }

    /** The table's height at representative row counts, and the row height. */
    private static String tableHeights(InterfaceText said) {
        SolarTableDialog.Content content = content(SolarTable.moon(), said);
        int row = content.table.getRowHeight();
        int header = content.table.getTableHeader().getPreferredSize().height;
        StringBuilder out = new StringBuilder("## A table's height by its rows\n\n")
                .append("Row height ").append(row).append(" px, header ").append(header)
                .append(" px, plus a sideways scroll bar; the rows are those a"
                        + " week of hours, a day of\nhours, a week of days, the"
                        + " sheets' four rows and the instant view give.\n\n")
                .append("| rows | table height |\n|---:|---:|\n");
        for (int rows : ROWS) {
            out.append("| ").append(rows).append(" | ").append(header + rows * row)
                    .append(" px |\n");
        }
        return out.append('\n').toString();
    }

    private static List<String> drawLight(InterfaceText said, String language)
            throws Exception {
        String stem = "controls-solar-" + language + "-";
        List<String> names = new ArrayList<>();
        write(sized(wrap(content(SolarTable.sun(), said)),
                SolarTable.sun().preferredWidth() + 24, Integer.MAX_VALUE),
                stem + "1-sun-dialog.png");
        names.add(stem + "1-sun-dialog.png");
        write(sized(wrap(ranged(SolarTable.moon(), said)),
                SolarTable.moon().preferredWidth() + 24, Integer.MAX_VALUE),
                stem + "2-moon-dialog-range.png");
        names.add(stem + "2-moon-dialog-range.png");
        write(shell(List.of(group(SolarTable.sun(), said, true, false)),
                Integer.MAX_VALUE), stem + "3-sun-group-card.png");
        names.add(stem + "3-sun-group-card.png");
        write(shell(List.of(group(SolarTable.sun(), said, false, false)),
                Integer.MAX_VALUE), stem + "4-sun-group-table.png");
        names.add(stem + "4-sun-group-table.png");
        write(shell(List.of(group(SolarTable.moon(), said, true, true)),
                Integer.MAX_VALUE), stem + "5-moon-group-range.png");
        names.add(stem + "5-moon-group-range.png");
        write(shell(List.of(solarSystem(said, true, true, true)),
                Integer.MAX_VALUE), stem + "6-both-open.png");
        names.add(stem + "6-both-open.png");
        write(shell(List.of(solarSystem(said, true, false, true)),
                Integer.MAX_VALUE), stem + "7-sun-open-moon-closed.png");
        names.add(stem + "7-sun-open-moon-closed.png");
        write(shell(List.of(solarSystem(said, false, true, true)),
                Integer.MAX_VALUE), stem + "8-sun-closed-moon-open.png");
        names.add(stem + "8-sun-closed-moon-open.png");
        List<JComponent> collapsed = new ArrayList<>(existing(said));
        collapsed.add(solarSystem(said, false, false, true));
        ((CompanionSection) collapsed.get(3)).heading().doClick();
        write(shell(collapsed, TALLEST), stem + "9-controller-collapsed-900.png");
        names.add(stem + "9-controller-collapsed-900.png");
        List<JComponent> sunOpen = new ArrayList<>(existing(said));
        sunOpen.add(solarSystem(said, true, false, true));
        write(shell(sunOpen, TALLEST), stem + "10-controller-sun-open-900.png");
        names.add(stem + "10-controller-sun-open-900.png");
        write(menuMockup(said, false), stem + "11-view-menu-today.png");
        names.add(stem + "11-view-menu-today.png");
        write(menuMockup(said, true), stem + "12-view-menu-proposed.png");
        names.add(stem + "12-view-menu-proposed.png");
        return names;
    }

    /** The View menu's rows, drawn with the popup's own border and background. */
    private static JPanel menuMockup(InterfaceText said, boolean proposed) {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(UIManager.getBorder("PopupMenu.border"));
        menu.setBackground(UIManager.getColor("PopupMenu.background"));
        menu.setOpaque(true);
        String[] keys = proposed
                ? new String[] {"menu.chartoptions", "menu.placeandtime", "menu.companion:x",
                        "menu.toolbar:x", "menu.inspector:x", "menu.ecliptic:x",
                        "submenu", "—", "menu.zoomIn", "menu.zoomOut"}
                : new String[] {"menu.chartoptions", "menu.placeandtime", "menu.companion:x",
                        "menu.toolbar:x", "menu.sun", "menu.moon", "menu.inspector:x",
                        "menu.ecliptic:x", "menu.sunchart:x", "menu.moonchart:x", "—",
                        "menu.zoomIn", "menu.zoomOut"};
        for (String key : keys) {
            if (key.equals("—")) {
                menu.add(new JPopupMenu.Separator());
            } else if (key.equals("submenu")) {
                JMenuItem item = new JMenuItem(study(said.language(), "solarsystem") + "   ▸");
                menu.add(item);
                JPanel sub = new JPanel();
                sub.setLayout(new BoxLayout(sub, BoxLayout.Y_AXIS));
                sub.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(0, 24, 4, 0),
                        UIManager.getBorder("PopupMenu.border")));
                sub.setOpaque(false);
                sub.add(new JCheckBoxMenuItem(said.say("menu.sunchart.label")));
                sub.add(new JCheckBoxMenuItem(said.say("menu.moonchart.label")));
                sub.add(new JPopupMenu.Separator());
                sub.add(new JMenuItem(said.say("menu.sun.label")));
                sub.add(new JMenuItem(said.say("menu.moon.label")));
                menu.add(sub);
            } else {
                boolean checkbox = key.endsWith(":x");
                String base = checkbox ? key.substring(0, key.length() - 2) : key;
                menu.add(checkbox ? new JCheckBoxMenuItem(said.say(base + ".label"))
                        : new JMenuItem(said.say(base + ".label")));
            }
        }
        JPanel shell = new JPanel(new BorderLayout());
        shell.add(menu, BorderLayout.WEST);
        return sized(shell, 300, Integer.MAX_VALUE);
    }

    // ---- the shell, laid out and drawn off screen ----------------------

    private static JPanel wrap(JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static JComponent leading(JComponent row) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(row, BorderLayout.WEST);
        holder.setAlignmentX(0f);
        holder.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                row.getPreferredSize().height + 4));
        return holder;
    }

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

    private static JPanel sized(JPanel shell, int width, int tallest) {
        shell.setSize(width, 2400);
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

    private static boolean visibleUpTo(Component component, Container root) {
        for (Component at = component; at != null && at != root; at = at.getParent()) {
            if (!at.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static List<Component> all(Component root) {
        List<Component> found = new ArrayList<>();
        found.add(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                found.addAll(all(child));
            }
        }
        return found;
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
