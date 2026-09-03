# Development arches

JUranometria can become feature-rich without turning the chart into a general
astronomy application. This roadmap records the directions already worth
protecting. It does not assign sprint numbers, promise releases, or settle
questions that need a design gate and measured evidence.

Two principles govern every arch:

1. **Feature richness comes through removable modules.** The chart publishes
   small, typed services. A module owns its domain and can be removed without
   leaving the chart incomplete or changed.
2. **A module must still be true to celestial cartography.** Modularity is not
   permission to add any astronomy feature. The fixed chart remains the
   instrument; animation, automatic attention, hidden frame changes, and
   domain-specific rendering policy remain outside it.

## Current horizon

| status | direction | next possible stone |
|---|---|---|
| next candidate | maintenance and dogfooding | small demonstrated usability fixes; consolidate Swing session-state tests in #224 |
| beyond the next sprint | [the celestial machine](directions/celestial-machine.md) | study a J2000 ecliptic and an optional astronomical zodiac belt before planetary positions |
| ready for a future gate | [reading the chart](directions/chart-reading.md) | constellation membership from the B1875 boundary geography |
| established foundation, later | [reader work](directions/reader-work.md) | decide whether persistent observing notes earn a separate local data model |
| continuous | [application trust](directions/application-trust.md) | local diagnostics and maintenance without telemetry |
| parked | Milky Way layer | cartography studied in Sprint 22; redistribution rights remain undocumented, so no data ships |

“Next candidate” is deliberately weaker than “scheduled.” Dogfooding can add
or remove maintenance work before a sprint is opened. A direction becomes a
commitment only when an issue defines its gate or acceptance criteria.

## How to use this roadmap

- Put durable intent and guardrails here or in a linked direction document.
- Put measured choices in `docs/decisions/` only after a gate decides them.
- Put executable work in GitHub issues only when it is ready to schedule.
- Keep sprint handovers as history and evidence; do not make readers reconstruct
  the future from twenty-five closing reports.
- Revisit an arch when completed work changes its premises. A direction may be
  narrowed, parked, or rejected without being treated as a broken promise.
