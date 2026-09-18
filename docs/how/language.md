# Language without translating the sky

JUranometria speaks to a reader in more than one language, but not everything
on a celestial chart is language. `M 42`, `ICRS J2000`, a right ascension and
an IAU abbreviation are astronomical identity or notation. *Orion* is a name
shown on the chart. *Centre here* is an instruction in the application. Those
three kinds of text have different owners and different fallback rules.

The language architecture exists to preserve those distinctions. Once the
boundaries are in place, another translation is principally resource work.
Building the first one is more involved because it exposes English that had
quietly become part of domain types, sentence assembly and layout assumptions.

## Four vocabularies

The application treats reader-visible words as four different things.

| Kind | Examples | Owner | May be translated? |
|---|---|---|---|
| Canonical identity and notation | `M 42`, `NGC 1976`, `ICRS J2000`, `34 Psc` | catalogue and domain data | No |
| Chart names | `Sagittarius`, `Skytten` | discovered sky-language pack | Yes, without changing identity |
| Interface prose | `Centre here`, descriptions and accessible names | discovered interface-language pack | Yes |
| Formatted values | `V 2.96`, `3h 58.0m`, `−13° 31′` | presentation code with an explicit notation policy | The surrounding prose may be; the stated value is stable |

This is why localisation is not a search-and-replace operation. Translating
`34 Psc` would corrupt a stellar designation. Leaving an accessible
description in English would leave part of the interface untranslated even if
every visible button looked correct.

## The path from resources to a reader

```text
packaged resources
  ├── interface-language/<tag>.manifest + <tag>.properties
  └── sky-language/<tag>.manifest + <tag>.tsv
             │
             ▼
generated, sorted resource indexes
             │
             ▼
InterfaceLanguages             SkyNames
             │                    │
             └──── available languages ────┐
                                            ▼
                                  SkyLanguageChoice
                                  SkyLanguageStore
                                  SkyLanguageSession
                                            │
                         ┌──────────────────┴──────────────────┐
                         ▼                                     ▼
                 InterfaceText                        chart name map
                         │                                     │
              visible and spoken UI                 SceneAssembler
                                                               │
                                                               ▼
                                                    screen and export
```

The two resource trees are deliberately independent. Installing Norwegian
constellation names does not claim that Norwegian interface prose exists, and
installing an interface descriptor does not create a chart-name pack.

There is no Java list of supported languages. The build discovers validated
resources, refuses incomplete or duplicate packs, and writes sorted indexes
for the packaged classpath. A language is offered because its resource ships.
The same discovery runs inside the packaged application, where source
directories do not exist.

## Identity is the key; language supplies a value

The chart is assembled in canonical constellation order. A sky-language pack
may replace the displayed value for each identity, but it may not replace the
key or determine iteration order.

```text
canonical key: Sgr
Latin value:   Sagittarius
nb-NO value:   Skytten
```

Sorting by the displayed value would make geometry, placement and possibly
which label wins depend on language. Instead, `SkyNames` resolves a complete
identity-keyed map before scene assembly. The renderer receives names; it does
not receive a language switch and performs no fallback of its own.

Fallback is equally explicit:

- unavailable interface prose falls back to English;
- an unavailable localised chart name falls back to official Latin;
- canonical identity never falls back, because it was never translated.

The special chart choice **Follow interface** therefore follows only when a
chart pack answers to the selected interface language. Otherwise it resolves
to Latin. The stored choice remains distinct from that current resolution.

## Preferences become session state

`SkyLanguageChoice` defines the persisted tokens and their migration rules.
`SkyLanguageStore` reads and writes them. `SkyLanguageSession` holds the
resolved choice used by the running application.

That separation preserves several behaviours:

- absence still means that the reader has never made a choice;
- reading preferences does not write a migration;
- malformed or unavailable values resolve deterministically;
- Reset View changes the view, not language preferences;
- a language change affects the next assembled page without a global
  “current language” singleton;
- screen and export ask the same chart session, so paper says what the reader
  saw on screen.

The interface language and chart language remain separate members of that
state. English application chrome with Norwegian constellation names is a
normal configuration, not an exception.

## Domain types carry meaning, not English

A domain or rendering type may identify a concept, but it must not decide how
that concept is described to a reader. For example, a symbol-family enum may
own symbol geometry, a mnemonic and canonical identity. Its label and prose
belong in the presentation layer.

