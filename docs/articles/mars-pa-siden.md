# Mars på siden

*Hva atlaset tegner av Mars, Phobos og Deimos, hvorfor Mars som regel er
et symbol og noen ganger en skive, hvordan fasen og de to små månene skal
leses, og hva atlaset ikke påstår*

## En planet som skifter størrelse

Mars er planeten som skifter størrelse mest. Nær Solens side av himmelen
er den en liten, fjern skive; ved en nær opposisjon, når jorden tar den
igjen, er den sju ganger bredere. Gjennom 1900-2100 finner atlaset den
mellom **3.50″** og **25.10″** i tverrmål. JUranometria 5.5 beregner
Mars og de to månene, Phobos og Deimos, for ditt sted på jorden og ditt
tidspunkt, gir dem en Mars-tabell ved siden av de andre planetenes, og
tegner dem på samme side som fiksstjernene. Denne artikkelen handler om
hvordan det atlaset tegner, skal leses.

Atlasets egen redegjørelse for beslutningene står i
`docs/decisions/mars-system.md` og `docs/decisions/mars-cartography.md`;
denne artikkelen forklarer dem, den erstatter dem ikke.

## Hvor tallene kommer fra

Ingenting hentes fra nettet. Mars' plass kommer fra **DE440**, JPLs
planetefemeride, som allerede er kilden til atlasets Sol, Måne, Merkur
og Venus: Mars-barysenteret, tyngdepunktet for Mars og månene, gjennom
**1900–2100**, i **709 632 bytes**. Barysenteret står for planeten fordi
Mars' sentrum aldri ligger mer enn **0.210 m** fra det.

Phobos og Deimos kommer fra NAIFs satellittefemeride **MAR099**, i en
Mars System-pakke på **43 951 104 bytes**. Den dekker fra 2000 og frem
til, **men ikke med**, det første øyeblikket i 2050, så tabellen og
kartet gir månene for **2000 til 2049**; utenfor de årene vises Mars
alene. Den lengre utgaven av samme efemeride, **1.2 GB**, ble ikke tatt
inn.

Ingen utgitt fil ble endret for Mars: Solens, Månens, Merkurs, Venus',
Jupiters og Saturns filer er byte for byte de samme som tidligere
versjoner leverte.

## Holdt opp mot en autoritet

Tallene kontrolleres, de tas ikke på tro. Atlasets Mars ble sammenlignet
med **JPL Horizons** på **36 Horizons-svar**, for flere observatører og
ukene gjennom hele intervallet. Der klokkekorreksjonen er kjent nøyaktig,
fra 1972 til **2027-06-28**, stemmer Mars' plass blant stjernene til
**0.0004″** og avstanden til **0.179 km**; månenes forskyvning fra Mars
stemmer til **0.0006″**, og tilstandene deres - klar, foran, bak, i
skyggen - på **3 777 av 3 780** rader for Phobos og **3 780 av 3 780**
for Deimos. De tre Phobos-radene som avviker, ligger alle etter
**2027-06-28**, der forskjellen mellom atomtid og jordens rotasjon er en
prognose; der kan Mars' plass avvike med opptil **4.0278″** innen 2100.
Det er klokken, ikke planeten.

Én korreksjon betyr mer for Mars enn for de ytre planetene: lyset som
bøyes når det passerer Solen. Uten den bommer den tilsynelatende plassen
på Horizons med **0.346″**; med den, med **0.130″**.

## Fase og helning

Mars er en ytre planet, så den er aldri en sigd: atlaset finner den
aldri mindre enn **83.86 %** belyst. Tabellen navngir fasen med de samme
ordene som for Venus og Merkur, og for Mars forekommer bare tre: *mer
enn halvt belyst*, *nær full* og *bak Solen*. Den gir også den belyste
siden - retningen til den lyse randen - der den er veldefinert, og sier
*ikke veldefinert* nær full, der det ikke finnes noen kant å peke på.

Helningen er hvor langt Mars' nord- eller sørpol heller mot deg, målt
over polen IAU definerer, *planetosentrisk*. Horizons skriver ut en annen
breddegrad, den planetodetiske på Mars' flattrykte klode; omregnet
stemmer de to innenfor **0.013″**, og atlaset viser aldri Horizons' tall
som helningen.

## Det navngitte tidspunktet

Raden atlaset holder som sin «i dag», er **2026-10-25 12:00** UTC i Oslo.
Mars står på **09h 11m 17.0s**, **+17° 45′ 47″**, **16.98°** over den
luftløse horisonten, **1.484474 AU** unna. Den er **6.3″ / 6.3″** i
tverrmål, ekvator og poler, **89.6 %** belyst og *mer enn halvt belyst*,
**76.8°** vest for Solen på morgenhimmelen, med den belyste siden mot
**108°** (ØSØ), nordpolen på **0.2°**, og en helning på **19.37°**
(nordpolen mot oss).

## Fra tallene til kartet

