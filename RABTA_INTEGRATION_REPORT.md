# RABTA — Integration Report

Generated: 2026-11-09 · Phase: extraction + static inspection (no features implemented, no files modified)

---

## 1. Project identity and discovered package

| Item | Value |
|---|---|
| Official name (requested) | RABTA — *When Every Connection Matters.* |
| Original project name in source | **Mohalla Ops** (tagline in code: "Practice today. Respond better tomorrow.") |
| Android package / namespace / applicationId | `com.mohallaops` (debug builds add `.debug`) |
| Gradle root project name | `MohallaOps` |
| Gradle modules | `:app` only |
| Package rename performed? | **No** (per instruction). All user-facing strings, theme `Theme.MohallaOps`, `MohallaApp`, `MohallaTheme`, `MohallaColors`, DB name `mohalla_ops.db` and the event-export marker `"mohalla-ops"` still carry the old name. |

## 2. ZIP archive location
`/mnt/user-data/uploads/MohallaOps.zip` — 52,300 bytes, SHA-256 `02e147acc58429cb702082eb409155d7b86f3fc290e39404d6bef8e11c53f9a0`. Not modified.

## 3. Destination location
`/mnt/user-data/outputs/RABTA/` (newly created, empty before extraction).
**Decision to flag:** the archive wraps everything in a top-level `MohallaOps/` folder. I stripped that wrapper so `RABTA/` is the project root (e.g. `RABTA/app/build.gradle.kts`, not `RABTA/MohallaOps/app/...`). Everything below the wrapper keeps its original structure.

## 4. Source archive inventory
38 file entries, 0 directory entries, 0 duplicate paths, 0 unsafe paths, 0 symlinks, 0 encrypted entries. Full list with sizes and SHA-256 in `RABTA_FILE_MANIFEST.csv`.

## 5. Destination inventory (before extraction)
Empty directory. No pre-existing files.

## 6. Files already existing and skipped
None (0).

## 7. Files added (38) — all SHA-256 verified
```
.gitignore
README.md
build.gradle.kts
settings.gradle.kts
gradle.properties
gradle/wrapper/gradle-wrapper.properties
functions/index.js
functions/package.json
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/res/drawable/ic_launcher.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/values-ur/strings.xml
app/src/main/java/com/mohallaops/MainActivity.kt
app/src/main/java/com/mohallaops/MohallaApp.kt
app/src/main/java/com/mohallaops/ai/Planner.kt
app/src/main/java/com/mohallaops/ai/RequestExtractor.kt
app/src/main/java/com/mohallaops/data/AppDatabase.kt
app/src/main/java/com/mohallaops/data/DrillDao.kt
app/src/main/java/com/mohallaops/data/DrillRepository.kt
app/src/main/java/com/mohallaops/data/Entities.kt
app/src/main/java/com/mohallaops/domain/Models.kt
app/src/main/java/com/mohallaops/ui/AppRoot.kt
app/src/main/java/com/mohallaops/ui/MainViewModel.kt
app/src/main/java/com/mohallaops/ui/components/Components.kt
app/src/main/java/com/mohallaops/ui/theme/Theme.kt
app/src/main/java/com/mohallaops/ui/screens/ActivityScreen.kt
app/src/main/java/com/mohallaops/ui/screens/DrillScreen.kt
app/src/main/java/com/mohallaops/ui/screens/HomeScreen.kt
app/src/main/java/com/mohallaops/ui/screens/Intro.kt
app/src/main/java/com/mohallaops/ui/screens/MapScreen.kt
app/src/main/java/com/mohallaops/ui/screens/RequestSheet.kt
app/src/main/java/com/mohallaops/ui/screens/ResultsScreen.kt
app/src/main/java/com/mohallaops/ui/screens/StationsScreen.kt
app/src/test/java/com/mohallaops/ExtractorTest.kt
app/src/androidTest/java/com/mohallaops/DrillRepositoryTest.kt
```

