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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void theMarkAppearsBesideTheNameWithoutDisplacingAnything()
            throws Exception {
        // Branding, added beside what About is for rather than in
        // place of it (#202): the version, the licensing summary and
        // the way to the notices all keep their room, and assistive
        // technology is told the application's name rather than the
        // position of three stars.
        JComponent content = AboutDialog.compactContent(() -> { });
        List<javax.swing.JLabel> marks = new ArrayList<>();
        collectIcons(content, marks);
        assertEquals(1, marks.size(),
                "one mark, beside the title: " + marks.size());
        assertEquals(48, marks.get(0).getIcon().getIconWidth(),
                "at the size About shows it");
        assertNull(marks.get(0).getAccessibleContext()
                        .getAccessibleDescription(),
                "decorative: assistive technology is not given a"
                        + " description of where three stars sit");
        assertFalse(marks.get(0).isFocusable(),
                "and a reader tabbing through does not stop on it");

        assertTrue(labels(content).stream().anyMatch(text ->
                        text.equals(AppInfo.NAME + " " + AppInfo.version())),
                "the version still titles the view");
        assertTrue(AboutDialog.summaryText().contains("CC BY-NC 3.0 IGO"),
                "and the licensing summary is untouched");
    }

    private static void collectIcons(Component component,
                                     List<javax.swing.JLabel> found) {
        if (component instanceof javax.swing.JLabel label
                && label.getIcon() != null) {
            found.add(label);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectIcons(child, found);
            }
        }
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
            {"star names|star-identity", "BSD-3-Clause"},
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
        assertPairings("the archive's packaging/LICENSING.md",
                Files.readString(Path.of("packaging/LICENSING.md")));
        // The practical consequence rides with the Tycho-2 pairing.
        assertTrue(paragraphWith(AboutDialog.summaryText(), "tycho")
                        .contains(normalize("non-commercially")),
                "the summary's Tycho paragraph states the consequence");
        assertTrue(paragraphWith(Files.readString(Path.of("LICENSING.md")),
                        "tycho")
                        .contains(normalize("may not be used commercially")),
                "LICENSING.md's Tycho entry states the consequence");
    }

    @Test
    void theBundledJavaRuntimeIsPairedWithItsLicenceAndItsArtifacts()
            throws Exception {
        // The audit added the runtime to the licensing map (issue
        // #145); without a guard the same drift returns with the
        // suite green (audit review, P2). Four of the five artifacts
        // ship a runtime, the portable archive ships none, and the
        // difference is part of the licensing statement.
        String map = Files.readString(Path.of("LICENSING.md"));
        String runtimeParagraph = paragraphWith(map, "temurin|openjdk");

        assertTrue(runtimeParagraph.contains(normalize(
                        "GPLv2 with the Classpath Exception")),
                "LICENSING.md must pair the bundled runtime with its"
                        + " licence: " + runtimeParagraph);
        assertTrue(map.toLowerCase(java.util.Locale.ROOT)
                        .contains("classpath exception is what keeps")
                        || map.contains("Classpath Exception"),
                "and say what that exception does for the MIT code");
        assertTrue(normalize(map).contains(normalize(
                        "The portable archive contains no runtime")),
                "and distinguish the self-contained images from the"
                        + " portable archive, which ships none");

        // The same statement travels with the artifact that actually
        // carries the runtime.
        String imageReadme = Files.readString(
                Path.of("packaging/README-app-image.txt"));
        assertTrue(imageReadme.contains("Temurin")
                        && imageReadme.contains("Classpath Exception"),
                "the application image's own README names the runtime"
                        + " licence it ships");

        // And the contract's deliberate division of surfaces holds:
        // runtime licences live with the runtime, never restated in
        // About's packaged summary.
        String summary = AboutDialog.summaryText().toLowerCase(
                java.util.Locale.ROOT);
        assertFalse(summary.contains("temurin")
                        || summary.contains("classpath exception"),
                "About carries what is packaged inside the"
                        + " application; the runtime's licence travels"
                        + " with the runtime");
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
        if (component instanceof JLabel label && label.getText() != null) {
            // A label may carry an icon and no words - the
            // application mark beside the title does (#202) - and a
            // wordless label has nothing to contribute here.
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
