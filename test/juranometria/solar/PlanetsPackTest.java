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
 * The bundled planets pack (issue #501, layout C as ruled on #500): one
 * logical, versioned pack with one kernel file per planet behind one
 * manifest and one validation boundary, Venus first and Mercury second
 * (#529, version 2); the radii carried verbatim from NAIF's PCK; refused
 * whole when any file is missing or corrupt - and the released Solar
 * System and Jovian packs beside it byte-identical to what they shipped,
 * Venus's own released file among them.
 */
class PlanetsPackTest {

    /** The released kernels' digests: the Solar System pack's (5.0.0) and the Jovian pack's (5.1.0). */
    static final Map<String, String> RELEASED = Map.of(
            "/resources/solar-system/juranometria-de440-sun-emb-earth-moon-1900-2100.bsp",
            "63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580",
            "/resources/jovian-system/juranometria-jup365-jupiter-1900-2100.bsp",
            "efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92",
            "/resources/jovian-system/juranometria-jup365-galilean-2000-2100.bsp",
            "6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6");

    static final String VENUS_KERNEL = "juranometria-de440-venus-1900-2100.bsp";
    static final String MERCURY_KERNEL = "juranometria-de440-mercury-1900-2100.bsp";

    /**
     * Venus's file as 5.2.0 shipped it (docs/decisions/venus-system.md,
     * "#501, as built"), and Mercury's as the #528 study cut it
     * (docs/studies/mercury/pack-measurements.md): the builder added
     * the second without rewriting the first.
     */
    static final String VENUS_KERNEL_SHA256 =
            "8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12";
    static final String MERCURY_KERNEL_SHA256 =
            "78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d";

    @Test
    void theBundledPackLoadsAndStatesItself() {
        PlanetsPack pack = PlanetsPack.load();
        Map<String, String> m = pack.manifest();
        assertEquals("planets", m.get("pack.name"));
        assertEquals("2", m.get("pack.version"), "Mercury joined in version 2 (#529)");
        assertEquals("DE440", m.get("ephemeris"));
        assertEquals(List.of("venus", "mercury"), pack.planets(),
                "Venus first, Mercury appended; a release appends");
        PlanetsPack.Planet venus = pack.planet("venus");
        assertEquals(VENUS_KERNEL, m.get("ephemeris.venus.kernel"),
                "one file per planet, named for it");
        assertEquals(List.of(2, 299), venus.bodies(), "the barycentre, then the planet");
        assertEquals(LocalDate.of(1900, 1, 1), venus.firstDay());
        assertEquals(LocalDate.of(2100, 12, 31), venus.lastDay());
        assertEquals(2, venus.kernel().segments().size());
        assertEquals(1, venus.kernel().segments(0, 2).size());
        assertEquals(1, venus.kernel().segments(2, 299).size());
        assertEquals("1", m.get("ephemeris.venus.added.in.version"));
        PlanetsPack.Planet mercury = pack.planet("mercury");
        assertEquals(MERCURY_KERNEL, m.get("ephemeris.mercury.kernel"));
        assertEquals(List.of(1, 199), mercury.bodies(), "the barycentre, then the planet");
        assertEquals(LocalDate.of(1900, 1, 1), mercury.firstDay());
        assertEquals(LocalDate.of(2100, 12, 31), mercury.lastDay());
        assertEquals(2, mercury.kernel().segments().size());
        assertEquals(1, mercury.kernel().segments(0, 1).size());
        assertEquals(1, mercury.kernel().segments(1, 199).size());
        assertEquals("2", m.get("ephemeris.mercury.added.in.version"));
        PackIntegrityException absent = assertThrows(PackIntegrityException.class,
                () -> pack.planet("mars"));
        assertTrue(absent.getMessage().contains("[venus, mercury]"), absent.getMessage());
    }

    @Test
    void venussReleasedFileWasNotRewrittenWhenMercurysWasAdded() throws IOException {
        Map<String, String> m = PlanetsPack.load().manifest();
        assertEquals(VENUS_KERNEL_SHA256, m.get("checksum." + VENUS_KERNEL),
                "the manifest still names the 5.2.0 file");
        assertEquals(MERCURY_KERNEL_SHA256, m.get("checksum." + MERCURY_KERNEL),
                "and the file the #528 study measured, 3 299 328 bytes");
        for (String[] k : new String[][] {{VENUS_KERNEL, VENUS_KERNEL_SHA256, "1242112"},
                {MERCURY_KERNEL, MERCURY_KERNEL_SHA256, "3299328"}}) {
            try (InputStream in = PlanetsPack.class.getResourceAsStream(PlanetsPack.root() + k[0])) {
                byte[] bytes = in.readAllBytes();
                assertEquals(Integer.parseInt(k[2]), bytes.length, k[0]);
                assertEquals(k[1], Sha256.hex(bytes), k[0] + " is byte for byte the file named");
            }
        }
    }

    @Test
    void theRadiiAreThePckValuesTheDecisionRecordCites() {
        // docs/decisions/venus-system.md, "The authority": Venus's
        // solid-body radius and the Sun's as pck00011.tpc states them
        // (sha256 3dff7b1d…), copied verbatim by the builder.
        PlanetsPack pack = PlanetsPack.load();
        assertEquals(6051.8, pack.planet("venus").radiusKm());
        // docs/decisions/mercury-system.md, "The ruling", 4: the equatorial
        // 2440.53 km of the triaxial figure, the mean never substituted.
        assertEquals(2440.53, pack.planet("mercury").radiusKm());
        assertEquals(695700.0, pack.sunRadiusKm());
        assertEquals(SolarSystemService.SOLAR_RADIUS_KM, pack.sunRadiusKm(),
                "the Sun's radius the Sun's contract already uses");
        Map<String, String> m = pack.manifest();
        assertEquals("6051.8,6051.8,6051.8", m.get("pck.BODY299_RADII"), "verbatim");
        assertEquals("2440.53,2440.53,2438.26", m.get("pck.BODY199_RADII"), "verbatim, triaxial");
        assertEquals("3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1",
                m.get("source.pck.sha256"));
        assertEquals("c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2",
                m.get("source.de440s.sha256"), "the released pack's own pinned source");
    }

    @Test
    void venusIsItsBarycentreAndCoversTheReleasedIntervalWithItsMargin() {
        SpkKernel k = PlanetsPack.load().planet("venus").kernel();
        double start = (2415020.5 - 31.0 - 2451545.0) * 86400.0;
        double end = (2488434.5 + 31.0 - 2451545.0) * 86400.0;
        for (SpkKernel.Segment s : k.segments()) {
            assertTrue(s.startEt() <= start && s.endEt() >= end,
                    s.center() + " -> " + s.target() + " covers 1900-2100 with the margin");
        }
        int states = 0;
        for (double et = start; et <= end; et += 17.0 * 86400.0) {
            SpkKernel.State s = k.state(2, 299, et);
            assertEquals(0.0, s.position().length(), "Venus has no moon");
            assertEquals(0.0, s.velocity().length());
            states++;
        }
        assertTrue(states > 4000);
    }

    @Test
    void mercuryIsItsBarycentreAndCoversTheReleasedIntervalWithItsMargin() {
        SpkKernel k = PlanetsPack.load().planet("mercury").kernel();
        double start = (2415020.5 - 31.0 - 2451545.0) * 86400.0;
        double end = (2488434.5 + 31.0 - 2451545.0) * 86400.0;
        for (SpkKernel.Segment s : k.segments()) {
            assertTrue(s.startEt() <= start && s.endEt() >= end,
                    s.center() + " -> " + s.target() + " covers 1900-2100 with the margin");
        }
        int states = 0;
        for (double et = start; et <= end; et += 17.0 * 86400.0) {
            SpkKernel.State s = k.state(1, 199, et);
            assertEquals(0.0, s.position().length(), "Mercury has no moon");
            assertEquals(0.0, s.velocity().length());
            states++;
        }
        assertTrue(states > 4000);
    }

    @Test
    void theReleasedSolarSystemAndJovianPacksAreByteIdenticalBesideIt() throws IOException {
        for (Map.Entry<String, String> r : RELEASED.entrySet()) {
            try (InputStream in = PlanetsPackTest.class.getResourceAsStream(r.getKey())) {
                assertEquals(r.getValue(), Sha256.hex(in.readAllBytes()),
                        r.getKey() + " is the file its release shipped");
            }
        }
        assertEquals(RELEASED.get("/resources/solar-system/"
                        + "juranometria-de440-sun-emb-earth-moon-1900-2100.bsp"),
                PlanetsPack.load().manifest().get("solar-system.kernel.sha256"),
                "and the planets pack's builder proved it before writing");
    }

    @Test
    void aCorruptKernelIsRefusedBeforeUse() throws IOException {
        byte[] bytes;
        try (InputStream in = PlanetsPack.class.getResourceAsStream(
                PlanetsPack.root() + VENUS_KERNEL)) {
            bytes = in.readAllBytes();
        }
        bytes[bytes.length / 2] ^= 0x01;
        byte[] tampered = bytes;
        PackIntegrityException refused = assertThrows(PackIntegrityException.class,
                () -> PlanetsPack.load(name -> name.equals(VENUS_KERNEL)
                        ? new ByteArrayInputStream(tampered)
                        : PlanetsPack.class.getResourceAsStream(PlanetsPack.root() + name)));
        assertTrue(refused.getMessage().contains("fails its checksum"), refused.getMessage());
    }

    @Test
    void aMissingResourceAndAWrongPackAreRefused() {
        PackIntegrityException missing = assertThrows(PackIntegrityException.class,
                () -> PlanetsPack.load(name -> name.equals("manifest.properties")
                        ? PlanetsPack.class.getResourceAsStream(PlanetsPack.root() + name)
                        : null));
        assertTrue(missing.getMessage().contains("is missing"), missing.getMessage());
        PackIntegrityException wrong = assertThrows(PackIntegrityException.class,
                () -> PlanetsPack.load(name -> JovianPack.class.getResourceAsStream(
                        JovianPack.RESOURCE_ROOT + name)));
        assertTrue(wrong.getMessage().contains("expected the planets pack"), wrong.getMessage());
        assertThrows(PackIntegrityException.class, () -> PlanetsPack.load(name -> null));
    }

    @Test
    void aCorruptMercuryKernelIsRefusedBeforeUseAndRefusesVenusToo() throws IOException {
        // One validation boundary (#500, layout C): the pack is whole or refused.
        byte[] bytes;
        try (InputStream in = PlanetsPack.class.getResourceAsStream(
                PlanetsPack.root() + MERCURY_KERNEL)) {
            bytes = in.readAllBytes();
        }
        bytes[bytes.length / 3] ^= 0x01;
        byte[] tampered = bytes;
        PackIntegrityException refused = assertThrows(PackIntegrityException.class,
                () -> PlanetsPack.load(name -> name.equals(MERCURY_KERNEL)
                        ? new ByteArrayInputStream(tampered)
                        : PlanetsPack.class.getResourceAsStream(PlanetsPack.root() + name)));
        assertTrue(refused.getMessage().contains("fails its checksum"), refused.getMessage());
    }

    @Test
    void aMissingPlanetsPackLeavesTheSunTheMoonAndJupiterAnswering() {
        assertThrows(PackIntegrityException.class, () -> new VenusService(
                SolarSystemPack.load(), PlanetsPack.load(name -> null)));
        assertThrows(PackIntegrityException.class, () -> new MercuryService(
                SolarSystemPack.load(), PlanetsPack.load(name -> null)));
        // The others read their own packs and never this one.
        assertTrue(SolarSystemService.load().timeScales() != null);
        assertTrue(JovianSystemService.load().pack() != null);
    }
}
