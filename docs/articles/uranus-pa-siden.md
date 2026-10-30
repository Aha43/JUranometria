# Uranus på siden

*Hva atlaset tegner av Uranus og de fem store månene, hvorfor Uranus
alltid er et symbol og aldri en skive, hvorfor ringene blir i tabellen,
hvordan helningen og månene skal leses, og hva atlaset ikke påstår*

## En planet som aldri vokser

Uranus er langt borte: nitten ganger jordens avstand fra Solen, og den
endrer seg knapt. Gjennom 1900-2100 finner atlaset den mellom
**3.34-4.08″** i tverrmål, en skive ingen side i atlaset kan oppløse.
JUranometria 6.0 beregner Uranus og de fem store månene - Miranda,
Ariel, Umbriel, Titania og Oberon - for ditt sted på jorden og ditt
tidspunkt, gir dem en Uranus-tabell ved siden av de andre planetenes, og
tegner dem på samme side som fiksstjernene. Denne artikkelen handler om
hvordan det atlaset tegner, skal leses, og om hvorfor så mye av Uranus
gis i tall heller enn i blekk.

Atlasets egen redegjørelse for beslutningene står i
`docs/decisions/uranus-system.md` og
`docs/decisions/uranus-cartography.md`; denne artikkelen forklarer dem,
den erstatter dem ikke.

## Hvor tallene kommer fra

Ingenting hentes fra nettet. Uranus' plass kommer fra **DE442**, JPLs
planetefemeride, slik NAIF har flettet den inn i sin satellittkjerne for
Uranus: Uranus-barysenteret, tyngdepunktet for Uranus og månene, gjennom
**1900–2100**, i **387 072 bytes**. Uranus er den første planeten i
atlaset hvis plass ikke kommer fra DE440. Grunnen er målt, ikke antatt:
sammenlignet med JPL Horizons stemmer DE442s barysenter med Horizons'
Uranus til **0.0018″**, mens DE440s bommer med **0.3779″**; de to
efemeridene legger barysenteret opptil **4 864.7 km** fra hverandre.
Solen og jorden forblir DE440s, og deres DE442-posisjoner avviker med
høyst **0.49 km**.

Barysenteret står for planeten. Uranus' sentrum ligger aldri mer enn
**45.136 km** fra det, som er **0.0035″** sett fra jorden, langt under
noe en side kunne vise. Men en avstand er en avstand: Horizons måler til
sentrum, og atlasets avstand til barysenteret får derfor avvike fra
Horizons' med **50 km**, et mål eieren endret da følgen var målt. Studien
hadde målt vinkelen og ikke kilometerne; lærdommen ble tatt med til neste
planet.

Månene kommer fra NAIFs satellittløsning **URA182.21**, i samme Uranus
System-pakke, **38 055 936 bytes**. De beregnes for **2000–2100**;
utenfor de årene vises Uranus alene.

Ingen utgitt fil ble endret for Uranus: Solens, Månens, Merkurs, Venus',
Mars', Jupiters og Saturns filer er byte for byte de samme som tidligere
versjoner leverte.

## Holdt opp mot en autoritet

Tallene kontrolleres, de tas ikke på tro. Atlasets Uranus ble sammenlignet
med **JPL Horizons** på **59 Horizons-forespørsler**, for flere
observatører og ukene gjennom hele intervallet. Der klokkekorreksjonen er
kjent nøyaktig, fra 1972 til **2027-06-28**, stemmer Uranus' plass blant
stjernene til **0.0033″** og avstanden til **43.529 km**; månenes
forskyvning fra Uranus stemmer til **0.0034″**, posisjonsvinklene deres
til **0.0051°**, og tilstandene deres - klar, foran, bak, i skyggen - på
**5 577 av 5 579** rader for Miranda og Ariel og **5 579 av 5 579** for
Titania og Oberon. Hvert avvik ligger etter **2027-06-28**, der
forskjellen mellom atomtid og jordens rotasjon er en prognose; der kan
Uranus' plass avvike med opptil **0.3194″** innen 2100. Det er klokken,
ikke planeten. Det pakkede programmet kontrollerer seg selv mot én
Horizons-rad ved hver bygging og treffer den innenfor **0.0008″**.

## Full, og lagt på siden

