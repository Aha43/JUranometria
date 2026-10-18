# All star identities where they fit

**Sprint 51, issue #557 - a study ahead of #558.** Whether, and how, a
reader may ask the chart for every known star identity on the page while
the restrained released policy stays the default. This document measures,
proposes and stops for the owner's rulings; nothing in production changes
here. The evidence is `docs/studies/star-identity-density/`
(`make star-identity-density-study`): the identities each page offers
(portable), what each candidate ordering places on this desktop
(`platform.md`), and the deliberately crowded comparisons.

Boundaries, from the issue: no NGC/IC/M density option and no individually
retained deep-sky labels - those belong to later bookmark and annotation
design - and the default page does not change.

## 1. What the atlas does today

Read from the code:

- **One label per star**, composed by `StarLabelPolicy.Forms`: the name
  with its Bayer letter, else the letter, else the Flamsteed number. The
  three Chart Options switches - Star names, Bayer letters, Flamsteed
  numbers - choose which forms may appear.
- **Thresholds by field and magnitude** decide which forms qualify: names
  to V 4.5 below 12°, 3.0 to 24°, 2.5 wider; letters to V 5.0, 4.5 and
  3.5, with Latin letters only below 12°; numbers only below 12°, to
  V 5.0, so that "a bare number near a deep-sky label" is not read as a
  Messier number.
- **Placement** is one decision for the whole page (`LabelPlacement`):
  the searched target first and guaranteed, then star labels brightest
  first, then deep-sky labels, then constellation names. A label whose
  every candidate is refused is placed at its least bad place, over
  whatever costs least - "under duress".
- **The searched target** already carries its full identity
  (`guaranteedLabelFor`), whatever the thresholds.

## 2. What was measured

Four regions, each found by searching the bundled catalogue for a named
star - Orion (Alnilam), Aries (Mesarthim), Cygnus (Sadr) and Eridanus
(Acamar) - at seven fields from 2° to 60°, the released default options,
limit V 8. Three orderings:

- **A - as stars.** The thresholds removed and nothing else: every
  identity competes as a star label, brightest first, ahead of deep-sky
  labels and constellation names; least-bad placement kept.
- **B - after the page.** The released page placed exactly as it is, then
  every extra identity, brightest first, in clean room only.
- **C - the page, fuller.** The released requests in their released order,
  each star's text upgraded to its full identity; then the extra
  identities as in B.

What the measurements say (`measurements.md`, `platform.md`):

- **The identities are few where a reader reads a page.** At 2° no page
  offers anything beyond the released policy, at 4° at most one, at 8°
  one to seven; the counts grow with the field to 144-277 offered at 60°.
- **A breaks the released page.** It places many labels under duress -
  up to 207 of 295 on the 60° Orion page - and on the 24° Orion page the
  labels around M 42 and the Trapezium are written over one another.
- **B keeps the released page but misses the issue's own case.**
  Mesarthim is not an *extra* identity at 2°-8°: the released policy
  already labels it, as `γ¹`, because at V 4.52 its name is past the
  threshold. B leaves every released label as it was, so it never shows
  the name.
- **C keeps everything and shows Mesarthim.** On all 28 pages C places
  every star label the released page places, every deep-sky label and
  every constellation name - a test holds it - writes *Mesarthim γ¹*, and
  adds 0-170 extra identities, refusing the rest rather than overlapping
  them. No extra identity is placed under duress.
- **At wide fields the extras are mostly bare numbers.** The 60° Orion
  page under C carries dozens of Flamsteed numbers - exactly the clutter
  the released policy's number rule exists to prevent.
- **Cost.** On this desktop C's extra pass costs little beside the page's
  own placement, and A's flood costs most at 60°. Time does not reproduce,
  so the study says it on standard error and commits no figure for it.

## 3. The proposals, for the rulings

