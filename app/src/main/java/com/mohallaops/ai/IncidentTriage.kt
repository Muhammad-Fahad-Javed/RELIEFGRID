package com.mohallaops.ai

import com.mohallaops.BuildConfig
import com.mohallaops.domain.S
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Structured result of AI triage for an emergency/disaster incident report.
 * AI never makes autonomous life-critical decisions: requiresHumanReview is always true.
 */
data class IncidentClassification(
    val category: String,          // FLOOD, ROAD_BLOCKAGE, MEDICAL, TRAPPED, INFRASTRUCTURE, OTHER
    val urgency: String,           // CRITICAL, HIGH, MEDIUM, LOW
    val stationId: String?,        // "NORTH", "SOUTH", or null if undetermined
    val location: String?,
    val summary: String,
    val recommendedAction: String,
    val missingInfo: List<String>,
    val confidence: Float,         // 0.0 .. 1.0 honest estimate
    val requiresHumanReview: Boolean = true,
    val source: String             // "on-device triage" or "Gemini"
)

/**
 * On-device emergency triage rule engine for Urdu and English.
 * Operates completely offline with zero network dependency.
 */
object LocalIncidentTriage {
    private val criticalWords = listOf(
        // English
        "trapped", "stranded", "drowning", "infant", "baby", "pregnant", "bleeding",
        "unconscious", "heart attack", "collapsed", "severe", "life threatening", "urgent rescue",
        // Urdu
        "پھنسے", "ڈوب", "بچہ", "حاملہ", "بے ہوش", "خون", "جان خطرے میں", "فوری مدد", "چھت پر"
    )

    private val highUrgencyWords = listOf(
        // English
        "rising water", "blocked", "washed out", "no food", "diabetic", "medicine",
        "elderly", "power cut", "cut off", "overflow",
        // Urdu
        "پانی چڑھ رہا", "راستہ بند", "پل ٹوٹ", "خوراک ختم", "دوا", "بوڑھے", "رابطہ منقطع"
    )

    private val medicalWords = listOf(
        "medical", "doctor", "injured", "injury", "sick", "wound", "patient", "ambulance",
        "طبی", "ڈاکٹر", "زخمی", "بیمار", "دوا", "مرض", "ایمبولینس"
    )

    private val roadWords = listOf(
        "road", "bridge", "street", "culvert", "crossing", "passage", "route", "access",
        "راستہ", "سڑک", "پل", "گلی", "پلیا"
    )

    private val trappedWords = listOf(
        "trapped", "stranded", "marooned", "stuck", "roof", "surrounded",
        "پھنس", "گھرے", "چھت", "محبوس"
    )

    private val infrastructureWords = listOf(
        "power", "wire", "electricity", "pole", "transformer", "gas", "pipeline",
        "بجلی", "تار", "کھمبا", "ٹرانسفارمر", "گیس"
    )

    private val southWords = listOf("south", "ساؤتھ", "سائوتھ", "ساوتھ", "جنوب")
    private val northWords = listOf("north", "نارتھ", "نورتھ", "شمال")

    fun isUrdu(text: String): Boolean = text.any { it.code in 0x0600..0x06FF }

