# JUranometria 6.4.0 — keep the Controller focused and the figures honest: handover to release

*Issue #700, closing road #698 (Sprint 58). Written on the integrated
head of `feature/698-dogfooding` (`256ec5f0` plus this handover), before
any `VERSION`, tag or publication decision; 6.4.0 is this document's
working name for the next release, the owner's to confirm. Since 6.3.0
(`856d1a23`, published 2026-11-05) nothing has landed: the two
owner-checkpointed dogfooding findings #699 and #571 stand on one
integrated branch awaiting a single qualification. No catalogue, service,
planet or pack changes. The road's promise: **the controls stay
manageable, and the chart says exactly what its stellar limit means.***

## 1. What a reader gets in 6.4.0

As the unreleased changelog states them:

- **Keep one Controller section open** (#699). A slim row at the foot of
  the Controller offers *Keep one section open* / *Hold én seksjon åpen*.
  While it is on, opening a section collapses the others in its group -
  the Chart tab's sections and the Solar System tab's body groups, each
  on its own. Off until chosen; remembered; turning it off reopens
  nothing.
- **Complete figures under an absolute stellar limit** (#571). No star
  fainter than the page's limit is drawn, named or selectable, at any
  field, apart from the page's own named target (#681). Constellation
  figures stay complete, so a line may end in open sky; raising the limit
  shows the star through the ordinary stellar path. #307's figure anchors
  are gone, and with them the star that vanished when the owner zoomed
  from 12° to 8° near Betelgeuse.

## 2. Every ruling of the road

| ruling | where it stands | in short |
|---|---|---|
| the road (2026-11-07) | #698 | two dogfooding findings, each with an explicitly unqualified candidate and focused development checks; one final qualification of the accepted integrated head under a declared gate-impact plan; no projection, meteor, #697, measurement-tool, portable-bookmark or catalogue work |
| #699's behaviour (2026-11-07) | #699 | one global choice, off by default, remembered; each governed group alone; heading and application actions under one rule; turning on keeps focus, else the last opened, else the first; turning off reopens nothing; collapses are ordinary stored collapses, no migration; Chart Options' inner groups not governed; no computation |
| #571's amended direction (2026-11-07) | #571 | complete constellation figures; absolute stellar magnitude limit; no exceptional anchor stars; measure before building; no confirmation dialog, automatic limit change or "do not ask again" |
| D-1 to D-7 (2026-11-07) | `docs/decisions/complete-figures.md` §5; #571 | "I accept the measured result and rule D-1 through D-7 as recommended": the rule as measured; no mark at a hidden endpoint; not selectable; the count only in the view report when non-zero; the bilingual sentence; #307's study kept as the record of the superseded rule; focused checks, an unqualified candidate, one qualification under #700 |
| #699 accepted (2026-11-07) | #699 | "Works well" |
| #571 accepted (2026-11-07) | #571 | "Works well" |

## 3. The owner's checkpoints and verdicts

- **#699, Keep one section open** - candidate `518daa7a` opened on the
  owner's screen with the measured home of the control (section 4.1):
  *"Works well"*.
- **#571, the measured pages** - `f593dd5a`, no production change: the
  60°, 90°, 120° and globe pages as built and as proposed; ruled D-1 to
  D-7 the same day.
- **#571, the build** - candidate `256ec5f0`: *"Works well"*.

## 4. The contracts as built

### 4.1 The Controller accordion (#699)

- **Home, measured.** At the default 360 px Controller width the tab strip
  leaves 114 px (English) / 129 px (Norwegian) at 13 pt and 54 / 73 px at
  16 pt; even a short toggle needs 100 / 89 px at 16 pt. The choice
  therefore lives in a slim footer row (`CompanionWindow.setFooter`),
  about 28 px, which stays put while the tabs scroll above it.
- **The rule.** `ui.companion.OneSectionOpen` governs groups of sibling
  `CompanionSection`s: `JUranometriaMain` hands it the Chart tab's
  sections and the Solar System tab's body groups. While it is on, a
  section's `onOpened` - by its heading or by `setExpanded(true)` from an
  application action - collapses its siblings in that group only.
- **Turning on** brings each group to one open section: the one holding
  keyboard focus, otherwise the one most recently opened, otherwise the
  first in visual order. **Turning off** reopens nothing.
- **Persistence.** The choice is `companion.oneOpen` in the Controller's
  store, false until a reader chooses it. A collapse it makes goes through
  the section's ordinary `saveCollapsed`, so restoring the Controller
  restores exactly what was left; no key was added for sections and none
  migrated.
- **Focus and access.** The box is named, has an accessible name, a hover
  and an explanation in both languages (`companion.oneOpen.*`), and is an
  ordinary checkbox reached by the keyboard like every Controller control.
  The focus rule above means turning the choice on never collapses the
  section the reader is working in.
- **No computation.** `keepingOneSectionOpenGovernsEachTabAloneAndComputesNothing`
  (`SolarSystemCompanionJourneyTest`) opens every Solar System group with
  the choice on and proves no service is asked anything, as the released
  journey proves with it off.

### 4.2 Complete figures under an absolute limit (#571)

`docs/decisions/complete-figures.md` §6 records the build in full. In
short: `ChartRenderer` draws a star exactly when it is within the limit
or is the page's named target; figure segments are drawn from the stored
geometry, unchanged; the hit test, labels and inventory read the drawn
marks, so a hidden endpoint has no target, name or entry;
`FigureEndpoints.belowLimit` counts the endpoints left in open sky for
the view report's `figure-endpoints-below-limit:` line, written only when
non-zero; the chart and speech never state it.

**The measured visual cost** (`docs/studies/complete-figures/measurements.md`,
on the rule as built):

| page | limit | endpoints in open sky | as the limit rises |
|---|---:|---:|---|
| Orion 60° | V 5.0 | 0 | - |
| Orion 90° | V 4.0 | 43 | - |
| Orion 120° | V 4.0 | 76 (75 on the paper) | 4 at V 5, 1 at V 6, 0 at V 7 |
| Ursa Major 120° | V 4.0 | 56 | - |
| globe | V 5.0 | 21 | - |

Before the change the 120° page at V 4 drew 76 stars fainter than its
limit, down to V 6.54; now its faintest drawn star is V 4.00. Unrelated
stars within 6 px of a hidden endpoint: 0 to 4 per page. A few labels
stand slightly differently because faint marks no longer crowd them.

## 5. Both languages, accessibility, screen and paper

- **Languages.** Both features are fully in English and Norwegian:
  `companion.oneOpen.{label,a11y,hover,explain}` appended to both files;
  the *Constellation figures* explanation edited in place in both, its
  key and line unchanged. The language ledger stays at 180 unresolved,
  under its bound of 190.
- **Accessibility.** The new box is named and explained; the control
  explanations count 174 controls on 21 surfaces. The chart's spoken
  description is unchanged by #571 (it names the limit, not the stars);
  a hidden endpoint is never spoken.
- **Screen and paper.** #699 is Controller-only and touches no chart or
  sheet. #571 draws the same page on screen and on every sheet format; a
  hidden endpoint has no mark on either.

## 6. Every stopped, failed, superseded or incomplete run

Every record is under `~/juranometria-runs/runs/`, unchanged; nothing was
rerun into green. No gate has run on this road yet.

| run | what happened | how it was continued |
|---|---|---|
| #699's first full suite (`j698-devcheck/suite-1`) | 5 of 2 678 failed, 12 focus aborts: pins on the changed surfaces - one ledger row, the control-explanation count, the test-evidence counts, the Controller photographs that now show the row | each fixed; the second full run on the candidate's tree had 0 failed (2 666 successful, 12 aborts); its log was not kept under `runs/`, as the checkpoint on #699 states |
| #699's interface evidence (`gate2-1`) | 9 of 9 | - |
| #571's first heap-logged contracts (`heap-571-first`) | the canonical contract breached on 47 renderer-drawn pages (sky-language, meridian-ticks, label-placement, below-horizon, chart-instant and others) that the absolute limit moves | expected movement: each regenerated; the portable route was not run on that tree |
| #571's first full suite (`suite-2`) | 3 of 2 675 failed, 12 aborts: the two string reports and the test-evidence counts | each fixed |
| #571's heap-logged contracts (`heap-571-candidate`) | canonical breached on 9 promoted structure-emphasis pages (the generator had been rerun but the pages not copied from `build/emphasis-study`); portable OK | re-promoted; both contracts then OK (`contracts-571`, `contracts-ci-571`) |
| #571's full suite on the candidate (`suite-3`) | 0 of 2 675 failed, 49 desktop-focus aborts, none unexplained | - |
| #571's interface evidence (`gate2-2`) | 9 of 9 | - |
| superseded: #307's figure-anchor rule | `docs/studies/figure-anchors/` held as committed with its superseded status at its head; its generator, test and make target removed | the record of the old rule |
| superseded: #571's earlier proposal to omit a segment with an excluded endpoint | replaced by the owner's amended direction before any measurement | not measured or built |

Lessons this road adds:

- **a promoted study directory needs its copy step** after its generator
  reruns; a contract breach on a promoted page means the copy, not the
  drawing;
- **a rule that changes which stars are drawn moves every page that
  draws figures**, across studies that never mention figures; regenerate
  by the contract's list, not by grep.

## 7. Routes, evidence movement and performance

### 7.1 Routes

| head | local gates | CI | outcome |
|---|---|---|---|
| `518daa7a` (#699) | none (unqualified candidate) | none | accepted |
| `f593dd5a` (#571 study) | none | none | ruled |
| `256ec5f0` (#571 build) | none (unqualified candidate) | none | accepted |
| the integrated head with this handover | the final qualification (section 9) | once | - |

The suite stood at **2 672** tests at 6.3.0, **2 678** with #699 and
**2 675** with #571 (four `FigureAnchorTest` tests removed, one more
`CompleteFiguresTest` and view-report test added).

### 7.2 Evidence movement

From `856d1a23` to `256ec5f0`: **119 files, +1 830 / −2 037 lines** -
#699 33 files, the #571 study 25 files, the #571 build 91 files. Under
`docs/studies`, 94 files: `interface-language` (the Controller's
photographs with the new row, the strings), `complete-figures` (new),
and the pages the absolute limit moves - `meridian-ticks`,
`structure-emphasis` (9 promoted pages), `sky-language`,
`label-placement`, `below-horizon`, `chart-instant`,
`chart-instant-furniture`, `guide-and-explain`, `toggle-shortcuts`,
`overview-ink` and the globe platform records - with
`control-explanations`, `language-ledger`, `test-evidence`
(1 183 explicit hand-offs; 83 state files) and the provenance record,
whose promoted artifacts went from **976 to 989**. Source: two new
classes (`OneSectionOpen`, `FigureEndpoints`), two removed
(`FigureAnchors`, `FigureAnchorStudyMain`), one study generator rewritten
(`CompleteFiguresStudyMain`). The packaged licensing inventory stays at
**16 resources**.

**The unaffected-chart invariance.** Everything outside the studies
listed above reproduced unchanged under both contracts on the candidate's
tree (779 reproduced, 89 through generator output, 46 platform-recorded):
every 8° and closer ladder page, which draws no figures, the gallery,
the sheets and every planet study. Only pages that draw figures moved,
because #307 had kept stars beyond the limit on them.

### 7.3 Performance

The heap, measured on #571's tree (`heap-571-candidate`, #676's commands,
2 GiB cap): canonical **14.9 min**, peak **1 600 MB** live; portable
**16.3 min**, peak **1 770 MB**; no full collection on either. The
portable peak is 153 MB above 6.3.0's 1 617 MB and below 6.1.0's
1 915 MB; the cap stays at 2 GiB, to be measured again before the next
evidence-heavy road. The road from the owner's filing (2026-11-07) to
both accepted candidates took one day.

## 8. What did not change

- **Catalogue, services, packs and planets.** Untouched.
- **Chart pages without hidden figure stars.** Unchanged, as section 7.2
  states; the page's named target keeps its #681 privilege.
- **Controller behaviour by default.** With the choice off - the default -
  sections behave exactly as released.
- **Persistence.** One new key, `companion.oneOpen`; no chart option,
  migration or existing key moved.
- **Keyboard.** No shortcut was added or moved.
- **Speech.** The chart's description is unchanged; the Controller's new
  box is described like every other control.
- **Export.** No export surface changed.

## 9. The packaged journey, the final qualification, and the owner's separate decisions

- **The packaged journey.** `PackagedAcceptanceMain` gains
  `dogfoodingJourney`, marker **`dogfooding OK`**: Keep one section open
  through the real store and box (fresh off, opening collapses only its
  own group's siblings as stored collapses, off reopens nothing, a
  restart reads it back), then the 120° Orion page at V 4 through the
  production component (no drawn star beyond V 4; 75 endpoints counted
  for the view report; figures off draw the same 139 stars and count
  none; V 7 admits every endpoint). Passed in the app image built on
  this tree (`app-image-3`).
- **The final qualification** of the integrated head, for the owner's
  approval: **all seven gates** - the head changes chart drawing,
  application classes, interface photographs, the packaged journeys and
  the distribution's contents - with the inventory at 16 and the markers
  extended by `dogfooding OK`; then the PR (closing #699, #571 and #700,
  keeping road #698 open), CI once and the pinned merge.

Then, as for 6.3.0, each of the following is a separate owner decision:

1. the version number and the `VERSION` bump with its changelog date and
   every version-fed regeneration (including `complete-figures`);
2. the release pull request and its merge under #494;
3. the release rehearsal, which publishes nothing;
4. the local annotated tag;
5. the tag push, which publishes;
6. the public verification and the close-out of road #698 and the
   Sprint 58 milestone.

## 10. What 6.4.0 contains, and what it explicitly does not

**Contains:** Keep one section open with its footer row, rule and
persistence; complete figures under an absolute stellar limit with the
view-report count and the bilingual explanation.

**Does not:** a hidden-endpoint mark, count on the chart or in speech; a
confirmation dialog or automatic limit change; governing Chart Options'
inner groups; a keyboard shortcut for either; and none of the backlog
below.

## 11. The backlog this road hands on, without starting it

- **Projections** (#686, #687): the absolute limit and complete figures
  apply to every projection the renderer draws; a new projection inherits
  them with nothing to add.
- **Meteor showers** (#564): a radiant is not a star and is untouched by
  the limit rule; its furniture would go through the existing seam.
- **Globe orientation** (#697): untouched; the globe's figure endpoints
  follow the same rule (21 in open sky at V 5).
- **Measurement tools** (#617): untouched; a measurement to a hidden
  endpoint would need its own ruling, since the endpoint is deliberately
  not a target.
- **Portable bookmarks** (#688): untouched; a bookmark's main object keeps
  its #681 privilege under the absolute limit.
- Still open as before: #618, #613, #598, #594, #589, #566, #554, #553,
  #417, #387, #373, #370, #362, #356, #355, #351, #339, #327, #326, #324,
  #295, #293, #282, and the Milky Way sprint #189-#192 with PR #199,
  blocked on licensing.
