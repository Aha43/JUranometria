# Sprint 31 handover — Give every label a place

Six issues, six pull requests. This is what the sprint did, what it
got wrong on the way, what is still owed, and what version it should
be.

## Why the sprint existed

The owner looked at a finished 120° page and found two things on it:
a constellation name with star names written through it, and a small
star's name laid across an unrelated bright star's mark. Both
reproduced. Neither was a bug in one family.

The atlas had three passes that placed text, developed separately,
avoiding different things:

| family | what it avoided |
|---|---|
| star labels | the title block, the magnitude key, deep-sky label boxes, other star labels |
| deep-sky labels | nothing |
| constellation names | nothing |

Each was correct on its own terms. Together they had no policy, and
the named fixture showed what that costs: on the Sagittarius
overview, Nunki's name was drawn across 68 pixels of Namalsadirah's
disc — more ink than the mark had left visible — **and Namalsadirah's
own Bayer letter, which qualifies at that field, was not drawn at
all**, because the star pass had accepted "Nunki σ" first and refused
φ for meeting it. The atlas declined to name a star in order to
protect a name it then drew across that star's mark.

## What shipped

**A measured account of the page** (#310, the gate). 23 pages, both
grounds, the screen and both paper extents, a searched target and a
working selection. Every collision it reports names both pieces of
ink and is measured by painting the page with each of them withheld,
because a rectangle overlap is not a collision a reader can see and
ink at a place is not that thing's ink. It changed no production
behaviour and no released page, and the decision it settles is
`docs/decisions/label-placement.md`.

**A deterministic placement seam** (#313). One boundary decides where
a page's text may go and why a candidate was refused, and it never
measures a string: rendering decides what the text and figure
geometry *is*, the seam decides where supplied geometry may go. No
rasterised pixel reaches a placement decision, so the same page comes
out the same way on every machine.

**Production geometry, published rather than reconstructed** (#313).
Each deep-sky symbol's **ink** as distinct from the silhouette a
reader aims at; each constellation figure's drawn ink and the point
its name is anchored on; the graticule's notation with its boxes; the
reference layer's names; and what the title block *covers* as
distinct from what it is laid out in — a one-pixel discrepancy that
had been clipping two constellation names.

**One decision for every label** (#314). The three families share the
gate's rule: eight stated positions around what a label names, in a
fixed order, avoiding every drawn mark, every symbol, the furniture
and each other, with a constellation name kept inside its own
figure's region, and **nothing cut short by the paper**.

**The reader's journey** (#315). One window, one reader: Home, the
page the defect was reported on, the families through the real
options dialog, the modules, four control pages, both papers in all
three formats through the real export dialog, and Home again.

## What a reader gets

Measured over the study's 23 pages, before and after, by the same
withhold-and-diff census:

| | before | now |
|---|---:|---:|
| a star's name across an unrelated mark | 252 | **32** |
| a constellation name and a star label sharing pixels | 56 | **9** |
| a star's disc drawn over a constellation name | 228 | **62** |
| attributed collisions of every kind | 1827 | **1362** |
| pieces of text drawn | 1152 | **1267** |

The named fixture is repaired on the page it was reported on: Nunki's
name shares no pixel of ink with Namalsadirah's disc, and
Namalsadirah has its own letter back.

**The searched target keeps its exemption.** Its name may still be
written over anything on the page — the gate kept that deliberately,
and the closing journey walks the page where it shows.

## What changed on the released pages

`docs/studies/wider-field/released-text.txt` was recorded from
`a0c8900` before anything moved and regenerated after, so its diff is
the change, one line per label. Across the 40 released pages:

| | labels |
|---|---:|
| kept exactly the place they had | 90 |
| moved to another of their stated positions | 77 |
| newly drawn — recovered by the collision policy | 3 |
| omitted, having been **cut by the edge** | 3 |
| omitted, having been drawn **wholly outside the paper** | 26 |

No label that was fully on the page is lost. Of the 80 released-page
digest rows, the **marks** and **ink** digests are unchanged in every
one — nothing but text moved — and 46 pixel digests changed.

## The gate's budgets, and where they stand

| | budget | measured |
|---|---|---|
| placement work, wide page | ≤ a tenth of every-against-every | 3.0% at `sagittarius-120`, 3.3% at `orion-120` |
| placement work, detail page | the same | 5.5% at `orion-36`; **23% at `home`**, where five labels and fifty-six obstacles leave an index nothing to save |
| placement, 900×700 overview | ≤ 60 ms | 11–13 ms on the machine named in the study |
| placement, released detail page | ≤ 20 ms | 1–2 ms at `home`, 5–8 ms at `orion-36` |
| labels displaced by a one-step pan | ≤ 15% | **14% over the corpus**, over on eleven pages of twenty-three |

**The stability budget was amended, not met.** Production displaces
172 labels of 1,233 on a one-step pan — inside the budget over the
corpus, outside it on individual pages, from 6% to 28% where five
labels of eighteen move. What refuses the position each displaced
label held:

| | labels |
|---|---:|
| nothing — it took an *earlier* candidate the pan had freed | 65 |
| a star's mark or a symbol | 45 |
| another label, itself displaced | 22 |
| the paper's edge | 20 |
| the title block or the key | 20 |

Two geometry-only remedies were measured and rejected: ignoring
contact below a pixel of shared ink removes one displacement of the
twenty at `sagittarius-120`; trying the neighbouring positions before
the far side moves the corpus from 172 to 179. The remedy that would
work — a label preferring the position it last had — is refused
outright, because a page depending on how the reader arrived would
export differently for two readers of the same sky. The budget is
restated in the decision as **≤15% over the corpus and no page above
30%**, with that table as its grounds.

## Every review correction worth carrying forward

**An oracle that cannot distinguish two causes will credit the wrong
one.** #307's lesson, and it recurred all sprint. The gate's first
collision measure asked both participants on the finished page, where
ink drawn later hides ink drawn earlier and ink drawn over its own
colour changes nothing; a boundary appeared to cover nine pixels of a
label that covered five of it, which is impossible for ink
underneath. The measure is symmetric now: each participant measured
with the other absent.

**Withholding takes more than you asked for.** Removing a star takes
its name; removing a figure takes its name *and* the anchor stars
that exist because it is drawn. Both had to be subtracted back out
before the numbers meant anything.

**Reading the ink off the page is right for judging geometry and
wrong for feeding it.** One round let rasterised pixels reach
placement. The same collision shares 68 pixels on macOS and 27 on the
Linux runner, and a policy fed on that would place labels differently
on the two. Pixels judge shapes; shapes decide placement.

**A constant copied from a few lines away is still a guess.** The
study dotted an open cluster's ring 1-on-3-off — the *boundary*
stroke — and wrote 2.5-on-2.5 in a comment as though checked. The
fix was for production to publish the geometry, not for the study to
copy it more carefully.

**Publish what production decided; do not re-derive it.** Every place
the study rebuilt something — the symbols' drawn shapes, the figures'
subdivision — was a place it could drift. The seam takes the
renderer's own lists now, and #314 found the last one: the study
assembled its own obstacle set beside production's and differed by
two labels, which is how one measurement acquired two numbers.

**Requested text is not painted text, twice over.** The migration
cost was first counted against every label that *qualified*, then
against every label the seam was *asked for* — neither of which is
what the atlas draws. It is counted against the renderer's own
published placements now.

**A test that cannot fail proves nothing.** The PNG export oracle
asked whether a box held a dark pixel, which a disc, a grid line or
another label answers. It reads by differencing now, and every
format's reader is checked against a writer mutated to get the order
wrong.

**The study's own greedy pass drew every constellation name five
pixels below where the atlas draws it**, having applied the
rule for a label beside a mark to a name on its own anchor. Found by
comparing two implementations that were supposed to agree.

## Keyboard and tooltip coverage

**This section was written before #312 and #311 landed, and said the
sprint added no keyboard shortcut and that a tooltip and its
accessible description should be identical. Both are now wrong, and
what replaces them is below.** The correction is recorded rather than
quietly overwritten, because a handover that silently changed its
mind about a promise is a handover nobody can check.

**Sprint 31 added one keyboard route**, in #312: a prefix that opens
the chart's own keyboard, and then a letter for the switch.

| route | key |
|---|---|
| the chart's own keyboard | ⌘K / Ctrl-K, then one letter (#312) |
| Export Chart Sheet… | menu accelerator (⌘E / Ctrl-E) |
| Inspector | menu accelerator (⌘I / Ctrl-I) |
| Zoom in / out | menu accelerators (⌘= / ⌘− and their variants) |
| Chart Options… | menu item, mnemonic C |
| Place & Time… | menu item, mnemonic P |
| Ecliptic | checkbox menu item, mnemonic E, or the palette's `I` |
| every label family | its own checkbox mnemonic in Chart Options — Constellation names is Alt-N, star names Alt-S, Bayer letters Alt-Y, Flamsteed numbers Alt-F, deep-sky labels Alt-L — and its own palette letter |

Every keystroke above comes from one registry, so no surface can
promise a stroke the application does not answer
(`docs/decisions/chart-toggle-shortcuts.md`,
`docs/decisions/control-explanations.md`).

**Tooltip coverage, as #311 left it.** A tooltip and an accessible
description are now deliberately *not* identical, and the seam
refuses it when they are. They have different jobs: a tooltip arrives
beside a control the reader can see, and a description is heard by
somebody who can see neither, so it must stand on its own and must
not read the control's name back. The journey asserts the pair
differ, that the tooltip names the palette sequence for the same
switch, and that the description says which master a dependent switch
waits for — the greying that tells a sighted reader is not a
sentence.

**Excluded deliberately:** the mnemonic *activation* path is not
driven by the journey. A dispatched key cannot stand in for the
platform's own mnemonic handling, so what is asserted is that the
control carries the route (`getMnemonic`), and the control is driven
another way. The accelerators a dispatched key genuinely does reach —
the zoom pair — are pressed for real.

**And a harness limit worth recording.** The Place and Time dialog's
checkboxes answer neither a synthetic pointer nor a synthetic space
on the desktop this was written on: the control holds the focus, the
events arrive, and its model does not move. A reader's own press
arrives through the native queue, so this is the harness meeting the
platform rather than the atlas. The journey activates those controls
directly instead — through the dialog's own checkbox and its own
wiring, never the module behind it — which still fails if the dialog
is unwired. Since #312 the same two lines are also switched from the
chart's own keyboard, by a letter dispatched at the shown palette,
which crosses a second route to the same module without going near
its flags. Finding that out corrected the step: the observer's lines
are on by the module's default, so the journey's first version proved
nothing at all, and it now switches them off through the dialog and
back on again.

## Screen, sheet and export evidence

The extent is a real input: `orion-42` at 900×700, at the A4 chart
area and at the Letter chart area are three different placements of
one sky. Screen and paper share the same decision, asked at each
extent.

The sheet writers replay one recording of one production render, so
no placement question is answered twice. The closing journey exports
**both papers in all three formats through the shown export dialog**,
and each file is read by something that did not write it:

- **SVG** by its text elements: every label at the box the page
  placed it in, nothing the page refused drawn at any of its
  positions, the families in the order the chart draws them, the
  title block last.
- **PDF** by the glyph outlines in its content stream, anchored at
  each label's own corner and as wide as its own box.
- **PNG** by differencing renders — each label measured by writing
  the sheet again without it, its pixels inside the box the page
  placed it in and nowhere else.

Each reader is checked against a writer mutated to get the order
wrong: with `SheetReplay` grouping text last, all three PNG oracles
fail, each naming its own reason.

## The evidence contracts, and a gap they caught

`make evidence-contracts` reproduces every generated artifact from
its own generator and compares. It is not part of CI, which runs
`make test`, and at the end of #314 it reported **107 breaches**:
moving where text is drawn moved every renderer-drawn study page in
the repository, and nothing had regenerated them. Measured on merged
`main` to be certain the debt was the migration's and not the
journey's.

#315 cleared them: the Bayer, coordinate-grid, chart-furniture,
black-sky, regional detail and zoom, place-and-time, chart-sheet,
chart-options, point-and-identify, working-selection, overview,
deep-sky-occlusion and gallery studies regenerated, and the promoted
pages of the constellation and star-identity studies re-promoted from
their generators' own output. The contract now reports
**EVIDENCE CONTRACTS OK**.

**One of those breaches was older than this sprint.**
`OverviewStudyMain` wrote its report to a file and printed a notice,
while the contract reproduces a report by running its main and
reading what it printed - so that report had been unverifiable since
the sprint that promoted it, and said so every run. It prints the
report now, like every other study main.

**It runs in CI now** — its own job on a virtual display, added by
#315, because a gate nobody runs reports its findings to nobody. A
study image is evidence of what the atlas drew *when it was
promoted*, and regenerating a hundred of them at once is honest only
because the contract demanded it and because what moved is stated
here.

**And running it there changed what the contract may claim.** Its
first run on a machine that was not the one that wrote it reported
**189 breaches** where the same command reports none locally — every
one of them a font, a desktop's own spelling of a modifier, or
floating-point rounding, and none of them stale evidence. That work
grew large enough to review on its own and was split out of this
branch into its own change; it is merged, and the reasoning is in
`docs/decisions/test-evidence.md` under *Three kinds of evidence*.
What it settled, in one line: deterministic reports are held to their
bytes everywhere, platform records to reproducing within their own
machine, renderings to being drawn twice on the runner, and promoted
images to the account they carry of themselves in
`docs/studies/PROVENANCE.md`. Two follow-ups came out of it —
**#323** for the contract's memory and where the full check runs, and
**#324** for the renderings whose generators the contract never
runs.

## Generated artifacts and provenance

- `docs/studies/label-placement/measurements.md` — regenerated by
  `make label-study`, byte-reproducible, no wall-clock timings.
- `docs/studies/label-placement/census-before.tsv` and
  `text-before.tsv` — **frozen** records of the atlas as 1.11.0 drew
  it, extracted before the migration. Nothing regenerates them: the
  code that produced them is not checked out any more.
- `docs/studies/wider-field/released-text.txt` — every label on every
  released page, regenerated by `make released-text`; a fact about
  the platform in its header as well as about this repository,
  exactly as the pixel column of `released-pages.txt` is.
- `docs/studies/wider-field/released-pages.txt` — the pixel column
  was re-recorded once, at #314; the marks and ink columns did not
  move.
- The 52 historical study images are unchanged; their provenance
  matters more than cosmetic freshness.

## Remaining risks

**Physical paper is still #293's.** Nothing in this sprint was read
at arm's length on a printed sheet. Whether an 11-point label at 300
dpi is comfortable, whether a moved label reads as deliberate, and
whether the omitted ones are missed are questions a ruler answers.
#293 remains open and unmilestoned, and remains the authority for
every claim requiring paper.

**Pan stability is a stated miss, not a fixed one.** Eleven pages of
twenty-three move more than a sixth of their text on a nudge. The
decision records the amended budget and why the obvious remedy is
refused; if a reader finds it distracting in use, the answer is a new
gate, not a quiet tuning constant.

**The vector-clipping test lost its premise.** `SheetFormatsTest`
asserted that the committed A4 sheet has labels anchored outside the
chart rectangle, so that its clipping check could not pass
vacuously - two constellation labels used to sit there. #314 removed
them, and with them the premise. The check now holds the stronger
statement, that no label is anchored outside the paper at all, with
the clip still shown to be doing work by the paths that do cross the
boundary.

**A label may still be placed behind the title block.** The gate lets
the fallback spend furniture, so a label with no free candidate can
end up under an opaque block, invisible. That is what the old star
pass did with such labels too — it refused to draw them — so no
reader loses anything, but the ink is wasted and the study counts it.

**Small pages read badly as percentages.** `home` has five labels;
one of them moving is 20%. Every per-page ratio in the study should
be read with its denominator.

## Recommended version: 1.12.0 — a recommendation only

Minor, not patch: the atlas draws text in different places on 46 of
80 released page renders, and stops drawing 29 labels it used to draw
(26 of which were entirely off the paper). Nothing a reader stored is
invalidated, no option changes meaning, and no file format moves — so
not major.

`VERSION` is unchanged, nothing is tagged, and nothing is published.
