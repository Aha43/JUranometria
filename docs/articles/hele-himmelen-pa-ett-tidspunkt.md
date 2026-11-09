# Hele himmelen på ett tidspunkt

*Hvorfor én side i atlaset viser hele himmelen ved kartets tid og sted,
hvorfor den er en ellipse med ekliptikken tvers over midten, hvordan
horisonten, sømmen og rutenettet skal leses, hvordan du går fra den og
inn i et vanlig kart, og hva den ikke påstår*

## Hvor er alt sammen, akkurat nå?

En vanlig side i atlaset svarer på et lokalt spørsmål: hva ligger rundt
denne stjernen, denne planeten, dette feltet på himmelen. Noen spørsmål
er ikke lokale. Hvor er Solen, Månen og alle planetene i dette
øyeblikket? Hvilke av dem er oppe, og hvilke er under føttene mine? Er
planeten jeg leter etter, i nærheten av den jeg ser på?

Ingen vanlig side kan svare på det, og grunnen ble målt før noe ble
bygget. Ved tre karttider som atlaset selv søkte fram, rommer den best
plasserte oversikten på 120° **5-6 av de ni legemene** og den beste
halvkulen på himmelgloben sju; alle ni får plass på en globe bare når
planetene tilfeldigvis samler seg på én side av himmelen.

JUranometria 6.5, utgaven som følger etter 6.4, får en side som rommer
**9 av 9** hver gang. Én bryter, *Hele himmelen*, i verktøylinjen, i
Kontrolleren og i Kart-menyen, bytter kartet ut med hele himmelen ved
Sted og tid-øyeblikket, som ett avgrenset kart. Denne artikkelen handler
om hvordan det kartet skal leses, hvorfor det har den formen det har, og
hvordan du kommer fra det og tilbake til et vanlig kart. Den beskriver
atlaset slik det er bygget for 6.5; den er ikke den utgavens
utgivelsesnotat.

Atlasets egen redegjørelse for beslutningene står i
`docs/decisions/all-sky-projection.md`; denne artikkelen forklarer den,
den erstatter den ikke.

## Én kveld, ni legemer

Siden nedenfor er Oslo (**59.91 N, 10.75 E**) **2026-12-15** klokken
**21:00** UTC, en kveld atlaset fant ved å søke gjennom et år med netter
etter den med mest å se: **seks av de åtte legemene utenom Solen** står
over horisonten i mørke.

[![Hele himmelen som en bred ellipse på hvitt papir: stjerner som prikker
med stjernebildefigurer og navn, en rett strekpunktlinje tvers over
midten med planetene langs den, en stiplet kurve som svinger ned rundt
midten av kartet, og alt utenfor den kurven tegnet i
grått.](images/whole-sky-evening-en-white.png)](images/whole-sky-evening-en-white.png)

*Hele himmelen fra Oslo, 2026-12-15 klokken 21:00 UTC. Den rette linjen
tvers over midten er ekliptikken; den stiplede kurven er horisonten, og
det som ligger utenfor den, dempet tegnet, er under den. Hver figur i
denne artikkelen åpnes i full størrelse når du velger den.*

Alt det vanlige kartet tegner, er her, tegnet med det samme blekket:
stjerner til **V 5.0**, stjernebildefigurene og navnene deres,
dyphimmelsymbolene, rutenettet, ekliptikken, horisonten med
himmelretningene og senit. Utenfor ellipsen er det ingenting. Siden sier
det selv når den leses opp: *utenfor det elliptiske omrisset er det
papir, ikke himmel*.

Solsystemlegemene på siden er de du har slått på, verken flere eller
færre. Hver figur i denne artikkelen har alle ni på.

Les midtlinjen fra venstre mot høyre, og hvert legeme er gjort rede for.
Første kolonne er legemets ekliptiske lengde fra kartets sentralmeridian,
positiv mot venstre; den andre er den ekliptiske bredden, høyden over
eller under midtlinjen; den tredje er høyden over Oslos horisont. Alt er
i grader.

