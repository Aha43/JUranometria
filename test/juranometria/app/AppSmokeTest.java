package juranometria.app;

import com.formdev.flatlaf.FlatLaf;
import org.junit.jupiter.api.Test;

import javax.swing.UIManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSmokeTest {

    @Test
    void flatLafInstallsAsLookAndFeel() {
        UiTheme.apply();
        assertInstanceOf(FlatLaf.class, UIManager.getLookAndFeel());
    }

    @Test
    void versionResourceIsBundledSemver() {
        assertTrue(AppInfo.version().matches("\\d+\\.\\d+\\.\\d+"),
                "expected semver, got: " + AppInfo.version());
    }
}
