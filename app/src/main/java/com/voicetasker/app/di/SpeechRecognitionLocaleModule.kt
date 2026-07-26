package com.voicetasker.app.di

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.voicetasker.app.data.recorder.SpeechRecognitionApplicationLocalesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCompatSpeechRecognitionApplicationLocalesProvider @Inject constructor() :
    SpeechRecognitionApplicationLocalesProvider {

    override fun getApplicationLocales(): LocaleListCompat =
        AppCompatDelegate.getApplicationLocales()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeechRecognitionLocaleModule {

    @Binds
    @Singleton
    abstract fun bindSpeechRecognitionApplicationLocalesProvider(
        implementation: AppCompatSpeechRecognitionApplicationLocalesProvider
    ): SpeechRecognitionApplicationLocalesProvider
}
