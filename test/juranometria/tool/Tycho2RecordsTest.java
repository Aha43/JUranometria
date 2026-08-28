package juranometria.tool;

import org.junit.jupiter.api.Test;

import juranometria.tool.Tycho2Records.StarRow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Tycho2RecordsTest {

    // Real records from the pinned I/259 distribution.
    static final String MAIN_EQUAL_MAGS =
            "0001 00008 1| |  2.31750494|  2.23184345|  -16.3|   -9.0| 68| 73| 1.7| 1.8"
            + "|1958.89|1951.94| 4|1.0|1.0|0.9|1.0|12.146|0.158|12.146|0.223|999| |"
            + "         |  2.31754222|  2.23186444|1.67|1.54| 88.0|100.8| |-0.2";
    static final String MAIN_TYCHO1 =
            "0001 00013 1| |  1.12558209|  2.26739400|   27.7|   -0.5|  9| 12| 1.2| 1.2"
            + "|1990.76|1989.25| 8|1.0|0.8|1.0|0.7|10.488|0.038| 8.670|0.015|999|T|"
            + "         |  1.12551889|  2.26739556|1.81|1.52|  9.3| 12.7| |-0.2";
    static final String SUPPLEMENT =
            "0002 00580 1|T|003.03708871|+01.57351477|       |       | 42.9| 39.5|     "
            + "|     | |11.688|0.122|11.171|0.121|999|T|       ";

    @Test
    void parsesARepresentativeMainRecord() {
        StarRow row = Tycho2Records.fromMainLine(MAIN_TYCHO1).orElseThrow();
        assertEquals("TYC 1-13-1", row.id());
        assertEquals(1.12558209, row.raDegrees(), 1e-9);
        assertEquals(2.26739400, row.decDegrees(), 1e-9);
        // V = VT - 0.090 * (BT - VT) = 8.670 - 0.090 * (10.488 - 8.670)
        assertEquals(8.506, row.vmag(), 1e-3);
        assertFalse(row.usedFallbackPosition());
        assertFalse(row.usedVtWithoutBt());
    }

    @Test
    void equalMagnitudesLeaveVAtVt() {
        StarRow row = Tycho2Records.fromMainLine(MAIN_EQUAL_MAGS).orElseThrow();
        assertEquals("TYC 1-8-1", row.id());
        assertEquals(12.146, row.vmag(), 1e-9);
    }

    @Test
    void fallsBackToTheObservedPositionWhenTheMeanIsAbsent() {
        String[] fields = MAIN_EQUAL_MAGS.split("\\|", -1);
        fields[1] = "X";
        fields[2] = "            ";
        fields[3] = "            ";
        StarRow row = Tycho2Records.fromMainLine(String.join("|", fields)).orElseThrow();
        assertTrue(row.usedFallbackPosition());
        assertEquals(2.31754222, row.raDegrees(), 1e-9);
        assertEquals(2.23186444, row.decDegrees(), 1e-9);
    }

    @Test
    void aRecordWithoutVtIsTheDocumentedDrop() {
        String[] fields = MAIN_EQUAL_MAGS.split("\\|", -1);
        fields[19] = "      ";
        assertTrue(Tycho2Records.fromMainLine(String.join("|", fields)).isEmpty());
    }

    @Test
    void aMissingBtUsesVtUnchangedAndSaysSo() {
        String[] fields = MAIN_EQUAL_MAGS.split("\\|", -1);
        fields[17] = "      ";
        StarRow row = Tycho2Records.fromMainLine(String.join("|", fields)).orElseThrow();
        assertEquals(12.146, row.vmag(), 1e-9);
        assertTrue(row.usedVtWithoutBt());
    }

    @Test
    void parsesARepresentativeSupplementRecord() {
        StarRow row = Tycho2Records.fromSupplementLine(SUPPLEMENT).orElseThrow();
        assertEquals("TYC 2-580-1", row.id());
        assertEquals(3.03708871, row.raDegrees(), 1e-9);
        assertEquals(1.57351477, row.decDegrees(), 1e-9);
        assertEquals(11.124, row.vmag(), 1e-3);
        assertFalse(row.usedHpMagnitude());
    }

    @Test
    void anHpFlaggedSupplementMagnitudeIsUsedUnchanged() {
        String[] fields = SUPPLEMENT.split("\\|", -1);
        fields[10] = "H";
        StarRow row = Tycho2Records.fromSupplementLine(String.join("|", fields)).orElseThrow();
        assertEquals(11.171, row.vmag(), 1e-9);
        assertTrue(row.usedHpMagnitude());
    }

    @Test
    void malformedLinesFailClearly() {
        assertThrows(IllegalArgumentException.class,
                () -> Tycho2Records.fromMainLine("0001 00008 1| "));
        assertThrows(IllegalArgumentException.class,
                () -> Tycho2Records.fromSupplementLine("garbage"));
    }
}
