# Sprint 29 handover — Take the chart to the observing table

Five issues, five pull requests, twenty rounds of independent
review. This is what the sprint did, what it got wrong on the way,
what is still owed, and what version it should be.

## Why the sprint existed

A club member printed his own star charts for an evening at
Fanafjellet, because the atlas could not give him one:

> Jeg laget egne stjernekart for print til Fanafjellet sist. Da kodet
> eg noe i python og det var mye knot, dette med en eksport til SVG
> ville vært veldig nyttig.

*("I made my own star charts for printing for Fanafjellet last time.
I coded something in Python and it was a lot of hassle; an export to
SVG would have been very useful.")*

He had solved the problem himself before asking us for anything. That
work identified the need, named the format, and told us the friction
was real rather than hypothetical. He asked for one other thing — to
go **a little** past 36°, not dramatically, accepting more projection
error for a practical overview. "A little" is a quantity, so the gate
measured it.

Both of those are in the atlas now.

## What shipped

**A 42-degree field step** (#284). One entry at the top of the
existing gnomonic sequence, chosen on a stated budget: corner
anisotropy no worse than 12%, half again the 8.1% the atlas already
accepted at 36°. That admits 40° and 42° and excludes 45° and 48°.
The projection did not change — gnomonic maps a great circle to a
straight line exactly, which is what lets three modules' ink be
clipped to the paper analytically instead of sampled.

**A chart sheet** (#285). `juranometria.sheet` is the one place a
chart stops being a thing on a screen: it plays the production
render into a recording `Graphics2D` and hands what the renderer did
to a writer. It reads no pixels, so it is not a screenshot, and it
holds no astronomy, so it is not a second description of a chart.
Two things differ from the screen and only two — the extent, because
a chart rectangle is 271.6 mm and not a window's shape, and the
ground, because printing a black sky asks a reader to lay down a
sheet of toner.

**One export surface** (#286). File → Export Chart Sheet…, with SVG
to edit, PDF to print and PNG to send, all written from one
recording of one render. A4 and US Letter, both offered, neither
guessed from a locale.

Reader documentation is in [docs/exporting.md](../exporting.md).

## Every review correction

Twenty rounds. The production designs were called sound early in
each issue; almost everything below is a correction to **evidence** —
tests that looked like proof and were not.

### #283, the gate (PR #288, five rounds)

1. The of-date comparison sent *mean* directions through a
   *true*-of-date transform; the pixel scale was flat rather than
   projected. Both fixed against SOFA's own `iauEcm06`.
2. Ecliptic landmarks reused the zenith's symbol and meaning.
3. The gate rejected the existing line ink without choosing its
   replacement.
4. "Its own module control" named no reader-facing location.
5. Seasonal names were Northern-Hemisphere specific.
6. Candidate ink was painted above the catalogue instead of in
   production's reference layer.
7. The persisted toggle had no fresh-install default.
8. Menu dimensions came from a mock-up rather than the real popup.
9. The PNG re-rendered the chart into a bigger pixel grid, shrinking
   every label fourfold against the paper while looking plausible.
10. SVG and PDF discarded production's clips, bleeding ink into the
    margin.
11. The Letter SVG reused the A4 recording.
12. Quadratic curves were converted to PDF cubics incorrectly.
13. Legibility was claimed before anything had been printed.
14. **Jonas's Python work was described as done "badly" and as "a
    worse atlas".** That violated the project's documentation voice
    and misrepresented work we are indebted to. Rewritten to record
    what it actually did for us.
15. Documentation still quoted the discarded PNG dimensions.
16. PNG tests did not prove physical ink scaling.
17. Clip tests counted syntax rather than proving clear margins.
18. The Letter test accepted reuse of the A4 recording.
19. The PNG test dropped its `pHYs` check while being rewritten.
20. PDF clipping was unguarded, and SVG accepted any narrower clip.
21. SVG `<text>` operations were absent from the clipping oracle,
    while two committed labels are anchored outside the chart.

### #284, the field step (PR #289, three rounds)

22. Mark derivation and clicking happened in separate event-thread
    turns — the stale-scene race of #220.
23. The journey wrote to `WorkingSelection` directly, masking the
    chart-to-selection wiring.
24. The pixel oracle's platform key was too coarse to justify a byte
    comparison across environments.
25. The decision recorded 17 reachability premises where the scanner
    found 18.
26. The landmark predicate accepted a box or an unclosed four-point
    path.
27. The italic test compared zero against zero and was vacuous.

### #285, the sheet (PR #290, three rounds)

28. The editable SVG lost the title block's bold weight.
29. A null reference layer became "no modules", masking miswiring.
30. The landmark test could mistake the ecliptic's own line for its
    open diamond.

### #286, the export surface (PR #291, five rounds)

31. A failed replacement **deleted the reader's existing file**.
32. Overwrite confirmation was absent, especially where the format's
    extension is appended after the chooser has approved a name.
33. SVG dropped the recorded miter limit and dash phase.
34. The read-only-directory fallback wrote the destination directly,
    so a partial failure could still corrupt it.
35. The confirmation wiring could be bypassed without any test
    failing.
36. Restoring the old bytes could itself fail while the message still
    claimed the file was unchanged.
37. A test created and leaked a real preferences node, through
    wrapped syntax the evidence scanner could not see.
38. The final destination could still receive partial output, through
    the direct write or a non-atomic move.

### #287, the sprint close (PR #292, final review)

39. The working-selection export omitted the **rings** around marked
    objects the page draws — it carried only the crosses for the ones
    it does not — and painted the reader's marks inside the reference
    layer, which put them underneath the stars they were marking.
    The screen paints them over the finished chart, and the sheet now
    does the same, in the same order.
40. The closing journey stitched the menu item, the dialog and the
    export together rather than driving them as one route. The
    session's surfaces are named now, and the journey presses the
    real item, which runs the real route, which asks through the real
    dialog with its Export button pressed.
41. The ecliptic is off Orion's page, so its export could have
    disappeared without failing anything, and the cross-format
    comparison was too thin. The journey now goes on to a page the
    ecliptic crosses and holds all three formats against production's
    own marks through the sky.
42. **The physical print and ruler inspection has not happened.**
    See below; it is the one thing in this sprint that cannot be
    done from here.
43. The real export route ran with an empty working selection and the
    inclusion switch unticked, so none of that path was exercised;
    and the new ink tests proved the rings without proving the
    crosses. The journey now marks two objects on the chart and ticks
    the switch in the real dialog, and the crosses have a test of
    their own.
44. The "all three formats" comparison inspected the SVG and the PNG
    and not the PDF — which is the one a club member prints. Its
    content stream is now read for the same marks, in the chart's own
    coordinates.
45. The journey still wrote to `WorkingSelection` directly rather
    than selecting through the chart, and its end-to-end export
    carried only drawn members, so the crosses were proved by a
    hand-built fixture alone. The reader now clicks both objects on
    the chart and then asks for fewer stars, which leaves one of them
    on the page and no longer drawn — the case that gets a cross.
46. The PDF oracle accepted any nearby coordinate as a mark: with
    every star disc removed it still reported 70 of 743 present. Both
    vector oracles now match a mark by its own shape — centred on the
    object and the size the renderer drew it — and the same mutation
    now reports 742 of 743 absent.

### What CI found that review did not

- The released-page oracle was not portable, and I answered that with
  reasoning **twice** before running the recorder under a Linux JDK in
  a container and diffing it. Two shapes of 4412 differed: the title
  block's panel, which is sized to its own text.
- The export dialog was 725 px wide at enlarged text with Linux
  fonts, against the 640 px narrowest window — the format
  explanations were inside the combo box, and a combo is as wide as
  its widest entry.
- A test written with `assumeTrue(isHeadless())` aborted on the
  display job, which counts an abort as a failure. It proved nothing
  precisely where it mattered.

### The pattern worth keeping

Three of my own corrections were the same mistake in different
clothes: a check that could not fail for the reason it claimed. A
predicate satisfied by the line it was meant to distinguish from; one
satisfied by a box; one comparing zero against zero. The habit that
caught them is running the mutation before believing the test, and it
caught the clip regression in #285 that no reviewer had asked about.

The other habit worth keeping is measuring instead of arguing. Both
times I reasoned about a platform difference I was wrong, and both
times a container answered it in minutes.

## What was inspected, and what was not

**Done here:**

- The sheets are parsed by the JDK's own XML reader, which has never
  heard of this program, and by an image decoder and a PDF
  content-stream read.
- An SVG was retitled by a standards-based transform — parse, change
  the title, write it back — and re-parsed as a valid chart with its
  ink untouched. This is issue #287's step 7 in its "standards-based
  transform" form.
- The packaged native image writes all three formats and reads them
  back, on every platform in the matrix.

**Owed, and not done:**

- **A printed sheet measured with a ruler.** Nothing in this sprint
  has been on paper. The final review named this as a P1, and it is
  correct to: it is the only evidence that can settle legibility, and
  no amount of arithmetic substitutes for it. **This is the one item
  in Sprint 29 that cannot be done from here at all** - it needs a
  printer, a sheet of A4 and a ruler. The expected measurements are in
  [docs/studies/chart-sheet/measurements.md](../studies/chart-sheet/measurements.md)
  under *What to measure on paper*, taken from the sheet's own ink:
  the frame at 271.6 × 184.6 mm, the thinnest line at 0.353 mm, the
  faintest star's disc at 1.83 mm, the smallest label at about
  2.47 mm of capital height. Print `sheet-a4.pdf` at actual size.
  The gate said this observation "can accept or revise these
  numbers", and it still can.
- **Opening a sheet in a browser and in a vector editor.** Not
  possible from this environment: the browser here cannot reach a
  local file or the loopback address, and no vector editor is
  available. `docs/studies/chart-sheet/` holds four SVGs, two PDFs
  and a PNG for exactly this.

Neither is a formality. The first is the only thing that can settle
legibility, and the second is the only thing that can settle whether
the file a club member opens looks like a chart.

## Remaining risks

- **Legibility on paper is unproven.** Every number is arithmetic.
  A 1 pt line is 0.353 mm whether or not a printer resolves it.
- **The 42° corner is 11.2% anisotropic** — a round cluster becomes
  an ellipse aimed at the page centre. That is inside the stated
  budget and has not been looked at on paper.
- **Label positions are this machine's.** The renderer places labels
  with font metrics, so another machine's sans-serif moves them and
  may fit one where this one did not. Sheets reproduce byte for byte
  per machine, which is the classification the renderer studies
  already carry.
- **PDF labels cannot be edited or searched.** A decision, not a
  defect; the SVG is the answer, and the reader documentation says
  so.
- **Exporting into a read-only folder is refused.** A capability that
  existed before this sprint's last round and was removed, because
  what it did was accept an interruptible write nobody had asked
  about.
- **The export surface has no test that opens its two modal
  dialogs.** `open()` is guarded by reading its source. That is a
  weaker instrument than driving it, and it is the place a future
  edit could reintroduce a silent overwrite.

## Recommended version: 1.10.0

A **minor** release.

- Reader-visible additions: a wider field step, and an export surface
  with three formats. Nothing a reader could do before has been
  taken away, with one exception, stated above, that removed a way to
  lose a file.
- No change to the catalogue, its format, its provenance or its
  licences.
- No change to preferences, their keys or their upgrade path.
- Every page at 36° and below renders exactly as it did in 1.9.0 —
  eighty of them, hashed, held by the evidence contracts.
- The offline promise is untouched: nothing added fetches anything,
  and the sheets carry no network reference of their own.

**Not to be done without the owner's instruction:** changing
`VERSION`, tagging, or releasing. This document recommends; it does
not act.
