package com.rpmstudio.texttospeech.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult.SERVICE_DISABLED
import com.google.android.gms.common.ConnectionResult.SERVICE_MISSING
import com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED
import com.google.android.gms.common.ConnectionResult.SUCCESS
import com.google.android.gms.common.GoogleApiAvailability

class MobileServicesChecker {
    fun isAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        return when (val status = googleApiAvailability.isGooglePlayServicesAvailable(context)) {
            SUCCESS -> true
            SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED, SERVICE_DISABLED -> {
                Log.e("GmsChecker", "GMS not available: status code $status")
                false
            }

            else -> {
                Log.w("GmsChecker", "Unexpected GMS availability status: $status")
                false
            }
        }
    }
}
