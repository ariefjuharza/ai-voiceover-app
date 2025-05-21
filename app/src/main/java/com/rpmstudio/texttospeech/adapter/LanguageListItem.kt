package com.rpmstudio.texttospeech.adapter

import com.appodeal.ads.NativeAd
import com.rpmstudio.texttospeech.data.Language

sealed interface LanguageListItem {
    fun getItemId(): Int

    data class LanguageItem(val language: Language, var isSelected: Boolean = false) :
        LanguageListItem {
        override fun getItemId() = language.hashCode()

        companion object {
            const val LANGUAGE_ITEM = 1
        }
    }

    class DynamicNativeAdLanguage(val getNativeAd: () -> NativeAd?) : LanguageListItem {
        override fun getItemId() = DYNAMIC_AD_ITEM

        companion object {
            const val DYNAMIC_AD_ITEM = 2
        }
    }
}