| legeme | fra sentralmeridianen | ekliptisk bredde | høyde | |
|---|---|---|---|---|
| Venus | +153.2 | +2.9 | -40.8 | under horisonten |
| Mars | +91.4 | +2.7 | +0.7 | |
| Jupiter | +81.7 | +0.9 | +6.9 | |
| Uranus | -2.5 | -0.1 | +50.0 | |
| Saturn | -57.4 | -2.5 | +22.7 | |
| Neptun | -63.7 | -1.4 | +18.9 | |
| Månen | -87.7 | +0.8 | +2.3 | |
| Solen | -161.4 | +0.0 | -46.7 | under horisonten |
| Merkur | -170.8 | -0.2 | -49.5 | under horisonten |

Uranus står nesten på sentralmeridianen, høyt på himmelen. Mars og
Jupiter står lavt i øst, til venstre; Månen står lavt i vest, til høyre.
Solen og Merkur er langt rundt mot høyre, og Venus langt rundt mot
venstre, alle tre dypt under horisonten.

## Ingen flat kartside bevarer alt

En kule kan ikke flates ut uten at noe gis opp, og atlasets første
artikkel, [From the celestial sphere to a
page](from-celestial-sphere-to-page.md) (på engelsk), handler om de tre
byttehandlene de vanlige sidene gjør. Ingen av de tre kan vise hele
himmelen. Detaljkartets projeksjon løper mot det uendelige en kvart
omgang ute; oversiktens utelater punktet rett overfor sentrum og vokser
uten grense inn mot det; globen viser én halvkule og ikke mer.

Hele himmelen tegnes i en fjerde projeksjon, **Hammer**. Konstruksjonen
er kort. Ta et arealriktig kart over én halvkule, en skive. Halver hver
lengdegrad, slik at hele kulen presses inn i den skiven; strekk så
skiven til dobbel bredde. Pressingen halverer hvert areal og strekket
dobler det igjen, så resultatet bevarer arealer nøyaktig, og hele
himmelen havner innenfor en ellipse som er dobbelt så bred som den er
høy. Kartets to poler er punkter, øverst og nederst. Venstre og høyre
kant er én og samme halvsirkel på himmelen, klippet opp: sømmen.

Det kartet bevarer, er **areal**. Et stjernebilde dekker like stor del
av siden uansett hvor det havner, stjernene står like tett på siden som
på himmelen, og den delen av himmelen som er under horisonten, er
**nøyaktig halvparten av kartets areal**, på hver side.

Det kartet gir opp, er **form**, mer og mer med avstanden fra sentrum.
Målet som brukes her, er det meste kartet kan endre en vinkel på et
sted. Langs midtlinjen er det ingenting i sentrum, **9.1°** en kvart
omgang ute, og **34.4°** ved 170° fra sentralmeridianen. Utenfor
midtlinjen vokser det raskere: ved 45° bredde og 170° lengde er det
**80.9°**. Nær sentrum ligner et stjernebilde seg selv; nær endene av
sømmen er det skjøvet nesten til det ugjenkjennelige. Det er prisen for
å se alt på én gang, og det er derfor siden tilbyr en vei tilbake til et
vanlig kart heller enn en måte å zoome på.

## Hvorfor ekliptikken ligger flatt

Et kart over hele himmelen må velge hva midtlinjen skal være. Atlaset
valgte **ekliptikken**, Solens årlige bane, fordi det er der legemene
er: på hver målte side ligger Solen, Månen og planetene **innenfor ±5
grader** fra den. På dette kartet er ekliptikken én rett linje gjennom
den mildeste delen av projeksjonen, og «følg ekliptikken for å finne
hvert legeme» er noe øyet kan gjøre.

Prisen er at kartets «opp» ikke er det vante. Opp er **ekliptisk nord**,
ekliptikkens pol i Dragen, øverst i ellipsen. Himmelens nord, polen ved
Polaris, er et annet sted: selv i kartets sentrum heller den fra sidens
opp med så mye som **23.4°**, og mot kantene dreier himmelens nord på
stedet så langt som **158°** fra den. Derfor sier siden aldri
*himmelens nord opp*. Tittelen sier det som er sant:

> Hele himmelen · Hammer · ekliptisk nord opp · ekliptisk lengde øker mot venstre i midten

