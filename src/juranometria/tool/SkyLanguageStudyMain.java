package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;

/**
 * What a Norwegian sky costs the page (Sprint 33, issue #347).
 *
 * <p><strong>PROVISIONAL.</strong> Every measurement here is taken
 * against the owner's 88-row lead, which is a lead and not
 * provenance. When a citable source replaces it, this study is
 * regenerated; until then no number below is the gate's answer, and
 * none of it describes the final catalogue.
 *
 * <p>The question is not whether the two languages agree - they must
 * not, or nothing was translated. It is what a reader loses. A
 * Norwegian name is often longer than its Latin counterpart
 * ({@code Sørlige vannslange} against {@code Hydrus}, three times the
 * width), and a longer word in the same place either moves, is
 * refused, or lands somewhere that no longer reads as belonging to
 * its own constellation.
 *
 * <p>Three outcomes are reported separately, because they cost a
 * reader different things:
 *
 * <ul>
 *   <li><strong>moved</strong> - written in both languages, at
 *       different candidate positions;</li>
 *   <li><strong>omitted</strong> - written in one and refused in the
 *       other, with the refusal that decided it;</li>
 *   <li><strong>renamed in place</strong> - different words at the
 *       same candidate, which is the outcome that costs nothing.</li>
 * </ul>
 *
 * <p>Distance to the page edge is measured from the label's whole
 * box, never from its anchor: an anchor comfortably inside the disc
 * says nothing about a word whose tail hangs over the rim.
 */
public final class SkyLanguageStudyMain {

    private SkyLanguageStudyMain() {
    }

    /**
     * The corpus this study promises, and must keep.
     *
     * <p>A study that quietly shrank its own fixture list would keep
     * reporting cleanly while measuring less - and the limb mutation
     * that survived four flat fixtures is what this number exists to
     * prevent. Named here so removing a page is a failure rather
     * than a smaller report.
     */
    private static final int PROMISED_PAGES = 14;

    /** The manifest that says what the lead is. */
    private static final Path MANIFEST =
            Path.of("docs/studies/sky-language/lead.manifest");

    /**
     * What the data says about itself.
     *
     * <p>Status belongs to the input's provenance, never to the
     * generator. A banner compiled into Java is a promise nobody can
     * check and everybody forgets: verify the catalogue and the
     * report goes on calling itself provisional; or worse, delete
     * the banner and it stops saying so while nothing has been
     * verified at all. Reading it from beside the data means the
     * report's status is a property of the data.
     *
     * <p>A digest corroborates but cannot replace this. It proves
     * which bytes were measured, not where their claims came from.
     */
    private record Provenance(String status, String source,
                              String licence, String retrieved,
                              String transformations) {

        boolean provisional() {
            return !"verified".equals(status);
        }

        /**
         * Reads a manifest, refusing a claim it cannot support.
         *
         * <p>{@code verified} without a source, a licence, a
         * retrieval date and transformation notes is not a status,
         * it is an assertion - so it is refused rather than
         * recorded.
         */
        static Provenance of(Map<String, String> manifest) {
            String status = manifest.getOrDefault("status", "").strip();
            if (!status.equals("provisional") && !status.equals("verified")) {
                throw new IllegalStateException(
                        "the lead's manifest must state status="
                                + "provisional or status=verified;"
                                + " found \"" + status + "\"");
            }
            Provenance read = new Provenance(status,
                    manifest.getOrDefault("source", "").strip(),
                    manifest.getOrDefault("licence", "").strip(),
                    manifest.getOrDefault("retrieved", "").strip(),
                    manifest.getOrDefault("transformations", "").strip());
            if (!read.provisional()) {
                List<String> missing = new ArrayList<>();
                if (read.source().isBlank()) {
                    missing.add("source");
                }
                if (read.licence().isBlank()) {
                    missing.add("licence");
                }
                if (read.retrieved().isBlank()) {
                    missing.add("retrieved");
                }
                if (read.transformations().isBlank()) {
                    missing.add("transformations");
                }
                if (!missing.isEmpty()) {
                    throw new IllegalStateException(
                            "status=verified claims the names were"
                                    + " checked against a citable"
                                    + " source, so the manifest must"
                                    + " say which; missing: "
                                    + String.join(", ", missing));
                }
            }
            return read;
        }
    }

