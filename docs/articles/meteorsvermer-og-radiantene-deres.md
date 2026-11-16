# Meteorsvermer og radiantene deres

*Hva en radiant er og hvorfor den flytter seg, hvorfor atlaset trenger
en karttid for å plassere den, hvorfor høyden, Månen og mørket avgjør en
svermnatt, hva en oppgitt rate og et nominelt maksimum sier og ikke sier,
hvordan du bruker panelet og kartet, og hva atlaset ikke påstår*

## Et punkt banene peker tilbake mot

En meteor er lysstripen som ses når et støvkorn, oftest ikke større enn
et sandkorn, brenner opp i møtet med luften. Selve kornet er en
meteoroide; meteoren er det vi ser av slutten på det. En meteorsverm
oppstår når Jorden går gjennom en strøm av slike korn som alle følger
nesten den samme banen rundt Solen. Fordi de deler bane, møter de Jorden
side om side, i parallelle baner.

Parallelle linjer sett innenfra ser ikke parallelle ut. Jernbaneskinner
ser ut til å møtes i et punkt på horisonten; snøfnugg i billysene ser ut
til å sprute ut fra et punkt foran bilen. Meteorene i én sverm gjør det
samme. Hver stripe kan dukke opp hvor som helst på himmelen, men
forlenger du hver av dem bakover, krysser linjene hverandre i ett lite
område. Det området er **radianten**, og svermen har navn etter
stjernebildet den ligger i: Perseidene etter Perseus, Geminidene etter
Tvillingene, Gemini.

Radianten er altså en retning, ikke et sted der noe skjer. Den er
retningen strømmen kommer fra, sett fra en jord i bevegelse. Det er
ingenting å se i selve radianten; en meteor som viser seg der, kommer
rett mot deg og blir en kort stripe eller et punkt.

JUranometria 6.6, utgaven som følger etter 6.5, kan tegne den retningen
på kartet og si hva atlaset vet om den ved kartets tid og sted. Denne
artikkelen handler om hvordan begge deler skal leses. Den beskriver
atlaset slik det er bygget for 6.6; den er ikke den utgavens
utgivelsesnotat.

Atlasets egen redegjørelse for beslutningene står i
`docs/decisions/meteor-showers.md`; denne artikkelen forklarer den, den
erstatter den ikke.

## Ni svermer, én om gangen

Atlaset har ni årlige svermer: de som står både i arbeidslisten til
International Meteor Organization og i IAU Meteor Data Centers liste
over etablerte svermer, som ses om natten, og som er oppført med en rate
på ti eller mer.

| sverm | aktiv, slik kalenderen daterer den | sollengde ved maksimum | oppført rate (ZHR) |
|---|---|---|---|
| Kvadrantidene | 28. des. - 12. jan. | 283.15 | 80+ |
| Lyridene | 14. apr. - 30. apr. | 32.32 | 18 |
| Eta-akvaridene | 19. apr. - 28. mai | 45.5 | 50 |
| Sørlige delta-akvarider | 12. juli - 23. aug. | 128 | 25 |
| Perseidene | 17. juli - 24. aug. | 140.0 | 110+ |
| Orionidene | 2. okt. - 7. nov. | 208 | 20 |
| Leonidene | 6. nov. - 30. nov. | 235.27 | 15+ |
| Geminidene | 4. des. - 20. des. | 262.2 | 150 |
| Ursidene | 17. des. - 26. des. | 270.7 | 10 |

Verdiene er fra én navngitt utgave, IMO Meteor Shower Calendar 2027, og
hver visning av dem i atlaset sier det. Source: International Meteor
Organization (IMO) – www.imo.net. Posisjoner mellom de trykte radene,
tidspunkter og alt som sies for et sted, er JUranometrias egne
beregninger, og JUranometria er ikke tilknyttet eller anbefalt av IMO.

Svermene bor i ett panel, *Meteorsvermer*, som åpnes fra
Solsystem-menyen og vises i Solsystemet-fanen i Kontrolleren. Velgeren
lister de ni i årets rekkefølge og merker dem som er aktive på
karttidspunktet. Du velger én, eller ingen. Radianten til den valgte
svermen tegnes da på kartet, som et lite merke av seks korte stråler
rundt et tomt sentrum, med svermens navn ved siden av.

