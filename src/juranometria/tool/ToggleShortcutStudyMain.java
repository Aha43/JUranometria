package juranometria.tool;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import juranometria.app.AppMenuBar;
import juranometria.app.ChartOptionsDialog;
import juranometria.render.ChartOptions;
import juranometria.render.SymbolFamily;

/**
 * What the atlas already binds, what it could bind, and what it
 * would cost (Sprint 31, issue #312's gate).
 *
 * <p>The issue asks for a keyboard route to every chart-content
 * toggle and forbids assigning a key merely because a letter is
 * unused. So the first question is not which key: it is how many
 * things there are to reach, what the application's keyboard already
 * says, and what a reader would have to remember.
 *
 * <p>Every number here is read from the production surfaces rather
 * than typed: the toggles from the chart-options dialog the reader
 * opens, the bindings from the menu bar and root pane the window
 * installs, and the editing conventions from a text field under the
 * application's own look and feel. A conflict audit written from
 * memory is a list of the conflicts somebody remembered.
 */
public final class ToggleShortcutStudyMain {

    private ToggleShortcutStudyMain() {
    }

    /** One thing a reader can switch, as its own control describes it. */
    private record Toggle(String label, char mnemonic, String tooltip,
                          String depends) {
    }

    /** One keystroke the application already answers. */
    private record Bound(String stroke, String what, String where) {
    }

    /**
     * Two documents, because there are two kinds of evidence here.
     *
     * <p>What the switches are, which letter reaches each, which
     * master each waits for and what a scheme would cost is the same
     * on every machine, and is printed to standard out where the
     * evidence contract holds it to its committed bytes.
     *
     * <p>What this desktop <em>calls</em> a modifier, which strokes
     * its look and feel binds into a text field, and how many of them
     * there are is one machine's answer. Held to the same bytes
     * across machines it is not evidence but a claim the project
     * explicitly does not make, so it is written beside the report
     * with the machine that produced it named (#315).
     */
    public static void main(String[] args) throws Exception {
        juranometria.app.UiTheme.apply(false);
        StringBuilder out = new StringBuilder();
        List<Toggle> toggles = toggles();
        List<Bound> bound = bindings();
        Set<String> editing = editingStrokes();

        preface(out);
        inventory(out, toggles);
        audit(out, bound, editing);
        letters(out, toggles, bound, editing);
        schemes(out, toggles, editing);
        built(out);
        System.out.print(PlatformEvidence.portable(out.toString()));

        StringBuilder platform = new StringBuilder();
        PlatformEvidence.preface(platform,
                "The chart keyboard, as this desktop spells it",
                "Sprint 31, issues #312 and #315.");
        strokes(platform, bound, editing);
        PlatformEvidence.write(platform,
                "docs/studies/toggle-shortcuts/platform.md");
    }

    /** The strokes and their spelling: one machine's answer. */
    private static void strokes(StringBuilder out, List<Bound> bound,
                                Set<String> editing) {
        out.append("## What this desktop already answers\n\n");
        out.append("| keystroke | what it does | where it is bound"
                + " |\n");
        out.append("|---|---|---|\n");
        for (Bound one : bound) {
            out.append(String.format(Locale.ROOT, "| `%s` | %s | %s |%n",
                    one.stroke(), one.what(), one.where()));
        }
        out.append(String.format(Locale.ROOT,
                "| **%d strokes** | | |%n", bound.size()));
        out.append(String.format(Locale.ROOT,
                "%nAnd a text field, under this look and feel on this"
                        + " platform, answers%n**%d keystrokes** of its"
                        + " own. That number is not the same"
                        + " everywhere - a%nMac's text field carries"
                        + " emacs-style bindings a Linux one does"
                        + " not - which is%nwhy the report beside this"
                        + " one counts what a scheme would collide"
                        + " with%nrather than quoting this"
                        + " figure.%n%n", editing.size()));
        out.append("The prefix, in this desktop's own words: **")
                .append(juranometria.app.ChartKeys.prefixText())
                .append("**.\n");
    }

