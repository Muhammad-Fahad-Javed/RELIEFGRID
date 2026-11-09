# RELIEFGRID — Disaster Relief Coordination System

> **Coordinate relief. Save lives.**  
> A disaster-relief and emergency drill coordination application built with modern Android (Kotlin + Jetpack Compose), Room database, meaningful AI triage (on-device + Gemini Cloud Functions), and Firebase.

---

## 1. Quick Start & Build

1. Clone or open the repository in **Android Studio (Koala 2024.1.1 or newer)** with **JDK 17**.
2. Let Gradle sync. Gradle wrapper scripts (`gradlew`, `gradlew.bat`) and `gradle/wrapper/gradle-wrapper.jar` (Gradle 8.7) are provided.
3. If building from the command line:
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```
4. Output APK location: `app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Core Capabilities

### 🛡️ Emergency & Disaster Incident Reporting
- **Multi-Category Incident Triage**: Report critical events including Floods, Road Blockages, Medical Emergencies, Trapped Residents, and Infrastructure Hazards.
- **Urgency Levels**: Triage priority levels (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) computed with AI advisory and verified by Station Leads.
- **Visual Incident Mapping**: Reactive pins and emergency markers on the simulated neighborhood map.

### 📦 Supply & Resource Coordination
- **Deterministic Inventory Ownership**: Physical relief supply boxes with strict ownership enforced in Room SQL (`ownerStationId`).
- **Complete Delivery Lifecycle**:
  `Submit Request` → `Lead Review & Approval` → `Assign Delivery & Reserve Stock` → `Volunteer Acceptance` → `Physical Box ID Verification & Pickup` → `Handoff Record` → `Recipient Confirmation`.
- **Recipient Confirmation Rule**: A family is **never** counted as served until the recipient family confirms receipt.

### 🤖 Meaningful AI Integration (Safe & Transparent)
- **Local On-Device Engine**: Completely offline, rule-based request parsing and incident triage supporting English and Urdu without internet.
- **Gemini 1.5 Flash Cloud Proxy**: Serverless Cloud Function in `functions/index.js` keeping API keys completely private in Firebase Secret Manager (`GEMINI_API_KEY`).
- **Safety Guarantee**: AI provides advisory recommendations only (`requiresHumanReview = true`). Life-safety decisions always require on-ground human authorization.
- **Deterministic Planner**: Detects stock shortages across stations and suggests pre-closure transfers based on stored numbers.

### 👥 Role-Based Access Control (RBAC)
- **Resident**: Submit emergency requests in Urdu/English, report incidents, track deliveries, confirm receipt.
- **Station Lead**: Review and approve station requests, verify reported incidents, assign deliveries, manage inventory.
- **Volunteer**: Accept/decline delivery dispatches, enter verified box IDs on pickup, report handoffs, assist in resolving incidents.
- **Drill Leader**: High-level command, execute automated demo scenarios (Run 1 & Run 2), simulate disruptions (road closures, volunteer outages, offline simulation).

---

## 3. Two-Run Demonstration

1. Select **Drill Leader** demo account on Home.
2. Navigate to **Drill** tab.
3. Tap **Run 1 — no transfer**: Resets scenario, closes road, attempts fulfilment. 3 families remain waiting due to supply imbalances.
4. Tap **Run 2 — move 3 boxes North → South first**: AI planner suggests moving 3 boxes before closure; all 6 families are successfully served.
5. Tap **Compare results** to see a side-by-side comparison of real, measured database metrics.

---

## 4. Firebase Architecture

- `firebase.json`: Configuration for Cloud Functions, Firestore, and Cloud Storage.
- `firestore.rules`: Production-grade security rules enforcing RBAC across stations, boxes, requests, deliveries, incidents, and immutable events.
- `storage.rules`: Secure image upload rules for incident damage documentation (< 5MB, image types only).
- `functions/index.js`:
  - `extractRequest`: Parses supply requests into structured JSON.
  - `triageIncident`: Emergency classification, urgency assessment, and resource coordination advice.
  - Optional Firebase Auth token verification.

---

## 5. Automated Tests

- **Unit Tests** (`app/src/test/java/com/mohallaops/ExtractorTest.kt`):
  - English & Urdu supply extraction
  - Missing field detection and confidence estimation
  - Deterministic planner rebalancing & closed-road rules
  - AI emergency incident triage (Urdu trapped person critical, road blockage, medical trauma)
- **Instrumented Tests** (`app/src/androidTest/java/com/mohallaops/DrillRepositoryTest.kt`):
  - Run 1 vs Run 2 real outcome verification
  - Box reservation cross-station denial in SQL
  - Role-based permission enforcement
  - Confirmation-gated served status
  - Deduplication of imported events
