# Mohalla Ops — Android (Kotlin + Jetpack Compose)

Practice-only neighborhood flood-relief drill app. **Not an emergency system.** All data is simulated.

## Open and run
1. Install **Android Studio (Koala or newer)** with JDK 17 and an Android 8.0+ (API 26) device/emulator.
2. File → Open → select this `MohallaOps` folder. Let Gradle sync (internet needed the first time).
   If `gradlew` is missing, Android Studio uses `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.7).
3. Press Run. No Firebase, API key or Maps key is needed for the core demo.

## Try the six-family demo (2 minutes)
1. Home → choose the **Drill Leader** chip → Drill tab → **Run 1** → Results tab (Drill → Compare results): 3 families waiting.
2. Drill → **Run 2** → compare: 0 families waiting. (The only difference: 3 boxes moved North→South before the same road closure.)
3. Map tab shows the stations, families, road status and active delivery routes.
4. Manual flow: Reset → switch to **Resident** → Request supplies (type or speak Urdu/English → Analyse → confirm) →
   **South Lead** approves & assigns → **Volunteer** accepts, enters the box ID, records handoff → **Resident** confirms receipt.
   Only the recipient's confirmation counts a family as served.
5. Switch to wrong roles on purpose (e.g. Resident pressing a stock action, South Lead touching a North box):
   the repository refuses — this is enforced in code and in SQL, not just by hiding buttons.

## Tests
- Unit tests (no device): `./gradlew testDebugUnitTest` (Urdu/English extraction, planner).
- Instrumented tests (device/emulator): `./gradlew connectedDebugAndroidTest`
  (box-ownership rule, permissions, delivery stages, both demo runs, duplicate-event handling).
**These were written but NOT executed by the author of this zip (no Android SDK was available). Run them first.**

## Architecture
`domain/` models, roles, rule exceptions · `data/` Room entities, DAO, `DrillRepository` (all business rules, one DB transaction per change) ·
`ai/` request extraction (on-device + Gemini client) and deterministic planner · `ui/` Compose screens and design system.
Manual wiring (`MohallaApp`) is used instead of Hilt to keep the first build simple.

## Gemini (optional, secure)
`functions/index.js` is a Cloud Function that holds the Gemini key as a Firebase secret.
Deploy it, then put its URL in `GEMINI_PROXY_URL` (`app/build.gradle.kts`). Without it the app uses the labelled on-device reader.

## What is implemented vs not (honest list)
Implemented: splash, onboarding (EN/UR strings), demo accounts, dashboard, stations/inventory with per-box lifecycle and audited transfers,
requests (text + system speech input + extraction + manual correction + duplicate refusal), full delivery workflow, volunteer availability & reassignment,
simulated map, road-closure/offline/volunteer disruptions, deterministic planner with accept/reject, timeline, two-run results, Room persistence,
event export/import between phones with unique IDs and duplicate detection.

NOT implemented in this build: Firebase Auth / Firestore / Storage / FCM, real Nearby Connections transport (import/export of event JSON is the
documented test mode), QR scanning (box IDs are typed), create-drill wizard and multi-drill management, Gemini-written after-action review and
replay explanation, conflict resolution for inventory merged across devices, WorkManager background sync (`Sync now` only marks the local queue),
Hilt, Maps SDK, in-app language switcher (UI follows device language for translated strings; most screen text is English-only), the Mystery Card extension point.
