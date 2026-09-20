package juranometria.ui.language;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.AboutDialog;
import juranometria.app.LicensingMap;
import juranometria.app.AppInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * About's three kinds of words, held apart (Sprint 33, issue #350).
 *
 * <p>The claims that make the boundary real: that no bundled document
 * is altered by being shown, that a licensing summary is used whole or
 * not at all, and that a language which writes its own is held to the
 * same map the English one is - not to a weaker check that counts
 * keys.
 */
class AboutTextTest {

    private static final InterfaceText EN =
            InterfaceText.forLanguage("en");
    private static final AboutText SAID = AboutText.in(EN);

    @Test
    void everyShippedSummaryCarriesTheWholeLicensingMap() throws Exception {
        List<String> shipped = summariesInTheSourceTree();
        assertTrue(shipped.contains("en"),
                "the premise: the canonical English summary is in the"
                        + " tree - " + shipped);
        for (String tag : shipped) {
            LicensingMap.assertSummary(tag, AboutText
                    .in(InterfaceText.forLanguage(tag)).summary());
        }
    }

    /**
     * The consequence is a pinned sentence, not a shape.
     *
     * <p>Three fixtures, all run through the <em>same</em> contract
     * the shipping summaries go through, because a check that cannot
     * be made to fail has never been checked:
     *
     * <ul>
     *   <li>delete the consequence - a citation without a
     *       restriction;</li>
     *   <li><strong>reverse</strong> it - a fluent, well-formed
     *       paragraph that says the opposite, which is the dangerous
     *       one and which no length or shape rule can catch;</li>
     *   <li>swap a licence onto the wrong source.</li>
     * </ul>
     */
    @Test
    void aDeletedReversedOrSwappedStatementAllFailTheSameContract() {
        String canonical = AboutText.canonicalSummary();

        String deleted = canonical.replace("The complete\npackage may"
                + " therefore be used and redistributed"
                + " non-commercially\nonly, for as long as that data"
                + " is included.", "");
        assertTrue(!deleted.contains("non-commercial"),
                "the premise: the fixture really dropped it");
        assertThrows(AssertionError.class,
                () -> LicensingMap.assertSummary("en", deleted),
                "a summary that cites the licence and never states"
                        + " the consequence is refused");

        String reversed = canonical.replace("non-commercially\nonly,",
                "commercially or non-commercially,");
        assertTrue(reversed.length() > canonical.length() - 20,
                "the premise: the reversal is a full, fluent"
                        + " paragraph, not a truncation - " 
                        + reversed.length() + " against "
                        + canonical.length());
        assertThrows(AssertionError.class,
                () -> LicensingMap.assertSummary("en", reversed),
                "and a summary that says the opposite is refused by"
                        + " the same contract, which no rule about"
                        + " paragraph length could have caught");

        String swapped = canonical.replace("CC BY-SA 4.0",
                "BSD-3-Clause");
        assertThrows(AssertionError.class,
                () -> LicensingMap.assertSummary("en", swapped),
                "OpenNGC paired with the wrong licence is refused");
    }

    /**
     * A language may not ship a summary without an approved statement.
     *
     * <p>The friction is the point: adding a language to a legal
     * statement is a decision a person makes.
     */
    @Test
    void aLanguageWithNoApprovedConsequenceCannotShipASummary() {
        AssertionError thrown = assertThrows(AssertionError.class,
                () -> LicensingMap.assertSummary("xx-XX",
                        AboutText.canonicalSummary()));
        assertTrue(thrown.getMessage().contains("approved"),
                "and says why: " + thrown.getMessage());
    }

    /**
     * A language with no summary of its own gets English, whole.
     *
     * <p>Held against a resource set that <strong>definitely</strong>
     * has no localized document, not against a file that happens not
     * to exist yet: the moment Norwegian writes one, a test relying
     * on its absence would stop exercising fallback and would say so
     * to nobody (review).
     */
    @Test
    void aLanguageWithoutASummaryFallsBackToTheWholeEnglishDocument() {
        // Only the canonical English document exists here. Any
        // language's own summary is absent by construction.
        AboutText.Documents onlyCanonical = path ->
                AboutDialog.SUMMARY_RESOURCE.equals(path)
                        ? AboutText.PACKAGED.open(path) : null;

        AboutText said = AboutText.in(
                InterfaceText.forLanguage("nb-NO"), onlyCanonical);
        assertTrue(!said.hasOwnSummary(),
                "the premise: this resource set has no Norwegian"
                        + " summary");
        assertEquals(AboutText.canonicalSummary(), said.summary(),
                "a language with no summary of its own reads the"
                        + " canonical English document, entire");

        // And a language that HAS one reads its own, so the branch
        // above is a fallback rather than the only behaviour.
        String pretend = "Code: MIT.\n\nTycho-2: CC BY-NC 3.0 IGO,"
                + " non-commercially only.\n";
        AboutText.Documents withNorwegian = path ->
                path.endsWith("nb-NO" + AboutText.SUMMARY_SUFFIX)
                        ? new java.io.ByteArrayInputStream(
                                pretend.getBytes(StandardCharsets.UTF_8))
                        : AboutText.PACKAGED.open(path);
        assertEquals(pretend, AboutText.in(
                        InterfaceText.forLanguage("nb-NO"), withNorwegian)
                        .summary(),
                "and a language that ships one reads its own");
    }

