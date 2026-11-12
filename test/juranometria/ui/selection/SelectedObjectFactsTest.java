package juranometria.ui.selection;

import java.time.Instant;
import java.util.List;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.module.Furniture;
import juranometria.render.SymbolFamily;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bounded, typed facts of the selected-object box (#692, ruled C-9 and
 * C-14 on #690), and the switch's own store (C-7).
 */
class SelectedObjectFactsTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    private static final SkyPosition PLACE = new SkyPosition(83.8187, -5.3897);

    @Test
    void aStarKnownOnlyByItsCatalogueIdIsHeadedByIt() {
        Furniture box = SelectedObjectFacts.box(new SelectedObjectFacts.StarFacts(
                "TYC 128-1551-2", null, null, null, null, 6.02, PLACE), EN);
        assertEquals("TYC 128-1551-2", box.heading());
        assertEquals(List.of("V 6.02", "5h 35.3m   −5° 23′"), box.lines());
    }

    @Test
    void aLetteredStarIsHeadedByItsLetterWithItsConstellation() {
        Furniture box = SelectedObjectFacts.box(new SelectedObjectFacts.StarFacts(
                "TYC 1-2-3", null, "δ", null, "Ori", 2.25, PLACE), EN);
        assertEquals("δ Ori", box.heading());
        assertEquals("TYC 1-2-3", box.lines().get(0));
    }

    @Test
    void aDeepSkyObjectSaysItsFamilyInTheReadersLanguage() {
        var facts = new SelectedObjectFacts.DeepSkyFacts("NGC 1976", "M 42", SymbolFamily.NEBULAE,
                DeepSkyObject.Recorded.Band.VISUAL, 4.0, PLACE);
        assertEquals(List.of("NGC 1976 · nebula", "V 4.00", "5h 35.3m   −5° 23′"),
                SelectedObjectFacts.box(facts, EN).lines());
        assertEquals("NGC 1976 · tåke", SelectedObjectFacts.box(facts, NB).lines().get(0));
        var silent = new SelectedObjectFacts.DeepSkyFacts("NGC 1", null, SymbolFamily.GALAXIES,
                DeepSkyObject.Recorded.Band.NONE, 0.0, PLACE);
        assertEquals("NGC 1", SelectedObjectFacts.box(silent, EN).heading());
        assertEquals("magnitude not recorded", SelectedObjectFacts.box(silent, EN).lines().get(1));
    }

    @Test
    void aBodySaysItsChartTimeAndADimmedOneSaysWhy() {
        Instant at = Instant.parse("2026-08-15T06:00:00Z");
        Furniture up = SelectedObjectFacts.box(new SelectedObjectFacts.BodyFacts(
                "moon", "Moon", at, PLACE, false), EN);
        assertEquals(List.of("for 2026-08-15 06:00 UTC", "5h 35.3m   −5° 23′"), up.lines());
        Furniture down = SelectedObjectFacts.box(new SelectedObjectFacts.BodyFacts(
                "moon", "Moon", at, PLACE, true), EN);
        assertEquals("(below the horizon)", down.lines().get(2), "the fourth line, a body's only");
        assertEquals("Selected: Moon, for 2026-08-15 06:00 UTC, 5h 35.3m   −5° 23′,"
                + " (below the horizon)", down.spoken());
    }

    @Test
    void aRadiantSaysWhatItIsItsChartTimeAndItsPlaceToAWholeDegree() {
        // An invented radiant: the box is the same whatever the shower (#716, C-5).
        Instant at = Instant.parse("2030-08-12T22:00:00Z");
        Furniture up = SelectedObjectFacts.box(new SelectedObjectFacts.RadiantFacts(
                "meteor-shower.PER", "Perseids", at, new SkyPosition(47.6, 57.4), false), EN);
        assertEquals("Perseids", up.heading());
        assertEquals(List.of("meteor shower radiant", "for 2030-08-12 22:00 UTC",
                "RA 48°   Dec +57° (J2000)"), up.lines(),
                "never without the chart time, and no finer than the source prints");
        Furniture down = SelectedObjectFacts.box(new SelectedObjectFacts.RadiantFacts(
                "meteor-shower.PER", "Perseidene", at, new SkyPosition(359.7, -0.6), true), NB);
        assertEquals(List.of("radiant for meteorsverm", "for 2030-08-12 22:00 UTC",
                "RA 0°   Dec −1° (J2000)", "(under horisonten)"), down.lines());
        assertTrue(down.spoken().startsWith("Valgt: Perseidene, radiant for meteorsverm,"));
    }

    @Test
    void aSelectedRadiantOnThePageGetsTheRadiantBoxNotABodys() {
        var moment = new juranometria.module.OverlayContribution.Moment(
                Instant.parse("2030-08-12T22:00:00Z"), 60.0, 10.0);
        var entry = new juranometria.page.PageEntry.Body("meteor-shower.PER", "Perseids",
                PLACE, moment, true, 0.0);
        assertTrue(SelectedObjectFacts.of(entry, false).orElseThrow()
                instanceof SelectedObjectFacts.RadiantFacts);
        var moon = new juranometria.page.PageEntry.Body("moon", "Moon", PLACE, moment, true, 0.0);
        assertTrue(SelectedObjectFacts.of(moon, false).orElseThrow()
                instanceof SelectedObjectFacts.BodyFacts);
    }

    @Test
    void theBoxIsBoundedAndAsksForTheUpperLeft() {
        Furniture box = SelectedObjectFacts.box(new SelectedObjectFacts.StarFacts(
                "TYC 129-1873-1", "Betelgeuse", "α", "58", "Ori", 0.58, PLACE), EN);
        assertTrue(box.lines().size() <= 3, "a heading and three lines for a star");
        assertTrue(box.upperLeftFirst());
        assertEquals(SelectedObjectFacts.IDENTITY, box.identity());
    }

    @Test
    void theStoreIsOnByDefaultAndRemembersAChoice() throws Exception {
        Preferences node = Preferences.userRoot().node("juranometria-test-selected-"
                + System.nanoTime());
        try {
            SelectedObjectStore store = SelectedObjectStore.forNode(node);
            assertTrue(store.shownOrDefault(), "on by default (C-7)");
            node.put("selectedObjectBox", "maybe");
            assertTrue(store.shown().isEmpty(), "an unreadable value is never a choice");
            SelectedObjectSwitch shown = new SelectedObjectSwitch(store);
            shown.toggle();
            assertFalse(SelectedObjectStore.forNode(node).shownOrDefault(), "remembered");
        } finally {
            node.removeNode();
        }
    }
}
