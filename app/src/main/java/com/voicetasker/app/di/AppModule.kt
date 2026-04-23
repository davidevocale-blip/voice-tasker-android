package com.voicetasker.app.di

import com.voicetasker.app.data.repository.CategoryRepositoryImpl
import com.voicetasker.app.data.repository.NoteRepositoryImpl
import com.voicetasker.app.data.repository.ReminderRepositoryImpl
import com.voicetasker.app.domain.repository.CategoryRepository
import com.voicetasker.app.domain.repository.NoteRepository
import com.voicetasker.app.domain.repository.ReminderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds @Singleton abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
    @Binds @Singleton abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
