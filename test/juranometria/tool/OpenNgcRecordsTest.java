package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.util.Map;

import juranometria.tool.OpenNgcRecords.DsoRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenNgcRecordsTest {

    // The real header and M31 row from OpenNGC v20260501.
    static final String HEADER =
            "Name;Type;RA;Dec;Const;MajAx;MinAx;PosAng;B-Mag;V-Mag;J-Mag;H-Mag;K-Mag;"
            + "SurfBr;Hubble;Pax;Pm-RA;Pm-Dec;RadVel;Redshift;Cstar U-Mag;Cstar B-Mag;"
            + "Cstar V-Mag;M;NGC;IC;Cstar Names;Identifiers;Common names;NED notes;"
            + "OpenNGC notes;Sources";
    static final String M31_LINE =
            "NGC0224;G;00:42:44.35;+41:16:08.6;And;177.83;69.66;35;4.29;3.44;2.09;1.28;"
            + "0.98;23.63;Sb;6.0000;;;-300;-0.001000;;;;031;;;;2MASX J00424433+4116074,"
            + "IRAS 00400+4059,MCG +07-02-016,PGC 002557,UGC 00454;Andromeda Galaxy;;;"
            + "Type:1|RA:1|Dec:1|Const:99|MajAx:3|MinAx:3|PosAng:3|B-Mag:3|V-Mag:2";

    @Test
    void parsesTheM31Row() {
        Map<String, Integer> header = OpenNgcRecords.headerIndex(HEADER);
        DsoRow row = OpenNgcRecords.fromLine(M31_LINE, header);
        assertEquals("NGC 224", row.id());
        assertEquals("G", row.type());
        assertEquals(10.684792, row.raDegrees(), 1e-5);
        assertEquals(41.269056, row.decDegrees(), 1e-5);
        assertEquals(177.83, row.majorAxisArcmin());
        assertEquals(69.66, row.minorAxisArcmin());
        assertEquals(35.0, row.positionAngleDegrees());
        assertEquals(3.44, row.vmag());
        assertTrue(row.aliases().contains("M 31"));
        assertTrue(row.aliases().contains("Andromeda Galaxy"));
    }

    @Test
    void normalizesCatalogueNames() {
        assertEquals("NGC 224", OpenNgcRecords.normalizeName("NGC0224"));
        assertEquals("IC 10", OpenNgcRecords.normalizeName("IC0010"));
        assertEquals("NGC 55 NED01", OpenNgcRecords.normalizeName("NGC0055 NED01"));
        assertEquals("Mel022", OpenNgcRecords.normalizeName("Mel022"));
    }

    @Test
    void southernDeclinationsKeepTheirSign() {
        Map<String, Integer> header = OpenNgcRecords.headerIndex(HEADER);
        String line = M31_LINE.replace("+41:16:08.6", "-05:20:10.0");
        assertEquals(-5.336111, OpenNgcRecords.fromLine(line, header).decDegrees(), 1e-5);
    }

    @Test
    void malformedInputFailsClearly() {
        Map<String, Integer> header = OpenNgcRecords.headerIndex(HEADER);
        assertThrows(IllegalArgumentException.class,
                () -> OpenNgcRecords.fromLine("NGC0001;G;bad-ra;+41:16:08.6", header));
        assertThrows(IllegalArgumentException.class,
                () -> OpenNgcRecords.headerIndex("Name;Type;RA"));
    }
}
