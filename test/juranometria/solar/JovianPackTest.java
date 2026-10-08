package juranometria.solar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.catalog.PackIntegrityException;
import juranometria.solar.spk.SpkKernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled Jovian pack (issue #473, layout A as ruled on #472): one
 * logical, versioned pack of two kernel files behind one manifest and
 * one validation boundary, the planetary constants carried verbatim
 * from NAIF's PCK, refused whole when any file is missing or corrupt -
 * and the released Solar System pack beside it byte-identical to what
 * 5.0.0 shipped.
 */
class JovianPackTest {

    /** The released kernel's digest, as 5.0.0 shipped it (PR #468). */
    static final String RELEASED_SOLAR_KERNEL_SHA256 =
            "63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580";

    /** JUP365's split, JD 2450464.5 TDB (1997-01-16), measured in #472. */
    static final double SPLIT_ET = (2450464.5 - 2451545.0) * 86400.0;

    @Test
    void theBundledPackLoadsAndStatesItself() {
        JovianPack pack = JovianPack.load();
        Map<String, String> m = pack.manifest();
        assertEquals("jovian-system", m.get("pack.name"));
        assertEquals("1", m.get("pack.version"));
        assertEquals("JUP365 with DE440", m.get("ephemeris"));
        assertEquals("5,599", m.get("ephemeris.jupiter.bodies"));
        assertEquals("501,502,503,504", m.get("ephemeris.moons.bodies"));
        assertEquals(LocalDate.of(1900, 1, 1), pack.jupiterFirstDay());
        assertEquals(LocalDate.of(2100, 12, 31), pack.jupiterLastDay());
        assertEquals(LocalDate.of(2000, 1, 1), pack.moonsFirstDay());
        assertEquals(LocalDate.of(2100, 12, 31), pack.moonsLastDay());
        assertEquals(3, pack.jupiterKernel().segments().size(),
                "the barycentre's one segment and both of Jupiter's");
        assertEquals(4, pack.moonsKernel().segments().size(),
                "one segment per moon, the later of JUP365's two");
        assertTrue(m.get("terms").contains("nothing of IMCCE's"));
    }

    @Test
    void theConstantsAreThePckValuesTheDecisionRecordCites() {
        // docs/decisions/jovian-system.md, "Frames, with their meanings
        // frozen": the IAU 2015 radii and pole as pck00011.tpc states
        // them (sha256 3dff7b1d…), copied verbatim by the builder.
        JovianPack.Constants k = JovianPack.load().constants();
        assertEquals(71492.0, k.jupiterEquatorialRadiusKm());
        assertEquals(66854.0, k.jupiterPolarRadiusKm());
        assertEquals(268.056595, k.poleRa0Degrees());
        assertEquals(-0.006499, k.poleRa1DegreesPerCentury());
        assertEquals(64.495303, k.poleDec0Degrees());
        assertEquals(0.002413, k.poleDec1DegreesPerCentury());
        assertEquals(695700.0, k.sunRadiusKm());
        assertEquals(SolarSystemService.SOLAR_RADIUS_KM, k.sunRadiusKm(),
                "the Sun's radius the Sun's contract already uses");
        assertEquals(Map.of(501, 1829.4, 502, 1562.6, 503, 2631.2, 504, 2410.3),
                k.moonEquatorialRadiiKm());
        Map<String, String> m = JovianPack.load().manifest();
        assertEquals("71492,71492,66854", m.get("pck.BODY599_RADII"), "verbatim");
        assertEquals("268.056595,-0.006499,0.", m.get("pck.BODY599_POLE_RA"));
        assertEquals("3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1",
                m.get("source.pck.sha256"));
    }

    @Test
    void jupitersKernelCarriesBothSegmentsAndTheMoonsOnlyTheLater() {
        JovianPack pack = JovianPack.load();
        List<SpkKernel.Segment> jupiter = pack.jupiterKernel().segments(5, 599);
        assertEquals(2, jupiter.size());
        assertEquals(SPLIT_ET, jupiter.get(0).endEt(), "the first ends at the split");
        assertEquals(SPLIT_ET, jupiter.get(1).startEt(), "the second begins there");
        assertEquals(1, pack.jupiterKernel().segments(0, 5).size(), "DE440's barycentre");
        for (int moon : new int[] {501, 502, 503, 504}) {
            List<SpkKernel.Segment> of = pack.moonsKernel().segments(5, moon);
            assertEquals(1, of.size(), "one segment for " + moon);
            assertTrue(of.get(0).startEt() >= SPLIT_ET, "from the later segment");
            assertTrue(of.get(0).startEt() < (2451544.5 - 31.0 - 2451545.0) * 86400.0,
                    "covering 2000-01-01 with the margin");
        }
    }

    @Test
    void theReleasedSolarSystemPackIsByteIdenticalBesideIt() {
        assertEquals(RELEASED_SOLAR_KERNEL_SHA256, SolarSystemPack.load().manifest()
                        .get("checksum.juranometria-de440-sun-emb-earth-moon-1900-2100.bsp"),
                "the Sun's and the Moon's kernel is the one 5.0.0 shipped");
        assertEquals(RELEASED_SOLAR_KERNEL_SHA256,
                JovianPack.load().manifest().get("solar-system.kernel.sha256"),
                "and the Jovian pack's builder proved it before writing");
    }

    @Test
    void aCorruptKernelIsRefusedBeforeUse() throws IOException {
        byte[] bytes;
        try (InputStream in = JovianPack.class.getResourceAsStream(
                JovianPack.RESOURCE_ROOT + "juranometria-jup365-galilean-2000-2100.bsp")) {
            bytes = in.readAllBytes();
        }
        bytes[bytes.length / 2] ^= 0x01;
        byte[] tampered = bytes;
        PackIntegrityException refused = assertThrows(PackIntegrityException.class,
                () -> JovianPack.load(name -> name.endsWith("galilean-2000-2100.bsp")
                        ? new ByteArrayInputStream(tampered)
                        : JovianPack.class.getResourceAsStream(JovianPack.RESOURCE_ROOT + name)));
        assertTrue(refused.getMessage().contains("fails its checksum"), refused.getMessage());
    }

    @Test
    void aMissingResourceAndAWrongPackAreRefused() {
        PackIntegrityException missing = assertThrows(PackIntegrityException.class,
                () -> JovianPack.load(name -> name.equals("manifest.properties")
                        ? JovianPack.class.getResourceAsStream(JovianPack.RESOURCE_ROOT + name)
                        : null));
        assertTrue(missing.getMessage().contains("is missing"), missing.getMessage());
        PackIntegrityException wrong = assertThrows(PackIntegrityException.class,
                () -> JovianPack.load(name -> SolarSystemPack.class.getResourceAsStream(
                        SolarSystemPack.RESOURCE_ROOT + name)));
        assertTrue(wrong.getMessage().contains("expected pack jovian-system"),
                wrong.getMessage());
        assertThrows(PackIntegrityException.class, () -> JovianPack.load(name -> null));
    }
}
