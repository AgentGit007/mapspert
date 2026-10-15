# Update 12 — new icon and clearer borders

Apply update 11 first; this update replaces whole files.

## Borders on the globe

Borders used to be one physical pixel of paper colour at 30 % opacity, which all but
vanished between two countries of the same colour (two mint countries in Pinpoint,
for example). They are now dark ink, sized in dp:

- countries in play: 0.9 dp at world view, growing to 1.4 dp when zoomed in
- countries out of play: 0.7 to 1.0 dp, and lighter
- light mode uses ink at 75 % instead of the night colour

Thinner at world view so small, dense regions such as the Balkans don't clog up.
This applies to every globe: home, Pinpoint, the knowledge globe and Browse.

## Icon

Same idea as before (a globe in a graduation cap) in the Kosmos palette: night
ground, kosmos-blue continents with ink borders and one ochre country, a coral
orbit with a satellite, a lilac cap and an ochre tassel. The tassel now hangs on the
left so it clears the orbit. The themed (monochrome) icon for Android 13+ includes
the orbit too.

## Copy into the repo

Copy the contents of `mapspert-update12/` over the repo root, saying yes to replace:

```
app/src/main/java/app/mapspert/ui/GlobeView.kt, Common.kt
app/src/main/res/drawable/ic_launcher_foreground.xml, ic_launcher_monochrome.xml
app/src/main/res/values/ic_launcher_background.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml, ic_launcher_round.xml   (unchanged, included for completeness)
docs/icon.png, docs/icon.svg, docs/UPDATE_12.md
fastlane/metadata/android/en-US/images/icon.png
```

If the launcher keeps showing the old icon after installing, uninstall the app
first, then install again; launchers cache icons.

## Not compiled here

Checked statically, as with update 11.
