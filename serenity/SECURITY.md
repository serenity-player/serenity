# Security

## Reporting a vulnerability

Please report security problems privately through GitHub's **Security → Report a vulnerability** form on this
repository, rather than opening a public issue. You'll get a reply as soon as the maintainer can manage.

## What Serenity stores

- Your Spotify **Client ID** (not a secret) in app-private storage.
- Your Spotify **access and refresh tokens**, encrypted with a key held in the Android Keystore.
- Nothing else. There are no analytics, no crash reporting and no servers of our own.

Serenity uses the OAuth Authorization Code flow with PKCE, so no client secret is ever needed or stored.
If you think a token has leaked, remove Serenity at <https://www.spotify.com/account/apps/>.