[![Et vanlig kart over Perseus, Kassiopeia og Sjiraffen på hvitt papir,
med et lite merke av seks korte stråler i midten, nord for
Perseus-figuren, merket
Perseids.](images/meteor-perseids-night-en-white.png)](images/meteor-perseids-night-en-white.png)

*Perseidenes radiant fra Oslo (**59.91 N, 10.75 E**) **2027-08-13**
klokken **00:00** UTC. Merket er en retning, tegnet i fast størrelse;
det bærer ingen rate og ingen styrke. Hver figur i denne artikkelen
åpnes i full størrelse når du velger den.*

[![Den samme siden på svart himmel med ordene på norsk: merket med seks
stråler i midten, merket
Perseidene.](images/meteor-perseids-night-nb-black.png)](images/meteor-perseids-night-nb-black.png)

*Den samme siden på svart himmel, på norsk. Geometrien er den samme;
bare bakgrunnen og ordene er byttet.*

Bare én sverm tegnes om gangen, og bare mens den er aktiv. En
desemberkveld kan Perseidene fortsatt være valgt, men panelet sier
*Ingen radiant tegnes eller beskrives*, og kartet viser ingen.

## Hvorfor kartet trenger en tid

En sverm kommer tilbake hvert år fordi Jorden kommer tilbake til stedet
der banen dens krysser strømmen. Det stedet er en posisjon i Jordens
bane, og kalenderen er bare en omtrentlig måte å navngi den på: et år er
omtrent et kvart døgn lengre enn 365 dager, og skuddagen retter opp
forskjellen hvert fjerde år. Meteorastronomer bruker derfor
**sollengden**, Solens ekliptiske lengde sett fra Jorden, som sier
nøyaktig hvor i banen Jorden er.

Atlaset stiller hvert spørsmål om en sverm til sollengden på
karttidspunktet, aldri til en dato. Perseidenes maksimum er oppført ved
sollengde 140.0. Solen når den på disse tidspunktene:

| år | Solen når sollengde 140.0 (UTC) |
|---|---|
| 2026 | 2026-08-13 02:00 |
| 2027 | 2027-08-13 08:11 |
| 2028 | 2028-08-12 14:28 |

Omtrent seks timer senere hvert år, og et døgn tidligere etter
skuddagen. For **5 av de ni** svermene faller maksimum på en annen
kalenderdato i 2027 enn i 2026, selv om ingenting ved svermene har
endret seg.

Det samme gjelder aktivitetsperioden. Kalenderens datoer gjøres én gang
om til et intervall i sollengde, og en sverm er aktiv når karttiden
ligger innenfor det. Panelet oppgir begge: *Aktivitetsperiode: 17. juli
til 24. aug. i kalenderen, sollengde 113.9° til 151.2°.* Det legger til at
endene er kalenderens konvensjon, riktige til omtrent et døgn. En strøm
har ingen kant; aktiviteten tiltar og avtar gradvis.

Sollengden kan også ses på kartet. Når ekliptikken vises, setter
*Sollengdemerke* i Kartvalg to små piler på ekliptikken ved Solens
ekliptiske lengde på karttidspunktet, med tallet ved siden av: det
samme tallet som panelet oppgir. *Lengdeskala* graderer linjen, slik at
en aktivitetsperiode kan leses langs den som en strekning av
ekliptikken, og merket ses gå inn i og ut av den når karttidspunktet
endres.
[Hele himmelen på ett tidspunkt](hele-himmelen-pa-ett-tidspunkt.md)
forklarer begge.

En radiant uten et tidspunkt er altså ingen posisjon. Atlaset oppgir
aldri en slik: panelet, Inspektøren og den oppleste beskrivelsen gir
alltid karttiden sammen med den.

## Radianten flytter seg

Radianten står ikke fast blant stjernene. Retningen meteorene ser ut til
å komme fra, er strømmens egen bevegelse satt sammen med Jordens, og
Jordens fartsretning dreier omtrent en grad i døgnet mens den går rundt
Solen. Radianten driver med, østover, natt for natt.

