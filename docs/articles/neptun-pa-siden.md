# Neptun på siden

*Hva atlaset tegner av Neptun, Triton og Nereid, hvorfor Neptun alltid er
et symbol og aldri en skive, hvorfor månene bare vises på de smaleste
sidene, hvordan polen og månene skal leses, og hva atlaset ikke påstår*

## Den fjerneste planeten

Neptun er den fjerneste planeten atlaset beregner: tretti ganger jordens
avstand fra Solen, og en skive som knapt endrer seg. Gjennom 1900-2100
finner atlaset den mellom **2.18-2.37″** i tverrmål, mindre enn Uranus og
langt under noe en side i atlaset kan oppløse. JUranometria 6.0, utgaven
som følger etter 5.5, beregner Neptun og de to vedtatte månene - Triton
og Nereid - for ditt sted på jorden og ditt tidspunkt, gir dem en
Neptun-tabell ved siden av de andre planetenes, og tegner dem på samme
side som fiksstjernene. Denne artikkelen handler om hvordan det atlaset
tegner, skal leses, og om hvorfor så mye av Neptun gis i tall heller enn
i blekk. Den beskriver atlaset slik det er bygget for 6.0; den er ikke
den utgavens utgivelsesnotat.

Atlasets egen redegjørelse for beslutningene står i
`docs/decisions/neptune-system.md` og
`docs/decisions/neptune-cartography.md`; denne artikkelen forklarer dem,
den erstatter dem ikke.

## Hvor tallene kommer fra

Ingenting hentes fra nettet. Neptuns plass kommer fra **DE442**, JPLs
planetefemeride, slik NAIF har flettet den inn i sin satellittkjerne for
Neptun: Neptun-barysenteret, tyngdepunktet for Neptun og månene, gjennom
**1900–2100**, i **416 768 bytes**. Neptun er den andre planeten i
atlaset, etter Uranus, hvis plass ikke kommer fra DE440, og grunnen er
målt, ikke arvet: sammenlignet med JPL Horizons stemmer DE442s barysenter
med Horizons' Neptun til **0.0040″**, mens DE440s bommer med **0.1072″**;
de to efemeridene legger barysenteret opptil **2 222.155 km** fra
hverandre. Solen og jorden forblir DE440s, og deres DE442-posisjoner
avviker med høyst **0.49 km**.

Barysenteret står for planeten. Neptuns sentrum ligger aldri mer enn
**74.190 km** fra det, som er **0.0036″** sett fra jorden, langt under
noe en side kunne vise. Men Horizons måler en avstand til sentrum, og
forskyvningen er det meste av atlasets avstandsfeil mot den: opptil
**89.48 km** i studien. Denne gangen ble kilometerne målt før noe ble
bygget - lærdommen Uranus-veien lærte sent - og eieren satte
avstandsmålet til **100 km**, med en pakkebygger som nekter å skrive i
det hele tatt om forskyvningen noen gang overstiger **80 km**.

Månene kommer fra NAIFs satellittløsning **NEP098**, i samme Neptun
System-pakke, **50 302 976 bytes**. De beregnes for **2000–2100**;
utenfor de årene vises Neptun alene.

Ingen utgitt fil ble endret for Neptun: Solens, Månens, Merkurs, Venus',
Mars', Jupiters, Saturns og Uranus' filer er byte for byte de samme som
tidligere versjoner leverte.

## Holdt opp mot en autoritet

Tallene kontrolleres, de tas ikke på tro. Atlasets Neptun ble sammenlignet
med **JPL Horizons** på **39 Horizons-forespørsler**, for flere
observatører og ukene gjennom hele intervallet. Der klokkekorreksjonen er
kjent nøyaktig, fra 1972 til **2027-06-28**, stemmer Neptuns plass blant
stjernene til **0.0035″** og avstanden til **93.426 km**, innenfor de
vedtatte 100 km; månenes forskyvning fra Neptun stemmer til **0.0038″**,
posisjonsvinklene deres til **0.0070°**, og tilstandene deres - klar,
foran, bak, i skyggen - på **5 578 av 5 579** rader for Triton og
**5 579 av 5 579** for Nereid. Det ene avviket ligger etter
**2027-06-28**, der forskjellen mellom atomtid og jordens rotasjon er en
prognose; der kan Neptuns plass avvike med opptil **0.2078″** innen 2100.
Det er klokken, ikke planeten. Det pakkede programmet kontrollerer seg
selv mot én Horizons-rad ved hver bygging og treffer den innenfor
**0.0032″**.