Det siste leddet er kartets dreieretning. Som hver side i atlaset viser
det himmelen sett fra innsiden av kulen, så ekliptisk lengde, retningen
Solen vandrer gjennom året, øker mot venstre. Det sies å gjelde *i
midten* fordi det er der det er målt å gjelde; nær kantene av en ellipse
er «venstre» ikke lenger en retning på himmelen.

## Hva rutenettet forteller

Rutenettet på denne siden er fortsatt det ekvatoriale, rektascensjon og
deklinasjon, tegnet på et kart bygget rundt ekliptikken. Det misforholdet
er det mest lærerike på siden. Løft det fram med *Fremheving* ▸
*Ekvatorialt rutenett*, og projeksjonen viser seg selv.

[![Kveldens kart over hele himmelen med det ekvatoriale rutenettet
tegnet i kraftige blå linjer: meridianene stråler ut fra et punkt ved
siden av Polaris øverst i midten, sirklene med høy deklinasjon er
lukkede sløyfer rundt det punktet, og de lavere sirklene løper tvers
over ellipsen som lange
bølger.](images/whole-sky-evening-en-white-grid.png)](images/whole-sky-evening-en-white-grid.png)

*Den samme kvelden med det ekvatoriale rutenettet løftet fram med
Fremheving. Ingenting annet ved siden er endret; rutenettet viser hva
projeksjonen gjør.*

Rektascensjonsmeridianene møtes ikke øverst på kartet. De samles i
himmelpolen ved siden av Polaris, godt under toppen og som regel til én
side for sentralmeridianen. Sirklene med høy deklinasjon lukker seg
rundt det punktet; de som ligger nærmere himmelekvator, løper tvers over
siden som lange bølger, og ekvator selv krysser den rette ekliptikken i
de to jevndøgnspunktene. Nær sentrum har rutene i nettet nesten den
formen de har på himmelen; mot sømmen heller de og blir lange. Men fordi
kartet bevarer arealer, dekker hver rute mellom de samme to
deklinasjonssirklene like stort areal på papiret, uansett hvordan den er
bøyd. Form er det rutenettet mister på vei mot kanten, aldri størrelse.

Rutenettet er sparsomt nummerert, og bare der et tall får plass i sin
helhet. Rektascensjon skrives på papiret utenfor ellipsen der en
meridian møter omrisset, noe som bare skjer ved sømmen, så hver time som
når den, står ved begge kanter. Deklinasjon skrives inne på himmelen der
en deklinasjonssirkel krysser sentralmeridianen.

## Der du står

Stjernene sitter på dette kartet etter sine ekliptiske koordinater, som
ingen observatør endrer. Det stedet og tiden din bestemmer, ved siden av
hvor Solen, Månen og planetene er, er hvilken lengde som ligger i
midten, og hvor horisonten går.

**Sentralmeridianen følger ditt senit.** Lengden som er tegnet ned
gjennom midten av kartet, er den ekliptiske lengden til punktet rett
over deg, og tittelens andre linje sier hvilken det er:
*Sentralmeridian: senits ekliptiske lengde 65.0° · Stjerner til V 5.0*.
Endre tiden eller stedet, og hele himmelen glir sidelengs under en ny
sentralmeridian. Senit ligger på den meridianen, men det er ikke kartets
sentrum: det beholder sin egen ekliptiske bredde, og fra Oslo legger det
senit godt over midtlinjen: **39.82°** fra sentrum denne kvelden, og
**37.7-68.6 grader** fra det på de tre målte sidene. Kartet er rettet
inn etter observatøren; det er ikke et bilde av himmelen med senit i
midten.

**Horisonten er én lang kurve.** Når den matematiske horisonten vises i
Sted og tid, tegner siden den stiplet, med de fire himmelretningene. På
himmelen er den en storsirkel; på dette kartet er den en dyp skål eller
en lang bølge som går fra den ene kanten til den andre, og himmelen over
den er den halvdelen som inneholder senit. Med *Demp under horisonten*
på tegnes alt på den andre siden med redusert kontrast, hver stjerne,
hvert navn og hvert legeme etter sin egen posisjon, og hver linje delt
der den krysser horisonten. På kveldssiden avviker det dempede området
fra den sanne horisonten på **0.070%** av himmelen.

