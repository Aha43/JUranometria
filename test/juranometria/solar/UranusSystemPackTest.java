package juranometria.solar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.catalog.PackIntegrityException;
import juranometria.catalog.Sha256;
import juranometria.solar.spk.SpkKernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled Uranus System pack (issue #634, as ruled on #633): the Uranus
 * barycentre from DE442 (U-1a, U-2) and the five moons about it from
 * URA182.21 (U-6a); the radii and pole verbatim from NAIF's PCK; refused
 * whole when corrupt; and every released kernel beside it byte-identical.
 */
class UranusSystemPackTest {

    static final String URANUS_KERNEL = "juranometria-de442-uranus-1900-2100.bsp";
    static final String MOONS_KERNEL = "juranometria-ura182-moons-2000-2100.bsp";

    /** The released kernels, by resource. */
    static final Map<String, String> RELEASED = Map.of(
            "/resources/solar-system/juranometria-de440-sun-emb-earth-moon-1900-2100.bsp",
            "63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580",
            "/resources/jovian-system/juranometria-jup365-jupiter-1900-2100.bsp",
            "efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92",
            "/resources/jovian-system/juranometria-jup365-galilean-2000-2100.bsp",
            "6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6",
            "/resources/planets/juranometria-de440-venus-1900-2100.bsp",
            "8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12",
            "/resources/planets/juranometria-de440-mercury-1900-2100.bsp",
            "78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d",
            "/resources/planets/juranometria-de440-mars-1900-2100.bsp",
            "c59b91b0188dc540906916c067238c8d311fd718f65b909825720e54e22e7ffb",
            "/resources/saturn-system/juranometria-sat441-saturn-1900-2100.bsp",
            "28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d",
            "/resources/saturn-system/juranometria-sat441-moons-2000-2100.bsp",
            "8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b",
            "/resources/mars-system/juranometria-mar099-moons-2000-2050.bsp",
            "cd84a9bfd574ebaeec63893d30735129de030e7e361af6c0f465f634629bc208");

    @Test
    void theBundledPackLoadsAndStatesItself() {
        UranusSystemPack pack = UranusSystemPack.load();
        Map<String, String> m = pack.manifest();
        assertEquals("uranus-system", m.get("pack.name"));
        assertEquals("1", m.get("pack.version"));
        assertEquals("URA182.21 with DE442", m.get("ephemeris"));
        assertEquals(List.of(705, 701, 702, 703, 704), pack.moons(), "outward from Uranus (#633, U-6a)");
        assertEquals("25559,25559,24973", m.get("pck.BODY799_RADII"), "verbatim");
        assertEquals(240.4, pack.moonRadiiKm().get(705), "Miranda's largest PCK radius");
        assertEquals(788.9, pack.moonRadiiKm().get(703), "Titania's");
        assertEquals("273cd4ccc470d1098562cd4e94fa48fc2b4c6b1c2896308e473c949b87937c53",
                m.get("source.ura184.sha256"));
    }

    @Test
    void theKernelsCoverTheirYearsWithTheMargins() {
        UranusSystemPack pack = UranusSystemPack.load();
        double start = (2415020.5 - 31.0 - 2451545.0) * 86400.0;
        double moonsStart = (2451544.5 - 31.0 - 2451545.0) * 86400.0;
        double end = (2488434.5 + 31.0 - 2451545.0) * 86400.0;
        SpkKernel uranus = pack.uranusKernel();
        for (SpkKernel.Segment s : uranus.segments()) {
            assertEquals(0, s.center());
            assertEquals(7, s.target(), "the barycentre stands for Uranus (U-2)");
            assertTrue(s.name().strip().startsWith("DE-0442"), "DE442's (U-1a)");
            assertTrue(s.startEt() <= start && s.endEt() >= end);
        }
        SpkKernel moons = pack.moonsKernel();
        for (int moon : pack.moons()) {
            List<SpkKernel.Segment> segments = moons.segments(7, moon);
            assertTrue(!segments.isEmpty(), moon + " about the Uranus barycentre");
            assertTrue(segments.get(0).startEt() <= moonsStart, moon + " starts with the margin before 2000");
            assertTrue(segments.get(segments.size() - 1).endEt() >= end, moon + " runs past 2100");
            double r = moons.state(7, moon, 0.0).position().length();
            assertTrue(r > 120000.0 && r < 600000.0, moon + " is a Uranus moon at J2000: " + r);
        }
    }

    @Test
    void theFilesAreTheOnesTheManifestNames() throws IOException {
        Map<String, String> m = UranusSystemPack.load().manifest();
        for (Map.Entry<String, Integer> k : Map.of(URANUS_KERNEL, 387072, MOONS_KERNEL, 38055936).entrySet()) {
            try (InputStream in = UranusSystemPack.class.getResourceAsStream(UranusSystemPack.root() + k.getKey())) {
                byte[] bytes = in.readAllBytes();
                assertEquals(m.get("checksum." + k.getKey()), Sha256.hex(bytes));
                assertEquals((int) k.getValue(), bytes.length,
                        "the #633 cut plus the comment area NAIF's rules require: " + k.getKey());
            }
        }
    }

    @Test
    void everyReleasedKernelIsByteIdenticalBesideIt() throws IOException {
        for (Map.Entry<String, String> r : RELEASED.entrySet()) {
            try (InputStream in = UranusSystemPackTest.class.getResourceAsStream(r.getKey())) {
                assertEquals(r.getValue(), Sha256.hex(in.readAllBytes()),
                        r.getKey() + " is the file its release shipped");
            }
        }
    }

    @Test
    void aCorruptKernelIsRefusedBeforeUse() throws IOException {
        byte[] bytes;
        try (InputStream in = UranusSystemPack.class.getResourceAsStream(UranusSystemPack.root() + MOONS_KERNEL)) {
            bytes = in.readAllBytes();
        }
        bytes[bytes.length / 2] ^= 0x01;
        byte[] tampered = bytes;
        PackIntegrityException refused = assertThrows(PackIntegrityException.class,
                () -> UranusSystemPack.load(name -> name.equals(MOONS_KERNEL)
                        ? new ByteArrayInputStream(tampered)
                        : UranusSystemPack.class.getResourceAsStream(UranusSystemPack.root() + name)));
        assertTrue(refused.getMessage().contains("fails its checksum"), refused.getMessage());
    }

    @Test
    void aWrongPackIsRefused() {
        PackIntegrityException wrong = assertThrows(PackIntegrityException.class,
                () -> UranusSystemPack.load(name -> SaturnPack.class.getResourceAsStream(
                        SaturnPack.RESOURCE_ROOT + name)));
        assertTrue(wrong.getMessage().contains("expected the uranus-system pack"), wrong.getMessage());
    }
}
