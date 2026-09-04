# Codex review: Sprint 26 close

Reviewed PR #255 at `52b7494` against issue #244 and the approved Sprint 26 decisions.

The close supplies the required integration evidence and keeps its release boundary. The clean-checkout and incomplete-evidence cases are distinguished honestly, the suite and packaged matrix are counted, the public black-sky route is demonstrated, and the transient mixed capture is recorded as an observation without being promoted into a product diagnosis. The 1.7.0 recommendation fits the project’s versioning contract.

## [P2] Do not restore the portable-JAR overclaim corrected in #245

The handover twice describes Duke as a property of a “bundle-less launch”; the residual-risk entry strengthens that to an **“inherent property of a bundle-less launch.”** That is the claim PR #251’s review deliberately removed. Finder cannot brand a JAR as an application bundle, but the running Dock tile is a property of today’s plain `java -jar` launcher because it sets no Taskbar/Dock image of its own; `Taskbar.setIconImage` remains a possible change precisely because the Dock result is not inherent.

Carry the approved narrow wording into both the reader-facing summary and residual-risk entry. Otherwise Sprint 26’s final account contradicts its own #245 correction record a few paragraphs earlier.

## [P2] Keep the timing conclusion non-causal

The timing section correctly says its small samples cannot establish causation, then concludes that elapsed time “has grown only with the work performed.” “Only with” assigns the cause those samples cannot establish. The measurements show that time grew **alongside** a larger suite doing additional full-page work; they do not partition runner variance, compilation effects, or the cost of individual additions.

Use the same careful formulation as the table: elapsed time remains modest and grew alongside the additional evidence, while the measured source of costly friction was diagnosis of untrustworthy failures rather than the gate’s wall time.

## Result

Two P2 documentation corrections remain. No code or release-process finding.
