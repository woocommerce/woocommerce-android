package com.woocommerce.android.ui.woopos

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosScreenSizeUtils
import com.woocommerce.android.ui.woopos.util.ext.getPhysicalScreenSizeDp
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosIsScreenSizeAllowed @Inject constructor(
    private val context: Context,
    private val featureFlagRepository: FeatureFlagRepository,
    private val wooPosLog: WooPosLogWrapper,
) {
    operator fun invoke(): Boolean {
        val size = context.getPhysicalScreenSizeDp()

        if (WooPosScreenSizeUtils.isTabletSize(size.shortSide.dp, size.longSide.dp)) return true

        if (featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)) return true

        wooPosLog.i(
            "POS Not allowed reason: Screen size is not allowed. " +
                "Short size: ${size.shortSide}, Long size: ${size.longSide}, " +
                "Minimum short size: ${WooPosScreenSizeUtils.MIN_TABLET_SHORT_SIZE_DP}, " +
                "Minimum long size: ${WooPosScreenSizeUtils.MIN_TABLET_LONG_SIZE_DP}"
        )
        return false
    }

    companion object {
        fun isTabletSize(shortSize: Dp, longSize: Dp): Boolean =
            WooPosScreenSizeUtils.isTabletSize(shortSize, longSize)
    }
}
