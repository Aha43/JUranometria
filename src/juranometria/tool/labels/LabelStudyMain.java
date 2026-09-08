package juranometria.tool.labels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.tool.WiderFieldStudyMain;
import juranometria.tool.labels.Participant.Family;

/**
 * How labels share an atlas page, measured before anything is changed
 * (Sprint 31, issue #310).
 *
 * <p>Writes {@code docs/studies/label-placement/measurements.md} and
 * the pages it argues from. Nothing here is production code and
 * nothing here changes any: the atlas draws exactly what it drew
 * before this package existed, and the candidate policies below are
 * this study's own, painted onto production's own chart so they can be
 * held to the same evidence as the released page.
 *
 * <p>Run with {@code make label-study}.
 */
public final class LabelStudyMain {

    private LabelStudyMain() {
    }

    private static final File DIR = new File("docs/studies/label-placement");

    /** The pages the candidate policies are worked out on. */
    private static final List<String> CANDIDATE_PAGES = List.of(
            "home", "orion-36", "orion-90", "orion-120",
            "sagittarius-120", "crux-90");

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        StringBuilder out = new StringBuilder();
        List<StudyPages.Look> corpus = StudyPages.corpus();
        Map<String, Census> censuses = new LinkedHashMap<>();
        for (StudyPages.Look look : corpus) {
            censuses.put(look.page().slug(), new Census(look.page()));
        }

        preface(out);
        corpusTable(out, corpus);
        inventory(out, censuses);
        censusTables(out, corpus, censuses);
        observedDefects(out, corpus, censuses);
        fixture(out);
        candidates(out, corpus, censuses);
        stability(out);
        exportSection(out);
        pages(out, corpus, censuses);

