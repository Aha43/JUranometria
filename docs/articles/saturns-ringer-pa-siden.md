# Saturns ringer på siden

*Hva atlaset tegner av Saturn, ringene og sju av månene, hvordan ringene
vender seg gjennom årene, hvordan figuren og merkene skal leses, og hva
atlaset ikke påstår*

## En planet som skifter form

Hver av de andre planetene i atlaset er en skive, mer eller mindre
belyst. Saturn er en figur: en flattrykt klode inne i to flate ringer
som er langt bredere enn selve planeten, og ringene ligger ikke stille.
Gjennom Saturns lange år ser jorden dem ovenfra og nedenfra etter tur -
vidt åpne mot den ene siden, så lukket til en strek, så åpne mot den
andre. JUranometria 5.4 beregner Saturn og sju av månene for ditt sted
på jorden og ditt tidspunkt, gir dem en Saturn-tabell ved siden av
Jupiters, og tegner figuren og månene på deres sanne plasser på samme
side som fiksstjernene. Denne artikkelen handler om hvordan den figuren
og de merkene skal leses.

Atlasets egen redegjørelse for beslutningene står i
`docs/decisions/saturn-system.md` og
`docs/decisions/saturn-cartography.md`; denne artikkelen forklarer dem,
den erstatter dem ikke.

## Hvor tallene kommer fra

Ingenting hentes fra nettet. Saturns sentrum og månene kommer fra NAIFs
efemeride **sat441**, JPLs løsning for Saturns satellitter, skåret til
en Saturn-pakke ved siden av atlasets andre pakker: selve Saturn i **16
516 096 bytes** som dekker **1900-2100**, månene i **62 829 568 bytes**
som dekker **2000-2100**, **79 345 664 bytes** i alt. Ingen utgitt fil
ble endret for den: Solens, Månens, Merkurs, Venus' og Jupiters filer er
byte for byte de samme som tidligere versjoner leverte.

Saturns størrelse er IAUs: radiene **60268, 60268 og 54364 km** - de to
ekvatoriale aksene like, den polare en tidel kortere - og tabellen gir
den tilsynelatende bredden både over ekvator og fra pol til pol, slik du
ser den ved det tidspunktet. Heller ikke ringene er et bilde. De er to
flate ringflater med kanter lest fra tabellen *Rings of Saturn* hos PDS
Ring-Moon Systems Node: fra **74 491 km** til **117 570 km**, C-ringens
indre kant til B-ringens ytre kant, og fra **122 050 km** til **136 770
km**, Cassini-delingens ytre kant til A-ringens ytre kant. Mellom dem
ligger selve Cassini-delingen. Ingenting annet av ringsystemet tegnes.

De sju månene er de vedtatte: **Mimas, Enceladus, Tethys, Dione, Rhea,
Titan og Iapetus**. Hyperion, den svakeste og den mest uregelmessige, er
utelatt i denne versjonen.

## Holdt opp mot en autoritet

Tallene kontrolleres, de tas ikke på tro. Atlasets Saturn ble
sammenlignet med **JPL Horizons** på de lagrede svarene - **74
Horizons-svar** - for fem observatører og det geosentriske tilfellet,
ved de navngitte hendelsene og ukene gjennom hele intervallet. Der
klokkekorreksjonen er kjent nøyaktig, fra 1972 til **2027-06-28**,
stemmer Saturns plass blant stjernene til **0.0000″** og avstanden til
**0.22 km**; månenes forskyvning fra Saturn stemmer til **0.0006″**, og
tilstandene deres - klar, foran, bak, i skyggen - på **1 819 av 1 819**
rader for hver måne. Etter **2027-06-28** er forskjellen mellom atomtid
og jordens rotasjon en prognose, og Saturns plass kan avvike med opptil
**0.6840″** innen 2100: det er klokken, ikke planeten.

Saturns passasjer gjennom sitt eget ringplan er der tallenes ærlighet
betyr mest, fordi det er der ringene forsvinner. Jorden krysset
ringplanet **2025-03-23 17:51** UTC og Solen **2025-05-06 15:35**, sett
fra Oslo, begge i samsvar med Horizons til minuttet; alle **44**
passasjene i intervallet, **30 for jorden og 14 for Solen**, stemmer
innenfor et minutt gjennom de nøyaktig kjente årene og glir til **2
minutter innen 2098**. Men atlaset sier rett ut hva det samsvaret er
verdt: Horizons svarer fra den samme sat441-familien, og ingen uavhengig
publisert autoritet kontrollerer passasjene. De er bare prøvd mot
Horizons.

## Ringenes åpning

