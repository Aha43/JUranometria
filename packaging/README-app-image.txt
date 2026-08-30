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
This build is not code-signed or notarized. On macOS, Gatekeeper
may block the first launch: right-click the app and choose Open, or
approve it under System Settings > Privacy & Security. On Windows,
SmartScreen may show "Windows protected your PC": choose More info,
then Run anyway. This is expected for an unsigned open-source
application.

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
