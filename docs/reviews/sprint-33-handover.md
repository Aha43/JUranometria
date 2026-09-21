# Sprint 33 handover — Name the sky in Norwegian

Three issues behind a gate, four pull requests. This is what the
sprint did, what it got wrong on the way, what owner testing found
that no check could, what is still owed, and what version it should
be.

Written against the accepted tree: PR #363 at `dbda14b`, merged as
`c3bb5c8`, plus the release preparation that follows it on
`release/2.1.0`.

## Why the sprint existed

The atlas spoke English. Not as a decision anyone had made — as the
absence of one. Every sentence it said, every name it drew, and every
word inside an exported file was English because that is the language
it had been written in, and nothing in it could have said otherwise.

The obstacle was never translation. It was that the application had
no notion of *a language a reader chose*, and several notions of
language that were not the reader's at all: the operating system's,
through `Locale.getDefault()`; the JDK's, through Swing's own
resource bundles; and the catalogue's, through Latin constellation
names that are not a language but a nomenclature.

Separating those is most of what this sprint did.

## What shipped

**The foundation (#348).** Interface language and chart language as
two independent, persisted settings, each resolved against what the
installation actually offers. Languages arrive by being *installed*:
discovery reads the descriptors present rather than a list in code,
which is why English registers as a descriptor even though its words
live in Java. A missing phrase falls back to English visibly; a key
never reaches a reader as itself.

**The names (#349).** Norwegian Bokmål constellation names on the
chart, from a reviewed set carried with its notice, licence and
provenance, reaching labels on screen and in every exported chart.
Identity, order, marks and geometry are unchanged: the same page,
with different names written on it.

**The application's own words (#350).** Every reader-facing surface
the atlas owns, the drawn and exported page included, through a
`PageWords` seam declared in the lower layer so that `render`,
`project` and `sheet` draw and write without knowing a language
exists.

## Three things that were not translation

**The toolkit's own words.** The buttons a reader presses to answer a
dialog, and every label in the file chooser, came from Swing,
resolved against the operating system. The JDK ships no Norwegian for
those at all, so a Norwegian reader answered a Norwegian question
with **Yes** on every machine, and a reader on a German desktop met
German under Norwegian sentences. The atlas installs its own
forty-one values now, in both languages.

**A layout with two answers.** Chart Options settled at either of two
self-consistent widths from identical inputs — 419 on 25 runs in 30
and 420 on the other five — because a wrapping label reported a
preferred width computed at the width it was last measured at. A
fixed point is no help when the function has two. Fixed at source:
the dialog declares the line width its prose is broken to.

**A restart boundary nobody had mentioned.** The interface language
applies fully at the next start, and the session between is mixed —
dialogs opened afterwards speak the new language while the menu bar,
the toolbar and the drawn page keep the old. Nothing said so.

## What owner testing found that no check could

The owner chose Norsk bokmål and the chart's title block went on
saying *Centre* and *Field*. Nothing was missing: the words existed
and a restart produced them. What was missing was anyone saying so.

That is the whole finding, and it could not have come from a test.
Every check the sprint had asked whether the resources were right.
None asked whether a reader would understand what they were seeing.

The repair is one notice, and the contract around it is larger than
the notice: exactly once, for a genuinely changed interface language,
never for confirming something else, both halves from one language or
neither, and decided after the choice is persisted so that a reader
can lose a sentence but never a setting.

## Every correction worth carrying forward

**A resource test does not prove the shipped wire.** The title-block
defect would have passed any test of `PageText`. What it needed was a
journey that starts the real application from a stored language and
asks the chart's own words. Mutating the page back to a one-time
English resolution fails that journey with the owner's exact symptom.

**An issue body is not the code.** The changelog draft claimed the
Norwegian names reach search and On This Page, because #349's issue
said so. They do not: search matches the genitive and the IAU
abbreviation, On This Page appends the abbreviation, and the chart's
spoken description names the page's subject. Read the code.

**Consent is per question, not per dialog.** #348 recorded both
language keys whenever either was chosen, reasoning that absence must
not claim a reader was never asked. The reasoning was right about
absence and wrong about who had answered: a reader upgrading from
2.0.0 who opened Settings and pressed OK was recorded as having
chosen English. `docs/decisions/sky-language.md` carries the
supersession with the reason.

**A check's reach must match its claim.** The display evidence gate
compared regenerated bytes with committed bytes everywhere, and CI
reported 104 of 106 artifacts stale — Linux had drawn them correctly
in its own fonts. Reproduced-twice is true anywhere; equals-committed
is true only where those bytes were recorded.

**Five runs agreeing is not determinism.** Three intermittent
renderings were found and two were fixed at source. The measurement
that mattered each time was the one taken under load.

## The evidence

`docs/studies/interface-language/` holds twelve companions and 94
images drawn from real components. It had no gate for most of the
sprint, and it showed: a report drifted four commits, a sheet
reproduced about once in six runs, and two sheets photographed the
home directory of whoever ran them.

`InterfaceEvidenceGateTest` closes that — one registry, generated
into scratch twice, failing on a missing registration, an unowned
artifact, stale text or a single changed pixel, each mutation-proved.
`SheetCapture` gives the twelve photographers one rule and refuses
rather than guesses.

## Remaining risks

**One unresolved intermittent.**
[#364](https://github.com/Aha43/JUranometria/issues/364): the export
companion produced two different artifacts from identical declared
inputs, once. The controls were correct; the cause and the layer are
unknown; 42 later runs were clean, which bounds the rate without
closing it. **Any recurrence before publication stops the release**,
and a failed gate is never retried into green.

**macOS remains unsigned.**
[#282](https://github.com/Aha43/JUranometria/issues/282) is blocked
on the external developer-program path. Nothing in this release
changes how the application is installed.

**Still not printed.**
[#293](https://github.com/Aha43/JUranometria/issues/293).

## What is owed

Four surfaces are outside this sprint's boundary, each a behaviour
change as much as a translation: the eighteen access letters and
Swing's mnemonic keys; the Chart Keyboard's stable-help and
transient-status split for a screen reader; what Place and Time says
when a reader types a latitude it cannot use; and finding a
constellation by name,
[#356](https://github.com/Aha43/JUranometria/issues/356).

## Recommended version

**2.1.0.** A coherent reader story — the atlas speaks Norwegian, and
lets a reader choose that twice — added without changing what any
existing page draws or invalidating anything a reader stored. Nothing
removed, no option changed meaning, no file format moved. Minor, not
major.
