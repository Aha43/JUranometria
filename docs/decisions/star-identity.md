# Decision: star-identity sources and cartographic contract

Decided 2026-08-30 for Sprint 13, "Name the stars" (issue #112), from
a measured join over the real pack and rendered candidate policies
(`make star-identity-study`; eight pages committed under
`docs/studies/star-identity/`).

## Source

**d3-celestial `starnames.json`** (Olaf Frohn, BSD-3-Clause), the same
repository and **the same pinned commit**
`7e720a3de062059d4c5400a379146a601d9010e0` as the constellation
geography — one licence chain, one download script, one provenance
story. SHA-256:

```
19c84bc885f8a97c3b8e1f6a380084c575a9758dedfe35256e911a823ec3a695  starnames.json
```

The download script fetches it atomically against this pin (temp
file, verify, move — a partial or corrupt local copy is detected and
re-downloaded; a mismatch after download is an error, never a kept
file), and the study independently refuses to measure from a file
that fails the pin, with a regression test on the refusal.

4,869 entries keyed by **Hipparcos number**, carrying per star: the
traditional proper name (602), Bayer designation (1,969), Flamsteed
number (2,651), constellation membership, and variable/HD/Gliese
identifiers. Upstream compilation sources (recorded for the notice):
the HD-DM-GC-HR-HIP-Bayer-Flamsteed Cross Index (Kostjuk 2002,
VizieR IV/27A), the FK5/common-name cross index (IV/22), GCVS, and
the IAU constellation chart data.

**Fact versus convention, for provenance and About wording:** Bayer
letters and Flamsteed numbers are historical designation systems,
not IAU standards; the proper names are the traditional names as
compiled by this source — largely coinciding with the IAU WGSN
approved set, but the pack must describe them as *traditional star
names* and never claim per-name IAU certification. Constellation
membership is carried as a fact from the source and cross-checkable
against our own boundaries.

Rejected alternatives: the IAU WGSN `IAU-CSN` list alone (authoritative
for approved names but no Bayer/Flamsteed and the familiar iau.org
no-explicit-data-licence problem from Sprint 7); VizieR cross-indexes
directly (no licence field); flattening everything into one display
string (destroys the designation systems as separate facts).

## The join: HIP through raw Tycho-2, never by display name

The pack's stars carry TYC identifiers; the raw Tycho-2 inputs
(already pinned) carry the HIP cross-reference — in the main files
**and in the supplement, which is where the brightest stars live**
(Betelgeuse, Rigel, and Sirius saturate Tycho-2's main catalogue; the
join without the supplement loses exactly the most famous names,
measured before being fixed). Measured over the real data:

- **4,805 / 4,869 identities join** to the V ≤ 8 pack (98.7%),
  including 539 of the 602 proper names.
- **64 unmatched**: predominantly exoplanet-host names on faint stars
  (Citadelle, Emiw, …) and famous faint stars (van Maanen's,
  Kapteyn's) genuinely below V 8 — an honest exclusion, not a defect;
  the report lists them.
- **201 multi-component systems** (e.g. Acrux → two packed
  components): the identity attaches to the **brightest packed
  component only**, never duplicated — one name, one star, and the
  exception category is counted in the report.
- Unknowns stay unknown: a star without an entry simply has no
  identity; no field is invented.
- Found while generating the pack (#113): **nine designation-less
  variable-star entries carry NSV catalogue-number fragments in the
  source's constellation field**. They fail the cross-check against
  our own 88 identities, so the pack carries their membership as
  unknown - counted in the manifest, never invented; a *designated*
  star with an unknown constellation remains a loud failure.

## Data model (for #113/#114)

A separate identity pack in the established pattern (manifest,
SHA-256, notices): `star-identities.csv` rows
`tyc,name,bayer,flamsteed,constellation` — designations as separate
columns, Greek letters and superscript components verbatim from the
source (Bayer's Latin letters after omega included), empty = unknown.
~4,800 rows ≈ 150 KB raw. Loading joins by TYC id against the
existing pack at load time; search entries extend `LocalSearch`.

## Search grammar (for #114)

Case-insensitive and forgiving, resolving to the existing star search
results (which already recenter on TYC identifiers):

- traditional names by prefix ("betel", "polaris");
- Bayer as Greek text or spelled-out letter plus constellation
  ("α ori", "alpha ori", "alpha orionis" — genitive and abbreviation
  both accepted);
- Flamsteed as number plus constellation ("58 ori");
- existing TYC identifiers unchanged.

A bare Greek letter or bare number without a constellation is
**ambiguous by design and never silently resolves** — it lists
matches like any prefix search, and the display line shows the full
identity ("Betelgeuse · α Ori · V 0.6") so the reader picks
knowingly. Duplicate proper names exist in the source (22, e.g.
across components); display lines disambiguate by designation.

## Scale and density policy (measured, rendered)

One deterministic label pass: **brightest star first, with a stable
TYC-identifier tie-break for equal magnitudes, collision-rejecting**
(a label whose box intersects an accepted star-label box, a deep-sky
label box, or the title block is omitted — prefer omission, the house
rule). The layer order is: stars, then star labels, then deep-sky
labels, then the title block — star labels **yield** to the deep-sky
labels and the title by seeding, and sit above geography. The study
renders through exactly this composition (the base page is produced
with deep-sky labels suppressed via the existing chart option, star
labels placed against the seeded collision set, deep-sky labels drawn
above). The seeded deep-sky label boxes and the title-block rectangle
are the renderer's own — `ChartRenderer.labelBounds` and
`ChartRenderer.titleBlockBounds`, shared with the study rather than
approximated, so collisions are measured against the exact geometry
the atlas draws (rotated-galaxy label anchors, baselines, and
metric-computed title bounds included). Every measurement below is
from that corrected path with **all pinned inputs SHA-256-verified
before measuring**. Identity
priority per star: **proper name, else Bayer, else Flamsteed**; Greek
letters render as themselves. A label may still touch a neighbouring
star's dot in tight pairs (labels avoid labels, not dots) — accepted
as the initial policy, with alternate-side placement recorded as the
known refinement if #115's review wants it.

| Field | Proper names | Bayer | Flamsteed |
|---:|---|---|---|
| 24–36° | V ≤ 2.0 | — | — |
| 12–18° | V ≤ 3.0 | V ≤ 3.0 | — |
| ≤ 8° | V ≤ 4.5 | V ≤ 4.5 | V ≤ 5.0 |

Measured on the committed pages: Orion at 36° carries exactly
Betelgeuse, Bellatrix, Rigel, Alnilam, Alnitak — the figure's anchor
stars named, nothing else; the Pleiades at 8° name their five
brightest sisters with one collision correctly rejected in the
densest field; Crux at 18° names its four bright stars (Acrux via its
brightest component); Polaris is named at every field. **The bad
alternative is committed too**: `orion-36-everything.png` (93 labels,
22 rejections, numerals everywhere) is the page the thresholds
exist to prevent.

## The M31 8° reference — an explicit owner decision

With the ≤ 8° thresholds above and the corrected composition, the
released M31 page gains exactly **one** small label: **35** (And,
V 4.51, placed first as the brighter of the close pair; ν And's label
is then correctly collision-rejected against it) — committed as
`m31-08.png` for review. The recommendation is to **accept this
deliberate reference change**: an 8° finder chart that names its
guide stars is a better finder chart, and the change is one quiet
numeral. The alternative
(restricting Bayer/Flamsteed to ≤ 6° so the reference stays
byte-identical) is recorded and rejected as optimizing for the test
anchor over the reader. **If the owner prefers the reference
unchanged, the ≤ 8° column becomes the ≤ 6° column and the 8° row
becomes names-only V ≤ 4.0** — a one-line policy change. The
reference image update, if accepted, happens in #115 with the usual
visual review.

## Searched-star guarantee

A searched star recenters and titles today; with identities it also
gains: **if the searched star has any identity, its best identity
labels it at every field, exempt from magnitude thresholds and
collision rejection** — the same shape as the DSO target exemption,
with no new symbol convention: the star keeps its ordinary magnitude
dot, only the label is guaranteed. A star with no identity behaves
exactly as today.

## Chart Options

One new option: **Star names and identifiers**, default **on**,
joining the Labels group (making it three controls; still no tabs).
It gates the whole label pass; the searched-star exemption survives
it exactly as the DSO target exemption survives the deep-sky toggle.
No dependencies: star labels attach to star dots, which are never
optional.

## Implementation seams (for #113–#115)

Generation extends the established pack pattern; loading extends
`Atlas`/`LocalSearch`; the scene carries the identities of its stars
(assembled data, option-free); the renderer gains one label pass
gated by the new option in the same composition seam as Sprint 12.
Costs measured in the study: the join is import-time only; the label
pass at the worst studied page places ~7 labels from a sorted pass
over already-assembled stars.

## Consequences

- #113 generates the identity pack exactly as above (with the
  measurement report reproduced by the generator); #114 loads and
  searches; #115 renders and, if the owner accepts, updates the M31
  reference deliberately; the study (`make star-identity-study`)
  stays the reproducibility path.
