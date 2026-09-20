package juranometria.ui.language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Told once, and only when it is true (Sprint 33, issue #350).
 *
 * <p>The owner chose Norsk bokmål and the chart's title block went on
 * saying <em>Centre</em> and <em>Field</em>. Nothing was missing: the
 * words existed and a restart produced them. What was missing was
 * anyone saying so.
 *
 * <p>The session really is mixed, which is why the sentence reaches
 * "throughout the application" rather than claiming the change has
 * already happened. A dialog opened after the change
 * resolves its words as it opens, so Chart Options, About, Place and
 * Time and the export switch at once; the menu bar, the toolbar and
 * the drawn page were built at startup and keep that language. A
 * notice claiming nothing had happened would be as untrue as the
 * silence it replaces.
 */
class InterfaceRestartNoticeTest {

    @Test
    void aChangedInterfaceLanguageIsWorthSayingSomethingAbout() {
        InterfaceRestartNotice.Said said =
                InterfaceRestartNotice.forChoice("en", "nb-NO");
        assertNotNull(said, "the reader changed the interface"
                + " language, and the change is restart-bound");
        assertEquals("Programmet må startes på nytt", said.title());
        assertEquals("Språket i programmet er lagret. Start"
                + " JUranometria på nytt for å ta det i bruk i hele"
                + " programmet.", said.message());
        // The title states the boundary; the body repeats what was
        // saved rather than relying on the title to carry half the
        // sentence, because a reader may see either first.
        assertTrue(said.message().contains("lagret"),
                "the body says the choice was saved: "
                        + said.message());
    }

    /**
     * Answered in the language they just asked for.
     *
     * <p>A reader who has chosen Norwegian and is told in English
     * that Norwegian has been saved has been given a small preview
     * of the thing they are complaining about.
     */
    @Test
    void theNoticeSpeaksTheLanguageTheReaderJustChose() {
        assertEquals("Restart required",
                InterfaceRestartNotice.forChoice("nb-NO", "en").title(),
                "changing to English is answered in English");
        assertEquals("Programmet må startes på nytt",
                InterfaceRestartNotice.forChoice("en", "nb-NO").title(),
                "and changing to Norwegian, in Norwegian");
    }

    /**
     * Every other way through Settings says nothing.
     *
     * <p>Confirming unchanged values, and choosing the same language
     * again. The cases that never reach this at all - opening
     * Settings, reading it, Cancel, Reset View - are covered by the
     * seam: only the confirm handler calls this, and Cancel does not
     * reach the confirm handler.
     */
    @Test
    void nothingElseIsWorthInterrupting() {
        assertNull(InterfaceRestartNotice.forChoice("en", "en"),
                "confirming the same interface language says"
                        + " nothing, however many other settings"
                        + " moved - a chart language change applies"
                        + " at once and needs no restart");
        assertNull(InterfaceRestartNotice.forChoice("nb-NO", "nb-NO"),
                "in either language");
        assertNull(InterfaceRestartNotice.forChoice("en", null),
                "and a choice that names no interface language is"
                        + " not a change to one");
    }

    /**
     * A language that cannot answer does not produce half a sentence.
     *
     * <p>Atomic, like the startup failure reporter: both halves from
     * one language or neither. A Norwegian title over an English
     * message reads as a defect in the very thing it is explaining.
     */
    @Test
    void anUnreadableLanguageFallsBackWholeRatherThanMixing() {
        InterfaceRestartNotice.Said said =
                InterfaceRestartNotice.forChoice("en", "zz-ZZ");
        assertNotNull(said, "a language that cannot be read falls"
                + " back to the one the reader has been reading,"
                + " rather than saying nothing");
        assertEquals("Restart required", said.title());
        assertEquals("The interface language has been saved. Restart"
                        + " JUranometria to apply it throughout the"
                        + " application.", said.message(),
                "and both halves come from that one language");

        assertNull(InterfaceRestartNotice.forChoice("zz-ZZ", "yy-YY"),
                "when neither language can answer, the reader is"
                        + " told nothing at all - never a key, never"
                        + " a fragment");
    }

    /**
     * Saving may not depend on any of this.
     *
     * <p>The notice is decided after the choice is persisted, and
     * the seam that shows it swallows everything. This pins the
     * half that is testable without a window: no input makes the
     * decision throw.
     */
    @Test
    void theDecisionNeverThrows() {
        for (String was : new String[] {null, "", "en", "zz"}) {
            for (String now : new String[] {null, "", "en", "zz"}) {
                InterfaceRestartNotice.forChoice(was, now);
            }
        }
    }

    /**
     * The reader can learn the boundary before committing to it.
     *
     * <p>The notice arrives after the fact. The explanation beside
     * the selector is where someone deciding gets to know, and it
     * has to say the same thing the notice will.
     */
    @Test
    void theSelectorSaysTheSameThingBeforeTheReaderCommits()
            throws Exception {
        for (String tag : List.of("en", "nb-NO")) {
            String explain = InterfaceText.forLanguage(tag)
                    .say("settings.language.interface.explain");
            assertTrue(explain.contains("JUranometria"),
                    tag + ": the explanation names the restart"
                            + " boundary before a reader commits to"
                            + " it: " + explain);
        }
        assertTrue(InterfaceText.forLanguage("en")
                        .say("settings.language.interface.explain")
                        .contains("throughout the application"),
                "and reaches as far as the notice does, because the"
                        + " session between the two is a mixed one");
        assertTrue(InterfaceText.forLanguage("nb-NO")
                        .say("settings.language.interface.explain")
                        .contains("i hele programmet"),
                "in Norwegian too");
    }

    /** Only the confirm handler may reach this. */
    @Test
    void nothingButConfirmingSettingsAsksForTheNotice()
            throws Exception {
        List<String> callers = new java.util.ArrayList<>();
        try (var files = Files.walk(Path.of("src/juranometria"))) {
            for (Path file : files.filter(f ->
                    f.toString().endsWith(".java")).toList()) {
                String code = Files.readString(file)
                        .replaceAll("(?s)/\\*.*?\\*/", " ")
                        .replaceAll("(?m)//.*$", " ");
                if (code.contains("InterfaceRestartNotice")) {
                    callers.add(file.getFileName().toString());
                }
            }
        }
        java.util.Collections.sort(callers);
        assertEquals(List.of("InterfaceRestartNotice.java",
                        "SettingsDialog.java"),
                callers,
                "one caller, and it is the dialog's confirm handler."
                        + " A second place deciding when to interrupt"
                        + " a reader is how they end up interrupted"
                        + " twice");
    }
}
