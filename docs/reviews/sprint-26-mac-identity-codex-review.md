# Sprint 26 macOS identity review

Reviewed PR #251 at `037c433`.

The seven captures were inspected at native size. They support the decision:
the published native application carries the Andromeda mark in Finder before
launch, the Dock, the application switcher, `/Applications`, and a Desktop
alias; the current portable `java -jar` route shows Duke. The candidate native
image also carries the mark.

The packaging check closes the mechanical gap correctly. It compares the
exact installed `Contents/Resources/JUranometria.icns` with the reviewed
container, then requires `CFBundleIconFile` to resolve to that same basename.
The captured-evidence registry rejects missing, changed, and unpinned captures.

## P2 — Describe the observed portable route without contradicting the recorded follow-up

The README, changelog, and decision repeatedly generalise the observation to
macOS's treatment of “any bare JAR” and call it an operating-system limitation.
The same decision later records that `java.awt.Taskbar.setIconImage` could
brand the JAR process's Dock tile. Both cannot be true as written: a raw JAR
does not acquire native bundle/Finder identity, but a running unbundled Java
process can choose a Dock image through that runtime API where supported.

State the measured claim narrowly: JUranometria's current portable launcher
executes `java -jar` without runtime Taskbar-icon integration, so its running
Dock tile shows Java's generic icon; the `.app` is the fully branded macOS
route. This preserves the issue's required decision and keeps the possible
runtime follow-up honest without implying it is impossible.

There is also a concrete cross-reference error in
`docs/decisions/mac-identity.md`: the portable launcher is said to be
mechanically the same as “the fourth row,” but the JAR/Dock observation is the
sixth row of the route matrix. Name the row by route instead of number so an
insert cannot make it stale again.

## Verdict

Changes requested on this P2 wording correction. The visual evidence,
captured-evidence contract, decision branch, and bundle verification otherwise
pass review.
