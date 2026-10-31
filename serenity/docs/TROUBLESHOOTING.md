# Troubleshooting

| What you see | Why | What to do |
|---|---|---|
| Browser says **INVALID_CLIENT: Invalid redirect URI** | The redirect URI in the dashboard doesn't match exactly | Add `serenity://callback` to your app's Redirect URIs, with no spaces or trailing slash, and save |
| Browser says **INVALID_CLIENT: Invalid client** | Wrong Client ID | On the login screen tap **Change Client ID** and paste the one from your app's Settings |
| Login works, then "This Spotify account isn't added to your developer app" | Development-mode apps only work for listed users | Add the account under **User Management** in the dashboard |
| Can't create a developer app | Spotify requires Premium on the owner account, and allows one Client ID per developer | Use a Premium account, or reuse your existing app |
| Banner: "Playback control needs Spotify Premium" | Spotify rejects remote control on free accounts | Browsing still works. Playback control needs Premium |
| "No active device" | Spotify only accepts commands for a device that has been active recently | Open Spotify on any device and press play once, or use **Choose a device** |
| "Nothing playing" although music is on | Spotify only reports a device after it has played something | Play something, then wait a few seconds |
| A playlist shows "Spotify only shares track lists for playlists you own" | Spotify limits playlist contents for apps in development mode | Play and Shuffle still work for that playlist |
| Search shows at most 10 results per type | Spotify's limit for development-mode apps | Use **Show more** under each section |
| Banner: "Can't reach Spotify" | You're offline, or Spotify is unreachable | It clears itself when the connection returns |
| Banner: rate limited | Too many requests in a short time | Wait the time shown; Serenity backs off automatically |
| "Your Spotify session expired" | The refresh token was revoked, or the app was removed from your account | Tap **Connect Spotify** again |
| No notification or lock-screen controls | Notification permission denied, or the battery saver stopped the service | Allow notifications for Serenity and exempt it from battery restrictions |
| Widget shows old info | The widget only updates while Serenity's playback service is running | Start playback with the app open; tapping a widget button also restarts it |

Still stuck? [Open an issue](../../issues/new/choose) with your Android version, Serenity version (⋮ menu → About) and
what you tried. Please don't include your Client ID or tokens.
