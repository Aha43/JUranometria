# The road to trusted platform releases

Status: future direction, recorded after the first external-user report from a
published JUranometria release. This is not a promise that current archives are
signed.

## The report that changes the priority

A reader downloaded the Apple-silicon release with Firefox. macOS presented:

> “JUranometria” is damaged and can't be opened. You should move it to the Bin.

The reader built JUranometria from source successfully and found the application
useful. That comparison identifies a distribution problem rather than an atlas
problem: the published macOS application is neither signed with a Developer ID
nor notarised by Apple, while the locally built copy did not arrive carrying a
browser-download quarantine marker.

The word *damaged* is macOS's message, not a conclusion supported by this
report. Published checksums continue to answer archive integrity. The reader's
ordinary launch route nevertheless failed, and a release intended for general
readers must treat that as a real defect.

The present workaround is appropriate only for a reader who trusts the official
download and first verifies it against the release's `SHA256SUMS.txt`:

```sh
xattr -dr com.apple.quarantine /Applications/JUranometria.app
```

The path must be adjusted if the application is elsewhere. This bypasses
Gatekeeper for that application; it is documentation of today's limitation,
not the finished distribution experience.

## Order on the road

Printing and the modest extension beyond the current 36-degree field come
first. They answer the same reader's concrete club workflow: charts prepared
for an observing evening, presently made through a custom Python program. The
first readers approached through that community are technically able to use
the documented workaround, including the reader who supplied this report.

That temporary tolerance must not turn into a permanent release contract.
Trusted platform identity follows as dedicated distribution work. Keep the
problem recorded while the print and wider-field work proceeds.

## One shared gate, two implementation sprints

Signing is not one cross-platform switch. Begin with a shared distribution gate
that decides the common promises, then implement macOS and Windows separately.
The platforms have different identities, services, secrets, warnings and
verification routes; either must be releasable without waiting for the other.

The shared gate should decide:

- official applications open through the ordinary reader route without a
  bypass command;
- signing occurs only in the protected release workflow;
- private keys, certificates and service credentials never enter the
  repository, build output or ordinary pull-request jobs;
- release artifacts retain the current checksum, source provenance, packaged
  acceptance and licensing evidence;
- signatures are verified after packaging and again from the downloaded public
  release;
- the effect of signing and notarisation on reproducibility is measured and
  stated rather than assumed; and
- certificate renewal, revocation and an emergency unsigned-build policy are
  recorded before credentials make the process operational.

### macOS: a release Gatekeeper accepts

This sprint comes first because the project has a reproduced reader failure.
It should:

- establish the Apple developer identity and obtain a Developer ID Application
  certificate;
- sign the application and every nested executable in the form Apple requires;
- submit the completed application to Apple's notarisation service and require
  acceptance;
- staple the notarisation ticket to the distributed application;
- verify the signature, notarisation ticket and Gatekeeper assessment in the
  release workflow;
- exercise both Apple-silicon and Intel artifacts; and
- download the public archive through a route that applies quarantine, then
  prove an ordinary Finder launch does not reproduce the reported “damaged”
  dialog and needs neither `xattr` nor a Privacy & Security exception.

A correctly signed, notarised and stapled application is the intended repair
for this report. It does not merely replace the dialog with instructions for a
different bypass. The sprint is incomplete until the public, quarantined
artifact proves that route.

### Windows: a release with a verified publisher

Windows follows as its own decision and implementation sprint. It should:

- choose the certificate and signing service with their cost, automation,
  custody and renewal implications stated;
- sign every executable surface a reader launches;
- timestamp signatures so they remain verifiable after certificate expiry;
- verify the signature and certificate chain on Windows, including after the
  public artifact is downloaded;
- test the ordinary Explorer launch route; and
- describe SmartScreen honestly: a valid signature establishes publisher
  identity, while a new certificate may still lack reputation for a time.

The portable archive's identity is a separate claim. A plain JAR is not a
native application bundle and must not be described as if signing the native
launchers gives it the same Finder or Explorer identity.

## What remains unchanged

Signing and notarisation change trust at the operating-system boundary. They do
not change the chart, its catalogues, modules, preferences or offline nature.
They also do not replace the existing release evidence: checksums answer bytes,
source provenance answers origin, packaged acceptance answers behaviour, and
platform signatures answer publisher identity. A trusted release needs all of
them.