1. **Wording, and its place.** One Chart Options switch in the Labels
   group, after Flamsteed numbers: *All star identities where they fit* /
   *Alle stjerneidentiteter der de får plass*. A **density** choice, not
   a fourth form: it lifts the thresholds for whichever of Star names,
   Bayer letters and Flamsteed numbers are on, so the three switches still
   say which forms may appear. With all three off it has nothing to widen
   and is shown unavailable, as the deep-sky families are while their
   master is off. Remembered with the other Chart Options; off by default,
   so the default page is the released page byte for byte. No chart-keyboard
   letter is proposed (A, Q, V, X and Z remain free if one is wanted).

2. **Priority (ordering C).** The released page first, in its released
   order, each star it labels written with its full identity; then every
   other identity on the page, brightest first, only where a candidate is
   clean - never under duress. Deep-sky labels, constellation names and the
   searched target keep exactly the places they have today. The
   composition is unchanged: name and letter, else the letter, else the
   number - a number is not added beside a letter.

3. **Never a promise of everything.** The switch says *where they fit*:
   refused identities are simply not drawn. The view report states the
   switch; it does not count refusals.

4. **Wide fields.** Offered at every field, because it is the reader's
   explicit choice and refusal keeps the page legible - *with a
   sub-ruling on numbers*: proposed, bare Flamsteed numbers join only
   below 12°, as in the released policy, while names and letters are
   unthresholded at every field. *Alternative:* the whole option only
   below a ruled field (24° would cover the constellation pages).

5. **Accessibility, report, export.** The switch has its explanation on
   hover and its accessible name in both languages. The view report's
   `options:` line gains `all-star-identities on|off`. An exported sheet
   is drawn from the same options and so carries what the screen shows;
   its description is unchanged. The spoken page description names no
   star labels today and does not start to.

6. **Searched and selected stars.** No further guarantee: the searched
   star already carries its full identity, and the Inspector names a
   selected star in full whether or not its label fits.

## 4. What #558 must prove

| claim | what must hold it |
|---|---|
| the default page is unchanged | the released pages byte for byte with the switch off |
| nothing released is displaced | with the switch on, every released star, deep-sky and constellation label is placed where it was or, for a fuller star text, at its released priority; never lost |
| no clutter by overlap | no extra identity is placed under duress |
| the switches still choose forms | each of the three off removes its form; all three off leaves the density unavailable |
| one state, remembered | Chart Options and the Controller show and change one persisted value |
| the issue's cases | Mesarthim named on the Aries pages; the Orion pages legible |
| both languages | the switch's words, hover and accessible name in English and Norwegian |

## 5. Not in this issue

No production change, no NGC/IC/M density, no pinned or retained labels,
no bookmark or annotation work; #558 implements only what is ruled here.

## The rulings (owner, 2026-10-18)

All six proposals of section 3 accepted as written:

1. **Wording and interaction - accepted.** One Chart Options switch after
   Flamsteed numbers, *All star identities where they fit* / *Alle
   stjerneidentiteter der de får plass*: a density choice that lifts the
   thresholds of whichever of Star names, Bayer letters and Flamsteed
   numbers are on; unavailable while all three are off; remembered with
   the Chart Options, off by default; no chart-keyboard letter.
2. **Priority - accepted: ordering C.** The released page first, in its
   released order, each star it labels written with its full identity;
   then every other identity, brightest first, only into clean room -
   never under duress. Deep-sky labels, constellation names and the
   searched target keep their places. Composition unchanged.
3. **No promise of everything - accepted.** Refused identities are not
   drawn; the view report states the switch, not refusals.
4. **Wide fields - accepted, with the proposed sub-ruling.** Offered at
   every field; names and letters unthresholded at every field; bare
   Flamsteed numbers join only below 12°, as in the released policy.
5. **Accessibility, report, export - accepted.** Hover explanation and
   accessible name in both languages; `all-star-identities on|off` in the
   view report's options; export follows the options; the spoken page
   description unchanged.
6. **Searched and selected stars - accepted.** No further guarantee.

#558 implements this design.
