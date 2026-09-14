JUranometria — a quiet star atlas
=================================

Requirements
------------
A Java runtime, version 21 or later. Any vendor's JDK or JRE works
(for example Temurin from https://adoptium.net). Nothing else is
needed, and the application never touches the network.

Run
---
macOS / Linux:     ./juranometria
Windows:           juranometria.bat
Everywhere:        java -jar JUranometria.jar

Keep this directory together: the application JAR loads its
libraries from the lib/ directory beside it. The directory may live
anywhere, including paths containing spaces.

If it does not start
--------------------
"java: command not found" (or a Windows dialog asking what to open
the file with): no Java runtime is installed or it is not on the
PATH. Install Java 21 or later and try again.

"UnsupportedClassVersionError" or the launcher reporting an older
version: your Java is older than 21. Install Java 21 or later.

Licensing
---------
The application code is MIT licensed. The bundled star catalogue is
derived from the Tycho-2 Catalogue under CC BY-NC 3.0 IGO: THE
COMPLETE PACKAGE IS FOR NON-COMMERCIAL USE AND REDISTRIBUTION ONLY
for as long as that data is included. Full details: LICENSING.md in
this directory, the notices beside it, and Help > About in the
application.

Unsigned application note
-------------------------
The native application images are not code-signed or notarized.

On macOS, a browser-downloaded image is quarantined, and macOS may
refuse it with "JUranometria is damaged and can't be opened." It is
not damaged: check the archive against SHA256SUMS.txt. Gatekeeper
offers no Open Anyway approval for that dialog, so ordinary Finder
installation is not supported for this release (issue #282). After
verifying the checksum, clearing the quarantine flag with
"xattr -dr com.apple.quarantine" is a temporary workaround, not the
accepted route.

On Windows, SmartScreen may show "Windows protected your PC":
choose More info, then Run anyway. This is expected for an unsigned
open-source application.

This portable ZIP, run with your own Java, avoids both entirely.