[![Det samme feltet med Perseus og Kassiopeia: merket med seks stråler,
merket Perseids, står nå langt til høyre, under Kassiopeia og over
Andromeda.](images/meteor-perseids-early-en-white.png)](images/meteor-perseids-early-en-white.png)

*Det samme feltet **2027-07-20**, tidlig i Perseidenes aktivitet.
Radianten står fortsatt godt vest for Perseus.*

[![Det samme feltet igjen: merket Perseids står litt til venstre for
plassen det hadde på den første
figuren.](images/meteor-perseids-late-en-white.png)](images/meteor-perseids-late-en-white.png)

*Og **2027-08-20**, en uke etter maksimum.*

Mellom den første av disse sidene og natten for maksimum flytter
Perseidenes radiant seg **22.1** grader over himmelen; i uken som
følger, **4.5** til. Gjennom en hel aktivitetsperiode vandrer de ni
radiantene mellom **4.0 og 37.7 grader**. En radiant slått opp for
natten for maksimum kan stå det meste av et stjernebilde unna der den
står to uker før.

Atlaset følger IMOs radianttabell, som trykker en posisjon hver femte
dag. Mellom to trykte rader flytter det radianten langs storsirkelen som
forbinder dem; utenfor den siste raden, mens svermen fortsatt er aktiv,
fører det bevegelsen videre og sier at det har gjort det. Koordinatene
oppgis til hele grader, slik kilden trykker dem. En radiant er ikke
skarpere enn det: holdt opp mot et uavhengig nettverk av videokameraer
ligger atlasets posisjoner for disse ni i verste fall **4.0°** fra den
målte radianten, og det nettverkets egen spredning er like stor.

## Hvor høyt den står

Radiantens høyde over horisonten din avgjør hvor mye av en sverm du i
det hele tatt kan se.

Står radianten under horisonten, kommer strømmen inn nedenfra, og
Jorden er i veien. Eta-akvaridene kommer fra en radiant nær
himmelekvator i Vannmannen, som fra et land i nord står opp like før
Solen.

[![Et vanlig kart over Vannmannen og Pegasus med en stiplet linje,
horisonten, tvers over øvre høyre hjørne; nesten alt, tegnet i grått,
ligger under den, blant annet merket med seks stråler, merket Eta
Aquariids (below the
horizon).](images/meteor-eta-aquariids-below-horizon-en-white.png)](images/meteor-eta-aquariids-below-horizon-en-white.png)

*Eta-akvaridenes radiant fra Oslo **2027-05-06** klokken **23:00** UTC,
**14.0** grader under den matematiske horisonten. Med Demp under
horisonten på tegnes merket og navnet dempet, som alt annet under den
stiplede linjen.*

En radiant under horisonten tegnes, kan velges og leses opp som ethvert
annet merke, og sier det i navnet: *Eta-akvaridene (under horisonten)*.

Over horisonten er høyere bedre, av en grunn som er ren geometri. En
strøm som kommer inn rett ovenfra, faller rett ned på luftlaget over
deg. En som kommer inn på skrå, spres over mer av det, slik skrått
sollys spres over mer mark. Alt annet likt følger det som når deg, sinus
til radiantens høyde. Fra Oslo er det høyeste punktet Perseidenes
radiant når i mørket natten for maksimum i 2027, **+60.3** grader, og
sinus til det er **0.87**. Ved 30 grader ville tallet vært en halv. Det
er en geometrisk faktor og ikke noe mer; det er ikke en rate for din
natt.

Høyden avhenger av hvor du er. Panelets nattsammendrag oppgir radiantens
største høyde mens Solen er mer enn 12 grader nede:

| sverm, natten for det nominelle maksimumet i 2027 | fra Oslo | fra Cape Town |
|---|---|---|
| Eta-akvaridene | -4.1 | +46.3 |
| Sørlige delta-akvarider | ikke noe slikt mørke | +71.3 |
| Perseidene | +60.3 | -1.7 |
| Geminidene | +63.0 | +23.1 |

Fra Oslo kommer Eta-akvaridenes radiant aldri
over horisonten mens himmelen er mørk; fra Cape Town (**33.93 S, 18.42
E**) er det Perseidenes som ikke gjør det. Den samme listen over svermer
er et annet meteorår på en annen breddegrad.

## Maksimum er kanskje ikke ditt

