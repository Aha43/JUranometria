package juranometria.ui.language;

import juranometria.project.PageWords;

/**
 * The words a drawn page says, in the reader's language
 * (Sprint 33, issue #350).
 *
 * <p>The only production implementation of {@link PageWords}. It is
 * built from an {@code InterfaceText} a caller states, and there is
 * no other way to build one: the interface it implements carries no
 * factory and no English, so the language a page speaks is always a
 * decision somebody made rather than a default somebody inherited.
 *
 * <p><strong>Where this is resolved.</strong> Twice in the whole
 * application. {@code JUranometriaMain} derives it from the session
 * for the screen; {@code ExportSheet.write} derives it from the
 * {@code InterfaceText} it already has, and the <em>same instance</em>
 * travels through {@code ChartSheet.record} to both the renderer and
 * the sheet's metadata. Nothing downstream remembers a language, so
 * screen and file cannot disagree about what the page is called.
 *
 * <p><strong>Two identities become words here.</strong> A projection
 * and a palette are stored as canonical tokens - {@code gnomonic},
 * {@code white-paper} - and a reader is shown a phrase instead. Both
 * lookups <strong>refuse an unknown token</strong> rather than
 * printing it: an id that reached a title block would look like a
 * word to everything except a reader.
 *
 * <p>The English phrase for {@code white-paper} is, deliberately,
 * {@code white-paper}. Every exported file the atlas has ever written
 * says {@code Ground: white-paper.}, and this commit is a seam rather
 * than an output change; Norwegian says {@code hvitt papir}, which is
 * what makes the mapping real rather than an identity function.
 * Changing the English typography is a separate, reviewed decision.
 */
public final class PageText implements PageWords {

    private static final String STEM = "page.";

    private final InterfaceText said;

    private PageText(InterfaceText said) {
        this.said = said;
    }

    /** The page's words in a language a caller states. */
    public static PageText in(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "a page says what it says in some language");
        }
        return new PageText(said);
    }

    /** The language these words are in, for callers that pass it on. */
    public InterfaceText words() {
        return said;
    }

    @Override
    public String chartName() {
        return said.say(STEM + "chart.a11y");
    }

    @Override
    public String chartInstructions() {
        return said.say(STEM + "chart.explain");
    }

    @Override
    public String titleCentre(String centreRa, String centreDec) {
        return said.say(STEM + "title.centre", centreRa, centreDec);
    }

    @Override
    public String titleFacts(String fieldDegrees, String magnitude,
                             String projection) {
        return said.say(STEM + "title.facts", fieldDegrees, magnitude,
                projection);
    }

    @Override
    public String magnitudeKeyHeading() {
        return said.say(STEM + "magnitudeKey.heading");
    }

    @Override
    public String spokenPage(String subject, String ra, String dec,
                             String fieldDegrees, String projection,
                             String magnitude, boolean bounded) {
        return said.say(STEM + (bounded ? "spoken.bounded" : "spoken"),
                subject, ra, dec, fieldDegrees, projection, magnitude);
    }

    @Override
    public String sheetTitle(String application, String subject) {
        return said.say(STEM + "sheet.title", application, subject);
    }

    @Override
    public String sheetDescription(String subject, String ra, String dec,
                                   String fieldDegrees, String projection,
                                   String magnitude, String paper,
                                   String ground) {
        return said.say(STEM + "sheet.description", subject, ra, dec,
                fieldDegrees, projection, magnitude, paper, ground);
    }

    @Override
    public String producedBy(String applicationAndVersion,
                             String repositoryUrl) {
        return said.say(STEM + "sheet.producedBy", applicationAndVersion,
                repositoryUrl);
    }

    @Override
    public String paper(String identity, String wideMm, String highMm,
                        String marginMm, String chartWideMm,
                        String chartHighMm) {
        return said.say(STEM + "paper", identity, wideMm, highMm,
                marginMm, chartWideMm, chartHighMm);
    }

    @Override
    public String projection(String projectionId) {
        return required("projection", projectionId);
    }

    @Override
    public String ground(String paletteToken) {
        return required("ground", paletteToken);
    }

    /**
     * An identity's reader phrase, or a refusal.
     *
     * <p>{@code InterfaceText} answers an unknown key with the key,
     * which is right for a surface that would rather show something
     * than nothing and wrong here: these are a closed set a test can
     * walk, and {@code page.projection.spherical} drawn on a chart is
     * a defect shaped like a word.
     */
    private String required(String kind, String identity) {
        if (identity == null || identity.isBlank()) {
            throw new IllegalStateException("a page cannot name a "
                    + kind + " that has no identity");
        }
        String key = STEM + kind + "." + identity;
        String value = said.say(key);
        if (value == null || value.isBlank() || value.equals(key)) {
            throw new IllegalStateException("no reader name for the "
                    + kind + " \"" + identity + "\". Its identity is"
                    + " the atlas talking to itself, and a page that"
                    + " printed it would be showing a reader a token"
                    + " nobody wrote. A study with a projection of its"
                    + " own supplies the wording by wrapping this,"
                    + " never by widening it.");
        }
        return value;
    }
}
