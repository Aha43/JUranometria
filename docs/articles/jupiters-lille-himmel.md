# Jupiters lille himmel

*Hva atlaset tegner av Jupiter og de fire store månene, hvordan
merkene skal leses, og hva det ikke påstår*

## En planet som bærer sin egen himmel

Rett et lite teleskop mot Jupiter, og du ser en himmel i miniatyr: en
flattrykt skive med fire lyspunkter på rad ved siden av, Io, Europa,
Ganymedes og Callisto, i en oppstilling som er ny hver kveld og ny
igjen før samme kveld er over. Galileo tegnet dem i 1610 som stjerner
ved siden av en sirkel. JUranometria 5.1 tegner dem også - der de står
for ditt sted på jorden og ditt tidspunkt, på samme side som de faste
stjernene - og denne artikkelen handler om hvordan de merkene skal
leses.

Atlasets egen redegjørelse for avgjørelsene står i
`docs/decisions/jovian-system.md` og `docs/decisions/jovian-cartography.md`;
artikkelen forklarer dem, den erstatter dem ikke.

## Hvor tallene kommer fra

Ingenting hentes. Atlaset bærer en pakke til ved siden av sol- og
månepakken: to modifiserte utdrag av NASA/JPLs **JUP365**-efemeride,
skåret til de årene atlaset svarer for, omdøpt og nymerket etter NAIFs
regler for modifiserte kjerner, med hver eneste koeffisient nøyaktig
slik JPL ga den ut. Jupiter dekkes for **1900–2100**, de fire
månene for **2000–2100**. Jupiters størrelse og polens retning er
IAU 2015-verdiene, kopiert ordrett fra NAIFs planetkonstanter;
ekvatorradien den tilsynelatende diameteren regnes ut fra, er
**71 492 km**. Pakken koster omtrent **59 MB installert, 51 MB
komprimert** - prisen eieren godtok for en planet som svarer uten nett.

Fra disse utdragene regner atlaset ut, for ditt sted og tidspunkt, de
samme fire svarene som for Solen og Månen: hvor Jupiter står blant
stjernene (det astrometriske stedet, i kartets faste ICRS/J2000-ramme),
hvor den står på din himmel (høyde og asimut, uten refraksjon), hvor
langt unna og hvor stor (avstanden; ekvator- og poldiameteren, for
Jupiter er synlig flattrykt), og for hvilket øyeblikk. Det legger til
tre som bare en planet med måner trenger: hvor mye av skiven som er
belyst, hvilken vei nordpolen peker på siden, og hvor hver måne står
ved siden av planeten.

En månes sted oppgis slik JPL Horizons gjør det: hvor langt øst eller
vest og nord eller sør for Jupiters sentrum, i buesekunder;
separasjonen; den samme avstanden i Jupiter-radier; og en *tilstand*
på én linje. Tilstanden er satt sammen av to fakta atlaset holder fra
hverandre. Overlapper månens skive Jupiters tilsynelatende figur, og i
så fall på den nære siden (*foran*) eller den fjerne (*bak*)? Og er
den belyst av Solen, eller helt eller delvis inne i Jupiters skygge? En
måne bak Jupiter kan ikke ses uansett belysning, så det sies først; en
måne foran er solbelyst av geometrien; en måne fri av skiven men i
skyggen er mørk, og tabellen sier hvorfor. Overlappingen prøves rand mot
rand på den flattrykte figuren, med månens egen radius medregnet, så
en måne som berører randen er allerede *foran* - det er det en
observatør ser ved inngangen.

## Holdt mot en autoritet

