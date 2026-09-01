# AGENTS.md

Guidance for coding agents working in JUranometria.

## Product direction

Read `docs/product-vision.md`, `docs/chart-conventions.md`, and the active
GitHub issue before implementation. JUranometria is a quiet working atlas, not
a planetarium. Preserve its monochrome, timeless, offline-first character.

## Architecture

- Keep the application simple and old-style Java: Swing, Java2D, plain source
  folders, a Makefile, and direct `javac`/`java` commands.
- Use FlatLaf for application chrome and bundled Tabler SVG icons through
  FlatLaf Extras/JSVG, following `docs/application-appearance.md`.
- Do not introduce Maven, Gradle, dependency injection, or a UI framework.
- Keep application theme colors out of the chart renderer; the default atlas
  remains white paper and dark ink in both light and dark UI themes.
- Keep chart projection and rendering independent of Swing components.
- Painting must not fetch catalogue data or mutate application state.
- Use immutable values for coordinates, viewports, and chart descriptions.
- Run Swing work on the Event Dispatch Thread.
- Prefer focused domain and rendering tests over Swing UI tests.

## Workflow

- Work only from a GitHub issue and one issue at a time.
- Check the current branch before editing; implementation does not happen on
  `main`.
- Follow `docs/development.md` for sprint, pull-request, changelog, and release
  conventions.
- Run the full tests before committing.
- Run the application after every visual change and report what was inspected.
- Stop after completing the issue so the owner can review the result.

When automated Codex review is enabled, “stop” means open or update the pull
request and follow the bounded handoff in `CLAUDE.md`: address reviews tied to
the current head, but never merge, release, or continue after approval or a
human-decision result.

## Code Review Rules

### Claims need non-vacuous evidence

- Check the premise of every acceptance assertion before its outcome. Flag
  conditional assertions, assumptions, self-comparisons, direct state calls
  standing in for production input, and tests that can pass because the
  claimed event never happened.
- A green build is execution evidence, not proof that a test exercises the
  behavior its prose names.

### Preserve catalogue and chart honesty

- Never present substituted rendering values as recorded astronomical facts.
  Magnitude bands, unknown extents, unknown orientation, coordinate frame, and
  provenance must stay explicit.
- Selection, searched target, and navigation are distinct. Pure selection must
  not move or reassemble the chart; explicit navigation must remain observable.

### Protect process-wide and packaged behavior

- Swing look-and-feel, preferences, listeners, temporary files, and other
  global state must be restored even after setup or assertion failure.
- Claims about packaged, offline, cross-platform, accessibility, or release
  behavior need evidence through the shipped path, not an illustrative seam.

## Data

- Record source, version, license, and transformation notes beside bundled
  catalogue data.
- Never add a large generated catalogue or remote dependency without an issue
  that explains its need.
- Keep rendering usable when optional name-resolution services are unavailable.
