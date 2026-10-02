package juranometria.ui.placeandtime;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import juranometria.meridian.MeridianModule;
import juranometria.module.TestChartServices;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Place and Time as one presentation in two hosts (#434, ruled on
 * #433): the dialog's content and the companion's section are the same
 * class over the same module, so a change made in either is shown in
 * the other at once, and an entry refused in either is refused the
 * same way, with the same words, and changes nothing.
 */
class PlaceAndTimePanelTest {

    private static final Instant WHEN = Instant.parse("2026-03-20T21:33:00Z");

    private final Preferences node = Preferences.userRoot().node(
            "juranometria-test-place-panel-" + System.nanoTime());

    @AfterEach
    void dropTestNode() throws Exception {
        node.removeNode();
    }

    private final TestChartServices services = new TestChartServices();
    private final MeridianModule module = attached();

    private MeridianModule attached() {
        MeridianModule attached = new MeridianModule(
                new Observer(59.913, 10.752, WHEN));
        attached.attach(services);
        attached.showing(false, false, false);
        return attached;
    }

    private PlaceAndTimePanel host(String language) {
        return new PlaceAndTimePanel(module, PlaceStore.forNode(node),
                () -> WHEN.plusSeconds(3600),
                InterfaceText.forLanguage(language));
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

    private static JTextField field(PlaceAndTimePanel panel, String name) {
        return (JTextField) named(panel, name);
    }

    private static AbstractButton button(PlaceAndTimePanel panel, String name) {
        return (AbstractButton) named(panel, name);
    }

    private static void commit(JTextField field, String text) {
        field.setText(text);
        field.postActionEvent();
    }

    @Test
    void theDialogHoldsThePanelTheCompanionHolds() {
        assertInstanceOf(PlaceAndTimePanel.class, PlaceAndTimeDialog.content(
                module, PlaceStore.forNode(node), () -> WHEN,
                InterfaceText.forLanguage("en")),
                "one implementation, two hosts");
        assertInstanceOf(PlaceAndTimePanel.class,
                PlaceAndTimeDialog.contentForStudy(module,
                        PlaceStore.forNode(node), InterfaceText.forLanguage("en")));
    }

    @Test
    void aChangeInEitherHostIsShownInTheOtherAtOnce() {
        PlaceAndTimePanel dialog = host("en");
        PlaceAndTimePanel companion = host("en");
        assertEquals(2, module.subscribers(), "each follows once");

        commit(field(companion, "latitudeField"), "-33.87");
        assertEquals("-33.87", field(dialog, "latitudeField").getText(),
                "typed in the companion, shown in the dialog");
        commit(field(dialog, "instantField"), "2027-01-01 00:00");
        assertEquals("2027-01-01 00:00:00",
                field(companion, "instantField").getText(),
                "typed in the dialog, shown in the companion");

        button(dialog, "showZenith").doClick();
        assertTrue(button(companion, "showZenith").isSelected());
        button(companion, "showMeridian").doClick();
        assertTrue(button(dialog, "showMeridian").isSelected());
        assertTrue(module.zenithShowing() && module.meridianShowing(),
                "and neither undid the other's line");

        // The chart keyboard writes the authority directly.
        module.showing(module.meridianShowing(), true,
                module.zenithShowing());
        assertTrue(button(dialog, "showMathematicalhorizon").isSelected()
                && button(companion, "showMathematicalhorizon").isSelected());

        button(companion, "nowButton").doClick();
        assertEquals("2026-03-20 22:33:00",
                field(dialog, "instantField").getText(),
                "Now in one is the instant in both");
    }

    @Test
    void aRefusedEntryIsRefusedTheSameWayInBothHostsAndChangesNothing() {
        for (String language : new String[] {"en", "nb-NO"}) {
            PlaceAndTimePanel dialog = host(language);
            PlaceAndTimePanel companion = host(language);
            Observer before = module.observer();
            int redraws = services.redraws;
            for (String[] entry : new String[][] {
                    {"latitudeField", "91"}, {"longitudeField", "east"},
                    {"instantField", "2026-02-30 12:00"}}) {
                commit(field(dialog, entry[0]), entry[1]);
                commit(field(companion, entry[0]), entry[1]);
                String said = dialog.refusalText();
                assertFalse(said.isEmpty(), language + " " + entry[0]
                        + ": the dialog says why");
                assertEquals(said, companion.refusalText(), language + " "
                        + entry[0] + ": the same words in the companion");
                assertTrue(said.contains(field(dialog, entry[0]).getText()),
                        "it names the value kept: " + said);
                assertEquals(said, field(companion, entry[0])
                        .getAccessibleContext().getAccessibleDescription(),
                        "and the field the reader is on carries it");
            }
            assertEquals(before, module.observer(),
                    language + ": no refused entry reached the module");
            assertEquals(redraws, services.redraws,
                    language + ": nor redrew the chart");

            commit(field(dialog, "latitudeField"), "60");
            assertEquals("", dialog.refusalText(),
                    "a good entry clears the line");
            assertTrue(!field(dialog, "latitudeField").getAccessibleContext()
                    .getAccessibleDescription().equals(companion.refusalText()),
                    "and gives the field its own description back");
            button(companion, "nowButton").doClick();
            assertEquals("", companion.refusalText(), "so does Now");
            module.observer(module.observer().from(59.913, 10.752));
        }
    }

    @Test
    void theRefusalWordsAreTheLanguagesOwn() {
        PlaceAndTimePanel english = host("en");
        PlaceAndTimePanel norwegian = host("nb-NO");
        commit(field(english, "latitudeField"), "95");
        commit(field(norwegian, "latitudeField"), "95");
        assertEquals("Latitude must be between −90° and 90°. Kept 59.913.",
                english.refusalText());
        assertEquals("Breddegraden må ligge mellom −90° og 90°. Beholdt 59.913.",
                norwegian.refusalText());
    }
}