    private static final int WIDE_PX = 1100;

    private static final int HIGH_PX = 800;

    /** Where the report is written. */
    private static final Path REPORT =
            Path.of("docs/studies/sky-language/placement.md");

    /** The lead, read from its committed copy. */
    private static final Path LEAD =
            Path.of("docs/studies/sky-language/norwegian-lead.tsv");

    /**
     * Matched pages: the same centre and field in both languages.
     *
     * <p>The globe is required, not illustrative. A whole family of
     * placement behaviour - the circular page region, the limb, names
     * refused for leaving the disc - exists only where the sky stops
     * short of the paper, and a study of flat pages would have
     * reported that family as costing nothing.
     */
    private static final Map<String, ChartViewState> PAGES =
            new LinkedHashMap<>();

    static {
        PAGES.put("crowded Sagittarius, 18 degrees", new ChartViewState(
                new SkyPosition(271.0, -24.0), 18.0, 8.0));
        PAGES.put("sparse Orion, 18 degrees", new ChartViewState(
                new SkyPosition(83.8, 0.0), 18.0, 8.0));
        PAGES.put("Sagittarius, 42 degrees", new ChartViewState(
                new SkyPosition(271.0, -24.0), 42.0, 8.0));
        PAGES.put("Orion, 42 degrees", new ChartViewState(
                new SkyPosition(83.8, 0.0), 42.0, 8.0));
        PAGES.put("Sagittarius, 120 degrees", new ChartViewState(
                new SkyPosition(271.0, -24.0), 120.0, 8.0));
        PAGES.put("Orion, 120 degrees", new ChartViewState(
                new SkyPosition(83.8, 0.0), 120.0, 8.0));
        PAGES.put("the globe at Sagittarius, 180 degrees",
                new ChartViewState(new SkyPosition(271.0, -24.0),
                        180.0, 8.0));
        PAGES.put("the globe at Orion, 180 degrees", new ChartViewState(
                new SkyPosition(83.8, 0.0), 180.0, 8.0));
        // The RA seam. Named by the gate's corpus, and not
        // satisfied by another test that happens to cross it: a
        // study promises the pages it lists.
        PAGES.put("the RA seam, 42 degrees", new ChartViewState(
                new SkyPosition(0.0, 20.0), 42.0, 8.0));
        PAGES.put("the RA seam, 180 degrees", new ChartViewState(
                new SkyPosition(0.0, 20.0), 180.0, 8.0));
        PAGES.put("the north pole, 42 degrees", new ChartViewState(
                new SkyPosition(0.0, 89.0), 42.0, 8.0));
        PAGES.put("the south pole, 42 degrees", new ChartViewState(
                new SkyPosition(0.0, -89.0), 42.0, 8.0));
        PAGES.put("the north pole, 180 degrees", new ChartViewState(
                new SkyPosition(0.0, 89.0), 180.0, 8.0));
        PAGES.put("the south pole, 180 degrees", new ChartViewState(
                new SkyPosition(0.0, -89.0), 180.0, 8.0));
    }

    /** One measured attachment, for the distribution. */
    private record Margin(String id, String page, double toOwn,
                          double toRival, String rival) {
    }

    /** One constellation name's fate on one page. */
    private record Fate(String id, String latin, String norwegian,
                        String outcome, String detail) {
    }

