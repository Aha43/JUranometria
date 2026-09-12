# Whether the modules' lines stop where the sky does

The observer's lines and the ecliptic, drawn on a hemisphere. Ink beyond the limb
is sky drawn where there is none, whatever computed it.

centred on       centre                            
zenith-overhead  RA 151.9, Dec +60.0               
horizon-south    RA 151.9, Dec -30.0               
ecliptic-high    RA 90.0, Dec +23.4                
sagittarius      RA 266.0, Dec -28.0               

How much each of those pages inks inside the limb and beyond it, and where
each module's name landed, is in docs/studies/globe-modules/platform.md.

The sky's own labels cannot be moved by module ink: the renderer builds its obstacles
from the scene and the options alone - textObstacles is never offered a module's
contribution - so reference lines are drawn beneath text placed without knowing
they exist.