## 8. Conflicting files — none
## 9. Duplicate archive entries — none
## 10. Unsafe or unsupported entries — none
## 11. Failed extractions — none

## 12. SHA-256 verification
38 / 38 verified (file exists, size matches, SHA-256 of written bytes equals SHA-256 of the ZIP entry). Per-file results: `RABTA_FILE_MANIFEST.csv`. No secrets were present or recorded.

---

## 13. What the source actually contains (from reading the code)

**Stack in use:** Kotlin 2.0.20, AGP 8.5.2, Gradle 8.7 (wrapper properties), KSP 2.0.20-1.0.25, compileSdk/targetSdk 34, minSdk 26, JVM 17, Jetpack Compose (BOM 2024.09.00), Material 3, Navigation Compose 2.8.0, Lifecycle 2.8.5, Room 2.6.1. Manual dependency wiring in `MohallaApp` (no Hilt). `GEMINI_PROXY_URL` BuildConfig field is empty by default.

**Architecture:** single-module, packages `domain/` (models, roles, exceptions) · `data/` (Room entities, one DAO, `DrillRepository` holding all rules) · `ai/` · `ui/` (AppRoot, ViewModel, screens, components, theme). One `MainViewModel`. Navigation is string routes (`home, stations, drill, map, activity, results`) inside `ui/AppRoot.kt`.

**Database:** Room v1, 10 tables — stations, boxes, families, requests, deliveries, volunteers, roads, inventory_tx, events, run_results. `exportSchema = false`. No migrations yet.

**Screens:** Splash, 3-page onboarding, Home (demo-account chips, stat cards, role quick actions), Stations (inventory, requests, deliveries, pickup dialog), Drill (demo runs, AI planning panel, road/offline/volunteer controls, manual steps), Map (canvas-drawn, labelled "Simulated map"), Activity (event timeline, sync, export/import), Results (two-run comparison), Request bottom sheet (text, system speech input, analyse, confirm).

**AI:** `LocalExtractor` (rule-based EN/UR, labelled "on-device demo"), `GeminiBackendClient` (calls a proxy URL, strict JSON validation, returns null on any failure so the local reader is used), `RequestExtractionService` (chooses between them), `Planner` (deterministic, not an LLM). Backend: `functions/index.js` Firebase Cloud Function that keeps the Gemini key in a Firebase secret, model `gemini-1.5-flash`, JSON response mode, temperature 0.

**Tests (written, never executed by me or, per its README, by the author):**
- `ExtractorTest` (unit, 7 tests): Urdu/English extraction, missing info, ambiguous station, planner suggest/closed-road/balanced.
- `DrillRepositoryTest` (instrumented, 9 tests): Run 1 and Run 2 outcomes, transfer refused when road closed, cross-station reservation refused, role permissions, delivery not counted until recipient confirms, duplicate request refused, imported-event dedup.

---

## 14. Comparison with the RABTA requirements

Legend: **Implemented** = code present and logic reads as correct · **Partial** = some of it exists · **Missing** = nothing found in the source. "Implemented" means *read and reasoned about*, not *run*.

### 14.1 Implemented (by source inspection)