Tabellens viktigste nye ord er ringenes åpning, **B**: hvor langt
ringene heller åpent mot deg, i grader, og hvilken side du ser. Det er
den sanne vinkelen mellom synslinjen din og ringplanet,
*planetosentrisk*, målt over Saturns pol. Horizons skriver en annen
breddegrad, den planetodetiske, en breddegrad på Saturns flattrykte
klode; de to skiller seg med opptil **18 196″**, fem grader, og atlaset
viser aldri det tallet som åpningen. Den største åpningen i intervallet
er **−26.995°** den **2003-04-07**, der Horizons' planetodetiske tall
ville si **−32.049°**.

![Et diagram over ringenes åpning mot jorden i Oslo, ukentlig fra 1900
til 2100, med engelske etiketter: en heltrukket kurve som svinger mellom
omtrent pluss og minus 27 grader sju ganger, en stiplet kurve som
svinger lenger ut, til omtrent 32 grader, og røde prikker på nullinjen
der kurven krysser.](images/saturn-ring-opening-1900-2100.png)

*Ringenes åpning mot jorden, hver uke i 1900-2100: atlasets
planetosentriske B (heltrukket) og Horizons' planetodetiske breddegrad
(stiplet). Hver rød prikk er en uke da åpningen skiftet fortegn - en
passasje av ringplanet for jorden. Diagrammet har engelske etiketter.*

Det finnes en andre åpning, **B′**, mot Solen: hvilken side Solen lyser
opp. Atlaset tar den der Saturn ser Solen, med lyset forsinket og
aberrert av Saturns egen bevegelse; uten den korreksjonen ville Solens
passasjer falle **1.31-1.33 timer** for tidlig. Når B og B′ har motsatt
fortegn, ser du på siden Solen ikke lyser opp - *den ubelyste siden* -
og tabellen sier det. I de ukentlige Oslo-radene for 1900-2100 skjer det
i **248 av de 10 488**.

## Det navngitte tidspunktet

Tidspunktet atlaset måler seg selv mot, er raden i 2026 som ligger
nærmest jorden, **2026-10-05 00:00** UTC, i Oslo. Saturn står på **00h
44m 16.0s**, **+1° 48′ 29″**, **31.18°** over den atmosfæreløse
horisonten, **8.434258** AE borte. Kloden er **19.7″ / 17.8″** bred,
over ekvator og mellom polene, **100.0 %** belyst, slik en ytre planet
nesten alltid er, med nordpolen mot **3.2°**. Ringene åpner seg
**7.43°** mot oss og **7.71°** mot Solen, begge mot sørsiden - siden vi
ser, er den belyste - og ytterkanten deres er **44.7″ × 5.8″** på
himmelen.

![Saturn-kortet 2026-10-05 00:00 UTC for Oslo: rektascensjon 00h 44m
16.0s, deklinasjon +01° 48′ 29″, høyde 31,2°, asimut 195,3°, avstand
8,434 AU, tilsynelatende diameter 19,7″ ganger 17,8″, 100,0 % belyst,
nordpol 3,2°, ringenes åpning 7,43° mot observatøren og 7,71° mot Solen,
begge mot sørsiden, ringene 44,7″ ganger
5,8″.](images/saturn-card-nb-NO.png)

*Saturn-kortet ved det navngitte tidspunktet: hver linje fra tjenesten.*

## Fra tallene til kartet

Kartet tegner Saturn der tallene plasserer den, gjennom bryteren *Saturn
på kartet* i Vis ▸ Solsystem, etter Jupiters, og i Kontrollerens
Saturn-gruppe - av til du velger den, deretter husket. Regelen er
Jupiters, brukt på hele figuren: **Saturn tegnes som sin sanne klode og
sine sanne ringer der ringene er minst 6 px brede, og som et
kartografisk symbol på 6 px der de ikke er det.** Det er en bryter, ikke
en glidende overgang: en sirkel kan ikke vokse til en figur med ringer.

Gjennom århundret spenner ringenes ytterkant fra **34.13″** til
**46.97″** og kloden fra **15.04″** til **20.70″**, så på atlasets sider
på 900 piksler tar den sanne figuren over bare ved de smaleste feltene.
Ved det navngitte tidspunktet tegner hvert felt fra 36° ned til 2°
symbolet på 6 px: det sier hvor Saturn er og ingenting om størrelsen,
det males under stjernene så det aldri skjuler en, og den opplesbare
beskrivelsen sier *et kartografisk symbol, ikke Saturns tilsynelatende
diameter*.

![Et kart på 8 grader sentrert på Saturn 5. oktober 2026 kl. 00:00 UTC:
et lite rundt merke merket Saturn nær midten, en liten prikk merket
Iapetus like til høyre for det, stjerner som prikker og galakser som små
grå ellipser spredt over siden, og
rutenettet.](images/saturn-card-8.png)