Hver størrelse ble sammenlignet, rad for rad, med JPL Horizons - en
uavhengig implementasjon i samme efemeridefamilie - for fem
observatører, hver sjuende dag gjennom to århundrer, daglig gjennom
2026, hver time gjennom desember 2026 og minutt for minutt én navngitt
kveld: **22 993 rader per legeme**. Gjennom årene der borgerlig tid er
kjent nøyaktig, stemmer Jupiters astrometriske sted til **0.0001″** og
det tilsynelatende til **0.1337″**; månenes forskyvninger fra Jupiter
stemmer til **0.0006″**. Synlighetstilstandene, bedømt etter
definisjonen Horizons publiserer, stemmer på **91 971 av 91 972** rader -
den ene uenigheten en strykende okkultasjon av Europa over Oslo
**2026-12-06 17:00** UTC, der separasjonen er **20.3635″** mot en
randsum på **20.3640″**: et halvt tusendels buesekund, innenfor
forskjellen mellom de to implementasjonenes behandling av lystiden.
Det tilfellet er navngitt i kontrakten, ikke tatt høyde for i sin
alminnelighet; et nummer to ville stoppet bygget.

To ærlige grenser ble funnet ved måling, ikke antatt. Jupiters
tilsynelatende sted trenger Solens gravitasjonsbøyning av lyset, som
Solen og Månen kunne se bort fra; uten den ville atlaset bomme med et
buesekund når Jupiter står en halv grad fra Solen, så den er tatt med.
Og etter at den festede skuddsekundlisten tar slutt, midt i 2027, kan
atlaset bare anslå forskjellen mellom borgerlig tid og efemeridens tid,
og Jupiters tilsynelatende sted er da godt til **1.3567″**, avstanden
til **3739.16 km** - utgivelsesnotatene setter grensen ved to
buesekunder, tabellen merker hver slik rad *est.*, og ingenting i den
tiden tilbys som et forutsagt hendelsestidspunkt.

## Den navngitte kvelden

**11. desember 2026**, sett fra Oslo, krysser tre av de fire månene
Jupiters skive samtidig. Atlasets navngitte fikstur er fire øyeblikk
den kvelden, hvert tall lest fra Horizons' egne rader. Klokken **22:45
UTC** står Jupiter **18.051°** over horisonten i asimut **94.580°**,
**4.868899** AU unna, **40.491″** over ekvator, **99.270 %** belyst. Io
er **+15.831″** øst og **-5.602″** sør for sentrum, **0.829** radier ut -
foran; Europa **+8.722″** øst, **-0.442″** sør, **0.431** radier - foran;
Callisto **-15.480″** vest, **+11.269″** nord, **0.946** radier - foran;
Ganymedes **302.990″** unna, **14.966** radier - fri.

![Jupiter-kortet for 11. desember 2026 kl. 22:45 UTC: rektascensjon 09h
56m 39.3s, deklinasjon +13 grader 25 minutter 46 sekunder, høyde 18.1
grader, asimut 94.6 grader, avstand 4.869 AU, tilsynelatende diameter
40.5 og 37.9 buesekunder, belyst 99.3 prosent, nordpolens
posisjonsvinkel 22.2 grader.](images/jupiter-card-en.png)

![De fire månene i det øyeblikket, på norsk: Io øst, 15.8 buesekunder
øst og 5.6 sør, 0.83 radier, foran Jupiter; Europa øst, 8.7 øst og 0.4
sør, 0.43 radier, foran; Ganymedes vest, 280.3 vest og 115.1 nord,
14.97 radier, fri av Jupiter; Callisto vest, 15.5 vest og 11.3 nord,
0.95 radier, foran.](images/jupiter-moons-table-nb-NO.png)

*Jupiter-kortet og de fire månene slik tabellen viser dem for den
tredoble passasjen. Der atlaset skriver "øst" ved siden av et tall,
skriver Horizons et fortegn; de to er enige.*

Fordi atlaset prøver rand mot rand på den flattrykte figuren mens
Horizons prøver mot en ekvatorkule, kan minuttene en måne går inn på
eller ut av skiven skille med ett: denne kvelden faller Europas inngang
på **21:53** mot Horizons' **21:52**, Callistos utgang på **22:58** mot
**22:59**, og Ios to kryssinger, **22:32** og **00:53**, er samme minutt
hos begge.
Det ene minuttet er den målte prisen for å si hvilken rand en måne
berører, og den sies i stedet for å gjemmes.