## Full, med en pol som nikker

Neptun er en ytre planet langt fra Solen, så den er aldri annet enn full:
atlaset finner den aldri mindre enn **99.971 %** belyst, ved en fasevinkel
på høyst **1.9529°**. Tabellen gir den belyste andelen og fasevinkelen og
bruker ingen faseord i det hele tatt; det er ingenting å navngi.

Neptuns helning er beskjeden ved siden av Uranus': gjennom 1900–2100 går
den observatørvendte breddegraden fra -**29.2178°** til +29.2066°, så
jorden ser Neptun litt ovenfra eller litt nedenfra ekvator, aldri rett
ned på en pol. Det nye er at polen selv beveger seg. Tritons bane får
Neptuns akse til å tegne en liten sirkel på omkring sju århundrer, og IAU
gir polen med et periodisk ledd: en rektascensjon på 299.36° +
**0.70 sin N**, der N er en vinkel som vokser litt hvert år. Uten det
leddet ville atlasets pol bomme på Horizons' med opptil **2 050.7″**; med
det, med **0.018″**. Pakkebyggeren låser leddet og nekter en
konstantfil som mangler det. Som for Uranus er helningen tabellen gir,
*planetosentrisk*; Horizons skriver ut den planetodetiske breddegraden,
og de to stemmer innenfor **0.0520″** når de regnes om. Polens egen plass
gis som en vinkel på himmelen, fra himmelens nord gjennom øst.

## Det navngitte tidspunktet

Raden atlaset holder som sin «i dag», er **2026-10-29 12:00** UTC i Oslo.
Neptun er **29.0485 AU** unna, **2.351″** i tverrmål, **99.991 %** belyst
ved en fasevinkel på **1.0636°**, med nordpolen på posisjonsvinkel
**316.498°** og sørpolen mot oss, og den står **20.77°** under den
luftløse horisonten. Begge månene er klar av Neptun.

## Fra tallene til kartet

Kartet tegner Neptun der tallene plasserer den, med bryteren *Neptun på
kartet* i Vis ▸ Solsystem og i Kontrollerens Neptun-gruppe - av til du
velger den, og deretter husket. Regelen er den samme som for Mars og
Uranus: **Neptun tegnes som det største av den sanne skiven og et
kartografisk symbol på 6 px.** For Neptun er det alltid symbolet. Den
største skiven ville nå merket på **6 px** først ved et felt på
**0.099°**, smalere enn noe felt atlaset tilbyr, nærfeltene medregnet: på
hver side står merket for Neptun, tegnet fullt og rundt i sidens blekk,
uten farge, og omtalt som *et kartografisk symbol, ikke Neptuns
tilsynelatende diameter*. På et merke på 6 px ville den mørke delen av
skiven være **0.002 px** og flattrykkingen høyst **0.040 px**, ingen av
dem synlige, så merket er uten fase og rundt etter regelen. Polen tegnes
heller ikke; tabellen gir den i tall, og den opplesbare beskrivelsen sier
helningen med ord.

Ved opposisjonen **2026-09-26**, da Neptun er **2.36″** i tverrmål og
vendt **18.9°** bort fra oss, er den sanne skiven **en tidel av en
piksel** ved 8°: siden tegner merket, kaller det et symbol, og maler det
under stjernene, så det aldri skjuler en.

![Et kart på 8 grader sentrert på Neptun 26. september 2026, med sidens
ord på norsk: et lite rundt merke med navnet Neptun i midten, stjerner
som prikker i ulike størrelser, rutenettet.](images/neptune-opposition-2026-8-nb.png)

