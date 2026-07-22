package com.voicetasker.app.data.ai

import android.util.Log
import com.voicetasker.app.data.remote.ApiKeyProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class NoteMetadata(
    val title: String = "",
    val improvedText: String = "",
    val date: String? = null,
    val time: String? = null,
    val location: String? = null,
    val category: String? = null
)

@Singleton
class GeminiService @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) {
    companion object {
        private const val TAG = "GeminiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Calls the Gemini API directly via OkHttp (no Ktor dependency).
     */
    private suspend fun generateContent(prompt: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1024)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "API call failed: ${response.code} ${response.message} $errorBody")
                if (response.code == 429) {
                    return@withContext "ERRORE: Quota API Gemini esaurita (Troppe richieste)."
                }
                return@withContext "ERRORE: Problema con l'API Gemini (Codice ${response.code}). Dettaglio: $errorBody"
            }

            val body = response.body?.string() ?: return@withContext "ERRORE: Risposta API vuota."
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates") ?: return@withContext null
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext null
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            val firstPart = parts.optJSONObject(0) ?: return@withContext null
            firstPart.optString("text", null)
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed", e)
            null
        }
    }

    /**
     * Estrae tutti i metadati dalla trascrizione in un'unica chiamata API.
     * Restituisce titolo, testo migliorato, data, ora, luogo e categoria suggerita.
     */
    suspend fun extractNoteMetadata(transcription: String, categoryNames: List<String>): NoteMetadata {
        if (transcription.isBlank()) return NoteMetadata(improvedText = transcription)

        val apiKey = apiKeyProvider.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "No Gemini API key available")
            return NoteMetadata(improvedText = transcription)
        }

        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return try {
            val categories = categoryNames.joinToString(", ")
            val prompt = """Sei un assistente italiano per note vocali. La data di oggi è $todayStr.
Analizza il seguente testo trascritto da una nota vocale e restituisci un JSON con questi campi:

1. "title": titolo sintetico (massimo 3 parole), che descriva l'essenza della nota. Esempi: "Dentista", "Riunione Team", "Spesa", "Compleanno Marco"
2. "improvedText": riscrivi il contenuto come una nota ordinata e chiara. Non limitarti a correggere la grammatica: sintetizza e riordina le informazioni in modo leggibile, eliminando ripetizioni e filler vocali. Mantieni tutti i dettagli importanti.
3. "date": se c'è un riferimento temporale (es. "domani", "lunedì", "il 15 maggio", "la prossima settimana"), calcola la data esatta rispetto a oggi ($todayStr) e restituisci in formato YYYY-MM-DD. Se non c'è, restituisci null
4. "time": se c'è un riferimento a un orario (es. "alle tre", "alle 15:30", "di mattina presto"), restituisci in formato HH:mm (24 ore). "di mattina" = 09:00, "di pomeriggio" = 15:00, "di sera" = 20:00. Se non c'è, restituisci null
5. "location": se c'è un luogo (es. "a Roma", "in ufficio", "dal dottore", "ospedale San Raffaele"), restituisci il nome del luogo. Se non c'è, restituisci null
6. "category": scegli la categoria più appropriata tra: $categories. Regole: se si parla di medici, visite, esami, farmaci → Salute. Se si parla di lavoro, riunioni, progetti, clienti → Lavoro. Se si parla di familiari, casa, figli → Famiglia. Restituisci il nome esatto della categoria.

Rispondi SOLO con il JSON valido, nient'altro.
Esempio: {"title":"Dentista","improvedText":"Appuntamento dal dentista per controllo annuale.","date":"2026-04-30","time":"10:30","location":"Studio dentistico","category":"Salute"}

Testo trascritto: $transcription"""

            val responseText = generateContent(prompt, apiKey)
            if (responseText == null) return NoteMetadata(improvedText = transcription)
            if (responseText.startsWith("ERRORE:")) {
                return NoteMetadata(improvedText = responseText)
            }

            Log.d(TAG, "Raw response: ${responseText.take(300)}")
            parseMetadataJson(responseText.trim(), transcription)
        } catch (e: Exception) {
            Log.e(TAG, "extractNoteMetadata FAILED", e)
            NoteMetadata(improvedText = transcription)
        }
    }

    private fun parseMetadataJson(jsonStr: String, fallbackText: String): NoteMetadata {
        return try {
            // Strip markdown code fences if present
            val clean = jsonStr
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = JSONObject(clean)
            NoteMetadata(
                title = json.optString("title", ""),
                improvedText = json.optString("improvedText", fallbackText),
                date = json.optString("date", "").takeIf { it.isNotBlank() && it != "null" },
                time = json.optString("time", "").takeIf { it.isNotBlank() && it != "null" },
                location = json.optString("location", "").takeIf { it.isNotBlank() && it != "null" },
                category = json.optString("category", "").takeIf { it.isNotBlank() && it != "null" }
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${jsonStr.take(200)}", e)
            NoteMetadata(improvedText = fallbackText)
        }
    }

    /**
     * Genera suggerimenti per una nota manuale (senza trascrizione vocale).
     */
    suspend fun suggestForManualNote(content: String, categoryNames: List<String>): NoteMetadata {
        return extractNoteMetadata(content, categoryNames)
    }

    /**
     * Reset (kept for API compatibility).
     */
    fun reset() { /* no-op with OkHttp */ }
}
