package juranometria.app;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import juranometria.ui.ChartViewController;

/**
 * The application's restrained conventional menu bar: a File menu
 * carrying Settings and a Help menu carrying About - nothing else,
 * and deliberately no placeholder items for future features. File
 * rather than an app-named menu: on macOS the screen menu bar already
 * provides the application menu, and a second menu with the same name
 * reads as a duplicate (owner review of Sprint 11); File is the
 * conventional cross-platform home and leaves room for future items
 * that genuinely belong there. Actions are injected so the wiring is
 * testable headless and the menu never reaches into chart state.
 */
public final class AppMenuBar {

    /** The inspector item's name, so callers can keep it in step. */
    public static final String INSPECTOR_ITEM = "inspectorItem";

    /** The File menu's export item, for tests and for automation. */
    public static final String EXPORT_ITEM = "exportSheetItem";

    private static JMenuItem named(JMenuBar bar, String name) {
        for (int menu = 0; menu < bar.getMenuCount(); menu++) {
            JMenuItem found = named(bar.getMenu(menu), name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** The item of that name in a menu, or in any submenu inside it (#458). */
    private static JMenuItem named(JMenu each, String name) {
        for (int item = 0; item < each.getItemCount(); item++) {
            JMenuItem candidate = each.getItem(item);
            if (candidate != null && name.equals(candidate.getName())) {
                return candidate;
            }
            if (candidate instanceof JMenu sub) {
                JMenuItem inside = named(sub, name);
                if (inside != null) {
                    return inside;
                }
            }
        }
        return null;
    }

    /** The File menu's export item in a built bar, or null. */
    public static JMenuItem exportItem(JMenuBar bar) {
        return named(bar, EXPORT_ITEM);
    }

    /** The ecliptic item's name, for the same reason (issue #274). */
    public static final String ECLIPTIC_ITEM = "eclipticItem";

    /** The inspector's menu item within a bar, or null. */
    public static javax.swing.JCheckBoxMenuItem inspectorItem(
            javax.swing.JMenuBar bar) {
        return checkBoxItem(bar, INSPECTOR_ITEM);
    }

    /**
     * The ecliptic's menu item within a bar, or null when no
     * ecliptic module is loaded - which is what an atlas without it
     * shows: no control for something that cannot be drawn.
     */
    public static javax.swing.JCheckBoxMenuItem eclipticItem(
            javax.swing.JMenuBar bar) {
        return checkBoxItem(bar, ECLIPTIC_ITEM);
    }

    private static javax.swing.JCheckBoxMenuItem checkBoxItem(
            javax.swing.JMenuBar bar, String name) {
        JMenuItem found = named(bar, name);
        return found instanceof javax.swing.JCheckBoxMenuItem box ? box : null;
    }

    private AppMenuBar() {
    }

    /** The menu bar without navigation; for wiring-level tests. */
    /**
     * The menu bar in a language a caller states.
     *
     * <p>One of these per arity that callers actually use, so the
     * thing nobody outside this package may omit is the LANGUAGE.
     * Every handler here is genuinely optional - a harness may want
     * a bar with no Place and Time, no ecliptic, no export - but
     * which words the bar says is not (#350).
     */

    /**
     * The menu bar in a language a caller states, with no chart
     * controller behind the zoom items.
     *
     * <p>One of these per arity that callers actually use, so the
     * thing nobody may omit is the LANGUAGE. Every handler is
     * genuinely optional; which words the bar says is not (#350).
     */
    public static JMenuBar create(Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  juranometria.ui.language.InterfaceText said) {
        return create(null, openSettings, openChartOptions, openAbout,
                null, null, null, null, said);
    }

    /** The same, with the chart controller behind the zoom items. */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                null, null, null, null, said);
    }

    /** The same, with the inspector toggle. */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, null, null, null, said);
    }

