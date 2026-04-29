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
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return try {
            val categories = categoryNames.joinToString(", ")
            val response = model!!.generateContent(
                content {
                    text("""Sei un assistente italiano per note vocali. La data di oggi è $todayStr.
Analizza il seguente testo trascritto da una nota vocale e restituisci un JSON con questi campi:

1. "title": titolo sintetico (massimo 3 parole), che descriva l'essenza della nota. Esempi: "Dentista", "Riunione Team", "Spesa", "Compleanno Marco"
2. "improvedText": riscrivi il contenuto come una nota ordinata e chiara. Non limitarti a correggere la grammatica: sintetizza e riordina le informazioni in modo leggibile, eliminando ripetizioni e filler vocali. Mantieni tutti i dettagli importanti.
3. "date": se c'è un riferimento temporale (es. "domani", "lunedì", "il 15 maggio", "la prossima settimana"), calcola la data esatta rispetto a oggi ($todayStr) e restituisci in formato YYYY-MM-DD. Se non c'è, restituisci null
4. "time": se c'è un riferimento a un orario (es. "alle tre", "alle 15:30", "di mattina presto"), restituisci in formato HH:mm (24 ore). "di mattina" = 09:00, "di pomeriggio" = 15:00, "di sera" = 20:00. Se non c'è, restituisci null
5. "location": se c'è un luogo (es. "a Roma", "in ufficio", "dal dottore", "ospedale San Raffaele"), restituisci il nome del luogo. Se non c'è, restituisci null
6. "category": scegli la categoria più appropriata tra: $categories. Regole: se si parla di medici, visite, esami, farmaci → Salute. Se si parla di lavoro, riunioni, progetti, clienti → Lavoro. Se si parla di familiari, casa, figli → Famiglia. Restituisci il nome esatto della categoria.

Rispondi SOLO con il JSON valido, nient'altro.
Esempio: {"title":"Dentista","improvedText":"Appuntamento dal dentista per controllo annuale.","date":"2026-04-30","time":"10:30","location":"Studio dentistico","category":"Salute"}

Testo trascritto: $transcription""")
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
