# Issue #220 settled-scene review

**PR #239, reviewed at `e7ee053`.** The diagnostics have identified a test
race, not a production selection defect. Keeping scene reads on the EDT is
correct, and the deterministic recenter is much better evidence than waiting
for native resize timing. One P1 remains in the repair.

## P1 — Derivation and click are serialized, but not atomic

`marks()` reads the scene and derives marks in one `invokeAndWait`, then
returns to the test thread. `clickOn()` later performs another
`invokeAndWait` to read the offset, followed by further EDT turns to dispatch
the press and release. An arbitrary queued resize, recenter, or layout event
can run between any of those turns and replace the scene. Serialization makes
each individual operation safe; it does not make the sequence adjacent.

The new regression queues its recenter **before** mark derivation. It therefore
proves that the fixed derivation waits for work already on the EDT, and kills
the old off-EDT read. It does not queue a page change between derivation and
click, which is the remaining gap in the stated contract that the mark and
click resolve against one settled scene.

For the click-under-test, perform scene read, mark selection, page-offset read,
and mouse press/release in one EDT task, returning the selected mark only for
the assertions that follow. Keep the diagnostic snapshot from that same task.
Add the complementary deterministic case: arrange for a page change to be
queued after a non-atomic derivation would finish but before its separate click
turn; the atomic helper must either click before that change in its one turn or
derive after it, never combine the two pages. Mutation back to separate EDT
turns must fail.

Do not forbid later legitimate relayout and do not change production code.
This remains a journey-test repair. PR #238 should continue waiting for this PR
to merge and be incorporated.
