package com.woocommerce.android.ui.jitm

import android.content.Context
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

class JitmClickHandler @Inject constructor(private val context: Context) {
    fun onJitmCtaClicked(url: String) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }
}
