# Merkur, aldri langt fra Solen

*Hva atlaset tegner av Merkur, hvorfor tegningen så ofte må være et
symbol, hva nærfeltene legger til, og hva atlaset ikke påstår*

## Planeten du må fange

Merkur er planeten de fleste aldri har sett med viten og vilje. Den
holder seg nær Solen - i 2026 går de største elongasjonene fra
**18.1°** til **27.8°** - så den står alltid lavt i skumringen eller
i grålysningen, og den har det travelt: en hel sving fra den ene siden
av Solen til den andre tar uker, ikke måneder. I et teleskop viser den
faser som Venus, på en skive som er en femtedel så stor. JUranometria
5.3 tegner Merkur med fasen på dens sanne plass for ditt sted på
jorden og ditt tidspunkt, på samme side som fiksstjernene; den legger
en Merkur-tabell ved siden av Venus-tabellen; og den åpner to felt
smalere enn 1°, *nærfeltene*, der Merkurs skive endelig kan sees på
skjermen. Denne artikkelen handler om hvordan de merkene og sidene
skal leses.

Atlasets egen redegjørelse for beslutningene står i
`docs/decisions/mercury-system.md` og
`docs/decisions/mercury-cartography.md`; denne artikkelen forklarer
dem, den erstatter dem ikke.

## Hvor tallene kommer fra

Ingenting hentes fra nettet. Merkur beregnes fra NASA/JPLs efemeride
**DE440**, den samme kilden som atlasets Sol, Måne, Venus og Jupiter
bruker, skåret til en egen fil i den medfølgende planetpakken:
**3 299 328 bytes**, som dekker **1900–2100**, med hver koeffisient
slik JPL publiserte den. Den ligger ved siden av Venus' fil, som er
byte for byte den samme som 5.2 leverte: en utgitt planets fil skrives
aldri om.

Merkurs størrelse er den faste kroppen: IAU-figuren er treakset,
**2440.53 km** langs to akser og **2438.26 km** langs den tredje, og
atlaset tegner den **ekvatoriale 2440.53 km**, aldri en middelradius.
Det finnes ikke noe skydekke å diskutere; det et teleskop viser av
Merkur, er stein, og atlaset tegner den steinen som en figur, ikke
som et bilde.

## Holdt opp mot en autoritet

Tallene kontrolleres, de tas ikke på tro. Atlasets Merkur ble
sammenlignet med **JPL Horizons** på **11 207** rader - fem
observatører (Oslo, Quito, Cape Town, Alert og Chathamøyene på andre
siden av datolinjen) og det geosentriske tilfellet, ved de navngitte
hendelsene og hver uke gjennom hele intervallet. I årene der
klokkekorreksjonen er kjent nøyaktig, stemmer plassen blant stjernene
til **0.000″** og avstanden til **0.2 km**; den tilsynelatende plassen
for datoen stemmer til **0.129″**, og det er atlasets eldre modeller
for presesjon og nutasjon, ikke planeten. Siden av Solen stemmer på
hver rad, og parallaksen som legges til - forskyvningen mellom jordens
sentrum og ditt sted på den - når **15.88″**.

Etter **2027-06-28** er forskjellen mellom atomtid og jordens rotasjon
en prognose, og tabellen merker de tidspunktene *est.*: Merkur beveger
seg fort, så klokkens usikkerhet koster mer her enn for Venus - opptil
**11.858″** innen 2100 - og det er klokken, ikke planeten.

De navngitte hendelsene er Fred Espenaks almanakkrader og
NASA/GSFCs passasjekatalog: konjunksjonene faller **−16 til +37**
minutter fra den oppgitte timen, de største elongasjonene stemmer med
den trykte vinkelen til tidels grad, og de **28** største passasjene
i 1900–2100 faller **−0.5 til +0.7** minutter fra katalogens minutt,
med avstanden mellom de to sentrene stemmende til **0.1″**.

Én av passasjene er ærlig om en grense. **1937-05-11** streifet Merkur
Solens rand; katalogen regner det som en passasje, men etter atlasets
egne radier ligger Merkurs sentrum **+0.017″** utenfor randen ved
største passasje, så atlaset sier *nær nedre konjunksjon* i stedet for
*foran Solen*. Marginen er bokført med navn, og ethvert nytt slikt
tilfelle stopper byggingen.

## Fasen i ord