Hver sverm er oppført med sollengden der aktiviteten normalt er størst.
Atlaset kaller øyeblikket da Solen når den, det **nominelle
maksimumet**, og panelet viser det ikke finere enn oppføringen gir
grunnlag for: en lengde trykt med hundredels grad gir et tidspunkt
*kjent til omtrent ti minutter*, en trykt med tidels grad *kjent til en
time eller to*, og en trykt i hele grader bare en dato, *kjent til
omtrent et døgn*.

Knappen *Sett karttiden til nominelt maksimum* setter kartet dit, og det
den finner, er ofte en overraskelse. Maksimum er ett øyeblikk for hele
Jorden, og på ditt sted kan det øyeblikket være midt på dagen:

| sverm | nominelt maksimum 2027 (UTC) | radiantens høyde fra Oslo | Solens høyde fra Oslo |
|---|---|---|---|
| Eta-akvaridene | 2027-05-06 15:11 | -16.6 | +29.4 |
| Perseidene | 2027-08-13 08:11 | +66.8 | +33.1 |
| Geminidene | 2027-12-14 19:53 | +32.5 | -39.9 |

Fra Oslo faller **3 av de ni** nominelle maksimumene i 2027 med Solen
over horisonten og **2** med radianten under den. Perseidenes faller en
formiddag i august. Det ødelegger ikke svermen: aktiviteten til en stor
sverm er bred, og nettene på hver side av maksimum er de du planlegger
for. Men det betyr at øyeblikket alene sier lite. Sett karttiden til
maksimum, flytt den så til din egen natt, og les hva panelet sier der.

Mens en sverm er aktiv, tilbyr knappen den aktivitetsperiodens maksimum,
også etter at det er passert, og panelet sier da *er passert*. Mens den
ikke er aktiv, tilbyr knappen det neste.

Ordet *nominelt* er valgt med omhu. Dette er gjennomsnittsverdier fra en
liste. Noen år gjør en sverm noe annet: Jorden møter en tett stripe støv
som ble lagt ut ved ett bestemt omløp av opphavskometen, og i en time er
raten mange ganger den vanlige. Slike hendelser er **forutsigelser**,
laget for et navngitt år av navngitte forskere, og IMOs kalender omtaler
dem i teksten sin. Atlaset har ingen av dem. Det sier *nominelt* om hver
verdi, navngir utgaven de kommer fra, og holder ordet *forutsagt* av til
noe det ennå ikke tilbyr.

## En rate for en ideell himmel

Tallet som oftest oppgis for en sverm, er den **zenitale timeraten**,
ZHR: 150 for Geminidene, 110 for Perseidene. Det er tallet som ligner
minst på det du kommer til å telle.

ZHR er et beregnet tall: antallet svermmeteorer én ideell observatør
ville se på en time under en fullkomment klar himmel som viser stjerner
til magnitude +6.5, med radianten i senit. Hver av de betingelsene er
som regel ikke oppfylt. Radianten står ikke i senit, og alt annet likt
faller raten med sinus til høyden, slik et tidligere avsnitt beskriver. Himmelen er sjelden så
mørk; under en himmel som viser færre stjerner, viser den også færre
meteorer, og den mister mest der en sverm er rik på svake.
**Populasjonsindeksen** som står ved siden av raten, sier hvor rik den
er: jo høyere indeks, desto større andel svake meteorer, og desto mer
koster en lys himmel. Og én observatør ser bare en del av himmelen.

Atlaset trykker derfor ZHR slik kilden gjør, og sier med en gang hva den
er: *raten for en ideell mørk himmel med radianten i senit, ikke det en
observatør vil telle*. Det beregner ingen lokal rate. En ærlig lokal
rate trenger mørket på din himmel den natten, og det kjenner ikke
atlaset.

## Månen og mørket

To ting avgjør hvor mørk en svermnatt er: hvor Solen står under
horisonten, og hvor Månen er.

For Solen oppgir panelet tre varigheter, prøvd gjennom natten rundt
karttiden: hvor lenge Solen står mer enn 6, 12 og 18 grader under
horisonten. Den siste er astronomisk mørke: 18 grader er den
konvensjonelle slutten på astronomisk tussmørke. På høye bredder er det
sanne svaret ofte
*ingen*. Natten for Perseidenes maksimum i 2027, fra Oslo: *Solen mer
enn 6° under horisonten: 6.4 t; mer enn 12°: 3.8 t; mer enn 18°: ingen.*
I desember har Geminidene **12.6 timer** astronomisk mørke fra det samme
stedet.

