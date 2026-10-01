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
    void theStarIdentityPackShipsWithItsLicenceAndNotice() {
        String license = resourceText(
                "/resources/catalog/star-identities/LICENSE-BSD-3-Clause.txt");
        assertTrue(license.contains("Copyright (c) 2015, Olaf Frohn"),
                "the upstream copyright line must be present");
        assertTrue(license.contains(
                        "Redistribution and use in source and binary forms"),
                "the complete BSD licence text must be present");

        String notice = resourceText(
                "/resources/catalog/star-identities/NOTICE-star-identities.md");
        assertTrue(notice.contains("d3-celestial"));
        assertTrue(notice.contains("BSD-3-Clause"));
        assertTrue(notice.contains("traditional star names"),
                "the names are described as traditional");
        assertTrue(notice.replaceAll("\\s+", " ")
                        .contains("does not claim per-name IAU certification"),
                "the fact-versus-convention honesty travels to the end user");
        assertTrue(notice.contains("not IAU standards"),
                "designation systems are never described as IAU standards");
    }

    @Test
    void everyStarIdentityResourceShipsOnTheClasspath() {
        for (String resource : new String[] {
                "manifest.properties", "star-identities.csv",
                "NOTICE-star-identities.md", "LICENSE-BSD-3-Clause.txt"}) {
            assertNotNull(BundledNoticesTest.class.getResource(
                            "/resources/catalog/star-identities/" + resource),
                    resource + " must ship as a resource");
        }
    }

    @Test
    void theSolarSystemPackShipsWithItsNoticeAndProvenance() {
        String notice = resourceText(
                "/resources/solar-system/NOTICE-solar-system.md");
        assertTrue(notice.contains("modified kernel"),
                "the excerpt is called what NAIF's rules call it");
        assertTrue(notice.contains("not an original JPL file"),
                "and never presented as JPL's own");
        assertTrue(notice.contains("NAIF") && notice.contains("DE440"));
        assertTrue(notice.contains("doi:10.3847/1538-3881/abd414"),
                "the ephemeris authors are acknowledged");
        assertTrue(notice.contains("IERS") && notice.contains("unmodified"),
                "the leap-second file is the IERS's, unchanged");
        String provenance = resourceText("/resources/solar-system/PROVENANCE.md");
        for (String required : new String[] {"naif.jpl.nasa.gov", "hpiers.obspm.fr",
                "SHA-256", "SpkExcerpt", "Validation", "identical to the source"}) {
            assertTrue(provenance.contains(required),
                    "the provenance records " + required);
        }
    }

    @Test
    void everySolarSystemResourceShipsOnTheClasspath() {
        for (String resource : new String[] {
                "manifest.properties",
                "juranometria-de440-sun-emb-earth-moon-1900-2100.bsp",
                "Leap_Second.dat", "NOTICE-solar-system.md", "PROVENANCE.md"}) {
            assertNotNull(BundledNoticesTest.class.getResource(
                            "/resources/solar-system/" + resource),
                    resource + " must ship as a resource");
        }
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
