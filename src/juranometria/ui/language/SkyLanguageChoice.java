package juranometria.ui.language;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * What a reader chose about language, and what silence means
 * (Sprint 33, issue #347).
 *
 * <p>Two independent choices, persisted as stable tokens:
 *
 * <pre>
 *   language.interface = en | nb-NO | …whatever is installed
 *   language.chart     = follow-interface | latin | …whatever pack is found
 * </pre>
 *
 * <p><strong>Availability is data, not a switch.</strong> An earlier
 * version of this class accepted only the tags compiled into it,
 * which quietly contradicted the contract it was written to hold: a
 * Swedish pack would have been refused until somebody edited Java,
 * so "a translation arrives as data" was true of the loader and
 * false of the setting. What is fixed here is only the two chart
 * <em>modes</em> that are not languages at all - {@link #FOLLOW} and
 * {@link #LATIN} - because those are behaviours of the atlas rather
 * than things a contributor supplies. Every actual language comes
 * from {@link Available}: installed interface resources, and
 * discovered validated packs.
 *
 * <p><strong>{@code follow-interface} is a value, not an absence.</strong>
 * A store that could not tell "never chose" from "chose to follow"
 * would have no way to change its default later without silently
 * moving the chart of a reader who never agreed to anything. Both
 * behave identically <em>today</em>, which is what makes the upgrade
 * from 2.0 invisible; they stay distinct so tomorrow has an honest
 * option.
 *
 * <p><strong>{@code latin} is a nomenclature mode.</strong> It selects
 * the official Latin names on the chart and says nothing about the
 * interface. It is deliberately not the language tag {@code la}: the
 * atlas does not speak Latin, it writes Latin names.
 *
 * <p>This began as a gate contract in {@code juranometria.tool},
 * where #347 settled what the tokens mean without changing any
 * reader behaviour. It stopped being tooling the moment
 * {@link SkyLanguageStore} persisted it: this is now the
 * application's language state, and production must not depend on
 * evidence code for it. Promoted here in #348, before more of the
 * application could take the dependency.
 */
public final class SkyLanguageChoice {

    /** The persisted key for the interface language. */
    public static final String INTERFACE_KEY = "language.interface";

    /** The persisted key for the chart language. */
    public static final String CHART_KEY = "language.chart";

    /** The tag the application's own resources ship in. */
    public static final String ENGLISH = "en";

    /**
     * The chart follows whatever the interface is.
     *
     * <p>A mode, not a language: no contributor supplies it and no
     * pack can be called it.
     */
    public static final String FOLLOW = "follow-interface";

    /**
     * The chart shows the official Latin names.
     *
     * <p>A mode, not a language, for the same reason. The Latin
     * names come from the IAU identity layer, which every build
     * carries whatever languages are installed.
     */
    public static final String LATIN = "latin";

    /** The two chart modes that are not languages. */
    public static final Set<String> CHART_MODES = Set.of(FOLLOW, LATIN);

    /**
     * What this installation can actually offer.
     *
     * <p>Interface languages are the resource bundles present; chart
     * languages are the packs discovered and validated by
     * {@link SkyLanguagePack}. Both are discovered, so adding either
     * is a matter of adding data - which is the whole contract. A
     * test registry may include the private fixture pack; a
     * production one never does, because the fixture is refused at
     * load.
     */
    public record Available(Set<String> interfaceLanguages,
                            Set<String> chartLanguages) {

        public Available {
            interfaceLanguages = Set.copyOf(interfaceLanguages);
            chartLanguages = Set.copyOf(chartLanguages);
            for (String mode : CHART_MODES) {
                if (chartLanguages.contains(mode)) {
                    throw new IllegalArgumentException(
                            "\"" + mode + "\" is a chart mode, not a"
                                    + " language a pack may claim");
                }
            }
            if (!interfaceLanguages.contains(ENGLISH)) {
                // English is the fallback every other language falls
                // back to. An installation without it has nowhere to
                // land when a translation is incomplete.
                throw new IllegalArgumentException(
                        "the interface fallback " + ENGLISH
                                + " is always installed");
            }
        }

        /** Whether this value may be stored under this key. */
        public boolean offers(String key, String value) {
            if (value == null) {
                return false;
            }
            return switch (key) {
                case INTERFACE_KEY -> interfaceLanguages.contains(value);
                case CHART_KEY -> CHART_MODES.contains(value)
                        || chartLanguages.contains(value);
                default -> false;
            };
        }

        /** Every value a reader could choose for the chart. */
        public Set<String> chartChoices() {
            Set<String> choices = new LinkedHashSet<>(CHART_MODES);
            choices.addAll(chartLanguages);
            return choices;
        }
    }

    private final String interfaceLanguage;

    private final String chartLanguage;


    private final Available available;

    private SkyLanguageChoice(String interfaceLanguage,
                              String chartLanguage,
                              boolean interfaceChosen,
                              boolean chartChosen,
                              Available available) {
        this.interfaceLanguage = interfaceLanguage;
        this.chartLanguage = chartLanguage;
        this.interfaceChosen = interfaceChosen;
        this.chartChosen = chartChosen;
        this.available = available;
    }

    /**
     * Whether the reader has settled the interface question.
     *
     * <p><strong>Consent is per key</strong>, because the two
     * settings are two questions. A reader who chooses Norwegian
     * constellation names has answered the sky's question; they have
     * not chosen English for the application merely because English
     * is the fallback displayed beside it. Opening Settings and
     * confirming something else is not an answer either.
     *
     * <p>This decides what is written down. An upgrading reader has
     * no interface key, because the setting did not exist when they
     * last used the atlas, and {@code SkyLanguageStore} is careful
     * not to answer that silence when it reads - "the moment a read
     * writes a default, the reader's silence has been answered for
     * them and can never be migrated". A confirmation that wrote
     * {@code en} would answer it just as finally.
     *
     * <p>Set by an explicit act - {@link #withInterface} - and never
     * inferred from the value. Choosing the English already showing
     * IS a choice, and must be recorded as one; it simply changes
     * nothing a reader can see.
     */
    private final boolean interfaceChosen;

    /** The same question, asked of the sky's names. */
    private final boolean chartChosen;

    /** Whether an interface language was ever actually chosen. */
    public boolean interfaceChosen() {
        return interfaceChosen;
    }

    /** Whether a chart language was ever actually chosen. */
    public boolean chartChosen() {
        return chartChosen;
    }

    /**
     * What a store says, with absence and nonsense resolved.
     *
     * <p>Reading resolves; it does not write. A 2.0 reader who never
     * opens the setting keeps a store with no language keys in it,
     * and that is the point: absence today means "never asked", and
     * a future default change must migrate it explicitly rather than
     * quietly reinterpret it.
     *
     * <p>A value this installation cannot offer is replaced
     * deterministically - never passed to {@code Locale.getDefault()}
     * and never accepted as an arbitrary language tag. The audit
     * found no {@code Locale.getDefault()} in production at all, and
     * that has to survive this sprint.
     */
    public static SkyLanguageChoice read(Map<String, String> stored,
                                         Available available) {
        String statedInterface = stored.get(INTERFACE_KEY);
        String statedChart = stored.get(CHART_KEY);
        return new SkyLanguageChoice(
                available.offers(INTERFACE_KEY, trim(statedInterface))
                        ? trim(statedInterface) : ENGLISH,
                available.offers(CHART_KEY, trim(statedChart))
                        ? trim(statedChart) : FOLLOW,
                statedInterface != null, statedChart != null,
                available);
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }

    /** Whether either key was stored at all. */
    public boolean everChosen() {
        // Derived, not stored. Its only valid value is the OR of the
        // two per-key facts, and a field beside them is a state the
        // constructor can build and the model says cannot exist.
        return interfaceChosen || chartChosen;
    }

    public String interfaceLanguage() {
        return interfaceLanguage;
    }

    public String chartLanguage() {
        return chartLanguage;
    }

    /** What this installation can offer. */
    public Available available() {
        return available;
    }

    /**
     * Which names the chart actually draws, following if asked to.
     *
     * <p>English maps to Latin nomenclature, which preserves 2.0's
     * page exactly for a reader who upgrades and changes nothing.
     *
     * <p>Following asks for the interface's language <em>if the sky
     * can be drawn in it</em>. An installation may carry a Swedish
     * interface and no Swedish constellation pack - the two sets are
     * separate precisely because neither implies the other - and
     * returning a tag no pack answers to would have named a language
     * that is not there. It falls back to the official Latin names,
     * which every build carries whatever else is installed.
     */
    public String namesOnTheChart() {
        if (!FOLLOW.equals(chartLanguage)) {
            return chartLanguage;
        }
        if (ENGLISH.equals(interfaceLanguage)) {
            return LATIN;
        }
        return available.chartLanguages().contains(interfaceLanguage)
                ? interfaceLanguage : LATIN;
    }

    /**
     * What the reader has actually settled, for saving.
     *
     * <p><strong>Each key is written only if that question was
     * answered.</strong> This once said "both keys, always", on the
     * reasoning that a half-written choice would leave the other an
     * absence and absence is reserved to mean "never asked". The
     * reasoning was right about what absence means and wrong about
     * who had answered: confirming a dialog is not acting on every
     * control in it, and under the old rule a reader upgrading from
     * 2.0.0 who opened Settings and pressed OK was recorded as having
     * chosen English - the one thing the absence exists to prevent.
     *
     * <p>So an unwritten key still means "never asked", and now it
     * is true per question rather than per dialog.
     */
    public Map<String, String> toStore() {
        // Only what has been settled. A key left out is a reader who
        // has not answered that question, which stays true until
        // they do - and stays migratable.
        Map<String, String> stored = new LinkedHashMap<>();
        if (interfaceChosen) {
            stored.put(INTERFACE_KEY, interfaceLanguage);
        }
        if (chartChosen) {
            stored.put(CHART_KEY, chartLanguage);
        }
        return stored;
    }

    /**
     * The same choice with a different interface language.
     *
     * <p>Refused if this installation does not offer it. The
     * malformed-store tests only ever exercised reading, so an
     * invalid choice could previously be built through this door and
     * then persisted by {@link #toStore()} - a store validated on
     * the way in and not on the way out.
     */
    public SkyLanguageChoice withInterface(String language) {
        // An explicit act, so it settles the interface question -
        // even when the value chosen is the fallback already
        // showing. The chart question is left exactly as it was.
        return chosen(INTERFACE_KEY, language,
                new SkyLanguageChoice(language, chartLanguage,
                        true, chartChosen, available));
    }

    /** The same choice with a different chart language or mode. */
    public SkyLanguageChoice withChart(String language) {
        // And the mirror of it: settling the sky's names says
        // nothing about the application's own words.
        return chosen(CHART_KEY, language,
                new SkyLanguageChoice(interfaceLanguage, language,
                        interfaceChosen, true, available));
    }

    private SkyLanguageChoice chosen(String key, String value,
                                     SkyLanguageChoice made) {
        if (!available.offers(key, value)) {
            throw new IllegalArgumentException(
                    "this installation cannot offer " + key + "=\""
                            + value + "\"; it has "
                            + (INTERFACE_KEY.equals(key)
                                    ? available.interfaceLanguages()
                                    : available.chartChoices()));
        }
        return made;
    }
}
