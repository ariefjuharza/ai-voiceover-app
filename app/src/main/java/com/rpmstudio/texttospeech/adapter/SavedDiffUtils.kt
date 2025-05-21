package com.rpmstudio.texttospeech.adapter

import androidx.recyclerview.widget.DiffUtil
import com.rpmstudio.texttospeech.interfaces.SavedListItem

internal class SavedDiffUtils : DiffUtil.ItemCallback<SavedListItem>() {
    override fun areItemsTheSame(oldItem: SavedListItem, newItem: SavedListItem): Boolean {
        return oldItem.getItemId() == newItem.getItemId()
    }

    override fun areContentsTheSame(oldItem: SavedListItem, newItem: SavedListItem): Boolean {
        return oldItem == newItem
    }
}