[![Det samme kartet over hele himmelen på svart bunn med sidens ord på
norsk: hvite stjerner og figurer inne i den stiplede horisontens skål,
grå utenfor den, planetene langs den rette
midtlinjen.](images/whole-sky-evening-nb-black.png)](images/whole-sky-evening-nb-black.png)

*Den samme kvelden på svart himmel, på norsk. Geometrien er den samme;
bare bunnen og ordene er endret.*

Et legeme under horisonten sier det i navnet sitt: *Venus (under
horisonten)*. Det lengre navnet trenger mer plass. På maskinen som
tegnet disse figurene, plasserer kveldssiden **sju av de ni navnene**,
og Merkurs og Solens, nær høyre kant, avvises; merkene er tegnet, kan
velges og leses opp. Med demping av er ingenting dempet, og **alle ni
er navngitt**.

[![Det samme kartet over hele himmelen uten noe tegnet i grått: hver
stjerne, figur og hvert navn med full kontrast på begge sider av den
stiplede horisonten, og Merkur og Solen navngitt ved høyre
kant.](images/whole-sky-evening-en-white-subdue-off.png)](images/whole-sky-evening-en-white-subdue-off.png)

*Den samme kvelden med Demp under horisonten av: horisonten tegnes
fortsatt, og ingenting er dempet.*

## Sømmen

Venstre og høyre kant av ellipsen er én halvsirkel på himmelen:
meridianen av ekliptisk lengde rett overfor den sentrale, fra pol til
pol. Et sted må den klippes, og det som skjer der, er avgjort:

- **Ikke noe blekk krysser den.** En linje som når sømmen, stopper ved
  omrisset og fortsetter fra den andre kanten. Ingenting skjules for å
  få til bruddet; en stjerne eller et legeme ved sømmen tegnes, på sin
  egen side.
- **En storsirkel klippes én gang.** På hver målte side er ekliptikken,
  himmelekvator, horisonten og meridianen hver for seg ett ubrutt løp
  fra kant til kant. De er ikke de eksakte kurvene fra de vanlige
  sidene: i denne ene projeksjonen tegnes hver av dem gjennom **1 440
  punkter**, og de to endene legges på selve omrisset.
- **Et delt stjernebilde får navnet sitt én gang.** En figur sømmen
  deler, tegnes i to deler, én ved hver kant, og navnet står på den
  delen som har mest av linjene. På hver målte side har hvert av de
  **88** stjernebildene navnet sitt forankret i sin egen figur, og
  ingen er strandet mellom to halvdeler.
- **Ikke noe navn ligger over den.** Et legeme innenfor en navnebredde
  fra sømmen beholder merket sitt, kan velges og leses opp, og navnet
  avvises.

Fordi kartet følger observatøren, møter sømmen alltid ekliptikken i dens
laveste punkt under horisonten. Rundt lokal midnatt er Solen i nærheten
av det. **2029-01-03** ved midnatt UTC, en dato funnet ved å søke etter
øyeblikket da de ni legemene er jevnest spredt rundt himmelen, står
Solen **+178.2°** fra sentralmeridianen, **1.8°** fra sømmen.

[![Et kart over hele himmelen der horisontens skål rommer den øvre midten
av ellipsen: planetene er spredt langs hele den rette midtlinjen, med
Mars og Månen til venstre for sentrum, Uranus og Saturn til høyre for
det, og Solen som et lite grått merke helt ytterst til venstre på den
linjen.](images/whole-sky-spread-en-white.png)](images/whole-sky-spread-en-white.png)

*2029-01-03, 00:00 UTC, Oslo: ingen åpning mellom de ni legemene er
bredere enn **64.2°**. Solen er det lille grå merket helt ytterst til
venstre på midtlinjen, tegnet på sin egen side av sømmen, uten navn.*

## Når planetene samler seg

Det motsatte tilfellet er **2027-04-25**, da de sju planetene ligger
innenfor **138.7°** i ekliptisk lengde. Fra Oslo ved midnatt UTC er bare
Jupiter og Mars oppe; Solen, Månen og de fem andre planetene er under
horisonten, de fleste av dem tett sammen mot høyre kant.

