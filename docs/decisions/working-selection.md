# Decision: the temporary working selection across chart pages

Decided 2026-09-04 for Sprint 27, issue #258, from the census and
composed evidence of `make working-selection-study`
(`docs/studies/working-selection/measurements.md`, with the
evidence images and surface mock-ups beside it). This gate changes
no production behaviour; the foundation (#260) and the reader
surfaces (#261) implement exactly what is decided here.

## What it is, and firmly is not

An ordered set of catalogue identities with one lead, built while
moving through the atlas, kept whole when the chart moves away
from its members, and gone at the end of the session. It is a
working set for an observing evening or a comparison — not an
observing list, planner, note layer, or saved collection, and
nothing about it is ever written to preferences or any other
store. A future planner module may consume the set and persist its
own domain object; that possibility adds nothing here.

## Today: two truths that can disagree

`SelectionModel` (Sprint 19) answers the last question — nothing,
empty sky, or one object with the click's candidates — and feeds
the Inspector and, through the application wiring, the chart's one
selection ring. `WorkingMarksModel` (Sprint 24) holds the ordered
marked set with its lead, feeds the On-this-page table and the
cross contributions, and is pruned to the page on every page
change. The cross painter takes its *lead* treatment from the
SelectionModel identity while Center here takes its lead from the
marks model: `today-two-leads.png` photographs the ring on M 32
and the lead cross on NGC 206 in one frame — two leads, one
reader. This decision ends that.

## One model: ownership and migration

**The working selection is one session-level model** — membership,
order, lead, whole-state transitions, reentrant queued delivery —
grown from `WorkingMarksModel`, whose semantics four review rounds
already settled: an ordered set that rejects duplicates by
construction, one lead that is always a member, transitions
delivered whole in one order for every consumer, and each change
built on the last queued state so nested changes cannot undo each
other. Migration, for #260:

- `WorkingMarksModel` moves out of page scope (to the chart
  package, beside `SelectionModel`, as `WorkingSelection`), keeps
  `mark`/`unmark`/`lead`/`replaceWith`/`clear` semantics, gains
  `toggle(identity)` for the accumulate gestures, and **loses
  `pruneTo`**: page navigation never mutates the set.
- `SelectionModel` remains the *answering* model — what the last
  gesture asked: facts, empty sky, the ambiguity candidates — and
  stops being a second identity truth: production wiring drives it
  from the working selection's lead, so the Inspector's answer,
  the chart's ink and Center here all read one lead. Its
  candidate chooser retargets the lead (and, under replace
  semantics, the single member) rather than holding a separate
  selection.
- `ChartServices` exposes the promoted model where
  `workingMarks()` stands today; the module rule holds: removing
  the On-this-page UI leaves an ordinary chart whose selection
  service works, because the model lives in the core and the
  table was only ever a consumer.
- **Accumulate is session interaction state, not membership**: a
  small observable mode holder beside the model, shared through
  services so the chart, the table and the toolbar control read
  one switch. Not persisted.

## The semantics, decided

Ordinary means Accumulate off and no platform modifier; additive
means Accumulate on **or** the platform's add-to-selection
modifier, which always works — the visible control exists so the
operation is discoverable and accessible without remembering it.

| gesture | ordinary | additive |
|---|---|---|
| chart click, one object hit | replace the set with that object; it leads | toggle it: added → it leads; removed → the lead rule below |
| chart click, several candidates | replace with the current candidate; candidates offered as today; choosing another candidate retargets member and lead in one transition | add the current candidate (it leads); the chooser retargets what was just added |
| chart click, empty sky | replace with the empty set; the Inspector answers with the place as today | membership untouched; the Inspector still answers the place — a question, not an edit |
| chart click, off the paper | nothing, as today | nothing |
| search result chosen | recentre as today; replace with the found object; it leads | recentre; add it; it leads |
| table row click | replace with that row; it leads | toggle that row |
| table range (shift, pointer or keyboard) | one transition: replace with the range, lead = last reached | one transition: the range joins the set, lead = last reached |
| table toggle (platform modifier / Accumulate) | — | toggle rows in and out; off-page members are never dropped by a table gesture |
| remove one member (Inspector ✕, or additive toggle) | the rest stay; removing the lead passes the lead to the **last-marked remaining member** — `WorkingMarksModel`'s standing rule, restated and kept | same |
| choose a different lead | membership untouched | same |
| Clear selection | the whole set empties, explicitly, from the Inspector and the existing Clear marks control — one action, one name | same |
| page navigation, sorting, column reordering, options repaint, theme or palette change, resize, module detach | never mutate the set | same |
| restart | the set begins empty; nothing was ever persisted | same |

Order is the order of first membership; re-adding a member does
not move it. Duplicates are impossible by the model's constructor
rule. A lead change is never a silent removal.

## The chart's ink

For each member on the current page: if its catalogue symbol is
drawn, the chart's existing selection ring — the one treatment,
radius from the mark's own reach — is drawn around it, once per
drawn member; if it is on the page but not drawn under the current
options, family switches, limit or detail policy, the existing
restrained working cross, once. **Never both for one object**, the
Sprint 24 rule generalised: cross contributions exist exactly for
on-page undrawn members. Off-page members leave no ink and stay in
the set. The lead keeps the cross vocabulary's existing heavier
treatment where it wears a cross; a drawn lead wears the same ring
as any member — the Inspector names the lead, and inventing a
second ring style would grow the vocabulary this decision reuses.
Hit testing and catalogue drawing are untouched.

`decided-members.png` previews all of it through the unchanged
production painters: rings on M 31 and M 32, the lead cross on
NGC 206. The preview also shows the honest consequence of reusing
the ring as-is: a member ring around M 31 is as large as M 31.
That is the existing vocabulary doing its existing job — the ring
follows the mark it names — and this decision keeps it rather than
inventing a smaller second ring; the review weighs it here, before
any code.

Presentation follows visibility, membership does not: hiding a
family or lowering the limit moves a member between ring and cross
without touching the set. Selection-only transitions are
repaint-only and issue no catalogue query.

## The Inspector's surface

Mock-ups beside the study (`selection-set*.png`, both themes,
enlarged text, the narrow sidebar; `selection-accumulate.png` for
the visible control): a **Working set** section listing every
member across pages in order — the lead marked and bold, off-page
members labelled *off this page* in words rather than colour
alone, a per-member remove, and **Clear selection**. Choosing a
row makes it the lead; the section never drops a member because
the chart moved. The Accumulate toggle is a toolbar control beside
the selection's home so it serves chart and table gestures alike,
with an accessible name and description saying exactly what it
changes.

## Evidence the implementation owes (from the issue, as contracts)

Cross-page set built from both surfaces over at least three pages
with every surface agreeing; removal of lead and non-lead with the
replacement rule asserted; every route through real controls with
premises; one treatment per selected object, none for off-page;
presentation-versus-membership under family/limit changes; the
no-mutation list (sorting, repaint, theme, palette, resize,
detach); repaint-only selection transitions with a query counter;
reentrant listeners observing the state their transition
describes; packaged restart beginning empty beside intact reader
options; and the named mutation checks — accidental pruning,
replacement while accumulate is active, irremovable member,
duplicate identities, lead/member disagreement, and any write of
selection identity to any store.

## Out of scope

Saved lists, plans, notes, annotations, priority ordering,
export/import, synchronisation, reminders, restoring a set after
restart — all excluded, exactly as the issue states them.
