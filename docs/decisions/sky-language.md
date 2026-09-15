# How JUranometria speaks the reader's sky

Issue #347, the gate for Sprint 33. This records what the sprint has
settled, what it has measured, and what is still open. **No production
reader behaviour changes in this issue**; everything here is a
contract for #348–#352 to build against.

Status: **gate complete; awaiting final independent review.** The
audit, the source verification, the placement study, the owner
checkpoint, the language model and the contribution contract are all
settled and recorded below. The component decisions were reviewed as
they were made; the closed record on its final tree has not been
reviewed, and this document does not claim otherwise.

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
input. **9,282 string literals**, partitioned by surface:

| surface | literals |
|---|---|
| developer tools and generators | 5,001 |
| confined diagnostics | 2,799 |
| application | 1,197 |
| export | 199 |
| reader-reachable diagnostics | 86 |

**The reader's own surface: 749 occurrences, 636 unique translation
units** — 638/540 in the application, 86/73 in reader-reachable
diagnostics, and 25/23 in export. Occurrences and units are reported
separately because `Cancel` appears six times and is one translation,
not six.

Each occurrence carries how firmly it is known: **544 traced** to a
named call, **184 recognised** as prose with no sink traced, and
**21 manually reviewed** through the ledger. The recognised ones are
*candidates*, and the label travels with them into #350.

Three findings changed the shape of the sprint:

- **The generators outnumber the reader's surface more than ten to
  one.** Reporting them together would have hidden the queue #350
  actually has to work through.
- **Exception text reaches readers.** `StartupFailure.describe` walks
  a failure's cause chain and quotes up to four `getMessage` strings
  verbatim into a dialog. Calling those developer-only because they
  are thrown deep in production would have been wrong.
- **The first honest-looking answer was wrong, twice over.** A rule
  set that knew only `setText` found 86 strings and looked complete.
  Naming the routes it missed took that to 446 occurrences over 388
  units — which also looked complete, and was not: draining the
  residue took it to **749 over 636**. Nearly three hundred
  reader-visible strings sat in a generic bucket while two successive
  confident numbers stood in front of them.

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

## Data provenance — SETTLED

The names are an **editorial JUranometria list**, not a copy of any
single publication. Owner ruling, 2026-09-15: nobody owns the
Norwegian names of the constellations. A publisher may restrict reuse
of its article — its prose, its presentation, its table as a compiled
work — without acquiring exclusive rights to the nomenclature itself.

**Two layers, established separately.**

| layer | source |
|---|---|
| canonical identities, Latin names, abbreviations, genitives | the IAU's official constellation list, retrieved from the ESO-hosted IAU archive and committed as `iau-constellations.tsv` |
| Norwegian names | pinned Norwegian Wikipedia revision, CC BY-SA |

A Norwegian source does not establish the identity table and the IAU
does not establish Norwegian names, so `norwegian-names.tsv` carries
**no Latin column at all** — two columns, IAU abbreviation and
Norwegian name. Repeating the Latin name there would invite the
layers to drift.

### The identity layer, compared rather than cited

Naming a source is not checking against it. All 88 rows were compared
against the atlas's own data by `IauIdentityTest` on 2026-09-15:

- **abbreviations: 88 of 88 identical**, none missing, none extra;
- **Latin nominatives and genitives: 86 of 88 agree exactly**;
- **two disagree**, and comparing every row is what establishes that
  there are only two.

| id | atlas holds | official |
|---|---|---|
| `Ser` | Serpens Caput | **Serpens** |
| `CrA` | Corona Austrina / Coronae Austrini | **Corona Australis** / **Coronae Australis** |

`CrA` is wrong twice — the name and a genitive that is malformed
Latin either way. Both corrections belong to **#348**; #347's job was
to establish their authoritative expected values, which
`IauIdentityTest` now records so #348 need not re-derive them. That
test also asserts the two defects are *still defects*, so a stale
exception cannot outlive the problem it was written for.

**How the list was built**, recorded in full in `names.manifest`:

1. Starting dataset: *Liste over stjernebilder*, Norwegian Wikipedia,
   **permanent revision 25182596** of 2025-06-10, CC BY-SA 4.0,
   retrieved 2026-09-15. Attribution and share-alike apply.
2. Restructured onto the canonical IAU identities.
3. **Fifteen disputed forms reviewed individually**, each adopting
   the astronomy-authored form, with *Store norske leksikon* cited as
   evidence for that choice. The other 73 are the starting dataset's,
   unchanged.

No SNL prose, table presentation, historical column or brightest-star
material is reproduced. SNL is cited as evidence for individual
editorial judgements, which is what citing a source is for.

### The fifteen choices

Five are different words, not spellings: `Boo` Bjørnevokteren →
**Oksedriveren**, `Cae` Meiselen → **Gravstikken**, `Lac` Firfislen →
**Øglen**, `Men` Bordet → **Taffelberget**, `Ind` Indianeren →
**Inderen** (a different referent entirely: Indian of India, not
Native American).

Two are variants: `Cet` Hvalfisken → **Hvalen**, `Per` Persevs →
**Perseus**.

