package juranometria.bookmark;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Where the bookmark document lives (#497, ruled C-5): the system's own
 * per-user data folder, and a hidden system property for another folder
 * (for tests and a later portable launch; no interface offers it).
 */
public final class BookmarkFolder {

    /** The system property naming another folder. */
    public static final String PROPERTY = "juranometria.bookmarks.dir";

    private BookmarkFolder() {
    }

    /** The folder for this system, user and environment. */
    public static Path resolve(String osName, String userHome, Map<String, String> environment,
                               String override) {
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return Path.of(userHome, "Library", "Application Support", "JUranometria");
        }
        if (os.contains("win")) {
            String appData = environment.get("APPDATA");
            return appData != null && !appData.isBlank()
                    ? Path.of(appData, "JUranometria")
                    : Path.of(userHome, "AppData", "Roaming", "JUranometria");
        }
        String xdg = environment.get("XDG_DATA_HOME");
        return xdg != null && !xdg.isBlank() && Path.of(xdg).isAbsolute()
                ? Path.of(xdg, "juranometria")
                : Path.of(userHome, ".local", "share", "juranometria");
    }

    /** The folder for the running process. */
    public static Path current() {
        return resolve(System.getProperty("os.name"), System.getProperty("user.home"),
                System.getenv(), System.getProperty(PROPERTY));
    }
}
