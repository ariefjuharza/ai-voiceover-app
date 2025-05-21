package com.rpmstudio.texttospeech.data

import android.speech.tts.TextToSpeech
import java.util.Locale

fun getVoicesForLocale(tts: TextToSpeech, locale: String): List<Voice> {
    val localeObject = Locale.forLanguageTag(locale)
    //    Log.d("Language", "All available voices: ${tts.voices}")
    val voices = tts.voices?.filter {
        it.locale.language.uppercase() == localeObject.language.uppercase() && it.locale.country.uppercase() == localeObject.country.uppercase()
    }?.map {
        Voice(it.name, it.name, it.locale)
    } ?: emptyList() // Return an empty list if tts.voices is null
    //   Log.d("Language", "Filtered voices for $locale: $voices")
    return voices
}