    /** The same, with Place and Time. */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, null, null, said);
    }

    /** The same, with the ecliptic switch. */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, toggleEcliptic, null,
                said);
    }

    // Package-private since #350: these default to English, and a
    // production caller that chose English by accident is exactly
    // what shipped a translated toolbar in English. Harnesses in
    // this package may use them; anything outside states a language.

    /**
     * @param navigation the chart-view controller backing the View
     *     menu's centre-preserving Zoom In/Out items (may be null,
     *     omitting them)
     * @param openSettings runs on the Settings... item (may be null
     *     while no settings exist, omitting the item)
     * @param openChartOptions runs on the View menu's Chart Options...
     *     item (may be null, omitting the View menu)
     * @param openAbout runs on the About item
     */

    /**
     * The menu bar with the inspector toggle (issue #170). Opening
     * and closing the inspector lives here rather than in the
     * toolbar, which stays the essential controls only.
     */

    /**
     * The menu bar with Place and Time (Sprint 25, issue #228). A
     * place and an instant are settings, not readings, so the item
     * lives on the View menu beside Chart Options - the gate's
     * ruling, measured against the Inspector alternative at 240 px.
     *
     * @param openPlaceAndTime runs on the View menu's Place and
     *     Time... item (may be null, omitting the item - which is
     *     what an atlas without the meridian module shows)
     */

    /**
     * The menu bar with the ecliptic switch (Sprint 28, issue #274).
     *
     * <p>A checkbox item, because a reader must be able to see
     * whether the ecliptic is showing without looking at the chart
     * and guessing - the same reason the Inspector's item is one.
     * The gate chose the View menu over a dialog and over Chart
     * Options: the ecliptic has no settings at all, so a dialog
     * would be a window built around a single checkbox, and Chart
     * Options is the chart's own drawing rather than an optional
     * module's.
     *
     * @param toggleEcliptic runs on the View menu's Ecliptic item
     *     (may be null, omitting the item - which is what an atlas
     *     without the ecliptic module shows)
     */

    /**
     * The menu bar with the export item (Sprint 29, issue #286).
     *
     * <p>File, because that is where every application a reader has
     * ever used keeps "make me a file". Not the toolbar, which is
     * for the chart, and not Chart Options, which is for what the
     * chart draws.
     *
     * @param exportSheet runs on File's Export chart sheet item (may
     *     be null, omitting the item)
     */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  Runnable exportSheet,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, toggleEcliptic,
                exportSheet, null, said);
    }

    /** The name the Copy View Report item carries, for tests to find. */
    public static final String VIEW_REPORT_ITEM = "copyViewReportItem";

    /** The name the Sun table item carries, for tests to find (#400). */
    public static final String SUN_ITEM = "sunTableItem";

    /** The name the Moon table item carries, for tests to find (#408). */
    public static final String MOON_ITEM = "moonTableItem";

    /** The name the Sun-on-the-chart switch carries, for tests to find (#415). */
    public static final String SUN_CHART_ITEM = "sunOnChartItem";

    /** The name the Moon-on-the-chart switch carries, for tests to find (#416). */
    public static final String MOON_CHART_ITEM = "moonOnChartItem";
    /**
     * Venus on the chart's item (#514), between the Sun's and the Moon's:
     * named by the Moon's rule - the body's identity and the item's
     * suffix - so no new name is written down.
     */
    public static final String VENUS_CHART_ITEM =
            juranometria.solarchart.SolarSystemModule.VENUS
                    + MOON_CHART_ITEM.substring(juranometria.solarchart.SolarSystemModule.MOON.length());

    /** Mercury on the chart's item (#542), between the Sun's and Venus's, named by the same rule. */
    public static final String MERCURY_CHART_ITEM =
            juranometria.solarchart.SolarSystemModule.MERCURY
                    + MOON_CHART_ITEM.substring(juranometria.solarchart.SolarSystemModule.MOON.length());

    /** Mars on the chart's item (#606), between the Moon's and Jupiter's, named by the same rule. */
    public static final String MARS_CHART_ITEM =
            juranometria.solarchart.SolarSystemModule.MARS
                    + MOON_CHART_ITEM.substring(juranometria.solarchart.SolarSystemModule.MOON.length());

    public static javax.swing.JCheckBoxMenuItem mercuryChartItem(javax.swing.JMenuBar bar) {
        return checkBoxItem(bar, MERCURY_CHART_ITEM);
    }

    public static javax.swing.JCheckBoxMenuItem venusChartItem(
            javax.swing.JMenuBar bar) {
        return checkBoxItem(bar, VENUS_CHART_ITEM);
    }

    /** View's Moon-on-the-chart switch, or null when the bar has none. */
    public static javax.swing.JCheckBoxMenuItem moonChartItem(
            javax.swing.JMenuBar bar) {
        return checkBoxItem(bar, MOON_CHART_ITEM);
    }

    /** The name View's Controls switch carries, for tests (#434). */
    public static final String COMPANION_ITEM = "companionItem";

    /** The name View's Chart Toolbar switch carries, for tests (#450). */
    public static final String TOOLBAR_ITEM = "toolbarItem";

    /** The name View's Solar System submenu carries, for tests (#458). */
    public static final String SOLAR_SYSTEM_MENU = "solarSystemMenu";

    /** View's Solar System submenu, or null when the bar has none (#458). */
    public static JMenu solarSystemMenu(javax.swing.JMenuBar bar) {
        JMenuItem found = named(bar, SOLAR_SYSTEM_MENU);
        return found instanceof JMenu menu ? menu : null;
    }

    /** View's Chart Toolbar switch, or null when the bar has none (#450). */
    public static javax.swing.JCheckBoxMenuItem toolbarItem(
            javax.swing.JMenuBar bar) {
        return checkBoxItem(bar, TOOLBAR_ITEM);
    }

    /** View's Controls switch, or null when the bar has none (#434). */
    public static javax.swing.JCheckBoxMenuItem companionItem(
            javax.swing.JMenuBar bar) {
        return checkBoxItem(bar, COMPANION_ITEM);
    }

    /** View's Sun-on-the-chart switch, or null when the bar has none. */
    public static javax.swing.JCheckBoxMenuItem sunChartItem(
            javax.swing.JMenuBar bar) {
        return checkBoxItem(bar, SUN_CHART_ITEM);
    }

    /**
     * The same, without the Sun table (#400): kept so every earlier
     * caller keeps its arity, as was done for Copy View Report.
     */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  Runnable exportSheet,
                                  Runnable copyViewReport,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, toggleEcliptic,
                exportSheet, copyViewReport, null, said);
    }

    /**
     * The menu bar with Copy View Report (issue #372).
     *
     * <p>Help, because it is how a reader asks for help with what
     * they are looking at. The menu does not learn how to inspect a
     * chart: the application hands it the action, as it hands every
     * other.
     *
     * @param copyViewReport runs on Help's Copy View Report item (may
     *     be null, omitting the item)
     */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  Runnable exportSheet,
                                  Runnable copyViewReport,
                                  Runnable openSunTable,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, toggleEcliptic,
                exportSheet, copyViewReport, openSunTable, null, said);
    }

    /**
     * The menu bar with View's Moon table (Sprint 36, issue #408),
     * beside the Sun's: the same seam, one more body.
     *
     * @param openMoonTable runs on View's Moon item (may be null,
     *     omitting the item)
     */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  Runnable exportSheet,
                                  Runnable copyViewReport,
                                  Runnable openSunTable,
                                  Runnable openMoonTable,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, toggleEcliptic,
                exportSheet, copyViewReport, openSunTable, openMoonTable, null,
                said);
    }

    /**
     * The menu bar with View's Sun-on-the-chart switch (Sprint 37,
     * issue #415): a checkbox beside the Ecliptic's, because both are
     * layers a reader turns on and off.
     *
     * @param toggleSunOnChart runs on View's Sun on the chart item (may
     *     be null, omitting the item)
     */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  Runnable exportSheet,
                                  Runnable copyViewReport,
                                  Runnable openSunTable,
                                  Runnable openMoonTable,
                                  Runnable toggleSunOnChart,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, toggleEcliptic,
                exportSheet, copyViewReport, openSunTable, openMoonTable,
                toggleSunOnChart, null, said);
    }

    /**
     * The menu bar with View's Moon-on-the-chart switch (Sprint 37,
     * issue #416), directly below the Sun's: the same kind of layer,
     * switched the same way, one more body.
     *
     * @param toggleMoonOnChart runs on View's Moon on the chart item
     *     (may be null, omitting the item)
     */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  Runnable exportSheet,
                                  Runnable copyViewReport,
                                  Runnable openSunTable,
                                  Runnable openMoonTable,
                                  Runnable toggleSunOnChart,
                                  Runnable toggleMoonOnChart,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, toggleEcliptic,
                exportSheet, copyViewReport, openSunTable, openMoonTable,
                toggleSunOnChart, toggleMoonOnChart, null, said);
    }

    /**
     * The menu bar with View's Controls switch (Sprint 39, issue #434,
     * ruled on #433): the one access point the companion window has,
     * a checkbox directly after Place and Time... - the section it
     * holds - showing whether the window is open. The menu is
     * otherwise unchanged, and Place and Time... still opens its
     * dialog.
     *
     * @param toggleCompanion runs on View's Controls item (may be
     *     null, omitting the item)
     */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  Runnable exportSheet,
                                  Runnable copyViewReport,
                                  Runnable openSunTable,
                                  Runnable openMoonTable,
                                  Runnable toggleSunOnChart,
                                  Runnable toggleMoonOnChart,
                                  Runnable toggleCompanion,
                                  juranometria.ui.language.InterfaceText said) {
        return create(navigation, openSettings, openChartOptions, openAbout,
                toggleInspector, openPlaceAndTime, toggleEcliptic,
                exportSheet, copyViewReport, openSunTable, openMoonTable,
                toggleSunOnChart, toggleMoonOnChart, toggleCompanion, null,
                said);
    }

    /**
     * The same, with View's Chart Toolbar switch (Sprint 41, issue
     * #450, ruled on #449): a checkbox beside Controls that shows or
     * hides the chart window's toolbar, with the keystroke the
     * registry names.
     *
     * @param toggleToolbar runs on View's Chart Toolbar item (may be
     *     null, omitting the item)
     */
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout,
                                  Runnable toggleInspector,
                                  Runnable openPlaceAndTime,
                                  Runnable toggleEcliptic,
                                  Runnable exportSheet,
                                  Runnable copyViewReport,
                                  Runnable openSunTable,
                                  Runnable openMoonTable,
                                  Runnable toggleSunOnChart,
                                  Runnable toggleMoonOnChart,
                                  Runnable toggleCompanion,
                                  Runnable toggleToolbar,
                                  juranometria.ui.language.InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the menu has to say its words in some language");
        }
        // An access letter marks a letter in the item's own word, so
        // it belongs to the language that wrote the word (#350).
        juranometria.ui.language.MnemonicText letters =
                juranometria.ui.language.MnemonicText.in(said);
        if (openAbout == null) {
            throw new IllegalArgumentException("about action is required");
        }
        JMenuBar bar = new JMenuBar();

        if (openSettings != null || exportSheet != null) {
            JMenu application = new JMenu(said.say("menu.file.label"));
            application.getAccessibleContext().setAccessibleName(
                    said.say("menu.file.a11y"));
            // A menu's own word is the whole of it, and a tooltip
            // over an open menu is a box between the reader and the
            // items they came for.
            juranometria.ui.Explain.selfExplanatory(application,
                    said.say("menu.file.explain"));
            if (exportSheet != null) {
                JMenuItem export =
                        new JMenuItem(said.say("menu.export.label"));
                export.setName(EXPORT_ITEM);
                letters.apply(export, "menu.export.mnemonic");
                export.setAccelerator(juranometria.ui.Shortcuts.of(
                        juranometria.ui.Shortcuts.EXPORT).stroke());
                export.getAccessibleContext().setAccessibleName(
                        said.say("menu.export.a11y"));
                juranometria.ui.Explain.selfExplanatory(export,
                        said.say("menu.export.explain"));
                export.addActionListener(event -> exportSheet.run());
                application.add(export);
                if (openSettings != null) {
                    application.addSeparator();
                }
            }
            if (openSettings != null) {
                JMenuItem settings =
                        new JMenuItem(said.say("menu.settings.label"));
                settings.getAccessibleContext().setAccessibleName(
                        said.say("menu.settings.a11y"));
                // Since #348 this window also chooses the interface
                // and chart languages; the old sentence named only
                // light and dark (#350).
                juranometria.ui.Explain.selfExplanatory(settings,
                        said.say("menu.settings.explain"));
                settings.addActionListener(event -> openSettings.run());
                application.add(settings);
            }
            bar.add(application);
        }

        if (openChartOptions != null) {
            JMenu view = new JMenu(said.say("menu.view.label"));
            view.getAccessibleContext().setAccessibleName(
                    said.say("menu.view.a11y"));
            juranometria.ui.Explain.selfExplanatory(view,
                    said.say("menu.view.explain"));
            JMenuItem chartOptions =
                    new JMenuItem(said.say("menu.chartoptions.label"));
            letters.apply(chartOptions, "menu.chartoptions.mnemonic");
            chartOptions.getAccessibleContext().setAccessibleName(
                    said.say("menu.chartoptions.a11y"));
            juranometria.ui.Explain.selfExplanatory(chartOptions,
                    // The keystroke is an argument, not a fragment
                    // glued into an English sentence (#350).
                    said.say("menu.chartoptions.explain",
                            ChartKeys.prefixText()));
            chartOptions.addActionListener(event -> openChartOptions.run());
            view.add(chartOptions);
            if (openPlaceAndTime != null) {
                JMenuItem placeAndTime =
                        new JMenuItem(said.say("menu.placeandtime.label"));
                letters.apply(placeAndTime, "menu.placeandtime.mnemonic");
                placeAndTime.getAccessibleContext().setAccessibleName(
                        said.say("menu.placeandtime.a11y"));
                juranometria.ui.Explain.selfExplanatory(placeAndTime,
                        said.say("menu.placeandtime.explain"));
                placeAndTime.addActionListener(event ->
                        openPlaceAndTime.run());
                view.add(placeAndTime);
            }
            if (toggleCompanion != null) {
                // The companion (#434): Controls beside the chart,
                // after the one section it holds. A checkbox, because
                // the window is open or it is not, and the tick says
                // which - including when the window's own close box
                // or Escape closed it.
                javax.swing.JCheckBoxMenuItem companion =
                        new javax.swing.JCheckBoxMenuItem(
                                said.say("menu.companion.label"));
                companion.setName(COMPANION_ITEM);
                letters.apply(companion, "menu.companion.mnemonic");
                companion.getAccessibleContext().setAccessibleName(
                        said.say("menu.companion.a11y"));
                juranometria.ui.Explain.selfExplanatory(companion,
                        said.say("menu.companion.explain"));
                companion.addActionListener(event -> toggleCompanion.run());
                view.add(companion);
            }
            if (toggleToolbar != null) {
                // The chart window's own toolbar (#450, ruled on #449):
                // shown or hidden, and the tick says which. Beside
                // Controls, which is where its controls remain while
                // it is hidden; the keystroke from the registry, so the
                // item and every tooltip that names it agree.
                javax.swing.JCheckBoxMenuItem toolbar =
                        new javax.swing.JCheckBoxMenuItem(
                                said.say("menu.toolbar.label"));
                toolbar.setName(TOOLBAR_ITEM);
                letters.apply(toolbar, "menu.toolbar.mnemonic");
                toolbar.setAccelerator(juranometria.ui.Shortcuts.of(
                        juranometria.ui.Shortcuts.TOOLBAR).stroke());
                toolbar.getAccessibleContext().setAccessibleName(
                        said.say("menu.toolbar.a11y"));
                juranometria.ui.Explain.selfExplanatory(toolbar,
                        said.say("menu.toolbar.explain"));
                toolbar.addActionListener(event -> toggleToolbar.run());
                view.add(toolbar);
            }
            if (toggleInspector != null) {
                // A checkbox, because the reader must be able to see
                // whether the inspector is showing - especially when
                // a narrow window has closed it for them (review).
                javax.swing.JCheckBoxMenuItem inspector =
                        new javax.swing.JCheckBoxMenuItem(
                                said.say("menu.inspector.label"));
                inspector.setName(INSPECTOR_ITEM);
                letters.apply(inspector, "menu.inspector.mnemonic");
                inspector.setAccelerator(juranometria.ui.Shortcuts.of(
                        juranometria.ui.Shortcuts.INSPECTOR).stroke());
                inspector.getAccessibleContext().setAccessibleName(
                        said.say("menu.inspector.a11y"));
                juranometria.ui.Explain.selfExplanatory(inspector,
                        said.say("menu.inspector.explain"));
                inspector.addActionListener(event -> toggleInspector.run());
                view.add(inspector);
            }
            if (toggleEcliptic != null) {
                // Below the Inspector's, as the gate drew it. Its
                // full astronomical name, so nothing here depends on
                // a glyph, a zodiac sign or a colour to say what it
                // is.
                javax.swing.JCheckBoxMenuItem ecliptic =
                        new javax.swing.JCheckBoxMenuItem(
                                said.say("menu.ecliptic.label"));
                ecliptic.setName(ECLIPTIC_ITEM);
                letters.apply(ecliptic, "menu.ecliptic.mnemonic");
                ecliptic.getAccessibleContext().setAccessibleName(
                        said.say("menu.ecliptic.a11y"));
                juranometria.ui.Explain.selfExplanatory(ecliptic,
                        // Through the shortcut seam, because
                        // Toggle.sequence() freezes an English "then"
                        // between two pieces of notation (#350).
                        said.say("menu.ecliptic.explain",
                                juranometria.ui.language.ShortcutText.in(said)
                                        .sequence(ChartKeys.prefixText(),
                                                ChartKeys.toggle(
                                                        "module.ecliptic")
                                                        .keyLetter())));
                ecliptic.addActionListener(event -> toggleEcliptic.run());
                view.add(ecliptic);
            }
            if (openSunTable != null || openMoonTable != null
                    || toggleSunOnChart != null || toggleMoonOnChart != null) {
                // View > Solar System (#458, ruled on #457): the Sun and
                // the Moon together - whether each is drawn on the chart,
                // then their tables - in the ruled order. The items are
                // the ones the flat menu held, with their names, letters
                // and words; only their home moved.
                JMenu solar = new JMenu(said.say("menu.solarsystem.label"));
                solar.setName(SOLAR_SYSTEM_MENU);
                letters.apply(solar, "menu.solarsystem.mnemonic");
                solar.getAccessibleContext().setAccessibleName(
                        said.say("menu.solarsystem.a11y"));
                juranometria.ui.Explain.selfExplanatory(solar,
                        said.say("menu.solarsystem.explain"));
                if (toggleSunOnChart != null) {
                    // The Sun on the chart (#415): a layer like the
                    // ecliptic, switched like it. No chart shortcut in
                    // 4.0, so the explanation names none.
                    javax.swing.JCheckBoxMenuItem sunOnChart =
                            new javax.swing.JCheckBoxMenuItem(
                                    said.say("menu.sunchart.label"));
                    sunOnChart.setName(SUN_CHART_ITEM);
                    letters.apply(sunOnChart, "menu.sunchart.mnemonic");
                    sunOnChart.getAccessibleContext().setAccessibleName(
                            said.say("menu.sunchart.a11y"));
                    juranometria.ui.Explain.selfExplanatory(sunOnChart,
                            said.say("menu.sunchart.explain"));
                    sunOnChart.addActionListener(event -> toggleSunOnChart.run());
                    solar.add(sunOnChart);
                }
                if (toggleMoonOnChart != null) {
                    // The Moon on the chart (#416): the Sun's kind of
                    // layer, directly below it, switched the same way.
                    javax.swing.JCheckBoxMenuItem moonOnChart =
                            new javax.swing.JCheckBoxMenuItem(
                                    said.say("menu.moonchart.label"));
                    moonOnChart.setName(MOON_CHART_ITEM);
                    letters.apply(moonOnChart, "menu.moonchart.mnemonic");
                    moonOnChart.getAccessibleContext().setAccessibleName(
                            said.say("menu.moonchart.a11y"));
                    juranometria.ui.Explain.selfExplanatory(moonOnChart,
                            said.say("menu.moonchart.explain"));
                    moonOnChart.addActionListener(event -> toggleMoonOnChart.run());
                    solar.add(moonOnChart);
                }
                if ((toggleSunOnChart != null || toggleMoonOnChart != null)
                        && (openSunTable != null || openMoonTable != null)) {
                    solar.addSeparator();
                }
                if (openSunTable != null) {
                    // The Sun table (#400): where the Sun is for the place
                    // and instant Place and Time owns, as numbers; the
                    // hover says what it reads before the reader chooses it.
                    JMenuItem sun = new JMenuItem(said.say("menu.sun.label"));
                    sun.setName(SUN_ITEM);
                    letters.apply(sun, "menu.sun.mnemonic");
                    sun.getAccessibleContext().setAccessibleName(
                            said.say("menu.sun.a11y"));
                    juranometria.ui.Explain.control(sun,
                            said.say("menu.sun.hover"),
                            said.say("menu.sun.explain"));
                    sun.addActionListener(event -> openSunTable.run());
                    solar.add(sun);
                }
                if (openMoonTable != null) {
                    // The Moon table (#408), under the Sun's, over the same
                    // Place and Time; the hover says so.
                    JMenuItem moon = new JMenuItem(said.say("menu.moon.label"));
                    moon.setName(MOON_ITEM);
                    letters.apply(moon, "menu.moon.mnemonic");
                    moon.getAccessibleContext().setAccessibleName(
                            said.say("menu.moon.a11y"));
                    juranometria.ui.Explain.control(moon,
                            said.say("menu.moon.hover"),
                            said.say("menu.moon.explain"));
                    moon.addActionListener(event -> openMoonTable.run());
                    solar.add(moon);
                }
                view.add(solar);
            }
            if (navigation != null) {
                // Centre-preserving zoom, exactly the toolbar's
                // transition (docs/decisions/pointer-zoom.md): same
                // enablement, same coverage checks, same target
                // preservation, one notification. The accelerators
                // shown here are the primary platform forms; the
                // practical variants (shifted +, keypad add/subtract)
                // bind through installZoomShortcuts.
                view.addSeparator();
                JMenuItem zoomIn =
                        new JMenuItem(said.say("menu.zoomIn.label"));
                zoomIn.getAccessibleContext().setAccessibleName(
                        said.say("menu.zoomIn.a11y"));
                juranometria.ui.Explain.selfExplanatory(zoomIn,
                        said.say("menu.zoomIn.explain"));
                zoomIn.setAccelerator(juranometria.ui.Shortcuts.of(
                        juranometria.ui.Shortcuts.ZOOM_IN).stroke());
                zoomIn.addActionListener(event -> {
                    if (navigation.canZoomIn()) {
                        navigation.zoomIn();
                    }
                });
                JMenuItem zoomOut =
                        new JMenuItem(said.say("menu.zoomOut.label"));
                zoomOut.getAccessibleContext().setAccessibleName(
                        said.say("menu.zoomOut.a11y"));
                juranometria.ui.Explain.selfExplanatory(zoomOut,
                        said.say("menu.zoomOut.explain"));
                zoomOut.setAccelerator(juranometria.ui.Shortcuts.of(
                        juranometria.ui.Shortcuts.ZOOM_OUT).stroke());
                zoomOut.addActionListener(event -> {
                    if (navigation.canZoomOut()) {
                        navigation.zoomOut();
                    }
                });
                // A step the ladder will not take says so, in this
                // menu's own words. The toolbar was repaired first
                // and the menu was not, so the two surfaces told a
                // reader different things about the same ladder
                // (#350 inventory).
                Runnable sayWhatTheLadderAllows = () -> {
                    boolean canIn = navigation.canZoomIn();
                    boolean canOut = navigation.canZoomOut();
                    zoomIn.setEnabled(canIn);
                    zoomOut.setEnabled(canOut);
                    // The closer step (#544) says what it is, and why it
                    // is not offered, in the toolbar's own terms.
                    juranometria.chart.ChartViewState now = navigation.state();
                    juranometria.ui.Explain.selfExplanatory(zoomIn,
                            said.say(canIn
                                    ? (navigation.zoomInTarget()
                                            .map(juranometria.chart.ChartViewState::closerField)
                                            .orElse(false)
                                            ? "menu.zoomIn.closer.explain"
                                            : "menu.zoomIn.explain")
                                    : now.closerStep().isPresent()
                                            ? "menu.zoomIn.closer.unavailable"
                                            : "menu.zoomIn.end"));
                    juranometria.ui.Explain.selfExplanatory(zoomOut,
                            said.say(canOut ? "menu.zoomOut.explain"
                                    : "menu.zoomOut.end"));
                };
                sayWhatTheLadderAllows.run();
                navigation.onChange(state -> sayWhatTheLadderAllows.run());
                navigation.onAvailabilityChange(sayWhatTheLadderAllows);
                view.add(zoomIn);
                view.add(zoomOut);
            }
            bar.add(view);
        }

        JMenu help = new JMenu(said.say("menu.help.label"));
        help.getAccessibleContext().setAccessibleName(
                said.say("menu.help.a11y"));
        juranometria.ui.Explain.selfExplanatory(help,
                said.say("menu.help.explain"));
        if (copyViewReport != null) {
            JMenuItem report = new JMenuItem(
                    said.say("menu.viewReport.label"));
            report.setName(VIEW_REPORT_ITEM);
            report.getAccessibleContext().setAccessibleName(
                    said.say("menu.viewReport.a11y"));
            // The copied report states the observing place and instant,
            // and the reader must be told so before choosing it - on
            // hover and to a screen reader alike.
            juranometria.ui.Explain.control(report,
                    said.say("menu.viewReport.hover"),
                    said.say("menu.viewReport.explain"));
            report.addActionListener(event -> copyViewReport.run());
            help.add(report);
        }
        // The product name is an identity handed to a pattern; the
        // sentence around it belongs to the language (#350).
        JMenuItem about =
                new JMenuItem(said.say("menu.about.label", AppInfo.NAME));
        about.getAccessibleContext().setAccessibleName(
                said.say("menu.about.a11y", AppInfo.NAME));
        juranometria.ui.Explain.selfExplanatory(about,
                said.say("menu.about.explain"));
        about.addActionListener(event -> openAbout.run());
        help.add(about);
        bar.add(help);
        return bar;
    }

    /**
     * Binds the platform zoom shortcuts on the window's root pane in
     * every practical form - the menu-mask '=' key (the unshifted home
     * of '+'), the explicit shifted '+', keypad add, '-', and keypad
     * subtract - all through the same centre-preserving controller
     * transition the toolbar and menu items use. The bindings carry
     * the platform menu shortcut mask (Command on macOS, Ctrl
     * elsewhere), so unmodified '+', '-', '=', and ordinary typing
     * reach the Search field untouched; they work while the window is
     * active, whatever has focus.
     */
    public static void installZoomShortcuts(JRootPane root,
                                            ChartViewController navigation) {
        int mask = menuShortcutMask();
        javax.swing.Action zoomIn = new javax.swing.AbstractAction("Zoom In") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (navigation.canZoomIn()) {
                    navigation.zoomIn();
                }
            }
        };
        javax.swing.Action zoomOut = new javax.swing.AbstractAction("Zoom Out") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (navigation.canZoomOut()) {
                    navigation.zoomOut();
                }
            }
        };
        var inputs = root.getInputMap(
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, mask),
                "chart.zoomIn");
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                mask | InputEvent.SHIFT_DOWN_MASK), "chart.zoomIn");
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, mask),
                "chart.zoomIn");
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, mask),
                "chart.zoomIn");
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, mask),
                "chart.zoomOut");
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, mask),
                "chart.zoomOut");
        root.getActionMap().put("chart.zoomIn", zoomIn);
        root.getActionMap().put("chart.zoomOut", zoomOut);
    }

    /**
     * The platform menu shortcut mask; the Ctrl fallback keeps
     * headless tests running where no toolkit mask exists.
     */
    public static int menuShortcutMask() {
        return juranometria.ui.Shortcuts.menuMask();
    }

    /**
     * Adds View > Solar System > Saturn on the chart directly after
     * Jupiter and moons on the chart (#579; ruling 5 on #578): the one
     * remembered switch for the Saturn module, toggled like Jupiter's.
     * Returns the item, which the session then keeps in step with the
     * module, or null on a bar without the submenu.
     */
    /**
     * Adds View > Centre on coordinates... after Place and Time... and the
     * Controller item that follows it (#623, ruling C-1), with its shortcut. Returns the item, or null on a
     * bar without a View menu.
     */
    public static JMenuItem addCentreOnCoordinates(javax.swing.JMenuBar bar,
            juranometria.ui.language.InterfaceText said, Runnable open) {
        JMenu view = null;
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            if (menu != null && said.say("menu.view.label").equals(menu.getText())) {
                view = menu;
            }
        }
        if (view == null || open == null) {
            return null;
        }
        JMenuItem item = new JMenuItem(said.say("menu.coordinates.label"));
        item.setName("centreOnCoordinatesItem");
        juranometria.ui.language.MnemonicText.in(said).apply(item, "menu.coordinates.mnemonic");
        item.getAccessibleContext().setAccessibleName(said.say("menu.coordinates.a11y"));
        juranometria.ui.Explain.selfExplanatory(item, said.say("menu.coordinates.explain"));
        item.setAccelerator(juranometria.ui.Shortcuts.of(
                juranometria.ui.Shortcuts.COORDINATES).stroke());
        item.addActionListener(event -> open.run());
        // After Place and Time and the Controller item that follows it:
        // the Controller stays directly after the section it holds.
        int after = -1;
        for (int i = 0; i < view.getMenuComponentCount(); i++) {
            if (view.getMenuComponent(i) instanceof JMenuItem existing
                    && (said.say("menu.placeandtime.label").equals(existing.getText())
                            || said.say("menu.companion.label").equals(existing.getText()))) {
                after = i;
            }
        }
        view.insert(item, after + 1);
        return item;
    }

    public static javax.swing.JCheckBoxMenuItem addSaturnOnChart(javax.swing.JMenuBar bar,
            juranometria.ui.language.InterfaceText said, Runnable toggleSaturnOnChart) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || toggleSaturnOnChart == null) {
            return null;
        }
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(
                said.say("menu.saturnchart.label"));
        item.setName("saturnOnChartItem");
        juranometria.ui.language.MnemonicText.in(said).apply(item, "menu.saturnchart.mnemonic");
        item.getAccessibleContext().setAccessibleName(said.say("menu.saturnchart.a11y"));
        juranometria.ui.Explain.selfExplanatory(item, said.say("menu.saturnchart.explain"));
        item.addActionListener(event -> toggleSaturnOnChart.run());
        int after = -1;
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            String name = solar.getMenuComponent(i).getName();
            if ("jupiterOnChartItem".equals(name) || MOON_CHART_ITEM.equals(name)
                    || SUN_CHART_ITEM.equals(name)) {
                after = i;
            }
        }
        solar.insert(item, after + 1);
        return item;
    }

    /** Uranus on the chart's item (#638), after Saturn's, named by the Moon's rule. */
    public static final String URANUS_CHART_ITEM =
            juranometria.solarchart.SolarSystemModule.URANUS
                    + MOON_CHART_ITEM.substring(juranometria.solarchart.SolarSystemModule.MOON.length());

    /**
     * Adds View > Solar System > Uranus on the chart directly after Saturn on
     * the chart (#638), outward from the Sun: the one remembered switch for
     * Uranus's layer. No access letter: in Norwegian none of its letters is
     * free on this menu, and every existing letter is kept, as for Mars on
     * the chart (#606). Returns the item, or null on a bar without the
     * submenu.
     */
    public static javax.swing.JCheckBoxMenuItem addUranusOnChart(javax.swing.JMenuBar bar,
            juranometria.ui.language.InterfaceText said, Runnable toggleUranusOnChart) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || toggleUranusOnChart == null) {
            return null;
        }
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(
                said.say("menu.uranuschart.label"));
        item.setName(URANUS_CHART_ITEM);
        item.getAccessibleContext().setAccessibleName(said.say("menu.uranuschart.a11y"));
        juranometria.ui.Explain.selfExplanatory(item, said.say("menu.uranuschart.explain"));
        item.addActionListener(event -> toggleUranusOnChart.run());
        int after = -1;
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            String name = solar.getMenuComponent(i).getName();
            if ("saturnOnChartItem".equals(name) || "jupiterOnChartItem".equals(name)
                    || MARS_CHART_ITEM.equals(name) || MOON_CHART_ITEM.equals(name)
                    || SUN_CHART_ITEM.equals(name)) {
                after = i;
            }
        }
        solar.insert(item, after + 1);
        return item;
    }

    /**
     * Adds View > Solar System > Jupiter... after Moon... (#474): the
     * Jupiter table's dialog, kept beside the Controller's Jupiter group
     * during dogfooding as the Sun's and the Moon's are. Does nothing on
     * a bar without the Solar System submenu.
     */
    /**
     * Adds View > Solar System > Jupiter and moons on the chart directly
     * after Moon on the chart (#484; ruling 1 on #482): the one
     * remembered switch for the Jovian module, toggled like the Sun's
     * and the Moon's. Returns the item, which the session then keeps in
     * step with the module, or null on a bar without the submenu.
     */
    public static javax.swing.JCheckBoxMenuItem addJupiterOnChart(javax.swing.JMenuBar bar,
            juranometria.ui.language.InterfaceText said, Runnable toggleJupiterOnChart) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || toggleJupiterOnChart == null) {
            return null;
        }
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(
                said.say("menu.jupiterchart.label"));
        item.setName("jupiterOnChartItem");
        juranometria.ui.language.MnemonicText.in(said).apply(item, "menu.jupiterchart.mnemonic");
        item.getAccessibleContext().setAccessibleName(said.say("menu.jupiterchart.a11y"));
        juranometria.ui.Explain.selfExplanatory(item, said.say("menu.jupiterchart.explain"));
        item.addActionListener(event -> toggleJupiterOnChart.run());
        int after = -1;
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            java.awt.Component c = solar.getMenuComponent(i);
            if (MOON_CHART_ITEM.equals(c.getName()) || SUN_CHART_ITEM.equals(c.getName())) {
                after = i;
            }
        }
        solar.insert(item, after + 1);
        return item;
    }

    /**
     * Adds View > Solar System > Venus on the chart directly after Sun
     * on the chart (#514; the ruled order outward from the Sun): the one
     * remembered switch for Venus's layer, toggled like the Sun's and the
     * Moon's. Returns the item, which the session then keeps in step with
     * the module, or null on a bar without the submenu.
     */
    public static javax.swing.JCheckBoxMenuItem addVenusOnChart(javax.swing.JMenuBar bar,
            juranometria.ui.language.InterfaceText said, Runnable toggleVenusOnChart) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || toggleVenusOnChart == null) {
            return null;
        }
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(
                said.say("menu.venuschart.label"));
        item.setName(VENUS_CHART_ITEM);
        juranometria.ui.language.MnemonicText.in(said).apply(item, "menu.venuschart.mnemonic");
        item.getAccessibleContext().setAccessibleName(said.say("menu.venuschart.a11y"));
        juranometria.ui.Explain.selfExplanatory(item, said.say("menu.venuschart.explain"));
        item.addActionListener(event -> toggleVenusOnChart.run());
        // After Sun on the chart, and after Mercury on the chart when the bar
        // has it (#542): the order outward from the Sun, whichever is added first.
        int after = -1;
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            String name = solar.getMenuComponent(i).getName();
            if (SUN_CHART_ITEM.equals(name) || MERCURY_CHART_ITEM.equals(name)) {
                after = i;
            }
        }
        solar.insert(item, after + 1);
        return item;
    }

    /**
     * Adds View > Solar System > Mars on the chart directly after Moon on
     * the chart (#606), outward from the Sun and so before Jupiter and moons
     * on the chart: the one remembered switch for Mars's layer. No access
     * letter: none of its letters is free on this menu in Norwegian, and
     * every existing letter is kept, as ruled for Mars... on #603. Returns
     * the item, or null on a bar without the submenu.
     */
    public static javax.swing.JCheckBoxMenuItem addMarsOnChart(javax.swing.JMenuBar bar,
            juranometria.ui.language.InterfaceText said, Runnable toggleMarsOnChart) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || toggleMarsOnChart == null) {
            return null;
        }
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(
                said.say("menu.marschart.label"));
        item.setName(MARS_CHART_ITEM);
        item.getAccessibleContext().setAccessibleName(said.say("menu.marschart.a11y"));
        juranometria.ui.Explain.selfExplanatory(item, said.say("menu.marschart.explain"));
        item.addActionListener(event -> toggleMarsOnChart.run());
        int after = -1;
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            if (MOON_CHART_ITEM.equals(solar.getMenuComponent(i).getName())) {
                after = i;
            }
        }
        solar.insert(item, after + 1);
        return item;
    }

    /**
     * Adds View > Solar System > Mercury on the chart between Sun on the
     * chart and Venus on the chart (#542, ruling 3 on #539): the one
     * remembered switch for Mercury's layer. Returns the item, or null on a
     * bar without the submenu.
     */
    public static javax.swing.JCheckBoxMenuItem addMercuryOnChart(javax.swing.JMenuBar bar,
            juranometria.ui.language.InterfaceText said, Runnable toggleMercuryOnChart) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || toggleMercuryOnChart == null) {
            return null;
        }
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(
                said.say("menu.mercurychart.label"));
        item.setName(MERCURY_CHART_ITEM);
        juranometria.ui.language.MnemonicText.in(said).apply(item, "menu.mercurychart.mnemonic");
        item.getAccessibleContext().setAccessibleName(said.say("menu.mercurychart.a11y"));
        juranometria.ui.Explain.selfExplanatory(item, said.say("menu.mercurychart.explain"));
        item.addActionListener(event -> toggleMercuryOnChart.run());
        int after = -1;
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            if (SUN_CHART_ITEM.equals(solar.getMenuComponent(i).getName())) {
                after = i;
            }
        }
        solar.insert(item, after + 1);
        return item;
    }

    /**
     * Adds View > Solar System > Venus... between Sun... and Moon... (#502):
     * the Venus table's dialog, kept beside the Controller's Venus group
     * as the Sun's and the Moon's are. Does nothing on a bar without the
     * Solar System submenu.
     */
    public static void addVenusTable(javax.swing.JMenuBar bar,
                                     juranometria.ui.language.InterfaceText said,
                                     Runnable openVenusTable) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || openVenusTable == null) {
            return;
        }
        JMenuItem venus = new JMenuItem(said.say("menu.venus.label"));
        venus.setName("venusTableItem");
        juranometria.ui.language.MnemonicText.in(said).apply(venus, "menu.venus.mnemonic");
        venus.getAccessibleContext().setAccessibleName(said.say("menu.venus.a11y"));
        juranometria.ui.Explain.control(venus, said.say("menu.venus.hover"),
                said.say("menu.venus.explain"));
        venus.addActionListener(event -> openVenusTable.run());
        int at = solar.getMenuComponentCount();
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            if (MOON_ITEM.equals(solar.getMenuComponent(i).getName())) {
                at = i;
            }
        }
        solar.insert(venus, at);
    }

    /**
     * Adds View > Solar System > Mercury... between Sun... and Venus...
     * (#530): the Mercury table's dialog, kept beside the Controller's
     * Mercury group as the others are - the order outward from the Sun.
     * Does nothing on a bar without the Solar System submenu.
     */
    public static void addMercuryTable(javax.swing.JMenuBar bar,
                                       juranometria.ui.language.InterfaceText said,
                                       Runnable openMercuryTable) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || openMercuryTable == null) {
            return;
        }
        JMenuItem mercury = new JMenuItem(said.say("menu.mercury.label"));
        mercury.setName("mercuryTableItem");
        juranometria.ui.language.MnemonicText.in(said).apply(mercury, "menu.mercury.mnemonic");
        mercury.getAccessibleContext().setAccessibleName(said.say("menu.mercury.a11y"));
        juranometria.ui.Explain.control(mercury, said.say("menu.mercury.hover"),
                said.say("menu.mercury.explain"));
        mercury.addActionListener(event -> openMercuryTable.run());
        int at = solar.getMenuComponentCount();
        for (int i = solar.getMenuComponentCount() - 1; i >= 0; i--) {
            String name = solar.getMenuComponent(i).getName();
            if ("venusTableItem".equals(name) || MOON_ITEM.equals(name)) {
                at = i;
            }
        }
        solar.insert(mercury, at);
    }

    public static void addJupiterTable(javax.swing.JMenuBar bar,
                                       juranometria.ui.language.InterfaceText said,
                                       Runnable openJupiterTable) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || openJupiterTable == null) {
            return;
        }
        JMenuItem jupiter = new JMenuItem(said.say("menu.jupiter.label"));
        jupiter.setName("jupiterTableItem");
        juranometria.ui.language.MnemonicText.in(said).apply(jupiter,
                "menu.jupiter.mnemonic");
        jupiter.getAccessibleContext().setAccessibleName(said.say("menu.jupiter.a11y"));
        juranometria.ui.Explain.control(jupiter, said.say("menu.jupiter.hover"),
                said.say("menu.jupiter.explain"));
        jupiter.addActionListener(event -> openJupiterTable.run());
        solar.add(jupiter);
    }

    /**
     * Adds View > Solar System > Mars... between Moon... and Jupiter...
     * (#603), outward from the Sun: the Mars table's dialog, kept beside the
     * Controller's Mars group for dogfooding as Saturn's is. Opening it
     * computes nothing. No Mars on the chart item: no Mars is drawn yet.
     */
    public static void addMarsTable(javax.swing.JMenuBar bar,
                                    juranometria.ui.language.InterfaceText said,
                                    Runnable openMarsTable) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || openMarsTable == null) {
            return;
        }
        JMenuItem mars = new JMenuItem(said.say("menu.mars.label"));
        mars.setName("marsTableItem");
        // No access letter, as ruled on #603 (2026-10-26): every letter of
        // "Mars" is already claimed on this menu, in English and in Norwegian,
        // two items never share one, and every existing letter is kept.
        // Revisited only if the Solar System menu is reorganized.
        mars.getAccessibleContext().setAccessibleName(said.say("menu.mars.a11y"));
        juranometria.ui.Explain.control(mars, said.say("menu.mars.hover"),
                said.say("menu.mars.explain"));
        mars.addActionListener(event -> openMarsTable.run());
        int at = solar.getMenuComponentCount();
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            if (MOON_ITEM.equals(solar.getMenuComponent(i).getName())) {
                at = i + 1;
            }
        }
        solar.insert(mars, at);
    }

    /**
     * Adds View > Solar System > Saturn... directly after Jupiter... (#576):
     * the Saturn table's dialog, kept beside the Controller's Saturn group
     * for dogfooding as Jupiter's is. Opening it computes nothing. No
     * Saturn on the chart item: no Saturn is drawn yet.
     */
    public static void addSaturnTable(javax.swing.JMenuBar bar,
                                      juranometria.ui.language.InterfaceText said,
                                      Runnable openSaturnTable) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || openSaturnTable == null) {
            return;
        }
        JMenuItem saturn = new JMenuItem(said.say("menu.saturn.label"));
        saturn.setName("saturnTableItem");
        juranometria.ui.language.MnemonicText.in(said).apply(saturn, "menu.saturn.mnemonic");
        saturn.getAccessibleContext().setAccessibleName(said.say("menu.saturn.a11y"));
        juranometria.ui.Explain.control(saturn, said.say("menu.saturn.hover"),
                said.say("menu.saturn.explain"));
        saturn.addActionListener(event -> openSaturnTable.run());
        int at = solar.getMenuComponentCount();
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            if ("jupiterTableItem".equals(solar.getMenuComponent(i).getName())) {
                at = i + 1;
            }
        }
        solar.insert(saturn, at);
    }

    /**
     * Adds View > Solar System > Uranus... directly after Saturn... (#635),
     * outward from the Sun: the Uranus table's dialog, kept beside the
     * Controller's Uranus group for dogfooding as Mars's is. Opening it
     * computes nothing. No Uranus on the chart item: no Uranus is drawn yet.
     */
    public static void addUranusTable(javax.swing.JMenuBar bar,
                                      juranometria.ui.language.InterfaceText said,
                                      Runnable openUranusTable) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || openUranusTable == null) {
            return;
        }
        JMenuItem uranus = new JMenuItem(said.say("menu.uranus.label"));
        uranus.setName("uranusTableItem");
        juranometria.ui.language.MnemonicText.in(said).apply(uranus, "menu.uranus.mnemonic");
        uranus.getAccessibleContext().setAccessibleName(said.say("menu.uranus.a11y"));
        juranometria.ui.Explain.control(uranus, said.say("menu.uranus.hover"),
                said.say("menu.uranus.explain"));
        uranus.addActionListener(event -> openUranusTable.run());
        int at = solar.getMenuComponentCount();
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            if ("saturnTableItem".equals(solar.getMenuComponent(i).getName())) {
                at = i + 1;
            }
        }
        solar.insert(uranus, at);
    }

    /**
     * Adds View > Solar System > Neptune... directly after Uranus... (#656),
     * outward from the Sun: the Neptune table's dialog, kept beside the
     * Controller's Neptune group for dogfooding as Uranus's is. Opening it
     * computes nothing. No Neptune on the chart item: no Neptune is drawn yet.
     */
    public static void addNeptuneTable(javax.swing.JMenuBar bar,
                                       juranometria.ui.language.InterfaceText said,
                                       Runnable openNeptuneTable) {
        JMenu solar = solarSystemMenu(bar);
        if (solar == null || openNeptuneTable == null) {
            return;
        }
        JMenuItem neptune = new JMenuItem(said.say("menu.neptune.label"));
        neptune.setName("neptuneTableItem");
        // No access letter, on Mars's ruling (#603, 2026-10-26): every letter
        // of "Neptune" and of "Neptun" is already claimed on this menu, two
        // items never share one, and every existing letter is kept.
        neptune.getAccessibleContext().setAccessibleName(said.say("menu.neptune.a11y"));
        juranometria.ui.Explain.control(neptune, said.say("menu.neptune.hover"),
                said.say("menu.neptune.explain"));
        neptune.addActionListener(event -> openNeptuneTable.run());
        int at = solar.getMenuComponentCount();
        for (int i = 0; i < solar.getMenuComponentCount(); i++) {
            if ("uranusTableItem".equals(solar.getMenuComponent(i).getName())) {
                at = i + 1;
            }
        }
        solar.insert(neptune, at);
    }
}
