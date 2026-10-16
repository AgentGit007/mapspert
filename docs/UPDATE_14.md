# Update 14 — correct borders, carried camera, knowledge globe start, title fit

Apply update 13 first; this update replaces only the files listed below.

## Borders (the Balkans and more)

They were not correct. Natural Earth's "map units" file splits some countries into
parts, and the build kept each part as its own outline. That drew internal lines as if
they were borders and left gaps:

- Bosnia showed the Federation / Republika Srpska line, and Brčko was missing (it fell
  under the "smaller than 2 % of the main landmass" cut).
- Serbia showed the Vojvodina line.
- The same affected the UK (England, Scotland, Wales, Northern Ireland), Belgium (three
  regions, drawn as two pieces), Portugal, the US, Australia, Norway, France and others.
- Map units without an ISO code were dropped, which left holes: northern Somalia
  (Somaliland) showed as open water, Cyprus missed its north, a strip ran across Korea.

The build now dissolves each country's map units into one shape before simplifying,
and assigns the unlabelled units to the country ISO 3166-1 lists them under. Disputed or
unclaimed areas stay out. Kosovo stays separate, as before. Details:
`data/README.md` and `docs/DATA_VERIFICATION.md`.

Also: zoomed in (3× or more) the globe now draws the detailed outlines, the same ones
used for tap detection, so borders are no longer angular up close. Countries out of view
are skipped, and each outline is built once per frame instead of twice, so it costs
little.

## The view carries over between rounds

A new round starts where the last one left the globe: same zoom, same position.
"Reset view" still frames the whole pool.

## Knowledge globe

It opens on the continent where you have played the most countries, framed to the
countries you have played there. A tie goes to the first in the order Europe, Asia,
Africa, North America, South America, Oceania. With nothing played yet it opens on
Europe and Africa. Before, it framed every country at once; that set spans the
antimeridian, so the framing went the long way round and landed on the Pacific.

## Title on narrow phones

"MAPSPERT" in Unbounded needs about 190 dp and only had about 185 dp next to the three
buttons, so the T wrapped on the S22 Ultra. The title now has its own full-width line
and cannot wrap. The operator name moved up beside the buttons; the tagline sits under
the colour bars.

## Copy into the repo

Copy the contents of `mapspert-update14/` over the repo root, saying yes to replace:

```
app/src/main/assets/map.bin, countries.json
data/countries.json, data/README.md
tools/build_map.py
docs/DATA_VERIFICATION.md, docs/UPDATE_14.md
app/src/main/java/app/mapspert/AppViewModel.kt
app/src/main/java/app/mapspert/ui/GlobeView.kt, GlobeQuizScreen.kt,
    KnowledgeGlobeScreen.kt, HomeScreens.kt
```

`countries.json` changes only in the `map_area` values, recomputed from the merged
outlines.
