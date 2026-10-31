# Architecture

Serenity is a **remote control and browser**, not a player. Audio is produced by the Spotify app or any Spotify
Connect device; Serenity reads state and sends commands through the Spotify Web API.

```
app/src/main/java/.../
├── MainActivity.kt, MainViewModel.kt   entry point, OAuth redirect, setup/login/home routing
├── SerenityApp.kt                      service locator; owns the process-wide NowPlayingViewModel
├── auth/                               PKCE, token exchange and refresh, encrypted token storage
├── data/
│   ├── api/                            Retrofit interface, DTOs, auth interceptor + 401 authenticator
│   ├── repository/                     PlaybackRepository, LibraryRepository, error mapping
│   └── settings/                       Client ID (DataStore)
├── player/                             playback state, polling, optimistic commands, devices, queue
├── library/                            paging (Pager), page stack, library + search view models
├── search/                             SearchViewModel
├── media/                              MediaSession service, notification, widget
└── ui/                                 Compose screens: nowplaying/, library/, search/, theme/, shell
```

## How playback state flows

1. `NowPlayingViewModel` polls `GET /me/player` (every 3 s with the UI visible, 6 s with only the notification
   service listening) and publishes a `NowPlayingUi`.
2. The UI interpolates the progress bar locally between polls.
3. Commands (play, seek, ...) update the state **optimistically**, send the request, then re-poll to confirm or
   revert. An `epoch` counter makes sure a stale poll can't overwrite a newer command.
4. The same view model instance is shared with `MediaPlaybackService` (it lives in an app-level
   `ViewModelStore`), which mirrors the state into a `MediaSession`, the notification and the widget.

## Authentication

Authorization Code flow with PKCE through a Custom Tab, redirecting to `serenity://callback`. Tokens are encrypted
with an Android Keystore AES-GCM key and stored in DataStore. `AuthManager.accessToken()` refreshes shortly before
expiry under a mutex; the `TokenAuthenticator` retries once after a 401. If the refresh token is revoked, the
user is returned to the login screen with an explanation.

## Lists and navigation

`Pager<T>` loads one page at a time (offset or cursor based) and is driven by `PagedList`, which also provides
pull-to-refresh. Each tab that can push pages (Library, Search) owns a `PageStack`; `PageContainer` animates
between the tab's content and the top page and handles predictive back.

## Constraints from Spotify's API

These shape the design (verified against Spotify's February and March 2026 changelogs):

- Playlist contents can only be listed for playlists the user owns (others can still be played).
- Search returns at most 10 results per type per request.
- Artist top tracks, and the profile field that reveals Premium, were removed, so there's no "popular songs"
  list and Premium is detected only when a command is rejected.
- Development-mode apps: Premium owner, one Client ID per developer, five authorized users. This is why every user
  brings their own Client ID.

## Where to add things

| I want to... | Start in |
|---|---|
| call a new endpoint | `data/api/SpotifyApi.kt` + `Dtos.kt`, then a repository |
| add a screen | `ui/` and a tab or `PageStack` entry in `ui/MainScaffold.kt` |
| change how errors read | `data/repository/SpotifyErrors.kt` |
| change polling or commands | `player/NowPlayingViewModel.kt` |