    private static void preface(StringBuilder out) {
        out.append("# A keyboard route to what the chart shows\n\n");
        out.append("Measurements for issue #312, the Sprint 31"
                + " interaction gate. Every table below\nis read from"
                + " the production surfaces as they are built: the"
                + " toggles from the\nchart-options dialog a reader"
                + " opens, the bindings from the menu bar and root"
                + " pane\nthe window installs, and the editing"
                + " conventions from a text field under the\n"
                + "application's own look and feel.\n\n");
        out.append("The question is not which letters are free. It is"
                + " how many things there are to\nreach, what the"
                + " keyboard already means, and what a reader would"
                + " have to carry\nin their head.\n\n");
    }

    /** Every chart-content toggle, read off the dialog that owns it. */
    private static void inventory(StringBuilder out, List<Toggle> toggles) {
        out.append("## What there is to switch\n\n");
        out.append("| control | its own key in the dialog | depends on"
                + " | what it says |\n");
        out.append("|---|---|---|---|\n");
        for (Toggle toggle : toggles) {
            out.append(String.format(Locale.ROOT, "| %s | `%s` | %s |"
                            + " %s |%n", toggle.label(),
                    Character.toUpperCase(toggle.mnemonic()),
                    toggle.depends(), shorten(toggle.tooltip())));
        }
        out.append(String.format(Locale.ROOT,
                "| **%d controls** | | | |%n", toggles.size()));
        out.append("\nEvery one of them is reachable today by opening"
                + " Chart Options and pressing the\ncontrol's own"
                + " mnemonic - a route that needs the dialog open,"
                + " which is the thing\nthis issue is asked to fix."
                + " The module toggles are elsewhere: the ecliptic on"
                + " the\nView menu, the observer's lines in Place and"
                + " Time.\n\n");
    }

    /** Every keystroke the application already answers, and where. */
    /**
     * What the keyboard already means, in terms no desktop decides.
     *
     * <p>How many strokes are bound and what each does is the atlas's
     * own fact. What those strokes are <em>called</em> is the
     * desktop's, and lives in `platform.md` beside this - as does the
     * number of editing strokes a text field answers, which is not
     * the same on two platforms.
     */
    private static void audit(StringBuilder out, List<Bound> bound,
                              Set<String> editing) {
        out.append("## What the keyboard already means\n\n");
        out.append("| what it does | where it is bound |\n");
        out.append("|---|---|\n");
        for (Bound one : bound) {
            out.append(String.format(Locale.ROOT, "| %s | %s |%n",
                    one.what(), one.where()));
        }
        out.append(String.format(Locale.ROOT,
                "| **%d strokes** | |%n", bound.size()));
        out.append("\nEvery one of them carries the platform's own"
                + " menu modifier, and a text field\nunder this look"
                + " and feel answers dozens of editing strokes of its"
                + " own - every\none of them a thing a reader typing a"
                + " star's name expects to keep. A scheme\nthat binds"
                + " bare letters takes them away, which is why none of"
                + " the candidates\nbelow does. The spellings and the"
                + " count are one machine's answer, recorded"
                + " in\n`platform.md`.\n\n");
    }

