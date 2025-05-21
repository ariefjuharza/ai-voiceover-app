package com.rpmstudio.texttospeech.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.adapter.VoiceAdapter
import com.rpmstudio.texttospeech.data.Language
import com.rpmstudio.texttospeech.data.UserPreferences
import com.rpmstudio.texttospeech.data.Voice
import com.rpmstudio.texttospeech.data.getVoicesForLocale
import com.rpmstudio.texttospeech.databinding.FragmentVoiceListBinding
import com.rpmstudio.texttospeech.interfaces.OnVoiceSelectedListener
import com.rpmstudio.texttospeech.utils.showErrorToast
import java.util.Locale

class VoiceListFragment : BottomSheetDialogFragment(), OnVoiceSelectedListener {
    companion object {
        private const val TAG = "VoiceListFragment"
        const val KEY_LANGUAGE = "language"
        const val KEY_VOICE = "selectedVoice"
        const val KEY_VOICE_RESULT = "voiceResult"
        const val KEY_COUNTRY_DISPLAY_NAME = "countryDisplayName"
        const val KEY_LANGUAGE_DISPLAY_NAME = "languageDisplayName"
    }

    private var _binding: FragmentVoiceListBinding? = null
    private val binding get() = _binding!!

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var voiceAdapter: VoiceAdapter
    private lateinit var language: Language

    private var onVoiceItemClickListener: ((Voice) -> Unit)? = null

    fun setOnVoiceItemClickListener(listener: (Voice) -> Unit) {
        onVoiceItemClickListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        language = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arguments?.getParcelable(
                KEY_LANGUAGE, Language::class.java
            )

            else -> @Suppress("DEPRECATION") arguments?.getParcelable(KEY_LANGUAGE)
        } ?: return
        val countryDisplayName = arguments?.getString(KEY_COUNTRY_DISPLAY_NAME) ?: ""
        val languageDisplayName = arguments?.getString(KEY_LANGUAGE_DISPLAY_NAME) ?: ""

