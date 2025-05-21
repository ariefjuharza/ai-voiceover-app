package com.rpmstudio.texttospeech.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.appodeal.ads.Appodeal
import com.appodeal.ads.NativeAd
import com.rpmstudio.texttospeech.adapter.LanguageAdapter
import com.rpmstudio.texttospeech.interfaces.LanguageListItem
import com.rpmstudio.texttospeech.data.Language
import com.rpmstudio.texttospeech.data.LanguageData
import com.rpmstudio.texttospeech.data.UiState
import com.rpmstudio.texttospeech.data.Voice
import com.rpmstudio.texttospeech.databinding.FragmentLanguageListBinding
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_COUNTRY_DISPLAY_NAME
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_LANGUAGE
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_LANGUAGE_DISPLAY_NAME
import com.rpmstudio.texttospeech.interfaces.OnLanguageAndVoiceSelectedListener
import com.rpmstudio.texttospeech.interfaces.OnLanguageClickListener
import kotlinx.coroutines.launch

class LanguageListFragment : Fragment(), OnLanguageClickListener {
    companion object {
        private const val TAG = "LanguageListFragment"

        private const val STEPS = 6

        fun newInstance(selectedTtsLanguage: String): LanguageListFragment {
            val fragment = LanguageListFragment()
            val args = Bundle()
            args.putString("selectedTtsLanguage", selectedTtsLanguage)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentLanguageListBinding? = null
    private val binding get() = _binding!!

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var languageAdapter: LanguageAdapter

    private var selectedLanguageCode: String? = null

    private var listener: OnLanguageAndVoiceSelectedListener? = null

    private val getNativeAd: () -> NativeAd? = { Appodeal.getNativeAds(1).firstOrNull() }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLanguageAndVoiceSelectedListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnLanguageAndVoiceSelectedListener")
        }
    }

    override fun onLanguageClick(language: Language) {
        val voiceListFragment = VoiceListFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_LANGUAGE, language)
                putString(KEY_COUNTRY_DISPLAY_NAME, language.countryDisplayName)
                putString(KEY_LANGUAGE_DISPLAY_NAME, language.languageDisplayName)
            }
            setOnVoiceItemClickListener { selectedVoice ->
                onVoiceItemClicked(language, selectedVoice)
            }
        }
        voiceListFragment.show(childFragmentManager, "voice_list_dialog")

    }

    private fun onVoiceItemClicked(language: Language, voice: Voice) {
        listener?.onLanguageAndVoiceSelected(language, voice)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedLanguageCode = it.getString("selectedTtsLanguage")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageListBinding.inflate(inflater, container, false)

        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.loadingTextView.visibility = View.VISIBLE

        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupLanguageAdapter()
            } else {
                Log.e(TAG, "TextToSpeech initialization failed: $status")
                showErrorDialog(
                    "TTS Initialization Failed", "TTS functionality might be limited."
                )
            }
        }
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupLanguageAdapter() {
        val selectedTtsLanguage = arguments?.getString("selectedTtsLanguage") ?: "en"

        languageAdapter = LanguageAdapter(this, viewLifecycleOwner)

        binding.list.adapter = languageAdapter
        binding.list.layoutManager = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> GridLayoutManager(requireContext(), 2)
            else -> LinearLayoutManager(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            LanguageData.languageDataState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.loadingTextView.visibility = View.VISIBLE
                    }

                    is UiState.Success -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.loadingTextView.visibility = View.GONE

                        // Update adapter items
                        val items =
                            state.data.foldIndexed(mutableListOf<LanguageListItem>()) { index, acc, language ->
                                acc.apply {
                                    add(
                                        LanguageListItem.LanguageItem(
                                            language,
                                            language.voiceAssistantLocale == selectedTtsLanguage
                                        )
                                    )
                                    if (index % STEPS == 0 && index != 0) {
                                        add(createAdItem())
                                    }
                                }
                            }
                        languageAdapter.submitList(items)

                        // Scroll to selected language (adjusting for ad items)
                        val selectedPosition = items.indexOfFirst {
                            it is LanguageListItem.LanguageItem && it.language.voiceAssistantLocale == selectedLanguageCode
                        }
                        if (selectedPosition != -1) {
                            // Adjust selectedPosition for ad items
                            val adjustedPosition =
                                selectedPosition - (selectedPosition / (STEPS + 1))
                            binding.list.scrollToPosition(adjustedPosition)
                        }
                    }

                    is UiState.Error -> {
                        // ... (Error handling) ...
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.loadingTextView.visibility = View.GONE
                        // Display error message
                        showErrorDialog(
                            "TTS Initialization Failed", "TTS functionality might be limited."
                        )
                    }
                }
            }
        }
    }

    private fun createAdItem(): LanguageListItem.DynamicNativeAdLanguage =
        LanguageListItem.DynamicNativeAdLanguage(getNativeAd = getNativeAd)


    private fun showErrorDialog(title: String, message: String, onRetry: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
            dialog.dismiss()
            onRetry?.invoke() // Call onRetry if provided
        }
        builder.setCancelable(false) // Prevent dismissing by clicking outside
        builder.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech.shutdown()
        _binding = null
    }
}
