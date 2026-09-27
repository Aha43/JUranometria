package juranometria.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartStructure;
import juranometria.sky.Observer;

/**
 * The view report's words, order and determinism (#372).
 *
 * <p>The formatter is held here on constructed states; that the
 * snapshot is assembled from the live owners is a separate contract.
 * The first test freezes the vocabulary and order the owner reviewed:
 * a change to it is a decision, not maintenance.
 */
class ViewReportTest {

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));

    /** A deliberately non-default state: every field has a premise. */
    static ViewReport.Snapshot rich() {
        ChartOptions chosen = ChartOptions.DEFAULTS
                .withPalette(ChartPalette.BLACK_SKY);
        return new ViewReport.Snapshot("2.3.0",
                new ChartViewState(new SkyPosition(331.98, 39.97), 60.0,
                        6.0, "M31 \u00b7 Andromeda Galaxy region", null),
                1180, 760, 1180, 760, 0, 0,
                new ViewReport.Scale(2.0, 2.0), chosen, chosen,
                "nb-NO", "follow-interface", "nb-NO",
                EnumSet.of(ChartStructure.MERIDIAN,
                        ChartStructure.HORIZON,
                        ChartStructure.EQUATORIAL_GRID),
                List.of("place-and-time", "ecliptic"),
                new ViewReport.PlaceAndTime(OSLO, true, true, false),
                Boolean.FALSE,
                new Selection.Object(Selection.Object.Kind.STAR,
                        "star:hip-102098",
                        new SkyPosition(310.36, 45.28)),
                List.of("star:hip-102098", "dso:ngc-7000"),
                "star:hip-102098",
                "TestOS", "9.9", "x9", "21.0.99");
    }

    @Test
    void theReportSaysTheStateInTheReviewedWordsAndOrder() {
        assertEquals("""
                JUranometria view report
                version: 2.3.0
                centre: RA 22h 07.9m; Dec +39° 58′
                centre-degrees: RA 331.980000; Dec +39.970000
                field-degrees: 60.0
                projection: stereographic
                page-subject: M31 · Andromeda Galaxy region
                searched-target: none
                chart-pixels: 1180 x 760
                page-pixels: 1180 x 760; offset-x 0; offset-y 0
                display-scale: 2.0 x 2.0
                ground: black-sky
                interface-language: nb-NO
                sky-language: nb-NO (chosen: follow-interface)
                limiting-magnitude: 6.0
                options: deep-sky-objects on, deep-sky-labels on, \
                galaxies on, open-clusters on, globular-clusters on, \
                nebulae on, planetary-nebulae on, constellation-figures \
                on, constellation-boundaries on, constellation-names on, \
                star-names on, bayer-letters on, flamsteed-numbers on, \
                equatorial-grid on, title-block on, magnitude-key off
                page-overrides: none
                emphasis: meridian+equatorial-grid+horizon
                modules: place-and-time, ecliptic
                observer-lines: meridian on, horizon on, zenith off
                observer: latitude +59.913000; longitude-east +10.752000
                instant-utc: 2026-03-20T21:33:00Z
                ecliptic: off
                selection: star star:hip-102098
                working-set: star:hip-102098, dso:ngc-7000 \
                (lead: star:hip-102098)
                os: TestOS 9.9 x9
                java: 21.0.99

                Comment:
                """, ViewReport.format(rich()));
    }

    @Test
    void everyFieldIsNamedOnceAndTheCommentIsLeftBlank() {
        String report = ViewReport.format(rich());
        List<String> keys = new ArrayList<>();
        for (String line : report.split("\n", -1)) {
            int colon = line.indexOf(": ");
            if (colon > 0) {
                keys.add(line.substring(0, colon));
            }
        }
        assertEquals(keys.size(), Set.copyOf(keys).size(),
                "no field is named twice: " + keys);
        assertTrue(report.endsWith("\n\nComment:\n"),
                "the report ends on a blank Comment section");
    }

    @Test
    void theSameStateGivesTheSameBytes() {
        assertEquals(ViewReport.format(rich()), ViewReport.format(rich()),
                "no timestamp, no clock, nothing generated");
    }

    @Test
    void changingOneOwnerChangesOnlyItsLine() {
        ViewReport.Snapshot base = rich();
        ChartOptions noGrid = new ChartOptions(true, true, true, true,
                true, true, true, true, false, true, false, true, true,
                true, true, true, ChartPalette.BLACK_SKY);
        String subject = base.view().targetLabel();
        assertOneLine(base, with(base, new ChartViewState(
                new SkyPosition(331.99, 39.97), 60.0, 6.0, subject,
                null)),
                "centre-degrees", "centre");
        assertOneLine(base, with(base, new ChartViewState(
                new SkyPosition(331.98, 39.97), 42.0, 6.0, subject,
                null)),
                "field-degrees", "projection", "limiting-magnitude");
        assertOneLine(base, withOptions(base, noGrid), "options");
        assertOneLine(base, withEmphasis(base,
                EnumSet.of(ChartStructure.MERIDIAN)), "emphasis");
        assertOneLine(base, withPlace(base,
                OSLO.from(60.0, 10.752)), "observer");
        assertOneLine(base, withPlace(base,
                OSLO.at(Instant.parse("2026-03-20T21:34:00Z"))),
                "instant-utc");
    }

    @Test
    void aModuleThatIsNotAttachedIsNotReported() {
        ViewReport.Snapshot base = rich();
        ViewReport.Snapshot detached = new ViewReport.Snapshot(
                base.version(), base.view(), base.chartWidthPx(),
                base.chartHeightPx(), base.pageWidthPx(),
                base.pageHeightPx(), base.pageOffsetXPx(),
                base.pageOffsetYPx(), base.displayScale(), base.chosen(),
                base.drawn(),
                base.interfaceLanguage(), base.skyLanguageChosen(),
                base.skyLanguageOnChart(), base.emphasis(),
                List.of(), null, null, base.selection(),
                base.workingSet(), base.workingLead(), base.osName(),
                base.osVersion(), base.osArch(), base.javaVersion());
        String report = ViewReport.format(detached);
        for (String gone : List.of("observer-lines:", "observer:",
                "instant-utc:", "ecliptic:")) {
            assertTrue(!report.contains("\n" + gone),
                    "a detached module leaves no remembered " + gone);
        }
        assertTrue(ViewReport.format(base).contains("\nobserver: "),
                "while attached, its state is there");
    }

    @Test
    void aGlobeNamesWhatItDrawsDifferentlyFromTheSwitches() {
        ViewReport.Snapshot base = rich();
        ChartOptions drawn = base.chosen().onAGlobe();
        String report = ViewReport.format(withOptions(base, base.chosen(),
                drawn));
        assertTrue(report.contains(
                        "\npage-overrides: constellation-boundaries off\n"),
                "the drawn difference is named, not folded in");
        assertTrue(report.contains("constellation-boundaries on,"),
                "while the options line keeps the reader's choice");
    }

    // ---- one owner at a time ----------------------------------------

    private static void assertOneLine(ViewReport.Snapshot before,
                                      ViewReport.Snapshot after,
                                      String... expected) {
        String[] was = ViewReport.format(before).split("\n", -1);
        String[] now = ViewReport.format(after).split("\n", -1);
        assertEquals(was.length, now.length, "the same fields");
        List<String> moved = new ArrayList<>();
        for (int i = 0; i < was.length; i++) {
            if (!was[i].equals(now[i])) {
                moved.add(was[i].substring(0, was[i].indexOf(':')));
            }
        }
        List<String> allowed = List.of(expected);
        assertTrue(!moved.isEmpty() && moved.contains(allowed.get(0)),
                "the changed owner's line moved: " + moved);
        assertTrue(allowed.containsAll(moved),
                "and no unrelated line did: " + moved);
    }

    private static ViewReport.Snapshot with(ViewReport.Snapshot b,
                                            ChartViewState view) {
        return copy(b, view, b.chosen(), b.drawn(), b.emphasis(),
                b.placeAndTime());
    }

    private static ViewReport.Snapshot withOptions(ViewReport.Snapshot b,
                                                   ChartOptions both) {
        return withOptions(b, both, both);
    }

    private static ViewReport.Snapshot withOptions(ViewReport.Snapshot b,
                                                   ChartOptions chosen,
                                                   ChartOptions drawn) {
        return copy(b, b.view(), chosen, drawn, b.emphasis(),
                b.placeAndTime());
    }

    private static ViewReport.Snapshot withEmphasis(ViewReport.Snapshot b,
            Set<ChartStructure> emphasis) {
        return copy(b, b.view(), b.chosen(), b.drawn(), emphasis,
                b.placeAndTime());
    }

    private static ViewReport.Snapshot withPlace(ViewReport.Snapshot b,
                                                 Observer observer) {
        ViewReport.PlaceAndTime p = b.placeAndTime();
        return copy(b, b.view(), b.chosen(), b.drawn(), b.emphasis(),
                new ViewReport.PlaceAndTime(observer, p.meridian(),
                        p.horizon(), p.zenith()));
    }

    private static ViewReport.Snapshot copy(ViewReport.Snapshot b,
            ChartViewState view, ChartOptions chosen, ChartOptions drawn,
            Set<ChartStructure> emphasis, ViewReport.PlaceAndTime place) {
        return new ViewReport.Snapshot(b.version(), view,
                b.chartWidthPx(), b.chartHeightPx(), b.pageWidthPx(),
                b.pageHeightPx(), b.pageOffsetXPx(), b.pageOffsetYPx(),
                b.displayScale(),
                chosen, drawn, b.interfaceLanguage(),
                b.skyLanguageChosen(), b.skyLanguageOnChart(), emphasis,
                b.modules(), place, b.eclipticShowing(), b.selection(),
                b.workingSet(), b.workingLead(), b.osName(),
                b.osVersion(), b.osArch(), b.javaVersion());
    }
}
