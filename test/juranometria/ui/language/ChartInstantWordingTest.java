package juranometria.ui.language;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The words #259 changed, in both packs (the rulings on #556): the one
 * action that reads the clock has one name everywhere - the Place and
 * Time button, the chart's box, the chart keyboard and the Solar System
 * tab - and the two stale wordings the study found are gone.
 */
class ChartInstantWordingTest {

    @Test
    void updateToNowHasOneNameEverywhere() {
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            String name = said.say("placeandtime.now.label");
            assertEquals(name, said.say("placeandtime.now.a11y"), language);
            assertEquals(name, said.say("furniture.placeandtime.action"), language);
            assertEquals(name, said.say("chartkeyboard.placeandtime.updateToNow.label"),
                    language);
        }
        assertEquals("Update to now",
                InterfaceText.forLanguage("en").say("placeandtime.now.label"),
                "Now is renamed (ruling 4)");
    }

    @Test
    void theSolarSystemMenuNamesEveryBodyItHolds() {
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            String explained = said.say("menu.solarsystem.explain");
            for (String body : List.of("sun", "mercury", "venus", "moon", "jupiter")) {
                String named = said.say("solarsystem." + body);
                assertTrue(explained.contains(named), language + ": the menu's explanation"
                        + " names " + named + ": " + explained);
            }
        }
    }

    @Test
    void noWordOfTheBoxOrTheTablesNotesNamesAZoneOrAnOffset() {
        for (String language : List.of("en", "nb-NO")) {
            InterfaceText said = InterfaceText.forLanguage(language);
            for (String key : List.of("furniture.placeandtime.heading",
                    "furniture.placeandtime.instant", "furniture.placeandtime.lst",
                    "furniture.placeandtime.explain", "furniture.placeandtime.spoken",
                    "furniture.placeandtime.sheet", "furniture.placeandtime.paper",
                    "solartable.stale.message", "solartable.observer.note",
                    "solartable.observer.computed",
                    "solarsystem.chartinstant")) {
                String text = said.say(key).toLowerCase(java.util.Locale.ROOT);
                for (String civil : List.of("zone", "sone", "cet", "gmt", "offset",
                        "summer", "sommer", "civil", "local time", "lokal tid")) {
                    assertFalse(text.contains(civil), language + " " + key + ": " + text);
                }
            }
        }
    }
}