*Det navngitte tidspunktet ved 8°: symbolet på 6 px, og Iapetus, langt
nok ute til å stå klar av det.*

Ved 1° tar den sanne figuren over, **12.2 × 4.5** px blekk: fortsatt
liten, men en figur med ringer, ugjennomsiktig, og opplest bare som
*Saturn*.

![Et kart på 1 grad 5. oktober 2026 kl. 00:00 UTC: en bitte liten
flattrykt figur med ringer merket Saturn, med en klynge av små prikker
og navn rundt seg - Enceladus, Dione, Titan, Rhea, Mimas - en større grå
ellipse oppe til høyre, og Iapetus nede til
høyre.](images/saturn-card-1.png)

*Det navngitte tidspunktet ved 1°: den sanne figuren, med månene rundt
den. Den grå ellipsen er en galakse fra katalogen, som på siden ved 8°,
ikke et legeme.*

## Nærfeltene

Saturn er et av legemene som zoom inn tilbyr *nærfeltene* for, **0.5°**
og **0.25°**, under det vanlige 1°-gulvet. De forstørrer ikke stjernene;
de finnes for at en figur noen titalls buesekunder bred skal kunne
tegnes som seg selv. Ved det navngitte tidspunktet er figuren **22.3 ×
8.9** px ved 0.5° og **44.6 × 17.8** px ved 0.25°, og der leses ringene
som ringer: halvdelen nærmest oss tegnet over kloden, den fjerne
halvdelen bak den, og Cassini-delingen skåret ut mellom dem bare der den
er minst én piksel bred.

![Et kart på 0,25 grader på svart himmel 5. oktober 2026: Saturns klode
inne i de smalt åpnede ringene, hvitt på svart, merket Saturn, med små
hvite prikker og navn - Mimas, Enceladus, Dione, Rhea, Titan - rundt
seg.](images/saturn-card-0.25-dark.png)

*Det navngitte tidspunktet ved 0.25° på svart himmel. Tethys er bak
Saturn og tegnes ikke; Iapetus er utenfor siden.*

## Ringene gjennom årene

De samme reglene tegner ringene ved hver åpning. Vidt åpne mot
nordsiden, **2076-10-10 00:00** UTC, er B **+26.97°**; vidt åpne mot
sørsiden, **2003-04-05 00:00**, er den **−26.99°**. Hvilken halvdel av
ringene som ligger foran kloden, skifter med siden: den nære halvdelen
ligger vekk fra den projiserte nordpolen når nordsiden sees, og mot den
når sørsiden sees.

![Et kart på 0,25 grader 10. oktober 2076: Saturns klode inne i vidt
åpne ringer, merket Saturn, med de sju månene som små prikker med
navnene rundt seg.](images/saturn-open-north-0.25.png)

*Ringene videst mot nordsiden, 2076, ved 0.25°.*

![Et kart på 0,25 grader på svart himmel 5. april 2003, på norsk: kloden
inne i vidt åpne ringer, hvitt på svart, merket Saturn, med små prikker
og navn rundt seg.](images/saturn-open-south-0.25-dark.png)

*Ringene videst mot sørsiden, 2003, ved 0.25° på svart himmel, siden på
norsk.*

**På kant.** Når ringplanet går gjennom jorden, er ringene bare en tynn
strek: på atlasets sider tegnes en lilleakse for ringene under 2 px som
en strek på **1 px** gjennom kloden, og ved 1° gjelder det i **30.4 %**
av århundrets uker. Ved jordens passasje **2025-03-23 17:51** UTC er
åpningen **+0.00°**, og tabellen sier *på kant*.

![Et kart på 0,25 grader på norsk 23. mars 2025 kl. 17:51 UTC: en liten
sirkel, kloden, med en tynn rett strek gjennom seg, ringene sett på
kant, merket Saturn, med månene som prikker tett langs
streken.](images/saturn-edge-on-0.25.png)

*Jordens passasje av ringplanet, 2025, ved 0.25°: ringene en strek.*

**Den ubelyste siden.** Mellom jordens passasje og Solens, seks uker
senere, sto jorden og Solen på hver sin side av ringplanet. Den
**2025-05-06 00:00** UTC, timer før Solens passasje, er åpningen mot oss
**−2.19°**, sørsiden, og mot Solen **+0.01°**, nordsiden: vi ser siden
Solen ikke lyser opp. Siden tegner de ringene med dempet blekk, så siden
sier det samme som tabellen.

![Et kart på 0,25 grader 6. mai 2025: en liten klode med en svak, nesten
flat ringstrek gjennom seg, merket Saturn, med månene som prikker like
ved.](images/saturn-unlit-0.25.png)

