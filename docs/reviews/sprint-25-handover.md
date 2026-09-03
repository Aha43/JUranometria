# Sprint 25 handover — A place, a time, and the reader's own sky

**Issues #225, #226, #227, #228, #229.** Written for the sprint
review. Nothing here is merged, tagged or released on the strength
of it.

## What Sprint 25 delivered

The atlas has always drawn one fixed sky — J2000, north up, east
left — for every reader alike. It could not answer the first
question an observer standing outside asks of a chart: *where is all
this, for me, tonight?* Sprint 25 answers it without unfixing
anything: the chart still draws the one sky, and a removable module
lays the reader's own geometry over it — their meridian, their
zenith, their mathematical horizon — for one frozen instant at one
stated place.

| # | what it was | PR |
|---|---|---|
| 225 | the design gate: the astronomy measured, the shortcut rejected, the ink and the surface decided | [#231](https://github.com/Aha43/JUranometria/pull/231) |
| 226 | the observer, sidereal-time and sky-orientation model, UI-independent | [#234](https://github.com/Aha43/JUranometria/pull/234) |
| 227 | the removable meridian module and the chart's reference-line ink | [#236](https://github.com/Aha43/JUranometria/pull/236) |
| 228 | the Place and Time dialog | [#237](https://github.com/Aha43/JUranometria/pull/237) |
| 229 | this journey, this document, and the packaged evidence | — |

A reader now opens **View → Place and Time…**, types a latitude, an
east-positive longitude and one UTC instant, and switches on any of
three reference lines. The chart draws them in the grey it already
uses for constellation figures, above the grid and beneath every
star: the meridian solid, the mathematical horizon dashed, the
zenith a small open ring with an upward tick, each carrying its own
name where it leaves the paper. Nothing ticks, nothing moves unless
the reader presses the one button whose name says it will, and on
most pages none of the three crosses the paper — the atlas draws
nothing rather than promise a line that is not there.

## The science, in reader language

- **Local sidereal time** is the right ascension standing on the
  reader's meridian: Greenwich apparent sidereal time plus east
  longitude, wrapped into a day. Longitude enters there and nowhere
  else, which is why the sign convention has exactly one place to be
  got wrong and one place to be tested.
- **The frame problem.** The chart is J2000; a reader's zenith and
  meridian live in the sky *of their date*. The gate measured the
  tempting shortcut — plot sidereal time straight onto the J2000
  page — at **21.26′ today, 39.55′ by 2050**, and rejected it. The
  model carries IAU 1976 precession and the twenty largest terms of
  IAU 1980 nutation instead, and the combined rotation agrees with
  IAU SOFA (release 2023-10-11, eighty provenance-carrying reference
  vectors) to **0.0101″**.
- **UTC, not UT1.** The atlas ships no earth-rotation tables and
  makes no network call, so it reads the instant as UTC. The price
  is bounded and stated: |UT1−UTC| ≤ 13.54″ of rotation, plus a
  0.50″ polar-motion *allowance* and 0.32″ of diurnal aberration —
  **14.36″ in all**, about 1/250 of the narrowest field's finest
  visible detail.
- **Mathematical, not apparent.** The horizon drawn is where the sky
  meets a perfectly flat, transparent Earth. A real horizon has
  hills and air in it; a line quietly claiming to be that would be a
  promise the atlas cannot keep, so the longer name is worn on the
  page and in the switch.
- **A frozen snapshot.** There is no clock anywhere in the model or
  the module: an `Observer` refuses a null instant because "a
  default would be a clock", and **Now** reads the system clock once,
  at the press.
- **Straight lines, exactly.** A gnomonic projection maps every
  great circle to a straight line, so reference lines are clipped
  analytically from a pole — measured at 0.0000 px deviation across
  every field the atlas offers — with no sampling, no thresholds,
  and no invented chord across refused sky. (Sprint 24's deep-sky
  ellipses are not great circles; they still need their measured
  subdivision. The two are different problems and got different
  answers.)

## The seam composed

This sprint is the module seam's second customer, which is the test
the Sprint 24 design was waiting for. The chart core learned
nothing: no observer, no clock, no longitude, no sidereal time — an
architecture test scans the compiled classes of `juranometria.sky`,
`.project`, `.module` and `.meridian` for toolkit, preferences, file
and network dependencies, with positive controls so it cannot pass
vacuously. The seam gained exactly three things, each recorded in
the gate's amendment section: the great-circle contribution (a pole
and a role), the `Reference` LINE/BOUNDARY kind (so the chart can
draw a boundary dashed without learning what a horizon is), and
paint-only `redraw()` (held to "nothing more than paint, and not
less" against the production host, by identity and by a recording
`RepaintManager`).

## Corrections from review

Every round found something real. This is the honest record.

| where | what was wrong |
|---|---|
| gate r1 | the nutation-omission budget measured the wrong tail of the series; an "any-date bound" was not a bound and was withdrawn |
| gate r2 | the decision quoted 0.0009″ where the study measured 0.0033″ — a hand-typed figure; the drift test now covers every stated number |
| gate r3 | polar motion was called a bound; it is an allowance, and is now labelled one |
| gate r4 | the accuracy contract said 14.64″ unsupported; split into bounds, allowance and measurement it is 14.36″ |
| gate r5 | the SOFA comparison was described as unobtainable offline; the vectors were then generated from the official release with provenance, and the prose replaced |
| gate r6 | invariant checks (pole on its circle, right rate, right size) were shown insufficient — a coherent sign error passes all of them; the SOFA fixture closed the gap |
| #226 | `nutationDegrees(double, int)` exposed the rejected truncation control; `clip` took sampled points, leaving callers the sampling the gate rejected; `Rectangle2D` sat on the AWT-free boundary |
| #226 | two refusal branches were unkillable by mutation; a fresh normaliser reintroduced the RA 360.0 edge the gate had already fixed once |
| #226 | mean-vs-apparent sidereal time was caught by nothing until a test was written to tell them apart |
| #227 | the horizon was shortened to "Horizon" on the page against the gate's explicit ruling; the packaged pixel count could not see a wrongly drawn horizon; the fake service could not see a production `redraw()` that rebuilt; a `redraw()` replaced by a no-op passed everything until the repaint request itself was observed |
| #227 | a test's interaction point sat under M31's ellipse, so dropping the painter's role filter changed nothing visible — vacuous by accident of location |
| #228 | impossible dates were silently normalised: February 30th became the 28th, 24:00 the following morning; both formatters are now STRICT and both accepted shapes are held to it separately |
| #228 | every commit in the first suite took the Enter path on a detached panel; the menu route, the close box, reopening, and real focus traversal were all untested — and the packed-dialog photographs the review demanded immediately exposed a real layout bug (`setSize` leaves the tree valid, so the width floor never reached the controls) |
| #228 | a redundant second closing mechanism made the close box unbreakable by mutation; removed |
| #229 | the journey's first red run: "home" via bare recentre is not home — the released default carries its target's identity in the title block, 1,168 pixels of difference |

## Evidence

- **735 tests, 0 failed, 0 aborted** on a display; the headless CI
  run aborts only the tests that honestly need a desktop, and the
  display job requires found == started == successful.
- **The journey** (`SprintTwentyFiveJourneyTest`) walks the
  production path: menu → dialog → fields → switches, the three
  geometries identified from the page itself (ink along the seam's
  predicted arc, pushed back through the chart's own inverse),
  longitude/latitude/time each changing only what astronomy says,
  the RA seam, a polar and a southern page with every changed pixel
  accounted to a geometry or its label, one navigation in the whole
  journey, byte-identical recovery on disable *and* on detach, and a
  restart returning the place, not the instant, not the switches.
- **Packaged acceptance** runs inside every native image on the
  bundled runtime alone: the model computes, the module contributes,
  the painter inks (meridian and zenith counted separately, the
  horizon proved silent *alone*, where a combined count would have
  hidden it), the place survives a restart through the bundled
  preference backend while no key exists for an instant to hide
  under, and a quiet or detached module leaves the released page
  byte for byte.
- **Studies** regenerate byte-for-byte (`make place-and-time-study`);
  the measurements document carries every number the decision
  quotes, enforced by test. The dialog is photographed as the packed
  production window at 12 pt, at 18 pt enlarged text, and in the
  dark theme; the reference ink was drawn over real pages in both
  themes during the gate.
- **Mutation testing** ran through every implementation issue:
  twenty-odd deliberate mutations across the sprint (truncated
  series, flipped signs, dropped guards, relaxed formatters, deleted
  listeners, no-op redraw, never-reopening singleton), each killed
  by a named test, with the survivors that exposed vacuous tests
  recorded above.
- **Performance.** Changing place, time or a switch repaints and
  does nothing else — no catalogue query, no scene reassembly, no
  inventory rebuild — held by identity against the production host.
  The reference layer costs one analytic clip per circle per paint.

## Residual risks

- **Scientific.** The 14.36″ UTC/UT1-and-allowances envelope is
  real and stated; it is invisible at every field the atlas draws.
  The polar-motion term is an allowance, not a computation. Nothing
  models refraction, and the horizon says so by its name.
- **UI.** Focus-out commitment depends on the desktop granting
  focus; the lifecycle test aborts honestly where it will not.
  The dialog trusts typed decimal degrees; it refuses the
  impossible but does not chaperone the merely unusual (a latitude
  of 89.999 is a reader's right).
- **Open issues.** #220 (the display-run click flake) now carries
  three committed diagnostics; the evidence so far exonerates page
  movement, options and the look-and-feel, and points at the star
  not being among the drawn marks at click time. #224 (test
  look-and-feel consolidation) remains open, non-urgent. Sprint 22's
  Milky Way data remains parked on an unanswered licensing question,
  and none of its raw bytes are distributed.

## Version recommendation

**1.6.0.** New reader-facing capability, new public seams
(`juranometria.sky`, the module contributions), no breaking change
to any existing surface, catalogue, or stored preference. Nothing
here alters the licensing position: the new code ships no new data
and phones nowhere.

Per `CLAUDE.md`: no milestone close, no version change, no tag and
no release on this document — those wait for the sprint review's
approval.
