package com.rpmstudio.texttospeech.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

class LocaleContextWrapper(base: Context, private val locale: Locale) : ContextWrapper(base) {
    override fun getResources(): Resources {
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocale(locale)
        return createConfigurationContext(configuration).resources
    }
}