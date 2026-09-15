# How JUranometria speaks the reader's sky

Issue #347, the gate for Sprint 33. This records what the sprint has
settled, what it has measured, and what is still open. **No production
reader behaviour changes in this issue**; everything here is a
contract for #348–#352 to build against.

Status: **in progress.** The audit and its calibration are done and
measured. The placement study, the source verification and the
fallback model are outstanding, and are marked as such below rather
than left to be inferred from silence.

## The reader's promise

Two choices, independent:

- **Interface language** — the words the application speaks about
  itself: menus, dialogs, controls, explanatory prose.
- **Chart language** — the words on the sky: constellation names and
  any page subject derived from them.

A reader may set them differently, and the combination is not a
mistake to be corrected. English controls with a Norwegian sky is a
reader learning the Norwegian names; Norwegian controls with a Latin
sky is a reader who wants the canonical names on the chart and their
own language everywhere else. Both are supported on purpose.

Norwegian Bokmål is the **first localised chart language, not a
replacement identity**. The 88 IAU constellations keep stable,
language-independent identities and abbreviations.

## Four things, kept apart

| what | example | changes with language? |
|---|---|---|
| canonical identity | `And`, `CrA`, `Ser` | never |
| official Latin name | *Andromeda* | never |
| localised name | *Vannmannen* | with chart language |
| a different cultural sky | — | not a translation at all |

The last row is a boundary, not a feature. Translating the IAU sky
into Norwegian is not the same as showing a sky that divides the
heavens differently, and a second name column must never be allowed
to imply otherwise. A genuinely different cultural sky needs its own
issue, its own geometry and its own provenance.

## What the audit measured

A mechanical classifier (`juranometria.tool.SkyLanguageScan`) over
the whole tree, calibrated by `SkyLanguageScanTest` against synthetic
input. **9,020 string literals**, partitioned by surface:

| surface | literals |
|---|---|
| developer tools and generators | 4,809 |
| confined diagnostics | 2,729 |
| application | 1,197 |
| export | 199 |
| reader-reachable diagnostics | 86 |

**The reader's own surface: 446 occurrences, 388 unique translation
units** — 360/315 in the application and 86/73 in reader-reachable
diagnostics. Occurrences and units are reported separately because
`Cancel` appears six times and is one translation, not six.

Three findings changed the shape of the sprint:

- **The generators outnumber the reader's surface more than ten to
  one.** Reporting them together would have hidden the queue #350
  actually has to work through.
- **Exception text reaches readers.** `StartupFailure.describe` walks
  a failure's cause chain and quotes up to four `getMessage` strings
  verbatim into a dialog. Calling those developer-only because they
  are thrown deep in production would have been wrong.
- **The first honest-looking answer was wrong.** A rule set that knew
  only `setText` found 86 strings and looked complete. The
  application shows far more; the number was small because the rules
  were, and the same shape would have recurred at every later stage
  had the fixture not been built to defeat it.

### What the audit cannot see, stated rather than implied

The classifier reads statements. It does not do data-flow analysis
and cannot follow a word through a collection, a return value or a
multi-step alias — `AboutDialog`'s notice titles, read out of a
`String[][]` inside a for-each, are reported unresolved rather than
claimed. Every unresolved literal carries a **named reason** so a
reviewer is sent to the right place: `collection/alias flow not
traced` points at a loop, `declared name used, but never at a known
sink` points at a call list. A single bucket would be numerically
honest and practically useless.

**The gate's bar is not zero unresolved.** It is that every
unresolved *application-surface* literal is either individually
resolved or belongs to a named, reviewable limitation with a manual
inspection route.

## Ruling: what export says, and in which language

Settled by the owner, 2026-09-14, extending #331's principle that an
export reflects the page the reader saw.

**Writers author no language.** `SvgSheetWriter`, `PdfSheetWriter`
and `PngSheetWriter` own no prose; their strings are SVG and PDF
scaffolding, and every word they emit arrives from elsewhere — the
sheet's metadata, or text replayed from the production render. This
was verified, not assumed: the audit first reported the export
surface as owning zero reader strings, and that zero had to be shown
to mean *"emits text supplied by another surface"* rather than
*"blind spot"*. It meant both — see below.

**Sheet metadata owns localised document prose.** `SheetMetadata`
composes the sentence a sheet says about itself. Its ownership splits
three ways:

```
localised template chosen by INTERFACE language
  + numbers formatted under deterministic notation rules
  + subject and names supplied by CHART language
```

