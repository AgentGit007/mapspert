# Update 13 — no repeats, uniform order, readable dots, green/red answers

This update ships the **whole Kotlin source tree** (`app/src/main/java/app/mapspert/`),
not just the changed files. Your `Globe.kt` showed that update 10 never fully landed,
and this update has to replace `AppViewModel.kt`, `Storage.kt` and `Quiz.kt`, whose
current versions build on update 10. Shipping every file keeps them consistent with
each other, instead of mixing old and new versions again.

## No country twice in a quiz

Before, every round was drawn independently, so with several rounds the same country
could come back. Now the whole session is planned up front, without repeats, and
split into rounds.

- If the pool cannot cover rounds × questions (EU has 27 countries; 5 rounds × 10
  questions would need 50), the pool is spread over the rounds instead: 6, 6, 5, 5, 5.
  The Start button shows the real numbers, and a note explains the split.
- "All" with 3 rounds now means every country once, split into three rounds.
- An interrupted quiz that you resume keeps its plan, so resuming does not bring
  repeats either. Quizzes saved by older versions keep the rest of the session clear
  of the current round's countries.

## Uniform order

Practice weighting (Settings) still decides *which* countries come up more often:
ones you have not seen or keep missing. It no longer decides the *order*: the chosen
countries are shuffled uniformly. Before, a weak country tended to come first
(measured: 28 % in position 1 against 13 % in position 5); now each of the five
positions gets 20 %.

Tested on the JVM with the real `countries.json`: 20,736 planned sessions over every
mode, region, difficulty, questions-per-round and rounds setting (642,728 questions):
no repeats, round sizes as expected, every subject eligible, no duplicate answer
options.

## Small-country dots

Dots are now a bullseye: paper halo, thick ink ring, then the fill. The old thin ring
vanished when a dot sat on a neighbour of the same colour (Luxembourg inside mint
Germany, France and Belgium).

## Answers on the globe

The answer now lights up **green** and a wrong tap **red**. The old ochre was
confusing because it means "learning" on the knowledge globe. The green is a stronger
signal green, so it stands apart from the mint of the countries in play. The knowledge
globe keeps its mastery colours.

## Copy into the repo

Copy the contents of `mapspert-update13/` over the repo root, saying yes to replace:

```
app/src/main/java/app/mapspert/   (every .kt file)
app/src/main/res/values/strings.xml
app/src/main/res/raw/keep.xml     (keeps runtime-looked-up flags out of resource shrinking)
docs/UPDATE_13.md
```

Then delete any `.kt` file under `app/src/main/java/app/mapspert/` that is *not* in the
package. There should be none, but an orphaned old file would break the build. The
package has 21 files: 6 in `app/mapspert/`, 5 in `quiz/`, 10 in `ui/`.

## Checked here

The `quiz` package (where the planning lives) was compiled and tested with the real
Kotlin compiler. The Android screens were checked statically; if the build fails,
paste the first error.
