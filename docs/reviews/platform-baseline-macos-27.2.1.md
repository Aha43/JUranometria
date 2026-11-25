# Platform baseline: macOS 27.2.1 after the update (#737)

**Status: unqualified study checkpoint, awaiting the owner's rulings.**
A measurement study from published `v7.0.0` / main `122c3187`. No
production file changed; no gate was run or is claimed. Nothing below
was retried: each command ran once and its first result is the result.

Retained records (the owner's machine, outside the repository):
`~/juranometria-runs/runs/platform-737/` - `env/`, `A-published/`,
`B-source/`, `driver/`.

## 1. The environment

| | before (the 7.0.0 road's gates) | now |
|---|---|---|
| macOS | 27.0.1 (26A434) | **27.2.1 (26B131)**, installed 2026-11-22 23:26 |
| Darwin kernel | not recorded | 27.2.0, `xnu-13432.41.4~25`, arm64 |
| Command Line Tools | 27.0 (2026-09-25) | **27.2** (`27.2.0.0.1792721180`), installed 2026-11-22 23:49 |
| selected developer directory | not recorded | `/Applications/Xcode.app/Contents/Developer` (Xcode 27.2, 27B52) |
| clang | not recorded | Apple clang 21.0.0 (clang-2100.4.1.1) |
| Mac | MacBook Pro `Mac17,2`, Apple M5, 10 cores, 24 GB | the same |
| displays | built-in | built-in Liquid Retina XDR, 3024 x 1964 native, drawn at 1512 x 982 points (2x); **no external display attached** |
| JDK | Homebrew OpenJDK 21.0.12.1 | the same, `/opt/homebrew/opt/openjdk@21` |
| Make / Git / Python / gh | GNU Make 3.81 | GNU Make 3.81 (`/usr/bin/make`), Git 2.54.0 (Apple Git-157), Python 3.14.7 (Homebrew), gh 2.104.0 |
| locale and languages | not recorded | `en_NO`; languages `en-NO`, `nb-NO` |
| appearance | not recorded | Dark; no accent override; no reduce-motion, increase-contrast or zoom setting found |

"Before" is what the gate records and the 7.0.0 handover retained
(`27.0.1 26A434` in every gate README since 2026-10-18; the handover's
section on the next road). Where it says "not recorded", no before/after
claim is made.

## 2. Subject A - the published reader artifact

`JUranometria-7.0.0-macos-arm64.zip` and `JUranometria-7.0.0-portable.zip`
fetched from the public release with `gh release download` into an empty
folder, unpacked with `ditto -x -k` outside any source tree.

| check | result |
|---|---|
| archives against the published `SHA256SUMS.txt` | both OK |
| bundle | `JUranometria.app`, 317 MB, arm64 only, version 7.0.0, bundled runtime 21.0.12.1 |
| the image's own `juranometria-acceptance` | `PACKAGED ACCEPTANCE OK`, 81 journeys, among them whole sky and ecliptic longitude |
| launch by `open` (the Finder route) | window "JUranometria 7.0.0" at 1512 x 868 in about ten seconds |
| quit by Cmd-Q, relaunch, quit | clean each time; the same window; no crash report |
| the reader's preferences (`com.apple.java.util.prefs`, node `juranometria`) | byte-identical before and after |
| user data (`~/Library/Application Support/JUranometria/bookmarks.json` and `.bak`) | unchanged |
| signature | ad-hoc, not notarized, no stapled ticket - as the README states and #282 tracks |
| **signature seal** | **invalid**: `codesign --verify --deep --strict` reports 15 "file modified" under `Contents/runtime/Contents/Home/legal/`, and `spctl` answers "a sealed resource is missing or invalid" |

The seal is finding F-2 below; it is a property of the archive, not of
the macOS update.

The portable archive unpacks to the documented layout. Its contents are
byte-identical to the archive built from source here except one line of
the jar's manifest (finding F-4).

## 3. Subject B - a clean source build

A fresh worktree at `v7.0.0`; `scripts/download-libs.sh` (the documented
bootstrap; four pinned jars, checksums verified by the script); **no
`imports/`**. Then, once each, in this order:

| step | result | time |
|---|---|---|
| `make classes` | OK | 4 s |
| `make test` | 2,817 found and started; **2,804 passed, 1 failed**, 12 aborted, 0 skipped | 683 s |
| `InterfaceEvidenceGateTest` alone (every interface photographer, twice) | 8 passed, **1 failed**, 0 aborted; **no capture refused** | 225 s |
| `make evidence-contracts-ci` (portable) | `EVIDENCE CONTRACTS OK` | 1,192 s |
| `make app-image` | `PACKAGED ACCEPTANCE OK` | 56 s |
| `make dist` | contents exact; headless smoke render OK from a path with a space | 19 s |
| `make gallery-site` | 24 article pages | under 1 s |
| `make evidence-contracts` (canonical; raw sources cloned from the main checkout first) | **`CONTRACT BREACH`** | 1,079 s |
| `make evidence-provenance` | 1,085 artifacts, 0 newly dated | 4 s |

**The one suite failure and the one interface-gate failure are the same
test and the same cause**:
`everyCommittedArtifactIsWhatItsGeneratorProducesTwice` -
`export-strings.md: committed bytes are not what the generator produces`.
The whole difference, in both of the gate's runs, is one line:

```
< Recorded on: `Mac OS X 27.0.1/aarch64/Homebrew 21.0.12.1`
> Recorded on: `Mac OS X 27.2.1/aarch64/Homebrew 21.0.12.1`
```

**The canonical breach is the same line** in
`docs/studies/sky-language/placement.md`. After the portable and after
the canonical run the tree held 49 modified files - 48 `platform.md` and
that `placement.md` - and every changed line in all 49 is that
"Recorded on" line (52 lines; the diffs are kept as
`4-portable-recorded-on.patch` and `9-after-provenance.patch`). No
picture, sheet, PDF, SVG, word list or measurement moved by a byte.

**The 12 aborts** are the usual keyboard-focus assumptions, the same
count as on the 7.0.0 road's quiet runs; none is outside that family.

Timings are within a few seconds of the 7.0.0 road's on 27.0.1 (suite
691 s, canonical about 17.6 min, portable about 20.0 min).

### What the source build assumes

- `make` and `git` at `/usr/bin` are the Command Line Tools' shims: the
  tools must be installed. Nothing else from Xcode is used; the selected
  developer directory was the full Xcode and the build did not care.
- A JDK 21 with `jpackage` at the Makefile's path (Homebrew
  `openjdk@21`).