Seven are a consistent house style on the definite article — `CMi`,
`CrA`, `CrB`, `Hyi`, `LMi`, `PsA`, `TrA` all drop *Den/Det*. This is
not merely taste: the shorter forms are narrower, and label width
decides what fits on a crowded page and at the globe rim.

### Sydkorset, settled on evidence

`Cru` Sørkorset → **Sydkorset**. Store norske leksikon's dedicated
astronomy article (updated 2025-09-17) carries **Sydkorset as its
headword** and records *Sørkorset* as an alternative. A current,
astronomy-authored encyclopaedia keeping the older form as its
primary headword for this constellation is evidence of an established
contemporary astronomical term — not an obsolete spelling in need of
modernising. Norwegian Wikipedia titles its article *Sørkorset*;
Nynorsk uses *Sørkrossen*. Chosen on that evidence, not by preference.

### Corroboration, not circularity

The finished 88 names are **identical to the owner's original working
lead**, which was assembled separately and has now been retired. That
is corroboration: the list is reachable by any contributor from a
pinned public revision plus fifteen documented choices, with no
private material involved — which is exactly what the gate had to
prove.

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
that. The 23 negative margins this machine measures are therefore
published as **inspection candidates** with their distances and
competing identity — never as acceptance failures, and never as a
count another machine must reproduce.

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

## The language settings, and what they persist

Owner ruling, 2026-09-15. Stated as meanings; the Java types may
differ, the meanings may not.

```
language.interface = en | nb-NO | …whatever is installed
language.chart     = follow-interface | latin | …whatever pack is found
```

**Availability is data, not a switch.** Only `follow-interface` and
`latin` are fixed, because they are behaviours the atlas owns rather
than things a contributor supplies — no pack may claim either name.
Every actual language comes from what is installed: interface
resource bundles, and packs discovered and validated by the
contribution contract. A Swedish pack is selectable, drawable and
persistable without editing Java, and `SkyLanguageChoiceTest` proves
exactly that with a tag appearing in no constant, enum or switch.

The same registry governs reading a stored value, creating a choice
and persisting one. An unavailable value cannot be *built*, so it
cannot be stored — validating on the way in but not on the way out
would leave a door open beside a locked one.

**Follow interface is an explicit third value, not an absence.** It is
something a reader chose, and a store that could not tell "never
chose" from "chose to follow" would have no way to change its default
later without silently moving somebody's chart. `EclipticStore`
already carries the `Optional` pattern this needs.

### Migration

- A missing `language.interface` reads as `en`.
- A missing `language.chart` reads as `follow-interface`.
- **Reading legacy preferences writes nothing.** A 2.0 reader who
  never opens the setting keeps a store with no language keys in it.
- Once the reader changes or saves either setting, the complete
  explicit choice is persisted — both keys, no implied halves.
- Absence and explicit `follow-interface` behave **identically
  today**. That is what makes the upgrade invisible: the persisted
  chart mode is `follow-interface`, which *resolves* to Latin names
  under an English interface — exactly the page 2.0 drew. The mode
  and its output are different things, and conflating them is how a
  later change would move somebody's chart by accident.
- **A future default change needs an explicit migration or version
  rule.** It may not reinterpret old absence, because absence today
  means "never asked" and reinterpreting it would change the chart of
  a reader who never agreed to anything.

### Malformed values

An unknown or malformed stored value falls back deterministically and
stays diagnosable. It is never passed to `Locale.getDefault()` and
never accepted as an arbitrary language tag — the audit found no
`Locale.getDefault()` in production at all, and that property has to
survive this sprint.

### `latin` is a nomenclature mode

`latin` selects the official Latin names on the chart. It is **not**
a claim that the interface speaks Latin, and it must not be disguised
as an interface locale `la`. The chart language and the interface
language answer different questions, and one of them has an answer
the other cannot express.

### Fallback

| missing | falls back to |
|---|---|
| interface text | English |
| localised constellation name | the official Latin name |
| IAU abbreviation, canonical identity | **never falls back** |

The last row is not an omission. An abbreviation is canonical data,
not a translation; there is nothing for it to fall back *to*.

## The contribution contract

A further translation arrives as **reviewed data plus its
provenance** — no edit to constellation identity, no renderer change,
no entry in a hard-coded list of supported languages.

A contribution carries: a schema version, a language tag, a display
name, all 88 canonical abbreviations exactly once, and the same
provenance fields production data requires — source, licence,
retrieval date, transformations.

It is rejected for a missing identity, a duplicate, an extra, or an
unknown one. Eighty-seven names is not a language pack, and an
eighty-ninth constellation does not exist.

### The fixture language

`x-juranometria-test` — an unmistakable private-use tag, living
permanently under **test resources**, excluded from packaged
resources and from every language selector.

It exercises the whole contract: all 88 abbreviations, synthetic
names visibly unlike Latin, non-ASCII coverage, and every required
field. Its manifest **declares itself a fixture** rather than
fabricating external provenance — inventing a source to satisfy a
schema would be the exact dishonesty the provenance rules exist to
prevent. Production loading rejects that fixture status, and a
packaged-asset test proves the fixture never ships.