    /** How much room a direct-accelerator scheme would actually have. */
    private static void letters(StringBuilder out, List<Toggle> toggles,
                                List<Bound> bound, Set<String> editing) {
        Set<Character> wanted = new LinkedHashSet<>();
        for (Toggle toggle : toggles) {
            wanted.add(Character.toUpperCase(toggle.mnemonic()));
        }
        Set<String> taken = new LinkedHashSet<>();
        for (Bound one : bound) {
            taken.add(one.stroke());
        }
        List<Character> clash = new ArrayList<>();
        for (char letter : wanted) {
            String withMenuKey = platformName(KeyStroke.getKeyStroke(
                    letter, AppMenuBar.menuShortcutMask()));
            if (taken.contains(withMenuKey)) {
                clash.add(letter);
            }
        }
        // The letters are not free, and not even distinct: the
        // dialog's mnemonics are unique per tab, which is all a
        // dialog needs and less than a keyboard route needs.
        Map<Character, List<String>> byLetter = new LinkedHashMap<>();
        for (Toggle toggle : toggles) {
            byLetter.computeIfAbsent(
                            Character.toUpperCase(toggle.mnemonic()),
                            key -> new ArrayList<>())
                    .add(toggle.label());
        }
        List<String> shared = new ArrayList<>();
        for (var entry : byLetter.entrySet()) {
            if (entry.getValue().size() > 1) {
                shared.add(entry.getKey() + " is "
                        + String.join(" and ", entry.getValue()));
            }
        }
        out.append("## The letters are not a map\n\n");
        out.append("A dialog's mnemonics only have to be unique on"
                + " their own tab, and these are not\nunique across"
                + " the dialog: **" + shared.size() + " letters"
                + " already mean two things**"
                + (shared.isEmpty() ? "" : " - " + String.join("; ",
                        shared)) + ".\n\nSo a scheme cannot simply"
                + " promote \"the letter already on the control\" to a"
                + " global\nkeystroke: two of them would collide on the"
                + " way out of the dialog. Any scheme has\nto assign"
                + " its own letters, and say where they came from."
                + "\n\n");
        out.append("## What sixteen accelerators would cost\n\n");
        out.append("The controls want " + wanted.size() + " distinct"
                + " letters for " + toggles.size() + " controls."
                + " Bound with the platform's own menu key,"
                + "\n**" + clash.size() + "** of them collide with"
                + " something the application already answers"
                + (clash.isEmpty() ? "" : " - "
                        + String.join(", ", clash.stream()
                                .map(String::valueOf).toList()))
                + ".\n\n");
        out.append("That is the smaller half of the objection. The"
                + " larger half is that sixteen new\nglobal strokes"
                + " are sixteen things a reader must know before any"
                + " of them helps,\nand the atlas has four in total"
                + " today.\n\n");
    }

    /** The three candidates, against what the gate has to settle. */
    private static void schemes(StringBuilder out, List<Toggle> toggles,
                                Set<String> editing) {
        out.append("## The candidates\n\n");
        out.append("| | new global strokes | to reach one toggle |"
                + " what a reader must remember | shows current state"
                + " |\n");
        out.append("|---|---:|---|---|---|\n");
        out.append(String.format(Locale.ROOT,
                "| direct accelerators for all | %d | one stroke |"
                        + " %d letters | no |%n",
                toggles.size(), toggles.size()));
        out.append("| a prefix, then the control's own letter | 1 |"
                + " two strokes | one prefix, then the letter already"
                + " on the control | only if the prefix says so |\n");
        out.append("| a palette listing every state | 1 | a stroke,"
                + " then a letter or a click | one stroke | yes, all"
                + " of it |\n");
        out.append("| direct for the few, prefix for the rest | 3-4 |"
                + " one or two strokes | the few, and the prefix |"
                + " partly |\n");
        out.append("\nThe rows are not exclusive: a prefix that shows"
                + " what it is waiting for **is** a\npalette, and a"
                + " palette that accepts the control's own letter"
                + " **is** a prefix. The\ndecision (docs/decisions/"
                + "chart-toggle-shortcuts.md) proposes exactly that"
                + " pair,\nand what follows is what such a scheme has"
                + " to settle before a key is wired.\n\n");
        out.append("| question | why it is not obvious |\n");
        out.append("|---|---|\n");
        out.append("| cancelling | a reader who opens the prefix and"
                + " changes their mind must be able to leave without"
                + " switching anything |\n");
        out.append("| waiting | a prefix that waits for ever is a"
                + " keyboard that has stopped answering; one that"
                + " times out surprises a slow reader |\n");
        out.append("| a field with the caret in it | the prefix may"
                + " not fire while a reader is typing a star's name,"
                + " and the "
                + editing.size() + " editing strokes above say what"
                + " else it may not take |\n");
        out.append("| the platform's own modifier | the same scheme"
                + " has to read as Command here and Ctrl elsewhere,"
                + " spelled from one place |\n");
        out.append("| what is remembered | the GUI control persists"
                + " the reader's choice, so the keyboard has to"
                + " persist exactly the same thing |\n");
        out.append("| a master and its dependants | switching deep-sky"
                + " objects off hides the labels; the keyboard must"
                + " mean what the checkbox means, including what it"
                + " stores |\n");
        out.append("| the searched target | its label survives its"
                + " family being switched off, and no shortcut may"
                + " quietly change that |\n");
        out.append("| the modules | the ecliptic and the observer's"
                + " lines are switched elsewhere and belong in the"
                + " same map, or are refused in it by name |\n");
        out.append("| what a screen reader hears | a state that"
                + " changed without a visible dialog has to be"
                + " announced, or the route is only for people who"
                + " can see the chart |\n");
        out.append("| what the tooltips say | every surface that"
                + " explains a control must show the same sequence,"
                + " from the same source |\n");
        out.append("\n");
    }