Merkur-tabellen beskriver fasen slik Venus-tabellen gjør det, ord for
ord. Når Merkurs skive møter Solens, sier tabellen det - *foran Solen*
eller *bak Solen* - og ellers bruker den de fem båndene på den belyste
andelen: *nær nedre konjunksjon* under **2 %**, *sigd*, *nær halvt
belyst*, *mer enn halvt belyst*, og *nær full* fra **98 %**. Ved siden
av båndet står den belyste siden, posisjonsvinkelen til den lyse
randen og nærmeste av seksten kompassretninger, og nær nedre
konjunksjon og nær full sier tabellen *ikke veldefinert* i stedet for
å finne på en retning.

Merkurs tilsynelatende diameter går fra **4.64″** når den er lengst
borte, bortenfor Solen, til **12.26″** når den er nærmest - en
femtedel av Venus' spenn, og lite nok til at det har noe å si for alt
som følger.

## Den navngitte kvelden

Tidspunktet atlaset måler seg selv mot, er den største østlige
elongasjonen i 2026, som Espenak oppgir til **2026-02-19 18:00** UTC.
I Oslo sier tabellen at Merkur er **49.7 %** belyst, *nær halvt
belyst*, **18.1°** øst for Solen på kveldshimmelen, med den belyste
siden mot **243°**, vest-sørvest - mot Solen, slik den må være. Skiven
er **7.2″** bred, Merkur er **0.937962** AE borte, omtrent **140.317**
millioner kilometer, og den står **1.80°** over den atmosfæreløse
horisonten: Merkur på sitt beste i en norsk februar er en planet i det
siste dagslyset, en håndsbredd over bakken.

![Merkur-tabellen 2026-02-19 18:00 UTC for Oslo: én rad med
rektascensjon 23h 17m 02.0s, deklinasjon −3° 20′ 04″, høyde 1.80°,
avstand 0.937962 AE, tilsynelatende diameter 7.2 buesekunder, 49.7 %
belyst, nær halvt belyst, 18.1° Ø, belyst side 243°
vest-sørvest.](images/mercury-table-nb-NO.png)

*Merkur-tabellen ved det navngitte tidspunktet: én rad, hver kolonne
fra tjenesten, tidsmerknaden under.*

## Fra tallene til kartet

Kartet tegner Merkur der tallene plasserer den, gjennom bryteren
*Merkur på kartet* i Vis ▸ Solsystem og i Kontrollerens Merkur-gruppe
- av til du velger den, deretter husket. Regelen er Venus', uendret:
**Merkur tegnes i sann størrelse der den kan sees, og som et symbol
med fase der den ikke kan det.**

For Merkur er den andre halvdelen nesten hele historien. På atlasets
vanlige sider, ved hvert felt ned til 1°, er Merkurs skive mellom
**1.16–3.06 px** bred - mindre enn den minste stjernens prikk - så
atlaset tegner det kartografiske symbolet på **6 px**, sier det i den
opplesbare beskrivelsen, *et kartografisk symbol, ikke Merkurs
tilsynelatende diameter*, og maler det under stjernene, så det aldri
skjuler en. Symbolet har fase: den belyste delen er planetens belyste
del, dreid gjennom sidens egen nord og øst, så halvdelen som vender
mot Solen på himmelen, vender mot Solen på siden.

![Et 8-graders kart på norsk sentrert nær Merkur 19. februar 2026 kl.
18:00 UTC: et lite, halvt belyst merke merket Merkur blant noen få
stjerner, med rutenett og tittelfelt på
norsk.](images/mercury-elongation-8-nb.png)

*Det navngitte tidspunktet ved 8°, siden på norsk: symbolet på 6 px,
halvt belyst, med den belyste siden mot vest, mot Solen.*

Ved 1°, der Venus' skive ved samme slags tidspunkt så vidt tar over
fra merket, er Merkurs fortsatt omtrent **1.8 px**: merket står i
stedet, og fasen du ser, er i merkets belyste del, aldri i en
størrelse skiven ikke har.

![Et 1-graders kart sentrert på Merkur 19. februar 2026 kl. 18:00
UTC: et lite, halvt belyst merke merket Mercury nær midten av en
nesten tom side, én stjerne nede til høyre, rutenettet merket i
bueminutter.](images/mercury-elongation-1.png)

*Samme tidspunkt ved 1°: fortsatt symbolet - den sanne skiven er
under to piksler her.*

På den svarte himmelen tegnes det samme merket i den svarte himmelens
stjerneblekk, med den mørke siden litt over himmelens bunn, så fasen
fortsatt kan leses.

