# Development workflow

JUranometria combines two practices already proven in the Nam projects:

- NamDesktop supplies the simple, direct Java/Swing organization.
- NamWeb supplies issue-sized work, sprint milestones, pull requests,
  changelog discipline, and deliberate releases.

The workflow should remain lightweight enough for a personal project. Its
purpose is to preserve intent and make AI-assisted work reviewable, not to
imitate a large organization.

## Unit of work

Every implementation change starts with a GitHub issue. An issue describes:

- the observable result;
- why it belongs in the product;
- what is deliberately outside its scope;
- acceptance criteria;
- relevant design or data-source notes.

Work one issue at a time. Use a short descriptive branch, implement and verify
the issue, open a pull request, and merge it before beginning the next issue.
Follow-up discoveries become separate issues unless they are required for the
current acceptance criteria.

## Sprint milestones

A sprint is a coherent vertical step, represented by a GitHub milestone. It is
not a time box and does not need a deadline. Name milestones after the result,
for example:

```text
Sprint 1 — The first convincing chart
Sprint 2 — Move around the atlas
```

Before implementation, create the sprint milestone and attach its ordered
issues. Keep a sprint small enough that its completed result can be judged as a
whole. Close the milestone only after the integrated application has been run
and reviewed.

The Markdown sprint document explains the product arc. GitHub issues are the
live execution record. If they diverge, update the document or explicitly note
the changed decision.

## Branches, commits, and pull requests

1. Start from current `main`.
2. Create a branch such as `feature/gnomonic-projection` or
   `fix/east-left-orientation`.
3. Keep the branch limited to one issue.
4. Add or adjust focused tests alongside the implementation.
5. Run the application for visual changes and run the full test suite.
6. Add an entry under `Unreleased` in `CHANGELOG.md` for user-visible work.
7. Open a pull request that links the issue with `Closes #NN`.
8. Merge only after checks pass and the result has been reviewed. The
   check is the `test` GitHub Actions workflow
   (`.github/workflows/test.yml`): it bootstraps the pinned
   dependencies with `scripts/download-libs.sh` and runs `make test`
   on JDK 21 for every pull request to `main` - the same command a
   contributor runs locally.

Prefer a small number of meaningful commits over preserving every experiment.
Commit messages state the result and may include `Closes #NN` when the commit
itself completes the issue.

## Definition of done

An issue is complete when:

- its acceptance criteria are met;
- relevant focused tests pass;
- the complete test suite passes;
- visual behaviour has been exercised by running the application;
- source/data attribution is present where applicable;
- the changelog describes user-visible behaviour;
- no known regression or unexplained warning is being carried forward.

For a renderer issue, also save or inspect a deterministic reference image so
the chart can be judged rather than inferred from tests alone.

## Releases

Merging is not the same as releasing. Changes accumulate under `Unreleased`.
The natural release boundary is a completed sprint whose integrated result is
worth naming.

Use semantic versions:

- `0.MINOR.0` for a completed pre-1.0 sprint or coherent feature milestone;
- `0.MINOR.PATCH` for an off-cycle fix worth releasing independently;
- `1.0.0` only when the atlas is useful and its core behaviour is stable.

To release:

1. Review the complete change since the previous release.
2. Run all tests and exercise the packaged application.
3. Move the accumulated changelog entries into a dated version section.
4. Update `VERSION` in a release pull request.
5. Merge the release PR.
6. Create and push an annotated `vX.Y.Z` tag.
7. Publish a GitHub Release using the matching changelog section.
8. Close the sprint milestone if it is not already closed.

Automate GitHub Release creation from tags once the first release makes the
exact package artifact and release notes format concrete.

## Working with AI coding agents

The issue is the contract. Before editing, the agent reads the issue, relevant
project documents, and current code. During implementation it should prefer the
smallest direct design that meets the acceptance criteria. It reports what
changed, how it was verified, and any decision that should alter the project
documents.

An agent should not silently begin the next sprint issue. Finishing one issue
is a review point for the human owner, especially while the atlas's visual
language is being discovered.

## Native access

FlatLaf loads a small native library for platform window integration
(macOS full-window content, window decorations). From JDK 24, JEP 472
("Prepare to Restrict the Use of JNI") makes that a restricted call:
without explicit permission the JVM prints a four-line warning, and
under `--illegal-native-access=deny` FlatLaf logs SEVERE and its
platform integration degrades. The permission is therefore granted
deliberately in two places and must not be removed as noise:

- the `run` target passes `--enable-native-access=ALL-UNNAMED`
  (required for `-cp` launches, where the manifest attribute is
  ignored);
- the packaged jar carries `Enable-Native-Access: ALL-UNNAMED` in its
  manifest, honoured by `java -jar` even under `deny`.

JDK 21, the recorded minimum, accepts the flag and is unaffected.
Measured on JDK 26.0.1 with FlatLaf 3.4.1 (issue #82): the flag
silences the warning, and the manifest attribute keeps a plain
`java -jar` launch silent even under the stricter default.

