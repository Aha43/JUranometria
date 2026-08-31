#!/bin/sh
# Where a release comes from (issue #88, automation review): the tag
# must be annotated, must point at the commit being built, and that
# commit must already belong to the protected branch.
#
#   scripts/release-provenance.sh <tag> <expected-sha> [tree] [branch]
#
# Agreement about versions is not provenance. Without this, an
# annotated tag on an unmerged branch publishes a release that no
# review ever saw, and a tag force-moved while the build runs
# publishes artifacts the tag no longer names.
#
# The workflow runs this twice: before building anything, and again
# immediately before the draft is created. The second run is the one
# that catches a tag moved in between - which is why it is a script
# rather than a step, so both callers make exactly the same check.
#
#   0   the release may proceed
#   8   the tag is lightweight, not annotated
#   9   the tag no longer points at the commit being built
#   10  that commit is not on the protected branch
set -eu

tag="${1:-}"
expected="${2:-}"
tree="${3:-.}"
branch="${4:-origin/main}"
if [ -z "$tag" ] || [ -z "$expected" ]; then
    echo "usage: $0 <tag> <expected-sha> [tree] [branch]" >&2
    exit 64
fi
cd "$tree"

kind="$(git cat-file -t "$tag" 2>/dev/null || echo missing)"
if [ "$kind" != tag ]; then
    if [ "$kind" = missing ]; then
        echo "no such tag: $tag" >&2
    else
        echo "$tag is a $kind, not an annotated tag" >&2
        echo "release tags carry their own message and author:" >&2
        echo "  git tag -a $tag -m 'JUranometria ${tag#v}'" >&2
    fi
    exit 8
fi

target="$(git rev-list -n 1 "$tag")"
if [ "$target" != "$expected" ]; then
    echo "$tag points at $target, not $expected" >&2
    echo "the tag moved, or this run was started for another" \
         "commit; nothing is published from a tag that no longer" \
         "names what was built" >&2
    exit 9
fi

if ! git merge-base --is-ancestor "$target" "$branch" 2>/dev/null; then
    echo "$tag ($target) is not on $branch" >&2
    echo "a release is published from reviewed, merged work only;" \
         "merge the commit first, then tag it there" >&2
    exit 10
fi

echo "$tag is annotated, points at $target, and that commit is on $branch"
