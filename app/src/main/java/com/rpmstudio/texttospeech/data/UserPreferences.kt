package com.rpmstudio.texttospeech.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

object UserPreferences {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"
    private const val KEY_SELECTED_VOICE = "selected_voice"
    private const val KEY_SELECTED_FLAG_URL = "selected_flag_url"

    private const val KEY_PITCH = "pitch"
    private const val KEY_SPEED = "speed"

    private const val KEY_FILE_COUNTER = "counter"

    private const val KEY_IS_PREMIUM_USER = "is_premium_user"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSelectedLanguageAndVoice(context: Context, language: Language, voice: Voice) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString(KEY_SELECTED_LANGUAGE, language.voiceAssistantLocale)
            putString(KEY_SELECTED_VOICE, voice.id)
            putString(KEY_SELECTED_FLAG_URL, language.flagUrl)
            apply()
        }
    }

    fun getSelectedLanguageAndVoice(context: Context): Triple<String?, String?, String?> { // Return Triple
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Triple(
            prefs.getString(KEY_SELECTED_LANGUAGE, null),
            prefs.getString(KEY_SELECTED_VOICE, null),
            prefs.getString(KEY_SELECTED_FLAG_URL, null) // Add this line
        )
    }

    fun getFlagUrlForLanguage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_FLAG_URL, null)
    }

    fun savePitch(context: Context, pitch: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putFloat(KEY_PITCH, pitch)
            apply()
        }
        Log.d("UserPreferences", "Saving pitch: $pitch")
    }

    fun saveSpeed(context: Context, speed: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putFloat(KEY_SPEED, speed)
            apply()
        }
        Log.d("UserPreferences", "Saving speed: $speed")
    }

    fun getPitch(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultPitch = 1.0f
        return prefs.getFloat(KEY_PITCH, defaultPitch)
    }

    fun getSpeed(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultSpeed = 1.0f
        return prefs.getFloat(KEY_SPEED, defaultSpeed)
    }

    fun getFileCounter(): Int {
        return prefs.getInt(KEY_FILE_COUNTER, 0)
    }

    fun incrementAndSaveFileCounter() {
        val currentCounter = getFileCounter()
        with(prefs.edit()) {
            putInt(KEY_FILE_COUNTER, currentCounter + 1)
            apply()
        }
    }

    fun isPremiumUser(): Boolean {
        return prefs.getBoolean(KEY_IS_PREMIUM_USER, true)
    }

    fun setPremiumUser(isPremium: Boolean) {
        prefs.edit { putBoolean(KEY_IS_PREMIUM_USER, isPremium) }
    }
}