*Opposisjonen i 2026 ved 8°, på norsk: symbolet på 6 px, og ingen måne på
en så bred side.*

![Et kart på 3 grader på svart himmel: et lite hvitt rundt merke med
navnet Neptune i midten, hvite stjerner rundt det, rutenettet i
grått.](images/neptune-opposition-2026-3-dark.png)

*Det samme ved 3° på svart himmel: merket i himmelens stjerneblekk, uten
farge. Siden er på engelsk.*

Neptuns konjunksjoner med Solen er av to slag på siden. Ved Espenaks
konjunksjonstime **2026-03-22** er konjunksjonen bare i ekliptisk lengde:
Neptun står **1.3°** fra Solens sentrum, ved siden av Solens skive, og
siden tegner merket uten å oppgi noe forhold til Solen. Ved timen
**2003-01-30** møter Neptuns skive Solens: Solens skive males over merket,
og siden sier *bak Solen*.

![Et kart på 3 grader 22. mars 2026: Solen som en stor hvit skive med
kraftig svart kant ved sidens øvre kant, med navnet Sun, og Neptun som et
lite rundt merke i midten, en grad og mer under
den.](images/neptune-conjunction-2026-3.png)

*Konjunksjonen i 2026 ved 3°: ved siden av Solen, ikke bak den. Siden er
på engelsk.*

![Et kart på 1 grad 30. januar 2003: Solen som en stor hvit skive med
kraftig svart kant som fyller midten av siden, med navnet Sun; Neptun
skjult under den.](images/neptune-conjunction-2003-1.png)

*Bak Solen, 2003, ved 1°: merket er dekket, og siden sier det. Siden er
på engelsk.*

## Ingen ringer, noe sted

Neptun har ringer, svake og delvis i buer. Atlaset har ingen tall for
dem: ingen ringautoritet ble bedt om for Neptun, så tabellen har ingen
ringrader og siden ingen ringblekk. Det er forskjellen fra Uranus, hvis
ringer atlaset kjenner og holder i tabellen. Ingenting tegnes som atlaset
ikke har tall for.

## Nærfeltene

Neptun er et av legemene som zoom inn tilbyr *nærfeltene* for, **0.5°** og
**0.25°**, under det vanlige gulvet på 1°. De gir Neptun ingenting -
planeten er fortsatt merket - men de er de eneste feltene der Triton står
for seg selv.

![Et kart på 0.25 grader på svart himmel 26. september 2026: Neptun som
et lite hvitt rundt merke i midten, med navnet Neptune, med Triton som en
liten prikk like nedenfor og Nereid som en liten prikk langt oppe til
venstre.](images/neptune-opposition-2026-0.25-dark.png)

*Opposisjonen i 2026 ved 0.25° på svart himmel: merket, fortsatt 6 px,
Triton så vidt klar av det, Nereid langt ute. Innenfor merkets omriss er
hver eneste av dets **12 av 12** piksler lys: merket tegnes fullt. Siden
er på engelsk.*

## To måner, og en ny regel

Hver måne er et symbol på **3 px** på sin egen plass blant stjernene,
avgjort for seg. Som Mars' og Uranus' måner har de ikke noe «gulv»: en av
Neptuns måner tegnes bare der merket står klar av Neptuns tegnede merke
og av månen som alt er tegnet. Fra 2000 til 2100 står Triton
**0.5348″–16.9617″** fra Neptuns sentrum og Nereid
**2.1413″–457.7006″**: Triton er alltid nær, Nereid spenner fra innenfor
merket til nesten åtte bueminutter ut, i den mest eksentriske banen til
noen måne atlaset tegner. Triton er aldri klar av merket ved 1° eller
bredere; ved 0.5° står den klar i **32.8 %** av ukene og ved 0.25° i
**91.3 %**. Nereid er det motsatte av hver måne hittil: etter
klaringsregelen alene ville den stå klar av Neptuns merke i **18.3 %** av
ukene selv ved 12°, i **86.9 %** ved 2°, og i **95.6 %** ved 1°.

