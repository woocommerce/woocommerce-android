package com.woocommerce.android.ui.woopos.common.util

import android.content.Context
import com.woocommerce.android.extensions.copyToClipboard
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WooPosClipboardHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun copyToClipboard(text: String) {
        context.copyToClipboard("WooCommerce", text)
    }
}
