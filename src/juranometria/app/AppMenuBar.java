package juranometria.app;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * The application's restrained conventional menu bar: an app-named
 * menu carrying Settings and a Help menu carrying About - nothing
 * else, and deliberately no placeholder items for future features.
 * Actions are injected so the wiring is testable headless and the
 * menu never reaches into chart state.
 */
public final class AppMenuBar {

    private AppMenuBar() {
    }

    /**
     * @param openSettings runs on the Settings... item (may be null
     *     while no settings exist, omitting the item)
     * @param openAbout runs on the About item
     */
    public static JMenuBar create(Runnable openSettings, Runnable openAbout) {
        if (openAbout == null) {
            throw new IllegalArgumentException("about action is required");
        }
        JMenuBar bar = new JMenuBar();

        if (openSettings != null) {
            JMenu application = new JMenu(AppInfo.NAME);
            application.getAccessibleContext().setAccessibleName(
                    AppInfo.NAME + " menu");
            JMenuItem settings = new JMenuItem("Settings...");
            settings.getAccessibleContext().setAccessibleName("Settings");
            settings.getAccessibleContext().setAccessibleDescription(
                    "Application appearance settings");
            settings.addActionListener(event -> openSettings.run());
            application.add(settings);
            bar.add(application);
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
