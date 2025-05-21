package com.rpmstudio.texttospeech.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.data.AppPreferences
import com.rpmstudio.texttospeech.data.Language
import com.rpmstudio.texttospeech.data.OnboardingPrefs
import com.rpmstudio.texttospeech.data.Voice
import com.rpmstudio.texttospeech.databinding.ActivityOnboardingBinding
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_LANGUAGE
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_VOICE
import com.rpmstudio.texttospeech.fragment.onBoarding.IntroFragment1
import com.rpmstudio.texttospeech.fragment.onBoarding.IntroFragment2
import com.rpmstudio.texttospeech.utils.LocaleContextWrapper
import com.rpmstudio.texttospeech.utils.showErrorToast
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class OnboardingActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "OnboardingActivity"
        private const val DELAY_MS: Long = 3000
        private const val PERIOD_MS: Long = 3000
    }

    private lateinit var binding: ActivityOnboardingBinding

    private lateinit var timer: Timer
    private var currentPage = 0

    private var selectedLanguage: Language? = null
    private var selectedVoice: Voice? = null

    private var isPermissionGranted = false
    private var isLanguageSelected = false
    private var isSetupComplete = false

    override fun attachBaseContext(newBase: Context) {
        val appLanguage = AppPreferences.getAppLanguage() ?: "en"
        val locale = Locale(appLanguage)
        val wrappedContext = LocaleContextWrapper(newBase, locale)
        super.attachBaseContext(wrappedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()

        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter

        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.cvLanguage.setOnClickListener {
            val intent = Intent(this, LanguageListActivity::class.java)
            startForResult.launch(intent)
        }


        if (!isPermissionGranted) {
            binding.cvPermission.setOnClickListener {
                requestPermissionLauncher.launch(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }
                )
            }
        } else {
            binding.cvPermission.visibility = View.GONE
        }

        binding.exitOnboardingButton.setOnClickListener {
            if (isSetupComplete) {
                val onboardingPrefs = OnboardingPrefs(this)
                onboardingPrefs.setOnboardingShown()

                val intentToMain = Intent(this, MainActivity::class.java).apply {
                    putExtra(KEY_LANGUAGE, selectedLanguage)
                    putExtra(KEY_VOICE, selectedVoice)
                }
                startActivity(intentToMain)
                finish()
            } else {
                showErrorToast(TAG, getString(R.string.please_complete_the_setup_first))
            }
        }

        val privacyPolicyUrl = getString(R.string.privacy_policy_url)
        val termsOfUseUrl = getString(R.string.terms_of_use_url)

        val formattedPrivacyText =
            getString(R.string.privacy_agreement_text, privacyPolicyUrl, termsOfUseUrl)
        binding.tvPrivacyLink.text = formattedPrivacyText

        val spannableString = SpannableString(binding.tvPrivacyLink.text)
        val privacyPolicyStartIndex =
            spannableString.indexOf(getString(R.string.privacy_policy_text))
        val termsOfUseStartIndex = spannableString.indexOf(getString(R.string.terms_of_use_text))

        if (privacyPolicyStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        openUrl(privacyPolicyUrl)
                    }
                },
                privacyPolicyStartIndex,
                privacyPolicyStartIndex + getString(R.string.privacy_policy_text).length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (termsOfUseStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        openUrl(termsOfUseUrl)
                    }
                },
                termsOfUseStartIndex,
                termsOfUseStartIndex + getString(R.string.terms_of_use_text).length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.tvPrivacyLink.text = spannableString
        binding.tvPrivacyLink.movementMethod = LinkMovementMethod.getInstance()

    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Helper function to open URLs
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isPermissionGranted = true
            binding.ivCheckPermission.visibility = View.VISIBLE
            checkSetupComplete()
        } else {
            // Handle permission denial
            isPermissionGranted = true
            binding.ivCheckPermission.setImageResource(R.drawable.danger_triangle_svgrepo_com)
            binding.ivCheckPermission.setColorFilter(
                ContextCompat.getColor(
                    this, R.color.color_warning
                )
            )
            binding.ivCheckPermission.visibility = View.VISIBLE
            checkSetupComplete()
        }
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                isLanguageSelected = true
                binding.ivCheckLanguage.visibility = View.VISIBLE
                checkSetupComplete()
            }
        }

    private fun checkSetupComplete() {
        isSetupComplete = isPermissionGranted
             //   && isLanguageSelected

        if (isSetupComplete) {
            binding.exitOnboardingButton.isEnabled = true

            val defaultButtonColor = TypedValue()
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimaryDark, defaultButtonColor, true
            )
            binding.exitOnboardingButton.backgroundTintList =
                ColorStateList.valueOf(defaultButtonColor.data)
        }
    }

    private fun startAutoSlide() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    currentPage = (currentPage + 1) % binding.viewPager.adapter?.itemCount!!
                    binding.viewPager.currentItem = currentPage
                }
            }
        }, DELAY_MS, PERIOD_MS)
    }

    override fun onResume() {
        super.onResume()
        startAutoSlide()
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }


    class OnboardingPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> IntroFragment1()
                1 -> IntroFragment2()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}