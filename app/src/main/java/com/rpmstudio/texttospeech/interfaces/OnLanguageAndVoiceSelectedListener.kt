package com.rpmstudio.texttospeech.interfaces

import com.rpmstudio.texttospeech.data.Language
import com.rpmstudio.texttospeech.data.Voice

interface OnLanguageAndVoiceSelectedListener {
    fun onLanguageAndVoiceSelected(language: Language, voice: Voice)
}