    public static void main(String[] args) throws IOException {
        if (PAGES.size() != PROMISED_PAGES) {
            throw new IllegalStateException(
                    "this study promises " + PROMISED_PAGES
                            + " matched pages and would have measured "
                            + PAGES.size() + "; a fixture may not"
                            + " disappear quietly");
        }
        Map<String, String> lead = readLead();
        Provenance provenance = Provenance.of(readManifest());
        boolean provisional = provenance.provisional();
        StringBuilder report = new StringBuilder();
        report.append(provisional
                ? "# Latin and Norwegian on the same page — PROVISIONAL\n"
                : "# Latin and Norwegian on the same page\n");
        report.append("""

                Issue #347. Generated by
                `juranometria.tool.SkyLanguageStudyMain`.

                **PROVISIONAL.** Measured against the owner's 88-row
                lead, which is a lead and not provenance. No number
                here is the gate's answer, and none of it describes
                the final catalogue. When a citable source replaces
                the lead this study is regenerated rather than
                reinterpreted.

                Each page is drawn at one centre and field, and placed
                twice: once with Latin names, once with Norwegian.
                Both read the same assembled scene and each makes its
                own placement decision through the production
                renderer, so neither answer is derived from the other.

                Constellation names are drawn uppercased
                (`toUpperCase(Locale.ROOT)`), so the glyphs a
                Norwegian sky needs are `Æ`, `Ø` and `Å`, not only
                their lower-case forms.

                """);

        int moved = 0;
        int omitted = 0;
        int renamed = 0;
        int unchanged = 0;
        List<String> negative = new ArrayList<>();
        List<String> unmeasurable = new ArrayList<>();
        List<String> boxCentreSaid = new ArrayList<>();
        List<Margin> margins = new ArrayList<>();
        java.util.Set<String> touched = new java.util.TreeSet<>();
        double worstRim = Double.MAX_VALUE;
        String worstRimWhere = "";

        for (Map.Entry<String, ChartViewState> page : PAGES.entrySet()) {
            ChartScene latinScene = Atlas.assembler()
                    .assemble(page.getValue(), WIDE_PX, HIGH_PX);
            ChartScene norskScene = named(latinScene, localised(
                    latinScene, lead));
            Map<String, LabelPlacement.Placement> latin =
                    constellationPlacements(latinScene);
            Map<String, LabelPlacement.Placement> norsk =
                    constellationPlacements(norskScene);

            List<Fate> fates = new ArrayList<>();
            for (String id : new TreeMap<>(
                    latinScene.geography().latinNames()).keySet()) {
                LabelPlacement.Placement was = latin.get(id);
                LabelPlacement.Placement now = norsk.get(id);
                String latinName = latinScene.geography()
                        .latinNames().get(id);
                String norskName = norskScene.geography()
                        .latinNames().get(id);
                if (was == null && now == null) {
                    continue;
                }
                if (written(was) && !written(now)) {
                    fates.add(new Fate(id, latinName, norskName,
                            "omitted in Norwegian", refusal(now)));
                    omitted++;
                    touched.add(id);
                } else if (!written(was) && written(now)) {
                    fates.add(new Fate(id, latinName, norskName,
                            "omitted in Latin", refusal(was)));
                    omitted++;
                    touched.add(id);
                } else if (written(was) && written(now)) {
                    double rim = Math.min(edgeGap(was), edgeGap(now));
                    if (rim < worstRim) {
                        worstRim = rim;
                        worstRimWhere = id + " on " + page.getKey();
                    }
                    Attachment held = attachment(now, norskScene);
                    if (!held.ownVisible() || !held.rivalVisible()) {
                        // Premise, not a result: a constellation that
                        // draws nothing here is neither near nor far
                        // from anything, and counting it either way
                        // would be inventing a measurement.
                        unmeasurable.add(id + " on " + page.getKey()
                                + (held.ownVisible()
                                        ? " (no rival geometry)"
                                        : " (no visible geometry of"
                                                + " its own)"));
                    } else {
                        margins.add(new Margin(id, page.getKey(),
                                held.toOwn(), held.toRival(),
                                held.rival()));
                        if (held.margin() < 0.0) {
                            negative.add(String.format(
                                    "`%s` on %s: %.1f px from its own"
                                            + " figure, %.1f px from"
                                            + " `%s` (margin %.1f px)",
                                    id, page.getKey(), held.toOwn(),
                                    held.toRival(), held.rival(),
                                    held.margin()));
                        }
                    }
                    if (!nearestByBoxCentre(now, norskScene)) {
                        boxCentreSaid.add(id + " on " + page.getKey());
                    }
                    if (was.candidate() != now.candidate()) {
                        fates.add(new Fate(id, latinName, norskName,
                                "moved",
                                "candidate " + was.candidate() + " to "
                                        + now.candidate() + ", "
                                        + String.format("%.0f px", shift(
                                                was, now))));
                        moved++;
                        touched.add(id);
                    } else if (!latinName.equals(norskName)) {
                        fates.add(new Fate(id, latinName, norskName,
                                "renamed in place",
                                "candidate " + was.candidate()));
                        renamed++;
                        touched.add(id);
                    } else {
                        unchanged++;
                    }
                }
            }
            appendPage(report, page.getKey(), fates, latin.size(),
                    norsk.size());
        }

        margins.sort((a, b) -> Double.compare(a.toRival() - a.toOwn(),
                b.toRival() - b.toOwn()));
        report.append("\n## What the lead costs, in total\n\n")
                .append("These are **placement observations across ")
                .append(PAGES.size()).append(" pages**, not distinct ")
                .append("constellations: one name measured on eight ")
                .append("pages contributes eight observations.\n\n")
                .append("| outcome | observations |\n|---|---|\n")
                .append("| renamed in place (no cost) | ")
                .append(renamed).append(" |\n| moved | ")
                .append(moved).append(" |\n| omitted in one language | ")
                .append(omitted)
                .append(" |\n| identical in both languages | ")
                .append(unchanged).append(" |\n\n")
                .append("**Unique constellation identities affected** ")
                .append("(moved, omitted or renamed at least once): ")
                .append(touched.size()).append(" of 88.\n\n")
                .append("Closest any written name comes to the page edge, ")
                .append("measured from its whole box: ")
                .append(String.format("%.1f px", worstRim))
                .append(" (").append(worstRimWhere).append(").\n\n")
                .append("## Attachment\n\n")
                .append("Vector distance from each Norwegian label's box ")
                .append("to the projected, page-clipped figure segments ")
                .append("of its own constellation, and to the nearest ")
                .append("rival's. Positive margin means the label is ")
                .append("nearer its own geography.\n\n")
                .append("Measured: ").append(margins.size())
                .append(" labels. Not measurable: ")
                .append(unmeasurable.size())
                .append(" (premise: the constellation, or every rival, ")
                .append("draws no visible segment on that page).\n\n");
        if (!margins.isEmpty()) {
            report.append("| id | page | to own | to rival | rival | margin |")
                    .append("\n|---|---|---|---|---|---|\n");
            for (Margin margin : margins.subList(0,
                    Math.min(12, margins.size()))) {
                report.append(String.format(
                        "| `%s` | %s | %.1f px | %.1f px | `%s` | %.1f px |%n",
                        margin.id(), margin.page(), margin.toOwn(),
                        margin.toRival(), margin.rival(),
                        margin.toRival() - margin.toOwn()));
            }
            report.append("\n*The twelve tightest margins, ascending.*\n\n");
        }
        report.append("**Negative margins: ").append(negative.size())
                .append("** — for owner inspection, not automatically a ")
                .append("defect. Placement uses the constellation's ")
                .append("visible geography, and where figures interlock ")
                .append("or one contributes only a stub of line, the ")
                .append("nearest line is an imperfect oracle for what a ")
                .append("reader reads as belonging.\n\n");
        for (String one : negative) {
            report.append("- ").append(one).append("\n");
        }
        report.append("\n### Why the metric changed\n\n")
                .append("The first attempt compared bounding-box ")
                .append("**centres**. It reported ")
                .append(boxCentreSaid.size())
                .append(" detached labels on this corpus, and their ")
                .append("distribution gave it away: they clustered on ")
                .append("180-degree globes, which is exactly where a ")
                .append("bounding box least resembles the figure inside ")
                .append("it. The corrected metric measures to the real ")
                .append("projected path and finds ").append(negative.size())
                .append(" negative margins, for a geometric reason ")
                .append("rather than a change of threshold.\n");

        if (!unmeasurable.isEmpty()) {
            // The premise behind every attachment number. If a
            // constellation stops contributing visible geometry the
            // margins silently stop meaning anything, so this is
            // loud rather than a footnote.
            throw new IllegalStateException(
                    "attachment is measurable for every observation or"
                            + " for none of them; " + unmeasurable.size()
                            + " became unmeasurable: " + unmeasurable);
        }
        report.append("\n## Why this report can be trusted to go stale\n\n")
                .append("The lead this study measures is pinned by digest, ")
                .append("so changing it without regenerating this report ")
                .append("is a contract breach rather than a silent ")
                .append("disagreement.\n\n")
                .append("- lead digest: `").append(digestOf(LEAD))
                .append("`\n- matched pages: ").append(PAGES.size())
                .append(" (all ").append(PROMISED_PAGES)
                .append(" promised)\n- attachment observations: ")
                .append(margins.size())
                .append(", unmeasurable 0\n")
                .append("- manifest digest: `").append(digestOf(MANIFEST))
                .append("`\n- status, as the data states it: **")
                .append(provenance.status().toUpperCase(
                        java.util.Locale.ROOT))
                .append("** — source `").append(provenance.source())
                .append("`")
                .append(provisional ? "" : ", licence `"
                        + provenance.licence() + "`, retrieved "
                        + provenance.retrieved())
                .append("\n");
        // Printed, not written. Every report generator in this
        // contract is judged on what it SAYS - the contract captures
        // stdout and holds it to the committed bytes - and the make
        // target is what puts it on disk. Writing the file here and
        // printing a summary instead made the contract compare a
        // one-line summary against a five-hundred-line report.
        System.out.print(report);
        System.err.println("sky-language placement study (PROVISIONAL): "
                + renamed + " renamed in place, " + moved + " moved, "
                + omitted + " omitted, " + unchanged + " identical"
                + " across " + PAGES.size() + " pages; "
                + touched.size() + " of 88 identities affected;"
                + " attachment measured for " + margins.size()
                + ", unmeasurable " + unmeasurable.size()
                + ", negative margins " + negative.size()
                + " (box-centre metric said " + boxCentreSaid.size()
                + ")");
    }