```text
domain value ──► presentation service ──► InterfaceText ──► reader
```

The rule has been applied twice, to a symbol-family enum and to the enum
saying why a page does or does not draw an object; both had accumulated a
label and a sentence per constant. It prevents a renderer, catalogue object or
module from becoming an unofficial English resource bundle. It also makes the same domain value usable
by the chart, Inspector, settings, evidence tools and future modules without
carrying UI grammar into each caller.

Canonical strings remain available for diagnostics, comparisons and evidence.
Architecture tests keep them away from reader sinks such as Swing labels,
tooltips, accessible descriptions and window titles.

## Translate complete thoughts

Production code must not build grammar from translated fragments. This is
wrong:

```text
"Needs " + label.toLowerCase() + " on."
```

It assumes English word order, English casing and an English way to express a
dependency. Each language instead owns a complete pattern:

```properties
# English
chartoptions.switch.spoken.depends = {0} Switch it here or from the chart with {1}. Requires {2} to be on.

# Norsk bokmål
chartoptions.switch.spoken.depends = {0} Slå det av eller på her eller fra kartet med {1}. {2} må være slått på.
```

Note where `{2}` falls. English needs it after a verb; Norwegian needs it
first, before `må være slått på`. Neither order can be produced by
concatenating a translated fragment onto an English sentence, which is what
the rejected form above does.

The arguments are stable values or separately translated terms. A key
sequence remains a key sequence, while the connector between its strokes is
language. Catalogue identities remain catalogue identities.

`InterfaceText` validates patterns when they are formatted. A pattern that
asks for `{2}` when its caller supplied two arguments is refused with the key
and counts named. Synthetic fixtures exercise valid data shapes that the
current catalogue happens not to contain; otherwise a valid but dormant
pattern could remain broken indefinitely.

## Visible text is only one channel

A Swing control may have several reader surfaces:

- its visible label;
- a tooltip;
- an accessible name;
- an accessible description;
- text spoken only when a window or control receives focus.

All of them belong to the interface language. Companion reports inventory the
channels separately so a visible translation cannot conceal English still
spoken by a screen reader.

Layout is part of the same contract. Norwegian may be longer than English, so
paired surfaces are rendered side by side in real packed windows, from the
compiled application rather than from a detached panel. A window is necessary
because a surface that measures itself — a table sizing a column from its own
words, a label breaking a sentence against font metrics — reports widths
nothing honours until it has been laid out. That the same resources are
present in the shipped image is a separate question, answered by packaged
acceptance. Sentences may wrap; coordinates, designations and magnitudes must not
break into shapes that look like separate values. The accessible text keeps
the complete unwrapped sentence because visual line breaks are not grammar.

## Why packaged and visual checks are necessary

A properties file can show that a key exists. It cannot establish that:

- resource discovery works from the shipped classpath;
- a language appears in the selector;
- the chosen language survives restart;
- chart identity and ordering remain unchanged;
- Norwegian text fits its control;
- screen and export resolve the same names;
- accessibility receives the complete sentence;
- a screenshot inherited the intended Swing look and feel.

JUranometria therefore checks language at several levels: focused unit
contracts, application journeys in fresh JVMs, packaged acceptance, paired
inspection sheets and the evidence contract. Tests assert their premise before
their conclusion—for example, a language comparison first proves that the page
actually contains names. Two empty maps agreeing would otherwise look like a
successful localisation test.

The visual checkpoint is an owner judgement, not a replacement for tests. It
answers whether the language reads naturally and whether the application still
has the quiet hierarchy intended by the atlas.

## Adding another interface language

Once the surface is externalised, a further interface language should require
no application-language switch:

1. Add a manifest and a properties resource under
   `src/resources/interface-language/`.
2. Translate complete patterns while preserving canonical identities and
   notation.
3. Let the build discover and index the descriptor.
4. Exercise the language in focused contracts and a fresh JVM.
5. Inspect paired packaged surfaces, including tooltip and accessibility-only
   text.
6. Keep the pack in draft status until its application surfaces have been
   reviewed in context.

Adding chart names follows the separate `sky-language` pack contract. The two
can ship independently.

## Related records

- [Technical architecture](../architecture.md)
- [Sky-language decision](../decisions/sky-language.md)
- [Test-evidence decision](../decisions/test-evidence.md)
- [Control explanations](../decisions/control-explanations.md)
- [Application appearance](../application-appearance.md)
