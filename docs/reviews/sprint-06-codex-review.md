# Sprint 6 Codex review — Reveal the wider sky

Reviewed 2026-08-29 against PR #61 at `0935ed1`, including the complete
Sprint 6 changes merged through issues #54–#57.

## Findings

### P2 — Report catalogue query time as part of scene assembly

The handover's table labels `0.4/0.6 ms` as **scene assembly**, and the
review answer concludes that assembly is sub-millisecond. The reproducible
study does not measure `SceneAssembler.assemble` for that column:
`RegionalStudyMain` queries stars and DSOs before `t1`, then its `asm`
interval (`t1` to `t2`) only constructs `ChartViewport` and `ChartScene`.
The catalogue work appears separately as `qry` and is commonly 1–4 ms
warm at 36° (including 1–3 ms for the stated LMC case).

The performance conclusion is still comfortable, but the labels and
comparison are not measuring what they claim. Either measure the real
`SceneAssembler.assemble` end to end, or report the existing components
honestly as **catalogue query + scene construction + render** and base the
review answer on their sum. Record the measurement method/repetition count
beside the table so the Sprint 5 comparison remains interpretable.

### P2 — Qualify the README's searched-target visibility promise

The README says that “Messier objects and the searched target always
remain” beyond 18°. That is broader than the implemented and documented
contract. A searched DSO whose type deliberately maps to `Symbol.NONE`
recentres and titles but remains undrawn; a searched star can also be
culled when the user selects a limiting magnitude brighter than that star.

State the actual DSO rule: **Messier objects remain, and a searched target
with an established DSO symbol remains drawn and labelled**. This keeps the
end-user guide aligned with `RegionalDetailPolicy` and the corrected
regional-zoom decision.

## Review notes

- The complete named-target journey preserves centre, label, stable
  identity, and magnitude through 12°/18°/24°/36° and restores the exact
  M31 default on reset.
- The 18° classic / 24° regional boundary and target exception remain
  protected by policy and rendered-ink tests.
- The LMC page exercises the pack's largest extent and a 17-tile southern
  query; the RA-wrap page crosses 0° without a visible seam.
- The committed southern and wrap pages reproduce byte for byte with
  `make regional-study`.
- The remaining M42/M43 label collision is real but correctly identified
  as deferred general cartographic work rather than hidden by this sprint.
- The handover's “900–1,500 stars” shorthand should preferably become the
  measured **880–1,510** range (or “roughly 900–1,500”), since its own study
  table contains M13 at 880 and M42 at 1,510.

## Verification

- `git diff --check`: clean before this review document
- `make test`: **156/156 passed**
- `make regional-study`: completed for all seven targets and five fields
- `make chart-image`: byte-identical to the released M31 8° reference
- `lmc-36deg.png` and `rawrap-36deg.png`: byte-identical to their committed
  post-policy study pages

Sprint 6's implementation is sound. Resolve the two documentation and
measurement-honesty findings before merging PR #61 and cutting 0.6.0.
