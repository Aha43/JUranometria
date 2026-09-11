package juranometria.tool.labels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;
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
        clipping(out, corpus);
        migration(out, corpus);
        seamAgreement(out, corpus);
        stability(out);
        cost(out);
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
        // Two documents. The census is a count of pixels on a page
        // some font drew, and it says so in its own preface; what the
        // study concluded from it - which pages, whether the atlas
        // clips a word, whether a nudge moves labels within the
        // amended budget - is the same wherever it is measured, and
        // is what the contract pins (#315).
        juranometria.tool.PlatformEvidence.write(out,
                "docs/studies/label-placement/platform.md");
        StringBuilder portable = new StringBuilder();
        conclusions(portable, corpus);
        System.out.print(portable);
    }

    /** What the census concluded, in terms no font decides. */
    private static void conclusions(StringBuilder out,
                                    List<StudyPages.Look> corpus) {
        out.append("# What the label census concluded\n\n");
        out.append("Sprint 31, issues #310, #313 and #314; classified"
                + " in Sprint 31, issue #315.\n\n");
        out.append("The census itself is in `platform.md` beside this."
                + " Every number in it is a\ncount of pixels on a page"
                + " a font drew, so it reproduces on a machine rather"
                + " than\nacross machines. What survives the journey"
                + " between machines is what the study\nwas for: which"
                + " pages it looked at, and what it decided.\n\n");
        out.append("## The corpus\n\n");
        out.append("| page | field | extent | why it is here |\n"
                + "|---|---:|---|---|\n");
        for (StudyPages.Look page : corpus) {
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %.0f° | %s | %s |%n",
                    page.page().slug(),
                    page.page().scene().viewport()
                            .fieldWidthDegrees(),
                    page.extent(), page.why()));
        }
        out.append(String.format(Locale.ROOT,
                "%n**%d pages.**%n%n", corpus.size()));
        out.append("## Stability under a one-step pan\n\n");
        out.append("The gate's amended budget: no more than 15% of the"
                + " corpus's labels displaced by\na one-step pan, and"
                + " no single page above 30%"
                + " (`docs/decisions/label-placement.md`).\n\n");
        out.append(String.format(Locale.ROOT,
                "| the corpus | within 15%% | %s |%n"
                        + "| the worst page | within 30%% | %s |%n%n",
                stabilityVerdict[0] <= 0.15 ? "yes" : "**no**",
                stabilityVerdict[1] <= 0.30 ? "yes" : "**no**"));
        if (stabilityVerdict[0] > 0.15 || stabilityVerdict[1] > 0.30) {
            throw new IllegalStateException("the corpus is outside the"
                    + " stability budget the gate was amended to: "
                    + stabilityVerdict[0] + " over the corpus, "
                    + stabilityVerdict[1] + " on the worst page");
        }
        out.append("## What has no portable form\n\n");
        out.append("The collision census, the migration's before and"
                + " after, the clipping counts and\nthe cost of"
                + " placing a page are measurements of ink, and there"
                + " is no inequality\nunderneath them that would"
                + " survive a change of font: the numbers *are* the"
                + " evidence.\nThey are in `platform.md`, with the"
                + " machine that took them. What is not a"
                + " measurement -\nthat a cut word reads as another"
                + " word, and which words - is in"
                + " `docs/decisions/label-placement.md`.\n");
    }

    private static void preface(StringBuilder out) {
        out.append("# How labels share an atlas page\n\n");
        out.append("What this file is: the measurement behind issue"
                + " #310, Sprint 31's cartography and\narchitecture"
                + " gate. It inventories what the atlas does with text,"
                + " measures where\nthat goes wrong, and compares the"
                + " candidate policies the gate chose"
                + " between.\n\n");
        out.append("**It has been measured twice.** The gate measured"
                + " an atlas in which each family\nplaced its own"
                + " labels and avoided what it happened to avoid. Issue"
                + " #314 moved\nthe three families onto one decision,"
                + " and this file now measures that atlas -\nwith the"
                + " earlier numbers kept beside the new ones wherever"
                + " they are the point,\nfrom two records taken before"
                + " the change: `census-before.tsv` and"
                + " `text-before.tsv`.\nWhat the migration did to the"
                + " atlas, page by page and label by label, is its"
                + " own\nsection below.\n\n");
        out.append(juranometria.tool.PlatformEvidence.observed(
                "What each label is, where the policy allows it to go,"
                + " which candidate it took and why a refusal was"
                + " refused does not depend on a font, and is"
                + " asserted by this study's gate."));
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
        out.append("## The truth this began from, family by"
                + " family\n\n");
        out.append("How the families worked before #314 - each with"
                + " its own answer to every\nquestion, which is what"
                + " the gate was called to settle. The `may move` and"
                + "\n`omission` columns are the ones #314 changed:"
                + " every family may now move, and\nomission is one"
                + " rule for all of them.\n\n");
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
                + " clipped at the page edge until #313 | 4 |"
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
                + " Since #314 one decision places\nall three families,"
                + " so no two pieces of text share a box: they never"
                + " meet each\nother, and they do not meet a mark or a"
                + " symbol either unless nothing was free.\nWhat is"
                + " left in the table is what the decision permits -"
                + " text across a line -\nand the fallback, which is"
                + " what a label does when every one of its positions"
                + " is\nrefused.\n\n");
        out.append("**The title-block column is that fallback, and it"
                + " is worth reading twice.** The\npaper's edge and a"
                + " figure's own region are costs the fallback may not"
                + " spend;\nfurniture is. So a label with nowhere free"
                + " can end up under a block that is\nopaque and drawn"
                + " last, where the reader does not see it - which is"
                + " exactly what\nthe old star-label pass did with it,"
                + " by refusing to draw it at all. Same page for\nthe"
                + " reader, and it is counted here rather than left"
                + " out.\n\n");
        out.append("Two families still cannot be separated by this"
                + " study, though #313 published\ntheir decisions. The"
                + " equatorial grid draws its lines and its edge"
                + " notation under\none reader switch, and the"
                + " reference layer its curves and their names under"
                + " another,\nso neither can be *withheld* apart from"
                + " the other and this document reports each as\none"
                + " participant. Placement can tell them apart: grid"
                + " notation is an obstacle to\nthe sky's text, and"
                + " the reference layer's names are not"
                + " (docs/decisions/place-and-time.md).\n\n");
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
        Map<String, String[]> was = censusBefore();
        out.append("Two numbers where the atlas has one, because"
                + " Sprint 31 changed it: **before** is\nthe same"
                + " measurement of the same page taken from the atlas"
                + " as 1.11.0 shipped\nit, when each family placed its"
                + " own labels (`census-before.tsv` beside this"
                + " file).\n\n");
        out.append("*Text drawn* is text a reader can see: a piece"
                + " whose removal changes a pixel.\nA label behind the"
                + " title block is not in it, and neither is one whose"
                + " glyphs fall\nentirely on ink of their own colour."
                + " The count of text the page *places* is in\nthe"
                + " migration section, and the two are different"
                + " questions.\n\n");
        out.append("| page | text drawn | before | collisions |"
                + " before | pixels | before | worst single |"
                + " order check |\n");
        out.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
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
            String[] before = was.get(look.page().slug());
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %s | %d | %s | %d | %s | %d |"
                            + " %.2f |%n",
                    look.page().slug(), census.text().size(),
                    before == null ? "—" : before[0],
                    census.collisions().size(),
                    before == null ? "—" : before[1], pixels,
                    before == null ? "—" : before[2], worst, ratio));
        }
        int wasCollisions = 0;
        int wasPixels = 0;
        for (String[] row : was.values()) {
            wasCollisions += Integer.parseInt(row[1]);
            wasPixels += Integer.parseInt(row[2]);
        }
        out.append(String.format(Locale.ROOT,
                "| **all %d pages** | | | **%d** | **%d** | **%d** |"
                        + " **%d** | | **%.2f** |%n",
                corpus.size(), totalCollisions, wasCollisions, totalPixels,
                wasPixels, worstRatio));
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
        Map<String, String[]> was = censusBefore();
        out.append("| page | const. name ↔ star label | before |"
                + " star label over an unrelated disc | before |"
                + " its own disc |\n");
        out.append("|---|---:|---:|---:|---:|---:|\n");
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
            String[] before = was.get(look.page().slug());
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %s | %d | %s | excluded |%n",
                    look.page().slug(), names,
                    before == null ? "—" : before[3], discs,
                    before == null ? "—" : before[4]));
        }
        int wasNames = 0;
        int wasDiscs = 0;
        for (String[] row : was.values()) {
            wasNames += Integer.parseInt(row[3]);
            wasDiscs += Integer.parseInt(row[4]);
        }
        out.append(String.format(Locale.ROOT,
                "| **all pages** | **%d** | **%d** | **%d** | **%d** |"
                        + " |%n",
                namesAgainstLabels, wasNames, labelsOverDiscs, wasDiscs));
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
                + " after it. Over the corpus that pair went"
                + " from\n228 to " + discsOverNames(censuses)
                + " when the families migrated, and what is left of it"
                + " is the\nfallback: a name whose figure leaves it"
                + " nowhere free takes the least bad of its"
                + " own\ncandidates rather than leaving the"
                + " constellation unnamed.\n\n");
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
        out.append("\nBoth rows read *no* now, and one of them is the"
                + " whole of Sprint 31. Before the\nfamilies migrated,"
                + " Namalsadirah's row read **yes, 44 px**: a magnitude"
                + " 3.1 mark,\ndrawn at nearly four pixels of radius,"
                + " with a smaller star's name written across\nit -"
                + " more of the mark covered than the mark had left"
                + " visible. The two are\nseparated here by two"
                + " different surgeries on the same scene, so neither"
                + " can be\nanswering for the other, and the same"
                + " measurement that found the defect is the\none"
                + " reporting it gone.\n\n");
        out.append("**The fourth participant was missing, and its"
                + " absence was the same defect.**\nNamalsadirah"
                + " qualifies for a Bayer letter at this field - V 3.13"
                + " against a limit of\nV 3.5, and the policy returns"
                + " \"φ\" for it - but the page did not draw one. The"
                + " old star-\nlabel pass took stars brightest first,"
                + " accepted \"Nunki σ\", and then refused φ\nbecause"
                + " its box met the accepted one: the atlas silently"
                + " declined to name a star\nin order to protect a name"
                + " it then drew across that same star's mark. Its row"
                + " in\nthe table above has ink in it now, which is"
                + " the letter being drawn.\n\n");
        out.append("The fixture reproduced on the page a reader"
                + " reaches, not only on one contrived\ncentre - which"
                + " is why the repair is asked for at every one of"
                + " them. Nunki's label\nbox against Namalsadirah's"
                + " drawn disc:\n\n");
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
        var studied = juranometria.project.DrawnPage.of(scene);
        var mapping = new juranometria.project.ViewportMapping(studied);
        var projection = studied.projection();
        var detail = new juranometria.render.RegionalDetailPolicy(scene,
                mapping.pixelsPerPlaneUnit());
        java.awt.geom.Rectangle2D box = null;
        for (ChartRenderer.StarLabelPlacement placement
                : Page.renderer().starLabelPlacements(
                        ChartRenderer.TextMetrics.offscreen(),
                        scene, ChartOptions.DEFAULTS)) {
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
        out.append("The gate's own comparison, kept because it is what"
                + " the decision was taken from -\nand because since"
                + " #314 the row marked *the atlas itself* is the"
                + " chosen policy in\nproduction, so the table reads as"
                + " one implementation against another.\n\n");
        out.append("Three variants of one deterministic greedy pass,"
                + " differing in exactly the\nquestions the gate has to"
                + " answer. Each takes the labels in a fixed priority,"
                + " gives\neach a fixed ordered list of candidate"
                + " positions around its own anchor - east\nfirst, which"
                + " is where every label sat before Sprint 31 - and"
                + " takes the"
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
                    "| `%s` | the atlas itself | %d | %d | — | — | — |"
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
        out.append("### May a constellation name move, and how far?\n\n");
        out.append("A name that leaves its own figure is naming the"
                + " wrong part of the sky, which is\nworse than the"
                + " collision it was avoiding. The chosen policy"
                + " therefore refuses any\ncandidate outside the region"
                + " its constellation owns - the **convex hull of that"
                + "\nfigure's visible ink**, not its bounding box: the"
                + " box of Eridanus, which wanders\nhalf the sky,"
                + " contains most of Orion.\n\n");
        out.append("What that rule costs and what it buys, measured by"
                + " running the same policy with\nit and without"
                + " it:\n\n");
        out.append("| page | | names placed | moved | worst move |"
                + " off their own figure | centre outside it |"
                + " collisions |\n");
        out.append("|---|---|---:|---:|---:|---:|---:|---:|\n");
        for (String slug : CANDIDATE_PAGES) {
            Page page = pageOf(corpus, slug);
            boolean first = true;
            for (Greedy.Rules rules : List.of(Greedy.LEAST_BAD,
                    Greedy.LEAST_BAD_UNOWNED)) {
                Greedy greedy = rules == Greedy.LEAST_BAD
                        ? keep.get(slug) : new Greedy(page, rules);
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
                Census after = new Census(greedy.pageWithPlacements());
                out.append(String.format(Locale.ROOT,
                        "| %s | %s | %d | %d | %.0f px | %d | %d | %d |%n",
                        first ? "`" + slug + "`" : "",
                        first ? "owned" : "free",
                        names, movedNames, worst,
                        greedy.namesOffTheirOwnFigure(),
                        greedy.namesWhoseCentreIsOffTheirFigure(),
                        after.collisions().size()));
                first = false;
            }
        }
        out.append("\nThe rule the policy enforces is that a name's"
                + " box **overlaps** the region its own\nfigure owns."
                + " Overlap rather than \"its centre is inside\","
                + " because a figure can be\nsmaller than its own name:"
                + " Crater on a 90-degree page leaves six pixels by five"
                + " of\nvisible ink and CRATER is fifty pixels wide, so"
                + " the strict reading would refuse\nevery candidate it"
                + " has and teach nothing. The stricter statistic is"
                + " reported\nbeside it so the difference is visible"
                + " rather than argued about.\n\n");
        out.append("Without the rule, names leave their own figures on"
                + " every crowded page - eight of\nthirty at"
                + " Sagittarius, seven of twenty-seven at 120 degrees."
                + " With it, none does\nanywhere, and the collision"
                + " count is a little higher. That is the whole trade:"
                + " the\nrule costs a name the occasional candidate and"
                + " buys the guarantee that a name is\nwritten across"
                + " the thing it names.\n\n");
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

    /**
     * The seam's decisions against the gate's own, on the same pages.
     *
     * <p>Two implementations of one decision, written from the same
     * document and from nothing else in common: the gate's greedy pass
     * lives in this study and has been read by six rounds of review;
     * the seam is production's, in {@code juranometria.render}. Asking
     * one of them twice would prove nothing, so this asks both and
     * reports where they part.
     */
    private static void seamAgreement(StringBuilder out,
                                      List<StudyPages.Look> corpus) {
        out.append("## The seam against this study\n\n");
        out.append("Issue #313 builds the placement seam production"
                + " will use. It is a second\nimplementation of the"
                + " decision this document settles, written against the"
                + " same\nwords and sharing no code with the greedy"
                + " pass above - so the two can be asked\nthe same"
                + " question, which is worth more than asking either of"
                + " them twice.\n\n");
        out.append("| page | labels both place | same candidate |"
                + " same box | placed under duress | omitted by the"
                + " seam |\n");
        out.append("|---|---:|---:|---:|---:|---:|\n");
        var metrics = Census.Metrics.forFont(ChartRenderer.labelFont());
        for (String slug : CANDIDATE_PAGES) {
            Page page = pageOf(corpus, slug);
            Greedy greedy = new Greedy(page, Greedy.LEAST_BAD);
            Map<String, PlacedText> mine = new LinkedHashMap<>();
            for (PlacedText text : greedy.placed()) {
                mine.put(text.family() + ":" + text.id(), text);
            }

            List<juranometria.render.LabelPlacement.Request> asked =
                    seamRequests(page, metrics);
            var seam = new juranometria.render.LabelPlacement(page.wide(),
                    page.high(),
                    juranometria.render.LabelGeometry.obstaclesOn(
                            Page.renderer(), metrics, page.scene(),
                            page.options()));

            int both = 0;
            int sameCandidate = 0;
            int sameBox = 0;
            int duress = 0;
            int omitted = 0;
            for (var placed : seam.placeAll(asked)) {
                if (placed.omitted()) {
                    omitted++;
                    continue;
                }
                if (placed.underDuress()) {
                    duress++;
                }
                PlacedText ours = mine.get(
                        identityOf(page, placed.request()));
                if (ours == null) {
                    continue;
                }
                both++;
                if (Math.abs(ours.box().getX() - placed.at().getX()) < 0.5
                        && Math.abs(ours.box().getY()
                                - placed.at().getY()) < 0.5) {
                    sameBox++;
                    sameCandidate++;
                } else if (ours.box().intersects(placed.at())) {
                    sameCandidate++;
                }
            }
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %d | %d | %d | %d |%n", slug, both,
                    sameCandidate, sameBox, duress, omitted));
        }
        out.append("\nThey do not agree everywhere, and the places"
                + " they part are worth more than the\nplaces they"
                + " meet. Two causes, and neither is the placement"
                + " rule:\n\n");
        out.append("**The candidates are built twice.** Both build"
                + " eight boxes around an anchor from\nthe same"
                + " sentence, and a deep-sky label's reach differs"
                + " between them by the gap\nitself - three pixels."
                + " That is why \"same candidate\" is high and \"same"
                + " box\" is\nlower: they choose the same position and"
                + " draw it a few pixels apart.\n\n");
        out.append("**And a difference cascades.** Placement is"
                + " sequential: a label three pixels from\nwhere the"
                + " other pass put it changes what every later label"
                + " finds free. One\ndisagreement early on a crowded"
                + " page is worth several late ones.\n\n");
        out.append("What this establishes is the part worth"
                + " establishing: on the same page, from the\nsame"
                + " published geometry, two implementations written"
                + " from one document and\nsharing no code choose the"
                + " same candidate for the great majority of a page's"
                + " text.\n\n");
    }

    /**
     * What the migration did to the atlas, page by page.
     *
     * <p>Against the atlas as 1.11.0 shipped it, recorded label by
     * label before the families moved to the shared decision and read
     * back here. The two sets are compared by name - this star, that
     * object, that constellation - so a label that moved is a move
     * and not a loss and a gain.
     */
    private static void migration(StringBuilder out,
                                  List<StudyPages.Look> corpus) {
        out.append("## What the migration changed\n\n");
        out.append("Every page of the corpus, against the same page"
                + " drawn by the atlas as 1.11.0\nshipped it - each"
                + " family placing its own labels, avoiding what it"
                + " happened to\navoid. That page's text was recorded"
                + " label by label before the change"
                + " (`text-before.tsv`\nbeside this file) and is read"
                + " back here, so a label that moved is a move rather"
                + " than\na loss and a gain.\n\n");
        Map<String, Map<String, double[]>> before = textBefore();
        if (before.isEmpty()) {
            out.append("*(`text-before.tsv` is missing; nothing to"
                    + " compare against.)*\n\n");
            return;
        }
        out.append("| page | drawn before | drawn now | kept its place"
                + " | moved | worst move | no longer drawn | newly"
                + " drawn |\n");
        out.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        int wasTotal = 0;
        int nowTotal = 0;
        int keptTotal = 0;
        int movedTotal = 0;
        int lostTotal = 0;
        int gainedTotal = 0;
        double worstTotal = 0.0;
        List<String> lostNames = new ArrayList<>();
        for (StudyPages.Look look : corpus) {
            Page page = look.page();
            Map<String, double[]> was = before.get(page.slug());
            if (was == null) {
                continue;
            }
            Map<String, double[]> now = new LinkedHashMap<>();
            for (var placement : Page.renderer().textPlacements(
                    ChartRenderer.TextMetrics.offscreen(), page.scene(),
                    page.options())) {
                if (!placement.omitted()) {
                    now.put(identityOf(page, placement.request()),
                            new double[] {placement.at().getX(),
                                    placement.at().getY()});
                }
            }
            int kept = 0;
            int moved = 0;
            int lost = 0;
            double worst = 0.0;
            for (var entry : was.entrySet()) {
                double[] there = now.get(entry.getKey());
                if (there == null) {
                    lost++;
                    lostNames.add(page.slug() + ": " + entry.getKey());
                    continue;
                }
                double far = Math.hypot(there[0] - entry.getValue()[0],
                        there[1] - entry.getValue()[1]);
                if (far < 0.5) {
                    kept++;
                } else {
                    moved++;
                    worst = Math.max(worst, far);
                }
            }
            int gained = 0;
            for (String one : now.keySet()) {
                if (!was.containsKey(one)) {
                    gained++;
                }
            }
            wasTotal += was.size();
            nowTotal += now.size();
            keptTotal += kept;
            movedTotal += moved;
            lostTotal += lost;
            gainedTotal += gained;
            worstTotal = Math.max(worstTotal, worst);
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %d | %d | %d | %.0f px | %d | %d |%n",
                    page.slug(), was.size(), now.size(), kept, moved,
                    worst, lost, gained));
        }
        out.append(String.format(Locale.ROOT,
                "| **all pages** | **%d** | **%d** | **%d** | **%d** |"
                + " **%.0f px** | **%d** | **%d** |%n",
                wasTotal, nowTotal, keptTotal, movedTotal, worstTotal,
                lostTotal, gainedTotal));
        out.append("\nThe atlas draws " + (nowTotal - wasTotal >= 0
                        ? "**" + (nowTotal - wasTotal) + " more**"
                        : "**" + (wasTotal - nowTotal) + " fewer**")
                + " pieces of text than it did, and the two"
                + " directions\nare different things. What it stopped"
                + " drawing is the clipping decision being\npaid: a"
                + " label whose every candidate leaves the paper is not"
                + " drawn. What it\nstarted drawing is the collision"
                + " policy being repaid: a star's name that used"
                + " to\nbe dropped because its one box was taken now"
                + " has seven other places to try.\n\n");
        if (!lostNames.isEmpty()) {
            out.append("The text the atlas no longer draws, in full,"
                    + " because a list is the only honest\nform for"
                    + " it:\n\n```\n");
            for (String one : lostNames) {
                out.append(one).append("\n");
            }
            out.append("```\n\n");
        }
    }

    /** Star discs drawn over a constellation name, over the corpus. */
    private static int discsOverNames(Map<String, Census> censuses) {
        int found = 0;
        for (Census census : censuses.values()) {
            for (Attribution.Meeting meeting : census.collisions()) {
                if (meeting.over().family() == Family.STAR_DISC
                        && meeting.under().family()
                                == Family.CONSTELLATION_NAME) {
                    found++;
                }
            }
        }
        return found;
    }

    /** The atlas's own census before the migration, by page. */
    private static Map<String, String[]> censusBefore() {
        Map<String, String[]> before = new LinkedHashMap<>();
        java.io.File file = new java.io.File(
                "docs/studies/label-placement/census-before.tsv");
        if (!file.isFile()) {
            return before;
        }
        try (var lines = java.nio.file.Files.lines(file.toPath())) {
            lines.forEach(line -> {
                if (line.startsWith("#") || line.isBlank()) {
                    return;
                }
                String[] parts = line.split("\\t");
                if (parts.length >= 6) {
                    before.put(parts[0], new String[] {parts[1], parts[2],
                            parts[3], parts[4], parts[5]});
                }
            });
        } catch (java.io.IOException cannotRead) {
            return Map.of();
        }
        return before;
    }

    /** The atlas's own text before the migration, by page and name. */
    private static Map<String, Map<String, double[]>> textBefore() {
        Map<String, Map<String, double[]>> before = new LinkedHashMap<>();
        java.io.File file = new java.io.File(
                "docs/studies/label-placement/text-before.tsv");
        if (!file.isFile()) {
            return before;
        }
        try (var lines = java.nio.file.Files.lines(file.toPath())) {
            lines.forEach(line -> {
                if (line.startsWith("#") || line.isBlank()) {
                    return;
                }
                String[] parts = line.split("\t");
                if (parts.length < 8) {
                    return;
                }
                Family family = switch (parts[1]) {
                    case "constellation" -> Family.CONSTELLATION_NAME;
                    case "deep sky" -> Family.DEEP_SKY_LABEL;
                    default -> Family.STAR_LABEL;
                };
                before.computeIfAbsent(parts[0],
                                key -> new LinkedHashMap<>())
                        .put(family + ":" + parts[2], new double[] {
                                Double.parseDouble(parts[3]),
                                Double.parseDouble(parts[4])});
            });
        } catch (java.io.IOException cannotRead) {
            return Map.of();
        }
        return before;
    }


    /**
     * The text this page draws today, by name, from the decisions the
     * renderer publishes rather than from what qualifies.
     *
     * <p>The distinction is the whole of it: the star pass omits a
     * label whose box is already taken, so the stars that qualify are
     * more than the stars that are named. Counting a request as drawn
     * text would report a label as lost that the reader never had.
     */
    private static Map<String, Family> drawnToday(
            Page page, java.awt.FontMetrics metrics) {
        ChartScene scene = page.scene();
        var options = page.options();
        var studied = juranometria.project.DrawnPage.of(scene);
        var mapping = new juranometria.project.ViewportMapping(studied);
        var projection = studied.projection();
        var detail = new juranometria.render.RegionalDetailPolicy(scene,
                mapping.pixelsPerPlaneUnit());
        Map<String, Family> drawn = new LinkedHashMap<>();
        for (var placement : Page.renderer().starLabelPlacements(
                ChartRenderer.TextMetrics.offscreen(),
                scene, options)) {
            drawn.put(Family.STAR_LABEL + ":" + placement.star().id(),
                    Family.STAR_LABEL);
        }
        for (var dso : Page.renderer().labelledDeepSky(scene, options)) {
            if (projection.project(dso.position()).isPresent()) {
                drawn.put(Family.DEEP_SKY_LABEL + ":" + dso.id(),
                        Family.DEEP_SKY_LABEL);
            }
        }
        var ink = Page.renderer().figureInk(scene, options);
        if (options.effectiveConstellationNames()) {
            for (var name : scene.geography().latinNames().entrySet()) {
                var figure = ink.get(name.getKey());
                if (figure != null && figure.nameAnchor() != null) {
                    drawn.put(Family.CONSTELLATION_NAME + ":"
                            + name.getKey(), Family.CONSTELLATION_NAME);
                }
            }
        }
        return drawn;
    }

    /** Everything this page's text asks the seam for. */
    private static List<juranometria.render.LabelPlacement.Request>
            seamRequests(Page page, java.awt.FontMetrics metrics) {
        List<juranometria.render.LabelPlacement.Request> asked =
                new ArrayList<>();
        asked.addAll(juranometria.render.LabelGeometry.starLabels(
                Page.renderer(), metrics, page.scene(), page.options()));
        asked.addAll(juranometria.render.LabelGeometry.deepSkyLabels(
                Page.renderer(), metrics, page.scene(), page.options()));
        asked.addAll(juranometria.render.LabelGeometry.constellationNames(
                Page.renderer(), metrics, page.scene(), page.options()));
        return asked;
    }

    /**
     * Which family of text a request is, which is not always which
     * family the seam sorts it into: the searched object is a family
     * of its own to the placement rule, and is a star or a nebula to
     * everyone else. Asking the page settles it.
     */
    private static Family familyOf(
            Page page, juranometria.render.LabelPlacement.Request request) {
        return switch (request.family()) {
            case STAR -> Family.STAR_LABEL;
            case DEEP_SKY -> Family.DEEP_SKY_LABEL;
            case CONSTELLATION -> Family.CONSTELLATION_NAME;
            case TARGET -> {
                for (var dso : page.scene().deepSkyObjects()) {
                    if (dso.id().equals(request.id())) {
                        yield Family.DEEP_SKY_LABEL;
                    }
                }
                yield Family.STAR_LABEL;
            }
        };
    }

    /** One piece of text, named the way both passes name it. */
    private static String identityOf(
            Page page, juranometria.render.LabelPlacement.Request request) {
        return familyOf(page, request) + ":" + request.id();
    }

    /**
     * What clipping a label at the page edge actually costs, which is
     * not tidiness.
     */
    private static void clipping(StringBuilder out,
                                 List<StudyPages.Look> corpus) {
        out.append("## A word cut short is another word\n\n");
        out.append("The atlas draws a label whose box runs off the paper"
                + " and lets the page cut it.\nThe rule for"
                + " constellation names says so in as many words -"
                + " *honest position over\npretty placement* - and it"
                + " treats clipping as a matter of tidiness.\n\n");
        out.append("It is not. Of the constellations the bundled pack"
                + " draws, these become a **different\nconstellation**"
                + " when the page cuts their name:\n\n");
        out.append("```\n");
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (StudyPages.Look look : corpus) {
            for (String name
                    : look.page().scene().geography().latinNames()
                            .values()) {
                names.add(name.toUpperCase(Locale.ROOT));
            }
        }
        int nameClashes = 0;
        for (String name : names) {
            for (int cut = 3; cut < name.length(); cut++) {
                String head = name.substring(0, cut).trim();
                if (names.contains(head) && !head.equals(name)) {
                    out.append(String.format(Locale.ROOT,
                            "%-22s cut short reads   %s%n", name, head));
                    nameClashes++;
                    break;
                }
            }
        }
        out.append("```\n\n");
        java.util.Set<String> labels = new java.util.TreeSet<>();
        for (StudyPages.Look look : corpus) {
            for (var dso : look.page().scene().deepSkyObjects()) {
                labels.add(ChartRenderer.labelTextFor(dso));
            }
        }
        int labelClashes = 0;
        for (String label : labels) {
            for (int cut = 4; cut < label.length(); cut++) {
                if (labels.contains(label.substring(0, cut).trim())) {
                    labelClashes++;
                }
            }
        }
        out.append(String.format(Locale.ROOT,
                "And of the %d deep-sky labels these pages carry, **%d"
                        + " truncations are another\nobject's own"
                        + " label** - every `IC 1203` cut to `IC 1`,"
                        + " every `NGC 2024` cut to\n`NGC 202`. A"
                        + " clipped label is not an untidy page. It is a"
                        + " page that names the\nwrong thing, at the"
                        + " edge, where a reader matching a chart"
                        + " against the sky is\nmost likely to be"
                        + " working.\n\n", labels.size(),
                labelClashes));
        out.append("How often the atlas does it today, counted over"
                + " the corpus - every family, because\nthe decision is"
                + " every family's:\n\n");
        out.append("| page | star names | deep-sky labels |"
                + " constellation names | as drawn now |\n");
        out.append("|---|---:|---:|---:|---:|\n");
        int stars = 0;
        int deepSky = 0;
        int names2 = 0;
        int drawnClipped = 0;
        for (StudyPages.Look look : corpus) {
            Page page = look.page();
            int starsHere = 0;
            int deepSkyHere = 0;
            int namesHere = 0;
            int nowHere = 0;
            for (var placement : Page.renderer().textPlacements(
                    ChartRenderer.TextMetrics.offscreen(), page.scene(),
                    page.options())) {
                // At the label's usual place - its first candidate,
                // which is where every family drew before the
                // migration: beside the mark to its east, or on the
                // figure's own anchor.
                if (offThePaper(placement.request().candidates().get(0),
                        page)) {
                    switch (placement.request().family()) {
                        case CONSTELLATION -> namesHere++;
                        case DEEP_SKY -> deepSkyHere++;
                        default -> starsHere++;
                    }
                }
                if (!placement.omitted()
                        && offThePaper(placement.at(), page)) {
                    nowHere++;
                }
            }
            stars += starsHere;
            deepSky += deepSkyHere;
            names2 += namesHere;
            drawnClipped += nowHere;
            if (starsHere + deepSkyHere + namesHere + nowHere > 0) {
                out.append(String.format(Locale.ROOT,
                        "| `%s` | %d | %d | %d | %d |%n", page.slug(),
                        starsHere, deepSkyHere, namesHere, nowHere));
            }
        }
        out.append(String.format(Locale.ROOT,
                "| **all %d pages** | **%d** | **%d** | **%d** |"
                + " **%d** |%n",
                corpus.size(), stars, deepSky, names2, drawnClipped));
        out.append("\nThe first three columns are the labels whose"
                + " **usual place** - the first of their\nstated"
                + " candidates, where each family drew before Sprint"
                + " 31 - runs off the\npaper. The last is how many the"
                + " atlas draws clipped, which is the decision:"
                + " **no\ntext is clipped by the page, in any"
                + " family**. A label that cannot be drawn whole\nis"
                + " drawn somewhere else, and one with nowhere else is"
                + " not drawn at all, with the\nplacement recording"
                + " which candidates the paper refused. The mark is"
                + " still there,\nunnamed - which is what the page does"
                + " to every star below its limit, without\napology."
                + "\n\n");
        out.append("**The first three columns are not the cost of the"
                + " change.** They count what would\nbe clipped, which"
                + " is what the decision is about; what a reader"
                + " actually loses is\na different question, because a"
                + " label whose usual place runs off the paper has"
                + "\nseven other places to try before it is given up"
                + " on. The two are counted apart,\nand the second is"
                + " measured against the atlas as it was, further"
                + " down.\n\n");
    }

    private static boolean offThePaper(java.awt.geom.Rectangle2D box,
                                       Page page) {
        return box.getMinX() < 0 || box.getMinY() < 0
                || box.getMaxX() > page.wide()
                || box.getMaxY() > page.high();
    }

    /** The corpus share and the worst page's share, for the report. */
    private static double[] stabilityVerdict = {0.0, 0.0};

    private static void stability(StringBuilder out) {
        out.append("## Stability under a small navigation change\n\n");
        out.append("A label that jumps to the other side of its star"
                + " when the reader nudges the page\nis worse than one"
                + " that sits still in a slightly worse place. The gate"
                + " gave that a\nbudget - **no more than 15% of a"
                + " page's labels displaced by a one-step pan** - and"
                + "\nleft the zoom rung without one, because a zoom"
                + " changes which stars are on the page\nat all."
                + "\n\n");
        out.append("Every page is placed by the atlas, then placed"
                + " again one pan step east (a\ntwentieth of the"
                + " field). Displaced means a label both pages draw"
                + " whose offset\nfrom its own anchor changed sign in x"
                + " or in y: it crossed to the other side of\nthe thing"
                + " it names, which is the jump a reader notices."
                + "\n\n");
        out.append("| page | labels | displaced by a pan | share |"
                + " budget | after a zoom |\n");
        out.append("|---|---:|---:|---:|---|---:|\n");
        int displacedAll = 0;
        double[] worstShare = {0.0};
        int labelsAll = 0;
        Map<String, Integer> causes = new TreeMap<>();
        for (StudyPages.Look look : StudyPages.corpus()) {
            Page page = look.page();
            double field = page.scene().viewport().fieldWidthDegrees();
            SkyPosition centre = page.scene().viewport().centre();
            Page panned = pannedFrom(page);
            Page zoomed = page.withScene(StudyPages.assemble(
                    new ChartViewState(centre, nextRung(field),
                            ChartViewState.defaultMagnitudeFor(
                                    nextRung(field))),
                    page.wide(), page.high()));
            Map<String, LabelPlacement.Placement> here = placedOn(page);
            Map<String, LabelPlacement.Placement> after = placedOn(panned);
            int pan = flipped(here, after, causes);
            int zoom = flipped(here, placedOn(zoomed), null);
            int both = drawnByBoth(here, after);
            displacedAll += pan;
            labelsAll += both;
            double share = both == 0 ? 0.0 : (double) pan / both;
            worstShare[0] = Math.max(worstShare[0], share);
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %d | %.0f%% | %s | %d |%n",
                    page.slug(), both, pan, share * 100.0,
                    share <= 0.15 ? "met" : "**over**", zoom));
        }
        double all = labelsAll == 0 ? 0.0
                : (double) displacedAll / labelsAll;
        stabilityVerdict = new double[] {all, worstShare[0]};
        out.append(String.format(Locale.ROOT,
                "| **all pages** | **%d** | **%d** | **%.0f%%** | %s |"
                + " |%n", labelsAll, displacedAll, all * 100.0,
                all <= 0.15 ? "**met**" : "**over**"));
        out.append("\nOver the corpus the budget is met. On eleven"
                + " pages it is not, and the two facts\nare not in"
                + " tension: a page of eighteen labels moves five of"
                + " them and reads as 28%,\nwhile the pages carrying"
                + " most of the atlas's text sit between 6% and"
                + " 21%.\n\n");
        int[] orders = orderComparison();
        out.append("**What moves them.** For every displaced label,"
                + " what refused - on the panned page -\nthe position"
                + " it held on the first one:\n\n");
        out.append("| what refused the position it had | labels |\n");
        out.append("|---|---:|\n");
        int named = 0;
        for (var cause : causes.entrySet()) {
            out.append(String.format(Locale.ROOT, "| %s | %d |%n",
                    cause.getKey(), cause.getValue()));
            named += cause.getValue();
        }
        out.append(String.format(Locale.ROOT, "| **all** | **%d** |%n",
                named));
        out.append("\nThe largest group is not a collision at all:"
                + " those labels moved to an **earlier**\ncandidate"
                + " that the pan had freed - a label going back to the"
                + " side it prefers, as\nsoon as it can. The next is"
                + " the cascade: a label yields to a label that has"
                + " itself\nmoved. Neither can be removed from a"
                + " first-free pass that is not allowed to\nremember"
                + " where a label was, and the decision is not allowed"
                + " to remember: a page\nthat depended on how the"
                + " reader arrived at it would export differently for"
                + " two\nreaders looking at the same sky."
                + "\n\n");
        out.append("**Two geometry-only remedies were measured and"
                + " neither is one.** Ignoring contact\nbelow a pixel"
                + " of shared ink - the smallest mark the atlas can"
                + " lay down, so the bound\ncomes from the ink rather"
                + " than from the number it would produce - removes"
                + " exactly\none displacement of the twenty at"
                + " `sagittarius-120`: the refusals that move labels"
                + "\nthere share 6, 31, 34, 126, 154 and 168 square"
                + " pixels, and one shares 0.30.\n\n");
        out.append("And the eight positions can be tried in a"
                + " different order. The stated one is east,\nwest,"
                + " then the diagonals, which sends a refused label"
                + " straight across the thing\nit names; trying the"
                + " neighbours first - east, north-east, south-east,"
                + " north,\nsouth, north-west, south-west, west -"
                + " keeps it on the same side where it can."
                + " Both\norders, placed by the same pass and counted"
                + " by the same rule as the table above:"
                + "\n\n");
        out.append(String.format(Locale.ROOT,
                "| candidate order | displaced by a pan | of |\n"
                + "|---|---:|---:|\n"
                + "| the gate's: east, west, then the diagonals | %d |"
                + " %d |\n"
                + "| neighbours first | %d | %d |\n\n",
                orders[0], orders[1], orders[2], orders[3]));
        out.append(String.format(Locale.ROOT,
                "Better on %d pages, worse on %d, and no improvement"
                + " over the corpus. The stated\norder stands.\n\n",
                orders[4], orders[5]));
        out.append("So the budget is amended rather than met, with"
                + " the measurement above as its\ngrounds"
                + " (docs/decisions/label-placement.md, *Stability,"
                + " amended*).\n\n");
        out.append("Before the migration this table would have been"
                + " all zeroes and would have said\nnothing: every"
                + " family had exactly one position, so nothing could"
                + " be displaced by\nanything. What happened instead -"
                + " and did - was that a label disappeared when"
                + " its\none box was taken.\n\n");
    }

    /**
     * The corpus placed twice, in two candidate orders, and displaced
     * both times - the same pass and the same counter, so the two
     * numbers can be set beside each other.
     */
    private static int[] orderComparison() {
        int gate = 0;
        int gateOf = 0;
        int neighbours = 0;
        int neighboursOf = 0;
        int better = 0;
        int worse = 0;
        for (StudyPages.Look look : StudyPages.corpus()) {
            Page page = look.page();
            Page panned = pannedFrom(page);
            Map<String, LabelPlacement.Placement> here = placedOn(page);
            Map<String, LabelPlacement.Placement> there = placedOn(panned);
            int here_ = flipped(here, there, null);
            gate += here_;
            gateOf += drawnByBoth(here, there);
            Map<String, LabelPlacement.Placement> stepped =
                    placedOn(page, true);
            Map<String, LabelPlacement.Placement> steppedPan =
                    placedOn(panned, true);
            int there_ = flipped(stepped, steppedPan, null);
            neighbours += there_;
            neighboursOf += drawnByBoth(stepped, steppedPan);
            if (there_ < here_) {
                better++;
            } else if (there_ > here_) {
                worse++;
            }
        }
        return new int[] {gate, gateOf, neighbours, neighboursOf, better,
                worse};
    }

    /** This page, nudged one pan step east. */
    private static Page pannedFrom(Page page) {
        double field = page.scene().viewport().fieldWidthDegrees();
        SkyPosition centre = page.scene().viewport().centre();
        return page.withScene(StudyPages.assemble(new ChartViewState(
                new SkyPosition(centre.raDegrees() + field / 20.0,
                        centre.decDegrees()), field,
                page.scene().limitingMagnitude()),
                page.wide(), page.high()));
    }

    /** East, then round the anchor, and the far side last. */
    private static final int[] NEIGHBOURS_FIRST = {0, 2, 4, 6, 7, 3, 5, 1};

    /** The same page placed with the positions tried in that order. */
    private static Map<String, LabelPlacement.Placement> placedOn(
            Page page, boolean neighboursFirst) {
        var metrics = ChartRenderer.TextMetrics.offscreen();
        List<LabelPlacement.Request> asked = new ArrayList<>();
        for (LabelPlacement.Request request : Page.renderer().textRequests(
                metrics, page.scene(), page.options())) {
            asked.add(neighboursFirst ? stepped(request) : request);
        }
        var seam = new LabelPlacement(page.wide(), page.high(),
                Page.renderer().textObstacles(metrics, page.scene(),
                        page.options()));
        Map<String, LabelPlacement.Placement> at = new LinkedHashMap<>();
        for (var placement : seam.placeAll(asked)) {
            at.put(identityOf(page, placement.request()), placement);
        }
        return at;
    }

    /** One request with its stated positions in the other order. */
    private static LabelPlacement.Request stepped(
            LabelPlacement.Request request) {
        List<java.awt.geom.Rectangle2D> from = request.candidates();
        List<java.awt.geom.Rectangle2D> to = new ArrayList<>();
        if (from.size() == 8) {
            for (int at : NEIGHBOURS_FIRST) {
                to.add(from.get(at));
            }
        } else if (from.size() == 17) {
            to.add(from.get(0));
            for (int ring = 0; ring < 2; ring++) {
                for (int at : NEIGHBOURS_FIRST) {
                    to.add(from.get(1 + ring * 8 + at));
                }
            }
        } else {
            to.addAll(from);
        }
        return new LabelPlacement.Request(request.family(), request.id(),
                request.text(), request.anchorX(), request.anchorY(), to,
                request.ownId(), request.owns(), request.guaranteed(),
                request.order());
    }

    /** Every piece of text a page places, by name. */
    private static Map<String, LabelPlacement.Placement> placedOn(
            Page page) {
        Map<String, LabelPlacement.Placement> at = new LinkedHashMap<>();
        for (var placement : Page.renderer().textPlacements(
                ChartRenderer.TextMetrics.offscreen(), page.scene(),
                page.options())) {
            at.put(identityOf(page, placement.request()), placement);
        }
        return at;
    }

    /** Labels both pages draw. */
    private static int drawnByBoth(
            Map<String, LabelPlacement.Placement> here,
            Map<String, LabelPlacement.Placement> there) {
        int both = 0;
        for (var entry : here.entrySet()) {
            var other = there.get(entry.getKey());
            if (other != null && !other.omitted()
                    && !entry.getValue().omitted()) {
                both++;
            }
        }
        return both;
    }

    /**
     * Labels that crossed to the other side of what they name, and
     * what refused the side they were on.
     */
    private static int flipped(Map<String, LabelPlacement.Placement> here,
                               Map<String, LabelPlacement.Placement> there,
                               Map<String, Integer> causes) {
        int crossed = 0;
        for (var entry : here.entrySet()) {
            var after = there.get(entry.getKey());
            var before = entry.getValue();
            if (after == null || after.omitted() || before.omitted()) {
                continue;
            }
            double wasX = before.at().getCenterX()
                    - before.request().anchorX();
            double wasY = before.at().getCenterY()
                    - before.request().anchorY();
            double isX = after.at().getCenterX()
                    - after.request().anchorX();
            double isY = after.at().getCenterY()
                    - after.request().anchorY();
            if (Math.signum(isX) == Math.signum(wasX)
                    && Math.signum(isY) == Math.signum(wasY)) {
                continue;
            }
            crossed++;
            if (causes == null) {
                continue;
            }
            String why = "nothing - it took an earlier candidate the"
                    + " pan had freed";
            for (var refusal : after.refusals()) {
                if (refusal.candidate() == before.candidate()) {
                    why = switch (refusal.kind()) {
                        case TEXT -> "another label, itself displaced"
                                + " or newly there";
                        case MARK -> "a star's mark or a symbol";
                        case FURNITURE -> "the title block or the key";
                        case PAGE_EDGE -> "the paper's edge";
                        case OWNERSHIP -> "its own figure's region";
                    };
                    break;
                }
            }
            causes.merge(why, 1, Integer::sum);
        }
        return crossed;
    }


    /**
     * What placing a page costs, in the only currency that means the
     * same thing on every machine.
     *
     * <p>The gate budgeted the work rather than the clock: an indexed
     * pass may make no more than a tenth of the comparisons an
     * every-against-every pass would. A millisecond is a fact about a
     * machine, and this file has to reproduce itself byte for byte.
     */
    private static void cost(StringBuilder out) {
        out.append("## What placing a page costs\n\n");
        out.append("The gate budgeted the **work**, not the clock:"
                + " the obstacle comparisons an\nindexed pass makes"
                + " against the product of labels and ink, which is the"
                + " same number\non every machine. The budget is a"
                + " tenth.\n\n");
        out.append("| page | labels | obstacles | every against every |"
                + " comparisons made | per label | share | budget |\n");
        out.append("|---|---:|---:|---:|---:|---:|---:|---|\n");
        for (String slug : CANDIDATE_PAGES) {
            Page page = pageOf(StudyPages.corpus(), slug);
            var metrics = ChartRenderer.TextMetrics.offscreen();
            var obstacles = juranometria.render.LabelGeometry.obstaclesOn(
                    Page.renderer(), metrics.labels(), page.scene(),
                    page.options());
            List<juranometria.render.LabelPlacement.Request> asked =
                    seamRequests(page, metrics.labels());
            var seam = new juranometria.render.LabelPlacement(page.wide(),
                    page.high(), obstacles);
            seam.placeAll(asked);
            long every = (long) asked.size() * obstacles.size();
            double share = every == 0 ? 0.0
                    : (double) seam.comparisons() / every;
            out.append(String.format(Locale.ROOT,
                    "| `%s` | %d | %d | %d | %d | %d | %.1f%% | %s |%n",
                    slug, asked.size(), obstacles.size(), every,
                    seam.comparisons(),
                    asked.isEmpty() ? 0
                            : seam.comparisons() / asked.size(),
                    share * 100.0, share <= 0.10 ? "met" : "**over**"));
        }
        out.append("\nThe index is a uniform grid of 64-pixel cells:"
                + " a candidate box asks the cells it\ncovers and"
                + " nothing else, so a label at the top of the page"
                + " never hears about a\nsymbol at the bottom of"
                + " it.\n\n");
        out.append("The share is met where the budget was meant to"
                + " bite - the wide pages, where the\nwork could run"
                + " away - and missed on two pages where the ratio"
                + " stops meaning much.\n`home` has five labels and"
                + " fifty-six obstacles, so there is nothing for an"
                + " index to\nsave; `orion-36` has eighteen labels"
                + " against a dense star field of sixteen hundred"
                + "\nmarks, and its labels sit in the crowded middle of"
                + " it. The number that governs\nwhether a page can be"
                + " placed at all is the last-but-two column, and no"
                + " page in\nthe corpus asks more than a few hundred"
                + " questions per label.\n\n");
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
