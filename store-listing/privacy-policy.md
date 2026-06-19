# Privacy Policy for StartRobot3

**Last updated: June 19, 2026**

StartRobot3 ("the app") is developed by Andy Simpson. This policy explains what data the app accesses and how it is used.

## Summary

StartRobot3 does not collect, store, or transmit any personal data to the developer or to any third party. All data the app uses stays on your device.

## What the app accesses, and why

**Microphone (RECORD_AUDIO)**
Used only when you choose to record a custom announcement clip inside the app (e.g. a recorded voice cue to play during a start sequence). Recordings are saved locally on your device as part of your event configuration and are never uploaded or shared.

**Location (ACCESS_FINE_LOCATION)**
Used only to read GPS time signals from satellites so the app's start clock stays accurate even with no cell or internet signal in the field. The app does not record, store, or transmit your geographic position, and does not track your location history. Location is used transiently, in memory, purely to extract a timestamp.

**Notifications (POST_NOTIFICATIONS)**
Used to show the persistent foreground-service notification required by Android while the start clock is running, so you can see at a glance that timing is active.

**Network access (INTERNET)**
Used only when you choose to load a start list from a URL you provide, instead of a local file. The app fetches that URL directly and parses the IOF XML 3.0 data it returns — it does not send any of your data to that server beyond the standard HTTP request, and does not contact any other server. If you only load start lists from local files, the app makes no network requests at all.

**Start list files**
If you load an IOF XML 3.0 start list file, the runner names and start times in that file are read and stored locally in the app's database on your device, solely to drive announcements during the event. This data is never transmitted anywhere and can be cleared by uninstalling the app or clearing app data.

## Data sharing

StartRobot3 does not share any data with third parties. There are no analytics, advertising, or crash-reporting SDKs in the app.

## Data retention and deletion

All data (start lists, recorded clips, configured announcement chains, settings) is stored locally in app-private storage and is deleted when you uninstall the app or clear its data from Android system settings.

## Children's privacy

StartRobot3 is a timing utility for orienteering event organizers and is not directed at children. It does not knowingly collect data from anyone.

## Changes to this policy

If this policy changes, the updated version will be posted at this same URL with a revised "Last updated" date.

## Contact

Questions about this policy can be sent to: undyau@gmail.com
