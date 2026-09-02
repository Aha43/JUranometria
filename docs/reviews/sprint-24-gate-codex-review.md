# Sprint 24 design-gate Codex review

Reviewed PR #219 against issue #214, the Sprint 24 milestone, the
production projection/rendering seams, and the chart/module principles.

## Result

**Changes requested.** The measured scale of the inventory, the fifth
visibility state, the anonymous-star grouping, and the refusal to expose
`Graphics2D` are good decisions. Four gaps must close before #215 begins.

## P1 — Position-on-paper and drawing can disagree at an edge

The decision says using the same paper rectangle as `drawnMarks` means
the table and drawing cannot disagree about where the page ends. They
still can. The study admits an object only when its **recorded centre**
lies inside the rectangle. `ChartRenderer.drawnMarks` admits a deep-sky
object when its **whole symbol outline intersects** that rectangle. The
scene deliberately queries beyond the corners by the pack's maximum
object extent for exactly this reason.

A large galaxy or nebula centred just outside the paper can therefore be
visibly drawn at the edge while absent from “On this page.” Measure this
case across the representative pages and decide it explicitly. Either
include every object whose production mark intersects the paper, in
addition to centre-contained invisible objects, or keep the centre-only
definition while stating and demonstrating the visible-edge exception.
Do not retain the claim that the two cannot disagree.

## P1 — The default order is undefined across stars and deep sky

The prose declares one default order: Messier first, then recorded
brightness, distance, and identity. The study applies it only to the
deep-sky list. The mock-up then places all deep-sky rows before all named
stars; a truly global application of the written comparator would put
several bright named stars among the non-Messier deep-sky rows.

Decide whether the table is grouped by object class or globally sorted.
Specify where the anonymous-star count belongs, how alternate sorts act
across groups, and how unknown/band-different magnitudes compare. Then
measure the complete released-page ordering, stars included, and reverse
the input order to prove the identity tie-break makes it total.

## P1 — The mock-ups claim catalogue evidence but contain invented rows

`OnThisPageMockupMain` describes its rows as real and says invented rows
would test nothing, but every row is hand-written. At least `VCC 1030`
does not exist as an identity in the bundled catalogue. The dense-page
summary is also manually copied rather than derived from the measured
inventory, so the decision and picture can drift while regeneration
remains byte-identical.

Build mock-up models from the same independently measured catalogue/page
answers, with an assertion that every object row resolves to the exact
catalogue identity and recorded facts shown. Keep presentation fixtures
only where they are explicitly labelled illustrative rather than
evidence.

## P2 — Keyboard and real-layout evidence required by #214 is absent

The issue asks for real mock-ups covering keyboard-only use. The tool
constructs an off-screen `JTable`, manually lays out its component tree,
preselects rows programmatically, and paints a bitmap. It opens no real
window, establishes no focus, and drives no selection, sorting, or
multi-selection controls. That is useful visual evidence but not the
interaction evidence the gate claims to deliver.

Add a display-backed gate test or study that uses the proposed real
Swing surface in a laid-out window and proves its focus order and normal
platform selection gestures at ordinary and enlarged text. If detailed
interaction is deliberately deferred to #216, narrow #214's acceptance
and decision language honestly rather than calling these images evidence
of keyboard usability.

## Verification note

`make on-this-page-study` reproduced cleanly with Java explicitly in
headless mode. In this review environment the unqualified command aborted
in the mock-up process; no production or study files remained changed.
The gate should either make its headless requirement explicit in the
target or establish why the ordinary invocation is portable.

Do not merge PR #219 or begin #215 until these findings are resolved.