Månen er et spørsmål om hell, og den skifter fra år til år. Panelet
oppgir den belyste andelen, fasen, høyden og avstanden fra radianten.
Ved Geminidenes nominelle maksimum, fra Oslo:

| år | Månen er belyst | grader fra radianten |
|---|---|---|
| 2026 | 25% | 146 |
| 2027 | 98% | 15 |
| 2028 | 5% | 128 |

Svermens oppførte verdier er de samme alle tre årene. Nettene er det
ikke.

[![Et vanlig kart over Tvillingene: merket med seks stråler, merket
Geminids, like over Castor, og Månen, en liten åpen sirkel, nede til
høyre for det på figurens linje, med den strekpunkterte ekliptikken
under begge.](images/meteor-geminids-evening-en-white.png)](images/meteor-geminids-evening-en-white.png)

*Geminidenes radiant fra Oslo **2027-12-14** klokken **20:00** UTC, med
en nesten full måne i det samme feltet. Kartet viser hvor Månen er; det
tegner ikke månelys.*

Panelet oppgir disse fakta og stopper der. Det kaller aldri en natt god
eller dårlig: *Ingenting her vurderer natten eller varsler været.*

## Radianten på hele himmelen

Radianten tegnes på alle slags sider, også hele himmelen. [Hele himmelen
på ett tidspunkt](hele-himmelen-pa-ett-tidspunkt.md) forklarer det
kartet; på det er en radiant ett punkt til, plassert etter posisjonen
sin og på sin egen side av horisonten.

[![Hele himmelen som en bred ellipse med planetene langs den rette
midtlinjen og horisonten som en stiplet kurve; et lite merke med seks
stråler, merket Geminids, står litt over midtlinjen, til venstre for
midten, med Månen like under
seg.](images/meteor-geminids-whole-sky-en-white.png)](images/meteor-geminids-whole-sky-en-white.png)

*Den samme karttiden på hele himmelen. Radianten, Månen og horisonten
står på én side: hva som er oppe, hva som er nede, og hva som står
mellom radianten og mørket.*

Dette er siden for spørsmålet en planlegger stiller først: på dette
tidspunktet, herfra, er radianten oppe, og hvor er Månen? *Sentrer på
radianten* i panelet åpner så det vanlige kartet rundt den.

## Hvor strømmene kommer fra

Støvet i en strøm er kastet av et opphavslegeme, og det fortsetter langs
det legemets bane. For åtte av de ni svermene navngir atlaset opphavet,
slik IAU Meteor Data Center oppgir det. De fleste er kometer, som mister
støv hver gang de passerer Solen: 109P/Swift-Tuttle for Perseidene,
55P/Tempel–Tuttle for Leonidene, 8P/Tuttle for Ursidene, C/1861 G1
(Thatcher) for Lyridene. Én komet, 1P/Halley, er opphav til to svermer,
Eta-akvaridene og Orionidene, fordi Jorden krysser banen dens to ganger
i året. Geminidenes opphav er oppført som en asteroide, (3200) Phaethon,
og Kvadrantidenes som legemet 2003 EH1. For Sørlige delta-akvarider
oppgir atlaset ikke noe opphav.

Dette er identifikasjoner astronomer har gjort ut fra baner, og atlaset
gjengir dem slik kilden gir dem. Det beregner ingen strøm og ingen
komet.

## Forenlig med en sverm, aldri bevist å høre til

Du ser en meteor, og banen peker tilbake mot radianten. Er den en
geminide?

Sannsynligvis, natten for Geminidene. Men retningen alene kan ikke si
det. Meteorer som ikke hører til noen sverm, kommer hele natten fra alle
retninger, og noen av dem ligger på linje med et hvilket som helst punkt
du velger. Studien målte hvor ofte, med IMOs egne regler for hvor bred
en radiant skal regnes: en meteor som beveger seg i en retning uten
sammenheng med radianten, ligger likevel på linje med den **15.6%** av
gangene når den viser seg 15 grader fra radianten, og **7.3%** av
gangene ved 50 grader. Omtrent én av ti tilfeldige peker mot radianten
ved et slumpetreff.

