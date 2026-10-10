#!/usr/bin/env bash
# Is a commit on main the very tree its pull request's CI already
# qualified? (issue #494, docs/decisions/post-merge-qualification.md)
#
#   scripts/verify-merge-identity.sh <merge> [--accepted-head <sha>]
#       [--github-output FILE] [--summary FILE]
#
# Gathers the facts juranometria.tool.MergeIdentity judges and judges
# them. The landed commit's parents and tree, and the parents and tree
# of the trial merge each pull-request run qualified, are read from git
# objects - the trial merge is fetched by the ID its run's classify job
# recorded in the `qualified-merge` artifact - so nothing rests on
# prose. The runs, their conclusions and their jobs come from the
# GitHub API.
#
# Fail-closed by construction: a fact that cannot be established is
# simply not written, and the judge refuses on any missing fact. The
# answer is qualified=true only when everything is proved. Without
# --accepted-head, the accepted head is the head of the pull request
# GitHub records as having produced the merge.
set -u

if [ $# -lt 1 ]; then
  echo "usage: $0 <merge> [--accepted-head SHA] [--github-output FILE] [--summary FILE]" >&2
  exit 2
fi
merge_arg=$1
shift
accepted=""
gh_output=""
summary=""
while [ $# -gt 0 ]; do
  case "$1" in
    --accepted-head) accepted=$2; shift 2 ;;
    --github-output) gh_output=$2; shift 2 ;;
    --summary) summary=$2; shift 2 ;;
    *) echo "unknown option $1" >&2; exit 2 ;;
  esac
done

out=build/merge-identity
rm -rf "$out"
mkdir -p "$out"
facts=$out/facts.properties
: > "$facts"
note() { [ -n "${2:-}" ] && printf '%s=%s\n' "$1" "$2" >> "$facts"; return 0; }

repo=${GITHUB_REPOSITORY:-$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null)}

merge=$(git rev-parse --verify -q "${merge_arg}^{commit}" 2>/dev/null || true)
if [ -n "$merge" ]; then
  note merge "$merge"
  note merge.parents "$(git rev-list --parents -n 1 "$merge" | cut -d' ' -f2-)"
  note merge.tree "$(git rev-parse "${merge}^{tree}")"
  if [ -z "$accepted" ]; then
    accepted=$(gh api "repos/$repo/commits/$merge/pulls" \
      --jq "[.[] | select(.merge_commit_sha == \"$merge\")][0].head.sha // empty" 2>/dev/null || true)
  fi
fi
note accepted.head "$accepted"

if [ -n "$accepted" ]; then
  for w in test app-image dist; do
    run=$(gh api "repos/$repo/actions/workflows/$w.yml/runs?event=pull_request&head_sha=$accepted&per_page=50" \
      --jq '[.workflow_runs[] | select(.status == "completed")] | sort_by(.run_started_at) | last
            | if . == null then empty else "\(.id) \(.conclusion)" end' 2>/dev/null || true)
    [ -z "$run" ] && continue
    id=${run%% *}
    note "run.$w.id" "$id"
    note "run.$w.conclusion" "${run#* }"
    note "run.$w.jobs" "$(gh api "repos/$repo/actions/runs/$id/jobs?per_page=100" \
      --jq '[.jobs[] | "\(.name)=\(.conclusion)"] | join(";")' 2>/dev/null || true)"
    if gh run download "$id" -R "$repo" -n qualified-merge -D "$out/$w" >/dev/null 2>&1 \
        && [ -f "$out/$w/qualified-merge.txt" ]; then
      trial=$(sed -n 's/^merge=//p' "$out/$w/qualified-merge.txt")
      note "run.$w.route" "$(sed -n 's/^route=//p' "$out/$w/qualified-merge.txt")"
      if [ -n "$trial" ]; then
        git cat-file -e "${trial}^{commit}" 2>/dev/null || git fetch -q origin "$trial" 2>/dev/null || true
        if git cat-file -e "${trial}^{commit}" 2>/dev/null; then
          note "run.$w.trial" "$trial"
          note "run.$w.trial.parents" "$(git rev-list --parents -n 1 "$trial" | cut -d' ' -f2-)"
          note "run.$w.trial.tree" "$(git rev-parse "${trial}^{tree}")"
        fi
      fi
    fi
  done
fi

echo "facts gathered in $facts"
mkdir -p "$out/classes"
javac -d "$out/classes" src/juranometria/tool/MergeIdentity.java \
  src/juranometria/tool/MergeIdentityMain.java || exit 1
java -cp "$out/classes" juranometria.tool.MergeIdentityMain --facts "$facts" \
  ${gh_output:+--github-output "$gh_output"} ${summary:+--summary "$summary"}
