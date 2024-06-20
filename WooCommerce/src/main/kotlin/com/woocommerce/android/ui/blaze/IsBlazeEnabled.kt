package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_BLAZE
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
) {
    suspend operator fun invoke(): Boolean = selectedSite.getIfExists()?.isAdmin ?: false &&
        selectedSite.connectionType == SiteConnectionType.Jetpack &&
        selectedSite.getIfExists()?.canBlaze ?: false &&
        isRemoteFeatureFlagEnabled(WOO_BLAZE)
}
