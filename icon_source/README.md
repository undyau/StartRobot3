# Orienteering Clock App — App Icon

## Design

Square orienteering control-flag background (white / control-orange
diagonal split) with a speaker + sound-wave glyph, representing the
start-time and audio-announcement feature of the app.

**Colors**
- Control orange — `#EE6B1F`
- White — `#FFFFFF`
- Ink (glyph) — `#232323`

## What's in here

- `src/ic_launcher_foreground.svg`, `src/ic_launcher_background.svg` —
  editable vector sources for the two adaptive-icon layers.
- `adaptive/mipmap-{density}/ic_launcher_foreground.png` and
  `ic_launcher_background.png` — pre-rendered raster layers at all five
  densities (mdpi → xxxhdpi).
- `adaptive/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` —
  the adaptive icon definitions referencing the two layers above.
- `legacy/mipmap-{density}/ic_launcher.png` and `ic_launcher_round.png` —
  flattened fallback icons for Android 7.1 and below.
- `play_store_icon_512.png` — the 512×512 listing icon for the Play Console.

## Installing

Copy the contents of `adaptive/` and `legacy/` straight into your project's
`app/src/main/res/` — the `mipmap-*` folder names already match what Android
expects, and the XML files already point at the right drawables.
