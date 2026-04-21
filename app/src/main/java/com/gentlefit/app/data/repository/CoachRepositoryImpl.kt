package com.gentlefit.app.data.repository

import com.gentlefit.app.data.local.dao.CoachMessageDao
import com.gentlefit.app.data.local.entity.CoachMessageEntity
import com.gentlefit.app.domain.model.CoachMessage
import com.gentlefit.app.domain.model.MessageType
import com.gentlefit.app.domain.repository.CoachRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CoachRepositoryImpl @Inject constructor(
    private val coachMessageDao: CoachMessageDao
) : CoachRepository {

    private val greetings = listOf(
        "Buongiorno %s! 🌸 Che bello rivederti! Oggi è un nuovo giorno per prenderti cura di te.",
        "Ciao %s! ☀️ Sei pronta per i tuoi 5 minuti di benessere?",
        "Bentornata %s! 🌿 Ricorda: anche un piccolo gesto fa la differenza.",
        "Ehi %s! 💕 Oggi dedichiamoci qualcosa di bello, senza fretta.",
        "Buongiorno %s! 🦋 Ogni giorno un passo in più verso il tuo benessere."
    )

    private val eveningGreetings = listOf(
        "Buonasera %s! 🌙 Com'è andata la giornata? Spero tu ti sia presa un momento per te.",
        "Ciao %s! 🌛 La serata è il momento perfetto per rilassarsi un po'.",
        "Ehi %s! ✨ Hai fatto un ottimo lavoro oggi. Ricorda di riposarti bene."
    )

    private val streakMessages = listOf(
        "🔥 %d giorni di fila! Sei una forza della natura!",
        "🔥 Wow, %d giorni consecutivi! Stai creando un'abitudine fantastica!",
        "🔥 %d giorni! Ogni giorno è una piccola vittoria, e tu ne hai collezionate tante!"
    )

    private val celebrations = listOf(
        "🎉 Fantastico! Hai completato: %s! Sono fiera di te!",
        "🌟 Bravissima! %s — un altro passo avanti nel tuo percorso!",
        "💪 Ce l'hai fatta! %s! Ogni piccolo gesto conta!",
        "🎊 Meravigliosa! %s completato! Ti meriti un applauso! 👏"
    )

    private val motivations = listOf(
        "💝 Ricorda: non devi essere perfetta, devi solo essere gentile con te stessa.",
        "🌈 I grandi cambiamenti nascono da piccoli gesti quotidiani.",
        "🌸 Non è una gara. È un percorso, e ogni passo conta.",
        "💫 Oggi fai quello che puoi. Domani farai un po' di più.",
        "🦋 Il tuo corpo ti ringrazia per ogni attenzione che gli dedichi.",
        "🌿 5 minuti per te possono cambiare tutta la giornata.",
        "💕 Non devi stravolgere la tua vita. Basta un piccolo gesto alla volta.",
        "☀️ Sei qui, e questo è già un grandissimo passo!",
        "🌺 La salute non è una meta, è un viaggio. E tu lo stai facendo benissimo.",
        "✨ Anche nei giorni difficili, hai il potere di fare qualcosa di bello per te."
    )

    private val quickReplyResponses = mapOf(
        "Mi sento bene" to listOf(
            "😊 Che bello sentirti dire così! Sfruttiamo questa energia positiva!",
            "🌟 Fantastico! Quando ti senti così, ogni piccolo gesto diventa ancora più speciale.",
            "💕 Sono contenta! Ricorda questa sensazione, è il frutto delle tue scelte."
        ),
        "Oggi è dura" to listOf(
            "🤗 Ti capisco, ci sono giorni così. Non devi fare tutto — anche solo un respiro profondo va benissimo.",
            "💝 Ehi, va bene così. Anche le giornate difficili passano. Sii gentile con te stessa oggi.",
            "🌸 Non ti giudico e non ti giudicherai nemmeno tu. Fai quello che riesci, va bene tutto."
        ),
        "Ho bisogno di motivazione" to listOf(
            "💪 Sei più forte di quanto pensi! Hai già fatto passi incredibili per arrivare fin qui.",
            "🌈 Pensa a come ti sentirai stasera sapendo che hai fatto qualcosa per te. Anche 2 minuti!",
            "✨ Ogni piccolo gesto è un atto d'amore verso te stessa. Tu meriti di stare bene."
        ),
        "Raccontami qualcosa" to listOf(
            "🧠 Lo sapevi che bastano 5 minuti di stretching al giorno per migliorare la postura e ridurre lo stress?",
            "💧 Un bicchiere d'acqua in più al giorno può migliorare l'energia e la concentrazione!",
            "🌿 Camminare 20 minuti al giorno riduce l'ansia del 30%. Anche 5 minuti aiutano!"
        )
    )

    override fun getMessages(): Flow<List<CoachMessage>> {
        return coachMessageDao.getAllMessages().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addMessage(message: CoachMessage) {
        coachMessageDao.insertMessage(message.toEntity())
    }

    override fun getGreeting(userName: String, hour: Int, streakDays: Int): CoachMessage {
        val name = userName.ifBlank { "cara" }
        val baseGreeting = if (hour < 17) {
            greetings.random().format(name)
        } else {
            eveningGreetings.random().format(name)
        }

        val streakText = if (streakDays > 1) {
            "\n\n" + streakMessages.random().format(streakDays)
        } else ""

        return CoachMessage(
            text = baseGreeting + streakText,
            type = MessageType.COACH,
            quickReplies = listOf("Mi sento bene", "Oggi è dura", "Ho bisogno di motivazione", "Raccontami qualcosa")
        )
    }

    override fun getCelebration(achievement: String): CoachMessage {
        return CoachMessage(
            text = celebrations.random().format(achievement),
            type = MessageType.CELEBRATION
        )
    }

    override fun getMotivation(): CoachMessage {
        return CoachMessage(
            text = motivations.random(),
            type = MessageType.COACH,
            quickReplies = listOf("Mi sento bene", "Ho bisogno di motivazione")
        )
    }

    override fun getQuickReplyResponse(reply: String): CoachMessage {
        val responses = quickReplyResponses[reply] ?: motivations
        return CoachMessage(
            text = responses.random(),
            type = MessageType.COACH,
            quickReplies = listOf("Mi sento bene", "Oggi è dura", "Ho bisogno di motivazione", "Raccontami qualcosa")
        )
    }

    private fun CoachMessageEntity.toDomain(): CoachMessage = CoachMessage(
        id = id,
        text = text,
        type = try { MessageType.valueOf(type) } catch (e: Exception) { MessageType.COACH },
        timestamp = timestamp,
        quickReplies = if (quickReplies.isBlank()) emptyList() else quickReplies.split(",")
    )

    private fun CoachMessage.toEntity(): CoachMessageEntity = CoachMessageEntity(
        id = id,
        text = text,
        type = type.name,
        timestamp = timestamp,
        quickReplies = quickReplies.joinToString(",")
    )
}