![Et 3-graders kart på den svarte himmelen ved det navngitte
tidspunktet: det halvt belyste Merkur-merket blant hvite stjerner på
svart.](images/mercury-elongation-3-dark.png)

*Det navngitte tidspunktet ved 3° på den svarte himmelen.*

## Nærfeltene

Målt over hver uke i 1900–2100 kan Merkurs fase leses på skjermen i
**ingen uke ved noe felt** i atlasets vanlige stige; på et A4-ark ved
300 dpi kan den leses i **30.3 %** av ukene ved 1°. Den sanne skiven
når merket på 6 px først ved **0.51°** når Merkur er nærmest og ved
**0.19°** når den er lengst borte. Derfor åpner 5.3 to felt under
1°-gulvet, **0.5°** og **0.25°**, og kaller dem *nærfelt*. Zoom inn
tilbyr det neste av dem - fra verktøylinjen, Kontrolleren, Vis-menyen,
hurtigtastene eller hjulet - når Merkur, Venus eller Jupiter, vist på
kartet, ligger på siden det steget ville vist; siden sier at den er et
nærfelt; panorering beholder feltet; zoom ut går tilbake til den
vanlige stigen; Hjem er uendret; og et nærfelt lagres aldri.

Ved det navngitte tidspunktet tegner 0.5°-siden fortsatt symbolet.

![Et 0.5-graders kart sentrert på Merkur 19. februar 2026 kl. 18:00
UTC: det halvt belyste merket merket Mercury alene på en side der
tittelfeltet sier Closer field
0.5°.](images/mercury-closer-0.5.png)

*Det navngitte tidspunktet ved 0.5°: fortsatt symbolet, og
tittelfeltet sier at dette er et nærfelt.*

Ved 0.25° er den sanne skiven **7.2 px**, over merket, og tar over:
ugjennomsiktig, Månens skive med fase i miniatyr, tegnet som seg selv.

![Et 0.25-graders kart sentrert på Merkur 19. februar 2026 kl. 18:00
UTC: en liten, halvt belyst skive merket Mercury alene på siden,
rutenettet merket for hvert annet
bueminutt.](images/mercury-closer-0.25.png)

*Det navngitte tidspunktet ved 0.25°: den sanne skiven, halvt belyst,
med den belyste siden mot Solen.*

Det er hele grunnen til at nærfeltene finnes: ikke for å forstørre
stjernene, som er den samme katalogen til samme grense, men for at en
planet med en skive på noen få buesekunder i det hele tatt skal kunne
tegnes som en skive.

## Å lese en konjunksjon

Det finnes ingen egen tegning for en konjunksjon; rekkefølgen
malingen skjer i, tar seg av den. Det nærmeste legemet males sist.

**En passasje.** **2019-11-11** kl. **15:20** UTC passerte Merkur over
Solens skive - største passasje, til katalogens minutt. Ved 1° er
Solen en ring rundt en fylt skive på **484.4 px**; Merkur er et symbol
foran en ugjennomsiktig skive, så i stedet for å legges under
stjernene males det over Solen, med den mørke siden lesbar mot Solens
bunn ved siden av Solens lille sentrumsprikk, og siden sier *foran
Solen* ved navnet.

![Et 1-graders kart 11. november 2019 kl. 15:20 UTC: en stor, tykk
ring, Solen, som fyller det meste av siden, med Merkurs lille, mørke
merke nær midten og Solens sentrumsprikk like under
det.](images/mercury-transit-2019-1.png)

*Passasjen i 2019 ved 1°: symbolet over Solens skive, navnet avvist
inne i Solens blekk.*

![Et 6-graders kart på norsk 11. november 2019 kl. 15:20 UTC: Solen
som en ring merket Solen med Merkurs lille merke inne i den nær
sentrum.](images/mercury-transit-2019-6-nb.png)

*Den samme passasjen ved 6°, på norsk: merket over Solens ring.*

Ved 0.25° er Solens skive **1937.8 px** bred - hele siden ligger inne
i den, ringen er utenfor siden - og Merkurs sanne skive, **10.0 px**,
tegnes over Solens bunn ved siden av sentrumsprikken, mørk, fordi den
belyste siden vender bort fra oss.

![Et 0.25-graders kart 11. november 2019 kl. 15:20 UTC: en helt hvit
side med én mørkegrå skive, Merkur, nær midten, og en mindre, svart
prikk, Solens sentrum, nedenfor og til høyre for
den.](images/mercury-transit-2019-0.25.png)

