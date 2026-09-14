JUranometria — a quiet star atlas (self-contained application)
==============================================================

This application includes its own Java runtime. Install nothing:
unpack this archive anywhere (paths with spaces are fine) and
launch:

macOS:    open JUranometria.app (double-click)
Windows:  JUranometria\JUranometria.exe
Linux:    JUranometria/bin/JUranometria

Unsigned application note
-------------------------
This build is not code-signed or notarized.

On macOS, a file fetched with a browser is quarantined, and macOS
may refuse a quarantined unsigned application with "JUranometria is
damaged and can't be opened." The application is not damaged and
this archive is not corrupt: check it against SHA256SUMS.txt and
the checksum will match. Gatekeeper's own Open Anyway approval is
not offered for that dialog, so ordinary Finder installation is not
supported for this release - see issue #282, which stays open until
the application is signed, notarized and stapled.

Until then, after verifying the checksum, you can clear the
quarantine flag yourself:

    xattr -dr com.apple.quarantine /Applications/JUranometria.app

That is a temporary workaround and not the accepted installation
route. The portable ZIP, run with your own Java 21+, avoids the
question entirely.

On Windows, SmartScreen may show "Windows protected your PC":
choose More info, then Run anyway. This is expected for an unsigned
open-source application.

Licensing
---------
The application code is MIT licensed. The bundled star catalogue is
derived from the Tycho-2 Catalogue under CC BY-NC 3.0 IGO: THE
COMPLETE PACKAGE IS FOR NON-COMMERCIAL USE AND REDISTRIBUTION ONLY
for as long as that data is included. The bundled Java runtime is
Eclipse Temurin (GPLv2 with the Classpath Exception); its complete
legal notices ship inside the application's runtime directory.
Full details: Help > About in the application and build-info.txt
beside this file.