*Den ubelyste siden sett, 2025, ved 0.25°: ringene dempet.*

## Månene

Hver av de sju månene er et symbol på **3 px** på sin egen plass blant
stjernene, og hver avgjøres for seg, så ett trangt par aldri skyver bort
de andre. En måne bak Saturn tegnes ikke. Ved 1° og i nærfeltene tegnes
hver av de andre månene på sin nøyaktige plass, med merket oppå figurens
eller en annens der den virkelige stillingen er så tett; i videre felt
tegnes en måne bare der merket står klar av figuren og av månene som
allerede er tegnet. Ved det navngitte tidspunktet er det **0 av 7**
måner ved 36°, **1 av 7** ved 8° - Iapetus - og **6 av 7** ved 1°, siden
Tethys er bak Saturn. Hver måne er langt mindre enn en piksel i hvert
felt - Titan, den største, er høyst **0.8849″** - så merket er alltid et
symbol, og den opplesbare beskrivelsen sier det.

Tilstandene leses som for Jupiters måner: klar av Saturn, en fylt prikk;
foran Saturn, en prikk med en ring i sidens bunnfarge, malt over figuren -
i felt videre enn 1° bare der selve kloden er minst **12 px** bred; i
Saturns skygge, helt eller delvis, en hul ring; bak, ikke tegnet. De
indre månene holder seg tett inntil: Mimas er aldri lenger enn **32.2″**
fra Saturns sentrum, der ringene rekker ut til **23.5″**.

![Et kart på 0,25 grader på norsk 1. juli 2026 kl. 03:00 UTC: Saturns
klode og ringer med en liten prikk med ring på kloden, Mimas foran den,
og de andre månene som prikker med navnene
rundt.](images/saturn-in-front-0.25.png)

*Mimas foran Saturn, **2026-07-01 03:00** UTC, ved 0.25°; siden sier
*foran Saturn*.*

![Et kart på 0,25 grader på svart himmel 1. juli 2026 kl. 14:00 UTC:
Saturns klode og ringer, hvitt på svart, med en hul ring like under
figuren, Enceladus i Saturns skygge, og andre måner som
prikker.](images/saturn-in-shadow-0.25-dark.png)

*Enceladus i Saturns skygge, **2026-07-01 14:00** UTC, ved 0.25° på
svart himmel: en hul ring, opplest *i Saturns skygge*.*

Hver tegnet månes navn prøves i de samme boksene rundt merket som hvert
legemes navn bruker, og avvises for seg der ingen er ledig, så nær
figuren mangler ofte noen navn: tabellen gir dem.

## Hva atlaset ikke påstår

- **Ingen overflate.** Kloden og ringene er en figur, ikke et bilde:
  ingen bånd, ingen farge, ingen tekstur, ingen skyggelegging av
  ringene utover de to flate tonene figuren tegnes med.
- **Ingen skygger.** Klodens skygge på ringene, ringenes skygge på
  kloden og en månes skygge på Saturn er geometri denne versjonen ikke
  beregner, og ingen av dem tegnes.
- **Ingen tilstand for at ringene skjuler en måne.** En måne hvis
  synslinje krysser ringene, tegnes bare etter rekkefølgen den males i;
  verken Horizons eller noen annen autoritet atlaset bruker, tar hensyn
  til ringene, så atlaset sier aldri at en måne er skjult av dem.
- **Symbolet er ikke størrelsen.** På de fleste sider er Saturn merket
  på 6 px, som sier hvor Saturn er og ingenting om hvor stor den ser ut;
  månene er alltid symboler.
- **Å se den på siden er ikke å se den på himmelen.** Høydene er
  atmosfæreløse, og atlaset sier ingenting om hvorvidt en ring eller en
  måne er synlig for deg; ingen lysstyrke skrives for noe legeme.
- **Ingen hendelser.** Atlaset beregner enkelttidspunkter; det leter
  ikke etter passasjer, elongasjoner eller månenes passasjer foran
  planeten, og forutsier ikke når de skjer. De navngitte tidspunktene er
  målepunkter, ikke en katalog.
- **Fire planeter.** Merkur, Venus, Jupiter og Saturn er planetene i
  versjon 5.4.

*Tekniske følgedokumenter: `docs/decisions/saturn-system.md` (tallene og
kontrakten deres), `docs/decisions/saturn-cartography.md` (figuren,
ringene, månene og avgjørelsene om dem),
`docs/decisions/saturn-handover.md` (den avgjorte numeriske kontrakten
samlet ett sted), `docs/studies/saturn-system/table-measurements.md` og
`docs/studies/saturn-on-the-chart/measurements.md` (målingene denne
artikkelen siterer).*