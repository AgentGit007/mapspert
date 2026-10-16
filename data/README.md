# Data provenance

Generated files, both committed:

- `countries.json` - `python3 tools/build_countries.py data/wikidata_countries.csv \
  app/src/main/assets/countries.json data/wikidata_tld.csv <flag-icons country.json>`
- `map.json` - `python3 tools/build_map.py <ne_10m_admin_0_map_units.geojson> \
  countries.json app/src/main/assets/map.json`

Sources:

- ISO 3166-1 code list and the "independent" flag: transcribed from the Wikipedia
  "ISO 3166-1" table (249 officially assigned alpha-2 codes; the script asserts the count).
- Names, capitals, numeric codes, top-level domains: Wikidata Query Service exports.
- Continents: `country.json` from the flag-icons package (MIT), cross-checked against
  Natural Earth 50m; 11 entries disagreed and are resolved by `CONTINENT_OVERRIDE`.
- Country outlines: Natural Earth 10m `admin_0_map_units` (public domain). Map units of
  the same country (Bosnia's three entities, Serbia and Vojvodina, the UK's four nations,
  Belgium's regions, ...) are dissolved into one shape first, then simplified
  topologically and quantized to 1/100 degree. Needs `pip install shapely`.
- Map units without an ISO code that ISO 3166-1 places inside a listed country are
  assigned to it (`ASSIGN_BY_GU_A3` in `tools/build_map.py`): Somaliland -> SO,
  Northern Cyprus and the UN buffer zone -> CY, the Korean DMZ halves -> KR / KP,
  Guantanamo Bay -> CU. Left out, so drawn as no country: Siachen Glacier, Bir Tawil,
  the Southern Patagonian Ice Field, the Paracel and Spratly islands and other reefs,
  and the UK Sovereign Base Areas on Cyprus (Akrotiri, Dhekelia).

Hand-made decisions worth reviewing:

- Capital order for BO, LK, PK, YT, PS (first entry is shown as "the" capital).
- `.sh` added for Saint Helena; `.bv .sj .um .eh` marked as reserved/not in use.
- Kosovo added as a non-ISO entry (user-assigned XK), continent Europe.
- EU membership list (27) typed from memory, not checked against an EU source.
- UN membership derived as "ISO independent minus the Holy See" = 193, because Wikidata's
  own membership flag was wrong for DK, RO and TW.
- US Minor Outlying Islands (UM) has no outline, so it is excluded from the map quiz.

TODO before release: confirm Wikidata's licence terms and re-check the disputed-status wording.
