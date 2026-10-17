# Venus' faser

*Hva atlaset tegner av Venus, hvorfor en planet trenger en fase på
kartet, hvordan merket skal leses, og hva atlaset ikke påstår*

## En planet som skifter form

Følg Venus i et lite teleskop gjennom noen måneder, og den holder ikke
formen: først en liten, rund skive som er mer enn halvt belyst, nær
Solen, så en halv, så en stor sigd som blir smalere mens planeten
svinger mot oss, så ingenting når den går mellom oss og Solen, og så
den samme rekken baklengs på morgenhimmelen. Galilei så det i 1610,
og det var en av observasjonene som avgjorde hvor Solen står i
solsystemet: i Ptolemaios' ordning, der Venus alltid er mellom oss og
Solen, kunne den aldri vise full fase. JUranometria 5.2 tegner Venus
med fasen - på dens sanne plass for ditt sted på jorden og ditt
tidspunkt, på samme side som fiksstjernene - og denne artikkelen
handler om hvordan det merket skal leses.

Atlasets egen redegjørelse for beslutningene står i
`docs/decisions/venus-system.md` og `docs/decisions/venus-cartography.md`;
denne artikkelen forklarer dem, den erstatter dem ikke.

## Hvor tallene kommer fra

Ingenting hentes fra nettet. Venus beregnes fra NASA/JPLs efemeride
**DE440**, den samme som atlasets Sol og Måne allerede brukte, tatt fra
den utgitte solsystempakkens egen festede kilde og skåret til en ny
medfølgende *planetpakke*: én fil for Venus, **1 242 112 bytes**, som
dekker **1900–2100**, med hver koeffisient nøyaktig slik JPL
publiserte den. En senere planet legger sin egen fil til i samme
pakke; en utgitt planets fil skrives aldri om.

Venus' størrelse er den faste kroppen: IAU-radien på **6051.8 km**,
slik NAIFs planetkonstanter oppgir den. Den kroppen ser du ikke. Det
et teleskop viser, er toppen av et permanent skydekke, **74 km**
høyere, som ville tegnet Venus **1.22 %** bredere - høyst **0.771″**,
for liten forskjell til å se på siden. Atlaset oppgir forskjellen i
tabellens forklaring og tegner aldri skyradien: den faste kroppen er
størrelsen autoritetene er enige om.

## Holdt opp mot en autoritet

Tallene kontrolleres, de tas ikke på tro. Atlasets Venus ble
sammenlignet med **JPL Horizons** på **10 771** rader - fem
observatører (Oslo, Quito, Cape Town, Alert og Chathamøyene på andre
siden av datolinjen) og det geosentriske tilfellet, ved de ni navngitte
hendelsene og hver uke eller måned gjennom hele intervallet. I årene
der klokkekorreksjonen er kjent nøyaktig, stemmer plassen blant
stjernene til **0.001″** og avstanden til **0.2 km**. Den tilsynelatende
plassen for datoen stemmer til **0.129″**; det siste tallet er ikke en
feil i Venus, men atlasets eldre modeller for presesjon og nutasjon,
den samme årsaken som Solens utgitte kontrakt målte.

Etter **2027-06-28** er forskjellen mellom atomtid og jordens rotasjon
en prognose, og tabellen merker de tidspunktene *est.*: kostnaden der
når **7.118″** innen 2100, og det er klokken, ikke planeten.

De navngitte hendelsene er Fred Espenaks almanakkrader: de fire
konjunksjonene faller **8–44** minutter ut i timen almanakken oppgir,
og de fem største elongasjonene stemmer med den
trykte vinkelen til tidels grad og med siden av Solen.

## Fasen i ord

Tabellen beskriver fasen slik en observatør ville gjort det, med ord
først og tall etterpå. Når Venus' skive møter Solens, sier tabellen
det - *foran Solen* eller *bak Solen* - og ellers bruker den fem bånd
på den belyste andelen: *nær nedre konjunksjon* under **2 %**,
*sigd*, *nær halvt belyst*, *mer enn halvt belyst*, og *nær full* fra
**98 %**. Ved siden av båndet står den belyste siden: posisjonsvinkelen
til den lyse randen og nærmeste av seksten kompassretninger. Nær
nedre konjunksjon og nær full er retningen ikke veldefinert, og
tabellen sier *ikke veldefinert* i stedet for å finne på en.

Venus' tilsynelatende diameter endrer seg mer enn noen annen planets i
atlaset: fra **9.61″** når den er lengst borte, på andre siden av
Solen, til **63.03″** når den er nærmest. Den store sigden og den lille
fulle skiven er den samme planeten ved hver sin ende av banen rundt
Solen.

