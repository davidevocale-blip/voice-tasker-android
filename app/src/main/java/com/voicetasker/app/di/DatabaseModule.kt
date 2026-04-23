package com.voicetasker.app.di

import android.content.Context
import androidx.room.Room
import com.voicetasker.app.data.local.VoiceTaskerDatabase
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideNoteDao(db: VoiceTaskerDatabase): NoteDao = db.noteDao()
    @Provides fun provideCategoryDao(db: VoiceTaskerDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideReminderDao(db: VoiceTaskerDatabase): ReminderDao = db.reminderDao()
}
