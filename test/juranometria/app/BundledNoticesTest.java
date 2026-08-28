package juranometria.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Packaging guard: the third-party notices required by bundled resources
 * must ship on the classpath, so a release cannot silently omit them.
 */
class BundledNoticesTest {

    @Test
    void tablerLicenseNoticeShipsBesideTheIcons() {
        String license = resourceText("/resources/icons/LICENSE");
        assertTrue(license.contains("MIT License"));
        assertTrue(license.contains("Copyright (c)"),
                "the upstream copyright line must be present");
        assertTrue(license.contains("shall be included in all"),
                "the permission notice must be present");
    }

    @Test
    void everyBundledIconIsOnTheClasspath() {
        for (String icon : new String[] {
                "zoom-in", "zoom-out", "zoom-reset", "minus", "plus"}) {
            assertNotNull(BundledNoticesTest.class.getResource(
                            "/resources/icons/" + icon + ".svg"),
                    icon + ".svg must ship as a resource");
        }
    }

    private static String resourceText(String resource) {
        try (InputStream stream = BundledNoticesTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, resource + " must ship on the classpath");
            return new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
