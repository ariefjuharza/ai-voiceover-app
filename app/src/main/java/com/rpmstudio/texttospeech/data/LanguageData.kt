package com.rpmstudio.texttospeech.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.rpmstudio.texttospeech.adapter.MutableStateFlowTypeAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

object LanguageData {
    private lateinit var contextProvider: () -> Context
    private var ttsInitialized = false
    private val availableLanguages = mutableListOf<Language>()
    private lateinit var tts: TextToSpeech

    private val _languageDataState = MutableStateFlow<UiState<List<Language>>>(UiState.Loading)
    val languageDataState: StateFlow<UiState<List<Language>>> = _languageDataState.asStateFlow()

    fun applyUserPreferences() {
        if (ttsInitialized) {
            val context = contextProvider.invoke()
            val (selectedLanguage, selectedVoiceId) = UserPreferences.getSelectedLanguageAndVoice(
                context
            )

            if (selectedLanguage != null && selectedVoiceId != null) {
                val selectedVoice = tts.voices.find { it.name == selectedVoiceId }
                if (selectedVoice != null) {
                    tts.language = Locale.forLanguageTag(selectedLanguage)
                    tts.voice = selectedVoice
                }
            }
        }
    }

    fun initialize(contextProvider: () -> Context) {
        this.contextProvider = contextProvider
        val context = contextProvider.invoke()

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _languageDataState.value = UiState.Loading
                CoroutineScope(Dispatchers.IO).launch {
                    ttsInitialized = true
                    loadAvailableLanguages()
                    _languageDataState.value =
                        UiState.Success(availableLanguages) // Emit Success with language data
                }
            } else {
                _languageDataState.value = UiState.Error(Exception("TTS initialization failed"))
                Log.e("LanguageData", "TTS initialization failed")
            }
        }
    }

    val languages: List<Language>
        get() = availableLanguages

    fun getFlagUrlForLocale(locale: Locale): String {
        val countryCode = locale.country.lowercase()
        return "https://flagcdn.com/w320/$countryCode.png"
    }

    private fun loadAvailableLanguages() {
        val prefs =
            contextProvider.invoke().getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val cachedLanguagesJson = prefs.getString("languages", null)

        val gson = GsonBuilder().registerTypeAdapter(
                MutableStateFlow::class.java, MutableStateFlowTypeAdapter(Boolean::class.java)
            ) // Register the adapter for MutableStateFlow<Boolean>
            .create()

        if (cachedLanguagesJson != null) {
            // Deserialize cachedLanguagesJson to List<Language> (use Gson or other library)
            val languageType = object : TypeToken<List<Language>>() {}.type
            val deserializedLanguages: List<Language> =
                gson.fromJson(cachedLanguagesJson, languageType)
            availableLanguages.addAll(deserializedLanguages)
        } else {
            if (ttsInitialized) {
                val availableTtsLocales = getAvailableTtsLocales(tts)
                availableLanguages.clear()
                availableLanguages.addAll(availableTtsLocales.mapNotNull { locale ->
                    val displayName = locale.displayCountry
                    val languageName = locale.displayLanguage
                    if (displayName.isNotBlank() && languageName.isNotBlank()) {
                        val voicesForLocale = getVoicesForLocale(tts, locale.toLanguageTag())

                        Language(
                            displayName,
                            languageName,
                            locale.toLanguageTag(),
                            flagUrl = getFlagUrlForLocale(locale),
                            voices = voicesForLocale
                        )
                    } else null
                }.sortedBy { it.countryDisplayName })
            }

            // Serialize availableLanguages to JSON string (use Gson or other library)
            if (availableLanguages.isNotEmpty()) {
                val languagesJson: String = gson.toJson(availableLanguages)
                with(prefs.edit()) {
                    putString("languages", languagesJson)
                    apply()
                }
            }
        }
    }

    private fun getAvailableTtsLocales(tts: TextToSpeech): Set<Locale> {
        return tts.voices.map { it.locale }.toSet()
    }
}

