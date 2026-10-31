# Setting up Serenity

Serenity needs **your own free Spotify developer app**. It takes about five minutes, and it means your listening
never goes through anyone else's account or quota.

## You'll need

- **A Spotify Premium account.** Premium is needed to control playback, and Spotify currently requires the owner of a
  developer app to have Premium too.
- The **Spotify app** (or any Spotify Connect device) to actually play the music.
- Serenity installed on an Android 12+ phone.

## 1. Create the developer app

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and log in.
2. Choose **Create app** and fill in:
   - **App name:** anything, for example `Serenity`.
   - **App description:** anything, for example `Personal Spotify remote`.
   - **Redirect URI:** `serenity://callback` (type it exactly, then click **Add**).
   - **Which API/SDKs are you planning to use?** tick **Web API**.
3. Accept the terms and **Save**.

> Spotify limits each developer to one Client ID, and each Client ID to five authorized users. Reuse your existing
> app if you already have one, and just add the redirect URI to it.

## 2. Copy the Client ID

Open the app, then **Settings**, and copy the **Client ID** (32 letters and numbers). You do **not** need the client
secret. Never paste it anywhere.

## 3. Allow your account

In the app's **User Management**, make sure the Spotify account you'll sign in with is listed. If it isn't, add its name and
email. Until you do, Spotify will reject requests with *"User not registered in the Developer Dashboard"*.

## 4. Connect Serenity

1. Open Serenity and paste the Client ID.
2. Tap **Save and continue**, then **Connect Spotify**.
3. Approve the permissions in the browser. You'll be sent back to Serenity.
4. Start playing something in the Spotify app. It appears in Serenity within a few seconds.

(The dashboard's wording changes now and then. If a label looks different, the idea is the same.)

## What Serenity asks for

| Permission | Why |
|---|---|
| `user-read-playback-state` | See what's playing and on which device |
| `user-modify-playback-state` | Play, pause, skip, seek, volume, queue, switch device |
| `user-read-currently-playing` | Current track details |
| `user-read-private` | Account details Spotify requires for playback requests |
| `user-library-read` | Your liked songs and saved albums |
| `playlist-read-private` | Your playlists |
| `user-follow-read` | Artists you follow |

Serenity never asks to change your library or playlists.

## Changing or removing the Client ID

On the login screen, tap **Change Client ID**. To revoke Serenity's access entirely, remove it at
<https://www.spotify.com/account/apps/>.

Having trouble? See [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
