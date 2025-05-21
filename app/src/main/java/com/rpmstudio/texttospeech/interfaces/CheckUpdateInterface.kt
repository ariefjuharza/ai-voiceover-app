package com.rpmstudio.texttospeech.interfaces

import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.common.IntentSenderForResultStarter
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.activity.MainActivity
import com.rpmstudio.texttospeech.fragment.SettingsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

interface CheckUpdateInterface {

    fun MainActivity.checkUpdateFromGooglePlay() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable = isUpdateAvailable(appUpdateInfo)
            val updateType = if (appUpdateInfo.isImmediateUpdateAllowed) AppUpdateType.IMMEDIATE
            else AppUpdateType.FLEXIBLE

            if (isUpdateDeveloperTriggered(appUpdateInfo)) {
                intentResultStarter()
                val appUpdateOptions =
                    AppUpdateOptions.newBuilder(updateType).setAllowAssetPackDeletion(false).build()
                startUpdate(
                    this,
                    appUpdateManager,
                    appUpdateInfo,
                    updateFlowResultLauncher,
                    appUpdateOptions
                )
            } else if (isUpdateAvailable) {
                intentResultStarter()
                val appUpdateOptions =
                    AppUpdateOptions.newBuilder(updateType).setAllowAssetPackDeletion(false).build()
                updateAvailable(appUpdateManager, appUpdateInfo, appUpdateOptions)
            }
        }
    }

    fun SettingsFragment.checkUpdateFromGooglePlay() {
        val appUpdateManager = AppUpdateManagerFactory.create(requireContext())
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable = isUpdateAvailable(appUpdateInfo)
            val updateType = if (appUpdateInfo.isImmediateUpdateAllowed) AppUpdateType.IMMEDIATE
            else AppUpdateType.FLEXIBLE
            if (isUpdateDeveloperTriggered(appUpdateInfo)) {
                val updateFlowResultLauncher = MainActivity.Companion.instance?.updateFlowResultLauncher
                MainActivity.Companion.instance?.intentResultStarter()
                val appUpdateOptions =
                    AppUpdateOptions.newBuilder(updateType).setAllowAssetPackDeletion(false).build()
                startUpdate(
                    requireContext(),
                    appUpdateManager,
                    appUpdateInfo,
                    updateFlowResultLauncher!!,
                    appUpdateOptions
                )
            }
            if (isUpdateAvailable) {
                MainActivity.Companion.instance?.intentResultStarter()
                val updateFlowResultLauncher = MainActivity.Companion.instance?.updateFlowResultLauncher
                val appUpdateOptions =
                    AppUpdateOptions.newBuilder(updateType).setAllowAssetPackDeletion(false).build()
                startUpdate(
                    requireContext(),
                    appUpdateManager,
                    appUpdateInfo,
                    updateFlowResultLauncher!!,
                    appUpdateOptions
                )
            } else {
                Toast.makeText(requireContext(), R.string.update_not_found, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun MainActivity.updateAvailable(
        appUpdateManager: AppUpdateManager,
        appUpdateInfo: AppUpdateInfo,
        appUpdateOptions: AppUpdateOptions
    ) {
        var isUpdate = false
        Snackbar.make(
            binding.root,
            getString(R.string.update_available),
            5.seconds.inWholeMilliseconds.toInt()
        ).apply {
            setAction(getString(R.string.update)) {
                isUpdate = true
                startUpdate(
                    this@updateAvailable,
                    appUpdateManager,
                    appUpdateInfo,
                    updateFlowResultLauncher,
                    appUpdateOptions
                )
            }
            show()
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(3.seconds)
        }
    }

    private fun MainActivity.intentResultStarter() =
        IntentSenderForResultStarter { intent, _, fillInIntent, flagsMask, flagsValues, _, _ ->
            val request = IntentSenderRequest.Builder(intent).setFillInIntent(fillInIntent)
                .setFlags(flagsValues, flagsMask).build()
            updateFlowResultLauncher.launch(request)
        }

    private fun isUpdateAvailable(appUpdateInfo: AppUpdateInfo): Boolean {
        val isUpdateAvailable =
            appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        val isUpdateAllowed =
            appUpdateInfo.isImmediateUpdateAllowed || appUpdateInfo.isFlexibleUpdateAllowed
        return isUpdateAvailable && isUpdateAllowed
    }

    private fun isUpdateDeveloperTriggered(appUpdateInfo: AppUpdateInfo) =
        appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

    private fun startUpdate(
        context: Context,
        appUpdateManager: AppUpdateManager,
        appUpdateInfo: AppUpdateInfo,
        updateFlowResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        appUpdateOptions: AppUpdateOptions
    ) = try {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo, updateFlowResultLauncher, appUpdateOptions
        )
    } catch (_: Exception) {
        Toast.makeText(context, R.string.update_error, Toast.LENGTH_LONG).show()
    }

}