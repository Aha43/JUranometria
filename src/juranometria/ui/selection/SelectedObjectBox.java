package juranometria.ui.selection;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.Furniture;
import juranometria.page.PageEntry;
import juranometria.ui.language.InterfaceText;

/**
 * The chart's explanation of the last selected object (#692, ruled C-7 to
 * C-14 on #690, docs/decisions/guide-and-explain.md §5): one box of chart
 * furniture describing the working selection's lead, offered through the
 * chart's own typed furniture seam, so its corner, ink, label yielding,
 * speech and paper are the chart's.
 *
 * <p>It is offered exactly while the switch is on, the chart is not clean,
 * and the lead belongs on the current page - drawn, or marked with the
 * working cross because an option, the limit or the field keeps it from
 * being drawn. Off the page there is no box; back on the page it returns.
 * An untouched chart has no lead, so it is the released page.
 */
public final class SelectedObjectBox implements ChartModule {

    private final SelectedObjectSwitch shown;
    private final Predicate<String> belowHorizon;
    private final InterfaceText said;
    private ChartServices services;
    private Runnable withdraw = () -> { };
    private Runnable unfollow = () -> { };
    private boolean chromeShown = true;

    /**
     * @param belowHorizon whether the page dimmed a body below the drawn
     *     horizon in its last paint, by identity - the page's own answer
     */
    public SelectedObjectBox(SelectedObjectSwitch shown, Predicate<String> belowHorizon,
                             InterfaceText said) {
        if (shown == null || belowHorizon == null || said == null) {
            throw new IllegalArgumentException("a switch, the page's horizon and words are required");
        }
        this.shown = shown;
        this.belowHorizon = belowHorizon;
        this.said = said;
    }

    @Override
    public String name() {
        return SelectedObjectFacts.IDENTITY;
    }

    @Override
    public void attach(ChartServices services) {
        this.services = services;
        withdraw = services.furnish(SelectedObjectFacts.IDENTITY,
                () -> offered().map(List::of).orElse(List.of()), action -> { });
        unfollow = shown.follow(on -> services.redraw());
    }

    @Override
    public void detach() {
        unfollow.run();
        withdraw.run();
        services = null;
    }

    /**
     * Clean chart (C-12): hidden with the toolbar, restored with it; the
     * selection and the switch are untouched.
     */
    public void chromeShown(boolean shownNow) {
        if (chromeShown == shownNow) {
            return;
        }
        chromeShown = shownNow;
        if (services != null) {
            services.redraw();
        }
    }

    /** The box the page would draw now, if any. */
    public Optional<Furniture> offered() {
        if (services == null || !shown.showing() || !chromeShown) {
            return Optional.empty();
        }
        String lead = services.workingSelection().lead();
        if (lead == null || services.inventory() == null) {
            return Optional.empty();
        }
        Optional<PageEntry> entry = services.inventory().entries().stream()
                .filter(e -> e.identity().equals(lead)).findFirst();
        return entry.flatMap(e -> SelectedObjectFacts.of(e, belowHorizon.test(lead)))
                .map(facts -> SelectedObjectFacts.box(facts, said));
    }
}
