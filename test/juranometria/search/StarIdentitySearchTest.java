package juranometria.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.app.Atlas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reviewed identity search grammar (docs/decisions/
 * star-identity.md) over the REAL bundled pack and constellation
 * geography: names by prefix, Bayer as Greek or spelled letter plus
 * constellation (abbreviation and genitive), Flamsteed as number
 * plus constellation, components with and without their digit - and
 * the rule that a bare letter or number never silently resolves.
 */
class StarIdentitySearchTest {

    @Test
    void theNamedStarsResolveToTheirExactCatalogueStars() {
        // The issue's named checks plus the southern sky, each through
        // representative accepted spellings.
        assertResolves("TYC 129-1873-1", "betel", "BETELGEUSE", "α ori",
                "alpha ori", "Alpha Orionis", "58 ori", "58 Orionis");
        assertResolves("TYC 5331-1752-1", "rigel", "beta orionis", "19 ori");
        assertResolves("TYC 4628-237-1", "polaris", "alpha umi",
                "α Ursae Minoris", "1 umi");
        assertResolves("TYC 5949-2777-1", "sirius", "alpha cma",
                "9 canis majoris");
        assertResolves("TYC 3105-2070-1", "vega", "alpha lyrae", "3 lyr");
        // Canonical Crux forms work because the constellation pack
        // records the source's wrong genitive ("Crux") as an erratum
        // and carries the correct "Crucis" (Codex review, PR #119).
        assertResolves("TYC 8979-3464-1", "acrux", "alpha1 cru",
                "α1 Crucis", "α¹ Crucis", "Alpha Crucis", "alpha1 crucis");
    }

    @Test
    void theDisplayLineCarriesTheFullIdentitySoTheReaderPicksKnowingly() {
        SearchResult betelgeuse = Atlas.search().search("betelgeuse").get(0);
        assertEquals("Betelgeuse · α Ori · V 0.6", betelgeuse.label(),
                "the decision's display line, verbatim");
        assertEquals("Betelgeuse · α Ori region", betelgeuse.regionTitle(),
                "the chart title is the star's honest identity");

        SearchResult flamsteedOnly = Atlas.search().search("35 crucis").get(0);
        assertEquals("TYC 8658-751-1", flamsteedOnly.identity());
        assertTrue(flamsteedOnly.label().startsWith("35 Cru · V "),
                "a Flamsteed-only star is its designation: "
                        + flamsteedOnly.label());
        assertEquals("35 Cru region", flamsteedOnly.regionTitle());

        SearchResult anonymous = Atlas.search().search("TYC 8658-1765-1").get(0);
        assertEquals("TYC 8658-1765-1", anonymous.label(),
                "a star with no designation stays its identifier");
        assertEquals("TYC 8658-1765-1 region", anonymous.regionTitle());
    }

    @Test
    void splitComponentsListTogetherRatherThanHidingBehindOne() {
        // Crux carries μ1 and μ2 as separate packed stars; the
        // componentless query lists both, never silently picking one.
        List<SearchResult> mu = Atlas.search().search("mu crucis");
        assertTrue(mu.stream().anyMatch(result ->
                        result.identity().equals("TYC 8656-3488-1")),
                "μ1 Cru is listed");
        assertTrue(mu.stream().anyMatch(result ->
                        result.identity().equals("TYC 8656-3487-1")),
                "μ2 Cru is listed");
        assertResolves("TYC 8656-3488-1", "mu1 cru", "μ1 crucis");
    }

    @Test
    void bareLettersAndNumbersNeverSilentlyResolve() {
        // Every Bayer and Flamsteed key carries a constellation form,
        // so a bare letter or number can only prefix-list its
        // candidates - ambiguous by design, resolved by the reader.
        List<SearchResult> alpha = Atlas.search().search("alpha");
        assertTrue(alpha.size() > 1, "a bare letter lists candidates");
        List<SearchResult> fiftyEight = Atlas.search().search("58");
        assertTrue(fiftyEight.size() > 1, "a bare number lists candidates");
    }

    @Test
    void unknownIdentityQueriesReturnEmptyNotArbitraryStars() {
        assertEquals(List.of(), Atlas.search().search("alpha nowhere"));
        assertEquals(List.of(), Atlas.search().search("9999 orionis"));
        assertEquals(List.of(), Atlas.search().search("betelgeusex"));
    }

    @Test
    void existingObjectSearchesKeepTheirSemanticsBesideTheNewGrammar() {
        // The identity keys must not shadow the established searches.
        assertEquals("NGC 1976", Atlas.search().search("M 42").get(0).identity());
        assertEquals("M 42", Atlas.search().search("M 42").get(0).label());
        assertEquals("NGC 224",
                Atlas.search().search("andromeda galaxy").get(0).identity());
    }

    private static void assertResolves(String tyc, String... queries) {
        for (String query : queries) {
            List<SearchResult> results = Atlas.search().search(query);
            assertTrue(!results.isEmpty(), "'" + query + "' must resolve");
            assertEquals(tyc, results.get(0).identity(),
                    "'" + query + "' must resolve to " + tyc + " first");
            assertEquals(SearchResult.Kind.STAR, results.get(0).kind());
        }
    }
}