| Requirement | Evidence |
|---|---|
| Four roles with enforced permissions | `Role` enum; every `DrillRepository` method takes an `Actor` and throws `PermissionDeniedException`. Ownership of boxes is also enforced in SQL (`reserveBox`, `moveBox` UPDATE with `ownerStationId` + `status='AVAILABLE'` conditions). |
| Six-family / two-station scenario | `seed()`: North 5 boxes + 2 families, South 1 box + 4 families, one road `ROAD-NS`, 3 volunteers. |
| Road-closure simulation | `setRoadOpen`; `transferBoxes` refuses when the road is closed or stock is short; atomic (one DB transaction). |
| Run 1 / Run 2 logic | `runDemo(label, transferFirst)`: reset → optional 3-box transfer → close road → `fulfilAll` → `completeRun`. By reading, Run 1 yields 3 served / 3 waiting (North 2, South 1), Run 2 yields 6 / 0 with 3 transferred. **Not executed.** |
| Results from real state | `completeRun` computes from DB counts (`servedCount`, `servedAt`, `transferCount`) and stores `RunResultEntity`. |
| AI-assisted improvement suggestion | `Planner.analyse` identifies South's shortfall and North's spare stock and proposes "move 3 North→South before the road closes"; requires human Accept. Numbers come from stored data. |
| Inventory ownership + history | `BoxEntity.ownerStationId`, `InventoryTxEntity` (RESERVE, TRANSFER, PICKUP, DELIVERED with actor and timestamp). |
| Duplicate-reservation prevention | SQL-level conditional UPDATE returning row count; tested in `southCannotReserveNorthBox`. |
| Full delivery workflow | request → approve/reject → assign (reserve boxes + volunteer) → accept/decline → pickup (all box IDs must be typed) → handoff → **recipient** confirms → family becomes SERVED. |
| Volunteer unavailability + reassignment | `setVolunteerAvailable` auto-reassigns or marks `PENDING_ASSIGNMENT`; `reassignDelivery`. |
| Duplicate request refusal | one active request per family (`activeRequestForFamily`). |
| Urdu/English request reading with honesty | Local reader leaves unknowns empty and lists them as missing; UI labels the source (`on-device demo` vs `Gemini`) and shows confidence as an estimate; nothing is saved until a person confirms. |
| Gemini key not in the app | Key lives only in `functions/index.js` as a Firebase secret. Meets the "secure backend" rule in design. |
| Event timeline | `events` table, shown on Activity screen. |
| Local persistence | Room; all actions work with no network because there is no network dependency in the core flow. |
| Tests present | 16 tests (see §13), unexecuted. |

### 14.2 Partial / incomplete

