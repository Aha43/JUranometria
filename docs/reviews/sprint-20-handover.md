# Sprint 20 handover — Chart furniture

Sprint 20 (issues #179–#181, milestone 20) is deliberately small: the
chart draws brightness as circle size and never said so, the
Inspector was two menu clicks away, and the lower-left panel had no
name a reader could see. This handover is the coder's account for the
review.

It is also the **first sprint delivered as one pull request under the
automated Claude–Codex review loop** (#178), and the trial is
reported honestly at the end.

## What Sprint 20 delivered

- **The gate** (#179):
  [`docs/decisions/chart-furniture.md`](../decisions/chart-furniture.md),
  measured over eight pages with `make furniture-study`.
- **The surface** (#180): the Inspector on the toolbar behind one
  shared switch, and both pieces of furniture as independent,
  persisted, migrated options.
- **The close** (#181): the journey, the packaged evidence, the
  documentation, and this handover.

## The decisions, and the measurements behind them

**Three samples, not nine.** A key stepping by one magnitude would
draw circles **0.4–1.4 px** apart — differences nobody can see, and a
precision the drawing does not have. The key shows the top of the
scale, its middle, and **the limit itself**, so it always names the
faintest star the page actually draws. At V 4 the three circles
differ by only 1.19 px, which is the honest answer: a bright-limited
page really does draw dots of nearly equal size.

**Upper right**, by elimination — the title block owns the lower
left, right-ascension labels the bottom, declination labels the left —
and then by inspection.

**Off by default, measured rather than inherited.** The box covers
288–635 px of chart ink depending on the page, including **290 px of
star ink on Orion at 36°**, where it blanks a patch of dense sky. A
reader who wants the scale explained accepts that knowingly; one who
does not should never pay it. Two consequences follow: **the released
default page is unchanged, so no reference or study image churns this
sprint**, and the key is discoverable exactly where every other layer
is.

**"Title block"** is the name everywhere a reader can see it, being
the cartographic term for a panel that states what the sheet shows.
"Chart title" was rejected: the panel is five statements about the
page, not a title.

**No third options group.** Two checkboxes joined Content rather than
earning a heading over two lines.

## One switch for the Inspector

`InspectorToggle` is a small model in the `ui` package: a control asks
it to flip and is told what actually happened. The toolbar button,
the View menu item, and the window's own width therefore cannot
disagree. The button is **disabled, never selected**, when the panel
cannot be shown, so it never claims a panel that is not there.

The toolbar learns nothing about panels or windows, and that is
checked rather than intended: a test reads `AtlasToolbar.java` and
requires it to mention no frame, window, panel, or application
package.

The icon is Tabler `list-details`, fetched through the pinned
`scripts/download-icons.sh` at v3.46.0 and recorded in `ICONS.md`
beside the others.

## Preferences and the first option that ships off

The magnitude key is the **first option the atlas ships switched
off**, and the store had no way to say so: `flag()` reads anything
but the literal `"false"` as on, which is right for a layer whose
default is on and exactly wrong here. `offByDefaultFlag()` is its
mirror. The consequence matters for an upgrading reader: a 1.1.0
store has no key at all, and must not upgrade into furniture nobody
asked for. Tested with the complete key set 1.1.0 wrote, and with
six damaged values (`TRUE`, `yes`, `1`, empty, whitespace, `banana`),
none of which can turn the key on.

## The 1.0 contract's key count

The contract stated a number of `chart.*` keys, and that number has
now been wrong twice: written as seven, corrected to nine by the
1.0 audit, and reached eleven here. **It no longer states one.** What
1.0 promises is that every key an earlier release wrote keeps its
meaning and that a key a newer release adds takes its documented
default — not that the set never grows. That is a documentation
correction, not a contract change.

## Verification

- **443 tests** on a display; headless, the display-dependent
  journeys abort visibly rather than fail.
- **The M31 reference is byte-identical** and no study image changed,
  because the released default page did not change.
- `make furniture-study` reproduces its measurements and its sixteen
  pages byte-for-byte.
- Rendering is covered in **all four furniture combinations**, and a
  test requires that no star label is ever placed where the key will
  cover it.
- The packaged acceptance now exercises both options **inside the
  native image**, on the packaged renderer, and checks that the key's
  samples end at the page's own limit:
  `chart furniture OK (… key samples [0.0, 4.0, 8.0])`. Verified in a
  built macOS image here, and by the four-platform matrix in CI.
- The journey drives the real controls throughout: the toolbar
  button, the View menu item, the real Chart Options dialog opened
  from the menu, its Cancel, OK and Restore Defaults buttons, and the
  toolbar's Reset view.

## Residual risks

- **The key's placement is fixed.** On a page whose upper right
  happens to hold something a reader cares about, the key covers it
  and the only remedy is to switch the key off. A movable or
  auto-placed key was not attempted.
- **Three samples cannot show the whole scale.** Between V 0 and the
  limit there are magnitudes the key does not draw, and a reader
  could over-read the middle sample as the only intermediate size.
- **The key describes the page's limit, not its contents.** A page
  whose brightest star is V 5 still shows a V 0 circle, because the
  scale is fixed and a page-dependent key would change as the reader
  panned.
- **The title block can now be switched off**, which removes the
  page's only statement of its own frame, field and orientation. That
  is the reader's choice, but a chart image saved with it off carries
  less of its own provenance. Nothing exports images yet, so this
  costs nothing today.
- **Furniture is not in the Inspector's world.** Selecting a mark and
  switching furniture are unrelated, which is right, but a reader
  looking for "what size is this dot" in the Inspector will not find
  a magnitude-scale explanation there.

## The automated-review trial

This is the first sprint delivered as one PR under the automated
loop, and this section is filled in as the trial runs:

- **What triggered reviews**: marking the pull request ready, and
  each pushed update after that.
- **Rounds**: recorded here as they complete.
- **Head-commit naming**: each review is checked to name the exact
  head it reviewed, per `CLAUDE.md`; a review of an older commit is
  ignored.
- **Where human judgement was needed**: recorded honestly, including
  any finding disputed rather than fixed.

## Recommendation

Hold for the owner. Sprint 20 changes no promise of the 1.0 contract
and leaves the released default page byte-identical, so it is a
**minor** release when the owner decides to cut one — but that
decision, the merge, the milestone and the tag are the owner's, not
this pull request's.
