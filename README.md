<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/87de21a7-7e2e-415b-871d-2b37d4674ca3

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. For local development only, create a `.env` file and set `GEMINI_API_KEY` (see `.env.example`); `.env` is ignored by Git.
5. Do not distribute an APK built with a real API key. Android APKs can be decompiled and client-side keys extracted. Use a backend proxy for distributed builds; public CI builds use the example placeholder only.
6. Run the app on an emulator or physical device