Det reiste et spørsmål ingen tidligere planet hadde stilt. Horizons gir
månenes lysstyrke som **13.43–13.632** for Triton og **18.669–18.875**
for Nereid, og atlaset tegner ingen stjerne svakere enn **V 8**: på sitt
lyseste er Triton **5.4 magnituder** svakere enn den svakeste stjernen
noen side tegner, og Nereid **10.7 magnituder** svakere. Tegnet på en
side på 8° eller 12° ville Nereid være en ensom prikk blant stjerner ti
magnituder lysere, som en leser kan ta for en stjerne. Så eieren vedtok:
**Neptuns måner tilbys bare i feltet på 1° og nærfeltene**, der planetens
system er sidens emne, og der er hvert merke fortsatt for seg underlagt
klaringsregelen. På en bredere side tegnes ingen måne, og ingen omtales;
Nereid er en kartlagt måne, aldri en ensom prikk på magnitude 18.7. Der
en måne tilbys, men ikke tegnes, nevner siden den og sier *for nær til å
skjelnes i dette feltet, ikke tegnet*. Hver måne tegnes på sin egen
J2000-plass på kartet, aldri på Neptuns plass pluss tabellens
forskyvning: for Nereid kan de to avvike med **2.317″**, mer enn to
piksler ved 0.25°.

![Et kart på 1 grad 26. september 2026: Neptun som et lite rundt merke
med navnet Neptune, med én liten prikk oppe til venstre med navnet
Nereid.](images/neptune-opposition-2026-1.png)

*Opposisjonen i 2026 ved 1°: Nereid klar og tegnet, Triton på merket og
omtalt. Siden er på engelsk.*

**2045-11-04** står Triton på sitt videste. Selv da er den på Neptuns
merke ved 1°, omtalt og ikke tegnet; ved 0.25° står den klar.

![Et kart på 1 grad 4. november 2045: Neptun som et lite rundt merke med
navnet Neptune, med én liten prikk like til høyre med navnet Nereid;
Triton er ikke tegnet.](images/neptune-triton-widest-2045-1.png)

*Triton på sitt videste, 2045, ved 1°: fortsatt på merket, omtalt; Nereid
der den står. Siden er på engelsk.*

![Et kart på 0.25 grader 4. november 2045, med sidens ord på norsk:
Neptun som et rundt merke med navnet Neptun, med Triton som en liten
prikk oppe til venstre for det og Nereid som en liten prikk til
høyre.](images/neptune-triton-widest-2045-0.25-nb.png)

*Det samme ved 0.25°, på norsk: Triton klar av merket.*

**2018-08-25** står Nereid på sitt videste, og regelen viser seg: ved 2°
ville den stå klar, og den tilbys ikke, så siden har Neptun alene og sier
ingenting om måner. **2008-03-22** er Nereid på sitt nærmeste, på merket
selv ved 0.25°, omtalt og ikke tegnet.

![Et kart på 2 grader 25. august 2018: Neptun som et lite rundt merke med
navnet Neptune blant noen få stjerner, ingen måne
tegnet.](images/neptune-nereid-widest-2018-2.png)

*Nereid på sitt videste, 2018, ved 2°: månene tilbys ikke på en så bred
side, og ingen tegnes eller omtales. Siden er på engelsk.*

![Et kart på 0.25 grader 22. mars 2008: Neptun som et lite rundt merke
med navnet Neptune, med Triton som en liten prikk til høyre; Nereid er
ikke tegnet.](images/neptune-nereid-nearest-2008-0.25.png)

*Nereid på sitt nærmeste, 2008, ved 0.25°: på merket, omtalt. Siden er
på engelsk.*