    fun triage(text: String): IncidentClassification {
        val lower = text.lowercase()
        val urdu = isUrdu(text)

        // 1. Determine category
        val category = when {
            trappedWords.any { lower.contains(it) } -> S.CAT_TRAPPED
            medicalWords.any { lower.contains(it) } -> S.CAT_MEDICAL
            roadWords.any { lower.contains(it) } -> S.CAT_ROAD
            infrastructureWords.any { lower.contains(it) } -> S.CAT_INFRASTRUCTURE
            lower.contains("flood") || lower.contains("water") || lower.contains("سیلاب") || lower.contains("پانی") -> S.CAT_FLOOD
            else -> S.CAT_OTHER
        }

        // 2. Determine urgency level
        val urgency = when {
            criticalWords.any { lower.contains(it) } -> S.URGENCY_CRITICAL
            highUrgencyWords.any { lower.contains(it) } -> S.URGENCY_HIGH
            category == S.CAT_TRAPPED || category == S.CAT_MEDICAL -> S.URGENCY_HIGH
            category == S.CAT_ROAD || category == S.CAT_FLOOD -> S.URGENCY_MEDIUM
            else -> S.URGENCY_LOW
        }

        // 3. Determine station affiliation
        val south = southWords.any { lower.contains(it) }
        val north = northWords.any { lower.contains(it) }
        val station = when {
            south && !north -> "SOUTH"
            north && !south -> "NORTH"
            else -> null
        }

        // 4. Missing information analysis
        val missing = buildList {
            if (station == null) add("Nearest station or sector (North/South)")
            if (!lower.contains("people") && !lower.contains("family") && !lower.contains("افراد") && !lower.contains("لوگ")) {
                add("Number of affected individuals")
            }
            if (text.length < 25) add("Specific landmarks or street address")
        }

        // 5. Confidence estimation
        var conf = 0.40f
        if (category != S.CAT_OTHER) conf += 0.25f
        if (station != null) conf += 0.20f
        if (criticalWords.any { lower.contains(it) } || highUrgencyWords.any { lower.contains(it) }) conf += 0.10f
        if (text.length < 15) conf -= 0.25f
        conf = conf.coerceIn(0.15f, 0.92f)

        // 6. Actionable recommendations
        val action = when (urgency) {
            S.URGENCY_CRITICAL -> "Dispatch immediate volunteer emergency team. Prioritize rescue and emergency supply allocation."
            S.URGENCY_HIGH -> "Assign nearest station volunteer to verify on ground and report accessibility."
            S.URGENCY_MEDIUM -> "Monitor situation and reserve standby supplies at ${station ?: "local"} station."
            else -> "Log incident advisory for routine follow-up."
        }

        val summary = buildString {
            append(if (urdu) "ایمرجنسی زمرہ: " else "Incident category: ")
            append(category)
            append(if (urdu) "، ترجیح: " else ", urgency: ")
            append(urgency)
            if (station != null) {
                append(if (urdu) "، قریبی اسٹیشن: " else ", nearest station: ")
                append(station)
            }
        }

        return IncidentClassification(
            category = category,
            urgency = urgency,
            stationId = station,
            location = station?.let { "Near ${it.lowercase().replaceFirstChar { c -> c.uppercase() }} Station" },
            summary = summary,
            recommendedAction = action,
            missingInfo = missing,
            confidence = conf,
            requiresHumanReview = true,
            source = "on-device triage"
        )
    }
}

/**
 * Backend client to call Gemini Cloud Function for emergency incident triage.
 * Secure: API key never stored client-side.
 */
class GeminiIncidentClient(private val endpoint: String) {
    suspend fun triage(text: String): IncidentClassification? = withContext(Dispatchers.IO) {
        try {
            val triageUrl = endpoint.trimEnd('/') + "/triageIncident"
            val conn = URL(triageUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use {
                it.write(JSONObject().put("text", text.take(1000)).toString().toByteArray())
            }
            if (conn.responseCode != 200) return@withContext null
            parse(conn.inputStream.bufferedReader().readText())
        } catch (e: Exception) {
            null
        }
    }

    internal fun parse(body: String): IncidentClassification? = try {
        val o = JSONObject(body)
        val cat = o.optString("category", S.CAT_OTHER).uppercase().takeIf {
            it in listOf(S.CAT_FLOOD, S.CAT_ROAD, S.CAT_MEDICAL, S.CAT_TRAPPED, S.CAT_INFRASTRUCTURE, S.CAT_OTHER)
        } ?: S.CAT_OTHER
        val urg = o.optString("urgency", S.URGENCY_MEDIUM).uppercase().takeIf {
            it in listOf(S.URGENCY_CRITICAL, S.URGENCY_HIGH, S.URGENCY_MEDIUM, S.URGENCY_LOW)
        } ?: S.URGENCY_MEDIUM
        val st = o.optString("stationId").uppercase().takeIf { it == "NORTH" || it == "SOUTH" }

        val missing = buildList {
            if (st == null) add("Nearest station (North or South)")
            val jsonMissing = o.optJSONArray("missing")
            if (jsonMissing != null) {
                for (i in 0 until jsonMissing.length()) {
                    add(jsonMissing.getString(i))
                }
            }
        }

        IncidentClassification(
            category = cat,
            urgency = urg,
            stationId = st,
            location = o.optString("location").takeIf { it.isNotBlank() },
            summary = o.optString("summary", "AI triaged incident"),
            recommendedAction = o.optString("recommendedAction", "Verify on ground before dispatching resources"),
            missingInfo = missing.distinct(),
            confidence = o.optDouble("confidence", 0.7).toFloat().coerceIn(0.1f, 1.0f),
            requiresHumanReview = true,
            source = "Gemini"
        )
    } catch (e: Exception) {
        null
    }
}

/** Triage service that prefers Gemini when configured, or safely falls back to local on-device triage. */
class IncidentTriageService(
    private val client: GeminiIncidentClient? =
        BuildConfig.GEMINI_PROXY_URL.takeIf { it.isNotBlank() }?.let { GeminiIncidentClient(it) }
) {
    val isAiConfigured: Boolean get() = client != null

    suspend fun triage(text: String): IncidentClassification =
        client?.triage(text) ?: LocalIncidentTriage.triage(text)
}
