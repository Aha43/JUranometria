package juranometria.tool;

import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;
import juranometria.ui.companion.CompanionStore;
import juranometria.ui.companion.CompanionWindow;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.placeandtime.PlaceAndTimePanel;
import juranometria.ui.placeandtime.PlaceStore;

/**
 * The companion window, in both languages, for owner review (#434).
 *
 * <p>The production window with its one production section, Place and
 * Time, in a real window at the size its own policy states: open in
 * the light and the dark appearance, with the section collapsed, and
 * with an entry refused. The companion report carries what no picture
 * can - what each control is called, hovered and read out.
 *
 * <p>Process-wide state is the photographer's to put back: the
 * contract runs every generator in one process, and an earlier one may
 * leave a default font behind (#362).
 */
public final class CompanionSheetMain {

    /**
     * Application-sized: the companion states its own width and the
     * height its sections ask for, and packing it would photograph a
     * window no reader meets.
     */
    public static final SheetCapture.Kind CAPTURE_KIND =
            SheetCapture.Kind.APPLICATION_SIZED;

    private static Path out = Path.of("docs/studies/interface-language");

    /** One arrangement, and what it shows. */
    private record State(String name, boolean dark, boolean collapsed,
                         String refused, boolean deepSkyOpen,
                         boolean solarSystemOpen, boolean jupiterOpen) {
    }

    /** A chart switch that stands still: the section's photograph is not the chart's. */
    private static final class StillSwitch implements juranometria.ui.solar.BodyOnChart {
        private boolean shown;
        private final java.util.List<java.util.function.Consumer<Boolean>> listeners =
                new java.util.ArrayList<>();

        @Override
        public boolean showing() {
            return shown;
        }

        @Override
        public void show(boolean wanted) {
            shown = wanted;
            for (java.util.function.Consumer<Boolean> l : java.util.List.copyOf(listeners)) {
                l.accept(shown);
            }
        }

        @Override
        public void onChange(java.util.function.Consumer<Boolean> listener) {
            listeners.add(listener);
            listener.accept(shown);
        }
    }

    private static juranometria.solar.SolarSystemService loaded;

    /** The service, read once for every photograph. */
    private static synchronized juranometria.solar.SolarSystemService solarSystem() {
        if (loaded == null) {
            loaded = juranometria.solar.SolarSystemService.load();
        }
        return loaded;
    }

    /**
     * All four sections in every state, as the application holds them
     * since #458: Chart controls, Place and Time, Solar System (introduced
     * collapsed; "solar-system-open" opens it, Sun open with its instant
     * applied, Moon collapsed), then Chart Options with its subject
     * groups as first introduced - Deep sky collapsed, the rest open.
     * "collapsed" collapses Place and Time.
     */
    private static final List<State> STATES = List.of(
            new State("open", false, false, null, false, false, false),
            new State("dark", true, false, null, false, false, false),
            new State("collapsed", false, true, null, false, false, false),
            new State("refused", false, false, "91", false, false, false),
            new State("deep-sky-open", false, true, null, true, false, false),
            new State("solar-system-open", false, true, null, false, true, false),
            // #486: Jupiter's group open, the Sun's collapsed, nothing
            // computed - the Jovian module's Show on chart box in place.
            new State("jupiter-open", false, true, null, false, true, true));