    private static void appendPage(StringBuilder report, String where,
                                   List<Fate> fates, int latinCount,
                                   int norskCount) {
        report.append("## ").append(where).append("\n\n")
                .append(latinCount).append(" constellation names asked in ")
                .append("Latin, ").append(norskCount)
                .append(" in Norwegian.\n\n");
        if (fates.isEmpty()) {
            report.append("Every name kept its place and its ")
                    .append("candidate.\n\n");
            return;
        }
        report.append("| id | Latin | Norwegian | outcome | detail |\n")
                .append("|---|---|---|---|---|\n");
        for (Fate fate : fates) {
            report.append("| `").append(fate.id()).append("` | ")
                    .append(fate.latin()).append(" | ")
                    .append(fate.norwegian()).append(" | ")
                    .append(fate.outcome()).append(" | ")
                    .append(fate.detail()).append(" |\n");
        }
        report.append("\n");
    }

    private static boolean written(LabelPlacement.Placement placed) {
        return placed != null && !placed.omitted();
    }

    private static String refusal(LabelPlacement.Placement placed) {
        if (placed == null) {
            return "not asked for on this page";
        }
        if (placed.refusals() == null || placed.refusals().isEmpty()) {
            return "refused with no reason recorded";
        }
        return placed.refusals().size() + " candidates refused, first: "
                + placed.refusals().get(0);
    }

