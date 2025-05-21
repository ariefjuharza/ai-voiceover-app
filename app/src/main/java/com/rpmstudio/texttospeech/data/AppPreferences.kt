package com.rpmstudio.texttospeech.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Locale
import androidx.core.content.edit

object AppPreferences {
    private const val PREFS_NAME = "app_prefs"
    const val KEY_APP_LANGUAGE = "app_language"

    const val IS_ENABLE_CHECK_UPDATE = "is_enable_check_update"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (isFirstRun()) {
            val deviceLanguage = Locale.getDefault().language
            setAppLanguage(deviceLanguage)
        }
    }

    fun getAppLanguage(): String? {
        val language = prefs.getString(KEY_APP_LANGUAGE, null)
        Log.d("AppPreferences", "Getting app language: $language")
        return language
    }

    fun setAppLanguage(languageCode: String) {
        Log.d("AppPreferences", "Setting app language: $languageCode")
        prefs.edit { putString(KEY_APP_LANGUAGE, languageCode) }
    }

    private fun isFirstRun(): Boolean {
        return prefs.getString(KEY_APP_LANGUAGE, null) == null
    }
}