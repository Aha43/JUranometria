package juranometria.ui.bookmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import juranometria.bookmark.Node;
import juranometria.bookmark.Subject;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.meridian.MeridianModule;
import juranometria.page.LeadSelection;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageVisibility;
import juranometria.sky.Observer;
import juranometria.ui.ChartViewController;
import juranometria.ui.solar.BodyOnChart;

/**
 * Opens a bookmark by the rules ruled on #497 (2026-11-02;
 * docs/decisions/bookmarks.md, section 4), through the atlas's own
 * routes and no parallel state:
 *
 * <ol>
 * <li>A stored place and a stored instant go into the chart's one
 *     context in a single installation, built from the current observer
 *     so that an absent part stays exactly as it was. The place is the
 *     chart's for this session only; it is never written to the reader's
 *     Place and Time preference (C-2).</li>
 * <li>Each Solar System subject's switch is turned on if it is off, and
 *     remembered, as Centre on chart does; a moon is drawn with its
 *     planet (C-3).</li>
 * <li>The view opens through the controller's own recenter, which keeps
 *     the reader's magnitude limit (lowering it only by the field's own
 *     rule). A star or deep-sky primary opens as the page's named target,
 *     so it is drawn whatever the current options or limit say (C-3). A
 *     closer field opens only where the page offers it, otherwise at 1°.</li>
 * <li>The chart is painted, so the bodies of the restored instant are on
 *     the page, and the primary - if it resolves there - becomes the
 *     selection. With no primary the selection is not touched; an
 *     unresolved primary selects nothing in its place.</li>
 * <li>Every subject is reported: drawn, hidden by the current options,
 *     fainter than the limit, not on this page, not drawn at this
 *     instant, or not found in this catalogue - its stored identity kept
 *     as it is either way.</li>
 * </ol>
 *
 * Nothing else is touched: no Chart Option, grid, label, emphasis or
 * layer other than a subject's own switch.
 */
public final class BookmarkOpener {

    /** What became of one subject when the bookmark opened. */
    public enum Outcome {
        DRAWN,
        HIDDEN_BY_OPTIONS,
        FAINTER_THAN_LIMIT,
        NOT_ON_THIS_PAGE,
        NOT_DRAWN_NOW,
        NOT_IN_CATALOGUE
    }

    /** One subject's outcome. */
    public record SubjectReport(Subject subject, Outcome outcome) {
    }

    /**
     * What opening did.
     *
     * @param subjects       each subject's outcome, in the bookmark's order
     * @param selected       whether the primary became the selection
     * @param closerRefused  whether a closer field was not offered and 1° opened instead
     * @param switchedOn     the Solar System switches turned on to show subjects
     */
    public record Report(List<SubjectReport> subjects, boolean selected, boolean closerRefused,
                         List<String> switchedOn) {
        public Report {
            subjects = List.copyOf(subjects);
            switchedOn = List.copyOf(switchedOn);
        }
    }

    private final ChartViewController controller;
    private final MeridianModule meridian;
    private final WorkingSelection working;
    private final Supplier<PageContents> page;
    private final Runnable paintNow;
    private final Predicate<ChartViewState> closerOffered;
    private final Map<String, BodyOnChart> switches;
    private final Function<Subject, Optional<String>> catalogueLabel;
    private final Runnable enterWholeSky;

    /**
     * @param switches       each planet's or luminary's on-the-chart switch, by its body identity
     * @param catalogueLabel a star's or deep-sky object's label, if the catalogue holds it
     */
    public BookmarkOpener(ChartViewController controller, MeridianModule meridian,
                          WorkingSelection working, Supplier<PageContents> page, Runnable paintNow,
                          Predicate<ChartViewState> closerOffered, Map<String, BodyOnChart> switches,
                          Function<Subject, Optional<String>> catalogueLabel) {
        this(controller, meridian, working, page, paintNow, closerOffered, switches,
                catalogueLabel, null);
    }

    /**
     * The same, able to open a whole-sky bookmark (#687, R-11).
     *
     * @param enterWholeSky the whole-sky switch's entry, run after the
     *     bookmark's time and place are the chart's; null refuses such a
     *     bookmark loudly
     */
    public BookmarkOpener(ChartViewController controller, MeridianModule meridian,
                          WorkingSelection working, Supplier<PageContents> page, Runnable paintNow,
                          Predicate<ChartViewState> closerOffered, Map<String, BodyOnChart> switches,
                          Function<Subject, Optional<String>> catalogueLabel,
                          Runnable enterWholeSky) {
        this.enterWholeSky = enterWholeSky;
        this.controller = controller;
        this.meridian = meridian;
        this.working = working;
        this.page = page;
        this.paintNow = paintNow;
        this.closerOffered = closerOffered;
        this.switches = Map.copyOf(switches);
        this.catalogueLabel = catalogueLabel;
    }