Uranus er en ytre planet langt fra Solen, så den er aldri annet enn full:
atlaset finner den aldri mindre enn **99.92 %** belyst, ved en fasevinkel
på høyst **3.1825°**. Tabellen gir den belyste andelen og fasevinkelen og
bruker ingen faseord i det hele tatt; det er ingenting å navngi.

Det Uranus har i stedet, er helningen. Aksen ligger nesten i banen, så
gjennom et Uranus-år ser jorden snart mot en pol, snart mot ekvator.
Gjennom 1900–2100 går den observatørvendte breddegraden fra -82.6° til
**82.6°**: to ganger på to århundrer ser jorden nesten rett ned på en pol.
Helningen tabellen gir, er *planetosentrisk*, målt over polen IAU
definerer; Horizons skriver ut den planetodetiske breddegraden på Uranus'
flattrykte klode, som avviker fra den med tusenvis av buesekunder, og de
to stemmer innenfor **0.37″** når de regnes om. Atlaset viser aldri
Horizons' tall som helningen. Polens egen plass gis som en vinkel på
himmelen, fra himmelens nord gjennom øst.

## Det navngitte tidspunktet

Raden atlaset holder som sin «i dag», er **2026-10-27 12:00** UTC i Oslo.
Uranus er **18.5757 AU** unna, **3.794″** i tverrmål, **99.983 %** belyst
ved en fasevinkel på **1.499°**, med nordpolen på posisjonsvinkel
**289.79°** og vendt mot oss, og den står **8.05°** under den luftløse
horisonten. Alle fem månene er klar av Uranus.

## Fra tallene til kartet

Kartet tegner Uranus der tallene plasserer den, med bryteren *Uranus på
kartet* i Vis ▸ Solsystem og i Kontrollerens Uranus-gruppe - av til du
velger den, og deretter husket. Regelen er den samme som for Mars:
**Uranus tegnes som det største av den sanne skiven og et kartografisk
symbol på 6 px.** For Uranus er det alltid symbolet. Den største skiven
ville nå merket på 6 px først ved et felt på **0.170°**, smalere enn noe
felt atlaset tilbyr, nærfeltene medregnet: på hver side står merket for
Uranus, tegnet fullt og rundt i sidens blekk, uten farge, og omtalt som
*et kartografisk symbol, ikke Uranus' tilsynelatende diameter*. På et
merke på 6 px ville den mørke delen av skiven være **0.005 px** og
flattrykkingen høyst **0.093 px**, ingen av dem synlige, så merket er
uten fase og rundt etter regelen.

Ved opposisjonen **2026-11-25**, da Uranus er **3.82″** i tverrmål og
vendt **73.8°** mot oss, er den sanne skiven **en tidel av en piksel**
ved 8°: siden tegner merket, kaller det et symbol, og maler det under
stjernene, så det aldri skjuler en.

![Et kart på 8 grader sentrert på Uranus 25. november 2026, med sidens
ord på norsk: et lite rundt merke med navnet Uranus nær midten, stjerner
som prikker i ulike størrelser, rutenettet.](images/uranus-opposition-2026-8-nb.png)

*Opposisjonen i 2026 ved 8°, på norsk: symbolet på 6 px.*

![Et kart på 3 grader på svart himmel: et lite hvitt rundt merke med
navnet Uranus i midten, hvite stjerner rundt det, rutenettet i
grått.](images/uranus-opposition-2026-3-dark.png)

*Det samme ved 3° på svart himmel: merket i himmelens stjerneblekk, uten
farge. Siden er på engelsk.*

Når Uranus går bak Solen, males Solens skive over den. Ved Espenaks
konjunksjonstime **2026-05-22** står Uranus **0.2°** fra Solens sentrum,
innenfor skiven: merket er dekket, og siden sier *bak Solen*.

![Et kart på 1 grad: Solen som en stor hvit skive med kraftig svart kant
som fyller den øvre midten av siden, med navnet Sun; Uranus skjult under
den; en liten prikk, Oberon, ved skivens nedre
kant.](images/uranus-conjunction-2026-1.png)

*Bak Solen, 2026, ved 1°: merket er dekket, og siden sier det. Siden er
på engelsk.*

## Ringene blir i tabellen

