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

    @Test
    void tycho2NoticeShipsWithTheStarResourceAndStatesTheNcTerms() {
        String notice = resourceText("/resources/catalog/m31/NOTICE-tycho2.md");
        assertTrue(notice.contains("CC BY-NC 3.0 IGO"));
        assertTrue(notice.contains("creativecommons.org/licenses/by-nc/3.0/igo"),
                "the canonical license link must be present");
        assertTrue(notice.contains("Hog E."), "the Tycho-2 attribution must be present");
        assertTrue(notice.contains("may not be used commercially"),
                "the non-commercial restriction must be stated");
    }

    @Test
    void openNgcNoticeAndLicenseTextShipWithTheDsoResource() {
        String notice = resourceText("/resources/catalog/m31/NOTICE-openngc.md");
        assertTrue(notice.contains("CC-BY-SA-4.0"));
        assertTrue(notice.contains("Mattia Verga"), "the OpenNGC attribution must be present");
        String license = resourceText("/resources/catalog/m31/LICENSE-CC-BY-SA-4.0.txt");
        assertTrue(license.contains("Attribution-ShareAlike 4.0"),
                "the complete CC-BY-SA-4.0 text must ship beside the data");
    }

    @Test
    void generatedCatalogueResourcesShipWithProvenance() {
        for (String resource : new String[] {
                "/resources/catalog/m31/stars.csv",
                "/resources/catalog/m31/dsos.csv",
                "/resources/catalog/m31/PROVENANCE.md"}) {
            assertNotNull(BundledNoticesTest.class.getResource(resource),
                    resource + " must ship on the classpath");
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