Kartet tegner Mars der tallene plasserer den, med bryteren *Mars på
kartet* i Vis ▸ Solsystem og i Kontrollerens Mars-gruppe - av til du
velger den, og deretter husket. Regelen er den samme som for Venus og
Merkur: **Mars tegnes som det største av den sanne skiven og et
kartografisk symbol på 6 px**, og begge får fasen slik tabellen oppgir
den.

På de fleste sider er det symbolet. Ved den nære opposisjonen
**2003-08-28**, da Mars var **25.1″** i tverrmål, var den sanne skiven
under én piksel ved 8°: siden tegner merket på 6 px, sier *et
kartografisk symbol, ikke Mars' tilsynelatende diameter*, og maler det
under stjernene, så det aldri skjuler en.

![Et kart på 8 grader sentrert på Mars 28. august 2003, med sidens ord på
norsk: et lite rundt merke med navnet Mars nær midten, stjerner som
prikker i ulike størrelser, rutenettet.](images/mars-opposition-2003-8-nb.png)

*Opposisjonen i 2003 ved 8°, på norsk: symbolet på 6 px.*

Mars' største skive når merket på 6 px først ved omtrent **1.05°**, så på
den vanlige stigen av felt tar den sanne skiven over bare ved 1°, og bare
nær de nærmeste opposisjonene: i 2003 er den **6.3 px** der, en ekte
skive, ikke lenger et symbol, og omtalt bare som *Mars*.

![Et kart på 1 grad sentrert på Mars 28. august 2003: en liten rund skive
med navnet Mars midt i et ellers tomt rutenett, ingen stjerne lyssterk
nok i nærheten.](images/mars-opposition-2003-1.png)

*Opposisjonen i 2003 ved 1°: den sanne skiven, litt over 6 px.*

Når Mars går bak Solen, males Solens skive over den. **1912-11-05**,
timen tjenesten finner Mars dypest bak Solens skive det året, er merket
dekket, og siden sier *bak Solen*.

![Et kart på 1 grad: Solen som en stor hvit skive med kraftig svart kant
og en prikk i sentrum, med navnet Sun, som fyller den nedre midten av
siden; Mars skjult under den; én enkelt stjerne øverst til
venstre.](images/mars-behind-the-sun-1912-1.png)

*Bak Solen, 1912, ved 1°: merket er dekket, og siden sier det. Siden er
på engelsk.*

## Nærfeltene

Mars er et av legemene som zoom inn tilbyr *nærfeltene* for, **0.5°** og
**0.25°**, under det vanlige gulvet på 1°. De forstørrer ikke stjernene;
de finnes for at en skive på noen buesekunder kan tegnes som seg selv.
Ved opposisjonen i 2003 er Mars **12.6 px** ved 0.5° og **25.1 px** ved
0.25°.

![Et kart på 0.25 grader på svart himmel 28. august 2003: Mars som en
hvit skive i midten, med navnet Mars, og to små hvite prikker nær den
med navnene Phobos og Deimos.](images/mars-opposition-2003-0.25-dark.png)

*Opposisjonen i 2003 ved 0.25° på svart himmel: den sanne skiven omtrent
25 px i tverrmål, og begge månene.*

Fasen er virkelig, men liten. Den dagen i 2018 da den mørke delen av
skiven var bredest på himmelen, **2018-10-06**, var Mars **87.9 %**
belyst og **15.1″** i tverrmål: ved 0.25° er den mørke siden et smalt
bånd langs randen bort fra Solen. På et symbol på 6 px er den mørke delen
**0.97 px** når Mars er på sitt slankeste, under én piksel.

![Et kart på 0.25 grader 6. oktober 2018: Mars som en liten rund skive
med navnet Mars, den mørke siden bare en flik langs den ene kanten, med
Phobos som en prikk til venstre og Deimos som en prikk nedenfor til
høyre.](images/mars-gibbous-2018-0.25.png)

*Mars med den bredeste mørke siden i 2018, ved 0.25°: selv da er den
mørke siden bare et par piksler langs randen bort fra Solen.*

## Phobos og Deimos

Hver måne er et symbol på **3 px** på sin egen plass blant stjernene,
avgjort for seg. I motsetning til Jupiters måner har de ikke noe «gulv»:
**en av Mars' måner tegnes bare der merket står klar av Mars' tegnede
merke og av den andre månen, i hvert felt.** Det er fordi de holder seg
nær. Fra 2000 til 2049, ukentlig, står Phobos **0.26″–34.22″** fra Mars'
sentrum og Deimos **0.47″–84.79″**, og når **35.02″** og **86.77″** ved
opposisjonen i 2003; ingen av dem er noen gang **0.1″** i tverrmål. I de
vanlige feltene står begge som regel på Mars' eget merke, og siden lar
dem være ute og sier hvorfor.

