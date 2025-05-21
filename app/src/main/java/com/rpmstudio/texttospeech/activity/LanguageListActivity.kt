package com.rpmstudio.texttospeech.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.data.AppPreferences
import com.rpmstudio.texttospeech.data.Language
import com.rpmstudio.texttospeech.data.Voice
import com.rpmstudio.texttospeech.databinding.ActivityLanguageListBinding
import com.rpmstudio.texttospeech.fragment.LanguageListFragment
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_LANGUAGE
import com.rpmstudio.texttospeech.fragment.VoiceListFragment.Companion.KEY_VOICE
import com.rpmstudio.texttospeech.interfaces.OnLanguageAndVoiceSelectedListener
import java.util.Locale

class LanguageListActivity : AppCompatActivity(), OnLanguageAndVoiceSelectedListener {
    private lateinit var binding: ActivityLanguageListBinding

    override fun attachBaseContext(newBase: Context) {
        val appLanguage = AppPreferences.getAppLanguage() ?: "en"
        val locale = Locale(appLanguage)
        val config = Configuration(newBase.resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)

        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()

        val selectedTtsLanguage = intent.getStringExtra("selectedTtsLanguage") ?: "en"

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container, LanguageListFragment.newInstance(selectedTtsLanguage)
            ).commit()
        }

    //    loadAd()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /*
    private fun loadAd() {
        cache(this, NATIVE, 5) // Cache 5 native ads
    }

     */

    override fun onLanguageAndVoiceSelected(language: Language, voice: Voice) {
        val resultIntent = Intent()
        resultIntent.putExtra(KEY_LANGUAGE, language)
        resultIntent.putExtra(KEY_VOICE, voice)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}