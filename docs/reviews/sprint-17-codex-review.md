# Sprint 17 Codex review — Letter the stars

Reviewed PR #162 at `8bdaa51`, including the Sprint 17 decision,
rendering and controls already reviewed in PRs #157, #160, and #161;
the closing journey; the regenerated Sprint 13 and Sprint 15 study
pages; the handover; and the current GitHub verification matrix.

## Finding

### P1 — The boundary-pan assertion proves neither determinism nor the claimed target transition

`LetteredStarJourneyTest` describes its drag as crossing a
constellation boundary deterministically and clearing the searched
target atomically, but the evidence does not establish either claim.
`goTo(120.0, 20.0, 18.0)` has already replaced the Dubhe search with a
coordinate recenter, so `targetIdentity` is null before the mouse press.
The test then checks only that the state changed and evaluates
`assertEquals(labels(), labels())`; two consecutive calls in the same
assertion are a tautology for the deterministic code path and contain
no expected post-drag result. A missing target-clear transition or a
drag landing on the wrong reproducible page would still pass.

Exercise the contract through the same real mouse events, but preserve
or establish a real target immediately before the drag if target
clearing remains part of the claimed journey. Assert it is present
before the accepted drag and absent in the same resulting transition.
Give the boundary journey an independent oracle: for example, assert a
known post-drag centre and known visible geography/labels, then replay
the same drag from the same starting state and compare the resulting
state and placed labels (or rendered pixels). The assertion must also
make clear what demonstrates that the chosen motion actually crosses
the intended constellation boundary. If target clearing is not part of
this Sprint 17 journey, remove that claim from the test commentary, PR,
and handover rather than implying it was re-proven here.

## Other review results

- The 18 regenerated production-study images are accounted for. An
  independent pixel comparison confirmed the stated 474 changed pixels
  on the widest Orion page and the 81-pixel, 14×10 M31 notation change.
- The sprint's policy remains option-free, and production and study
  code share the renderer's accepted-placement decision rather than
  duplicating collision logic.
- The three persisted controls retain the reviewed migration,
  repaint-only, target-exemption, and Restore Defaults contracts.
- GitHub reports the PR mergeable and clean. The required test,
  portable distribution checks, all four native-image cells, and the
  cross-architecture smoke check are green on the reviewed head.
- The brief local-main slip has no repository effect: the PR is a
  clean single commit over remote `main`.

## Release recommendation

Do not merge or cut 0.17.0 until the journey evidence above is repaired
and the corrected current head is green. No production-code defect was
found in the delivered Bayer/Flamsteed notation itself.
