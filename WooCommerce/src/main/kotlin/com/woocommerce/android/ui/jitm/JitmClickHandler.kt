package com.woocommerce.android.ui.jitm

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.woocommerce.android.ui.mystore.MyStoreViewModel
import javax.inject.Inject

class JitmClickHandler @Inject constructor(
    private val context: Context,
    private val jitmTracker: JitmTracker,
) {
    fun onJitmCtaClicked(
        id: String,
        featureClass: String,
        url: String
    ) {
        jitmTracker.trackJitmCtaTapped(
            MyStoreViewModel.UTM_SOURCE,
            id,
            featureClass
        )

        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }
}
