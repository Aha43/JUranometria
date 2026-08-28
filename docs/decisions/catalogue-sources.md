# Decision: catalogue sources and the reproducible import contract

Decided 2026-08-29 for Sprint 3, "Local catalogue foundation"
(issue #23). This document settles the sources, licensing, magnitude
semantics, regional coverage, and import contract that issues #24–#26
implement. Every factual claim below was verified against the linked
primary source on the decision date.

## Decision summary

| Question | Decision |
|---|---|
| Star source | Tycho-2 (CDS I/259), main catalogue + supplement-1 |
| DSO source | OpenNGC, release v20260501 |
| Star brightness | Johnson V derived from Tycho BT/VT; UI keeps "V" |
| Coverage | 10° radius cone around the M31 centre |
| Star import limit | V ≤ 10.0 |
| Sky tiling / binary format | Deferred, with reasons recorded below |
| Runtime network access | None; imports run offline at development time |

## Star source: Tycho-2

**What:** The Tycho-2 Catalogue of the 2.5 Million Brightest Stars,
Høg E. et al., Astron. Astrophys. 355, L27 (2000). CDS catalogue
**I/259**.

**Download (pinned):** `https://cdsarc.cds.unistra.fr/ftp/I/259/` —
`tyc2.dat.00.gz` … `tyc2.dat.19.gz` (main catalogue, 2,539,913
records), `suppl_1.dat.gz` (17,588 Hipparcos/Tycho-1 stars absent from
the main file, which is where several bright stars live), and `ReadMe`
(the authoritative fixed-width record layout). The I/259 archive has
been static since 2000-02-08; the import records SHA-256 checksums of
the exact files used, since CDS publishes none.

**Why Tycho-2:**

- *Complete at the atlas's own promise.* The UI says "Stars to
  V 8.0"; showing that label over an incomplete star field is exactly
  the dishonesty this project forbids. Tycho-2 is 99% complete to
  V ≈ 11.0 (ReadMe, Catalogue Characteristics), with headroom for the
  fainter limits the detail-by-scale table in
  `docs/chart-conventions.md` will eventually want at 1–3° fields.
- *Alternatives fail one requirement each.* Hipparcos-based compilations
  (HYG and similar) are only complete to V ≈ 7.3–9.0 depending on sky
  position — marginal or incomplete at our own V 8.0 limit. The Yale
  Bright Star Catalogue stops near V 6.5. Gaia DR3 carries G-band
  magnitudes (not V) and is weakest exactly at the bright end that
  anchors a finder chart; it remains the intended **all-sky exit
  path** (see Deferred decisions).
- *ICRS J2000 positions* at the catalogue epoch match the chart model
  directly; proper motions exist if a future issue wants epoch
  propagation (out of scope now).

**License and redistribution:** The CDS landing page for I/259
(`https://cdsarc.cds.unistra.fr/viz-bin/cat/I/259`) states license
**CC-BY-NC-3.0 IGO** (the Tycho data are an ESA mission product).
Conclusion: redistribution of a derived regional extract inside this
repository and its releases is permitted **with attribution, for
non-commercial purposes**. JUranometria is a personal, non-commercial
project, and the repository currently carries no top-level license, so
no conflict exists today. Two consequences are accepted and recorded:

1. The generated star resource ships with its own NOTICE file naming
   the license and attribution (the per-directory notice pattern
   already used for the Tabler icons), and the packaging test guards
   it.
2. If the project ever adopts a permissive license or a commercial
   use appears, the star data must be re-imported from a source with
   compatible terms — the Gaia exit path below. The importer contract
   is written so only the source adapter would change.

**Attribution (required):** "This work has made use of data from the
Tycho-2 Catalogue (Høg et al. 2000, A&A 355, L27)" plus the standard
CDS acknowledgment ("This research has made use of the VizieR
catalogue access tool, CDS, Strasbourg, France — DOI:
10.26093/cds/vizier"). Both go in the generated NOTICE.

## Star brightness: derived Johnson V, labeled V

Tycho-2 carries two-colour Tycho photometry (BT, VT), not Johnson V.
The I/259 ReadMe itself defines the standard transformation
(Note 7, citing ESA SP-1200, Vol. 1, Sect. 1.3):

```text
V = VT − 0.090 · (BT − VT)
```

Decision: the import computes Johnson V by this published
transformation and the atlas keeps saying **"Stars to V 8.0"** — that
is scientifically honest because the quantity *is* Johnson V, derived
by the source's own documented recipe, at fixture-photometry accuracy
(VT s.e. 0.013 mag for VT < 9). Normalization rules for the edges:

- BT missing → use VT unchanged as the V estimate; count recorded in
  provenance.
- VT missing → drop the record; count recorded (rare; these are
  photometric outliers below our limit in practice).
- Supplement-1 records use the same rule on their BT/VT columns.

The rejected alternative — importing Gaia G and relabeling the UI
"Stars to G 8.0" — was declined because G ≠ V by up to several tenths
for red stars, every chart convention and the existing fixture speak
V, and the bright-end coverage is worse.

## DSO source: OpenNGC

**What:** OpenNGC (`https://github.com/mattiaverga/OpenNGC`), Mattia
Verga — "a license friendly NGC/IC objects database", built from NED,
SIMBAD, and HyperLEDA. DOI 10.21938/y.1ejWUD_MQ6b_eDFoVbbw.

**Download (pinned):** release tag **v20260501** (published
2026-05-01) — `database_files/NGC.csv` (13,969 objects) and
`database_files/addendum.csv`, via
`https://raw.githubusercontent.com/mattiaverga/OpenNGC/v20260501/…`.
SHA-256 of both files recorded at import.

**License:** **CC-BY-SA-4.0** (stated in the repository README and
`LICENSES/`). Redistribution of a derived regional extract is
permitted with attribution and share-alike on the data; the generated
DSO resource ships with its own NOTICE carrying the OpenNGC credit and
the upstream acknowledgments its README requests (NED, SIMBAD,
HyperLEDA).

**Fields verified** (`NGC_guide.txt` at the pinned tag): semicolon-
separated; `Name`, `Type`, sexagesimal `RA`/`Dec` (J2000), `MajAx`/
`MinAx` (arcmin), `PosAng` (degrees, north-eastwards — matching the
chart model's convention), `B-Mag`, `V-Mag`, `M` (Messier cross
reference), `Common names`, `Identifiers`.

**Type mapping:** OpenNGC `G` → `DsoType.GALAXY`. All other types in
the region (open/globular clusters, nebulae, `GPair`, duplicates,
nonexistent entries, …) are **counted and skipped** by the import
until their symbols exist in the renderer; the counts appear in
provenance so nothing disappears silently. `Dup` and `NonEx` records
are always excluded.

**Known visual consequence:** OpenNGC gives M31 MajAx 177.83′ × 69.66′
(PA 35°) where the retiring SIMBAD fixture said 199.53′ × 70.79′. The
drawn M31 ellipse will shrink slightly when #25 switches the loader;
this is expected and correct, not a regression.

## Regional coverage and import limit

**Region:** a cone of **radius 10°** centred on the M31 chart centre
(10.684708°, +41.268750° ICRS). Rationale: the widest 8° view's
corners reach 5.06° from centre at the current 900×700 aspect, so 10°
covers every existing 1°–8° view with ≈2° of recentring headroom for
the future mutable centre — enough to *test* recentring against real
data without pretending to all-sky coverage. A cone (not an RA/Dec
box) reuses the existing `SkyRegion` geometry, keeps the boundary
honest under the RA wrap, and mirrors how a future tile query would
compose.

**Star limit: V ≤ 10.0.** Two whole-magnitude steps below the current
UI bound, inside Tycho-2's 99%-complete range, giving the magnitude
sequence room to grow at narrow fields without a re-import. Estimated
volume at this galactic latitude (b ≈ −21°): roughly 4–8 thousand
stars, a few hundred kilobytes of CSV — comfortably reviewable and
committable, far from needing an index (measured numbers land in the
#24 provenance).

**DSO limit:** none; every mappable OpenNGC object in the region is
imported (the NGC/IC density here is low).

## The import contract (consumed by #24)

The importer is a plain-Java development tool in this repository (no
new dependencies, no Maven/Gradle), run offline by a developer via a
Make target. The application never runs it and never touches the
network.

**Inputs, pinned:** the URLs above; the tool verifies each downloaded
file against SHA-256 checksums committed beside the importer after
the first audited download. A checksum mismatch fails the import.

**Normalization rules:**

- Tycho-2 fixed-width parsing per the I/259 ReadMe byte layout; prefer
  the mean position (`RAmdeg`/`DEmdeg`); fall back to the observed
  Tycho position when the mean is absent (`pflag`); record both counts.
- Star id: canonical `TYC 1-13-1` hyphen form from TYC1/TYC2/TYC3.
- OpenNGC sexagesimal coordinates → decimal degrees.
- DSO id: `NGC 224` / `IC 10` normalized spacing; aliases from the
  `M` column (`M 31`), `Common names`, and NGC/IC cross references.
- Position angle normalized to [0, 180); absent PA → 0.0 with the
  count recorded (round or unmeasured objects).
- Absent MinAx → MinAx := MajAx; absent V-Mag → B-Mag with the count
  recorded; both counts in provenance.
- Label priority 1 for Messier objects, 2 otherwise.

**Deterministic output:** CSV in the existing fixture column shapes,
coordinates at 6 decimals, magnitudes and arcminutes at 2, `Locale.ROOT`
formatting, LF endings, stars ordered by (vmag, id) and DSOs by id.
Re-running the import on the same pinned inputs must produce
byte-identical files; #24 adds a test asserting it.

**Generated provenance:** each output directory carries a generated
`PROVENANCE.md` recording source names/versions/URLs, SHA-256 of the
raw inputs, retrieval date, the region and limits, every
normalization count above, row counts, and the importer's own
identity — plus the NOTICE files described under licensing.

## Deferred: sky tiling and binary encoding

Both stay deferred, per `docs/architecture.md`'s rule that formats are
chosen on evidence. The regional extract is a few hundred kilobytes
and thousands of rows; parsing it at startup is the same cost as the
current fixture and needs no index. The evidence that should drive a
tiling/encoding choice — load time, memory, and query cost at
all-sky volume (2.5M stars) — does not exist until an all-sky import
is attempted, and choosing now would encode guesses. The contract
above deliberately produces per-region output directories so a tiled
future can reuse the same generator per tile.

## Consequences for the existing application

- No UI change is required by this decision: the magnitude notation
  stays honest V, and the limiting-magnitude pipeline already culls
  by scene limit end-to-end.
- The hand-curated SIMBAD fixture retires when #25 switches the
  `Catalogue` boundary to the generated resources (expect the M31
  ellipse and small star-position/magnitude diffs; the reference image
  will change and must be inspected then).
- `MAGNITUDE_LIMIT_STEPS` may grow toward V 10.0 only when a UI issue
  chooses to expose it; the data will already be there.
