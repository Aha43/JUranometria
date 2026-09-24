package juranometria.project;

/**
 * What a drawn page is called, in somebody's language
 * (Sprint 33, issue #350).
 *
 * <p>A page says things. It says them in its title block, it says
 * them to a screen reader, and it says them again inside every file
 * the reader exports - the SVG's {@code <title>} and {@code <desc>},
 * the PDF's information dictionary, the PNG's text chunks. Until this
 * issue all of it was English written into {@code ChartRenderer},
 * {@code DrawnPage}, {@code SheetMetadata} and {@code PaperSize}: four
 * owners, one of them the renderer itself, none of them able to say
 * anything else.
 *
 * <p><strong>There is no implementation here, and there must not
 * be.</strong> No constant, no factory, no fallback, not one string.
 * An English implementation in this package would put English back
 * behind the boundary the whole sprint is building, and it would
 * become the convenient default wearing a different spelling
 * (review). The only production implementation is
 * {@code juranometria.ui.language.PageText}, built from an
 * {@code InterfaceText} a caller states.
 *
 * <p><strong>Every number arrives as a {@code String}.</strong> The
 * caller formats it with {@code Locale.ROOT} before handing it over,
 * because a chart value is notation: a magnitude limit of {@code 6.0}
 * is {@code 6.0} to every reader, and {@code 6,0} in a title block
 * would be a different number to half of them. The type does not
 * prove the formatting - the interface/chart-language matrix and the
 * comma-format mutation do - but it removes every accidental route to
 * getting it wrong, because a language has no number to reformat.
 *
 * <p>Methods are <strong>whole sentences</strong>. A caller never
 * joins two of these together: where English needs a clause and
 * another language needs it somewhere else, that is a different
 * method, not a concatenation at the call site.
 */
public interface PageWords {

    // ---- the surface the page is drawn on ----------------------

    /**
     * What a screen reader calls the chart itself.
     *
     * <p>Here rather than in a second adapter so that the component
     * holding a page's words does not also have to hold a language:
     * one seam, and the thing drawn and the thing it is drawn on
     * cannot end up in different tongues.
     */
    String chartName();

    /** How a reader is told they can move and scale the chart. */
    String chartInstructions();

    // ---- the title block, drawn on the page --------------------

    /**
     * The line naming where the page is centred.
     *
     * @param centreRa right ascension, in the chart's own notation
     * @param centreDec declination, likewise
     */
    String titleCentre(String centreRa, String centreDec);

    /**
     * The line naming how wide the page is, how faint it goes, which
     * way is up, and what drew it.
     *
     * @param fieldDegrees the field width, formatted, degree sign
     *     included by the language
     * @param magnitude the limiting magnitude, formatted
     * @param projection the projection's reader name, from
     *     {@link #projection}
     */
    String titleFacts(String fieldDegrees, String magnitude,
                      String projection);

    /**
     * The separator between the facts of {@link #titleFacts}, as this
     * language writes it - where a fact line may be broken when the
     * page has less room than the line (#359 completion). It is the
     * language's because the facts are.
     */
    String titleFactSeparator();

    /** The heading over the magnitude key. */
    String magnitudeKeyHeading();

    // ---- the observer's directions, on the horizon (#359) ------

    /**
     * The letter drawn at a cardinal horizon mark.
     *
     * <p>A language owns its letters - N/E/S/W in English, N/\u00d8/S/V
     * in Norwegian Bokm\u00e5l - so they arrive through this seam and
     * never from the module that computed where the mark is.
     */
    String directionLetter(juranometria.chart.Cardinal direction);

    /**
     * What a screen reader calls the mark: full and explicit, such
     * as "North on your horizon", and never the drawn abbreviation.
     * The page's celestial north and the observer's N are different
     * norths, and only words keep them apart.
     */
    String directionSpoken(juranometria.chart.Cardinal direction);

    // ---- what a screen reader is told --------------------------

    /**
     * The whole page, spoken.
     *
     * <p>{@code bounded} selects a complete sentence rather than
     * appending one: a page whose sky has an edge says so, and a
     * language decides where that belongs rather than receiving it
     * glued on the end.
     */
    String spokenPage(String subject, String ra, String dec,
                      String fieldDegrees, String projection,
                      String magnitude, boolean bounded);

    // ---- what an exported file says about itself ---------------

    /**
     * The exported file's title.
     *
     * @param application the application's name - passed in, because
     *     this package must not reach up into
     *     {@code juranometria.app.AppInfo} to find it (review)
     */
    String sheetTitle(String application, String subject);

    /** The exported file's description. */
    String sheetDescription(String subject, String ra, String dec,
                            String fieldDegrees, String projection,
                            String magnitude, String paper,
                            String ground);

    /** Who drew the file, and out of what. */
    String producedBy(String applicationAndVersion,
                      String repositoryUrl);

    /** The paper, its size and its margins, as a sentence. */
    String paper(String identity, String wideMm, String highMm,
                 String marginMm, String chartWideMm,
                 String chartHighMm);

    // ---- identities that become words --------------------------

    /**
     * What a projection is called for a reader.
     *
     * <p>The id stays canonical and is never shown. An id this
     * implementation does not know <strong>must be refused</strong>,
     * not printed: a study-only projection that reached a reader's
     * title block as its own identity would be a defect wearing the
     * shape of a word. A study supplies its own wording by wrapping
     * an implementation, never by widening one.
     *
     * @throws IllegalStateException if the id has no reader name here
     */
    String projection(String projectionId);

    /**
     * What a chart's ground is called for a reader.
     *
     * <p>The token is the one the atlas stores, and it is not shown
     * raw. Refused rather than printed when unknown, for the same
     * reason as {@link #projection}.
     *
     * @throws IllegalStateException if the token has no reader phrase
     */
    String ground(String paletteToken);
}