**The mutation that matters is architectural**: adding the fixture
data must make the naming service resolve it *without* editing
constellation identity, `SceneAssembler`, `LabelGeometry`, renderer
code or a supported-language enum. Removing one row, or adding an
invented 89th identity, must fail at the data boundary. #348 owns
the service; this gate owns the contract it must satisfy.

## What is portable, and what is this machine's

Stated before any figure is quoted, because the distinction was
learned the hard way: a run on the Linux CI runner (`ubuntu-24.04`)
measured **9 omissions and 98 moves** where this machine measures 10
and 102, with neither machine wrong. Naming the runner matters: CI is
an execution route, not a typography, and a different runner image or
a font update would be another observation again.
Placement depends on how wide a word is drawn, and fonts and
rasterisers differ.

**Portable — held across machines:**

- the method: one canonical scene placed twice, neither answer
  derived from the other;
- the fixtures: fourteen pages, each at its rung's own density;
- canonical identities and the IAU comparison;
- the names and their provenance;
- that every attachment observation must be measurable;
- the contribution and language-choice contracts.

**This machine's — a platform observation, not a fact about the
atlas:** every count and distance below. Totals of moved, omitted,
renamed and identical; identities affected; attachment margins; the
closest approach to a page edge; and the seven-versus-three split of
omissions between the languages. `placement.md` carries the standard
`Platform observation` banner and is registered as a platform record
rather than a deterministic report.

All figures quoted here were recorded on
`Mac OS X 26.5.2/aarch64/Homebrew 21.0.11`.

## Owner checkpoint — passed 2026-09-15 (macOS rendering)

Eight matched Latin/Norwegian pages and an index of every asymmetric
omission (`checkpoint.md`). All six questions pass, with **no
production policy change requested**.

The sheets the owner inspected were rendered on macOS. The visual
conclusions hold **for those sheets**; they are not cross-platform
assertions, and another machine's typography would place some names
differently without any of these judgements becoming wrong.

- **The Norwegian sky reads naturally** — not as decoration laid over
  a Latin atlas. *Skytten*, *Slangebæreren*, *Vannmannen*,
  *Steinbukken*, *Svanen*, *Store bjørn* and *Lille bjørn* read as
  names of the actual figures.
- **Moved names remain attached.** No Norwegian name appears to
  belong to a neighbouring figure, including `SLANGEN` above
  `SLANGEBÆREREN` on Sagittarius 120° — despite that being the case
  the nearest-line metric scores worst.
- **The omissions leave honest space**, and none makes an
  unexplained hole.
- **The hierarchy stays quiet.** Longer words do not become louder;
  constellation names keep their subdued place beneath stars, object
  labels and figure lines.
- **The globe stays balanced** at V 5.0: both languages read as the
  same restrained hemisphere, and Norwegian does not rebuild the
  crowded edge the limb treatment exists to avoid.
- **The seam and wide flat pages hold.**

### `Sørlige vannslange`, answered in two halves

Neither half answers it alone, and the question as first written
asked for something no page shows. On the **south-pole globe** the
eighteen characters are written in full, comfortably inside the disc,
at the same weight as *Smelteovnen* and *Paradisfuglen* beside them.
On the **Sagittarius globe**, where that whole name cannot stay
inside the limb, it is **omitted rather than compressed, clipped or
pushed into another constellation**. That is the right cartographic
behaviour.

### Every omission is at a globe's limb

On this machine there are ten, seven in Norwegian and three in
Latin; one run on the Linux CI runner measured nine. The count is
typography's; **the pattern is the finding** — every one falls at a
limb, in both languages. The limb refuses
whichever *whole word* cannot fit rather than favouring either
language — the placement policy applying evenly under a language it
was never designed for, not a translation tax.

## Handoff to #348

What this gate settled, #348 implements.

**Correct the canonical Latin records**, each to the value
`IauIdentityTest` already holds:

- `Ser` → **`Serpens`** (the genitive `Serpentis` is already right);
- `CrA` → **`Corona Australis`** with genitive **`Coronae
  Australis`**.

Both change existing labels, search results and pinned evidence on
purpose. The exception list in `IauIdentityTest` must shrink as each
lands — a stale allowance may not outlive the defect it was written
for.

**Build the naming service** from discovered, validated packs. Adding
a pack must be enough: no edit to constellation identity, to
`SceneAssembler`, to `LabelGeometry`, to renderer code, or to any
list of supported languages. `SkyLanguagePack` holds the data
contract; the service is #348's.

**Preserve identity-keyed ordering.** `SceneGeography` sorts by
constellation id, and chart language changes values, never keys or
iteration order. Mutation-proved by sorting on the displayed name.

**Implement the two preference keys** against the settled migration
contract: absence means "never asked", reading writes nothing, saving
writes both keys, and `follow-interface` is a value distinct from
absence.

**Ship English interface resources and the verified `nb-NO` chart
pack**, keeping the deterministic fallbacks: interface text to
English, localised names to official Latin, and identity never.

**Keep `x-juranometria-test` outside production packaging** and
outside every language selector.
