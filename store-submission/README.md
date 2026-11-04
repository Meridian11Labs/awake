# Google Play submission kit

Everything the Play Console asks for when publishing Awake (`com.meridian11.books.awake`), published by Meridian11
LLC on the developer account Meridian11.

| Play Console field | File | Limit / spec |
|---|---|---|
| App name | [listing/en-US/title.txt](listing/en-US/title.txt) | 30 characters |
| Short description | [listing/en-US/short-description.txt](listing/en-US/short-description.txt) | 80 characters |
| Full description | [listing/en-US/full-description.txt](listing/en-US/full-description.txt) | 4,000 characters |
| Release notes ("What's new") | [listing/en-US/release-notes.txt](listing/en-US/release-notes.txt) | 500 characters |
| App icon | [graphics/icon-512.png](graphics/icon-512.png) | 512 × 512 PNG, full square (Play rounds the corners) |
| Feature graphic | [graphics/feature-graphic.png](graphics/feature-graphic.png) | 1024 × 500, 24-bit PNG |
| Screenshots | [graphics/large-screen/](graphics/large-screen) | 1920 × 1080 (16:9), 24-bit PNG; used for phone, 7-inch, 10-inch and Chromebook |
| Store settings, contact, category | [forms/store-settings.md](forms/store-settings.md) | |
| Privacy policy | https://meridian11.com/privacy/awake | public |
| Data safety | [forms/data-safety.md](forms/data-safety.md) | no data collected or shared |
| Content rating (IARC) | [forms/content-rating.md](forms/content-rating.md) | expected: Everyone / PEGI 3 |
| Other App content declarations, foreground service | [forms/app-content.md](forms/app-content.md) | |

The listing text avoids what Play's metadata policy rules out: rankings or superlatives, promotional words,
testimonials, emoji, calls to action and other companies' app names. No store image shows a desktop wallpaper or
another company's artwork: the screenshots show Awake's own window, notification and Quick Settings tile, cut out of
captures on a Googlebook (`play/graphics.js` in kuscher-ops adds the captions).

## Releasing

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`, write `docs/release-notes/<version>.md` (GitHub)
   and `listing/en-US/release-notes.txt` (Play, 500 characters at most).
2. Push a tag `v<version>`. `.github/workflows/release.yml` builds, signs with the Awake release key (secrets of the
   `release` environment; it checks the certificate against the pinned SHA-256), publishes the GitHub release with
   the APK, and keeps the bundle as the run's `bundle` artifact. "Run workflow" without a tag is a dry run.
3. The Play job uploads the bundle as a draft on closed testing; it is off while the repository variable
   `PLAY_UPLOAD` is `off`, and then the bundle goes into the Play Console by hand. Sending a release for review is
   always a decision made in the Console, never automatic.

The release key is also the Play app signing key (uploaded with PEPK), so the Play build and the GitHub APK have the
same signature and update each other.