## Den navngitte kvelden

Tidspunktet atlaset måler seg selv mot, er den største østlige
elongasjonen i 2026, som Espenak oppgir til **2026-08-15 06:00** UTC. I
Oslo sier tabellen at Venus er **48.7 %** belyst, *nær halvt belyst*,
**45.9°** øst for Solen på kveldshimmelen, med den belyste siden mot
**295°**, vest-nordvest - mot Solen, slik den må være. Skiven er
**24.3″** bred, og Venus er **0.686211** AE borte, omtrent
**102.656** millioner kilometer.

![Venus-tabellen 2026-08-15 06:00 UTC for Oslo: én rad med
rektascensjon 12h 27m 00.7s, deklinasjon −4° 26′ 50″, under
horisonten, avstand 0.686211 AE, tilsynelatende diameter 24.3
buesekunder, 48.7 % belyst, nær halvt belyst, 45.9° Ø, belyst side
295° vest-nordvest.](images/venus-table-nb-NO.png)

*Venus-tabellen ved det navngitte tidspunktet: én rad, hver kolonne
fra tjenesten, tidsmerknaden under.*

## Fra tallene til kartet

Kartet tegner Venus der tallene plasserer den, gjennom bryteren
*Venus på kartet* i Vis ▸ Solsystem og i Kontrollerens Venus-gruppe -
av til du velger den, deretter husket. Én regel avgjør hva du ser, og
det er den samme regelen som tegner Jupiter.

**Venus tegnes i sann størrelse der den kan sees, og som et symbol
med fase der den ikke kan det.** Ved feltene en leser vanligvis
arbeider med, er Venus' noen titalls buesekunder mindre enn en
stjernes prikk, så atlaset tegner et kartografisk symbol på **6 px**,
sier det i den opplesbare beskrivelsen - *et kartografisk symbol, ikke
Venus' tilsynelatende diameter* - og maler det under stjernene, så det
aldri skjuler en. Symbolet har fase: den belyste delen av merket er
den belyste delen av planeten, dreid gjennom sidens egen nord og øst,
så halvdelen som vender mot Solen på himmelen, vender mot Solen på
siden.

![Et 8-graders kart på norsk, sentrert nær Venus 15. august 2026 kl.
06:00 UTC: et lite, halvt belyst merke merket Venus blant spredte
stjerner, med rutenett og tittelfelt på
norsk.](images/venus-elongation-8-nb.png)

*Det navngitte tidspunktet ved 8°: symbolet på 6 px, halvt belyst,
med den belyste siden mot vest, mot Solen.*

Ved det navngitte tidspunktet er den sanne skiven i 1°-feltet
**6.1 px**, så vidt over merket, og tar over: ugjennomsiktig, Månens
skive med fase i miniatyr, ikke lenger et symbol.

![Et 1-graders kart sentrert på Venus 15. august 2026 kl. 06:00 UTC:
en liten, halvt belyst skive merket Venus nær midten av siden, to
svake stjerner, rutenettet merket i
bueminutter.](images/venus-elongation-1.png)

*Samme tidspunkt ved 1°: den sanne skiven, så vidt over merket, halvt
belyst og tegnet som seg selv.*

På den svarte himmelen tegnes det samme merket i den svarte himmelens
stjerneblekk, med den mørke siden litt over himmelens bunn, så fasen
fortsatt kan leses.

![Et 3-graders kart på den svarte himmelen ved det navngitte
tidspunktet: det halvt belyste Venus-merket blant hvite stjerner på
svart.](images/venus-elongation-3-dark.png)

*Det navngitte tidspunktet ved 3° på den svarte himmelen.*

## Å lese en konjunksjon

Det finnes ingen egen tegning for en konjunksjon; rekkefølgen
malingen skjer i, tar seg av den. Det nærmeste legemet males sist.

**En passasje.** 2012-06-06 kl. **01:00** UTC passerte Venus over
Solens skive. Ved 1° er Solen en ring rundt en fylt skive, og Venus'
sanne skive, **14.5 px**, males over den - mørk, fordi den belyste
siden vender bort fra oss - og siden sier *foran Solen* ved navnet.

![Et 1-graders kart 6. juni 2012 kl. 01:00 UTC: en stor, tykk ring,
Solen, som fyller det meste av nedre del av siden, med en liten grå
skive inne i den nær toppen, Venus, og Solens sentrumsprikk under
den.](images/venus-transit-2012-1.png)

