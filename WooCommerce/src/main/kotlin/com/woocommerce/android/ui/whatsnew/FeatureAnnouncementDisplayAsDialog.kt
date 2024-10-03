package com.woocommerce.android.ui.whatsnew

import android.content.Context
import com.woocommerce.android.ui.woopos.util.ext.getScreenHeightDp
import com.woocommerce.android.ui.woopos.util.ext.getScreenWidthDp
import javax.inject.Inject
import kotlin.math.min

class FeatureAnnouncementDisplayAsDialog @Inject constructor(private val context: Context) {
    operator fun invoke(): Boolean {
        val screenWidthDp = context.getScreenWidthDp()
        val screenHeightDp = context.getScreenHeightDp()

        val shortSize = min(screenWidthDp, screenHeightDp)

        return shortSize >= MIN_SCREEN_SHORT_SIZE_DP
    }

    private companion object {
        const val MIN_SCREEN_SHORT_SIZE_DP = 674
    }
}
