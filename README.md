# FlashDecks

An offline flashcard study app for Android. Create named decks, add/edit/delete
front-back cards, and run self-graded quiz sessions with shuffled order,
tap-to-flip cards, and per-deck best scores. Everything is stored locally with
Room — there is no account, no sync, and no network access at all.

## Features

- **Deck management** — create, rename, and delete decks (deleting a deck
  removes its cards via a Room `ON DELETE CASCADE` foreign key).
- **Card management** — add, edit, and delete cards in a dialog with front/back
  fields; cards keep a stable creation order through a `position` column.
- **Quiz mode** — cards are shuffled each session; tap a card to flip it with a
  3D rotation animation, then self-grade with "I got it right" / "I got it
  wrong". Progress ("Card 3 of 10") is shown throughout.
- **Scoring** — each session ends on a result screen with the percentage score,
  correct/total count, and a "New best score!" badge when applicable. The deck
  stores its best score and total quizzes taken.
- **Material 3 theming** — light / dark / system-follows-device toggle in the
  toolbar menu, persisted in SharedPreferences and applied before the first
  activity starts (no theme flash).
- **Fully offline** — the manifest declares zero permissions; the app never
  touches the network.

## Tech stack

- Kotlin, XML layouts + Fragments (no Compose — see Interview notes)
- AndroidX: AppCompat, Material 3, ConstraintLayout, RecyclerView
- Architecture: MVVM — Room → `FlashcardRepository` → ViewModels → Fragments
- Reactive UI: Kotlin `StateFlow` collected with
  `lifecycleScope.repeatOnLifecycle`
- Room 2.6.1 (kapt) with `Deck` / `Card` entities, DAOs, and a v1 database
  using `fallbackToDestructiveMigration()`
- Manual fragment navigation in `MainActivity` (no Navigation Component)
- minSdk 26, targetSdk/compileSdk 34, AGP 8.5.2, Kotlin 2.0.20, Gradle 8.7,
  Java 17

## Project structure

```
FlashDecks/
├── settings.gradle / build.gradle / gradle.properties
├── gradle/wrapper/            # wrapper properties (jar is fetched on first sync)
├── gradlew / gradlew.bat
├── .gitignore
├── README.md
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml        # no permissions declared
        ├── java/com/entitycoder/flashdecks/
        │   ├── FlashDecksApp.kt       # owns DB + repository, applies saved theme
        │   ├── data/
        │   │   ├── Deck.kt / Card.kt / DeckWithCardCount.kt
        │   │   ├── DeckDao.kt / CardDao.kt
        │   │   ├── AppDatabase.kt     # Room DB v1, fallbackToDestructiveMigration
        │   │   └── FlashcardRepository.kt
        │   └── ui/
        │       ├── MainActivity.kt    # manual fragment navigation + theme menu
        │       ├── theme/ThemeManager.kt
        │       ├── decks/             # deck list: fragment, adapter, ViewModel
        │       ├── detail/            # deck detail: fragment, adapter, ViewModel
        │       ├── editor/            # add/edit card dialog
        │       └── quiz/              # quiz session, ViewModel, result screen
        └── res/
            ├── layout/                # 8 XML layouts
            ├── menu/                  # toolbar theme menu + detail overflow menu
            ├── values/                # strings, colors, Material3 theme
            └── values-night/          # dark theme variant
```

## How to open and run

1. Open Android Studio (Hedgehog or newer recommended) and choose **Open**,
   then select the `FlashDecks` project folder.
2. Let Gradle sync finish. The Gradle wrapper jar (`gradle-wrapper.jar`) is
   **not** checked in — it is downloaded automatically on the first sync from
   the distribution URL in `gradle/wrapper/gradle-wrapper.properties`, so the
   first sync needs internet access (the app itself needs none).
3. Make sure JDK 17 is selected (Settings → Build → Build Tools → Gradle →
   Gradle JDK).
4. Press **Run** (or Shift+F10) with an emulator (API 26+) or a physical
   device connected.

> **Note:** this project was authored in an environment without the Android
> SDK, so it has **not** been compiled or run on a device here. Open it in
> Android Studio and run it there — the README instructions above are the
> tested path conceptually, not a verified build.

## Offline / no-network note

`AndroidManifest.xml` declares no `<uses-permission>` entries. All data lives
in the app's private Room database (`flashdecks.db`); uninstalling the app
removes it. There are no analytics, ads, or network calls anywhere in the
codebase.

## Interview notes

**What the project is:** FlashDecks is a fully offline flashcard study app —
decks of front/back cards with full CRUD, plus a quiz mode (shuffle,
tap-to-flip, self-grading) that records a best score per deck. It is built as
a single-activity, multi-fragment app following MVVM: Room is the single
source of truth, a repository exposes Flows, ViewModels expose StateFlows,
and fragments collect them lifecycle-safely.

**Why XML layouts + Fragments instead of Compose:** this was a deliberate
choice for maximum tutorial and interview compatibility. Most Android
interviewers still expect candidates to read and explain the classic
XML/Fragment stack, and every screen in this app maps 1:1 to concepts that
come up in interviews (RecyclerView + DiffUtil, DialogFragment with the
Fragment Result API, manual back-stack management). Compose would be less
code, but the XML version demonstrates more of the fundamentals this project
was meant to practice.

**Why Room instead of SharedPreferences or files:** the data is relational
(decks have many cards; deleting a deck must delete its cards), which is
exactly what SQLite is for. Room gives compile-time-checked queries, the
`ON DELETE CASCADE` foreign key, and observable `Flow` queries so the UI
updates itself when the database changes — none of which SharedPreferences
offers cleanly. A JSON file would work for v1 but would need manual parsing,
manual threading, and manual change notification.

**Why StateFlow instead of LiveData:** StateFlow is the Kotlin-idiomatic
choice, composes well with Room's `Flow` queries (via `stateIn`), and makes
combining streams (the quiz screen merges cards/index/revealed/finished into
one `QuizUiState`) straightforward. Collection uses
`repeatOnLifecycle(STARTED)` so updates stop when the screen isn't visible —
no leaks, no wasted work.

**Why manual fragment navigation instead of the Navigation Component:** with
five screens and simple linear flows, a small `showFragment()` helper in
`MainActivity` is easier to read than a navigation graph, and it keeps the
back-stack behavior explicit (e.g. the quiz result *replaces* the quiz without
a back-stack entry so Back/Done lands on the deck detail screen). For a larger
app with deep links and nested graphs, Navigation Component would be the
better call.

**Trade-offs and limitations:**
- Quiz grading is self-reported ("I got it right") — honest, but the scores
  are only as accurate as the user.
- No spaced-repetition algorithm (e.g. SM-2); cards are simply reshuffled
  each session.
- Card order is append-only; there is no drag-to-reorder yet.
- No import/export and no backup — data lives only in the app's database.
- The quiz session is in-memory (`QuizViewModel`); rotating the device keeps
  it (ViewModel survives config changes) but process death restarts the
  session.
- `fallbackToDestructiveMigration()` means a future schema change wipes user
  data instead of migrating it — acceptable for v1, not for a shipped app
  with real users.

**How I'd extend it:**
1. Spaced repetition — track per-card ease factors and schedule reviews.
2. Timed quiz mode and multiple-choice mode for variety.
3. CSV/Markdown import/export so decks can be shared.
4. Search across decks and tags on cards.
5. Proper Room `Migration` objects instead of destructive fallback, plus an
   automated backup/export.
6. A home-screen widget showing "cards due today".
