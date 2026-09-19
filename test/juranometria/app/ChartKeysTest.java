package juranometria.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

import juranometria.render.ChartOptions;
import juranometria.render.SymbolFamily;
import juranometria.ui.ChartViewController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The map, and what it may not collide with (Sprint 31, issue #312).
 *
 * <p>The audit is in two scopes, because the scheme is: <em>one</em>
 * stroke opens the chart's keyboard and must be free of everything
 * the application already answers, and the letters after it live
 * inside a visible palette that owns the keyboard while it is there.
 * A letter that clashes with a menu accelerator is not a conflict -
 * the palette is holding the keyboard - but the prefix that clashes
 * with anything is.
 */
class ChartKeysTest {

    @Test
    void everyLetterNamesOneSwitchAndEverySwitchOneLetter() {
        Map<Character, String> byLetter = new LinkedHashMap<>();
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            char letter = Character.toUpperCase(toggle.key());
            String taken = byLetter.put(letter, toggle.id());
            assertEquals(null, taken, letter + " reaches one switch,"
                    + " not " + taken + " and " + toggle.id());
        }
        assertEquals(ChartKeys.toggles().size(), byLetter.size(),
                "and every switch has a letter of its own");
    }

    @Test
    void thePrefixIsFreeOfEverythingTheApplicationAlreadyAnswers() {
        // The scope that has to be globally clean. Every accelerator
        // the menus carry, every stroke the window binds, and every
        // editing stroke a text field answers under the look and feel
        // the application ships.
        Set<KeyStroke> taken = new LinkedHashSet<>();
        JMenuBar bar = AppMenuBar.create(new ChartViewController(),
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, () -> { }, juranometria.ui.language.InterfaceText.forLanguage("en"));
        for (int menu = 0; menu < bar.getMenuCount(); menu++) {
            JMenu each = bar.getMenu(menu);
            for (int at = 0; at < each.getItemCount(); at++) {
                JMenuItem item = each.getItem(at);
                if (item != null && item.getAccelerator() != null) {
                    taken.add(item.getAccelerator());
                }
            }
        }
        JRootPane root = new JRootPane();
        AppMenuBar.installZoomShortcuts(root, new ChartViewController());
        var window = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (window.allKeys() != null) {
            taken.addAll(List.of(window.allKeys()));
        }
        for (KeyStroke stroke : editingStrokes()) {
            taken.add(stroke);
        }
        assertFalse(taken.contains(ChartKeys.prefix()),
                "the chart keyboard's own key is free: "
                        + ChartKeys.prefix());
        assertFalse(taken.isEmpty(),
                "and the audit really looked at something: "
                        + taken.size() + " strokes");
    }

    @Test
    void theLettersNeedOnlyBeFreeInsideThePalette() {
        // Stated rather than assumed. The letters are bare - D, G, O -
        // and bare letters are exactly what a text field wants. They
        // are bound on the palette, which is on the screen and owns
        // the keyboard while it is; nothing binds them globally, and
        // this is the test that would fail if something did.
        JRootPane root = new JRootPane();
        ChartKeyboard.install(root, switches(ChartOptions.DEFAULTS), juranometria.ui.language.InterfaceText.forLanguage("en"),
                keyboard -> { });
        var window = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        List<String> global = new ArrayList<>();
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            KeyStroke bare = KeyStroke.getKeyStroke(
                    Character.toUpperCase(toggle.key()), 0);
            if (window.get(bare) != null) {
                global.add(String.valueOf(toggle.key()));
            }
        }
        assertEquals(List.of(), global,
                "no letter of the map is bound to the window itself");
        assertNotNull(window.get(ChartKeys.prefix()),
                "only the prefix is, which is the whole scheme");
    }

    @Test
    void theSpellingOfEveryStrokeComesFromOnePlace() {
        // A tooltip that says "Cmd-K" while the binding is Ctrl-K is
        // a lie a reader finds out by pressing. Both come from here,
        // and this holds them to the platform's own mask.
        String prefix = ChartKeys.prefixText();
        assertTrue(prefix.endsWith("K"),
                "the prefix is spelled with its own key: " + prefix);
        // Compared as keystrokes rather than as modifier bits: Swing
        // keeps the legacy mask alongside the extended one, so the
        // numbers differ while the stroke a reader presses does not.
        assertEquals(KeyStroke.getKeyStroke(
                        java.awt.event.KeyEvent.VK_K,
                        AppMenuBar.menuShortcutMask()),
                ChartKeys.prefix(),
                "and with the platform's own menu modifier");
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            assertTrue(juranometria.ui.language.ChartKeyboardText.in(juranometria.ui.language.InterfaceText.forLanguage("en")).sequence(toggle).startsWith(prefix),
                    toggle.id() + " is spelled from the prefix: "
                            + juranometria.ui.language.ChartKeyboardText.in(juranometria.ui.language.InterfaceText.forLanguage("en")).sequence(toggle));
            assertTrue(juranometria.ui.language.ChartKeyboardText.in(juranometria.ui.language.InterfaceText.forLanguage("en")).sequence(toggle).endsWith(
                            String.valueOf(toggle.key())
                                    .toUpperCase(Locale.ROOT)),
                    "and its own letter: " + juranometria.ui.language.ChartKeyboardText.in(juranometria.ui.language.InterfaceText.forLanguage("en")).sequence(toggle));
        }
    }

    @Test
    void everySwitchTheChartHasIsOnTheMapOrRefusedByName() {
        // The claim "every layer the chart shows" is only worth
        // making if something checks it. The chart's own options are
        // the list; anything not on the map has to be refused with a
        // reason a reader can read.
        Set<String> mapped = new LinkedHashSet<>();
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            mapped.add(toggle.id());
        }
        // By identity. This listed the chart's layers by their
        // English names, which stopped being a thing the registry
        // knows when the prose moved out of it (#350) - and a
        // coverage claim that can be satisfied by renaming a label
        // was never checking coverage anyway.
        List<String> missing = new ArrayList<>();
        for (String layer : List.of("chart.deepSkyObjects",
                "chart.deepSkyLabels", "chart.constellationFigures",
                "chart.constellationBoundaries",
                "chart.constellationNames", "chart.starNames",
                "chart.bayerLetters", "chart.flamsteedNumbers",
                "chart.equatorialGrid", "chart.titleBlock",
                "chart.magnitudeKey", "chart.blackSky",
                "chart.galaxies", "chart.openClusters",
                "chart.globularClusters", "chart.nebulae",
                "chart.planetaryNebulae")) {
            if (!mapped.contains(layer)) {
                missing.add(layer);
            }
        }
        assertEquals(SymbolFamily.values().length, 5,
                "the premise: all five families are listed above -"
                        + " a family added to the chart must be added"
                        + " to this list or refused");
        assertEquals(List.of(), missing,
                "every layer of the chart is on the map");
        assertTrue(mapped.contains("module.ecliptic")
                        && mapped.contains("module.meridian")
                        && mapped.contains("module.horizon"),
                "and the modules a reader switches from the chart");
        // By identity. The refusal was keyed by the English word
        // "Zenith", which was also the Swing component name, so
        // translating a resource would have renamed a component
        // (#350).
        assertEquals(List.of("zenith"), ChartKeys.refused().stream()
                        .map(ChartKeys.Refusal::id).toList(),
                "and what is not switchable is refused by identity");
    }

    private static Set<KeyStroke> editingStrokes() {
        Set<KeyStroke> strokes = new LinkedHashSet<>();
        JTextField field = new JTextField();
        for (int condition : new int[] {JComponent.WHEN_FOCUSED,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT}) {
            var map = field.getInputMap(condition);
            while (map != null) {
                if (map.keys() != null) {
                    strokes.addAll(List.of(map.keys()));
                }
                map = map.getParent();
            }
        }
        return strokes;
    }

    /** The switches over a chart nobody is looking at. */
    static ChartSwitches switches(ChartOptions from) {
        ChartOptions[] held = {from};
        boolean[] modules = {false, false};
        return ChartSwitches.of(
                new ChartOptionsController(new ChartOptionsStore() {
                    @Override
                    public ChartOptions load() {
                        return held[0];
                    }

                    @Override
                    public void save(ChartOptions options) {
                        held[0] = options;
                    }
                }),
                new ChartSwitches.Ecliptic() {
                    @Override
                    public boolean showing() {
                        return modules[0];
                    }

                    @Override
                    public void toggle() {
                        modules[0] = !modules[0];
                    }
                },
                new ChartSwitches.ObserverLines() {
                    @Override
                    public boolean meridianShowing() {
                        return modules[1];
                    }

                    @Override
                    public boolean horizonShowing() {
                        return false;
                    }

                    @Override
                    public void showing(boolean meridian, boolean horizon) {
                        modules[1] = meridian;
                    }
                });
    }
}
