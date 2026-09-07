package juranometria.tool.overview;

import java.awt.geom.Rectangle2D;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 30 gate's load-bearing claims, held executable
 * (issue #296).
 *
 * <p>The gate decided things the rest of the sprint is built on: that
 * the study's gnomonic candidate <em>is</em> production, that the
 * curve vocabulary is exact and complete, that a page can ask for
 * more than one run and for a run with no ends, and that production's
 * scene query is short for any projection but its own. A decision
 * document can go stale. These cannot.
 *
 * <p>Nothing here tests the atlas: this issue changed no production
 * behaviour. It tests the measurements the next four issues quote.
 */
class OverviewProjectionGateTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);
    private static final SkyPosition POLE = new SkyPosition(0.0, 90.0);

    @Test
    void theGnomonicCandidateIsProductionAndNotAnImitationOfIt() {
        // Every gnomonic row in every table is a measurement of the
        // released atlas only if this is exactly zero. "Close" would
        // mean the study had quietly built its own gnomonic
        // projection and been comparing two of its own things.
        assertEquals(0.0, AgreementReport.gnomonicAgreement(ORION),
                "the study's gnomonic candidate lands on the same plane"
                        + " point as production's GnomonicProjection");
    }

    @Test
    void theStereographicCandidateAgreesWithTheOneSprintTwentyNineWrote() {
        // A different derivation - Sprint 29 wrote it from the
        // projection's closed form, this one from a radius function
        // shared with the other two candidates.
        assertTrue(AgreementReport.stereographicAgreement(ORION) < 1.0e-12,
                "the two independent stereographic implementations agree:"
                        + " " + AgreementReport.stereographicAgreement(ORION));
    }

    @Test
    void everyCandidateCanBeInvertedWellEnoughToPointAt() {
        // Also the guard on how the angle is measured. The obvious
        // cosine formula has a floor near 8.5e-07 degrees whatever it
        // is given, and reported exactly that for all three
        // candidates - so a threshold below the floor is what keeps
        // the measurement honest as well as the projections.
        for (String name : List.of("gnomonic", "stereographic",
                "orthographic")) {
            double back = AgreementReport.roundTrip(
                    Candidates.named(name, ORION));
            assertTrue(back < 1.0e-11, name + " round-trips sky to plane"
                    + " and back to within 1e-11 degrees: " + back);
        }
    }

    @Test
    void everyGreatCircleOnEveryStudiedPageTakesAnExactForm() {
        // The issue refuses a sampled-polyline compromise. This is
        // that refusal, measured: over the whole grid the study
        // reports, no combination falls back to sampling and none
        // misses its own projected points by more than the
        // acceptance threshold.
        Rectangle2D page = new Rectangle2D.Double(0, 0, 900, 700);
        int checked = 0;
        for (String name : List.of("gnomonic", "stereographic",
                "orthographic")) {
            for (PageCurveReport.Field field : PageCurveReport.FIELDS) {
                StudyProjection projection =
                        Candidates.named(name, field.centre());
                for (double width : new double[] {42, 60, 90, 120, 180}) {
                    if (width / 2.0 >= projection.limitDegrees()) {
                        continue;
                    }
                    StudyMapping mapping = new StudyMapping(projection,
                            width, 900, 700);
                    for (PageCurveReport.Circle circle
                            : PageCurveReport.CIRCLES) {
                        var built = PageCurves.greatCircle(mapping,
                                circle.pole(), 720);
                        if (built.isEmpty()
                                || built.get().curve().clipTo(page)
                                        .isEmpty()) {
                            continue;
                        }
                        checked++;
                        assertFalse(
                                built.get().curve()
                                        instanceof PageCurve.Sampled,
                                name + " draws " + circle.name() + " at "
                                        + width + " degrees exactly, not"
                                        + " by sampling it");
                        assertTrue(built.get().residual() < PageCurves.EXACT,
                                name + " " + circle.name() + " at " + width
                                        + " degrees misses its own points"
                                        + " by " + built.get().residual());
                    }
                }
            }
        }
        assertTrue(checked > 100, "the grid the decision quotes is the"
                + " grid measured here: " + checked + " combinations");
    }

    @Test
    void aPageCanAskForMoreThanOneRunAndTheClipperNeverInventsThem() {
        // Production's Optional<Arc> can say "once" or "not at all".
        // Both halves matter: that more than one run really happens,
        // so the wider return type is earned, and that the clipper
        // never reports more pieces than a curve and a rectangle can
        // make - four - which an earlier version of it did, by
        // counting a grazing touch as a way in.
        Rectangle2D page = new Rectangle2D.Double(0, 0, 900, 700);
        int severalRuns = 0;
        for (String name : List.of("gnomonic", "stereographic",
                "orthographic")) {
            for (PageCurveReport.Field field : PageCurveReport.FIELDS) {
                StudyProjection projection =
                        Candidates.named(name, field.centre());
                for (double width : new double[] {42, 60, 90, 120, 180}) {
                    if (width / 2.0 >= projection.limitDegrees()) {
                        continue;
                    }
                    StudyMapping mapping = new StudyMapping(projection,
                            width, 900, 700);
                    for (PageCurveReport.Circle circle
                            : PageCurveReport.CIRCLES) {
                        var built = PageCurves.greatCircle(mapping,
                                circle.pole(), 720);
                        if (built.isEmpty()) {
                            continue;
                        }
                        List<PageCurve.Run> runs =
                                built.get().curve().clipTo(page);
                        int most = built.get().curve()
                                instanceof PageCurve.Straight ? 1 : 4;
                        assertTrue(runs.size() <= most,
                                name + " " + circle.name() + " at " + width
                                        + " degrees comes back in "
                                        + runs.size() + " runs, where "
                                        + most + " is the most the"
                                        + " geometry allows");
                        if (runs.size() > 1) {
                            severalRuns++;
                        }
                    }
                }
            }
        }
        assertTrue(severalRuns > 0, "real pages do ask for more than one"
                + " run, which is why the return type widened");
    }

    @Test
    void aCurveThatGrazesAnEdgeIsNotCutInHalfThere() {
        // The count bound above is too loose to catch this, because
        // the bug does not make too many runs - it makes one run
        // where there should be none of the boundary at all. The
        // pole-centred equator at a 180-degree field is exactly
        // tangent to the left and right edges of the page and
        // crosses the top and bottom, so it must come back as two
        // runs that are mirror images of each other. Rounding
        // decides whether each tangency is found twice, once or not
        // at all, and found twice it puts a hair of curve a rounding
        // error outside the paper: the same geometry then came back
        // in three pieces on one side of the page and one on the
        // other, which is what asymmetry from symmetric input looks
        // like.
        Rectangle2D page = new Rectangle2D.Double(0, 0, 900, 700);
        StudyMapping mapping = new StudyMapping(
                Candidates.stereographic(POLE), 180.0, 900, 700);
        List<PageCurve.Run> runs = PageCurves
                .greatCircle(mapping, POLE, 720).orElseThrow()
                .curve().clipTo(page);

        assertEquals(2, runs.size(), "the equator crosses this page on"
                + " either side of it, once each");
        Rectangle2D left = runs.get(0).shape().getBounds2D();
        Rectangle2D right = runs.get(1).shape().getBounds2D();
        assertEquals(left.getWidth(), right.getWidth(), 1.0e-6,
                "and the two runs are the same size");
        assertEquals(left.getHeight(), right.getHeight(), 1.0e-6,
                "in both directions");
        assertEquals(900.0 - left.getCenterX(), right.getCenterX(), 1.0e-6,
                "mirrored about the middle of the page, because the sky"
                        + " they come from is");
    }

    @Test
    void aCurveCanCloseOnThePageWithNoEndForALabel() {
        // Not at any field Sprint 30 offers - the pole-centred
        // equator first closes past 208 degrees - but one page width
        // away from one it does. A run therefore has optional ends,
        // and this is the case that would otherwise be discovered by
        // a crash in whatever names the line.
        StudyMapping mapping = new StudyMapping(
                Candidates.stereographic(POLE), 240.0, 900, 700);
        var built = PageCurves.greatCircle(mapping, POLE, 720).orElseThrow();
        List<PageCurve.Run> runs = built.curve()
                .clipTo(new Rectangle2D.Double(0, 0, 900, 700));
        assertEquals(1, runs.size(), "a closed curve is one run");
        assertTrue(runs.get(0).closed(),
                "and it has no ends to hang a name on");

        // And the same circle at a field Sprint 30 does offer has
        // ends, so the two cases are genuinely distinguished rather
        // than one of them being reported always.
        StudyMapping narrower = new StudyMapping(
                Candidates.stereographic(POLE), 120.0, 900, 700);
        var onPage = PageCurves.greatCircle(narrower, POLE, 720)
                .orElseThrow().curve()
                .clipTo(new Rectangle2D.Double(0, 0, 900, 700));
        assertTrue(onPage.stream().noneMatch(PageCurve.Run::closed),
                "at 120 degrees the same circle leaves the page");
    }

    @Test
    void productionsSceneQueryIsShortForAnyProjectionButItsOwn() {
        // The #297 contract. SceneAssembler works out how much sky to
        // fetch from the gnomonic page corner, so a stereographic
        // page of the same field reaches past what it was given - and
        // the objects in the ring are not drawn badly, they are
        // absent in the corners.
        StudyScenes scenes = new StudyScenes();
        StudyProjection stereographic = Candidates.stereographic(ORION);
        double honest = scenes.queryRadiusFor(stereographic, 120.0);
        double asProduction = scenes.queryRadiusFor(
                Candidates.gnomonic(ORION), 120.0);
        assertTrue(honest > asProduction + 5.0,
                "a 120-degree stereographic page reaches " + honest
                        + " degrees where the gnomonic rule fetches "
                        + asProduction);
        assertTrue(scenes.shortfall(stereographic, 120.0) > 1000,
                "and the ring between them holds catalogue objects: "
                        + scenes.shortfall(stereographic, 120.0));

        // The rule is not short for the projection it was written
        // for, which is why nothing has ever gone wrong.
        assertEquals(0, scenes.shortfall(Candidates.gnomonic(ORION), 120.0),
                "gnomonic pages have always been fetched correctly");
    }

    @Test
    void theArchitecturalRuleIsRecordedWhereTheRepositoryKeepsRules()
            throws Exception {
        String architecture = Files.readString(
                Path.of("docs/architecture.md"), StandardCharsets.UTF_8);
        assertTrue(architecture.contains(
                        "A module says what belongs on the sky. A projection"
                                + " says how the sky\nbecomes a page."),
                "docs/architecture.md states the rule this gate settled");

        String decision = Files.readString(
                Path.of("docs/decisions/overview-projection.md"),
                StandardCharsets.UTF_8);
        assertTrue(decision.contains(
                        "The first overview projection is **stereographic**"),
                "and the decision document names what was chosen");
    }
}