| Requirement | What exists | What is lacking |
|---|---|---|
| Authentication / roles | Five hard-coded "demo accounts" switched by chips on Home | No registration, sign-in, or per-user role assignment. Any user can switch to any role. |
| AI request parsing | Quantity of **boxes**, station (North/South), household size, language, missing-field list | No item types (the spec's "2 water boxes + 1 food package" cannot be represented: `RequestEntity` has one `quantity`, no item); location limited to two stations (no free-text/address); no clarification dialogue beyond "Missing: …"; duplicate flagging is a per-family rule, not AI. Quantity extraction is keyword-proximity based and will be fragile on varied phrasing. |
| Gemini integration | Client, validation, and Cloud Function written | Never deployed or tested (`GEMINI_PROXY_URL` is `""`). Function has no authentication or rate limiting (its own TODO). Model id `gemini-1.5-flash` should be re-checked against current Gemini availability — I have not verified it. |
| AI recommendations | Deterministic planner (single transfer suggestion) | No Gemini-generated explanation, no after-action review, no multi-option planning. The README states this honestly. |
| Offline-first | Local DB, offline flag that tags new events `WAITING`, banner, "Sync now" | The offline switch is a **simulation toggle**, not real connectivity detection. "Sync now" only relabels local rows as `SYNCED`; there is no remote to sync to. |
| Device-to-device exchange | Export event log as JSON (clipboard), import with validation and uid-based dedup | **Not Nearby Connections.** Imported events are appended to the log only; they do **not** change inventory, requests or deliveries on the receiving phone. So two phones cannot converge on the same state. |
| Inventory management | Per-box lifecycle, audited transfers, available/reserved/delivered derivable from box status | No UI/ops for adding stock, adjusting or removing it; no stored quantities per item; no transfer *status* entity (transfers are instantaneous); no stock-movement history screen (data exists in `inventory_tx`, not displayed that I could find). |
| Delivery data model | Delivery id, request id, volunteer, station, family, box IDs, status, `failureReason`, `createdAt`, `deliveredAt` | No separate pickup timestamp field (pickup time exists only in `inventory_tx`). `S.FAILED` is declared but no repository method sets it, so "record unsuccessful attempt" exists only as *decline*. |
| Disruptions | Road closure, volunteer unavailability, offline toggle | No stock-shortage or station-capacity disruption. |
| Results comparison | Shows North/South served, waiting, transferred, avg handoff, plus per-run bars | The spec's "Boxes distributed" row is not shown as such. Before runs are recorded the table displays hard-coded "example values" (marked with `*` and explained on screen) — acceptable only because labelled; the spec wants real state. |
| Map | Canvas map of the scenario, labelled "Simulated map" | Not a real map; fine, and honestly labelled. |
| UI quality | Consistent custom palette, shared components, empty states, offline banner | Light theme only (no dark theme found). Default Material icons (`Menu` for Stations, `Settings` for Drill). Same five tabs for every role (role-specific navigation is by enabled/disabled buttons). Urdu strings exist for only ~13 strings (nav, onboarding, banner); most screen text is English literals in Kotlin. |
| Branding | — | Still "Mohalla Ops" throughout. |
| Structure vs reference | Valid but different | Flat `data/domain/ai/ui` layout; no `di/`, `core/`, `feature/*`, `navigation/`, `offline/` packages. This is acceptable per your instructions; noted for planning. |
| Hard-coded scenario | — | Station ids `"NORTH"/"SOUTH"` and road `"ROAD-NS"` appear as literals in the repository and UI. Supporting arbitrary drills will require refactoring, not just adding screens. |

### 14.3 Missing from the source entirely

- Create-drill wizard, naming/selecting scenarios, adding families/stations/volunteers/supplies, multi-drill management, pause/resume (only reset + run).
- Firebase (Auth, Firestore, FCM), `google-services.json` (correctly absent; not fabricated).
- Google Nearby Connections or any real device-to-device transport.
- Conflict detection/resolution for inventory merged across devices.
- WorkManager background sync.
- Hilt, DataStore (onboarding flag uses `SharedPreferences`).
- QR scanning for box IDs (IDs are typed).
- Item-typed inventory and requests (water, food, etc.).
- Station-capacity limits; stock-shortage injection; add/remove stock UI.
- Replay of an improved plan; AI after-action review.
- Request history view per resident beyond the live list; delivery history view per volunteer.
- Push notifications; in-app language switcher; dark theme.
- Gradle wrapper script/jar (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`), `libs.versions.toml` (versions are inline), launcher `mipmap` assets/adaptive icon, `res/xml` rules, `backend/` and `docs/` folders (a `functions/` folder exists instead), `firebase.json`.
- Analytics beyond the single two-run comparison.

---

## 15. Build and test results

**Not run.** The environment has Java but no Android SDK, no Gradle distribution, no `gradlew`/wrapper jar in the archive, and network access is disabled, so Gradle could not download anything. I did not execute any ZIP script or Gradle task. Consequently:
- Compilation status: **unknown**.
- Unit tests (`ExtractorTest`): **not run**.
- Instrumented tests (`DrillRepositoryTest`): **not run** (need a device/emulator).
- Static compatibility check by reading only: AGP 8.5.2 ↔ Gradle 8.7, Kotlin 2.0.20 ↔ KSP 2.0.20-1.0.25, compileSdk 34 ↔ AndroidX versions used all look consistent. This is not a substitute for a build.

## 16. Final status

**Extraction: COMPLETE** — 38/38 files added, 38/38 SHA-256 verified, 0 conflicts, 0 duplicates, 0 unsafe entries, 0 failures.
**Project validation: NOT PERFORMED** (no SDK / wrapper / network). Open in Android Studio (Koala+, JDK 17) to build; per the source README, run the tests first.
