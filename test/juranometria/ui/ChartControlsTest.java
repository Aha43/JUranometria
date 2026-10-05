package juranometria.ui;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SelectionMode;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartStructure;
import juranometria.search.LocalSearch;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart's controls in their two hosts (Sprint 41, issue #450,
 * ruled on #449): the atlas toolbar and the Controls companion each
 * hold one {@link ChartControls} over the same authorities and the
 * same {@link ChartActions}, so a change made to any authority is shown
 * by both at once, with the same words; search text stays local while
 * its effect is shared; Home clears the shared target and every field;
 * and every control, Emphasis included, can be reached by keyboard in
 * both. Headless, of the content-test kind: the authorities are driven
 * here and both hosts read; the buttons themselves are pressed for real
 * in {@code ChartControlsCompanionJourneyTest}.
 */
class ChartControlsTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");

    /** Two hosts over one set of authorities. */
    private static final class Hosts {
        final SceneAssembler assembler;
        final ChartViewController controller;
        final InspectorToggle inspector = new InspectorToggle();
        final SelectionMode mode = new SelectionMode();
        final ZoomLock lock = new ZoomLock();
        final ChartActions actions;
        final LocalSearch search;
        final ChartComponent chart;
        final AtlasToolbar bar;
        final ChartControls companion;
        final JComponent section;
        boolean inspectorShowing;

        Hosts() throws Exception {
            assembler = new SceneAssembler(new SceneAssemblerTest.CountingCatalogue(),
                    SearchFieldTest.DATA_CENTRE, 10.0, 1.5);
            controller = new ChartViewController(assembler::fits);
            actions = new ChartActions(controller);
            search = new LocalSearch(List.of(), List.of(
                    new DeepSkyObject("NGC 224", List.of("M 31", "Andromeda Galaxy"),
                            DsoType.GALAXY, SearchFieldTest.M31_POSITION,
                            177.83, 69.66, 35.0, 3.44, 1),
                    new DeepSkyObject("NGC 221", List.of("M 32"), DsoType.GALAXY,
                            new SkyPosition(10.674, 40.865), 8.0, 6.0, 10.0, 8.1, 1)));
            inspector.bind(() -> inspector.report(inspectorShowing = !inspectorShowing),
                    () -> true);
            Object[] built = new Object[4];
            SwingUtilities.invokeAndWait(() -> {
                ChartComponent drawn = new ChartComponent(assembler,
                        juranometria.ui.language.PageText.in(EN));
                controller.onChange(drawn::setViewState);
                AtlasToolbar toolbar = new AtlasToolbar(controller,
                        new SearchField(search, assembler, controller, EN),
                        inspector, "0.0.0", () -> { }, mode, lock, actions, EN);
                toolbar.attachEmphasis(drawn);
                ChartControls second = new ChartControls(controller,
                        new SearchField(search, assembler, controller, EN),
                        inspector, mode, lock, actions, EN);
                second.attachEmphasis(drawn);
                built[0] = drawn;
                built[1] = toolbar;
                built[2] = second;
                built[3] = second.inCompanion();
            });
            chart = (ChartComponent) built[0];
            bar = (AtlasToolbar) built[1];
            companion = (ChartControls) built[2];
            section = (JComponent) built[3];
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

    private static AbstractButton named(Component root, String accessibleName) {
        return (AbstractButton) all(root).stream()
                .filter(c -> c instanceof AbstractButton b
                        && accessibleName.equals(
                                b.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no control named " + accessibleName));
    }

    private static JMenuItem item(JPopupMenu menu, String text) {
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenuItem item && text.equals(item.getText())) {
                return item;
            }
        }
        throw new AssertionError("no menu item " + text);
    }

    @Test
    void bothHostsHoldTheSameControlsClassOverTheSameActions() throws Exception {
        Hosts hosts = new Hosts();
        assertSame(hosts.actions, hosts.bar.controls().actions());
        assertSame(hosts.actions, hosts.companion.actions());
        assertTrue(Boolean.TRUE.equals(hosts.section.getClientProperty(
                ChartControls.MARK)), "the section is the controls' own host");
        for (String name : new String[] {"Zoom in", "Zoom out", "Lock zoom",
                "Fewer stars", "More stars", "Reset view", "Inspector",
                "Accumulate selection", "Emphasis"}) {
            named(hosts.bar, name);
            named(hosts.section, name);
        }
        assertEquals(2, hosts.actions.fields().size(),
                "Home knows both fields, one per host");
        assertEquals(1, all(hosts.section).stream()
                .filter(c -> c instanceof SearchField).count());
        assertTrue(all(hosts.section).stream().noneMatch(c ->
                c instanceof AbstractButton b && "Exit JUranometria".equals(
                        b.getAccessibleContext().getAccessibleName())),
                "no door in the companion (ruled on #449)");
        assertTrue(all(hosts.section).stream().noneMatch(c ->
                c instanceof javax.swing.JLabel l
                        && String.valueOf(l.getText()).startsWith("v0.")),
                "and no version");
    }

    @Test
    void aChangeToAnyAuthorityIsShownByBothHostsWithTheSameWords() throws Exception {
        Hosts hosts = new Hosts();
        SwingUtilities.invokeAndWait(hosts.controller::zoomIn);
        assertEquals(hosts.controller.state().fieldWidthDegrees(),
                hosts.chart.viewState().fieldWidthDegrees());
        assertEquals("Field 6° · Stars to V 8.0", hosts.companion.readout().getText());
        assertEquals(hosts.bar.controls().readout().getText(),
                hosts.companion.readout().getText(), "one readout, two labels");
        SwingUtilities.invokeAndWait(() -> {
            while (hosts.controller.canZoomIn()) {
                hosts.controller.zoomIn();
            }
        });
        assertFalse(named(hosts.section, "Zoom in").isEnabled()
                        || named(hosts.bar, "Zoom in").isEnabled(),
                "the end of the ladder disables the step in both hosts");
        assertEquals(named(hosts.bar, "Zoom in").getToolTipText(),
                named(hosts.section, "Zoom in").getToolTipText());
        assertEquals(named(hosts.bar, "Zoom in").getAccessibleContext()
                        .getAccessibleDescription(),
                named(hosts.section, "Zoom in").getAccessibleContext()
                        .getAccessibleDescription(),
                "and says so in the same words");

        SwingUtilities.invokeAndWait(() -> hosts.lock.lock(true));
        assertTrue(named(hosts.bar, "Lock zoom").isSelected());
        assertTrue(named(hosts.section, "Lock zoom").isSelected());

        SwingUtilities.invokeAndWait(() -> hosts.mode.accumulate(true));
        assertTrue(named(hosts.bar, "Accumulate selection").isSelected());
        assertTrue(named(hosts.section, "Accumulate selection").isSelected());

        SwingUtilities.invokeAndWait(hosts.inspector::toggle);
        assertTrue(hosts.inspector.isShowing());
        assertTrue(named(hosts.bar, "Inspector").isSelected());
        assertTrue(named(hosts.section, "Inspector").isSelected());
        assertEquals(named(hosts.bar, "Inspector").getToolTipText(),
                named(hosts.section, "Inspector").getToolTipText());
    }

    @Test
    void emphasisIsOneMenuReadingTheChartFromEitherHost() throws Exception {
        Hosts hosts = new Hosts();
        JPopupMenu[] menus = new JPopupMenu[3];
        SwingUtilities.invokeAndWait(() -> {
            menus[0] = hosts.actions.emphasisMenu(EN);
            hosts.chart.toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
            menus[1] = hosts.bar.emphasisMenu(hosts.chart);
            menus[2] = hosts.companion.actions().emphasisMenu(EN);
        });
        assertFalse(item(menus[0], "Normal").isEnabled(), "nothing raised: no Normal");
        assertTrue(item(menus[1], "Equatorial grid").isSelected(),
                "a menu opened from the bar reads the chart");
        assertTrue(item(menus[2], "Equatorial grid").isSelected(),
                "and so does one opened from the companion: the same menu");
        assertTrue(item(menus[2], "Normal").isEnabled());
        assertSame(hosts.chart, hosts.companion.actions().chart());
    }

    @Test
    void searchTextIsLocalAndItsEffectIsShared() throws Exception {
        Hosts hosts = new Hosts();
        SearchField barField = (SearchField) all(hosts.bar).stream()
                .filter(c -> c instanceof SearchField).findFirst().orElseThrow();
        SearchField companionField = hosts.companion.search();
        SearchField.Outcome[] outcome = new SearchField.Outcome[1];
        SwingUtilities.invokeAndWait(() -> {
            barField.setText("M 3");
            companionField.setText("M 32");
            outcome[0] = companionField.handle(companionField.getText());
        });
        assertEquals(SearchField.Outcome.RECENTERED, outcome[0]);
        assertEquals("M 32", companionField.getText());
        assertEquals("M 3", barField.getText(),
                "a search in one field leaves the other's unfinished text");
        assertEquals("NGC 221", hosts.controller.state().targetIdentity(),
                "while the chart it moved is shared");
        SwingUtilities.invokeAndWait(hosts.actions::home);
        assertNull(hosts.controller.state().targetIdentity(), "Home clears the target");
        assertEquals("", barField.getText());
        assertEquals("", companionField.getText(), "and every field, whichever"
                + " host's Home was pressed - both press this one action");
    }

    @Test
    void everyControlCanBeReachedByKeyboardInBothHosts() throws Exception {
        Hosts hosts = new Hosts();
        for (Component host : new Component[] {hosts.bar, hosts.section}) {
            for (Component control : all(host)) {
                if (control instanceof AbstractButton button) {
                    assertTrue(button.isFocusable(), button.getAccessibleContext()
                            .getAccessibleName() + " is reachable in "
                            + host.getClass().getSimpleName());
                }
            }
            assertNotNull(named(host, "Emphasis"));
            assertTrue(named(host, "Emphasis").isFocusable(),
                    "Emphasis, which the bar could not reach before #449");
        }
        assertFalse(hosts.companion.readout().isFocusable(),
                "the readout is said, not operated");
    }

    @Test
    void aHostWithoutASwitchHasNoControlForIt() throws Exception {
        ChartViewController controller = new ChartViewController();
        ChartActions actions = new ChartActions(controller);
        ChartControls[] built = new ChartControls[1];
        SwingUtilities.invokeAndWait(() -> built[0] = new ChartControls(controller,
                new SearchField(new LocalSearch(List.of(), List.of()),
                        new SceneAssembler(new SceneAssemblerTest.CountingCatalogue(),
                                SearchFieldTest.DATA_CENTRE, 10.0, 1.5),
                        controller, EN),
                null, null, null, actions, EN));
        assertNull(built[0].inspector());
        assertNull(built[0].accumulate());
        assertNull(built[0].zoomLock());
        assertNull(built[0].emphasis(), "until a chart is attached");
    }
}
