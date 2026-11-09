package com.mohallaops.ai

import com.mohallaops.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Structured result of reading a supply request. Nothing here is saved until a human confirms it. */
data class Extraction(
    val stationId: String?,        // "NORTH", "SOUTH" or null when unclear
    val locationText: String?,
    val quantity: Int?,
    val householdSize: Int?,
    val language: String,          // "ur" or "en"
    val details: String,
    val missing: List<String>,
    val confidence: Float,         // heuristic or model-reported, 0..1 — never a guarantee
    val explanation: String,
    val source: String             // "on-device demo" or "Gemini"
)

/**
 * On-device, rule-based reader for English and Urdu. It runs when no Gemini backend is configured
 * (demo mode) and as the fallback if the backend fails. It is deliberately conservative: when a
 * field is unclear it is left empty and listed in `missing` so a person must fill it in.
 */
object LocalExtractor {
    private val numberWords: Map<String, Int> = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "ایک" to 1, "دو" to 2, "تین" to 3, "چار" to 4, "پانچ" to 5, "چھ" to 6,
        "سات" to 7, "آٹھ" to 8, "نو" to 9, "دس" to 10
    )
    private val southWords = listOf("south", "ساؤتھ", "سائوتھ", "ساوتھ", "جنوب")
    private val northWords = listOf("north", "نارتھ", "نورتھ", "شمال")
    private val boxWords = listOf("box", "boxes", "باکس", "باکسز", "ڈبہ", "ڈبے")
    private val peopleWords = listOf("people", "persons", "members", "family", "افراد", "لوگ", "ممبر")
    private val splitter = Regex("[\\s,.،۔!?؟:;]+")

    fun isUrdu(text: String) = text.any { it.code in 0x0600..0x06FF }

    private fun toNumber(token: String): Int? = token.lowercase().toIntOrNull() ?: numberWords[token.lowercase()]

    fun extract(text: String): Extraction {
        val urdu = isUrdu(text)
        val tokens = text.split(splitter).filter { it.isNotBlank() }
        val lower = text.lowercase()

        var quantity: Int? = null
        var household: Int? = null
        tokens.forEachIndexed { i, t ->
            val tl = t.lowercase()
            if (quantity == null && boxWords.any { tl.contains(it) }) {
                for (back in 1..3) {
                    val n = tokens.getOrNull(i - back)?.let { toNumber(it) }
                    if (n != null) { quantity = n; break }
                }
            }
            if (household == null && peopleWords.any { tl.contains(it) }) {
                for (back in 1..2) {
                    val n = tokens.getOrNull(i - back)?.let { toNumber(it) }
                    if (n != null) { household = n; break }
                }
            }
        }
        val boxMentioned = tokens.any { t -> boxWords.any { t.lowercase().contains(it) } }
        // A lone number with no box word is too ambiguous to trust: leave it empty.
        val south = southWords.any { lower.contains(it) }
        val north = northWords.any { lower.contains(it) }
        val station = when {
            south && !north -> "SOUTH"
            north && !south -> "NORTH"
            else -> null
        }

        val missing = buildList {
            if (quantity == null) add("Number of boxes")
            if (station == null) add("Nearest station (North or South)")
        }
        var conf = 0.30f
        if (quantity != null) conf += 0.30f
        if (station != null) conf += 0.30f
        if (boxMentioned) conf += 0.05f
        if (tokens.size < 3) conf -= 0.2f
        conf = conf.coerceIn(0.05f, 0.95f)

        val explanation = buildString {
            append(if (quantity != null) "Boxes read from the number next to the word 'box'. " else "No clear number of boxes found. ")
            append(if (station != null) "Station read from the place name in the message." else "Station is unclear or both stations were mentioned.")
        }
        return Extraction(
            stationId = station,
            locationText = station?.let { "Near ${it.lowercase().replaceFirstChar { c -> c.uppercase() }} Station" },
            quantity = quantity,
            householdSize = household,
            language = if (urdu) "ur" else "en",
            details = household?.let { "Household has $it people" } ?: "",
            missing = missing,
            confidence = conf,
            explanation = explanation,
            source = "on-device demo"
        )
    }
}

/**
 * Calls YOUR Cloud Function (see /functions), which holds the Gemini API key server-side.
 * The Android app never contains a Gemini key. Returns null on any failure so the caller can fall back.
 */
class GeminiBackendClient(private val endpoint: String) {
    suspend fun extract(text: String): Extraction? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(JSONObject().put("text", text.take(1000)).toString().toByteArray()) }
            if (conn.responseCode != 200) return@withContext null
            parse(conn.inputStream.bufferedReader().readText())
        } catch (e: Exception) {
            null
        }
    }

    /** Strict validation of the model's JSON before anything is shown or saved. */
    internal fun parse(body: String): Extraction? = try {
        val o = JSONObject(body)
        val station = o.optString("stationId").uppercase().takeIf { it == "NORTH" || it == "SOUTH" }
        val qty = if (o.has("quantity") && !o.isNull("quantity")) o.getInt("quantity").takeIf { it in 1..10 } else null
        val household = if (o.has("householdSize") && !o.isNull("householdSize")) o.getInt("householdSize").takeIf { it in 1..50 } else null
        val missing = buildList {
            if (qty == null) add("Number of boxes")
            if (station == null) add("Nearest station (North or South)")
        }
        Extraction(
            stationId = station,
            locationText = o.optString("location").takeIf { it.isNotBlank() },
            quantity = qty,
            householdSize = household,
            language = if (o.optString("language") == "ur") "ur" else "en",
            details = o.optString("details").take(300),
            missing = missing,
            confidence = o.optDouble("confidence", 0.5).toFloat().coerceIn(0f, 1f),
            explanation = o.optString("explanation").take(300),
            source = "Gemini"
        )
    } catch (e: Exception) {
        null
    }
}

/** Chooses Gemini when a backend URL is configured, otherwise (or on failure) the on-device reader. */
class RequestExtractionService(
    private val client: GeminiBackendClient? =
        BuildConfig.GEMINI_PROXY_URL.takeIf { it.isNotBlank() }?.let { GeminiBackendClient(it) }
) {
    val isAiConfigured: Boolean get() = client != null

    suspend fun analyse(text: String): Extraction =
        client?.extract(text) ?: LocalExtractor.extract(text)
}