    /** Opens the bookmark; on the event thread. */
    public Report open(Node.Bookmark bookmark) {
        // 1. The chart's one context, in one installation; never PlaceStore.
        Observer current = meridian.observer();
        Observer next = current;
        if (bookmark.place().isPresent()) {
            next = next.from(bookmark.place().get().latitudeDegrees(),
                    bookmark.place().get().eastLongitudeDegrees());
        }
        if (bookmark.time().isPresent()) {
            next = next.at(bookmark.time().get().instant());
        }
        if (!next.equals(current)) {
            meridian.observer(next);
        }

        // 2. The Solar System subjects' switches.
        List<String> switchedOn = new ArrayList<>();
        for (Subject s : bookmark.subjects()) {
            if (s.kind() != Subject.Kind.BODY) {
                continue;
            }
            String planet = planetOf(s.id());
            BodyOnChart layer = switches.get(planet);
            if (layer != null && !layer.showing()) {
                layer.show(true);
                switchedOn.add(planet);
            }
        }

        // 3. The view, through recenter; the primary as the named target.
        // A whole-sky bookmark enters the whole sky at the time and place
        // just installed (#687, R-11); it has no centre to recentre on.
        boolean closerRefused = false;
        if (bookmark.view().wholeSky()) {
            if (enterWholeSky == null) {
                throw new IllegalStateException(
                        "a whole-sky bookmark needs the whole-sky switch to open");
            }
            enterWholeSky.run();
            return finish(bookmark, closerRefused, switchedOn);
        }
        SkyPosition centre = new SkyPosition(bookmark.view().raDegrees(), bookmark.view().decDegrees());
        double field = bookmark.view().fieldWidthDegrees();
        if (field < ChartViewState.normalMinimumFieldDegrees()) {
            ChartViewState candidate = new ChartViewState(centre, field,
                    controller.state().limitingMagnitude(), null, null);
            if (!closerOffered.test(candidate)) {
                field = ChartViewState.normalMinimumFieldDegrees();
                closerRefused = true;
            }
        }
        Optional<Subject> primary = bookmark.primary();
        Optional<String> targetLabel = primary
                .filter(p -> p.kind() != Subject.Kind.BODY)
                .flatMap(catalogueLabel);
        if (targetLabel.isPresent()) {
            controller.recenter(centre, field, targetLabel.get(), primary.get().id());
        } else {
            controller.recenter(centre, field);
        }

        return finish(bookmark, closerRefused, switchedOn);
    }

    /** Steps 4 and 5: the paint, the primary, and the report. */
    private Report finish(Node.Bookmark bookmark, boolean closerRefused, List<String> switchedOn) {
        Optional<Subject> primary = bookmark.primary();
        // 4. Paint, so the restored instant's bodies are on the page; then the primary.
        paintNow.run();
        PageContents contents = page.get();
        boolean selected = false;
        if (primary.isPresent()) {
            String id = primary.get().id();
            if (LeadSelection.resolve(contents, id).isPresent()) {
                working.replaceWith(List.of(id), id);
                selected = true;
            }
        }

        // 5. The report.
        List<SubjectReport> reports = new ArrayList<>();
        for (Subject s : bookmark.subjects()) {
            reports.add(new SubjectReport(s, outcome(s, contents)));
        }
        return new Report(reports, selected, closerRefused, switchedOn);
    }

    private Outcome outcome(Subject s, PageContents contents) {
        Optional<PageEntry> entry = contents.find(s.id());
        if (entry.isPresent()) {
            PageVisibility visibility = switch (entry.get()) {
                case PageEntry.DeepSky d -> d.visibility();
                case PageEntry.StarEntry st -> st.visibility();
                default -> PageVisibility.DRAWN;
            };
            return switch (visibility) {
                case DRAWN -> Outcome.DRAWN;
                case BELOW_LIMIT -> Outcome.FAINTER_THAN_LIMIT;
                case FAMILY_HIDDEN, NO_SYMBOL, TOO_SMALL -> Outcome.HIDDEN_BY_OPTIONS;
            };
        }
        if (s.kind() == Subject.Kind.BODY) {
            return Outcome.NOT_DRAWN_NOW;
        }
        return catalogueLabel.apply(s).isPresent() ? Outcome.NOT_ON_THIS_PAGE : Outcome.NOT_IN_CATALOGUE;
    }

    /** The switch a body is drawn by: its own, or its planet's for a moon. */
    static String planetOf(String bodyIdentity) {
        if (bodyIdentity.startsWith("jovian.")) {
            return "jovian.jupiter";
        }
        int dot = bodyIdentity.indexOf('.');
        return dot < 0 ? bodyIdentity : bodyIdentity.substring(0, dot);
    }
}
