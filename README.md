# CatQuest 🐾

CatQuest is an Android cat-collecting prototype: choose a photo or take a photo, let on-device image labeling check for a cat, name valid discoveries, and keep them in your Catdex.

## Build an APK
Every push to `main` starts the **Build CatQuest APK** GitHub Actions workflow. You can also run it manually from the repository's **Actions** tab. When it succeeds, download the `CatQuest-debug-apk` artifact from the workflow run.

## Current features
- Android photo picker and camera thumbnail capture
- On-device ML Kit image labeling; only images labeled as cat/kitten/tabby cat above the confidence threshold are accepted
- Name and collect accepted discoveries in a local Catdex
- Catdex saved locally on the device

## Notes
This is an early MVP. Camera capture currently uses Android's camera thumbnail, and the app does not yet perform true background removal. ML Kit's default labeling model may need Google Play services/network access on first use. It can occasionally misclassify images; it is a best-effort filter, not a guarantee.
