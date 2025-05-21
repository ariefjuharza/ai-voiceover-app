package com.rpmstudio.texttospeech.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object AppStoreHelper {
    private const val TAG = "AppStoreHelper"
    const val GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending"
    const val SAMSUNG_GALAXY_STORE_PACKAGE_NAME = "com.sec.android.app.samsungapps"

    fun getInstallerPackageName(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION") context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "getInstallerPackageName: Package name not found", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "getInstallerPackageName: An unexpected error occurred", e)
            null
        }
    }

    fun isGooglePlayStore(context: Context): Boolean {
        val installer = getInstallerPackageName(context)
        return installer == GOOGLE_PLAY_PACKAGE_NAME
    }

    fun isSamsungGalaxyStore(context: Context): Boolean {
        val installer = getInstallerPackageName(context)
        return installer == SAMSUNG_GALAXY_STORE_PACKAGE_NAME
    }

    fun isRunningInEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic") || Build.FINGERPRINT.contains("emulator")
    }
}