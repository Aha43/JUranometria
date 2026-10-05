package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.app.Atlas;
import juranometria.app.AtlasChrome;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsControls;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.chart.SelectionMode;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartStructure;
import juranometria.search.SearchResult;
import juranometria.sky.Observer;
import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.InspectorToggle;
import juranometria.ui.SceneAssembler;
import juranometria.ui.SearchField;
import juranometria.ui.ZoomLock;
import juranometria.ui.companion.CompanionPlacement;
import juranometria.ui.companion.CompanionSection;
import juranometria.ui.companion.CompanionStore;
import juranometria.ui.companion.CompanionWindow;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.ui.language.SkyLanguageStore;
import juranometria.ui.placeandtime.PlaceAndTimePanel;
import juranometria.ui.placeandtime.PlaceStore;

/**
 * The chart toolbar's controls in the Controls companion, and the
 * chart window without its toolbar, measured before either is built
 * (Sprint 41, issue #449).
 *
 * <p>Every mock-up holds the <em>production</em> toolbar's own
 * controls: a bar is composed through {@link AtlasChrome}, the
 * application's own seam, from a language written to a preference
 * node, and its components are lifted out into the arrangement being
 * compared. So every word, icon, accessible name and explanation is
 * the one a reader gets today. The section titles the arrangements
 * need are study words, and are marked as such. Two lists are drawn
 * by the study from production answers - search results from the
 * production index, and the Emphasis menu's rows from the production
 * words - because the production popups are windows, and this runs
 * without a display.
 *
 * <p>Painted off screen, as the contract's headless process requires;
 * the implementation's photographs will be of real windows. Standard
 * output is the portable report: the inventory, the geometry, the
 * placement arithmetic and the keyboard stops. How tall and wide the
 * words are drawn is this machine's, in {@code platform.md}. The
 * images carry the {@code controls-} prefix: widget inspection
 * imagery, held to substance and not to bytes.
 */
public final class CleanChartStudyMain {

    static final Path DIR = Path.of("docs/studies/clean-chart-controls");

    /** The companion's default width (CompanionWindow.DEFAULT_WIDTH). */
    static final int WIDTH = CompanionWindow.DEFAULT_WIDTH;

    /** The tallest the companion opens (CompanionWindow's own cap). */
    static final int TALLEST = 900;

    static final String[] LANGUAGES = {"en", "nb-NO"};

    /** The chart's own preferred size, as the application packs it. */
    private static Dimension chartSize;

    private CleanChartStudyMain() {
    }