*Passasjen i 2012 ved 1°: Venus' mørke skive inne i Solens ring.*

Ved bredere felt er Venus et symbol igjen, og et symbol males
vanligvis under stjernene - der Solens skive ville dekket det. Derfor
males et symbol foran en ugjennomsiktig skive over den skiven i
stedet, i malerekkefølge, og navnet tar den første ledige plassen ved
siden av.

![Et 8-graders kart på den svarte himmelen på norsk 6. juni 2012 kl.
01:00 UTC: Solen som en hvit ring merket Solen, med Venus' lille merke
inne i den nær kanten, merket Venus
over.](images/venus-transit-2012-8.png)

*Den samme passasjen ved 8°, på den svarte himmelen og på norsk:
merket over Solens ring, navnet over.*

**Bak Solen** dekker den samme rekkefølgen Venus: Solens skive males
etter den, og siden sier *bak Solen*.

**En nedre konjunksjon.** **2026-10-24** kl. **03:00** UTC går Venus
mellom oss og Solen. Tabellen sier **0.6 %** belyst, *nær nedre
konjunksjon*. Skiven er den største i hele synlighetsperioden,
**15.3 px** ved 1°, og den tegnes mørk med randen sin: den belyste
sigden er smalere enn en tidels piksel, og atlaset finner ikke på en
for å få fasen til å se penere ut.

![Et 1-graders kart 24. oktober 2026 kl. 03:00 UTC: en mørkegrå skive
merket Venus midt på en nesten tom
side.](images/venus-inferior-conjunction-2026-1.png)

*Nedre konjunksjon ved 1°: periodens største skive, og nesten hele den
ubelyst.*

## En hel synlighetsperiode

Atlaset holder den tegnede fasen opp mot tabellens. Fra den øvre
konjunksjonen i 2026, **2026-01-06**, hver **14. dag** gjennom den nedre konjunksjonen og videre inn i 2027, tegnes en
1°-side av produksjonskartet, og merket klippes ut av den. Pikslene i
hvert merke telles: hvor mye av det som er tegnet belyst, og hvilken
vei den belyste siden vender, lest tilbake gjennom sidens nord og øst.
Et merke på 6 px rommer **omtrent to dusin** piksler, så andelen er
grov; det som holdes, er at den tegnede halvdelen er
tabellens halvdel, og at den belyste siden vender **innenfor 45°** av
tabellens retning. Det gjør den for **alle 18** trinn der tabellen
oppgir en retning.

![Tre rader med små merker klippet fra 1-graders sider hver 14. dag
gjennom 2026 og 2027: små, fulle skiver først, så halvt belyste
merker, så større sigder som blir mørke, så tynne sigder som åpner
seg igjen på den andre siden.](images/venus-apparition-2026-27.png)

*Synlighetsperioden 2026–27, ett merke hver 14. dag, fra venstre mot
høyre. Navnene er klippet bort med rutene; sidene de kommer fra, står
i dokumentasjonen.*

## Hva atlaset ikke påstår

- **Ingen skyer og ingen overflate.** Skiven er den faste kroppen,
  tegnet som en figur, ikke et bilde; skytoppene oppgis, men tegnes
  aldri.
- **Symbolet er ikke størrelsen.** Der Venus er merket på 6 px, sier
  merket hvor Venus er og hvilken side som er belyst; det sier ingenting
  om hvor stor Venus ser ut, og den opplesbare beskrivelsen sier det.
- **Ingen oppdiktet sigd.** Nær nedre konjunksjon og nær full er
  tegningen det geometrien gir, også når det er en mørk skive eller en
  helt jevn en.
- **Ingen hendelser.** Atlaset beregner enkelttidspunkter; det leter
  ikke etter passasjer eller elongasjoner og forutsier ikke tidene
  deres. De navngitte tidspunktene er faste målepunkter, ikke en
  katalog.
- **Ingen atmosfære på jorden heller.** Høydene regnes uten
  atmosfære; refraksjon nær horisonten tas ikke med.
- **To planeter.** Venus og Jupiter er planetene i versjon 5.2.

*Tekniske følgedokumenter: `docs/decisions/venus-system.md` (tallene og
kontrakten deres), `docs/decisions/venus-cartography.md` (merket og
avgjørelsene om det), `docs/decisions/venus-handover.md` (den avgjorte
kontrakten samlet), `docs/studies/venus/table-measurements.md`,
`docs/studies/venus/measurements.md` og
`docs/studies/venus-on-the-chart/measurements.md` (målingene artikkelen
siterer).*