        int renders = 0;
        for (Census census : censuses.values()) {
            renders += census.renders();
        }
        out.append("\n## What this cost to measure\n\n");
        out.append(String.format(Locale.ROOT,
                "%d pages and %d painted renders for the census alone."
                        + " Every collision in this\ndocument is one of"
                        + " those renders differenced against another,"
                        + " which is what a\ncollision nobody can"
                        + " dispute costs.\n\nNo wall-clock time is"
                        + " recorded here: a millisecond is a fact about"
                        + " a machine\nrather than about the atlas, and"
                        + " this file has to reproduce itself byte for"
                        + " byte.\nThe placement policies' cost is"
                        + " reported above as candidates examined, and"
                        + " their\nmeasured runtime is in"
                        + " `docs/decisions/label-placement.md`, which"
                        + " says which machine\nit was taken on.\n",
                corpus.size(), renders));
        System.out.print(out);
    }

    private static void preface(StringBuilder out) {
        out.append("# How labels share an atlas page\n\n");
        out.append("What this file is: the measurement behind issue"
                + " #310, Sprint 31's cartography and\narchitecture"
                + " gate. It inventories what the atlas does with text"
                + " today, measures\nwhere that goes wrong, and compares"
                + " the candidate policies for #313 to build\nagainst."
                + " It changes no production behaviour and no released"
                + " page.\n\n");
        out.append("Recorded on: `" + WiderFieldStudyMain.platform()
                + "`\n\n");
        out.append("## What counts as a collision here\n\n");
        out.append("Two rules, and both of them cost this study a"
                + " rewrite.\n\n");
        out.append("**A box is not ink.** \"Nunki σ\" reserves a box 45"
                + " pixels wide of which the letters\nare about half,"
                + " and a star's mark is a disc in a square that is"
                + " empty at the\ncorners. Counting overlapping"
                + " rectangles reports collisions a reader cannot"
                + " see.\n\n");
        out.append("**Ink at a place is not that thing's ink.** A"
                + " figure line, a boundary, a grid line\nand a star all"
                + " cross the same page, and any of them will answer for"
                + " another if\nall that is asked is whether something"
                + " is there.\n\n");
        out.append("So every collision below is measured by taking the"
                + " participants away, and by taking\nthem away"
                + " *separately*:\n\n");
        out.append("```\none   = page without other  -  page without"
                + " both\nother = page without one    -  page without"
                + " both\ncollision = one ∩ other\n```\n\n");
        out.append("Each piece is measured with the other absent, which"
                + " matters twice. Ink drawn\nlater hides ink drawn"
                + " earlier, so measuring both on the finished page"
                + " would make\ntheir visible sets disjoint by"
                + " construction and find nothing at all. And ink"
                + " drawn\nover ink of its own colour changes no pixel,"
                + " so measuring the later one on the\nfinished page"
                + " quietly loses the pixels where a dark glyph lands on"
                + " a dark line -\nwhich on a crowded page is most of"
                + " the collision.\n\n");
        out.append("An earlier draft of this study did both, and its own"
                + " paint-order check caught it: a\nboundary appeared to"
                + " be covering nine pixels of a label that was covering"
                + " five of\nit, which is impossible for ink underneath."
                + " The same check found a second one -\nwithholding a"
                + " star to measure its disc was taking that star's"
                + " *name* with it, so\na name across another star's"
                + " mark and a name across another star's name were"
                + " the\nsame reading. Both are the gate's own rule"
                + " turned on the gate: an oracle that cannot"
                + "\ndistinguish two causes will credit the wrong"
                + " one.\n\n");
        out.append("Both participants are named by construction: each"
                + " because taking it away changed\nthose pixels while"
                + " the other was not there to. Rectangles are still"
                + " used, but only\nto **nominate** pairs worth"
                + " painting; nothing reaches a table below that a"
                + " nomination\nalone decided.\n\n");
    }

    private static void corpusTable(StringBuilder out,
                                    List<StudyPages.Look> corpus) {
        out.append("## The pages\n\n");
        out.append("| page | centre | field | limit | extent | why |\n");
        out.append("|---|---|---:|---:|---|---|\n");
        for (StudyPages.Look look : corpus) {
            ChartScene scene = look.page().scene();
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %.1f°, %+.1f° | %.0f° | V %.1f | %s | %s |%n",
                    look.page().slug(),
                    scene.viewport().centre().raDegrees(),
                    scene.viewport().centre().decDegrees(),
                    scene.viewport().fieldWidthDegrees(),
                    scene.limitingMagnitude(), look.extent(), look.why()));
        }
        out.append("\nEvery page is assembled by the production"
                + " assembler and painted by the production\nrenderer,"
                + " through the same reference layer the chart component"
                + " hands it. The\nA4 and Letter extents are the chart"
                + " areas the sheet writer actually draws into.\n\n");
        out.append("The atlas's chart text does **not** follow the"
                + " application's text-size setting:\nthe renderer's"
                + " label face is a fixed 11-point sans and the"
                + " constellation-name\nface a fixed 12-point sans, so"
                + " enlarging application text changes the interface"
                + "\naround the chart and not the chart. What does"
                + " change placement is the room:\n`orion-120-small` is"
                + " the same sky in a 700×500 window.\n\n");
    }

    private static void inventory(StringBuilder out,
                                  Map<String, Census> censuses) {
        out.append("## The existing truth, family by family\n\n");
        out.append("| family | anchor | eligibility | may move |"
                + " omission | clipping | draw order | option |\n");
        out.append("|---|---|---|---|---|---|---:|---|\n");
        out.append("| searched target | the target's own mark |"
                + " exempt from every magnitude threshold | no |"
                + " never omitted; drawn first and reserves its box |"
                + " page rectangle | 8 | none: a target is always"
                + " named |\n");
        out.append("| star name, letter, number | east of the star's"
                + " disc at its magnitude radius | `StarLabelPolicy`"
                + " limits by field band | no |"
                + " omitted when its box meets an accepted box |"
                + " refused off-page | 8 | Star names, Bayer letters,"
                + " Flamsteed numbers |\n");
        out.append("| deep-sky label | east of the symbol's own"
                + " half-extent | `RegionalDetailPolicy` |"
                + " no | never: it is placed before star labels and"
                + " they yield to it | none | 9 | Deep-sky labels |\n");
        out.append("| constellation name | centroid of the"
                + " constellation's **visible figure ink** | a figure"
                + " that leaves ink | no | never omitted |"
                + " clips at the page edge by decision | 4 |"
                + " Constellation names, and figures |\n");
        out.append("| equatorial grid notation | the page edge the"
                + " line leaves by | spacing policy | no |"
                + " omitted when it meets the title block or key |"
                + " kept inside the paper | 1 | Equatorial grid |\n");
        out.append("| meridian, horizon, ecliptic names | the upper"
                + " end of the curve's own run | the module's |"
                + " down the edge, past names already written | omitted"
                + " when there is no room left below | paper rectangle"
                + " | 5 | the module's own control |\n");
        out.append("| title block, magnitude key | the page's own"
                + " corners | the reader's switch | no | omitted when"
                + " the page is too small | none | 10, 11 | Title"
                + " block, Magnitude key |\n");
        out.append("\nDraw order is the renderer's own sequence, and"
                + " it decides who covers whom.\n\n");
        out.append("### What each family actually avoids\n\n");
        out.append("Not read off the code: counted. A pair that"
                + " collides on these pages is a pair\nthat is not being"
                + " avoided, and a pair that never collides is either"
                + " avoided or\nnever near. The distinction is in the"
                + " footnote under the table.\n\n");
        Map<Family, Map<Family, Integer>> matrix = new LinkedHashMap<>();
        for (Census census : censuses.values()) {
            for (Attribution.Meeting meeting : census.collisions()) {
                matrix.computeIfAbsent(meeting.over().family(),
                        key -> new LinkedHashMap<>())
                        .merge(meeting.under().family(), 1, Integer::sum);
                matrix.computeIfAbsent(meeting.under().family(),
                        key -> new LinkedHashMap<>())
                        .merge(meeting.over().family(), 1, Integer::sum);
            }
        }
        List<Family> columns = new ArrayList<>(Painting.order());
        out.append("| text family |");
        for (Family column : columns) {
            out.append(' ').append(shortName(column)).append(" |");
        }
        out.append('\n').append("|---|");
        out.append("---:|".repeat(columns.size())).append('\n');
        for (Family row : columns) {
            if (!row.isText()) {
                continue;
            }
            out.append("| ").append(shortName(row)).append(" |");
            for (Family column : columns) {
                int count = matrix.getOrDefault(row, Map.of())
                        .getOrDefault(column, 0);
                out.append(' ').append(row == column ? "—"
                        : count == 0 ? "·" : String.valueOf(count))
                        .append(" |");
            }
            out.append('\n');
        }
        out.append("\nA dot is no collision anywhere in the corpus."
                + " Star labels and deep-sky labels\nnever meet because"
                + " the star-label pass yields to the deep-sky boxes"
                + " before it\nplaces anything; star labels never meet"
                + " each other for the same reason; and\nneither meets"
                + " the title block or the key, which they both reserve."
                + " Everything\nelse is a collision nobody is"
                + " preventing.\n\n");
        out.append("Two families cannot be separated by this study and"
                + " should be separable by\n#313. The equatorial grid"
                + " draws its lines and its edge notation under one"
                + " reader\nswitch, and the reference layer its curves"
                + " and their names under another, so\nneither can be"
                + " withheld apart from the other and this document"
                + " reports each as\none participant. The star-label"
                + " pass has published its decisions since #154;"
                + " the\nother text families have not.\n\n");
    }

    private static String shortName(Family family) {
        return switch (family) {
            case STAR_LABEL -> "star label";
            case STAR_DISC -> "star disc";
            case CONSTELLATION_NAME -> "const. name";
            case DEEP_SKY_LABEL -> "dso label";
            case DEEP_SKY_SYMBOL -> "dso symbol";
            case FIGURE_LINE -> "figure";
            case BOUNDARY_LINE -> "boundary";
            case GRID_INK -> "grid";
            case TITLE_BLOCK -> "title";
            case MAGNITUDE_KEY -> "key";
            case REFERENCE_INK -> "reference";
            case SELECTION_RING -> "ring";
        };
    }

    private static void censusTables(StringBuilder out,
                                     List<StudyPages.Look> corpus,
                                     Map<String, Census> censuses) {
        out.append("## What the atlas draws today\n\n");
        out.append("| page | text drawn | collisions | pixels |"
                + " worst single | order check |\n");
        out.append("|---|---:|---:|---:|---:|---:|\n");
        int totalCollisions = 0;
        int totalPixels = 0;
        double worstRatio = 0.0;
        for (StudyPages.Look look : corpus) {
            Census census = censuses.get(look.page().slug());
            int pixels = 0;
            int worst = 0;
            for (Attribution.Meeting meeting : census.collisions()) {
                pixels += meeting.where().pixels();
                worst = Math.max(worst, meeting.where().pixels());
            }
            double ratio = census.worstOrderRatio();
            worstRatio = Math.max(worstRatio, ratio);
            totalCollisions += census.collisions().size();
            totalPixels += pixels;
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %d | %d | %d | %.2f |%n",
                    look.page().slug(), census.text().size(),
                    census.collisions().size(), pixels, worst, ratio));
        }
        out.append(String.format(Locale.ROOT,
                "| **all %d pages** | | **%d** | **%d** | | **%.2f** |%n",
                corpus.size(), totalCollisions, totalPixels, worstRatio));
        out.append("\nThe collision measure does not depend on who is"
                + " on top: each participant's ink is\nmeasured with the"
                + " other absent, and the collision is the intersection."
                + " Which of the\ntwo a reader sees is the renderer's"
                + " own drawing sequence, and the last column checks"
                + "\nthat sequence rather than trusting it. Of the"
                + " shared pixels, each participant is\nasked how many"
                + " it is still holding on the finished page - how many"
                + " its removal\nwould change - and the one drawn later"
                + " should hold at least as many. The column\nis the"
                + " worst ratio of earlier-held to later-held over the"
                + " page's family pairs.\nAnything above one would mean"
                + " this study reports its collisions the wrong way"
                + "\nround.\n\n");
        out.append("Where it sits a little above one, the two families"
                + " share a tone. A constellation\nname and a figure"
                + " line are drawn in the *same* grey, so a name laid"
                + " on that line\nchanges no pixel and the measurement"
                + " credits the line with holding what the name\nis"
                + " covering. The atlas's text inks, for reference:"
                + " star and deep-sky labels\n`#222222`, constellation"
                + " names and figure lines `#787878`, star marks"
                + " `#000000`,\ngrid and reference notation lighter"
                + " still.\n\n");

        out.append("### By pair\n\n");
        Map<String, int[]> byPair = new TreeMap<>();
        for (Census census : censuses.values()) {
            for (Attribution.Meeting meeting : census.collisions()) {
                int[] tally = byPair.computeIfAbsent(
                        shortName(meeting.over().family()) + " over "
                                + shortName(meeting.under().family()),
                        key -> new int[2]);
                tally[0]++;
                tally[1] += meeting.where().pixels();
            }
        }
        out.append("| what covers what | how often | pixels |\n");
        out.append("|---|---:|---:|\n");
        byPair.entrySet().stream()
                .sorted((one, other) -> other.getValue()[0]
                        - one.getValue()[0])
                .forEach(entry -> out.append(String.format(Locale.ROOT,
                        "| %s | %d | %d |%n", entry.getKey(),
                        entry.getValue()[0], entry.getValue()[1])));
        out.append("\n");
    }

    private static void observedDefects(StringBuilder out,
                                        List<StudyPages.Look> corpus,
                                        Map<String, Census> censuses) {
        out.append("## The two defects the owner saw\n\n");
        out.append("These are the gate's first priority, and they are"
                + " counted apart from everything\nelse rather than"
                + " folded into a total that a hairline crossing could"
                + " dominate.\n\n");
        out.append("| page | const. name ↔ star label | star label over"
                + " an unrelated disc | its own disc |\n");
        out.append("|---|---:|---:|---:|\n");
        int namesAgainstLabels = 0;
        int labelsOverDiscs = 0;
        for (StudyPages.Look look : corpus) {
            Census census = censuses.get(look.page().slug());
            int names = 0;
            int discs = 0;
            for (Attribution.Meeting meeting : census.collisions()) {
                Family over = meeting.over().family();
                Family under = meeting.under().family();
                if (over == Family.STAR_LABEL
                        && under == Family.CONSTELLATION_NAME
                        || over == Family.CONSTELLATION_NAME
                                && under == Family.STAR_LABEL) {
                    names++;
                }
                if (over == Family.STAR_LABEL
                        && under == Family.STAR_DISC) {
                    discs++;
                }
            }
            namesAgainstLabels += names;
            labelsOverDiscs += discs;
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %d | excluded |%n",
                    look.page().slug(), names, discs));
        }
        out.append(String.format(Locale.ROOT,
                "| **all pages** | **%d** | **%d** | |%n",
                namesAgainstLabels, labelsOverDiscs));
        out.append("\nThe last column says what is deliberately not"
                + " counted: a star's own disc, which its\nname is"
                + " anchored beside by decision. Counting it would bury"
                + " the defect the owner\nreported - a name across"
                + " somebody *else's* star - under one entry per label"
                + " on\nthe page.\n\n");
        out.append("A constellation name is also covered by star"
                + " **discs**, which is the same defect\nwith the layers"
                + " the other way up: the name is drawn during the"
                + " geography pass\nand every mark on the page is drawn"
                + " after it.\n\n");
    }

    private static void fixture(StringBuilder out) {
        out.append("## The named fixture: Nunki against Namalsadirah\n\n");
        out.append("| star | catalogue | chart identity | V | drawn"
                + " radius |\n");
        out.append("|---|---|---|---:|---:|\n");
        Census census = new Census(fixturePage());
        Map<String, Star> stars = new LinkedHashMap<>();
        for (Star star : fixturePage().scene().stars()) {
            if (star.id().equals(StudyPages.NUNKI)
                    || star.id().equals(StudyPages.NAMALSADIRAH)) {
                stars.put(star.id(), star);
            }
        }
        for (Map.Entry<String, Star> entry : stars.entrySet()) {
            Star star = entry.getValue();
            out.append(String.format(Locale.ROOT,
                    "| %s | `%s` | %s | %.2f | %.2f px |%n",
                    star.identity() == null ? "—"
                            : star.identity().name() == null
                                    ? star.identity().bayer()
                                    : star.identity().name(),
                    star.id(), chartIdentity(star), star.magnitude(),
                    StarSizePolicy.DEFAULT.radiusFor(star.magnitude())));
        }
        out.append("\nOn the production `sagittarius-120` page, the"
                + " four pieces the fixture asks to be\nkept apart, each"
                + " one measured by withholding it and painting the page"
                + " again:\n\n");
        out.append("| piece | withheld by | ink | meets Nunki's"
                + " label |\n");
        out.append("|---|---|---:|---|\n");
        Participant nunkiLabel = new Participant(Family.STAR_LABEL,
                StudyPages.NUNKI, "Nunki σ");
        // Withholding a star takes its name with it, so each mark is
        // told what else its removal takes and that ink is subtracted
        // back out. Without this the fixture's third row would hold
        // the fourth row's glyphs.
        Participant nunkiDisc = new Participant(Family.STAR_DISC,
                StudyPages.NUNKI, "Nunki's own mark")
                .alsoTaking(nunkiLabel);
        Participant otherLabel = new Participant(Family.STAR_LABEL,
                StudyPages.NAMALSADIRAH, "φ");
        Participant otherDisc = new Participant(Family.STAR_DISC,
                StudyPages.NAMALSADIRAH, "Namalsadirah's mark")
                .alsoTaking(otherLabel);
        Attribution attribution = new Attribution(fixturePage());
        record Piece(String what, String how, Participant who,
                     boolean against) {
        }
        List<Piece> pieces = List.of(
                new Piece("Nunki's label glyphs",
                        "its star's identity set aside, its mark kept",
                        nunkiLabel, false),
                new Piece("Nunki's own disc",
                        "its star removed from the scene", nunkiDisc,
                        true),
                new Piece("Namalsadirah's disc",
                        "its star removed from the scene", otherDisc,
                        true),
                new Piece("Namalsadirah's label glyphs",
                        "its star's identity set aside", otherLabel, true));
        for (Piece piece : pieces) {
            Ink ink = attribution.inkOf(piece.who());
            String meets = "—";
            if (piece.against()) {
                Attribution.Meeting meeting = Painting.over(
                        Family.STAR_LABEL, piece.who().family())
                        ? attribution.meeting(nunkiLabel, piece.who())
                        : attribution.meeting(piece.who(), nunkiLabel);
                meets = meeting.collides()
                        ? String.format(Locale.ROOT,
                                "**yes, %d px** at %d,%d",
                                meeting.where().pixels(),
                                meeting.where().somewhere().x,
                                meeting.where().somewhere().y)
                        : "no";
            }
            out.append(String.format(Locale.ROOT, "| %s | %s | %d px | %s |%n",
                    piece.what(), piece.how(), ink.pixels(), meets));
        }
        out.append("\nThe label is anchored beside its own disc, which"
                + " is why that row is not a defect.\nThe row that is,"
                + " is Namalsadirah's: a magnitude 3.1 mark, drawn at"
                + " nearly four\npixels of radius, with a smaller star's"
                + " name written across it. The two are\nseparated here"
                + " by two different surgeries on the same scene, so"
                + " neither can be\nanswering for the other.\n\n");
        out.append("The shared area is larger than Namalsadirah's own"
                + " visible ink, and that is the\ndefect stated"
                + " arithmetically: what the mark has left on the"
                + " finished page is what\nthe name did not cover."
                + "\n\n");
        out.append("**The fourth participant is missing, and its"
                + " absence is the same defect.**\nNamalsadirah"
                + " qualifies for a Bayer letter at this field - V 3.13"
                + " against a limit of\nV 3.5, and the policy returns"
                + " \"φ\" for it - but the page does not draw one. The"
                + " star-\nlabel pass takes stars brightest first,"
                + " accepts \"Nunki σ\", and then refuses φ\nbecause"
                + " its box meets the accepted one. So the atlas"
                + " silently declines to name a\nstar in order to"
                + " protect a name it then draws across that same"
                + " star's mark. The\nomission is not recorded"
                + " anywhere; it is the pass returning early.\n\n");
        out.append("The fixture reproduces on the page a reader"
                + " reaches, not only on one contrived\ncentre. Nunki's"
                + " label box meets Namalsadirah's drawn disc at:\n\n");
        out.append(fixtureSpread());
        out.append("\n");
        out.append(census.collisions().isEmpty() ? "" : "");
    }

    private static String chartIdentity(Star star) {
        if (star.identity() == null) {
            return "—";
        }
        List<String> parts = new ArrayList<>();
        if (star.identity().bayer() != null) {
            parts.add(star.identity().bayer() + " "
                    + star.identity().constellation());
        }
        if (star.identity().flamsteed() != null) {
            parts.add("Flamsteed " + star.identity().flamsteed());
        }
        return String.join(" · ", parts);
    }

    private static Page fixturePage() {
        return new Page("sagittarius-120",
                StudyPages.assemble(new ChartViewState(
                        new SkyPosition(266.0, -28.0), 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                        StudyPages.SCREEN_WIDE, StudyPages.SCREEN_HIGH),
                ChartOptions.DEFAULTS, List.of());
    }

    /** Where the fixture's two pieces meet, over fields and extents. */
    private static String fixtureSpread() {
        StringBuilder out = new StringBuilder();
        out.append("| centre | field | 900×700 | 1100×800 |\n");
        out.append("|---|---:|---|---|\n");
        double[][] centres = {{266.0, -28.0}, {283.816320, -26.296594},
                {275.0, -25.0}};
        String[] names = {"Sagittarius", "Nunki itself", "the Milky Way"};
        var metrics = Census.Metrics.forFont(ChartRenderer.labelFont());
        for (int at = 0; at < centres.length; at++) {
            for (double field : new double[] {42.0, 60.0, 90.0, 120.0}) {
                out.append(String.format(Locale.ROOT, "| %s | %.0f° |",
                        names[at], field));
                for (int[] size : new int[][] {{900, 700}, {1100, 800}}) {
                    ChartScene scene = StudyPages.assemble(
                            new ChartViewState(new SkyPosition(
                                    centres[at][0], centres[at][1]), field,
                                    ChartViewState.defaultMagnitudeFor(field)),
                            size[0], size[1]);
                    out.append(' ').append(meets(scene, metrics)).append(" |");
                }
                out.append('\n');
            }
        }
        return out.toString();
    }

    private static String meets(ChartScene scene,
                                java.awt.FontMetrics metrics) {
        var mapping = new juranometria.project.ViewportMapping(
                scene.viewport());
        var projection = juranometria.project.Projections.forViewport(
                scene.viewport());
        var detail = new juranometria.render.RegionalDetailPolicy(scene,
                mapping.pixelsPerPlaneUnit());
        java.awt.geom.Rectangle2D box = null;
        for (ChartRenderer.StarLabelPlacement placement
                : Page.renderer().starLabelPlacements(metrics, scene,
                        ChartOptions.DEFAULTS, detail, projection, mapping)) {
            if (placement.star().id().equals(StudyPages.NUNKI)) {
                box = placement.box();
            }
        }
        if (box == null) {
            return "no label";
        }
        for (ChartRenderer.DrawnMark mark
                : Page.renderer().drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.star() != null && mark.star().id()
                    .equals(StudyPages.NAMALSADIRAH)) {
                return mark.outline().intersects(box) ? "**meets**" : "clear";
            }
        }
        return "not drawn";
    }

    private static void candidates(StringBuilder out,
                                   List<StudyPages.Look> corpus,
                                   Map<String, Census> censuses)
            throws IOException {
        out.append("## The candidate policies\n\n");
        out.append("Three variants of one deterministic greedy pass,"
                + " differing in exactly the\nquestions the gate has to"
                + " answer. Each takes the labels in a fixed priority,"
                + " gives\neach a fixed ordered list of candidate"
                + " positions around its own anchor - east\nfirst, which"
                + " is where every label sits today - and takes the"
                + " first that no\naccepted box, no drawn mark, no"
                + " furniture and no page edge refuses. A label"
                + " with\nno free candidate is omitted rather than drawn"
                + " over something.\n\n");
        out.append("Their pages are painted the same way the released"
                + " page is: production's chart\nwith the text families"
                + " switched off through the reader's own options, and"
                + " the\ncandidate's text written on top in the"
                + " renderer's own fonts and inks. They are\nthen put"
                + " through the same collision oracle, so nothing below"
                + " is a candidate\nmarking its own work.\n\n");
        List<Greedy.Rules> policies = List.of(Greedy.LABELS_FIRST,
                Greedy.NAMES_FIRST, Greedy.AVOIDING_LINES,
                Greedy.KEEPING_EVERYTHING, Greedy.LEAST_BAD);
        out.append("| page | policy | collisions | of the two"
                + " defects | lost | gained | moved | worst move |"
                + " candidates tried |\n");
        out.append("|---|---|---:|---:|---:|---:|---:|---:|---:|\n");
        Map<String, Greedy> keep = new LinkedHashMap<>();
        for (String slug : CANDIDATE_PAGES) {
            Page page = pageOf(corpus, slug);
            Census current = censuses.get(slug);
            java.util.Set<String> today = new java.util.LinkedHashSet<>();
            for (Participant drawn : current.text()) {
                today.add(drawn.family() + ":" + drawn.id());
            }
            out.append(String.format(Locale.ROOT,
                    "| `%s` | the atlas today | %d | %d | — | — | — |"
                            + " — | — |%n",
                    slug, current.collisions().size(),
                    defects(current.collisions())));
            for (Greedy.Rules rules : policies) {
                Greedy greedy = new Greedy(page, rules);
                Census after = new Census(greedy.pageWithPlacements());
                java.util.Set<String> now = new java.util.LinkedHashSet<>();
                for (Participant drawn : after.text()) {
                    now.add(drawn.family() + ":" + drawn.id());
                }
                int lost = 0;
                for (String was : today) {
                    if (!now.contains(was)) {
                        lost++;
                    }
                }
                int gained = 0;
                for (String is : now) {
                    if (!today.contains(is)) {
                        gained++;
                    }
                }
                double worstMove = 0.0;
                for (double move : greedy.moved().values()) {
                    worstMove = Math.max(worstMove, move);
                }
                out.append(String.format(Locale.ROOT,
                        "| | %s | %d | %d | %d | %d | %d | %.0f px |"
                                + " %d |%n",
                        rules.name(), after.collisions().size(),
                        defects(after.collisions()), lost, gained,
                        greedy.moved().size(), worstMove,
                        greedy.candidatesTried()));
                if (rules == Greedy.LEAST_BAD) {
                    keep.put(slug, greedy);
                }
            }
        }
        out.append("\n\"Of the two defects\" counts only the owner's"
                + " two: a constellation name and a\nstar label sharing"
                + " pixels either way up, and a star's name across"
                + " another\nstar's disc. The wider count includes every"
                + " hairline a label crosses.\n\n");
        out.append("**Lost** is the measurement that decides this."
                + " It is not \"labels the policy\nrefused\" - the"
                + " atlas refuses labels today too, silently, and"
                + " Namalsadirah's\nletter is one of them. It is the"
                + " text the released page draws that the candidate"
                + "\npage does not, counted from the two painted pages."
                + " **Gained** is the other\ndirection: text the"
                + " candidate draws that the atlas does not.\n\n");
        out.append("The wider collision count goes **up** for the last"
                + " two policies on the widest pages,\nand that is worth"
                + " looking at rather than hiding: a label moved off a"
                + " star's mark has\nto go somewhere, and on a"
                + " 120-degree page what is everywhere else is the"
                + " graticule\nand the figures. `orion-120` trades 37"
                + " of the owner's defects for 2, and pays for it\nwith"
                + " label-over-hairline crossings. That is the right"
                + " trade and the gate should say\nso explicitly: a"
                + " name across a star hides a fact, a name across a"
                + " grid line hides a\ngrid line, and the two are not"
                + " commensurable. A policy that chased the total would"
                + "\nchoose the third, which reaches nearly zero by"
                + " refusing to draw a third of the\ntext.\n\n");
        out.append("The first three policies buy their repair with"
                + " omissions, and on the widest pages\nthe price is a"
                + " third of the text a reader has today. The last two"
                + " pay nothing: a\nlabel with no free candidate is"
                + " still drawn, so no page loses anything it has."
                + "\nThey differ in where it goes - back to its own"
                + " anchor, which is where the released\npage puts it,"
                + " or to whichever of its eight candidates covers the"
                + " least ink.\n\n");
        out.append("### May a constellation name move?\n\n");
        out.append("| page | names placed | moved | worst move | left"
                + " their own figure's ink |\n");
        out.append("|---|---:|---:|---:|---:|\n");
        for (Map.Entry<String, Greedy> entry : keep.entrySet()) {
            Greedy greedy = entry.getValue();
            int names = 0;
            int movedNames = 0;
            double worst = 0.0;
            for (PlacedText text : greedy.placed()) {
                if (text.family() != Family.CONSTELLATION_NAME) {
                    continue;
                }
                names++;
                Double move = greedy.moved().get(
                        Family.CONSTELLATION_NAME + ":" + text.id());
                if (move != null) {
                    movedNames++;
                    worst = Math.max(worst, move);
                }
            }
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %d | %.0f px | %d |%n",
                    entry.getKey(), names, movedNames, worst,
                    greedy.namesOffTheirOwnFigure()));
        }
        out.append("\nA name that leaves its own figure's ink is naming"
                + " the wrong part of the sky, which\nis worse than"
                + " sitting on a star. The measurement above is what"
                + " decides how far a\nname may be allowed to go.\n\n");
    }

    private static int defects(List<Attribution.Meeting> collisions) {
        int found = 0;
        for (Attribution.Meeting meeting : collisions) {
            Family over = meeting.over().family();
            Family under = meeting.under().family();
            if (over == Family.STAR_LABEL && under == Family.CONSTELLATION_NAME
                    || over == Family.CONSTELLATION_NAME
                            && under == Family.STAR_LABEL
                    || over == Family.STAR_DISC
                            && under == Family.CONSTELLATION_NAME
                    || over == Family.STAR_LABEL
                            && under == Family.STAR_DISC) {
                found++;
            }
        }
        return found;
    }

    private static Page pageOf(List<StudyPages.Look> corpus, String slug) {
        for (StudyPages.Look look : corpus) {
            if (look.page().slug().equals(slug)) {
                return look.page();
            }
        }
        throw new IllegalArgumentException("no such page: " + slug);
    }

    private static void stability(StringBuilder out) {
        out.append("## Stability under a small navigation change\n\n");
        out.append("A label that jumps to the other side of its star"
                + " when the reader nudges the page\nis worse than one"
                + " that sits still in a slightly worse place. Each page"
                + " below is\nplaced, then placed again one pan step"
                + " east (a twentieth of the field) and one\nrung deeper"
                + " on the zoom ladder, and the labels that changed"
                + " candidate are\ncounted.\n\n");
        out.append("| page | labels | changed after a pan | after a"
                + " zoom |\n");
        out.append("|---|---:|---:|---:|\n");
        for (String slug : List.of("orion-90", "sagittarius-120",
                "home")) {
            Page page = pageOf(StudyPages.corpus(), slug);
            Greedy here = new Greedy(page, Greedy.LEAST_BAD);
            double field = page.scene().viewport().fieldWidthDegrees();
            SkyPosition centre = page.scene().viewport().centre();
            Page panned = page.withScene(StudyPages.assemble(
                    new ChartViewState(new SkyPosition(
                            centre.raDegrees() + field / 20.0,
                            centre.decDegrees()), field,
                            page.scene().limitingMagnitude()),
                    page.wide(), page.high()));
            Page zoomed = page.withScene(StudyPages.assemble(
                    new ChartViewState(centre, nextRung(field),
                            ChartViewState.defaultMagnitudeFor(
                                    nextRung(field))),
                    page.wide(), page.high()));
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %d | %d |%n", slug, here.placed().size(),
                    changedSide(here, new Greedy(panned,
                            Greedy.LEAST_BAD)),
                    changedSide(here, new Greedy(zoomed,
                            Greedy.LEAST_BAD))));
        }
        out.append("\nCounted as: a label that both pages draw, whose"
                + " offset from its own anchor changed\nsign in x or in"
                + " y - it moved to the other side of the thing it"
                + " names.\n\n");
    }

    private static double nextRung(double field) {
        double[] ladder = {120, 90, 60, 42, 36, 24, 18, 12, 8, 6, 4, 3, 2, 1};
        for (int at = 0; at < ladder.length - 1; at++) {
            if (ladder[at] == field) {
                return ladder[at + 1];
            }
        }
        return field;
    }

    private static int changedSide(Greedy before, Greedy after) {
        Map<String, PlacedText> was = new LinkedHashMap<>();
        for (PlacedText text : before.placed()) {
            was.put(text.family() + ":" + text.id(), text);
        }
        int changed = 0;
        for (PlacedText now : after.placed()) {
            PlacedText then = was.get(now.family() + ":" + now.id());
            if (then == null) {
                continue;
            }
            boolean xFlipped = Math.signum(then.x() - then.anchorX())
                    != Math.signum(now.x() - now.anchorX());
            boolean yFlipped = Math.signum(then.baseline() - then.anchorY())
                    != Math.signum(now.baseline() - now.anchorY());
            if (xFlipped || yFlipped) {
                changed++;
            }
        }
        return changed;
    }

    private static void exportSection(StringBuilder out) {
        out.append("## Screen and paper\n\n");
        out.append("The sheet writers replay one recording of one"
                + " production render (#285, #298), so\nSVG, PDF and PNG"
                + " carry whatever the renderer decided and no placement"
                + " question\nis answered twice. What differs between"
                + " screen and paper is the extent, and the\nextent"
                + " changes placement: `orion-42` at 900×700, at the A4"
                + " chart area and at\nthe Letter chart area are three"
                + " different pages of the same sky, measured\nabove."
                + "\n\n");
    }

    private static void pages(StringBuilder out,
                              List<StudyPages.Look> corpus,
                              Map<String, Census> censuses)
            throws IOException {
        out.append("## The pages themselves\n\n");
        List<String> written = new ArrayList<>();
        for (String slug : List.of("sagittarius-120", "orion-90", "home")) {
            Page page = pageOf(corpus, slug);
            write(slug + "-today.png", censuses.get(slug).base());
            written.add(slug + "-today.png");
            Greedy greedy = new Greedy(page, Greedy.LEAST_BAD);
            write(slug + "-candidate.png",
                    greedy.pageWithPlacements().paint());
            written.add(slug + "-candidate.png");
        }
        for (String name : written) {
            out.append("- `").append(name).append("`\n");
        }
        out.append("\nThe two `sagittarius-120` pages are the fixture's"
                + " own: the released page with\nNunki's name across"
                + " Namalsadirah, and the same page with the candidate"
                + " policy\nplacing it. `home` is the control - the page"
                + " a reader opens the atlas on, which\nmust not get"
                + " worse.\n\n");
    }

    private static void write(String name, java.awt.image.BufferedImage image)
            throws IOException {
        ImageIO.write(image, "png", new File(DIR, name));
    }
}