    /** How far the whole box sits from the nearest page edge. */
    private static double edgeGap(LabelPlacement.Placement placed) {
        Rectangle2D box = placed.at();
        return Math.min(Math.min(box.getMinX(), box.getMinY()),
                Math.min(WIDE_PX - box.getMaxX(), HIGH_PX - box.getMaxY()));
    }

    private static double shift(LabelPlacement.Placement was,
                                LabelPlacement.Placement now) {
        return Math.hypot(was.at().getCenterX() - now.at().getCenterX(),
                was.at().getCenterY() - now.at().getCenterY());
    }

    /**
     * The metric this replaced: bounding-box centres.
     *
     * <p>Kept, and reported beside the corrected one, so the claim
     * that it was measuring geometry rather than belonging is shown
     * instead of asserted. It treats a sprawling figure as a point,
     * which is worst exactly where its answers clustered.
     */
    private static boolean nearestByBoxCentre(LabelPlacement.Placement placed,
                                              ChartScene scene) {
        Shape own = placed.request().owns();
        if (own == null) {
            return true;
        }
        Rectangle2D box = placed.at();
        double mine = centreGap(own.getBounds2D(), box);
        for (Map.Entry<String, ChartRenderer.FigureInk> rival
                : new ChartRenderer(StarSizePolicy.DEFAULT)
                        .figureInk(scene, ChartOptions.DEFAULTS).entrySet()) {
            if (rival.getKey().equals(placed.request().id())
                    || rival.getValue().ink() == null) {
                continue;
            }
            if (centreGap(rival.getValue().ink().getBounds2D(), box) < mine) {
                return false;
            }
        }
        return true;
    }

