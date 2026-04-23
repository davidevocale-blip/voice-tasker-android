package com.voicetasker.app.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.voicetasker.app.BuildConfig
import org.json.JSONObject
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
class GeminiService @Inject constructor() {

    private val model: GenerativeModel? by lazy {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) null
        else GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = key,
            generationConfig = generationConfig {
                temperature = 0.3f
                maxOutputTokens = 1024
            }
        )
    }

    val isAvailable: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /**
     * Estrae tutti i metadati dalla trascrizione in un'unica chiamata API.
     * Restituisce titolo, testo migliorato, data, ora, luogo e categoria suggerita.
     */
    suspend fun extractNoteMetadata(transcription: String, categoryNames: List<String>): NoteMetadata {
        if (transcription.isBlank() || model == null) return NoteMetadata(improvedText = transcription)
        return try {
            val categories = categoryNames.joinToString(", ")
            val response = model!!.generateContent(
                content {
                    text("""Sei un assistente italiano per note vocali. Analizza il seguente testo trascritto da voce e restituisci un JSON con questi campi:

1. "title": titolo sintetico (max 6 parole), descrittivo del contenuto
2. "improvedText": il testo corretto con punteggiatura e grammatica corretta, mantenendo il significato originale
3. "date": se nel testo c'è un riferimento a una data specifica (es. "domani", "lunedì", "il 15 marzo"), restituisci la data in formato YYYY-MM-DD. Se non c'è, restituisci null
4. "time": se nel testo c'è un riferimento a un orario (es. "alle tre", "alle 15:30", "di mattina"), restituisci l'orario in formato HH:mm. Se non c'è, restituisci null
5. "location": se nel testo c'è un riferimento a un luogo (es. "a Roma", "in ufficio", "dal dottore", "al parco"), restituisci il nome del luogo. Se non c'è, restituisci null
6. "category": scegli la categoria più appropriata tra: $categories. Restituisci il nome esatto

Rispondi SOLO con il JSON valido, niente altro. Esempio:
{"title":"Riunione con il team","improvedText":"Domani alle 15 ho una riunione con il team di sviluppo in ufficio.","date":"2026-04-24","time":"15:00","location":"Ufficio","category":"Lavoro"}

Testo da analizzare: $transcription""")
                }
            )
            val jsonStr = response.text?.trim() ?: return NoteMetadata(improvedText = transcription)
            parseMetadataJson(jsonStr, transcription)
        } catch (e: Exception) {
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
            NoteMetadata(improvedText = fallbackText)
        }
    }

    /**
     * Genera suggerimenti per una nota manuale (senza trascrizione vocale).
     */
    suspend fun suggestForManualNote(content: String, categoryNames: List<String>): NoteMetadata {
        return extractNoteMetadata(content, categoryNames)
    }
}