Uranus har ringer, ni smale klassiske ringer fra Seks-ringen til Epsilon,
den ytterste, **51 149 km** fra planetens sentrum. Atlaset kjenner dem og
tegner dem ikke. Epsilon-ringen spenner over **6.69-8.16″** på himmelen:
på den vanlige stigen av felt ligger den innenfor merket på 6 px, ved
0.5° er den fortsatt **4.08 px** i tverrmål, og først ved 0.25° rekker
den utenfor merket, med **1.08 px** på hver side. Ringene er bredere enn
merket bare ved et felt smalere enn **0.34°**, og hver rings bredde er
langt under én piksel i hvert felt. Tegnet sant ville de vært en hårstrek
like utenfor merket i ett felt, og atlaset forstørrer aldri det det
tegner. Så ringene finnes bare i tabellen: åpningen deres mot oss er
helningen tabellen allerede gir.

## Nærfeltene

Uranus er et av legemene som zoom inn tilbyr *nærfeltene* for, **0.5°** og
**0.25°**, under det vanlige gulvet på 1°. De gir Uranus ingenting -
planeten er fortsatt merket - men de er de eneste feltene der de indre
månene står for seg selv.

![Et kart på 0.25 grader på svart himmel 25. november 2026: Uranus som et
lite hvitt rundt merke i midten, med navnet Uranus, og fem små prikker
rundt det med navnene Oberon, Titania, Umbriel, Ariel og
Miranda.](images/uranus-opposition-2026-0.25-dark.png)

*Opposisjonen i 2026 ved 0.25° på svart himmel: merket, fortsatt 6 px, og
alle fem månene klar av det. Innenfor merkets omriss er hver eneste av
dets **12 av 12** piksler lys: merket tegnes fullt.*

## De fem månene

Hver måne er et symbol på **3 px** på sin egen plass blant stjernene,
avgjort for seg. Som Mars' måner og i motsetning til Jupiters har de ikke
noe «gulv»: **en av Uranus' måner tegnes bare der merket står klar av
Uranus' tegnede merke og av månene som alt er tegnet, i hvert felt.** Fra
2000 til 2100, ukentlig, står Miranda **0.1133″–10.3213″** fra Uranus'
sentrum og Oberon **0.964″–46.4638″**; ingen måne er noen gang **0.13″** i
tverrmål. I de vanlige feltene står de på Uranus' eget merke: ved 1° er
Titania klar i **30.8 %** av ukene og Oberon i **67.0 %**, de tre indre
månene aldri; ved 0.5° kommer Umbriel til i **66.9 %** av ukene; Miranda
står klar bare ved 0.25°, i **64.2 %** av ukene. Der en måne ikke tegnes,
nevner siden den og sier *for nær til å skjelnes i dette feltet, ikke
tegnet*.

**2052-03-23** står Titania og Oberon på sitt videste. Ved 1° tegnes
Titania og resten omtales; ved 0.25° står fire av de fem for seg selv;
ved 8° er ingen klar av merket, og hver omtales.

![Et kart på 1 grad 23. mars 2052: Uranus som et lite rundt merke med
navnet Uranus, med én liten prikk like nedenfor med navnet
Titania.](images/uranus-moons-widest-2052-1.png)

*Titania og Oberon på sitt videste, 2052, ved 1°: Titania klar og tegnet,
de andre omtalt. Siden er på engelsk.*

![Et kart på 0.25 grader 23. mars 2052, med sidens ord på norsk: Uranus
som et rundt merke i midten, med fire små prikker rundt det med navnene
Oberon, Titania, Umbriel og Ariel.](images/uranus-moons-widest-2052-0.25-nb.png)

*Det samme ved 0.25°, på norsk: det meste av systemet står for seg selv.*

![Et kart på 8 grader 23. mars 2052: Uranus som et lite rundt merke med
navnet Uranus blant stjerner, ingen måne tegnet.](images/uranus-moons-widest-2052-8.png)

*Det samme ved 8°: hver måne på Uranus' merke, ingen tegnet, hver omtalt.*

Tilstandene leses som for Jupiters, Saturns og Mars' måner, men Uranus'
merke endrer hva som kan vises. En måne foran planeten sin tegnes som en
prikk med ring bare på en skive som er minst **12 px** bred, og Uranus'
merke er 6: en passasje tegnes aldri. **2005-05-28** krysser Miranda
Uranus; siden sier *for nær til å skjelnes i dette feltet, ikke tegnet*,
som er sannheten om et merke på 6 px. En måne bak Uranus tegnes aldri;
**2005-03-19** er Miranda bak Uranus i skyggen, og siden sier *bak
Uranus, ikke tegnet*. En måne i Uranus' skygge ville være en hul ring der
den står klar - men en formørket måne står ved randen, innenfor skyggens
bredde, og er dermed nesten aldri klar av merket: **2006-02-04** er Ariel
i Uranus' skygge og omtales, tegnes ikke.

