package com.rpmstudio.texttospeech.interfaces

import com.rpmstudio.texttospeech.data.Voice

interface OnVoiceSelectedListener {
    fun onVoiceSelected(voice: Voice)
}