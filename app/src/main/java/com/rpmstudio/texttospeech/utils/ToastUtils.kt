package com.rpmstudio.texttospeech.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

fun Context.showErrorToast(tag: String, errorMessage: String) {
    Log.e(tag, errorMessage)
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

fun Context.showSuccessToast(tag: String, successMessage: String) {
    Log.d(tag, successMessage)
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
    }
}