*Passasjen i 2019 ved 0.25°: Merkurs sanne skive på Solens bunn.
Ingenting på selve siden sier at dette er Solen; det gjør tabellen.*

Den neste passasjen atlasets intervall rommer, er **2032-11-13 08:54**
UTC, merket *est.* fordi den ligger etter den nøyaktig kjente klokken.

**Bak Solen** dekker den samme rekkefølgen Merkur: **2026-05-14** kl.
**14:00** UTC legges merket under stjernene og Solens skive males
etter det, og siden sier *bak Solen*.

**En nedre konjunksjon.** **2026-03-07** kl. **11:00** UTC går Merkur
mellom oss og Solen, **3.6°** vest for den. Tabellen sier **0.7 %**
belyst, *nær nedre konjunksjon*. Merket tegnes mørkt med randen sin:
den belyste sigden er under en piksel, og atlaset finner ikke på en
for å få fasen til å se penere ut.

![Et 1-graders kart 7. mars 2026 kl. 11:00 UTC: et lite, mørkt merke
merket Mercury midt på en nesten tom
side.](images/mercury-inferior-conjunction-2026-1.png)

*Nedre konjunksjon ved 1°: det mørke merket med randen sin.*

## En hel synlighetsperiode

Atlaset holder den tegnede fasen opp mot tabellens. Fra den øvre
konjunksjonen i 2026, **2026-01-21**, hver **3. dag** gjennom den
nedre konjunksjonen og **50 dager** videre, tegnes en 1°-side av
produksjonskartet, og merket klippes ut av den. Pikslene i hvert merke
telles: hvor mye av det som er tegnet belyst, og hvilken vei den
belyste siden vender, lest tilbake gjennom sidens nord og øst. Et
merke på 6 px rommer **omtrent to dusin** piksler, så andelen er grov;
det som holdes, er at en sigd tegnes mindre enn halvt belyst og et mer
enn halvt belyst merke mer, og at den belyste siden vender **innenfor
45°** av tabellens retning. Det gjør den for **alle 19** trinn der
tabellen oppgir en retning.

![Tre rader med små merker klippet fra 1-graders sider hver 3. dag
gjennom første halvdel av 2026, hvert med bokstaven M fra navnet ved
siden av: nesten fulle merker først, så mer enn halvt belyste, så
sigder som blir mørke, så sigder som åpner seg igjen på den andre
siden og fylles opp.](images/mercury-apparition-2026.png)

*Synlighetsperioden i 2026, ett merke hver 3. dag, fra venstre mot
høyre. Navnene er klippet bort med rutene; sidene de kommer fra, står
i dokumentasjonen.*

## Hva atlaset ikke påstår

- **Ingen overflate.** Skiven er den faste kroppen, tegnet som en
  figur, ikke et bilde: ingen tegninger på den, ingen albedo, ingen
  farge.
- **Symbolet er ikke størrelsen.** På hver vanlig side er Merkur
  merket på 6 px, som sier hvor Merkur er og hvilken side som er
  belyst, og ingenting om hvor stor Merkur ser ut; den opplesbare
  beskrivelsen sier det.
- **Å se den på siden er ikke å se den på himmelen.** Merkurs høyde
  ved det navngitte tidspunktet oppgis uten atmosfære; refraksjon nær
  horisonten tas ikke med, og atlaset sier ingenting om hvorvidt
  Merkur kan sees med øyet.
- **Ingen oppdiktet sigd.** Nær nedre konjunksjon og nær full er
  tegningen det geometrien gir.
- **Ingen hendelser.** Atlaset beregner enkelttidspunkter; det leter
  ikke etter passasjer eller elongasjoner og forutsier ikke tidene
  deres. De navngitte tidspunktene er faste målepunkter, ikke en
  katalog.
- **Tre planeter.** Merkur, Venus og Jupiter er planetene i versjon
  5.3.

*Tekniske følgedokumenter: `docs/decisions/mercury-system.md` (tallene
og kontrakten deres), `docs/decisions/mercury-cartography.md` (merket,
nærfeltene og avgjørelsene om dem), `docs/decisions/mercury-handover.md`
(den avgjorte tallkontrakten samlet),
`docs/studies/mercury/table-measurements.md`,
`docs/studies/mercury-cartography/measurements.md`,
`docs/studies/mercury-on-the-chart/measurements.md` og
`docs/studies/closer-fields/measurements.md` (målingene artikkelen
siterer).*
