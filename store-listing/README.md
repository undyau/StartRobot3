# Play Store launch checklist for StartRobot3

## Done (this session)

- [x] Upload keystore generated at `~/.android/startrobot3-upload.jks` (PKCS12, 28-char random password, 27-year validity)
- [x] `keystore.properties` created at repo root (gitignored) holding the store/key passwords
- [x] `app/build.gradle.kts` wired to read `keystore.properties` and sign release builds with it
- [x] Release build type now has `isMinifyEnabled = true` and `isShrinkResources = true`
- [x] Verified `./gradlew assembleRelease` and `./gradlew bundleRelease` both succeed — signed `app-release.aab` is at `app/build/outputs/bundle/release/app-release.aab`
- [x] Drafted [privacy-policy.md](privacy-policy.md), [data-safety-form.md](data-safety-form.md), [content-rating.md](content-rating.md), [listing-copy.md](listing-copy.md)

## You still need to do

1. **Host the privacy policy at a public URL.** Play requires a live link, not a file upload. A ready-to-serve page is at `docs/index.html` (HTML version of [privacy-policy.md](privacy-policy.md)) — push it to a **public** GitHub repo, then in repo Settings → Pages set Source to "Deploy from a branch", branch `main`, folder `/docs`. The policy will then be live at `https://<your-username>.github.io/<repo-name>/`.
2. **Back up the keystore.** Copy `~/.android/startrobot3-upload.jks` and `keystore.properties` somewhere durable (password manager attachment, encrypted drive, etc.) — if this machine is lost and you have no backup, you'd need to go through Google's upload-key-reset process, which costs time. Play App Signing means losing the upload key isn't fatal, but avoiding the hassle is free.
3. **Opt in to Play App Signing** in Play Console → your app → Setup → App integrity (choose "Use Play Console to generate and manage your app signing key" if not already set, then upload `app-release.aab` signed with the upload key above for your first release).
4. **Test loading a start list from an HTTP (not HTTPS) URL if that's a real scenario** — `targetSdk 36` blocks cleartext traffic by default, so a plain `http://` start-list URL will fail unless you add a network security config. See [data-safety-form.md](data-safety-form.md) for details.
5. **Produce screenshots and the feature graphic** (sizes listed in [listing-copy.md](listing-copy.md)). I can help drive the app via an emulator/device to capture these if you want — just say so.
6. **Fill in Play Console forms** using the drafts here:
   - Store listing → title/descriptions from [listing-copy.md](listing-copy.md)
   - Privacy policy URL → link from step 1
   - Data safety → answers from [data-safety-form.md](data-safety-form.md)
   - Content rating questionnaire → answers from [content-rating.md](content-rating.md)
   - Target audience & content → likely "13+" / general audience, no ads, no in-app purchases
7. **Pick a release track.** For a first submission, Google generally requires a closed (or open) testing track with a minimum number of testers for ~2 weeks before you can publish to Production, unless your developer account is exempt. Check the requirement banner in Play Console for this app.
8. **Upload `app-release.aab`** to your chosen track and submit for review.

## Re-running the release build later
```
JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew bundleRelease
```
Bump `versionCode` (and `versionName`) in `app/build.gradle.kts` before each new upload — Play rejects re-uploads with a versionCode it has already seen.