    private static double centreGap(Rectangle2D a, Rectangle2D b) {
        return Math.hypot(a.getCenterX() - b.getCenterX(),
                a.getCenterY() - b.getCenterY());
    }

    /** How far a label sits from its own figure and its nearest rival. */
    private record Attachment(double toOwn, double toRival, String rival,
                              boolean ownVisible, boolean rivalVisible) {

        /** Positive when the label is nearer its own geography. */
        double margin() {
            return toRival - toOwn;
        }
    }

    /**
     * How firmly a label attaches to its own constellation.
     *
     * <p>Measured from the label's box to the <em>projected,
     * page-clipped figure segments</em> that constellation actually
     * draws on this page, and then to the nearest rival's visible
     * segments. Both come from {@code figureInk} for the same scene
     * and options that placed the label, so the projection and the
     * clip are the page's own and not a reconstruction.
     *
     * <p>Distances are vector distances to the real path. A raster
     * mask would fold stroke width and antialiasing into a question
     * about attachment, which is about where the line is, not how
     * thickly it was painted.
     *
     * <p>Reported as a continuous margin rather than a verdict. A
     * negative margin is a page for the owner to look at, not
     * automatically a defect: the placement policy uses the
     * constellation's visible geography, and where figures interlock
     * or a constellation contributes only a stub of line, the
     * nearest line is an imperfect oracle for what a reader reads as
     * belonging.
     *
     * <p>The metric this replaced compared bounding-box <em>centres</em>,
     * which treats a sprawling figure as a point; it reported 25
     * detached labels, every one of them on a 180-degree globe, which
     * is exactly where a bounding box least resembles its contents.
     */
    private static Attachment attachment(LabelPlacement.Placement placed,
                                         ChartScene scene) {
        Map<String, ChartRenderer.FigureInk> ink =
                new ChartRenderer(StarSizePolicy.DEFAULT)
                        .figureInk(scene, ChartOptions.DEFAULTS);
        Rectangle2D box = placed.at();
        ChartRenderer.FigureInk own = ink.get(placed.request().id());
        double toOwn = own == null ? Double.NaN
                : distanceToPath(box, own.ink());
        double toRival = Double.MAX_VALUE;
        String nearest = "none";
        boolean rivalVisible = false;
        for (Map.Entry<String, ChartRenderer.FigureInk> other
                : ink.entrySet()) {
            if (other.getKey().equals(placed.request().id())) {
                continue;
            }
            double away = distanceToPath(box, other.getValue().ink());
            if (!Double.isNaN(away)) {
                rivalVisible = true;
                // Ties are common and must not be broken by map
                // order. On a crowded globe several figures touch the
                // label box at exactly 0.0 px, and keeping "whichever
                // came first" made the recorded rival change between
                // runs on identical input - which the evidence
                // contract would report as a report that does not
                // reproduce. The identity settles it.
                if (away < toRival
                        || (away == toRival
                                && other.getKey().compareTo(nearest) < 0)) {
                    toRival = away;
                    nearest = other.getKey();
                }
            }
        }
        return new Attachment(toOwn, toRival, nearest,
                !Double.isNaN(toOwn), rivalVisible);
    }

    /**
     * Vector distance from a box to a path, zero where they meet.
     *
     * <p>{@code NaN} when the path contributes no visible segment on
     * this page, which is a premise failure rather than a distance:
     * a constellation that draws nothing here cannot be near or far
     * from anything.
     */
    private static double distanceToPath(Rectangle2D box, Shape path) {
        if (path == null) {
            return Double.NaN;
        }
        double[] point = new double[6];
        double from = -1.0;
        double lastX = 0.0;
        double lastY = 0.0;
        boolean any = false;
        for (java.awt.geom.PathIterator walk = path.getPathIterator(null, 1.0);
                !walk.isDone(); walk.next()) {
            int kind = walk.currentSegment(point);
            if (kind == java.awt.geom.PathIterator.SEG_MOVETO) {
                lastX = point[0];
                lastY = point[1];
                continue;
            }
            if (kind != java.awt.geom.PathIterator.SEG_LINETO) {
                continue;
            }
            any = true;
            double gap = boxToSegment(box, lastX, lastY, point[0], point[1]);
            if (from < 0.0 || gap < from) {
                from = gap;
            }
            lastX = point[0];
            lastY = point[1];
            if (from == 0.0) {
                return 0.0;
            }
        }
        return any ? from : Double.NaN;
    }

