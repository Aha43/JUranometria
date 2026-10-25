package juranometria.solar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
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
 * The bundled Mars System pack (issue #602, as ruled on #601, M-7a with
 * the exclusive boundary): Phobos and Deimos about the Mars barycentre
 * from MAR099, 2000 up to, not including, 2050-01-01 00:00 TDB; their
 * radii verbatim from NAIF's PCK; refused whole when corrupt; and every
 * released kernel beside it byte-identical.
 */
class MarsSystemPackTest {

    static final String MOONS_KERNEL = "juranometria-mar099-moons-2000-2050.bsp";

    /** The released kernels, by resource: Solar System, Jovian, planets and Saturn System. */
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
            "/resources/saturn-system/juranometria-sat441-saturn-1900-2100.bsp",
            "28438fff5d8cf7c401390f6e109be4733398e591902c2ff5f471ee257374758d",
            "/resources/saturn-system/juranometria-sat441-moons-2000-2100.bsp",
            "8844bf082d391006c4b73c98c325bea8fb22ce0c31b3c61371a872bbeb6c297b");

    @Test
    void theBundledPackLoadsAndStatesItself() {
        MarsSystemPack pack = MarsSystemPack.load();
        Map<String, String> m = pack.manifest();
        assertEquals("mars-system", m.get("pack.name"));
        assertEquals("1", m.get("pack.version"));
        assertEquals("MAR099 with DE440", m.get("ephemeris"));
        assertEquals(List.of(401, 402), pack.moons(), "Phobos and Deimos, outward (#601, M-7a)");
        assertEquals(LocalDate.of(2000, 1, 1), pack.moonsFirstDay());
        assertEquals(2049, pack.moonsLastYear(), "the reader is told 2000-2049");
        assertEquals(2469807.5, pack.moonsEndJdTdb(), "2050-01-01 00:00 TDB, exclusive");
        assertEquals(13.0, pack.moonRadiiKm().get(401), "Phobos's largest PCK radius");
        assertEquals(7.8, pack.moonRadiiKm().get(402), "Deimos's");
        assertEquals("13.0,11.4,9.1", m.get("pck.BODY401_RADII"), "verbatim");
        assertEquals("7.8,6.0,5.1", m.get("pck.BODY402_RADII"), "verbatim");
        assertEquals("997dc93ba640e476da7a494d2237dcdeb145e528db37be8ccee588c615e4e1ff",
                m.get("source.mar099s.sha256"));
    }

    @Test
    void theMoonsCoverTheirYearsAndEndExactlyAtTheExclusiveBoundary() {
        MarsSystemPack pack = MarsSystemPack.load();
        SpkKernel k = pack.moonsKernel();
        double start = (2451544.5 - 31.0 - 2451545.0) * 86400.0;
        double end = (pack.moonsEndJdTdb() - 2451545.0) * 86400.0;
        assertEquals(2, k.segments().size());
        for (SpkKernel.Segment s : k.segments()) {
            assertEquals(4, s.center(), "about the Mars barycentre");
            assertTrue(s.startEt() <= start, s.target() + " starts with the margin before 2000");
            assertEquals(end, s.endEt(), s.target() + " ends at mar099s's own end");
        }
        for (int moon : pack.moons()) {
            double r = k.state(4, moon, end - 86400.0).position().length();
            assertTrue(r > 9000.0 && r < 24000.0, moon + " is a Mars moon a day before the end: " + r);
        }
    }

    @Test
    void theFileIsTheOneTheManifestNames() throws IOException {
        Map<String, String> m = MarsSystemPack.load().manifest();
        try (InputStream in = MarsSystemPack.class.getResourceAsStream(
                MarsSystemPack.RESOURCE_ROOT + MOONS_KERNEL)) {
            byte[] bytes = in.readAllBytes();
            assertEquals(m.get("checksum." + MOONS_KERNEL), Sha256.hex(bytes));
            assertEquals(43951104, bytes.length,
                    "the #601 cut, 43 910 144 bytes, plus the comment area NAIF's rules require");
        }
    }

    @Test
    void everyReleasedKernelIsByteIdenticalBesideIt() throws IOException {
        for (Map.Entry<String, String> r : RELEASED.entrySet()) {
            try (InputStream in = MarsSystemPackTest.class.getResourceAsStream(r.getKey())) {
                assertEquals(r.getValue(), Sha256.hex(in.readAllBytes()),
                        r.getKey() + " is the file its release shipped");
            }
        }
    }

    @Test
    void aCorruptKernelIsRefusedBeforeUse() throws IOException {
        byte[] bytes;
        try (InputStream in = MarsSystemPack.class.getResourceAsStream(
                MarsSystemPack.RESOURCE_ROOT + MOONS_KERNEL)) {
            bytes = in.readAllBytes();
        }
        bytes[bytes.length / 2] ^= 0x01;
        byte[] tampered = bytes;
        PackIntegrityException refused = assertThrows(PackIntegrityException.class,
                () -> MarsSystemPack.load(name -> name.equals(MOONS_KERNEL)
                        ? new ByteArrayInputStream(tampered)
                        : MarsSystemPack.class.getResourceAsStream(MarsSystemPack.RESOURCE_ROOT + name)));
        assertTrue(refused.getMessage().contains("fails its checksum"), refused.getMessage());
    }

    @Test
    void aWrongPackIsRefused() {
        PackIntegrityException wrong = assertThrows(PackIntegrityException.class,
                () -> MarsSystemPack.load(name -> SaturnPack.class.getResourceAsStream(
                        SaturnPack.RESOURCE_ROOT + name)));
        assertTrue(wrong.getMessage().contains("expected pack mars-system"), wrong.getMessage());
    }
}