- `python3` for the site build only.
- Network once, for the four pinned jars.
- **No raw sources** for the build, the suite, the portable contract,
  the app image, the archive or the site. Only the canonical contract
  needs `imports/raw`, which no fresh clone has.

## 4. Published artifact against source build

| | published (A) | built here (B) |
|---|---|---|
| launches, quits, passes its packaged acceptance | yes | yes |
| signature seal | **invalid** (15 runtime `legal` files) | **valid** - the same files are symbolic links in the built image |
| portable archive | sha `8af37104...` | sha `513eac32...`; identical contents except the jar manifest's `Created-By` (`21.0.13 (Eclipse Adoptium)` against `21.0.12.1 (Homebrew)`) |
| Saturn at the former "false moons" instant, 24 pages | byte-identical between A's jar and B's classes | |

Neither subject's success is taken for the other's: A was exercised only
from its unpacked archive, B only from its worktree; the last row is the
one place they were set side by side on purpose.

## 5. The audit, item by item

"Measured" means a retained log, image or byte comparison. "Hands-on"
means a person looked at the live window. Most of what #737 lists was
measured through the application's own photographers and journeys; very
little was looked at live, and section 7 says so.

| item | how | result |
|---|---|---|
| ordinary launch, shutdown, restart | A, live, twice | OK |
| full suite | B | 1 failure (F-1), 12 focus aborts |
| every interface photographer | B, the interface gate's two complete runs | every image byte-identical to the committed one; one word list differs by F-1's line; no capture refused |
| Controller wrapping and resizing; Place and Time, Export Sheet and the other dialogs with declared geometry | B, the photographers above | committed geometry reproduced byte for byte |
| both languages, both grounds, ordinary and enlarged text | B, the photographers and the study generators | reproduced byte for byte |
| label placement, selected-object box, chart title, font metrics | B, canonical and portable regeneration | reproduced byte for byte (font metrics did not move with the update) |
| whole sky, ecliptic-longitude ruler, solar-longitude marker | A's and B's packaged acceptance; B's studies | OK; studies byte-identical |
| close Saturn, the former Cassini-gap "false moons" case | study driver outside the repository, through A's jar and B's classes: 2034-01-08 at 00:00 and 20:00 UTC, fields 1°, 0.5°, 0.25°, at 1x and 2x | no dots on the rings at any field or scale; figure 46.6 x 19.9 px at 0.25°; A and B byte-identical |
| bookmarks, export, restart, preference persistence | A's and B's packaged acceptance (their own preference nodes and folders); A live for restart and for leaving the reader's own data untouched | OK |
| app image, portable archive, data and user paths | sections 2 to 4 | OK but for F-2 |
| built-in display | everything above | OK |
| external display, moving between displays | - | **not tested: none attached** |

