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
    void theConstellationGeographyPackShipsWithItsLicenceAndNotice() {
        String license = resourceText(
                "/resources/geo/constellations/LICENSE-BSD-3-Clause.txt");
        assertTrue(license.contains("Copyright (c) 2015, Olaf Frohn"),
                "the upstream copyright line must be present");
        assertTrue(license.contains(
                        "Redistribution and use in source and binary forms"),
                "the complete BSD licence text must be present");

        String notice = resourceText(
                "/resources/geo/constellations/NOTICE-constellations.md");
        assertTrue(notice.contains("d3-celestial"));
        assertTrue(notice.contains("BSD-3-Clause"));
        assertTrue(notice.contains("Delporte"),
                "the boundary provenance must be credited");
        assertTrue(notice.contains("not an IAU standard"),
                "figures must never be described as an IAU standard");
    }

    @Test
    void everyConstellationGeographyResourceShipsOnTheClasspath() {
        for (String resource : new String[] {
                "manifest.properties", "constellations.csv", "figures.csv",
                "boundaries.csv"}) {
            assertNotNull(BundledNoticesTest.class.getResource(
                            "/resources/geo/constellations/" + resource),
                    resource + " must ship as a resource");
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
