package com.woocommerce.android.ui.blaze

import com.woocommerce.android.extensions.isSitePublic
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_BLAZE
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
) {
    suspend operator fun invoke(): Boolean = FeatureFlag.BLAZE.isEnabled() &&
        selectedSite.getIfExists()?.isAdmin ?: false &&
        selectedSite.getIfExists()?.canBlaze ?: false &&
        selectedSite.getIfExists()?.isSitePublic ?: false &&
        isRemoteFeatureFlagEnabled(WOO_BLAZE)
}