    /**
     * Falling back by fragment instead of by document is refused.
     *
     * <p>The mutation the rule exists for: a summary that took its
     * first paragraph from one language and the rest from another
     * would read fluently and state a legal position nobody wrote. A
     * whole-document comparison is what makes that impossible to pass
     * off, so the comparison is asserted on the whole string.
     */
    @Test
    void aSummaryStitchedFromTwoLanguagesIsNotWhatFallbackMeans() {
        String canonical = AboutText.canonicalSummary();
        String[] paragraphs = canonical.split("\n\s*\n", 2);
        assertTrue(paragraphs.length == 2,
                "the premise: the canonical summary has more than one"
                        + " paragraph");
        String stitched = "Kode og dokumentasjon: MIT-lisens.\n\n"
                + paragraphs[1];
        assertNotEquals(canonical, stitched,
                "a document with one paragraph replaced is not the"
                        + " canonical document, and the equality the"
                        + " fallback test asserts is what says so");
        assertTrue(stitched.contains("CC BY-NC 3.0 IGO"),
                "even though the stitched version still carries the"
                        + " identifiers a weaker check would look for");
    }

    /**
     * The notices view is exactly headings and untouched documents.
     *
     * <p><strong>Reconstructed, not searched.</strong> This asked
     * whether each body appeared somewhere in the rendered text, and
     * a mutation that trimmed every document on its way to the reader
     * <em>passed</em> it: the renderer appends a blank line after
     * each body, so the untrimmed text was still a substring of the
     * trimmed text plus that newline. A containment check cannot see
     * a document being altered at its edges.
     *
     * <p>So the whole string is rebuilt from the parts the design
     * says it is made of - rule, translated heading, rule, the
     * packaged characters, a blank line - and compared entire. Now
     * trimming, reflowing or reordering anything fails.
     *
     * <p><strong>Character, not byte.</strong> Production decodes the
     * packaged bytes as UTF-8 and renders a {@code String}; what this
     * holds is that every character survives, in order. Calling a
     * string comparison "byte identity" would claim more than it
     * proves (review). The files' own bytes are pinned by the
     * repository, not by this.
     */
    @Test
    void theNoticesViewIsExactlyHeadingsAndUntouchedDocuments()
            throws Exception {
        assertEquals(7, AboutDialog.NOTICES.size(),
                "the premise: seven documents are bundled");
        String rule = "================================================\n";
        for (String tag : List.of("en", "nb-NO")) {
            InterfaceText words = InterfaceText.forLanguage(tag);
            AboutText said = AboutText.in(words);
            StringBuilder expected = new StringBuilder();
            int characters = 0;
            for (AboutDialog.Notice notice : AboutDialog.NOTICES) {
                byte[] packaged;
                try (var stream = AboutDialog.class.getResourceAsStream(
                        notice.resourcePath())) {
                    assertNotNull(stream,
                            notice.resourcePath() + " ships");
                    packaged = stream.readAllBytes();
                }
                String body = new String(packaged, StandardCharsets.UTF_8);
                characters += body.length();
                expected.append(rule).append(said.heading(notice))
                        .append('\n').append(rule).append('\n')
                        .append(body).append("\n\n");
            }
            assertTrue(characters > 25000,
                    "the premise: this is the whole corpus - "
                            + characters + " characters");
            assertEquals(expected.toString(),
                    AboutDialog.noticesText(words),
                    tag + ": the notices view is the headings this"
                            + " language gives and the documents"
                            + " exactly as they ship, and nothing"
                            + " else");
        }
    }