The Saturn driver is `driver/src/SaturnClose.java`: it builds the
application's own `ChartComponent` and `SaturnModule` as the packaged
acceptance does and paints them to an image. It ran under the Homebrew
JDK, because the bundled runtime has no `java` launcher; the jar and
libraries were the published ones.

## 6. Findings, classified

**F-1. Fifty committed platform records name macOS 27.0.1, and three
local checks fail on 27.2.1 for that line alone.**
*Class: test and evidence fragility. Not a rendering difference.*
The full suite (1 failure), the interface evidence gate and the
canonical contract cannot pass on this machine until the line is
re-recorded; CI is unaffected (it does not byte-compare these records).
The same happened at the last update and was repaired by commit
`334206a3` ("Re-record the environment line of 25 platform records for
macOS 27.0.1", #469). Until it is repaired, gates 1, 2 and 3 cannot be
green locally, so nothing can be qualified on this machine.

**F-2. The published macOS image's signature seal is broken by the way
CI zips it.**
*Class: packaging/install defect. Independent of the macOS update.*
`app-image.yml` packs the image with `zip -q -r -9`, which stores the
fifteen symbolic links under the runtime's `legal` folder as copies; the
ad-hoc signature was made over links. Reproduced here from the image
built in section 3: zipped the same way, `codesign` fails and `spctl`
says "a sealed resource is missing or invalid"; zipped with `-y`
(links kept), `codesign` passes and `spctl` says only "rejected" - the
ordinary answer for an unsigned application. This may be why #282's
readers see "damaged" with no *Open Anyway*; **that connection is not
proven here**, because `gh` does not quarantine a download and no
Gatekeeper dialog was seen in this study.

**F-3. The macOS update moved no rendered byte.**
*Class: portable and correct.* Every picture, sheet and measurement the
canonical and portable routes regenerate, and every interface image,
reproduced byte for byte on 27.2.1 with Command Line Tools 27.2. Fonts,
label placement and dialog geometry are unchanged.

**F-4. The published portable jar says `Created-By: 21.0.13 (Eclipse
Adoptium)` while the images bundle and report 21.0.12.1.**
*Class: harmless platform difference, already known* (the JDK patch the
jar job resolves floats). Every class and resource is identical. Noted
because "the published portable archive is reproducible from source"
is true of its contents and not of its checksum.

**F-5. The image's `Info.plist` carries jpackage's defaults**:
`LSMinimumSystemVersion` 10.11, a microphone usage sentence the
application never uses, and "Copyright (C) 2026".
*Class: harmless platform difference; a candidate for the signing work
under #282, not for this road.*

**F-6. The published application, the source build and the packaged
acceptance share the reader's one preference node** (`juranometria`).
*Class: observation, portable and correct as built* - the 7.0.0 guard
correction relies on it. It means no "first run" was observed here: the
owner's stored preferences were present and were left untouched.

## 7. What was not tested

- **No Gatekeeper dialog.** The download was not quarantined; the
  reader's real first launch from a browser download was not seen.
- **No external display**, and so no moving or resizing between
  displays, and no non-Retina rendering on a live screen.
- **No true first run** with an empty preference node and no bookmarks
  folder: that would have meant removing the owner's stored choices.
- **Almost nothing was looked at live.** The live part was launch, the
  default M31 page, quit and restart of the published application. The
  Controller, the dialogs, both languages and grounds, enlarged text,
  the whole sky, the ruler and marker, bookmarks and export were proved
  by regenerating their committed evidence and by the packaged journeys,
  not by a person using the window on the updated system.
- **System-level text scaling and accessibility display settings** were
  recorded as found (none set) and not varied.
- **The Intel image and the Windows and Linux images** - outside this
  issue.
- **The bundled runtime under the Saturn driver** - the driver used the
  published jar under the Homebrew JDK of the same version.

## 8. Proposed rulings

1. **Accept the baseline**: macOS 27.2.1 / Command Line Tools 27.2 on
   this Mac renders what 27.0.1 rendered, byte for byte.
2. **F-1 enters #735**: re-record the environment line of the fifty
   records by regeneration, as `334206a3` did. Evidence only, no
   production code. Estimate: under an hour of work, then the gates the
   diff calls for (I would expect 1, 2, 3, 4 and 7; 5 and 6 only if a
   `src/` file moves, which this should not need).
   A separate question for the owner, not proposed for this road: whether
   a platform record should go on naming the operating system's patch
   version at all, since every update makes three local checks fail for
   a line that says nothing about the rendering.
3. **F-2: decide where it belongs.** The repair is small - keep
   symbolic links when the macOS images are zipped (`zip -y`, or
   `ditto -c -k --keepParent`), and have `release-artifacts.sh` or the
   image job verify the seal after unpacking. Estimate: a workflow-only
   change plus one rehearsal run to prove the unpacked seal on both
   macOS images. It can enter #735 as a platform repair, or go to #282
   as part of the signing work. Before either, one hands-on check by the
   owner would settle whether it changes what a reader sees: download
   the 7.0.0 image with a browser and note the dialog, then the same for
   a links-kept archive.
4. **F-4, F-5, F-6**: record and leave.
5. **Before the baseline is called complete**, the owner's hands on the
   published application for the items section 7 says were not looked at
   live, and an external display when one is at hand.

#734, #735 and #736 are not started.

## 9. Rulings of 2026-11-23, and the re-recording

The owner ruled at the checkpoint:

- **The baseline is accepted, within its recorded scope.** macOS 27.2.1
  renders the committed material byte for byte as 27.0.1 did, and the
  close-Saturn repair holds.
- **F-1 is completed inside #737**, not as a product repair in #735: the
  committed platform records are regenerated to name macOS 27.2.1.
- **F-2 enters #735 and is linked to #282**, for both macOS
  architectures, held by checks on the extracted published-form
  archive. The claim is narrow: a valid signature seal. Not
  notarization, and not Gatekeeper's behaviour for a quarantined
  download.
- **The owner's hands-on inspection** of the published application
  (Controller, Place and Time, Export Sheet, whole sky with its ruler,
  close Saturn) comes before #737 closes and is recorded separately
  from this automated evidence (section 10, when given).
- **The external display stays untested**, does not block #737, and is
  carried to the later platform checkpoint.

### What was re-recorded

Fifty files, one kind of line. Records are in
`~/juranometria-runs/runs/platform-737/C-rerecord`.

| Files | How regenerated | Lines |
|---|---|---|
| 48 `platform.md` under `docs/studies/` | `make evidence-contracts-ci` on the study head, in place (`EVIDENCE CONTRACTS OK`, 1,205 s) | 51 |
| `docs/studies/sky-language/placement.md` | the same run | 1 |
| `docs/studies/interface-language/export-strings.md` | the export photographer's output from the interface gate run of section 3 (its two runs agree byte for byte) | 1 |

- Every removed line is ``Recorded on: `Mac OS X 27.0.1/aarch64/Homebrew 21.0.12.1` `` and every added line is the same with `27.2.1`:
  53 lines out, 53 in, nothing else. Forty-seven files change one line;
  three carry the environment line twice and change two.
- The regeneration of this section and the one left behind by section
  3's portable run are the same patch, byte for byte: two independent
  runs, one result.
- No image, sheet, measurement, fixture or source file moved.
- **Provenance did not need to move**: `make evidence-provenance` over
  the re-recorded tree reports 1,085 promoted artifacts, 0 newly dated,
  and leaves `docs/studies/PROVENANCE.md` unchanged. Platform records
  are not promoted renderings.

### What was deliberately not re-recorded

Two `platform.md` files still name 27.0.1 and are right to:
`docs/studies/all-sky/platform.md` (generator removed in #687) and
`docs/studies/figure-anchors/platform.md` (superseded rule, #571). Both
are marked historical and held as committed; no generator produces them
on this machine, so nothing here was recorded on 27.2.1 and the line
stays what it was. Neither is compared by a check that failed in
section 3.