![Et kart på 0.25 grader 19. mars 2005: Uranus som et rundt merke med
navnet Uranus, med Oberon, Titania og Ariel som små prikker rundt det;
Miranda er ikke tegnet.](images/uranus-miranda-behind-2005-0.25.png)

*Miranda bak Uranus, 2005, ved 0.25°: ikke tegnet, og omtalt. Siden er på
engelsk.*

![Et kart på 0.25 grader på svart himmel 28. mai 2005: Uranus som et
hvitt rundt merke med navnet Uranus, med Oberon, Titania og Umbriel som
små prikker; Miranda, i passasje, er ikke
tegnet.](images/uranus-miranda-in-front-2005-0.25-dark.png)

*Miranda foran Uranus, 2005, ved 0.25°: merket kan ikke romme en
passasje, så månen omtales, tegnes ikke. Siden er på engelsk.*

![Et kart på 0.25 grader 4. februar 2006, med sidens ord på norsk: Uranus
som et rundt merke med Oberon, Titania, Umbriel og Miranda som små
prikker rundt det; Ariel, i skyggen ved randen, er ikke
tegnet.](images/uranus-ariel-shadow-2006-0.25-nb.png)

*Ariel i Uranus' skygge, 2006, ved 0.25°, på norsk: ved randen, ikke klar
av merket, omtalt.*

Før månenes år tegner siden Uranus alene, slik tabellen viser den, og
sier ingenting om måner som ikke har tall: **1999-12-31** ved 0.25° er
det Uranus og ingenting annet.

![Et kart på 0.25 grader 31. desember 1999: Uranus som et lite rundt
merke med navnet Uranus og ingenting i nærheten.](images/uranus-before-the-moons-1999-0.25.png)

*Før månenes år, 1999, ved 0.25°: Uranus alene. Siden er på engelsk.*

## Hva atlaset ikke påstår

- **Ingen overflate, ingen farge, ingen ringer i blekk.** Uranus tegnes i
  sidens blekk og bunnfarge, fullt og rundt, uten markeringer, farge,
  tekstur eller ring. Ingenting atlaset beregner, måler en farge, og
  ringene gis i tabellen.
- **Symbolet er ikke størrelsen.** Uranus er alltid merket på 6 px, som
  sier hvor Uranus er og ingenting om hvor stor den ser ut; månene er
  alltid symboler.
- **Å se den på siden er ikke å se den på himmelen.** Høydene er
  luftløse, og atlaset sier ingenting om hvorvidt Uranus eller månene kan
  ses der du står; ingen lysstyrke skrives ut for noe legeme. Månene er
  svake og nær en planet som er langt lysere enn dem.
- **Beregnet geometri, ikke observasjon.** En måne foran, bak eller i
  skyggen er geometrien ved tidspunktet du valgte. En månes skygge på
  Uranus beregnes ikke, og ingen tegnes.
- **Ingen hendelser.** Atlaset tar ut tidspunkter; det leter ikke etter
  opposisjoner, konjunksjoner, passasjer eller formørkelser, og forutsier
  ikke tidene deres. Espenaks publiserte opposisjoner og konjunksjoner
  er en grov kontroll, funnet mellom tre og en halv time før og
  **+37 minutes** etter timen han oppgir - almanakkens egen presisjon for
  Uranus, ikke en påstand om atlaset.
- **Månenes år.** De fem månene beregnes bare for 2000 til 2100; Uranus
  selv for 1900-2100.

*Tekniske følgedokumenter: `docs/decisions/uranus-system.md` (tallene og
kontrakten deres), `docs/decisions/uranus-cartography.md` (merket,
månene, ringene og vedtakene), `docs/decisions/uranus-handover.md` (den
avklarte numeriske kontrakten på ett sted),
`docs/studies/uranus-cartography/measurements.md` og
`docs/studies/uranus-on-the-chart/measurements.md` (målingene denne
artikkelen siterer).*
