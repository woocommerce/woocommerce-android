package com.woocommerce.android.ui.woopos

import android.content.Context
import javax.inject.Inject

class WooPosIsPhoneMode @Inject constructor(
    private val context: Context,
) {
    operator fun invoke(): Boolean =
        context.resources.configuration.smallestScreenWidthDp < PHONE_MAX_SMALLEST_WIDTH_DP

    companion object {
        private const val PHONE_MAX_SMALLEST_WIDTH_DP = 600
    }
}
