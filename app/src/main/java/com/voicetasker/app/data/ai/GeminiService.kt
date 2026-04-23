package com.voicetasker.app.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.voicetasker.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {

    private val model: GenerativeModel? by lazy {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) null
        else GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = key,
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 512
            }
        )
    }

    val isAvailable: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /**
     * Migliora/corregge la trascrizione grezza dal SpeechRecognizer.
     */
    suspend fun improveTranscription(rawText: String): String {
        if (rawText.isBlank() || model == null) return rawText
        return try {
            val response = model!!.generateContent(
                content {
                    text("""Sei un assistente di trascrizione italiano. 
Correggi errori grammaticali, punteggiatura e formattazione del seguente testo trascritto da voce.
Mantieni il significato originale. Rispondi SOLO con il testo corretto, nient'altro.

Testo: $rawText""")
                }
            )
            response.text?.trim() ?: rawText
        } catch (e: Exception) { rawText }
    }

    /**
     * Genera un titolo breve e descrittivo dal contenuto della nota.
     */
    suspend fun generateTitle(content: String): String {
        if (content.isBlank() || model == null) return ""
        return try {
            val response = model!!.generateContent(
                content {
                    text("""Genera un titolo breve (massimo 6 parole) in italiano per questa nota.
Rispondi SOLO con il titolo, senza virgolette, senza punteggiatura finale.

Nota: $content""")
                }
            )
            response.text?.trim()?.removeSurrounding("\"") ?: ""
        } catch (e: Exception) { "" }
    }

    /**
     * Suggerisce la categoria migliore tra quelle disponibili.
     * Restituisce il nome esatto della categoria suggerita.
     */
    suspend fun suggestCategory(content: String, categoryNames: List<String>): String? {
        if (content.isBlank() || categoryNames.isEmpty() || model == null) return null
        return try {
            val categories = categoryNames.joinToString(", ")
            val response = model!!.generateContent(
                content {
                    text("""Dato il seguente testo, scegli la categoria più appropriata tra: $categories.
Rispondi SOLO con il nome esatto della categoria, nient'altro.

Testo: $content""")
                }
            )
            val suggested = response.text?.trim() ?: return null
            categoryNames.find { it.equals(suggested, ignoreCase = true) }
        } catch (e: Exception) { null }
    }

    /**
     * Genera un riassunto breve della nota.
     */
    suspend fun summarize(content: String): String {
        if (content.isBlank() || model == null) return ""
        return try {
            val response = model!!.generateContent(
                content {
                    text("""Riassumi in massimo 2 frasi brevi in italiano il seguente testo.
Rispondi SOLO con il riassunto.

Testo: $content""")
                }
            )
            response.text?.trim() ?: ""
        } catch (e: Exception) { "" }
    }
}
