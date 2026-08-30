package juranometria.app;

import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AboutDialogTest {

    @Test
    void theDisplayedVersionIsThePackagedVersionNeverASecondCopy() throws Exception {
        String packaged = Files.readString(Path.of("VERSION")).trim();
        assertEquals(packaged, AppInfo.version(),
                "AppInfo reads the packaged VERSION resource");
        JComponent content = AboutDialog.compactContent(() -> { });
        assertTrue(labels(content).stream().anyMatch(text ->
                        text.equals(AppInfo.NAME + " " + AppInfo.version())),
                "the compact view titles itself from AppInfo");
    }

    @Test
    void theCompactSummaryStatesEveryLicenceFamilyAndTheNcConsequence() {
        String summary = AboutDialog.summaryText();
        assertTrue(summary.contains("MIT license"));
        assertTrue(summary.contains("JUranometria\nContributors")
                        || summary.contains("JUranometria Contributors"),
                "code copyright holder is named");
        assertTrue(summary.contains("CC BY-NC 3.0 IGO"));
        assertTrue(summary.contains("non-commercially"),
                "the practical consequence is stated in plain language");
        assertTrue(summary.contains("CC BY-SA 4.0"));
        assertTrue(summary.contains("BSD-3-Clause"));
        assertTrue(summary.contains("Tabler"));
    }

    /** The resource/licence pairings both documents must agree on. */
    private static final String[][] PAIRINGS = {
            {"code", "MIT"},
            {"tycho", "CC BY-NC 3.0 IGO"},
            {"openngc", "CC BY-SA 4.0"},
            {"constellation", "BSD-3-Clause"},
            {"tabler|icons", "MIT"},
    };

    @Test
    void eachLicenceStaysAttachedToItsResourceInBothDocuments() throws Exception {
        // The source-of-truth rule, semantically (Sprint 11 Codex
        // review, P2): it is not enough that the licence names occur -
        // each licence must appear in the same paragraph as its
        // resource, in the packaged summary and in LICENSING.md alike,
        // so a swapped assignment can never pass.
        assertPairings("the packaged summary", AboutDialog.summaryText());
        assertPairings("LICENSING.md",
                Files.readString(Path.of("LICENSING.md")));
        // The practical consequence rides with the Tycho-2 pairing.
        assertTrue(paragraphWith(AboutDialog.summaryText(), "tycho")
                        .contains(normalize("non-commercially")),
                "the summary's Tycho paragraph states the consequence");
        assertTrue(paragraphWith(Files.readString(Path.of("LICENSING.md")),
                        "tycho")
                        .contains(normalize("may not be used commercially")),
                "LICENSING.md's Tycho entry states the consequence");
    }

    private static void assertPairings(String document, String text) {
        for (String[] pairing : PAIRINGS) {
            String paragraph = paragraphWith(text, pairing[0]);
            assertTrue(paragraph.contains(normalize(pairing[1])),
                    document + ": the paragraph naming '" + pairing[0]
                            + "' must carry " + pairing[1]);
        }
    }

    /**
     * The first normalized paragraph (blank-line block or table row)
     * mentioning any of the |-separated keywords; empty if none does,
     * which fails the containing assertion.
     */
    private static String paragraphWith(String text, String keywords) {
        for (String paragraph : text.split("\\n\\s*\\n|\\n(?=\\|)")) {
            String normalized = normalize(paragraph);
            for (String keyword : keywords.split("\\|")) {
                if (normalized.contains(normalize(keyword))) {
                    return normalized;
                }
            }
        }
        return "";
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "");
    }

    @Test
    void everyBundledNoticeResourceExistsAndTheFullViewCarriesThem() {
        for (String[] notice : AboutDialog.NOTICE_RESOURCES) {
            assertNotNull(AboutDialog.class.getResource(notice[1]),
                    notice[1] + " must ship on the classpath");
        }
        String notices = AboutDialog.noticesText();
        assertTrue(notices.contains("may not be used commercially"),
                "the Tycho-2 notice's restriction is present verbatim");
        assertTrue(notices.contains("CC-BY-SA-4.0"));
        assertTrue(notices.contains(
                "Redistribution and use in source and binary forms"),
                "the full BSD licence text is present");
        assertTrue(notices.contains("MIT License"),
                "the icons' MIT licence text is present");
        assertTrue(notices.contains("not an IAU standard"),
                "the figure-convention honesty travels to the end user");
    }

    @Test
    void theCompactViewWiresItsButtonsAndTouchesNoChartState() {
        int[] noticesOpened = new int[1];
        JComponent content = AboutDialog.compactContent(
                () -> noticesOpened[0]++);
        JButton notices = button(content, "Full notices and licences");
        JButton close = button(content, "Close");
        assertNotNull(close, "a Close button exists with an accessible name");
        notices.doClick();
        assertEquals(1, noticesOpened[0],
                "the notices button runs exactly its action");
        // No chart types are reachable from the About surface at all:
        // the dialog depends only on AppInfo and packaged resources, so
        // chart state cannot change by construction.
    }

    private static List<String> labels(Component component) {
        List<String> texts = new ArrayList<>();
        collect(component, texts);
        return texts;
    }

    private static void collect(Component component, List<String> texts) {
        if (component instanceof JLabel label) {
            texts.add(label.getText());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, texts);
            }
        }
    }

    static JButton button(Component component, String accessibleName) {
        if (component instanceof JButton buttonComponent
                && accessibleName.equals(buttonComponent
                        .getAccessibleContext().getAccessibleName())) {
            return buttonComponent;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = button(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
