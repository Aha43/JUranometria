# Codex review: Sprint 27 working-selection gate

Reviewed PR #266 at `ec41919` against issue #258. The evidence establishes the
problem rather than merely naming it: today's two models can present two leads
in one frame. Promoting the reviewed ordered-set semantics to session scope,
keeping the answering model distinct, and removing page pruning is the right
architectural direction. The mock-ups keep off-page membership explicit, and
the decided chart image makes the cost of reusing the existing M31-sized ring
visible before implementation. That ink remains legible and is requested,
temporary interaction ink rather than catalogue furniture.

## [P1] Define additive ambiguity when either candidate is already a member

The gesture table says an additive chart click with several candidates “add[s]
the current candidate” and that the chooser “retargets what was just added.”
That is defined only when the initially offered candidate was absent. At the
M31/M32 overlap, either or both candidates may already belong to the working
set:

- if the current candidate is already a member, an additive one-object click
  would toggle it out, while the several-candidate row says to add it;
- if the chooser moves from an existing member to an absent one, there is no
  newly added member to retarget;
- if it moves between two existing members, replacing one identity with the
  other can silently remove membership or collapse a duplicate; and
- moving the chooser back must have a stated result rather than replaying a
  sequence of set edits for one unresolved click.

Decide the transaction represented by an ambiguous click: what membership is
committed initially, what changing the candidate replaces (if anything), and
what happens for absent/present combinations in ordinary and additive modes.
The chooser should operate against a captured pre-click set so cycling through
candidates cannot accumulate side effects. Add these combinations to the
gesture table and to #260/#261's owed transitions and journey evidence.

## [P1] Define how an additive table range contracts

The table says an additive range “joins the set,” but a keyboard or pointer
Shift range is not only a one-way growth operation. A reader can press
Shift-Down twice and then Shift-Up, or drag back toward the anchor. Swing's
visible range contracts; the current rule can either leave the departed row in
the working set (making the table immediately select it again), remove it even
if it was a member before this range gesture, or rebuild from the table and
drop off-page members. All three conflict with some part of the decision, and
the implementation issues are otherwise required to share one rule.

State range selection as a transaction with a captured anchor and baseline:
which rows a growing range adds, which additions a contracting range retracts,
how members that predated the gesture are protected, how off-page members are
retained, and which row leads after growth and contraction. Cover both
directions with real keyboard and pointer gestures, including a pre-existing
member inside the range and an off-page member outside it. “One transition”
must apply to each delivered range change without making the visible table and
the session set disagree.
