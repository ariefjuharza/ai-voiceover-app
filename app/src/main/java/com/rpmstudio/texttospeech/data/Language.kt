package com.rpmstudio.texttospeech.data

import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Language(
    val countryDisplayName: String,
    val languageDisplayName: String,
    val voiceAssistantLocale: String,
    val flagUrl: String,
    val voices: List<Voice>,
    private val _hasDefaultVoice: MutableStateFlow<Boolean> = MutableStateFlow(false)
) : Parcelable {

    val hasDefaultVoice = _hasDefaultVoice.asStateFlow()

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createTypedArrayList(Voice.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(countryDisplayName)
        parcel.writeString(languageDisplayName)
        parcel.writeString(voiceAssistantLocale)
        parcel.writeString(flagUrl)
        parcel.writeTypedList(voices)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Language> {
        override fun createFromParcel(parcel: Parcel): Language {
            return Language(parcel)
        }

        override fun newArray(size: Int): Array<Language?> {
            return arrayOfNulls(size)
        }
    }
}


