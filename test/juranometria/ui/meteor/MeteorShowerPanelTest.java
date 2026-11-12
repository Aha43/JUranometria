package juranometria.ui.meteor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.context.ChartContextHolder;
import juranometria.meteor.MeteorShowers;
import juranometria.meteor.SyntheticShowers;
import juranometria.meteorchart.MeteorShowerModule;
import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The meteor-shower panel's session, words and controls (#716) over the
 * invented pack and Sun, with the production Sun and Moon for the
 * altitudes: time is a coordinate (a radiant is never worded without
 * its chart time), the panel owns no clock (R-17), selecting moves
 * nothing, and no line judges the night (R-16).
 */
class MeteorShowerPanelTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB = InterfaceText.forLanguage("nb-NO");
    private static final MeteorShowers SERVICE = SyntheticShowers.service();
    private static final SolarSystemService SOLAR = SolarSystemService.load();

    private final ChartContextHolder context = new ChartContextHolder(
            new Observer(59.91, 10.75, SyntheticShowers.at(100.0)));
    private final MeteorShowerModule module = new MeteorShowerModule(context::observer,
            () -> SERVICE, () -> true);
    private final MeteorShowerSession session = session();

    private MeteorShowerSession session() {
        MeteorShowerSession made = new MeteorShowerSession(context::observer, () -> SERVICE,
                () -> SOLAR, module);
        made.installThrough(context);
        context.onChange(change -> made.changed());
        return made;
    }

    private static <T> T onEventThread(java.util.concurrent.Callable<T> work) throws Exception {
        Object[] out = new Object[1];
        Exception[] failed = new Exception[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                out[0] = work.call();
            } catch (Exception e) {
                failed[0] = e;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T value = (T) out[0];
        return value;
    }

    @Test
    void withNothingSelectedThePanelSaysWhichShowersAreActiveAtTheChartTime() {
        List<String> lines = new MeteorShowerText(EN).unselected(session);
        assertTrue(lines.get(0).startsWith("Active at chart time 2030-04-11 00:00 UTC: "),
                lines.get(0));
        assertTrue(lines.get(1).contains("Invented Calendar 2030"), "the edition is named");
        assertTrue(session.facts().isEmpty());
    }

    @Test
    void anActiveShowersRadiantIsWordedWithTheChartTimeThatDefinesIt() {
        session.select("AAA");
        MeteorShowerSession.Facts facts = session.facts().orElseThrow();
        assertTrue(facts.active());
        List<String> lines = new MeteorShowerText(EN).lines(facts, SERVICE.pack());
        String all = String.join("\n", lines);
        int at = indexOfLineStarting(lines, "At chart time 2030-04-11 00:00 UTC, 59.91° north,"
                + " 10.75° east:");
        int radiant = indexOfLineStarting(lines, "Radiant: right ascension 40°, declination"
                + " +20° (J2000");
        assertTrue(at >= 0 && radiant == at + 1, "the chart time and place, then the radiant:\n"
                + all);
        assertTrue(all.contains("Active at chart time 2030-04-11 00:00 UTC"), all);
        assertTrue(all.contains("Nominal maximum at solar longitude 100.5°; next: 2030-04-11,"
                + " about 12 h UTC, known to an hour or two."), all);
        assertTrue(all.contains("Zenithal hourly rate (ZHR) 12: the rate for an ideal dark sky"),
                all);
        assertTrue(all.contains("Parent body, as the IAU Meteor Data Center states it:"
                + " 9P/Invented."), all);
        assertTrue(all.contains("Sun: altitude ") && all.contains("Moon: "), all);
        assertTrue(all.contains("The night around chart time, 2030-04-10 11:17 to 2030-04-11"
                + " 11:17 UTC"), all);
        assertTrue(lines.get(lines.size() - 1).startsWith("Values are nominal, from the Invented"
                + " Calendar 2030 (working list correct as of never). Source: nobody."), all);
        for (String word : List.of("good", "bad", "poor", "favourable", "unfavourable",
                "worth", "weather")) {
            assertFalse(all.toLowerCase(java.util.Locale.ROOT).contains(word),
                    "facts, never a verdict: " + word);
        }
    }

    private static int indexOfLineStarting(List<String> lines, String start) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(start)) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void anInactiveShowerStaysSelectedAndNoRadiantIsDescribed() throws Exception {
        session.select("AAA");
        onEventThread(() -> {
            context.install(context.observer().at(SyntheticShowers.at(200.0)));
            return null;
        });
        assertEquals("AAA", session.selected(), "the selection survives any chart time (R-12)");
        MeteorShowerSession.Facts facts = session.facts().orElseThrow();
        assertFalse(facts.active());
        String all = String.join("\n", new MeteorShowerText(EN).lines(facts, SERVICE.pack()));
        assertTrue(all.contains("Not active at chart time 2030-07-20 00:00 UTC"), all);
        assertTrue(all.contains("No radiant is drawn or described."), all);
        assertFalse(all.contains("Radiant:"), "no radiant outside the activity:\n" + all);
        assertFalse(all.contains("The night around"), all);
        assertTrue(module.contributedGeometry().isEmpty(), "and none on the chart");
        assertFalse(session.canCentre());
    }

    @Test
    void theMaximumIsShownNoFinerThanItIsKnown() {
        MeteorShowerText text = new MeteorShowerText(EN);
        Instant at = Instant.parse("2030-01-01T06:03:10Z");
        assertEquals("2030-01-01 06:00 UTC, known to about ten minutes",
                text.maximum(SERVICE.shower("BBB").orElseThrow(), at));
        assertEquals("2030-01-01, about 06 h UTC, known to an hour or two",
                text.maximum(SERVICE.shower("AAA").orElseThrow(), at));
        assertEquals("around 2030-01-01 UTC, known to about a day",
                text.maximum(SERVICE.shower("CCC").orElseThrow(), at));
    }

    @Test
    void theOneTimeActionInstallsThroughTheChartContextAndKeepsThePlace() throws Exception {
        session.select("AAA");
        List<Instant> installed = new ArrayList<>();
        context.onChange(change -> installed.add(change.current().instant()));
        Instant set = onEventThread(() -> session.setChartTimeToNextMaximum().orElseThrow());
        assertEquals(SyntheticShowers.at(100.5), set);
        assertEquals(List.of(set), installed, "one installation, through the one chart time");
        assertEquals(59.91, context.observer().latitudeDegrees());
        assertEquals(10.75, context.observer().eastLongitudeDegrees());
        assertEquals(set, module.radiant().orElseThrow().moment().instant(),
                "the chart's radiant is for the new chart time: no second clock");
    }

    @Test
    void selectingMovesNeitherTheChartNorTheTime() {
        List<SkyPosition> centred = new ArrayList<>();
        session.centreThrough(centred::add);
        Instant before = context.observer().instant();
        session.select("AAA");
        assertTrue(centred.isEmpty(), "selection is not navigation");
        assertEquals(before, context.observer().instant());
        session.centreOnRadiant();
        assertEquals(List.of(session.facts().orElseThrow().radiant().at()), centred,
                "navigation is explicit, and goes to the radiant for the chart time");
    }

    @Test
    void theNorwegianUsesTheRuledTerms() {
        session.select("AAA");
        String all = String.join("\n", new MeteorShowerText(NB).lines(
                session.facts().orElseThrow(), SERVICE.pack()));
        assertTrue(all.contains("solens ekliptiske lengde, sollengde"), all);
        assertTrue(all.contains("Zenital timerate (ZHR) 12"), all);
        assertTrue(all.contains("Nominelt maksimum ved sollengde 100.5°"), all);
        assertTrue(all.contains("Populasjonsindeks 2.5"), all);
        assertTrue(all.contains("Opphavslegeme"), all);
        assertTrue(all.contains("Aktiv på karttidspunktet 2030-04-11 00:00 UTC"), all);
    }

    @Test
    void bothHostsShowOneSessionAndTheControlsFollowTheChartTime() throws Exception {
        MeteorShowerControls[] hosts = onEventThread(() -> new MeteorShowerControls[] {
                new MeteorShowerControls(session, EN), new MeteorShowerControls(session, EN)});
        assertFalse(hosts[0].awake() || hosts[1].awake(),
                "built but never shown, a host has read nothing");
        assertEquals(0, (int) onEventThread(() -> hosts[0].selector.getItemCount()));
        onEventThread(() -> {
            // What first showing does; then the hosts follow the session.
            hosts[0].refresh();
            hosts[1].refresh();
            // "None", then the pack's showers in its order.
            hosts[0].selector.setSelectedIndex(1);
            return null;
        });
        assertEquals("AAA", session.selected(), "choosing in one host selects in the session");
        assertEquals(1, (int) onEventThread(() -> hosts[1].selector.getSelectedIndex()),
                "and the other host shows it");
        assertTrue(onEventThread(() -> hosts[1].facts.getText())
                .contains("Active at chart time 2030-04-11 00:00 UTC"));
        assertTrue(onEventThread(() -> hosts[1].centre.isEnabled()
                || !session.canCentre()));
        onEventThread(() -> {
            context.install(context.observer().at(SyntheticShowers.at(200.0)));
            return null;
        });
        assertTrue(onEventThread(() -> hosts[0].facts.getText())
                .contains("Not active at chart time 2030-07-20 00:00 UTC"),
                "the facts follow the chart time");
        assertTrue(onEventThread(() -> hosts[0].setMaximum.isEnabled()));
        assertFalse(onEventThread(() -> hosts[0].centre.isEnabled()));
        onEventThread(() -> {
            hosts[0].release();
            hosts[1].release();
            return null;
        });
    }
}
