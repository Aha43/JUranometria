package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.ui.language.ExportText;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exporting a sheet says its words in the reader's language, and
 * spells identities as the atlas does (Sprint 33, issue #350).
 *
 * <p>Export is one journey. A reader presses it, meets a chooser,
 * perhaps a question about replacing a file, and then either a
 * confirmation or a refusal - and localising only the first window
 * would leave the consequential half in English. The refusals in
 * particular are the half a reader meets when something has gone
 * wrong, which is the worst moment to be handed a foreign language.
 *
 * <p>{@code SheetFormat} and {@code PaperSize} were the third and
 * fourth domain types found owning English prose this sprint, after
 * {@code SymbolFamily} and {@code PageVisibility}.
 */
class ExportLanguageTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");

    /** Neither enum can hand anybody a reader's sentence any more. */
    @Test
    void theDomainTypesOwnIdentityAndNotProse() throws Exception {
        for (String name : List.of("src/juranometria/sheet/SheetFormat.java",
                "src/juranometria/sheet/PaperSize.java")) {
            String code = Files.readString(Path.of(name))
                    .replaceAll("(?s)/\\*.*?\\*/", " ")
                    .replaceAll("(?m)//.*$", " ");
            assertTrue(!code.contains("readableName"),
                    name + " no longer offers a reader-facing name");
            assertTrue(!code.contains("String explanation"),
                    name + " no longer carries a reader's sentence");
        }
        // And the identities are still exactly what they were.
        assertEquals("SVG", SheetFormat.SVG.identity());
        assertEquals("US Letter", PaperSize.LETTER.identity());
    }

    /**
     * Identities are handed in, never written into a value.
     *
     * <p>Owner ruling: {@code A4} and {@code US Letter} are paper
     * identities in this atlas, like {@code SVG} and {@code PDF}. A
     * Norwegian print shop orders US Letter by that name. If a
     * translator ever found one of them inside a translated sentence
     * they would reasonably translate it, and the atlas would name a
     * paper nobody sells.
     */
    @Test
    void noTranslatedValueContainsAFormatOrPaperIdentity()
            throws Exception {
        List<String> offenders = new ArrayList<>();
        for (String language : List.of("en", "nb-NO")) {
            Path file = Path.of("src/resources/interface-language/"
                    + language + ".properties");
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")
                        || !trimmed.startsWith("export.")) {
                    continue;
                }
                String value = trimmed.split("=", 2)[1];
                for (String identity : List.of("SVG", "PDF", "PNG",
                        "US Letter", "A4")) {
                    // The format explanations name the OTHER formats
                    // on purpose - "SVG and PDF keep the drawing as
                    // lines" is a sentence about them, not a label
                    // for one - so only the naming keys are held.
                    if (trimmed.startsWith("export.format.")
                            || trimmed.startsWith("export.resolution.")) {
                        continue;
                    }
                    if (value.contains(identity)) {
                        offenders.add(trimmed);
                    }
                }
            }
        }
        assertEquals(List.of(), offenders,
                "a format or paper identity is handed to a sentence as"
                        + " an argument, never written into one");
    }

    /** Every whole pattern is given the arguments it asks for. */
    @Test
    void everyExportPatternIsCalledWithTheArgumentsItNames() {
        ExportText words = ExportText.in(EN);

        assertTrue(words.resolution(300).contains("300"),
                "the number is placed by the language, not glued on");
        assertTrue(words.replaceQuestion("orion.svg", "/tmp")
                        .contains("orion.svg"),
                "and so are a file name and its folder");
        assertTrue(words.replaceQuestion("orion.svg", "/tmp")
                        .contains("/tmp"));

        // The arity check refuses a pattern asking for more than it
        // was given, naming the key - so a translation that added an
        // argument fails loudly rather than printing a placeholder.
        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> EN.say("export.replace.question", "only-one"));
        assertTrue(refused.getMessage().contains("export.replace.question"),
                "and says which key: " + refused.getMessage());
    }

    /** Each visible label is programmatically tied to its control. */
    @Test
    void everyVisibleLabelNamesTheControlItBelongsTo() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the relation exists on a built dialog");
        inDialog(EN, content -> {
            List<String> unrelated = new ArrayList<>();
            int related = 0;
            for (JLabel label : labelsIn(content)) {
                if (label.getText() == null
                        || !label.getText().endsWith(":")) {
                    continue;
                }
                if (label.getLabelFor() == null) {
                    unrelated.add(label.getText());
                } else {
                    related++;
                }
            }
            assertEquals(List.of(), unrelated,
                    "a visible label is tied to its control, so a"
                            + " screen reader can say which control it"
                            + " names");
            assertEquals(3, related,
                    "the premise: there are three such labels -"
                            + " format, paper and resolution");
        });
    }

    /**
     * The disabled resolution keeps an explanation that is true.
     *
     * <p>It says SVG and PDF ignore the setting, which is why it is
     * grey - rather than describing an action it will not perform.
     * That is the opposite of the mistake the toolbar's magnitude
     * buttons made, and it is worth keeping rather than normalising.
     */
    @Test
    void theDisabledResolutionStillExplainsWhyItIsIgnored()
            throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "enablement follows a built dialog's format choice");
        inDialog(EN, content -> {
            JComboBox<Object> format = comboNamed(content, "export.format");
            JComboBox<Object> resolution =
                    comboNamed(content, "export.resolution");
            format.setSelectedItem(SheetFormat.SVG);
            assertTrue(!resolution.isEnabled(),
                    "the premise: SVG greys the resolution");
            assertEquals(EN.say("export.resolution.explain"),
                    resolution.getAccessibleContext()
                            .getAccessibleDescription(),
                    "and it still explains the ignoring");
            format.setSelectedItem(SheetFormat.PNG);
            assertTrue(resolution.isEnabled(),
                    "PNG gives it back");
        });
    }

    /** A resolution is read as words, with the number unchanged. */
    @Test
    void aResolutionIsSaidAsWordsAroundItsNumber() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the list is painted by a built dialog's renderer");
        inDialog(EN, content -> {
            JComboBox<Object> resolution =
                    comboNamed(content, "export.resolution");
            List<String> shown = new ArrayList<>();
            JList<Object> list = new JList<>();
            for (int i = 0; i < resolution.getItemCount(); i++) {
                Component cell = resolution.getRenderer()
                        .getListCellRendererComponent(list,
                                resolution.getItemAt(i), i, false, false);
                if (cell instanceof JLabel label) {
                    shown.add(label.getText());
                }
            }
            assertTrue(shown.contains(EN.say("export.resolution.item", "300")),
                    "the list says what the language says: " + shown);
        });
    }

    /** Every refusal a temporary directory can produce, said in words. */
    @Test
    void eachDeterministicRefusalSaysWhatWentWrong(@TempDir Path folder)
            throws Exception {
        // Reached with real filesystem states rather than asserted in
        // isolation, so each is the sentence the branch actually
        // raises.
        assertEquals(EN.say("export.refused.nofile"),
                refusalFor(null, folder),
                "no destination at all");

        Path missing = folder.resolve("nowhere").resolve("sheet.svg");
        assertEquals(EN.say("export.refused.nofolder",
                        missing.getParent().toAbsolutePath().toString()),
                refusalFor(missing.toFile(), folder),
                "a folder that is not there names the path it looked"
                        + " for");

        Path directory = Files.createDirectory(folder.resolve("a.svg"));
        assertEquals(EN.say("export.refused.isfolder",
                        directory.getFileName().toString()),
                refusalFor(directory.toFile(), folder),
                "a directory where a file was meant");
    }

    /** A refusal names the file, and the file only. */
    @Test
    void aRefusalCarriesTheNameAsDataAndTheSentenceAsLanguage(
            @TempDir Path folder) throws Exception {
        Path directory = Files.createDirectory(folder.resolve("orion.svg"));
        String said = refusalFor(directory.toFile(), folder);

        assertTrue(said.contains("orion.svg"),
                "the name reaches the reader unchanged: " + said);
        assertNotEquals("orion.svg", said,
                "inside a sentence the language owns");
        assertEquals(EN.say("export.refused.isfolder", "orion.svg"), said,
                "and the sentence is the pattern, not a concatenation");
    }

    /**
     * No refusal is built by gluing prose around a name.
     *
     * <p>A source contract, and it is here because the obvious test
     * could not see the defect. Comparing the refusal against
     * {@code EN.say(key, name)} passes whether the sentence came from
     * the pattern or from {@code file.getName() + " is a folder, not
     * a file."} - the two produce identical English, which is exactly
     * the blind spot this sprint keeps finding. Restoring the
     * concatenation was mutation-tested and the equality assertion
     * stayed green; this is what failed it.
     */
    @Test
    void noRefusalIsAssembledAroundAFileName() throws Exception {
        String code = Files.readString(
                Path.of("src/juranometria/app/ExportSheet.java"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");

        List<String> glued = new ArrayList<>();
        for (String fragment : List.of("\" is a folder",
                "\" What was already there is unchanged.\"",
                "failure.getMessage()",
                "\" cannot be replaced", "\" was left as it was",
                "\" cannot be written to", "There is no folder at ",
                "No file was chosen.")) {
            if (code.contains(fragment)) {
                glued.add(fragment);
            }
        }
        assertEquals(List.of(), glued,
                "every refusal is a whole pattern taking the name or"
                        + " path as an argument; none is a sentence"
                        + " with a value spliced into it");
    }

    /**
     * A file the reader chose not to replace.
     *
     * <p>Reached through {@code ReplaceDecision.REFUSE} rather than a
     * modal click: the answer is the input, and what is under test is
     * the sentence that follows it. The real prompt is held
     * separately through its own {@code Confirmer} seam.
     */
    @Test
    void refusingToReplaceSaysTheFileWasLeftUnchanged(@TempDir Path folder)
            throws Exception {
        Path existing = Files.writeString(folder.resolve("orion.svg"),
                "the original");

        ExportSheet.Outcome outcome = write(existing.toFile(),
                ExportSheet.ReplaceDecision.REFUSE, ExportSheet.SINK);

        assertEquals(EN.say("export.refused.kept", "orion.svg"),
                reasonOf(outcome),
                "the reader is told which file kept its contents");
        assertEquals("the original", Files.readString(existing),
                "and it really did keep them");
    }

    /**
     * The write itself fails, at a new destination and at an old one.
     *
     * <p>{@code ByteSink} exists so that this can be tried. The two
     * cases say different things, and the difference is the point:
     * replacing something and failing must tell a reader their file
     * is still there.
     */
    @Test
    void aFailedWriteSaysWhetherAnythingWasLost(@TempDir Path folder)
            throws Exception {
        ExportSheet.ByteSink broken = (file, bytes) -> {
            throw new java.io.IOException("the disk said no");
        };

        Path fresh = folder.resolve("new.svg");
        assertEquals(EN.say("export.refused.notwritten", "new.svg"),
                reasonOf(write(fresh.toFile(),
                        ExportSheet.ReplaceDecision.REFUSE, broken)),
                "a new destination: nothing was there and nothing is");
        assertTrue(!Files.exists(fresh),
                "and nothing was left looking finished");

        Path existing = Files.writeString(folder.resolve("old.svg"),
                "the original");
        assertEquals(EN.say("export.refused.notreplaced", "old.svg"),
                reasonOf(write(existing.toFile(),
                        existing1 -> true, broken)),
                "an existing destination: the reader is told their"
                        + " file is unchanged");
        assertEquals("the original", Files.readString(existing),
                "and it is");

        // The exception's own words never reach the reader: they are
        // written by whoever threw them, in whatever language that
        // library uses.
        assertTrue(!reasonOf(write(fresh.toFile(),
                        ExportSheet.ReplaceDecision.REFUSE, broken))
                        .contains("the disk said no"),
                "and the platform's own message is not spliced in");
    }

    /**
     * A byte count is grouped by the language, not by English.
     *
     * <p>Through {@code Written.message} - the call site - rather than
     * through {@code say} with a number of this test's own. Asking the
     * pattern proves the pattern; what was wrong was the argument the
     * call site handed it, and a test that built its own argument
     * could not see that. Mutation-proved: restoring
     * {@code String.format(ROOT, "%,d", bytes)} left the earlier
     * version of this test green.
     */
    @Test
    void theByteCountIsGroupedInTheLanguageBeingRead(@TempDir Path folder)
            throws Exception {
        ExportSheet.Outcome outcome = write(
                folder.resolve("orion.svg").toFile(),
                ExportSheet.ReplaceDecision.REFUSE, ExportSheet.SINK);
        assertTrue(outcome instanceof ExportSheet.Outcome.Written,
                "the premise: a sheet really was written - " + outcome);
        ExportSheet.Outcome.Written written =
                (ExportSheet.Outcome.Written) outcome;
        assertTrue(written.bytes() > 100_000,
                "and it is large enough for grouping to show: "
                        + written.bytes());

        InterfaceText norsk = InterfaceText.forLanguage("nb-NO");
        String english = written.message(EN);
        String said = written.message(norsk);

        assertTrue(english.matches(".*\\d,\\d{3}.*"),
                "English groups with commas: " + english);
        // A NO-BREAK space (U+00A0), which is the point: it is the
        // language's own punctuation, and asserting a plain space
        // here would have been asserting English typing habits about
        // a Norwegian number.
        assertTrue(said.matches(".*\\d\u00a0\\d{3}.*"),
                "Norwegian groups with no-break spaces: " + said);
        assertTrue(!said.contains(","),
                "and never with a comma, which in Norwegian is the"
                        + " decimal mark: " + said);
        assertNotEquals(english, said,
                "the count was formatted in Locale.ROOT before it"
                        + " reached the pattern, which handed one"
                        + " language's punctuation to every other");
    }

    /**
     * Nothing the Norwegian export journey says is English.
     *
     * <p>Every application-owned value, across the dialog and the
     * whole after-Export half. Identities and data are exempt by
     * construction: this compares the sentences, and `SVG`, a file
     * name and a byte count are not sentences.
     */
    @Test
    void noEnglishPhraseSurvivesIntoTheNorwegianJourney() throws Exception {
        InterfaceText norsk = InterfaceText.forLanguage("nb-NO");
        List<String> untranslated = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(
                "src/resources/interface-language/en.properties"))) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("export.") || !trimmed.contains("=")) {
                continue;
            }
            String key = trimmed.split("=", 2)[0].trim();
            String english = EN.say(key);
            // Identity-only values are the same on purpose.
            if (english.equals(norsk.say(key))
                    && !key.endsWith(".a11y")) {
                untranslated.add(key + " = " + english);
            }
        }
        assertEquals(List.of(), untranslated,
                "every export sentence is written in Norwegian rather"
                        + " than falling back to English");
    }

    // ---- driving the real code ---------------------------------------

    /** One write, with a stated replace decision and sink. */
    private static ExportSheet.Outcome write(File destination,
                                             ExportSheet.ReplaceDecision replace,
                                             ExportSheet.ByteSink sink) {
        return ExportSheet.write(
                (state, wide, high) -> juranometria.app.Atlas.assembler()
                        .assemble(state, wide, high),
                juranometria.chart.ChartViewState.DEFAULT,
                juranometria.render.ChartOptions.DEFAULTS,
                juranometria.render.ChartRenderer.ReferenceLayer.NONE,
                juranometria.render.ChartRenderer.ReferenceLayer.NONE,
                new ExportSheet.Request(SheetFormat.SVG, PaperSize.A4,
                        150, false),
                destination, replace, sink, EN);
    }

    private static String reasonOf(ExportSheet.Outcome outcome) {
        assertTrue(outcome instanceof ExportSheet.Outcome.Refused,
                "the premise: this is refused - " + outcome);
        return ((ExportSheet.Outcome.Refused) outcome).reason();
    }


    /** What the writer refuses, for one destination. */
    private static String refusalFor(File destination, Path folder)
            throws Exception {
        ExportSheet.Outcome outcome = ExportSheet.write(
                (state, wide, high) -> juranometria.app.Atlas.assembler()
                        .assemble(state, wide, high),
                juranometria.chart.ChartViewState.DEFAULT,
                juranometria.render.ChartOptions.DEFAULTS,
                juranometria.render.ChartRenderer.ReferenceLayer.NONE,
                new ExportSheet.Request(SheetFormat.SVG, PaperSize.A4,
                        150, false),
                destination, existing -> true, EN);
        assertTrue(outcome instanceof ExportSheet.Outcome.Refused,
                "the premise: this destination is refused - " + outcome);
        return ((ExportSheet.Outcome.Refused) outcome).reason();
    }

    private interface Check {
        void on(JComponent content);
    }

    private static void inDialog(InterfaceText said, Check check)
            throws Exception {
        JFrame[] owner = new JFrame[1];
        JComponent[] content = new JComponent[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                content[0] = ExportSheetDialog.contentForStudy(said);
                owner[0] = new JFrame("export");
                owner[0].setContentPane(content[0]);
                owner[0].pack();
            });
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> check.on(content[0]));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static JComboBox<Object> comboNamed(Container from, String name) {
        for (Component child : from.getComponents()) {
            if (name.equals(child.getName())
                    && child instanceof JComboBox) {
                return (JComboBox<Object>) child;
            }
            if (child instanceof Container nested) {
                JComboBox<Object> found = comboNamed(nested, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
