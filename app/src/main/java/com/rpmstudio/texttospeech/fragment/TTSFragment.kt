package com.rpmstudio.texttospeech.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.appodeal.ads.Appodeal.BANNER
import com.appodeal.ads.Appodeal.BANNER_VIEW
import com.appodeal.ads.Appodeal.INTERSTITIAL
import com.appodeal.ads.Appodeal.canShow
import com.appodeal.ads.Appodeal.setBannerCallbacks
import com.appodeal.ads.Appodeal.setBannerViewId
import com.appodeal.ads.Appodeal.setInterstitialCallbacks
import com.appodeal.ads.Appodeal.show
import com.appodeal.ads.BannerCallbacks
import com.appodeal.ads.InterstitialCallbacks
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.activity.LanguageListActivity
import com.rpmstudio.texttospeech.data.BannerAppInfo
import com.rpmstudio.texttospeech.data.Language
import com.rpmstudio.texttospeech.data.LanguageData
import com.rpmstudio.texttospeech.data.OnboardingPrefs
import com.rpmstudio.texttospeech.data.UiState
import com.rpmstudio.texttospeech.data.UserPreferences
import com.rpmstudio.texttospeech.data.Voice
import com.rpmstudio.texttospeech.databinding.FragmentTTSBinding
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_LANGUAGE
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_VOICE
import com.rpmstudio.texttospeech.interfaces.OnVoiceConfirmedListener
import com.rpmstudio.texttospeech.utils.AppStoreHelper
import com.rpmstudio.texttospeech.utils.showErrorToast
import com.rpmstudio.texttospeech.utils.showSuccessToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.Unregistrar
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class TTSFragment : Fragment(), OnVoiceConfirmedListener {
    companion object {
        private const val TAG = "TTSFragment"
        private const val MAX_WORDS_NON_PREMIUM = 100
    }

    private var _binding: FragmentTTSBinding? = null
    private val binding get() = _binding!!

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var clipboardManager: ClipboardManager

    private var selectedLanguage: Language? = null
    private var selectedVoice: android.speech.tts.Voice? = null

    private var isTextToSpeechInitialized = false
    private var pendingLanguage: Language? = null
    private var pendingVoice: Voice? = null

    private var savedPitch = 1.0f
    private var savedSpeed = 1.0f

    private var keyboardVisibilityEventUnregistrar: Unregistrar? = null

    private var tapTargetSequence: TapTargetSequence? = null

    private val interstitialPlacement = "interstitialPlacement"
    private var bannerPlacement = "bannerPlacement"

    private lateinit var customBannerContainer: View
    private lateinit var customBannerIcon: ImageView
    private lateinit var customBannerTitle: TextView
    private lateinit var customBannerRatingBar: RatingBar
    private lateinit var customBannerDescription: TextView
    private lateinit var customBannerButton: Button

    private val appList = listOf(
        BannerAppInfo(
            R.drawable.power_life_icon,
            "PowerLife - Battery Monitor",
            4.5f,
            "Free",
            "com.bonodigitalstudio.batterymonitorapp"
        ),
        BannerAppInfo(
            R.drawable.univpn_icon,
            "UniVPN - Fast and Secure VPN",
            5f,
            "Free",
            "com.spiritmandiri.carspuzzle"
        ),
        BannerAppInfo(
            R.drawable.word_search_icon,
            "Word Master: A Fun Word Puzzle",
            4.2f,
            "Free",
            "com.bonodigitalstudio.wordsearchinnovation"
        ),
        BannerAppInfo(
            R.drawable.audio_recorder_icon,
            "Advanced Audio Recorder",
            3.8f,
            "Free",
            "com.rpmstudio.audiorecorder"
        ),
        // Add more apps here...
    )

    private var currentAppIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private val bannerRotationRunnable = object : Runnable {
        override fun run() {
            rotateBanner()
            handler.postDelayed(this, 10000) // 5 seconds delay
        }
    }

    private val primaryColor: Int by lazy {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, typedValue, true
        )
        typedValue.data
    }

    override fun onVoiceConfirmed(language: Language, voice: Voice) {
        selectedLanguage = language
        val selectedVoiceObject = textToSpeech.voices.find { it.name == voice.id }
        selectedVoiceObject?.let {
            selectedVoice = it
        }
        updateTTSWithSelectedVoice(language, voice)
    }

    private fun updateTTSWithSelectedVoice(language: Language, voice: Voice) {
        textToSpeech.language = Locale.forLanguageTag(language.voiceAssistantLocale)

        val selectedVoiceObject = textToSpeech.voices.find { it.name == voice.id }
        selectedVoiceObject?.let {
            textToSpeech.voice = it
        }

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) {}

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                Log.e(TAG, "Error setting voice")
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTTSBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "onViewCreated: TextToSpeech engine is initialized successfully")
                isTextToSpeechInitialized = true
                initializeTTS()
            } else {
                requireContext().showErrorToast(
                    TAG, getString(R.string.failed_to_initialize_text_to_speech_engine)
                )
                // Consider providing guidance on how to resolve the issue (e.g., install TTS data)
            }
        }
        Log.d(TAG, "Available voices: ${textToSpeech.voices}")

        binding.controlBlock.setOnClickListener {
            toggleControlBlockVisibility()
        }

        keyboardVisibilityEventUnregistrar = KeyboardVisibilityEvent.registerEventListener(
            requireActivity()
        ) { isOpen ->
            if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                handleKeyboardVisibility(isOpen)
            }
        }

        loadAndApplyPitchAndSpeed()

        setupSeekBar(binding.pitchSeekBar, binding.tvPitchValue, savedPitch, onProgressChanged = {
            textToSpeech.setPitch(it)
        }, onSave = {
            UserPreferences.savePitch(requireContext(), it)
        })

        setupSeekBar(binding.speedSeekBar, binding.tvSpeedValue, savedSpeed, onProgressChanged = {
            textToSpeech.setSpeechRate(it)
        }, onSave = {
            UserPreferences.saveSpeed(requireContext(), it)
        })

        binding.tvPitchReset.setOnClickListener {
            resetAndSavePitch()
        }

        binding.tvSpeedReset.setOnClickListener {
            resetAndSaveSpeed()
        }

        binding.languageSelectionContainer.setOnClickListener {
            showLanguageListFragment()
        }

        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateButtonStates(s.toString())
                updateTextLimitIndicator()
            }
        })
        applyWordLimit()
        updateTextLimitIndicator()

        setupClipboardFunctionality()

        binding.speakBlock.setOnClickListener {
            speakButtonClicked()
        }

        binding.saveBlock.setOnClickListener {
            saveButtonClicked()
        }

        showTutorial()

        // Shuffle the appList to display banners randomly
        Collections.shuffle(appList)
        currentAppIndex = 0 // Reset the index after shuffling

        // Initialize custom banner views
        customBannerContainer = binding.customBanner.root
        customBannerIcon = binding.customBanner.customBannerIcon
        customBannerTitle = binding.customBanner.customBannerTitle
        customBannerRatingBar = binding.customBanner.ratingBar
        customBannerDescription = binding.customBanner.customBannerDescription
        customBannerButton = binding.customBanner.customBannerButton

        // Set up click listener for the custom banner button
        customBannerButton.setOnClickListener {
            openAppInStore()
        }

        showBanner()
    }

    private fun initializeTTS() {
        if (pendingLanguage != null && pendingVoice != null) {
            handleLanguageAndVoiceResult(pendingLanguage!!, pendingVoice!!)
            pendingLanguage = null
            pendingVoice = null
        } else if (UserPreferences.getSelectedLanguageAndVoice(requireContext()).first == null) {
            initializeWithDefaults()
        }
        loadSavedPreferences()

        lifecycleScope.launch {
            LanguageData.languageDataState.collect { state ->
                if (state is UiState.Success) {
                    delay(100)
                    LanguageData.applyUserPreferences()
                } else if (state is UiState.Error) {
                    requireContext().showErrorToast(
                        TAG, getString(R.string.failed_to_load_languages)
                    )
                }
            }
        }
    }

    private fun handleLanguageAndVoiceResult(selectedLanguage: Language, selectedVoice: Voice) {
        val ttsVoice = textToSpeech.voices.find { it.name == selectedVoice.id }
        ttsVoice?.let { voice ->
            updateUiWithLanguageAndVoice(selectedLanguage, voice)
            updateTTSWithSelectedVoice(selectedLanguage, selectedVoice)
            UserPreferences.saveSelectedLanguageAndVoice(
                requireContext(), selectedLanguage, selectedVoice
            )
        }
    }

    private fun initializeWithDefaults() {
        val defaultLocale = textToSpeech.voice.locale
        val defaultLanguage = defaultLocale.getDisplayLanguage(defaultLocale)
        val defaultVoice = textToSpeech.voices.find { it.name == textToSpeech.voice.name }

        Log.d(TAG, "Default Country Code: ${defaultLocale.country}")

        defaultVoice?.let {
            val tempDefaultLanguage = Language(
                countryDisplayName = defaultLanguage,
                languageDisplayName = defaultLanguage,
                voiceAssistantLocale = defaultLocale.toLanguageTag(),
                flagUrl = LanguageData.getFlagUrlForLocale(defaultLocale),
                voices = listOf(Voice(it.name, it.name, it.locale))
            )
            updateUiWithLanguageAndVoice(tempDefaultLanguage, it)

            val displayName = calculateDisplayNameForVoice(it)
            binding.tvLanguageDetail.text = displayName

            binding.ivFlag.post {
                binding.ivFlag.load(tempDefaultLanguage.flagUrl) {
                    placeholder(R.drawable.flag_2_svgrepo_com)
                    error(R.drawable.danger_triangle_svgrepo_com)
                }
            }
        }
    }

    private fun loadSavedPreferences() {
        val (selectedLanguageTag, selectedVoiceId, selectedFlagUrl) = UserPreferences.getSelectedLanguageAndVoice(
            requireContext()
        )

        if (selectedLanguageTag != null && selectedVoiceId != null && selectedFlagUrl != null) {
            val selectedLocale = Locale.forLanguageTag(selectedLanguageTag)
            val selectedVoice = textToSpeech.voices.find { it.name == selectedVoiceId }

            binding.tvLanguage.text = selectedLocale.getDisplayLanguage(selectedLocale)
            selectedVoice?.let {
                val displayName = calculateDisplayNameForVoice(it)
                binding.tvLanguageDetail.text = displayName

                textToSpeech.voice = it

                binding.ivFlag.load(selectedFlagUrl) {
                    placeholder(R.drawable.flag_2_svgrepo_com)
                    error(R.drawable.danger_triangle_svgrepo_com)
                }
            }
        }
    }

    private fun loadAndApplyPitchAndSpeed() {
        val loadedPitch = UserPreferences.getPitch(requireContext())
        val loadedSpeed = UserPreferences.getSpeed(requireContext())
        savedPitch = loadedPitch
        savedSpeed = loadedSpeed

        Log.d(TAG, "Loaded pitch: $loadedPitch, speed: $loadedSpeed")
        binding.pitchSeekBar.progress = (savedPitch * 100).toInt()
        binding.tvPitchValue.text = String.format(Locale.getDefault(), "%.1f", savedPitch)
        textToSpeech.setPitch(savedPitch)

        binding.speedSeekBar.progress = (savedSpeed * 100).toInt()
        binding.tvSpeedValue.text = String.format(Locale.getDefault(), "%.1f", savedSpeed)
        textToSpeech.setSpeechRate(savedSpeed)
    }

    private fun updateUiWithLanguageAndVoice(language: Language, voice: android.speech.tts.Voice) {
        binding.let {
            val selectedLocale = Locale.forLanguageTag(language.voiceAssistantLocale)
            it.tvLanguage.text = selectedLocale.getDisplayLanguage(selectedLocale)
            val displayName = calculateDisplayNameForVoice(voice)

            it.tvLanguageDetail.text = displayName

            val flagUrl = UserPreferences.getFlagUrlForLanguage(requireContext())
            it.ivFlag.load(flagUrl) {
                placeholder(R.drawable.flag_2_svgrepo_com)
                error(R.drawable.danger_triangle_svgrepo_com)
            }
        }
    }

    private fun calculateDisplayNameForVoice(voice: android.speech.tts.Voice): String {
        val languageName = voice.locale.displayLanguage
        val languageAndCountry = "${languageName}-${voice.locale.country}"

        val voicesByLanguageAndCountry = textToSpeech.voices.groupBy {
            "${it.locale.displayLanguage}-${it.locale.country}"
        }

        val count = voicesByLanguageAndCountry[languageAndCountry]?.indexOf(voice)?.plus(1) ?: 1
        return "$languageName $count (${voice.name})"
    }

    /* --- */

    private fun toggleControlBlockVisibility() {
        val areControlsExpanded = binding.pitchBlock.isVisible

        // Hide the keyboard if it's visible and we're expanding the controls
        if (!areControlsExpanded) {
            hideKeyboard()
            binding.editText.clearFocus()
        }

        binding.pitchBlock.animate().setDuration(200)
            .translationY(if (areControlsExpanded) binding.pitchBlock.height.toFloat() else 0f)
            .withEndAction {
                binding.pitchBlock.visibility = if (areControlsExpanded) View.GONE else View.VISIBLE
            }

        binding.speedBlock.animate().setDuration(200)
            .translationY(if (areControlsExpanded) binding.speedBlock.height.toFloat() else 0f)
            .withEndAction {
                binding.speedBlock.visibility = if (areControlsExpanded) View.GONE else View.VISIBLE
            }

        binding.lineControlBlock.animate().setDuration(200)
            .alpha(if (areControlsExpanded) 0f else 1f).withEndAction {
                binding.lineControlBlock.visibility =
                    if (areControlsExpanded) View.GONE else View.VISIBLE
            }

        binding.voiceBlock.animate().setDuration(200)
            .translationY(if (areControlsExpanded) binding.voiceBlock.height.toFloat() else 0f)
            .withEndAction {
                binding.voiceBlock.visibility = if (areControlsExpanded) View.GONE else View.VISIBLE
            }

        binding.viewVoice.animate().setDuration(200).alpha(if (areControlsExpanded) 0f else 1f)
            .withEndAction {
                binding.viewVoice.visibility = if (areControlsExpanded) View.GONE else View.VISIBLE
            }

        binding.ivExpandCollapse.animate().setDuration(200)
            .rotation(if (areControlsExpanded) 180f else 0f)
    }

    private fun handleKeyboardVisibility(isOpen: Boolean) {
        binding.pitchBlock.animate().setDuration(200)
            .translationY(if (isOpen) binding.pitchBlock.height.toFloat() else 0f).withEndAction {
                if (_binding != null) {
                    binding.pitchBlock.visibility = if (isOpen) View.GONE else View.VISIBLE
                }
            }

        binding.speedBlock.animate().setDuration(200)
            .translationY(if (isOpen) binding.speedBlock.height.toFloat() else 0f).withEndAction {
                if (_binding != null) {
                    binding.speedBlock.visibility = if (isOpen) View.GONE else View.VISIBLE
                }
            }

        binding.lineControlBlock.animate().setDuration(200).alpha(if (isOpen) 0f else 1f)
            .withEndAction {
                if (_binding != null) {
                    binding.lineControlBlock.visibility = if (isOpen) View.GONE else View.VISIBLE
                }
            }

        binding.voiceBlock.animate().setDuration(200)
            .translationY(if (isOpen) binding.voiceBlock.height.toFloat() else 0f).withEndAction {
                if (_binding != null) {
                    binding.voiceBlock.visibility = if (isOpen) View.GONE else View.VISIBLE
                }
            }

        binding.viewVoice.animate().setDuration(200).alpha(if (isOpen) 0f else 1f).withEndAction {
            if (_binding != null) {
                binding.viewVoice.visibility = if (isOpen) View.GONE else View.VISIBLE
            }
        }

        binding.ivExpandCollapse.animate().setDuration(200).rotation(if (isOpen) 180f else 0f)
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = requireActivity().currentFocus
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        }
    }

    /* --- */

    private fun setupSeekBar(
        seekBar: SeekBar,
        valueTextView: TextView,
        initialValue: Float,
        onProgressChanged: (Float) -> Unit,
        onSave: (Float) -> Unit
    ) {
        seekBar.post {
            seekBar.progress = (initialValue * 100).toInt()
            valueTextView.text = String.format(Locale.getDefault(), "%.1f", initialValue)
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val floatValue = (progress / 100.0f).coerceAtLeast(0.1f)
                valueTextView.text = String.format(Locale.getDefault(), "%.1f", floatValue)
                onProgressChanged(floatValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val floatValue = (seekBar?.progress ?: 100) / 100.0f
                onSave(floatValue)
            }
        })
    }

    private fun resetAndSavePitch() {
        val defaultPitch = 1.0f
        binding.pitchSeekBar.progress = (defaultPitch * 100).toInt()
        binding.tvPitchValue.text = String.format(Locale.getDefault(), "%.1f", defaultPitch)
        textToSpeech.setPitch(defaultPitch)
        UserPreferences.savePitch(requireContext(), defaultPitch)
        Log.d(TAG, "resetAndSavePitch: $defaultPitch")
    }

    private fun resetAndSaveSpeed() {
        val defaultSpeed = 1.0f
        binding.speedSeekBar.progress = (defaultSpeed * 100).toInt()
        binding.tvSpeedValue.text = String.format(Locale.getDefault(), "%.1f", defaultSpeed)
        textToSpeech.setSpeechRate(defaultSpeed)
        UserPreferences.saveSpeed(requireContext(), defaultSpeed)
        Log.d(TAG, "resetAndSaveSpeed: $defaultSpeed")
    }

    /* --- */

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data

                if (data?.getBooleanExtra("language_changed", false) == true) {
                    requireActivity().recreate()
                }

                val selectedLanguage = getParcelableExtra<Language>(data, KEY_LANGUAGE)
                val selectedVoice = getParcelableExtra<Voice>(data, KEY_VOICE)

                if (selectedLanguage != null && selectedVoice != null) {
                    if (isTextToSpeechInitialized) {
                        handleLanguageAndVoiceResult(selectedLanguage, selectedVoice)
                    } else {
                        pendingLanguage = selectedLanguage
                        pendingVoice = selectedVoice
                    }
                } else {
                    // Handle case where extras are missing or of the wrong type
                    Log.e(TAG, "Error retrieving language or voice from result data")
                    // You could also display an error message to the user here
                }
            }
        }

    private inline fun <reified T : Parcelable> getParcelableExtra(
        intent: Intent?, key: String
    ): T? {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(key)
        }
    }

    private fun showLanguageListFragment() {
        val intent = Intent(requireContext(), LanguageListActivity::class.java)
        val selectedTtsLanguage =
            UserPreferences.getSelectedLanguageAndVoice(requireContext()).first
        intent.putExtra("selectedTtsLanguage", selectedTtsLanguage)
        startForResult.launch(intent)
    }

    /* --- */

    private fun updateButtonStates(s: String) {
        binding.saveBlock.isEnabled = s.isNotBlank()
        binding.speakBlock.isEnabled = s.isNotBlank()
        val color = when {
            s.isBlank() -> ContextCompat.getColor(requireContext(), R.color.colorAccent)
            else -> primaryColor
        }

        binding.saveButton.setColorFilter(color)
        binding.speakButton.setColorFilter(color)
    }

    private fun applyWordLimit() {
        val maxWords = if (isUserPremium()) {
            Int.MAX_VALUE
        } else {
            MAX_WORDS_NON_PREMIUM
        }

        val wordLimitFilter = WordLimitInputFilter(maxWords)
        binding.editText.filters = if (maxWords == Int.MAX_VALUE) {
            arrayOf() // Remove any existing filters
        } else {
            arrayOf(wordLimitFilter)
        }
    }

    private class WordLimitInputFilter(private val maxWords: Int) : InputFilter {
        override fun filter(
            source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int
        ): CharSequence? {
            val currentText = dest.toString()
            val newText =
                currentText.substring(0, dstart) + source.toString() + currentText.substring(dend)
            val wordCount = newText.trim().split("\\s+".toRegex()).size

            return if (wordCount <= maxWords) {
                null // Accept the input
            } else {
                "" // Block the input
            }
        }
    }

    private fun isUserPremium(): Boolean {
        return UserPreferences.isPremiumUser()
    }

    private fun updateTextLimitIndicator() {
        val maxWords = if (isUserPremium()) {
            Int.MAX_VALUE
        } else {
            MAX_WORDS_NON_PREMIUM
        }

        val currentText = binding.editText.text.toString()
        val currentWords =
            if (currentText.isBlank()) 0 else currentText.trim().split("\\s+".toRegex()).size
        val indicatorText = if (maxWords == Int.MAX_VALUE) {
            "$currentWords/∞"
        } else {
            "$currentWords/$maxWords"
        }
        binding.tvTextLimitIndicator.text = indicatorText
    }

    /* --- */

    private fun setupClipboardFunctionality() {
        clipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        updatePasteButtonState()

        clipboardManager.addPrimaryClipChangedListener {
            updatePasteButtonState()
        }

        binding.pasteBlock.setOnClickListener {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val textToPaste = item.text
                if (textToPaste != null) {
                    binding.editText.setText(textToPaste.toString())
                } else {
                    requireContext().showErrorToast(TAG, getString(R.string.clipboard_not_text))
                }

                //    val textToPaste = clipData.getItemAt(0).text.toString()
                //    binding.editText.setText(textToPaste)
            }
        }

        // Optional: Monitor EditText changes to disable paste if text is manually entered
        binding.editText.addTextChangedListener {
            updatePasteButtonState()
        }
    }

    private fun updatePasteButtonState() {
        if (_binding == null) return

        val hasText = clipboardManager.hasPrimaryClip()
        binding.pasteBlock.isEnabled = hasText

        val color = if (hasText) {
            primaryColor
        } else {
            ContextCompat.getColor(requireContext(), R.color.colorAccent)
        }
        binding.pasteButton.setColorFilter(color)
    }

    /* --- */

    private fun initTextToSpeechListener() {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}

            override fun onDone(utteranceId: String) {
                Handler(Looper.getMainLooper()).post {
                    binding.animationSpeak.visibility = View.INVISIBLE
                    binding.animationSpeak.cancelAnimation()
                    binding.speakButton.visibility = View.VISIBLE
                    binding.speakButton.isEnabled = true
                    binding.speakText.text = getString(R.string.play)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
            }
        })
    }

    private fun speakButtonClicked() {
        val textToSpeak = binding.editText.text.toString()
        if (textToSpeak.isBlank()) {
            requireContext().showErrorToast(TAG, getString(R.string.cannot_speak_empty_text))
            binding.editText.requestFocus()
        } else {
            if (!textToSpeech.isSpeaking) {
                binding.speakButton.visibility = View.INVISIBLE
                binding.speakButton.isEnabled = false
                binding.animationSpeak.visibility = View.VISIBLE
                binding.animationSpeak.playAnimation()
                binding.speakText.text = getString(R.string.stop)

                Handler(requireContext().mainLooper).postDelayed({
                    textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, textToSpeak)
                }, 100)

                initTextToSpeechListener()

            } else if (textToSpeech.isSpeaking) {
                binding.speakButton.isEnabled = false
                Handler(requireContext().mainLooper).postDelayed({
                    textToSpeech.stop()
                    binding.animationSpeak.visibility = View.INVISIBLE
                    binding.animationSpeak.cancelAnimation()
                    binding.speakButton.visibility = View.VISIBLE
                    binding.speakButton.isEnabled = true
                    binding.speakText.text = getString(R.string.play)
                }, 100)

                binding.saveButton.isEnabled = true
            }
        }
    }

    /* --- */

    private fun saveButtonClicked() {
        val textToSave = binding.editText.text.toString()
        if (textToSave.isBlank()) {
            requireContext().showErrorToast(TAG, getString(R.string.cannot_save_empty_text))
            binding.editText.requestFocus()
        } else {
            val filename = generateFilename()
            saveTextToInternalStorage(textToSave, filename)
        }
    }

    private fun generateFilename(): String {
        val dateFormat = SimpleDateFormat("yyMMdd_HHmmss", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())

        UserPreferences.incrementAndSaveFileCounter()

        val fileCounter = UserPreferences.getFileCounter()

        val formattedCounter = String.format(Locale.getDefault(), "%03d", fileCounter)
        return "TTS_${currentDateTime}_$formattedCounter"
    }

    private fun saveTextToInternalStorage(text: String, filename: String) {
        try {
            val fileOutputStream = requireContext().openFileOutput(filename, Context.MODE_PRIVATE)
            fileOutputStream.write(text.toByteArray())
            fileOutputStream.close()
            requireContext().showSuccessToast(
                TAG, getString(R.string.text_saved_to_internal_storage)
            )

            lifecycleScope.launch {
                delay(1500)
                withContext(Dispatchers.Main) {
                    showInterstitialAd()
                }
            }
        } catch (_: Exception) {
            requireContext().showErrorToast(TAG, getString(R.string.failed_to_save_text))
        }
    }

    private fun showInterstitialAd() {
        if (canShow(INTERSTITIAL, interstitialPlacement)) {
            show(requireActivity(), INTERSTITIAL, interstitialPlacement)
        } else {
            Log.e(TAG, "Cannot show interstitial")
        }

        setInterstitialCallbacks(object : InterstitialCallbacks {
            override fun onInterstitialLoaded(isPrecache: Boolean) {
                Log.d(TAG, "Interstitial was loaded, isPrecache: $isPrecache")
            }

            override fun onInterstitialFailedToLoad() {
                Log.e(TAG, "Interstitial failed to load")
            }

            override fun onInterstitialClicked() {
                Log.d(TAG, "Interstitial was clicked")
            }

            override fun onInterstitialShowFailed() {
                Log.e(TAG, "Interstitial failed to show")
            }

            override fun onInterstitialShown() {
                Log.d(TAG, "Interstitial was shown")
            }

            override fun onInterstitialClosed() {
                Log.d(TAG, "Interstitial was closed")
            }

            override fun onInterstitialExpired() {
                Log.d(TAG, "Interstitial was expired")
            }
        })
    }

    private fun showBanner() {
        // Initially show the custom banner and hide the Appodeal banner
        customBannerContainer.visibility = View.VISIBLE
        binding.appodealBannerView.visibility = View.GONE

        setBannerViewId(binding.appodealBannerView.id)

        // Check if the banner is already loaded
        if (canShow(BANNER, bannerPlacement)) {
            customBannerContainer.visibility = View.GONE
            binding.appodealBannerView.visibility = View.VISIBLE
            context?.let {
                binding.appodealBannerView.setBackgroundColor(
                    it.getColor(
                        R.color.colorAccent
                    )
                )
            }
            show(requireActivity(), BANNER_VIEW, bannerPlacement)
            return
        }

        setBannerCallbacks(object : BannerCallbacks {
            override fun onBannerLoaded(height: Int, isPrecache: Boolean) {
                Log.d(TAG, "Banner was loaded, isPrecache: $isPrecache")
                Log.d(TAG, "onBannerLoaded called")
                if (isAdded && isResumed) {
                    val activity = activity
                    if (activity != null) {
                        // It's safe to use the activity here
                        // ... (your code to show the banner using the activity)
                        Log.d(TAG, "onBannerLoaded: Activity is not null")
                        if (canShow(BANNER, bannerPlacement)) {
                            customBannerContainer.visibility = View.GONE
                            binding.appodealBannerView.visibility = View.VISIBLE
                            context?.let {
                                binding.appodealBannerView.setBackgroundColor(
                                    it.getColor(
                                        R.color.colorAccent
                                    )
                                )
                            }
                            show(requireActivity(), BANNER_VIEW, bannerPlacement)
                        }
                    } else {
                        Log.w(TAG, "onBannerLoaded: Activity is null")
                    }
                } else {
                    Log.w(TAG, "onBannerLoaded: Fragment is not added or not resumed")
                }
            }

            override fun onBannerFailedToLoad() {
                Log.e(TAG, "Banner failed to load")
                customBannerContainer.visibility = View.VISIBLE
            }

            override fun onBannerClicked() {
                Log.d(TAG, "Banner was clicked")
            }

            override fun onBannerShowFailed() {
                Log.e(TAG, "Banner failed to show")
            }

            override fun onBannerShown() {
                Log.d(TAG, "Banner was shown")
            }

            override fun onBannerExpired() {
                Log.d(TAG, "Banner was expired")
            }
        })
    }

    private fun rotateBanner() {
        val appInfo = appList[currentAppIndex]

        // Update the views
        customBannerIcon.setImageResource(appInfo.iconResId)
        customBannerTitle.text = appInfo.title
        customBannerRatingBar.rating = appInfo.rating
        customBannerDescription.text = appInfo.description
        customBannerButton.setOnClickListener {
            openAppInStore(appInfo.packageName)
        }

        currentAppIndex = (currentAppIndex + 1) % appList.size
    }

    private fun openAppInStore(appPackageName: String = "") {
        val packageName = if (appPackageName.isBlank()) {
            "com.bonodigitalstudio.batterymonitorapp" // Replace with your default app package name
        } else {
            appPackageName
        }
        try {
            when {
                AppStoreHelper.isGooglePlayStore(requireContext()) || AppStoreHelper.isRunningInEmulator() -> {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")
                        )
                    )
                }

                AppStoreHelper.isSamsungGalaxyStore(requireContext()) -> {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("samsungapps://ProductDetail/$packageName")
                        )
                    )
                }
            }
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    /* --- */

    private fun showTutorial() {
        val onboardingPrefs = OnboardingPrefs(requireContext())
        if (onboardingPrefs.isTutorialShown()) {
            return
        }

        tapTargetSequence = TapTargetSequence(requireActivity())
        tapTargetSequence?.targets(
            TapTarget.forView(
                binding.editText,
                getString(R.string.tutorial_title_1),
                getString(R.string.tutorial_desc_1)
            ).outerCircleAlpha(0.91f).outerCircleColor(R.color.colorPrimary)
                .targetCircleColor(R.color.white).titleTextColor(R.color.white)
                .descriptionTextColor(R.color.white).transparentTarget(true).drawShadow(true)
                .cancelable(true),

            TapTarget.forView(
                binding.speakButton,
                getString(R.string.tutorial_title_2),
                getString(R.string.tutorial_desc_2)
            ).outerCircleAlpha(0.91f).outerCircleColor(R.color.colorPrimary)
                .targetCircleColor(R.color.white).titleTextColor(R.color.white)
                .descriptionTextColor(R.color.white).transparentTarget(true).drawShadow(true)
                .cancelable(true),

            TapTarget.forView(
                binding.pitchSeekBar,
                getString(R.string.tutorial_title_3),
                getString(R.string.tutorial_desc_3)
            ).outerCircleAlpha(0.91f).outerCircleColor(R.color.colorPrimary)
                .targetCircleColor(R.color.white).titleTextColor(R.color.white)
                .descriptionTextColor(R.color.white).transparentTarget(true).drawShadow(true)
                .cancelable(true),

            TapTarget.forView(
                binding.tvLanguage,
                getString(R.string.tutorial_title_4),
                getString(R.string.tutorial_desc_4)
            ).outerCircleAlpha(0.91f).outerCircleColor(R.color.colorPrimary)
                .targetCircleColor(R.color.white).titleTextColor(R.color.white)
                .descriptionTextColor(R.color.white).transparentTarget(true).drawShadow(true)
                .cancelable(true)
        )
        tapTargetSequence?.listener(object : TapTargetSequence.Listener {
            override fun onSequenceFinish() {
                // Called when the entire sequence is finished
                onboardingPrefs.setTutorialShown() // Mark tutorial as shown
            }

            override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {}
            override fun onSequenceCanceled(lastTarget: TapTarget) {
                onboardingPrefs.setTutorialShown()
            }
        })
        tapTargetSequence?.start()
    }

    /* --- */

    override fun onResume() {
        super.onResume()
        selectedLanguage?.let { language ->
            selectedVoice?.let { voice ->
                updateUiWithLanguageAndVoice(language, voice)
            }
        }
        selectedLanguage = null
        selectedVoice = null

        // Start banner rotation when the fragment is resumed
        handler.post(bannerRotationRunnable)

        showBanner()
    }

    override fun onPause() {
        super.onPause()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
        }

        // Stop banner rotation when the fragment is paused
        handler.removeCallbacks(bannerRotationRunnable)

    }

    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.shutdown()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        keyboardVisibilityEventUnregistrar?.unregister()
        _binding = null
    }
}

