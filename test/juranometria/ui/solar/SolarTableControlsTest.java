package juranometria.ui.solar;

import java.awt.Component;
import java.awt.Container;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.ui.companion.CompanionStore;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.solar.SolarTableSession.Mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One body's controls in their two hosts (Sprint 42, issue #458, ruled
 * on #457): the dialog's content and the Controller's group over one
 * session. A query applied in either is shown by both; a result
 * arriving never overwrites the other host's unfinished draft; the
 * Controller's group computes nothing when built, carries no access
 * letters, shows the instant as a card and a range as the table, and
 * carries the body's Show on chart box following the shared switch.
 * Headless, of the content-test kind, as the dialog's own tests are.
 */
class SolarTableControlsTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    /** The observer the dialog tests state: Oslo at midsummer 2026, as SunTableDialogTest's OSLO. */
    private static Observer oslo() {
        return new Observer(59.91, 10.75, java.time.Instant.parse("2026-06-21T10:00:00Z"));
    }

    /** A fake of the body's chart switch, standing in for the module. */
    static final class FakeOnChart implements BodyOnChart {
        boolean shown;
        final List<Consumer<Boolean>> listeners = new ArrayList<>();

        @Override
        public boolean showing() {
            return shown;
        }

        @Override
        public void show(boolean wanted) {
            shown = wanted;
            for (Consumer<Boolean> listener : List.copyOf(listeners)) {
                listener.accept(shown);
            }
        }

        @Override
        public void onChange(Consumer<Boolean> listener) {
            listeners.add(listener);
            listener.accept(shown);
        }
    }

    /** Two hosts over one session. */
    private static final class Hosts {
        final Observer[] observer = {oslo()};
        final SolarTableSession session;
        final SolarTableDialog.Content dialog;
        final SolarTableControls controller;
        final JComponent group;
        final FakeOnChart onChart = new FakeOnChart();

        Hosts(SolarTable table) throws Exception {
            session = new SolarTableSession(() -> observer[0], service, table);
            Object[] built = new Object[3];
            SwingUtilities.invokeAndWait(() -> {
                built[0] = new SolarTableControls(session, EN, onChart, false);
                built[1] = ((SolarTableControls) built[0]).inController();
                built[2] = SolarTableDialog.content(session, EN);
            });
            controller = (SolarTableControls) built[0];
            group = (JComponent) built[1];
            dialog = (SolarTableDialog.Content) built[2];
        }
    }

    private static List<Component> all(Component root) {
        List<Component> found = new ArrayList<>();
        found.add(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                found.addAll(all(child));
            }
        }
        return found;
    }

    private static Component named(Component root, String name) {
        return all(root).stream().filter(c -> name.equals(c.getName()))
                .findFirst().orElse(null);
    }

    @Test
    void theControllersGroupComputesNothingWhenBuilt() throws Exception {
        Observer[] observer = {oslo()};
        SolarTableSession session = new SolarTableSession(() -> observer[0], service,
                SolarTable.sun());
        SolarTableControls[] built = new SolarTableControls[1];
        JComponent[] group = new JComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            built[0] = new SolarTableControls(session, EN, new FakeOnChart(), false);
            group[0] = built[0].inController();
        });
        assertEquals(0, session.computations(), "building the group asks the service nothing");
        assertTrue(built[0].observerNote.getText().contains("59.91"),
                "but the observer note is read: " + built[0].observerNote.getText());
        assertEquals(" ", built[0].status(), "and there is nothing to say yet");
        assertNull(named(group[0], "sunCard"), "no result, no card");
        assertTrue(Boolean.TRUE.equals(group[0].getClientProperty(SolarTableControls.MARK)));
    }

    @Test
    void aQueryAppliedInEitherHostIsShownByBoth() throws Exception {
        Hosts hosts = new Hosts(SolarTable.sun());
        assertEquals(1, hosts.session.computations(),
                "the dialog's content computes when built, as ruled on #400");
        assertEquals(1, hosts.controller.model.getRowCount(),
                "and the Controller's group shows that result");
        assertNotNull(named(hosts.group, "sunCard"), "the instant as a card");

        // A range applied from the Controller: the dialog shows it.
        String end = SolarTableSession.shown(hosts.observer[0].instant()
                .plus(Duration.ofDays(2)).plus(Duration.ofHours(10)));
        SwingUtilities.invokeAndWait(() -> {
            hosts.controller.rangeView.setSelected(true);
            hosts.controller.end.setText(end);
            hosts.controller.apply();
        });
        assertEquals(4, hosts.dialog.model.getRowCount(), "the dialog shows the range");
        assertEquals(hosts.dialog.status(), hosts.controller.status(),
                "and says the same: " + hosts.dialog.status());
        assertTrue(hosts.dialog.instantView.isSelected(),
                "the dialog's own draft is still the instant view");
        assertNull(named(hosts.group, "sunCard"), "a range is the table, not a card");
        assertNotNull(named(hosts.group, "sunTable"));

        // Update from the dialog: its own view, over a fresh observer.
        hosts.observer[0] = new Observer(hosts.observer[0].latitudeDegrees(),
                hosts.observer[0].eastLongitudeDegrees(),
                hosts.observer[0].instant().plus(Duration.ofDays(1)));
        SwingUtilities.invokeAndWait(hosts.dialog::update);
        assertEquals(1, hosts.controller.model.getRowCount(),
                "the dialog's instant view, shown by the Controller");
        assertTrue(hosts.controller.observerNote.getText().contains("2026-06-22"),
                "over the fresh observer: " + hosts.controller.observerNote.getText());
    }

    @Test
    void aSharedResultNeverOverwritesAnUnfinishedDraft() throws Exception {
        Hosts hosts = new Hosts(SolarTable.moon());
        SwingUtilities.invokeAndWait(() -> {
            hosts.controller.rangeView.setSelected(true);
            hosts.controller.start.setText("2026-06-2");
            hosts.dialog.rangeView.setSelected(true);
            hosts.dialog.update();
        });
        assertEquals("2026-06-2", hosts.controller.start.getText(),
                "the Controller's unfinished text survives the dialog's result");
        assertTrue(hosts.controller.rangeView.isSelected());
        assertEquals(8, hosts.controller.model.getRowCount(), "while the rows are shared");
        SwingUtilities.invokeAndWait(hosts.controller::apply);
        assertEquals(SolarTableSession.Outcome.REFUSED_INSTANT,
                hosts.session.result().outcome(), "applied, the draft is refused as typed");
        assertTrue(hosts.dialog.status().contains("2026-06-2"),
                "and the dialog says so too: " + hosts.dialog.status());
    }

    @Test
    void theControllersGroupCarriesNoAccessLettersAndTheDialogKeepsIts() throws Exception {
        Hosts hosts = new Hosts(SolarTable.sun());
        for (Component c : all(hosts.group)) {
            if (c instanceof AbstractButton button) {
                assertEquals(0, button.getMnemonic(), button.getText() + " in the Controller");
            }
            if (c instanceof JLabel label) {
                assertEquals(0, label.getDisplayedMnemonic(), label.getText());
            }
        }
        assertTrue(hosts.dialog.instantView.getMnemonic() != 0, "the dialog's letters stay");
        for (JTextField field : List.of(hosts.controller.start, hosts.controller.end)) {
            assertNotNull(field.getAccessibleContext().getAccessibleName());
            assertTrue(all(hosts.group).stream().anyMatch(c -> c instanceof JLabel label
                    && label.getLabelFor() == field), "each field has its label");
        }
        assertNotNull(named(hosts.group, "sunOnChart"), "the body's Show on chart box");
    }

    @Test
    void showOnChartFollowsTheSharedSwitchAndAsksIt() throws Exception {
        Hosts hosts = new Hosts(SolarTable.sun());
        AbstractButton box = (AbstractButton) named(hosts.group, "sunOnChart");
        assertFalse(box.isSelected());
        hosts.onChart.show(true);
        assertTrue(box.isSelected(), "a change made elsewhere is shown");
        SwingUtilities.invokeAndWait(() -> {
            box.setSelected(false);
            for (java.awt.event.ActionListener l : box.getActionListeners()) {
                l.actionPerformed(new java.awt.event.ActionEvent(box, 0, "press"));
            }
        });
        assertFalse(hosts.onChart.showing(), "the box asks the switch");
        assertEquals(1, hosts.session.computations(),
                "and toggling computes no table");
        assertNull(hosts.dialog.controls().onChart, "the dialog offers no switch");
    }

    @Test
    void theSectionRemembersEachGroupApartAndIntroducesMoonCollapsed() throws Exception {
        juranometria.app.SwingSession.scratchPreferences("solar-section", node -> {
            Observer observer = oslo();
            SolarTableSession sun = new SolarTableSession(() -> observer, service,
                    SolarTable.sun());
            SolarTableSession moon = new SolarTableSession(() -> observer, service,
                    SolarTable.moon());
            CompanionStore store = CompanionStore.forNode(node);
            JComponent[] built = new JComponent[1];
            SwingUtilities.invokeAndWait(() -> built[0] = new SolarSystemSection(sun, moon,
                    new FakeOnChart(), new FakeOnChart(), EN).inController(store, EN));
            AbstractButton sunHeading = (AbstractButton) named(built[0],
                    "heading.solarsystem.sun");
            AbstractButton moonHeading = (AbstractButton) named(built[0],
                    "heading.solarsystem.moon");
            assertTrue(sunHeading.isSelected(), "Sun introduced open");
            assertFalse(moonHeading.isSelected(), "Moon introduced collapsed");
            assertEquals(0, sun.computations() + moon.computations(),
                    "building the section computes nothing");
            // Collapse is the companion's own, held by its store and
            // proved by CompanionSection's tests; here it is written as
            // a reader's press leaves it, and read back apart.
            store.saveCollapsed("solarsystem.sun", true);
            store.saveCollapsed("solarsystem.moon", false);
            JComponent[] again = new JComponent[1];
            SwingUtilities.invokeAndWait(() -> again[0] = new SolarSystemSection(sun, moon,
                    new FakeOnChart(), new FakeOnChart(), EN).inController(store, EN));
            assertFalse(((AbstractButton) named(again[0], "heading.solarsystem.sun"))
                    .isSelected(), "Sun closed is remembered");
            assertTrue(((AbstractButton) named(again[0], "heading.solarsystem.moon"))
                    .isSelected(), "Moon opened is remembered, apart");
            assertEquals(0, sun.computations() + moon.computations(),
                    "and rebuilding over remembered collapse computes nothing");
            for (String key : node.keys()) {
                assertTrue(key.startsWith("companion.collapsed."), key);
            }
        });
    }
}
