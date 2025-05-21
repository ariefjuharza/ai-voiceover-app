package com.rpmstudio.texttospeech.data

import android.os.Parcel
import android.os.Parcelable
import java.util.Locale

data class Voice(
    val name: String, val id: String, val locale: Locale
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        Locale.forLanguageTag(parcel.readString() ?: "")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(id)
        parcel.writeString(locale.toLanguageTag())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Voice> {
        override fun createFromParcel(parcel: Parcel): Voice {
            return Voice(parcel)
        }

        override fun newArray(size: Int): Array<Voice?> {
            return arrayOfNulls(size)
        }
    }
}
