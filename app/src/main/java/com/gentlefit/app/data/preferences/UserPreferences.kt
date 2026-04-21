package com.gentlefit.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gentlefit_prefs")

class UserPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_GOAL = stringPreferencesKey("user_goal")
        val SHOW_WEIGHT = booleanPreferencesKey("show_weight")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HAS_COMPLETED_ONBOARDING] ?: false
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME] ?: ""
    }

    val userGoal: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_GOAL] ?: ""
    }

    val showWeight: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_WEIGHT] ?: false
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED] ?: true
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PREMIUM] ?: false
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs -> prefs[HAS_COMPLETED_ONBOARDING] = true }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs -> prefs[USER_NAME] = name }
    }

    suspend fun setUserGoal(goal: String) {
        context.dataStore.edit { prefs -> prefs[USER_GOAL] = goal }
    }

    suspend fun setShowWeight(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_WEIGHT] = show }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setPremium(premium: Boolean) {
        context.dataStore.edit { prefs -> prefs[IS_PREMIUM] = premium }
    }
}
