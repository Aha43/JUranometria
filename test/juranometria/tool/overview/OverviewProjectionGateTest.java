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
        // The issue refuses a sampled-polyline compromise, and the
        // vocabulary has no sampled member to fall back to, so this
        // asks the two questions that are left: that the projection
        // states a form at all, and that the curve it states passes
        // through points the projection genuinely produced.
        Rectangle2D page = new Rectangle2D.Double(0, 0, 900, 700);
        int checked = 0;
        for (Case each : grid()) {
            var built = PageCurves.greatCircle(each.mapping(),
                    each.pole(), 720);
            if (built.isEmpty()
                    || built.get().curve().clipTo(page).isEmpty()) {
                continue;
            }
            checked++;
            assertTrue(built.get().residual() < PageCurves.EXACT,
                    each + " misses its own projected points by "
                            + built.get().residual());
        }
        assertTrue(checked > 100, "the grid the decision quotes is the"
                + " grid measured here: " + checked + " combinations");
    }

    @Test
    void theProjectionStatesTheCurveAndDoesNotHaveToBeAskedTwice() {
        // The finding a review made of this gate's first proposal.
        // An interface that only maps points forces its caller to
        // sample and then to type-check; this one answers. The check
        // is that the closed form and an independent fit to several
        // hundred projected points name the same form every time -
        // two derivations that could not plausibly be wrong in the
        // same way.
        int agreed = 0;
        for (Case each : grid()) {
            // Through the page, because which drawable form a conic
            // becomes is the page's decision and not the
            // projection's.
            String stated = each.mapping().projection()
                    .greatCircle(each.pole())
                    .map(conic -> each.mapping().onPage(conic).form())
                    .orElse("none");
            String fitted = PageCurves
                    .fitted(each.mapping(), each.pole(), 720)
                    .map(PlaneCurve::form).orElse("none");
            if ("none".equals(stated)) {
                continue;  // not on this page at all
            }
            assertEquals(fitted, stated, each + ": the form the"
                    + " projection states and the form fitted to the"
                    + " points it projected");
            if (!"none".equals(stated)) {
                agreed++;
            }
        }
        assertTrue(agreed > 100,
                "over a grid worth quoting: " + agreed + " curves");
    }

    @Test
    void theVocabularyHasThreeWordsAndAllOfThemAreUsed() {
        // If a form were never reached, it would be a word in the
        // vocabulary that nothing had checked - and #301 is only an
        // addition rather than a redesign because the elliptical form
        // is written and exercised now.
        java.util.Set<String> forms = new java.util.TreeSet<>();
        for (Case each : grid()) {
            each.mapping().projection().greatCircle(each.pole())
                    .ifPresent(conic -> forms.add(
                            each.mapping().onPage(conic).form()));
        }
        assertEquals(java.util.Set.of("circular", "elliptical",
                "straight"), forms,
                "the three forms the decision names, all reached");
    }

    @Test
    void eachProjectionAgreesWithItselfAboutItsOwnEdge() {
        // A domain stated one way by project() and another by
        // unproject() is a projection that does not know where it
        // stops. Both halves were wrong once: the orthographic
        // inverse refused a radius of exactly one - the limb, which
        // its forward projection places quite happily - and the
        // stereographic limit was written 179.999, which is not a
        // rounding guard but a finite region of sky silently
        // dropped.
        for (String name : List.of("gnomonic", "stereographic",
                "orthographic")) {
            StudyProjection projection = Candidates.named(name, ORION);
            for (double away = 0.0; away <= 180.0; away += 0.25) {
                SkyPosition out = along(ORION, away);
                var plane = projection.project(out);
                if (plane.isEmpty()) {
                    continue;
                }
                assertTrue(projection.unproject(plane.get()).isPresent(),
                        name + " places a point " + away + " degrees"
                                + " from its centre and must be able to"
                                + " take it back");
            }
        }
    }

    @Test
    void theOrthographicLimbIsOnTheGlobeAndTheAntipodeIsNotOnThePlane() {
        // The two edges named above, at the exact angles where they
        // were wrong, so a re-introduction is caught at the point it
        // would be made.
        StudyProjection orthographic = Candidates.orthographic(ORION);
        var limb = orthographic.project(along(ORION, 90.0));
        assertTrue(limb.isPresent(), "the limb is on the globe");
        assertEquals(1.0, Math.hypot(limb.get().xiEast(),
                limb.get().etaNorth()), 1.0e-12,
                "at a radius of exactly one");
        assertTrue(orthographic.unproject(limb.get()).isPresent(),
                "and the inverse accepts it");

        StudyProjection stereographic = Candidates.stereographic(ORION);
        assertEquals(180.0, stereographic.limitDegrees(),
                "stereographic reaches everything but one point");
        assertTrue(stereographic.project(along(ORION, 179.999)).isPresent(),
                "a thousandth of a degree from the antipode is sky, and"
                        + " is drawn");
        assertTrue(stereographic.project(along(ORION, 180.0)).isEmpty(),
                "the antipode itself is the one point it has no answer"
                        + " for - and tan(pi/2) is 1.6e16 in a double,"
                        + " not infinity, so this cannot be left to an"
                        + " overflow");
    }

    @Test
    void aCurveNearlyDegenerateIsStillRightWhereItIsDrawn() {
        // The case no epsilon survived. A pole a hundred-millionth of
        // a degree from square to the page centre gives a circle a
        // hundred million pages wide, which is a line for every
        // purpose a reader has. Measured at the allowance the study
        // chose, over the narrowest and widest pages it draws.
        // The allowance is the one the sweep chose, not one that
        // drifted afterwards. Without this the check below passes
        // for any allowance at all, because a looser allowance is
        // honoured just as faithfully as a tighter one - it simply
        // permits a worse curve.
        assertEquals(SubstitutionReport.CHOSEN,
                StudyMapping.DEFAULT_ALLOWED_PAGE_UNITS,
                "the page allowance is the one the measured sweep"
                        + " settled on");

        for (double offset : new double[] {1.0e-3, 1.0e-6, 1.0e-9,
                1.0e-12, 1.0e-15}) {
            SkyPosition pole = new SkyPosition(
                    90.0 - Math.toDegrees(offset), 0.0);
            for (double field : new double[] {1.0, 42.0, 180.0}) {
                StudyMapping mapping = new StudyMapping(
                        Candidates.stereographic(new SkyPosition(0, 0)),
                        field, 900, 700);
                var built = PageCurves.greatCircle(mapping, pole, 720);
                if (built.isEmpty()
                        || Double.isNaN(built.get().residual())) {
                    continue;
                }
                assertTrue(built.get().residual()
                                < 2.0 * StudyMapping
                                        .DEFAULT_ALLOWED_PAGE_UNITS,
                        "a pole " + offset + " from square, on a "
                                + field + " degree page, is drawn to "
                                + built.get().residual()
                                + " page units, against an allowance of "
                                + StudyMapping.DEFAULT_ALLOWED_PAGE_UNITS);
            }
        }
    }

    @Test
    void theEclipticSeenFromTheEquinoxIsDrawnAtAll() {
        // The case that broke every attempt to special-case the
        // degeneracy by an equality. The ecliptic passes through the
        // vernal equinox, so its pole is square to that page centre -
        // but cos(270 degrees) is -1.8e-16 in a double, not zero, so
        // the exactly degenerate case does not arrive exactly. Held
        // by what a reader would notice: the line is on the page.
        StudyMapping mapping = new StudyMapping(
                Candidates.stereographic(new SkyPosition(0.0, 0.0)),
                60.0, 900, 700);
        var built = PageCurves.greatCircle(mapping,
                new SkyPosition(270.0, 66.5607), 720).orElseThrow();
        assertEquals("straight", built.curve().form(),
                "a great circle through the page centre is straight");
        assertTrue(built.curve()
                        .clipTo(new Rectangle2D.Double(0, 0, 900, 700))
                        .size() == 1,
                "and it crosses the page");
        assertTrue(built.residual() < PageCurves.EXACT,
                "and lands where the projected points are: "
                        + built.residual());
    }

    /** A position a given angle from the centre, due north of it. */
    private static SkyPosition along(SkyPosition centre, double degrees) {
        double dec = centre.decDegrees() + degrees;
        if (dec <= 90.0) {
            return new SkyPosition(centre.raDegrees(), dec);
        }
        return new SkyPosition((centre.raDegrees() + 180.0) % 360.0,
                180.0 - dec);
    }

    /** One projection, one field, one great circle. */
    private record Case(String projection, String field,
                        StudyMapping mapping, SkyPosition pole,
                        String circle, double width) {

        @Override
        public String toString() {
            return projection + " " + circle + " over " + field + " at "
                    + width + " degrees";
        }
    }

    /** The grid the study reports, as the tests read it. */
    private static List<Case> grid() {
        List<Case> cases = new java.util.ArrayList<>();
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
                        cases.add(new Case(name, field.name(), mapping,
                                circle.pole(), circle.name(), width));
                    }
                }
            }
        }
        return cases;
    }

    @Test
    void aPageCanAskForMoreThanOneRunAndTheClipperNeverInventsThem() {
        // Production's Optional<Arc> can say "once" or "not at all".
        // Both halves matter: that more than one run really happens,
        // so the wider return type is earned, and that the clipper
        // never reports more runs than a curve and a rectangle can
        // make. They meet in up to eight points - two per edge - so
        // four runs is the ceiling, and an earlier version of the
        // clipper passed it by counting a grazing touch as a way in.
        Rectangle2D page = new Rectangle2D.Double(0, 0, 900, 700);
        int severalRuns = 0;
        for (Case each : grid()) {
            var built = PageCurves.greatCircle(each.mapping(),
                    each.pole(), 720);
            if (built.isEmpty()) {
                continue;
            }
            List<PlaneCurve.Run> runs = built.get().curve().clipTo(page);
            int most = built.get().curve()
                    instanceof PlaneCurve.Straight ? 1 : 4;
            assertTrue(runs.size() <= most, each + " comes back in "
                    + runs.size() + " runs, where " + most + " is the"
                    + " most the geometry allows");
            if (runs.size() > 1) {
                severalRuns++;
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
        List<PlaneCurve.Run> runs = PageCurves
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
        List<PlaneCurve.Run> runs = built.curve()
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
        assertTrue(onPage.stream().noneMatch(PlaneCurve.Run::closed),
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
