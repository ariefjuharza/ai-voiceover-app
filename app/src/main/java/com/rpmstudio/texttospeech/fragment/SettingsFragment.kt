package com.rpmstudio.texttospeech.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.activity.AboutLibsActivity
import com.rpmstudio.texttospeech.activity.RateDialog
import com.rpmstudio.texttospeech.data.AppPreferences
import com.rpmstudio.texttospeech.data.AppPreferences.KEY_APP_LANGUAGE
import com.rpmstudio.texttospeech.utils.AppStoreHelper
import com.rpmstudio.texttospeech.interfaces.CheckUpdateInterface
import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat(), CheckUpdateInterface {

    private var rateDialog: RateDialog? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        appLanguagePreference()

        findPreference<Preference>("rate_app")?.setOnPreferenceClickListener {
            openPlayStore()
            true
        }

        findPreference<Preference>("feedback")?.setOnPreferenceClickListener {
            openFeedbackEmail()
            true
        }

        findPreference<Preference>("about")?.setOnPreferenceClickListener {
            showAboutApp()
            true
        }

        findPreference<Preference>("check_update")?.setOnPreferenceClickListener {
            checkUpdate()
            true
        }

    }

    private fun appLanguagePreference() {
        val appLanguagePreference = findPreference<ListPreference>(KEY_APP_LANGUAGE)
        val currentLanguage = AppPreferences.getAppLanguage()
        Log.d("SettingsFragment", "Current Language: $currentLanguage")
        val entryMap =
            appLanguagePreference?.entries?.zip(appLanguagePreference.entryValues)?.toMap()

        Log.d("SettingsFragment", "Current Language: $currentLanguage")
        Log.d("SettingsFragment", "Entry Map: $entryMap")
        Log.d("SettingsFragment", "Entry Map Keys: ${entryMap?.values}")

        appLanguagePreference?.value = when {
            entryMap?.values?.contains(currentLanguage) == true -> {
                Log.d("SettingsFragment", "Selecting: $currentLanguage")
                currentLanguage
            }

            else -> {
                Log.d("SettingsFragment", "Defaulting to English")
                "en"
            }
        }

        appLanguagePreference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val selectedLanguage = newValue as String
                Log.d("SettingsFragment", "Saving language: $selectedLanguage")
                AppPreferences.setAppLanguage(selectedLanguage)
                Log.d("SettingsFragment", "Language saved.")

                val locale = Locale.forLanguageTag(selectedLanguage)
                val localeList = LocaleListCompat.create(locale)
                AppCompatDelegate.setApplicationLocales(localeList)

                true
            }

        appLanguagePreference?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }

    private fun openPlayStore() {
        rateDialog = RateDialog(requireActivity())
        rateDialog?.show()
    }

    private fun openFeedbackEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:dev.rpmstudio@gmail.com".toUri()
            putExtra(Intent.EXTRA_SUBJECT, requireContext().getString(R.string.app_feedback))
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun showAboutApp() {
        startActivity(Intent(requireContext(), AboutLibsActivity::class.java))
    }

    private fun checkUpdate() {
        when {
            AppStoreHelper.isGooglePlayStore(requireContext()) || AppStoreHelper.isRunningInEmulator() -> {
                checkUpdateFromGooglePlay()
            }

            AppStoreHelper.isSamsungGalaxyStore(requireContext()) -> {
                redirectToSamsungGalaxy()
            }
        }
    }

    private fun redirectToSamsungGalaxy() {
        val packageName = requireContext().packageName
        val galaxyStoreProductDetailUri = "samsungapps://ProductDetail/$packageName".toUri()
        val intent = Intent(Intent.ACTION_VIEW, galaxyStoreProductDetailUri)

        // Check if the Galaxy Store app is installed
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("RateDialog", "Error: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    getString(R.string.could_not_open_galaxy_store),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // Handle the case where the Galaxy Store is not installed
            Log.e("RateDialog", getString(R.string.galaxy_store_not_found))
            Toast.makeText(
                requireContext(), getString(R.string.galaxy_store_not_found), Toast.LENGTH_SHORT
            ).show()
        }
    }
}