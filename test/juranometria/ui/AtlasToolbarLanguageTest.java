package juranometria.ui;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionMode;
import juranometria.chart.SkyPosition;
import juranometria.ui.language.InterfaceLanguages;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The toolbar speaks the reader's language, and keeps the keystrokes
 * the platform gave it (Sprint 33, issue #350).
 *
 * <p>Held here are the repairs the inventory forced, each of which
 * was a defect in English before it was a translation problem:
 *
 * <ul>
 *   <li>a disabled magnitude button described what it would do. At
 *       V 4.0 <em>Fewer stars</em> still said "Draws only the
 *       brighter stars, one step at a time" - a control gone grey,
 *       telling a reader it does something it will not;
 *   <li>the end-of-ladder sentence was assembled by lowercasing the
 *       button's English label with {@code Locale.ROOT}, which is an
 *       English rule about English words offered as though it were
 *       universal;
 *   <li>{@code Shortcuts.saying} decided centrally that a keystroke
 *       goes last and in round brackets - English typography applied
 *       to every language at once;
 *   <li>the version label was quietened with
 *       {@code setEnabled(false)}, so the one purely informative
 *       thing on the bar was announced as unavailable.
 * </ul>
 *
 * <p>{@code SearchField} is on this bar and is deliberately still
 * English: it is a separate language surface with its own grammar and
 * its own inventory to come. Nothing here asserts about it.
 */
class AtlasToolbarLanguageTest {

    private static final String NORWEGIAN = "nb-NO";
    private static final InterfaceText EN = InterfaceText.forLanguage("en");
    private static final InterfaceText NB =
            InterfaceText.forLanguage(NORWEGIAN);

    /**
     * Neither fragment-assembly helper survives in toolbar prose.
     *
     * <p>A source contract, because both produced <em>plausible</em>
     * English: nothing a reader saw looked wrong, and no test
     * comparing English to English would have noticed either.
     */
    @Test
    void theToolbarBuildsNoSentenceFromFragments() throws Exception {
        String code = Files.readString(
                Path.of("src/juranometria/ui/AtlasToolbar.java"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");

        assertTrue(!code.contains("toLowerCase"),
                "no English casing rule is applied to a word that a"
                        + " translation owns");
        assertTrue(!code.contains("Shortcuts.saying"),
                "and no helper decides centrally where a keystroke"
                        + " sits in a sentence; the language places it"
                        + " and Shortcuts still spells it");
    }

    /** At either end of the magnitude ladder, the control says so. */
    @Test
    void aMagnitudeControlAtTheEndOfItsLadderSaysSo() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the bar wires its wording when a controller reports");
        for (InterfaceText said : List.of(EN, NB)) {
            String brightest = said.say("toolbar.fewerStars.end.explain",
                    "4.0");
            String ordinary = said.say("toolbar.fewerStars.explain");

            List<String> spoken = new ArrayList<>();
            atLimit(said, 4.0, bar -> spoken.addAll(chromeOf(bar)));
            assertTrue(spoken.contains(brightest),
                    "at V 4.0 the brighter step says why it cannot"
                            + " step: " + brightest);
            assertTrue(!spoken.contains(ordinary),
                    "and does not go on describing what it would do."
                            + " This is the mutation that matters: put"
                            + " the ordinary description back on a"
                            + " disabled control and a reader is told"
                            + " it does something it will not - \""
                            + ordinary + "\"");

            List<String> faint = new ArrayList<>();
            atLimit(said, 8.0, bar -> faint.addAll(chromeOf(bar)));
            assertTrue(faint.contains(
                            said.say("toolbar.moreStars.end.explain", "8.0")),
                    "and the fainter step does the same at V 8.0");
            assertTrue(!faint.contains(said.say("toolbar.moreStars.explain")),
                    "without keeping its ordinary description");
        }
    }

    /**
     * The magnitude value is notation and is spelled once.
     *
     * <p>It is formatted in {@code Locale.ROOT} and handed to the
     * pattern already written, so no language turns the point of
     * {@code V 4.0} into a comma.
     */
    @Test
    void theMagnitudeValueIsIdenticalInBothLanguages() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the value is written when the controller reports");
        List<String> english = new ArrayList<>();
        List<String> norsk = new ArrayList<>();
        atLimit(EN, 4.0, bar -> english.addAll(chromeOf(bar)));
        atLimit(NB, 4.0, bar -> norsk.addAll(chromeOf(bar)));

        assertTrue(english.stream().anyMatch(s -> s.contains("V 4.0")),
                "English spells the limit V 4.0: " + english);
        assertTrue(norsk.stream().anyMatch(s -> s.contains("V 4.0")),
                "and so does Norwegian, unchanged: " + norsk);
    }

    /** The readout's values are notation; only its frame is language. */
    @Test
    void theReadoutKeepsItsValuesAndChangesItsFrame() {
        String english = EN.say("toolbar.readout", "42", "6.0");
        String norsk = NB.say("toolbar.readout", "42", "6.0");

        assertNotEquals(english, norsk,
                "the frame is the language's: " + english + " / " + norsk);
        for (String said : List.of(english, norsk)) {
            assertTrue(said.contains("42°"),
                    "the field keeps its digits and its degree sign: "
                            + said);
            assertTrue(said.contains("V 6.0"),
                    "and the magnitude keeps its band and its point: "
                            + said);
            assertTrue(said.contains(" · "),
                    "and the atlas's own separator: " + said);
        }
    }

    /**
     * The language places the keystroke; the platform spells it.
     *
     * <p>The keystroke is asked of the same registry that binds it,
     * so a hover cannot promise a stroke the menu does not answer,
     * and it is whatever this desktop calls those keys.
     */
    @Test
    void theLanguageOwnsThePlacementAndNotTheKeystroke() {
        String keys = Shortcuts.text(Shortcuts.ZOOM_IN);
        assertTrue(keys != null && !keys.isBlank(),
                "the premise: this platform names that binding");

        String english = juranometria.ui.language.ShortcutText.in(EN)
                .withKeystroke(EN.say("toolbar.zoomIn.hover"),
                        Shortcuts.ZOOM_IN);
        String norsk = juranometria.ui.language.ShortcutText.in(NB)
                .withKeystroke(NB.say("toolbar.zoomIn.hover"),
                        Shortcuts.ZOOM_IN);

        assertTrue(english.contains(keys) && norsk.contains(keys),
                "the keystroke reaches a reader unchanged in both: "
                        + keys);
        assertNotEquals(english, norsk,
                "while the words around it are each language's own: "
                        + english + " / " + norsk);
        assertTrue(!english.contains("Zoom inn") && !norsk.contains("Zoom in ("),
                "with no English text left in the Norwegian form: "
                        + norsk);
    }

    /**
     * A two-keystroke hint is composed from both patterns, not one.
     *
     * <p>Two layers can freeze independently: the connector between
     * the keystrokes, and the placement of the whole thing beside the
     * description. The connector is caught by Norwegian, because the
     * two packs word it differently. The placement is not: both
     * declare {@code {0} ({1})}, so a hard-coded English parenthesis
     * is byte-identical in either language and comparing them proves
     * nothing.
     *
     * <p>So it is held against the patterns themselves. If
     * {@code withSequence} ever composes the placement itself, this
     * stops matching what the pack says - in English, without waiting
     * for a language that punctuates differently to exist.
     */
    @Test
    void aTwoKeystrokeHintUsesBothPatternsAndComposesNeither()
            throws Exception {
        for (InterfaceText said : List.of(EN, NB)) {
            juranometria.ui.language.ShortcutText shortcuts =
                    juranometria.ui.language.ShortcutText.in(said);
            String composed = shortcuts.withSequence("Do the thing",
                    "\u2318K", "I");
            // Consistency, not proof: with both packs declaring
            // {0} ({1}), a hard-coded English parenthesis produces
            // the identical string, and this assertion passes under
            // that mutation. Mutation-tested, and it did.
            assertEquals(said.say("shortcut.hovered", "Do the thing",
                            said.say("shortcut.sequence", "\u2318K", "I")),
                    composed,
                    "the hint agrees with the two patterns");
            assertTrue(composed.contains("\u2318K") && composed.contains("I"),
                    "with the keystrokes unchanged: " + composed);
        }
        assertNotEquals(
                EN.say("shortcut.sequence", "\u2318K", "I"),
                NB.say("shortcut.sequence", "\u2318K", "I"),
                "the premise for the connector half: the two packs"
                        + " really do join two keystrokes differently");

        // The placement half, proved at RUNTIME against a pack that
        // punctuates it differently. No shipped language does - both
        // declare "{0} ({1})" - so a hard-coded English parenthesis
        // produces a byte-identical string and comparing the two
        // shipped packs proves nothing. Mutation-tested: it did not.
        //
        // The pack lives here, reaches InterfaceText through its own
        // resource route, and is outside production discovery and the
        // packaged resources entirely.
        InterfaceText reordered = InterfaceText.forLanguage("x-placement",
                AtlasToolbarLanguageTest::testPack);
        String placed = juranometria.ui.language.ShortcutText.in(reordered)
                .withSequence("Do the thing", "\u2318K", "I");
        assertEquals("[\u2318K then I] Do the thing", placed,
                "the language decides where the keystrokes go, and"
                        + " this one puts them first and in square"
                        + " brackets: " + placed);

        // And the source guard, as a second line rather than the
        // proof.
        String code = Files.readString(Path.of(
                "src/juranometria/ui/language/ShortcutText.java"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");
        assertTrue(!code.contains("\" (\""),
                "with no parenthesis written into the class");
    }

    /**
     * A language that punctuates a shortcut hint differently.
     *
     * <p>Test-only: it is served from here rather than from the
     * classpath, so production discovery and the packaged resources
     * never see it. English is supplied because every language falls
     * back to it.
     */
    private static java.io.InputStream testPack(String path) {
        String pack = path.endsWith("en.properties")
                ? "shortcut.hovered = {0} ({1})\n"
                        + "shortcut.sequence = {0} then {1}\n"
                : path.endsWith("x-placement.properties")
                        ? "shortcut.hovered = [{1}] {0}\n"
                        : null;
        return pack == null ? null : new java.io.ByteArrayInputStream(
                pack.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * The reordering pack is a test instrument and never ships.
     *
     * <p>It exists because the two real packs punctuate a shortcut
     * hint identically, so freezing the placement into
     * {@code ShortcutText} changes no output either of them can
     * produce - English compared to English, which is how three
     * surfaces passed with a defect in them (#350).
     *
     * <p>An instrument that reached production would be a language
     * offered to a reader that nobody wrote and nobody reviewed. So
     * three claims, not one: discovery does not list it, the
     * classpath does not carry it, and the resource directory the
     * image is built from has no file with that name. The first
     * alone would pass if the file were present but unlisted.
     */
    @Test
    void theReorderingPackIsAnInstrumentAndNeverShips() throws Exception {
        List<String> offered = InterfaceLanguages.discover().tags();
        assertTrue(!offered.isEmpty(),
                "the premise: discovery found the real languages - "
                        + offered);
        assertTrue(!offered.contains("x-placement"),
                "no reader is offered the test pack: " + offered);

        // Not on the classpath the packaged image is built from, by
        // the same route InterfaceText.forLanguage(tag) uses.
        assertNull(InterfaceText.class.getResourceAsStream(
                        "/resources/interface-language/"
                                + "x-placement.properties"),
                "and the classpath carries no such resource");

        // And not in the directory that becomes that classpath.
        try (java.util.stream.Stream<Path> files =
                     Files.list(Path.of(
                             "src/resources/interface-language"))) {
            List<String> names = files.map(each ->
                    each.getFileName().toString()).sorted().toList();
            assertTrue(names.size() >= 4,
                    "the premise: the real packs are there - " + names);
            assertTrue(names.stream().noneMatch(each ->
                            each.startsWith("x-placement")),
                    "and nothing named for the instrument: " + names);
        }
    }

    /** Every zoom form exists in both languages, and they differ. */
    @Test
    void everyZoomFormIsWrittenWholeInBothLanguages() {
        for (String stem : List.of("toolbar.zoomIn", "toolbar.zoomOut")) {
            for (String role : List.of(".hover", ".explain", ".end",
                    ".overview.hover", ".overview.explain")) {
                String english = EN.say(stem + role);
                String norsk = NB.say(stem + role);
                assertTrue(!english.isBlank() && !norsk.isBlank(),
                        stem + role + " is written in both languages");
                assertNotEquals(english, norsk,
                        stem + role + " is really translated rather"
                                + " than falling back to English");
            }
        }
        assertNotEquals(EN.say("toolbar.zoomIn.end"),
                EN.say("toolbar.zoomOut.end"),
                "and the two ends of the ladder say different things,"
                        + " which the old assembled sentence achieved"
                        + " only by lowercasing a label");
    }

    /**
     * The version is information, not an unavailable control.
     *
     * <p>Two claims, because the repair could satisfy either alone
     * and be wrong: it must not be disabled, and it must still look
     * subordinate.
     */
    @Test
    void theVersionIsQuietWithoutBeingAnnouncedAsUnavailable()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "a colour is resolved by a realised look and feel");
        // Under both themes, because the subdued colour is resolved
        // per theme: written down once in light it would be wrong in
        // dark, and a test that never installed the application's own
        // look and feel would not notice either - the first version
        // of this test did exactly that and measured the plain
        // label's colour. Look and feel is process-wide state and is
        // restored by the shared guard (#224).
        juranometria.app.SwingSession.restoring(() -> {
            for (boolean dark : new boolean[] {false, true}) {
                SwingUtilities.invokeAndWait(() -> {
                    juranometria.app.UiTheme.apply(dark);
                    com.formdev.flatlaf.FlatLaf.updateUI();
                });
                checkVersion(dark);
            }
        });
    }

    private static void checkVersion(boolean dark) throws Exception {
        for (InterfaceText said : List.of(EN, NB)) {
            build(said, 42.0, 6.0, bar -> {
                JLabel version = null;
                for (JLabel label : labelsIn(bar)) {
                    if ("v2.0.0".equals(label.getText())) {
                        version = label;
                    }
                }
                assertTrue(version != null, "the premise: it is shown");
                assertTrue(version.isEnabled(),
                        "the version is not a disabled control. A"
                                + " screen reader announces disabled as"
                                + " unavailable, and this is the one"
                                + " thing here a reader can only read");
                assertTrue(!version.isFocusable(),
                        "and it is still not in the tab order");
                assertEquals(said.say("toolbar.version.a11y", "2.0.0"),
                        version.getAccessibleContext().getAccessibleName(),
                        "and it names itself in the reader's language");
                assertEquals(
                        javax.swing.UIManager.getColor(
                                "Label.disabledForeground"),
                        version.getForeground(),
                        "while staying visually subordinate: it wears"
                                + " the theme's own subdued colour,"
                                + " resolved for "
                                + (dark ? "dark" : "light"));
                assertNotEquals(new JLabel().getForeground(),
                        version.getForeground(),
                        "which is not an ordinary label's colour in "
                                + (dark ? "dark" : "light"));
            });
        }
    }

    /** An icon-only control says everything through words it has none of. */
    @Test
    void everyIconOnlyControlSpeaksOnAllThreeChannels() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the controls are wired when the bar is built");
        for (InterfaceText said : List.of(EN, NB)) {
            List<String> silent = new ArrayList<>();
            build(said, 42.0, 6.0, bar -> {
                for (Component child : bar.getComponents()) {
                    if (!(child instanceof JButton button)) {
                        continue;
                    }
                    if (button.getText() != null
                            && !button.getText().isBlank()) {
                        continue;
                    }
                    String name = button.getAccessibleContext()
                            .getAccessibleName();
                    String description = button.getAccessibleContext()
                            .getAccessibleDescription();
                    if (blank(button.getToolTipText()) || blank(name)
                            || blank(description)) {
                        silent.add(name + " [hover=" + button.getToolTipText()
                                + ", spoken=" + description + "]");
                    }
                }
            });
            assertEquals(List.of(), silent,
                    "an icon-only control has no visible words at all,"
                            + " so the tooltip is everything a sighted"
                            + " reader has and the description is"
                            + " everything anyone else has - in "
                            + said.say("toolbar.zoomIn.a11y"));
        }
    }

    // ---- building the real bar ---------------------------------------

    private interface Check {
        void on(AtlasToolbar bar);
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    /** The bar at a stated magnitude limit, reached by stepping. */
    private static void atLimit(InterfaceText said, double limit, Check check)
            throws Exception {
        build(said, 42.0, limit, check);
    }

    private static void build(InterfaceText said, double field, double limit,
                              Check check) throws Exception {
        JFrame[] owner = new JFrame[1];
        AtlasToolbar[] bar = new AtlasToolbar[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                ChartViewController controller = new ChartViewController(
                        Atlas.assembler()::fits);
                SearchField search = new SearchField(Atlas.search(),
                        Atlas.assembler(), controller);
                InspectorToggle toggle = new InspectorToggle();
                toggle.bind(() -> { }, () -> true);
                bar[0] = new AtlasToolbar(controller, search, toggle,
                        "2.0.0", () -> { }, new SelectionMode(), said);
                controller.recenter(new SkyPosition(83.8, 0.0), field);
                // The limit has no setter; it is stepped the way a
                // reader steps it, which is also the only way the
                // can-queries reach their ends.
                for (int i = 0; i < 12
                        && controller.state().limitingMagnitude() > limit
                        && controller.canDecreaseMagnitudeLimit(); i++) {
                    controller.decreaseMagnitudeLimit();
                }
                for (int i = 0; i < 12
                        && controller.state().limitingMagnitude() < limit
                        && controller.canIncreaseMagnitudeLimit(); i++) {
                    controller.increaseMagnitudeLimit();
                }
                owner[0] = new JFrame("toolbar");
                owner[0].setContentPane(bar[0]);
                owner[0].pack();
            });
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> check.on(bar[0]));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
        }
    }

    /** Every word the bar says, on every channel. */
    private static List<String> chromeOf(Container from) {
        List<String> said = new ArrayList<>();
        for (Component child : from.getComponents()) {
            if (child instanceof JComponent widget) {
                if (widget instanceof AbstractButton button) {
                    add(said, button.getText());
                }
                if (widget instanceof JLabel label) {
                    add(said, label.getText());
                }
                add(said, widget.getToolTipText());
                if (widget.getAccessibleContext() != null) {
                    add(said, widget.getAccessibleContext()
                            .getAccessibleName());
                    add(said, widget.getAccessibleContext()
                            .getAccessibleDescription());
                }
            }
            if (child instanceof Container nested) {
                said.addAll(chromeOf(nested));
            }
        }
        return said;
    }

    private static void add(List<String> said, String text) {
        if (text != null && !text.isBlank()) {
            said.add(text.replaceAll("<[^>]*>", " ")
                    .replaceAll("\\s+", " ").trim());
        }
    }

    private static List<JLabel> labelsIn(Container from) {
        List<JLabel> found = new ArrayList<>();
        for (Component child : from.getComponents()) {
            if (child instanceof JLabel label) {
                found.add(label);
            }
            if (child instanceof Container nested) {
                found.addAll(labelsIn(nested));
            }
        }
        return found;
    }
}
