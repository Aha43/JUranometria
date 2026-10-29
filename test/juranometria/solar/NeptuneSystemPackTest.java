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
 * The bundled Neptune System pack (issue #655, as ruled on #654): the Neptune
 * barycentre from DE442 (N-1a, N-2) and Triton and Nereid about it from
 * NEP098 (N-6a); the radii, the pole and its periodic term verbatim from
 * NAIF's PCK; refused whole when corrupt; and every released kernel beside
 * it byte-identical, Uranus's included.
 */
class NeptuneSystemPackTest {

    static final String NEPTUNE_KERNEL = "juranometria-de442-neptune-1900-2100.bsp";
    static final String MOONS_KERNEL = "juranometria-nep098-moons-2000-2100.bsp";

    /** The released kernels, by resource. */
    static final Map<String, String> RELEASED = Map.ofEntries(
            Map.entry("/resources/solar-system/juranometria-de440-sun-emb-earth-moon-1900-2100.bsp",
                    "63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580"),
            Map.entry("/resources/jovian-system/juranometria-jup365-jupiter-1900-2100.bsp",
                    "efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92"),
            Map.entry("/resources/jovian-system/juranometria-jup365-galilean-2000-2100.bsp",
                    "6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6"),
            Map.entry("/resources/planets/juranometria-de440-venus-1900-2100.bsp",
                    "8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12"),
            Map.entry("/resources/planets/juranometria-de440-mercury-1900-2100.bsp",
                    "78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d"),
            Map.entry("/resources/planets/juranometria-de440-mars-1900-2100.bsp",
                    "c59b91b0188dc540906916c067238c8d311fd718f65b909825720e54e22e7ffb"),
            Map.entry("/resources/saturn-system/juranometria-sat441-saturn-1900-2100.bsp",
                    "28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d"),
            Map.entry("/resources/saturn-system/juranometria-sat441-moons-2000-2100.bsp",
                    "8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b"),
            Map.entry("/resources/mars-system/juranometria-mar099-moons-2000-2050.bsp",
                    "cd84a9bfd574ebaeec63893d30735129de030e7e361af6c0f465f634629bc208"),
            Map.entry("/resources/uranus-system/juranometria-de442-uranus-1900-2100.bsp",
                    "21964f569b7fe33b2a5648090215942e86542af8a5718f93fa5987259af25c9e"),
            Map.entry("/resources/uranus-system/juranometria-ura182-moons-2000-2100.bsp",
                    "6f8ff87802472a0c30f4c1779d6b1dec004922ce5573795200b518a6ebc19180"));

    @Test
    void theBundledPackLoadsAndStatesItself() {
        NeptuneSystemPack pack = NeptuneSystemPack.load();
        Map<String, String> m = pack.manifest();
        assertEquals("neptune-system", m.get("pack.name"));
        assertEquals("1", m.get("pack.version"));
        assertEquals("NEP098 with DE442", m.get("ephemeris"));
        assertEquals(List.of(801, 802), pack.moons(), "outward from Neptune (#654, N-6a)");
        assertEquals("24764,24764,24341", m.get("pck.BODY899_RADII"), "verbatim");
        assertEquals("0.70,0.,0.,0.,0.,0.,0.,0.", m.get("pck.BODY899_NUT_PREC_RA"), "the periodic term (N-4)");
        assertEquals("-0.51,0.,0.,0.,0.,0.,0.,0.", m.get("pck.BODY899_NUT_PREC_DEC"), "the periodic term (N-4)");
        assertTrue(m.get("pck.BODY8_NUT_PREC_ANGLES").startsWith("357.85,52.316,"), "the angle N first");
        assertEquals(1352.6, pack.moonRadiiKm().get(801), "Triton's largest PCK radius");
        assertEquals(170.0, pack.moonRadiiKm().get(802), "Nereid's");
        assertEquals("5327f47abba438d993237ff676ab03b728778a965bb238f86de883c0ad7790b8",
                m.get("source.nep098.sha256"));
    }

    @Test
    void theKernelsCoverTheirYearsWithTheMargins() {
        NeptuneSystemPack pack = NeptuneSystemPack.load();
        double start = (2415020.5 - 31.0 - 2451545.0) * 86400.0;
        double moonsStart = (2451544.5 - 31.0 - 2451545.0) * 86400.0;
        double end = (2488434.5 + 31.0 - 2451545.0) * 86400.0;
        SpkKernel neptune = pack.neptuneKernel();
        for (SpkKernel.Segment s : neptune.segments()) {
            assertEquals(0, s.center());
            assertEquals(8, s.target(), "the barycentre stands for Neptune (N-2)");
            assertTrue(s.name().strip().startsWith("DE-0442"), "DE442's (N-1a)");
            assertTrue(s.startEt() <= start && s.endEt() >= end);
        }
        SpkKernel moons = pack.moonsKernel();
        for (int moon : pack.moons()) {
            List<SpkKernel.Segment> segments = moons.segments(8, moon);
            assertTrue(!segments.isEmpty(), moon + " about the Neptune barycentre");
            assertTrue(segments.get(0).startEt() <= moonsStart, moon + " starts with the margin before 2000");
            assertTrue(segments.get(segments.size() - 1).endEt() >= end, moon + " runs past 2100");
            double r = moons.state(8, moon, 0.0).position().length();
            assertTrue(r > 300000.0 && r < 10000000.0, moon + " is a Neptune moon at J2000: " + r);
        }
    }

    @Test
    void theFilesAreTheOnesTheManifestNames() throws IOException {
        Map<String, String> m = NeptuneSystemPack.load().manifest();
        for (Map.Entry<String, Integer> k : Map.of(NEPTUNE_KERNEL, 416768, MOONS_KERNEL, 50302976).entrySet()) {
            try (InputStream in = NeptuneSystemPack.class.getResourceAsStream(NeptuneSystemPack.root() + k.getKey())) {
                byte[] bytes = in.readAllBytes();
                assertEquals(m.get("checksum." + k.getKey()), Sha256.hex(bytes));
                assertEquals((int) k.getValue(), bytes.length,
                        "the #654 cut plus the comment area NAIF's rules require: " + k.getKey());
            }
        }
    }

    @Test
    void everyReleasedKernelIsByteIdenticalBesideIt() throws IOException {
        for (Map.Entry<String, String> r : RELEASED.entrySet()) {
            try (InputStream in = NeptuneSystemPackTest.class.getResourceAsStream(r.getKey())) {
                assertEquals(r.getValue(), Sha256.hex(in.readAllBytes()),
                        r.getKey() + " is the file its release shipped");
            }
        }
    }

    @Test
    void aCorruptKernelIsRefusedBeforeUse() throws IOException {
        byte[] bytes;
        try (InputStream in = NeptuneSystemPack.class.getResourceAsStream(NeptuneSystemPack.root() + MOONS_KERNEL)) {
            bytes = in.readAllBytes();
        }
        bytes[bytes.length / 2] ^= 0x01;
        byte[] tampered = bytes;
        PackIntegrityException refused = assertThrows(PackIntegrityException.class,
                () -> NeptuneSystemPack.load(name -> name.equals(MOONS_KERNEL)
                        ? new ByteArrayInputStream(tampered)
                        : NeptuneSystemPack.class.getResourceAsStream(NeptuneSystemPack.root() + name)));
        assertTrue(refused.getMessage().contains("fails its checksum"), refused.getMessage());
    }

    @Test
    void aWrongPackIsRefused() {
        PackIntegrityException wrong = assertThrows(PackIntegrityException.class,
                () -> NeptuneSystemPack.load(name -> SaturnPack.class.getResourceAsStream(
                        SaturnPack.RESOURCE_ROOT + name)));
        assertTrue(wrong.getMessage().contains("expected the neptune-system pack"), wrong.getMessage());
    }
}
