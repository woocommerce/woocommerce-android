package com.woocommerce.android.ui.woopos

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.ext.getScreenHeightDp
import com.woocommerce.android.ui.woopos.util.ext.getScreenWidthDp
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class WooPosIsScreenSizeAllowed @Inject constructor(
    private val context: Context,
    private val featureFlagRepository: FeatureFlagRepository,
    private val wooPosLog: WooPosLogWrapper,
) {
    operator fun invoke(): Boolean {
        val screenWidthDp = context.getScreenWidthDp()
        val screenHeightDp = context.getScreenHeightDp()

        val shortSize = min(screenWidthDp, screenHeightDp)
        val longSize = max(screenWidthDp, screenHeightDp)

        if (isTabletSize(shortSize.dp, longSize.dp)) return true

        if (featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)) return true

        wooPosLog.i(
            "POS Not allowed reason: Screen size is not allowed. " +
                "Short size: $shortSize, Long size: $longSize, " +
                "Minimum short size: $MIN_TABLET_SHORT_SIZE_DP, Minimum long size: $MIN_TABLET_LONG_SIZE_DP"
        )
        return false
    }

    companion object {
        internal const val MIN_TABLET_SHORT_SIZE_DP = 674
        internal const val MIN_TABLET_LONG_SIZE_DP = 800

        fun isTabletSize(shortSize: Dp, longSize: Dp): Boolean =
            shortSize >= MIN_TABLET_SHORT_SIZE_DP.dp && longSize >= MIN_TABLET_LONG_SIZE_DP.dp
    }
}
