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
