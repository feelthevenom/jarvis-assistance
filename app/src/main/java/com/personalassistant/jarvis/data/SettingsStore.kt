package com.personalassistant.jarvis.data

import android.content.Context
import com.personalassistant.jarvis.ui.theme.AppTheme

data class AppSettings(
    val theme: AppTheme = AppTheme.Light,
    val saveHistory: Boolean = true,
    val responseSounds: Boolean = true,
    val voiceAutoSpeak: Boolean = true,
    val historyReminders: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val storeAudio: Boolean = false,
    val cacheModelInMemory: Boolean = true,
)

/**
 * SharedPreferences-backed settings used by the active Settings tab.
 * Toggles map directly to behaviour: saveHistory disables ConversationStore
 * persistence, voiceAutoSpeak toggles TextToSpeech, etc.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        theme = runCatching { AppTheme.valueOf(prefs.getString(KEY_THEME, AppTheme.Light.name)!!) }
            .getOrDefault(AppTheme.Light),
        saveHistory = prefs.getBoolean(KEY_SAVE_HISTORY, true),
        responseSounds = prefs.getBoolean(KEY_RESPONSE_SOUNDS, true),
        voiceAutoSpeak = prefs.getBoolean(KEY_VOICE_AUTO_SPEAK, true),
        historyReminders = prefs.getBoolean(KEY_HISTORY_REMINDERS, false),
        analyticsEnabled = prefs.getBoolean(KEY_ANALYTICS, false),
        storeAudio = prefs.getBoolean(KEY_STORE_AUDIO, false),
        cacheModelInMemory = prefs.getBoolean(KEY_CACHE_MODEL, true),
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME, settings.theme.name)
            .putBoolean(KEY_SAVE_HISTORY, settings.saveHistory)
            .putBoolean(KEY_RESPONSE_SOUNDS, settings.responseSounds)
            .putBoolean(KEY_VOICE_AUTO_SPEAK, settings.voiceAutoSpeak)
            .putBoolean(KEY_HISTORY_REMINDERS, settings.historyReminders)
            .putBoolean(KEY_ANALYTICS, settings.analyticsEnabled)
            .putBoolean(KEY_STORE_AUDIO, settings.storeAudio)
            .putBoolean(KEY_CACHE_MODEL, settings.cacheModelInMemory)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "concierge_settings"
        private const val KEY_THEME = "theme"
        private const val KEY_SAVE_HISTORY = "save_history"
        private const val KEY_RESPONSE_SOUNDS = "response_sounds"
        private const val KEY_VOICE_AUTO_SPEAK = "voice_auto_speak"
        private const val KEY_HISTORY_REMINDERS = "history_reminders"
        private const val KEY_ANALYTICS = "analytics_enabled"
        private const val KEY_STORE_AUDIO = "store_audio"
        private const val KEY_CACHE_MODEL = "cache_model_in_memory"
    }
}
