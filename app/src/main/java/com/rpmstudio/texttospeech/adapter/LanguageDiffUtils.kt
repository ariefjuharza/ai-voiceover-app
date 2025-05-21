package com.rpmstudio.texttospeech.adapter

import androidx.recyclerview.widget.DiffUtil
import com.rpmstudio.texttospeech.interfaces.LanguageListItem

internal class LanguageDiffUtils : DiffUtil.ItemCallback<LanguageListItem>() {
    override fun areItemsTheSame(oldItem: LanguageListItem, newItem: LanguageListItem): Boolean {
        return oldItem.getItemId() == newItem.getItemId()
    }

    override fun areContentsTheSame(oldItem: LanguageListItem, newItem: LanguageListItem): Boolean {
        return oldItem == newItem
    }
}