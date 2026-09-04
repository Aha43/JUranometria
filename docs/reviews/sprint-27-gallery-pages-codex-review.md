# Codex review: Sprint 27 gallery publication

Reviewed PR #264 at `e5eae74` against issue #253 and the public Pages site.
The generated artifact is self-contained, its nine slides remain derived from
the reviewed manifest, and the public desktop and narrow views preserve the
three-room structure. Direct slide URLs, plain previous/next links, keyboard
navigation, the immutable 1.7.0 release link and the distinction between core,
module and application UI evidence all hold. The temporary review-branch
deployment is clearly marked for removal before merge.

The revised public voice is also right. The polar caption now explains the
coordinate geometry visible on the page without inventing a lesser chart or
reader to compare against it, and the two smaller claims in the same register
have been made plain.

## [P1] Put the editorial rule in the documentation review path

`docs/decisions/gallery.md` says its new rule governs “the gallery and all
future documentation,” but that file is the historical design record for one
feature. A future README, module or release-note author has no ordinary reason
to read it, and the project’s actual development/review guidance does not name
the rule. The correction therefore fixes this gallery while leaving the wider
promise dependent on remembering this review.

Add a short editorial criterion to `docs/development.md` (or another general
writing guide that `docs/development.md` points reviewers to), and make the
gallery decision refer to that shared rule. It should preserve the useful
distinction the owner made here: write confidently about what JUranometria
lets a reader see and do, while avoiding unsupported superlatives, uniqueness
claims, marketing comparisons, or wording that implies other tools or readers
failed. Where relevant, the same criterion should keep provenance and the
project's place among the open catalogues, astronomy sources, printed atlases
and software it builds beside visible.

This is a human review criterion, not a banned-word scanner. Words such as
“honest” have precise uses in the rendering and test contracts; the problem is
the implied comparison, which requires reading the sentence.
