package juranometria.app;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

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

    /**
     * @param openSettings runs on the Settings... item (may be null
     *     while no settings exist, omitting the item)
     * @param openChartOptions runs on the View menu's Chart Options...
     *     item (may be null, omitting the View menu)
     * @param openAbout runs on the About item
     */
    public static JMenuBar create(Runnable openSettings,
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
}
