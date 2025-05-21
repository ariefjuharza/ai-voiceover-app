package com.rpmstudio.texttospeech.data

import android.content.Context
import androidx.core.content.edit

class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    fun isOnboardingShown(): Boolean {
        return prefs.getBoolean("onboarding_shown", false)
    }

    fun setOnboardingShown() {
        prefs.edit { putBoolean("onboarding_shown", true) }
    }

    fun isTutorialShown(): Boolean {
        return prefs.getBoolean("tutorial_shown", false)
    }

    fun setTutorialShown() {
        prefs.edit { putBoolean("tutorial_shown", true) }
    }
}