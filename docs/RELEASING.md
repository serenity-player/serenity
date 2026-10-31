# Releasing

Releases are built by GitHub Actions when you push a tag like `v1.0.0`. The workflow runs the tests, builds a
**signed** APK, verifies the signature, and attaches the APK and a `SHA256SUMS.txt` to a GitHub Release with
auto-generated notes.

## One-time setup

### 1. Create a signing key

```bash
keytool -genkeypair -v \
  -keystore serenity-release.jks -alias serenity \
  -keyalg RSA -keysize 4096 -validity 10000
```

> **Back this file up somewhere safe, and remember the passwords.** Android only installs an update if it's signed
> with the same key as the installed version. If you lose the key, existing users have to uninstall to update.
> Never commit it (the `.gitignore` already excludes `*.jks` and `keystore.properties`).

### 2. Add repository secrets

Encode the keystore, then add four secrets under **Settings → Secrets and variables → Actions**:

```bash
base64 -w0 serenity-release.jks        # macOS: base64 -i serenity-release.jks
```

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | the output of the command above |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `serenity` (or the alias you chose) |
| `KEY_PASSWORD` | the key password |

## Cutting a release

1. Replace the template placeholders if you haven't (`scripts/rename-package.sh`), and decide on a final name: search
   for trademark and app-store conflicts before announcing.
2. Update `CHANGELOG.md`: rename *Unreleased* to the version and date, and add the compare link.
3. Make sure `main` is green.
4. Tag and push:

   ```bash
   git tag -a v1.0.0 -m "Serenity 1.0.0"
   git push origin v1.0.0
   ```

5. Watch the **Release** workflow. When it finishes, open the release, check the notes and the APK, and edit as needed.

Tags containing a hyphen (for example `v1.1.0-rc1`) are published as pre-releases.

`versionName` comes from the tag and `versionCode` from the workflow run number, so codes always increase.

## Building a signed APK locally

Create `keystore.properties` in the project root (it's git-ignored):

```properties
storeFile=/absolute/path/to/serenity-release.jks
storePassword=...
keyAlias=serenity
keyPassword=...
```

Then run `./gradlew assembleRelease`. Without that file, the release build is produced unsigned.

## Verifying a download

```bash
sha256sum -c SHA256SUMS.txt
```