Derfor bruker observatører som klassifiserer meteorer, mer enn
retningen: meteorens vinkelfart og lengden på banen må også passe med
svermens fart og avstanden fra radianten.

Atlaset plotter ingen meteorer, og ordbruken er likevel fastlagt. En
retning som ligger på linje med en radiant, er **forenlig med** den
svermen. Ingenting i atlaset sier at en meteor *hører til* eller *er
fra* en sverm.

## Slik bruker du panelet og kartet

- **Åpne panelet** med *Meteorsvermer...* i Solsystem-menyen, eller finn
  gruppen *Meteorsvermer* i Solsystemet-fanen i Kontrolleren. Begge er
  det samme panelet.
- **Velg en sverm.** Valget flytter ikke kartet og endrer ikke
  karttiden. Panelet beskriver nå svermen ved kartets tid og sted.
- **Gi kartet en tid.** En sverm beskrives bare for en karttid, så Sted
  og tid må være på. *Sett karttiden til nominelt maksimum* er et
  utgangspunkt; sett deretter din egen kveld.
- **Les fakta**: om svermen er aktiv, hvor radianten er og hvor høyt,
  Solen, Månen og natten rundt karttiden.
- **Finn den på kartet** med *Sentrer på radianten*, som beholder feltet
  og tiden. Velg merket, og Inspektøren kaller det det det er: *Radiant
  for meteorsverm: punktet svermens meteorer ser ut til å komme fra, ved
  karttiden som er oppgitt.*
- **Se ikke bare på radianten.** De lengste stripene viser seg et godt
  stykke unna den. Merket forteller hvilken vei de vil bevege seg, og
  hvor høyt kilden står.

En sverm ved en karttid kan lagres som et bokmerke, med tid og sted, og
når det åpnes, velges svermen igjen.

Opplest sier siden om merket hva det er: *Perseidene (radianten til en
meteorsverm)*.

## Hva atlaset ikke påstår

- **Det er ikke et varsel.** Panelet oppgir hvor ting er på
  karttidspunktet. Det forutsier ingen rater, ingen utbrudd og ikke noe
  vær, og det kaller aldri en natt god eller dårlig.
- **Verdiene er nominelle.** De er én utgaves liste over hvordan disse
  svermene vanligvis oppfører seg. Et gitt år kan avvike, og atlaset har
  ingen forutsigelse for et bestemt år.
- **ZHR er ikke din telling.** Den trykkes slik kilden trykker den, for
  en ideell himmel med radianten i senit. Ingen lokal rate beregnes.
- **En radiant er kjent på en grad eller så.** Posisjoner oppgis til
  hele grader, i J2000, slik kilden trykker dem; merkets størrelse betyr
  ingenting.
- **Ni svermer, ikke flere.** Svakere svermer, dagsvermer og den
  sporadiske bakgrunnen er ikke i atlaset.
- **Horisonten er den matematiske.** Den har ingen luft og ikke noe
  landskap. En radiant like over den er i praksis ikke oppe ennå.
- **Månen tegnes, månelyset ikke.** Kartet viser hvor Månen er, og
  panelet hvor mye av den som er belyst; ingen av dem viser hvor lys
  himmelen blir.
- **Ingen meteor plottes, og ingen tilordnes.** En bane som ligger på
  linje med en radiant, er forenlig med svermen, ikke bevist å høre til
  den.
- **Listen beskriver strømmene slik de observeres nå.** Solen beregnes
  for 1900 til 2100, så regnestykket virker gjennom de årene, men
  strømmer endrer seg, og en karttid langt fra utgaven sier bare hvor
  verdiene ville plassert radianten.

*Tekniske følgedokumenter: `docs/decisions/meteor-showers.md` (studien,
kjennelsene og funksjonen slik den er bygget),
`docs/studies/meteor-radiants/measurements.md` og dens `platform.md`
(sidene denne artikkelen viser, og fakta den siterer), og
`docs/studies/meteor-showers/measurements.md` (målingene kjennelsene ble
truffet ut fra).*
