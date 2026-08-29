package juranometria.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSearchTest {

    static final SkyPosition M31_POSITION = new SkyPosition(10.684792, 41.269056);

    static final DeepSkyObject M31 = new DeepSkyObject("NGC 224",
            List.of("M 31", "Andromeda Galaxy"), DsoType.GALAXY,
            M31_POSITION, 177.83, 69.66, 35.0, 3.44, 1);
    static final DeepSkyObject NGC_225_LIKE = new DeepSkyObject("NGC 206",
            List.of(), DsoType.GALAXY,
            new SkyPosition(10.1, 40.7), 4.0, 2.0, 10.0, 12.0, 2);
    static final Star MIRACH = new Star("TYC 2286-1329-1",
            new SkyPosition(17.433012, 35.620558), 2.07);

    static final LocalSearch SEARCH =
            new LocalSearch(List.of(MIRACH), List.of(M31, NGC_225_LIKE));

    @Test
    void everySpellingOfM31ResolvesToTheSameObject() {
        for (String query : new String[] {
                "M31", "m 31", "Messier 31", "MESSIER31",
                "NGC 224", "ngc224", "Andromeda Galaxy", "andromeda   galaxy"}) {
            List<SearchResult> results = SEARCH.search(query);
            assertEquals("NGC 224", results.get(0).identity(),
                    "query '" + query + "' must resolve to M31 first");
            assertEquals("M 31", results.get(0).label());
            assertEquals(SearchResult.Kind.DEEP_SKY_OBJECT, results.get(0).kind());
        }
    }

    @Test
    void aliasesOfTheSameObjectDeduplicateToOneResult() {
        // "an" prefixes nothing but partially matches both "andromedagalaxy"
        // and nothing else; "m" alone prefix-matches M 31 keys once.
        List<SearchResult> results = SEARCH.search("NGC 2");
        long m31Count = results.stream()
                .filter(result -> result.identity().equals("NGC 224")).count();
        assertEquals(1, m31Count, "one result per object, however many aliases match");
    }

    @Test
    void aTycIdentifierResolvesToItsStar() {
        List<SearchResult> results = SEARCH.search("tyc 2286-1329-1");
        assertEquals("TYC 2286-1329-1", results.get(0).identity());
        assertEquals(SearchResult.Kind.STAR, results.get(0).kind());
        assertEquals(MIRACH.position(), results.get(0).position());
    }

    @Test
    void exactMatchesRankBeforePrefixMatches() {
        // "NGC 2" is a prefix of both; "NGC 206" exactly matches only itself.
        List<SearchResult> exact = SEARCH.search("NGC 206");
        assertEquals("NGC 206", exact.get(0).identity());

        List<SearchResult> prefix = SEARCH.search("NGC 2");
        assertEquals(2, prefix.size());
        assertEquals("NGC 224", prefix.get(0).identity(),
                "the prefix bucket orders by label: 'M 31' sorts before 'NGC 206'");
        assertEquals("NGC 206", prefix.get(1).identity());
    }

    @Test
    void decimalAndSexagesimalCoordinatesResolveEquivalently() {
        List<SearchResult> decimal = SEARCH.search("10.684708 41.268750");
        List<SearchResult> sexagesimal = SEARCH.search("0:42:44.33 +41:16:07.5");

        assertEquals(SearchResult.Kind.COORDINATES, decimal.get(0).kind());
        assertEquals(10.684708, decimal.get(0).position().raDegrees(), 1e-6);
        assertEquals(decimal.get(0).position().raDegrees(),
                sexagesimal.get(0).position().raDegrees(), 1e-4);
        assertEquals(decimal.get(0).position().decDegrees(),
                sexagesimal.get(0).position().decDegrees(), 1e-4);
        assertEquals("0h 42.7m, +41° 16′", decimal.get(0).label());

        List<SearchResult> comma = SEARCH.search("10.684708, 41.268750");
        assertEquals(decimal.get(0).position(), comma.get(0).position());
    }

    @Test
    void unhelpfulInputReturnsEmptyNotExceptions() {
        assertEquals(List.of(), SEARCH.search(null));
        assertEquals(List.of(), SEARCH.search("   "));
        assertEquals(List.of(), SEARCH.search("Betelgeuse"));
        assertEquals(List.of(), SEARCH.search("400.0 41.0"), "RA out of range");
        assertEquals(List.of(), SEARCH.search("10.0 95.0"), "Dec out of range");
        assertEquals(List.of(), SEARCH.search("10:0 41:0"), "malformed sexagesimal");
    }

    @Test
    void resultsAreImmutableAndBounded() {
        List<SearchResult> results = SEARCH.search("m");
        assertTrue(results.size() <= LocalSearch.MAX_RESULTS);
        assertThrows(UnsupportedOperationException.class,
                () -> results.add(results.get(0)));
    }
}
