# Codex review: Sprint 27 gallery gate

Reviewed PR #263 at `a10029f` against issue #252. All nine selected sources
were inspected at native size, and the generated index and slide pages were
exercised in a browser at desktop and narrow widths.

The curation works. Core chart, On This Page and Place and Time read as three
rooms rather than one mixed feature list. The gate correctly rejected the old
Place-and-Time previews, and `GalleryPageMain` produces the missing module
evidence through the shipped component, registry, module and ink paths.
Captions and alt text explain what matters without pretending UI captures are
chart output. The static navigation remains complete with JavaScript absent.

## [P1] Remove the maintainer's browser chrome from the narrow captures

Both committed narrow captures begin with a strip of the maintainer's browser
UI above the gallery. This contradicts `docs/decisions/gallery.md`, which says
the responsive captures were committed “with the browser chrome cropped
away,” and publishes unrelated personal workspace UI as gallery evidence.

Crop or recapture both narrow images so they contain only the page viewport,
update their captured-evidence digests, and add a structural bound that
prevents this particular capture geometry from returning unnoticed. A digest
can pin the unwanted strip just as faithfully as the intended page; this needs
the capture itself corrected before publication.

## [P2] Make the named release link stable

The manifest says the displayed release is 1.7.0, but its `downloads` value is
the moving `/releases` index. Every generated footer therefore labels a link
“the GitHub release (release 1.7.0)” while the destination will lead to
whatever releases exist later. Use the immutable v1.7.0 release URL
(`/releases/tag/v1.7.0`), preferably derived or checked against the manifest's
release value, so the public gallery's stated provenance and download
destination cannot drift apart after 1.8.0.

## Re-review

Resolved at `5ef5b26`. Both narrow captures now contain only gallery page
content when inspected at native size, their digests are repinned, and the
regression reads every committed gallery capture's own corner patches rather
than a stand-in. The manifest and every derived footer now point to the
immutable v1.7.0 release URL.

No findings remain. PR #263 is approved; publication issue #253 may begin
after this gate merges.
