# Codex review: JUranometria 1.8.0 release candidate

Reviewed PR #270 at `c55753e` against the approved Sprint 27 handover and the
release metadata contract. `VERSION`, the dated changelog section and the
requested tag agree, and 1.8.0 is the appropriate minor version. One release
blocker and two changelog corrections remain.

## [P1] Publishing 1.8.0 would leave the public gallery downloading 1.7.0

Sprint 27's closing acceptance required the public gallery's current-release
link, and the handover correctly records that it pointed to `v1.7.0` while
1.7.0 was current. This release changes what “current” means, but
`docs/gallery/manifest.json` and every generated gallery page still link and
label the download as release 1.7.0. Merging and tagging this candidate would
therefore publish 1.8.0 while the new gallery continues directing readers to
the previous application.

Advance only the manifest's downloads target/label to the immutable
`releases/tag/v1.8.0` and regenerate the gallery pages from that source of
truth. Keep each slide's `produced` provenance at the actual v1.7.0 tree; the
images did come from that reviewed tree and a release-link update must not
rewrite their history. Run the gallery/evidence contracts and require every
generated page to agree with the manifest. Because the Pages workflow watches
these inputs, merging the candidate will deploy the updated links; verify the
public link after the tag exists.

## [P2] The #260 changelog entry calls a stateful page view stateless

The foundation entry says `WorkingMarksModel` was narrowed to a “stateless
one-way adapter.” The #260 review changed that design precisely because the
adapter needed presentation scope, one model subscription, delivered state
and a serialized view queue. It owns no second *membership truth*, but it is
not stateless. Retell it as a page-scoped compatibility view over the one
session model, explicitly distinguishing its presentation state from
membership, so the public release history records the reviewed architecture
rather than the first draft.

## [P2] The Pages permission count omits the build permission

The new #253 entry says the workflow holds “only the two permissions Pages
needs.” The workflow has three explicit permissions across its jobs:
`contents: read` for build, then `pages: write` and `id-token: write` for
deployment. The underlying least-privilege claim is correct; the count is not.
Use the precise wording already established in the workflow and handover:
contents-read for the build, pages-write and OIDC only for deploy.

Keep PR #270 unmerged and do not create `v1.8.0` until the gallery link and
release notes agree with the release they publish.

## Approval

Approved at `a6acb06`. The manifest now names the immutable v1.8.0 release and
all ten generated gallery pages carry the same link and label, while every
slide retains the v1.7.0 tree as its actual production provenance. The #260
entry now describes `WorkingMarksModel` as a page-scoped compatibility view
whose state is presentation rather than membership, and the #253 entry names
all three explicit workflow permissions in their proper jobs. Release
metadata agreement still holds. No findings remain for the 1.8.0 candidate;
after tagging, verify that the public gallery's release link resolves.
