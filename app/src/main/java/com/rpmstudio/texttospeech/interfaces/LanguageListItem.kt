package com.rpmstudio.texttospeech.interfaces

import com.appodeal.ads.NativeAd
import com.rpmstudio.texttospeech.data.Language

sealed interface LanguageListItem {
    fun getItemId(): Int

    data class LanguageItem(val language: Language, var isSelected: Boolean = false) :
        LanguageListItem {
        override fun getItemId() = LANGUAGE_ITEM

        companion object {
            const val LANGUAGE_ITEM = 0
        }
    }

    class DynamicNativeAdLanguage(val getNativeAd: () -> NativeAd?) : LanguageListItem {
        override fun getItemId() = DYNAMIC_AD_ITEM

        companion object {
            const val DYNAMIC_AD_ITEM = 1
        }
    }
}