package juranometria.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Whether a merge to {@code main} is the very tree its pull request's CI
 * already qualified (issue #494, docs/decisions/post-merge-qualification.md).
 *
 * <p>A pull request's runs check out GitHub's trial merge of the head into
 * the base, not the head. When the merge that lands has the accepted head
 * as its exact second parent, the qualified base as its first, and the
 * trial merge's tree as its own, rebuilding and retesting it after merge
 * proves nothing new. This class decides that, from facts gathered by
 * {@code scripts/verify-merge-identity.sh} - every parent and tree read
 * from git objects, never from prose.
 *
 * <p>It is fail-closed. Every fact it needs must be present and agree;
 * anything missing, unknown or different is a reason, and any reason
 * means the appropriate full route runs. It never waives an owner
 * checkpoint, a failed or incomplete required check, a release
 * qualification or a publication check, and it says nothing about
 * main-only workflows, which run on their own triggers regardless.
 *
 * <p>Pure: it reads a map of facts and starts no process, as every class
 * that ships must ({@code OfflinePromiseTest}).
 */
public final class MergeIdentity {

    /** The pull-request workflows whose runs must all have qualified the trial merge. */
    public static final List<String> WORKFLOWS = List.of("test", "app-image", "dist");

    private MergeIdentity() {
    }

    /** The verdict: proven, or the full route with every reason it was not. */
    public record Verdict(boolean qualified, List<String> reasons, List<String> proven) {

        public Verdict {
            reasons = List.copyOf(reasons);
            proven = List.copyOf(proven);
        }
    }

    /**
     * Judges the facts.
     *
     * <p>Keys: {@code merge}, {@code merge.parents} (space separated, in
     * order), {@code merge.tree}, {@code accepted.head}; and for each of
     * {@link #WORKFLOWS}, {@code run.<w>.id}, {@code run.<w>.conclusion},
     * {@code run.<w>.trial}, {@code run.<w>.trial.parents},
     * {@code run.<w>.trial.tree}, {@code run.<w>.route} and
     * {@code run.<w>.jobs} ({@code name=conclusion} pairs separated by
     * {@code ;}).
     */
    public static Verdict judge(Map<String, String> facts) {
        List<String> no = new ArrayList<>();
        List<String> yes = new ArrayList<>();

        String merge = fact(facts, "merge", no);
        String accepted = fact(facts, "accepted.head", no);
        String mergeTree = fact(facts, "merge.tree", no);
        List<String> mergeParents = parents(fact(facts, "merge.parents", no));
        if (mergeParents != null && mergeParents.size() != 2) {
            no.add("the landed commit " + shortOf(merge) + " has " + mergeParents.size()
                    + " parent(s), not a two-parent merge (a squash or rebase is not the"
                    + " qualified trial merge)");
            mergeParents = null;
        }
        if (mergeParents != null && accepted != null) {
            if (mergeParents.get(1).equals(accepted)) {
                yes.add("1. its second parent is the accepted head " + shortOf(accepted));
            } else {
                no.add("1. its second parent " + shortOf(mergeParents.get(1))
                        + " is not the accepted head " + shortOf(accepted));
            }
        }

        String trial = null;
        String trialTree = null;
        List<String> trialParents = null;
        String route = null;
        for (String w : WORKFLOWS) {
            String p = "run." + w + ".";
            String id = fact(facts, p + "id", no);
            String conclusion = fact(facts, p + "conclusion", no);
            String runTrial = fact(facts, p + "trial", no);
            String runTree = fact(facts, p + "trial.tree", no);
            List<String> runParents = parents(fact(facts, p + "trial.parents", no));
            String runRoute = fact(facts, p + "route", no);
            String jobs = fact(facts, p + "jobs", no);
            if (conclusion != null && !conclusion.equals("success")) {
                no.add("4. the " + w + " run " + id + " concluded " + conclusion + ", not success");
            }
            if (runParents != null && runParents.size() != 2) {
                no.add("4. the " + w + " run " + id + " qualified " + shortOf(runTrial)
                        + ", which is not a two-parent trial merge: it qualified the branch"
                        + " head alone");
                runParents = null;
            }
            if (runParents != null && accepted != null && !runParents.get(1).equals(accepted)) {
                no.add("4. the " + w + " run " + id + " qualified a merge of "
                        + shortOf(runParents.get(1)) + ", not of the accepted head "
                        + shortOf(accepted));
            }
            if (runRoute != null && !List.of("narrow", "interaction", "wide").contains(runRoute)) {
                no.add("4. the " + w + " run " + id + " recorded an unknown route '" + runRoute + "'");
                runRoute = null;
            }
            if (runTrial != null) {
                if (trial == null) {
                    trial = runTrial;
                    trialTree = runTree;
                    trialParents = runParents;
                } else if (!trial.equals(runTrial)) {
                    no.add("4. the " + w + " run " + id + " qualified trial merge "
                            + shortOf(runTrial) + ", not " + shortOf(trial)
                            + " like the others");
                }
            }
            if (runRoute != null) {
                if (route == null) {
                    route = runRoute;
                } else if (!route.equals(runRoute)) {
                    no.add("4. the " + w + " run " + id + " took the " + runRoute
                            + " route, the others " + route);
                }
            }
            if (jobs != null && runRoute != null) {
                requiredJobs(w, id, runRoute, jobs, no);
            }
        }

        if (mergeParents != null && trialParents != null) {
            if (mergeParents.get(0).equals(trialParents.get(0))) {
                yes.add("2. its first parent is the qualified base " + shortOf(trialParents.get(0)));
            } else {
                no.add("2. its first parent " + shortOf(mergeParents.get(0))
                        + " is not the qualified base " + shortOf(trialParents.get(0))
                        + ": main advanced after CI qualified the trial merge");
            }
        }
        if (mergeTree != null && trialTree != null) {
            if (mergeTree.equals(trialTree)) {
                yes.add("3. its tree " + shortOf(mergeTree) + " is the qualified trial merge "
                        + shortOf(trial) + "'s tree");
            } else {
                no.add("3. its tree " + shortOf(mergeTree) + " is not the qualified trial tree "
                        + shortOf(trialTree));
            }
        }
        if (no.isEmpty()) {
            yes.add("4. the test, app-image and dist runs all qualified trial merge "
                    + shortOf(trial) + " on the " + route + " route, every required job green");
        }
        return new Verdict(no.isEmpty(), no, yes);
    }

    /** The jobs a route requires to have succeeded, and none of any job failing. */
    private static void requiredJobs(String workflow, String id, String route, String jobs,
                                     List<String> no) {
        Map<String, String> byName = new java.util.LinkedHashMap<>();
        for (String pair : jobs.split(";")) {
            int at = pair.lastIndexOf('=');
            if (at > 0) {
                byName.put(pair.substring(0, at).trim(), pair.substring(at + 1).trim());
            }
        }
        for (Map.Entry<String, String> job : byName.entrySet()) {
            if (!job.getValue().equals("success") && !job.getValue().equals("skipped")) {
                no.add("4. the " + workflow + " run " + id + "'s job '" + job.getKey()
                        + "' concluded " + job.getValue());
            }
        }
        List<String> required = new ArrayList<>(List.of("classify / classify"));
        List<String> prefixes = new ArrayList<>();
        boolean full = !route.equals("narrow");
        switch (workflow) {
            case "test" -> {
                required.add("test");
                required.add("display");
                if (route.equals("wide")) {
                    required.add("evidence");
                }
                if (route.equals("interaction")) {
                    required.add("interaction-evidence");
                }
            }
            case "app-image" -> {
                required.add("jar");
                if (full) {
                    required.add("smoke-cross-architecture");
                    prefixes.add("image (");
                }
            }
            case "dist" -> {
                if (full) {
                    required.add("build");
                    prefixes.add("verify (");
                }
            }
            default -> no.add("4. no required jobs are known for workflow " + workflow);
        }
        for (String name : required) {
            String conclusion = byName.get(name);
            if (!"success".equals(conclusion)) {
                no.add("4. the " + workflow + " run " + id + " needs '" + name + "' on the "
                        + route + " route; it " + (conclusion == null ? "did not run"
                                : "concluded " + conclusion));
            }
        }
        for (String prefix : prefixes) {
            boolean any = byName.entrySet().stream().anyMatch(e -> e.getKey().startsWith(prefix)
                    && e.getValue().equals("success"));
            if (!any) {
                no.add("4. the " + workflow + " run " + id + " needs '" + prefix + "...)' jobs"
                        + " on the " + route + " route; none succeeded");
            }
        }
    }

    private static String fact(Map<String, String> facts, String key, List<String> no) {
        String value = facts.get(key);
        if (value == null || value.isBlank()) {
            no.add("the fact '" + key + "' could not be established");
            return null;
        }
        return value.trim();
    }

    private static List<String> parents(String value) {
        return value == null ? null : List.of(value.trim().split("\\s+"));
    }

    private static String shortOf(String sha) {
        return sha == null ? "(unknown)" : sha.length() > 8 ? sha.substring(0, 8) : sha;
    }
}
