# Direction: application trust

## Purpose

Keep the atlas dependable across long use, rare failures, four packaged
platforms, and future modules. This arch is continuous maintenance rather than
a destination release.

The existing foundation is unusually strong: deterministic chart evidence,
display-backed CI, native packaged acceptance, source-commit provenance,
checksum verification, duplicate-delivery safety, and one shutdown path that
flushes reader choices before leaving.

## Near work

- Consolidate tests that temporarily change Swing look and feel or font state
  behind the shared restoration discipline (#224), then add a structural guard
  against new unguarded borrowers.
- Let dogfooding supply small usability corrections rather than manufacturing a
  maintenance backlog in advance.
- Keep display-backed tests required and distinguish desktop refusal from a
  passing keyboard or pointer claim.

## Possible diagnostic logging

If rare failures justify it, JUranometria may gain a small local diagnostic
system. It is not telemetry and not an observing log.

A gate should decide:

- the exact events that help diagnose failure: version, platform, packaged
  runtime, startup, catalogue verification, failed operations, and bounded
  state transitions rather than a transcript of reading;
- whether logging is always minimal, enabled for one session, or produced only
  by an explicit **Save diagnostic report** action;
- rotation, size bounds, deletion, redaction, and a human-readable format;
- how precise location, entered instants, searches, notes, and filesystem paths
  are excluded or included only with explicit informed action; and
- packaged evidence that nothing is transmitted and no unbounded file grows.

## Guardrails

- No telemetry, analytics, crash upload, account, or network destination.
- A diagnostic record must not quietly become a history of what the reader
  searched for or where they observed.
- Failures remain visible to the reader; logging is evidence, not a substitute
  for an explanation.
- Global state and temporary resources are restored even when setup or an
  assertion fails.
- Cross-platform claims continue through the shipped artifacts, not only the
  portable development runtime.
