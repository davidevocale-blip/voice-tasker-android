package com.voicetasker.app.di

import android.content.Context
import androidx.room.Room
import com.voicetasker.app.data.local.VoiceTaskerDatabase
import com.voicetasker.app.data.local.MIGRATION_1_2
import com.voicetasker.app.data.local.MIGRATION_2_3
import com.voicetasker.app.data.local.dao.CategoryDao
import com.voicetasker.app.data.local.dao.NoteDao
import com.voicetasker.app.data.local.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VoiceTaskerDatabase =
        Room.databaseBuilder(context, VoiceTaskerDatabase::class.java, VoiceTaskerDatabase.DATABASE_NAME)
            .addCallback(VoiceTaskerDatabase.getCallback())
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun provideNoteDao(db: VoiceTaskerDatabase): NoteDao = db.noteDao()
    @Provides fun provideCategoryDao(db: VoiceTaskerDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideReminderDao(db: VoiceTaskerDatabase): ReminderDao = db.reminderDao()
}
