package juranometria.tool;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;

import juranometria.app.AboutDialog;
import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.app.ChartKeyboard;
import juranometria.app.ChartKeys;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.app.ChartSwitches;
import juranometria.app.ExportSheetDialog;
import juranometria.app.InspectorPanel;
import juranometria.app.SettingsDialog;
import juranometria.render.ChartOptions;
import juranometria.ui.Explain;
import juranometria.ui.Shortcuts;

/**
 * What every control the reader can operate says about itself
 * (Sprint 31, issue #311).
 *
 * <p>An audit rather than a count. A grep for {@code setToolTipText}
 * rises when somebody adds a tooltip and says nothing about the
 * control nobody thought of, so this walks the surfaces the
 * application actually builds - the toolbar, the menu bar, every
 * dialog, the Inspector, the module panel, the chart keyboard - and
 * reports each operable control with:
 *
 * <ul>
 *   <li>what a reader <strong>sees</strong> on it;</li>
 *   <li>what a reader <strong>hovers</strong> to read;</li>
 *   <li>what a reader <strong>hears</strong> after its name;</li>
 *   <li>and which of the three decisions was taken about it -
 *       explained by a tooltip, explained by one that follows the
 *       state, or deliberately left to its own visible words.</li>
 * </ul>
 *
 * <p>A control with no decision at all is listed as
 * <strong>UNDECIDED</strong>, which is what the gate test fails on.
 * That is the whole point: the number that matters is not how many
 * tooltips there are but whether anything has been left unconsidered.
 *
 * <p>The chart canvas is excluded by name and by decision: pointing
 * at it is answered by the Inspector and by the coordinates it
 * already carries, and a hover label over a star map is clutter over
 * the one surface this atlas exists to draw.
 */
public final class ControlExplanationStudyMain {

    private ControlExplanationStudyMain() {
    }

    /**
     * One control, as a reader meets it.
     *
     * <p>Public because the gate reads the same inventory this
     * document is written from. Two walkers would be two answers.
     */
    public record Control(String surface, String kind, String seen,
                          String hovered, String heard, String how) {
    }

    /**
     * What one walk of the application's surfaces found.
     *
     * <p>The rows and the components they were read from, in the
     * same order. The second list is the one that makes "each
     * control once" checkable: a row can only say what a control
     * says, and two rows saying the same thing are exactly what an
     * overlapping traversal produces.
     */
    public record Audit(List<Control> controls,
                        List<JComponent> components) {
    }

    /**
     * Every operable control the application builds, walked once.
     *
     * <p>Applies the shipped look and feel first, because a surface
     * is built out of the look and feel's own parts and an audit
     * under Metal would be an audit of a program nobody ships.
     */
    public static Audit audit() {
        juranometria.app.UiTheme.apply(false);
        List<Control> controls = new ArrayList<>();
        List<JComponent> visited = new ArrayList<>();
        for (Map.Entry<String, Component> surface : surfaces().entrySet()) {
            walk(surface.getKey(), surface.getValue(), controls, visited);
        }
        return new Audit(controls, visited);
    }

    /** How many surfaces the audit walks. */
    public static int surfaceCount() {
        return surfaces().size();
    }

