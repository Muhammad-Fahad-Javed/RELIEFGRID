// RELIEFGRID Cloud Functions — Secure Gemini AI Proxy & Emergency Incident Triage
// The Gemini API key lives ONLY here (as a Firebase secret), never in the Android client.
// Deploy:
//   firebase functions:secrets:set GEMINI_API_KEY
//   firebase deploy --only functions

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK for auth verification & Firestore access
if (!admin.apps.length) {
  admin.initializeApp();
}

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

/**
 * Helper to optionally verify Firebase Auth token if present in headers.
 * Does not block unauthenticated demo requests, but adds user claims when present.
 */
async function authenticateRequest(req) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith("Bearer ")) {
    const idToken = authHeader.split("Bearer ")[1];
    try {
      return await admin.auth().verifyIdToken(idToken);
    } catch (e) {
      console.warn("Invalid auth token:", e.message);
      return null;
    }
  }
  return null;
}

// -----------------------------------------------------------------------------
// 1. Supply Request Extractor
// -----------------------------------------------------------------------------
const REQUEST_PROMPT = `You read short supply requests written in Urdu or English for a disaster-relief coordination application (RELIEFGRID).
Return ONLY JSON with keys:
- stationId ("NORTH", "SOUTH" or null if unclear)
- location (string or null)
- quantity (integer 1-10 boxes requested, or null if unclear)
- householdSize (integer or null)
- language ("ur" or "en")
- details (string)
- confidence (0-1, your honest certainty estimate)
- explanation (one short sentence)

Never guess: use null when the message does not explicitly say. The number of people is NOT the number of boxes.`;

exports.extractRequest = onRequest(
  { secrets: [GEMINI_API_KEY], cors: true, timeoutSeconds: 30, maxInstances: 5 },
  async (req, res) => {
    if (req.method !== "POST") return res.status(405).send("POST only");
    const text = String((req.body && req.body.text) || "").slice(0, 1000);
    if (!text.trim()) return res.status(400).send("text required");

    try {
      await authenticateRequest(req);
      const model = new GoogleGenerativeAI(GEMINI_API_KEY.value()).getGenerativeModel({
        model: "gemini-1.5-flash",
        generationConfig: { responseMimeType: "application/json", temperature: 0 },
      });
      const result = await model.generateContent(`${REQUEST_PROMPT}\n\nMessage:\n${text}`);
      const parsed = JSON.parse(result.response.text());
      res.json(parsed);
    } catch (e) {
      console.error("AI Request Extraction failed:", e);
      res.status(502).json({ error: "AI unavailable", fallback: true });
    }
  }
);

// -----------------------------------------------------------------------------
// 2. Emergency & Disaster Incident Triage
// -----------------------------------------------------------------------------
const TRIAGE_PROMPT = `You are an emergency disaster-relief triage assistant for RELIEFGRID.
Analyze the incoming disaster incident report (written or transcribed in Urdu or English).
Return ONLY valid JSON with keys:
- category: one of ["FLOOD", "ROAD_BLOCKAGE", "MEDICAL", "TRAPPED", "INFRASTRUCTURE", "OTHER"]
- urgency: one of ["CRITICAL", "HIGH", "MEDIUM", "LOW"]
  * CRITICAL: imminent threat to human life, drowning, trapped persons, severe medical trauma, infant/elderly cut off by rising water.
  * HIGH: rapidly rising flood water, primary road washed out/blocked, vulnerable cluster cut off.
  * MEDIUM: property inundation, secondary road blockage, general relief supply need.
  * LOW: minor debris, advisory notice.
- stationId: "NORTH", "SOUTH", or null if location does not clearly identify one.
- location: short string describing landmark or sector, or null.
- summary: concise single-sentence summary of the incident.
- recommendedAction: clear, actionable recommendation for relief coordinators (e.g. "Dispatch rescue boat team", "Re-route supplies via North Station").
- missing: array of strings naming missing crucial information (e.g. ["Number of people trapped", "Exact street address"]).
- confidence: number between 0.1 and 1.0 representing classification certainty.

Safety rule: Do not invent missing facts. Be conservative with life-safety ratings.`;

exports.triageIncident = onRequest(
  { secrets: [GEMINI_API_KEY], cors: true, timeoutSeconds: 30, maxInstances: 5 },
  async (req, res) => {
    if (req.method !== "POST") return res.status(405).send("POST only");
    const text = String((req.body && req.body.text) || "").slice(0, 1000);
    if (!text.trim()) return res.status(400).send("text required");

    try {
      await authenticateRequest(req);
      const model = new GoogleGenerativeAI(GEMINI_API_KEY.value()).getGenerativeModel({
        model: "gemini-1.5-flash",
        generationConfig: { responseMimeType: "application/json", temperature: 0 },
      });
      const result = await model.generateContent(`${TRIAGE_PROMPT}\n\nIncident Report:\n${text}`);
      const parsed = JSON.parse(result.response.text());
      res.json(parsed);
    } catch (e) {
      console.error("AI Incident Triage failed:", e);
      res.status(502).json({ error: "AI triage unavailable", fallback: true });
    }
  }
);