    /** The map as it was built, read from the registry that owns it. */
    private static void built(StringBuilder out) {
        out.append("## What was built\n\n");
        out.append("Read from `ChartKeys`, which is the one place the"
                + " keystrokes live: the palette,\nthe tooltips and"
                + " the tests all ask it, so this table cannot drift"
                + " from the\napplication without the study changing"
                + " with it.\n\n");
        out.append("The chart's keyboard opens with the platform's own"
                + " menu key and `K`: **⌘K** on\nmacOS and **Ctrl+K**"
                + " elsewhere, spelled by the registry rather than"
                + " typed anywhere.\nThis document is generated"
                + " headlessly, where the toolkit reports no menu mask"
                + " at\nall, so what it prints for itself is the"
                + " fallback: **"
                + juranometria.app.ChartKeys.prefixText() + "**.\n\n");
        out.append("| switch | key | remembered | needs | why this"
                + " letter |\n");
        out.append("|---|---|---|---|---|\n");
        for (var toggle : juranometria.app.ChartKeys.toggles()) {
            var master = toggle.dependsOn() == null ? null
                    : juranometria.app.ChartKeys.toggle(toggle.dependsOn());
            out.append(String.format(Locale.ROOT,
                    "| %s | `%s` | %s | %s | %s |%n", toggle.label(),
                    String.valueOf(toggle.key())
                            .toUpperCase(Locale.ROOT),
                    toggle.persistent() ? "between sessions"
                            : "for this session",
                    master == null ? "—" : master.label(),
                    toggle.note() == null ? "the control's own"
                            : toggle.note()));
        }
        out.append("\n");
        for (var refused : juranometria.app.ChartKeys.refused()
                .entrySet()) {
            out.append("**" + refused.getKey() + "** is refused: "
                    + refused.getValue() + ".\n\n");
        }
        out.append("Three of the twenty letters differ from the"
                + " control's own mnemonic, for the reason\nthe table"
                + " gives - and the palette gives the same reason"
                + " beside the same letter,\nfrom the same field, so a"
                + " reader is never left wondering why `F` did"
                + " something\nother than what the dialog told"
                + " them.\n\n");
        out.append("The `remembered` column is the promise the atlas"
                + " already makes, not a new one:\nthe chart's own"
                + " layers and the ecliptic are stored, and the"
                + " observer's lines are\nnot - Place and Time keeps a"
                + " place, not a picture. The palette says which every"
                + "\ntime it switches one.\n\n");
    }

    // ---- read from the production surfaces --------------------------