So *Centre*, *Field*, *Stars to* and *Ground* follow the interface
language; constellation names and the chart subject derived from them
follow the chart language; coordinates, magnitudes and field widths
follow neither. An English-interface, Norwegian-chart export
therefore carries English metadata prose around a Norwegian sky. That
is not an inconsistency — it is the independence the reader was
promised.

**`Locale.ROOT` keeps the numbers and stops choosing the prose.** It
remains responsible for reproducible numeric formatting. That
`String.format(Locale.ROOT, …)` happens to hold the sentence today is
an artefact of how the code was written, and is **not** evidence that
the sentence must stay English.

**Export reproduces the page's explicit language state, never the
ambient machine locale.** The audit found no `Locale.getDefault()`
anywhere in production — 85 locale uses, all pinned — and that
property must survive the sprint.

### What this costs the evidence, and what it does not

The historic rendering corpus does **not** become a four-way matrix.

- Existing generators explicitly select **English interface + Latin
  chart** and stay byte-identical.
- New, focused export evidence covers the four meaningful
  combinations: English+Latin, English+Norwegian, Norwegian+Latin,
  Norwegian+Norwegian.

Those four prove ownership and routing. The rest of the corpus is
unaffected, because it pins the default state and the default state
does not move.

## Ruling: reader-reachable diagnostics

The 73 unique strings a reader can meet in the startup-failure dialog
are **not** to be handled by translating arbitrary `getMessage()`
values. That would be incomplete by construction and would make
support harder, not easier. The architecture separates:

- a **stable reader explanation and suggested action** — localised;
- **technical cause and detail** — may remain diagnostic;
- **identifiers, paths and system messages** — preserved verbatim.

A startup failure may be framed in Norwegian while retaining the
exact technical evidence needed to diagnose it.

## Contract: names change values, never keys or order

