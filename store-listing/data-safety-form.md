# Play Console "Data Safety" form — answers

Based on the app's actual permissions and code (no network client, no analytics/ad SDKs anywhere in the dependency graph).

## Does your app collect or share any of the required user data types?
**No data collected, No data shared.**

Justification for each permission, in case Play asks you to clarify during review:

| Permission | Data type | Collected? | Shared? | Notes |
|---|---|---|---|---|
| ACCESS_FINE_LOCATION | Location | No | No | Read in-memory only, to extract GPS time. Never written to disk, displayed, logged, or transmitted. |
| RECORD_AUDIO | Audio | No (stored locally only, not "collected" in Play's sense since it never leaves the device or your control) | No | User-initiated recording of announcement clips, stored in app-private storage. |
| INTERNET | — | No | No | Used only to fetch a start-list URL the user explicitly enters (`StartListViewModel.loadFromUrl`). The app sends a standard HTTP GET to that URL and parses the XML response; it doesn't send any user data as part of that request, and contacts no other server. |

## Security practices section
- **Is data encrypted in transit?** Not applicable — no data leaves the device.
- **Can users request data deletion?** Yes — uninstalling the app or clearing app data removes everything (Android's standard mechanism). No separate request process needed since nothing is collected server-side.
- **Data collected:** None.

## Walkthrough for the Play Console questionnaire
1. "Does your app collect or share any of the required user data types?" → **No**
2. Since you answered No, Play Console will skip the per-data-type questions (location, audio, etc.) entirely — you don't need to declare them as "collected" because none of it leaves the device or is shared.
3. Submit. You can revisit this if you ever add networked features (e.g. cloud sync of start lists).

## Note
`INTERNET` is genuinely used (loading a start list from a user-supplied URL), so keep it declared — no action needed here.

One thing worth checking before submission: `StartListViewModel.loadFromUrl` uses plain `java.net.URL(url).openStream()`. Since `targetSdk` is 36, Android blocks cleartext (`http://`) requests by default unless a network security config explicitly allows it. If any real-world start-list URLs are served over plain HTTP rather than HTTPS, that fetch will fail at runtime. Worth testing with a real URL before release, or adding a network security config if HTTP support is needed.