    private CompanionSheetMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && !args[0].isBlank()) {
            out = Path.of(args[0]);
        }
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.err.println("a window needs a display, and a stand-in"
                    + " panel is what the review rejected");
            System.exit(1);
        }
        Files.createDirectories(out);
        javax.swing.LookAndFeel before = UIManager.getLookAndFeel();
        boolean hadFont = UIManager.getDefaults().containsKey("defaultFont");
        Object font = hadFont ? UIManager.get("defaultFont") : null;
        PlaceAndTimeSheetMain.State oslo = PlaceAndTimeSheetMain.STATES.stream()
                .filter(state -> state.name().equals("oslo"))
                .findFirst().orElseThrow();
        StringBuilder said = new StringBuilder("""
                # Every word the companion says

                Issue #434. Generated by
                `juranometria.tool.CompanionSheetMain` from compiled
                application classes and their classpath resources.

                The production companion window, holding its four
                production sections - the chart's controls, the same
                class the toolbar hosts; the same `PlaceAndTimePanel`
                the Place and Time dialog holds; Solar System, the same
                controls the Sun and Moon dialogs host; and Chart
                Options - in
                a real window at the size the companion's own policy
                states. Place and Time is `PlaceAndTimeSheetMain`'s Oslo
                arrangement at its frozen instant.

                %s

                """.formatted(InterfaceLanguageStatus.statement("nb-NO")));
        try {
            SwingUtilities.invokeAndWait(() -> UIManager.put("defaultFont", null));
            for (String language : List.of("en", "nb-NO")) {
                said.append("## ").append(language.equals("en")
                        ? "English" : "Norsk bokmål").append(" (`")
                        .append(language).append("`)\n\n");
                int number = 1;
                for (State state : STATES) {
                    String name = "companion-" + language + "-" + number++
                            + "-" + state.name() + ".png";
                    said.append(sheet(language, state, oslo, out.resolve(name)));
                }
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
        Files.writeString(out.resolve("companion-strings.md"),
                said.toString(), StandardCharsets.UTF_8);
        System.out.println("companion sheets: " + 2 * STATES.size() + " images and "
                + out.resolve("companion-strings.md"));
    }

    private static String sheet(String language, State state,
                                PlaceAndTimeSheetMain.State place, Path to)
            throws Exception {
        Preferences node = Preferences.userRoot().node(
                "juranometria-study-companion-sheet-" + System.nanoTime());
        JFrame[] owner = new JFrame[1];
        CompanionWindow[] window = new CompanionWindow[1];
        PlaceAndTimePanel[] panel = new PlaceAndTimePanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                juranometria.app.UiTheme.apply(state.dark());
                InterfaceText words = InterfaceText.forLanguage(language);
                MeridianModule module = new MeridianModule(new Observer(
                        place.latitude(), place.eastLongitude(),
                        PlaceAndTimeSheetMain.WHEN));
                module.showing(place.meridian(), place.horizon(),
                        place.zenith());
                PlaceStore store = PlaceStore.forNode(node);
                store.save(place.latitude(), place.eastLongitude());
                owner[0] = new JFrame("study");
                window[0] = new CompanionWindow(owner[0], words,
                        CompanionStore.forNode(node));
                // Chart controls first (#450, ruled on #449), composed
                // through AtlasChrome from a stored language as the
                // toolbar sheet is, so the companion's controls are
                // photographed in the language the application hands
                // them, not one handed straight to them.
                juranometria.ui.language.SkyLanguageStore languages =
                        juranometria.ui.language.SkyLanguageStore.forNode(node);
                languages.save(languages.choice(
                        juranometria.app.Atlas.languages()).withInterface(language));
                juranometria.ui.ChartViewController navigation =
                        new juranometria.ui.ChartViewController(
                                juranometria.app.Atlas.assembler()::fits);
                juranometria.ui.InspectorToggle inspector =
                        new juranometria.ui.InspectorToggle();
                inspector.bind(() -> { }, () -> true);
                juranometria.app.AtlasChrome chrome = juranometria.app.AtlasChrome.of(
                        juranometria.ui.language.SkyLanguageSession.begin(languages,
                                juranometria.app.Atlas.languages()),
                        navigation, juranometria.app.Atlas.search(),
                        juranometria.app.Atlas.assembler(), inspector, "2.0.0",
                        () -> { }, new juranometria.chart.SelectionMode(),
                        new juranometria.ui.ZoomLock());
                juranometria.ui.ChartComponent chart =
                        new juranometria.ui.ChartComponent(
                                juranometria.app.Atlas.assembler(),
                                juranometria.ui.language.PageText.in(words));
                navigation.onChange(chart::setViewState);
                window[0].addSection("chartcontrols",
                        words.say("chartcontrols.title"),
                        chrome.companionControls(chart).inCompanion());
                panel[0] = new PlaceAndTimePanel(module, store,
                        () -> PlaceAndTimeSheetMain.WHEN, words);
                window[0].addSection("placeandtime",
                        words.say("placeandtime.title"), panel[0]);
                CompanionStore companionStore = CompanionStore.forNode(node);
                // Solar System (#458, ruled on #457), third: the Sun and
                // Moon groups over sessions of their own and a chart
                // switch standing in for the module's - the photograph is
                // of the section, not of the chart. Introduced collapsed,
                // Sun open, Moon collapsed; opened for the state that
                // shows it.
                juranometria.sky.Observer observer = new juranometria.sky.Observer(
                        place.latitude(), place.eastLongitude(),
                        PlaceAndTimeSheetMain.WHEN);
                juranometria.solar.SolarSystemService solar = solarSystem();
                if (state.jupiterOpen()) {
                    companionStore.saveCollapsed(
                            juranometria.ui.solar.SolarSystemSection.ID + ".sun", true);
                    companionStore.saveCollapsed(
                            juranometria.ui.solar.SolarSystemSection.ID + ".jupiter", false);
                }
                juranometria.ui.solar.SolarSystemSection solarSection =
                        new juranometria.ui.solar.SolarSystemSection(
                                new juranometria.ui.solar.SolarTableSession(
                                        () -> observer, solar,
                                        juranometria.ui.solar.SolarTable.sun()),
                                new juranometria.ui.solar.SolarTableSession(
                                        () -> observer, solar,
                                        juranometria.ui.solar.SolarTable.moon()),
                                new StillSwitch(), new StillSwitch(),
                                // Jupiter's group (#474) with the Jovian
                                // module's switch (#484, #486), as the
                                // application's Controller holds it;
                                // introduced collapsed, it computes nothing,
                                // so the pack is never read.
                                new juranometria.ui.solar.JovianTableSession(
                                        () -> observer,
                                        juranometria.solar.JovianSystemService::load),
                                new StillSwitch(), null, words);
                window[0].addSection("solarsystem", words.say("solarsystem.title"),
                        solarSection.inController(companionStore, words), true);
                if (state.solarSystemOpen()) {
                    window[0].sections().get(2).heading().doClick();
                    if (!state.jupiterOpen()) {
                        solarSection.sun().apply();
                    }
                }
                window[0].addSection("chartoptions",
                        words.say("chartoptions.title"),
                        new juranometria.app.ChartOptionsControls(
                                new juranometria.app.ChartOptionsController(
                                        juranometria.app.ChartOptionsStore.forNode(node)),
                                () -> false, words).inCompanion(companionStore,
                                words));
                if (state.collapsed()) {
                    window[0].sections().get(1).heading().doClick();
                }
                if (state.deepSkyOpen()) {
                    deepSkyHeading(window[0].sections().get(3)).doClick();
                }
                if (state.refused() != null) {
                    JTextField latitude = (JTextField) named(panel[0],
                            "latitudeField");
                    latitude.setText(state.refused());
                    latitude.postActionEvent();
                }
                window[0].applySizePolicy();
            });
            SwingUtilities.invokeAndWait(() -> { });
            JComponent content = (JComponent) window[0].getContentPane();
            SheetCapture.write(window[0], content,
                    SheetCapture.applicationSized(
                            "CompanionWindow.applySizePolicy",
                            new SheetCapture.ApplicationPolicy() {
                                @Override
                                public java.awt.Dimension declaredContent() {
                                    return window[0].sizePolicyContent();
                                }

                                @Override
                                public void apply() {
                                    window[0].applySizePolicy();
                                }
                            }),
                    to);
            StringBuilder rows = new StringBuilder();
            rows.append("### ").append(state.name()).append("\n\n")
                    .append("![](").append(to.getFileName()).append(")\n\n")
                    .append("| shown | hovered | spoken as | read out |\n")
                    .append("|---|---|---|---|\n")
                    .append("| ").append(cell(window[0].getTitle()))
                    .append(" | — | ")
                    .append(cell(window[0].getAccessibleContext()
                            .getAccessibleName()))
                    .append(" | ")
                    .append(cell(window[0].getAccessibleContext()
                            .getAccessibleDescription()))
                    .append(" |\n")
                    .append(describe(content)).append('\n');
            return rows.toString();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (window[0] != null) {
                    window[0].dispose();
                }
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
            try {
                node.removeNode();
            } catch (java.util.prefs.BackingStoreException leaving) {
                // An empty node is a blemish, not a failure.
            }
        }
    }

    /** The Deep sky group's heading inside the Chart Options section. */
    private static javax.swing.AbstractButton deepSkyHeading(Component root) {
        return (javax.swing.AbstractButton) named(root,
                "heading.chartoptions.deepsky");
    }

    private static Component named(Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = named(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Every visible control's four texts, in reading order. */
    private static String describe(Component from) {
        StringBuilder rows = new StringBuilder();
        for (Component control : reading(from)) {
            if (!(control instanceof JComponent said) || !control.isVisible()) {
                continue;
            }
            String shown = control instanceof AbstractButton button
                    ? button.getText()
                    : control instanceof JLabel label ? label.getText()
                    : control instanceof JTextField field ? field.getText()
                    : null;
            String hovered = said.getToolTipText();
            String name = said.getAccessibleContext() == null ? null
                    : said.getAccessibleContext().getAccessibleName();
            String read = said.getAccessibleContext() == null ? null
                    : said.getAccessibleContext().getAccessibleDescription();
            if (blank(shown) && blank(hovered) && blank(name) && blank(read)) {
                continue;
            }
            rows.append("| ").append(cell(shown))
                    .append(" | ").append(cell(hovered))
                    .append(" | ").append(cell(name))
                    .append(" | ").append(cell(read)).append(" |\n");
        }
        return rows.toString();
    }

    private static List<Component> reading(Component from) {
        List<Component> found = new ArrayList<>();
        if (!(from instanceof Container container) || !from.isVisible()) {
            return found;
        }
        for (Component child : container.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            found.add(child);
            found.addAll(reading(child));
        }
        return found;
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    private static String cell(String text) {
        return blank(text) ? "—" : ChartOptionsSheetMain.plainText(text)
                .replace("|", "\\|");
    }
}
