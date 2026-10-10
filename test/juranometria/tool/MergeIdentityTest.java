package juranometria.tool;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exact-tree post-merge rule (issue #494), held case by case: the
 * accepted case is structurally verified; every case the rule names as
 * fail-closed runs the full route, with the reason said.
 */
class MergeIdentityTest {

    private static final String BASE = "7746098b50fb9744a771537432a0adb42011aab6";
    private static final String HEAD = "da0525ff1db933d72197a13157ec307e23112a62";
    private static final String TRIAL = "39db4b1bb67d4c500277827b4c41c5d79845f10d";
    private static final String MERGE = "3102ef26468f471bec93f570f1dcf570ee07bf11";
    private static final String TREE = "e8fec3a50f1f99910adeb036306e5506fc42782f";

    /** The Sprint 45 handover's own landing (#493): narrow, every fact agreeing. */
    private static Map<String, String> landed() {
        Map<String, String> f = new HashMap<>();
        f.put("merge", MERGE);
        f.put("merge.parents", BASE + " " + HEAD);
        f.put("merge.tree", TREE);
        f.put("accepted.head", HEAD);
        run(f, "test", "38057637753", "narrow",
                "test=success;display=success;classify / classify=success;"
                        + "evidence=skipped;interaction-evidence=skipped");
        run(f, "app-image", "38057637755", "narrow",
                "jar=success;classify / classify=success;image=skipped;"
                        + "smoke-cross-architecture=skipped");
        run(f, "dist", "38057637737", "narrow",
                "classify / classify=success;build=skipped;verify=skipped");
        return f;
    }

    private static void run(Map<String, String> f, String w, String id, String route,
                            String jobs) {
        f.put("run." + w + ".id", id);
        f.put("run." + w + ".conclusion", "success");
        f.put("run." + w + ".trial", TRIAL);
        f.put("run." + w + ".trial.parents", BASE + " " + HEAD);
        f.put("run." + w + ".trial.tree", TREE);
        f.put("run." + w + ".route", route);
        f.put("run." + w + ".jobs", jobs);
    }

    private static void refused(Map<String, String> f, String because) {
        MergeIdentity.Verdict v = MergeIdentity.judge(f);
        assertFalse(v.qualified(), "fail-closed: " + because);
        assertTrue(v.reasons().stream().anyMatch(r -> r.contains(because)),
                "the reason is said (" + because + "): " + v.reasons());
    }

    @Test
    void theAcceptedCaseIsStructurallyVerified() {
        MergeIdentity.Verdict v = MergeIdentity.judge(landed());
        assertTrue(v.qualified(), v.reasons().toString());
        assertTrue(v.proven().stream().anyMatch(p -> p.startsWith("1. ")));
        assertTrue(v.proven().stream().anyMatch(p -> p.startsWith("2. ")));
        assertTrue(v.proven().stream().anyMatch(p -> p.startsWith("3. ")));
        assertTrue(v.proven().stream().anyMatch(p -> p.startsWith("4. ")));
    }

    @Test
    void aWideLandingNeedsEvidenceImagesAndTheArchiveGreen() {
        Map<String, String> f = landed();
        run(f, "test", "1", "wide", "test=success;display=success;classify / classify=success;"
                + "evidence=success;interaction-evidence=skipped");
        run(f, "app-image", "2", "wide", "jar=success;classify / classify=success;"
                + "image (macos-15, macos-arm64)=success;smoke-cross-architecture=success");
        run(f, "dist", "3", "wide", "classify / classify=success;build=success;"
                + "verify (ubuntu-latest)=success");
        assertTrue(MergeIdentity.judge(f).qualified());
        f.put("run.test.jobs", "test=success;display=success;classify / classify=success;"
                + "evidence=skipped");
        refused(f, "needs 'evidence' on the wide route");
        f = landed();
        run(f, "test", "1", "wide", "test=success;display=success;classify / classify=success;"
                + "evidence=success");
        run(f, "app-image", "2", "wide", "jar=success;classify / classify=success;"
                + "image (macos-15, macos-arm64)=skipped;smoke-cross-architecture=success");
        run(f, "dist", "3", "wide", "classify / classify=success;build=success;"
                + "verify (ubuntu-latest)=success");
        refused(f, "'image (...)' jobs");
    }

    @Test
    void mainAdvancedAfterQualification() {
        Map<String, String> f = landed();
        f.put("merge.parents", "1111111111111111111111111111111111111111 " + HEAD);
        refused(f, "main advanced");
    }

    @Test
    void theAcceptedHeadDiffers() {
        Map<String, String> f = landed();
        f.put("merge.parents", BASE + " 2222222222222222222222222222222222222222");
        refused(f, "is not the accepted head");
    }

    @Test
    void theMergeTreeDiffers() {
        Map<String, String> f = landed();
        f.put("merge.tree", "3333333333333333333333333333333333333333");
        refused(f, "is not the qualified trial tree");
    }

    @Test
    void ciQualifiedOnlyTheBranchHead() {
        Map<String, String> f = landed();
        f.put("run.test.trial", HEAD);
        f.put("run.test.trial.parents", BASE);
        refused(f, "qualified the branch head alone");
    }

    @Test
    void aRunQualifiedADifferentTrialMerge() {
        Map<String, String> f = landed();
        f.put("run.dist.trial", "4444444444444444444444444444444444444444");
        refused(f, "like the others");
        f = landed();
        f.put("run.app-image.trial.parents", BASE + " 5555555555555555555555555555555555555555");
        refused(f, "not of the accepted head");
    }

    @Test
    void aRequiredCheckFailedWasSkippedOrIsIncomplete() {
        Map<String, String> f = landed();
        f.put("run.test.conclusion", "failure");
        refused(f, "concluded failure");
        f = landed();
        f.put("run.test.jobs", "test=success;display=cancelled;classify / classify=success");
        refused(f, "'display' concluded cancelled");
        f = landed();
        f.put("run.test.jobs", "test=success;display=skipped;classify / classify=success");
        refused(f, "needs 'display'");
        f = landed();
        f.remove("run.dist.id");
        f.remove("run.dist.conclusion");
        refused(f, "'run.dist.conclusion' could not be established");
    }

    @Test
    void aStructuralFactThatCannotBeProvedFailsClosed() {
        for (String key : new String[] {"merge.tree", "merge.parents", "accepted.head",
                "run.test.trial", "run.test.trial.tree", "run.app-image.route"}) {
            Map<String, String> f = landed();
            f.remove(key);
            refused(f, "'" + key + "' could not be established");
        }
        Map<String, String> f = landed();
        f.put("run.test.route", "medium");
        refused(f, "unknown route");
        assertFalse(MergeIdentity.judge(Map.of()).qualified(), "no facts, no verification");
    }

    @Test
    void aSquashOrRebaseIsNotTheQualifiedTrialMerge() {
        Map<String, String> f = landed();
        f.put("merge.parents", BASE);
        refused(f, "not a two-parent merge");
    }

    @Test
    void routesMustAgreeAcrossTheRuns() {
        Map<String, String> f = landed();
        f.put("run.dist.route", "wide");
        refused(f, "the others narrow");
    }
}