    /** An identity never stands in for a word a reader reads. */
    @Test
    void noReaderIsShownAnIdentityOrAResourcePath() {
        List<String> everything = new ArrayList<>(List.of(
                SAID.title(AppInfo.NAME), SAID.windowExplain(),
                SAID.description(), SAID.summaryName(),
                SAID.summaryExplain(), SAID.noticesButton(),
                SAID.noticesButtonName(), SAID.noticesButtonExplain(),
                SAID.noticesName(), SAID.noticesExplain(),
                SAID.closeButton(), SAID.closeExplain()));
        for (AboutDialog.Notice notice : AboutDialog.NOTICES) {
            everything.add(SAID.heading(notice));
        }
        assertEquals(19, everything.size(),
                "the premise: twelve UI strings and seven headings");

        for (String said : everything) {
            for (AboutDialog.Notice notice : AboutDialog.NOTICES) {
                assertTrue(!said.contains(notice.id()),
                        notice.id() + " is an identity and reached a"
                                + " reader: " + said);
                assertTrue(!said.contains(notice.resourcePath()),
                        "a resource path reached a reader: " + said);
            }
            assertTrue(!said.contains("/resources/"),
                    "no path fragment reaches a reader: " + said);
        }

        // The heading is the one place identity belongs, and it
        // arrives as an argument rather than glued to a word.
        assertEquals(AppInfo.NAME + " " + AppInfo.version(),
                SAID.heading(AppInfo.NAME, AppInfo.version()),
                "the name and version are AppInfo's, placed by the"
                        + " language");
        assertEquals("About " + AppInfo.NAME, SAID.title(AppInfo.NAME),
                "and the title is a pattern, not \"About \" + a name");
    }

    /** A bundled document with no heading fails rather than showing its id. */
    @Test
    void aNoticeWithNoHeadingIsRefusedRatherThanTitledByItsId() {
        AboutDialog.Notice invented =
                new AboutDialog.Notice("notInAnyPack", "/resources/icons/LICENSE");
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> SAID.heading(invented));
        assertTrue(thrown.getMessage().contains("notInAnyPack"),
                "and says which one: " + thrown.getMessage());
    }

    /** The four approved corrections, pinned as sentences. */
    @Test
    void theEnglishSaysWhatAboutActuallyShows() {
        assertEquals("The short form of the atlas's licensing. The"
                        + " bundled data and icon notices are behind"
                        + " the button below.", SAID.summaryExplain(),
                "it claimed \"the full texts\" were behind the button,"
                        + " and About repeats neither the MIT source"
                        + " licence nor the packaged runtime's");
        assertEquals("Opens the complete notices and licence texts for"
                        + " the data and icons bundled with the"
                        + " atlas.", SAID.noticesButtonExplain(),
                "narrowed from \"everything the atlas bundles\"");
        assertEquals("The notices and licence texts for the data and"
                        + " icons bundled with the atlas, in full.",
                SAID.noticesExplain(), "and the same narrowing again");
        assertEquals("Opens the window with the application's name,"
                        + " version and the software and data it is"
                        + " built on",
                EN.say("menu.about.explain"),
                "the menu said \"software\" for a surface that chiefly"
                        + " accounts for bundled data");
    }

    /**
     * The notice registry carries identity and a path, nothing else.
     *
     * <p>Read off the record's own components, so putting an English
     * heading back beside the path fails this whether or not anybody
     * remembers a test exists - the same hold the chart keyboard's
     * registry has.
     */
    @Test
    void theNoticeRegistryCarriesIdentityAndAPathAndNothingElse() {
        List<String> members = new ArrayList<>();
        for (var component
                : AboutDialog.Notice.class.getRecordComponents()) {
            members.add(component.getName());
        }
        assertEquals(List.of("id", "resourcePath"), members,
                "AboutDialog.Notice is identity and location. An"
                        + " English heading here is prose in an"
                        + " application registry, and the next"
                        + " consumer would read it (#350)");
    }

    /** Blank is not a name, and not a path. */
    @Test
    void aBlankIdentityOrPathIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new AboutDialog.Notice("  ",
                        "/resources/icons/LICENSE"),
                "a blank id would name a resource key nothing answers");
        assertThrows(IllegalArgumentException.class,
                () -> new AboutDialog.Notice("tabler", ""),
                "and a blank path would ship nothing");
    }

    /**
     * Which languages have a summary <em>in the source tree</em>.
     *
     * <p>Named for what it reads. It is not the classpath and not the
     * application image: that a Norwegian summary actually ships and
     * is selected from the image is a claim for packaged acceptance
     * to make, and this cannot make it (review).
     */
    private static List<String> summariesInTheSourceTree()
            throws Exception {
        List<String> tags = new ArrayList<>();
        tags.add("en");
        try (var files = Files.list(
                Path.of("src/resources/interface-language"))) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(AboutText.SUMMARY_SUFFIX)) {
                    tags.add(name.substring(0, name.length()
                            - AboutText.SUMMARY_SUFFIX.length()));
                }
            }
        }
        return tags;
    }



}
