package com.rpmstudio.texttospeech.adapter

import com.appodeal.ads.NativeAd

sealed interface SavedListItem {
    fun getItemId(): Int

    data class SavedTextItemSaved(val filename: String, val text: String) : SavedListItem {
        override fun getItemId() = filename.hashCode()

        companion object {
            const val USER_ITEM = 2
        }
    }

    class DynamicNativeAdItemSaved(val getNativeAd: () -> NativeAd?) : SavedListItem {
        override fun getItemId() = DYNAMIC_AD_ITEM

        companion object {
            const val DYNAMIC_AD_ITEM = 3
        }
    }
}