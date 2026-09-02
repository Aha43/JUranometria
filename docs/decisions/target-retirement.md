# Decision: hiding the family a searched target belongs to

**Sprint 23, issue #196.**

## The complaint

Search **M 33**, open Chart Options, switch **Galaxies** off. Every
galaxy disappears except M 33, which stays drawn and labelled. Select
something else through point-and-identify and it *still* stays,
because selection and searched-target identity are separate things.

Nothing on the surface explains why one galaxy survives a switch
labelled "Galaxies", or why choosing another object does not remove
its privilege.

Internally the atlas was consistent: a chart titled for a
symbol-capable target draws that target — the target-honesty
invariant, and a good rule. It was still a bug, because a control
must do what its label promises.

## The rule

**The explicit hide wins.**

Hiding the family a target belongs to is as explicit a request as
searching for it was, and it is the later one. So the target retires
where it stands:

- the target **label and identity clear together**, atomically;
- the chart falls back to its **honest coordinate title**;
- the former target is hidden **with the rest of its family**;
- **centre, field width, limiting magnitude and every unrelated
  option are untouched** — the reader keeps the place they reached.

This is the transition panning already makes, for the same reason and
through the same atomic rule. Leaving the page is what retirement
shares with panning away; losing your place is not.

## What was asked, and decided

**Does the master switch follow the same rule?** Yes, and by
construction rather than by a second branch. The rule asks
`ChartOptions.effectiveFamily`, which already folds in the
**Deep-sky objects** master switch, so switching everything off
retires a target on exactly the code path that switching one family
off does. A reader who hides all deep-sky objects and is left one
galaxy has been told nothing useful.

**Does selection clear?** No. Selection is UI-independent state that
a future module will read, and coupling identity to presentation for
convenience is what this issue is complaining about in the other
direction. The selection survives, and the **Inspector says plainly
that what is selected is no longer on the page** — the panel already
had those words for an object that had left the scene, and now uses
them for one the page no longer draws. It asks production's own
`ChartRenderer.permitted`, so the panel and the drawing cannot come
to disagree about what is on the paper.

**Does Cancel restore the target?** **No — it restores the options,
and only the options.** Retiring a target is a navigation transition;
the options controller owns no navigation, holds no scene and makes
no queries, and that separation is worth more than the convenience of
undoing one transition from the wrong side. After Cancel the families
are back, the former target is drawn again as the ordinary galaxy it
is, and the chart keeps its honest coordinate title. Asking for M 33
again is a search — which is what asking for it always was.

**Does Restore Defaults resurrect a retired target?** No. It is an
ordinary forward transition, not an undo. Every family returns,
including the object that used to be the target, drawn as an ordinary
member of its family. Showing galaxies again is not the same request
as asking for *this* galaxy, and the atlas does not guess.

**What about searching while a family is hidden?** It establishes the
target, and the target draws. The reader has explicitly asked to find
that object, which is exactly the kind of explicit request the whole
rule turns on. The symmetry is what makes the behaviour explicable:
an explicit find, then an explicit hide, each winning in turn — and
switching the family off again retires it again.

**Is repaint-only family filtering preserved?** Yes, for every
ordinary case. Hiding a family the target does not belong to moves
nothing, retires nothing and assembles no page — asserted by
`assertSame` on the scene. Only the conflict case becomes a
navigation transition, and it is openly one: the page is assembled
once more, because the page's own target has changed.

## Where the rule lives

`TargetRetirement.retires(scene, options)` is the decision alone —
no Swing, no navigation — and `TargetRetirement.connect(...)` is the
one wiring the application and every journey install. A rule the
application wires by hand and a test wires by hand again is two
rules that can drift, and the second one always passes.

The predicate reads production for every part of its answer:
`ChartRenderer.symbolForType` for the symbol an object draws, and
`ChartOptions.effectiveFamily` for whether that symbol is permitted.
An object the atlas draws no symbol for was never kept by the
exemption and has nothing to lose.

## What Sprint 21 had to give up

`DeepSkyFamilyJourneyTest` asserted the old rule directly — that a
family leaves the page "but for a target the page names", and that
the master switch "leaves only the named target". Those assertions
now encode the new rule, including the one honest cost: when the
hidden family holds the target, the transition is **not**
repaint-only, and the journey says so rather than quietly relaxing
its query count.