    /** Distance from a rectangle to one segment; zero if they touch. */
    private static double boxToSegment(Rectangle2D box, double x1,
                                       double y1, double x2, double y2) {
        if (box.intersectsLine(x1, y1, x2, y2)) {
            return 0.0;
        }
        double[][] edges = {
                {box.getMinX(), box.getMinY(), box.getMaxX(), box.getMinY()},
                {box.getMaxX(), box.getMinY(), box.getMaxX(), box.getMaxY()},
                {box.getMaxX(), box.getMaxY(), box.getMinX(), box.getMaxY()},
                {box.getMinX(), box.getMaxY(), box.getMinX(), box.getMinY()}};
        double nearest = Double.MAX_VALUE;
        for (double[] edge : edges) {
            nearest = Math.min(nearest, Math.min(
                    Math.min(java.awt.geom.Line2D.ptSegDist(x1, y1, x2, y2,
                                    edge[0], edge[1]),
                            java.awt.geom.Line2D.ptSegDist(x1, y1, x2, y2,
                                    edge[2], edge[3])),
                    Math.min(java.awt.geom.Line2D.ptSegDist(edge[0], edge[1],
                                    edge[2], edge[3], x1, y1),
                            java.awt.geom.Line2D.ptSegDist(edge[0], edge[1],
                                    edge[2], edge[3], x2, y2))));
        }
        return nearest;
    }

    private static Map<String, LabelPlacement.Placement>
            constellationPlacements(ChartScene scene) {
        Map<String, LabelPlacement.Placement> placed = new LinkedHashMap<>();
        BufferedImage canvas = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            for (LabelPlacement.Placement one
                    : new ChartRenderer(StarSizePolicy.DEFAULT)
                            .textPlacements(ChartRenderer.TextMetrics.of(g),
                                    scene, ChartOptions.DEFAULTS)) {
                if (one.request().family()
                        == LabelPlacement.Family.CONSTELLATION) {
                    placed.put(one.request().id(), one);
                }
            }
        } finally {
            g.dispose();
        }
        return placed;
    }

    private static ChartScene named(ChartScene scene,
                                    Map<String, String> names) {
        SceneGeography geography = scene.geography();
        return new ChartScene(scene.viewport(), scene.stars(),
                scene.deepSkyObjects(), scene.title(),
                scene.limitingMagnitude(), scene.targetIdentity(),
                new SceneGeography(geography.figureSegments(),
                        geography.boundarySegments(), names));
    }

    private static Map<String, String> localised(ChartScene scene,
                                                 Map<String, String> lead) {
        Map<String, String> names = new LinkedHashMap<>();
        scene.geography().latinNames().forEach((id, latin) ->
                names.put(id, lead.getOrDefault(id, latin)));
        return names;
    }

    /**
     * The lead, read from its committed copy.
     *
     * <p>Comment lines and the column header are skipped by what
     * they are rather than by their position: the file carries a
     * provenance warning above its data, and a reader that assumed
     * "line one is the header" silently took that header as an
     * eighty-ninth constellation.
     */
    /** The manifest, as stated keys. */
    private static Map<String, String> readManifest() throws IOException {
        Map<String, String> stated = new LinkedHashMap<>();
        for (String line : Files.readAllLines(MANIFEST)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int is = line.indexOf('=');
            if (is > 0) {
                stated.put(line.substring(0, is).strip(),
                        line.substring(is + 1).strip());
            }
        }
        return stated;
    }

    /** SHA-256 of a file, by the same idiom the provenance uses. */
    private static String digestOf(Path file) throws IOException {
        return juranometria.catalog.Sha256.hex(Files.readAllBytes(file));
    }

    private static Map<String, String> readLead() throws IOException {
        Map<String, String> lead = new LinkedHashMap<>();
        for (String line : Files.readAllLines(LEAD)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t");
            if (columns.length != 3 || columns[1].equals("IAU")) {
                continue;
            }
            lead.put(columns[1], columns[2]);
        }
        if (lead.size() != 88) {
            throw new IllegalStateException(
                    "the lead states 88 constellations; found "
                            + lead.size());
        }
        return lead;
    }
}
