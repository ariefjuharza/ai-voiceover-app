package com.rpmstudio.texttospeech.adapter

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.appodeal.ads.nativead.NativeAdView
import com.appodeal.ads.nativead.NativeAdViewAppWall
import com.appodeal.ads.nativead.NativeAdViewContentStream
import com.appodeal.ads.nativead.NativeAdViewNewsFeed
import com.rpmstudio.texttospeech.activity.MainActivity
import com.rpmstudio.texttospeech.activity.MainActivity.Companion.configureNativeAdView
import com.rpmstudio.texttospeech.interfaces.SavedListItem.DynamicNativeAdItemSaved.Companion.DYNAMIC_AD_ITEM
import com.rpmstudio.texttospeech.interfaces.SavedListItem.SavedTextItemSaved.Companion.SAVED_ITEM
import com.rpmstudio.texttospeech.databinding.ItemSavedBinding
import com.rpmstudio.texttospeech.databinding.NativeAdViewCustomBinding
import com.rpmstudio.texttospeech.fragment.DetailSavedFragment
import com.rpmstudio.texttospeech.interfaces.OnFileDeletedListener
import com.rpmstudio.texttospeech.interfaces.SavedListItem
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class SavedAdapter(
    private val fragmentManager: FragmentManager,
) : ListAdapter<SavedListItem, RecyclerView.ViewHolder>(SavedDiffUtils()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            DYNAMIC_AD_ITEM -> {
                val nativeAdView = createNativeAdView(parent)
                DynamicAdViewHolder(nativeAdView)
            }

            else -> {
                val binding =
                    ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SavedTextViewHolder(binding, this)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SavedListItem.SavedTextItemSaved -> (holder as SavedTextViewHolder).bind(item)

            is SavedListItem.DynamicNativeAdItemSaved -> (holder as DynamicAdViewHolder).bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SavedListItem.SavedTextItemSaved -> SAVED_ITEM
            is SavedListItem.DynamicNativeAdItemSaved -> DYNAMIC_AD_ITEM
        }
    }

    inner class SavedTextViewHolder(binding: ItemSavedBinding, private val adapter: SavedAdapter) :
        RecyclerView.ViewHolder(binding.root) {
        private val tvSavedText = binding.tvSavedText
        private val tvSavedDate = binding.tvSavedDate

        fun bind(item: SavedListItem.SavedTextItemSaved) {
            tvSavedText.text = item.text
            tvSavedDate.text = formatDate(item.filename)

            itemView.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("filename", item.filename)
                bundle.putString("text", item.text)

                val detailFragment = DetailSavedFragment().apply {
                    arguments = bundle
                    // Set the listener here!
                    setOnFileDeletedListener(object : OnFileDeletedListener {
                        override fun onFileDeleted(filename: String) {
                            // Update adapter data and notify
                            Handler(Looper.getMainLooper()).post {
                                val currentList = adapter.currentList.toMutableList()
                                val itemToRemove = currentList.find {
                                    if (it is SavedListItem.SavedTextItemSaved) it.filename == filename else false
                                }
                                if (itemToRemove != null) {
                                    currentList.remove(itemToRemove)
                                    adapter.submitList(currentList)
                                }
                            }
                            Log.d("SavedAdapter", "Listener set on DetailSavedFragment")
                        }
                    })
                }

                detailFragment.show(fragmentManager, DetailSavedFragment.TAG)
            }
        }
    }

    inner class DynamicAdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: SavedListItem.DynamicNativeAdItemSaved) {
            val nativeAd = item.getNativeAd.invoke() ?: return
            (itemView as NativeAdView).registerView(nativeAd)
        }
    }

    private fun createNativeAdView(parent: ViewGroup): NativeAdView {
        val context = parent.context
        val nativeAdView = when (MainActivity.nativeAdViewType) {
            NativeAdViewAppWall::class -> NativeAdViewAppWall(context)
            NativeAdViewNewsFeed::class -> NativeAdViewNewsFeed(context)
            NativeAdViewContentStream::class -> NativeAdViewContentStream(context)
            else -> NativeAdViewCustomBinding.inflate(
                LayoutInflater.from(context), parent, false
            ).root
        }
        configureNativeAdView(nativeAdView)
        return nativeAdView
    }

    private fun formatDate(filename: String): String {
        val inputDateFormat = SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault())

        return try {
            val date =
                inputDateFormat.parse(filename.substringAfter("TTS_").substringBeforeLast("_"))
            if (date != null) outputDateFormat.format(date) else "Invalid Date"
        } catch (e: ParseException) {
            Log.e("SavedAdapter", "Error parsing date from filename: $filename", e)
            "Invalid Date"
        }
    }
}