package juranometria.geo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The atlas's constellations are the IAU's (Sprint 33, issue #347).
 *
 * <p>The gate names the IAU's official list as the source of the
 * canonical identity layer, and a source that is named but never
 * compared against is not a source. This compares every one of the
 * 88 rows the atlas carries - abbreviation, Latin nominative and
 * genitive - against the official table committed beside it.
 *
 * <p>Two rows are known to disagree, and they are listed here by
 * name rather than tolerated by a count. Listing them makes the test
 * do two jobs at once: it holds the other 86 exactly, and it records
 * the authoritative value each defect should be corrected <em>to</em>,
 * so #348 has the expected answer written down rather than having to
 * re-derive it. When #348 corrects them, this list shrinks to empty
 * and the test tightens by itself.
 *
 * <p>The identity layer is deliberately separate from the Norwegian
 * one. A Norwegian source does not establish which constellations
 * exist or what they are called in Latin, and the IAU does not
 * establish what they are called in Norwegian.
 */
class IauIdentityTest {

    private static final Path OFFICIAL =
            Path.of("docs/studies/sky-language/iau-constellations.tsv");

    /**
     * The rows the atlas is known to get wrong, and their correct
     * values. Each belongs to #348, which owns the Latin/IAU
     * foundation; #347 only establishes what they should be.
     */
    private static final Map<String, String[]> KNOWN_DEFECTS =
            new LinkedHashMap<>();

    static {
        // The atlas's own record disagrees with itself: the genitive
        // Serpentis belongs to "Serpens", not to "Serpens Caput".
        // Caput and Cauda are two regions of one constellation, and
        // the atlas already gives both to this single identity.
        KNOWN_DEFECTS.put("Ser",
                new String[] {"Serpens Caput", "Serpens"});
        // Wrong twice: the name, and a genitive that is malformed
        // Latin either way - Austrini is masculine against the
        // feminine corona. A reader searching the standard
        // "alpha Coronae Australis" finds nothing today.
        KNOWN_DEFECTS.put("CrA",
                new String[] {"Corona Austrina", "Corona Australis"});
    }

    @Test
    void everyConstellationMatchesTheOfficialListExceptTheKnownTwo() throws Exception {
        Map<String, String[]> official = official();
        Map<String, Constellation> atlas = new TreeMap<>();
        for (Constellation each
                : ConstellationGeography.load().constellations()) {
            atlas.put(each.id(), each);
        }

        assertEquals(88, official.size(),
                "the official table carries all 88 constellations");
        assertEquals(official.keySet(), atlas.keySet(),
                "and the atlas carries exactly the same identities,"
                        + " by IAU abbreviation");

        List<String> unexpected = new ArrayList<>();
        int agreed = 0;
        for (Map.Entry<String, String[]> row : official.entrySet()) {
            String id = row.getKey();
            Constellation held = atlas.get(id);
            String name = row.getValue()[0];
            String genitive = row.getValue()[1];
            boolean matches = held.latinName().equals(name)
                    && held.genitive().equals(genitive);
            if (matches) {
                agreed++;
            } else if (!KNOWN_DEFECTS.containsKey(id)) {
                unexpected.add(id + ": atlas has \"" + held.latinName()
                        + "\" / \"" + held.genitive() + "\", official is \""
                        + name + "\" / \"" + genitive + "\"");
            }
        }

        assertEquals(List.of(), unexpected,
                "no constellation disagrees with the IAU beyond the"
                        + " two already recorded");
        assertEquals(88 - KNOWN_DEFECTS.size(), agreed,
                "and the other " + (88 - KNOWN_DEFECTS.size())
                        + " agree exactly, so this is a comparison"
                        + " rather than a formality");
    }

    /**
     * The known defects are still defects.
     *
     * <p>The other half of the contract. Without it, #348 could
     * correct the data and leave this list behind, and a stale
     * exception would silently excuse a row that had become correct -
     * which is how an allowance outlives the problem it was made for.
     */
    @Test
    void theKnownDefectsAreStillPresentAndStillWrong() throws Exception {
        Map<String, String[]> official = official();
        Map<String, Constellation> atlas = new TreeMap<>();
        for (Constellation each
                : ConstellationGeography.load().constellations()) {
            atlas.put(each.id(), each);
        }

        for (Map.Entry<String, String[]> defect : KNOWN_DEFECTS.entrySet()) {
            String id = defect.getKey();
            assertEquals(defect.getValue()[0], atlas.get(id).latinName(),
                    id + " still holds the name this exception was"
                            + " written for; if it has been corrected,"
                            + " remove the exception rather than"
                            + " leaving it to excuse nothing");
            assertEquals(defect.getValue()[1], official.get(id)[0],
                    id + " should be corrected to the official name,"
                            + " which is what #348 will write");
        }
        assertTrue(KNOWN_DEFECTS.size() == 2,
                "two known defects, both assigned to #348: "
                        + KNOWN_DEFECTS.keySet());
    }

    /** The official table: abbreviation to name and genitive. */
    private static Map<String, String[]> official() throws Exception {
        Map<String, String[]> rows = new TreeMap<>();
        for (String line : Files.readAllLines(OFFICIAL)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t");
            if (columns.length != 3 || columns[1].equals("IAU")) {
                continue;
            }
            rows.put(columns[1], new String[] {columns[0], columns[2]});
        }
        return rows;
    }
}