**2018-07-27**, nær Deimos' største elongasjon, står Deimos **70.7″** fra
Mars' sentrum. Ved 0.25° står begge månene klar og tegnes; ved 8° står
begge på Mars' merke, ingen av dem tegnes, og siden sier *for nær til å
skjelnes i dette feltet, ikke tegnet*.

![Et kart på 0.25 grader 27. juli 2018: Mars som en skive med navnet
Mars, Phobos som en liten prikk like til venstre og Deimos som en liten
prikk lenger til venstre, hver med navn.](images/mars-deimos-wide-2018-0.25.png)

*Deimos nær sin største avstand, 2018, ved 0.25°: begge månene klar og
tegnet.*

![Et kart på 8 grader 27. juli 2018: Mars som et lite rundt merke med
navnet Mars blant stjerner, ingen måne tegnet.](images/mars-deimos-wide-2018-8.png)

*Samme tidspunkt ved 8°: begge månene på Mars' merke, ikke tegnet, og
omtalt.*

Tilstandene leses som for Jupiters og Saturns måner. En måne foran Mars
er en prikk med en ring i sidens bunnfarge, malt over skiven - bare der
den tegnede skiven er minst **12 px** bred, så ringen kan ses.
**2001-06-30** krysser Phobos en skive på **20.5″**, **6.9″** fra
sentrum, og ved 0.25° viser og sier siden det: *foran Mars*.

![Et kart på 0.25 grader 30. juni 2001: Mars som en skive med navnet
Mars og en liten prikk med ring på den, Phobos, og Deimos som en prikk
lenger unna oppe til høyre.](images/mars-phobos-in-front-2001-0.25.png)

*Phobos foran Mars, 2001, ved 0.25°: prikken med ring i bunnfargen, malt
over skiven.*

En måne i Mars' skygge er en hul ring. **2018-05-01**, før opposisjonen
det året, falt skyggen ved siden av skiven, og begge månene sto i den,
klar av Mars: to hule ringer, hver omtalt *i Mars' skygge*. En måne bak
Mars tegnes aldri; siden nevner den og sier *bak Mars, ikke tegnet*.

![Et kart på 0.25 grader 1. mai 2018: Mars som en skive med navnet Mars,
med to små hule ringer like til høyre, Phobos og Deimos, i Mars'
skygge.](images/mars-phobos-shadow-2018-0.25.png)

*Begge månene i Mars' skygge, 2018, ved 0.25°: hule ringer.*

Etter månenes år tegner siden Mars alene, slik tabellen viser den, og
sier ingenting om måner som ikke har tall: **2051-03-01** ved 0.25° er
det Mars og ingenting annet.

![Et kart på 0.25 grader 1. mars 2051: Mars som et lite rundt merke med
navnet Mars og ingenting i nærheten.](images/mars-after-the-moons-2051-0.25.png)

*Etter månenes år, 2051, ved 0.25°: Mars alene.*

## Hva atlaset ikke påstår

- **Ingen overflate, ingen farge.** Mars tegnes i sidens blekk og
  bunnfarge, med fase, uten markeringer, polkapper, farge eller tekstur.
  Ingenting atlaset beregner, måler en farge.
- **Symbolet er ikke størrelsen.** På de fleste sider er Mars merket på
  6 px, som sier hvor Mars er og ingenting om hvor stor den ser ut;
  månene er alltid symboler.
- **Å se den på siden er ikke å se den på himmelen.** Høydene er
  luftløse, og atlaset sier ingenting om hvorvidt Mars eller månene kan
  ses der du står; ingen lysstyrke skrives ut for noe legeme. Phobos og
  Deimos er svært svake og svært nær en lys planet.
- **Beregnet geometri, ikke observasjon.** En måne foran, bak eller i
  skyggen er geometrien ved tidspunktet du valgte. En månes skygge på
  Mars beregnes ikke, og ingen tegnes.
- **Ingen hendelser.** Atlaset tar ut tidspunkter; det leter ikke etter
  opposisjoner, nærmeste avstander, passasjer eller formørkelser, og
  forutsier ikke tidene deres. Espenaks publiserte opposisjoner og
  konjunksjoner er en grov kontroll, funnet innenfor **tre timer** fra
  timen han oppgir - almanakkens egen presisjon for Mars, ikke en
  påstand om atlaset.
- **Månenes år.** Phobos og Deimos beregnes bare for 2000 til 2049; Mars
  selv for 1900-2100.
- **Fem planeter.** Merkur, Venus, Mars, Jupiter og Saturn er planetene
  i versjon 5.5.

*Tekniske følgedokumenter: `docs/decisions/mars-system.md` (tallene og
kontrakten deres), `docs/decisions/mars-cartography.md` (merket, månene
og vedtakene), `docs/decisions/mars-handover.md` (den avklarte numeriske
kontrakten på ett sted), `docs/studies/mars-system/table-measurements.md`
og `docs/studies/mars-on-the-chart/measurements.md` (målingene denne
artikkelen siterer).*