## Fra tallene til kartet

Kartet tegner Jupiter der tallene setter den, gjennom samme bryter,
*Jupiter og måner på kartet*, i Vis-menyen og i Kontrollerens
Jupiter-gruppe - av til du velger den, siden husket. To regler avgjør
hva du ser.

**Jupiter tegnes i sann størrelse der det kan ses, og som et symbol der
det ikke kan.** Ved feltene en leser vanligvis arbeider i, er Jupiters
førti buesekunder mindre enn prikken til den svakeste stjernen, så
atlaset tegner et **6 px** kartografisk symbol, sier det i den opplese
beskrivelsen - *et kartografisk symbol, ikke Jupiters tilsynelatende
diameter* - og maler det under stjernene, så det aldri skjuler en. Ved
1°-feltet har den sanne skiven vokst til **10.1 px** og tar over: dekkende,
med omrisset dreid mot planetens flattrykte figur, den korte aksen
langs polen. Flattrykningen tegnes der, men påstås ikke synlig; i den
målestokken ligger de to aksene under én piksel fra hverandre.

![Et 1-graders kart sentrert på Jupiter 11. desember 2026 kl. 22:45
UTC: en liten skive merket Jupiter med tre prikker på, merket Europa,
Callisto og Io, og én prikk oppe til høyre merket
Ganymedes.](images/jupiter-triple-transit-1.png)

*Den tredoble passasjen ved 1°: Jupiters sanne skive med Io, Europa og
Callisto foran, hver en prikk med ring i papirets farge, og Ganymedes
fri av den oppe til høyre, vest og nord for Jupiter (den engelske siden).*

**Hver måne er et 3 px merke, og merket sier tilstanden.** En månes egen
skive er mindre enn en piksel, så hver måne er et symbol, og ordforrådet
er fire merker: *fri* av Jupiter, en fylt prikk i stjerneblekk; *foran*,
en fylt prikk med ring i sidens bunnfarge, malt over skiven; *i
Jupiters skygge*, helt eller delvis, en hul ring; *bak*, ingenting -
ingen spøkelsesprikk, intet stiplet omriss. Tabellen holder de finere
skillene i ord; kartet tegner det som kan ses.

![Et 1-graders kart på norsk 2. desember 2026 kl. 04:00 UTC: Jupiters
skive med en liten hul ring like til høyre merket Io, og fylte prikker
merket Europa, Callisto og Ganymedes lenger ut.](images/jupiter-io-shadow-1.png)

*Io helt i Jupiters skygge, 2026-12-02 04:00 UTC - en hul ring
ved siden av skiven - på den norske siden, der månen leses opp som
"Io, i Jupiters skygge".*

![Et 1-graders kart 2. desember 2026 kl. 07:00 UTC: Jupiters skive med
bare tre måner tegnet - Europa, Callisto og Ganymedes - og intet merke
for Io.](images/jupiter-io-behind-1.png)

*Tre timer senere, 2026-12-02 07:00 UTC, er Io bak Jupiter og tegnes rett
og slett ikke.*

Ved 1° tegnes hver måne som ikke er bak Jupiter, også der de trenges,
for ellers ville atlasets ene navngitte kveld miste hele poenget. Ved
videre felt tegnes en måne bare når merket kan skilles fra Jupiters og
fra de andres, og hver måne avgjøres for seg, så ett trangt par tar
aldri resten med seg. Navn legges rundt merkene på kartets vanlige vis,
og et navn uten ren plass sløyfes for seg, skyves aldri over noe annet.

![Et 3-graders kart på svart himmel 6. mars 2026 kl. 06:00 UTC: Jupiter
som et lite fylt symbol med to prikker til høyre merket Ganymedes og
Callisto, og ingen merker for Io og Europa.](images/jupiter-spread-3-dark.png)

