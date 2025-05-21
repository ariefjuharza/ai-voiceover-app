package com.rpmstudio.texttospeech.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.appodeal.ads.Appodeal
import com.appodeal.ads.NativeAd
import com.appodeal.ads.NativeCallbacks
import com.appodeal.ads.NativeMediaViewContentType
import com.appodeal.ads.initializing.ApdInitializationError
import com.appodeal.ads.nativead.NativeAdView
import com.appodeal.ads.nativead.Position
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rpmstudio.texttospeech.BuildConfig
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.data.AppPreferences
import com.rpmstudio.texttospeech.data.AppPreferences.IS_ENABLE_CHECK_UPDATE
import com.rpmstudio.texttospeech.data.OnboardingPrefs
import com.rpmstudio.texttospeech.databinding.ActivityMainBinding
import com.rpmstudio.texttospeech.interfaces.CheckUpdateInterface
import com.rpmstudio.texttospeech.utils.LocaleContextWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.collections.forEach

class MainActivity : AppCompatActivity(), CheckUpdateInterface {
    companion object {
        private const val TAG = "MainActivity"
        var instance: MainActivity? = null

        /**
         * Use NativeAdView::class to checking your custom layout view.
         * Use NativeAdViewNewsFeed::class or
         * NativeAdViewContentStream::class or
         * NativeAdViewAppWall::class to check native templates
         * */
        val nativeAdViewType = NativeAdView::class

        fun configureNativeAdView(nativeAdView: NativeAdView) {
            nativeAdView.setAdChoicesPosition(Position.END_TOP)
            nativeAdView.setAdAttributionBackground(Color.RED)
            nativeAdView.setAdAttributionTextColor(Color.WHITE)
        }
    }

    val updateFlowResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { _ -> }

    lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView
    private var isAppReady = false

    override fun attachBaseContext(newBase: Context) {
        val appLanguage = AppPreferences.getAppLanguage() ?: "en"
        val locale = Locale(appLanguage)
        val wrappedContext = LocaleContextWrapper(newBase, locale)
        super.attachBaseContext(wrappedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        lifecycleScope.launch {
            delay(1000) // Simulate some work
            isAppReady = true
        }

        val onboardingPrefs = OnboardingPrefs(this)
        if (!onboardingPrefs.isOnboardingShown()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setupEdgeToEdge()
        setContentView(binding.root)

        bottomNavigationView = binding.bottomNavigationView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavigationView.setupWithNavController(navController)

        setUpAppodealSDK()
    }

    private fun setUpAppodealSDK() {
    //    Appodeal.setLogLevel(LogLevel.verbose)
        Appodeal.setTesting(false)
        Appodeal.setPreferredNativeContentType(NativeMediaViewContentType.Auto)
        Appodeal.initialize(
            this,
            BuildConfig.APP_KEY,
            Appodeal.INTERSTITIAL or Appodeal.NATIVE or Appodeal.BANNER,
        ) { errors: List<ApdInitializationError>? ->
            errors?.forEach {
                Log.e(TAG, "onInitializationFinished: ", it)
            }
        }

        Appodeal.setNativeCallbacks(object : NativeCallbacks {
            override fun onNativeLoaded() {
                Log.d(TAG, "Native was loaded")
            }

            override fun onNativeFailedToLoad() {
                Log.e(TAG, "Native failed to load")
            }

            override fun onNativeClicked(nativeAd: NativeAd?) {
                Log.d(TAG, "Native was clicked")
            }

            override fun onNativeShowFailed(nativeAd: NativeAd?) {
                Log.e(TAG, "Native failed to show")
            }

            override fun onNativeShown(nativeAd: NativeAd?) {
                Log.d(TAG, "Native was shown")
            }

            override fun onNativeExpired() {
                Log.d(TAG, "Native was expired")
            }
        })
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        if (instance == null) instance = this

        if (prefs.getBoolean(
                IS_ENABLE_CHECK_UPDATE, resources.getBoolean(
                    R.bool.is_enable_check_update
                )
            )
        ) checkUpdateFromGooglePlay()
    }
}

