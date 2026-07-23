package com.voicetasker.app.data.ai

import com.voicetasker.app.domain.ai.NoteMetadata
import kotlinx.serialization.Serializable

@Serializable
internal data class ProcessNoteAiRequest(
    val text: String,
    val categoryNames: List<String>,
    val currentDate: String
)

@Serializable
internal data class ProcessNoteAiResponse(
    val title: String,
    val improvedText: String,
    val date: String?,
    val time: String?,
    val location: String?,
    val category: String?
) {
    fun toDomain(): NoteMetadata = NoteMetadata(
        title = title,
        improvedText = improvedText,
        date = date,
        time = time,
        location = location,
        category = category
    )
}
