package juranometria.solar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * The bundled Saturn pack (issue #575, Jupiter's layout as ruled on
 * #574): one logical, versioned pack of two kernel files behind one
 * manifest and one validation boundary - Saturn and its barycentre over
 * 1900–2100, the seven ruled moons over 2000–2100, both of SAT441's
 * segments per body - the planetary constants carried verbatim from
 * NAIF's PCK and the ruled ring radii from the PDS table, refused whole
 * when any file is missing or corrupt; and every released pack beside
 * it byte-identical to what it shipped.
 */
class SaturnPackTest {

    /** SAT441's split, JD 2456928.5 TDB (2014-09-28), measured in #574's pack study. */
    static final double SPLIT_ET = (2456928.5 - 2451545.0) * 86400.0;

    @Test
    void theBundledPackLoadsAndStatesItself() {
        SaturnPack pack = SaturnPack.load();
        Map<String, String> m = pack.manifest();
        assertEquals("saturn-system", m.get("pack.name"));
        assertEquals("1", m.get("pack.version"));
        assertEquals("SAT441 with DE440", m.get("ephemeris"));
        assertEquals("6,699", m.get("ephemeris.saturn.bodies"));
        assertEquals("601,602,603,604,605,606,608", m.get("ephemeris.moons.bodies"),
                "the seven ruled moons; Hyperion (607) excluded (#574, ruling 2)");
        assertEquals(LocalDate.of(1900, 1, 1), pack.saturnFirstDay());
        assertEquals(LocalDate.of(2100, 12, 31), pack.saturnLastDay());
        assertEquals(LocalDate.of(2000, 1, 1), pack.moonsFirstDay());
        assertEquals(LocalDate.of(2100, 12, 31), pack.moonsLastDay());
        assertEquals(SaturnSystemService.SATURN_FIRST_DAY, pack.saturnFirstDay());
        assertEquals(SaturnSystemService.SATURN_LAST_DAY, pack.saturnLastDay());
        assertEquals(SaturnSystemService.MOONS_FIRST_DAY, pack.moonsFirstDay());
        assertEquals(SaturnSystemService.MOONS_LAST_DAY, pack.moonsLastDay());
        assertEquals(3, pack.saturnKernel().segments().size(),
                "the barycentre's one segment and both of Saturn's");
        assertEquals(14, pack.moonsKernel().segments().size(), "both segments of each of the seven");
        assertTrue(m.get("terms").contains("nothing of IMCCE's"));
    }

    @Test
    void theConstantsAreThePckValuesTheDecisionRecordCites() {
        // docs/decisions/saturn-system.md, "The authority": Saturn's radii
        // 60268, 60268 and 54364 km and the pole RA 40.589 − 0.036 T,
        // Dec 83.537 − 0.004 T, as pck00011.tpc states them (sha256
        // 3dff7b1d…), copied verbatim by the builder.
        SaturnPack.Constants k = SaturnPack.load().constants();
        assertEquals(60268.0, k.saturnEquatorialRadiusKm());
        assertEquals(54364.0, k.saturnPolarRadiusKm());
        assertEquals(40.589, k.poleRa0Degrees());
        assertEquals(-0.036, k.poleRa1DegreesPerCentury());
        assertEquals(83.537, k.poleDec0Degrees());
        assertEquals(-0.004, k.poleDec1DegreesPerCentury());
        assertEquals(SolarSystemService.SOLAR_RADIUS_KM, k.sunRadiusKm(),
                "the Sun's radius the Sun's contract already uses");
        assertEquals(List.of(601, 602, 603, 604, 605, 606, 608), List.copyOf(k.moonRadiiKm().keySet()));
        Map<String, String> m = SaturnPack.load().manifest();
        assertEquals("60268,60268,54364", m.get("pck.BODY699_RADII"), "verbatim");
        assertEquals("40.589,-0.036,0.", m.get("pck.BODY699_POLE_RA"));
        assertEquals("83.537,-0.004,0.", m.get("pck.BODY699_POLE_DEC"));
        assertEquals("3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1",
                m.get("source.pck.sha256"));
    }

    @Test
    void theRingRadiiAreTheRuledPdsBoundaries() throws IOException {
        // #574, ruling 4: two bands, from the C Ring's inner boundary to the
        // B Ring's outer (the B Ring row's 117,570 km) and from the Cassini
        // Division's outer boundary to the A Ring's outer - read here from
        // the committed extraction of the retained page, as the builder did.
        List<String> rows = Files.readAllLines(Path.of(
                "docs/studies/saturn-system/rings/pds-rings-table.tsv"), StandardCharsets.UTF_8)
                .stream().filter(l -> !l.startsWith("#")).toList();
        SaturnPack.Constants k = SaturnPack.load().constants();
        assertEquals(boundary(rows, "C Ring", 1), k.ringInnerRadiusKm());
        assertEquals(boundary(rows, "B Ring", 2), k.ringBOuterRadiusKm());
        assertEquals(117570.0, k.ringBOuterRadiusKm(), "the B Ring row's, as ruled");
        assertEquals(boundary(rows, "Cassini Division", 2), k.ringCassiniOuterRadiusKm());
        assertEquals(boundary(rows, "A Ring", 2), k.ringOuterRadiusKm());
        assertEquals("2dea8fcda149c94324353ccfb47e3df27f5dfef00ba8d62ab738a917378fc64c",
                SaturnPack.load().manifest().get("source.rings.sha256"));
    }

    private static double boundary(List<String> rows, String feature, int column) {
        for (String row : rows) {
            String[] cells = row.split("\t", -1);
            if (cells[0].equals(feature)) {
                return Double.parseDouble(cells[column].replace(",", ""));
            }
        }
        throw new IllegalStateException(feature);
    }

    @Test
    void bothKernelsCarryBothOfSat441sSegmentsMeetingAtTheSplit() {
        SaturnPack pack = SaturnPack.load();
        List<SpkKernel.Segment> saturn = pack.saturnKernel().segments(6, 699);
        assertEquals(2, saturn.size());
        assertEquals(SPLIT_ET, saturn.get(0).endEt(), "the first ends at the split");
        assertEquals(SPLIT_ET, saturn.get(1).startEt(), "the second begins there");
        assertEquals(1, pack.saturnKernel().segments(0, 6).size(), "DE440's barycentre");
        for (int moon : new int[] {601, 602, 603, 604, 605, 606, 608}) {
            List<SpkKernel.Segment> of = pack.moonsKernel().segments(6, moon);
            assertEquals(2, of.size(), "both segments for " + moon);
            assertEquals(SPLIT_ET, of.get(0).endEt());
            assertEquals(SPLIT_ET, of.get(1).startEt());
            assertTrue(of.get(0).startEt() <= (2451544.5 - 31.0 - 2451545.0) * 86400.0,
                    "covering 2000-01-01 with the margin");
        }
        assertTrue(pack.moonsKernel().segments(6, 607).isEmpty(), "no Hyperion");
    }

    @Test
    void everyReleasedPackIsByteIdenticalBesideIt() {
        assertEquals(JovianPackTest.RELEASED_SOLAR_KERNEL_SHA256, SolarSystemPack.load().manifest()
                        .get("checksum.juranometria-de440-sun-emb-earth-moon-1900-2100.bsp"),
                "the Sun's and the Moon's kernel is the one 5.0.0 shipped");
        assertEquals(JovianPackTest.RELEASED_SOLAR_KERNEL_SHA256,
                SaturnPack.load().manifest().get("solar-system.kernel.sha256"),
                "and the Saturn pack's builder proved it before writing");
        Map<String, String> jovian = JovianPack.load().manifest();
        assertEquals("efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92",
                jovian.get("checksum.juranometria-jup365-jupiter-1900-2100.bsp"));
        assertEquals("6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6",
                jovian.get("checksum.juranometria-jup365-galilean-2000-2100.bsp"));
        Map<String, String> planets = PlanetsPack.load().manifest();
        assertEquals("8b0760b4a5fceee224a8f52c0ed81c9f250cf1f4fba57351462ec73267f85b12",
                planets.get("checksum.juranometria-de440-venus-1900-2100.bsp"));
        assertEquals("78736932fbd2cd36f360a5b5003ff107ed62322803871c06bdf029d763360a7d",
                planets.get("checksum.juranometria-de440-mercury-1900-2100.bsp"));
    }

    @Test
    void aCorruptKernelIsRefusedBeforeUse() throws IOException {
        byte[] bytes;
        try (InputStream in = SaturnPack.class.getResourceAsStream(
                SaturnPack.RESOURCE_ROOT + "juranometria-sat441-moons-2000-2100.bsp")) {
            bytes = in.readAllBytes();
        }
        bytes[bytes.length / 2] ^= 0x01;
        byte[] tampered = bytes;
        PackIntegrityException refused = assertThrows(PackIntegrityException.class,
                () -> SaturnPack.load(name -> name.endsWith("moons-2000-2100.bsp")
                        ? new ByteArrayInputStream(tampered)
                        : SaturnPack.class.getResourceAsStream(SaturnPack.RESOURCE_ROOT + name)));
        assertTrue(refused.getMessage().contains("fails its checksum"), refused.getMessage());
    }

    @Test
    void aMissingResourceAndAWrongPackAreRefused() {
        PackIntegrityException missing = assertThrows(PackIntegrityException.class,
                () -> SaturnPack.load(name -> name.equals("manifest.properties")
                        ? SaturnPack.class.getResourceAsStream(SaturnPack.RESOURCE_ROOT + name)
                        : null));
        assertTrue(missing.getMessage().contains("is missing"), missing.getMessage());
        PackIntegrityException wrong = assertThrows(PackIntegrityException.class,
                () -> SaturnPack.load(name -> JovianPack.class.getResourceAsStream(
                        JovianPack.RESOURCE_ROOT + name)));
        assertTrue(wrong.getMessage().contains("expected pack saturn-system"), wrong.getMessage());
        assertThrows(PackIntegrityException.class, () -> SaturnPack.load(name -> null));
    }
}