[![Et kart over hele himmelen der horisonten går som en lang bølge tvers
over ellipsen med den øvre delen av himmelen over seg: Mars og Jupiter
står til høyre for sentrum på midtlinjen, og Solen, Merkur og flere
andre merker står tett i det grå området ytterst til
høyre.](images/whole-sky-gathering-en-white.png)](images/whole-sky-gathering-en-white.png)

*2027-04-25, 00:00 UTC, Oslo: planetene samlet. Alle ni merkene er på
siden; fire navn fikk plass.*

Denne siden viser kartets grense like tydelig som nytten. Alle ni
legemene er tegnet, men der seks av dem står tett sammen og hvert navn
bærer *(under horisonten)*, er det ikke plass til å skrive alle: på
maskinen som tegnet figuren, er **fire av de ni navnene** plassert.
Merkene står der, hvert av dem kan velges, og den opplesbare
beskrivelsen lister alle ni med horisonttilstanden deres. Navnene er det
Åpne her er til for.

I denne målestokken er hver planet **et kartografisk symbol**, og siden
sier det når den leses opp - *et kartografisk symbol, ikke Neptuns
tilsynelatende diameter* - og ingen måne tegnes: om hver måne siden
ellers ville tilbudt, sier den *for nær til å skjelnes i dette feltet,
ikke tegnet*.

## Fra hele himmelen og inn i et kart

Hele himmelen er et sted å velge fra, ikke et sted å bli. Velg et
legeme, en stjerne eller en tom posisjon, som på enhver side, og knappen
i Utforskeren heter *Åpne her*. Den forlater hele himmelen for det
vanlige kartet sentrert på det du valgte, med synsfeltet du brukte før
du gikk inn.

[![Et vanlig rektangulært kart på 60 grader: Saturn og Neptun som små
merker med navn nær midten, marsjevndøgnet merket på den skrå
strekpunktede ekliptikken, figurene til Fiskene og Hvalen, og Månen ved
høyre kant.](images/whole-sky-journey-open-here.png)](images/whole-sky-journey-open-here.png)

*Åpne her på Saturn, fra kveldssiden: det vanlige kartet på **60°**,
med himmelens nord opp igjen, og Neptun og Månen i samme felt.*

I den målte reisen velges Saturn én piksel utenfor merket, og kartet
åpner sentrert **0.000000°** fra Saturns posisjon, i feltet på 60° som
ble forlatt, i det feltets egen projeksjon. Hele himmelen har gjort
jobben sin: spørsmålet «hvor er Saturn, og hva er i nærheten?» ble
besvart på én side, og svaret er nå et vanlig kart du kan zoome,
panorere og lese som ethvert annet.

Slår du *Hele himmelen* av uten å velge noe, kommer det vanlige kartet
tilbake nøyaktig som du forlot det.

## Hva zoom, panorering og Tilbakestill visningen gjør her

Hele himmelen har ikke noe felt som kan gjøres bredere eller smalere, og
ikke noe sentrum som kan flyttes, så kontrollene som endrer dem, avvises
heller enn å få en ny betydning:

- **Zoom** er utilgjengelig, og sier hvorfor: *zoom går aldri inn i
  eller ut av denne visningen*. Du kommer ikke til hele himmelen ved å
  zoome ut, og du forlater den ikke ved å zoome inn.
- **Å dra** panorerer ikke. Sentralmeridianen tilhører observatøren,
  ikke pekeren.
- **Tilbakestill visningen** forlater hele himmelen for atlasets første
  side, slik den gjør fra hvor som helst.
- **På denne siden** er utilgjengelig, fordi *en liste over hele
  himmelen er ikke den regionale tabellen dette lover*. Bruk Åpne her
  først, og så På denne siden i kartet som åpnes.
- **Meridianens høydeavlesning** plasseres ikke; et kart over hele
  himmelen leses ikke langs meridianen.

Ingenting ved modusen huskes når atlaset lukkes: det starter alltid på
det vanlige kartet. En visning av hele himmelen kan lagres som et
**bokmerke**, og et slikt bokmerke har alltid med seg kartets tid og
sted, for uten dem er siden ingenting. Eksisterende bokmerker er
uendret, og en eldre versjon av atlaset beholder et helhimmel-bokmerke
nøyaktig som det er, merket som noe som trenger oppmerksomhet, uten å
åpne det eller skrive det om.

