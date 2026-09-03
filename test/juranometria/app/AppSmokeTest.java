package juranometria.app;

import com.formdev.flatlaf.FlatLaf;
import org.junit.jupiter.api.Test;

import javax.swing.UIManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSmokeTest {

    @Test
    void flatLafInstallsAsLookAndFeel() throws Exception {
        // Under the shared guard (#224): this test installed FlatLaf
        // in the suite's first minutes and walked away for
        // twenty-six sprints, quietly theming every test that ran
        // after it in the same JVM. The gate's widened scan (#241)
        // finally caught it.
        juranometria.app.SwingSession.restoring(() -> {
            UiTheme.apply();
            assertInstanceOf(FlatLaf.class, UIManager.getLookAndFeel());
        });
    }

    @Test
    void versionResourceIsBundledSemver() {
        assertTrue(AppInfo.version().matches("\\d+\\.\\d+\\.\\d+"),
                "expected semver, got: " + AppInfo.version());
    }
}