`SceneGeography` carries its constellation names in a map **sorted by
constellation id**, deliberately: iteration order reaches the
renderer, so overlapping name text must stack identically on every
render (PR #69).

Because the key is the identity and not the name, this holds by
construction:

> **Changing chart language changes values, never keys or iteration
> order.**

#348 must keep it, and it is to be mutation-proved by sorting on the
localised display name — which must change stacking or output, and
fail.

## Data provenance — OUTSTANDING

An 88-row lead was supplied by the owner on #347. It is a **lead, not
provenance**, and is recorded as such.

Validated mechanically against the atlas's own geography: 88 rows, 88
canonical constellations, IAU abbreviations matching exactly — none
missing, none extra, no duplicates, no malformed rows, no duplicate
Norwegian names.

Still required before #348: verification of every name against a
citable source, with licence, version or retrieval date,
transformation notes and every editorial deviation recorded. The
lead's brightest-star and historical-attribution columns are
deliberately excluded.

### Two separate fixture events, not one

Both the lead and its manifest are pinned in the evidence contract's
`FIXTURES` as byte-exact working material. Pinning them does not
manufacture provenance - it records exactly which owner-supplied
bytes the study measured, and forces review when they change.

When verification happens, these are **two reviewed changes and must
not be collapsed into one**:

1. **Replacing the lead with sourced data** is a fixture provenance
   event in its own right. Re-pin it only after comparing all 88 rows
   against the source and recording every deviation - including
   whichever of `Sydkorset` / `Sørkorset` the source supports.
2. **Moving the manifest from `provisional` to `verified`** is a
   separate reviewed fixture change, and the generator refuses it
   without source, licence, retrieval date and transformation notes.

One refreshed digest must never implicitly approve the other. A
single commit re-pinning both would let new names arrive under a
status nobody checked, or a status change bless names nobody
compared.

**Known open discrepancy:** the lead gives Crux → `Sydkorset`, the
older Dano-Norwegian form; modern Bokmål usage also has `Sørkorset`.
Not to be normalised silently. The verified source settles it.

### Shape of the Norwegian column, for placement

- 18 of 88 names carry `æ`, `ø` or `å` — font coverage must be
  checked on every writer, not only on screen.
- 11 are multi-word with a lowercase second word (`Store hund`,
  `Berenikes hår`, `Sørlige krone`), which decides casing and
  collation rules.
- Longest is `Sørlige vannslange` at 18 characters against Latin
  `Hydrus` at 6 — three times the width, on a southern constellation
  that lands near the globe rim where there is least room.

## Two defects found on the way, not this sprint's to fix

Cross-checking the lead against the bundled geography surfaced two
pre-existing errors in the atlas's **Latin** data, in both of which
the record disagrees with itself:

- **`Ser`** — `latinName` is `Serpens Caput` but `genitive` is
  `Serpentis`, which is the genitive of *Serpens*. Confirmed in the
  bundled data: `constellations.csv` holds
  `Ser,Serpens Caput,Serpentis,3`, while `figures.csv` gives that one
  `Ser` identity **both disconnected regions**. So the model already
  has one correct stable identity and one combined figure; only the
  official Latin display name is wrongly narrowed to the name of half
  of it.

  **This must not become a localisation convention.** The gate rules:

  - canonical identity stays `Ser`;
  - the official Latin name should be **`Serpens`**;
  - the Norwegian lead stays `Slangen`;
  - *Caput* and *Cauda* name two regions of one constellation, not
    two IAU constellations;
  - correcting the Latin name will intentionally change existing
    labels, search results and pinned evidence.

  The correction belongs to **#348**, which owns the authoritative
  Latin/IAU foundation, preferably as its own commit enumerating the
  exact rendering and search consequences. It is not a reason to
  change production in #347.
- **`CrA`** — `latinName` is `Corona Austrina` with genitive
  `Coronae Austrini`. The IAU name is *Corona Australis*, genitive
  *Coronae Australis*; and *Austrini* is a masculine genitive against
  feminine *corona*, so that form is malformed whichever variant is
  preferred.

Measured consequence, not argued: searching `alpha Coronae Australis`
returns **0 results**, while the atlas's non-standard `alpha Coronae
Austrini` returns 1. A reader typing the standard genitive finds
nothing.

Recorded for the owner; outside this gate's remit to change. Both
corrections belong to #348.

### Serpens is also the study's explanatory fixture

The placement study's tightest attachment margin is `Ser` on
Sagittarius at 120°: **89.6 px from its own figure, 0.0 px from
Ophiuchus**, which lies physically *between* Serpens' two regions.
`Ser` appears three times among the twelve tightest margins.

This is required reading beside the attribution numbers, because it
shows why nearest-line attribution is **informative but not
absolute**. A Serpens label cannot avoid being nearer Ophiuchus' lines
than to half of its own figure, and no placement policy could change
that. The 25 negative margins are therefore published as
**inspection candidates** with their distances and competing
identity - never as acceptance failures.

## Scope finding: search does not accept constellation names at all

`"Serpens"`, `"Corona Australis"` and `"Serpens Caput"` each return
**0 results** today. Search accepts a Bayer letter with a
constellation *genitive or abbreviation*, never a bare constellation
name.

So "should localised names be accepted in search" is really "should
constellation names become searchable at all" — a **new capability**,
not a localisation of an existing one, and materially larger than the
gate's wording suggests. The gate must decide it explicitly.

## Working defaults for the language model

Owner-stated 2026-09-15, to be settled with exact keys and schema in
the gate's remaining work. Recorded here so they are decisions rather
than something a later implementer invents.

- **Language tags:** `en` and `nb-NO`.
- **Existing readers migrate** to interface English with chart
  language **Follow interface**. English maps to Latin chart names,
  so a 2.0 reader upgrading sees exactly the page they had.
- **New readers get the same default.**
- **Follow interface is live:** changing the interface language while
  chart language follows it changes the names on the chart. An
  explicit chart choice stays independent of the interface.
- **Missing interface text falls back to English.**
- **Missing localised constellation names fall back to the official
  Latin name.**
- **The IAU abbreviation and the canonical identity never fall back**,
  because they are canonical data and not translations. There is
  nothing for them to fall back *to*.
- **A further translation arrives as reviewed resource data plus its
  provenance** - no edit to constellation identity, no renderer
  change.
- **A different cultural sky is outside that contribution format**,
  as recorded above: it needs its own issue, geometry and provenance.

## Still open

- Placement study: Latin against Norwegian on crowded Sagittarius,
  sparse Orion, both poles, the RA seam, 42°, 120° and the 180° globe
  rim; labels retained, omitted and moved, collisions and candidate
  ranks, bounds, font coverage, exported text, runtime.
- The same-page-twice contract: with only chart language changed, all
  non-text geometry, mark membership, hit testing and inventory
  identities must be unchanged. Labels may move or be omitted; the
  sky may not.
- Source verification for all 88 names, and the `Sydkorset` question.
- Language tags, and whether the first locale is explicitly `nb-NO`.
- Defaults, persistence and fallback when a translation is absent.
- The data-only contribution shape for a further translation.
- Draining the unresolved application-surface residue to a
  deliberately inspectable size.
