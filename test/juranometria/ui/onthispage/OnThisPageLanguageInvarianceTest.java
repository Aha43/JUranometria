package juranometria.ui.onthispage;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarIdentity;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageInventory;
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * On This Page does not change language (Sprint 33, issue #349).
 *
 * <p>The sidebar is an object inventory, and its designations carry
 * canonical IAU abbreviations: {@code 44 Psc} is a Flamsteed number
 * bound to a constellation, not a sentence about Pisces. The gate
 * settled what happens to those:
 *
 * <blockquote>The IAU abbreviation and the canonical identity never
 * fall back, because they are canonical data and not translations.
 * There is nothing for them to fall back <em>to</em>.</blockquote>
 *
 * <p>So there is no translatable constellation-name surface here at
 * all, and the one constellation reference there is must stay put.
 * Translating {@code 44 Psc} would corrupt a designation rather than
 * localise it - a reader could no longer look it up anywhere.
 *
 * <p>That was true before this test and true by accident: nothing had
 * ever wired a chart language into the sidebar, so nothing changed it.
 * Held on purpose from here, because "it happens not to" and "it must
 * not" look identical until somebody tries.
 *
 * <p>Whether a reader should be able to see a constellation's
 * Norwegian name in the sidebar is a different question - an
 * information-design capability with width and hierarchy
 * consequences - and belongs with #356, not here.
 */
class OnThisPageLanguageInvarianceTest {

    private static final String NORWEGIAN = "nb-NO";

    /**
     * A page dense with designations.
     *
     * <p>Orion at 42 degrees, chosen because the test is about what
     * happens to abbreviations: a page whose objects all carried
     * proper names would pass this without ever exercising one.
     */
    private static final ChartViewState ORION = new ChartViewState(
            new SkyPosition(83.8, 0.0), 42.0,
            ChartViewState.defaultMagnitudeFor(42.0));

    @Test
    void theSidebarIsIdenticalInBothChartLanguages() {
        PageContents latin = inventory(juranometria.geo.SkyNames.LATIN);
        PageContents norsk = inventory(NORWEGIAN);

        // The premise, before anything is concluded from equality:
        // this page really does carry both kinds of designation, so
        // the comparison exercises an abbreviation rather than
        // passing over objects that never had one.
        List<String> bayer = new ArrayList<>();
        List<String> flamsteed = new ArrayList<>();
        for (PageEntry.StarEntry star : latin.namedStars()) {
            StarIdentity identity = star.star().identity();
            if (identity.name() != null) {
                continue;
            }
            if (identity.bayer() != null) {
                bayer.add(identity.bayer() + " " + identity.constellation());
            } else if (identity.flamsteed() != null) {
                flamsteed.add(identity.flamsteed() + " "
                        + identity.constellation());
            }
        }
        assertTrue(!bayer.isEmpty(),
                "the premise: this page carries Bayer designations,"
                        + " which is what binds a letter to an"
                        + " abbreviation");
        assertTrue(!flamsteed.isEmpty(),
                "and Flamsteed numbers, which is the 44 Psc shape the"
                        + " gate's ruling is about: " + flamsteed.size()
                        + " of them");

        assertEquals(latin.entries(), norsk.entries(),
                "every entry, in order, with its identity, its"
                        + " designation and its magnitude - a sidebar"
                        + " is a list of objects, and an object does"
                        + " not become a different object because the"
                        + " sky around it was relabelled");
    }

    /**
     * The abbreviations themselves, named rather than implied.
     *
     * <p>The equality above would also hold if both languages were
     * equally wrong. This says what the values actually are, so a
     * change that localised both sides identically still fails.
     */
    @Test
    void designationsCarryTheCanonicalAbbreviation() {
        for (String language : new String[] {
                juranometria.geo.SkyNames.LATIN, NORWEGIAN}) {
            List<String> localised = new ArrayList<>();
            for (PageEntry.StarEntry star : inventory(language).namedStars()) {
                StarIdentity identity = star.star().identity();
                String constellation = identity.constellation();
                if (constellation == null) {
                    continue;
                }
                // The Norwegian for Orion is "Orion"; for Taurus it is
                // "Tyren", for Gemini "Tvillingene". A designation
                // carrying one of those has been translated, and a
                // reader can no longer look it up.
                if (constellation.equals("Tyren")
                        || constellation.equals("Tvillingene")
                        || constellation.length() != 3) {
                    localised.add(identity.bayer() != null
                            ? identity.bayer() + " " + constellation
                            : identity.flamsteed() + " " + constellation);
                }
            }
            assertEquals(List.of(), localised,
                    "every designation on the " + language + " page"
                            + " ends in a three-letter IAU"
                            + " abbreviation, because that is what a"
                            + " designation is made of");
        }
    }

    private static PageContents inventory(String chartLanguage) {
        ChartScene scene = Atlas.assemblerNamedIn(chartLanguage)
                .assemble(ORION, 900, 700);
        return PageInventory.of(scene, ChartOptions.DEFAULTS);
    }
}