    public static void main(String[] args) throws Exception {
        List<Control> controls = audit().controls();

        StringBuilder out = new StringBuilder();
        preface(out);
        table(out, controls);
        counted(out, controls);
        excluded(out);
        // The portable half: which control was decided about and how,
        // with this desktop's own word for the menu modifier reduced
        // to a token. What that word actually is, and what a tooltip
        // therefore shows a reader, is the machine's answer and is
        // written beside this (#315).
        System.out.print(PlatformEvidence.portable(out.toString()));

        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "What a control shows, as this desktop spells it",
                "Sprint 31, issues #311 and #315.");
        shortcuts(platform);
        quoted(platform, controls);
        PlatformEvidence.write(platform,
                "docs/studies/control-explanations/platform.md");
    }

    /** Every explanation that quotes a key, as a reader sees it here. */
    private static void quoted(StringBuilder out, List<Control> controls) {
        String modifier = Shortcuts.menuModifierText();
        out.append("## Every explanation that names a key\n\n");
        out.append("| surface | control | as a reader is shown it"
                + " |\n|---|---|---|\n");
        int named = 0;
        for (Control control : controls) {
            String hovered = control.hovered();
            if (hovered == null || !hovered.contains(modifier)) {
                continue;
            }
            named++;
            out.append("| ").append(control.surface()).append(" | ")
                    .append(cell(control.seen())).append(" | ")
                    .append(cell(hovered)).append(" |\n");
        }
        out.append(String.format(Locale.ROOT,
                "%n**%d explanations** name a key on this platform."
                        + " The report beside this one%nholds the same"
                        + " sentences with the modifier reduced to a"
                        + " token, so that what%nis pinned is which"
                        + " control quotes which switch rather than"
                        + " what this%ndesktop calls a key.%n",
                named));
    }

    private static void preface(StringBuilder out) {
        out.append("# What every control says about itself\n\n");
        out.append("Sprint 31, issue #311. Generated by"
                + " `make control-explanation-study` from the surfaces"
                + " the application builds - not from a list in a"
                + " document, and not from counting"
                + " `setToolTipText`.\n\n");
        out.append("Three decisions are possible for a control, and"
                + " every one of them is a decision:\n\n");
        out.append("- **hovered** - a tooltip that does not change;\n");
        out.append("- **dynamic** - a tooltip that follows what the"
                + " atlas can do next;\n");
        out.append("- **self-explanatory** - deliberately no tooltip,"
                + " because the words on the control are the whole"
                + " meaning.\n\n");
        out.append("A control with no decision is listed as"
                + " **UNDECIDED**. The gate fails on one.\n\n");
    }

    private static void table(StringBuilder out, List<Control> controls) {
        out.append("## Every operable control\n\n");
        out.append("| surface | control | seen | hovered | heard |"
                + " decided |\n");
        out.append("|---|---|---|---|---|---|\n");
        for (Control control : controls) {
            out.append("| ").append(control.surface())
                    .append(" | ").append(control.kind())
                    .append(" | ").append(cell(control.seen()))
                    .append(" | ").append(cell(control.hovered()))
                    .append(" | ").append(cell(control.heard()))
                    .append(" | ").append(control.how())
                    .append(" |\n");
        }
        out.append('\n');
    }

    private static void counted(StringBuilder out, List<Control> controls) {
        long hovered = controls.stream()
                .filter(c -> "hovered".equals(c.how())).count();
        long dynamic = controls.stream()
                .filter(c -> "dynamic".equals(c.how())).count();
        long own = controls.stream()
                .filter(c -> "self-explanatory".equals(c.how())).count();
        long undecided = controls.stream()
                .filter(c -> "UNDECIDED".equals(c.how())).count();
        out.append("## The audit\n\n");
        out.append(String.format(
                "**%d operable controls** across %d surfaces: **%d**"
                        + " hovered, **%d** dynamic, **%d** left to"
                        + " their own visible words, **%d"
                        + " undecided**.%n%n",
                controls.size(), surfaceCount(), hovered, dynamic,
                own, undecided));
        List<Control> sameWords = controls.stream()
                .filter(c -> c.hovered() != null)
                .filter(c -> c.hovered().equals(c.heard())).toList();
        out.append(String.format(
                "**%d** say the same words twice - a tooltip read back"
                        + " as a description. The seam refuses it, so"
                        + " this is zero or a finding.%n%n",
                sameWords.size()));
        for (Control control : sameWords) {
            out.append("- ").append(control.surface()).append(", ")
                    .append(control.kind()).append(" ")
                    .append(cell(control.seen())).append('\n');
        }
        if (!sameWords.isEmpty()) {
            out.append('\n');
        }
        List<Control> undecidedControls = controls.stream()
                .filter(c -> "UNDECIDED".equals(c.how())).toList();
        for (Control control : undecidedControls) {
            out.append("- **undecided:** ").append(control.surface())
                    .append(", ").append(control.kind()).append(" ")
                    .append(cell(control.seen())).append('\n');
        }
        if (!undecidedControls.isEmpty()) {
            out.append('\n');
        }
    }

    private static void shortcuts(StringBuilder out) {
        out.append("## Keys a tooltip may name\n\n");
        out.append("Every one of them from the registry that binds it,"
                + " so a tooltip cannot promise a stroke the"
                + " application does not answer.\n\n");
        out.append("| action | keys |\n|---|---|\n");
        for (Shortcuts.Shortcut shortcut : Shortcuts.all()) {
            out.append("| ").append(shortcut.label())
                    .append(" | `").append(shortcut.text())
                    .append("` |\n");
        }
        out.append("| the chart's own keyboard | `")
                .append(ChartKeys.prefixText())
                .append("` then a letter |\n\n");
    }

    private static void excluded(StringBuilder out) {
        out.append("## Named exclusions\n\n");
        out.append("- **The chart canvas.** Pointing at it is answered"
                + " by the Inspector and by the coordinates the page"
                + " already carries. A hover label over a star map is"
                + " clutter over the one surface the atlas exists to"
                + " draw.\n");
        out.append("- **Labels, headings and readouts.** They are"
                + " text, not controls; what they say is already what"
                + " they say.\n");
        out.append("- **The platform's own file chooser and message"
                + " boxes.** The desktop builds them and explains"
                + " them.\n");
        out.append("- **The parts the look and feel builds inside a"
                + " control.** A"
                + " scroll bar's arrows and a combo box's button"
                + " belong to the list or the field they sit on,"
                + " which is listed above.\n");
        out.append("- **Study and mock-up rigs under"
                + " `src/juranometria/tool`.** No reader ever presses"
                + " one.\n");
    }

    private static String cell(String text) {
        if (text == null) {
            return "&mdash;";
        }
        String flat = text.replace("|", "\\|").replaceAll("\\s+", " ")
                .trim();
        return flat.length() <= 90 ? flat
                : flat.substring(0, 87) + "...";
    }

    // ---- the surfaces the application builds ------------------------

    private static Map<String, Component> surfaces() {
        Map<String, Component> surfaces = new LinkedHashMap<>();
        juranometria.ui.ChartViewController navigation =
                new juranometria.ui.ChartViewController();
        juranometria.ui.SearchField search =
                new juranometria.ui.SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
        surfaces.put("Toolbar", new juranometria.ui.AtlasToolbar(
                navigation, search,
                new juranometria.ui.InspectorToggle(), "0.0.0",
                () -> { }, new juranometria.chart.SelectionMode()));
        surfaces.put("Menu bar", AppMenuBar.create(navigation, () -> { },
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }));
        surfaces.put("Chart Options",
                ChartOptionsDialog.contentForStudy(options()));
        surfaces.put("Place and Time", placeAndTime());
        surfaces.put("Export Chart Sheet",
                ExportSheetDialog.contentForStudy());
        surfaces.put("Settings", SettingsDialog.contentForStudy());
        surfaces.put("About", AboutDialog.compactContentForStudy());
        surfaces.put("About, notices",
                AboutDialog.noticesContentForStudy());
        surfaces.put("Inspector", inspector());
        surfaces.put("On this page", onThisPage());
        surfaces.put("Chart keyboard", ChartKeyboard.of(
                ChartSwitches.of(options(), new ChartSwitches.Ecliptic() {
                    @Override
                    public boolean showing() {
                        return false;
                    }

                    @Override
                    public void toggle() {
                    }
                }, new ChartSwitches.ObserverLines() {
                    @Override
                    public boolean meridianShowing() {
                        return false;
                    }

                    @Override
                    public boolean horizonShowing() {
                        return false;
                    }

                    @Override
                    public void showing(boolean line, boolean horizon) {
                    }
                })));
        return surfaces;
    }

    private static ChartOptionsController options() {
        return new ChartOptionsController(new ChartOptionsStore() {
            @Override
            public ChartOptions load() {
                return ChartOptions.DEFAULTS;
            }

            @Override
            public void save(ChartOptions next) {
            }
        });
    }

    private static Component placeAndTime() {
        juranometria.meridian.MeridianModule module =
                new juranometria.meridian.MeridianModule(
                        new juranometria.sky.Observer(59.913, 10.752,
                                java.time.Instant.parse(
                                        "2026-03-20T21:33:00Z")));
        return juranometria.ui.placeandtime.PlaceAndTimeDialog
                .contentForStudy(module,
                        new juranometria.ui.placeandtime.PlaceStore() {
                            @Override
                            public juranometria.sky.Observer load(
                                    java.time.Instant instant) {
                                return module.observer().at(instant);
                            }

                            @Override
                            public void save(double latitude,
                                             double eastLongitude) {
                            }

                            @Override
                            public boolean remembered() {
                                return true;
                            }

                            @Override
                            public void flush() {
                            }
                        });
    }

    private static Component inspector() {
        juranometria.chart.SelectionModel selection =
                new juranometria.chart.SelectionModel();
        InspectorPanel panel = new InspectorPanel(selection,
                () -> Atlas.assembler().assemble(
                        juranometria.chart.ChartViewState.DEFAULT,
                        900, 700),
                () -> ChartOptions.DEFAULTS, chosen -> { });
        juranometria.chart.WorkingSelection working =
                new juranometria.chart.WorkingSelection();
        panel.showWorkingSet(working, () ->
                juranometria.page.PageContents.EMPTY);
        // One member, so the row a reader meets is in the inventory
        // rather than only the empty case.
        working.add("M 31");
        return panel;
    }

    private static Component onThisPage() {
        juranometria.ui.ChartComponent chart =
                new juranometria.ui.ChartComponent(Atlas.assembler());
        chart.setSize(900, 700);
        chart.setViewState(juranometria.chart.ChartViewState.DEFAULT);
        juranometria.ui.ChartModuleHost host =
                new juranometria.ui.ChartModuleHost(chart,
                        new juranometria.chart.SelectionModel(),
                        request -> { });
        return host.attach(
                new juranometria.ui.onthispage.OnThisPageModule())
                .panel();
    }

    // ---- walking one surface ----------------------------------------

    private static void walk(String surface, Component root,
                             List<Control> found,
                             List<JComponent> visited) {
        if (root instanceof JComponent component && operable(component)) {
            found.add(read(surface, component));
            visited.add(component);
        }
        if (root instanceof JMenuBar bar) {
            for (int i = 0; i < bar.getMenuCount(); i++) {
                walk(surface, bar.getMenu(i), found, visited);
            }
            return;
        }
        if (root instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                if (menu.getItem(i) != null) {
                    walk(surface, menu.getItem(i), found, visited);
                }
            }
            return;
        }
        // No branch for a tabbed pane. Its tab components are its
        // own children, so the general walk below reaches them - and
        // walking them here as well reached every control on every
        // tab twice, which inflated the inventory and every count
        // taken from it (review, #311).
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                walk(surface, child, found, visited);
            }
        }
    }

    /**
     * Whether a reader can operate this thing.
     *
     * <p>Buttons of every kind, the boxes and fields they type or
     * choose in, the lists and tables they walk, and the tab strips
     * they move between. Labels, panels, scroll panes and struts are
     * text and furniture: they answer to nothing.
     */
    private static boolean operable(JComponent component) {
        boolean itself = component instanceof AbstractButton
                || component instanceof JComboBox<?>
                || component instanceof JTextComponent
                || component instanceof JList<?>
                || component instanceof JTable
                || component instanceof JTabbedPane;
        return itself && !insideAnotherControl(component);
    }

    /**
     * Whether this is a part of a control the audit already lists.
     *
     * <p>A scroll bar has two arrows, a combo box has one, and a
     * reader operating them is operating the list or the field they
     * belong to. Explaining them separately would be explaining the
     * look and feel rather than the atlas - and there are fifty of
     * them, which would bury the controls that matter.
     */
    private static boolean insideAnotherControl(JComponent component) {
        for (Container above = component.getParent(); above != null;
                above = above.getParent()) {
            if (above instanceof javax.swing.JScrollBar
                    || above instanceof JComboBox<?>) {
                return true;
            }
        }
        return false;
    }

    private static Control read(String surface, JComponent component) {
        Explain.How how = Explain.how(component);
        String kind = component.getClass().getSimpleName();
        if (kind.isEmpty()) {
            kind = component.getClass().getSuperclass().getSimpleName();
        }
        return new Control(surface, kind, seen(component),
                component.getToolTipText(),
                component.getAccessibleContext()
                        .getAccessibleDescription(),
                how == null ? "UNDECIDED"
                        : switch (how) {
                            case HOVERED -> "hovered";
                            case DYNAMIC -> "dynamic";
                            case SELF_EXPLANATORY -> "self-explanatory";
                        });
    }

    private static String seen(JComponent component) {
        if (component instanceof AbstractButton button) {
            String text = button.getText();
            return text == null || text.isBlank()
                    ? "(icon only)" : text;
        }
        if (component instanceof JTextComponent text) {
            return text.isEditable() ? "(a field)" : "(read-only text)";
        }
        if (component instanceof JTabbedPane tabs) {
            StringBuilder titles = new StringBuilder();
            for (int i = 0; i < tabs.getTabCount(); i++) {
                titles.append(i == 0 ? "" : ", ").append(tabs.getTitleAt(i));
            }
            return titles.toString();
        }
        String name = component.getAccessibleContext()
                .getAccessibleName();
        return name == null ? "(no name)" : "(" + name + ")";
    }
}
