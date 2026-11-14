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

    /**
     * The transcription, held (owner's ruling of 2026-11-14, decision record
     * section 10): the pack's two tables are the ones the two independent
     * keyings of the calendar agreed on in all 307 fields. A change to either
     * table is a new import, reviewed, and changes these digests with it.
     */
    @Test
    void theTwiceKeyedValuesAreTheOnesThePackCarries() {
        assertEquals("731bdf2ad416b7b23514afa7e33adc56d911643cbbf8b6cb893305cbb7dc9756",
                PACK.manifest().get("checksum.showers.tsv"), "the working-list rows");
        assertEquals("b8bfb1079bafa0481eaa7adc666c95b395058cb9c81774299cddcd4256f52b32",
                PACK.manifest().get("checksum.radiants.tsv"), "the radiant rows");
        assertEquals("307", PACK.manifest().get("keying.fields.compared"));
        assertEquals("9", PACK.manifest().get("showers"));
        assertEquals("52", PACK.manifest().get("radiant.rows"));
    }

    /** The one ruled normalization: the comet's name is carried, the source's spelling kept. */
    @Test
    void theLeonidsParentIsTheCometsNameAndTheSourcesSpellingIsRecorded() {
        MeteorShower leonids = PACK.showers().stream()
                .filter(s -> s.code().equals("LEO")).findFirst().orElseThrow();
        assertEquals("55P/Tempel\u2013Tuttle", leonids.parentBody());
        assertEquals("55P/Temple-Tuttle", PACK.manifest().get("parent.normalized.LEO.source"));
        assertEquals(leonids.parentBody(), PACK.manifest().get("parent.normalized.LEO.shipped"));
        for (MeteorShower shower : PACK.showers()) {
            assertFalse(shower.parentBody().contains("Temple"), shower.code());
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
