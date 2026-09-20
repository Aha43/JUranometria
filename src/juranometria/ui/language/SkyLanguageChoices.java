package juranometria.ui.language;

import java.util.ArrayList;
import java.util.List;

/**
 * What a language selector offers, and what each item says
 * (Sprint 33, issue #348).
 *
 * <p>Built from the two registries rather than written down, so a
 * language that ships appears without an edit here. The labels are
 * computed too: {@code Follow interface} says what it currently
 * resolves to, because with one interface language installed it draws
 * exactly the same page as {@code Latin (IAU)} and a reader offered
 * two identical-looking choices deserves to be told why.
 *
 * <p><strong>Follow is offered, never collapsed into Latin.</strong>
 * It would be tempting to hide an item that behaves identically
 * today. It would also be a silent migration: a reader whose stored
 * choice is {@code follow-interface} has asked for a living
 * relationship to their interface language, and opening Settings and
 * pressing OK would convert that into a fixed {@code latin} they
 * never chose. The gate kept absence, follow and explicit Latin
 * apart; a selector that cannot show one of them cannot preserve it.
 */
public final class SkyLanguageChoices {

    private SkyLanguageChoices() {
    }

    /** One item in a selector: what is stored, and what is shown. */
    public record Item(String token, String label) {

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * The chart-name choices, in the order a selector lists them.
     *
     * <p>Follow first, because it is the one that defers rather than
     * decides; then Latin, which every build can draw; then the
     * installed packs in their own sorted order.
     *
     * @param names        the chart registry, for display names
     * @param available    what this build can offer
     * @param interfaceLanguage what Follow would currently follow
     */
    public static List<Item> forTheChart(
            juranometria.geo.SkyNames names,
            SkyLanguageChoice.Available available,
            String interfaceLanguage,
            InterfaceText said) {
        List<Item> items = new ArrayList<>();
        // One pattern with a numbered argument, not a sentence glued
        // to a name: Norwegian may want the language elsewhere in the
        // phrase, and only the translation can decide that (#350).
        items.add(new Item(SkyLanguageChoice.FOLLOW,
                said.say("settings.language.chart.follow",
                        displayed(names, said,
                                followWouldDraw(available,
                                        interfaceLanguage)))));
        items.add(new Item(SkyLanguageChoice.LATIN,
                said.say("settings.language.chart.latin")));
        for (String tag : available.chartLanguages().stream().sorted()
                .toList()) {
            items.add(new Item(tag, names.displayName(tag)));
        }
        return List.copyOf(items);
    }

    /**
     * The interface choices, from the interface registry alone.
     *
     * <p>One item today, and deliberately still a selector: a control
     * populated from the real registry proves the registry feeds the
     * interface, which a hard-coded "English" label would not.
     */
    public static List<Item> forTheInterface(
            InterfaceLanguages interfaces) {
        List<Item> items = new ArrayList<>();
        for (String tag : interfaces.tags()) {
            items.add(new Item(tag, interfaces.displayName(tag)));
        }
        return List.copyOf(items);
    }

    /**
     * What {@code follow-interface} draws today.
     *
     * <p>Computed from the installation, never assumed. When a
     * Norwegian interface exists this returns {@code nb-NO} and the
     * item reads "currently Norsk bokmal" - with the stored token
     * unchanged, which is the whole point of following.
     */
    public static String followWouldDraw(
            SkyLanguageChoice.Available available,
            String interfaceLanguage) {
        return available.chartLanguages().contains(interfaceLanguage)
                ? interfaceLanguage
                : SkyLanguageChoice.LATIN;
    }

    /** The item a stored token selects. */
    public static Item selected(List<Item> items, String token) {
        for (Item item : items) {
            if (item.token().equals(token)) {
                return item;
            }
        }
        // A token this build cannot offer resolves the same way the
        // store resolves it, so the selector shows what the reader is
        // actually getting rather than an empty box.
        for (Item item : items) {
            if (item.token().equals(SkyLanguageChoice.LATIN)) {
                return item;
            }
        }
        return items.get(0);
    }

    private static String displayed(juranometria.geo.SkyNames names,
                                    InterfaceText said, String token) {
        return SkyLanguageChoice.LATIN.equals(token)
                ? said.say("settings.language.chart.latin")
                : names.displayName(token);
    }
}
