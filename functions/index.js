// Secure Gemini proxy. The API key lives ONLY here (as a Firebase secret), never in the Android app.
// Deploy:  firebase functions:secrets:set GEMINI_API_KEY   then   firebase deploy --only functions
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleGenerativeAI } = require("@google/generative-ai");

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

const PROMPT = `You read short supply requests written in Urdu or English for a neighborhood flood-relief PRACTICE drill.
Return ONLY JSON with keys: stationId ("NORTH", "SOUTH" or null if unclear), location (string or null),
quantity (integer 1-10 boxes requested, or null if unclear), householdSize (integer or null),
language ("ur" or "en"), details (string), confidence (0-1, your honest certainty), explanation (one short sentence).
Never guess: use null when the message does not say. The number of people is NOT the number of boxes.`;

exports.extractRequest = onRequest(
  { secrets: [GEMINI_API_KEY], cors: true, timeoutSeconds: 30, maxInstances: 5 },
  async (req, res) => {
    if (req.method !== "POST") return res.status(405).send("POST only");
    const text = String((req.body && req.body.text) || "").slice(0, 1000);
    if (!text.trim()) return res.status(400).send("text required");
    try {
      const model = new GoogleGenerativeAI(GEMINI_API_KEY.value()).getGenerativeModel({
        model: "gemini-1.5-flash",
        generationConfig: { responseMimeType: "application/json", temperature: 0 },
      });
      const result = await model.generateContent(`${PROMPT}\n\nMessage:\n${text}`);
      const parsed = JSON.parse(result.response.text());
      res.json(parsed);
    } catch (e) {
      res.status(502).send("AI unavailable");
    }
  }
);
// TODO (production): require a Firebase Auth ID token and add per-user rate limiting.
