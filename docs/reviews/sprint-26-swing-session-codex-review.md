# Sprint 26 shared-state review — PR #248

Reviewed at `4054839`. The consolidation is well aimed and closes the historic
FlatLaf leak. Two cleanup contracts still need evidence before #224 is done.

## P1 — The exit probe does not prove its node left the backing store

`ExitProbeMain` installs a shutdown hook which calls `scratch.removeNode()` and
then `scratch.flush()`. A removed `Preferences` node cannot be flushed:
`flush()` throws `IllegalStateException`, which the hook catches and discards.
The subprocess test proves only that the process exited with zero. It never
observes, from the parent process, whether the uniquely named node survived.

This is the exact leak the change claims to close, and success is the path that
previously leaked it. Retain the parent node, remove the scratch child, and
settle the parent (or use another JDK-supported sequence that persists the
deletion). Have the probe report its unique node path and make the parent test
assert after process exit that the node does not exist. Prove the assertion
fails when shutdown cleanup is removed or moved after `System.exit`.

Apply the same persistence rule to `scratchPreferences`: its contract says the
node is removed whatever happens, but its current test observes only the same
process's in-memory preferences tree. Settle the parent after removal and test
the failure path. A test helper must not leave disk-backed debris that becomes
visible only to the next JVM.

## P2 — Restoration failure replaces the failure being investigated

The new wrappers use a plain `finally` whose restore may throw. If a body fails
and `Held.restore()`, `removeNode()`, or another restore also fails, the cleanup
exception replaces the original test failure. The suite then reports its
cleanup mechanism as the defect and discards the evidence it was cleaning up
after. `PackagedAcceptanceMain.withTemporaryOptions` already carries the
project's decided rule: preserve the primary failure and attach restoration
failure as suppressed.

Give the shared wrappers that same rule. Tests must cover body-only failure,
restore-only failure, and both together; in the last case the body failure
remains primary and the restore failure is present as suppressed. Do not
swallow restoration failure when the body succeeded.

## Re-review gate

Keep the zero-unprotected structural ratchet and the exact inherited-state
tests. Re-run the full display suite after the correction, since look-and-feel
restoration refreshes live windows. #243 remains held.

## Follow-up review at `194ace2`

The exit probe now proves deletion from its parent JVM after synchronising the
backing store, and shared wrapper failures preserve the primary exception.
One P1 remains from the original finding.

### P1 — `scratchPreferences` still proves only in-process removal

The original review required the same persistent-deletion rule for the shared
`scratchPreferences` helper. It still calls `node.removeNode()` without
settling the parent, and `aScratchNodeIsRemovedWhateverHappens...` still asks
`nodeExists` from the same JVM. Thus the exit probe is repaired, while the
helper now intended to own every scratch fixture retains the backing-store gap.

Capture the parent before removal, remove the child, and flush or sync the
parent through the same guarded cleanup. Test from a fresh process or otherwise
demonstrate backing-store visibility after both a successful body and a failing
body. Ensure a failure settling the deletion follows the newly established
suppression rule: primary when the body succeeded, suppressed when the body
already failed.

## Closing review at `f619a7c`

Persistent deletion is now proved from another JVM after both body outcomes.
One new P1 in the scanner must be closed before approval.

### P1 — The read-only witness exemption hides preference writes without `.node(`

The scanner now cancels a `Preferences.userRoot` touch whenever the file lacks
`.node(`. That is not equivalent to read-only access: the root is itself a
preference node, so `Preferences.userRoot().put(...)` or `.clear()` writes the
backing store without ever calling `.node(`. A write through an already-held
node can have the same shape. Such a file now classifies as touching nothing
and escapes the zero-unprotected ratchet.

Keep the general preference rule conservative and give `PrefsExistsProbe` a
narrow, pinned read-only-witness exemption, or detect the actual write APIs as
well as child-node creation. Add an adversarial fixture that writes directly
to `userRoot()` with no `.node(` and require it to classify as a preference
touch. Also prove the witness remains the only accepted read-only exception.
