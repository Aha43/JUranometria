package juranometria.meteor;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled meteor-shower pack against the rulings (#716): the nine
 * showers and no others (C-3), in the order of the year, each with its
 * page words in both languages, an edition that is named (R-4), and the
 * source's credit line. Codes and structure only: no listed value is
 * repeated here.
 */
class MeteorShowerPackContractTest {

    private static final MeteorShowerPack PACK = MeteorShowerPack.load();

    @Test
    void thePackCarriesTheNineRuledShowersInTheOrderOfTheYear() {
        assertEquals(List.of("QUA", "LYR", "ETA", "SDA", "PER", "ORI", "LEO", "GEM", "URS"),
                PACK.showers().stream().map(MeteorShower::code).toList());
    }

    @Test
    void everyShowerHasItsWordsInBothLanguages() {
        InterfaceText english = InterfaceText.forLanguage("en");
        InterfaceText norwegian = InterfaceText.forLanguage("nb-NO");
        for (MeteorShower shower : PACK.showers()) {
            for (String key : List.of("page.body." + shower.identity(),
                    "page.body." + shower.identity() + ".symbol")) {
                assertTrue(english.keys().contains(key), key);
                assertTrue(norwegian.keys().contains(key), key + " in Norwegian");
            }
        }
    }

    @Test
    void theEditionIsNamedAndTheSourceIsCredited() {
        assertFalse(PACK.edition().isBlank());
        assertFalse(PACK.correctAsOf().isBlank());
        assertEquals("Source: International Meteor Organization (IMO) – www.imo.net",
                PACK.credit(), "the line the source asks for, exactly");
        assertEquals(PACK.manifest().get("edition.year"),
                PACK.edition().replaceAll("\\D", ""), "the edition names its year");
    }

    @Test
    void everyShowerIsActiveAtItsOwnMaximumAndItsRowsAreInTheOrderTheEarthMeetsThem() {
        for (MeteorShower shower : PACK.showers()) {
            assertTrue(MeteorShowers.active(shower, shower.maximumLongitude()), shower.code());
            assertTrue(shower.rows().size() >= 2, shower.code() + " has a drifting radiant");
            double before = Double.NEGATIVE_INFINITY;
            for (MeteorShower.Row row : shower.rows()) {
                double from = MeteorShowers.signed(row.solarLongitude()
                        - shower.maximumLongitude());
                assertTrue(from > before, shower.code() + " rows in order");
                before = from;
            }
            assertTrue(shower.radiantAtMaximum().separationDegrees(MeteorShowers.radiantAt(
                    shower, shower.maximumLongitude()).at()) <= 2.0,
                    shower.code() + ": the radiant table read at the maximum agrees with the"
                            + " working list's radiant");
        }
    }
}
