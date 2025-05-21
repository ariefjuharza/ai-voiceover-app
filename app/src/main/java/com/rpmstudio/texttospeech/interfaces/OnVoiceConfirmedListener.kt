package com.rpmstudio.texttospeech.interfaces

import com.rpmstudio.texttospeech.data.Language
import com.rpmstudio.texttospeech.data.Voice

interface OnVoiceConfirmedListener {
    fun onVoiceConfirmed(language: Language, voice: Voice)
}