*De samme fire månene ved 3° på svart himmel, 2026-03-06 06:00 UTC:
Jupiter er et symbol nå,
og bare Ganymedes og Callisto står langt nok fra den til å tegnes; Io
og Europa gjør det ikke, og kartet later ikke som noe annet.*

![Et 8-graders kart på svart himmel 11. desember 2026 kl. 22:45 UTC:
Jupiter som et lite symbol merket Jupiter, med én prikk ved siden av
merket Ganymedes.](images/jupiter-triple-8.png)

*Den tredoble passasjen ved 8°: i denne målestokken kan de tre
passerende månene ikke vises, og bare Ganymedes, langt mot vest, vises.*

De samme to reglene gjelder under en tegnet horisont, der Jupiter
dempes og sier det, og på papir, der et eksportert ark tegner Jupiter
og månene nøyaktig som skjermen - samme steder, tilstander og navn.

![Et 8-graders kart 1. desember 2026 kl. 12:20 UTC med en stiplet
matematisk horisont; like under den et blekt symbol merket Jupiter
(below the horizon) med to prikker ved siden av.](images/jupiter-horizon-drawn-8.png)

*Jupiter 1.2° under den matematiske horisonten i Oslo, 2026-12-01 12:20 UTC:
tegnet blekt, og navngitt som under den.*

## Å lese rekkefølgen

![Et 1-graders kart 11. desember 2026 kl. 22:30 UTC: Jupiters skive med
Europa og Callisto på, og Io som en fylt prikk like utenfor den
østlige randen; Ganymedes oppe til høyre.](images/jupiter-sequence-2230.png)

*22:30 UTC: Io, en fylt prikk, har ennå ikke rørt randen; Europa og
Callisto er allerede foran.*

![Samme kart på svart himmel kl. 23:00 UTC: Io og Europa på skiven,
Callisto en fylt prikk like utenfor vestsiden.](images/jupiter-sequence-2300.png)

*23:00 UTC: Callisto har forlatt skiven og er en vanlig prikk igjen; Io
og Europa er fortsatt foran.*

Les de tre sidene sammen, og merkene forteller kvelden: en fylt prikk
blir en prikk med ring når en måne går inn, og en prikk med ring blir en
fylt prikk når den går ut. Ingenting beveger seg av seg selv - atlaset
har ingen klokke og ingen animasjon - men sett tidspunktet, og siden
svarer for det.

## Hva atlaset ikke påstår

- **Ingen overflate.** Ingen skybelter, ingen stor rød flekk og ingen
  rotasjon; skiven er en figur, ikke et bilde.
- **Ingen skygger på Jupiter.** En månes skygge som faller på skiven,
  regnes ikke ut og tegnes derfor ikke, selv om tabellen sier når en
  måne selv står i Jupiters skygge.
- **Ingen hendelser.** Atlaset tar prøver av øyeblikk; det leter ikke
  etter passasjer, formørkelser eller okkultasjoner, og forutsier ikke
  tidspunktene deres. Den ene navngitte kvelden er en fikstur for
  måling, ikke en katalog.
- **Ingen måner før 2000.** Fra 1900 til 1999 tegnes og tabellføres
  Jupiter; månene avvises med årene sine navngitt.
- **Ingen zoom nærmere enn 1°**, og ingen påstand om at flattrykningen
  er synlig der.
- **Én planet.** Jupiter er den eneste planeten i utgivelse 5.1.

*Tekniske følgesvenner: `docs/decisions/jovian-system.md` (tallene og
kontrakten deres), `docs/decisions/jovian-cartography.md` (merkene og
avgjørelsene om dem), `docs/studies/jovian-system/service-accuracy.md`
og `docs/studies/jupiter-on-the-chart/measurements.md` (målingene
artikkelen siterer).*
