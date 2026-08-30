# Sprint 11 handover — Give the application a public face

Written 2026-08-30 by the coder for the independent pre-release review
of 0.11.0, following the established handover pattern. The sprint's
issues, executed in order: #98 (About and licensing dialog), #99
(persistent Light/Dark appearance), #100 (this finish). Run to the
handover in one pass per the sprint instruction, for one review.

## What Sprint 11 delivered

- **A restrained conventional menu bar**: a File menu carrying
  Settings… and a Help menu carrying About JUranometria — nothing
  else, no placeholder Chart Options, no toolbar changes. Actions are
  injected, so the wiring is headless-testable and the menus cannot
  reach chart state.
- **An honest About surface**: the packaged version through the
  existing `AppInfo`/`VERSION` path (a test proves it equals the
  packaged file — never a second hard-coded copy), the product
  description in the established language, and the licensing of the
  code and all four bundled resource families — with the Tycho-2
  non-commercial consequence in plain words — plus the complete
  bundled notices, all offline. **The source-of-truth rule holds
  structurally**: the compact summary is the packaged
  `resources/about/licensing-summary.txt`, tested to agree with
  `LICENSING.md` (normalized licence identifiers and the
  non-commercial statement present in both), and the fuller view
  concatenates the notice and licence files already shipped in the
  jar. No legal prose lives in Java strings.
- **A persistent appearance setting** behind a tiny injectable
  boundary (`AppearanceStore`, JDK preferences only): Light default;
  OK applies live to the whole tree and persists; **nothing changes
  until OK**, so Cancel, window close, and Escape trivially leave
  both the store and the appearance untouched; corrupt or unknown
  stored values mean the light default, never a launch failure. The
  precedence rule lives in one documented place and is tested:
  `--dark` wins for its session and never rewrites the store.
  Appearance is application state — chart view state, catalogues, and
  painting are never involved.

## The journey, exercised

`PublicFaceJourneyTest` drives real windows through the real menu
items: About opens owned/centred/accessible and closes on Escape;
Settings opens the same way; choosing Dark and confirming applies the
theme immediately (asserted on the active look-and-feel) and persists
it; a **fresh store over the same node proves the session boundary**;
`--dark` is proven a non-writing override in both directions; Cancel
after selecting Light changes neither the store nor the appearance;
the return to Light persists; and the chart controller's state is
proven untouched throughout. The headless-safe layer (dialog content,
menu wiring, summary/notices resources, preference boundary) is
tested unconditionally.

The fresh-clone journey was re-run end to end: bootstrap, 217/217
tests, byte-identical M31 reference, `make app`, and a running
`java -jar` launch with the menu bar aboard; the tree stays clean.

## Visual verification

Both themes launched with the About and Settings dialogs open and
inspected: FlatLight and FlatDark chrome style both dialogs, the
correct appearance is preselected in Settings for each session, the
non-commercial statement is visible in the compact About view without
scrolling past it, and the chart page behind remains the atlas's own
white paper and dark ink in both. The dialogs are compact enough for
the small-window case (the About summary scrolls within a fixed
compact viewport rather than growing the dialog).

## Platform behaviour, stated honestly

- macOS runs with the screen menu bar
  (`apple.laf.useScreenMenuBar=true`), so the File and Help menus
  appear in the system bar beside the macOS-provided application
  menu; on Linux and in the visual-check harness they appear
  in-window. The Settings menu was renamed from an app-named menu to
  File during the owner's review: macOS already shows an application
  menu with the product's name, and the duplicate read wrongly. The
  fuller macOS answer — `java.awt.Desktop` about/preferences handlers
  placing both items into the native application menu — remains
  explicitly out of scope this sprint and is the natural follow-up
  when native integration is wanted.
- The journey test requires a display and is **skipped, not silently
  passed, on headless runners** (the CI check remains green with the
  headless-safe layer still enforced there; the journey runs on the
  development machine and in review).

## Worth extra scrutiny

1. **The Escape binding is exercised through its registered action**
   in the journey test rather than a synthesized key event through the
   full focus pipeline — the binding's registration and effect are
   both asserted, but focus-order subtleties are not simulated.
2. **`FlatLaf.updateUI()` restyles all open windows on an accepted
   change**; with only the atlas frame and modeless dialogs open this
   is the whole story today, but any future long-lived window joins
   that contract implicitly.
3. **The summary/LICENSING agreement test is lexical** (normalized
   identifiers and the NC statement), deliberately not a prose diff —
   the two documents serve different readers and may word things
   differently while agreeing on the facts.

## Sprint review answers

- **Does the application now identify itself honestly?** Yes: name,
  packaged version, description, and the full licensing story —
  including the one restriction an end user must know (non-commercial
  while the Tycho-2-derived data is included) — readable offline from
  the running application, with the repository document and the
  end-user summary held in tested agreement.
- **Is the appearance setting trustworthy?** Yes, by construction:
  only OK applies and persists; every abandonment path is inert; the
  override flag cannot corrupt the stored choice; corrupt storage
  cannot prevent launch; and persistence crosses a proven session
  boundary.
- **Did the sprint stay restrained?** Two menus, two dialogs, one
  preference. No Chart Options shell, no toolbar changes, no new
  chart ink; the M31 reference is byte-identical.
- **What next?** The standing product recommendation is unchanged and
  now twice-deferred: **star names, Bayer–Flamsteed identifiers, and
  common-alias search**, with wheel zoom about the pointer after.
  The new Settings dialog also gives a future chart-options surface a
  natural home when its content earns existence — but building that
  shell early remains rejected.

## Process expectations

The established pattern: this handover accompanies the open sprint PR;
the independent Codex review lands as
`docs/reviews/sprint-11-codex-review.md`; findings are fixed on the PR;
both documents are committed with the fixes; then merge, close
milestone 11, and cut 0.11.0.
