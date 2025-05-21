package com.rpmstudio.texttospeech.utils

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.core.net.toUri
import androidx.preference.Preference
import com.rpmstudio.texttospeech.R

class WebLinkPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    private var url: String? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.WebLinkPreference) {
            url = getString(R.styleable.WebLinkPreference_url)
        }
    }

    override fun onClick() {
        super.onClick()
        url?.let {
            val intent = Intent(Intent.ACTION_VIEW, it.toUri())
            context.startActivity(intent)
        }
    }
}