## Kartene som ikke ble valgt

Valget av Hammer, av ekliptikken og av senits meridian ble gjort ut fra
tegnede sider og målinger, og alternativene tapte av grunner det er verd
å kjenne.

**Mollweide**, den andre arealriktige ellipsen, er den nærmeste
rivalen og er bedre i gjennomsnitt: en middelforvrengning på **32.2°**
over hele himmelen mot Hammers **35.6°**, med rette breddesirkler. Men
den bærer konstante **12.0°** langs hele midtlinjen, der Hammer ikke har
noen i sentrum. Midtlinjen og sentrum er nettopp der dette kartet legger
planetene og observatørens himmel.

**Det enkle rektangelet**, lengde bortover og bredde oppover, har den
laveste middelforvrengningen av alle, **16.8°**. Det betaler for det i
areal, som det blåser opp **opptil 36 ganger** mot øvre og nedre kant,
der hver pol blir en linje like bred som kartet. For en leser i Oslo
smører det den sirkumpolare himmelen, den delen som ses hver klare natt,
ut over hele toppen av siden.

**To halvkuler side om side** ville byttet sømmen mot et snitt langs en
hel storsirkel, randen av begge skivene, så ekliptikken og hver figur
som går fra den ene skiven til den andre, ville blitt klippet to ganger.

**Himmelens nord opp** var alternativet til ekliptikken. Rutenettet
ville vært det vante og nord nøyaktig opp i sentrum, men planetene ville
vandret opp og ned langs en kurve i stedet for å ligge langs en rett
linje gjennom den beste delen av kartet.

**Andre sentralmeridianer** ble også tegnet. Én som ligger fast for
alltid, lar observatørens senit havne så mye som **175 grader** i lengde
fra midten av kartet. Én som følger sentrum i det forrige kartet,
avhenger av noe som ikke har med spørsmålet å gjøre, og la Jupiter **3.1
grader** fra sømmen. Én rett overfor Solen, det fristende valget for en
nattehimmel, klipper Solen i to ved sømmen hver gang.

## Hva visningen ikke påstår

- **Den er ikke himmelen slik du ser den.** Senit er ikke sentrum, og
  opp er ikke rett over deg; horisonten er en kurve tegnet på et kart
  over ekliptikken, ikke kanten av et utsyn. For himmelen slik den står
  rundt en retning, åpne et vanlig kart der.
- **Former holder bare nær sentrum.** Arealer er sanne overalt; figurer
  langt fra sentrum er skjøvet, og ingen målestokk, feltbredde eller
  noe sentrum oppgis, fordi kartet ikke har én av noen av dem.
- **På siden er ikke på himmelen.** Horisonten er den matematiske, uten
  luft og uten landskap, og atlaset skriver ikke ut noen magnitude for
  en planet. Et legeme over horisonten kan stå i dagslys eller for lavt
  til å ses.
- **Symboler er ikke størrelser, og navn er ikke garantert.** Hver
  planet er et symbol her, og ingen måne tegnes; et navn skrives bare
  der det får plass i sin helhet, og ved sømmen eller i en klynge får
  det ofte ikke det.
- **Stjerner til V 5.0, ikke svakere.** Siden tar den klareste av din
  egen grense og **V 5.0**; en grense på **V 8.0** i det vanlige kartet
  blir V 5.0 her og kommer tilbake når du går ut.
- **Ett øyeblikk.** Siden viser hvor alt er ved kartets tid. Den tegner
  ingen baner, forutsier ingen oppganger, nedganger eller konjunksjoner,
  og søker ikke etter hendelser.
- **Ingen Melkevei.** Atlaset tegner ingen på noen side, og derfor ingen
  her.

*Tekniske følgedokumenter: `docs/decisions/all-sky-projection.md`
(studien, avgjørelsen og visningen slik den er bygget),
`docs/studies/whole-sky/measurements.md` og dens `platform.md` (målingene
og figurene denne artikkelen viser), og
`docs/studies/all-sky/measurements.md` (kandidatene som ble tegnet og
ikke valgt, beholdt som historikk).*
