# 🌸 GentleFit – Il benessere senza sforzo

Un'app Android nativa pensata per donne 30–60 che vogliono sentirsi meglio, dimagrire e migliorare la salute senza stravolgere la vita.

> "Ti aiuto a stare meglio senza obbligarti a diventare una sportiva"

## 🎯 Concept

L'app trasforma piccoli gesti quotidiani in risultati visibili con un approccio:
- ⏱️ **5–10 minuti al giorno**
- 🧘 **Esercizi dolci** (stretching, mobilità, camminata, yoga)
- 🥗 **Suggerimenti alimentari semplici**
- 💕 **Motivazione emotiva** (mai tecnica, mai giudicante)

## 🔑 Funzionalità

### 🆓 Gratuite
- **🌿 Routine giornaliera "zero sforzo"**: 1 mini allenamento + 1 micro-consiglio alimentare + 1 obiettivo semplice
- **💬 Coach motivazionale "Marta"**: chat empatica che incoraggia senza giudicare
- **📊 Progressi "soft"**: focus su emozioni (energia, sonno, umore), NO numeri ossessivi
- **🎯 Micro-obiettivi**: piccoli gesti fattibili con streak tracking
- **📰 News benessere**: articoli su alimentazione, movimento, mente
- **👯 Invita un'amica**: condividi il percorso

### 💎 Premium (€5–10/mese)
- Programmi personalizzati (menopausa, gonfiore, schiena, postura)
- Piani alimentari soft
- Video extra (yoga dolce, relax guidato)
- Reminder intelligenti
- Community privata

## 🛠️ Tech Stack

- **Linguaggio**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architettura**: Clean Architecture (Data → Domain → UI)
- **DI**: Hilt/Dagger
- **Database**: Room
- **Preferences**: DataStore
- **Background**: WorkManager
- **Navigation**: Navigation Compose
- **Fonts**: Google Fonts (Nunito + Poppins)

## 📁 Struttura Progetto

```
com.gentlefit.app/
├── data/           # Room entities, DAOs, repositories, DataStore
├── di/             # Hilt modules
├── domain/         # Models, repository interfaces, use cases
├── navigation/     # NavGraph
├── ui/
│   ├── components/ # UI reusabili (RoutineCard, CoachBubble, etc.)
│   ├── screen/     # Schermate (Home, Coach, Progress, Goals, News, etc.)
│   └── theme/      # Design system (colori, tipografia, forme)
└── worker/         # WorkManager workers
```

## 🎨 Design

- **Palette**: Rosa gentile, Verde salvia, Crema caldo
- **Font**: Nunito (headings) + Poppins (body)
- **Stile**: Morbido, arrotondato, con gradients delicati e emoji
- **Tono**: Amichevole, empatico, come un'amica

## 📄 License

MIT
