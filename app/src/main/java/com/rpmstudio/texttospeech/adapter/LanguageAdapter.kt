package com.rpmstudio.texttospeech.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.appodeal.ads.nativead.NativeAdView
import com.appodeal.ads.nativead.NativeAdViewAppWall
import com.appodeal.ads.nativead.NativeAdViewContentStream
import com.appodeal.ads.nativead.NativeAdViewNewsFeed
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.activity.MainActivity
import com.rpmstudio.texttospeech.activity.MainActivity.Companion.configureNativeAdView
import com.rpmstudio.texttospeech.adapter.LanguageAdapter.ListHolder.DynamicAdViewHolder
import com.rpmstudio.texttospeech.adapter.LanguageAdapter.ListHolder.LanguageViewHolder
import com.rpmstudio.texttospeech.interfaces.LanguageListItem.DynamicNativeAdLanguage.Companion.DYNAMIC_AD_ITEM
import com.rpmstudio.texttospeech.interfaces.LanguageListItem.LanguageItem.Companion.LANGUAGE_ITEM
import com.rpmstudio.texttospeech.data.Language
import com.rpmstudio.texttospeech.databinding.ItemLanguageBinding
import com.rpmstudio.texttospeech.databinding.NativeAdViewCustomBinding
import com.rpmstudio.texttospeech.interfaces.LanguageListItem
import com.rpmstudio.texttospeech.interfaces.OnLanguageClickListener
import kotlinx.coroutines.launch

class LanguageAdapter(
    private val listener: OnLanguageClickListener, private val lifecycleOwner: LifecycleOwner
) : ListAdapter<LanguageListItem, LanguageAdapter.ListHolder>(LanguageDiffUtils()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListHolder {
        return when (viewType) {
            DYNAMIC_AD_ITEM.hashCode() -> {
                val nativeAdView = createNativeAdView(parent)
                DynamicAdViewHolder(nativeAdView)
            }

            else -> {
                val binding =
                    ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LanguageViewHolder(binding, lifecycleOwner, listener, this)
            }
        }
    }

    override fun onBindViewHolder(holder: ListHolder, position: Int) {
        when (val item = getItem(position)) {
            is LanguageListItem.LanguageItem -> (holder as LanguageViewHolder).bind(
                item.language, item.isSelected
            )

            is LanguageListItem.DynamicNativeAdLanguage -> {
                (holder as DynamicAdViewHolder).bind(item)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is LanguageListItem.LanguageItem -> LANGUAGE_ITEM
            is LanguageListItem.DynamicNativeAdLanguage -> DYNAMIC_AD_ITEM
        }
    }

    sealed class ListHolder(root: View) : RecyclerView.ViewHolder(root) {
        class LanguageViewHolder(
            binding: ItemLanguageBinding,
            private val lifecycleOwner: LifecycleOwner,
            private val listener: OnLanguageClickListener,
            private val adapter: LanguageAdapter
        ) : ListHolder(binding.root) {
            private val flagImageView = binding.ivFlag
            private val languageTextView = binding.tvLanguage
            private val languageDetailTextView = binding.tvLanguageDetail
            private val defaultVoiceIndicator = binding.ivDefault
            private val itemLanguageBlock = binding.itemLanguageBlock

            fun bind(language: Language, isSelected: Boolean) {
                flagImageView.load(language.flagUrl) {
                    placeholder(R.drawable.flag_2_svgrepo_com)
                    error(R.drawable.danger_triangle_svgrepo_com)
                }
                languageTextView.text = language.countryDisplayName
                languageDetailTextView.text = language.languageDisplayName

                if (isSelected) {
                    defaultVoiceIndicator.visibility = View.VISIBLE
                    itemLanguageBlock.background = AppCompatResources.getDrawable(
                        itemLanguageBlock.context, R.drawable.selected_item_background
                    )
                } else {
                    defaultVoiceIndicator.visibility = View.GONE
                    itemLanguageBlock.background = null
                }

                lifecycleOwner.lifecycleScope.launch {
                    language.hasDefaultVoice.collect { hasDefault ->
                        Log.d(
                            "LanguageAdapter", "bind: ${language.countryDisplayName} - $hasDefault"
                        )
                    }
                }
            }

            init {
                itemView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = adapter.getItem(position)
                        if (item is LanguageListItem.LanguageItem) {
                            listener.onLanguageClick(item.language)
                        }
                    }
                }
            }
        }

        class DynamicAdViewHolder(itemView: View) : ListHolder(itemView) {
            fun bind(item: LanguageListItem.DynamicNativeAdLanguage) {
                val nativeAd = item.getNativeAd.invoke() ?: return
                (itemView as NativeAdView).registerView(nativeAd)
            }
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
}

