package com.woocommerce.android.ui.woopos

import android.content.Context
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.ext.getScreenHeightDp
import com.woocommerce.android.ui.woopos.util.ext.getScreenWidthDp
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class WooPosIsScreenSizeAllowed @Inject constructor(
    private val context: Context,
    private val wooPosLog: WooPosLogWrapper,
) {
    operator fun invoke(): Boolean {
        val screenWidthDp = context.getScreenWidthDp()
        val screenHeightDp = context.getScreenHeightDp()

        val shortSize = min(screenWidthDp, screenHeightDp)
        val longSize = max(screenWidthDp, screenHeightDp)

        val isAllowed = shortSize >= MIN_SCREEN_SHORT_SIZE_DP && longSize >= MIN_SCREEN_LONG_SIZE_DP
        return isAllowed.also {
            if (!isAllowed) {
                wooPosLog.i(
                    "POS Not allowed reason: Screen size is not allowed. " +
                        "Short size: $shortSize, Long size: $longSize, " +
                        "Minimum short size: $MIN_SCREEN_SHORT_SIZE_DP, Minimum long size: $MIN_SCREEN_LONG_SIZE_DP"
                )
            }
        }
    }

    private companion object {
        const val MIN_SCREEN_SHORT_SIZE_DP = 674
        const val MIN_SCREEN_LONG_SIZE_DP = 800
    }
}
