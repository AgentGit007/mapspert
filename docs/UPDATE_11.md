# Update 11 — Kosmos pastel redesign

The app moves to the "Kosmos pastel" look from the design canvas: Soviet space-age
posters crossed with the faded pastels of post-Soviet panel blocks.

Apply update 10 first if you have not already; this update replaces whole files.

## What changed

- **Palette** (`Theme.kt`): night `#1D2230` / paper `#EEE5D3` grounds; five pastels, one
  per category — Globe kosmos blue, Flags mint, Capitals coral, ISO codes ochre,
  Domains lilac. The pastels carry dark ink in both modes.
- **Fonts**: Unbounded (titles, names, buttons) and PT Mono (labels and readouts),
  both SIL OFL 1.1. Space Grotesk is gone.
- **Home**: categories are postage stamps with perforated edges and `№ 01–05`
  numbers, drawn line icons replace the emoji, a satellite orbits the globe, and a
  live `18°N 28°E` readout tracks the globe's centre.
- **Quiz**: progress is a row of control-panel lamps (ochre = current, mint = right,
  red = wrong). Flags and outlines are shown on a stamp, words and codes on a ticket
  with a tear-off stub, and text answers get A/B/C keys. Facts appear on a ruled
  archive card with a rubber stamp (Correct / Missed / Skipped). Buttons are chunky
  push-buttons that sink when pressed.
- **Pinpoint**: the objective is a ticket, countries in play are mint, the answer
  lights up ochre, and a chip shows how many countries are in play.
- **Behaviour change**: after a correct multiple-choice answer the pinned green
  verdict bar is gone; the lit option and the stamp show it. A wrong answer or a skip
  still pins the correct answer above the Next button.

## Copy into the repo

Unzip and copy the contents of `mapspert-update11/` over the repo root, saying yes to
replace. It adds or replaces:

```
README.md
docs/UPDATE_11.md, docs/DATA_VERIFICATION.md, docs/FDROID.md
licenses/OFL-Unbounded.txt, licenses/OFL-PTMono.txt
app/src/main/assets/licenses/OFL-Unbounded.txt, OFL-PTMono.txt
app/src/main/res/font/unbounded_regular.ttf, unbounded_medium.ttf,
    unbounded_bold.ttf, unbounded_extrabold.ttf, ptmono_regular.ttf
app/src/main/res/values/strings.xml
app/src/main/java/app/mapspert/Theme.kt
app/src/main/java/app/mapspert/ui/Common.kt, HomeScreens.kt, QuizScreens.kt,
    GlobeQuizScreen.kt, KnowledgeGlobeScreen.kt, BrowseScreens.kt
```

## Delete from the repo

Space Grotesk is no longer used. In PowerShell, from the repo folder:

```powershell
Remove-Item app\src\main\res\font\spacegrotesk_*.ttf
Remove-Item app\src\main\assets\licenses\OFL-SpaceGrotesk.txt
Remove-Item licenses\OFL-SpaceGrotesk.txt
```

Nothing breaks if you skip this; the files would just sit unused in the repo.

## Font notes

- Unbounded is cut from Google's variable font into four static weights and
  subset to Latin, which is all the app's text needs (checked against every country
  name, capital and string). That is allowed: Unbounded has no Reserved Font Name.
- PT Mono is shipped byte-for-byte unmodified, because its licence reserves the name
  "PT Mono" for unmodified copies.
- The one character neither font has is the ʻokina in "Nukuʻalofa"; Android draws it
  from the system font.

## Not compiled here

The sandbox cannot reach Google's Maven repository, so this was checked statically
(imports, brackets, resource names, identifier resolution) rather than built. If the
build fails, paste the first error.