        binding.tvCountry.text = countryDisplayName
        binding.tvLocale.text = languageDisplayName

        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(
                    TAG, "Available voices: ${textToSpeech.voices.joinToString { it.name }}"
                )
                loadVoices()

            } else {
                requireContext().showErrorToast(
                    TAG, getString(R.string.texttospeech_initialization_failed)
                )
            }
        }

        binding.btnSelected.text = getString(R.string.select_voice_first)
        val defaultButtonColor = TypedValue()
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorAccent, defaultButtonColor, true
        )
        binding.btnSelected.backgroundTintList = ColorStateList.valueOf(defaultButtonColor.data)

        binding.btnSelected.setOnClickListener {
            val selectedPosition = voiceAdapter.selectedPosition

            // Access voices from the updated language object
            if (selectedPosition != -1 && selectedPosition in language.voices.indices) {
                val selectedVoice = language.voices[selectedPosition]

                setFragmentResult(KEY_VOICE_RESULT, Bundle().apply {
                    putParcelable(KEY_LANGUAGE, language)
                    putParcelable(KEY_VOICE, selectedVoice)
                })

                UserPreferences.saveSelectedLanguageAndVoice(
                    requireContext(), language, selectedVoice
                )

                val resultIntent = Intent().apply {
                    putExtra(KEY_LANGUAGE, language)
                    putExtra(KEY_VOICE, selectedVoice)
                }
                activity?.setResult(Activity.RESULT_OK, resultIntent)
                activity?.finish()

            } else {
                requireContext().showErrorToast(TAG, getString(R.string.please_select_a_voice))
            }
        }
    }

    private fun loadVoices() {
        Log.d(TAG, "loadVoices: Starting to load voices for ${language.voiceAssistantLocale}")
        val allVoices = getVoicesForLocale(textToSpeech, language.voiceAssistantLocale)
        Log.d(TAG, "loadVoices: allVoices size: ${allVoices.size}")
        allVoices.forEach {
            Log.d(
                TAG,
                "loadVoices: allVoices: ${it.name} - ${it.locale.language} - ${it.locale.country}"
            )
        }
        val languageLocale = Locale.forLanguageTag(language.voiceAssistantLocale)
        Log.d(TAG, "loadVoices: languageLocale: ${languageLocale.language}")

        val filteredVoices = allVoices.filter { voice ->
            val isSameLanguage = voice.locale.language == languageLocale.language
            Log.d(
                TAG,
                "loadVoices: voice: ${voice.name} - ${voice.locale.language} - isSameLanguage: $isSameLanguage"
            )
            isSameLanguage
        }

        // Create a new Language object with the updated voices
        language = language.copy(voices = filteredVoices)

        requireActivity().runOnUiThread {
            val displayNames = calculateDisplayNames(filteredVoices) // Use filteredVoices here

            voiceAdapter = VoiceAdapter(
                filteredVoices, this@VoiceListFragment, textToSpeech, displayNames
            )
            binding.voiceList.adapter = voiceAdapter
            binding.voiceList.layoutManager = LinearLayoutManager(requireContext())

            if (filteredVoices.isEmpty() && isAdded) { // Use filteredVoices here
                showInstallVoiceDataDialog()
            } else {
                // Set default voice (if needed)
                if (filteredVoices.isEmpty()) {
                    Log.w(
                        TAG,
                        "No voices found for ${language.voiceAssistantLocale}, using fallback voice"
                    )
                    val fallbackLocale = Locale.US
                    val fallbackVoices =
                        getVoicesForLocale(textToSpeech, fallbackLocale.toLanguageTag())
                    if (fallbackVoices.isNotEmpty()) {
                        language = language.copy(voices = fallbackVoices)
                        val fallbackDisplayNames = calculateDisplayNames(fallbackVoices)
                        voiceAdapter = VoiceAdapter(
                            fallbackVoices,
                            this@VoiceListFragment,
                            textToSpeech,
                            fallbackDisplayNames
                        )
                        binding.voiceList.adapter = voiceAdapter
                        binding.voiceList.layoutManager = LinearLayoutManager(requireContext())
                    } else {
                        Log.e(TAG, "No fallback voices found either!")
                    }
                }
            }
        }
    }

    private fun calculateDisplayNames(voices: List<Voice>): List<String> {
        val displayNames = mutableListOf<String>()

        val voicesByLanguageAndCountry = voices.groupBy {
            "${it.locale.displayLanguage}-${it.locale.country}"
        }

        voicesByLanguageAndCountry.forEach { (_, voicesForGroup) ->
            var count = 1
            voicesForGroup.forEach { voice ->
                val languageName = voice.locale.displayLanguage
                val displayName = "$languageName $count (${voice.name})"
                Log.d(TAG, "calculateDisplayNames:$displayName")
                displayNames.add(displayName)
                count++
            }
        }
        return displayNames
    }

    private fun showInstallVoiceDataDialog() {
        if (!isAdded) {
            Log.w(TAG, "showInstallVoiceDataDialog: Fragment is not attached, skipping")
            return
        }
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.install_voice_data_title))
        builder.setMessage(
            getString(
                R.string.no_voices_available_for_please_install_voice_data,
                language.countryDisplayName
            ) + "\n\n" + getString(R.string.install_voice_data_instructions)
        )

        builder.setPositiveButton(getString(R.string.install_voice_data)) { dialog, _ ->
            checkAndInstallVoiceData()
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun checkAndInstallVoiceData() {
        if (!isAdded) {
            Log.w(TAG, "checkAndInstallVoiceData: Fragment is not attached, skipping")
            return
        }
        Log.d(TAG, "checkAndInstallVoiceData: Starting check and install process")
        val defaultEngine = getDefaultTtsEngine()
        Log.d(TAG, "checkAndInstallVoiceData: defaultEngine: $defaultEngine")

        when (defaultEngine) {
            "com.google.android.tts" -> {
                // Google TTS is the default
                Log.d(TAG, "checkAndInstallVoiceData: Google TTS is the default")
                installGoogleTtsVoiceData()
            }

            else -> {
                // Another TTS engine is the default
                Log.d(TAG, "checkAndInstallVoiceData: Another TTS engine is the default")
                showGenericTtsSettings()
            }
        }
    }

    private fun getDefaultTtsEngine(): String {
        if (!isAdded) {
            Log.w(TAG, "getDefaultTtsEngine: Fragment is not attached, returning empty string")
            return ""
        }
        return textToSpeech.defaultEngine ?: ""
    }

    private fun installGoogleTtsVoiceData() {
        if (!isAdded) {
            Log.w(TAG, "installGoogleTtsVoiceData: Fragment is not attached, skipping")
            return
        }
        Log.d(TAG, "installGoogleTtsVoiceData: Starting install process")

        val locale = Locale.forLanguageTag(language.voiceAssistantLocale)
        val intent = Intent()
        intent.action = "com.android.settings.TTS_SETTINGS"
        intent.putExtra("locale", locale)
        intent.setPackage("com.google.android.tts")
        try {
            requireContext().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "installGoogleTtsVoiceData: Activity not found", e)
            showGenericTtsSettings()
        }
    }

    private fun showGenericTtsSettings() {
        if (!isAdded) {
            Log.w(TAG, "showGenericTtsSettings: Fragment is not attached, skipping")
            return
        }
        Log.d(TAG, "showGenericTtsSettings: Starting show generic settings process")

        //    val intent = Intent(Settings.ACTION_SETTINGS)
        val intent = Intent("com.android.settings.TTS_SETTINGS")
        try {
            requireContext().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "showGenericTtsSettings: Activity not found", e)
        }
    }

    override fun onVoiceSelected(voice: Voice) {
        val position = language.voices.indexOf(voice) // Use language.voices
        if (position != -1) {
            voiceAdapter.speakTrialPhraseWithVoice(position)
        }
        Log.d(TAG, "onVoiceSelected: $position")

        binding.btnSelected.text = getString(R.string.confirm)
        val selectedButtonColor = TypedValue()
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimaryDark, selectedButtonColor, true
        )
        binding.btnSelected.backgroundTintList = ColorStateList.valueOf(selectedButtonColor.data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        textToSpeech.shutdown()
    }
}

