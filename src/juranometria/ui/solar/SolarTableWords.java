package juranometria.ui.solar;

import juranometria.ui.language.InterfaceText;

/**
 * The words a Solar System table says, resolved body first (Sprint 36,
 * issue #408).
 *
 * <p>The Sun's and the Moon's tables share one shell - the observer
 * note, the two views, the range and its refusals, the marks on an
 * instant, the shared column headings - and differ in what only a body
 * can say: its name, what its distance is measured in, what its lit
 * side means. So there are two key families. {@code solartable.*}
 * holds the shared words once; {@code suntable.*} and
 * {@code moontable.*} hold each body's own. A word is looked up under
 * the body's stem first and under the shared stem when the body has
 * nothing of its own to say, so a body-specific key is an override
 * rather than a copy, and the two languages stay complete together.
 */
public final class SolarTableWords {

    static final String SHARED = "solartable";

    private final InterfaceText said;
    private final String stem;
    private final String decimal;

    public SolarTableWords(InterfaceText said, String stem) {
        if (said == null || stem == null || stem.isBlank()) {
            throw new IllegalArgumentException("a table says its words in"
                    + " some language, for some body");
        }
        this.said = said;
        this.stem = stem;
        this.decimal = say("decimal");
    }

    /** The language itself, for what is not a table word. */
    public InterfaceText said() {
        return said;
    }

    /** The body's own key family. */
    public String stem() {
        return stem;
    }

    /** The key a suffix resolves to: the body's if defined, else the shared. */
    public String key(String suffix) {
        String own = stem + "." + suffix;
        if (said.sayIfDefined(own) != null) {
            return own;
        }
        return SHARED + "." + suffix;
    }

    /** Whether either family defines the suffix. */
    public boolean defines(String suffix) {
        return said.sayIfDefined(key(suffix)) != null;
    }

    /** The word, with its values filled in where the pattern takes any. */
    public String say(String suffix, Object... values) {
        String key = key(suffix);
        return values.length == 0 ? said.say(key) : said.say(key, values);
    }

    /** The word if either family defines it, else null. */
    public String sayIfDefined(String suffix) {
        return defines(suffix) ? say(suffix) : null;
    }

    /** The language's decimal separator for what the reader sees. */
    public String decimal() {
        return decimal;
    }

    /** A {@code Locale.ROOT} number respelled with the language's separator. */
    public String n(String rootNumber) {
        return SunTableFormat.decimal(rootNumber, decimal);
    }
}
