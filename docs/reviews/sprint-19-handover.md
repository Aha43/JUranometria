# Sprint 19 handover — Explore the map

Sprint 19 (issues #168–#171, milestone 19) gave the atlas the
question it could not answer. It could always find Betelgeuse; a
reader looking at the sky asks the opposite — **"what is that?"** —
and until now the only way to ask was to guess a name and search for
it. This handover is the coder's account for the review before the
release decision.

This is the first sprint after 1.0, and the first shaped by actual
use rather than by a plan written in advance.

## What Sprint 19 delivered

- **The gate** (#168, PR #172):
  [`docs/decisions/point-and-identify.md`](../decisions/point-and-identify.md),
  measured over nine real pages with `make identify-study`. What is
  selectable, the hit geometry and its four-pixel tolerance,
  ambiguity ordering, the selection payload, the inspector's
  content, its keyboard and layout, the relationship between
  selection, target and navigation, and the ring highlight.
- **The foundation** (#169, PR #173): `Selection`, `SelectionModel`
  and `ChartHitTest` — the state UI-independent, the geometry read
  from the renderer's own placements.
- **The inspector** (#170, PR #174): the first consumer, with the
  ring, `Center here`, the keyboard route, and the responsive
  layout.
- **The journey** (#171, this PR): `MapExplorationJourneyTest`, the
  packaged selection leg, and this handover.

## Hit tolerance, ambiguity, and what the measurements settled

Four pixels, in page pixels, expanding each mark's **own footprint**.
At that tolerance the intended mark is in the answer for 93–100% of
hand-wobbled clicks while 69–98% remain unambiguous. It does not
scale with field width: it models a hand, not the sky.

Ambiguity is real — 12.7% of aimed clicks on the default page and
31% at 36°, worst case ten candidates — so the atlas offers an
ordered choice and never resolves it. The order is **ink before
nearness**, then distance rounded to a tenth of a pixel, then the
tighter mark, then catalogue identity: four keys that mean the same
thing for a star and a galaxy, and a stable one last so the answer
can never depend on iteration order.

The journey found the rule working in a way no one wrote down: in
Crux, a star lies **inside** IC 2944's outline, so the nebula the
reader is standing on leads and the star follows. The test asserts
the star is *offered and reachable* rather than demanding it come
first — which is the contract, and which my first draft got wrong.

## Catalogue honesty

The gate found the inspector it had designed could not be built
honestly. Measured over the pack's 13,371 deep-sky rows: **19.4%**
record no position angle, **68.1%** no V magnitude, **54.4%** carry
only a B magnitude, **9.7%** no extent. The loader substituted a
nominal size, a position angle of exactly zero, and stored V-or-B in
one unlabelled field — so a panel built on it would have stated a
size nobody measured and a **blue magnitude labelled visual for most
of the catalogue**.

`DeepSkyObject` now carries `Recorded` beside its display values, the
renderer's contract untouched. The inspector says "magnitude not
recorded", "size not recorded", "orientation not recorded" where
those are the truth, and a test pins the loaded model's exact counts
against the gate's independent measurement of the raw CSV.

## Accessibility

Every control names itself, the panel is in the focus order, the
candidate list walks with the arrow keys and settles with Enter into
the facts, and Escape closes the inspector and hands focus back to
the chart. **The honest limit, stated rather than buried**: there is
no keyboard cursor walking star to star, because "the next star"
across a projected page is a design of its own. A reader without a
pointer arrives by searching — which now selects what it finds,
without which the inspector would have been unreachable for them
entirely. That gap was caught in review, not by me.

## Performance and state ownership

Selection carries identity and position only; details are read from
the page already assembled, so **answering costs no catalogue query
and no scene assembly**. The journey asserts this directly: after a
click, the view state is unchanged *and the same `ChartScene`
instance is still in place*. Choosing a different candidate
reassembles nothing either.

`SelectionModel` is a listener list and nothing more — no lifecycle,
no discovery, no plugin API. The journey runs a second, independent
observer beside the inspector and requires it to hear exactly what
the inspector hears, so the seam is demonstrated rather than
asserted.

## The journey drives the controls, not the classes

The first draft of the journey reached its destinations by calling
the controller, applying search results directly, setting the
panel's visibility, choosing candidates through the model, and
resetting the view in code. It passed, and it proved much less than
it claimed (sprint review). It now goes through the surfaces a
reader actually touches:

| step | driven by |
|---|---|
| open and close the inspector | the **View → Inspector** menu item, whose checkbox must then agree |
| search | typing into the field and pressing Enter |
| zoom | real mouse-wheel events on the paper |
| pan | real press-drag-release |
| choose a candidate | the panel's own list |
| Home | the toolbar's **Reset view** button |
| hide a layer | the chart-options controller the dialog drives |

and it adds the cases the issue named and the first draft skipped: a
**hidden layer cannot be pointed at**, a click on real **letterbox
chrome** asks nothing, the **narrow window** closes the panel while
the chart still answers, **both themes** describe the same object,
chart options and magnitude **end as they began**, and the final
page is compared **pixel for pixel with the released reference**.

Two premises were tightened as well. The star the reader points at
is now proven **unlabelled** against the renderer's own label
placements — which is the whole reason a reader must ask — and a
deep-sky object the catalogue records poorly is sought deliberately
rather than taken by luck, so the silences are exercised on purpose.
The second observer must hear **exactly one** event per action,
carrying the same candidates and current index as the model.

## Verification

- **423 tests** on a display; headless, the display-dependent
  journeys abort visibly rather than fail (verified: the inspector
  and exploration classes report 17 passed, 1 aborted, 0 failed).
- The M31 reference and every study page reproduce **byte-for-byte**:
  the highlight is a separate pass after the chart, so the chart is
  what it always was.
- `make identify-study` regenerates its measurements and pictures
  deterministically.
- **Point-and-identify runs inside the packaged application** on all
  four platform images, with no system Java: a drawn star is found,
  identified from the packaged catalogue, the shared model tells its
  consumers exactly once, and the letterbox surround answers
  nothing.
- The responsive layout is measured in a real frame: at 640 px the
  chart keeps exactly its promised 400 px and the panel takes 240.

## Residual risks

- **The inspector describes; it does not yet explore.** No history,
  no "next object", no measurement between two marks. That is
  deliberate for a first sprint on this, and the seam is ready if
  use asks for more.
- **Constellation is still absent** from the inspector. The bundled
  geography carries boundary segments, not closed polygons, so
  containment needs polygon assembly and its own correctness study
  at the poles and across RA 0. It remains the strongest candidate
  for the next sprint.
- **Keyboard reach depends on search.** Honest, stated, and the
  first thing to revisit if a reader without a pointer finds it
  thin.
- **The tolerance is one number for every display.** It was measured
  in page pixels on a 900×700 page; a very high-density display may
  want a different hand-model, and nothing measures that yet.
- **Ambiguity ordering is a judgement.** "Ink before nearness" and
  "the tighter mark wins" are defensible and tested, but they are
  choices; a reader who disagrees will disagree consistently, which
  is the most that can be promised.

## What this sprint cost, honestly

Five review rounds across four PRs, and the findings were not
cosmetic: measurements inflated by 466 invisible objects on the
default page, an inspector the data could not honestly support, a
hit region an order of magnitude too large, event delivery that
could hand a consumer a stale state, and a keyboard route that did
not exist. Every one of them passed a green build. The gate-first
pattern is why they were found in a document or a foundation rather
than in a reader's hands.

## Recommendation

Release when the review is satisfied — this is a **minor** release:
new behaviour, no promise of the 1.0 contract changed, the default
chart byte-identical. Then keep dogfooding: the next sprint should
come from using this, as this one did.
