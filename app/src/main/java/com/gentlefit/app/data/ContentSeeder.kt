package com.gentlefit.app.data

import com.gentlefit.app.data.local.dao.GoalDao
import com.gentlefit.app.data.local.dao.NewsDao
import com.gentlefit.app.data.local.dao.RoutineDao
import com.gentlefit.app.data.local.entity.GoalEntity
import com.gentlefit.app.data.local.entity.NewsEntity
import com.gentlefit.app.data.local.entity.RoutineEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ContentSeeder {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val exercises = listOf(
        Triple("Stretching mattutino", "Allunga dolcemente braccia, collo e schiena. Movimenti lenti e controllati per risvegliare il corpo.", 5),
        Triple("Camminata consapevole", "Cammina con attenzione ad ogni passo. Respira profondamente e osserva il mondo intorno a te.", 8),
        Triple("Mobilità articolare", "Ruota dolcemente polsi, caviglie, spalle e anche. Scioglie le tensioni accumulate.", 6),
        Triple("Yoga dolce del mattino", "Pochi movimenti yoga per iniziare la giornata con calma: gatto-mucca, bambino, montagna.", 7),
        Triple("Respirazione profonda", "Inspira per 4 secondi, trattieni per 4, espira per 6. Ripeti per 5 minuti.", 5),
        Triple("Stretching della schiena", "Esercizi dolci per sciogliere la zona lombare e dorsale. Perfetto dopo ore sedute.", 6),
        Triple("Postura consapevole", "Esercizi per allineare spalle e colonna. Semplici ma efficaci.", 5),
        Triple("Mini camminata energizzante", "Una breve camminata a passo svelto per riattivare la circolazione.", 8),
        Triple("Stretching del collo", "Movimenti delicati per sciogliere le tensioni cervicali. Ideale dopo il lavoro.", 5),
        Triple("Rilassamento progressivo", "Contrai e rilascia ogni gruppo muscolare, partendo dai piedi fino alla testa.", 7),
        Triple("Yoga della sera", "Sequenza calmante per preparare il corpo al riposo. Movimenti fluidi e respiro lungo.", 8),
        Triple("Equilibrio e stabilità", "Esercizi semplici su una gamba per migliorare equilibrio e consapevolezza corporea.", 5),
        Triple("Apertura del petto", "Movimenti per aprire il petto e migliorare la respirazione. Libera le tensioni.", 6),
        Triple("Camminata meditativa", "Cammina lentamente, sentendo ogni contatto del piede con il suolo.", 8),
        Triple("Stretching gambe dolce", "Allunga quadricipiti, polpacci e ischiocrurali con movimenti gentili.", 6),
        Triple("Mobilità delle spalle", "Circoli e movimenti per liberare le spalle dalla rigidità quotidiana.", 5),
        Triple("Mini flow yoga", "Una piccola sequenza fluida: saluto al sole modificato, adatto a tutti.", 7),
        Triple("Esercizi per la postura", "Rinforza dolcemente i muscoli che sostengono la colonna vertebrale.", 6),
        Triple("Cammina e respira", "Combina una breve camminata con la respirazione ritmica 4-4-6.", 8),
        Triple("Stretching totale", "Un giro completo di stretching dalla testa ai piedi, lento e gentile.", 7),
        Triple("Rilassamento schiena", "Posizioni sdraiata per lasciare andare le tensioni della schiena.", 6),
        Triple("Yoga del respiro", "Pranayama: tecniche di respirazione yoga per calma e energia.", 5),
        Triple("Mobilità anche", "Esercizi per le anche, spesso rigide per chi sta seduta. Movimenti circolari.", 6),
        Triple("Camminata con gratitudine", "Mentre cammini, pensa a 3 cose per cui sei grata oggi.", 8),
        Triple("Stretching al risveglio", "Ancora a letto? Perfetto! Allunga braccia e gambe prima di alzarti.", 5),
        Triple("Mini danza libera", "Metti la tua canzone preferita e muoviti liberamente. Nessuna regola!", 5),
        Triple("Esercizi piedi e caviglie", "Spesso trascurati: ruota, fletti e punta i piedi per migliorare la circolazione.", 5),
        Triple("Stretching ufficio", "Esercizi che puoi fare anche alla scrivania, senza alzarti.", 5),
        Triple("Yoga della gratitudine", "Poche posizioni con focus sulla gratitudine verso il tuo corpo.", 7),
        Triple("Passeggiata serale", "Una breve passeggiata dopo cena per digerire e rilassarsi.", 8),
    )

    private val exerciseTypes = listOf(
        "STRETCHING", "CAMMINATA", "MOBILITA", "YOGA", "RESPIRAZIONE", "STRETCHING",
        "POSTURA", "CAMMINATA", "STRETCHING", "RESPIRAZIONE", "YOGA", "MOBILITA",
        "STRETCHING", "CAMMINATA", "STRETCHING", "MOBILITA", "YOGA", "POSTURA",
        "CAMMINATA", "STRETCHING", "STRETCHING", "RESPIRAZIONE", "MOBILITA", "CAMMINATA",
        "STRETCHING", "MOBILITA", "MOBILITA", "STRETCHING", "YOGA", "CAMMINATA"
    )

    private val foodTips = listOf(
        "Oggi riduci lo zucchero nel caffè. Prova con la metà! ☕",
        "Aggiungi un frutto come spuntino di metà mattina 🍎",
        "Prova a bere un bicchiere d'acqua prima di pranzo 💧",
        "Oggi scegli pane integrale invece che bianco 🍞",
        "Aggiungi una porzione extra di verdure a pranzo 🥗",
        "Sostituisci una bibita con acqua aromatizzata al limone 🍋",
        "Prova a masticare ogni boccone 20 volte. Aiuta la digestione! 🍽️",
        "Oggi fai merenda con della frutta secca: noci o mandorle 🥜",
        "Riduci il sale a cena. Prova con spezie ed erbe aromatiche 🌿",
        "Mangia un kiwi: ricco di vitamina C e ottimo per l'intestino 🥝",
        "Prova una tisana dopo cena invece del dolce 🍵",
        "Oggi prepara un piatto colorato: più colori = più nutrienti 🌈",
        "Aggiungi semi di lino o chia allo yogurt 🥄",
        "Bevi almeno 6 bicchieri d'acqua oggi 💧",
        "Prova a cenare un'ora prima del solito ⏰",
        "Oggi mangia una porzione di legumi: lenticchie, ceci o fagioli 🫘",
        "Sostituisci il dolce con un quadratino di cioccolato fondente 🍫",
        "Aggiungi avocado al pranzo: grassi buoni per il cuore 🥑",
        "Prova a mangiare senza telefono oggi. Goditi il pasto! 📱❌",
        "Bevi un frullato di frutta fresca come spuntino 🍓",
        "Oggi prova una spezia nuova: curcuma, zenzero o cannella ✨",
        "Mangia pesce oggi: omega-3 per il cervello e l'umore 🐟",
        "Prepara una zuppa di verdure: calda e nutriente 🍲",
        "Sostituisci le patatine con delle carote crude 🥕",
        "Prova a non mangiare dopo le 21:00 stasera 🌙",
        "Aggiungi dello yogurt greco con miele per colazione 🍯",
        "Oggi bevi acqua con fette di cetriolo: fresca e depurativa 🥒",
        "Mangia 5 porzioni di frutta e verdura oggi 🍇🥬",
        "Prova il tè verde al posto del caffè del pomeriggio 🍵",
        "Oggi cucina qualcosa di semplice e fatto in casa 👩‍🍳"
    )

    private val dailyGoals = listOf(
        "Cammina 2000 passi in più oggi 🚶‍♀️",
        "Bevi 1 bicchiere d'acqua in più del solito 💧",
        "Fai 3 respiri profondi prima di pranzo 🌬️",
        "Vai a letto 15 minuti prima stasera 😴",
        "Fai una pausa di 5 minuti dallo schermo 📵",
        "Sorridi a uno sconosciuto oggi 😊",
        "Ascolta la tua canzone preferita 🎵",
        "Scrivi 3 cose per cui sei grata 📝",
        "Fai le scale invece dell'ascensore 🪜",
        "Chiama un'amica per 5 minuti 📞",
        "Stacca il telefono per 30 minuti 📱",
        "Fai una passeggiata di 10 minuti 🌳",
        "Bevi una tisana rilassante 🍵",
        "Leggi 5 pagine di un libro 📖",
        "Siediti dritta per 5 minuti e respira 🧘",
        "Mangia un pasto senza fretta 🍽️",
        "Fai un complimento a qualcuno 💕",
        "Prenditi 5 minuti solo per te ✨",
        "Stirati appena sveglia 🌅",
        "Guarda il cielo per 2 minuti 🌤️",
        "Bevi acqua calda con limone al mattino 🍋",
        "Cammina durante una telefonata 📱🚶‍♀️",
        "Fai 10 squat mentre aspetti il caffè ☕",
        "Dedica 5 minuti al silenzio 🤫",
        "Organizza un angolo della casa 🏠",
        "Cucina qualcosa con amore 👩‍🍳",
        "Fai stretching mentre guardi la TV 📺",
        "Vai a fare la spesa a piedi 🛒",
        "Ascolta un podcast motivazionale 🎧",
        "Regalati un piccolo momento di gioia 🌸"
    )

    private val motivationalQuotes = listOf(
        "\"Il cambiamento più grande inizia dal gesto più piccolo.\" 🌱",
        "\"Non devi essere perfetta, devi solo essere te stessa.\" 💕",
        "\"Ogni giorno è una nuova opportunità per stare un po' meglio.\" ☀️",
        "\"La gentilezza verso te stessa è il primo passo del benessere.\" 🌸",
        "\"5 minuti per te oggi = una vita migliore domani.\" ⏰",
        "\"Il tuo corpo è la tua casa. Prenditi cura di lui.\" 🏡",
        "\"Non è mai troppo tardi per iniziare qualcosa di bello.\" 🦋",
        "\"Piccoli gesti, grandi risultati. Fidati del processo.\" 🌿",
        "\"Tu meriti di stare bene. Ogni giorno.\" ✨",
        "\"La forza non è fare tutto, è scegliere cosa conta.\" 💪",
        "\"Il viaggio di mille miglia inizia con un singolo passo.\" 🚶‍♀️",
        "\"Sii paziente con te stessa. La crescita richiede tempo.\" 🌷",
        "\"Oggi è un buon giorno per prenderti cura di te.\" 🌞",
        "\"Non confrontarti con gli altri. Il tuo percorso è unico.\" 🌈",
        "\"Respira. Sei viva. Questo è già meraviglioso.\" 🌬️"
    )

    fun getMotivationalQuote(dayIndex: Int): String {
        return motivationalQuotes[dayIndex % motivationalQuotes.size]
    }

    suspend fun seedRoutines(routineDao: RoutineDao) {
        val today = LocalDate.now()
        for (i in 0 until 30) {
            val date = today.plusDays(i.toLong()).format(dateFormatter)
            val exercise = exercises[i]
            routineDao.insertRoutine(
                RoutineEntity(
                    date = date,
                    exerciseTitle = exercise.first,
                    exerciseDescription = exercise.second,
                    exerciseDurationMin = exercise.third,
                    exerciseType = exerciseTypes[i],
                    foodTip = foodTips[i],
                    dailyGoal = dailyGoals[i]
                )
            )
        }
    }

    suspend fun seedGoals(goalDao: GoalDao) {
        val today = LocalDate.now().format(dateFormatter)
        val defaultGoals = listOf(
            GoalEntity(title = "Bevi 6 bicchieri d'acqua", description = "Idratati durante la giornata", category = "ACQUA", createdDate = today),
            GoalEntity(title = "Cammina 15 minuti", description = "Una breve passeggiata fa miracoli", category = "MOVIMENTO", createdDate = today),
            GoalEntity(title = "5 minuti di respiro", description = "Fermati e respira consapevolmente", category = "RELAX", createdDate = today),
            GoalEntity(title = "Mangia un frutto", description = "Aggiungi un frutto alla giornata", category = "ALIMENTAZIONE", createdDate = today),
        )
        defaultGoals.forEach { goalDao.insertGoal(it) }
    }

    suspend fun seedNews(newsDao: NewsDao) {
        val news = listOf(
            NewsEntity(
                title = "5 motivi per cui camminare è il miglior esercizio",
                summary = "Scopri perché una semplice camminata quotidiana può trasformare la tua salute.",
                content = "Camminare è sottovalutato, ma è uno degli esercizi più efficaci e accessibili.\n\n1. **Brucia calorie senza stress** — Una camminata di 30 minuti brucia circa 150 calorie.\n\n2. **Migliora l'umore** — Rilascia endorfine e riduce cortisolo.\n\n3. **Protegge il cuore** — Riduce il rischio cardiovascolare del 30%.\n\n4. **Non richiede attrezzatura** — Basta un paio di scarpe comode.\n\n5. **Si adatta a tutti** — Puoi farla ovunque, a qualsiasi ritmo.",
                category = "MOVIMENTO",
                publishedDate = "2026-04-21"
            ),
            NewsEntity(
                title = "Come dormire meglio: 7 consigli semplici",
                summary = "Piccole abitudini serali che possono migliorare drasticamente il tuo sonno.",
                content = "Il sonno è fondamentale per il benessere. Ecco 7 consigli pratici:\n\n1. Spegni gli schermi 30 minuti prima di dormire\n2. Mantieni una temperatura fresca in camera\n3. Bevi una tisana alla camomilla\n4. Fai stretching leggero prima di coricarti\n5. Usa la tecnica 4-7-8 per addormentarti\n6. Vai a letto sempre alla stessa ora\n7. Evita caffeina dopo le 15:00",
                category = "BENESSERE",
                publishedDate = "2026-04-20"
            ),
            NewsEntity(
                title = "Alimentazione anti-gonfiore: cosa mangiare",
                summary = "Cibi che aiutano a ridurre il gonfiore addominale in modo naturale.",
                content = "Il gonfiore è un problema comune. Ecco gli alimenti che aiutano:\n\n• **Finocchio** — riduce i gas intestinali\n• **Zenzero** — stimola la digestione\n• **Ananas** — contiene bromelina, enzima digestivo\n• **Cetriolo** — idrata e sgonfia\n• **Yogurt** — probiotici per l'intestino\n\nEvita: bibite gassate, chewing gum, eccesso di sale, legumi non ammollati.",
                category = "ALIMENTAZIONE",
                publishedDate = "2026-04-19"
            ),
            NewsEntity(
                title = "Stress e cortisolo: come spezzare il ciclo",
                summary = "Capire il legame tra stress e peso, e come gestirlo con gesti semplici.",
                content = "Lo stress cronico alza il cortisolo, che:\n\n• Aumenta la fame nervosa\n• Favorisce l'accumulo di grasso addominale\n• Disturba il sonno\n• Abbassa le difese immunitarie\n\nCome gestirlo:\n1. Respirazione diaframmatica 5 min/giorno\n2. Camminata all'aperto\n3. Ridurre caffeina\n4. Dormire 7-8 ore\n5. Parlare con qualcuno di fiducia",
                category = "MENTE",
                publishedDate = "2026-04-18"
            ),
            NewsEntity(
                title = "Stretching: perché dovresti farlo ogni giorno",
                summary = "I benefici sorprendenti dello stretching quotidiano sul corpo e la mente.",
                content = "Lo stretching non è solo per sportivi. 5-10 minuti al giorno possono:\n\n• Ridurre i dolori alla schiena del 40%\n• Migliorare la postura\n• Ridurre lo stress e la tensione muscolare\n• Aumentare la flessibilità e la mobilità\n• Migliorare la circolazione sanguigna\n\nNon serve essere flessibili: parti da dove sei, il corpo si adatterà.",
                category = "MOVIMENTO",
                publishedDate = "2026-04-17"
            ),
            NewsEntity(
                title = "La colazione perfetta per avere energia tutto il giorno",
                summary = "Combinazioni alimentari per una colazione che ti sostiene senza appesantirti.",
                content = "Una colazione bilanciata include:\n\n• **Proteine**: yogurt greco, uova, frutta secca\n• **Carboidrati complessi**: avena, pane integrale\n• **Grassi buoni**: avocado, semi di chia\n• **Frutta fresca**: per vitamine e fibre\n\nEvita: cornetti industriali, succhi confezionati, eccesso di zucchero. Il segreto è la combinazione, non la quantità.",
                category = "ALIMENTAZIONE",
                publishedDate = "2026-04-16"
            ),
            NewsEntity(
                title = "Menopausa e benessere: guida gentile",
                summary = "Come affrontare i cambiamenti con serenità e piccole abitudini quotidiane.",
                content = "La menopausa è una fase naturale, non una malattia. Ecco come viverla al meglio:\n\n• **Movimento dolce**: yoga, pilates, camminate\n• **Alimentazione**: più calcio, vitamina D, omega-3\n• **Idratazione**: almeno 2 litri d'acqua al giorno\n• **Sonno**: routine serale regolare\n• **Supporto emotivo**: parlare, condividere, non isolarsi\n\nOgni donna vive la menopausa in modo diverso. Ascolta il tuo corpo.",
                category = "BENESSERE",
                publishedDate = "2026-04-15"
            ),
            NewsEntity(
                title = "Mindfulness in 5 minuti: guida pratica",
                summary = "Tecniche di consapevolezza che puoi praticare ovunque, in pochi minuti.",
                content = "La mindfulness non richiede ore di meditazione. Prova:\n\n1. **Body scan (2 min)**: chiudi gli occhi, scansiona mentalmente il corpo dalla testa ai piedi\n2. **5-4-3-2-1**: nota 5 cose che vedi, 4 che tocchi, 3 che senti, 2 che odori, 1 che gusti\n3. **Respiro consapevole (1 min)**: conta i respiri fino a 10\n4. **Mangiare consapevole**: assapora ogni boccone del pranzo\n\nBastano 5 minuti per cambiare la tua giornata.",
                category = "MENTE",
                publishedDate = "2026-04-14"
            ),
            NewsEntity(
                title = "Acqua: quanta berne e perché è così importante",
                summary = "La guida definitiva all'idratazione per il tuo benessere quotidiano.",
                content = "L'acqua è essenziale per ogni funzione del corpo:\n\n• **Quanta**: 1.5-2 litri al giorno (6-8 bicchieri)\n• **Quando**: un bicchiere appena sveglia, prima dei pasti, durante il giorno\n• **Benefici**: pelle più luminosa, digestione migliore, più energia, meno mal di testa\n\nTrucco: tieni una bottiglia d'acqua sempre visibile. Se non ti piace il gusto, aggiungi limone, cetriolo o menta.",
                category = "BENESSERE",
                publishedDate = "2026-04-13"
            ),
            NewsEntity(
                title = "Postura corretta: esercizi per chi sta seduta tutto il giorno",
                summary = "Semplici esercizi alla scrivania per prevenire mal di schiena e cervicale.",
                content = "Stare sedute troppo a lungo causa:\n• Mal di schiena\n• Tensione cervicale\n• Affaticamento\n\nEsercizi da fare alla scrivania:\n1. **Rotazione collo**: 5 cerchi per lato\n2. **Apertura petto**: mani dietro la testa, apri i gomiti\n3. **Cat-cow da seduta**: inarca e arrotonda la schiena\n4. **Stretching polsi**: estendi e fletti\n5. **Alzati ogni 30 minuti**: anche solo per 1 minuto\n\nLa regola d'oro: ogni 30 minuti, muoviti!",
                category = "MOVIMENTO",
                publishedDate = "2026-04-12"
            )
        )
        newsDao.insertAllNews(news)
    }
}
