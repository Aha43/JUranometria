package juranometria.app;

import java.io.InputStream;

/** Static application identity and version information. */
public final class AppInfo {

    public static final String NAME = "JUranometria";
    public static final String REPO_URL = "https://github.com/Aha43/JUranometria";

    private AppInfo() {
    }

    /** Reads the application version from the bundled VERSION resource. */
    public static String version() {
        try (InputStream stream = AppInfo.class.getResourceAsStream("/VERSION")) {
            if (stream == null) {
                return "unknown";
            }
            return new String(stream.readAllBytes()).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