    /** Words the arrangements need that production does not have. */
    private static String study(String language, String what) {
        boolean en = language.equals("en");
        return switch (what) {
            case "chartcontrols" -> en ? "Chart controls" : "Kartkontroller";
            case "find" -> en ? "Find" : "Finn";
            case "navigate" -> en ? "Navigate" : "Naviger";
            case "stars" -> en ? "Stars" : "Stjerner";
            case "work" -> en ? "Work with the chart" : "Arbeid med kartet";
            case "toolbar" -> en ? "Chart toolbar (wrapped)" : "Kartverktøylinje (brutt)";
            default -> throw new IllegalArgumentException(what);
        };
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIR);
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        StringBuilder report = new StringBuilder();
        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "The toolbar's controls in the companion, as this desktop draws them",
                "Sprint 41, issue #449.");
        List<String> images = new ArrayList<>();
        try {
            SwingUtilities.invokeAndWait(() -> UIManager.put("defaultFont", null));
            preface(report);
            onEdt(() -> {
                juranometria.app.UiTheme.apply(false);
                inventory(report, InterfaceText.forLanguage("en"));
                geometry(report, platform);
                return null;
            });
            platform.append("## Heights and widths\n\n")
                    .append("At the companion's default width, ").append(WIDTH)
                    .append(" px, in light appearance. \"Narrowest\" is the"
                            + " narrowest companion in which no\nbutton, field"
                            + " or label of the arrangement is drawn narrower"
                            + " than it asks; \"with both\" adds Place\nand Time"
                            + " and Chart Options (Deep sky collapsed) above it,"
                            + " as the companion holds them today.\n\n")
                    .append("| language | arrangement | alone | with both |"
                            + " with both, Chart Options collapsed | narrowest"
                            + " | fits 900 with both |\n")
                    .append("|---|---|---:|---:|---:|---:|---|\n");
            for (String language : LANGUAGES) {
                InterfaceText said = InterfaceText.forLanguage(language);
                onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    for (Arrangement arrangement : Arrangement.values()) {
                        int alone = shell(List.of(section(arrangement, said, State.IDLE)),
                                Integer.MAX_VALUE).getHeight();
                        int both = shell(withBoth(arrangement, said, State.IDLE),
                                Integer.MAX_VALUE).getHeight();
                        List<JComponent> collapsed = withBoth(arrangement, said,
                                State.IDLE);
                        ((CompanionSection) collapsed.get(1)).heading().doClick();
                        int optionsCollapsed = shell(collapsed, Integer.MAX_VALUE)
                                .getHeight();
                        int narrowest = narrowest(() -> List.of(
                                section(arrangement, said, State.IDLE)));
                        platform.append("| `").append(language).append("` | ")
                                .append(arrangement.title(said)).append(" | ")
                                .append(alone).append(" | ").append(both)
                                .append(" | ").append(optionsCollapsed)
                                .append(" | ").append(narrowest).append(" | ")
                                .append(both <= TALLEST ? "fits" : "scrolls")
                                .append(" |\n");
                    }
                    return null;
                });
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    return drawLight(said, language);
                }));
                images.addAll(onEdt(() -> {
                    juranometria.app.UiTheme.apply(true);
                    List<String> dark = new ArrayList<>();
                    for (Arrangement arrangement : new Arrangement[] {
                            Arrangement.ROWS, Arrangement.SECTIONS}) {
                        String name = "controls-clean-chart-" + language + "-"
                                + arrangement.number + "-" + arrangement.stem
                                + "-dark.png";
                        write(shell(List.of(section(arrangement, said, State.IDLE)),
                                Integer.MAX_VALUE), name);
                        dark.add(name);
                    }
                    return dark;
                }));
                onEdt(() -> {
                    juranometria.app.UiTheme.apply(false);
                    focus(report, said);
                    return null;
                });
            }
            placement(report);
            images.addAll(placementImages());
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
        out.append("# The chart toolbar's controls in the companion, measured before they move\n\n")
                .append("Sprint 41, issue #449. Generated by\n"
                        + "`juranometria.tool.CleanChartStudyMain` from the"
                        + " compiled application.\n\n")
                .append("Every mock-up holds the production toolbar's own"
                        + " controls, lifted out of a bar\ncomposed through"
                        + " `AtlasChrome` from a stored language, and the"
                        + " production companion\nsection heading. The"
                        + " section titles are study words, marked below."
                        + " How tall and wide\nthe words are drawn is one"
                        + " machine's answer, in `platform.md` beside"
                        + " this.\n\n");
    }

    // ---- the production pieces ---------------------------------------

    /** What a mock-up shows the instrument doing. */
    enum State {
        IDLE, RESULTS, NO_MATCH, ZOOM_END, INSPECTOR_UNAVAILABLE,
        INSPECTOR_SHOWING, EMPHASES
    }

    /** The three arrangements compared. */
    enum Arrangement {
        WRAPPED(1, "wrapped"), ROWS(2, "rows"), SECTIONS(3, "sections");

        final int number;
        final String stem;

        Arrangement(int number, String stem) {
            this.number = number;
            this.stem = stem;
        }

        String title(InterfaceText said) {
            return switch (this) {
                case WRAPPED -> "1. the toolbar, wrapped";
                case ROWS -> "2. one Chart controls section, rows";
                case SECTIONS -> "3. Find, Navigate, Stars, Work with the chart";
            };
        }
    }

    /** A production bar and what it was built over, lifted apart. */
    private record Bar(AtlasToolbar toolbar, SearchField search,
                       ChartViewController controller, InspectorToggle inspector,
                       ChartComponent chart, Map<String, Component> parts,
                       JLabel readout) {
    }

    private static SceneAssembler assembler;

    private static SceneAssembler assembler() {
        if (assembler == null) {
            assembler = Atlas.assembler();
        }
        return assembler;
    }

    /**
     * The bar the application composes, in the stated language, with
     * every optional part: the Inspector toggle, a version, a way out,
     * Accumulate, the zoom lock and Emphasis. Built the way
     * {@code ToolbarSheetMain} builds it - from a language written to a
     * preference node and read once - so a defaulting constructor
     * cannot be photographed.
     */
    private static Bar bar(InterfaceText said, State state) {
        Preferences node = Preferences.userRoot().node(
                "juranometria-study-clean-chart-" + System.nanoTime());
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            store.save(store.choice(Atlas.languages()).withInterface(said.language()));
            SkyLanguageSession session = SkyLanguageSession.begin(store,
                    Atlas.languages());
            ChartViewController controller = new ChartViewController(
                    assembler()::fits);
            InspectorToggle inspector = new InspectorToggle();
            boolean[] showing = {false};
            inspector.bind(() -> inspector.report(showing[0] = !showing[0]),
                    () -> state != State.INSPECTOR_UNAVAILABLE);
            AtlasChrome chrome = AtlasChrome.of(session, controller,
                    Atlas.search(), assembler(), inspector, "2.0.0", () -> { },
                    new SelectionMode(), new ZoomLock());
            ChartComponent chart = new ChartComponent(assembler(), PageText.in(said));
            controller.onChange(chart::setViewState);
            chrome.toolbar().attachEmphasis(chart);
            if (state == State.ZOOM_END) {
                while (controller.canZoomIn()) {
                    controller.zoomIn();
                }
                while (controller.canIncreaseMagnitudeLimit()) {
                    controller.increaseMagnitudeLimit();
                }
            }
            if (state == State.INSPECTOR_UNAVAILABLE) {
                inspector.report(false);
            }
            if (state == State.INSPECTOR_SHOWING) {
                inspector.toggle();
            }
            if (state == State.EMPHASES) {
                chart.toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
                chart.toggleEmphasis(ChartStructure.CONSTELLATION_FIGURES);
            }
            if (state == State.RESULTS) {
                chrome.searchField().setText("NGC");
            }
            if (state == State.NO_MATCH) {
                chrome.searchField().setText("qqzzx");
            }
            Map<String, Component> parts = new LinkedHashMap<>();
            JLabel readout = null;
            for (Component component : chrome.toolbar().getComponents()) {
                if (component instanceof JLabel label) {
                    String name = label.getAccessibleContext().getAccessibleName();
                    if (name != null && name.equals(said.say("toolbar.version.a11y",
                            "2.0.0"))) {
                        parts.put("version", label);
                    } else {
                        readout = label;
                        parts.put("readout", label);
                    }
                } else if (component instanceof SearchField) {
                    parts.put("search", component);
                } else if (component instanceof AbstractButton button) {
                    parts.put(key(button, said), button);
                }
            }
            return new Bar(chrome.toolbar(), chrome.searchField(), controller,
                    inspector, chart, parts, readout);
        } finally {
            try {
                node.removeNode();
            } catch (java.util.prefs.BackingStoreException leaving) {
                // An empty node is a blemish, not a failure.
            }
        }
    }

    /** Which production control a button is, by the name it is spoken as. */
    private static String key(AbstractButton button, InterfaceText said) {
        String name = button.getAccessibleContext().getAccessibleName();
        for (String id : new String[] {"zoomIn", "zoomOut", "zoomLock",
                "fewerStars", "moreStars", "reset", "inspector", "accumulate",
                "emphasis", "exit"}) {
            if (said.say("toolbar." + id + ".a11y").equals(name)) {
                return id;
            }
        }
        throw new IllegalStateException("an unclassified control on the bar: "
                + name);
    }

    /** A row of lifted controls, drawn as the toolbar draws them. */
    private static JToolBar row(Bar bar, String... keys) {
        JToolBar row = new JToolBar();
        row.setFloatable(false);
        row.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        for (String key : keys) {
            if (key.equals("|")) {
                row.addSeparator();
            } else {
                Component part = bar.parts().get(key);
                if (part == null) {
                    throw new IllegalStateException("no such part: " + key);
                }
                row.add(part);
            }
        }
        for (Component child : row.getComponents()) {
            if (child instanceof AbstractButton) {
                child.setFocusable(true);
            }
        }
        return row;
    }

    /** The search field on a row of its own, as wide as the section. */
    private static JComponent searchRow(Bar bar) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        SearchField field = bar.search();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    /** The readout as a quiet line, subdued like the toolbar's version. */
    private static JComponent readoutRow(Bar bar) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBorder(BorderFactory.createEmptyBorder(2, 12, 6, 10));
        JLabel readout = bar.readout();
        readout.setBorder(BorderFactory.createEmptyBorder());
        readout.putClientProperty("FlatLaf.styleClass", "small");
        readout.putClientProperty("FlatLaf.style",
                "foreground: $Label.disabledForeground");
        row.add(readout, BorderLayout.WEST);
        return row;
    }

    /**
     * The study's own copy of what a popup would show beneath the
     * field or the button: the production rows, in a panel drawn with
     * the popup's own border and background. A {@code JPopupMenu} is a
     * window, and begins invisible, so it cannot be painted here.
     */
    private static JComponent popupFor(Bar bar, State state, InterfaceText said) {
        if (state == State.RESULTS) {
            JPanel menu = popupPanel();
            List<SearchResult> results = Atlas.search().search("NGC");
            for (SearchResult result : results) {
                menu.add(new JMenuItem(result.label().equals(result.identity())
                        ? result.label()
                        : result.label() + " · " + result.identity()));
            }
            return menu;
        }
        if (state == State.NO_MATCH) {
            JPanel menu = popupPanel();
            JMenuItem item = new JMenuItem(said.say("search.nomatch.label"));
            item.setEnabled(false);
            menu.add(item);
            return menu;
        }
        if (state == State.EMPHASES) {
            JPanel menu = popupPanel();
            JMenuItem normal = new JMenuItem(said.say("emphasis.normal"));
            normal.setEnabled(!bar.chart().emphasizedSet().isEmpty());
            menu.add(normal);
            menu.add(new JPopupMenu.Separator());
            for (ChartStructure structure : ChartStructure.values()) {
                JCheckBoxMenuItem item = new JCheckBoxMenuItem(
                        said.say("emphasis." + structure.token()),
                        bar.chart().emphasizedSet().contains(structure));
                item.setEnabled(bar.chart().emphasizedSet().contains(structure)
                        || bar.chart().emphasisAvailable(structure));
                menu.add(item);
            }
            return menu;
        }
        return null;
    }

    private static JPanel popupPanel() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(UIManager.getBorder("PopupMenu.border"));
        menu.setBackground(UIManager.getColor("PopupMenu.background"));
        menu.setOpaque(true);
        return menu;
    }

    /** A popup placed beneath its anchor, inside the section's own panel. */
    private static JComponent withPopup(JComponent content, JComponent popup,
                                        int indent) {
        if (popup == null) {
            return content;
        }
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(content);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(0, indent, 6, 0));
        holder.add(popup, BorderLayout.WEST);
        stack.add(holder);
        return stack;
    }

    /** One arrangement of the toolbar's controls, as a companion section. */
    private static JComponent section(Arrangement arrangement, InterfaceText said,
                                      State state) {
        Bar bar = bar(said, state);
        JComponent popup = popupFor(bar, state, said);
        String language = said.language();
        return switch (arrangement) {
            case WRAPPED -> {
                JPanel wrapped = new JPanel(new WrapLayout(6, 4));
                wrapped.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                for (Component component : bar.toolbar().getComponents()) {
                    if (component instanceof Box.Filler) {
                        continue;
                    }
                    if (component instanceof JToolBar.Separator) {
                        continue;
                    }
                    wrapped.add(component);
                }
                yield CompanionSection.forStudy("toolbar",
                        study(language, "toolbar"),
                        withPopup(wrapped, popup, 10), said);
            }
            case ROWS -> {
                JPanel rows = new JPanel();
                rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
                rows.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 4));
                rows.add(leading(row(bar, "zoomIn", "zoomOut", "zoomLock", "|",
                        "fewerStars", "moreStars", "|", "reset")));
                rows.add(leading(row(bar, "inspector", "|", "accumulate", "|",
                        "emphasis")));
                rows.add(searchRow(bar));
                rows.add(readoutRow(bar));
                int indent = popup == null ? 0 : state == State.EMPHASES ? 100 : 10;
                yield CompanionSection.forStudy("chartcontrols",
                        study(language, "chartcontrols"),
                        withPopup(rows, popup, indent), said);
            }
            case SECTIONS -> {
                JPanel sections = new JPanel();
                sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
                JPanel find = new JPanel();
                find.setLayout(new BoxLayout(find, BoxLayout.Y_AXIS));
                find.add(searchRow(bar));
                sections.add(CompanionSection.forStudy("find", study(language, "find"),
                        state == State.RESULTS || state == State.NO_MATCH
                                ? withPopup(find, popup, 10) : find, said));
                JPanel navigate = new JPanel();
                navigate.setLayout(new BoxLayout(navigate, BoxLayout.Y_AXIS));
                navigate.add(leading(row(bar, "zoomIn", "zoomOut", "zoomLock", "|",
                        "reset")));
                navigate.add(readoutRow(bar));
                sections.add(CompanionSection.forStudy("navigate",
                        study(language, "navigate"), navigate, said));
                sections.add(CompanionSection.forStudy("stars",
                        study(language, "stars"),
                        leading(row(bar, "fewerStars", "moreStars")), said));
                JComponent work = leading(row(bar, "inspector", "|", "accumulate",
                        "|", "emphasis"));
                sections.add(CompanionSection.forStudy("work", study(language, "work"),
                        state == State.EMPHASES ? withPopup(work, popup, 100) : work,
                        said));
                yield sections;
            }
        };
    }

    /** A row kept to its own preferred width, against the leading edge. */
    private static JComponent leading(JComponent row) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(row, BorderLayout.WEST);
        return holder;
    }

    /** The arrangement beneath Place and Time and Chart Options. */
    private static List<JComponent> withBoth(Arrangement arrangement,
                                             InterfaceText said, State state) {
        return List.of(placeAndTime(said), chartOptions(said),
                section(arrangement, said, state));
    }

    private static JComponent placeAndTime(InterfaceText said) {
        PlaceAndTimeSheetMain.State oslo = PlaceAndTimeSheetMain.STATES.stream()
                .filter(state -> state.name().equals("oslo"))
                .findFirst().orElseThrow();
        MeridianModule module = new MeridianModule(new Observer(oslo.latitude(),
                oslo.eastLongitude(), PlaceAndTimeSheetMain.WHEN));
        module.showing(oslo.meridian(), oslo.horizon(), oslo.zenith());
        Preferences place = Preferences.userRoot().node(
                "juranometria-study-cc-place-" + System.nanoTime());
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

    /** Chart Options as the companion holds it: the production groups. */
    private static JComponent chartOptions(InterfaceText said) {
        Preferences node = Preferences.userRoot().node(
                "juranometria-study-cc-options-" + System.nanoTime());
        try {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            JComponent groups = new ChartOptionsControls(controller, () -> false,
                    said).inCompanion(CompanionStore.forNode(node), said);
            return CompanionSection.forStudy("chartoptions",
                    said.say("chartoptions.title"), groups, said);
        } finally {
            try {
                node.removeNode();
            } catch (java.util.prefs.BackingStoreException leaving) {
                // An empty node is a blemish, not a failure.
            }
        }
    }

    // ---- the inventory and the geometry --------------------------------

    /** What each control on the bar is, read from the bar itself. */
    private static void inventory(StringBuilder out, InterfaceText said) {
        Bar bar = bar(said, State.IDLE);
        out.append("## The toolbar, inventoried\n\n")
                .append("Read from the bar `AtlasChrome` composes, in `en`, left"
                        + " to right. The\n\"class\" column is the study's:"
                        + " an **action** changes shared state once, a\n"
                        + "**state** control shows and sets a shared switch, a"
                        + " **readout** says what the\nchart is, and"
                        + " **furniture** belongs to the application rather"
                        + " than the chart.\nThe authority is the object a"
                        + " second presentation would follow.\n\n")
                .append("| # | control | kind | focusable | class | authority"
                        + " | menu or key | remembered |\n")
                .append("|---:|---|---|---|---|---|---|---|\n");
        int n = 0;
        for (Component component : bar.toolbar().getComponents()) {
            if (component instanceof Box.Filler || component instanceof JToolBar.Separator) {
                continue;
            }
            n++;
            String name = component instanceof JComponent j
                    && j.getAccessibleContext() != null
                    ? j.getAccessibleContext().getAccessibleName() : "";
            String key = component instanceof SearchField ? "search"
                    : component instanceof JLabel
                            ? (component == bar.readout() ? "readout" : "version")
                            : key((AbstractButton) component, said);
            String[] classified = CLASSES.get(key);
            if (classified == null) {
                throw new IllegalStateException("unclassified: " + key);
            }
            out.append("| ").append(n).append(" | ").append(name).append(" | ")
                    .append(component.getClass().getSimpleName()).append(" | ")
                    .append(component.isFocusable() ? "yes" : "no");
            for (String cell : classified) {
                out.append(" | ").append(cell);
            }
            out.append(" |\n");
        }
        out.append("\n**").append(n).append(" controls on the bar.** Every one"
                + " is classified above; a control the study has not\nclassified"
                + " fails this generator. A label answers \"focusable\" with"
                + " Swing's default, yes,\nbut the layout focus policy skips a"
                + " component with no focused-key bindings, so neither\nlabel is"
                + " a keyboard stop. The Emphasis button answered **no** when"
                + " this was first measured\n(#449): it was attached after the"
                + " bar re-asserted its buttons' focusability, and the look\nand"
                + " feel took it away again - the one control on the bar a"
                + " keyboard could not reach.\nRepaired in #450, ruled on #449;"
                + " the row above reads what the bar answers now.\n\n");
    }

    /** class, authority, menu or key, remembered - per control. */
    private static final Map<String, String[]> CLASSES = Map.ofEntries(
            Map.entry("zoomIn", new String[] {"action", "`ChartViewController`",
                    "View ▸ Zoom In, ⌘/Ctrl =", "no"}),
            Map.entry("zoomOut", new String[] {"action", "`ChartViewController`",
                    "View ▸ Zoom Out, ⌘/Ctrl −", "no"}),
            Map.entry("zoomLock", new String[] {"state", "`ZoomLock`", "none",
                    "yes, `zoomLocked`"}),
            Map.entry("fewerStars", new String[] {"action", "`ChartViewController`",
                    "none", "no"}),
            Map.entry("moreStars", new String[] {"action", "`ChartViewController`",
                    "none", "no"}),
            Map.entry("reset", new String[] {"action",
                    "`ChartViewController.reset` and the search field's clearing",
                    "none", "no"}),
            Map.entry("inspector", new String[] {"state", "`InspectorToggle`",
                    "View ▸ Inspector, ⌘/Ctrl I", "no"}),
            Map.entry("accumulate", new String[] {"state", "`SelectionMode`",
                    "none; the platform's add-to-selection modifier", "no"}),
            Map.entry("emphasis", new String[] {"state",
                    "`ChartComponent` (`toggleEmphasis`, `emphasizedSet`,"
                            + " `emphasisAvailable`)", "none", "no"}),
            Map.entry("search", new String[] {"action, with a local query",
                    "`LocalSearch`, `SearchNavigation`, the selection", "none",
                    "no"}),
            Map.entry("readout", new String[] {"readout", "`ChartViewController`",
                    "the title block on the chart", "no"}),
            Map.entry("version", new String[] {"furniture", "`AppInfo`",
                    "Help ▸ About", "no"}),
            Map.entry("exit", new String[] {"furniture", "`AppShutdown`",
                    "the close box; the platform's Quit", "no"}));

    /**
     * The chart window's layout with and without its toolbar, laid out
     * off screen over the production components: the bar, the chart
     * at its own preferred size, nothing else. What the real window
     * does was reproduced on a display and is quoted in the decision
     * record; this is the arithmetic, portable.
     */
    private static void geometry(StringBuilder out, StringBuilder platform) {
        Bar bar = bar(InterfaceText.forLanguage("en"), State.IDLE);
        chartSize = bar.chart().getPreferredSize();
        JPanel frame = new JPanel(new BorderLayout());
        frame.add(bar.toolbar(), BorderLayout.NORTH);
        frame.add(bar.chart(), BorderLayout.CENTER);
        int barHeight = bar.toolbar().getPreferredSize().height;
        frame.setSize(chartSize.width, chartSize.height + barHeight);
        layOut(frame);
        Rectangle shown = bar.chart().getBounds();
        var stateBefore = bar.controller().state();
        bar.toolbar().setVisible(false);
        layOut(frame);
        Rectangle hidden = bar.chart().getBounds();
        var stateAfter = bar.controller().state();
        bar.toolbar().setVisible(true);
        layOut(frame);
        Rectangle again = bar.chart().getBounds();
        out.append("## The chart window without its toolbar\n\n")
                .append("The window's layout is `BorderLayout`: the bar north,"
                        + " the chart centre, the\nInspector east. Hiding the"
                        + " bar gives the chart the bar's own height and"
                        + " nothing\nelse; the window keeps its size, and the"
                        + " view state - centre, field, magnitude,\ntarget,"
                        + " projection - is the controller's and is not"
                        + " consulted by the layout.\n\n")
                .append("| | chart x, y | chart width × height | view state |\n")
                .append("|---|---|---|---|\n")
                .append("| toolbar shown | ").append(shown.x).append(", ")
                .append(shown.y).append(" | ").append(shown.width).append(" × ")
                .append(shown.height).append(" | ").append(stateBefore.equals(
                        stateAfter) ? "unchanged" : "CHANGED").append(" |\n")
                .append("| toolbar hidden | ").append(hidden.x).append(", ")
                .append(hidden.y).append(" | ").append(hidden.width).append(" × ")
                .append(hidden.height).append(" | ").append(stateBefore.equals(
                        stateAfter) ? "unchanged" : "CHANGED").append(" |\n")
                .append("| shown again | ").append(again.x).append(", ")
                .append(again.y).append(" | ").append(again.width).append(" × ")
                .append(again.height).append(" | ").append(
                        again.equals(shown) ? "as before" : "DIFFERENT").append(" |\n\n")
                .append("The chart grows by exactly the bar's height and"
                        + " moves up by it; the bar's height is\nthis"
                        + " machine's, in `platform.md`.\n\n");
        try {
            chartWindowImage(shown, hidden, barHeight);
        } catch (java.io.IOException trouble) {
            throw new IllegalStateException(trouble);
        }
        platform.append("## The bar's height\n\n")
                .append("The toolbar's preferred height, which is what the"
                        + " chart gains when the bar hides: **")
                .append(barHeight).append(" px** (chart ").append(chartSize.width)
                .append(" × ").append(chartSize.height).append(" shown, ")
                .append(hidden.width).append(" × ").append(hidden.height)
                .append(" hidden).\n\n");
    }

    /** The chart window as rectangles: the bar shown, and hidden. */
    private static void chartWindowImage(Rectangle shown, Rectangle hidden,
                                         int barHeight) throws java.io.IOException {
        double scale = 0.4;
        int w = (int) (chartSize.width * scale);
        int h = (int) ((chartSize.height + barHeight) * scale);
        BufferedImage image = new BufferedImage(2 * w + 48, h + 40,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        Rectangle[] charts = {shown, hidden};
        String[] captions = {"toolbar shown: chart " + shown.width + " × " + shown.height,
                "toolbar hidden: chart " + hidden.width + " × " + hidden.height};
        for (int i = 0; i < 2; i++) {
            int x0 = 16 + i * (w + 16);
            g.setColor(new Color(0xE8EBEF));
            g.fillRect(x0, 8, w, h);
            g.setColor(new Color(0x8A9099));
            g.drawRect(x0, 8, w - 1, h - 1);
            if (i == 0) {
                g.setColor(new Color(0xC9CED6));
                g.fillRect(x0, 8, w, (int) (barHeight * scale));
            }
            Rectangle c = charts[i];
            g.setColor(Color.WHITE);
            g.fillRect(x0 + (int) (c.x * scale), 8 + (int) (c.y * scale),
                    (int) (c.width * scale), (int) (c.height * scale));
            g.setColor(new Color(0x5A6270));
            g.drawRect(x0 + (int) (c.x * scale), 8 + (int) (c.y * scale),
                    (int) (c.width * scale) - 1, (int) (c.height * scale) - 1);
            g.setColor(new Color(0x20283A));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.drawString(captions[i], x0, h + 26);
        }
        g.dispose();
        ImageIO.write(image, "png",
                DIR.resolve("controls-clean-chart-window-shown-hidden.png").toFile());
    }

    // ---- the placement arithmetic --------------------------------------

    /** A laptop display and an external monitor, as macOS reports them. */
    private static final Rectangle LAPTOP = new Rectangle(0, 33, 1512, 868);
    private static final Rectangle EXTERNAL = new Rectangle(1512, -458, 2560, 1440);

    /** One dual-display case: the screens present, the chart, and the remembered companion. */
    private record Case(String name, List<Rectangle> screens, Rectangle chart,
                        Optional<Rectangle> remembered) {
    }

    private static List<Case> cases() {
        Rectangle laptopChart = new Rectangle(306, 75, 900, 785);
        Rectangle externalChart = EXTERNAL;
        Rectangle onLaptop = new Rectangle(1140, 60, 360, 814);
        Rectangle onExternal = new Rectangle(3700, -400, 360, 814);
        return List.of(
                new Case("one screen, both windows on it", List.of(LAPTOP),
                        laptopChart, Optional.of(onLaptop)),
                new Case("chart maximised on the external monitor, companion on the laptop",
                        List.of(LAPTOP, EXTERNAL), externalChart, Optional.of(onLaptop)),
                new Case("chart maximised on the external monitor, companion remembered beside it",
                        List.of(LAPTOP, EXTERNAL), externalChart, Optional.of(onExternal)),
                new Case("the external monitor disconnected, companion last seen on it",
                        List.of(LAPTOP), laptopChart, Optional.of(onExternal)),
                new Case("the external monitor reconnected, companion last seen on the laptop",
                        List.of(LAPTOP, EXTERNAL), externalChart, Optional.of(onLaptop)),
                new Case("nothing remembered, chart maximised on the external monitor",
                        List.of(LAPTOP, EXTERNAL), externalChart, Optional.empty()));
    }

    private static void placement(StringBuilder out) {
        out.append("## Where the companion goes, on two displays\n\n")
                .append("`CompanionPlacement.place` over the screens a desktop"
                        + " reports: a laptop display\n(1512 × 868 usable"
                        + " below a 33 px menu bar) and an external monitor"
                        + " (2560 × 1440,\nplaced to its right and above, as"
                        + " macOS arranges a taller monitor). The companion's\n"
                        + "remembered rectangle is used when its heading strip"
                        + " is on a screen that exists;\notherwise it goes"
                        + " beside the chart window's trailing edge on that"
                        + " window's\nscreen, over the edge when there is no"
                        + " room. This is arithmetic the suite already\nholds;"
                        + " what the desktop does when a monitor goes away is"
                        + " the owner's journey.\n\n")
                .append("| case | companion placed at | on | beside or over |\n")
                .append("|---|---|---|---|\n");
        Dimension preferred = new Dimension(WIDTH, 814);
        Dimension minimum = new Dimension(336, 120);
        for (Case c : cases()) {
            Rectangle at = CompanionPlacement.place(c.remembered(), c.screens(),
                    c.chart(), preferred, minimum);
            String on = at.intersects(EXTERNAL) && c.screens().contains(EXTERNAL)
                    ? "the external monitor" : "the laptop display";
            String how = c.remembered().isPresent() && at.equals(fittedOrSame(
                    c.remembered().get(), c.screens(), minimum))
                    ? "where it was"
                    : at.x >= c.chart().x + c.chart().width ? "beside the chart"
                            : "over the chart's trailing edge";
            out.append("| ").append(c.name()).append(" | ").append(at.x).append(", ")
                    .append(at.y).append(" ").append(at.width).append(" × ")
                    .append(at.height).append(" | ").append(on).append(" | ")
                    .append(how).append(" |\n");
        }
        out.append('\n');
    }

    private static Rectangle fittedOrSame(Rectangle was, List<Rectangle> screens,
                                          Dimension minimum) {
        for (Rectangle screen : screens) {
            Rectangle heading = new Rectangle(was.x, was.y, was.width, 32);
            if (!heading.intersection(screen).isEmpty()) {
                return CompanionPlacement.place(Optional.of(was), screens,
                        new Rectangle(0, 0, 1, 1), new Dimension(1, 1), minimum);
            }
        }
        return null;
    }

    private static List<String> placementImages() throws java.io.IOException {
        List<String> names = new ArrayList<>();
        List<Case> cases = cases();
        int cell = 300;
        int rows = 2;
        int cols = 3;
        BufferedImage image = new BufferedImage(cols * (cell + 16) + 16,
                rows * (cell / 2 + 60) + 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D whole = image.createGraphics();
        whole.setColor(Color.WHITE);
        whole.fillRect(0, 0, image.getWidth(), image.getHeight());
        whole.dispose();
        Dimension preferred = new Dimension(WIDTH, 814);
        Dimension minimum = new Dimension(336, 120);
        for (int i = 0; i < cases.size(); i++) {
            Case c = cases.get(i);
            Graphics2D g = image.createGraphics();
            g.translate(16 + (i % cols) * (cell + 16), 16 + (i / cols) * (cell / 2 + 60));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // The whole desktop, every screen, scaled into the cell.
            Rectangle desktop = new Rectangle(LAPTOP);
            desktop.add(EXTERNAL);
            double scale = (double) cell / desktop.width;
            for (Rectangle screen : List.of(LAPTOP, EXTERNAL)) {
                boolean present = c.screens().contains(screen);
                g.setColor(present ? new Color(0xE8EBEF) : new Color(0xF8F8F8));
                g.fillRect(sx(screen.x, desktop, scale), sy(screen.y, desktop, scale),
                        (int) (screen.width * scale), (int) (screen.height * scale));
                g.setColor(present ? new Color(0x8A9099) : new Color(0xD0D0D0));
                g.setStroke(new BasicStroke(present ? 1f : 1f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 1f, present ? null
                                : new float[] {3f, 3f}, 0f));
                g.drawRect(sx(screen.x, desktop, scale), sy(screen.y, desktop, scale),
                        (int) (screen.width * scale) - 1, (int) (screen.height * scale) - 1);
            }
            Rectangle at = CompanionPlacement.place(c.remembered(), c.screens(),
                    c.chart(), preferred, minimum);
            Rectangle[] windows = {c.chart(), at};
            Color[] fills = {new Color(0x20283A), new Color(0xF7F8FA)};
            Color[] inks = {Color.WHITE, new Color(0x20283A)};
            String[] labels = {"chart", "Controls"};
            for (int w = 0; w < 2; w++) {
                Rectangle r = windows[w];
                int x = sx(r.x, desktop, scale);
                int y = sy(r.y, desktop, scale);
                int rw = Math.max(4, (int) Math.round(r.width * scale));
                int rh = Math.max(4, (int) Math.round(r.height * scale));
                g.setColor(fills[w]);
                g.fillRect(x, y, rw, rh);
                g.setColor(new Color(0x5A6270));
                g.setStroke(new BasicStroke(1f));
                g.drawRect(x, y, rw, rh);
                g.setColor(inks[w]);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
                g.drawString(labels[w], x + 3, y + 11);
            }
            if (c.remembered().isPresent() && !c.remembered().get().equals(at)) {
                Rectangle r = c.remembered().get();
                g.setColor(new Color(0xB04040));
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 1f, new float[] {2f, 2f}, 0f));
                g.drawRect(sx(r.x, desktop, scale), sy(r.y, desktop, scale),
                        (int) Math.round(r.width * scale), (int) Math.round(r.height * scale));
            }
            g.setColor(new Color(0x20283A));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            String[] words = c.name().split(", ");
            for (int line = 0; line < words.length; line++) {
                g.drawString(words[line] + (line < words.length - 1 ? "," : ""),
                        0, (int) (desktop.height * scale) + 14 + line * 13);
            }
            g.dispose();
        }
        String name = "controls-clean-chart-placement-two-displays.png";
        ImageIO.write(image, "png", DIR.resolve(name).toFile());
        names.add(name);
        names.add("controls-clean-chart-window-shown-hidden.png");
        return names;
    }

    private static int sx(int x, Rectangle desktop, double scale) {
        return (int) Math.round((x - desktop.x) * scale);
    }

    private static int sy(int y, Rectangle desktop, double scale) {
        return (int) Math.round((y - desktop.y) * scale);
    }

    // ---- the shell, laid out and drawn off screen ----------------------

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
     * Lays a mock-up out off screen, invalidating the whole tree before
     * each pass and re-wrapping any Chart Options descriptions in it,
     * as the #442 study does (and for the reason it records).
     */
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

    /**
     * The narrowest shell in which every control is whole: none is
     * drawn narrower than it asks, and none reaches past the viewport's
     * edge (a row kept to its own width overflows rather than shrinks).
     * The search field is allowed to shrink to 120 px, as a field may.
     */
    private static int narrowest(java.util.function.Supplier<List<JComponent>> sections) {
        for (int width = 200; width <= 800; width += 2) {
            JPanel shell = new JPanel(new BorderLayout());
            JPanel stack = new JPanel();
            stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
            for (JComponent section : sections.get()) {
                stack.add(section);
            }
            JScrollPane scroll = new JScrollPane(new WidthTracking(stack),
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            shell.add(scroll, BorderLayout.CENTER);
            sized(shell, width, Integer.MAX_VALUE);
            if (truncated(shell, scroll.getViewport()).isEmpty()) {
                return width;
            }
        }
        return -1;
    }

    static List<String> truncated(Container root, JComponent viewport) {
        List<Component> all = new ArrayList<>();
        // Only what the viewport shows: the scroll bar's own buttons
        // stand beside it, outside it, by design.
        walk(viewport, all);
        List<String> narrow = new ArrayList<>();
        int edge = viewport.getWidth();
        for (Component component : all) {
            if (!(component instanceof AbstractButton
                    || component instanceof JTextField
                    || (component instanceof JLabel label
                            && !String.valueOf(label.getText()).startsWith("<html>")))
                    || !visibleUpTo(component, root)) {
                continue;
            }
            int asked = component instanceof JTextField ? 120
                    : component.getPreferredSize().width;
            Rectangle inViewport = SwingUtilities.convertRectangle(
                    component.getParent(), component.getBounds(), viewport);
            if (component.getWidth() < asked
                    || inViewport.x + inViewport.width > edge) {
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

    // ---- the mock-ups ----------------------------------------------------

    private static List<String> drawLight(InterfaceText said, String language)
            throws Exception {
        String stem = "controls-clean-chart-" + language + "-";
        List<String> names = new ArrayList<>();
        for (Arrangement arrangement : Arrangement.values()) {
            String name = stem + arrangement.number + "-" + arrangement.stem + ".png";
            write(shell(List.of(section(arrangement, said, State.IDLE)),
                    Integer.MAX_VALUE), name);
            names.add(name);
        }
        write(shell(withBoth(Arrangement.ROWS, said, State.IDLE), TALLEST),
                stem + "4-rows-with-both-900.png");
        names.add(stem + "4-rows-with-both-900.png");
        write(shell(withBoth(Arrangement.SECTIONS, said, State.IDLE), TALLEST),
                stem + "5-sections-with-both-900.png");
        names.add(stem + "5-sections-with-both-900.png");
        int n = 6;
        for (State state : new State[] {State.RESULTS, State.NO_MATCH, State.ZOOM_END,
                State.INSPECTOR_UNAVAILABLE, State.INSPECTOR_SHOWING, State.EMPHASES}) {
            String name = stem + n++ + "-rows-" + state.name().toLowerCase()
                    .replace('_', '-') + ".png";
            write(shell(List.of(section(Arrangement.ROWS, said, state)),
                    Integer.MAX_VALUE), name);
            names.add(name);
        }
        write(shell(List.of(section(Arrangement.SECTIONS, said, State.RESULTS)),
                Integer.MAX_VALUE), stem + n + "-sections-results.png");
        names.add(stem + n + "-sections-results.png");
        return names;
    }

    /** How many stops a keyboard makes through each arrangement. */
    private static void focus(StringBuilder out, InterfaceText said) {
        out.append("## Keyboard stops through each arrangement, `")
                .append(said.language()).append("`\n\n")
                .append("Focusable controls in traversal order, headings"
                        + " included; the toolbar today has the\nsame controls"
                        + " on one row, with no heading.\n\n")
                .append("| arrangement | headings | controls | stops |\n")
                .append("|---|---:|---:|---:|\n");
        for (Arrangement arrangement : Arrangement.values()) {
            JComponent section = section(arrangement, said, State.IDLE);
            List<Component> all = new ArrayList<>();
            walk(section, all);
            int headings = 0;
            int controls = 0;
            for (Component component : all) {
                if (!component.isFocusable() || !(component instanceof JComponent)) {
                    continue;
                }
                if (component instanceof AbstractButton button
                        && String.valueOf(button.getName()).startsWith("heading.")) {
                    headings++;
                } else if (component instanceof AbstractButton
                        || component instanceof JTextField) {
                    controls++;
                }
            }
            out.append("| ").append(arrangement.title(said)).append(" | ")
                    .append(headings).append(" | ").append(controls).append(" | ")
                    .append(headings + controls).append(" |\n");
        }
        out.append('\n');
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

    /**
     * A flow layout whose preferred height follows the width it is
     * given, so a wrapped toolbar reports how tall it really is.
     */
    private static final class WrapLayout extends java.awt.FlowLayout {

        WrapLayout(int hgap, int vgap) {
            super(LEFT, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return wrapped(target);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return wrapped(target);
        }

        private Dimension wrapped(Container target) {
            int width = target.getWidth();
            if (width <= 0) {
                Container parent = target.getParent();
                width = parent == null || parent.getWidth() <= 0 ? WIDTH : parent.getWidth();
            }
            java.awt.Insets insets = target.getInsets();
            int available = width - insets.left - insets.right - getHgap() * 2;
            int rowWidth = 0;
            int rowHeight = 0;
            int height = 0;
            for (Component child : target.getComponents()) {
                if (!child.isVisible()) {
                    continue;
                }
                Dimension d = child.getPreferredSize();
                if (rowWidth + d.width > available && rowWidth > 0) {
                    height += rowHeight + getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                }
                rowWidth += d.width + getHgap();
                rowHeight = Math.max(rowHeight, d.height);
            }
            height += rowHeight;
            return new Dimension(width, height + insets.top + insets.bottom
                    + getVgap() * 2);
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
