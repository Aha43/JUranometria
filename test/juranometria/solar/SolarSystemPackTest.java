package juranometria.solar;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import juranometria.catalog.PackIntegrityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled Solar System pack loads, verifies itself and says what
 * it is (issue #399).
 */
class SolarSystemPackTest {

    @Test
    void theBundledPackLoadsAndStatesItself() {
        SolarSystemPack pack = SolarSystemPack.load();
        assertEquals("solar-system", pack.manifest().get("pack.name"));
        assertEquals("1", pack.manifest().get("pack.version"));
        assertEquals("DE440", pack.manifest().get("ephemeris"));
        assertEquals("3,10,399", pack.manifest().get("ephemeris.bodies"),
                "the bodies are explicit in the manifest");
        assertEquals(3, pack.kernel().segments().size());
        assertEquals(LocalDate.of(1972, 1, 1), pack.leapSeconds().first());
        assertTrue(pack.leapSeconds().expires().isAfter(LocalDate.of(2026, 1, 1)),
                "the pinned record vouches for the present");
        for (String key : List.of("source.de440s.url", "source.de440s.sha256",
                "source.leapseconds.url", "source.leapseconds.sha256",
                "audit.date", "terms",
                "checksum.juranometria-de440-sun-emb-earth-1900-2100.bsp",
                "checksum.Leap_Second.dat", "checksum.NOTICE-solar-system.md",
                "checksum.PROVENANCE.md")) {
            assertTrue(pack.manifest().containsKey(key), "manifest carries " + key);
        }
    }

    @Test
    void aCorruptKernelIsRefusedBeforeUse() {
        Function<String, InputStream> tampered = name -> {
            InputStream real = SolarSystemPack.class.getResourceAsStream(
                    SolarSystemPack.RESOURCE_ROOT + name);
            if (!name.endsWith(".bsp")) {
                return real;
            }
            try (real) {
                byte[] bytes = real.readAllBytes();
                bytes[bytes.length / 2] ^= 0x01;
                return new ByteArrayInputStream(bytes);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        };
        PackIntegrityException refused = assertThrows(
                PackIntegrityException.class, () -> SolarSystemPack.load(tampered));
        assertTrue(refused.getMessage().contains("fails its checksum"),
                refused.getMessage());
    }

    @Test
    void aMissingResourceAndAWrongPackAreRefused() {
        assertThrows(PackIntegrityException.class,
                () -> SolarSystemPack.load(name -> null));
        Function<String, InputStream> wrong = name ->
                name.equals("manifest.properties")
                        ? new ByteArrayInputStream(
                                "format.version=1\npack.name=other\n"
                                        .getBytes(StandardCharsets.UTF_8))
                        : null;
        PackIntegrityException refused = assertThrows(
                PackIntegrityException.class, () -> SolarSystemPack.load(wrong));
        assertTrue(refused.getMessage().contains("other"));
    }

    @Test
    void theNoticeAndProvenanceShipBesideTheData() throws Exception {
        for (String name : List.of("NOTICE-solar-system.md", "PROVENANCE.md",
                "manifest.properties", "Leap_Second.dat")) {
            try (InputStream in = SolarSystemPack.class.getResourceAsStream(
                    SolarSystemPack.RESOURCE_ROOT + name)) {
                assertTrue(in != null, name + " is on the classpath");
            }
        }
        String notice;
        try (InputStream in = SolarSystemPack.class.getResourceAsStream(
                SolarSystemPack.RESOURCE_ROOT + "NOTICE-solar-system.md")) {
            notice = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String required : List.of("modified kernel", "NAIF", "DE440",
                "not an original JPL file", "Leap_Second.dat", "unmodified",
                "doi:10.3847/1538-3881/abd414")) {
            assertTrue(notice.contains(required), "the notice says: " + required);
        }
    }
}
