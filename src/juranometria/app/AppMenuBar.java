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
            JMenu each = bar.getMenu(menu);
            for (int item = 0; item < each.getItemCount(); item++) {
                JMenuItem candidate = each.getItem(item);
                if (candidate != null
                        && name.equals(candidate.getName())) {
                    return candidate;
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
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu menu = bar.getMenu(i);
            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item instanceof javax.swing.JCheckBoxMenuItem box
                        && name.equals(box.getName())) {
                    return box;
                }
            }
        }
        return null;
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
                    juranometria.ui.Explain.selfExplanatory(zoomIn,
                            said.say(canIn ? "menu.zoomIn.explain"
                                    : "menu.zoomIn.end"));
                    juranometria.ui.Explain.selfExplanatory(zoomOut,
                            said.say(canOut ? "menu.zoomOut.explain"
                                    : "menu.zoomOut.end"));
                };
                sayWhatTheLadderAllows.run();
                navigation.onChange(state -> sayWhatTheLadderAllows.run());
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
}
