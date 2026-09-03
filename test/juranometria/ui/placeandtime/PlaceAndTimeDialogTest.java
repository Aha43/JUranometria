package juranometria.ui.placeandtime;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import juranometria.meridian.MeridianModule;
import juranometria.module.TestChartServices;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The place-and-time controls (Sprint 25, issue #228).
 *
 * <p>Held to the gate's decisions through the real controls: three
 * fields that apply on commitment and never on keystrokes, three
 * visibility switches, exactly two actions, east-positive stated
 * where the number is typed, the place remembered and the instant
 * not, and nothing - nothing - that moves the page except the one
 * button whose name says it will.
 */
class PlaceAndTimeDialogTest {

    private static final Instant WHEN =
            Instant.parse("2026-03-20T21:33:00Z");

    private final Preferences node = Preferences.userRoot().node(
            "juranometria-test-place-" + System.nanoTime());

    @AfterEach
    void dropTestNode() throws Exception {
        node.removeNode();
    }

    /** The dialog's content over a freshly attached module. */
    private static final class Rig {
        final TestChartServices services = new TestChartServices();
        final MeridianModule module;
        final PlaceStore store;
        final JComponent content;
        final List<Instant> clockReads = new ArrayList<>();
        Instant clockAnswer = Instant.parse("2027-01-01T12:00:00Z");

        Rig(Preferences node) {
            module = new MeridianModule(new Observer(59.913, 10.752, WHEN));
            module.attach(services);
            store = PlaceStore.forNode(node);
            content = PlaceAndTimeDialog.content(module, store, () -> {
                clockReads.add(clockAnswer);
                return clockAnswer;
            });
        }

        JTextField field(String name) {
            return (JTextField) named(content, name);
        }

        AbstractButton button(String name) {
            return (AbstractButton) named(content, name);
        }
    }

    private static Component named(Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = named(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<Component> all(Component root, Class<?> type) {
        List<Component> found = new ArrayList<>();
        if (type.isInstance(root)) {
            found.add(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                found.addAll(all(child, type));
            }
        }
        return found;
    }

    /** Commits a field the way Enter does. */
    private static void commit(JTextField field, String text) {
        field.setText(text);
        field.postActionEvent();
    }

    // ---- the surface is the gate's ----------------------------------

    @Test
    void threeFieldsThreeSwitchesAndExactlyTwoActions() {
        Rig rig = new Rig(node);

        assertEquals(3, all(rig.content, JTextField.class).size(),
                "latitude, longitude, instant - and no fourth field");
        List<Component> switches = all(rig.content, JCheckBox.class);
        assertEquals(3, switches.size(),
                "meridian, mathematical horizon, zenith");
        assertEquals(2, all(rig.content, JButton.class).size(),
                "two deliberate actions, and no others: no Apply, no"
                        + " Cancel, no OK");

        List<String> names = switches.stream()
                .map(box -> ((JCheckBox) box).getText()).toList();
        assertTrue(names.contains("Mathematical horizon"),
                "the horizon wears its full name on the switch too: "
                        + names);
    }

    @Test
    void eastPositiveIsStatedWhereTheNumberIsTyped() {
        Rig rig = new Rig(node);
        boolean stated = all(rig.content, JLabel.class).stream()
                .map(label -> ((JLabel) label).getText())
                .anyMatch(text -> text != null
                        && text.toLowerCase(java.util.Locale.ROOT)
                                .contains("east positive"));
        assertTrue(stated,
                "the label says which way longitude counts - the"
                        + " single easiest thing to get wrong, and a"
                        + " chart drawn for the wrong hemisphere looks"
                        + " entirely plausible");
    }

    @Test
    void everyControlSpeaksItsNameAndPurpose() {
        Rig rig = new Rig(node);
        for (Component control : all(rig.content, JComponent.class)) {
            if (!(control instanceof JTextField)
                    && !(control instanceof AbstractButton)) {
                continue;
            }
            var accessible = control.getAccessibleContext();
            assertTrue(accessible.getAccessibleName() != null
                            && !accessible.getAccessibleName().isBlank(),
                    "a control a reader cannot see still says what it"
                            + " is: " + control.getName());
            assertTrue(accessible.getAccessibleDescription() != null,
                    "and what it does: " + control.getName());
        }
        for (Component label : all(rig.content, JLabel.class)) {
            if (((JLabel) label).getLabelFor() != null) {
                assertTrue(((JLabel) label).getDisplayedMnemonic() != 0,
                        "a field's label carries a mnemonic, so the"
                                + " keyboard reaches it: "
                                + ((JLabel) label).getText());
            }
        }
    }

    // ---- typing is nothing, commitment is everything ----------------

    @Test
    void typingChangesNothingUntilTheFieldIsCommitted() {
        Rig rig = new Rig(node);
        Observer before = rig.module.observer();

        rig.field("latitudeField").setText("-33.87");

        assertEquals(before, rig.module.observer(),
                "typing is not asking: the module holds what it held");
        assertEquals(false, rig.store.remembered(),
                "and nothing was remembered");

        rig.field("latitudeField").postActionEvent();
        assertEquals(-33.87, rig.module.observer().latitudeDegrees(),
                "commitment applies it");
    }

    @Test
    void committingAPlaceRemembersItAndCommittingAnInstantDoesNot()
            throws Exception {
        Rig rig = new Rig(node);
        commit(rig.field("latitudeField"), "-33.87");
        commit(rig.field("longitudeField"), "151.21");
        commit(rig.field("instantField"), "2026-06-01 03:00:00");
        rig.store.flush();

        Observer reloaded = PlaceStore.forNode(node).load(WHEN);
        assertEquals(-33.87, reloaded.latitudeDegrees(),
                "a fresh session gets the reader's place back");
        assertEquals(151.21, reloaded.eastLongitudeDegrees());
        assertEquals(WHEN, reloaded.instant(),
                "with whatever instant that session states - the"
                        + " committed one was applied, never stored");
        for (String key : node.keys()) {
            assertTrue(!key.toLowerCase(java.util.Locale.ROOT)
                            .contains("instant")
                            && !key.toLowerCase(java.util.Locale.ROOT)
                                    .contains("time"),
                    "no key remembers a moment, so a stale clock"
                            + " cannot masquerade as Now: " + key);
        }
    }

    @Test
    void anEntryThatDoesNotSurviveParsingIsPutBackAndNeverApplied() {
        Rig rig = new Rig(node);
        Observer before = rig.module.observer();

        for (String wrong : List.of("north-ish", "91", "-90.001", "NaN",
                "", "10,752")) {
            commit(rig.field("latitudeField"), wrong);
            assertEquals(before, rig.module.observer(),
                    "the module never sees a wrong entry: " + wrong);
            assertEquals("59.913", rig.field("latitudeField").getText(),
                    "and the field is put back as it was: " + wrong);
        }
        for (String wrong : List.of("today", "2026-13-01 00:00",
                "21:33 2026-03-20", "2026-03-20")) {
            commit(rig.field("instantField"), wrong);
            assertEquals(before, rig.module.observer(),
                    "an instant that is not one applies nothing: "
                            + wrong);
        }
        assertEquals(false, rig.store.remembered(),
                "and none of it was remembered");
    }

    @Test
    void anInstantAloneRemembersNothingAtAll() {
        // On a store that has never held a place: an instant commit
        // that also wrote the place would have been invisible to the
        // test above, which had already stored the same values - a
        // mutation proved it. Here there is nothing for it to hide
        // behind.
        Rig rig = new Rig(node);

        commit(rig.field("instantField"), "2026-06-01 03:00:00");

        assertEquals(Instant.parse("2026-06-01T03:00:00Z"),
                rig.module.observer().instant(), "the instant applied");
        assertEquals(false, rig.store.remembered(),
                "and the store is untouched: a reader who set only a"
                        + " moment has told the atlas nothing worth"
                        + " keeping");
    }

    @Test
    void theInstantIsAcceptedWithOrWithoutSeconds() {
        Rig rig = new Rig(node);
        commit(rig.field("instantField"), "2026-03-20 22:00");
        assertEquals(Instant.parse("2026-03-20T22:00:00Z"),
                rig.module.observer().instant(),
                "the mock-up's minute form is honoured");
        assertEquals("2026-03-20 22:00:00",
                rig.field("instantField").getText(),
                "and shown back with the seconds it now carries");
    }

    // ---- the directions a reader would notice going wrong -----------

    @Test
    void movingEastMovesTheSiderealClockForwardByTheSameAngle() {
        Rig rig = new Rig(node);
        double before = new LocalSky(rig.module.observer())
                .localSiderealTimeDegrees();

        commit(rig.field("longitudeField"), "20.752");

        double after = new LocalSky(rig.module.observer())
                .localSiderealTimeDegrees();
        assertEquals(10.0, angleForward(before, after), 1e-9,
                "ten degrees further east is ten degrees later in the"
                        + " sidereal day - the sign the label promises");
    }

    @Test
    void anHourLaterTurnsTheSkyByFifteenDegreesAndABit() {
        Rig rig = new Rig(node);
        double before = new LocalSky(rig.module.observer())
                .localSiderealTimeDegrees();

        commit(rig.field("instantField"), "2026-03-20 22:33:00");

        double turned = angleForward(before,
                new LocalSky(rig.module.observer())
                        .localSiderealTimeDegrees());
        assertEquals(15.041, turned, 0.01,
                "a sidereal hour is a solar hour and a bit: the sky"
                        + " turned " + turned + " degrees");
    }

    @Test
    void latitudeMovesTheZenithAndNothingElseDoesItSideways() {
        Rig rig = new Rig(node);
        double before = new LocalSky(rig.module.observer())
                .zenith().decDegrees();

        commit(rig.field("latitudeField"), "-33.87");

        double after = new LocalSky(rig.module.observer())
                .zenith().decDegrees();
        assertTrue(before > 59 && after < -33,
                "the zenith followed the reader south: "
                        + before + " to " + after);
    }

    // ---- nothing moves the page but the button that says so ---------

    @Test
    void onlyCentreOnZenithMovesThePageAndOnlyOncePerPress() {
        Rig rig = new Rig(node);

        commit(rig.field("latitudeField"), "-33.87");
        commit(rig.field("longitudeField"), "151.21");
        commit(rig.field("instantField"), "2026-06-01 03:00:00");
        rig.button("nowButton").doClick();
        for (Component box : all(rig.content, JCheckBox.class)) {
            ((JCheckBox) box).doClick();
        }
        assertEquals(List.of(), rig.services.requested,
                "a reader set a whole session's worth of state and the"
                        + " page never moved");

        rig.button("centreButton").doClick();
        assertEquals(1, rig.services.requested.size(),
                "the one button whose name says it moves the page"
                        + " moved it, once");
        assertEquals(new LocalSky(rig.module.observer()).zenith(),
                rig.services.requested.get(0).centre(),
                "to the point overhead for the state the reader"
                        + " entered");
    }

    @Test
    void nowReadsTheClockOncePerPressAndNothingTicksAfterwards() {
        Rig rig = new Rig(node);

        rig.button("nowButton").doClick();

        assertEquals(1, rig.clockReads.size(),
                "one press, one read: Now is a button, not a state");
        assertEquals(rig.clockAnswer, rig.module.observer().instant(),
                "frozen on the moment it was pressed");
        assertEquals("2027-01-01 12:00:00",
                rig.field("instantField").getText(),
                "and the field says so");

        rig.clockAnswer = Instant.parse("2027-01-01T12:00:07Z");
        assertEquals(1, rig.clockReads.size(),
                "and the clock is not read again until the reader"
                        + " presses again - nothing polls it");
        rig.button("nowButton").doClick();
        assertEquals(2, rig.clockReads.size(),
                "pressing again is how a reader moves the sky forward");
        assertEquals(rig.clockAnswer, rig.module.observer().instant());
    }

    // ---- the switches lose nothing ----------------------------------

    @Test
    void disablingTheOverlayKeepsEverythingTheReaderEntered() {
        Rig rig = new Rig(node);
        commit(rig.field("latitudeField"), "-33.87");
        commit(rig.field("instantField"), "2026-06-01 03:00:00");
        Observer entered = rig.module.observer();
        var before = rig.module.contributedGeometry();

        for (Component box : all(rig.content, JCheckBox.class)) {
            ((JCheckBox) box).doClick();
        }
        assertEquals(List.of(), rig.module.contributedGeometry(),
                "all three off is the overlay disabled");
        assertEquals(entered, rig.module.observer(),
                "and the reader's place and instant are exactly where"
                        + " they left them");

        for (Component box : all(rig.content, JCheckBox.class)) {
            ((JCheckBox) box).doClick();
        }
        assertEquals(before, rig.module.contributedGeometry(),
                "re-enabling shows the same sky, not a reset one");
    }

    @Test
    void aFreshSessionWearsThePlaceAndNotTheSwitchesOrTheInstant() {
        // The restart, as the wiring performs it: the place from the
        // store, the instant stated by the new session, the switches
        // off - the gate approves persisting the place and nothing
        // else, so the reference lines are a choice a reader makes
        // each time.
        Rig first = new Rig(node);
        commit(first.field("latitudeField"), "66.5");
        commit(first.field("longitudeField"), "25.7");
        commit(first.field("instantField"), "2026-06-01 03:00:00");
        first.store.flush();

        Instant secondSession = Instant.parse("2026-09-04T20:00:00Z");
        MeridianModule reborn = new MeridianModule(
                PlaceStore.forNode(node).load(secondSession));
        reborn.showing(false, false, false);

        assertEquals(66.5, reborn.observer().latitudeDegrees(),
                "the place came back");
        assertEquals(25.7, reborn.observer().eastLongitudeDegrees());
        assertEquals(secondSession, reborn.observer().instant(),
                "the instant did not - it is the new session's own");
        assertEquals(List.of(), reborn.contributedGeometry(),
                "and the session begins with the ordinary chart");
    }

    // ----------------------------------------------------------------

    /** How far the sidereal clock moved forward, in [0, 360). */
    private static double angleForward(double from, double to) {
        double turned = (to - from) % 360.0;
        return turned < 0 ? turned + 360.0 : turned;
    }

    @Test
    void aModulelessMenuHasNoPlaceAndTimeItemAndAModularOneDoes() {
        // The item exists exactly when the module does: an atlas
        // built without the meridian module must not offer a dialog
        // that reaches nothing.
        javax.swing.JMenuBar without = juranometria.app.AppMenuBar
                .create(null, () -> { }, () -> { }, () -> { }, null);
        javax.swing.JMenuBar with = juranometria.app.AppMenuBar
                .create(null, () -> { }, () -> { }, () -> { }, null,
                        () -> { });

        assertEquals(false, hasItem(without, "Place and Time..."),
                "no module, no item");
        assertTrue(hasItem(with, "Place and Time..."),
                "the item is on the View menu, beside Chart Options");
    }

    private static boolean hasItem(javax.swing.JMenuBar bar, String text) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            var menu = bar.getMenu(i);
            for (int j = 0; j < menu.getItemCount(); j++) {
                var item = menu.getItem(j);
                if (item != null && text.equals(item.getText())) {
                    return true;
                }
            }
        }
        return false;
    }
}
