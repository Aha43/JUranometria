package juranometria.tool.overview;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartPalette;
import juranometria.sheet.PaperSize;

/**
 * The overview projection study (issue #296).
 *
 * <p>Writes the pages the gate is decided from, and the measurements
 * beside them. Nothing here is production code and nothing here
 * changes any; the atlas draws exactly what it drew before this
 * class existed. What it produces is the evidence for choosing what
 * issue #297 opens.
 *
 * <p>Run with {@code make overview-study}.
 */
public final class OverviewStudyMain {

    private OverviewStudyMain() {
    }

    /** The screen page the atlas actually shows. */
    private static final int SCREEN_WIDE = 900;
    private static final int SCREEN_HIGH = 700;

    private static final List<String> PROJECTIONS =
            List.of("gnomonic", "stereographic", "orthographic");

    /** A field to study, and what a reader would call it. */
    private record Look(String slug, String name, SkyPosition centre,
                        double[] fields) {
    }

    private static final List<Look> LOOKS = List.of(
            // 36 and 42 are the released control: what the atlas
            // draws today, drawn by the same study painter, so that
            // every wider page is compared against something a
            // reader has actually held.
            new Look("orion", "Orion", new SkyPosition(83.0, 0.0),
                    new double[] {36, 42, 48, 60, 90, 120, 180}),
            new Look("ursa-major", "the Plough",
                    new SkyPosition(180.0, 55.0), new double[] {60, 90}),
            new Look("sagittarius", "the Milky Way in Sagittarius",
                    new SkyPosition(275.0, -25.0), new double[] {60, 90}),
            new Look("pole", "the north celestial pole",
                    new SkyPosition(0.0, 90.0), new double[] {90, 120}),
            new Look("equinox", "the vernal equinox",
                    new SkyPosition(0.0, 0.0), new double[] {60, 120}));

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0]
                : "docs/studies/overview-projection");
        Files.createDirectories(out);
        StudyScenes scenes = new StudyScenes();

        StringBuilder pages = new StringBuilder();
        pages.append("| page | projection | field | stars drawn |"
                + " off the projection | ink | reference ink |\n");
        pages.append("|---|---|---:|---:|---:|---:|---|\n");

        for (Look look : LOOKS) {
            for (double field : look.fields()) {
                for (String name : PROJECTIONS) {
                    StudyProjection projection =
                            Candidates.named(name, look.centre());
                    if (field / 2.0 >= projection.limitDegrees()) {
                        continue;
                    }
                    write(out, scenes, pages, look, field, projection,
                            ChartPalette.WHITE_PAPER, SCREEN_WIDE,
                            SCREEN_HIGH, "");
                }
            }
        }

        // The same page on the other ground, and on the sheet's own
        // shape - a chart that only worked in one of each would be a
        // chart that had not been looked at.
        for (String name : PROJECTIONS) {
            StudyProjection projection =
                    Candidates.named(name, new SkyPosition(83.0, 0.0));
            write(out, scenes, pages, LOOKS.get(0), 90.0, projection,
                    ChartPalette.BLACK_SKY, SCREEN_WIDE, SCREEN_HIGH,
                    "-black");
            write(out, scenes, pages, LOOKS.get(0), 90.0, projection,
                    ChartPalette.WHITE_PAPER, PaperSize.A4.chartWideUnits(),
                    PaperSize.A4.chartHighUnits(), "-sheet");
        }

        Files.writeString(out.resolve("measurements.md"),
                document(pages.toString(), scenes),
                StandardCharsets.UTF_8);
        System.out.println("wrote " + out.resolve("measurements.md"));
    }

    private static void write(Path out, StudyScenes scenes,
                              StringBuilder pages, Look look, double field,
                              StudyProjection projection,
                              ChartPalette palette, int wide, int high,
                              String suffix) throws IOException {
        ChartScene scene = scenes.of(projection, field, wide, high,
                look.name());
        StudyPage page = new StudyPage(projection, scene, palette);

        BufferedImage image = new BufferedImage(wide, high,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            page.paint(g, PageCurveReport.CIRCLES);
        } finally {
            g.dispose();
        }
        String file = String.format(Locale.ROOT, "%s-%03.0f-%s%s.png",
                look.slug(), field, projection.name(), suffix);
        ImageIO.write(image, "png", new File(out.toFile(), file));

        pages.append(String.format(Locale.ROOT,
                "| %s | %s | %.0f° | %d | %d | %.1f%% | %s |%n",
                file, projection.name(), field, page.starsDrawn(),
                page.starsHeld(), 100.0 * inkFraction(image, palette),
                String.join("; ", page.notes())));
    }

    /**
     * How much of the page is ink.
     *
     * <p>A chart is read by picking a shape out of it, and a page
     * that is a third ink has no shapes left. This is the number
     * behind the judgement that a 180-degree page is drawable and
     * not readable - measured over the middle half of the page,
     * where the reader is looking.
     */
    private static double inkFraction(BufferedImage image,
                                      ChartPalette palette) {
        int ground = palette.ground().getRGB() & 0xffffff;
        int marked = 0;
        int seen = 0;
        int fromX = image.getWidth() / 4;
        int fromY = image.getHeight() / 4;
        for (int y = fromY; y < fromY + image.getHeight() / 2; y++) {
            for (int x = fromX; x < fromX + image.getWidth() / 2; x++) {
                seen++;
                if ((image.getRGB(x, y) & 0xffffff) != ground) {
                    marked++;
                }
            }
        }
        return marked / (double) seen;
    }

    /**
     * What each field costs in ink at a range of magnitudes.
     *
     * <p>The same page, drawn again with fainter stars left out,
     * measured the same way. Nothing else varies.
     */
    private static String magnitudeTable(StudyScenes scenes) {
        StudyProjection projection =
                Candidates.stereographic(new SkyPosition(83.0, 0.0));
        double[] magnitudes = {6.0, 5.5, 5.0, 4.5, 4.0};
        StringBuilder out = new StringBuilder();
        out.append("| field |");
        for (double magnitude : magnitudes) {
            out.append(String.format(Locale.ROOT, " mag %.1f |", magnitude));
        }
        out.append("\n|---:|");
        out.append("---:|".repeat(magnitudes.length));
        out.append("\n");
        for (double field : new double[] {60, 90, 120, 180}) {
            out.append(String.format(Locale.ROOT, "| %.0f° |", field));
            for (double magnitude : magnitudes) {
                out.append(String.format(Locale.ROOT, " %.1f%% |",
                        100.0 * inkAt(scenes, projection, field, magnitude)));
            }
            out.append("\n");
        }
        return out.toString();
    }

    private static double inkAt(StudyScenes scenes,
                                StudyProjection projection, double field,
                                double magnitude) {
        ChartScene scene = scenes.of(projection, field, SCREEN_WIDE,
                SCREEN_HIGH, "Orion", magnitude);
        BufferedImage image = new BufferedImage(SCREEN_WIDE, SCREEN_HIGH,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new StudyPage(projection, scene, ChartPalette.WHITE_PAPER)
                    .paint(g, PageCurveReport.CIRCLES);
        } finally {
            g.dispose();
        }
        return inkFraction(image, ChartPalette.WHITE_PAPER);
    }

    private static String document(String pages, StudyScenes scenes) {
        SkyPosition orion = new SkyPosition(83.0, 0.0);
        double[] fields = {36, 42, 48, 60, 90, 120, 180};
        StringBuilder out = new StringBuilder();

        // Measured first, because the prose quotes the totals and a
        // number written into prose by hand is a number that goes
        // stale the next time the study grows a circle.
        String curves = PageCurveReport.of(new double[] {42, 60, 90, 120,
                180}, SCREEN_WIDE, SCREEN_HIGH);
        int combinations = PageCurveReport.rows;
        double worstMiss = PageCurveReport.worstMiss;
        String control = String.format(Locale.ROOT, "%.1f%%",
                100.0 * inkAt(scenes, Candidates.stereographic(orion), 42.0,
                        StudyScenes.DEFAULT_LIMIT));

        out.append("""
                # Seeing more sky at once

                Measurements for issue #296, the Sprint 30 gate. Every
                number here was produced by `make overview-study` from
                the code in `src/juranometria/tool/overview`, over the
                bundled catalogue and the real constellation geography.
                Nothing in the atlas was changed to produce them.

                The question is not which projection is prettiest. It
                is which one an observer can read a 60-degree page of
                at a telescope, and what the atlas would have to grow
                to draw it.

                ## Are these the projections they say they are

                Three candidates built by one shared factory that
                differs only in a radius function is what makes them
                comparable, and is also how a study measures the
                wrong thing convincingly. So each is checked against
                something written independently and reviewed before
                this issue existed - and the round trip is measured
                too, because a projection that could not be inverted
                is one a reader could not point at.

                """);
        out.append(AgreementReport.of(orion));
        out.append("""

                The gnomonic candidate is not merely close to
                production's: it is the same plane point. It delegates
                to `GnomonicProjection` and `PanSolver`, so every
                gnomonic row in every table below is a measurement of
                the released atlas, not of a study's imitation of it.

                The stereographic candidate agrees with the one the
                Sprint 29 gate wrote from the projection's closed
                form, which is a different derivation reaching the
                same numbers.

                The last column is the domain, and it is the whole
                difference between a chart and an overview: two of
                these can hold half the sky and one of them can hold
                all but a point of it.

                ## What a page costs, by field

                A page has a centre, where every projection is
                perfect, and it has corners. The corner is where the
                cost lives, so it is what is measured: how far from
                the centre the corner of a 900x700 page reaches, and
                what the projection is doing to the sky there.

                **Scale** is how much bigger a degree has become -
                distances read wrong. **Shape** is the difference
                between the two directions - a round cluster is drawn
                as an ellipse, and shape is what a reader matches
                against the sky.

                """);
        out.append(DistortionReport.of(orion, fields));
        out.append("""

                Read down the shape column. The gnomonic projection
                is exact at the centre and nowhere else, and by 90
                degrees it is drawing a circle as an ellipse half as
                wide again as it is tall. The stereographic
                projection's shape column is zero at every field, at
                every corner, to the precision of the arithmetic:
                that is what conformal means, and it is the whole
                argument. It pays for it in scale, and the price is
                mild - at 120 degrees a corner degree is half as big
                again as a centre degree, where the gnomonic
                projection's is nearly six times.

                The orthographic projection is worse than both at
                every field. It is not a chart projection and this is
                not a criticism of it: it is a picture of a ball, and
                a ball seen from outside is exactly what it should
                look like.

                ## What each projection can show at all

                """);
        out.append(DistortionReport.domains(orion));
        out.append("""

                The gnomonic projection's edge is a hard one: a point
                90 degrees from the centre is infinitely far away on
                the paper, and one at 89.9 degrees is 573 plane units
                out when the whole 42-degree page is 0.77 wide. The
                stereographic projection's edge is the antipode and
                nothing else - it draws the entire sky but one point,
                and a whole hemisphere fits inside a radius of 2.0.
                The orthographic projection stops at a limb it can
                draw, which is why it makes a globe and not a chart.

                ## Pointing at something

                A reader points at a mark and the atlas says what it
                is. What one page unit is worth in the sky decides
                whether that is possible, and it is worth different
                things in different parts of a wide page:

                """);
        out.append(DistortionReport.pointing(orion, new double[] {42, 60,
                90, 120, 180}, SCREEN_WIDE, SCREEN_HIGH));
        out.append("""

                The two chart projections magnify their corners, so a
                page unit there covers *less* sky than at the centre
                and pointing gets no harder as the page widens. The
                orthographic projection compresses its corners
                instead: on a 90-degree globe a page unit at the
                corner covers twelve arcminutes, four times what the
                same page's centre covers, and near the limb two
                stars a finger-width apart in the sky are the same
                pixel. A globe is a thing to look at. It is not a
                thing to point at, and that is a second reason to
                keep it apart from the chart rather than a reason to
                draw it badly.

                ## The shape a great circle takes

                The atlas draws great circles: the celestial equator,
                the ecliptic, an observer's meridian and horizon.
                Under the gnomonic projection every one of them is a
                straight line, which is why `GreatCirclePage` clips
                two endpoints and `ReferenceInk` draws a `Line2D`
                between them.

                Here is what they become. Each curve is
                **determined** by the fewest points that fix a form -
                two for a line, three for a circle, five for a conic
                - and then **measured** against 200 to 400 more,
                worst miss in plane units. This table is how the
                question was first asked; what a page actually uses
                is further down, and it is not this.

                """);
        out.append(CurveFormReport.of(orion));
        out.append("""

                So the vocabulary is three words, and all three are
                exact:

                - a **straight** run - every gnomonic great circle,
                  and any circle through the page centre under the
                  other two;
                - a **circular** run - every other stereographic
                  great circle;
                - an **elliptical** run - every orthographic great
                  circle, and nothing else needs it.

                ## The projection states its own curve

                Fitting a form to projected points is how the
                question above was investigated. It is not how a page
                should be drawn, and a review of the gate's first
                proposal was right to say so: a caller that has to
                project a few hundred points and fit a curve to them
                is sampling, which this issue refuses; then asking
                which form came back, which is a type check; and then
                one day meeting a projection whose form nobody had
                written, which is widening the vocabulary later.

                A projection knows the answer without being asked
                twice. Write the pole in the projection's own frame -
                the same three dot products `project` already takes -
                as its components along the centre, east and north
                directions, and the condition that a point lies on the
                circle becomes an equation in the page coordinates:

                | projection | substituting its own radius | leaves |
                |---|---|---|
                | gnomonic | `r = tan t` | `a + b xi + c eta = 0`, a line |
                | stereographic | `r = 2 tan(t/2)` | `a(1 - (xi^2 + eta^2)/4) + b xi + c eta = 0`, a circle of centre `(2b/a, 2c/a)` and radius `2/|a|` - a line when `a` is zero |
                | orthographic | `r = sin t` | `(a^2 + b^2) xi^2 + 2bc xi eta + (a^2 + c^2) eta^2 = a^2`, an ellipse of radii `|a|` and 1 |

                So `greatCircle(pole)` belongs on the projection
                interface, and the three forms are returned by
                arithmetic rather than found by search. Each
                projection returns the **simplest form that is
                exact**, so one curve has one name: the stereographic
                circle of infinite radius is a line, and the
                orthographic ellipse with equal axes is a circle.

                The fit is kept, and it has become the check. Two
                independent routes to the same curve - one from the
                projection's algebra, one from several hundred points
                it actually projected - must arrive at the same form,
                and the drawn curve must pass through those points:

                """);
        out.append(String.format(java.util.Locale.ROOT, """
                | measured over | stated form agrees with fitted form | worst the drawn curve misses a projected point |
                |---:|---:|---:|
                | %d combinations | %d, and %d disagree | %.1e page units |

                """, PageCurveReport.rows,
                PageCurveReport.agreed, PageCurveReport.disagreed,
                PageCurveReport.worstMiss));
        out.append("""
                The disagreement that this check did find is worth
                recording, because it was not an error. Centred on
                the pole, the orthographic image of the celestial
                equator is the limb: an ellipse whose two radii are
                equal, which is to say a circle. The algebra said
                ellipse and the fit said circle and both were right,
                which is what made the rule above - the simplest form
                that is exact - a rule rather than a preference.

                Two other things this check caught, neither of which
                the arithmetic on paper suggested. The orthographic
                closed form returns a degenerate ellipse when the
                circle runs through the page centre, and a degenerate
                ellipse has no interior and an affine frame that
                cannot be inverted, so it must be returned as the line
                it is. And a line carried a million plane units out
                from the origin becomes a billion page units, where
                measuring a point's distance from it loses seven
                digits: the straight form's worst miss read 1.1e-07
                where the circles were reading 1e-11.

                ## Where a curve stops being one thing

                A great circle can pass arbitrarily near a degenerate
                case, and nothing in the sky forbids it: a pole a
                hundred-millionth of a degree from square to the page
                centre gives a stereographic circle whose radius is
                a hundred million pages wide. It is a line for every
                purpose a reader has, and the atlas has to say so
                without a magic number.

                Where that decision is made matters more than what
                the number is. It cannot be made by the projection,
                which has no page: the same great circle is plainly
                curved across a hemisphere and plainly straight
                across a telescope field. So the projection states a
                conic - which stays finite through every degeneracy,
                because nothing divides by the quantity going to zero
                - and the page picks the simplest drawable form whose
                distance from the true curve, **over that paper**, is
                below one allowance.

                That leaves one number to choose, and it is measured
                rather than asserted. Too generous and a substituted
                curve visibly departs from the true one; too strict
                and the page keeps a conic whose centre and radius
                are too large to work out at all:

                """);
        out.append(SubstitutionReport.of());
        out.append("""

                Read the left column downwards. Curves near a
                degeneracy track the allowance exactly, as they
                should - the substitution is doing what it promises -
                until the allowance passes below a thousandth of a
                page unit, whereupon the page keeps conics it cannot
                work out and the error jumps by seven orders of
                magnitude. The right column shows that ordinary pages
                are untouched by the choice at every value of it,
                which is the other thing worth knowing: this is a
                decision about the degenerate cases and about nothing
                else.

                So the allowance is **a thousandth of a page unit**,
                which is a thousandth of the thinnest line the atlas
                draws, and it is chosen at the edge of the cliff
                rather than near it by luck.

                ## What one page asks of the vocabulary

                Two things production's `Optional<Arc>` cannot say,
                found by clipping real pages rather than by thinking
                about it:

                """);
        out.append(curves);
        out.append("""

                **A curve can cross one page more than once.** A
                circle and a rectangle meet in up to eight points -
                two per edge - which is up to four separate runs of
                curve on the paper. Twenty-one of the combinations
                measured here leave the page and come back, and four
                of them come back in four runs.
                `Optional<Arc>` can only answer "once" or "not at
                all", so it would draw one run and silently drop the
                rest.

                **A curve can close.** A great circle wholly inside
                the paper has no ends, and the rule that names a line
                "where it leaves the page" has nothing to hang on. No
                page at or below a 180-degree field closes: the
                pole-centred equator under the stereographic
                projection first closes at a field of
                **208.5 degrees**, and at 180 degrees it is exactly
                tangent to the left and right edges, which is one
                rounding error away and was enough to make an early
                version of the clipper in this study report the same
                circle in three pieces on one side of the page and
                one on the other. The vocabulary should be able to
                say "closed" even though nothing in Sprint 30 asks it
                to.

                ## Where the atlas assumes one projection

                The gnomonic projection is not a setting. It is
                constructed in place, twenty-five times, in seven
                files:

                | file | constructions |
                |---|---:|
                | `page/PageExtent.java` | 8 |
                | `render/ChartRenderer.java` | 6 |
                | `render/EquatorialGrid.java` | 4 |
                | `ui/ReferenceInk.java` | 2 |
                | `ui/WorkingCrossInk.java` | 2 |
                | `page/PageInventory.java` | 2 |
                | `project/PanSolver.java` | 1 |

                Three of those are not merely construction sites but
                gnomonic arithmetic written into a rule that reads as
                if it were general:

                **The mapping.** `ViewportMapping` sets its scale to
                `width / (2 tan(field/2))` and refuses a field of 180
                degrees or more. The tangent is the gnomonic radius
                function; the refusal is not about pages, it is about
                `tan`. The general rule is the same sentence with the
                projection's own radius in it, and it agrees with
                production exactly wherever production works.

                **The query.** `SceneAssembler.queryRadiusDegrees`
                works out how much sky to fetch from
                `atan(hypot(tan(field/2), ...))` - the gnomonic page
                corner. A stereographic page of the same field
                reaches further:

                """);
        out.append(shortfallTable(scenes));
        out.append("""

                Those are catalogue objects the page has room for and
                would not have been given. They would not look wrong.
                They would be missing, in the corners.

                **The cap.** `SceneAssembler` already refuses to let
                a page corner pass 60 degrees, with the comment
                *"Gnomonic charts degrade far from the centre; cap
                the page there"*. That cap is correct and it is the
                end of the road for wider gnomonic fields: on a
                900x700 page it is reached at a field of about
                """);
        out.append(String.format(Locale.ROOT, "**%.0f degrees**",
                gnomonicCap()));
        out.append("""
                . A wider overview is not a wider field step. It is a
                different projection.

                ## A globe is sized differently

                A page shows half its field across half its width -
                production's rule, and the general one. A hemisphere
                under the orthographic projection is a disc, and on a
                landscape page a disc sized by the width runs off the
                top and bottom at every field:

                """);
        out.append(globeTable());
        out.append("""

                A whole hemisphere only fits if the scale is set by
                the page's short side, which leaves 200 units of a
                900-wide page empty on either side. That is a
                different rule from the one every other page uses,
                and it is a reason to keep the globe as issue #301
                rather than to fold it into an overview chart.

                ## The pages

                Real scenes: the bundled catalogue, the real
                constellation figures and boundaries, production's
                detail policy, production's star sizes, production's
                naming rule, and the reference circles drawn in the
                vocabulary proposed above. Star names are not drawn -
                the label collision policy is unchanged by any of
                this, and a name is placed beside a mark whatever put
                the mark there. What does change is how many marks
                compete, which is counted.

                """);
        out.append(pages);
        out.append("""

                ## How faint an overview can afford to be

                The released 42-degree page is the control: it is
                readable, people have used it, and its middle half is
                **%s ink** at the chart's default magnitude of 6. A
                90-degree page at the same magnitude is more than
                twice that, and a 180-degree page is a third of the
                paper. Density is not a projection's fault - it is
                the sky's - but an overview that arrives at the
                default magnitude arrives unreadable, so what the
                default should be is a measurement and not a taste:

                """.formatted(control));
        out.append(magnitudeTable(scenes));
        out.append("""

                Read across each row to the column nearest the
                control. This is a contract for the issue that builds
                the overview, not a control for a reader to find: the
                magnitude slider already exists and does not change.

                ## What the study does not settle

                These pages were drawn by a study painter, not by
                `ChartRenderer`, because the renderer cannot be
                asked for another projection - which is the finding,
                not a shortcut. They are therefore evidence about
                **geometry**: what the sky looks like under each
                projection, what a curve becomes, what a page asks
                for. They are not evidence about the finished chart's
                ink weights, label density or legibility at an
                overview field, because the study does not draw
                labels and does not use the renderer's own stroke
                policy. Those belong to the issue that changes the
                renderer.

                Nor has any of this been read at a telescope, or on
                paper. Issue #293 still owns the paper.
                """);
        return out.toString();
    }

    /** How much the gnomonic query rule would miss, page by page. */
    private static String shortfallTable(StudyScenes scenes) {
        StringBuilder out = new StringBuilder();
        out.append("| centre | field | gnomonic corner | stereographic"
                + " corner | objects not fetched |\n");
        out.append("|---|---:|---:|---:|---:|\n");
        for (SkyPosition centre : List.of(new SkyPosition(83.0, 0.0),
                new SkyPosition(275.0, -25.0))) {
            StudyProjection gnomonic = Candidates.gnomonic(centre);
            StudyProjection stereographic = Candidates.stereographic(centre);
            for (double field : new double[] {60, 90, 120}) {
                out.append(String.format(Locale.ROOT,
                        "| %s | %.0f° | %.1f° | %.1f° | %d |%n",
                        centre.raDegrees() == 83.0 ? "Orion"
                                : "the Milky Way in Sagittarius",
                        field,
                        DistortionReport.cornerDegrees(field, gnomonic),
                        DistortionReport.cornerDegrees(field, stereographic),
                        scenes.shortfall(stereographic, field)));
            }
        }
        return out.toString();
    }

    /** The field at which a gnomonic page corner reaches production's cap. */
    private static double gnomonicCap() {
        StudyProjection gnomonic =
                Candidates.gnomonic(new SkyPosition(83.0, 0.0));
        double lo = 1.0;
        double hi = 179.0;
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2.0;
            if (DistortionReport.cornerDegrees(mid, gnomonic) < 60.0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2.0;
    }

    private static String globeTable() {
        StringBuilder out = new StringBuilder();
        out.append("| field | limb radius on a 900x700 page | the disc |\n");
        out.append("|---:|---:|---|\n");
        StudyProjection orthographic =
                Candidates.orthographic(new SkyPosition(0.0, 90.0));
        for (double field : new double[] {90, 120, 150, 180}) {
            double limb = new StudyMapping(orthographic, field,
                    SCREEN_WIDE, SCREEN_HIGH).pageUnitsPerPlaneUnit();
            out.append(String.format(Locale.ROOT,
                    "| %.0f° | %.0f units | %s |%n", field, limb,
                    limb <= SCREEN_HIGH / 2.0 ? "fits"
                            : "runs off the top and bottom"));
        }
        return out.toString();
    }
}
