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

    private AppMenuBar() {
    }

    /** The menu bar without navigation; for wiring-level tests. */
    public static JMenuBar create(Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout) {
        return create(null, openSettings, openChartOptions, openAbout);
    }

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
    public static JMenuBar create(ChartViewController navigation,
                                  Runnable openSettings,
                                  Runnable openChartOptions,
                                  Runnable openAbout) {
        if (openAbout == null) {
            throw new IllegalArgumentException("about action is required");
        }
        JMenuBar bar = new JMenuBar();

        if (openSettings != null) {
            JMenu application = new JMenu("File");
            application.getAccessibleContext().setAccessibleName(
                    "File menu");
            JMenuItem settings = new JMenuItem("Settings...");
            settings.getAccessibleContext().setAccessibleName("Settings");
            settings.getAccessibleContext().setAccessibleDescription(
                    "Application appearance settings");
            settings.addActionListener(event -> openSettings.run());
            application.add(settings);
            bar.add(application);
        }

        if (openChartOptions != null) {
            JMenu view = new JMenu("View");
            view.getAccessibleContext().setAccessibleName("View menu");
            JMenuItem chartOptions = new JMenuItem("Chart Options...");
            chartOptions.setMnemonic('C');
            chartOptions.getAccessibleContext().setAccessibleName(
                    "Chart Options");
            chartOptions.getAccessibleContext().setAccessibleDescription(
                    "Choose which chart content and labels draw");
            chartOptions.addActionListener(event -> openChartOptions.run());
            view.add(chartOptions);
            if (navigation != null) {
                // Centre-preserving zoom, exactly the toolbar's
                // transition (docs/decisions/pointer-zoom.md): same
                // enablement, same coverage checks, same target
                // preservation, one notification. The accelerators
                // shown here are the primary platform forms; the
                // practical variants (shifted +, keypad add/subtract)
                // bind through installZoomShortcuts.
                view.addSeparator();
                JMenuItem zoomIn = new JMenuItem("Zoom In");
                zoomIn.getAccessibleContext().setAccessibleName("Zoom In");
                zoomIn.setAccelerator(KeyStroke.getKeyStroke(
                        KeyEvent.VK_EQUALS, menuShortcutMask()));
                zoomIn.addActionListener(event -> {
                    if (navigation.canZoomIn()) {
                        navigation.zoomIn();
                    }
                });
                JMenuItem zoomOut = new JMenuItem("Zoom Out");
                zoomOut.getAccessibleContext().setAccessibleName("Zoom Out");
                zoomOut.setAccelerator(KeyStroke.getKeyStroke(
                        KeyEvent.VK_MINUS, menuShortcutMask()));
                zoomOut.addActionListener(event -> {
                    if (navigation.canZoomOut()) {
                        navigation.zoomOut();
                    }
                });
                navigation.onChange(state -> {
                    zoomIn.setEnabled(navigation.canZoomIn());
                    zoomOut.setEnabled(navigation.canZoomOut());
                });
                view.add(zoomIn);
                view.add(zoomOut);
            }
            bar.add(view);
        }

        JMenu help = new JMenu("Help");
        help.getAccessibleContext().setAccessibleName("Help menu");
        JMenuItem about = new JMenuItem("About " + AppInfo.NAME);
        about.getAccessibleContext().setAccessibleName("About " + AppInfo.NAME);
        about.getAccessibleContext().setAccessibleDescription(
                "Application identity, version, and licensing");
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
        try {
            return java.awt.Toolkit.getDefaultToolkit()
                    .getMenuShortcutKeyMaskEx();
        } catch (java.awt.HeadlessException e) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }
}