    /** The toggles, from the dialog a reader opens to find them. */
    private static List<Toggle> toggles() throws Exception {
        List<Toggle> found = new ArrayList<>();
        // The surface a reader opens, built from a controller with
        // the released chart in it: the labels, the keys and the
        // sentences are the dialog's own, never a second list.
        JComponent content = ChartOptionsDialog.contentForStudy(
                new juranometria.app.ChartOptionsController(
                        new juranometria.app.ChartOptionsStore() {
                            private ChartOptions kept =
                                    ChartOptions.DEFAULTS;

                            @Override
                            public ChartOptions load() {
                                return kept;
                            }

                            @Override
                            public void save(ChartOptions options) {
                                kept = options;
                            }
                        }));
        collect(content, found);
        return found;
    }

    private static void collect(Container root, List<Toggle> found) {
        for (Component child : root.getComponents()) {
            if (child instanceof JCheckBox box) {
                found.add(new Toggle(box.getText(),
                        (char) box.getMnemonic(), box.getToolTipText(),
                        dependency(box.getText())));
            }
            if (child instanceof Container inside) {
                collect(inside, found);
            }
        }
    }

    /** What a control needs switched on before it can show anything. */
    private static String dependency(String label) {
        if (label.equals("Deep-sky labels")) {
            return "Deep-sky objects";
        }
        if (label.equals("Constellation names")) {
            return "Constellation figures";
        }
        for (SymbolFamily family : SymbolFamily.values()) {
            if (family.label().equals(label)) {
                return "Deep-sky objects";
            }
        }
        return "—";
    }

    /** Every stroke the window binds, from the window's own parts. */
    private static List<Bound> bindings() throws Exception {
        List<Bound> found = new ArrayList<>();
        JMenuBar bar = AppMenuBar.create(
                new juranometria.ui.ChartViewController(),
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, () -> { });
        for (int menu = 0; menu < bar.getMenuCount(); menu++) {
            JMenu each = bar.getMenu(menu);
            for (int at = 0; at < each.getItemCount(); at++) {
                JMenuItem item = each.getItem(at);
                if (item != null && item.getAccelerator() != null) {
                    found.add(new Bound(platformName(item.getAccelerator()),
                            item.getText(), "the " + each.getText()
                                    + " menu"));
                }
            }
        }
        JRootPane root = new JRootPane();
        AppMenuBar.installZoomShortcuts(root,
                new juranometria.ui.ChartViewController());
        var inputs = root.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (inputs.allKeys() != null) {
            for (KeyStroke stroke : inputs.allKeys()) {
                found.add(new Bound(platformName(stroke),
                        String.valueOf(inputs.get(stroke)),
                        "the window's own keys"));
            }
        }
        found.sort((one, other) -> one.stroke().compareTo(other.stroke()));
        return found;
    }

    /** What a text field answers, so that a scheme cannot take it. */
    private static Set<String> editingStrokes() {
        Set<String> strokes = new LinkedHashSet<>();
        JTextField field = new JTextField();
        for (int condition : new int[] {JComponent.WHEN_FOCUSED,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT}) {
            var map = field.getInputMap(condition);
            while (map != null) {
                if (map.keys() != null) {
                    for (KeyStroke stroke : map.keys()) {
                        strokes.add(platformName(stroke));
                    }
                }
                map = map.getParent();
            }
        }
        return strokes;
    }

    /** A keystroke as a reader would say it on this platform. */
    private static String platformName(KeyStroke stroke) {
        String modifiers = java.awt.event.InputEvent.getModifiersExText(
                stroke.getModifiers());
        String key = java.awt.event.KeyEvent.getKeyText(
                stroke.getKeyCode());
        return modifiers.isEmpty() ? key : modifiers + "+" + key;
    }

    private static String shorten(String text) {
        if (text == null) {
            return "—";
        }
        String plain = text.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ").trim();
        return plain.length() > 60 ? plain.substring(0, 57) + "..." : plain;
    }

}
