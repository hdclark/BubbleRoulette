# Bubble Roulette

Bubble Roulette is a light-hearted Android memory game built with Kotlin and Jetpack Compose.

Nadine presents one legitimate glass of champagne alongside an increasing number of suspicious bubble-mixture decoys. The identities are revealed briefly, then the glasses either visibly shuffle or Nadine barges in and blocks the table while they move. Pick the real glass to advance. Pick soap and the screen erupts into a dramatic bubble disaster.

> Cartoon game only: never drink an unidentified real-world beverage.

## Gameplay

- Level 1 starts with one champagne glass and one decoy.
- Every successful guess adds exactly one new decoy.
- Each round randomly uses a visible shuffle or a Nadine occlusion gag.
- A wrong selection triggers the animated bubble-storm game-over sequence.
- The best level reached during the current app session is displayed at the top.

## Architecture

The app deliberately stays small:

- `GameEngine.kt` contains the pure round-generation, shuffle, validation, and guess rules.
- `MainActivity.kt` contains the Compose UI and phase timing.
- `GameEngineTest.kt` validates the central invariants across many levels.
- No image, animation, navigation, persistence, analytics, or dependency-injection libraries are required.

## Building

GitHub Actions is the supported build path. The workflow pins JDK 17, Gradle 8.9, Android API 35, and Build Tools 35.0.0, then runs unit tests, Android lint, and `assembleDebug`.

To download an APK:

1. Open the repository's **Actions** tab.
2. Open a successful **Android APK** run.
3. Download the `BubbleRoulette-debug-<commit>` artifact.
4. Extract and install `app-debug.apk` on an Android 8.0 or newer device.

## License

MIT
