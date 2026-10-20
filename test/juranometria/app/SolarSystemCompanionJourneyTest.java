package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.ui.ChartChromeStore;
import juranometria.ui.ChartWindowStore;
import juranometria.ui.ReaderInput;
import juranometria.ui.ZoomLockStore;
import juranometria.ui.companion.CompanionSection;
import juranometria.ui.companion.CompanionWindow;
import juranometria.ui.language.SkyLanguageStore;
import juranometria.ui.solar.SunChartStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Solar System section in the real Controller (Sprint 42, issue
 * #458, ruled on #457): the real {@code JUranometriaMain.start} on a
 * scratch node. Solar System is third and introduced collapsed; opening
 * the Controller and expanding the section and its Sun group - real
 * presses on their headings - computes nothing; a real press on the
 * group's Compute computes the Sun alone; a real press on its Show on
 * chart box ticks View's item and is remembered under the key it always
 * had; the window's own focus traversal reaches the group's controls;
 * and View holds the Solar System submenu with its entries in the
 * ruled order - Jupiter... after Moon... since #474 and Saturn... last
 * since #576, whose groups follow the Moon's, introduced collapsed, and
 * opened compute nothing.
 */
class SolarSystemCompanionJourneyTest {

    @Test
    void theSectionRevealsAndComputesOnlyWhenAsked() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the real application opens a window");
        SwingSession.restoring(() -> SwingSession.scratchPreferences(
                "juranometria-458-solar", node -> run(node, frame -> {
                    CompanionWindow companion = companionOf(frame);
                    SwingUtilities.invokeAndWait(() -> companion.showCompanion(true));
                    drain();
                    // Since #507 the Solar System is the Controller's second
                    // tab, its groups standing on it with no outer group;
                    // Chart Options is the Chart tab's fourth section, after
                    // Chart controls, Emphasis (#525) and Place and Time.
                    juranometria.ui.companion.CompanionTabs tabs = companion.tabs();
                    assertEquals(List.of(juranometria.ui.companion.CompanionTabs.CHART, "solarsystem"),
                            tabs.ids());
                    assertEquals(juranometria.ui.companion.CompanionTabs.CHART, tabs.chosen(),
                            "a fresh profile opens Chart");
                    List<CompanionSection> sections = companion.sections();
                    assertEquals("section.emphasis", sections.get(1).getName(),
                            "Emphasis is the Chart tab's second section (#525)");
                    assertEquals("section.chartoptions", sections.get(3).getName(),
                            "Chart Options is the Chart tab's fourth section");
                    JComponent content = tabs.content("solarsystem");
                    assertFalse(content.isShowing(), "not shown until its tab is chosen");
                    assertNull(find(companion.getContentPane(), "section.solarsystem"),
                            "no outer Solar System group: the tab is that identity");

                    // Choosing the tab, a real press on its header, reveals;
                    // nothing is computed.
                    ReaderInput.chooseTab(tabs, "Solar System");
                    drain();
                    assertEquals("solarsystem", tabs.chosen());
                    assertTrue(content.isShowing());
                    AbstractButton sunHeading = (AbstractButton) named(content,
                            "heading.solarsystem.sun");
                    AbstractButton moonHeading = (AbstractButton) named(content,
                            "heading.solarsystem.moon");
                    assertTrue(sunHeading.isSelected(), "Sun introduced open");
                    assertFalse(moonHeading.isSelected(), "Moon introduced collapsed");
                    assertNull(find(content, "sunCard"), "revealed, not computed: no card");
                    assertNull(find(content, "sunTable"), "and no table");

                    // Compute, pressed for real: the Sun alone.
                    reveal((JComponent) named(content, "sunCompute"));
                    ReaderInput.click((JComponent) named(content, "sunCompute"));
                    drain();
                    assertNotNull(find(content, "sunCard"), "the instant's row, as a card");
                    assertNull(find(content, "sunTable"), "an instant is a card, not a table");
                    reveal(moonHeading);
                    ReaderInput.click(moonHeading);
                    drain();
                    assertTrue(moonHeading.isSelected(), "the Moon opened");
                    assertNull(find(content, "moonCard"),
                            "the Moon, opened, has computed nothing");
                    assertNull(find(content, "moonTable"));

                    // Venus (#502): between the Sun and the Moon, introduced
                    // collapsed; opened, it has computed nothing; since #514 it
                    // offers Show on chart and Centre on chart over its layer.
                    AbstractButton venusHeading = (AbstractButton) named(content,
                            "heading.solarsystem.venus");
                    java.util.List<String> headings = new java.util.ArrayList<>();
                    for (java.awt.Component c : all(content)) {
                        if (c.getName() != null && c.getName().startsWith("heading.solarsystem.")) {
                            headings.add(c.getName());
                        }
                    }
                    assertEquals(List.of("heading.solarsystem.sun", "heading.solarsystem.mercury",
                            "heading.solarsystem.venus", "heading.solarsystem.moon",
                            "heading.solarsystem.jupiter", "heading.solarsystem.saturn"), headings,
                            "outward from the Sun, the bodies the atlas has");
                    // Mercury (#530): between the Sun and Venus, introduced
                    // collapsed; opened, it has computed nothing; since #542
                    // it offers Show on chart and Centre on chart over its layer.
                    AbstractButton mercuryHeading = (AbstractButton) named(content,
                            "heading.solarsystem.mercury");
                    assertFalse(mercuryHeading.isSelected(), "Mercury introduced collapsed");
                    reveal(mercuryHeading);
                    ReaderInput.click(mercuryHeading);
                    drain();
                    assertTrue(mercuryHeading.isSelected(), "Mercury opened");
                    assertNull(find(content, "mercuryCard"), "Mercury, opened, has computed nothing");
                    assertNull(find(content, "mercuryTable"));
                    assertNotNull(find(content, "mercuryCompute"), "but its controls are there");
                    assertNotNull(find(content, "mercuryOnChart"), "Show on chart for Mercury (#542)");
                    assertFalse(((javax.swing.JCheckBox) find(content, "mercuryOnChart"))
                            .isSelected(), "off by default: opening draws nothing");
                    assertNotNull(find(content, "mercuryCentreOnChart"), "and Centre on chart");
                    assertFalse(venusHeading.isSelected(), "Venus introduced collapsed");
                    reveal(venusHeading);
                    ReaderInput.click(venusHeading);
                    drain();
                    assertTrue(venusHeading.isSelected(), "Venus opened");
                    assertNull(find(content, "venusCard"), "Venus, opened, has computed nothing");
                    assertNull(find(content, "venusTable"));
                    assertNotNull(find(content, "venusCompute"), "but its controls are there");
                    assertNotNull(find(content, "venusOnChart"), "Show on chart for Venus (#514)");
                    assertFalse(((javax.swing.JCheckBox) find(content, "venusOnChart"))
                            .isSelected(), "off by default: opening draws nothing");
                    assertNotNull(find(content, "venusCentreOnChart"), "and Centre on chart");

                    // Jupiter (#474): after the Moon, introduced collapsed;
                    // opened, it has computed nothing.
                    AbstractButton jupiterHeading = (AbstractButton) named(content,
                            "heading.solarsystem.jupiter");
                    assertFalse(jupiterHeading.isSelected(), "Jupiter introduced collapsed");
                    reveal(jupiterHeading);
                    ReaderInput.click(jupiterHeading);
                    drain();
                    assertTrue(jupiterHeading.isSelected(), "Jupiter opened");
                    assertNull(find(content, "jupiterCard"),
                            "Jupiter, opened, has computed nothing: no card");
                    assertNull(find(content, "jupiterTable"), "and no moons");
                    assertNotNull(find(content, "jupiterCompute"), "but its controls are there");
                    assertNotNull(find(content, "jupiterOnChart"),
                            "Jupiter and moons on the chart, in Jupiter's group (#484)");
                    assertFalse(((javax.swing.JCheckBox) find(content, "jupiterOnChart"))
                            .isSelected(), "off by default: opening draws nothing");

                    // Saturn (#576): after Jupiter, introduced collapsed;
                    // opened, it has computed nothing, and offers no chart
                    // switch and no Centre on chart - no Saturn is drawn.
                    AbstractButton saturnHeading = (AbstractButton) named(content,
                            "heading.solarsystem.saturn");
                    assertFalse(saturnHeading.isSelected(), "Saturn introduced collapsed");
                    reveal(saturnHeading);
                    ReaderInput.click(saturnHeading);
                    drain();
                    assertTrue(saturnHeading.isSelected(), "Saturn opened");
                    assertNull(find(content, "saturnCard"), "Saturn, opened, has computed nothing: no card");
                    assertNull(find(content, "saturnTable"), "and no moons");
                    assertNotNull(find(content, "saturnCompute"), "but its controls are there");
                    assertNull(find(content, "saturnOnChart"), "no Show on chart for Saturn");
                    assertNull(find(content, "saturnCentreOnChart"), "and no Centre on chart");

                    // Show on chart, pressed for real: View's item follows,
                    // and the key is the one it always was.
                    JComponent sunBox = (JComponent) named(content, "sunOnChart");
                    assertFalse(AppMenuBar.sunChartItem(frame.getJMenuBar()).isSelected());
                    reveal(sunBox);
                    ReaderInput.click(sunBox);
                    drain();
                    assertTrue(AppMenuBar.sunChartItem(frame.getJMenuBar()).isSelected(),
                            "the View item shows the box's choice");
                    assertEquals(java.util.Optional.of(Boolean.TRUE),
                            SunChartStore.forNode(node).shown(),
                            "remembered under sunOnChartShown");
                    assertNotNull(find(content, "sunCard"), "and toggling computed nothing new");
                    assertNull(find(content, "sunTable"));

                    // Centre on chart (#483), pressed for real: hunting the
                    // Moon from an unrelated part of the sky with its layer
                    // off, then again with it on; then the Sun.
                    juranometria.ui.ChartComponent chart = chartOf(frame);
                    assertFalse(AppMenuBar.moonChartItem(frame.getJMenuBar()).isSelected(),
                            "the Moon's layer starts off");
                    for (int hunt = 0; hunt < 2; hunt++) {
                        awayFromTheMoon(chart, hunt);
                        JComponent centre = (JComponent) named(content, "moonCentreOnChart");
                        reveal(centre);
                        ReaderInput.click(centre);
                        drain();
                        assertTrue(AppMenuBar.moonChartItem(frame.getJMenuBar()).isSelected(),
                                "the Moon's layer is on (hunt " + hunt + ")");
                        assertCentredOnTheCard(chart, content, "moonCard");
                    }
                    awayFromTheMoon(chart, 2);
                    JComponent sunCentre = (JComponent) named(content, "sunCentreOnChart");
                    reveal(sunCentre);
                    ReaderInput.click(sunCentre);
                    drain();
                    assertCentredOnTheCard(chart, content, "sunCard");

                    // Reopening a dialog adds no listener (#483): the Moon's
                    // table over a session of its own, opened and closed four
                    // times over the application's chart window.
                    juranometria.ui.solar.SolarTableSession reopened =
                            new juranometria.ui.solar.SolarTableSession(
                                    () -> new juranometria.sky.Observer(59.91, 10.75,
                                            java.time.Instant.parse("2026-06-21T10:00:00Z")),
                                    juranometria.solar.SolarSystemService.load(),
                                    juranometria.ui.solar.SolarTable.moon());
                    juranometria.ui.solar.CentreOnChart nowhere =
                            new juranometria.ui.solar.CentreOnChart(
                                    new juranometria.ui.solar.CentreOnChart.Chart() {
                                        @Override
                                        public void centre(juranometria.chart.SkyPosition p,
                                                           double f) {
                                        }

                                        @Override
                                        public void bringForward() {
                                        }

                                        @Override
                                        public double normalMinimumFieldDegrees() {
                                            return 1.0;
                                        }
                                    });
                    for (int round = 0; round < 4; round++) {
                        SwingUtilities.invokeAndWait(() ->
                                juranometria.ui.solar.SolarTableDialog.open(frame, reopened,
                                        juranometria.ui.language.InterfaceText.forLanguage("en"),
                                        nowhere, new juranometria.ui.solar.BodyOnChart() {
                                            @Override
                                            public boolean showing() {
                                                return true;
                                            }

                                            @Override
                                            public void show(boolean shown) {
                                            }

                                            @Override
                                            public void onChange(
                                                    java.util.function.Consumer<Boolean> l) {
                                            }
                                        }));
                        drain();
                        assertEquals(1, reopened.subscribers(), "an open dialog follows once");
                        SwingUtilities.invokeAndWait(() -> java.util.Arrays.stream(
                                        java.awt.Window.getWindows())
                                .filter(w -> w instanceof juranometria.ui.solar.SolarTableDialog
                                        && w.isDisplayable())
                                .forEach(java.awt.Window::dispose));
                        drain();
                        assertEquals(0, reopened.subscribers(), "closed, it has let go");
                    }

                    // View > Solar System, in the ruled order.
                    javax.swing.JMenu solar = AppMenuBar.solarSystemMenu(frame.getJMenuBar());
                    assertNotNull(solar, "View holds the submenu");
                    List<String> order = new ArrayList<>();
                    for (int i = 0; i < solar.getItemCount(); i++) {
                        order.add(solar.getItem(i) == null ? "—" : solar.getItem(i).getText());
                    }
                    assertEquals(List.of("Sun on the chart", "Mercury on the chart",
                            "Venus on the chart", "Moon on the chart",
                            "Jupiter and moons on the chart", "—",
                            "Sun...", "Mercury...", "Venus...", "Moon...", "Jupiter...", "Saturn..."),
                            order, "outward from the Sun, the switches and the tables (#530, #542,"
                                    + " #576)");

                    // The window's own focus traversal reaches the group: the
                    // chosen view (its sibling is reached by the arrow keys,
                    // as in every Swing button group), the fields, Compute,
                    // the box. Update from Place and Time stands in the
                    // agreement line since #259, offered only while the table
                    // differs from the chart - and this one agrees.
                    assertReachable(companion, content,
                            "Show the Sun at the instant set in Place and Time",
                            "From (UTC)", "To (UTC)", "Step between rows", "Compute the range",
                            "Sun on the chart");

                    // Jupiter's group the same way, by its own spoken names;
                    // then Compute, pressed for real: the card and the four
                    // moons appear, and the table is reached too (#474).
                    assertReachable(companion, content,
                            "Show Jupiter and its moons at the instant set in Place and Time",
                            "Jupiter and moons on the chart", "Centre the chart on Jupiter");
                    reveal((JComponent) named(content, "jupiterCompute"));
                    ReaderInput.click((JComponent) named(content, "jupiterCompute"));
                    drain();
                    assertNotNull(find(content, "jupiterCard"), "Jupiter's card, computed");
                    assertEquals(4, ((javax.swing.JTable) named(content, "jupiterTable"))
                            .getRowCount(), "and the four moons");
                    assertReachable(companion, content, "The four Galilean moons");

                    // The strip is reached by the Controller's own traversal,
                    // and walked by the keyboard for real (#507): Ctrl-PageUp
                    // to Chart, Ctrl-PageDown back - and what was computed is
                    // intact on return, the very cards, nothing computed anew.
                    assertReachable(companion, (Container) companion.getContentPane(),
                            "Controller tabs");
                    Component sunCard = find(content, "sunCard");
                    Component jupiterCard = find(content, "jupiterCard");
                    ReaderInput.shortcutOn(tabs, java.awt.event.KeyEvent.VK_PAGE_UP,
                            java.awt.event.InputEvent.CTRL_DOWN_MASK);
                    drain();
                    assertEquals(juranometria.ui.companion.CompanionTabs.CHART, tabs.chosen(),
                            "Ctrl-PageUp chose Chart");
                    assertFalse(content.isShowing());
                    assertTrue(sections.get(0).content().isShowing(),
                            "the chart's controls are shown instead");
                    ReaderInput.shortcutOn(tabs, java.awt.event.KeyEvent.VK_PAGE_DOWN,
                            java.awt.event.InputEvent.CTRL_DOWN_MASK);
                    drain();
                    assertEquals("solarsystem", tabs.chosen(), "Ctrl-PageDown chose it back");
                    assertTrue(sunCard == find(content, "sunCard")
                            && jupiterCard == find(content, "jupiterCard"),
                            "the Sun's and Jupiter's cards are the ones computed before");
                    assertEquals(4, ((javax.swing.JTable) named(content, "jupiterTable"))
                            .getRowCount(), "and the moons are still there");
                    assertEquals(java.util.Optional.of("solarsystem"),
                            juranometria.ui.companion.CompanionStore.forNode(node).tab(),
                            "the tab chosen is remembered");

                    // Hidden and shown again: the same strip, no tab added.
                    SwingUtilities.invokeAndWait(companion::hideCompanion);
                    drain();
                    SwingUtilities.invokeAndWait(() -> companion.showCompanion(true));
                    drain();
                    assertTrue(tabs == companion.tabs() && tabs.getTabCount() == 2,
                            "one strip, two tabs, however often it is shown");
                    assertEquals("solarsystem", tabs.chosen(), "still on Solar System");
                })));
    }

    private static void assertReachable(Window window, Container host,
                                        String... names) throws Exception {
        List<String> reached = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            java.awt.FocusTraversalPolicy policy = window.getFocusTraversalPolicy();
            Component first = policy.getFirstComponent(window);
            Component at = first;
            for (int i = 0; i < 600 && at != null; i++) {
                if (at instanceof JComponent j && j.getAccessibleContext() != null
                        && SwingUtilities.isDescendingFrom(at, host)) {
                    reached.add(j.getAccessibleContext().getAccessibleName());
                }
                at = policy.getComponentAfter(window, at);
                if (at == first) {
                    break;
                }
            }
        });
        for (String name : names) {
            assertTrue(reached.contains(name), name + " is reached by the Controller's"
                    + " own focus traversal; reached: " + reached);
        }
    }

    private interface Session {
        void run(JFrame frame) throws Exception;
    }

    private static void run(java.util.prefs.Preferences node, Session session)
            throws Exception {
        SkyLanguageStore language = SkyLanguageStore.forNode(node);
        language.save(language.choice(Atlas.languages()).withInterface("en"));
        StartupStores stores = new StartupStores(AppearanceStore.forNode(node),
                ChartOptionsStore.forNode(node), language,
                juranometria.ui.placeandtime.PlaceStore.forNode(node),
                juranometria.ui.ecliptic.EclipticStore.forNode(node),
                SunChartStore.forNode(node),
                juranometria.ui.solar.MercuryChartStore.forNode(node),
                juranometria.ui.solar.VenusChartStore.forNode(node),
                juranometria.ui.solar.MoonChartStore.forNode(node),
                juranometria.ui.solar.JovianChartStore.forNode(node),
                ZoomLockStore.forNode(node),
                juranometria.ui.companion.CompanionStore.forNode(node),
                ChartChromeStore.forNode(node),
                ChartWindowStore.forNode(node));
        JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(() ->
                    frame[0] = JUranometriaMain.start(false, stores));
            drain();
            session.run(frame[0]);
        } finally {
            StartupJourneyTest.closeEverything(frame[0]);
        }
    }

    /** Scrolls the control into the Controller's view, as a reader would. */
    private static void reveal(JComponent control) throws Exception {
        SwingUtilities.invokeAndWait(() -> control.scrollRectToVisible(
                new java.awt.Rectangle(control.getSize())));
        drain();
    }

    private static void drain() throws Exception {
        for (int i = 0; i < 3; i++) {
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    private static CompanionWindow companionOf(JFrame frame) {
        for (Window owned : frame.getOwnedWindows()) {
            if (owned instanceof CompanionWindow companion) {
                return companion;
            }
        }
        throw new AssertionError("the application owns no companion");
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

    private static juranometria.ui.ChartComponent chartOf(JFrame frame) {
        return (juranometria.ui.ChartComponent) all(frame.getContentPane()).stream()
                .filter(c -> c instanceof juranometria.ui.ChartComponent)
                .findFirst().orElseThrow();
    }

    /**
     * Takes the chart away the way a reader does: the first hunt starts
     * from wherever the application opened (unrelated sky); before the
     * others the chart is dragged across most of its width, which pans
     * it through its controller.
     */
    private static void awayFromTheMoon(juranometria.ui.ChartComponent chart, int hunt)
            throws Exception {
        if (hunt == 0) {
            return;
        }
        juranometria.chart.ChartViewState before = chart.viewState();
        int w = chart.getWidth();
        int h = chart.getHeight();
        ReaderInput.drag(chart, w / 10, h / 2, w * 9 / 10, h / 3);
        drain();
        assertTrue(!chart.viewState().centre().equals(before.centre()),
                "the drag moved the chart away (hunt " + hunt + ")");
    }

    /** The chart's centre is the card's right ascension and declination, at 1°. */
    private static void assertCentredOnTheCard(juranometria.ui.ChartComponent chart,
                                               Container content, String card) throws Exception {
        juranometria.chart.ChartViewState view = chart.viewState();
        assertEquals(1.0, view.fieldWidthDegrees(), "the normal minimum field");
        List<String> spoken = new ArrayList<>();
        for (Component c : all((Container) named(content, card))) {
            if (c instanceof javax.swing.JLabel label && label.getAccessibleContext() != null
                    && label.getAccessibleContext().getAccessibleName() != null) {
                spoken.add(label.getAccessibleContext().getAccessibleName());
            }
        }
        String ra = juranometria.ui.solar.SunTableFormat.hms(view.centre());
        String dec = juranometria.ui.solar.SunTableFormat.dms(view.centre());
        assertTrue(spoken.stream().anyMatch(s -> s.startsWith("Right ascension") && s.endsWith(ra)),
                "the chart's centre is the card's right ascension " + ra + ": " + spoken);
        assertTrue(spoken.stream().anyMatch(s -> s.startsWith("Declination") && s.endsWith(dec)),
                "and its declination " + dec + ": " + spoken);
    }

    private static Component find(Container from, String name) {
        return all(from).stream().filter(c -> name.equals(c.getName()))
                .findFirst().orElse(null);
    }

    private static Component named(Container from, String name) {
        Component found = find(from, name);
        assertNotNull(found, "no component named " + name);
        return found;
    }
}
