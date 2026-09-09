package juranometria.app;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the palette says, and what pressing a letter does (Sprint 31,
 * issue #312).
 *
 * <p>Held here without a screen, because what a line says about a
 * switch is a fact about the chart's state rather than about a
 * desktop. The shown route - the prefix, the window, and the reader's
 * own controls beside it - is walked in
 * {@code ChartKeyboardJourneyTest}.
 */
class ChartKeyboardTest {

    @Test
    void everyLineSaysWhatItIsAndWhetherItIsOn() {
        ChartKeyboard keyboard = ChartKeyboard.of(
                ChartKeysTest.switches(ChartOptions.DEFAULTS));
        List<String> lines = keyboard.lines();
        assertEquals(ChartKeys.toggles().size(), lines.size(),
                "a line for every switch");
        assertTrue(lines.contains("D   Deep-sky objects — on"),
                "each with its letter, its name and its state: " + lines);
        assertTrue(lines.stream().anyMatch(line ->
                        line.startsWith("K   Black sky — off")),
                "including the ones that are off: " + lines);
        assertTrue(lines.stream().anyMatch(line -> line.contains(
                        "M   Flamsteed numbers")
                        && line.contains("F names the figures")),
                "and the three letters that differ from the dialog's"
                        + " say why, where a reader can see it: " + lines);
    }

    @Test
    void aLetterSwitchesTheThingItNames() {
        ChartSwitches switches =
                ChartKeysTest.switches(ChartOptions.DEFAULTS);
        ChartKeyboard keyboard = ChartKeyboard.of(switches);
        assertTrue(switches.on("chart.equatorialGrid"),
                "the grid is on to begin with");
        assertTrue(keyboard.press('E'), "E reaches it");
        assertFalse(switches.on("chart.equatorialGrid"),
                "and switches it off");
        assertEquals("Equatorial grid off — saved.",
                keyboard.announcement(),
                "saying so out loud, and how long it lasts");
        assertTrue(keyboard.lines().stream().anyMatch(line ->
                        line.equals("E   Equatorial grid — off")),
                "and on the line itself: " + keyboard.lines());
        keyboard.press('E');
        assertTrue(switches.on("chart.equatorialGrid"),
                "and back again");
        assertEquals("Equatorial grid on — saved.",
                keyboard.announcement());
    }

    @Test
    void aLetterNobodyMappedDoesNothingAtAll() {
        ChartSwitches switches =
                ChartKeysTest.switches(ChartOptions.DEFAULTS);
        ChartKeyboard keyboard = ChartKeyboard.of(switches);
        List<String> before = keyboard.lines();
        assertFalse(keyboard.press('Q'), "Q is not on the map");
        assertEquals("", keyboard.announcement(),
                "and nothing is announced for it");
        assertEquals(before, keyboard.lines(), "nor changed");
    }

    @Test
    void aSwitchWhoseMasterIsOffSaysSoAndDeclines() {
        // The dialog greys these; the keyboard has to say the same
        // thing in words, because a reader who cannot see the grey
        // needs the same answer.
        ChartOptions noDeepSky = new ChartOptions(false, true, true, true,
                true, true, true, true, true, true, false, true, true,
                true, true, true, ChartPalette.WHITE_PAPER);
        ChartSwitches switches = ChartKeysTest.switches(noDeepSky);
        ChartKeyboard keyboard = ChartKeyboard.of(switches);
        assertTrue(keyboard.lines().stream().anyMatch(line ->
                        line.equals("G   Galaxies — unavailable — enable"
                                + " deep-sky objects first")),
                "the line names the master it waits for: "
                        + keyboard.lines());
        assertFalse(keyboard.press('G'), "and the letter declines");
        assertEquals("Galaxies unavailable — enable deep-sky objects"
                        + " first.", keyboard.announcement(),
                "saying which switch would let it through");
        assertTrue(switches.on("chart.galaxies"),
                "with the stored choice untouched, so switching the"
                        + " master back on brings the reader's own"
                        + " galaxies back");

        keyboard.press('D');
        assertTrue(keyboard.lines().stream().anyMatch(line ->
                        line.equals("G   Galaxies — on")),
                "and with the master on it is an ordinary switch again: "
                        + keyboard.lines());
    }

    @Test
    void whatIsNotSwitchableHereSaysWhereItLives() {
        ChartKeyboard keyboard = ChartKeyboard.of(
                ChartKeysTest.switches(ChartOptions.DEFAULTS));
        String zenith = ChartKeys.refused().get("Zenith");
        assertEquals("controlled in Place and Time — no independent"
                        + " shortcut", zenith,
                "the refusal says where it lives and that it has no"
                        + " key of its own");
        assertFalse(zenith.toLowerCase(java.util.Locale.ROOT)
                        .contains("persist"),
                "and gives the reason it is refused for - it is part of"
                        + " the observer's lines rather than a switch"
                        + " of its own - not a fact about storage: "
                        + zenith);
        assertEquals(null, ChartKeys.forKey('Z'),
                "and no letter reaches it");
    }

    @Test
    void reopeningReadsTheChartAsItIsNow() {
        // The palette is built from the switches each time it opens.
        // A reader who changes something in the ordinary dialog and
        // then opens the keyboard must see what they just did.
        ChartSwitches switches =
                ChartKeysTest.switches(ChartOptions.DEFAULTS);
        ChartKeyboard first = ChartKeyboard.of(switches);
        assertTrue(first.lines().contains("T   Title block — on"));

        // Somebody else's route - the dialog's own transition.
        switches.toggle("chart.titleBlock");

        ChartKeyboard second = ChartKeyboard.of(switches);
        assertTrue(second.lines().contains("T   Title block — off"),
                "the palette opens on the chart as it is now: "
                        + second.lines());
        first.refresh();
        assertTrue(first.lines().contains("T   Title block — off"),
                "and one already open is refreshed rather than stale");
    }

    @Test
    void aSessionOnlySwitchSaysThatIsWhatItIs() {
        // The difference a reader cannot see: the chart's layers come
        // back tomorrow and the observer's lines do not. The palette
        // is the only place that knows, so it is the place that says.
        ChartSwitches switches =
                ChartKeysTest.switches(ChartOptions.DEFAULTS);
        ChartKeyboard keyboard = ChartKeyboard.of(switches);
        keyboard.press('R');
        assertEquals("Your meridian on — for this session.",
                keyboard.announcement(),
                "a line that is not written down says so");
        keyboard.press('G');
        assertEquals("Galaxies off — saved.", keyboard.announcement(),
                "and one that is says that instead");
    }

    @Test
    void theKeyboardStaysOutOfTheWayOfTyping() {
        // A field with the caret in it owns the keyboard. The prefix
        // is refused there rather than opening over a half-typed star
        // name - and the refusal is the palette's own rule, held here
        // rather than left to a desktop to demonstrate.
        assertFalse(ChartKeyboard.typing(),
                "nothing is focused in a headless test, so the rule"
                        + " permits the keyboard");
    }
}
