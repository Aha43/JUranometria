# Direction: reader work

## Purpose

Allow a reader to do work with the atlas while keeping their attention,
temporary marks, and durable records distinct from the sky catalogue.

The current foundation is deliberately temporary. Selection is the thing being
considered now. Working marks are a set used while exploring one page. They are
pruned as the page changes and are never persisted.

## Possible later stone: observing notes

A persistent notes or observing-log layer may eventually earn a place. It is
not an extension of working marks. It would be reader-owned data with a
separate lifecycle and a visible act of creation.

Before implementation, a gate must decide:

- whether the first useful thing is a note attached to a catalogue identity, a
  sky position, an observing session, or some combination;
- which timestamps mean observation time, creation time, and modification
  time, and which are optional;
- a durable local format, schema/version migration, backup, import, and export;
- what happens when catalogue identities change or a note refers to empty sky;
- how notes appear without becoming permanent clutter on every chart;
- privacy consequences of storing precise places and times; and
- whether attachments, equipment records, planning, and scheduling are
  rejected rather than allowed to expand the first model.

## Selection events

Future modules may need to observe selection. That should remain a small,
typed subscription to the existing selection state, with initial-state
delivery and safe detachment. A subscriber must not gain ownership of
selection, navigation, or rendering as a side effect.

## Guardrails

- Selection is attention, working marks are temporary, and notes are durable;
  never collapse them into one state.
- Reader data never becomes catalogue truth.
- Persistence is local, explicit, inspectable, and exportable; no account or
  synchronization requirement follows from notes.
- Nothing records a reader's actions merely because the application can.
- Removing the notes module leaves the chart and catalogue complete.
