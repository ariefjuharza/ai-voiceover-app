package com.rpmstudio.texttospeech.interfaces

import com.appodeal.ads.NativeAd

sealed interface SavedListItem {
    fun getItemId(): Int

    data class SavedTextItemSaved(val filename: String, val text: String) : SavedListItem {
        override fun getItemId() = SAVED_ITEM

        companion object {
            const val SAVED_ITEM = 0
        }
    }

    class DynamicNativeAdItemSaved(val getNativeAd: () -> NativeAd?) : SavedListItem {
        override fun getItemId() = DYNAMIC_AD_ITEM

        companion object {
            const val DYNAMIC_AD_ITEM = 1
        }
    }
}