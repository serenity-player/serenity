# Contributing to Serenity

Thanks for helping out! Serenity is a small project, so the process is light.

## Getting started

1. Fork and clone the repo.
2. Open it in Android Studio (Ladybug or newer) and let Gradle sync.
3. Follow [docs/SETUP.md](docs/SETUP.md) to create your own Spotify developer app, then run on a device or emulator
   with the Spotify app installed (an emulator with Google Play works).
4. Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for how the pieces fit together.

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # debug APK
```

## Making changes

- Open an issue first for anything big, so we can agree on the approach.
- Keep pull requests focused. One change per PR is easiest to review.
- Follow the existing style: Kotlin official code style, Compose state hoisted to ViewModels, user-facing
  strings that explain what to *do* about a problem.
- Add or update unit tests for logic that doesn't need Android (parsing, state, paging).
- Check UI changes in light and dark mode, with large font sizes, and with TalkBack for anything interactive.
- Add a line to `CHANGELOG.md` under *Unreleased*.

## Ground rules

- **Never commit secrets.** No Client IDs in code, no tokens, no keystores. The Client ID is entered by the user and
  lives in one place (`SettingsStore`).
- **Respect Spotify's terms.** Serenity is a remote control. Don't add audio downloading, recording or stream
  extraction, and don't try to replace Spotify's own playback. Don't use Spotify's logos as Serenity branding.
- **Be kind.** Assume good intent and keep feedback about the code.

## Good first areas

- A two-pane library layout for tablets
- More unit tests (paging, repository mapping)
- Translations (strings are currently inline; moving them to `strings.xml` is a welcome first PR)
- R8/ProGuard shrinking for release builds, verified on a device