Tilstandene leses som for Jupiters, Saturns, Mars' og Uranus' måner, men
Neptuns merke endrer hva som kan vises. En måne foran planeten sin tegnes
som en prikk med ring bare på en skive som er minst **12 px** bred, og
Neptuns merke er 6: en passasje tegnes aldri. **2043-09-07** krysser
Triton Neptun; siden sier *for nær til å skjelnes i dette feltet, ikke
tegnet*, som er sannheten om et merke på 6 px. En måne bak Neptun tegnes
aldri; **2044-09-26** er Triton bak Neptun i skyggen, og siden sier *bak
Neptun, ikke tegnet*. Tritons passasjer og formørkelser kommer bare i
årstidene rundt Neptuns jevndøgn, de første av dem i 2040-årene; Nereid
er, i hver rad atlaset sammenlignet, aldri foran, bak eller i skyggen.

![Et kart på 0.25 grader 26. september 2044: Neptun som et rundt merke
med navnet Neptune, med Nereid som en liten prikk oppe til venstre;
Triton er ikke tegnet.](images/neptune-triton-behind-2044-0.25.png)

*Triton bak Neptun, 2044, ved 0.25°: ikke tegnet, og omtalt. Siden er på
engelsk.*

![Et kart på 0.25 grader på svart himmel 7. september 2043, med sidens
ord på norsk: Neptun som et hvitt rundt merke med navnet Neptun, med
Nereid som en liten prikk langt oppe til venstre; Triton, i passasje, er
ikke tegnet.](images/neptune-triton-in-front-2043-0.25-dark.png)

*Triton foran Neptun, 2043, ved 0.25°, på norsk: merket kan ikke romme en
passasje, så månen omtales, tegnes ikke.*

Før månenes år tegner siden Neptun alene, slik tabellen viser den, og
sier ingenting om måner som ikke har tall: **1999-12-31** ved 0.25° er
det Neptun og ingenting annet.

![Et kart på 0.25 grader 31. desember 1999: Neptun som et lite rundt
merke med navnet Neptune og ingenting i nærheten.](images/neptune-before-the-moons-1999-0.25.png)

*Før månenes år, 1999, ved 0.25°: Neptun alene. Siden er på engelsk.*

## Hva atlaset ikke påstår

- **Ingen overflate, ingen farge, ingen ringer.** Neptun tegnes i sidens
  blekk og bunnfarge, fullt og rundt, uten markeringer, farge, tekstur
  eller ring. Ingenting atlaset beregner, måler en farge, og atlaset har
  ikke noe tall for en ring.
- **Symbolet er ikke størrelsen.** Neptun er alltid merket på 6 px, som
  sier hvor Neptun er og ingenting om hvor stor den ser ut; månene er
  alltid symboler.
- **Å se den på siden er ikke å se den på himmelen.** Høydene er
  luftløse, og atlaset sier ingenting om hvorvidt Neptun eller månene kan
  ses der du står; ingen lysstyrke skrives ut for noe legeme. Nereid er
  det svakeste legemet atlaset noen gang har tegnet, og det er derfor den
  tegnes bare der Neptun er sidens emne.
- **Beregnet geometri, ikke observasjon.** En måne foran, bak eller i
  skyggen er geometrien ved tidspunktet du valgte. En månes skygge på
  Neptun beregnes ikke, og ingen tegnes.
- **Ingen hendelser.** Atlaset tar ut tidspunkter; det leter ikke etter
  opposisjoner, konjunksjoner, passasjer eller formørkelser, og forutsier
  ikke tidene deres. Espenaks publiserte opposisjoner og konjunksjoner
  er en grov kontroll, funnet mellom sytten minutter før og
  **+114 minutter** etter timen han oppgir - almanakkens egen presisjon
  for Neptun, ikke en påstand om atlaset.
- **Månenes år.** Triton og Nereid beregnes bare for 2000 til 2100; Neptun
  selv for 1900-2100. Neptuns seks indre måner og de ytre irregulære
  månene beregnes ikke i det hele tatt.

*Tekniske følgedokumenter: `docs/decisions/neptune-system.md` (tallene og
kontrakten deres), `docs/decisions/neptune-cartography.md` (merket,
månene og vedtakene), `docs/decisions/neptune-handover.md` (den avklarte
numeriske kontrakten på ett sted),
`docs/studies/neptune-cartography/measurements.md` og
`docs/studies/neptune-on-the-chart/measurements.md` (målingene denne
artikkelen siterer).*
