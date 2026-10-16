# Data verification

Status of every claim the app makes about its own data.

## Verified

| Claim | Status | Source |
| --- | --- | --- |
| Wikidata data is CC0 | **Confirmed** | wikidata.org/wiki/Wikidata:Licensing: structured data in the main, property and lexeme namespaces is CC0; text elsewhere is CC BY-SA. We use only structured data. |
| The EU has 27 members | **Confirmed**, and the 27 codes in `build_countries.py` match | Multiple current sources; unchanged since the UK left in 2020, newest member Croatia (2013) |
| Natural Earth is public domain | Confirmed earlier from the project's own terms | naturalearthdata.com |
| flag-icons is MIT | Confirmed from the LICENSE file in the npm package | lipis/flag-icons |
| Unbounded is SIL OFL 1.1, no Reserved Font Name | Confirmed from OFL.txt in google/fonts (ofl/unbounded) | googlefonts/unbounded |
| PT Mono is SIL OFL 1.1, Reserved Font Name "PT Mono" | Confirmed from OFL.txt in google/fonts (ofl/ptmono); shipped unmodified | ParaType |
| 249 officially assigned ISO alpha-2 codes | Transcribed from the Wikipedia ISO 3166-1 table; the build script asserts the count | secondary source |

## Still unverified: editorial choices

These are not errors, they are decisions. Each is contestable and each is in
`tools/build_countries.py` under `CAPITAL_ORDER` or nearby.

| Entry | What the app shows | The argument against |
| --- | --- | --- |
| Bolivia | Sucre first, La Paz second | La Paz is the seat of government and what most quizzes expect |
| Sri Lanka | Sri Jayawardenepura Kotte first, Colombo second | Colombo is the commercial capital and far better known |
| Palestine | Ramallah first, East Jerusalem second | Politically contested in both directions |
| Pakistan | Islamabad only | Wikidata also lists Rawalpindi, which was interim 1959-67 and is stale |
| South Africa | Three capitals, Pretoria first | Correct, but quiz answers need one |
| Cyprus | Counted as Europe | ISO and the UN place it in Asia |
| `.sh` for Saint Helena | Added by hand | Absent from the Wikidata export |
| `.bv .sj .um .eh` | Marked reserved, not in use | Own knowledge, unchecked |

**What would settle these:** the ISO 3166 Online Browsing Platform for the code list and
names, and the UN Statistics Division's M49 for continents. Both are authoritative and
free to consult. Neither is machine-readable without agreeing to terms, which is why the
build still uses Wikipedia and Wikidata.

## Map: editorial calls

- **Unlabelled map units assigned to their ISO country:** Somaliland (SO), Northern
  Cyprus and the UN buffer zone (CY), the Korean DMZ (KR/KP), Guantanamo Bay (CU). This
  follows the app's rule of repeating ISO 3166-1, as with the country list.
- **Left as no country:** Siachen, Bir Tawil, the Southern Patagonian Ice Field, the
  disputed reefs and islands, and the UK Sovereign Base Areas on Cyprus. Assigning the
  bases to GB would make two dots of the UK appear on Cyprus; assigning them to CY would
  misstate their status.

## How you can help

The fastest wins, in order:

1. **Play the quiz and report anything that looks wrong.** You are the best detector of
   bad data here, and every correction can go straight into the override tables.
2. **Run one query** if you want more fields (see below) and send the CSV.
3. **Decide the editorial calls above** - they are judgement, not research, and it is
   your app.
