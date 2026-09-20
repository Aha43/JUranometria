package juranometria.ui.language;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.SwingSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The toolkit's own words, in the reader's language
 * (Sprint 33, issue #350).
 *
 * <p>Two kinds of claim, and the first is not enough on its own. A
 * defaults table can be asserted without a screen and proves the
 * installer writes what it claims; only a <strong>real option pane
 * and a real file chooser, built and walked</strong>, prove that a
 * reader is shown it. The first version of this surface's inventory
 * probed keys and would have missed the six pairs where a chooser
 * button says one thing to a pointer and another to a screen reader.
 *
 * <p><strong>The hostile locale is the point.</strong> Swing resolves
 * these against {@code Locale.getDefault()}, so a test run on an
 * English machine cannot tell "the atlas installed Norwegian" from
 * "the platform happened to agree". Every live walk here runs with
 * the default locale set to German, which the JDK <em>does</em>
 * translate - so anything the atlas failed to install comes back as
 * {@code Ja}, {@code Nein}, {@code Abbrechen}, and is impossible to
 * mistake for a pass.
 */
class SwingTextTest {

    private static final InterfaceText NORSK =
            InterfaceText.forLanguage("nb-NO");
    private static final InterfaceText ENGLISH =
            InterfaceText.forLanguage("en");

    // ---- scoped: no global state touched at all -------------------

    @Test
    void theInstallerWritesEveryKeyItClaimsAndNothingElse() {
        UIDefaults table = new UIDefaults();
        SwingText.in(NORSK).installInto(table);

        assertEquals(41, SwingText.toolkitKeys().size(),
                "the inventory is forty-one keys - seven option-pane"
                        + " and thirty-four chooser - and a change to"
                        + " it is a decision, not a drift");
        assertEquals(SwingText.toolkitKeys().size(), table.size(),
                "the installer writes exactly what it claims: "
                        + table.size() + " entries for "
                        + SwingText.toolkitKeys().size() + " keys");
        for (String key : SwingText.toolkitKeys()) {
            Object value = table.get(key);
            assertTrue(value instanceof String text && !text.isBlank(),
                    key + " is answered");
            assertTrue(!String.valueOf(value)
                            .equals(SwingText.packKeyFor(key)),
                    key + " is answered with a word, not a resource"
                            + " key - these replace the toolkit's own,"
                            + " so a gap does not fall back to English");
        }
    }

    /** Both languages install, because mixed chrome cuts both ways. */
    @Test
    void englishIsInstalledToo() {
        UIDefaults table = new UIDefaults();
        SwingText.in(ENGLISH).installInto(table);
        assertEquals("Yes", table.get("OptionPane.yesButtonText"));
        assertEquals("Save", table.get("FileChooser.saveButtonText"));
        assertEquals("Up One Level",
                table.get("FileChooser.upFolderToolTipText"));

        UIDefaults norwegian = new UIDefaults();
        SwingText.in(NORSK).installInto(norwegian);
        assertEquals("Ja", norwegian.get("OptionPane.yesButtonText"));
        assertEquals("Lagre", norwegian.get("FileChooser.saveButtonText"));
        assertEquals("Opp ett nivå",
                norwegian.get("FileChooser.upFolderToolTipText"));

        // An English reader on a German desktop had the same defect
        // in the other direction, and installing English is the fix
        // for it rather than an accident of the platform agreeing.
        int differ = 0;
        for (String key : SwingText.toolkitKeys()) {
            if (!table.get(key).equals(norwegian.get(key))) {
                differ++;
            }
        }
        assertTrue(differ >= 30,
                "the two languages really differ, in " + differ
                        + " of " + SwingText.toolkitKeys().size()
                        + " keys");
    }

    /** The six pairs: a pointer and a screen reader are told different things. */
    @Test
    void theHoverAndSpokenHalvesAreBothClaimedAndBothDiffer() {
        SwingText said = SwingText.in(NORSK);
        String[][] pairs = {
            {"FileChooser.upFolderToolTipText",
                    "FileChooser.upFolderAccessibleName"},
            {"FileChooser.newFolderToolTipText",
                    "FileChooser.newFolderAccessibleName"},
        };
        for (String[] pair : pairs) {
            assertTrue(!said.say(pair[0]).equals(said.say(pair[1])),
                    pair[0] + " and " + pair[1] + " say different"
                            + " things, as they do in English: \""
                            + said.say(pair[0]) + "\" against \""
                            + said.say(pair[1]) + "\"");
        }
        for (String key : List.of("homeFolder", "listViewButton",
                "detailsViewButton")) {
            assertTrue(said.say("FileChooser." + key + "ToolTipText")
                            != null
                            && said.say("FileChooser." + key
                                    + "AccessibleName") != null,
                    key + " claims both halves");
        }
    }

    /** A key nobody inventoried is refused rather than guessed. */
    @Test
    void aToolkitKeyTheAtlasNeverInventoriedIsRefused() {
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> SwingText.in(NORSK).say("ColorChooser.okText"));
        assertTrue(thrown.getMessage().contains("ColorChooser.okText"),
                "and says which: " + thrown.getMessage());
    }

    /** Access keys stay with the deferred access letters. */
    @Test
    void noAccessKeyIsClaimed() {
        List<String> mnemonics = new ArrayList<>();
        for (String key : SwingText.toolkitKeys()) {
            if (key.toLowerCase(Locale.ROOT).contains("mnemonic")) {
                mnemonics.add(key);
            }
        }
        assertEquals(List.of(), mnemonics,
                "Swing's access keys belong with the eighteen access"
                        + " letters already deferred; installing some"
                        + " of them would make that surface"
                        + " half-owned");
    }

    // ---- live: real components, under a hostile default locale ----

    @Test
    void aRealOptionPaneAnswersInNorwegianOnAGermanMachine()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a dialog has to be built for its words to exist");
        withHostileLocaleAndOwnDefaults(NORSK, () -> {
            for (int option : new int[] {JOptionPane.YES_NO_OPTION,
                    JOptionPane.DEFAULT_OPTION}) {
                List<String> said = wordsOf(new JOptionPane(
                        "Erstatt orion.svg?",
                        JOptionPane.WARNING_MESSAGE, option));
                assertNoGerman(said, "an option pane");
                // "OK" is not in this list for the same reason
                // "Ja" is not in the German one: Norwegian spells it
                // OK too, so it discriminates nothing. Only words
                // the two languages spell differently can be
                // evidence either way.
                assertTrue(said.stream().noneMatch(w ->
                                w.equals("Yes") || w.equals("No")),
                        "and no English either: " + said);
            }
            List<String> confirm = wordsOf(new JOptionPane(
                    "Erstatt orion.svg?", JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION));
            assertTrue(confirm.contains("Ja") && confirm.contains("Nei"),
                    "the reader answers in their own language: "
                            + confirm);
        });
    }

    @Test
    void aRealFileChooserSpeaksNorwegianToPointerAndScreenReaderAlike()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a chooser has to be built for its words to exist");
        withHostileLocaleAndOwnDefaults(NORSK, () -> {
            List<String> said = wordsOf(new JFileChooser());
            assertTrue(said.size() >= 12,
                    "the premise: the chooser said a great many"
                            + " things - " + said.size());
            assertNoGerman(said, "a file chooser");
            for (String english : List.of("Cancel", "Look In:",
                    "File Name:", "Files of Type:", "Up One Level",
                    "Create New Folder", "New Folder", "Details")) {
                assertTrue(!said.contains(english),
                        "\"" + english + "\" is gone from the"
                                + " chooser: " + said);
            }
            assertTrue(said.contains("Avbryt") && said.contains("Se i:")
                            && said.contains("Filnavn:")
                            && said.contains("Opp ett nivå")
                            && said.contains("Ny mappe"),
                    "and its Norwegian is there, visible, hovered and"
                            + " spoken: " + said);
        });
    }

    /**
     * The one installed word the walk could not otherwise reach.
     *
     * <p>{@code FileChooser.other.newFolder} names a folder a reader
     * creates and then renames, so nothing shows it until the action
     * runs and a directory exists on disk.
     *
     * <p>It is also the only one of the forty-one that is not a
     * lookup. The JDK copies it into a {@code static final} field
     * when {@code FileSystemView} initialises - which the first
     * {@code JFileChooser} anywhere in the JVM does - so the value
     * that wins is whichever was in the defaults table at that
     * instant, and a later install is ignored. The question is
     * therefore about order, and a shared test JVM cannot ask it:
     * by the time this method runs, the chooser sheets above have
     * already frozen the JDK's own name.
     *
     * <p>So it is asked in two child processes. One runs the real
     * {@code JUranometriaMain.start} and then creates a folder, which
     * is production's order. The other builds a chooser first, and
     * must <em>not</em> produce the atlas's word - the mutation that
     * shows this test can fail and that the ordering is what saves
     * it. Each gets a scratch directory, removed in {@code finally}.
     */
    @Test
    void theNewFolderTheReaderCreatesIsNamedInTheirLanguage()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a chooser has to be built for its action to exist");

        assertEquals("Ny mappe", newFolderNameFrom("application"),
                "the folder a Norwegian reader creates, in a JVM that"
                        + " started the way a reader starts one - named"
                        + " in words they would type, not \"NewFolder\""
                        + " and not the token \"NyMappe\"");

        String latched = newFolderNameFrom("latched");
        assertNotEquals("Ny mappe", latched,
                "and the mutation: a chooser built before the words"
                        + " are installed freezes the JDK's own name"
                        + " for the whole JVM, which is why the"
                        + " installer runs at startup and nothing"
                        + " before it may touch FileSystemView. It"
                        + " said: " + latched);
    }

    /**
     * Runs the probe in a JVM of its own and reports what it created.
     *
     * @param mode {@code application} for production's order,
     *     {@code latched} for the hazard
     */
    private static String newFolderNameFrom(String mode)
            throws Exception {
        java.nio.file.Path scratch =
                java.nio.file.Files.createTempDirectory(
                        "chooser-new-folder-" + mode);
        try {
            Process probe = new ProcessBuilder(
                    java.nio.file.Path.of(System.getProperty("java.home"),
                            "bin", "java").toString(),
                    "-cp", System.getProperty("java.class.path"),
                    "juranometria.app.NewFolderProbeMain",
                    mode, scratch.toString())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(probe.getInputStream()
                    .readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(probe.waitFor(4, java.util.concurrent.TimeUnit.MINUTES),
                    "the probe finishes");
            assertEquals(0, probe.exitValue(),
                    "the probe started the application and ran the"
                            + " action. It said:\n" + output);
            String marker = "NEW-FOLDER-NAME=";
            int at = output.indexOf(marker);
            assertTrue(at >= 0, "the probe reports a name. It said:\n"
                    + output);
            return output.substring(at + marker.length()).lines()
                    .findFirst().orElseThrow().strip();
        } finally {
            try (var files = java.nio.file.Files.walk(scratch)) {
                files.sorted(java.util.Comparator.reverseOrder())
                        .forEach(one -> one.toFile().delete());
            }
        }
    }

    @Test
    void anEnglishInterfaceOnAGermanMachineGetsEnglish() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a dialog has to be built for its words to exist");
        withHostileLocaleAndOwnDefaults(ENGLISH, () -> {
            List<String> said = wordsOf(new JOptionPane("Replace?",
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION));
            assertNoGerman(said, "an English option pane");
            assertTrue(said.contains("Yes") && said.contains("No"),
                    "an English reader on a German desktop gets"
                            + " English: " + said);
        });
    }

    /** Without the install, the hostile locale wins - so the test is real. */
    @Test
    void withoutTheInstallTheOperatingSystemWins() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a dialog has to be built for its words to exist");
        SwingSession.restoringLocale(() -> SwingSession.restoring(() -> {
            Locale before = Locale.getDefault();
            Map<String, Object> saved = snapshot();
            try {
                Locale.setDefault(Locale.GERMANY);
                SwingUtilities.invokeAndWait(() -> {
                    juranometria.app.UiTheme.apply(false);
                    remove(saved.keySet());
                });
                List<String> said = wordsOf(new JOptionPane("Ersetzen?",
                        JOptionPane.WARNING_MESSAGE,
                        JOptionPane.YES_NO_OPTION));
                assertTrue(said.contains("Ja") && said.contains("Nein"),
                        "the premise this whole surface rests on: with"
                                + " nothing installed, the toolkit"
                                + " follows the OPERATING SYSTEM and"
                                + " not the reader - " + said);
            } finally {
                Locale.setDefault(before);
                restore(saved);
            }
        }));
    }

    // ---- isolation -------------------------------------------------

    /**
     * Runs a body with a stated language installed and a hostile
     * default locale, and puts the JVM back exactly as it was.
     *
     * <p><strong>Absence is part of the snapshot.</strong> These keys
     * are not in the defaults table before the install - Swing
     * resolves them through a resource bundle - so restoring means
     * <em>removing</em> them again, not writing an old value back.
     * Restoring only the look and feel, which the shared guard does,
     * would leave forty-one developer defaults behind for whatever
     * test ran next.
     */
    private static void withHostileLocaleAndOwnDefaults(
            InterfaceText language, SwingSession.Body body)
            throws Exception {
        SwingSession.restoringLocale(() -> SwingSession.restoring(() -> {
            Locale before = Locale.getDefault();
            Map<String, Object> saved = snapshot();
            try {
                Locale.setDefault(Locale.GERMANY);
                // BOTH supported looks and feels, every time. A look
                // and feel replaces the defaults table, so "it worked
                // under the light theme" is not evidence that it
                // works under the dark one - and this sprint has
                // already been caught by a generator that inherited
                // whichever theme ran first.
                for (boolean dark : new boolean[] {false, true}) {
                    SwingUtilities.invokeAndWait(() -> {
                        // The application's own ordered pair: a look
                        // and feel, then the words, because
                        // installing a look and feel replaces the
                        // table.
                        juranometria.app.UiTheme.apply(dark);
                        SwingText.in(language)
                                .installInto(UIManager.getDefaults());
                    });
                    body.run();
                }
            } finally {
                Locale.setDefault(before);
                restore(saved);
            }
        }));
    }

    /** Every claimed key's value, or its absence, before anything moves. */
    private static Map<String, Object> snapshot() {
        Map<String, Object> saved = new LinkedHashMap<>();
        for (String key : SwingText.toolkitKeys()) {
            saved.put(key, UIManager.getDefaults().get(key));
        }
        return saved;
    }

    private static void restore(Map<String, Object> saved) {
        for (Map.Entry<String, Object> entry : saved.entrySet()) {
            if (entry.getValue() == null) {
                UIManager.getDefaults().remove(entry.getKey());
            } else {
                UIManager.getDefaults().put(entry.getKey(),
                        entry.getValue());
            }
        }
    }

    private static void remove(Iterable<String> keys) {
        for (String key : keys) {
            UIManager.getDefaults().remove(key);
        }
    }

    // ---- walking a real component ---------------------------------

    private static List<String> wordsOf(Component probe)
            throws Exception {
        List<String> said = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            JDialog holder = new JDialog();
            try {
                holder.setContentPane((Container) probe);
                holder.pack();
                walk((Container) probe, said);
            } finally {
                holder.dispose();
            }
        });
        return said;
    }

    private static void walk(Container from, List<String> said) {
        for (Component child : from.getComponents()) {
            if (child instanceof AbstractButton button) {
                add(said, button.getText());
                add(said, button.getToolTipText());
                if (button.getAccessibleContext() != null) {
                    add(said, button.getAccessibleContext()
                            .getAccessibleName());
                }
            }
            if (child instanceof JLabel label) {
                add(said, label.getText());
            }
            if (child instanceof JComponent widget) {
                add(said, widget.getToolTipText());
            }
            if (child instanceof Container nested) {
                walk(nested, said);
            }
        }
    }

    private static void add(List<String> said, String text) {
        if (text != null && !text.isBlank()
                && text.chars().anyMatch(Character::isLetter)
                && !said.contains(text.trim())) {
            said.add(text.trim());
        }
    }

    /** The hostile locale's own words, which must not appear. */
    private static void assertNoGerman(List<String> said, String what) {
        // "Ja" is NOT in this list, and that is the point: German
        // and Norwegian spell it identically, so it discriminates
        // nothing. "Nein" against "Nei" does. A marker that cannot
        // tell the two languages apart would have passed a test that
        // installed nothing at all.
        for (String german : List.of("Nein", "Abbrechen",
                "Speichern", "Öffnen", "Dateiname:", "Hilfe",
                "Aktualisieren", "Eine Ebene h\u00f6her",
                "Startverzeichnis")) {
            assertTrue(!said.contains(german),
                    what + " under a German default locale still said"
                            + " \"" + german + "\", so the atlas did"
                            + " not install that word: " + said);
        }
    }
}
